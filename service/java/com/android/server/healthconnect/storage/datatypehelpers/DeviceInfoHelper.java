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

import static android.healthconnect.Constants.DEFAULT_LONG;

import static com.android.server.healthconnect.storage.utils.StorageUtils.INTEGER;
import static com.android.server.healthconnect.storage.utils.StorageUtils.PRIMARY;
import static com.android.server.healthconnect.storage.utils.StorageUtils.TEXT_NULL;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorInt;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorLong;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorString;

import android.annotation.NonNull;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.healthconnect.datatypes.Device.DeviceType;
import android.healthconnect.internal.datatypes.RecordInternal;
import android.util.ArrayMap;
import android.util.Pair;

import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.request.CreateTableRequest;
import com.android.server.healthconnect.storage.request.ReadTableRequest;
import com.android.server.healthconnect.storage.request.UpsertTableRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A class to help with the DB transaction for storing Device Info. {@link DeviceInfoHelper} acts as
 * a layer b/w the device_info_table stored in the DB and helps perform insert and read operations
 * on the table
 *
 * @hide
 */
public class DeviceInfoHelper {
    private static final class DeviceInfo {
        private final String mManufacturer;
        private final String mModel;
        @DeviceType private final int mDeviceType;

        DeviceInfo(String manufacturer, String model, @DeviceType int deviceType) {
            mManufacturer = manufacturer;
            mModel = model;
            mDeviceType = deviceType;
        }

        @Override
        public int hashCode() {
            int result = mManufacturer != null ? mManufacturer.hashCode() : 0;
            result = 31 * result + (mModel != null ? mModel.hashCode() : 0) + mDeviceType;
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (Objects.isNull(o)) {
                return false;
            }
            if (this == o) {
                return true;
            }
            if (getClass() != o.getClass()) {
                return false;
            }

            DeviceInfo deviceInfo = (DeviceInfo) o;
            if (!Objects.equals(mManufacturer, deviceInfo.mManufacturer)) {
                return false;
            }
            if (!Objects.equals(mModel, deviceInfo.mModel)) {
                return false;
            }

            return mDeviceType == deviceInfo.mDeviceType;
        }
    }

    private static final String TABLE_NAME = "device_info_table";
    private static final String MANUFACTURER_COLUMN_NAME = "manufacturer";
    private static final String MODEL_COLUMN_NAME = "model";
    private static final String DEVICE_TYPE_COLUMN_NAME = "device_type";
    private static DeviceInfoHelper sDeviceInfoHelper;
    /** ArrayMap to store DeviceInfo -> rowId mapping (model,manufacturer,device_type -> rowId) */
    private Map<DeviceInfo, Long> mDeviceInfoMap;
    /** Map to store deviceInfoId -> DeviceInfo mapping for populating record for read */
    private final Map<Long, DeviceInfo> mIdDeviceInfoMap = new ArrayMap<>();

    public static DeviceInfoHelper getInstance() {
        if (sDeviceInfoHelper == null) {
            sDeviceInfoHelper = new DeviceInfoHelper();
        }
        return sDeviceInfoHelper;
    }

    /**
     * Returns a requests representing the tables that should be created corresponding to this
     * helper
     */
    @NonNull
    public final CreateTableRequest getCreateTableRequest() {
        return new CreateTableRequest(TABLE_NAME, getColumnInfo());
    }

    public String getTableName() {
        return TABLE_NAME;
    }

    /** Populates record with deviceInfoId */
    public void populateDeviceInfoId(@NonNull RecordInternal<?> recordInternal) {
        if (Objects.isNull(mDeviceInfoMap)) {
            populateDeviceInfoMap();
        }

        String manufacturer = recordInternal.getManufacturer();
        String model = recordInternal.getModel();
        int deviceType = recordInternal.getDeviceType();
        DeviceInfo deviceInfo = new DeviceInfo(manufacturer, model, deviceType);
        long rowId = mDeviceInfoMap.getOrDefault(deviceInfo, DEFAULT_LONG);
        if (rowId == DEFAULT_LONG) {
            rowId = insertDeviceInfoAndGetRowId(manufacturer, model, deviceType);
            mDeviceInfoMap.put(deviceInfo, rowId);
            mIdDeviceInfoMap.put(rowId, deviceInfo);
        }
        recordInternal.setDeviceInfoId(rowId);
    }

    public void populateDeviceInfoMap() {
        mDeviceInfoMap = new ArrayMap<>();
        final TransactionManager transactionManager = TransactionManager.getInitialisedInstance();
        try (SQLiteDatabase db = transactionManager.getReadableDb();
                Cursor cursor = transactionManager.read(db, new ReadTableRequest(TABLE_NAME))) {
            while (cursor.moveToNext()) {
                long rowId = getCursorLong(cursor, RecordHelper.PRIMARY_COLUMN_NAME);
                String manufacturer = getCursorString(cursor, MANUFACTURER_COLUMN_NAME);
                String model = getCursorString(cursor, MODEL_COLUMN_NAME);
                int deviceType = getCursorInt(cursor, DEVICE_TYPE_COLUMN_NAME);
                DeviceInfo deviceInfo = new DeviceInfo(manufacturer, model, deviceType);
                mDeviceInfoMap.put(deviceInfo, rowId);
                mIdDeviceInfoMap.put(rowId, deviceInfo);
            }
        }
    }

    /**
     * Populates record with manufacturer, model and deviceType values
     *
     * @param deviceInfoId rowId from {@code device_info_table }
     * @param record The record to be populated with values
     */
    public void populateRecordWithValue(long deviceInfoId, @NonNull RecordInternal<?> record) {
        DeviceInfo deviceInfo = mIdDeviceInfoMap.get(deviceInfoId);
        if (deviceInfo != null) {
            record.setDeviceType(deviceInfo.mDeviceType);
            record.setManufacturer(deviceInfo.mManufacturer);
            record.setModel(deviceInfo.mModel);
        }
    }

    // Called on DB update.
    public void onUpgrade(int newVersion, @NonNull SQLiteDatabase db) {
        // empty by default
    }

    private long insertDeviceInfoAndGetRowId(String manufacturer, String model, int deviceType) {
        return TransactionManager.getInitialisedInstance()
                .insert(
                        new UpsertTableRequest(
                                TABLE_NAME, getContentValues(manufacturer, model, deviceType)));
    }

    @NonNull
    private ContentValues getContentValues(String manufacturer, String model, int deviceType) {
        ContentValues contentValues = new ContentValues();

        contentValues.put(MANUFACTURER_COLUMN_NAME, manufacturer);
        contentValues.put(MODEL_COLUMN_NAME, model);
        contentValues.put(DEVICE_TYPE_COLUMN_NAME, deviceType);

        return contentValues;
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
        columnInfo.add(new Pair<>(RecordHelper.PRIMARY_COLUMN_NAME, PRIMARY));
        columnInfo.add(new Pair<>(MANUFACTURER_COLUMN_NAME, TEXT_NULL));
        columnInfo.add(new Pair<>(MODEL_COLUMN_NAME, TEXT_NULL));
        columnInfo.add(new Pair<>(DEVICE_TYPE_COLUMN_NAME, INTEGER));

        return columnInfo;
    }
}
