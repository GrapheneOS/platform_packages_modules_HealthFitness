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
import android.healthconnect.cts.lib.ActivityLauncher.launchMainActivity
import android.healthconnect.cts.lib.TestAppProxy
import android.healthconnect.cts.lib.UiTestUtils.navigateToNewPage
import android.healthconnect.cts.lib.UiTestUtils.scrollDownToAndFindText
import android.healthconnect.cts.lib.UiTestUtils.scrollDownToAndFindTextContains
import android.healthconnect.cts.lib.UiTestUtils.scrollToEnd
import android.healthconnect.cts.lib.UiTestUtils.verifyTextNotFound
import android.healthconnect.cts.phr.utils.PhrDataFactory.FHIR_DATA_IMMUNIZATION
import android.healthconnect.cts.phr.utils.PhrDataFactory.getCreateMedicalDataSourceRequest
import android.healthconnect.cts.utils.DataFactory.getEmptyMetadata
import android.healthconnect.cts.utils.DeviceSupportUtils
import android.healthconnect.cts.utils.TestUtils
import android.platform.test.annotations.RequiresFlagsDisabled
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import com.android.healthfitness.flags.Flags.FLAG_PERSONAL_HEALTH_RECORD
import com.android.healthfitness.flags.Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE
import com.android.settingslib.widget.theme.flags.Flags.FLAG_IS_EXPRESSIVE_DESIGN_ENABLED
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

            TestUtils.deleteAllStagedRemoteData()
            TestUtils.deleteAllMedicalData()

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
            TestUtils.deleteAllStagedRemoteData()
            TestUtils.deleteAllMedicalData()
        }
    }

    @Test
    fun homeFragment_opensAppPermissions() {
        context.launchMainActivity {
            navigateToNewPage("App permissions")

            scrollDownToAndFindText("Allowed access")
            scrollDownToAndFindText("Not allowed access")
        }
    }

    @Test
    fun homeFragment_opensDataManagement() {
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
    @RequiresFlagsDisabled(FLAG_IS_EXPRESSIVE_DESIGN_ENABLED)
    fun homeFragment_legacyRecentAccessShownOnHomeScreen() {
        context.launchMainActivity {
            scrollDownToAndFindTextContains("CtsHealthConnectTest")
            scrollDownToAndFindText("See all recent access")
        }
    }

    @Test
    @RequiresFlagsEnabled(FLAG_IS_EXPRESSIVE_DESIGN_ENABLED)
    fun homeFragment_expressiveRecentAccessShownOnHomeScreen() {
        context.launchMainActivity {
            scrollDownToAndFindTextContains("CtsHealthConnectTest")
            scrollDownToAndFindText("View all")
        }
    }

    @Test
    @RequiresFlagsDisabled(FLAG_IS_EXPRESSIVE_DESIGN_ENABLED)
    fun homeFragment_navigatesToLegacyRecentAccess() {
        context.launchMainActivity {
            navigateToNewPage("See all recent access")

            scrollDownToAndFindText("Today")
            scrollDownToAndFindTextContains("CtsHealthConnectTest")
        }
    }

    @Test
    @RequiresFlagsEnabled(FLAG_IS_EXPRESSIVE_DESIGN_ENABLED)
    fun homeFragment_navigatesToExpressiveRecentAccess() {
        context.launchMainActivity {
            navigateToNewPage("View all")

            scrollDownToAndFindText("Today")
            scrollDownToAndFindTextContains("CtsHealthConnectTest")
        }
    }

    @Test
    @RequiresFlagsEnabled(FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    fun homeFragment_withMedicalData_opensBrowseMedicalRecords() {
        val dataSource =
            APP_A_WITH_READ_WRITE_PERMS.createMedicalDataSource(getCreateMedicalDataSourceRequest())
        APP_A_WITH_READ_WRITE_PERMS.upsertMedicalResource(dataSource.id, FHIR_DATA_IMMUNIZATION)
        context.launchMainActivity {
            navigateToNewPage("Browse health records")

            scrollDownToAndFindText("Vaccines")
        }
    }

    @Test
    @RequiresFlagsDisabled(FLAG_PERSONAL_HEALTH_RECORD)
    fun homeFragment_withMedicalData_flagOff_hidesBrowseMedicalRecords() {
        context.launchMainActivity {
            scrollToEnd()
            verifyTextNotFound("Browse health records")
        }
    }
}
