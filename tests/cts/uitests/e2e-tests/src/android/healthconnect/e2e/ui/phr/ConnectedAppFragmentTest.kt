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
package android.healthconnect.cts.ui.phr

import android.healthconnect.cts.ui.HealthConnectBaseTest
import android.healthconnect.testing.cts.TestUtils
import android.healthconnect.testing.cts.ui.ActivityLauncher.launchMainActivity
import android.healthconnect.testing.cts.ui.UiTestUtils.findText
import android.healthconnect.testing.cts.ui.UiTestUtils.navigateToManagePermissionsForApp
import android.healthconnect.testing.cts.ui.UiTestUtils.scrollDownToAndFindText
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

class ConnectedAppFragmentTest : HealthConnectBaseTest() {
    @get:Rule val mCheckFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    @Before
    fun setup() {
        TestUtils.deleteAllDataFromHealthConnect()
    }

    @Test
    fun appWithMedicalAndFitnessPermissions_showsCombinedPermissionsScreen() {
        context.launchMainActivity {
            navigateToManagePermissionsForApp("Health Connect cts test app 2")

            findText("Health Connect cts test app 2")
            scrollDownToAndFindText("Permissions")
            scrollDownToAndFindText("Fitness and wellness")
            scrollDownToAndFindText("Health records")
            scrollDownToAndFindText("Additional access")

            scrollDownToAndFindText("Manage app")
            scrollDownToAndFindText("See app data")
            scrollDownToAndFindText("Remove access for this app")
        }
    }

    @Test
    @Ignore("b/391460826 - Elements size too large on expressive devices")
    fun appWithFitnessPermissionsOnly_showsFitnessPermissionsScreen() {
        context.launchMainActivity {
            navigateToManagePermissionsForApp(
                "CtsHealthConnectTestAppBWithNormalReadWritePermission"
            )

            scrollDownToAndFindText("Allowed to read")
            scrollDownToAndFindText("Allowed to write")

            scrollDownToAndFindText("See app data")
        }
    }
}
