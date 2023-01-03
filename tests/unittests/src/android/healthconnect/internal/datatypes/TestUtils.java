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

import android.healthconnect.datatypes.ExerciseSessionType;

import java.util.List;

public class TestUtils {

    public static ExerciseRouteInternal.LocationInternal buildInternalLocationAllFields() {
        return new ExerciseRouteInternal.LocationInternal()
                .setTime((long) 1e10)
                .setLatitude(60.321)
                .setLongitude(59.123)
                .setVerticalAccuracy(1.2)
                .setHorizontalAccuracy(20)
                .setAltitude(-12);
    }

    public static ExerciseRouteInternal.LocationInternal buildInternalLocation() {
        return new ExerciseRouteInternal.LocationInternal()
                .setTime((long) 1e10)
                .setLatitude(60.321)
                .setLongitude(59.123);
    }

    public static ExerciseRouteInternal buildExerciseRouteInternal() {
        return new ExerciseRouteInternal(
                List.of(buildInternalLocationAllFields(), buildInternalLocation()));
    }

    public static ExerciseSessionRecordInternal buildExerciseSessionInternal() {
        return (ExerciseSessionRecordInternal)
                new ExerciseSessionRecordInternal()
                        .setExerciseType(
                                ExerciseSessionType.EXERCISE_SESSION_TYPE_FOOTBALL_AMERICAN)
                        .setRoute(buildExerciseRouteInternal())
                        .setStartTime((long) 1e9)
                        .setEndTime((long) 1e10)
                        .setEndZoneOffset(1)
                        .setStartZoneOffset(1)
                        .setAppInfoId(1)
                        .setClientRecordId("client_id")
                        .setManufacturer("manufacturer")
                        .setClientRecordVersion(12)
                        .setUuid("id")
                        .setPackageName("android.healthconnect.unittests")
                        .setModel("Pixel4a");
    }

    public static ExerciseSessionRecordInternal buildExerciseSessionInternalNoExtraFields() {
        return (ExerciseSessionRecordInternal)
                new ExerciseSessionRecordInternal()
                        .setExerciseType(
                                ExerciseSessionType.EXERCISE_SESSION_TYPE_FOOTBALL_AMERICAN)
                        .setStartTime((long) 1e9)
                        .setEndTime((long) 1e10)
                        .setUuid("id");
    }
}
