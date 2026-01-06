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

package com.android.server.healthconnect.common.metadata;

import static android.health.connect.Constants.DEFAULT_LONG;

import static com.android.healthfitness.flags.AconfigFlagHelper.isDeviceUdiEnabled;
import static com.android.server.healthconnect.storage.utils.StorageUtils.INTEGER;
import static com.android.server.healthconnect.storage.utils.StorageUtils.PRIMARY;
import static com.android.server.healthconnect.storage.utils.StorageUtils.TEXT_NULL;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorInt;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorLong;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorString;

import android.annotation.Nullable;
import android.content.ContentValues;
import android.database.Cursor;
import android.health.connect.datatypes.Device.DeviceType;
import android.health.connect.device.SyntheticPackageNameMatcher;
import android.health.connect.internal.datatypes.RecordInternal;
import android.util.Pair;

import androidx.annotation.VisibleForTesting;

import com.android.healthfitness.flags.AconfigFlagHelper;
import com.android.server.healthconnect.fitness.recordhelpers.RecordHelper;
import com.android.server.healthconnect.storage.DatabaseHelper;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.request.AlterTableRequest;
import com.android.server.healthconnect.storage.request.CreateTableRequest;
import com.android.server.healthconnect.storage.request.ReadTableRequest;
import com.android.server.healthconnect.storage.request.UpsertTableRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A class to help with the DB transaction for storing Device Info. {@link DeviceInfoHelper} acts as
 * a layer b/w the device_info_table stored in the DB and helps perform insert and read operations
 * on the table
 *
 * @hide
 */
public class DeviceInfoHelper extends DatabaseHelper {
    public static final String TABLE_NAME = "device_info_table";
    public static final String MANUFACTURER_COLUMN_NAME = "manufacturer";
    public static final String MODEL_COLUMN_NAME = "model";
    public static final String DEVICE_TYPE_COLUMN_NAME = "device_type";
    public static final String DEVICE_ID_COLUMN_NAME = "device_id";
    public static final String DISPLAY_NAME_COLUMN_NAME = "display_name";
    public static final String UDI_COLUMN_NAME = "udi";

    record DeviceInfoCache(
            // Map to store deviceInfoId -> DeviceInfo mapping for populating record for read.
            ConcurrentHashMap<Long, DeviceInfo> idToDevice,
            // DeviceInfo -> rowId mapping (model,manufacturer,device_type -> rowId)
            ConcurrentHashMap<DeviceInfo, Long> deviceToRowId,
            // Map to store deviceId -> deviceType mapping
            ConcurrentHashMap<String, Integer> deviceIdToDeviceType) {}

    @Nullable private volatile DeviceInfoCache mDeviceInfoCache;

    private final TransactionManager mTransactionManager;

    public DeviceInfoHelper(
            TransactionManager transactionManager, DatabaseHelpers databaseHelpers) {
        super(databaseHelpers);
        mTransactionManager = transactionManager;
    }

    /**
     * Returns a requests representing the tables that should be created corresponding to this
     * helper
     */
    public static CreateTableRequest getCreateTableRequest() {
        return new CreateTableRequest(TABLE_NAME, getColumnInfo());
    }

    /** Populates record with deviceInfoId */
    public void populateDeviceInfoId(RecordInternal<?> recordInternal) {
        if (recordInternal.getPackageName() != null
                && SyntheticPackageNameMatcher.matches(recordInternal.getPackageName())
                && recordInternal.getDeviceInfoId() != DEFAULT_LONG) {
            // DDP APIs will have already set the deviceInfoId and packageName. Return early as the
            // DeviceInfo from the DDP advertisement includes a deviceId but the record doesn't so
            // this method would create another DeviceInfo entry.
            return;
        }

        String manufacturer = recordInternal.getManufacturer();
        String model = recordInternal.getModel();
        int deviceType = recordInternal.getDeviceType();

        String deviceId = null;
        String displayName = null;
        if (AconfigFlagHelper.isDeviceDataProvidersEnabled()) {
            displayName = recordInternal.getDisplayName();
        }
        String udi = null;
        if (isDeviceUdiEnabled()) {
            udi = recordInternal.getUdi();
        }

        DeviceInfo deviceInfo =
                new DeviceInfo(manufacturer, model, deviceType, deviceId, displayName, udi);
        long rowId = getDeviceInfoMap().getOrDefault(deviceInfo, DEFAULT_LONG);
        if (rowId == DEFAULT_LONG) {
            rowId = insertIfNotPresent(deviceInfo);
        }
        recordInternal.setDeviceInfoId(rowId);
    }

