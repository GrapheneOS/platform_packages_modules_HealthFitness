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

package android.healthconnect.testing.shared.recordfactory;

import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.PowerRecord;
import android.health.connect.datatypes.units.Power;
import android.os.Bundle;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

/** Note: This class is AI generated, validate before using, and remove this note */
public final class PowerRecordFactory extends RecordFactory<PowerRecord> {

    private static final String KEY_TIMES = PREFIX + "TIMES";
    private static final String KEY_POWER_IN_WATTS = PREFIX + "POWER_IN_WATTS";

    @Override
    public PowerRecord newFullRecord(Metadata metadata, Instant startTime, Instant endTime) {
        return new PowerRecord.Builder(
                        metadata,
                        startTime,
                        endTime,
                        Collections.singletonList(
                                new PowerRecord.PowerRecordSample(
                                        Power.fromWatts(100.0), startTime.plusSeconds(1))))
                .setStartZoneOffset(ZoneOffset.ofHours(3))
                .setEndZoneOffset(ZoneOffset.ofHours(-2))
                .build();
    }

    @Override
    public PowerRecord anotherFullRecord(Metadata metadata, Instant startTime, Instant endTime) {
        return new PowerRecord.Builder(
                        metadata,
                        startTime,
                        endTime,
                        Collections.singletonList(
                                new PowerRecord.PowerRecordSample(
                                        Power.fromWatts(200.0), startTime.plusSeconds(1))))
                .setStartZoneOffset(ZoneOffset.ofHours(-1))
                .setEndZoneOffset(ZoneOffset.ofHours(2))
                .build();
    }

    @Override
    public PowerRecord newEmptyRecord(Metadata metadata, Instant startTime, Instant endTime) {
        return new PowerRecord.Builder(
                        metadata,
                        startTime,
                        endTime,
                        Collections.singletonList(
                                new PowerRecord.PowerRecordSample(
                                        Power.fromWatts(100.0), startTime.plusSeconds(1))))
                .build();
    }

    @Override
    protected PowerRecord recordWithMetadata(PowerRecord record, Metadata metadata) {
        return new PowerRecord.Builder(
                        metadata, record.getStartTime(), record.getEndTime(), record.getSamples())
                .setStartZoneOffset(record.getStartZoneOffset())
                .setEndZoneOffset(record.getEndZoneOffset())
                .build();
    }

    @Override
    protected Bundle getValuesBundleForRecord(PowerRecord record) {
        Bundle values = new Bundle();
        long[] times =
                record.getSamples().stream()
                        .map(PowerRecord.PowerRecordSample::getTime)
                        .mapToLong(Instant::toEpochMilli)
                        .toArray();
        double[] watts =
                record.getSamples().stream()
                        .mapToDouble((sample) -> sample.getPower().getInWatts())
                        .toArray();

        values.putLongArray(KEY_TIMES, times);
        values.putDoubleArray(KEY_POWER_IN_WATTS, watts);
        return values;
    }

    @Override
    public PowerRecord newRecordFromValuesBundle(
            Metadata metadata,
            Instant startTime,
            Instant endTime,
            ZoneOffset startZoneOffset,
            ZoneOffset endZoneOffset,
            Bundle bundle) {
        long[] times = bundle.getLongArray(KEY_TIMES);
        double[] watts = bundle.getDoubleArray(KEY_POWER_IN_WATTS);
        List<PowerRecord.PowerRecordSample> samples =
                IntStream.range(0, times.length)
                        .mapToObj(
                                i ->
                                        new PowerRecord.PowerRecordSample(
                                                Power.fromWatts(watts[i]),
                                                Instant.ofEpochMilli(times[i])))
                        .toList();
        return new PowerRecord.Builder(metadata, startTime, endTime, samples)
                .setStartZoneOffset(startZoneOffset)
                .setEndZoneOffset(endZoneOffset)
                .build();
    }

    @Override
    public String recordToString(PowerRecord record) {
        return "PowerRecord{"
                + "\n\tstartTime = "
                + record.getStartTime()
                + ",\n\tendTime = "
                + record.getEndTime()
                + ",\n\tstartZoneOffset = "
                + record.getStartZoneOffset()
                + ",\n\tendZoneOffset = "
                + record.getEndZoneOffset()
                + ",\n\tmetadata = "
                + metadataToString(record.getMetadata())
                + ",\n\tsamples = "
                + record.getSamples()
                + "\n}";
    }
}
