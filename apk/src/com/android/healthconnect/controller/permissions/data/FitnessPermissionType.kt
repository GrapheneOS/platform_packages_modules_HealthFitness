/**
 * Copyright (C) 2022 The Android Open Source Project
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

import android.content.Context
import android.graphics.drawable.Drawable
import android.health.connect.HealthPermissionCategory
import com.android.healthconnect.controller.shared.HealthDataCategoryExtensions.fromFitnessPermissionType
import com.android.healthconnect.controller.shared.HealthDataCategoryExtensions.icon

// TODO (b/299880830) possibly rename "category" to something else
enum class FitnessPermissionType(val category: Int) : HealthPermissionType {
    // ACTIVITY
    ACTIVE_CALORIES_BURNED(HealthPermissionCategory.ACTIVE_CALORIES_BURNED),
    ACTIVITY_INTENSITY(HealthPermissionCategory.ACTIVITY_INTENSITY),
    DISTANCE(HealthPermissionCategory.DISTANCE),
    ELEVATION_GAINED(HealthPermissionCategory.ELEVATION_GAINED),
    EXERCISE(HealthPermissionCategory.EXERCISE),
    PLANNED_EXERCISE(HealthPermissionCategory.PLANNED_EXERCISE),
    FLOORS_CLIMBED(HealthPermissionCategory.FLOORS_CLIMBED),
    STEPS(HealthPermissionCategory.STEPS),
    TOTAL_CALORIES_BURNED(HealthPermissionCategory.TOTAL_CALORIES_BURNED),
    VO2_MAX(HealthPermissionCategory.VO2_MAX),
    WHEELCHAIR_PUSHES(HealthPermissionCategory.WHEELCHAIR_PUSHES),
    POWER(HealthPermissionCategory.POWER),
    SPEED(HealthPermissionCategory.SPEED),
    EXERCISE_ROUTE(HealthPermissionCategory.EXERCISE),

    // BODY_MEASUREMENTS
    BASAL_METABOLIC_RATE(HealthPermissionCategory.BASAL_METABOLIC_RATE),
    BODY_FAT(HealthPermissionCategory.BODY_FAT),
    BODY_WATER_MASS(HealthPermissionCategory.BODY_WATER_MASS),
    BONE_MASS(HealthPermissionCategory.BONE_MASS),
    HEIGHT(HealthPermissionCategory.HEIGHT),
    LEAN_BODY_MASS(HealthPermissionCategory.LEAN_BODY_MASS),
    WEIGHT(HealthPermissionCategory.WEIGHT),

    // CYCLE_TRACKING
    CERVICAL_MUCUS(HealthPermissionCategory.CERVICAL_MUCUS),
    MENSTRUAL_CYCLE_PHASE(HealthPermissionCategory.MENSTRUAL_CYCLE_PHASE),
    MENSTRUATION(HealthPermissionCategory.MENSTRUATION),
    OVULATION_TEST(HealthPermissionCategory.OVULATION_TEST),
    SEXUAL_ACTIVITY(HealthPermissionCategory.SEXUAL_ACTIVITY),
    INTERMENSTRUAL_BLEEDING(HealthPermissionCategory.INTERMENSTRUAL_BLEEDING),

    // NUTRITION
    HYDRATION(HealthPermissionCategory.HYDRATION),
    NUTRITION(HealthPermissionCategory.NUTRITION),

    // SLEEP
    SLEEP(HealthPermissionCategory.SLEEP),

    // VITALS
    BASAL_BODY_TEMPERATURE(HealthPermissionCategory.BASAL_BODY_TEMPERATURE),
    BLOOD_GLUCOSE(HealthPermissionCategory.BLOOD_GLUCOSE),
    BLOOD_PRESSURE(HealthPermissionCategory.BLOOD_PRESSURE),
    BODY_TEMPERATURE(HealthPermissionCategory.BODY_TEMPERATURE),
    HEART_RATE(HealthPermissionCategory.HEART_RATE),
    HEART_RATE_VARIABILITY(HealthPermissionCategory.HEART_RATE_VARIABILITY),
    OXYGEN_SATURATION(HealthPermissionCategory.OXYGEN_SATURATION),
    RESPIRATORY_RATE(HealthPermissionCategory.RESPIRATORY_RATE),
    RESTING_HEART_RATE(HealthPermissionCategory.RESTING_HEART_RATE),
    SKIN_TEMPERATURE(HealthPermissionCategory.SKIN_TEMPERATURE),

    // WELLNESS
    ALCOHOL_CONSUMPTION(HealthPermissionCategory.ALCOHOL_CONSUMPTION),
    MINDFULNESS(HealthPermissionCategory.MINDFULNESS),
    NICOTINE_INTAKE(HealthPermissionCategory.NICOTINE_INTAKE),

    // SYMPTOMS
    SYMPTOM_ABDOMINAL_PAIN(HealthPermissionCategory.SYMPTOM_ABDOMINAL_PAIN),
    SYMPTOM_ACNE(HealthPermissionCategory.SYMPTOM_ACNE),
    SYMPTOM_BACK_PAIN(HealthPermissionCategory.SYMPTOM_BACK_PAIN),
    SYMPTOM_BLOATING(HealthPermissionCategory.SYMPTOM_BLOATING),
    SYMPTOM_BRAIN_FOG(HealthPermissionCategory.SYMPTOM_BRAIN_FOG),
    SYMPTOM_BREAST_TENDERNESS(HealthPermissionCategory.SYMPTOM_BREAST_TENDERNESS),
    SYMPTOM_BRITTLE_NAILS(HealthPermissionCategory.SYMPTOM_BRITTLE_NAILS),
    SYMPTOM_BURNING_MOUTH(HealthPermissionCategory.SYMPTOM_BURNING_MOUTH),
    SYMPTOM_CHEST_PAIN(HealthPermissionCategory.SYMPTOM_CHEST_PAIN),
    SYMPTOM_CHEST_TIGHTNESS(HealthPermissionCategory.SYMPTOM_CHEST_TIGHTNESS),
    SYMPTOM_CHILLS(HealthPermissionCategory.SYMPTOM_CHILLS),
    SYMPTOM_CONSTIPATION(HealthPermissionCategory.SYMPTOM_CONSTIPATION),
    SYMPTOM_COUGH(HealthPermissionCategory.SYMPTOM_COUGH),
    SYMPTOM_CRAMPS(HealthPermissionCategory.SYMPTOM_CRAMPS),
    SYMPTOM_CRAVINGS(HealthPermissionCategory.SYMPTOM_CRAVINGS),
    SYMPTOM_DEHYDRATION(HealthPermissionCategory.SYMPTOM_DEHYDRATION),
    SYMPTOM_DIARRHEA(HealthPermissionCategory.SYMPTOM_DIARRHEA),
    SYMPTOM_DIFFICULTY_SWALLOWING(HealthPermissionCategory.SYMPTOM_DIFFICULTY_SWALLOWING),
    SYMPTOM_DIZZINESS(HealthPermissionCategory.SYMPTOM_DIZZINESS),
    SYMPTOM_DRY_SKIN(HealthPermissionCategory.SYMPTOM_DRY_SKIN),
    SYMPTOM_EARACHES(HealthPermissionCategory.SYMPTOM_EARACHES),
    SYMPTOM_FATIGUE(HealthPermissionCategory.SYMPTOM_FATIGUE),
    SYMPTOM_FEVER(HealthPermissionCategory.SYMPTOM_FEVER),
    SYMPTOM_GENERALIZED_BODY_ACHE(HealthPermissionCategory.SYMPTOM_GENERALIZED_BODY_ACHE),
    SYMPTOM_HAIR_LOSS(HealthPermissionCategory.SYMPTOM_HAIR_LOSS),
    SYMPTOM_HEADACHE(HealthPermissionCategory.SYMPTOM_HEADACHE),
    SYMPTOM_HEARTBURN(HealthPermissionCategory.SYMPTOM_HEARTBURN),
    SYMPTOM_HEART_PALPITATIONS(HealthPermissionCategory.SYMPTOM_HEART_PALPITATIONS),
    SYMPTOM_HOT_FLASHES(HealthPermissionCategory.SYMPTOM_HOT_FLASHES),
    SYMPTOM_INSOMNIA(HealthPermissionCategory.SYMPTOM_INSOMNIA),
    SYMPTOM_JOINT_PAIN(HealthPermissionCategory.SYMPTOM_JOINT_PAIN),
    SYMPTOM_JOINT_STIFFNESS(HealthPermissionCategory.SYMPTOM_JOINT_STIFFNESS),
    SYMPTOM_LOSS_OF_APPETITE(HealthPermissionCategory.SYMPTOM_LOSS_OF_APPETITE),
    SYMPTOM_LOSS_OF_CONSCIOUSNESS(HealthPermissionCategory.SYMPTOM_LOSS_OF_CONSCIOUSNESS),
    SYMPTOM_LOWER_BACK_PAIN(HealthPermissionCategory.SYMPTOM_LOWER_BACK_PAIN),
    SYMPTOM_MEMORY_LAPSE(HealthPermissionCategory.SYMPTOM_MEMORY_LAPSE),
    SYMPTOM_MOOD_CHANGE(HealthPermissionCategory.SYMPTOM_MOOD_CHANGE),
    SYMPTOM_MUSCLE_PAIN(HealthPermissionCategory.SYMPTOM_MUSCLE_PAIN),
    SYMPTOM_NAUSEA(HealthPermissionCategory.SYMPTOM_NAUSEA),
    SYMPTOM_NIGHT_SWEATS(HealthPermissionCategory.SYMPTOM_NIGHT_SWEATS),
    SYMPTOM_PELVIC_PAIN(HealthPermissionCategory.SYMPTOM_PELVIC_PAIN),
    SYMPTOM_RAPID_POUNDING_OR_FLUTTERING_HEARTBEAT(
        HealthPermissionCategory.SYMPTOM_RAPID_POUNDING_OR_FLUTTERING_HEARTBEAT
    ),
    SYMPTOM_REDUCED_CAPACITY_FOR_EXERCISE(
        HealthPermissionCategory.SYMPTOM_REDUCED_CAPACITY_FOR_EXERCISE
    ),
    SYMPTOM_RUNNY_NOSE(HealthPermissionCategory.SYMPTOM_RUNNY_NOSE),
    SYMPTOM_SHORTNESS_OF_BREATH(HealthPermissionCategory.SYMPTOM_SHORTNESS_OF_BREATH),
    SYMPTOM_SKIPPED_HEARTBEAT(HealthPermissionCategory.SYMPTOM_SKIPPED_HEARTBEAT),
    SYMPTOM_SLEEP_CHANGES(HealthPermissionCategory.SYMPTOM_SLEEP_CHANGES),
    SYMPTOM_SLEEPINESS(HealthPermissionCategory.SYMPTOM_SLEEPINESS),
    SYMPTOM_SNEEZING(HealthPermissionCategory.SYMPTOM_SNEEZING),
    SYMPTOM_SNORE(HealthPermissionCategory.SYMPTOM_SNORE),
    SYMPTOM_SORE_THROAT(HealthPermissionCategory.SYMPTOM_SORE_THROAT),
    SYMPTOM_STOMACH_ACHE(HealthPermissionCategory.SYMPTOM_STOMACH_ACHE),
    SYMPTOM_STUFFY_NOSE(HealthPermissionCategory.SYMPTOM_STUFFY_NOSE),
    SYMPTOM_UNEXPLAINED_WEIGHT_CHANGES(HealthPermissionCategory.SYMPTOM_UNEXPLAINED_WEIGHT_CHANGES),
    SYMPTOM_VAGINAL_DRYNESS(HealthPermissionCategory.SYMPTOM_VAGINAL_DRYNESS),
    SYMPTOM_VAGINAL_ITCHINESS(HealthPermissionCategory.SYMPTOM_VAGINAL_ITCHINESS),
    SYMPTOM_VOMITING(HealthPermissionCategory.SYMPTOM_VOMITING),
    SYMPTOM_WATER_RETENTION(HealthPermissionCategory.SYMPTOM_WATER_RETENTION),
    SYMPTOM_WHEEZING(HealthPermissionCategory.SYMPTOM_WHEEZING);

    override fun lowerCaseLabel(): Int =
        FitnessPermissionStrings.fromPermissionType(this).lowercaseLabel

    override fun upperCaseLabel(): Int =
        FitnessPermissionStrings.fromPermissionType(this).uppercaseLabel

    override fun icon(context: Context): Drawable? = fromFitnessPermissionType(this).icon(context)
}

fun isValidFitnessPermissionType(permissionTypeString: String): Boolean {
    try {
        FitnessPermissionType.valueOf(permissionTypeString)
    } catch (e: IllegalArgumentException) {
        return false
    }
    return true
}

fun fromHealthPermissionCategory(healthPermissionCategory: Int): HealthPermissionType {
    return when (healthPermissionCategory) {
        HealthPermissionCategory.UNKNOWN ->
            throw IllegalArgumentException("PermissionType is UNKNOWN.")
        // ACTIVITY
        HealthPermissionCategory.ACTIVE_CALORIES_BURNED ->
            FitnessPermissionType.ACTIVE_CALORIES_BURNED
        HealthPermissionCategory.ACTIVITY_INTENSITY -> FitnessPermissionType.ACTIVITY_INTENSITY
        HealthPermissionCategory.DISTANCE -> FitnessPermissionType.DISTANCE
        HealthPermissionCategory.ELEVATION_GAINED -> FitnessPermissionType.ELEVATION_GAINED
        HealthPermissionCategory.EXERCISE -> FitnessPermissionType.EXERCISE
        HealthPermissionCategory.PLANNED_EXERCISE -> FitnessPermissionType.PLANNED_EXERCISE
        HealthPermissionCategory.FLOORS_CLIMBED -> FitnessPermissionType.FLOORS_CLIMBED
        HealthPermissionCategory.STEPS -> FitnessPermissionType.STEPS
        HealthPermissionCategory.TOTAL_CALORIES_BURNED ->
            FitnessPermissionType.TOTAL_CALORIES_BURNED
        HealthPermissionCategory.VO2_MAX -> FitnessPermissionType.VO2_MAX
        HealthPermissionCategory.WHEELCHAIR_PUSHES -> FitnessPermissionType.WHEELCHAIR_PUSHES
        HealthPermissionCategory.POWER -> FitnessPermissionType.POWER
        HealthPermissionCategory.SPEED -> FitnessPermissionType.SPEED
        // BODY_MEASUREMENTS
        HealthPermissionCategory.BASAL_METABOLIC_RATE -> FitnessPermissionType.BASAL_METABOLIC_RATE
        HealthPermissionCategory.BODY_FAT -> FitnessPermissionType.BODY_FAT
        HealthPermissionCategory.BODY_WATER_MASS -> FitnessPermissionType.BODY_WATER_MASS
        HealthPermissionCategory.BONE_MASS -> FitnessPermissionType.BONE_MASS
        HealthPermissionCategory.HEIGHT -> FitnessPermissionType.HEIGHT
        HealthPermissionCategory.LEAN_BODY_MASS -> FitnessPermissionType.LEAN_BODY_MASS
        HealthPermissionCategory.WEIGHT -> FitnessPermissionType.WEIGHT
        // CYCLE_TRACKING
        HealthPermissionCategory.CERVICAL_MUCUS -> FitnessPermissionType.CERVICAL_MUCUS
        HealthPermissionCategory.MENSTRUAL_CYCLE_PHASE ->
            FitnessPermissionType.MENSTRUAL_CYCLE_PHASE
        HealthPermissionCategory.MENSTRUATION -> FitnessPermissionType.MENSTRUATION
        HealthPermissionCategory.OVULATION_TEST -> FitnessPermissionType.OVULATION_TEST
        HealthPermissionCategory.SEXUAL_ACTIVITY -> FitnessPermissionType.SEXUAL_ACTIVITY
        HealthPermissionCategory.INTERMENSTRUAL_BLEEDING ->
            FitnessPermissionType.INTERMENSTRUAL_BLEEDING
        // NUTRITION
        HealthPermissionCategory.HYDRATION -> FitnessPermissionType.HYDRATION
        HealthPermissionCategory.NUTRITION -> FitnessPermissionType.NUTRITION
        // SLEEP
        HealthPermissionCategory.SLEEP -> FitnessPermissionType.SLEEP
        // VITALS
        HealthPermissionCategory.BASAL_BODY_TEMPERATURE ->
            FitnessPermissionType.BASAL_BODY_TEMPERATURE
        HealthPermissionCategory.BLOOD_GLUCOSE -> FitnessPermissionType.BLOOD_GLUCOSE
        HealthPermissionCategory.BLOOD_PRESSURE -> FitnessPermissionType.BLOOD_PRESSURE
        HealthPermissionCategory.BODY_TEMPERATURE -> FitnessPermissionType.BODY_TEMPERATURE
        HealthPermissionCategory.HEART_RATE -> FitnessPermissionType.HEART_RATE
        HealthPermissionCategory.HEART_RATE_VARIABILITY ->
            FitnessPermissionType.HEART_RATE_VARIABILITY
        HealthPermissionCategory.OXYGEN_SATURATION -> FitnessPermissionType.OXYGEN_SATURATION
        HealthPermissionCategory.RESPIRATORY_RATE -> FitnessPermissionType.RESPIRATORY_RATE
        HealthPermissionCategory.RESTING_HEART_RATE -> FitnessPermissionType.RESTING_HEART_RATE
        HealthPermissionCategory.SKIN_TEMPERATURE -> FitnessPermissionType.SKIN_TEMPERATURE

        // WELLNESS
        HealthPermissionCategory.MINDFULNESS -> FitnessPermissionType.MINDFULNESS
        HealthPermissionCategory.NICOTINE_INTAKE -> FitnessPermissionType.NICOTINE_INTAKE
        HealthPermissionCategory.ALCOHOL_CONSUMPTION -> FitnessPermissionType.ALCOHOL_CONSUMPTION

        // SYMPTOMS
        HealthPermissionCategory.SYMPTOM_ABDOMINAL_PAIN ->
            FitnessPermissionType.SYMPTOM_ABDOMINAL_PAIN
        HealthPermissionCategory.SYMPTOM_ACNE -> FitnessPermissionType.SYMPTOM_ACNE
        HealthPermissionCategory.SYMPTOM_BACK_PAIN -> FitnessPermissionType.SYMPTOM_BACK_PAIN
        HealthPermissionCategory.SYMPTOM_BLOATING -> FitnessPermissionType.SYMPTOM_BLOATING
        HealthPermissionCategory.SYMPTOM_BRAIN_FOG -> FitnessPermissionType.SYMPTOM_BRAIN_FOG
        HealthPermissionCategory.SYMPTOM_BREAST_TENDERNESS ->
            FitnessPermissionType.SYMPTOM_BREAST_TENDERNESS
        HealthPermissionCategory.SYMPTOM_BRITTLE_NAILS ->
            FitnessPermissionType.SYMPTOM_BRITTLE_NAILS
        HealthPermissionCategory.SYMPTOM_BURNING_MOUTH ->
            FitnessPermissionType.SYMPTOM_BURNING_MOUTH
        HealthPermissionCategory.SYMPTOM_CHEST_PAIN -> FitnessPermissionType.SYMPTOM_CHEST_PAIN
        HealthPermissionCategory.SYMPTOM_CHEST_TIGHTNESS ->
            FitnessPermissionType.SYMPTOM_CHEST_TIGHTNESS
        HealthPermissionCategory.SYMPTOM_CHILLS -> FitnessPermissionType.SYMPTOM_CHILLS
        HealthPermissionCategory.SYMPTOM_CONSTIPATION -> FitnessPermissionType.SYMPTOM_CONSTIPATION
        HealthPermissionCategory.SYMPTOM_COUGH -> FitnessPermissionType.SYMPTOM_COUGH
        HealthPermissionCategory.SYMPTOM_CRAMPS -> FitnessPermissionType.SYMPTOM_CRAMPS
        HealthPermissionCategory.SYMPTOM_CRAVINGS -> FitnessPermissionType.SYMPTOM_CRAVINGS
        HealthPermissionCategory.SYMPTOM_DEHYDRATION -> FitnessPermissionType.SYMPTOM_DEHYDRATION
        HealthPermissionCategory.SYMPTOM_DIARRHEA -> FitnessPermissionType.SYMPTOM_DIARRHEA
        HealthPermissionCategory.SYMPTOM_DIFFICULTY_SWALLOWING ->
            FitnessPermissionType.SYMPTOM_DIFFICULTY_SWALLOWING
        HealthPermissionCategory.SYMPTOM_DIZZINESS -> FitnessPermissionType.SYMPTOM_DIZZINESS
        HealthPermissionCategory.SYMPTOM_DRY_SKIN -> FitnessPermissionType.SYMPTOM_DRY_SKIN
        HealthPermissionCategory.SYMPTOM_EARACHES -> FitnessPermissionType.SYMPTOM_EARACHES
        HealthPermissionCategory.SYMPTOM_FATIGUE -> FitnessPermissionType.SYMPTOM_FATIGUE
        HealthPermissionCategory.SYMPTOM_FEVER -> FitnessPermissionType.SYMPTOM_FEVER
        HealthPermissionCategory.SYMPTOM_GENERALIZED_BODY_ACHE ->
            FitnessPermissionType.SYMPTOM_GENERALIZED_BODY_ACHE
        HealthPermissionCategory.SYMPTOM_HAIR_LOSS -> FitnessPermissionType.SYMPTOM_HAIR_LOSS
        HealthPermissionCategory.SYMPTOM_HEADACHE -> FitnessPermissionType.SYMPTOM_HEADACHE
        HealthPermissionCategory.SYMPTOM_HEARTBURN -> FitnessPermissionType.SYMPTOM_HEARTBURN
        HealthPermissionCategory.SYMPTOM_HEART_PALPITATIONS ->
            FitnessPermissionType.SYMPTOM_HEART_PALPITATIONS
        HealthPermissionCategory.SYMPTOM_HOT_FLASHES -> FitnessPermissionType.SYMPTOM_HOT_FLASHES
        HealthPermissionCategory.SYMPTOM_INSOMNIA -> FitnessPermissionType.SYMPTOM_INSOMNIA
        HealthPermissionCategory.SYMPTOM_JOINT_PAIN -> FitnessPermissionType.SYMPTOM_JOINT_PAIN
        HealthPermissionCategory.SYMPTOM_JOINT_STIFFNESS ->
            FitnessPermissionType.SYMPTOM_JOINT_STIFFNESS
        HealthPermissionCategory.SYMPTOM_LOSS_OF_APPETITE ->
            FitnessPermissionType.SYMPTOM_LOSS_OF_APPETITE
        HealthPermissionCategory.SYMPTOM_LOSS_OF_CONSCIOUSNESS ->
            FitnessPermissionType.SYMPTOM_LOSS_OF_CONSCIOUSNESS
        HealthPermissionCategory.SYMPTOM_LOWER_BACK_PAIN ->
            FitnessPermissionType.SYMPTOM_LOWER_BACK_PAIN
        HealthPermissionCategory.SYMPTOM_MEMORY_LAPSE -> FitnessPermissionType.SYMPTOM_MEMORY_LAPSE
        HealthPermissionCategory.SYMPTOM_MOOD_CHANGE -> FitnessPermissionType.SYMPTOM_MOOD_CHANGE
        HealthPermissionCategory.SYMPTOM_MUSCLE_PAIN -> FitnessPermissionType.SYMPTOM_MUSCLE_PAIN
        HealthPermissionCategory.SYMPTOM_NAUSEA -> FitnessPermissionType.SYMPTOM_NAUSEA
        HealthPermissionCategory.SYMPTOM_NIGHT_SWEATS -> FitnessPermissionType.SYMPTOM_NIGHT_SWEATS
        HealthPermissionCategory.SYMPTOM_PELVIC_PAIN -> FitnessPermissionType.SYMPTOM_PELVIC_PAIN
        HealthPermissionCategory.SYMPTOM_RAPID_POUNDING_OR_FLUTTERING_HEARTBEAT ->
            FitnessPermissionType.SYMPTOM_RAPID_POUNDING_OR_FLUTTERING_HEARTBEAT
        HealthPermissionCategory.SYMPTOM_REDUCED_CAPACITY_FOR_EXERCISE ->
            FitnessPermissionType.SYMPTOM_REDUCED_CAPACITY_FOR_EXERCISE
        HealthPermissionCategory.SYMPTOM_RUNNY_NOSE -> FitnessPermissionType.SYMPTOM_RUNNY_NOSE
        HealthPermissionCategory.SYMPTOM_SHORTNESS_OF_BREATH ->
            FitnessPermissionType.SYMPTOM_SHORTNESS_OF_BREATH
        HealthPermissionCategory.SYMPTOM_SKIPPED_HEARTBEAT ->
            FitnessPermissionType.SYMPTOM_SKIPPED_HEARTBEAT
        HealthPermissionCategory.SYMPTOM_SLEEP_CHANGES ->
            FitnessPermissionType.SYMPTOM_SLEEP_CHANGES
        HealthPermissionCategory.SYMPTOM_SLEEPINESS -> FitnessPermissionType.SYMPTOM_SLEEPINESS
        HealthPermissionCategory.SYMPTOM_SNEEZING -> FitnessPermissionType.SYMPTOM_SNEEZING
        HealthPermissionCategory.SYMPTOM_SNORE -> FitnessPermissionType.SYMPTOM_SNORE
        HealthPermissionCategory.SYMPTOM_SORE_THROAT -> FitnessPermissionType.SYMPTOM_SORE_THROAT
        HealthPermissionCategory.SYMPTOM_STOMACH_ACHE -> FitnessPermissionType.SYMPTOM_STOMACH_ACHE
        HealthPermissionCategory.SYMPTOM_STUFFY_NOSE -> FitnessPermissionType.SYMPTOM_STUFFY_NOSE
        HealthPermissionCategory.SYMPTOM_UNEXPLAINED_WEIGHT_CHANGES ->
            FitnessPermissionType.SYMPTOM_UNEXPLAINED_WEIGHT_CHANGES
        HealthPermissionCategory.SYMPTOM_VAGINAL_DRYNESS ->
            FitnessPermissionType.SYMPTOM_VAGINAL_DRYNESS
        HealthPermissionCategory.SYMPTOM_VAGINAL_ITCHINESS ->
            FitnessPermissionType.SYMPTOM_VAGINAL_ITCHINESS
        HealthPermissionCategory.SYMPTOM_VOMITING -> FitnessPermissionType.SYMPTOM_VOMITING
        HealthPermissionCategory.SYMPTOM_WATER_RETENTION ->
            FitnessPermissionType.SYMPTOM_WATER_RETENTION
        HealthPermissionCategory.SYMPTOM_WHEEZING -> FitnessPermissionType.SYMPTOM_WHEEZING
        else -> throw IllegalArgumentException("PermissionType is not supported.")
    }
}
