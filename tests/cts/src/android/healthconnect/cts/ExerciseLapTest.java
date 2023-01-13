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

import android.healthconnect.datatypes.ExerciseLap;
import android.healthconnect.datatypes.units.Length;

import org.junit.Test;

import java.time.Instant;

public class ExerciseLapTest {
    private static final Instant START_TIME = Instant.ofEpochMilli((long) 1e1);
    private static final Instant END_TIME = Instant.ofEpochMilli((long) 1e2);

    @Test
    public void testExerciseLap_buildLap_buildCorrectObject() {
        ExerciseLap lap =
                new ExerciseLap.Builder(START_TIME, END_TIME)
                        .setLength(Length.fromMeters(12))
                        .build();
        assertThat(lap.getStartTime()).isEqualTo(START_TIME);
        assertThat(lap.getEndTime()).isEqualTo(END_TIME);
        assertThat(lap.getLength()).isEqualTo(Length.fromMeters(12));
    }

    @Test
    public void testExerciseLap_buildWithoutLength_lengthIsNull() {
        ExerciseLap lap = new ExerciseLap.Builder(START_TIME, END_TIME).build();
        assertThat(lap.getLength()).isNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExerciseLap_endTimeEarlierThanStartTime_throwsException() {
        new ExerciseLap.Builder(END_TIME, START_TIME).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExerciseLap_lengthIsNegative_throwsException() {
        new ExerciseLap.Builder(START_TIME, END_TIME).setLength(Length.fromMeters(-10)).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExerciseLap_lengthIsHuge_throwsException() {
        new ExerciseLap.Builder(START_TIME, END_TIME).setLength(Length.fromMeters(1e10)).build();
    }
}