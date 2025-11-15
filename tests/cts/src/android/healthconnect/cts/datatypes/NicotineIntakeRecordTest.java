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
package android.healthconnect.cts.datatypes;

import static android.health.connect.datatypes.Device.DEVICE_TYPE_FITNESS_BAND;
import static android.health.connect.datatypes.Metadata.RECORDING_METHOD_MANUAL_ENTRY;
import static android.health.connect.datatypes.NicotineIntakeRecord.NICOTINE_INTAKE_TYPE_CIGARETTE;
import static android.health.connect.datatypes.NicotineIntakeRecord.NICOTINE_INTAKE_TYPE_VAPE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_NICOTINE_INTAKE;

import static com.android.healthfitness.flags.Flags.FLAG_HEALTH_CONNECT_MAPPINGS;
import static com.android.healthfitness.flags.Flags.FLAG_SMOKING;
import static com.android.healthfitness.flags.Flags.FLAG_SMOKING_DB;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.Device;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.NicotineIntakeRecord;
import android.health.connect.datatypes.units.Mass;
import android.healthconnect.testing.shared.AssumptionCheckerRule;
import android.healthconnect.testing.shared.DeviceSupportUtils;
import android.platform.test.annotations.AppModeFull;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.time.ZoneOffset;

@AppModeFull(reason = "HealthConnectManager is not accessible to instant apps")
@RunWith(AndroidJUnit4.class)
@RequiresFlagsEnabled({FLAG_SMOKING, FLAG_SMOKING_DB, FLAG_HEALTH_CONNECT_MAPPINGS})
public class NicotineIntakeRecordTest {

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Rule
    public AssumptionCheckerRule mSupportedHardwareRule =
            new AssumptionCheckerRule(
                    DeviceSupportUtils::isHealthConnectFullySupported,
                    "Tests should run on supported hardware only.");

    @Test
    public void nicotineIntakeRecordBuilder_allFieldsSet() {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minusSeconds(60);
        ZoneOffset startZoneOffset = ZoneOffset.ofHours(2);
        ZoneOffset endZoneOffset = ZoneOffset.ofHours(-3);
        Mass nicotineIntake = Mass.fromGrams(0.01);
        Metadata metadata =
                new Metadata.Builder()
                        .setClientRecordId("clientRecordId")
                        .setClientRecordVersion(123)
                        .setDataOrigin(
                                new DataOrigin.Builder().setPackageName("package.name").build())
                        .setId("id-foo-bar")
                        .setRecordingMethod(RECORDING_METHOD_MANUAL_ENTRY)
                        .setDevice(
                                new Device.Builder()
                                        .setType(DEVICE_TYPE_FITNESS_BAND)
                                        .setManufacturer("manufacturer")
                                        .setModel("model")
                                        .build())
                        .setLastModifiedTime(endTime.minusSeconds(15))
                        .build();

        NicotineIntakeRecord record =
                new NicotineIntakeRecord.Builder(
                                metadata,
                                startTime,
                                endTime,
                                /* quantity= */ 20,
                                NICOTINE_INTAKE_TYPE_VAPE)
                        .setStartZoneOffset(startZoneOffset)
                        .setEndZoneOffset(endZoneOffset)
                        .setNicotineIntake(nicotineIntake)
                        .build();

        assertThat(record.getRecordType()).isEqualTo(RECORD_TYPE_NICOTINE_INTAKE);
        assertThat(record.getMetadata()).isEqualTo(metadata);
        assertThat(record.getStartTime()).isEqualTo(startTime);
        assertThat(record.getEndTime()).isEqualTo(endTime);
        assertThat(record.getStartZoneOffset()).isEqualTo(startZoneOffset);
        assertThat(record.getEndZoneOffset()).isEqualTo(endZoneOffset);
        assertThat(record.getNicotineIntake()).isEqualTo(nicotineIntake);
        assertThat(record.getQuantity()).isEqualTo(20);
        assertThat(record.getNicotineIntakeType()).isEqualTo(NICOTINE_INTAKE_TYPE_VAPE);
    }

