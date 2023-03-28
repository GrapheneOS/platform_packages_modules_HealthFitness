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
import android.healthconnect.cts.ui.testing.ActivityLauncher.launchDataActivity
import android.healthconnect.cts.ui.testing.ActivityLauncher.launchMainActivity
import android.healthconnect.cts.ui.testing.UiTestUtils.clickOnText
import android.healthconnect.cts.ui.testing.UiTestUtils.navigateBackToHomeScreen
import android.healthconnect.cts.ui.testing.UiTestUtils.stepsRecordFromTestApp
import android.healthconnect.cts.ui.testing.UiTestUtils.waitDisplayed
import androidx.test.uiautomator.By
import java.time.Duration
import java.time.Instant
import org.junit.After
import org.junit.Test

/** CTS test for HealthConnect Data access screen. */
class DataAccessFragmentTest : HealthConnectBaseTest() {

    @Test
    fun dataAccess_navigateToDataAccess() {
        insertRecords(listOf(stepsRecordFromTestApp()))
        context.launchDataActivity {
            clickOnText("Activity")

            waitDisplayed(By.text("Steps"))
        }
    }

    @Test
    fun dataAccess_deleteCategoryData_showsDeleteDataRanges() {
        insertRecords(listOf(stepsRecordFromTestApp(Instant.now().minus(Duration.ofDays(20)))))
        context.launchMainActivity {
            clickOnText("Data and access")
            clickOnText("Activity")
            clickOnText("Steps")

            clickOnText("Delete this data")
            waitDisplayed(By.text("Delete last 7 days"))
            waitDisplayed(By.text("Delete last 30 days"))
            waitDisplayed(By.text("Delete all data"))
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
        private const val TAG = "DataAccessFragmentTest"
    }
}
