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

import android.health.connect.datatypes.BoneMassRecord;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.units.Mass;
import android.os.Bundle;

import java.time.Instant;
import java.time.ZoneOffset;

/** Note: This class is AI generated, validate before using, and remove this note */
public final class BoneMassRecordFactory extends RecordFactory<BoneMassRecord> {
    private static final String KEY_MASS = PREFIX + "MASS";

    @Override
    public BoneMassRecord newFullRecord(Metadata metadata, Instant time, Instant endTime) {
        return new BoneMassRecord.Builder(metadata, time, Mass.fromGrams(3.0))
                .setZoneOffset(ZoneOffset.ofHours(1))
                .build();
    }

    @Override
    public BoneMassRecord anotherFullRecord(Metadata metadata, Instant time, Instant endTime) {
        return new BoneMassRecord.Builder(metadata, time, Mass.fromGrams(3.2))
                .setZoneOffset(ZoneOffset.ofHours(2))
                .build();
    }

    @Override
    public BoneMassRecord newEmptyRecord(Metadata metadata, Instant time, Instant endTime) {
        return new BoneMassRecord.Builder(metadata, time, Mass.fromGrams(3.0)).build();
    }

    @Override
    protected BoneMassRecord recordWithMetadata(BoneMassRecord record, Metadata metadata) {
        return new BoneMassRecord.Builder(metadata, record.getTime(), record.getMass())
                .setZoneOffset(record.getZoneOffset())
                .build();
    }

    @Override
    protected Bundle getValuesBundleForRecord(BoneMassRecord record) {
        Bundle values = new Bundle();
        values.putDouble(KEY_MASS, record.getMass().getInGrams());
        return values;
    }

    @Override
    public BoneMassRecord newRecordFromValuesBundle(
            Metadata metadata,
            Instant time,
            Instant endTime,
            ZoneOffset zoneOffset,
            ZoneOffset endZoneOffset,
            Bundle bundle) {
        return new BoneMassRecord.Builder(
                        metadata, time, Mass.fromGrams(bundle.getDouble(KEY_MASS)))
                .setZoneOffset(zoneOffset)
                .build();
    }

    @Override
    public String recordToString(BoneMassRecord record) {
        return "BoneMassRecord{"
                + "\n\ttime = "
                + record.getTime()
                + ",\n\tzoneOffset = "
                + record.getZoneOffset()
                + ",\n\tmetadata = "
                + metadataToString(record.getMetadata())
                + ",\n\tmass = "
                + record.getMass()
                + "\n}";
    }
}
