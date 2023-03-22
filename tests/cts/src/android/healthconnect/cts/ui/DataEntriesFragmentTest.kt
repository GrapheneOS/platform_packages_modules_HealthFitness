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
import android.health.connect.datatypes.DistanceRecord
import android.health.connect.datatypes.StepsRecord
import android.healthconnect.cts.TestUtils.insertRecords
import android.healthconnect.cts.TestUtils.verifyDeleteRecords
import android.healthconnect.cts.ui.testing.ActivityLauncher.launchMainActivity
import android.healthconnect.cts.ui.testing.UiTestUtils.clickOnContentDescription
import android.healthconnect.cts.ui.testing.UiTestUtils.clickOnText
import android.healthconnect.cts.ui.testing.UiTestUtils.distanceRecordFromTestApp
import android.healthconnect.cts.ui.testing.UiTestUtils.navigateBackToHomeScreen
import android.healthconnect.cts.ui.testing.UiTestUtils.navigateUp
import android.healthconnect.cts.ui.testing.UiTestUtils.stepsRecordFromTestApp
import android.healthconnect.cts.ui.testing.UiTestUtils.waitDisplayed
import android.healthconnect.cts.ui.testing.UiTestUtils.waitNotDisplayed
import androidx.test.uiautomator.By
import java.time.Instant
import java.time.Period.ofDays
import org.junit.After
import org.junit.Test

/** CTS test for HealthConnect Data entries screen. */
class DataEntriesFragmentTest : HealthConnectBaseTest() {
    @Test
    fun dataEntries_changeUnit_deleteEntry() {
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

            // Delete entry
            clickOnContentDescription("Delete data entry")
            clickOnText("Delete")
            clickOnText("Done")
            waitDisplayed(By.text("No data"))
        }
    }

    @Test
    fun dataEntries_navigateToYesterday() {
        insertRecords(listOf(stepsRecordFromTestApp(12, Instant.now().minus(ofDays(1)))))
        context.launchMainActivity {
            clickOnText("Data and access")
            clickOnText("Activity")
            clickOnText("Steps")
            clickOnText("See all entries")
            waitDisplayed(By.text("No data"))
            waitNotDisplayed(By.text("12 steps"))

            clickOnContentDescription("Previous day")
            waitNotDisplayed(By.text("No data"))
            waitDisplayed(By.text("12 steps"))

            // Delete data
            navigateBackToHomeScreen()
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
        verifyDeleteRecords(
            DistanceRecord::class.java,
            TimeInstantRangeFilter.Builder()
                .setStartTime(Instant.EPOCH)
                .setEndTime(Instant.now())
                .build())
        navigateBackToHomeScreen()
    }

    companion object {
        private const val TAG = "DataAccessFragmentTest"
    }
}
