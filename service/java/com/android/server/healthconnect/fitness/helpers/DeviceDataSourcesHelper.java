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

import static com.android.server.healthconnect.fitness.recordhelpers.RecordHelper.APP_INFO_ID_COLUMN_NAME;
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
import android.database.sqlite.SQLiteConstraintException;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.SymptomRecord;
import android.health.connect.device.DeviceDataAdvertisement;
import android.health.connect.device.DeviceDataTypeAdvertisement;
import android.health.connect.internal.datatypes.utils.HealthConnectMappings;
import android.util.Pair;
import android.util.Slog;

import androidx.annotation.VisibleForTesting;

import com.android.healthfitness.flags.AconfigFlagHelper;
import com.android.server.healthconnect.common.metadata.AppInfoHelper;
import com.android.server.healthconnect.fitness.recordhelpers.RecordHelper;
import com.android.server.healthconnect.storage.DatabaseHelper;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.request.CreateTableRequest;
import com.android.server.healthconnect.storage.request.DeleteTableRequest;
import com.android.server.healthconnect.storage.request.ReadTableRequest;
import com.android.server.healthconnect.storage.request.UpsertTableRequest;
import com.android.server.healthconnect.storage.utils.WhereClauses;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * A class to help with the DB transaction for storing device data source metadata and state.
 *
 * @hide
 */
public class DeviceDataSourcesHelper extends DatabaseHelper {
    private static final String TAG = "DeviceDataSourcesHelper";

    public static final String TABLE_NAME = "device_data_sources_table";
    public static final String SOURCE_PACKAGE_NAME = "source_package_name";
    public static final String DATA_TYPE = "data_type";
    public static final String DATA_SUBTYPE = "data_subtype";
    public static final String IS_AVAILABLE = "is_available";
    public static final String IS_USER_ENABLED = "is_user_enabled";
    public static final String IS_VISIBLE_BY_DEFAULT_IN_MATCHMAKING =
            "is_visible_by_default_in_matchmaking";

    // Only update the states isAvailable and isUserEnabled for each sourcePackageName, appInfoId
    // and dataType "key"
    public static final List<Pair<String, Integer>> UNIQUE_COLUMN_INFO =
            List.of(
                    new Pair<>(SOURCE_PACKAGE_NAME, TYPE_STRING),
                    new Pair<>(APP_INFO_ID_COLUMN_NAME, TYPE_STRING),
                    new Pair<>(DATA_TYPE, TYPE_STRING),
                    new Pair<>(DATA_SUBTYPE, TYPE_STRING));

    private final TransactionManager mTransactionManager;
    private final HealthConnectMappings mHealthConnectMappings;

    @VisibleForTesting
    public record DeviceDataProviderKey(
            String sourcePackageName, long appInfoId, int dataType, int dataSubtype) {}

    @VisibleForTesting
    public record DeviceDataProviderInfo(
            DeviceDataProviderKey key,
            boolean isAvailable,
            boolean isUserEnabled,
            boolean isVisibleByDefaultInMatchmaking) {}

    @Nullable
    private volatile ConcurrentHashMap<DeviceDataProviderKey, DeviceDataProviderInfo> mDdpCache;

