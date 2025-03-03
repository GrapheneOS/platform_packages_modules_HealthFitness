/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.server.healthconnect.exportimport;

import static android.health.connect.Constants.DEFAULT_LONG;
import static android.health.connect.Constants.DEFAULT_PAGE_SIZE;
import static android.health.connect.PageTokenWrapper.EMPTY_PAGE_TOKEN;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_EXERCISE_SESSION;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_PLANNED_EXERCISE_SESSION;

import static com.android.healthfitness.flags.AconfigFlagHelper.isCloudBackupRestoreEnabled;
import static com.android.server.healthconnect.storage.datatypehelpers.HealthDataCategoryPriorityHelper.APP_ID_PRIORITY_ORDER_COLUMN_NAME;
import static com.android.server.healthconnect.storage.datatypehelpers.HealthDataCategoryPriorityHelper.HEALTH_DATA_CATEGORY_COLUMN_NAME;
import static com.android.server.healthconnect.storage.datatypehelpers.HealthDataCategoryPriorityHelper.PRIORITY_TABLE_NAME;
import static com.android.server.healthconnect.storage.datatypehelpers.MedicalDataSourceHelper.getReadQueryForDataSourcesUsingUniqueIds;
import static com.android.server.healthconnect.storage.utils.StorageUtils.DELIMITER;
import static com.android.server.healthconnect.storage.utils.StorageUtils.checkTableExists;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorLong;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorString;

import static java.util.Objects.requireNonNull;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.health.connect.PageTokenWrapper;
import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.datatypes.MedicalDataSource;
import android.health.connect.datatypes.MedicalResource;
import android.health.connect.datatypes.Record;
import android.health.connect.internal.datatypes.PlannedExerciseSessionRecordInternal;
import android.health.connect.internal.datatypes.RecordInternal;
import android.health.connect.internal.datatypes.utils.HealthConnectMappings;
import android.util.ArrayMap;
import android.util.Pair;
import android.util.Slog;

import com.android.healthfitness.flags.Flags;
import com.android.server.healthconnect.phr.PhrPageTokenWrapper;
import com.android.server.healthconnect.phr.ReadMedicalResourcesInternalResponse;
import com.android.server.healthconnect.storage.HealthConnectDatabase;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.datatypehelpers.AppInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.ChangeLogsRequestHelper;
import com.android.server.healthconnect.storage.datatypehelpers.DeviceInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.HealthDataCategoryPriorityHelper;
import com.android.server.healthconnect.storage.datatypehelpers.MedicalDataSourceHelper;
import com.android.server.healthconnect.storage.datatypehelpers.MedicalResourceHelper;
import com.android.server.healthconnect.storage.datatypehelpers.MedicalResourceIndicesHelper;
import com.android.server.healthconnect.storage.datatypehelpers.RecordHelper;
import com.android.server.healthconnect.storage.request.DeleteTableRequest;
import com.android.server.healthconnect.storage.request.ReadTableRequest;
import com.android.server.healthconnect.storage.request.ReadTransactionRequest;
import com.android.server.healthconnect.storage.request.UpsertTransactionRequest;
import com.android.server.healthconnect.storage.utils.InternalHealthConnectMappings;
import com.android.server.healthconnect.storage.utils.StorageUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Merges a secondary database's contents with the HC database. This will be used in D2D migration
 * and Export/Import.
 *
 * @hide
 */
public final class DatabaseMerger {

    private static final String TAG = "HealthConnectDatabaseMerger";

    private final TransactionManager mTransactionManager;
    private final AppInfoHelper mAppInfoHelper;
    private final HealthConnectMappings mHealthConnectMappings;
    private final InternalHealthConnectMappings mInternalHealthConnectMappings;
    private final HealthDataCategoryPriorityHelper mHealthDataCategoryPriorityHelper;
    private final DeviceInfoHelper mDeviceInfoHelper;

