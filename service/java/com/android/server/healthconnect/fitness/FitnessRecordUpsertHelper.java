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

package com.android.server.healthconnect.fitness;

import static com.android.server.healthconnect.fitness.recordhelpers.RecordHelper.APP_INFO_ID_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.RecordHelper.UUID_COLUMN_NAME;
import static com.android.server.healthconnect.storage.utils.StorageUtils.addNameBasedUUIDTo;
import static com.android.server.healthconnect.storage.utils.WhereClauses.LogicalOperator.AND;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;

import android.annotation.Nullable;
import android.database.Cursor;
import android.health.connect.Constants;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.health.connect.internal.datatypes.RecordInternal;
import android.util.ArraySet;
import android.util.Slog;

import com.android.healthfitness.flags.AconfigFlagHelper;
import com.android.server.healthconnect.HealthConnectThreadScheduler;
import com.android.server.healthconnect.common.accesslog.AccessLogsHelper;
import com.android.server.healthconnect.common.changelog.ChangeLogsHelper.ChangeLogsTableRequests;
import com.android.server.healthconnect.common.metadata.AppInfoHelper;
import com.android.server.healthconnect.common.metadata.DeviceInfoHelper;
import com.android.server.healthconnect.fitness.helpers.RecordDateHelper;
import com.android.server.healthconnect.fitness.mappings.InternalHealthConnectMappings;
import com.android.server.healthconnect.fitness.recordhelpers.RecordHelper;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.request.UpsertTableRequest;
import com.android.server.healthconnect.storage.utils.StorageUtils;
import com.android.server.healthconnect.storage.utils.WhereClauses;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Upsert FitnessRecords from the database based on the given request, using the TransactionManager.
 *
 * <p>FitnessRecord refers to any of the record types defined in {@link
 * android.health.connect.datatypes.RecordTypeIdentifier};
 *
 * @hide
 */
public class FitnessRecordUpsertHelper {
    private static final String TAG = "HealthConnectUTR";

    private final TransactionManager mTransactionManager;
    private final DeviceInfoHelper mDeviceInfoHelper;
    private final AppInfoHelper mAppInfoHelper;
    private final AccessLogsHelper mAccessLogsHelper;
    private final RecordDateHelper mRecordDateHelper;
    private final HealthConnectThreadScheduler mThreadScheduler;
    private final InternalHealthConnectMappings mInternalHealthConnectMappings;

    /** Create an upsert request for insert API calls. */
    public FitnessRecordUpsertHelper(
            TransactionManager transactionManager,
            DeviceInfoHelper deviceInfoHelper,
            AppInfoHelper appInfoHelper,
            AccessLogsHelper accessLogsHelper,
            RecordDateHelper recordDateHelper,
            HealthConnectThreadScheduler threadScheduler,
            InternalHealthConnectMappings internalHealthConnectMappings) {
        mTransactionManager = transactionManager;
        mDeviceInfoHelper = deviceInfoHelper;
        mAppInfoHelper = appInfoHelper;
        mAccessLogsHelper = accessLogsHelper;
        mRecordDateHelper = recordDateHelper;
        mThreadScheduler = threadScheduler;
        mInternalHealthConnectMappings = internalHealthConnectMappings;
    }

    /**
     * Insert the given records from the given package name into Health Connect.
     *
     * <p>This method sanitises the records by overriding the package name and creating a new uuid
     * for each record.
     *
     * @param callingPackageName The package name inserting the records.
     * @param recordInternals The list of records to be inserted.
     * @param grantedPerRecordWritePermissions The granted per-record write permissions.
     * @param shouldGenerateAccessLogs Whether access logs should be generated or not.
     * @return List of UUIDs of the inserted records.
     */
    public List<String> insertRecords(
            String callingPackageName,
            List<? extends RecordInternal<?>> recordInternals,
            Set<String> grantedPerRecordWritePermissions,
            boolean shouldGenerateAccessLogs) {
        Map<Integer, List<RecordInternal<?>>> recordTypesToRecordInternals = new HashMap<>();
        for (RecordInternal<?> recordInternal : recordInternals) {
            // Override each record package to the given package i.e. the API caller package.
            StorageUtils.addPackageNameTo(recordInternal, callingPackageName);
            // For insert, we should generate a fresh UUID. Don't let the client choose it.
            addNameBasedUUIDTo(recordInternal);
            recordTypesToRecordInternals
                    .computeIfAbsent(recordInternal.getRecordType(), k -> new ArrayList<>())
                    .add(recordInternal);
        }

        enforcePreUpsertChecks(recordTypesToRecordInternals);

        List<String> insertedUuids =
                upsert(
                        callingPackageName,
                        recordInternals,
                        /* isInsertRequest= */ true,
                        shouldGenerateAccessLogs,
                        /* shouldGenerateChangeLog= */ true,
                        /* shouldPreferNewRecord= */ true,
                        /* updateLastModifiedTime= */ true,
                        grantedPerRecordWritePermissions);

        mThreadScheduler.scheduleInternalTask(
                () -> postInsertTasks(callingPackageName, recordInternals));
        return insertedUuids;
    }

