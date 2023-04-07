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

import android.health.connect.TimeInstantRangeFilter
import android.health.connect.datatypes.StepsRecord
import android.healthconnect.cts.TestUtils.insertRecords
import android.healthconnect.cts.TestUtils.verifyDeleteRecords
import android.healthconnect.cts.lib.ActivityLauncher.launchMainActivity
import android.healthconnect.cts.lib.UiTestUtils.clickOnText
import android.healthconnect.cts.lib.UiTestUtils.navigateBackToHomeScreen
import android.healthconnect.cts.lib.UiTestUtils.navigateUp
import android.healthconnect.cts.lib.UiTestUtils.stepsRecordFromTestApp
import android.healthconnect.cts.lib.UiTestUtils.stepsRecordFromTestApp2
import android.healthconnect.cts.lib.UiTestUtils.waitDisplayed
import androidx.test.uiautomator.By
import java.time.Instant
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

    @Test
    fun homeFragment_recentAccessShownOnHomeScreen() {
        // TODO(b/265789268): Finish when ag/21642785 is merged.
        insertRecords(listOf(stepsRecordFromTestApp()))
        insertRecords(listOf(stepsRecordFromTestApp2()))
        context.launchMainActivity {
            // waitDisplayed(By.text("TestApp"))
            // waitDisplayed(By.text("TestApp2"))
            waitDisplayed(By.text("See all recent access"))

            // Delete all data
            clickOnText("Data and access")
            clickOnText("Delete all data")
            clickOnText("Delete all data")
            clickOnText("Next")
            clickOnText("Delete")
            clickOnText("Done")
        }
    }

    @Test
    fun homeFragment_navigateToRecentAccess() {
        // TODO(b/265789268): Finish when ag/21642785 is merged.
        insertRecords(listOf(stepsRecordFromTestApp()))
        insertRecords(listOf(stepsRecordFromTestApp2()))
        context.launchMainActivity {
            clickOnText("See all recent access")

            // waitDisplayed(By.text("TestApp"))
            // waitDisplayed(By.text("TestApp2"))

            // Delete all data
            navigateUp()
            clickOnText("Data and access")
            clickOnText("Delete all data")
            clickOnText("Delete all data")
            clickOnText("Next")
            clickOnText("Delete")
            clickOnText("Done")
        }
    }

    @After
    fun tearDown() {
        verifyDeleteRecords(
            StepsRecord::class.java,
            TimeInstantRangeFilter.Builder()
                .setStartTime(Instant.EPOCH)
                .setEndTime(Instant.now())
                .build())
        navigateBackToHomeScreen()
    }

    companion object {
        private const val TAG = "HomeFragmentTest"
    }
}
