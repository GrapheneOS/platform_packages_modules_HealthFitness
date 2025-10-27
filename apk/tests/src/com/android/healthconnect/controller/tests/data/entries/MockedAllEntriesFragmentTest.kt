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
package com.android.healthconnect.controller.tests.data.entries

import android.health.connect.AggregateRecordsResponse
import android.health.connect.AggregateResult
import android.health.connect.HealthConnectManager
import android.health.connect.ReadRecordsRequestUsingFilters
import android.health.connect.ReadRecordsResponse
import android.health.connect.datatypes.AggregationType
import android.health.connect.datatypes.Record
import android.health.connect.datatypes.StepsCadenceRecord
import android.health.connect.datatypes.StepsRecord
import android.os.OutcomeReceiver
import androidx.core.os.bundleOf
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.data.appdata.AppDataFragment.Companion.PERMISSION_TYPE_NAME_KEY
import com.android.healthconnect.controller.data.entries.AllEntriesFragment
import com.android.healthconnect.controller.data.entries.EntriesViewModel
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType.STEPS
import com.android.healthconnect.controller.service.DispatcherModule
import com.android.healthconnect.controller.service.HealthManagerModule
import com.android.healthconnect.controller.shared.usecase.DefaultDispatcher
import com.android.healthconnect.controller.shared.usecase.IoDispatcher
import com.android.healthconnect.controller.shared.usecase.MainDispatcher
import com.android.healthconnect.controller.tests.utils.CoroutineTestRule
import com.android.healthconnect.controller.tests.utils.FakeParentFragment
import com.android.healthconnect.controller.tests.utils.NESTED_FRAGMENT_TAG
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.assertCheckboxChecked
import com.android.healthconnect.controller.tests.utils.assertCheckboxNotChecked
import com.android.healthconnect.controller.tests.utils.assertCheckboxNotShown
import com.android.healthconnect.controller.tests.utils.forDataType
import com.android.healthconnect.controller.tests.utils.getMetaDataWithUniqueIds
import com.android.healthconnect.controller.tests.utils.getStepsRecordWithUniqueIds
import com.android.healthconnect.controller.tests.utils.launchNestedFragment
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import dagger.hilt.components.SingletonComponent
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.invocation.InvocationOnMock

