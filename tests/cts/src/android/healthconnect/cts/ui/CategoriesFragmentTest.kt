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
import android.health.connect.datatypes.Record
import android.health.connect.datatypes.StepsRecord
import android.healthconnect.cts.TestUtils.insertRecords
import android.healthconnect.cts.TestUtils.verifyDeleteRecords
import android.healthconnect.cts.ui.testing.ActivityLauncher.launchMainActivity
import android.healthconnect.cts.ui.testing.UiTestUtils.clickOnText
import android.healthconnect.cts.ui.testing.UiTestUtils.navigateBackToHomeScreen
import android.healthconnect.cts.ui.testing.UiTestUtils.navigateUp
import android.healthconnect.cts.ui.testing.UiTestUtils.stepsRecordFromTestApp
import android.healthconnect.cts.ui.testing.UiTestUtils.waitDisplayed
import android.healthconnect.cts.ui.testing.UiTestUtils.waitNotDisplayed
import androidx.test.uiautomator.By
import java.time.Duration.ofDays
import java.time.Instant
import org.junit.After
import org.junit.Test

/** CTS test for HealthConnect Categories screen. */
class CategoriesFragmentTest : HealthConnectBaseTest() {

    @Test
    fun categoriesFragment_openAllCategories() {
        val records: List<Record> = listOf(stepsRecordFromTestApp(), stepsRecordFromTestApp())
        insertRecords(records)

        context.launchMainActivity {
            clickOnText("Data and access")
            clickOnText("See all categories")
            waitDisplayed(By.text("Nutrition"))
        }
    }

    @Test
    fun categoriesFragment_setAutoDelete() {
        context.launchMainActivity {
            clickOnText("Data and access")

            clickOnText("Auto-delete")
            clickOnText("Never")

            navigateUp()
            waitDisplayed(By.text("Never"))

            clickOnText("Auto-delete")
            clickOnText("After 3 months")
            clickOnText("Set auto-delete")
            clickOnText("Done")

            navigateUp()
            waitDisplayed(By.text("After 3 months"))
        }
    }

    @Test
    fun categoriesFragment_deleteAllData() {
        val records: List<Record> = listOf(stepsRecordFromTestApp(Instant.now().minus(ofDays(100))))
        insertRecords(records)

        context.launchMainActivity {
            clickOnText("Data and access")
            waitDisplayed(By.text("Activity"))

            clickOnText("Delete all data")
            clickOnText("Delete all data")
            clickOnText("Next")
            clickOnText("Delete")
            clickOnText("Done")

            waitNotDisplayed(By.text("Activity"))
        }
    }

    @Test
    fun categoriesFragment_deleteFromTimeRange() {
        insertRecords(listOf(stepsRecordFromTestApp(Instant.now().minus(ofDays(20)))))

        context.launchMainActivity {
            clickOnText("Data and access")
            waitDisplayed(By.text("Activity"))

            clickOnText("Delete all data")
            clickOnText("Delete last 7 days")
            clickOnText("Next")
            clickOnText("Delete")
            clickOnText("Done")

            waitDisplayed(By.text("Activity"))

            clickOnText("Delete all data")
            clickOnText("Delete last 30 days")
            clickOnText("Next")
            clickOnText("Delete")
            clickOnText("Done")

            navigateUp()
            waitNotDisplayed(By.text("Activity"))
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
        private const val TAG = "CategoriesFragmentTest"
    }
}
