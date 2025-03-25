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

package android.healthconnect.cts.ui.permissions

import android.health.connect.HealthPermissions
import android.healthconnect.cts.lib.ActivityLauncher.launchMainActivity
import android.healthconnect.cts.lib.UiTestUtils.TEST_APP_PACKAGE_NAME
import android.healthconnect.cts.lib.UiTestUtils.clickOnDescAndWaitForNewWindow
import android.healthconnect.cts.lib.UiTestUtils.clickOnTextAndWaitForNewWindow
import android.healthconnect.cts.lib.UiTestUtils.findText
import android.healthconnect.cts.lib.UiTestUtils.findTextAndClick
import android.healthconnect.cts.lib.UiTestUtils.grantPermissionViaPackageManager
import android.healthconnect.cts.lib.UiTestUtils.revokeAllPermissionsViaPackageManager
import android.healthconnect.cts.lib.UiTestUtils.setPermissionsAsUserFixed
import android.healthconnect.cts.lib.UiTestUtils.hasUserFixedPermissions
import android.healthconnect.cts.lib.UiTestUtils.scrollDownToAndFindText
import android.healthconnect.cts.ui.HealthConnectBaseTest
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import com.android.healthfitness.flags.Flags.FLAG_LAUNCH_ONBOARDING_ACTIVITY
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Rule
import org.junit.Test

class ManageHealthPermissionsUITest : HealthConnectBaseTest() {

    @get:Rule
    val mCheckFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    @Test
    fun showsListOfHealthConnectApps() {
        context.launchMainActivity {
            navigateToManagePermissions()
            scrollDownToAndFindText("Health Connect cts test app")
        }
    }

    @Test
    fun showsHelpAndFeedback() {
        context.launchMainActivity {
            navigateToManagePermissions()
            scrollDownToAndFindText("Settings & help")
        }
    }

    @Test
    @RequiresFlagsEnabled(FLAG_LAUNCH_ONBOARDING_ACTIVITY)
    fun onboardingActivity_launchedWhenAppPermissionsDenied() {
        context.launchMainActivity {
            revokeAllPermissionsViaPackageManager(context, TEST_APP_PACKAGE_NAME)
            navigateToManagePermissions()
            scrollDownToAndFindText("Health Connect cts test app")
            clickOnTextAndWaitForNewWindow("Health Connect cts test app")
            verifyOnboardingActivityLaunched()
        }
    }

    @Test
    @RequiresFlagsEnabled(FLAG_LAUNCH_ONBOARDING_ACTIVITY)
    fun onboardingActivity_resetUserFixedPermissionFlagState() {
        context.launchMainActivity {
            revokeAllPermissionsViaPackageManager(context, TEST_APP_PACKAGE_NAME)
            setPermissionsAsUserFixed(context, TEST_APP_PACKAGE_NAME, true)
            assertThat(hasUserFixedPermissions(context, TEST_APP_PACKAGE_NAME)).isTrue()
            navigateToManagePermissions()
            scrollDownToAndFindText("Health Connect cts test app")
            clickOnTextAndWaitForNewWindow("Health Connect cts test app")
            verifyOnboardingActivityLaunched()
            assertThat(hasUserFixedPermissions(context, TEST_APP_PACKAGE_NAME)).isFalse()
        }
    }

    @Test
    fun revokeAllPermissions_showsRevokeAllConnectedAppsPermission() {
        grantPermissionViaPackageManager(
            context,
            TEST_APP_PACKAGE_NAME,
            HealthPermissions.READ_HEIGHT,
        )

        context.launchMainActivity {
            navigateToManagePermissions()

            scrollDownToAndFindText("Remove access for all apps")
            findTextAndClick("Remove access for all apps")
            findText("Remove all")
            // We cannot actually revoke all the permissions because that would also
            // revoke the test app permissions and lead to a test crash
        }
    }

    @Test
    fun showSearchOption() {
        context.launchMainActivity {
            navigateToManagePermissions()
            clickOnDescAndWaitForNewWindow("Search apps")
            findText("Search apps")
        }
    }

    @After
    fun tearDown() {
        grantPermissionViaPackageManager(
            context,
            TEST_APP_PACKAGE_NAME,
            HealthPermissions.READ_HEIGHT,
        )
        grantPermissionViaPackageManager(
            context,
            TEST_APP_PACKAGE_NAME,
            HealthPermissions.WRITE_HEIGHT,
        )
        grantPermissionViaPackageManager(
            context,
            TEST_APP_PACKAGE_NAME,
            HealthPermissions.WRITE_BODY_FAT,
        )
    }

    private fun verifyOnboardingActivityLaunched() {
        // We just export the MainActivity as the onboarding activity. Sufficient to simply
        // verify that the activity successfully launches.
        findText("MainActivity")
    }

    private fun navigateToManagePermissions() {
        scrollDownToAndFindText("App permissions")
        clickOnTextAndWaitForNewWindow("App permissions")
        scrollDownToAndFindText("Allowed access")
    }
}
