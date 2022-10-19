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
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.healthconnect.datatypes.RecordTypeIdentifier;
import android.healthconnect.internal.datatypes.RecordInternal;
import android.util.ArrayMap;

import com.android.server.healthconnect.storage.datatypehelpers.RecordHelper;
import com.android.server.healthconnect.storage.datatypehelpers.StepsRecordHelper;
import com.android.server.healthconnect.storage.request.InsertTransactionRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A class to handle all the DB transaction request from the clients. {@link TransactionManager}
 * acts as a layer b/w the DB and the data type helper classes and helps perform actual operations
 * on the DB.
 *
 * @hide
 */
public class TransactionManager {
    private static TransactionManager sTransactionManager = null;

    @NonNull private final HealthConnectDatabase mHealthConnectDatabase;
    @NonNull private final Map<Integer, RecordHelper<?>> mRecordIDToHelperMap;

    private TransactionManager(@NonNull Context context) {
        Map<Integer, RecordHelper<?>> recordIDToHelperMap = new ArrayMap<>();
        recordIDToHelperMap.put(RecordTypeIdentifier.RECORD_TYPE_STEPS, new StepsRecordHelper());

        mRecordIDToHelperMap = Collections.unmodifiableMap(recordIDToHelperMap);
        mHealthConnectDatabase =
                new HealthConnectDatabase(context, new ArrayList<>(mRecordIDToHelperMap.values()));
    }

    /**
     * Inserts all the {@link RecordInternal} in {@code request} into the HealthConnect database.
     *
     * @param request an insert request.
     * @return List of uids of the inserted {@link RecordInternal}, in the same order as they
     *     presented to {@code request}.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<String> insertAll(@NonNull InsertTransactionRequest request)
            throws SQLiteException {
        SQLiteDatabase db = mHealthConnectDatabase.getWritableDatabase();
        db.beginTransaction();
        try {
            request.getInsertRequestsMap()
                    .forEach(
                            ((recordIdentifier, records) -> {
                                RecordHelper recordHelper =
                                        mRecordIDToHelperMap.get(recordIdentifier);
                                Objects.requireNonNull(recordHelper);
                                records.forEach(
                                        (record) -> {
                                            db.insertOrThrow(
                                                    recordHelper.getTableName(),
                                                    null,
                                                    recordHelper.getContentValuesFor(record));
                                        });
                            }));
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

        return request.getUUIdsInOrder();
    }

    public static TransactionManager getInstance(@NonNull Context context) {
        if (sTransactionManager == null) {
            sTransactionManager = new TransactionManager(context);
        }

        return sTransactionManager;
    }
}