    private void postInsertTasks(
            String callingPackageName, List<? extends RecordInternal<?>> recordInternals) {
        mRecordDateHelper.insertRecordDate(recordInternals);
        Set<Integer> recordsTypesInsertedSet =
                recordInternals.stream().map(RecordInternal::getRecordType).collect(toSet());
        // Update AppInfo table with the record types of records inserted in the request for the
        // current package.
        mAppInfoHelper.updateAppInfoRecordTypesUsedOnInsert(
                recordsTypesInsertedSet, callingPackageName);
    }

    /**
     * Update the given records from the given package name into Health Connect.
     *
     * <p>This method sanitises the records by overriding the package name each record. For UUID,
     * the passed in client id (preferred) / uuid is used.
     *
     * @param callingPackageName The package name inserting the records.
     * @param recordInternals The list of records to be inserted.
     * @param grantedPerRecordWritePermissions The granted per-record write permissions.
     * @param shouldGenerateAccessLogs Whether access logs should be generated or not.
     * @return List of UUIDs of the inserted records.
     */
    public List<String> updateRecords(
            String callingPackageName,
            List<? extends RecordInternal<?>> recordInternals,
            Set<String> grantedPerRecordWritePermissions,
            boolean shouldGenerateAccessLogs) {

        Map<Integer, List<RecordInternal<?>>> recordTypesToRecordInternals = new HashMap<>();
        for (RecordInternal<?> recordInternal : recordInternals) {
            // Override each record package to the given package i.e. the API caller package.
            StorageUtils.addPackageNameTo(recordInternal, callingPackageName);
            // For update requests, generate uuid if the clientRecordID is present, else use the
            // uuid passed as input.
            StorageUtils.updateNameBasedUUIDIfRequired(recordInternal);
            recordTypesToRecordInternals
                    .computeIfAbsent(recordInternal.getRecordType(), k -> new ArrayList<>())
                    .add(recordInternal);
        }

        enforcePreUpsertChecks(recordTypesToRecordInternals);

        List<String> updatedUuids =
                upsert(
                        callingPackageName,
                        recordInternals,
                        /* isInsertRequest= */ false,
                        shouldGenerateAccessLogs,
                        /* shouldGenerateChangeLog= */ true,
                        /* shouldPreferNewRecord= */ true,
                        /* updateLastModifiedTime= */ true,
                        grantedPerRecordWritePermissions);

        mThreadScheduler.scheduleInternalTask(
                () ->
                        mRecordDateHelper.reSyncByRecordTypeIds(
                                recordInternals.stream()
                                        .map(RecordInternal::getRecordType)
                                        .toList()));
        return updatedUuids;
    }

    /**
     * Insert the given records into Health Connect.
     *
     * <p>The records should have a pre-existing package name present.
     *
     * <p>This method prefers existing records, if a similar record is already present.
     *
     * <p>Note: This method doesn't run post delete tasks. In most cases, they should be run at the
     * end of the operation (e.g. with d2d transfer, once all the data has been merged).
     *
     * @param recordInternals The list of records to be inserted.
     * @param shouldGenerateChangeLog Whether change logs should be generated for these inserts.
     * @return List of uuids of the inserted records.
     */
    public List<String> insertRecordsUnrestricted(
            List<? extends RecordInternal<?>> recordInternals, boolean shouldGenerateChangeLog) {
        // Ensure each record has a record id set.
        for (RecordInternal<?> recordInternal : recordInternals) {
            Objects.requireNonNull(recordInternal.getUuid());
        }
        return upsert(
                /* callingPackageName= */ null,
                recordInternals,
                /* isInsertRequest= */ true,
                /* shouldGenerateAccessLog= */ false,
                shouldGenerateChangeLog,
                /* shouldPreferNewRecord= */ false,
                /* updateLastModifiedTime= */ false,
                mInternalHealthConnectMappings.getAllPerRecordWritePermissions());
    }

