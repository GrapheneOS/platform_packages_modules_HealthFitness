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
import static android.health.connect.Constants.DEFAULT_PAGE_SIZE;

import static com.android.server.healthconnect.fitness.recordhelpers.RecordHelper.PRIMARY_COLUMN_NAME;
import static com.android.server.healthconnect.storage.datatypehelpers.ChangeLogsRequestHelper.getChangeLogRetentionDuration;
import static com.android.server.healthconnect.storage.utils.StorageUtils.BLOB_NON_NULL;
import static com.android.server.healthconnect.storage.utils.StorageUtils.INTEGER;
import static com.android.server.healthconnect.storage.utils.StorageUtils.PRIMARY_AUTOINCREMENT;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorInt;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorLong;
import static com.android.server.healthconnect.storage.utils.WhereClauses.LogicalOperator.AND;

import static java.lang.Integer.min;

import android.content.ContentValues;
import android.database.Cursor;
import android.health.connect.accesslog.AccessLog.OperationType;
import android.health.connect.changelog.ChangeLogsRequest;
import android.health.connect.changelog.ChangeLogsResponse.DeletedLog;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.util.ArrayMap;
import android.util.Pair;

import com.android.internal.annotations.VisibleForTesting;
import com.android.server.healthconnect.storage.DatabaseHelper;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.request.CreateTableRequest;
import com.android.server.healthconnect.storage.request.DeleteTableRequest;
import com.android.server.healthconnect.storage.request.ReadTableRequest;
import com.android.server.healthconnect.storage.request.UpsertTableRequest;
import com.android.server.healthconnect.storage.utils.OrderByClause;
import com.android.server.healthconnect.storage.utils.StorageUtils;
import com.android.server.healthconnect.storage.utils.WhereClauses;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * A helper class to fetch and store the change logs.
 *
 * @hide
 */
public final class ChangeLogsHelper extends DatabaseHelper {
    public static final String TABLE_NAME = "change_logs_table";
    @VisibleForTesting public static final String RECORD_TYPE_COLUMN_NAME = "record_type";
    @VisibleForTesting public static final String APP_ID_COLUMN_NAME = "app_id";
    @VisibleForTesting public static final String UUIDS_COLUMN_NAME = "uuids";
    @VisibleForTesting public static final String OPERATION_TYPE_COLUMN_NAME = "operation_type";
    @VisibleForTesting public static final String TIME_COLUMN_NAME = "time";

    private final TransactionManager mTransactionManager;

    public ChangeLogsHelper(
            TransactionManager transactionManager, DatabaseHelpers databaseHelpers) {
        super(databaseHelpers);
        mTransactionManager = transactionManager;
    }

    public static DeleteTableRequest getDeleteRequestForAutoDelete() {
        return new DeleteTableRequest(TABLE_NAME)
                .setTimeFilter(
                        TIME_COLUMN_NAME,
                        Instant.EPOCH.toEpochMilli(),
                        Instant.now().minus(getChangeLogRetentionDuration()).toEpochMilli());
    }

    public static CreateTableRequest getCreateTableRequest() {
        var columns =
                List.of(
                        new Pair<>(PRIMARY_COLUMN_NAME, PRIMARY_AUTOINCREMENT),
                        new Pair<>(RECORD_TYPE_COLUMN_NAME, INTEGER),
                        new Pair<>(APP_ID_COLUMN_NAME, INTEGER),
                        new Pair<>(UUIDS_COLUMN_NAME, BLOB_NON_NULL),
                        new Pair<>(OPERATION_TYPE_COLUMN_NAME, INTEGER),
                        new Pair<>(TIME_COLUMN_NAME, INTEGER));
        return new CreateTableRequest(TABLE_NAME, columns)
                .createIndexOn(RECORD_TYPE_COLUMN_NAME)
                .createIndexOn(APP_ID_COLUMN_NAME);
    }

    /** Returns datatypes being written/updates in past 30 days. */
    public Set<Integer> getRecordTypesWrittenInPast30Days() {
        Set<Integer> recordTypesWrittenInPast30Days = new HashSet<>();
        WhereClauses whereClauses =
                new WhereClauses(AND)
                        .addWhereEqualsClause(
                                OPERATION_TYPE_COLUMN_NAME,
                                String.valueOf(OperationType.OPERATION_TYPE_UPSERT))
                        .addWhereGreaterThanOrEqualClause(
                                TIME_COLUMN_NAME,
                                Instant.now().minus(30, ChronoUnit.DAYS).toEpochMilli());

        final ReadTableRequest readTableRequest =
                new ReadTableRequest(TABLE_NAME)
                        .setColumnNames(List.of(RECORD_TYPE_COLUMN_NAME))
                        .setWhereClause(whereClauses)
                        .setDistinctClause(true);

        try (Cursor cursor = mTransactionManager.read(readTableRequest)) {
            while (cursor.moveToNext()) {
                recordTypesWrittenInPast30Days.add(getCursorInt(cursor, RECORD_TYPE_COLUMN_NAME));
            }
        }
        return recordTypesWrittenInPast30Days;
    }

