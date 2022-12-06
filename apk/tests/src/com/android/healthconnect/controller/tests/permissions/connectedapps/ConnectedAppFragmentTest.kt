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
package com.android.healthconnect.controller.tests.permissions.connectedapps

import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceCategory
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.permissions.connectedApps.HealthPermissionStatus
import com.android.healthconnect.controller.permissions.connectedapps.AppPermissionViewModel
import com.android.healthconnect.controller.permissions.connectedapps.ConnectedAppFragment
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.permissions.data.HealthPermissionType.DISTANCE
import com.android.healthconnect.controller.permissions.data.HealthPermissionType.EXERCISE
import com.android.healthconnect.controller.permissions.data.PermissionsAccessType.READ
import com.android.healthconnect.controller.permissions.data.PermissionsAccessType.WRITE
import com.android.healthconnect.controller.shared.AppMetadata
import com.android.healthconnect.controller.tests.TestActivity
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.launchFragment
import com.android.settingslib.widget.MainSwitchPreference
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.*

@HiltAndroidTest
class ConnectedAppFragmentTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @BindValue val viewModel: AppPermissionViewModel = mock(AppPermissionViewModel::class.java)

    @Before
    fun setup() {
        hiltRule.inject()
        val context = InstrumentationRegistry.getInstrumentation().context
        `when`(viewModel.allAppPermissionsGranted).then { MutableLiveData(false) }
        `when`(viewModel.atLeastOnePermissionGranted).then { MutableLiveData(false) }
        `when`(viewModel.appInfo).then {
            MutableLiveData(
                AppMetadata(
                    TEST_APP_PACKAGE_NAME,
                    TEST_APP_NAME,
                    context.getDrawable(R.drawable.health_connect_logo)))
        }
    }

    @Test
    fun test_noPermissions() {
        `when`(viewModel.appPermissions).then { MutableLiveData(listOf<HealthPermissionStatus>()) }

        val scenario = launchFragment({ ConnectedAppFragment.newInstance(TEST_APP_PACKAGE_NAME) })

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
        val permission = HealthPermission(DISTANCE, READ)
        `when`(viewModel.appPermissions).then {
            MutableLiveData(
                listOf(HealthPermissionStatus(healthPermission = permission, isGranted = true)))
        }

        val scenario = launchFragment({ ConnectedAppFragment.newInstance(TEST_APP_PACKAGE_NAME) })

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
        val permission = HealthPermission(EXERCISE, WRITE)
        `when`(viewModel.appPermissions).then {
            MutableLiveData(
                listOf(HealthPermissionStatus(healthPermission = permission, isGranted = true)))
        }

        val scenario = launchFragment({ ConnectedAppFragment.newInstance(TEST_APP_PACKAGE_NAME) })

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
        val writePermission =
            HealthPermissionStatus(
                healthPermission = HealthPermission(EXERCISE, WRITE), isGranted = true)
        val readPermission =
            HealthPermissionStatus(
                healthPermission = HealthPermission(DISTANCE, READ), isGranted = false)
        `when`(viewModel.appPermissions).then {
            MutableLiveData(listOf(writePermission, readPermission))
        }

        val scenario = launchFragment({ ConnectedAppFragment.newInstance(TEST_APP_PACKAGE_NAME) })

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

    @Test
    fun test_allowAllToggleOn_whenAllPermissionsOn() {
        val writePermission =
            HealthPermissionStatus(
                healthPermission = HealthPermission(EXERCISE, WRITE), isGranted = true)
        val readPermission =
            HealthPermissionStatus(
                healthPermission = HealthPermission(DISTANCE, READ), isGranted = true)
        `when`(viewModel.appPermissions).then {
            MutableLiveData(listOf(writePermission, readPermission))
        }
        `when`(viewModel.allAppPermissionsGranted).then { MutableLiveData(true) }

        val scenario = launchFragment({ ConnectedAppFragment.newInstance(TEST_APP_PACKAGE_NAME) })

        scenario.onActivity { activity: TestActivity ->
            val fragment =
                activity.supportFragmentManager.findFragmentById(android.R.id.content)
                    as ConnectedAppFragment
            val mainSwitchPreference =
                fragment.preferenceScreen.findPreference("allow_all_preference")
                    as MainSwitchPreference?

            assertThat(mainSwitchPreference?.isChecked).isTrue()
        }
    }

    @Test
    fun test_allowAllToggleOff_whenAtLeastOnePermissionOff() {
        val writePermission =
            HealthPermissionStatus(
                healthPermission = HealthPermission(EXERCISE, WRITE), isGranted = true)
        val readPermission =
            HealthPermissionStatus(
                healthPermission = HealthPermission(DISTANCE, READ), isGranted = false)
        `when`(viewModel.appPermissions).then {
            MutableLiveData(listOf(writePermission, readPermission))
        }
        `when`(viewModel.allAppPermissionsGranted).then { MutableLiveData(false) }

        val scenario = launchFragment({ ConnectedAppFragment.newInstance(TEST_APP_PACKAGE_NAME) })

        scenario.onActivity { activity: TestActivity ->
            val fragment =
                activity.supportFragmentManager.findFragmentById(android.R.id.content)
                    as ConnectedAppFragment

            val mainSwitchPreference =
                fragment.preferenceScreen.findPreference("allow_all_preference")
                    as MainSwitchPreference?

            assertThat(mainSwitchPreference?.isChecked).isFalse()
        }
    }

    @Test
    fun test_footerIsDisplayed() {
        val permission = HealthPermission(DISTANCE, READ)
        `when`(viewModel.appPermissions).then {
            MutableLiveData(
                listOf(HealthPermissionStatus(healthPermission = permission, isGranted = true)))
        }

        launchFragment({ ConnectedAppFragment.newInstance(TEST_APP_PACKAGE_NAME) })

        // TODO (b/261395536) update with the time the first permission was granted
        onView(
                withText(
                    "To manage other Android permissions this app can " +
                        "access, go to Settings > Apps" +
                        "\n\n" +
                        "Data you share with $TEST_APP_NAME is covered by their privacy policy"))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
        onView(withText("Read privacy policy")).perform(scrollTo()).check(matches(isDisplayed()))
    }
}
