/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.server.healthconnect.telemetry.dataquality;

import static android.health.connect.Constants.MAXIMUM_PAGE_SIZE;

import android.content.pm.PackageManager;
import android.util.Slog;

import com.android.server.healthconnect.common.metadata.AppInfoHelper;
import com.android.server.healthconnect.fitness.recordhelpers.IntervalRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.RecordHelper;
import com.android.server.healthconnect.storage.request.ReadTableRequest;
import com.android.server.healthconnect.storage.utils.WhereClauses;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Util class for various classes collecting Data QUality Metrics.
 *
 * @hide
 */
final class DataQualityUtils {
    private static final String TAG = "DataQualityUtils";

    /** Returns {@link ReadTableRequest} to read past week data for given table name. */
    static ReadTableRequest getReadLastWeekSessionsRequest(String tableName) {
        final Instant now = Instant.now();
        final Instant weekAgo = now.minus(7, ChronoUnit.DAYS);

        // TODO(b/437880789): update if filtering column needs to be changed to Last Modified Date
        // instead
        WhereClauses whereClause = new WhereClauses(WhereClauses.LogicalOperator.AND);
        whereClause.addWhereLaterThanTimeClause(
                IntervalRecordHelper.END_TIME_COLUMN_NAME, weekAgo.toEpochMilli());

        return new ReadTableRequest(tableName)
                .setColumnNames(
                        List.of(
                                RecordHelper.APP_INFO_ID_COLUMN_NAME,
                                IntervalRecordHelper.END_TIME_COLUMN_NAME,
                                IntervalRecordHelper.START_TIME_COLUMN_NAME,
                                RecordHelper.LAST_MODIFIED_TIME_COLUMN_NAME))
                // We do not expect more than MAXIMUM_PAGE_SIZE records for Exercise or Sleep in
                // one week.
                .setLimit(MAXIMUM_PAGE_SIZE)
                .setWhereClause(whereClause);
    }

    static Optional<String> getPackageName(AppInfoHelper appInfoHelper, long appId) {
        String packageName;
        try {
            packageName = appInfoHelper.getPackageName(appId);
        } catch (PackageManager.NameNotFoundException ex) {
            Slog.w(TAG, "Invalid app id " + appId);
            return Optional.empty();
        }
        return Optional.of(packageName);
    }
}
