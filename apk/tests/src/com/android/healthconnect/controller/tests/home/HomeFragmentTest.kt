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
package com.android.healthconnect.controller.tests.home

import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.categories.HealthDataCategory
import com.android.healthconnect.controller.home.HomeFragment
import com.android.healthconnect.controller.home.HomeFragmentViewModel
import com.android.healthconnect.controller.recentaccess.RecentAccessEntry
import com.android.healthconnect.controller.recentaccess.RecentAccessViewModel
import com.android.healthconnect.controller.tests.utils.TEST_APP
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME
import com.android.healthconnect.controller.tests.utils.launchFragment
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.Instant
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito

@HiltAndroidTest
class HomeFragmentTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @BindValue
    val viewModel: HomeFragmentViewModel = Mockito.mock(HomeFragmentViewModel::class.java)

    @BindValue
    val recentAccessViewModel: RecentAccessViewModel =
        Mockito.mock(RecentAccessViewModel::class.java)

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun test_HomeFragment_withRecentAccessApps() {
        val recentApp =
            RecentAccessEntry(
                metadata = TEST_APP,
                instantTime = Instant.parse("2022-10-20T18:40:13.00Z"),
                isToday = true,
                dataTypesWritten =
                    mutableSetOf(
                        HealthDataCategory.ACTIVITY.uppercaseTitle,
                        HealthDataCategory.VITALS.uppercaseTitle),
                dataTypesRead =
                    mutableSetOf(
                        HealthDataCategory.SLEEP.uppercaseTitle,
                        HealthDataCategory.NUTRITION.uppercaseTitle))

        Mockito.`when`(recentAccessViewModel.recentAccessApps).then {
            MutableLiveData(listOf(recentApp))
        }
        launchFragment<HomeFragment>(Bundle())

        onView(withText("Manage the health and fitness data on your phone, and control which apps can access it")).check(matches(isDisplayed()))
        onView(withText("App permissions")).check(matches(isDisplayed()))
        onView(withText("Manage your apps and permissions")).check(matches(isDisplayed()))
        onView(withText("Data and access")).check(matches(isDisplayed()))

        onView(withText("Recent access")).check(matches(isDisplayed()))
        onView(withText(TEST_APP_NAME)).check(matches(isDisplayed()))
        onView(withText("18:40")).check(matches(isDisplayed()))
        onView(withText("See all recent access")).check(matches(isDisplayed()))
    }

    @Test
    fun test_HomeFragment_withNoRecentAccessApps() {
        Mockito.`when`(recentAccessViewModel.recentAccessApps).then {
            MutableLiveData<List<RecentAccessEntry>>(emptyList())
        }
        launchFragment<HomeFragment>(Bundle())

        onView(withText("Manage the health and fitness data on your phone, and control which apps can access it")).check(matches(isDisplayed()))
        onView(withText("App permissions")).check(matches(isDisplayed()))
        onView(withText("Manage your apps and permissions")).check(matches(isDisplayed()))
        onView(withText("Data and access")).check(matches(isDisplayed()))

        onView(withText("Recent access")).check(matches(isDisplayed()))
        onView(withText("No apps recently accessed Health\u00A0Connect")).check(matches(isDisplayed()))
    }
}
