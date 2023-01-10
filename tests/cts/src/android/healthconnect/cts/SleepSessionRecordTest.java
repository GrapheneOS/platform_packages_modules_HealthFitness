/*
 * Copyright (C) 2022 The Android Open Source Project
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

import static org.junit.Assert.fail;

import android.healthconnect.datatypes.SleepSessionRecord;

import org.junit.Test;

import java.time.Instant;
import java.util.List;

public class SleepSessionRecordTest {
    private static final Instant START_TIME = Instant.ofEpochMilli((long) 1e10);
    private static final Instant INTERMEDIATE_TIME = Instant.ofEpochMilli((long) 1e10 + 300);
    private static final Instant INTERMEDIATE_TIME2 = Instant.ofEpochMilli((long) 1e10 + 700);
    private static final Instant END_TIME = Instant.ofEpochMilli((long) 1e10 + 1000);
    private static final CharSequence NOTES = "felt sleepy";
    private static final CharSequence TITLE = "Afternoon nap";

    @Test(expected = IllegalArgumentException.class)
    public void testSleepStage_startTimeLaterThanEnd_throwsException() {
        new SleepSessionRecord.Stage(
                END_TIME, START_TIME, SleepSessionRecord.StageType.STAGE_TYPE_AWAKE);
        fail("Must throw exception if sleep stage start time is after end time.");
    }

    @Test
    public void testSleepStage_buildStage_equalsIsCorrect() {
        SleepSessionRecord.Stage stage1 =
                new SleepSessionRecord.Stage(
                        START_TIME, END_TIME, SleepSessionRecord.StageType.STAGE_TYPE_AWAKE);
        SleepSessionRecord.Stage stage2 =
                new SleepSessionRecord.Stage(
                        START_TIME, END_TIME, SleepSessionRecord.StageType.STAGE_TYPE_AWAKE);
        assertThat(stage1).isEqualTo(stage2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSleepSession_sleepStageEndTimeIllegal_throwsException() {
        new SleepSessionRecord.Builder(
                        TestUtils.generateMetadata(),
                        START_TIME,
                        INTERMEDIATE_TIME,
                        List.of(
                                new SleepSessionRecord.Stage(
                                        START_TIME,
                                        END_TIME,
                                        SleepSessionRecord.StageType.STAGE_TYPE_SLEEPING_LIGHT)))
                .build();
        fail("Must throw an exception if sleep stage end time is later than session end time.");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSleepSession_stagesOverlap_throwsException() {
        new SleepSessionRecord.Builder(
                        TestUtils.generateMetadata(),
                        START_TIME,
                        END_TIME,
                        List.of(
                                new SleepSessionRecord.Stage(
                                        START_TIME,
                                        INTERMEDIATE_TIME2,
                                        SleepSessionRecord.StageType.STAGE_TYPE_SLEEPING_LIGHT),
                                new SleepSessionRecord.Stage(
                                        INTERMEDIATE_TIME,
                                        END_TIME,
                                        SleepSessionRecord.StageType.STAGE_TYPE_SLEEPING_LIGHT)))
                .build();
        fail("Must throw an exception if sleep stages overlap.");
    }

    @Test
    public void testSleepSession_buildStage_gettersAreCorrect() {
        SleepSessionRecord.Stage stage1 =
                new SleepSessionRecord.Stage(
                        START_TIME, END_TIME, SleepSessionRecord.StageType.STAGE_TYPE_AWAKE);
        assertThat(stage1.getType()).isEqualTo(SleepSessionRecord.StageType.STAGE_TYPE_AWAKE);
        assertThat(stage1.getStartTime()).isEqualTo(START_TIME);
        assertThat(stage1.getEndTime()).isEqualTo(END_TIME);
    }

    @Test
    public void testSleepSession_addStagesOneByOne_objectsAreEqual() {
        SleepSessionRecord.Stage stage1 =
                new SleepSessionRecord.Stage(
                        START_TIME,
                        INTERMEDIATE_TIME,
                        SleepSessionRecord.StageType.STAGE_TYPE_AWAKE);
        SleepSessionRecord.Stage stage2 =
                new SleepSessionRecord.Stage(
                        INTERMEDIATE_TIME,
                        END_TIME,
                        SleepSessionRecord.StageType.STAGE_TYPE_SLEEPING_DEEP);
        SleepSessionRecord record =
                new SleepSessionRecord.Builder(
                                TestUtils.generateMetadata(), START_TIME, END_TIME, List.of())
                        .addStage(stage1)
                        .addStage(stage2)
                        .build();
        assertThat(record.getStages()).isEqualTo(List.of(stage1, stage2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSleepSession_sleepStageStartTimeIllegal_throwsException() {
        new SleepSessionRecord.Builder(
                        TestUtils.generateMetadata(),
                        INTERMEDIATE_TIME,
                        END_TIME,
                        List.of(
                                new SleepSessionRecord.Stage(
                                        START_TIME,
                                        END_TIME,
                                        SleepSessionRecord.StageType.STAGE_TYPE_SLEEPING_LIGHT)))
                .build();
        fail(
                "Must throw an exception if sleep stage start time is earlier than session start"
                        + " time.");
    }

    @Test
    public void testSleepSession_buildSession_buildsCorrectObject() {
        SleepSessionRecord record =
                new SleepSessionRecord.Builder(
                                TestUtils.generateMetadata(),
                                START_TIME,
                                END_TIME,
                                List.of(
                                        new SleepSessionRecord.Stage(
                                                START_TIME,
                                                INTERMEDIATE_TIME,
                                                SleepSessionRecord.StageType
                                                        .STAGE_TYPE_SLEEPING_LIGHT),
                                        new SleepSessionRecord.Stage(
                                                INTERMEDIATE_TIME,
                                                END_TIME,
                                                SleepSessionRecord.StageType
                                                        .STAGE_TYPE_SLEEPING_DEEP)))
                        .setNotes(NOTES)
                        .setTitle(TITLE)
                        .build();
        assertThat(record.getStartTime()).isEqualTo(START_TIME);
        assertThat(record.getEndTime()).isEqualTo(END_TIME);
        assertThat(record.getStages()).hasSize(2);
        assertThat(CharSequence.compare(record.getTitle(), TITLE)).isEqualTo(0);
        assertThat(CharSequence.compare(record.getNotes(), NOTES)).isEqualTo(0);
    }
}
