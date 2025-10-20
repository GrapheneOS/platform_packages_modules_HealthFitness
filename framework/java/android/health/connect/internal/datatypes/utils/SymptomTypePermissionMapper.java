/*
 * Copyright (C) 2025 The Android Open Source Project
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
package android.health.connect.internal.datatypes.utils;

import static android.health.connect.datatypes.SymptomRecord.SymptomType;

import android.health.connect.HealthPermissions;
import android.health.connect.datatypes.SymptomRecord;

import java.util.Set;

/**
 * A helper class to map symptom types to their corresponding permissions.
 *
 * @hide
 */
public final class SymptomTypePermissionMapper {

    /** Returns the read permission for the given symptom type. */
    public static String getReadPermission(@SymptomType int symptomType) {
        switch (symptomType) {
            case SymptomRecord.SYMPTOM_TYPE_ABDOMINAL_PAIN:
                return HealthPermissions.READ_SYMPTOM_ABDOMINAL_PAIN;
            case SymptomRecord.SYMPTOM_TYPE_ACNE:
                return HealthPermissions.READ_SYMPTOM_ACNE;
            case SymptomRecord.SYMPTOM_TYPE_BACK_PAIN:
                return HealthPermissions.READ_SYMPTOM_BACK_PAIN;
            case SymptomRecord.SYMPTOM_TYPE_BLOATING:
                return HealthPermissions.READ_SYMPTOM_BLOATING;
            case SymptomRecord.SYMPTOM_TYPE_BRAIN_FOG:
                return HealthPermissions.READ_SYMPTOM_BRAIN_FOG;
            case SymptomRecord.SYMPTOM_TYPE_BREAST_TENDERNESS:
                return HealthPermissions.READ_SYMPTOM_BREAST_TENDERNESS;
            case SymptomRecord.SYMPTOM_TYPE_BRITTLE_NAILS:
                return HealthPermissions.READ_SYMPTOM_BRITTLE_NAILS;
            case SymptomRecord.SYMPTOM_TYPE_BURNING_MOUTH:
                return HealthPermissions.READ_SYMPTOM_BURNING_MOUTH;
            case SymptomRecord.SYMPTOM_TYPE_CHEST_PAIN:
                return HealthPermissions.READ_SYMPTOM_CHEST_PAIN;
            case SymptomRecord.SYMPTOM_TYPE_CHEST_TIGHTNESS:
                return HealthPermissions.READ_SYMPTOM_CHEST_TIGHTNESS;
            case SymptomRecord.SYMPTOM_TYPE_CHILLS:
                return HealthPermissions.READ_SYMPTOM_CHILLS;
            case SymptomRecord.SYMPTOM_TYPE_CONSTIPATION:
                return HealthPermissions.READ_SYMPTOM_CONSTIPATION;
            case SymptomRecord.SYMPTOM_TYPE_COUGH:
                return HealthPermissions.READ_SYMPTOM_COUGH;
            case SymptomRecord.SYMPTOM_TYPE_CRAMPS:
                return HealthPermissions.READ_SYMPTOM_CRAMPS;
            case SymptomRecord.SYMPTOM_TYPE_CRAVINGS:
                return HealthPermissions.READ_SYMPTOM_CRAVINGS;
            case SymptomRecord.SYMPTOM_TYPE_DEHYDRATION:
                return HealthPermissions.READ_SYMPTOM_DEHYDRATION;
            case SymptomRecord.SYMPTOM_TYPE_DIARRHEA:
                return HealthPermissions.READ_SYMPTOM_DIARRHEA;
            case SymptomRecord.SYMPTOM_TYPE_DIFFICULTY_SWALLOWING:
                return HealthPermissions.READ_SYMPTOM_DIFFICULTY_SWALLOWING;
            case SymptomRecord.SYMPTOM_TYPE_DIZZINESS:
                return HealthPermissions.READ_SYMPTOM_DIZZINESS;
            case SymptomRecord.SYMPTOM_TYPE_DRY_SKIN:
                return HealthPermissions.READ_SYMPTOM_DRY_SKIN;
            case SymptomRecord.SYMPTOM_TYPE_EARACHES:
                return HealthPermissions.READ_SYMPTOM_EARACHES;
            case SymptomRecord.SYMPTOM_TYPE_FATIGUE:
                return HealthPermissions.READ_SYMPTOM_FATIGUE;
            case SymptomRecord.SYMPTOM_TYPE_FEVER:
                return HealthPermissions.READ_SYMPTOM_FEVER;
            case SymptomRecord.SYMPTOM_TYPE_GENERALIZED_BODY_ACHE:
                return HealthPermissions.READ_SYMPTOM_GENERALIZED_BODY_ACHE;
            case SymptomRecord.SYMPTOM_TYPE_HAIR_LOSS:
                return HealthPermissions.READ_SYMPTOM_HAIR_LOSS;
            case SymptomRecord.SYMPTOM_TYPE_HEADACHE:
                return HealthPermissions.READ_SYMPTOM_HEADACHE;
            case SymptomRecord.SYMPTOM_TYPE_HEARTBURN:
                return HealthPermissions.READ_SYMPTOM_HEARTBURN;
            case SymptomRecord.SYMPTOM_TYPE_HEART_PALPITATIONS:
                return HealthPermissions.READ_SYMPTOM_HEART_PALPITATIONS;
            case SymptomRecord.SYMPTOM_TYPE_HOT_FLASHES:
                return HealthPermissions.READ_SYMPTOM_HOT_FLASHES;
            case SymptomRecord.SYMPTOM_TYPE_INSOMNIA:
                return HealthPermissions.READ_SYMPTOM_INSOMNIA;
            case SymptomRecord.SYMPTOM_TYPE_JOINT_PAIN:
                return HealthPermissions.READ_SYMPTOM_JOINT_PAIN;
            case SymptomRecord.SYMPTOM_TYPE_JOINT_STIFFNESS:
                return HealthPermissions.READ_SYMPTOM_JOINT_STIFFNESS;
            case SymptomRecord.SYMPTOM_TYPE_LOSS_OF_APPETITE:
                return HealthPermissions.READ_SYMPTOM_LOSS_OF_APPETITE;
            case SymptomRecord.SYMPTOM_TYPE_LOSS_OF_CONSCIOUSNESS:
                return HealthPermissions.READ_SYMPTOM_LOSS_OF_CONSCIOUSNESS;
            case SymptomRecord.SYMPTOM_TYPE_LOWER_BACK_PAIN:
                return HealthPermissions.READ_SYMPTOM_LOWER_BACK_PAIN;
            case SymptomRecord.SYMPTOM_TYPE_MEMORY_LAPSE:
                return HealthPermissions.READ_SYMPTOM_MEMORY_LAPSE;
            case SymptomRecord.SYMPTOM_TYPE_MOOD_CHANGE:
                return HealthPermissions.READ_SYMPTOM_MOOD_CHANGE;
            case SymptomRecord.SYMPTOM_TYPE_MUSCLE_PAIN:
                return HealthPermissions.READ_SYMPTOM_MUSCLE_PAIN;
            case SymptomRecord.SYMPTOM_TYPE_NAUSEA:
                return HealthPermissions.READ_SYMPTOM_NAUSEA;
            case SymptomRecord.SYMPTOM_TYPE_NIGHT_SWEATS:
                return HealthPermissions.READ_SYMPTOM_NIGHT_SWEATS;
            case SymptomRecord.SYMPTOM_TYPE_PELVIC_PAIN:
                return HealthPermissions.READ_SYMPTOM_PELVIC_PAIN;
            case SymptomRecord.SYMPTOM_TYPE_RAPID_POUNDING_OR_FLUTTERING_HEARTBEAT:
                return HealthPermissions.READ_SYMPTOM_RAPID_POUNDING_OR_FLUTTERING_HEARTBEAT;
            case SymptomRecord.SYMPTOM_TYPE_REDUCED_CAPACITY_FOR_EXERCISE:
                return HealthPermissions.READ_SYMPTOM_REDUCED_CAPACITY_FOR_EXERCISE;
            case SymptomRecord.SYMPTOM_TYPE_RUNNY_NOSE:
                return HealthPermissions.READ_SYMPTOM_RUNNY_NOSE;
            case SymptomRecord.SYMPTOM_TYPE_SHORTNESS_OF_BREATH:
                return HealthPermissions.READ_SYMPTOM_SHORTNESS_OF_BREATH;
            case SymptomRecord.SYMPTOM_TYPE_SKIPPED_HEARTBEAT:
                return HealthPermissions.READ_SYMPTOM_SKIPPED_HEARTBEAT;
            case SymptomRecord.SYMPTOM_TYPE_SLEEP_CHANGES:
                return HealthPermissions.READ_SYMPTOM_SLEEP_CHANGES;
            case SymptomRecord.SYMPTOM_TYPE_SLEEPINESS:
                return HealthPermissions.READ_SYMPTOM_SLEEPINESS;
            case SymptomRecord.SYMPTOM_TYPE_SNEEZING:
                return HealthPermissions.READ_SYMPTOM_SNEEZING;
            case SymptomRecord.SYMPTOM_TYPE_SNORE:
                return HealthPermissions.READ_SYMPTOM_SNORE;
            case SymptomRecord.SYMPTOM_TYPE_SORE_THROAT:
                return HealthPermissions.READ_SYMPTOM_SORE_THROAT;
            case SymptomRecord.SYMPTOM_TYPE_STOMACH_ACHE:
                return HealthPermissions.READ_SYMPTOM_STOMACH_ACHE;
            case SymptomRecord.SYMPTOM_TYPE_STUFFY_NOSE:
                return HealthPermissions.READ_SYMPTOM_STUFFY_NOSE;
            case SymptomRecord.SYMPTOM_TYPE_UNEXPLAINED_WEIGHT_CHANGES:
                return HealthPermissions.READ_SYMPTOM_UNEXPLAINED_WEIGHT_CHANGES;
            case SymptomRecord.SYMPTOM_TYPE_VAGINAL_DRYNESS:
                return HealthPermissions.READ_SYMPTOM_VAGINAL_DRYNESS;
            case SymptomRecord.SYMPTOM_TYPE_VAGINAL_ITCHINESS:
                return HealthPermissions.READ_SYMPTOM_VAGINAL_ITCHINESS;
            case SymptomRecord.SYMPTOM_TYPE_VOMITING:
                return HealthPermissions.READ_SYMPTOM_VOMITING;
            case SymptomRecord.SYMPTOM_TYPE_WATER_RETENTION:
                return HealthPermissions.READ_SYMPTOM_WATER_RETENTION;
            case SymptomRecord.SYMPTOM_TYPE_WHEEZING:
                return HealthPermissions.READ_SYMPTOM_WHEEZING;
            default:
                throw new IllegalArgumentException("Invalid symptom type: " + symptomType);
        }
    }