    @Override
    protected String getMainTableName() {
        return TABLE_NAME;
    }

    /** Returns change logs post the time when {@code changeLogTokenRequest} was generated */
    public ChangeLogsResponse getChangeLogs(
            AppInfoHelper appInfoHelper,
            ChangeLogsRequestHelper.TokenRequest changeLogTokenRequest,
            ChangeLogsRequest changeLogsRequest,
            ChangeLogsRequestHelper changeLogsRequestHelper) {
        long token = changeLogTokenRequest.getRowIdChangeLogs();
        WhereClauses whereClause =
                new WhereClauses(AND)
                        .addWhereGreaterThanClause(PRIMARY_COLUMN_NAME, String.valueOf(token));
        if (!changeLogTokenRequest.getRecordTypes().isEmpty()) {
            whereClause.addWhereInIntsClause(
                    RECORD_TYPE_COLUMN_NAME, changeLogTokenRequest.getRecordTypes());
        }

        if (!changeLogTokenRequest.getPackageNamesToFilter().isEmpty()) {
            whereClause.addWhereInLongsClause(
                    APP_ID_COLUMN_NAME,
                    appInfoHelper.getAppInfoIds(changeLogTokenRequest.getPackageNamesToFilter()));
        }

        // We set limit size to requested pageSize plus extra 1 record so that if number of records
        // queried is more than pageSize we know there are more records available to return for the
        // next read.
        int pageSize = changeLogsRequest.getPageSize();
        final ReadTableRequest readTableRequest =
                new ReadTableRequest(TABLE_NAME)
                        .setWhereClause(whereClause)
                        .setLimit(pageSize + 1)
                        .setOrderBy(
                                new OrderByClause()
                                        .addOrderByClause(
                                                PRIMARY_COLUMN_NAME, /* isAscending= */ true));

        List<ChangeLogsResponse.ChangeLogRow> changeLogRows = new ArrayList<>();
        long nextChangesToken = DEFAULT_LONG;
        boolean hasMoreRecords = false;
        try (Cursor cursor = mTransactionManager.read(readTableRequest)) {
            int count = 0;
            while (cursor.moveToNext()) {
                if (count >= pageSize) {
                    hasMoreRecords = true;
                    break;
                }
                var row = ChangeLogsResponse.ChangeLogRow.readFromCursor(cursor);
                changeLogRows.add(row);
                count += row.recordIdList().size();
                nextChangesToken = getCursorInt(cursor, PRIMARY_COLUMN_NAME);
            }
        }

        String nextToken =
                nextChangesToken != DEFAULT_LONG
                        ? changeLogsRequestHelper.getNextPageToken(
                                changeLogTokenRequest, nextChangesToken)
                        : changeLogsRequest.getToken();

        return new ChangeLogsResponse(changeLogRows, nextToken, hasMoreRecords);
    }

    public long getLatestRowId() {
        return mTransactionManager.runWithoutTransaction(
                db -> {
                    return StorageUtils.getLastRowIdFor(db, TABLE_NAME);
                });
    }

    /**
     * Change logs to be written to the database.
     *
     * <p>Change logs are grouped by {@link RecordGrouping} and stored in a single row per group.
     */
    public static final class ChangeLogsTableRequests {
        private final Map<RecordGrouping, List<UUID>> mRecordGroups = new ArrayMap<>();
        @OperationType.OperationTypes private final int mOperationType;
        private final Instant mChangeLogTimeStamp;

        /**
         * Creates a change logs object used to add a new change log for {@code operationType}
         * logged at time {@code timeStamp }
         *
         * @param operationType Type of the operation for which change log is added whether insert
         *     or delete.
         * @param timeStamp Time when the change log is added.
         */
        private ChangeLogsTableRequests(
                @OperationType.OperationTypes int operationType, Instant timeStamp) {
            mOperationType = operationType;
            mChangeLogTimeStamp = timeStamp;
        }

        /** Create for {@link OperationType#OPERATION_TYPE_UPSERT} */
        public static ChangeLogsTableRequests ofUpsertion(Instant timeStamp) {
            return new ChangeLogsTableRequests(OperationType.OPERATION_TYPE_UPSERT, timeStamp);
        }

        /** Create for {@link OperationType#OPERATION_TYPE_DELETE} */
        public static ChangeLogsTableRequests ofDeletion(Instant timeStamp) {
            return new ChangeLogsTableRequests(OperationType.OPERATION_TYPE_DELETE, timeStamp);
        }

