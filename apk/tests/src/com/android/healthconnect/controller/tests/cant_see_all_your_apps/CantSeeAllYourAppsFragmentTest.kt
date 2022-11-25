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

package com.android.healthconnect.controller.tests.cant_see_all_your_apps

import android.os.Bundle
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import com.android.healthconnect.controller.cantseeallyourapps.CantSeeAllYourAppsFragment
import com.android.healthconnect.controller.tests.utils.launchFragment
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class CantSeeAllYourAppsFragmentTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @Test
    fun cantSeeAllYourAppsFragment_isDisplayedCorrectly() {
        launchFragment<CantSeeAllYourAppsFragment>(Bundle())

        Espresso.onView(
                ViewMatchers.withText(
                    "If you can’t see an installed app, it may not be compatible with Health\u00A0Connect yet"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withText("Things to try"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withText("Check for updates"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withText("Make sure installed apps are up-to-date"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withText("See all compatible apps"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withText("Find apps on Google\u00A0Play"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withText("Send feedback"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(
                ViewMatchers.withText(
                    "Tell us which health & fitness apps you’d like to work with Health\u00A0Connect"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun cantSeeAllYourAppsFragment_sendFeedbackButton_isClickable() {
        launchFragment<CantSeeAllYourAppsFragment>(Bundle())

        Espresso.onView(ViewMatchers.withText("Send feedback")).perform(ViewActions.click())
    }
}
