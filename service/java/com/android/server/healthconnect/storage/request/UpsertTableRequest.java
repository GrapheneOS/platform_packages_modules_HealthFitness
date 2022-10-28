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
import android.annotation.Nullable;
import android.content.ContentValues;
import android.healthconnect.Constants;
import android.util.Slog;

import com.android.server.healthconnect.storage.utils.WhereClauses;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** @hide */
public class UpsertTableRequest {
    public static final int INVALID_ROW_ID = -1;
    private static final String TAG = "HealthConnectUpsert";

    private final String mTable;
    private final ContentValues mContentValues;
    private List<UpsertTableRequest> mChildTableRequests = Collections.emptyList();
    private String mParentCol;
    private long mRowId = INVALID_ROW_ID;
    private WhereClauses mWhereClausesForUpdate;

    public UpsertTableRequest(@NonNull String table, @NonNull ContentValues contentValues) {
        mTable = table;
        mContentValues = contentValues;
    }

    @NonNull
    public UpsertTableRequest withParentKey(long rowId) {
        mRowId = rowId;
        return this;
    }

    /**
     * Use this if you want to add row_id of the parent table to all the child entries in {@code
     * parentCol}
     */
    @NonNull
    public UpsertTableRequest setParentColumnForChildTables(@Nullable String parentCol) {
        mParentCol = parentCol;
        return this;
    }

    @NonNull
    public String getTable() {
        return mTable;
    }

    @NonNull
    public ContentValues getContentValues() {
        // Set the parent column of the creator of this requested to do that
        if (!Objects.isNull(mParentCol) && mRowId != INVALID_ROW_ID) {
            mContentValues.put(mParentCol, mRowId);
        }

        if (Constants.DEBUG) {
            Slog.d(TAG, "Upsert to " + mTable + ":" + mContentValues);
        }
        return mContentValues;
    }

    @NonNull
    public List<UpsertTableRequest> getChildTableRequests() {
        return mChildTableRequests;
    }

    public void setWhereClauses(WhereClauses whereClauses) {
        mWhereClausesForUpdate = whereClauses;
    }

    @NonNull
    public WhereClauses getWhereClauses() {
        return mWhereClausesForUpdate;
    }

    @NonNull
    public UpsertTableRequest setChildTableRequests(
            @NonNull List<UpsertTableRequest> childTableRequests) {
        Objects.requireNonNull(childTableRequests);

        mChildTableRequests = childTableRequests;
        return this;
    }
}
