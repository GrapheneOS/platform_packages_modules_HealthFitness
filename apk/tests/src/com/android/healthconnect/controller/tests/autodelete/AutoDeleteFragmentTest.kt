/**
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * ```
 *      http://www.apache.org/licenses/LICENSE-2.0
 * ```
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.tests.autodelete

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.autodelete.AutoDeleteFragment
import com.android.healthconnect.controller.tests.utils.launchFragment
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class AutoDeleteFragmentTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun autoDeleteFragment_isDisplayed() {
        launchFragment<AutoDeleteFragment>()

        onView(
                withText(
                    "Control how long your data is stored in Health\u00A0Connect by scheduling it to delete after a set time"))
            .check(matches(isDisplayed()))
        onView(withText("Auto-delete data")).check(matches(isDisplayed()))
        onView(withText("After 3 months")).check(matches(isDisplayed()))
        onView(withText("After 18 months")).check(matches(isDisplayed()))
        onView(withText("Never")).check(matches(isDisplayed()))
        onView(
                withText(
                    "When you change these settings, Health\u00A0Connect deletes existing data to reflect your new preferences"))
            .check(matches(isDisplayed()))
    }

    @Test
    @Throws(Exception::class)
    fun autoDelete_checkDefaultRange_defaultRangeIsNever() {
        launchFragment<AutoDeleteFragment>()
        onView(withId(R.id.radio_button_never)).check(matches(isChecked()))
    }

    @Test
    @Throws(java.lang.Exception::class)
    fun autoDelete_setRangeTo3Months_confirmationDialog() {
        launchFragment<AutoDeleteFragment>()

        onView(withId(R.id.radio_button_3_months)).perform(click())
        onView(withText("Auto-delete data after 3 months?"))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        onView(
                withText(
                    "Health\u00A0Connect will auto-delete new data after 3 months. Setting this will also delete existing data older than 3 months."))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        onView(withText("Set auto-delete")).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withText("Cancel")).inRoot(isDialog()).check(matches(isDisplayed()))
    }

    @Test
    @Throws(java.lang.Exception::class)
    fun autoDelete_setRangeTo18Months_confirmationDialog() {
        launchFragment<AutoDeleteFragment>()

        onView(withId(R.id.radio_button_18_months)).perform(click())
        onView(withText("Auto-delete data after 18 months?"))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        onView(
                withText(
                    "Health\u00A0Connect will auto-delete new data after 18 months. Setting this will also delete existing data older than 18 months."))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        onView(withText("Set auto-delete")).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withText("Cancel")).inRoot(isDialog()).check(matches(isDisplayed()))
    }
}
