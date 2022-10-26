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

import android.annotation.NonNull;
import android.healthconnect.aidl.DeleteUsingFiltersRequestParcel;
import android.healthconnect.datatypes.RecordTypeIdentifier;
import android.healthconnect.internal.datatypes.utils.RecordMapper;

import com.android.server.healthconnect.storage.datatypehelpers.ChangeLogsHelper;
import com.android.server.healthconnect.storage.datatypehelpers.RecordHelper;
import com.android.server.healthconnect.storage.utils.RecordHelperProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** @hide */
public final class DeleteTransactionRequest {
    private final List<DeleteTableRequest> mDeleteTableRequests;
    private final ChangeLogsHelper.ChangeLogs mChangeLogs;

    public DeleteTransactionRequest(String packageName, DeleteUsingFiltersRequestParcel request) {
        mDeleteTableRequests = new ArrayList<>(request.getRecordTypeFilters().size());
        mChangeLogs = new ChangeLogsHelper.ChangeLogs(ChangeLogsHelper.DELETE, packageName);
        List<Integer> recordTypeFilters = request.getRecordTypeFilters();
        if (recordTypeFilters == null || recordTypeFilters.isEmpty()) {
            recordTypeFilters =
                    new ArrayList<>(
                            RecordMapper.getInstance()
                                    .getRecordIdToExternalRecordClassMap()
                                    .keySet());
        }

        recordTypeFilters.forEach(
                (recordType) -> {
                    RecordHelper<?> recordHelper =
                            RecordHelperProvider.getInstance().getRecordHelper(recordType);
                    Objects.requireNonNull(recordHelper);

                    mDeleteTableRequests.add(
                            recordHelper.getDeleteTableRequest(
                                    request.getPackageNameFilters(),
                                    request.getStartTime(),
                                    request.getEndTime()));
                });
    }

    public List<DeleteTableRequest> getDeleteTableRequests() {
        return mDeleteTableRequests;
    }

    public void onUuidFetched(
            @RecordTypeIdentifier.RecordType int recordType, @NonNull String uuid) {
        mChangeLogs.addUUID(recordType, uuid);
    }

    @NonNull
    public List<UpsertTableRequest> getChangeLogUpsertRequests() {
        return mChangeLogs.getUpsertTableRequests();
    }
}
