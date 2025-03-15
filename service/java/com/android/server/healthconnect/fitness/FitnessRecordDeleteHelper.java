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

import static android.health.connect.accesslog.AccessLog.OperationType.OPERATION_TYPE_DELETE;
import static android.health.connect.accesslog.AccessLog.OperationType.OPERATION_TYPE_UPSERT;

import static com.android.server.healthconnect.storage.datatypehelpers.RecordHelper.APP_INFO_ID_COLUMN_NAME;
import static com.android.server.healthconnect.storage.datatypehelpers.RecordHelper.UUID_COLUMN_NAME;

import static java.util.Objects.requireNonNull;

import android.annotation.Nullable;
import android.database.Cursor;
import android.health.connect.RecordIdFilter;
import android.health.connect.aidl.DeleteUsingFiltersRequestParcel;
import android.health.connect.internal.datatypes.utils.HealthConnectMappings;
import android.util.ArrayMap;
import android.util.ArraySet;

import com.android.healthfitness.flags.Flags;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.datatypehelpers.AccessLogsHelper;
import com.android.server.healthconnect.storage.datatypehelpers.AppInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.ChangeLogsHelper;
import com.android.server.healthconnect.storage.datatypehelpers.RecordHelper;
import com.android.server.healthconnect.storage.request.DeleteTableRequest;
import com.android.server.healthconnect.storage.request.ReadTableRequest;
import com.android.server.healthconnect.storage.request.UpsertTableRequest;
import com.android.server.healthconnect.storage.utils.InternalHealthConnectMappings;
import com.android.server.healthconnect.storage.utils.StorageUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Delete FitnessRecords from the database based on the given request, using the TransactionManager.
 *
 * <p>FitnessRecord refers to any of the record types defined in {@link
 * android.health.connect.datatypes.RecordTypeIdentifier};
 *
 * @hide
 */
public final class FitnessRecordDeleteHelper {
    private static final String TAG = "HealthConnectFitnessDelete";

    private final TransactionManager mTransactionManager;
    private final AppInfoHelper mAppInfoHelper;
    private final AccessLogsHelper mAccessLogsHelper;
    private final HealthConnectMappings mHealthConnectMappings;
    private final InternalHealthConnectMappings mInternalHealthConnectMappings;

    public FitnessRecordDeleteHelper(
            TransactionManager transactionManager,
            AppInfoHelper appInfoHelper,
            AccessLogsHelper accessLogsHelper,
            InternalHealthConnectMappings internalHealthConnectMappings) {
        mTransactionManager = transactionManager;
        mAppInfoHelper = appInfoHelper;
        mAccessLogsHelper = accessLogsHelper;
        mHealthConnectMappings = internalHealthConnectMappings.getExternalMappings();
        mInternalHealthConnectMappings = internalHealthConnectMappings;
    }

    /**
     * Delete records specified by the given request.
     *
     * @param callingPackageName The package name trying to delete the records.
     * @param request The request that specifies what to delete.
     * @param holdsDataManagementPermission Whether the caller holds data management permission
     * @param shouldRecordAccessLog Whether access logs should be recorded for this call
     * @return number of records deleted.
     */
    public int deleteRecords(
            String callingPackageName,
            DeleteUsingFiltersRequestParcel request,
            boolean holdsDataManagementPermission,
            boolean shouldRecordAccessLog) {
        if (request.usesIdFilters() && request.usesNonIdFilters()) {
            throw new IllegalArgumentException(
                    "Requests with both id and non-id filters are not" + " supported");
        }

        if (request.usesIdFilters()) {
            return deleteByIdFilter(
                    callingPackageName,
                    request,
                    holdsDataManagementPermission,
                    shouldRecordAccessLog);
        } else {
            return deleteByNonIdFilter(
                    callingPackageName,
                    request,
                    holdsDataManagementPermission,
                    shouldRecordAccessLog);
        }
    }

