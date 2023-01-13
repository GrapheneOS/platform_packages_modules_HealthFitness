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

import static android.healthconnect.Constants.UPSERT;

import android.annotation.NonNull;
import android.content.Context;
import android.healthconnect.datatypes.RecordTypeIdentifier;
import android.healthconnect.internal.datatypes.RecordInternal;
import android.util.ArraySet;

import com.android.server.healthconnect.storage.datatypehelpers.AccessLogsHelper;
import com.android.server.healthconnect.storage.datatypehelpers.AppInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.ChangeLogsHelper;
import com.android.server.healthconnect.storage.datatypehelpers.DeviceInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.RecordHelper;
import com.android.server.healthconnect.storage.utils.RecordHelperProvider;
import com.android.server.healthconnect.storage.utils.StorageUtils;
import com.android.server.healthconnect.storage.utils.WhereClauses;

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
    @NonNull private final List<UpsertTableRequest> mInsertRequests = new ArrayList<>();
    @NonNull private final List<String> mUUIDsInOrder = new ArrayList<>();
    @NonNull private final String mPackageName;
    @RecordTypeIdentifier.RecordType Set<Integer> mRecordTypes = new ArraySet<>();

    public UpsertTransactionRequest(
            @NonNull String packageName,
            @NonNull List<RecordInternal<?>> recordInternals,
            Context context,
            boolean isInsertRequest) {
        mPackageName = packageName;
        ChangeLogsHelper.ChangeLogs changeLogs =
                new ChangeLogsHelper.ChangeLogs(UPSERT, mPackageName);

        for (RecordInternal<?> recordInternal : recordInternals) {
            StorageUtils.addPackageNameTo(recordInternal, mPackageName);
            AppInfoHelper.getInstance()
                    .populateAppInfoId(recordInternal, context, /*requireAllFields=*/ true);
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
            changeLogs.addUUID(recordInternal.getRecordType(), recordInternal.getUuid());
            addRequest(recordInternal);
        }

        // Add commands to update the change log table with all the upserts
        mInsertRequests.addAll(changeLogs.getUpsertTableRequests());
        if (!mRecordTypes.isEmpty()) {
            AccessLogsHelper.getInstance()
                    .addAccessLog(
                            packageName,
                            mRecordTypes.stream().collect(Collectors.toList()),
                            UPSERT);
        }
    }

    @NonNull
    public List<UpsertTableRequest> getUpsertRequests() {
        return mInsertRequests;
    }

    @NonNull
    public String getPackageName() {
        return mPackageName;
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
