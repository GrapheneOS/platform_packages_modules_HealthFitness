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

import android.health.connect.datatypes.MenstruationFlowRecord;
import android.health.connect.datatypes.Metadata;
import android.os.Bundle;

import java.time.Instant;
import java.time.ZoneOffset;

/** Note: This class is AI generated, validate before using, and remove this note */
public final class MenstruationFlowRecordFactory extends RecordFactory<MenstruationFlowRecord> {
    private static final String KEY_FLOW = PREFIX + "FLOW";

    @Override
    public MenstruationFlowRecord newFullRecord(Metadata metadata, Instant time, Instant endTime) {
        return new MenstruationFlowRecord.Builder(
                        metadata, time, MenstruationFlowRecord.MenstruationFlowType.FLOW_LIGHT)
                .setZoneOffset(ZoneOffset.ofHours(1))
                .build();
    }

    @Override
    public MenstruationFlowRecord anotherFullRecord(
            Metadata metadata, Instant time, Instant endTime) {
        return new MenstruationFlowRecord.Builder(
                        metadata, time, MenstruationFlowRecord.MenstruationFlowType.FLOW_MEDIUM)
                .setZoneOffset(ZoneOffset.ofHours(2))
                .build();
    }

    @Override
    public MenstruationFlowRecord newEmptyRecord(Metadata metadata, Instant time, Instant endTime) {
        return new MenstruationFlowRecord.Builder(
                        metadata, time, MenstruationFlowRecord.MenstruationFlowType.FLOW_UNKNOWN)
                .build();
    }

    @Override
    protected MenstruationFlowRecord recordWithMetadata(
            MenstruationFlowRecord record, Metadata metadata) {
        return new MenstruationFlowRecord.Builder(metadata, record.getTime(), record.getFlow())
                .setZoneOffset(record.getZoneOffset())
                .build();
    }

    @Override
    protected Bundle getValuesBundleForRecord(MenstruationFlowRecord record) {
        Bundle values = new Bundle();
        values.putInt(KEY_FLOW, record.getFlow());
        return values;
    }

    @Override
    public MenstruationFlowRecord newRecordFromValuesBundle(
            Metadata metadata,
            Instant time,
            Instant endTime,
            ZoneOffset zoneOffset,
            ZoneOffset endZoneOffset,
            Bundle bundle) {
        return new MenstruationFlowRecord.Builder(metadata, time, bundle.getInt(KEY_FLOW))
                .setZoneOffset(zoneOffset)
                .build();
    }

    @Override
    public String recordToString(MenstruationFlowRecord record) {
        return "MenstruationFlowRecord{"
                + "\n\ttime = "
                + record.getTime()
                + ",\n\tzoneOffset = "
                + record.getZoneOffset()
                + ",\n\tmetadata = "
                + metadataToString(record.getMetadata())
                + ",\n\tflow = "
                + record.getFlow()
                + "\n}";
    }
}