    /*
     * Record types in this list will always be migrated such that the ordering here is respected.
     * When adding a new priority override, group the types that need to migrated together within
     * their own list. This makes the logical separation clear and also reduces storage usage during
     * migration, as we delete the original records.
     */
    public static final List<List<Integer>> RECORD_TYPE_MIGRATION_ORDERING_OVERRIDES =
            List.of(
                    // Training plans must be migrated before exercise sessions. Exercise sessions
                    // may contain a reference to a training plan, so the training plan needs to
                    // exist so that the foreign key constraints are not violated.
                    List.of(RECORD_TYPE_PLANNED_EXERCISE_SESSION, RECORD_TYPE_EXERCISE_SESSION));

    private static final List<String> PHR_TABLES_TO_MERGE =
            List.of(
                    MedicalDataSourceHelper.getMainTableName(),
                    MedicalResourceHelper.getMainTableName(),
                    MedicalResourceIndicesHelper.getTableName());

    public DatabaseMerger(
            AppInfoHelper appInfoHelper,
            DeviceInfoHelper deviceInfoHelper,
            HealthDataCategoryPriorityHelper healthDataCategoryPriorityHelper,
            TransactionManager transactionManager) {
        mTransactionManager = transactionManager;
        mAppInfoHelper = appInfoHelper;
        mHealthConnectMappings = HealthConnectMappings.getInstance();
        mInternalHealthConnectMappings = InternalHealthConnectMappings.getInstance();
        mHealthDataCategoryPriorityHelper = healthDataCategoryPriorityHelper;
        mDeviceInfoHelper = deviceInfoHelper;
    }

    /** Merge data */
    @SuppressWarnings("NullAway") // TODO(b/317029272): fix this suppression
    public synchronized void merge(HealthConnectDatabase stagedDatabase) {
        TransactionManager stagedTransactionManager =
                TransactionManager.forStagedDatabase(
                        stagedDatabase, mInternalHealthConnectMappings);

        Slog.i(TAG, "Merging app info");

        Map<Long, String> stagedPackageNamesByAppIds = new ArrayMap<>();
        try (Cursor cursor = read(stagedDatabase, new ReadTableRequest(AppInfoHelper.TABLE_NAME))) {
            while (cursor.moveToNext()) {
                long rowId = getCursorLong(cursor, RecordHelper.PRIMARY_COLUMN_NAME);
                String packageName = getCursorString(cursor, AppInfoHelper.PACKAGE_COLUMN_NAME);
                String appName = getCursorString(cursor, AppInfoHelper.APPLICATION_COLUMN_NAME);
                stagedPackageNamesByAppIds.put(rowId, packageName);

                // If this package is not installed on the target device and is not present in the
                // health db, then fill the health db with the info from source db. According to the
                // security review b/341253579, we should not parse the imported icon.
                mAppInfoHelper.addOrUpdateAppInfoIfNoAppInfoEntryExists(packageName, appName);
            }
        }

        // Similar to current HC behaviour, we honour what is on the target device. This means
        // that if a MedicalResource or MedicalDataSource of the same unique ids as the
        // stagedDatabase exists on the targetDatabase, we ignore the one in stagedDatabase.
        // TODO(b/376645901): Verify that there's no timeout with large datasets on actual
        // devices.
        if (Flags.personalHealthRecordEnableD2dAndExportImport()) {
            Slog.i(TAG, "Merging PHR data");
            try {
                mergePhrContent(stagedDatabase.getReadableDatabase());
            } catch (Exception e) {
                Slog.e(TAG, "Failed to transfer PHR data from staged database", e);
            }
        }

        Slog.i(TAG, "Merging records");

        // Determine the order in which we should migrate data types. This involves first
        // migrating data types according to the specified ordering overrides. Remaining
        // records are migrated in no particular order.
        List<Integer> recordTypesWithOrderingOverrides =
                RECORD_TYPE_MIGRATION_ORDERING_OVERRIDES.stream().flatMap(List::stream).toList();
        List<Integer> recordTypesWithoutOrderingOverrides =
                mHealthConnectMappings.getRecordIdToExternalRecordClassMap().keySet().stream()
                        .filter(it -> !recordTypesWithOrderingOverrides.contains(it))
                        .toList();

        // Migrate special case records in their defined order.
        for (List<Integer> recordTypeMigrationGroup : RECORD_TYPE_MIGRATION_ORDERING_OVERRIDES) {
            for (int recordTypeToMigrate : recordTypeMigrationGroup) {
                mergeRecordsOfType(
                        stagedTransactionManager,
                        stagedDatabase,
                        stagedPackageNamesByAppIds,
                        recordTypeToMigrate);
            }
            // Delete records within a group together, once all records within that group
            // have been migrated. This ensures referential integrity is preserved during
            // migration.
            for (int recordTypeToMigrate : recordTypeMigrationGroup) {
                deleteRecordsOfType(stagedDatabase, recordTypeToMigrate);
            }
        }
        // Migrate remaining record types in no particular order.
        for (Integer recordTypeToMigrate : recordTypesWithoutOrderingOverrides) {
            mergeRecordsOfType(
                    stagedTransactionManager,
                    stagedDatabase,
                    stagedPackageNamesByAppIds,
                    recordTypeToMigrate);
            deleteRecordsOfType(stagedDatabase, recordTypeToMigrate);
        }

        Slog.i(TAG, "Syncing app info records after restored data merge");
        mAppInfoHelper.syncAppInfoRecordTypesUsed();

        Slog.i(TAG, "Merging priority list");
        mergePriorityList(stagedDatabase, stagedPackageNamesByAppIds);

        Slog.i(TAG, "Merging done");
    }

