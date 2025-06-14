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

import android.health.connect.datatypes.ExerciseSessionRecord;
import android.health.connect.datatypes.ExerciseSessionType;
import android.health.connect.datatypes.Metadata;
import android.os.Bundle;

import java.time.Instant;
import java.time.ZoneOffset;

/** Note: This class is AI generated, validate before using, and remove this note */
public final class ExerciseSessionRecordFactory extends RecordFactory<ExerciseSessionRecord> {
    private static final String KEY_EXERCISE_TYPE = PREFIX + "EXERCISE_TYPE";

    @Override
    public ExerciseSessionRecord newFullRecord(
            Metadata metadata, Instant startTime, Instant endTime) {
        return new ExerciseSessionRecord.Builder(
                        metadata,
                        startTime,
                        endTime,
                        ExerciseSessionType.EXERCISE_SESSION_TYPE_WALKING)
                .setStartZoneOffset(ZoneOffset.ofHours(3))
                .setEndZoneOffset(ZoneOffset.ofHours(-2))
                .build();
    }

    @Override
    public ExerciseSessionRecord anotherFullRecord(
            Metadata metadata, Instant startTime, Instant endTime) {
        return new ExerciseSessionRecord.Builder(
                        metadata,
                        startTime,
                        endTime,
                        ExerciseSessionType.EXERCISE_SESSION_TYPE_RUNNING)
                .setStartZoneOffset(ZoneOffset.ofHours(-1))
                .setEndZoneOffset(ZoneOffset.ofHours(2))
                .build();
    }

    @Override
    public ExerciseSessionRecord newEmptyRecord(
            Metadata metadata, Instant startTime, Instant endTime) {
        return new ExerciseSessionRecord.Builder(
                        metadata,
                        startTime,
                        endTime,
                        ExerciseSessionType.EXERCISE_SESSION_TYPE_WALKING)
                .build();
    }

    @Override
    protected ExerciseSessionRecord recordWithMetadata(
            ExerciseSessionRecord record, Metadata metadata) {
        return new ExerciseSessionRecord.Builder(
                        metadata,
                        record.getStartTime(),
                        record.getEndTime(),
                        record.getExerciseType())
                .setStartZoneOffset(record.getStartZoneOffset())
                .setEndZoneOffset(record.getEndZoneOffset())
                .build();
    }

    @Override
    protected Bundle getValuesBundleForRecord(ExerciseSessionRecord record) {
        Bundle values = new Bundle();
        values.putInt(KEY_EXERCISE_TYPE, record.getExerciseType());
        return values;
    }

    @Override
    public ExerciseSessionRecord newRecordFromValuesBundle(
            Metadata metadata,
            Instant startTime,
            Instant endTime,
            ZoneOffset startZoneOffset,
            ZoneOffset endZoneOffset,
            Bundle bundle) {
        return new ExerciseSessionRecord.Builder(
                        metadata,
                        startTime,
                        endTime,
                        bundle.getInt(
                                KEY_EXERCISE_TYPE,
                                ExerciseSessionType.EXERCISE_SESSION_TYPE_UNKNOWN))
                .setStartZoneOffset(startZoneOffset)
                .setEndZoneOffset(endZoneOffset)
                .build();
    }
}
