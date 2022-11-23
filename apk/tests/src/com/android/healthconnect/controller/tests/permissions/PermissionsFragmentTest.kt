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
package com.android.healthconnect.controller.tests.permissions

import androidx.preference.PreferenceCategory
import androidx.preference.SwitchPreference
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.permissions.PermissionsFragment
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.permissions.data.HealthPermissionType
import com.android.healthconnect.controller.permissions.data.PermissionsAccessType
import com.android.healthconnect.controller.tests.TestActivity
import com.android.healthconnect.controller.tests.utils.launchFragment
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class PermissionsFragmentTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun test_displaysCategories() {
        launchFragment({ PermissionsFragment.newInstance(mapOf()) })

        onView(withText(R.string.read_permission_category)).check(matches(isDisplayed()))
        onView(withText(R.string.write_permission_category)).check(matches(isDisplayed()))
    }

    @Test
    fun test_displaysReadPermissions() {
        launchFragment({
            PermissionsFragment.newInstance(
                mapOf(
                    HealthPermission(HealthPermissionType.STEPS, PermissionsAccessType.READ) to
                        true,
                    HealthPermission(HealthPermissionType.HEART_RATE, PermissionsAccessType.READ) to
                        true))
        })

        onView(withText("Steps")).check(matches(isDisplayed()))
        onView(withText("Heart rate")).check(matches(isDisplayed()))
    }

    @Test
    fun test_displaysWritePermissions() {
        launchFragment({
            PermissionsFragment.newInstance(
                mapOf(
                    HealthPermission(HealthPermissionType.DISTANCE, PermissionsAccessType.WRITE) to
                        true,
                    HealthPermission(HealthPermissionType.EXERCISE, PermissionsAccessType.WRITE) to
                        true))
        })

        onView(withText("Distance")).check(matches(isDisplayed()))
        onView(withText("Exercise")).check(matches(isDisplayed()))
    }

    @Test
    fun test_returnsGrants() {
        val activityScenario =
            launchFragment({
                PermissionsFragment.newInstance(
                    mapOf(
                        HealthPermission(
                            HealthPermissionType.DISTANCE, PermissionsAccessType.READ) to true,
                        HealthPermission(
                            HealthPermissionType.EXERCISE, PermissionsAccessType.WRITE) to true))
            })

        activityScenario.onActivity { activity: TestActivity ->
            assertThat(
                    (activity.supportFragmentManager.findFragmentById(android.R.id.content)
                            as PermissionsFragment)
                        .getPermissionAssignments())
                .isEqualTo(
                    hashMapOf(
                        HealthPermission(
                            HealthPermissionType.DISTANCE, PermissionsAccessType.READ) to true,
                        HealthPermission(
                            HealthPermissionType.EXERCISE, PermissionsAccessType.WRITE) to true))
        }
    }

    @Test
    fun test_togglesPermissions() {
        val activityScenario =
            launchFragment({
                PermissionsFragment.newInstance(
                    mapOf(
                        HealthPermission(
                            HealthPermissionType.DISTANCE, PermissionsAccessType.READ) to true,
                        HealthPermission(
                            HealthPermissionType.EXERCISE, PermissionsAccessType.WRITE) to true))
            })

        activityScenario.onActivity { activity: TestActivity ->
            val fragment =
                activity.supportFragmentManager.findFragmentById(android.R.id.content)
                    as PermissionsFragment
            val preferenceCategory =
                fragment.preferenceScreen.findPreference("read_permission_category")
                    as PreferenceCategory?
            val preference = preferenceCategory?.getPreference(0) as SwitchPreference
            preference.onPreferenceChangeListener?.onPreferenceChange(preference, false)

            assertThat(fragment.getPermissionAssignments())
                .isEqualTo(
                    hashMapOf(
                        HealthPermission(
                            HealthPermissionType.DISTANCE, PermissionsAccessType.READ) to false,
                        HealthPermission(
                            HealthPermissionType.EXERCISE, PermissionsAccessType.WRITE) to true))
        }
    }
}
