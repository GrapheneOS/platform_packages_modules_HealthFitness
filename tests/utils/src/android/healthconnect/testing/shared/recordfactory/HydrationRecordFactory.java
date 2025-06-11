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

import android.health.connect.datatypes.HydrationRecord;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.units.Volume;
import android.os.Bundle;

import java.time.Instant;
import java.time.ZoneOffset;

public final class HydrationRecordFactory extends RecordFactory<HydrationRecord> {
    private static final String KEY_VOLUME = PREFIX + "VOLUME";

    @Override
    public HydrationRecord newFullRecord(Metadata metadata, Instant startTime, Instant endTime) {
        return new HydrationRecord.Builder(metadata, startTime, endTime, Volume.fromLiters(1.0))
                .setStartZoneOffset(ZoneOffset.ofHours(3))
                .setEndZoneOffset(ZoneOffset.ofHours(-2))
                .build();
    }

    @Override
    public HydrationRecord anotherFullRecord(
            Metadata metadata, Instant startTime, Instant endTime) {
        return new HydrationRecord.Builder(metadata, startTime, endTime, Volume.fromLiters(2.0))
                .setStartZoneOffset(ZoneOffset.ofHours(-1))
                .setEndZoneOffset(ZoneOffset.ofHours(2))
                .build();
    }

    @Override
    public HydrationRecord newEmptyRecord(Metadata metadata, Instant startTime, Instant endTime) {
        return new HydrationRecord.Builder(metadata, startTime, endTime, Volume.fromLiters(1.0))
                .build();
    }

    @Override
    protected HydrationRecord recordWithMetadata(HydrationRecord record, Metadata metadata) {
        return new HydrationRecord.Builder(
                        metadata, record.getStartTime(), record.getEndTime(), record.getVolume())
                .setStartZoneOffset(record.getStartZoneOffset())
                .setEndZoneOffset(record.getEndZoneOffset())
                .build();
    }

    @Override
    protected Bundle getValuesBundleForRecord(HydrationRecord record) {
        Bundle values = new Bundle();
        values.putDouble(KEY_VOLUME, record.getVolume().getInLiters());
        return values;
    }

    @Override
    public HydrationRecord newRecordFromValuesBundle(
            Metadata metadata,
            Instant startTime,
            Instant endTime,
            ZoneOffset startZoneOffset,
            ZoneOffset endZoneOffset,
            Bundle bundle) {
        return new HydrationRecord.Builder(
                        metadata,
                        startTime,
                        endTime,
                        Volume.fromLiters(bundle.getDouble(KEY_VOLUME)))
                .setStartZoneOffset(startZoneOffset)
                .setEndZoneOffset(endZoneOffset)
                .build();
    }
}
