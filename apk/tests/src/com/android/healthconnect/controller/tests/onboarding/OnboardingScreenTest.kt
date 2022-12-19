/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.healthconnect.controller.tests.onboarding

import android.content.ComponentName
import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.MainActivity
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.onboarding.OnboardingActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import junit.framework.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class OnboardingScreenTest {
    @get:Rule val hiltRule = HiltAndroidRule(this)

    @Before
    fun setup() {
        hiltRule.inject()
    }

    private fun startOnboardingActivity() {
        val startOnboardingActivityIntent =
            Intent.makeMainActivity(
                    ComponentName(
                        InstrumentationRegistry.getInstrumentation().getContext(),
                        OnboardingActivity::class.java))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

        InstrumentationRegistry.getInstrumentation()
            .getContext()
            .startActivity(startOnboardingActivityIntent)
    }

    @Test
    fun onboardingScreen_isDisplayedCorrectly() {
        startOnboardingActivity()

        Espresso.onView(ViewMatchers.withText("Get Started with Health\u00A0Connect"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(
                ViewMatchers.withText(
                    "Health\u00A0Connect stores your health and fitness data, giving you a simple way to sync the different apps on your phone"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withId(R.id.onboarding_image))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withId(R.id.share_icon))
            .perform(ViewActions.scrollTo())
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withText("Share data with your apps"))
            .perform(ViewActions.scrollTo())
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(
                ViewMatchers.withText(
                    "Choose the data each app can read or write to Health\u00A0Connect"))
            .perform(ViewActions.scrollTo())
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withId(R.id.manage_icon))
            .perform(ViewActions.scrollTo())
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withText("Manage your settings and privacy"))
            .perform(ViewActions.scrollTo())
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(
                ViewMatchers.withText("Change app permissions and manage your data at any time"))
            .perform(ViewActions.scrollTo())
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withText("Get started"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withText("Go back"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun onboardingScreen_goBackButton_isClickable() {
        ActivityScenario.launch(MainActivity::class.java).use { activityScenario ->
            startOnboardingActivity()
            Espresso.onView(ViewMatchers.withId(R.id.go_back_button)).perform(ViewActions.click())
            assertTrue(activityScenario.state == Lifecycle.State.DESTROYED)
        }
    }

    @Test
    fun onboardingScreen_getStartedButton_isClickable() {
        startOnboardingActivity()

        Espresso.onView(ViewMatchers.withId(R.id.get_started_button)).perform(ViewActions.click())
    }
}
