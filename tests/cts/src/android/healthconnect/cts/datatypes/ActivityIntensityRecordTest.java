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

package android.healthconnect.cts.datatypes;

import static android.health.connect.datatypes.ActivityIntensityRecord.ACTIVITY_INTENSITY_TYPE_MODERATE;
import static android.health.connect.datatypes.ActivityIntensityRecord.ACTIVITY_INTENSITY_TYPE_VIGOROUS;
import static android.health.connect.datatypes.Device.DEVICE_TYPE_FITNESS_BAND;
import static android.health.connect.datatypes.Metadata.RECORDING_METHOD_MANUAL_ENTRY;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_ACTIVITY_INTENSITY;

import static com.android.healthfitness.flags.Flags.FLAG_ACTIVITY_INTENSITY;
import static com.android.healthfitness.flags.Flags.FLAG_ACTIVITY_INTENSITY_DB;
import static com.android.healthfitness.flags.Flags.FLAG_HEALTH_CONNECT_MAPPINGS;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.datatypes.ActivityIntensityRecord;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.Device;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.SkinTemperatureRecord;
import android.healthconnect.cts.utils.AssumptionCheckerRule;
import android.healthconnect.cts.utils.DeviceSupportUtils;
import android.healthconnect.cts.utils.TestUtils;
import android.platform.test.annotations.AppModeFull;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.time.ZoneOffset;

@AppModeFull(reason = "HealthConnectManager is not accessible to instant apps")
@RunWith(AndroidJUnit4.class)
@RequiresFlagsEnabled({
    FLAG_ACTIVITY_INTENSITY,
    FLAG_ACTIVITY_INTENSITY_DB,
    FLAG_HEALTH_CONNECT_MAPPINGS
})
public class ActivityIntensityRecordTest {

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Rule
    public AssumptionCheckerRule mSupportedHardwareRule =
            new AssumptionCheckerRule(
                    DeviceSupportUtils::isHealthConnectFullySupported,
                    "Tests should run on supported hardware only.");

