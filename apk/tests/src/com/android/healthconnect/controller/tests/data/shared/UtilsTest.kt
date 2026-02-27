/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.healthconnect.controller.tests.data.shared

import android.health.connect.HealthDataCategory
import android.health.connect.HealthPermissionCategory
import android.health.connect.RecordTypeInfoResponse
import android.health.connect.datatypes.Record
import android.health.connect.datatypes.StepsRecord
import android.health.connect.datatypes.SymptomRecord
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.data.shared.getPermissionTypesPerCategory
import com.android.healthconnect.controller.data.shared.getSymptomPermissionTypes
import com.android.healthconnect.controller.data.shared.hasData
import com.android.healthconnect.controller.data.shared.hasDataByApp
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.tests.utils.DEVICE_DATA_PROVIDER_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME_2
import com.android.healthconnect.controller.tests.utils.getDataOrigin
import com.android.healthfitness.flags.Flags
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UtilsTest {

    @get:Rule val setFlagsRule = SetFlagsRule()

    @Test
    fun getSymptomPermissionTypes_noPackage_hasData_returnsSymptomType() {
        val recordTypeInfoMap =
            mapOf<Class<out Record>, RecordTypeInfoResponse>(
                SymptomRecord::class.java to
                    RecordTypeInfoResponse(
                        setOf(
                            HealthPermissionCategory.SYMPTOM_CHILLS,
                            HealthPermissionCategory.SYMPTOM_BACK_PAIN,
                        ),
                        HealthDataCategory.SYMPTOMS,
                        listOf(getDataOrigin(TEST_APP_PACKAGE_NAME)),
                    )
            )

        val result = getSymptomPermissionTypes(recordTypeInfoMap, packageName = null)

        assertThat(result.category).isEqualTo(HealthDataCategory.SYMPTOMS)
        assertThat(result.data).containsExactly(FitnessPermissionType.SYMPTOM_ABDOMINAL_PAIN)
    }

    @Test
    fun getSymptomPermissionTypes_noPackage_noData_returnsEmpty() {
        val recordTypeInfoMap =
            mapOf<Class<out Record>, RecordTypeInfoResponse>(
                SymptomRecord::class.java to
                    RecordTypeInfoResponse(
                        setOf(HealthPermissionCategory.SYMPTOM_ABDOMINAL_PAIN),
                        HealthDataCategory.SYMPTOMS,
                        emptyList(),
                    )
            )

        val result = getSymptomPermissionTypes(recordTypeInfoMap, packageName = null)

        assertThat(result.category).isEqualTo(HealthDataCategory.SYMPTOMS)
        assertThat(result.data).isEmpty()
    }

    @Test
    fun getSymptomPermissionTypes_withPackage_hasMatchingData_returnsSymptomType() {
        val recordTypeInfoMap =
            mapOf<Class<out Record>, RecordTypeInfoResponse>(
                SymptomRecord::class.java to
                    RecordTypeInfoResponse(
                        setOf(HealthPermissionCategory.SYMPTOM_BRAIN_FOG),
                        HealthDataCategory.SYMPTOMS,
                        listOf(getDataOrigin(TEST_APP_PACKAGE_NAME)),
                    )
            )

        val result =
            getSymptomPermissionTypes(recordTypeInfoMap, packageName = TEST_APP_PACKAGE_NAME)

        assertThat(result.category).isEqualTo(HealthDataCategory.SYMPTOMS)
        assertThat(result.data).containsExactly(FitnessPermissionType.SYMPTOM_ABDOMINAL_PAIN)
    }

    @Test
    fun getSymptomPermissionTypes_withPackage_noMatchingData_returnsEmpty() {
        val recordTypeInfoMap =
            mapOf<Class<out Record>, RecordTypeInfoResponse>(
                SymptomRecord::class.java to
                    RecordTypeInfoResponse(
                        setOf(HealthPermissionCategory.SYMPTOM_ABDOMINAL_PAIN),
                        HealthDataCategory.SYMPTOMS,
                        listOf(getDataOrigin(TEST_APP_PACKAGE_NAME_2)),
                    )
            )

        val result =
            getSymptomPermissionTypes(recordTypeInfoMap, packageName = TEST_APP_PACKAGE_NAME)

        assertThat(result.category).isEqualTo(HealthDataCategory.SYMPTOMS)
        assertThat(result.data).isEmpty()
    }

    @Test
    fun hasData_permissionTypePresentWithData_returnsTrue() {
        val recordTypeInfoMap =
            mapOf<Class<out Record>, RecordTypeInfoResponse>(
                StepsRecord::class.java to
                    RecordTypeInfoResponse(
                        setOf(HealthPermissionCategory.STEPS),
                        HealthDataCategory.ACTIVITY,
                        listOf(getDataOrigin(TEST_APP_PACKAGE_NAME)),
                    )
            )

        val result = hasData(FitnessPermissionType.STEPS, recordTypeInfoMap)

        assertThat(result).isTrue()
    }

    @Test
    fun hasData_permissionTypePresentWithoutData_returnsFalse() {
        val recordTypeInfoMap =
            mapOf<Class<out Record>, RecordTypeInfoResponse>(
                StepsRecord::class.java to
                    RecordTypeInfoResponse(
                        setOf(HealthPermissionCategory.STEPS),
                        HealthDataCategory.ACTIVITY,
                        emptyList(),
                    )
            )

        val result = hasData(FitnessPermissionType.STEPS, recordTypeInfoMap)

        assertThat(result).isFalse()
    }

    @Test
    fun hasDataByApp_permissionTypePresentAndAppMatches_returnsTrue() {
        val recordTypeInfoMap =
            mapOf<Class<out Record>, RecordTypeInfoResponse>(
                StepsRecord::class.java to
                    RecordTypeInfoResponse(
                        setOf(HealthPermissionCategory.STEPS),
                        HealthDataCategory.ACTIVITY,
                        listOf(getDataOrigin(TEST_APP_PACKAGE_NAME)),
                    )
            )

        val result =
            hasDataByApp(FitnessPermissionType.STEPS, recordTypeInfoMap, TEST_APP_PACKAGE_NAME)

        assertThat(result).isTrue()
    }

    @Test
    fun hasDataByApp_permissionTypePresentAndAppDoesNotMatch_returnsFalse() {
        val recordTypeInfoMap =
            mapOf<Class<out Record>, RecordTypeInfoResponse>(
                StepsRecord::class.java to
                    RecordTypeInfoResponse(
                        setOf(HealthPermissionCategory.STEPS),
                        HealthDataCategory.ACTIVITY,
                        listOf(getDataOrigin(TEST_APP_PACKAGE_NAME_2)),
                    )
            )

        val result =
            hasDataByApp(FitnessPermissionType.STEPS, recordTypeInfoMap, TEST_APP_PACKAGE_NAME)

        assertThat(result).isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    fun getPermissionTypesPerCategory_ddpOn_currentDevice_includesLegacyDdp() {
        val currentDeviceId = "current_device_id"
        val recordTypeInfoMap =
            mapOf<Class<out Record>, RecordTypeInfoResponse>(
                StepsRecord::class.java to
                    RecordTypeInfoResponse(
                        setOf(HealthPermissionCategory.STEPS),
                        HealthDataCategory.ACTIVITY,
                        listOf(getDataOrigin(DEVICE_DATA_PROVIDER_PACKAGE_NAME)),
                    )
            )

        val result =
            getPermissionTypesPerCategory(
                HealthDataCategory.ACTIVITY,
                recordTypeInfoMap,
                packageName = currentDeviceId,
                currentDeviceId = currentDeviceId,
            )

        assertThat(result.category).isEqualTo(HealthDataCategory.ACTIVITY)
        assertThat(result.data).containsExactly(FitnessPermissionType.STEPS)
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    fun getPermissionTypesPerCategory_ddpOn_legacyDdpPackage_includesCurrentDevice() {
        val currentDeviceId = "current_device_id"
        val recordTypeInfoMap =
            mapOf<Class<out Record>, RecordTypeInfoResponse>(
                StepsRecord::class.java to
                    RecordTypeInfoResponse(
                        setOf(HealthPermissionCategory.STEPS),
                        HealthDataCategory.ACTIVITY,
                        listOf(getDataOrigin(currentDeviceId)),
                    )
            )

        val result =
            getPermissionTypesPerCategory(
                HealthDataCategory.ACTIVITY,
                recordTypeInfoMap,
                packageName = DEVICE_DATA_PROVIDER_PACKAGE_NAME,
                currentDeviceId = currentDeviceId,
            )

        assertThat(result.category).isEqualTo(HealthDataCategory.ACTIVITY)
        assertThat(result.data).containsExactly(FitnessPermissionType.STEPS)
    }

    @Test
    @DisableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    fun getPermissionTypesPerCategory_ddpOff_currentDevice_ignoresLegacyDdp() {
        val currentDeviceId = "current_device_id"
        val recordTypeInfoMap =
            mapOf<Class<out Record>, RecordTypeInfoResponse>(
                StepsRecord::class.java to
                    RecordTypeInfoResponse(
                        setOf(HealthPermissionCategory.STEPS),
                        HealthDataCategory.ACTIVITY,
                        listOf(getDataOrigin(DEVICE_DATA_PROVIDER_PACKAGE_NAME)),
                    )
            )

        val result =
            getPermissionTypesPerCategory(
                HealthDataCategory.ACTIVITY,
                recordTypeInfoMap,
                packageName = currentDeviceId,
                currentDeviceId = currentDeviceId,
            )

        assertThat(result.category).isEqualTo(HealthDataCategory.ACTIVITY)
        assertThat(result.data).isEmpty()
    }
}
