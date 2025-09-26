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

import android.content.pm.PackageManager
import android.health.connect.HealthPermissions.READ_HEIGHT
import android.health.connect.HealthPermissions.READ_MINDFULNESS
import android.health.connect.HealthPermissions.WRITE_BODY_FAT
import android.health.connect.HealthPermissions.WRITE_HEIGHT
import android.health.connect.HealthPermissions.WRITE_STEPS
import android.healthconnect.cts.ui.HealthConnectBaseTest
import android.healthconnect.testing.cts.ui.ActivityLauncher.launchMainActivity
import android.healthconnect.testing.cts.ui.UiTestUtils.TEST_APP_PACKAGE_NAME
import android.healthconnect.testing.cts.ui.UiTestUtils.clickOnDescAndWaitForNewWindow
import android.healthconnect.testing.cts.ui.UiTestUtils.clickOnText
import android.healthconnect.testing.cts.ui.UiTestUtils.clickOnTextAndWaitForNewWindow
import android.healthconnect.testing.cts.ui.UiTestUtils.findObject
import android.healthconnect.testing.cts.ui.UiTestUtils.findText
import android.healthconnect.testing.cts.ui.UiTestUtils.findTextAndClick
import android.healthconnect.testing.cts.ui.UiTestUtils.grantPermissionViaPackageManager
import android.healthconnect.testing.cts.ui.UiTestUtils.navigateBackToHomeScreen
import android.healthconnect.testing.cts.ui.UiTestUtils.revokePermissionViaPackageManager
import android.healthconnect.testing.cts.ui.UiTestUtils.scrollDownToAndFindText
import android.platform.test.annotations.RequiresFlagsDisabled
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import androidx.test.uiautomator.By
import com.android.healthfitness.flags.Flags
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Rule
import org.junit.Test

class ManageAppHealthPermissionUITest : HealthConnectBaseTest() {

    @get:Rule val mCheckFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    @Test
    fun showDeclaredPermissions() {
        context.launchMainActivity {
            navigateToManageAppPermissions()

            scrollDownToAndFindText("Height")
        }
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_PERMISSIONS_GROUPING_FITNESS_APP_SCREEN)
    fun grantPermission_updatesAppPermissions() {
        revokePermissionViaPackageManager(context, TEST_APP_PACKAGE_NAME, WRITE_BODY_FAT)
        context.launchMainActivity {
            navigateToManageAppPermissions()

            scrollDownToAndFindText("Body fat")
            findTextAndClick("Body fat")
            clickOnDescAndWaitForNewWindow("Navigate up")

            assertPermGrantedForApp(TEST_APP_PACKAGE_NAME, WRITE_BODY_FAT)
        }
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_PERMISSIONS_GROUPING_FITNESS_APP_SCREEN)
    fun revokePermission_updatesAppPermissions() {
        grantPermissionViaPackageManager(context, TEST_APP_PACKAGE_NAME, WRITE_BODY_FAT)
        context.launchMainActivity {
            navigateToManageAppPermissions()
            assertPermGrantedForApp(TEST_APP_PACKAGE_NAME, WRITE_BODY_FAT)

            scrollDownToAndFindText("Body fat")
            findTextAndClick("Body fat")
            clickOnDescAndWaitForNewWindow("Navigate up")

            assertPermNotGrantedForApp(TEST_APP_PACKAGE_NAME, WRITE_BODY_FAT)
        }
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_PERMISSIONS_GROUPING_FITNESS_APP_SCREEN)
    fun whenGroupedPermissionsEnabled_grantPermission_updatesAppPermissions() {
        revokePermissionViaPackageManager(context, TEST_APP_PACKAGE_NAME, WRITE_BODY_FAT)
        revokePermissionViaPackageManager(context, TEST_APP_PACKAGE_NAME, WRITE_HEIGHT)
        context.launchMainActivity {
            navigateToManageAppPermissions()

            scrollDownToAndFindText("Body measurements (2)")
            findTextAndClick("Body measurements (2)")
            scrollDownToAndFindText("Body fat")
            findTextAndClick("Body fat")
            clickOnDescAndWaitForNewWindow("Navigate up")

            assertPermGrantedForApp(TEST_APP_PACKAGE_NAME, WRITE_BODY_FAT)
            assertPermNotGrantedForApp(TEST_APP_PACKAGE_NAME, WRITE_HEIGHT)
        }
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_PERMISSIONS_GROUPING_FITNESS_APP_SCREEN)
    fun whenGroupedPermissionsEnabled_revokePermission_updatesAppPermissions() {
        grantPermissionViaPackageManager(context, TEST_APP_PACKAGE_NAME, WRITE_BODY_FAT)
        grantPermissionViaPackageManager(context, TEST_APP_PACKAGE_NAME, WRITE_HEIGHT)
        context.launchMainActivity {
            navigateToManageAppPermissions()
            assertPermGrantedForApp(TEST_APP_PACKAGE_NAME, WRITE_BODY_FAT)

            scrollDownToAndFindText("Body measurements (2)")
            findTextAndClick("Body measurements (2)")
            scrollDownToAndFindText("Body fat")
            findTextAndClick("Body fat")
            clickOnDescAndWaitForNewWindow("Navigate up")

            assertPermNotGrantedForApp(TEST_APP_PACKAGE_NAME, WRITE_BODY_FAT)
            assertPermGrantedForApp(TEST_APP_PACKAGE_NAME, WRITE_HEIGHT)
        }
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_PERMISSIONS_GROUPING_FITNESS_APP_SCREEN)
    fun whenGroupedPermissionsEnabled_grantAllPermissionsForCategory_updatesAppPermissions() {
        revokePermissionViaPackageManager(context, TEST_APP_PACKAGE_NAME, WRITE_BODY_FAT)
        revokePermissionViaPackageManager(context, TEST_APP_PACKAGE_NAME, WRITE_HEIGHT)
        context.launchMainActivity {
            navigateToManageAppPermissions()

            // TODO(b/447325422): Use content description once toggles have A11y support
            scrollDownToAndFindText("Body measurements (2)")
            val preferenceRow = findObject(By.hasDescendant(By.text("Body measurements (2)")))
            val switchWidget = preferenceRow.parent.findObject(By.checkable(true))
            switchWidget.click()
            clickOnDescAndWaitForNewWindow("Navigate up")

            assertPermGrantedForApp(TEST_APP_PACKAGE_NAME, WRITE_BODY_FAT)
            assertPermGrantedForApp(TEST_APP_PACKAGE_NAME, WRITE_HEIGHT)
        }
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_PERMISSIONS_GROUPING_FITNESS_APP_SCREEN)
    fun whenGroupedPermissionsEnabled_revokeAllPermissionsForCategory_updatesAppPermissions() {
        grantPermissionViaPackageManager(context, TEST_APP_PACKAGE_NAME, WRITE_BODY_FAT)
        grantPermissionViaPackageManager(context, TEST_APP_PACKAGE_NAME, WRITE_HEIGHT)
        context.launchMainActivity {
            navigateToManageAppPermissions()
            assertPermGrantedForApp(TEST_APP_PACKAGE_NAME, WRITE_BODY_FAT)

            // TODO(b/447325422): Use content description once toggles have A11y support
            scrollDownToAndFindText("Body measurements (2)")
            val preferenceRow = findObject(By.hasDescendant(By.text("Body measurements (2)")))
            val switchWidget = preferenceRow.parent.findObject(By.checkable(true))
            switchWidget.click()
            clickOnDescAndWaitForNewWindow("Navigate up")

            assertPermNotGrantedForApp(TEST_APP_PACKAGE_NAME, WRITE_BODY_FAT)
            assertPermNotGrantedForApp(TEST_APP_PACKAGE_NAME, WRITE_HEIGHT)
        }
    }

