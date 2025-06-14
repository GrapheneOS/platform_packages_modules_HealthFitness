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

package android.healthconnect.testing.shared.recordfactory;

import android.health.connect.datatypes.HeartRateRecord;
import android.health.connect.datatypes.Metadata;
import android.os.Bundle;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

public final class HeartRateRecordFactory extends RecordFactory<HeartRateRecord> {

    private static final String KEY_TIMES = PREFIX + "TIMES";
    private static final String KEY_BEATS = PREFIX + "BEATS";

    @Override
    public HeartRateRecord newFullRecord(Metadata metadata, Instant startTime, Instant endTime) {
        return new HeartRateRecord.Builder(
                        metadata,
                        startTime,
                        endTime,
                        Collections.singletonList(
                                new HeartRateRecord.HeartRateSample(80, startTime.plusSeconds(1))))
                .setStartZoneOffset(ZoneOffset.ofHours(3))
                .setEndZoneOffset(ZoneOffset.ofHours(-2))
                .build();
    }

    @Override
    public HeartRateRecord anotherFullRecord(
            Metadata metadata, Instant startTime, Instant endTime) {
        return new HeartRateRecord.Builder(
                        metadata,
                        startTime,
                        endTime,
                        Collections.singletonList(
                                new HeartRateRecord.HeartRateSample(90, startTime.plusSeconds(1))))
                .setStartZoneOffset(ZoneOffset.ofHours(-1))
                .setEndZoneOffset(ZoneOffset.ofHours(2))
                .build();
    }

    @Override
    public HeartRateRecord newEmptyRecord(Metadata metadata, Instant startTime, Instant endTime) {
        return new HeartRateRecord.Builder(
                        metadata,
                        startTime,
                        endTime,
                        Collections.singletonList(
                                new HeartRateRecord.HeartRateSample(80, startTime.plusSeconds(1))))
                .build();
    }

    @Override
    protected HeartRateRecord recordWithMetadata(HeartRateRecord record, Metadata metadata) {
        return new HeartRateRecord.Builder(
                        metadata, record.getStartTime(), record.getEndTime(), record.getSamples())
                .setStartZoneOffset(record.getStartZoneOffset())
                .setEndZoneOffset(record.getEndZoneOffset())
                .build();
    }

    @Override
    protected Bundle getValuesBundleForRecord(HeartRateRecord record) {
        Bundle values = new Bundle();
        long[] times =
                record.getSamples().stream()
                        .map(HeartRateRecord.HeartRateSample::getTime)
                        .mapToLong(Instant::toEpochMilli)
                        .toArray();
        long[] bpm =
                record.getSamples().stream()
                        .mapToLong(HeartRateRecord.HeartRateSample::getBeatsPerMinute)
                        .toArray();

        values.putLongArray(KEY_TIMES, times);
        values.putLongArray(KEY_BEATS, bpm);
        return values;
    }

    @Override
    public HeartRateRecord newRecordFromValuesBundle(
            Metadata metadata,
            Instant startTime,
            Instant endTime,
            ZoneOffset startZoneOffset,
            ZoneOffset endZoneOffset,
            Bundle bundle) {
        long[] times = bundle.getLongArray(KEY_TIMES);
        long[] bpm = bundle.getLongArray(KEY_BEATS);

        List<HeartRateRecord.HeartRateSample> samples =
                IntStream.range(0, times.length)
                        .mapToObj(
                                i ->
                                        new HeartRateRecord.HeartRateSample(
                                                bpm[i], Instant.ofEpochMilli(times[i])))
                        .toList();
        return new HeartRateRecord.Builder(metadata, startTime, endTime, samples)
                .setStartZoneOffset(startZoneOffset)
                .setEndZoneOffset(endZoneOffset)
                .build();
    }
}