    private void mergePhrContent(SQLiteDatabase stagedDatabase) {
        if (!checkPhrTablesExist(stagedDatabase)) {
            return;
        }
        // We have made the decision to not transfer partial PHR data to the target device.
        // Hence why we wrap it in a transaction to ensure either all or none of the PHR
        // data is transferred to the target device.
        mTransactionManager.runAsTransaction(
                targetDatabase -> {
                    Map<String, Long> dataSourceUuidToRowId =
                            mergeMedicalDataSourceTable(stagedDatabase, targetDatabase);
                    mergeMedicalResourceAndIndices(
                            stagedDatabase, targetDatabase, dataSourceUuidToRowId);
                });
    }

    private boolean checkPhrTablesExist(SQLiteDatabase stagedDatabase) {
        for (String table : PHR_TABLES_TO_MERGE) {
            if (!checkTableExists(stagedDatabase, table)) {
                return false;
            }
        }
        return true;
    }

    private Map<String, Long> mergeMedicalDataSourceTable(
            SQLiteDatabase stagedDatabase, SQLiteDatabase targetDatabase) {
        // Read the dataSources from the staged database along with their lastModifiedTimestamp.
        // We don't want to update the lastModifiedTimestamp, as this currently holds a different
        // meaning in PHR. We use the lastModifiedTimestamp columns in MedicalResource and
        // MedicalDataSource to understand when an app has updated the MedicalResource/DataSource.
        // Since the merge process is not the source app writing the data, we write the
        // lastModifiedTimestamp using what is in the stagedDatabase rather than based on the
        // current merge time.
        List<Pair<MedicalDataSource, Long>> dataSourceTimestampPairs =
                readMedicalDataSources(stagedDatabase);
        // To map dataSource uuid string to its rowId in the targetDatabase.
        Map<String, Long> uuidToRowId = new ArrayMap<>();
        for (Pair<MedicalDataSource, Long> dataSourceAndTimestamp : dataSourceTimestampPairs) {
            MedicalDataSource dataSource = dataSourceAndTimestamp.first;
            long lastModifiedTime = dataSourceAndTimestamp.second;
            // Get the appId from the target database.
            long appInfoId = mAppInfoHelper.getAppInfoId(dataSource.getPackageName());
            if (appInfoId == DEFAULT_LONG) {
                throw new IllegalStateException("App id does not exist.");
            }

            long insertedRowId =
                    targetDatabase.insertWithOnConflict(
                            MedicalDataSourceHelper.getMainTableName(),
                            /* nullColumnHack= */ null,
                            MedicalDataSourceHelper.getContentValues(
                                    dataSource, appInfoId, lastModifiedTime),
                            SQLiteDatabase.CONFLICT_IGNORE);

            // If insertedRowId is -1, there probably was a conflict. In this case, we need to do
            // a read on the targetDatabase, to find out the rowId of the existing dataSource
            // with the same unique ids as the one we were trying to insert.
            if (insertedRowId == DEFAULT_LONG) {
                insertedRowId =
                        readMedicalDataSourcesUsingDisplayNameAndAppId(
                                targetDatabase, dataSource.getDisplayName(), appInfoId);
            }

            uuidToRowId.put(dataSource.getId(), insertedRowId);
        }

        return uuidToRowId;
    }

