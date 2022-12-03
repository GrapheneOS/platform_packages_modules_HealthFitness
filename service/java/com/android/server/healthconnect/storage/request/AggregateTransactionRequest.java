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
import android.healthconnect.aidl.AggregateDataRequestParcel;
import android.healthconnect.datatypes.AggregationType;
import android.healthconnect.internal.datatypes.utils.AggregationTypeIdMapper;

import com.android.server.healthconnect.storage.utils.RecordHelperProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Refines aggregate request from what the client sent to a format that makes the most sense for the
 * TransactionManager.
 *
 * @hide
 */
public final class AggregateTransactionRequest {
    private final String mPackageName;
    private final List<AggregateTableRequest> mAggregateTableRequests;

    /**
     * TODO(b/249581069): Add support for aggregates that require information from multiple tables
     */
    public AggregateTransactionRequest(
            @NonNull String packageName, @NonNull AggregateDataRequestParcel request) {
        mPackageName = packageName;
        mAggregateTableRequests = new ArrayList<>(request.getAggregateIds().length);
        final AggregationTypeIdMapper aggregationTypeIdMapper =
                AggregationTypeIdMapper.getInstance();

        for (int id : request.getAggregateIds()) {
            AggregationType<?> aggregationType = aggregationTypeIdMapper.getAggregationTypeFor(id);
            List<Integer> recordTypeIds = aggregationType.getApplicableRecordTypeIds();
            if (recordTypeIds.size() == 1) {
                mAggregateTableRequests.add(
                        RecordHelperProvider.getInstance()
                                .getRecordHelper(recordTypeIds.get(0))
                                .getAggregateTableRequest(
                                        aggregationType,
                                        request.getPackageFilters(),
                                        request.getStartTime(),
                                        request.getEndTime()));
            } else {
                throw new UnsupportedOperationException();
            }
        }
    }

    @NonNull
    public String getPackageName() {
        return mPackageName;
    }

    @NonNull
    public List<AggregateTableRequest> getAggregateTableRequests() {
        return mAggregateTableRequests;
    }
}