    private List<String> upsert(
            @Nullable String callingPackageName,
            List<? extends RecordInternal<?>> recordInternals,
            boolean isInsertRequest,
            boolean shouldGenerateAccessLog,
            boolean shouldGenerateChangeLog,
            boolean shouldPreferNewRecord,
            boolean updateLastModifiedTime,
            Set<String> grantedPerRecordWritePermissions) {
        if (shouldGenerateAccessLog) {
            Objects.requireNonNull(callingPackageName);
        }

        @RecordTypeIdentifier.RecordType Set<Integer> recordTypes = new ArraySet<>();
        for (RecordInternal<?> recordInternal : recordInternals) {
            mAppInfoHelper.populateAppInfoId(recordInternal, /* requireAllFields= */ true);
            mDeviceInfoHelper.populateDeviceInfoId(recordInternal);
            recordTypes.add(recordInternal.getRecordType());
            if (updateLastModifiedTime) {
                recordInternal.setLastModifiedTime(Instant.now().toEpochMilli());
            }
        }

        if (Constants.DEBUG) {
            Slog.d(
                    TAG,
                    "Upsert transaction for "
                            + callingPackageName
                            + " with size "
                            + recordInternals.size());
        }

        var currentTime = Instant.now();
        var upsertionChangeLogs = ChangeLogsTableRequests.ofUpsertion(currentTime);
        var otherModifiedRecordsChangeLogs = ChangeLogsTableRequests.ofUpsertion(currentTime);

        return mTransactionManager.runAsTransaction(
                db -> {
                    for (RecordInternal<?> recordInternal : recordInternals) {
                        UpsertTableRequest upsertRequest =
                                createUpsertRequestForRecord(
                                        recordInternal,
                                        isInsertRequest,
                                        grantedPerRecordWritePermissions);
                        if (shouldGenerateChangeLog) {
                            addChangeLogsForOtherModifiedRecords(
                                    recordInternal, otherModifiedRecordsChangeLogs);
                        }

                        if (isInsertRequest) {
                            if (shouldPreferNewRecord) {
                                mTransactionManager.insertOrReplaceOnConflict(db, upsertRequest);
                            } else {
                                mTransactionManager.insertOrIgnoreOnConflict(db, upsertRequest);
                            }
                        } else {
                            mTransactionManager.update(db, upsertRequest);
                        }

                        // RecordInternal objects are mutable and can be modified by
                        // mTransactionManager.insertOrReplaceOnConflict, therefore upsert change
                        // logs must be generated AFTER the upserts have taken places.
                        // See b/430891167
                        if (shouldGenerateChangeLog) {
                            upsertionChangeLogs.addRecordInfo(
                                    recordInternal.getRecordType(),
                                    recordInternal.getAppInfoId(),
                                    recordInternal.getUuid());
                        }
                    }
                    if (shouldGenerateChangeLog) {
                        for (UpsertTableRequest upsertRequestsForChangeLog :
                                upsertionChangeLogs.getUpsertTableRequests()) {
                            mTransactionManager.insertOrThrowOnConflict(
                                    db, upsertRequestsForChangeLog);
                        }
                        for (UpsertTableRequest modificationChangeLog :
                                otherModifiedRecordsChangeLogs.getUpsertTableRequests()) {
                            mTransactionManager.insertOrThrowOnConflict(db, modificationChangeLog);
                        }
                    }

                    if (shouldGenerateAccessLog) {
                        Objects.requireNonNull(mAccessLogsHelper)
                                .recordUpsertAccessLog(
                                        db,
                                        Objects.requireNonNull(callingPackageName),
                                        recordTypes);
                    }
                    return getUUIdsInOrder(recordInternals);
                });
    }

