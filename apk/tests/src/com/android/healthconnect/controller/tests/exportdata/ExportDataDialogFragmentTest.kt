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
package com.android.healthconnect.controller.tests.exportdata

import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.categories.HealthDataCategoriesFragment
import com.android.healthconnect.controller.categories.HealthDataCategory
import com.android.healthconnect.controller.exportdata.ExportDataSelectionItem
import com.android.healthconnect.controller.exportdata.ExportDataViewModel
import com.android.healthconnect.controller.tests.utils.launchFragment
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.hamcrest.Matchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

@HiltAndroidTest
class ExportDataDialogFragmentTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @BindValue val viewModel: ExportDataViewModel = mock(ExportDataViewModel::class.java)

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun test_AllAvailableCategoriesShown() {
        `when`(viewModel.allCategoryStates).then {
            MutableLiveData(
                arrayListOf(
                    ExportDataSelectionItem(
                        HealthDataCategory.ACTIVITY, true, R.id.activity_check_box),
                    ExportDataSelectionItem(
                        HealthDataCategory.VITALS, true, R.id.vitals_check_box)))
        }

        launchFragment<HealthDataCategoriesFragment>(Bundle())

        onView(withText("Export data")).check(matches(isDisplayed()))
        onView(withText("Export data")).perform(click())

        onView(withText("Export Health Connect data"))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        onView(withText("Deselect all")).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withText("Activity")).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withText("Sleep")).inRoot(isDialog()).check(matches(not(isDisplayed())))
        onView(withText("Vitals")).inRoot(isDialog()).check(matches(isDisplayed()))
    }
}
