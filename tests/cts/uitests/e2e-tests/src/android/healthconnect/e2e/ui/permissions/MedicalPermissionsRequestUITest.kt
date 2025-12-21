/*
 * Copyright (C) 2024 The Android Open Source Project
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
import android.healthconnect.testing.cts.ui.UiTestUtils.TEST_APP_2_PACKAGE_NAME
import android.healthconnect.testing.cts.ui.UiTestUtils.clickOnTextAndWaitForNewWindow
import android.healthconnect.testing.cts.ui.UiTestUtils.findText
import android.healthconnect.testing.cts.ui.UiTestUtils.findTextAndClick
import android.healthconnect.testing.cts.ui.UiTestUtils.grantPermissionViaPackageManager
import android.healthconnect.testing.cts.ui.UiTestUtils.revokePermissionViaPackageManager
import android.healthconnect.testing.cts.ui.UiTestUtils.scrollDownToAndFindText
import android.healthconnect.testing.cts.ui.UiTestUtils.verifyTextNotFound
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test

class MedicalPermissionsRequestUITest : HealthConnectBaseTest() {

    @get:Rule val mCheckFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    @Test
    fun requestMedicalWrite_allow_grantsPermission() {
        revokePermissionViaPackageManager(
            context,
            TEST_APP_2_PACKAGE_NAME,
            HealthPermissions.WRITE_MEDICAL_DATA,
        )
        context.launchRequestPermissionActivity(
            packageName = TEST_APP_2_PACKAGE_NAME,
            permissions = listOf(HealthPermissions.WRITE_MEDICAL_DATA),
        ) {
            scrollDownToAndFindText(
                "Allow CtsHealthConnectTestAppBWithNormalReadWritePermission to access your medical records?"
            )
            scrollDownToAndFindText("Data to share includes")

            clickOnTextAndWaitForNewWindow("Allow")
            assertPermGrantedForApp(TEST_APP_2_PACKAGE_NAME, HealthPermissions.WRITE_MEDICAL_DATA)
        }
    }

    @Test
    fun requestMedicalWrite_dontAllow_doesNotGrantPermission() {
        revokePermissionViaPackageManager(
            context,
            TEST_APP_2_PACKAGE_NAME,
            HealthPermissions.WRITE_MEDICAL_DATA,
        )
        context.launchRequestPermissionActivity(
            packageName = TEST_APP_2_PACKAGE_NAME,
            permissions = listOf(HealthPermissions.WRITE_MEDICAL_DATA),
        ) {
            scrollDownToAndFindText(
                "Allow CtsHealthConnectTestAppBWithNormalReadWritePermission to access your medical records?"
            )
            scrollDownToAndFindText("Data to share includes")

            clickOnTextAndWaitForNewWindow("Don't allow")
            assertPermNotGrantedForApp(
                TEST_APP_2_PACKAGE_NAME,
                HealthPermissions.WRITE_MEDICAL_DATA,
            )
        }
    }

    @Test
    fun requestMedicalReadAndWrite_showsRequestedPermissions() {
        revokePermissionViaPackageManager(
            context,
            TEST_APP_2_PACKAGE_NAME,
            HealthPermissions.WRITE_MEDICAL_DATA,
        )
        revokePermissionViaPackageManager(
            context,
            TEST_APP_2_PACKAGE_NAME,
            HealthPermissions.READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES,
        )
        revokePermissionViaPackageManager(
            context,
            TEST_APP_2_PACKAGE_NAME,
            HealthPermissions.READ_MEDICAL_DATA_CONDITIONS,
        )
        context.launchRequestPermissionActivity(
            packageName = TEST_APP_2_PACKAGE_NAME,
            permissions =
                listOf(
                    HealthPermissions.WRITE_MEDICAL_DATA,
                    HealthPermissions.READ_MEDICAL_DATA_CONDITIONS,
                    HealthPermissions.READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES,
                ),
        ) {
            scrollDownToAndFindText(
                "Allow CtsHealthConnectTestAppBWithNormalReadWritePermission to access your medical records?"
            )
            scrollDownToAndFindText("Allergies")
            scrollDownToAndFindText("Conditions")
            scrollDownToAndFindText("All medical records")
        }
    }

    @Test
    fun requestMedicalReadAndWrite_doesNotShowGrantedPermissions() {
        revokePermissionViaPackageManager(
            context,
            TEST_APP_2_PACKAGE_NAME,
            HealthPermissions.WRITE_MEDICAL_DATA,
        )
        revokePermissionViaPackageManager(
            context,
            TEST_APP_2_PACKAGE_NAME,
            HealthPermissions.READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES,
        )
        grantPermissionViaPackageManager(
            context,
            TEST_APP_2_PACKAGE_NAME,
            HealthPermissions.READ_MEDICAL_DATA_CONDITIONS,
        )
        context.launchRequestPermissionActivity(
            packageName = TEST_APP_2_PACKAGE_NAME,
            permissions =
                listOf(
                    HealthPermissions.WRITE_MEDICAL_DATA,
                    HealthPermissions.READ_MEDICAL_DATA_CONDITIONS,
                    HealthPermissions.READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES,
                ),
        ) {
            scrollDownToAndFindText(
                "Allow CtsHealthConnectTestAppBWithNormalReadWritePermission to access your medical records?"
            )
            scrollDownToAndFindText("Allergies")

            verifyTextNotFound("Conditions")

            scrollDownToAndFindText("All medical records")
        }
    }

    @Test
    fun requestMedicalReadAndWrite_grantsOnlyRequestedPermissions() {
        revokePermissionViaPackageManager(
            context,
            TEST_APP_2_PACKAGE_NAME,
            HealthPermissions.WRITE_MEDICAL_DATA,
        )
        revokePermissionViaPackageManager(
            context,
            TEST_APP_2_PACKAGE_NAME,
            HealthPermissions.READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES,
        )
        revokePermissionViaPackageManager(
            context,
            TEST_APP_2_PACKAGE_NAME,
            HealthPermissions.READ_MEDICAL_DATA_CONDITIONS,
        )
        context.launchRequestPermissionActivity(
            packageName = TEST_APP_2_PACKAGE_NAME,
            permissions =
                listOf(
                    HealthPermissions.WRITE_MEDICAL_DATA,
                    HealthPermissions.READ_MEDICAL_DATA_CONDITIONS,
                    HealthPermissions.READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES,
                ),
        ) {
            scrollDownToAndFindText(
                "Allow CtsHealthConnectTestAppBWithNormalReadWritePermission to access your medical records?"
            )
            scrollDownToAndFindText("Allergies")
            findTextAndClick("Allergies")

            scrollDownToAndFindText("Conditions")

            scrollDownToAndFindText("All medical records")
            findTextAndClick("All medical records")

            clickOnTextAndWaitForNewWindow("Allow")

            assertPermGrantedForApp(TEST_APP_2_PACKAGE_NAME, HealthPermissions.WRITE_MEDICAL_DATA)
            assertPermGrantedForApp(
                TEST_APP_2_PACKAGE_NAME,
                HealthPermissions.READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES,
            )
            assertPermNotGrantedForApp(
                TEST_APP_2_PACKAGE_NAME,
                HealthPermissions.READ_MEDICAL_DATA_CONDITIONS,
            )
        }
    }

    @Test
    fun requestMedicalReadAndWrite_allowAll_grantsAllRequestedPermissions() {
        revokePermissionViaPackageManager(
            context,
            TEST_APP_2_PACKAGE_NAME,
            HealthPermissions.WRITE_MEDICAL_DATA,
        )
        revokePermissionViaPackageManager(
            context,
            TEST_APP_2_PACKAGE_NAME,
            HealthPermissions.READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES,
        )
        revokePermissionViaPackageManager(
            context,
            TEST_APP_2_PACKAGE_NAME,
            HealthPermissions.READ_MEDICAL_DATA_CONDITIONS,
        )
        context.launchRequestPermissionActivity(
            packageName = TEST_APP_2_PACKAGE_NAME,
            permissions =
                listOf(
                    HealthPermissions.WRITE_MEDICAL_DATA,
                    HealthPermissions.READ_MEDICAL_DATA_CONDITIONS,
                    HealthPermissions.READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES,
                ),
        ) {
            findText(
                "Allow CtsHealthConnectTestAppBWithNormalReadWritePermission to access your medical records?"
            )
            scrollDownToAndFindText("Allow all")
            findTextAndClick("Allow all")

            clickOnTextAndWaitForNewWindow("Allow")

            assertPermGrantedForApp(TEST_APP_2_PACKAGE_NAME, HealthPermissions.WRITE_MEDICAL_DATA)
            assertPermGrantedForApp(
                TEST_APP_2_PACKAGE_NAME,
                HealthPermissions.READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES,
            )
            assertPermGrantedForApp(
                TEST_APP_2_PACKAGE_NAME,
                HealthPermissions.READ_MEDICAL_DATA_CONDITIONS,
            )
        }
    }

    @Test
    fun requestMedicalReadAndWrite_dontAllow_doesNotGrantPermissions() {
        revokePermissionViaPackageManager(
            context,
            TEST_APP_2_PACKAGE_NAME,
            HealthPermissions.WRITE_MEDICAL_DATA,
        )
        revokePermissionViaPackageManager(
            context,
            TEST_APP_2_PACKAGE_NAME,
            HealthPermissions.READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES,
        )
        revokePermissionViaPackageManager(
            context,
            TEST_APP_2_PACKAGE_NAME,
            HealthPermissions.READ_MEDICAL_DATA_CONDITIONS,
        )
        context.launchRequestPermissionActivity(
            packageName = TEST_APP_2_PACKAGE_NAME,
            permissions =
                listOf(
                    HealthPermissions.WRITE_MEDICAL_DATA,
                    HealthPermissions.READ_MEDICAL_DATA_CONDITIONS,
                    HealthPermissions.READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES,
                ),
        ) {
            findText(
                "Allow CtsHealthConnectTestAppBWithNormalReadWritePermission to access your medical records?"
            )

            clickOnTextAndWaitForNewWindow("Don't allow")

            assertPermNotGrantedForApp(
                TEST_APP_2_PACKAGE_NAME,
                HealthPermissions.WRITE_MEDICAL_DATA,
            )
            assertPermNotGrantedForApp(
                TEST_APP_2_PACKAGE_NAME,
                HealthPermissions.READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES,
            )
            assertPermNotGrantedForApp(
                TEST_APP_2_PACKAGE_NAME,
                HealthPermissions.READ_MEDICAL_DATA_CONDITIONS,
            )
        }
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