    @Test
    public void nicotineIntakeRecordBuilder_optionalFieldsUnset() {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minusSeconds(60);
        Metadata metadata = new Metadata.Builder().build();

        NicotineIntakeRecord record =
                new NicotineIntakeRecord.Builder(
                                metadata,
                                startTime,
                                endTime,
                                /* quantity= */ 20,
                                NICOTINE_INTAKE_TYPE_VAPE)
                        .build();

        assertThat(record.getRecordType()).isEqualTo(RECORD_TYPE_NICOTINE_INTAKE);
        assertThat(record.getMetadata()).isEqualTo(metadata);
        assertThat(record.getStartTime()).isEqualTo(startTime);
        assertThat(record.getEndTime()).isEqualTo(endTime);
        assertThat(record.getStartZoneOffset()).isEqualTo(getDefaultZoneOffset(startTime));
        assertThat(record.getEndZoneOffset()).isEqualTo(getDefaultZoneOffset(endTime));
        assertThat(record.getNicotineIntake()).isEqualTo(null);
        assertThat(record.getQuantity()).isEqualTo(20);
        assertThat(record.getNicotineIntakeType()).isEqualTo(NICOTINE_INTAKE_TYPE_VAPE);
    }

    @Test
    public void nicotineIntakeRecordBuilder_invalidQuantity() {
        NicotineIntakeRecord.Builder builder =
                new NicotineIntakeRecord.Builder(
                        new Metadata.Builder().build(),
                        Instant.now().minusSeconds(60),
                        Instant.now(),
                        /* quantity= */ 150,
                        NICOTINE_INTAKE_TYPE_VAPE);

        assertThrows(IllegalArgumentException.class, builder::build);
    }

    @Test
    public void nicotineIntakeRecordBuilder_invalidZeroQuantity() {
        NicotineIntakeRecord.Builder builder =
                new NicotineIntakeRecord.Builder(
                        new Metadata.Builder().build(),
                        Instant.now().minusSeconds(60),
                        Instant.now(),
                        /* quantity= */ 0,
                        NICOTINE_INTAKE_TYPE_VAPE);

        assertThrows(IllegalArgumentException.class, builder::build);
    }

    @Test
    public void nicotineIntakeRecordBuilder_invalidNicotineIntakeType() {
        NicotineIntakeRecord.Builder builder =
                new NicotineIntakeRecord.Builder(
                        new Metadata.Builder().build(),
                        Instant.now().minusSeconds(60),
                        Instant.now(),
                        /* quantity= */ 20,
                        /* nicotineIntakeType= */ 35);

        assertThrows(IllegalArgumentException.class, builder::build);
    }

    @Test
    public void equals_hashCode_allFieldsEqual_recordsEqual() {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minusSeconds(60);
        ZoneOffset startZoneOffset = ZoneOffset.ofHours(2);
        ZoneOffset endZoneOffset = ZoneOffset.ofHours(-3);
        Metadata metadata = new Metadata.Builder().build();
        Mass nicotineIntake = Mass.fromGrams(0.01);

        NicotineIntakeRecord recordA =
                new NicotineIntakeRecord.Builder(
                                metadata,
                                startTime,
                                endTime,
                                /* quantity= */ 20,
                                NICOTINE_INTAKE_TYPE_VAPE)
                        .setStartZoneOffset(startZoneOffset)
                        .setEndZoneOffset(endZoneOffset)
                        .setNicotineIntake(nicotineIntake)
                        .build();

        NicotineIntakeRecord recordB =
                new NicotineIntakeRecord.Builder(
                                metadata,
                                startTime,
                                endTime,
                                /* quantity= */ 20,
                                NICOTINE_INTAKE_TYPE_VAPE)
                        .setStartZoneOffset(startZoneOffset)
                        .setEndZoneOffset(endZoneOffset)
                        .setNicotineIntake(nicotineIntake)
                        .build();

        assertThat(recordA.equals(recordB)).isTrue();
        assertThat(recordA.hashCode()).isEqualTo(recordB.hashCode());
    }

    @Test
    public void equals_hashCode_metadataNotEqual_recordsNotEqual() {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minusSeconds(60);
        ZoneOffset startZoneOffset = ZoneOffset.ofHours(2);
        ZoneOffset endZoneOffset = ZoneOffset.ofHours(-3);
        Metadata metadataA = new Metadata.Builder().setId("id-a").build();
        Metadata metadataB = new Metadata.Builder().setId("id-b").build();
        Mass nicotineIntake = Mass.fromGrams(0.01);

        NicotineIntakeRecord recordA =
                new NicotineIntakeRecord.Builder(
                                metadataA,
                                startTime,
                                endTime,
                                /* quantity= */ 20,
                                NICOTINE_INTAKE_TYPE_VAPE)
                        .setStartZoneOffset(startZoneOffset)
                        .setEndZoneOffset(endZoneOffset)
                        .setNicotineIntake(nicotineIntake)
                        .build();

        NicotineIntakeRecord recordB =
                new NicotineIntakeRecord.Builder(
                                metadataB,
                                startTime,
                                endTime,
                                /* quantity= */ 20,
                                NICOTINE_INTAKE_TYPE_VAPE)
                        .setStartZoneOffset(startZoneOffset)
                        .setEndZoneOffset(endZoneOffset)
                        .setNicotineIntake(nicotineIntake)
                        .build();

        assertThat(recordA).isNotEqualTo(recordB);
        assertThat(recordA.hashCode()).isNotEqualTo(recordB.hashCode());
    }

