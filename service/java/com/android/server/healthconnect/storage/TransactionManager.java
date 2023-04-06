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

package com.android.server.healthconnect.storage;

import static android.health.connect.Constants.DEFAULT_LONG;
import static android.health.connect.Constants.DEFAULT_PAGE_SIZE;

import static com.android.server.healthconnect.storage.datatypehelpers.RecordHelper.APP_INFO_ID_COLUMN_NAME;
import static com.android.server.healthconnect.storage.datatypehelpers.RecordHelper.PRIMARY_COLUMN_NAME;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorLong;

import android.annotation.NonNull;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.health.connect.Constants;
import android.health.connect.internal.datatypes.RecordInternal;
import android.os.UserHandle;
import android.util.Pair;
import android.util.Slog;

import com.android.server.healthconnect.HealthConnectUserContext;
import com.android.server.healthconnect.storage.datatypehelpers.AppInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.RecordHelper;
import com.android.server.healthconnect.storage.request.AggregateTableRequest;
import com.android.server.healthconnect.storage.request.DeleteTableRequest;
import com.android.server.healthconnect.storage.request.DeleteTransactionRequest;
import com.android.server.healthconnect.storage.request.ReadTableRequest;
import com.android.server.healthconnect.storage.request.ReadTransactionRequest;
import com.android.server.healthconnect.storage.request.UpsertTableRequest;
import com.android.server.healthconnect.storage.request.UpsertTransactionRequest;
import com.android.server.healthconnect.storage.utils.RecordHelperProvider;
import com.android.server.healthconnect.storage.utils.StorageUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * A class to handle all the DB transaction request from the clients. {@link TransactionManager}
 * acts as a layer b/w the DB and the data type helper classes and helps perform actual operations
 * on the DB.
 *
 * @hide
 */
public final class TransactionManager {
    private static final String TAG = "HealthConnectTransactionMan";
    private static final HashMap<UserHandle, HealthConnectDatabase> mUserHandleToDatabaseMap =
            new HashMap<>();
    private static TransactionManager sTransactionManager;
    private HealthConnectDatabase mHealthConnectDatabase;

    private TransactionManager(@NonNull HealthConnectUserContext context) {
        mHealthConnectDatabase = new HealthConnectDatabase(context);
        mUserHandleToDatabaseMap.put(context.getCurrentUserHandle(), mHealthConnectDatabase);
    }

    @NonNull
    public static TransactionManager getInstance(@NonNull HealthConnectUserContext context) {
        if (sTransactionManager == null) {
            sTransactionManager = new TransactionManager(context);
        }

        return sTransactionManager;
    }

    @NonNull
    public static TransactionManager getInitialisedInstance() {
        Objects.requireNonNull(sTransactionManager);

        return sTransactionManager;
    }

    public void onUserUnlocked(@NonNull HealthConnectUserContext healthConnectUserContext) {
        if (!mUserHandleToDatabaseMap.containsKey(
                healthConnectUserContext.getCurrentUserHandle())) {
            mUserHandleToDatabaseMap.put(
                    healthConnectUserContext.getCurrentUserHandle(),
                    new HealthConnectDatabase(healthConnectUserContext));
        }

        mHealthConnectDatabase =
                mUserHandleToDatabaseMap.get(healthConnectUserContext.getCurrentUserHandle());
    }

    /**
     * Inserts all the {@link RecordInternal} in {@code request} into the HealthConnect database.
     *
     * @param request an insert request.
     * @return List of uids of the inserted {@link RecordInternal}, in the same order as they
     *     presented to {@code request}.
     */
    public List<String> insertAll(@NonNull UpsertTransactionRequest request)
            throws SQLiteException {
        if (Constants.DEBUG) {
            Slog.d(TAG, "Inserting " + request.getUpsertRequests().size() + " requests.");
        }

        insertAll(request.getUpsertRequests(), this::insertOrReplaceRecord);
        return request.getUUIdsInOrder();
    }

