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

/** @hide */
public final class SqlJoin {
    private final String mSelfTableName;
    private final String mTableNameToJoinOn;
    private final String mSelfColumnNameToMatch;
    private final String mJoiningColumnNameToMatch;

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

    public String getInnerJoinClause() {
        return " INNER JOIN "
                + mTableNameToJoinOn
                + " ON "
                + mSelfTableName
                + "."
                + mSelfColumnNameToMatch
                + " = "
                + mTableNameToJoinOn
                + "."
                + mJoiningColumnNameToMatch;
    }
}
