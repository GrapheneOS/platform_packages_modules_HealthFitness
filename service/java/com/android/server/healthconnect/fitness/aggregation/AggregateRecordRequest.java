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

package com.android.server.healthconnect.fitness.aggregation;

import static android.health.connect.datatypes.AggregationType.AVG;
import static android.health.connect.datatypes.AggregationType.COUNT;
import static android.health.connect.datatypes.AggregationType.MAX;
import static android.health.connect.datatypes.AggregationType.MIN;
import static android.health.connect.datatypes.AggregationType.SUM;

import static com.android.server.healthconnect.fitness.recordhelpers.RecordHelper.APP_INFO_ID_COLUMN_NAME;

import android.database.Cursor;
import android.health.connect.AggregateResult;
import android.health.connect.Constants;
import android.health.connect.datatypes.AggregationType;
import android.health.connect.datatypes.DataOrigin;
import android.util.ArrayMap;
import android.util.Pair;
import android.util.Slog;

import com.android.server.healthconnect.fitness.helpers.HealthDataCategoryPriorityHelper;
import com.android.server.healthconnect.fitness.mappings.InternalHealthConnectMappings;
import com.android.server.healthconnect.fitness.recordhelpers.RecordHelper;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.datatypehelpers.AppInfoHelper;
import com.android.server.healthconnect.storage.utils.OrderByClause;
import com.android.server.healthconnect.storage.utils.SqlJoin;
import com.android.server.healthconnect.storage.utils.StorageUtils;
import com.android.server.healthconnect.storage.utils.WhereClauses;

import java.time.Duration;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A request for {@link TransactionManager} to query the DB for aggregation results
 *
 * @hide
 */
public class AggregateRecordRequest {
    private static final String TAG = "HealthConnectAggregate";
    private static final String GROUP_BY_COLUMN_NAME = "category";

    private final String mTableName;
    private final List<String> mColumnNamesToAggregate;
    private final AggregationType<?> mAggregationType;
    private final RecordHelper<?> mRecordHelper;

    /**
     * Represents "start time" for interval record, and "time" for instant record.
     *
     * <p>{@link #mUseLocalTime} is already taken into account when this field is set, meaning if
     * {@link #mUseLocalTime} is {@code true}, then this field represent local time, otherwise
     * physical time.
     */
    private final String mTimeColumnName;

    private final WhereClauses mWhereClauses;
    private final SqlJoin mSqlJoin;
    private final List<String> mAdditionalColumnsToFetch;
    private final AggregateParams.PriorityAggregationExtraParams mPriorityParams;
    private final boolean mUseLocalTime;
    private final HealthDataCategoryPriorityHelper mHealthDataCategoryPriorityHelper;
    private final InternalHealthConnectMappings mInternalHealthConnectMappings;
    private final AppInfoHelper mAppInfoHelper;
    private final TransactionManager mTransactionManager;
    private final TimeSplits mTimeSplits;

