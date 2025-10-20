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

import static com.android.server.healthconnect.fitness.recordhelpers.ExerciseSessionRecordHelper.EXERCISE_SESSION_RECORD_TABLE_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.RecordHelper.APP_INFO_ID_COLUMN_NAME;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorLong;
import static com.android.server.healthconnect.telemetry.dataquality.DataQualityUtils.getPackageName;
import static com.android.server.healthconnect.telemetry.dataquality.DataQualityUtils.getReadLastWeekSessionsRequest;

import android.database.Cursor;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.util.Slog;

import com.android.healthfitness.flags.Flags;
import com.android.server.healthconnect.common.metadata.AppInfoHelper;
import com.android.server.healthconnect.fitness.mappings.InternalHealthConnectMappings;
import com.android.server.healthconnect.fitness.recordhelpers.IntervalRecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.RecordHelper;
import com.android.server.healthconnect.fitness.recordhelpers.SeriesRecordHelper;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.request.ReadTableRequest;
import com.android.server.healthconnect.storage.utils.SqlJoin;
import com.android.server.healthconnect.storage.utils.WhereClauses;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Logs Health Connect granularity for various datatypes.
 *
 * @hide
 */
public final class DataGranularityStatsCollector {

    private static final String TAG = "DataGranularityStatsCollector";
    private static final long MILLIS_PER_DAY = 24 * 60 * 60 * 1000L;

    private final TransactionManager mTransactionManager;
    private final AppInfoHelper mAppInfoHelper;
    private final Clock mClock;
    private final List<GranularityStats> mActiveStats = new ArrayList<>();
    private final List<GranularityStats> mPassiveStats = new ArrayList<>();
    private final Map<Long, List<TimeRange>> mAppToSessionTimeMap = new HashMap<>();
    private long mSevenDaysAgoMillis;

    record GranularityStats(
            String packageName,
            @RecordTypeIdentifier.RecordType int recordIdentifier,
            long granularity) {}

    /** A record to hold all granularity stats, categorized by active and passive data. */
    record AllGranularityStats(
            List<GranularityStats> activeStats, List<GranularityStats> passiveStats) {}

    /** Data class to hold start and end time for a session. */
    private record TimeRange(long startTime, long endTime) {}

    private record StatsKey(long appId, boolean isActive) {}

    private record SessionKey(long appId, TimeRange session) {}

    public DataGranularityStatsCollector(
            TransactionManager transactionManager, AppInfoHelper appInfoHelper, Clock clock) {
        mTransactionManager = transactionManager;
        mAppInfoHelper = appInfoHelper;
        mClock = clock;
    }

    /** Returns {@link AllGranularityStats} for given session data type for past week. */
    AllGranularityStats getAllGranularityStatsForLastWeek() {
        if (!Flags.latencyMetricsFlag()) {
            return new AllGranularityStats(Collections.emptyList(), Collections.emptyList());
        }

        // Clear state from any previous runs.
        mActiveStats.clear();
        mPassiveStats.clear();
        mSevenDaysAgoMillis = mClock.instant().minus(7, ChronoUnit.DAYS).toEpochMilli();

        populateAppToSessionTimeMap(EXERCISE_SESSION_RECORD_TABLE_NAME);
        calculateLastWeekIntervalGranularityStats();
        calculateLastWeekSeriesGranularityStats();

        return new AllGranularityStats(
                Collections.unmodifiableList(mActiveStats),
                Collections.unmodifiableList(mPassiveStats));
    }

