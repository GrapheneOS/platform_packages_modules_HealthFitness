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
import android.health.connect.datatypes.SkinTemperatureRecord;
import android.health.connect.datatypes.units.Temperature;
import android.health.connect.datatypes.units.TemperatureDelta;
import android.os.Bundle;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

/** Note: This class is AI generated, validate before using, and remove this note */
public final class SkinTemperatureRecordFactory extends RecordFactory<SkinTemperatureRecord> {

    private static final String KEY_TIMES = PREFIX + "TIMES";
    private static final String KEY_DELTAS = PREFIX + "DELTAS";

    @Override
    public SkinTemperatureRecord newFullRecord(
            Metadata metadata, Instant startTime, Instant endTime) {
        return new SkinTemperatureRecord.Builder(metadata, startTime, endTime)
                .setBaseline(Temperature.fromCelsius(36.0))
                .setDeltas(
                        Collections.singletonList(
                                new SkinTemperatureRecord.Delta(
                                        TemperatureDelta.fromCelsius(1.0),
                                        startTime.plusSeconds(1))))
                .setStartZoneOffset(ZoneOffset.ofHours(3))
                .setEndZoneOffset(ZoneOffset.ofHours(-2))
                .build();
    }

    @Override
    public SkinTemperatureRecord anotherFullRecord(
            Metadata metadata, Instant startTime, Instant endTime) {
        return new SkinTemperatureRecord.Builder(metadata, startTime, endTime)
                .setBaseline(Temperature.fromCelsius(37.0))
                .setDeltas(
                        Collections.singletonList(
                                new SkinTemperatureRecord.Delta(
                                        TemperatureDelta.fromCelsius(2.0),
                                        startTime.plusSeconds(1))))
                .setStartZoneOffset(ZoneOffset.ofHours(-1))
                .setEndZoneOffset(ZoneOffset.ofHours(2))
                .build();
    }

    @Override
    public SkinTemperatureRecord newEmptyRecord(
            Metadata metadata, Instant startTime, Instant endTime) {
        return new SkinTemperatureRecord.Builder(metadata, startTime, endTime).build();
    }

    @Override
    protected SkinTemperatureRecord recordWithMetadata(
            SkinTemperatureRecord record, Metadata metadata) {
        return new SkinTemperatureRecord.Builder(
                        metadata, record.getStartTime(), record.getEndTime())
                .setBaseline(record.getBaseline())
                .setDeltas(record.getDeltas())
                .setMeasurementLocation(record.getMeasurementLocation())
                .setStartZoneOffset(record.getStartZoneOffset())
                .setEndZoneOffset(record.getEndZoneOffset())
                .build();
    }

    @Override
    protected Bundle getValuesBundleForRecord(SkinTemperatureRecord record) {
        Bundle values = new Bundle();
        long[] times =
                record.getDeltas().stream()
                        .map(SkinTemperatureRecord.Delta::getTime)
                        .mapToLong(Instant::toEpochMilli)
                        .toArray();
        double[] deltas =
                record.getDeltas().stream()
                        .mapToDouble((delta) -> delta.getDelta().getInCelsius())
                        .toArray();

        values.putLongArray(KEY_TIMES, times);
        values.putDoubleArray(KEY_DELTAS, deltas);
        return values;
    }

    @Override
    public SkinTemperatureRecord newRecordFromValuesBundle(
            Metadata metadata,
            Instant startTime,
            Instant endTime,
            ZoneOffset startZoneOffset,
            ZoneOffset endZoneOffset,
            Bundle bundle) {
        long[] times = bundle.getLongArray(KEY_TIMES);
        double[] deltas = bundle.getDoubleArray(KEY_DELTAS);
        List<SkinTemperatureRecord.Delta> samples =
                IntStream.range(0, times.length)
                        .mapToObj(
                                i ->
                                        new SkinTemperatureRecord.Delta(
                                                TemperatureDelta.fromCelsius(deltas[i]),
                                                Instant.ofEpochMilli(times[i])))
                        .toList();
        return new SkinTemperatureRecord.Builder(metadata, startTime, endTime)
                .setDeltas(samples)
                .setStartZoneOffset(startZoneOffset)
                .setEndZoneOffset(endZoneOffset)
                .build();
    }

    @Override
    public String recordToString(SkinTemperatureRecord record) {
        return "SkinTemperatureRecord{"
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
                + ",\n\tbaseline = "
                + record.getBaseline()
                + ",\n\tdeltas = "
                + record.getDeltas()
                + ",\n\tmeasurementLocation = "
                + record.getMeasurementLocation()
                + "\n}";
    }
}
