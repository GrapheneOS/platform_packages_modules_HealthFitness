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
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthfitness.flags.Flags
import com.google.common.truth.Truth.assertThat
import java.lang.reflect.Modifier
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
@RequiresFlagsEnabled(Flags.FLAG_SYMPTOMS, Flags.FLAG_SYMPTOMS_DB)
class SymptomTypeMapperTest {

    @get:Rule val checkFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    @Test
    fun symptomTypeMapper_mapsAllSymptomPermissionTypes() {
        val symptomPermissionTypes =
            FitnessPermissionType.values().filter { it.name.startsWith("SYMPTOM_") }

        val allSymptomTypeConstants =
            SymptomRecord::class
                .java
                .fields
                .filter {
                    Modifier.isStatic(it.modifiers) &&
                        it.type == Int::class.java &&
                        it.name.startsWith("SYMPTOM_TYPE_") &&
                        it.name != "SYMPTOM_TYPE_UNKNOWN"
                }
                .map { it.getInt(null) }

        // Assert that every permission type has a mapping
        assertThat(symptomPermissionTypes).isNotEmpty()
        symptomPermissionTypes.forEach { permissionType ->
            val symptomTypeInt = SymptomTypeMapper.getSymptomType(permissionType.category)
            assertThat(symptomTypeInt).isIn(allSymptomTypeConstants)
            assertThat(symptomTypeInt).isNotEqualTo(SymptomRecord.SYMPTOM_TYPE_UNKNOWN)
        }

        // Assert that every symptom type constant has a corresponding permission
        val mappedSymptomTypes =
            symptomPermissionTypes.map { SymptomTypeMapper.getSymptomType(it.category) }
        assertThat(mappedSymptomTypes).containsExactlyElementsIn(allSymptomTypeConstants)
    }

