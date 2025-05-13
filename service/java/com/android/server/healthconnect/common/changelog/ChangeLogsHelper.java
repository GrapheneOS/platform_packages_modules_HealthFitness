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

package com.android.server.healthconnect.common.changelog;

import static android.health.connect.Constants.DEFAULT_LONG;
import static android.health.connect.Constants.DEFAULT_PAGE_SIZE;

import static com.android.server.healthconnect.common.changelog.ChangeLogsRequestHelper.getChangeLogRetentionDuration;
import static com.android.server.healthconnect.fitness.recordhelpers.RecordHelper.PRIMARY_COLUMN_NAME;
import static com.android.server.healthconnect.storage.utils.StorageUtils.BLOB_NON_NULL;
import static com.android.server.healthconnect.storage.utils.StorageUtils.INTEGER;
import static com.android.server.healthconnect.storage.utils.StorageUtils.PRIMARY_AUTOINCREMENT;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorBlob;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorInt;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorLong;
import static com.android.server.healthconnect.storage.utils.WhereClauses.LogicalOperator.AND;
import static com.android.server.healthconnect.storage.utils.WhereClauses.LogicalOperator.OR;

import static java.lang.Integer.min;

import android.content.ContentValues;
import android.database.Cursor;
import android.health.connect.MedicalResourceId;
import android.health.connect.accesslog.AccessLog.OperationType;
import android.health.connect.changelog.ChangeLogsRequest;
import android.health.connect.changelog.ChangeLogsResponse.DeletedLog;
import android.health.connect.changelog.ChangeLogsResponse.DeletedMedicalResource;
import android.health.connect.datatypes.MedicalResource;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.util.ArrayMap;
import android.util.Pair;

