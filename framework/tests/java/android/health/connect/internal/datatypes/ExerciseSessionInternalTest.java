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

package android.health.connect.internal.datatypes;

import static android.health.connect.Constants.DEFAULT_FLOAT;
import static android.health.connect.Constants.DEFAULT_INT;

import static com.android.healthfitness.flags.Flags.FLAG_EXERCISE_SEGMENT_IMPROVEMENTS;

import static com.google.common.truth.Truth.assertThat;

import android.health.connect.datatypes.ExerciseLap;
import android.health.connect.datatypes.ExerciseRoute;
import android.health.connect.datatypes.ExerciseSegment;
import android.health.connect.datatypes.ExerciseSessionRecord;
import android.healthconnect.testing.shared.StringUtil;
import android.healthconnect.testing.unittest.RecordInternalFactory;
import android.os.Parcel;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class ExerciseSessionInternalTest {

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Test
    @EnableFlags({FLAG_EXERCISE_SEGMENT_IMPROVEMENTS})
    public void testSessionConvertToExternal_convertToExternalWithRPE_fieldsAreEqual() {
        ExerciseSessionRecordInternal session =
                RecordInternalFactory.buildExerciseSessionInternalWithRpe();
        ExerciseSessionRecord externalSession = session.toExternalRecord();
        assertFieldsAreEqual(externalSession, session);
    }

    @Test
    public void testSessionConvertToExternal_convertToExternal_fieldsIsEqual() {
        ExerciseSessionRecordInternal session =
                RecordInternalFactory.buildExerciseSessionInternal();
        ExerciseSessionRecord externalSession = session.toExternalRecord();
        assertFieldsAreEqual(externalSession, session);
    }

    @Test
    public void testSessionConvertToExternal_convertToExternalNoExtra_fieldsIsEqual() {
        ExerciseSessionRecordInternal session =
                RecordInternalFactory.buildExerciseSessionInternalNoExtraFields();
        ExerciseSessionRecord externalSession = session.toExternalRecord();
        assertFieldsAreEqual(externalSession, session);
    }

    @Test
    @EnableFlags({FLAG_EXERCISE_SEGMENT_IMPROVEMENTS})
    public void
            testSessionWriteToParcel_populateToParcelAndFromWithRpe_restoredFieldsAreIdentical() {
        ExerciseSessionRecordInternal session =
                RecordInternalFactory.buildExerciseSessionInternalWithRpe();
        ExerciseSessionRecordInternal restoredSession = writeAndRestoreFromParcel(session);

        assertFieldsAreEqual(session, restoredSession);
    }

    @Test
    public void testSessionWriteToParcel_populateToParcelAndFrom_restoredFieldsAreIdentical() {
        ExerciseSessionRecordInternal session =
                RecordInternalFactory.buildExerciseSessionInternal();
        ExerciseSessionRecordInternal restoredSession = writeAndRestoreFromParcel(session);

        assertFieldsAreEqual(session, restoredSession);
    }

    @Test
    public void
            testSessionWriteToParcel_populateToParcelAndFromNoExtra_restoredFieldsAreIdentical() {
        ExerciseSessionRecordInternal session =
                RecordInternalFactory.buildExerciseSessionInternalNoExtraFields();
        ExerciseSessionRecordInternal restoredSession = writeAndRestoreFromParcel(session);

        assertFieldsAreEqual(session, restoredSession);
    }

    @Test
    public void testSessionHashCode_getHashCode_noExceptions() {
        int unused =
                RecordInternalFactory.buildExerciseSessionInternal().toExternalRecord().hashCode();
    }

    private ExerciseSessionRecordInternal writeAndRestoreFromParcel(
            ExerciseSessionRecordInternal session) {
        Parcel parcel = Parcel.obtain();
        session.writeToParcel(parcel);
        parcel.setDataPosition(0);
        ExerciseSessionRecordInternal restoredSession = new ExerciseSessionRecordInternal(parcel);
        parcel.recycle();
        return restoredSession;
    }

    private void assertFieldsAreEqual(
            ExerciseSessionRecord external, ExerciseSessionRecordInternal internal) {
        assertThat(internal.getStartTimeInMillis())
                .isEqualTo(external.getStartTime().toEpochMilli());
        assertThat(internal.getEndTimeInMillis()).isEqualTo(external.getEndTime().toEpochMilli());
        assertThat(internal.getStartZoneOffsetInSeconds())
                .isEqualTo(external.getStartZoneOffset().getTotalSeconds());
        assertThat(internal.getEndZoneOffsetInSeconds())
                .isEqualTo(external.getEndZoneOffset().getTotalSeconds());
        if (external.hasRateOfPerceivedExertion()) {
            assertThat(internal.getRateOfPerceivedExertion())
                    .isEqualTo(external.getRateOfPerceivedExertion());
        } else {
            assertThat(internal.getRateOfPerceivedExertion()).isEqualTo(DEFAULT_FLOAT);
        }
        if (internal.getRoute() == null) {
            assertThat(external.getRoute()).isNull();
        } else {
            ExerciseRoute convertedRoute = internal.getRoute().toExternalRoute();
            assertThat(external.getRoute().getRouteLocations())
                    .isEqualTo(convertedRoute.getRouteLocations());
            assertThat(external.getRoute()).isEqualTo(convertedRoute);
        }
        StringUtil.assertCharSequencesEqualToStringWithNull(
                internal.getTitle(), external.getTitle());
        StringUtil.assertCharSequencesEqualToStringWithNull(
                internal.getNotes(), external.getNotes());
        assertLapsAreEqual(internal.getLaps(), external.getLaps());
        assertSegmentsAreEqual(internal.getSegments(), external.getSegments());
    }

    private void assertLapsAreEqual(
            List<ExerciseLapInternal> internalLaps, List<ExerciseLap> externalLaps) {
        if (internalLaps == null) {
            assertThat(externalLaps).isEmpty();
            return;
        }

        assertThat(internalLaps.size()).isEqualTo(externalLaps.size());
        for (int i = 0; i < internalLaps.size(); i++) {
            ExerciseLapInternal internalLap = internalLaps.get(i);
            ExerciseLap externalLap = externalLaps.get(i);
            assertThat(internalLap.getStartTime())
                    .isEqualTo(externalLap.getStartTime().toEpochMilli());
            assertThat(internalLap.getEndTime()).isEqualTo(externalLap.getEndTime().toEpochMilli());
            assertThat(internalLap.getLength()).isEqualTo(externalLap.getLength().getInMeters());
        }
    }

    private void assertSegmentsAreEqual(
            List<ExerciseSegmentInternal> internalSegments,
            List<ExerciseSegment> externalSegments) {
        if (internalSegments == null) {
            assertThat(externalSegments).isEmpty();
            return;
        }

        assertThat(internalSegments.size()).isEqualTo(externalSegments.size());
        for (int i = 0; i < internalSegments.size(); i++) {
            ExerciseSegmentInternal internalSegment = internalSegments.get(i);
            ExerciseSegment externalSegment = externalSegments.get(i);
            assertThat(internalSegment.getStartTime())
                    .isEqualTo(externalSegment.getStartTime().toEpochMilli());
            assertThat(internalSegment.getEndTime())
                    .isEqualTo(externalSegment.getEndTime().toEpochMilli());
            assertThat(internalSegment.getSegmentType())
                    .isEqualTo(externalSegment.getSegmentType());
            assertThat(internalSegment.getRepetitionsCount())
                    .isEqualTo(externalSegment.getRepetitionsCount());
            if (externalSegment.getWeight() != null) {
                assertThat(internalSegment.getWeightGrams())
                        .isEqualTo(externalSegment.getWeight().getInGrams());
            } else {
                assertThat(internalSegment.getWeightGrams()).isEqualTo(null);
            }
            if (externalSegment.hasSetIndex()) {
                assertThat(internalSegment.getSetIndex()).isEqualTo(externalSegment.getSetIndex());
            } else {
                assertThat(internalSegment.getSetIndex()).isEqualTo(DEFAULT_INT);
            }
            if (externalSegment.hasSetIndex()) {
                assertThat(internalSegment.getRateOfPerceivedExertion())
                        .isEqualTo(externalSegment.getRateOfPerceivedExertion());
            } else {
                assertThat(internalSegment.getRateOfPerceivedExertion()).isEqualTo(DEFAULT_FLOAT);
            }
        }
    }

    private void assertFieldsAreEqual(
            ExerciseSessionRecordInternal internal, ExerciseSessionRecordInternal internal2) {
        assertThat(internal.getStartTimeInMillis()).isEqualTo(internal2.getStartTimeInMillis());
        assertThat(internal.getEndTimeInMillis()).isEqualTo(internal2.getEndTimeInMillis());
        assertThat(internal.getStartZoneOffsetInSeconds())
                .isEqualTo(internal2.getStartZoneOffsetInSeconds());
        assertThat(internal.getEndZoneOffsetInSeconds())
                .isEqualTo(internal2.getEndZoneOffsetInSeconds());
        assertThat(internal.getRateOfPerceivedExertion())
                .isEqualTo(internal2.getRateOfPerceivedExertion());
        assertThat(internal.getRoute()).isEqualTo(internal2.getRoute());
        assertThat(internal.getExerciseType()).isEqualTo(internal2.getExerciseType());
        StringUtil.assertCharSequencesEqualToStringWithNull(
                internal.getNotes(), internal2.getNotes());
        StringUtil.assertCharSequencesEqualToStringWithNull(
                internal.getTitle(), internal2.getTitle());
    }
}
