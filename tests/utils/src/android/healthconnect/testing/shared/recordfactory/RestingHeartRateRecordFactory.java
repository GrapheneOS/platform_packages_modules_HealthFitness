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
import android.health.connect.datatypes.RestingHeartRateRecord;
import android.os.Bundle;

import java.time.Instant;
import java.time.ZoneOffset;

/** Note: This class is AI generated, validate before using, and remove this note */
public final class RestingHeartRateRecordFactory extends RecordFactory<RestingHeartRateRecord> {
    private static final String KEY_BEATS_PER_MINUTE = PREFIX + "BEATS_PER_MINUTE";

    @Override
    public RestingHeartRateRecord newFullRecord(Metadata metadata, Instant time, Instant endTime) {
        return new RestingHeartRateRecord.Builder(metadata, time, 60)
                .setZoneOffset(ZoneOffset.ofHours(1))
                .build();
    }

    @Override
    public RestingHeartRateRecord anotherFullRecord(
            Metadata metadata, Instant time, Instant endTime) {
        return new RestingHeartRateRecord.Builder(metadata, time, 65)
                .setZoneOffset(ZoneOffset.ofHours(2))
                .build();
    }

    @Override
    public RestingHeartRateRecord newEmptyRecord(Metadata metadata, Instant time, Instant endTime) {
        return new RestingHeartRateRecord.Builder(metadata, time, 60).build();
    }

    @Override
    protected RestingHeartRateRecord recordWithMetadata(
            RestingHeartRateRecord record, Metadata metadata) {
        return new RestingHeartRateRecord.Builder(
                        metadata, record.getTime(), record.getBeatsPerMinute())
                .setZoneOffset(record.getZoneOffset())
                .build();
    }

    @Override
    protected Bundle getValuesBundleForRecord(RestingHeartRateRecord record) {
        Bundle values = new Bundle();
        values.putLong(KEY_BEATS_PER_MINUTE, record.getBeatsPerMinute());
        return values;
    }

    @Override
    public RestingHeartRateRecord newRecordFromValuesBundle(
            Metadata metadata,
            Instant time,
            Instant endTime,
            ZoneOffset zoneOffset,
            ZoneOffset endZoneOffset,
            Bundle bundle) {
        return new RestingHeartRateRecord.Builder(
                        metadata, time, bundle.getLong(KEY_BEATS_PER_MINUTE))
                .setZoneOffset(zoneOffset)
                .build();
    }

    @Override
    public String recordToString(RestingHeartRateRecord record) {
        return "RestingHeartRateRecord{"
                + "\n\ttime = "
                + record.getTime()
                + ",\n\tzoneOffset = "
                + record.getZoneOffset()
                + ",\n\tmetadata = "
                + metadataToString(record.getMetadata())
                + ",\n\tbeatsPerMinute = "
                + record.getBeatsPerMinute()
                + "\n}";
    }
}
