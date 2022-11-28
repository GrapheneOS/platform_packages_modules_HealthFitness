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

import static com.android.server.healthconnect.storage.utils.StorageUtils.INTEGER;
import static com.android.server.healthconnect.storage.utils.StorageUtils.PRIMARY_AUTOINCREMENT;
import static com.android.server.healthconnect.storage.utils.StorageUtils.TEXT_NOT_NULL_UNIQUE;
import static com.android.server.healthconnect.storage.utils.StorageUtils.TEXT_NULL;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorLong;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorString;

import android.annotation.NonNull;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.healthconnect.datatypes.RecordTypeIdentifier;
import android.healthconnect.internal.datatypes.RecordInternal;
import android.healthconnect.internal.datatypes.utils.RecordMapper;
import android.util.Pair;

import com.android.server.healthconnect.storage.request.CreateTableRequest;
import com.android.server.healthconnect.storage.request.ReadTableRequest;
import com.android.server.healthconnect.storage.request.UpsertTableRequest;
import com.android.server.healthconnect.storage.utils.SqlJoin;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Parent class for all the helper classes for all the records
 *
 * @hide
 */
public abstract class RecordHelper<T extends RecordInternal<?>> {
    public static final String PRIMARY_COLUMN_NAME = "row_id";
    public static final String UUID_COLUMN_NAME = "uuid";
    public static final String APP_INFO_ID_COLUMN_NAME = "app_info_id";
    private static final String LAST_MODIFIED_TIME_COLUMN_NAME = "last_modified_time";
    private static final String CLIENT_RECORD_ID_COLUMN_NAME = "client_record_id";
    private static final String CLIENT_RECORD_VERSION_COLUMN_NAME = "client_record_version";
    private static final String DEVICE_INFO_ID_COLUMN_NAME = "device_info_id";
    @RecordTypeIdentifier.RecordType private final int mRecordIdentifier;

    RecordHelper() {
        HelperFor annotation = this.getClass().getAnnotation(HelperFor.class);
        Objects.requireNonNull(annotation);
        mRecordIdentifier = annotation.recordIdentifier();
    }

    @RecordTypeIdentifier.RecordType
    public int getRecordIdentifier() {
        return mRecordIdentifier;
    }

    // Called on DB update. Inheriting classes should implement this if they need to add new
    // columns.
    public void onUpgrade(int newVersion, @NonNull SQLiteDatabase db) {
        // empty by default
    }

