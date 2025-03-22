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

import static android.health.connect.accesslog.AccessLog.OperationType.OPERATION_TYPE_UPSERT;

import static com.android.server.healthconnect.storage.datatypehelpers.RecordHelper.APP_INFO_ID_COLUMN_NAME;
import static com.android.server.healthconnect.storage.datatypehelpers.RecordHelper.UUID_COLUMN_NAME;
import static com.android.server.healthconnect.storage.utils.StorageUtils.addNameBasedUUIDTo;
import static com.android.server.healthconnect.storage.utils.WhereClauses.LogicalOperator.AND;

import static java.util.Objects.requireNonNull;

import android.annotation.Nullable;
import android.database.Cursor;
import android.health.connect.Constants;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.health.connect.internal.datatypes.RecordInternal;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Slog;

import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.datatypehelpers.AccessLogsHelper;
import com.android.server.healthconnect.storage.datatypehelpers.AppInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.ChangeLogsHelper;
import com.android.server.healthconnect.storage.datatypehelpers.DeviceInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.RecordHelper;
import com.android.server.healthconnect.storage.request.ReadTableRequest;
import com.android.server.healthconnect.storage.request.UpsertTableRequest;
import com.android.server.healthconnect.storage.utils.InternalHealthConnectMappings;
import com.android.server.healthconnect.storage.utils.StorageUtils;
import com.android.server.healthconnect.storage.utils.WhereClauses;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
    private final InternalHealthConnectMappings mInternalHealthConnectMappings;

    /** Create an upsert request for insert API calls. */
    public FitnessRecordUpsertHelper(
            TransactionManager transactionManager,
            DeviceInfoHelper deviceInfoHelper,
            AppInfoHelper appInfoHelper,
            AccessLogsHelper accessLogsHelper,
            InternalHealthConnectMappings internalHealthConnectMappings) {
        mTransactionManager = transactionManager;
        mDeviceInfoHelper = deviceInfoHelper;
        mAppInfoHelper = appInfoHelper;
        mAccessLogsHelper = accessLogsHelper;
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
     * @param extraPermsStateMap A map of extra permissions and their grant state. An empty map
     *     means all permissions are granted.
     * @return List of UUIDs of the inserted records.
     */
    public List<String> insertRecords(
            String callingPackageName,
            List<? extends RecordInternal<?>> recordInternals,
            ArrayMap<String, Boolean> extraPermsStateMap) {
        for (RecordInternal<?> recordInternal : recordInternals) {
            // Override each record package to the given package i.e. the API caller package.
            StorageUtils.addPackageNameTo(recordInternal, callingPackageName);
            // For insert, we should generate a fresh UUID. Don't let the client choose it.
            addNameBasedUUIDTo(recordInternal);
        }

        return upsert(
                callingPackageName,
                recordInternals,
                /* isInsertRequest= */ true,
                /* shouldGenerateAccessLog= */ true,
                /* shouldGenerateChangeLog= */ true,
                /* shouldPreferNewRecord= */ true,
                /* updateLastModifiedTime= */ true,
                extraPermsStateMap);
    }

    /**
     * Update the given records from the given package name into Health Connect.
     *
     * <p>This method sanitises the records by overriding the package name each record. For UUID,
     * the passed in client id (preferred) / uuid is used.
     *
     * @param callingPackageName The package name inserting the records.
     * @param recordInternals The list of records to be inserted.
     * @param extraPermsStateMap A map of extra permissions and their grant state. An empty map
     *     means all permissions are granted.
     * @return List of UUIDs of the inserted records.
     */
    public List<String> updateRecords(
            String callingPackageName,
            List<? extends RecordInternal<?>> recordInternals,
            ArrayMap<String, Boolean> extraPermsStateMap) {
        for (RecordInternal<?> recordInternal : recordInternals) {
            // Override each record package to the given package i.e. the API caller package.
            StorageUtils.addPackageNameTo(recordInternal, callingPackageName);
            // For update requests, generate uuid if the clientRecordID is present, else use the
            // uuid passed as input.
            StorageUtils.updateNameBasedUUIDIfRequired(recordInternal);
        }
        return upsert(
                callingPackageName,
                recordInternals,
                /* isInsertRequest= */ false,
                /* shouldGenerateAccessLog= */ true,
                /* shouldGenerateChangeLog= */ true,
                /* shouldPreferNewRecord= */ true,
                /* updateLastModifiedTime= */ true,
                extraPermsStateMap);
    }

    /**
     * Insert the given records into Health Connect.
     *
     * <p>The records should have a pre-existing package name present.
     *
     * <p>This method prefers existing records, if a similar record is already present.
     *
     * @param recordInternals The list of records to be inserted.
     * @param shouldGenerateChangeLog Whether changelogs should be generated for these inserts.
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
                /* extraPermsStateMap= */ null);
    }

    private List<String> upsert(
            @Nullable String callingPackageName,
            List<? extends RecordInternal<?>> recordInternals,
            boolean isInsertRequest,
            boolean shouldGenerateAccessLog,
            boolean shouldGenerateChangeLog,
            boolean shouldPreferNewRecord,
            boolean updateLastModifiedTime,
            @Nullable ArrayMap<String, Boolean> extraPermsStateMap) {
        if (shouldGenerateAccessLog) {
            Objects.requireNonNull(callingPackageName);
        }

        List<UpsertTableRequest> upsertRequests = new ArrayList<>();
        @RecordTypeIdentifier.RecordType Set<Integer> recordTypes = new ArraySet<>();
        for (RecordInternal<?> recordInternal : recordInternals) {
            mAppInfoHelper.populateAppInfoId(recordInternal, /* requireAllFields= */ true);
            mDeviceInfoHelper.populateDeviceInfoId(recordInternal);
            recordTypes.add(recordInternal.getRecordType());
            if (updateLastModifiedTime) {
                recordInternal.setLastModifiedTime(Instant.now().toEpochMilli());
            }
            upsertRequests.add(
                    createUpsertRequestForRecord(
                            recordInternal, isInsertRequest, extraPermsStateMap));
        }

        if (Constants.DEBUG) {
            Slog.d(
                    TAG,
                    "Upsert transaction for "
                            + callingPackageName
                            + " with size "
                            + recordInternals.size());
        }

        long currentTime = Instant.now().toEpochMilli();
        ChangeLogsHelper.ChangeLogs upsertionChangelogs =
                new ChangeLogsHelper.ChangeLogs(OPERATION_TYPE_UPSERT, currentTime);
        ChangeLogsHelper.ChangeLogs otherModifiedRecordsChangelogs =
                new ChangeLogsHelper.ChangeLogs(OPERATION_TYPE_UPSERT, currentTime);

        return mTransactionManager.runAsTransaction(
                db -> {
                    for (UpsertTableRequest upsertRequest : upsertRequests) {
                        if (shouldGenerateChangeLog) {
                            upsertionChangelogs.addUUID(
                                    upsertRequest.getRecordInternal().getRecordType(),
                                    upsertRequest.getRecordInternal().getAppInfoId(),
                                    upsertRequest.getRecordInternal().getUuid());
                            addChangelogsForOtherModifiedRecords(
                                    mAppInfoHelper.getAppInfoId(
                                            upsertRequest.getRecordInternal().getPackageName()),
                                    upsertRequest,
                                    otherModifiedRecordsChangelogs);
                        }
                        if (isInsertRequest) {
                            if (shouldPreferNewRecord) {
                                mTransactionManager.insertOrReplaceOnConflict(db, upsertRequest);
                            } else {
                                mTransactionManager.insertOrIgnoreOnConflict(db, upsertRequest);
                            }
                        } else {
                            mTransactionManager.update(upsertRequest);
                        }
                    }
                    if (shouldGenerateChangeLog) {
                        for (UpsertTableRequest upsertRequestsForChangeLog :
                                upsertionChangelogs.getUpsertTableRequests()) {
                            mTransactionManager.insert(db, upsertRequestsForChangeLog);
                        }
                        for (UpsertTableRequest modificationChangelog :
                                otherModifiedRecordsChangelogs.getUpsertTableRequests()) {
                            mTransactionManager.insert(db, modificationChangelog);
                        }
                    }

                    if (shouldGenerateAccessLog) {
                        Objects.requireNonNull(mAccessLogsHelper)
                                .recordUpsertAccessLog(
                                        db,
                                        Objects.requireNonNull(callingPackageName),
                                        recordTypes);
                    }
                    return getUUIdsInOrder(upsertRequests);
                });
    }

    private List<String> getUUIdsInOrder(List<UpsertTableRequest> upsertRequests) {
        return upsertRequests.stream()
                .map((request) -> request.getRecordInternal().getUuid().toString())
                .collect(Collectors.toList());
    }

    private WhereClauses generateWhereClausesForUpdate(RecordInternal<?> recordInternal) {
        WhereClauses whereClauseForUpdateRequest = new WhereClauses(AND);
        whereClauseForUpdateRequest.addWhereEqualsClause(
                RecordHelper.UUID_COLUMN_NAME, StorageUtils.getHexString(recordInternal.getUuid()));
        whereClauseForUpdateRequest.addWhereEqualsClause(
                RecordHelper.APP_INFO_ID_COLUMN_NAME,
                /* expected args value */ String.valueOf(recordInternal.getAppInfoId()));
        return whereClauseForUpdateRequest;
    }

    private UpsertTableRequest createUpsertRequestForRecord(
            RecordInternal<?> recordInternal,
            boolean isInsertRequest,
            @Nullable ArrayMap<String, Boolean> extraPermsStateMap) {
        RecordHelper<?> recordHelper =
                mInternalHealthConnectMappings.getRecordHelper(recordInternal.getRecordType());

        UpsertTableRequest request =
                recordHelper.getUpsertTableRequest(recordInternal, extraPermsStateMap);
        request.setRecordType(recordHelper.getRecordIdentifier());
        if (!isInsertRequest) {
            request.setUpdateWhereClauses(generateWhereClausesForUpdate(recordInternal));
        }
        request.setRecordInternal(recordInternal);

        return request;
    }

    private void addChangelogsForOtherModifiedRecords(
            long callingPackageAppInfoId,
            UpsertTableRequest upsertRequest,
            ChangeLogsHelper.ChangeLogs modificationChangelogs) {
        // Carries out read requests provided by the record helper and uses the results to add
        // changelogs to the transaction.
        final RecordHelper<?> recordHelper =
                mInternalHealthConnectMappings.getRecordHelper(upsertRequest.getRecordType());
        for (ReadTableRequest additionalChangelogUuidRequest :
                recordHelper.getReadRequestsForRecordsModifiedByUpsertion(
                        upsertRequest.getRecordInternal().getUuid(),
                        upsertRequest,
                        callingPackageAppInfoId)) {
            Cursor cursorAdditionalUuids = mTransactionManager.read(additionalChangelogUuidRequest);
            while (cursorAdditionalUuids.moveToNext()) {
                RecordHelper<?> extraRecordHelper =
                        requireNonNull(additionalChangelogUuidRequest.getRecordHelper());
                modificationChangelogs.addUUID(
                        extraRecordHelper.getRecordIdentifier(),
                        StorageUtils.getCursorLong(cursorAdditionalUuids, APP_INFO_ID_COLUMN_NAME),
                        StorageUtils.getCursorUUID(cursorAdditionalUuids, UUID_COLUMN_NAME));
            }
            cursorAdditionalUuids.close();
        }
    }
}
