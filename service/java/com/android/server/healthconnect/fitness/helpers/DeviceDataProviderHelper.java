/*
 * Copyright (C) 2025 The Android Open Source Project
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
package com.android.server.healthconnect.fitness.helpers;

import static com.android.server.healthconnect.fitness.recordhelpers.RecordHelper.DEVICE_INFO_ID_COLUMN_NAME;
import static com.android.server.healthconnect.storage.request.UpsertTableRequest.TYPE_STRING;
import static com.android.server.healthconnect.storage.utils.StorageUtils.INTEGER_NOT_NULL;
import static com.android.server.healthconnect.storage.utils.StorageUtils.PRIMARY;
import static com.android.server.healthconnect.storage.utils.StorageUtils.TEXT_NOT_NULL;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorInt;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorString;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getIntegerAndConvertToBoolean;
import static com.android.server.healthconnect.storage.utils.WhereClauses.LogicalOperator.AND;

import android.annotation.Nullable;
import android.content.ContentValues;
import android.database.Cursor;
import android.health.connect.internal.datatypes.utils.HealthConnectMappings;
import android.util.Pair;

import com.android.server.healthconnect.common.metadata.DeviceInfoHelper;
import com.android.server.healthconnect.device.DeviceDataSourceAdvertisement;
import com.android.server.healthconnect.device.DeviceDataSourceState;
import com.android.server.healthconnect.fitness.recordhelpers.RecordHelper;
import com.android.server.healthconnect.storage.DatabaseHelper;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.request.CreateTableRequest;
import com.android.server.healthconnect.storage.request.DeleteTableRequest;
import com.android.server.healthconnect.storage.request.ReadTableRequest;
import com.android.server.healthconnect.storage.request.UpsertTableRequest;
import com.android.server.healthconnect.storage.utils.WhereClauses;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * A class to help with the DB transaction for storing Device Data Provider metadata and state.
 *
 * @hide
 */
public class DeviceDataProviderHelper extends DatabaseHelper {

    public static final String TABLE_NAME = "device_data_provider_table";
    public static final String SOURCE_PACKAGE_NAME = "source_package_name";
    public static final String DATA_TYPE = "data_type";
    public static final String IS_AVAILABLE = "is_available";
    public static final String IS_USER_ENABLED = "is_user_enabled";

    // Only update the states isAvailable and isUserEnabled for each sourcePackageName, deviceInfoId
    // and dataType "key"
    public static final List<Pair<String, Integer>> UNIQUE_COLUMN_INFO =
            List.of(
                    new Pair<>(SOURCE_PACKAGE_NAME, TYPE_STRING),
                    new Pair<>(DEVICE_INFO_ID_COLUMN_NAME, TYPE_STRING),
                    new Pair<>(DATA_TYPE, TYPE_STRING));

    private final TransactionManager mTransactionManager;
    private final HealthConnectMappings mHealthConnectMappings;

    private record DeviceDataProviderKey(
            String sourcePackageName, int deviceInfoId, int dataType) {}

    private record DeviceDataProviderInfo(
            DeviceDataProviderKey key, boolean isAvailable, boolean isUserEnabled) {}

    @Nullable
    private volatile ConcurrentHashMap<DeviceDataProviderKey, DeviceDataProviderInfo> mDdpCache;

    public DeviceDataProviderHelper(
            DatabaseHelpers databaseHelpers,
            TransactionManager transactionManager,
            HealthConnectMappings healthConnectMappings) {
        super(databaseHelpers);
        this.mTransactionManager = transactionManager;
        this.mHealthConnectMappings = healthConnectMappings;
    }

    @Override
    public synchronized void clearCache() {
        mDdpCache = null;
    }

    /**
     * Returns a requests representing the tables that should be created corresponding to this
     * helper
     */
    public static CreateTableRequest getCreateTableRequest() {
        return new CreateTableRequest(TABLE_NAME, getColumnInfo())
                .addUniqueConstraints(
                        List.of(SOURCE_PACKAGE_NAME, DEVICE_INFO_ID_COLUMN_NAME, DATA_TYPE))
                .addForeignKey(
                        /* referencedTable= */ DeviceInfoHelper.TABLE_NAME,
                        /* columnNames= */ List.of(DEVICE_INFO_ID_COLUMN_NAME),
                        /* referencedColumnNames= */ List.of(RecordHelper.PRIMARY_COLUMN_NAME));
    }

    /**
     * Update the database with the provided Device Data Provider information.
     *
     * <p>Any data types no longer being advertised are removed from the database.
     */
    public synchronized void insertOrUpdateAdvertisement(
            String sourcePackageName,
            int deviceInfoId,
            DeviceDataSourceAdvertisement deviceDataSourceAdvertisement) {
        deleteObsoleteAdvertisements(
                sourcePackageName, deviceInfoId, deviceDataSourceAdvertisement);

        for (DeviceDataSourceState state :
                deviceDataSourceAdvertisement.getDeviceDataSourceState()) {
            int dataType = mHealthConnectMappings.getRecordType(state.getDataType());
            DeviceDataProviderKey key =
                    new DeviceDataProviderKey(sourcePackageName, deviceInfoId, dataType);
            DeviceDataProviderInfo ddpInfo =
                    new DeviceDataProviderInfo(key, state.isAvailable(), state.isUserEnabled());

            if (!getDdpMap().containsKey(key) || !getDdpMap().get(key).equals(ddpInfo)) {
                insertOrUpdate(ddpInfo);
            }
        }
    }