    private void calculateLastWeekIntervalGranularityStats() {
        for (@RecordTypeIdentifier.RecordType int recordIdentifier : getIntervalTypeIds()) {
            Map<StatsKey, IntervalAggregator> keyToAggregatorMap = new HashMap<>();
            String tableName =
                    InternalHealthConnectMappings.getInstance()
                            .getRecordHelper(recordIdentifier)
                            .getMainTableName();
            ReadTableRequest readTableRequest = getReadIntervalTableRequest(tableName);

            try (Cursor cursor = mTransactionManager.read(readTableRequest)) {
                while (cursor.moveToNext()) {
                    long appId = getCursorLong(cursor, APP_INFO_ID_COLUMN_NAME);
                    long startTime =
                            getCursorLong(cursor, IntervalRecordHelper.START_TIME_COLUMN_NAME);
                    long endTime = getCursorLong(cursor, IntervalRecordHelper.END_TIME_COLUMN_NAME);
                    boolean isActive = isRecordActive(startTime, endTime, appId);

                    StatsKey key = new StatsKey(appId, isActive);
                    keyToAggregatorMap
                            .computeIfAbsent(key, k -> new IntervalAggregator())
                            .accept(startTime, endTime);
                }
            }
            processIntervalGranularityStats(keyToAggregatorMap, recordIdentifier);
        }
    }

    private void calculateLastWeekSeriesGranularityStats() {
        for (@RecordTypeIdentifier.RecordType int recordIdentifier : getSeriesTypeIds()) {
            SeriesRecordHelper<?, ?> helper =
                    (SeriesRecordHelper<?, ?>)
                            InternalHealthConnectMappings.getInstance()
                                    .getRecordHelper(recordIdentifier);
            String tableName = helper.getMainTableName();
            String seriesTableName = helper.getSeriesDataTableName();
            ReadTableRequest readTableRequest =
                    getReadSeriesTableRequest(tableName, seriesTableName);

            Map<SessionKey, Integer> sessionToRecordCountMap = new HashMap<>();
            Map<Long, Map<Integer, List<Long>>> appIdToDailyBucketsMap = new HashMap<>();
            try (Cursor cursor = mTransactionManager.read(readTableRequest)) {
                Slog.d(
                        TAG,
                        "Number of series samples for type "
                                + recordIdentifier
                                + ": "
                                + cursor.getCount());
                while (cursor.moveToNext()) {
                    long appId = getCursorLong(cursor, APP_INFO_ID_COLUMN_NAME);
                    long timestamp =
                            getCursorLong(cursor, SeriesRecordHelper.EPOCH_MILLIS_COLUMN_NAME);

                    // A single data point can contribute to multiple sessions.
                    List<TimeRange> sessions = getActiveSessionTimeRanges(timestamp, appId);
                    for (TimeRange session : sessions) {
                        SessionKey key = new SessionKey(appId, session);
                        sessionToRecordCountMap.putIfAbsent(key, 0);
                        sessionToRecordCountMap.computeIfPresent(key, (k, v) -> v + 1);
                    }
                    // data point not in any session, is a passive data point
                    if (sessions.isEmpty()) {
                        long elapsedMillis = timestamp - mSevenDaysAgoMillis;
                        int bucketIndex = (int) (elapsedMillis / MILLIS_PER_DAY);

                        appIdToDailyBucketsMap
                                .computeIfAbsent(appId, k -> new HashMap<>())
                                .computeIfAbsent(bucketIndex, k -> new ArrayList<>())
                                .add(timestamp);
                    }
                }
            }
            processActiveSeriesStats(sessionToRecordCountMap, recordIdentifier);
            processPassiveSeriesStats(appIdToDailyBucketsMap, recordIdentifier);
        }
    }

    private void processIntervalGranularityStats(
            Map<StatsKey, IntervalAggregator> keyToAggregatorMap,
            @RecordTypeIdentifier.RecordType int recordIdentifier) {
        keyToAggregatorMap.forEach(
                (key, aggregator) -> {
                    if (aggregator.mRecordCount == 0) {
                        return;
                    }
                    Optional<String> packageName = getPackageName(mAppInfoHelper, key.appId());
                    if (packageName.isEmpty()) {
                        return;
                    }
                    // Average duration millis of the records
                    long granularity = aggregator.mTotalDuration / aggregator.mRecordCount;
                    GranularityStats stats =
                            new GranularityStats(packageName.get(), recordIdentifier, granularity);
                    if (key.isActive()) {
                        mActiveStats.add(stats);
                    } else {
                        mPassiveStats.add(stats);
                    }
                });
    }

