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

package com.android.healthconnect.controller.tests.exportimport

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isClickable
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.exportimport.ExportFrequencyFragment
import com.android.healthconnect.controller.exportimport.api.ExportFrequency
import com.android.healthconnect.controller.exportimport.api.ExportSettingsViewModel
import com.android.healthconnect.controller.tests.utils.checkBoxOf
import com.android.healthconnect.controller.tests.utils.launchFragment
import com.android.healthconnect.controller.tests.utils.toggleAnimation
import com.android.healthconnect.controller.utils.logging.ExportFrequencyElement
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@HiltAndroidTest
class ExportFrequencyFragmentTest {
    @get:Rule val hiltRule = HiltAndroidRule(this)

    @BindValue val exportSettingsViewModel: ExportSettingsViewModel = mock()

    @BindValue val healthConnectLogger: HealthConnectLogger = mock()

    private lateinit var navHostController: TestNavHostController
    private lateinit var context: Context

    @Before
    fun setup() {
        hiltRule.inject()
        context = InstrumentationRegistry.getInstrumentation().context
        navHostController = TestNavHostController(context)
        whenever(exportSettingsViewModel.selectedExportFrequency).then {
            MutableLiveData(ExportFrequency.EXPORT_FREQUENCY_NEVER)
        }
        toggleAnimation(false)
    }

    @After
    fun tearDown() {
        reset(healthConnectLogger)
        reset(exportSettingsViewModel)
        toggleAnimation(true)
    }

    @Test
    fun exportFrequencyFragment_isDisplayedCorrectly() {
        launchFragment<ExportFrequencyFragment>(Bundle())

        onView(withText("Set up scheduled export")).check(matches(isDisplayed()))
        onView(withText("Choose how frequently to export your data")).check(matches(isDisplayed()))

        onView(withText("Daily")).check(matches(isDisplayed()))
        onView(withText("Weekly")).check(matches(isDisplayed()))
        onView(withText("Monthly")).check(matches(isDisplayed()))

        onView(withText("Cancel")).check(matches(isDisplayed()))
        onView(withText("Next")).check(matches(isDisplayed()))
    }

    @Test
    fun exportFrequencyFragment_impressionsLogged() {
        launchFragment<ExportFrequencyFragment>(Bundle())

        verify(healthConnectLogger, atLeast(1)).logPageImpression()
        verify(healthConnectLogger)
            .logImpression(ExportFrequencyElement.EXPORT_FREQUENCY_BACK_BUTTON)
        verify(healthConnectLogger)
            .logImpression(ExportFrequencyElement.EXPORT_FREQUENCY_NEXT_BUTTON)
        verify(healthConnectLogger)
            .logImpression(ExportFrequencyElement.EXPORT_FREQUENCY_DAILY_BUTTON)
        verify(healthConnectLogger)
            .logImpression(ExportFrequencyElement.EXPORT_FREQUENCY_WEEKLY_BUTTON)
        verify(healthConnectLogger)
            .logImpression(ExportFrequencyElement.EXPORT_FREQUENCY_MONTHLY_BUTTON)
    }

    @Test
    fun exportFrequencyFragment_cancelButton_isClickable() {
        launchFragment<ExportFrequencyFragment>(Bundle())

        onView(withId(R.id.secondary_button)).check(matches(isClickable()))
        onView(withId(R.id.secondary_button)).perform(click())

        verify(exportSettingsViewModel)
            .updateSelectedFrequency(ExportFrequency.EXPORT_FREQUENCY_NEVER)
        verify(healthConnectLogger)
            .logInteraction(ExportFrequencyElement.EXPORT_FREQUENCY_BACK_BUTTON)
    }

    @Test
    fun exportFrequencyFragment_clicksNextButton_navigatesToDestinationFragment() {
        launchFragment<ExportFrequencyFragment>(Bundle()) {
            navHostController.setGraph(R.navigation.export_nav_graph)
            navHostController.setCurrentDestination(R.id.exportFrequencyFragment)
            Navigation.setViewNavController(this.requireView(), navHostController)
        }

        onView(withId(R.id.primary_button_full)).check(matches(isClickable()))
        onView(withId(R.id.primary_button_full)).perform(click())

        assertThat(navHostController.currentDestination?.id)
            .isEqualTo(R.id.exportDestinationFragment)
        verify(healthConnectLogger)
            .logInteraction(ExportFrequencyElement.EXPORT_FREQUENCY_NEXT_BUTTON)
    }

