/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.server.healthconnect.storage;

import android.util.Slog;

import com.android.server.healthconnect.fitness.FitnessRecordDeleteHelper;
import com.android.server.healthconnect.storage.datatypehelpers.AccessLogsHelper;
import com.android.server.healthconnect.storage.datatypehelpers.ActivityDateHelper;
import com.android.server.healthconnect.storage.datatypehelpers.AppInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.ChangeLogsHelper;
import com.android.server.healthconnect.storage.datatypehelpers.ChangeLogsRequestHelper;
import com.android.server.healthconnect.storage.datatypehelpers.HealthDataCategoryPriorityHelper;
import com.android.server.healthconnect.storage.datatypehelpers.ReadAccessLogsHelper;
import com.android.server.healthconnect.storage.request.DeleteTableRequest;
import com.android.server.healthconnect.storage.utils.InternalHealthConnectMappings;
import com.android.server.healthconnect.storage.utils.PreferencesManager;

import java.util.ArrayList;
import java.util.List;

/**
 * A service that is run periodically to handle deletion of stale entries in HC DB.
 *
 * @hide
 */
public class DailyCleanupJob {

    private static final String TAG = "HealthConnectAutoDelete";

    private final HealthDataCategoryPriorityHelper mHealthDataCategoryPriorityHelper;
    private final PreferencesManager mPreferencesManager;
    private final AppInfoHelper mAppInfoHelper;
    private final TransactionManager mTransactionManager;
    private final FitnessRecordDeleteHelper mFitnessRecordDeleteHelper;
    private final ActivityDateHelper mActivityDateHelper;

    public DailyCleanupJob(
            HealthDataCategoryPriorityHelper healthDataCategoryPriorityHelper,
            PreferencesManager preferencesManager,
            AppInfoHelper appInfoHelper,
            TransactionManager transactionManager,
            FitnessRecordDeleteHelper fitnessRecordDeleteHelper,
            ActivityDateHelper activityDateHelper) {
        mHealthDataCategoryPriorityHelper = healthDataCategoryPriorityHelper;
        mPreferencesManager = preferencesManager;
        mAppInfoHelper = appInfoHelper;
        mTransactionManager = transactionManager;
        mFitnessRecordDeleteHelper = fitnessRecordDeleteHelper;
        mActivityDateHelper = activityDateHelper;
    }

    /** Starts the Auto Deletion process. */
    public void startDailyCleanup() {
        try {
            // Only do transactional operations here - as this job might get cancelled for several
            // reasons, such as: User switch, low battery etc.
            deleteStaleRecordEntries();
            deleteStaleChangeLogEntries();
            deleteStaleAccessLogEntries();
            // Update the recordTypesUsed by packages if required after the deletion of records.
            mAppInfoHelper.syncAppInfoRecordTypesUsed();
            // Re-sync activity dates table
            mActivityDateHelper.reSyncForAllRecords();
            // Sync health data priority list table
            mHealthDataCategoryPriorityHelper.reSyncHealthDataPriorityTable();
        } catch (Exception e) {
            Slog.e(TAG, "Auto delete run failed", e);
            // Don't rethrow as that will crash system_server
        }
    }

    private void deleteStaleRecordEntries() {
        int recordAutoDeletePeriod = mPreferencesManager.getRecordRetentionPeriodInDays();
        if (recordAutoDeletePeriod != 0) {
            // 0 represents that no period is set,to delete only if not 0 else don't do anything
            List<DeleteTableRequest> deleteTableRequests = new ArrayList<>();
            InternalHealthConnectMappings.getInstance()
                    .getRecordHelpers()
                    .forEach(
                            (recordHelper) -> {
                                DeleteTableRequest request =
                                        recordHelper.getDeleteRequestForAutoDelete(
                                                recordAutoDeletePeriod);
                                deleteTableRequests.add(request);
                            });
            try {
                mFitnessRecordDeleteHelper.deleteRecordsUnrestricted(deleteTableRequests);
            } catch (Exception exception) {
                Slog.e(TAG, "Auto delete for records failed", exception);
                // Don't rethrow as that will crash system_server
            }
        }
    }

    private void deleteStaleChangeLogEntries() {
        try {
            mTransactionManager.deleteAll(
                    List.of(
                            ChangeLogsHelper.getDeleteRequestForAutoDelete(),
                            ChangeLogsRequestHelper.getDeleteRequestForAutoDelete()));
        } catch (Exception exception) {
            Slog.e(TAG, "Auto delete for Change logs failed", exception);
            // Don't rethrow as that will crash system_server
        }
    }

    private void deleteStaleAccessLogEntries() {
        try {
            mTransactionManager.deleteAll(
                    List.of(
                            AccessLogsHelper.getDeleteRequestForAutoDelete(),
                            ReadAccessLogsHelper.getDeleteRequestForAutoDelete()));
        } catch (Exception exception) {
            Slog.e(TAG, "Auto delete for Access logs failed", exception);
            // Don't rethrow as that will crash system_server
        }
    }
}