import com.android.internal.annotations.VisibleForTesting;
import com.android.server.healthconnect.common.metadata.AppInfoHelper;
import com.android.server.healthconnect.proto.serialization.MedicalResourceIdList;
import com.android.server.healthconnect.storage.DatabaseHelper;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.request.AlterTableRequest;
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
import java.util.Objects;
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

    @VisibleForTesting
    public static final String MEDICAL_RESOURCE_TYPE_COLUMN_NAME = "medical_resource_type";

    @VisibleForTesting
    public static final String MEDICAL_DATA_SOURCE_ID_COLUMN_NAME = "medical_data_source_id";

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

    /** Adds the required columns for the PHR change logs feature. */
    public static AlterTableRequest getAlterTableRequestForPhrChangeLogs() {
        var columns =
                List.of(
                        new Pair<>(MEDICAL_RESOURCE_TYPE_COLUMN_NAME, INTEGER),
                        new Pair<>(MEDICAL_DATA_SOURCE_ID_COLUMN_NAME, INTEGER));
        return new AlterTableRequest(TABLE_NAME, columns)
                .createIndexOn(MEDICAL_RESOURCE_TYPE_COLUMN_NAME)
                .createIndexOn(MEDICAL_DATA_SOURCE_ID_COLUMN_NAME);
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
        WhereClauses whereClause =
                new WhereClauses(AND)
                        .addWhereGreaterThanClause(
                                PRIMARY_COLUMN_NAME,
                                String.valueOf(changeLogTokenRequest.getRowIdChangeLogs()));

        WhereClauses dataTypeClauses = new WhereClauses(OR);
        if (!changeLogTokenRequest.getRecordTypes().isEmpty()) {
            dataTypeClauses.addWhereInIntsClause(
                    RECORD_TYPE_COLUMN_NAME, changeLogTokenRequest.getRecordTypes());
        }
        if (!changeLogTokenRequest.getMedicalResourceTypes().isEmpty()) {
            dataTypeClauses.addWhereInIntsClause(
                    MEDICAL_RESOURCE_TYPE_COLUMN_NAME,
                    changeLogTokenRequest.getMedicalResourceTypes());
        }
        whereClause.addNestedWhereClauses(dataTypeClauses);

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
                count += row.count();
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
        private final Map<MedicalResourceGrouping, List<MedicalResourceId>> mMedicalResourceGroups =
                new ArrayMap<>();
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

        /** Add a medical resource to the list of changes */
        public void addMedicalResourceInfo(
                @MedicalResource.MedicalResourceType int resourceType,
                long appId,
                MedicalResourceId medicalResourceId) {
            var medicalResourceGrouping =
                    new MedicalResourceGrouping(
                            resourceType, appId, medicalResourceId.getDataSourceId());
            mMedicalResourceGroups
                    .computeIfAbsent(medicalResourceGrouping, k -> new ArrayList<>())
                    .add(medicalResourceId);
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
            mMedicalResourceGroups.forEach(
                    (medicalResourceGrouping, medicalResourceIds) -> {
                        for (int i = 0; i < medicalResourceIds.size(); i += DEFAULT_PAGE_SIZE) {
                            ContentValues contentValues = new ContentValues();
                            contentValues.put(
                                    MEDICAL_RESOURCE_TYPE_COLUMN_NAME,
                                    medicalResourceGrouping.resourceType());
                            contentValues.put(
                                    MEDICAL_DATA_SOURCE_ID_COLUMN_NAME,
                                    medicalResourceGrouping.medicalDataSourceId());
                            contentValues.put(APP_ID_COLUMN_NAME, medicalResourceGrouping.appId());
                            contentValues.put(OPERATION_TYPE_COLUMN_NAME, mOperationType);
                            contentValues.put(TIME_COLUMN_NAME, mChangeLogTimeStamp.toEpochMilli());
                            contentValues.put(
                                    UUIDS_COLUMN_NAME,
                                    toByteArray(
                                            medicalResourceIds.subList(
                                                    i,
                                                    min(
                                                            i + DEFAULT_PAGE_SIZE,
                                                            medicalResourceIds.size()))));
                            requests.add(new UpsertTableRequest(TABLE_NAME, contentValues));
                        }
                    });
            return requests;
        }

        // The following record classes are implementing equals and hashcode because the default
        // implementations are extremely slow and are occasionally timing out some tests.
        // https://stackoverflow.com/q/77514303

        private record RecordGrouping(@RecordTypeIdentifier.RecordType int recordType, long appId) {
            @Override
            public boolean equals(Object o) {
                return o instanceof RecordGrouping that
                        && appId == that.appId
                        && recordType == that.recordType;
            }

            @Override
            public int hashCode() {
                return Objects.hash(recordType, appId);
            }
        }

        private record MedicalResourceGrouping(
                @MedicalResource.MedicalResourceType int resourceType,
                long appId,
                String medicalDataSourceId) {
            @Override
            public boolean equals(Object o) {
                return o instanceof MedicalResourceGrouping that
                        && appId == that.appId
                        && resourceType == that.resourceType
                        && Objects.equals(medicalDataSourceId, that.medicalDataSourceId);
            }

            @Override
            public int hashCode() {
                return Objects.hash(resourceType, appId, medicalDataSourceId);
            }
        }
    }

    /** Change logs that are read from the database. */
    public static final class ChangeLogsResponse {
        private final List<DeletedLog> mDeletedLogs;
        private final Map<Integer, List<UUID>> mRecordTypeToUpsertedUuids;
        private final List<DeletedMedicalResource> mDeletedMedicalResources;
        private final List<MedicalResourceId> mUpsertedMedicalResourceIds;
        private final String mNextPageToken;
        private final boolean mHasMorePages;

        private ChangeLogsResponse(
                List<ChangeLogRow> rows, String nextPageToken, boolean hasMorePages) {
            mDeletedLogs = filterDeletedLogs(rows);
            mRecordTypeToUpsertedUuids = toRecordTypeToUpsertedUuids(rows);
            mDeletedMedicalResources = toDeletedMedicalResources(rows);
            mUpsertedMedicalResourceIds = toUpsertedMedicalResourceIds(rows);
            mNextPageToken = nextPageToken;
            mHasMorePages = hasMorePages;
        }

        private static List<DeletedLog> filterDeletedLogs(List<ChangeLogRow> rows) {
            return rows.stream()
                    .filter(row -> row.operationType() == OperationType.OPERATION_TYPE_DELETE)
                    .filter(ChangeLogRecordRow.class::isInstance)
                    .map(ChangeLogRecordRow.class::cast)
                    .flatMap(
                            row ->
                                    row.recordIdList().stream()
                                            .map(
                                                    recordId ->
                                                            new DeletedLog(
                                                                    recordId.toString(),
                                                                    row.timeStamp())))
                    .toList();
        }

        private static Map<Integer, List<UUID>> toRecordTypeToUpsertedUuids(
                List<ChangeLogRow> rows) {
            return rows.stream()
                    .filter(row -> row.operationType() == OperationType.OPERATION_TYPE_UPSERT)
                    .filter(ChangeLogRecordRow.class::isInstance)
                    .map(ChangeLogRecordRow.class::cast)
                    .collect(
                            Collectors.groupingBy(
                                    ChangeLogRecordRow::recordType,
                                    Collectors.flatMapping(
                                            row -> row.recordIdList().stream(),
                                            Collectors.toList())));
        }

        private static List<DeletedMedicalResource> toDeletedMedicalResources(
                List<ChangeLogRow> rows) {
            return rows.stream()
                    .filter(row -> row.operationType() == OperationType.OPERATION_TYPE_DELETE)
                    .filter(ChangeLogMedicalResourceRow.class::isInstance)
                    .map(ChangeLogMedicalResourceRow.class::cast)
                    .flatMap(
                            row ->
                                    row.medicalResourceIdList().stream()
                                            .map(
                                                    medicalResourceId ->
                                                            new DeletedMedicalResource(
                                                                    medicalResourceId,
                                                                    row.timeStamp())))
                    .toList();
        }

        private static List<MedicalResourceId> toUpsertedMedicalResourceIds(
                List<ChangeLogRow> rows) {
            return rows.stream()
                    .filter(row -> row.operationType() == OperationType.OPERATION_TYPE_UPSERT)
                    .filter(ChangeLogMedicalResourceRow.class::isInstance)
                    .map(ChangeLogMedicalResourceRow.class::cast)
                    .flatMap(row -> row.medicalResourceIdList().stream())
                    .toList();
        }

        public List<DeletedLog> getDeletedLogs() {
            return mDeletedLogs;
        }

        public Map<Integer, List<UUID>> getRecordTypeToUpsertedUuids() {
            return mRecordTypeToUpsertedUuids;
        }

        public List<DeletedMedicalResource> getDeletedMedicalResources() {
            return mDeletedMedicalResources;
        }

        public List<MedicalResourceId> getUpsertedMedicalResourceIds() {
            return mUpsertedMedicalResourceIds;
        }

        /** Returns the next page token for the change logs */
        public String getNextPageToken() {
            return mNextPageToken;
        }

        /** Returns true if there are more change logs to be read */
        public boolean hasMorePages() {
            return mHasMorePages;
        }

        private sealed interface ChangeLogRow {
            Instant timeStamp();

            int operationType();

            int count();

            private static ChangeLogRow readFromCursor(Cursor cursor) {
                var timeStamp = Instant.ofEpochMilli(getCursorLong(cursor, TIME_COLUMN_NAME));
                @OperationType.OperationTypes
                int operationType = getCursorInt(cursor, OPERATION_TYPE_COLUMN_NAME);
                if (!cursor.isNull(cursor.getColumnIndexOrThrow(RECORD_TYPE_COLUMN_NAME))) {
                    @RecordTypeIdentifier.RecordType
                    int recordType = getCursorInt(cursor, RECORD_TYPE_COLUMN_NAME);
                    List<UUID> recordIdList =
                            StorageUtils.getCursorUUIDList(cursor, UUIDS_COLUMN_NAME);
                    return new ChangeLogRecordRow(
                            timeStamp, operationType, recordType, recordIdList);
                } else if (!cursor.isNull(
                        cursor.getColumnIndexOrThrow(MEDICAL_RESOURCE_TYPE_COLUMN_NAME))) {
                    @MedicalResource.MedicalResourceType
                    int medicalResourceType =
                            getCursorInt(cursor, MEDICAL_RESOURCE_TYPE_COLUMN_NAME);
                    List<MedicalResourceId> medicalResourceIdList =
                            toMedicalResourceIdList(getCursorBlob(cursor, UUIDS_COLUMN_NAME));
                    return new ChangeLogMedicalResourceRow(
                            timeStamp, operationType, medicalResourceType, medicalResourceIdList);
                }
                throw new IllegalStateException("Invalid change log row");
            }
        }

        private record ChangeLogRecordRow(
                Instant timeStamp,
                int operationType,
                @RecordTypeIdentifier.RecordType int recordType,
                List<UUID> recordIdList)
                implements ChangeLogRow {
            @Override
            public int count() {
                return recordIdList.size();
            }
        }

        private record ChangeLogMedicalResourceRow(
                Instant timeStamp,
                int operationType,
                @MedicalResource.MedicalResourceType int medicalResourceType,
                List<MedicalResourceId> medicalResourceIdList)
                implements ChangeLogRow {
            @Override
            public int count() {
                return medicalResourceIdList.size();
            }
        }
    }

    @VisibleForTesting
    static byte[] toByteArray(List<MedicalResourceId> medicalResourceIdList) {
        return MedicalResourceIdList.newBuilder()
                .addAllMedicalResourceId(
                        medicalResourceIdList.stream()
                                .map(
                                        id ->
                                                com.android.server.healthconnect.proto.serialization
                                                        .MedicalResourceId.newBuilder()
                                                        .setDataSourceId(id.getDataSourceId())
                                                        .setFhirResourceId(id.getFhirResourceId())
                                                        .setFhirResourceType(
                                                                id.getFhirResourceType())
                                                        .build())
                                .toList())
                .build()
                .toByteArray();
    }

    @VisibleForTesting
    public static List<MedicalResourceId> toMedicalResourceIdList(byte[] byteArray) {
        try {
            return MedicalResourceIdList.parseFrom(byteArray).getMedicalResourceIdList().stream()
                    .map(
                            id ->
                                    new MedicalResourceId(
                                            id.getDataSourceId(),
                                            id.getFhirResourceType(),
                                            id.getFhirResourceId()))
                    .toList();
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}
