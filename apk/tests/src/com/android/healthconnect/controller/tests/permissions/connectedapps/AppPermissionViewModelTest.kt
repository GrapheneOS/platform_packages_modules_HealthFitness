/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.android.healthconnect.controller.tests.permissions.connectedapps

import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.android.healthconnect.controller.permissions.additionalaccess.ExerciseRouteState
import com.android.healthconnect.controller.permissions.additionalaccess.PermissionUiState
import com.android.healthconnect.controller.permissions.api.GrantHealthPermissionUseCase
import com.android.healthconnect.controller.permissions.api.LoadAccessDateUseCase
import com.android.healthconnect.controller.permissions.api.RevokeAllHealthPermissionsUseCase
import com.android.healthconnect.controller.permissions.api.RevokeHealthPermissionUseCase
import com.android.healthconnect.controller.permissions.app.AppPermissionViewModel
import com.android.healthconnect.controller.permissions.app.LoadAppPermissionsStatusUseCase
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.permissions.data.HealthPermission.AdditionalPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.FitnessPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.MedicalPermission
import com.android.healthconnect.controller.permissions.data.MedicalPermissionType
import com.android.healthconnect.controller.permissions.data.PermissionsAccessType
import com.android.healthconnect.controller.selectabledeletion.DeletionType.DeleteAppData
import com.android.healthconnect.controller.selectabledeletion.api.DeleteAppDataUseCase
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.shared.app.AppInfoReader
import com.android.healthconnect.controller.tests.utils.InstantTaskExecutorRule
import com.android.healthconnect.controller.tests.utils.NOW
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.TestObserver
import com.android.healthconnect.controller.tests.utils.di.FakeGetGrantedHealthPermissionsUseCase
import com.android.healthconnect.controller.tests.utils.di.FakeLoadExerciseRoute
import com.android.healthconnect.controller.utils.DeviceInfoUtils
import com.android.healthfitness.flags.Flags
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
class AppPermissionViewModelTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)
    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()
    @get:Rule val setFlagsRule = SetFlagsRule()
    private val testDispatcher = UnconfinedTestDispatcher()

    private var appInfoReader: AppInfoReader = mock()
    private val healthPermissionReader: HealthPermissionReader = mock()
    private val getGrantedHealthPermissionsUseCase = FakeGetGrantedHealthPermissionsUseCase()
    private val loadAccessDateUseCase: LoadAccessDateUseCase = mock()
    private val deleteAppDataUseCase: DeleteAppDataUseCase = mock()
    private val revokeAllHealthPermissionsUseCase: RevokeAllHealthPermissionsUseCase = mock()
    private val revokePermissionStatusUseCase: RevokeHealthPermissionUseCase = mock()
    private val grantPermissionsUseCase: GrantHealthPermissionUseCase = mock()
    private val loadExerciseRoutePermissionUseCase = FakeLoadExerciseRoute()
    private val deviceInfoUtils: DeviceInfoUtils = mock()

    private lateinit var loadAppPermissionsStatusUseCase: LoadAppPermissionsStatusUseCase
    private lateinit var appPermissionViewModel: AppPermissionViewModel

    private val readExercisePermission =
        FitnessPermission(FitnessPermissionType.EXERCISE, PermissionsAccessType.READ)
    private val readNutritionPermission =
        FitnessPermission(FitnessPermissionType.NUTRITION, PermissionsAccessType.READ)
    private val readHeartRatePermission =
        FitnessPermission(FitnessPermissionType.HEART_RATE, PermissionsAccessType.READ)
    private val readSkinTemperaturePermission =
        FitnessPermission(FitnessPermissionType.SKIN_TEMPERATURE, PermissionsAccessType.READ)
    private val readExerciseRoutesPermission = AdditionalPermission.READ_EXERCISE_ROUTES
    private val readHistoryDataPermission = AdditionalPermission.READ_HEALTH_DATA_HISTORY
    private val readDataInBackgroundPermission = AdditionalPermission.READ_HEALTH_DATA_IN_BACKGROUND
    private val readImmunization = MedicalPermission(MedicalPermissionType.VACCINES)
    private val readAllergies = MedicalPermission(MedicalPermissionType.ALLERGIES_INTOLERANCES)
    private val writeSleepPermission =
        FitnessPermission(FitnessPermissionType.SLEEP, PermissionsAccessType.WRITE)
    private val writeDistancePermission =
        FitnessPermission(FitnessPermissionType.DISTANCE, PermissionsAccessType.WRITE)
    private val writeMedicalData = MedicalPermission(MedicalPermissionType.ALL_MEDICAL_DATA)

    @Captor lateinit var appDataCaptor: ArgumentCaptor<DeleteAppData>

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        hiltRule.inject()
        Dispatchers.setMain(testDispatcher)
        whenever(healthPermissionReader.getValidHealthPermissions(any())).thenCallRealMethod()
        whenever(healthPermissionReader.getSystemHealthPermissions())
            .thenReturn(
                listOf(readHeartRatePermission.toString(), readSkinTemperaturePermission.toString())
            )
        whenever(healthPermissionReader.maybeFilterOutAdditionalIfNotValid(any()))
            .thenCallRealMethod()
        loadAppPermissionsStatusUseCase =
            LoadAppPermissionsStatusUseCase(
                getGrantedHealthPermissionsUseCase,
                healthPermissionReader,
                Dispatchers.Main,
            )
        appPermissionViewModel =
            AppPermissionViewModel(
                getApplicationContext(),
                appInfoReader,
                loadAppPermissionsStatusUseCase,
                grantPermissionsUseCase,
                revokePermissionStatusUseCase,
                revokeAllHealthPermissionsUseCase,
                deleteAppDataUseCase,
                loadAccessDateUseCase,
                getGrantedHealthPermissionsUseCase,
                loadExerciseRoutePermissionUseCase,
                healthPermissionReader,
                deviceInfoUtils,
                Dispatchers.Main,
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadsCorrectAccessDate() {
        val accessDate = NOW
        whenever(loadAccessDateUseCase.invoke(TEST_APP_PACKAGE_NAME)).thenReturn(accessDate)
        val result = appPermissionViewModel.loadAccessDate(TEST_APP_PACKAGE_NAME)
        assertThat(result).isEqualTo(accessDate)
    }

    @Test
    fun whenPackageSupported_fitnessOnly_loadAllPermissions() = runTest {
        setupDeclaredAndGrantedFitnessPermissions()

        val fitnessPermissionsObserver = TestObserver<List<FitnessPermission>>()
        val grantedFitnessPermissionsObserver = TestObserver<Set<FitnessPermission>>()
        appPermissionViewModel.fitnessPermissions.observeForever(fitnessPermissionsObserver)
        appPermissionViewModel.grantedFitnessPermissions.observeForever(
            grantedFitnessPermissionsObserver
        )
        val medicalPermissionsObserver = TestObserver<List<MedicalPermission>>()
        val grantedMedicalPermissionsObserver = TestObserver<Set<MedicalPermission>>()
        appPermissionViewModel.medicalPermissions.observeForever(medicalPermissionsObserver)
        appPermissionViewModel.grantedMedicalPermissions.observeForever(
            grantedMedicalPermissionsObserver
        )

        val atLeastOneFitnessPermissionGrantedObserver = TestObserver<Boolean>()
        val atLeastOneMedicalPermissionGrantedObserver = TestObserver<Boolean>()
        val atLeastOneHealthPermissionGrantedObserver = TestObserver<Boolean>()
        appPermissionViewModel.atLeastOneFitnessPermissionGranted.observeForever(
            atLeastOneFitnessPermissionGrantedObserver
        )
        appPermissionViewModel.atLeastOneMedicalPermissionGranted.observeForever(
            atLeastOneMedicalPermissionGrantedObserver
        )
        appPermissionViewModel.atLeastOneHealthPermissionGranted.observeForever(
            atLeastOneHealthPermissionGrantedObserver
        )

        appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        val fitnessPermissionsResult = fitnessPermissionsObserver.getLastValue()
        val grantedFitnessPermissionsResult = grantedFitnessPermissionsObserver.getLastValue()
        val medicalPermissionsResult = medicalPermissionsObserver.getLastValue()
        val grantedMedicalPermissionsResult = grantedMedicalPermissionsObserver.getLastValue()

        val atLeastOneFitnessPermissionGrantedResult =
            atLeastOneFitnessPermissionGrantedObserver.getLastValue()
        val atLeastOneMedicalPermissionGrantedResult =
            atLeastOneMedicalPermissionGrantedObserver.getLastValue()
        val atLeastOneHealthPermissionGrantedResult =
            atLeastOneHealthPermissionGrantedObserver.getLastValue()

        assertThat(fitnessPermissionsResult)
            .containsExactlyElementsIn(
                listOf(
                    readExercisePermission,
                    readNutritionPermission,
                    writeSleepPermission,
                    writeDistancePermission,
                )
            )
        assertThat(grantedFitnessPermissionsResult)
            .containsExactlyElementsIn(setOf(readExercisePermission, writeDistancePermission))
        assertThat(medicalPermissionsResult).containsExactlyElementsIn(listOf<MedicalPermission>())
        assertThat(grantedMedicalPermissionsResult)
            .containsExactlyElementsIn(setOf<MedicalPermission>())

        assertThat(atLeastOneFitnessPermissionGrantedResult).isTrue()
        assertThat(atLeastOneMedicalPermissionGrantedResult).isFalse()
        assertThat(atLeastOneHealthPermissionGrantedResult).isTrue()
    }

    @Test
    fun whenPackageSupported_fitnessAndMedical_loadAllPermissions() = runTest {
        setupDeclaredAndGrantedFitnessAndMedicalPermissions()
        val fitnessPermissionsObserver = TestObserver<List<FitnessPermission>>()
        val grantedFitnessPermissionsObserver = TestObserver<Set<FitnessPermission>>()
        val medicalPermissionsObserver = TestObserver<List<MedicalPermission>>()
        val grantedMedicalPermissionsObserver = TestObserver<Set<MedicalPermission>>()
        appPermissionViewModel.fitnessPermissions.observeForever(fitnessPermissionsObserver)
        appPermissionViewModel.grantedFitnessPermissions.observeForever(
            grantedFitnessPermissionsObserver
        )
        appPermissionViewModel.medicalPermissions.observeForever(medicalPermissionsObserver)
        appPermissionViewModel.grantedMedicalPermissions.observeForever(
            grantedMedicalPermissionsObserver
        )

        val atLeastOneFitnessPermissionGrantedObserver = TestObserver<Boolean>()
        val atLeastOneMedicalPermissionGrantedObserver = TestObserver<Boolean>()
        val atLeastOneHealthPermissionGrantedObserver = TestObserver<Boolean>()
        appPermissionViewModel.atLeastOneFitnessPermissionGranted.observeForever(
            atLeastOneFitnessPermissionGrantedObserver
        )
        appPermissionViewModel.atLeastOneMedicalPermissionGranted.observeForever(
            atLeastOneMedicalPermissionGrantedObserver
        )
        appPermissionViewModel.atLeastOneHealthPermissionGranted.observeForever(
            atLeastOneHealthPermissionGrantedObserver
        )

        appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        val fitnessPermissionsResult = fitnessPermissionsObserver.getLastValue()
        val grantedFitnessPermissionsResult = grantedFitnessPermissionsObserver.getLastValue()
        val medicalPermissionsResult = medicalPermissionsObserver.getLastValue()
        val grantedMedicalPermissionsResult = grantedMedicalPermissionsObserver.getLastValue()

        val atLeastOneFitnessPermissionGrantedResult =
            atLeastOneFitnessPermissionGrantedObserver.getLastValue()
        val atLeastOneMedicalPermissionGrantedResult =
            atLeastOneMedicalPermissionGrantedObserver.getLastValue()
        val atLeastOneHealthPermissionGrantedResult =
            atLeastOneHealthPermissionGrantedObserver.getLastValue()

        assertThat(fitnessPermissionsResult)
            .containsExactlyElementsIn(
                listOf(
                    readExercisePermission,
                    readNutritionPermission,
                    writeSleepPermission,
                    writeDistancePermission,
                )
            )
        assertThat(grantedFitnessPermissionsResult)
            .containsExactlyElementsIn(setOf(readExercisePermission))
        assertThat(medicalPermissionsResult)
            .containsExactlyElementsIn(listOf(readImmunization, writeMedicalData))
        assertThat(grantedMedicalPermissionsResult)
            .containsExactlyElementsIn(setOf(readImmunization))

        assertThat(atLeastOneFitnessPermissionGrantedResult).isTrue()
        assertThat(atLeastOneMedicalPermissionGrantedResult).isTrue()
        assertThat(atLeastOneHealthPermissionGrantedResult).isTrue()
    }

    @Test
    fun whenPackageSupported_medicalOnly_loadAllPermissions() = runTest {
        whenever(healthPermissionReader.isRationaleIntentDeclared(any())).thenReturn(true)
        whenever(healthPermissionReader.getDeclaredHealthPermissions(any()))
            .thenReturn(listOf(readImmunization.toString(), writeMedicalData.toString()))
        getGrantedHealthPermissionsUseCase.updateData(
            TEST_APP_PACKAGE_NAME,
            listOf(writeMedicalData.toString()),
        )
        val fitnessPermissionsObserver = TestObserver<List<FitnessPermission>>()
        val grantedFitnessPermissionsObserver = TestObserver<Set<FitnessPermission>>()
        val medicalPermissionsObserver = TestObserver<List<MedicalPermission>>()
        val grantedMedicalPermissionsObserver = TestObserver<Set<MedicalPermission>>()
        appPermissionViewModel.fitnessPermissions.observeForever(fitnessPermissionsObserver)
        appPermissionViewModel.grantedFitnessPermissions.observeForever(
            grantedFitnessPermissionsObserver
        )
        appPermissionViewModel.medicalPermissions.observeForever(medicalPermissionsObserver)
        appPermissionViewModel.grantedMedicalPermissions.observeForever(
            grantedMedicalPermissionsObserver
        )

        val atLeastOneFitnessPermissionGrantedObserver = TestObserver<Boolean>()
        val atLeastOneMedicalPermissionGrantedObserver = TestObserver<Boolean>()
        val atLeastOneHealthPermissionGrantedObserver = TestObserver<Boolean>()
        appPermissionViewModel.atLeastOneFitnessPermissionGranted.observeForever(
            atLeastOneFitnessPermissionGrantedObserver
        )
        appPermissionViewModel.atLeastOneMedicalPermissionGranted.observeForever(
            atLeastOneMedicalPermissionGrantedObserver
        )
        appPermissionViewModel.atLeastOneHealthPermissionGranted.observeForever(
            atLeastOneHealthPermissionGrantedObserver
        )

        appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        val fitnessPermissionsResult = fitnessPermissionsObserver.getLastValue()
        val grantedFitnessPermissionsResult = grantedFitnessPermissionsObserver.getLastValue()
        val medicalPermissionsResult = medicalPermissionsObserver.getLastValue()
        val grantedMedicalPermissionsResult = grantedMedicalPermissionsObserver.getLastValue()

        val atLeastOneFitnessPermissionGrantedResult =
            atLeastOneFitnessPermissionGrantedObserver.getLastValue()
        val atLeastOneMedicalPermissionGrantedResult =
            atLeastOneMedicalPermissionGrantedObserver.getLastValue()
        val atLeastOneHealthPermissionGrantedResult =
            atLeastOneHealthPermissionGrantedObserver.getLastValue()

        assertThat(fitnessPermissionsResult).containsExactlyElementsIn(listOf<FitnessPermission>())
        assertThat(grantedFitnessPermissionsResult)
            .containsExactlyElementsIn(setOf<FitnessPermission>())
        assertThat(medicalPermissionsResult)
            .containsExactlyElementsIn(listOf(readImmunization, writeMedicalData))
        assertThat(grantedMedicalPermissionsResult)
            .containsExactlyElementsIn(setOf(writeMedicalData))

        assertThat(atLeastOneFitnessPermissionGrantedResult).isFalse()
        assertThat(atLeastOneMedicalPermissionGrantedResult).isTrue()
        assertThat(atLeastOneHealthPermissionGrantedResult).isTrue()
    }

    @Test
    @EnableFlags(Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED)
    fun whenPackageSupported_wearOnlyReturnsSystemPermissions_loadAllPermissions() = runTest {
        whenever(deviceInfoUtils.isOnWatch(any())).thenReturn(true)
        whenever(healthPermissionReader.isRationaleIntentDeclared(any())).thenReturn(false)
        whenever(healthPermissionReader.getDeclaredHealthPermissions(any()))
            .thenReturn(
                listOf(
                    readHeartRatePermission.toString(),
                    readSkinTemperaturePermission.toString(),
                    readNutritionPermission.toString(),
                    writeSleepPermission.toString(),
                    writeDistancePermission.toString(),
                    readDataInBackgroundPermission.toString(),
                    readHistoryDataPermission.toString(),
                    readAllergies.toString(),
                )
            )
        getGrantedHealthPermissionsUseCase.updateData(
            TEST_APP_PACKAGE_NAME,
            listOf(readHeartRatePermission.toString(), readDataInBackgroundPermission.toString()),
        )

        val fitnessPermissionsObserver = TestObserver<List<FitnessPermission>>()
        val grantedFitnessPermissionsObserver = TestObserver<Set<FitnessPermission>>()
        appPermissionViewModel.fitnessPermissions.observeForever(fitnessPermissionsObserver)
        appPermissionViewModel.grantedFitnessPermissions.observeForever(
            grantedFitnessPermissionsObserver
        )
        val medicalPermissionsObserver = TestObserver<List<MedicalPermission>>()
        val grantedMedicalPermissionsObserver = TestObserver<Set<MedicalPermission>>()
        appPermissionViewModel.medicalPermissions.observeForever(medicalPermissionsObserver)
        appPermissionViewModel.grantedMedicalPermissions.observeForever(
            grantedMedicalPermissionsObserver
        )
        val grantedAdditionalPermissionsObserver = TestObserver<Set<AdditionalPermission>>()
        appPermissionViewModel.grantedAdditionalPermissions.observeForever(
            grantedAdditionalPermissionsObserver
        )

        val atLeastOneFitnessPermissionGrantedObserver = TestObserver<Boolean>()
        val atLeastOneMedicalPermissionGrantedObserver = TestObserver<Boolean>()
        val atLeastOneHealthPermissionGrantedObserver = TestObserver<Boolean>()
        appPermissionViewModel.atLeastOneFitnessPermissionGranted.observeForever(
            atLeastOneFitnessPermissionGrantedObserver
        )
        appPermissionViewModel.atLeastOneMedicalPermissionGranted.observeForever(
            atLeastOneMedicalPermissionGrantedObserver
        )
        appPermissionViewModel.atLeastOneHealthPermissionGranted.observeForever(
            atLeastOneHealthPermissionGrantedObserver
        )

        appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        val fitnessPermissionsResult = fitnessPermissionsObserver.getLastValue()
        val grantedFitnessPermissionsResult = grantedFitnessPermissionsObserver.getLastValue()
        val medicalPermissionsResult = medicalPermissionsObserver.getLastValue()
        val grantedMedicalPermissionsResult = grantedMedicalPermissionsObserver.getLastValue()
        val grantedAdditionalPermissionsResult = grantedAdditionalPermissionsObserver.getLastValue()

        val atLeastOneFitnessPermissionGrantedResult =
            atLeastOneFitnessPermissionGrantedObserver.getLastValue()
        val atLeastOneMedicalPermissionGrantedResult =
            atLeastOneMedicalPermissionGrantedObserver.getLastValue()
        val atLeastOneHealthPermissionGrantedResult =
            atLeastOneHealthPermissionGrantedObserver.getLastValue()

        assertThat(fitnessPermissionsResult)
            .containsExactlyElementsIn(
                listOf(readHeartRatePermission, readSkinTemperaturePermission)
            )
        assertThat(grantedFitnessPermissionsResult)
            .containsExactlyElementsIn(setOf(readHeartRatePermission))
        assertThat(grantedAdditionalPermissionsResult)
            .containsExactlyElementsIn(setOf(readDataInBackgroundPermission))
        assertThat(medicalPermissionsResult).containsExactlyElementsIn(listOf<MedicalPermission>())
        assertThat(grantedMedicalPermissionsResult)
            .containsExactlyElementsIn(setOf<MedicalPermission>())

        assertThat(atLeastOneFitnessPermissionGrantedResult).isTrue()
        assertThat(atLeastOneMedicalPermissionGrantedResult).isFalse()
        assertThat(atLeastOneHealthPermissionGrantedResult).isTrue()
    }

    @Test
    fun whenPackageNotSupported_fitnessOnly_loadOnlyGrantedPermissions() = runTest {
        whenever(healthPermissionReader.isRationaleIntentDeclared(any())).thenReturn(false)
        whenever(healthPermissionReader.getDeclaredHealthPermissions(any()))
            .thenReturn(
                listOf(
                    readExercisePermission.toString(),
                    readNutritionPermission.toString(),
                    writeSleepPermission.toString(),
                    writeDistancePermission.toString(),
                )
            )
        getGrantedHealthPermissionsUseCase.updateData(
            TEST_APP_PACKAGE_NAME,
            listOf(readExercisePermission.toString()),
        )
        val fitnessPermissionsObserver = TestObserver<List<FitnessPermission>>()
        val grantedFitnessPermissionsObserver = TestObserver<Set<FitnessPermission>>()
        val medicalPermissionsObserver = TestObserver<List<MedicalPermission>>()
        val grantedMedicalPermissionsObserver = TestObserver<Set<MedicalPermission>>()
        appPermissionViewModel.fitnessPermissions.observeForever(fitnessPermissionsObserver)
        appPermissionViewModel.grantedFitnessPermissions.observeForever(
            grantedFitnessPermissionsObserver
        )
        appPermissionViewModel.medicalPermissions.observeForever(medicalPermissionsObserver)
        appPermissionViewModel.grantedMedicalPermissions.observeForever(
            grantedMedicalPermissionsObserver
        )

        appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        val fitnessPermissionResult = fitnessPermissionsObserver.getLastValue()
        val grantedFitnessPermissionsResult = grantedFitnessPermissionsObserver.getLastValue()
        val medicalPermissionResult = medicalPermissionsObserver.getLastValue()
        val grantedMedicalPermissionsResult = grantedMedicalPermissionsObserver.getLastValue()
        assertThat(fitnessPermissionResult)
            .containsExactlyElementsIn(listOf(readExercisePermission))
        assertThat(grantedFitnessPermissionsResult)
            .containsExactlyElementsIn(setOf(readExercisePermission))
        assertThat(medicalPermissionResult).containsExactlyElementsIn(listOf<MedicalPermission>())
        assertThat(grantedMedicalPermissionsResult)
            .containsExactlyElementsIn(setOf<MedicalPermission>())
    }

    @Test
    fun whenPackageNotSupported_medicalOnly_loadOnlyGrantedPermissions() = runTest {
        whenever(healthPermissionReader.isRationaleIntentDeclared(any())).thenReturn(false)
        whenever(healthPermissionReader.getDeclaredHealthPermissions(any()))
            .thenReturn(listOf(writeMedicalData.toString(), readImmunization.toString()))
        getGrantedHealthPermissionsUseCase.updateData(
            TEST_APP_PACKAGE_NAME,
            listOf(writeMedicalData.toString()),
        )
        val fitnessPermissionsObserver = TestObserver<List<FitnessPermission>>()
        val grantedFitnessPermissionsObserver = TestObserver<Set<FitnessPermission>>()
        val medicalPermissionsObserver = TestObserver<List<MedicalPermission>>()
        val grantedMedicalPermissionsObserver = TestObserver<Set<MedicalPermission>>()
        appPermissionViewModel.fitnessPermissions.observeForever(fitnessPermissionsObserver)
        appPermissionViewModel.grantedFitnessPermissions.observeForever(
            grantedFitnessPermissionsObserver
        )
        appPermissionViewModel.medicalPermissions.observeForever(medicalPermissionsObserver)
        appPermissionViewModel.grantedMedicalPermissions.observeForever(
            grantedMedicalPermissionsObserver
        )

        appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        val fitnessPermissionResult = fitnessPermissionsObserver.getLastValue()
        val grantedFitnessPermissionsResult = grantedFitnessPermissionsObserver.getLastValue()
        val medicalPermissionResult = medicalPermissionsObserver.getLastValue()
        val grantedMedicalPermissionsResult = grantedMedicalPermissionsObserver.getLastValue()
        assertThat(fitnessPermissionResult).containsExactlyElementsIn(listOf<FitnessPermission>())
        assertThat(grantedFitnessPermissionsResult)
            .containsExactlyElementsIn(setOf<FitnessPermission>())
        assertThat(medicalPermissionResult).containsExactlyElementsIn(listOf(writeMedicalData))
        assertThat(grantedMedicalPermissionsResult)
            .containsExactlyElementsIn(setOf(writeMedicalData))
    }

    @Test
    fun whenPackageNotSupported_fitnessAndMedical_loadOnlyGrantedPermissions() = runTest {
        whenever(healthPermissionReader.isRationaleIntentDeclared(any())).thenReturn(false)
        whenever(healthPermissionReader.getDeclaredHealthPermissions(any()))
            .thenReturn(
                listOf(
                    readExercisePermission.toString(),
                    readNutritionPermission.toString(),
                    readImmunization.toString(),
                    writeSleepPermission.toString(),
                    writeDistancePermission.toString(),
                    writeMedicalData.toString(),
                )
            )
        getGrantedHealthPermissionsUseCase.updateData(
            TEST_APP_PACKAGE_NAME,
            listOf(readExercisePermission.toString(), writeMedicalData.toString()),
        )
        val fitnessPermissionsObserver = TestObserver<List<FitnessPermission>>()
        val grantedFitnessPermissionsObserver = TestObserver<Set<FitnessPermission>>()
        val medicalPermissionsObserver = TestObserver<List<MedicalPermission>>()
        val grantedMedicalPermissionsObserver = TestObserver<Set<MedicalPermission>>()
        appPermissionViewModel.fitnessPermissions.observeForever(fitnessPermissionsObserver)
        appPermissionViewModel.grantedFitnessPermissions.observeForever(
            grantedFitnessPermissionsObserver
        )
        appPermissionViewModel.medicalPermissions.observeForever(medicalPermissionsObserver)
        appPermissionViewModel.grantedMedicalPermissions.observeForever(
            grantedMedicalPermissionsObserver
        )

        appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        val fitnessPermissionResult = fitnessPermissionsObserver.getLastValue()
        val grantedFitnessPermissionsResult = grantedFitnessPermissionsObserver.getLastValue()
        val medicalPermissionResult = medicalPermissionsObserver.getLastValue()
        val grantedMedicalPermissionsResult = grantedMedicalPermissionsObserver.getLastValue()
        assertThat(fitnessPermissionResult)
            .containsExactlyElementsIn(listOf(readExercisePermission))
        assertThat(grantedFitnessPermissionsResult)
            .containsExactlyElementsIn(setOf(readExercisePermission))
        assertThat(medicalPermissionResult).containsExactlyElementsIn(listOf(writeMedicalData))
        assertThat(grantedMedicalPermissionsResult)
            .containsExactlyElementsIn(setOf(writeMedicalData))
    }

    @Test
    fun whenPackageNotSupported_wearReturnsOnlySystemPermissions_loadOnlyGrantedPermissions() =
        runTest {
            whenever(deviceInfoUtils.isOnWatch(any())).thenReturn(true)
            whenever(healthPermissionReader.isRationaleIntentDeclared(any())).thenReturn(false)
            whenever(healthPermissionReader.getDeclaredHealthPermissions(any()))
                .thenReturn(
                    listOf(
                        readHeartRatePermission.toString(),
                        readNutritionPermission.toString(),
                        readImmunization.toString(),
                        writeSleepPermission.toString(),
                        writeDistancePermission.toString(),
                        writeMedicalData.toString(),
                        readDataInBackgroundPermission.toString(),
                    )
                )
            getGrantedHealthPermissionsUseCase.updateData(
                TEST_APP_PACKAGE_NAME,
                listOf(
                    readHeartRatePermission.toString(),
                    writeDistancePermission.toString(),
                    readImmunization.toString(),
                    readDataInBackgroundPermission.toString(),
                ),
            )
            val fitnessPermissionsObserver = TestObserver<List<FitnessPermission>>()
            val grantedFitnessPermissionsObserver = TestObserver<Set<FitnessPermission>>()
            val medicalPermissionsObserver = TestObserver<List<MedicalPermission>>()
            val grantedMedicalPermissionsObserver = TestObserver<Set<MedicalPermission>>()
            appPermissionViewModel.fitnessPermissions.observeForever(fitnessPermissionsObserver)
            appPermissionViewModel.grantedFitnessPermissions.observeForever(
                grantedFitnessPermissionsObserver
            )
            appPermissionViewModel.medicalPermissions.observeForever(medicalPermissionsObserver)
            appPermissionViewModel.grantedMedicalPermissions.observeForever(
                grantedMedicalPermissionsObserver
            )
            val grantedAdditionalPermissionsObserver = TestObserver<Set<AdditionalPermission>>()
            appPermissionViewModel.grantedAdditionalPermissions.observeForever(
                grantedAdditionalPermissionsObserver
            )

            appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
            advanceUntilIdle()

            val fitnessPermissionResult = fitnessPermissionsObserver.getLastValue()
            val grantedFitnessPermissionsResult = grantedFitnessPermissionsObserver.getLastValue()
            val medicalPermissionResult = medicalPermissionsObserver.getLastValue()
            val grantedMedicalPermissionsResult = grantedMedicalPermissionsObserver.getLastValue()
            val grantedAdditionalPermissionsResult =
                grantedAdditionalPermissionsObserver.getLastValue()
            assertThat(fitnessPermissionResult)
                .containsExactlyElementsIn(listOf(readHeartRatePermission))
            assertThat(grantedFitnessPermissionsResult)
                .containsExactlyElementsIn(setOf(readHeartRatePermission))
            assertThat(grantedAdditionalPermissionsResult)
                .containsExactlyElementsIn(listOf(readDataInBackgroundPermission))
            assertThat(medicalPermissionResult)
                .containsExactlyElementsIn(listOf<MedicalPermission>())
            assertThat(grantedMedicalPermissionsResult)
                .containsExactlyElementsIn(setOf<MedicalPermission>())
        }

    @Test
    fun updateFitnessPermissions_grant_whenSuccessful_returnsTrue() = runTest {
        val grantedPermissionsObserver = TestObserver<Set<FitnessPermission>>()
        val readExercisePermission =
            FitnessPermission(FitnessPermissionType.EXERCISE, PermissionsAccessType.READ)
        appPermissionViewModel.grantedFitnessPermissions.observeForever(grantedPermissionsObserver)

        val result =
            appPermissionViewModel.updatePermission(
                TEST_APP_PACKAGE_NAME,
                readExercisePermission,
                true,
            )
        advanceUntilIdle()

        assertThat(grantedPermissionsObserver.getLastValue())
            .containsExactlyElementsIn(setOf(readExercisePermission))
        assertThat(result).isTrue()
    }

    @Test
    fun updateFitnessPermissions_grant_whenUnsuccessful_returnsFalse() = runTest {
        val grantedPermissionsObserver = TestObserver<Set<FitnessPermission>>()
        val readExercisePermission =
            FitnessPermission(FitnessPermissionType.EXERCISE, PermissionsAccessType.READ)
        whenever(grantPermissionsUseCase.invoke(any(), any())).thenThrow(RuntimeException("Error!"))
        appPermissionViewModel.grantedFitnessPermissions.observeForever(grantedPermissionsObserver)

        val result =
            appPermissionViewModel.updatePermission(
                TEST_APP_PACKAGE_NAME,
                readExercisePermission,
                true,
            )
        advanceUntilIdle()

        assertThat(grantedPermissionsObserver.getLastValue()).isEmpty()
        assertThat(result).isFalse()
    }

    @Test
    fun updateFitnessPermissions_deny_whenSuccessful_returnsTrue() = runTest {
        setupDeclaredAndGrantedFitnessPermissions()
        val appPermissionsObserver = TestObserver<List<FitnessPermission>>()
        val grantedPermissionsObserver = TestObserver<Set<FitnessPermission>>()
        appPermissionViewModel.fitnessPermissions.observeForever(appPermissionsObserver)
        appPermissionViewModel.grantedFitnessPermissions.observeForever(grantedPermissionsObserver)

        appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        val appPermissionsResult = appPermissionsObserver.getLastValue()
        val grantedPermissionsResult = grantedPermissionsObserver.getLastValue()

        assertThat(appPermissionsResult)
            .containsExactlyElementsIn(
                listOf(
                    readExercisePermission,
                    readNutritionPermission,
                    writeSleepPermission,
                    writeDistancePermission,
                )
            )
        assertThat(grantedPermissionsResult)
            .containsExactlyElementsIn(setOf(readExercisePermission, writeDistancePermission))

        val result =
            appPermissionViewModel.updatePermission(
                TEST_APP_PACKAGE_NAME,
                writeDistancePermission,
                false,
            )
        advanceUntilIdle()

        assertThat(grantedPermissionsObserver.getLastValue())
            .containsExactlyElementsIn(setOf(readExercisePermission))
        assertThat(result).isTrue()
    }

    @Test
    fun updatePermissions_denyLastReadFitnessPermission_noReadMedicalGranted_updatesAdditionalPermissions() =
        runTest {
            whenever(healthPermissionReader.isRationaleIntentDeclared(any())).thenReturn(true)
            whenever(healthPermissionReader.getDeclaredHealthPermissions(any()))
                .thenReturn(
                    listOf(
                        readExercisePermission.toString(),
                        readNutritionPermission.toString(),
                        readExerciseRoutesPermission.toString(),
                        readDataInBackgroundPermission.toString(),
                        readHistoryDataPermission.toString(),
                        writeSleepPermission.toString(),
                        writeDistancePermission.toString(),
                        readImmunization.toString(),
                        writeMedicalData.toString(),
                    )
                )
            getGrantedHealthPermissionsUseCase.updateData(
                TEST_APP_PACKAGE_NAME,
                listOf(
                    readNutritionPermission.toString(),
                    writeDistancePermission.toString(),
                    readExerciseRoutesPermission.additionalPermission,
                    readHistoryDataPermission.additionalPermission,
                    readDataInBackgroundPermission.additionalPermission,
                    writeMedicalData.toString(),
                ),
            )

            loadExerciseRoutePermissionUseCase.setExerciseRouteState(
                ExerciseRouteState(
                    exerciseRoutePermissionState = PermissionUiState.ALWAYS_ALLOW,
                    exercisePermissionState = PermissionUiState.ALWAYS_ALLOW,
                )
            )

            val appPermissionsObserver = TestObserver<List<FitnessPermission>>()
            val grantedPermissionsObserver = TestObserver<Set<FitnessPermission>>()
            appPermissionViewModel.fitnessPermissions.observeForever(appPermissionsObserver)
            appPermissionViewModel.grantedFitnessPermissions.observeForever(
                grantedPermissionsObserver
            )
            val grantedAdditionalPermissionsObserver = TestObserver<Set<AdditionalPermission>>()
            appPermissionViewModel.grantedAdditionalPermissions.observeForever(
                grantedAdditionalPermissionsObserver
            )

            appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
            advanceUntilIdle()

            val appPermissionsResult = appPermissionsObserver.getLastValue()
            val grantedPermissionsResult = grantedPermissionsObserver.getLastValue()

            assertThat(appPermissionsResult)
                .containsExactlyElementsIn(
                    listOf(
                        readExercisePermission,
                        readNutritionPermission,
                        writeSleepPermission,
                        writeDistancePermission,
                    )
                )
            assertThat(grantedPermissionsResult)
                .containsExactlyElementsIn(setOf(readNutritionPermission, writeDistancePermission))

            val result =
                appPermissionViewModel.updatePermission(
                    TEST_APP_PACKAGE_NAME,
                    readNutritionPermission,
                    false,
                )
            advanceUntilIdle()

            assertThat(result).isTrue()
            assertThat(grantedPermissionsObserver.getLastValue())
                .containsExactlyElementsIn(setOf(writeDistancePermission))
            assertThat(grantedAdditionalPermissionsObserver.getLastValue()).isEmpty()
            verify(revokePermissionStatusUseCase)
                .invoke(TEST_APP_PACKAGE_NAME, readExerciseRoutesPermission.additionalPermission)
            verify(revokePermissionStatusUseCase)
                .invoke(TEST_APP_PACKAGE_NAME, readDataInBackgroundPermission.additionalPermission)
            verify(revokePermissionStatusUseCase)
                .invoke(TEST_APP_PACKAGE_NAME, readHistoryDataPermission.additionalPermission)
        }

    @Test
    fun updatePermissions_denyLastReadFitnessPermission_noReadMedicalGranted_skipsERIfAlreadyAskEveryTime() =
        runTest {
            whenever(healthPermissionReader.isRationaleIntentDeclared(any())).thenReturn(true)
            whenever(healthPermissionReader.getDeclaredHealthPermissions(any()))
                .thenReturn(
                    listOf(
                        readExercisePermission.toString(),
                        readNutritionPermission.toString(),
                        readExerciseRoutesPermission.toString(),
                        readDataInBackgroundPermission.toString(),
                        readHistoryDataPermission.toString(),
                        writeSleepPermission.toString(),
                        writeDistancePermission.toString(),
                        writeMedicalData.toString(),
                    )
                )
            getGrantedHealthPermissionsUseCase.updateData(
                TEST_APP_PACKAGE_NAME,
                listOf(
                    readExercisePermission.toString(),
                    writeDistancePermission.toString(),
                    readHistoryDataPermission.additionalPermission,
                    readDataInBackgroundPermission.additionalPermission,
                    writeMedicalData.toString(),
                ),
            )

            loadExerciseRoutePermissionUseCase.setExerciseRouteState(
                ExerciseRouteState(
                    exerciseRoutePermissionState = PermissionUiState.ASK_EVERY_TIME,
                    exercisePermissionState = PermissionUiState.ALWAYS_ALLOW,
                )
            )

            val appPermissionsObserver = TestObserver<List<FitnessPermission>>()
            val grantedPermissionsObserver = TestObserver<Set<FitnessPermission>>()
            appPermissionViewModel.fitnessPermissions.observeForever(appPermissionsObserver)
            appPermissionViewModel.grantedFitnessPermissions.observeForever(
                grantedPermissionsObserver
            )

            appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
            advanceUntilIdle()

            val appPermissionsResult = appPermissionsObserver.getLastValue()
            val grantedPermissionsResult = grantedPermissionsObserver.getLastValue()

            assertThat(appPermissionsResult)
                .containsExactlyElementsIn(
                    listOf(
                        readExercisePermission,
                        readNutritionPermission,
                        writeSleepPermission,
                        writeDistancePermission,
                    )
                )
            assertThat(grantedPermissionsResult)
                .containsExactlyElementsIn(setOf(readExercisePermission, writeDistancePermission))

            val result =
                appPermissionViewModel.updatePermission(
                    TEST_APP_PACKAGE_NAME,
                    readExercisePermission,
                    false,
                )
            advanceUntilIdle()

            assertThat(grantedPermissionsObserver.getLastValue())
                .containsExactlyElementsIn(setOf(writeDistancePermission))
            assertThat(result).isTrue()
            verify(revokePermissionStatusUseCase, times(0))
                .invoke(TEST_APP_PACKAGE_NAME, readExerciseRoutesPermission.additionalPermission)
            verify(revokePermissionStatusUseCase)
                .invoke(TEST_APP_PACKAGE_NAME, readDataInBackgroundPermission.additionalPermission)
            verify(revokePermissionStatusUseCase)
                .invoke(TEST_APP_PACKAGE_NAME, readHistoryDataPermission.additionalPermission)
        }

    @Test
    fun updatePermissions_denyLastReadFitnessPermission_withMedicalGranted_doesNotUpdateAdditionalPermissions() =
        runTest {
            whenever(healthPermissionReader.isRationaleIntentDeclared(any())).thenReturn(true)
            whenever(healthPermissionReader.getDeclaredHealthPermissions(any()))
                .thenReturn(
                    listOf(
                        readExercisePermission.toString(),
                        readNutritionPermission.toString(),
                        readExerciseRoutesPermission.toString(),
                        readDataInBackgroundPermission.toString(),
                        readHistoryDataPermission.toString(),
                        writeSleepPermission.toString(),
                        writeDistancePermission.toString(),
                        readImmunization.toString(),
                        writeMedicalData.toString(),
                    )
                )
            getGrantedHealthPermissionsUseCase.updateData(
                TEST_APP_PACKAGE_NAME,
                listOf(
                    readNutritionPermission.toString(),
                    writeDistancePermission.toString(),
                    readExerciseRoutesPermission.additionalPermission,
                    readHistoryDataPermission.additionalPermission,
                    readDataInBackgroundPermission.additionalPermission,
                    readImmunization.toString(),
                    writeMedicalData.toString(),
                ),
            )

            loadExerciseRoutePermissionUseCase.setExerciseRouteState(
                ExerciseRouteState(
                    exerciseRoutePermissionState = PermissionUiState.ALWAYS_ALLOW,
                    exercisePermissionState = PermissionUiState.ALWAYS_ALLOW,
                )
            )

            val appPermissionsObserver = TestObserver<List<FitnessPermission>>()
            val grantedPermissionsObserver = TestObserver<Set<FitnessPermission>>()
            appPermissionViewModel.fitnessPermissions.observeForever(appPermissionsObserver)
            appPermissionViewModel.grantedFitnessPermissions.observeForever(
                grantedPermissionsObserver
            )

            appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
            advanceUntilIdle()

            val appPermissionsResult = appPermissionsObserver.getLastValue()
            val grantedPermissionsResult = grantedPermissionsObserver.getLastValue()

            assertThat(appPermissionsResult)
                .containsExactlyElementsIn(
                    listOf(
                        readExercisePermission,
                        readNutritionPermission,
                        writeSleepPermission,
                        writeDistancePermission,
                    )
                )
            assertThat(grantedPermissionsResult)
                .containsExactlyElementsIn(setOf(readNutritionPermission, writeDistancePermission))

            val result =
                appPermissionViewModel.updatePermission(
                    TEST_APP_PACKAGE_NAME,
                    readNutritionPermission,
                    false,
                )
            advanceUntilIdle()

            assertThat(grantedPermissionsObserver.getLastValue())
                .containsExactlyElementsIn(setOf(writeDistancePermission))
            assertThat(result).isTrue()
            verify(revokePermissionStatusUseCase, never())
                .invoke(TEST_APP_PACKAGE_NAME, readExerciseRoutesPermission.additionalPermission)
            verify(revokePermissionStatusUseCase, never())
                .invoke(TEST_APP_PACKAGE_NAME, readDataInBackgroundPermission.additionalPermission)
            verify(revokePermissionStatusUseCase, never())
                .invoke(TEST_APP_PACKAGE_NAME, readHistoryDataPermission.additionalPermission)
        }

    @Test
    fun updatePermissions_denyLastReadMedicalPermission_noReadFitnessGranted_updatesAdditionalPermissions() =
        runTest {
            whenever(healthPermissionReader.isRationaleIntentDeclared(any())).thenReturn(true)
            whenever(healthPermissionReader.getDeclaredHealthPermissions(any()))
                .thenReturn(
                    listOf(
                        readExercisePermission.toString(),
                        readNutritionPermission.toString(),
                        readExerciseRoutesPermission.toString(),
                        readDataInBackgroundPermission.toString(),
                        readHistoryDataPermission.toString(),
                        writeSleepPermission.toString(),
                        writeDistancePermission.toString(),
                        readImmunization.toString(),
                        writeMedicalData.toString(),
                    )
                )
            getGrantedHealthPermissionsUseCase.updateData(
                TEST_APP_PACKAGE_NAME,
                listOf(
                    writeSleepPermission.toString(),
                    readHistoryDataPermission.additionalPermission,
                    readDataInBackgroundPermission.additionalPermission,
                    readImmunization.toString(),
                    writeMedicalData.toString(),
                ),
            )

            loadExerciseRoutePermissionUseCase.setExerciseRouteState(
                ExerciseRouteState(
                    exerciseRoutePermissionState = PermissionUiState.ASK_EVERY_TIME,
                    exercisePermissionState = PermissionUiState.ASK_EVERY_TIME,
                )
            )

            val appPermissionsObserver = TestObserver<List<MedicalPermission>>()
            val grantedPermissionsObserver = TestObserver<Set<MedicalPermission>>()
            appPermissionViewModel.medicalPermissions.observeForever(appPermissionsObserver)
            appPermissionViewModel.grantedMedicalPermissions.observeForever(
                grantedPermissionsObserver
            )

            appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
            advanceUntilIdle()

            val appPermissionsResult = appPermissionsObserver.getLastValue()
            val grantedPermissionsResult = grantedPermissionsObserver.getLastValue()

            assertThat(appPermissionsResult)
                .containsExactlyElementsIn(listOf(readImmunization, writeMedicalData))
            assertThat(grantedPermissionsResult)
                .containsExactlyElementsIn(setOf(readImmunization, writeMedicalData))

            val result =
                appPermissionViewModel.updatePermission(
                    TEST_APP_PACKAGE_NAME,
                    readImmunization,
                    false,
                )
            advanceUntilIdle()

            assertThat(grantedPermissionsObserver.getLastValue())
                .containsExactlyElementsIn(setOf(writeMedicalData))
            assertThat(result).isTrue()
            verify(revokePermissionStatusUseCase, never())
                .invoke(TEST_APP_PACKAGE_NAME, readExerciseRoutesPermission.additionalPermission)
            verify(revokePermissionStatusUseCase)
                .invoke(TEST_APP_PACKAGE_NAME, readDataInBackgroundPermission.additionalPermission)
            verify(revokePermissionStatusUseCase)
                .invoke(TEST_APP_PACKAGE_NAME, readHistoryDataPermission.additionalPermission)
        }

    @Test
    fun updatePermissions_denyLastReadMedicalPermission_withFitnessGranted_doesNotUpdateAdditionalPermissions() =
        runTest {
            whenever(healthPermissionReader.isRationaleIntentDeclared(any())).thenReturn(true)
            whenever(healthPermissionReader.getDeclaredHealthPermissions(any()))
                .thenReturn(
                    listOf(
                        readExercisePermission.toString(),
                        readNutritionPermission.toString(),
                        readExerciseRoutesPermission.toString(),
                        readDataInBackgroundPermission.toString(),
                        readHistoryDataPermission.toString(),
                        writeSleepPermission.toString(),
                        writeDistancePermission.toString(),
                        readImmunization.toString(),
                        writeMedicalData.toString(),
                    )
                )
            getGrantedHealthPermissionsUseCase.updateData(
                TEST_APP_PACKAGE_NAME,
                listOf(
                    readNutritionPermission.toString(),
                    writeSleepPermission.toString(),
                    readHistoryDataPermission.additionalPermission,
                    readDataInBackgroundPermission.additionalPermission,
                    readImmunization.toString(),
                    writeMedicalData.toString(),
                ),
            )

            loadExerciseRoutePermissionUseCase.setExerciseRouteState(
                ExerciseRouteState(
                    exerciseRoutePermissionState = PermissionUiState.ASK_EVERY_TIME,
                    exercisePermissionState = PermissionUiState.ASK_EVERY_TIME,
                )
            )

            val appPermissionsObserver = TestObserver<List<MedicalPermission>>()
            val grantedPermissionsObserver = TestObserver<Set<MedicalPermission>>()
            appPermissionViewModel.medicalPermissions.observeForever(appPermissionsObserver)
            appPermissionViewModel.grantedMedicalPermissions.observeForever(
                grantedPermissionsObserver
            )

            appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
            advanceUntilIdle()

            val appPermissionsResult = appPermissionsObserver.getLastValue()
            val grantedPermissionsResult = grantedPermissionsObserver.getLastValue()

            assertThat(appPermissionsResult)
                .containsExactlyElementsIn(listOf(readImmunization, writeMedicalData))
            assertThat(grantedPermissionsResult)
                .containsExactlyElementsIn(setOf(readImmunization, writeMedicalData))

            val result =
                appPermissionViewModel.updatePermission(
                    TEST_APP_PACKAGE_NAME,
                    readImmunization,
                    false,
                )
            advanceUntilIdle()

            assertThat(grantedPermissionsObserver.getLastValue())
                .containsExactlyElementsIn(setOf(writeMedicalData))
            assertThat(result).isTrue()
            verify(revokePermissionStatusUseCase, never())
                .invoke(TEST_APP_PACKAGE_NAME, readExerciseRoutesPermission.additionalPermission)
            verify(revokePermissionStatusUseCase, never())
                .invoke(TEST_APP_PACKAGE_NAME, readDataInBackgroundPermission.additionalPermission)
            verify(revokePermissionStatusUseCase, never())
                .invoke(TEST_APP_PACKAGE_NAME, readHistoryDataPermission.additionalPermission)
        }

    @Test
    fun updatePermissions_deny_whenUnsuccessful_returnsFalse() = runTest {
        setupDeclaredAndGrantedFitnessPermissions()

        val appPermissionsObserver = TestObserver<List<FitnessPermission>>()
        val grantedPermissionsObserver = TestObserver<Set<FitnessPermission>>()
        appPermissionViewModel.fitnessPermissions.observeForever(appPermissionsObserver)
        appPermissionViewModel.grantedFitnessPermissions.observeForever(grantedPermissionsObserver)

        appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        val appPermissionsResult = appPermissionsObserver.getLastValue()
        val grantedPermissionsResult = grantedPermissionsObserver.getLastValue()

        assertThat(appPermissionsResult)
            .containsExactlyElementsIn(
                listOf(
                    readExercisePermission,
                    readNutritionPermission,
                    writeSleepPermission,
                    writeDistancePermission,
                )
            )
        assertThat(grantedPermissionsResult)
            .containsExactlyElementsIn(setOf(readExercisePermission, writeDistancePermission))

        whenever(grantPermissionsUseCase.invoke(any(), any())).thenThrow(RuntimeException("Error!"))
        val result =
            appPermissionViewModel.updatePermission(
                TEST_APP_PACKAGE_NAME,
                readExercisePermission,
                true,
            )
        advanceUntilIdle()

        assertThat(grantedPermissionsObserver.getLastValue())
            .containsExactlyElementsIn(setOf(readExercisePermission, writeDistancePermission))
        assertThat(result).isFalse()
    }

    @Test
    fun grantAllFitnessPermissions_whenSuccessful_returnsTrue() = runTest {
        setupDeclaredAndGrantedFitnessPermissions()

        val appPermissionsObserver = TestObserver<List<FitnessPermission>>()
        val grantedPermissionsObserver = TestObserver<Set<FitnessPermission>>()
        appPermissionViewModel.fitnessPermissions.observeForever(appPermissionsObserver)
        appPermissionViewModel.grantedFitnessPermissions.observeForever(grantedPermissionsObserver)

        appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        val appPermissionsResult = appPermissionsObserver.getLastValue()
        val grantedPermissionsResult = grantedPermissionsObserver.getLastValue()

        assertThat(appPermissionsResult)
            .containsExactlyElementsIn(
                listOf(
                    readExercisePermission,
                    readNutritionPermission,
                    writeSleepPermission,
                    writeDistancePermission,
                )
            )
        assertThat(grantedPermissionsResult)
            .containsExactlyElementsIn(setOf(readExercisePermission, writeDistancePermission))

        val result = appPermissionViewModel.grantAllFitnessPermissions(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        assertThat(grantedPermissionsObserver.getLastValue())
            .containsExactlyElementsIn(
                setOf(
                    readExercisePermission,
                    readNutritionPermission,
                    writeSleepPermission,
                    writeDistancePermission,
                )
            )
        assertThat(result).isTrue()
    }

    @Test
    fun grantAllFitnessPermissions_whenSuccessful_noChangeInMedicalPermissions() = runTest {
        setupDeclaredAndGrantedFitnessAndMedicalPermissions()
        val medicalPermissionsObserver = TestObserver<List<MedicalPermission>>()
        val grantedMedicalPermissionsObserver = TestObserver<Set<MedicalPermission>>()
        appPermissionViewModel.medicalPermissions.observeForever(medicalPermissionsObserver)
        appPermissionViewModel.grantedMedicalPermissions.observeForever(
            grantedMedicalPermissionsObserver
        )

        appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        val medicalPermissionsResult = medicalPermissionsObserver.getLastValue()
        val grantedMedicalPermissionsResult = grantedMedicalPermissionsObserver.getLastValue()

        assertThat(medicalPermissionsResult)
            .containsExactlyElementsIn(listOf(readImmunization, writeMedicalData))
        assertThat(grantedMedicalPermissionsResult)
            .containsExactlyElementsIn(setOf(readImmunization))

        val result = appPermissionViewModel.grantAllFitnessPermissions(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        assertThat(grantedMedicalPermissionsObserver.getLastValue())
            .containsExactlyElementsIn(setOf(readImmunization))
        assertThat(result).isTrue()
    }

    @Test
    fun grantAllFitnessPermissions_whenUnsuccessful_returnsFalse() = runTest {
        setupDeclaredAndGrantedFitnessPermissions()
        val appPermissionsObserver = TestObserver<List<FitnessPermission>>()
        val grantedPermissionsObserver = TestObserver<Set<FitnessPermission>>()
        appPermissionViewModel.fitnessPermissions.observeForever(appPermissionsObserver)
        appPermissionViewModel.grantedFitnessPermissions.observeForever(grantedPermissionsObserver)

        appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        val appPermissionsResult = appPermissionsObserver.getLastValue()
        val grantedPermissionsResult = grantedPermissionsObserver.getLastValue()
        assertThat(appPermissionsResult)
            .containsExactlyElementsIn(
                listOf(
                    readExercisePermission,
                    readNutritionPermission,
                    writeSleepPermission,
                    writeDistancePermission,
                )
            )
        assertThat(grantedPermissionsResult)
            .containsExactlyElementsIn(setOf(readExercisePermission, writeDistancePermission))

        whenever(grantPermissionsUseCase.invoke(any(), any())).thenThrow(RuntimeException("Error!"))
        val result = appPermissionViewModel.grantAllFitnessPermissions(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        assertThat(grantedPermissionsObserver.getLastValue())
            .containsExactlyElementsIn(setOf(readExercisePermission, writeDistancePermission))
        assertThat(result).isFalse()
    }

    @Test
    fun grantAllFitnessPermissions_whenUnsuccessful_noChangeInMedicalPermission() = runTest {
        setupDeclaredAndGrantedFitnessAndMedicalPermissions()
        val medicalPermissionsObserver = TestObserver<List<MedicalPermission>>()
        val grantedMedicalPermissionsObserver = TestObserver<Set<MedicalPermission>>()
        appPermissionViewModel.medicalPermissions.observeForever(medicalPermissionsObserver)
        appPermissionViewModel.grantedMedicalPermissions.observeForever(
            grantedMedicalPermissionsObserver
        )

        appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        val medicalPermissionsResult = medicalPermissionsObserver.getLastValue()
        val grantedMedicalPermissionsResult = grantedMedicalPermissionsObserver.getLastValue()
        assertThat(medicalPermissionsResult)
            .containsExactlyElementsIn(listOf(readImmunization, writeMedicalData))
        assertThat(grantedMedicalPermissionsResult)
            .containsExactlyElementsIn(setOf(readImmunization))

        whenever(grantPermissionsUseCase.invoke(any(), any())).thenThrow(RuntimeException("Error!"))
        val result = appPermissionViewModel.grantAllFitnessPermissions(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        assertThat(grantedMedicalPermissionsObserver.getLastValue())
            .containsExactlyElementsIn(setOf(readImmunization))
        assertThat(result).isFalse()
    }

    @Test
    fun grantAllMedicalPermissions_whenSuccessful_returnsTrue() = runTest {
        setupDeclaredAndGrantedFitnessAndMedicalPermissions()
        val fitnessPermissionsObserver = TestObserver<List<FitnessPermission>>()
        val grantedFitnessPermissionsObserver = TestObserver<Set<FitnessPermission>>()
        val medicalPermissionsObserver = TestObserver<List<MedicalPermission>>()
        val grantedMedicalPermissionsObserver = TestObserver<Set<MedicalPermission>>()
        appPermissionViewModel.fitnessPermissions.observeForever(fitnessPermissionsObserver)
        appPermissionViewModel.grantedFitnessPermissions.observeForever(
            grantedFitnessPermissionsObserver
        )
        appPermissionViewModel.medicalPermissions.observeForever(medicalPermissionsObserver)
        appPermissionViewModel.grantedMedicalPermissions.observeForever(
            grantedMedicalPermissionsObserver
        )

        appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        val fitnessPermissionsResult = fitnessPermissionsObserver.getLastValue()
        val grantedFitnessPermissionsResult = grantedFitnessPermissionsObserver.getLastValue()
        val medicalPermissionsResult = medicalPermissionsObserver.getLastValue()
        val grantedMedicalPermissionsResult = grantedMedicalPermissionsObserver.getLastValue()

        assertThat(fitnessPermissionsResult)
            .containsExactlyElementsIn(
                listOf(
                    readExercisePermission,
                    readNutritionPermission,
                    writeSleepPermission,
                    writeDistancePermission,
                )
            )
        assertThat(grantedFitnessPermissionsResult)
            .containsExactlyElementsIn(setOf(readExercisePermission))
        assertThat(medicalPermissionsResult)
            .containsExactlyElementsIn(listOf(readImmunization, writeMedicalData))
        assertThat(grantedMedicalPermissionsResult)
            .containsExactlyElementsIn(setOf(readImmunization))

        val result = appPermissionViewModel.grantAllMedicalPermissions(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        assertThat(grantedFitnessPermissionsObserver.getLastValue())
            .containsExactlyElementsIn(setOf(readExercisePermission))
        assertThat(grantedMedicalPermissionsObserver.getLastValue())
            .containsExactlyElementsIn(setOf(readImmunization, writeMedicalData))
        assertThat(result).isTrue()
    }

    @Test
    fun grantAllMedicalPermissions_whenUnsuccessful_returnsFalse() = runTest {
        setupDeclaredAndGrantedFitnessAndMedicalPermissions()
        val fitnessPermissionsObserver = TestObserver<List<FitnessPermission>>()
        val grantedFitnessPermissionsObserver = TestObserver<Set<FitnessPermission>>()
        val medicalPermissionsObserver = TestObserver<List<MedicalPermission>>()
        val grantedMedicalPermissionsObserver = TestObserver<Set<MedicalPermission>>()
        appPermissionViewModel.fitnessPermissions.observeForever(fitnessPermissionsObserver)
        appPermissionViewModel.grantedFitnessPermissions.observeForever(
            grantedFitnessPermissionsObserver
        )
        appPermissionViewModel.medicalPermissions.observeForever(medicalPermissionsObserver)
        appPermissionViewModel.grantedMedicalPermissions.observeForever(
            grantedMedicalPermissionsObserver
        )

        appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        val grantedFitnessPermissionsResult = grantedFitnessPermissionsObserver.getLastValue()
        val medicalPermissionsResult = medicalPermissionsObserver.getLastValue()
        val grantedMedicalPermissionsResult = grantedMedicalPermissionsObserver.getLastValue()

        assertThat(grantedFitnessPermissionsResult)
            .containsExactlyElementsIn(setOf(readExercisePermission))
        assertThat(medicalPermissionsResult)
            .containsExactlyElementsIn(listOf(readImmunization, writeMedicalData))
        assertThat(grantedMedicalPermissionsResult)
            .containsExactlyElementsIn(setOf(readImmunization))

        whenever(grantPermissionsUseCase.invoke(any(), any())).thenThrow(RuntimeException("Error!"))
        val result = appPermissionViewModel.grantAllFitnessPermissions(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        assertThat(grantedFitnessPermissionsResult)
            .containsExactlyElementsIn(setOf(readExercisePermission))
        assertThat(grantedMedicalPermissionsObserver.getLastValue())
            .containsExactlyElementsIn(setOf(readImmunization))
        assertThat(result).isFalse()
    }

    @Test
    @Ignore("b/379884589")
    fun revokeAllPermissions_fitnessOnly_revokesFitness() = runTest {
        setupDeclaredAndGrantedFitnessPermissions()
        val appPermissionsObserver = TestObserver<List<FitnessPermission>>()
        val grantedPermissionsObserver = TestObserver<Set<FitnessPermission>>()
        appPermissionViewModel.fitnessPermissions.observeForever(appPermissionsObserver)
        appPermissionViewModel.grantedFitnessPermissions.observeForever(grantedPermissionsObserver)

        appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        val appPermissionsResult = appPermissionsObserver.getLastValue()
        val grantedPermissionsResult = grantedPermissionsObserver.getLastValue()
        assertThat(appPermissionsResult)
            .containsExactlyElementsIn(
                listOf(
                    readExercisePermission,
                    readNutritionPermission,
                    writeSleepPermission,
                    writeDistancePermission,
                )
            )
        assertThat(grantedPermissionsResult)
            .containsExactlyElementsIn(setOf(readExercisePermission, writeDistancePermission))

        val result = appPermissionViewModel.revokeAllHealthPermissions(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()
        assertThat(grantedPermissionsObserver.getLastValue()).isEmpty()
        assertThat(result).isTrue()
    }

    @Test
    @Ignore("b/379884589")
    fun revokeAllPermissions_fitnessAndAdditional_revokesFitnessAndAdditional() = runTest {
        whenever(healthPermissionReader.isRationaleIntentDeclared(any())).thenReturn(true)
        whenever(healthPermissionReader.getDeclaredHealthPermissions(any()))
            .thenReturn(
                listOf(
                    readExercisePermission.toString(),
                    readNutritionPermission.toString(),
                    readExerciseRoutesPermission.toString(),
                    readDataInBackgroundPermission.toString(),
                    readHistoryDataPermission.toString(),
                    writeSleepPermission.toString(),
                    writeDistancePermission.toString(),
                )
            )
        getGrantedHealthPermissionsUseCase.updateData(
            TEST_APP_PACKAGE_NAME,
            listOf(
                writeSleepPermission.toString(),
                readHistoryDataPermission.additionalPermission,
                readDataInBackgroundPermission.additionalPermission,
            ),
        )

        loadExerciseRoutePermissionUseCase.setExerciseRouteState(
            ExerciseRouteState(
                exerciseRoutePermissionState = PermissionUiState.ASK_EVERY_TIME,
                exercisePermissionState = PermissionUiState.ASK_EVERY_TIME,
            )
        )

        val appPermissionsObserver = TestObserver<List<FitnessPermission>>()
        val grantedAdditionalPermissionsObserver = TestObserver<Set<AdditionalPermission>>()
        val grantedFitnessPermissionsObserver = TestObserver<Set<FitnessPermission>>()
        appPermissionViewModel.fitnessPermissions.observeForever(appPermissionsObserver)
        appPermissionViewModel.grantedAdditionalPermissions.observeForever(
            grantedAdditionalPermissionsObserver
        )
        appPermissionViewModel.grantedFitnessPermissions.observeForever(
            grantedFitnessPermissionsObserver
        )

        appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        val appPermissionsResult = appPermissionsObserver.getLastValue()
        val grantedAdditionalPermissionsResult = grantedAdditionalPermissionsObserver.getLastValue()
        val grantedFitnessPermissionsResult = grantedFitnessPermissionsObserver.getLastValue()
        assertThat(appPermissionsResult)
            .containsExactlyElementsIn(
                listOf(
                    readExercisePermission,
                    readNutritionPermission,
                    writeSleepPermission,
                    writeDistancePermission,
                )
            )
        assertThat(grantedAdditionalPermissionsResult)
            .containsExactlyElementsIn(
                setOf(readDataInBackgroundPermission, readHistoryDataPermission)
            )
        assertThat(grantedFitnessPermissionsResult)
            .containsExactlyElementsIn(setOf(writeSleepPermission))

        val result = appPermissionViewModel.revokeAllHealthPermissions(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()
        assertThat(grantedFitnessPermissionsObserver.getLastValue()).isEmpty()
        assertThat(grantedAdditionalPermissionsObserver.getLastValue()).isEmpty()
        assertThat(result).isTrue()
    }

    @Test
    @Ignore("b/379884589")
    fun revokeAllPermissions_fitnessAndMedical_revokesFitnessAndMedical() = runTest {
        whenever(healthPermissionReader.isRationaleIntentDeclared(any())).thenReturn(true)
        whenever(healthPermissionReader.getDeclaredHealthPermissions(any()))
            .thenReturn(
                listOf(
                    readExercisePermission.toString(),
                    readNutritionPermission.toString(),
                    writeSleepPermission.toString(),
                    writeDistancePermission.toString(),
                    readImmunization.toString(),
                    writeMedicalData.toString(),
                )
            )
        getGrantedHealthPermissionsUseCase.updateData(
            TEST_APP_PACKAGE_NAME,
            listOf(
                readExercisePermission.toString(),
                writeSleepPermission.toString(),
                readImmunization.toString(),
                writeMedicalData.toString(),
            ),
        )

        val fitnessPermissionsObserver = TestObserver<List<FitnessPermission>>()
        val medicalPermissionsObserver = TestObserver<List<MedicalPermission>>()
        val grantedAdditionalPermissionsObserver = TestObserver<Set<AdditionalPermission>>()
        val grantedFitnessPermissionsObserver = TestObserver<Set<FitnessPermission>>()
        val grantedMedicalPermissionsObserver = TestObserver<Set<MedicalPermission>>()
        appPermissionViewModel.fitnessPermissions.observeForever(fitnessPermissionsObserver)
        appPermissionViewModel.medicalPermissions.observeForever(medicalPermissionsObserver)
        appPermissionViewModel.grantedAdditionalPermissions.observeForever(
            grantedAdditionalPermissionsObserver
        )
        appPermissionViewModel.grantedFitnessPermissions.observeForever(
            grantedFitnessPermissionsObserver
        )
        appPermissionViewModel.grantedMedicalPermissions.observeForever(
            grantedMedicalPermissionsObserver
        )

        appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        val fitnessPermissionsResult = fitnessPermissionsObserver.getLastValue()
        val medicalPermissionsResult = medicalPermissionsObserver.getLastValue()
        val grantedAdditionalPermissionsResult = grantedAdditionalPermissionsObserver.getLastValue()
        val grantedFitnessPermissionsResult = grantedFitnessPermissionsObserver.getLastValue()
        val grantedMedicalPermissionsResult = grantedMedicalPermissionsObserver.getLastValue()

        assertThat(fitnessPermissionsResult)
            .containsExactlyElementsIn(
                listOf(
                    readExercisePermission,
                    readNutritionPermission,
                    writeSleepPermission,
                    writeDistancePermission,
                )
            )
        assertThat(medicalPermissionsResult)
            .containsExactlyElementsIn(listOf(readImmunization, writeMedicalData))

        assertThat(grantedAdditionalPermissionsResult).isEmpty()

        assertThat(grantedFitnessPermissionsResult)
            .containsExactlyElementsIn(setOf(readExercisePermission, writeSleepPermission))
        assertThat(grantedMedicalPermissionsResult)
            .containsExactlyElementsIn(setOf(readImmunization, writeMedicalData))

        val result = appPermissionViewModel.revokeAllHealthPermissions(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()
        assertThat(grantedFitnessPermissionsObserver.getLastValue()).isEmpty()
        assertThat(grantedMedicalPermissionsObserver.getLastValue()).isEmpty()
        assertThat(grantedAdditionalPermissionsObserver.getLastValue()).isEmpty()
        assertThat(result).isTrue()
    }

    @Test
    @Ignore("b/379884589")
    fun revokeAllPermissions_fitnessAndMedicalAndAdditional_revokesFitnessAndMedicalAndAdditional() =
        runTest {
            whenever(healthPermissionReader.isRationaleIntentDeclared(any())).thenReturn(true)
            whenever(healthPermissionReader.getDeclaredHealthPermissions(any()))
                .thenReturn(
                    listOf(
                        readExercisePermission.toString(),
                        readNutritionPermission.toString(),
                        readExerciseRoutesPermission.toString(),
                        readDataInBackgroundPermission.toString(),
                        readHistoryDataPermission.toString(),
                        writeSleepPermission.toString(),
                        writeDistancePermission.toString(),
                        readImmunization.toString(),
                        writeMedicalData.toString(),
                    )
                )
            getGrantedHealthPermissionsUseCase.updateData(
                TEST_APP_PACKAGE_NAME,
                listOf(
                    readExercisePermission.toString(),
                    writeSleepPermission.toString(),
                    readExerciseRoutesPermission.toString(),
                    readDataInBackgroundPermission.toString(),
                    readHistoryDataPermission.toString(),
                    readImmunization.toString(),
                    writeMedicalData.toString(),
                ),
            )

            val fitnessPermissionsObserver = TestObserver<List<FitnessPermission>>()
            val medicalPermissionsObserver = TestObserver<List<MedicalPermission>>()
            val grantedAdditionalPermissionsObserver = TestObserver<Set<AdditionalPermission>>()
            val grantedFitnessPermissionsObserver = TestObserver<Set<FitnessPermission>>()
            val grantedMedicalPermissionsObserver = TestObserver<Set<MedicalPermission>>()
            appPermissionViewModel.fitnessPermissions.observeForever(fitnessPermissionsObserver)
            appPermissionViewModel.medicalPermissions.observeForever(medicalPermissionsObserver)
            appPermissionViewModel.grantedAdditionalPermissions.observeForever(
                grantedAdditionalPermissionsObserver
            )
            appPermissionViewModel.grantedFitnessPermissions.observeForever(
                grantedFitnessPermissionsObserver
            )
            appPermissionViewModel.grantedMedicalPermissions.observeForever(
                grantedMedicalPermissionsObserver
            )

            appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
            advanceUntilIdle()

            val fitnessPermissionsResult = fitnessPermissionsObserver.getLastValue()
            val medicalPermissionsResult = medicalPermissionsObserver.getLastValue()
            val grantedAdditionalPermissionsResult =
                grantedAdditionalPermissionsObserver.getLastValue()
            val grantedFitnessPermissionsResult = grantedFitnessPermissionsObserver.getLastValue()
            val grantedMedicalPermissionsResult = grantedMedicalPermissionsObserver.getLastValue()

            assertThat(fitnessPermissionsResult)
                .containsExactlyElementsIn(
                    listOf(
                        readExercisePermission,
                        readNutritionPermission,
                        writeSleepPermission,
                        writeDistancePermission,
                    )
                )
            assertThat(medicalPermissionsResult)
                .containsExactlyElementsIn(listOf(readImmunization, writeMedicalData))

            assertThat(grantedAdditionalPermissionsResult)
                .containsExactlyElementsIn(
                    setOf(
                        readExerciseRoutesPermission,
                        readDataInBackgroundPermission,
                        readHistoryDataPermission,
                    )
                )

            assertThat(grantedFitnessPermissionsResult)
                .containsExactlyElementsIn(setOf(readExercisePermission, writeSleepPermission))
            assertThat(grantedMedicalPermissionsResult)
                .containsExactlyElementsIn(setOf(readImmunization, writeMedicalData))

            val result = appPermissionViewModel.revokeAllHealthPermissions(TEST_APP_PACKAGE_NAME)
            advanceUntilIdle()
            assertThat(grantedFitnessPermissionsObserver.getLastValue()).isEmpty()
            assertThat(grantedMedicalPermissionsObserver.getLastValue()).isEmpty()
            assertThat(grantedAdditionalPermissionsObserver.getLastValue()).isEmpty()
            assertThat(result).isTrue()
        }

    @Test
    fun revokeAllFitness_fitnessOnly_revokesFitnessOnly() = runTest {
        whenever(healthPermissionReader.isRationaleIntentDeclared(any())).thenReturn(true)
        whenever(healthPermissionReader.getDeclaredHealthPermissions(any()))
            .thenReturn(
                listOf(
                    readExercisePermission.toString(),
                    readNutritionPermission.toString(),
                    writeSleepPermission.toString(),
                    writeDistancePermission.toString(),
                )
            )
        getGrantedHealthPermissionsUseCase.updateData(
            TEST_APP_PACKAGE_NAME,
            listOf(readExercisePermission.toString(), writeSleepPermission.toString()),
        )

        val fitnessPermissionsObserver = TestObserver<List<FitnessPermission>>()
        val medicalPermissionsObserver = TestObserver<List<MedicalPermission>>()
        val grantedAdditionalPermissionsObserver = TestObserver<Set<AdditionalPermission>>()
        val grantedFitnessPermissionsObserver = TestObserver<Set<FitnessPermission>>()
        val grantedMedicalPermissionsObserver = TestObserver<Set<MedicalPermission>>()
        appPermissionViewModel.fitnessPermissions.observeForever(fitnessPermissionsObserver)
        appPermissionViewModel.medicalPermissions.observeForever(medicalPermissionsObserver)
        appPermissionViewModel.grantedAdditionalPermissions.observeForever(
            grantedAdditionalPermissionsObserver
        )
        appPermissionViewModel.grantedFitnessPermissions.observeForever(
            grantedFitnessPermissionsObserver
        )
        appPermissionViewModel.grantedMedicalPermissions.observeForever(
            grantedMedicalPermissionsObserver
        )

        appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        val fitnessPermissionsResult = fitnessPermissionsObserver.getLastValue()
        val medicalPermissionsResult = medicalPermissionsObserver.getLastValue()
        val grantedAdditionalPermissionsResult = grantedAdditionalPermissionsObserver.getLastValue()
        val grantedFitnessPermissionsResult = grantedFitnessPermissionsObserver.getLastValue()
        val grantedMedicalPermissionsResult = grantedMedicalPermissionsObserver.getLastValue()

        assertThat(fitnessPermissionsResult)
            .containsExactlyElementsIn(
                listOf(
                    readExercisePermission,
                    readNutritionPermission,
                    writeSleepPermission,
                    writeDistancePermission,
                )
            )
        assertThat(medicalPermissionsResult).isEmpty()

        assertThat(grantedAdditionalPermissionsResult).isEmpty()

        assertThat(grantedFitnessPermissionsResult)
            .containsExactlyElementsIn(setOf(readExercisePermission, writeSleepPermission))
        assertThat(grantedMedicalPermissionsResult).isEmpty()

        val result =
            appPermissionViewModel.revokeAllFitnessAndMaybeAdditionalPermissions(
                TEST_APP_PACKAGE_NAME
            )
        advanceUntilIdle()
        assertThat(grantedFitnessPermissionsObserver.getLastValue()).isEmpty()
        assertThat(grantedMedicalPermissionsObserver.getLastValue()).isEmpty()
        assertThat(grantedAdditionalPermissionsObserver.getLastValue()).isEmpty()
        // we revoke all declared permissions
        verify(revokePermissionStatusUseCase)
            .invoke(TEST_APP_PACKAGE_NAME, readExercisePermission.toString())
        verify(revokePermissionStatusUseCase)
            .invoke(TEST_APP_PACKAGE_NAME, readNutritionPermission.toString())
        verify(revokePermissionStatusUseCase)
            .invoke(TEST_APP_PACKAGE_NAME, writeSleepPermission.toString())
        verify(revokePermissionStatusUseCase)
            .invoke(TEST_APP_PACKAGE_NAME, writeDistancePermission.toString())
        verifyNoMoreInteractions(revokePermissionStatusUseCase)
        assertThat(result).isTrue()
    }

    @Test
    fun revokeAllFitness_fitnessAndMedical_revokesFitnessOnly() = runTest {
        whenever(healthPermissionReader.isRationaleIntentDeclared(any())).thenReturn(true)
        whenever(healthPermissionReader.getDeclaredHealthPermissions(any()))
            .thenReturn(
                listOf(
                    readExercisePermission.toString(),
                    readNutritionPermission.toString(),
                    writeSleepPermission.toString(),
                    writeDistancePermission.toString(),
                    readImmunization.toString(),
                    writeMedicalData.toString(),
                )
            )
        getGrantedHealthPermissionsUseCase.updateData(
            TEST_APP_PACKAGE_NAME,
            listOf(
                readExercisePermission.toString(),
                writeSleepPermission.toString(),
                readImmunization.toString(),
                writeMedicalData.toString(),
            ),
        )

        val fitnessPermissionsObserver = TestObserver<List<FitnessPermission>>()
        val medicalPermissionsObserver = TestObserver<List<MedicalPermission>>()
        val grantedAdditionalPermissionsObserver = TestObserver<Set<AdditionalPermission>>()
        val grantedFitnessPermissionsObserver = TestObserver<Set<FitnessPermission>>()
        val grantedMedicalPermissionsObserver = TestObserver<Set<MedicalPermission>>()
        appPermissionViewModel.fitnessPermissions.observeForever(fitnessPermissionsObserver)
        appPermissionViewModel.medicalPermissions.observeForever(medicalPermissionsObserver)
        appPermissionViewModel.grantedAdditionalPermissions.observeForever(
            grantedAdditionalPermissionsObserver
        )
        appPermissionViewModel.grantedFitnessPermissions.observeForever(
            grantedFitnessPermissionsObserver
        )
        appPermissionViewModel.grantedMedicalPermissions.observeForever(
            grantedMedicalPermissionsObserver
        )

        appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        val fitnessPermissionsResult = fitnessPermissionsObserver.getLastValue()
        val medicalPermissionsResult = medicalPermissionsObserver.getLastValue()
        val grantedAdditionalPermissionsResult = grantedAdditionalPermissionsObserver.getLastValue()
        val grantedFitnessPermissionsResult = grantedFitnessPermissionsObserver.getLastValue()
        val grantedMedicalPermissionsResult = grantedMedicalPermissionsObserver.getLastValue()

        assertThat(fitnessPermissionsResult)
            .containsExactlyElementsIn(
                listOf(
                    readExercisePermission,
                    readNutritionPermission,
                    writeSleepPermission,
                    writeDistancePermission,
                )
            )
        assertThat(medicalPermissionsResult)
            .containsExactlyElementsIn(listOf(readImmunization, writeMedicalData))

        assertThat(grantedAdditionalPermissionsResult).isEmpty()

        assertThat(grantedFitnessPermissionsResult)
            .containsExactlyElementsIn(setOf(readExercisePermission, writeSleepPermission))
        assertThat(grantedMedicalPermissionsResult)
            .containsExactlyElementsIn(setOf(readImmunization, writeMedicalData))

        val result =
            appPermissionViewModel.revokeAllFitnessAndMaybeAdditionalPermissions(
                TEST_APP_PACKAGE_NAME
            )
        advanceUntilIdle()
        assertThat(grantedFitnessPermissionsObserver.getLastValue()).isEmpty()
        assertThat(grantedMedicalPermissionsObserver.getLastValue())
            .containsExactlyElementsIn(setOf(readImmunization, writeMedicalData))
        assertThat(grantedAdditionalPermissionsObserver.getLastValue()).isEmpty()
        // we revoke all declared permissions
        verify(revokePermissionStatusUseCase)
            .invoke(TEST_APP_PACKAGE_NAME, readExercisePermission.toString())
        verify(revokePermissionStatusUseCase)
            .invoke(TEST_APP_PACKAGE_NAME, readNutritionPermission.toString())
        verify(revokePermissionStatusUseCase)
            .invoke(TEST_APP_PACKAGE_NAME, writeSleepPermission.toString())
        verify(revokePermissionStatusUseCase)
            .invoke(TEST_APP_PACKAGE_NAME, writeDistancePermission.toString())
        verifyNoMoreInteractions(revokePermissionStatusUseCase)
        assertThat(result).isTrue()
    }

    @Test
    fun revokeAllFitness_fitnessAndMedicalAndAdditional_medicalReadGranted_revokesFitnessOnly() =
        runTest {
            whenever(healthPermissionReader.isRationaleIntentDeclared(any())).thenReturn(true)
            whenever(healthPermissionReader.getDeclaredHealthPermissions(any()))
                .thenReturn(
                    listOf(
                        readExercisePermission.toString(),
                        readNutritionPermission.toString(),
                        writeSleepPermission.toString(),
                        writeDistancePermission.toString(),
                        readDataInBackgroundPermission.toString(),
                        readHistoryDataPermission.toString(),
                        readImmunization.toString(),
                        writeMedicalData.toString(),
                    )
                )
            getGrantedHealthPermissionsUseCase.updateData(
                TEST_APP_PACKAGE_NAME,
                listOf(
                    readExercisePermission.toString(),
                    writeSleepPermission.toString(),
                    readHistoryDataPermission.additionalPermission,
                    readDataInBackgroundPermission.additionalPermission,
                    readImmunization.toString(),
                    writeMedicalData.toString(),
                ),
            )

            val fitnessPermissionsObserver = TestObserver<List<FitnessPermission>>()
            val medicalPermissionsObserver = TestObserver<List<MedicalPermission>>()
            val grantedAdditionalPermissionsObserver = TestObserver<Set<AdditionalPermission>>()
            val grantedFitnessPermissionsObserver = TestObserver<Set<FitnessPermission>>()
            val grantedMedicalPermissionsObserver = TestObserver<Set<MedicalPermission>>()
            appPermissionViewModel.fitnessPermissions.observeForever(fitnessPermissionsObserver)
            appPermissionViewModel.medicalPermissions.observeForever(medicalPermissionsObserver)
            appPermissionViewModel.grantedAdditionalPermissions.observeForever(
                grantedAdditionalPermissionsObserver
            )
            appPermissionViewModel.grantedFitnessPermissions.observeForever(
                grantedFitnessPermissionsObserver
            )
            appPermissionViewModel.grantedMedicalPermissions.observeForever(
                grantedMedicalPermissionsObserver
            )

            appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
            advanceUntilIdle()

            val fitnessPermissionsResult = fitnessPermissionsObserver.getLastValue()
            val medicalPermissionsResult = medicalPermissionsObserver.getLastValue()
            val grantedAdditionalPermissionsResult =
                grantedAdditionalPermissionsObserver.getLastValue()
            val grantedFitnessPermissionsResult = grantedFitnessPermissionsObserver.getLastValue()
            val grantedMedicalPermissionsResult = grantedMedicalPermissionsObserver.getLastValue()

            assertThat(fitnessPermissionsResult)
                .containsExactlyElementsIn(
                    listOf(
                        readExercisePermission,
                        readNutritionPermission,
                        writeSleepPermission,
                        writeDistancePermission,
                    )
                )
            assertThat(medicalPermissionsResult)
                .containsExactlyElementsIn(listOf(readImmunization, writeMedicalData))

            assertThat(grantedAdditionalPermissionsResult)
                .containsExactlyElementsIn(
                    setOf(readDataInBackgroundPermission, readHistoryDataPermission)
                )

            assertThat(grantedFitnessPermissionsResult)
                .containsExactlyElementsIn(setOf(readExercisePermission, writeSleepPermission))
            assertThat(grantedMedicalPermissionsResult)
                .containsExactlyElementsIn(setOf(readImmunization, writeMedicalData))

            val result =
                appPermissionViewModel.revokeAllFitnessAndMaybeAdditionalPermissions(
                    TEST_APP_PACKAGE_NAME
                )
            advanceUntilIdle()
            assertThat(grantedFitnessPermissionsObserver.getLastValue()).isEmpty()
            assertThat(grantedMedicalPermissionsObserver.getLastValue())
                .containsExactlyElementsIn(setOf(readImmunization, writeMedicalData))
            assertThat(grantedAdditionalPermissionsObserver.getLastValue())
                .containsExactlyElementsIn(
                    setOf(readDataInBackgroundPermission, readHistoryDataPermission)
                )
            // we revoke all declared fitness permissions
            verify(revokePermissionStatusUseCase)
                .invoke(TEST_APP_PACKAGE_NAME, readExercisePermission.toString())
            verify(revokePermissionStatusUseCase)
                .invoke(TEST_APP_PACKAGE_NAME, readNutritionPermission.toString())
            verify(revokePermissionStatusUseCase)
                .invoke(TEST_APP_PACKAGE_NAME, writeSleepPermission.toString())
            verify(revokePermissionStatusUseCase)
                .invoke(TEST_APP_PACKAGE_NAME, writeDistancePermission.toString())
            verifyNoMoreInteractions(revokePermissionStatusUseCase)
            assertThat(result).isTrue()
        }

    @Test
    fun revokeAllFitness_fitnessAndMedicalAndAdditional_medicalNotGranted_revokesFitnessAndAdditional() =
        runTest {
            whenever(healthPermissionReader.isRationaleIntentDeclared(any())).thenReturn(true)
            whenever(healthPermissionReader.getDeclaredHealthPermissions(any()))
                .thenReturn(
                    listOf(
                        readExercisePermission.toString(),
                        readNutritionPermission.toString(),
                        writeSleepPermission.toString(),
                        writeDistancePermission.toString(),
                        readDataInBackgroundPermission.toString(),
                        readHistoryDataPermission.toString(),
                        readImmunization.toString(),
                        writeMedicalData.toString(),
                    )
                )
            getGrantedHealthPermissionsUseCase.updateData(
                TEST_APP_PACKAGE_NAME,
                listOf(
                    readExercisePermission.toString(),
                    writeSleepPermission.toString(),
                    readHistoryDataPermission.additionalPermission,
                    readDataInBackgroundPermission.additionalPermission,
                    writeMedicalData.toString(),
                ),
            )

            val fitnessPermissionsObserver = TestObserver<List<FitnessPermission>>()
            val medicalPermissionsObserver = TestObserver<List<MedicalPermission>>()
            val grantedAdditionalPermissionsObserver = TestObserver<Set<AdditionalPermission>>()
            val grantedFitnessPermissionsObserver = TestObserver<Set<FitnessPermission>>()
            val grantedMedicalPermissionsObserver = TestObserver<Set<MedicalPermission>>()
            appPermissionViewModel.fitnessPermissions.observeForever(fitnessPermissionsObserver)
            appPermissionViewModel.medicalPermissions.observeForever(medicalPermissionsObserver)
            appPermissionViewModel.grantedAdditionalPermissions.observeForever(
                grantedAdditionalPermissionsObserver
            )
            appPermissionViewModel.grantedFitnessPermissions.observeForever(
                grantedFitnessPermissionsObserver
            )
            appPermissionViewModel.grantedMedicalPermissions.observeForever(
                grantedMedicalPermissionsObserver
            )

            appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
            advanceUntilIdle()

            val fitnessPermissionsResult = fitnessPermissionsObserver.getLastValue()
            val medicalPermissionsResult = medicalPermissionsObserver.getLastValue()
            val grantedAdditionalPermissionsResult =
                grantedAdditionalPermissionsObserver.getLastValue()
            val grantedFitnessPermissionsResult = grantedFitnessPermissionsObserver.getLastValue()
            val grantedMedicalPermissionsResult = grantedMedicalPermissionsObserver.getLastValue()

            assertThat(fitnessPermissionsResult)
                .containsExactlyElementsIn(
                    listOf(
                        readExercisePermission,
                        readNutritionPermission,
                        writeSleepPermission,
                        writeDistancePermission,
                    )
                )
            assertThat(medicalPermissionsResult)
                .containsExactlyElementsIn(listOf(readImmunization, writeMedicalData))

            assertThat(grantedAdditionalPermissionsResult)
                .containsExactlyElementsIn(
                    setOf(readDataInBackgroundPermission, readHistoryDataPermission)
                )

            assertThat(grantedFitnessPermissionsResult)
                .containsExactlyElementsIn(setOf(readExercisePermission, writeSleepPermission))
            assertThat(grantedMedicalPermissionsResult)
                .containsExactlyElementsIn(setOf(writeMedicalData))

            val result =
                appPermissionViewModel.revokeAllFitnessAndMaybeAdditionalPermissions(
                    TEST_APP_PACKAGE_NAME
                )
            advanceUntilIdle()
            verify(revokePermissionStatusUseCase)
                .invoke(TEST_APP_PACKAGE_NAME, writeDistancePermission.toString())
            assertThat(grantedFitnessPermissionsObserver.getLastValue()).isEmpty()
            assertThat(grantedMedicalPermissionsObserver.getLastValue())
                .containsExactlyElementsIn(setOf(writeMedicalData))
            assertThat(grantedAdditionalPermissionsObserver.getLastValue()).isEmpty()
            // we revoke all declared fitness permissions
            verify(revokePermissionStatusUseCase)
                .invoke(TEST_APP_PACKAGE_NAME, readExercisePermission.toString())
            verify(revokePermissionStatusUseCase)
                .invoke(TEST_APP_PACKAGE_NAME, readNutritionPermission.toString())
            verify(revokePermissionStatusUseCase)
                .invoke(TEST_APP_PACKAGE_NAME, writeSleepPermission.toString())
            verify(revokePermissionStatusUseCase)
                .invoke(TEST_APP_PACKAGE_NAME, writeDistancePermission.toString())
            verify(revokePermissionStatusUseCase)
                .invoke(TEST_APP_PACKAGE_NAME, readDataInBackgroundPermission.toString())
            verify(revokePermissionStatusUseCase)
                .invoke(TEST_APP_PACKAGE_NAME, readHistoryDataPermission.toString())
            verifyNoMoreInteractions(revokePermissionStatusUseCase)
            assertThat(result).isTrue()
        }

    @Test
    fun revokeAllMedical_medicalOnly_revokesMedicalOnly() = runTest {
        whenever(healthPermissionReader.isRationaleIntentDeclared(any())).thenReturn(true)
        whenever(healthPermissionReader.getDeclaredHealthPermissions(any()))
            .thenReturn(
                listOf(
                    readAllergies.toString(),
                    readImmunization.toString(),
                    writeMedicalData.toString(),
                )
            )
        getGrantedHealthPermissionsUseCase.updateData(
            TEST_APP_PACKAGE_NAME,
            listOf(readImmunization.toString(), writeMedicalData.toString()),
        )

        val fitnessPermissionsObserver = TestObserver<List<FitnessPermission>>()
        val medicalPermissionsObserver = TestObserver<List<MedicalPermission>>()
        val grantedAdditionalPermissionsObserver = TestObserver<Set<AdditionalPermission>>()
        val grantedFitnessPermissionsObserver = TestObserver<Set<FitnessPermission>>()
        val grantedMedicalPermissionsObserver = TestObserver<Set<MedicalPermission>>()
        appPermissionViewModel.fitnessPermissions.observeForever(fitnessPermissionsObserver)
        appPermissionViewModel.medicalPermissions.observeForever(medicalPermissionsObserver)
        appPermissionViewModel.grantedAdditionalPermissions.observeForever(
            grantedAdditionalPermissionsObserver
        )
        appPermissionViewModel.grantedFitnessPermissions.observeForever(
            grantedFitnessPermissionsObserver
        )
        appPermissionViewModel.grantedMedicalPermissions.observeForever(
            grantedMedicalPermissionsObserver
        )

        appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        val fitnessPermissionsResult = fitnessPermissionsObserver.getLastValue()
        val medicalPermissionsResult = medicalPermissionsObserver.getLastValue()
        val grantedAdditionalPermissionsResult = grantedAdditionalPermissionsObserver.getLastValue()
        val grantedFitnessPermissionsResult = grantedFitnessPermissionsObserver.getLastValue()
        val grantedMedicalPermissionsResult = grantedMedicalPermissionsObserver.getLastValue()

        assertThat(fitnessPermissionsResult).isEmpty()
        assertThat(medicalPermissionsResult)
            .containsExactlyElementsIn(listOf(readAllergies, readImmunization, writeMedicalData))

        assertThat(grantedAdditionalPermissionsResult).isEmpty()

        assertThat(grantedFitnessPermissionsResult).isEmpty()
        assertThat(grantedMedicalPermissionsResult)
            .containsExactlyElementsIn(setOf(readImmunization, writeMedicalData))

        val result =
            appPermissionViewModel.revokeAllMedicalAndMaybeAdditionalPermissions(
                TEST_APP_PACKAGE_NAME
            )
        advanceUntilIdle()
        assertThat(grantedFitnessPermissionsObserver.getLastValue()).isEmpty()
        assertThat(grantedMedicalPermissionsObserver.getLastValue()).isEmpty()
        assertThat(grantedAdditionalPermissionsObserver.getLastValue()).isEmpty()
        // we revoke all declared medical permissions
        verify(revokePermissionStatusUseCase)
            .invoke(TEST_APP_PACKAGE_NAME, readAllergies.toString())
        verify(revokePermissionStatusUseCase)
            .invoke(TEST_APP_PACKAGE_NAME, readImmunization.toString())
        verify(revokePermissionStatusUseCase)
            .invoke(TEST_APP_PACKAGE_NAME, writeMedicalData.toString())
        verifyNoMoreInteractions(revokePermissionStatusUseCase)
        assertThat(result).isTrue()
    }

    @Test
    fun revokeAllMedical_fitnessAndMedical_revokesMedicalOnly() = runTest {
        whenever(healthPermissionReader.isRationaleIntentDeclared(any())).thenReturn(true)
        whenever(healthPermissionReader.getDeclaredHealthPermissions(any()))
            .thenReturn(
                listOf(
                    readNutritionPermission.toString(),
                    writeSleepPermission.toString(),
                    readAllergies.toString(),
                    readImmunization.toString(),
                    writeMedicalData.toString(),
                )
            )
        getGrantedHealthPermissionsUseCase.updateData(
            TEST_APP_PACKAGE_NAME,
            listOf(
                readNutritionPermission.toString(),
                writeSleepPermission.toString(),
                readImmunization.toString(),
                writeMedicalData.toString(),
            ),
        )

        val fitnessPermissionsObserver = TestObserver<List<FitnessPermission>>()
        val medicalPermissionsObserver = TestObserver<List<MedicalPermission>>()
        val grantedAdditionalPermissionsObserver = TestObserver<Set<AdditionalPermission>>()
        val grantedFitnessPermissionsObserver = TestObserver<Set<FitnessPermission>>()
        val grantedMedicalPermissionsObserver = TestObserver<Set<MedicalPermission>>()
        appPermissionViewModel.fitnessPermissions.observeForever(fitnessPermissionsObserver)
        appPermissionViewModel.medicalPermissions.observeForever(medicalPermissionsObserver)
        appPermissionViewModel.grantedAdditionalPermissions.observeForever(
            grantedAdditionalPermissionsObserver
        )
        appPermissionViewModel.grantedFitnessPermissions.observeForever(
            grantedFitnessPermissionsObserver
        )
        appPermissionViewModel.grantedMedicalPermissions.observeForever(
            grantedMedicalPermissionsObserver
        )

        appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        val fitnessPermissionsResult = fitnessPermissionsObserver.getLastValue()
        val medicalPermissionsResult = medicalPermissionsObserver.getLastValue()
        val grantedAdditionalPermissionsResult = grantedAdditionalPermissionsObserver.getLastValue()
        val grantedFitnessPermissionsResult = grantedFitnessPermissionsObserver.getLastValue()
        val grantedMedicalPermissionsResult = grantedMedicalPermissionsObserver.getLastValue()

        assertThat(fitnessPermissionsResult)
            .containsExactlyElementsIn(listOf(readNutritionPermission, writeSleepPermission))
        assertThat(medicalPermissionsResult)
            .containsExactlyElementsIn(listOf(readAllergies, readImmunization, writeMedicalData))

        assertThat(grantedAdditionalPermissionsResult).isEmpty()

        assertThat(grantedFitnessPermissionsResult)
            .containsExactlyElementsIn(setOf(readNutritionPermission, writeSleepPermission))
        assertThat(grantedMedicalPermissionsResult)
            .containsExactlyElementsIn(setOf(readImmunization, writeMedicalData))

        val result =
            appPermissionViewModel.revokeAllMedicalAndMaybeAdditionalPermissions(
                TEST_APP_PACKAGE_NAME
            )
        advanceUntilIdle()
        assertThat(grantedFitnessPermissionsObserver.getLastValue())
            .containsExactlyElementsIn(setOf(readNutritionPermission, writeSleepPermission))
        assertThat(grantedMedicalPermissionsObserver.getLastValue()).isEmpty()
        assertThat(grantedAdditionalPermissionsObserver.getLastValue()).isEmpty()
        // we revoke all declared medical permissions
        verify(revokePermissionStatusUseCase)
            .invoke(TEST_APP_PACKAGE_NAME, readAllergies.toString())
        verify(revokePermissionStatusUseCase)
            .invoke(TEST_APP_PACKAGE_NAME, readImmunization.toString())
        verify(revokePermissionStatusUseCase)
            .invoke(TEST_APP_PACKAGE_NAME, writeMedicalData.toString())
        verifyNoMoreInteractions(revokePermissionStatusUseCase)
        assertThat(result).isTrue()
    }

    @Test
    fun revokeAllMedical_fitnessAndMedicalAndAdditional_fitnessGranted_revokesMedicalOnly() =
        runTest {
            whenever(healthPermissionReader.isRationaleIntentDeclared(any())).thenReturn(true)
            whenever(healthPermissionReader.getDeclaredHealthPermissions(any()))
                .thenReturn(
                    listOf(
                        readExercisePermission.toString(),
                        readNutritionPermission.toString(),
                        writeSleepPermission.toString(),
                        readExerciseRoutesPermission.toString(),
                        readDataInBackgroundPermission.toString(),
                        readHistoryDataPermission.toString(),
                        readAllergies.toString(),
                        readImmunization.toString(),
                        writeMedicalData.toString(),
                    )
                )
            getGrantedHealthPermissionsUseCase.updateData(
                TEST_APP_PACKAGE_NAME,
                listOf(
                    readNutritionPermission.toString(),
                    writeSleepPermission.toString(),
                    readImmunization.toString(),
                    writeMedicalData.toString(),
                    readDataInBackgroundPermission.toString(),
                    readHistoryDataPermission.toString(),
                ),
            )

            val fitnessPermissionsObserver = TestObserver<List<FitnessPermission>>()
            val medicalPermissionsObserver = TestObserver<List<MedicalPermission>>()
            val grantedAdditionalPermissionsObserver = TestObserver<Set<AdditionalPermission>>()
            val grantedFitnessPermissionsObserver = TestObserver<Set<FitnessPermission>>()
            val grantedMedicalPermissionsObserver = TestObserver<Set<MedicalPermission>>()
            appPermissionViewModel.fitnessPermissions.observeForever(fitnessPermissionsObserver)
            appPermissionViewModel.medicalPermissions.observeForever(medicalPermissionsObserver)
            appPermissionViewModel.grantedAdditionalPermissions.observeForever(
                grantedAdditionalPermissionsObserver
            )
            appPermissionViewModel.grantedFitnessPermissions.observeForever(
                grantedFitnessPermissionsObserver
            )
            appPermissionViewModel.grantedMedicalPermissions.observeForever(
                grantedMedicalPermissionsObserver
            )

            appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
            advanceUntilIdle()

            val fitnessPermissionsResult = fitnessPermissionsObserver.getLastValue()
            val medicalPermissionsResult = medicalPermissionsObserver.getLastValue()
            val grantedAdditionalPermissionsResult =
                grantedAdditionalPermissionsObserver.getLastValue()
            val grantedFitnessPermissionsResult = grantedFitnessPermissionsObserver.getLastValue()
            val grantedMedicalPermissionsResult = grantedMedicalPermissionsObserver.getLastValue()

            assertThat(fitnessPermissionsResult)
                .containsExactlyElementsIn(
                    listOf(readExercisePermission, readNutritionPermission, writeSleepPermission)
                )
            assertThat(medicalPermissionsResult)
                .containsExactlyElementsIn(
                    listOf(readAllergies, readImmunization, writeMedicalData)
                )

            assertThat(grantedAdditionalPermissionsResult)
                .containsExactlyElementsIn(
                    setOf(readDataInBackgroundPermission, readHistoryDataPermission)
                )

            assertThat(grantedFitnessPermissionsResult)
                .containsExactlyElementsIn(setOf(readNutritionPermission, writeSleepPermission))
            assertThat(grantedMedicalPermissionsResult)
                .containsExactlyElementsIn(setOf(readImmunization, writeMedicalData))

            val result =
                appPermissionViewModel.revokeAllMedicalAndMaybeAdditionalPermissions(
                    TEST_APP_PACKAGE_NAME
                )
            advanceUntilIdle()
            assertThat(grantedFitnessPermissionsObserver.getLastValue())
                .containsExactlyElementsIn(setOf(readNutritionPermission, writeSleepPermission))
            assertThat(grantedMedicalPermissionsObserver.getLastValue()).isEmpty()
            assertThat(grantedAdditionalPermissionsObserver.getLastValue())
                .containsExactlyElementsIn(
                    setOf(readDataInBackgroundPermission, readHistoryDataPermission)
                )
            // we revoke all declared medical permissions
            verify(revokePermissionStatusUseCase)
                .invoke(TEST_APP_PACKAGE_NAME, readAllergies.toString())
            verify(revokePermissionStatusUseCase)
                .invoke(TEST_APP_PACKAGE_NAME, readImmunization.toString())
            verify(revokePermissionStatusUseCase)
                .invoke(TEST_APP_PACKAGE_NAME, writeMedicalData.toString())
            verifyNoMoreInteractions(revokePermissionStatusUseCase)
            assertThat(result).isTrue()
        }

    @Test
    fun revokeAllMedical_fitnessAndMedicalAndAdditional_fitnessReadNotGranted_revokesMedicalAndAdditional() =
        runTest {
            whenever(healthPermissionReader.isRationaleIntentDeclared(any())).thenReturn(true)
            whenever(healthPermissionReader.getDeclaredHealthPermissions(any()))
                .thenReturn(
                    listOf(
                        readExercisePermission.toString(),
                        readNutritionPermission.toString(),
                        writeSleepPermission.toString(),
                        readExerciseRoutesPermission.toString(),
                        readDataInBackgroundPermission.toString(),
                        readHistoryDataPermission.toString(),
                        readAllergies.toString(),
                        readImmunization.toString(),
                        writeMedicalData.toString(),
                    )
                )
            getGrantedHealthPermissionsUseCase.updateData(
                TEST_APP_PACKAGE_NAME,
                listOf(
                    writeSleepPermission.toString(),
                    readImmunization.toString(),
                    writeMedicalData.toString(),
                    readDataInBackgroundPermission.toString(),
                    readHistoryDataPermission.toString(),
                ),
            )

            val fitnessPermissionsObserver = TestObserver<List<FitnessPermission>>()
            val medicalPermissionsObserver = TestObserver<List<MedicalPermission>>()
            val grantedAdditionalPermissionsObserver = TestObserver<Set<AdditionalPermission>>()
            val grantedFitnessPermissionsObserver = TestObserver<Set<FitnessPermission>>()
            val grantedMedicalPermissionsObserver = TestObserver<Set<MedicalPermission>>()
            appPermissionViewModel.fitnessPermissions.observeForever(fitnessPermissionsObserver)
            appPermissionViewModel.medicalPermissions.observeForever(medicalPermissionsObserver)
            appPermissionViewModel.grantedAdditionalPermissions.observeForever(
                grantedAdditionalPermissionsObserver
            )
            appPermissionViewModel.grantedFitnessPermissions.observeForever(
                grantedFitnessPermissionsObserver
            )
            appPermissionViewModel.grantedMedicalPermissions.observeForever(
                grantedMedicalPermissionsObserver
            )

            appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
            advanceUntilIdle()

            val fitnessPermissionsResult = fitnessPermissionsObserver.getLastValue()
            val medicalPermissionsResult = medicalPermissionsObserver.getLastValue()
            val grantedAdditionalPermissionsResult =
                grantedAdditionalPermissionsObserver.getLastValue()
            val grantedFitnessPermissionsResult = grantedFitnessPermissionsObserver.getLastValue()
            val grantedMedicalPermissionsResult = grantedMedicalPermissionsObserver.getLastValue()

            assertThat(fitnessPermissionsResult)
                .containsExactlyElementsIn(
                    listOf(readExercisePermission, readNutritionPermission, writeSleepPermission)
                )
            assertThat(medicalPermissionsResult)
                .containsExactlyElementsIn(
                    listOf(readAllergies, readImmunization, writeMedicalData)
                )

            assertThat(grantedAdditionalPermissionsResult)
                .containsExactlyElementsIn(
                    setOf(readDataInBackgroundPermission, readHistoryDataPermission)
                )

            assertThat(grantedFitnessPermissionsResult)
                .containsExactlyElementsIn(setOf(writeSleepPermission))
            assertThat(grantedMedicalPermissionsResult)
                .containsExactlyElementsIn(setOf(readImmunization, writeMedicalData))

            val result =
                appPermissionViewModel.revokeAllMedicalAndMaybeAdditionalPermissions(
                    TEST_APP_PACKAGE_NAME
                )
            advanceUntilIdle()
            assertThat(grantedFitnessPermissionsObserver.getLastValue())
                .containsExactlyElementsIn(setOf(writeSleepPermission))
            assertThat(grantedMedicalPermissionsObserver.getLastValue()).isEmpty()
            assertThat(grantedAdditionalPermissionsObserver.getLastValue()).isEmpty()
            // we revoke all declared medical permissions
            verify(revokePermissionStatusUseCase)
                .invoke(TEST_APP_PACKAGE_NAME, readAllergies.toString())
            verify(revokePermissionStatusUseCase)
                .invoke(TEST_APP_PACKAGE_NAME, readImmunization.toString())
            verify(revokePermissionStatusUseCase)
                .invoke(TEST_APP_PACKAGE_NAME, writeMedicalData.toString())
            verify(revokePermissionStatusUseCase)
                .invoke(TEST_APP_PACKAGE_NAME, readDataInBackgroundPermission.toString())
            verify(revokePermissionStatusUseCase)
                .invoke(TEST_APP_PACKAGE_NAME, readHistoryDataPermission.toString())
            verifyNoMoreInteractions(revokePermissionStatusUseCase)
            assertThat(result).isTrue()
        }

    // TODO (b/324247426) unignore when we can mock suspend functions
    @Test
    @Ignore
    fun deleteAppData_invokesUseCaseWithCorrectFilter() = runTest {
        appPermissionViewModel.deleteAppData(TEST_APP_PACKAGE_NAME, TEST_APP_NAME)
        advanceUntilIdle()

        verify(deleteAppDataUseCase).invoke(appDataCaptor.capture())
    }

    @Test
    fun shouldNavigateToFragment_whenPackageNameSupported_returnsTrue() = runTest {
        whenever(healthPermissionReader.isRationaleIntentDeclared(any())).thenReturn(true)
        whenever(healthPermissionReader.getDeclaredHealthPermissions(any()))
            .thenReturn(
                listOf(
                    readExercisePermission.toString(),
                    readNutritionPermission.toString(),
                    writeSleepPermission.toString(),
                    writeDistancePermission.toString(),
                )
            )
        getGrantedHealthPermissionsUseCase.updateData(TEST_APP_PACKAGE_NAME, listOf())

        advanceUntilIdle()

        assertThat(
            appPermissionViewModel.shouldNavigateToAppPermissionsFragment(TEST_APP_PACKAGE_NAME)
        )
            .isTrue()
    }

    @Test
    fun shouldNavigateToFragment_whenAnyPermissionGranted_returnsTrue() = runTest {
        whenever(healthPermissionReader.isRationaleIntentDeclared(any())).thenReturn(false)
        whenever(healthPermissionReader.getDeclaredHealthPermissions(any()))
            .thenReturn(
                listOf(
                    readExercisePermission.toString(),
                    readNutritionPermission.toString(),
                    writeSleepPermission.toString(),
                    writeDistancePermission.toString(),
                )
            )
        getGrantedHealthPermissionsUseCase.updateData(
            TEST_APP_PACKAGE_NAME,
            listOf(writeSleepPermission.toString()),
        )

        advanceUntilIdle()

        assertThat(
            appPermissionViewModel.shouldNavigateToAppPermissionsFragment(TEST_APP_PACKAGE_NAME)
        )
            .isTrue()
    }

    @Test
    fun shouldNavigateToFragment_whenPackageNotSupported_andNoPermissionsGranted_returnsFalse() =
        runTest {
            whenever(healthPermissionReader.isRationaleIntentDeclared(any())).thenReturn(false)
            whenever(healthPermissionReader.getDeclaredHealthPermissions(any()))
                .thenReturn(
                    listOf(
                        readExercisePermission.toString(),
                        readNutritionPermission.toString(),
                        writeSleepPermission.toString(),
                        writeDistancePermission.toString(),
                    )
                )
            getGrantedHealthPermissionsUseCase.updateData(TEST_APP_PACKAGE_NAME, listOf())

            advanceUntilIdle()

            assertThat(
                appPermissionViewModel.shouldNavigateToAppPermissionsFragment(
                    TEST_APP_PACKAGE_NAME
                )
            )
                .isFalse()
        }

    @Test
    fun isPackageSupported_callsCorrectMethod() {
        whenever(healthPermissionReader.isBodySensorSplitPermissionApp(TEST_APP_PACKAGE_NAME))
            .thenReturn(false)

        appPermissionViewModel.isPackageSupported(TEST_APP_PACKAGE_NAME)

        verify(healthPermissionReader).isRationaleIntentDeclared(TEST_APP_PACKAGE_NAME)
    }

    @Test
    @DisableFlags(Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED)
    fun isPackageSupported_watch_flagDisabled_callsCorrectMethod() {
        whenever(deviceInfoUtils.isOnWatch(any())).thenReturn(true)

        appPermissionViewModel.isPackageSupported(TEST_APP_PACKAGE_NAME)

        verify(healthPermissionReader).isRationaleIntentDeclared(TEST_APP_PACKAGE_NAME)
    }

    @Test
    @EnableFlags(Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED)
    fun isPackageSupported_watch_flagEnabled_packageSupported() {
        whenever(deviceInfoUtils.isOnWatch(any())).thenReturn(true)

        assertThat(appPermissionViewModel.isPackageSupported(TEST_APP_PACKAGE_NAME)).isTrue()
        verify(healthPermissionReader, never()).isRationaleIntentDeclared(any())
    }

    @Test
    @EnableFlags(Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED)
    fun isPackageSupported_notWatch_flagEnabled_splitPermissionApp_packageSupported() {
        whenever(healthPermissionReader.isBodySensorSplitPermissionApp(TEST_APP_PACKAGE_NAME))
            .thenReturn(true)

        assertThat(appPermissionViewModel.isPackageSupported(TEST_APP_PACKAGE_NAME)).isTrue()

        verify(healthPermissionReader, never()).isRationaleIntentDeclared(any())
    }

    @Test
    fun grantAllFitnessPermissions_isAllFitnessPermissionsGranted_returnTrue() = runTest {
        setupDeclaredAndGrantedFitnessAndMedicalPermissions()
        appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        appPermissionViewModel.grantAllFitnessPermissions(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        appPermissionViewModel.allFitnessPermissionsGranted.observeForever {
            assertThat(it).isTrue()
        }
    }

    @Test
    fun grantAllFitnessPermissions_isAllMedicalPermissionsGranted_returnFalse() = runTest {
        setupDeclaredAndGrantedFitnessAndMedicalPermissions()
        appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        appPermissionViewModel.grantAllFitnessPermissions(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        appPermissionViewModel.allMedicalPermissionsGranted.observeForever {
            assertThat(it).isFalse()
        }
    }

    @Test
    fun grantAllMedicalPermissions_isAllFitnessPermissionsGranted_returnFalse() = runTest {
        setupDeclaredAndGrantedFitnessAndMedicalPermissions()
        appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        appPermissionViewModel.grantAllMedicalPermissions(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        appPermissionViewModel.allFitnessPermissionsGranted.observeForever {
            assertThat(it).isFalse()
        }
    }

    @Test
    fun grantAllMedicalPermissions_isAllMedicalPermissionsGranted_returnTrue() = runTest {
        setupDeclaredAndGrantedFitnessAndMedicalPermissions()
        appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        appPermissionViewModel.grantAllMedicalPermissions(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        appPermissionViewModel.allMedicalPermissionsGranted.observeForever {
            assertThat(it).isTrue()
        }
    }

    @Test
    fun revokeFitnessShouldIncludeBackground_whenBGNotDeclared_returnsFalse() = runTest {
        whenever(healthPermissionReader.isRationaleIntentDeclared(any())).thenReturn(true)
        whenever(healthPermissionReader.getDeclaredHealthPermissions(any()))
            .thenReturn(
                listOf(
                    readExercisePermission.toString(),
                    readNutritionPermission.toString(),
                    writeSleepPermission.toString(),
                    writeDistancePermission.toString(),
                    readHistoryDataPermission.toString(),
                    readImmunization.toString(),
                    writeMedicalData.toString(),
                )
            )
        getGrantedHealthPermissionsUseCase.updateData(
            TEST_APP_PACKAGE_NAME,
            listOf(
                readExercisePermission.toString(),
                writeSleepPermission.toString(),
                readHistoryDataPermission.additionalPermission,
                readImmunization.toString(),
                writeMedicalData.toString(),
            ),
        )
        appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        val revokeFitnessShouldIncludeBackgroundResult =
            appPermissionViewModel.revokeFitnessShouldIncludeBackground()
        assertThat(revokeFitnessShouldIncludeBackgroundResult).isFalse()
    }

    @Test
    fun revokeFitnessShouldIncludeBackground_whenNoReadPermissionDeclared_returnsFalse() = runTest {
        whenever(healthPermissionReader.isRationaleIntentDeclared(any())).thenReturn(true)
        whenever(healthPermissionReader.getDeclaredHealthPermissions(any()))
            .thenReturn(
                listOf(
                    writeSleepPermission.toString(),
                    writeDistancePermission.toString(),
                    writeMedicalData.toString(),
                    readDataInBackgroundPermission.toString(),
                )
            )
        getGrantedHealthPermissionsUseCase.updateData(
            TEST_APP_PACKAGE_NAME,
            listOf(writeSleepPermission.toString(), writeMedicalData.toString()),
        )
        appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        val revokeFitnessShouldIncludeBackgroundResult =
            appPermissionViewModel.revokeFitnessShouldIncludeBackground()
        assertThat(revokeFitnessShouldIncludeBackgroundResult).isFalse()
    }

    @Test
    fun revokeFitnessShouldIncludeBackground_whenNoFitnessReadGranted_returnsFalse() = runTest {
        // If e.g. we revoke permissions for an app that has just write permissions
        // declared/granted,
        // Then we shouldn't revoke BG, because its only dependent on medical read
        // And if there is no medical read, then we can't have BG anyway
        whenever(healthPermissionReader.isRationaleIntentDeclared(any())).thenReturn(true)
        whenever(healthPermissionReader.getDeclaredHealthPermissions(any()))
            .thenReturn(
                listOf(
                    readExercisePermission.toString(),
                    readNutritionPermission.toString(),
                    writeSleepPermission.toString(),
                    writeDistancePermission.toString(),
                    readDataInBackgroundPermission.toString(),
                    readHistoryDataPermission.toString(),
                    readImmunization.toString(),
                    writeMedicalData.toString(),
                )
            )
        getGrantedHealthPermissionsUseCase.updateData(
            TEST_APP_PACKAGE_NAME,
            listOf(
                writeSleepPermission.toString(),
                readDataInBackgroundPermission.additionalPermission,
                readHistoryDataPermission.additionalPermission,
                writeMedicalData.toString(),
            ),
        )
        appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()
        val revokeFitnessShouldIncludeBackgroundResult =
            appPermissionViewModel.revokeFitnessShouldIncludeBackground()
        assertThat(revokeFitnessShouldIncludeBackgroundResult).isFalse()
    }

    @Test
    fun revokeFitnessShouldIncludeBackground_whenMedicalRead_returnsFalse() = runTest {
        whenever(healthPermissionReader.isRationaleIntentDeclared(any())).thenReturn(true)
        whenever(healthPermissionReader.getDeclaredHealthPermissions(any()))
            .thenReturn(
                listOf(
                    readExercisePermission.toString(),
                    readNutritionPermission.toString(),
                    writeSleepPermission.toString(),
                    writeDistancePermission.toString(),
                    readDataInBackgroundPermission.toString(),
                    readHistoryDataPermission.toString(),
                    readImmunization.toString(),
                    writeMedicalData.toString(),
                )
            )
        getGrantedHealthPermissionsUseCase.updateData(
            TEST_APP_PACKAGE_NAME,
            listOf(
                readNutritionPermission.toString(),
                writeSleepPermission.toString(),
                readDataInBackgroundPermission.additionalPermission,
                readHistoryDataPermission.additionalPermission,
                readImmunization.toString(),
                writeMedicalData.toString(),
            ),
        )

        appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        val revokeFitnessShouldIncludeBackgroundResult =
            appPermissionViewModel.revokeFitnessShouldIncludeBackground()
        assertThat(revokeFitnessShouldIncludeBackgroundResult).isFalse()
    }

    @Test
    fun revokeFitnessShouldIncludeBackground_whenBGDeclared_andFitnessRead_andNoMedicalRead_returnsTrue() =
        runTest {
            whenever(healthPermissionReader.isRationaleIntentDeclared(any())).thenReturn(true)
            whenever(healthPermissionReader.getDeclaredHealthPermissions(any()))
                .thenReturn(
                    listOf(
                        readExercisePermission.toString(),
                        readNutritionPermission.toString(),
                        writeSleepPermission.toString(),
                        writeDistancePermission.toString(),
                        readDataInBackgroundPermission.toString(),
                        readHistoryDataPermission.toString(),
                        readImmunization.toString(),
                        writeMedicalData.toString(),
                    )
                )
            getGrantedHealthPermissionsUseCase.updateData(
                TEST_APP_PACKAGE_NAME,
                listOf(
                    readNutritionPermission.toString(),
                    writeSleepPermission.toString(),
                    readDataInBackgroundPermission.additionalPermission,
                    readHistoryDataPermission.additionalPermission,
                    writeMedicalData.toString(),
                ),
            )

            appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
            advanceUntilIdle()

            val revokeFitnessShouldIncludeBackgroundResult =
                appPermissionViewModel.revokeFitnessShouldIncludeBackground()
            assertThat(revokeFitnessShouldIncludeBackgroundResult).isTrue()
        }

    @Test
    fun revokeFitnessShouldIncludePastData_whenPastDataNotDeclared_returnsFalse() = runTest {
        whenever(healthPermissionReader.isRationaleIntentDeclared(any())).thenReturn(true)
        whenever(healthPermissionReader.getDeclaredHealthPermissions(any()))
            .thenReturn(
                listOf(
                    readExercisePermission.toString(),
                    readNutritionPermission.toString(),
                    writeSleepPermission.toString(),
                    writeDistancePermission.toString(),
                    readDataInBackgroundPermission.toString(),
                    readImmunization.toString(),
                    writeMedicalData.toString(),
                )
            )
        getGrantedHealthPermissionsUseCase.updateData(
            TEST_APP_PACKAGE_NAME,
            listOf(
                readNutritionPermission.toString(),
                writeSleepPermission.toString(),
                readDataInBackgroundPermission.additionalPermission,
                writeMedicalData.toString(),
            ),
        )

        appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        val revokeFitnessShouldIncludePastDataResult =
            appPermissionViewModel.revokeFitnessShouldIncludePastData()
        assertThat(revokeFitnessShouldIncludePastDataResult).isFalse()
    }

    @Test
    fun revokeFitnessShouldIncludePastData_whenNoFitnessReadDeclared_returnsFalse() = runTest {
        whenever(healthPermissionReader.isRationaleIntentDeclared(any())).thenReturn(true)
        whenever(healthPermissionReader.getDeclaredHealthPermissions(any()))
            .thenReturn(
                listOf(
                    writeSleepPermission.toString(),
                    writeDistancePermission.toString(),
                    readDataInBackgroundPermission.toString(),
                    readHistoryDataPermission.toString(),
                    readImmunization.toString(),
                    writeMedicalData.toString(),
                )
            )
        getGrantedHealthPermissionsUseCase.updateData(
            TEST_APP_PACKAGE_NAME,
            listOf(
                writeSleepPermission.toString(),
                readDataInBackgroundPermission.additionalPermission,
                writeMedicalData.toString(),
            ),
        )

        appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        val revokeFitnessShouldIncludePastDataResult =
            appPermissionViewModel.revokeFitnessShouldIncludePastData()
        assertThat(revokeFitnessShouldIncludePastDataResult).isFalse()
    }

    @Test
    fun revokeFitnessShouldIncludePastData_whenNoFitnessRead_returnsFalse() = runTest {
        whenever(healthPermissionReader.isRationaleIntentDeclared(any())).thenReturn(true)
        whenever(healthPermissionReader.getDeclaredHealthPermissions(any()))
            .thenReturn(
                listOf(
                    writeSleepPermission.toString(),
                    writeDistancePermission.toString(),
                    readHistoryDataPermission.toString(),
                    readDataInBackgroundPermission.toString(),
                    readImmunization.toString(),
                    writeMedicalData.toString(),
                )
            )
        getGrantedHealthPermissionsUseCase.updateData(
            TEST_APP_PACKAGE_NAME,
            listOf(
                writeSleepPermission.toString(),
                readHistoryDataPermission.additionalPermission,
                readDataInBackgroundPermission.additionalPermission,
                writeMedicalData.toString(),
            ),
        )

        appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        val revokeFitnessShouldIncludePastDataResult =
            appPermissionViewModel.revokeFitnessShouldIncludePastData()
        assertThat(revokeFitnessShouldIncludePastDataResult).isFalse()
    }

    @Test
    fun revokeFitnessShouldIncludePastData_whenMedicalRead_returnsFalse() = runTest {
        whenever(healthPermissionReader.isRationaleIntentDeclared(any())).thenReturn(true)
        whenever(healthPermissionReader.getDeclaredHealthPermissions(any()))
            .thenReturn(
                listOf(
                    readNutritionPermission.toString(),
                    writeSleepPermission.toString(),
                    writeDistancePermission.toString(),
                    readHistoryDataPermission.toString(),
                    readDataInBackgroundPermission.toString(),
                    readImmunization.toString(),
                    writeMedicalData.toString(),
                )
            )
        getGrantedHealthPermissionsUseCase.updateData(
            TEST_APP_PACKAGE_NAME,
            listOf(
                readNutritionPermission.toString(),
                writeSleepPermission.toString(),
                readHistoryDataPermission.additionalPermission,
                readDataInBackgroundPermission.additionalPermission,
                readImmunization.toString(),
                writeMedicalData.toString(),
            ),
        )

        appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        val revokeFitnessShouldIncludePastDataResult =
            appPermissionViewModel.revokeFitnessShouldIncludePastData()
        assertThat(revokeFitnessShouldIncludePastDataResult).isFalse()
    }

    @Test
    fun revokeFitnessShouldIncludePastData_whenPastDataDeclared_andFitnessRead_andNoMedicalRead_returnsTrue() =
        runTest {
            whenever(healthPermissionReader.isRationaleIntentDeclared(any())).thenReturn(true)
            whenever(healthPermissionReader.getDeclaredHealthPermissions(any()))
                .thenReturn(
                    listOf(
                        readNutritionPermission.toString(),
                        writeSleepPermission.toString(),
                        writeDistancePermission.toString(),
                        readHistoryDataPermission.toString(),
                        readDataInBackgroundPermission.toString(),
                        readImmunization.toString(),
                        writeMedicalData.toString(),
                    )
                )
            getGrantedHealthPermissionsUseCase.updateData(
                TEST_APP_PACKAGE_NAME,
                listOf(
                    readNutritionPermission.toString(),
                    writeSleepPermission.toString(),
                    readHistoryDataPermission.additionalPermission,
                    readDataInBackgroundPermission.additionalPermission,
                    writeMedicalData.toString(),
                ),
            )

            appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
            advanceUntilIdle()

            val revokeFitnessShouldIncludePastDataResult =
                appPermissionViewModel.revokeFitnessShouldIncludePastData()
            assertThat(revokeFitnessShouldIncludePastDataResult).isTrue()
        }

    @Test
    fun revokeMedicalShouldIncludeBackground_whenBgNotDeclared_returnsFalse() = runTest {
        whenever(healthPermissionReader.isRationaleIntentDeclared(any())).thenReturn(true)
        whenever(healthPermissionReader.getDeclaredHealthPermissions(any()))
            .thenReturn(
                listOf(
                    readExercisePermission.toString(),
                    readNutritionPermission.toString(),
                    writeSleepPermission.toString(),
                    writeDistancePermission.toString(),
                    readHistoryDataPermission.toString(),
                    readImmunization.toString(),
                    writeMedicalData.toString(),
                )
            )
        getGrantedHealthPermissionsUseCase.updateData(
            TEST_APP_PACKAGE_NAME,
            listOf(
                readExercisePermission.toString(),
                writeSleepPermission.toString(),
                readHistoryDataPermission.additionalPermission,
                readImmunization.toString(),
                writeMedicalData.toString(),
            ),
        )

        appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        val revokeMedicalShouldIncludeBackgroundResult =
            appPermissionViewModel.revokeMedicalShouldIncludeBackground()
        assertThat(revokeMedicalShouldIncludeBackgroundResult).isFalse()
    }

    @Test
    fun revokeMedicalShouldIncludeBackground_whenNoReadPermissionDeclared_returnsFalse() = runTest {
        whenever(healthPermissionReader.isRationaleIntentDeclared(any())).thenReturn(true)
        whenever(healthPermissionReader.getDeclaredHealthPermissions(any()))
            .thenReturn(
                listOf(
                    writeSleepPermission.toString(),
                    writeDistancePermission.toString(),
                    readDataInBackgroundPermission.toString(),
                    writeMedicalData.toString(),
                )
            )
        getGrantedHealthPermissionsUseCase.updateData(
            TEST_APP_PACKAGE_NAME,
            listOf(writeSleepPermission.toString(), writeMedicalData.toString()),
        )

        appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        val revokeMedicalShouldIncludeBackgroundResult =
            appPermissionViewModel.revokeMedicalShouldIncludeBackground()
        assertThat(revokeMedicalShouldIncludeBackgroundResult).isFalse()
    }

    @Test
    fun revokeMedicalShouldIncludeBackground_whenNoMedicalRead_returnsFalse() = runTest {
        whenever(healthPermissionReader.isRationaleIntentDeclared(any())).thenReturn(true)
        whenever(healthPermissionReader.getDeclaredHealthPermissions(any()))
            .thenReturn(
                listOf(
                    readExercisePermission.toString(),
                    readNutritionPermission.toString(),
                    writeSleepPermission.toString(),
                    writeDistancePermission.toString(),
                    readDataInBackgroundPermission.toString(),
                    readHistoryDataPermission.toString(),
                    writeMedicalData.toString(),
                )
            )
        getGrantedHealthPermissionsUseCase.updateData(
            TEST_APP_PACKAGE_NAME,
            listOf(
                writeSleepPermission.toString(),
                readDataInBackgroundPermission.additionalPermission,
                readHistoryDataPermission.additionalPermission,
                writeMedicalData.toString(),
            ),
        )

        appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        val revokeMedicalShouldIncludeBackgroundResult =
            appPermissionViewModel.revokeMedicalShouldIncludeBackground()
        assertThat(revokeMedicalShouldIncludeBackgroundResult).isFalse()
    }

    @Test
    fun revokeMedicalShouldIncludeBackground_whenFitnessRead_returnsFalse() = runTest {
        whenever(healthPermissionReader.isRationaleIntentDeclared(any())).thenReturn(true)
        whenever(healthPermissionReader.getDeclaredHealthPermissions(any()))
            .thenReturn(
                listOf(
                    readExercisePermission.toString(),
                    readNutritionPermission.toString(),
                    writeSleepPermission.toString(),
                    writeDistancePermission.toString(),
                    readDataInBackgroundPermission.toString(),
                    readHistoryDataPermission.toString(),
                    readImmunization.toString(),
                    writeMedicalData.toString(),
                )
            )
        getGrantedHealthPermissionsUseCase.updateData(
            TEST_APP_PACKAGE_NAME,
            listOf(
                readExercisePermission.toString(),
                writeSleepPermission.toString(),
                readDataInBackgroundPermission.additionalPermission,
                readHistoryDataPermission.additionalPermission,
                readImmunization.toString(),
                writeMedicalData.toString(),
            ),
        )

        appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        val revokeMedicalShouldIncludeBackgroundResult =
            appPermissionViewModel.revokeMedicalShouldIncludeBackground()
        assertThat(revokeMedicalShouldIncludeBackgroundResult).isFalse()
    }

    @Test
    fun revokeMedicalShouldIncludeBackground_whenBgDeclared_andMedicalRead_andNoFitnessRead_returnsTrue() =
        runTest {
            whenever(healthPermissionReader.isRationaleIntentDeclared(any())).thenReturn(true)
            whenever(healthPermissionReader.getDeclaredHealthPermissions(any()))
                .thenReturn(
                    listOf(
                        readExercisePermission.toString(),
                        readNutritionPermission.toString(),
                        writeSleepPermission.toString(),
                        writeDistancePermission.toString(),
                        readDataInBackgroundPermission.toString(),
                        readHistoryDataPermission.toString(),
                        readImmunization.toString(),
                        writeMedicalData.toString(),
                    )
                )
            getGrantedHealthPermissionsUseCase.updateData(
                TEST_APP_PACKAGE_NAME,
                listOf(
                    writeSleepPermission.toString(),
                    readDataInBackgroundPermission.additionalPermission,
                    readHistoryDataPermission.additionalPermission,
                    readImmunization.toString(),
                    writeMedicalData.toString(),
                ),
            )

            appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
            advanceUntilIdle()

            val revokeMedicalShouldIncludeBackgroundResult =
                appPermissionViewModel.revokeMedicalShouldIncludeBackground()
            assertThat(revokeMedicalShouldIncludeBackgroundResult).isTrue()
        }

    @Test
    fun revokeMedicalShouldIncludePastData_whenPastDataNotDeclared_returnsFalse() = runTest {
        whenever(healthPermissionReader.isRationaleIntentDeclared(any())).thenReturn(true)
        whenever(healthPermissionReader.getDeclaredHealthPermissions(any()))
            .thenReturn(
                listOf(
                    readExercisePermission.toString(),
                    readNutritionPermission.toString(),
                    writeSleepPermission.toString(),
                    writeDistancePermission.toString(),
                    readDataInBackgroundPermission.toString(),
                    readImmunization.toString(),
                    writeMedicalData.toString(),
                )
            )
        getGrantedHealthPermissionsUseCase.updateData(
            TEST_APP_PACKAGE_NAME,
            listOf(
                readExercisePermission.toString(),
                writeSleepPermission.toString(),
                readDataInBackgroundPermission.additionalPermission,
                readImmunization.toString(),
                writeMedicalData.toString(),
            ),
        )

        appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        val revokeMedicalShouldIncludePastDataResult =
            appPermissionViewModel.revokeMedicalShouldIncludePastData()
        assertThat(revokeMedicalShouldIncludePastDataResult).isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    fun revokeMedicalShouldIncludePastData_whenNoFitnessReadDeclared_returnsFalse() = runTest {
        whenever(healthPermissionReader.isRationaleIntentDeclared(any())).thenReturn(true)
        whenever(healthPermissionReader.getDeclaredHealthPermissions(any()))
            .thenReturn(
                listOf(
                    writeSleepPermission.toString(),
                    writeDistancePermission.toString(),
                    readHistoryDataPermission.toString(),
                    readImmunization.toString(),
                    writeMedicalData.toString(),
                )
            )
        getGrantedHealthPermissionsUseCase.updateData(
            TEST_APP_PACKAGE_NAME,
            listOf(
                writeSleepPermission.toString(),
                readImmunization.toString(),
                writeMedicalData.toString(),
            ),
        )

        appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        val revokeMedicalShouldIncludePastDataResult =
            appPermissionViewModel.revokeMedicalShouldIncludePastData()
        assertThat(revokeMedicalShouldIncludePastDataResult).isFalse()
    }

    @Test
    fun revokeMedicalShouldIncludePastData_whenNoMedicalRead_returnsFalse() = runTest {
        whenever(healthPermissionReader.isRationaleIntentDeclared(any())).thenReturn(true)
        whenever(healthPermissionReader.getDeclaredHealthPermissions(any()))
            .thenReturn(
                listOf(
                    readExercisePermission.toString(),
                    readNutritionPermission.toString(),
                    writeSleepPermission.toString(),
                    writeDistancePermission.toString(),
                    readHistoryDataPermission.toString(),
                    readDataInBackgroundPermission.toString(),
                    writeMedicalData.toString(),
                )
            )
        getGrantedHealthPermissionsUseCase.updateData(
            TEST_APP_PACKAGE_NAME,
            listOf(
                readExercisePermission.toString(),
                writeSleepPermission.toString(),
                readHistoryDataPermission.additionalPermission,
                readDataInBackgroundPermission.additionalPermission,
                writeMedicalData.toString(),
            ),
        )

        appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        val revokeMedicalShouldIncludePastDataResult =
            appPermissionViewModel.revokeMedicalShouldIncludePastData()
        assertThat(revokeMedicalShouldIncludePastDataResult).isFalse()
    }

    @Test
    fun revokeMedicalShouldIncludePastData_whenFitnessRead_returnsFalse() = runTest {
        whenever(healthPermissionReader.isRationaleIntentDeclared(any())).thenReturn(true)
        whenever(healthPermissionReader.getDeclaredHealthPermissions(any()))
            .thenReturn(
                listOf(
                    readExercisePermission.toString(),
                    readNutritionPermission.toString(),
                    writeSleepPermission.toString(),
                    writeDistancePermission.toString(),
                    readHistoryDataPermission.toString(),
                    readDataInBackgroundPermission.toString(),
                    readImmunization.toString(),
                    writeMedicalData.toString(),
                )
            )
        getGrantedHealthPermissionsUseCase.updateData(
            TEST_APP_PACKAGE_NAME,
            listOf(
                readExercisePermission.toString(),
                writeSleepPermission.toString(),
                readHistoryDataPermission.additionalPermission,
                readDataInBackgroundPermission.additionalPermission,
                readImmunization.toString(),
                writeMedicalData.toString(),
            ),
        )

        appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        val revokeMedicalShouldIncludePastDataResult =
            appPermissionViewModel.revokeMedicalShouldIncludePastData()
        assertThat(revokeMedicalShouldIncludePastDataResult).isFalse()
    }

    @Test
    fun revokeMedicalShouldIncludePastData_whenPastDataDeclared_andMedicalRead_andNoFitnessRead_returnsTrue() =
        runTest {
            whenever(healthPermissionReader.isRationaleIntentDeclared(any())).thenReturn(true)
            whenever(healthPermissionReader.getDeclaredHealthPermissions(any()))
                .thenReturn(
                    listOf(
                        readExercisePermission.toString(),
                        readNutritionPermission.toString(),
                        writeSleepPermission.toString(),
                        writeDistancePermission.toString(),
                        readHistoryDataPermission.toString(),
                        readDataInBackgroundPermission.toString(),
                        readImmunization.toString(),
                        writeMedicalData.toString(),
                    )
                )
            getGrantedHealthPermissionsUseCase.updateData(
                TEST_APP_PACKAGE_NAME,
                listOf(
                    writeSleepPermission.toString(),
                    readHistoryDataPermission.additionalPermission,
                    readDataInBackgroundPermission.additionalPermission,
                    readImmunization.toString(),
                    writeMedicalData.toString(),
                ),
            )

            appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
            advanceUntilIdle()

            val revokeMedicalShouldIncludePastDataResult =
                appPermissionViewModel.revokeMedicalShouldIncludePastData()
            assertThat(revokeMedicalShouldIncludePastDataResult).isTrue()
        }

    @Test
    fun revokeAllShouldIncludeBackground_whenBgNotDeclared_returnsFalse() = runTest {
        whenever(healthPermissionReader.isRationaleIntentDeclared(any())).thenReturn(true)
        whenever(healthPermissionReader.getDeclaredHealthPermissions(any()))
            .thenReturn(
                listOf(
                    readExercisePermission.toString(),
                    readNutritionPermission.toString(),
                    writeSleepPermission.toString(),
                    writeDistancePermission.toString(),
                    readHistoryDataPermission.toString(),
                    readImmunization.toString(),
                    writeMedicalData.toString(),
                )
            )
        getGrantedHealthPermissionsUseCase.updateData(
            TEST_APP_PACKAGE_NAME,
            listOf(
                readNutritionPermission.toString(),
                writeSleepPermission.toString(),
                readHistoryDataPermission.additionalPermission,
                readImmunization.toString(),
                writeMedicalData.toString(),
            ),
        )

        appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        val revokeAllShouldIncludeBackgroundResult =
            appPermissionViewModel.revokeAllShouldIncludeBackground()
        assertThat(revokeAllShouldIncludeBackgroundResult).isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    fun revokeAllShouldIncludeBackground_whenNoReadPermissionDeclared_returnsFalse() = runTest {
        whenever(healthPermissionReader.isRationaleIntentDeclared(any())).thenReturn(true)
        whenever(healthPermissionReader.getDeclaredHealthPermissions(any()))
            .thenReturn(
                listOf(
                    writeSleepPermission.toString(),
                    writeDistancePermission.toString(),
                    readDataInBackgroundPermission.toString(),
                    readHistoryDataPermission.toString(),
                    writeMedicalData.toString(),
                )
            )
        getGrantedHealthPermissionsUseCase.updateData(
            TEST_APP_PACKAGE_NAME,
            listOf(writeSleepPermission.toString(), writeMedicalData.toString()),
        )

        appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        val revokeAllShouldIncludeBackgroundResult =
            appPermissionViewModel.revokeAllShouldIncludeBackground()
        assertThat(revokeAllShouldIncludeBackgroundResult).isFalse()
    }

    @Test
    fun revokeAllShouldIncludeBackground_whenBgDeclared_andAtLeastOneReadPermissionDeclared_returnsTrue() =
        runTest {
            whenever(healthPermissionReader.isRationaleIntentDeclared(any())).thenReturn(true)
            whenever(healthPermissionReader.getDeclaredHealthPermissions(any()))
                .thenReturn(
                    listOf(
                        readNutritionPermission.toString(),
                        writeSleepPermission.toString(),
                        writeDistancePermission.toString(),
                        readHistoryDataPermission.toString(),
                        readDataInBackgroundPermission.toString(),
                        writeMedicalData.toString(),
                    )
                )
            getGrantedHealthPermissionsUseCase.updateData(
                TEST_APP_PACKAGE_NAME,
                listOf(
                    readNutritionPermission.toString(),
                    writeSleepPermission.toString(),
                    readDataInBackgroundPermission.toString(),
                    writeMedicalData.toString(),
                ),
            )

            appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
            advanceUntilIdle()

            val revokeAllShouldIncludeBackgroundResult =
                appPermissionViewModel.revokeAllShouldIncludeBackground()
            assertThat(revokeAllShouldIncludeBackgroundResult).isTrue()
        }

    @Test
    fun revokeAllShouldIncludePastData_whenPastDataNotDeclared_returnsFalse() = runTest {
        whenever(healthPermissionReader.isRationaleIntentDeclared(any())).thenReturn(true)
        whenever(healthPermissionReader.getDeclaredHealthPermissions(any()))
            .thenReturn(
                listOf(
                    readNutritionPermission.toString(),
                    writeSleepPermission.toString(),
                    writeDistancePermission.toString(),
                    readDataInBackgroundPermission.toString(),
                    writeMedicalData.toString(),
                )
            )
        getGrantedHealthPermissionsUseCase.updateData(
            TEST_APP_PACKAGE_NAME,
            listOf(
                readNutritionPermission.toString(),
                writeSleepPermission.toString(),
                writeMedicalData.toString(),
            ),
        )

        appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        val revokeAllShouldIncludePastDataResult =
            appPermissionViewModel.revokeAllShouldIncludePastData()
        assertThat(revokeAllShouldIncludePastDataResult).isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    fun revokeAllShouldIncludePastData_whenNoReadFitnessPermissionDeclared_returnsFalse() =
        runTest {
            whenever(healthPermissionReader.isRationaleIntentDeclared(any())).thenReturn(true)
            whenever(healthPermissionReader.getDeclaredHealthPermissions(any()))
                .thenReturn(
                    listOf(
                        writeSleepPermission.toString(),
                        writeDistancePermission.toString(),
                        readHistoryDataPermission.toString(),
                        writeMedicalData.toString(),
                    )
                )
            getGrantedHealthPermissionsUseCase.updateData(
                TEST_APP_PACKAGE_NAME,
                listOf(writeSleepPermission.toString(), writeMedicalData.toString()),
            )

            appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
            advanceUntilIdle()

            val revokeAllShouldIncludePastDataResult =
                appPermissionViewModel.revokeAllShouldIncludePastData()
            assertThat(revokeAllShouldIncludePastDataResult).isFalse()
        }

    @Test
    fun revokeAllShouldIncludePastData_whenPastDataDeclared_andAtLeastOneReadFitnessPermissionDeclared_returnsTrue() =
        runTest {
            whenever(healthPermissionReader.isRationaleIntentDeclared(any())).thenReturn(true)
            whenever(healthPermissionReader.getDeclaredHealthPermissions(any()))
                .thenReturn(
                    listOf(
                        readNutritionPermission.toString(),
                        writeSleepPermission.toString(),
                        writeDistancePermission.toString(),
                        readHistoryDataPermission.toString(),
                        writeMedicalData.toString(),
                    )
                )
            getGrantedHealthPermissionsUseCase.updateData(
                TEST_APP_PACKAGE_NAME,
                listOf(writeSleepPermission.toString(), writeMedicalData.toString()),
            )

            appPermissionViewModel.loadPermissionsForPackage(TEST_APP_PACKAGE_NAME)
            advanceUntilIdle()

            val revokeAllShouldIncludePastDataResult =
                appPermissionViewModel.revokeAllShouldIncludePastData()
            assertThat(revokeAllShouldIncludePastDataResult).isTrue()
        }

    private fun setupDeclaredAndGrantedFitnessPermissions() {
        whenever(healthPermissionReader.isRationaleIntentDeclared(any())).thenReturn(true)
        whenever(healthPermissionReader.getDeclaredHealthPermissions(any()))
            .thenReturn(
                listOf(
                    readExercisePermission.toString(),
                    readNutritionPermission.toString(),
                    writeSleepPermission.toString(),
                    writeDistancePermission.toString(),
                )
            )
        getGrantedHealthPermissionsUseCase.updateData(
            TEST_APP_PACKAGE_NAME,
            listOf(readExercisePermission.toString(), writeDistancePermission.toString()),
        )
    }

    private fun setupDeclaredAndGrantedFitnessAndMedicalPermissions() {
        whenever(healthPermissionReader.isRationaleIntentDeclared(any())).thenReturn(true)
        whenever(healthPermissionReader.getDeclaredHealthPermissions(any()))
            .thenReturn(
                listOf(
                    readExercisePermission.toString(),
                    readNutritionPermission.toString(),
                    readImmunization.toString(),
                    writeSleepPermission.toString(),
                    writeDistancePermission.toString(),
                    writeMedicalData.toString(),
                )
            )
        getGrantedHealthPermissionsUseCase.updateData(
            TEST_APP_PACKAGE_NAME,
            listOf(readExercisePermission.toString(), readImmunization.toString()),
        )
    }
}
