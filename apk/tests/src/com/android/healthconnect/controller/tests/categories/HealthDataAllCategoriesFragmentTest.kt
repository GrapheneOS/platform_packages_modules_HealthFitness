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

package com.android.healthconnect.controller.tests.categories

import android.health.connect.HealthDataCategory
import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.categories.AllCategoriesScreenHealthDataCategory
import com.android.healthconnect.controller.categories.HealthDataAllCategoriesFragment
import com.android.healthconnect.controller.categories.HealthDataCategoryViewModel
import com.android.healthconnect.controller.tests.utils.launchFragment
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito

/** List of all Health data categories. */
private val HEALTH_DATA_ALL_CATEGORIES =
    listOf(
        AllCategoriesScreenHealthDataCategory(
            category = HealthDataCategory.ACTIVITY, noData = false),
        AllCategoriesScreenHealthDataCategory(
            category = HealthDataCategory.BODY_MEASUREMENTS, noData = false),
        AllCategoriesScreenHealthDataCategory(category = HealthDataCategory.SLEEP, noData = false),
        AllCategoriesScreenHealthDataCategory(category = HealthDataCategory.VITALS, noData = false),
        AllCategoriesScreenHealthDataCategory(
            category = HealthDataCategory.CYCLE_TRACKING, noData = false),
        AllCategoriesScreenHealthDataCategory(
            category = HealthDataCategory.NUTRITION, noData = true),
    )

@HiltAndroidTest
class HealthDataAllCategoriesFragmentTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @BindValue
    val viewModel: HealthDataCategoryViewModel =
        Mockito.mock(HealthDataCategoryViewModel::class.java)

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun allCategoriesFragment_isDisplayedCorrectly() {
        Mockito.`when`(viewModel.allCategoriesData).then {
            MutableLiveData(HEALTH_DATA_ALL_CATEGORIES)
        }
        launchFragment<HealthDataAllCategoriesFragment>(Bundle())

        onView(withText("Activity")).check(matches(isDisplayed()))
        onView(withText("Body measurements")).check(matches(isDisplayed()))
        onView(withText("Cycle tracking")).check(matches(isDisplayed()))
        onView(withText("Nutrition")).check(matches(isDisplayed()))
        onView(withText("Sleep")).check(matches(isDisplayed()))
        onView(withText("Vitals")).check(matches(isDisplayed()))
    }

    @Test
    fun allCategoriesFragment_noDataForSomeCategories_isDisplayedNoDataLabel() {
        Mockito.`when`(viewModel.allCategoriesData).then {
            MutableLiveData(HEALTH_DATA_ALL_CATEGORIES)
        }
        launchFragment<HealthDataAllCategoriesFragment>(Bundle())

        onView(withText(R.string.no_data)).check(matches(isDisplayed()))
    }
}
