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

import static com.android.server.healthconnect.storage.datatypehelpers.RecordHelper.UUID_COLUMN_NAME;

import android.annotation.NonNull;
import android.database.Cursor;
import android.healthconnect.RecordIdFilter;
import android.healthconnect.aidl.ReadRecordsRequestParcel;
import android.healthconnect.datatypes.RecordTypeIdentifier;
import android.healthconnect.internal.datatypes.RecordInternal;
import android.healthconnect.internal.datatypes.utils.RecordMapper;

import com.android.server.healthconnect.storage.datatypehelpers.RecordHelper;
import com.android.server.healthconnect.storage.utils.RecordHelperProvider;
import com.android.server.healthconnect.storage.utils.StorageUtils;
import com.android.server.healthconnect.storage.utils.WhereClauses;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Refines a request from what the user sent to a format that makes the most sense for the
 * TransactionManager.
 *
 * <p>Notes, This class refines the queries the records stored in the DB
 *
 * @hide
 */
public class ReadTransactionRequest {
    private final ReadTableRequest mReadTableRequest;
    @RecordTypeIdentifier.RecordType private int mRecordType;
    private RecordHelper<?> mRecordHelper;

    public ReadTransactionRequest(String packageName, ReadRecordsRequestParcel request) {
        List<RecordIdFilter> recordIdFilterList =
                request.getRecordIdFiltersParcel().getRecordIdFilters();
        Set<String> ids = new HashSet<String>();
        for (RecordIdFilter recordIdFilter : recordIdFilterList) {
            String uuid = StorageUtils.getUUIDFor(recordIdFilter, packageName);
            ids.add(uuid);
        }
        mRecordType =
                RecordMapper.getInstance().getRecordType(recordIdFilterList.get(0).getRecordType());
        mRecordHelper = RecordHelperProvider.getInstance().getRecordHelper(mRecordType);
        mReadTableRequest = mRecordHelper.getReadTableRequest();
        mReadTableRequest.setWhereClause(
                new WhereClauses().addWhereInClause(new ArrayList<String>(ids), UUID_COLUMN_NAME));
    }

    @NonNull
    public ReadTableRequest getReadRequest() {
        return mReadTableRequest;
    }

    /** Returns list of records from the cursor */
    public List<RecordInternal<?>> getInternalRecords(Cursor cursor) {
        return mRecordHelper.getInternalRecords(cursor);
    }
}
