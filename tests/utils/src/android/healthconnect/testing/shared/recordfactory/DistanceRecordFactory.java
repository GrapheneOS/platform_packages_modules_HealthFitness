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

import android.health.connect.datatypes.DistanceRecord;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.units.Length;
import android.os.Bundle;

import java.time.Instant;
import java.time.ZoneOffset;

/** Note: This class is AI generated, validate before using, and remove this note */
public final class DistanceRecordFactory extends RecordFactory<DistanceRecord> {
    private static final String KEY_DISTANCE = PREFIX + "DISTANCE";

    @Override
    public DistanceRecord newFullRecord(Metadata metadata, Instant startTime, Instant endTime) {
        return new DistanceRecord.Builder(metadata, startTime, endTime, Length.fromMeters(100))
                .setStartZoneOffset(ZoneOffset.ofHours(3))
                .setEndZoneOffset(ZoneOffset.ofHours(-2))
                .build();
    }

    @Override
    public DistanceRecord anotherFullRecord(Metadata metadata, Instant startTime, Instant endTime) {
        return new DistanceRecord.Builder(metadata, startTime, endTime, Length.fromMeters(200))
                .setStartZoneOffset(ZoneOffset.ofHours(-1))
                .setEndZoneOffset(ZoneOffset.ofHours(2))
                .build();
    }

    @Override
    public DistanceRecord newEmptyRecord(Metadata metadata, Instant startTime, Instant endTime) {
        return new DistanceRecord.Builder(metadata, startTime, endTime, Length.fromMeters(100))
                .build();
    }

    @Override
    protected DistanceRecord recordWithMetadata(DistanceRecord record, Metadata metadata) {
        return new DistanceRecord.Builder(
                        metadata, record.getStartTime(), record.getEndTime(), record.getDistance())
                .setStartZoneOffset(record.getStartZoneOffset())
                .setEndZoneOffset(record.getEndZoneOffset())
                .build();
    }

    @Override
    protected Bundle getValuesBundleForRecord(DistanceRecord record) {
        Bundle values = new Bundle();
        values.putDouble(KEY_DISTANCE, record.getDistance().getInMeters());
        return values;
    }

    @Override
    public DistanceRecord newRecordFromValuesBundle(
            Metadata metadata,
            Instant startTime,
            Instant endTime,
            ZoneOffset startZoneOffset,
            ZoneOffset endZoneOffset,
            Bundle bundle) {
        return new DistanceRecord.Builder(
                        metadata,
                        startTime,
                        endTime,
                        Length.fromMeters(bundle.getDouble(KEY_DISTANCE)))
                .setStartZoneOffset(startZoneOffset)
                .setEndZoneOffset(endZoneOffset)
                .build();
    }

    @Override
    public String recordToString(DistanceRecord record) {
        return "DistanceRecord{"
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
                + ",\n\tdistance = "
                + record.getDistance()
                + "\n}";
    }
}
