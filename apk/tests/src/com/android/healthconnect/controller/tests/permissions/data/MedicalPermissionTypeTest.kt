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
package com.android.healthconnect.controller.tests.permissions.data

import android.content.Context
import android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.permissions.data.MedicalPermissionType
import com.android.healthconnect.controller.permissions.data.fromMedicalResourceType
import com.android.healthconnect.controller.permissions.data.toMedicalResourceType
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
class MedicalPermissionTypeTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var healthPermissionReader: HealthPermissionReader
    private lateinit var context: Context

    @Before
    fun setup() {
        hiltRule.inject()
        context = InstrumentationRegistry.getInstrumentation().context
    }

    @Test
    fun fromMedicalResourceType_immunization() {
        assertThat(fromMedicalResourceType(MEDICAL_RESOURCE_TYPE_VACCINES))
            .isEqualTo(MedicalPermissionType.VACCINES)
    }

    @Test
    fun fromMedicalResourceType_notSupported_throws() {
        val thrown =
            assertThrows(IllegalArgumentException::class.java) { fromMedicalResourceType(123456) }
        assertThat(thrown).hasMessageThat().isEqualTo("MedicalResourceType is not supported.")
    }

    @Test
    fun toMedicalResourceType_immunization() {
        assertThat(toMedicalResourceType(MedicalPermissionType.VACCINES))
            .isEqualTo(MEDICAL_RESOURCE_TYPE_VACCINES)
    }

    @Test
    fun toMedicalResourceType_allMedicalData_throws() {
        val thrown =
            assertThrows(IllegalArgumentException::class.java) {
                toMedicalResourceType(MedicalPermissionType.ALL_MEDICAL_DATA)
            }
        assertThat(thrown).hasMessageThat().isEqualTo("MedicalPermissionType does not map to a MedicalResourceType.")
    }

    @Test
    fun toMedicalResourceType_supportsAllMedicalPermissionType() {
        for (permissionType in
            MedicalPermissionType.entries.filterNot {
                it == MedicalPermissionType.ALL_MEDICAL_DATA
            }) {
            assertThat(toMedicalResourceType(permissionType)).isNotNull()
        }
    }
}