    public DeviceDataSourcesHelper(
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

    @Override
    public synchronized void clearData(TransactionManager transactionManager) {
        if (!AconfigFlagHelper.isDeviceDataProvidersEnabled()) {
            return;
        }
        super.clearData(transactionManager);
    }

    /**
     * Returns a requests representing the tables that should be created corresponding to this
     * helper
     */
    public static CreateTableRequest getCreateTableRequest() {
        return new CreateTableRequest(TABLE_NAME, getColumnInfo())
                .addUniqueConstraints(
                        List.of(
                                SOURCE_PACKAGE_NAME,
                                APP_INFO_ID_COLUMN_NAME,
                                DATA_TYPE,
                                DATA_SUBTYPE))
                .addForeignKey(
                        /* referencedTable= */ AppInfoHelper.TABLE_NAME,
                        /* columnNames= */ List.of(APP_INFO_ID_COLUMN_NAME),
                        /* referencedColumnNames= */ List.of(RecordHelper.PRIMARY_COLUMN_NAME));
    }

    /**
     * Update the database with the provided device data source information.
     *
     * <p>Any data types no longer being advertised are removed from the database.
     */
    public synchronized void insertOrUpdateAdvertisement(
            String sourcePackageName,
            long appInfoId,
            DeviceDataAdvertisement deviceDataAdvertisement) {
        deleteObsoleteAdvertisements(sourcePackageName, appInfoId, deviceDataAdvertisement);

        for (DeviceDataTypeAdvertisement state :
                deviceDataAdvertisement.getDeviceDataTypeAdvertisements()) {
            int dataType = mHealthConnectMappings.getRecordType(state.getDataType());
            int dataSubtype = state.getSymptomType();
            DeviceDataProviderKey key =
                    new DeviceDataProviderKey(sourcePackageName, appInfoId, dataType, dataSubtype);
            DeviceDataProviderInfo ddpInfo =
                    new DeviceDataProviderInfo(
                            key,
                            state.isAvailable(),
                            state.isUserEnabled(),
                            state.isVisibleByDefaultInMatchmaking());

            if (!getDdpMap().containsKey(key) || !getDdpMap().get(key).equals(ddpInfo)) {
                insertOrUpdate(ddpInfo);
            }
        }
    }

    /** Returns a map of appInfoId -> (sourcePackageName -> list of DeviceDataTypeAdvertisement). */
    public Map<Long, Map<String, List<DeviceDataTypeAdvertisement>>>
            getDeviceDataTypeAdvertisements() {
        Map<Long, Map<String, List<DeviceDataTypeAdvertisement>>> appInfoIdToDdpAds =
                new HashMap<>();

        for (Map.Entry<DeviceDataProviderKey, DeviceDataProviderInfo> entry :
                getDdpMap().entrySet()) {
            DeviceDataProviderKey key = entry.getKey();
            DeviceDataProviderInfo info = entry.getValue();

            long appInfoId = key.appInfoId();
            String sourcePackageName = key.sourcePackageName();
            int dataType = key.dataType();
            int dataSubtype = key.dataSubtype();

            Class<? extends Record> recordClass =
                    mHealthConnectMappings.getRecordIdToExternalRecordClassMap().get(dataType);
            if (recordClass == null) {
                // Hypothetically possible if e.g. module rollback occurs.
                Slog.e(TAG, "Encountered unrecognised record type");
                continue;
            }

            DeviceDataTypeAdvertisement.Builder adBuilder =
                    new DeviceDataTypeAdvertisement.Builder(recordClass)
                            .setAvailable(info.isAvailable())
                            .setUserEnabled(info.isUserEnabled())
                            .setVisibleByDefaultInMatchmaking(
                                    info.isVisibleByDefaultInMatchmaking());

            // Only set symptom type for symptom records.
            if (SymptomRecord.class.isAssignableFrom(recordClass)) {
                adBuilder.setSymptomType(dataSubtype);
            }

            appInfoIdToDdpAds
                    .computeIfAbsent(appInfoId, k -> new HashMap<>())
                    .computeIfAbsent(sourcePackageName, k -> new ArrayList<>())
                    .add(adBuilder.build());
        }

        return appInfoIdToDdpAds;
    }

    /**
     * Returns a list of data types that have been advertised for the given {@code
     * sourcePackageName} and {@code appInfoId}.
     */
    public synchronized List<Integer> getAdvertisedDataTypes(
            String sourcePackageName, long appInfoId) {
        return getExistingAdvertisements(sourcePackageName, appInfoId).stream()
                .map(key -> key.dataType)
                .collect(Collectors.toList());
    }

    /** Returns a list of all {@code appInfoId}s for the sourcePackageName. */
    public synchronized List<Long> getAppInfoIds(String sourcePackageName) {
        List<Long> appInfoIds = new ArrayList<>();
        for (DeviceDataProviderKey key : getDdpMap().keySet()) {
            if (key.sourcePackageName.equals(sourcePackageName)) {
                appInfoIds.add(key.appInfoId);
            }
        }
        return appInfoIds;
    }

    /** Removes all advertisements for the given {@code sourcePackageName} and {@code appInfoId}. */
    public synchronized void deleteAdvertisements(String sourcePackageName, long appInfoId) {
        List<DeviceDataProviderKey> keysToDelete =
                getExistingAdvertisements(sourcePackageName, appInfoId);

        for (DeviceDataProviderKey key : keysToDelete) {
            delete(key);
        }
    }

    /**
     * Delete advertisements from the database for data types no longer present for the {@code
     * sourcePackageName} and {@code appInfoId}.
     */
    private synchronized void deleteObsoleteAdvertisements(
            String sourcePackageName,
            long appInfoId,
            DeviceDataAdvertisement latestDeviceDataAdvertisement) {
        List<DeviceDataProviderKey> existingAdvertisements =
                getExistingAdvertisements(sourcePackageName, appInfoId);

        Set<Pair<Integer, Integer>> latestTypeSubtypes = new HashSet<>();
        for (DeviceDataTypeAdvertisement state :
                latestDeviceDataAdvertisement.getDeviceDataTypeAdvertisements()) {
            latestTypeSubtypes.add(
                    new Pair<>(
                            mHealthConnectMappings.getRecordType(state.getDataType()),
                            state.getSymptomType()));
        }

        for (DeviceDataProviderKey existingAdvertisement : existingAdvertisements) {
            if (!latestTypeSubtypes.contains(
                    new Pair<>(
                            existingAdvertisement.dataType, existingAdvertisement.dataSubtype))) {
                delete(existingAdvertisement);
            }
        }
    }

    private List<DeviceDataProviderKey> getExistingAdvertisements(
            String sourcePackageName, long appInfoId) {
        return getDdpMap().keySet().stream()
                .filter(
                        key ->
                                key.sourcePackageName.equals(sourcePackageName)
                                        && key.appInfoId == appInfoId)
                .collect(Collectors.toList());
    }

    /**
     * Delete ddpInfo from the db and cache for the given sourcePackageName, appInfoId and dataType.
     */
    private synchronized void delete(DeviceDataProviderKey key) {
        mTransactionManager.delete(
                new DeleteTableRequest(TABLE_NAME)
                        .addExtraWhereClauses(
                                new WhereClauses(AND)
                                        .addWhereEqualsClause(
                                                SOURCE_PACKAGE_NAME, key.sourcePackageName)
                                        .addWhereEqualsClause(
                                                APP_INFO_ID_COLUMN_NAME,
                                                String.valueOf(key.appInfoId))
                                        .addWhereEqualsClause(
                                                DATA_TYPE, String.valueOf(key.dataType))
                                        .addWhereEqualsClause(
                                                DATA_SUBTYPE, String.valueOf(key.dataSubtype))));
        getDdpMap().remove(key);
    }

    /**
     * Insert ddpInfo if not present in the db or updates the states isAvailable and isUserEnabled
     * for the given sourcePackageName, appInfoId and dataType.
     */
    private synchronized void insertOrUpdate(DeviceDataProviderInfo ddpInfo) {
        getDdpMap().remove(ddpInfo.key);

        // We do not use the TransactionManager's convenience method
        // insertOrReplaceOnConflict here because it assumes that the unique columns
        // are OR-ed together (e.g. uuid OR client_id).
        // Here we have a composite key, so we need to AND the columns together.
        UpsertTableRequest request =
                new UpsertTableRequest(TABLE_NAME, getContentValues(ddpInfo), UNIQUE_COLUMN_INFO);
        try {
            mTransactionManager.insertOrThrowOnConflict(request);
        } catch (SQLiteConstraintException e) {
            request.setUpdateWhereClauses(
                    new WhereClauses(AND)
                            .addWhereEqualsClause(
                                    SOURCE_PACKAGE_NAME, ddpInfo.key.sourcePackageName)
                            .addWhereEqualsClause(
                                    APP_INFO_ID_COLUMN_NAME, String.valueOf(ddpInfo.key.appInfoId))
                            .addWhereEqualsClause(DATA_TYPE, String.valueOf(ddpInfo.key.dataType))
                            .addWhereEqualsClause(
                                    DATA_SUBTYPE, String.valueOf(ddpInfo.key.dataSubtype)));
            mTransactionManager.update(request);
        }

        getDdpMap().put(ddpInfo.key, ddpInfo);
    }

    /**
     * Returns a map of DDP key sourcePackageName, appInfoId, dataType <> key, isAvailable,
     * isUserEnabled, isVisibleByDefaultInMatchmaking.
     */
    @VisibleForTesting
    public ConcurrentHashMap<DeviceDataProviderKey, DeviceDataProviderInfo> getDdpMap() {
        if (mDdpCache == null) {
            mDdpCache = generateDdpCache(mTransactionManager);
        }
        return mDdpCache;
    }

    private synchronized ConcurrentHashMap<DeviceDataProviderKey, DeviceDataProviderInfo>
            generateDdpCache(TransactionManager transactionManager) {
        ConcurrentHashMap<DeviceDataProviderKey, DeviceDataProviderInfo> ddpInfoMap =
                new ConcurrentHashMap<>();
        try (Cursor cursor = transactionManager.read(new ReadTableRequest(TABLE_NAME))) {
            while (cursor.moveToNext()) {
                long appInfoId = getCursorInt(cursor, APP_INFO_ID_COLUMN_NAME);
                String sourcePackageName = getCursorString(cursor, SOURCE_PACKAGE_NAME);
                int dataType = getCursorInt(cursor, DATA_TYPE);
                int dataSubtype = getCursorInt(cursor, DATA_SUBTYPE);
                Class<? extends Record> recordClass =
                        mHealthConnectMappings.getRecordIdToExternalRecordClassMap().get(dataType);
                if (recordClass == null) {
                    // Hypothetically possible if e.g. module rollback occurs.
                    Slog.e(TAG, "Encountered unrecognised record type");
                    continue;
                }
                boolean isAvailable = getIntegerAndConvertToBoolean(cursor, IS_AVAILABLE);
                boolean isUserEnabled = getIntegerAndConvertToBoolean(cursor, IS_USER_ENABLED);
                boolean isVisibleByDefaultInMatchmaking =
                        getIntegerAndConvertToBoolean(cursor, IS_VISIBLE_BY_DEFAULT_IN_MATCHMAKING);

                DeviceDataProviderKey key =
                        new DeviceDataProviderKey(
                                sourcePackageName, appInfoId, dataType, dataSubtype);
                DeviceDataProviderInfo ddpInfo =
                        new DeviceDataProviderInfo(
                                key, isAvailable, isUserEnabled, isVisibleByDefaultInMatchmaking);
                ddpInfoMap.put(key, ddpInfo);
            }
        }

        return ddpInfoMap;
    }

    private ContentValues getContentValues(DeviceDataProviderInfo ddpInfo) {
        ContentValues contentValues = new ContentValues();

        contentValues.put(APP_INFO_ID_COLUMN_NAME, ddpInfo.key.appInfoId);
        contentValues.put(SOURCE_PACKAGE_NAME, ddpInfo.key.sourcePackageName);
        contentValues.put(DATA_TYPE, ddpInfo.key.dataType);
        // When not set, this defaults to zero which corresponds to the *_TYPE_UNKNOWN value.
        contentValues.put(DATA_SUBTYPE, ddpInfo.key.dataSubtype);
        contentValues.put(IS_AVAILABLE, ddpInfo.isAvailable);
        contentValues.put(IS_USER_ENABLED, ddpInfo.isUserEnabled);
        contentValues.put(
                IS_VISIBLE_BY_DEFAULT_IN_MATCHMAKING, ddpInfo.isVisibleByDefaultInMatchmaking);

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
        columnInfo.add(new Pair<>(APP_INFO_ID_COLUMN_NAME, INTEGER_NOT_NULL)); // Foreign key
        columnInfo.add(new Pair<>(SOURCE_PACKAGE_NAME, TEXT_NOT_NULL));
        columnInfo.add(new Pair<>(DATA_TYPE, INTEGER_NOT_NULL));
        columnInfo.add(new Pair<>(DATA_SUBTYPE, INTEGER_NOT_NULL));
        columnInfo.add(new Pair<>(IS_AVAILABLE, INTEGER_NOT_NULL));
        columnInfo.add(new Pair<>(IS_USER_ENABLED, INTEGER_NOT_NULL));
        columnInfo.add(new Pair<>(IS_VISIBLE_BY_DEFAULT_IN_MATCHMAKING, INTEGER_NOT_NULL));

        return columnInfo;
    }

    @Override
    protected String getMainTableName() {
        return TABLE_NAME;
    }

    /** Returns a set of all advertised record types from all DDPs. */
    public Set<Integer> getAllAdvertisedRecordTypes() {
        return getDdpMap().keySet().stream().map(key -> key.dataType).collect(Collectors.toSet());
    }
}
