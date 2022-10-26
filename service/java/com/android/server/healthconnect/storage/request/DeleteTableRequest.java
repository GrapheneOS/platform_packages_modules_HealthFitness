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

import static android.healthconnect.Constants.DEFAULT_LONG;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.healthconnect.Constants;
import android.healthconnect.datatypes.RecordTypeIdentifier;
import android.util.Slog;

import com.android.server.healthconnect.storage.utils.WhereClauses;

import java.util.List;
import java.util.Objects;

/**
 * No need to have delete-requests for child tables as ideally they should be following cascaded
 * deletes. If not please rethink the table structure and if possible remove the parent-child
 * relationship.
 *
 * @hide
 */
public class DeleteTableRequest {
    private static final String TAG = "HealthConnectDelete";
    private final String mTableName;
    @RecordTypeIdentifier.RecordType private final int mRecordType;

    private String mIdColumnName;
    private String mPackageColumnName;
    private String mTimeColumnName;
    private List<Long> mPackageFilters;
    private long mStartTime = DEFAULT_LONG;
    private long mEndTime = DEFAULT_LONG;
    private boolean mRequiresUuId;
    private String mId;

    public DeleteTableRequest(
            @NonNull String tableName, @RecordTypeIdentifier.RecordType int recordType) {
        Objects.requireNonNull(tableName);

        mTableName = tableName;
        mRecordType = recordType;
    }

    public DeleteTableRequest setId(@NonNull String id, String idColumnName) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(idColumnName);

        mId = id;
        mIdColumnName = idColumnName;
        return this;
    }

    public boolean requiresUuId() {
        return mRequiresUuId;
    }

    public DeleteTableRequest setRequiresUuId(@NonNull String idColumnName) {
        Objects.requireNonNull(idColumnName);

        mRequiresUuId = true;
        mIdColumnName = idColumnName;

        return this;
    }

    public DeleteTableRequest unsetRequiresUuId() {
        mRequiresUuId = false;
        mIdColumnName = null;

        return this;
    }

    public int getRecordType() {
        return mRecordType;
    }

    @Nullable
    public String getIdColumnName() {
        return mIdColumnName;
    }

    @NonNull
    public String getTableName() {
        return mTableName;
    }

    @NonNull
    public DeleteTableRequest setPackageFilter(
            String packageColumnName, List<Long> packageFilters) {
        mPackageFilters = packageFilters;
        mPackageColumnName = packageColumnName;

        return this;
    }

    @NonNull
    public String getDeleteWhereCommand() {
        WhereClauses whereClauses = new WhereClauses();
        whereClauses.addWhereInLongsClause(mPackageColumnName, mPackageFilters);
        whereClauses.addWhereBetweenTimeClause(mTimeColumnName, mStartTime, mEndTime);
        whereClauses.addWhereEqualsClause(mIdColumnName, mId);

        if (Constants.DEBUG) {
            Slog.d(
                    TAG,
                    "delete query: tableName: "
                            + mTableName
                            + " whereClause: "
                            + whereClauses.get(true));
        }

        return whereClauses.get(true);
    }

    @NonNull
    public DeleteTableRequest setTimeFilter(
            @NonNull String timeColumnName, long startTime, long endTime) {
        Objects.requireNonNull(timeColumnName);

        // Return if the params will result in no impact on the query
        if (endTime < 0 || startTime < 0 || endTime > startTime) {
            return this;
        }

        mStartTime = startTime;
        mEndTime = endTime;
        mTimeColumnName = timeColumnName;

        return this;
    }
}
