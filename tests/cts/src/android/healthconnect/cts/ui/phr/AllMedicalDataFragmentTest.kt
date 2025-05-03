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

import android.healthconnect.cts.lib.ActivityLauncher.launchMainActivity
import android.healthconnect.cts.lib.TestAppProxy
import android.healthconnect.cts.lib.UiTestUtils.clickOnTextAndWaitForNewWindow
import android.healthconnect.cts.lib.UiTestUtils.findText
import android.healthconnect.cts.lib.UiTestUtils.navigateToNewPage
import android.healthconnect.cts.lib.UiTestUtils.scrollDownToAndFindText
import android.healthconnect.cts.ui.HealthConnectBaseTest
import android.healthconnect.cts.utils.TestUtils
import android.healthconnect.testing.shared.phr.PhrDataFactory.FHIR_DATA_ALLERGY
import android.healthconnect.testing.shared.phr.PhrDataFactory.FHIR_DATA_IMMUNIZATION
import android.healthconnect.testing.shared.phr.PhrDataFactory.getCreateMedicalDataSourceRequest
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AllMedicalDataFragmentTest : HealthConnectBaseTest() {

    @get:Rule val mCheckFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    private val APP_A_WITH_READ_WRITE_PERMS: TestAppProxy =
        TestAppProxy.forPackageName("android.healthconnect.cts.testapp.readWritePerms.A")

    @Before
    fun setup() {
        TestUtils.deleteAllStagedRemoteData()
        TestUtils.deleteAllMedicalData()
        insertMedicalData()
    }

    @After
    fun tearDown() {
        TestUtils.deleteAllStagedRemoteData()
        TestUtils.deleteAllMedicalData()
    }

    private fun insertMedicalData() {
        val dataSource =
            APP_A_WITH_READ_WRITE_PERMS.createMedicalDataSource(getCreateMedicalDataSourceRequest())
        APP_A_WITH_READ_WRITE_PERMS.upsertMedicalResource(dataSource.id, FHIR_DATA_IMMUNIZATION)
        APP_A_WITH_READ_WRITE_PERMS.upsertMedicalResource(dataSource.id, FHIR_DATA_ALLERGY)
    }

    @Test
    fun allMedicalDataFragment_showsAvailableDataTypes() {
        context.launchMainActivity {
            navigateToNewPage("Browse health records")

            scrollDownToAndFindText("Allergies")
            scrollDownToAndFindText("Vaccines")
        }
    }

    @Test
    fun allMedicalDataFragment_clickOnPermissionType_navigatesToEntriesAndAccess() {
        context.launchMainActivity {
            navigateToNewPage("Browse health records")

            scrollDownToAndFindText("Allergies")
            clickOnTextAndWaitForNewWindow("Allergies")
            findText("Entries")
            findText("Access")
        }
    }
}
