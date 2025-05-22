/*
 * Copyright (C) 2023 The Android Open Source Project
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
 *
 *
 */

package com.android.healthconnect.controller.tests.shared

import android.health.connect.HealthDataCategory
import android.health.connect.HealthDataCategory.ACTIVITY
import android.health.connect.HealthDataCategory.BODY_MEASUREMENTS
import android.health.connect.HealthDataCategory.CYCLE_TRACKING
import android.health.connect.HealthDataCategory.NUTRITION
import android.health.connect.HealthDataCategory.SLEEP
import android.health.connect.HealthDataCategory.VITALS
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.permissions.data.HealthPermission.Companion.isAdditionalPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.Companion.isFitnessPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.Companion.isMedicalPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.FitnessPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.MedicalPermission
import com.android.healthconnect.controller.shared.FITNESS_DATA_CATEGORIES
import com.android.healthconnect.controller.shared.HealthDataCategoryExtensions.MEDICAL
import com.android.healthconnect.controller.shared.HealthDataCategoryExtensions.fromFitnessPermissionType
import com.android.healthconnect.controller.shared.HealthDataCategoryExtensions.healthPermissionTypes
import com.android.healthconnect.controller.shared.HealthDataCategoryExtensions.lowercaseTitle
import com.android.healthconnect.controller.shared.HealthDataCategoryExtensions.uppercaseTitle
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class HealthDataCategoryExtensionsTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var healthPermissionReader: HealthPermissionReader

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun fitnessDataCategories() {
        assertThat(FITNESS_DATA_CATEGORIES).hasSize(7)
        assertThat(FITNESS_DATA_CATEGORIES).containsNoDuplicates()
        assertThat(FITNESS_DATA_CATEGORIES).doesNotContain(HealthDataCategory.UNKNOWN)
    }

    @Test
    fun healthPermissionTypes_containNoDuplicates() {
        FITNESS_DATA_CATEGORIES.forEach { dataCategory ->
            assertWithMessage("HealthDataCategoty ${dataCategory}")
                .that(dataCategory.healthPermissionTypes())
                .containsNoDuplicates()
        }

        assertThat(FITNESS_DATA_CATEGORIES.flatMap { it.healthPermissionTypes() })
            .containsNoDuplicates()
    }

    @Test
    fun allFitnessPermissions_haveParentCategory() {
        val allFitnessPermissions =
            healthPermissionReader.getHealthPermissions().filterNot { perm ->
                isAdditionalPermission(perm) || isMedicalPermission(perm)
            }
        for (permissionString in allFitnessPermissions) {
            val fitnessPermission = FitnessPermission.fromPermissionString(permissionString)

            assertWithMessage(permissionString)
                .that(
                    FITNESS_DATA_CATEGORIES.count() {
                        it.healthPermissionTypes().contains(fitnessPermission.fitnessPermissionType)
                    }
                )
                .isEqualTo(1)
        }
    }

    @Test
    fun allMedicalPermissions_haveParentCategory() {
        val allMedicalsPermissions =
            healthPermissionReader.getHealthPermissions().filterNot { perm ->
                isAdditionalPermission(perm) || isFitnessPermission(perm)
            }
        for (permissionString in allMedicalsPermissions) {
            val medicalPermission = MedicalPermission.fromPermissionString(permissionString)
            assertThat(
                    MEDICAL.healthPermissionTypes()
                        .contains(medicalPermission.medicalPermissionType)
                )
                .isEqualTo(true)
        }
    }

    @Test
    fun uppercaseTitles() {
        assertThat(ACTIVITY.uppercaseTitle()).isEqualTo(R.string.activity_category_uppercase)
        assertThat(BODY_MEASUREMENTS.uppercaseTitle())
            .isEqualTo(R.string.body_measurements_category_uppercase)
        assertThat(CYCLE_TRACKING.uppercaseTitle())
            .isEqualTo(R.string.cycle_tracking_category_uppercase)
        assertThat(NUTRITION.uppercaseTitle()).isEqualTo(R.string.nutrition_category_uppercase)
        assertThat(SLEEP.uppercaseTitle()).isEqualTo(R.string.sleep_category_uppercase)
        assertThat(VITALS.uppercaseTitle()).isEqualTo(R.string.vitals_category_uppercase)
        assertThat(MEDICAL.uppercaseTitle()).isEqualTo(R.string.medical_permissions)
    }

    @Test
    fun uppercaseTitles_categoryNotSupported_throws() {
        assertThrows("Category 100 is not supported", IllegalArgumentException::class.java) {
            100.uppercaseTitle()
        }
    }

    @Test
    fun lowercaseTitles() {
        assertThat(ACTIVITY.lowercaseTitle()).isEqualTo(R.string.activity_category_lowercase)
        assertThat(BODY_MEASUREMENTS.lowercaseTitle())
            .isEqualTo(R.string.body_measurements_category_lowercase)
        assertThat(CYCLE_TRACKING.lowercaseTitle())
            .isEqualTo(R.string.cycle_tracking_category_lowercase)
        assertThat(NUTRITION.lowercaseTitle()).isEqualTo(R.string.nutrition_category_lowercase)
        assertThat(SLEEP.lowercaseTitle()).isEqualTo(R.string.sleep_category_lowercase)
        assertThat(VITALS.lowercaseTitle()).isEqualTo(R.string.vitals_category_lowercase)
        assertThat(MEDICAL.lowercaseTitle()).isEqualTo(R.string.medical_permissions_lowercase)
    }

    @Test
    fun lowercaseTitles_categoryNotSupported_throws() {
        assertThrows("Category 100 is not supported", IllegalArgumentException::class.java) {
            100.lowercaseTitle()
        }
    }

    @Test
    fun fromFitnessPermissionType() {
        assertThat(fromFitnessPermissionType(FitnessPermissionType.HEART_RATE)).isEqualTo(VITALS)
        assertThat(fromFitnessPermissionType(FitnessPermissionType.PLANNED_EXERCISE))
            .isEqualTo(ACTIVITY)
        assertThat(fromFitnessPermissionType(FitnessPermissionType.EXERCISE_ROUTE))
            .isEqualTo(ACTIVITY)
    }
}