    /**
     * Inserts or replaces all the {@link UpsertTableRequest} into the HealthConnect database.
     *
     * @param upsertTableRequests a list of insert table requests.
     */
    public void insertOrReplaceAll(@NonNull List<UpsertTableRequest> upsertTableRequests)
            throws SQLiteException {
        insertAll(upsertTableRequests, this::insertOrReplace);
    }

    /**
     * Inserts or ignore on conflicts all the {@link UpsertTableRequest} into the HealthConnect
     * database.
     *
     * @param upsertTableRequests a list of insert table requests.
     */
    public void insertOrIgnoreOnConflict(@NonNull List<UpsertTableRequest> upsertTableRequests) {
        final SQLiteDatabase db = getWritableDb();
        db.beginTransaction();
        try {
            upsertTableRequests.forEach(
                    (upsertTableRequest) -> insertOrIgnore(db, upsertTableRequest));
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Deletes all the {@link RecordInternal} in {@code request} into the HealthConnect database.
     *
     * <p>NOTE: Please don't add logic to explicitly delete child table entries here as they should
     * be deleted via cascade
     *
     * @param request a delete request.
     */
    public int deleteAll(@NonNull DeleteTransactionRequest request) throws SQLiteException {
        final SQLiteDatabase db = getWritableDb();
        db.beginTransaction();
        int numberOfRecordsDeleted = 0;
        try {
            for (DeleteTableRequest deleteTableRequest : request.getDeleteTableRequests()) {
                if (deleteTableRequest.requiresRead()) {
                    /*
                    Delete request needs UUID before the entry can be
                    deleted, fetch and set it in {@code request}
                    */
                    try (Cursor cursor = db.rawQuery(deleteTableRequest.getReadCommand(), null)) {
                        int numberOfUuidsToDelete = 0;
                        while (cursor.moveToNext()) {
                            numberOfUuidsToDelete++;
                            if (deleteTableRequest.requiresPackageCheck()) {
                                request.enforcePackageCheck(
                                        StorageUtils.getCursorString(
                                                cursor, deleteTableRequest.getIdColumnName()),
                                        StorageUtils.getCursorLong(
                                                cursor, deleteTableRequest.getPackageColumnName()));
                            }
                            request.onRecordFetched(
                                    deleteTableRequest.getRecordType(),
                                    StorageUtils.getCursorLong(
                                            cursor, deleteTableRequest.getPackageColumnName()),
                                    StorageUtils.getCursorString(
                                            cursor, deleteTableRequest.getIdColumnName()));
                        }
                        deleteTableRequest.setNumberOfUuidsToDelete(numberOfUuidsToDelete);
                    }
                }
                numberOfRecordsDeleted += deleteTableRequest.getTotalNumberOfRecordsDeleted();
                db.execSQL(deleteTableRequest.getDeleteCommand());
            }

            request.getChangeLogUpsertRequests()
                    .forEach((insertRequest) -> insertRecord(db, insertRequest));

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return numberOfRecordsDeleted;
    }

    /**
     * Handles the aggregation requests for {@code aggregateTableRequest}
     *
     * @param aggregateTableRequest an aggregate request.
     */
    @NonNull
    public void populateWithAggregation(AggregateTableRequest aggregateTableRequest) {
        final SQLiteDatabase db = getReadableDb();
        if (!aggregateTableRequest.getRecordHelper().isRecordOperationsEnabled()) {
            return;
        }
        try (Cursor cursor = db.rawQuery(aggregateTableRequest.getAggregationCommand(), null);
                Cursor metaDataCursor =
                        db.rawQuery(
                                aggregateTableRequest.getCommandToFetchAggregateMetadata(), null)) {
            aggregateTableRequest.onResultsFetched(cursor, metaDataCursor);
        }
    }

    /**
     * Reads the records {@link RecordInternal} stored in the HealthConnect database.
     *
     * @param request a read request.
     * @return List of records read {@link RecordInternal} from table based on ids.
     */
    public List<RecordInternal<?>> readRecords(@NonNull ReadTransactionRequest request)
            throws SQLiteException {
        List<RecordInternal<?>> recordInternals = new ArrayList<>();
        request.getReadRequests()
                .forEach(
                        (readTableRequest -> {
                            if (readTableRequest.getRecordHelper().isRecordOperationsEnabled()) {
                                try (Cursor cursor = read(readTableRequest)) {
                                    Objects.requireNonNull(readTableRequest.getRecordHelper());
                                    List<RecordInternal<?>> internalRecords =
                                            readTableRequest
                                                    .getRecordHelper()
                                                    .getInternalRecords(cursor, DEFAULT_PAGE_SIZE);

                                    populateInternalRecordsWithExtraData(
                                            internalRecords, readTableRequest);

                                    recordInternals.addAll(internalRecords);
                                }
                            }
                        }));
        return recordInternals;
    }

    /**
     * Reads the records {@link RecordInternal} stored in the HealthConnect database and returns the
     * max row_id as next page token.
     *
     * @param request a read request.
     * @return Pair containing records list read {@link RecordInternal} from the table and a next
     *     page token for pagination
     */
    public Pair<List<RecordInternal<?>>, Long> readRecordsAndGetNextToken(
            @NonNull ReadTransactionRequest request) throws SQLiteException {
        // throw an exception if read requested is not for a single record type
        // i.e. size of read table request is not equal to 1.
        if (request.getReadRequests().size() != 1) {
            throw new IllegalArgumentException("Read requested is not for a single record type");
        }
        List<RecordInternal<?>> recordInternalList;
        long token = DEFAULT_LONG;
        ReadTableRequest readTableRequest = request.getReadRequests().get(0);
        RecordHelper<?> helper = readTableRequest.getRecordHelper();
        Objects.requireNonNull(helper);
        if (!helper.isRecordOperationsEnabled()) {
            recordInternalList = new ArrayList<>(0);
            return Pair.create(recordInternalList, token);
        }

        try (Cursor cursor = read(readTableRequest)) {
            recordInternalList = helper.getInternalRecords(cursor, readTableRequest.getPageSize());
            String startTimeColumnName = helper.getStartTimeColumnName();

            populateInternalRecordsWithExtraData(recordInternalList, readTableRequest);
            if (cursor.moveToNext()) {
                token = getCursorLong(cursor, startTimeColumnName);
            }
        }
        return Pair.create(recordInternalList, token);
    }

    /**
     * Inserts record into the table in {@code request} into the HealthConnect database.
     *
     * <p>NOTE: PLEASE ONLY USE THIS FUNCTION IF YOU WANT TO INSERT A SINGLE RECORD PER API. PLEASE
     * DON'T USE THIS FUNCTION INSIDE A FOR LOOP OR REPEATEDLY: The reason is that this function
     * tries to insert a record inside its own transaction and if you are trying to insert multiple
     * things using this method in the same api call, they will all get inserted in their separate
     * transactions and will be less performant. If at all, the requirement is to insert them in
     * different transactions, as they are not related to each, then this method can be used.
     *
     * @param request an insert request.
     * @return rowId of the inserted record.
     */
    public long insert(@NonNull UpsertTableRequest request) {
        final SQLiteDatabase db = getWritableDb();
        return insertRecord(db, request);
    }

    /**
     * Update record into the table in {@code request} into the HealthConnect database.
     *
     * <p>NOTE: PLEASE ONLY USE THIS FUNCTION IF YOU WANT TO UPDATE A SINGLE RECORD PER API. PLEASE
     * DON'T USE THIS FUNCTION INSIDE A FOR LOOP OR REPEATEDLY: The reason is that this function
     * tries to update a record inside its own transaction and if you are trying to insert multiple
     * things using this method in the same api call, they will all get updates in their separate
     * transactions and will be less performant. If at all, the requirement is to update them in
     * different transactions, as they are not related to each, then this method can be used.
     *
     * @param request an update request.
     */
    public void update(@NonNull UpsertTableRequest request) {
        final SQLiteDatabase db = getWritableDb();
        updateRecord(db, request);
    }

    /**
     * Inserts (or updates if the row exists) record into the table in {@code request} into the
     * HealthConnect database.
     *
     * <p>NOTE: PLEASE ONLY USE THIS FUNCTION IF YOU WANT TO UPSERT A SINGLE RECORD. PLEASE DON'T
     * USE THIS FUNCTION INSIDE A FOR LOOP OR REPEATEDLY: The reason is that this function tries to
     * insert a record out of a transaction and if you are trying to insert a record before or after
     * opening up a transaction please rethink if you really want to use this function.
     *
     * <p>NOTE: INSERt+WITH_CONFLICT_REPLACE only works on unique columns, else in case of conflict
     * it leads to abort of the transaction.
     *
     * @param request an insert request.
     * @return rowId of the inserted or updated record.
     */
    public long insertOrReplace(@NonNull UpsertTableRequest request) {
        final SQLiteDatabase db = getWritableDb();
        return insertOrReplaceRecord(db, request);
    }

    /** Note: It is the responsibility of the caller to close the returned cursor */
    @NonNull
    public Cursor read(@NonNull ReadTableRequest request) {
        if (Constants.DEBUG) {
            Slog.d(TAG, "Read query: " + request.getReadCommand());
        }
        return getReadableDb().rawQuery(request.getReadCommand(), null);
    }

    public long getLastRowIdFor(String tableName) {
        final SQLiteDatabase db = getReadableDb();
        try (Cursor cursor = db.rawQuery(StorageUtils.getMaxPrimaryKeyQuery(tableName), null)) {
            cursor.moveToFirst();
            return cursor.getLong(cursor.getColumnIndex(PRIMARY_COLUMN_NAME));
        }
    }

    /**
     * Get number of entries in the given table.
     *
     * @param tableName Name of table
     * @return Number of entries in the given table
     */
    public long getNumberOfEntriesInTheTable(@NonNull String tableName) {
        Objects.requireNonNull(tableName);
        return DatabaseUtils.queryNumEntries(getReadableDb(), tableName);
    }

    /**
     * Size of Health Connect database in bytes.
     *
     * @param context Context
     * @return Size of the database
     */
    public long getDatabaseSize(@NonNull Context context) {
        Objects.requireNonNull(context);
        return context.getDatabasePath(getReadableDb().getPath()).length();
    }

    public void delete(DeleteTableRequest request) {
        final SQLiteDatabase db = getWritableDb();
        db.execSQL(request.getDeleteCommand());
    }

    /**
     * Updates all the {@link RecordInternal} in {@code request} into the HealthConnect database.
     *
     * @param request an update request.
     */
    public void updateAll(@NonNull UpsertTransactionRequest request) {
        final SQLiteDatabase db = getWritableDb();
        db.beginTransaction();
        try {
            request.getUpsertRequests()
                    .forEach((upsertTableRequest) -> updateRecord(db, upsertTableRequest));
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /**
     * @return list of distinct packageNames corresponding to the input table name after querying
     *     the table.
     */
    public HashMap<Integer, HashSet<String>> getDistinctPackageNamesForRecordsTable(
            Set<Integer> recordTypes) throws SQLiteException {
        final SQLiteDatabase db = getReadableDb();
        HashMap<Integer, HashSet<String>> packagesForRecordTypeMap = new HashMap<>();
        for (Integer recordType : recordTypes) {
            RecordHelper<?> recordHelper =
                    RecordHelperProvider.getInstance().getRecordHelper(recordType);
            HashSet<String> packageNamesForDatatype = new HashSet<>();
            try (Cursor cursorForDistinctPackageNames =
                    db.rawQuery(
                            /* sql query */
                            recordHelper
                                    .getReadTableRequestWithDistinctAppInfoIds()
                                    .getReadCommand(),
                            /* selectionArgs */ null)) {
                if (cursorForDistinctPackageNames.getCount() > 0) {
                    AppInfoHelper appInfoHelper = AppInfoHelper.getInstance();
                    while (cursorForDistinctPackageNames.moveToNext()) {
                        String packageName =
                                appInfoHelper.getPackageName(
                                        cursorForDistinctPackageNames.getLong(
                                                cursorForDistinctPackageNames.getColumnIndex(
                                                        APP_INFO_ID_COLUMN_NAME)));
                        if (!packageName.isEmpty()) {
                            packageNamesForDatatype.add(packageName);
                        }
                    }
                }
            }
            packagesForRecordTypeMap.put(recordType, packageNamesForDatatype);
        }
        return packagesForRecordTypeMap;
    }

    /**
     * ONLY DO OPERATIONS IN A SINGLE TRANSACTION HERE
     *
     * <p>This is because this function is called from {@link AutoDeleteService}, and we want to
     * make sure that either all its operation succeed or fail in a single run.
     */
    public void deleteWithoutChangeLogs(@NonNull List<DeleteTableRequest> deleteTableRequests) {
        Objects.requireNonNull(deleteTableRequests);
        final SQLiteDatabase db = getWritableDb();
        db.beginTransaction();
        try {
            for (DeleteTableRequest deleteTableRequest : deleteTableRequests) {
                db.execSQL(deleteTableRequest.getDeleteCommand());
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void onUserSwitching() {
        mHealthConnectDatabase.close();
    }

    private void insertAll(
            @NonNull List<UpsertTableRequest> upsertTableRequests,
            @NonNull BiConsumer<SQLiteDatabase, UpsertTableRequest> insert) {
        final SQLiteDatabase db = getWritableDb();
        db.beginTransaction();
        try {
            upsertTableRequests.forEach(
                    (upsertTableRequest) -> insert.accept(db, upsertTableRequest));
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public <E extends Throwable> void runAsTransaction(TransactionRunnable<E> task) throws E {
        final SQLiteDatabase db = getWritableDb();
        db.beginTransaction();
        try {
            task.run(db);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /** Assumes that caller will be closing {@code db} and handling the transaction if required */
    public long insertRecord(@NonNull SQLiteDatabase db, @NonNull UpsertTableRequest request) {
        long rowId = db.insertOrThrow(request.getTable(), null, request.getContentValues());
        request.getChildTableRequests()
                .forEach(childRequest -> insertRecord(db, childRequest.withParentKey(rowId)));

        return rowId;
    }

    /**
     * Inserts the provided {@link UpsertTableRequest} into the database.
     *
     * <p>Assumes that caller will be closing {@code db} and handling the transaction if required.
     *
     * @return the row ID of the newly inserted row or <code>-1</code> if an error occurred.
     */
    public long insertOrIgnore(@NonNull SQLiteDatabase db, @NonNull UpsertTableRequest request) {
        long rowId =
                db.insertWithOnConflict(
                        request.getTable(),
                        null,
                        request.getContentValues(),
                        SQLiteDatabase.CONFLICT_IGNORE);

        if (rowId != -1) {
            request.getChildTableRequests()
                    .forEach(childRequest -> insertRecord(db, childRequest.withParentKey(rowId)));
        }

        return rowId;
    }

    /** Note: NEVER close this DB */
    @NonNull
    private SQLiteDatabase getReadableDb() {
        SQLiteDatabase sqLiteDatabase = mHealthConnectDatabase.getReadableDatabase();

        if (sqLiteDatabase == null) {
            throw new InternalError("SQLite DB not found");
        }
        return sqLiteDatabase;
    }

    /** Note: NEVER close this DB */
    @NonNull
    private SQLiteDatabase getWritableDb() {
        SQLiteDatabase sqLiteDatabase = mHealthConnectDatabase.getWritableDatabase();

        if (sqLiteDatabase == null) {
            throw new InternalError("SQLite DB not found");
        }
        return sqLiteDatabase;
    }

    public File getDatabasePath() {
        return mHealthConnectDatabase.getDatabasePath();
    }

    public void updateTable(UpsertTableRequest upsertTableRequest) {
        getWritableDb()
                .update(
                        upsertTableRequest.getTable(),
                        upsertTableRequest.getContentValues(),
                        upsertTableRequest.getWhereClauses().get(false),
                        null);
    }

    public int getDatabaseVersion() {
        return getReadableDb().getVersion();
    }

    private void updateRecord(SQLiteDatabase db, UpsertTableRequest request) {
        // perform an update operation where UUID and packageName (mapped by appInfoId) is same
        // as that of the update request.

        if (request.getChildTableRequests().isEmpty()) {
            long numberOfRowsUpdated =
                    db.update(
                            request.getTable(),
                            request.getContentValues(),
                            request.getWhereClauses().get(/* withWhereKeyword */ false),
                            /* WHERE args */ null);

            // throw an exception if the no row was updated, i.e. the uuid with corresponding
            // app_id_info for this request is not found in the table.
            if (numberOfRowsUpdated == 0) {
                throw new IllegalArgumentException(
                        "No record found for the following input : "
                                + new StorageUtils.RecordIdentifierData(
                                        request.getContentValues()));
            }
            return;
        }

        // If the current request has connecting child tables that needs to be updated too in
        // that case the entire record will be first deleted and re-inserted.

        // delete the record corresponding to the provided uuid and packageName. This will
        // delete child table contents in cascade.
        int numberOfRowsDeleted =
                db.delete(
                        request.getTable(),
                        request.getWhereClauses().get(/* withWhereKeyword */ false),
                        /* where args */ null);

        // throw an exception if the no row was deleted, i.e. the uuid for this request is not
        // found in the table.
        if (numberOfRowsDeleted == 0) {
            throw new IllegalArgumentException(
                    "No record found for the following input : "
                            + new StorageUtils.RecordIdentifierData(request.getContentValues()));
        } else {
            // If the record was deleted successfully then re-insert the record with the
            // updated contents.
            insertRecord(db, request);
        }
    }

    /**
     * Do extra sql requests to populate optional extra data. Used to populate {@link
     * android.health.connect.internal.datatypes.ExerciseRouteInternal}.
     */
    private void populateInternalRecordsWithExtraData(
            List<RecordInternal<?>> records, ReadTableRequest request) {
        if (request.getExtraReadRequests() == null) {
            return;
        }
        for (ReadTableRequest extraDataRequest : request.getExtraReadRequests()) {
            Cursor cursorExtraData = read(extraDataRequest);
            request.getRecordHelper()
                    .updateInternalRecordsWithExtraFields(
                            records, cursorExtraData, extraDataRequest.getTableName());
        }
    }

    /** Assumes that caller will be closing {@code db} */
    private long insertOrReplaceRecord(
            @NonNull SQLiteDatabase db, @NonNull UpsertTableRequest request) {
        long rowId =
                db.insertWithOnConflict(
                        request.getTable(),
                        null,
                        request.getContentValues(),
                        SQLiteDatabase.CONFLICT_REPLACE);
        request.getChildTableRequests()
                .forEach(childRequest -> insertRecord(db, childRequest.withParentKey(rowId)));

        return rowId;
    }

    private void insertOrReplace(@NonNull SQLiteDatabase db, @NonNull UpsertTableRequest request) {
        db.replace(request.getTable(), null, request.getContentValues());
    }

    public interface TransactionRunnable<E extends Throwable> {
        void run(SQLiteDatabase db) throws E;
    }
}
