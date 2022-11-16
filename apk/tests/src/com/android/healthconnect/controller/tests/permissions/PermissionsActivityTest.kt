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
package com.android.healthconnect.controller.tests.permissions

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager.EXTRA_REQUEST_PERMISSIONS_NAMES
import android.content.pm.PackageManager.EXTRA_REQUEST_PERMISSIONS_RESULTS
import android.content.pm.PackageManager.PERMISSION_DENIED
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.healthconnect.HealthPermissions.READ_HEART_RATE
import android.healthconnect.HealthPermissions.READ_STEPS
import android.healthconnect.HealthPermissions.WRITE_DISTANCE
import android.healthconnect.HealthPermissions.WRITE_EXERCISE
import android.widget.Button
import androidx.preference.PreferenceCategory
import androidx.preference.SwitchPreference
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.permissions.PermissionsActivity
import com.android.healthconnect.controller.permissions.PermissionsFragment
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class PermissionsActivityTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun intentLaunchesPermissionsActivity() {
        val startActivityIntent =
            Intent.makeMainActivity(
                    ComponentName(
                        getInstrumentation().getContext(), PermissionsActivity::class.java))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

        getInstrumentation().getContext().startActivity(startActivityIntent)

        onView(withText("Cancel")).check(matches(isDisplayed()))
        onView(withText("Allow")).check(matches(isDisplayed()))
    }

    @Test
    fun intentDisplaysPermissions() {
        val startActivityIntent =
            Intent.makeMainActivity(
                    ComponentName(
                        getInstrumentation().getContext(), PermissionsActivity::class.java))
                .putExtra(
                    EXTRA_REQUEST_PERMISSIONS_NAMES,
                    arrayOf(READ_STEPS, READ_HEART_RATE, WRITE_DISTANCE, WRITE_EXERCISE))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

        getInstrumentation().getContext().startActivity(startActivityIntent)

        onView(withText("Steps")).check(matches(isDisplayed()))
        onView(withText("Heart rate")).check(matches(isDisplayed()))
        onView(withText("Distance")).check(matches(isDisplayed()))
        onView(withText("Exercise")).check(matches(isDisplayed()))
    }

    @Test
    fun intentSkipsUnrecognisedPermission() {
        val startActivityIntent =
            Intent.makeMainActivity(
                    ComponentName(
                        getInstrumentation().getContext(), PermissionsActivity::class.java))
                .putExtra(
                    EXTRA_REQUEST_PERMISSIONS_NAMES,
                    arrayOf(READ_STEPS, WRITE_EXERCISE, "permission"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

        getInstrumentation().getContext().startActivity(startActivityIntent)

        onView(withText("Steps")).check(matches(isDisplayed()))
        onView(withText("Exercise")).check(matches(isDisplayed()))
    }

    @Test
    fun sendsOkResult_emptyRequest() {
        val startActivityIntent =
            Intent.makeMainActivity(
                    ComponentName(
                        getInstrumentation().getContext(), PermissionsActivity::class.java))
                .putExtra(EXTRA_REQUEST_PERMISSIONS_NAMES, arrayOf<String>())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

        val scenario =
            ActivityScenario.launchActivityForResult<PermissionsActivity>(startActivityIntent)

        scenario.onActivity { activity: PermissionsActivity ->
            activity.findViewById<Button>(R.id.allow).callOnClick()
            assertThat(activity.isFinishing()).isTrue()
        }

        assertThat(scenario.getResult().getResultCode()).isEqualTo(Activity.RESULT_OK)
        val returnedIntent = scenario.getResult().getResultData()
        assertThat(returnedIntent.getStringArrayExtra(EXTRA_REQUEST_PERMISSIONS_NAMES)).isEmpty()
        assertThat(returnedIntent.getIntArrayExtra(EXTRA_REQUEST_PERMISSIONS_RESULTS)).isEmpty()
    }

    @Test
    fun sendsOkResult_requestWithPermissions() {
        val permissions = arrayOf(READ_STEPS, READ_HEART_RATE, WRITE_DISTANCE, WRITE_EXERCISE)

        val startActivityIntent =
            Intent.makeMainActivity(
                    ComponentName(
                        getInstrumentation().getContext(), PermissionsActivity::class.java))
                .putExtra(EXTRA_REQUEST_PERMISSIONS_NAMES, permissions)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

        val scenario =
            ActivityScenario.launchActivityForResult<PermissionsActivity>(startActivityIntent)

        scenario.onActivity { activity: PermissionsActivity ->
            activity.findViewById<Button>(R.id.allow).callOnClick()
            assertThat(activity.isFinishing()).isTrue()
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
    fun sendsOkResult_requestWithPermissionsSomeDenied() {
        val permissions = arrayOf(READ_STEPS, READ_HEART_RATE, WRITE_DISTANCE, WRITE_EXERCISE)

        val startActivityIntent =
            Intent.makeMainActivity(
                    ComponentName(
                        getInstrumentation().getContext(), PermissionsActivity::class.java))
                .putExtra(EXTRA_REQUEST_PERMISSIONS_NAMES, permissions)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

        val scenario =
            ActivityScenario.launchActivityForResult<PermissionsActivity>(startActivityIntent)

        scenario.onActivity { activity: PermissionsActivity ->
            val fragment =
                activity.supportFragmentManager.findFragmentById(R.id.permission_content)
                    as PermissionsFragment
            val preferenceCategory =
                fragment.preferenceScreen.findPreference("read_permission_category")
                    as PreferenceCategory?
            val preference = preferenceCategory?.getPreference(0) as SwitchPreference
            preference.onPreferenceChangeListener?.onPreferenceChange(preference, false)
        }

        scenario.onActivity { activity: PermissionsActivity ->
            activity.findViewById<Button>(R.id.allow).callOnClick()
            assertThat(activity.isFinishing).isTrue()
        }

        assertThat(scenario.getResult().getResultCode()).isEqualTo(Activity.RESULT_OK)
        val returnedIntent = scenario.getResult().getResultData()
        assertThat(returnedIntent.getStringArrayExtra(EXTRA_REQUEST_PERMISSIONS_NAMES))
            .isEqualTo(permissions)
        assertThat(returnedIntent.getIntArrayExtra(EXTRA_REQUEST_PERMISSIONS_RESULTS))
            .isEqualTo(
                intArrayOf(
                    PERMISSION_DENIED, PERMISSION_GRANTED, PERMISSION_GRANTED, PERMISSION_GRANTED))
    }
}
