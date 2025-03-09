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

import android.content.Context
import android.health.connect.HealthConnectManager
import android.health.connect.HealthDataCategory
import android.health.connect.HealthPermissionCategory
import android.health.connect.MedicalResourceTypeInfo
import android.health.connect.RecordTypeInfoResponse
import android.health.connect.datatypes.HeartRateRecord
import android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES
import android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES
import android.health.connect.datatypes.Record
import android.health.connect.datatypes.StepsRecord
import android.health.connect.datatypes.WeightRecord
import android.os.OutcomeReceiver
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.data.alldata.AllDataViewModel
import com.android.healthconnect.controller.data.appdata.AllDataUseCase
import com.android.healthconnect.controller.data.appdata.PermissionTypesPerCategory
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.permissions.data.MedicalPermissionType
import com.android.healthconnect.controller.selectabledeletion.DeletionDataViewModel
import com.android.healthconnect.controller.shared.HealthDataCategoryExtensions.MEDICAL
import com.android.healthconnect.controller.tests.utils.InstantTaskExecutorRule
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME_2
import com.android.healthconnect.controller.tests.utils.TEST_MEDICAL_DATA_SOURCE
import com.android.healthconnect.controller.tests.utils.TestObserver
import com.android.healthconnect.controller.tests.utils.getDataOrigin
import com.android.healthconnect.controller.tests.utils.setLocale
import com.google.common.truth.Truth.assertThat
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
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import org.mockito.invocation.InvocationOnMock

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
class AllDataViewModelTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()
    private val testDispatcher = UnconfinedTestDispatcher()

    var manager: HealthConnectManager = mock(HealthConnectManager::class.java)

    private lateinit var viewModel: AllDataViewModel
    private lateinit var context: Context

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        context = InstrumentationRegistry.getInstrumentation().context
        context.setLocale(Locale.US)
        hiltRule.inject()
        Dispatchers.setMain(testDispatcher)
        viewModel = AllDataViewModel(AllDataUseCase(manager, Dispatchers.Main))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadAllData_noFitnessData_returnsEmptyList() = runTest {
        doAnswer(prepareAnswer(mapOf())).`when`(manager).queryAllRecordTypesInfo(any(), any())

        val testObserver = TestObserver<AllDataViewModel.AllDataState>()
        viewModel.allData.observeForever(testObserver)
        viewModel.loadAllFitnessData()
        advanceUntilIdle()

        val expected =
            listOf(
                PermissionTypesPerCategory(HealthDataCategory.ACTIVITY, listOf()),
                PermissionTypesPerCategory(HealthDataCategory.BODY_MEASUREMENTS, listOf()),
                PermissionTypesPerCategory(HealthDataCategory.CYCLE_TRACKING, listOf()),
                PermissionTypesPerCategory(HealthDataCategory.NUTRITION, listOf()),
                PermissionTypesPerCategory(HealthDataCategory.SLEEP, listOf()),
                PermissionTypesPerCategory(HealthDataCategory.VITALS, listOf()),
                PermissionTypesPerCategory(HealthDataCategory.WELLNESS, listOf()),
            )
        assertThat(testObserver.getLastValue())
            .isEqualTo(AllDataViewModel.AllDataState.WithData(expected))
    }

    @Test
    fun loadAllData_hasData_returnsDataWrittenByAllFitnessApps() = runTest {
        val recordTypeInfoMap: Map<Class<out Record>, RecordTypeInfoResponse> =
            mapOf(
                StepsRecord::class.java to
                    RecordTypeInfoResponse(
                        HealthPermissionCategory.STEPS,
                        HealthDataCategory.ACTIVITY,
                        listOf(
                            getDataOrigin(TEST_APP_PACKAGE_NAME),
                            getDataOrigin(TEST_APP_PACKAGE_NAME_2),
                        ),
                    ),
                WeightRecord::class.java to
                    RecordTypeInfoResponse(
                        HealthPermissionCategory.WEIGHT,
                        HealthDataCategory.BODY_MEASUREMENTS,
                        listOf((getDataOrigin(TEST_APP_PACKAGE_NAME_2))),
                    ),
                HeartRateRecord::class.java to
                    RecordTypeInfoResponse(
                        HealthPermissionCategory.HEART_RATE,
                        HealthDataCategory.VITALS,
                        listOf((getDataOrigin(TEST_APP_PACKAGE_NAME))),
                    ),
            )
        doAnswer(prepareAnswer(recordTypeInfoMap))
            .`when`(manager)
            .queryAllRecordTypesInfo(any(), any())

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
                PermissionTypesPerCategory(HealthDataCategory.CYCLE_TRACKING, listOf()),
                PermissionTypesPerCategory(HealthDataCategory.NUTRITION, listOf()),
                PermissionTypesPerCategory(HealthDataCategory.SLEEP, listOf()),
                PermissionTypesPerCategory(
                    HealthDataCategory.VITALS,
                    listOf(FitnessPermissionType.HEART_RATE),
                ),
                PermissionTypesPerCategory(HealthDataCategory.WELLNESS, listOf()),
            )
        assertThat(testObserver.getLastValue())
            .isEqualTo(AllDataViewModel.AllDataState.WithData(expected))
    }

    @Test
    fun loadMedicalData_noMedicalData_returnsEmptyList() = runTest {
        doAnswer(
                prepareAnswer(
                    listOf(
                        MedicalResourceTypeInfo(MEDICAL_RESOURCE_TYPE_VACCINES, setOf()),
                        MedicalResourceTypeInfo(
                            MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES,
                            setOf(),
                        ),
                    )
                )
            )
            .`when`(manager)
            .queryAllMedicalResourceTypeInfos(ArgumentMatchers.any(), ArgumentMatchers.any())

        val testObserver = TestObserver<AllDataViewModel.AllDataState>()
        viewModel.allData.observeForever(testObserver)
        viewModel.loadAllMedicalData()
        advanceUntilIdle()

        val expected = emptyList<PermissionTypesPerCategory>()
        assertThat(testObserver.getLastValue())
            .isEqualTo(AllDataViewModel.AllDataState.WithData(expected))
    }

    @Test
    fun loadMedicalData_hasMedicalData_returnsMedicalData() = runTest {
        val medicalResourceTypeResources: List<MedicalResourceTypeInfo> =
            listOf(
                MedicalResourceTypeInfo(
                    MEDICAL_RESOURCE_TYPE_VACCINES,
                    setOf(TEST_MEDICAL_DATA_SOURCE),
                ),
                MedicalResourceTypeInfo(MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES, setOf()),
            )
        doAnswer(prepareAnswer(medicalResourceTypeResources))
            .`when`(manager)
            .queryAllMedicalResourceTypeInfos(ArgumentMatchers.any(), ArgumentMatchers.any())

        val testObserver = TestObserver<AllDataViewModel.AllDataState>()
        viewModel.allData.observeForever(testObserver)
        viewModel.loadAllMedicalData()
        advanceUntilIdle()

        val expected =
            listOf(PermissionTypesPerCategory(MEDICAL, listOf(MedicalPermissionType.VACCINES)))
        assertThat(testObserver.getLastValue())
            .isEqualTo(AllDataViewModel.AllDataState.WithData(expected))
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
        viewModel.setDeletionScreenStateValue(DeletionDataViewModel.DeletionScreenState.DELETE)

        assertThat(viewModel.getDeletionScreenStateValue())
            .isEqualTo(DeletionDataViewModel.DeletionScreenState.DELETE)
    }

    @Test
    fun getDeletionScreenState_getsCorrectValue() {
        viewModel.setDeletionScreenStateValue(DeletionDataViewModel.DeletionScreenState.VIEW)

        assertThat(viewModel.getDeletionScreenStateValue())
            .isEqualTo(DeletionDataViewModel.DeletionScreenState.VIEW)
    }

    @Test
    fun resetDeleteSet_emptiesDeleteSet() {
        viewModel.addToDeletionSet(FitnessPermissionType.MENSTRUATION)
        viewModel.addToDeletionSet(FitnessPermissionType.DISTANCE)
        viewModel.resetDeletionSet()

        assertThat(viewModel.setOfPermissionTypesToBeDeleted.value).isEmpty()
    }

    @Test
    fun getNumOfPermissionTypes_returnsCorrect() = runTest {
        val recordTypeInfoMap: Map<Class<out Record>, RecordTypeInfoResponse> =
            mapOf(
                StepsRecord::class.java to
                    RecordTypeInfoResponse(
                        HealthPermissionCategory.STEPS,
                        HealthDataCategory.ACTIVITY,
                        listOf(
                            getDataOrigin(TEST_APP_PACKAGE_NAME),
                            getDataOrigin(TEST_APP_PACKAGE_NAME_2),
                        ),
                    ),
                WeightRecord::class.java to
                    RecordTypeInfoResponse(
                        HealthPermissionCategory.WEIGHT,
                        HealthDataCategory.BODY_MEASUREMENTS,
                        listOf((getDataOrigin(TEST_APP_PACKAGE_NAME_2))),
                    ),
                HeartRateRecord::class.java to
                    RecordTypeInfoResponse(
                        HealthPermissionCategory.HEART_RATE,
                        HealthDataCategory.VITALS,
                        listOf((getDataOrigin(TEST_APP_PACKAGE_NAME))),
                    ),
            )
        doAnswer(prepareAnswer(recordTypeInfoMap))
            .`when`(manager)
            .queryAllRecordTypesInfo(any(), any())

        viewModel.loadAllFitnessData()
        advanceUntilIdle()

        assertThat(viewModel.getTheNumOfPermissionTypes()).isEqualTo(3)
    }

    private fun prepareAnswer(
        recordTypeInfoMap: Map<Class<out Record>, RecordTypeInfoResponse>
    ): (InvocationOnMock) -> Map<Class<out Record>, RecordTypeInfoResponse> {
        val answer = { args: InvocationOnMock ->
            val receiver = args.arguments[1] as OutcomeReceiver<Any?, *>
            receiver.onResult(recordTypeInfoMap)
            recordTypeInfoMap
        }
        return answer
    }

    private fun prepareAnswer(
        medicalResourceTypeInfo: List<MedicalResourceTypeInfo>
    ): (InvocationOnMock) -> List<MedicalResourceTypeInfo> {
        val answer = { args: InvocationOnMock ->
            val receiver = args.arguments[1] as OutcomeReceiver<Any?, *>
            receiver.onResult(medicalResourceTypeInfo)
            medicalResourceTypeInfo
        }
        return answer
    }
}
