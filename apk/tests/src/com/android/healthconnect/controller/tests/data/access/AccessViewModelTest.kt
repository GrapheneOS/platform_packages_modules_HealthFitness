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

import android.health.connect.HealthConnectManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.data.access.AccessViewModel
import com.android.healthconnect.controller.data.access.AppAccessMetadata
import com.android.healthconnect.controller.data.access.AppAccessState
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.permissions.data.MedicalPermissionType
import com.android.healthconnect.controller.service.HealthManagerModule
import com.android.healthconnect.controller.shared.app.AppInfoReader
import com.android.healthconnect.controller.shared.app.AppPermissionsType.COMBINED_PERMISSIONS
import com.android.healthconnect.controller.shared.app.AppPermissionsType.MEDICAL_PERMISSIONS_ONLY
import com.android.healthconnect.controller.tests.data.access.api.FakeLoadAccessUseCase
import com.android.healthconnect.controller.tests.data.access.api.FakeLoadSymptomAccessUseCase
import com.android.healthconnect.controller.tests.utils.FakeUseCaseRule
import com.android.healthconnect.controller.tests.utils.InstantTaskExecutorRule
import com.android.healthconnect.controller.tests.utils.TEST_APP
import com.android.healthconnect.controller.tests.utils.TEST_APP_2
import com.android.healthconnect.controller.tests.utils.TEST_APP_3
import com.android.healthconnect.controller.tests.utils.TestObserver
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

