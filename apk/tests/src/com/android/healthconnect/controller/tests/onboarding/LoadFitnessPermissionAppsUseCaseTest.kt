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

package com.android.healthconnect.controller.tests.onboarding

import android.content.Context
import android.content.Intent
import android.health.connect.HealthConnectManager.ACTION_SHOW_ONBOARDING
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.onboarding.ConnectedFitnessAppMetadata
import com.android.healthconnect.controller.onboarding.LoadFitnessPermissionAppsUseCase
import com.android.healthconnect.controller.permissions.app.LoadAppPermissionsStatusUseCase
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.permissions.data.HealthPermission.AdditionalPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.FitnessPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.MedicalPermission
import com.android.healthconnect.controller.permissions.data.MedicalPermissionType
import com.android.healthconnect.controller.permissions.data.PermissionsAccessType
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.shared.app.AppInfoReader
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import com.android.healthconnect.controller.tests.utils.InstantTaskExecutorRule
import com.android.healthconnect.controller.tests.utils.di.FakeGetGrantedHealthPermissionsUseCase
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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class LoadFitnessPermissionAppsUseCaseTest {
    @get:Rule val hiltRule = HiltAndroidRule(this)
    private val healthPermissionReader: HealthPermissionReader = mock()

    private lateinit var context: Context
    private lateinit var loadAppPermissionsStatusUseCase: LoadAppPermissionsStatusUseCase
    private val getGrantedHealthPermissionsUseCase = FakeGetGrantedHealthPermissionsUseCase()
    private lateinit var useCase: LoadFitnessPermissionAppsUseCase
    private var appInfoReader: AppInfoReader = mock()

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()
    private val testDispatcher = UnconfinedTestDispatcher()

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
    private val writeMedicalData = MedicalPermission(MedicalPermissionType.ALL_MEDICAL_DATA)
    private val medicalAppPackageName = "medical.app"
    private val combinedAppPackageName = "combined.app"
    private val fitnessAppPackageName = "fitness.app"
    private val fitnessAppPackageName2 = "fitness.app.2"
    private val systemAppPackageName = "system.app"
    private val medicalApp = AppMetadata(medicalAppPackageName, "MedicalApp", null)
    private val fitnessApp = AppMetadata(fitnessAppPackageName, "FitnessApp", null)
    private val fitnessApp2 = AppMetadata(fitnessAppPackageName2, "FitnessApp2", null)
    private val combinedApp = AppMetadata(combinedAppPackageName, "CombinedApp", null)
    private val systemApp = AppMetadata(systemAppPackageName, "SystemApp", null, isSystem = true)

    @Before
    fun setup() {
        hiltRule.inject()
        Dispatchers.setMain(testDispatcher)
        context = InstrumentationRegistry.getInstrumentation().context
        loadAppPermissionsStatusUseCase =
            LoadAppPermissionsStatusUseCase(
                getGrantedHealthPermissionsUseCase,
                healthPermissionReader,
                Dispatchers.Main,
            )
        useCase =
            LoadFitnessPermissionAppsUseCase(
                context,
                healthPermissionReader,
                loadAppPermissionsStatusUseCase,
                appInfoReader,
                Dispatchers.Main,
            )

        whenever(healthPermissionReader.getAppsWithHealthPermissions())
            .thenReturn(
                mapOf(
                    medicalAppPackageName to false,
                    combinedAppPackageName to false,
                    fitnessAppPackageName to false,
                    fitnessAppPackageName2 to false,
                    systemAppPackageName to true,
                )
            )
        whenever(healthPermissionReader.getValidHealthPermissions(medicalAppPackageName))
            .thenReturn(
                listOf(writeMedicalData, readAllergies, readImmunization, readHistoryDataPermission)
            )
        whenever(healthPermissionReader.getValidHealthPermissions(combinedAppPackageName))
            .thenReturn(
                listOf(
                    writeMedicalData,
                    readAllergies,
                    readExercisePermission,
                    readHeartRatePermission,
                    readDataInBackgroundPermission,
                )
            )
        whenever(healthPermissionReader.getValidHealthPermissions(fitnessAppPackageName))
            .thenReturn(
                listOf(
                    readSkinTemperaturePermission,
                    readHeartRatePermission,
                    readHistoryDataPermission,
                )
            )
        whenever(healthPermissionReader.getValidHealthPermissions(fitnessAppPackageName2))
            .thenReturn(
                listOf(
                    readExercisePermission,
                    readSkinTemperaturePermission,
                    readHeartRatePermission,
                    readHistoryDataPermission,
                )
            )
        whenever(healthPermissionReader.getValidHealthPermissions(systemAppPackageName))
            .thenReturn(listOf(readSkinTemperaturePermission, readHeartRatePermission))
        getGrantedHealthPermissionsUseCase.updateData(
            systemAppPackageName,
            listOf(readSkinTemperaturePermission.toString(), readHeartRatePermission.toString()),
        )
        getGrantedHealthPermissionsUseCase.updateData(
            fitnessAppPackageName2,
            listOf(readExercisePermission.toString(), readHistoryDataPermission.toString()),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        getGrantedHealthPermissionsUseCase.reset()
    }

    @Test
    fun filtersOutAppsWithNoFitnessPermissions() = runTest {
        mockAppMetadata()

        getGrantedHealthPermissionsUseCase.updateData(
            medicalAppPackageName,
            listOf(writeMedicalData.toString(), readAllergies.toString()),
        )
        getGrantedHealthPermissionsUseCase.updateData(
            combinedAppPackageName,
            listOf(
                writeMedicalData.toString(),
                readAllergies.toString(),
                readHistoryDataPermission.toString(),
            ),
        )
        getGrantedHealthPermissionsUseCase.updateData(
            fitnessAppPackageName,
            listOf(readSkinTemperaturePermission.toString(), readHeartRatePermission.toString()),
        )

        val actual = useCase.invoke(Unit)
        advanceUntilIdle()

        assertThat(actual is UseCaseResults.Success).isTrue()
        assertThat((actual as UseCaseResults.Success).data)
            .containsExactlyElementsIn(
                listOf(
                    ConnectedFitnessAppMetadata(fitnessApp, true),
                    ConnectedFitnessAppMetadata(fitnessApp2, true),
                    ConnectedFitnessAppMetadata(combinedApp, false),
                )
            )
    }

    @Test
    fun appsWithNoFitnessPermissionsGranted_areNotConnected() = runTest {
        mockAppMetadata()

        getGrantedHealthPermissionsUseCase.updateData(
            medicalAppPackageName,
            listOf(writeMedicalData.toString(), readAllergies.toString()),
        )
        getGrantedHealthPermissionsUseCase.updateData(
            combinedAppPackageName,
            listOf(writeMedicalData.toString(), readAllergies.toString()),
        )
        getGrantedHealthPermissionsUseCase.updateData(
            fitnessAppPackageName,
            listOf(readDataInBackgroundPermission.toString()),
        )

        val actual = useCase.invoke(Unit)
        advanceUntilIdle()

        assertThat(actual is UseCaseResults.Success).isTrue()
        assertThat((actual as UseCaseResults.Success).data)
            .containsExactlyElementsIn(
                listOf(
                    ConnectedFitnessAppMetadata(fitnessApp2, true),
                    ConnectedFitnessAppMetadata(fitnessApp, false),
                    ConnectedFitnessAppMetadata(combinedApp, false),
                )
            )
    }

    @Test
    fun appsWithAtLeastOneFitnessPermissionGranted_areConnected() = runTest {
        mockAppMetadata()

        getGrantedHealthPermissionsUseCase.updateData(
            medicalAppPackageName,
            listOf(writeMedicalData.toString(), readAllergies.toString()),
        )
        getGrantedHealthPermissionsUseCase.updateData(
            combinedAppPackageName,
            listOf(
                writeMedicalData.toString(),
                readAllergies.toString(),
                readExercisePermission.toString(),
            ),
        )
        getGrantedHealthPermissionsUseCase.updateData(
            fitnessAppPackageName,
            listOf(readSkinTemperaturePermission.toString()),
        )

        val actual = useCase.invoke(Unit)
        advanceUntilIdle()

        assertThat(actual is UseCaseResults.Success).isTrue()
        assertThat((actual as UseCaseResults.Success).data)
            .containsExactlyElementsIn(
                listOf(
                    ConnectedFitnessAppMetadata(combinedApp, true),
                    ConnectedFitnessAppMetadata(fitnessApp, true),
                    ConnectedFitnessAppMetadata(fitnessApp2, true),
                )
            )
    }

    @Test
    fun appsWithOnlyMedicalPermissionsGranted_areNotConnected() = runTest {
        mockAppMetadata()

        getGrantedHealthPermissionsUseCase.updateData(
            medicalAppPackageName,
            listOf(writeMedicalData.toString(), readAllergies.toString()),
        )
        getGrantedHealthPermissionsUseCase.updateData(
            combinedAppPackageName,
            listOf(writeMedicalData.toString()),
        )
        getGrantedHealthPermissionsUseCase.updateData(
            fitnessAppPackageName,
            listOf(readSkinTemperaturePermission.toString()),
        )

        val actual = useCase.invoke(Unit)
        advanceUntilIdle()

        assertThat(actual is UseCaseResults.Success).isTrue()
        assertThat((actual as UseCaseResults.Success).data)
            .containsExactlyElementsIn(
                listOf(
                    ConnectedFitnessAppMetadata(fitnessApp, true),
                    ConnectedFitnessAppMetadata(fitnessApp2, true),
                    ConnectedFitnessAppMetadata(combinedApp, false),
                )
            )
    }

    @Test
    fun filtersOutSystemApps() = runTest {
        mockAppMetadata()
        getGrantedHealthPermissionsUseCase.updateData(
            medicalAppPackageName,
            listOf(writeMedicalData.toString(), readAllergies.toString()),
        )
        getGrantedHealthPermissionsUseCase.updateData(
            combinedAppPackageName,
            listOf(writeMedicalData.toString()),
        )
        getGrantedHealthPermissionsUseCase.updateData(
            fitnessAppPackageName,
            listOf(readSkinTemperaturePermission.toString()),
        )

        val actual = useCase.invoke(Unit)
        advanceUntilIdle()

        assertThat(actual is UseCaseResults.Success).isTrue()
        assertThat((actual as UseCaseResults.Success).data)
            .containsExactlyElementsIn(
                listOf(
                    ConnectedFitnessAppMetadata(fitnessApp, true),
                    ConnectedFitnessAppMetadata(fitnessApp2, true),
                    ConnectedFitnessAppMetadata(combinedApp, false),
                )
            )
    }

    @Test
    fun returnsCorrectAppsWithOnboarding() = runTest {
        mockAppMetadata()
        val testIntent = Intent(ACTION_SHOW_ONBOARDING)
        testIntent.setPackage(fitnessAppPackageName2)
        whenever(
                healthPermissionReader.getOnboardingActivityIntent(
                    any(),
                    eq(fitnessAppPackageName2),
                )
            )
            .thenReturn(testIntent)
        getGrantedHealthPermissionsUseCase.updateData(
            medicalAppPackageName,
            listOf(writeMedicalData.toString(), readAllergies.toString()),
        )
        getGrantedHealthPermissionsUseCase.updateData(
            combinedAppPackageName,
            listOf(writeMedicalData.toString()),
        )
        getGrantedHealthPermissionsUseCase.updateData(
            fitnessAppPackageName,
            listOf(readSkinTemperaturePermission.toString()),
        )

        val actual = useCase.invoke(Unit)
        advanceUntilIdle()

        assertThat(actual is UseCaseResults.Success).isTrue()
        assertThat((actual as UseCaseResults.Success).data)
            .containsExactlyElementsIn(
                listOf(
                    ConnectedFitnessAppMetadata(
                        appMetadata = fitnessApp,
                        isConnected = true,
                        hasOnboarding = false,
                    ),
                    ConnectedFitnessAppMetadata(
                        appMetadata = fitnessApp2,
                        isConnected = true,
                        hasOnboarding = true,
                    ),
                    ConnectedFitnessAppMetadata(
                        appMetadata = combinedApp,
                        isConnected = false,
                        hasOnboarding = false,
                    ),
                )
            )
    }

    private suspend fun mockAppMetadata() {
        whenever(appInfoReader.getAppMetadata(medicalAppPackageName, false)).thenReturn(medicalApp)
        whenever(appInfoReader.getAppMetadata(fitnessAppPackageName, false)).thenReturn(fitnessApp)
        whenever(appInfoReader.getAppMetadata(fitnessAppPackageName2, false))
            .thenReturn(fitnessApp2)
        whenever(appInfoReader.getAppMetadata(combinedAppPackageName, false))
            .thenReturn(combinedApp)
        whenever(appInfoReader.getAppMetadata(eq(systemAppPackageName), any()))
            .thenReturn(systemApp)
    }
}
