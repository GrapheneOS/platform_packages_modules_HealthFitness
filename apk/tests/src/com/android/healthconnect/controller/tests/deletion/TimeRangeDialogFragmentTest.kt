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

package com.android.healthconnect.controller.tests.deletion

import android.os.Bundle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.categories.HealthDataCategoriesFragment
import com.android.healthconnect.controller.tests.utils.launchFragment
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class TimeRangeDialogFragmentTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @Test
    fun test_TimeRangeDialogFragment_showsWhenDeleteAllDataButtonClicked() {
        launchFragment<HealthDataCategoriesFragment>(Bundle())
        onView(withText(R.string.delete_all_data_button)).perform(click())
        onView(withId(R.id.dialog_icon)).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withText(R.string.time_range_title)).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withText(R.string.time_range_message_all))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        onView(withText(R.string.time_range_one_day))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        onView(withText(R.string.time_range_one_week))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        onView(withText(R.string.time_range_one_month))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        onView(withText(R.string.time_range_all)).inRoot(isDialog()).check(matches(isDisplayed()))
    }
}
