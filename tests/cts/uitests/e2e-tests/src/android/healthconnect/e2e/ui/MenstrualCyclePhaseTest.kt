/*
 * Copyright (C) 2025 The Android Open Source Project
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

import android.health.connect.HealthPermissions
import android.health.connect.datatypes.MenstrualCyclePhaseRecord
import android.health.connect.datatypes.MenstruationPeriodRecord
import android.health.connect.datatypes.RespiratoryRateRecord
import android.healthconnect.testing.cts.TestUtils.readAllRecords
import android.healthconnect.testing.cts.ui.ActivityLauncher.launchDataActivity
import android.healthconnect.testing.cts.ui.UiTestUtils.findDesc
import android.healthconnect.testing.cts.ui.UiTestUtils.findDescAndClick
import android.healthconnect.testing.cts.ui.UiTestUtils.findObject
import android.healthconnect.testing.cts.ui.UiTestUtils.findText
import android.healthconnect.testing.cts.ui.UiTestUtils.findTextAndClick
import android.healthconnect.testing.cts.ui.UiTestUtils.navigateToNewPage
import android.healthconnect.testing.cts.ui.UiTestUtils.scrollDownToAndFindText
import android.healthconnect.testing.cts.ui.UiTestUtils.scrollToEnd
import android.healthconnect.testing.cts.ui.UiTestUtils.verifyTextNotFound
import android.healthconnect.testing.cts.ui.UiTestUtils.waitForObjectNotFound
import android.healthconnect.testing.shared.recordfactory.RecordFactory.newEmptyMetadata
import android.platform.test.annotations.RequiresFlagsEnabled
import androidx.test.uiautomator.By
import com.android.healthfitness.flags.Flags.FLAG_CYCLE_PHASES_FLAG
import com.google.common.truth.Truth.assertThat
import java.time.Duration.ofSeconds
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Test

@RequiresFlagsEnabled(FLAG_CYCLE_PHASES_FLAG)
class MenstrualCyclePhaseTest : BaseDataTypeTest<MenstrualCyclePhaseRecord>() {
    private val testDate: LocalDate = LocalDate.of(2025, 11, 5)
    private val testDate11am: ZonedDateTime =
        LocalDate.of(2025, 11, 5).atTime(11, 0).atZone(ZoneId.systemDefault())

    override val dataTypeString = "Menstrual cycle phase"
    override val dataCategoryString = "Cycle tracking"
    override val permissionString = "Menstrual cycle phase"
    override val permissions =
        listOf(
            HealthPermissions.READ_MENSTRUAL_CYCLE_PHASE,
            HealthPermissions.WRITE_MENSTRUAL_CYCLE_PHASE,
        )

    override val sameCategoryDataTypeString = "Menstruation"
    override val anotherCategoryString = "Vitals"

    override val hasDetailsScreen = false
    override val expectedRecordDetailsHeader = null
    override val expectedRecordDetailsTitle = null

    override fun createRecord(): MenstrualCyclePhaseRecord {
        return MenstrualCyclePhaseRecord.Builder(
                newEmptyMetadata(),
                LocalDate.from(testDate.minusDays(1)),
                MenstrualCyclePhaseRecord.PHASE_FOLLICULAR,
            )
            .setDayOfCycle(5)
            .build()
    }

    override val expectedRecordHeader = "Nov 4 • ${context.packageName}"

    override val expectedRecordTitle = "Follicular Day 5"
    override val expectedRecordSubtitle = null

    override fun createRecordToBeDeleted(): MenstrualCyclePhaseRecord {
        return MenstrualCyclePhaseRecord.Builder(
                newEmptyMetadata(),
                LocalDate.from(testDate),
                MenstrualCyclePhaseRecord.PHASE_LUTEAL,
            )
            .setDayOfCycle(20)
            .build()
    }

    // It's only possible to insert one record per day per app, so we'll delete _the_ record
    override val expectedRecordToBeDeletedHeader = "Nov 5 • ${context.packageName}"
    override val expectedRecordToBeDeletedTitle = "Luteal Day 20"

    override fun createSameCategoryRecord(): MenstruationPeriodRecord {
        return MenstruationPeriodRecord.Builder(
                newEmptyMetadata(),
                testDate11am.minusDays(3).toInstant(),
                testDate11am.minusDays(2).toInstant(),
            )
            .build()
    }

    override fun createAnotherCategoryRecord(): RespiratoryRateRecord {
        return RespiratoryRateRecord.Builder(
                newEmptyMetadata(),
                testDate11am.minusDays(4).toInstant(),
                14.0,
            )
            .build()
    }

    @Test
    override fun dataAndAccess_showsEntriesOfFirstAvailableDay_deletesEntry() {
        context.launchDataActivity {
            scrollDownToAndFindText(dataCategoryString)
            navigateToNewPage(dataTypeString)

            waitForObjectNotFound(By.text("No data"), timeout = ofSeconds(3))
            scrollToEnd()

            findText(expectedRecordToBeDeletedHeader)
            findText(expectedRecordToBeDeletedTitle)
            findDesc("Previous day")

            findDescAndClick("Enter deletion")
            findTextAndClick(expectedRecordToBeDeletedTitle)
            findDescAndClick("Delete data")
            findTextAndClick("Delete")
            findObject(By.text("Done"), timeout = ofSeconds(3))
            findTextAndClick("Done")
            verifyTextNotFound(expectedRecordToBeDeletedTitle)

            findText("No data")

            assertThat(readAllRecords(MenstrualCyclePhaseRecord::class.java))
                .containsExactly(record)
        }
    }
}
