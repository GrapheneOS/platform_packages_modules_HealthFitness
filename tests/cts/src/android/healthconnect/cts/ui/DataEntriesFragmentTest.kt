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
import android.healthconnect.cts.ui.testing.UiTestUtils.clickOnContentDescription
import android.healthconnect.cts.ui.testing.UiTestUtils.clickOnText
import android.healthconnect.cts.ui.testing.UiTestUtils.distanceRecordFromTestApp
import android.healthconnect.cts.ui.testing.UiTestUtils.navigateBackToHomeScreen
import android.healthconnect.cts.ui.testing.UiTestUtils.navigateUp
import android.healthconnect.cts.ui.testing.UiTestUtils.waitDisplayed
import androidx.test.uiautomator.By
import org.junit.After
import org.junit.Test

/** CTS test for HealthConnect Data entries screen. */
class DataEntriesFragmentTest : HealthConnectBaseTest() {

    @Test
    fun dataEntries_changeUnit() {
        insertRecords(listOf(distanceRecordFromTestApp()))
        context.launchMainActivity {
            clickOnText("Data and access")
            clickOnText("Activity")
            clickOnText("Distance")
            clickOnText("See all entries")

            clickOnContentDescription("More options")
            clickOnText("Set data units")
            clickOnText("Distance")
            clickOnText("Kilometers")
            navigateUp()

            waitDisplayed(By.text("0.5 km"))

            clickOnContentDescription("More options")
            clickOnText("Set data units")
            clickOnText("Distance")
            clickOnText("Miles")
            navigateUp()

            waitDisplayed(By.text("0.311 miles"))
        }
    }

    // TODO(b/265789268): Add date picker navigation test
    // TODO(b/265789268): Add delete entry test.

    @After
    fun tearDown() {
        navigateBackToHomeScreen()
    }

    companion object {
        private const val TAG = "DataAccessFragmentTest"
    }
}