    private void processActiveSeriesStats(
            Map<SessionKey, Integer> sessionToRecordCountMap,
            @RecordTypeIdentifier.RecordType int recordIdentifier) {
        sessionToRecordCountMap.forEach(
                (key, recordCount) -> {
                    Optional<String> packageName = getPackageName(mAppInfoHelper, key.appId());
                    if (packageName.isEmpty() || recordCount == 0) {
                        return;
                    }

                    long sessionDuration = key.session().endTime() - key.session().startTime();
                    if (sessionDuration <= 0) {
                        return;
                    }

                    // Average time gap between each sample of a series data for the duration of the
                    // session.
                    long granularity = sessionDuration / recordCount;

                    GranularityStats stats =
                            new GranularityStats(packageName.get(), recordIdentifier, granularity);
                    mActiveStats.add(stats);
                });
    }

    private void processPassiveSeriesStats(
            Map<Long, Map<Integer, List<Long>>> appIdToDailyBucketsMap,
            @RecordTypeIdentifier.RecordType int recordIdentifier) {
        appIdToDailyBucketsMap.forEach(
                (appId, dailyBuckets) -> {
                    Optional<String> packageName = getPackageName(mAppInfoHelper, appId);
                    if (packageName.isEmpty()) {
                        return;
                    }
                    dailyBuckets.forEach(
                            (bucketIndex, timestamps) -> {
                                if (timestamps.isEmpty()) {
                                    return;
                                }
                                long granularity;
                                if (timestamps.size() == 1) {
                                    // duration 24 hours / 1 record
                                    granularity = MILLIS_PER_DAY;
                                } else {
                                    Collections.sort(timestamps);
                                    List<Long> passiveGaps = new ArrayList<>();
                                    for (int i = 1; i < timestamps.size(); i++) {
                                        passiveGaps.add(timestamps.get(i) - timestamps.get(i - 1));
                                    }
                                    // for passive data, we use median instead of average to not
                                    // account for data
                                    // unavailable time (e.g. user not wearing device)
                                    granularity = calculateMedian(passiveGaps);
                                }
                                GranularityStats stats =
                                        new GranularityStats(
                                                packageName.get(), recordIdentifier, granularity);
                                mPassiveStats.add(stats);
                            });
                });
    }

    private long calculateMedian(List<Long> values) {
        Collections.sort(values);
        int middle = values.size() / 2;
        return values.size() % 2 == 1
                ? values.get(middle)
                : (values.get(middle - 1) + values.get(middle)) / 2;
    }

    /** Returns a map of app info id to session time map from reading the sessions table. */
    private void populateAppToSessionTimeMap(String tableName) {
        ReadTableRequest readTableRequest =
                getReadLastWeekSessionsRequest(tableName, mClock.instant());
        try (Cursor cursor = mTransactionManager.read(readTableRequest)) {
            while (cursor.moveToNext()) {
                long appInfoId = getCursorLong(cursor, RecordHelper.APP_INFO_ID_COLUMN_NAME);
                if (getPackageName(mAppInfoHelper, appInfoId).isEmpty()) {
                    Slog.e(TAG, "Package name not found while retrieving session");
                    continue;
                }
                long startTime = getCursorLong(cursor, IntervalRecordHelper.START_TIME_COLUMN_NAME);
                long endTime = getCursorLong(cursor, IntervalRecordHelper.END_TIME_COLUMN_NAME);
                mAppToSessionTimeMap
                        .computeIfAbsent(appInfoId, id -> new ArrayList<>())
                        .add(new TimeRange(startTime, endTime));
            }
        }
    }

