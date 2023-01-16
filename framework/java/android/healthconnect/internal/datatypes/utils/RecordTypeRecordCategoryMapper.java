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

import static android.healthconnect.HealthDataCategory.ACTIVITY;
import static android.healthconnect.HealthDataCategory.BODY_MEASUREMENTS;
import static android.healthconnect.HealthDataCategory.CYCLE_TRACKING;
import static android.healthconnect.HealthDataCategory.NUTRITION;
import static android.healthconnect.HealthDataCategory.VITALS;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_ACTIVE_CALORIES_BURNED;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_BASAL_BODY_TEMPERATURE;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_BASAL_METABOLIC_RATE;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_BLOOD_GLUCOSE;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_BLOOD_PRESSURE;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_BODY_FAT;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_BODY_TEMPERATURE;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_BONE_MASS;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_CERVICAL_MUCUS;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_CYCLING_PEDALING_CADENCE;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_DISTANCE;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_ELEVATION_GAINED;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_FLOORS_CLIMBED;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_HEART_RATE;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_HEIGHT;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_HYDRATION;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_LEAN_BODY_MASS;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_MENSTRUATION_FLOW;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_NUTRITION;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_OVULATION_TEST;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_OXYGEN_SATURATION;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_POWER;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_RESPIRATORY_RATE;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_RESTING_HEART_RATE;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_SEXUAL_ACTIVITY;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_SPEED;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_STEPS;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_STEPS_CADENCE;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_TOTAL_CALORIES_BURNED;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_VO2_MAX;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_WEIGHT;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_WHEELCHAIR_PUSHES;

import android.annotation.SuppressLint;
import android.healthconnect.HealthDataCategory;
import android.healthconnect.datatypes.RecordTypeIdentifier;
import android.util.SparseIntArray;

import java.util.Objects;

/** @hide */
public final class RecordTypeRecordCategoryMapper {

    private static SparseIntArray sRecordTypeToRecordCategoryMapper = new SparseIntArray();

    private RecordTypeRecordCategoryMapper() {}

    private static void populateRecordTypeToRecordCategoryMap() {
        sRecordTypeToRecordCategoryMapper =
                new SparseIntArray() {
                    {
                        put(RECORD_TYPE_STEPS, ACTIVITY);
                        put(RECORD_TYPE_HEART_RATE, VITALS);
                        put(RECORD_TYPE_BASAL_METABOLIC_RATE, BODY_MEASUREMENTS);
                        put(RECORD_TYPE_CYCLING_PEDALING_CADENCE, ACTIVITY);
                        put(RECORD_TYPE_POWER, ACTIVITY);
                        put(RECORD_TYPE_SPEED, ACTIVITY);
                        put(RECORD_TYPE_STEPS_CADENCE, ACTIVITY);
                        put(RECORD_TYPE_DISTANCE, ACTIVITY);
                        put(RECORD_TYPE_WHEELCHAIR_PUSHES, ACTIVITY);
                        put(RECORD_TYPE_TOTAL_CALORIES_BURNED, ACTIVITY);
                        put(RECORD_TYPE_FLOORS_CLIMBED, ACTIVITY);
                        put(RECORD_TYPE_ELEVATION_GAINED, ACTIVITY);
                        put(RECORD_TYPE_ACTIVE_CALORIES_BURNED, ACTIVITY);
                        put(RECORD_TYPE_HYDRATION, NUTRITION);
                        put(RECORD_TYPE_NUTRITION, NUTRITION);
                        put(RECORD_TYPE_RESPIRATORY_RATE, VITALS);
                        put(RECORD_TYPE_BONE_MASS, BODY_MEASUREMENTS);
                        put(RECORD_TYPE_RESTING_HEART_RATE, VITALS);
                        put(RECORD_TYPE_BODY_FAT, BODY_MEASUREMENTS);
                        put(RECORD_TYPE_VO2_MAX, ACTIVITY);
                        put(RECORD_TYPE_CERVICAL_MUCUS, CYCLE_TRACKING);
                        put(RECORD_TYPE_BASAL_BODY_TEMPERATURE, VITALS);
                        put(RECORD_TYPE_MENSTRUATION_FLOW, CYCLE_TRACKING);
                        put(RECORD_TYPE_OXYGEN_SATURATION, VITALS);
                        put(RECORD_TYPE_BLOOD_PRESSURE, VITALS);
                        put(RECORD_TYPE_HEIGHT, BODY_MEASUREMENTS);
                        put(RECORD_TYPE_BLOOD_GLUCOSE, VITALS);
                        put(RECORD_TYPE_WEIGHT, BODY_MEASUREMENTS);
                        put(RECORD_TYPE_LEAN_BODY_MASS, BODY_MEASUREMENTS);
                        put(RECORD_TYPE_SEXUAL_ACTIVITY, CYCLE_TRACKING);
                        put(RECORD_TYPE_BODY_TEMPERATURE, VITALS);
                        put(RECORD_TYPE_OVULATION_TEST, CYCLE_TRACKING);
                    }
                };
    }

    /**
     * Returns {@link HealthDataCategory} for the input {@link
     * android.healthconnect.datatypes.RecordTypeIdentifier.RecordType}.
     */
    @SuppressLint("LongLogTag")
    @HealthDataCategory.Type
    public static int getRecordCategoryForRecordType(
            @RecordTypeIdentifier.RecordType int recordType) {
        if (sRecordTypeToRecordCategoryMapper.size() == 0) {
            populateRecordTypeToRecordCategoryMap();
        }
        @HealthDataCategory.Type
        Integer recordCategory = sRecordTypeToRecordCategoryMapper.get(recordType);
        Objects.requireNonNull(
                recordCategory, "Record Category not found for Record Type :" + recordType);
        return recordCategory;
    }
}
