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
package com.android.healthconnect.controller.tests.data.entries

import android.content.Context
import android.health.connect.datatypes.ExerciseSessionRecord
import android.health.connect.datatypes.HeartRateRecord
import android.health.connect.datatypes.PlannedExerciseSessionRecord
import android.health.connect.datatypes.SleepSessionRecord
import android.health.connect.datatypes.StepsRecord
import androidx.core.os.bundleOf
import androidx.lifecycle.MutableLiveData
import androidx.test.espresso.Espresso.onIdle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.data.appdata.AppDataFragment.Companion.PERMISSION_TYPE_NAME_KEY
import com.android.healthconnect.controller.data.entries.AllEntriesFragment
import com.android.healthconnect.controller.data.entries.EntriesViewModel
import com.android.healthconnect.controller.data.entries.EntriesViewModel.EntriesFragmentState.Empty
import com.android.healthconnect.controller.data.entries.EntriesViewModel.EntriesFragmentState.Loading
import com.android.healthconnect.controller.data.entries.EntriesViewModel.EntriesFragmentState.LoadingFailed
import com.android.healthconnect.controller.data.entries.EntriesViewModel.EntriesFragmentState.With
import com.android.healthconnect.controller.data.entries.FormattedEntry
import com.android.healthconnect.controller.data.entries.FormattedEntry.FormattedAggregation
import com.android.healthconnect.controller.data.entries.FormattedEntry.FormattedDataEntry
import com.android.healthconnect.controller.data.entries.datenavigation.DateNavigationPeriod
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType.EXERCISE
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType.HEART_RATE
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType.PLANNED_EXERCISE
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType.SLEEP
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType.STEPS
import com.android.healthconnect.controller.permissions.data.MedicalPermissionType
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.tests.utils.FakeParentFragment
import com.android.healthconnect.controller.tests.utils.NESTED_FRAGMENT_TAG
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.TEST_MEDICAL_RESOURCE_IMMUNIZATION
import com.android.healthconnect.controller.tests.utils.TEST_MEDICAL_RESOURCE_IMMUNIZATION_2
import com.android.healthconnect.controller.tests.utils.TEST_MEDICAL_RESOURCE_IMMUNIZATION_3
import com.android.healthconnect.controller.tests.utils.launchNestedFragment
import com.android.healthconnect.controller.tests.utils.setLocale
import com.android.healthconnect.controller.tests.utils.withIndex
import com.android.healthconnect.controller.utils.logging.DataEntriesElement
import com.android.healthconnect.controller.utils.logging.EntriesElement
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.PageName
import com.android.settingslib.widget.SettingsThemeHelper
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.ZoneId
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AllEntriesFragmentTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @BindValue val viewModel: EntriesViewModel = Mockito.mock(EntriesViewModel::class.java)

    @BindValue val healthConnectLogger: HealthConnectLogger = mock()

    private lateinit var context: Context

    @Before
    fun setup() {
        hiltRule.inject()
        context = InstrumentationRegistry.getInstrumentation().context
        context.setLocale(Locale.UK)
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("UTC")))

        whenever(viewModel.currentSelectedDate).thenReturn(MutableLiveData())
        whenever(viewModel.period).thenReturn(MutableLiveData(DateNavigationPeriod.PERIOD_DAY))
        whenever(viewModel.appInfo)
            .thenReturn(
                MutableLiveData(
                    AppMetadata(
                        TEST_APP_PACKAGE_NAME,
                        TEST_APP_NAME,
                        context.getDrawable(R.drawable.health_connect_logo),
                    )
                )
            )
        whenever(viewModel.screenState)
            .thenReturn(MutableLiveData(EntriesViewModel.EntriesDeletionScreenState.VIEW))
        whenever(viewModel.mapOfEntriesToBeDeleted).thenReturn(MutableLiveData())
        whenever(viewModel.allEntriesSelected).thenReturn(MutableLiveData(false))
    }

    @After
    fun tearDown() {
        reset(healthConnectLogger)
    }

    @Test
    fun showsDateNavigationPreference() {
        whenever(viewModel.entries).thenReturn(MutableLiveData(With(FORMATTED_STEPS_LIST)))
        whenever(viewModel.getEntriesList()).thenReturn((FORMATTED_STEPS_LIST.toMutableList()))

        launchNestedFragment<AllEntriesFragment>(bundleOf(PERMISSION_TYPE_NAME_KEY to STEPS.name))

        onView(withId(R.id.date_picker_spinner)).check(matches(isDisplayed()))
        verify(healthConnectLogger, atLeast(1)).logImpression(EntriesElement.DATE_VIEW_SPINNER_DAY)
        verify(healthConnectLogger).logImpression(DataEntriesElement.PREVIOUS_DAY_BUTTON)
        verify(healthConnectLogger).logImpression(DataEntriesElement.NEXT_DAY_BUTTON)
    }

    @Test
    fun fitnessData_logsFitnessPageImpression() {
        whenever(viewModel.entries).thenReturn(MutableLiveData(Empty))

        launchNestedFragment<AllEntriesFragment>(bundleOf(PERMISSION_TYPE_NAME_KEY to STEPS.name))

        verify(healthConnectLogger, atLeast(1)).setPageId(PageName.TAB_ENTRIES_PAGE)
        verify(healthConnectLogger).logPageImpression()
    }

    @Test
    fun medicalData_logsMedicalPageImpression() {
        whenever(viewModel.entries).thenReturn(MutableLiveData(Empty))

        launchNestedFragment<AllEntriesFragment>(
            bundleOf(PERMISSION_TYPE_NAME_KEY to MedicalPermissionType.VACCINES.name)
        )

        verify(healthConnectLogger, atLeast(1)).setPageId(PageName.TAB_MEDICAL_ENTRIES_PAGE)
        verify(healthConnectLogger).logPageImpression()
    }

    @Test
    fun whenNoData_showsNoData() {
        whenever(viewModel.entries).thenReturn(MutableLiveData(Empty))

        launchNestedFragment<AllEntriesFragment>(bundleOf(PERMISSION_TYPE_NAME_KEY to STEPS.name))

        if(SettingsThemeHelper.isExpressiveTheme(context)) {
            onView(withId(R.id.zerostate_view)).check(matches(isDisplayed()))
        } else {
            onView(withId(R.id.no_data_view)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun whenError_showsErrorView() {
        whenever(viewModel.entries).thenReturn(MutableLiveData(LoadingFailed))

        launchNestedFragment<AllEntriesFragment>(bundleOf(PERMISSION_TYPE_NAME_KEY to STEPS.name))

        onView(withId(R.id.error_view)).check(matches(isDisplayed()))
    }

    @Test
    fun whenLoading_showsLoading() {
        whenever(viewModel.entries).thenReturn(MutableLiveData(Loading))

        launchNestedFragment<AllEntriesFragment>(bundleOf(PERMISSION_TYPE_NAME_KEY to STEPS.name))

        onView(withId(R.id.loading)).check(matches(isDisplayed()))
    }

    @Test
    fun withSleepData_showsListOfEntries() {
        whenever(viewModel.entries).thenReturn(MutableLiveData(With(FORMATTED_SLEEP_LIST)))
        whenever(viewModel.getEntriesList()).thenReturn(FORMATTED_SLEEP_LIST.toMutableList())

        launchNestedFragment<AllEntriesFragment>(bundleOf(PERMISSION_TYPE_NAME_KEY to SLEEP.name))

        onView(withText("7:06 - 7:06")).check(matches(isDisplayed()))
        onView(withText("7 hours")).check(matches(isDisplayed()))

        verify(healthConnectLogger).logImpression(EntriesElement.ENTRY_BUTTON_NO_CHECKBOX)
    }

    @Test
    fun withHeartRateData_showsListOfEntries() {
        whenever(viewModel.entries).thenReturn(MutableLiveData(With(FORMATTED_HEART_RATE_LIST)))
        whenever(viewModel.getEntriesList()).thenReturn(FORMATTED_HEART_RATE_LIST.toMutableList())

        launchNestedFragment<AllEntriesFragment>(
            bundleOf(PERMISSION_TYPE_NAME_KEY to HEART_RATE.name)
        )

        onView(withText("7:06 - 7:06")).check(matches(isDisplayed()))
        onView(withText("128 - 140 bpm")).check(matches(isDisplayed()))
        verify(healthConnectLogger).logImpression(EntriesElement.ENTRY_BUTTON_NO_CHECKBOX)
    }

    @Test
    fun withExerciseData_showsListOfEntries() {
        whenever(viewModel.entries)
            .thenReturn(MutableLiveData(With(FORMATTED_EXERCISE_SESSION_LIST)))
        whenever(viewModel.getEntriesList())
            .thenReturn(FORMATTED_EXERCISE_SESSION_LIST.toMutableList())

        launchNestedFragment<AllEntriesFragment>(
            bundleOf(PERMISSION_TYPE_NAME_KEY to EXERCISE.name)
        )

        onView(withText("7:06 - 7:06")).check(matches(isDisplayed()))
        onView(withText("Biking")).check(matches(isDisplayed()))
        verify(healthConnectLogger).logImpression(EntriesElement.ENTRY_BUTTON_NO_CHECKBOX)
    }

    @Test
    fun withPlannedExerciseData_showsListOfEntries() {
        whenever(viewModel.entries)
            .thenReturn(MutableLiveData(With(FORMATTED_PLANNED_EXERCISE_LIST)))
        whenever(viewModel.getEntriesList())
            .thenReturn(FORMATTED_PLANNED_EXERCISE_LIST.toMutableList())

        launchNestedFragment<AllEntriesFragment>(
            bundleOf(PERMISSION_TYPE_NAME_KEY to PLANNED_EXERCISE.name)
        )

        onView(withText("7:06 - 7:06")).check(matches(isDisplayed()))
        onView(withText("Workout")).check(matches(isDisplayed()))
        verify(healthConnectLogger).logImpression(EntriesElement.ENTRY_BUTTON_NO_CHECKBOX)
    }

    @Test
    fun withStepsData_showsListOfEntries() {
        whenever(viewModel.entries).thenReturn(MutableLiveData(With(FORMATTED_STEPS_LIST)))
        whenever(viewModel.getEntriesList())
            .thenReturn(FORMATTED_PLANNED_EXERCISE_LIST.toMutableList())

        launchNestedFragment<AllEntriesFragment>(bundleOf(PERMISSION_TYPE_NAME_KEY to STEPS.name))

        onView(withText("7:06 - 7:06")).check(matches(isDisplayed()))
        onView(withText("12 steps")).check(matches(isDisplayed()))
        onView(withText("8:06 - 8:06")).check(matches(isDisplayed()))
        onView(withText("15 steps")).check(matches(isDisplayed()))
        verify(healthConnectLogger, times(2)).logImpression(EntriesElement.ENTRY_BUTTON_NO_CHECKBOX)
    }

    @Test
    fun showsData_onOrientationChange() {
        whenever(viewModel.entries).thenReturn(MutableLiveData(With(FORMATTED_STEPS_LIST)))
        whenever(viewModel.getEntriesList()).thenReturn(FORMATTED_STEPS_LIST.toMutableList())

        val scenario =
            launchNestedFragment<AllEntriesFragment>(
                bundleOf(PERMISSION_TYPE_NAME_KEY to STEPS.name)
            )

        onView(withText("7:06 - 7:06")).check(matches(isDisplayed()))
        onView(withText("12 steps")).check(matches(isDisplayed()))
        onView(withText("8:06 - 8:06")).check(matches(isDisplayed()))
        onView(withText("15 steps")).check(matches(isDisplayed()))

        scenario.recreate()

        onIdle()
        onView(withText("7:06 - 7:06")).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withText("12 steps")).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withText("8:06 - 8:06")).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withText("15 steps")).perform(scrollTo()).check(matches(isDisplayed()))
    }

    @Test
    fun whenNoMedicalData_showsNoData() {
        whenever(viewModel.entries).thenReturn(MutableLiveData(Empty))

        launchNestedFragment<AllEntriesFragment>(
            bundleOf(PERMISSION_TYPE_NAME_KEY to MedicalPermissionType.VACCINES.name)
        )

        if(SettingsThemeHelper.isExpressiveTheme(context)) {
            onView(withId(R.id.zerostate_view)).check(matches(isDisplayed()))
        } else {
            onView(withId(R.id.no_data_view)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun whenMedicalError_showsErrorView() {
        whenever(viewModel.entries).thenReturn(MutableLiveData(LoadingFailed))

        launchNestedFragment<AllEntriesFragment>(
            bundleOf(PERMISSION_TYPE_NAME_KEY to MedicalPermissionType.VACCINES.name)
        )

        onView(withId(R.id.error_view)).check(matches(isDisplayed()))
    }

    @Test
    fun whenMedicalLoading_showsLoading() {
        whenever(viewModel.entries).thenReturn(MutableLiveData(Loading))

        launchNestedFragment<AllEntriesFragment>(
            bundleOf(PERMISSION_TYPE_NAME_KEY to MedicalPermissionType.VACCINES.name)
        )

        onView(withId(R.id.loading)).check(matches(isDisplayed()))
    }

    @Test
    fun withMedicalData_showsListOfEntries() {
        whenever(viewModel.entries).thenReturn(MutableLiveData(With(FORMATTED_IMMUNIZATION_LIST)))
        whenever(viewModel.getEntriesList()).thenReturn(FORMATTED_IMMUNIZATION_LIST.toMutableList())

        launchNestedFragment<AllEntriesFragment>(
            bundleOf(PERMISSION_TYPE_NAME_KEY to MedicalPermissionType.VACCINES.name)
        )

        onView(withText("02 May 2023 • Health Connect Toolbox")).check(matches(isDisplayed()))
        onView(withText("12 Aug 2022 • Health Connect Toolbox")).check(matches(isDisplayed()))
        onView(withText("25 Sep 2021 • Health Connect Toolbox")).check(matches(isDisplayed()))
        verify(healthConnectLogger, times(3)).logImpression(EntriesElement.ENTRY_BUTTON_NO_CHECKBOX)
    }

    @Test
    fun triggerDeletion_showsCheckboxes() {
        whenever(viewModel.entries).thenReturn(MutableLiveData(With(FORMATTED_STEPS_LIST)))
        whenever(viewModel.getEntriesList()).thenReturn(FORMATTED_STEPS_LIST.toMutableList())

        val scenario =
            launchNestedFragment<AllEntriesFragment>(
                bundleOf(PERMISSION_TYPE_NAME_KEY to STEPS.name)
            )

        scenario.onActivity { activity ->
            val parentFragment =
                activity.supportFragmentManager.findFragmentByTag("") as FakeParentFragment
            val fragment =
                parentFragment.childFragmentManager.findFragmentByTag(NESTED_FRAGMENT_TAG)
            (fragment as AllEntriesFragment).triggerDeletionState(
                EntriesViewModel.EntriesDeletionScreenState.DELETE
            )
        }

        onView(withIndex(withId(R.id.item_checkbox_button), 1)).check(matches(isDisplayed()))
        verify(healthConnectLogger).logImpression(EntriesElement.SELECT_ALL_BUTTON)
        verify(healthConnectLogger, times(2))
            .logImpression(EntriesElement.ENTRY_BUTTON_WITH_CHECKBOX)
    }

    @Test
    fun inDeletion_checkboxesRemainOnOrientationChange() = runTest {
        whenever(viewModel.entries).thenReturn(MutableLiveData(With(FORMATTED_STEPS_LIST)))
        whenever(viewModel.screenState)
            .thenReturn(MutableLiveData(EntriesViewModel.EntriesDeletionScreenState.DELETE))
        whenever(viewModel.getEntriesList()).thenReturn(FORMATTED_STEPS_LIST.toMutableList())

        val scenario =
            launchNestedFragment<AllEntriesFragment>(
                bundleOf(PERMISSION_TYPE_NAME_KEY to STEPS.name)
            )

        scenario.onActivity { activity ->
            val parentFragment =
                activity.supportFragmentManager.findFragmentByTag("") as FakeParentFragment
            val fragment =
                parentFragment.childFragmentManager.findFragmentByTag(NESTED_FRAGMENT_TAG)
            (fragment as AllEntriesFragment).triggerDeletionState(
                EntriesViewModel.EntriesDeletionScreenState.DELETE
            )
        }

        onView(withIndex(withId(R.id.item_checkbox_button), 1)).check(matches(isDisplayed()))

        scenario.recreate()

        onIdle()
        onView(withIndex(withId(R.id.item_checkbox_button), 0)).check(matches(isDisplayed()))
    }

    @Test
    fun inDeletion_checkedItemsAddedToDeleteSet() {
        whenever(viewModel.entries)
            .thenReturn(MutableLiveData(With(FORMATTED_STEPS_LIST_WITH_AGGREGATION)))
        whenever(viewModel.getEntriesList())
            .thenReturn(FORMATTED_STEPS_LIST_WITH_AGGREGATION.toMutableList())

        val scenario =
            launchNestedFragment<AllEntriesFragment>(
                bundleOf(PERMISSION_TYPE_NAME_KEY to STEPS.name)
            )

        scenario.onActivity { activity ->
            val parentFragment =
                activity.supportFragmentManager.findFragmentByTag("") as FakeParentFragment
            val fragment =
                parentFragment.childFragmentManager.findFragmentByTag(NESTED_FRAGMENT_TAG)
            (fragment as AllEntriesFragment).triggerDeletionState(
                EntriesViewModel.EntriesDeletionScreenState.DELETE
            )
        }

        onView(withText("12 steps")).perform(click())
        onIdle()
        verify(viewModel).addToDeleteMap("test_id", StepsRecord::class)
        verify(healthConnectLogger).logInteraction(EntriesElement.ENTRY_BUTTON_WITH_CHECKBOX)
    }

    @Test
    fun triggerDeletion_displaysSelectAll() {
        whenever(viewModel.entries)
            .thenReturn(MutableLiveData(With(FORMATTED_STEPS_LIST_WITH_AGGREGATION)))
        whenever(viewModel.getEntriesList())
            .thenReturn(FORMATTED_STEPS_LIST_WITH_AGGREGATION.toMutableList())

        val scenario =
            launchNestedFragment<AllEntriesFragment>(
                bundleOf(PERMISSION_TYPE_NAME_KEY to STEPS.name)
            )

        scenario.onActivity { activity ->
            val parentFragment =
                activity.supportFragmentManager.findFragmentByTag("") as FakeParentFragment
            val fragment =
                parentFragment.childFragmentManager.findFragmentByTag(NESTED_FRAGMENT_TAG)
            (fragment as AllEntriesFragment).triggerDeletionState(
                EntriesViewModel.EntriesDeletionScreenState.DELETE
            )
        }

        onIdle()
        onView(withIndex(withText("Select all"), 0)).check(matches(isDisplayed()))
        verify(healthConnectLogger, times(2))
            .logImpression(EntriesElement.ENTRY_BUTTON_WITH_CHECKBOX)
        verify(healthConnectLogger).logImpression(EntriesElement.SELECT_ALL_BUTTON)
    }

    @Test
    fun inDeletion_selectAllChecked_allEntriesChecked() {
        whenever(viewModel.entries)
            .thenReturn(MutableLiveData(With(FORMATTED_STEPS_LIST_WITH_AGGREGATION)))
        whenever(viewModel.getEntriesList())
            .thenReturn(FORMATTED_STEPS_LIST_WITH_AGGREGATION.toMutableList())

        val scenario =
            launchNestedFragment<AllEntriesFragment>(
                bundleOf(PERMISSION_TYPE_NAME_KEY to STEPS.name)
            )

        scenario.onActivity { activity ->
            val parentFragment =
                activity.supportFragmentManager.findFragmentByTag("") as FakeParentFragment
            val fragment =
                parentFragment.childFragmentManager.findFragmentByTag(NESTED_FRAGMENT_TAG)
            (fragment as AllEntriesFragment).triggerDeletionState(
                EntriesViewModel.EntriesDeletionScreenState.DELETE
            )
        }

        onView(withText("Select all")).perform(click())
        onIdle()
        verify(viewModel).addToDeleteMap("test_id", StepsRecord::class)
        verify(viewModel).addToDeleteMap("test_id_2", StepsRecord::class)
        verify(healthConnectLogger).logImpression(EntriesElement.SELECT_ALL_BUTTON)
    }
}

private val FORMATTED_STEPS_LIST =
    listOf(
        FormattedDataEntry(
            uuid = "test_id",
            header = "7:06 - 7:06",
            headerA11y = "from 7:06 to 7:06",
            title = "12 steps",
            titleA11y = "12 steps",
            dataType = StepsRecord::class,
        ),
        FormattedDataEntry(
            uuid = "test_id_2",
            header = "8:06 - 8:06",
            headerA11y = "from 8:06 to 8:06",
            title = "15 steps",
            titleA11y = "15 steps",
            dataType = StepsRecord::class,
        ),
    )

private val FORMATTED_SLEEP_LIST =
    listOf(
        FormattedEntry.SleepSessionEntry(
            uuid = "test_id",
            header = "7:06 - 7:06",
            headerA11y = "from 7:06 to 7:06",
            title = "7 hours",
            titleA11y = "7 hours",
            dataType = SleepSessionRecord::class,
            notes = "",
        )
    )
private val FORMATTED_HEART_RATE_LIST =
    listOf(
        FormattedEntry.SeriesDataEntry(
            uuid = "test_id",
            header = "7:06 - 7:06",
            headerA11y = "from 7:06 to 7:06",
            title = "128 - 140 bpm",
            titleA11y = "128 - 140 bpm",
            dataType = HeartRateRecord::class,
        )
    )
private val FORMATTED_PLANNED_EXERCISE_LIST =
    listOf(
        FormattedEntry.PlannedExerciseSessionEntry(
            uuid = "test_id",
            header = "7:06 - 7:06",
            headerA11y = "from 7:06 to 7:06",
            title = "Workout",
            titleA11y = "Workout",
            dataType = PlannedExerciseSessionRecord::class,
            notes = "",
        )
    )
private val FORMATTED_EXERCISE_SESSION_LIST =
    listOf(
        FormattedEntry.ExerciseSessionEntry(
            uuid = "test_id",
            header = "7:06 - 7:06",
            headerA11y = "from 7:06 to 7:06",
            title = "Biking",
            titleA11y = "Biking",
            dataType = ExerciseSessionRecord::class,
            notes = "",
        )
    )

private val FORMATTED_IMMUNIZATION_LIST =
    listOf(
        FormattedEntry.FormattedMedicalDataEntry(
            header = "02 May 2023 • Health Connect Toolbox",
            headerA11y = "02 May 2023 • Health Connect Toolbox",
            title = "Covid vaccine",
            titleA11y = "important vaccination",
            medicalResourceId = TEST_MEDICAL_RESOURCE_IMMUNIZATION.id,
        ),
        FormattedEntry.FormattedMedicalDataEntry(
            header = "12 Aug 2022 • Health Connect Toolbox",
            headerA11y = "12 Aug 2022 • Health Connect Toolbox",
            title = "Covid vaccine",
            titleA11y = "important vaccination",
            medicalResourceId = TEST_MEDICAL_RESOURCE_IMMUNIZATION_2.id,
        ),
        FormattedEntry.FormattedMedicalDataEntry(
            header = "25 Sep 2021 • Health Connect Toolbox",
            headerA11y = "25 Sep 2021 • Health Connect Toolbox",
            title = "Covid vaccine",
            titleA11y = "important vaccination",
            medicalResourceId = TEST_MEDICAL_RESOURCE_IMMUNIZATION_3.id,
        ),
    )

private val FORMATTED_STEPS_LIST_WITH_AGGREGATION =
    listOf(
        FormattedAggregation(
            aggregation = "27",
            aggregationA11y = "27",
            contributingApps = TEST_APP_NAME,
        ),
        FormattedDataEntry(
            uuid = "test_id",
            header = "7:06 - 7:06",
            headerA11y = "from 7:06 to 7:06",
            title = "12 steps",
            titleA11y = "12 steps",
            dataType = StepsRecord::class,
        ),
        FormattedDataEntry(
            uuid = "test_id_2",
            header = "8:06 - 8:06",
            headerA11y = "from 8:06 to 8:06",
            title = "15 steps",
            titleA11y = "15 steps",
            dataType = StepsRecord::class,
        ),
    )
