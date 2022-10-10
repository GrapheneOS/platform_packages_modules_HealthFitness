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

import static com.android.server.healthconnect.storage.utils.StorageUtils.INTEGER;
import static com.android.server.healthconnect.storage.utils.StorageUtils.PRIMARY;
import static com.android.server.healthconnect.storage.utils.StorageUtils.TEXT_NULL;

import android.annotation.NonNull;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.healthconnect.datatypes.Device.DeviceType;
import android.healthconnect.internal.datatypes.RecordInternal;
import android.util.ArrayMap;
import android.util.Pair;

import com.android.server.healthconnect.storage.datatypehelpers.RecordHelper;
import com.android.server.healthconnect.storage.request.CreateTableRequest;
import com.android.server.healthconnect.storage.request.ReadTableRequest;
import com.android.server.healthconnect.storage.request.UpsertTableRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A class to help with the DB transaction for storing Device Info. {@link DeviceInfoHelper} acts as
 * a layer b/w the device_info_table stored in the DB and helps perform insert and read operations
 * on the table
 *
 * @hide
 */
public class DeviceInfoHelper {
    private static DeviceInfoHelper sDeviceInfoHelper = null;

    /** ArrayMap to store DeviceInfo -> rowId mapping (model,manufacturer,device_type -> rowId) */
    private final Map<DeviceInfo, Long> mDeviceInfoMap = new ArrayMap<>();

    private static final String TABLE_NAME = "device_info_table";
    private static final String MANUFACTURER_COLUMN_NAME = "manufacturer";
    private static final String MODEL_COLUMN_NAME = "model";
    private static final String DEVICE_TYPE_COLUMN_NAME = "device_type";
    private static final long INVALID_ID = -1;

    private class DeviceInfo {
        private final String mManufacturer;
        private final String mModel;
        @DeviceType private final int mDeviceType;

        DeviceInfo(String manufacturer, String model, @DeviceType int deviceType) {
            this.mManufacturer = manufacturer;
            this.mModel = model;
            this.mDeviceType = deviceType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            DeviceInfo deviceInfo = (DeviceInfo) o;
            if (!mManufacturer.equals(deviceInfo.mManufacturer)) {
                return false;
            }
            if (!mModel.equals(deviceInfo.mModel)) {
                return false;
            }
            if (mDeviceType != deviceInfo.mDeviceType) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int result = mManufacturer != null ? mManufacturer.hashCode() : 0;
            result = 31 * result + (mModel != null ? mModel.hashCode() : 0) + mDeviceType;
            return result;
        }
    }

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
        String manufacturer = recordInternal.getManufacturer();
        String model = recordInternal.getModel();
        int deviceType = recordInternal.getDeviceType();
        DeviceInfo deviceInfo = new DeviceInfo(manufacturer, model, deviceType);
        long rowId = mDeviceInfoMap.getOrDefault(deviceInfo, INVALID_ID);
        if (rowId == INVALID_ID) {
            rowId = insertDeviceInfoAndGetRowId(manufacturer, model, deviceType);
            mDeviceInfoMap.put(deviceInfo, rowId);
        }
        recordInternal.setDeviceInfoId(rowId);
    }

    public void populateDeviceInfoMap() {
        try (SQLiteDatabase db = TransactionManager.getInitializedInstance().getReadableDb()) {
            try (Cursor cursor =
                    TransactionManager.getInitializedInstance()
                            .readTable(
                                    db,
                                    new ReadTableRequest(
                                            TABLE_NAME, getColumns(getColumnInfo())))) {
                while (cursor.moveToNext()) {
                    long rowId =
                            cursor.getLong(cursor.getColumnIndex(RecordHelper.PRIMARY_COLUMN_NAME));
                    String manufacturer =
                            cursor.getString(cursor.getColumnIndex(MANUFACTURER_COLUMN_NAME));
                    String model = cursor.getString(cursor.getColumnIndex(MODEL_COLUMN_NAME));
                    int deviceType = cursor.getInt(cursor.getColumnIndex(DEVICE_TYPE_COLUMN_NAME));
                    DeviceInfo deviceInfo = new DeviceInfo(manufacturer, model, deviceType);
                    mDeviceInfoMap.put(deviceInfo, rowId);
                }
            }
        }
    }

    // Called on DB update.
    public void onUpgrade(int newVersion, @NonNull SQLiteDatabase db) {
        // empty by default
    }

    private long insertDeviceInfoAndGetRowId(String manufacturer, String model, int deviceType) {
        long rowId =
                TransactionManager.getInitializedInstance()
                        .insert(
                                new UpsertTableRequest(
                                        TABLE_NAME,
                                        getContentValues(manufacturer, model, deviceType)));
        return rowId;
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

    private String[] getColumns(List<Pair<String, String>> columnInfo) {
        List<String> columns = new ArrayList<String>();
        for (Pair<String, String> pair : columnInfo) {
            columns.add(pair.first);
        }
        return columns.toArray(new String[columns.size()]);
    }
}
