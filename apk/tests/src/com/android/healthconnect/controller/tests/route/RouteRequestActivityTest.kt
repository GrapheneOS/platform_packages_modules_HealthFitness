/*
 * Copyright (C) 2023 The Android Open Source Project
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
package com.android.healthconnect.controller.tests.route

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.health.connect.HealthConnectManager.EXTRA_EXERCISE_ROUTE
import android.health.connect.HealthConnectManager.EXTRA_SESSION_ID
import android.health.connect.datatypes.ExerciseRoute
import android.health.connect.datatypes.ExerciseSessionRecord
import android.health.connect.datatypes.ExerciseSessionType
import android.widget.Button
import android.widget.LinearLayout
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ActivityScenario.launch
import androidx.test.core.app.ActivityScenario.launchActivityForResult
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.onboarding.OnboardingActivity.Companion.ONBOARDING_SHOWN_PREF_KEY
import com.android.healthconnect.controller.onboarding.OnboardingActivity.Companion.USER_ACTIVITY_TRACKER
import com.android.healthconnect.controller.route.ExerciseRouteViewModel
import com.android.healthconnect.controller.route.ExerciseRouteViewModel.SessionWithAttribution
import com.android.healthconnect.controller.route.RouteRequestActivity
import com.android.healthconnect.controller.tests.utils.TEST_APP
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME_2
import com.android.healthconnect.controller.tests.utils.getMetaData
import com.android.healthconnect.controller.tests.utils.setLocale
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import java.util.TimeZone
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`

@HiltAndroidTest
class RouteRequestActivityTest {

    private val START = Instant.ofEpochMilli(1234567891011)
    private val TEST_SESSION =
        ExerciseSessionRecord.Builder(
                getMetaData(),
                START,
                START.plusMillis(123456),
                ExerciseSessionType.EXERCISE_SESSION_TYPE_RUNNING)
            .setTitle("Session title")
            .setRoute(
                ExerciseRoute(
                    listOf(
                        ExerciseRoute.Location.Builder(START.plusSeconds(12), 52.26019, 21.02268)
                            .build(),
                        ExerciseRoute.Location.Builder(START.plusSeconds(40), 52.26000, 21.02360)
                            .build(),
                        ExerciseRoute.Location.Builder(START.plusSeconds(48), 52.25973, 21.02356)
                            .build(),
                        ExerciseRoute.Location.Builder(START.plusSeconds(60), 52.25966, 21.02313)
                            .build(),
                        ExerciseRoute.Location.Builder(START.plusSeconds(78), 52.25993, 21.02309)
                            .build(),
                        ExerciseRoute.Location.Builder(START.plusSeconds(79), 52.25972, 21.02271)
                            .build(),
                        ExerciseRoute.Location.Builder(START.plusSeconds(90), 52.25948, 21.02276)
                            .build(),
                        ExerciseRoute.Location.Builder(START.plusSeconds(93), 52.25945, 21.02335)
                            .build(),
                        ExerciseRoute.Location.Builder(START.plusSeconds(94), 52.25960, 21.02338)
                            .build(),
                        ExerciseRoute.Location.Builder(START.plusSeconds(100), 52.25961, 21.02382)
                            .build(),
                        ExerciseRoute.Location.Builder(START.plusSeconds(102), 52.25954, 21.02370)
                            .build(),
                        ExerciseRoute.Location.Builder(START.plusSeconds(105), 52.25945, 21.02362)
                            .build(),
                        ExerciseRoute.Location.Builder(START.plusSeconds(109), 52.25954, 21.02354)
                            .build(),
                    )))
            .build()

    @get:Rule val hiltRule = HiltAndroidRule(this)
    @BindValue
    val viewModel: ExerciseRouteViewModel = Mockito.mock(ExerciseRouteViewModel::class.java)

    private lateinit var context: Context

    @Before
    fun setup() {
        hiltRule.inject()
        context = getInstrumentation().context
        context.setLocale(Locale.US)
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("UTC")))
    }

    @Test
    fun intentLaunchesPermissionsActivity_noSessionId() {
        val startActivityIntent =
            Intent.makeMainActivity(ComponentName(context, RouteRequestActivity::class.java))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

        `when`(viewModel.exerciseSession).then {
            MutableLiveData(SessionWithAttribution(TEST_SESSION, TEST_APP))
        }

        val scenario = launchActivityForResult<RouteRequestActivity>(startActivityIntent)

        assertThat(scenario.getResult().getResultCode()).isEqualTo(Activity.RESULT_CANCELED)
        val returnedIntent = scenario.getResult().getResultData()
        assertThat(returnedIntent.hasExtra(EXTRA_EXERCISE_ROUTE)).isFalse()
    }

    @Test
    fun intentLaunchesPermissionsActivity_nullSessionId() {
        val startActivityIntent =
            Intent.makeMainActivity(ComponentName(context, RouteRequestActivity::class.java))
                .putExtra(EXTRA_SESSION_ID, null as String?)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

        `when`(viewModel.exerciseSession).then {
            MutableLiveData(SessionWithAttribution(TEST_SESSION, TEST_APP))
        }

        val scenario = launchActivityForResult<RouteRequestActivity>(startActivityIntent)

        assertThat(scenario.getResult().getResultCode()).isEqualTo(Activity.RESULT_CANCELED)
        val returnedIntent = scenario.getResult().getResultData()
        assertThat(returnedIntent.hasExtra(EXTRA_EXERCISE_ROUTE)).isFalse()
    }

    @Test
    fun intentLaunchesPermissionsActivity_noRoute() {
        val startActivityIntent =
            Intent.makeMainActivity(ComponentName(context, RouteRequestActivity::class.java))
                .putExtra(Intent.EXTRA_PACKAGE_NAME, TEST_APP_PACKAGE_NAME_2)
                .putExtra(EXTRA_SESSION_ID, "sessionID")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

        `when`(viewModel.exerciseSession).then {
            MutableLiveData(
                SessionWithAttribution(
                    ExerciseSessionRecord.Builder(
                            getMetaData(),
                            START,
                            START.plusMillis(123456),
                            ExerciseSessionType.EXERCISE_SESSION_TYPE_RUNNING)
                        .setTitle("Session title")
                        .setRoute(ExerciseRoute(listOf()))
                        .build(),
                    TEST_APP))
        }

        val scenario = launchActivityForResult<RouteRequestActivity>(startActivityIntent)

        assertThat(scenario.getResult().getResultCode()).isEqualTo(Activity.RESULT_CANCELED)
        val returnedIntent = scenario.getResult().getResultData()
        assertThat(returnedIntent.hasExtra(EXTRA_EXERCISE_ROUTE)).isFalse()
    }

    @Test
    fun intentLaunchesPermissionsActivity_emptyRoute() {
        val startActivityIntent =
            Intent.makeMainActivity(ComponentName(context, RouteRequestActivity::class.java))
                .putExtra(Intent.EXTRA_PACKAGE_NAME, TEST_APP_PACKAGE_NAME_2)
                .putExtra(EXTRA_SESSION_ID, "sessionID")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

        `when`(viewModel.exerciseSession).then {
            MutableLiveData(
                SessionWithAttribution(
                    ExerciseSessionRecord.Builder(
                            getMetaData(),
                            START,
                            START.plusMillis(123456),
                            ExerciseSessionType.EXERCISE_SESSION_TYPE_RUNNING)
                        .setTitle("Session title")
                        .setRoute(ExerciseRoute(listOf()))
                        .build(),
                    TEST_APP))
        }

        val scenario = launchActivityForResult<RouteRequestActivity>(startActivityIntent)

        assertThat(scenario.getResult().getResultCode()).isEqualTo(Activity.RESULT_CANCELED)
        val returnedIntent = scenario.getResult().getResultData()
        assertThat(returnedIntent.hasExtra(EXTRA_EXERCISE_ROUTE)).isFalse()
    }

    @Test
    fun intentLaunchesPermissionsActivity() {
        val startActivityIntent =
            Intent.makeMainActivity(ComponentName(context, RouteRequestActivity::class.java))
                .putExtra(EXTRA_SESSION_ID, "sessionID")
                .putExtra(Intent.EXTRA_PACKAGE_NAME, TEST_APP_PACKAGE_NAME_2)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

        `when`(viewModel.exerciseSession).then {
            MutableLiveData(SessionWithAttribution(TEST_SESSION, TEST_APP))
        }

        launchActivityForResult<RouteRequestActivity>(startActivityIntent)

        onView(withText("Don\'t allow")).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withText("Allow")).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(
                withText(
                    "Allow Health Connect to access this exercise route in Health Connect?"))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        onView(withText("This app will be able to read your past location in the route"))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        onView(withText("Session title")).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withText("February 13, 2009 • Health Connect test app"))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        onView(withId(R.id.map_view)).inRoot(isDialog()).check(matches(isDisplayed()))
    }

    @Test
    fun intentLaunchesPermissionsActivity_sessionWithNoTitle() {
        val startActivityIntent =
            Intent.makeMainActivity(ComponentName(context, RouteRequestActivity::class.java))
                .putExtra(EXTRA_SESSION_ID, "sessionID")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

        `when`(viewModel.exerciseSession).then {
            MutableLiveData(
                SessionWithAttribution(
                    ExerciseSessionRecord.Builder(
                            getMetaData(),
                            START,
                            START.plusMillis(123456),
                            ExerciseSessionType.EXERCISE_SESSION_TYPE_RUNNING)
                        .setRoute(
                            ExerciseRoute(
                                listOf(
                                    ExerciseRoute.Location.Builder(
                                            START.plusSeconds(12), 52.26019, 21.02268)
                                        .build(),
                                    ExerciseRoute.Location.Builder(
                                            START.plusSeconds(40), 52.26000, 21.02360)
                                        .build())))
                        .build(),
                    TEST_APP))
        }

        launchActivityForResult<RouteRequestActivity>(startActivityIntent)

        onView(withText("Don\'t allow")).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withText("Allow")).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(
                withText(
                    "Allow Health Connect to access this exercise route in Health Connect?"))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        onView(withText("This app will be able to read your past location in the route"))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        onView(withText("Running")).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withText("February 13, 2009 • Health Connect test app"))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        onView(withId(R.id.map_view)).inRoot(isDialog()).check(matches(isDisplayed()))
    }

    @Test
    fun intentLaunchesPermissionsActivity_infoDialog() {
        val startActivityIntent =
            Intent.makeMainActivity(ComponentName(context, RouteRequestActivity::class.java))
                .putExtra(EXTRA_SESSION_ID, "sessionID")
                .putExtra(Intent.EXTRA_PACKAGE_NAME, TEST_APP_PACKAGE_NAME_2)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

        `when`(viewModel.exerciseSession).then {
            MutableLiveData(SessionWithAttribution(TEST_SESSION, TEST_APP))
        }

        val scenario = launchActivityForResult<RouteRequestActivity>(startActivityIntent)

        scenario.onActivity { activity: RouteRequestActivity ->
            activity.dialog.findViewById<LinearLayout>(R.id.more_info)?.callOnClick()
        }

        onView(withText("Exercise routes include location information"))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        onView(withText("Who can see this data?")).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withText("Only apps you allow to access your exercise routes"))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        onView(withText("How can I manage access?"))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        onView(withText("You can manage app access to exercise routes in Health Connect settings"))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
    }

    @Test
    fun intentLaunchesPermissionsActivity_dontAllow() {
        val startActivityIntent =
            Intent.makeMainActivity(ComponentName(context, RouteRequestActivity::class.java))
                .putExtra(Intent.EXTRA_PACKAGE_NAME, TEST_APP_PACKAGE_NAME_2)
                .putExtra(EXTRA_SESSION_ID, "sessionID")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

        `when`(viewModel.exerciseSession).then {
            MutableLiveData(SessionWithAttribution(TEST_SESSION, TEST_APP))
        }

        val scenario = launchActivityForResult<RouteRequestActivity>(startActivityIntent)

        scenario.onActivity { activity: RouteRequestActivity ->
            activity.dialog.findViewById<Button>(R.id.route_dont_allow_button)?.callOnClick()
        }

        assertThat(scenario.getResult().getResultCode()).isEqualTo(Activity.RESULT_CANCELED)
        val returnedIntent = scenario.getResult().getResultData()
        assertThat(returnedIntent.hasExtra(EXTRA_EXERCISE_ROUTE)).isFalse()
    }

    @Test
    fun intentLaunchesPermissionsActivity_allow() {
        val startActivityIntent =
            Intent.makeMainActivity(ComponentName(context, RouteRequestActivity::class.java))
                .putExtra(Intent.EXTRA_PACKAGE_NAME, TEST_APP_PACKAGE_NAME_2)
                .putExtra(EXTRA_SESSION_ID, "sessionID")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

        `when`(viewModel.exerciseSession).then {
            MutableLiveData(SessionWithAttribution(TEST_SESSION, TEST_APP))
        }

        val scenario = launchActivityForResult<RouteRequestActivity>(startActivityIntent)

        scenario.onActivity { activity: RouteRequestActivity ->
            activity.dialog.findViewById<Button>(R.id.route_allow_button)?.callOnClick()
        }

        assertThat(scenario.getResult().getResultCode()).isEqualTo(Activity.RESULT_OK)
        val returnedIntent = scenario.getResult().getResultData()
        assertThat(returnedIntent.hasExtra(EXTRA_EXERCISE_ROUTE)).isTrue()
        assertThat(returnedIntent.getParcelableExtra<ExerciseRoute>(EXTRA_EXERCISE_ROUTE))
            .isEqualTo(TEST_SESSION.route)
    }
}
