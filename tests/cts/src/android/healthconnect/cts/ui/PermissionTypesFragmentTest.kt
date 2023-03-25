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
import android.healthconnect.cts.ui.testing.ActivityLauncher.launchDataActivity
import android.healthconnect.cts.ui.testing.UiTestUtils.clickOnText
import android.healthconnect.cts.ui.testing.UiTestUtils.distanceRecordFromTestApp
import android.healthconnect.cts.ui.testing.UiTestUtils.stepsRecordFromTestApp
import android.healthconnect.cts.ui.testing.UiTestUtils.stepsRecordFromTestApp2
import android.healthconnect.cts.ui.testing.UiTestUtils.waitDisplayed
import androidx.test.uiautomator.By
import java.time.Instant
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test

/** CTS test for HealthConnect Permission types screen. */
class PermissionTypesFragmentTest : HealthConnectBaseTest() {

    companion object {
        private const val TAG = "DataAccessFragmentTest"

        @BeforeClass
        @JvmStatic
        fun setup() {
            insertRecords(listOf(stepsRecordFromTestApp()))
            insertRecords(listOf(distanceRecordFromTestApp()))
            insertRecords(listOf(stepsRecordFromTestApp2()))
        }

        @AfterClass
        @JvmStatic
        fun teardown() {
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
        }
    }

    @Test
    fun permissionTypes_navigateToPermissionTypes() {
        context.launchDataActivity { clickOnText("Activity") }
    }

    @Test
    fun permissionTypes_showsDeleteCategoryData() {
        context.launchDataActivity {
            clickOnText("Activity")
            waitDisplayed(By.text("Delete activity data"))
        }
    }

    @Test
    fun permissionTypes_filterByApp() {
        context.launchDataActivity {
            clickOnText("Activity")
            waitDisplayed(By.text("Distance"))
            waitDisplayed(By.text("Steps"))

            //            clickOnText("TestApp2")
            //            waitNotDisplayed(By.text("Distance"))
            //            waitDisplayed(By.text("Steps"))
            //
            //            clickOnText("All apps")
            //            waitDisplayed(By.text("Distance"))
            //            waitDisplayed(By.text("Steps"))
        }
    }

    @Test
    fun permissionTypes_openAppPriority() {
        // TODO(b/265789268): Finish when ag/21642785 is merged.
        context.launchDataActivity {
            clickOnText("Activity")
            waitDisplayed(By.text("Distance"))
            waitDisplayed(By.text("Steps"))

            //            clickOnText("App priority")
            //            waitNotDisplayed(By.text("Distance"))
            //            waitDisplayed(By.text("Set app priority"))
            //            waitDisplayed(By.text("If more than one app adds Activity data, Health
            // Connect prioritises the app highest in this list. Drag apps to reorder them."))
            //            waitDisplayed(By.text("Cancel"))
            //            clickOnText("Save")
        }
    }
}
