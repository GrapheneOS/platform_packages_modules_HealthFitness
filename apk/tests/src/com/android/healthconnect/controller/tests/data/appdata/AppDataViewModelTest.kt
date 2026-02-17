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
package com.android.healthconnect.controller.tests.data.appdata

import android.content.Context
import android.health.connect.HealthDataCategory
import android.platform.test.annotations.RequiresFlagsDisabled
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.data.api.PermissionTypesPerCategory
import com.android.healthconnect.controller.data.appdata.AppDataViewModel
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.permissions.data.MedicalPermissionType.VACCINES
import com.android.healthconnect.controller.permissions.data.getAllSymptomPermissionTypes
import com.android.healthconnect.controller.selectabledeletion.DeletionDataViewModel.DeletionScreenState.DELETE
import com.android.healthconnect.controller.selectabledeletion.DeletionDataViewModel.DeletionScreenState.VIEW
import com.android.healthconnect.controller.shared.HealthDataCategoryExtensions.MEDICAL
import com.android.healthconnect.controller.shared.app.AppInfoReader
import com.android.healthconnect.controller.tests.data.appdata.api.FakeGetAppFitnessPermissionTypesUseCase
import com.android.healthconnect.controller.tests.data.appdata.api.FakeGetAppMedicalPermissionTypesUseCase
import com.android.healthconnect.controller.tests.utils.FakeUseCaseRule
import com.android.healthconnect.controller.tests.utils.InstantTaskExecutorRule
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME_2
import com.android.healthconnect.controller.tests.utils.TEST_MEDICAL_DATA_SOURCE
import com.android.healthconnect.controller.tests.utils.TEST_MEDICAL_DATA_SOURCE_DIFFERENT_APP
import com.android.healthconnect.controller.tests.utils.TestObserver
import com.android.healthconnect.controller.tests.utils.createFakeAppInfoReader
import com.android.healthconnect.controller.tests.utils.setLocale
import com.android.healthfitness.flags.Flags
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.util.Locale
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

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AppDataViewModelTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @get:Rule val checkFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()
    @get:Rule val fakeUseCaseRule = FakeUseCaseRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    @BindValue lateinit var appInfoReader: AppInfoReader

    private val getAppFitnessPermissionTypesWithDataUseCase =
        fakeUseCaseRule.watch(FakeGetAppFitnessPermissionTypesUseCase())
    private val getAppMedicalPermissionTypesWithDataUseCase =
        fakeUseCaseRule.watch(FakeGetAppMedicalPermissionTypesUseCase())
    private lateinit var viewModel: AppDataViewModel
    private lateinit var context: Context

    @Before
    fun setup() = runTest {
        context = InstrumentationRegistry.getInstrumentation().context
        context.setLocale(Locale.US)
        appInfoReader = createFakeAppInfoReader()
        hiltRule.inject()
        Dispatchers.setMain(testDispatcher)
        viewModel =
            AppDataViewModel(
                appInfoReader,
                getAppFitnessPermissionTypesWithDataUseCase,
                getAppMedicalPermissionTypesWithDataUseCase,
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_SYMPTOMS, Flags.FLAG_SYMPTOMS_DB)
    fun noData_symptomsFlagEnabled_returnsEmptyList() = runTest {
        getAppFitnessPermissionTypesWithDataUseCase.setPermissionTypesForApp(
            TEST_APP_PACKAGE_NAME,
            listOf(),
        )

        val testObserver = TestObserver<AppDataViewModel.AppDataState>()
        viewModel.appFitnessData.observeForever(testObserver)
        viewModel.loadAppData(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        assertThat(testObserver.getLastValue())
            .isEqualTo(AppDataViewModel.AppDataState.WithData(listOf()))
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_SYMPTOMS, Flags.FLAG_SYMPTOMS_DB)
    fun noData_symptomsFlagDisabled_returnsEmptyList() = runTest {
        getAppFitnessPermissionTypesWithDataUseCase.setPermissionTypesForApp(
            TEST_APP_PACKAGE_NAME,
            listOf(),
        )

        val testObserver = TestObserver<AppDataViewModel.AppDataState>()
        viewModel.appFitnessData.observeForever(testObserver)
        viewModel.loadAppData(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()
        assertThat(testObserver.getLastValue())
            .isEqualTo(AppDataViewModel.AppDataState.WithData(listOf()))
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_SYMPTOMS, Flags.FLAG_SYMPTOMS_DB)
    fun symptomsOnly_symptomsFlagDisabled_returnsEmptyList() = runTest {
        val permissionTypesPerCategory =
            listOf(
                PermissionTypesPerCategory(
                    category = HealthDataCategory.SYMPTOMS,
                    data = listOf(FitnessPermissionType.SYMPTOM_ABDOMINAL_PAIN),
                )
            )
        getAppFitnessPermissionTypesWithDataUseCase.setPermissionTypesForApp(
            TEST_APP_PACKAGE_NAME,
            permissionTypesPerCategory,
        )

        val testObserver = TestObserver<AppDataViewModel.AppDataState>()
        viewModel.appFitnessData.observeForever(testObserver)
        viewModel.loadAppData(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        assertThat(testObserver.getLastValue())
            .isEqualTo(AppDataViewModel.AppDataState.WithData(listOf()))
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_SYMPTOMS, Flags.FLAG_SYMPTOMS_DB)
    fun fitnessData_symptomsFlagEnabled_returnsDataWrittenByGivenApp() = runTest {
        val permissionTypesPerCategoryForTestApp =
            listOf(
                PermissionTypesPerCategory(
                    category = HealthDataCategory.ACTIVITY,
                    data = listOf(FitnessPermissionType.STEPS),
                ),
                PermissionTypesPerCategory(
                    category = HealthDataCategory.VITALS,
                    data = listOf(FitnessPermissionType.HEART_RATE),
                ),
                PermissionTypesPerCategory(
                    category = HealthDataCategory.SYMPTOMS,
                    data = listOf(FitnessPermissionType.SYMPTOM_ABDOMINAL_PAIN),
                ),
            )

        val permissionTypesPerCategoryForTestApp2 =
            listOf(
                PermissionTypesPerCategory(
                    category = HealthDataCategory.ACTIVITY,
                    data = listOf(FitnessPermissionType.STEPS),
                ),
                PermissionTypesPerCategory(
                    category = HealthDataCategory.BODY_MEASUREMENTS,
                    data = listOf(FitnessPermissionType.WEIGHT),
                ),
                PermissionTypesPerCategory(
                    category = HealthDataCategory.SYMPTOMS,
                    data = listOf(FitnessPermissionType.SYMPTOM_ABDOMINAL_PAIN),
                ),
            )
        getAppFitnessPermissionTypesWithDataUseCase.setPermissionTypesForApp(
            TEST_APP_PACKAGE_NAME,
            permissionTypesPerCategoryForTestApp,
        )
        getAppFitnessPermissionTypesWithDataUseCase.setPermissionTypesForApp(
            TEST_APP_PACKAGE_NAME_2,
            permissionTypesPerCategoryForTestApp2,
        )

        val testObserver = TestObserver<AppDataViewModel.AppDataState>()
        viewModel.appFitnessData.observeForever(testObserver)
        viewModel.loadAppData(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        val expected =
            listOf(
                PermissionTypesPerCategory(
                    HealthDataCategory.ACTIVITY,
                    listOf(FitnessPermissionType.STEPS),
                ),
                PermissionTypesPerCategory(
                    HealthDataCategory.VITALS,
                    listOf(FitnessPermissionType.HEART_RATE),
                ),
                // Special case for symptoms since we show "All symptoms" on the screen
                PermissionTypesPerCategory(
                    HealthDataCategory.SYMPTOMS,
                    listOf(FitnessPermissionType.SYMPTOM_ABDOMINAL_PAIN),
                ),
            )
        assertThat(testObserver.getLastValue())
            .isEqualTo(AppDataViewModel.AppDataState.WithData(expected))
        assertThat(viewModel.getTheNumOfPermissionTypes())
            .isEqualTo(2 + getAllSymptomPermissionTypes().size)
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_SYMPTOMS, Flags.FLAG_SYMPTOMS_DB)
    fun fitnessData_symptomsFlagDisabled_returnsDataWrittenByGivenApp() = runTest {
        val permissionTypesPerCategoryForTestApp =
            listOf(
                PermissionTypesPerCategory(
                    category = HealthDataCategory.ACTIVITY,
                    data = listOf(FitnessPermissionType.STEPS),
                ),
                PermissionTypesPerCategory(
                    category = HealthDataCategory.VITALS,
                    data = listOf(FitnessPermissionType.HEART_RATE),
                ),
            )

        val permissionTypesPerCategoryForTestApp2 =
            listOf(
                PermissionTypesPerCategory(
                    category = HealthDataCategory.ACTIVITY,
                    data = listOf(FitnessPermissionType.STEPS),
                ),
                PermissionTypesPerCategory(
                    category = HealthDataCategory.BODY_MEASUREMENTS,
                    data = listOf(FitnessPermissionType.WEIGHT),
                ),
            )
        getAppFitnessPermissionTypesWithDataUseCase.setPermissionTypesForApp(
            TEST_APP_PACKAGE_NAME,
            permissionTypesPerCategoryForTestApp,
        )
        getAppFitnessPermissionTypesWithDataUseCase.setPermissionTypesForApp(
            TEST_APP_PACKAGE_NAME_2,
            permissionTypesPerCategoryForTestApp2,
        )

        val testObserver = TestObserver<AppDataViewModel.AppDataState>()
        viewModel.appFitnessData.observeForever(testObserver)
        viewModel.loadAppData(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        val expected =
            listOf(
                PermissionTypesPerCategory(
                    HealthDataCategory.ACTIVITY,
                    listOf(FitnessPermissionType.STEPS),
                ),
                PermissionTypesPerCategory(
                    HealthDataCategory.VITALS,
                    listOf(FitnessPermissionType.HEART_RATE),
                ),
            )
        assertThat(testObserver.getLastValue())
            .isEqualTo(AppDataViewModel.AppDataState.WithData(expected))
        assertThat(viewModel.getTheNumOfPermissionTypes()).isEqualTo(2)
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_SYMPTOMS, Flags.FLAG_SYMPTOMS_DB)
    fun fitnessAndMedicalData_symptomsFlagEnabled_returnsDataWrittenByGivenApp() = runTest {
        val permissionTypesPerCategoryForTestApp =
            listOf(
                PermissionTypesPerCategory(
                    category = HealthDataCategory.ACTIVITY,
                    data = listOf(FitnessPermissionType.STEPS),
                ),
                PermissionTypesPerCategory(
                    category = HealthDataCategory.VITALS,
                    data = listOf(FitnessPermissionType.HEART_RATE),
                ),
                PermissionTypesPerCategory(
                    category = HealthDataCategory.SYMPTOMS,
                    data = listOf(FitnessPermissionType.SYMPTOM_ABDOMINAL_PAIN),
                ),
            )

        val permissionTypesPerCategoryForTestApp2 =
            listOf(
                PermissionTypesPerCategory(
                    category = HealthDataCategory.ACTIVITY,
                    data = listOf(FitnessPermissionType.STEPS),
                ),
                PermissionTypesPerCategory(
                    category = HealthDataCategory.BODY_MEASUREMENTS,
                    data = listOf(FitnessPermissionType.WEIGHT),
                ),
                PermissionTypesPerCategory(
                    category = HealthDataCategory.SYMPTOMS,
                    data = listOf(FitnessPermissionType.SYMPTOM_ABDOMINAL_PAIN),
                ),
            )
        getAppFitnessPermissionTypesWithDataUseCase.setPermissionTypesForApp(
            TEST_APP_PACKAGE_NAME,
            permissionTypesPerCategoryForTestApp,
        )
        getAppFitnessPermissionTypesWithDataUseCase.setPermissionTypesForApp(
            TEST_APP_PACKAGE_NAME_2,
            permissionTypesPerCategoryForTestApp2,
        )
        getAppMedicalPermissionTypesWithDataUseCase.setPermissionTypesForApp(
            TEST_MEDICAL_DATA_SOURCE.packageName,
            listOf(PermissionTypesPerCategory(category = MEDICAL, data = listOf(VACCINES))),
        )

        val testObserver = TestObserver<AppDataViewModel.AppDataState>()
        viewModel.fitnessAndMedicalData.observeForever(testObserver)
        viewModel.loadAppData(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        val expected =
            listOf(
                PermissionTypesPerCategory(
                    HealthDataCategory.ACTIVITY,
                    listOf(FitnessPermissionType.STEPS),
                ),
                PermissionTypesPerCategory(
                    HealthDataCategory.VITALS,
                    listOf(FitnessPermissionType.HEART_RATE),
                ),
                // Special case for symptoms since we show "All symptoms" on the screen
                PermissionTypesPerCategory(
                    HealthDataCategory.SYMPTOMS,
                    listOf(FitnessPermissionType.SYMPTOM_ABDOMINAL_PAIN),
                ),
                PermissionTypesPerCategory(MEDICAL, listOf(VACCINES)),
            )
        assertThat(testObserver.getLastValue())
            .isEqualTo(AppDataViewModel.AppDataState.WithData(expected))
        assertThat(viewModel.getTheNumOfPermissionTypes())
            .isEqualTo(3 + getAllSymptomPermissionTypes().size)
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_SYMPTOMS, Flags.FLAG_SYMPTOMS_DB)
    fun fitnessAndMedicalData_symptomsFlagDisabled_returnsDataWrittenByGivenApp() = runTest {
        val permissionTypesPerCategoryForTestApp =
            listOf(
                PermissionTypesPerCategory(
                    category = HealthDataCategory.ACTIVITY,
                    data = listOf(FitnessPermissionType.STEPS),
                ),
                PermissionTypesPerCategory(
                    category = HealthDataCategory.VITALS,
                    data = listOf(FitnessPermissionType.HEART_RATE),
                ),
            )

        val permissionTypesPerCategoryForTestApp2 =
            listOf(
                PermissionTypesPerCategory(
                    category = HealthDataCategory.ACTIVITY,
                    data = listOf(FitnessPermissionType.STEPS),
                ),
                PermissionTypesPerCategory(
                    category = HealthDataCategory.BODY_MEASUREMENTS,
                    data = listOf(FitnessPermissionType.WEIGHT),
                ),
            )
        getAppFitnessPermissionTypesWithDataUseCase.setPermissionTypesForApp(
            TEST_APP_PACKAGE_NAME,
            permissionTypesPerCategoryForTestApp,
        )
        getAppFitnessPermissionTypesWithDataUseCase.setPermissionTypesForApp(
            TEST_APP_PACKAGE_NAME_2,
            permissionTypesPerCategoryForTestApp2,
        )
        getAppMedicalPermissionTypesWithDataUseCase.setPermissionTypesForApp(
            TEST_MEDICAL_DATA_SOURCE.packageName,
            listOf(PermissionTypesPerCategory(category = MEDICAL, data = listOf(VACCINES))),
        )

        val testObserver = TestObserver<AppDataViewModel.AppDataState>()
        viewModel.fitnessAndMedicalData.observeForever(testObserver)
        viewModel.loadAppData(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        val expected =
            listOf(
                PermissionTypesPerCategory(
                    HealthDataCategory.ACTIVITY,
                    listOf(FitnessPermissionType.STEPS),
                ),
                PermissionTypesPerCategory(
                    HealthDataCategory.VITALS,
                    listOf(FitnessPermissionType.HEART_RATE),
                ),
                PermissionTypesPerCategory(MEDICAL, listOf(VACCINES)),
            )
        assertThat(testObserver.getLastValue())
            .isEqualTo(AppDataViewModel.AppDataState.WithData(expected))
        assertThat(viewModel.getTheNumOfPermissionTypes()).isEqualTo(3)
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_SYMPTOMS, Flags.FLAG_SYMPTOMS_DB)
    fun medicalDataOnly_symptomsFlagEnabled_returnsDataWrittenByGivenApp() = runTest {
        getAppMedicalPermissionTypesWithDataUseCase.setPermissionTypesForApp(
            TEST_MEDICAL_DATA_SOURCE.packageName,
            listOf(PermissionTypesPerCategory(category = MEDICAL, data = listOf(VACCINES))),
        )

        val testObserver = TestObserver<AppDataViewModel.AppDataState>()
        viewModel.fitnessAndMedicalData.observeForever(testObserver)
        viewModel.loadAppData(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        val expected = listOf(PermissionTypesPerCategory(MEDICAL, listOf(VACCINES)))
        assertThat(testObserver.getLastValue())
            .isEqualTo(AppDataViewModel.AppDataState.WithData(expected))
        assertThat(viewModel.getTheNumOfPermissionTypes()).isEqualTo(1)
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_SYMPTOMS, Flags.FLAG_SYMPTOMS_DB)
    fun medicalDataOnly_symptomsFlagDisabled_returnsDataWrittenByGivenApp() = runTest {
        getAppMedicalPermissionTypesWithDataUseCase.setPermissionTypesForApp(
            TEST_MEDICAL_DATA_SOURCE.packageName,
            listOf(PermissionTypesPerCategory(category = MEDICAL, data = listOf(VACCINES))),
        )

        val testObserver = TestObserver<AppDataViewModel.AppDataState>()
        viewModel.fitnessAndMedicalData.observeForever(testObserver)
        viewModel.loadAppData(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        val expected = listOf(PermissionTypesPerCategory(MEDICAL, listOf(VACCINES)))
        assertThat(testObserver.getLastValue())
            .isEqualTo(AppDataViewModel.AppDataState.WithData(expected))
        assertThat(viewModel.getTheNumOfPermissionTypes()).isEqualTo(1)
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_SYMPTOMS, Flags.FLAG_SYMPTOMS_DB)
    fun medicalDataFromDifferentAppOnly_symptomsFlagEnabled_returnsNoMedicalData() = runTest {
        getAppMedicalPermissionTypesWithDataUseCase.setPermissionTypesForApp(
            TEST_MEDICAL_DATA_SOURCE_DIFFERENT_APP.packageName,
            listOf(PermissionTypesPerCategory(category = MEDICAL, data = listOf(VACCINES))),
        )

        val testObserver = TestObserver<AppDataViewModel.AppDataState>()
        viewModel.fitnessAndMedicalData.observeForever(testObserver)
        viewModel.loadAppData(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        val expected = listOf<PermissionTypesPerCategory>()
        assertThat(testObserver.getLastValue())
            .isEqualTo(AppDataViewModel.AppDataState.WithData(expected))
        assertThat(viewModel.getTheNumOfPermissionTypes()).isEqualTo(0)
    }

    @Test
    fun addToDeleteSet_updatesDeleteSetCorrectly() = runTest {
        assertThat(viewModel.setOfPermissionTypesToBeDeleted.value.orEmpty()).isEmpty()

        viewModel.addToDeletionSet(FitnessPermissionType.DISTANCE)

        assertThat(viewModel.setOfPermissionTypesToBeDeleted.value)
            .containsExactly(FitnessPermissionType.DISTANCE)
    }

    @Test
    fun removeFromDeleteSet_updatesDeleteSetCorrectly() {
        viewModel.addToDeletionSet(FitnessPermissionType.DISTANCE)
        viewModel.addToDeletionSet(FitnessPermissionType.MENSTRUATION)
        viewModel.removeFromDeletionSet(FitnessPermissionType.DISTANCE)

        assertThat(viewModel.setOfPermissionTypesToBeDeleted.value)
            .containsExactly(FitnessPermissionType.MENSTRUATION)
    }

    @Test
    fun setDeletionScreenState_setsCorrectly() {
        viewModel.setDeletionScreenStateValue(DELETE)

        assertThat(viewModel.getDeletionScreenStateValue()).isEqualTo(DELETE)
    }

    @Test
    fun getDeletionScreenState_getsCorrectValue() {
        viewModel.setDeletionScreenStateValue(VIEW)

        assertThat(viewModel.getDeletionScreenStateValue()).isEqualTo(VIEW)
    }

    @Test
    fun resetDeleteSet_emptiesDeleteSet() {
        viewModel.addToDeletionSet(FitnessPermissionType.MENSTRUATION)
        viewModel.addToDeletionSet(FitnessPermissionType.DISTANCE)
        viewModel.resetDeletionSet()

        assertThat(viewModel.setOfPermissionTypesToBeDeleted.value).isEmpty()
    }

    @Test
    fun addToDeleteSet_allPermissionTypesSelected_valueUpdatedToTrue() = runTest {
        val permissionTypesPerCategoryForTestApp =
            listOf(
                PermissionTypesPerCategory(
                    category = HealthDataCategory.ACTIVITY,
                    data = listOf(FitnessPermissionType.STEPS),
                ),
                PermissionTypesPerCategory(
                    category = HealthDataCategory.VITALS,
                    data = listOf(FitnessPermissionType.HEART_RATE),
                ),
            )

        val permissionTypesPerCategoryForTestApp2 =
            listOf(
                PermissionTypesPerCategory(
                    category = HealthDataCategory.ACTIVITY,
                    data = listOf(FitnessPermissionType.STEPS),
                )
            )
        getAppFitnessPermissionTypesWithDataUseCase.setPermissionTypesForApp(
            TEST_APP_PACKAGE_NAME,
            permissionTypesPerCategoryForTestApp,
        )
        getAppFitnessPermissionTypesWithDataUseCase.setPermissionTypesForApp(
            TEST_APP_PACKAGE_NAME_2,
            permissionTypesPerCategoryForTestApp2,
        )

        viewModel.loadAppData(TEST_APP_PACKAGE_NAME)
        viewModel.addToDeletionSet(FitnessPermissionType.STEPS)
        viewModel.addToDeletionSet(FitnessPermissionType.HEART_RATE)
        advanceUntilIdle()

        assertThat(viewModel.allPermissionTypesSelected.value).isTrue()
    }

    @Test
    fun removeFromDeleteSet_allPermissionTypesSelected_valueUpdatedToFalse() = runTest {
        val permissionTypesPerCategoryForTestApp =
            listOf(
                PermissionTypesPerCategory(
                    category = HealthDataCategory.ACTIVITY,
                    data = listOf(FitnessPermissionType.STEPS),
                ),
                PermissionTypesPerCategory(
                    category = HealthDataCategory.VITALS,
                    data = listOf(FitnessPermissionType.HEART_RATE),
                ),
            )

        val permissionTypesPerCategoryForTestApp2 =
            listOf(
                PermissionTypesPerCategory(
                    category = HealthDataCategory.ACTIVITY,
                    data = listOf(FitnessPermissionType.STEPS),
                )
            )
        getAppFitnessPermissionTypesWithDataUseCase.setPermissionTypesForApp(
            TEST_APP_PACKAGE_NAME,
            permissionTypesPerCategoryForTestApp,
        )
        getAppFitnessPermissionTypesWithDataUseCase.setPermissionTypesForApp(
            TEST_APP_PACKAGE_NAME_2,
            permissionTypesPerCategoryForTestApp2,
        )

        viewModel.loadAppData(TEST_APP_PACKAGE_NAME)
        viewModel.addToDeletionSet(FitnessPermissionType.STEPS)
        viewModel.addToDeletionSet(FitnessPermissionType.HEART_RATE)
        advanceUntilIdle()

        assertThat(viewModel.allPermissionTypesSelected.value).isTrue()

        viewModel.removeFromDeletionSet(FitnessPermissionType.STEPS)

        assertThat(viewModel.allPermissionTypesSelected.value).isFalse()
    }

    @Test
    fun getNumOfPermissionTypes_withoutSymptoms_returnsCorrect() = runTest {
        val permissionTypesPerCategoryForTestApp =
            listOf(
                PermissionTypesPerCategory(
                    category = HealthDataCategory.ACTIVITY,
                    data = listOf(FitnessPermissionType.STEPS),
                ),
                PermissionTypesPerCategory(
                    category = HealthDataCategory.VITALS,
                    data = listOf(FitnessPermissionType.HEART_RATE),
                ),
                PermissionTypesPerCategory(
                    category = HealthDataCategory.BODY_MEASUREMENTS,
                    data = listOf(FitnessPermissionType.WEIGHT),
                ),
            )

        val permissionTypesPerCategoryForTestApp2 =
            listOf(
                PermissionTypesPerCategory(
                    category = HealthDataCategory.ACTIVITY,
                    data = listOf(FitnessPermissionType.STEPS),
                )
            )
        getAppFitnessPermissionTypesWithDataUseCase.setPermissionTypesForApp(
            TEST_APP_PACKAGE_NAME,
            permissionTypesPerCategoryForTestApp,
        )
        getAppFitnessPermissionTypesWithDataUseCase.setPermissionTypesForApp(
            TEST_APP_PACKAGE_NAME_2,
            permissionTypesPerCategoryForTestApp2,
        )

        viewModel.loadAppData(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        assertThat(viewModel.getTheNumOfPermissionTypes()).isEqualTo(3)
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_SYMPTOMS, Flags.FLAG_SYMPTOMS_DB)
    fun getNumOfPermissionTypes_withSymptoms_returnsCorrect() = runTest {
        val permissionTypesPerCategoryForTestApp =
            listOf(
                PermissionTypesPerCategory(
                    category = HealthDataCategory.ACTIVITY,
                    data = listOf(FitnessPermissionType.STEPS),
                ),
                PermissionTypesPerCategory(
                    category = HealthDataCategory.VITALS,
                    data = listOf(FitnessPermissionType.HEART_RATE),
                ),
                PermissionTypesPerCategory(
                    category = HealthDataCategory.BODY_MEASUREMENTS,
                    data = listOf(FitnessPermissionType.WEIGHT),
                ),
                PermissionTypesPerCategory(
                    category = HealthDataCategory.SYMPTOMS,
                    data = listOf(FitnessPermissionType.SYMPTOM_ABDOMINAL_PAIN),
                ),
            )

        val permissionTypesPerCategoryForTestApp2 =
            listOf(
                PermissionTypesPerCategory(
                    category = HealthDataCategory.ACTIVITY,
                    data = listOf(FitnessPermissionType.STEPS),
                ),
                PermissionTypesPerCategory(
                    category = HealthDataCategory.SYMPTOMS,
                    data = listOf(FitnessPermissionType.SYMPTOM_ABDOMINAL_PAIN),
                ),
            )
        getAppFitnessPermissionTypesWithDataUseCase.setPermissionTypesForApp(
            TEST_APP_PACKAGE_NAME,
            permissionTypesPerCategoryForTestApp,
        )
        getAppFitnessPermissionTypesWithDataUseCase.setPermissionTypesForApp(
            TEST_APP_PACKAGE_NAME_2,
            permissionTypesPerCategoryForTestApp2,
        )

        viewModel.loadAppData(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        assertThat(viewModel.getTheNumOfPermissionTypes())
            .isEqualTo(3 + getAllSymptomPermissionTypes().size)
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_SYMPTOMS, Flags.FLAG_SYMPTOMS_DB)
    fun prepareDeletionType_withAllSymptoms_returnsAllSymptomTypes() = runTest {
        val permissionTypesPerCategoryForTestApp =
            listOf(
                PermissionTypesPerCategory(
                    category = HealthDataCategory.ACTIVITY,
                    data = listOf(FitnessPermissionType.STEPS),
                ),
                PermissionTypesPerCategory(
                    category = HealthDataCategory.VITALS,
                    data = listOf(FitnessPermissionType.HEART_RATE),
                ),
                PermissionTypesPerCategory(
                    category = HealthDataCategory.SYMPTOMS,
                    data = listOf(FitnessPermissionType.SYMPTOM_ABDOMINAL_PAIN),
                ),
            )

        val permissionTypesPerCategoryForTestApp2 =
            listOf(
                PermissionTypesPerCategory(
                    category = HealthDataCategory.ACTIVITY,
                    data = listOf(FitnessPermissionType.STEPS),
                ),
                PermissionTypesPerCategory(
                    category = HealthDataCategory.BODY_MEASUREMENTS,
                    data = listOf(FitnessPermissionType.WEIGHT),
                ),
                PermissionTypesPerCategory(
                    category = HealthDataCategory.SYMPTOMS,
                    data = listOf(FitnessPermissionType.SYMPTOM_ABDOMINAL_PAIN),
                ),
            )
        getAppFitnessPermissionTypesWithDataUseCase.setPermissionTypesForApp(
            TEST_APP_PACKAGE_NAME,
            permissionTypesPerCategoryForTestApp,
        )
        getAppFitnessPermissionTypesWithDataUseCase.setPermissionTypesForApp(
            TEST_APP_PACKAGE_NAME_2,
            permissionTypesPerCategoryForTestApp2,
        )

        val testObserver = TestObserver<AppDataViewModel.AppDataState>()
        viewModel.fitnessAndMedicalData.observeForever(testObserver)
        viewModel.loadAppData(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        val expected =
            listOf(
                PermissionTypesPerCategory(
                    HealthDataCategory.ACTIVITY,
                    listOf(FitnessPermissionType.STEPS),
                ),
                PermissionTypesPerCategory(
                    HealthDataCategory.VITALS,
                    listOf(FitnessPermissionType.HEART_RATE),
                ),
                // Special case for symptoms since we show "All symptoms" on the screen
                PermissionTypesPerCategory(
                    HealthDataCategory.SYMPTOMS,
                    listOf(FitnessPermissionType.SYMPTOM_ABDOMINAL_PAIN),
                ),
            )
        assertThat(testObserver.getLastValue())
            .isEqualTo(AppDataViewModel.AppDataState.WithData(expected))
        assertThat(viewModel.getTheNumOfPermissionTypes())
            .isEqualTo(2 + getAllSymptomPermissionTypes().size)
        viewModel.loadAppInfo(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()
        viewModel.addToDeletionSet(FitnessPermissionType.SYMPTOM_ABDOMINAL_PAIN)
        val deletionType = viewModel.prepareDeletionType()
        val expectedSymptomTypes = getAllSymptomPermissionTypes()
        assertThat(deletionType).isNotNull()
        assertThat(deletionType!!.healthPermissionTypes)
            .containsExactlyElementsIn(expectedSymptomTypes)
        assertThat(deletionType.totalPermissionTypes)
            .isEqualTo(2 + getAllSymptomPermissionTypes().size)
        assertThat(deletionType.packageName).isEqualTo(TEST_APP_PACKAGE_NAME)
        assertThat(deletionType.appName).isEqualTo(TEST_APP_NAME)
    }

    @Test
    fun prepareDeletionType_withoutSymptoms_returnsSelectedTypes() = runTest {
        val permissionTypesPerCategoryForTestApp =
            listOf(
                PermissionTypesPerCategory(
                    category = HealthDataCategory.ACTIVITY,
                    data = listOf(FitnessPermissionType.STEPS),
                ),
                PermissionTypesPerCategory(
                    category = HealthDataCategory.VITALS,
                    data = listOf(FitnessPermissionType.HEART_RATE),
                ),
                PermissionTypesPerCategory(
                    category = HealthDataCategory.SYMPTOMS,
                    data = listOf(FitnessPermissionType.SYMPTOM_ABDOMINAL_PAIN),
                ),
            )

        val permissionTypesPerCategoryForTestApp2 =
            listOf(
                PermissionTypesPerCategory(
                    category = HealthDataCategory.ACTIVITY,
                    data = listOf(FitnessPermissionType.STEPS),
                ),
                PermissionTypesPerCategory(
                    category = HealthDataCategory.BODY_MEASUREMENTS,
                    data = listOf(FitnessPermissionType.WEIGHT),
                ),
                PermissionTypesPerCategory(
                    category = HealthDataCategory.SYMPTOMS,
                    data = listOf(FitnessPermissionType.SYMPTOM_ABDOMINAL_PAIN),
                ),
            )
        getAppFitnessPermissionTypesWithDataUseCase.setPermissionTypesForApp(
            TEST_APP_PACKAGE_NAME,
            permissionTypesPerCategoryForTestApp,
        )
        getAppFitnessPermissionTypesWithDataUseCase.setPermissionTypesForApp(
            TEST_APP_PACKAGE_NAME_2,
            permissionTypesPerCategoryForTestApp2,
        )

        val testObserver = TestObserver<AppDataViewModel.AppDataState>()
        viewModel.fitnessAndMedicalData.observeForever(testObserver)
        viewModel.loadAppData(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        val expected =
            listOf(
                PermissionTypesPerCategory(
                    HealthDataCategory.ACTIVITY,
                    listOf(FitnessPermissionType.STEPS),
                ),
                PermissionTypesPerCategory(
                    HealthDataCategory.VITALS,
                    listOf(FitnessPermissionType.HEART_RATE),
                ),
                // Special case for symptoms since we show "All symptoms" on the screen
                PermissionTypesPerCategory(
                    HealthDataCategory.SYMPTOMS,
                    listOf(FitnessPermissionType.SYMPTOM_ABDOMINAL_PAIN),
                ),
            )
        assertThat(testObserver.getLastValue())
            .isEqualTo(AppDataViewModel.AppDataState.WithData(expected))
        assertThat(viewModel.getTheNumOfPermissionTypes())
            .isEqualTo(2 + getAllSymptomPermissionTypes().size)
        viewModel.loadAppInfo(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        viewModel.addToDeletionSet(FitnessPermissionType.STEPS)
        viewModel.addToDeletionSet(FitnessPermissionType.HEART_RATE)
        val deletionType = viewModel.prepareDeletionType()
        assertThat(deletionType).isNotNull()
        assertThat(deletionType!!.healthPermissionTypes)
            .containsExactly(FitnessPermissionType.STEPS, FitnessPermissionType.HEART_RATE)
        assertThat(deletionType.totalPermissionTypes)
            .isEqualTo(2 + getAllSymptomPermissionTypes().size)
        assertThat(deletionType.packageName).isEqualTo(TEST_APP_PACKAGE_NAME)
        assertThat(deletionType.appName).isEqualTo(TEST_APP_NAME)
    }

    @Test
    fun prepareDeletionType_medicalAndFitness_setsCorrectly() = runTest {
        val permissionTypesPerCategoryForTestApp =
            listOf(
                PermissionTypesPerCategory(
                    category = HealthDataCategory.ACTIVITY,
                    data = listOf(FitnessPermissionType.STEPS),
                ),
                PermissionTypesPerCategory(
                    category = HealthDataCategory.VITALS,
                    data = listOf(FitnessPermissionType.HEART_RATE),
                ),
            )

        val permissionTypesPerCategoryForTestApp2 =
            listOf(
                PermissionTypesPerCategory(
                    category = HealthDataCategory.ACTIVITY,
                    data = listOf(FitnessPermissionType.STEPS),
                ),
                PermissionTypesPerCategory(
                    category = HealthDataCategory.BODY_MEASUREMENTS,
                    data = listOf(FitnessPermissionType.WEIGHT),
                ),
            )
        getAppFitnessPermissionTypesWithDataUseCase.setPermissionTypesForApp(
            TEST_APP_PACKAGE_NAME,
            permissionTypesPerCategoryForTestApp,
        )
        getAppFitnessPermissionTypesWithDataUseCase.setPermissionTypesForApp(
            TEST_APP_PACKAGE_NAME_2,
            permissionTypesPerCategoryForTestApp2,
        )
        getAppMedicalPermissionTypesWithDataUseCase.setPermissionTypesForApp(
            TEST_MEDICAL_DATA_SOURCE.packageName,
            listOf(PermissionTypesPerCategory(category = MEDICAL, data = listOf(VACCINES))),
        )

        val testObserver = TestObserver<AppDataViewModel.AppDataState>()
        viewModel.fitnessAndMedicalData.observeForever(testObserver)
        viewModel.loadAppData(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        val expected =
            listOf(
                PermissionTypesPerCategory(
                    HealthDataCategory.ACTIVITY,
                    listOf(FitnessPermissionType.STEPS),
                ),
                PermissionTypesPerCategory(
                    HealthDataCategory.VITALS,
                    listOf(FitnessPermissionType.HEART_RATE),
                ),
                // Special case for symptoms since we show "All symptoms" on the screen
                PermissionTypesPerCategory(MEDICAL, listOf(VACCINES)),
            )
        assertThat(testObserver.getLastValue())
            .isEqualTo(AppDataViewModel.AppDataState.WithData(expected))
        assertThat(viewModel.getTheNumOfPermissionTypes()).isEqualTo(3)
        viewModel.loadAppInfo(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()
        // Now add to deletion set
        viewModel.addToDeletionSet(FitnessPermissionType.STEPS)
        viewModel.addToDeletionSet(VACCINES)
        val deletionType = viewModel.prepareDeletionType()
        assertThat(deletionType).isNotNull()
        assertThat(deletionType!!.healthPermissionTypes)
            .containsExactly(FitnessPermissionType.STEPS, VACCINES)
        assertThat(deletionType.totalPermissionTypes).isEqualTo(3)
        assertThat(deletionType.packageName).isEqualTo(TEST_APP_PACKAGE_NAME)
        assertThat(deletionType.appName).isEqualTo(TEST_APP_NAME)
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_SYMPTOMS, Flags.FLAG_SYMPTOMS_DB)
    fun prepareDeletionType_symptomsOnly_setsCorrectly() = runTest {
        val permissionTypesPerCategoryForTestApp =
            listOf(
                PermissionTypesPerCategory(
                    category = HealthDataCategory.ACTIVITY,
                    data = listOf(FitnessPermissionType.STEPS),
                ),
                PermissionTypesPerCategory(
                    category = HealthDataCategory.VITALS,
                    data = listOf(FitnessPermissionType.HEART_RATE),
                ),
                PermissionTypesPerCategory(
                    category = HealthDataCategory.SYMPTOMS,
                    data = listOf(FitnessPermissionType.SYMPTOM_ABDOMINAL_PAIN),
                ),
            )

        val permissionTypesPerCategoryForTestApp2 =
            listOf(
                PermissionTypesPerCategory(
                    category = HealthDataCategory.ACTIVITY,
                    data = listOf(FitnessPermissionType.STEPS),
                ),
                PermissionTypesPerCategory(
                    category = HealthDataCategory.BODY_MEASUREMENTS,
                    data = listOf(FitnessPermissionType.WEIGHT),
                ),
                PermissionTypesPerCategory(
                    category = HealthDataCategory.SYMPTOMS,
                    data = listOf(FitnessPermissionType.SYMPTOM_ABDOMINAL_PAIN),
                ),
            )
        getAppFitnessPermissionTypesWithDataUseCase.setPermissionTypesForApp(
            TEST_APP_PACKAGE_NAME,
            permissionTypesPerCategoryForTestApp,
        )
        getAppFitnessPermissionTypesWithDataUseCase.setPermissionTypesForApp(
            TEST_APP_PACKAGE_NAME_2,
            permissionTypesPerCategoryForTestApp2,
        )
        getAppMedicalPermissionTypesWithDataUseCase.setPermissionTypesForApp(
            TEST_MEDICAL_DATA_SOURCE.packageName,
            listOf(PermissionTypesPerCategory(category = MEDICAL, data = listOf(VACCINES))),
        )

        val testObserver = TestObserver<AppDataViewModel.AppDataState>()
        viewModel.fitnessAndMedicalData.observeForever(testObserver)
        viewModel.loadAppData(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()

        val expected =
            listOf(
                PermissionTypesPerCategory(
                    HealthDataCategory.ACTIVITY,
                    listOf(FitnessPermissionType.STEPS),
                ),
                PermissionTypesPerCategory(
                    HealthDataCategory.VITALS,
                    listOf(FitnessPermissionType.HEART_RATE),
                ),
                // Special case for symptoms since we show "All symptoms" on the screen
                PermissionTypesPerCategory(
                    HealthDataCategory.SYMPTOMS,
                    listOf(FitnessPermissionType.SYMPTOM_ABDOMINAL_PAIN),
                ),
                PermissionTypesPerCategory(MEDICAL, listOf(VACCINES)),
            )
        assertThat(testObserver.getLastValue())
            .isEqualTo(AppDataViewModel.AppDataState.WithData(expected))
        assertThat(viewModel.getTheNumOfPermissionTypes())
            .isEqualTo(3 + getAllSymptomPermissionTypes().size)
        viewModel.loadAppInfo(TEST_APP_PACKAGE_NAME)
        advanceUntilIdle()
        // Now add to deletion set
        viewModel.addToDeletionSet(FitnessPermissionType.SYMPTOM_ABDOMINAL_PAIN)
        val deletionType = viewModel.prepareDeletionType()
        assertThat(deletionType).isNotNull()
        assertThat(deletionType!!.healthPermissionTypes)
            .containsExactlyElementsIn(getAllSymptomPermissionTypes())
        assertThat(deletionType.totalPermissionTypes)
            .isEqualTo(3 + getAllSymptomPermissionTypes().size)
        assertThat(deletionType.packageName).isEqualTo(TEST_APP_PACKAGE_NAME)
        assertThat(deletionType.appName).isEqualTo(TEST_APP_NAME)
    }
}