    /** Returns the write permission for the given symptom type. */
    public static String getWritePermission(@SymptomType int symptomType) {
        switch (symptomType) {
            case SymptomRecord.SYMPTOM_TYPE_ABDOMINAL_PAIN:
                return HealthPermissions.WRITE_SYMPTOM_ABDOMINAL_PAIN;
            case SymptomRecord.SYMPTOM_TYPE_ACNE:
                return HealthPermissions.WRITE_SYMPTOM_ACNE;
            case SymptomRecord.SYMPTOM_TYPE_BACK_PAIN:
                return HealthPermissions.WRITE_SYMPTOM_BACK_PAIN;
            case SymptomRecord.SYMPTOM_TYPE_BLOATING:
                return HealthPermissions.WRITE_SYMPTOM_BLOATING;
            case SymptomRecord.SYMPTOM_TYPE_BRAIN_FOG:
                return HealthPermissions.WRITE_SYMPTOM_BRAIN_FOG;
            case SymptomRecord.SYMPTOM_TYPE_BREAST_TENDERNESS:
                return HealthPermissions.WRITE_SYMPTOM_BREAST_TENDERNESS;
            case SymptomRecord.SYMPTOM_TYPE_BRITTLE_NAILS:
                return HealthPermissions.WRITE_SYMPTOM_BRITTLE_NAILS;
            case SymptomRecord.SYMPTOM_TYPE_BURNING_MOUTH:
                return HealthPermissions.WRITE_SYMPTOM_BURNING_MOUTH;
            case SymptomRecord.SYMPTOM_TYPE_CHEST_PAIN:
                return HealthPermissions.WRITE_SYMPTOM_CHEST_PAIN;
            case SymptomRecord.SYMPTOM_TYPE_CHEST_TIGHTNESS:
                return HealthPermissions.WRITE_SYMPTOM_CHEST_TIGHTNESS;
            case SymptomRecord.SYMPTOM_TYPE_CHILLS:
                return HealthPermissions.WRITE_SYMPTOM_CHILLS;
            case SymptomRecord.SYMPTOM_TYPE_CONSTIPATION:
                return HealthPermissions.WRITE_SYMPTOM_CONSTIPATION;
            case SymptomRecord.SYMPTOM_TYPE_COUGH:
                return HealthPermissions.WRITE_SYMPTOM_COUGH;
            case SymptomRecord.SYMPTOM_TYPE_CRAMPS:
                return HealthPermissions.WRITE_SYMPTOM_CRAMPS;
            case SymptomRecord.SYMPTOM_TYPE_CRAVINGS:
                return HealthPermissions.WRITE_SYMPTOM_CRAVINGS;
            case SymptomRecord.SYMPTOM_TYPE_DEHYDRATION:
                return HealthPermissions.WRITE_SYMPTOM_DEHYDRATION;
            case SymptomRecord.SYMPTOM_TYPE_DIARRHEA:
                return HealthPermissions.WRITE_SYMPTOM_DIARRHEA;
            case SymptomRecord.SYMPTOM_TYPE_DIFFICULTY_SWALLOWING:
                return HealthPermissions.WRITE_SYMPTOM_DIFFICULTY_SWALLOWING;
            case SymptomRecord.SYMPTOM_TYPE_DIZZINESS:
                return HealthPermissions.WRITE_SYMPTOM_DIZZINESS;
            case SymptomRecord.SYMPTOM_TYPE_DRY_SKIN:
                return HealthPermissions.WRITE_SYMPTOM_DRY_SKIN;
            case SymptomRecord.SYMPTOM_TYPE_EARACHES:
                return HealthPermissions.WRITE_SYMPTOM_EARACHES;
            case SymptomRecord.SYMPTOM_TYPE_FATIGUE:
                return HealthPermissions.WRITE_SYMPTOM_FATIGUE;
            case SymptomRecord.SYMPTOM_TYPE_FEVER:
                return HealthPermissions.WRITE_SYMPTOM_FEVER;
            case SymptomRecord.SYMPTOM_TYPE_GENERALIZED_BODY_ACHE:
                return HealthPermissions.WRITE_SYMPTOM_GENERALIZED_BODY_ACHE;
            case SymptomRecord.SYMPTOM_TYPE_HAIR_LOSS:
                return HealthPermissions.WRITE_SYMPTOM_HAIR_LOSS;
            case SymptomRecord.SYMPTOM_TYPE_HEADACHE:
                return HealthPermissions.WRITE_SYMPTOM_HEADACHE;
            case SymptomRecord.SYMPTOM_TYPE_HEARTBURN:
                return HealthPermissions.WRITE_SYMPTOM_HEARTBURN;
            case SymptomRecord.SYMPTOM_TYPE_HEART_PALPITATIONS:
                return HealthPermissions.WRITE_SYMPTOM_HEART_PALPITATIONS;
            case SymptomRecord.SYMPTOM_TYPE_HOT_FLASHES:
                return HealthPermissions.WRITE_SYMPTOM_HOT_FLASHES;
            case SymptomRecord.SYMPTOM_TYPE_INSOMNIA:
                return HealthPermissions.WRITE_SYMPTOM_INSOMNIA;
            case SymptomRecord.SYMPTOM_TYPE_JOINT_PAIN:
                return HealthPermissions.WRITE_SYMPTOM_JOINT_PAIN;
            case SymptomRecord.SYMPTOM_TYPE_JOINT_STIFFNESS:
                return HealthPermissions.WRITE_SYMPTOM_JOINT_STIFFNESS;
            case SymptomRecord.SYMPTOM_TYPE_LOSS_OF_APPETITE:
                return HealthPermissions.WRITE_SYMPTOM_LOSS_OF_APPETITE;
            case SymptomRecord.SYMPTOM_TYPE_LOSS_OF_CONSCIOUSNESS:
                return HealthPermissions.WRITE_SYMPTOM_LOSS_OF_CONSCIOUSNESS;
            case SymptomRecord.SYMPTOM_TYPE_LOWER_BACK_PAIN:
                return HealthPermissions.WRITE_SYMPTOM_LOWER_BACK_PAIN;
            case SymptomRecord.SYMPTOM_TYPE_MEMORY_LAPSE:
                return HealthPermissions.WRITE_SYMPTOM_MEMORY_LAPSE;
            case SymptomRecord.SYMPTOM_TYPE_MOOD_CHANGE:
                return HealthPermissions.WRITE_SYMPTOM_MOOD_CHANGE;
            case SymptomRecord.SYMPTOM_TYPE_MUSCLE_PAIN:
                return HealthPermissions.WRITE_SYMPTOM_MUSCLE_PAIN;
            case SymptomRecord.SYMPTOM_TYPE_NAUSEA:
                return HealthPermissions.WRITE_SYMPTOM_NAUSEA;
            case SymptomRecord.SYMPTOM_TYPE_NIGHT_SWEATS:
                return HealthPermissions.WRITE_SYMPTOM_NIGHT_SWEATS;
            case SymptomRecord.SYMPTOM_TYPE_PELVIC_PAIN:
                return HealthPermissions.WRITE_SYMPTOM_PELVIC_PAIN;
            case SymptomRecord.SYMPTOM_TYPE_RAPID_POUNDING_OR_FLUTTERING_HEARTBEAT:
                return HealthPermissions.WRITE_SYMPTOM_RAPID_POUNDING_OR_FLUTTERING_HEARTBEAT;
            case SymptomRecord.SYMPTOM_TYPE_REDUCED_CAPACITY_FOR_EXERCISE:
                return HealthPermissions.WRITE_SYMPTOM_REDUCED_CAPACITY_FOR_EXERCISE;
            case SymptomRecord.SYMPTOM_TYPE_RUNNY_NOSE:
                return HealthPermissions.WRITE_SYMPTOM_RUNNY_NOSE;
            case SymptomRecord.SYMPTOM_TYPE_SHORTNESS_OF_BREATH:
                return HealthPermissions.WRITE_SYMPTOM_SHORTNESS_OF_BREATH;
            case SymptomRecord.SYMPTOM_TYPE_SKIPPED_HEARTBEAT:
                return HealthPermissions.WRITE_SYMPTOM_SKIPPED_HEARTBEAT;
            case SymptomRecord.SYMPTOM_TYPE_SLEEP_CHANGES:
                return HealthPermissions.WRITE_SYMPTOM_SLEEP_CHANGES;
            case SymptomRecord.SYMPTOM_TYPE_SLEEPINESS:
                return HealthPermissions.WRITE_SYMPTOM_SLEEPINESS;
            case SymptomRecord.SYMPTOM_TYPE_SNEEZING:
                return HealthPermissions.WRITE_SYMPTOM_SNEEZING;
            case SymptomRecord.SYMPTOM_TYPE_SNORE:
                return HealthPermissions.WRITE_SYMPTOM_SNORE;
            case SymptomRecord.SYMPTOM_TYPE_SORE_THROAT:
                return HealthPermissions.WRITE_SYMPTOM_SORE_THROAT;
            case SymptomRecord.SYMPTOM_TYPE_STOMACH_ACHE:
                return HealthPermissions.WRITE_SYMPTOM_STOMACH_ACHE;
            case SymptomRecord.SYMPTOM_TYPE_STUFFY_NOSE:
                return HealthPermissions.WRITE_SYMPTOM_STUFFY_NOSE;
            case SymptomRecord.SYMPTOM_TYPE_UNEXPLAINED_WEIGHT_CHANGES:
                return HealthPermissions.WRITE_SYMPTOM_UNEXPLAINED_WEIGHT_CHANGES;
            case SymptomRecord.SYMPTOM_TYPE_VAGINAL_DRYNESS:
                return HealthPermissions.WRITE_SYMPTOM_VAGINAL_DRYNESS;
            case SymptomRecord.SYMPTOM_TYPE_VAGINAL_ITCHINESS:
                return HealthPermissions.WRITE_SYMPTOM_VAGINAL_ITCHINESS;
            case SymptomRecord.SYMPTOM_TYPE_VOMITING:
                return HealthPermissions.WRITE_SYMPTOM_VOMITING;
            case SymptomRecord.SYMPTOM_TYPE_WATER_RETENTION:
                return HealthPermissions.WRITE_SYMPTOM_WATER_RETENTION;
            case SymptomRecord.SYMPTOM_TYPE_WHEEZING:
                return HealthPermissions.WRITE_SYMPTOM_WHEEZING;
            default:
                throw new IllegalArgumentException("Invalid symptom type: " + symptomType);
        }
    }

    /** Returns the set of all symptom types. */
    public static Set<Integer> getSymptomTypes() {
        return SymptomRecord.VALID_SYMPTOM_TYPES;
    }

    private SymptomTypePermissionMapper() {}
}
