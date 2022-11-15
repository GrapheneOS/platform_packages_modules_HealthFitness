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

package com.android.healthconnect.controller.tests.autodelete

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.android.healthconnect.controller.autodelete.AutoDeleteConfirmationDialogFragment
import com.android.healthconnect.controller.autodelete.AutoDeleteRange
import com.android.healthconnect.controller.tests.TestActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

@HiltAndroidTest
class AutoDeleteConfirmationDialogFragmentTest {

    private val hiltRule = HiltAndroidRule(this)
    private val activityScenarioRule = ActivityScenarioRule(TestActivity::class.java)

    @get:Rule val rules: RuleChain = RuleChain.outerRule(hiltRule).around(activityScenarioRule)

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun autoDeleteConfirmationDialog_rangeThreeMonths_showsCorrectTexts() {
        activityScenarioRule.scenario.onActivity { activity ->
            AutoDeleteConfirmationDialogFragment(AutoDeleteRange.AUTO_DELETE_RANGE_THREE_MONTHS)
                .show(activity.supportFragmentManager, AutoDeleteConfirmationDialogFragment.TAG)
        }

        onView(withText("Auto-delete data after 3 months?"))
            .inRoot(RootMatchers.isDialog())
            .check(matches(isDisplayed()))
        onView(
                withText(
                    "Health\u00A0Connect will auto-delete new data after 3 months. Setting this will also delete existing data older than 3 months."))
            .inRoot(RootMatchers.isDialog())
            .check(matches(isDisplayed()))
        onView(withText("Set auto-delete"))
            .inRoot(RootMatchers.isDialog())
            .check(matches(isDisplayed()))
        onView(withText("Cancel")).inRoot(RootMatchers.isDialog()).check(matches(isDisplayed()))
    }

    @Test
    fun autoDeleteConfirmationDialog_rangeEighteenMonths_showsCorrectTexts() {
        activityScenarioRule.scenario.onActivity { activity ->
            AutoDeleteConfirmationDialogFragment(AutoDeleteRange.AUTO_DELETE_RANGE_EIGHTEEN_MONTHS)
                .show(activity.supportFragmentManager, AutoDeleteConfirmationDialogFragment.TAG)
        }

        onView(withText("Auto-delete data after 18 months?"))
            .inRoot(RootMatchers.isDialog())
            .check(matches(isDisplayed()))
        onView(
                withText(
                    "Health\u00A0Connect will auto-delete new data after 18 months. Setting this will also delete existing data older than 18 months."))
            .inRoot(RootMatchers.isDialog())
            .check(matches(isDisplayed()))
        onView(withText("Set auto-delete"))
            .inRoot(RootMatchers.isDialog())
            .check(matches(isDisplayed()))
        onView(withText("Cancel")).inRoot(RootMatchers.isDialog()).check(matches(isDisplayed()))
    }
}
