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

import static com.android.server.healthconnect.storage.datatypehelpers.RecordHelper.APP_INFO_ID_COLUMN_NAME;
import static com.android.server.healthconnect.storage.datatypehelpers.RecordHelper.PRIMARY_COLUMN_NAME;

import android.annotation.NonNull;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.healthconnect.Constants;
import android.healthconnect.datatypes.DataOrigin;
import android.healthconnect.internal.datatypes.RecordInternal;
import android.util.Slog;

import com.android.server.healthconnect.storage.datatypehelpers.AppInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.RecordHelper;
import com.android.server.healthconnect.storage.request.DeleteTableRequest;
import com.android.server.healthconnect.storage.request.DeleteTransactionRequest;
import com.android.server.healthconnect.storage.request.ReadTableRequest;
import com.android.server.healthconnect.storage.request.ReadTransactionRequest;
import com.android.server.healthconnect.storage.request.UpsertTableRequest;
import com.android.server.healthconnect.storage.request.UpsertTransactionRequest;
import com.android.server.healthconnect.storage.utils.StorageUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A class to handle all the DB transaction request from the clients. {@link TransactionManager}
 * acts as a layer b/w the DB and the data type helper classes and helps perform actual operations
 * on the DB.
 *
 * @hide
 */
public class TransactionManager {
    private static final String TAG = "HealthConnectTransactionMan";
    private static TransactionManager sTransactionManager;
    private final HealthConnectDatabase mHealthConnectDatabase;

    private TransactionManager(@NonNull Context context) {
        mHealthConnectDatabase = new HealthConnectDatabase(context);
    }

    @NonNull
    public static TransactionManager getInstance(@NonNull Context context) {
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

    /**
     * Inserts all the {@link RecordInternal} in {@code request} into the HealthConnect database.
     *
     * @param request an insert request.
     * @return List of uids of the inserted {@link RecordInternal}, in the same order as they
     *     presented to {@code request}.
     */
    public List<String> insertAll(@NonNull UpsertTransactionRequest request)
            throws SQLiteException {
        try (SQLiteDatabase db = mHealthConnectDatabase.getWritableDatabase()) {
            db.beginTransaction();
            try {
                request.getUpsertRequests()
                        .forEach((upsertTableRequest) -> insertRecord(db, upsertTableRequest));
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }

            return request.getUUIdsInOrder();
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
    public void deleteAll(@NonNull DeleteTransactionRequest request) throws SQLiteException {
        try (SQLiteDatabase db = mHealthConnectDatabase.getWritableDatabase()) {
            db.beginTransaction();
            try {
                for (DeleteTableRequest deleteTableRequest : request.getDeleteTableRequests()) {
                    if (deleteTableRequest.requiresRead()) {
                        /*
                        Delete request needs UUID before the entry can be
                        deleted, fetch and set it in {@code request}
                        */
                        try (Cursor cursor =
                                db.rawQuery(deleteTableRequest.getReadCommand(), null)) {
                            while (cursor.moveToNext()) {
                                request.onUuidFetched(
                                        deleteTableRequest.getRecordType(),
                                        StorageUtils.getCursorString(
                                                cursor, deleteTableRequest.getIdColumnName()));
                                if (deleteTableRequest.requiresPackageCheck()) {
                                    request.enforcePackageCheck(
                                            StorageUtils.getCursorString(
                                                    cursor, deleteTableRequest.getIdColumnName()),
                                            StorageUtils.getCursorLong(
                                                    cursor,
                                                    deleteTableRequest.getPackageColumnName()));
                                }
                            }
                        }
                    }
                    db.execSQL(deleteTableRequest.getDeleteCommand());
                }

                request.getChangeLogUpsertRequests()
                        .forEach((insertRequest) -> insertRecord(db, insertRequest));

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
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
        try (SQLiteDatabase db = mHealthConnectDatabase.getReadableDatabase()) {
            request.getReadRequests()
                    .forEach(
                            (readTableRequest -> {
                                try (Cursor cursor = read(db, readTableRequest)) {
                                    Objects.requireNonNull(readTableRequest.getRecordHelper());
                                    recordInternals.addAll(
                                            readTableRequest
                                                    .getRecordHelper()
                                                    .getInternalRecords(cursor));
                                }
                            }));
            return recordInternals;
        }
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
        try (SQLiteDatabase db = mHealthConnectDatabase.getWritableDatabase()) {
            return insertRecord(db, request);
        }
    }

    /** Note: It is the responsibility of the caller to properly manage and close {@code db} */
    @NonNull
    public Cursor read(@NonNull SQLiteDatabase db, @NonNull ReadTableRequest request) {
        if (Constants.DEBUG) {
            Slog.d(TAG, "Read query: " + request.getReadCommand());
        }
        return db.rawQuery(request.getReadCommand(), null);
    }

    public long getLastRowIdFor(String tableName) {
        try (SQLiteDatabase db = mHealthConnectDatabase.getReadableDatabase();
                Cursor cursor = db.rawQuery(StorageUtils.getMaxPrimaryKeyQuery(tableName), null)) {
            cursor.moveToFirst();
            return cursor.getLong(cursor.getColumnIndex(PRIMARY_COLUMN_NAME));
        }
    }

    /** Note: it is the responsibility of the requester to manage and close {@code db} */
    public SQLiteDatabase getReadableDb() {
        return mHealthConnectDatabase.getReadableDatabase();
    }

    /**
     * Updates all the {@link RecordInternal} in {@code request} into the HealthConnect database.
     *
     * @param request an update request.
     */
    public void updateAll(@NonNull UpsertTransactionRequest request) throws Exception {
        try (SQLiteDatabase db = mHealthConnectDatabase.getWritableDatabase()) {
            db.beginTransaction();
            try {
                request.getUpsertRequests()
                        .forEach((upsertTableRequest) -> updateRecord(db, upsertTableRequest));
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
    }

    /** Assumes that caller will be closing {@code db} and handling the transaction if required */
    private long insertRecord(@NonNull SQLiteDatabase db, @NonNull UpsertTableRequest request) {
        long rowId = db.insertOrThrow(request.getTable(), null, request.getContentValues());
        request.getChildTableRequests()
                .forEach(childRequest -> insertRecord(db, childRequest.withParentKey(rowId)));

        return rowId;
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
     * @return list of distinct packageNames corresponding to the input table name after querying
     *     the table.
     */
    public ArrayList<DataOrigin> getDistinctPackageNamesForRecordTable(RecordHelper<?> recordHelper)
            throws SQLiteException {
        try (SQLiteDatabase db = getReadableDb()) {
            ArrayList<DataOrigin> packageNamesForDatatype = new ArrayList<>();
            try (Cursor cursorForDistinctPackageNames =
                    db.rawQuery(
                            /* sql query */ recordHelper
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
                            packageNamesForDatatype.add(
                                    new DataOrigin.Builder().setPackageName(packageName).build());
                        }
                    }
                }
            }
            return packageNamesForDatatype;
        }
    }
}
