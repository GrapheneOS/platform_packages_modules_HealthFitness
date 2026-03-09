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

import android.health.connect.datatypes.StepsRecord
import android.healthconnect.testing.cts.TestUtils
import android.healthconnect.testing.cts.testapphelpers.TestAppProxy
import android.healthconnect.testing.cts.ui.ActivityLauncher.launchMainActivity
import android.healthconnect.testing.cts.ui.UiTestUtils.navigateToAppPermissions
import android.healthconnect.testing.cts.ui.UiTestUtils.navigateToMedicalRecords
import android.healthconnect.testing.cts.ui.UiTestUtils.navigateToNewPage
import android.healthconnect.testing.cts.ui.UiTestUtils.scrollDownToAndFindText
import android.healthconnect.testing.cts.ui.UiTestUtils.scrollDownToAndFindTextContains
import android.healthconnect.testing.shared.DataFactory.getEmptyMetadata
import android.healthconnect.testing.shared.DeviceSupportUtils
import android.healthconnect.testing.shared.phr.PhrDataFactory.FHIR_DATA_IMMUNIZATION
import android.healthconnect.testing.shared.phr.PhrDataFactory.getCreateMedicalDataSourceRequest
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import java.time.Instant
import java.time.temporal.ChronoUnit
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test

/** CTS test for HealthConnect Home screen. */
class HomeFragmentTest : HealthConnectBaseTest() {

    @get:Rule val mCheckFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    companion object {

        private val APP_A_WITH_READ_WRITE_PERMS: TestAppProxy =
            TestAppProxy.forPackageName("android.healthconnect.cts.testapp.readWritePerms.A")

        @JvmStatic
        @BeforeClass
        fun setup() {
            if (!DeviceSupportUtils.isHealthConnectFullySupported()) {
                return
            }

            TestUtils.deleteAllDataFromHealthConnect()

            val now = Instant.now().truncatedTo(ChronoUnit.MILLIS)
            APP_A_WITH_READ_WRITE_PERMS.insertRecords(
                StepsRecord.Builder(getEmptyMetadata(), now.minusSeconds(30), now, 43).build()
            )
        }

        @JvmStatic
        @AfterClass
        fun teardown() {
            if (!DeviceSupportUtils.isHealthConnectFullySupported()) {
                return
            }
            TestUtils.deleteAllDataFromHealthConnect()
        }
    }

    @Test
    fun homeFragment_opensAppPermissions() {
        context.launchMainActivity {
            navigateToAppPermissions()

            scrollDownToAndFindText("Allowed access")
            scrollDownToAndFindText("Not allowed access")
        }
    }

    @Test
    fun homeFragment_opensDataAndAccess() {
        context.launchMainActivity {
            navigateToNewPage("Data and access")

            scrollDownToAndFindText("Activity")
            scrollDownToAndFindText("Steps")
        }
    }

    @Test
    fun homeFragment_opensManageData() {
        context.launchMainActivity {
            navigateToNewPage("Manage data")

            scrollDownToAndFindText("Auto-delete")
            scrollDownToAndFindText("Data sources and priority")
            scrollDownToAndFindText("Set units")
        }
    }

    @Test
    fun homeFragment_navigatesToRecentAccess() {
        context.launchMainActivity {
            navigateToNewPage("Recent access")
            scrollDownToAndFindText("Today")
            scrollDownToAndFindTextContains("CtsHealthConnectTest")
        }
    }

    @Test
    fun homeFragment_showsCombinedData_inDataAndAccess() {
        val dataSource =
            APP_A_WITH_READ_WRITE_PERMS.createMedicalDataSource(getCreateMedicalDataSourceRequest())
        APP_A_WITH_READ_WRITE_PERMS.upsertMedicalResource(dataSource.id, FHIR_DATA_IMMUNIZATION)
        context.launchMainActivity {
            navigateToMedicalRecords()
            scrollDownToAndFindText("Steps")
            scrollDownToAndFindText("Vaccines")
        }
    }
}
