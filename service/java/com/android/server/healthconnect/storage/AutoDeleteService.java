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

import com.android.server.healthconnect.storage.datatypehelpers.PreferenceHelper;

/**
 * A service that is run periodically to handle deletion of stale entries in HC DB.
 *
 * @hide
 */
public class AutoDeleteService {
    private static final String AUTO_DELETE_DURATION_RECORDS_KEY =
            "auto_delete_duration_records_key";
    private static final String TAG = "HealthConnectAutoDelete";

    /** Gets auto delete period for automatically deleting record entries */
    public static int getRecordRetentionPeriodInDays() {
        String result =
                PreferenceHelper.getInstance().getPreference(AUTO_DELETE_DURATION_RECORDS_KEY);

        if (result == null) return 0;
        return Integer.parseInt(result);
    }

    /** Sets auto delete period for automatically deleting record entries */
    public static void setRecordRetentionPeriodInDays(int days) {
        PreferenceHelper.getInstance()
                .insertPreference(AUTO_DELETE_DURATION_RECORDS_KEY, String.valueOf(days));
    }

    /** Starts the Auto Deletion process. */
    public static void startAutoDelete() {
        try {
            // Only do transactional operations here - as this job might get cancelled for
            // several reasons, such as: User switch, low battery etc.
            String recordAutoDeletePeriodString =
                    PreferenceHelper.getInstance().getPreference(AUTO_DELETE_DURATION_RECORDS_KEY);
            int recordAutoDeletePeriod =
                    recordAutoDeletePeriodString == null
                            ? 0
                            : Integer.parseInt(recordAutoDeletePeriodString);
            try {
                TransactionManager.getInitialisedInstance()
                        .deleteStaleRecordEntries(recordAutoDeletePeriod);
            } catch (Exception exception) {
                Slog.e(TAG, "Auto delete for records failed", exception);
                // Don't rethrow as that will crash system_server
            }

            try {
                TransactionManager.getInitialisedInstance().deleteStaleChangeLogEntries();
            } catch (Exception exception) {
                Slog.e(TAG, "Auto delete for change logs failed", exception);
                // Don't rethrow as that will crash system_server
            }
        } catch (Exception e) {
            Slog.e(TAG, "Auto delete run failed", e);
            // Don't rethrow as that will crash system_server
        }
    }
}
