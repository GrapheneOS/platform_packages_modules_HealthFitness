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

import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__DATA_TYPE_UNKNOWN;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_UNKNOWN;

import static com.android.server.healthconnect.fitness.mappings.InternalDataTypeDescriptors.getAllInternalDataTypeDescriptors;
import static com.android.server.healthconnect.fitness.mappings.RecordTypeIdForUuid.RECORD_TYPE_ID_FOR_UUID_UNKNOWN;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import android.health.connect.datatypes.AggregationType;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.health.connect.internal.datatypes.utils.HealthConnectMappings;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.healthfitness.flags.Flags;
import com.android.server.healthconnect.common.logging.HealthConnectServiceLogger;
import com.android.server.healthconnect.fitness.recordhelpers.RecordHelper;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;
import java.util.List;

@RunWith(AndroidJUnit4.class)
@EnableFlags({Flags.FLAG_HEALTH_CONNECT_MAPPINGS})
public class InternalHealthConnectMappingsTest {

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Test
    public void recordTypeIds_unique() {
        List<InternalDataTypeDescriptor> descriptors = getAllInternalDataTypeDescriptors();

        List<Integer> recordTypeIds =
                descriptors.stream()
                        .map(InternalDataTypeDescriptor::getRecordTypeIdentifier)
                        .toList();

        assertThat(recordTypeIds).containsNoDuplicates();
        assertThat(recordTypeIds).hasSize(descriptors.size());
        assertThat(recordTypeIds).doesNotContain(RECORD_TYPE_UNKNOWN);
    }

    @Test
    public void getRecordTypeIdForUuid() {
        List<InternalDataTypeDescriptor> descriptors = getAllInternalDataTypeDescriptors();
        InternalHealthConnectMappings mappings =
                new InternalHealthConnectMappings(new HealthConnectMappings());
        for (var descriptor : descriptors) {
            String className = descriptor.getRecordHelper().getClass().getSimpleName();
            int recordTypeId = descriptor.getRecordTypeIdentifier();

            assertWithMessage(className)
                    .that(mappings.getRecordTypeIdForUuid(recordTypeId))
                    .isGreaterThan(RECORD_TYPE_ID_FOR_UUID_UNKNOWN);
        }

        List<Integer> allRecordIdsForUuid =
                descriptors.stream()
                        .map(InternalDataTypeDescriptor::getRecordTypeIdForUuid)
                        .toList();
        assertThat(allRecordIdsForUuid).containsNoDuplicates();
        assertThat(allRecordIdsForUuid).hasSize(descriptors.size());
        assertThat(allRecordIdsForUuid).doesNotContain(RECORD_TYPE_ID_FOR_UUID_UNKNOWN);
    }

    @Test
    public void getRecordHelpers() {
        List<InternalDataTypeDescriptor> descriptors = getAllInternalDataTypeDescriptors();
        InternalHealthConnectMappings mappings =
                new InternalHealthConnectMappings(descriptors, new HealthConnectMappings());

        Collection<RecordHelper<?>> recordHelpers = mappings.getRecordHelpers();

        assertThat(recordHelpers).containsNoDuplicates();
        assertThat(recordHelpers).hasSize(descriptors.size());
        for (var descriptor : descriptors) {
            assertThat(recordHelpers).contains(descriptor.getRecordHelper());
        }
    }

    @Test
    public void getRecordHelper() {
        List<InternalDataTypeDescriptor> descriptors = getAllInternalDataTypeDescriptors();
        InternalHealthConnectMappings mappings =
                new InternalHealthConnectMappings(descriptors, new HealthConnectMappings());

        for (var descriptor : descriptors) {
            int recordTypeId = descriptor.getRecordTypeIdentifier();
            assertThat(mappings.getRecordHelper(recordTypeId))
                    .isSameInstanceAs(descriptor.getRecordHelper());
        }

        assertThat(
                        descriptors.stream()
                                .map(InternalDataTypeDescriptor::getRecordTypeIdentifier)
                                .map(mappings::getRecordHelper)
                                .map(Object::getClass)
                                .toList())
                .containsNoDuplicates();
    }

