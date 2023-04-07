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

package com.android.server.healthconnect.migration;

import static android.health.connect.HealthConnectDataState.MIGRATION_STATE_ALLOWED;
import static android.health.connect.HealthConnectDataState.MIGRATION_STATE_IN_PROGRESS;

import static com.android.server.healthconnect.migration.MigrationConstants.COUNT_DEFAULT;
import static com.android.server.healthconnect.migration.MigrationConstants.COUNT_MIGRATION_STATE_ALLOWED;
import static com.android.server.healthconnect.migration.MigrationConstants.COUNT_MIGRATION_STATE_IN_PROGRESS;
import static com.android.server.healthconnect.migration.MigrationConstants.EXTRA_USER_ID;
import static com.android.server.healthconnect.migration.MigrationConstants.INTERVAL_DEFAULT;
import static com.android.server.healthconnect.migration.MigrationConstants.IN_PROGRESS_STATE_TIMEOUT_PERIOD;
import static com.android.server.healthconnect.migration.MigrationConstants.NON_IDLE_STATE_TIMEOUT_PERIOD;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.health.connect.Constants;
import android.os.PersistableBundle;
import android.util.Slog;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * This class schedules the {@link MigrationBroadcastJobService} service.
 *
 * @hide
 */
public final class MigrationBroadcastScheduler {

    private static final String TAG = "MigrationBroadcastScheduler";
    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private int mUserId;

    @GuardedBy("mLock")
    private List<Integer> mJobIdArray;

    public MigrationBroadcastScheduler(int userId) {
        mUserId = userId;
        mJobIdArray = new ArrayList<Integer>();
    }

    /** Sets userId. Invoked when the user is switched. */
    public void setUserId(int userId) {
        synchronized (mLock) {
            mUserId = userId;
        }
    }

    /***
     * Cancels all previously scheduled {@link MigrationBroadcastJobService} service jobs.
     * Retrieves the requiredCount and requiredInterval corresponding to the given migration
     * state.
     * If the requiredInterval is greater than or equal to the minimum interval allowed for
     * periodic jobs, a periodic job is scheduled, else a set of non-periodic jobs are
     * pre-scheduled.
     */
    public void scheduleNewJobs(Context context) {
        synchronized (mLock) {
            int migrationState = MigrationStateManager.getInitialisedInstance().getMigrationState();

            if (Constants.DEBUG) {
                Slog.d(TAG, "Current migration state: " + migrationState);
                Slog.d(TAG, "Current user: " + mUserId);
            }

            JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);
            Objects.requireNonNull(jobScheduler);
            for (int jobId : mJobIdArray) {
                if (Constants.DEBUG) {
                    Slog.d(TAG, "Cancelling job : " + jobId);
                }
                jobScheduler.cancel(jobId);
            }
            mJobIdArray.clear();

            int requiredCount = getRequiredCount(migrationState);
            long requiredInterval = getRequiredInterval(migrationState);

            try {
                if ((requiredCount > 0) && (requiredInterval >= JobInfo.getMinPeriodMillis())) {
                    createJobLocked(
                            /* createPeriodic= */ true,
                            /* scheduledCount= */ 0,
                            requiredInterval,
                            context);
                } else {
                    int scheduledCount = 0;
                    while (scheduledCount < requiredCount) {
                        createJobLocked(
                                /* createPeriodic= */ false,
                                scheduledCount,
                                requiredInterval,
                                context);
                        scheduledCount += 1;
                    }
                }
            } catch (Exception e) {
                Slog.e(TAG, "Exception while creating job : ", e);
            }
        }
    }

    /***
     * Creates a new {@link MigrationBroadcastJobService} job, to which it passes the user id in a
     * PersistableBundle object.
     * If the requiredInterval is greater than the minimum interval allowed for periodic jobs, a
     * periodic job is created.
     *
     * @param createPeriodic Indicates whether to create a periodic job
     * @param scheduledCount Number of jobs that have already been scheduled for a particular
     *                       migration state
     * @param requiredInterval Time interval between each successive job for that current
     *                         migration state
     * @param context Context
     *
     * @throws Exception if migration broadcast job scheduling fails.
     */
    @GuardedBy("mLock")
    private void createJobLocked(
            boolean createPeriodic, int scheduledCount, long requiredInterval, Context context)
            throws Exception {
        ComponentName schedulerServiceComponent =
                new ComponentName(context, MigrationBroadcastJobService.class);

        int uuid = UUID.randomUUID().toString().hashCode();
        int jobId = String.valueOf(mUserId + uuid).hashCode();

        final PersistableBundle extras = new PersistableBundle();
        extras.putInt(EXTRA_USER_ID, mUserId);

        JobInfo.Builder builder =
                new JobInfo.Builder(jobId, schedulerServiceComponent).setExtras(extras);

        if (createPeriodic) {
            builder.setPeriodic(requiredInterval);
        } else {
            long interval = requiredInterval * scheduledCount;
            builder.setMinimumLatency(interval).setOverrideDeadline(interval);
        }

        JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);
        Objects.requireNonNull(jobScheduler);
        int result = jobScheduler.schedule(builder.build());
        if (result == JobScheduler.RESULT_SUCCESS) {
            if (Constants.DEBUG) {
                Slog.d(TAG, "Successfully scheduled migration broadcast job");
            }
            mJobIdArray.add(jobId);
        } else {
            throw new Exception("Failed to schedule migration broadcast job");
        }
    }

    /**
     * Returns the number of migration broadcast jobs to be scheduled for the given migration state.
     */
    @VisibleForTesting
    int getRequiredCount(int migrationState) {
        switch (migrationState) {
            case MIGRATION_STATE_IN_PROGRESS:
                return COUNT_MIGRATION_STATE_IN_PROGRESS;
            case MIGRATION_STATE_ALLOWED:
                return COUNT_MIGRATION_STATE_ALLOWED;
            default:
                return COUNT_DEFAULT;
        }
    }

    /** Returns the interval between each migration broadcast job for the given migration state. */
    @VisibleForTesting
    long getRequiredInterval(int migrationState) {
        switch (migrationState) {
            case MIGRATION_STATE_IN_PROGRESS:
                return calculateRequiredInterval(
                        IN_PROGRESS_STATE_TIMEOUT_PERIOD, COUNT_MIGRATION_STATE_IN_PROGRESS);
            case MIGRATION_STATE_ALLOWED:
                return calculateRequiredInterval(
                        NON_IDLE_STATE_TIMEOUT_PERIOD, COUNT_MIGRATION_STATE_ALLOWED);
            default:
                return INTERVAL_DEFAULT;
        }
    }

    private static long calculateRequiredInterval(Duration timeoutPeriod, int maxBroadcastCount) {
        return timeoutPeriod.toMillis() / maxBroadcastCount;
    }
}