    private int deleteByIdFilter(
            String callingPackageName,
            DeleteUsingFiltersRequestParcel request,
            boolean holdsDataManagementPermission,
            boolean shouldRecordAccessLog) {
        List<DeleteTableRequest> deleteTableRequests =
                new ArrayList<>(request.getRecordTypeFilters().size());
        Set<Integer> recordTypeIds = new HashSet<>();

        List<RecordIdFilter> recordIds = request.getRecordIdFiltersParcel().getRecordIdFilters();
        Set<UUID> uuidSet = new ArraySet<>();
        Map<RecordHelper<?>, List<UUID>> recordTypeToUuids = new ArrayMap<>();
        for (RecordIdFilter recordId : recordIds) {
            RecordHelper<?> recordHelper =
                    mInternalHealthConnectMappings.getRecordHelper(
                            mHealthConnectMappings.getRecordType(recordId.getRecordType()));
            UUID uuid = StorageUtils.getUUIDFor(recordId, callingPackageName);
            if (uuidSet.contains(uuid)) {
                // id has been already been processed;
                continue;
            }
            recordTypeToUuids.putIfAbsent(recordHelper, new ArrayList<>());
            Objects.requireNonNull(recordTypeToUuids.get(recordHelper)).add(uuid);
            uuidSet.add(uuid);
        }

        recordTypeToUuids.forEach(
                (recordHelper, uuids) -> {
                    deleteTableRequests.add(recordHelper.getDeleteTableRequest(uuids));
                    recordTypeIds.add(recordHelper.getRecordIdentifier());
                });

        return delete(
                callingPackageName,
                deleteTableRequests,
                recordTypeIds,
                holdsDataManagementPermission,
                shouldRecordAccessLog);
    }

    private int deleteByNonIdFilter(
            String callingPackageName,
            DeleteUsingFiltersRequestParcel request,
            boolean holdsDataManagementPermission,
            boolean shouldRecordAccessLog) {
        List<DeleteTableRequest> deleteTableRequests =
                new ArrayList<>(request.getRecordTypeFilters().size());
        Set<Integer> recordTypeIds = new HashSet<>();

        List<Integer> recordTypeFilters = request.getRecordTypeFilters();
        if (recordTypeFilters == null || recordTypeFilters.isEmpty()) {
            recordTypeFilters =
                    new ArrayList<>(
                            HealthConnectMappings.getInstance()
                                    .getRecordIdToExternalRecordClassMap()
                                    .keySet());
        }

        recordTypeFilters.forEach(
                (recordType) -> {
                    RecordHelper<?> recordHelper =
                            mInternalHealthConnectMappings.getRecordHelper(recordType);
                    Objects.requireNonNull(recordHelper);

                    deleteTableRequests.add(
                            recordHelper.getDeleteTableRequest(
                                    request.getPackageNameFilters(),
                                    request.getStartTime(),
                                    request.getEndTime(),
                                    request.isLocalTimeFilter(),
                                    mAppInfoHelper));
                    recordTypeIds.add(recordHelper.getRecordIdentifier());
                });

        return delete(
                callingPackageName,
                deleteTableRequests,
                recordTypeIds,
                holdsDataManagementPermission,
                shouldRecordAccessLog);
    }

