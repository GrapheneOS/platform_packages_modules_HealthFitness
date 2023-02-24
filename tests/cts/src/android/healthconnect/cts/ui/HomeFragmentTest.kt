/*
 * Copyright (C) 2022 The Android Open Source Project
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
package android.healthconnect.cts.ui

import android.healthconnect.cts.ui.testing.ActivityLauncher.launchMainActivity
import android.healthconnect.cts.ui.testing.UiTestUtils.clickOnText
import android.healthconnect.cts.ui.testing.UiTestUtils.navigateBackToHomeScreen
import android.healthconnect.cts.ui.testing.UiTestUtils.waitDisplayed
import androidx.test.uiautomator.By
import org.junit.After
import org.junit.Test

/** CTS test for HealthConnect Home screen. */
class HomeFragmentTest : HealthConnectBaseTest() {

    @Test
    fun homeFragment_openAppPermissions() {
        context.launchMainActivity {
            clickOnText("App permissions")

            waitDisplayed(By.text("Allowed access"))
            waitDisplayed(By.text("Not allowed access"))
        }
    }

    @Test
    fun homeFragment_openDataManagement() {
        context.launchMainActivity {
            clickOnText("Data and access")

            waitDisplayed(By.text("Browse data"))
            waitDisplayed(By.text("Manage data"))
            waitDisplayed(By.text("Auto-delete"))

            waitDisplayed(By.text("Delete all data"))
        }
    }

    // TODO(b/265789268): Add recent access test.

    @After
    fun tearDown() {
        navigateBackToHomeScreen()
    }

    companion object {
        private const val TAG = "HomeFragmentTest"
    }
}
