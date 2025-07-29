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
import static android.health.connect.datatypes.AlcoholConsumptionRecord.RECORD_TEMPORAL_TYPE_INSTANT;
import static android.health.connect.datatypes.AlcoholConsumptionRecord.RECORD_TEMPORAL_TYPE_INTERVAL;
import static android.health.connect.datatypes.AlcoholConsumptionRecord.RECORD_TEMPORAL_TYPE_LOCAL_DATE;
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
import java.time.LocalDate;
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
    public void builder_allFieldsSet() {
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
                        .setLastModifiedTime(Instant.now())
                        .build();
        AlcoholConsumptionRecord record = getFullRecordBuilder(metadata).build();

        assertThat(record.getRecordType()).isEqualTo(RECORD_TYPE_ALCOHOL_CONSUMPTION);
        assertThat(record.getMetadata()).isEqualTo(metadata);
        assertThat(record.getServingCount()).isEqualTo(2);
        assertThat(record.getBeverageType()).isEqualTo(ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_BEER);
        assertThat(record.getServingSize()).isEqualTo(ALCOHOL_CONSUMPTION_SERVING_SIZE_PINT);
        assertThat(record.getServingVolume()).isEqualTo(Volume.fromLiters(0.568));
        assertThat(record.getAlcoholByVolume()).isEqualTo(Percentage.fromValue(7));
        assertThat(record.getNote()).isEqualTo("Pub Crawl");
        assertThat(record.getTemporalType()).isEqualTo(RECORD_TEMPORAL_TYPE_INTERVAL);
    }

    @Test
    public void builder_optionalFieldsNotSet() {
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
        assertThat(record.getTemporalType()).isEqualTo(RECORD_TEMPORAL_TYPE_INTERVAL);
    }

    @Test
    public void builder_invalidType() {
        Metadata metadata = new Metadata.Builder().build();
        Instant time = Instant.now();

        assertThrows(
                IllegalArgumentException.class,
                () -> new AlcoholConsumptionRecord.Builder(metadata, time, 1, 505).build());
    }

    @Test
    public void builder_invalidServingSize() {
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
        AlcoholConsumptionRecord recordA = getFullRecordBuilder().build();
        AlcoholConsumptionRecord recordB = getFullRecordBuilder().build();

        assertThat(recordA).isEqualTo(recordB);
        assertThat(recordA.hashCode()).isEqualTo(recordB.hashCode());
    }

    @Test
    public void equals_hashCode_metadataNotEqual_recordsNotEqual() {
        Metadata metadataA = new Metadata.Builder().setId("id-a").build();
        Metadata metadataB = new Metadata.Builder().setId("id-b").build();
        AlcoholConsumptionRecord recordA = getFullRecordBuilder(metadataA).build();
        AlcoholConsumptionRecord recordB = getFullRecordBuilder(metadataB).build();

        assertThat(recordA).isNotEqualTo(recordB);
        assertThat(recordA.hashCode()).isNotEqualTo(recordB.hashCode());
    }

    @Test
    public void equals_hashCode_servingCountNotEqual_recordsNotEqual() {
        AlcoholConsumptionRecord recordA = getFullRecordBuilder().setServingCount(2).build();
        AlcoholConsumptionRecord recordB = getFullRecordBuilder().setServingCount(20).build();

        assertThat(recordA).isNotEqualTo(recordB);
        assertThat(recordA.hashCode()).isNotEqualTo(recordB.hashCode());
    }

    @Test
    public void equals_hashCode_alcoholTypeNotEqual_recordsNotEqual() {
        AlcoholConsumptionRecord recordA =
                getFullRecordBuilder()
                        .setBeverageType(ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_BEER)
                        .build();
        AlcoholConsumptionRecord recordB =
                getFullRecordBuilder()
                        .setBeverageType(ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_WINE)
                        .build();

        assertThat(recordA).isNotEqualTo(recordB);
        assertThat(recordA.hashCode()).isNotEqualTo(recordB.hashCode());
    }

    @Test
    public void equals_hashCode_servingSizeNotEqual_recordsNotEqual() {
        AlcoholConsumptionRecord recordA =
                getFullRecordBuilder()
                        .setServingSize(ALCOHOL_CONSUMPTION_SERVING_SIZE_PINT)
                        .build();
        AlcoholConsumptionRecord recordB =
                getFullRecordBuilder()
                        .setServingSize(ALCOHOL_CONSUMPTION_SERVING_SIZE_HALF_PINT)
                        .build();

        assertThat(recordA).isNotEqualTo(recordB);
        assertThat(recordA.hashCode()).isNotEqualTo(recordB.hashCode());
    }

    @Test
    public void equals_hashCode_servingVolumeNotEqual_recordsNotEqual() {
        AlcoholConsumptionRecord recordA =
                getFullRecordBuilder().setServingVolume(Volume.fromLiters(0.568)).build();
        AlcoholConsumptionRecord recordB =
                getFullRecordBuilder().setServingVolume(Volume.fromLiters(0.252)).build();

        assertThat(recordA).isNotEqualTo(recordB);
        assertThat(recordA.hashCode()).isNotEqualTo(recordB.hashCode());
    }

    @Test
    public void equals_hashCode_alcoholByVolumeNotEqual_recordsNotEqual() {
        AlcoholConsumptionRecord recordA =
                getFullRecordBuilder().setAlcoholByVolume(Percentage.fromValue(7)).build();
        AlcoholConsumptionRecord recordB =
                getFullRecordBuilder().setAlcoholByVolume(Percentage.fromValue(12)).build();

        assertThat(recordA).isNotEqualTo(recordB);
        assertThat(recordA.hashCode()).isNotEqualTo(recordB.hashCode());
    }

    @Test
    public void equals_hashCode_noteNotEqual_recordsNotEqual() {
        AlcoholConsumptionRecord recordA = getFullRecordBuilder().setNote("Pub Crawl").build();
        AlcoholConsumptionRecord recordB = getFullRecordBuilder().setNote("Otley Run").build();

        assertThat(recordA).isNotEqualTo(recordB);
        assertThat(recordA.hashCode()).isNotEqualTo(recordB.hashCode());
    }

    @Test
    public void builder_instantaneousRecord() {
        Instant time = Instant.now();
        int servingCount = 2;
        int beverageType = ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_BEER;
        Metadata metadata = new Metadata.Builder().build();

        AlcoholConsumptionRecord record =
                new AlcoholConsumptionRecord.Builder(metadata, time, servingCount, beverageType)
                        .build();

        assertThat(record.getRecordType()).isEqualTo(RECORD_TYPE_ALCOHOL_CONSUMPTION);
        assertThat(record.getMetadata()).isEqualTo(metadata);
        assertThat(record.getStartTime()).isEqualTo(time);
        assertThat(record.getEndTime()).isEqualTo(time);
        assertThat(record.getStartZoneOffset()).isEqualTo(getDefaultZoneOffset(time));
        assertThat(record.getEndZoneOffset()).isEqualTo(getDefaultZoneOffset(time));
        assertThat(record.getServingCount()).isEqualTo(servingCount);
        assertThat(record.getBeverageType()).isEqualTo(beverageType);
        assertThat(record.getTemporalType()).isEqualTo(RECORD_TEMPORAL_TYPE_INSTANT);
    }

    @Test
    public void builder_intervalRecord() {
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
        assertThat(record.getTemporalType()).isEqualTo(RECORD_TEMPORAL_TYPE_INTERVAL);
    }

    @Test
    public void builder_localDateRecord() {
        LocalDate date = LocalDate.of(2023, 1, 1);
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
        assertThat(record.getTemporalType()).isEqualTo(RECORD_TEMPORAL_TYPE_LOCAL_DATE);
    }

    @Test
    public void builder_invalidServingVolume() {
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

    private AlcoholConsumptionRecord.Builder getFullRecordBuilder() {
        return getFullRecordBuilder(new Metadata.Builder().build());
    }

    private AlcoholConsumptionRecord.Builder getFullRecordBuilder(Metadata metadata) {
        Instant startTime = Instant.now().minusSeconds(60);
        Instant endTime = Instant.now();
        return new AlcoholConsumptionRecord.Builder(
                        metadata,
                        startTime,
                        endTime,
                        /* servingCount= */ 2,
                        ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_BEER)
                .setServingSize(ALCOHOL_CONSUMPTION_SERVING_SIZE_PINT)
                .setStartZoneOffset(ZoneOffset.ofHours(2))
                .setEndZoneOffset(ZoneOffset.ofHours(2))
                .setServingVolume(Volume.fromLiters(0.568))
                .setAlcoholByVolume(Percentage.fromValue(7.0))
                .setNote("Pub Crawl");
    }
}
