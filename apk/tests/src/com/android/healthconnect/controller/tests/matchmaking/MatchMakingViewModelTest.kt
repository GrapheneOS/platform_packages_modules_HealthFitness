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

import android.health.connect.HealthPermissions.WRITE_EXERCISE
import android.health.connect.HealthPermissions.WRITE_SLEEP
import android.health.connect.HealthPermissions.WRITE_STEPS
import android.health.connect.datatypes.ExerciseSessionRecord
import android.health.connect.datatypes.StepsRecord
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.matchmaking.MatchmakingAppData
import com.android.healthconnect.controller.matchmaking.MatchmakingViewModel
import com.android.healthconnect.controller.matchmaking.MatchmakingViewModel.MatchmakingState.LoadingFailed
import com.android.healthconnect.controller.matchmaking.MatchmakingViewModel.MatchmakingState.WithData
import com.android.healthconnect.controller.matchmaking.api.GetMatchingAppsUseCase
import com.android.healthconnect.controller.matchmaking.api.RecordMatchmakingDenialUseCase
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
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@HiltAndroidTest
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class MatchMakingViewModelTest {

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()
    @BindValue lateinit var appInfoReader: AppInfoReader

    private val getMatchingAppsUseCase: GetMatchingAppsUseCase = mock()
    private val recordMatchmakingDenialUseCase: RecordMatchmakingDenialUseCase = mock()
    private val healthPermissionManager: HealthPermissionManager = mock()

    private lateinit var viewModel: MatchmakingViewModel

    @Before
    fun setup() = runTest {
        Dispatchers.setMain(Dispatchers.Unconfined)
        appInfoReader = createFakeAppInfoReader()
        viewModel =
            MatchmakingViewModel(
                getMatchingAppsUseCase,
                recordMatchmakingDenialUseCase,
                appInfoReader,
                SavedStateHandle(),
                healthPermissionManager,
            )
    }

    @Test
    fun loadMatchmakingApps_withSuccess_updatesStateToWithData() = runTest {
        val packageName = TEST_APP_PACKAGE_NAME
        val recordTypes = setOf(StepsRecord::class.java)
        val appMetadata = AppMetadata(TEST_APP_NAME_2, TEST_APP_PACKAGE_NAME_2, null)
        val expected =
            listOf(
                MatchmakingAppData(
                    appMetadata,
                    listOf(
                        HealthPermission.fromPermissionString(WRITE_STEPS)
                            as HealthPermission.FitnessPermission
                    ),
                )
            )
        val useCaseResult = UseCaseResults.Success(expected)
        whenever(getMatchingAppsUseCase.invoke(any())).doReturn(useCaseResult)

        viewModel.loadMatchmakingApps(packageName, recordTypes)

        val state = viewModel.matchmakingState.value
        assertThat(state).isInstanceOf(WithData::class.java)
        assertThat((state as WithData).matchingApps).isEqualTo(expected)
    }

    @Test
    fun loadMatchmakingApps_withError_updatesStateToLoadingFailed() = runTest {
        val packageName = TEST_APP_PACKAGE_NAME
        val recordTypes = setOf(StepsRecord::class.java)
        val exception = IllegalStateException("Error")
        val useCaseResult = UseCaseResults.Failed(exception)
        whenever(getMatchingAppsUseCase.invoke(any())).doReturn(useCaseResult)

        viewModel.loadMatchmakingApps(packageName, recordTypes)

        assertThat(viewModel.matchmakingState.value).isInstanceOf(LoadingFailed::class.java)
    }

    @Test
    fun loadMatchmakingApps_returnsSortedAppsAndPermissions() = runTest {
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
        whenever(getMatchingAppsUseCase.invoke(any()))
            .thenReturn(UseCaseResults.Success(listOf(appB, appA)))
        whenever(appInfoReader.getAppMetadata(TEST_APP_PACKAGE_NAME))
            .thenReturn(AppMetadata(TEST_APP_PACKAGE_NAME, TEST_APP_NAME, null))

        viewModel.loadMatchmakingApps(TEST_APP_PACKAGE_NAME, setOf(StepsRecord::class.java))

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

        viewModel.addPermissionToGrantedList(TEST_APP_PACKAGE_NAME, permission)

        assertThat(viewModel.grantedPermissions.value?.get(TEST_APP_PACKAGE_NAME))
            .contains(permission)
        assertThat(viewModel.atLeastOnePermissionGranted.value).isTrue()
    }

    @Test
    fun removePermissionFromGrantedList_removesPermissionFromGrantedPermissions() {
        val permission =
            HealthPermission.fromPermissionString(WRITE_STEPS) as HealthPermission.FitnessPermission
        viewModel.addPermissionToGrantedList(TEST_APP_PACKAGE_NAME, permission)

        viewModel.removePermissionFromGrantedList(TEST_APP_PACKAGE_NAME, permission)

        assertThat(viewModel.grantedPermissions.value?.get(TEST_APP_PACKAGE_NAME)).isNull()
        assertThat(viewModel.atLeastOnePermissionGranted.value).isFalse()
    }

    @Test
    fun addAllPermissionsToGrantedList_addsAllPermissionsToGrantedPermissions() = runTest {
        setupWithData()

        viewModel.addAllPermissionsToGrantedList()

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
        viewModel.addAllPermissionsToGrantedList()

        viewModel.removeAllPermissionsFromGrantedList()

        assertThat(viewModel.grantedPermissions.value).isEmpty()
        assertThat(viewModel.atLeastOnePermissionGranted.value).isFalse()
        assertThat(viewModel.allPermissionsGranted.value).isFalse()
    }

    @Test
    fun grantPermissions_grantsAllPermissions() = runTest {
        setupWithData()

        viewModel.addAllPermissionsToGrantedList()
        viewModel.grantPermissions()

        verify(healthPermissionManager, times(2)).grantHealthPermission(any(), any())
    }

    @Test
    fun grantPermissions_grantsPartialPermissions() = runTest {
        setupWithData()
        val writeStepsPermission =
            HealthPermission.fromPermissionString(WRITE_STEPS) as HealthPermission.FitnessPermission
        viewModel.addPermissionToGrantedList(TEST_APP_PACKAGE_NAME, writeStepsPermission)

        viewModel.grantPermissions()

        verify(healthPermissionManager).grantHealthPermission(TEST_APP_PACKAGE_NAME, WRITE_STEPS)
        verify(healthPermissionManager, never())
            .grantHealthPermission(TEST_APP_PACKAGE_NAME_2, WRITE_EXERCISE)
    }

    @Test
    fun grantPermissions_withNoPermissionsGranted_recordsDenialForAllPermissions() = runTest {
        setupWithData()
        val captor = argumentCaptor<RecordMatchmakingDenialUseCase.RecordMatchmakingDenialInput>()

        viewModel.grantPermissions()

        verify(recordMatchmakingDenialUseCase).invoke(captor.capture())
        assertThat(captor.firstValue.callingPackageName).isEqualTo(TEST_APP_PACKAGE_NAME_3)
        assertThat(captor.firstValue.deniedApps)
            .containsExactly(
                TEST_APP_PACKAGE_NAME,
                listOf(WRITE_STEPS),
                TEST_APP_PACKAGE_NAME_2,
                listOf(WRITE_EXERCISE),
            )
    }

    @Test
    fun grantPermissions_allFromOneAppAndSomeFromAnother_recordsDenialCorrectly() = runTest {
        val packageName = TEST_APP_PACKAGE_NAME_3
        val recordTypes = setOf(ExerciseSessionRecord::class.java, StepsRecord::class.java)
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
        val expected =
            listOf(
                MatchmakingAppData(appMetadata, permissions),
                MatchmakingAppData(appMetadata2, permissions2),
            )
        val useCaseResult = UseCaseResults.Success(expected)
        whenever(getMatchingAppsUseCase.invoke(any())).doReturn(useCaseResult)
        viewModel.loadMatchmakingApps(packageName, recordTypes)

        val writeStepsPermission =
            HealthPermission.fromPermissionString(WRITE_STEPS) as HealthPermission.FitnessPermission
        val writeExercisePermission =
            HealthPermission.fromPermissionString(WRITE_EXERCISE)
                as HealthPermission.FitnessPermission
        viewModel.addPermissionToGrantedList(TEST_APP_PACKAGE_NAME, writeStepsPermission)
        viewModel.addPermissionToGrantedList(TEST_APP_PACKAGE_NAME_2, writeExercisePermission)
        val captor = argumentCaptor<RecordMatchmakingDenialUseCase.RecordMatchmakingDenialInput>()

        viewModel.grantPermissions()

        verify(recordMatchmakingDenialUseCase).invoke(captor.capture())
        assertThat(captor.firstValue.callingPackageName).isEqualTo(TEST_APP_PACKAGE_NAME_3)
        assertThat(captor.firstValue.deniedApps)
            .containsExactly(TEST_APP_PACKAGE_NAME_2, listOf(WRITE_SLEEP))
    }

    @Test
    fun grantPermissions_withAllPermissionsGranted_doesNotRecordDenial() = runTest {
        setupWithData()
        viewModel.addAllPermissionsToGrantedList()

        viewModel.grantPermissions()

        verify(recordMatchmakingDenialUseCase, never()).invoke(any())
    }

    @Test
    fun recordMatchmakingDenial_notWithData_doesNothing() = runTest {
        viewModel.recordMatchmakingDenial()

        verify(recordMatchmakingDenialUseCase, never()).invoke(any())
    }

    @Test
    fun recordMatchmakingDenial_noMatchingApps_doesNothing() = runTest {
        whenever(getMatchingAppsUseCase.invoke(any()))
            .thenReturn(UseCaseResults.Success(emptyList()))
        viewModel.loadMatchmakingApps(TEST_APP_PACKAGE_NAME_3, setOf(StepsRecord::class.java))

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
        whenever(getMatchingAppsUseCase.invoke(any()))
            .thenReturn(UseCaseResults.Success(appsWithNoPermissions))
        viewModel.loadMatchmakingApps(TEST_APP_PACKAGE_NAME_3, setOf(StepsRecord::class.java))

        viewModel.recordMatchmakingDenial()

        verify(recordMatchmakingDenialUseCase, never()).invoke(any())
    }

    @Test
    fun recordMatchmakingDenial_callsUseCaseWithCorrectParameters() = runTest {
        setupWithData()
        val captor = argumentCaptor<RecordMatchmakingDenialUseCase.RecordMatchmakingDenialInput>()
        val callingPackageName = TEST_APP_PACKAGE_NAME_3
        val deniedApps =
            mapOf(
                TEST_APP_PACKAGE_NAME to listOf(WRITE_STEPS),
                TEST_APP_PACKAGE_NAME_2 to listOf(WRITE_EXERCISE),
            )

        viewModel.recordMatchmakingDenial()

        verify(recordMatchmakingDenialUseCase).invoke(captor.capture())
        assertThat(captor.firstValue.callingPackageName).isEqualTo(callingPackageName)
        assertThat(captor.firstValue.deniedApps).isEqualTo(deniedApps)
    }

    @Test
    fun recordMatchmakingDenial_grantedListNonEmpty_callsUseCaseForAllPermissions() = runTest {
        setupWithData()
        val permission =
            HealthPermission.fromPermissionString(WRITE_STEPS) as HealthPermission.FitnessPermission
        viewModel.addPermissionToGrantedList(TEST_APP_PACKAGE_NAME, permission)
        val captor = argumentCaptor<RecordMatchmakingDenialUseCase.RecordMatchmakingDenialInput>()
        val callingPackageName = TEST_APP_PACKAGE_NAME_3
        val deniedApps =
            mapOf(
                TEST_APP_PACKAGE_NAME to listOf(WRITE_STEPS),
                TEST_APP_PACKAGE_NAME_2 to listOf(WRITE_EXERCISE),
            )

        viewModel.recordMatchmakingDenial()

        verify(recordMatchmakingDenialUseCase).invoke(captor.capture())
        assertThat(captor.firstValue.callingPackageName).isEqualTo(callingPackageName)
        assertThat(captor.firstValue.deniedApps).isEqualTo(deniedApps)
    }

    private suspend fun setupWithData() {
        val packageName = TEST_APP_PACKAGE_NAME_3
        val recordTypes = setOf(ExerciseSessionRecord::class.java, StepsRecord::class.java)
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
        val expected =
            listOf(
                MatchmakingAppData(appMetadata, permissions),
                MatchmakingAppData(appMetadata2, permissions2),
            )
        val useCaseResult = UseCaseResults.Success(expected)
        whenever(getMatchingAppsUseCase.invoke(any())).doReturn(useCaseResult)
        viewModel.loadMatchmakingApps(packageName, recordTypes)
    }
}
