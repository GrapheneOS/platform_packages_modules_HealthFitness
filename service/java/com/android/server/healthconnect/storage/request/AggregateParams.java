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

package com.android.server.healthconnect.storage.request;

import android.annotation.IntDef;

import com.android.server.healthconnect.storage.utils.SqlJoin;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

/**
 * Collection of parameters of {@link AggregateTableRequest}.
 *
 * @hide
 */
public class AggregateParams {
    private final String mTableName;

    // Column used for time filtering. Start time for interval records.
    private final String mTimeColumnName;
    private final List<String> mColumnsToFetch;
    private SqlJoin mJoin;

    // Additional column used for time filtering. End time for interval records,
    // null for other records.
    private String mExtraTimeColumnName = null;

    private PriorityAggregationExtraParams mPriorityAggregationExtraParams;

    public AggregateParams(String tableName, List<String> columnsToFetch, String timeColumnName) {
        this(tableName, columnsToFetch, timeColumnName, null);
    }

    public AggregateParams(
            String tableName,
            List<String> columnsToFetch,
            String timeColumnName,
            Class<?> priorityColumnDataType) {
        mTableName = tableName;
        mColumnsToFetch = columnsToFetch;
        mTimeColumnName = timeColumnName;

        // TODO(b/277776749): remove dependency on columns orders
        mPriorityAggregationExtraParams =
                new PriorityAggregationExtraParams(columnsToFetch.get(0), priorityColumnDataType);
    }

    public SqlJoin getJoin() {
        return mJoin;
    }

    public String getTableName() {
        return mTableName;
    }

    public String getTimeColumnName() {
        return mTimeColumnName;
    }

    public String getExtraTimeColumnName() {
        return mExtraTimeColumnName;
    }

    public List<String> getColumnsToFetch() {
        return mColumnsToFetch;
    }

    /** Sets join type. */
    public AggregateParams setJoin(SqlJoin join) {
        mJoin = join;
        return this;
    }

    /** Appends additional columns to fetch. */
    public AggregateParams appendAdditionalColumns(List<String> additionColumns) {
        mColumnsToFetch.addAll(additionColumns);
        return this;
    }

    /** Sets params for priority aggregation. */
    public AggregateParams setPriorityAggregationExtraParams(
            PriorityAggregationExtraParams extraParams) {
        mPriorityAggregationExtraParams = extraParams;
        return this;
    }

    /** Returns params for priority aggregation. */
    public PriorityAggregationExtraParams getPriorityAggregationExtraParams() {
        return mPriorityAggregationExtraParams;
    }

    /** Sets params for priority aggregation. */
    public AggregateParams setExtraTimeColumn(String extraTimeColumn) {
        mExtraTimeColumnName = extraTimeColumn;
        return this;
    }

    /** Collections of parameters of priority AggregationRequest. */
    public static class PriorityAggregationExtraParams {

        public static final int VALUE_TYPE_LONG = 0;
        public static final int VALUE_TYPE_DOUBLE = 1;

        /** @hide */
        @IntDef({
            VALUE_TYPE_LONG,
            VALUE_TYPE_DOUBLE,
        })
        @Retention(RetentionPolicy.SOURCE)
        public @interface ValueColumnType {}

        @ValueColumnType private int mColumnToAggregateType;

        private String mColumnToAggregateName;
        private String mExcludeIntervalEndColumnName;
        private String mExcludeIntervalStartColumnName;

        public PriorityAggregationExtraParams(
                String excludeIntervalStartColumnName, String excludeIntervalEndColumnName) {
            mExcludeIntervalStartColumnName = excludeIntervalStartColumnName;
            mExcludeIntervalEndColumnName = excludeIntervalEndColumnName;
        }

        public PriorityAggregationExtraParams(
                String columnToAggregateName, Class<?> aggregationType) {
            mColumnToAggregateName = columnToAggregateName;
            // TODO(b/277776749): use intdef instead of unlimited Class<?>
            mColumnToAggregateType =
                    (aggregationType == Long.class ? VALUE_TYPE_LONG : VALUE_TYPE_DOUBLE);
        }

        public String getExcludeIntervalStartColumnName() {
            return mExcludeIntervalStartColumnName;
        }

        public String getExcludeIntervalEndColumnName() {
            return mExcludeIntervalEndColumnName;
        }

        @ValueColumnType
        public int getColumnToAggregateType() {
            return mColumnToAggregateType;
        }

        public String getColumnToAggregateName() {
            return mColumnToAggregateName;
        }
    }
}
