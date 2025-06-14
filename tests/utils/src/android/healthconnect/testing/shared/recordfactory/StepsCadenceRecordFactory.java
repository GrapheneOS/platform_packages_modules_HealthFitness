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
import android.health.connect.datatypes.StepsCadenceRecord;
import android.os.Bundle;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

/** Note: This class is AI generated, validate before using, and remove this note */
public final class StepsCadenceRecordFactory extends RecordFactory<StepsCadenceRecord> {

    private static final String KEY_TIMES = PREFIX + "TIMES";
    private static final String KEY_RATE = PREFIX + "RATE";

    @Override
    public StepsCadenceRecord newFullRecord(Metadata metadata, Instant startTime, Instant endTime) {
        return new StepsCadenceRecord.Builder(
                        metadata,
                        startTime,
                        endTime,
                        Collections.singletonList(
                                new StepsCadenceRecord.StepsCadenceRecordSample(
                                        100.0, startTime.plusSeconds(1))))
                .setStartZoneOffset(ZoneOffset.ofHours(3))
                .setEndZoneOffset(ZoneOffset.ofHours(-2))
                .build();
    }

    @Override
    public StepsCadenceRecord anotherFullRecord(
            Metadata metadata, Instant startTime, Instant endTime) {
        return new StepsCadenceRecord.Builder(
                        metadata,
                        startTime,
                        endTime,
                        Collections.singletonList(
                                new StepsCadenceRecord.StepsCadenceRecordSample(
                                        120.0, startTime.plusSeconds(1))))
                .setStartZoneOffset(ZoneOffset.ofHours(-1))
                .setEndZoneOffset(ZoneOffset.ofHours(2))
                .build();
    }

    @Override
    public StepsCadenceRecord newEmptyRecord(
            Metadata metadata, Instant startTime, Instant endTime) {
        return new StepsCadenceRecord.Builder(
                        metadata,
                        startTime,
                        endTime,
                        Collections.singletonList(
                                new StepsCadenceRecord.StepsCadenceRecordSample(
                                        100.0, startTime.plusSeconds(1))))
                .build();
    }

    @Override
    protected StepsCadenceRecord recordWithMetadata(StepsCadenceRecord record, Metadata metadata) {
        return new StepsCadenceRecord.Builder(
                        metadata, record.getStartTime(), record.getEndTime(), record.getSamples())
                .setStartZoneOffset(record.getStartZoneOffset())
                .setEndZoneOffset(record.getEndZoneOffset())
                .build();
    }

    @Override
    protected Bundle getValuesBundleForRecord(StepsCadenceRecord record) {
        Bundle values = new Bundle();
        long[] times =
                record.getSamples().stream()
                        .map(StepsCadenceRecord.StepsCadenceRecordSample::getTime)
                        .mapToLong(Instant::toEpochMilli)
                        .toArray();
        double[] rate =
                record.getSamples().stream()
                        .mapToDouble(StepsCadenceRecord.StepsCadenceRecordSample::getRate)
                        .toArray();

        values.putLongArray(KEY_TIMES, times);
        values.putDoubleArray(KEY_RATE, rate);
        return values;
    }

    @Override
    public StepsCadenceRecord newRecordFromValuesBundle(
            Metadata metadata,
            Instant startTime,
            Instant endTime,
            ZoneOffset startZoneOffset,
            ZoneOffset endZoneOffset,
            Bundle bundle) {
        long[] times = bundle.getLongArray(KEY_TIMES);
        double[] rate = bundle.getDoubleArray(KEY_RATE);
        List<StepsCadenceRecord.StepsCadenceRecordSample> samples =
                IntStream.range(0, times.length)
                        .mapToObj(
                                i ->
                                        new StepsCadenceRecord.StepsCadenceRecordSample(
                                                rate[i], Instant.ofEpochMilli(times[i])))
                        .toList();
        return new StepsCadenceRecord.Builder(metadata, startTime, endTime, samples)
                .setStartZoneOffset(startZoneOffset)
                .setEndZoneOffset(endZoneOffset)
                .build();
    }
}
