/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.server.healthconnect.storage.utils;

import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__DATA_TYPE_UNKNOWN;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_UNKNOWN;

import static com.android.internal.util.Preconditions.checkArgument;
import static com.android.server.healthconnect.storage.utils.RecordTypeIdForUuid.RECORD_TYPE_ID_FOR_UUID_UNKNOWN;

import android.annotation.Nullable;
import android.health.connect.datatypes.RecordTypeIdentifier;

import com.android.server.healthconnect.fitness.recordhelpers.RecordHelper;

import java.util.Objects;

/** @hide */
public final class InternalDataTypeDescriptor {
    @RecordTypeIdentifier.RecordType private final int mRecordTypeIdentifier;
    private final RecordHelper<?> mRecordHelper;
    @RecordTypeIdForUuid.Type private final int mRecordTypeIdForUuid;
    private final int mLoggingEnum;
    private final boolean mIsDerived;

    private InternalDataTypeDescriptor(Builder builder) {
        checkArgument(builder.mRecordTypeIdentifier != RECORD_TYPE_UNKNOWN);
        checkArgument(builder.mRecordTypeIdForUuid != RECORD_TYPE_ID_FOR_UUID_UNKNOWN);
        checkArgument(
                builder.mLoggingEnum
                        != HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__DATA_TYPE_UNKNOWN);
        mRecordTypeIdentifier = builder.mRecordTypeIdentifier;
        mRecordHelper = Objects.requireNonNull(builder.mRecordHelper);
        mRecordTypeIdForUuid = builder.mRecordTypeIdForUuid;
        mLoggingEnum = builder.mLoggingEnum;
        mIsDerived = builder.mIsDerived;
    }

    @RecordTypeIdentifier.RecordType
    public int getRecordTypeIdentifier() {
        return mRecordTypeIdentifier;
    }

    public RecordHelper<?> getRecordHelper() {
        return mRecordHelper;
    }

    @RecordTypeIdForUuid.Type
    public int getRecordTypeIdForUuid() {
        return mRecordTypeIdForUuid;
    }

    public int getLoggingEnum() {
        return mLoggingEnum;
    }

    public boolean isDerived() {
        return mIsDerived;
    }

    interface RecordTypeIdentifierBuilderStep {
        RecordHelperBuilderStep setRecordTypeIdentifier(
                @RecordTypeIdentifier.RecordType int recordTypeIdentifier);
    }

    interface RecordHelperBuilderStep {
        RecordTypeIdForUuidBuilderStep setRecordHelper(RecordHelper<?> recordHelper);
    }

    interface RecordTypeIdForUuidBuilderStep {
        LoggingEnumBuilderStep setRecordTypeIdForUuid(
                @RecordTypeIdForUuid.Type int recordTypeIdForUuid);
    }

    interface LoggingEnumBuilderStep {
        BuildStep setLoggingEnum(int loggingEnum);
    }

    interface BuildStep {
        BuildStep setDerived();

        InternalDataTypeDescriptor build();
    }

    static RecordTypeIdentifierBuilderStep builder() {
        return new Builder();
    }

    private static class Builder
            implements RecordTypeIdentifierBuilderStep,
                    RecordHelperBuilderStep,
                    RecordTypeIdForUuidBuilderStep,
                    LoggingEnumBuilderStep,
                    BuildStep {
        @RecordTypeIdentifier.RecordType private int mRecordTypeIdentifier = RECORD_TYPE_UNKNOWN;
        @Nullable private RecordHelper<?> mRecordHelper;

        @RecordTypeIdForUuid.Type
        private int mRecordTypeIdForUuid = RECORD_TYPE_ID_FOR_UUID_UNKNOWN;

        private int mLoggingEnum = HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__DATA_TYPE_UNKNOWN;

        private boolean mIsDerived = false;

        private Builder() {}

        @Override
        public Builder setRecordTypeIdentifier(
                @RecordTypeIdentifier.RecordType int recordTypeIdentifier) {
            checkArgument(recordTypeIdentifier != RECORD_TYPE_UNKNOWN);
            mRecordTypeIdentifier = recordTypeIdentifier;
            return this;
        }

        @Override
        public Builder setRecordHelper(RecordHelper<?> recordHelper) {
            mRecordHelper = Objects.requireNonNull(recordHelper);
            return this;
        }

        @Override
        public Builder setRecordTypeIdForUuid(@RecordTypeIdForUuid.Type int recordTypeIdForUuid) {
            checkArgument(recordTypeIdForUuid != RECORD_TYPE_ID_FOR_UUID_UNKNOWN);
            mRecordTypeIdForUuid = recordTypeIdForUuid;
            return this;
        }

        @Override
        public Builder setLoggingEnum(int loggingEnum) {
            checkArgument(
                    loggingEnum != HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__DATA_TYPE_UNKNOWN);
            mLoggingEnum = loggingEnum;
            return this;
        }

        @Override
        public Builder setDerived() {
            mIsDerived = true;
            return this;
        }

        @Override
        public InternalDataTypeDescriptor build() {
            return new InternalDataTypeDescriptor(this);
        }
    }
}
