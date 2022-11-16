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

package android.healthconnect.internal.datatypes.utils;

import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_ACTIVE_CALORIES_BURNED;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_BASAL_METABOLIC_RATE;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_CYCLING_PEDALING_CADENCE;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_DISTANCE;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_ELEVATION_GAINED;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_EXERCISE_EVENT;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_EXERCISE_LAP;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_EXERCISE_REPETITIONS;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_EXERCISE_SESSION;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_FLOORS_CLIMBED;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_HEART_RATE;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_HYDRATION;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_NUTRITION;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_POWER;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_SLEEP_SESSION;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_SLEEP_STAGE;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_SPEED;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_STEPS;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_STEPS_CADENCE;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_SWIMMING_STROKES;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_TOTAL_CALORIES_BURNED;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_WHEELCHAIR_PUSHES;

import android.annotation.SuppressLint;
import android.healthconnect.HealthDataCategory;
import android.healthconnect.HealthPermissionCategory;
import android.healthconnect.datatypes.RecordTypeIdentifier;
import android.util.SparseIntArray;

import java.util.Objects;

/** @hide */
public final class RecordTypePermissionCategoryMapper {

    private static final String TAG = "RecordTypePermissionGroupMapper";
    private static SparseIntArray sRecordTypeHealthPermissionCategoryMap = new SparseIntArray();

    private RecordTypePermissionCategoryMapper() {}

    private static void populateDataTypeHealthPermissionCategoryMap() {
        sRecordTypeHealthPermissionCategoryMap =
                new SparseIntArray() {
                    {
                        put(RECORD_TYPE_STEPS, HealthPermissionCategory.STEPS);
                        put(RECORD_TYPE_HEART_RATE, HealthPermissionCategory.HEART_RATE);
                        put(
                                RECORD_TYPE_BASAL_METABOLIC_RATE,
                                HealthPermissionCategory.BASAL_METABOLIC_RATE);
                        put(
                                RECORD_TYPE_CYCLING_PEDALING_CADENCE,
                                HealthPermissionCategory.EXERCISE);
                        put(RECORD_TYPE_POWER, HealthPermissionCategory.POWER);
                        put(RECORD_TYPE_SPEED, HealthPermissionCategory.SPEED);
                        put(RECORD_TYPE_STEPS_CADENCE, HealthPermissionCategory.STEPS);
                        put(RECORD_TYPE_DISTANCE, HealthPermissionCategory.DISTANCE);
                        put(
                                RECORD_TYPE_WHEELCHAIR_PUSHES,
                                HealthPermissionCategory.WHEELCHAIR_PUSHES);
                        put(RECORD_TYPE_EXERCISE_SESSION, HealthPermissionCategory.EXERCISE);
                        put(
                                RECORD_TYPE_TOTAL_CALORIES_BURNED,
                                HealthPermissionCategory.TOTAL_CALORIES_BURNED);
                        put(RECORD_TYPE_SWIMMING_STROKES, HealthPermissionCategory.EXERCISE);
                        put(RECORD_TYPE_FLOORS_CLIMBED, HealthPermissionCategory.FLOORS_CLIMBED);
                        put(
                                RECORD_TYPE_ELEVATION_GAINED,
                                HealthPermissionCategory.ELEVATION_GAINED);
                        put(
                                RECORD_TYPE_ACTIVE_CALORIES_BURNED,
                                HealthPermissionCategory.ACTIVE_CALORIES_BURNED);
                        put(RECORD_TYPE_HYDRATION, HealthPermissionCategory.HYDRATION);
                        put(RECORD_TYPE_SLEEP_SESSION, HealthPermissionCategory.SLEEP);
                        put(RECORD_TYPE_EXERCISE_REPETITIONS, HealthPermissionCategory.EXERCISE);
                        put(RECORD_TYPE_SLEEP_STAGE, HealthPermissionCategory.SLEEP);
                        put(RECORD_TYPE_EXERCISE_EVENT, HealthPermissionCategory.EXERCISE);
                        put(RECORD_TYPE_EXERCISE_LAP, HealthPermissionCategory.EXERCISE);
                        put(RECORD_TYPE_NUTRITION, HealthPermissionCategory.NUTRITION);
                    }
                };
    }

    /**
     * Returns {@link HealthDataCategory} for the input {@link
     * android.healthconnect.datatypes.RecordTypeIdentifier.RecordType}.
     */
    @SuppressLint("LongLogTag")
    @HealthPermissionCategory.Type
    public static int getHealthPermissionCategoryForRecordType(
            @RecordTypeIdentifier.RecordType int recordType) {
        if (sRecordTypeHealthPermissionCategoryMap.size() == 0) {
            populateDataTypeHealthPermissionCategoryMap();
        }
        @HealthPermissionCategory.Type
        Integer recordPermission = sRecordTypeHealthPermissionCategoryMap.get(recordType);
        Objects.requireNonNull(
                recordPermission,
                "Health Permission Category not found for " + "RecordType :" + recordType);
        return recordPermission;
    }
}
