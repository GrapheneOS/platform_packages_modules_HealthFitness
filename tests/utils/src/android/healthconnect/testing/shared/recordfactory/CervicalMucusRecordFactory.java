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

import android.health.connect.datatypes.CervicalMucusRecord;
import android.health.connect.datatypes.Metadata;
import android.os.Bundle;

import java.time.Instant;
import java.time.ZoneOffset;

/** Note: This class is AI generated, validate before using, and remove this note */
public final class CervicalMucusRecordFactory extends RecordFactory<CervicalMucusRecord> {
    private static final String KEY_APPEARANCE = PREFIX + "APPEARANCE";
    private static final String KEY_SENSATION = PREFIX + "SENSATION";

    @Override
    public CervicalMucusRecord newFullRecord(Metadata metadata, Instant time, Instant endTime) {
        return new CervicalMucusRecord.Builder(
                        metadata,
                        time,
                        CervicalMucusRecord.CervicalMucusSensation.SENSATION_LIGHT,
                        CervicalMucusRecord.CervicalMucusAppearance.APPEARANCE_DRY)
                .setZoneOffset(ZoneOffset.ofHours(1))
                .build();
    }

    @Override
    public CervicalMucusRecord anotherFullRecord(Metadata metadata, Instant time, Instant endTime) {
        return new CervicalMucusRecord.Builder(
                        metadata,
                        time,
                        CervicalMucusRecord.CervicalMucusSensation.SENSATION_MEDIUM,
                        CervicalMucusRecord.CervicalMucusAppearance.APPEARANCE_CREAMY)
                .setZoneOffset(ZoneOffset.ofHours(2))
                .build();
    }

    @Override
    public CervicalMucusRecord newEmptyRecord(Metadata metadata, Instant time, Instant endTime) {
        return new CervicalMucusRecord.Builder(
                        metadata,
                        time,
                        CervicalMucusRecord.CervicalMucusSensation.SENSATION_LIGHT,
                        CervicalMucusRecord.CervicalMucusAppearance.APPEARANCE_DRY)
                .build();
    }

    @Override
    protected CervicalMucusRecord recordWithMetadata(
            CervicalMucusRecord record, Metadata metadata) {
        return new CervicalMucusRecord.Builder(
                        metadata, record.getTime(), record.getSensation(), record.getAppearance())
                .setZoneOffset(record.getZoneOffset())
                .build();
    }

    @Override
    protected Bundle getValuesBundleForRecord(CervicalMucusRecord record) {
        Bundle values = new Bundle();
        values.putInt(KEY_APPEARANCE, record.getAppearance());
        values.putInt(KEY_SENSATION, record.getSensation());
        return values;
    }

    @Override
    public CervicalMucusRecord newRecordFromValuesBundle(
            Metadata metadata,
            Instant time,
            Instant endTime,
            ZoneOffset zoneOffset,
            ZoneOffset endZoneOffset,
            Bundle bundle) {
        return new CervicalMucusRecord.Builder(
                        metadata, time, bundle.getInt(KEY_SENSATION), bundle.getInt(KEY_APPEARANCE))
                .setZoneOffset(zoneOffset)
                .build();
    }

    @Override
    public String recordToString(CervicalMucusRecord record) {
        return "CervicalMucusRecord{"
                + "\n\ttime = "
                + record.getTime()
                + ",\n\tzoneOffset = "
                + record.getZoneOffset()
                + ",\n\tmetadata = "
                + metadataToString(record.getMetadata())
                + ",\n\tappearance = "
                + record.getAppearance()
                + ",\n\tsensation = "
                + record.getSensation()
                + "\n}";
    }
}
