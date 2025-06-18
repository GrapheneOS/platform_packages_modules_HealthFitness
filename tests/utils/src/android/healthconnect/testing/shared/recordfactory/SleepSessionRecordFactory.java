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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Note: This class is AI generated, validate before using, and remove this note */
public final class SleepSessionRecordFactory extends RecordFactory<SleepSessionRecord> {
    private static final String KEY_TITLE = PREFIX + "TITLE";
    private static final String KEY_NOTES = PREFIX + "NOTES";
    private static final String KEY_STAGES = PREFIX + "STAGES";

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
                .setTitle("My sleep session")
                .setNotes("A long night's sleep")
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
                .setTitle("My nap")
                .setNotes("A short nap")
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
                .setTitle(record.getTitle())
                .setNotes(record.getNotes())
                .build();
    }

    private static final String KEY_STAGES_START_TIMES = KEY_STAGES + "_START_TIMES";
    private static final String KEY_STAGES_END_TIMES = KEY_STAGES + "_END_TIMES";
    private static final String KEY_STAGES_TYPES = KEY_STAGES + "_TYPES";

    @Override
    protected Bundle getValuesBundleForRecord(SleepSessionRecord record) {
        Bundle values = new Bundle();
        values.putCharSequence(KEY_TITLE, record.getTitle());
        values.putCharSequence(KEY_NOTES, record.getNotes());

        long[] stageStartTimes =
                record.getStages().stream()
                        .mapToLong(stage -> stage.getStartTime().toEpochMilli())
                        .toArray();
        long[] stageEndTimes =
                record.getStages().stream()
                        .mapToLong(stage -> stage.getEndTime().toEpochMilli())
                        .toArray();
        ArrayList<Integer> stageTypes = new ArrayList<>();
        for (SleepSessionRecord.Stage stage : record.getStages()) {
            stageTypes.add(stage.getType());
        }
        values.putLongArray(KEY_STAGES_START_TIMES, stageStartTimes);
        values.putLongArray(KEY_STAGES_END_TIMES, stageEndTimes);
        values.putIntegerArrayList(KEY_STAGES_TYPES, stageTypes);

        return values;
    }

    @Override
    public SleepSessionRecord newRecordFromValuesBundle(
            Metadata metadata,
            Instant startTime,
            Instant endTime,
            ZoneOffset startZoneOffset,
            ZoneOffset endZoneOffset,
            Bundle bundle) {
        SleepSessionRecord.Builder builder =
                new SleepSessionRecord.Builder(metadata, startTime, endTime)
                        .setStartZoneOffset(startZoneOffset)
                        .setEndZoneOffset(endZoneOffset);
        if (bundle.containsKey(KEY_TITLE)) {
            builder.setTitle(bundle.getString(KEY_TITLE));
        }
        if (bundle.containsKey(KEY_NOTES)) {
            builder.setNotes(bundle.getString(KEY_NOTES));
        }

        long[] stageStartTimes = bundle.getLongArray(KEY_STAGES_START_TIMES);
        if (stageStartTimes != null) {
            long[] stageEndTimes = bundle.getLongArray(KEY_STAGES_END_TIMES);
            ArrayList<Integer> stageTypes = bundle.getIntegerArrayList(KEY_STAGES_TYPES);
            List<SleepSessionRecord.Stage> stages = new ArrayList<>();
            for (int i = 0; i < stageStartTimes.length; i++) {
                stages.add(
                        new SleepSessionRecord.Stage(
                                Instant.ofEpochMilli(stageStartTimes[i]),
                                Instant.ofEpochMilli(stageEndTimes[i]),
                                stageTypes.get(i)));
            }
            builder.setStages(stages);
        }

        return builder.build();
    }

    @Override
    public String recordToString(SleepSessionRecord record) {
        return "SleepSessionRecord{"
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
                + ",\n\ttitle = "
                + record.getTitle()
                + ",\n\tnotes = "
                + record.getNotes()
                + ",\n\tstages = "
                + record.getStages()
                + "\n}";
    }
}
