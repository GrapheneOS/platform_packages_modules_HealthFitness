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

package com.android.server.healthconnect.storage.utils;

import android.annotation.Nullable;

import com.android.server.healthconnect.storage.request.ReadTableRequest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/** @hide */
public final class WhereClauses {
    public enum LogicalOperator {
        AND(" AND "),
        OR(" OR ");

        private final String opKeyword;

        LogicalOperator(String opKeyword) {
            this.opKeyword = opKeyword;
        }
    }

    private final List<String> mClauses = new ArrayList<>();
    private final LogicalOperator mLogicalOperator;

    public WhereClauses(LogicalOperator logicalOperator) {
        mLogicalOperator = logicalOperator;
    }


    public WhereClauses addWhereBetweenClause(String columnName, long start, long end) {
        mClauses.add(columnName + " >= " + start + " AND " + columnName + " < " + end);

        return this;
    }

    /**
     * Adds a clause for a time column between two values.
     *
     * <p>If the clause would be guaranteed empty (end < 0 or start > end) no clause is added.
     *
     * @param columnName the column to check (or no effect if null)
     * @param startTime the earliest start time (inclusive)
     * @param endTime the latest end time (inclusive)
     */
    public WhereClauses addWhereBetweenTimeClause(
            @Nullable String columnName, long startTime, long endTime) {
        if (columnName == null || endTime < 0 || endTime < startTime) {
            // Below method has check for startTime less than 0.
            // If both startTime and endTime are less than 0 then no new time clause will be added
            // and just return current object.
            return addWhereLaterThanTimeClause(columnName, startTime);
        }

        mClauses.add(columnName + " >= " + startTime + " AND " + columnName + " < " + endTime);

        return this;
    }

    /**
     * Adds a clause for a time column after a given time.
     *
     * <p>If the clause would be guaranteed empty (startTime < 0) then no clause is added.
     *
     * @param columnName the column to check (or no effect if null)
     * @param startTime the earliest start time (inclusive)
     */
    public WhereClauses addWhereLaterThanTimeClause(@Nullable String columnName, long startTime) {
        if (startTime < 0 || columnName == null) {
            return this;
        }

        mClauses.add(columnName + " > " + startTime);

        return this;
    }

    /** Adds a clause for a time column before a given time. */
    public WhereClauses addWhereBeforeThanTimeClause(@Nullable String columnName, long endTime) {
        if (endTime < 0 || columnName == null) {
            return this;
        }

        mClauses.add(columnName + " < " + endTime);

        return this;
    }

    public WhereClauses addWhereInClause(String columnName, List<String> values) {
        if (values == null || values.isEmpty()) return this;

        mClauses.add(columnName + " IN " + "('" + String.join("', '", values) + "')");

        return this;
    }

    /**
     * Adds a clause for a value contained in a list of values.
     *
     * <p>If the clause would be guaranteed empty (values null or emoty) no clause is added.
     *
     * @param columnName the column to check (or no effect if null)
     * @param values the column to check (or no effect if null)
     */
    public WhereClauses addWhereInClauseWithoutQuotes(
            @Nullable String columnName, @Nullable List<String> values) {
        if (columnName == null || values == null || values.isEmpty()) return this;

        mClauses.add(columnName + " IN " + "(" + String.join(", ", values) + ")");

        return this;
    }

    public WhereClauses addWhereEqualsClause(String columnName, String value) {
        if (columnName == null || value == null || value.isEmpty() || columnName.isEmpty()) {
            return this;
        }

        mClauses.add(columnName + " = " + StorageUtils.getNormalisedString(value));
        return this;
    }

    public WhereClauses addWhereGreaterThanClause(String columnName, String value) {
        mClauses.add(columnName + " > '" + value + "'");

        return this;
    }

    /** Add clause columnName > value */
    public WhereClauses addWhereGreaterThanClause(String columnName, long value) {
        mClauses.add(columnName + " > " + value);

        return this;
    }

    public WhereClauses addWhereGreaterThanOrEqualClause(String columnName, long value) {
        mClauses.add(columnName + " >= " + value);

        return this;
    }

    public WhereClauses addWhereLessThanOrEqualClause(String columnName, long value) {
        mClauses.add(columnName + " <= " + value);

        return this;
    }

    /** Add clause columnName < value */
    public WhereClauses addWhereLessThanClause(String columnName, long value) {
        mClauses.add(columnName + " < " + value);

        return this;
    }

    /**
     * Adds where in condition for the column.
     *
     * @param columnName Column name on which where condition to be applied
     * @param values to check in the where condition
     */
    public WhereClauses addWhereInIntsClause(
            @Nullable String columnName, @Nullable Collection<Integer> values) {
        if (columnName == null || values == null || values.isEmpty()) return this;

        mClauses.add(
                columnName
                        + " IN ("
                        + values.stream()
                                .distinct()
                                .map(String::valueOf)
                                .collect(Collectors.joining(", "))
                        + ")");

        return this;
    }

    /**
     * Adds where in condition for the column.
     *
     * @param columnName Column name on which where condition to be applied
     * @param values to check in the where condition
     */
    public WhereClauses addWhereInLongsClause(
            @Nullable String columnName, @Nullable Collection<Long> values) {
        if (columnName == null || values == null || values.isEmpty()) return this;

        mClauses.add(
                columnName
                        + " IN ("
                        + values.stream()
                                .distinct()
                                .map(String::valueOf)
                                .collect(Collectors.joining(", "))
                        + ")");

        return this;
    }

    /**
     * Creates IN clause, where in range is another SQL request. Returns instance with extra clauses
     * set.
     */
    public WhereClauses addWhereInSQLRequestClause(String columnName, ReadTableRequest inRequest) {
        mClauses.add(columnName + " IN (" + inRequest.getReadCommand() + ") ");

        return this;
    }

    /** Adds other {@link WhereClauses} as conditions of this where clause. */
    public WhereClauses addNestedWhereClauses(WhereClauses... otherWhereClauses) {
        for (WhereClauses whereClauses : otherWhereClauses) {
            if (whereClauses.mClauses.isEmpty()) {
                // skip empty WhereClauses so we don't add empty pairs of parentheses "()" to the
                // final SQL statement
                continue;
            }
            if (mLogicalOperator.equals(whereClauses.mLogicalOperator)) {
                // If the logical operator matches we don't need extra parentheses
                mClauses.addAll(whereClauses.mClauses);
            } else {
                mClauses.add("(" + whereClauses.get(/* withWhereKeyword= */ false) + ")");
            }
        }

        return this;
    }

    /**
     * Returns where clauses joined by 'AND', if the input parameter isIncludeWHEREinClauses is true
     * then the clauses are preceded by 'WHERE'.
     */
    public String get(boolean withWhereKeyword) {
        if (mClauses.isEmpty()) {
            return "";
        }

        return (withWhereKeyword ? " WHERE " : "")
                + String.join(mLogicalOperator.opKeyword, mClauses);
    }
}
