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

import static com.google.common.truth.Truth.assertThat;

import android.health.connect.datatypes.PowerRecord.PowerRecordSample;
import android.health.connect.datatypes.units.Power;
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
public class PowerRecordTest {
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
        List<PowerRecordSample> oneSample =
                List.of(new PowerRecordSample(Power.fromWatts(60.2), midTime));
        new EqualsTester()
                .addEqualityGroup(
                        new PowerRecord.Builder(emptyMetadata, start, end, oneSample).build(),
                        new PowerRecord.Builder(emptyMetadata, start, end, oneSample).build())
                .addEqualityGroup(
                        new PowerRecord.Builder(emptyMetadata, start, end, oneSample)
                                .setStartZoneOffset(startOffset)
                                .build(),
                        new PowerRecord.Builder(emptyMetadata, start, end, oneSample)
                                .setStartZoneOffset(startOffset)
                                .build())
                .addEqualityGroup(
                        new PowerRecord.Builder(emptyMetadata, start, end, oneSample)
                                .setStartZoneOffset(startOffset)
                                .setEndZoneOffset(endOffset)
                                .build(),
                        new PowerRecord.Builder(emptyMetadata, start, end, oneSample)
                                .setStartZoneOffset(startOffset)
                                .setEndZoneOffset(endOffset)
                                .build())
                .addEqualityGroup(
                        new PowerRecord.Builder(fullMetadata, start, end, oneSample).build(),
                        new PowerRecord.Builder(fullMetadata, start, end, oneSample).build())
                .addEqualityGroup(
                        new PowerRecord.Builder(fullMetadata, midTime, end, oneSample).build(),
                        new PowerRecord.Builder(fullMetadata, midTime, end, oneSample).build())
                .addEqualityGroup(
                        new PowerRecord.Builder(fullMetadata, start, midTime, oneSample).build(),
                        new PowerRecord.Builder(fullMetadata, start, midTime, oneSample).build())
                .addEqualityGroup(
                        new PowerRecord.Builder(
                                        fullMetadata,
                                        start,
                                        end,
                                        List.of(
                                                new PowerRecordSample(
                                                        Power.fromWatts(70.1), midTime)))
                                .build(),
                        new PowerRecord.Builder(
                                        fullMetadata,
                                        start,
                                        end,
                                        List.of(
                                                new PowerRecordSample(
                                                        Power.fromWatts(70.1), midTime)))
                                .build())
                .addEqualityGroup(
                        new PowerRecord.Builder(
                                        fullMetadata,
                                        start,
                                        end,
                                        List.of(
                                                new PowerRecordSample(Power.fromWatts(70.1), start),
                                                new PowerRecordSample(
                                                        Power.fromWatts(80.1), midTime)))
                                .build(),
                        new PowerRecord.Builder(
                                        fullMetadata,
                                        start,
                                        end,
                                        List.of(
                                                new PowerRecordSample(Power.fromWatts(70.1), start),
                                                new PowerRecordSample(
                                                        Power.fromWatts(80.1), midTime)))
                                .build())
                .testEquals();
    }

    @Test
    public void testSampleEqualsHashcode() {
        Instant time1 = Instant.ofEpochMilli(1_000_000_000);
        Instant time2 = Instant.ofEpochMilli(1_500_000_000);
        new EqualsTester()
                .addEqualityGroup(
                        new PowerRecordSample(Power.fromWatts(60.1), time1),
                        new PowerRecordSample(Power.fromWatts(60.1), time1))
                .addEqualityGroup(
                        new PowerRecordSample(Power.fromWatts(60.1), time2),
                        new PowerRecordSample(Power.fromWatts(60.1), time2))
                .addEqualityGroup(
                        new PowerRecordSample(Power.fromWatts(70.1), time1),
                        new PowerRecordSample(Power.fromWatts(70.1), time1))
                .testEquals();
    }

    @Test
    @EnableFlags(Flags.FLAG_SAMPLE_TIME_ORDERING)
    public void testSamplesConstructedInTimeOrder() {
        // Construct a set of samples such that they are unlikely to be sorted by chance.
        ArrayList<PowerRecordSample> samples = new ArrayList<>();
        Power power = Power.fromWatts(100);
        for (long i = 20L; i < 25L; i++) {
            samples.add(new PowerRecordSample(power, Instant.ofEpochMilli(i)));
        }
        for (long i = 0L; i < 5L; i++) {
            samples.add(new PowerRecordSample(power, Instant.ofEpochMilli(i)));
        }
        for (long i = 1000L; i < 1005L; i++) {
            samples.add(new PowerRecordSample(power, Instant.ofEpochMilli(i)));
        }
        Metadata emptyMetadata = new Metadata.Builder().build();
        Instant start = Instant.ofEpochMilli(0);
        Instant end = Instant.ofEpochMilli(2_000_000_000);

        PowerRecord record = new PowerRecord.Builder(emptyMetadata, start, end, samples).build();
        List<PowerRecordSample> resultSamples = record.getSamples();

        List<PowerRecordSample> expected =
                samples.stream().sorted(Comparator.comparing(PowerRecordSample::getTime)).toList();
        assertThat(resultSamples).isEqualTo(expected);
    }

    @Test
    @EnableFlags(Flags.FLAG_SAMPLE_TIME_ORDERING)
    public void testSamplesWithDuplicateTimes_dropsDuplicates() {
        // Construct a set of samples such that they are unlikely to be sorted by chance.
        List<PowerRecordSample> samples = new ArrayList<>();
        for (int watts = 70; watts < 100; watts++) {
            samples.add(new PowerRecordSample(Power.fromWatts(watts), Instant.ofEpochMilli(20)));
        }
        Metadata emptyMetadata = new Metadata.Builder().build();
        Instant start = Instant.ofEpochMilli(0);
        Instant end = Instant.ofEpochMilli(2_000_000_000);
        PowerRecord record = new PowerRecord.Builder(emptyMetadata, start, end, samples).build();
        List<PowerRecordSample> resultSamples = record.getSamples();

        assertThat(resultSamples).hasSize(1);
    }
}
