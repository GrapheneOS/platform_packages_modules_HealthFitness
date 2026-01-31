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

package com.android.healthconnect.controller.tests.matchmaking

import android.health.connect.DeviceDataProviderInfo
import android.health.connect.DeviceDataSourceInfo
import android.health.connect.HealthConnectManager.RESULT_DEVICE_ONBOARDING_ABORTED
import android.health.connect.HealthConnectManager.RESULT_DEVICE_ONBOARDING_ALLOWED
import android.health.connect.HealthConnectManager.RESULT_DEVICE_ONBOARDING_DENIED
import android.health.connect.HealthPermissions.WRITE_EXERCISE
import android.health.connect.HealthPermissions.WRITE_SLEEP
import android.health.connect.HealthPermissions.WRITE_STEPS
import android.health.connect.datatypes.DataOrigin
import android.health.connect.datatypes.ExerciseSessionRecord
import android.health.connect.datatypes.StepsRecord
import android.health.connect.device.DeviceDataTypeAdvertisement
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.matchmaking.MatchmakingViewModel
import com.android.healthconnect.controller.matchmaking.MatchmakingViewModel.MatchmakingState.LoadingFailed
import com.android.healthconnect.controller.matchmaking.MatchmakingViewModel.MatchmakingState.WithData
import com.android.healthconnect.controller.matchmaking.api.GetMatchingDataSourcesUseCase
import com.android.healthconnect.controller.matchmaking.api.GetMatchingDataSourcesUseCase.GetMatchingDataSourcesInput
import com.android.healthconnect.controller.matchmaking.api.GetMatchingDataSourcesUseCase.MatchingDataSources
import com.android.healthconnect.controller.matchmaking.api.MatchmakingAppData
import com.android.healthconnect.controller.matchmaking.api.MatchmakingDeviceData
import com.android.healthconnect.controller.matchmaking.api.RecordMatchmakingDenialUseCase
import com.android.healthconnect.controller.matchmaking.api.RecordMatchmakingDenialUseCase.RecordMatchmakingDenialInput
import com.android.healthconnect.controller.permissions.api.HealthPermissionManager
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.permissions.data.PermissionsAccessType
import com.android.healthconnect.controller.shared.app.AppInfoReader
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import com.android.healthconnect.controller.tests.utils.InstantTaskExecutorRule
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME_2
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME_2
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME_3
import com.android.healthconnect.controller.tests.utils.createFakeAppInfoReader
import com.android.healthfitness.flags.Flags
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@HiltAndroidTest
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class MatchmakingViewModelTest {

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()
    @get:Rule val setFlagsRule = SetFlagsRule()
    @BindValue lateinit var appInfoReader: AppInfoReader

    private val getMatchingDataSourcesUseCase: GetMatchingDataSourcesUseCase = mock()
    private val recordMatchmakingDenialUseCase: RecordMatchmakingDenialUseCase = mock()
    private val healthPermissionManager: HealthPermissionManager = mock()

    private lateinit var viewModel: MatchmakingViewModel

    companion object {
        private const val TEST_WATCH_DEVICE_PACKAGE_NAME = "com.example.watchdevice"
    }

    @Before
    fun setup() = runTest {
        Dispatchers.setMain(Dispatchers.Unconfined)
        appInfoReader = createFakeAppInfoReader()
        viewModel =
            MatchmakingViewModel(
                getMatchingDataSourcesUseCase,
                recordMatchmakingDenialUseCase,
                appInfoReader,
                SavedStateHandle(),
                healthPermissionManager,
            )
        viewModel.matchingAppsCount.postValue(0)
    }

    private suspend fun stubGetMatchingDataSourcesUseCase(
        testMatchingApps: List<MatchmakingAppData> = emptyList(),
        testMatchingDevices: List<MatchmakingDeviceData> = emptyList(),
    ) {
        val useCaseResult =
            UseCaseResults.Success(MatchingDataSources(testMatchingApps, testMatchingDevices))
        whenever(getMatchingDataSourcesUseCase.invoke(any())).doReturn(useCaseResult)
    }

    private suspend fun setupLoadMatchmakingDataWithSuccess() {
        val packageName = TEST_APP_PACKAGE_NAME
        val recordTypeNames = arrayOf(StepsRecord::class.java.name)

        stubGetMatchingDataSourcesUseCase(getExpectedApps(), getExpectedDevices())

        viewModel.loadMatchmakingData(packageName, recordTypeNames)
    }

    private fun getExpectedApps(): List<MatchmakingAppData> {
        return listOf(
            MatchmakingAppData(
                AppMetadata("com.example.fitapp", "FitApp", null),
                listOf(
                    HealthPermission.fromPermissionString(WRITE_STEPS)
                        as HealthPermission.FitnessPermission
                ),
            )
        )
    }

    private fun getExpectedDevices(): List<MatchmakingDeviceData> {
        return listOf(
            MatchmakingDeviceData(
                DeviceDataSourceInfo(
                    DataOrigin.Builder().setPackageName("com.example.watchdevice").build(),
                    android.health.connect.datatypes.Device.Builder()
                        .setManufacturer("Google")
                        .setModel("Pixel Watch")
                        .setType(2)
                        .build(),
                    false,
                    listOf(
                        DeviceDataProviderInfo(
                            "com.google.android.apps.fitness",
                            "MyFit",
                            "",
                            "",
                            setOf(
                                DeviceDataTypeAdvertisement.Builder(StepsRecord::class.java)
                                    .setAvailable(true)
                                    .setUserEnabled(false)
                                    .setVisibleByDefaultInMatchmaking(false)
                                    .build()
                            ),
                        )
                    ),
                ),
                emptyList(),
            )
        )
    }

    @Test
    @EnableFlags(
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_UI_MATCHMAKING_SCREEN,
    )
    fun loadMatchmakingData_withDdpApis_withSuccess_updatesStateToWithData() = runTest {
        setupLoadMatchmakingDataWithSuccess()

        val state = viewModel.matchmakingState.value
        assertThat(state).isInstanceOf(WithData::class.java)
        val data = (state as WithData)
        assertThat(data.matchingApps).isEqualTo(getExpectedApps())
        assertThat(data.matchingDevices).isEqualTo(getExpectedDevices())
    }

    @Test
    @DisableFlags(
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_UI_MATCHMAKING_SCREEN,
    )
    fun loadMatchmakingData_withoutDdpApis_withSuccess_updatesStateToWithData() = runTest {
        val packageName = TEST_APP_PACKAGE_NAME
        val recordTypeNames = arrayOf(StepsRecord::class.java.name)

        stubGetMatchingDataSourcesUseCase(getExpectedApps(), emptyList())

        viewModel.loadMatchmakingData(packageName, recordTypeNames)

        val state = viewModel.matchmakingState.value
        assertThat(state).isInstanceOf(WithData::class.java)
        val data = (state as WithData)
        assertThat(data.matchingApps).isEqualTo(getExpectedApps())
        assertThat(data.matchingDevices).isEmpty()
    }

    @Test
    @EnableFlags(
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_UI_MATCHMAKING_SCREEN,
    )
    fun loadMatchmakingData_withIncludedAndExcludedSources_callsUseCaseWithCorrectOrigins() =
        runTest {
            val packageName = TEST_APP_PACKAGE_NAME
            val recordTypeNames = arrayOf(StepsRecord::class.java.name)
            val included = arrayOf("inc.pkg")
            val excluded = arrayOf("exc.pkg")

            stubGetMatchingDataSourcesUseCase(emptyList(), emptyList())
            val captor = argumentCaptor<GetMatchingDataSourcesInput>()
            whenever(getMatchingDataSourcesUseCase.invoke(captor.capture()))
                .doReturn(UseCaseResults.Success(MatchingDataSources(emptyList(), emptyList())))

            viewModel.loadMatchmakingData(packageName, recordTypeNames, included, excluded)

            val input = captor.firstValue
            assertThat(input.includedDataOrigins.map { it.packageName }).containsExactly("inc.pkg")
            assertThat(input.excludedDataOrigins.map { it.packageName }).containsExactly("exc.pkg")
        }

    @Test
    fun loadMatchmakingData_withError_updatesStateToLoadingFailed() = runTest {
        val packageName = TEST_APP_PACKAGE_NAME
        val recordTypeNames = arrayOf(StepsRecord::class.java.name)
        val exception = IllegalStateException("Error")
        val useCaseResult = UseCaseResults.Failed(exception)
        whenever(getMatchingDataSourcesUseCase.invoke(any())).doReturn(useCaseResult)

        viewModel.loadMatchmakingData(packageName, recordTypeNames)

        assertThat(viewModel.matchmakingState.value).isInstanceOf(LoadingFailed::class.java)
    }

    @Test
    fun loadMatchmakingData_withNullRecordTypes_callsUseCaseWithEmptySet() = runTest {
        val packageName = TEST_APP_PACKAGE_NAME
        stubGetMatchingDataSourcesUseCase(testMatchingDevices = emptyList())

        val captor = argumentCaptor<GetMatchingDataSourcesInput>()
        whenever(getMatchingDataSourcesUseCase.invoke(captor.capture()))
            .doReturn(UseCaseResults.Success(MatchingDataSources(emptyList(), emptyList())))

        viewModel.loadMatchmakingData(packageName, null)

        assertThat(captor.firstValue.recordTypes).isEmpty()
    }

    @Test
    fun loadMatchmakingData_returnsSortedAppsAndPermissions() = runTest {
        val appA =
            MatchmakingAppData(
                AppMetadata("a.package", "A App", null),
                listOf(
                    HealthPermission.FitnessPermission(
                        FitnessPermissionType.STEPS,
                        PermissionsAccessType.READ,
                    ),
                    HealthPermission.FitnessPermission(
                        FitnessPermissionType.EXERCISE,
                        PermissionsAccessType.READ,
                    ),
                ),
            )
        val appB = MatchmakingAppData(AppMetadata("b.package", "B App", null), emptyList())

        stubGetMatchingDataSourcesUseCase(listOf(appB, appA), emptyList())

        viewModel.loadMatchmakingData(TEST_APP_PACKAGE_NAME, arrayOf(StepsRecord::class.java.name))

        val state = viewModel.matchmakingState.value

        val data = (state as WithData).matchingApps
        assertThat(data.size).isEqualTo(2)
        assertThat(data[0].metadata.appName).isEqualTo("A App")
        assertThat(data[1].metadata.appName).isEqualTo("B App")
        assertThat(data[0].permissions[0].fitnessPermissionType)
            .isEqualTo(FitnessPermissionType.EXERCISE)
        assertThat(data[0].permissions[1].fitnessPermissionType)
            .isEqualTo(FitnessPermissionType.STEPS)
    }

    @Test
    fun addPermissionToGrantedList_addsPermissionToGrantedPermissions() {
        val permission =
            HealthPermission.fromPermissionString(WRITE_STEPS) as HealthPermission.FitnessPermission

        viewModel.addAppPermissionToGrantedList(TEST_APP_PACKAGE_NAME, permission)

        assertThat(viewModel.grantedPermissions.value?.get(TEST_APP_PACKAGE_NAME))
            .contains(permission)
        assertThat(viewModel.atLeastOnePermissionGranted.value).isTrue()
    }

    @Test
    fun removePermissionFromGrantedList_removesPermissionFromGrantedPermissions() {
        val permission =
            HealthPermission.fromPermissionString(WRITE_STEPS) as HealthPermission.FitnessPermission
        viewModel.addAppPermissionToGrantedList(TEST_APP_PACKAGE_NAME, permission)

        viewModel.removePermissionFromGrantedList(TEST_APP_PACKAGE_NAME, permission)

        assertThat(viewModel.grantedPermissions.value?.get(TEST_APP_PACKAGE_NAME)).isNull()
        assertThat(viewModel.atLeastOnePermissionGranted.value).isFalse()
    }

    @Test
    @EnableFlags(
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_UI_MATCHMAKING_SCREEN,
    )
    fun addAllPermissionsToGrantedList_addsAllPermissionsToGrantedPermissions() = runTest {
        setupWithData()

        viewModel.addAllPermissionsToGrantedList(TEST_APP_PACKAGE_NAME)
        viewModel.addAllPermissionsToGrantedList(TEST_APP_PACKAGE_NAME_2)
        viewModel.addDevicePermissionToGrantedList(TEST_WATCH_DEVICE_PACKAGE_NAME)

        val state = viewModel.matchmakingState.value as WithData
        val gran = viewModel.grantedPermissions
        assertThat(gran.value?.get(TEST_APP_PACKAGE_NAME))
            .isEqualTo(
                state.matchingApps
                    .first { it.metadata.packageName == TEST_APP_PACKAGE_NAME }
                    .permissions
            )
        assertThat(viewModel.grantedPermissions.value?.get(TEST_APP_PACKAGE_NAME_2))
            .isEqualTo(
                state.matchingApps
                    .first { it.metadata.packageName == TEST_APP_PACKAGE_NAME_2 }
                    .permissions
            )
        assertThat(viewModel.enabledDevicePackages.value).contains(TEST_WATCH_DEVICE_PACKAGE_NAME)
        assertThat(viewModel.atLeastOnePermissionGranted.value).isTrue()
        assertThat(viewModel.allPermissionsGranted.value).isTrue()
    }

    @Test
    @DisableFlags(
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_UI_MATCHMAKING_SCREEN,
    )
    fun addAllPermissionsToGrantedList_withoutDdpApis_addsAllPermissionsToGrantedPermissions() =
        runTest {
            setupWithData()

            viewModel.addAllPermissionsToGrantedList(TEST_APP_PACKAGE_NAME)
            viewModel.addAllPermissionsToGrantedList(TEST_APP_PACKAGE_NAME_2)

            val state = viewModel.matchmakingState.value as WithData
            val gran = viewModel.grantedPermissions
            assertThat(gran.value?.get(TEST_APP_PACKAGE_NAME))
                .isEqualTo(
                    state.matchingApps
                        .first { it.metadata.packageName == TEST_APP_PACKAGE_NAME }
                        .permissions
                )
            assertThat(viewModel.grantedPermissions.value?.get(TEST_APP_PACKAGE_NAME_2))
                .isEqualTo(
                    state.matchingApps
                        .first { it.metadata.packageName == TEST_APP_PACKAGE_NAME_2 }
                        .permissions
                )
            assertThat(viewModel.atLeastOnePermissionGranted.value).isTrue()
            assertThat(viewModel.allPermissionsGranted.value).isTrue()
        }

    @Test
    fun removeAllPermissionsFromGrantedList_clearsGrantedPermissions() = runTest {
        setupWithData()
        viewModel.addAllPermissionsToGrantedList(TEST_APP_PACKAGE_NAME)
        viewModel.addAllPermissionsToGrantedList(TEST_APP_PACKAGE_NAME_2)
        viewModel.addDevicePermissionToGrantedList(TEST_WATCH_DEVICE_PACKAGE_NAME)

        viewModel.removeAllPermissionsFromGrantedList(TEST_APP_PACKAGE_NAME)
        viewModel.removeAllPermissionsFromGrantedList(TEST_APP_PACKAGE_NAME_2)
        viewModel.removeAllPermissionsFromGrantedList(TEST_WATCH_DEVICE_PACKAGE_NAME)

        assertThat(viewModel.grantedPermissions.value).isEmpty()
        assertThat(viewModel.enabledDevicePackages.value).isEmpty()
        assertThat(viewModel.atLeastOnePermissionGranted.value).isFalse()
        assertThat(viewModel.allPermissionsGranted.value).isFalse()
    }

    @Test
    fun grantPermissions_grantsAllPermissions() = runTest {
        setupWithData()

        viewModel.addAllPermissionsToGrantedList(TEST_APP_PACKAGE_NAME)
        viewModel.addAllPermissionsToGrantedList(TEST_APP_PACKAGE_NAME_2)
        viewModel.addDevicePermissionToGrantedList(TEST_WATCH_DEVICE_PACKAGE_NAME)

        viewModel.grantPermissions()

        verify(healthPermissionManager, times(2)).grantHealthPermission(any(), any())
        // TODO(b/325752113): Verify DDP enabling logic once implemented.
    }

    @Test
    fun grantPermissions_grantsPartialPermissions() = runTest {
        setupWithData()
        val writeStepsPermission =
            HealthPermission.fromPermissionString(WRITE_STEPS) as HealthPermission.FitnessPermission
        viewModel.addAppPermissionToGrantedList(TEST_APP_PACKAGE_NAME, writeStepsPermission)

        viewModel.grantPermissions()

        verify(healthPermissionManager).grantHealthPermission(TEST_APP_PACKAGE_NAME, WRITE_STEPS)
        verify(healthPermissionManager, never())
            .grantHealthPermission(TEST_APP_PACKAGE_NAME_2, WRITE_EXERCISE)
    }

    @Test
    @DisableFlags(
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_UI_MATCHMAKING_SCREEN,
    )
    fun grantPermissions_withoutDdpApis_withNoPermissionsGranted_recordsDenialForAppsOnly() =
        runTest {
            setupWithData()
            val captor = argumentCaptor<RecordMatchmakingDenialInput>()

            viewModel.grantPermissions()

            verify(recordMatchmakingDenialUseCase).invoke(captor.capture())
            assertThat(captor.firstValue.callingPackageName).isEqualTo(TEST_APP_PACKAGE_NAME_3)
            val expectedDenied =
                mapOf(
                    TEST_APP_PACKAGE_NAME to listOf(WRITE_STEPS),
                    TEST_APP_PACKAGE_NAME_2 to listOf(WRITE_EXERCISE),
                )
            assertThat(captor.firstValue.deniedDataSources).isEqualTo(expectedDenied)
        }

    @Test
    @DisableFlags(
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_UI_MATCHMAKING_SCREEN,
    )
    fun grantPermissions_withoutDdpApis_allFromOneAppAndSomeFromAnother_recordsDenialCorrectly() =
        runTest {
            setupWithDataForGrantPermissions()
            val writeStepsPermission =
                HealthPermission.fromPermissionString(WRITE_STEPS)
                    as HealthPermission.FitnessPermission
            val writeExercisePermission =
                HealthPermission.fromPermissionString(WRITE_EXERCISE)
                    as HealthPermission.FitnessPermission
            viewModel.addAppPermissionToGrantedList(TEST_APP_PACKAGE_NAME, writeStepsPermission)
            viewModel.addAppPermissionToGrantedList(
                TEST_APP_PACKAGE_NAME_2,
                writeExercisePermission,
            )
            val captor = argumentCaptor<RecordMatchmakingDenialInput>()

            viewModel.grantPermissions()

            verify(recordMatchmakingDenialUseCase).invoke(captor.capture())
            assertThat(captor.firstValue.callingPackageName).isEqualTo(TEST_APP_PACKAGE_NAME_3)
            val expectedDenied = mapOf(TEST_APP_PACKAGE_NAME_2 to listOf(WRITE_SLEEP))
            assertThat(captor.firstValue.deniedDataSources).isEqualTo(expectedDenied)
        }

    @Test
    fun grantPermissions_withAllPermissionsGranted_doesNotRecordDenial() = runTest {
        setupWithData()
        viewModel.addAllPermissionsToGrantedList(TEST_APP_PACKAGE_NAME)
        viewModel.addAllPermissionsToGrantedList(TEST_APP_PACKAGE_NAME_2)
        viewModel.addDevicePermissionToGrantedList("com.example.watchdevice")

        viewModel.grantPermissions()

        verify(recordMatchmakingDenialUseCase, never()).invoke(any())
    }

    @Test
    fun recordMatchmakingDenial_notWithData_doesNothing() = runTest {
        viewModel.recordMatchmakingDenial()

        verify(recordMatchmakingDenialUseCase, never()).invoke(any())
    }

    @Test
    fun recordMatchmakingDenial_noMatchingDataSources_doesNothing() = runTest {
        stubGetMatchingDataSourcesUseCase(testMatchingDevices = emptyList())

        viewModel.loadMatchmakingData(
            TEST_APP_PACKAGE_NAME_3,
            arrayOf(StepsRecord::class.java.name),
        )

        viewModel.recordMatchmakingDenial()

        verify(recordMatchmakingDenialUseCase, never()).invoke(any())
    }

    @Test
    fun recordMatchmakingDenial_noPermissions_doesNothing() = runTest {
        val appMetadata = AppMetadata(TEST_APP_PACKAGE_NAME, TEST_APP_NAME, null)
        val appMetadata2 = AppMetadata(TEST_APP_PACKAGE_NAME_2, TEST_APP_NAME_2, null)
        val appsWithNoPermissions =
            listOf(
                MatchmakingAppData(appMetadata, emptyList()),
                MatchmakingAppData(appMetadata2, emptyList()),
            )
        stubGetMatchingDataSourcesUseCase(appsWithNoPermissions, emptyList())
        viewModel.loadMatchmakingData(
            TEST_APP_PACKAGE_NAME_3,
            arrayOf(StepsRecord::class.java.name),
        )

        viewModel.recordMatchmakingDenial()

        verify(recordMatchmakingDenialUseCase, never()).invoke(any())
    }

    @Test
    @DisableFlags(
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_UI_MATCHMAKING_SCREEN,
    )
    fun recordMatchmakingDenial_withoutDdpApis_callsUseCaseWithCorrectParameters() = runTest {
        setupWithDataForGrantPermissions()
        val captor = argumentCaptor<RecordMatchmakingDenialInput>()

        viewModel.grantPermissions()

        verify(recordMatchmakingDenialUseCase).invoke(captor.capture())
        assertThat(captor.firstValue.callingPackageName).isEqualTo(TEST_APP_PACKAGE_NAME_3)
        val expectedDenied =
            mapOf(
                TEST_APP_PACKAGE_NAME to listOf(WRITE_STEPS),
                TEST_APP_PACKAGE_NAME_2 to listOf(WRITE_EXERCISE, WRITE_SLEEP),
            )
        assertThat(captor.firstValue.deniedDataSources).isEqualTo(expectedDenied)
    }

    @Test
    @DisableFlags(
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_UI_MATCHMAKING_SCREEN,
    )
    fun recordMatchmakingDenial_withoutDdpApis_grantedListNonEmpty_callsUseCaseWithCorrectPermissions() =
        runTest {
            setupWithDataForGrantPermissions()
            val permission =
                HealthPermission.fromPermissionString(WRITE_STEPS)
                    as HealthPermission.FitnessPermission
            viewModel.addAppPermissionToGrantedList(TEST_APP_PACKAGE_NAME, permission)

            viewModel.recordMatchmakingDenial()

            val expectedDenied =
                mapOf(TEST_APP_PACKAGE_NAME_2 to listOf(WRITE_EXERCISE, WRITE_SLEEP))
            verify(recordMatchmakingDenialUseCase)
                .invoke(RecordMatchmakingDenialInput(TEST_APP_PACKAGE_NAME_3, expectedDenied))
        }

    @Test
    fun grantPermissions_withMultipleDevices_postsIntentsToQueue() = runTest {
        setupWithData()
        viewModel.addDevicePermissionToGrantedList(TEST_WATCH_DEVICE_PACKAGE_NAME)

        viewModel.grantPermissions()

        assertThat(viewModel.ddpIntentQueue.value).hasSize(1)
        assertThat(viewModel.ddpOnboardingState.value)
            .isInstanceOf(MatchmakingViewModel.DdpOnboardingState.Onboarding::class.java)
    }

    @Test
    @EnableFlags(
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_UI_MATCHMAKING_SCREEN,
    )
    fun grantPermissions_withNoPermissionsGranted_recordsDenialForAllAppsAndDevices() = runTest {
        setupWithData()
        val captor = argumentCaptor<RecordMatchmakingDenialInput>()

        viewModel.grantPermissions()

        verify(recordMatchmakingDenialUseCase).invoke(captor.capture())
        assertThat(captor.firstValue.callingPackageName).isEqualTo(TEST_APP_PACKAGE_NAME_3)

        val deniedSources = captor.firstValue.deniedDataSources
        // Verify apps are denied
        assertThat(deniedSources).containsKey(TEST_APP_PACKAGE_NAME)
        assertThat(deniedSources).containsKey(TEST_APP_PACKAGE_NAME_2)
        // Verify devices are denied
        assertThat(deniedSources).containsKey("com.example.watchdevice")
    }

    @Test
    @EnableFlags(
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_UI_MATCHMAKING_SCREEN,
    )
    fun recordMatchmakingDenial_withNoSelections_recordsDenialForAllAppsAndDevices() = runTest {
        setupWithData()
        val captor = argumentCaptor<RecordMatchmakingDenialInput>()

        viewModel.recordMatchmakingDenial()

        verify(recordMatchmakingDenialUseCase).invoke(captor.capture())
        val deniedSources = captor.firstValue.deniedDataSources
        assertThat(deniedSources).containsKey(TEST_APP_PACKAGE_NAME)
        assertThat(deniedSources).containsKey(TEST_APP_PACKAGE_NAME_2)
        assertThat(deniedSources).containsKey("com.example.watchdevice")
    }

    @Test
    fun onDdpIntentFinished_withResultAllowed_setsAtLeastOneGrantSucceededAndPostsNextIntent() =
        runTest {
            setupWithData()
            viewModel.addDevicePermissionToGrantedList("com.example.watchdevice")
            viewModel.grantPermissions()

            viewModel.onDdpIntentFinished(RESULT_DEVICE_ONBOARDING_ALLOWED)

            assertThat(viewModel.ddpIntentQueue.value).isEmpty()
            assertThat(viewModel.ddpOnboardingState.value)
                .isEqualTo(
                    MatchmakingViewModel.DdpOnboardingState.Finished(android.app.Activity.RESULT_OK)
                )
        }

    @Test
    fun onDdpIntentFinished_withResultAborted_recordsDenialAndFinishesWithPreviousResult() =
        runTest {
            setupWithData()
            viewModel.addDevicePermissionToGrantedList("com.example.watchdevice")
            viewModel.grantPermissions()

            // Reset mock to ignore the denial recorded for apps during grantPermissions
            reset(recordMatchmakingDenialUseCase)

            viewModel.onDdpIntentFinished(RESULT_DEVICE_ONBOARDING_ABORTED)

            // Verify denial recorded for the aborted device
            verify(recordMatchmakingDenialUseCase, times(1)).invoke(any())
            assertThat(viewModel.ddpIntentQueue.value).isEmpty()
            // In this case, app grants weren't done, so it should be RESULT_CANCELED
            assertThat(viewModel.ddpOnboardingState.value)
                .isEqualTo(
                    MatchmakingViewModel.DdpOnboardingState.Finished(
                        android.app.Activity.RESULT_CANCELED
                    )
                )
        }

    @Test
    fun onDdpIntentFinished_withResultDenied_recordsDenialAndPostsNextIntent() = runTest {
        setupWithData()
        viewModel.addDevicePermissionToGrantedList("com.example.watchdevice")
        viewModel.grantPermissions()

        reset(recordMatchmakingDenialUseCase)

        viewModel.onDdpIntentFinished(RESULT_DEVICE_ONBOARDING_DENIED)

        // Verify denial recorded for the denied device
        verify(recordMatchmakingDenialUseCase, times(1)).invoke(any())
        assertThat(viewModel.ddpIntentQueue.value).isEmpty()
        assertThat(viewModel.ddpOnboardingState.value)
            .isEqualTo(
                MatchmakingViewModel.DdpOnboardingState.Finished(
                    android.app.Activity.RESULT_CANCELED
                )
            )
    }

    @Test
    fun onDdpIntentFinished_withResultAborted_withPreviousGrants_returnsResultOk() = runTest {
        setupWithData()
        // Grant all app permissions so no app denial is recorded during grantPermissions
        viewModel.addAllPermissionsToGrantedList(TEST_APP_PACKAGE_NAME)
        viewModel.addAllPermissionsToGrantedList(TEST_APP_PACKAGE_NAME_2)
        viewModel.addDevicePermissionToGrantedList("com.example.watchdevice")
        viewModel.grantPermissions()

        reset(recordMatchmakingDenialUseCase)

        viewModel.onDdpIntentFinished(RESULT_DEVICE_ONBOARDING_ABORTED)

        // Verify denial recorded for the aborted device
        verify(recordMatchmakingDenialUseCase, times(1)).invoke(any())
        assertThat(viewModel.ddpOnboardingState.value)
            .isEqualTo(
                MatchmakingViewModel.DdpOnboardingState.Finished(android.app.Activity.RESULT_OK)
            )
    }

    @Test
    fun onDdpIntentFinished_withResultCanceled_withPreviousGrants_returnsResultOk() = runTest {
        setupWithData()
        // Grant an app permission to set _atLeastOneGrantSucceeded to true
        viewModel.addAllPermissionsToGrantedList(TEST_APP_PACKAGE_NAME)
        viewModel.addDevicePermissionToGrantedList(TEST_WATCH_DEVICE_PACKAGE_NAME)
        viewModel.grantPermissions()

        viewModel.onDdpIntentFinished(android.app.Activity.RESULT_CANCELED)

        assertThat(viewModel.ddpIntentQueue.value).isEmpty()
        // Should be OK because we granted app permissions before starting DDP flow
        assertThat(viewModel.ddpOnboardingState.value)
            .isEqualTo(
                MatchmakingViewModel.DdpOnboardingState.Finished(android.app.Activity.RESULT_OK)
            )
    }

    @Test
    fun onDdpIntentFinished_withResultCanceled_noPreviousGrants_returnsResultCanceled() = runTest {
        // Setup with zero matched apps but with a device
        val packageName = TEST_APP_PACKAGE_NAME_3
        val recordTypeNames = arrayOf(StepsRecord::class.java.name)
        stubGetMatchingDataSourcesUseCase(emptyList(), getExpectedDevices())
        viewModel.loadMatchmakingData(packageName, recordTypeNames)

        // Select device and "Allow" (triggers DDP flow but no app grants)
        viewModel.addDevicePermissionToGrantedList(TEST_WATCH_DEVICE_PACKAGE_NAME)
        viewModel.grantPermissions()

        // Simulate DDP cancellation
        viewModel.onDdpIntentFinished(android.app.Activity.RESULT_CANCELED)

        assertThat(viewModel.ddpOnboardingState.value)
            .isEqualTo(
                MatchmakingViewModel.DdpOnboardingState.Finished(
                    android.app.Activity.RESULT_CANCELED
                )
            )
    }

    @Test
    fun onDdpIntentFinished_withResultOkAndEmptyQueue_postsNullAndOkToFinishedEvent() = runTest {
        setupWithData()
        viewModel.addDevicePermissionToGrantedList(TEST_WATCH_DEVICE_PACKAGE_NAME)
        viewModel.grantPermissions()

        viewModel.onDdpIntentFinished(android.app.Activity.RESULT_OK)

        assertThat(viewModel.ddpIntentQueue.value).isEmpty()
        assertThat(viewModel.ddpOnboardingState.value)
            .isEqualTo(
                MatchmakingViewModel.DdpOnboardingState.Finished(android.app.Activity.RESULT_OK)
            )
    }

    @Test
    fun grantPermissions_deviceSelected_postsOkToFinishedEvent() = runTest {
        viewModel.addDevicePermissionToGrantedList(TEST_WATCH_DEVICE_PACKAGE_NAME)

        viewModel.grantPermissions()

        assertThat(viewModel.ddpOnboardingState.value)
            .isEqualTo(
                MatchmakingViewModel.DdpOnboardingState.Finished(android.app.Activity.RESULT_OK)
            )
    }

    @Test
    @EnableFlags(
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_UI_MATCHMAKING_SCREEN,
    )
    fun onDdpIntentFinished_withMultipleIntents_progressesThroughQueueCorrectly() = runTest {
        // Setup with 2 providers for one device
        val packageName = TEST_APP_PACKAGE_NAME_3
        val devices = getExpectedDevicesWithMultipleProviders()
        stubGetMatchingDataSourcesUseCase(emptyList(), devices)
        viewModel.loadMatchmakingData(packageName, arrayOf(StepsRecord::class.java.name))

        viewModel.addDevicePermissionToGrantedList("com.example.watchdevice")
        viewModel.grantPermissions()

        assertThat(viewModel.ddpIntentQueue.value).hasSize(2)
        val firstState =
            viewModel.ddpOnboardingState.value as MatchmakingViewModel.DdpOnboardingState.Onboarding
        assertThat(firstState.intent.`package`).isEqualTo("com.google.android.apps.fitness")

        // Finish first intent
        viewModel.onDdpIntentFinished(android.app.Activity.RESULT_OK)

        assertThat(viewModel.ddpIntentQueue.value).hasSize(1)
        val secondState =
            viewModel.ddpOnboardingState.value as MatchmakingViewModel.DdpOnboardingState.Onboarding
        assertThat(secondState.intent.`package`).isEqualTo("com.google.android.apps.fitness2")

        // Finish second intent
        viewModel.onDdpIntentFinished(android.app.Activity.RESULT_OK)

        assertThat(viewModel.ddpIntentQueue.value).isEmpty()
        assertThat(viewModel.ddpOnboardingState.value)
            .isEqualTo(
                MatchmakingViewModel.DdpOnboardingState.Finished(android.app.Activity.RESULT_OK)
            )
    }

    @Test
    fun consumeDdpOnboardingEvent_resetsStateToSetup() = runTest {
        setupWithData()
        viewModel.addDevicePermissionToGrantedList(TEST_WATCH_DEVICE_PACKAGE_NAME)
        viewModel.grantPermissions()

        assertThat(viewModel.ddpOnboardingState.value)
            .isInstanceOf(MatchmakingViewModel.DdpOnboardingState.Onboarding::class.java)

        viewModel.consumeDdpOnboardingEvent()

        assertThat(viewModel.ddpOnboardingState.value)
            .isEqualTo(MatchmakingViewModel.DdpOnboardingState.Setup)
    }

    private fun getExpectedDevicesWithMultipleProviders(): List<MatchmakingDeviceData> {
        return listOf(
            MatchmakingDeviceData(
                DeviceDataSourceInfo(
                    DataOrigin.Builder().setPackageName("com.example.watchdevice").build(),
                    android.health.connect.datatypes.Device.Builder()
                        .setManufacturer("Google")
                        .setModel("Pixel Watch")
                        .setType(2)
                        .build(),
                    false,
                    listOf(
                        DeviceDataProviderInfo(
                            "com.google.android.apps.fitness",
                            "MyFit",
                            "",
                            "",
                            emptySet(),
                        ),
                        DeviceDataProviderInfo(
                            "com.google.android.apps.fitness2",
                            "MyFit2",
                            "",
                            "",
                            emptySet(),
                        ),
                    ),
                ),
                emptyList(),
            )
        )
    }

    private suspend fun setupWithData() {
        val packageName = TEST_APP_PACKAGE_NAME_3
        val recordTypeNames =
            arrayOf(ExerciseSessionRecord::class.java.name, StepsRecord::class.java.name)
        val appMetadata = AppMetadata(TEST_APP_PACKAGE_NAME, TEST_APP_NAME, null)
        val appMetadata2 = AppMetadata(TEST_APP_PACKAGE_NAME_2, TEST_APP_NAME_2, null)
        val permissions =
            listOf(
                HealthPermission.fromPermissionString(WRITE_STEPS)
                    as HealthPermission.FitnessPermission
            )
        val permissions2 =
            listOf(
                HealthPermission.fromPermissionString(WRITE_EXERCISE)
                    as HealthPermission.FitnessPermission
            )
        val expectedApps =
            listOf(
                MatchmakingAppData(appMetadata, permissions),
                MatchmakingAppData(appMetadata2, permissions2),
            )
        if (Flags.deviceDataProvidersUiMatchmakingScreen()) {
            stubGetMatchingDataSourcesUseCase(expectedApps, getExpectedDevices())
        } else {
            stubGetMatchingDataSourcesUseCase(expectedApps, emptyList<MatchmakingDeviceData>())
        }
        viewModel.loadMatchmakingData(packageName, recordTypeNames)
    }

    private suspend fun setupWithDataForGrantPermissions() {
        val packageName = TEST_APP_PACKAGE_NAME_3
        val recordTypeNames =
            arrayOf(ExerciseSessionRecord::class.java.name, StepsRecord::class.java.name)
        val appMetadata = AppMetadata(TEST_APP_PACKAGE_NAME, TEST_APP_NAME, null)
        val appMetadata2 = AppMetadata(TEST_APP_PACKAGE_NAME_2, TEST_APP_NAME_2, null)
        val permissions =
            listOf(
                HealthPermission.fromPermissionString(WRITE_STEPS)
                    as HealthPermission.FitnessPermission
            )
        val permissions2 =
            listOf(
                HealthPermission.fromPermissionString(WRITE_EXERCISE)
                    as HealthPermission.FitnessPermission,
                HealthPermission.fromPermissionString(WRITE_SLEEP)
                    as HealthPermission.FitnessPermission,
            )
        val expectedApps =
            listOf(
                MatchmakingAppData(appMetadata, permissions),
                MatchmakingAppData(appMetadata2, permissions2),
            )
        if (Flags.deviceDataProvidersUiMatchmakingScreen()) {
            stubGetMatchingDataSourcesUseCase(expectedApps, getExpectedDevices())
        } else {
            stubGetMatchingDataSourcesUseCase(expectedApps, emptyList<MatchmakingDeviceData>())
        }
        viewModel.loadMatchmakingData(packageName, recordTypeNames)
    }
}
