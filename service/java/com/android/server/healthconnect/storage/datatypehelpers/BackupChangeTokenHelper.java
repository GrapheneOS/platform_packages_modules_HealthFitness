/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.server.healthconnect.storage.datatypehelpers;

import static com.android.server.healthconnect.storage.HealthConnectDatabase.createTable;
import static com.android.server.healthconnect.storage.datatypehelpers.RecordHelper.PRIMARY_COLUMN_NAME;
import static com.android.server.healthconnect.storage.utils.StorageUtils.INTEGER;
import static com.android.server.healthconnect.storage.utils.StorageUtils.INTEGER_NOT_NULL;
import static com.android.server.healthconnect.storage.utils.StorageUtils.PRIMARY;
import static com.android.server.healthconnect.storage.utils.StorageUtils.TEXT_NULL;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorInt;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorLong;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorString;
import static com.android.server.healthconnect.storage.utils.WhereClauses.LogicalOperator.AND;

import android.annotation.Nullable;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.util.Pair;

import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.request.CreateTableRequest;
import com.android.server.healthconnect.storage.request.ReadTableRequest;
import com.android.server.healthconnect.storage.request.UpsertTableRequest;
import com.android.server.healthconnect.storage.utils.WhereClauses;

import java.util.ArrayList;
import java.util.List;

/**
 * A class to interact with the DB table that stores the information about the back up requests i.e.
 * {@code TABLE_NAME}
 *
 * <p>This class returns the row_id of the backup_token_table as a token, that can later be used to
 * recreate the backup token.
 *
 * @hide
 */
public class BackupChangeTokenHelper {
    private static final String TABLE_NAME = "backup_change_token_table";
    private static final String RECORD_TYPE_COLUMN_NAME = "record_type";
    private static final String DATA_TABLE_PAGE_TOKEN_COLUMN_NAME = "data_table_page_token";
    private static final String CHANGE_LOGS_REQUEST_TOKEN_COLUMN_NAME = "change_logs_request_token";

    /**
     * @return the row Id for the backup_change_token_table.
     */
    public static String getBackupChangeTokenRowId(
            TransactionManager transactionManager,
            @RecordTypeIdentifier.RecordType int recordType,
            long dataTablePageToken,
            @Nullable String changeLogsRequestToken) {
        ContentValues contentValues = new ContentValues();

        contentValues.put(RECORD_TYPE_COLUMN_NAME, recordType);
        contentValues.put(DATA_TABLE_PAGE_TOKEN_COLUMN_NAME, dataTablePageToken);
        contentValues.put(CHANGE_LOGS_REQUEST_TOKEN_COLUMN_NAME, changeLogsRequestToken);

        return String.valueOf(
                transactionManager.insert(new UpsertTableRequest(TABLE_NAME, contentValues)));
    }

    /** Reads the database and get backup change token. */
    public static BackupChangeToken getBackupChangeToken(
            TransactionManager transactionManager, String token) {
        ReadTableRequest readTableRequest =
                new ReadTableRequest(TABLE_NAME)
                        .setWhereClause(
                                new WhereClauses(AND)
                                        .addWhereEqualsClause(PRIMARY_COLUMN_NAME, token));
        try (Cursor cursor = transactionManager.read(readTableRequest)) {
            if (!cursor.moveToFirst()) {
                throw new IllegalArgumentException("Invalid backup change token");
            }

            return new BackupChangeToken(
                    getCursorInt(cursor, RECORD_TYPE_COLUMN_NAME),
                    getCursorLong(cursor, DATA_TABLE_PAGE_TOKEN_COLUMN_NAME),
                    getCursorString(cursor, CHANGE_LOGS_REQUEST_TOKEN_COLUMN_NAME));
        }
    }

    /** A class to represent the request corresponding to a backup change token. */
    public static class BackupChangeToken {
        private final @RecordTypeIdentifier.RecordType int mRecordType;
        private final long mDataTablePageToken;
        private final @Nullable String mChangeLogsRequestToken;

        /**
         * @param recordType record type to be backed up next
         * @param dataTablePageToken page token for the data table to be backed up
         * @param changeLogsRequestToken row id in change logs request table to get token for change
         *     logs table
         */
        public BackupChangeToken(
                @RecordTypeIdentifier.RecordType int recordType,
                long dataTablePageToken,
                @Nullable String changeLogsRequestToken) {
            mRecordType = recordType;
            mDataTablePageToken = dataTablePageToken;
            mChangeLogsRequestToken = changeLogsRequestToken;
        }

        /**
         * Returns the record type to be backed up next.
         *
         * <p>Set to 0 before a complete full backup or for an incremental backup.
         */
        public @RecordTypeIdentifier.RecordType int getRecordType() {
            return mRecordType;
        }

        /**
         * Returns the page token for the data table to be backed up if the data type is not
         * RECORD_TYPE_UNKNOWN.
         *
         * <p>If the data type name is 0, returns -1.
         */
        public long getDataTablePageToken() {
            return mDataTablePageToken;
        }

        /**
         * Returns the row id in the change logs request table to for retrieving the token in the
         * change log table.
         */
        public @Nullable String getChangeLogsRequestToken() {
            return mChangeLogsRequestToken;
        }
    }

    /** Creates the backup token table. */
    public static void applyBackupTokenUpgrade(SQLiteDatabase db) {
        createTable(db, getCreateTableRequest());
    }

    /**
     * @return the table name.
     */
    public static String getTableName() {
        return TABLE_NAME;
    }

    protected String getMainTableName() {
        return TABLE_NAME;
    }

    private static CreateTableRequest getCreateTableRequest() {
        return new CreateTableRequest(TABLE_NAME, getColumnInfo());
    }

    private static List<Pair<String, String>> getColumnInfo() {
        List<Pair<String, String>> columnInfo = new ArrayList<>();
        columnInfo.add(new Pair<>(PRIMARY_COLUMN_NAME, PRIMARY));
        columnInfo.add(new Pair<>(RECORD_TYPE_COLUMN_NAME, INTEGER_NOT_NULL));
        columnInfo.add(new Pair<>(DATA_TABLE_PAGE_TOKEN_COLUMN_NAME, INTEGER));
        columnInfo.add(new Pair<>(CHANGE_LOGS_REQUEST_TOKEN_COLUMN_NAME, TEXT_NULL));
        return columnInfo;
    }
}
