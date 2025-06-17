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
package com.android.healthconnect.controller.tests.data.access

import android.content.Context
import android.health.connect.HealthConnectManager
import android.health.connect.MedicalResourceTypeInfo
import android.health.connect.datatypes.MedicalResource
import android.os.OutcomeReceiver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.data.access.LoadMedicalTypeContributorAppsUseCase
import com.android.healthconnect.controller.permissions.data.MedicalPermissionType
import com.android.healthconnect.controller.shared.app.AppInfoReader
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME_2
import com.android.healthconnect.controller.tests.utils.TEST_MEDICAL_DATA_SOURCE
import com.android.healthconnect.controller.tests.utils.TEST_MEDICAL_DATA_SOURCE_2
import com.android.healthconnect.controller.tests.utils.TEST_MEDICAL_DATA_SOURCE_DIFFERENT_APP
import com.android.healthconnect.controller.tests.utils.createFakeAppInfoReader
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.invocation.InvocationOnMock

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class LoadMedicalTypeContributorAppsUseCaseTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @BindValue lateinit var appInfoReader: AppInfoReader
    private lateinit var context: Context
    private val healthConnectManager: HealthConnectManager =
        Mockito.mock(HealthConnectManager::class.java)
    private lateinit var loadMedicalTypeContributorAppsUseCase:
        LoadMedicalTypeContributorAppsUseCase

    @Before
    fun setup() = runTest {
        MockitoAnnotations.initMocks(this)
        context = InstrumentationRegistry.getInstrumentation().context
        appInfoReader = createFakeAppInfoReader()
        hiltRule.inject()
        loadMedicalTypeContributorAppsUseCase =
            LoadMedicalTypeContributorAppsUseCase(
                appInfoReader,
                healthConnectManager,
                Dispatchers.Main,
            )
    }

    @Test
    fun whenNoData_returnsEmptyMap() = runTest {
        Mockito.doAnswer(prepareAnswer(listOf()))
            .`when`(healthConnectManager)
            .queryAllMedicalResourceTypeInfos(ArgumentMatchers.any(), ArgumentMatchers.any())
        val result = loadMedicalTypeContributorAppsUseCase.invoke(MedicalPermissionType.VACCINES)
        val expected = listOf<AppMetadata>()
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun whenOneContributingPackage_returnsCorrectApp() = runTest {
        val medicalResourceTypeInfos =
            listOf(
                MedicalResourceTypeInfo(
                    MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES,
                    setOf(TEST_MEDICAL_DATA_SOURCE, TEST_MEDICAL_DATA_SOURCE_2),
                ),
                MedicalResourceTypeInfo(
                    MedicalResource.MEDICAL_RESOURCE_TYPE_MEDICATIONS,
                    setOf(TEST_MEDICAL_DATA_SOURCE_2, TEST_MEDICAL_DATA_SOURCE_DIFFERENT_APP),
                ),
            )
        Mockito.doAnswer(prepareAnswer(medicalResourceTypeInfos))
            .`when`(healthConnectManager)
            .queryAllMedicalResourceTypeInfos(ArgumentMatchers.any(), ArgumentMatchers.any())
        val result = loadMedicalTypeContributorAppsUseCase.invoke(MedicalPermissionType.VACCINES)
        assertThat(result.size).isEqualTo(1)
        assertThat(result[0].packageName).isEqualTo(TEST_APP_PACKAGE_NAME)
    }

    @Test
    fun whenMultipleContributingPackages_returnsCorrectApps() = runTest {
        val medicalResourceTypeInfos =
            listOf(
                MedicalResourceTypeInfo(
                    MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES,
                    setOf(TEST_MEDICAL_DATA_SOURCE, TEST_MEDICAL_DATA_SOURCE_2),
                ),
                MedicalResourceTypeInfo(
                    MedicalResource.MEDICAL_RESOURCE_TYPE_MEDICATIONS,
                    setOf(TEST_MEDICAL_DATA_SOURCE_2, TEST_MEDICAL_DATA_SOURCE_DIFFERENT_APP),
                ),
            )
        Mockito.doAnswer(prepareAnswer(medicalResourceTypeInfos))
            .`when`(healthConnectManager)
            .queryAllMedicalResourceTypeInfos(ArgumentMatchers.any(), ArgumentMatchers.any())
        val result = loadMedicalTypeContributorAppsUseCase.invoke(MedicalPermissionType.MEDICATIONS)
        assertThat(result.size).isEqualTo(2)
        assertThat(result[0].packageName).isEqualTo(TEST_APP_PACKAGE_NAME)
        assertThat(result[1].packageName).isEqualTo(TEST_APP_PACKAGE_NAME_2)
    }

    private fun prepareAnswer(
        medicalResourceTypeInfos: List<MedicalResourceTypeInfo>
    ): (InvocationOnMock) -> List<MedicalResourceTypeInfo> {
        val answer = { args: InvocationOnMock ->
            val receiver = args.arguments[1] as OutcomeReceiver<Any?, *>
            receiver.onResult(medicalResourceTypeInfos)
            medicalResourceTypeInfos
        }
        return answer
    }
}
