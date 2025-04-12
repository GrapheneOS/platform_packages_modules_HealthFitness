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

package com.android.server.healthconnect.fitness.mappings;

import static android.health.connect.HealthDataCategory.ACTIVITY;
import static android.health.connect.HealthDataCategory.SLEEP;
import static android.health.connect.HealthDataCategory.WELLNESS;
import static android.health.connect.datatypes.AggregationType.SUM;

import static java.util.Objects.requireNonNull;

import android.annotation.Nullable;
import android.health.connect.datatypes.AggregationType;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.health.connect.internal.datatypes.utils.HealthConnectMappings;
import android.util.ArrayMap;

import com.android.healthfitness.flags.Flags;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.healthconnect.fitness.recordhelpers.RecordHelper;
import com.android.server.healthconnect.logging.HealthConnectServiceLogger;
import com.android.server.healthconnect.storage.utils.StorageUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/** @hide */
public class InternalHealthConnectMappings {

    private final HealthConnectMappings mExternalMappings;
    private final Map<Integer, InternalDataTypeDescriptor> mRecordTypeIdToDescriptor;
    private final List<RecordHelper<?>> mAllRecordHelpers;

    @Nullable private static volatile InternalHealthConnectMappings sInternalHealthConnectMappings;

    /** Exists for compatibility with classes which don't support injections yet. */
    // TODO(b/353283052): inject where possible instead of using the singleton.
    public static InternalHealthConnectMappings getInstance() {
        if (sInternalHealthConnectMappings == null) {
            sInternalHealthConnectMappings =
                    new InternalHealthConnectMappings(HealthConnectMappings.getInstance());
        }
        return sInternalHealthConnectMappings;
    }

    /**
     * Use {@link #getInstance()} to avoid creating multiple instances until it gets migrated off.
     */
    @VisibleForTesting
    public InternalHealthConnectMappings(HealthConnectMappings healthConnectMappings) {
        this(
                InternalDataTypeDescriptors.getAllInternalDataTypeDescriptors(),
                healthConnectMappings);
    }

    @VisibleForTesting
    InternalHealthConnectMappings(
            List<InternalDataTypeDescriptor> descriptors,
            HealthConnectMappings healthConnectMappings) {
        mExternalMappings = healthConnectMappings;
        mRecordTypeIdToDescriptor = new ArrayMap<>(descriptors.size());
        mAllRecordHelpers = new ArrayList<>(descriptors.size());

        if (!Flags.healthConnectMappings()) {
            return;
        }

        for (var descriptor : descriptors) {
            mRecordTypeIdToDescriptor.put(descriptor.getRecordTypeIdentifier(), descriptor);
            mAllRecordHelpers.add(descriptor.getRecordHelper());
        }
    }

    /** Maps the internal record type to a special record type for UUIDs. */
    @RecordTypeIdForUuid.Type
    public int getRecordTypeIdForUuid(@RecordTypeIdentifier.RecordType int recordTypeId) {
        if (!Flags.healthConnectMappings()) {
            return RecordTypeForUuidMappings.getRecordTypeIdForUuid(recordTypeId);
        }

        return requireNonNull(
                        mRecordTypeIdToDescriptor.get(recordTypeId),
                        "No mapping for " + recordTypeId)
                .getRecordTypeIdForUuid();
    }

    /** Returns a collection of all supported record helpers. */
    public Collection<RecordHelper<?>> getRecordHelpers() {
        if (!Flags.healthConnectMappings()) {
            return RecordHelperProvider.getRecordHelpers();
        }
        return mAllRecordHelpers;
    }

    /** Returns a {@link RecordHelper} for given record type id. */
    public RecordHelper<?> getRecordHelper(@RecordTypeIdentifier.RecordType int recordTypeId) {
        if (!Flags.healthConnectMappings()) {
            return RecordHelperProvider.getRecordHelper(recordTypeId);
        }

        return getDescriptorFor(recordTypeId).getRecordHelper();
    }

    /** Returns a logging enum for given record type id. */
    public int getLoggingEnumForRecordTypeId(@RecordTypeIdentifier.RecordType int recordTypeId) {
        if (!Flags.healthConnectMappings()) {
            return HealthConnectServiceLogger.Builder.getDataTypeEnumFromRecordType(recordTypeId);
        }
        return getDescriptorFor(recordTypeId).getLoggingEnum();
    }

    public HealthConnectMappings getExternalMappings() {
        return mExternalMappings;
    }

    /** Returns true if given record type/aggregation operation type pair support priority. */
    public boolean supportsPriority(
            @RecordTypeIdentifier.RecordType int recordType,
            @AggregationType.AggregateOperationType int operationType) {
        if (!Flags.healthConnectMappings()) {
            return StorageUtils.supportsPriority(recordType, operationType);
        }

        if (operationType != SUM) {
            return false;
        }

        return switch (mExternalMappings.getRecordCategoryForRecordType(recordType)) {
            case ACTIVITY, SLEEP, WELLNESS -> true;
            default -> false;
        };
    }

    /** Returns true if given record type is derived. */
    public boolean isDerivedType(@RecordTypeIdentifier.RecordType int recordType) {
        if (!Flags.healthConnectMappings()) {
            return StorageUtils.isDerivedType(recordType);
        }

        return getDescriptorFor(recordType).isDerived();
    }

    private InternalDataTypeDescriptor getDescriptorFor(
            @RecordTypeIdentifier.RecordType int recordTypeId) {
        return requireNonNull(
                mRecordTypeIdToDescriptor.get(recordTypeId), "No mapping for " + recordTypeId);
    }
}
