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

import android.annotation.NonNull;
import android.healthconnect.Constants;
import android.util.Slog;

import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.utils.WhereClauses;

import java.util.List;
import java.util.Objects;

/**
 * A request for {@link TransactionManager} to read the DB
 *
 * @hide
 */
public class ReadTableRequest {
    private static final String TAG = "HealthConnectRead";

    private final String mTableName;
    private List<String> mColumnNames;
    private WhereClauses mWhereClauses = new WhereClauses();

    public ReadTableRequest(@NonNull String tableName) {
        Objects.requireNonNull(tableName);

        mTableName = tableName;
    }

    public ReadTableRequest setColumnNames(@NonNull List<String> columnNames) {
        Objects.requireNonNull(columnNames);

        mColumnNames = columnNames;
        return this;
    }

    public ReadTableRequest setWhereClause(WhereClauses whereClauses) {
        mWhereClauses = whereClauses;
        return this;
    }

    /** Returns SQL statement to perform aggregation operation */
    @NonNull
    public String getReadCommand() {
        final StringBuilder builder = new StringBuilder("SELECT * FROM " + mTableName + " ");

        builder.append(mWhereClauses.get());

        if (Constants.DEBUG) {
            Slog.d(TAG, "read query: " + builder);
        }

        return builder.toString();
    }

    @NonNull
    public String[] getSelectionArgs() {
        if (mColumnNames != null) {
            return mColumnNames.toArray(new String[0]);
        }

        return new String[0];
    }
}
