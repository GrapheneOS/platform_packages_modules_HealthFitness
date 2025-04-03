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

import android.health.connect.datatypes.RecordTypeIdentifier;

import com.android.server.healthconnect.storage.request.DeleteTableRequest;

import java.util.Objects;

/**
 * A combination of a standard {@link DeleteTableRequest} and extra information needed when deleting
 * from record tables.
 *
 * @hide
 */
public class RecordDeleteTableRequest {

    private final DeleteTableRequest mDeleteTableRequest;
    @RecordTypeIdentifier.RecordType private final int mRecordType;

    public RecordDeleteTableRequest(
            DeleteTableRequest deleteTableRequest,
            @RecordTypeIdentifier.RecordType int recordType) {
        mDeleteTableRequest = deleteTableRequest;
        mRecordType = recordType;
    }

    public DeleteTableRequest getDeleteTableRequest() {
        return mDeleteTableRequest;
    }

    public int getRecordType() {
        return mRecordType;
    }

    public String getTableName() {
        return mDeleteTableRequest.getTableName();
    }

    public String getIdColumnName() {
        return Objects.requireNonNull(mDeleteTableRequest.getIdColumnName());
    }

    public String getPackageColumnName() {
        return Objects.requireNonNull(mDeleteTableRequest.getPackageColumnName());
    }

    public String getReadCommand() {
        return "SELECT "
                + getIdColumnName()
                + ", "
                + getPackageColumnName()
                + " FROM "
                + getTableName()
                + mDeleteTableRequest.getWhereCommand();
    }
}
