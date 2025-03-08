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

package com.android.server.healthconnect.testing;

import android.database.DatabaseUtils;
import android.os.UserHandle;

import com.android.server.healthconnect.HealthConnectThreadScheduler;
import com.android.server.healthconnect.storage.HealthConnectDatabase;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

public final class TestUtils {
    public static final UserHandle TEST_USER = UserHandle.of(UserHandle.myUserId());

    public static void waitForTaskToFinishSuccessfully(Runnable task) throws TimeoutException {
        Instant startTime = Instant.now();
        while (true) {
            try {
                task.run();
                return;
            } catch (Exception e) {
                // ignore
            } finally {
                if (ChronoUnit.SECONDS.between(startTime, Instant.now()) > 3) {
                    throw new TimeoutException();
                }
            }
        }
    }

    static void waitForCondition(Predicate<Void> predicate, int timeoutSeconds)
            throws TimeoutException {
        Instant startTime = Instant.now();
        while (!predicate.test(null)) {
            if (ChronoUnit.SECONDS.between(startTime, Instant.now()) > timeoutSeconds) {
                throw new TimeoutException();
            }
        }
    }

    /** Wait for all the tasks to complete on the given scheduler. */
    public static void waitForAllScheduledTasksToComplete(
            HealthConnectThreadScheduler threadScheduler) throws TimeoutException {
        waitForCondition(
                (unused) ->
                        (threadScheduler.mInternalBackgroundExecutor.getTaskCount()
                                        == threadScheduler.mInternalBackgroundExecutor
                                                .getCompletedTaskCount())
                                && (threadScheduler.mControllerExecutor.getTaskCount()
                                        == threadScheduler.mControllerExecutor
                                                .getCompletedTaskCount())
                                && (threadScheduler.mBackgroundThreadExecutor.getTaskCount()
                                        == threadScheduler.mBackgroundThreadExecutor
                                                .getCompletedTaskCount())
                                && (threadScheduler.mForegroundExecutor.getTaskCount()
                                        == threadScheduler.mForegroundExecutor
                                                .getCompletedTaskCount()),
                15);
    }

    /** Returns the number of rows in the specified table. */
    public static long queryNumEntries(HealthConnectDatabase database, String tableName) {
        return DatabaseUtils.queryNumEntries(database.getReadableDatabase(), tableName);
    }
}
