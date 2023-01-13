/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.healthconnect.cts;

import static com.google.common.truth.Truth.assertThat;

import android.healthconnect.datatypes.ExerciseSegment;
import android.healthconnect.datatypes.ExerciseSegmentType;

import org.junit.Test;

import java.time.Instant;

public class ExerciseSegmentTest {
    private static final Instant START_TIME = Instant.ofEpochMilli((long) 1e1);
    private static final Instant END_TIME = Instant.ofEpochMilli((long) 1e2);

    @Test
    public void testExerciseSegment_buildSegment_buildCorrectObject() {
        ExerciseSegment segment =
                new ExerciseSegment.Builder(
                                START_TIME,
                                END_TIME,
                                ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_ARM_CURL)
                        .setRepetitionsCount(10)
                        .build();
        assertThat(segment.getStartTime()).isEqualTo(START_TIME);
        assertThat(segment.getEndTime()).isEqualTo(END_TIME);
        assertThat(segment.getSegmentType())
                .isEqualTo(ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_ARM_CURL);
        assertThat(segment.getRepetitionsCount()).isEqualTo(10);
    }

    @Test
    public void testExerciseSegment_buildWithoutRepetitions_repetitionsIsZero() {
        ExerciseSegment segment =
                new ExerciseSegment.Builder(
                                START_TIME,
                                END_TIME,
                                ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_ARM_CURL)
                        .build();
        assertThat(segment.getRepetitionsCount()).isEqualTo(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExerciseSegment_endTimeEarlierThanStartTime_throwsException() {
        new ExerciseSegment.Builder(
                        END_TIME, START_TIME, ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_ARM_CURL)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExerciseSegment_repetitionsIsNegative_throwsException() {
        new ExerciseSegment.Builder(
                        START_TIME, END_TIME, ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_ARM_CURL)
                .setRepetitionsCount(-1)
                .build();
    }
}
