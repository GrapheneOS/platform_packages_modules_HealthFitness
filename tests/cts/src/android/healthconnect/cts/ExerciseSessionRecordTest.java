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

import android.healthconnect.datatypes.Device;
import android.healthconnect.datatypes.ExerciseRoute;
import android.healthconnect.datatypes.ExerciseSessionRecord;
import android.healthconnect.datatypes.ExerciseSessionType;
import android.healthconnect.datatypes.Metadata;

import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.time.ZoneOffset;

@RunWith(AndroidJUnit4.class)
public class ExerciseSessionRecordTest {
    private static final Instant START_TIME = Instant.ofEpochMilli((long) 1e9);
    private static final Instant END_TIME = Instant.ofEpochMilli((long) 1e10);

    @Test
    public void testExerciseSession_buildSession_buildCorrectObject() {
        ExerciseSessionRecord record =
                new ExerciseSessionRecord.Builder(
                                generateMetadata(),
                                START_TIME,
                                END_TIME,
                                ExerciseSessionType.EXERCISE_SESSION_TYPE_FOOTBALL_AMERICAN)
                        .build();
        assertThat(record.getStartTime()).isEqualTo(START_TIME);
        assertThat(record.getEndTime()).isEqualTo(END_TIME);
        assertThat(record.hasRoute()).isFalse();
        assertThat(record.getRoute()).isNull();
        assertThat(record.getNotes()).isNull();
        assertThat(record.getTitle()).isNull();
    }

    @Test
    public void testExerciseSession_buildEqualSessions_equalsReturnsTrue() {
        Metadata metadata = generateMetadata();
        ExerciseSessionRecord record =
                new ExerciseSessionRecord.Builder(
                                metadata,
                                START_TIME,
                                END_TIME,
                                ExerciseSessionType.EXERCISE_SESSION_TYPE_BADMINTON)
                        .build();
        ExerciseSessionRecord record2 =
                new ExerciseSessionRecord.Builder(
                                metadata,
                                START_TIME,
                                END_TIME,
                                ExerciseSessionType.EXERCISE_SESSION_TYPE_BADMINTON)
                        .build();
        assertThat(record).isEqualTo(record2);
    }

    @Test
    public void testExerciseSession_buildSessionWithAllFields_buildCorrectObject() {
        ExerciseRoute route = TestUtils.buildExerciseRoute();
        CharSequence notes = "rain";
        CharSequence title = "Morning training";
        ExerciseSessionRecord record =
                new ExerciseSessionRecord.Builder(
                                generateMetadata(),
                                START_TIME,
                                END_TIME,
                                ExerciseSessionType.EXERCISE_SESSION_TYPE_FOOTBALL_AMERICAN)
                        .setRoute(route)
                        .setEndZoneOffset(ZoneOffset.MAX)
                        .setStartZoneOffset(ZoneOffset.MIN)
                        .setNotes(notes)
                        .setTitle(title)
                        .build();
        assertThat(record.hasRoute()).isTrue();
        assertThat(record.getRoute()).isEqualTo(route);
        assertThat(record.getEndZoneOffset()).isEqualTo(ZoneOffset.MAX);
        assertThat(record.getStartZoneOffset()).isEqualTo(ZoneOffset.MIN);
        assertThat(record.getNotes()).isEqualTo(notes);
        assertThat(record.getTitle()).isEqualTo(title);
    }

    private static Metadata generateMetadata() {
        return new Metadata.Builder()
                .setDevice(buildDevice())
                .setClientRecordId("ExerciseSession" + Math.random())
                .build();
    }

    private static Device buildDevice() {
        return new Device.Builder()
                .setManufacturer("google")
                .setModel("Pixel4a")
                .setType(2)
                .build();
    }
}
