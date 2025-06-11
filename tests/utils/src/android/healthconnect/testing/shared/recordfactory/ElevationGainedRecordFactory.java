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

import android.health.connect.datatypes.ElevationGainedRecord;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.units.Length;
import android.os.Bundle;

import java.time.Instant;
import java.time.ZoneOffset;

/** Note: This class is AI generated, validate before using, and remove this note */
public final class ElevationGainedRecordFactory extends RecordFactory<ElevationGainedRecord> {
    private static final String KEY_ELEVATION = PREFIX + "ELEVATION";

    @Override
    public ElevationGainedRecord newFullRecord(
            Metadata metadata, Instant startTime, Instant endTime) {
        return new ElevationGainedRecord.Builder(
                        metadata, startTime, endTime, Length.fromMeters(100))
                .setStartZoneOffset(ZoneOffset.ofHours(3))
                .setEndZoneOffset(ZoneOffset.ofHours(-2))
                .build();
    }

    @Override
    public ElevationGainedRecord anotherFullRecord(
            Metadata metadata, Instant startTime, Instant endTime) {
        return new ElevationGainedRecord.Builder(
                        metadata, startTime, endTime, Length.fromMeters(200))
                .setStartZoneOffset(ZoneOffset.ofHours(-1))
                .setEndZoneOffset(ZoneOffset.ofHours(2))
                .build();
    }

    @Override
    public ElevationGainedRecord newEmptyRecord(
            Metadata metadata, Instant startTime, Instant endTime) {
        return new ElevationGainedRecord.Builder(
                        metadata, startTime, endTime, Length.fromMeters(100))
                .build();
    }

    @Override
    protected ElevationGainedRecord recordWithMetadata(
            ElevationGainedRecord record, Metadata metadata) {
        return new ElevationGainedRecord.Builder(
                        metadata, record.getStartTime(), record.getEndTime(), record.getElevation())
                .setStartZoneOffset(record.getStartZoneOffset())
                .setEndZoneOffset(record.getEndZoneOffset())
                .build();
    }

    @Override
    protected Bundle getValuesBundleForRecord(ElevationGainedRecord record) {
        Bundle values = new Bundle();
        values.putDouble(KEY_ELEVATION, record.getElevation().getInMeters());
        return values;
    }

    @Override
    public ElevationGainedRecord newRecordFromValuesBundle(
            Metadata metadata,
            Instant startTime,
            Instant endTime,
            ZoneOffset startZoneOffset,
            ZoneOffset endZoneOffset,
            Bundle bundle) {
        return new ElevationGainedRecord.Builder(
                        metadata,
                        startTime,
                        endTime,
                        Length.fromMeters(bundle.getDouble(KEY_ELEVATION)))
                .setStartZoneOffset(startZoneOffset)
                .setEndZoneOffset(endZoneOffset)
                .build();
    }
}
