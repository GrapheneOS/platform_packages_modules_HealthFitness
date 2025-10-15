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
package com.android.healthconnect.controller.data.entries.api

import android.health.connect.HealthPermissionCategory
import android.health.connect.datatypes.SymptomRecord

object SymptomTypeMapper {
    fun getSymptomType(category: Int): Int {
        return when (category) {
            HealthPermissionCategory.SYMPTOM_ABDOMINAL_PAIN ->
                SymptomRecord.SYMPTOM_TYPE_ABDOMINAL_PAIN
            HealthPermissionCategory.SYMPTOM_ACNE -> SymptomRecord.SYMPTOM_TYPE_ACNE
            HealthPermissionCategory.SYMPTOM_BACK_PAIN -> SymptomRecord.SYMPTOM_TYPE_BACK_PAIN
            HealthPermissionCategory.SYMPTOM_BLOATING -> SymptomRecord.SYMPTOM_TYPE_BLOATING
            HealthPermissionCategory.SYMPTOM_BRAIN_FOG -> SymptomRecord.SYMPTOM_TYPE_BRAIN_FOG
            HealthPermissionCategory.SYMPTOM_BREAST_TENDERNESS ->
                SymptomRecord.SYMPTOM_TYPE_BREAST_TENDERNESS
            HealthPermissionCategory.SYMPTOM_BRITTLE_NAILS ->
                SymptomRecord.SYMPTOM_TYPE_BRITTLE_NAILS
            HealthPermissionCategory.SYMPTOM_BURNING_MOUTH ->
                SymptomRecord.SYMPTOM_TYPE_BURNING_MOUTH
            HealthPermissionCategory.SYMPTOM_CHEST_PAIN -> SymptomRecord.SYMPTOM_TYPE_CHEST_PAIN
            HealthPermissionCategory.SYMPTOM_CHEST_TIGHTNESS ->
                SymptomRecord.SYMPTOM_TYPE_CHEST_TIGHTNESS
            HealthPermissionCategory.SYMPTOM_CHILLS -> SymptomRecord.SYMPTOM_TYPE_CHILLS
            HealthPermissionCategory.SYMPTOM_CONSTIPATION -> SymptomRecord.SYMPTOM_TYPE_CONSTIPATION
            HealthPermissionCategory.SYMPTOM_COUGH -> SymptomRecord.SYMPTOM_TYPE_COUGH
            HealthPermissionCategory.SYMPTOM_CRAMPS -> SymptomRecord.SYMPTOM_TYPE_CRAMPS
            HealthPermissionCategory.SYMPTOM_CRAVINGS -> SymptomRecord.SYMPTOM_TYPE_CRAVINGS
            HealthPermissionCategory.SYMPTOM_DEHYDRATION -> SymptomRecord.SYMPTOM_TYPE_DEHYDRATION
            HealthPermissionCategory.SYMPTOM_DIARRHEA -> SymptomRecord.SYMPTOM_TYPE_DIARRHEA
            HealthPermissionCategory.SYMPTOM_DIFFICULTY_SWALLOWING ->
                SymptomRecord.SYMPTOM_TYPE_DIFFICULTY_SWALLOWING
            HealthPermissionCategory.SYMPTOM_DIZZINESS -> SymptomRecord.SYMPTOM_TYPE_DIZZINESS
            HealthPermissionCategory.SYMPTOM_DRY_SKIN -> SymptomRecord.SYMPTOM_TYPE_DRY_SKIN
            HealthPermissionCategory.SYMPTOM_EARACHES -> SymptomRecord.SYMPTOM_TYPE_EARACHES
            HealthPermissionCategory.SYMPTOM_FATIGUE -> SymptomRecord.SYMPTOM_TYPE_FATIGUE
            HealthPermissionCategory.SYMPTOM_FEVER -> SymptomRecord.SYMPTOM_TYPE_FEVER
            HealthPermissionCategory.SYMPTOM_GENERALIZED_BODY_ACHE ->
                SymptomRecord.SYMPTOM_TYPE_GENERALIZED_BODY_ACHE
            HealthPermissionCategory.SYMPTOM_HAIR_LOSS -> SymptomRecord.SYMPTOM_TYPE_HAIR_LOSS
            HealthPermissionCategory.SYMPTOM_HEADACHE -> SymptomRecord.SYMPTOM_TYPE_HEADACHE
            HealthPermissionCategory.SYMPTOM_HEARTBURN -> SymptomRecord.SYMPTOM_TYPE_HEARTBURN
            HealthPermissionCategory.SYMPTOM_HEART_PALPITATIONS ->
                SymptomRecord.SYMPTOM_TYPE_HEART_PALPITATIONS
            HealthPermissionCategory.SYMPTOM_HOT_FLASHES -> SymptomRecord.SYMPTOM_TYPE_HOT_FLASHES
            HealthPermissionCategory.SYMPTOM_INSOMNIA -> SymptomRecord.SYMPTOM_TYPE_INSOMNIA
            HealthPermissionCategory.SYMPTOM_JOINT_PAIN -> SymptomRecord.SYMPTOM_TYPE_JOINT_PAIN
            HealthPermissionCategory.SYMPTOM_JOINT_STIFFNESS ->
                SymptomRecord.SYMPTOM_TYPE_JOINT_STIFFNESS
            HealthPermissionCategory.SYMPTOM_LOSS_OF_APPETITE ->
                SymptomRecord.SYMPTOM_TYPE_LOSS_OF_APPETITE
            HealthPermissionCategory.SYMPTOM_LOSS_OF_CONSCIOUSNESS ->
                SymptomRecord.SYMPTOM_TYPE_LOSS_OF_CONSCIOUSNESS
            HealthPermissionCategory.SYMPTOM_LOWER_BACK_PAIN ->
                SymptomRecord.SYMPTOM_TYPE_LOWER_BACK_PAIN
            HealthPermissionCategory.SYMPTOM_MEMORY_LAPSE -> SymptomRecord.SYMPTOM_TYPE_MEMORY_LAPSE
            HealthPermissionCategory.SYMPTOM_MOOD_CHANGE -> SymptomRecord.SYMPTOM_TYPE_MOOD_CHANGE
            HealthPermissionCategory.SYMPTOM_MUSCLE_PAIN -> SymptomRecord.SYMPTOM_TYPE_MUSCLE_PAIN
            HealthPermissionCategory.SYMPTOM_NAUSEA -> SymptomRecord.SYMPTOM_TYPE_NAUSEA
            HealthPermissionCategory.SYMPTOM_NIGHT_SWEATS -> SymptomRecord.SYMPTOM_TYPE_NIGHT_SWEATS
            HealthPermissionCategory.SYMPTOM_PELVIC_PAIN -> SymptomRecord.SYMPTOM_TYPE_PELVIC_PAIN
            HealthPermissionCategory.SYMPTOM_RAPID_POUNDING_OR_FLUTTERING_HEARTBEAT ->
                SymptomRecord.SYMPTOM_TYPE_RAPID_POUNDING_OR_FLUTTERING_HEARTBEAT
            HealthPermissionCategory.SYMPTOM_REDUCED_CAPACITY_FOR_EXERCISE ->
                SymptomRecord.SYMPTOM_TYPE_REDUCED_CAPACITY_FOR_EXERCISE
            HealthPermissionCategory.SYMPTOM_RUNNY_NOSE -> SymptomRecord.SYMPTOM_TYPE_RUNNY_NOSE
            HealthPermissionCategory.SYMPTOM_SHORTNESS_OF_BREATH ->
                SymptomRecord.SYMPTOM_TYPE_SHORTNESS_OF_BREATH
            HealthPermissionCategory.SYMPTOM_SKIPPED_HEARTBEAT ->
                SymptomRecord.SYMPTOM_TYPE_SKIPPED_HEARTBEAT
            HealthPermissionCategory.SYMPTOM_SLEEPINESS -> SymptomRecord.SYMPTOM_TYPE_SLEEPINESS
            HealthPermissionCategory.SYMPTOM_SLEEP_CHANGES ->
                SymptomRecord.SYMPTOM_TYPE_SLEEP_CHANGES
            HealthPermissionCategory.SYMPTOM_SNEEZING -> SymptomRecord.SYMPTOM_TYPE_SNEEZING
            HealthPermissionCategory.SYMPTOM_SNORE -> SymptomRecord.SYMPTOM_TYPE_SNORE
            HealthPermissionCategory.SYMPTOM_SORE_THROAT -> SymptomRecord.SYMPTOM_TYPE_SORE_THROAT
            HealthPermissionCategory.SYMPTOM_STOMACH_ACHE -> SymptomRecord.SYMPTOM_TYPE_STOMACH_ACHE
            HealthPermissionCategory.SYMPTOM_STUFFY_NOSE -> SymptomRecord.SYMPTOM_TYPE_STUFFY_NOSE
            HealthPermissionCategory.SYMPTOM_UNEXPLAINED_WEIGHT_CHANGES ->
                SymptomRecord.SYMPTOM_TYPE_UNEXPLAINED_WEIGHT_CHANGES
            HealthPermissionCategory.SYMPTOM_VAGINAL_DRYNESS ->
                SymptomRecord.SYMPTOM_TYPE_VAGINAL_DRYNESS
            HealthPermissionCategory.SYMPTOM_VAGINAL_ITCHINESS ->
                SymptomRecord.SYMPTOM_TYPE_VAGINAL_ITCHINESS
            HealthPermissionCategory.SYMPTOM_VOMITING -> SymptomRecord.SYMPTOM_TYPE_VOMITING
            HealthPermissionCategory.SYMPTOM_WATER_RETENTION ->
                SymptomRecord.SYMPTOM_TYPE_WATER_RETENTION
            HealthPermissionCategory.SYMPTOM_WHEEZING -> SymptomRecord.SYMPTOM_TYPE_WHEEZING
            else -> throw IllegalArgumentException("Invalid symptom category: $category")
        }
    }
}
