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
import android.healthconnect.cts.lib.ActivityLauncher.launchRequestPermissionActivity
import android.healthconnect.cts.lib.UiTestUtils.TEST_APP_PACKAGE_NAME
import android.healthconnect.cts.lib.UiTestUtils.clickOnTextAndWaitForNewWindow
import android.healthconnect.cts.lib.UiTestUtils.findText
import android.healthconnect.cts.lib.UiTestUtils.findTextAndClick
import android.healthconnect.cts.lib.UiTestUtils.grantPermissionViaPackageManager
import android.healthconnect.cts.lib.UiTestUtils.revokePermissionViaPackageManager
import android.healthconnect.cts.lib.UiTestUtils.scrollDownToAndFindText
import android.healthconnect.cts.lib.UiTestUtils.waitForObjectNotFound
import android.healthconnect.cts.ui.HealthConnectBaseTest
import android.os.Build
import androidx.test.filters.SdkSuppress
import androidx.test.uiautomator.By
import com.google.common.truth.Truth
import java.time.Duration.ofSeconds
import org.junit.After
import org.junit.Test

class RequestHealthPermissionUITest : HealthConnectBaseTest() {

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
            HealthPermissions.WRITE_BODY_FAT,
        )
        context.launchRequestPermissionActivity(
            packageName = TEST_APP_PACKAGE_NAME,
            permissions = listOf(HealthPermissions.READ_HEIGHT, HealthPermissions.WRITE_BODY_FAT),
        ) {
            findText("Allow Health Connect cts test app to access Health Connect?")
            scrollDownToAndFindText("Height")
            scrollDownToAndFindText("Body fat")
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
            HealthPermissions.WRITE_BODY_FAT,
        )
        context.launchRequestPermissionActivity(
            packageName = TEST_APP_PACKAGE_NAME,
            permissions = listOf(HealthPermissions.READ_HEIGHT, HealthPermissions.WRITE_BODY_FAT),
        ) {
            findText("Allow Health Connect cts test app to access your fitness and wellness data?")
            scrollDownToAndFindText("Height")
            scrollDownToAndFindText("Body fat")
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
