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

import static android.health.connect.Constants.DEFAULT_INT;
import static android.health.connect.Constants.DEFAULT_LONG;

import static com.android.server.healthconnect.fitness.recordhelpers.RecordHelper.PRIMARY_COLUMN_NAME;
import static com.android.server.healthconnect.storage.utils.StorageUtils.INTEGER_NOT_NULL;
import static com.android.server.healthconnect.storage.utils.StorageUtils.PRIMARY_AUTOINCREMENT;
import static com.android.server.healthconnect.storage.utils.StorageUtils.TEXT_NOT_NULL;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorInt;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorLong;
import static com.android.server.healthconnect.storage.utils.WhereClauses.LogicalOperator.AND;

import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.health.connect.internal.datatypes.RecordInternal;
import android.util.Pair;
import android.util.Slog;

import androidx.annotation.VisibleForTesting;

import com.android.healthfitness.flags.AconfigFlagHelper;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.request.CreateTableRequest;
import com.android.server.healthconnect.storage.request.DeleteTableRequest;
import com.android.server.healthconnect.storage.request.ReadTableRequest;
import com.android.server.healthconnect.storage.request.UpsertTableRequest;
import com.android.server.healthconnect.storage.utils.OrderByClause;
import com.android.server.healthconnect.storage.utils.WhereClauses;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Table to maintain detailed read access logs.
 *
 * @hide
 */
public class ReadAccessLogsHelper extends DatabaseHelper {

    public static final String TABLE_NAME = "read_access_logs_table";
    private static final int NUM_COLS = 6;
    private static final int TIME_WINDOW_DAYS = 30;
    private static final int DATA_RETENTION_PERIOD_DAYS = 31;
    @VisibleForTesting static final int PAGE_SIZE = 1000;
    private static final String RECORD_TYPE_COLUMN_NAME = "record_type";
    private static final String READER_APP_ID_COLUMN_NAME = "reader_app_id";
    private static final String WRITER_APP_ID_COLUMN_NAME = "writer_app_id";
    private static final String READ_TIME = "read_time";
    private static final String WAS_READ_RECORD_WRITTEN_IN_PAST_30_DAYS = "write_time";

    private static final String TAG = "HCReadAccessLogsHelper";

    private final AppInfoHelper mAppInfoHelper;
    private final TransactionManager mTransactionManager;

    public ReadAccessLogsHelper(
            AppInfoHelper appInfoHelper,
            TransactionManager transactionManager,
            DatabaseHelpers databaseHelpers) {
        super(databaseHelpers);
        mAppInfoHelper = appInfoHelper;
        mTransactionManager = transactionManager;
    }

    protected String getMainTableName() {
        return TABLE_NAME;
    }

    @Override
    public synchronized void clearData(TransactionManager transactionManager) {
        if (!AconfigFlagHelper.isEcosystemMetricsEnabled()) {
            return;
        }
        super.clearData(transactionManager);
    }

    /**
     * Returns an instance of {@link DeleteTableRequest} to delete entries in the table older than
     * the retention period.
     */
    public static DeleteTableRequest getDeleteRequestForAutoDelete() {
        return new DeleteTableRequest(TABLE_NAME)
                .setTimeFilter(
                        READ_TIME,
                        Instant.EPOCH.toEpochMilli(),
                        Instant.now()
                                .minus(DATA_RETENTION_PERIOD_DAYS, ChronoUnit.DAYS)
                                .toEpochMilli());
    }

    public static CreateTableRequest getCreateTableRequest() {
        return new CreateTableRequest(TABLE_NAME, getColumnInfo())
                .addForeignKey(
                        /* referencedTable= */ AppInfoHelper.TABLE_NAME,
                        /* columnNames= */ List.of(READER_APP_ID_COLUMN_NAME),
                        /* referencedColumnNames= */ List.of(PRIMARY_COLUMN_NAME))
                .addForeignKey(
                        /* referencedTable= */ AppInfoHelper.TABLE_NAME,
                        /* columnNames= */ List.of(WRITER_APP_ID_COLUMN_NAME),
                        /* referencedColumnNames= */ List.of(PRIMARY_COLUMN_NAME));
    }

