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
package com.android.healthconnect.controller.tests.data.alldata

import android.health.connect.HealthDataCategory
import android.platform.test.annotations.RequiresFlagsDisabled
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.data.alldata.AllDataViewModel
import com.android.healthconnect.controller.data.alldata.AllDataViewModel.AllDataState.WithData
import com.android.healthconnect.controller.data.api.PermissionTypesPerCategory
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.permissions.data.MedicalPermissionType
import com.android.healthconnect.controller.permissions.data.getAllSymptomPermissionTypes
import com.android.healthconnect.controller.selectabledeletion.DeletionDataViewModel.DeletionScreenState
import com.android.healthconnect.controller.shared.HealthDataCategoryExtensions.MEDICAL
import com.android.healthconnect.controller.tests.data.alldata.api.FakeGetFitnessPermissionTypesWithDataUseCase
import com.android.healthconnect.controller.tests.data.alldata.api.FakeGetMedicalPermissionTypesWithDataUseCase
import com.android.healthconnect.controller.tests.utils.FakeUseCaseRule
import com.android.healthconnect.controller.tests.utils.InstantTaskExecutorRule
import com.android.healthconnect.controller.tests.utils.TestObserver
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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AllDataViewModelTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)
    @get:Rule val checkFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()
    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()
    @get:Rule val fakeUseCaseRule = FakeUseCaseRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private val fakeGetFitnessPermissionTypesWithDataUseCase =
        fakeUseCaseRule.watch(FakeGetFitnessPermissionTypesWithDataUseCase())
    private val fakeGetMedicalPermissionTypesWithDataUseCase =
        fakeUseCaseRule.watch(FakeGetMedicalPermissionTypesWithDataUseCase())

    private lateinit var viewModel: AllDataViewModel

    @Before
    fun setup() {
        hiltRule.inject()
        Dispatchers.setMain(testDispatcher)
        viewModel =
            AllDataViewModel(
                fakeGetFitnessPermissionTypesWithDataUseCase,
                fakeGetMedicalPermissionTypesWithDataUseCase,
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_SYMPTOMS, Flags.FLAG_SYMPTOMS_DB)
    fun loadAllData_symptomsFlagEnabled_noFitnessData_returnsEmptyList() = runTest {
        fakeGetFitnessPermissionTypesWithDataUseCase.setPermissionTypesPerCategory(emptyList())

        val testObserver = TestObserver<AllDataViewModel.AllDataState>()
        viewModel.allData.observeForever(testObserver)
        viewModel.loadAllFitnessData()
        advanceUntilIdle()

        val expected = emptyList<PermissionTypesPerCategory>()
        assertThat(testObserver.getLastValue()).isEqualTo(WithData(expected))
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_SYMPTOMS, Flags.FLAG_SYMPTOMS_DB)
    fun loadAllData_symptomsFlagDisabled_noFitnessData_returnsEmptyList() = runTest {
        fakeGetFitnessPermissionTypesWithDataUseCase.setPermissionTypesPerCategory(emptyList())

        val testObserver = TestObserver<AllDataViewModel.AllDataState>()
        viewModel.allData.observeForever(testObserver)
        viewModel.loadAllFitnessData()
        advanceUntilIdle()

        val expected = emptyList<PermissionTypesPerCategory>()
        assertThat(testObserver.getLastValue()).isEqualTo(WithData(expected))
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_SYMPTOMS, Flags.FLAG_SYMPTOMS_DB)
    fun loadAllData_symptomsFlagEnabled_hasData_returnsDataWrittenByAllFitnessApps() = runTest {
        val permissionTypes =
            listOf(
                PermissionTypesPerCategory(
                    HealthDataCategory.ACTIVITY,
                    listOf(FitnessPermissionType.STEPS),
                ),
                PermissionTypesPerCategory(
                    HealthDataCategory.BODY_MEASUREMENTS,
                    listOf(FitnessPermissionType.WEIGHT),
                ),
                PermissionTypesPerCategory(
                    HealthDataCategory.VITALS,
                    listOf(FitnessPermissionType.HEART_RATE),
                ),
            )
        fakeGetFitnessPermissionTypesWithDataUseCase.setPermissionTypesPerCategory(permissionTypes)

        val testObserver = TestObserver<AllDataViewModel.AllDataState>()
        viewModel.allData.observeForever(testObserver)
        viewModel.loadAllFitnessData()
        advanceUntilIdle()

        val expected =
            listOf(
                PermissionTypesPerCategory(
                    HealthDataCategory.ACTIVITY,
                    listOf(FitnessPermissionType.STEPS),
                ),
                PermissionTypesPerCategory(
                    HealthDataCategory.BODY_MEASUREMENTS,
                    listOf(FitnessPermissionType.WEIGHT),
                ),
                PermissionTypesPerCategory(
                    HealthDataCategory.VITALS,
                    listOf(FitnessPermissionType.HEART_RATE),
                ),
            )
        assertThat(testObserver.getLastValue()).isEqualTo(WithData(expected))
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_SYMPTOMS, Flags.FLAG_SYMPTOMS_DB)
    fun loadAllData_symptomsFlagDisabled_hasData_returnsDataWrittenByAllFitnessApps() = runTest {
        val permissionTypes =
            listOf(
                PermissionTypesPerCategory(
                    HealthDataCategory.ACTIVITY,
                    listOf(FitnessPermissionType.STEPS),
                ),
                PermissionTypesPerCategory(
                    HealthDataCategory.BODY_MEASUREMENTS,
                    listOf(FitnessPermissionType.WEIGHT),
                ),
                PermissionTypesPerCategory(
                    HealthDataCategory.VITALS,
                    listOf(FitnessPermissionType.HEART_RATE),
                ),
            )
        fakeGetFitnessPermissionTypesWithDataUseCase.setPermissionTypesPerCategory(permissionTypes)

        val testObserver = TestObserver<AllDataViewModel.AllDataState>()
        viewModel.allData.observeForever(testObserver)
        viewModel.loadAllFitnessData()
        advanceUntilIdle()

        assertThat(testObserver.getLastValue()).isEqualTo(WithData(permissionTypes))
    }

    @Test
    fun loadMedicalData_noMedicalData_returnsEmptyList() = runTest {
        fakeGetMedicalPermissionTypesWithDataUseCase.setPermissionTypesPerCategory(emptyList())

        val testObserver = TestObserver<AllDataViewModel.AllDataState>()
        viewModel.allData.observeForever(testObserver)
        viewModel.loadAllMedicalData()
        advanceUntilIdle()

        val expected = emptyList<PermissionTypesPerCategory>()
        assertThat(testObserver.getLastValue()).isEqualTo(WithData(expected))
    }

    @Test
    fun loadMedicalData_hasMedicalData_returnsMedicalData() = runTest {
        val medicalData =
            listOf(PermissionTypesPerCategory(MEDICAL, listOf(MedicalPermissionType.VACCINES)))
        fakeGetMedicalPermissionTypesWithDataUseCase.setPermissionTypesPerCategory(medicalData)

        val testObserver = TestObserver<AllDataViewModel.AllDataState>()
        viewModel.allData.observeForever(testObserver)
        viewModel.loadAllMedicalData()
        advanceUntilIdle()

        val expected =
            listOf(PermissionTypesPerCategory(MEDICAL, listOf(MedicalPermissionType.VACCINES)))
        assertThat(testObserver.getLastValue()).isEqualTo(WithData(expected))
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
        viewModel.setDeletionScreenStateValue(DeletionScreenState.DELETE)

        assertThat(viewModel.getDeletionScreenStateValue()).isEqualTo(DeletionScreenState.DELETE)
    }

    @Test
    fun getDeletionScreenState_getsCorrectValue() {
        viewModel.setDeletionScreenStateValue(DeletionScreenState.VIEW)

        assertThat(viewModel.getDeletionScreenStateValue()).isEqualTo(DeletionScreenState.VIEW)
    }

    @Test
    fun resetDeleteSet_emptiesDeleteSet() {
        viewModel.addToDeletionSet(FitnessPermissionType.MENSTRUATION)
        viewModel.addToDeletionSet(FitnessPermissionType.DISTANCE)
        viewModel.resetDeletionSet()

        assertThat(viewModel.setOfPermissionTypesToBeDeleted.value).isEmpty()
    }

    @Test
    fun getNumOfPermissionTypes_withoutSymptoms_returnsCorrect() = runTest {
        val permissionTypes =
            listOf(
                PermissionTypesPerCategory(
                    HealthDataCategory.ACTIVITY,
                    listOf(FitnessPermissionType.STEPS),
                ),
                PermissionTypesPerCategory(
                    HealthDataCategory.BODY_MEASUREMENTS,
                    listOf(FitnessPermissionType.WEIGHT),
                ),
                PermissionTypesPerCategory(
                    HealthDataCategory.VITALS,
                    listOf(FitnessPermissionType.HEART_RATE),
                ),
            )
        fakeGetFitnessPermissionTypesWithDataUseCase.setPermissionTypesPerCategory(permissionTypes)

        viewModel.loadAllFitnessData()
        advanceUntilIdle()

        assertThat(viewModel.getTheNumOfPermissionTypes()).isEqualTo(3)
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_SYMPTOMS, Flags.FLAG_SYMPTOMS_DB)
    fun getNumOfPermissionTypes_withSymptoms_returnsCorrect() = runTest {
        val permissionTypes =
            listOf(
                PermissionTypesPerCategory(
                    HealthDataCategory.ACTIVITY,
                    listOf(FitnessPermissionType.STEPS),
                ),
                PermissionTypesPerCategory(
                    HealthDataCategory.BODY_MEASUREMENTS,
                    listOf(FitnessPermissionType.WEIGHT),
                ),
                PermissionTypesPerCategory(
                    HealthDataCategory.VITALS,
                    listOf(FitnessPermissionType.HEART_RATE),
                ),
                PermissionTypesPerCategory(
                    HealthDataCategory.SYMPTOMS,
                    // SYMPTOM_ABDOMINAL_PAIN represents the Symptoms category
                    listOf(FitnessPermissionType.SYMPTOM_ABDOMINAL_PAIN),
                ),
            )
        fakeGetFitnessPermissionTypesWithDataUseCase.setPermissionTypesPerCategory(permissionTypes)

        viewModel.loadAllFitnessData()
        advanceUntilIdle()

        assertThat(viewModel.getTheNumOfPermissionTypes())
            .isEqualTo(3 + getAllSymptomPermissionTypes().size)
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_SYMPTOMS, Flags.FLAG_SYMPTOMS_DB)
    fun prepareDeletionType_withAllSymptoms_returnsAllSymptomTypes() = runTest {
        val permissionTypes =
            listOf(
                PermissionTypesPerCategory(
                    HealthDataCategory.ACTIVITY,
                    listOf(FitnessPermissionType.STEPS),
                ),
                PermissionTypesPerCategory(
                    HealthDataCategory.BODY_MEASUREMENTS,
                    listOf(FitnessPermissionType.WEIGHT),
                ),
                PermissionTypesPerCategory(
                    HealthDataCategory.VITALS,
                    listOf(FitnessPermissionType.HEART_RATE),
                ),
                PermissionTypesPerCategory(
                    HealthDataCategory.SYMPTOMS,
                    // SYMPTOM_ABDOMINAL_PAIN represents the Symptoms category
                    listOf(FitnessPermissionType.SYMPTOM_ABDOMINAL_PAIN),
                ),
            )
        fakeGetFitnessPermissionTypesWithDataUseCase.setPermissionTypesPerCategory(permissionTypes)

        val testObserver = TestObserver<AllDataViewModel.AllDataState>()
        viewModel.allData.observeForever(testObserver)
        viewModel.loadAllFitnessData()
        advanceUntilIdle()

        assertThat(testObserver.getLastValue()).isEqualTo(WithData(permissionTypes))
        assertThat(viewModel.getTheNumOfPermissionTypes())
            .isEqualTo(3 + getAllSymptomPermissionTypes().size)

        viewModel.addToDeletionSet(FitnessPermissionType.SYMPTOM_ABDOMINAL_PAIN)
        val deletionType = viewModel.prepareDeletionType()
        val expectedSymptomTypes = getAllSymptomPermissionTypes()
        assertThat(deletionType.healthPermissionTypes)
            .containsExactlyElementsIn(expectedSymptomTypes)
        assertThat(deletionType.totalPermissionTypes)
            .isEqualTo(3 + getAllSymptomPermissionTypes().size)
    }

    @Test
    fun prepareDeletionType_withoutSymptoms_returnsSelectedTypes() = runTest {
        val permissionTypes =
            listOf(
                PermissionTypesPerCategory(
                    HealthDataCategory.ACTIVITY,
                    listOf(FitnessPermissionType.STEPS),
                ),
                PermissionTypesPerCategory(
                    HealthDataCategory.BODY_MEASUREMENTS,
                    listOf(FitnessPermissionType.WEIGHT),
                ),
                PermissionTypesPerCategory(
                    HealthDataCategory.VITALS,
                    listOf(FitnessPermissionType.HEART_RATE),
                ),
            )
        fakeGetFitnessPermissionTypesWithDataUseCase.setPermissionTypesPerCategory(permissionTypes)

        val testObserver = TestObserver<AllDataViewModel.AllDataState>()
        viewModel.allData.observeForever(testObserver)
        viewModel.loadAllFitnessData()
        advanceUntilIdle()

        assertThat(testObserver.getLastValue()).isEqualTo(WithData(permissionTypes))
        assertThat(viewModel.getTheNumOfPermissionTypes()).isEqualTo(3)

        viewModel.addToDeletionSet(FitnessPermissionType.STEPS)
        viewModel.addToDeletionSet(FitnessPermissionType.HEART_RATE)
        val deletionType = viewModel.prepareDeletionType()
        assertThat(deletionType.healthPermissionTypes)
            .containsExactly(FitnessPermissionType.STEPS, FitnessPermissionType.HEART_RATE)
        assertThat(deletionType.totalPermissionTypes).isEqualTo(3)
    }

    @Test
    fun prepareDeletionType_medicalAndFitness_setsCorrectly() = runTest {
        val permissionTypes =
            listOf(
                PermissionTypesPerCategory(
                    HealthDataCategory.ACTIVITY,
                    listOf(FitnessPermissionType.STEPS),
                ),
                PermissionTypesPerCategory(
                    HealthDataCategory.BODY_MEASUREMENTS,
                    listOf(FitnessPermissionType.WEIGHT),
                ),
                PermissionTypesPerCategory(
                    HealthDataCategory.VITALS,
                    listOf(FitnessPermissionType.HEART_RATE),
                ),
            )
        fakeGetFitnessPermissionTypesWithDataUseCase.setPermissionTypesPerCategory(permissionTypes)

        val medicalData =
            listOf(PermissionTypesPerCategory(MEDICAL, listOf(MedicalPermissionType.VACCINES)))
        fakeGetMedicalPermissionTypesWithDataUseCase.setPermissionTypesPerCategory(medicalData)

        val testObserver = TestObserver<AllDataViewModel.AllDataState>()
        viewModel.allData.observeForever(testObserver)
        viewModel.loadAllFitnessAndMedicalData()
        advanceUntilIdle()

        val expected =
            listOf(
                PermissionTypesPerCategory(
                    HealthDataCategory.ACTIVITY,
                    listOf(FitnessPermissionType.STEPS),
                ),
                PermissionTypesPerCategory(
                    HealthDataCategory.BODY_MEASUREMENTS,
                    listOf(FitnessPermissionType.WEIGHT),
                ),
                PermissionTypesPerCategory(
                    HealthDataCategory.VITALS,
                    listOf(FitnessPermissionType.HEART_RATE),
                ),
                PermissionTypesPerCategory(MEDICAL, listOf(MedicalPermissionType.VACCINES)),
            )
        assertThat(testObserver.getLastValue()).isEqualTo(WithData(expected))
        assertThat(viewModel.getTheNumOfPermissionTypes()).isEqualTo(4)

        viewModel.addToDeletionSet(FitnessPermissionType.STEPS)
        viewModel.addToDeletionSet(MedicalPermissionType.VACCINES)
        val deletionType = viewModel.prepareDeletionType()
        assertThat(deletionType.healthPermissionTypes)
            .containsExactly(FitnessPermissionType.STEPS, MedicalPermissionType.VACCINES)
        assertThat(deletionType.totalPermissionTypes).isEqualTo(4)
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_SYMPTOMS, Flags.FLAG_SYMPTOMS_DB)
    fun prepareDeletionType_symptomsOnly_setsCorrectly() = runTest {
        val permissionTypes =
            listOf(
                PermissionTypesPerCategory(
                    HealthDataCategory.ACTIVITY,
                    listOf(FitnessPermissionType.STEPS),
                ),
                PermissionTypesPerCategory(
                    HealthDataCategory.BODY_MEASUREMENTS,
                    listOf(FitnessPermissionType.WEIGHT),
                ),
                PermissionTypesPerCategory(
                    HealthDataCategory.VITALS,
                    listOf(FitnessPermissionType.HEART_RATE),
                ),
                PermissionTypesPerCategory(
                    HealthDataCategory.SYMPTOMS,
                    // SYMPTOM_ABDOMINAL_PAIN represents the Symptoms category
                    listOf(FitnessPermissionType.SYMPTOM_ABDOMINAL_PAIN),
                ),
            )
        fakeGetFitnessPermissionTypesWithDataUseCase.setPermissionTypesPerCategory(permissionTypes)

        val medicalData =
            listOf(PermissionTypesPerCategory(MEDICAL, listOf(MedicalPermissionType.VACCINES)))
        fakeGetMedicalPermissionTypesWithDataUseCase.setPermissionTypesPerCategory(medicalData)

        val testObserver = TestObserver<AllDataViewModel.AllDataState>()
        viewModel.allData.observeForever(testObserver)
        viewModel.loadAllFitnessAndMedicalData()
        advanceUntilIdle()

        val expected =
            listOf(
                PermissionTypesPerCategory(
                    HealthDataCategory.ACTIVITY,
                    listOf(FitnessPermissionType.STEPS),
                ),
                PermissionTypesPerCategory(
                    HealthDataCategory.BODY_MEASUREMENTS,
                    listOf(FitnessPermissionType.WEIGHT),
                ),
                PermissionTypesPerCategory(
                    HealthDataCategory.VITALS,
                    listOf(FitnessPermissionType.HEART_RATE),
                ),
                PermissionTypesPerCategory(
                    HealthDataCategory.SYMPTOMS,
                    listOf(FitnessPermissionType.SYMPTOM_ABDOMINAL_PAIN),
                ),
                PermissionTypesPerCategory(MEDICAL, listOf(MedicalPermissionType.VACCINES)),
            )
        assertThat(testObserver.getLastValue()).isEqualTo(WithData(expected))
        assertThat(viewModel.getTheNumOfPermissionTypes())
            .isEqualTo(4 + getAllSymptomPermissionTypes().size)

        viewModel.addToDeletionSet(FitnessPermissionType.SYMPTOM_ABDOMINAL_PAIN)
        val deletionType = viewModel.prepareDeletionType()
        assertThat(deletionType.healthPermissionTypes)
            .containsExactlyElementsIn(getAllSymptomPermissionTypes())
        assertThat(deletionType.totalPermissionTypes)
            .isEqualTo(4 + getAllSymptomPermissionTypes().size)
    }
}
