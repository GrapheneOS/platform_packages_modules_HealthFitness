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
 *
 *
 */

/**
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.tests.data.entrydetails

import android.content.Context
import android.health.connect.HealthConnectManager
import android.health.connect.datatypes.ExerciseCompletionGoal
import android.health.connect.datatypes.ExercisePerformanceGoal
import android.health.connect.datatypes.ExerciseRoute
import android.health.connect.datatypes.ExerciseSegmentType
import android.health.connect.datatypes.ExerciseSessionRecord
import android.health.connect.datatypes.HeartRateRecord
import android.health.connect.datatypes.PlannedExerciseSessionRecord
import android.health.connect.datatypes.PlannedExerciseStep
import android.health.connect.datatypes.SkinTemperatureRecord
import android.health.connect.datatypes.SleepSessionRecord
import android.health.connect.datatypes.units.Length
import android.health.connect.datatypes.units.Velocity
import androidx.lifecycle.MutableLiveData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.data.entries.FormattedEntry
import com.android.healthconnect.controller.data.entries.FormattedEntry.ExercisePerformanceGoalEntry
import com.android.healthconnect.controller.data.entries.FormattedEntry.ExerciseSessionEntry
import com.android.healthconnect.controller.data.entries.FormattedEntry.PlannedExerciseBlockEntry
import com.android.healthconnect.controller.data.entries.FormattedEntry.PlannedExerciseFormattedSectionContent
import com.android.healthconnect.controller.data.entries.FormattedEntry.PlannedExerciseSessionEntry
import com.android.healthconnect.controller.data.entries.FormattedEntry.PlannedExerciseStepEntry
import com.android.healthconnect.controller.data.entries.FormattedEntry.SeriesDataEntry
import com.android.healthconnect.controller.data.entries.FormattedEntry.SessionHeader
import com.android.healthconnect.controller.data.entries.FormattedEntry.SleepSessionEntry
import com.android.healthconnect.controller.data.entrydetails.DataEntryDetailsFragment
import com.android.healthconnect.controller.data.entrydetails.DataEntryDetailsViewModel
import com.android.healthconnect.controller.data.entrydetails.DataEntryDetailsViewModel.DateEntryFragmentState.Loading
import com.android.healthconnect.controller.data.entrydetails.DataEntryDetailsViewModel.DateEntryFragmentState.LoadingFailed
import com.android.healthconnect.controller.data.entrydetails.DataEntryDetailsViewModel.DateEntryFragmentState.WithData
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType.EXERCISE
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType.HEART_RATE
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType.PLANNED_EXERCISE
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType.SKIN_TEMPERATURE
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType.SLEEP
import com.android.healthconnect.controller.service.HealthManagerModule
import com.android.healthconnect.controller.shared.app.AppInfoReader
import com.android.healthconnect.controller.tests.utils.TestData.WARSAW_ROUTE
import com.android.healthconnect.controller.tests.utils.createFakeAppInfoReader
import com.android.healthconnect.controller.tests.utils.getPlannedExerciseBlock
import com.android.healthconnect.controller.tests.utils.getPlannedExerciseStep
import com.android.healthconnect.controller.tests.utils.launchFragment
import com.android.healthconnect.controller.tests.utils.setLocale
import com.android.healthconnect.controller.utils.logging.EntryDetailsElement
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.PageName
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import java.time.ZoneId
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.test.runTest
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.atMost
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@HiltAndroidTest
@UninstallModules(HealthManagerModule::class)
@RunWith(AndroidJUnit4::class)
class DataEntryDetailsFragmentTest {
    @get:Rule val hiltRule = HiltAndroidRule(this)

    @BindValue lateinit var appInfoReader: AppInfoReader
    @BindValue val viewModel: DataEntryDetailsViewModel = mock()
    @BindValue val healthConnectManager: HealthConnectManager = mock()
    private lateinit var context: Context

    @BindValue val healthConnectLogger: HealthConnectLogger = mock()

    @Before
    fun setup() = runTest {
        appInfoReader = createFakeAppInfoReader()
        hiltRule.inject()
        context = InstrumentationRegistry.getInstrumentation().context
        context.setLocale(Locale.UK)
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("UTC")))
    }

    @After
    fun tearDown() {
        reset(healthConnectLogger)
    }

    @Test
    fun dataEntriesDetailsInit_showsFragment() {
        whenever(viewModel.sessionData).thenReturn(MutableLiveData(WithData(emptyList())))

        launchFragment<DataEntryDetailsFragment>(
                DataEntryDetailsFragment.createBundle(
                    permissionType = SLEEP,
                    entryId = "1",
                    showDataOrigin = true,
                )
            )
            .use { onView(withId(R.id.loading)).check(matches(not(isDisplayed()))) }
    }

    @Test
    fun dataEntriesDetailsInit_error_showsError() {
        whenever(viewModel.sessionData).thenReturn(MutableLiveData(LoadingFailed))

        launchFragment<DataEntryDetailsFragment>(
                DataEntryDetailsFragment.createBundle(
                    permissionType = SLEEP,
                    entryId = "1",
                    showDataOrigin = true,
                )
            )
            .use { onView(withId(R.id.error_view)).check(matches(isDisplayed())) }
    }

    @Test
    fun dataEntriesDetailsInit_loading_showsLoading() {
        whenever(viewModel.sessionData).thenReturn(MutableLiveData(Loading))

        launchFragment<DataEntryDetailsFragment>(
                DataEntryDetailsFragment.createBundle(
                    permissionType = SLEEP,
                    entryId = "1",
                    showDataOrigin = true,
                )
            )
            .use { onView(withId(R.id.loading)).check(matches(isDisplayed())) }
    }

    @Test
    fun dataEntriesDetailsInit_withData_showsItem() {
        whenever(viewModel.sessionData)
            .thenReturn(
                MutableLiveData(
                    WithData(
                        listOf(
                            SleepSessionEntry(
                                uuid = "1",
                                header = "07:06 • TEST_APP_NAME",
                                headerA11y = "07:06 • TEST_APP_NAME",
                                title = "12 hour sleeping",
                                titleA11y = "12 hour sleeping",
                                dataType = SleepSessionRecord::class,
                                notes = "notes",
                            )
                        )
                    )
                )
            )

        launchFragment<DataEntryDetailsFragment>(
                DataEntryDetailsFragment.createBundle(
                    permissionType = SLEEP,
                    entryId = "1",
                    showDataOrigin = true,
                )
            )
            .use {
                onView(withText("07:06 • TEST_APP_NAME")).check(matches(isDisplayed()))
                onView(withText("12 hour sleeping")).check(matches(isDisplayed()))
                onView(withText("notes")).check(matches(isDisplayed()))

                verify(healthConnectLogger, atLeast(1)).setPageId(PageName.ENTRY_DETAILS_PAGE)
                verify(healthConnectLogger).logPageImpression()
            }
    }

    @Test
    fun dataEntriesDetailsInit_withDetails_showsItem_showsDetails() {
        val list = buildList {
            add(getFormattedSleepSession())
            addAll(getSleepStages())
        }
        whenever(viewModel.sessionData).thenReturn(MutableLiveData(WithData(list)))

        launchFragment<DataEntryDetailsFragment>(
                DataEntryDetailsFragment.createBundle(
                    permissionType = SLEEP,
                    entryId = "1",
                    showDataOrigin = true,
                )
            )
            .use {
                onView(withText("12 hour sleeping")).check(matches(isDisplayed()))
                onView(withText("6 hour light sleeping")).check(matches(isDisplayed()))
                onView(withText("6 hour deep sleeping")).check(matches(isDisplayed()))
            }
    }

    @Test
    fun dataEntriesDetailsInit_withHeartRate_showsItem_showsDetails() {
        val list = buildList { add(getFormattedSeriesData()) }

        whenever(viewModel.sessionData).thenReturn(MutableLiveData(WithData(list)))

        launchFragment<DataEntryDetailsFragment>(
                DataEntryDetailsFragment.createBundle(
                    permissionType = HEART_RATE,
                    entryId = "1",
                    showDataOrigin = true,
                )
            )
            .use {
                onView(withText("07:06 - 8:06 • TEST_APP_NAME")).check(matches(isDisplayed()))
                onView(withText("100 bpm")).check(matches(isDisplayed()))
            }
    }

    @Test
    fun dataEntriesInit_withSkinTemperature_showsItem_showsDetails() {
        val list = buildList {
            add(getSkinTemperatureEntry())
            addAll(getSkinTemperatureDeltas(includeBaseline = true, includeLocation = true))
        }
        whenever(viewModel.sessionData).thenReturn(MutableLiveData(WithData(list)))

        launchFragment<DataEntryDetailsFragment>(
                DataEntryDetailsFragment.createBundle(
                    permissionType = SKIN_TEMPERATURE,
                    entryId = "1",
                    showDataOrigin = true,
                )
            )
            .use {
                onView(withText("+0.5℃ (avg variation)")).check(matches(isDisplayed()))
                onView(withText("Measurement location")).check(matches(isDisplayed()))
                onView(withText("Toe")).check(matches(isDisplayed()))
                onView(withText("Baseline")).check(matches(isDisplayed()))
                onView(withText("25℃")).check(matches(isDisplayed()))
                onView(withText("Variation from baseline")).check(matches(isDisplayed()))
                verify(healthConnectLogger, times(2))
                    .logImpression((EntryDetailsElement.REVERSE_SESSION_DETAIL_ENTRY_VIEW))
                verify(healthConnectLogger)
                    .logImpression((EntryDetailsElement.FORMATTED_SECTION_TITLE_VIEW))
            }
    }

    @Test
    fun dataEntriesInit_withSkinTemperature_locationUnknown_hidesLocationDetail() {
        val list = buildList {
            add(getSkinTemperatureEntry())
            addAll(getSkinTemperatureDeltas(includeBaseline = true, includeLocation = false))
        }
        whenever(viewModel.sessionData).thenReturn(MutableLiveData(WithData(list)))

        launchFragment<DataEntryDetailsFragment>(
                DataEntryDetailsFragment.createBundle(
                    permissionType = SKIN_TEMPERATURE,
                    entryId = "1",
                    showDataOrigin = true,
                )
            )
            .use {
                onView(withText("+0.5℃ (avg variation)")).check(matches(isDisplayed()))
                onView(withText("Measurement location")).check(doesNotExist())
                onView(withText("Toe")).check(doesNotExist())
                onView(withText("Baseline")).check(matches(isDisplayed()))
                onView(withText("25℃")).check(matches(isDisplayed()))
                onView(withText("Variation from baseline")).check(matches(isDisplayed()))
                verify(healthConnectLogger, atMost(1))
                    .logImpression((EntryDetailsElement.REVERSE_SESSION_DETAIL_ENTRY_VIEW))
                verify(healthConnectLogger)
                    .logImpression((EntryDetailsElement.FORMATTED_SECTION_TITLE_VIEW))
            }
    }

    @Test
    fun dataEntriesInit_withSkinTemperature_baselineUnknown_hidesBaselineDetail() {
        val list = buildList {
            add(getSkinTemperatureEntry())
            addAll(getSkinTemperatureDeltas(includeBaseline = false, includeLocation = true))
        }
        whenever(viewModel.sessionData).thenReturn(MutableLiveData(WithData(list)))

        launchFragment<DataEntryDetailsFragment>(
                DataEntryDetailsFragment.createBundle(
                    permissionType = SKIN_TEMPERATURE,
                    entryId = "1",
                    showDataOrigin = true,
                )
            )
            .use {
                onView(withText("+0.5℃ (avg variation)")).check(matches(isDisplayed()))
                onView(withText("Measurement location")).check(matches(isDisplayed()))
                onView(withText("Toe")).check(matches(isDisplayed()))
                onView(withText("Baseline")).check(doesNotExist())
                onView(withText("25℃")).check(doesNotExist())
                onView(withText("Variation from baseline")).check(matches(isDisplayed()))
                verify(healthConnectLogger, atMost(1))
                    .logImpression((EntryDetailsElement.REVERSE_SESSION_DETAIL_ENTRY_VIEW))
                verify(healthConnectLogger)
                    .logImpression((EntryDetailsElement.FORMATTED_SECTION_TITLE_VIEW))
            }
    }

    @Test
    fun dataEntriesDetailsInit_withSegmentDetails_showsItem_showsDetails() {
        val list = buildList {
            add(getFormattedExerciseSession(showSession = false))
            add(getFormattedExerciseSegment())
        }
        whenever(viewModel.sessionData).thenReturn(MutableLiveData(WithData(list)))

        launchFragment<DataEntryDetailsFragment>(
                DataEntryDetailsFragment.createBundle(
                    permissionType = EXERCISE,
                    entryId = "1",
                    showDataOrigin = true,
                )
            )
            .use {
                onView(withText("• Set 0")).check(matches(isDisplayed()))
                onView(withText("• 5 kg")).check(matches(isDisplayed()))
                onView(withText("• RPE: 4")).check(matches(isDisplayed()))
            }
    }

    @Test
    fun dataEntriesDetailsInit_withRouteDetails_showsMapView() {
        val list = buildList { add(getFormattedExerciseSession(showSession = true)) }
        whenever(viewModel.sessionData).thenReturn(MutableLiveData(WithData(list)))
        launchFragment<DataEntryDetailsFragment>(
                DataEntryDetailsFragment.createBundle(
                    permissionType = EXERCISE,
                    entryId = "1",
                    showDataOrigin = true,
                )
            )
            .use {
                onView(withText("12 hour running")).check(matches(isDisplayed()))
                onView(withId(R.id.map_view)).check(matches(isDisplayed()))
            }
    }

    @Test
    fun dataEntriesDetailsInit_noRouteDetails_hidesMapView() {
        val list = buildList { add(getFormattedExerciseSession(showSession = false)) }
        whenever(viewModel.sessionData).thenReturn(MutableLiveData(WithData(list)))
        launchFragment<DataEntryDetailsFragment>(
                DataEntryDetailsFragment.createBundle(
                    permissionType = EXERCISE,
                    entryId = "1",
                    showDataOrigin = true,
                )
            )
            .use {
                onView(withText("12 hour running")).check(matches(isDisplayed()))
                onView(withId(R.id.map_view)).check(matches(not(isDisplayed())))
            }
    }

    @Test
    fun dataEntriesDetailsInit_withPlannedExerciseSession_showsItem_showsDetails() {
        val list = buildList {
            add(
                PlannedExerciseSessionEntry(
                    uuid = "test_id",
                    header = "07:06 - 08:06 • Health Connect test app",
                    headerA11y = "from 07:06 to 08:06 • Health Connect test app",
                    title = "Running • Morning Run",
                    titleA11y = "Running • Morning Run",
                    dataType = PlannedExerciseSessionRecord::class,
                    notes = "Morning quick run by the park",
                )
            )
            add(
                PlannedExerciseBlockEntry(
                    block =
                        getPlannedExerciseBlock(
                            1,
                            "Warm up",
                            listOf(
                                getPlannedExerciseStep(
                                    exerciseSegmentType =
                                        ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_RUNNING,
                                    completionGoal =
                                        ExerciseCompletionGoal.DistanceGoal(
                                            Length.fromMeters(1000.0)
                                        ),
                                    performanceGoals =
                                        listOf(
                                            ExercisePerformanceGoal.HeartRateGoal(100, 150),
                                            ExercisePerformanceGoal.SpeedGoal(
                                                Velocity.fromMetersPerSecond(25.0),
                                                Velocity.fromMetersPerSecond(15.0),
                                            ),
                                        ),
                                )
                            ),
                        ),
                    title = "Warm up: 1 time",
                    titleA11y = "Warm up 1 time",
                )
            )
            add(SessionHeader("Notes"))
            add(PlannedExerciseFormattedSectionContent("Morning quick run by the park"))
            add(
                PlannedExerciseStepEntry(
                    step =
                        getPlannedExerciseStepBuilder()
                            .setPerformanceGoals(
                                listOf(
                                    ExercisePerformanceGoal.HeartRateGoal(150, 180),
                                    ExercisePerformanceGoal.SpeedGoal(
                                        Velocity.fromMetersPerSecond(25.0),
                                        Velocity.fromMetersPerSecond(15.0),
                                    ),
                                )
                            )
                            .build(),
                    title = "4 km Running",
                    titleA11y = "4 kilometres Running",
                )
            )
            add(
                PlannedExerciseFormattedSectionContent(
                    title = "This is a test exercise step",
                    bulleted = true,
                )
            )
            add(
                ExercisePerformanceGoalEntry(
                    goal = ExercisePerformanceGoal.HeartRateGoal(150, 180),
                    title = "150 bpm - 180 bpm",
                    titleA11y = "150 beats per minute - 180 beats per minute",
                )
            )
            add(
                ExercisePerformanceGoalEntry(
                    goal =
                        ExercisePerformanceGoal.SpeedGoal(
                            Velocity.fromMetersPerSecond(180.0),
                            Velocity.fromMetersPerSecond(90.0),
                        ),
                    title = "180 km/h - 90 km/h",
                    titleA11y = "180 kilometres per hour - 90 kilometres per hour",
                )
            )
        }

        whenever(viewModel.sessionData).thenReturn(MutableLiveData(WithData(list)))

        launchFragment<DataEntryDetailsFragment>(
                DataEntryDetailsFragment.createBundle(
                    permissionType = PLANNED_EXERCISE,
                    entryId = "1",
                    showDataOrigin = true,
                )
            )
            .use {
                onView(withText("07:06 - 08:06 • Health Connect test app"))
                    .check(matches(isDisplayed()))
                onView(withText("Running • Morning Run")).check(matches(isDisplayed()))
                onView(withText("Notes")).check(matches(isDisplayed()))
                onView(withText("Morning quick run by the park")).check(matches(isDisplayed()))
                onView(withText("Warm up: 1 time")).check(matches(isDisplayed()))
                onView(withText("4 km Running")).check(matches(isDisplayed()))
                onView(withText("• This is a test exercise step")).check(matches(isDisplayed()))
                onView(withText("• 150 bpm - 180 bpm")).check(matches(isDisplayed()))
                onView(withText("• 180 km/h - 90 km/h")).check(matches(isDisplayed()))
                verify(healthConnectLogger, times(2))
                    .logImpression((EntryDetailsElement.FORMATTED_SECTION_CONTENT_VIEW))
                verify(healthConnectLogger)
                    .logImpression((EntryDetailsElement.PLANNED_EXERCISE_BLOCK_ENTRY_VIEW))
                verify(healthConnectLogger)
                    .logImpression((EntryDetailsElement.PLANNED_EXERCISE_STEP_ENTRY_VIEW))
                verify(healthConnectLogger)
                    .logImpression((EntryDetailsElement.SESSION_DETAIL_HEADER_VIEW))
            }
    }

    private fun getSleepStages(): List<FormattedEntry> {
        return listOf(
            FormattedEntry.SessionHeader(header = "Stages"),
            FormattedEntry.FormattedSessionDetail(
                uuid = "1",
                header = "07:06 • TEST_APP_NAME",
                headerA11y = "07:06 • TEST_APP_NAME",
                title = "6 hour light sleeping",
                titleA11y = "6 hour light sleeping",
            ),
            FormattedEntry.FormattedSessionDetail(
                uuid = "1",
                header = "07:06 • TEST_APP_NAME",
                headerA11y = "07:06 • TEST_APP_NAME",
                title = "6 hour deep sleeping",
                titleA11y = "6 hour deep sleeping",
            ),
        )
    }

    private fun getSkinTemperatureEntry(): FormattedEntry {
        return SeriesDataEntry(
            uuid = "1",
            header = "16:00 - 17:00 • TEST_APP_NAME",
            headerA11y = "16:00 - 17:00 • TEST_APP_NAME",
            title = "+0.5℃ (avg variation)",
            titleA11y = "+0.5 degrees Celsius (average variation)",
            dataType = SkinTemperatureRecord::class,
        )
    }

    private fun getSkinTemperatureDeltas(
        includeBaseline: Boolean,
        includeLocation: Boolean,
    ): List<FormattedEntry> {
        val locationDefinedFormattedEntry: FormattedEntry.ReverseSessionDetail =
            FormattedEntry.ReverseSessionDetail(
                uuid = "1",
                header = "Measurement location",
                headerA11y = "Measurement location",
                title = "Toe",
                titleA11y = "Toe",
            )

        val baselineDefinedFormattedEntry: FormattedEntry.ReverseSessionDetail =
            FormattedEntry.ReverseSessionDetail(
                uuid = "1",
                header = "Baseline",
                headerA11y = "Baseline",
                title = "25℃",
                titleA11y = "25 degrees Celsius",
            )

        val deltaFormattedEntries: List<FormattedEntry> =
            listOf(
                FormattedEntry.FormattedSectionTitle(title = "Variation from baseline"),
                FormattedEntry.FormattedSessionDetail(
                    uuid = "1",
                    header = "16:10 AM",
                    headerA11y = "16:10 AM",
                    title = "+1.5℃",
                    titleA11y = "+1.5 degrees Celsius",
                ),
                FormattedEntry.FormattedSessionDetail(
                    uuid = "1",
                    header = "16:40 AM",
                    headerA11y = "16:40 AM",
                    title = "-0.5℃",
                    titleA11y = "-0.5 degrees Celsius",
                ),
            )

        return if (includeBaseline && includeLocation) {
            listOf(locationDefinedFormattedEntry, baselineDefinedFormattedEntry)
                .plus(deltaFormattedEntries)
        } else if (includeBaseline) {
            listOf(baselineDefinedFormattedEntry).plus(deltaFormattedEntries)
        } else if (includeLocation) {
            listOf(locationDefinedFormattedEntry).plus(deltaFormattedEntries)
        } else {
            deltaFormattedEntries
        }
    }

    private fun getFormattedSleepSession(): SleepSessionEntry {
        return SleepSessionEntry(
            uuid = "1",
            header = "07:06 • TEST_APP_NAME",
            headerA11y = "07:06 • TEST_APP_NAME",
            title = "12 hour sleeping",
            titleA11y = "12 hour sleeping",
            dataType = SleepSessionRecord::class,
            notes = "notes",
        )
    }

    private fun getFormattedExerciseSession(showSession: Boolean): ExerciseSessionEntry {
        return ExerciseSessionEntry(
            uuid = "1",
            header = "07:06 • TEST_APP_NAME",
            headerA11y = "07:06 • TEST_APP_NAME",
            title = "12 hour running",
            titleA11y = "12 hour running",
            dataType = ExerciseSessionRecord::class,
            notes = "notes",
            route =
                if (showSession) {
                    ExerciseRoute(WARSAW_ROUTE)
                } else {
                    null
                },
        )
    }

    private fun getFormattedExerciseSegment(): FormattedEntry.FormattedSegment {
        return FormattedEntry.FormattedSegment(
            uuid = "1",
            header = "07:06 • TEST_APP_NAME",
            headerA11y = "07:06 • TEST_APP_NAME",
            title = "Stretching",
            titleA11y = "Stretching",
            setIndex = "Set 0",
            setIndexA11y = "Set 0",
            weight = "5 kg",
            weightA11y = "5 kilograms",
            rpe = "RPE: 4",
            rpeA11y = "Rate of perceived exertion: 4",
        )
    }

    private fun getFormattedSeriesData(): SeriesDataEntry {
        return SeriesDataEntry(
            uuid = "1",
            header = "07:06 - 8:06 • TEST_APP_NAME",
            headerA11y = "07:06 - 8:06 • TEST_APP_NAME",
            title = "100 bpm",
            titleA11y = "100 beats per minute",
            dataType = HeartRateRecord::class,
        )
    }

    private fun getPlannedExerciseStepBuilder(): PlannedExerciseStep.Builder {
        return PlannedExerciseStep.Builder(
            ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_RUNNING,
            PlannedExerciseStep.EXERCISE_CATEGORY_ACTIVE,
            ExerciseCompletionGoal.DistanceGoal(Length.fromMeters(1000.0)),
        )
    }
}
