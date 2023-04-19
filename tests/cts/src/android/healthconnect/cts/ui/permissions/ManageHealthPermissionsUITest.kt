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
import android.health.connect.HealthPermissions
import android.healthconnect.cts.lib.ActivityLauncher.launchManagePermissionActivity
import android.healthconnect.cts.lib.UiTestUtils.TEST_APP_PACKAGE_NAME
import android.healthconnect.cts.lib.UiTestUtils.clickOnContentDescription
import android.healthconnect.cts.lib.UiTestUtils.clickOnText
import android.healthconnect.cts.lib.UiTestUtils.grantPermissionViaPackageManager
import android.healthconnect.cts.lib.UiTestUtils.revokePermissionViaPackageManager
import android.healthconnect.cts.lib.UiTestUtils.waitDisplayed
import android.healthconnect.cts.lib.UiTestUtils.writeText
import android.healthconnect.cts.ui.HealthConnectBaseTest
import androidx.test.uiautomator.By
import com.google.common.truth.Truth
import org.junit.After
import org.junit.Test

class ManageHealthPermissionsUITest : HealthConnectBaseTest() {

    @Test
    fun showsListOfHealthConnectApps() {
        context.launchManagePermissionActivity {
            waitDisplayed(By.text("Health Connect cts test app"))
        }
    }

    @Test
    fun revokeAllPermissions_revokeAllConnectedAppsPermission() {
        grantPermissionViaPackageManager(
            context, TEST_APP_PACKAGE_NAME, HealthPermissions.READ_HEIGHT)

        context.launchManagePermissionActivity {
            clickOnText("Remove access for all apps")
            clickOnText("Remove all")

            waitDisplayed(By.text("Not allowed access"))
            assertPermNotGrantedForApp(TEST_APP_PACKAGE_NAME, HealthPermissions.READ_HEIGHT)
        }
    }

    @Test
    fun showSearchOption() {
        context.launchManagePermissionActivity {
            clickOnContentDescription("Search apps")

            waitDisplayed(By.text("Search apps"))
            writeText(By.text("Search apps"), "Health Connect cts")
            waitDisplayed(By.text("Health Connect cts test app"))
        }
    }

    @After
    fun tearDown() {
        revokePermissionViaPackageManager(
            context, TEST_APP_PACKAGE_NAME, HealthPermissions.READ_HEIGHT)
        revokePermissionViaPackageManager(
            context, TEST_APP_PACKAGE_NAME, HealthPermissions.WRITE_HEIGHT)
        revokePermissionViaPackageManager(
            context, TEST_APP_PACKAGE_NAME, HealthPermissions.WRITE_BODY_FAT)
    }

    private fun assertPermNotGrantedForApp(packageName: String, permName: String) {
        Truth.assertThat(context.packageManager.checkPermission(permName, packageName))
            .isEqualTo(PackageManager.PERMISSION_DENIED)
    }
}
