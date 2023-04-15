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

package com.android.server.healthconnect.storage.request;

import static android.health.connect.datatypes.AggregationType.AVG;
import static android.health.connect.datatypes.AggregationType.COUNT;
import static android.health.connect.datatypes.AggregationType.MAX;
import static android.health.connect.datatypes.AggregationType.MIN;
import static android.health.connect.datatypes.AggregationType.SUM;

import static com.android.server.healthconnect.storage.datatypehelpers.RecordHelper.APP_INFO_ID_COLUMN_NAME;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.database.Cursor;
import android.health.connect.AggregateResult;
import android.health.connect.Constants;
import android.health.connect.TimeRangeFilter;
import android.health.connect.TimeRangeFilterHelper;
import android.health.connect.datatypes.AggregationType;
import android.util.ArrayMap;
import android.util.Slog;

import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.datatypehelpers.AppInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.RecordHelper;
import com.android.server.healthconnect.storage.datatypehelpers.aggregation.PriorityRecordsAggregator;
import com.android.server.healthconnect.storage.utils.OrderByClause;
import com.android.server.healthconnect.storage.utils.SqlJoin;
import com.android.server.healthconnect.storage.utils.StorageUtils;
import com.android.server.healthconnect.storage.utils.WhereClauses;

import java.time.Duration;
import java.time.Instant;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A request for {@link TransactionManager} to query the DB for aggregation results
 *
 * @hide
 */
public class AggregateTableRequest {
    private static final String TAG = "HealthConnectAggregate";
    private static final String GROUP_BY_COLUMN_NAME = "category";

    private final long DEFAULT_TIME = -1;
    private final String mTableName;
    private final List<String> mColumnNamesToAggregate;
    private final AggregationType<?> mAggregationType;
    private final RecordHelper<?> mRecordHelper;
    private final Map<Integer, AggregateResult> mAggregateResults = new ArrayMap<>();
    private final String mTimeColumnName;
    // Additional column used for time filtering. End time for interval records,
    // null for other records.
    private String mEndTimeColumnName;
    private final SqlJoin mSqlJoin;
    private List<Long> mPackageFilters;
    private long mStartTime = DEFAULT_TIME;
    private long mEndTime = DEFAULT_TIME;
    private String mPackageColumnName;
    private long mGroupByStart;
    private long mGroupByDelta;
    private String mGroupByColumnName;
    private long mGroupByEnd;
    private int mGroupBySize = 1;
    private List<String> mAdditionalColumnsToFetch;
    private final AggregateParams.PriorityAggregationExtraParams mPriorityParams;

    public AggregateTableRequest(
            AggregateParams params, AggregationType aggregationType, RecordHelper recordHelper) {
        mTableName = params.getTableName();
        mColumnNamesToAggregate = params.getColumnsToFetch();
        mTimeColumnName = params.getTimeColumnName();
        mAggregationType = aggregationType;
        mRecordHelper = recordHelper;
        mSqlJoin = params.getJoin();
        mPriorityParams = params.getPriorityAggregationExtraParams();
        mEndTimeColumnName = params.getExtraTimeColumnName();
    }

    /**
     * @param additionalColumnsToFetch Additional columns to fetch for the matching record
     */
    public AggregateTableRequest setAdditionalColumnsToFetch(
            @Nullable List<String> additionalColumnsToFetch) {
        mAdditionalColumnsToFetch = additionalColumnsToFetch;
        return this;
    }

    /**
     * @return {@link AggregationType} for this request
     */
    public AggregationType<?> getAggregationType() {
        return mAggregationType;
    }

    /**
     * @return {@link RecordHelper} for this request
     */
    public RecordHelper<?> getRecordHelper() {
        return mRecordHelper;
    }

    /**
     * @return results fetched after performing aggregate operation for this class.
     *     <p>Note: Only available after the call to {@link
     *     TransactionManager#populateWithAggregation} has been made
     */
    public List<AggregateResult<?>> getAggregateResults() {
        List<AggregateResult<?>> aggregateResults = new ArrayList<>(mGroupBySize);
        for (int i = 0; i < mGroupBySize; i++) {
            aggregateResults.add(mAggregateResults.get(i));
        }

        return aggregateResults;
    }

    /** Returns SQL statement to get data origins for the aggregation operation */
    public String getCommandToFetchAggregateMetadata() {
        final StringBuilder builder = new StringBuilder("SELECT DISTINCT ");
        builder.append(APP_INFO_ID_COLUMN_NAME).append(", ");
        return appendAggregateCommand(builder, true);
    }

