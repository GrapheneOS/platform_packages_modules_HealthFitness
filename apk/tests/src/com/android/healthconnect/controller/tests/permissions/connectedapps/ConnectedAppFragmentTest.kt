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
package com.android.healthconnect.controller.tests.permissions.connectedapps

import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceCategory
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.android.healthconnect.controller.permissions.connectedapps.AppPermissionViewModel
import com.android.healthconnect.controller.permissions.connectedapps.ConnectedAppFragment
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.permissions.data.HealthPermissionType
import com.android.healthconnect.controller.permissions.data.PermissionsAccessType
import com.android.healthconnect.controller.tests.TestActivity
import com.android.healthconnect.controller.tests.utils.launchFragment
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito

@HiltAndroidTest
class ConnectedAppFragmentTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @BindValue
    val viewModel: AppPermissionViewModel = Mockito.mock(AppPermissionViewModel::class.java)

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun test_noPermissions() {
        Mockito.`when`(viewModel.grantedPermissions).then {
            MutableLiveData(listOf<HealthPermission>())
        }

        val scenario = launchFragment({ ConnectedAppFragment.newInstance("package.name") })

        scenario.onActivity { activity: TestActivity ->
            val fragment =
                activity.supportFragmentManager.findFragmentById(android.R.id.content)
                    as ConnectedAppFragment
            val readCategory =
                fragment.preferenceScreen.findPreference("read_permission_category")
                    as PreferenceCategory?
            val writeCategory =
                fragment.preferenceScreen.findPreference("write_permission_category")
                    as PreferenceCategory?
            assertThat(readCategory?.preferenceCount).isEqualTo(0)
            assertThat(writeCategory?.preferenceCount).isEqualTo(0)
        }
    }

    @Test
    fun test_readPermission() {
        Mockito.`when`(viewModel.grantedPermissions).then {
            MutableLiveData(
                listOf(HealthPermission(HealthPermissionType.DISTANCE, PermissionsAccessType.READ)))
        }

        val scenario = launchFragment({ ConnectedAppFragment.newInstance("package.name") })

        scenario.onActivity { activity: TestActivity ->
            val fragment =
                activity.supportFragmentManager.findFragmentById(android.R.id.content)
                    as ConnectedAppFragment
            val readCategory =
                fragment.preferenceScreen.findPreference("read_permission_category")
                    as PreferenceCategory?
            val writeCategory =
                fragment.preferenceScreen.findPreference("write_permission_category")
                    as PreferenceCategory?
            assertThat(readCategory?.preferenceCount).isEqualTo(1)
            assertThat(writeCategory?.preferenceCount).isEqualTo(0)
        }
        onView(withText("Distance")).check(matches(isDisplayed()))
    }

    @Test
    fun test_writePermission() {
        Mockito.`when`(viewModel.grantedPermissions).then {
            MutableLiveData(
                listOf(
                    HealthPermission(HealthPermissionType.EXERCISE, PermissionsAccessType.WRITE)))
        }

        val scenario = launchFragment({ ConnectedAppFragment.newInstance("package.name") })

        scenario.onActivity { activity: TestActivity ->
            val fragment =
                activity.supportFragmentManager.findFragmentById(android.R.id.content)
                    as ConnectedAppFragment
            val readCategory =
                fragment.preferenceScreen.findPreference("read_permission_category")
                    as PreferenceCategory?
            val writeCategory =
                fragment.preferenceScreen.findPreference("write_permission_category")
                    as PreferenceCategory?
            assertThat(readCategory?.preferenceCount).isEqualTo(0)
            assertThat(writeCategory?.preferenceCount).isEqualTo(1)
        }
        onView(withText("Exercise")).check(matches(isDisplayed()))
    }

    @Test
    fun test_readAndWritePermission() {
        Mockito.`when`(viewModel.grantedPermissions).then {
            MutableLiveData(
                listOf(
                    HealthPermission(HealthPermissionType.DISTANCE, PermissionsAccessType.READ),
                    HealthPermission(HealthPermissionType.EXERCISE, PermissionsAccessType.WRITE)))
        }

        val scenario = launchFragment({ ConnectedAppFragment.newInstance("package.name") })

        scenario.onActivity { activity: TestActivity ->
            val fragment =
                activity.supportFragmentManager.findFragmentById(android.R.id.content)
                    as ConnectedAppFragment
            val readCategory =
                fragment.preferenceScreen.findPreference("read_permission_category")
                    as PreferenceCategory?
            val writeCategory =
                fragment.preferenceScreen.findPreference("write_permission_category")
                    as PreferenceCategory?
            assertThat(readCategory?.preferenceCount).isEqualTo(1)
            assertThat(writeCategory?.preferenceCount).isEqualTo(1)
        }
        onView(withText("Exercise")).check(matches(isDisplayed()))
        onView(withText("Distance")).check(matches(isDisplayed()))
    }
}