@OptIn(ExperimentalCoroutinesApi::class)
@UninstallModules(HealthManagerModule::class, DispatcherModule::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MockedAllEntriesFragmentTest {

    @get:Rule val coroutineTestRule = CoroutineTestRule()
    @get:Rule val hiltRule = HiltAndroidRule(this)
    @BindValue val manager: HealthConnectManager = Mockito.mock(HealthConnectManager::class.java)
    private val NOW: Instant =
        LocalDate.now(ZoneId.systemDefault())
            .atStartOfDay()
            .atZone(ZoneId.systemDefault())
            .toInstant()
    private val recyclerViewId = R.id.data_entries_list

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun fragmentDisplaysCorrectly() = runTest {
        mockData()
        launchNestedFragment<AllEntriesFragment>(bundleOf(PERMISSION_TYPE_NAME_KEY to STEPS.name))
            .use {
                advanceUntilIdle()

                onView(withText("10 steps")).check(matches(isDisplayed()))
                onView(withText("20 steps")).check(matches(isDisplayed()))
                onView(withText("30 steps")).check(matches(isDisplayed()))
                onView(withText("15.2 steps/min")).check(matches(isDisplayed()))

                onView(withText("60 steps")).check(matches(isDisplayed()))
                onView(withText("Select all")).check(doesNotExist())

                assertCheckboxNotShown(recyclerViewId, "10 steps", 1)
                assertCheckboxNotShown(recyclerViewId, "20 steps", 2)
                assertCheckboxNotShown(recyclerViewId, "30 steps", 3)
                assertCheckboxNotShown(recyclerViewId, "15.2 steps/min", 4)
            }
    }

    @Test
    fun toggleDeletion_hidesAggregation_showsSelectAll_showsCheckboxes() = runTest {
        mockData()
        launchNestedFragment<AllEntriesFragment>(bundleOf(PERMISSION_TYPE_NAME_KEY to STEPS.name))
            .use { scenario ->
                advanceUntilIdle()
                scenario.onActivity { activity ->
                    val parentFragment =
                        activity.supportFragmentManager.findFragmentByTag("") as FakeParentFragment
                    val fragment =
                        parentFragment.childFragmentManager.findFragmentByTag(NESTED_FRAGMENT_TAG)
                    (fragment as AllEntriesFragment).triggerDeletionState(
                        EntriesViewModel.EntriesDeletionScreenState.DELETE
                    )
                }
                advanceUntilIdle()

                onView(withText("Select all")).check(matches(isDisplayed()))
                onView(withText("60 steps")).check(doesNotExist())

                assertCheckboxNotChecked(recyclerViewId, "10 steps", 1)
                assertCheckboxNotChecked(recyclerViewId, "20 steps", 2)
                assertCheckboxNotChecked(recyclerViewId, "30 steps", 3)
                assertCheckboxNotChecked(recyclerViewId, "15.2 steps/min", 4)
            }
    }

    @Test
    fun inDeletion_screenStateRemainsOnOrientationChange() = runTest {
        mockData()
        launchNestedFragment<AllEntriesFragment>(bundleOf(PERMISSION_TYPE_NAME_KEY to STEPS.name))
            .use { scenario ->
                advanceUntilIdle()
                scenario.onActivity { activity ->
                    val parentFragment =
                        activity.supportFragmentManager.findFragmentByTag("") as FakeParentFragment
                    val fragment =
                        parentFragment.childFragmentManager.findFragmentByTag(NESTED_FRAGMENT_TAG)
                    (fragment as AllEntriesFragment).triggerDeletionState(
                        EntriesViewModel.EntriesDeletionScreenState.DELETE
                    )
                }
                advanceUntilIdle()

                onView(withText("Select all")).check(matches(isDisplayed()))
                onView(withText("60 steps")).check(doesNotExist())

                assertCheckboxNotChecked(recyclerViewId, "10 steps", 1)
                assertCheckboxNotChecked(recyclerViewId, "20 steps", 2)
                assertCheckboxNotChecked(recyclerViewId, "30 steps", 3)
                assertCheckboxNotChecked(recyclerViewId, "15.2 steps/min", 4)

                onView(withText("10 steps")).perform(click())

                assertCheckboxChecked(recyclerViewId, "10 steps", 1)

                scenario.recreate()
                advanceUntilIdle()
                onView(withText("Select all")).check(matches(isDisplayed()))
                onView(withText("60 steps")).check(doesNotExist())

                assertCheckboxChecked(recyclerViewId, "10 steps", 1)
                assertCheckboxNotChecked(recyclerViewId, "20 steps", 2)
                assertCheckboxNotChecked(recyclerViewId, "30 steps", 3)
                assertCheckboxNotChecked(recyclerViewId, "15.2 steps/min", 4)
            }
    }

    @Test
    fun inDeletion_whenAllCheckboxesChecked_selectAllChecked() = runTest {
        mockData()
        launchNestedFragment<AllEntriesFragment>(bundleOf(PERMISSION_TYPE_NAME_KEY to STEPS.name))
            .use { scenario ->
                advanceUntilIdle()
                scenario.onActivity { activity ->
                    val parentFragment =
                        activity.supportFragmentManager.findFragmentByTag("") as FakeParentFragment
                    val fragment =
                        parentFragment.childFragmentManager.findFragmentByTag(NESTED_FRAGMENT_TAG)
                    (fragment as AllEntriesFragment).triggerDeletionState(
                        EntriesViewModel.EntriesDeletionScreenState.DELETE
                    )
                }
                advanceUntilIdle()

                onView(withText("Select all")).check(matches(isDisplayed()))
                onView(withText("60 steps")).check(doesNotExist())

                assertCheckboxNotChecked(recyclerViewId, "10 steps", 1)
                assertCheckboxNotChecked(recyclerViewId, "20 steps", 2)
                assertCheckboxNotChecked(recyclerViewId, "30 steps", 3)
                assertCheckboxNotChecked(recyclerViewId, "15.2 steps/min", 4)

                onView(withText("10 steps")).perform(click())
                onView(withText("20 steps")).perform(click())
                onView(withText("30 steps")).perform(click())
                onView(withText("15.2 steps/min")).perform(click())

                // assert select all checked
                assertCheckboxChecked(recyclerViewId, "Select all", 0)
            }
    }

    @Test
    fun inDeletion_whenOneCheckboxUnchecked_selectAllUnchecked() = runTest {
        mockData()
        launchNestedFragment<AllEntriesFragment>(bundleOf(PERMISSION_TYPE_NAME_KEY to STEPS.name))
            .use { scenario ->
                advanceUntilIdle()
                scenario.onActivity { activity ->
                    val parentFragment =
                        activity.supportFragmentManager.findFragmentByTag("") as FakeParentFragment
                    val fragment =
                        parentFragment.childFragmentManager.findFragmentByTag(NESTED_FRAGMENT_TAG)
                    (fragment as AllEntriesFragment).triggerDeletionState(
                        EntriesViewModel.EntriesDeletionScreenState.DELETE
                    )
                }
                advanceUntilIdle()

                onView(withText("Select all")).check(matches(isDisplayed()))
                onView(withText("60 steps")).check(doesNotExist())

                assertCheckboxNotChecked(recyclerViewId, "10 steps", 1)
                assertCheckboxNotChecked(recyclerViewId, "20 steps", 2)
                assertCheckboxNotChecked(recyclerViewId, "30 steps", 3)
                assertCheckboxNotChecked(recyclerViewId, "15.2 steps/min", 4)

                onView(withText("Select all")).perform(click())

                assertCheckboxChecked(recyclerViewId, "10 steps", 1)
                assertCheckboxChecked(recyclerViewId, "20 steps", 2)
                assertCheckboxChecked(recyclerViewId, "30 steps", 3)
                assertCheckboxChecked(recyclerViewId, "15.2 steps/min", 4)

                assertCheckboxChecked(recyclerViewId, "Select all", 0)

                onView(withText("10 steps")).perform(click())

                assertCheckboxNotChecked(recyclerViewId, "Select all", 0)
            }
    }

    @Test
    fun inDeletion_whenSelectAllChecked_allCheckboxesChecked() = runTest {
        mockData()
        launchNestedFragment<AllEntriesFragment>(bundleOf(PERMISSION_TYPE_NAME_KEY to STEPS.name))
            .use { scenario ->
                advanceUntilIdle()
                scenario.onActivity { activity ->
                    val parentFragment =
                        activity.supportFragmentManager.findFragmentByTag("") as FakeParentFragment
                    val fragment =
                        parentFragment.childFragmentManager.findFragmentByTag(NESTED_FRAGMENT_TAG)
                    (fragment as AllEntriesFragment).triggerDeletionState(
                        EntriesViewModel.EntriesDeletionScreenState.DELETE
                    )
                }
                advanceUntilIdle()

                onView(withText("Select all")).check(matches(isDisplayed()))
                onView(withText("60 steps")).check(doesNotExist())

                assertCheckboxNotChecked(recyclerViewId, "10 steps", 1)
                assertCheckboxNotChecked(recyclerViewId, "20 steps", 2)
                assertCheckboxNotChecked(recyclerViewId, "30 steps", 3)
                assertCheckboxNotChecked(recyclerViewId, "15.2 steps/min", 4)

                onView(withText("Select all")).perform(click())

                assertCheckboxChecked(recyclerViewId, "10 steps", 1)
                assertCheckboxChecked(recyclerViewId, "20 steps", 2)
                assertCheckboxChecked(recyclerViewId, "30 steps", 3)
                assertCheckboxChecked(recyclerViewId, "15.2 steps/min", 4)
            }
    }

    @Test
    fun inDeletion_whenSelectAllUnchecked_allCheckboxesUnchecked() = runTest {
        mockData()
        launchNestedFragment<AllEntriesFragment>(bundleOf(PERMISSION_TYPE_NAME_KEY to STEPS.name))
            .use { scenario ->
                advanceUntilIdle()
                scenario.onActivity { activity ->
                    val parentFragment =
                        activity.supportFragmentManager.findFragmentByTag("") as FakeParentFragment
                    val fragment =
                        parentFragment.childFragmentManager.findFragmentByTag(NESTED_FRAGMENT_TAG)
                    (fragment as AllEntriesFragment).triggerDeletionState(
                        EntriesViewModel.EntriesDeletionScreenState.DELETE
                    )
                }
                advanceUntilIdle()

                onView(withText("Select all")).check(matches(isDisplayed()))
                onView(withText("60 steps")).check(doesNotExist())

                assertCheckboxNotChecked(recyclerViewId, "10 steps", 1)
                assertCheckboxNotChecked(recyclerViewId, "20 steps", 2)
                assertCheckboxNotChecked(recyclerViewId, "30 steps", 3)
                assertCheckboxNotChecked(recyclerViewId, "15.2 steps/min", 4)

                onView(withText("Select all")).perform(click())

                assertCheckboxChecked(recyclerViewId, "10 steps", 1)
                assertCheckboxChecked(recyclerViewId, "20 steps", 2)
                assertCheckboxChecked(recyclerViewId, "30 steps", 3)
                assertCheckboxChecked(recyclerViewId, "15.2 steps/min", 4)

                onView(withText("Select all")).perform(click())

                assertCheckboxNotChecked(recyclerViewId, "10 steps", 1)
                assertCheckboxNotChecked(recyclerViewId, "20 steps", 2)
                assertCheckboxNotChecked(recyclerViewId, "30 steps", 3)
                assertCheckboxNotChecked(recyclerViewId, "15.2 steps/min", 4)
            }
    }

    private fun mockData() {
        val stepsRecordsList =
            listOf(
                getStepsRecordWithUniqueIds(10, NOW),
                getStepsRecordWithUniqueIds(20, NOW),
                getStepsRecordWithUniqueIds(30, NOW),
            )
        Mockito.doAnswer(prepareRecordsAnswer(stepsRecordsList))
            .`when`(manager)
            .readRecords(
                ArgumentMatchers.argThat<ReadRecordsRequestUsingFilters<Record>> { request ->
                    request.forDataType(dataType = StepsRecord::class.java)
                },
                ArgumentMatchers.any(),
                ArgumentMatchers.any(),
            )

        Mockito.doAnswer(prepareRecordsAnswer(listOf(getStepsCadence(listOf(10.3, 20.1)))))
            .`when`(manager)
            .readRecords(
                ArgumentMatchers.argThat<ReadRecordsRequestUsingFilters<Record>> { request ->
                    request.forDataType(dataType = StepsCadenceRecord::class.java)
                },
                ArgumentMatchers.any(),
                ArgumentMatchers.any(),
            )

        Mockito.doAnswer(prepareStepsAggregationAnswer())
            .`when`(manager)
            .aggregate<Long>(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())
    }

    private fun prepareRecordsAnswer(records: List<Record>): (InvocationOnMock) -> Nothing? {
        val answer = { args: InvocationOnMock ->
            val receiver = args.arguments[2] as OutcomeReceiver<ReadRecordsResponse<Record>, *>
            receiver.onResult(ReadRecordsResponse(records, -1))
            null
        }
        return answer
    }

    private fun prepareStepsAggregationAnswer():
        (InvocationOnMock) -> AggregateRecordsResponse<Long> {
        val answer = { args: InvocationOnMock ->
            val receiver = args.arguments[2] as OutcomeReceiver<AggregateRecordsResponse<Long>, *>
            receiver.onResult(getStepsAggregationResponse())
            getStepsAggregationResponse()
        }
        return answer
    }

    private fun getStepsAggregationResponse(): AggregateRecordsResponse<Long> {
        val aggregationResult =
            AggregateResult<Long>(
                60,
                null,
                AggregateResult.convertDataOrigins(listOf(TEST_APP_PACKAGE_NAME)),
            )
        return AggregateRecordsResponse<Long>(
            mapOf(
                AggregationType.AggregationTypeIdentifier.STEPS_RECORD_COUNT_TOTAL to
                    aggregationResult
            )
        )
    }

    private fun getStepsCadence(samples: List<Double>): StepsCadenceRecord {
        return StepsCadenceRecord.Builder(
                getMetaDataWithUniqueIds(),
                NOW,
                NOW.plusSeconds(samples.size.toLong() + 1),
                samples.map { rate ->
                    StepsCadenceRecord.StepsCadenceRecordSample(rate, NOW.plusSeconds(1))
                },
            )
            .build()
    }

    @Module
    @InstallIn(SingletonComponent::class)
    object TestCoroutineModule {
        @DefaultDispatcher
        @Provides
        fun providesDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Main

        @IoDispatcher @Provides fun providesIoDispatcher(): CoroutineDispatcher = Dispatchers.Main

        @MainDispatcher
        @Provides
        fun providesMainDispatcher(): CoroutineDispatcher = Dispatchers.Main
    }
}
