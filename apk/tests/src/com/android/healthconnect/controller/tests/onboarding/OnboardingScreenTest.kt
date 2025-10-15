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
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isClickable
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.onboarding.OnboardingActivity
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.OnboardingElement
import com.android.healthconnect.controller.utils.logging.PageName
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class OnboardingScreenTest {
    @get:Rule val hiltRule = HiltAndroidRule(this)
    @get:Rule val setFlagsRule = SetFlagsRule()

    @BindValue val healthConnectLogger: HealthConnectLogger = mock()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    private fun startOnboardingActivity(): ActivityScenario<OnboardingActivity> {
        val startOnboardingActivityIntent =
            Intent.makeMainActivity(
                    ComponentName(
                        ApplicationProvider.getApplicationContext(),
                        OnboardingActivity::class.java,
                    )
                )
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

        return ActivityScenario.launchActivityForResult(startOnboardingActivityIntent)
    }

    @Test
    fun onboardingScreen_isDisplayedCorrectly() {
        startOnboardingActivity()

        onView(withText("Get started with Health\u00A0Connect")).check(matches(isDisplayed()))
        onView(
                withText(
                    "Health\u00A0Connect stores and syncs your health and fitness data from different apps.\n\nFitness and wellness data, including exercise sessions, steps, nutrition, sleep and more\n\nMedical records, including vaccines, lab results and more"
                )
            )
            .check(matches(isDisplayed()))
        onView(withId(R.id.onboarding_image)).check(matches(isDisplayed()))
        onView(withText("With Health\u00A0Connect you can"))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
        onView(
                withText(
                    "Share data with your apps\n" +
                        "Choose the data each app can read or write to Health\u00A0Connect"
                )
            )
            .perform(scrollTo())
            .check(matches(isDisplayed()))
        onView(
                withText(
                    "Manage your settings and privacy\n" +
                        "Change app permissions and manage your data at any time"
                )
            )
            .perform(scrollTo())
            .check(matches(isDisplayed()))
        onView(withText("Get started")).check(matches(isDisplayed()))
        onView(withText("Go back")).check(matches(isDisplayed()))
    }

    @Test
    fun correctLogging() {
        startOnboardingActivity()

        verify(healthConnectLogger, atLeast(1)).setPageId(PageName.ONBOARDING_PAGE)
        verify(healthConnectLogger).logPageImpression()
        verify(healthConnectLogger).logImpression(OnboardingElement.ONBOARDING_GO_BACK_BUTTON)
        verify(healthConnectLogger).logImpression(OnboardingElement.ONBOARDING_COMPLETED_BUTTON)
        verify(healthConnectLogger).logImpression(OnboardingElement.ONBOARDING_MESSAGE_WITH_PHR)
    }

    @Test
    fun onboardingScreen_actions_isClickable() {
        startOnboardingActivity()
        onView(withText("Go back")).check(matches(isClickable()))
        onView(withText("Get started")).check(matches(isClickable()))
    }
}
