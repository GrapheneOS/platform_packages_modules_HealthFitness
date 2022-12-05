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
package com.android.healthconnect.controller.tests.permissions.connectedapps.settings

import android.healthconnect.HealthConnectManager
import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.matcher.ViewMatchers
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.permissions.connectedapps.ConnectedAppMetadata
import com.android.healthconnect.controller.permissions.connectedapps.ConnectedAppStatus.ALLOWED
import com.android.healthconnect.controller.permissions.connectedapps.ConnectedAppStatus.DENIED
import com.android.healthconnect.controller.permissions.connectedapps.ConnectedAppStatus.INACTIVE
import com.android.healthconnect.controller.permissions.connectedapps.ConnectedAppsViewModel
import com.android.healthconnect.controller.permissions.connectedapps.settings.SettingsManagePermissionFragment
import com.android.healthconnect.controller.shared.AppMetadata
import com.android.healthconnect.controller.tests.utils.TEST_APP
import com.android.healthconnect.controller.tests.utils.TEST_APP_2
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME_2
import com.android.healthconnect.controller.tests.utils.launchFragment
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito

@HiltAndroidTest
class SettingsManagePermissionFragmentTest {
    @get:Rule val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var manager: HealthConnectManager

    @BindValue
    val viewModel: ConnectedAppsViewModel = Mockito.mock(ConnectedAppsViewModel::class.java)

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun test_displaysSections() {
        Mockito.`when`(viewModel.connectedApps).then { MutableLiveData(listOf<AppMetadata>()) }

        launchFragment<SettingsManagePermissionFragment>(Bundle())

        Espresso.onView(ViewMatchers.withText("Allowed access"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withText("No apps allowed"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withText("Not allowed access"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withText("No apps denied"))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun test_allowedApps() {
        val connectApp = listOf(ConnectedAppMetadata(TEST_APP, status = ALLOWED))
        Mockito.`when`(viewModel.connectedApps).then { MutableLiveData(connectApp) }

        launchFragment<SettingsManagePermissionFragment>(Bundle())

        Espresso.onView(ViewMatchers.withText(TEST_APP_NAME))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withText("No apps allowed")).check(doesNotExist())
    }

    @Test
    fun test_deniedApps() {
        val connectApp = listOf(ConnectedAppMetadata(TEST_APP, status = DENIED))
        Mockito.`when`(viewModel.connectedApps).then { MutableLiveData(connectApp) }

        launchFragment<SettingsManagePermissionFragment>(Bundle())

        Espresso.onView(ViewMatchers.withText(TEST_APP_NAME))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withText("No apps denied")).check(doesNotExist())
    }

    @Test
    fun test_inactiveApp_doesNotShowInactiveApps() {
        val connectApp = listOf(ConnectedAppMetadata(TEST_APP, status = INACTIVE))
        Mockito.`when`(viewModel.connectedApps).then { MutableLiveData(connectApp) }

        launchFragment<SettingsManagePermissionFragment>(Bundle())
        Thread.sleep(10000)
        Espresso.onView(ViewMatchers.withText(TEST_APP_NAME)).check(doesNotExist())
        Espresso.onView(ViewMatchers.withText(R.string.inactive_apps)).check(doesNotExist())
    }

    @Test
    fun test_all() {
        val connectApp =
            listOf(
                ConnectedAppMetadata(TEST_APP, status = DENIED),
                ConnectedAppMetadata(TEST_APP_2, status = ALLOWED))
        Mockito.`when`(viewModel.connectedApps).then { MutableLiveData(connectApp) }

        launchFragment<SettingsManagePermissionFragment>(Bundle())

        Espresso.onView(ViewMatchers.withText(TEST_APP_NAME))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withText(TEST_APP_NAME_2))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withText("No apps allowed")).check(doesNotExist())
        Espresso.onView(ViewMatchers.withText("No apps denied")).check(doesNotExist())
    }
}
