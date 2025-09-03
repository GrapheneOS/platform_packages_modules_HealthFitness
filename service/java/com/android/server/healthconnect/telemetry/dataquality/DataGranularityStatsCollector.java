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

import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorLong;
import static com.android.server.healthconnect.telemetry.dataquality.DataQualityUtils.getReadLastWeekSessionsRequest;

import android.content.pm.PackageManager;
import android.database.Cursor;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.util.Slog;

import com.android.healthfitness.flags.Flags;
import com.android.server.healthconnect.common.metadata.AppInfoHelper;
import com.android.server.healthconnect.fitness.recordhelpers.ActiveCaloriesBurnedRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.CyclingPedalingCadenceRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.DistanceRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.ElevationGainedRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.ExerciseSessionRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.FloorsClimbedRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.HeartRateRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.IntervalRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.PowerRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.RecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.SeriesRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.SkinTemperatureRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.SpeedRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.StepsCadenceRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.StepsRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.TotalCaloriesBurnedRecordHelper;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.request.ReadTableRequest;
import com.android.server.healthconnect.storage.utils.SqlJoin;
import com.android.server.healthconnect.storage.utils.WhereClauses;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Logs Health Connect granularity for various datatypes.
 *
 * @hide
 */
public final class DataGranularityStatsCollector {

    private static final String TAG = "DataGranularityStatsCollector";

    private final TransactionManager mTransactionManager;
    private final AppInfoHelper mAppInfoHelper;

    record SeriesHelperData(String tableName, String seriesTableName) {}

    record GranularityStats(
            String packageName,
            @RecordTypeIdentifier.RecordType int recordIdentifier,
            long granularity) {}

    /** A record to hold all granularity stats, categorized by active and passive data. */
    record AllGranularityStats(
            List<GranularityStats> activeStats, List<GranularityStats> passiveStats) {}

    /** Data class to hold start and end time for a session. */
    private record TimeRange(long startTime, long endTime) {}

    private static final Map<@RecordTypeIdentifier.RecordType Integer, SeriesHelperData>
            SERIES_TYPE_ID_TO_TABLE_NAME_MAP =
                    Map.of(
                            RecordTypeIdentifier.RECORD_TYPE_HEART_RATE,
                            new SeriesHelperData(
                                    HeartRateRecordHelper.TABLE_NAME,
                                    HeartRateRecordHelper.SERIES_TABLE_NAME),
                            RecordTypeIdentifier.RECORD_TYPE_SPEED,
                            new SeriesHelperData(
                                    SpeedRecordHelper.TABLE_NAME,
                                    SpeedRecordHelper.SERIES_TABLE_NAME),
                            RecordTypeIdentifier.RECORD_TYPE_POWER,
                            new SeriesHelperData(
                                    PowerRecordHelper.TABLE_NAME,
                                    PowerRecordHelper.SERIES_TABLE_NAME),
                            RecordTypeIdentifier.RECORD_TYPE_STEPS_CADENCE,
                            new SeriesHelperData(
                                    StepsCadenceRecordHelper.TABLE_NAME,
                                    StepsCadenceRecordHelper.SERIES_TABLE_NAME),
                            RecordTypeIdentifier.RECORD_TYPE_CYCLING_PEDALING_CADENCE,
                            new SeriesHelperData(
                                    CyclingPedalingCadenceRecordHelper.TABLE_NAME,
                                    CyclingPedalingCadenceRecordHelper.SERIES_TABLE_NAME),
                            RecordTypeIdentifier.RECORD_TYPE_SKIN_TEMPERATURE,
                            new SeriesHelperData(
                                    SkinTemperatureRecordHelper.TABLE_NAME,
                                    SkinTemperatureRecordHelper.SERIES_TABLE_NAME));

    private static final Map<@RecordTypeIdentifier.RecordType Integer, String>
            INTERVAL_TYPE_ID_TO_TABLE_NAME_MAP =
                    Map.of(
                            RecordTypeIdentifier.RECORD_TYPE_STEPS,
                            StepsRecordHelper.STEPS_TABLE_NAME,
                            RecordTypeIdentifier.RECORD_TYPE_DISTANCE,
                            DistanceRecordHelper.DISTANCE_RECORD_TABLE_NAME,
                            RecordTypeIdentifier.RECORD_TYPE_ACTIVE_CALORIES_BURNED,
                            ActiveCaloriesBurnedRecordHelper
                                    .ACTIVE_CALORIES_BURNED_RECORD_TABLE_NAME,
                            RecordTypeIdentifier.RECORD_TYPE_TOTAL_CALORIES_BURNED,
                            TotalCaloriesBurnedRecordHelper.TOTAL_CALORIES_BURNED_RECORD_TABLE_NAME,
                            RecordTypeIdentifier.RECORD_TYPE_ELEVATION_GAINED,
                            ElevationGainedRecordHelper.ELEVATION_GAINED_RECORD_TABLE_NAME,
                            RecordTypeIdentifier.RECORD_TYPE_FLOORS_CLIMBED,
                            FloorsClimbedRecordHelper.FLOORS_CLIMBED_RECORD_TABLE_NAME);

