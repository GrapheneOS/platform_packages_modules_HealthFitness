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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** @hide */
public final class WhereClauses {
    final List<String> mClauses = new ArrayList<>();

    public WhereClauses addWhereBetweenClause(String columnName, long start, long end) {
        mClauses.add(columnName + " BETWEEN " + start + " AND " + end);

        return this;
    }

    public WhereClauses addWhereBetweenTimeClause(String columnName, long startTime, long endTime) {
        if (startTime < 0 || endTime < 0 || endTime < startTime) {
            return this;
        }

        mClauses.add(columnName + " BETWEEN " + startTime + " AND " + endTime);

        return this;
    }

    public WhereClauses addWhereInClause(String columnName, List<String> values) {
        if (values == null || values.isEmpty()) return this;

        mClauses.add(columnName + " IN " + "('" + String.join("', '", values) + "')");

        return this;
    }

    public WhereClauses addWhereEqualsClause(String columnName, String value) {
        if (columnName == null || value == null || value.isEmpty() || columnName.isEmpty()) {
            return this;
        }

        mClauses.add(columnName + " = '" + value + "'");

        return this;
    }

    public WhereClauses addWhereGreaterThanClause(String columnName, String value) {
        mClauses.add(columnName + " > '" + value + "'");

        return this;
    }

    public WhereClauses addWhereInIntsClause(String columnName, List<Integer> values) {
        if (values == null || values.isEmpty()) return this;

        mClauses.add(
                columnName
                        + " IN ("
                        + values.stream().map(String::valueOf).collect(Collectors.joining(", "))
                        + ")");

        return this;
    }

    public WhereClauses addWhereInLongsClause(String columnName, List<Long> values) {
        if (values == null || values.isEmpty()) return this;

        mClauses.add(
                columnName
                        + " IN ("
                        + values.stream().map(String::valueOf).collect(Collectors.joining(", "))
                        + ")");

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

        return (withWhereKeyword ? " WHERE " : "") + String.join(" AND ", mClauses);
    }
}
