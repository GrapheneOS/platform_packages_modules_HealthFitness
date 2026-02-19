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
package com.android.healthconnect.controller.tests.data.access.api

import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.data.access.AppAccessMetadata
import com.android.healthconnect.controller.data.access.AppAccessState
import com.android.healthconnect.controller.data.access.LoadAccessUseCase
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.FitnessPermission
import com.android.healthconnect.controller.permissions.data.HealthPermissionType
import com.android.healthconnect.controller.permissions.data.MedicalPermissionType
import com.android.healthconnect.controller.permissions.data.PermissionsAccessType
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.shared.app.AppInfoReader
import com.android.healthconnect.controller.shared.app.AppPermissionsType.COMBINED_PERMISSIONS
import com.android.healthconnect.controller.shared.usecase.BaseUseCase
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import com.android.healthconnect.controller.tests.utils.FakeUseCaseRule
import com.android.healthconnect.controller.tests.utils.TEST_APP
import com.android.healthconnect.controller.tests.utils.TEST_APP_2
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME_2
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME_2
import com.android.healthconnect.controller.tests.utils.di.FakeGetGrantedHealthPermissionsUseCase
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations

@ExperimentalCoroutinesApi
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class LoadAccessUseCaseTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)
    @get:Rule val setFlagsRule = SetFlagsRule()
    @get:Rule val fakeUseCaseRule = FakeUseCaseRule()

    private lateinit var useCase:
        BaseUseCase<HealthPermissionType, Map<AppAccessState, List<AppAccessMetadata>>>
    private val fakeLoadFitnessTypeContributorAppsUseCase =
        fakeUseCaseRule.watch(FakeLoadFitnessTypeContributorAppsUseCase())
    private val fakeLoadMedicalTypeContributorAppsUseCase =
        fakeUseCaseRule.watch(FakeLoadMedicalTypeContributorAppsUseCase())
    private val fakeGetGrantedHealthPermissionsUseCase = FakeGetGrantedHealthPermissionsUseCase()

    @Inject lateinit var appInfoReader: AppInfoReader
    @Inject lateinit var healthPermissionReader: HealthPermissionReader

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        hiltRule.inject()
        useCase =
            LoadAccessUseCase(
                fakeLoadFitnessTypeContributorAppsUseCase,
                fakeLoadMedicalTypeContributorAppsUseCase,
                fakeGetGrantedHealthPermissionsUseCase,
                healthPermissionReader,
                appInfoReader,
                Dispatchers.Main,
            )
    }

    @Test
    fun noDataNorPermission_returnsEmptyMap() = runTest {
        val result = useCase.invoke(FitnessPermissionType.STEPS)
        assertThat(result).isInstanceOf(UseCaseResults.Success::class.java)
        val actual = (result as UseCaseResults.Success).data

        assertThat(actual[AppAccessState.Write]!!.size).isEqualTo(0)
        assertThat(actual[AppAccessState.Read]!!.size).isEqualTo(0)
        assertThat(actual[AppAccessState.Inactive]!!.size).isEqualTo(0)
    }

    @Test
    fun fitnessContributingApps_writeSteps_returnsCorrectApps() = runTest {
        fakeLoadFitnessTypeContributorAppsUseCase.updateList(listOf(TEST_APP, TEST_APP_2))
        val writeSteps =
            FitnessPermission(FitnessPermissionType.STEPS, PermissionsAccessType.WRITE).toString()
        fakeGetGrantedHealthPermissionsUseCase.updateData(TEST_APP_PACKAGE_NAME, listOf(writeSteps))

        val result = useCase.invoke(FitnessPermissionType.STEPS)
        assertThat(result).isInstanceOf(UseCaseResults.Success::class.java)
        val actual = (result as UseCaseResults.Success).data

        assertThat(actual[AppAccessState.Write]).isNotNull()
        assertThat(actual[AppAccessState.Write]!!.size).isEqualTo(1)
        assertThat(actual[AppAccessState.Write]!![0].appMetadata.packageName)
            .isEqualTo(TEST_APP_PACKAGE_NAME)
        assertThat(actual[AppAccessState.Write]!![0].appMetadata.appName).isEqualTo(TEST_APP_NAME)
        assertThat(actual[AppAccessState.Write]!![0].appPermissionsType)
            .isEqualTo(COMBINED_PERMISSIONS)
        assertThat(actual[AppAccessState.Read]).isNotNull()
        assertThat(actual[AppAccessState.Read]!!.size).isEqualTo(0)
        assertThat(actual[AppAccessState.Inactive]).isNotNull()
        assertThat(actual[AppAccessState.Inactive]!!.size).isEqualTo(1)
        assertThat(actual[AppAccessState.Inactive]!![0].appMetadata.packageName)
            .isEqualTo(TEST_APP_PACKAGE_NAME_2)
        assertThat(actual[AppAccessState.Inactive]!![0].appMetadata.appName)
            .isEqualTo(TEST_APP_NAME_2)
    }

    @Test
    fun fitnessContributingApps_readSteps_returnsCorrectApps() = runTest {
        fakeLoadFitnessTypeContributorAppsUseCase.updateList(listOf(TEST_APP, TEST_APP_2))
        val writeSteps =
            FitnessPermission(FitnessPermissionType.STEPS, PermissionsAccessType.READ).toString()
        fakeGetGrantedHealthPermissionsUseCase.updateData(TEST_APP_PACKAGE_NAME, listOf(writeSteps))

        val result = useCase.invoke(FitnessPermissionType.STEPS)
        assertThat(result).isInstanceOf(UseCaseResults.Success::class.java)
        val actual = (result as UseCaseResults.Success).data

        assertThat(actual[AppAccessState.Write]).isNotNull()
        assertThat(actual[AppAccessState.Write]!!.size).isEqualTo(0)
        assertThat(actual[AppAccessState.Read]).isNotNull()
        assertThat(actual[AppAccessState.Read]!!.size).isEqualTo(1)
        assertThat(actual[AppAccessState.Read]!![0].appMetadata.packageName)
            .isEqualTo(TEST_APP_PACKAGE_NAME)
        assertThat(actual[AppAccessState.Read]!![0].appMetadata.appName).isEqualTo(TEST_APP_NAME)
        assertThat(actual[AppAccessState.Read]!![0].appPermissionsType)
            .isEqualTo(COMBINED_PERMISSIONS)
        assertThat(actual[AppAccessState.Inactive]).isNotNull()
        assertThat(actual[AppAccessState.Inactive]!!.size).isEqualTo(1)
        assertThat(actual[AppAccessState.Inactive]!![0].appMetadata.packageName)
            .isEqualTo(TEST_APP_PACKAGE_NAME_2)
        assertThat(actual[AppAccessState.Inactive]!![0].appMetadata.appName)
            .isEqualTo(TEST_APP_NAME_2)
    }

    @Test
    fun medicalData_readImmunization_returnsCorrectApps() = runTest {
        fakeLoadFitnessTypeContributorAppsUseCase.updateList(listOf(TEST_APP_2))
        fakeLoadMedicalTypeContributorAppsUseCase.updateList(listOf(TEST_APP))
        val steps =
            FitnessPermission(FitnessPermissionType.STEPS, PermissionsAccessType.WRITE).toString()
        val immunization =
            HealthPermission.MedicalPermission(MedicalPermissionType.VACCINES).toString()
        fakeGetGrantedHealthPermissionsUseCase.updateData(
            TEST_APP_PACKAGE_NAME,
            listOf(steps, immunization),
        )

        val result = useCase.invoke(MedicalPermissionType.VACCINES)
        assertThat(result).isInstanceOf(UseCaseResults.Success::class.java)
        val actual = (result as UseCaseResults.Success).data

        assertThat(actual[AppAccessState.Write]).isNotNull()
        assertThat(actual[AppAccessState.Write]!!.size).isEqualTo(0)
        assertThat(actual[AppAccessState.Read]).isNotNull()
        assertThat(actual[AppAccessState.Read]!!.size).isEqualTo(1)
        assertThat(actual[AppAccessState.Read]!![0].appMetadata.packageName)
            .isEqualTo(TEST_APP_PACKAGE_NAME)
        assertThat(actual[AppAccessState.Read]!![0].appMetadata.appName).isEqualTo(TEST_APP_NAME)
        assertThat(actual[AppAccessState.Read]!![0].appPermissionsType)
            .isEqualTo(COMBINED_PERMISSIONS)
        assertThat(actual[AppAccessState.Inactive]).isNotNull()
        assertThat(actual[AppAccessState.Inactive]!!.size).isEqualTo(0)
    }

    @Test
    fun medicalData_immunizationAndAllMedicalData_returnsCorrectApps() = runTest {
        fakeLoadFitnessTypeContributorAppsUseCase.updateList(listOf(TEST_APP_2))
        fakeLoadMedicalTypeContributorAppsUseCase.updateList(listOf(TEST_APP))
        val steps =
            FitnessPermission(FitnessPermissionType.STEPS, PermissionsAccessType.WRITE).toString()
        val immunization =
            HealthPermission.MedicalPermission(MedicalPermissionType.VACCINES).toString()
        val allMedicalData =
            HealthPermission.MedicalPermission(MedicalPermissionType.ALL_MEDICAL_DATA).toString()
        fakeGetGrantedHealthPermissionsUseCase.updateData(
            TEST_APP_PACKAGE_NAME,
            listOf(steps, immunization, allMedicalData),
        )

        val result = useCase.invoke(MedicalPermissionType.VACCINES)
        assertThat(result).isInstanceOf(UseCaseResults.Success::class.java)
        val actual = (result as UseCaseResults.Success).data

        assertThat(actual[AppAccessState.Write]).isNotNull()
        assertThat(actual[AppAccessState.Write]!!.size).isEqualTo(1)
        assertThat(actual[AppAccessState.Write]!![0].appMetadata.packageName)
            .isEqualTo(TEST_APP_PACKAGE_NAME)
        assertThat(actual[AppAccessState.Write]!![0].appMetadata.appName).isEqualTo(TEST_APP_NAME)
        assertThat(actual[AppAccessState.Write]!![0].appPermissionsType)
            .isEqualTo(COMBINED_PERMISSIONS)
        assertThat(actual[AppAccessState.Read]).isNotNull()
        assertThat(actual[AppAccessState.Read]!!.size).isEqualTo(1)
        assertThat(actual[AppAccessState.Read]!![0].appMetadata.packageName)
            .isEqualTo(TEST_APP_PACKAGE_NAME)
        assertThat(actual[AppAccessState.Read]!![0].appMetadata.appName).isEqualTo(TEST_APP_NAME)
        assertThat(actual[AppAccessState.Read]!![0].appPermissionsType)
            .isEqualTo(COMBINED_PERMISSIONS)
        assertThat(actual[AppAccessState.Inactive]).isNotNull()
        assertThat(actual[AppAccessState.Inactive]!!.size).isEqualTo(0)
    }

    @Test
    fun medicalData_writeAllMedicalData_returnsCorrectApps() = runTest {
        fakeLoadFitnessTypeContributorAppsUseCase.updateList(listOf(TEST_APP_2))
        fakeLoadMedicalTypeContributorAppsUseCase.updateList(listOf(TEST_APP))
        val steps =
            FitnessPermission(FitnessPermissionType.STEPS, PermissionsAccessType.WRITE).toString()
        val allMedicalData =
            HealthPermission.MedicalPermission(MedicalPermissionType.ALL_MEDICAL_DATA).toString()
        fakeGetGrantedHealthPermissionsUseCase.updateData(
            TEST_APP_PACKAGE_NAME,
            listOf(steps, allMedicalData),
        )

        val result = useCase.invoke(MedicalPermissionType.ALL_MEDICAL_DATA)
        assertThat(result).isInstanceOf(UseCaseResults.Success::class.java)
        val actual = (result as UseCaseResults.Success).data

        assertThat(actual[AppAccessState.Write]).isNotNull()
        assertThat(actual[AppAccessState.Write]!!.size).isEqualTo(1)
        assertThat(actual[AppAccessState.Write]!![0].appMetadata.packageName)
            .isEqualTo(TEST_APP_PACKAGE_NAME)
        assertThat(actual[AppAccessState.Write]!![0].appMetadata.appName).isEqualTo(TEST_APP_NAME)
        assertThat(actual[AppAccessState.Write]!![0].appPermissionsType)
            .isEqualTo(COMBINED_PERMISSIONS)
        assertThat(actual[AppAccessState.Read]).isNotNull()
        assertThat(actual[AppAccessState.Read]!!.size).isEqualTo(0)
        assertThat(actual[AppAccessState.Inactive]).isNotNull()
        assertThat(actual[AppAccessState.Inactive]!!.size).isEqualTo(0)
    }

    @Test
    fun medicalData_immunizationInactive_returnsCorrectApps() = runTest {
        fakeLoadFitnessTypeContributorAppsUseCase.updateList(listOf(TEST_APP_2))
        fakeLoadMedicalTypeContributorAppsUseCase.updateList(listOf(TEST_APP))

        val result = useCase.invoke(MedicalPermissionType.VACCINES)
        assertThat(result).isInstanceOf(UseCaseResults.Success::class.java)
        val actual = (result as UseCaseResults.Success).data

        assertThat(actual[AppAccessState.Write]).isNotNull()
        assertThat(actual[AppAccessState.Write]!!.size).isEqualTo(0)
        assertThat(actual[AppAccessState.Read]).isNotNull()
        assertThat(actual[AppAccessState.Read]!!.size).isEqualTo(0)
        assertThat(actual[AppAccessState.Inactive]).isNotNull()
        assertThat(actual[AppAccessState.Inactive]!!.size).isEqualTo(1)
        assertThat(actual[AppAccessState.Inactive]!![0].appMetadata.packageName)
            .isEqualTo(TEST_APP_PACKAGE_NAME)
        assertThat(actual[AppAccessState.Inactive]!![0].appMetadata.appName)
            .isEqualTo(TEST_APP_NAME)
    }
}