    @After
    public void tearDown() throws InterruptedException {
        TestUtils.verifyDeleteRecords(
                SkinTemperatureRecord.class,
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.EPOCH)
                        .setEndTime(Instant.now())
                        .build());
        TestUtils.deleteAllStagedRemoteData();
    }

    @Before
    public void setUp() throws InterruptedException {
        TestUtils.deleteAllStagedRemoteData();
    }

    @Test
    public void activityIntensityRecordBuilder_allFieldsSet() {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minusSeconds(60);
        ZoneOffset startZoneOffset = ZoneOffset.ofHours(2);
        ZoneOffset endZoneOffset = ZoneOffset.ofHours(-3);
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

        ActivityIntensityRecord record =
                new ActivityIntensityRecord.Builder(
                                metadata, startTime, endTime, ACTIVITY_INTENSITY_TYPE_VIGOROUS)
                        .setStartZoneOffset(startZoneOffset)
                        .setEndZoneOffset(endZoneOffset)
                        .build();

        assertThat(record.getRecordType()).isEqualTo(RECORD_TYPE_ACTIVITY_INTENSITY);
        assertThat(record.getMetadata()).isEqualTo(metadata);
        assertThat(record.getStartTime()).isEqualTo(startTime);
        assertThat(record.getEndTime()).isEqualTo(endTime);
        assertThat(record.getStartZoneOffset()).isEqualTo(startZoneOffset);
        assertThat(record.getEndZoneOffset()).isEqualTo(endZoneOffset);
        assertThat(record.getActivityIntensityType()).isEqualTo(ACTIVITY_INTENSITY_TYPE_VIGOROUS);
    }

    @Test
    public void activityIntensityRecordBuilder_optionalFieldsUnset() {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minusSeconds(60);
        Metadata metadata = new Metadata.Builder().build();

        ActivityIntensityRecord record =
                new ActivityIntensityRecord.Builder(
                                metadata, startTime, endTime, ACTIVITY_INTENSITY_TYPE_MODERATE)
                        .build();

        assertThat(record.getRecordType()).isEqualTo(RECORD_TYPE_ACTIVITY_INTENSITY);
        assertThat(record.getMetadata()).isEqualTo(metadata);
        assertThat(record.getStartTime()).isEqualTo(startTime);
        assertThat(record.getEndTime()).isEqualTo(endTime);
        assertThat(record.getStartZoneOffset()).isEqualTo(getDefaultZoneOffset(startTime));
        assertThat(record.getEndZoneOffset()).isEqualTo(getDefaultZoneOffset(endTime));
        assertThat(record.getActivityIntensityType()).isEqualTo(ACTIVITY_INTENSITY_TYPE_MODERATE);
    }

    @Test
    public void activityIntensityRecordBuilder_invalidActivityIntensityType() {
        ActivityIntensityRecord.Builder builder =
                new ActivityIntensityRecord.Builder(
                        new Metadata.Builder().build(),
                        Instant.now().minusSeconds(60),
                        Instant.now(),
                        54);

        assertThrows(IllegalArgumentException.class, builder::build);
    }

    @Test
    public void equals_hashCode_allFieldsEqual_recordsEqual() {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minusSeconds(60);
        ZoneOffset startZoneOffset = ZoneOffset.ofHours(2);
        ZoneOffset endZoneOffset = ZoneOffset.ofHours(-3);
        Metadata metadata = new Metadata.Builder().build();

        ActivityIntensityRecord recordA =
                new ActivityIntensityRecord.Builder(
                                metadata, startTime, endTime, ACTIVITY_INTENSITY_TYPE_VIGOROUS)
                        .setStartZoneOffset(startZoneOffset)
                        .setEndZoneOffset(endZoneOffset)
                        .build();

        ActivityIntensityRecord recordB =
                new ActivityIntensityRecord.Builder(
                                metadata, startTime, endTime, ACTIVITY_INTENSITY_TYPE_VIGOROUS)
                        .setStartZoneOffset(startZoneOffset)
                        .setEndZoneOffset(endZoneOffset)
                        .build();

        assertThat(recordA).isEqualTo(recordB);
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

        ActivityIntensityRecord recordA =
                new ActivityIntensityRecord.Builder(
                                metadataA, startTime, endTime, ACTIVITY_INTENSITY_TYPE_VIGOROUS)
                        .setStartZoneOffset(startZoneOffset)
                        .setEndZoneOffset(endZoneOffset)
                        .build();

        ActivityIntensityRecord recordB =
                new ActivityIntensityRecord.Builder(
                                metadataB, startTime, endTime, ACTIVITY_INTENSITY_TYPE_VIGOROUS)
                        .setStartZoneOffset(startZoneOffset)
                        .setEndZoneOffset(endZoneOffset)
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

        ActivityIntensityRecord recordA =
                new ActivityIntensityRecord.Builder(
                                metadata, startTimeA, endTime, ACTIVITY_INTENSITY_TYPE_MODERATE)
                        .setStartZoneOffset(startZoneOffset)
                        .setEndZoneOffset(endZoneOffset)
                        .build();

        ActivityIntensityRecord recordB =
                new ActivityIntensityRecord.Builder(
                                metadata, startTimeB, endTime, ACTIVITY_INTENSITY_TYPE_MODERATE)
                        .setStartZoneOffset(startZoneOffset)
                        .setEndZoneOffset(endZoneOffset)
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

        ActivityIntensityRecord recordA =
                new ActivityIntensityRecord.Builder(
                                metadata, startTime, endTimeA, ACTIVITY_INTENSITY_TYPE_MODERATE)
                        .setStartZoneOffset(startZoneOffset)
                        .setEndZoneOffset(endZoneOffset)
                        .build();

        ActivityIntensityRecord recordB =
                new ActivityIntensityRecord.Builder(
                                metadata, startTime, endTimeB, ACTIVITY_INTENSITY_TYPE_MODERATE)
                        .setStartZoneOffset(startZoneOffset)
                        .setEndZoneOffset(endZoneOffset)
                        .build();

        assertThat(recordA).isNotEqualTo(recordB);
        assertThat(recordA.hashCode()).isNotEqualTo(recordB.hashCode());
    }

    @Test
    public void equals_hashCode_intensityTypeNotEqual_recordsNotEqual() {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minusSeconds(60);
        ZoneOffset startZoneOffset = ZoneOffset.ofHours(2);
        ZoneOffset endZoneOffset = ZoneOffset.ofHours(-3);
        Metadata metadata = new Metadata.Builder().build();

        ActivityIntensityRecord recordA =
                new ActivityIntensityRecord.Builder(
                                metadata, startTime, endTime, ACTIVITY_INTENSITY_TYPE_MODERATE)
                        .setStartZoneOffset(startZoneOffset)
                        .setEndZoneOffset(endZoneOffset)
                        .build();

        ActivityIntensityRecord recordB =
                new ActivityIntensityRecord.Builder(
                                metadata, startTime, endTime, ACTIVITY_INTENSITY_TYPE_VIGOROUS)
                        .setStartZoneOffset(startZoneOffset)
                        .setEndZoneOffset(endZoneOffset)
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

        ActivityIntensityRecord recordA =
                new ActivityIntensityRecord.Builder(
                                metadata, startTime, endTime, ACTIVITY_INTENSITY_TYPE_MODERATE)
                        .setStartZoneOffset(startZoneOffsetA)
                        .setEndZoneOffset(endZoneOffset)
                        .build();

        ActivityIntensityRecord recordB =
                new ActivityIntensityRecord.Builder(
                                metadata, startTime, endTime, ACTIVITY_INTENSITY_TYPE_MODERATE)
                        .setStartZoneOffset(startZoneOffsetB)
                        .setEndZoneOffset(endZoneOffset)
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
        ZoneOffset endZoneOffsetB = ZoneOffset.ofHours(-2);
        Metadata metadata = new Metadata.Builder().build();

        ActivityIntensityRecord recordA =
                new ActivityIntensityRecord.Builder(
                                metadata, startTime, endTime, ACTIVITY_INTENSITY_TYPE_MODERATE)
                        .setStartZoneOffset(startZoneOffset)
                        .setEndZoneOffset(endZoneOffsetA)
                        .build();

        ActivityIntensityRecord recordB =
                new ActivityIntensityRecord.Builder(
                                metadata, startTime, endTime, ACTIVITY_INTENSITY_TYPE_MODERATE)
                        .setStartZoneOffset(startZoneOffset)
                        .setEndZoneOffset(endZoneOffsetB)
                        .build();

        assertThat(recordA).isNotEqualTo(recordB);
        assertThat(recordA.hashCode()).isNotEqualTo(recordB.hashCode());
    }

    private static ZoneOffset getDefaultZoneOffset(Instant instant) {
        return ZoneOffset.systemDefault().getRules().getOffset(instant);
    }
}
