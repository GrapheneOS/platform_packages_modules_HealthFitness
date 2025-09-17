/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.healthconnect.controller.tests.matchmaking

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.Gravity.CENTER
import android.widget.FrameLayout
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.matchmaking.MatchmakingHeaderIconView
import com.android.healthconnect.controller.tests.TestActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.hamcrest.core.IsNot.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MatchmakingHeaderIconViewTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    private lateinit var scenario: ActivityScenario<TestActivity>
    private lateinit var view: MatchmakingHeaderIconView
    private val mockRequestingAppIcon: Drawable = ColorDrawable(Color.RED)
    private val mockHealthConnectIcon: Drawable = ColorDrawable(Color.RED)
    private val mockMatchedAppIcon1: Drawable = ColorDrawable(Color.RED)
    private val mockMatchedAppIcon2: Drawable = ColorDrawable(Color.RED)
    private val mockMatchedAppIcon3: Drawable = ColorDrawable(Color.RED)

    @Before
    fun setup() {
        hiltRule.inject()
        scenario = ActivityScenario.launch(TestActivity::class.java)
        scenario.onActivity { activity ->
            view = MatchmakingHeaderIconView(activity)
            view.id = R.id.matchmaking_header_icon_view
            val layoutParams =
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                )
            layoutParams.gravity = CENTER
            activity.findViewById<FrameLayout>(android.R.id.content).addView(view, layoutParams)
        }
    }

    @Test
    fun setIcons_oneMatchedAppIcon_showsOnlyFirstIcon() {
        scenario.onActivity {
            view.requestingAppIcon = mockRequestingAppIcon
            view.healthConnectIcon = mockHealthConnectIcon
            view.matchedAppIcons = listOf(mockMatchedAppIcon1)
        }

        onView(withId(R.id.requesting_app_icon)).check(matches(isDisplayed()))
        onView(withId(R.id.health_connect_icon)).check(matches(isDisplayed()))
        onView(withId(R.id.line_1)).check(matches(isDisplayed()))
        onView(withId(R.id.line_2)).check(matches(isDisplayed()))
        onView(withId(R.id.matched_icons_group)).check(matches(isDisplayed()))
        onView(withId(R.id.matched_app_icon_1_container)).check(matches(isDisplayed()))
        onView(withId(R.id.matched_app_icon_2_container)).check(matches(not(isDisplayed())))
        onView(withId(R.id.plus_n_container)).check(matches(not(isDisplayed())))
    }

    @Test
    fun setIcons_twoMatchedAppIcons_showsBothIconsOverlapping() {
        scenario.onActivity {
            view.requestingAppIcon = mockRequestingAppIcon
            view.healthConnectIcon = mockHealthConnectIcon
            view.matchedAppIcons = listOf(mockMatchedAppIcon1, mockMatchedAppIcon2)
        }

        onView(withId(R.id.requesting_app_icon)).check(matches(isDisplayed()))
        onView(withId(R.id.health_connect_icon)).check(matches(isDisplayed()))
        onView(withId(R.id.line_1)).check(matches(isDisplayed()))
        onView(withId(R.id.line_2)).check(matches(isDisplayed()))
        onView(withId(R.id.matched_icons_group)).check(matches(isDisplayed()))
        onView(withId(R.id.matched_app_icon_1_container)).check(matches(isDisplayed()))
        onView(withId(R.id.matched_app_icon_2_container)).check(matches(isDisplayed()))
        onView(withId(R.id.plus_n_container)).check(matches(not(isDisplayed())))
    }

    @Test
    fun setIcons_threeMatchedAppIcons_showsFirstIconAndPlusN() {
        scenario.onActivity {
            view.requestingAppIcon = mockRequestingAppIcon
            view.healthConnectIcon = mockHealthConnectIcon
            view.matchedAppIcons =
                listOf(mockMatchedAppIcon1, mockMatchedAppIcon2, mockMatchedAppIcon3)
        }

        onView(withId(R.id.requesting_app_icon)).check(matches(isDisplayed()))
        onView(withId(R.id.health_connect_icon)).check(matches(isDisplayed()))
        onView(withId(R.id.line_1)).check(matches(isDisplayed()))
        onView(withId(R.id.line_2)).check(matches(isDisplayed()))
        onView(withId(R.id.matched_icons_group)).check(matches(isDisplayed()))
        onView(withId(R.id.matched_app_icon_1_container)).check(matches(isDisplayed()))
        onView(withId(R.id.matched_app_icon_2_container)).check(matches(not(isDisplayed())))
        onView(withId(R.id.plus_n_container)).check(matches(isDisplayed()))
    }

    @Test
    fun setIcons_sixMatchedAppIcons_showsFirstIconAndPlus5() {
        scenario.onActivity {
            view.requestingAppIcon = mockRequestingAppIcon
            view.healthConnectIcon = mockHealthConnectIcon
            view.matchedAppIcons =
                listOf(
                    mockMatchedAppIcon1,
                    mockMatchedAppIcon2,
                    mockMatchedAppIcon3,
                    mockMatchedAppIcon1,
                    mockMatchedAppIcon2,
                    mockMatchedAppIcon3,
                )
        }

        onView(withId(R.id.requesting_app_icon)).check(matches(isDisplayed()))
        onView(withId(R.id.health_connect_icon)).check(matches(isDisplayed()))
        onView(withId(R.id.line_1)).check(matches(isDisplayed()))
        onView(withId(R.id.line_2)).check(matches(isDisplayed()))
        onView(withId(R.id.matched_icons_group)).check(matches(isDisplayed()))
        onView(withId(R.id.matched_app_icon_1_container)).check(matches(isDisplayed()))
        onView(withId(R.id.matched_app_icon_2_container)).check(matches(not(isDisplayed())))
        onView(withId(R.id.plus_n_container)).check(matches(isDisplayed()))
        onView(withId(R.id.plus_n_text)).check(matches(withText("+5")))
    }
}