    private void mergeMedicalResourceAndIndices(
            SQLiteDatabase stagedDatabase,
            SQLiteDatabase targetDatabase,
            Map<String, Long> uuidToRowId) {
        String nextPageToken = null;
        do {
            // Read MedicalResources from staged database.
            ReadMedicalResourcesInternalResponse response =
                    readMedicalResources(
                            stagedDatabase,
                            PhrPageTokenWrapper.fromPageTokenAllowingNull(nextPageToken));

            // Write MedicalResources to the target database.
            for (MedicalResource medicalResource : response.getMedicalResources()) {
                String dataSourceUuid = medicalResource.getDataSourceId();
                Long dataSourceRowId = uuidToRowId.get(dataSourceUuid);
                if (dataSourceRowId == null) {
                    throw new IllegalStateException("DataSource UUID was not found");
                }

                ContentValues contentValues =
                        MedicalResourceHelper.getContentValues(
                                dataSourceRowId,
                                medicalResource.getLastModifiedTimestamp(),
                                medicalResource);
                long medicalResourceRowId =
                        targetDatabase.insertWithOnConflict(
                                MedicalResourceHelper.getMainTableName(),
                                /* nullColumnHack= */ null,
                                contentValues,
                                SQLiteDatabase.CONFLICT_IGNORE);

                // With CONFLICT_IGNORE, if there already exists a row with the same unique ids
                // the insertion would be ignored and -1 is returned. In this case, we would
                // want to continue with copying the rest of the data.
                if (medicalResourceRowId != DEFAULT_LONG) {
                    targetDatabase.insertWithOnConflict(
                            MedicalResourceIndicesHelper.getTableName(),
                            /* nullColumnHack= */ null,
                            MedicalResourceIndicesHelper.getContentValues(
                                    medicalResourceRowId, medicalResource.getType()),
                            SQLiteDatabase.CONFLICT_IGNORE);
                }
            }

            nextPageToken = response.getPageToken();

        } while (nextPageToken != null);
    }

    private List<Pair<MedicalDataSource, Long>> readMedicalDataSources(
            SQLiteDatabase stagedDatabase) {
        try (Cursor cursor =
                read(stagedDatabase, MedicalDataSourceHelper.getReadQueryForDataSources())) {
            return MedicalDataSourceHelper.getMedicalDataSourcesWithTimestamps(cursor);
        }
    }

    private long readMedicalDataSourcesUsingDisplayNameAndAppId(
            SQLiteDatabase targetDatabase, String displayName, long appId) {
        try (Cursor cursor =
                mTransactionManager.read(
                        targetDatabase,
                        getReadQueryForDataSourcesUsingUniqueIds(displayName, appId))) {
            return MedicalDataSourceHelper.readDisplayNameAndAppIdFromCursor(cursor);
        }
    }

    private ReadMedicalResourcesInternalResponse readMedicalResources(
            SQLiteDatabase stagedDatabase, PhrPageTokenWrapper pageTokenWrapper) {
        ReadTableRequest readTableRequest =
                MedicalResourceHelper.getReadTableRequestUsingRequestFilters(
                        pageTokenWrapper, DEFAULT_PAGE_SIZE);
        return MedicalResourceHelper.getMedicalResources(
                stagedDatabase, readTableRequest, pageTokenWrapper, DEFAULT_PAGE_SIZE);
    }

