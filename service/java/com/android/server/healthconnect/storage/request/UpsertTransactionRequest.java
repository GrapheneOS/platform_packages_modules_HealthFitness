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

package com.android.server.healthconnect.storage.request;

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

import com.android.internal.annotations.VisibleForTesting;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.datatypehelpers.AccessLogsHelper;
import com.android.server.healthconnect.storage.datatypehelpers.AppInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.ChangeLogsHelper;
import com.android.server.healthconnect.storage.datatypehelpers.DeviceInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.RecordHelper;
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
 * Refines a request from what the user sent to a format that makes the most sense for the
 * TransactionManager.
 *
 * <p>Notes, This class refines the request as well by replacing the untrusted fields with the
 * platform's trusted sources. As a part of that this class populates uuid and package name for all
 * the entries in {@param records}.
 *
 * @hide
 */
public class UpsertTransactionRequest {
    private static final String TAG = "HealthConnectUTR";

    @Nullable private final String mPackageName;

    private final TransactionManager mTransactionManager;
    private final InternalHealthConnectMappings mInternalHealthConnectMappings;
    private final AppInfoHelper mAppInfoHelper;
    @Nullable private final AccessLogsHelper mAccessLogsHelper;

    private final List<UpsertTableRequest> mUpsertRequests = new ArrayList<>();
    @RecordTypeIdentifier.RecordType Set<Integer> mRecordTypes = new ArraySet<>();
    private final boolean mIsInsertRequest;
    private final boolean mShouldGenerateAccessLogs;
    private final boolean mShouldPreferNewRecord;

    /** Create an upsert request for insert API calls. */
    public static UpsertTransactionRequest createForInsert(
            String packageName,
            List<? extends RecordInternal<?>> recordInternals,
            TransactionManager transactionManager,
            InternalHealthConnectMappings internalHealthConnectMappings,
            DeviceInfoHelper deviceInfoHelper,
            AppInfoHelper appInfoHelper,
            AccessLogsHelper accessLogsHelper,
            ArrayMap<String, Boolean> extraPermsStateMap) {
        for (RecordInternal<?> recordInternal : recordInternals) {
            // Override each record package to the given package i.e. the API caller package.
            StorageUtils.addPackageNameTo(recordInternal, packageName);
            // For insert, we should generate a fresh UUID. Don't let the client choose it.
            addNameBasedUUIDTo(recordInternal);
        }
        return new UpsertTransactionRequest(
                packageName,
                recordInternals,
                transactionManager,
                internalHealthConnectMappings,
                deviceInfoHelper,
                appInfoHelper,
                accessLogsHelper,
                /* isInsertRequest= */ true,
                /* shouldGenerateAccessLogs= */ true,
                /* shouldPreferNewRecord= */ true,
                /* updateLastModifiedTime= */ true,
                extraPermsStateMap);
    }

    /** Create an upsert request for update API calls. */
    public static UpsertTransactionRequest createForUpdate(
            String packageName,
            List<? extends RecordInternal<?>> recordInternals,
            TransactionManager transactionManager,
            InternalHealthConnectMappings internalHealthConnectMappings,
            DeviceInfoHelper deviceInfoHelper,
            AppInfoHelper appInfoHelper,
            AccessLogsHelper accessLogsHelper,
            ArrayMap<String, Boolean> extraPermsStateMap) {
        for (RecordInternal<?> recordInternal : recordInternals) {
            // Override each record package to the given package i.e. the API caller package.
            StorageUtils.addPackageNameTo(recordInternal, packageName);
            // For update requests, generate uuid if the clientRecordID is present, else use the
            // uuid passed as input.
            StorageUtils.updateNameBasedUUIDIfRequired(recordInternal);
        }
        return new UpsertTransactionRequest(
                packageName,
                recordInternals,
                transactionManager,
                internalHealthConnectMappings,
                deviceInfoHelper,
                appInfoHelper,
                accessLogsHelper,
                /* isInsertRequest= */ false,
                /* shouldGenerateAccessLogs= */ true,
                /* shouldPreferNewRecord= */ true,
                /* updateLastModifiedTime= */ true,
                extraPermsStateMap);
    }

    /** Create an upsert request for restore operation. */
    public static UpsertTransactionRequest createForRestore(
            List<? extends RecordInternal<?>> recordInternals,
            TransactionManager transactionManager,
            InternalHealthConnectMappings internalHealthConnectMappings,
            DeviceInfoHelper deviceInfoHelper,
            AppInfoHelper appInfoHelper) {
        // Ensure each record has a record id set.
        for (RecordInternal<?> recordInternal : recordInternals) {
            Objects.requireNonNull(recordInternal.getUuid());
        }
        return new UpsertTransactionRequest(
                null,
                recordInternals,
                transactionManager,
                internalHealthConnectMappings,
                deviceInfoHelper,
                appInfoHelper,
                /* accessLogsHelper= */ null,
                /* isInsertRequest= */ true,
                /* shouldGenerateAccessLogs= */ false,
                /* shouldPreferNewRecord= */ false,
                /* updateLastModifiedTime= */ false,
                /* extraPermsStateMap= */ null);
    }