    @Test
    fun getSymptomType_mapsCorrectly() {
        assertThat(
                SymptomTypeMapper.getSymptomType(HealthPermissionCategory.SYMPTOM_ABDOMINAL_PAIN)
            )
            .isEqualTo(SymptomRecord.SYMPTOM_TYPE_ABDOMINAL_PAIN)
        assertThat(SymptomTypeMapper.getSymptomType(HealthPermissionCategory.SYMPTOM_ACNE))
            .isEqualTo(SymptomRecord.SYMPTOM_TYPE_ACNE)
        assertThat(SymptomTypeMapper.getSymptomType(HealthPermissionCategory.SYMPTOM_BACK_PAIN))
            .isEqualTo(SymptomRecord.SYMPTOM_TYPE_BACK_PAIN)
        assertThat(SymptomTypeMapper.getSymptomType(HealthPermissionCategory.SYMPTOM_BLOATING))
            .isEqualTo(SymptomRecord.SYMPTOM_TYPE_BLOATING)
        assertThat(SymptomTypeMapper.getSymptomType(HealthPermissionCategory.SYMPTOM_BRAIN_FOG))
            .isEqualTo(SymptomRecord.SYMPTOM_TYPE_BRAIN_FOG)
        assertThat(
                SymptomTypeMapper.getSymptomType(HealthPermissionCategory.SYMPTOM_BREAST_TENDERNESS)
            )
            .isEqualTo(SymptomRecord.SYMPTOM_TYPE_BREAST_TENDERNESS)
        assertThat(SymptomTypeMapper.getSymptomType(HealthPermissionCategory.SYMPTOM_BRITTLE_NAILS))
            .isEqualTo(SymptomRecord.SYMPTOM_TYPE_BRITTLE_NAILS)
        assertThat(SymptomTypeMapper.getSymptomType(HealthPermissionCategory.SYMPTOM_BURNING_MOUTH))
            .isEqualTo(SymptomRecord.SYMPTOM_TYPE_BURNING_MOUTH)
        assertThat(SymptomTypeMapper.getSymptomType(HealthPermissionCategory.SYMPTOM_CHEST_PAIN))
            .isEqualTo(SymptomRecord.SYMPTOM_TYPE_CHEST_PAIN)
        assertThat(
                SymptomTypeMapper.getSymptomType(HealthPermissionCategory.SYMPTOM_CHEST_TIGHTNESS)
            )
            .isEqualTo(SymptomRecord.SYMPTOM_TYPE_CHEST_TIGHTNESS)
        assertThat(SymptomTypeMapper.getSymptomType(HealthPermissionCategory.SYMPTOM_CHILLS))
            .isEqualTo(SymptomRecord.SYMPTOM_TYPE_CHILLS)
        assertThat(SymptomTypeMapper.getSymptomType(HealthPermissionCategory.SYMPTOM_CONSTIPATION))
            .isEqualTo(SymptomRecord.SYMPTOM_TYPE_CONSTIPATION)
        assertThat(SymptomTypeMapper.getSymptomType(HealthPermissionCategory.SYMPTOM_COUGH))
            .isEqualTo(SymptomRecord.SYMPTOM_TYPE_COUGH)
        assertThat(SymptomTypeMapper.getSymptomType(HealthPermissionCategory.SYMPTOM_CRAMPS))
            .isEqualTo(SymptomRecord.SYMPTOM_TYPE_CRAMPS)
        assertThat(SymptomTypeMapper.getSymptomType(HealthPermissionCategory.SYMPTOM_CRAVINGS))
            .isEqualTo(SymptomRecord.SYMPTOM_TYPE_CRAVINGS)
        assertThat(SymptomTypeMapper.getSymptomType(HealthPermissionCategory.SYMPTOM_DEHYDRATION))
            .isEqualTo(SymptomRecord.SYMPTOM_TYPE_DEHYDRATION)
        assertThat(SymptomTypeMapper.getSymptomType(HealthPermissionCategory.SYMPTOM_DIARRHEA))
            .isEqualTo(SymptomRecord.SYMPTOM_TYPE_DIARRHEA)
        assertThat(
                SymptomTypeMapper.getSymptomType(
                    HealthPermissionCategory.SYMPTOM_DIFFICULTY_SWALLOWING
                )
            )
            .isEqualTo(SymptomRecord.SYMPTOM_TYPE_DIFFICULTY_SWALLOWING)
        assertThat(SymptomTypeMapper.getSymptomType(HealthPermissionCategory.SYMPTOM_DIZZINESS))
            .isEqualTo(SymptomRecord.SYMPTOM_TYPE_DIZZINESS)
        assertThat(SymptomTypeMapper.getSymptomType(HealthPermissionCategory.SYMPTOM_DRY_SKIN))
            .isEqualTo(SymptomRecord.SYMPTOM_TYPE_DRY_SKIN)
        assertThat(SymptomTypeMapper.getSymptomType(HealthPermissionCategory.SYMPTOM_EARACHES))
            .isEqualTo(SymptomRecord.SYMPTOM_TYPE_EARACHES)
        assertThat(SymptomTypeMapper.getSymptomType(HealthPermissionCategory.SYMPTOM_FATIGUE))
            .isEqualTo(SymptomRecord.SYMPTOM_TYPE_FATIGUE)
        assertThat(SymptomTypeMapper.getSymptomType(HealthPermissionCategory.SYMPTOM_FEVER))
            .isEqualTo(SymptomRecord.SYMPTOM_TYPE_FEVER)
        assertThat(
                SymptomTypeMapper.getSymptomType(
                    HealthPermissionCategory.SYMPTOM_GENERALIZED_BODY_ACHE
                )
            )
            .isEqualTo(SymptomRecord.SYMPTOM_TYPE_GENERALIZED_BODY_ACHE)
        assertThat(SymptomTypeMapper.getSymptomType(HealthPermissionCategory.SYMPTOM_HAIR_LOSS))
            .isEqualTo(SymptomRecord.SYMPTOM_TYPE_HAIR_LOSS)
        assertThat(SymptomTypeMapper.getSymptomType(HealthPermissionCategory.SYMPTOM_HEADACHE))
            .isEqualTo(SymptomRecord.SYMPTOM_TYPE_HEADACHE)
        assertThat(SymptomTypeMapper.getSymptomType(HealthPermissionCategory.SYMPTOM_HEARTBURN))
            .isEqualTo(SymptomRecord.SYMPTOM_TYPE_HEARTBURN)
        assertThat(
                SymptomTypeMapper.getSymptomType(
                    HealthPermissionCategory.SYMPTOM_HEART_PALPITATIONS
                )
            )
            .isEqualTo(SymptomRecord.SYMPTOM_TYPE_HEART_PALPITATIONS)
        assertThat(SymptomTypeMapper.getSymptomType(HealthPermissionCategory.SYMPTOM_HOT_FLASHES))
            .isEqualTo(SymptomRecord.SYMPTOM_TYPE_HOT_FLASHES)
        assertThat(SymptomTypeMapper.getSymptomType(HealthPermissionCategory.SYMPTOM_INSOMNIA))
            .isEqualTo(SymptomRecord.SYMPTOM_TYPE_INSOMNIA)
        assertThat(SymptomTypeMapper.getSymptomType(HealthPermissionCategory.SYMPTOM_JOINT_PAIN))
            .isEqualTo(SymptomRecord.SYMPTOM_TYPE_JOINT_PAIN)
        assertThat(
                SymptomTypeMapper.getSymptomType(HealthPermissionCategory.SYMPTOM_JOINT_STIFFNESS)
            )
            .isEqualTo(SymptomRecord.SYMPTOM_TYPE_JOINT_STIFFNESS)
        assertThat(
                SymptomTypeMapper.getSymptomType(HealthPermissionCategory.SYMPTOM_LOSS_OF_APPETITE)
            )
            .isEqualTo(SymptomRecord.SYMPTOM_TYPE_LOSS_OF_APPETITE)
        assertThat(
                SymptomTypeMapper.getSymptomType(
                    HealthPermissionCategory.SYMPTOM_LOSS_OF_CONSCIOUSNESS
                )
            )
            .isEqualTo(SymptomRecord.SYMPTOM_TYPE_LOSS_OF_CONSCIOUSNESS)
        assertThat(
                SymptomTypeMapper.getSymptomType(HealthPermissionCategory.SYMPTOM_LOWER_BACK_PAIN)
            )
            .isEqualTo(SymptomRecord.SYMPTOM_TYPE_LOWER_BACK_PAIN)
        assertThat(SymptomTypeMapper.getSymptomType(HealthPermissionCategory.SYMPTOM_MEMORY_LAPSE))
            .isEqualTo(SymptomRecord.SYMPTOM_TYPE_MEMORY_LAPSE)
        assertThat(SymptomTypeMapper.getSymptomType(HealthPermissionCategory.SYMPTOM_MOOD_CHANGE))
            .isEqualTo(SymptomRecord.SYMPTOM_TYPE_MOOD_CHANGE)
        assertThat(SymptomTypeMapper.getSymptomType(HealthPermissionCategory.SYMPTOM_MUSCLE_PAIN))
            .isEqualTo(SymptomRecord.SYMPTOM_TYPE_MUSCLE_PAIN)
        assertThat(SymptomTypeMapper.getSymptomType(HealthPermissionCategory.SYMPTOM_NAUSEA))
            .isEqualTo(SymptomRecord.SYMPTOM_TYPE_NAUSEA)
        assertThat(SymptomTypeMapper.getSymptomType(HealthPermissionCategory.SYMPTOM_NIGHT_SWEATS))
            .isEqualTo(SymptomRecord.SYMPTOM_TYPE_NIGHT_SWEATS)
        assertThat(SymptomTypeMapper.getSymptomType(HealthPermissionCategory.SYMPTOM_PELVIC_PAIN))
            .isEqualTo(SymptomRecord.SYMPTOM_TYPE_PELVIC_PAIN)
        assertThat(
                SymptomTypeMapper.getSymptomType(
                    HealthPermissionCategory.SYMPTOM_RAPID_POUNDING_OR_FLUTTERING_HEARTBEAT
                )
            )
            .isEqualTo(SymptomRecord.SYMPTOM_TYPE_RAPID_POUNDING_OR_FLUTTERING_HEARTBEAT)
        assertThat(
                SymptomTypeMapper.getSymptomType(
                    HealthPermissionCategory.SYMPTOM_REDUCED_CAPACITY_FOR_EXERCISE
                )
            )
            .isEqualTo(SymptomRecord.SYMPTOM_TYPE_REDUCED_CAPACITY_FOR_EXERCISE)
        assertThat(SymptomTypeMapper.getSymptomType(HealthPermissionCategory.SYMPTOM_RUNNY_NOSE))
            .isEqualTo(SymptomRecord.SYMPTOM_TYPE_RUNNY_NOSE)
        assertThat(
                SymptomTypeMapper.getSymptomType(
                    HealthPermissionCategory.SYMPTOM_SHORTNESS_OF_BREATH
                )
            )
            .isEqualTo(SymptomRecord.SYMPTOM_TYPE_SHORTNESS_OF_BREATH)
        assertThat(
                SymptomTypeMapper.getSymptomType(HealthPermissionCategory.SYMPTOM_SKIPPED_HEARTBEAT)
            )
            .isEqualTo(SymptomRecord.SYMPTOM_TYPE_SKIPPED_HEARTBEAT)
        assertThat(SymptomTypeMapper.getSymptomType(HealthPermissionCategory.SYMPTOM_SLEEPINESS))
            .isEqualTo(SymptomRecord.SYMPTOM_TYPE_SLEEPINESS)
        assertThat(SymptomTypeMapper.getSymptomType(HealthPermissionCategory.SYMPTOM_SLEEP_CHANGES))
            .isEqualTo(SymptomRecord.SYMPTOM_TYPE_SLEEP_CHANGES)
        assertThat(SymptomTypeMapper.getSymptomType(HealthPermissionCategory.SYMPTOM_SNEEZING))
            .isEqualTo(SymptomRecord.SYMPTOM_TYPE_SNEEZING)
        assertThat(SymptomTypeMapper.getSymptomType(HealthPermissionCategory.SYMPTOM_SNORE))
            .isEqualTo(SymptomRecord.SYMPTOM_TYPE_SNORE)
        assertThat(SymptomTypeMapper.getSymptomType(HealthPermissionCategory.SYMPTOM_SORE_THROAT))
            .isEqualTo(SymptomRecord.SYMPTOM_TYPE_SORE_THROAT)
        assertThat(SymptomTypeMapper.getSymptomType(HealthPermissionCategory.SYMPTOM_STOMACH_ACHE))
            .isEqualTo(SymptomRecord.SYMPTOM_TYPE_STOMACH_ACHE)
        assertThat(SymptomTypeMapper.getSymptomType(HealthPermissionCategory.SYMPTOM_STUFFY_NOSE))
            .isEqualTo(SymptomRecord.SYMPTOM_TYPE_STUFFY_NOSE)
        assertThat(
                SymptomTypeMapper.getSymptomType(
                    HealthPermissionCategory.SYMPTOM_UNEXPLAINED_WEIGHT_CHANGES
                )
            )
            .isEqualTo(SymptomRecord.SYMPTOM_TYPE_UNEXPLAINED_WEIGHT_CHANGES)
        assertThat(
                SymptomTypeMapper.getSymptomType(HealthPermissionCategory.SYMPTOM_VAGINAL_DRYNESS)
            )
            .isEqualTo(SymptomRecord.SYMPTOM_TYPE_VAGINAL_DRYNESS)
        assertThat(
                SymptomTypeMapper.getSymptomType(HealthPermissionCategory.SYMPTOM_VAGINAL_ITCHINESS)
            )
            .isEqualTo(SymptomRecord.SYMPTOM_TYPE_VAGINAL_ITCHINESS)
        assertThat(SymptomTypeMapper.getSymptomType(HealthPermissionCategory.SYMPTOM_VOMITING))
            .isEqualTo(SymptomRecord.SYMPTOM_TYPE_VOMITING)
        assertThat(
                SymptomTypeMapper.getSymptomType(HealthPermissionCategory.SYMPTOM_WATER_RETENTION)
            )
            .isEqualTo(SymptomRecord.SYMPTOM_TYPE_WATER_RETENTION)
        assertThat(SymptomTypeMapper.getSymptomType(HealthPermissionCategory.SYMPTOM_WHEEZING))
            .isEqualTo(SymptomRecord.SYMPTOM_TYPE_WHEEZING)
    }
}