    @Test
    public void equals_hashCode_startTimeNotEqual_recordsNotEqual() {
        Instant endTime = Instant.now();
        Instant startTimeA = endTime.minusSeconds(60);
        Instant startTimeB = endTime.minusSeconds(120);
        ZoneOffset startZoneOffset = ZoneOffset.ofHours(2);
        ZoneOffset endZoneOffset = ZoneOffset.ofHours(-3);
        Metadata metadata = new Metadata.Builder().build();
        Mass nicotineIntake = Mass.fromGrams(0.01);

        NicotineIntakeRecord recordA =
                new NicotineIntakeRecord.Builder(
                                metadata,
                                startTimeA,
                                endTime,
                                /* quantity= */ 20,
                                NICOTINE_INTAKE_TYPE_VAPE)
                        .setStartZoneOffset(startZoneOffset)
                        .setEndZoneOffset(endZoneOffset)
                        .setNicotineIntake(nicotineIntake)
                        .build();

        NicotineIntakeRecord recordB =
                new NicotineIntakeRecord.Builder(
                                metadata,
                                startTimeB,
                                endTime,
                                /* quantity= */ 20,
                                NICOTINE_INTAKE_TYPE_VAPE)
                        .setStartZoneOffset(startZoneOffset)
                        .setEndZoneOffset(endZoneOffset)
                        .setNicotineIntake(nicotineIntake)
                        .build();

        assertThat(recordA).isNotEqualTo(recordB);
        assertThat(recordA.hashCode()).isNotEqualTo(recordB.hashCode());
    }

    @Test
    public void equals_hashCode_endTimeNotEqual_recordsNotEqual() {
        Instant endTimeA = Instant.now();
        Instant endTimeB = endTimeA.minusSeconds(1);
        Instant startTime = endTimeA.minusSeconds(60);
        ZoneOffset startZoneOffset = ZoneOffset.ofHours(2);
        ZoneOffset endZoneOffset = ZoneOffset.ofHours(-3);
        Metadata metadata = new Metadata.Builder().build();
        Mass nicotineIntake = Mass.fromGrams(0.01);

        NicotineIntakeRecord recordA =
                new NicotineIntakeRecord.Builder(
                                metadata,
                                startTime,
                                endTimeA,
                                /* quantity= */ 20,
                                NICOTINE_INTAKE_TYPE_VAPE)
                        .setStartZoneOffset(startZoneOffset)
                        .setEndZoneOffset(endZoneOffset)
                        .setNicotineIntake(nicotineIntake)
                        .build();

        NicotineIntakeRecord recordB =
                new NicotineIntakeRecord.Builder(
                                metadata,
                                startTime,
                                endTimeB,
                                /* quantity= */ 20,
                                NICOTINE_INTAKE_TYPE_VAPE)
                        .setStartZoneOffset(startZoneOffset)
                        .setEndZoneOffset(endZoneOffset)
                        .setNicotineIntake(nicotineIntake)
                        .build();

        assertThat(recordA).isNotEqualTo(recordB);
        assertThat(recordA.hashCode()).isNotEqualTo(recordB.hashCode());
    }

    @Test
    public void equals_hashCode_nicotineIntakeNotEqual_recordsNotEqual() {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minusSeconds(60);
        ZoneOffset startZoneOffset = ZoneOffset.ofHours(2);
        ZoneOffset endZoneOffset = ZoneOffset.ofHours(-3);
        Metadata metadata = new Metadata.Builder().build();
        Mass nicotineIntakeA = Mass.fromGrams(0.01);
        Mass nicotineIntakeB = Mass.fromGrams(0.02);

        NicotineIntakeRecord recordA =
                new NicotineIntakeRecord.Builder(
                                metadata,
                                startTime,
                                endTime,
                                /* quantity= */ 20,
                                NICOTINE_INTAKE_TYPE_VAPE)
                        .setStartZoneOffset(startZoneOffset)
                        .setEndZoneOffset(endZoneOffset)
                        .setNicotineIntake(nicotineIntakeA)
                        .build();

        NicotineIntakeRecord recordB =
                new NicotineIntakeRecord.Builder(
                                metadata,
                                startTime,
                                endTime,
                                /* quantity= */ 20,
                                NICOTINE_INTAKE_TYPE_VAPE)
                        .setStartZoneOffset(startZoneOffset)
                        .setEndZoneOffset(endZoneOffset)
                        .setNicotineIntake(nicotineIntakeB)
                        .build();

        assertThat(recordA).isNotEqualTo(recordB);
        assertThat(recordA.hashCode()).isNotEqualTo(recordB.hashCode());
    }

