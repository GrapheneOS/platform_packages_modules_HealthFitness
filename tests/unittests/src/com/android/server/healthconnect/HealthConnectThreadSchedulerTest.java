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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Process;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.server.healthconnect.testing.TestUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

@RunWith(AndroidJUnit4.class)
public class HealthConnectThreadSchedulerTest {
    private ThreadPoolExecutor mInternalTaskScheduler;
    private ThreadPoolExecutor mControllerTaskScheduler;
    private ThreadPoolExecutor mForegroundTaskScheduler;
    private ThreadPoolExecutor mBackgroundTaskScheduler;
    private long mInternalTaskSchedulerCompletedJobs;
    private long mControllerTaskSchedulerCompletedJobs;
    private long mForegroundTaskSchedulerCompletedJobs;
    private long mBackgroundTaskSchedulerCompletedJobs;
    private Context mContext;
    private HealthConnectThreadScheduler mHealthConnectThreadScheduler;

    @Mock private Context mMockContext;
    @Mock private ActivityManager mActivityManager;

    @Before
    public void setUp() {
        mHealthConnectThreadScheduler = new HealthConnectThreadScheduler();
        MockitoAnnotations.initMocks(this);

        mHealthConnectThreadScheduler.resetThreadPools();

        mInternalTaskScheduler = mHealthConnectThreadScheduler.mInternalBackgroundExecutor;
        mInternalTaskSchedulerCompletedJobs = mInternalTaskScheduler.getCompletedTaskCount();
        mControllerTaskScheduler = mHealthConnectThreadScheduler.mControllerExecutor;
        mControllerTaskSchedulerCompletedJobs = mControllerTaskScheduler.getCompletedTaskCount();
        mForegroundTaskScheduler = mHealthConnectThreadScheduler.mForegroundExecutor;
        mForegroundTaskSchedulerCompletedJobs = mForegroundTaskScheduler.getCompletedTaskCount();
        mBackgroundTaskScheduler = mHealthConnectThreadScheduler.mBackgroundThreadExecutor;
        mBackgroundTaskSchedulerCompletedJobs = mBackgroundTaskScheduler.getCompletedTaskCount();
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
    }

    @Test
    public void testSchedulerScheduleInternal() throws Exception {
        mHealthConnectThreadScheduler.scheduleInternalTask(() -> {});
        TestUtils.waitForTaskToFinishSuccessfully(
                () -> {
                    if (mInternalTaskScheduler.getCompletedTaskCount()
                            != mInternalTaskSchedulerCompletedJobs + 1) {
                        throw new RuntimeException();
                    }
                });
    }

    @Test
    public void testScheduleControllerTask() throws Exception {
        mHealthConnectThreadScheduler.scheduleControllerTask(() -> {});
        TestUtils.waitForTaskToFinishSuccessfully(
                () -> {
                    if (mControllerTaskScheduler.getCompletedTaskCount()
                            != mControllerTaskSchedulerCompletedJobs + 1) {
                        throw new RuntimeException();
                    }
                });
    }

    @Test
    public void testScheduleBackgroundTask() throws Exception {
        mockCurrentProcessImportance(ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED);

        mHealthConnectThreadScheduler.schedule(mMockContext, () -> {}, Process.myUid(), false);
        TestUtils.waitForTaskToFinishSuccessfully(
                () -> {
                    if (mBackgroundTaskScheduler.getCompletedTaskCount()
                            != mBackgroundTaskSchedulerCompletedJobs + 1) {
                        throw new RuntimeException();
                    }
                });
    }

    @Test
    public void testScheduleForegroundTask() throws Exception {
        mockCurrentProcessImportance(ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND);

        mHealthConnectThreadScheduler.schedule(mMockContext, () -> {}, Process.myUid(), false);
        TestUtils.waitForTaskToFinishSuccessfully(
                () -> {
                    if (mForegroundTaskScheduler.getCompletedTaskCount()
                            != mForegroundTaskSchedulerCompletedJobs + 1) {
                        throw new RuntimeException();
                    }
                });
    }

    @Test
    public void testHealthConnectScheduler_runningAppProcessNull() throws Exception {
        when(mMockContext.getSystemService(ActivityManager.class)).thenReturn(mActivityManager);
        when(mActivityManager.getRunningAppProcesses()).thenReturn(null);

        mHealthConnectThreadScheduler.scheduleInternalTask(() -> {});
        TestUtils.waitForTaskToFinishSuccessfully(
                () -> {
                    if (mInternalTaskScheduler.getCompletedTaskCount()
                            != mInternalTaskSchedulerCompletedJobs + 1) {
                        throw new RuntimeException();
                    }
                });
    }

    @Test
    public void testHealthConnectSchedulerClear() {
        assertThat(mInternalTaskSchedulerCompletedJobs).isEqualTo(0);
        assertThat(mControllerTaskSchedulerCompletedJobs).isEqualTo(0);
        assertThat(mForegroundTaskSchedulerCompletedJobs).isEqualTo(0);
        assertThat(mBackgroundTaskSchedulerCompletedJobs).isEqualTo(0);
    }

    @Test
    public void testScheduleAfterTheSchedulersAreShutdown_expectNoException() {
        mHealthConnectThreadScheduler.shutdownThreadPools();

        mHealthConnectThreadScheduler.schedule(mContext, () -> {}, Process.myUid(), false);
        mHealthConnectThreadScheduler.schedule(mContext, () -> {}, Process.myUid(), true);
        mHealthConnectThreadScheduler.scheduleInternalTask(() -> {});
        mHealthConnectThreadScheduler.scheduleControllerTask(() -> {});
    }

    @Test
    public void testInternalSchedulerThreadName() throws Exception {
        Future<String> name = mInternalTaskScheduler.submit(() -> Thread.currentThread().getName());
        assertThat(name.get()).isEqualTo("hc-int-bg-0");
    }

    @Test
    public void testControllerSchedulerThreadName() throws Exception {
        Future<String> name =
                mControllerTaskScheduler.submit(() -> Thread.currentThread().getName());
        assertThat(name.get()).startsWith("hc-ctrl-");
    }

    @Test
    public void testForegroundSchedulerThreadName() throws Exception {
        Future<String> name =
                mForegroundTaskScheduler.submit(() -> Thread.currentThread().getName());
        assertThat(name.get()).startsWith("hc-fg-0");
    }

    @Test
    public void testBackgroundSchedulerThreadName() throws Exception {
        Future<String> name =
                mBackgroundTaskScheduler.submit(() -> Thread.currentThread().getName());
        assertThat(name.get()).isEqualTo("hc-bg-0");
    }

    private void mockCurrentProcessImportance(int importance) {
        when(mMockContext.getSystemService(ActivityManager.class)).thenReturn(mActivityManager);
        ActivityManager.RunningAppProcessInfo runningAppProcessInfo =
                new ActivityManager.RunningAppProcessInfo();
        runningAppProcessInfo.uid = Process.myUid();
        runningAppProcessInfo.importance = importance;
        when(mActivityManager.getRunningAppProcesses()).thenReturn(List.of(runningAppProcessInfo));
    }
}
