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
package com.android.healthconnect.controller.tests.data.entries

import android.content.Context
import android.health.connect.datatypes.MenstruationPeriodRecord
import android.health.connect.datatypes.StepsRecord
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.data.entries.EntriesViewModel
import com.android.healthconnect.controller.data.entries.FormattedEntry
import com.android.healthconnect.controller.data.entries.datenavigation.DateNavigationPeriod
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.permissions.data.MedicalPermissionType
import com.android.healthconnect.controller.shared.app.AppInfoReader
import com.android.healthconnect.controller.tests.utils.InstantTaskExecutorRule
import com.android.healthconnect.controller.tests.utils.TEST_MEDICAL_RESOURCE_IMMUNIZATION
import com.android.healthconnect.controller.tests.utils.TestObserver
import com.android.healthconnect.controller.tests.utils.TestTimeSource
import com.android.healthconnect.controller.tests.utils.di.FakeLoadDataAggregationsUseCase
import com.android.healthconnect.controller.tests.utils.di.FakeLoadDataEntriesUseCase
import com.android.healthconnect.controller.tests.utils.di.FakeLoadMedicalEntriesUseCase
import com.android.healthconnect.controller.tests.utils.di.FakeLoadMenstruationDataUseCase
import com.android.healthconnect.controller.utils.TimeSource
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.Instant
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
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class EntriesViewModelTest {

    companion object {
        private fun formattedAggregation(aggregation: String) =
            FormattedEntry.FormattedAggregation(
                aggregation = aggregation,
                aggregationA11y = aggregation,
                contributingApps = "Test App",
            )

        private val FORMATTED_STEPS =
            FormattedEntry.FormattedDataEntry(
                uuid = "test_id",
                header = "7:06 - 7:06",
                headerA11y = "from 7:06 to 7:06",
                title = "12 steps",
                titleA11y = "12 steps",
                dataType = StepsRecord::class,
            )
        private val FORMATTED_STEPS_2 =
            FormattedEntry.FormattedDataEntry(
                uuid = "test_id_2",
                header = "8:06 - 8:06",
                headerA11y = "from 8:06 to 8:06",
                title = "15 steps",
                titleA11y = "15 steps",
                dataType = StepsRecord::class,
            )
        private val FORMATTED_MENSTRUATION_PERIOD =
            FormattedEntry.FormattedDataEntry(
                uuid = "test_id",
                header = "8:06 - 8:06",
                headerA11y = "from 8:06 to 8:06",
                title = "15 steps",
                titleA11y = "15 steps",
                dataType = MenstruationPeriodRecord::class,
            )
        private val FORMATTED_IMMUNIZATION =
            FormattedEntry.FormattedMedicalDataEntry(
                header = "Health Connect Toolbox",
                headerA11y = "Health Connect Toolbox",
                title = "Covid vaccine",
                titleA11y = "Covid vaccine",
                medicalResourceId = TEST_MEDICAL_RESOURCE_IMMUNIZATION.id,
            )
    }

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()
    private val testDispatcher = UnconfinedTestDispatcher()

    @Inject lateinit var appInfoReader: AppInfoReader
    private val timeSource: TimeSource = TestTimeSource
    private val fakeLoadDataEntriesUseCase = FakeLoadDataEntriesUseCase()
    private val fakeLoadMenstruationDataUseCase = FakeLoadMenstruationDataUseCase()
    private val fakeLoadDataAggregationsUseCase = FakeLoadDataAggregationsUseCase()
    private val fakeLoadMedicalEntriesUseCase = FakeLoadMedicalEntriesUseCase()

    private lateinit var viewModel: EntriesViewModel
    private lateinit var context: Context

    @Before
    fun setup() {
        hiltRule.inject()
        Dispatchers.setMain(testDispatcher)
        context = InstrumentationRegistry.getInstrumentation().context
        viewModel =
            EntriesViewModel(
                appInfoReader,
                fakeLoadDataEntriesUseCase,
                fakeLoadMenstruationDataUseCase,
                fakeLoadDataAggregationsUseCase,
                fakeLoadMedicalEntriesUseCase,
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadDataEntries_hasStepsData_returnsFragmentStateWitAggregationAndSteps() = runTest {
        fakeLoadDataEntriesUseCase.updateList(listOf(FORMATTED_STEPS))
        fakeLoadDataAggregationsUseCase.updateAggregation(formattedAggregation("12 steps"))
        val testObserver = TestObserver<EntriesViewModel.EntriesFragmentState>()
        viewModel.entries.observeForever(testObserver)
        viewModel.loadEntries(
            FitnessPermissionType.STEPS,
            Instant.ofEpochMilli(timeSource.currentTimeMillis()),
            DateNavigationPeriod.PERIOD_WEEK,
        )
        advanceUntilIdle()

        val actual = testObserver.getLastValue()
        val expected =
            EntriesViewModel.EntriesFragmentState.With(
                listOf(formattedAggregation("12 steps"), FORMATTED_STEPS)
            )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun loadDataEntries_hasMultipleSteps_returnsFragmentStateWitAggregationAndSteps() = runTest {
        fakeLoadDataEntriesUseCase.updateList(listOf(FORMATTED_STEPS, FORMATTED_STEPS_2))
        fakeLoadDataAggregationsUseCase.updateAggregation(formattedAggregation("27 steps"))
        val testObserver = TestObserver<EntriesViewModel.EntriesFragmentState>()
        viewModel.entries.observeForever(testObserver)
        viewModel.loadEntries(
            FitnessPermissionType.STEPS,
            Instant.ofEpochMilli(timeSource.currentTimeMillis()),
            DateNavigationPeriod.PERIOD_WEEK,
        )
        advanceUntilIdle()

        val actual = testObserver.getLastValue()
        val expected =
            EntriesViewModel.EntriesFragmentState.With(
                listOf(formattedAggregation("27 steps"), FORMATTED_STEPS, FORMATTED_STEPS_2)
            )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun loadDataEntries_hasMenstruationData_returnsFragmentStateWithData() = runTest {
        fakeLoadMenstruationDataUseCase.updateList(listOf(FORMATTED_MENSTRUATION_PERIOD))
        val testObserver = TestObserver<EntriesViewModel.EntriesFragmentState>()
        viewModel.entries.observeForever(testObserver)
        viewModel.loadEntries(
            FitnessPermissionType.MENSTRUATION,
            Instant.ofEpochMilli(timeSource.currentTimeMillis()),
            DateNavigationPeriod.PERIOD_WEEK,
        )
        advanceUntilIdle()

        val actual = testObserver.getLastValue()
        val expected =
            EntriesViewModel.EntriesFragmentState.With(listOf(FORMATTED_MENSTRUATION_PERIOD))
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun loadDataEntries_hasImmunizationData_returnsFragmentStateWitImmunization() = runTest {
        fakeLoadMedicalEntriesUseCase.updateList(listOf(FORMATTED_IMMUNIZATION))
        val testObserver = TestObserver<EntriesViewModel.EntriesFragmentState>()
        viewModel.entries.observeForever(testObserver)
        viewModel.loadEntries(
            MedicalPermissionType.VACCINES,
            Instant.ofEpochMilli(timeSource.currentTimeMillis()),
            DateNavigationPeriod.PERIOD_WEEK,
        )
        advanceUntilIdle()

        val actual = testObserver.getLastValue()
        val expected = EntriesViewModel.EntriesFragmentState.With(listOf(FORMATTED_IMMUNIZATION))
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun addToDeleteSet_updatesDeleteMapCorrectly() {
        assertThat(viewModel.mapOfEntriesToBeDeleted.value.orEmpty()).isEmpty()

        viewModel.addToDeleteMap(FORMATTED_STEPS.uuid, FORMATTED_STEPS.dataType)

        assertThat(viewModel.mapOfEntriesToBeDeleted.value)
            .containsExactlyEntriesIn(mapOf(FORMATTED_STEPS.uuid to FORMATTED_STEPS.dataType))
    }

    @Test
    fun removeFromDeleteSet_updatesDeleteMapCorrectly() {
        viewModel.addToDeleteMap(FORMATTED_STEPS.uuid, FORMATTED_STEPS.dataType)
        viewModel.addToDeleteMap(FORMATTED_STEPS_2.uuid, FORMATTED_STEPS.dataType)
        viewModel.removeFromDeleteMap(FORMATTED_STEPS.uuid)

        assertThat(viewModel.mapOfEntriesToBeDeleted.value)
            .containsExactlyEntriesIn(mapOf(FORMATTED_STEPS_2.uuid to FORMATTED_STEPS.dataType))
    }

    @Test
    fun setScreenState_setsCorrectly() {
        viewModel.setScreenState(EntriesViewModel.EntriesDeletionScreenState.DELETE)

        assertThat(viewModel.screenState.value)
            .isEqualTo(EntriesViewModel.EntriesDeletionScreenState.DELETE)
    }

    @Test
    fun setAllEntriesSelectedValue_setCorrectValue() {
        viewModel.setAllEntriesSelectedValue(true)

        assertThat(viewModel.allEntriesSelected.value).isTrue()
    }

    @Test
    fun getEntriesList_getsCorrectValue() = runTest {
        fakeLoadDataEntriesUseCase.updateList(listOf(FORMATTED_STEPS))
        fakeLoadDataAggregationsUseCase.updateAggregation(formattedAggregation("12 steps"))
        val testObserver = TestObserver<EntriesViewModel.EntriesFragmentState>()
        viewModel.entries.observeForever(testObserver)
        viewModel.loadEntries(
            FitnessPermissionType.STEPS,
            Instant.ofEpochMilli(timeSource.currentTimeMillis()),
            DateNavigationPeriod.PERIOD_WEEK,
        )

        advanceUntilIdle()

        assertThat(viewModel.getEntriesList())
            .isEqualTo(mutableListOf(formattedAggregation("12 steps"), FORMATTED_STEPS))
    }

    @Test
    fun setPeriodToWeek_allEntriesAddedToDeleteSet_allEntriesSelectedValueSetToTrue() = runTest {
        fakeLoadDataEntriesUseCase.updateList(listOf(FORMATTED_STEPS))
        fakeLoadDataAggregationsUseCase.updateAggregation(formattedAggregation("12 steps"))
        val testObserver = TestObserver<EntriesViewModel.EntriesFragmentState>()
        viewModel.entries.observeForever(testObserver)
        viewModel.loadEntries(
            FitnessPermissionType.STEPS,
            Instant.ofEpochMilli(timeSource.currentTimeMillis()),
            DateNavigationPeriod.PERIOD_WEEK,
        )

        viewModel.addToDeleteMap(FORMATTED_STEPS.uuid, FORMATTED_STEPS.dataType)

        advanceUntilIdle()

        assertThat(viewModel.allEntriesSelected.value).isTrue()
    }
}
