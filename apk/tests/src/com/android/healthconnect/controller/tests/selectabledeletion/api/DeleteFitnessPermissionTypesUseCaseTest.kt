/*
 * Copyright (C) 2023 The Android Open Source Project
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
package com.android.healthconnect.controller.tests.selectabledeletion.api

import android.health.connect.DeleteUsingFiltersRequest
import android.health.connect.HealthConnectException
import android.health.connect.HealthConnectManager
import android.health.connect.RecordIdFilter
import android.health.connect.datatypes.CyclingPedalingCadenceRecord
import android.health.connect.datatypes.ExerciseSessionRecord
import android.health.connect.datatypes.HeartRateRecord
import android.health.connect.datatypes.MenstruationFlowRecord
import android.health.connect.datatypes.MenstruationPeriodRecord
import android.health.connect.datatypes.SleepSessionRecord
import android.health.connect.datatypes.StepsCadenceRecord
import android.health.connect.datatypes.StepsRecord
import android.health.connect.datatypes.SymptomRecord
import android.os.OutcomeReceiver
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.permissions.data.MedicalPermissionType
import com.android.healthconnect.controller.selectabledeletion.DeletionType.DeleteHealthPermissionTypes
import com.android.healthconnect.controller.selectabledeletion.api.DeleteFitnessPermissionTypesUseCase
import com.android.healthfitness.flags.Flags
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mockito
import org.mockito.Mockito.doAnswer
import org.mockito.MockitoAnnotations
import org.mockito.invocation.InvocationOnMock
import org.mockito.kotlin.any

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class DeleteFitnessPermissionTypesUseCaseTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)
    @get:Rule val checkFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    private lateinit var useCase: DeleteFitnessPermissionTypesUseCase
    var manager: HealthConnectManager = Mockito.mock(HealthConnectManager::class.java)

    @Captor lateinit var filtersCaptor: ArgumentCaptor<DeleteUsingFiltersRequest>
    @Captor lateinit var idFiltersCaptor: ArgumentCaptor<List<RecordIdFilter>>

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        useCase = DeleteFitnessPermissionTypesUseCase(manager, Dispatchers.Main)
    }

    @Test
    fun invoke_deletePermissionTypes_callsHealthManager() = runTest {
        doAnswer(prepareAnswer())
            .`when`(manager)
            .deleteRecords(any<DeleteUsingFiltersRequest>(), any(), any())

        val deletePermissionType =
            DeleteHealthPermissionTypes(
                setOf(
                    FitnessPermissionType.STEPS,
                    FitnessPermissionType.HEART_RATE,
                    FitnessPermissionType.SLEEP,
                    FitnessPermissionType.EXERCISE,
                    FitnessPermissionType.MENSTRUATION,
                    MedicalPermissionType.VACCINES,
                ),
                8,
            )

        useCase.invoke(deletePermissionType)

        Mockito.verify(manager, Mockito.times(1))
            .deleteRecords(filtersCaptor.capture(), any(), any())

        assertThat(filtersCaptor.value.timeRangeFilter).isNull()
        assertThat(filtersCaptor.value.dataOrigins).isEmpty()
        assertThat(filtersCaptor.value.recordTypes)
            .containsExactly(
                StepsRecord::class.java,
                StepsCadenceRecord::class.java,
                HeartRateRecord::class.java,
                SleepSessionRecord::class.java,
                ExerciseSessionRecord::class.java,
                MenstruationFlowRecord::class.java,
                MenstruationPeriodRecord::class.java,
                CyclingPedalingCadenceRecord::class.java,
            )
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_SYMPTOMS, Flags.FLAG_SYMPTOMS_DB)
    fun invoke_deleteAnySymptomPermissionType_deletesAllSymptomRecords() = runTest {
        doAnswer(prepareAnswer())
            .`when`(manager)
            .deleteRecords(any<DeleteUsingFiltersRequest>(), any(), any())

        val deletePermissionType =
            DeleteHealthPermissionTypes(setOf(FitnessPermissionType.SYMPTOM_COUGH), 1)

        useCase.invoke(deletePermissionType)

        Mockito.verify(manager, Mockito.times(1))
            .deleteRecords(filtersCaptor.capture(), any(), any())

        assertThat(filtersCaptor.value.timeRangeFilter).isNull()
        assertThat(filtersCaptor.value.dataOrigins).isEmpty()
        assertThat(filtersCaptor.value.recordTypes).containsExactly(SymptomRecord::class.java)
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_SYMPTOMS, Flags.FLAG_SYMPTOMS_DB)
    fun invoke_deleteSymptomAndOtherType_deletesAllSymptomRecordsAndOther() = runTest {
        doAnswer(prepareAnswer())
            .`when`(manager)
            .deleteRecords(any<DeleteUsingFiltersRequest>(), any(), any())

        val deletePermissionType =
            DeleteHealthPermissionTypes(
                setOf(FitnessPermissionType.SYMPTOM_FEVER, FitnessPermissionType.STEPS),
                2,
            )

        useCase.invoke(deletePermissionType)

        Mockito.verify(manager, Mockito.times(1))
            .deleteRecords(filtersCaptor.capture(), any(), any())

        assertThat(filtersCaptor.value.timeRangeFilter).isNull()
        assertThat(filtersCaptor.value.dataOrigins).isEmpty()
        assertThat(filtersCaptor.value.recordTypes)
            .containsExactly(
                SymptomRecord::class.java,
                StepsRecord::class.java,
                StepsCadenceRecord::class.java,
            )
    }

    private fun prepareAnswer(): (InvocationOnMock) -> Nothing? {
        val answer = { invocation: InvocationOnMock ->
            val receiver =
                invocation.getArgument(2) as OutcomeReceiver<Void, HealthConnectException>
            receiver.onResult(null)
            null
        }
        return answer
    }
}
