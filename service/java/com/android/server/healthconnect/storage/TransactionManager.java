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

import static android.health.connect.HealthConnectException.ERROR_INTERNAL;

import static com.android.internal.util.Preconditions.checkArgument;

import android.annotation.Nullable;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.health.connect.Constants;
import android.health.connect.HealthConnectException;
import android.util.Slog;

import com.android.server.healthconnect.storage.request.DeleteTableRequest;
import com.android.server.healthconnect.storage.request.ReadTableRequest;
import com.android.server.healthconnect.storage.request.UpsertTableRequest;
import com.android.server.healthconnect.storage.utils.StorageUtils;
import com.android.server.healthconnect.storage.utils.TableColumnPair;

import java.io.File;
import java.util.List;

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

    /** Create for the given context */
    public static TransactionManager create(HealthConnectContext hcContext) {
        return new TransactionManager(new HealthConnectDatabase(hcContext));
    }

    /** Create for a staged database, used in import and d2d restore */
    public static TransactionManager forStagedDatabase(HealthConnectDatabase stagedDatabase) {
        return new TransactionManager(stagedDatabase);
    }

    private TransactionManager(HealthConnectDatabase hcDatabase) {
        mHealthConnectDatabase = hcDatabase;
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
     * Inserts (or throws if the table exists) the given {@code UpsertTableRequest} into the Health
     * Connect Database.
     *
     * <p>See {@link TransactionManager#insertOrThrowOnConflict(SQLiteDatabase, UpsertTableRequest)}
     * for details.
     *
     * @param request representing the data to be inserted.
     * @return the row_id representing the row id of the newly inserted data.
     */
    public long insertOrThrowOnConflict(UpsertTableRequest request) {
        return runAsTransaction(
                db -> {
                    return insertOrThrowOnConflict(db, request);
                });
    }

    /**
     * Inserts (or throws if the table exists) the given {@code UpsertTableRequest} into the given
     * database.
     *
     * <p>Note: This should always be called within a transaction.
     *
     * @param db the db to insert into.
     * @param request representing the data to be inserted.
     * @return the row_id representing the row id of the newly inserted data.
     * @throws android.database.SQLException in case insertion fails due to a conflict.
     */
    public long insertOrThrowOnConflict(SQLiteDatabase db, UpsertTableRequest request) {
        long rowId = db.insertOrThrow(request.getTable(), null, request.getContentValues());
        insertChildTableRequests(request, rowId, db);
        for (String postUpsertCommand : request.getPostUpsertCommands()) {
            db.execSQL(postUpsertCommand);
        }

        return rowId;
    }

    /**
     * Inserts or replaces all the {@link UpsertTableRequest} into the HealthConnect database.
     *
     * <p>See {@link TransactionManager#insertOrReplaceOnConflict(SQLiteDatabase,
     * UpsertTableRequest)} for details.
     *
     * @param upsertTableRequests list of table requests to insert.
     */
    public void insertOrReplaceAllOnConflict(List<UpsertTableRequest> upsertTableRequests)
            throws SQLiteException {
        runAsTransaction(
                db -> {
                    upsertTableRequests.forEach(request -> insertOrReplaceOnConflict(db, request));
                });
    }

    /**
     * Inserts or replaces the {@link UpsertTableRequest} into the HealthConnect database.
     *
     * <p>See {@link TransactionManager#insertOrReplaceOnConflict(SQLiteDatabase,
     * UpsertTableRequest)} for details.
     *
     * @param request representing the data to be inserted.
     */
    public void insertOrReplaceOnConflict(UpsertTableRequest request) {
        runAsTransaction(
                db -> {
                    insertOrReplaceOnConflict(db, request);
                });
    }

    /**
     * Inserts (or updates if the row exists) the given {@code UpsertTableRequest} into the given
     * database.
     *
     * <p>Note: This should always be called within a transaction.
     *
     * <p>Note: In case of a conflict, this function updates rather than the traditional delete +
     * insert in SQLite.
     *
     * @param db the db to insert into.
     * @param request representing the data to be inserted.
     */
    public void insertOrReplaceOnConflict(SQLiteDatabase db, UpsertTableRequest request) {
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
            insertChildTableRequests(request, rowId, db);
            for (String postUpsertCommand : request.getPostUpsertCommands()) {
                db.execSQL(postUpsertCommand);
            }
        } catch (SQLiteConstraintException e) {
            try (Cursor cursor = db.rawQuery(request.getReadRequest().getReadCommand(), null)) {
                if (!cursor.moveToFirst()) {
                    throw new HealthConnectException(
                            ERROR_INTERNAL, "Conflict found, but couldn't read the entry.", e);
                }

                if (request.requiresUpdate(cursor)) {
                    try {
                        update(db, request);
                    } catch (IllegalArgumentException ex) {
                        Slog.e(TAG, "Unexpected exception when trying to update", ex);
                        // Update within insert historically doesn't throw exceptions for some
                        // error prone scenarios.
                        // See http://ag/32804082/comment/843fbba3_3bb8b5da/
                    }
                }
            }
        }
    }

    /**
     * Inserts (or updates if the row exists) the given {@code UpsertTableRequest} into the given
     * database.
     *
     * <p>See {@link TransactionManager#insertOrIgnoreOnConflict(SQLiteDatabase,
     * UpsertTableRequest)} for details.
     *
     * @param upsertTableRequests representing the data to be inserted.
     */
    public void insertOrIgnoreAllOnConflict(List<UpsertTableRequest> upsertTableRequests) {
        runAsTransaction(
                db -> {
                    upsertTableRequests.forEach(request -> insertOrIgnoreOnConflict(db, request));
                });
    }

    /**
     * Inserts (or updates if the row exists) the given {@code UpsertTableRequest} into the given
     * database.
     *
     * <p>Note: This should always be called within a transaction.
     *
     * <p>Note: In case of a conflict, this function ignores the request.
     *
     * @param db the db to insert into.
     * @param request representing the data to be inserted.
     * @return the row_id representing the row id of the newly inserted data, or -1 if insertion
     *     failed.
     */
    public long insertOrIgnoreOnConflict(SQLiteDatabase db, UpsertTableRequest request) {
        long rowId =
                db.insertWithOnConflict(
                        request.getTable(),
                        null,
                        request.getContentValues(),
                        SQLiteDatabase.CONFLICT_IGNORE);

        if (rowId != -1) {
            insertChildTableRequests(request, rowId, db);
            for (String postUpsertCommand : request.getPostUpsertCommands()) {
                db.execSQL(postUpsertCommand);
            }
        }

        return rowId;
    }

    /**
     * Updates the data represented by {@code UpsertTableRequest} into the given database.
     *
     * <p>See {@link TransactionManager#update(SQLiteDatabase, UpsertTableRequest)} for details.
     *
     * @param request representing the data to be inserted.
     */
    public void update(UpsertTableRequest request) {
        runAsTransaction(
                db -> {
                    update(db, request);
                });
    }

    /**
     * Updates the data represented by {@code UpsertTableRequest} into the given database.
     *
     * <p>Note: This should always be called within a transaction.
     *
     * <p>Note: Only call this if the row is definitely present. For insert or update, use {@link
     * TransactionManager#insertOrReplaceOnConflict(SQLiteDatabase, UpsertTableRequest)}.
     *
     * @param db the db to insert into.
     * @param request representing the data to be inserted.
     * @throws IllegalArgumentException if no data was updated.
     */
    public void update(SQLiteDatabase db, UpsertTableRequest request) {
        try {
            long numberOfRowsUpdated =
                    db.update(
                            request.getTable(),
                            request.getContentValues(),
                            request.getUpdateWhereClauses().get(/* withWhereKeyword */ false),
                            /* WHERE args */ null);

            // Throw an exception if the no row was updated.
            if (numberOfRowsUpdated == 0) {
                throw new IllegalArgumentException(
                        "No record found for the following input : "
                                + new StorageUtils.RecordIdentifierData(
                                        request.getContentValues()));
            }

            for (String postUpsertCommand : request.getPostUpsertCommands()) {
                db.execSQL(postUpsertCommand);
            }
        } catch (SQLiteConstraintException e) {
            try (Cursor cursor = db.rawQuery(request.getReadRequest().getReadCommand(), null)) {
                cursor.moveToFirst();
                throw new IllegalArgumentException(
                        StorageUtils.getConflictErrorMessageForRecord(
                                cursor, request.getContentValues()));
            }
        }

        if (request.getChildTablesWithRowsToBeDeletedDuringUpdate().isEmpty()) {
            return;
        }

        try (Cursor cursor =
                db.rawQuery(request.getReadRequestUsingUpdateClause().getReadCommand(), null)) {
            if (!cursor.moveToFirst()) {
                throw new HealthConnectException(
                        ERROR_INTERNAL, "Expected to read an entry for update, but none found");
            }
            if (cursor.getColumnIndex(request.getRowIdColName()) == -1) {
                // Any table with child tables currently requires row_ids.
                // Note: request.getRowIdColName() currently defaults to RecordHelper's primary id.
                throw new IllegalArgumentException(
                        "row_id not found when trying to insert child tables");
            }
            final long rowId = StorageUtils.getCursorLong(cursor, request.getRowIdColName());
            deleteChildTableRequests(request, rowId, db);
            insertChildTableRequests(request, rowId, db);
        }
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
        runAsTransaction(
                db -> {
                    delete(db, request);
                });
    }

    /** Delete data for the given request, from the given db. */
    public void delete(SQLiteDatabase db, DeleteTableRequest request) {
        db.execSQL(request.getDeleteCommand());
    }

    /** Note: It is the responsibility of the caller to close the returned cursor */
    public Cursor read(ReadTableRequest request) {
        return read(getReadableDb(), request);
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

    private void deleteChildTableRequests(
            UpsertTableRequest request, long rowId, SQLiteDatabase db) {
        for (TableColumnPair childTableAndColumn :
                request.getChildTablesWithRowsToBeDeletedDuringUpdate()) {
            DeleteTableRequest deleteTableRequest =
                    new DeleteTableRequest(childTableAndColumn.getTableName())
                            .setId(childTableAndColumn.getColumnName(), String.valueOf(rowId));
            db.execSQL(deleteTableRequest.getDeleteCommand());
        }
    }

    private void insertChildTableRequests(
            UpsertTableRequest request, long rowId, SQLiteDatabase db) {
        for (UpsertTableRequest childTableRequest : request.getChildTableRequests()) {
            String tableName = childTableRequest.getTable();
            ContentValues contentValues = childTableRequest.withParentKey(rowId).getContentValues();
            long childRowId = db.insertOrThrow(tableName, null, contentValues);
            insertChildTableRequests(childTableRequest, childRowId, db);
        }
    }
}