    @Test
    fun revokeAllPermissions_revokesAllAppPermissions() {
        grantPermissionViaPackageManager(context, TEST_APP_PACKAGE_NAME, READ_HEIGHT)
        grantPermissionViaPackageManager(context, TEST_APP_PACKAGE_NAME, READ_MINDFULNESS)
        grantPermissionViaPackageManager(context, TEST_APP_PACKAGE_NAME, WRITE_HEIGHT)
        grantPermissionViaPackageManager(context, TEST_APP_PACKAGE_NAME, WRITE_BODY_FAT)
        grantPermissionViaPackageManager(context, TEST_APP_PACKAGE_NAME, WRITE_STEPS)

        context.launchMainActivity {
            navigateToManageAppPermissions()
            scrollDownToAndFindText("Allow all")
            findTextAndClick("Allow all")
            findText("Remove all permissions?")
            findText("Also delete Health Connect cts test app data from Health Connect")
            clickOnText("Remove all")
            clickOnDescAndWaitForNewWindow("Navigate up")

            assertPermNotGrantedForApp(TEST_APP_PACKAGE_NAME, READ_HEIGHT)
            assertPermNotGrantedForApp(TEST_APP_PACKAGE_NAME, READ_MINDFULNESS)
            assertPermNotGrantedForApp(TEST_APP_PACKAGE_NAME, WRITE_HEIGHT)
            assertPermNotGrantedForApp(TEST_APP_PACKAGE_NAME, WRITE_BODY_FAT)
            assertPermNotGrantedForApp(TEST_APP_PACKAGE_NAME, WRITE_STEPS)
        }
    }

    @Throws(Exception::class)
    private fun assertPermNotGrantedForApp(packageName: String, permName: String) {
        assertThat(context.packageManager.checkPermission(permName, packageName))
            .isEqualTo(PackageManager.PERMISSION_DENIED)
    }

    @Throws(Exception::class)
    private fun assertPermGrantedForApp(packageName: String, permName: String) {
        assertThat(context.packageManager.checkPermission(permName, packageName))
            .isEqualTo(PackageManager.PERMISSION_GRANTED)
    }

    private fun navigateToManageAppPermissions() {
        scrollDownToAndFindText("App permissions")
        clickOnTextAndWaitForNewWindow("App permissions")
        scrollDownToAndFindText("Health Connect cts test app")
        clickOnTextAndWaitForNewWindow("Health Connect cts test app")
        scrollDownToAndFindText("Health Connect cts test app")
        scrollDownToAndFindText("Allowed to read")
    }

    @After
    fun tearDown() {
        grantPermissionViaPackageManager(context, TEST_APP_PACKAGE_NAME, READ_HEIGHT)
        grantPermissionViaPackageManager(context, TEST_APP_PACKAGE_NAME, WRITE_HEIGHT)
        grantPermissionViaPackageManager(context, TEST_APP_PACKAGE_NAME, WRITE_BODY_FAT)
        navigateBackToHomeScreen()
    }
}