        /** Add a record to the list of changes */
        public void addRecordInfo(
                @RecordTypeIdentifier.RecordType int recordType, long appId, UUID uuid) {
            var recordGrouping = new RecordGrouping(recordType, appId);
            mRecordGroups.computeIfAbsent(recordGrouping, k -> new ArrayList<>()).add(uuid);
        }

        /**
         * @return List of {@link UpsertTableRequest} for change log table as per {@code
         *     mRecordTypeAndAppIdPairToUUIDMap}
         */
        public List<UpsertTableRequest> getUpsertTableRequests() {
            List<UpsertTableRequest> requests = new ArrayList<>(mRecordGroups.size());
            mRecordGroups.forEach(
                    (recordGrouping, uuids) -> {
                        for (int i = 0; i < uuids.size(); i += DEFAULT_PAGE_SIZE) {
                            ContentValues contentValues = new ContentValues();
                            contentValues.put(RECORD_TYPE_COLUMN_NAME, recordGrouping.recordType());
                            contentValues.put(APP_ID_COLUMN_NAME, recordGrouping.appId());
                            contentValues.put(OPERATION_TYPE_COLUMN_NAME, mOperationType);
                            contentValues.put(TIME_COLUMN_NAME, mChangeLogTimeStamp.toEpochMilli());
                            contentValues.put(
                                    UUIDS_COLUMN_NAME,
                                    StorageUtils.getSingleByteArray(
                                            uuids.subList(
                                                    i, min(i + DEFAULT_PAGE_SIZE, uuids.size()))));
                            requests.add(new UpsertTableRequest(TABLE_NAME, contentValues));
                        }
                    });
            return requests;
        }

        private record RecordGrouping(
                @RecordTypeIdentifier.RecordType int recordType, long appId) {}
    }

    /** Change logs that are read from the database. */
    public static final class ChangeLogsResponse {
        private final List<DeletedLog> mDeletedLogs;
        private final Map<Integer, List<UUID>> mRecordTypeToUpsertedUuids;
        private final String mNextPageToken;
        private final boolean mHasMorePages;

        public ChangeLogsResponse(
                List<ChangeLogRow> rows, String nextPageToken, boolean hasMorePages) {
            mDeletedLogs = filterDeletedLogs(rows);
            mRecordTypeToUpsertedUuids = toRecordTypeToUpsertedUuids(rows);
            mNextPageToken = nextPageToken;
            mHasMorePages = hasMorePages;
        }

        private static List<DeletedLog> filterDeletedLogs(List<ChangeLogRow> rows) {
            return rows.stream()
                    .filter(row -> row.operationType() == OperationType.OPERATION_TYPE_DELETE)
                    .flatMap(
                            row ->
                                    row.recordIdList().stream()
                                            .map(
                                                    recordId ->
                                                            new DeletedLog(
                                                                    recordId.toString(),
                                                                    row.timeStamp()
                                                                            .toEpochMilli())))
                    .toList();
        }

        private static Map<Integer, List<UUID>> toRecordTypeToUpsertedUuids(
                List<ChangeLogRow> rows) {
            return rows.stream()
                    .filter(row -> row.operationType() == OperationType.OPERATION_TYPE_UPSERT)
                    .collect(
                            Collectors.groupingBy(
                                    ChangeLogRow::recordType,
                                    Collectors.flatMapping(
                                            row -> row.recordIdList().stream(),
                                            Collectors.toList())));
        }

        public List<DeletedLog> getDeletedLogs() {
            return mDeletedLogs;
        }

        public Map<Integer, List<UUID>> getRecordTypeToUpsertedUuids() {
            return mRecordTypeToUpsertedUuids;
        }

        /** Returns the next page token for the change logs */
        public String getNextPageToken() {
            return mNextPageToken;
        }

        /** Returns true if there are more change logs to be read */
        public boolean hasMorePages() {
            return mHasMorePages;
        }

        private record ChangeLogRow(
                Instant timeStamp,
                int operationType,
                @RecordTypeIdentifier.RecordType int recordType,
                List<UUID> recordIdList) {

            private static ChangeLogRow readFromCursor(Cursor cursor) {
                var timeStamp = Instant.ofEpochMilli(getCursorLong(cursor, TIME_COLUMN_NAME));
                @OperationType.OperationTypes
                int operationType = getCursorInt(cursor, OPERATION_TYPE_COLUMN_NAME);
                @RecordTypeIdentifier.RecordType
                int recordType = getCursorInt(cursor, RECORD_TYPE_COLUMN_NAME);
                List<UUID> uuidList = StorageUtils.getCursorUUIDList(cursor, UUIDS_COLUMN_NAME);
                return new ChangeLogRow(timeStamp, operationType, recordType, uuidList);
            }
        }
    }
}
