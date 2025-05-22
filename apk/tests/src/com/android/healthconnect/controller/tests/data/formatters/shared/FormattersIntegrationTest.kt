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

package com.android.healthconnect.controller.tests.data.formatters.shared

import android.health.connect.internal.datatypes.utils.HealthConnectMappings
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.permissions.data.fromHealthPermissionCategory
import com.android.healthconnect.controller.shared.HealthPermissionToDatatypeMapper
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FormattersIntegrationTest {

    @Test
    fun allRecordsHaveFormatters() {
        val healthConnectMappings = HealthConnectMappings()
        val frameworkRecordClasses = healthConnectMappings.recordIdToExternalRecordClassMap.values
        val controllerRecordClasses =
            HealthPermissionToDatatypeMapper.getAllDataTypes().values.flatten()

        assertThat(controllerRecordClasses).containsExactlyElementsIn(frameworkRecordClasses)

        for (recordTypeId in healthConnectMappings.allRecordTypeIdentifiers) {
            val permissionCategory =
                healthConnectMappings.getHealthPermissionCategoryForRecordType(recordTypeId)
            val fitnessPermissionType =
                fromHealthPermissionCategory(permissionCategory) as FitnessPermissionType
            val expectedRecordClass =
                healthConnectMappings.recordIdToExternalRecordClassMap[recordTypeId]!!

            assertThat(HealthPermissionToDatatypeMapper.getAllDataTypes())
                .containsKey(fitnessPermissionType)
            assertThat(HealthPermissionToDatatypeMapper.getAllDataTypes()[fitnessPermissionType]!!)
                .contains(expectedRecordClass)
            assertThat(HealthPermissionToDatatypeMapper.getDataTypes(fitnessPermissionType))
                .contains(expectedRecordClass)
        }
    }
}
