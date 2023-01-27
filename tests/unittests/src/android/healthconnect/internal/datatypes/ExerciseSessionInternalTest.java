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

package android.healthconnect.internal.datatypes;

import static com.google.common.truth.Truth.assertThat;

import android.health.connect.datatypes.ExerciseRoute;
import android.health.connect.datatypes.ExerciseSessionRecord;
import android.health.connect.datatypes.RecordUtils;
import android.health.connect.internal.datatypes.ExerciseSessionRecordInternal;
import android.os.Parcel;

import org.junit.Test;

public class ExerciseSessionInternalTest {
    @Test
    public void testSessionConvertToExternal_convertToExternal_fieldsIsEqual() {
        ExerciseSessionRecordInternal session = TestUtils.buildExerciseSessionInternal();
        ExerciseSessionRecord externalSession = session.toExternalRecord();
        assertFieldsAreEqual(externalSession, session);
    }

    @Test
    public void testSessionConvertToExternal_convertToExternalNoExtra_fieldsIsEqual() {
        ExerciseSessionRecordInternal session =
                TestUtils.buildExerciseSessionInternalNoExtraFields();
        ExerciseSessionRecord externalSession = session.toExternalRecord();
        assertFieldsAreEqual(externalSession, session);
    }

    @Test
    public void testSessionWriteToParcel_populateToParcelAndFrom_restoredFieldsAreIdentical() {
        ExerciseSessionRecordInternal session = TestUtils.buildExerciseSessionInternal();
        ExerciseSessionRecordInternal restoredSession = writeAndRestoreFromParcel(session);

        assertFieldsAreEqual(session, restoredSession);
        assertThat(session).isEqualTo(restoredSession);
    }

    @Test
    public void
            testSessionWriteToParcel_populateToParcelAndFromNoExtra_restoredFieldsAreIdentical() {
        ExerciseSessionRecordInternal session =
                TestUtils.buildExerciseSessionInternalNoExtraFields();
        ExerciseSessionRecordInternal restoredSession = writeAndRestoreFromParcel(session);

        assertFieldsAreEqual(session, restoredSession);
        assertThat(session).isEqualTo(restoredSession);
    }

    @Test
    public void testSessionHashCode_getHashCode_noExceptionsAndNotNull() {
        assertThat(TestUtils.buildExerciseSessionInternal().toExternalRecord().hashCode())
                .isNotNull();
    }

    private ExerciseSessionRecordInternal writeAndRestoreFromParcel(
            ExerciseSessionRecordInternal session) {
        Parcel parcel = Parcel.obtain();
        session.populateIntervalRecordTo(parcel);
        parcel.setDataPosition(0);
        ExerciseSessionRecordInternal restoredSession = new ExerciseSessionRecordInternal();
        restoredSession.populateIntervalRecordFrom(parcel);
        return restoredSession;
    }

    private void assertFieldsAreEqual(
            ExerciseSessionRecord external, ExerciseSessionRecordInternal internal) {
        if (internal.getRoute() == null) {
            assertThat(external.getRoute()).isNull();
        } else {
            ExerciseRoute convertedRoute = internal.getRoute().toExternalRoute();
            assertThat(external.getRoute().getRouteLocations())
                    .isEqualTo(convertedRoute.getRouteLocations());
            assertThat(external.getRoute()).isEqualTo(convertedRoute);
        }
        assertCharSequencesEqualToStringWithNull(internal.getTitle(), external.getTitle());
        assertCharSequencesEqualToStringWithNull(internal.getTitle(), external.getTitle());
    }

    private void assertCharSequencesEqualToStringWithNull(String str, CharSequence sequence) {
        if (str == null) {
            assertThat(sequence).isNull();
        } else {
            assertThat(sequence).isNotNull();
            assertThat(sequence.toString()).isEqualTo(str);
        }
    }

    private void assertFieldsAreEqual(
            ExerciseSessionRecordInternal internal, ExerciseSessionRecordInternal internal2) {
        assertThat(internal.getRoute()).isEqualTo(internal2.getRoute());
        assertThat(internal.getExerciseType()).isEqualTo(internal2.getExerciseType());
        assertThat(
                        RecordUtils.isEqualNullableCharSequences(
                                internal.getTitle(), internal2.getTitle()))
                .isTrue();
        assertThat(
                        RecordUtils.isEqualNullableCharSequences(
                                internal.getTitle(), internal2.getNotes()))
                .isTrue();
    }
}
