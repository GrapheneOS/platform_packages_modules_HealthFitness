/*
 * Copyright (C) 2023 The Android Open Source Project
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

import android.healthconnect.testing.cts.TestUtils
import android.healthconnect.testing.cts.ui.ActivityLauncher.launchMainActivity
import android.healthconnect.testing.cts.ui.UiTestUtils.findText
import android.healthconnect.testing.cts.ui.UiTestUtils.navigateToNewPage
import org.junit.Test

/** CTS test for HealthConnect Manage data screen. */
class ManageDataFragmentTest : HealthConnectBaseTest() {

    companion object {
        private const val THREE_MONTHS = 3 * 30
    }

    @Test
    fun manageDataFragment_never_showsAutoDeleteOption() {
        TestUtils.setRecordRetentionPeriodInDays(0)

        context.launchMainActivity {
            navigateToNewPage("Manage data")
            findText("Auto-delete")
            findText("Off")
        }
    }

    @Test
    fun manageDataFragment_3months_showsAutoDeleteOption() {
        TestUtils.setRecordRetentionPeriodInDays(THREE_MONTHS)

        context.launchMainActivity {
            navigateToNewPage("Manage data")
            findText("Auto-delete")
            findText("After 3 months")
        }
    }
}
