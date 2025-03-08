/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.healthconnect;

import android.app.ActivityManager;
import android.content.Context;
import android.util.Slog;

import com.android.internal.annotations.VisibleForTesting;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A scheduler class to schedule task on the most relevant thread-pool.
 *
 * @hide
 */
public final class HealthConnectThreadScheduler {
    private static final int NUM_EXECUTOR_THREADS_INTERNAL_BACKGROUND = 1;
    private static final long KEEP_ALIVE_TIME_INTERNAL_BACKGROUND = 60L;
    private static final int NUM_EXECUTOR_THREADS_BACKGROUND = 1;
    private static final long KEEP_ALIVE_TIME_BACKGROUND = 60L;
    private static final int NUM_EXECUTOR_THREADS_FOREGROUND = 1;
    private static final long KEEP_ALIVE_TIME_SHARED = 60L;
    private static final int NUM_EXECUTOR_THREADS_CONTROLLER = 2;
    private static final long KEEP_ALIVE_TIME_CONTROLLER = 60L;

    private static final String TAG = "HealthConnectScheduler";

    // Scheduler to run the tasks in a RR fashion based on client package names.
    private final HealthConnectRoundRobinScheduler mRoundRobinScheduler =
            new HealthConnectRoundRobinScheduler();

    // Executor to run HC background tasks
    @VisibleForTesting
    public volatile ThreadPoolExecutor mBackgroundThreadExecutor = createBackgroundExecutor();

    // Executor to run HC background tasks
    @VisibleForTesting
    public volatile ThreadPoolExecutor mInternalBackgroundExecutor =
            createInternalBackgroundExecutor();

    // Executor to run HC tasks for clients
    @VisibleForTesting
    public volatile ThreadPoolExecutor mForegroundExecutor = createForegroundExecutor();

    // Executor to run HC controller tasks
    @VisibleForTesting
    public volatile ThreadPoolExecutor mControllerExecutor = createControllerExecutor();

    /** Reset all the executor thread pools in this executor */
    public void resetThreadPools() {
        mInternalBackgroundExecutor = createInternalBackgroundExecutor();
        mBackgroundThreadExecutor = createBackgroundExecutor();
        mForegroundExecutor = createForegroundExecutor();
        mControllerExecutor = createControllerExecutor();

        mRoundRobinScheduler.resume();
    }

    private static ThreadPoolExecutor createInternalBackgroundExecutor() {
        return new ThreadPoolExecutor(
                NUM_EXECUTOR_THREADS_INTERNAL_BACKGROUND,
                NUM_EXECUTOR_THREADS_INTERNAL_BACKGROUND,
                KEEP_ALIVE_TIME_INTERNAL_BACKGROUND,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                new NamedThreadFactory("hc-int-bg-"));
    }

    private static ThreadPoolExecutor createBackgroundExecutor() {
        return new ThreadPoolExecutor(
                NUM_EXECUTOR_THREADS_BACKGROUND,
                NUM_EXECUTOR_THREADS_BACKGROUND,
                KEEP_ALIVE_TIME_BACKGROUND,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                new NamedThreadFactory("hc-bg-"));
    }

    private static ThreadPoolExecutor createForegroundExecutor() {
        return new ThreadPoolExecutor(
                NUM_EXECUTOR_THREADS_FOREGROUND,
                NUM_EXECUTOR_THREADS_FOREGROUND,
                KEEP_ALIVE_TIME_SHARED,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                new NamedThreadFactory("hc-fg-"));
    }

    private static ThreadPoolExecutor createControllerExecutor() {
        return new ThreadPoolExecutor(
                NUM_EXECUTOR_THREADS_CONTROLLER,
                NUM_EXECUTOR_THREADS_CONTROLLER,
                KEEP_ALIVE_TIME_CONTROLLER,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                new NamedThreadFactory("hc-ctrl-"));
    }

    void shutdownThreadPools() {
        mRoundRobinScheduler.killTasksAndPauseScheduler();

        mInternalBackgroundExecutor.shutdownNow();
        mBackgroundThreadExecutor.shutdownNow();
        mForegroundExecutor.shutdownNow();
        mControllerExecutor.shutdownNow();
    }

    /** Schedules the task on the executor dedicated for performing internal tasks */
    public void scheduleInternalTask(Runnable task) {
        safeExecute(mInternalBackgroundExecutor, getSafeRunnable(task));
    }

    /** Schedules the task on the executor dedicated for performing controller tasks */
    void scheduleControllerTask(Runnable task) {
        safeExecute(mControllerExecutor, getSafeRunnable(task));
    }

    /** Schedules the task on the best possible executor based on the parameters */
    void schedule(Context context, Runnable task, int uid, boolean isController) {
        if (isController) {
            safeExecute(mControllerExecutor, getSafeRunnable(task));
            return;
        }

        if (isUidInForeground(context, uid)) {
            safeExecute(
                    mForegroundExecutor,
                    getSafeRunnable(
                            () -> {
                                if (!isUidInForeground(context, uid)) {
                                    // The app is no longer in foreground so move the task to
                                    // background thread. This is because foreground thread should
                                    // only be used by the foreground app and since the request of
                                    // this task is no longer in foreground we don't want it to
                                    // consume foreground resource anymore.
                                    mRoundRobinScheduler.addTask(uid, task);
                                    safeExecute(
                                            mBackgroundThreadExecutor,
                                            () -> mRoundRobinScheduler.getNextTask().run());
                                    return;
                                }

                                task.run();
                            }));
        } else {
            mRoundRobinScheduler.addTask(uid, task);
            safeExecute(
                    mBackgroundThreadExecutor,
                    getSafeRunnable(() -> mRoundRobinScheduler.getNextTask().run()));
        }
    }

    private static boolean isUidInForeground(Context context, int uid) {
        ActivityManager activityManager = context.getSystemService(ActivityManager.class);
        Objects.requireNonNull(activityManager);
        List<ActivityManager.RunningAppProcessInfo> runningAppProcesses =
                activityManager.getRunningAppProcesses();
        if (runningAppProcesses == null) {
            return false;
        }
        for (ActivityManager.RunningAppProcessInfo info : runningAppProcesses) {
            if (info.uid == uid
                    && info.importance
                            == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                return true;
            }
        }
        return false;
    }

    private static void safeExecute(ThreadPoolExecutor executor, Runnable task) {
        try {
            executor.execute(task);
        } catch (RejectedExecutionException ex) {
            // this is to prevent unexpected crashes, see b/325746130
            Slog.e(TAG, executor + " is shutting down or already terminated!", ex);
        }
    }

    // Makes sure that any exceptions don't end up in system_server.
    private static Runnable getSafeRunnable(Runnable task) {
        return () -> {
            try {
                task.run();
            } catch (Exception e) {
                Slog.e(TAG, "Internal task schedule failed", e);
            }
        };
    }

    private static class NamedThreadFactory implements ThreadFactory {
        private final ThreadFactory mDefaultFactory = Executors.defaultThreadFactory();
        private final AtomicInteger mCount = new AtomicInteger();
        private final String mPrefix;

        NamedThreadFactory(String prefix) {
            mPrefix = prefix;
        }

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = mDefaultFactory.newThread(runnable);
            thread.setName(mPrefix + mCount.getAndIncrement());
            return thread;
        }
    }
}