    private int delete(
            @Nullable String callingPackageName,
            List<DeleteTableRequest> deleteTableRequests,
            @Nullable Set<Integer> recordTypeIds,
            boolean holdsDataManagementPermission,
            boolean shouldRecordAccessLog) {
        if (shouldRecordAccessLog) {
            Objects.requireNonNull(recordTypeIds);
        }
        if (!holdsDataManagementPermission || shouldRecordAccessLog) {
            Objects.requireNonNull(callingPackageName);
        }

        long currentTime = Instant.now().toEpochMilli();
        ChangeLogsHelper.ChangeLogs deletionChangelogs =
                new ChangeLogsHelper.ChangeLogs(OPERATION_TYPE_DELETE, currentTime);
        ChangeLogsHelper.ChangeLogs modificationChangelogs =
                new ChangeLogsHelper.ChangeLogs(OPERATION_TYPE_UPSERT, currentTime);

        return mTransactionManager.runAsTransaction(
                db -> {
                    int numberOfRecordsDeleted = 0;
                    for (DeleteTableRequest deleteTableRequest : deleteTableRequests) {
                        final RecordHelper<?> recordHelper =
                                mInternalHealthConnectMappings.getRecordHelper(
                                        deleteTableRequest.getRecordType());
                        int innerRequestRecordsDeleted;
                        if (deleteTableRequest.requiresRead()) {
                            try (Cursor cursor =
                                    db.rawQuery(deleteTableRequest.getReadCommand(), null)) {
                                int numberOfUuidsToDelete = 0;
                                while (cursor.moveToNext()) {
                                    String packageColumnName =
                                            requireNonNull(
                                                    deleteTableRequest.getPackageColumnName());
                                    String idColumnName =
                                            requireNonNull(deleteTableRequest.getIdColumnName());
                                    numberOfUuidsToDelete++;
                                    long readDataAppInfoId =
                                            StorageUtils.getCursorLong(cursor, packageColumnName);
                                    if (deleteTableRequest.requiresPackageCheck()) {
                                        enforcePackageCheck(
                                                StorageUtils.getCursorUUID(cursor, idColumnName),
                                                readDataAppInfoId,
                                                Objects.requireNonNull(callingPackageName),
                                                holdsDataManagementPermission);
                                    }
                                    UUID deletedRecordUuid =
                                            StorageUtils.getCursorUUID(cursor, idColumnName);
                                    deletionChangelogs.addUUID(
                                            deleteTableRequest.getRecordType(),
                                            readDataAppInfoId,
                                            deletedRecordUuid);

                                    // Add changelogs for affected records, e.g. a training plan
                                    // being deleted will create changelogs for affected exercise
                                    // sessions.
                                    for (ReadTableRequest additionalChangelogUuidRequest :
                                            recordHelper
                                                    .getReadRequestsForRecordsModifiedByDeletion(
                                                            deletedRecordUuid)) {
                                        Cursor cursorAdditionalUuids =
                                                mTransactionManager.read(
                                                        additionalChangelogUuidRequest);
                                        while (cursorAdditionalUuids.moveToNext()) {
                                            modificationChangelogs.addUUID(
                                                    requireNonNull(
                                                                    additionalChangelogUuidRequest
                                                                            .getRecordHelper())
                                                            .getRecordIdentifier(),
                                                    StorageUtils.getCursorLong(
                                                            cursorAdditionalUuids,
                                                            APP_INFO_ID_COLUMN_NAME),
                                                    StorageUtils.getCursorUUID(
                                                            cursorAdditionalUuids,
                                                            UUID_COLUMN_NAME));
                                        }
                                        cursorAdditionalUuids.close();
                                    }
                                }
                                innerRequestRecordsDeleted = numberOfUuidsToDelete;
                            }
                        } else {
                            // TODO(b/401504096) Seems to be an unused code path, cleanup.
                            List<String> ids = deleteTableRequest.getIds();
                            if (ids == null) {
                                throw new IllegalStateException(
                                        "Called with no required reads and no list of ids set");
                            }
                            innerRequestRecordsDeleted = ids.size();
                        }
                        numberOfRecordsDeleted += innerRequestRecordsDeleted;
                        db.execSQL(deleteTableRequest.getDeleteCommand());
                    }

                    for (UpsertTableRequest insertRequestsForChangeLog :
                            deletionChangelogs.getUpsertTableRequests()) {
                        mTransactionManager.insert(db, insertRequestsForChangeLog);
                    }
                    for (UpsertTableRequest modificationChangelog :
                            modificationChangelogs.getUpsertTableRequests()) {
                        mTransactionManager.insert(db, modificationChangelog);
                    }
                    if (Flags.addMissingAccessLogs() && shouldRecordAccessLog) {
                        mAccessLogsHelper.recordDeleteAccessLog(
                                db,
                                Objects.requireNonNull(callingPackageName),
                                Objects.requireNonNull(recordTypeIds));
                    }
                    return numberOfRecordsDeleted;
                });
    }

    /**
     * Delete records for the given deleteTableRequests.
     *
     * @param deleteTableRequests list of delete requests for a record table.
     */
    public void deleteRecordsUnrestricted(List<DeleteTableRequest> deleteTableRequests) {
        delete(
                null,
                deleteTableRequests,
                null,
                /* holdsDataManagementPermission= */ true,
                /* shouldRecordAccessLog= */ false);
    }

    private void enforcePackageCheck(
            UUID uuid,
            long readDataAppInfoId,
            String callingPackageName,
            boolean holdsDataManagementPermission) {
        if (holdsDataManagementPermission) {
            // Skip this check if the caller has data management permission
            return;
        }

        long callingAppInfoId = mAppInfoHelper.getAppInfoId(callingPackageName);
        if (callingAppInfoId != readDataAppInfoId) {
            throw new IllegalArgumentException(callingAppInfoId + " is not the owner for " + uuid);
        }
    }
}
