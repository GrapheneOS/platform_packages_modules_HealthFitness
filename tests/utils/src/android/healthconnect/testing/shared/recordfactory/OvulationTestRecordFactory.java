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
import android.health.connect.datatypes.OvulationTestRecord;
import android.os.Bundle;

import java.time.Instant;
import java.time.ZoneOffset;

/** Note: This class is AI generated, validate before using, and remove this note */
public final class OvulationTestRecordFactory extends RecordFactory<OvulationTestRecord> {
    private static final String KEY_RESULT = PREFIX + "RESULT";

    @Override
    public OvulationTestRecord newFullRecord(Metadata metadata, Instant time, Instant endTime) {
        return new OvulationTestRecord.Builder(
                        metadata, time, OvulationTestRecord.OvulationTestResult.RESULT_NEGATIVE)
                .setZoneOffset(ZoneOffset.ofHours(1))
                .build();
    }

    @Override
    public OvulationTestRecord anotherFullRecord(Metadata metadata, Instant time, Instant endTime) {
        return new OvulationTestRecord.Builder(
                        metadata, time, OvulationTestRecord.OvulationTestResult.RESULT_POSITIVE)
                .setZoneOffset(ZoneOffset.ofHours(2))
                .build();
    }

    @Override
    public OvulationTestRecord newEmptyRecord(Metadata metadata, Instant time, Instant endTime) {
        return new OvulationTestRecord.Builder(
                        metadata, time, OvulationTestRecord.OvulationTestResult.RESULT_INCONCLUSIVE)
                .build();
    }

    @Override
    protected OvulationTestRecord recordWithMetadata(
            OvulationTestRecord record, Metadata metadata) {
        return new OvulationTestRecord.Builder(metadata, record.getTime(), record.getResult())
                .setZoneOffset(record.getZoneOffset())
                .build();
    }

    @Override
    protected Bundle getValuesBundleForRecord(OvulationTestRecord record) {
        Bundle values = new Bundle();
        values.putInt(KEY_RESULT, record.getResult());
        return values;
    }

    @Override
    public OvulationTestRecord newRecordFromValuesBundle(
            Metadata metadata,
            Instant time,
            Instant endTime,
            ZoneOffset zoneOffset,
            ZoneOffset endZoneOffset,
            Bundle bundle) {
        return new OvulationTestRecord.Builder(metadata, time, bundle.getInt(KEY_RESULT))
                .setZoneOffset(zoneOffset)
                .build();
    }
}