    @Test
    public void equals_hashCode_quantityNotEqual_recordsNotEqual() {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minusSeconds(60);
        ZoneOffset startZoneOffset = ZoneOffset.ofHours(2);
        ZoneOffset endZoneOffset = ZoneOffset.ofHours(-3);
        Metadata metadata = new Metadata.Builder().build();
        Mass nicotineIntake = Mass.fromGrams(0.01);

        NicotineIntakeRecord recordA =
                new NicotineIntakeRecord.Builder(
                                metadata,
                                startTime,
                                endTime,
                                /* quantity= */ 20,
                                NICOTINE_INTAKE_TYPE_VAPE)
                        .setStartZoneOffset(startZoneOffset)
                        .setEndZoneOffset(endZoneOffset)
                        .setNicotineIntake(nicotineIntake)
                        .build();

        NicotineIntakeRecord recordB =
                new NicotineIntakeRecord.Builder(
                                metadata,
                                startTime,
                                endTime,
                                /* quantity= */ 30,
                                NICOTINE_INTAKE_TYPE_VAPE)
                        .setStartZoneOffset(startZoneOffset)
                        .setEndZoneOffset(endZoneOffset)
                        .setNicotineIntake(nicotineIntake)
                        .build();

        assertThat(recordA).isNotEqualTo(recordB);
        assertThat(recordA.hashCode()).isNotEqualTo(recordB.hashCode());
    }

    @Test
    public void equals_hashCode_nicotineIntakeTypeNotEqual_recordsNotEqual() {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minusSeconds(60);
        ZoneOffset startZoneOffset = ZoneOffset.ofHours(2);
        ZoneOffset endZoneOffset = ZoneOffset.ofHours(-3);
        Metadata metadata = new Metadata.Builder().build();
        Mass nicotineIntake = Mass.fromGrams(0.01);

        NicotineIntakeRecord recordA =
                new NicotineIntakeRecord.Builder(
                                metadata,
                                startTime,
                                endTime,
                                /* quantity= */ 20,
                                NICOTINE_INTAKE_TYPE_VAPE)
                        .setStartZoneOffset(startZoneOffset)
                        .setEndZoneOffset(endZoneOffset)
                        .setNicotineIntake(nicotineIntake)
                        .build();

        NicotineIntakeRecord recordB =
                new NicotineIntakeRecord.Builder(
                                metadata,
                                startTime,
                                endTime,
                                /* quantity= */ 20,
                                NICOTINE_INTAKE_TYPE_CIGARETTE)
                        .setStartZoneOffset(startZoneOffset)
                        .setEndZoneOffset(endZoneOffset)
                        .setNicotineIntake(nicotineIntake)
                        .build();

        assertThat(recordA).isNotEqualTo(recordB);
        assertThat(recordA.hashCode()).isNotEqualTo(recordB.hashCode());
    }

    @Test
    public void equals_hashCode_startZoneOffsetNotEqual_recordsNotEqual() {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minusSeconds(60);
        ZoneOffset startZoneOffsetA = ZoneOffset.ofHours(2);
        ZoneOffset startZoneOffsetB = ZoneOffset.ofHours(1);
        ZoneOffset endZoneOffset = ZoneOffset.ofHours(-3);
        Metadata metadata = new Metadata.Builder().build();
        Mass nicotineIntake = Mass.fromGrams(0.01);

        NicotineIntakeRecord recordA =
                new NicotineIntakeRecord.Builder(
                                metadata,
                                startTime,
                                endTime,
                                /* quantity= */ 20,
                                NICOTINE_INTAKE_TYPE_VAPE)
                        .setStartZoneOffset(startZoneOffsetA)
                        .setEndZoneOffset(endZoneOffset)
                        .setNicotineIntake(nicotineIntake)
                        .build();

        NicotineIntakeRecord recordB =
                new NicotineIntakeRecord.Builder(
                                metadata,
                                startTime,
                                endTime,
                                /* quantity= */ 20,
                                NICOTINE_INTAKE_TYPE_VAPE)
                        .setStartZoneOffset(startZoneOffsetB)
                        .setEndZoneOffset(endZoneOffset)
                        .setNicotineIntake(nicotineIntake)
                        .build();

        assertThat(recordA).isNotEqualTo(recordB);
        assertThat(recordA.hashCode()).isNotEqualTo(recordB.hashCode());
    }