    @Test
    public void getLoggingEnumForRecordTypeId() {
        HealthConnectMappings externalMappings = new HealthConnectMappings();
        List<InternalDataTypeDescriptor> descriptors = getAllInternalDataTypeDescriptors();
        InternalHealthConnectMappings mappings =
                new InternalHealthConnectMappings(externalMappings);

        for (var descriptor : descriptors) {
            int loggingEnum =
                    mappings.getLoggingEnumForRecordTypeId(descriptor.getRecordTypeIdentifier());

            assertThat(loggingEnum).isEqualTo(descriptor.getLoggingEnum());
            assertThat(loggingEnum)
                    .isNotEqualTo(HEALTH_CONNECT_API_INVOKED__DATA_TYPE_ONE__DATA_TYPE_UNKNOWN);
        }

        assertThat(
                        descriptors.stream()
                                .map(InternalDataTypeDescriptor::getRecordTypeIdentifier)
                                .map(mappings::getLoggingEnumForRecordTypeId)
                                .toList())
                .containsNoDuplicates();
    }

    @DisableFlags(Flags.FLAG_ACTIVITY_INTENSITY)
    @Test
    public void getLoggingEnumForRecordTypeId_equalsToLegacy() {
        List<InternalDataTypeDescriptor> descriptors = getAllInternalDataTypeDescriptors();
        InternalHealthConnectMappings mappings =
                new InternalHealthConnectMappings(new HealthConnectMappings());

        for (var descriptor : descriptors) {
            assertThat(mappings.getLoggingEnumForRecordTypeId(descriptor.getRecordTypeIdentifier()))
                    .isEqualTo(
                            HealthConnectServiceLogger.Builder.getDataTypeEnumFromRecordType(
                                    descriptor.getRecordTypeIdentifier()));
        }
    }

    @Test
    public void supportsPriority() {
        InternalHealthConnectMappings mappings =
                new InternalHealthConnectMappings(new HealthConnectMappings());

        assertThat(
                        mappings.supportsPriority(
                                RecordTypeIdentifier.RECORD_TYPE_STEPS, AggregationType.SUM))
                .isTrue();
        assertThat(
                        mappings.supportsPriority(
                                RecordTypeIdentifier.RECORD_TYPE_STEPS, AggregationType.COUNT))
                .isFalse();
        assertThat(
                        mappings.supportsPriority(
                                RecordTypeIdentifier.RECORD_TYPE_EXERCISE_SESSION,
                                AggregationType.SUM))
                .isTrue();
        assertThat(
                        mappings.supportsPriority(
                                RecordTypeIdentifier.RECORD_TYPE_EXERCISE_SESSION,
                                AggregationType.COUNT))
                .isFalse();
        assertThat(
                        mappings.supportsPriority(
                                RecordTypeIdentifier.RECORD_TYPE_MINDFULNESS_SESSION,
                                AggregationType.SUM))
                .isTrue();
        assertThat(
                        mappings.supportsPriority(
                                RecordTypeIdentifier.RECORD_TYPE_MINDFULNESS_SESSION,
                                AggregationType.AVG))
                .isFalse();
        assertThat(
                        mappings.supportsPriority(
                                RecordTypeIdentifier.RECORD_TYPE_SLEEP_SESSION,
                                AggregationType.SUM))
                .isTrue();
        assertThat(
                        mappings.supportsPriority(
                                RecordTypeIdentifier.RECORD_TYPE_SLEEP_SESSION,
                                AggregationType.MAX))
                .isFalse();
        assertThat(
                        mappings.supportsPriority(
                                RecordTypeIdentifier.RECORD_TYPE_NUTRITION, AggregationType.SUM))
                .isFalse();
        assertThat(
                        mappings.supportsPriority(
                                RecordTypeIdentifier.RECORD_TYPE_BASAL_BODY_TEMPERATURE,
                                AggregationType.SUM))
                .isFalse();
    }

    @Test
    public void isDerived() {
        InternalHealthConnectMappings mappings =
                new InternalHealthConnectMappings(new HealthConnectMappings());

        for (var descriptor : getAllInternalDataTypeDescriptors()) {
            assertThat(mappings.isDerivedType(descriptor.getRecordTypeIdentifier()))
                    .isEqualTo(descriptor.isDerived());
        }
    }
}
