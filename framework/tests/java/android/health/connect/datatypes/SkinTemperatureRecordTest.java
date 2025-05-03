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

package android.health.connect.datatypes;

import static android.health.connect.datatypes.Device.DEVICE_TYPE_WATCH;
import static android.health.connect.datatypes.Metadata.RECORDING_METHOD_MANUAL_ENTRY;
import static android.health.connect.datatypes.SkinTemperatureRecord.MEASUREMENT_LOCATION_FINGER;

import static com.google.common.truth.Truth.assertThat;

import android.health.connect.datatypes.SkinTemperatureRecord.Delta;
import android.health.connect.datatypes.units.Temperature;
import android.health.connect.datatypes.units.TemperatureDelta;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.healthfitness.flags.Flags;

import com.google.common.testing.EqualsTester;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@RunWith(AndroidJUnit4.class)
public class SkinTemperatureRecordTest {
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Test
    public void testEqualsHashcode() {
        Metadata emptyMetadata = new Metadata.Builder().build();
        UUID uuid = UUID.randomUUID();
        Metadata fullMetadata =
                new Metadata.Builder()
                        .setId(uuid.toString())
                        .setClientRecordId("client-record-id")
                        .setClientRecordVersion(567)
                        .setDevice(
                                new Device.Builder()
                                        .setType(DEVICE_TYPE_WATCH)
                                        .setModel("model")
                                        .setManufacturer("manufacturer")
                                        .build())
                        .setDataOrigin(
                                new DataOrigin.Builder().setPackageName("package.name").build())
                        .setRecordingMethod(RECORDING_METHOD_MANUAL_ENTRY)
                        .setLastModifiedTime(Instant.ofEpochMilli(9012345))
                        .build();
        Instant start = Instant.ofEpochMilli(1_000_000_000);
        Instant midTime = Instant.ofEpochMilli(1_500_000_000);
        Instant end = Instant.ofEpochMilli(2_000_000_000);

        // Use strange offsets so they don't match the local offset for the test runner by accident.
        ZoneOffset startOffset = ZoneOffset.ofHoursMinutes(1, 23);
        ZoneOffset endOffset = ZoneOffset.ofHoursMinutes(-2, -49);
        List<Delta> oneSample = List.of(new Delta(TemperatureDelta.fromCelsius(1.2), midTime));
        Temperature baseline = Temperature.fromCelsius(36.4);
        new EqualsTester()
                .addEqualityGroup(
                        new SkinTemperatureRecord.Builder(emptyMetadata, start, end)
                                .setMeasurementLocation(MEASUREMENT_LOCATION_FINGER)
                                .setBaseline(baseline)
                                .setDeltas(oneSample)
                                .build(),
                        new SkinTemperatureRecord.Builder(emptyMetadata, start, end)
                                .setMeasurementLocation(MEASUREMENT_LOCATION_FINGER)
                                .setBaseline(baseline)
                                .setDeltas(oneSample)
                                .build())
                .addEqualityGroup(
                        new SkinTemperatureRecord.Builder(emptyMetadata, start, end)
                                .setMeasurementLocation(MEASUREMENT_LOCATION_FINGER)
                                .setBaseline(Temperature.fromCelsius(39.1))
                                .setDeltas(oneSample)
                                .build(),
                        new SkinTemperatureRecord.Builder(emptyMetadata, start, end)
                                .setMeasurementLocation(MEASUREMENT_LOCATION_FINGER)
                                .setBaseline(Temperature.fromCelsius(39.1))
                                .setDeltas(oneSample)
                                .build())
                .addEqualityGroup(
                        new SkinTemperatureRecord.Builder(emptyMetadata, start, end)
                                .setMeasurementLocation(MEASUREMENT_LOCATION_FINGER)
                                .setBaseline(baseline)
                                .setDeltas(oneSample)
                                .setStartZoneOffset(startOffset)
                                .build(),
                        new SkinTemperatureRecord.Builder(emptyMetadata, start, end)
                                .setMeasurementLocation(MEASUREMENT_LOCATION_FINGER)
                                .setBaseline(baseline)
                                .setDeltas(oneSample)
                                .setStartZoneOffset(startOffset)
                                .build())
                .addEqualityGroup(
                        new SkinTemperatureRecord.Builder(emptyMetadata, start, end)
                                .setMeasurementLocation(MEASUREMENT_LOCATION_FINGER)
                                .setBaseline(baseline)
                                .setDeltas(oneSample)
                                .setStartZoneOffset(startOffset)
                                .setEndZoneOffset(endOffset)
                                .build(),
                        new SkinTemperatureRecord.Builder(emptyMetadata, start, end)
                                .setMeasurementLocation(MEASUREMENT_LOCATION_FINGER)
                                .setBaseline(baseline)
                                .setDeltas(oneSample)
                                .setStartZoneOffset(startOffset)
                                .setEndZoneOffset(endOffset)
                                .build())
                .addEqualityGroup(
                        new SkinTemperatureRecord.Builder(fullMetadata, start, end)
                                .setMeasurementLocation(MEASUREMENT_LOCATION_FINGER)
                                .setBaseline(baseline)
                                .setDeltas(oneSample)
                                .build(),
                        new SkinTemperatureRecord.Builder(fullMetadata, start, end)
                                .setMeasurementLocation(MEASUREMENT_LOCATION_FINGER)
                                .setBaseline(baseline)
                                .setDeltas(oneSample)
                                .build())
                .addEqualityGroup(
                        new SkinTemperatureRecord.Builder(fullMetadata, midTime, end)
                                .setMeasurementLocation(MEASUREMENT_LOCATION_FINGER)
                                .setBaseline(baseline)
                                .setDeltas(oneSample)
                                .build(),
                        new SkinTemperatureRecord.Builder(fullMetadata, midTime, end)
                                .setMeasurementLocation(MEASUREMENT_LOCATION_FINGER)
                                .setBaseline(baseline)
                                .setDeltas(oneSample)
                                .build())
                .addEqualityGroup(
                        new SkinTemperatureRecord.Builder(fullMetadata, start, midTime)
                                .setMeasurementLocation(MEASUREMENT_LOCATION_FINGER)
                                .setBaseline(baseline)
                                .setDeltas(oneSample)
                                .build(),
                        new SkinTemperatureRecord.Builder(fullMetadata, start, midTime)
                                .setMeasurementLocation(MEASUREMENT_LOCATION_FINGER)
                                .setBaseline(baseline)
                                .setDeltas(oneSample)
                                .build())
                .addEqualityGroup(
                        new SkinTemperatureRecord.Builder(fullMetadata, start, end)
                                .setMeasurementLocation(MEASUREMENT_LOCATION_FINGER)
                                .setBaseline(baseline)
                                .setDeltas(
                                        List.of(
                                                new Delta(
                                                        TemperatureDelta.fromCelsius(-1.5),
                                                        midTime)))
                                .build(),
                        new SkinTemperatureRecord.Builder(fullMetadata, start, end)
                                .setMeasurementLocation(MEASUREMENT_LOCATION_FINGER)
                                .setBaseline(baseline)
                                .setDeltas(
                                        List.of(
                                                new Delta(
                                                        TemperatureDelta.fromCelsius(-1.5),
                                                        midTime)))
                                .build())
                .addEqualityGroup(
                        new SkinTemperatureRecord.Builder(fullMetadata, start, end)
                                .setMeasurementLocation(MEASUREMENT_LOCATION_FINGER)
                                .setBaseline(baseline)
                                .setDeltas(
                                        List.of(
                                                new Delta(
                                                        TemperatureDelta.fromCelsius(-1.1), start),
                                                new Delta(
                                                        TemperatureDelta.fromCelsius(1.2),
                                                        midTime)))
                                .build(),
                        new SkinTemperatureRecord.Builder(fullMetadata, start, end)
                                .setMeasurementLocation(MEASUREMENT_LOCATION_FINGER)
                                .setBaseline(baseline)
                                .setDeltas(
                                        List.of(
                                                new Delta(
                                                        TemperatureDelta.fromCelsius(-1.1), start),
                                                new Delta(
                                                        TemperatureDelta.fromCelsius(1.2),
                                                        midTime)))
                                .build())
                .testEquals();
    }

