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

import static android.health.connect.Constants.DEFAULT_PAGE_SIZE;
import static android.health.connect.HealthConnectException.ERROR_INTERNAL;
import static android.health.connect.accesslog.AccessLog.OperationType.OPERATION_TYPE_DELETE;
import static android.health.connect.accesslog.AccessLog.OperationType.OPERATION_TYPE_UPSERT;

import static com.android.internal.util.Preconditions.checkArgument;
import static com.android.server.healthconnect.storage.datatypehelpers.RecordHelper.APP_INFO_ID_COLUMN_NAME;
import static com.android.server.healthconnect.storage.datatypehelpers.RecordHelper.UUID_COLUMN_NAME;

import static java.util.Objects.requireNonNull;

import android.annotation.Nullable;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.health.connect.Constants;
import android.health.connect.HealthConnectException;
import android.health.connect.PageTokenWrapper;
import android.health.connect.internal.datatypes.RecordInternal;
import android.util.Pair;
import android.util.Slog;

import com.android.healthfitness.flags.Flags;
import com.android.server.healthconnect.storage.datatypehelpers.AccessLogsHelper;
import com.android.server.healthconnect.storage.datatypehelpers.AppInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.ChangeLogsHelper;
import com.android.server.healthconnect.storage.datatypehelpers.DeviceInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.ReadAccessLogsHelper;
import com.android.server.healthconnect.storage.datatypehelpers.RecordHelper;
import com.android.server.healthconnect.storage.request.DeleteTableRequest;
import com.android.server.healthconnect.storage.request.DeleteTransactionRequest;
import com.android.server.healthconnect.storage.request.ReadTableRequest;
import com.android.server.healthconnect.storage.request.ReadTransactionRequest;
import com.android.server.healthconnect.storage.request.UpsertTableRequest;
import com.android.server.healthconnect.storage.utils.InternalHealthConnectMappings;
import com.android.server.healthconnect.storage.utils.StorageUtils;
import com.android.server.healthconnect.storage.utils.TableColumnPair;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * A class to handle all the DB transaction request from the clients. {@link TransactionManager}
 * acts as a layer b/w the DB and the data type helper classes and helps perform actual operations
 * on the DB.
 *
 * @hide
 */
public final class TransactionManager {
    private static final String TAG = "HealthConnectTransactionMan";

    private volatile HealthConnectDatabase mHealthConnectDatabase;
    private final InternalHealthConnectMappings mInternalHealthConnectMappings;

    /** Create for the given context */
    public static TransactionManager create(
            HealthConnectContext hcContext,
            InternalHealthConnectMappings internalHealthConnectMappings) {
        return new TransactionManager(
                new HealthConnectDatabase(hcContext), internalHealthConnectMappings);
    }

    /** Create for a staged database, used in import and d2d restore */
    public static TransactionManager forStagedDatabase(
            HealthConnectDatabase stagedDatabase,
            InternalHealthConnectMappings internalHealthConnectMappings) {
        return new TransactionManager(stagedDatabase, internalHealthConnectMappings);
    }

    private TransactionManager(
            HealthConnectDatabase hcDatabase,
            InternalHealthConnectMappings internalHealthConnectMappings) {
        mHealthConnectDatabase = hcDatabase;
        mInternalHealthConnectMappings = internalHealthConnectMappings;
    }

    /** Called when we are switching from the current user. */
    public void shutDownCurrentUser() {
        mHealthConnectDatabase.close();
    }

