/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * ```
 *      http://www.apache.org/licenses/LICENSE-2.0
 * ```
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.permissions.data

import androidx.annotation.StringRes
import com.android.healthconnect.controller.R

data class FitnessPermissionStrings(
    @StringRes val uppercaseLabel: Int,
    @StringRes val lowercaseLabel: Int,
    @StringRes val readContentDescription: Int,
    @StringRes val writeContentDescription: Int,
) {
    companion object {
        fun fromPermissionType(
            fitnessPermissionType: FitnessPermissionType
        ): FitnessPermissionStrings {
            return PERMISSION_TYPE_STRINGS[fitnessPermissionType]
                ?: throw IllegalArgumentException(
                    "No strings for permission group " + fitnessPermissionType.name
                )
        }
    }
}

private val PERMISSION_TYPE_STRINGS: Map<FitnessPermissionType, FitnessPermissionStrings> =
    mapOf(
        FitnessPermissionType.ACTIVE_CALORIES_BURNED to
            FitnessPermissionStrings(
                R.string.active_calories_burned_uppercase_label,
                R.string.active_calories_burned_lowercase_label,
                R.string.active_calories_burned_read_content_description,
                R.string.active_calories_burned_write_content_description,
            ),
        FitnessPermissionType.ACTIVITY_INTENSITY to
            FitnessPermissionStrings(
                R.string.activity_intensity_uppercase_label,
                R.string.activity_intensity_lowercase_label,
                R.string.activity_intensity_read_content_description,
                R.string.activity_intensity_write_content_description,
            ),
        FitnessPermissionType.ALCOHOL_CONSUMPTION to
            FitnessPermissionStrings(
                R.string.alcohol_consumption_uppercase_label,
                R.string.alcohol_consumption_lowercase_label,
                R.string.alcohol_consumption_read_content_description,
                R.string.alcohol_consumption_write_content_description,
            ),
        FitnessPermissionType.DISTANCE to
            FitnessPermissionStrings(
                R.string.distance_uppercase_label,
                R.string.distance_lowercase_label,
                R.string.distance_read_content_description,
                R.string.distance_write_content_description,
            ),
        FitnessPermissionType.ELEVATION_GAINED to
            FitnessPermissionStrings(
                R.string.elevation_gained_uppercase_label,
                R.string.elevation_gained_lowercase_label,
                R.string.elevation_gained_read_content_description,
                R.string.elevation_gained_write_content_description,
            ),
        FitnessPermissionType.EXERCISE to
            FitnessPermissionStrings(
                R.string.exercise_uppercase_label,
                R.string.exercise_lowercase_label,
                R.string.exercise_read_content_description,
                R.string.exercise_write_content_description,
            ),
        FitnessPermissionType.SPEED to
            FitnessPermissionStrings(
                R.string.speed_uppercase_label,
                R.string.speed_lowercase_label,
                R.string.speed_read_content_description,
                R.string.speed_write_content_description,
            ),
        FitnessPermissionType.POWER to
            FitnessPermissionStrings(
                R.string.power_uppercase_label,
                R.string.power_lowercase_label,
                R.string.power_read_content_description,
                R.string.power_write_content_description,
            ),
        FitnessPermissionType.FLOORS_CLIMBED to
            FitnessPermissionStrings(
                R.string.floors_climbed_uppercase_label,
                R.string.floors_climbed_lowercase_label,
                R.string.floors_climbed_read_content_description,
                R.string.floors_climbed_write_content_description,
            ),
        FitnessPermissionType.INTERMENSTRUAL_BLEEDING to
            FitnessPermissionStrings(
                R.string.spotting_uppercase_label,
                R.string.spotting_lowercase_label,
                R.string.spotting_read_content_description,
                R.string.spotting_write_content_description,
            ),
        FitnessPermissionType.STEPS to
            FitnessPermissionStrings(
                R.string.steps_uppercase_label,
                R.string.steps_lowercase_label,
                R.string.steps_read_content_description,
                R.string.steps_write_content_description,
            ),
        FitnessPermissionType.TOTAL_CALORIES_BURNED to
            FitnessPermissionStrings(
                R.string.total_calories_burned_uppercase_label,
                R.string.total_calories_burned_lowercase_label,
                R.string.total_calories_burned_read_content_description,
                R.string.total_calories_burned_write_content_description,
            ),
        FitnessPermissionType.VO2_MAX to
            FitnessPermissionStrings(
                R.string.vo2_max_uppercase_label,
                R.string.vo2_max_lowercase_label,
                R.string.vo2_max_read_content_description,
                R.string.vo2_max_write_content_description,
            ),
        FitnessPermissionType.WHEELCHAIR_PUSHES to
            FitnessPermissionStrings(
                R.string.wheelchair_pushes_uppercase_label,
                R.string.wheelchair_pushes_lowercase_label,
                R.string.wheelchair_pushes_read_content_description,
                R.string.wheelchair_pushes_write_content_description,
            ),
        FitnessPermissionType.BASAL_METABOLIC_RATE to
            FitnessPermissionStrings(
                R.string.basal_metabolic_rate_uppercase_label,
                R.string.basal_metabolic_rate_lowercase_label,
                R.string.basal_metabolic_rate_read_content_description,
                R.string.basal_metabolic_rate_write_content_description,
            ),
        FitnessPermissionType.BODY_FAT to
            FitnessPermissionStrings(
                R.string.body_fat_uppercase_label,
                R.string.body_fat_lowercase_label,
                R.string.body_fat_read_content_description,
                R.string.body_fat_write_content_description,
            ),
        FitnessPermissionType.BODY_WATER_MASS to
            FitnessPermissionStrings(
                R.string.body_water_mass_uppercase_label,
                R.string.body_water_mass_lowercase_label,
                R.string.body_water_mass_read_content_description,
                R.string.body_water_mass_write_content_description,
            ),
        FitnessPermissionType.BONE_MASS to
            FitnessPermissionStrings(
                R.string.bone_mass_uppercase_label,
                R.string.bone_mass_lowercase_label,
                R.string.bone_mass_read_content_description,
                R.string.bone_mass_write_content_description,
            ),
        FitnessPermissionType.HEIGHT to
            FitnessPermissionStrings(
                R.string.height_uppercase_label,
                R.string.height_lowercase_label,
                R.string.height_read_content_description,
                R.string.height_write_content_description,
            ),
        FitnessPermissionType.LEAN_BODY_MASS to
            FitnessPermissionStrings(
                R.string.lean_body_mass_uppercase_label,
                R.string.lean_body_mass_lowercase_label,
                R.string.lean_body_mass_read_content_description,
                R.string.lean_body_mass_write_content_description,
            ),
        FitnessPermissionType.WEIGHT to
            FitnessPermissionStrings(
                R.string.weight_uppercase_label,
                R.string.weight_lowercase_label,
                R.string.weight_read_content_description,
                R.string.weight_write_content_description,
            ),
        FitnessPermissionType.CERVICAL_MUCUS to
            FitnessPermissionStrings(
                R.string.cervical_mucus_uppercase_label,
                R.string.cervical_mucus_lowercase_label,
                R.string.cervical_mucus_read_content_description,
                R.string.cervical_mucus_write_content_description,
            ),
        FitnessPermissionType.MENSTRUATION to
            FitnessPermissionStrings(
                R.string.menstruation_uppercase_label,
                R.string.menstruation_lowercase_label,
                R.string.menstruation_read_content_description,
                R.string.menstruation_write_content_description,
            ),
        FitnessPermissionType.OVULATION_TEST to
            FitnessPermissionStrings(
                R.string.ovulation_test_uppercase_label,
                R.string.ovulation_test_lowercase_label,
                R.string.ovulation_test_read_content_description,
                R.string.ovulation_test_write_content_description,
            ),
        FitnessPermissionType.SEXUAL_ACTIVITY to
            FitnessPermissionStrings(
                R.string.sexual_activity_uppercase_label,
                R.string.sexual_activity_lowercase_label,
                R.string.sexual_activity_read_content_description,
                R.string.sexual_activity_write_content_description,
            ),
        FitnessPermissionType.HYDRATION to
            FitnessPermissionStrings(
                R.string.hydration_uppercase_label,
                R.string.hydration_lowercase_label,
                R.string.hydration_read_content_description,
                R.string.hydration_write_content_description,
            ),
        FitnessPermissionType.NUTRITION to
            FitnessPermissionStrings(
                R.string.nutrition_uppercase_label,
                R.string.nutrition_lowercase_label,
                R.string.nutrition_read_content_description,
                R.string.nutrition_write_content_description,
            ),
        FitnessPermissionType.SLEEP to
            FitnessPermissionStrings(
                R.string.sleep_uppercase_label,
                R.string.sleep_lowercase_label,
                R.string.sleep_read_content_description,
                R.string.sleep_write_content_description,
            ),
        FitnessPermissionType.BASAL_BODY_TEMPERATURE to
            FitnessPermissionStrings(
                R.string.basal_body_temperature_uppercase_label,
                R.string.basal_body_temperature_lowercase_label,
                R.string.basal_body_temperature_read_content_description,
                R.string.basal_body_temperature_write_content_description,
            ),
        FitnessPermissionType.BLOOD_GLUCOSE to
            FitnessPermissionStrings(
                R.string.blood_glucose_uppercase_label,
                R.string.blood_glucose_lowercase_label,
                R.string.blood_glucose_read_content_description,
                R.string.blood_glucose_write_content_description,
            ),
        FitnessPermissionType.BLOOD_PRESSURE to
            FitnessPermissionStrings(
                R.string.blood_pressure_uppercase_label,
                R.string.blood_pressure_lowercase_label,
                R.string.blood_pressure_read_content_description,
                R.string.blood_pressure_write_content_description,
            ),
        FitnessPermissionType.BODY_TEMPERATURE to
            FitnessPermissionStrings(
                R.string.body_temperature_uppercase_label,
                R.string.body_temperature_lowercase_label,
                R.string.body_temperature_read_content_description,
                R.string.body_temperature_write_content_description,
            ),
        FitnessPermissionType.HEART_RATE to
            FitnessPermissionStrings(
                R.string.heart_rate_uppercase_label,
                R.string.heart_rate_lowercase_label,
                R.string.heart_rate_read_content_description,
                R.string.heart_rate_write_content_description,
            ),
        FitnessPermissionType.HEART_RATE_VARIABILITY to
            FitnessPermissionStrings(
                R.string.heart_rate_variability_uppercase_label,
                R.string.heart_rate_variability_lowercase_label,
                R.string.heart_rate_variability_read_content_description,
                R.string.heart_rate_variability_write_content_description,
            ),
        FitnessPermissionType.OXYGEN_SATURATION to
            FitnessPermissionStrings(
                R.string.oxygen_saturation_uppercase_label,
                R.string.oxygen_saturation_lowercase_label,
                R.string.oxygen_saturation_read_content_description,
                R.string.oxygen_saturation_write_content_description,
            ),
        FitnessPermissionType.RESPIRATORY_RATE to
            FitnessPermissionStrings(
                R.string.respiratory_rate_uppercase_label,
                R.string.respiratory_rate_lowercase_label,
                R.string.respiratory_rate_read_content_description,
                R.string.respiratory_rate_write_content_description,
            ),
        FitnessPermissionType.RESTING_HEART_RATE to
            FitnessPermissionStrings(
                R.string.resting_heart_rate_uppercase_label,
                R.string.resting_heart_rate_lowercase_label,
                R.string.resting_heart_rate_read_content_description,
                R.string.resting_heart_rate_write_content_description,
            ),
        FitnessPermissionType.SKIN_TEMPERATURE to
            FitnessPermissionStrings(
                R.string.skin_temperature_uppercase_label,
                R.string.skin_temperature_lowercase_label,
                R.string.skin_temperature_read_content_description,
                R.string.skin_temperature_write_content_description,
            ),
        FitnessPermissionType.EXERCISE_ROUTE to
            FitnessPermissionStrings(
                R.string.exercise_route_uppercase_label,
                R.string.exercise_route_lowercase_label,
                R.string.exercise_route_read_content_description,
                R.string.exercise_route_write_content_description,
            ),
        FitnessPermissionType.PLANNED_EXERCISE to
            FitnessPermissionStrings(
                R.string.planned_exercise_uppercase_label,
                R.string.planned_exercise_lowercase_label,
                R.string.planned_exercise_read_content_description,
                R.string.planned_exercise_write_content_description,
            ),
        FitnessPermissionType.MINDFULNESS to
            FitnessPermissionStrings(
                R.string.mindfulness_uppercase_label,
                R.string.mindfulness_lowercase_label,
                R.string.mindfulness_read_content_description,
                R.string.mindfulness_write_content_description,
            ),
        FitnessPermissionType.NICOTINE_INTAKE to
            FitnessPermissionStrings(
                R.string.nicotine_intake_uppercase_label,
                R.string.nicotine_intake_lowercase_label,
                R.string.nicotine_intake_read_content_description,
                R.string.nicotine_intake_write_content_description,
            ),
        FitnessPermissionType.SYMPTOM_COUGH to
            FitnessPermissionStrings(
                R.string.symptom_cough_uppercase_label,
                R.string.symptom_cough_lowercase_label,
                R.string.symptom_cough_read_content_description,
                R.string.symptom_cough_write_content_description,
            ),
        FitnessPermissionType.SYMPTOM_ABDOMINAL_PAIN to
            FitnessPermissionStrings(
                R.string.abdominal_pain_uppercase_label,
                R.string.abdominal_pain_lowercase_label,
                R.string.abdominal_pain_read_content_description,
                R.string.abdominal_pain_write_content_description,
            ),
        FitnessPermissionType.SYMPTOM_ACNE to
            FitnessPermissionStrings(
                R.string.acne_uppercase_label,
                R.string.acne_lowercase_label,
                R.string.acne_read_content_description,
                R.string.acne_write_content_description,
            ),
        FitnessPermissionType.SYMPTOM_BACK_PAIN to
            FitnessPermissionStrings(
                R.string.back_pain_uppercase_label,
                R.string.back_pain_lowercase_label,
                R.string.back_pain_read_content_description,
                R.string.back_pain_write_content_description,
            ),
        FitnessPermissionType.SYMPTOM_BLOATING to
            FitnessPermissionStrings(
                R.string.bloating_uppercase_label,
                R.string.bloating_lowercase_label,
                R.string.bloating_read_content_description,
                R.string.bloating_write_content_description,
            ),
        FitnessPermissionType.SYMPTOM_BRAIN_FOG to
            FitnessPermissionStrings(
                R.string.brain_fog_uppercase_label,
                R.string.brain_fog_lowercase_label,
                R.string.brain_fog_read_content_description,
                R.string.brain_fog_write_content_description,
            ),
        FitnessPermissionType.SYMPTOM_BREAST_TENDERNESS to
            FitnessPermissionStrings(
                R.string.breast_tenderness_uppercase_label,
                R.string.breast_tenderness_lowercase_label,
                R.string.breast_tenderness_read_content_description,
                R.string.breast_tenderness_write_content_description,
            ),
        FitnessPermissionType.SYMPTOM_BRITTLE_NAILS to
            FitnessPermissionStrings(
                R.string.brittle_nails_uppercase_label,
                R.string.brittle_nails_lowercase_label,
                R.string.brittle_nails_read_content_description,
                R.string.brittle_nails_write_content_description,
            ),
        FitnessPermissionType.SYMPTOM_BURNING_MOUTH to
            FitnessPermissionStrings(
                R.string.burning_mouth_uppercase_label,
                R.string.burning_mouth_lowercase_label,
                R.string.burning_mouth_read_content_description,
                R.string.burning_mouth_write_content_description,
            ),
        FitnessPermissionType.SYMPTOM_CHEST_PAIN to
            FitnessPermissionStrings(
                R.string.chest_pain_uppercase_label,
                R.string.chest_pain_lowercase_label,
                R.string.chest_pain_read_content_description,
                R.string.chest_pain_write_content_description,
            ),
        FitnessPermissionType.SYMPTOM_CHEST_TIGHTNESS to
            FitnessPermissionStrings(
                R.string.chest_tightness_uppercase_label,
                R.string.chest_tightness_lowercase_label,
                R.string.chest_tightness_read_content_description,
                R.string.chest_tightness_write_content_description,
            ),
        FitnessPermissionType.SYMPTOM_CHILLS to
            FitnessPermissionStrings(
                R.string.chills_uppercase_label,
                R.string.chills_lowercase_label,
                R.string.chills_read_content_description,
                R.string.chills_write_content_description,
            ),
        FitnessPermissionType.SYMPTOM_CONSTIPATION to
            FitnessPermissionStrings(
                R.string.constipation_uppercase_label,
                R.string.constipation_lowercase_label,
                R.string.constipation_read_content_description,
                R.string.constipation_write_content_description,
            ),
        FitnessPermissionType.SYMPTOM_CRAMPS to
            FitnessPermissionStrings(
                R.string.cramps_uppercase_label,
                R.string.cramps_lowercase_label,
                R.string.cramps_read_content_description,
                R.string.cramps_write_content_description,
            ),
        FitnessPermissionType.SYMPTOM_CRAVINGS to
            FitnessPermissionStrings(
                R.string.cravings_uppercase_label,
                R.string.cravings_lowercase_label,
                R.string.cravings_read_content_description,
                R.string.cravings_write_content_description,
            ),
        FitnessPermissionType.SYMPTOM_DEHYDRATION to
            FitnessPermissionStrings(
                R.string.dehydration_uppercase_label,
                R.string.dehydration_lowercase_label,
                R.string.dehydration_read_content_description,
                R.string.dehydration_write_content_description,
            ),
        FitnessPermissionType.SYMPTOM_DIARRHEA to
            FitnessPermissionStrings(
                R.string.diarrhea_uppercase_label,
                R.string.diarrhea_lowercase_label,
                R.string.diarrhea_read_content_description,
                R.string.diarrhea_write_content_description,
            ),
        FitnessPermissionType.SYMPTOM_DIFFICULTY_SWALLOWING to
            FitnessPermissionStrings(
                R.string.difficulty_swallowing_uppercase_label,
                R.string.difficulty_swallowing_lowercase_label,
                R.string.difficulty_swallowing_read_content_description,
                R.string.difficulty_swallowing_write_content_description,
            ),
        FitnessPermissionType.SYMPTOM_DIZZINESS to
            FitnessPermissionStrings(
                R.string.dizziness_uppercase_label,
                R.string.dizziness_lowercase_label,
                R.string.dizziness_read_content_description,
                R.string.dizziness_write_content_description,
            ),
        FitnessPermissionType.SYMPTOM_DRY_SKIN to
            FitnessPermissionStrings(
                R.string.dry_skin_uppercase_label,
                R.string.dry_skin_lowercase_label,
                R.string.dry_skin_read_content_description,
                R.string.dry_skin_write_content_description,
            ),
        FitnessPermissionType.SYMPTOM_EARACHES to
            FitnessPermissionStrings(
                R.string.earaches_uppercase_label,
                R.string.earaches_lowercase_label,
                R.string.earaches_read_content_description,
                R.string.earaches_write_content_description,
            ),
        FitnessPermissionType.SYMPTOM_FATIGUE to
            FitnessPermissionStrings(
                R.string.fatigue_uppercase_label,
                R.string.fatigue_lowercase_label,
                R.string.fatigue_read_content_description,
                R.string.fatigue_write_content_description,
            ),
        FitnessPermissionType.SYMPTOM_FEVER to
            FitnessPermissionStrings(
                R.string.fever_uppercase_label,
                R.string.fever_lowercase_label,
                R.string.fever_read_content_description,
                R.string.fever_write_content_description,
            ),
        FitnessPermissionType.SYMPTOM_GENERALIZED_BODY_ACHE to
            FitnessPermissionStrings(
                R.string.generalized_body_ache_uppercase_label,
                R.string.generalized_body_ache_lowercase_label,
                R.string.generalized_body_ache_read_content_description,
                R.string.generalized_body_ache_write_content_description,
            ),
        FitnessPermissionType.SYMPTOM_HAIR_LOSS to
            FitnessPermissionStrings(
                R.string.hair_loss_uppercase_label,
                R.string.hair_loss_lowercase_label,
                R.string.hair_loss_read_content_description,
                R.string.hair_loss_write_content_description,
            ),
        FitnessPermissionType.SYMPTOM_HEADACHE to
            FitnessPermissionStrings(
                R.string.headache_uppercase_label,
                R.string.headache_lowercase_label,
                R.string.headache_read_content_description,
                R.string.headache_write_content_description,
            ),
        FitnessPermissionType.SYMPTOM_HEARTBURN to
            FitnessPermissionStrings(
                R.string.heartburn_uppercase_label,
                R.string.heartburn_lowercase_label,
                R.string.heartburn_read_content_description,
                R.string.heartburn_write_content_description,
            ),
        FitnessPermissionType.SYMPTOM_HEART_PALPITATIONS to
            FitnessPermissionStrings(
                R.string.heart_palpitations_uppercase_label,
                R.string.heart_palpitations_lowercase_label,
                R.string.heart_palpitations_read_content_description,
                R.string.heart_palpitations_write_content_description,
            ),
        FitnessPermissionType.SYMPTOM_HOT_FLASHES to
            FitnessPermissionStrings(
                R.string.hot_flashes_uppercase_label,
                R.string.hot_flashes_lowercase_label,
                R.string.hot_flashes_read_content_description,
                R.string.hot_flashes_write_content_description,
            ),
        FitnessPermissionType.SYMPTOM_INSOMNIA to
            FitnessPermissionStrings(
                R.string.insomnia_uppercase_label,
                R.string.insomnia_lowercase_label,
                R.string.insomnia_read_content_description,
                R.string.insomnia_write_content_description,
            ),
        FitnessPermissionType.SYMPTOM_JOINT_PAIN to
            FitnessPermissionStrings(
                R.string.joint_pain_uppercase_label,
                R.string.joint_pain_lowercase_label,
                R.string.joint_pain_read_content_description,
                R.string.joint_pain_write_content_description,
            ),
        FitnessPermissionType.SYMPTOM_JOINT_STIFFNESS to
            FitnessPermissionStrings(
                R.string.joint_stiffness_uppercase_label,
                R.string.joint_stiffness_lowercase_label,
                R.string.joint_stiffness_read_content_description,
                R.string.joint_stiffness_write_content_description,
            ),
        FitnessPermissionType.SYMPTOM_LOSS_OF_APPETITE to
            FitnessPermissionStrings(
                R.string.loss_of_appetite_uppercase_label,
                R.string.loss_of_appetite_lowercase_label,
                R.string.loss_of_appetite_read_content_description,
                R.string.loss_of_appetite_write_content_description,
            ),
        FitnessPermissionType.SYMPTOM_LOSS_OF_CONSCIOUSNESS to
            FitnessPermissionStrings(
                R.string.loss_of_consciousness_uppercase_label,
                R.string.loss_of_consciousness_lowercase_label,
                R.string.loss_of_consciousness_read_content_description,
                R.string.loss_of_consciousness_write_content_description,
            ),
        FitnessPermissionType.SYMPTOM_LOWER_BACK_PAIN to
            FitnessPermissionStrings(
                R.string.lower_back_pain_uppercase_label,
                R.string.lower_back_pain_lowercase_label,
                R.string.lower_back_pain_read_content_description,
                R.string.lower_back_pain_write_content_description,
            ),
        FitnessPermissionType.SYMPTOM_MEMORY_LAPSE to
            FitnessPermissionStrings(
                R.string.memory_lapse_uppercase_label,
                R.string.memory_lapse_lowercase_label,
                R.string.memory_lapse_read_content_description,
                R.string.memory_lapse_write_content_description,
            ),
        FitnessPermissionType.SYMPTOM_MOOD_CHANGE to
            FitnessPermissionStrings(
                R.string.mood_change_uppercase_label,
                R.string.mood_change_lowercase_label,
                R.string.mood_change_read_content_description,
                R.string.mood_change_write_content_description,
            ),
        FitnessPermissionType.SYMPTOM_MUSCLE_PAIN to
            FitnessPermissionStrings(
                R.string.muscle_pain_uppercase_label,
                R.string.muscle_pain_lowercase_label,
                R.string.muscle_pain_read_content_description,
                R.string.muscle_pain_write_content_description,
            ),
        FitnessPermissionType.SYMPTOM_NAUSEA to
            FitnessPermissionStrings(
                R.string.nausea_uppercase_label,
                R.string.nausea_lowercase_label,
                R.string.nausea_read_content_description,
                R.string.nausea_write_content_description,
            ),
        FitnessPermissionType.SYMPTOM_NIGHT_SWEATS to
            FitnessPermissionStrings(
                R.string.night_sweats_uppercase_label,
                R.string.night_sweats_lowercase_label,
                R.string.night_sweats_read_content_description,
                R.string.night_sweats_write_content_description,
            ),
        FitnessPermissionType.SYMPTOM_PELVIC_PAIN to
            FitnessPermissionStrings(
                R.string.pelvic_pain_uppercase_label,
                R.string.pelvic_pain_lowercase_label,
                R.string.pelvic_pain_read_content_description,
                R.string.pelvic_pain_write_content_description,
            ),
        FitnessPermissionType.SYMPTOM_RAPID_POUNDING_OR_FLUTTERING_HEARTBEAT to
            FitnessPermissionStrings(
                R.string.rapid_pounding_or_fluttering_heartbeat_uppercase_label,
                R.string.rapid_pounding_or_fluttering_heartbeat_lowercase_label,
                R.string.rapid_pounding_or_fluttering_heartbeat_read_content_description,
                R.string.rapid_pounding_or_fluttering_heartbeat_write_content_description,
            ),
        FitnessPermissionType.SYMPTOM_REDUCED_CAPACITY_FOR_EXERCISE to
            FitnessPermissionStrings(
                R.string.reduced_capacity_for_exercise_uppercase_label,
                R.string.reduced_capacity_for_exercise_lowercase_label,
                R.string.reduced_capacity_for_exercise_read_content_description,
                R.string.reduced_capacity_for_exercise_write_content_description,
            ),
        FitnessPermissionType.SYMPTOM_RUNNY_NOSE to
            FitnessPermissionStrings(
                R.string.runny_nose_uppercase_label,
                R.string.runny_nose_lowercase_label,
                R.string.runny_nose_read_content_description,
                R.string.runny_nose_write_content_description,
            ),
        FitnessPermissionType.SYMPTOM_SHORTNESS_OF_BREATH to
            FitnessPermissionStrings(
                R.string.shortness_of_breath_uppercase_label,
                R.string.shortness_of_breath_lowercase_label,
                R.string.shortness_of_breath_read_content_description,
                R.string.shortness_of_breath_write_content_description,
            ),
        FitnessPermissionType.SYMPTOM_SKIPPED_HEARTBEAT to
            FitnessPermissionStrings(
                R.string.skipped_heartbeat_uppercase_label,
                R.string.skipped_heartbeat_lowercase_label,
                R.string.skipped_heartbeat_read_content_description,
                R.string.skipped_heartbeat_write_content_description,
            ),
        FitnessPermissionType.SYMPTOM_SLEEP_CHANGES to
            FitnessPermissionStrings(
                R.string.sleep_changes_uppercase_label,
                R.string.sleep_changes_lowercase_label,
                R.string.sleep_changes_read_content_description,
                R.string.sleep_changes_write_content_description,
            ),
        FitnessPermissionType.SYMPTOM_SLEEPINESS to
            FitnessPermissionStrings(
                R.string.sleepiness_uppercase_label,
                R.string.sleepiness_lowercase_label,
                R.string.sleepiness_read_content_description,
                R.string.sleepiness_write_content_description,
            ),
        FitnessPermissionType.SYMPTOM_SNEEZING to
            FitnessPermissionStrings(
                R.string.sneezing_uppercase_label,
                R.string.sneezing_lowercase_label,
                R.string.sneezing_read_content_description,
                R.string.sneezing_write_content_description,
            ),
        FitnessPermissionType.SYMPTOM_SNORE to
            FitnessPermissionStrings(
                R.string.snore_uppercase_label,
                R.string.snore_lowercase_label,
                R.string.snore_read_content_description,
                R.string.snore_write_content_description,
            ),
        FitnessPermissionType.SYMPTOM_SORE_THROAT to
            FitnessPermissionStrings(
                R.string.sore_throat_uppercase_label,
                R.string.sore_throat_lowercase_label,
                R.string.sore_throat_read_content_description,
                R.string.sore_throat_write_content_description,
            ),
        FitnessPermissionType.SYMPTOM_STOMACH_ACHE to
            FitnessPermissionStrings(
                R.string.stomach_ache_uppercase_label,
                R.string.stomach_ache_lowercase_label,
                R.string.stomach_ache_read_content_description,
                R.string.stomach_ache_write_content_description,
            ),
        FitnessPermissionType.SYMPTOM_STUFFY_NOSE to
            FitnessPermissionStrings(
                R.string.stuffy_nose_uppercase_label,
                R.string.stuffy_nose_lowercase_label,
                R.string.stuffy_nose_read_content_description,
                R.string.stuffy_nose_write_content_description,
            ),
        FitnessPermissionType.SYMPTOM_UNEXPLAINED_WEIGHT_CHANGES to
            FitnessPermissionStrings(
                R.string.unexplained_weight_changes_uppercase_label,
                R.string.unexplained_weight_changes_lowercase_label,
                R.string.unexplained_weight_changes_read_content_description,
                R.string.unexplained_weight_changes_write_content_description,
            ),
        FitnessPermissionType.SYMPTOM_VAGINAL_DRYNESS to
            FitnessPermissionStrings(
                R.string.vaginal_dryness_uppercase_label,
                R.string.vaginal_dryness_lowercase_label,
                R.string.vaginal_dryness_read_content_description,
                R.string.vaginal_dryness_write_content_description,
            ),
        FitnessPermissionType.SYMPTOM_VAGINAL_ITCHINESS to
            FitnessPermissionStrings(
                R.string.vaginal_itchiness_uppercase_label,
                R.string.vaginal_itchiness_lowercase_label,
                R.string.vaginal_itchiness_read_content_description,
                R.string.vaginal_itchiness_write_content_description,
            ),
        FitnessPermissionType.SYMPTOM_VOMITING to
            FitnessPermissionStrings(
                R.string.vomiting_uppercase_label,
                R.string.vomiting_lowercase_label,
                R.string.vomiting_read_content_description,
                R.string.vomiting_write_content_description,
            ),
        FitnessPermissionType.SYMPTOM_WATER_RETENTION to
            FitnessPermissionStrings(
                R.string.water_retention_uppercase_label,
                R.string.water_retention_lowercase_label,
                R.string.water_retention_read_content_description,
                R.string.water_retention_write_content_description,
            ),
        FitnessPermissionType.SYMPTOM_WHEEZING to
            FitnessPermissionStrings(
                R.string.wheezing_uppercase_label,
                R.string.wheezing_lowercase_label,
                R.string.wheezing_read_content_description,
                R.string.wheezing_write_content_description,
            ),
        FitnessPermissionType.MENSTRUAL_CYCLE_PHASE to
            FitnessPermissionStrings(
                R.string.menstrual_cycle_phase_uppercase_label,
                R.string.menstrual_cycle_phase_lowercase_label,
                R.string.menstrual_cycle_phase_read_content_description,
                R.string.menstrual_cycle_phase_write_content_description,
            ),
    )