    /** Returns SQL statement to perform aggregation operation */
    @NonNull
    public String getAggregationCommand() {
        final StringBuilder builder = new StringBuilder("SELECT ");
        String aggCommand;
        boolean usingPriority =
                StorageUtils.supportsPriority(
                                mRecordHelper.getRecordIdentifier(),
                                mAggregationType.getAggregateOperationType())
                        || StorageUtils.isDerivedType(mRecordHelper.getRecordIdentifier());
        if (usingPriority) {
            for (String columnName : mColumnNamesToAggregate) {
                builder.append(columnName).append(", ");
            }
        } else {
            aggCommand = getSqlCommandFor(mAggregationType.getAggregateOperationType());

            for (String columnName : mColumnNamesToAggregate) {
                builder.append(aggCommand)
                        .append("(")
                        .append(columnName)
                        .append(")")
                        .append(" as ")
                        .append(columnName)
                        .append(", ");
            }
        }

        if (mAdditionalColumnsToFetch != null) {
            for (String additionalColumnToFetch : mAdditionalColumnsToFetch) {
                builder.append(additionalColumnToFetch).append(", ");
            }
        }

        return appendAggregateCommand(builder, usingPriority);
    }

    public AggregateTableRequest setPackageFilter(
            List<Long> packageFilters, String packageColumnName) {
        mPackageFilters = packageFilters;
        mPackageColumnName = packageColumnName;

        return this;
    }

    /** Sets time filter for table request. */
    public AggregateTableRequest setTimeFilter(long startTime, long endTime) {
        // Return if the params will result in no impact on the query
        if (startTime < 0 || endTime < startTime) {
            return this;
        }

        mStartTime = startTime;
        mEndTime = endTime;
        return this;
    }

    /** Sets group by fields. */
    public void setGroupBy(String columnName, Period period, TimeRangeFilter timeRangeFilter) {
        mGroupByColumnName = columnName;
        mGroupByStart = TimeRangeFilterHelper.getPeriodStart(timeRangeFilter);
        mGroupByDelta = StorageUtils.getPeriodDelta(period);
        mGroupByEnd = TimeRangeFilterHelper.getPeriodEnd(timeRangeFilter);
        setGroupBySize();
    }

    /** Sets group by fields. */
    public void setGroupBy(String columnName, Duration duration, TimeRangeFilter timeRangeFilter) {
        mGroupByColumnName = columnName;
        mGroupByStart = TimeRangeFilterHelper.getDurationStart(timeRangeFilter);
        mGroupByDelta = StorageUtils.getDurationDelta(duration);
        mGroupByEnd = TimeRangeFilterHelper.getDurationEnd(timeRangeFilter);
        setGroupBySize();
    }

    public void onResultsFetched(Cursor cursor, Cursor metaDataCursor) {
        if (StorageUtils.isDerivedType(mRecordHelper.getRecordIdentifier())) {
            deriveAggregate(cursor);
        } else if (StorageUtils.supportsPriority(
                mRecordHelper.getRecordIdentifier(),
                mAggregationType.getAggregateOperationType())) {
            processPriorityRequest(cursor);
        } else {
            processNoPrioritiesRequest(cursor);
        }

        updateResultWithDataOriginPackageNames(metaDataCursor);
    }

    private void processPriorityRequest(Cursor cursor) {
        List<Long> priorityList =
                StorageUtils.getAppIdPriorityList(mRecordHelper.getRecordIdentifier());
        PriorityRecordsAggregator aggregator =
                new PriorityRecordsAggregator(
                        getGroupSplits(),
                        priorityList,
                        mAggregationType.getAggregationTypeIdentifier(),
                        mPriorityParams);
        aggregator.calculateAggregation(cursor);
        AggregateResult<?> result;
        for (int groupNumber = 0; groupNumber < mGroupBySize; groupNumber++) {
            if (aggregator.getResultForGroup(groupNumber) == null) {
                continue;
            }

            if (mAggregationType.getAggregateResultClass() == Long.class) {
                result =
                        new AggregateResult<>(
                                aggregator.getResultForGroup(groupNumber).longValue());
            } else {
                result = new AggregateResult<>(aggregator.getResultForGroup(groupNumber));
            }
            mAggregateResults.put(
                    groupNumber,
                    result.setZoneOffset(aggregator.getZoneOffsetForGroup(groupNumber)));
        }

        if (Constants.DEBUG) {
            Slog.d(TAG, "Priority aggregation result: " + mAggregateResults);
        }
    }

    private void processNoPrioritiesRequest(Cursor cursor) {
        while (cursor.moveToNext()) {
            mAggregateResults.put(
                    StorageUtils.getCursorInt(cursor, GROUP_BY_COLUMN_NAME),
                    mRecordHelper.getAggregateResult(cursor, mAggregationType));
        }
    }