    private static List<Pair<String, String>> getColumnInfo() {
        List<Pair<String, String>> columnInfo = new ArrayList<>(NUM_COLS);
        columnInfo.add(new Pair<>(PRIMARY_COLUMN_NAME, PRIMARY_AUTOINCREMENT));
        columnInfo.add(new Pair<>(READER_APP_ID_COLUMN_NAME, INTEGER_NOT_NULL));
        columnInfo.add(new Pair<>(WRITER_APP_ID_COLUMN_NAME, INTEGER_NOT_NULL));
        columnInfo.add(new Pair<>(RECORD_TYPE_COLUMN_NAME, TEXT_NOT_NULL));
        columnInfo.add(new Pair<>(READ_TIME, INTEGER_NOT_NULL));
        columnInfo.add(new Pair<>(WAS_READ_RECORD_WRITTEN_IN_PAST_30_DAYS, INTEGER_NOT_NULL));
        return columnInfo;
    }

    /**
     * Returns a list of all {@link ReadAccessLog} DO NOT CALL WITHOUT FLAGGING UNDER {@link
     * AconfigFlagHelper.isEcosystemMetricsEnabled}.
     *
     * @param rowId zero if the table is being read from the start.
     */
    public ReadAccessLogsResponse queryReadAccessLogs(int rowId) {
        WhereClauses whereClause =
                new WhereClauses(AND).addWhereGreaterThanOrEqualClause(PRIMARY_COLUMN_NAME, rowId);
        OrderByClause orderByClause =
                new OrderByClause().addOrderByClause(PRIMARY_COLUMN_NAME, /* isAscending= */ true);

        // We set limit size to requested PAGE_SIZE plus extra 1 record so that if number of
        // records queried is more than PAGE_SIZE we know there are more records available to return
        // for the next read.
        final ReadTableRequest readTableRequest =
                new ReadTableRequest(TABLE_NAME)
                        .setWhereClause(whereClause)
                        .setOrderBy(orderByClause)
                        .setLimit(PAGE_SIZE + 1);
        int nextRowId = DEFAULT_INT;
        int count = 0;
        boolean hasMoreRecords = false;
        List<ReadAccessLog> readAccessLogList = new ArrayList<>();
        try (Cursor cursor = mTransactionManager.read(readTableRequest)) {
            while (cursor.moveToNext()) {
                try {
                    if (count == PAGE_SIZE) {
                        hasMoreRecords = true;
                        break;
                    }
                    count++;
                    String readerPackage =
                            mAppInfoHelper.getPackageName(
                                    getCursorLong(cursor, READER_APP_ID_COLUMN_NAME));
                    String writerPackage =
                            mAppInfoHelper.getPackageName(
                                    getCursorLong(cursor, WRITER_APP_ID_COLUMN_NAME));
                    long readTimeStamp = getCursorLong(cursor, READ_TIME);
                    boolean isRecordWithinPast30Days =
                            getCursorInt(cursor, WAS_READ_RECORD_WRITTEN_IN_PAST_30_DAYS) > 0;
                    int dataType = getCursorInt(cursor, RECORD_TYPE_COLUMN_NAME);

                    readAccessLogList.add(
                            new ReadAccessLog(
                                    /* readerPackage= */ readerPackage,
                                    /* writerPackage= */ writerPackage,
                                    /* dataType= */ dataType,
                                    /* readTimeStamp= */ readTimeStamp,
                                    /* isRecordWithinPast30Days= */ isRecordWithinPast30Days));
                    nextRowId = getCursorInt(cursor, PRIMARY_COLUMN_NAME) + 1;
                } catch (PackageManager.NameNotFoundException e) {
                    Slog.e(TAG, "encounter null package name while query access logs");
                }
            }
        }

        return new ReadAccessLogsResponse(
                Collections.unmodifiableList(readAccessLogList), nextRowId, hasMoreRecords);
    }

