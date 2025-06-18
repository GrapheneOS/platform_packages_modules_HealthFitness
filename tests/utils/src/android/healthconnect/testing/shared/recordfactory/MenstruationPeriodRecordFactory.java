/*
 * Copyright (C) 2024 The Android Open Source Project
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

import android.health.connect.datatypes.MenstruationPeriodRecord;
import android.health.connect.datatypes.Metadata;
import android.os.Bundle;

import java.time.Instant;
import java.time.ZoneOffset;

public final class MenstruationPeriodRecordFactory extends RecordFactory<MenstruationPeriodRecord> {

    @Override
    public MenstruationPeriodRecord newFullRecord(
            Metadata metadata, Instant startTime, Instant endTime) {
        return new MenstruationPeriodRecord.Builder(metadata, startTime, endTime)
                .setStartZoneOffset(ZoneOffset.ofHours(3))
                .setEndZoneOffset(ZoneOffset.ofHours(-2))
                .build();
    }

    @Override
    public MenstruationPeriodRecord anotherFullRecord(
            Metadata metadata, Instant startTime, Instant endTime) {
        return new MenstruationPeriodRecord.Builder(metadata, startTime, endTime)
                .setStartZoneOffset(ZoneOffset.ofHours(-1))
                .setEndZoneOffset(ZoneOffset.ofHours(2))
                .build();
    }

    @Override
    public MenstruationPeriodRecord newEmptyRecord(
            Metadata metadata, Instant startTime, Instant endTime) {
        return new MenstruationPeriodRecord.Builder(metadata, startTime, endTime).build();
    }

    @Override
    protected MenstruationPeriodRecord recordWithMetadata(
            MenstruationPeriodRecord record, Metadata metadata) {
        return new MenstruationPeriodRecord.Builder(
                        metadata, record.getStartTime(), record.getEndTime())
                .setStartZoneOffset(record.getStartZoneOffset())
                .setEndZoneOffset(record.getEndZoneOffset())
                .build();
    }

    @Override
    protected Bundle getValuesBundleForRecord(MenstruationPeriodRecord record) {
        return new Bundle();
    }

    @Override
    public MenstruationPeriodRecord newRecordFromValuesBundle(
            Metadata metadata,
            Instant startTime,
            Instant endTime,
            ZoneOffset startZoneOffset,
            ZoneOffset endZoneOffset,
            Bundle bundle) {
        return new MenstruationPeriodRecord.Builder(metadata, startTime, endTime)
                .setStartZoneOffset(startZoneOffset)
                .setEndZoneOffset(endZoneOffset)
                .build();
    }

    @Override
    public String recordToString(MenstruationPeriodRecord record) {
        return "MenstruationPeriodRecord{"
                + "\n\tstartTime = "
                + record.getStartTime()
                + ",\n\tendTime = "
                + record.getEndTime()
                + ",\n\tstartZoneOffset = "
                + record.getStartZoneOffset()
                + ",\n\tendZoneOffset = "
                + record.getEndZoneOffset()
                + ",\n\tmetadata = "
                + metadataToString(record.getMetadata())
                + "\n}";
    }
}