    private void setGroupBySize() {
        mGroupBySize = (int) ((mGroupByEnd - mGroupByStart) / mGroupByDelta);
    }

    private static String getSqlCommandFor(@AggregationType.AggregateOperationType int type) {
        switch (type) {
            case MAX:
                return "MAX";
            case MIN:
                return "MIN";
            case AVG:
                return "AVG";
            case SUM:
                return "SUM";
            case COUNT:
                return "COUNT";
            default:
                return null;
        }
    }

    private String appendAggregateCommand(StringBuilder builder, boolean isMetadata) {
        boolean useGroupBy = mGroupByColumnName != null && !isMetadata;
        if (useGroupBy) {
            builder.append(" CASE ");
            int groupByIndex = 0;
            for (long i = mGroupByStart; i < mGroupByEnd; i += mGroupByDelta) {
                builder.append(" WHEN ")
                        .append(mGroupByColumnName)
                        .append(" >= ")
                        .append(i)
                        .append(" AND ")
                        .append(mGroupByColumnName)
                        .append(" < ")
                        .append(i + mGroupByDelta)
                        .append(" THEN ")
                        .append(groupByIndex++);
            }
            builder.append(" END " + GROUP_BY_COLUMN_NAME + " ");
        } else {
            builder.setLength(builder.length() - 2); // Remove the last 2 char i.e. ", "
        }

        builder.append(" FROM ").append(mTableName);
        if (mSqlJoin != null) {
            builder.append(mSqlJoin.getJoinCommand());
        }

        builder.append(buildAggregationWhereCondition());

        if (useGroupBy) {
            builder.append(" GROUP BY " + GROUP_BY_COLUMN_NAME);
        }

        OrderByClause orderByClause = new OrderByClause();
        orderByClause.addOrderByClause(mTimeColumnName, true);
        builder.append(orderByClause.getOrderBy());

        if (Constants.DEBUG) {
            Slog.d(TAG, "Aggregation origin query: " + builder);
        }

        return builder.toString();
    }

    private String buildAggregationWhereCondition() {
        WhereClauses whereClauses = new WhereClauses();
        whereClauses.addWhereInLongsClause(mPackageColumnName, mPackageFilters);

        if (mEndTimeColumnName != null) {
            // Filter all records which overlap with time filter interval:
            // recordStartTime < filterEndTime and recordEndTime > filterStartTime
            whereClauses.addWhereLessThanClause(mTimeColumnName, mEndTime);
            whereClauses.addWhereGreaterThanClause(mEndTimeColumnName, mStartTime);
        } else {
            whereClauses.addWhereBetweenClause(mTimeColumnName, mStartTime, mEndTime);
        }

        return whereClauses.get(/* withWhereKeyword= */ true);
    }

    private void updateResultWithDataOriginPackageNames(Cursor metaDataCursor) {
        List<Long> packageIds = new ArrayList<>();
        while (metaDataCursor.moveToNext()) {
            packageIds.add(StorageUtils.getCursorLong(metaDataCursor, APP_INFO_ID_COLUMN_NAME));
        }
        List<String> packageNames = AppInfoHelper.getInstance().getPackageNames(packageIds);

        mAggregateResults.replaceAll(
                (n, v) -> mAggregateResults.get(n).setDataOrigins(packageNames));
    }

    private List<Long> getGroupSplits() {
        long currentStart = mGroupByStart;
        List<Long> splits = new ArrayList<>();
        splits.add(currentStart);
        long currentEnd = getGroupEndTime(currentStart);
        while (currentEnd <= mEndTime) {
            splits.add(currentEnd);
            currentStart = currentEnd;
            currentEnd = getGroupEndTime(currentStart);
        }
        return splits;
    }

    private long getGroupEndTime(long groupStartTime) {
        if (mGroupByColumnName.equals(mRecordHelper.getPeriodGroupByColumnName())) {
            // Calculate and return start time for group Aggregation based on period
            return (Instant.ofEpochMilli(groupStartTime).plus(mGroupByDelta, ChronoUnit.DAYS))
                    .toEpochMilli();
        } else {
            // Calculate and return end time for group Aggregation based on duration
            return groupStartTime + mGroupByDelta;
        }
    }

    private void deriveAggregate(Cursor cursor) {
        double[] derivedAggregateArray =
                mRecordHelper.deriveAggregate(
                        cursor,
                        mStartTime,
                        mEndTime,
                        mGroupBySize,
                        mGroupByDelta,
                        mGroupByColumnName);
        int index = 0;
        cursor.moveToFirst();
        for (double aggregate : derivedAggregateArray) {
            mAggregateResults.put(
                    index, mRecordHelper.getAggregateResult(cursor, mAggregationType, aggregate));
            index++;
        }
    }

}