    private List<String> getUUIdsInOrder(List<? extends RecordInternal<?>> recordInternals) {
        return recordInternals.stream()
                .map((recordInternal) -> recordInternal.getUuid().toString())
                .collect(Collectors.toList());
    }

    private WhereClauses generateWhereClausesForUpdate(RecordInternal<?> recordInternal) {
        WhereClauses whereClauseForUpdateRequest = new WhereClauses(AND);
        whereClauseForUpdateRequest.addWhereEqualsClause(
                RecordHelper.UUID_COLUMN_NAME, StorageUtils.getHexString(recordInternal.getUuid()));
        whereClauseForUpdateRequest.addWhereEqualsClause(
                RecordHelper.APP_INFO_ID_COLUMN_NAME,
                /* expected args value */ String.valueOf(recordInternal.getAppInfoId()));
        // We filter for ids > 0 as valid SQLite row indices start at 1 (see
        // https://sqlite.org/autoinc.html).
        // Any value values below 1 (e.g., the internal initialization value DEFAULT_LONG) suggests
        // that an id is not set / invalid and should be ignored.
        if (AconfigFlagHelper.isDeviceDataProvidersEnabled()
                && recordInternal.getDeviceDataProviderId() > 0) {
            whereClauseForUpdateRequest.addWhereEqualsClause(
                    RecordHelper.DDP_ID_COLUMN_NAME,
                    String.valueOf(recordInternal.getDeviceDataProviderId()));
        }
        return whereClauseForUpdateRequest;
    }

    private UpsertTableRequest createUpsertRequestForRecord(
            RecordInternal<?> recordInternal,
            boolean isInsertRequest,
            Set<String> grantedPerRecordWritePermissions) {
        RecordHelper<?> recordHelper =
                mInternalHealthConnectMappings.getRecordHelper(recordInternal.getRecordType());

        UpsertTableRequest request =
                recordHelper.getUpsertTableRequest(
                        recordInternal, grantedPerRecordWritePermissions);
        if (!isInsertRequest) {
            request.setUpdateWhereClauses(generateWhereClausesForUpdate(recordInternal));
        }
        return request;
    }

    private void addChangeLogsForOtherModifiedRecords(
            RecordInternal<?> recordInternal, ChangeLogsTableRequests modificationChangeLogs) {
        // Carries out read requests provided by the record helper and uses the results to add
        // change logs to the transaction.
        final RecordHelper<?> recordHelper =
                mInternalHealthConnectMappings.getRecordHelper(recordInternal.getRecordType());
        for (RecordReadTableRequest additionalChangeLogUuidRequest :
                recordHelper.getReadRequestsForRecordsModifiedByUpsertion(recordInternal)) {
            Cursor cursorAdditionalUuids =
                    mTransactionManager.read(additionalChangeLogUuidRequest.getReadTableRequest());
            while (cursorAdditionalUuids.moveToNext()) {
                RecordHelper<?> extraRecordHelper =
                        requireNonNull(additionalChangeLogUuidRequest.getRecordHelper());
                modificationChangeLogs.addRecordInfo(
                        extraRecordHelper.getRecordIdentifier(),
                        StorageUtils.getCursorLong(cursorAdditionalUuids, APP_INFO_ID_COLUMN_NAME),
                        StorageUtils.getCursorUUID(cursorAdditionalUuids, UUID_COLUMN_NAME));
            }
            cursorAdditionalUuids.close();
        }
    }

    private void enforcePreUpsertChecks(
            Map<Integer, List<RecordInternal<?>>> recordTypesToRecordInternals) {
        if (!AconfigFlagHelper.isSymptomsEnabled()) {
            return;
        }

        for (Map.Entry<Integer, List<RecordInternal<?>>> recordTypeToRecordInternals :
                recordTypesToRecordInternals.entrySet()) {
            RecordHelper<?> recordHelper =
                    mInternalHealthConnectMappings.getRecordHelper(
                            recordTypeToRecordInternals.getKey());
            recordHelper.enforcePreUpsertChecks(
                    recordTypeToRecordInternals.getValue(), mTransactionManager);
        }
    }
}
