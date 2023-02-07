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

package com.android.server.healthconnect.storage.utils;

import static com.android.server.healthconnect.storage.utils.StorageUtils.SELECT_ALL;

import android.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Interface of the join SQL class.
 *
 * @hide
 */
public class SqlJoin {

    public static final String SQL_JOIN_INNER = "INNER";
    public static final String SQL_JOIN_LEFT = "LEFT";

    /** @hide */
    @StringDef(
            value = {
                SQL_JOIN_INNER,
                SQL_JOIN_LEFT,
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface JoinType {}

    private final String mSelfTableName;
    private final String mTableNameToJoinOn;
    private final String mSelfColumnNameToMatch;
    private final String mJoiningColumnNameToMatch;
    private String mJoinType = SQL_JOIN_INNER;

    public SqlJoin(
            String selfTableName,
            String tableNameToJoinOn,
            String selfColumnNameToMatch,
            String joiningColumnNameToMatch) {
        mSelfTableName = selfTableName;
        mTableNameToJoinOn = tableNameToJoinOn;
        mSelfColumnNameToMatch = selfColumnNameToMatch;
        mJoiningColumnNameToMatch = joiningColumnNameToMatch;
    }

    /**
     * Sets join type to the current joint, default value is inner join. Returns class with join
     * type set.
     */
    public SqlJoin setJoinType(@JoinType String joinType) {
        mJoinType = joinType;
        return this;
    }

    /**
     * Returns query by applying JOIN condition on the innerQuery
     *
     * @param innerQuery An inner query to be used for the JOIN
     * @return Final query with JOIN condition
     */
    public String getJoinWithQueryCommand(String innerQuery) {
        if (innerQuery == null) {
            throw new IllegalArgumentException("Inner query cannot be null");
        }
        return SELECT_ALL
                + "( "
                + innerQuery
                + " ) "
                + getJoinCommand(/* withSelfTableNamePrefix= */ false);
    }

    /** Returns join command. */
    public String getJoinCommand() {
        return getJoinCommand(/* withSelfTableNamePrefix= */ true);
    }

    private String getJoinCommand(boolean withSelfTableNamePrefix) {
        String selfColumnPrefix = withSelfTableNamePrefix ? mSelfTableName + "." : "";
        return " "
                + mJoinType
                + " JOIN "
                + mTableNameToJoinOn
                + " ON "
                + selfColumnPrefix
                + mSelfColumnNameToMatch
                + " = "
                + mTableNameToJoinOn
                + "."
                + mJoiningColumnNameToMatch;
    }
}
