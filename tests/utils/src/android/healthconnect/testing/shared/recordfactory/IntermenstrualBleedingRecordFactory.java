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

import android.health.connect.datatypes.IntermenstrualBleedingRecord;
import android.health.connect.datatypes.Metadata;
import android.os.Bundle;

import java.time.Instant;
import java.time.ZoneOffset;

/** Note: This class is AI generated, validate before using, and remove this note */
public final class IntermenstrualBleedingRecordFactory
        extends RecordFactory<IntermenstrualBleedingRecord> {

    @Override
    public IntermenstrualBleedingRecord newFullRecord(
            Metadata metadata, Instant time, Instant endTime) {
        return new IntermenstrualBleedingRecord.Builder(metadata, time)
                .setZoneOffset(ZoneOffset.ofHours(1))
                .build();
    }

    @Override
    public IntermenstrualBleedingRecord anotherFullRecord(
            Metadata metadata, Instant time, Instant endTime) {
        return new IntermenstrualBleedingRecord.Builder(metadata, time)
                .setZoneOffset(ZoneOffset.ofHours(2))
                .build();
    }

    @Override
    public IntermenstrualBleedingRecord newEmptyRecord(
            Metadata metadata, Instant time, Instant endTime) {
        return new IntermenstrualBleedingRecord.Builder(metadata, time).build();
    }

    @Override
    protected IntermenstrualBleedingRecord recordWithMetadata(
            IntermenstrualBleedingRecord record, Metadata metadata) {
        return new IntermenstrualBleedingRecord.Builder(metadata, record.getTime())
                .setZoneOffset(record.getZoneOffset())
                .build();
    }

    @Override
    protected Bundle getValuesBundleForRecord(IntermenstrualBleedingRecord record) {
        return new Bundle();
    }

    @Override
    public IntermenstrualBleedingRecord newRecordFromValuesBundle(
            Metadata metadata,
            Instant time,
            Instant endTime,
            ZoneOffset zoneOffset,
            ZoneOffset endZoneOffset,
            Bundle bundle) {
        return new IntermenstrualBleedingRecord.Builder(metadata, time)
                .setZoneOffset(zoneOffset)
                .build();
    }
}
