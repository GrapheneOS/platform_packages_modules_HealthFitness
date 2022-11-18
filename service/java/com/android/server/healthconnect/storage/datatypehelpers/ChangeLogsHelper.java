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

package com.android.server.healthconnect.storage.datatypehelpers;

import static android.healthconnect.Constants.DEFAULT_INT;

import static com.android.server.healthconnect.storage.datatypehelpers.RecordHelper.PRIMARY_COLUMN_NAME;
import static com.android.server.healthconnect.storage.utils.StorageUtils.DELIMITER;
import static com.android.server.healthconnect.storage.utils.StorageUtils.INTEGER;
import static com.android.server.healthconnect.storage.utils.StorageUtils.PRIMARY_AUTOINCREMENT;
import static com.android.server.healthconnect.storage.utils.StorageUtils.TEXT_NOT_NULL;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorInt;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorStringList;

import android.annotation.NonNull;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.healthconnect.datatypes.RecordTypeIdentifier;
import android.util.ArrayMap;
import android.util.Pair;

import androidx.annotation.IntDef;

import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.request.CreateTableRequest;
import com.android.server.healthconnect.storage.request.ReadTableRequest;
import com.android.server.healthconnect.storage.request.UpsertTableRequest;
import com.android.server.healthconnect.storage.utils.WhereClauses;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A helper class to fetch and store the change logs.
 *
 * @hide
 */
public final class ChangeLogsHelper {
    public static final class ChangeLogs {
        private final Map<Integer, List<String>> mRecordTypeToUUIDMap = new ArrayMap<>();
        @OperationType private final int mOperationType;
        private final String mPackageName;
        /**
         * Create a change logs object that can be used to get change log request for {@code
         * operationType} for {@code packageName}
         */
        public ChangeLogs(@OperationType int operationType, @NonNull String packageName) {
            mOperationType = operationType;
            mPackageName = packageName;
        }

        /**
         * Create a change logs object that can be used to get change log request for {@code
         * operationType} for {@code packageName}
         */
        public ChangeLogs(@OperationType int operationType) {
            mOperationType = operationType;
            mPackageName = null;
        }

        public Map<Integer, List<String>> getRecordTypeToUUIDMap() {
            return mRecordTypeToUUIDMap;
        }

        public List<String> getUUIds() {
            return mRecordTypeToUUIDMap.values().stream()
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());
        }

        public void addUUID(@RecordTypeIdentifier.RecordType int recordType, @NonNull String uuid) {
            Objects.requireNonNull(uuid);

            mRecordTypeToUUIDMap.putIfAbsent(recordType, new ArrayList<>());
            mRecordTypeToUUIDMap.get(recordType).add(uuid);
        }

        /**
         * @return List of {@link UpsertTableRequest} for change log table as per {@code
         *     mRecordIdToUUIDMap}
         */
        public List<UpsertTableRequest> getUpsertTableRequests() {
            Objects.requireNonNull(mPackageName);

            List<UpsertTableRequest> requests = new ArrayList<>(mRecordTypeToUUIDMap.size());
            long packageNameId = AppInfoHelper.getInstance().getAppInfoId(mPackageName);
            if (packageNameId == DEFAULT_INT) {
                throw new IllegalArgumentException("Invalid package name");
            }
            mRecordTypeToUUIDMap.forEach(
                    (recordType, uuids) -> {
                        ContentValues contentValues = new ContentValues();
                        contentValues.put(RECORD_TYPE_COLUMN_NAME, recordType);
                        contentValues.put(APP_ID_COLUMN_NAME, packageNameId);
                        contentValues.put(OPERATION_TYPE_COLUMN_NAME, mOperationType);
                        contentValues.put(UUIDS_COLUMN_NAME, String.join(DELIMITER, uuids));
                        requests.add(new UpsertTableRequest(TABLE_NAME, contentValues));
                    });
            return requests;
        }