    private ReadTableRequest getReadIntervalTableRequest(String tableName) {
        List<String> columnNames =
                List.of(
                        APP_INFO_ID_COLUMN_NAME,
                        IntervalRecordHelper.START_TIME_COLUMN_NAME,
                        IntervalRecordHelper.END_TIME_COLUMN_NAME);
        WhereClauses whereClause =
                new WhereClauses(WhereClauses.LogicalOperator.AND)
                        .addWhereLaterThanTimeClause(
                                IntervalRecordHelper.START_TIME_COLUMN_NAME, mSevenDaysAgoMillis);

        return new ReadTableRequest(tableName)
                .setColumnNames(columnNames)
                .setWhereClause(whereClause);
    }

    private ReadTableRequest getReadSeriesTableRequest(String tableName, String seriesTableName) {
        return new ReadTableRequest(seriesTableName)
                .setColumnNames(
                        List.of(
                                tableName + "." + APP_INFO_ID_COLUMN_NAME,
                                SeriesRecordHelper.EPOCH_MILLIS_COLUMN_NAME))
                .setWhereClause(
                        new WhereClauses(WhereClauses.LogicalOperator.AND)
                                .addWhereGreaterThanOrEqualClause(
                                        SeriesRecordHelper.EPOCH_MILLIS_COLUMN_NAME,
                                        mSevenDaysAgoMillis))
                .setJoinClause(
                        new SqlJoin(
                                seriesTableName,
                                tableName,
                                SeriesRecordHelper.PARENT_KEY_COLUMN_NAME,
                                RecordHelper.PRIMARY_COLUMN_NAME));
    }

    private boolean isRecordActive(long startTimeMillis, long endTimeMillis, long appId) {
        if (!mAppToSessionTimeMap.containsKey(appId)) {
            return false;
        }

        for (TimeRange sessionTimeRange : mAppToSessionTimeMap.get(appId)) {
            if (startTimeMillis >= sessionTimeRange.startTime()
                    && endTimeMillis <= sessionTimeRange.endTime()) {
                return true;
            }
        }

        return false;
    }

    private List<TimeRange> getActiveSessionTimeRanges(long timestamp, long appId) {
        if (!mAppToSessionTimeMap.containsKey(appId)) {
            return Collections.emptyList();
        }

        List<TimeRange> matchingSessions = new ArrayList<>();
        for (TimeRange sessionTimeRange : mAppToSessionTimeMap.get(appId)) {
            if (timestamp >= sessionTimeRange.startTime()
                    && timestamp <= sessionTimeRange.endTime()) {
                matchingSessions.add(sessionTimeRange);
            }
        }

        return Collections.unmodifiableList(matchingSessions);
    }

    private List<Integer> getSeriesTypeIds() {
        return InternalHealthConnectMappings.getInstance().getRecordHelpers().stream()
                .filter(recordHelper -> recordHelper instanceof SeriesRecordHelper<?, ?>)
                .map(RecordHelper::getRecordIdentifier)
                .filter(
                        recordIdentifier ->
                                InternalHealthConnectMappings.getInstance()
                                        .supportsGranularityLogging(recordIdentifier))
                .collect(Collectors.toList());
    }

    private List<Integer> getIntervalTypeIds() {
        return InternalHealthConnectMappings.getInstance().getRecordHelpers().stream()
                .filter(recordHelper -> recordHelper instanceof IntervalRecordHelper<?>)
                .filter(recordHelper -> !(recordHelper instanceof SeriesRecordHelper<?, ?>))
                .map(RecordHelper::getRecordIdentifier)
                .filter(
                        recordIdentifier ->
                                InternalHealthConnectMappings.getInstance()
                                        .supportsGranularityLogging(recordIdentifier))
                .collect(Collectors.toList());
    }

    private static final class IntervalAggregator {
        private long mTotalDuration = 0;
        private long mRecordCount = 0;

        private void accept(long startTime, long endTime) {
            mTotalDuration += (endTime - startTime);
            mRecordCount++;
        }
    }
}
