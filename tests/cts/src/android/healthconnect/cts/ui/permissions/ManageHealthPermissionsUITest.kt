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

import android.content.Context
import android.content.pm.PackageManager.FLAG_PERMISSION_USER_SENSITIVE_WHEN_DENIED
import android.content.pm.PackageManager.FLAG_PERMISSION_USER_SENSITIVE_WHEN_GRANTED
import android.health.connect.HealthPermissions
import android.healthconnect.cts.lib.ActivityLauncher.launchMainActivity
import android.healthconnect.cts.lib.ActivityLauncher.launchManageHealthPermissionActivity
import android.healthconnect.cts.lib.UiTestUtils.TEST_APP_2_NAME
import android.healthconnect.cts.lib.UiTestUtils.TEST_APP_2_PACKAGE_NAME
import android.healthconnect.cts.lib.UiTestUtils.TEST_APP_NAME
import android.healthconnect.cts.lib.UiTestUtils.TEST_APP_PACKAGE_NAME
import android.healthconnect.cts.lib.UiTestUtils.clickOnDescAndWaitForNewWindow
import android.healthconnect.cts.lib.UiTestUtils.clickOnTextAndWaitForNewWindow
import android.healthconnect.cts.lib.UiTestUtils.findActionButtonAndClick
import android.healthconnect.cts.lib.UiTestUtils.findText
import android.healthconnect.cts.lib.UiTestUtils.findTextAndClick
import android.healthconnect.cts.lib.UiTestUtils.grantPermissionViaPackageManager
import android.healthconnect.cts.lib.UiTestUtils.hasUserFixedPermissions
import android.healthconnect.cts.lib.UiTestUtils.revokeAllPermissionsViaPackageManager
import android.healthconnect.cts.lib.UiTestUtils.scrollDownToAndFindText
import android.healthconnect.cts.lib.UiTestUtils.scrollToEnd
import android.healthconnect.cts.lib.UiTestUtils.scrollUpToAndFindText
import android.healthconnect.cts.lib.UiTestUtils.setPermissionsAsUserFixed
import android.healthconnect.cts.lib.UiTestUtils.verifyTextNotFound
import android.healthconnect.cts.ui.HealthConnectBaseTest
import android.os.Process
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import com.android.compatibility.common.util.SystemUtil.runWithShellPermissionIdentity
import com.android.healthfitness.flags.Flags.FLAG_LAUNCH_ONBOARDING_ACTIVITY
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Rule
import org.junit.Test

class ManageHealthPermissionsUITest : HealthConnectBaseTest() {

    @get:Rule val mCheckFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

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
    fun showsShowHideSystemAppsButton() {
        // Update TEST_APP_2 all health permission flags so that it can be considered a system app.
        clearPermissionFlag(
            context,
            TEST_APP_2_PACKAGE_NAME,
            listOf(
                HealthPermissions.READ_HEIGHT,
                HealthPermissions.WRITE_HEIGHT,
                HealthPermissions.READ_HEALTH_DATA_HISTORY,
                HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND,
                HealthPermissions.READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES,
                HealthPermissions.READ_MEDICAL_DATA_CONDITIONS,
                HealthPermissions.WRITE_MEDICAL_DATA,
            ),
            FLAG_PERMISSION_USER_SENSITIVE_WHEN_DENIED,
        )
        clearPermissionFlag(
            context,
            TEST_APP_2_PACKAGE_NAME,
            listOf(
                HealthPermissions.READ_HEIGHT,
                HealthPermissions.WRITE_HEIGHT,
                HealthPermissions.READ_HEALTH_DATA_HISTORY,
                HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND,
                HealthPermissions.READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES,
                HealthPermissions.READ_MEDICAL_DATA_CONDITIONS,
                HealthPermissions.WRITE_MEDICAL_DATA,
            ),
            FLAG_PERMISSION_USER_SENSITIVE_WHEN_GRANTED,
        )

        context.launchManageHealthPermissionActivity {
            // By default, system apps are hidden
            findText(TEST_APP_NAME)
            verifyTextNotFound(TEST_APP_2_NAME)
            scrollToEnd()
            verifyTextNotFound(TEST_APP_2_NAME)

            // Click "Show system". Verify both system and non-system apps are shown.
            findActionButtonAndClick()
            findTextAndClick("Show system")
            scrollUpToAndFindText(TEST_APP_NAME)
            scrollDownToAndFindText(TEST_APP_2_NAME)

            // Click "Hide system". Verify system apps are hidden, non-system apps still display.
            findActionButtonAndClick()
            findTextAndClick("Hide system")
            scrollUpToAndFindText(TEST_APP_NAME)
            verifyTextNotFound(TEST_APP_2_NAME)
            scrollToEnd()
            verifyTextNotFound(TEST_APP_2_NAME)
        }
        // Reset TEST_APP_2 all health permission flags so that it can be considered a non-system
        // app.
        setPermissionFlag(
            context,
            TEST_APP_2_PACKAGE_NAME,
            listOf(
                HealthPermissions.READ_HEIGHT,
                HealthPermissions.WRITE_HEIGHT,
                HealthPermissions.READ_HEALTH_DATA_HISTORY,
                HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND,
                HealthPermissions.READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES,
                HealthPermissions.READ_MEDICAL_DATA_CONDITIONS,
                HealthPermissions.WRITE_MEDICAL_DATA,
            ),
            FLAG_PERMISSION_USER_SENSITIVE_WHEN_DENIED,
        )
        setPermissionFlag(
            context,
            TEST_APP_2_PACKAGE_NAME,
            listOf(
                HealthPermissions.READ_HEIGHT,
                HealthPermissions.WRITE_HEIGHT,
                HealthPermissions.READ_HEALTH_DATA_HISTORY,
                HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND,
                HealthPermissions.READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES,
                HealthPermissions.READ_MEDICAL_DATA_CONDITIONS,
                HealthPermissions.WRITE_MEDICAL_DATA,
            ),
            FLAG_PERMISSION_USER_SENSITIVE_WHEN_GRANTED,
        )
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

    fun clearPermissionFlag(context: Context, app: String, permissions: List<String>, flag: Int) {
        runWithShellPermissionIdentity {
            val packageManager = context.packageManager

            permissions.forEach { permission ->
                packageManager.updatePermissionFlags(
                    permission,
                    app,
                    flag,
                    0,
                    Process.myUserHandle(),
                )
            }
        }
    }

    fun setPermissionFlag(context: Context, app: String, permissions: List<String>, flag: Int) {
        runWithShellPermissionIdentity {
            val packageManager = context.packageManager

            permissions.forEach { permission ->
                packageManager.updatePermissionFlags(
                    permission,
                    app,
                    flag,
                    flag,
                    Process.myUserHandle(),
                )
            }
        }
    }
}