    private void mergePriorityList(
            HealthConnectDatabase stagedDatabase, Map<Long, String> importedAppInfo) {
        Map<Integer, List<String>> importPriorityMap = new HashMap<>();
        try (Cursor cursor = read(stagedDatabase, new ReadTableRequest(PRIORITY_TABLE_NAME))) {
            while (cursor.moveToNext()) {
                int dataCategory =
                        cursor.getInt(
                                cursor.getColumnIndexOrThrow(HEALTH_DATA_CATEGORY_COLUMN_NAME));
                List<Long> appIdsInOrder =
                        StorageUtils.getCursorLongList(
                                cursor, APP_ID_PRIORITY_ORDER_COLUMN_NAME, DELIMITER);
                Slog.i(TAG, "Priority count for " + dataCategory + ": " + appIdsInOrder.size());
                importPriorityMap.put(
                        dataCategory, getPackageNamesFromImport(appIdsInOrder, importedAppInfo));
            }
        }

        importPriorityMap.forEach(
                (category, importPriorityList) -> {
                    if (importPriorityList.isEmpty()) {
                        return;
                    }

                    List<String> currentPriorityList =
                            mAppInfoHelper.getPackageNames(
                                    mHealthDataCategoryPriorityHelper.getAppIdPriorityOrder(
                                            category));
                    List<String> newPriorityList =
                            Stream.concat(currentPriorityList.stream(), importPriorityList.stream())
                                    .distinct()
                                    .toList();
                    mHealthDataCategoryPriorityHelper.setPriorityOrder(category, newPriorityList);
                    Slog.d(
                            TAG,
                            "Added "
                                    + importPriorityList.size()
                                    + " apps to priority list of category "
                                    + category);
                });
    }

    private void mergeRecordsOfType(
            TransactionManager stagedTransactionManager,
            HealthConnectDatabase stagedDatabase,
            Map<Long, String> stagedPackageNamesByAppIds,
            int recordType) {
        RecordHelper<?> recordHelper = mInternalHealthConnectMappings.getRecordHelper(recordType);
        if (!checkTableExists(
                stagedDatabase.getReadableDatabase(), recordHelper.getMainTableName())) {
            return;
        }

        Class<? extends Record> recordTypeClass =
                mHealthConnectMappings.getRecordIdToExternalRecordClassMap().get(recordType);
        // Read all the records of the given type from the staged db and insert them into the
        // existing healthconnect db.
        PageTokenWrapper currentToken = EMPTY_PAGE_TOKEN;
        do {
            var recordsToMergeAndToken =
                    getRecordsToMerge(
                            stagedTransactionManager,
                            stagedPackageNamesByAppIds,
                            requireNonNull(recordTypeClass),
                            currentToken,
                            recordHelper);
            List<RecordInternal<?>> records = recordsToMergeAndToken.first;
            PageTokenWrapper token = recordsToMergeAndToken.second;
            if (records.isEmpty()) {
                Slog.d(TAG, "No records to merge: " + recordTypeClass);
                break;
            }
            Slog.d(TAG, "Found records to merge: " + recordTypeClass);
            if (recordType == RECORD_TYPE_PLANNED_EXERCISE_SESSION) {
                // For training plans we nullify any autogenerated references to exercise sessions.
                // When the corresponding exercise sessions get migrated, these references will be
                // automatically generated again.
                records.forEach(
                        it -> {
                            PlannedExerciseSessionRecordInternal record =
                                    (PlannedExerciseSessionRecordInternal) it;
                            record.setCompletedExerciseSessionId(null);
                        });
            }
            // Using null package name for making insertion for two reasons:
            // 1. we don't want to update the logs for this package.
            // 2. we don't want to update the package name in the records as they already have the
            //    correct package name.
            UpsertTransactionRequest upsertTransactionRequest =
                    UpsertTransactionRequest.createForRestore(
                            records,
                            mTransactionManager,
                            mInternalHealthConnectMappings,
                            mDeviceInfoHelper,
                            mAppInfoHelper);

            // Both methods use ON CONFLICT IGNORE strategy, which means that if the source data
            // being inserted into target db already exists, the source data will be ignored. We
            // won't apply updates to the target data.
            //
            // Only generate change logs when any change logs token are present. Client apps can
            // only read change logs if they have ever requested a change logs token.
            if (isCloudBackupRestoreEnabled()
                    && mTransactionManager.checkTableExists(ChangeLogsRequestHelper.TABLE_NAME)
                    && mTransactionManager.queryNumEntries(ChangeLogsRequestHelper.TABLE_NAME)
                            != 0) {
                upsertTransactionRequest.execute();
            } else {
                mTransactionManager.insertOrIgnoreAllOnConflict(
                        upsertTransactionRequest.getUpsertRequests());
            }
            currentToken = token;
        } while (!currentToken.isEmpty());
    }

