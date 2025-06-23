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
package android.healthconnect.cts.ui.phr

import android.health.connect.datatypes.HeartRateRecord
import android.health.connect.datatypes.MenstruationPeriodRecord
import android.health.connect.datatypes.Record
import android.health.connect.datatypes.SleepSessionRecord
import android.health.connect.datatypes.StepsRecord
import android.healthconnect.cts.ui.HealthConnectBaseTest
import android.healthconnect.testing.cts.TestUtils
import android.healthconnect.testing.cts.testapphelpers.TestAppProxy
import android.healthconnect.testing.cts.ui.ActivityLauncher.launchMainActivity
import android.healthconnect.testing.cts.ui.UiTestUtils.findObject
import android.healthconnect.testing.cts.ui.UiTestUtils.findText
import android.healthconnect.testing.cts.ui.UiTestUtils.navigateToNewPage
import android.healthconnect.testing.cts.ui.UiTestUtils.navigateToSeeAppData
import android.healthconnect.testing.cts.ui.UiTestUtils.scrollDownToAndFindText
import android.healthconnect.testing.cts.ui.UiTestUtils.scrollUpToAndFindText
import android.healthconnect.testing.shared.phr.PhrDataFactory.FHIR_DATA_ALLERGY
import android.healthconnect.testing.shared.phr.PhrDataFactory.FHIR_DATA_IMMUNIZATION
import android.healthconnect.testing.shared.phr.PhrDataFactory.getCreateMedicalDataSourceRequest
import android.healthconnect.testing.shared.recordfactory.RecordFactory.newEmptyMetadata
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import androidx.test.uiautomator.By
import java.time.Instant
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MedicalAppDataFragmentTest : HealthConnectBaseTest() {

    @get:Rule val mCheckFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    private val APP_A_WITH_READ_WRITE_PERMS: TestAppProxy =
        TestAppProxy.forPackageName("android.healthconnect.cts.testapp.readWritePerms.A")

    @Before
    fun setup() {
        TestUtils.deleteAllDataFromHealthConnect()
        insertMedicalData()
    }

    @After
    fun tearDown() {
        TestUtils.deleteAllDataFromHealthConnect()
    }

    private fun insertMedicalData() {
        val dataSource =
            APP_A_WITH_READ_WRITE_PERMS.createMedicalDataSource(getCreateMedicalDataSourceRequest())
        APP_A_WITH_READ_WRITE_PERMS.upsertMedicalResource(dataSource.id, FHIR_DATA_IMMUNIZATION)
        APP_A_WITH_READ_WRITE_PERMS.upsertMedicalResource(dataSource.id, FHIR_DATA_ALLERGY)
    }

    @Test
    fun medicalAppData_showsAvailableDataTypes() {
        context.launchMainActivity {
            navigateToSeeAppData("CtsHealthConnectTestAppAWithNormalReadWritePermission")

            // This string should be at the top of the screen
            scrollUpToAndFindText("CtsHealthConnectTestAppAWithNormalReadWritePermission")
            scrollDownToAndFindText("Allergies")
            scrollDownToAndFindText("Vaccines")
        }
    }

    @Test
    fun appWithFitnessAndMedicalData_showsBothTypes() {
        val NOW = Instant.now()
        APP_A_WITH_READ_WRITE_PERMS.insertRecords(
            mutableListOf(
                StepsRecord.Builder(newEmptyMetadata(), NOW, NOW.plusSeconds(2), 10).build(),
                HeartRateRecord.Builder(
                        newEmptyMetadata(),
                        NOW,
                        NOW.plusSeconds(10),
                        listOf(HeartRateRecord.HeartRateSample(140, NOW)),
                    )
                    .build(),
                MenstruationPeriodRecord.Builder(newEmptyMetadata(), NOW, NOW.plusSeconds(10))
                    .build(),
                SleepSessionRecord.Builder(newEmptyMetadata(), NOW, NOW.plusSeconds(1000)).build(),
            )
                as List<Record>?
        )
        context.launchMainActivity {
            navigateToSeeAppData("CtsHealthConnectTestAppAWithNormalReadWritePermission")
            // This string should be at the top of the screen
            scrollUpToAndFindText("CtsHealthConnectTestAppAWithNormalReadWritePermission")

            scrollDownToAndFindText("Activity")
            scrollDownToAndFindText("Steps")
            scrollDownToAndFindText("Cycle tracking")
            scrollDownToAndFindText("Menstruation")
            scrollDownToAndFindText("Sleep")
            scrollDownToAndFindText("Vitals")
            scrollDownToAndFindText("Heart rate")
            scrollDownToAndFindText("Health records")
            scrollDownToAndFindText("Allergies")
            scrollDownToAndFindText("Vaccines")
        }
    }

    @Test
    fun clickOnMedicalAppDataType_navigatesToMedicalAppEntries() {
        context.launchMainActivity {
            navigateToSeeAppData("CtsHealthConnectTestAppAWithNormalReadWritePermission")

            findText("CtsHealthConnectTestAppAWithNormalReadWritePermission")
            navigateToNewPage("Allergies")

            findText("CtsHealthConnectTestAppAWithNormalReadWritePermission")
            findObject(By.textContains("Hospital X"))
        }
    }
}
