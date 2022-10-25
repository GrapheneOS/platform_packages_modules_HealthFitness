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
import com.android.healthconnect.controller.home.HomeFragment
import com.android.healthconnect.controller.home.HomeFragmentViewModel
import com.android.healthconnect.controller.recentaccess.RECENT_APP_1
import com.android.healthconnect.controller.recentaccess.RecentAccessApp
import com.android.healthconnect.controller.tests.utils.launchFragment
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito

@HiltAndroidTest
class HomeFragmentTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @BindValue
    val viewModel: HomeFragmentViewModel = Mockito.mock(HomeFragmentViewModel::class.java)

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun test_HomeFragment_withRecentAccessApps() {
        Mockito.`when`(viewModel.recentAccessApps).then { MutableLiveData(listOf(RECENT_APP_1)) }
        launchFragment<HomeFragment>(Bundle())

        onView(withText(R.string.home_subtitle)).check(matches(isDisplayed()))
        onView(withText(R.string.connected_apps_title)).check(matches(isDisplayed()))
        onView(withText(R.string.connected_apps_subtitle)).check(matches(isDisplayed()))
        onView(withText(R.string.data_title)).check(matches(isDisplayed()))
        onView(withText(R.string.data_subtitle)).check(matches(isDisplayed()))

        onView(withText(R.string.recent_access_header)).check(matches(isDisplayed()))
        onView(withText(R.string.recent_app_1)).check(matches(isDisplayed()))
        onView(withText(R.string.see_all_recent_access)).check(matches(isDisplayed()))
    }

    @Test
    fun test_HomeFragment_withNoRecentAccessApps() {
        Mockito.`when`(viewModel.recentAccessApps).then {
            MutableLiveData<List<RecentAccessApp>>(emptyList())
        }
        launchFragment<HomeFragment>(Bundle())

        onView(withText(R.string.home_subtitle)).check(matches(isDisplayed()))
        onView(withText(R.string.connected_apps_title)).check(matches(isDisplayed()))
        onView(withText(R.string.connected_apps_subtitle)).check(matches(isDisplayed()))
        onView(withText(R.string.data_title)).check(matches(isDisplayed()))
        onView(withText(R.string.data_subtitle)).check(matches(isDisplayed()))

        onView(withText(R.string.recent_access_header)).check(matches(isDisplayed()))
        onView(withText(R.string.no_recent_access)).check(matches(isDisplayed()))
    }
}
