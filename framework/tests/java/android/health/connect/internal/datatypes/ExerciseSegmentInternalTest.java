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

package android.health.connect.internal.datatypes;

import static com.android.healthfitness.flags.Flags.FLAG_EXERCISE_SEGMENT_IMPROVEMENTS;
import static com.android.healthfitness.flags.Flags.FLAG_EXERCISE_SEGMENT_IMPROVEMENTS_DB;

import static com.google.common.truth.Truth.assertThat;

import android.health.connect.datatypes.ExerciseSegment;
import android.health.connect.datatypes.ExerciseSegmentType;
import android.health.connect.datatypes.units.Mass;
import android.os.Parcel;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.time.Period;

@RunWith(AndroidJUnit4.class)
public class ExerciseSegmentInternalTest {

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private final Instant mStartTime = Instant.now().minus(Period.ofDays(1));
    private final Instant mEndTime = Instant.now();

    @Test
    public void testExerciseSegmentInternal_convertToExternalAndBack_recordsAreEqual_noNewFields() {
        ExerciseSegment externalSegment =
                new ExerciseSegment.Builder(
                                mStartTime,
                                mEndTime,
                                ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_ARM_CURL)
                        .setRepetitionsCount(45)
                        .build();
        ExerciseSegment converted = externalSegment.toSegmentInternal().toExternalRecord();
        assertSegmentsAreEqual(converted, externalSegment);
    }

    @Test
    @EnableFlags({FLAG_EXERCISE_SEGMENT_IMPROVEMENTS, FLAG_EXERCISE_SEGMENT_IMPROVEMENTS_DB})
    public void testExerciseSegmentInternal_convertToExternalAndBack_recordsAreEqual_newFields() {
        ExerciseSegment externalSegment =
                new ExerciseSegment.Builder(
                                mStartTime,
                                mEndTime,
                                ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_ARM_CURL)
                        .setRepetitionsCount(45)
                        .setWeight(Mass.fromGrams(100.0))
                        .setSetIndex(3)
                        .setRateOfPerceivedExertion(5f)
                        .build();
        ExerciseSegment converted = externalSegment.toSegmentInternal().toExternalRecord();
        assertSegmentsAreEqual(converted, externalSegment);
    }

    @Test
    public void
            testExerciseSegmentInternal_convertToExternalAndBackNoRepsOrWeight_recordsAreEqual() {
        ExerciseSegment externalSegment =
                new ExerciseSegment.Builder(
                                mStartTime,
                                mEndTime,
                                ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_ARM_CURL)
                        .build();
        ExerciseSegment converted = externalSegment.toSegmentInternal().toExternalRecord();
        assertSegmentsAreEqual(converted, externalSegment);
    }

    @Test
    @EnableFlags({FLAG_EXERCISE_SEGMENT_IMPROVEMENTS, FLAG_EXERCISE_SEGMENT_IMPROVEMENTS_DB})
    public void testExerciseSegmentInternal_writeToParcelAndBack_recordsAreEqual_newFields() {
        ExerciseSegmentInternal segment =
                new ExerciseSegmentInternal()
                        .setStartTime(mStartTime.toEpochMilli())
                        .setEndTime(mEndTime.toEpochMilli())
                        .setSegmentType(ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_ARM_CURL)
                        .setRepetitionsCount(10)
                        .setWeightGrams(100.0)
                        .setSetIndex(1)
                        .setRateOfPerceivedExertion(6f);
        Parcel parcel = Parcel.obtain();
        segment.writeToParcel(parcel);
        parcel.setDataPosition(0);
        ExerciseSegmentInternal restored = ExerciseSegmentInternal.readFromParcel(parcel);
        parcel.recycle();
        assertSegmentsAreEqual(restored, segment);
    }

    @Test
    public void
            testExerciseSegmentInternal_writeToParcelAndBackNoAdditionalFields_recordsAreEqual() {
        ExerciseSegmentInternal segment =
                new ExerciseSegmentInternal()
                        .setStartTime(mStartTime.toEpochMilli())
                        .setEndTime(mEndTime.toEpochMilli())
                        .setSegmentType(ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_ARM_CURL);
        Parcel parcel = Parcel.obtain();
        segment.writeToParcel(parcel);
        parcel.setDataPosition(0);
        ExerciseSegmentInternal restored = ExerciseSegmentInternal.readFromParcel(parcel);
        parcel.recycle();
        assertSegmentsAreEqual(restored, segment);
    }

    private void assertSegmentsAreEqual(
            ExerciseSegment converted, ExerciseSegment externalSegment) {
        // Compare time in milliseconds as we store time in milliseconds in the database.
        assertThat(converted.getStartTime().toEpochMilli())
                .isEqualTo(externalSegment.getStartTime().toEpochMilli());
        assertThat(converted.getEndTime().toEpochMilli())
                .isEqualTo(externalSegment.getEndTime().toEpochMilli());

        assertThat(converted.getSegmentType()).isEqualTo(externalSegment.getSegmentType());
        assertThat(converted.getRepetitionsCount())
                .isEqualTo(externalSegment.getRepetitionsCount());
        assertThat(converted.getWeight()).isEqualTo(externalSegment.getWeight());
        assertThat(converted.hasSetIndex()).isEqualTo(externalSegment.hasSetIndex());
        if (converted.hasSetIndex()) {
            assertThat(converted.getSetIndex()).isEqualTo(externalSegment.getSetIndex());
        }
        assertThat(converted.hasRateOfPerceivedExertion())
                .isEqualTo(externalSegment.hasRateOfPerceivedExertion());
        if (converted.hasRateOfPerceivedExertion()) {
            assertThat(converted.getRateOfPerceivedExertion())
                    .isEqualTo(externalSegment.getRateOfPerceivedExertion());
        }
    }

    private void assertSegmentsAreEqual(
            ExerciseSegmentInternal restored, ExerciseSegmentInternal segment) {
        assertThat(restored.getRepetitionsCount()).isEqualTo(segment.getRepetitionsCount());
        assertThat(restored.getStartTime()).isEqualTo(segment.getStartTime());
        assertThat(restored.getEndTime()).isEqualTo(segment.getEndTime());
        assertThat(restored.getSegmentType()).isEqualTo(segment.getSegmentType());
        assertThat(restored.getWeightGrams()).isEqualTo(segment.getWeightGrams());
        assertThat(restored.getSetIndex()).isEqualTo(segment.getSetIndex());
        assertThat(restored.getRateOfPerceivedExertion())
                .isEqualTo(segment.getRateOfPerceivedExertion());
        assertThat(restored).isEqualTo(segment);
    }
}
