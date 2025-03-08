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

package com.android.server.healthconnect.fitness;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

import android.annotation.Nullable;
import android.database.Cursor;
import android.health.connect.PageTokenWrapper;
import android.health.connect.aidl.ReadRecordsRequestParcel;
import android.health.connect.internal.datatypes.RecordInternal;
import android.util.Pair;

import com.android.healthfitness.flags.Flags;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.datatypehelpers.AccessLogsHelper;
import com.android.server.healthconnect.storage.datatypehelpers.AppInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.DeviceInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.ReadAccessLogsHelper;
import com.android.server.healthconnect.storage.datatypehelpers.RecordHelper;
import com.android.server.healthconnect.storage.request.ReadTableRequest;
import com.android.server.healthconnect.storage.utils.InternalHealthConnectMappings;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Read FitnessRecords from the database based on the given request, using the TransactionManager.
 *
 * <p>FitnessRecord refers to any of the record types defined in {@link
 * android.health.connect.datatypes.RecordTypeIdentifier};
 *
 * @hide
 */
public class FitnessRecordReadHelper {
    public static final String TYPE_NOT_PRESENT_PACKAGE_NAME = "package_name";

    private final DeviceInfoHelper mDeviceInfoHelper;
    private final AppInfoHelper mAppInfoHelper;
    private final AccessLogsHelper mAccessLogsHelper;
    private final ReadAccessLogsHelper mReadAccessLogsHelper;

    public FitnessRecordReadHelper(
            DeviceInfoHelper deviceInfoHelper,
            AppInfoHelper appInfoHelper,
            AccessLogsHelper accessLogsHelper,
            ReadAccessLogsHelper readAccessLogsHelper) {
        mDeviceInfoHelper = deviceInfoHelper;
        mAppInfoHelper = appInfoHelper;
        mAccessLogsHelper = accessLogsHelper;
        mReadAccessLogsHelper = readAccessLogsHelper;
    }

    /**
     * Reads and returns a list of records for the given request, along with the next page token.
     */
    public Pair<List<RecordInternal<?>>, PageTokenWrapper> readRecords(
            TransactionManager transactionManager,
            String callingPackageName,
            ReadRecordsRequestParcel request,
            long startDateAccessMillis,
            boolean enforceSelfRead,
            Set<String> grantedExtraReadPermissions,
            boolean isInForeground,
            boolean shouldRecordAccessLog,
            @Nullable Map<Long, String> packageNamesByAppIds) {
        int recordTypeId = request.getRecordType();
        RecordHelper<?> recordHelper =
                InternalHealthConnectMappings.getInstance().getRecordHelper(recordTypeId);
        ReadTableRequest readTableRequest =
                recordHelper.getReadTableRequest(
                        request,
                        callingPackageName,
                        enforceSelfRead,
                        startDateAccessMillis,
                        grantedExtraReadPermissions,
                        isInForeground,
                        mAppInfoHelper);

        if (request.getRecordIdFiltersParcel() != null) {
            return Pair.create(
                    readRecordsByIdsInternal(
                            transactionManager,
                            callingPackageName,
                            Set.of(recordTypeId),
                            singletonList(readTableRequest),
                            shouldRecordAccessLog,
                            // TODO(b/366149374): Consider the case of read by id from other apps
                            true /* isReadingSelfData */),
                    PageTokenWrapper.EMPTY_PAGE_TOKEN);
        }

        PageTokenWrapper pageToken =
                PageTokenWrapper.from(request.getPageToken(), request.isAscending());
        int pageSize = request.getPageSize();
        boolean isReadingSelfData =
                request.getPackageFilters().equals(singletonList(callingPackageName));

        Pair<List<RecordInternal<?>>, PageTokenWrapper> readResult;
        try (Cursor cursor = transactionManager.read(readTableRequest)) {
            readResult =
                    recordHelper.getNextInternalRecordsPageAndToken(
                            mDeviceInfoHelper,
                            cursor,
                            pageSize,
                            pageToken,
                            packageNamesByAppIds,
                            mAppInfoHelper);
            populateInternalRecordsWithExtraData(
                    transactionManager, readResult.first, readTableRequest);
        }

        maybeRecordAccessLogs(
                transactionManager,
                callingPackageName,
                Set.of(recordTypeId),
                readResult.first,
                shouldRecordAccessLog && !isReadingSelfData);

        return readResult;
    }

