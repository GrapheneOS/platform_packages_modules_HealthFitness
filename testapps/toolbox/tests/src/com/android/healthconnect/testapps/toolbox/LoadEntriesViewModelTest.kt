/**
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.healthconnect.testapps.toolbox

import android.content.Context
import android.health.connect.datatypes.Record
import android.health.connect.datatypes.WeightRecord
import android.health.connect.datatypes.units.Mass
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.testapps.toolbox.read.controller.DataEntriesLoader
import com.android.healthconnect.testapps.toolbox.read.controller.LoadEntriesInput
import com.android.healthconnect.testapps.toolbox.utils.GeneralUtils
import com.android.healthconnect.testapps.toolbox.viewmodels.DataState
import com.android.healthconnect.testapps.toolbox.viewmodels.LoadEntriesViewModel
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import java.time.Instant
import java.time.temporal.ChronoUnit


@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class LoadEntriesViewModelTest {

    private val mockLoadEntries = mock<DataEntriesLoader>()
    private lateinit var viewModel: LoadEntriesViewModel
    private val testDispatcher = StandardTestDispatcher()
    private val dataType = Constants.HealthPermissionType.WEIGHT
    private val startTime = Instant.now().truncatedTo(ChronoUnit.DAYS)
    private val endTime = Instant.now()
    private val input = LoadEntriesInput(
        dataType = dataType,
        startTime = startTime,
        endTime = endTime
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = LoadEntriesViewModel(mockLoadEntries)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadEntriesViewModel_initializes_asLoading() = runTest {
        val currentRecordDataState = viewModel.entriesState.value
        assertThat(currentRecordDataState is DataState.Loading).isTrue()
    }

    @Test
    fun loadEntriesViewModel_requestsData_returnsErrorOnException() = runTest {
        val securityException = SecurityException("Security Exception")
        mockLoadEntries.stub {
            onBlocking { load(input) } doThrow (securityException)
        }

        viewModel.loadEntries(input)
        testDispatcher.scheduler.advanceUntilIdle()
        val currentRecordDataState = viewModel.entriesState.first()

        assertThat(currentRecordDataState is DataState.Error).isTrue()
        assertThat((currentRecordDataState as DataState.Error).exception)
            .isEqualTo(securityException)
    }

    @Test
    fun loadEntriesViewModel_requestsDataWithSufficientPermissions_returnsListOfRecords() = runTest {
        val context: Context = ApplicationProvider.getApplicationContext()
        val now = Instant.now().truncatedTo(ChronoUnit.DAYS)
        val weightRecord1 =
            WeightRecord.Builder(GeneralUtils.getMetaData(context), now, Mass.fromGrams(60000.0)).build()
        val weightRecord2 =
            WeightRecord.Builder(GeneralUtils.getMetaData(context), now, Mass.fromGrams(65000.0)).build()
        val recordList: List<Record> = listOf(weightRecord1, weightRecord2)
        mockLoadEntries.stub {
            onBlocking { load(input) } doReturn (recordList)
        }

        viewModel.loadEntries(input)
        testDispatcher.scheduler.advanceUntilIdle()
        val currentRecordDataState = viewModel.entriesState.first()

        assertThat(currentRecordDataState is DataState.Success).isTrue()
        assertThat((currentRecordDataState as DataState.Success).records).containsExactly(
            weightRecord1,
            weightRecord2
        )
    }
}