    /**
     * Stores read access logs for given package names. DO NOT INSERT ACCESS LOGS FOR SELF READ and
     * DO NOT CALL WITHOUT FLAGGING UNDER {@link AconfigFlagHelper.isEcosystemMetricsEnabled}.
     */
    public void recordAccessLogForAggregationReads(
            SQLiteDatabase db,
            String readerPackageName,
            long readTimeStamp,
            @RecordTypeIdentifier.RecordType int recordTypeId,
            long endTimeStamp,
            List<String> writingPackageNames) {
        long readerAppInfoId = mAppInfoHelper.getAppInfoId(readerPackageName);
        if (readerAppInfoId == DEFAULT_LONG) {
            Slog.e(TAG, "invalid package name " + readerPackageName + " used for read access log");
            return;
        }
        for (String writingPackageName : writingPackageNames) {
            long writerAppInfoId = mAppInfoHelper.getAppInfoId(writingPackageName);
            if (writerAppInfoId == DEFAULT_LONG || writerAppInfoId == readerAppInfoId) {
                continue;
            }

            ContentValues contentValues =
                    populateCommonColumns(
                            /* readerAppInfoId= */ readerAppInfoId,
                            /* dataType= */ recordTypeId,
                            /* writerAppInfoId= */ writerAppInfoId,
                            /* writeTimeStamp= */ endTimeStamp,
                            /* readTimeStamp' */ readTimeStamp);
            UpsertTableRequest upsertTableRequest =
                    new UpsertTableRequest(TABLE_NAME, contentValues);
            mTransactionManager.insert(db, upsertTableRequest);
        }
    }

    /**
     * Stores read access logs for given records. DO NOT INSERT ACCESS LOGS FOR SELF READ and DO NOT
     * CALL WITHOUT FLAGGING UNDER {@link AconfigFlagHelper.isEcosystemMetricsEnabled}.
     */
    public void recordAccessLogForNonAggregationReads(
            SQLiteDatabase db,
            String readerPackageName,
            long readTimeStamp,
            List<RecordInternal<?>> recordsRead) {
        long readerAppInfoId = mAppInfoHelper.getAppInfoId(readerPackageName);
        if (readerAppInfoId == DEFAULT_LONG) {
            Slog.e(TAG, "invalid package name " + readerPackageName + " used for read access log");
            return;
        }
        Map<Integer, Map<Long, Long>> datatypeToLatestWritePerPackageName =
                processRecordsIntoLogs(recordsRead, readerAppInfoId);

        for (Map.Entry<Integer, Map<Long, Long>> datatypeToLatestWritePerPackageNameEntry :
                datatypeToLatestWritePerPackageName.entrySet()) {
            for (Map.Entry<Long, Long> packageNameToTimeStamp :
                    datatypeToLatestWritePerPackageNameEntry.getValue().entrySet()) {
                ContentValues contentValues =
                        populateCommonColumns(
                                /* readerAppInfoId= */ readerAppInfoId,
                                /* dataType= */ datatypeToLatestWritePerPackageNameEntry.getKey(),
                                /* writerAppInfoId= */ packageNameToTimeStamp.getKey(),
                                /* writeTimeStamp= */ packageNameToTimeStamp.getValue(),
                                /* readTimeStamp' */ readTimeStamp);
                UpsertTableRequest upsertTableRequest =
                        new UpsertTableRequest(TABLE_NAME, contentValues);
                mTransactionManager.insert(db, upsertTableRequest);
            }
        }
    }

    private Map<Integer, Map<Long, Long>> processRecordsIntoLogs(
            List<RecordInternal<?>> recordInternals, long readerAppInfoId) {
        // We only need to store latest entry for each package name and datatype pairing
        // datatype -> package name (app id) -> latest timestamp
        Map<Integer, Map<Long, Long>> datatypeToLatestWritePerPackageName = new HashMap<>();

        for (RecordInternal<?> recordInternal : recordInternals) {
            int dataType = recordInternal.getRecordType();
            long appInfoId = recordInternal.getAppInfoId();
            long recordTimeStamp = recordInternal.getRecordTime();
            if (appInfoId == DEFAULT_LONG
                    || recordTimeStamp == DEFAULT_LONG
                    || appInfoId == readerAppInfoId) {
                continue;
            }
            datatypeToLatestWritePerPackageName.putIfAbsent(dataType, new HashMap<>());
            Map<Long, Long> packageNameToLatestTimeStamp =
                    datatypeToLatestWritePerPackageName.get(dataType);
            long latestTimeStamp =
                    packageNameToLatestTimeStamp.getOrDefault(appInfoId, DEFAULT_LONG);
            if (recordTimeStamp > latestTimeStamp) {
                packageNameToLatestTimeStamp.put(appInfoId, recordTimeStamp);
            }
        }

        return datatypeToLatestWritePerPackageName;
    }

