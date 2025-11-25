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
import android.healthconnect.cts.ui.HealthConnectBaseTest
import android.healthconnect.testing.cts.TestUtils
import android.healthconnect.testing.cts.ui.ActivityLauncher.launchDataActivity
import android.healthconnect.testing.cts.ui.UiTestUtils.clickOnDescAndWaitForNewWindow
import android.healthconnect.testing.cts.ui.UiTestUtils.findObjectAndClick
import android.healthconnect.testing.cts.ui.UiTestUtils.findText
import android.healthconnect.testing.cts.ui.UiTestUtils.findTextAndClick
import android.healthconnect.testing.cts.ui.UiTestUtils.navigateToNewPage
import android.healthconnect.testing.cts.ui.UiTestUtils.scrollDownToAndFindText
import android.healthconnect.testing.cts.ui.UiTestUtils.scrollUpTo
import android.healthconnect.testing.cts.ui.UiTestUtils.verifyObjectNotFound
import android.healthconnect.testing.cts.ui.UiTestUtils.waitDisplayed
import android.healthconnect.testing.shared.recordfactory.RecordFactory.newEmptyMetadata
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import androidx.test.uiautomator.By
import java.time.Duration
import java.time.Instant
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** CTS test for Health Connect All Data fragment. */
class AllDataFragmentTest : HealthConnectBaseTest() {
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
            scrollDownToAndFindText("Data sources")
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
        val pastInstant = Instant.now().minus(Duration.ofDays(100))
        TestUtils.insertRecords(
            mutableListOf(
                StepsRecord.Builder(newEmptyMetadata(), pastInstant, pastInstant.plusSeconds(2), 10)
                    .build(),
                HeightRecord.Builder(newEmptyMetadata(), pastInstant, Length.fromMeters(1.75))
                    .build(),
                HeartRateRecord.Builder(
                        newEmptyMetadata(),
                        pastInstant,
                        pastInstant.plusSeconds(10),
                        listOf(HeartRateRecord.HeartRateSample(140, pastInstant)),
                    )
                    .build(),
                HydrationRecord.Builder(
                        newEmptyMetadata(),
                        pastInstant,
                        pastInstant.plusSeconds(100),
                        Volume.fromLiters(0.5),
                    )
                    .build(),
                OvulationTestRecord.Builder(
                        newEmptyMetadata(),
                        pastInstant,
                        OvulationTestResult.RESULT_INCONCLUSIVE,
                    )
                    .build(),
                SleepSessionRecord.Builder(
                        newEmptyMetadata(),
                        pastInstant,
                        pastInstant.plusSeconds(1000),
                    )
                    .build(),
            )
        )
    }
}
