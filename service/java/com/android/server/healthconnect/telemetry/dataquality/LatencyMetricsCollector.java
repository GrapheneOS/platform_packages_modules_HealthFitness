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

import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorLong;

import android.content.pm.PackageManager;
import android.database.Cursor;

import com.android.server.healthconnect.common.metadata.AppInfoHelper;
import com.android.server.healthconnect.fitness.recordhelpers.ExerciseSessionRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.IntervalRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.RecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.SleepSessionRecordHelper;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.request.ReadTableRequest;
import com.android.server.healthconnect.storage.utils.WhereClauses;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * A class to collect latency metrics.
 *
 * @hide
 */
public final class LatencyMetricsCollector {

    private final TransactionManager mTransactionManager;
    private final AppInfoHelper mAppInfoHelper;

    public LatencyMetricsCollector(
            TransactionManager transactionManager, AppInfoHelper appInfoHelper) {
        mTransactionManager = transactionManager;
        mAppInfoHelper = appInfoHelper;
    }

    /** Collects latency for past 7 days' exercise sessions. */
    public List<LatencyMetricsData> readLastWeekExerciseSessions() {
        return readLastWeekSessions(ExerciseSessionRecordHelper.EXERCISE_SESSION_RECORD_TABLE_NAME);
    }

    /** Collects latency for past 7 days' sleep sessions. */
    public List<LatencyMetricsData> readLastWeekSleepSessions() {
        return readLastWeekSessions(SleepSessionRecordHelper.SLEEP_SESSION_RECORD_TABLE_NAME);
    }

    private List<LatencyMetricsData> readLastWeekSessions(String tableName) {
        List<LatencyMetricsData> latencyMetricsPerRecordList = new ArrayList<>();
        try (Cursor cursor = mTransactionManager.read(getReadLastWeekSessionsRequest(tableName))) {
            while (cursor.moveToNext()) {
                long endTime = getCursorLong(cursor, IntervalRecordHelper.END_TIME_COLUMN_NAME);
                long lastModifiedTime =
                        getCursorLong(cursor, RecordHelper.LAST_MODIFIED_TIME_COLUMN_NAME);
                long appInfoId = getCursorLong(cursor, RecordHelper.APP_INFO_ID_COLUMN_NAME);
                String packageName;
                try {
                    packageName = mAppInfoHelper.getPackageName(appInfoId);
                } catch (PackageManager.NameNotFoundException e) {
                    continue;
                }
                latencyMetricsPerRecordList.add(
                        new LatencyMetricsData(
                                packageName,
                                /* latency= */ Duration.ofMillis(lastModifiedTime - endTime)));
            }
        }
        return latencyMetricsPerRecordList;
    }

    private ReadTableRequest getReadLastWeekSessionsRequest(String tableName) {
        final Instant now = Instant.now();
        final Instant weekAgo = now.minus(7, ChronoUnit.DAYS);

        WhereClauses whereClause = new WhereClauses(WhereClauses.LogicalOperator.AND);
        whereClause.addWhereLaterThanTimeClause(
                IntervalRecordHelper.END_TIME_COLUMN_NAME, weekAgo.toEpochMilli());

        return new ReadTableRequest(tableName)
                .setColumnNames(
                        List.of(
                                RecordHelper.APP_INFO_ID_COLUMN_NAME,
                                IntervalRecordHelper.END_TIME_COLUMN_NAME,
                                RecordHelper.LAST_MODIFIED_TIME_COLUMN_NAME))
                // We do not expect more than MAXIMUM_PAGE_SIZE records for Exercise or Sleep in
                // one week.
                .setLimit(MAXIMUM_PAGE_SIZE)
                .setWhereClause(whereClause);
    }

    /**
     * Data class to hold latency i.e. time between session end and time when the session was
     * inserted for every record.
     *
     * @param packageName The package name of the app that inserted the record.
     * @param latency The duration between the end time of the record and the last modified time.
     */
    public record LatencyMetricsData(String packageName, Duration latency) {}
}
