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

import static android.healthconnect.datatypes.AggregationType.AVG;
import static android.healthconnect.datatypes.AggregationType.MAX;
import static android.healthconnect.datatypes.AggregationType.MIN;
import static android.healthconnect.datatypes.AggregationType.SUM;

import android.annotation.NonNull;
import android.database.Cursor;
import android.healthconnect.AggregateRecordsResponse;
import android.healthconnect.Constants;
import android.healthconnect.datatypes.AggregationType;
import android.util.Slog;

import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.datatypehelpers.RecordHelper;
import com.android.server.healthconnect.storage.utils.SqlJoin;
import com.android.server.healthconnect.storage.utils.WhereClauses;

import java.util.List;
import java.util.Objects;

/**
 * A request for {@link TransactionManager} to query the DB for aggregation results
 *
 * @hide
 */
public class AggregateTableRequest {
    private static final String TAG = "HealthConnectAggregate";
    private final long DEFAULT_TIME = -1;

    private final String mTableName;
    private final List<String> mColumnNames;
    private final AggregationType<?> mAggregationType;
    private final RecordHelper<?> mRecordHelper;
    private List<Long> mPackageFilters;
    private long mStartTime = DEFAULT_TIME;
    private long mEndTime = DEFAULT_TIME;
    private String mPackageColumnName;
    private String mTimeColumnName;
    private SqlJoin mSqlJoin;

    public AggregateTableRequest(
            @NonNull String tableName,
            @NonNull List<String> columnNames,
            @NonNull AggregationType<?> aggregationType,
            @NonNull RecordHelper<?> recordHelper) {
        Objects.requireNonNull(tableName);
        Objects.requireNonNull(columnNames);
        Objects.requireNonNull(aggregationType);
        Objects.requireNonNull(recordHelper);

        mTableName = tableName;
        mColumnNames = columnNames;
        mAggregationType = aggregationType;
        mRecordHelper = recordHelper;
    }

    public AggregateTableRequest setSqlJoin(SqlJoin sqlJoin) {
        mSqlJoin = sqlJoin;
        return this;
    }

    @NonNull
    public AggregationType<?> getDataAggregation() {
        return mAggregationType;
    }

    /** Returns SQL statement to perform aggregation operation */
    @NonNull
    public String getAggregationCommand() {
        final StringBuilder builder = new StringBuilder("SELECT ");
        String aggCommand = getSqlCommandFor(mAggregationType.getAggregateOperationType());

        for (String columnName : mColumnNames) {
            builder.append(aggCommand)
                    .append("(")
                    .append(columnName)
                    .append(")")
                    .append(" as ")
                    .append(columnName)
                    .append(", ");
        }
        builder.setLength(builder.length() - 2); // Remove the last 2 char i.e. ", "
        builder.append(" FROM ").append(mTableName);

        if (mSqlJoin != null) {
            builder.append(mSqlJoin.getInnerJoinClause());
        }

        WhereClauses whereClauses = new WhereClauses();
        whereClauses.addWhereInLongsClause(mPackageColumnName, mPackageFilters);
        if (mStartTime != DEFAULT_TIME || mEndTime != DEFAULT_TIME) {
            whereClauses.addWhereBetweenClause(mTimeColumnName, mStartTime, mEndTime);
        }
        builder.append(whereClauses.get(true));

        if (Constants.DEBUG) {
            Slog.d(TAG, "Aggregation query: " + builder);
        }

        return builder.toString();
    }

    public AggregateTableRequest setPackageFilter(
            List<Long> packageFilters, String packageColumnName) {
        mPackageFilters = packageFilters;
        mPackageColumnName = packageColumnName;

        return this;
    }

    public AggregateTableRequest setTimeFilter(
            long startTime, long endTime, String timeColumnName) {
        // Return if the params will result in no impact on the query
        if (startTime < 0 || endTime < startTime) {
            return this;
        }

        mStartTime = startTime;
        mEndTime = endTime;
        mTimeColumnName = timeColumnName;

        return this;
    }

    public AggregateRecordsResponse.AggregateResult<?> getAggregateResult(Cursor cursor) {
        return mRecordHelper.getAggregateResult(cursor, mAggregationType);
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
            default:
                return null;
        }
    }
}
