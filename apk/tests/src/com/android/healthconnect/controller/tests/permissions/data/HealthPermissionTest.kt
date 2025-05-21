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
package com.android.healthconnect.controller.tests.permissions.data

import android.health.connect.HealthPermissions
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType.ACTIVE_CALORIES_BURNED
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType.BLOOD_GLUCOSE
import com.android.healthconnect.controller.permissions.data.HealthPermission.AdditionalPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.Companion.fromPermissionString
import com.android.healthconnect.controller.permissions.data.HealthPermission.Companion.isAdditionalPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.Companion.isFitnessPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.Companion.isFitnessReadPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.Companion.isMedicalPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.Companion.isMedicalReadPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.FitnessPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.MedicalPermission
import com.android.healthconnect.controller.permissions.data.MedicalPermissionType
import com.android.healthconnect.controller.permissions.data.PermissionsAccessType
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.google.common.truth.Truth.assertThat
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
class HealthPermissionTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var healthPermissionReader: HealthPermissionReader

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun fromPermission_returnsCorrectReadFitnessPermission() {
        assertThat(fromPermissionString("android.permission.health.READ_ACTIVE_CALORIES_BURNED"))
            .isEqualTo(FitnessPermission(ACTIVE_CALORIES_BURNED, PermissionsAccessType.READ))
    }

    @Test
    fun fromPermission_returnsCorrectWriteFitnessPermission() {
        assertThat(fromPermissionString("android.permission.health.WRITE_BLOOD_GLUCOSE"))
            .isEqualTo(FitnessPermission(BLOOD_GLUCOSE, PermissionsAccessType.WRITE))
    }

    @Test
    fun fromPermissionString_canParseAllFitnessPermissions() {
        val allPermissions =
            healthPermissionReader.getHealthPermissions().filterNot { perm ->
                isAdditionalPermission(perm) || isMedicalPermission(perm)
            }
        for (permissionString in allPermissions) {
            assertThat(fromPermissionString(permissionString).toString())
                .isEqualTo(permissionString)
        }
    }

    @Test
    fun fromPermissionString_canParseAllMedicalPermissions() {
        val medicalPermissions =
            healthPermissionReader.getHealthPermissions().filter { perm ->
                isMedicalPermission(perm)
            }
        for (permissionString in medicalPermissions) {
            assertThat(fromPermissionString(permissionString).toString())
                .isEqualTo(permissionString)
        }
    }

    @Test
    fun fromPermissionString_returnsCorrectAdditionalPermission() {
        assertThat(fromPermissionString("android.permission.health.READ_HEALTH_DATA_HISTORY"))
            .isEqualTo(AdditionalPermission(HealthPermissions.READ_HEALTH_DATA_HISTORY))

        assertThat(fromPermissionString("android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND"))
            .isEqualTo(AdditionalPermission(HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND))
    }

    @Test
    fun fromPermissionString_returnsCorrectMedicalPermission() {
        assertThat(fromPermissionString("android.permission.health.WRITE_MEDICAL_DATA"))
            .isEqualTo(MedicalPermission(MedicalPermissionType.ALL_MEDICAL_DATA))

        assertThat(
                fromPermissionString(
                    "android.permission.health.READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES"
                )
            )
            .isEqualTo(MedicalPermission(MedicalPermissionType.ALLERGIES_INTOLERANCES))

        assertThat(fromPermissionString("android.permission.health.READ_MEDICAL_DATA_CONDITIONS"))
            .isEqualTo(MedicalPermission(MedicalPermissionType.CONDITIONS))

        assertThat(
                fromPermissionString("android.permission.health.READ_MEDICAL_DATA_VACCINES")
            )
            .isEqualTo(MedicalPermission(MedicalPermissionType.VACCINES))

        assertThat(
                fromPermissionString(
                    "android.permission.health.READ_MEDICAL_DATA_LABORATORY_RESULTS"
                )
            )
            .isEqualTo(MedicalPermission(MedicalPermissionType.LABORATORY_RESULTS))

        assertThat(fromPermissionString("android.permission.health.READ_MEDICAL_DATA_MEDICATIONS"))
            .isEqualTo(MedicalPermission(MedicalPermissionType.MEDICATIONS))

        assertThat(
                fromPermissionString("android.permission.health.READ_MEDICAL_DATA_PERSONAL_DETAILS")
            )
            .isEqualTo(MedicalPermission(MedicalPermissionType.PERSONAL_DETAILS))

        assertThat(
                fromPermissionString(
                    "android.permission.health.READ_MEDICAL_DATA_PRACTITIONER_DETAILS"
                )
            )
            .isEqualTo(MedicalPermission(MedicalPermissionType.PRACTITIONER_DETAILS))

        assertThat(fromPermissionString("android.permission.health.READ_MEDICAL_DATA_PREGNANCY"))
            .isEqualTo(MedicalPermission(MedicalPermissionType.PREGNANCY))

        assertThat(fromPermissionString("android.permission.health.READ_MEDICAL_DATA_PROCEDURES"))
            .isEqualTo(MedicalPermission(MedicalPermissionType.PROCEDURES))

        assertThat(
                fromPermissionString("android.permission.health.READ_MEDICAL_DATA_SOCIAL_HISTORY")
            )
            .isEqualTo(MedicalPermission(MedicalPermissionType.SOCIAL_HISTORY))

        assertThat(fromPermissionString("android.permission.health.READ_MEDICAL_DATA_VISITS"))
            .isEqualTo(MedicalPermission(MedicalPermissionType.VISITS))

        assertThat(fromPermissionString("android.permission.health.READ_MEDICAL_DATA_VITAL_SIGNS"))
            .isEqualTo(MedicalPermission(MedicalPermissionType.VITAL_SIGNS))
    }

    @Test
    fun fromPermissionString_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException::class.java) {
            fromPermissionString("Unsupported_permission")
        }
    }

    @Test
    fun isAdditionalPermission_whenAdditionalPermission_returnsTrue() {
        val perm = AdditionalPermission(HealthPermissions.READ_EXERCISE_ROUTES)
        assertThat(isAdditionalPermission(perm.toString())).isTrue()
    }

    @Test
    fun isAdditionalPermission_whenNotAdditionalPermissions_returnsFalse() {
        val perm = MedicalPermission(MedicalPermissionType.ALL_MEDICAL_DATA)
        assertThat(isAdditionalPermission(perm.toString())).isFalse()
    }

    @Test
    fun isMedicalPermission_whenMedicalPermission_returnsTrue() {
        val perm = MedicalPermission(MedicalPermissionType.ALL_MEDICAL_DATA)
        assertThat(isMedicalPermission(perm.toString())).isTrue()
    }

    @Test
    fun isMedicalPermission_whenNotMedicalPermission_returnsFalse() {
        val perm = AdditionalPermission(HealthPermissions.READ_EXERCISE_ROUTES)
        assertThat(isMedicalPermission(perm.toString())).isFalse()
    }

    @Test
    fun isFitnessPermission_whenFitnessPermission_returnsTrue() {
        val perm =
            FitnessPermission.fromPermissionString(HealthPermissions.READ_ACTIVE_CALORIES_BURNED)
        assertThat(isFitnessPermission(perm.toString())).isTrue()
    }

    @Test
    fun isFitnessPermission_whenNotFitnessPermission_returnsFalse() {
        val perm = AdditionalPermission(HealthPermissions.READ_EXERCISE_ROUTES)
        assertThat(isFitnessPermission(perm.toString())).isFalse()
    }

    @Test
    fun isFitnessReadPermission_whenFitnessReadPermission_returnsTrue() {
        val perm = FitnessPermission.fromPermissionString(HealthPermissions.READ_EXERCISE)
        assertThat(isFitnessReadPermission(perm)).isTrue()
        assertThat(isFitnessReadPermission(perm.toString())).isTrue()
    }

    @Test
    fun isFitnessReadPermission_whenNotFitnessReadPermission_returnsFalse() {
        val perm = FitnessPermission.fromPermissionString(HealthPermissions.WRITE_SLEEP)
        assertThat(isFitnessReadPermission(perm)).isFalse()
        assertThat(isFitnessReadPermission(perm.toString())).isFalse()
    }

    @Test
    fun isMedicalReadPermission_whenMedicalReadPermission_returnsTrue() {
        val perm = MedicalPermission(MedicalPermissionType.CONDITIONS)
        assertThat(isMedicalReadPermission(perm)).isTrue()
        assertThat(isMedicalReadPermission(perm.toString())).isTrue()
    }

    @Test
    fun isMedicalReadPermission_whenNotMedicalReadPermission_returnsFalse() {
        val perm = MedicalPermission(MedicalPermissionType.ALL_MEDICAL_DATA)
        assertThat(isMedicalReadPermission(perm)).isFalse()
        assertThat(isMedicalReadPermission(perm.toString())).isFalse()
    }
}
