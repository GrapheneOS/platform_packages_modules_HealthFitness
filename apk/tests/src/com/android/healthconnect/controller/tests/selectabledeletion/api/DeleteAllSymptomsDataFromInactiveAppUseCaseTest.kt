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
package com.android.healthconnect.controller.tests.selectabledeletion.api

import android.health.connect.DeleteUsingFiltersRequest
import android.health.connect.HealthConnectException
import android.health.connect.HealthConnectManager
import android.health.connect.datatypes.DataOrigin
import android.health.connect.datatypes.SymptomRecord
import android.os.OutcomeReceiver
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.selectabledeletion.DeletionType
import com.android.healthconnect.controller.selectabledeletion.api.DeleteAllSymptomsDataFromInactiveAppUseCase
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.util.concurrent.Executor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.invocation.InvocationOnMock
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class DeleteAllSymptomsDataFromInactiveAppUseCaseTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    private lateinit var useCase: DeleteAllSymptomsDataFromInactiveAppUseCase
    private val manager: HealthConnectManager = mock()

    private val filtersCaptor = argumentCaptor<DeleteUsingFiltersRequest>()

    @Before
    fun setup() {
        useCase = DeleteAllSymptomsDataFromInactiveAppUseCase(manager, Dispatchers.Main)
    }

    @Test
    fun invoke_deletesAllSymptomRecordsForApp() = runTest {
        doAnswer { invocation: InvocationOnMock ->
                val receiver =
                    invocation.getArgument(2) as OutcomeReceiver<Void, HealthConnectException>
                receiver.onResult(null)
                null
            }
            .whenever(manager)
            .deleteRecords(
                any<DeleteUsingFiltersRequest>(),
                any<Executor>(),
                any<OutcomeReceiver<Void, HealthConnectException>>(),
            )

        val deletionType =
            DeletionType.DeleteAllSymptomsDataFromInactiveApp(
                packageName = "test.package",
                appName = "Test App",
            )

        useCase.invoke(deletionType)

        verify(manager, times(1)).deleteRecords(filtersCaptor.capture(), any(), any())

        assertThat(filtersCaptor.firstValue.timeRangeFilter).isNull()
        assertThat(filtersCaptor.firstValue.dataOrigins)
            .containsExactly(DataOrigin.Builder().setPackageName("test.package").build())
        assertThat(filtersCaptor.firstValue.recordTypes).containsExactly(SymptomRecord::class.java)
    }
}