    /** Reads and returns a list of records for the given record ids. */
    public List<RecordInternal<?>> readRecords(
            TransactionManager transactionManager,
            String callingPackageName,
            Map<Integer, List<UUID>> recordTypeToUuids,
            long startDateAccessMillis,
            Set<String> grantedExtraReadPermissions,
            boolean isInForeground,
            boolean shouldRecordAccessLog,
            boolean isReadingSelfData) {
        Set<Integer> recordTypeIds = recordTypeToUuids.keySet();
        List<ReadTableRequest> readTableRequests = new ArrayList<>();
        recordTypeToUuids.forEach(
                (recordType, uuids) ->
                        readTableRequests.add(
                                InternalHealthConnectMappings.getInstance()
                                        .getRecordHelper(recordType)
                                        .getReadTableRequest(
                                                callingPackageName,
                                                uuids,
                                                startDateAccessMillis,
                                                grantedExtraReadPermissions,
                                                isInForeground,
                                                mAppInfoHelper)));

        return readRecordsByIdsInternal(
                transactionManager,
                callingPackageName,
                recordTypeIds,
                readTableRequests,
                shouldRecordAccessLog,
                isReadingSelfData);
    }

    private List<RecordInternal<?>> readRecordsByIdsInternal(
            TransactionManager transactionManager,
            String callingPackageName,
            Set<Integer> recordTypeIds,
            List<ReadTableRequest> readTableRequests,
            boolean shouldRecordAccessLog,
            boolean isReadingSelfData) {
        List<RecordInternal<?>> recordInternals = new ArrayList<>();
        for (ReadTableRequest readTableRequest : readTableRequests) {
            RecordHelper<?> helper = readTableRequest.getRecordHelper();
            requireNonNull(helper);
            try (Cursor cursor = transactionManager.read(readTableRequest)) {
                List<RecordInternal<?>> internalRecords =
                        helper.getInternalRecords(cursor, mDeviceInfoHelper, mAppInfoHelper);
                populateInternalRecordsWithExtraData(
                        transactionManager, internalRecords, readTableRequest);
                recordInternals.addAll(internalRecords);
            }
        }

        maybeRecordAccessLogs(
                transactionManager,
                callingPackageName,
                recordTypeIds,
                recordInternals,
                shouldRecordAccessLog && !isReadingSelfData);

        return recordInternals;
    }

    private void maybeRecordAccessLogs(
            TransactionManager transactionManager,
            String callingPackageName,
            Set<Integer> recordTypeIds,
            List<RecordInternal<?>> recordInternals,
            boolean shouldRecordAccessLog) {
        if (!shouldRecordAccessLog) {
            return;
        }
        if (Flags.ecosystemMetrics()) {
            transactionManager.runWithoutTransaction(
                    db -> {
                        mReadAccessLogsHelper.recordAccessLogForNonAggregationReads(
                                db,
                                callingPackageName,
                                /* readTimeStamp= */ Instant.now().toEpochMilli(),
                                recordInternals);
                    });
        }
        if (Flags.addMissingAccessLogs()) {
            transactionManager.runWithoutTransaction(
                    db -> {
                        mAccessLogsHelper.recordReadAccessLog(
                                db, callingPackageName, recordTypeIds);
                    });
        }
    }

    /**
     * Do extra sql requests to populate optional extra data. Used to populate {@link
     * android.health.connect.internal.datatypes.ExerciseRouteInternal}.
     */
    private void populateInternalRecordsWithExtraData(
            TransactionManager transactionManager,
            List<RecordInternal<?>> records,
            ReadTableRequest request) {
        if (request.getExtraReadRequests() == null) {
            return;
        }
        for (ReadTableRequest extraDataRequest : request.getExtraReadRequests()) {
            Cursor cursorExtraData = transactionManager.read(extraDataRequest);
            RecordHelper<?> recordHelper = request.getRecordHelper();
            if (recordHelper == null) {
                throw new IllegalArgumentException(
                        "Extra read request with no attached record helper.");
            }
            recordHelper.updateInternalRecordsWithExtraFields(records, cursorExtraData);
        }
    }
}
