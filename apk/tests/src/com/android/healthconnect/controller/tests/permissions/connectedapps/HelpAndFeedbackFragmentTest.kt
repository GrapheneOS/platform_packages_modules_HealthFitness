/**
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.tests.permissions.connectedapps

import android.os.Bundle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.android.healthconnect.controller.permissions.shared.HelpAndFeedbackFragment
import com.android.healthconnect.controller.tests.utils.launchFragment
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class HelpAndFeedbackFragmentTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @Test
    fun helpAndFeedbackFragment_isDisplayedCorrectly() {
        launchFragment<HelpAndFeedbackFragment>(Bundle())

        onView(
                withText(
                    "If you can't see an installed app, it may not be compatible with Health\u00A0Connect yet"))
            .check(matches(isDisplayed()))
        onView(withText("Things to try")).check(matches(isDisplayed()))
        onView(withText("Check for updates")).check(matches(isDisplayed()))
        onView(withText("Make sure installed apps are up-to-date")).check(matches(isDisplayed()))
        onView(withText("See all compatible apps")).check(matches(isDisplayed()))
        onView(withText("Find apps on Google\u00A0Play")).check(matches(isDisplayed()))
        onView(withText("Send feedback")).check(matches(isDisplayed()))
        onView(
                withText(
                    "Tell us which health & fitness apps you'd like to work with Health\u00A0Connect"))
            .check(matches(isDisplayed()))
    }
}