    public DataGranularityStatsCollector(
            TransactionManager transactionManager, AppInfoHelper appInfoHelper) {
        mTransactionManager = transactionManager;
        mAppInfoHelper = appInfoHelper;
    }

    /** Returns {@link AllGranularityStats} for given session data type for past week. */
    AllGranularityStats getAllGranularityStatsForLastWeek() {
        if (!Flags.latencyMetricsFlag()) {
            return new AllGranularityStats(Collections.emptyList(), Collections.emptyList());
        }

        String tableName = ExerciseSessionRecordHelper.EXERCISE_SESSION_RECORD_TABLE_NAME;
        Map<Long, List<TimeRange>> appToSessionTimeMap = getAppToSessionTimeMap(tableName);

        List<GranularityStats> activeStats = new ArrayList<>();
        activeStats.addAll(getLastWeekIntervalGranularityStats(appToSessionTimeMap));
        activeStats.addAll(getLastWeekSeriesGranularityStats(appToSessionTimeMap));

        return new AllGranularityStats(
                Collections.unmodifiableList(activeStats), Collections.emptyList());
    }

    private List<GranularityStats> getLastWeekIntervalGranularityStats(
            Map<Long, List<TimeRange>> appToSessionTimeMap) {
        List<GranularityStats> granularityStats = new ArrayList<>();
        for (Map.Entry<Long, List<TimeRange>> entry : appToSessionTimeMap.entrySet()) {
            long appInfoId = entry.getKey();
            String packageName;
            try {
                packageName = mAppInfoHelper.getPackageName(appInfoId);
            } catch (PackageManager.NameNotFoundException exception) {
                // This should not happen as we have already filtered out invalid app ids.
                Slog.wtf(TAG, "Package name not found for a pre-filtered appInfoId", exception);
                continue;
            }

            for (TimeRange sessionTimeRange : entry.getValue()) {
                for (Map.Entry<@RecordTypeIdentifier.RecordType Integer, String> dataTypeInfo :
                        INTERVAL_TYPE_ID_TO_TABLE_NAME_MAP.entrySet()) {
                    long granularity =
                            getIntervalGranularityDataForExerciseSessions(
                                    dataTypeInfo.getValue(),
                                    sessionTimeRange.startTime(),
                                    sessionTimeRange.endTime(),
                                    appInfoId);
                    if (granularity == -1L) {
                        continue;
                    }
                    granularityStats.add(
                            new GranularityStats(packageName, dataTypeInfo.getKey(), granularity));
                }
            }
        }
        return granularityStats;
    }

    private List<GranularityStats> getLastWeekSeriesGranularityStats(
            Map<Long, List<TimeRange>> appToSessionTimeMap) {
        List<GranularityStats> granularityStats = new ArrayList<>();
        for (Map.Entry<Long, List<TimeRange>> entry : appToSessionTimeMap.entrySet()) {
            long appInfoId = entry.getKey();
            String packageName;
            try {
                packageName = mAppInfoHelper.getPackageName(appInfoId);
            } catch (PackageManager.NameNotFoundException exception) {
                // This should not happen as we have already filtered out invalid app ids.
                Slog.wtf(TAG, "Package name not found for a pre-filtered appInfoId", exception);
                continue;
            }

            for (TimeRange sessionTimeRange : entry.getValue()) {
                for (Map.Entry<@RecordTypeIdentifier.RecordType Integer, SeriesHelperData>
                        dataTypeInfo : SERIES_TYPE_ID_TO_TABLE_NAME_MAP.entrySet()) {
                    long granularity =
                            getSeriesGranularityDataForExerciseSessions(
                                    dataTypeInfo.getValue(),
                                    sessionTimeRange.startTime(),
                                    sessionTimeRange.endTime(),
                                    appInfoId);
                    if (granularity == -1L) {
                        continue;
                    }
                    granularityStats.add(
                            new GranularityStats(packageName, dataTypeInfo.getKey(), granularity));
                }
            }
        }
        return granularityStats;
    }