    private UpsertTransactionRequest(
            @Nullable String packageName,
            List<? extends RecordInternal<?>> recordInternals,
            TransactionManager transactionManager,
            InternalHealthConnectMappings internalHealthConnectMappings,
            DeviceInfoHelper deviceInfoHelper,
            AppInfoHelper appInfoHelper,
            @Nullable AccessLogsHelper accessLogsHelper,
            boolean isInsertRequest,
            boolean shouldGenerateAccessLogs,
            boolean shouldPreferNewRecord,
            boolean updateLastModifiedTime,
            @Nullable ArrayMap<String, Boolean> extraPermsStateMap) {
        if (shouldGenerateAccessLogs) {
            Objects.requireNonNull(packageName);
            Objects.requireNonNull(accessLogsHelper);
        }
        mPackageName = packageName;
        mInternalHealthConnectMappings = internalHealthConnectMappings;
        mTransactionManager = transactionManager;
        mAppInfoHelper = appInfoHelper;
        mAccessLogsHelper = accessLogsHelper;
        mShouldGenerateAccessLogs = shouldGenerateAccessLogs;
        mIsInsertRequest = isInsertRequest;
        mShouldPreferNewRecord = shouldPreferNewRecord;

        for (RecordInternal<?> recordInternal : recordInternals) {
            appInfoHelper.populateAppInfoId(recordInternal, /* requireAllFields= */ true);
            deviceInfoHelper.populateDeviceInfoId(recordInternal);
            mRecordTypes.add(recordInternal.getRecordType());
            if (updateLastModifiedTime) {
                recordInternal.setLastModifiedTime(Instant.now().toEpochMilli());
            }
            addRequest(recordInternal, isInsertRequest, extraPermsStateMap);
        }

        if (!mRecordTypes.isEmpty() && Constants.DEBUG) {
            Slog.d(
                    TAG,
                    "Upsert transaction for "
                            + packageName
                            + " with size "
                            + recordInternals.size());
        }
    }

    /** Execute the upsert request */
    public List<String> execute() {
        if (Constants.DEBUG) {
            Slog.d(TAG, "Upserting " + mUpsertRequests.size() + " requests.");
        }

        long currentTime = Instant.now().toEpochMilli();
        ChangeLogsHelper.ChangeLogs upsertionChangelogs =
                new ChangeLogsHelper.ChangeLogs(OPERATION_TYPE_UPSERT, currentTime);
        ChangeLogsHelper.ChangeLogs otherModifiedRecordsChangelogs =
                new ChangeLogsHelper.ChangeLogs(OPERATION_TYPE_UPSERT, currentTime);

        return mTransactionManager.runAsTransaction(
                db -> {
                    for (UpsertTableRequest upsertRequest : mUpsertRequests) {
                        upsertionChangelogs.addUUID(
                                upsertRequest.getRecordInternal().getRecordType(),
                                upsertRequest.getRecordInternal().getAppInfoId(),
                                upsertRequest.getRecordInternal().getUuid());
                        addChangelogsForOtherModifiedRecords(
                                mAppInfoHelper.getAppInfoId(
                                        upsertRequest.getRecordInternal().getPackageName()),
                                upsertRequest,
                                otherModifiedRecordsChangelogs);
                        if (mIsInsertRequest) {
                            if (mShouldPreferNewRecord) {
                                mTransactionManager.insertOrReplaceOnConflict(db, upsertRequest);
                            } else {
                                mTransactionManager.insertOrIgnoreOnConflict(db, upsertRequest);
                            }
                        } else {
                            mTransactionManager.update(upsertRequest);
                        }
                    }
                    for (UpsertTableRequest upsertRequestsForChangeLog :
                            upsertionChangelogs.getUpsertTableRequests()) {
                        mTransactionManager.insert(db, upsertRequestsForChangeLog);
                    }
                    for (UpsertTableRequest modificationChangelog :
                            otherModifiedRecordsChangelogs.getUpsertTableRequests()) {
                        mTransactionManager.insert(db, modificationChangelog);
                    }

                    if (mShouldGenerateAccessLogs) {
                        Objects.requireNonNull(mAccessLogsHelper)
                                .recordUpsertAccessLog(
                                        db, Objects.requireNonNull(mPackageName), mRecordTypes);
                    }
                    return getUUIdsInOrder();
                });
    }

    private List<String> getUUIdsInOrder() {
        return mUpsertRequests.stream()
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

    private void addRequest(
            RecordInternal<?> recordInternal,
            boolean isInsertRequest,
            @Nullable ArrayMap<String, Boolean> extraPermsStateMap) {
        RecordHelper<?> recordHelper =
                InternalHealthConnectMappings.getInstance()
                        .getRecordHelper(recordInternal.getRecordType());
        Objects.requireNonNull(recordHelper);

        UpsertTableRequest request =
                recordHelper.getUpsertTableRequest(recordInternal, extraPermsStateMap);
        request.setRecordType(recordHelper.getRecordIdentifier());
        if (!isInsertRequest) {
            request.setUpdateWhereClauses(generateWhereClausesForUpdate(recordInternal));
        }
        request.setRecordInternal(recordInternal);
        mUpsertRequests.add(request);
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

    // TODO(399112898): Remove this method.
    public List<UpsertTableRequest> getUpsertRequests() {
        return mUpsertRequests;
    }

    // Package name can be null if we don't need to generate access log, like when we are restoring
    // data.
    @VisibleForTesting
    public @Nullable String getPackageName() {
        return mPackageName;
    }

    @VisibleForTesting
    public Set<Integer> getRecordTypeIds() {
        return mRecordTypes;
    }
}