    /** Setup the transaction manager for the new user. */
    public void setupForUser(HealthConnectContext hcContext) {
        mHealthConnectDatabase = new HealthConnectDatabase(hcContext);
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
    public long insert(UpsertTableRequest request) {
        final SQLiteDatabase db = getWritableDb();
        return insert(db, request);
    }

    /**
     * Inserts record into the table in {@code request} into the HealthConnect database using the
     * given {@link SQLiteDatabase}.
     *
     * <p>Assumes that caller will be closing {@code db} and handling the transaction if required.
     *
     * <p>NOTE: PLEASE ONLY USE THIS FUNCTION IF YOU WANT TO INSERT A SINGLE RECORD PER API. PLEASE
     * DON'T USE THIS FUNCTION INSIDE A FOR LOOP OR REPEATEDLY: The reason is that this function
     * tries to insert a record inside its own transaction and if you are trying to insert multiple
     * things using this method in the same api call, they will all get inserted in their separate
     * transactions and will be less performant. If at all, the requirement is to insert them in
     * different transactions, as they are not related to each, then this method can be used.
     *
     * @param db a {@link SQLiteDatabase}.
     * @param request an insert request.
     * @return rowId of the inserted record.
     */
    public long insert(SQLiteDatabase db, UpsertTableRequest request) {
        long rowId = db.insertOrThrow(request.getTable(), null, request.getContentValues());
        request.getChildTableRequests()
                .forEach(childRequest -> insert(db, childRequest.withParentKey(rowId)));
        for (String postUpsertCommand : request.getPostUpsertCommands()) {
            db.execSQL(postUpsertCommand);
        }

        return rowId;
    }

    /**
     * Inserts or replaces all the {@link UpsertTableRequest} into the HealthConnect database.
     *
     * @param upsertTableRequests a list of insert table requests.
     */
    public void insertOrReplaceAllOnConflict(List<UpsertTableRequest> upsertTableRequests)
            throws SQLiteException {
        runAsTransaction(
                db -> {
                    upsertTableRequests.forEach(request -> insertOrReplaceOnConflict(db, request));
                });
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
     * <p>NOTE: INSERT + WITH_CONFLICT_REPLACE only works on unique columns, else in case of
     * conflict it leads to abort of the transaction.
     *
     * @param request an insert request.
     * @return rowId of the inserted or updated record.
     */
    public long insertOrReplaceOnConflict(UpsertTableRequest request) {
        final SQLiteDatabase db = getWritableDb();
        return insertOrReplaceOnConflict(db, request);
    }

    /**
     * Assumes that caller will be closing {@code db}. Returns -1 in case the update was triggered
     * and reading the row_id was not supported on the table.
     *
     * <p>Note: This function updates rather than the traditional delete + insert in SQLite
     */
    public long insertOrReplaceOnConflict(SQLiteDatabase db, UpsertTableRequest request) {
        try {
            if (request.getUniqueColumnsCount() == 0) {
                throw new RuntimeException(
                        "insertOrReplaceRecord should only be called with unique columns set");
            }

            long rowId =
                    db.insertWithOnConflict(
                            request.getTable(),
                            null,
                            request.getContentValues(),
                            SQLiteDatabase.CONFLICT_FAIL);
            insertChildTableRequest(request, rowId, db);
            for (String postUpsertCommand : request.getPostUpsertCommands()) {
                db.execSQL(postUpsertCommand);
            }

            return rowId;
        } catch (SQLiteConstraintException e) {
            try (Cursor cursor = db.rawQuery(request.getReadRequest().getReadCommand(), null)) {
                if (!cursor.moveToFirst()) {
                    throw new HealthConnectException(
                            ERROR_INTERNAL, "Conflict found, but couldn't read the entry.", e);
                }

                long updateResult = updateEntriesIfRequired(db, request, cursor);
                for (String postUpsertCommand : request.getPostUpsertCommands()) {
                    db.execSQL(postUpsertCommand);
                }
                return updateResult;
            }
        }
    }

    /**
     * Inserts or ignore on conflicts all the {@link UpsertTableRequest} into the HealthConnect
     * database.
     */
    public void insertOrIgnoreAllOnConflict(List<UpsertTableRequest> upsertTableRequests) {
        runAsTransaction(
                db -> {
                    upsertTableRequests.forEach(request -> insertOrIgnoreOnConflict(db, request));
                });
    }

    /**
     * Inserts the provided {@link UpsertTableRequest} into the database.
     *
     * <p>Assumes that caller will be closing {@code db} and handling the transaction if required.
     *
     * @return the row ID of the newly inserted row or <code>-1</code> if an error occurred.
     */
    public long insertOrIgnoreOnConflict(SQLiteDatabase db, UpsertTableRequest request) {
        long rowId =
                db.insertWithOnConflict(
                        request.getTable(),
                        null,
                        request.getContentValues(),
                        SQLiteDatabase.CONFLICT_IGNORE);

        if (rowId != -1) {
            request.getChildTableRequests()
                    .forEach(childRequest -> insert(db, childRequest.withParentKey(rowId)));
            for (String postUpsertCommand : request.getPostUpsertCommands()) {
                db.execSQL(postUpsertCommand);
            }
        }

        return rowId;
    }

    /** Updates data for the given request. */
    public void update(UpsertTableRequest request) {
        final SQLiteDatabase db = getWritableDb();
        update(db, request);
    }

    private void update(SQLiteDatabase db, UpsertTableRequest request) {
        // Perform an update operation where UUID and packageName (mapped by appInfoId) is same
        // as that of the update request.
        try {
            long numberOfRowsUpdated =
                    db.update(
                            request.getTable(),
                            request.getContentValues(),
                            request.getUpdateWhereClauses().get(/* withWhereKeyword */ false),
                            /* WHERE args */ null);
            for (String postUpsertCommand : request.getPostUpsertCommands()) {
                db.execSQL(postUpsertCommand);
            }

            // throw an exception if the no row was updated, i.e. the uuid with corresponding
            // app_id_info for this request is not found in the table.
            if (numberOfRowsUpdated == 0) {
                throw new IllegalArgumentException(
                        "No record found for the following input : "
                                + new StorageUtils.RecordIdentifierData(
                                        request.getContentValues()));
            }
        } catch (SQLiteConstraintException e) {
            try (Cursor cursor = db.rawQuery(request.getReadRequest().getReadCommand(), null)) {
                cursor.moveToFirst();
                throw new IllegalArgumentException(
                        StorageUtils.getConflictErrorMessageForRecord(
                                cursor, request.getContentValues()));
            }
        }

        if (request.getAllChildTables().isEmpty()) {
            return;
        }

        try (Cursor cursor =
                db.rawQuery(request.getReadRequestUsingUpdateClause().getReadCommand(), null)) {
            if (!cursor.moveToFirst()) {
                throw new HealthConnectException(
                        ERROR_INTERNAL, "Expected to read an entry for update, but none found");
            }
            final long rowId = StorageUtils.getCursorLong(cursor, request.getRowIdColName());
            deleteChildTableRequest(request, rowId, db);
            insertChildTableRequest(request, rowId, db);
        }
    }

    /**
     * Deletes all the {@link RecordInternal} in {@code request} into the HealthConnect database.
     */
    // NOTE: Please don't add logic to explicitly delete child table entries here as they should
    // be deleted via cascade.
    public int deleteAllRecords(
            DeleteTransactionRequest request,
            // TODO(b/382009199): Move this to the request.
            boolean shouldRecordDeleteAccessLogs,
            AccessLogsHelper accessLogsHelper)
            throws SQLiteException {
        long currentTime = Instant.now().toEpochMilli();
        ChangeLogsHelper.ChangeLogs deletionChangelogs =
                new ChangeLogsHelper.ChangeLogs(OPERATION_TYPE_DELETE, currentTime);
        ChangeLogsHelper.ChangeLogs modificationChangelogs =
                new ChangeLogsHelper.ChangeLogs(OPERATION_TYPE_UPSERT, currentTime);

        return runAsTransaction(
                db -> {
                    int numberOfRecordsDeleted = 0;
                    for (DeleteTableRequest deleteTableRequest : request.getDeleteTableRequests()) {
                        final RecordHelper<?> recordHelper =
                                mInternalHealthConnectMappings.getRecordHelper(
                                        deleteTableRequest.getRecordType());
                        int innerRequestRecordsDeleted;
                        if (deleteTableRequest.requiresRead()) {
                            /*
                            Delete request needs UUID before the entry can be
                            deleted, fetch and set it in {@code request}
                            */
                            try (Cursor cursor =
                                    db.rawQuery(deleteTableRequest.getReadCommand(), null)) {
                                int numberOfUuidsToDelete = 0;
                                while (cursor.moveToNext()) {
                                    String packageColumnName =
                                            requireNonNull(
                                                    deleteTableRequest.getPackageColumnName());
                                    String idColumnName =
                                            requireNonNull(deleteTableRequest.getIdColumnName());
                                    numberOfUuidsToDelete++;
                                    long appInfoId =
                                            StorageUtils.getCursorLong(cursor, packageColumnName);
                                    if (deleteTableRequest.requiresPackageCheck()) {
                                        request.enforcePackageCheck(
                                                StorageUtils.getCursorUUID(cursor, idColumnName),
                                                appInfoId);
                                    }
                                    UUID deletedRecordUuid =
                                            StorageUtils.getCursorUUID(cursor, idColumnName);
                                    deletionChangelogs.addUUID(
                                            deleteTableRequest.getRecordType(),
                                            appInfoId,
                                            deletedRecordUuid);

                                    // Add changelogs for affected records, e.g. a training plan
                                    // being deleted will create changelogs for affected exercise
                                    // sessions.
                                    for (ReadTableRequest additionalChangelogUuidRequest :
                                            recordHelper
                                                    .getReadRequestsForRecordsModifiedByDeletion(
                                                            deletedRecordUuid)) {
                                        Cursor cursorAdditionalUuids =
                                                read(additionalChangelogUuidRequest);
                                        while (cursorAdditionalUuids.moveToNext()) {
                                            modificationChangelogs.addUUID(
                                                    requireNonNull(
                                                                    additionalChangelogUuidRequest
                                                                            .getRecordHelper())
                                                            .getRecordIdentifier(),
                                                    StorageUtils.getCursorLong(
                                                            cursorAdditionalUuids,
                                                            APP_INFO_ID_COLUMN_NAME),
                                                    StorageUtils.getCursorUUID(
                                                            cursorAdditionalUuids,
                                                            UUID_COLUMN_NAME));
                                        }
                                        cursorAdditionalUuids.close();
                                    }
                                }
                                innerRequestRecordsDeleted = numberOfUuidsToDelete;
                            }
                        } else {
                            List<String> ids = deleteTableRequest.getIds();
                            if (ids == null) {
                                throw new IllegalStateException(
                                        "Called with no required reads and no list of ids set");
                            }
                            innerRequestRecordsDeleted = ids.size();
                        }
                        numberOfRecordsDeleted += innerRequestRecordsDeleted;
                        db.execSQL(deleteTableRequest.getDeleteCommand());
                    }

                    for (UpsertTableRequest insertRequestsForChangeLog :
                            deletionChangelogs.getUpsertTableRequests()) {
                        insert(db, insertRequestsForChangeLog);
                    }
                    for (UpsertTableRequest modificationChangelog :
                            modificationChangelogs.getUpsertTableRequests()) {
                        insert(db, modificationChangelog);
                    }
                    if (Flags.addMissingAccessLogs() && shouldRecordDeleteAccessLogs) {
                        accessLogsHelper.recordDeleteAccessLog(
                                db, request.getPackageName(), request.getRecordTypeIds());
                    }
                    return numberOfRecordsDeleted;
                });
    }

    /** Deletes all data for the list of requests into the database in a single transaction. */
    public void deleteAll(List<DeleteTableRequest> deleteTableRequests) {
        runAsTransaction(
                db -> {
                    deleteTableRequests.forEach(request -> delete(db, request));
                });
    }

    /** Delete data for the given request. */
    public void delete(DeleteTableRequest request) {
        delete(getWritableDb(), request);
    }

    /** Delete data for the given request, from the given db. */
    public void delete(SQLiteDatabase db, DeleteTableRequest request) {
        db.execSQL(request.getDeleteCommand());
    }

    /**
     * Reads the records {@link RecordInternal} stored in the HealthConnect database without
     * generating any access logs.
     *
     * @param request a read request.
     * @return List of records read {@link RecordInternal} from table based on ids.
     * @throws IllegalArgumentException if the {@link ReadTransactionRequest} contains pagination
     *     information, which should use {@link #readRecordsAndPageToken(ReadTransactionRequest)}
     *     instead.
     */
    public List<RecordInternal<?>> readRecordsByIdsWithoutAccessLogs(
            ReadTransactionRequest request,
            AppInfoHelper appInfoHelper,
            DeviceInfoHelper deviceInfoHelper)
            throws SQLiteException {
        return readRecordsByIds(request, appInfoHelper, deviceInfoHelper, null, null, false);
    }

    /**
     * Reads the records {@link RecordInternal} stored in the HealthConnect database.
     *
     * @param request a read request.
     * @return List of records read {@link RecordInternal} from table based on ids.
     * @throws IllegalArgumentException if the {@link ReadTransactionRequest} contains pagination
     *     information, which should use {@link #readRecordsAndPageToken(ReadTransactionRequest)}
     *     instead.
     */
    public List<RecordInternal<?>> readRecordsByIds(
            ReadTransactionRequest request,
            AppInfoHelper appInfoHelper,
            DeviceInfoHelper deviceInfoHelper,
            @Nullable AccessLogsHelper accessLogsHelper,
            @Nullable ReadAccessLogsHelper readAccessLogsHelper,
            boolean shouldRecordAccessLog)
            throws SQLiteException {
        // TODO(b/308158714): Make this build time check once we have different classes.
        checkArgument(
                request.getPageToken() == null && request.getPageSize().isEmpty(),
                "Expect read by id request, but request contains pagination info.");
        List<RecordInternal<?>> recordInternals = new ArrayList<>();
        for (ReadTableRequest readTableRequest : request.getReadRequests()) {
            RecordHelper<?> helper = readTableRequest.getRecordHelper();
            requireNonNull(helper);
            try (Cursor cursor = read(readTableRequest)) {
                List<RecordInternal<?>> internalRecords =
                        helper.getInternalRecords(cursor, deviceInfoHelper, appInfoHelper);
                populateInternalRecordsWithExtraData(internalRecords, readTableRequest);
                recordInternals.addAll(internalRecords);
            }
        }

        if (Flags.ecosystemMetrics() && shouldRecordAccessLog && !request.isReadingSelfData()) {
            Objects.requireNonNull(readAccessLogsHelper)
                    .recordAccessLogForNonAggregationReads(
                            getWritableDb(),
                            request.getPackageName(),
                            /* readTimeStamp= */ Instant.now().toEpochMilli(),
                            recordInternals);
        }
        if (Flags.addMissingAccessLogs()) {
            if (!request.isReadingSelfData() && shouldRecordAccessLog) {
                Objects.requireNonNull(accessLogsHelper)
                        .recordReadAccessLog(
                                getWritableDb(),
                                request.getPackageName(),
                                request.getRecordTypeIds());
            }
        }
        return recordInternals;
    }

    /**
     * Reads the records {@link RecordInternal} stored in the HealthConnect database and returns the
     * next page token. Does not generate access logs.
     *
     * @param request a read request. Only one {@link ReadTableRequest} is expected in the {@link
     *     ReadTransactionRequest request}.
     * @return Pair containing records list read {@link RecordInternal} from the table and a page
     *     token for pagination.
     * @throws IllegalArgumentException if the {@link ReadTransactionRequest} doesn't contain
     *     pagination information, which should use {@link
     *     #readRecordsByIds(ReadTransactionRequest)} instead.
     */
    public Pair<List<RecordInternal<?>>, PageTokenWrapper> readRecordsAndPageTokenWithoutAccessLogs(
            ReadTransactionRequest request,
            AppInfoHelper appInfoHelper,
            DeviceInfoHelper deviceInfoHelper,
            @Nullable Map<Long, String> packageNamesByAppIds)
            throws SQLiteException {
        return readRecordsAndPageToken(
                request, appInfoHelper, deviceInfoHelper, false, null, null, packageNamesByAppIds);
    }

    /**
     * Reads the records {@link RecordInternal} stored in the HealthConnect database and returns the
     * next page token.
     *
     * @param request a read request. Only one {@link ReadTableRequest} is expected in the {@link
     *     ReadTransactionRequest request}.
     * @return Pair containing records list read {@link RecordInternal} from the table and a page
     *     token for pagination.
     * @throws IllegalArgumentException if the {@link ReadTransactionRequest} doesn't contain
     *     pagination information, which should use {@link
     *     #readRecordsByIds(ReadTransactionRequest)} instead.
     */
    public Pair<List<RecordInternal<?>>, PageTokenWrapper> readRecordsAndPageToken(
            ReadTransactionRequest request,
            AppInfoHelper appInfoHelper,
            DeviceInfoHelper deviceInfoHelper,
            boolean shouldRecordAccessLog,
            @Nullable AccessLogsHelper accessLogsHelper,
            @Nullable ReadAccessLogsHelper readAccessLogsHelper,
            @Nullable Map<Long, String> packageNamesByAppIds)
            throws SQLiteException {
        // TODO(b/308158714): Make these build time checks once we have different classes.
        checkArgument(
                request.getPageToken() != null && request.getPageSize().isPresent(),
                "Expect read by filter request, but request doesn't contain pagination info.");
        checkArgument(
                request.getReadRequests().size() == 1,
                "Expected read by filter request, but request contains multiple read requests.");
        checkArgument(!shouldRecordAccessLog || accessLogsHelper != null);
        checkArgument(!shouldRecordAccessLog || readAccessLogsHelper != null);
        ReadTableRequest readTableRequest = request.getReadRequests().get(0);
        List<RecordInternal<?>> recordInternalList;
        RecordHelper<?> helper = readTableRequest.getRecordHelper();
        requireNonNull(helper);

        PageTokenWrapper pageToken;
        try (Cursor cursor = read(readTableRequest)) {
            Pair<List<RecordInternal<?>>, PageTokenWrapper> readResult =
                    helper.getNextInternalRecordsPageAndToken(
                            deviceInfoHelper,
                            cursor,
                            request.getPageSize().orElse(DEFAULT_PAGE_SIZE),
                            // pageToken is never null for read by filter requests
                            requireNonNull(request.getPageToken()),
                            packageNamesByAppIds,
                            appInfoHelper);
            recordInternalList = readResult.first;
            pageToken = readResult.second;
            populateInternalRecordsWithExtraData(recordInternalList, readTableRequest);
            if (Flags.ecosystemMetrics() && shouldRecordAccessLog && !request.isReadingSelfData()) {
                Objects.requireNonNull(readAccessLogsHelper)
                        .recordAccessLogForNonAggregationReads(
                                getWritableDb(),
                                request.getPackageName(),
                                /* readTimeStamp= */ Instant.now().toEpochMilli(),
                                recordInternalList);
            }
        }

        if (Flags.addMissingAccessLogs()) {
            if (!request.isReadingSelfData() && shouldRecordAccessLog) {
                Objects.requireNonNull(accessLogsHelper)
                        .recordReadAccessLog(
                                getWritableDb(),
                                request.getPackageName(),
                                request.getRecordTypeIds());
            }
        }
        return Pair.create(recordInternalList, pageToken);
    }

    /** Note: It is the responsibility of the caller to close the returned cursor */
    public Cursor read(ReadTableRequest request) {
        if (Constants.DEBUG) {
            Slog.d(TAG, "Read query: " + request.getReadCommand());
        }
        return getReadableDb().rawQuery(request.getReadCommand(), null);
    }

    /**
     * Reads the given {@link SQLiteDatabase} using the given {@link ReadTableRequest}.
     *
     * <p>Note: It is the responsibility of the caller to close the returned cursor.
     */
    public Cursor read(SQLiteDatabase db, ReadTableRequest request) {
        if (Constants.DEBUG) {
            Slog.d(TAG, "Read query: " + request.getReadCommand());
        }
        return db.rawQuery(request.getReadCommand(), null);
    }

    /**
     * Do a read using {@link SQLiteDatabase#rawQuery(String, String[])}. This method should be used
     * in preference to {@link ReadTableRequest} when it is necessary to read using a query with
     * untrusted user input, to prevent SQL injection attacks.
     *
     * <p>Note: It is the responsibility of the caller to close the returned cursor
     */
    public Cursor rawQuery(String sql, @Nullable String[] selectionArgs) {
        return getReadableDb().rawQuery(sql, selectionArgs);
    }

    /** Returns the count of rows that would be returned by the given request. */
    public int count(ReadTableRequest request) {
        return count(getReadableDb(), request);
    }

    /**
     * Returns the count of rows that would be returned by the given request.
     *
     * <p>Use {@link #count(ReadTableRequest)} unless you already have the database from a
     * transaction.
     */
    public static int count(SQLiteDatabase db, ReadTableRequest request) {
        String countSql = request.getCountCommand();
        if (Constants.DEBUG) {
            Slog.d(TAG, "Count query: " + countSql);
        }
        try (Cursor cursor = db.rawQuery(countSql, null)) {
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            } else {
                throw new RuntimeException("Bad count SQL:" + countSql);
            }
        }
    }

    /** Check if a table exists. */
    public boolean checkTableExists(String tableName) {
        return StorageUtils.checkTableExists(getReadableDb(), tableName);
    }

    /** Get number of entries in the given table. */
    public long queryNumEntries(String tableName) {
        return DatabaseUtils.queryNumEntries(getReadableDb(), tableName);
    }

    /** Size of Health Connect database in bytes. */
    public long getDatabaseSize() {
        return mHealthConnectDatabase.getDatabasePath().length();
    }

    public File getDatabasePath() {
        return mHealthConnectDatabase.getDatabasePath();
    }

    public int getDatabaseVersion() {
        return getReadableDb().getVersion();
    }

    /**
     * Runs a {@link Runnable} task in a Transaction. Using the given request on the provided DB.
     *
     * <p>Note that the provided DB can not be read-only.
     */
    public static <E extends Throwable> void runAsTransaction(SQLiteDatabase db, Runnable<E> task)
            throws E {
        checkArgument(!db.isReadOnly(), "db is read only");
        db.beginTransaction();
        try {
            task.run(db);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /** Runs a {@link Runnable} task in a Transaction. */
    public <E extends Throwable> void runAsTransaction(Runnable<E> task) throws E {
        final SQLiteDatabase db = getWritableDb();
        db.beginTransaction();
        try {
            task.run(db);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /** Runs a {@link Runnable} task without a transaction. */
    public <E extends Throwable> void runWithoutTransaction(Runnable<E> task) throws E {
        final SQLiteDatabase db = getWritableDb();
        task.run(db);
    }

    /**
     * Runnable interface where run method throws Throwable or its subclasses.
     *
     * @param <E> Throwable or its subclass.
     */
    public interface Runnable<E extends Throwable> {
        /** Task to be executed that throws throwable of type E. */
        void run(SQLiteDatabase db) throws E;
    }

    /**
     * Runs a {@link RunnableWithReturn} task in a Transaction.
     *
     * @param task is a {@link RunnableWithReturn}.
     * @param <R> is the return type of the {@code task}.
     * @param <E> is the exception thrown by the {@code task}.
     */
    public <R, E extends Throwable> R runAsTransaction(RunnableWithReturn<R, E> task) throws E {
        final SQLiteDatabase db = getWritableDb();
        db.beginTransaction();
        try {
            R result = task.run(db);
            db.setTransactionSuccessful();
            return result;
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Runs a {@link RunnableWithReturn} task without a transaction.
     *
     * @param task is a {@link RunnableWithReturn}.
     * @param <R> is the return type of the {@code task}.
     * @param <E> is the exception thrown by the {@code task}.
     */
    public <R, E extends Throwable> R runWithoutTransaction(RunnableWithReturn<R, E> task)
            throws E {
        final SQLiteDatabase db = getWritableDb();
        return task.run(db);
    }

    /**
     * Runnable interface where run method throws Throwable or its subclasses and returns any data
     * type R.
     *
     * @param <E> Throwable or its subclass.
     * @param <R> any data type.
     */
    public interface RunnableWithReturn<R, E extends Throwable> {
        /** Task to be executed that throws throwable of type E and returns type R. */
        R run(SQLiteDatabase db) throws E;
    }

    /** Note: NEVER close this DB */
    private SQLiteDatabase getReadableDb() {
        SQLiteDatabase sqLiteDatabase = mHealthConnectDatabase.getReadableDatabase();

        if (sqLiteDatabase == null) {
            throw new InternalError("SQLite DB not found");
        }
        return sqLiteDatabase;
    }

    /** Note: NEVER close this DB */
    private SQLiteDatabase getWritableDb() {
        SQLiteDatabase sqLiteDatabase = mHealthConnectDatabase.getWritableDatabase();

        if (sqLiteDatabase == null) {
            throw new InternalError("SQLite DB not found");
        }
        return sqLiteDatabase;
    }

    /**
     * Do extra sql requests to populate optional extra data. Used to populate {@link
     * android.health.connect.internal.datatypes.ExerciseRouteInternal}.
     */
    public void populateInternalRecordsWithExtraData(
            List<RecordInternal<?>> records, ReadTableRequest request) {
        if (request.getExtraReadRequests() == null) {
            return;
        }
        for (ReadTableRequest extraDataRequest : request.getExtraReadRequests()) {
            Cursor cursorExtraData = read(extraDataRequest);
            RecordHelper<?> recordHelper = request.getRecordHelper();
            if (recordHelper == null) {
                throw new IllegalArgumentException(
                        "Extra read request with no attached record helper.");
            }
            recordHelper.updateInternalRecordsWithExtraFields(records, cursorExtraData);
        }
    }

    private long updateEntriesIfRequired(
            SQLiteDatabase db, UpsertTableRequest request, Cursor cursor) {
        if (!request.requiresUpdate(cursor, request)) {
            return -1;
        }
        db.update(
                request.getTable(),
                request.getContentValues(),
                request.getUpdateWhereClauses().get(/* withWhereKeyword */ false),
                /* WHERE args */ null);
        if (cursor.getColumnIndex(request.getRowIdColName()) == -1) {
            // The table is not explicitly using row_ids hence returning -1 here is ok, as
            // the rowid is of no use to this table.
            // NOTE: Such tables in HC don't support child tables either as child tables
            // inherently require row_ids to have support parent key.
            return -1;
        }
        final long rowId = StorageUtils.getCursorLong(cursor, request.getRowIdColName());
        deleteChildTableRequest(request, rowId, db);
        insertChildTableRequest(request, rowId, db);

        return rowId;
    }

    private void deleteChildTableRequest(
            UpsertTableRequest request, long rowId, SQLiteDatabase db) {
        for (TableColumnPair childTableAndColumn :
                request.getChildTablesWithRowsToBeDeletedDuringUpdate()) {
            DeleteTableRequest deleteTableRequest =
                    new DeleteTableRequest(childTableAndColumn.getTableName())
                            .setId(childTableAndColumn.getColumnName(), String.valueOf(rowId));
            db.execSQL(deleteTableRequest.getDeleteCommand());
        }
    }

    private void insertChildTableRequest(
            UpsertTableRequest request, long rowId, SQLiteDatabase db) {
        for (UpsertTableRequest childTableRequest : request.getChildTableRequests()) {
            String tableName = childTableRequest.getTable();
            ContentValues contentValues = childTableRequest.withParentKey(rowId).getContentValues();
            long childRowId = db.insertOrThrow(tableName, null, contentValues);
            insertChildTableRequest(childTableRequest, childRowId, db);
        }
    }
}