    /**
     * Returns a requests representing the tables that should be created corresponding to this
     * helper
     */
    @NonNull
    public final CreateTableRequest getCreateTableRequest() {
        return new CreateTableRequest(getMainTableName(), getColumnInfo())
                .addForeignKey(
                        DeviceInfoHelper.getInstance().getTableName(),
                        Collections.singletonList(DEVICE_INFO_ID_COLUMN_NAME),
                        Collections.singletonList(PRIMARY_COLUMN_NAME))
                .addForeignKey(
                        AppInfoHelper.getInstance().getTableName(),
                        Collections.singletonList(APP_INFO_ID_COLUMN_NAME),
                        Collections.singletonList(PRIMARY_COLUMN_NAME))
                .setChildTableRequests(getChildTableCreateRequests());
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public UpsertTableRequest getUpsertTableRequest(RecordInternal<?> recordInternal) {
        return new UpsertTableRequest(getMainTableName(), getContentValues((T) recordInternal))
                .setChildTableRequests(getChildTableUpsertRequests((T) recordInternal));
    }

    /** Returns ReadTableRequest for the record corresponding to this helper */
    public ReadTableRequest getReadTableRequest() {
        return new ReadTableRequest(getMainTableName())
                .setInnerJoinClause(getInnerJoinFoReadRequest());
    }

    /** Returns List of Internal records from the cursor */
    public List<RecordInternal<?>> getInternalRecords(Cursor cursor) {
        List<RecordInternal<?>> recordInternalList = new ArrayList<>();
        while (cursor.moveToNext()) {
            try {
                @SuppressWarnings("unchecked")
                T record =
                        (T)
                                RecordMapper.getInstance()
                                        .getRecordIdToInternalRecordClassMap()
                                        .get(getRecordIdentifier())
                                        .getConstructor()
                                        .newInstance();
                record.setUuid(getCursorString(cursor, UUID_COLUMN_NAME));
                record.setLastModifiedTime(getCursorLong(cursor, LAST_MODIFIED_TIME_COLUMN_NAME));
                record.setClientRecordId(getCursorString(cursor, CLIENT_RECORD_ID_COLUMN_NAME));
                record.setClientRecordVersion(
                        getCursorLong(cursor, CLIENT_RECORD_VERSION_COLUMN_NAME));
                long deviceInfoId = getCursorLong(cursor, DEVICE_INFO_ID_COLUMN_NAME);
                DeviceInfoHelper.getInstance().populateRecordWithValue(deviceInfoId, record);
                long appInfoId = getCursorLong(cursor, APP_INFO_ID_COLUMN_NAME);
                AppInfoHelper.getInstance().populateRecordWithValue(appInfoId, record);
                populateRecordValue(cursor, record);
                recordInternalList.add(record);
            } catch (InstantiationException
                    | IllegalAccessException
                    | NoSuchMethodException
                    | InvocationTargetException exception) {
                throw new IllegalArgumentException(exception);
            }
        }
        return recordInternalList;
    }

    public abstract String getStartTimeColumnName();

    /**
     * Child classes should implement this if it wants to create additional tables, apart from the
     * main table.
     */
    @NonNull
    List<CreateTableRequest> getChildTableCreateRequests() {
        return Collections.emptyList();
    }

    /** Returns the table name to be created corresponding to this helper */
    @NonNull
    abstract String getMainTableName();

    /**
     * This implementation should return the column names with which the table should be created.
     *
     * <p>NOTE: New columns can only be added via onUpgrade. Why? Consider what happens if a table
     * already exists on the device
     *
     * <p>PLEASE DON'T USE THIS METHOD TO ADD NEW COLUMNS
     */
    @NonNull
    abstract List<Pair<String, String>> getSpecificColumnInfo();

    /**
     * Child classes implementation should add the values of {@code recordInternal} that needs to be
     * populated in the DB to {@code contentValues}.
     */
    abstract void populateContentValues(
            @NonNull ContentValues contentValues, @NonNull T recordInternal);

    List<UpsertTableRequest> getChildTableUpsertRequests(T record) {
        return Collections.emptyList();
    }

    SqlJoin getInnerJoinFoReadRequest() {
        return null;
    }

    @NonNull
    private ContentValues getContentValues(@NonNull T recordInternal) {
        ContentValues recordContentValues = new ContentValues();

        recordContentValues.put(UUID_COLUMN_NAME, recordInternal.getUuid());
        recordContentValues.put(
                LAST_MODIFIED_TIME_COLUMN_NAME, recordInternal.getLastModifiedTime());
        recordContentValues.put(CLIENT_RECORD_ID_COLUMN_NAME, recordInternal.getClientRecordId());
        recordContentValues.put(
                CLIENT_RECORD_VERSION_COLUMN_NAME, recordInternal.getClientRecordVersion());
        recordContentValues.put(DEVICE_INFO_ID_COLUMN_NAME, recordInternal.getDeviceInfoId());
        recordContentValues.put(APP_INFO_ID_COLUMN_NAME, recordInternal.getAppInfoId());

        populateContentValues(recordContentValues, recordInternal);

        return recordContentValues;
    }

    /**
     * This implementation should return the column names with which the table should be created.
     *
     * <p>NOTE: New columns can only be added via onUpgrade. Why? Consider what happens if a table
     * already exists on the device
     *
     * <p>PLEASE DON'T USE THIS METHOD TO ADD NEW COLUMNS
     */
    @NonNull
    private List<Pair<String, String>> getColumnInfo() {
        ArrayList<Pair<String, String>> columnInfo = new ArrayList<>();
        columnInfo.add(new Pair<>(PRIMARY_COLUMN_NAME, PRIMARY_AUTOINCREMENT));
        columnInfo.add(new Pair<>(UUID_COLUMN_NAME, TEXT_NOT_NULL_UNIQUE));
        columnInfo.add(new Pair<>(LAST_MODIFIED_TIME_COLUMN_NAME, INTEGER));
        columnInfo.add(new Pair<>(CLIENT_RECORD_ID_COLUMN_NAME, TEXT_NULL));
        columnInfo.add(new Pair<>(CLIENT_RECORD_VERSION_COLUMN_NAME, TEXT_NULL));
        columnInfo.add(new Pair<>(DEVICE_INFO_ID_COLUMN_NAME, INTEGER));
        columnInfo.add(new Pair<>(APP_INFO_ID_COLUMN_NAME, INTEGER));

        columnInfo.addAll(getSpecificColumnInfo());

        return columnInfo;
    }

    /**
     * Child classes implementation should populate the values to the {@code record} using the
     * cursor {@code cursor} queried from the DB .
     */
    abstract void populateRecordValue(@NonNull Cursor cursor, @NonNull T recordInternal);
}
