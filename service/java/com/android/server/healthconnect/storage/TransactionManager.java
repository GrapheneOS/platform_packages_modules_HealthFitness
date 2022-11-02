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
import com.android.server.healthconnect.storage.request.InsertTransactionRequest;
import com.android.server.healthconnect.storage.request.ReadTableRequest;
import com.android.server.healthconnect.storage.request.UpsertTableRequest;

import java.util.List;

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

    public static TransactionManager getInstance(@NonNull Context context) {
        if (sTransactionManager == null) {
            sTransactionManager = new TransactionManager(context);
            DeviceInfoHelper.getInstance().populateDeviceInfoMap();
            AppInfoHelper.getInstance().populateAppInfoMap();
        }

        return sTransactionManager;
    }

    public static TransactionManager getInitializedInstance() {
        return sTransactionManager;
    }

    /**
     * Inserts all the {@link RecordInternal} in {@code request} into the HealthConnect database.
     *
     * @param request an insert request.
     * @return List of uids of the inserted {@link RecordInternal}, in the same order as they
     *     presented to {@code request}.
     */
    public List<String> insertAll(@NonNull InsertTransactionRequest request)
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
     * Inserts record into the table in {@code request} into the HealthConnect database.
     *
     * @param request an insert request.
     * @return rowId of the inserted record.
     */
    public long insert(@NonNull UpsertTableRequest request) {
        try (SQLiteDatabase db = mHealthConnectDatabase.getWritableDatabase()) {
            return db.insertOrThrow(request.getTable(), null, request.getContentValues());
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

    private void insertRecord(SQLiteDatabase db, UpsertTableRequest request) {
        long rowId = db.insertOrThrow(request.getTable(), null, request.getContentValues());
        request.getChildTableRequests()
                .forEach(childRequest -> insertRecord(db, childRequest.withParentKey(rowId)));
    }
}
