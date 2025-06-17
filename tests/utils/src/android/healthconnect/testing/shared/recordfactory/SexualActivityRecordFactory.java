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
import android.health.connect.datatypes.SexualActivityRecord;
import android.os.Bundle;

import java.time.Instant;
import java.time.ZoneOffset;

/** Note: This class is AI generated, validate before using, and remove this note */
public final class SexualActivityRecordFactory extends RecordFactory<SexualActivityRecord> {
    private static final String KEY_PROTECTION_USED = PREFIX + "PROTECTION_USED";

    @Override
    public SexualActivityRecord newFullRecord(Metadata metadata, Instant time, Instant endTime) {
        return new SexualActivityRecord.Builder(
                        metadata,
                        time,
                        SexualActivityRecord.SexualActivityProtectionUsed
                                .PROTECTION_USED_UNPROTECTED)
                .setZoneOffset(ZoneOffset.ofHours(1))
                .build();
    }

    @Override
    public SexualActivityRecord anotherFullRecord(
            Metadata metadata, Instant time, Instant endTime) {
        return new SexualActivityRecord.Builder(
                        metadata,
                        time,
                        SexualActivityRecord.SexualActivityProtectionUsed.PROTECTION_USED_PROTECTED)
                .setZoneOffset(ZoneOffset.ofHours(2))
                .build();
    }

    @Override
    public SexualActivityRecord newEmptyRecord(Metadata metadata, Instant time, Instant endTime) {
        return new SexualActivityRecord.Builder(
                        metadata,
                        time,
                        SexualActivityRecord.SexualActivityProtectionUsed
                                .PROTECTION_USED_UNPROTECTED)
                .build();
    }

    @Override
    protected SexualActivityRecord recordWithMetadata(
            SexualActivityRecord record, Metadata metadata) {
        return new SexualActivityRecord.Builder(
                        metadata, record.getTime(), record.getProtectionUsed())
                .setZoneOffset(record.getZoneOffset())
                .build();
    }

    @Override
    protected Bundle getValuesBundleForRecord(SexualActivityRecord record) {
        Bundle values = new Bundle();
        values.putInt(KEY_PROTECTION_USED, record.getProtectionUsed());
        return values;
    }

    @Override
    public SexualActivityRecord newRecordFromValuesBundle(
            Metadata metadata,
            Instant time,
            Instant endTime,
            ZoneOffset zoneOffset,
            ZoneOffset endZoneOffset,
            Bundle bundle) {
        return new SexualActivityRecord.Builder(metadata, time, bundle.getInt(KEY_PROTECTION_USED))
                .setZoneOffset(zoneOffset)
                .build();
    }
}
