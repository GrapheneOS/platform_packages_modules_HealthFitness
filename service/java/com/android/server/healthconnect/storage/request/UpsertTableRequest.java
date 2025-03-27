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

import static com.android.server.healthconnect.storage.utils.WhereClauses.LogicalOperator.OR;

import android.annotation.IntDef;
import android.annotation.Nullable;
import android.content.ContentValues;
import android.database.Cursor;
import android.util.Pair;

import com.android.server.healthconnect.fitness.recordhelpers.RecordHelper;
import com.android.server.healthconnect.storage.utils.StorageUtils;
import com.android.server.healthconnect.storage.utils.TableColumnPair;
import com.android.server.healthconnect.storage.utils.WhereClauses;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** @hide */
public class UpsertTableRequest {
    public static final int INVALID_ROW_ID = -1;

    public static final int TYPE_STRING = 0;
    public static final int TYPE_BLOB = 1;
    private final String mTable;
    private final ContentValues mContentValues;
    private final List<Pair<String, Integer>> mUniqueColumns;
    private List<UpsertTableRequest> mChildTableRequests = Collections.emptyList();
    @Nullable private String mParentCol;
    private long mRowId = INVALID_ROW_ID;
    @Nullable private WhereClauses mWhereClausesForUpdate;
    private IRequiresUpdate mRequiresUpdate = new IRequiresUpdate() {};
    private List<String> mPostUpsertCommands = Collections.emptyList();
    private List<TableColumnPair> mChildTableAndColumnPairsToDelete = Collections.emptyList();

    public UpsertTableRequest(String table, ContentValues contentValues) {
        this(table, contentValues, Collections.emptyList());
    }

    public UpsertTableRequest(
            String table, ContentValues contentValues, List<Pair<String, Integer>> uniqueColumns) {
        Objects.requireNonNull(table);
        Objects.requireNonNull(contentValues);
        Objects.requireNonNull(uniqueColumns);

        mTable = table;
        mContentValues = contentValues;
        mUniqueColumns = uniqueColumns;
    }

    public int getUniqueColumnsCount() {
        return mUniqueColumns.size();
    }

    public UpsertTableRequest withParentKey(long rowId) {
        mRowId = rowId;
        return this;
    }

    /**
     * Use this if you want to add row_id of the parent table to all the child entries in {@code
     * parentCol}
     */
    public UpsertTableRequest setParentColumnForChildTables(@Nullable String parentCol) {
        mParentCol = parentCol;
        return this;
    }

    /**
     * Return a method which tells whether to perform an update in the case of a duplicate primary
     * key being found, or some other conflict.
     *
     * <p>Sometimes when an insert is requested a duplication will be found, eg for same primary
     * key. By default UpsertTableRequest will do an update to replace the information. However for
     * some tables (eg records) it can be faster in some circumstances to skip the update. This sets
     * an {@link IRequiresUpdate} to make the decision on whether the update is required.
     *
     * @param requiresUpdate a method to decide whether to update in the event of conflict
     * @return this UpsertTableRequest
     */
    public UpsertTableRequest setRequiresUpdateClause(IRequiresUpdate requiresUpdate) {
        Objects.requireNonNull(requiresUpdate);

        mRequiresUpdate = requiresUpdate;
        return this;
    }

    public String getTable() {
        return mTable;
    }

    public ContentValues getContentValues() {
        // Set the parent column of the creator of this requested to do that
        if (!Objects.isNull(mParentCol) && mRowId != INVALID_ROW_ID) {
            mContentValues.put(mParentCol, mRowId);
        }

        return mContentValues;
    }

    public List<UpsertTableRequest> getChildTableRequests() {
        return mChildTableRequests;
    }

    public UpsertTableRequest setChildTableRequests(List<UpsertTableRequest> childTableRequests) {
        Objects.requireNonNull(childTableRequests);

        mChildTableRequests = childTableRequests;
        return this;
    }

    public WhereClauses getUpdateWhereClauses() {
        if (mWhereClausesForUpdate == null) {
            return getReadWhereClauses();
        }

        return mWhereClausesForUpdate;
    }

    public UpsertTableRequest setUpdateWhereClauses(WhereClauses whereClauses) {
        Objects.requireNonNull(whereClauses);

        mWhereClausesForUpdate = whereClauses;
        return this;
    }

    public ReadTableRequest getReadRequest() {
        return new ReadTableRequest(getTable()).setWhereClause(getReadWhereClauses());
    }

    public ReadTableRequest getReadRequestUsingUpdateClause() {
        return new ReadTableRequest(getTable()).setWhereClause(getUpdateWhereClauses());
    }

    private WhereClauses getReadWhereClauses() {
        WhereClauses readWhereClause = new WhereClauses(OR);

        for (Pair<String, Integer> uniqueColumn : mUniqueColumns) {
            switch (uniqueColumn.second) {
                case TYPE_BLOB ->
                        readWhereClause.addWhereEqualsClause(
                                uniqueColumn.first,
                                StorageUtils.getHexString(
                                        mContentValues.getAsByteArray(uniqueColumn.first)));
                case TYPE_STRING ->
                        readWhereClause.addWhereEqualsClause(
                                uniqueColumn.first, mContentValues.getAsString(uniqueColumn.first));
                default ->
                        throw new UnsupportedOperationException(
                                "Unable to find type: " + uniqueColumn.second);
            }
        }

        return readWhereClause;
    }

    /**
     * Returns true if an update should be performed for the element at the given cursor on the
     * event of a duplication conflict.
     *
     * @param cursor a database cursor pointing to the row to potentially update
     * @return true if the row should be updated, false otherwise
     */
    public boolean requiresUpdate(Cursor cursor) {
        return mRequiresUpdate.requiresUpdate(cursor, getContentValues(), this);
    }

    public String getRowIdColName() {
        return RecordHelper.PRIMARY_COLUMN_NAME;
    }

    /**
     * Returns the {@link UpsertTableRequest} with the {@code childTableAndColumnPairsToDelete} set.
     *
     * @param childTableAndColumnPairsToDelete a list of {@link TableColumnPair}.
     */
    public UpsertTableRequest setChildTablesWithRowsToBeDeletedDuringUpdate(
            List<TableColumnPair> childTableAndColumnPairsToDelete) {
        mChildTableAndColumnPairsToDelete = childTableAndColumnPairsToDelete;
        return this;
    }

    /**
     * Returns a list {@link TableColumnPair}s to be deleted when an update happens to the parent
     * row.
     */
    public List<TableColumnPair> getChildTablesWithRowsToBeDeletedDuringUpdate() {
        return mChildTableAndColumnPairsToDelete;
    }

    /** Get SQL commands to be exected after this upsert has completed. */
    public List<String> getPostUpsertCommands() {
        return mPostUpsertCommands;
    }

    /** Set SQL commands to be exected after this upsert has completed. */
    public UpsertTableRequest setPostUpsertCommands(List<String> commands) {
        mPostUpsertCommands = commands;
        return this;
    }

    @Target(ElementType.TYPE_USE)
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({TYPE_STRING, TYPE_BLOB})
    public @interface ColumnType {}

    public interface IRequiresUpdate {
        default boolean requiresUpdate(
                Cursor cursor, ContentValues contentValues, UpsertTableRequest request) {
            return true;
        }
    }
}