@ExperimentalCoroutinesApi
@HiltAndroidTest
@UninstallModules(HealthManagerModule::class)
@RunWith(AndroidJUnit4::class)
class AccessViewModelTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)
    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()
    @get:Rule val fakeUseCaseRule = FakeUseCaseRule()

    @BindValue val healthConnectManager: HealthConnectManager = mock()

    private val fakeLoadAccessUseCase = fakeUseCaseRule.watch(FakeLoadAccessUseCase())
    private val fakeLoadSymptomAccessUseCase = fakeUseCaseRule.watch(FakeLoadSymptomAccessUseCase())

    private lateinit var viewModel: AccessViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Inject lateinit var appInfoReader: AppInfoReader

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        hiltRule.inject()
        viewModel = AccessViewModel(fakeLoadAccessUseCase, fakeLoadSymptomAccessUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadAppMetadataMap_returnsCorrectApps() = runTest {
        val expected =
            mapOf(
                AppAccessState.Read to
                    listOf(AppAccessMetadata(TEST_APP), AppAccessMetadata(TEST_APP_2)),
                AppAccessState.Write to listOf(AppAccessMetadata(TEST_APP_2, COMBINED_PERMISSIONS)),
                AppAccessState.Inactive to
                    listOf(AppAccessMetadata(TEST_APP_3, MEDICAL_PERMISSIONS_ONLY)),
            )
        fakeLoadAccessUseCase.updateMap(expected)

        val testObserver = TestObserver<AccessViewModel.AccessScreenState>()
        viewModel.appMetadataMap.observeForever(testObserver)
        viewModel.loadAppMetaDataMap(FitnessPermissionType.STEPS)
        advanceUntilIdle()

        assertThat(testObserver.getLastValue())
            .isEqualTo(AccessViewModel.AccessScreenState.WithData(expected))
    }

    @Test
    fun loadAppMetadataMap_medicalPermissions_returnsCorrectApps() = runTest {
        val expected =
            mapOf(
                AppAccessState.Read to
                    listOf(AppAccessMetadata(TEST_APP), AppAccessMetadata(TEST_APP_2)),
                AppAccessState.Write to listOf(AppAccessMetadata(TEST_APP_2, COMBINED_PERMISSIONS)),
                AppAccessState.Inactive to
                    listOf(AppAccessMetadata(TEST_APP_3, MEDICAL_PERMISSIONS_ONLY)),
            )
        fakeLoadAccessUseCase.updateMap(expected)

        val testObserver = TestObserver<AccessViewModel.AccessScreenState>()
        viewModel.appMetadataMap.observeForever(testObserver)
        viewModel.loadAppMetaDataMap(MedicalPermissionType.VACCINES)
        advanceUntilIdle()

        assertThat(testObserver.getLastValue())
            .isEqualTo(AccessViewModel.AccessScreenState.WithData(expected))
    }

    @Test
    fun loadAppMetadataMap_showAllSymptoms_returnsCorrectApps() = runTest {
        val expected =
            mapOf(
                AppAccessState.Read to
                    listOf(AppAccessMetadata(TEST_APP), AppAccessMetadata(TEST_APP_2)),
                AppAccessState.Write to listOf(AppAccessMetadata(TEST_APP_2, COMBINED_PERMISSIONS)),
                AppAccessState.Inactive to
                    listOf(AppAccessMetadata(TEST_APP_3, MEDICAL_PERMISSIONS_ONLY)),
            )
        fakeLoadSymptomAccessUseCase.updateMap(expected)

        val testObserver = TestObserver<AccessViewModel.AccessScreenState>()
        viewModel.appMetadataMap.observeForever(testObserver)
        viewModel.loadAppMetaDataMap(permissionType = null, showAllSymptoms = true)
        advanceUntilIdle()

        assertThat(testObserver.getLastValue())
            .isEqualTo(AccessViewModel.AccessScreenState.WithData(expected))
        assertThat(fakeLoadSymptomAccessUseCase.numberOfInvocations).isEqualTo(1)
    }

    @Test
    fun loadAppMetadataMap_showAllSymptoms_loadFailed() = runTest {
        fakeLoadSymptomAccessUseCase.setForceFail(true)

        val testObserver = TestObserver<AccessViewModel.AccessScreenState>()
        viewModel.appMetadataMap.observeForever(testObserver)
        viewModel.loadAppMetaDataMap(permissionType = null, showAllSymptoms = true)
        advanceUntilIdle()

        assertThat(testObserver.getLastValue()).isEqualTo(AccessViewModel.AccessScreenState.Error)
        assertThat(fakeLoadSymptomAccessUseCase.numberOfInvocations).isEqualTo(1)
    }

    @Test
    fun loadAppMetadataMap_showAllSymptoms_resetsInvocationCount() = runTest {
        val expected =
            mapOf(
                AppAccessState.Read to
                    listOf(AppAccessMetadata(TEST_APP), AppAccessMetadata(TEST_APP_2)),
                AppAccessState.Write to listOf(AppAccessMetadata(TEST_APP_2, COMBINED_PERMISSIONS)),
                AppAccessState.Inactive to
                    listOf(AppAccessMetadata(TEST_APP_3, MEDICAL_PERMISSIONS_ONLY)),
            )
        fakeLoadSymptomAccessUseCase.updateMap(expected)

        viewModel.loadAppMetaDataMap(permissionType = null, showAllSymptoms = true)
        advanceUntilIdle()
        assertThat(fakeLoadSymptomAccessUseCase.numberOfInvocations).isEqualTo(1)

        fakeLoadSymptomAccessUseCase.reset()
        assertThat(fakeLoadSymptomAccessUseCase.numberOfInvocations).isEqualTo(0)

        fakeLoadSymptomAccessUseCase.updateMap(expected)
        viewModel.loadAppMetaDataMap(permissionType = null, showAllSymptoms = true)
        advanceUntilIdle()
        assertThat(fakeLoadSymptomAccessUseCase.numberOfInvocations).isEqualTo(1)
    }

    @Test
    fun loadAppMetadataMap_nullPermissionTypeAndNotShowAllSymptoms_returnsError() = runTest {
        val testObserver = TestObserver<AccessViewModel.AccessScreenState>()
        viewModel.appMetadataMap.observeForever(testObserver)
        viewModel.loadAppMetaDataMap(permissionType = null, showAllSymptoms = false)
        advanceUntilIdle()

        assertThat(testObserver.getLastValue()).isEqualTo(AccessViewModel.AccessScreenState.Error)
        assertThat(fakeLoadSymptomAccessUseCase.numberOfInvocations).isEqualTo(0)
        assertThat(fakeLoadAccessUseCase.numberOfInvocations).isEqualTo(0)
    }

    @Test
    fun loadAppMetadataMap_symptomType_invokesSymptomUseCase() = runTest {
        fakeLoadSymptomAccessUseCase.reset()
        fakeLoadAccessUseCase.reset()
        val expected = mapOf<AppAccessState, List<AppAccessMetadata>>()
        fakeLoadSymptomAccessUseCase.updateMap(expected)

        viewModel.loadAppMetaDataMap(
            FitnessPermissionType.SYMPTOM_ABDOMINAL_PAIN,
            showAllSymptoms = true,
        )
        advanceUntilIdle()

        assertThat(fakeLoadSymptomAccessUseCase.numberOfInvocations).isEqualTo(1)
        assertThat(fakeLoadAccessUseCase.numberOfInvocations).isEqualTo(0)
    }

    @Test
    fun loadAppMetaDataMap_nonSymptomType_invokesLoadAccessUseCase() = runTest {
        fakeLoadSymptomAccessUseCase.reset()
        fakeLoadAccessUseCase.reset()
        val expected = mapOf<AppAccessState, List<AppAccessMetadata>>()
        fakeLoadAccessUseCase.updateMap(expected)

        viewModel.loadAppMetaDataMap(FitnessPermissionType.STEPS)
        advanceUntilIdle()

        assertThat(fakeLoadSymptomAccessUseCase.numberOfInvocations).isEqualTo(0)
        assertThat(fakeLoadAccessUseCase.numberOfInvocations).isEqualTo(1)
    }

    @Test
    fun loadAppMetadataMap_loadAccessUseCaseFails_returnsError() = runTest {
        fakeLoadAccessUseCase.reset()
        fakeLoadAccessUseCase.setForceFail(true)

        val testObserver = TestObserver<AccessViewModel.AccessScreenState>()
        viewModel.appMetadataMap.observeForever(testObserver)
        viewModel.loadAppMetaDataMap(FitnessPermissionType.STEPS)
        advanceUntilIdle()

        assertThat(testObserver.getLastValue()).isEqualTo(AccessViewModel.AccessScreenState.Error)
    }
}