    /**
     * Delete advertisements from the database for data types no longer present for the {@code
     * sourcePackageName} and {@code deviceInfoId}.
     */
    private synchronized void deleteObsoleteAdvertisements(
            String sourcePackageName,
            int deviceInfoId,
            DeviceDataSourceAdvertisement latestDeviceDataSourceAdvertisement) {
        List<DeviceDataProviderKey> existingAdvertisements =
                getDdpMap().keySet().stream()
                        .filter(
                                key ->
                                        key.sourcePackageName.equals(sourcePackageName)
                                                && key.deviceInfoId == deviceInfoId)
                        .toList();

        Set<Integer> latestDataTypes =
                latestDeviceDataSourceAdvertisement.getDeviceDataSourceState().stream()
                        .map(state -> mHealthConnectMappings.getRecordType(state.getDataType()))
                        .collect(Collectors.toSet());
        for (DeviceDataProviderKey existingAdvertisement : existingAdvertisements) {
            if (!latestDataTypes.contains(existingAdvertisement.dataType)) {
                delete(existingAdvertisement);
            }
        }
    }

    /**
     * Delete ddpInfo from the db and cache for the given sourcePackageName, deviceInfoId and
     * dataType.
     */
    private synchronized void delete(DeviceDataProviderKey key) {
        mTransactionManager.delete(
                new DeleteTableRequest(TABLE_NAME)
                        .addExtraWhereClauses(
                                new WhereClauses(AND)
                                        .addWhereEqualsClause(
                                                SOURCE_PACKAGE_NAME, key.sourcePackageName)
                                        .addWhereEqualsClause(
                                                DEVICE_INFO_ID_COLUMN_NAME,
                                                String.valueOf(key.deviceInfoId))
                                        .addWhereEqualsClause(
                                                DATA_TYPE, String.valueOf(key.dataType))));
        getDdpMap().remove(key);
    }

    /**
     * Insert ddpInfo if not present in the db or updates the states isAvailable and isUserEnabled
     * for the given sourcePackageName, deviceInfoId and dataType.
     */
    private synchronized void insertOrUpdate(DeviceDataProviderInfo ddpInfo) {
        getDdpMap().remove(ddpInfo.key);
        mTransactionManager.insertOrReplaceOnConflict(
                new UpsertTableRequest(TABLE_NAME, getContentValues(ddpInfo), UNIQUE_COLUMN_INFO));
        getDdpMap().put(ddpInfo.key, ddpInfo);
    }

    private ConcurrentHashMap<DeviceDataProviderKey, DeviceDataProviderInfo> getDdpMap() {
        if (mDdpCache == null) {
            mDdpCache = generateDdpCache(mTransactionManager);
        }
        return mDdpCache;
    }

    private static synchronized ConcurrentHashMap<DeviceDataProviderKey, DeviceDataProviderInfo>
            generateDdpCache(TransactionManager transactionManager) {
        ConcurrentHashMap<DeviceDataProviderKey, DeviceDataProviderInfo> ddpInfoMap =
                new ConcurrentHashMap<>();
        try (Cursor cursor = transactionManager.read(new ReadTableRequest(TABLE_NAME))) {
            while (cursor.moveToNext()) {
                int deviceInfoId = getCursorInt(cursor, DEVICE_INFO_ID_COLUMN_NAME);
                String sourcePackageName = getCursorString(cursor, SOURCE_PACKAGE_NAME);
                int dataType = getCursorInt(cursor, DATA_TYPE);
                boolean isAvailable = getIntegerAndConvertToBoolean(cursor, IS_AVAILABLE);
                boolean isUserEnabled = getIntegerAndConvertToBoolean(cursor, IS_USER_ENABLED);

                DeviceDataProviderKey key =
                        new DeviceDataProviderKey(sourcePackageName, deviceInfoId, dataType);
                DeviceDataProviderInfo ddpInfo =
                        new DeviceDataProviderInfo(key, isAvailable, isUserEnabled);
                ddpInfoMap.put(key, ddpInfo);
            }
        }

        return ddpInfoMap;
    }

    private ContentValues getContentValues(DeviceDataProviderInfo ddpInfo) {
        ContentValues contentValues = new ContentValues();

        contentValues.put(DEVICE_INFO_ID_COLUMN_NAME, ddpInfo.key.deviceInfoId);
        contentValues.put(SOURCE_PACKAGE_NAME, ddpInfo.key.sourcePackageName);
        contentValues.put(DATA_TYPE, ddpInfo.key.dataType);
        contentValues.put(IS_AVAILABLE, ddpInfo.isAvailable);
        contentValues.put(IS_USER_ENABLED, ddpInfo.isUserEnabled);

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
        columnInfo.add(new Pair<>(DEVICE_INFO_ID_COLUMN_NAME, INTEGER_NOT_NULL)); // Foreign key
        columnInfo.add(new Pair<>(SOURCE_PACKAGE_NAME, TEXT_NOT_NULL));
        columnInfo.add(new Pair<>(DATA_TYPE, INTEGER_NOT_NULL));
        columnInfo.add(new Pair<>(IS_AVAILABLE, INTEGER_NOT_NULL));
        columnInfo.add(new Pair<>(IS_USER_ENABLED, INTEGER_NOT_NULL));

        return columnInfo;
    }

    @Override
    protected String getMainTableName() {
        return TABLE_NAME;
    }
}