    public AggregateRecordRequest(
            AggregateParams params,
            AggregationType<?> aggregationType,
            RecordHelper<?> recordHelper,
            WhereClauses whereClauses,
            HealthDataCategoryPriorityHelper healthDataCategoryPriorityHelper,
            InternalHealthConnectMappings internalHealthConnectMappings,
            AppInfoHelper appInfoHelper,
            TransactionManager transactionManager,
            boolean useLocalTime,
            TimeSplits timeSplits) {
        mTableName = params.getTableName();
        mColumnNamesToAggregate = params.getColumnsToFetch();
        mTimeColumnName = params.getTimeColumnName();
        mAggregationType = aggregationType;
        mRecordHelper = recordHelper;
        mSqlJoin = params.getJoin();
        mPriorityParams = params.getPriorityAggregationExtraParams();
        mWhereClauses = whereClauses;
        mAdditionalColumnsToFetch = new ArrayList<>();
        mAdditionalColumnsToFetch.add(params.getTimeOffsetColumnName());
        mAdditionalColumnsToFetch.add(mTimeColumnName);
        String endTimeColumnName = params.getExtraTimeColumnName();
        if (endTimeColumnName != null) {
            mAdditionalColumnsToFetch.add(endTimeColumnName);
        }
        mUseLocalTime = useLocalTime;
        mHealthDataCategoryPriorityHelper = healthDataCategoryPriorityHelper;
        mInternalHealthConnectMappings = internalHealthConnectMappings;
        mAppInfoHelper = appInfoHelper;
        mTransactionManager = transactionManager;
        mTimeSplits = timeSplits;
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

    /** {@return results fetched after performing aggregate operation for this class}. */
    private List<AggregateResult<?>> getAggregateResults(
            ArrayMap<Integer, AggregateResult<?>> results) {
        List<AggregateResult<?>> aggregateResults = new ArrayList<>(mTimeSplits.size());
        for (int i = 0; i < mTimeSplits.size(); i++) {
            aggregateResults.add(results.get(i));
        }

        return aggregateResults;
    }

    /** Returns SQL statement to get data origins for the aggregation operation */
    public String getCommandToFetchAggregateMetadata() {
        final StringBuilder builder = new StringBuilder("SELECT DISTINCT ");
        builder.append(APP_INFO_ID_COLUMN_NAME).append(", ");
        return appendAggregateCommand(builder, /* isMetadata= */ true);
    }

    /** Returns name of the main time column (start time for Interval, time for Instant records) */
    public String getTimeColumnName() {
        return mTimeColumnName;
    }

    /** Returns whether request is using local time instead of physical one. */
    public boolean getUseLocalTime() {
        return mUseLocalTime;
    }

    /** Returns SQL statement to perform aggregation operation */
    public String getAggregationCommand() {
        final StringBuilder builder = new StringBuilder("SELECT ");
        String aggCommand;
        boolean usingPriority =
                mInternalHealthConnectMappings.supportsPriority(
                                mRecordHelper.getRecordIdentifier(),
                                mAggregationType.getAggregateOperationType())
                        || mInternalHealthConnectMappings.isDerivedType(
                                mRecordHelper.getRecordIdentifier());
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

    /** Returns the result of the aggregation. */
    public List<AggregateResult<?>> processResults(
            Cursor cursor, List<String> dataOriginsPackageNames) {
        Set<DataOrigin> dataOrigins = AggregateResult.convertDataOrigins(dataOriginsPackageNames);
        ArrayMap<Integer, AggregateResult<?>> results;
        if (mInternalHealthConnectMappings.isDerivedType(mRecordHelper.getRecordIdentifier())) {
            results = deriveAggregate(cursor, dataOrigins);
        } else if (mInternalHealthConnectMappings.supportsPriority(
                mRecordHelper.getRecordIdentifier(),
                mAggregationType.getAggregateOperationType())) {
            results = processPriorityRequest(cursor, dataOrigins);
        } else {
            results = processNoPrioritiesRequest(cursor, dataOrigins);
        }
        return getAggregateResults(results);
    }

    /** Returns list of app Ids of contributing apps for the record type in the priority order */
    public List<Long> getAppIdPriorityList(int recordType) {
        return mHealthDataCategoryPriorityHelper.getAppIdPriorityOrder(
                mInternalHealthConnectMappings
                        .getExternalMappings()
                        .getRecordCategoryForRecordType(recordType));
    }

    private ArrayMap<Integer, AggregateResult<?>> processPriorityRequest(
            Cursor cursor, Set<DataOrigin> dataOrigins) {
        List<Long> priorityList = getAppIdPriorityList(mRecordHelper.getRecordIdentifier());
        PriorityRecordsAggregator aggregator =
                new PriorityRecordsAggregator(
                        mTimeSplits.getSplits(),
                        priorityList,
                        mAggregationType.getAggregationTypeIdentifier(),
                        mPriorityParams,
                        mUseLocalTime);
        aggregator.calculateAggregation(cursor);
        ArrayMap<Integer, AggregateResult<?>> results = new ArrayMap<>(mTimeSplits.size());
        AggregateResult<?> result;
        for (int groupNumber = 0; groupNumber < mTimeSplits.size(); groupNumber++) {
            Double resultForGroup = aggregator.getResultForGroup(groupNumber);
            if (resultForGroup == null) {
                continue;
            }
            ZoneOffset zoneOffsetForGroup = aggregator.getZoneOffsetForGroup(groupNumber);

            if (mAggregationType.getAggregateResultClass() == Long.class
                    || mAggregationType.getAggregateResultClass() == Duration.class) {
                result =
                        new AggregateResult<>(
                                resultForGroup.longValue(), zoneOffsetForGroup, dataOrigins);
            } else {
                result = new AggregateResult<>(resultForGroup, zoneOffsetForGroup, dataOrigins);
            }
            results.put(groupNumber, result);
        }

        if (Constants.DEBUG) {
            Slog.d(TAG, "Priority aggregation result: " + results);
        }
        return results;
    }

    private ArrayMap<Integer, AggregateResult<?>> processNoPrioritiesRequest(
            Cursor cursor, Set<DataOrigin> dataOrigins) {
        ArrayMap<Integer, AggregateResult<?>> results = new ArrayMap<>();
        while (cursor.moveToNext()) {
            results.put(
                    StorageUtils.getCursorInt(cursor, GROUP_BY_COLUMN_NAME),
                    mRecordHelper.getNoPriorityAggregateResult(
                            cursor, mAggregationType, dataOrigins));
        }
        return results;
    }

    @SuppressWarnings("NullAway") // TODO(b/317029272): fix this suppression
    private static String getSqlCommandFor(@AggregationType.AggregateOperationType int type) {
        return switch (type) {
            case MAX -> "MAX";
            case MIN -> "MIN";
            case AVG -> "AVG";
            case SUM -> "SUM";
            case COUNT -> "COUNT";
            default -> null;
        };
    }

    private String appendAggregateCommand(StringBuilder builder, boolean isMetadata) {
        boolean useGroupBy = mTimeSplits.shouldGroupBy() && !isMetadata;
        if (useGroupBy) {
            builder.append(" CASE ");
            int groupByIndex = 0;
            List<Pair<Long, Long>> intervals = mTimeSplits.getIntervals();
            for (Pair<Long, Long> interval : intervals) {
                builder.append(" WHEN ")
                        .append(mTimeColumnName)
                        .append(" >= ")
                        .append(interval.first)
                        .append(" AND ")
                        .append(mTimeColumnName)
                        .append(" < ")
                        .append(interval.second)
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

        builder.append(mWhereClauses.get(/* withWhereKeyword= */ true));

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

    /** Returns the names of the packages that contributed to this aggregation result. */
    public List<String> getDataOriginPackageNames(Cursor metaDataCursor) {
        List<Long> packageIds = new ArrayList<>();
        List<Long> priorityList = getAppIdPriorityList(mRecordHelper.getRecordIdentifier());
        boolean supportsPriority =
                (mInternalHealthConnectMappings.supportsPriority(
                        mRecordHelper.getRecordIdentifier(),
                        mAggregationType.getAggregateOperationType()));
        while (metaDataCursor.moveToNext()) {
            long packageId = StorageUtils.getCursorLong(metaDataCursor, APP_INFO_ID_COLUMN_NAME);

            // As there is currently no way for us to tell which sources have been included in
            // an aggregation, we filter the package names included in the data origins list to
            // exclude those that are not in the priority list for data types that support
            // aggregations based on priority, which cannot have been included in the aggregation.
            // This is a partial fix for b/336783737, which would require a larger refactor to fix
            // fully.
            if (!supportsPriority || priorityList.contains(packageId)) {
                packageIds.add(packageId);
            }
        }
        List<String> packageNames = mAppInfoHelper.getPackageNames(packageIds);
        return packageNames;
    }

    public List<Pair<Long, Long>> getGroupSplitIntervals() {
        return mTimeSplits.getIntervals();
    }

    private ArrayMap<Integer, AggregateResult<?>> deriveAggregate(
            Cursor cursor, Set<DataOrigin> dataOrigins) {
        double[] derivedAggregateArray =
                mRecordHelper.deriveAggregate(cursor, this, mTransactionManager);
        int index = 0;
        cursor.moveToFirst();
        ArrayMap<Integer, AggregateResult<?>> results = new ArrayMap<>();
        for (double aggregate : derivedAggregateArray) {
            results.put(
                    index,
                    mRecordHelper.getDerivedAggregateResult(
                            cursor, mAggregationType, aggregate, dataOrigins));
            index++;
        }
        return results;
    }

    public int getRecordTypeId() {
        return mRecordHelper.getRecordIdentifier();
    }
}
