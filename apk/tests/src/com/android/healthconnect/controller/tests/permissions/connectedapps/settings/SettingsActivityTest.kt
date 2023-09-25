/**
 * Copyright (C) 2023 The Android Open Source Project
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
package com.android.healthconnect.controller.tests.permissions.connectedapps.settings

import android.content.Context
import android.content.Intent
import android.content.Intent.EXTRA_PACKAGE_NAME
import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.onboarding.OnboardingActivity
import com.android.healthconnect.controller.permissions.api.HealthPermissionManager
import com.android.healthconnect.controller.permissions.app.AppPermissionViewModel
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.permissions.data.HealthPermissionType
import com.android.healthconnect.controller.permissions.data.PermissionsAccessType
import com.android.healthconnect.controller.permissions.shared.SettingsActivity
import com.android.healthconnect.controller.service.HealthPermissionManagerModule
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.di.FakeHealthPermissionManager
import com.android.healthconnect.controller.tests.utils.setLocale
import com.android.healthconnect.controller.tests.utils.whenever
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import junit.framework.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import java.util.TimeZone

@UninstallModules(HealthPermissionManagerModule::class)
@HiltAndroidTest
class SettingsActivityTest {

    private lateinit var context: Context

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @BindValue
    val viewModel: AppPermissionViewModel = Mockito.mock(AppPermissionViewModel::class.java)
    @BindValue val permissionManager: HealthPermissionManager = FakeHealthPermissionManager()

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().context
        context.setLocale(Locale.US)
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("UTC")))
        hiltRule.inject()
        (permissionManager as FakeHealthPermissionManager).setGrantedPermissionsForTest(
            TEST_APP_PACKAGE_NAME, listOf()
        )
        whenever(viewModel.grantedPermissions).then {
            MutableLiveData<Set<HealthPermission>>(
                emptySet()
            )
        }

        val sharedPreference =
            context.getSharedPreferences(OnboardingActivity.USER_ACTIVITY_TRACKER, Context.MODE_PRIVATE)
        val editor = sharedPreference.edit()
        editor.putBoolean(OnboardingActivity.ONBOARDING_SHOWN_PREF_KEY, true)
        editor.apply()

        // Needed for the fragment
        whenever(viewModel.revokeAllPermissionsState).then {
            MutableLiveData(AppPermissionViewModel.RevokeAllState.NotStarted)
        }
        whenever(viewModel.allAppPermissionsGranted).then { MutableLiveData(false) }
        whenever(viewModel.atLeastOnePermissionGranted).then { MutableLiveData(false) }
        val accessDate = Instant.parse("2022-10-20T18:40:13.00Z")
        whenever(viewModel.loadAccessDate(Mockito.anyString())).thenReturn(accessDate)

        whenever(viewModel.appInfo).then {
            MutableLiveData(
                AppMetadata(
                    TEST_APP_PACKAGE_NAME,
                    TEST_APP_NAME,
                    context.getDrawable(R.drawable.health_connect_logo)
                )
            )

        }
        val writePermission =
            HealthPermission(HealthPermissionType.EXERCISE, PermissionsAccessType.WRITE)
        val readPermission =
            HealthPermission(HealthPermissionType.DISTANCE, PermissionsAccessType.READ)
        whenever(viewModel.appPermissions).then {
            MutableLiveData(listOf(writePermission, readPermission))
        }
        whenever(viewModel.grantedPermissions).then { MutableLiveData(setOf(writePermission)) }
    }

    @Test
    fun settingsActivityFinishes_whenNoPackageName_ifNoRationaleIntent_andNoPermissions() {
        val intent = Intent(context, SettingsActivity::class.java)
        val bundle = Bundle()
        bundle.putString(EXTRA_PACKAGE_NAME, TEST_APP_PACKAGE_NAME)
        intent.putExtras(bundle)

        whenever(viewModel.isPackageSupported(TEST_APP_PACKAGE_NAME)).then { false }
        whenever(viewModel.shouldNavigateToFragment).then {
            MutableLiveData(
                false
            )
        }

        ActivityScenario.launch<SettingsActivity>(intent).use { scenario ->
            Thread.sleep(4_000) // Need to wait for Activity to close before checking state
            Assert.assertEquals(Lifecycle.State.DESTROYED, scenario.state)
        }
    }

    @Test
    fun settingsActivity_navigatesToAppPermissionsFragment_ifRationaleIntentDeclared() {
        val intent = Intent(context, SettingsActivity::class.java)
        val bundle = Bundle()
        bundle.putString(EXTRA_PACKAGE_NAME, TEST_APP_PACKAGE_NAME)
        intent.putExtras(bundle)

        whenever(viewModel.isPackageSupported(TEST_APP_PACKAGE_NAME)).then { true }
        whenever(viewModel.shouldNavigateToFragment).then {
            MutableLiveData(
                true
            )
        }

        ActivityScenario.launch<SettingsActivity>(intent).use { scenario ->
            onView(withText(TEST_APP_NAME)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun settingsActivity_navigatesToAppPermissionsFragment_ifNoRationaleIntentDeclared_andGrantedPermission() {
        val intent = Intent(context, SettingsActivity::class.java)
        val bundle = Bundle()
        bundle.putString(EXTRA_PACKAGE_NAME, TEST_APP_PACKAGE_NAME)
        intent.putExtras(bundle)

        whenever(viewModel.isPackageSupported(TEST_APP_PACKAGE_NAME)).then { false }
        whenever(viewModel.shouldNavigateToFragment).then {
            MutableLiveData(
                true
            )
        }

        ActivityScenario.launch<SettingsActivity>(intent).use { scenario ->
            onView(withText(TEST_APP_NAME)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun settingsActivity_navigatesToManagePermissionsFragment_ifNoPackageName() {
        val intent = Intent(context, SettingsActivity::class.java)

        ActivityScenario.launch<SettingsActivity>(intent).use { scenario ->
            onView(withText("Apps with this permission can read and write your" +
                    " health and fitness data.")).check(matches(isDisplayed()))
        }
    }

}