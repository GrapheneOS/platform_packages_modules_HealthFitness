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
 *
 *
 */

/**
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * ```
 *      http://www.apache.org/licenses/LICENSE-2.0
 * ```
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.tests.permissions.request

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.EXTRA_REQUEST_PERMISSIONS_NAMES
import android.content.pm.PackageManager.EXTRA_REQUEST_PERMISSIONS_RESULTS
import android.content.pm.PackageManager.PERMISSION_DENIED
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.health.connect.HealthPermissions.READ_HEART_RATE
import android.health.connect.HealthPermissions.READ_STEPS
import android.health.connect.HealthPermissions.WRITE_DISTANCE
import android.health.connect.HealthPermissions.WRITE_EXERCISE
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario.launchActivityForResult
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.scrollToLastPosition
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.isNotEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.Companion.fromPermissionString
import com.android.healthconnect.controller.permissions.data.PermissionState
import com.android.healthconnect.controller.permissions.request.PermissionsActivity
import com.android.healthconnect.controller.permissions.request.RequestPermissionViewModel
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.whenever
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.instanceOf
import org.hamcrest.Matchers.`is`
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.anyString

@HiltAndroidTest
class PermissionsActivityTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @BindValue
    val viewModel: RequestPermissionViewModel = Mockito.mock(RequestPermissionViewModel::class.java)

    private lateinit var context: Context

    @Before
    fun setup() {
        hiltRule.inject()
        context = getInstrumentation().context
        val permissionsList =
            listOf(
                fromPermissionString(READ_STEPS),
                fromPermissionString(READ_HEART_RATE),
                fromPermissionString(WRITE_DISTANCE),
                fromPermissionString(WRITE_EXERCISE))
        whenever(viewModel.permissionsList).then { MutableLiveData(permissionsList) }
        whenever(viewModel.grantedPermissions).then { MutableLiveData(permissionsList.toSet()) }
        whenever(viewModel.allPermissionsGranted).then { MutableLiveData(true) }
        whenever(viewModel.appMetadata).then {
            MutableLiveData(
                AppMetadata(
                    TEST_APP_PACKAGE_NAME,
                    TEST_APP_NAME,
                    context.getDrawable(R.drawable.health_connect_logo)))
        }
        val sharedPreference =
            context.getSharedPreferences("USER_ACTIVITY_TRACKER", Context.MODE_PRIVATE)
        val editor = sharedPreference.edit()
        editor.putBoolean("Previously Opened", true)
        editor.apply()
    }

    @Test
    fun intentLaunchesPermissionsActivity() {
        val startActivityIntent =
            Intent.makeMainActivity(ComponentName(context, PermissionsActivity::class.java))
                .putExtra(Intent.EXTRA_PACKAGE_NAME, TEST_APP_PACKAGE_NAME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

        context.startActivity(startActivityIntent)
        onView(withText("Cancel")).check(matches(isDisplayed()))
        onView(withText("Allow")).check(matches(isDisplayed()))
        onView(withText("Allow $TEST_APP_NAME to access Health Connect?"))
            .check(matches(isDisplayed()))
        onView(withText("Choose data you want this app to read or write to Health Connect"))
            .check(matches(isDisplayed()))
        onView(
                withText(
                    "If you give read access, this app can read new data and data from the past 30 days"))
            .check(matches(isDisplayed()))
        onView(
                withText(
                    "You can learn how $TEST_APP_NAME handles your data in the developer's privacy policy"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun intentDisplaysPackageName() {
        val startActivityIntent =
            Intent.makeMainActivity(ComponentName(context, PermissionsActivity::class.java))
                .putExtra(Intent.EXTRA_PACKAGE_NAME, TEST_APP_PACKAGE_NAME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

        context.startActivity(startActivityIntent)

        onView(withText("Allow $TEST_APP_NAME to access Health Connect?"))
            .check(matches(isDisplayed()))
        onView(
                withText(
                    "You can learn how $TEST_APP_NAME handles your data in the developer's privacy policy"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun intentDisplaysPermissions() {
        val startActivityIntent =
            Intent.makeMainActivity(ComponentName(context, PermissionsActivity::class.java))
                .putExtra(
                    EXTRA_REQUEST_PERMISSIONS_NAMES,
                    arrayOf(READ_STEPS, READ_HEART_RATE, WRITE_DISTANCE, WRITE_EXERCISE))
                .putExtra(Intent.EXTRA_PACKAGE_NAME, TEST_APP_PACKAGE_NAME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

        context.startActivity(startActivityIntent)
        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(scrollToLastPosition<RecyclerView.ViewHolder>())
        Espresso.onIdle()

        onData(allOf(`is`(instanceOf(TextView::class.java)), `is`("")))
        onView(withText("Steps")).check(matches(isDisplayed()))
        onView(withText("Distance")).check(matches(isDisplayed()))
    }

    @Test
    fun intentSkipsUnrecognisedPermission() {
        val startActivityIntent =
            getPermissionScreenIntent(arrayOf(READ_STEPS, WRITE_EXERCISE, "permission"))

        context.startActivity(startActivityIntent)
        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(scrollToLastPosition<RecyclerView.ViewHolder>())
        Espresso.onIdle()

        onView(withText("Steps")).check(matches(isDisplayed()))
        onView(withText("Exercise")).check(matches(isDisplayed()))
    }

    @Test
    fun sendsOkResult_emptyRequest() {
        val startActivityIntent =
            Intent.makeMainActivity(ComponentName(context, PermissionsActivity::class.java))
                .putExtra(EXTRA_REQUEST_PERMISSIONS_NAMES, arrayOf<String>())
                .putExtra(Intent.EXTRA_PACKAGE_NAME, TEST_APP_PACKAGE_NAME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

        val scenario = launchActivityForResult<PermissionsActivity>(startActivityIntent)

        scenario.onActivity { activity: PermissionsActivity ->
            activity.findViewById<Button>(R.id.allow).callOnClick()
        }

        assertThat(scenario.getResult().getResultCode()).isEqualTo(Activity.RESULT_OK)
        val returnedIntent = scenario.getResult().getResultData()
        assertThat(returnedIntent.getStringArrayExtra(EXTRA_REQUEST_PERMISSIONS_NAMES)).isEmpty()
        assertThat(returnedIntent.getIntArrayExtra(EXTRA_REQUEST_PERMISSIONS_RESULTS)).isEmpty()
    }

    @Test
    fun sendsOkResult_requestWithPermissions() {
        whenever(viewModel.request(anyString())).then {
            mapOf(
                fromPermissionString(READ_STEPS) to PermissionState.GRANTED,
                fromPermissionString(READ_HEART_RATE) to PermissionState.GRANTED,
                fromPermissionString(WRITE_DISTANCE) to PermissionState.GRANTED,
                fromPermissionString(WRITE_EXERCISE) to PermissionState.GRANTED)
        }
        val permissions = arrayOf(READ_STEPS, READ_HEART_RATE, WRITE_DISTANCE, WRITE_EXERCISE)

        val startActivityIntent = getPermissionScreenIntent(permissions)

        val scenario = launchActivityForResult<PermissionsActivity>(startActivityIntent)

        scenario.onActivity { activity: PermissionsActivity ->
            activity.findViewById<Button>(R.id.allow).callOnClick()
        }

        assertThat(scenario.getResult().getResultCode()).isEqualTo(Activity.RESULT_OK)
        val returnedIntent = scenario.getResult().getResultData()
        assertThat(returnedIntent.getStringArrayExtra(EXTRA_REQUEST_PERMISSIONS_NAMES))
            .isEqualTo(permissions)
        assertThat(returnedIntent.getIntArrayExtra(EXTRA_REQUEST_PERMISSIONS_RESULTS))
            .isEqualTo(
                intArrayOf(
                    PERMISSION_GRANTED, PERMISSION_GRANTED, PERMISSION_GRANTED, PERMISSION_GRANTED))
    }

    @Test
    fun allowButton_noPermissionsSelected_isDisabled() {
        val permissions = arrayOf(READ_STEPS, READ_HEART_RATE, WRITE_DISTANCE, WRITE_EXERCISE)
        whenever(viewModel.grantedPermissions).then {
            MutableLiveData(emptySet<HealthPermission>())
        }

        val startActivityIntent = getPermissionScreenIntent(permissions)

        launchActivityForResult<PermissionsActivity>(startActivityIntent)

        onView(withText("Allow")).check(matches(isNotEnabled()))
    }

    @Test
    fun allowButton_permissionsSelected_isEnabled() {
        val permissions = arrayOf(READ_STEPS, READ_HEART_RATE, WRITE_DISTANCE, WRITE_EXERCISE)
        whenever(viewModel.grantedPermissions).then {
            MutableLiveData(setOf(fromPermissionString(READ_STEPS)))
        }

        val startActivityIntent = getPermissionScreenIntent(permissions)

        launchActivityForResult<PermissionsActivity>(startActivityIntent)

        onView(withText("Allow")).check(matches(isEnabled()))
    }

    @Test
    fun sendsOkResult_requestWithPermissionsSomeDenied() {
        val permissions = arrayOf(READ_STEPS, READ_HEART_RATE, WRITE_DISTANCE, WRITE_EXERCISE)
        whenever(viewModel.request(anyString())).then {
            mapOf(
                fromPermissionString(READ_STEPS) to PermissionState.GRANTED,
                fromPermissionString(READ_HEART_RATE) to PermissionState.GRANTED,
                fromPermissionString(WRITE_DISTANCE) to PermissionState.NOT_GRANTED,
                fromPermissionString(WRITE_EXERCISE) to PermissionState.GRANTED,
            )
        }
        val startActivityIntent = getPermissionScreenIntent(permissions)

        val scenario = launchActivityForResult<PermissionsActivity>(startActivityIntent)

        scenario.onActivity { activity: PermissionsActivity ->
            activity.findViewById<Button>(R.id.allow).callOnClick()
        }

        assertThat(scenario.getResult().getResultCode()).isEqualTo(Activity.RESULT_OK)
        val returnedIntent = scenario.getResult().getResultData()
        assertThat(returnedIntent.getStringArrayExtra(EXTRA_REQUEST_PERMISSIONS_NAMES))
            .isEqualTo(permissions)
        assertThat(returnedIntent.getIntArrayExtra(EXTRA_REQUEST_PERMISSIONS_RESULTS))
            .isEqualTo(
                intArrayOf(
                    PERMISSION_GRANTED, PERMISSION_GRANTED, PERMISSION_DENIED, PERMISSION_GRANTED))
    }

    @Test
    fun sendsOkResult_requestWithPermissionsSomeWithError() {
        whenever(viewModel.request(anyString())).then {
            mapOf(
                fromPermissionString(READ_STEPS) to PermissionState.GRANTED,
                fromPermissionString(READ_HEART_RATE) to PermissionState.GRANTED,
                fromPermissionString(WRITE_DISTANCE) to PermissionState.GRANTED,
                fromPermissionString(WRITE_EXERCISE) to PermissionState.ERROR)
        }
        val permissions = arrayOf(READ_STEPS, READ_HEART_RATE, WRITE_DISTANCE, WRITE_EXERCISE)

        val startActivityIntent = getPermissionScreenIntent(permissions)

        val scenario = launchActivityForResult<PermissionsActivity>(startActivityIntent)

        scenario.onActivity { activity: PermissionsActivity ->
            activity.findViewById<Button>(R.id.allow).callOnClick()
        }

        assertThat(scenario.getResult().getResultCode()).isEqualTo(Activity.RESULT_OK)
        val returnedIntent = scenario.getResult().getResultData()
        assertThat(returnedIntent.getStringArrayExtra(EXTRA_REQUEST_PERMISSIONS_NAMES))
            .isEqualTo(permissions)
        assertThat(returnedIntent.getIntArrayExtra(EXTRA_REQUEST_PERMISSIONS_RESULTS))
            .isEqualTo(
                intArrayOf(
                    PERMISSION_GRANTED, PERMISSION_GRANTED, PERMISSION_GRANTED, PERMISSION_DENIED))
    }

    @Test
    fun allPermissionsGranted_finishesActivity() {
        whenever(viewModel.request(anyString())).then {
            mapOf(
                fromPermissionString(READ_STEPS) to PermissionState.GRANTED,
                fromPermissionString(READ_HEART_RATE) to PermissionState.GRANTED,
                fromPermissionString(WRITE_DISTANCE) to PermissionState.GRANTED,
                fromPermissionString(WRITE_EXERCISE) to PermissionState.GRANTED)
        }
        whenever(viewModel.permissionsList).then {
            MutableLiveData<List<HealthPermission>>(emptyList())
        }
        val permissions = arrayOf(READ_STEPS, READ_HEART_RATE, WRITE_DISTANCE, WRITE_EXERCISE)

        val startActivityIntent = getPermissionScreenIntent(permissions)

        val scenario = launchActivityForResult<PermissionsActivity>(startActivityIntent)

        assertThat(scenario.result.resultCode).isEqualTo(Activity.RESULT_OK)
        val returnedIntent = scenario.getResult().getResultData()
        assertThat(returnedIntent.getStringArrayExtra(EXTRA_REQUEST_PERMISSIONS_NAMES))
            .isEqualTo(permissions)
        assertThat(returnedIntent.getIntArrayExtra(EXTRA_REQUEST_PERMISSIONS_RESULTS))
            .isEqualTo(
                intArrayOf(
                    PERMISSION_GRANTED, PERMISSION_GRANTED, PERMISSION_GRANTED, PERMISSION_GRANTED))
    }

    private fun getPermissionScreenIntent(permissions: Array<String>): Intent =
        Intent.makeMainActivity(ComponentName(context, PermissionsActivity::class.java))
            .putExtra(EXTRA_REQUEST_PERMISSIONS_NAMES, permissions)
            .putExtra(Intent.EXTRA_PACKAGE_NAME, TEST_APP_PACKAGE_NAME)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
}
