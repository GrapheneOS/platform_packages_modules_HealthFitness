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
import static android.healthconnect.Constants.DEFAULT_LONG;

import static com.android.server.healthconnect.storage.datatypehelpers.ChangeLogsRequestHelper.DEFAULT_CHANGE_LOG_TIME_PERIOD_IN_DAYS;
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
import com.android.server.healthconnect.storage.request.DeleteTableRequest;
import com.android.server.healthconnect.storage.request.ReadTableRequest;
import com.android.server.healthconnect.storage.request.UpsertTableRequest;
import com.android.server.healthconnect.storage.utils.WhereClauses;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
    public static final int UPSERT = 0;
    public static final int DELETE = 1;
    public static final String TABLE_NAME = "change_logs_table";
    private static final String RECORD_TYPE_COLUMN_NAME = "record_type";
    private static final String APP_ID_COLUMN_NAME = "app_id";
    private static final String UUIDS_COLUMN_NAME = "uuids";
    private static final String OPERATION_TYPE_COLUMN_NAME = "operation_type";
    private static final String TIME_COLUMN_NAME = "time";
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

    public DeleteTableRequest getDeleteRequestForAutoDelete() {
        return new DeleteTableRequest(TABLE_NAME)
                .setTimeFilter(
                        TIME_COLUMN_NAME,
                        Instant.EPOCH.toEpochMilli(),
                        Instant.now()
                                .minus(DEFAULT_CHANGE_LOG_TIME_PERIOD_IN_DAYS, ChronoUnit.DAYS)
                                .toEpochMilli());
    }

    @NonNull
    public CreateTableRequest getCreateTableRequest() {
        return new CreateTableRequest(TABLE_NAME, getColumnInfo())
                .createIndexOn(RECORD_TYPE_COLUMN_NAME)
                .createIndexOn(APP_ID_COLUMN_NAME);
    }

    // Called on DB update.
    public void onUpgrade(int newVersion, @NonNull SQLiteDatabase db) {
        // empty by default
    }

    /** Returns change logs post the time when {@code changeLogTokenRequest} was generated */
    public ChangeLogsResponse getChangeLogs(
            ChangeLogsRequestHelper.TokenRequest changeLogTokenRequest, int pageSize) {
        long token = changeLogTokenRequest.getRowIdChangeLogs();
        WhereClauses whereClause =
                new WhereClauses()
                        .addWhereGreaterThanClause(PRIMARY_COLUMN_NAME, String.valueOf(token));
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

        // We set limit size to pageSize + 1,so that if number of records returned is more than
        // pageSize we know there are more records available to return for the next read.
        final ReadTableRequest readTableRequest =
                new ReadTableRequest(TABLE_NAME).setWhereClause(whereClause).setLimit(pageSize + 1);

        Map<Integer, ChangeLogs> operationToChangeLogMap = new ArrayMap<>();
        TransactionManager transactionManager = TransactionManager.getInitialisedInstance();
        long nextChangesToken = DEFAULT_LONG;
        boolean hasMoreRecords = false;
        try (SQLiteDatabase db = transactionManager.getReadableDb();
                Cursor cursor = transactionManager.read(db, readTableRequest)) {
            int count = 0;
            while (cursor.moveToNext()) {
                if (count >= pageSize) {
                    hasMoreRecords = true;
                    break;
                }
                count += addChangeLogs(cursor, operationToChangeLogMap);
                nextChangesToken = getCursorInt(cursor, PRIMARY_COLUMN_NAME);
            }
        }

        String nextToken =
                nextChangesToken != DEFAULT_LONG
                        ? ChangeLogsRequestHelper.getNextPageToken(
                                changeLogTokenRequest, nextChangesToken)
                        : String.valueOf(token);

        return new ChangeLogsResponse(operationToChangeLogMap, nextToken, hasMoreRecords);
    }

    public long getLatestRowId() {
        return TransactionManager.getInitialisedInstance().getLastRowIdFor(TABLE_NAME);
    }

    private int addChangeLogs(Cursor cursor, Map<Integer, ChangeLogs> changeLogs) {
        @RecordTypeIdentifier.RecordType
        int recordType = getCursorInt(cursor, RECORD_TYPE_COLUMN_NAME);
        @OperationType int operationType = getCursorInt(cursor, OPERATION_TYPE_COLUMN_NAME);
        List<String> uuidList = getCursorStringList(cursor, UUIDS_COLUMN_NAME, DELIMITER);

        changeLogs.putIfAbsent(operationType, new ChangeLogs(operationType));
        changeLogs.get(operationType).addUUIDs(recordType, uuidList);
        return uuidList.size();
    }

    @NonNull
    private List<Pair<String, String>> getColumnInfo() {
        List<Pair<String, String>> columnInfo = new ArrayList<>(NUM_COLS);
        columnInfo.add(new Pair<>(PRIMARY_COLUMN_NAME, PRIMARY_AUTOINCREMENT));
        columnInfo.add(new Pair<>(RECORD_TYPE_COLUMN_NAME, INTEGER));
        columnInfo.add(new Pair<>(APP_ID_COLUMN_NAME, INTEGER));
        columnInfo.add(new Pair<>(UUIDS_COLUMN_NAME, TEXT_NOT_NULL));
        columnInfo.add(new Pair<>(OPERATION_TYPE_COLUMN_NAME, INTEGER));
        columnInfo.add(new Pair<>(TIME_COLUMN_NAME, INTEGER));

        return columnInfo;
    }

    @IntDef({UPSERT, DELETE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface OperationType {}

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

    /** A class to represent the token for pagination for the change logs response */
    public static final class ChangeLogsResponse {
        private final Map<Integer, ChangeLogsHelper.ChangeLogs> mChangeLogsMap;
        private final String mNextPageToken;
        private final boolean mHasMorePages;

        public ChangeLogsResponse(
                @NonNull Map<Integer, ChangeLogsHelper.ChangeLogs> changeLogsMap,
                @NonNull String nextPageToken,
                boolean hasMorePages) {
            mChangeLogsMap = changeLogsMap;
            mNextPageToken = nextPageToken;
            mHasMorePages = hasMorePages;
        }

        /** Returns map of operation type to change logs */
        @NonNull
        public Map<Integer, ChangeLogs> getChangeLogsMap() {
            return mChangeLogsMap;
        }

        /** Returns the next page token for the change logs */
        @NonNull
        public String getNextPageToken() {
            return mNextPageToken;
        }

        /** Returns true if there are more change logs to be read */
        public boolean hasMorePages() {
            return mHasMorePages;
        }
    }
}
