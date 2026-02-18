/*
 * Copyright (C) 2025 The Android Open Source Project
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

package android.healthconnect.e2e.ui.matchmaking

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.health.connect.HealthConnectManager
import android.health.connect.HealthPermissions
import android.health.connect.MatchmakingRequest
import android.health.connect.datatypes.HeightRecord
import android.health.connect.datatypes.Record
import android.health.connect.datatypes.StepsRecord
import android.healthconnect.cts.ui.HealthConnectBaseTest
import android.healthconnect.testing.cts.testapphelpers.TestAppProxy
import android.healthconnect.testing.cts.ui.UiTestUtils.TEST_APP_2_PACKAGE_NAME
import android.healthconnect.testing.cts.ui.UiTestUtils.TEST_APP_PACKAGE_NAME
import android.healthconnect.testing.cts.ui.UiTestUtils.clickOnTextAndWaitForNewWindow
import android.healthconnect.testing.cts.ui.UiTestUtils.findText
import android.healthconnect.testing.cts.ui.UiTestUtils.findTextAndClick
import android.healthconnect.testing.cts.ui.UiTestUtils.grantPermissionViaPackageManager
import android.healthconnect.testing.cts.ui.UiTestUtils.pressBack
import android.healthconnect.testing.cts.ui.UiTestUtils.revokePermissionViaPackageManager
import android.healthconnect.testing.cts.ui.UiTestUtils.scrollDownToAndFindText
import android.healthconnect.testing.cts.ui.UiTestUtils.waitForIdle
import android.healthconnect.testing.cts.ui.UiTestUtils.waitMatchmakingActivityDisplayed
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import androidx.core.content.ContextCompat.getSystemService
import com.android.healthfitness.flags.Flags.FLAG_MATCHMAKING
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Rule
import org.junit.Test

@RequiresFlagsEnabled(FLAG_MATCHMAKING)
class MatchmakingUITest : HealthConnectBaseTest() {

    @get:Rule val mCheckFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    private val matchmakingReaderApp: TestAppProxy =
        TestAppProxy.forPackageName(TEST_APP_PACKAGE_NAME)

    @Test
    fun permissionSelectedUnderAnApp_allowClicked_grantsTheSelectedPermissions() {
        setupPermissions()

        assertPermGrantedForApp(TEST_APP_PACKAGE_NAME, HealthPermissions.READ_HEIGHT)
        assertPermGrantedForApp(TEST_APP_PACKAGE_NAME, HealthPermissions.READ_STEPS)
        assertPermissionDenied(TEST_APP_2_PACKAGE_NAME, HealthPermissions.WRITE_HEIGHT)
        assertPermissionDenied(TEST_APP_2_PACKAGE_NAME, HealthPermissions.WRITE_STEPS)

        val result =
            matchmakingReaderApp.startActivityForResult(
                context.createMatchmakingIntent(
                    listOf(HeightRecord::class.java, StepsRecord::class.java)
                )
            ) {
                waitMatchmakingActivityDisplayed()
                findText("Sync your apps and devices")
                scrollDownToAndFindText(
                    "Allow the CtsHealthConnectTestAppAWithNormalReadWritePermission app to read data from other apps or devices on this device using Health Connect. This data can also be read by other apps you give access to."
                )
                scrollDownToAndFindText("Allow all")
                scrollDownToAndFindText("CtsHealthConnectTestAppBWithNormalReadWritePermission")
                scrollDownToAndFindText("Height")
                findTextAndClick("Height")
                scrollDownToAndFindText("Steps")
                findTextAndClick("Steps")
                clickOnTextAndWaitForNewWindow("Allow")
            }

        assertPermGrantedForApp(TEST_APP_2_PACKAGE_NAME, HealthPermissions.WRITE_HEIGHT)
        assertPermGrantedForApp(TEST_APP_2_PACKAGE_NAME, HealthPermissions.WRITE_STEPS)
        assertThat(result.resultCode).isEqualTo(Activity.RESULT_OK)
    }

    @Test
    fun allowAllClicked_grantsPermissions() {
        setupPermissions()

        assertPermGrantedForApp(TEST_APP_PACKAGE_NAME, HealthPermissions.READ_HEIGHT)
        assertPermGrantedForApp(TEST_APP_PACKAGE_NAME, HealthPermissions.READ_STEPS)
        assertPermissionDenied(TEST_APP_2_PACKAGE_NAME, HealthPermissions.WRITE_HEIGHT)
        assertPermissionDenied(TEST_APP_2_PACKAGE_NAME, HealthPermissions.WRITE_STEPS)

        val result =
            matchmakingReaderApp.startActivityForResult(
                context.createMatchmakingIntent(
                    listOf(HeightRecord::class.java, StepsRecord::class.java)
                )
            ) {
                waitMatchmakingActivityDisplayed()
                findText("Sync your apps and devices")
                scrollDownToAndFindText(
                    "Allow the CtsHealthConnectTestAppAWithNormalReadWritePermission app to read data from other apps or devices on this device using Health Connect. This data can also be read by other apps you give access to."
                )
                scrollDownToAndFindText("Allow all")
                findTextAndClick("Allow all")
                clickOnTextAndWaitForNewWindow("Allow")
            }

        assertPermGrantedForApp(TEST_APP_2_PACKAGE_NAME, HealthPermissions.WRITE_HEIGHT)
        assertPermGrantedForApp(TEST_APP_2_PACKAGE_NAME, HealthPermissions.WRITE_STEPS)
        assertThat(result.resultCode).isEqualTo(Activity.RESULT_OK)
    }

    @Test
    fun userCancels_activityReturnsResultCancelled() {
        setupPermissions()

        assertPermGrantedForApp(TEST_APP_PACKAGE_NAME, HealthPermissions.READ_HEIGHT)
        assertPermGrantedForApp(TEST_APP_PACKAGE_NAME, HealthPermissions.READ_STEPS)
        assertPermissionDenied(TEST_APP_2_PACKAGE_NAME, HealthPermissions.WRITE_HEIGHT)
        assertPermissionDenied(TEST_APP_2_PACKAGE_NAME, HealthPermissions.WRITE_STEPS)

        val result =
            matchmakingReaderApp.startActivityForResult(
                context.createMatchmakingIntent(
                    listOf(HeightRecord::class.java, StepsRecord::class.java)
                )
            ) {
                waitMatchmakingActivityDisplayed()
                findText("Sync your apps and devices")
                scrollDownToAndFindText(
                    "Allow the CtsHealthConnectTestAppAWithNormalReadWritePermission app to read data from other apps or devices on this device using Health Connect. This data can also be read by other apps you give access to."
                )

                pressBack()
            }

        assertThat(result.resultCode).isEqualTo(Activity.RESULT_CANCELED)
    }

    @Test
    fun noMatchingApps_activityReturnsResultCancelled() {
        grantPermissionViaPackageManager(
            context,
            TEST_APP_PACKAGE_NAME,
            HealthPermissions.READ_HEIGHT,
        )
        grantPermissionViaPackageManager(
            context,
            TEST_APP_2_PACKAGE_NAME,
            HealthPermissions.WRITE_HEIGHT,
        )

        waitForIdle()

        val result =
            matchmakingReaderApp.startActivityForResult(
                context.createMatchmakingIntent(listOf(HeightRecord::class.java))
            )

        assertThat(result.resultCode).isEqualTo(Activity.RESULT_CANCELED)
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
            TEST_APP_2_PACKAGE_NAME,
            HealthPermissions.WRITE_HEIGHT,
        )
        revokePermissionViaPackageManager(
            context,
            TEST_APP_2_PACKAGE_NAME,
            HealthPermissions.WRITE_STEPS,
        )
    }

    private fun Context.createMatchmakingIntent(recordTypes: List<Class<out Record>>): Intent {
        val manager = getSystemService(HealthConnectManager::class.java)!!
        val request =
            MatchmakingRequest.Builder()
                .apply {
                    for (recordType in recordTypes) {
                        addRecordType(recordType)
                    }
                }
                .build()

        val intent = manager.createMatchmakingIntent(request)
        return intent
    }

    private fun setupPermissions() {
        grantPermissionViaPackageManager(
            context,
            TEST_APP_PACKAGE_NAME,
            HealthPermissions.READ_HEIGHT,
        )
        grantPermissionViaPackageManager(
            context,
            TEST_APP_PACKAGE_NAME,
            HealthPermissions.READ_STEPS,
        )
        revokePermissionViaPackageManager(
            context,
            TEST_APP_2_PACKAGE_NAME,
            HealthPermissions.WRITE_HEIGHT,
        )
        revokePermissionViaPackageManager(
            context,
            TEST_APP_2_PACKAGE_NAME,
            HealthPermissions.WRITE_STEPS,
        )

        waitForIdle()
    }

    @Throws(Exception::class)
    private fun assertPermGrantedForApp(packageName: String, permName: String) {
        assertThat(context.packageManager.checkPermission(permName, packageName))
            .isEqualTo(PackageManager.PERMISSION_GRANTED)
    }

    @Throws(Exception::class)
    private fun assertPermNotGrantedForApp(packageName: String, permName: String) {
        assertThat(context.packageManager.checkPermission(permName, packageName))
            .isEqualTo(PackageManager.PERMISSION_DENIED)
    }
}
