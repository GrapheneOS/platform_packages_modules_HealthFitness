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

package android.healthconnect.cts.datatypes;

import static android.healthconnect.testing.shared.DataFactory.generateMetadata;
import static android.healthconnect.testing.shared.DataFactory.sessionEndTime;
import static android.healthconnect.testing.shared.DataFactory.sessionStartTime;

import static com.android.healthfitness.flags.Flags.FLAG_EXERCISE_SEGMENT_IMPROVEMENTS;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.health.connect.datatypes.ExerciseSegment;
import android.health.connect.datatypes.ExerciseSegmentType;
import android.health.connect.datatypes.ExerciseSessionRecord;
import android.health.connect.datatypes.ExerciseSessionType;
import android.health.connect.datatypes.units.Mass;
import android.healthconnect.testing.shared.DataFactory;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import org.junit.Rule;
import org.junit.Test;

import java.time.Instant;
import java.util.List;

public class ExerciseSegmentTest {

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    private static final Instant START_TIME = Instant.ofEpochMilli((long) 1e1);
    private static final Instant END_TIME = Instant.ofEpochMilli((long) 1e2);

    private final Instant mNow = DataFactory.now();

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
    @RequiresFlagsEnabled(FLAG_EXERCISE_SEGMENT_IMPROVEMENTS)
    public void testExerciseSegmentWithNewFields_buildSegment_buildCorrectObject() {
        ExerciseSegment segment =
                new ExerciseSegment.Builder(
                                START_TIME,
                                END_TIME,
                                ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_ARM_CURL)
                        .setRepetitionsCount(10)
                        .setWeight(Mass.fromGrams(5000))
                        .setRateOfPerceivedExertion(1.5f)
                        .setSetIndex(4)
                        .build();
        assertThat(segment.getStartTime()).isEqualTo(START_TIME);
        assertThat(segment.getEndTime()).isEqualTo(END_TIME);
        assertThat(segment.getSegmentType())
                .isEqualTo(ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_ARM_CURL);
        assertThat(segment.getRepetitionsCount()).isEqualTo(10);
        assertThat(segment.getWeight()).isEqualTo(Mass.fromGrams(5000));
        assertThat(segment.hasRateOfPerceivedExertion()).isEqualTo(true);
        assertThat(segment.getRateOfPerceivedExertion()).isEqualTo(1.5f);
        assertThat(segment.hasSetIndex()).isEqualTo(true);
        assertThat(segment.getSetIndex()).isEqualTo(4);
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

    @Test
    @RequiresFlagsEnabled(FLAG_EXERCISE_SEGMENT_IMPROVEMENTS)
    public void testExerciseSegment_buildWithoutRpe_throwsException() {
        ExerciseSegment segment =
                new ExerciseSegment.Builder(
                                START_TIME,
                                END_TIME,
                                ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_ARM_CURL)
                        .build();
        assertThrows(IllegalStateException.class, segment::getRateOfPerceivedExertion);
    }

    @Test
    @RequiresFlagsEnabled(FLAG_EXERCISE_SEGMENT_IMPROVEMENTS)
    public void testExerciseSegment_buildWithoutSetIndex_throwsException() {
        ExerciseSegment segment =
                new ExerciseSegment.Builder(
                                START_TIME,
                                END_TIME,
                                ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_ARM_CURL)
                        .build();
        assertThrows(IllegalStateException.class, segment::getSetIndex);
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

    @Test(expected = IllegalArgumentException.class)
    public void testExerciseSegment_lapStartTimeIllegal_throwsException() {
        new ExerciseSessionRecord.Builder(
                        generateMetadata(),
                        sessionStartTime(mNow),
                        sessionStartTime(mNow).plusSeconds(200),
                        ExerciseSessionType.EXERCISE_SESSION_TYPE_BADMINTON)
                .setSegments(
                        List.of(
                                new ExerciseSegment.Builder(
                                                sessionStartTime(mNow).minusSeconds(2),
                                                sessionStartTime(mNow).plusSeconds(100),
                                                ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_BURPEE)
                                        .build()))
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExerciseSegment_lapEndTimeIllegal_throwsException() {
        new ExerciseSessionRecord.Builder(
                        generateMetadata(),
                        sessionStartTime(mNow),
                        sessionStartTime(mNow).plusSeconds(200),
                        ExerciseSessionType.EXERCISE_SESSION_TYPE_BADMINTON)
                .setSegments(
                        List.of(
                                new ExerciseSegment.Builder(
                                                sessionStartTime(mNow),
                                                sessionStartTime(mNow).plusSeconds(1200),
                                                ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_BURPEE)
                                        .build()))
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExerciseSegment_segmentsOverlap_throwsException() {
        new ExerciseSessionRecord.Builder(
                        generateMetadata(),
                        sessionStartTime(mNow),
                        sessionEndTime(mNow),
                        ExerciseSessionType.EXERCISE_SESSION_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING)
                .setSegments(
                        List.of(
                                new ExerciseSegment.Builder(
                                                sessionStartTime(mNow),
                                                sessionStartTime(mNow).plusSeconds(100),
                                                ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_BURPEE)
                                        .build(),
                                new ExerciseSegment.Builder(
                                                sessionStartTime(mNow).plusSeconds(50),
                                                sessionStartTime(mNow).plusSeconds(200),
                                                ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_CRUNCH)
                                        .build()))
                .build();
    }
}