    /**
     * Populates record with manufacturer, model, deviceType, deviceId and displayName values
     *
     * @param deviceInfoId rowId from {@code device_info_table }
     * @param record The record to be populated with values
     */
    public void populateRecordWithValue(long deviceInfoId, RecordInternal<?> record) {
        DeviceInfo deviceInfo = getIdDeviceInfoMap().get(deviceInfoId);
        if (deviceInfo != null) {
            record.setDeviceType(deviceInfo.mDeviceType);
            record.setManufacturer(deviceInfo.mManufacturer);
            record.setModel(deviceInfo.mModel);
            if (AconfigFlagHelper.isDeviceDataProvidersEnabled()) {
                record.setDisplayName(deviceInfo.mDisplayName);
            }
            if (isDeviceUdiEnabled()) {
                record.setUdi(deviceInfo.mUdi);
            }
        }
    }

    @Override
    public synchronized void clearCache() {
        mDeviceInfoCache = null;
    }

    @Override
    protected String getMainTableName() {
        return TABLE_NAME;
    }

    /**
     * Creates an {@link AlterTableRequest} for adding enhanced device info specific columns, {@link
     * #DEVICE_ID_COLUMN_NAME} and {@link #DISPLAY_NAME_COLUMN_NAME} to the device_info_table.
     */
    public static AlterTableRequest getAlterTableRequestForDdpColumns() {
        return new AlterTableRequest(TABLE_NAME, getEnhancedDeviceInfoColumnInfo());
    }

    /**
     * Creates an {@link AlterTableRequest} for adding device UDI specific column, {@link
     * #UDI_COLUMN_NAME} to the device_info_table.
     */
    public static AlterTableRequest getAlterTableRequestForUdiColumn() {
        return new AlterTableRequest(TABLE_NAME, List.of(Pair.create(UDI_COLUMN_NAME, TEXT_NULL)));
    }

    /** Returns the rowId for the given DeviceInfo. */
    @Nullable
    public Long getDeviceInfoId(DeviceInfo deviceInfo) {
        return getDeviceInfoMap().get(deviceInfo);
    }

    /** Returns DeviceInfo for the given deviceInfoId. */
    @Nullable
    public DeviceInfo getDeviceInfo(long deviceInfoId) {
        return getIdDeviceInfoMap().getOrDefault(deviceInfoId, null);
    }

    /** Returns the device type for the given deviceId. */
    @Nullable
    public Integer getDeviceType(String deviceId) {
        return getDeviceIdToDeviceTypeMap().getOrDefault(deviceId, null);
    }

    /**
     * Gets the columns to add for an {@link AlterTableRequest} for adding enhanced device info
     * specific columns.
     */
    private static List<Pair<String, String>> getEnhancedDeviceInfoColumnInfo() {
        return List.of(
                Pair.create(DEVICE_ID_COLUMN_NAME, TEXT_NULL),
                Pair.create(DISPLAY_NAME_COLUMN_NAME, TEXT_NULL));
    }