        public ChangeLogs addUUIDs(
                @RecordTypeIdentifier.RecordType int recordType, @NonNull List<String> uuids) {
            mRecordTypeToUUIDMap.putIfAbsent(recordType, new ArrayList<>());
            mRecordTypeToUUIDMap.get(recordType).addAll(uuids);
            return this;
        }
    }

    public static final int UPSERT = 0;
    public static final int DELETE = 1;

    public static final String TABLE_NAME = "change_logs_table";
    private static final String RECORD_TYPE_COLUMN_NAME = "record_type";
    private static final String APP_ID_COLUMN_NAME = "app_id";
    private static final String UUIDS_COLUMN_NAME = "uuids";
    private static final String OPERATION_TYPE_COLUMN_NAME = "operation_type";
    private static final int NUM_COLS = 5;
    private static ChangeLogsHelper sChangeLogsHelper;

    private ChangeLogsHelper() {}

    public static ChangeLogsHelper getInstance() {
        if (sChangeLogsHelper == null) {
            sChangeLogsHelper = new ChangeLogsHelper();
        }

        return sChangeLogsHelper;
    }

    @NonNull
    public static List<String> getDeletedIds(Map<Integer, ChangeLogs> operationToChangeLogs) {
        ChangeLogs logs = operationToChangeLogs.get(DELETE);

        if (logs != null) {
            return logs.getUUIds();
        }

        return Collections.emptyList();
    }

    @NonNull
    public static Map<Integer, List<String>> getRecordTypeToInsertedUuids(
            Map<Integer, ChangeLogs> operationToChangeLogs) {
        ChangeLogs logs = operationToChangeLogs.getOrDefault(UPSERT, null);

        if (!Objects.isNull(logs)) {
            return logs.getRecordTypeToUUIDMap();
        }

        return new ArrayMap<>(0);
    }

    @NonNull
    public CreateTableRequest getCreateTableRequest() {
        return new CreateTableRequest(TABLE_NAME, getColumnInfo())
                .createIndexOn(RECORD_TYPE_COLUMN_NAME)
                .createIndexOn(APP_ID_COLUMN_NAME);
    }

    /**
     * @return recordId -> ChangeLogs
     */
    public Map<Integer, ChangeLogs> getChangeLogs(
            ChangeLogsRequestHelper.TokenRequest changeLogTokenRequest) {
        WhereClauses whereClause =
                new WhereClauses()
                        .addWhereGreaterThanClause(
                                PRIMARY_COLUMN_NAME,
                                String.valueOf(changeLogTokenRequest.getRowIdChangeLogs()));
        if (!changeLogTokenRequest.getRecordTypes().isEmpty()) {
            whereClause.addWhereInIntsClause(
                    RECORD_TYPE_COLUMN_NAME, changeLogTokenRequest.getRecordTypes());
        }

        if (!changeLogTokenRequest.getPackageNamesToFilter().isEmpty()) {
            whereClause.addWhereInLongsClause(
                    APP_ID_COLUMN_NAME,
                    AppInfoHelper.getInstance()
                            .getAppInfoIds(changeLogTokenRequest.getPackageNamesToFilter()));
        }

        final ReadTableRequest readTableRequest =
                new ReadTableRequest(TABLE_NAME).setWhereClause(whereClause);

        Map<Integer, ChangeLogs> operationToChangeLogMap = new ArrayMap<>();
        TransactionManager transactionManager = TransactionManager.getInitialisedInstance();
        try (SQLiteDatabase db = transactionManager.getReadableDb();
                Cursor cursor = transactionManager.read(db, readTableRequest)) {
            while (cursor.moveToNext()) {
                addChangeLogs(cursor, operationToChangeLogMap);
            }
        }

        return operationToChangeLogMap;
    }

    public long getLatestRowId() {
        return TransactionManager.getInitialisedInstance().getLastRowIdFor(TABLE_NAME);
    }

    private void addChangeLogs(Cursor cursor, Map<Integer, ChangeLogs> changeLogs) {
        @RecordTypeIdentifier.RecordType
        int recordType = getCursorInt(cursor, RECORD_TYPE_COLUMN_NAME);
        @OperationType int operationType = getCursorInt(cursor, OPERATION_TYPE_COLUMN_NAME);

        changeLogs.putIfAbsent(operationType, new ChangeLogs(operationType));
        changeLogs
                .get(operationType)
                .addUUIDs(recordType, getCursorStringList(cursor, UUIDS_COLUMN_NAME, DELIMITER));
    }

    @NonNull
    private List<Pair<String, String>> getColumnInfo() {
        List<Pair<String, String>> columnInfo = new ArrayList<>(NUM_COLS);
        columnInfo.add(new Pair<>(PRIMARY_COLUMN_NAME, PRIMARY_AUTOINCREMENT));
        columnInfo.add(new Pair<>(RECORD_TYPE_COLUMN_NAME, INTEGER));
        columnInfo.add(new Pair<>(APP_ID_COLUMN_NAME, INTEGER));
        columnInfo.add(new Pair<>(UUIDS_COLUMN_NAME, TEXT_NOT_NULL));
        columnInfo.add(new Pair<>(OPERATION_TYPE_COLUMN_NAME, INTEGER));

        return columnInfo;
    }

    @IntDef({UPSERT, DELETE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface OperationType {}
}
