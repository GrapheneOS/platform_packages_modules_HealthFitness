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

import static com.android.server.healthconnect.storage.utils.StorageUtils.DELIMITER;
import static com.android.server.healthconnect.storage.utils.StorageUtils.DISTINCT;
import static com.android.server.healthconnect.storage.utils.StorageUtils.FROM;
import static com.android.server.healthconnect.storage.utils.StorageUtils.LIMIT_SIZE;
import static com.android.server.healthconnect.storage.utils.StorageUtils.SELECT;
import static com.android.server.healthconnect.storage.utils.StorageUtils.SELECT_ALL;
import static com.android.server.healthconnect.storage.utils.WhereClauses.LogicalOperator.AND;

import android.annotation.Nullable;
import android.annotation.StringDef;
import android.health.connect.Constants;
import android.util.Slog;

import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.utils.OrderByClause;
import com.android.server.healthconnect.storage.utils.SqlJoin;
import com.android.server.healthconnect.storage.utils.WhereClauses;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A request for {@link TransactionManager} to read the DB
 *
 * @hide
 */
public class ReadTableRequest {
    private static final String TAG = "HealthConnectRead";
    public static final String UNION_ALL = " UNION ALL ";
    public static final String UNION = " UNION ";

    /** @hide */
    @StringDef(
            value = {
                UNION, UNION_ALL,
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface UnionType {}

    private final String mTableName;
    @Nullable private List<String> mColumnNames;
    @Nullable private SqlJoin mJoinClause;
    private WhereClauses mWhereClauses = new WhereClauses(AND);
    private WhereClauses mPostJoinWhereClauses = new WhereClauses(AND);
    private boolean mDistinct = false;
    private OrderByClause mOrderByClause = new OrderByClause();
    private OrderByClause mFinalOrderByClause = new OrderByClause();

    // Null means no limit.
    @Nullable private Integer mLimit = null;
    @Nullable private Integer mFinalLimit = null;
    @Nullable private List<ReadTableRequest> mExtraReadRequests;
    @Nullable private List<ReadTableRequest> mUnionReadRequests;
    private String mUnionType = UNION_ALL;

    public ReadTableRequest(String tableName) {
        mTableName = tableName;
    }

    /** Sets the column names to select. */
    public ReadTableRequest setColumnNames(List<String> columnNames) {
        mColumnNames = columnNames;
        return this;
    }

    /** Sets the WHERE clause to use in this SELECT. */
    public ReadTableRequest setWhereClause(WhereClauses whereClauses) {
        mWhereClauses = whereClauses;
        return this;
    }

    /** Sets the final WHERE clause to use in this SELECT i.e. after join. */
    public ReadTableRequest setPostJoinWhereClause(WhereClauses whereClauses) {
        mPostJoinWhereClauses = whereClauses;
        return this;
    }

    /** Used to set Join Clause for the read query */
    public ReadTableRequest setJoinClause(@Nullable SqlJoin joinClause) {
        mJoinClause = joinClause;
        return this;
    }

    /**
     * Use this method to enable the Distinct clause in the read command.
     *
     * <p><b>NOTE: make sure to use the {@link ReadTableRequest#setColumnNames(List)} to set the
     * column names to be used as the selection args.</b>
     */
    public ReadTableRequest setDistinctClause(boolean isDistinctValuesRequired) {
        mDistinct = isDistinctValuesRequired;
        return this;
    }

    /**
     * Returns this {@link ReadTableRequest} with union type set. If not set, the default uses
     * {@link ReadTableRequest#UNION_ALL}.
     */
    public ReadTableRequest setUnionType(@UnionType String unionType) {
        Objects.requireNonNull(unionType);
        mUnionType = unionType;
        return this;
    }

    /**
     * Returns SQL statement to perform read operation.
     *
     * @throws IllegalArgumentException if the {@link ReadTableRequest} does not have a join clause
     *     and has both a {@link #setOrderBy} and {@link #setFinalOrderBy}, or if it does not have a
     *     join clause but has both a {@link #setLimit} and {@link #setFinalLimit} as this would
     *     lead to an invalid query.
     */
    public String getReadCommand() {
        return getReadCommand(/* asCount= */ false);
    }

    /**
     * Returns an SQL statement that performs a count of the number of items that would be returned
     * by the read operation.
     *
     * <p>The SQL result will have a single row, single column with the count of rows as the integer
     * value in that first row, first column.
     *
     * @throws IllegalArgumentException if the {@link ReadTableRequest} does not have a join clause
     *     and has both a {@link #setOrderBy} and {@link #setFinalOrderBy}, or if it does not have a
     *     join clause but has both a {@link #setLimit} and {@link #setFinalLimit} as this would
     *     lead to an invalid query.
     */
    public String getCountCommand() {
        return getReadCommand(/* asCount= */ true);
    }

    /**
     * Returns the SQL for this request.
     *
     * @param asCount if true, the SQL returns the count of the results, if false returns the
     *     results
     */
    private String getReadCommand(boolean asCount) {
        if (mUnionReadRequests != null && !mUnionReadRequests.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            if (asCount) {
                builder.append("SELECT COUNT(*) FROM (");
            }
            for (ReadTableRequest unionReadRequest : mUnionReadRequests) {
                builder.append("SELECT * FROM (");
                builder.append(unionReadRequest.getReadCommand());
                builder.append(")");
                builder.append(mUnionType);
            }
            // For a union request we have to do the count outside the query.
            builder.append(getReadQuery(/* asCount= */ false));
            if (asCount) {
                builder.append(")");
            }
            return builder.toString();
        } else {
            return getReadQuery(asCount);
        }
    }

    /**
     * Returns the SQL for this request, ignoring union read requests.
     *
     * @param asCount if true, the SQL returns the count of the results, if false returns the
     *     results
     */
    private String getReadQuery(boolean asCount) {
        String selectStatement = buildSelectStatement(asCount);

        String readQuery;
        if (mJoinClause != null) {
            String innerQuery = buildReadQuery(SELECT_ALL);
            readQuery = mJoinClause.getJoinWithQueryCommand(selectStatement, innerQuery);
            readQuery = addPostJoinWhereClauses(readQuery);
        } else {
            if (!mOrderByClause.getOrderBy().isEmpty()
                    && !mFinalOrderByClause.getOrderBy().isEmpty()) {
                throw new IllegalArgumentException(
                        "Without a join clause only one of orderByClause or finalOrderByClause may "
                                + "be set");
            }
            if (mLimit != null && mFinalLimit != null) {
                throw new IllegalArgumentException(
                        "Without a join clause only one of the limit or finalLimit may be set");
            }
            readQuery = buildReadQuery(selectStatement);
        }

        readQuery = appendFinalOrderByAndLimit(readQuery);

        if (Constants.DEBUG) {
            Slog.d(TAG, "read query: " + readQuery);
        }
        return readQuery;
    }

    private String buildSelectStatement(boolean asCount) {
        StringBuilder selectStatement = new StringBuilder(SELECT);
        if (asCount) {
            selectStatement.append("COUNT(");
        }
        if (mDistinct) {
            selectStatement.append(DISTINCT);
        }
        // If we have distinct over multiple columns, or no count we need the column names.
        // COUNT(*) differs from COUNT(column) in that it only counts non-null values. However,
        // a select query will return null values. Therefore, we need COUNT(*) for the count
        // to match the number of rows in the result.
        if (!mDistinct && asCount) {
            selectStatement.append("*");
        } else {
            String columns = "*";
            if (mColumnNames != null && !mColumnNames.isEmpty()) {
                columns = String.join(DELIMITER, mColumnNames);
            }
            selectStatement.append(columns);
        }
        if (asCount) {
            selectStatement.append(")");
        }

        selectStatement.append(FROM);
        return selectStatement.toString();
    }

    private String buildReadQuery(String selectStatement) {
        return selectStatement
                + mTableName
                + mWhereClauses.get(/* withWhereKeyword */ true)
                + mOrderByClause.getOrderBy()
                + (mLimit == null ? "" : LIMIT_SIZE + mLimit);
    }

    private String addPostJoinWhereClauses(String query) {
        return query + mPostJoinWhereClauses.get(/* withWhereKeyword */ true);
    }

    private String appendFinalOrderByAndLimit(String query) {
        return query
                + mFinalOrderByClause.getOrderBy()
                + (mFinalLimit == null ? "" : LIMIT_SIZE + mFinalLimit);
    }

    /** Get requests for populating extra data */
    @Nullable
    public List<ReadTableRequest> getExtraReadRequests() {
        return mExtraReadRequests;
    }

    /** Sets requests to populate extra data */
    public ReadTableRequest setExtraReadRequests(List<ReadTableRequest> extraDataReadRequests) {
        mExtraReadRequests = new ArrayList<>(extraDataReadRequests);
        return this;
    }

    /** Get table name of the request */
    public String getTableName() {
        return mTableName;
    }

    /**
     * Sets order by clause for the read query
     *
     * <p>Note that if the query contains a join, the order by clause will be applied to the main
     * table read sub query before joining. For setting the clause on the full query result use
     * {@link #setFinalOrderBy}.
     */
    public ReadTableRequest setOrderBy(OrderByClause orderBy) {
        mOrderByClause = orderBy;
        return this;
    }

    /**
     * Sets order by clause for the read query, applied to the final query result
     *
     * <p>This means that if the query contains a join, the order by will be applied to the full
     * query result instead of the main table read before joining, as is the case with {@link
     * #setOrderBy}.
     */
    public ReadTableRequest setFinalOrderBy(OrderByClause orderBy) {
        mFinalOrderByClause = orderBy;
        return this;
    }

    /** Returns the current LIMIT size for the query, or null for no LIMIT. */
    @Nullable
    public Integer getLimit() {
        return mLimit;
    }

    /**
     * Sets LIMIT size for the read query, or null for no limit.
     *
     * <p>Note that if the query contains a join, the limit will be applied to the main table read
     * before joining. For setting the limit on the full query result use {@link #setFinalLimit}.
     *
     * <p>If this ReadTableRequest has any unionReadRequests set, but no joinClause, the limit
     * applies to the full query including the unionReadRequests.
     */
    public ReadTableRequest setLimit(@Nullable Integer limit) {
        mLimit = limit;
        return this;
    }

    /** Returns the current final LIMIT size for the query, or null for no LIMIT. */
    @Nullable
    public Integer getFinalLimit() {
        return mFinalLimit;
    }

    /**
     * Sets final LIMIT size, or null for the read query, which will be applied to the final query
     *
     * <p>This means that if the query contains a join, the limit will apply to the full query
     * result instead of the main table read before joining, as is the case with {@link #setLimit}.
     */
    public ReadTableRequest setFinalLimit(@Nullable Integer limit) {
        mFinalLimit = limit;
        return this;
    }

    /** Sets union read requests. */
    public ReadTableRequest setUnionReadRequests(
            @Nullable List<ReadTableRequest> unionReadRequests) {
        mUnionReadRequests = unionReadRequests;

        return this;
    }
}
