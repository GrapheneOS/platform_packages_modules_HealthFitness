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
package android.healthconnect.cts.ui.newia

import android.health.connect.datatypes.StepsRecord
import android.healthconnect.cts.ui.HealthConnectBaseTest
import android.healthconnect.testing.cts.TestUtils
import android.healthconnect.testing.cts.ui.ActivityLauncher.launchDataActivity
import android.healthconnect.testing.cts.ui.UiTestUtils.findObjectAndClick
import android.healthconnect.testing.cts.ui.UiTestUtils.findText
import android.healthconnect.testing.cts.ui.UiTestUtils.findTextAndClick
import android.healthconnect.testing.cts.ui.UiTestUtils.navigateToNewPage
import android.healthconnect.testing.cts.ui.UiTestUtils.scrollDownToAndFindText
import android.healthconnect.testing.cts.ui.UiTestUtils.scrollUpTo
import android.healthconnect.testing.cts.ui.UiTestUtils.verifyObjectNotFound
import android.healthconnect.testing.cts.ui.UiTestUtils.verifyTextNotFound
import android.healthconnect.testing.cts.ui.UiTestUtils.waitDisplayed
import android.healthconnect.testing.shared.recordfactory.RecordFactory.newEmptyMetadata
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import androidx.test.uiautomator.By
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** CTS test for Health Connect All Entries fragment. */
class AllEntriesFragmentTest : HealthConnectBaseTest() {

    @get:Rule val mCheckFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    @Before
    fun setup() {
        TestUtils.deleteAllDataFromHealthConnect()
        insertData()
    }

    @After
    fun tearDown() {
        TestUtils.deleteAllDataFromHealthConnect()
    }

    companion object {
        private val TODAY =
            LocalDate.now(ZoneId.systemDefault()).atTime(0, 0).atZone(ZoneId.systemDefault())
    }

    @Test
    fun allEntries_dayView_showsDataOnlyFromDay() {
        context.launchDataActivity {
            findText("Activity")
            navigateToNewPage("Steps")
            findText("Entries")
            findText("Access")

            findText("Today")
            findText("10 steps")
            verifyTextNotFound("20 steps")
        }
    }

    @Test
    fun allEntries_navigationView_showsDayWeekMonth() {
        context.launchDataActivity {
            findText("Activity")
            navigateToNewPage("Steps")
            findText("Entries")
            findText("Access")

            findTextAndClick("Today")
            findText("Day")
            findText("Week")
            findText("Month")
        }
    }

    @Test
    fun allEntries_clickOnAccessTab_navigatesToAccessScreen() {
        context.launchDataActivity {
            findText("Activity")
            navigateToNewPage("Steps")
            findText("Entries")

            findTextAndClick("Access")
            findText("Can read steps")
            scrollDownToAndFindText("Can write steps")
        }
    }

    @Test
    fun allEntries_deletesAllData() {
        context.launchDataActivity {
            findText("Activity")
            navigateToNewPage("Steps")
            findText("Entries")

            verifyObjectNotFound(By.text("Select all"))
            findObjectAndClick(By.desc("Enter deletion"))
            scrollUpTo(By.text("Select all"))
            findTextAndClick("Select all")
            findObjectAndClick(By.desc("Delete data"))
            findTextAndClick("Delete")
            waitDisplayed(By.text("Done"), Duration.ofSeconds(3))
            findTextAndClick("Done")
            findText("No data")
        }
    }

    private fun insertData() {
        TestUtils.insertRecords(
            mutableListOf(
                StepsRecord.Builder(
                        newEmptyMetadata(),
                        TODAY.toInstant(),
                        TODAY.plusMinutes(2).toInstant(),
                        10,
                    )
                    .build(),
                StepsRecord.Builder(
                        newEmptyMetadata(),
                        TODAY.minusDays(1).toInstant(),
                        TODAY.minusDays(1).plusMinutes(1).toInstant(),
                        20,
                    )
                    .build(),
            )
        )
    }
}