    /** Populates values for insert into db. */
    @VisibleForTesting
    public static ContentValues populateCommonColumns(
            long readerAppInfoId,
            int dataType,
            long writerAppInfoId,
            long writeTimeStamp,
            long readTimeStamp) {

        boolean wasReadRecordWrittenInPast30Days =
                writeTimeStamp
                        >= Instant.ofEpochMilli(readTimeStamp)
                                .minus(TIME_WINDOW_DAYS, ChronoUnit.DAYS)
                                .toEpochMilli();

        ContentValues contentValues = new ContentValues();
        contentValues.put(READER_APP_ID_COLUMN_NAME, readerAppInfoId);
        contentValues.put(RECORD_TYPE_COLUMN_NAME, dataType);
        contentValues.put(WRITER_APP_ID_COLUMN_NAME, writerAppInfoId);
        contentValues.put(READ_TIME, readTimeStamp);
        contentValues.put(
                WAS_READ_RECORD_WRITTEN_IN_PAST_30_DAYS, wasReadRecordWrittenInPast30Days);
        return contentValues;
    }

    public static class ReadAccessLog {
        private final String mReaderPackage;
        private final String mWriterPackage;
        private final int mDataType;
        private final long mReadTimeStamp;
        private final boolean mIsRecordWithinPast30Days;

        public ReadAccessLog(
                String readerPackage,
                String writerPackage,
                @RecordTypeIdentifier.RecordType int dataType,
                long readTimeStamp,
                boolean isRecordWithinPast30Days) {
            this.mReaderPackage = readerPackage;
            this.mWriterPackage = writerPackage;
            this.mDataType = dataType;
            this.mReadTimeStamp = readTimeStamp;
            this.mIsRecordWithinPast30Days = isRecordWithinPast30Days;
        }

        public String getReaderPackage() {
            return mReaderPackage;
        }

        @RecordTypeIdentifier.RecordType
        public int getDataType() {
            return mDataType;
        }

        public String getWriterPackage() {
            return mWriterPackage;
        }

        public long getReadTimeStamp() {
            return mReadTimeStamp;
        }

        public boolean getRecordWithinPast30Days() {
            return mIsRecordWithinPast30Days;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ReadAccessLog that)) return false;
            return mDataType == that.mDataType
                    && mReadTimeStamp == that.mReadTimeStamp
                    && mIsRecordWithinPast30Days == that.mIsRecordWithinPast30Days
                    && Objects.equals(mReaderPackage, that.mReaderPackage)
                    && Objects.equals(mWriterPackage, that.mWriterPackage);
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                    mReaderPackage,
                    mWriterPackage,
                    mDataType,
                    mReadTimeStamp,
                    mIsRecordWithinPast30Days);
        }
    }

    public static class ReadAccessLogsResponse {
        private final List<ReadAccessLog> mReadAccessLogs;
        private final int mNextRowId;
        private final boolean mHasMoreRecords;

        public ReadAccessLogsResponse(
                List<ReadAccessLog> readAccessLogs, int nextRowId, boolean hasMoreRecords) {
            mReadAccessLogs = readAccessLogs;
            mNextRowId = nextRowId;
            mHasMoreRecords = hasMoreRecords;
        }

        public int getNextRowId() {
            return mNextRowId;
        }

        public List<ReadAccessLog> getReadAccessLogs() {
            return mReadAccessLogs;
        }

        public boolean hasMoreRecords() {
            return mHasMoreRecords;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ReadAccessLogsResponse that)) return false;
            return Objects.equals(mReadAccessLogs, that.mReadAccessLogs)
                    && mNextRowId == that.mNextRowId
                    && mHasMoreRecords == that.mHasMoreRecords;
        }

        @Override
        public int hashCode() {
            return Objects.hash(mReadAccessLogs, mNextRowId, mHasMoreRecords);
        }
    }
}
