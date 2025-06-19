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
import android.health.connect.datatypes.OxygenSaturationRecord;
import android.health.connect.datatypes.units.Percentage;
import android.os.Bundle;

import java.time.Instant;
import java.time.ZoneOffset;

/** Note: This class is AI generated, validate before using, and remove this note */
public final class OxygenSaturationRecordFactory extends RecordFactory<OxygenSaturationRecord> {
    private static final String KEY_PERCENTAGE = PREFIX + "PERCENTAGE";

    @Override
    public OxygenSaturationRecord newFullRecord(Metadata metadata, Instant time, Instant endTime) {
        return new OxygenSaturationRecord.Builder(metadata, time, Percentage.fromValue(98.0))
                .setZoneOffset(ZoneOffset.ofHours(1))
                .build();
    }

    @Override
    public OxygenSaturationRecord anotherFullRecord(
            Metadata metadata, Instant time, Instant endTime) {
        return new OxygenSaturationRecord.Builder(metadata, time, Percentage.fromValue(99.0))
                .setZoneOffset(ZoneOffset.ofHours(2))
                .build();
    }

    @Override
    public OxygenSaturationRecord newEmptyRecord(Metadata metadata, Instant time, Instant endTime) {
        return new OxygenSaturationRecord.Builder(metadata, time, Percentage.fromValue(98.0))
                .build();
    }

    @Override
    protected OxygenSaturationRecord recordWithMetadata(
            OxygenSaturationRecord record, Metadata metadata) {
        return new OxygenSaturationRecord.Builder(
                        metadata, record.getTime(), record.getPercentage())
                .setZoneOffset(record.getZoneOffset())
                .build();
    }

    @Override
    protected Bundle getValuesBundleForRecord(OxygenSaturationRecord record) {
        Bundle values = new Bundle();
        values.putDouble(KEY_PERCENTAGE, record.getPercentage().getValue());
        return values;
    }

    @Override
    public OxygenSaturationRecord newRecordFromValuesBundle(
            Metadata metadata,
            Instant time,
            Instant endTime,
            ZoneOffset zoneOffset,
            ZoneOffset endZoneOffset,
            Bundle bundle) {
        return new OxygenSaturationRecord.Builder(
                        metadata, time, Percentage.fromValue(bundle.getDouble(KEY_PERCENTAGE)))
                .setZoneOffset(zoneOffset)
                .build();
    }

    @Override
    public String recordToString(OxygenSaturationRecord record) {
        return "OxygenSaturationRecord{"
                + "\n\ttime = "
                + record.getTime()
                + ",\n\tzoneOffset = "
                + record.getZoneOffset()
                + ",\n\tmetadata = "
                + metadataToString(record.getMetadata())
                + ",\n\tpercentage = "
                + record.getPercentage()
                + "\n}";
    }
}
