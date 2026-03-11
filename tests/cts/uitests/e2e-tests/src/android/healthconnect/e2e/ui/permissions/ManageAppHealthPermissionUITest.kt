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
import android.health.connect.HealthPermissions.WRITE_BODY_FAT
import android.health.connect.HealthPermissions.WRITE_HEIGHT
import android.healthconnect.cts.ui.HealthConnectBaseTest
import android.healthconnect.testing.cts.PermissionUtils
import android.healthconnect.testing.cts.ui.ActivityLauncher.launchMainActivity
import android.healthconnect.testing.cts.ui.UiTestUtils.TEST_APP_NAME
import android.healthconnect.testing.cts.ui.UiTestUtils.TEST_APP_PACKAGE_NAME
import android.healthconnect.testing.cts.ui.UiTestUtils.clickOnDescAndWaitForNewWindow
import android.healthconnect.testing.cts.ui.UiTestUtils.findObject
import android.healthconnect.testing.cts.ui.UiTestUtils.findTextAndClick
import android.healthconnect.testing.cts.ui.UiTestUtils.grantPermissionViaPackageManager
import android.healthconnect.testing.cts.ui.UiTestUtils.navigateBackToHomeScreen
import android.healthconnect.testing.cts.ui.UiTestUtils.navigateToManagePermissionsForApp
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
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ManageAppHealthPermissionUITest : HealthConnectBaseTest() {

    @get:Rule val mCheckFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_PERMISSIONS_GROUPING_FITNESS_APP_SCREEN)
    fun showDeclaredPermissions_withGrouping() {
        context.launchMainActivity {
            navigateToManagePermissionsForApp(TEST_APP_NAME)
            scrollDownToAndFindText("Fitness and wellness")
            findTextAndClick("Fitness and wellness")
            scrollDownToAndFindText("Activity")
            findTextAndClick("Activity")
            scrollDownToAndFindText("Steps")
        }
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_PERMISSIONS_GROUPING_FITNESS_APP_SCREEN)
    fun showDeclaredPermissions() {
        context.launchMainActivity {
            navigateToManagePermissionsForApp(TEST_APP_NAME)

            scrollDownToAndFindText("Steps")
        }
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_PERMISSIONS_GROUPING_FITNESS_APP_SCREEN)
    fun grantPermission_updatesAppPermissions() {
        revokePermissionViaPackageManager(context, TEST_APP_PACKAGE_NAME, WRITE_BODY_FAT)
        context.launchMainActivity {
            navigateToManagePermissionsForApp(TEST_APP_NAME)

            scrollDownToAndFindText("Allowed to write")
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
            navigateToManagePermissionsForApp(TEST_APP_NAME)
            assertPermGrantedForApp(TEST_APP_PACKAGE_NAME, WRITE_BODY_FAT)

            scrollDownToAndFindText("Allowed to write")
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
            navigateToManagePermissionsForApp(TEST_APP_NAME)
            scrollDownToAndFindText("Fitness and wellness")
            findTextAndClick("Fitness and wellness")
            scrollDownToAndFindText("Allowed to write")

            // TODO(b/447325422): Use content description once toggles have A11y support
            scrollDownToAndFindText("5 of 7 selected")
            findTextAndClick("5 of 7 selected")
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
        context.launchMainActivity {
            navigateToManagePermissionsForApp(TEST_APP_NAME)
            scrollDownToAndFindText("Fitness and wellness")
            findTextAndClick("Fitness and wellness")
            scrollDownToAndFindText("Allowed to write")

            // TODO(b/447325422): Use content description once toggles have A11y support
            scrollDownToAndFindText("7 of 7 selected")
            findTextAndClick("7 of 7 selected")
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
            navigateToManagePermissionsForApp(TEST_APP_NAME)
            scrollDownToAndFindText("Fitness and wellness")
            findTextAndClick("Fitness and wellness")
            scrollDownToAndFindText("Allowed to write")

            // TODO(b/447325422): Use content description once toggles have A11y support
            scrollDownToAndFindText("5 of 7 selected")
            val preferenceRow = findObject(By.hasDescendant(By.text("5 of 7 selected")))
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
        context.launchMainActivity {
            navigateToManagePermissionsForApp(TEST_APP_NAME)
            scrollDownToAndFindText("Fitness and wellness")
            findTextAndClick("Fitness and wellness")
            scrollDownToAndFindText("Allowed to write")

            // TODO(b/447325422): Use content description once toggles have A11y support
            scrollDownToAndFindText("7 of 7 selected")
            val preferenceRow = findObject(By.hasDescendant(By.text("7 of 7 selected")))
            val switchWidget = preferenceRow.parent.findObject(By.checkable(true))
            switchWidget.click()
            clickOnDescAndWaitForNewWindow("Navigate up")

            assertPermNotGrantedForApp(TEST_APP_PACKAGE_NAME, WRITE_BODY_FAT)
            assertPermNotGrantedForApp(TEST_APP_PACKAGE_NAME, WRITE_HEIGHT)
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

    @Before
    fun setup() {
        PermissionUtils.grantAllHealthPermissions(TEST_APP_PACKAGE_NAME)
        navigateBackToHomeScreen()
    }

    @After
    fun tearDown() {
        PermissionUtils.grantAllHealthPermissions(TEST_APP_PACKAGE_NAME)
        navigateBackToHomeScreen()
    }
}
