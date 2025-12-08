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

import static android.health.connect.Constants.DEFAULT_LONG;

import static com.android.server.healthconnect.storage.request.UpsertTableRequest.TYPE_STRING;
import static com.android.server.healthconnect.storage.utils.StorageUtils.PRIMARY;
import static com.android.server.healthconnect.storage.utils.StorageUtils.TEXT_NOT_NULL;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorLong;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorString;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.ContentValues;
import android.database.Cursor;
import android.util.Pair;

import androidx.annotation.GuardedBy;
import androidx.annotation.VisibleForTesting;

import com.android.healthfitness.flags.AconfigFlagHelper;
import com.android.server.healthconnect.fitness.recordhelpers.RecordHelper;
import com.android.server.healthconnect.storage.DatabaseHelper;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.request.CreateTableRequest;
import com.android.server.healthconnect.storage.request.ReadTableRequest;
import com.android.server.healthconnect.storage.request.UpsertTableRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A class to help with the DB transaction for storing device data provider metadata.
 *
 * @hide
 */
public class DeviceDataProviderMetadataHelper extends DatabaseHelper {
    public static final String TABLE_NAME = "device_data_provider_metadata_table";
    public static final String SOURCE_PACKAGE_COLUMN_NAME = "source_package_name";
    public static final List<Pair<String, Integer>> UNIQUE_COLUMN_INFO =
            Collections.singletonList(new Pair<>(SOURCE_PACKAGE_COLUMN_NAME, TYPE_STRING));

    private final TransactionManager mTransactionManager;

    record MetadataCache(
            HashMap<Long, DeviceDataProviderMetadata> idToMetadata,
            HashMap<DeviceDataProviderMetadata, Long> metadataToId) {}

    @GuardedBy("this")
    @Nullable
    private MetadataCache mCache;

    @VisibleForTesting
    public record DeviceDataProviderMetadata(String sourcePackageName) {}

    public DeviceDataProviderMetadataHelper(
            DatabaseHelpers databaseHelpers, TransactionManager transactionManager) {
        super(databaseHelpers);
        this.mTransactionManager = transactionManager;
    }

    @Override
    public synchronized void clearCache() {
        mCache = null;
    }

    @Override
    public synchronized void clearData(TransactionManager transactionManager) {
        if (!AconfigFlagHelper.isDeviceDataProvidersEnabled()) {
            return;
        }
        super.clearData(transactionManager);
    }

    /** Returns the rowId for the given device data provider package name. */
    public synchronized long getDeviceDataProviderMetadataId(@NonNull String ddpPackageName) {
        DeviceDataProviderMetadata deviceDataProviderMetadata =
                new DeviceDataProviderMetadata(ddpPackageName);

        return getDeviceDataProviderMetadataIdMap()
                .getOrDefault(deviceDataProviderMetadata, DEFAULT_LONG);
    }

    /** Inserts the device data provider metadata into the db. */
    public synchronized void insertIfNotPresent(@NonNull String ddpPackageName) {
        DeviceDataProviderMetadata deviceDataProviderMetadata =
                new DeviceDataProviderMetadata(ddpPackageName);

        if (getDeviceDataProviderMetadataIdMap().containsKey(deviceDataProviderMetadata)) {
            return;
        }

        long rowId =
                mTransactionManager.insertOrThrowOnConflict(
                        new UpsertTableRequest(
                                TABLE_NAME,
                                getContentValues(deviceDataProviderMetadata),
                                UNIQUE_COLUMN_INFO));

        getIdDeviceDataProviderMetadataMap().put(rowId, deviceDataProviderMetadata);
        getDeviceDataProviderMetadataIdMap().put(deviceDataProviderMetadata, rowId);
    }

    /** Returns a map of key rowId <> DeviceDataProviderMetadata. */
    @VisibleForTesting
    public synchronized HashMap<Long, DeviceDataProviderMetadata>
            getIdDeviceDataProviderMetadataMap() {
        if (mCache == null) {
            mCache = populateCache(mTransactionManager);
        }
        return mCache.idToMetadata;
    }

    /** Returns a map of key DeviceDataProviderMetadata <> rowId. */
    @VisibleForTesting
    public synchronized HashMap<DeviceDataProviderMetadata, Long>
            getDeviceDataProviderMetadataIdMap() {
        if (mCache == null) {
            mCache = populateCache(mTransactionManager);
        }
        return mCache.metadataToId;
    }

    private synchronized MetadataCache populateCache(TransactionManager transactionManager) {
        if (mCache != null) {
            return mCache;
        }

        ConcurrentHashMap<Long, DeviceDataProviderMetadata> idToMetadataMap =
                new ConcurrentHashMap<>();
        ConcurrentHashMap<DeviceDataProviderMetadata, Long> metadataToIdMap =
                new ConcurrentHashMap<>();

        try (Cursor cursor = transactionManager.read(new ReadTableRequest(TABLE_NAME))) {
            while (cursor.moveToNext()) {
                long rowId = getCursorLong(cursor, RecordHelper.PRIMARY_COLUMN_NAME);
                String sourcePackageName = getCursorString(cursor, SOURCE_PACKAGE_COLUMN_NAME);

                DeviceDataProviderMetadata metadata =
                        new DeviceDataProviderMetadata(sourcePackageName);

                idToMetadataMap.put(rowId, metadata);
                metadataToIdMap.put(metadata, rowId);
            }
        }

        mCache =
                new DeviceDataProviderMetadataHelper.MetadataCache(
                        new HashMap<>(idToMetadataMap), new HashMap<>(metadataToIdMap));
        return mCache;
    }

    /**
     * Returns a requests representing the tables that should be created corresponding to this
     * helper
     */
    public static CreateTableRequest getCreateTableRequest() {
        return new CreateTableRequest(TABLE_NAME, getColumnInfo());
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
        columnInfo.add(new Pair<>(SOURCE_PACKAGE_COLUMN_NAME, TEXT_NOT_NULL));

        return columnInfo;
    }

    private ContentValues getContentValues(DeviceDataProviderMetadata deviceDataProviderMetadata) {
        ContentValues contentValues = new ContentValues();

        contentValues.put(SOURCE_PACKAGE_COLUMN_NAME, deviceDataProviderMetadata.sourcePackageName);

        return contentValues;
    }

    @Override
    protected String getMainTableName() {
        return TABLE_NAME;
    }
}
