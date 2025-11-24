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
import android.healthconnect.cts.ui.HealthConnectBaseTest
import android.healthconnect.testing.cts.ui.ActivityLauncher.launchRequestPermissionActivity
import android.healthconnect.testing.cts.ui.UiTestUtils.TEST_APP_PACKAGE_NAME
import android.healthconnect.testing.cts.ui.UiTestUtils.clickOnTextAndWaitForNewWindow
import android.healthconnect.testing.cts.ui.UiTestUtils.findText
import android.healthconnect.testing.cts.ui.UiTestUtils.findTextAndClick
import android.healthconnect.testing.cts.ui.UiTestUtils.grantPermissionViaPackageManager
import android.healthconnect.testing.cts.ui.UiTestUtils.revokePermissionViaPackageManager
import android.healthconnect.testing.cts.ui.UiTestUtils.scrollDownToAndClick
import android.healthconnect.testing.cts.ui.UiTestUtils.scrollDownToAndFindText
import android.healthconnect.testing.cts.ui.UiTestUtils.waitForObjectNotFound
import android.os.Build
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import androidx.test.filters.SdkSuppress
import androidx.test.uiautomator.By
import com.android.healthfitness.flags.Flags.FLAG_PERMISSIONS_GROUPING_UI
import com.google.common.truth.Truth
import java.time.Duration.ofSeconds
import org.junit.After
import org.junit.Rule
import org.junit.Test

@RequiresFlagsEnabled(FLAG_PERMISSIONS_GROUPING_UI)
class RequestHealthPermissionUITest : HealthConnectBaseTest() {

    @get:Rule val mCheckFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @Test
    fun showsAppName_showsRequestedPermissions_healthConnectBrand() {
        revokePermissionViaPackageManager(
            context,
            TEST_APP_PACKAGE_NAME,
            HealthPermissions.READ_HEIGHT,
        )
        revokePermissionViaPackageManager(
            context,
            TEST_APP_PACKAGE_NAME,
            HealthPermissions.WRITE_STEPS,
        )
        context.launchRequestPermissionActivity(
            packageName = TEST_APP_PACKAGE_NAME,
            permissions = listOf(HealthPermissions.READ_HEIGHT, HealthPermissions.WRITE_STEPS),
        ) {
            findText("Allow Health Connect cts test app to access Health Connect?")
            // First category expanded by default
            scrollDownToAndFindText("Activity")
            scrollDownToAndFindText("Steps")
            scrollDownToAndClick(By.text("Body measurements"))
            scrollDownToAndFindText("Height")
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
    @Test
    fun showsAppName_showsRequestedPermissions_healthFitnessBrand() {
        revokePermissionViaPackageManager(
            context,
            TEST_APP_PACKAGE_NAME,
            HealthPermissions.READ_HEIGHT,
        )
        revokePermissionViaPackageManager(
            context,
            TEST_APP_PACKAGE_NAME,
            HealthPermissions.WRITE_STEPS,
        )
        context.launchRequestPermissionActivity(
            packageName = TEST_APP_PACKAGE_NAME,
            permissions = listOf(HealthPermissions.READ_HEIGHT, HealthPermissions.WRITE_STEPS),
        ) {
            findText("Allow Health Connect cts test app to access your fitness and wellness data?")
            // First category expanded by default
            scrollDownToAndFindText("Activity")
            scrollDownToAndFindText("Steps")
            scrollDownToAndClick(By.text("Body measurements"))
            scrollDownToAndFindText("Height")
        }
    }

    @Test
    fun requestGrantedPermissions_doesNotShowGrantedPermissions() {
        revokePermissionViaPackageManager(
            context,
            TEST_APP_PACKAGE_NAME,
            HealthPermissions.WRITE_BODY_FAT,
        )
        grantPermissionViaPackageManager(
            context,
            TEST_APP_PACKAGE_NAME,
            HealthPermissions.READ_HEIGHT,
        )

        context.launchRequestPermissionActivity(
            packageName = TEST_APP_PACKAGE_NAME,
            permissions = listOf(HealthPermissions.READ_HEIGHT, HealthPermissions.WRITE_BODY_FAT),
        ) {
            // First category expanded by default
            scrollDownToAndFindText("Body measurements")
            waitForObjectNotFound(By.text("Height"), timeout = ofSeconds(1))
            scrollDownToAndFindText("Body fat")
        }
    }

    @Test
    fun grantPermission_grantsOnlyRequestedPermission() {
        revokePermissionViaPackageManager(
            context,
            TEST_APP_PACKAGE_NAME,
            HealthPermissions.READ_HEIGHT,
        )
        revokePermissionViaPackageManager(
            context,
            TEST_APP_PACKAGE_NAME,
            HealthPermissions.WRITE_BODY_FAT,
        )
        context.launchRequestPermissionActivity(
            packageName = TEST_APP_PACKAGE_NAME,
            permissions = listOf(HealthPermissions.READ_HEIGHT, HealthPermissions.WRITE_BODY_FAT),
        ) {
            // Body measurements category expanded by default
            scrollDownToAndFindText("Height")
            findTextAndClick("Height")
            clickOnTextAndWaitForNewWindow("Allow")

            assertPermGrantedForApp(TEST_APP_PACKAGE_NAME, HealthPermissions.READ_HEIGHT)
            assertPermNotGrantedForApp(TEST_APP_PACKAGE_NAME, HealthPermissions.WRITE_BODY_FAT)
        }
    }

    @Test
    fun grantAllPermissions_grantsAllPermissions() {
        revokePermissionViaPackageManager(
            context,
            TEST_APP_PACKAGE_NAME,
            HealthPermissions.READ_HEIGHT,
        )
        revokePermissionViaPackageManager(
            context,
            TEST_APP_PACKAGE_NAME,
            HealthPermissions.WRITE_HEIGHT,
        )
        context.launchRequestPermissionActivity(
            packageName = TEST_APP_PACKAGE_NAME,
            permissions = listOf(HealthPermissions.READ_HEIGHT, HealthPermissions.WRITE_HEIGHT),
        ) {
            scrollDownToAndFindText("Allow all")
            findTextAndClick("Allow all")
            clickOnTextAndWaitForNewWindow("Allow")

            assertPermGrantedForApp(TEST_APP_PACKAGE_NAME, HealthPermissions.READ_HEIGHT)
            assertPermGrantedForApp(TEST_APP_PACKAGE_NAME, HealthPermissions.WRITE_HEIGHT)
        }
    }

    @After
    fun tearDown() {
        revokePermissionViaPackageManager(
            context,
            TEST_APP_PACKAGE_NAME,
            HealthPermissions.READ_HEIGHT,
        )
        revokePermissionViaPackageManager(
            context,
            TEST_APP_PACKAGE_NAME,
            HealthPermissions.WRITE_HEIGHT,
        )
        revokePermissionViaPackageManager(
            context,
            TEST_APP_PACKAGE_NAME,
            HealthPermissions.WRITE_BODY_FAT,
        )
    }

    @Throws(Exception::class)
    private fun assertPermGrantedForApp(packageName: String, permName: String) {
        Truth.assertThat(context.packageManager.checkPermission(permName, packageName))
            .isEqualTo(PackageManager.PERMISSION_GRANTED)
    }

    @Throws(Exception::class)
    private fun assertPermNotGrantedForApp(packageName: String, permName: String) {
        Truth.assertThat(context.packageManager.checkPermission(permName, packageName))
            .isEqualTo(PackageManager.PERMISSION_DENIED)
    }
}
