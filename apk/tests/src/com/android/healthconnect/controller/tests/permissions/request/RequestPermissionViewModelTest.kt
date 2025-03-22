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
 */

package com.android.healthconnect.controller.tests.permissions.request

import android.content.Context
import android.content.pm.PackageManager
import android.health.connect.HealthPermissions.READ_EXERCISE
import android.health.connect.HealthPermissions.READ_EXERCISE_ROUTES
import android.health.connect.HealthPermissions.READ_HEALTH_DATA_HISTORY
import android.health.connect.HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND
import android.health.connect.HealthPermissions.READ_HEART_RATE
import android.health.connect.HealthPermissions.READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES
import android.health.connect.HealthPermissions.READ_MEDICAL_DATA_VACCINES
import android.health.connect.HealthPermissions.READ_SKIN_TEMPERATURE
import android.health.connect.HealthPermissions.READ_SLEEP
import android.health.connect.HealthPermissions.READ_STEPS
import android.health.connect.HealthPermissions.WRITE_DISTANCE
import android.health.connect.HealthPermissions.WRITE_EXERCISE
import android.health.connect.HealthPermissions.WRITE_MEDICAL_DATA
import android.health.connect.HealthPermissions.WRITE_PLANNED_EXERCISE
import android.health.connect.HealthPermissions.WRITE_SKIN_TEMPERATURE
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import com.android.healthconnect.controller.permissions.additionalaccess.LoadDeclaredHealthPermissionUseCase
import com.android.healthconnect.controller.permissions.api.GetGrantedHealthPermissionsUseCase
import com.android.healthconnect.controller.permissions.api.GetHealthPermissionsFlagsUseCase
import com.android.healthconnect.controller.permissions.api.GrantHealthPermissionUseCase
import com.android.healthconnect.controller.permissions.api.HealthPermissionManager
import com.android.healthconnect.controller.permissions.api.LoadAccessDateUseCase
import com.android.healthconnect.controller.permissions.api.RevokeHealthPermissionUseCase
import com.android.healthconnect.controller.permissions.data.HealthPermission.AdditionalPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.Companion.fromPermissionString
import com.android.healthconnect.controller.permissions.data.HealthPermission.FitnessPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.MedicalPermission
import com.android.healthconnect.controller.permissions.data.PermissionState
import com.android.healthconnect.controller.permissions.request.AdditionalScreenState
import com.android.healthconnect.controller.permissions.request.FitnessScreenState
import com.android.healthconnect.controller.permissions.request.MedicalScreenState
import com.android.healthconnect.controller.permissions.request.PermissionsActivityState
import com.android.healthconnect.controller.permissions.request.RequestPermissionViewModel
import com.android.healthconnect.controller.service.HealthPermissionManagerModule
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.shared.app.AppInfoReader
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.tests.utils.InstantTaskExecutorRule
import com.android.healthconnect.controller.tests.utils.NOW
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME_2
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME_2
import com.android.healthconnect.controller.tests.utils.TestObserver
import com.android.healthconnect.controller.tests.utils.di.FakeHealthPermissionManager
import com.android.healthfitness.flags.Flags
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.qualifiers.ApplicationContext
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
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@UninstallModules(HealthPermissionManagerModule::class)
@HiltAndroidTest
class RequestPermissionViewModelTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)
    @get:Rule val setFlagsRule = SetFlagsRule()
    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()
    private val testDispatcher = UnconfinedTestDispatcher()

    @BindValue val permissionManager: HealthPermissionManager = FakeHealthPermissionManager()

    @Inject @ApplicationContext lateinit var context: Context
    @Inject lateinit var appInfoReader: AppInfoReader
    @Inject lateinit var healthPermissionReader: HealthPermissionReader
    @Inject lateinit var grantHealthPermissionUseCase: GrantHealthPermissionUseCase
    @Inject lateinit var revokeHealthPermissionUseCase: RevokeHealthPermissionUseCase
    @Inject lateinit var getGrantHealthPermissionUseCase: GetGrantedHealthPermissionsUseCase
    @Inject lateinit var getHealthPermissionsFlagsUseCase: GetHealthPermissionsFlagsUseCase
    @Inject lateinit var loadDeclaredHealthPermissionUseCase: LoadDeclaredHealthPermissionUseCase
    @BindValue var loadAccessDateUseCase: LoadAccessDateUseCase = mock()

    lateinit var viewModel: RequestPermissionViewModel

    @Before
    fun setup() {
        hiltRule.inject()
        permissionManager.revokeAllHealthPermissions(TEST_APP_PACKAGE_NAME)
        permissionManager.revokeAllHealthPermissions(TEST_APP_PACKAGE_NAME_2)
        Dispatchers.setMain(testDispatcher)
        viewModel =
            RequestPermissionViewModel(
                context,
                appInfoReader,
                healthPermissionReader,
                grantHealthPermissionUseCase,
                revokeHealthPermissionUseCase,
                getGrantHealthPermissionUseCase,
                getHealthPermissionsFlagsUseCase,
                loadAccessDateUseCase,
                loadDeclaredHealthPermissionUseCase,
            )
        whenever(loadAccessDateUseCase.invoke(eq(TEST_APP_PACKAGE_NAME))).thenReturn(NOW)
        whenever(loadAccessDateUseCase.invoke(eq(TEST_APP_PACKAGE_NAME_2))).thenReturn(NOW)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun init_loadsAppInfo() = runTest {
        val permissions = arrayOf(READ_EXERCISE, READ_SLEEP)
        viewModel.init(TEST_APP_PACKAGE_NAME, permissions)

        val testObserver = TestObserver<AppMetadata>()
        viewModel.appMetadata.observeForever(testObserver)
        advanceUntilIdle()
        assertThat(testObserver.getLastValue().appName).isEqualTo(TEST_APP_NAME)
        assertThat(testObserver.getLastValue().packageName).isEqualTo(TEST_APP_PACKAGE_NAME)
    }

    // PermissionScreenStates
    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    fun init_withMedicalReadAndWritePermissions_loadsPermissionActivityScreenStateShowMedical() =
        runTest {
            val permissions =
                arrayOf(
                    READ_MEDICAL_DATA_VACCINES,
                    WRITE_MEDICAL_DATA,
                    READ_EXERCISE,
                    READ_SLEEP,
                    WRITE_EXERCISE,
                    READ_HEALTH_DATA_IN_BACKGROUND,
                    READ_HEALTH_DATA_HISTORY,
                )
            val permissionActivityStateObserver = TestObserver<PermissionsActivityState>()
            viewModel.permissionsActivityState.observeForever(permissionActivityStateObserver)

            viewModel.init(TEST_APP_PACKAGE_NAME, permissions)
            advanceUntilIdle()

            val result = permissionActivityStateObserver.getLastValue()
            assertThat(result is PermissionsActivityState.ShowMedical).isTrue()
            assertThat((result as PermissionsActivityState.ShowMedical).isWriteOnly).isFalse()
        }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    fun init_withMedicalWritePermissions_loadsPermissionActivityScreenStateShowMedical() = runTest {
        val permissions =
            arrayOf(
                WRITE_MEDICAL_DATA,
                READ_EXERCISE,
                READ_SLEEP,
                WRITE_EXERCISE,
                READ_HEALTH_DATA_IN_BACKGROUND,
                READ_HEALTH_DATA_HISTORY,
            )
        val permissionActivityStateObserver = TestObserver<PermissionsActivityState>()
        viewModel.permissionsActivityState.observeForever(permissionActivityStateObserver)

        viewModel.init(TEST_APP_PACKAGE_NAME, permissions)
        advanceUntilIdle()

        val result = permissionActivityStateObserver.getLastValue()
        assertThat(result is PermissionsActivityState.ShowMedical).isTrue()
        assertThat((result as PermissionsActivityState.ShowMedical).isWriteOnly).isTrue()
    }

    @Test
    fun init_withFitnessPermissions_loadsPermissionActivityScreenStateShowFitness() = runTest {
        val permissions =
            arrayOf(
                READ_EXERCISE,
                READ_SLEEP,
                WRITE_EXERCISE,
                READ_HEALTH_DATA_IN_BACKGROUND,
                READ_HEALTH_DATA_HISTORY,
            )
        val permissionActivityStateObserver = TestObserver<PermissionsActivityState>()
        viewModel.permissionsActivityState.observeForever(permissionActivityStateObserver)

        viewModel.init(TEST_APP_PACKAGE_NAME, permissions)
        advanceUntilIdle()

        val result = permissionActivityStateObserver.getLastValue()
        assertThat(result is PermissionsActivityState.ShowFitness).isTrue()
    }

    @Test
    fun init_withSingleAdditionalPermissions_loadsPermissionActivityScreenStateShowAdditional() =
        runTest {
            val permissions =
                arrayOf(READ_EXERCISE, READ_SLEEP, WRITE_EXERCISE, READ_HEALTH_DATA_IN_BACKGROUND)
            (permissionManager as FakeHealthPermissionManager).setGrantedPermissionsForTest(
                TEST_APP_PACKAGE_NAME,
                listOf(READ_EXERCISE, READ_SLEEP, WRITE_EXERCISE),
            )
            val permissionActivityStateObserver = TestObserver<PermissionsActivityState>()
            viewModel.permissionsActivityState.observeForever(permissionActivityStateObserver)

            viewModel.init(TEST_APP_PACKAGE_NAME, permissions)
            advanceUntilIdle()

            val result = permissionActivityStateObserver.getLastValue()
            assertThat(result is PermissionsActivityState.ShowAdditional).isTrue()
            assertThat((result as PermissionsActivityState.ShowAdditional).singlePermission)
                .isTrue()
        }

    @Test
    fun init_withMultipleAdditionalPermissions_loadsPermissionActivityScreenStateShowAdditional() =
        runTest {
            val permissions =
                arrayOf(
                    READ_EXERCISE,
                    READ_SLEEP,
                    WRITE_EXERCISE,
                    READ_HEALTH_DATA_IN_BACKGROUND,
                    READ_HEALTH_DATA_HISTORY,
                )
            (permissionManager as FakeHealthPermissionManager).setGrantedPermissionsForTest(
                TEST_APP_PACKAGE_NAME,
                listOf(READ_EXERCISE, READ_SLEEP, WRITE_EXERCISE),
            )
            val permissionActivityStateObserver = TestObserver<PermissionsActivityState>()
            viewModel.permissionsActivityState.observeForever(permissionActivityStateObserver)

            viewModel.init(TEST_APP_PACKAGE_NAME, permissions)
            advanceUntilIdle()

            val result = permissionActivityStateObserver.getLastValue()
            assertThat(result is PermissionsActivityState.ShowAdditional).isTrue()
            assertThat((result as PermissionsActivityState.ShowAdditional).singlePermission)
                .isFalse()
        }

    // MedicalScreenStates
    @Test
    fun init_withNoMedicalPermissions_loadsMedicalScreenStateNoMedicalData() = runTest {
        val permissions =
            arrayOf(
                READ_EXERCISE,
                READ_SLEEP,
                WRITE_EXERCISE,
                READ_HEALTH_DATA_IN_BACKGROUND,
                READ_HEALTH_DATA_HISTORY,
            )
        val medicalScreenStateObserver = TestObserver<MedicalScreenState>()
        viewModel.medicalScreenState.observeForever(medicalScreenStateObserver)

        viewModel.init(TEST_APP_PACKAGE_NAME, permissions)
        advanceUntilIdle()

        val result = medicalScreenStateObserver.getLastValue()
        assertThat(result is MedicalScreenState.NoMedicalData).isTrue()
    }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    fun init_withMedicalWritePermissions_loadsMedicalScreenStateShowMedicalWrite() = runTest {
        val permissions =
            arrayOf(
                WRITE_MEDICAL_DATA,
                READ_EXERCISE,
                READ_SLEEP,
                WRITE_EXERCISE,
                READ_HEALTH_DATA_IN_BACKGROUND,
                READ_HEALTH_DATA_HISTORY,
            )
        val medicalScreenStateObserver = TestObserver<MedicalScreenState>()
        viewModel.medicalScreenState.observeForever(medicalScreenStateObserver)

        viewModel.init(TEST_APP_PACKAGE_NAME, permissions)
        advanceUntilIdle()

        val result = medicalScreenStateObserver.getLastValue()
        assertThat(result is MedicalScreenState.ShowMedicalWrite).isTrue()
        assertThat((result as MedicalScreenState.ShowMedicalWrite).appMetadata.appName)
            .isEqualTo(TEST_APP_NAME)
        assertThat(result.appMetadata.packageName).isEqualTo(TEST_APP_PACKAGE_NAME)
        assertThat(result.medicalPermissions)
            .containsExactlyElementsIn(listOf(fromPermissionString(WRITE_MEDICAL_DATA)))
    }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    fun init_withMedicalReadPermissions_loadsMedicalScreenStateShowMedicalRead() = runTest {
        val permissions =
            arrayOf(
                READ_MEDICAL_DATA_VACCINES,
                READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES,
                READ_EXERCISE,
                READ_HEALTH_DATA_IN_BACKGROUND,
                READ_HEALTH_DATA_HISTORY,
            )
        val medicalScreenStateObserver = TestObserver<MedicalScreenState>()
        viewModel.medicalScreenState.observeForever(medicalScreenStateObserver)

        viewModel.init(TEST_APP_PACKAGE_NAME, permissions)
        advanceUntilIdle()

        val result = medicalScreenStateObserver.getLastValue()
        assertThat(result is MedicalScreenState.ShowMedicalRead).isTrue()
        assertThat((result as MedicalScreenState.ShowMedicalRead).appMetadata.appName)
            .isEqualTo(TEST_APP_NAME)
        assertThat(result.appMetadata.packageName).isEqualTo(TEST_APP_PACKAGE_NAME)
        assertThat(result.medicalPermissions)
            .containsExactlyElementsIn(
                listOf(READ_MEDICAL_DATA_VACCINES, READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES)
                    .map { fromPermissionString(it) }
            )
    }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    fun init_withMedicalReadWritePermissions_loadsMedicalScreenStateShowMedicalReadWrite() =
        runTest {
            val permissions =
                arrayOf(
                    READ_MEDICAL_DATA_VACCINES,
                    READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES,
                    WRITE_MEDICAL_DATA,
                    READ_EXERCISE,
                    READ_HEALTH_DATA_IN_BACKGROUND,
                    READ_HEALTH_DATA_HISTORY,
                )
            val medicalScreenStateObserver = TestObserver<MedicalScreenState>()
            viewModel.medicalScreenState.observeForever(medicalScreenStateObserver)

            viewModel.init(TEST_APP_PACKAGE_NAME, permissions)
            advanceUntilIdle()

            val result = medicalScreenStateObserver.getLastValue()
            assertThat(result is MedicalScreenState.ShowMedicalReadWrite).isTrue()
            assertThat((result as MedicalScreenState.ShowMedicalReadWrite).appMetadata.appName)
                .isEqualTo(TEST_APP_NAME)
            assertThat(result.appMetadata.packageName).isEqualTo(TEST_APP_PACKAGE_NAME)
            assertThat(result.medicalPermissions)
                .containsExactlyElementsIn(
                    listOf(
                            READ_MEDICAL_DATA_VACCINES,
                            READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES,
                            WRITE_MEDICAL_DATA,
                        )
                        .map { fromPermissionString(it) }
                )
        }

    // FitnessScreenStates
    @Test
    fun init_withNoFitnessPermissions_loadsFitnessScreenStateNoFitnessData() = runTest {
        val permissions =
            arrayOf(
                READ_MEDICAL_DATA_VACCINES,
                READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES,
                READ_HEALTH_DATA_IN_BACKGROUND,
                READ_HEALTH_DATA_HISTORY,
            )
        val fitnessScreenStateObserver = TestObserver<FitnessScreenState>()
        viewModel.fitnessScreenState.observeForever(fitnessScreenStateObserver)

        viewModel.init(TEST_APP_PACKAGE_NAME, permissions)
        advanceUntilIdle()

        val result = fitnessScreenStateObserver.getLastValue()
        assertThat(result is FitnessScreenState.NoFitnessData).isTrue()
    }

    @Test
    fun init_withFitnessRead_withNoMedical_withNoHistory_loadsFitnessScreenStateShowFitnessRead() =
        runTest {
            val permissions =
                arrayOf(
                    READ_EXERCISE,
                    READ_SLEEP,
                    READ_HEALTH_DATA_IN_BACKGROUND,
                    READ_HEALTH_DATA_HISTORY,
                )
            val fitnessScreenStateObserver = TestObserver<FitnessScreenState>()
            viewModel.fitnessScreenState.observeForever(fitnessScreenStateObserver)

            viewModel.init(TEST_APP_PACKAGE_NAME_2, permissions)
            advanceUntilIdle()

            val result = fitnessScreenStateObserver.getLastValue()
            assertThat(result is FitnessScreenState.ShowFitnessRead).isTrue()
            assertThat((result as FitnessScreenState.ShowFitnessRead).appMetadata.appName)
                .isEqualTo(TEST_APP_NAME_2)
            assertThat(result.appMetadata.packageName).isEqualTo(TEST_APP_PACKAGE_NAME_2)
            assertThat(result.historyGranted).isFalse()
            assertThat(result.hasMedical).isFalse()
            assertThat(result.fitnessPermissions)
                .containsExactlyElementsIn(
                    listOf(READ_EXERCISE, READ_SLEEP).map { fromPermissionString(it) }
                )
        }

    @Test
    fun init_withFitnessRead_withNoMedical_withHistory_loadsFitnessScreenStateShowFitnessRead() =
        runTest {
            val permissions =
                arrayOf(
                    READ_HEART_RATE,
                    READ_SLEEP,
                    READ_HEALTH_DATA_IN_BACKGROUND,
                    READ_HEALTH_DATA_HISTORY,
                )
            (permissionManager as FakeHealthPermissionManager).setGrantedPermissionsForTest(
                TEST_APP_PACKAGE_NAME_2,
                listOf(READ_HEALTH_DATA_HISTORY),
            )
            val fitnessScreenStateObserver = TestObserver<FitnessScreenState>()
            viewModel.fitnessScreenState.observeForever(fitnessScreenStateObserver)

            viewModel.init(TEST_APP_PACKAGE_NAME_2, permissions)
            advanceUntilIdle()

            val result = fitnessScreenStateObserver.getLastValue()
            assertThat(result is FitnessScreenState.ShowFitnessRead).isTrue()
            assertThat((result as FitnessScreenState.ShowFitnessRead).appMetadata.appName)
                .isEqualTo(TEST_APP_NAME_2)
            assertThat(result.appMetadata.packageName).isEqualTo(TEST_APP_PACKAGE_NAME_2)
            assertThat(result.historyGranted).isTrue()
            assertThat(result.hasMedical).isFalse()
            assertThat(result.fitnessPermissions)
                .containsExactlyElementsIn(
                    listOf(READ_HEART_RATE, READ_SLEEP).map { fromPermissionString(it) }
                )
        }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    fun init_withFitnessRead_withMedical_withNoHistory_loadsFitnessScreenStateShowFitnessRead() =
        runTest {
            val permissions =
                arrayOf(
                    READ_EXERCISE,
                    READ_SLEEP,
                    READ_HEALTH_DATA_IN_BACKGROUND,
                    READ_HEALTH_DATA_HISTORY,
                )
            val fitnessScreenStateObserver = TestObserver<FitnessScreenState>()
            viewModel.fitnessScreenState.observeForever(fitnessScreenStateObserver)

            viewModel.init(TEST_APP_PACKAGE_NAME, permissions)
            advanceUntilIdle()

            val result = fitnessScreenStateObserver.getLastValue()
            assertThat(result is FitnessScreenState.ShowFitnessRead).isTrue()
            assertThat((result as FitnessScreenState.ShowFitnessRead).appMetadata.appName)
                .isEqualTo(TEST_APP_NAME)
            assertThat(result.appMetadata.packageName).isEqualTo(TEST_APP_PACKAGE_NAME)
            assertThat(result.historyGranted).isFalse()
            assertThat(result.hasMedical).isTrue()
            assertThat(result.fitnessPermissions)
                .containsExactlyElementsIn(
                    listOf(READ_EXERCISE, READ_SLEEP).map { fromPermissionString(it) }
                )
        }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    fun init_withFitnessRead_withMedical_withHistory_loadsFitnessScreenStateShowFitnessRead() =
        runTest {
            val permissions =
                arrayOf(
                    READ_EXERCISE,
                    READ_SLEEP,
                    READ_HEALTH_DATA_IN_BACKGROUND,
                    READ_HEALTH_DATA_HISTORY,
                )
            (permissionManager as FakeHealthPermissionManager).setGrantedPermissionsForTest(
                TEST_APP_PACKAGE_NAME,
                listOf(READ_HEALTH_DATA_HISTORY),
            )
            val fitnessScreenStateObserver = TestObserver<FitnessScreenState>()
            viewModel.fitnessScreenState.observeForever(fitnessScreenStateObserver)

            viewModel.init(TEST_APP_PACKAGE_NAME, permissions)
            advanceUntilIdle()

            val result = fitnessScreenStateObserver.getLastValue()
            assertThat(result is FitnessScreenState.ShowFitnessRead).isTrue()
            assertThat((result as FitnessScreenState.ShowFitnessRead).appMetadata.appName)
                .isEqualTo(TEST_APP_NAME)
            assertThat(result.appMetadata.packageName).isEqualTo(TEST_APP_PACKAGE_NAME)
            assertThat(result.historyGranted).isTrue()
            assertThat(result.hasMedical).isTrue()
            assertThat(result.fitnessPermissions)
                .containsExactlyElementsIn(
                    listOf(READ_EXERCISE, READ_SLEEP).map { fromPermissionString(it) }
                )
        }

    @Test
    fun init_withFitnessWrite_withNoMedical_loadsFitnessScreenStateShowFitnessWrite() = runTest {
        val permissions =
            arrayOf(
                WRITE_SKIN_TEMPERATURE,
                WRITE_EXERCISE,
                WRITE_PLANNED_EXERCISE,
                READ_HEALTH_DATA_IN_BACKGROUND,
                READ_HEALTH_DATA_HISTORY,
            )
        val fitnessScreenStateObserver = TestObserver<FitnessScreenState>()
        viewModel.fitnessScreenState.observeForever(fitnessScreenStateObserver)

        viewModel.init(TEST_APP_PACKAGE_NAME_2, permissions)
        advanceUntilIdle()

        val result = fitnessScreenStateObserver.getLastValue()
        assertThat(result is FitnessScreenState.ShowFitnessWrite).isTrue()
        assertThat((result as FitnessScreenState.ShowFitnessWrite).appMetadata.appName)
            .isEqualTo(TEST_APP_NAME_2)
        assertThat(result.appMetadata.packageName).isEqualTo(TEST_APP_PACKAGE_NAME_2)
        assertThat(result.hasMedical).isFalse()
        assertThat(result.fitnessPermissions)
            .containsExactlyElementsIn(
                listOf(WRITE_EXERCISE, WRITE_PLANNED_EXERCISE, WRITE_SKIN_TEMPERATURE).map {
                    fromPermissionString(it)
                }
            )
    }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    fun init_withFitnessWrite_withMedical_loadsFitnessScreenStateShowFitnessWrite() = runTest {
        val permissions =
            arrayOf(
                WRITE_SKIN_TEMPERATURE,
                WRITE_EXERCISE,
                WRITE_PLANNED_EXERCISE,
                READ_HEALTH_DATA_IN_BACKGROUND,
                READ_HEALTH_DATA_HISTORY,
            )
        val fitnessScreenStateObserver = TestObserver<FitnessScreenState>()
        viewModel.fitnessScreenState.observeForever(fitnessScreenStateObserver)

        viewModel.init(TEST_APP_PACKAGE_NAME, permissions)
        advanceUntilIdle()

        val result = fitnessScreenStateObserver.getLastValue()
        assertThat(result is FitnessScreenState.ShowFitnessWrite).isTrue()
        assertThat((result as FitnessScreenState.ShowFitnessWrite).appMetadata.appName)
            .isEqualTo(TEST_APP_NAME)
        assertThat(result.appMetadata.packageName).isEqualTo(TEST_APP_PACKAGE_NAME)
        assertThat(result.hasMedical).isTrue()
        assertThat(result.fitnessPermissions)
            .containsExactlyElementsIn(
                listOf(WRITE_EXERCISE, WRITE_PLANNED_EXERCISE, WRITE_SKIN_TEMPERATURE).map {
                    fromPermissionString(it)
                }
            )
    }

    @Test
    fun init_withFitnessReadWrite_withNoMedical_withNoHistory_loadsFitnessScreenStateShowFitnessReadWrite() =
        runTest {
            val permissions =
                arrayOf(
                    READ_EXERCISE,
                    READ_SLEEP,
                    WRITE_SKIN_TEMPERATURE,
                    WRITE_EXERCISE,
                    WRITE_PLANNED_EXERCISE,
                    READ_HEALTH_DATA_IN_BACKGROUND,
                    READ_HEALTH_DATA_HISTORY,
                )
            val fitnessScreenStateObserver = TestObserver<FitnessScreenState>()
            viewModel.fitnessScreenState.observeForever(fitnessScreenStateObserver)

            viewModel.init(TEST_APP_PACKAGE_NAME_2, permissions)
            advanceUntilIdle()

            val result = fitnessScreenStateObserver.getLastValue()
            assertThat(result is FitnessScreenState.ShowFitnessReadWrite).isTrue()
            assertThat((result as FitnessScreenState.ShowFitnessReadWrite).appMetadata.appName)
                .isEqualTo(TEST_APP_NAME_2)
            assertThat(result.appMetadata.packageName).isEqualTo(TEST_APP_PACKAGE_NAME_2)
            assertThat(result.hasMedical).isFalse()
            assertThat(result.historyGranted).isFalse()
            assertThat(result.fitnessPermissions)
                .containsExactlyElementsIn(
                    listOf(
                            READ_EXERCISE,
                            READ_SLEEP,
                            WRITE_EXERCISE,
                            WRITE_PLANNED_EXERCISE,
                            WRITE_SKIN_TEMPERATURE,
                        )
                        .map { fromPermissionString(it) }
                )
        }

    @Test
    fun init_withFitnessReadWrite_withNoMedical_withHistory_loadsFitnessScreenStateShowFitnessReadWrite() =
        runTest {
            val permissions =
                arrayOf(
                    READ_HEART_RATE,
                    READ_STEPS,
                    WRITE_SKIN_TEMPERATURE,
                    WRITE_EXERCISE,
                    WRITE_PLANNED_EXERCISE,
                    READ_HEALTH_DATA_IN_BACKGROUND,
                    READ_HEALTH_DATA_HISTORY,
                )
            (permissionManager as FakeHealthPermissionManager).setGrantedPermissionsForTest(
                TEST_APP_PACKAGE_NAME_2,
                listOf(READ_HEALTH_DATA_HISTORY),
            )
            val fitnessScreenStateObserver = TestObserver<FitnessScreenState>()
            viewModel.fitnessScreenState.observeForever(fitnessScreenStateObserver)

            viewModel.init(TEST_APP_PACKAGE_NAME_2, permissions)
            advanceUntilIdle()

            val result = fitnessScreenStateObserver.getLastValue()
            assertThat(result is FitnessScreenState.ShowFitnessReadWrite).isTrue()
            assertThat((result as FitnessScreenState.ShowFitnessReadWrite).appMetadata.appName)
                .isEqualTo(TEST_APP_NAME_2)
            assertThat(result.appMetadata.packageName).isEqualTo(TEST_APP_PACKAGE_NAME_2)
            assertThat(result.hasMedical).isFalse()
            assertThat(result.historyGranted).isTrue()
            assertThat(result.fitnessPermissions)
                .containsExactlyElementsIn(
                    listOf(
                            READ_HEART_RATE,
                            READ_STEPS,
                            WRITE_EXERCISE,
                            WRITE_PLANNED_EXERCISE,
                            WRITE_SKIN_TEMPERATURE,
                        )
                        .map { fromPermissionString(it) }
                )
        }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    fun init_withFitnessReadWrite_withMedical_withNoHistory_loadsFitnessScreenStateShowFitnessReadWrite() =
        runTest {
            val permissions =
                arrayOf(
                    READ_EXERCISE,
                    READ_SLEEP,
                    WRITE_SKIN_TEMPERATURE,
                    WRITE_EXERCISE,
                    WRITE_PLANNED_EXERCISE,
                    READ_HEALTH_DATA_IN_BACKGROUND,
                    READ_HEALTH_DATA_HISTORY,
                )
            val fitnessScreenStateObserver = TestObserver<FitnessScreenState>()
            viewModel.fitnessScreenState.observeForever(fitnessScreenStateObserver)

            viewModel.init(TEST_APP_PACKAGE_NAME, permissions)
            advanceUntilIdle()

            val result = fitnessScreenStateObserver.getLastValue()
            assertThat(result is FitnessScreenState.ShowFitnessReadWrite).isTrue()
            assertThat((result as FitnessScreenState.ShowFitnessReadWrite).appMetadata.appName)
                .isEqualTo(TEST_APP_NAME)
            assertThat(result.appMetadata.packageName).isEqualTo(TEST_APP_PACKAGE_NAME)
            assertThat(result.hasMedical).isTrue()
            assertThat(result.historyGranted).isFalse()
            assertThat(result.fitnessPermissions)
                .containsExactlyElementsIn(
                    listOf(
                            READ_EXERCISE,
                            READ_SLEEP,
                            WRITE_EXERCISE,
                            WRITE_PLANNED_EXERCISE,
                            WRITE_SKIN_TEMPERATURE,
                        )
                        .map { fromPermissionString(it) }
                )
        }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    fun init_withFitnessReadWrite_withMedical_withHistory_loadsFitnessScreenStateShowFitnessReadWrite() =
        runTest {
            val permissions =
                arrayOf(
                    READ_EXERCISE,
                    READ_SLEEP,
                    WRITE_SKIN_TEMPERATURE,
                    WRITE_EXERCISE,
                    WRITE_PLANNED_EXERCISE,
                    READ_HEALTH_DATA_IN_BACKGROUND,
                    READ_HEALTH_DATA_HISTORY,
                )
            (permissionManager as FakeHealthPermissionManager).setGrantedPermissionsForTest(
                TEST_APP_PACKAGE_NAME,
                listOf(READ_HEALTH_DATA_HISTORY),
            )
            val fitnessScreenStateObserver = TestObserver<FitnessScreenState>()
            viewModel.fitnessScreenState.observeForever(fitnessScreenStateObserver)

            viewModel.init(TEST_APP_PACKAGE_NAME, permissions)
            advanceUntilIdle()

            val result = fitnessScreenStateObserver.getLastValue()
            assertThat(result is FitnessScreenState.ShowFitnessReadWrite).isTrue()
            assertThat((result as FitnessScreenState.ShowFitnessReadWrite).appMetadata.appName)
                .isEqualTo(TEST_APP_NAME)
            assertThat(result.appMetadata.packageName).isEqualTo(TEST_APP_PACKAGE_NAME)
            assertThat(result.hasMedical).isTrue()
            assertThat(result.historyGranted).isTrue()
            assertThat(result.fitnessPermissions)
                .containsExactlyElementsIn(
                    listOf(
                            READ_EXERCISE,
                            READ_SLEEP,
                            WRITE_EXERCISE,
                            WRITE_PLANNED_EXERCISE,
                            WRITE_SKIN_TEMPERATURE,
                        )
                        .map { fromPermissionString(it) }
                )
        }

    // AdditionalScreenStates
    @Test
    fun init_withNoAdditionalPermissions_loadsAdditionalScreenStateNoAdditionalData() = runTest {
        val permissions = arrayOf(READ_HEALTH_DATA_IN_BACKGROUND, READ_HEALTH_DATA_HISTORY)
        val additionalScreenStateObserver = TestObserver<AdditionalScreenState>()
        viewModel.additionalScreenState.observeForever(additionalScreenStateObserver)

        viewModel.init(TEST_APP_PACKAGE_NAME, permissions)
        advanceUntilIdle()

        val result = additionalScreenStateObserver.getLastValue()
        assertThat(result is AdditionalScreenState.NoAdditionalData).isTrue()
    }

    @Test
    fun init_withHistoryRead_withNoMedical_loadsAdditionalScreenStateShowHistory() = runTest {
        val permissions = arrayOf(READ_HEALTH_DATA_HISTORY)
        (permissionManager as FakeHealthPermissionManager).setGrantedPermissionsForTest(
            TEST_APP_PACKAGE_NAME_2,
            listOf(READ_SLEEP),
        )
        val additionalScreenStateObserver = TestObserver<AdditionalScreenState>()
        viewModel.additionalScreenState.observeForever(additionalScreenStateObserver)

        viewModel.init(TEST_APP_PACKAGE_NAME_2, permissions)
        advanceUntilIdle()

        val result = additionalScreenStateObserver.getLastValue()
        assertThat(result is AdditionalScreenState.ShowHistory).isTrue()
        assertThat((result as AdditionalScreenState.ShowHistory).appMetadata.appName)
            .isEqualTo(TEST_APP_NAME_2)
        assertThat(result.appMetadata.packageName).isEqualTo(TEST_APP_PACKAGE_NAME_2)
        assertThat(result.hasMedical).isFalse()
        assertThat(result.isMedicalReadGranted).isFalse()
        assertThat(result.dataAccessDate).isEqualTo(NOW)
    }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    fun init_withHistoryRead_withMedical_medicalNotGranted_loadsAdditionalScreenStateShowHistory() =
        runTest {
            val permissions = arrayOf(READ_HEALTH_DATA_HISTORY)
            (permissionManager as FakeHealthPermissionManager).setGrantedPermissionsForTest(
                TEST_APP_PACKAGE_NAME,
                listOf(READ_SLEEP),
            )
            val additionalScreenStateObserver = TestObserver<AdditionalScreenState>()
            viewModel.additionalScreenState.observeForever(additionalScreenStateObserver)

            viewModel.init(TEST_APP_PACKAGE_NAME, permissions)
            advanceUntilIdle()

            val result = additionalScreenStateObserver.getLastValue()
            assertThat(result is AdditionalScreenState.ShowHistory).isTrue()
            assertThat((result as AdditionalScreenState.ShowHistory).appMetadata.appName)
                .isEqualTo(TEST_APP_NAME)
            assertThat(result.appMetadata.packageName).isEqualTo(TEST_APP_PACKAGE_NAME)
            assertThat(result.hasMedical).isTrue()
            assertThat(result.isMedicalReadGranted).isFalse()
            assertThat(result.dataAccessDate).isEqualTo(NOW)
        }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    fun init_withHistoryRead_withMedical_medicalGranted_loadsAdditionalScreenStateShowHistory() =
        runTest {
            val permissions = arrayOf(READ_HEALTH_DATA_HISTORY)
            (permissionManager as FakeHealthPermissionManager).setGrantedPermissionsForTest(
                TEST_APP_PACKAGE_NAME,
                listOf(READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES, READ_SLEEP),
            )
            val additionalScreenStateObserver = TestObserver<AdditionalScreenState>()
            viewModel.additionalScreenState.observeForever(additionalScreenStateObserver)

            viewModel.init(TEST_APP_PACKAGE_NAME, permissions)
            advanceUntilIdle()

            val result = additionalScreenStateObserver.getLastValue()
            assertThat(result is AdditionalScreenState.ShowHistory).isTrue()
            assertThat((result as AdditionalScreenState.ShowHistory).appMetadata.appName)
                .isEqualTo(TEST_APP_NAME)
            assertThat(result.appMetadata.packageName).isEqualTo(TEST_APP_PACKAGE_NAME)
            assertThat(result.hasMedical).isTrue()
            assertThat(result.isMedicalReadGranted).isTrue()
            assertThat(result.dataAccessDate).isEqualTo(NOW)
        }

    @Test
    fun init_withBackgroundRead_withNoMedical_loadsAdditionalScreenStateShowBackground() = runTest {
        val permissions = arrayOf(READ_HEALTH_DATA_IN_BACKGROUND)
        (permissionManager as FakeHealthPermissionManager).setGrantedPermissionsForTest(
            TEST_APP_PACKAGE_NAME_2,
            listOf(READ_SLEEP),
        )
        val additionalScreenStateObserver = TestObserver<AdditionalScreenState>()
        viewModel.additionalScreenState.observeForever(additionalScreenStateObserver)

        viewModel.init(TEST_APP_PACKAGE_NAME_2, permissions)
        advanceUntilIdle()

        val result = additionalScreenStateObserver.getLastValue()
        assertThat(result is AdditionalScreenState.ShowBackground).isTrue()
        assertThat((result as AdditionalScreenState.ShowBackground).appMetadata.appName)
            .isEqualTo(TEST_APP_NAME_2)
        assertThat(result.appMetadata.packageName).isEqualTo(TEST_APP_PACKAGE_NAME_2)
        assertThat(result.hasMedical).isFalse()
        assertThat(result.isMedicalReadGranted).isFalse()
        assertThat(result.isFitnessReadGranted).isTrue()
    }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    fun init_withBackgroundRead_withMedical_medicalGranted_fitnessNotGranted_loadsAdditionalScreenStateShowBackground() =
        runTest {
            val permissions = arrayOf(READ_HEALTH_DATA_IN_BACKGROUND)
            (permissionManager as FakeHealthPermissionManager).setGrantedPermissionsForTest(
                TEST_APP_PACKAGE_NAME,
                listOf(READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES),
            )
            val additionalScreenStateObserver = TestObserver<AdditionalScreenState>()
            viewModel.additionalScreenState.observeForever(additionalScreenStateObserver)

            viewModel.init(TEST_APP_PACKAGE_NAME, permissions)
            advanceUntilIdle()

            val result = additionalScreenStateObserver.getLastValue()
            assertThat(result is AdditionalScreenState.ShowBackground).isTrue()
            assertThat((result as AdditionalScreenState.ShowBackground).appMetadata.appName)
                .isEqualTo(TEST_APP_NAME)
            assertThat(result.appMetadata.packageName).isEqualTo(TEST_APP_PACKAGE_NAME)
            assertThat(result.hasMedical).isTrue()
            assertThat(result.isMedicalReadGranted).isTrue()
            assertThat(result.isFitnessReadGranted).isFalse()
        }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    fun init_withBackgroundRead_withMedical_medicalNotGranted_fitnessGranted_loadsAdditionalScreenStateShowBackground() =
        runTest {
            val permissions = arrayOf(READ_HEALTH_DATA_IN_BACKGROUND)
            (permissionManager as FakeHealthPermissionManager).setGrantedPermissionsForTest(
                TEST_APP_PACKAGE_NAME,
                listOf(READ_SLEEP),
            )
            val additionalScreenStateObserver = TestObserver<AdditionalScreenState>()
            viewModel.additionalScreenState.observeForever(additionalScreenStateObserver)

            viewModel.init(TEST_APP_PACKAGE_NAME, permissions)
            advanceUntilIdle()

            val result = additionalScreenStateObserver.getLastValue()
            assertThat(result is AdditionalScreenState.ShowBackground).isTrue()
            assertThat((result as AdditionalScreenState.ShowBackground).appMetadata.appName)
                .isEqualTo(TEST_APP_NAME)
            assertThat(result.appMetadata.packageName).isEqualTo(TEST_APP_PACKAGE_NAME)
            assertThat(result.hasMedical).isTrue()
            assertThat(result.isMedicalReadGranted).isFalse()
            assertThat(result.isFitnessReadGranted).isTrue()
        }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    fun init_withBackgroundRead_withMedical_medicalGranted_fitnessGranted_loadsAdditionalScreenStateShowBackground() =
        runTest {
            val permissions = arrayOf(READ_HEALTH_DATA_IN_BACKGROUND)
            (permissionManager as FakeHealthPermissionManager).setGrantedPermissionsForTest(
                TEST_APP_PACKAGE_NAME,
                listOf(READ_SLEEP, READ_MEDICAL_DATA_VACCINES),
            )
            val additionalScreenStateObserver = TestObserver<AdditionalScreenState>()
            viewModel.additionalScreenState.observeForever(additionalScreenStateObserver)

            viewModel.init(TEST_APP_PACKAGE_NAME, permissions)
            advanceUntilIdle()

            val result = additionalScreenStateObserver.getLastValue()
            assertThat(result is AdditionalScreenState.ShowBackground).isTrue()
            assertThat((result as AdditionalScreenState.ShowBackground).appMetadata.appName)
                .isEqualTo(TEST_APP_NAME)
            assertThat(result.appMetadata.packageName).isEqualTo(TEST_APP_PACKAGE_NAME)
            assertThat(result.hasMedical).isTrue()
            assertThat(result.isMedicalReadGranted).isTrue()
            assertThat(result.isFitnessReadGranted).isTrue()
        }

    @Test
    fun init_withAdditionalPermissions_withNoMedical_loadsAdditionalScreenStateShowCombined() =
        runTest {
            val permissions = arrayOf(READ_HEALTH_DATA_HISTORY, READ_HEALTH_DATA_IN_BACKGROUND)
            (permissionManager as FakeHealthPermissionManager).setGrantedPermissionsForTest(
                TEST_APP_PACKAGE_NAME_2,
                listOf(READ_SLEEP),
            )
            val additionalScreenStateObserver = TestObserver<AdditionalScreenState>()
            viewModel.additionalScreenState.observeForever(additionalScreenStateObserver)

            viewModel.init(TEST_APP_PACKAGE_NAME_2, permissions)
            advanceUntilIdle()

            val result = additionalScreenStateObserver.getLastValue()
            assertThat(result is AdditionalScreenState.ShowCombined).isTrue()
            assertThat((result as AdditionalScreenState.ShowCombined).appMetadata.appName)
                .isEqualTo(TEST_APP_NAME_2)
            assertThat(result.appMetadata.packageName).isEqualTo(TEST_APP_PACKAGE_NAME_2)
            assertThat(result.hasMedical).isFalse()
            assertThat(result.isMedicalReadGranted).isFalse()
            assertThat(result.isFitnessReadGranted).isTrue()
            assertThat(result.dataAccessDate).isEqualTo(NOW)
        }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    fun init_withAdditionalPermissions_withMedical_medicalNotGranted_fitnessGranted_loadsAdditionalScreenStateShowCombined() =
        runTest {
            val permissions = arrayOf(READ_HEALTH_DATA_HISTORY, READ_HEALTH_DATA_IN_BACKGROUND)
            (permissionManager as FakeHealthPermissionManager).setGrantedPermissionsForTest(
                TEST_APP_PACKAGE_NAME,
                listOf(READ_SLEEP),
            )
            val additionalScreenStateObserver = TestObserver<AdditionalScreenState>()
            viewModel.additionalScreenState.observeForever(additionalScreenStateObserver)

            viewModel.init(TEST_APP_PACKAGE_NAME, permissions)
            advanceUntilIdle()

            val result = additionalScreenStateObserver.getLastValue()
            assertThat(result is AdditionalScreenState.ShowCombined).isTrue()
            assertThat((result as AdditionalScreenState.ShowCombined).appMetadata.appName)
                .isEqualTo(TEST_APP_NAME)
            assertThat(result.appMetadata.packageName).isEqualTo(TEST_APP_PACKAGE_NAME)
            assertThat(result.hasMedical).isTrue()
            assertThat(result.isMedicalReadGranted).isFalse()
            assertThat(result.isFitnessReadGranted).isTrue()
            assertThat(result.dataAccessDate).isEqualTo(NOW)
        }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    fun init_withAdditionalPermissions_withMedical_medicalGranted_fitnessGranted_loadsAdditionalScreenStateShowCombined() =
        runTest {
            val permissions = arrayOf(READ_HEALTH_DATA_HISTORY, READ_HEALTH_DATA_IN_BACKGROUND)
            (permissionManager as FakeHealthPermissionManager).setGrantedPermissionsForTest(
                TEST_APP_PACKAGE_NAME,
                listOf(READ_SLEEP, READ_MEDICAL_DATA_VACCINES),
            )
            val additionalScreenStateObserver = TestObserver<AdditionalScreenState>()
            viewModel.additionalScreenState.observeForever(additionalScreenStateObserver)

            viewModel.init(TEST_APP_PACKAGE_NAME, permissions)
            advanceUntilIdle()

            val result = additionalScreenStateObserver.getLastValue()
            assertThat(result is AdditionalScreenState.ShowCombined).isTrue()
            assertThat((result as AdditionalScreenState.ShowCombined).appMetadata.appName)
                .isEqualTo(TEST_APP_NAME)
            assertThat(result.appMetadata.packageName).isEqualTo(TEST_APP_PACKAGE_NAME)
            assertThat(result.hasMedical).isTrue()
            assertThat(result.isMedicalReadGranted).isTrue()
            assertThat(result.isFitnessReadGranted).isTrue()
            assertThat(result.dataAccessDate).isEqualTo(NOW)
        }

    @Test
    fun initPermissions_filtersOutUndeclaredPermissions() = runTest {
        (permissionManager as FakeHealthPermissionManager).setGrantedPermissionsForTest(
            TEST_APP_PACKAGE_NAME,
            listOf(READ_EXERCISE),
        )
        val permissionActivityStateObserver = TestObserver<PermissionsActivityState>()
        viewModel.permissionsActivityState.observeForever(permissionActivityStateObserver)

        val fitnessStateObserver = TestObserver<FitnessScreenState>()
        viewModel.fitnessScreenState.observeForever(fitnessStateObserver)

        val additionalStateObserver = TestObserver<AdditionalScreenState>()
        viewModel.additionalScreenState.observeForever(additionalStateObserver)

        viewModel.init(
            TEST_APP_PACKAGE_NAME,
            arrayOf(READ_STEPS, READ_HEART_RATE, READ_HEALTH_DATA_IN_BACKGROUND),
        )

        advanceUntilIdle()
        assertThat(
                permissionActivityStateObserver.getLastValue()
                    is PermissionsActivityState.ShowAdditional
            )
            .isTrue()
        assertThat(fitnessStateObserver.getLastValue() is FitnessScreenState.NoFitnessData).isTrue()
        assertThat(additionalStateObserver.getLastValue() is AdditionalScreenState.ShowBackground)
            .isTrue()
    }

    @Test
    fun initPermissions_filtersOutUnrecognisedPermissions() = runTest {
        val permissionActivityStateObserver = TestObserver<PermissionsActivityState>()
        viewModel.permissionsActivityState.observeForever(permissionActivityStateObserver)

        val fitnessStateObserver = TestObserver<FitnessScreenState>()
        viewModel.fitnessScreenState.observeForever(fitnessStateObserver)

        viewModel.init(TEST_APP_PACKAGE_NAME, arrayOf(READ_EXERCISE, READ_SLEEP, "permission"))
        advanceUntilIdle()

        assertThat(
                permissionActivityStateObserver.getLastValue()
                    is PermissionsActivityState.ShowFitness
            )
            .isTrue()
        assertThat(fitnessStateObserver.getLastValue() is FitnessScreenState.ShowFitnessRead)
            .isTrue()
        assertThat(
                (fitnessStateObserver.getLastValue() as FitnessScreenState.ShowFitnessRead)
                    .fitnessPermissions
            )
            .containsExactlyElementsIn(
                listOf(READ_EXERCISE, READ_SLEEP).map { fromPermissionString(it) }
            )
    }

    @Test
    fun initPermissions_filtersOutGrantedPermissions() = runTest {
        (permissionManager as FakeHealthPermissionManager).setGrantedPermissionsForTest(
            TEST_APP_PACKAGE_NAME,
            listOf(READ_EXERCISE, READ_HEALTH_DATA_IN_BACKGROUND),
        )
        val permissionActivityStateObserver = TestObserver<PermissionsActivityState>()
        viewModel.permissionsActivityState.observeForever(permissionActivityStateObserver)

        val fitnessStateObserver = TestObserver<FitnessScreenState>()
        viewModel.fitnessScreenState.observeForever(fitnessStateObserver)

        val additionalStateObserver = TestObserver<AdditionalScreenState>()
        viewModel.additionalScreenState.observeForever(additionalStateObserver)

        viewModel.init(
            TEST_APP_PACKAGE_NAME,
            arrayOf(READ_EXERCISE, READ_SLEEP, READ_HEALTH_DATA_IN_BACKGROUND),
        )

        advanceUntilIdle()
        assertThat(
                permissionActivityStateObserver.getLastValue()
                    is PermissionsActivityState.ShowFitness
            )
            .isTrue()
        assertThat(fitnessStateObserver.getLastValue() is FitnessScreenState.ShowFitnessRead)
            .isTrue()
        assertThat(
                (fitnessStateObserver.getLastValue() as FitnessScreenState.ShowFitnessRead)
                    .fitnessPermissions
            )
            .containsExactlyElementsIn(listOf(READ_SLEEP).map { fromPermissionString(it) })
        assertThat(additionalStateObserver.getLastValue() is AdditionalScreenState.NoAdditionalData)
            .isTrue()
    }

    @Test
    fun isPermissionLocallyGranted_fitnessPermissionGranted_returnsTrue() = runTest {
        val permissions = arrayOf(READ_EXERCISE, READ_SLEEP)
        viewModel.init(TEST_APP_PACKAGE_NAME, permissions)

        val readExercisePermission = fromPermissionString(READ_EXERCISE)
        viewModel.updateHealthPermission(readExercisePermission, grant = true)

        assertThat(viewModel.isPermissionLocallyGranted(readExercisePermission)).isTrue()
    }

    @Test
    fun isPermissionLocallyGranted_medicalPermissionGranted_returnsTrue() = runTest {
        val permissions = arrayOf(READ_EXERCISE, READ_SLEEP, READ_MEDICAL_DATA_VACCINES)
        viewModel.init(TEST_APP_PACKAGE_NAME, permissions)

        val readImmunizationPermission = fromPermissionString(READ_MEDICAL_DATA_VACCINES)
        viewModel.updateHealthPermission(readImmunizationPermission, grant = true)

        assertThat(viewModel.isPermissionLocallyGranted(readImmunizationPermission)).isTrue()
    }

    @Test
    fun isPermissionLocallyGranted_additionalPermissionGranted_returnsTrue() = runTest {
        val permissions = arrayOf(READ_EXERCISE, READ_SLEEP, READ_HEALTH_DATA_HISTORY)
        viewModel.init(TEST_APP_PACKAGE_NAME, permissions)

        val historyReadPermission = fromPermissionString(READ_HEALTH_DATA_HISTORY)
        viewModel.updateHealthPermission(historyReadPermission, grant = true)

        assertThat(viewModel.isPermissionLocallyGranted(historyReadPermission)).isTrue()
    }

    @Test
    fun isPermissionLocallyGranted_fitnessPermissionRevoked_returnsFalse() = runTest {
        val permissions = arrayOf(READ_EXERCISE, READ_SLEEP, READ_HEALTH_DATA_HISTORY)
        viewModel.init(TEST_APP_PACKAGE_NAME, permissions)

        val readStepsPermission = fromPermissionString(READ_STEPS)
        viewModel.updateHealthPermission(readStepsPermission, grant = false)

        assertThat(viewModel.isPermissionLocallyGranted(readStepsPermission)).isFalse()
    }

    @Test
    fun isPermissionLocallyGranted_medicalPermissionRevoked_returnsFalse() = runTest {
        val permissions = arrayOf(READ_EXERCISE, READ_SLEEP, READ_MEDICAL_DATA_VACCINES)
        viewModel.init(TEST_APP_PACKAGE_NAME, permissions)

        val readImmunizationPermission = fromPermissionString(READ_MEDICAL_DATA_VACCINES)
        viewModel.updateHealthPermission(readImmunizationPermission, grant = false)

        assertThat(viewModel.isPermissionLocallyGranted(readImmunizationPermission)).isFalse()
    }

    @Test
    fun isPermissionLocallyGranted_additionalPermissionRevoked_returnsFalse() = runTest {
        val permissions =
            arrayOf(
                READ_EXERCISE,
                READ_SLEEP,
                READ_HEALTH_DATA_HISTORY,
                READ_HEALTH_DATA_IN_BACKGROUND,
            )
        viewModel.init(TEST_APP_PACKAGE_NAME, permissions)

        val historyReadPermission = fromPermissionString(READ_HEALTH_DATA_HISTORY)
        viewModel.updateHealthPermission(historyReadPermission, grant = false)

        assertThat(viewModel.isPermissionLocallyGranted(historyReadPermission)).isFalse()
    }

    @Test
    fun init_anyReadPermissionsGranted_whenReadFitnessPermissionGranted_returnsTrue() = runTest {
        val permissions =
            arrayOf(
                READ_EXERCISE,
                READ_SLEEP,
                WRITE_DISTANCE,
                READ_HEALTH_DATA_IN_BACKGROUND,
                READ_HEALTH_DATA_HISTORY,
            )
        (permissionManager as FakeHealthPermissionManager).setGrantedPermissionsForTest(
            TEST_APP_PACKAGE_NAME,
            listOf(READ_EXERCISE),
        )
        viewModel.init(TEST_APP_PACKAGE_NAME, permissions)

        advanceUntilIdle()
        assertThat(viewModel.isAnyReadPermissionGranted()).isTrue()
    }

    @Test
    fun init_anyReadPermissionsGranted_whenReadMedicalPermissionGranted_returnsTrue() = runTest {
        val permissions =
            arrayOf(
                READ_EXERCISE,
                READ_SLEEP,
                WRITE_DISTANCE,
                READ_HEALTH_DATA_IN_BACKGROUND,
                READ_HEALTH_DATA_HISTORY,
            )
        (permissionManager as FakeHealthPermissionManager).setGrantedPermissionsForTest(
            TEST_APP_PACKAGE_NAME,
            listOf(READ_MEDICAL_DATA_VACCINES),
        )
        viewModel.init(TEST_APP_PACKAGE_NAME, permissions)

        advanceUntilIdle()
        assertThat(viewModel.isAnyReadPermissionGranted()).isTrue()
    }

    @Test
    fun init_anyReadPermissionsGranted_whenNoReadPermissionGranted_returnsFalse() = runTest {
        val permissions =
            arrayOf(
                READ_EXERCISE,
                READ_SLEEP,
                WRITE_EXERCISE,
                READ_HEALTH_DATA_IN_BACKGROUND,
                READ_HEALTH_DATA_HISTORY,
            )
        (permissionManager as FakeHealthPermissionManager).setGrantedPermissionsForTest(
            TEST_APP_PACKAGE_NAME,
            listOf(WRITE_MEDICAL_DATA),
        )
        viewModel.init(TEST_APP_PACKAGE_NAME, permissions)

        advanceUntilIdle()
        assertThat(viewModel.isAnyReadPermissionGranted()).isFalse()
    }

    @Test
    fun init_isHistoryReadPermissionsGranted_whenHistoryReadPermissionGranted_returnsTrue() =
        runTest {
            val permissions =
                arrayOf(
                    READ_EXERCISE,
                    READ_SLEEP,
                    WRITE_EXERCISE,
                    READ_HEALTH_DATA_IN_BACKGROUND,
                    READ_HEALTH_DATA_HISTORY,
                )
            (permissionManager as FakeHealthPermissionManager).setGrantedPermissionsForTest(
                TEST_APP_PACKAGE_NAME,
                listOf(READ_HEALTH_DATA_HISTORY),
            )
            viewModel.init(TEST_APP_PACKAGE_NAME, permissions)

            advanceUntilIdle()
            assertThat(viewModel.isHistoryAccessGranted()).isTrue()
        }

    @Test
    fun init_isHistoryReadPermissionsGranted_whenHistoryReadPermissionNotGranted_returnsFalse() =
        runTest {
            val permissions =
                arrayOf(
                    READ_EXERCISE,
                    READ_SLEEP,
                    WRITE_EXERCISE,
                    READ_HEALTH_DATA_IN_BACKGROUND,
                    READ_HEALTH_DATA_HISTORY,
                )
            (permissionManager as FakeHealthPermissionManager).setGrantedPermissionsForTest(
                TEST_APP_PACKAGE_NAME,
                listOf(READ_HEALTH_DATA_IN_BACKGROUND),
            )
            viewModel.init(TEST_APP_PACKAGE_NAME, permissions)

            advanceUntilIdle()
            assertThat(viewModel.isHistoryAccessGranted()).isFalse()
        }

    @Test
    fun setFitnessPermissionsConcluded_correctlySets() = runTest {
        viewModel.setFitnessPermissionRequestConcluded(true)
        assertThat(viewModel.isFitnessPermissionRequestConcluded()).isTrue()

        viewModel.setFitnessPermissionRequestConcluded(false)
        assertThat(viewModel.isFitnessPermissionRequestConcluded()).isFalse()
    }

    @Test
    fun updateHealthPermission_grant_updatesGrantedFitnessPermissions() = runTest {
        val permissions = arrayOf(READ_EXERCISE, READ_SLEEP, READ_HEALTH_DATA_HISTORY)
        viewModel.init(TEST_APP_PACKAGE_NAME, permissions)

        val readExercisePermission = fromPermissionString(READ_EXERCISE)
        val testObserver = TestObserver<Set<FitnessPermission>>()
        viewModel.grantedFitnessPermissions.observeForever(testObserver)
        viewModel.updateHealthPermission(readExercisePermission, grant = true)
        advanceUntilIdle()

        assertThat(testObserver.getLastValue()).contains(readExercisePermission)
    }

    @Test
    fun updateHealthPermission_grant_updatesGrantedMedicalPermissions() = runTest {
        val permissions =
            arrayOf(READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES, READ_SLEEP, READ_HEALTH_DATA_HISTORY)
        viewModel.init(TEST_APP_PACKAGE_NAME, permissions)

        val readAllergiesPermission = fromPermissionString(READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES)
        val testObserver = TestObserver<Set<MedicalPermission>>()
        viewModel.grantedMedicalPermissions.observeForever(testObserver)
        viewModel.updateHealthPermission(readAllergiesPermission, grant = true)
        advanceUntilIdle()

        assertThat(testObserver.getLastValue()).contains(readAllergiesPermission)
    }

    @Test
    fun updateHealthPermission_grant_updatesGrantedAdditionalPermissions() = runTest {
        val permissions = arrayOf(READ_EXERCISE, READ_SLEEP, READ_HEALTH_DATA_HISTORY)
        viewModel.init(TEST_APP_PACKAGE_NAME, permissions)

        val historyReadPermission = fromPermissionString(READ_HEALTH_DATA_HISTORY)
        val testObserver = TestObserver<Set<AdditionalPermission>>()
        viewModel.grantedAdditionalPermissions.observeForever(testObserver)
        viewModel.updateHealthPermission(historyReadPermission, grant = true)
        advanceUntilIdle()

        assertThat(testObserver.getLastValue()).contains(historyReadPermission)
    }

    @Test
    fun updateHealthPermission_revoke_updatesGrantedFitnessPermissions() = runTest {
        val permissions = arrayOf(READ_EXERCISE, READ_SLEEP, READ_HEALTH_DATA_HISTORY)
        viewModel.init(TEST_APP_PACKAGE_NAME, permissions)

        val readExercisePermission = fromPermissionString(READ_EXERCISE)
        val testObserver = TestObserver<Set<FitnessPermission>>()
        viewModel.grantedFitnessPermissions.observeForever(testObserver)
        viewModel.updateHealthPermission(readExercisePermission, grant = false)
        advanceUntilIdle()

        assertThat(testObserver.getLastValue()).doesNotContain(readExercisePermission)
    }

    @Test
    fun updateHealthPermission_revoke_updatesGrantedMedicalPermissions() = runTest {
        val permissions =
            arrayOf(READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES, READ_SLEEP, READ_HEALTH_DATA_HISTORY)
        viewModel.init(TEST_APP_PACKAGE_NAME, permissions)

        val readAllergiesPermission = fromPermissionString(READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES)
        val testObserver = TestObserver<Set<MedicalPermission>>()
        viewModel.grantedMedicalPermissions.observeForever(testObserver)
        viewModel.updateHealthPermission(readAllergiesPermission, grant = false)
        advanceUntilIdle()

        assertThat(testObserver.getLastValue()).doesNotContain(readAllergiesPermission)
    }

    @Test
    fun updateHealthPermission_revoke_updatesGrantedAdditionalPermissions() = runTest {
        val permissions = arrayOf(READ_EXERCISE, READ_SLEEP, READ_HEALTH_DATA_HISTORY)
        viewModel.init(TEST_APP_PACKAGE_NAME, permissions)

        val historyReadPermission = fromPermissionString(READ_HEALTH_DATA_HISTORY)
        val testObserver = TestObserver<Set<AdditionalPermission>>()
        viewModel.grantedAdditionalPermissions.observeForever(testObserver)
        viewModel.updateHealthPermission(historyReadPermission, grant = false)
        advanceUntilIdle()

        assertThat(testObserver.getLastValue()).doesNotContain(historyReadPermission)
    }

    @Test
    fun updateFitnessPermissions_grant_updatesGrantedFitnessPermissions() = runTest {
        val permissions = arrayOf(READ_EXERCISE, READ_SLEEP, READ_HEALTH_DATA_HISTORY)
        viewModel.init(TEST_APP_PACKAGE_NAME, permissions)

        val testObserver = TestObserver<Set<FitnessPermission>>()
        viewModel.grantedFitnessPermissions.observeForever(testObserver)
        viewModel.updateFitnessPermissions(grant = true)
        advanceUntilIdle()

        assertThat(testObserver.getLastValue())
            .containsExactly(fromPermissionString(READ_EXERCISE), fromPermissionString(READ_SLEEP))
    }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    fun updateMedicalPermissions_grant_updatesGrantedMedicalPermissions() = runTest {
        val permissions =
            arrayOf(
                READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES,
                WRITE_MEDICAL_DATA,
                READ_EXERCISE,
                READ_SLEEP,
                READ_HEALTH_DATA_HISTORY,
            )
        viewModel.init(TEST_APP_PACKAGE_NAME, permissions)

        val testObserver = TestObserver<Set<MedicalPermission>>()
        viewModel.grantedMedicalPermissions.observeForever(testObserver)
        viewModel.updateMedicalPermissions(grant = true)
        advanceUntilIdle()

        assertThat(testObserver.getLastValue())
            .containsExactlyElementsIn(
                setOf(READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES, WRITE_MEDICAL_DATA).map {
                    fromPermissionString(it)
                }
            )
    }

    @Test
    fun updateAdditionalPermissions_grant_updatesGrantedAdditionalPermissions() = runTest {
        (permissionManager as FakeHealthPermissionManager).setGrantedPermissionsForTest(
            TEST_APP_PACKAGE_NAME,
            listOf(READ_EXERCISE),
        )
        val permissions =
            arrayOf(
                READ_EXERCISE,
                READ_SLEEP,
                READ_HEALTH_DATA_HISTORY,
                READ_HEALTH_DATA_IN_BACKGROUND,
            )
        viewModel.init(TEST_APP_PACKAGE_NAME, permissions)

        val testObserver = TestObserver<Set<AdditionalPermission>>()
        viewModel.grantedAdditionalPermissions.observeForever(testObserver)
        viewModel.updateAdditionalPermissions(grant = true)
        advanceUntilIdle()

        assertThat(testObserver.getLastValue())
            .containsExactly(
                fromPermissionString(READ_HEALTH_DATA_HISTORY),
                fromPermissionString(READ_HEALTH_DATA_IN_BACKGROUND),
            )
    }

    @Test
    fun updateFitnessPermissions_revoke_updatesGrantedFitnessPermissions() = runTest {
        val permissions = arrayOf(READ_EXERCISE, READ_SLEEP, READ_HEALTH_DATA_HISTORY)
        viewModel.init(TEST_APP_PACKAGE_NAME, permissions)

        val testObserver = TestObserver<Set<FitnessPermission>>()
        viewModel.grantedFitnessPermissions.observeForever(testObserver)
        viewModel.updateFitnessPermissions(grant = false)
        advanceUntilIdle()

        assertThat(testObserver.getLastValue()).isEmpty()
    }

    @Test
    fun updateMedicalPermissions_revoke_updatesGrantedMedicalPermissions() = runTest {
        val permissions =
            arrayOf(
                READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES,
                WRITE_MEDICAL_DATA,
                READ_EXERCISE,
                READ_SLEEP,
                READ_HEALTH_DATA_HISTORY,
            )
        viewModel.init(TEST_APP_PACKAGE_NAME, permissions)

        val testObserver = TestObserver<Set<MedicalPermission>>()
        viewModel.grantedMedicalPermissions.observeForever(testObserver)
        viewModel.updateMedicalPermissions(grant = false)
        advanceUntilIdle()

        assertThat(testObserver.getLastValue()).isEmpty()
    }

    @Test
    fun updateAdditionalPermissions_revoke_updatesGrantedAdditionalPermissions() = runTest {
        val permissions =
            arrayOf(
                READ_EXERCISE,
                READ_SLEEP,
                READ_HEALTH_DATA_HISTORY,
                READ_HEALTH_DATA_IN_BACKGROUND,
            )
        viewModel.init(TEST_APP_PACKAGE_NAME, permissions)

        val testObserver = TestObserver<Set<AdditionalPermission>>()
        viewModel.grantedAdditionalPermissions.observeForever(testObserver)
        viewModel.updateAdditionalPermissions(grant = false)
        advanceUntilIdle()

        assertThat(testObserver.getLastValue()).isEmpty()
    }

    @Test
    fun requestFitnessPermissions_updatesPermissionState() {
        val permissions =
            arrayOf(
                READ_EXERCISE,
                READ_SLEEP,
                READ_HEALTH_DATA_HISTORY,
                READ_HEALTH_DATA_IN_BACKGROUND,
            )
        viewModel.init(TEST_APP_PACKAGE_NAME, permissions)
        viewModel.updateFitnessPermissions(true)

        viewModel.requestFitnessPermissions(TEST_APP_PACKAGE_NAME)
        assertThat(permissionManager.getGrantedHealthPermissions(TEST_APP_PACKAGE_NAME))
            .containsExactly(READ_EXERCISE, READ_SLEEP)
        assertThat(viewModel.getPermissionGrants())
            .isEqualTo(
                mutableMapOf(
                    fromPermissionString(READ_EXERCISE) to PermissionState.GRANTED,
                    fromPermissionString(READ_SLEEP) to PermissionState.GRANTED,
                    fromPermissionString(READ_HEALTH_DATA_HISTORY) to PermissionState.NOT_GRANTED,
                    fromPermissionString(READ_HEALTH_DATA_IN_BACKGROUND) to
                        PermissionState.NOT_GRANTED,
                )
            )
    }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    fun requestMedicalPermissions_updatesPermissionState() {
        val permissions =
            arrayOf(
                READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES,
                WRITE_MEDICAL_DATA,
                READ_EXERCISE,
                READ_SLEEP,
                READ_HEALTH_DATA_HISTORY,
                READ_HEALTH_DATA_IN_BACKGROUND,
            )
        viewModel.init(TEST_APP_PACKAGE_NAME, permissions)
        viewModel.updateMedicalPermissions(true)

        viewModel.requestMedicalPermissions(TEST_APP_PACKAGE_NAME)
        assertThat(permissionManager.getGrantedHealthPermissions(TEST_APP_PACKAGE_NAME))
            .containsExactly(READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES, WRITE_MEDICAL_DATA)
        assertThat(viewModel.getPermissionGrants())
            .isEqualTo(
                mutableMapOf(
                    fromPermissionString(READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES) to
                        PermissionState.GRANTED,
                    fromPermissionString(WRITE_MEDICAL_DATA) to PermissionState.GRANTED,
                    fromPermissionString(READ_EXERCISE) to PermissionState.NOT_GRANTED,
                    fromPermissionString(READ_SLEEP) to PermissionState.NOT_GRANTED,
                    fromPermissionString(READ_HEALTH_DATA_HISTORY) to PermissionState.NOT_GRANTED,
                    fromPermissionString(READ_HEALTH_DATA_IN_BACKGROUND) to
                        PermissionState.NOT_GRANTED,
                )
            )
    }

    @Test
    fun requestAdditionalPermissions_updatesPermissionState() {
        (permissionManager as FakeHealthPermissionManager).setGrantedPermissionsForTest(
            TEST_APP_PACKAGE_NAME,
            listOf(READ_EXERCISE),
        )
        val permissions =
            arrayOf(
                READ_EXERCISE,
                READ_SLEEP,
                READ_HEALTH_DATA_HISTORY,
                READ_HEALTH_DATA_IN_BACKGROUND,
            )
        viewModel.init(TEST_APP_PACKAGE_NAME, permissions)
        viewModel.updateAdditionalPermissions(true)

        viewModel.requestAdditionalPermissions(TEST_APP_PACKAGE_NAME)
        assertThat(permissionManager.getGrantedHealthPermissions(TEST_APP_PACKAGE_NAME))
            .containsExactly(
                READ_EXERCISE,
                READ_HEALTH_DATA_HISTORY,
                READ_HEALTH_DATA_IN_BACKGROUND,
            )
        assertThat(viewModel.getPermissionGrants())
            .isEqualTo(
                mutableMapOf(
                    fromPermissionString(READ_EXERCISE) to PermissionState.GRANTED,
                    fromPermissionString(READ_SLEEP) to PermissionState.NOT_GRANTED,
                    fromPermissionString(READ_HEALTH_DATA_HISTORY) to PermissionState.GRANTED,
                    fromPermissionString(READ_HEALTH_DATA_IN_BACKGROUND) to PermissionState.GRANTED,
                )
            )
    }

    @Test
    fun requestAdditionalPermissions_skipsExerciseRoutePermission_updatesPermissionState() {
        (permissionManager as FakeHealthPermissionManager).setGrantedPermissionsForTest(
            TEST_APP_PACKAGE_NAME,
            listOf(READ_EXERCISE_ROUTES),
        )
        val permissions =
            arrayOf(
                READ_EXERCISE,
                READ_SLEEP,
                READ_HEALTH_DATA_HISTORY,
                READ_HEALTH_DATA_IN_BACKGROUND,
                READ_EXERCISE_ROUTES,
            )
        viewModel.init(TEST_APP_PACKAGE_NAME, permissions)
        viewModel.updateAdditionalPermissions(false)

        viewModel.requestAdditionalPermissions(TEST_APP_PACKAGE_NAME)
        assertThat(permissionManager.getGrantedHealthPermissions(TEST_APP_PACKAGE_NAME))
            .containsExactly(READ_EXERCISE_ROUTES)
        assertThat(viewModel.getPermissionGrants())
            .isEqualTo(
                mutableMapOf(
                    fromPermissionString(READ_EXERCISE) to PermissionState.NOT_GRANTED,
                    fromPermissionString(READ_SLEEP) to PermissionState.NOT_GRANTED,
                    fromPermissionString(READ_HEALTH_DATA_HISTORY) to PermissionState.NOT_GRANTED,
                    fromPermissionString(READ_HEALTH_DATA_IN_BACKGROUND) to
                        PermissionState.NOT_GRANTED,
                    fromPermissionString(READ_EXERCISE_ROUTES) to PermissionState.GRANTED,
                )
            )
    }

    @Test
    fun requestHealthPermissionsWithoutGrantingOrRevoking_doesNotUpdatePermissions() = runTest {
        (permissionManager as FakeHealthPermissionManager).setGrantedPermissionsForTest(
            TEST_APP_PACKAGE_NAME,
            listOf(READ_EXERCISE, READ_SLEEP, READ_HEALTH_DATA_HISTORY),
        )
        val permissions =
            arrayOf(
                READ_EXERCISE,
                READ_SLEEP,
                READ_HEALTH_DATA_HISTORY,
                READ_HEALTH_DATA_IN_BACKGROUND,
                READ_EXERCISE_ROUTES,
            )
        viewModel.init(TEST_APP_PACKAGE_NAME, permissions)

        viewModel.updatePermissionGrants()
        assertThat(permissionManager.getGrantedHealthPermissions(TEST_APP_PACKAGE_NAME))
            .containsExactly(READ_EXERCISE, READ_SLEEP, READ_HEALTH_DATA_HISTORY)
        assertThat(viewModel.getPermissionGrants())
            .isEqualTo(
                mutableMapOf(
                    fromPermissionString(READ_EXERCISE) to PermissionState.GRANTED,
                    fromPermissionString(READ_SLEEP) to PermissionState.GRANTED,
                    fromPermissionString(READ_HEALTH_DATA_HISTORY) to PermissionState.GRANTED,
                    fromPermissionString(READ_HEALTH_DATA_IN_BACKGROUND) to
                        PermissionState.NOT_GRANTED,
                    fromPermissionString(READ_EXERCISE_ROUTES) to PermissionState.NOT_GRANTED,
                )
            )
    }

    @Test
    fun isAnyPermissionUserFixed_whenNoPermissionUserFixed_returnsFalse() {
        val permissionFlags =
            mapOf(
                READ_EXERCISE to PackageManager.FLAG_PERMISSION_USER_SET,
                READ_SLEEP to PackageManager.FLAG_PERMISSION_GRANTED_BY_DEFAULT,
            )
        (permissionManager as FakeHealthPermissionManager).setHealthPermissionFlags(
            TEST_APP_PACKAGE_NAME,
            permissionFlags,
        )

        val result =
            viewModel.isAnyPermissionUserFixed(
                TEST_APP_PACKAGE_NAME,
                arrayOf(READ_EXERCISE, READ_SLEEP),
            )
        assertThat(result).isFalse()
    }

    @Test
    fun isAnyPermissionUserFixed_whenAtLeastOnePermissionIsUserFixed_returnsTrue() {
        val permissionFlags =
            mapOf(
                READ_EXERCISE to PackageManager.FLAG_PERMISSION_USER_SET,
                READ_SLEEP to PackageManager.FLAG_PERMISSION_USER_FIXED,
            )
        (permissionManager as FakeHealthPermissionManager).setHealthPermissionFlags(
            TEST_APP_PACKAGE_NAME,
            permissionFlags,
        )

        val result =
            viewModel.isAnyPermissionUserFixed(
                TEST_APP_PACKAGE_NAME,
                arrayOf(READ_EXERCISE, READ_SLEEP),
            )
        assertThat(result).isTrue()
    }

    @Test
    fun isAnyPermissionUserFixed_whenNoPermissionUserFixed_andSomePermissionsNotDeclared_returnsFalse() {
        val permissionFlags =
            mapOf(
                READ_EXERCISE to PackageManager.FLAG_PERMISSION_USER_SET,
                READ_SLEEP to PackageManager.FLAG_PERMISSION_GRANTED_BY_DEFAULT,
            )
        (permissionManager as FakeHealthPermissionManager).setHealthPermissionFlags(
            TEST_APP_PACKAGE_NAME,
            permissionFlags,
        )

        val result =
            viewModel.isAnyPermissionUserFixed(
                TEST_APP_PACKAGE_NAME,
                arrayOf(READ_EXERCISE, READ_SLEEP, READ_SKIN_TEMPERATURE),
            )
        assertThat(result).isFalse()
    }

    @Test
    fun isAnyPermissionUserFixed_whenAtLeastOnePermissionIsUserFixed__andSomePermissionsNotDeclared_returnsTrue() {
        val permissionFlags =
            mapOf(
                READ_EXERCISE to PackageManager.FLAG_PERMISSION_USER_SET,
                READ_SLEEP to PackageManager.FLAG_PERMISSION_USER_FIXED,
            )
        (permissionManager as FakeHealthPermissionManager).setHealthPermissionFlags(
            TEST_APP_PACKAGE_NAME,
            permissionFlags,
        )

        val result =
            viewModel.isAnyPermissionUserFixed(
                TEST_APP_PACKAGE_NAME,
                arrayOf(READ_EXERCISE, READ_SLEEP, WRITE_PLANNED_EXERCISE),
            )
        assertThat(result).isTrue()
    }
}
