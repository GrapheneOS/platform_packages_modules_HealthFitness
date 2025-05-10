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

import android.health.connect.datatypes.ActivityIntensityRecord;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.StepsRecord;
import android.os.Bundle;

import androidx.annotation.Nullable;

import java.time.Instant;
import java.time.ZoneOffset;

public final class StepsRecordFactory extends RecordFactory<StepsRecord> {

    private static final String KEY_COUNT = PREFIX + "COUNT";

    @Override
    public StepsRecord newFullRecord(Metadata metadata, Instant startTime, Instant endTime) {
        return new StepsRecord.Builder(metadata, startTime, endTime, 20)
                .setStartZoneOffset(ZoneOffset.ofHours(3))
                .setEndZoneOffset(ZoneOffset.ofHours(-2))
                .build();
    }

    @Override
    public StepsRecord anotherFullRecord(Metadata metadata, Instant startTime, Instant endTime) {
        return new StepsRecord.Builder(metadata, startTime, endTime, 50)
                .setStartZoneOffset(ZoneOffset.ofHours(-1))
                .setEndZoneOffset(ZoneOffset.ofHours(2))
                .build();
    }

    @Override
    public StepsRecord newEmptyRecord(Metadata metadata, Instant startTime, Instant endTime) {
        return new StepsRecord.Builder(metadata, startTime, endTime, 10).build();
    }

    @Override
    protected StepsRecord recordWithMetadata(StepsRecord record, Metadata metadata) {
        return new StepsRecord.Builder(
                        metadata, record.getStartTime(), record.getEndTime(), record.getCount())
                .setStartZoneOffset(record.getStartZoneOffset())
                .setEndZoneOffset(record.getEndZoneOffset())
                .build();
    }

    @Override
    protected Bundle getValuesBundleForRecord(StepsRecord record) {
        Bundle values = new Bundle();
        values.putLong(KEY_COUNT, record.getCount());
        return values;
    }

    @Override
    public StepsRecord newRecordFromValuesBundle(
            Metadata metadata,
            Instant startTime,
            Instant endTime,
            ZoneOffset startZoneOffset,
            ZoneOffset endZoneOffset,
            Bundle bundle) {
        long count = bundle.getLong(KEY_COUNT);
        return new StepsRecord.Builder(metadata, startTime, endTime, count)
                .setStartZoneOffset(startZoneOffset)
                .setEndZoneOffset(endZoneOffset)
                .build();
    }

    /** Creates a new {@link ActivityIntensityRecord} with empty metadata. */
    public StepsRecord newRecord(Instant startTime, Instant endTime, int type) {
        return newRecord(startTime, endTime, type, null, null);
    }

    /** Creates a new {@link ActivityIntensityRecord} with empty metadata. */
    public StepsRecord newRecord(
            Instant startTime,
            Instant endTime,
            long count,
            @Nullable ZoneOffset startZoneOffset,
            @Nullable ZoneOffset endZoneOffset) {
        var builder = new StepsRecord.Builder(newEmptyMetadata(), startTime, endTime, count);

        if (startZoneOffset != null) {
            builder.setStartZoneOffset(startZoneOffset);
        }

        if (endZoneOffset != null) {
            builder.setEndZoneOffset(endZoneOffset);
        }

        return builder.build();
    }
}