    @Test
    public void equals_hashCode_endZoneOffsetNotEqual_recordsNotEqual() {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minusSeconds(60);
        ZoneOffset startZoneOffset = ZoneOffset.ofHours(2);
        ZoneOffset endZoneOffsetA = ZoneOffset.ofHours(-3);
        ZoneOffset endZoneOffsetB = ZoneOffset.ofHours(-4);
        Metadata metadata = new Metadata.Builder().build();
        Mass nicotineIntake = Mass.fromGrams(0.01);

        NicotineIntakeRecord recordA =
                new NicotineIntakeRecord.Builder(
                                metadata,
                                startTime,
                                endTime,
                                /* quantity= */ 20,
                                NICOTINE_INTAKE_TYPE_VAPE)
                        .setStartZoneOffset(startZoneOffset)
                        .setEndZoneOffset(endZoneOffsetA)
                        .setNicotineIntake(nicotineIntake)
                        .build();

        NicotineIntakeRecord recordB =
                new NicotineIntakeRecord.Builder(
                                metadata,
                                startTime,
                                endTime,
                                /* quantity= */ 20,
                                NICOTINE_INTAKE_TYPE_VAPE)
                        .setStartZoneOffset(startZoneOffset)
                        .setEndZoneOffset(endZoneOffsetB)
                        .setNicotineIntake(nicotineIntake)
                        .build();

        assertThat(recordA).isNotEqualTo(recordB);
        assertThat(recordA.hashCode()).isNotEqualTo(recordB.hashCode());
    }

    @Test
    public void equals_hashCode_nicotineIntakeValueEqual_referencesNotEqual_recordsEqual() {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minusSeconds(60);
        ZoneOffset startZoneOffset = ZoneOffset.ofHours(2);
        ZoneOffset endZoneOffset = ZoneOffset.ofHours(-3);
        Metadata metadata = new Metadata.Builder().build();
        Mass nicotineIntakeA = Mass.fromGrams(0.01);
        Mass nicotineIntakeB = Mass.fromGrams(0.01);

        NicotineIntakeRecord recordA =
                new NicotineIntakeRecord.Builder(
                                metadata,
                                startTime,
                                endTime,
                                /* quantity= */ 20,
                                NICOTINE_INTAKE_TYPE_VAPE)
                        .setStartZoneOffset(startZoneOffset)
                        .setEndZoneOffset(endZoneOffset)
                        .setNicotineIntake(nicotineIntakeA)
                        .build();

        NicotineIntakeRecord recordB =
                new NicotineIntakeRecord.Builder(
                                metadata,
                                startTime,
                                endTime,
                                /* quantity= */ 20,
                                NICOTINE_INTAKE_TYPE_VAPE)
                        .setStartZoneOffset(startZoneOffset)
                        .setEndZoneOffset(endZoneOffset)
                        .setNicotineIntake(nicotineIntakeB)
                        .build();

        assertThat(recordA.equals(recordB)).isTrue();
        assertThat(recordA.hashCode()).isEqualTo(recordB.hashCode());
    }

    @Test
    public void equals_hashCode_optionalFieldsNotSet_recordsEqual() {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minusSeconds(60);
        ZoneOffset startZoneOffset = ZoneOffset.ofHours(2);
        ZoneOffset endZoneOffset = ZoneOffset.ofHours(-3);
        Metadata metadata = new Metadata.Builder().build();

        NicotineIntakeRecord recordA =
                new NicotineIntakeRecord.Builder(
                                metadata,
                                startTime,
                                endTime,
                                /* quantity= */ 20,
                                NICOTINE_INTAKE_TYPE_VAPE)
                        .setStartZoneOffset(startZoneOffset)
                        .setEndZoneOffset(endZoneOffset)
                        .build();

        NicotineIntakeRecord recordB =
                new NicotineIntakeRecord.Builder(
                                metadata,
                                startTime,
                                endTime,
                                /* quantity= */ 20,
                                NICOTINE_INTAKE_TYPE_VAPE)
                        .setStartZoneOffset(startZoneOffset)
                        .setEndZoneOffset(endZoneOffset)
                        .build();

        assertThat(recordA.equals(recordB)).isTrue();
        assertThat(recordA.hashCode()).isEqualTo(recordB.hashCode());
    }

    private static ZoneOffset getDefaultZoneOffset(Instant instant) {
        return ZoneOffset.systemDefault().getRules().getOffset(instant);
    }
}
