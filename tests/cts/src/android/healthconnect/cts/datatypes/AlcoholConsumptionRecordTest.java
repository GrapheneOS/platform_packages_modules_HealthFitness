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

import static android.health.connect.datatypes.AlcoholConsumptionRecord.ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_BEER;
import static android.health.connect.datatypes.AlcoholConsumptionRecord.ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_OTHER;
import static android.health.connect.datatypes.AlcoholConsumptionRecord.ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_WINE;
import static android.health.connect.datatypes.AlcoholConsumptionRecord.ALCOHOL_CONSUMPTION_SERVING_SIZE_HALF_PINT;
import static android.health.connect.datatypes.AlcoholConsumptionRecord.ALCOHOL_CONSUMPTION_SERVING_SIZE_OTHER;
import static android.health.connect.datatypes.AlcoholConsumptionRecord.ALCOHOL_CONSUMPTION_SERVING_SIZE_PINT;
import static android.health.connect.datatypes.Device.DEVICE_TYPE_PHONE;
import static android.health.connect.datatypes.Metadata.RECORDING_METHOD_MANUAL_ENTRY;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_ALCOHOL_CONSUMPTION;

import static com.android.healthfitness.flags.Flags.FLAG_ALCOHOL_CONSUMPTION;
import static com.android.healthfitness.flags.Flags.FLAG_ALCOHOL_CONSUMPTION_DB;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.health.connect.datatypes.AlcoholConsumptionRecord;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.Device;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.units.Percentage;
import android.health.connect.datatypes.units.Volume;
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
@RequiresFlagsEnabled({FLAG_ALCOHOL_CONSUMPTION, FLAG_ALCOHOL_CONSUMPTION_DB})
public class AlcoholConsumptionRecordTest {
    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Rule
    public AssumptionCheckerRule mSupportedHardwareRule =
            new AssumptionCheckerRule(
                    DeviceSupportUtils::isHealthConnectFullySupported,
                    "Tests should run on supported hardware only.");

    @Test
    public void alcoholConsumptionRecordBuilder_allFieldsSet() {
        Instant startTime = Instant.now().minusSeconds(60);
        Instant endTime = Instant.now();
        ZoneOffset startZoneOffset = ZoneOffset.ofHours(2);
        ZoneOffset endZoneOffset = ZoneOffset.ofHours(2);
        int servingCount = 2;
        int beverageType = ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_BEER;
        int servingSize = ALCOHOL_CONSUMPTION_SERVING_SIZE_PINT;
        Volume servingVolume = Volume.fromLiters(568.0 / 1000);
        Percentage alcoholByVolume = Percentage.fromValue(7);
        CharSequence note = "Pub Crawl";
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
                                        .setType(DEVICE_TYPE_PHONE)
                                        .setManufacturer("manufacturer")
                                        .setModel("model")
                                        .build())
                        .setLastModifiedTime(startTime.plusSeconds(10))
                        .build();

        AlcoholConsumptionRecord record =
                new AlcoholConsumptionRecord.Builder(
                                metadata, startTime, endTime, servingCount, beverageType)
                        .setServingSize(servingSize)
                        .setStartZoneOffset(startZoneOffset)
                        .setEndZoneOffset(endZoneOffset)
                        .setServingVolume(servingVolume)
                        .setAlcoholByVolume(alcoholByVolume)
                        .setNote(note)
                        .build();

