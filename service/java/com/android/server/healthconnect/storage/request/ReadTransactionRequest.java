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

import static android.healthconnect.Constants.DEFAULT_INT;

import static com.android.server.healthconnect.storage.datatypehelpers.RecordHelper.APP_INFO_ID_COLUMN_NAME;
import static com.android.server.healthconnect.storage.datatypehelpers.RecordHelper.UUID_COLUMN_NAME;

import android.annotation.NonNull;
import android.database.Cursor;
import android.healthconnect.aidl.ReadRecordsRequestParcel;
import android.healthconnect.internal.datatypes.RecordInternal;

import com.android.server.healthconnect.storage.datatypehelpers.AppInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.RecordHelper;
import com.android.server.healthconnect.storage.utils.RecordHelperProvider;
import com.android.server.healthconnect.storage.utils.StorageUtils;
import com.android.server.healthconnect.storage.utils.WhereClauses;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Refines a request from what the user sent to a format that makes the most sense for the
 * TransactionManager.
 *
 * <p>Notes, This class refines the queries the records stored in the DB
 *
 * @hide
 */
public class ReadTransactionRequest {
    public static final String TYPE_NOT_PRESENT_PACKAGE_NAME = "package_name";
    private final ReadTableRequest mReadTableRequest;
    private final RecordHelper<?> mRecordHelper;

    public ReadTransactionRequest(String packageName, ReadRecordsRequestParcel request) {
        if (request.getRecordIdFiltersParcel() == null) {
            // Since no records ids we present in the request, this means that we just want to use
            // filters.
            mRecordHelper =
                    RecordHelperProvider.getInstance().getRecordHelper(request.getRecordType());
            mReadTableRequest =
                    mRecordHelper.getReadTableRequest().setWhereClause(getWhereClause(request));
            return;
        }

        // Since for now we don't support mixing IDs and filters, we will just look at IDs from now
        // on
        List<String> ids =
                request.getRecordIdFiltersParcel().getRecordIdFilters().stream()
                        .map(
                                (recordIdFilter) ->
                                        StorageUtils.getUUIDFor(recordIdFilter, packageName))
                        .collect(Collectors.toList());

        mRecordHelper = RecordHelperProvider.getInstance().getRecordHelper(request.getRecordType());
        mReadTableRequest = mRecordHelper.getReadTableRequest();
        mReadTableRequest.setWhereClause(
                new WhereClauses().addWhereInClause(ids, UUID_COLUMN_NAME));
    }

    @NonNull
    public ReadTableRequest getReadRequest() {
        return mReadTableRequest;
    }

    /** Returns list of records from the cursor */
    public List<RecordInternal<?>> getInternalRecords(Cursor cursor) {
        return mRecordHelper.getInternalRecords(cursor);
    }

    private WhereClauses getWhereClause(ReadRecordsRequestParcel request) {
        List<Long> appIds =
                AppInfoHelper.getInstance().getAppInfoIds(request.getPackageFilters()).stream()
                        .distinct()
                        .collect(Collectors.toList());
        if (appIds.size() == 1 && appIds.get(0) == DEFAULT_INT) {
            throw new TypeNotPresentException(TYPE_NOT_PRESENT_PACKAGE_NAME, new Throwable());
        }

        WhereClauses clauses =
                new WhereClauses()
                        .addWhereInLongsClause(
                                APP_INFO_ID_COLUMN_NAME,
                                AppInfoHelper.getInstance()
                                        .getAppInfoIds(request.getPackageFilters()));

        return clauses.addWhereBetweenTimeClause(
                mRecordHelper.getStartTimeColumnName(),
                request.getStartTime(),
                request.getEndTime());
    }
}
