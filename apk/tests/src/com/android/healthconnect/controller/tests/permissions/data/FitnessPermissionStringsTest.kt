/**
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.tests.permissions.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.permissions.data.FitnessPermissionStrings
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.permissions.data.HealthPermission.Companion.isAdditionalPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.Companion.isMedicalPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.FitnessPermission
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class FitnessPermissionStringsTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var healthPermissionReader: HealthPermissionReader

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun allHealthPermissionTypesHaveStrings() {
        for (type in FitnessPermissionType.entries) {
            assertThat(FitnessPermissionStrings.fromPermissionType(type)).isNotNull()
        }
    }

    @Test
    fun allFitnessPermissionsHaveStrings() {
        val allPermissions =
            healthPermissionReader.getHealthPermissions().filterNot { perm ->
                isAdditionalPermission(perm) || isMedicalPermission(perm)
            }
        for (permission in allPermissions) {
            val type = FitnessPermission.fromPermissionString(permission).fitnessPermissionType
            assertThat(FitnessPermissionStrings.fromPermissionType(type)).isNotNull()
        }
    }
}