        assertThat(record.getRecordType()).isEqualTo(RECORD_TYPE_ALCOHOL_CONSUMPTION);
        assertThat(record.getMetadata()).isEqualTo(metadata);
        assertThat(record.getStartTime()).isEqualTo(startTime);
        assertThat(record.getEndTime()).isEqualTo(endTime);
        assertThat(record.getStartZoneOffset()).isEqualTo(startZoneOffset);
        assertThat(record.getEndZoneOffset()).isEqualTo(endZoneOffset);
        assertThat(record.getServingCount()).isEqualTo(servingCount);
        assertThat(record.getBeverageType()).isEqualTo(beverageType);
        assertThat(record.getServingSize()).isEqualTo(servingSize);
        assertThat(record.getServingVolume()).isEqualTo(servingVolume);
        assertThat(record.getAlcoholByVolume()).isEqualTo(alcoholByVolume);
        assertThat(record.getNote()).isEqualTo(note);
    }

    @Test
    public void alcoholConsumptionRecordBuilder_optionalFieldsNotSet() {
        Instant startTime = Instant.now().minusSeconds(60);
        Instant endTime = Instant.now();
        int servingCount = 2;
        int beverageType = ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_BEER;
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
                                        .setType(DEVICE_TYPE_PHONE)
                                        .setManufacturer("manufacturer")
                                        .setModel("model")
                                        .build())
                        .setLastModifiedTime(startTime.plusSeconds(10))
                        .build();

        AlcoholConsumptionRecord record =
                new AlcoholConsumptionRecord.Builder(
                                metadata, startTime, endTime, servingCount, beverageType)
                        .build();

        assertThat(record.getRecordType()).isEqualTo(RECORD_TYPE_ALCOHOL_CONSUMPTION);
        assertThat(record.getMetadata()).isEqualTo(metadata);
        assertThat(record.getStartTime()).isEqualTo(startTime);
        assertThat(record.getEndTime()).isEqualTo(endTime);
        assertThat(record.getStartZoneOffset()).isEqualTo(getDefaultZoneOffset(startTime));
        assertThat(record.getEndZoneOffset()).isEqualTo(getDefaultZoneOffset(endTime));
        assertThat(record.getServingCount()).isEqualTo(servingCount);
        assertThat(record.getBeverageType()).isEqualTo(beverageType);
        assertThat(record.getServingSize()).isEqualTo(ALCOHOL_CONSUMPTION_SERVING_SIZE_OTHER);
        assertThat(record.getServingVolume()).isEqualTo(null);
        assertThat(record.getAlcoholByVolume()).isEqualTo(null);
        assertThat(record.getNote()).isEqualTo(null);
    }

    @Test
    public void alcoholConsumptionRecordBuilder_invalidType() {
        Metadata metadata = new Metadata.Builder().build();
        Instant time = Instant.now();

        assertThrows(
                IllegalArgumentException.class,
                () -> new AlcoholConsumptionRecord.Builder(metadata, time, 1, 505).build());
    }

    @Test
    public void alcoholConsumptionRecordBuilder_invalidServingSize() {
        Metadata metadata = new Metadata.Builder().build();
        Instant time = Instant.now();

        AlcoholConsumptionRecord.Builder builder =
                new AlcoholConsumptionRecord.Builder(
                                metadata,
                                time,
                                time.plusSeconds(1),
                                1,
                                ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_OTHER)
                        .setServingSize(1000);

        assertThrows(IllegalArgumentException.class, builder::build);
    }

    @Test
    public void equals_hashCode_allFieldsEqual_recordsEqual() {
        Instant startTime = Instant.now().minusSeconds(60);
        Instant endTime = Instant.now();
        ZoneOffset startZoneOffset = ZoneOffset.ofHours(2);
        ZoneOffset endZoneOffset = ZoneOffset.ofHours(2);
        int servingCount = 2;
        int beverageType = ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_BEER;
        int servingSize = ALCOHOL_CONSUMPTION_SERVING_SIZE_PINT;
        Volume servingVolume = Volume.fromLiters(568.0 / 1000);
        Percentage alcoholByVolume = Percentage.fromValue(7);
        CharSequence note = "Pub Crawl";
        Metadata metadata = new Metadata.Builder().build();

        AlcoholConsumptionRecord recordA =
                new AlcoholConsumptionRecord.Builder(
                                metadata, startTime, endTime, servingCount, beverageType)
                        .setServingSize(servingSize)
                        .setStartZoneOffset(startZoneOffset)
                        .setEndZoneOffset(endZoneOffset)
                        .setServingVolume(servingVolume)
                        .setAlcoholByVolume(alcoholByVolume)
                        .setNote(note)
                        .build();

        AlcoholConsumptionRecord recordB =
                new AlcoholConsumptionRecord.Builder(
                                metadata, startTime, endTime, servingCount, beverageType)
                        .setServingSize(servingSize)
                        .setStartZoneOffset(startZoneOffset)
                        .setEndZoneOffset(endZoneOffset)
                        .setServingVolume(servingVolume)
                        .setAlcoholByVolume(alcoholByVolume)
                        .setNote(note)
                        .build();

        assertThat(recordA).isEqualTo(recordB);
        assertThat(recordA.hashCode()).isEqualTo(recordB.hashCode());
    }

    @Test
    public void equals_hashCode_metadataNotEqual_recordsNotEqual() {
        Instant time = Instant.now().minusSeconds(60);
        ZoneOffset zoneOffset = ZoneOffset.ofHours(2);
        int servingCount = 2;
        int beverageType = ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_BEER;
        int servingSize = ALCOHOL_CONSUMPTION_SERVING_SIZE_PINT;
        Volume servingVolume = Volume.fromLiters(568.0 / 1000);
        Percentage alcoholByVolume = Percentage.fromValue(7);
        CharSequence note = "Pub Crawl";
        Metadata metadataA = new Metadata.Builder().setId("id-a").build();
        Metadata metadataB = new Metadata.Builder().setId("id-b").build();

        AlcoholConsumptionRecord recordA =
                new AlcoholConsumptionRecord.Builder(metadataA, time, servingCount, beverageType)
                        .setServingSize(servingSize)
                        .setStartZoneOffset(zoneOffset)
                        .setEndZoneOffset(zoneOffset)
                        .setServingVolume(servingVolume)
                        .setAlcoholByVolume(alcoholByVolume)
                        .setNote(note)
                        .build();

        AlcoholConsumptionRecord recordB =
                new AlcoholConsumptionRecord.Builder(metadataB, time, servingCount, beverageType)
                        .setServingSize(servingSize)
                        .setStartZoneOffset(zoneOffset)
                        .setEndZoneOffset(zoneOffset)
                        .setServingVolume(servingVolume)
                        .setAlcoholByVolume(alcoholByVolume)
                        .setNote(note)
                        .build();

        assertThat(recordA).isNotEqualTo(recordB);
        assertThat(recordA.hashCode()).isNotEqualTo(recordB.hashCode());
    }

    @Test
    public void equals_hashCode_timeNotEqual_recordsNotEqual() {
        Instant timeA = Instant.now().minusSeconds(60);
        Instant timeB = Instant.now().minusSeconds(180);
        ZoneOffset zoneOffset = ZoneOffset.ofHours(2);
        int servingCount = 2;
        int beverageType = ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_BEER;
        int servingSize = ALCOHOL_CONSUMPTION_SERVING_SIZE_PINT;
        Volume servingVolume = Volume.fromLiters(568.0 / 1000);
        Percentage alcoholByVolume = Percentage.fromValue(7);
        CharSequence note = "Pub Crawl";
        Metadata metadata = new Metadata.Builder().build();

        AlcoholConsumptionRecord recordA =
                new AlcoholConsumptionRecord.Builder(metadata, timeA, servingCount, beverageType)
                        .setServingSize(servingSize)
                        .setStartZoneOffset(zoneOffset)
                        .setEndZoneOffset(zoneOffset)
                        .setServingVolume(servingVolume)
                        .setAlcoholByVolume(alcoholByVolume)
                        .setNote(note)
                        .build();

        AlcoholConsumptionRecord recordB =
                new AlcoholConsumptionRecord.Builder(metadata, timeB, servingCount, beverageType)
                        .setServingSize(servingSize)
                        .setStartZoneOffset(zoneOffset)
                        .setEndZoneOffset(zoneOffset)
                        .setServingVolume(servingVolume)
                        .setAlcoholByVolume(alcoholByVolume)
                        .setNote(note)
                        .build();

        assertThat(recordA).isNotEqualTo(recordB);
        assertThat(recordA.hashCode()).isNotEqualTo(recordB.hashCode());
    }

    @Test
    public void equals_hashCode_zoneOffsetNotEqual_recordsNotEqual() {
        Instant time = Instant.now().minusSeconds(60);
        ZoneOffset zoneOffsetA = ZoneOffset.ofHours(2);
        ZoneOffset zoneOffsetB = ZoneOffset.ofHours(5);
        int servingCount = 2;
        int beverageType = ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_BEER;
        int servingSize = ALCOHOL_CONSUMPTION_SERVING_SIZE_PINT;
        Volume servingVolume = Volume.fromLiters(568.0 / 1000);
        Percentage alcoholByVolume = Percentage.fromValue(7);
        CharSequence note = "Pub crawl";
        Metadata metadata = new Metadata.Builder().build();

        AlcoholConsumptionRecord recordA =
                new AlcoholConsumptionRecord.Builder(metadata, time, servingCount, beverageType)
                        .setServingSize(servingSize)
                        .setStartZoneOffset(zoneOffsetA)
                        .setEndZoneOffset(zoneOffsetA)
                        .setServingVolume(servingVolume)
                        .setAlcoholByVolume(alcoholByVolume)
                        .setNote(note)
                        .build();

        AlcoholConsumptionRecord recordB =
                new AlcoholConsumptionRecord.Builder(metadata, time, servingCount, beverageType)
                        .setServingSize(servingSize)
                        .setStartZoneOffset(zoneOffsetB)
                        .setEndZoneOffset(zoneOffsetB)
                        .setServingVolume(servingVolume)
                        .setAlcoholByVolume(alcoholByVolume)
                        .setNote(note)
                        .build();

        assertThat(recordA).isNotEqualTo(recordB);
        assertThat(recordA.hashCode()).isNotEqualTo(recordB.hashCode());
    }

    @Test
    public void equals_hashCode_servingCountNotEqual_recordsNotEqual() {
        Instant time = Instant.now().minusSeconds(60);
        ZoneOffset zoneOffset = ZoneOffset.ofHours(2);
        int servingCountA = 2;
        int servingCountB = 20;
        int beverageType = ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_BEER;
        int servingSize = ALCOHOL_CONSUMPTION_SERVING_SIZE_PINT;
        Volume servingVolume = Volume.fromLiters(568.0 / 1000);
        Percentage alcoholByVolume = Percentage.fromValue(7);
        CharSequence note = "Pub Crawl";
        Metadata metadata = new Metadata.Builder().build();

        AlcoholConsumptionRecord recordA =
                new AlcoholConsumptionRecord.Builder(metadata, time, servingCountA, beverageType)
                        .setServingSize(servingSize)
                        .setStartZoneOffset(zoneOffset)
                        .setEndZoneOffset(zoneOffset)
                        .setServingVolume(servingVolume)
                        .setAlcoholByVolume(alcoholByVolume)
                        .setNote(note)
                        .build();

        AlcoholConsumptionRecord recordB =
                new AlcoholConsumptionRecord.Builder(metadata, time, servingCountB, beverageType)
                        .setServingSize(servingSize)
                        .setStartZoneOffset(zoneOffset)
                        .setEndZoneOffset(zoneOffset)
                        .setServingVolume(servingVolume)
                        .setAlcoholByVolume(alcoholByVolume)
                        .setNote(note)
                        .build();

        assertThat(recordA).isNotEqualTo(recordB);
        assertThat(recordA.hashCode()).isNotEqualTo(recordB.hashCode());
    }

    @Test
    public void equals_hashCode_alcoholTypeNotEqual_recordsNotEqual() {
        Instant time = Instant.now().minusSeconds(60);
        ZoneOffset zoneOffset = ZoneOffset.ofHours(2);
        int servingCount = 2;
        int servingSize = ALCOHOL_CONSUMPTION_SERVING_SIZE_PINT;
        Volume servingVolume = Volume.fromLiters(568.0 / 1000);
        Percentage alcoholByVolume = Percentage.fromValue(7);
        CharSequence note = "Pub Crawl";
        Metadata metadata = new Metadata.Builder().build();

        AlcoholConsumptionRecord recordA =
                new AlcoholConsumptionRecord.Builder(
                                metadata,
                                time,
                                servingCount,
                                ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_BEER)
                        .setServingSize(servingSize)
                        .setStartZoneOffset(zoneOffset)
                        .setEndZoneOffset(zoneOffset)
                        .setServingVolume(servingVolume)
                        .setAlcoholByVolume(alcoholByVolume)
                        .setNote(note)
                        .build();

        AlcoholConsumptionRecord recordB =
                new AlcoholConsumptionRecord.Builder(
                                metadata,
                                time,
                                servingCount,
                                ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_WINE)
                        .setServingSize(servingSize)
                        .setStartZoneOffset(zoneOffset)
                        .setEndZoneOffset(zoneOffset)
                        .setServingVolume(servingVolume)
                        .setAlcoholByVolume(alcoholByVolume)
                        .setNote(note)
                        .build();

        assertThat(recordA).isNotEqualTo(recordB);
        assertThat(recordA.hashCode()).isNotEqualTo(recordB.hashCode());
    }

    @Test
    public void equals_hashCode_servingSizeNotEqual_recordsNotEqual() {
        Instant time = Instant.now().minusSeconds(60);
        ZoneOffset zoneOffset = ZoneOffset.ofHours(2);
        int servingCount = 2;
        int beverageType = ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_BEER;
        Volume servingVolume = Volume.fromLiters(568.0 / 1000);
        Percentage alcoholByVolume = Percentage.fromValue(7);
        CharSequence note = "Pub Crawl";
        Metadata metadata = new Metadata.Builder().build();

        AlcoholConsumptionRecord recordA =
                new AlcoholConsumptionRecord.Builder(metadata, time, servingCount, beverageType)
                        .setServingSize(ALCOHOL_CONSUMPTION_SERVING_SIZE_PINT)
                        .setStartZoneOffset(zoneOffset)
                        .setEndZoneOffset(zoneOffset)
                        .setServingVolume(servingVolume)
                        .setAlcoholByVolume(alcoholByVolume)
                        .setNote(note)
                        .build();

        AlcoholConsumptionRecord recordB =
                new AlcoholConsumptionRecord.Builder(metadata, time, servingCount, beverageType)
                        .setServingSize(ALCOHOL_CONSUMPTION_SERVING_SIZE_HALF_PINT)
                        .setStartZoneOffset(zoneOffset)
                        .setEndZoneOffset(zoneOffset)
                        .setServingVolume(servingVolume)
                        .setAlcoholByVolume(alcoholByVolume)
                        .setNote(note)
                        .build();

        assertThat(recordA).isNotEqualTo(recordB);
        assertThat(recordA.hashCode()).isNotEqualTo(recordB.hashCode());
    }

    @Test
    public void equals_hashCode_servingVolumeNotEqual_recordsNotEqual() {
        Instant time = Instant.now().minusSeconds(60);
        ZoneOffset zoneOffset = ZoneOffset.ofHours(2);
        int servingCount = 2;
        int beverageType = ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_BEER;
        int servingSize = ALCOHOL_CONSUMPTION_SERVING_SIZE_PINT;
        Volume servingVolumeA = Volume.fromLiters(568.0 / 1000);
        Volume servingVolumeB = Volume.fromLiters(252.0 / 1000);
        Percentage alcoholByVolume = Percentage.fromValue(7);
        CharSequence note = "Pub Crawl";
        Metadata metadata = new Metadata.Builder().build();

        AlcoholConsumptionRecord recordA =
                new AlcoholConsumptionRecord.Builder(metadata, time, servingCount, beverageType)
                        .setServingSize(servingSize)
                        .setStartZoneOffset(zoneOffset)
                        .setEndZoneOffset(zoneOffset)
                        .setServingVolume(servingVolumeA)
                        .setAlcoholByVolume(alcoholByVolume)
                        .setNote(note)
                        .build();

        AlcoholConsumptionRecord recordB =
                new AlcoholConsumptionRecord.Builder(metadata, time, servingCount, beverageType)
                        .setServingSize(servingSize)
                        .setStartZoneOffset(zoneOffset)
                        .setEndZoneOffset(zoneOffset)
                        .setServingVolume(servingVolumeB)
                        .setAlcoholByVolume(alcoholByVolume)
                        .setNote(note)
                        .build();

        assertThat(recordA).isNotEqualTo(recordB);
        assertThat(recordA.hashCode()).isNotEqualTo(recordB.hashCode());
    }

    @Test
    public void equals_hashCode_alcoholByVolumeNotEqual_recordsNotEqual() {
        Instant time = Instant.now().minusSeconds(60);
        ZoneOffset zoneOffset = ZoneOffset.ofHours(2);
        int servingCount = 2;
        int beverageType = ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_BEER;
        int servingSize = ALCOHOL_CONSUMPTION_SERVING_SIZE_PINT;
        Volume servingVolume = Volume.fromLiters(568.0 / 1000);
        Percentage alcoholByVolumeA = Percentage.fromValue(7);
        Percentage alcoholByVolumeB = Percentage.fromValue(12);
        CharSequence note = "Pub Crawl";
        Metadata metadata = new Metadata.Builder().build();

        AlcoholConsumptionRecord recordA =
                new AlcoholConsumptionRecord.Builder(metadata, time, servingCount, beverageType)
                        .setServingSize(servingSize)
                        .setStartZoneOffset(zoneOffset)
                        .setEndZoneOffset(zoneOffset)
                        .setServingVolume(servingVolume)
                        .setAlcoholByVolume(alcoholByVolumeA)
                        .setNote(note)
                        .build();

        AlcoholConsumptionRecord recordB =
                new AlcoholConsumptionRecord.Builder(metadata, time, servingCount, beverageType)
                        .setServingSize(servingSize)
                        .setStartZoneOffset(zoneOffset)
                        .setEndZoneOffset(zoneOffset)
                        .setServingVolume(servingVolume)
                        .setAlcoholByVolume(alcoholByVolumeB)
                        .setNote(note)
                        .build();

        assertThat(recordA).isNotEqualTo(recordB);
        assertThat(recordA.hashCode()).isNotEqualTo(recordB.hashCode());
    }

    @Test
    public void equals_hashCode_noteNotEqual_recordsNotEqual() {
        Instant time = Instant.now().minusSeconds(60);
        ZoneOffset zoneOffset = ZoneOffset.ofHours(2);
        int servingCount = 2;
        int beverageType = ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_BEER;
        int servingSize = ALCOHOL_CONSUMPTION_SERVING_SIZE_PINT;
        Volume servingVolume = Volume.fromLiters(568.0 / 1000);
        Percentage alcoholByVolume = Percentage.fromValue(7);
        CharSequence noteA = "Pub Crawl";
        CharSequence noteB = "Otley Run";
        Metadata metadata = new Metadata.Builder().build();

        AlcoholConsumptionRecord recordA =
                new AlcoholConsumptionRecord.Builder(metadata, time, servingCount, beverageType)
                        .setServingSize(servingSize)
                        .setStartZoneOffset(zoneOffset)
                        .setEndZoneOffset(zoneOffset)
                        .setServingVolume(servingVolume)
                        .setAlcoholByVolume(alcoholByVolume)
                        .setNote(noteA)
                        .build();

        AlcoholConsumptionRecord recordB =
                new AlcoholConsumptionRecord.Builder(metadata, time, servingCount, beverageType)
                        .setServingSize(servingSize)
                        .setStartZoneOffset(zoneOffset)
                        .setEndZoneOffset(zoneOffset)
                        .setServingVolume(servingVolume)
                        .setAlcoholByVolume(alcoholByVolume)
                        .setNote(noteB)
                        .build();

        assertThat(recordA).isNotEqualTo(recordB);
        assertThat(recordA.hashCode()).isNotEqualTo(recordB.hashCode());
    }

    @Test
    public void alcoholConsumptionRecordBuilder_interval() {
        Instant startTime = Instant.now().minusSeconds(60);
        Instant endTime = Instant.now();
        int servingCount = 2;
        int beverageType = ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_BEER;
        Metadata metadata = new Metadata.Builder().build();

        AlcoholConsumptionRecord record =
                new AlcoholConsumptionRecord.Builder(
                                metadata, startTime, endTime, servingCount, beverageType)
                        .build();

        assertThat(record.getRecordType()).isEqualTo(RECORD_TYPE_ALCOHOL_CONSUMPTION);
        assertThat(record.getMetadata()).isEqualTo(metadata);
        assertThat(record.getStartTime()).isEqualTo(startTime);
        assertThat(record.getEndTime()).isEqualTo(endTime);
        assertThat(record.getStartZoneOffset()).isEqualTo(getDefaultZoneOffset(startTime));
        assertThat(record.getEndZoneOffset()).isEqualTo(getDefaultZoneOffset(endTime));
        assertThat(record.getServingCount()).isEqualTo(servingCount);
        assertThat(record.getBeverageType()).isEqualTo(beverageType);
    }

    @Test
    public void alcoholConsumptionRecordBuilder_localDate() {
        java.time.LocalDate date = java.time.LocalDate.of(2023, 1, 1);
        int servingCount = 2;
        int beverageType = ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_BEER;
        Metadata metadata = new Metadata.Builder().build();

        AlcoholConsumptionRecord record =
                new AlcoholConsumptionRecord.Builder(metadata, date, servingCount, beverageType)
                        .build();

        assertThat(record.getRecordType()).isEqualTo(RECORD_TYPE_ALCOHOL_CONSUMPTION);
        assertThat(record.getMetadata()).isEqualTo(metadata);
        assertThat(record.getDate()).isEqualTo(date);
        assertThat(record.getServingCount()).isEqualTo(servingCount);
        assertThat(record.getBeverageType()).isEqualTo(beverageType);
    }

    @Test
    public void alcoholConsumptionRecordBuilder_invalidServingVolume() {
        Metadata metadata = new Metadata.Builder().build();
        Instant time = Instant.now();

        AlcoholConsumptionRecord.Builder builder =
                new AlcoholConsumptionRecord.Builder(
                                metadata,
                                time,
                                time.plusSeconds(1),
                                1,
                                ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_OTHER)
                        .setServingVolume(Volume.fromLiters(0.0));

        assertThrows(IllegalArgumentException.class, builder::build);
    }

    private static ZoneOffset getDefaultZoneOffset(Instant instant) {
        return ZoneOffset.systemDefault().getRules().getOffset(instant);
    }
}
