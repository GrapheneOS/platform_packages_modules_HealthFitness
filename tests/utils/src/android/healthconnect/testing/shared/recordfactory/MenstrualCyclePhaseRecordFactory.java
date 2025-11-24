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

import static android.health.connect.datatypes.MenstrualCyclePhaseRecord.PHASE_FOLLICULAR;
import static android.health.connect.datatypes.MenstrualCyclePhaseRecord.PHASE_LUTEAL;

import android.health.connect.datatypes.MenstrualCyclePhaseRecord;
import android.health.connect.datatypes.Metadata;
import android.os.Bundle;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

public final class MenstrualCyclePhaseRecordFactory
        extends RecordFactory<MenstrualCyclePhaseRecord> {
    private static final String KEY_PHASE = PREFIX + "PHASE";
    private static final String KEY_DAY_OF_CYCLE = PREFIX + "DAY_OF_CYCLE";

    private static final ZoneOffset TEST_ZONE_OFFSET_1 = ZoneOffset.ofHours(4);
    private static final ZoneOffset TEST_ZONE_OFFSET_2 = ZoneOffset.ofHours(5);

    @Override
    public MenstrualCyclePhaseRecord newFullRecord(
            Metadata metadata, Instant startTime, Instant endTime) {
        LocalDate date = startTime.atOffset(TEST_ZONE_OFFSET_1).toLocalDate();
        return new MenstrualCyclePhaseRecord.Builder(metadata, date, PHASE_FOLLICULAR)
                .setDayOfCycle(1)
                .build();
    }

    @Override
    public MenstrualCyclePhaseRecord anotherFullRecord(
            Metadata metadata, Instant startTime, Instant endTime) {
        LocalDate date = startTime.atOffset(TEST_ZONE_OFFSET_2).toLocalDate();
        return new MenstrualCyclePhaseRecord.Builder(metadata, date, PHASE_LUTEAL)
                .setDayOfCycle(3)
                .build();
    }

    @Override
    public MenstrualCyclePhaseRecord newEmptyRecord(
            Metadata metadata, Instant startTime, Instant endTime) {
        LocalDate date = startTime.atOffset(ZoneOffset.UTC).toLocalDate();
        return new MenstrualCyclePhaseRecord.Builder(metadata, date, PHASE_LUTEAL).build();
    }

    @Override
    protected MenstrualCyclePhaseRecord recordWithMetadata(
            MenstrualCyclePhaseRecord record, Metadata metadata) {
        MenstrualCyclePhaseRecord.Builder builder =
                new MenstrualCyclePhaseRecord.Builder(
                        metadata, record.getDate(), record.getPhase());
        if (record.isDayOfCycleSet()) {
            builder.setDayOfCycle(record.getDayOfCycle());
        }
        return builder.build();
    }

    @Override
    protected Bundle getValuesBundleForRecord(MenstrualCyclePhaseRecord record) {
        Bundle values = new Bundle();
        values.putInt(KEY_PHASE, record.getPhase());
        if (record.isDayOfCycleSet()) {
            values.putInt(KEY_DAY_OF_CYCLE, record.getDayOfCycle());
        }
        return values;
    }

    @Override
    public MenstrualCyclePhaseRecord newRecordFromValuesBundle(
            Metadata metadata,
            Instant startTime,
            Instant endTime,
            ZoneOffset startZoneOffset,
            ZoneOffset endZoneOffset,
            Bundle bundle) {
        LocalDate date = startTime.atOffset(startZoneOffset).toLocalDate();
        MenstrualCyclePhaseRecord.Builder builder =
                new MenstrualCyclePhaseRecord.Builder(metadata, date, bundle.getInt(KEY_PHASE));
        if (bundle.containsKey(KEY_DAY_OF_CYCLE)) {
            builder.setDayOfCycle(bundle.getInt(KEY_DAY_OF_CYCLE));
        }
        return builder.build();
    }

    @Override
    public String recordToString(MenstrualCyclePhaseRecord record) {
        return record.toString()
                + "{"
                + "\n\tstartTime = "
                + record.getStartTime()
                + ",\n\tstartZoneOffset = "
                + record.getStartZoneOffset()
                + "\n\tendTime = "
                + record.getEndTime()
                + ",\n\tendZoneOffset = "
                + record.getEndZoneOffset()
                + ",\n\tmetadata = "
                + metadataToString(record.getMetadata())
                + ",\n\tphase = "
                + record.getPhase()
                + ",\n\tdayOfCycle = "
                + (record.isDayOfCycleSet() ? record.getDayOfCycle() : -1)
                + "\n}";
    }
}
