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
package com.android.healthconnect.controller.tests.data.appdata

import android.content.Context
import android.health.connect.HealthConnectManager
import android.health.connect.HealthDataCategory
import android.health.connect.HealthDataCategory.SYMPTOMS
import android.health.connect.HealthPermissionCategory
import android.health.connect.MedicalResourceTypeInfo
import android.health.connect.ReadRecordsResponse
import android.health.connect.RecordTypeInfoResponse
import android.health.connect.datatypes.HeartRateRecord
import android.health.connect.datatypes.MedicalResource
import android.health.connect.datatypes.Record
import android.health.connect.datatypes.StepsRecord
import android.health.connect.datatypes.SymptomRecord
import android.health.connect.datatypes.WeightRecord
import android.os.OutcomeReceiver
import android.platform.test.annotations.RequiresFlagsDisabled
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.data.appdata.AllDataUseCase
import com.android.healthconnect.controller.data.appdata.PermissionTypesPerCategory
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.permissions.data.MedicalPermissionType
import com.android.healthconnect.controller.shared.HealthDataCategoryExtensions.MEDICAL
import com.android.healthconnect.controller.shared.app.AppInfoReader
import com.android.healthconnect.controller.shared.usecase.UseCaseResults.Success
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME_2
import com.android.healthconnect.controller.tests.utils.TEST_MEDICAL_DATA_SOURCE
import com.android.healthconnect.controller.tests.utils.TEST_MEDICAL_DATA_SOURCE_2
import com.android.healthconnect.controller.tests.utils.TEST_MEDICAL_DATA_SOURCE_DIFFERENT_APP
import com.android.healthconnect.controller.tests.utils.createFakeAppInfoReader
import com.android.healthconnect.controller.tests.utils.getDataOrigin
import com.android.healthfitness.flags.Flags
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.invocation.InvocationOnMock
import org.mockito.kotlin.any

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AllDataUseCaseTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)
    @get:Rule val checkFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    @BindValue lateinit var appInfoReader: AppInfoReader
    private lateinit var context: Context
    private val healthConnectManager: HealthConnectManager =
        Mockito.mock(HealthConnectManager::class.java)
    private lateinit var allDataUseCase: AllDataUseCase

    @Before
    fun setup() = runTest {
        MockitoAnnotations.initMocks(this)
        context = InstrumentationRegistry.getInstrumentation().context
        appInfoReader = createFakeAppInfoReader()
        hiltRule.inject()
        allDataUseCase = AllDataUseCase(healthConnectManager, Dispatchers.Main)
        Mockito.doAnswer { invocation ->
                val receiver = invocation.arguments[2] as OutcomeReceiver<ReadRecordsResponse<*>, *>
                receiver.onResult(ReadRecordsResponse<Record>(emptyList(), -1))
                null
            }
            .`when`(healthConnectManager)
            .readRecords<Record>(any(), any(), any())
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_SYMPTOMS, Flags.FLAG_SYMPTOMS_DB)
    fun loadFitnessData_symptomsFlagEnabled_returnsDataWrittenByGivenApp() = runTest {
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
        Mockito.doAnswer(prepareAnswer(recordTypeInfoMap))
            .`when`(healthConnectManager)
            .queryAllRecordTypesInfo(any(), any())

        val expected =
            Success(
                listOf(
                    PermissionTypesPerCategory(
                        HealthDataCategory.ACTIVITY,
                        listOf(FitnessPermissionType.STEPS),
                    ),
                    PermissionTypesPerCategory(HealthDataCategory.BODY_MEASUREMENTS, listOf()),
                    PermissionTypesPerCategory(HealthDataCategory.CYCLE_TRACKING, listOf()),
                    PermissionTypesPerCategory(HealthDataCategory.NUTRITION, listOf()),
                    PermissionTypesPerCategory(HealthDataCategory.SLEEP, listOf()),
                    PermissionTypesPerCategory(
                        HealthDataCategory.VITALS,
                        listOf(FitnessPermissionType.HEART_RATE),
                    ),
                    PermissionTypesPerCategory(HealthDataCategory.WELLNESS, listOf()),
                    PermissionTypesPerCategory(HealthDataCategory.SYMPTOMS, emptyList()),
                )
            )
        assertThat(allDataUseCase.loadFitnessAppData(TEST_APP_PACKAGE_NAME)).isEqualTo(expected)
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_SYMPTOMS, Flags.FLAG_SYMPTOMS_DB)
    fun loadFitnessData_symptomsFlagDisabled_returnsDataWrittenByGivenApp() = runTest {
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
        Mockito.doAnswer(prepareAnswer(recordTypeInfoMap))
            .`when`(healthConnectManager)
            .queryAllRecordTypesInfo(any(), any())

        val expected =
            Success(
                listOf(
                    PermissionTypesPerCategory(
                        HealthDataCategory.ACTIVITY,
                        listOf(FitnessPermissionType.STEPS),
                    ),
                    PermissionTypesPerCategory(HealthDataCategory.BODY_MEASUREMENTS, listOf()),
                    PermissionTypesPerCategory(HealthDataCategory.CYCLE_TRACKING, listOf()),
                    PermissionTypesPerCategory(HealthDataCategory.NUTRITION, listOf()),
                    PermissionTypesPerCategory(HealthDataCategory.SLEEP, listOf()),
                    PermissionTypesPerCategory(
                        HealthDataCategory.VITALS,
                        listOf(FitnessPermissionType.HEART_RATE),
                    ),
                    PermissionTypesPerCategory(HealthDataCategory.WELLNESS, listOf()),
                )
            )
        assertThat(allDataUseCase.loadFitnessAppData(TEST_APP_PACKAGE_NAME)).isEqualTo(expected)
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_SYMPTOMS, Flags.FLAG_SYMPTOMS_DB)
    fun loadAllFitnessData_symptomsFlagEnabled_returnsAllData() = runTest {
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
        Mockito.doAnswer(prepareAnswer(recordTypeInfoMap))
            .`when`(healthConnectManager)
            .queryAllRecordTypesInfo(any(), any())

        val expected =
            Success(
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
                    PermissionTypesPerCategory(HealthDataCategory.SYMPTOMS, emptyList()),
                )
            )
        assertThat(allDataUseCase.loadAllFitnessData()).isEqualTo(expected)
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_SYMPTOMS, Flags.FLAG_SYMPTOMS_DB)
    fun loadAllFitnessData_symptomsFlagDisabled_returnsAllData() = runTest {
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
        Mockito.doAnswer(prepareAnswer(recordTypeInfoMap))
            .`when`(healthConnectManager)
            .queryAllRecordTypesInfo(any(), any())

        val expected =
            Success(
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
            )
        assertThat(allDataUseCase.loadAllFitnessData()).isEqualTo(expected)
    }

    @Test
    fun loadMedicalAppData_apiReturnsEmptyList_returnEmptyList() = runTest {
        // This test case should not happen IRL because the API always returns all valid
        // MedicalPermissionTypes and makes the set of contributing data sources empty if there is
        // no data.
        Mockito.doAnswer(prepareAnswer(listOf()))
            .`when`(healthConnectManager)
            .queryAllMedicalResourceTypeInfos(any(), any())

        val actual = allDataUseCase.loadMedicalAppData(TEST_APP_PACKAGE_NAME)
        assertThat(actual).isEqualTo(Success(listOf<PermissionTypesPerCategory>()))
    }

    @Test
    fun loadMedicalAppData_noData_returnEmptyList() = runTest {
        val medicalResourceTypeResources: List<MedicalResourceTypeInfo> =
            listOf(MedicalResourceTypeInfo(MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES, setOf()))
        Mockito.doAnswer(prepareAnswer(medicalResourceTypeResources))
            .`when`(healthConnectManager)
            .queryAllMedicalResourceTypeInfos(any(), any())

        val actual = allDataUseCase.loadMedicalAppData(TEST_APP_PACKAGE_NAME)
        assertThat(actual).isEqualTo(Success(listOf<PermissionTypesPerCategory>()))
    }

    @Test
    fun loadMedicalAppData_immunization_returnsImmunization() = runTest {
        val medicalResourceTypeResources: List<MedicalResourceTypeInfo> =
            listOf(
                MedicalResourceTypeInfo(
                    MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES,
                    setOf(TEST_MEDICAL_DATA_SOURCE),
                )
            )
        Mockito.doAnswer(prepareAnswer(medicalResourceTypeResources))
            .`when`(healthConnectManager)
            .queryAllMedicalResourceTypeInfos(any(), any())

        val actual = allDataUseCase.loadMedicalAppData(TEST_APP_PACKAGE_NAME)
        val expected =
            listOf(PermissionTypesPerCategory(MEDICAL, listOf(MedicalPermissionType.VACCINES)))
        assertThat(actual).isEqualTo(Success(expected))
    }

    @Test
    fun loadMedicalAppData_multipleContributingApps_returnsImmunization() = runTest {
        val medicalResourceTypeResources: List<MedicalResourceTypeInfo> =
            listOf(
                MedicalResourceTypeInfo(
                    MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES,
                    setOf(
                        TEST_MEDICAL_DATA_SOURCE,
                        TEST_MEDICAL_DATA_SOURCE_2,
                        TEST_MEDICAL_DATA_SOURCE_DIFFERENT_APP,
                    ),
                )
            )
        Mockito.doAnswer(prepareAnswer(medicalResourceTypeResources))
            .`when`(healthConnectManager)
            .queryAllMedicalResourceTypeInfos(any(), any())

        val actual = allDataUseCase.loadMedicalAppData(TEST_APP_PACKAGE_NAME)
        val expected =
            listOf(PermissionTypesPerCategory(MEDICAL, listOf(MedicalPermissionType.VACCINES)))
        assertThat(actual).isEqualTo(Success(expected))
    }

    @Test
    fun loadMedicalAppData_onlyOtherAppsContributing_returnsEmptyList() = runTest {
        val medicalResourceTypeResources: List<MedicalResourceTypeInfo> =
            listOf(
                MedicalResourceTypeInfo(
                    MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES,
                    setOf(TEST_MEDICAL_DATA_SOURCE_DIFFERENT_APP),
                )
            )
        Mockito.doAnswer(prepareAnswer(medicalResourceTypeResources))
            .`when`(healthConnectManager)
            .queryAllMedicalResourceTypeInfos(any(), any())

        val actual = allDataUseCase.loadMedicalAppData(TEST_APP_PACKAGE_NAME)
        assertThat(actual).isEqualTo(Success(listOf<PermissionTypesPerCategory>()))
    }

    @Test
    fun loadAllMedicalAppData_apiReturnsEmptyList_returnEmptyList() = runTest {
        // This test case should not happen IRL because the API always returns all valid
        // MedicalPermissionTypes and makes the set of contributing data sources empty if there is
        // no data.
        Mockito.doAnswer(prepareAnswer(listOf()))
            .`when`(healthConnectManager)
            .queryAllMedicalResourceTypeInfos(any(), any())

        val actual = allDataUseCase.loadAllMedicalData()
        assertThat(actual).isEqualTo(Success(listOf<MedicalPermissionType>()))
    }

    @Test
    fun loadAllMedicalAppData_noData_returnEmptyList() = runTest {
        val medicalResourceTypeResources: List<MedicalResourceTypeInfo> =
            listOf(MedicalResourceTypeInfo(MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES, setOf()))
        Mockito.doAnswer(prepareAnswer(medicalResourceTypeResources))
            .`when`(healthConnectManager)
            .queryAllMedicalResourceTypeInfos(any(), any())

        val actual = allDataUseCase.loadAllMedicalData()
        assertThat(actual).isEqualTo(Success(listOf<MedicalPermissionType>()))
    }

    @Test
    fun loadAllMedicalAppData_immunization_returnsImmunization() = runTest {
        val medicalResourceTypeResources: List<MedicalResourceTypeInfo> =
            listOf(
                MedicalResourceTypeInfo(
                    MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES,
                    setOf(TEST_MEDICAL_DATA_SOURCE),
                )
            )
        Mockito.doAnswer(prepareAnswer(medicalResourceTypeResources))
            .`when`(healthConnectManager)
            .queryAllMedicalResourceTypeInfos(any(), any())

        val actual = allDataUseCase.loadAllMedicalData()
        val expected =
            listOf(PermissionTypesPerCategory(MEDICAL, listOf(MedicalPermissionType.VACCINES)))
        assertThat(actual).isEqualTo(Success(expected))
    }

    @Test
    fun loadAllMedicalAppData_multipleContributingApps_returnsImmunization() = runTest {
        val medicalResourceTypeResources: List<MedicalResourceTypeInfo> =
            listOf(
                MedicalResourceTypeInfo(
                    MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES,
                    setOf(
                        TEST_MEDICAL_DATA_SOURCE,
                        TEST_MEDICAL_DATA_SOURCE_2,
                        TEST_MEDICAL_DATA_SOURCE_DIFFERENT_APP,
                    ),
                )
            )
        Mockito.doAnswer(prepareAnswer(medicalResourceTypeResources))
            .`when`(healthConnectManager)
            .queryAllMedicalResourceTypeInfos(any(), any())

        val actual = allDataUseCase.loadAllMedicalData()
        val expected =
            listOf(PermissionTypesPerCategory(MEDICAL, listOf(MedicalPermissionType.VACCINES)))
        assertThat(actual).isEqualTo(Success(expected))
    }

    @Test
    fun loadHasAnyFitnessData_noData_returnsFalse() = runTest {
        val recordTypeInfoMap: Map<Class<out Record>, RecordTypeInfoResponse> =
            mapOf(
                StepsRecord::class.java to
                    RecordTypeInfoResponse(
                        HealthPermissionCategory.STEPS,
                        HealthDataCategory.ACTIVITY,
                        listOf(),
                    ),
                WeightRecord::class.java to
                    RecordTypeInfoResponse(
                        HealthPermissionCategory.WEIGHT,
                        HealthDataCategory.BODY_MEASUREMENTS,
                        listOf(),
                    ),
            )
        Mockito.doAnswer(prepareAnswer(recordTypeInfoMap))
            .`when`(healthConnectManager)
            .queryAllRecordTypesInfo(any(), any())

        assertThat(allDataUseCase.loadHasAnyFitnessData()).isEqualTo(Success(false))
    }

    @Test
    fun loadHasAnyFitnessData_someData_returnsTrue() = runTest {
        val recordTypeInfoMap: Map<Class<out Record>, RecordTypeInfoResponse> =
            mapOf(
                StepsRecord::class.java to
                    RecordTypeInfoResponse(
                        HealthPermissionCategory.STEPS,
                        HealthDataCategory.ACTIVITY,
                        listOf(getDataOrigin(TEST_APP_PACKAGE_NAME)),
                    ),
                WeightRecord::class.java to
                    RecordTypeInfoResponse(
                        HealthPermissionCategory.WEIGHT,
                        HealthDataCategory.BODY_MEASUREMENTS,
                        listOf(getDataOrigin(TEST_APP_PACKAGE_NAME)),
                    ),
            )
        Mockito.doAnswer(prepareAnswer(recordTypeInfoMap))
            .`when`(healthConnectManager)
            .queryAllRecordTypesInfo(any(), any())

        assertThat(allDataUseCase.loadHasAnyFitnessData()).isEqualTo(Success(true))
    }

    @Test
    fun loadHasAnyMedicalAppData_noData_returnFalse() = runTest {
        val medicalResourceTypeResources: List<MedicalResourceTypeInfo> =
            listOf(MedicalResourceTypeInfo(MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES, setOf()))
        Mockito.doAnswer(prepareAnswer(medicalResourceTypeResources))
            .`when`(healthConnectManager)
            .queryAllMedicalResourceTypeInfos(any(), any())

        val actual = allDataUseCase.loadHasAnyMedicalData()
        assertThat(actual).isEqualTo(Success(false))
    }

    @Test
    fun loadHasAnyMedicalAppData_hasData_returnTrue() = runTest {
        val medicalResourceTypeResources: List<MedicalResourceTypeInfo> =
            listOf(
                MedicalResourceTypeInfo(
                    MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES,
                    setOf(TEST_MEDICAL_DATA_SOURCE),
                )
            )
        Mockito.doAnswer(prepareAnswer(medicalResourceTypeResources))
            .`when`(healthConnectManager)
            .queryAllMedicalResourceTypeInfos(any(), any())

        val actual = allDataUseCase.loadHasAnyMedicalData()

        assertThat(actual).isEqualTo(Success(true))
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_SYMPTOMS, Flags.FLAG_SYMPTOMS_DB)
    fun loadAllFitnessData_symptomsFlagEnabled_apiError_returnsEmptySymptoms() = runTest {
        val recordTypeInfoMap: Map<Class<out Record>, RecordTypeInfoResponse> =
            mapOf(
                StepsRecord::class.java to
                    RecordTypeInfoResponse(
                        HealthPermissionCategory.STEPS,
                        HealthDataCategory.ACTIVITY,
                        listOf(getDataOrigin(TEST_APP_PACKAGE_NAME)),
                    )
            )
        Mockito.doAnswer(prepareAnswer(recordTypeInfoMap))
            .`when`(healthConnectManager)
            .queryAllRecordTypesInfo(any(), any())

        Mockito.doAnswer { invocation ->
                val receiver =
                    invocation.arguments[2] as OutcomeReceiver<ReadRecordsResponse<*>, Exception>
                receiver.onError(RuntimeException("API Error"))
                null
            }
            .`when`(healthConnectManager)
            .readRecords<Record>(any(), any(), any())

        val result = allDataUseCase.loadAllFitnessData()

        assertThat(result).isInstanceOf(Success::class.java)

        val categories = (result as Success).data

        val symptomsCategory = categories.find { it.category == SYMPTOMS }

        assertThat(symptomsCategory?.data).isEmpty()
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_SYMPTOMS, Flags.FLAG_SYMPTOMS_DB)
    fun loadAllFitnessData_symptomsFlagEnabled_withSymptomData_returnsMappedPermissions() =
        runTest {
            val recordTypeInfoMap: Map<Class<out Record>, RecordTypeInfoResponse> =
                mapOf(
                    SymptomRecord::class.java to
                        RecordTypeInfoResponse(
                            HealthPermissionCategory.SYMPTOM_SNORE,
                            SYMPTOMS,
                            listOf(getDataOrigin(TEST_APP_PACKAGE_NAME)),
                        )
                )
            Mockito.doAnswer(prepareAnswer(recordTypeInfoMap))
                .`when`(healthConnectManager)
                .queryAllRecordTypesInfo(any(), any())
            val result = allDataUseCase.loadAllFitnessData()
            assertThat(result).isInstanceOf(Success::class.java)
            val categories = (result as Success).data
            val symptomsCategory = categories.find { it.category == HealthDataCategory.SYMPTOMS }
            assertThat(symptomsCategory).isNotNull()
            assertThat(symptomsCategory?.data)
                .containsExactly(FitnessPermissionType.SYMPTOM_ABDOMINAL_PAIN)
        }

    private fun prepareAnswer(
        map: Map<Class<out Record>, RecordTypeInfoResponse>
    ): (InvocationOnMock) -> Map<Class<out Record>, RecordTypeInfoResponse> {
        val answer = { args: InvocationOnMock ->
            val receiver =
                args.arguments[1]
                    as OutcomeReceiver<Map<Class<out Record>, RecordTypeInfoResponse>, *>
            receiver.onResult(map)
            map
        }
        return answer
    }

    private fun prepareAnswer(
        medicalResourceTypeInfos: List<MedicalResourceTypeInfo>
    ): (InvocationOnMock) -> List<MedicalResourceTypeInfo> {
        val answer = { args: InvocationOnMock ->
            val receiver = args.arguments[1] as OutcomeReceiver<Any?, *>
            receiver.onResult(medicalResourceTypeInfos)
            medicalResourceTypeInfos
        }
        return answer
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_SYMPTOMS, Flags.FLAG_SYMPTOMS_DB)
    fun loadFitnessAppData_symptomsFlagEnabled_requestsFilteredSymptomData() = runTest {
        val recordTypeInfoMap: Map<Class<out Record>, RecordTypeInfoResponse> =
            mapOf(
                SymptomRecord::class.java to
                    RecordTypeInfoResponse(
                        HealthPermissionCategory.SYMPTOM_COUGH,
                        HealthDataCategory.SYMPTOMS,
                        listOf(getDataOrigin(TEST_APP_PACKAGE_NAME)),
                    )
            )
        Mockito.doAnswer(prepareAnswer(recordTypeInfoMap))
            .`when`(healthConnectManager)
            .queryAllRecordTypesInfo(any(), any())

        val result = allDataUseCase.loadFitnessAppData(TEST_APP_PACKAGE_NAME)
        assertThat(result).isInstanceOf(Success::class.java)
        val categories = (result as Success).data
        val symptomsCategory = categories.find { it.category == HealthDataCategory.SYMPTOMS }
        assertThat(symptomsCategory).isNotNull()
        assertThat(symptomsCategory?.data)
            .containsExactly(FitnessPermissionType.SYMPTOM_ABDOMINAL_PAIN)
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_SYMPTOMS, Flags.FLAG_SYMPTOMS_DB)
    fun loadAllFitnessData_symptomsFlagEnabled_requestsAllSymptomData() = runTest {
        val recordTypeInfoMap: Map<Class<out Record>, RecordTypeInfoResponse> =
            mapOf(
                SymptomRecord::class.java to
                    RecordTypeInfoResponse(
                        HealthPermissionCategory.SYMPTOM_COUGH,
                        HealthDataCategory.SYMPTOMS,
                        listOf(getDataOrigin(TEST_APP_PACKAGE_NAME)),
                    )
            )
        Mockito.doAnswer(prepareAnswer(recordTypeInfoMap))
            .`when`(healthConnectManager)
            .queryAllRecordTypesInfo(any(), any())

        allDataUseCase.loadAllFitnessData()

        Mockito.verify(healthConnectManager).queryAllRecordTypesInfo(any(), any())
    }
}