    private synchronized DeviceInfoCache populateDeviceInfoCache() {
        DeviceInfoCache cache = mDeviceInfoCache;
        if (cache != null) {
            return cache;
        }

        ConcurrentHashMap<DeviceInfo, Long> deviceInfoMap = new ConcurrentHashMap<>();
        ConcurrentHashMap<Long, DeviceInfo> idDeviceInfoMap = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, Integer> deviceIdToDeviceTypeMap = new ConcurrentHashMap<>();
        try (Cursor cursor = mTransactionManager.read(new ReadTableRequest(TABLE_NAME))) {
            while (cursor.moveToNext()) {
                long rowId = getCursorLong(cursor, RecordHelper.PRIMARY_COLUMN_NAME);
                String manufacturer = getCursorString(cursor, MANUFACTURER_COLUMN_NAME);
                String model = getCursorString(cursor, MODEL_COLUMN_NAME);
                int deviceType = getCursorInt(cursor, DEVICE_TYPE_COLUMN_NAME);

                String deviceId = null;
                String displayName = null;
                if (AconfigFlagHelper.isDeviceDataProvidersEnabled()) {
                    deviceId = getCursorString(cursor, DEVICE_ID_COLUMN_NAME);
                    displayName = getCursorString(cursor, DISPLAY_NAME_COLUMN_NAME);
                    if (deviceId != null) {
                        deviceIdToDeviceTypeMap.put(deviceId, deviceType);
                    }
                }
                String udi = null;
                if (isDeviceUdiEnabled()) {
                    udi = getCursorString(cursor, UDI_COLUMN_NAME);
                }

                DeviceInfo deviceInfo =
                        new DeviceInfo(manufacturer, model, deviceType, deviceId, displayName, udi);
                deviceInfoMap.put(deviceInfo, rowId);
                idDeviceInfoMap.put(rowId, deviceInfo);
            }
        }

        cache = new DeviceInfoCache(idDeviceInfoMap, deviceInfoMap, deviceIdToDeviceTypeMap);
        mDeviceInfoCache = cache;
        return cache;
    }

    /** Returns a map of deviceInfoId <> DeviceInfo. */
    @VisibleForTesting
    public Map<Long, DeviceInfo> getIdDeviceInfoMap() {
        // Avoid a synchronized call to populateDeviceInfoCache if possible.
        DeviceInfoCache cache = mDeviceInfoCache;
        if (cache == null) {
            cache = populateDeviceInfoCache();
        }
        return cache.idToDevice;
    }

    private Map<DeviceInfo, Long> getDeviceInfoMap() {
        // Avoid a synchronized call to populateDeviceInfoCache if possible.
        DeviceInfoCache cache = mDeviceInfoCache;
        if (cache == null) {
            cache = populateDeviceInfoCache();
        }
        return cache.deviceToRowId;
    }

    private Map<String, Integer> getDeviceIdToDeviceTypeMap() {
        // Avoid a synchronized call to populateDeviceInfoCache if possible.
        DeviceInfoCache cache = mDeviceInfoCache;
        if (cache == null) {
            cache = populateDeviceInfoCache();
        }
        return cache.deviceIdToDeviceType;
    }

    /** Inserts the device info into the db and returns the row id. */
    public synchronized long insertIfNotPresent(DeviceInfo deviceInfo) {
        Long currentRowId = getDeviceInfoMap().get(deviceInfo);
        if (currentRowId != null) {
            return currentRowId;
        }

        long rowId =
                mTransactionManager.insertOrThrowOnConflict(
                        new UpsertTableRequest(
                                TABLE_NAME,
                                getContentValues(
                                        deviceInfo.mManufacturer,
                                        deviceInfo.mModel,
                                        deviceInfo.mDeviceType,
                                        deviceInfo.mDeviceId,
                                        deviceInfo.mDisplayName,
                                        deviceInfo.mUdi)));
        getDeviceInfoMap().put(deviceInfo, rowId);
        getIdDeviceInfoMap().put(rowId, deviceInfo);
        if (AconfigFlagHelper.isDeviceDataProvidersEnabled() && deviceInfo.mDeviceId != null) {
            getDeviceIdToDeviceTypeMap().put(deviceInfo.mDeviceId, deviceInfo.mDeviceType);
        }
        return rowId;
    }