    @Test
    fun exportFrequencyFragment_clicksNextButtonWithDailyFrequency_interactionLogged() {
        launchFragment<ExportFrequencyFragment>(Bundle()) {
            navHostController.setGraph(R.navigation.export_nav_graph)
            navHostController.setCurrentDestination(R.id.exportFrequencyFragment)
            Navigation.setViewNavController(this.requireView(), navHostController)
        }

        onView(withText("Daily")).perform(click())
        onView(withId(R.id.primary_button_full)).perform(click())

        verify(exportSettingsViewModel, times(2))
            .updateSelectedFrequency(ExportFrequency.EXPORT_FREQUENCY_DAILY)
        verify(healthConnectLogger)
            .logInteraction(ExportFrequencyElement.EXPORT_FREQUENCY_DAILY_BUTTON)
    }

    @Test
    fun exportFrequencyFragment_clicksNextButtonWithWeeklyFrequency_interactionLogged() {
        launchFragment<ExportFrequencyFragment>(Bundle()) {
            navHostController.setGraph(R.navigation.export_nav_graph)
            navHostController.setCurrentDestination(R.id.exportFrequencyFragment)
            Navigation.setViewNavController(this.requireView(), navHostController)
        }

        onView(withText("Weekly")).perform(click())
        onView(withId(R.id.primary_button_full)).perform(click())

        verify(exportSettingsViewModel)
            .updateSelectedFrequency(ExportFrequency.EXPORT_FREQUENCY_WEEKLY)
        verify(healthConnectLogger)
            .logInteraction(ExportFrequencyElement.EXPORT_FREQUENCY_WEEKLY_BUTTON)
    }

    @Test
    fun exportFrequencyFragment_clicksNextButtonWithMonthlyFrequency_interactionLogged() {
        launchFragment<ExportFrequencyFragment>(Bundle()) {
            navHostController.setGraph(R.navigation.export_nav_graph)
            navHostController.setCurrentDestination(R.id.exportFrequencyFragment)
            Navigation.setViewNavController(this.requireView(), navHostController)
        }

        onView(withText("Monthly")).perform(click())
        onView(withId(R.id.primary_button_full)).perform(click())

        verify(exportSettingsViewModel)
            .updateSelectedFrequency(ExportFrequency.EXPORT_FREQUENCY_MONTHLY)
        verify(healthConnectLogger)
            .logInteraction(ExportFrequencyElement.EXPORT_FREQUENCY_MONTHLY_BUTTON)
    }

    @Test
    fun exportFrequencyFragment_dailyButtonIsCheckedByDefault() {
        launchFragment<ExportFrequencyFragment>(Bundle())

        onView(checkBoxOf("Daily")).check(matches(isChecked()))

        verify(healthConnectLogger, never())
            .logInteraction(ExportFrequencyElement.EXPORT_FREQUENCY_DAILY_BUTTON)
    }

    @Test
    fun exportFrequencyFragment_checksWeeklyButton_updatesSelectedFrequency() {
        launchFragment<ExportFrequencyFragment>(Bundle())

        onView(withText("Weekly")).perform(click())

        verify(exportSettingsViewModel)
            .updateSelectedFrequency(ExportFrequency.EXPORT_FREQUENCY_WEEKLY)
        verify(healthConnectLogger)
            .logInteraction(ExportFrequencyElement.EXPORT_FREQUENCY_WEEKLY_BUTTON)
    }

    @Test
    fun exportFrequencyFragment_checksMonthlyButton_updatesSelectedFrequency() {
        launchFragment<ExportFrequencyFragment>(Bundle())

        onView(withText("Monthly")).perform(click())

        verify(exportSettingsViewModel)
            .updateSelectedFrequency(ExportFrequency.EXPORT_FREQUENCY_MONTHLY)
        verify(healthConnectLogger)
            .logInteraction(ExportFrequencyElement.EXPORT_FREQUENCY_MONTHLY_BUTTON)
    }

    @Test
    fun exportFrequencyFragment_selectedFrequencyIsDaily_dailyButtonChecked() {
        whenever(exportSettingsViewModel.selectedExportFrequency).then {
            MutableLiveData(ExportFrequency.EXPORT_FREQUENCY_DAILY)
        }
        launchFragment<ExportFrequencyFragment>(Bundle())

        onView(checkBoxOf("Daily")).check(matches(isChecked()))
    }

    @Test
    fun exportFrequencyFragment_selectedFrequencyIsWeekly_weeklyButtonChecked() {
        whenever(exportSettingsViewModel.selectedExportFrequency).then {
            MutableLiveData(ExportFrequency.EXPORT_FREQUENCY_WEEKLY)
        }
        launchFragment<ExportFrequencyFragment>(Bundle())

        onView(checkBoxOf("Weekly")).check(matches(isChecked()))
    }

    @Test
    fun exportFrequencyFragment_selectedFrequencyIsMonthly_monthlyButtonChecked() {
        whenever(exportSettingsViewModel.selectedExportFrequency).then {
            MutableLiveData(ExportFrequency.EXPORT_FREQUENCY_MONTHLY)
        }
        launchFragment<ExportFrequencyFragment>(Bundle())

        onView(checkBoxOf("Monthly")).check(matches(isChecked()))
    }
}
