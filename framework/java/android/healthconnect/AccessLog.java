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

package android.healthconnect;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.healthconnect.datatypes.Record;
import android.healthconnect.datatypes.RecordTypeIdentifier;
import android.healthconnect.internal.datatypes.utils.RecordMapper;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A class to represent access log which is logged whenever a package requests a read on a record
 * type
 *
 * @hide
 */
@SystemApi
public class AccessLog {
    private final List<Class<? extends Record>> mRecordTypesList = new ArrayList<>();
    private final String mPackageName;
    private final Instant mAccessTime;
    @OperationType.OperationTypes private final int mOperationType;

    /**
     * Creates an access logs object that can be used to get access log request for {@code
     * packageName}
     *
     * @param packageName name of the package that requested an access
     * @param recordTypes List of Record class type the was accessed
     * @param accessTimeInMillis time when the access was requested
     * @param operationType Type of access
     * @hide
     */
    public AccessLog(
            @NonNull String packageName,
            @NonNull @RecordTypeIdentifier.RecordType List<Integer> recordTypes,
            long accessTimeInMillis,
            @OperationType.OperationTypes int operationType) {
        Objects.requireNonNull(packageName);
        Objects.requireNonNull(recordTypes);

        mPackageName = packageName;
        RecordMapper recordMapper = RecordMapper.getInstance();
        for (@RecordTypeIdentifier.RecordType int recordType : recordTypes) {
            mRecordTypesList.add(
                    recordMapper.getRecordIdToExternalRecordClassMap().get(recordType));
        }
        mAccessTime = Instant.ofEpochMilli(accessTimeInMillis);
        mOperationType = operationType;
    }

    /** Returns List of Record types that was accessed by the app */
    @NonNull
    public List<Class<? extends Record>> getRecordTypes() {
        return mRecordTypesList;
    }

    /** Returns package name of app that accessed the records */
    @NonNull
    public String getPackageName() {
        return mPackageName;
    }

    /** Returns the instant at which the app accessed the record */
    @NonNull
    public Instant getAccessTime() {
        return mAccessTime;
    }

    /** Returns the type of operation performed by the app */
    @OperationType.OperationTypes
    public int getOperationType() {
        return mOperationType;
    }

    /** Identifier for Operation type. */
    public static final class OperationType {

        /** Identifier for read operation done on user health data. */
        public static final int OPERATION_TYPE_READ = Constants.READ;

        /** Identifier for update or insert operation done on user health data. */
        public static final int OPERATION_TYPE_UPSERT = Constants.UPSERT;

        /** Identifier for delete operation done on user health data. */
        public static final int OPERATION_TYPE_DELETE = Constants.DELETE;

        /** @hide */
        @IntDef({OPERATION_TYPE_UPSERT, OPERATION_TYPE_DELETE, OPERATION_TYPE_READ})
        @Retention(RetentionPolicy.SOURCE)
        public @interface OperationTypes {}

        private OperationType() {}
    }
}
