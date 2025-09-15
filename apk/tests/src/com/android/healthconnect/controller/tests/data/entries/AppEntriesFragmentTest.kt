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
import android.content.Intent.EXTRA_PACKAGE_NAME
import android.health.connect.datatypes.ExerciseSessionRecord
import android.health.connect.datatypes.HeartRateRecord
import android.health.connect.datatypes.PlannedExerciseSessionRecord
import android.health.connect.datatypes.SleepSessionRecord
import android.health.connect.datatypes.StepsRecord
import android.health.connect.datatypes.SymptomRecord
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import android.platform.test.flag.junit.SetFlagsRule
import androidx.core.os.bundleOf
import androidx.lifecycle.MutableLiveData
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
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
import com.android.healthconnect.controller.data.entries.AppEntriesFragment
import com.android.healthconnect.controller.data.entries.EntriesViewModel
import com.android.healthconnect.controller.data.entries.EntriesViewModel.EntriesFragmentState.Empty
import com.android.healthconnect.controller.data.entries.EntriesViewModel.EntriesFragmentState.Loading
import com.android.healthconnect.controller.data.entries.EntriesViewModel.EntriesFragmentState.LoadingFailed
import com.android.healthconnect.controller.data.entries.EntriesViewModel.EntriesFragmentState.With
import com.android.healthconnect.controller.data.entries.FormattedEntry
import com.android.healthconnect.controller.data.entries.FormattedEntry.FormattedDataEntry
import com.android.healthconnect.controller.data.entries.datenavigation.DateNavigationPeriod
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType.EXERCISE
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType.HEART_RATE
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType.PLANNED_EXERCISE
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType.SLEEP
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType.STEPS
import com.android.healthconnect.controller.permissions.data.MedicalPermissionType
import com.android.healthconnect.controller.selectabledeletion.DeletionViewModel
import com.android.healthconnect.controller.shared.Constants
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.TEST_MEDICAL_RESOURCE_IMMUNIZATION
import com.android.healthconnect.controller.tests.utils.TEST_MEDICAL_RESOURCE_IMMUNIZATION_2
import com.android.healthconnect.controller.tests.utils.TEST_MEDICAL_RESOURCE_IMMUNIZATION_3
import com.android.healthconnect.controller.tests.utils.launchFragment
import com.android.healthconnect.controller.tests.utils.setLocale
import com.android.healthconnect.controller.tests.utils.withIndex
import com.android.healthconnect.controller.utils.logging.DataEntriesElement
import com.android.healthconnect.controller.utils.logging.EntriesElement
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.PageName
import com.android.healthfitness.flags.Flags
import com.android.settingslib.widget.SettingsThemeHelper
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
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
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AppEntriesFragmentTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)
    @get:Rule val setFlagsRule = SetFlagsRule()
    @get:Rule val checkFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    @BindValue val viewModel: EntriesViewModel = Mockito.mock(EntriesViewModel::class.java)
    @BindValue
    val deletionViewModel: DeletionViewModel = Mockito.mock(DeletionViewModel::class.java)
    @BindValue val healthConnectLogger: HealthConnectLogger = mock()

    private lateinit var context: Context
    private lateinit var navHostController: TestNavHostController

    @Before
    fun setup() {
        hiltRule.inject()
        context = InstrumentationRegistry.getInstrumentation().context
        context.setLocale(Locale.UK)
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("UTC")))
        navHostController = TestNavHostController(context)

        whenever(viewModel.currentSelectedDate).thenReturn(MutableLiveData())
        whenever(viewModel.latestDate).thenReturn(MutableLiveData())
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
        whenever(viewModel.mapOfEntriesToBeDeleted).thenReturn(MutableLiveData())
        whenever(viewModel.screenState)
            .thenReturn(MutableLiveData(EntriesViewModel.EntriesDeletionScreenState.VIEW))
        whenever(viewModel.mapOfEntriesToBeDeleted).thenReturn(MutableLiveData())
        whenever(viewModel.allEntriesSelected).thenReturn(MutableLiveData(false))
        whenever(deletionViewModel.appEntriesReloadNeeded).thenReturn(MutableLiveData(false))
        whenever(deletionViewModel.deletionProgress)
            .thenReturn(MutableLiveData(DeletionViewModel.DeletionProgress.NOT_STARTED))
    }

    @After
    fun tearDown() {
        reset(healthConnectLogger)
    }

    @Test
    fun showsDateNavigationPreference() {
        whenever(viewModel.entries).thenReturn(MutableLiveData(With(emptyList())))

        launchFragment<AppEntriesFragment>(
            bundleOf(
                PERMISSION_TYPE_NAME_KEY to STEPS.name,
                EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                Constants.EXTRA_APP_NAME to TEST_APP_NAME,
            )
        )

        onView(withId(R.id.date_picker_spinner)).check(matches(isDisplayed()))
        verify(healthConnectLogger, atLeast(1)).setPageId(PageName.APP_ENTRIES_PAGE)
        verify(healthConnectLogger).logPageImpression()
        verify(healthConnectLogger, atLeast(1)).logImpression(EntriesElement.DATE_VIEW_SPINNER_DAY)
        verify(healthConnectLogger).logImpression(DataEntriesElement.PREVIOUS_DAY_BUTTON)
        verify(healthConnectLogger).logImpression(DataEntriesElement.NEXT_DAY_BUTTON)
    }

    @Test
    fun whenNoData_showsNoData() {
        whenever(viewModel.entries).thenReturn(MutableLiveData(Empty))

        launchFragment<AppEntriesFragment>(
            bundleOf(
                PERMISSION_TYPE_NAME_KEY to STEPS.name,
                EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                Constants.EXTRA_APP_NAME to TEST_APP_NAME,
            )
        )

        if (SettingsThemeHelper.isExpressiveTheme(context)) {
            onView(withId(R.id.zerostate_view)).check(matches(isDisplayed()))
        } else {
            onView(withId(R.id.no_data_view)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun whenError_showsErrorView() {
        whenever(viewModel.entries).thenReturn(MutableLiveData(LoadingFailed))

        launchFragment<AppEntriesFragment>(
            bundleOf(
                PERMISSION_TYPE_NAME_KEY to STEPS.name,
                EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                Constants.EXTRA_APP_NAME to TEST_APP_NAME,
            )
        )

        onView(withId(R.id.error_view)).check(matches(isDisplayed()))
    }

    @Test
    fun whenLoading_showsLoading() {
        whenever(viewModel.entries).thenReturn(MutableLiveData(Loading))

        launchFragment<AppEntriesFragment>(
            bundleOf(
                PERMISSION_TYPE_NAME_KEY to STEPS.name,
                EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                Constants.EXTRA_APP_NAME to TEST_APP_NAME,
            )
        )

        onView(withId(R.id.loading)).check(matches(isDisplayed()))
    }

    @Test
    fun withSleepData_showsListOfEntries() {
        whenever(viewModel.entries).thenReturn(MutableLiveData(With(FORMATTED_SLEEP_LIST)))
        whenever(viewModel.getEntriesList()).thenReturn(FORMATTED_SLEEP_LIST.toMutableList())

        launchFragment<AppEntriesFragment>(
            bundleOf(
                PERMISSION_TYPE_NAME_KEY to SLEEP.name,
                EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                Constants.EXTRA_APP_NAME to TEST_APP_NAME,
            )
        )

        onView(withText("7:06 - 7:06")).check(matches(isDisplayed()))
        onView(withText("7 hours")).check(matches(isDisplayed()))
        verify(healthConnectLogger).logImpression(EntriesElement.ENTRY_BUTTON_NO_CHECKBOX)
    }

    @Test
    fun withHeartRateData_showsListOfEntries() {
        whenever(viewModel.entries).thenReturn(MutableLiveData(With(FORMATTED_HEART_RATE_LIST)))
        whenever(viewModel.getEntriesList()).thenReturn(FORMATTED_HEART_RATE_LIST.toMutableList())

        launchFragment<AppEntriesFragment>(
            bundleOf(
                PERMISSION_TYPE_NAME_KEY to HEART_RATE.name,
                EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                Constants.EXTRA_APP_NAME to TEST_APP_NAME,
            )
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

        launchFragment<AppEntriesFragment>(
            bundleOf(
                PERMISSION_TYPE_NAME_KEY to EXERCISE.name,
                EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                Constants.EXTRA_APP_NAME to TEST_APP_NAME,
            )
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

        launchFragment<AppEntriesFragment>(
            bundleOf(
                PERMISSION_TYPE_NAME_KEY to PLANNED_EXERCISE.name,
                EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                Constants.EXTRA_APP_NAME to TEST_APP_NAME,
            )
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

        launchFragment<AppEntriesFragment>(
            bundleOf(
                PERMISSION_TYPE_NAME_KEY to STEPS.name,
                EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                Constants.EXTRA_APP_NAME to TEST_APP_NAME,
            )
        )

        onView(withText("7:06 - 7:06")).check(matches(isDisplayed()))
        onView(withText("12 steps")).check(matches(isDisplayed()))
        onView(withText("8:06 - 8:06")).check(matches(isDisplayed()))
        onView(withText("15 steps")).check(matches(isDisplayed()))
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_SYMPTOMS)
    fun withSymptomsData_showsListOfEntries() {
        whenever(viewModel.entries).thenReturn(MutableLiveData(With(FORMATTED_SYMPTOMS_LIST)))
        whenever(viewModel.getEntriesList()).thenReturn(FORMATTED_SYMPTOMS_LIST.toMutableList())

        launchFragment<AppEntriesFragment>(
            bundleOf(
                PERMISSION_TYPE_NAME_KEY to FitnessPermissionType.SYMPTOM_COUGH.name,
                EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                Constants.EXTRA_APP_NAME to TEST_APP_NAME,
            )
        )

        onView(withText("7:06 - 7:06")).check(matches(isDisplayed()))
        onView(withText("Mild cough")).check(matches(isDisplayed()))
        onView(withId(R.id.item_data_entry_notes)).check(matches(isDisplayed()))
        onView(withId(R.id.item_data_entry_notes)).check(matches(withText("Test notes")))
        onView(withId(R.id.item_data_entry_divider)).check(matches(not(isDisplayed())))
    }

    @Test
    fun showsData_onOrientationChange() {
        whenever(viewModel.entries).thenReturn(MutableLiveData(With(FORMATTED_STEPS_LIST)))
        whenever(viewModel.getEntriesList()).thenReturn(FORMATTED_STEPS_LIST.toMutableList())

        val scenario =
            launchFragment<AppEntriesFragment>(
                bundleOf(
                    PERMISSION_TYPE_NAME_KEY to STEPS.name,
                    EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                    Constants.EXTRA_APP_NAME to TEST_APP_NAME,
                )
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
    fun reloadsAppEntries_whenReloadNeeded() {
        whenever(viewModel.entries).thenReturn(MutableLiveData(With(FORMATTED_STEPS_LIST)))
        whenever(viewModel.getEntriesList()).thenReturn(FORMATTED_STEPS_LIST.toMutableList())

        val scenario =
            launchFragment<AppEntriesFragment>(
                bundleOf(
                    PERMISSION_TYPE_NAME_KEY to STEPS.name,
                    EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                    Constants.EXTRA_APP_NAME to TEST_APP_NAME,
                )
            )

        onView(withText("7:06 - 7:06")).check(matches(isDisplayed()))
        onView(withText("12 steps")).check(matches(isDisplayed()))
        onView(withText("8:06 - 8:06")).check(matches(isDisplayed()))
        onView(withText("15 steps")).check(matches(isDisplayed()))

        whenever(deletionViewModel.appEntriesReloadNeeded).thenReturn(MutableLiveData(true))
        scenario.recreate()

        onIdle()
        verify(viewModel, atLeastOnce())
            .loadEntries(
                eq(STEPS),
                eq(TEST_APP_PACKAGE_NAME),
                any(),
                eq(DateNavigationPeriod.PERIOD_DAY),
            )
        verify(viewModel, never())
            .loadEntries(eq(STEPS), any(), eq(DateNavigationPeriod.PERIOD_DAY))
    }

    @Test
    fun inDeletion_showsCheckboxes() {
        whenever(viewModel.entries).thenReturn(MutableLiveData(With(FORMATTED_STEPS_LIST)))
        whenever(viewModel.getEntriesList()).thenReturn(FORMATTED_STEPS_LIST.toMutableList())

        val scenario =
            launchFragment<AppEntriesFragment>(
                bundleOf(
                    PERMISSION_TYPE_NAME_KEY to STEPS.name,
                    EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                    Constants.EXTRA_APP_NAME to TEST_APP_NAME,
                )
            )
        scenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager.findFragmentByTag("")
            (fragment as AppEntriesFragment).triggerDeletionState(
                EntriesViewModel.EntriesDeletionScreenState.DELETE
            )
        }
        onIdle()

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
            launchFragment<AppEntriesFragment>(
                bundleOf(
                    PERMISSION_TYPE_NAME_KEY to STEPS.name,
                    EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                    Constants.EXTRA_APP_NAME to TEST_APP_NAME,
                )
            )
        scenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager.findFragmentByTag("")
            (fragment as AppEntriesFragment).triggerDeletionState(
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
            launchFragment<AppEntriesFragment>(
                bundleOf(
                    PERMISSION_TYPE_NAME_KEY to STEPS.name,
                    EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                    Constants.EXTRA_APP_NAME to TEST_APP_NAME,
                )
            )
        scenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager.findFragmentByTag("")
            (fragment as AppEntriesFragment).triggerDeletionState(
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
            launchFragment<AppEntriesFragment>(
                bundleOf(
                    PERMISSION_TYPE_NAME_KEY to STEPS.name,
                    EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                    Constants.EXTRA_APP_NAME to TEST_APP_NAME,
                )
            )
        scenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager.findFragmentByTag("")
            (fragment as AppEntriesFragment).triggerDeletionState(
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
            launchFragment<AppEntriesFragment>(
                bundleOf(
                    PERMISSION_TYPE_NAME_KEY to STEPS.name,
                    EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                    Constants.EXTRA_APP_NAME to TEST_APP_NAME,
                )
            )
        scenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager.findFragmentByTag("")
            (fragment as AppEntriesFragment).triggerDeletionState(
                EntriesViewModel.EntriesDeletionScreenState.DELETE
            )
        }

        onView(withText("Select all")).perform(click())
        onIdle()
        verify(viewModel).addToDeleteMap("test_id", StepsRecord::class)
        verify(viewModel).addToDeleteMap("test_id_2", StepsRecord::class)
        verify(healthConnectLogger).logImpression(EntriesElement.SELECT_ALL_BUTTON)
    }

    @Test
    fun clickOnFitnessPermission_notClickable() {
        whenever(viewModel.entries).thenReturn(MutableLiveData(With(FORMATTED_STEPS_LIST)))
        whenever(viewModel.getEntriesList())
            .thenReturn(FORMATTED_PLANNED_EXERCISE_LIST.toMutableList())

        launchFragment<AppEntriesFragment>(
            bundleOf(
                PERMISSION_TYPE_NAME_KEY to STEPS.name,
                EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                Constants.EXTRA_APP_NAME to TEST_APP_NAME,
            )
        ) {
            navHostController.setGraph(R.navigation.app_data_nav_graph)
            navHostController.setCurrentDestination(R.id.appEntriesFragment)
            Navigation.setViewNavController(this.requireView(), navHostController)
        }
        onView(withText("12 steps")).perform(click())

        assertThat(navHostController.currentDestination?.id).isEqualTo(R.id.appEntriesFragment)
    }

    @Test
    fun clickOnFitnessPermission_navigateToEntryDetailsFragment() {
        whenever(viewModel.entries).thenReturn(MutableLiveData(With(FORMATTED_SLEEP_LIST)))
        whenever(viewModel.getEntriesList()).thenReturn(FORMATTED_SLEEP_LIST.toMutableList())

        launchFragment<AppEntriesFragment>(
            bundleOf(
                PERMISSION_TYPE_NAME_KEY to SLEEP.name,
                EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                Constants.EXTRA_APP_NAME to TEST_APP_NAME,
            )
        ) {
            navHostController.setGraph(R.navigation.app_data_nav_graph)
            navHostController.setCurrentDestination(R.id.appEntriesFragment)
            Navigation.setViewNavController(this.requireView(), navHostController)
        }
        onView(withText("7 hours")).perform(click())

        assertThat(navHostController.currentDestination?.id)
            .isEqualTo(R.id.dataEntryDetailsFragment)
    }

    @Test
    fun withMedicalData_showsListOfEntries() {
        whenever(viewModel.entries).thenReturn(MutableLiveData(With(FORMATTED_IMMUNIZATION_LIST)))
        whenever(viewModel.getEntriesList()).thenReturn(FORMATTED_IMMUNIZATION_LIST.toMutableList())

        launchFragment<AppEntriesFragment>(
            bundleOf(
                PERMISSION_TYPE_NAME_KEY to MedicalPermissionType.VACCINES.name,
                EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                Constants.EXTRA_APP_NAME to TEST_APP_NAME,
            )
        )

        onView(withText("Covid vaccine 1")).check(matches(isDisplayed()))
        onView(withText("Covid vaccine 2")).check(matches(isDisplayed()))
        onView(withText("Covid vaccine 3")).check(matches(isDisplayed()))
        verify(healthConnectLogger, times(3)).logImpression(EntriesElement.ENTRY_BUTTON_NO_CHECKBOX)
    }

    @Test
    @DisableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD_ENTRIES_SCREEN)
    fun clickOnMedicalPermission_navigateToRawFhir() {
        whenever(viewModel.entries).thenReturn(MutableLiveData(With(FORMATTED_IMMUNIZATION_LIST)))
        whenever(viewModel.getEntriesList()).thenReturn(FORMATTED_IMMUNIZATION_LIST.toMutableList())

        launchFragment<AppEntriesFragment>(
            bundleOf(
                PERMISSION_TYPE_NAME_KEY to MedicalPermissionType.VACCINES.name,
                EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                Constants.EXTRA_APP_NAME to TEST_APP_NAME,
            )
        ) {
            navHostController.setGraph(R.navigation.app_data_nav_graph)
            navHostController.setCurrentDestination(R.id.appEntriesFragment)
            Navigation.setViewNavController(this.requireView(), navHostController)
        }
        onView(withText("Covid vaccine 2")).perform(click())

        assertThat(navHostController.currentDestination?.id).isEqualTo(R.id.rawFhirFragment)
    }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD_ENTRIES_SCREEN)
    fun clickOnMedicalPermission_navigateToPrettyFhir() {
        whenever(viewModel.entries).thenReturn(MutableLiveData(With(FORMATTED_IMMUNIZATION_LIST)))
        whenever(viewModel.getEntriesList()).thenReturn(FORMATTED_IMMUNIZATION_LIST.toMutableList())

        launchFragment<AppEntriesFragment>(
            bundleOf(
                PERMISSION_TYPE_NAME_KEY to MedicalPermissionType.VACCINES.name,
                EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                Constants.EXTRA_APP_NAME to TEST_APP_NAME,
            )
        ) {
            navHostController.setGraph(R.navigation.app_data_nav_graph)
            navHostController.setCurrentDestination(R.id.appEntriesFragment)
            Navigation.setViewNavController(this.requireView(), navHostController)
        }
        onView(withText("Covid vaccine 2")).perform(click())

        assertThat(navHostController.currentDestination?.id).isEqualTo(R.id.prettyFhirFragment)
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

private val FORMATTED_SYMPTOMS_LIST =
    listOf(
        FormattedEntry.SymptomEntry(
            uuid = "test_id",
            header = "7:06 - 7:06",
            headerA11y = "from 7:06 to 7:06",
            title = "Mild cough",
            titleA11y = "Mild cough",
            dataType = SymptomRecord::class,
            notes = "Test notes",
        )
    )

private val FORMATTED_IMMUNIZATION_LIST =
    listOf(
        FormattedEntry.FormattedMedicalDataEntry(
            header = "02 May 2023 • Health Connect Toolbox",
            headerA11y = "My Hospital",
            title = "Covid vaccine 1",
            titleA11y = "important vaccination",
            medicalResourceId = TEST_MEDICAL_RESOURCE_IMMUNIZATION.id,
        ),
        FormattedEntry.FormattedMedicalDataEntry(
            header = "My Hospital",
            headerA11y = "My Hospital",
            title = "Covid vaccine 2",
            titleA11y = "important vaccination",
            medicalResourceId = TEST_MEDICAL_RESOURCE_IMMUNIZATION_2.id,
        ),
        FormattedEntry.FormattedMedicalDataEntry(
            header = "My Hospital 2",
            headerA11y = "My Hospital 2",
            title = "Covid vaccine 3",
            titleA11y = "important vaccination",
            medicalResourceId = TEST_MEDICAL_RESOURCE_IMMUNIZATION_3.id,
        ),
    )

private val FORMATTED_STEPS_LIST_WITH_AGGREGATION =
    listOf(
        FormattedEntry.FormattedAggregation(
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
