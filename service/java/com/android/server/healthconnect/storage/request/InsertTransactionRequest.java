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
import android.healthconnect.datatypes.RecordTypeIdentifier;
import android.healthconnect.internal.datatypes.RecordInternal;
import android.util.ArrayMap;

import com.android.server.healthconnect.storage.utils.StorageUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
public class InsertTransactionRequest {
    @NonNull
    private final Map<Integer, List<RecordInternal<?>>> mInsertRequestsMap = new ArrayMap<>();

    @NonNull private final List<String> mUUIDsInOrder = new ArrayList<>();
    @NonNull private final String mPackageName;

    public InsertTransactionRequest(
            @NonNull String packageName, @NonNull List<RecordInternal<?>> recordInternals) {
        mPackageName = packageName;
        for (RecordInternal<?> recordInternal : recordInternals) {
            // Always generate an uuid field for insert requests, we should not trust what is
            // already present.
            StorageUtils.addPackageNameTo(recordInternal, mPackageName);
            StorageUtils.addNameBasedUUIDTo(recordInternal);
            mUUIDsInOrder.add(recordInternal.getUuid());
            addRequest(recordInternal.getRecordType(), recordInternal);
        }
    }

    @NonNull
    public Map<Integer, List<RecordInternal<?>>> getInsertRequestsMap() {
        return mInsertRequestsMap;
    }

    @NonNull
    public String getPackageName() {
        return mPackageName;
    }

    @NonNull
    public List<String> getUUIdsInOrder() {
        return mUUIDsInOrder;
    }

    private void addRequest(
            @RecordTypeIdentifier.RecordType int dataTypeName,
            @NonNull RecordInternal<?> recordInternal) {
        mInsertRequestsMap.getOrDefault(dataTypeName, new ArrayList<>()).add(recordInternal);
    }
}
