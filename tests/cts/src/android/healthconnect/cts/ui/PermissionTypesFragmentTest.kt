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

import android.healthconnect.cts.TestUtils.insertRecords
import android.healthconnect.cts.ui.testing.ActivityLauncher.launchMainActivity
import android.healthconnect.cts.ui.testing.UiTestUtils.clickOnText
import android.healthconnect.cts.ui.testing.UiTestUtils.navigateBackToHomeScreen
import android.healthconnect.cts.ui.testing.UiTestUtils.stepsRecordFromTestApp
import org.junit.After
import org.junit.Test

/** CTS test for HealthConnect Permission types screen. */
class PermissionTypesFragmentTest : HealthConnectBaseTest() {

    @Test
    fun permissionTypes_navigateToPermissionTypes() {
        insertRecords(listOf(stepsRecordFromTestApp()))
        context.launchMainActivity {
            clickOnText("Data and access")
            clickOnText("Activity")
        }
    }

    @Test
    fun permissionTypes_deleteCategoryData() {
        insertRecords(listOf(stepsRecordFromTestApp()))
        context.launchMainActivity {
            clickOnText("Data and access")
            clickOnText("Activity")
            clickOnText("Delete activity data")
            clickOnText("Delete all data")
            clickOnText("Next")
            clickOnText("Delete")
            clickOnText("Done")
        }
    }

    // TODO(b/265789268): Add one app, no filter test.
    // TODO(b/265789268): Add two apps, filter test.
    // TODO(b/265789268): Add delete category data test.
    // TODO(b/265789268): Add app priority test.

    @After
    fun tearDown() {
        navigateBackToHomeScreen()
    }

    companion object {
        private const val TAG = "DataAccessFragmentTest"
    }
}