    @Test
    public void testDeltaEqualsHashcode() {
        Instant time1 = Instant.ofEpochMilli(1_000_000_000);
        Instant time2 = Instant.ofEpochMilli(1_500_000_000);
        new EqualsTester()
                .addEqualityGroup(
                        new Delta(TemperatureDelta.fromCelsius(-1.1), time1),
                        new Delta(TemperatureDelta.fromCelsius(-1.1), time1))
                .addEqualityGroup(
                        new Delta(TemperatureDelta.fromCelsius(-1.1), time2),
                        new Delta(TemperatureDelta.fromCelsius(-1.1), time2))
                .addEqualityGroup(
                        new Delta(TemperatureDelta.fromCelsius(1.2), time1),
                        new Delta(TemperatureDelta.fromCelsius(1.2), time1))
                .testEquals();
    }

    @Test
    @EnableFlags(Flags.FLAG_SAMPLE_TIME_ORDERING)
    public void testSamplesConstructedInTimeOrder() {
        // Construct a set of samples such that they are unlikely to be sorted by chance.
        ArrayList<Delta> deltas = new ArrayList<>();
        TemperatureDelta temperatureDelta = TemperatureDelta.fromCelsius(-1.1);
        for (long i = 20L; i < 25L; i++) {
            deltas.add(new Delta(temperatureDelta, Instant.ofEpochMilli(i)));
        }
        for (long i = 0L; i < 5L; i++) {
            deltas.add(new Delta(temperatureDelta, Instant.ofEpochMilli(i)));
        }
        for (long i = 1000L; i < 1005L; i++) {
            deltas.add(new Delta(temperatureDelta, Instant.ofEpochMilli(i)));
        }
        Metadata emptyMetadata = new Metadata.Builder().build();
        Instant start = Instant.ofEpochMilli(0);
        Instant end = Instant.ofEpochMilli(2_000_000_000);

        SkinTemperatureRecord record =
                new SkinTemperatureRecord.Builder(emptyMetadata, start, end)
                        .setBaseline(Temperature.fromCelsius(36.4))
                        .setDeltas(deltas)
                        .build();
        List<Delta> resultSamples = record.getDeltas();

        List<Delta> expected =
                deltas.stream().sorted(Comparator.comparing(Delta::getTime)).toList();
        assertThat(resultSamples).isEqualTo(expected);
    }

    @Test
    @EnableFlags(Flags.FLAG_SAMPLE_TIME_ORDERING)
    public void testSamplesWithDuplicateTimes_dropsDuplicates() {
        // Construct a set of samples such that they are unlikely to be sorted by chance.
        List<Delta> deltas = new ArrayList<>();
        for (int watts = 70; watts < 100; watts++) {
            deltas.add(new Delta(TemperatureDelta.fromCelsius(-1.1), Instant.ofEpochMilli(20)));
        }
        Metadata emptyMetadata = new Metadata.Builder().build();
        Instant start = Instant.ofEpochMilli(0);
        Instant end = Instant.ofEpochMilli(2_000_000_000);
        SkinTemperatureRecord record =
                new SkinTemperatureRecord.Builder(emptyMetadata, start, end)
                        .setBaseline(Temperature.fromCelsius(36.4))
                        .setDeltas(deltas)
                        .build();
        List<Delta> resultSamples = record.getDeltas();

        assertThat(resultSamples).hasSize(1);
    }
}