    private void deleteRecordsOfType(HealthConnectDatabase stagedDatabase, int recordType) {
        RecordHelper<?> recordHelper = mInternalHealthConnectMappings.getRecordHelper(recordType);
        if (!checkTableExists(
                stagedDatabase.getReadableDatabase(), recordHelper.getMainTableName())) {
            return;
        }

        // Passing -1 for startTime and endTime as we don't want to have time based filtering in the
        // final query.
        Class<? extends Record> recordTypeClass =
                mHealthConnectMappings.getRecordIdToExternalRecordClassMap().get(recordType);
        Slog.d(TAG, "Deleting table for: " + recordTypeClass);
        @SuppressWarnings("NullAway") // TODO(b/317029272): fix this suppression
        DeleteTableRequest deleteTableRequest =
                recordHelper.getDeleteTableRequest(
                        null /* packageFilters */,
                        DEFAULT_LONG /* startTime */,
                        DEFAULT_LONG /* endTime */,
                        false /* useLocalTimeFilter */,
                        mAppInfoHelper);

        stagedDatabase.getWritableDatabase().execSQL(deleteTableRequest.getDeleteCommand());
    }

    private Pair<List<RecordInternal<?>>, PageTokenWrapper> getRecordsToMerge(
            TransactionManager stagedTransactionManager,
            Map<Long, String> stagedPackageNamesByAppIds,
            Class<? extends Record> recordTypeClass,
            PageTokenWrapper requestToken,
            RecordHelper<?> recordHelper) {
        ReadRecordsRequestUsingFilters<?> readRecordsRequest =
                new ReadRecordsRequestUsingFilters.Builder<>(recordTypeClass)
                        .setPageSize(DEFAULT_PAGE_SIZE)
                        .setPageToken(requestToken.encode())
                        .build();

        Set<String> grantedExtraReadPermissions =
                Set.copyOf(recordHelper.getExtraReadPermissions());

        // Working with startDateAccess of -1 as we don't want to have time based filtering in the
        // query.
        @SuppressWarnings("NullAway") // TODO(b/317029272): fix this suppression
        ReadTransactionRequest readTransactionRequest =
                new ReadTransactionRequest(
                        mAppInfoHelper,
                        null,
                        readRecordsRequest.toReadRecordsRequestParcel(),
                        // Avoid time based filtering.
                        /* startDateAccessMillis= */ DEFAULT_LONG,
                        /* enforceSelfRead= */ false,
                        grantedExtraReadPermissions,
                        // Make sure foreground only types get included in the response.
                        /* isInForeground= */ true);

        return stagedTransactionManager.readRecordsAndPageTokenWithoutAccessLogs(
                readTransactionRequest,
                mAppInfoHelper,
                mDeviceInfoHelper,
                stagedPackageNamesByAppIds);
    }

    private synchronized Cursor read(
            HealthConnectDatabase stagedDatabase, ReadTableRequest request) {
        return read(stagedDatabase.getReadableDatabase(), request.getReadCommand());
    }

    private synchronized Cursor read(SQLiteDatabase stagedDatabase, String query) {
        Slog.d(TAG, "Running command: " + query);
        Cursor cursor = stagedDatabase.rawQuery(query, null);
        Slog.d(TAG, "Cursor count: " + cursor.getCount());
        return cursor;
    }

    /**
     * Returns a list of package names, mapped from the passed-in {@code packageIds} list using the
     * mapping from the import file.
     */
    private static List<String> getPackageNamesFromImport(
            List<Long> packageIds, Map<Long, String> importedPackageNameMapping) {
        List<String> packageNames = new ArrayList<>();
        if (packageIds == null || packageIds.isEmpty() || importedPackageNameMapping.isEmpty()) {
            return packageNames;
        }
        packageIds.forEach(
                (packageId) -> {
                    String packageName = importedPackageNameMapping.get(packageId);
                    requireNonNull(packageName);
                    packageNames.add(packageName);
                });
        return packageNames;
    }
}