    private ContentValues getContentValues(
            String manufacturer,
            String model,
            int deviceType,
            @Nullable String deviceId,
            @Nullable String displayName,
            @Nullable String udi) {
        ContentValues contentValues = new ContentValues();

        contentValues.put(MANUFACTURER_COLUMN_NAME, manufacturer);
        contentValues.put(MODEL_COLUMN_NAME, model);
        contentValues.put(DEVICE_TYPE_COLUMN_NAME, deviceType);

        if (AconfigFlagHelper.isDeviceDataProvidersEnabled()) {
            contentValues.put(DEVICE_ID_COLUMN_NAME, deviceId);
            contentValues.put(DISPLAY_NAME_COLUMN_NAME, displayName);
        }

        if (isDeviceUdiEnabled()) {
            contentValues.put(UDI_COLUMN_NAME, udi);
        }

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
    private static List<Pair<String, String>> getColumnInfo() {
        ArrayList<Pair<String, String>> columnInfo = new ArrayList<>();
        columnInfo.add(new Pair<>(RecordHelper.PRIMARY_COLUMN_NAME, PRIMARY));
        columnInfo.add(new Pair<>(MANUFACTURER_COLUMN_NAME, TEXT_NULL));
        columnInfo.add(new Pair<>(MODEL_COLUMN_NAME, TEXT_NULL));
        columnInfo.add(new Pair<>(DEVICE_TYPE_COLUMN_NAME, INTEGER));

        return columnInfo;
    }

    // TODO(b/473489261): Migrate DeviceInfo to builder pattern
    public static final class DeviceInfo {
        private final String mManufacturer;
        private final String mModel;
        @DeviceType private final int mDeviceType;
        @Nullable private final String mDeviceId;
        @Nullable private final String mDisplayName;
        @Nullable private final String mUdi;

        public DeviceInfo(
                String manufacturer,
                String model,
                @DeviceType int deviceType,
                @Nullable String deviceId,
                @Nullable String displayName) {
            this(manufacturer, model, deviceType, deviceId, displayName, /* udi= */ null);
        }

        public DeviceInfo(
                String manufacturer,
                String model,
                @DeviceType int deviceType,
                @Nullable String deviceId,
                @Nullable String displayName,
                @Nullable String udi) {
            mManufacturer = manufacturer;
            mModel = model;
            mDeviceType = deviceType;
            mDeviceId = deviceId;
            mDisplayName = displayName;
            mUdi = isDeviceUdiEnabled() ? udi : null;
        }

        public String getManufacturer() {
            return mManufacturer;
        }

        public String getModel() {
            return mModel;
        }

        public int getDeviceType() {
            return mDeviceType;
        }

        @Nullable
        public String getDeviceId() {
            return mDeviceId;
        }

        @Nullable
        public String getDisplayName() {
            return mDisplayName;
        }

        @Nullable
        public String getUdi() {
            return mUdi;
        }

        @Override
        public int hashCode() {
            int result = mManufacturer != null ? mManufacturer.hashCode() : 0;
            result = 31 * result + (mModel != null ? mModel.hashCode() : 0) + mDeviceType;
            result = 31 * result + (mDeviceId != null ? mDeviceId.hashCode() : 0);
            result = 31 * result + (mDisplayName != null ? mDisplayName.hashCode() : 0);
            result = 31 * result + (mUdi != null ? mUdi.hashCode() : 0);
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
            if (!Objects.equals(mDeviceId, deviceInfo.mDeviceId)) {
                return false;
            }
            if (!Objects.equals(mDisplayName, deviceInfo.mDisplayName)) {
                return false;
            }
            if (!Objects.equals(mUdi, deviceInfo.mUdi)) {
                return false;
            }

            return mDeviceType == deviceInfo.mDeviceType;
        }
    }
}
