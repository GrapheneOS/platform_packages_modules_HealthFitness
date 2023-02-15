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

import static android.health.connect.Constants.DEFAULT_LONG;
import static android.health.connect.Constants.DELETE;
import static android.health.connect.Constants.UPSERT;

import static com.android.server.healthconnect.storage.datatypehelpers.ChangeLogsRequestHelper.DEFAULT_CHANGE_LOG_TIME_PERIOD_IN_DAYS;
import static com.android.server.healthconnect.storage.datatypehelpers.RecordHelper.PRIMARY_COLUMN_NAME;
import static com.android.server.healthconnect.storage.utils.StorageUtils.DELIMITER;
import static com.android.server.healthconnect.storage.utils.StorageUtils.INTEGER;
import static com.android.server.healthconnect.storage.utils.StorageUtils.PRIMARY_AUTOINCREMENT;
import static com.android.server.healthconnect.storage.utils.StorageUtils.TEXT_NOT_NULL;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorInt;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorLong;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorStringList;

import android.annotation.NonNull;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.health.connect.accesslog.AccessLog.OperationType;
import android.health.connect.changelog.ChangeLogsResponse.DeletedLog;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.util.ArrayMap;
import android.util.Pair;

import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.request.CreateTableRequest;
import com.android.server.healthconnect.storage.request.DeleteTableRequest;
import com.android.server.healthconnect.storage.request.ReadTableRequest;
import com.android.server.healthconnect.storage.request.UpsertTableRequest;
import com.android.server.healthconnect.storage.utils.WhereClauses;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
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
    public static final String TABLE_NAME = "change_logs_table";
    private static final String RECORD_TYPE_COLUMN_NAME = "record_type";
    private static final String APP_ID_COLUMN_NAME = "app_id";
    private static final String UUIDS_COLUMN_NAME = "uuids";
    private static final String OPERATION_TYPE_COLUMN_NAME = "operation_type";
    private static final String TIME_COLUMN_NAME = "time";
    private static final int NUM_COLS = 5;
    private static volatile ChangeLogsHelper sChangeLogsHelper;

    private ChangeLogsHelper() {}

    public static synchronized ChangeLogsHelper getInstance() {
        if (sChangeLogsHelper == null) {
            sChangeLogsHelper = new ChangeLogsHelper();
        }

        return sChangeLogsHelper;
    }

    @NonNull
    public static List<DeletedLog> getDeletedLogs(Map<Integer, ChangeLogs> operationToChangeLogs) {
        ChangeLogs logs = operationToChangeLogs.get(DELETE);

        if (!Objects.isNull(logs)) {
            List<String> ids = logs.getUUIds();
            long timeStamp = logs.getChangeLogTimeStamp();
            List<DeletedLog> deletedLogs = new ArrayList<>(ids.size());
            for (String id : ids) {
                deletedLogs.add(new DeletedLog(id, timeStamp));
            }
            return deletedLogs;
        }
        return new ArrayList<>();
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

        // In setLimit(pagesize) method size will be set to pageSize + 1,so that if number of
        // records returned is more than pageSize we know there are more records available to return
        // for the next read.
        final ReadTableRequest readTableRequest =
                new ReadTableRequest(TABLE_NAME).setWhereClause(whereClause).setLimit(pageSize);

        Map<Integer, ChangeLogs> operationToChangeLogMap = new ArrayMap<>();
        TransactionManager transactionManager = TransactionManager.getInitialisedInstance();
        long nextChangesToken = DEFAULT_LONG;
        boolean hasMoreRecords = false;
        final SQLiteDatabase db = transactionManager.getReadableDb();
        try (Cursor cursor = transactionManager.read(db, readTableRequest)) {
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
        @OperationType.OperationTypes
        int operationType = getCursorInt(cursor, OPERATION_TYPE_COLUMN_NAME);
        List<String> uuidList = getCursorStringList(cursor, UUIDS_COLUMN_NAME, DELIMITER);
        changeLogs.putIfAbsent(
                operationType,
                new ChangeLogs(operationType, getCursorLong(cursor, TIME_COLUMN_NAME)));
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

    public static final class ChangeLogs {
        private final Map<Integer, List<String>> mRecordTypeToUUIDMap = new ArrayMap<>();
        @OperationType.OperationTypes private final int mOperationType;
        private final String mPackageName;
        private final long mChangeLogTimeStamp;
        /**
         * Creates a change logs object used to add a new change log for {@code operationType} for
         * {@code packageName} logged at time {@code timeStamp }
         *
         * @param operationType Type of the operation for which change log is added whether insert
         *     or delete.
         * @param packageName Package name of the records for which change log is added.
         * @param timeStamp Time when the change log is added.
         */
        public ChangeLogs(
                @OperationType.OperationTypes int operationType,
                @NonNull String packageName,
                long timeStamp) {
            mOperationType = operationType;
            mPackageName = packageName;
            mChangeLogTimeStamp = timeStamp;
        }

        /**
         * Creates a change logs object used to add a new change log for {@code operationType}
         * logged at time {@code timeStamp }
         *
         * @param operationType Type of the operation for which change log is added whether insert
         *     or delete.
         * @param timeStamp Time when the change log is added.
         */
        public ChangeLogs(@OperationType.OperationTypes int operationType, long timeStamp) {
            mOperationType = operationType;
            mChangeLogTimeStamp = timeStamp;
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

        public long getChangeLogTimeStamp() {
            return mChangeLogTimeStamp;
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
            // TODO(b/261848494): Use correct packageNameId when deletes are from UI APK.
            // Pass appId in addUUIDs and then we can create upsert requests there itself
            long packageNameId = AppInfoHelper.getInstance().getAppInfoId(mPackageName);
            mRecordTypeToUUIDMap.forEach(
                    (recordType, uuids) -> {
                        ContentValues contentValues = new ContentValues();
                        contentValues.put(RECORD_TYPE_COLUMN_NAME, recordType);
                        contentValues.put(APP_ID_COLUMN_NAME, packageNameId);
                        contentValues.put(OPERATION_TYPE_COLUMN_NAME, mOperationType);
                        contentValues.put(TIME_COLUMN_NAME, mChangeLogTimeStamp);
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
