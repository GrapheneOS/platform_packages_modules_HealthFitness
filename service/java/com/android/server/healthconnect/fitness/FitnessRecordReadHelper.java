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

import static android.health.connect.Constants.DEFAULT_LONG;

import static java.util.Collections.singletonList;

import android.annotation.Nullable;
import android.database.Cursor;
import android.health.connect.PageTokenWrapper;
import android.health.connect.aidl.ReadRecordsRequestParcel;
import android.health.connect.internal.datatypes.RecordInternal;
import android.util.Pair;

import com.android.server.healthconnect.common.accesslog.AccessLogsHelper;
import com.android.server.healthconnect.common.accesslog.ReadAccessLogsHelper;
import com.android.server.healthconnect.common.metadata.AppInfoHelper;
import com.android.server.healthconnect.common.metadata.DeviceInfoHelper;
import com.android.server.healthconnect.fitness.mappings.InternalHealthConnectMappings;
import com.android.server.healthconnect.fitness.recordhelpers.RecordHelper;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.request.ReadTableRequest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private final InternalHealthConnectMappings mInternalHealthConnectMappings;

    // TODO(b/399825886): Inject transactionManager once this bug is fixed.
    public FitnessRecordReadHelper(
            DeviceInfoHelper deviceInfoHelper,
            AppInfoHelper appInfoHelper,
            AccessLogsHelper accessLogsHelper,
            ReadAccessLogsHelper readAccessLogsHelper,
            InternalHealthConnectMappings internalHealthConnectMappings) {
        mDeviceInfoHelper = deviceInfoHelper;
        mAppInfoHelper = appInfoHelper;
        mAccessLogsHelper = accessLogsHelper;
        mReadAccessLogsHelper = readAccessLogsHelper;
        mInternalHealthConnectMappings = internalHealthConnectMappings;
    }

    /**
     * Reads and returns a list of records for the given request, along with the next page token.
     *
     * @param transactionManager The TransactionManager to be used to perform this request.
     * @param callingPackageName The package name of the app making this request. This can be empty
     *     for internal calls.
     * @param request The read request describing what to read.
     * @param grantedExtraReadPermissions List of permissions granted to this app to read associated
     *     data.
     * @param grantedGranularPermissions A set of granted granular permissions for the read
     *     operation. These will be used to filter data in cases where a record type is associated
     *     with multiple permissions, ensuring only data permitted based on these permissions is
     *     returned.
     * @param startDateAccessMillis The earliest time this app is allowed to read from.
     * @param isInForeground If the calling app is in the foreground.
     * @param shouldRecordAccessLog If access logs should be recorded for this call.
     * @param enforceSelfRead Whether returned data should be filtered for data written by the
     *     calling app.
     * @param packageNamesByAppIds Map of package names to app Ids. If this is not present, app info
     *     is read using AppInfoHelper.
     * @return A pair containing the list of records for this request, along with the page token.
     */
    // TODO(b/399825886): packageNamesByAppIds is not consistently used and should be removed once
    // this bug is fixed.
    public Pair<List<RecordInternal<?>>, PageTokenWrapper> readRecords(
            TransactionManager transactionManager,
            String callingPackageName,
            ReadRecordsRequestParcel request,
            Set<String> grantedExtraReadPermissions,
            Set<String> grantedGranularPermissions,
            long startDateAccessMillis,
            boolean isInForeground,
            boolean shouldRecordAccessLog,
            boolean enforceSelfRead,
            @Nullable Map<Long, String> packageNamesByAppIds) {
        int recordTypeId = request.getRecordType();
        RecordHelper<?> recordHelper = mInternalHealthConnectMappings.getRecordHelper(recordTypeId);
        RecordReadTableRequest readTableRequest =
                recordHelper.getReadTableRequest(
                        request,
                        callingPackageName,
                        enforceSelfRead,
                        startDateAccessMillis,
                        grantedExtraReadPermissions,
                        grantedGranularPermissions,
                        isInForeground,
                        mAppInfoHelper);

        if (request.getRecordIdFiltersParcel() != null) {
            return Pair.create(
                    readRecords(
                            transactionManager,
                            callingPackageName,
                            Set.of(recordTypeId),
                            singletonList(readTableRequest),
                            shouldRecordAccessLog),
                    PageTokenWrapper.EMPTY_PAGE_TOKEN);
        }

        PageTokenWrapper pageToken =
                PageTokenWrapper.from(request.getPageToken(), request.isAscending());
        int pageSize = request.getPageSize();

        Pair<List<RecordInternal<?>>, PageTokenWrapper> readResult;
        try (Cursor cursor = transactionManager.read(readTableRequest.getReadTableRequest())) {
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
                shouldRecordAccessLog);

        return readResult;
    }

    /**
     * Reads and returns a list of records for the given request, along with the next page token.
     *
     * <p>This method is used for internal use cases, and aims to remove any possible checks to
     * maximise the data read.
     */
    public Pair<List<RecordInternal<?>>, PageTokenWrapper> readRecordsUnrestricted(
            TransactionManager transactionManager,
            ReadRecordsRequestParcel request,
            @Nullable Map<Long, String> packageNamesByAppIds) {
        // Passing in empty package name is a hacky solution here.
        // This method uses the package name to read extra data based on
        // grantedExtraReadPermissions. Since the extra read permissions contains all permissions,
        // this package name doesn't get used for this call.
        String callingPackageName = "";
        // Include all permissions so that we read all the data.
        Set<String> grantedExtraReadPermissions =
                new HashSet<>(
                        mInternalHealthConnectMappings
                                .getRecordHelper(request.getRecordType())
                                .getExtraReadPermissions());

        Set<String> grantedGranularReadPermissions =
                new HashSet<>(
                        mInternalHealthConnectMappings
                                .getRecordHelper(request.getRecordType())
                                .getGranularReadPermissions());

        return readRecords(
                transactionManager,
                callingPackageName,
                request,
                grantedExtraReadPermissions,
                grantedGranularReadPermissions,
                /* startDateAccessMillis= */ DEFAULT_LONG,
                // Pass in caller as foreground so that all data is read.
                /* isInForeground= */ true,
                // Don't record access logs for internal reads.
                /* shouldRecordAccessLog= */ false,
                /* enforceSelfRead= */ false,
                packageNamesByAppIds);
    }

    /**
     * Reads and returns a list of records for the given record ids.
     *
     * @param transactionManager The TransactionManager to be used to perform this request.
     * @param callingPackageName The package name of the app making this request. This can be empty
     *     for internal calls.
     * @param recordTypeToUuids A map from record types to the list of UUIDs of that record type to
     *     be read.
     * @param grantedExtraReadPermissions List of permissions granted to this app to read associated
     *     data.
     * @param grantedGranularPermissions A set of granted granular permissions for the read
     *     operation.
     * @param startDateAccessMillis The earliest time this app is allowed to read from.
     * @param isInForeground If the calling app is in the foreground.
     * @param shouldRecordAccessLog If access logs should be recorded for this call.
     * @return A list of records for this request.
     */
    public List<RecordInternal<?>> readRecords(
            TransactionManager transactionManager,
            String callingPackageName,
            Map<Integer, List<UUID>> recordTypeToUuids,
            Set<String> grantedExtraReadPermissions,
            Set<String> grantedGranularPermissions,
            long startDateAccessMillis,
            boolean isInForeground,
            boolean shouldRecordAccessLog) {
        List<RecordReadTableRequest> readTableRequests = new ArrayList<>();
        recordTypeToUuids.forEach(
                (recordType, uuids) -> {
                    if (!uuids.isEmpty()) {
                        RecordHelper<?> recordHelper =
                                mInternalHealthConnectMappings.getRecordHelper(recordType);
                        readTableRequests.add(
                                recordHelper.getReadTableRequest(
                                        callingPackageName,
                                        uuids,
                                        startDateAccessMillis,
                                        grantedExtraReadPermissions,
                                        grantedGranularPermissions,
                                        isInForeground,
                                        mAppInfoHelper));
                    }
                });

        return readRecords(
                transactionManager,
                callingPackageName,
                recordTypeToUuids.keySet(),
                readTableRequests,
                shouldRecordAccessLog);
    }

    /**
     * Reads and returns a list of records for the given record ids.
     *
     * <p>This method is used for internal use cases, and aims to remove any possible checks to
     * maximise the data read.
     */
    public List<RecordInternal<?>> readRecordsUnrestricted(
            TransactionManager transactionManager, Map<Integer, List<UUID>> recordTypeToUuids) {
        // Passing in empty package name is a hacky solution here.
        // This method uses the package name to read extra data based on
        // grantedExtraReadPermissions. Since the extra read permissions contains all permissions,
        // this package name doesn't get used for this call.
        String callingPackageName = "";
        // Include all permissions so that we read all the data.
        Set<String> grantedExtraReadPermissions =
                recordTypeToUuids.keySet().stream()
                        .map(mInternalHealthConnectMappings::getRecordHelper)
                        .flatMap(recordHelper -> recordHelper.getExtraReadPermissions().stream())
                        .collect(Collectors.toSet());

        Set<String> grantedGranularReadPermissions =
                recordTypeToUuids.keySet().stream()
                        .map(mInternalHealthConnectMappings::getRecordHelper)
                        .flatMap(recordHelper -> recordHelper.getGranularReadPermissions().stream())
                        .collect(Collectors.toSet());

        return readRecords(
                transactionManager,
                callingPackageName,
                recordTypeToUuids,
                grantedExtraReadPermissions,
                grantedGranularReadPermissions,
                /* startDateAccessMillis= */ DEFAULT_LONG,
                // Pass in caller as foreground so that all data is read.
                /* isInForeground= */ true,
                // Don't record access logs for internal reads.
                /* shouldRecordAccessLog= */ false);
    }

    private List<RecordInternal<?>> readRecords(
            TransactionManager transactionManager,
            String callingPackageName,
            Set<Integer> recordTypeIds,
            List<RecordReadTableRequest> readTableRequests,
            boolean shouldRecordAccessLog) {
        List<RecordInternal<?>> recordInternals = new ArrayList<>();
        for (RecordReadTableRequest readTableRequest : readTableRequests) {
            RecordHelper<?> helper = readTableRequest.getRecordHelper();
            try (Cursor cursor = transactionManager.read(readTableRequest.getReadTableRequest())) {
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
                shouldRecordAccessLog);

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
        transactionManager.runWithoutTransaction(
                db -> {
                    mReadAccessLogsHelper.recordAccessLogForNonAggregationReads(
                            db,
                            callingPackageName,
                            /* readTimeStamp= */ Instant.now().toEpochMilli(),
                            recordInternals);
                });
        transactionManager.runWithoutTransaction(
                db -> {
                    mAccessLogsHelper.recordReadAccessLog(db, callingPackageName, recordTypeIds);
                });
    }

    /**
     * Do extra sql requests to populate optional extra data. Used to populate {@link
     * android.health.connect.internal.datatypes.ExerciseRouteInternal}.
     */
    private void populateInternalRecordsWithExtraData(
            TransactionManager transactionManager,
            List<RecordInternal<?>> records,
            RecordReadTableRequest request) {
        if (request.getReadTableRequest().getExtraReadRequests() == null) {
            return;
        }
        for (ReadTableRequest extraDataRequest :
                request.getReadTableRequest().getExtraReadRequests()) {
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
