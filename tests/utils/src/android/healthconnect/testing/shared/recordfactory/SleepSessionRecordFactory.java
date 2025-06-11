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
import android.health.connect.datatypes.SleepSessionRecord;
import android.os.Bundle;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;

/** Note: This class is AI generated, validate before using, and remove this note */
public final class SleepSessionRecordFactory extends RecordFactory<SleepSessionRecord> {

    @Override
    public SleepSessionRecord newFullRecord(Metadata metadata, Instant startTime, Instant endTime) {
        return new SleepSessionRecord.Builder(metadata, startTime, endTime)
                .setStages(
                        Collections.singletonList(
                                new SleepSessionRecord.Stage(
                                        startTime,
                                        endTime,
                                        SleepSessionRecord.StageType.STAGE_TYPE_SLEEPING_DEEP)))
                .setStartZoneOffset(ZoneOffset.ofHours(3))
                .setEndZoneOffset(ZoneOffset.ofHours(-2))
                .build();
    }

    @Override
    public SleepSessionRecord anotherFullRecord(
            Metadata metadata, Instant startTime, Instant endTime) {
        return new SleepSessionRecord.Builder(metadata, startTime, endTime)
                .setStages(
                        Collections.singletonList(
                                new SleepSessionRecord.Stage(
                                        startTime,
                                        endTime,
                                        SleepSessionRecord.StageType.STAGE_TYPE_SLEEPING_LIGHT)))
                .setStartZoneOffset(ZoneOffset.ofHours(-1))
                .setEndZoneOffset(ZoneOffset.ofHours(2))
                .build();
    }

    @Override
    public SleepSessionRecord newEmptyRecord(
            Metadata metadata, Instant startTime, Instant endTime) {
        return new SleepSessionRecord.Builder(metadata, startTime, endTime).build();
    }

    @Override
    protected SleepSessionRecord recordWithMetadata(SleepSessionRecord record, Metadata metadata) {
        return new SleepSessionRecord.Builder(metadata, record.getStartTime(), record.getEndTime())
                .setStages(record.getStages())
                .setStartZoneOffset(record.getStartZoneOffset())
                .setEndZoneOffset(record.getEndZoneOffset())
                .build();
    }

    @Override
    protected Bundle getValuesBundleForRecord(SleepSessionRecord record) {
        // TODO(b/424728751): Implement.
        return new Bundle();
    }

    @Override
    public SleepSessionRecord newRecordFromValuesBundle(
            Metadata metadata,
            Instant startTime,
            Instant endTime,
            ZoneOffset startZoneOffset,
            ZoneOffset endZoneOffset,
            Bundle bundle) {
        return new SleepSessionRecord.Builder(metadata, startTime, endTime)
                .setStartZoneOffset(startZoneOffset)
                .setEndZoneOffset(endZoneOffset)
                .build();
    }
}