    private long getIntervalGranularityDataForExerciseSessions(
            String intervalDataTypeTableName,
            long sessionStartTime,
            long sessionEndTime,
            long appInfoId) {
        WhereClauses whereClause = new WhereClauses(WhereClauses.LogicalOperator.AND);
        whereClause
                .addWhereGreaterThanOrEqualClause(
                        IntervalRecordHelper.START_TIME_COLUMN_NAME, sessionStartTime)
                .addWhereLessThanOrEqualClause(
                        IntervalRecordHelper.END_TIME_COLUMN_NAME, sessionEndTime)
                .addWhereEqualsClause(
                        RecordHelper.APP_INFO_ID_COLUMN_NAME, String.valueOf(appInfoId));

        ReadTableRequest readTableRequest =
                new ReadTableRequest(intervalDataTypeTableName)
                        .setColumnNames(
                                List.of(
                                        IntervalRecordHelper.START_TIME_COLUMN_NAME,
                                        IntervalRecordHelper.END_TIME_COLUMN_NAME))
                        .setWhereClause(whereClause);

        long numberOfRecords = 0L;
        long totalDurationInMillis = 0L;
        try (Cursor cursor = mTransactionManager.read(readTableRequest)) {
            while (cursor.moveToNext()) {
                long startTime = getCursorLong(cursor, IntervalRecordHelper.START_TIME_COLUMN_NAME);
                long endTime = getCursorLong(cursor, IntervalRecordHelper.END_TIME_COLUMN_NAME);
                totalDurationInMillis += (endTime - startTime);
                numberOfRecords++;
            }
        }

        if (numberOfRecords == 0) {
            return -1L;
        }

        // We define granularity as total duration we have interval data for divided by number of
        // records we have for that data type in the given duration.
        return totalDurationInMillis / numberOfRecords;
    }

    private long getSeriesGranularityDataForExerciseSessions(
            SeriesHelperData seriesHelperData,
            long sessionStartTime,
            long sessionEndTime,
            long appInfoId) {

        WhereClauses whereClause = new WhereClauses(WhereClauses.LogicalOperator.AND);
        whereClause
                .addWhereGreaterThanOrEqualClause(
                        SeriesRecordHelper.EPOCH_MILLIS_COLUMN_NAME, sessionStartTime)
                .addWhereLessThanOrEqualClause(
                        SeriesRecordHelper.EPOCH_MILLIS_COLUMN_NAME, sessionEndTime);

        WhereClauses postJoinWhereClause = new WhereClauses(WhereClauses.LogicalOperator.AND);
        postJoinWhereClause.addWhereEqualsClause(
                RecordHelper.APP_INFO_ID_COLUMN_NAME, String.valueOf(appInfoId));

        ReadTableRequest readTableRequest =
                new ReadTableRequest(seriesHelperData.seriesTableName())
                        .setWhereClause(whereClause)
                        .setJoinClause(
                                new SqlJoin(
                                        seriesHelperData.seriesTableName,
                                        seriesHelperData.tableName,
                                        SeriesRecordHelper.PARENT_KEY_COLUMN_NAME,
                                        RecordHelper.PRIMARY_COLUMN_NAME))
                        .setPostJoinWhereClause(postJoinWhereClause);

        int numberOfRecords = mTransactionManager.count(readTableRequest);
        long totalTimeGap = sessionEndTime - sessionStartTime;

        if (numberOfRecords == 0) {
            return -1L;
        }

        // We define granularity by the average time gap between each data point of a series
        // data type for the duration of the session.
        return totalTimeGap / numberOfRecords;
    }

    /** Returns a map of app info id to session time map from reading the sessions table. */
    private Map<Long, List<TimeRange>> getAppToSessionTimeMap(String tableName) {
        Map<Long, List<TimeRange>> appToSessionTimeMap = new HashMap<>();
        ReadTableRequest readTableRequest = getReadLastWeekSessionsRequest(tableName);
        try (Cursor cursor = mTransactionManager.read(readTableRequest)) {
            while (cursor.moveToNext()) {
                long appInfoId = getCursorLong(cursor, RecordHelper.APP_INFO_ID_COLUMN_NAME);
                try {
                    mAppInfoHelper.getPackageName(appInfoId);
                } catch (PackageManager.NameNotFoundException e) {
                    Slog.e(TAG, "Package name not found while retrieving session", e);
                    // Skip if package name not found for the app id.
                    continue;
                }
                long startTime = getCursorLong(cursor, IntervalRecordHelper.START_TIME_COLUMN_NAME);
                long endTime = getCursorLong(cursor, IntervalRecordHelper.END_TIME_COLUMN_NAME);
                appToSessionTimeMap
                        .computeIfAbsent(appInfoId, id -> new ArrayList<>())
                        .add(new TimeRange(startTime, endTime));
            }
        }
        return appToSessionTimeMap;
    }
}
