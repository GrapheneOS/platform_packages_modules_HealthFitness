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

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

import android.annotation.Nullable;
import android.database.Cursor;
import android.health.connect.RecordIdFilter;
import android.health.connect.aidl.DeleteUsingFiltersRequestParcel;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.health.connect.internal.datatypes.utils.HealthConnectMappings;
import android.util.ArrayMap;
import android.util.ArraySet;

import com.android.healthfitness.flags.Flags;
import com.android.server.healthconnect.HealthConnectThreadScheduler;
import com.android.server.healthconnect.common.accesslog.AccessLogsHelper;
import com.android.server.healthconnect.common.changelog.ChangeLogsHelper.ChangeLogsTableRequests;
import com.android.server.healthconnect.common.metadata.AppInfoHelper;
import com.android.server.healthconnect.fitness.helpers.RecordDateHelper;
import com.android.server.healthconnect.fitness.mappings.InternalHealthConnectMappings;
import com.android.server.healthconnect.fitness.recordhelpers.RecordHelper;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.request.UpsertTableRequest;
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
    private final RecordDateHelper mRecordDateHelper;
    private final HealthConnectThreadScheduler mThreadScheduler;
    private final HealthConnectMappings mHealthConnectMappings;
    private final InternalHealthConnectMappings mInternalHealthConnectMappings;

    public FitnessRecordDeleteHelper(
            TransactionManager transactionManager,
            AppInfoHelper appInfoHelper,
            AccessLogsHelper accessLogsHelper,
            RecordDateHelper recordDateHelper,
            HealthConnectThreadScheduler threadScheduler,
            InternalHealthConnectMappings internalHealthConnectMappings) {
        mTransactionManager = transactionManager;
        mAppInfoHelper = appInfoHelper;
        mAccessLogsHelper = accessLogsHelper;
        mRecordDateHelper = recordDateHelper;
        mThreadScheduler = threadScheduler;
        mHealthConnectMappings = internalHealthConnectMappings.getExternalMappings();
        mInternalHealthConnectMappings = internalHealthConnectMappings;
    }

    /**
     * Delete records specified by the given request.
     *
     * @param callingPackageName The package name trying to delete the records.
     * @param request The request that specifies what to delete.
     * @param enforceSelfDelete Whether the caller should only be able to delete their own data.
     * @param shouldRecordAccessLog Whether access logs should be recorded for this call
     * @return number of records deleted.
     */
    public int deleteRecords(
            String callingPackageName,
            DeleteUsingFiltersRequestParcel request,
            boolean enforceSelfDelete,
            boolean shouldRecordAccessLog) {
        if (request.usesIdFilters() && request.usesNonIdFilters()) {
            throw new IllegalArgumentException(
                    "Requests with both id and non-id filters are not" + " supported");
        }

        if (enforceSelfDelete) {
            request.setPackageNameFilters(singletonList(callingPackageName));
        }

        int recordsDeleted;
        if (request.usesIdFilters()) {
            recordsDeleted =
                    deleteByIdFilter(
                            callingPackageName, request, enforceSelfDelete, shouldRecordAccessLog);
        } else {
            recordsDeleted =
                    deleteByNonIdFilter(callingPackageName, request, shouldRecordAccessLog);
        }

        if (recordsDeleted > 0) {
            mThreadScheduler.scheduleInternalTask(() -> postDeleteTasks(request));
        }
        return recordsDeleted;
    }

    private void postDeleteTasks(DeleteUsingFiltersRequestParcel request) {
        if (request.getRecordTypeFilters().isEmpty()) {
            // Resync for all records in case a record filter is not specified.
            mAppInfoHelper.syncAppInfoRecordTypesUsed();
            mRecordDateHelper.reSyncForAllRecords();
        } else {
            List<Integer> recordTypeFilters = request.getRecordTypeFilters();
            mAppInfoHelper.syncAppInfoRecordTypesUsed(new HashSet<>(recordTypeFilters));
            mRecordDateHelper.reSyncByRecordTypeIds(recordTypeFilters);
        }
    }

    private int deleteByIdFilter(
            String callingPackageName,
            DeleteUsingFiltersRequestParcel request,
            boolean enforceSelfDelete,
            boolean shouldRecordAccessLog) {
        List<RecordDeleteTableRequest> deleteTableRequests =
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
                shouldRecordAccessLog,
                enforceSelfDelete);
    }

    private int deleteByNonIdFilter(
            String callingPackageName,
            DeleteUsingFiltersRequestParcel request,
            boolean shouldRecordAccessLog) {
        List<RecordDeleteTableRequest> deleteTableRequests =
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
                shouldRecordAccessLog,
                // Always send false here, since we set the package filters in the request itself.
                /* enforceSelfDelete= */ false);
    }

    private int delete(
            @Nullable String callingPackageName,
            List<RecordDeleteTableRequest> deleteTableRequests,
            @Nullable Set<Integer> recordTypeIds,
            boolean shouldRecordAccessLog,
            boolean enforceSelfDelete) {
        if (shouldRecordAccessLog) {
            Objects.requireNonNull(recordTypeIds);
        }
        if (shouldRecordAccessLog || enforceSelfDelete) {
            Objects.requireNonNull(callingPackageName);
        }

        var currentTime = Instant.now();
        var deletionChangeLogs = ChangeLogsTableRequests.ofDeletion(currentTime);
        var modificationChangeLogs = ChangeLogsTableRequests.ofUpsertion(currentTime);

        return mTransactionManager.runAsTransaction(
                db -> {
                    int numberOfRecordsDeleted = 0;
                    for (RecordDeleteTableRequest deleteTableRequest : deleteTableRequests) {
                        final RecordHelper<?> recordHelper =
                                mInternalHealthConnectMappings.getRecordHelper(
                                        deleteTableRequest.getRecordType());

                        // We first always read the records for:
                        // (1) generating change logs
                        // (2) logging number of records deleted
                        try (Cursor cursor =
                                db.rawQuery(deleteTableRequest.getReadCommand(), null)) {
                            while (cursor.moveToNext()) {
                                String packageColumnName =
                                        deleteTableRequest.getPackageColumnName();
                                String idColumnName = deleteTableRequest.getIdColumnName();
                                numberOfRecordsDeleted++;
                                long readDataAppInfoId =
                                        StorageUtils.getCursorLong(cursor, packageColumnName);
                                if (enforceSelfDelete) {
                                    enforcePackageCheck(
                                            StorageUtils.getCursorUUID(cursor, idColumnName),
                                            readDataAppInfoId,
                                            Objects.requireNonNull(callingPackageName));
                                }
                                UUID deletedRecordUuid =
                                        StorageUtils.getCursorUUID(cursor, idColumnName);
                                // TODO(b/448836403): Prevent insertion of Symptoms change log as we
                                //  are not sure yet what inserted ChangeLogs for Symptoms should
                                //  look like.
                                if (deleteTableRequest.getRecordType()
                                        != RecordTypeIdentifier.RECORD_TYPE_SYMPTOM) {
                                    deletionChangeLogs.addRecordInfo(
                                            deleteTableRequest.getRecordType(),
                                            readDataAppInfoId,
                                            deletedRecordUuid);
                                }

                                // Add change logs for affected records, e.g. a training plan
                                // being deleted will create change logs for affected exercise
                                // sessions.
                                for (RecordReadTableRequest additionalChangeLogUuidRequest :
                                        recordHelper.getReadRequestsForRecordsModifiedByDeletion(
                                                deletedRecordUuid)) {
                                    Cursor cursorAdditionalUuids =
                                            mTransactionManager.read(
                                                    additionalChangeLogUuidRequest
                                                            .getReadTableRequest());
                                    while (cursorAdditionalUuids.moveToNext()) {
                                        modificationChangeLogs.addRecordInfo(
                                                requireNonNull(
                                                                additionalChangeLogUuidRequest
                                                                        .getRecordHelper())
                                                        .getRecordIdentifier(),
                                                StorageUtils.getCursorLong(
                                                        cursorAdditionalUuids,
                                                        APP_INFO_ID_COLUMN_NAME),
                                                StorageUtils.getCursorUUID(
                                                        cursorAdditionalUuids, UUID_COLUMN_NAME));
                                    }
                                    cursorAdditionalUuids.close();
                                }
                            }
                        }
                        db.execSQL(deleteTableRequest.getDeleteTableRequest().getDeleteCommand());
                    }

                    for (UpsertTableRequest insertRequestsForChangeLog :
                            deletionChangeLogs.getUpsertTableRequests()) {
                        mTransactionManager.insertOrThrowOnConflict(db, insertRequestsForChangeLog);
                    }
                    for (UpsertTableRequest modificationChangeLog :
                            modificationChangeLogs.getUpsertTableRequests()) {
                        mTransactionManager.insertOrThrowOnConflict(db, modificationChangeLog);
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
     * <p>Note: This method doesn't run post delete tasks. In most cases, they should be run at the
     * end of the operation (e.g. with d2d transfer, once all the data has been merged).
     *
     * @param deleteTableRequests list of delete requests for a record table.
     */
    public void deleteRecordsUnrestricted(List<RecordDeleteTableRequest> deleteTableRequests) {
        delete(
                /* callingPackageName= */ null,
                deleteTableRequests,
                /* recordTypeIds= */ null,
                /* shouldRecordAccessLog= */ false,
                /* enforceSelfDelete= */ false);
    }

    private void enforcePackageCheck(UUID uuid, long readDataAppInfoId, String callingPackageName) {
        long callingAppInfoId = mAppInfoHelper.getAppInfoId(callingPackageName);
        if (callingAppInfoId != readDataAppInfoId) {
            throw new IllegalArgumentException(callingAppInfoId + " is not the owner for " + uuid);
        }
    }
}
