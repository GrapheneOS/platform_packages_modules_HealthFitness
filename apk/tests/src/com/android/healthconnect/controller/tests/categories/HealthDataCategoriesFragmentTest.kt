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
package com.android.healthconnect.controller.tests.categories

import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.android.healthconnect.controller.categories.AllCategoriesScreenHealthDataCategory
import com.android.healthconnect.controller.categories.HealthDataCategoriesFragment
import com.android.healthconnect.controller.categories.HealthDataCategory
import com.android.healthconnect.controller.categories.HealthDataCategoryViewModel
import com.android.healthconnect.controller.tests.utils.launchFragment
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.hamcrest.Matchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito

@HiltAndroidTest
class HealthDataCategoriesFragmentTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @BindValue
    val viewModel: HealthDataCategoryViewModel =
        Mockito.mock(HealthDataCategoryViewModel::class.java)

    /** List of all Health data categories. */
    val HEALTH_DATA_ALL_CATEGORIES =
        listOf(
            AllCategoriesScreenHealthDataCategory(
                category = HealthDataCategory.ACTIVITY, noData = false),
            AllCategoriesScreenHealthDataCategory(
                category = HealthDataCategory.BODY_MEASUREMENTS, noData = false),
            AllCategoriesScreenHealthDataCategory(
                category = HealthDataCategory.SLEEP, noData = false),
            AllCategoriesScreenHealthDataCategory(
                category = HealthDataCategory.VITALS, noData = false),
            AllCategoriesScreenHealthDataCategory(
                category = HealthDataCategory.CYCLE_TRACKING, noData = false),
            AllCategoriesScreenHealthDataCategory(
                category = HealthDataCategory.NUTRITION, noData = true),
        )

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun categoriesFragment_isDisplayed() {
        Mockito.`when`(viewModel.categoriesData).then {
            MutableLiveData<List<HealthDataCategory>>(emptyList())
        }
        Mockito.`when`(viewModel.allCategoriesData).then {
            MutableLiveData(HEALTH_DATA_ALL_CATEGORIES)
        }
        launchFragment<HealthDataCategoriesFragment>(Bundle())

        onView(withText("Browse data")).check(matches(isDisplayed()))
        onView(withText("Manage data")).check(matches(isDisplayed()))
    }

    @Test
    fun categoriesFragment_emptyCategories_noDataViewIsDisplayed_deleteIconIsDisabled() {
        Mockito.`when`(viewModel.categoriesData).then {
            MutableLiveData<List<HealthDataCategory>>(emptyList())
        }
        Mockito.`when`(viewModel.allCategoriesData).then {
            MutableLiveData(HEALTH_DATA_ALL_CATEGORIES)
        }
        launchFragment<HealthDataCategoriesFragment>(Bundle())

        onView(withText("You don't have any data in Health\u00A0Connect"))
            .check(matches(isDisplayed()))
        onView(withText("Delete all data")).check(matches(not(isEnabled())))
    }

    @Test
    fun categoriesFragment_withCategories_categoryInformationIsDisplayed() {
        Mockito.`when`(viewModel.categoriesData).then {
            MutableLiveData(
                listOf(HealthDataCategory.ACTIVITY, HealthDataCategory.BODY_MEASUREMENTS))
        }
        Mockito.`when`(viewModel.allCategoriesData).then {
            MutableLiveData(HEALTH_DATA_ALL_CATEGORIES)
        }
        launchFragment<HealthDataCategoriesFragment>(Bundle())

        onView(withText("Activity")).check(matches(isDisplayed()))
        onView(withText("Body measurements")).check(matches(isDisplayed()))
    }

    @Test
    fun seeAllCategoriesPreference_isDisplayed() {
        Mockito.`when`(viewModel.categoriesData).then {
            MutableLiveData(listOf(HealthDataCategory.ACTIVITY))
        }
        Mockito.`when`(viewModel.allCategoriesData).then {
            MutableLiveData(HEALTH_DATA_ALL_CATEGORIES)
        }
        launchFragment<HealthDataCategoriesFragment>(Bundle())

        onView(withText("See all categories")).check(matches(isDisplayed()))
    }

    @Test
    fun categoriesFragment_withAllCategoriesPresent_seeAllCategoriesPreferenceIsNotDisplayed() {
        Mockito.`when`(viewModel.allCategoriesData).then {
            MutableLiveData(HEALTH_DATA_ALL_CATEGORIES)
        }
        Mockito.`when`(viewModel.categoriesData).then {
            MutableLiveData(
                listOf(
                    HealthDataCategory.ACTIVITY,
                    HealthDataCategory.BODY_MEASUREMENTS,
                    HealthDataCategory.SLEEP,
                    HealthDataCategory.VITALS,
                    HealthDataCategory.NUTRITION,
                    HealthDataCategory.CYCLE_TRACKING))
        }
        launchFragment<HealthDataCategoriesFragment>(Bundle())

        onView(withText("See all categories")).check(doesNotExist())
    }
}
