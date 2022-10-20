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

import android.annotation.NonNull;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.healthconnect.internal.datatypes.RecordInternal;

import com.android.server.healthconnect.storage.datatypehelpers.AppInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.DeviceInfoHelper;
import com.android.server.healthconnect.storage.request.ReadTableRequest;
import com.android.server.healthconnect.storage.request.ReadTransactionRequest;
import com.android.server.healthconnect.storage.request.UpsertTableRequest;
import com.android.server.healthconnect.storage.request.UpsertTransactionRequest;
import com.android.server.healthconnect.storage.utils.StorageUtils;

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
    private static TransactionManager sTransactionManager;
    private final HealthConnectDatabase mHealthConnectDatabase;

    private TransactionManager(@NonNull Context context) {
        mHealthConnectDatabase = new HealthConnectDatabase(context);
    }

    @NonNull
    public static TransactionManager getInstance(@NonNull Context context) {
        if (sTransactionManager == null) {
            sTransactionManager = new TransactionManager(context);
            DeviceInfoHelper.getInstance().populateDeviceInfoMap();
            AppInfoHelper.getInstance().populateAppInfoMap();
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
     * Reads the records {@link RecordInternal} stored in the HealthConnect database.
     *
     * @param request a read request.
     * @return List of records read {@link RecordInternal} from table based on ids.
     */
    public List<RecordInternal<?>> readRecords(@NonNull ReadTransactionRequest request)
            throws SQLiteException {
        try (SQLiteDatabase db = mHealthConnectDatabase.getReadableDatabase();
                Cursor cursor = read(db, request.getReadRequest())) {
            return request.getInternalRecords(cursor);
        }
    }

    /**
     * Inserts record into the table in {@code request} into the HealthConnect database.
     *
     * <p>NOTE: PLEASE ONLY USE THIS FUNCTION IF YOU WANT TO INSERT A SINGLE RECORD. PLEASE DON'T
     * USE THIS FUNCTION INSIDE A FOR LOOP OR REPEATEDLY: The reason is that this function tries to
     * insert a record out of a transaction and if you are trying to insert a record before or after
     * opening up a transaction please rethink if you really want to use this function.
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
        return db.rawQuery(request.getReadCommand(), request.getSelectionArgs());
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
}
