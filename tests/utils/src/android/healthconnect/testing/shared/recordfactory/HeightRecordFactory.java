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

import android.health.connect.datatypes.HeightRecord;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.units.Length;
import android.os.Bundle;

import java.time.Instant;
import java.time.ZoneOffset;

/** Note: This class is AI generated, validate before using, and remove this note */
public final class HeightRecordFactory extends RecordFactory<HeightRecord> {
    private static final String KEY_HEIGHT = PREFIX + "HEIGHT";

    @Override
    public HeightRecord newFullRecord(Metadata metadata, Instant time, Instant endTime) {
        return new HeightRecord.Builder(metadata, time, Length.fromMeters(1.7))
                .setZoneOffset(ZoneOffset.ofHours(1))
                .build();
    }

    @Override
    public HeightRecord anotherFullRecord(Metadata metadata, Instant time, Instant endTime) {
        return new HeightRecord.Builder(metadata, time, Length.fromMeters(1.8))
                .setZoneOffset(ZoneOffset.ofHours(2))
                .build();
    }

    @Override
    public HeightRecord newEmptyRecord(Metadata metadata, Instant time, Instant endTime) {
        return new HeightRecord.Builder(metadata, time, Length.fromMeters(1.7)).build();
    }

    @Override
    protected HeightRecord recordWithMetadata(HeightRecord record, Metadata metadata) {
        return new HeightRecord.Builder(metadata, record.getTime(), record.getHeight())
                .setZoneOffset(record.getZoneOffset())
                .build();
    }

    @Override
    protected Bundle getValuesBundleForRecord(HeightRecord record) {
        Bundle values = new Bundle();
        values.putDouble(KEY_HEIGHT, record.getHeight().getInMeters());
        return values;
    }

    @Override
    public HeightRecord newRecordFromValuesBundle(
            Metadata metadata,
            Instant time,
            Instant endTime,
            ZoneOffset zoneOffset,
            ZoneOffset endZoneOffset,
            Bundle bundle) {
        return new HeightRecord.Builder(
                        metadata, time, Length.fromMeters(bundle.getDouble(KEY_HEIGHT)))
                .setZoneOffset(zoneOffset)
                .build();
    }

    @Override
    public String recordToString(HeightRecord record) {
        return "HeightRecord{"
                + "\n\ttime = "
                + record.getTime()
                + ",\n\tzoneOffset = "
                + record.getZoneOffset()
                + ",\n\tmetadata = "
                + metadataToString(record.getMetadata())
                + ",\n\theight = "
                + record.getHeight()
                + "\n}";
    }
}
