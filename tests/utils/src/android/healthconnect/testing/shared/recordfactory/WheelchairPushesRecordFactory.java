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
import android.health.connect.datatypes.WheelchairPushesRecord;
import android.os.Bundle;

import java.time.Instant;
import java.time.ZoneOffset;

/** Note: This class is AI generated, validate before using, and remove this note */
public final class WheelchairPushesRecordFactory extends RecordFactory<WheelchairPushesRecord> {
    private static final String KEY_COUNT = PREFIX + "COUNT";

    @Override
    public WheelchairPushesRecord newFullRecord(
            Metadata metadata, Instant startTime, Instant endTime) {
        return new WheelchairPushesRecord.Builder(metadata, startTime, endTime, 100)
                .setStartZoneOffset(ZoneOffset.ofHours(3))
                .setEndZoneOffset(ZoneOffset.ofHours(-2))
                .build();
    }

    @Override
    public WheelchairPushesRecord anotherFullRecord(
            Metadata metadata, Instant startTime, Instant endTime) {
        return new WheelchairPushesRecord.Builder(metadata, startTime, endTime, 200)
                .setStartZoneOffset(ZoneOffset.ofHours(-1))
                .setEndZoneOffset(ZoneOffset.ofHours(2))
                .build();
    }

    @Override
    public WheelchairPushesRecord newEmptyRecord(
            Metadata metadata, Instant startTime, Instant endTime) {
        return new WheelchairPushesRecord.Builder(metadata, startTime, endTime, 100).build();
    }

    @Override
    protected WheelchairPushesRecord recordWithMetadata(
            WheelchairPushesRecord record, Metadata metadata) {
        return new WheelchairPushesRecord.Builder(
                        metadata, record.getStartTime(), record.getEndTime(), record.getCount())
                .setStartZoneOffset(record.getStartZoneOffset())
                .setEndZoneOffset(record.getEndZoneOffset())
                .build();
    }

    @Override
    protected Bundle getValuesBundleForRecord(WheelchairPushesRecord record) {
        Bundle values = new Bundle();
        values.putLong(KEY_COUNT, record.getCount());
        return values;
    }

    @Override
    public WheelchairPushesRecord newRecordFromValuesBundle(
            Metadata metadata,
            Instant startTime,
            Instant endTime,
            ZoneOffset startZoneOffset,
            ZoneOffset endZoneOffset,
            Bundle bundle) {
        return new WheelchairPushesRecord.Builder(
                        metadata, startTime, endTime, bundle.getLong(KEY_COUNT, 100))
                .setStartZoneOffset(startZoneOffset)
                .setEndZoneOffset(endZoneOffset)
                .build();
    }
}
