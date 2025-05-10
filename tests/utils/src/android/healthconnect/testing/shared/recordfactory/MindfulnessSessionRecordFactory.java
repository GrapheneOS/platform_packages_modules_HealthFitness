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

import static android.health.connect.datatypes.MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_MEDITATION;
import static android.health.connect.datatypes.MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_OTHER;
import static android.health.connect.datatypes.MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_UNKNOWN;

import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.MindfulnessSessionRecord;
import android.os.Bundle;

import java.time.Instant;
import java.time.ZoneOffset;

public final class MindfulnessSessionRecordFactory extends RecordFactory<MindfulnessSessionRecord> {

    private static final String KEY_TYPE = PREFIX + "TYPE";
    private static final String KEY_TITLE = PREFIX + "TITLE";
    private static final String KEY_NOTES = PREFIX + "NOTES";

    @Override
    public MindfulnessSessionRecord newFullRecord(
            Metadata metadata, Instant startTime, Instant endTime) {
        return new MindfulnessSessionRecord.Builder(
                        metadata, startTime, endTime, MINDFULNESS_SESSION_TYPE_OTHER)
                .setTitle("title-foo")
                .setNotes("notes-foo")
                .setStartZoneOffset(ZoneOffset.ofHours(3))
                .setEndZoneOffset(ZoneOffset.ofHours(-2))
                .build();
    }

    @Override
    public MindfulnessSessionRecord anotherFullRecord(
            Metadata metadata, Instant startTime, Instant endTime) {
        return new MindfulnessSessionRecord.Builder(
                        metadata, startTime, endTime, MINDFULNESS_SESSION_TYPE_MEDITATION)
                .setTitle("title-bar")
                .setNotes("notes-bar")
                .setStartZoneOffset(ZoneOffset.ofHours(-1))
                .setEndZoneOffset(ZoneOffset.ofHours(2))
                .build();
    }

    @Override
    public MindfulnessSessionRecord newEmptyRecord(
            Metadata metadata, Instant startTime, Instant endTime) {
        return new MindfulnessSessionRecord.Builder(
                        metadata, startTime, endTime, MINDFULNESS_SESSION_TYPE_UNKNOWN)
                .build();
    }

    @Override
    protected MindfulnessSessionRecord recordWithMetadata(
            MindfulnessSessionRecord record, Metadata metadata) {
        return new MindfulnessSessionRecord.Builder(
                        metadata,
                        record.getStartTime(),
                        record.getEndTime(),
                        record.getMindfulnessSessionType())
                .setTitle(record.getTitle())
                .setNotes(record.getNotes())
                .setStartZoneOffset(record.getStartZoneOffset())
                .setEndZoneOffset(record.getEndZoneOffset())
                .build();
    }

    @Override
    protected Bundle getValuesBundleForRecord(MindfulnessSessionRecord record) {
        Bundle values = new Bundle();
        values.putInt(KEY_TYPE, record.getMindfulnessSessionType());
        values.putCharSequence(KEY_TITLE, record.getTitle());
        values.putCharSequence(KEY_NOTES, record.getNotes());
        return values;
    }

    @Override
    public MindfulnessSessionRecord newRecordFromValuesBundle(
            Metadata metadata,
            Instant startTime,
            Instant endTime,
            ZoneOffset startZoneOffset,
            ZoneOffset endZoneOffset,
            Bundle bundle) {
        int type = bundle.getInt(KEY_TYPE);
        String title = bundle.getString(KEY_TITLE);
        String notes = bundle.getString(KEY_NOTES);
        return new MindfulnessSessionRecord.Builder(metadata, startTime, endTime, type)
                .setTitle(title)
                .setNotes(notes)
                .setStartZoneOffset(startZoneOffset)
                .setEndZoneOffset(endZoneOffset)
                .build();
    }
}
