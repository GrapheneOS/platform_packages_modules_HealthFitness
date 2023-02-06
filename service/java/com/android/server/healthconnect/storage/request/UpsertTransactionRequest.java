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

import static android.health.connect.Constants.UPSERT;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.health.connect.Constants;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.health.connect.internal.datatypes.RecordInternal;
import android.util.ArraySet;
import android.util.Slog;

import com.android.server.healthconnect.storage.datatypehelpers.AccessLogsHelper;
import com.android.server.healthconnect.storage.datatypehelpers.AppInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.ChangeLogsHelper;
import com.android.server.healthconnect.storage.datatypehelpers.DeviceInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.RecordHelper;
import com.android.server.healthconnect.storage.utils.RecordHelperProvider;
import com.android.server.healthconnect.storage.utils.StorageUtils;
import com.android.server.healthconnect.storage.utils.WhereClauses;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

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
    @NonNull private final List<UpsertTableRequest> mInsertRequests = new ArrayList<>();
    @NonNull private final List<String> mUUIDsInOrder = new ArrayList<>();
    @RecordTypeIdentifier.RecordType Set<Integer> mRecordTypes = new ArraySet<>();

    public UpsertTransactionRequest(
            @Nullable String packageName,
            @NonNull List<RecordInternal<?>> recordInternals,
            Context context,
            boolean isInsertRequest) {
        this(
                packageName,
                recordInternals,
                context,
                isInsertRequest,
                false /* skipPackageNameAndLogs */);
    }

    public UpsertTransactionRequest(
            @Nullable String packageName,
            @NonNull List<RecordInternal<?>> recordInternals,
            Context context,
            boolean isInsertRequest,
            boolean skipPackageNameAndLogs) {
        long currentTime = Instant.now().toEpochMilli();
        ChangeLogsHelper.ChangeLogs changeLogs = null;
        if (!skipPackageNameAndLogs) {
            changeLogs = new ChangeLogsHelper.ChangeLogs(UPSERT, packageName, currentTime);
        }

        for (RecordInternal<?> recordInternal : recordInternals) {
            if (!skipPackageNameAndLogs) {
                StorageUtils.addPackageNameTo(recordInternal, packageName);
            }
            AppInfoHelper.getInstance()
                    .populateAppInfoId(recordInternal, context, /* requireAllFields= */ true);
            DeviceInfoHelper.getInstance().populateDeviceInfoId(recordInternal);

            if (isInsertRequest) {
                // Always generate an uuid field for insert requests, we should not trust what is
                // already present.
                StorageUtils.addNameBasedUUIDTo(recordInternal);
                // Add uuids to change logs
                mUUIDsInOrder.add(recordInternal.getUuid());
                mRecordTypes.add(recordInternal.getRecordType());
            } else {
                // For update requests, generate uuid if the clientRecordID is present, else use the
                // uuid passed as input.
                StorageUtils.updateNameBasedUUIDIfRequired(recordInternal);
            }
            if (!skipPackageNameAndLogs) {
                changeLogs.addUUID(
                        recordInternal.getRecordType(),
                        recordInternal.getAppInfoId(),
                        recordInternal.getUuid());
            }
            recordInternal.setLastModifiedTime(currentTime);
            addRequest(recordInternal);
        }

        // Add commands to update the change log table with all the upserts
        if (!skipPackageNameAndLogs && !mRecordTypes.isEmpty()) {
            mInsertRequests.addAll(changeLogs.getUpsertTableRequests());
            AccessLogsHelper.getInstance()
                    .addAccessLog(packageName, new ArrayList<>(mRecordTypes), UPSERT);
        }
        if (Constants.DEBUG) {
            Slog.d(
                    TAG,
                    "Upserting transaction for "
                            + packageName
                            + " with size "
                            + recordInternals.size());
        }
    }

    @NonNull
    public List<UpsertTableRequest> getUpsertRequests() {
        return mInsertRequests;
    }

    @NonNull
    public List<String> getUUIdsInOrder() {
        return mUUIDsInOrder;
    }

    private WhereClauses generateWhereClausesForUpdate(@NonNull RecordInternal<?> recordInternal) {
        WhereClauses whereClauseForUpdateRequest = new WhereClauses();
        whereClauseForUpdateRequest.addWhereEqualsClause(
                RecordHelper.UUID_COLUMN_NAME, /* expected args value */ recordInternal.getUuid());
        whereClauseForUpdateRequest.addWhereEqualsClause(
                RecordHelper.APP_INFO_ID_COLUMN_NAME,
                /* expected args value */ String.valueOf(recordInternal.getAppInfoId()));
        return whereClauseForUpdateRequest;
    }

    private void addRequest(@NonNull RecordInternal<?> recordInternal) {
        RecordHelper<?> recordHelper =
                RecordHelperProvider.getInstance().getRecordHelper(recordInternal.getRecordType());
        Objects.requireNonNull(recordHelper);

        UpsertTableRequest request = recordHelper.getUpsertTableRequest(recordInternal);
        request.setWhereClauses(generateWhereClausesForUpdate(recordInternal));
        mInsertRequests.add(request);
    }
}
