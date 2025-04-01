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

import android.health.connect.datatypes.HeartRateRecord
import android.health.connect.datatypes.HeightRecord
import android.health.connect.datatypes.HydrationRecord
import android.health.connect.datatypes.OvulationTestRecord
import android.health.connect.datatypes.OvulationTestRecord.OvulationTestResult
import android.health.connect.datatypes.SleepSessionRecord
import android.health.connect.datatypes.StepsRecord
import android.health.connect.datatypes.units.Length
import android.health.connect.datatypes.units.Volume
import android.healthconnect.cts.lib.ActivityLauncher.launchDataActivity
import android.healthconnect.cts.lib.RecordFactory.newEmptyMetadata
import android.healthconnect.cts.lib.UiTestUtils.clickOnDescAndWaitForNewWindow
import android.healthconnect.cts.lib.UiTestUtils.findObjectAndClick
import android.healthconnect.cts.lib.UiTestUtils.findText
import android.healthconnect.cts.lib.UiTestUtils.findTextAndClick
import android.healthconnect.cts.lib.UiTestUtils.navigateToNewPage
import android.healthconnect.cts.lib.UiTestUtils.scrollDownToAndFindText
import android.healthconnect.cts.lib.UiTestUtils.scrollUpTo
import android.healthconnect.cts.lib.UiTestUtils.verifyObjectNotFound
import android.healthconnect.cts.lib.UiTestUtils.waitDisplayed
import android.healthconnect.cts.ui.HealthConnectBaseTest
import android.healthconnect.cts.utils.TestUtils
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import androidx.test.uiautomator.By
import com.android.compatibility.common.util.SystemUtil.runShellCommand
import java.time.Duration
import java.time.Instant
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** CTS test for Health Connect All Data fragment. */
class AllDataFragmentTest : HealthConnectBaseTest() {
    @get:Rule val mCheckFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    private var previousDate = ""

    @Before
    fun setup() {
        previousDate = runShellCommand("date +%Y-%m-%d")
        // Don't throw if failed to set the system time as this might not be available on all
        // devices. As tests are generally expected to be running with the current real-world time
        // this usually isn't an issue. However some tests might have previously had the device time
        // set to the past and this protects against that as Health Connect records generally can't
        // be in the future.
        runShellCommand("su 0 date -s $TEST_SYSTEM_CLOCK_TIME")
        TestUtils.deleteAllStagedRemoteData()
        insertData()
    }

    @After
    fun tearDown() {
        TestUtils.deleteAllStagedRemoteData()
        if (!previousDate.isEmpty()) {
            runShellCommand("su 0 date -s $previousDate")
        }
    }

    companion object {
        private const val TEST_SYSTEM_CLOCK_TIME: String = "2025-03-31"
        private val NOW: Instant = Instant.parse("2024-01-20T07:06:05.432Z")
    }

    @Test
    fun allDataFragment_showsAllAvailableDataTypes() {
        context.launchDataActivity {
            scrollDownToAndFindText("Activity")
            scrollDownToAndFindText("Steps")
            scrollDownToAndFindText("Body measurements")
            scrollDownToAndFindText("Height")
            scrollDownToAndFindText("Cycle tracking")
            scrollDownToAndFindText("Ovulation test")
            scrollDownToAndFindText("Sleep")
            scrollDownToAndFindText("Vitals")
            scrollDownToAndFindText("Heart rate")
        }
    }

    @Test
    fun allDataFragment_clickOnDataSourcesIcon_navigatesToDataSources() {
        context.launchDataActivity {
            clickOnDescAndWaitForNewWindow("Data sources and priority")
            scrollDownToAndFindText("App sources")
        }
    }

    @Test
    fun allDataFragment_deletesAllData() {
        context.launchDataActivity {
            findText("Activity")
            findText("Steps")
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

    @Test
    fun allDataFragment_clickOnPermissionType_navigatesToEntriesAndAccess() {
        context.launchDataActivity {
            findText("Activity")
            navigateToNewPage("Steps")

            findText("Entries")
            findText("Access")
        }
    }

    private fun insertData() {
        TestUtils.insertRecords(
            mutableListOf(
                StepsRecord.Builder(newEmptyMetadata(), NOW, NOW.plusSeconds(2), 10).build(),
                HeightRecord.Builder(newEmptyMetadata(), NOW, Length.fromMeters(1.75)).build(),
                HeartRateRecord.Builder(
                        newEmptyMetadata(),
                        NOW,
                        NOW.plusSeconds(10),
                        listOf(HeartRateRecord.HeartRateSample(140, NOW)),
                    )
                    .build(),
                HydrationRecord.Builder(
                        newEmptyMetadata(),
                        NOW,
                        NOW.plusSeconds(100),
                        Volume.fromLiters(0.5),
                    )
                    .build(),
                OvulationTestRecord.Builder(
                        newEmptyMetadata(),
                        NOW,
                        OvulationTestResult.RESULT_INCONCLUSIVE,
                    )
                    .build(),
                SleepSessionRecord.Builder(newEmptyMetadata(), NOW, NOW.plusSeconds(1000)).build(),
            )
        )
    }
}
