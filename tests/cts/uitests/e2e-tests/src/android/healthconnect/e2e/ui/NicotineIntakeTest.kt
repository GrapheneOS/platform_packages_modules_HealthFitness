/**
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package android.healthconnect.cts.ui

import android.health.connect.HealthPermissions.READ_NICOTINE_INTAKE
import android.health.connect.HealthPermissions.WRITE_NICOTINE_INTAKE
import android.health.connect.datatypes.MindfulnessSessionRecord
import android.health.connect.datatypes.MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_BREATHING
import android.health.connect.datatypes.NicotineIntakeRecord
import android.health.connect.datatypes.NicotineIntakeRecord.NICOTINE_INTAKE_TYPE_CIGARETTE
import android.health.connect.datatypes.NicotineIntakeRecord.NICOTINE_INTAKE_TYPE_VAPE
import android.health.connect.datatypes.RespiratoryRateRecord
import android.health.connect.datatypes.units.Mass
import android.healthconnect.testing.shared.recordfactory.RecordFactory.YESTERDAY_11AM
import android.healthconnect.testing.shared.recordfactory.RecordFactory.newEmptyMetadata
import android.platform.test.annotations.RequiresFlagsEnabled
import android.text.format.DateFormat.is24HourFormat
import com.android.healthfitness.flags.Flags.FLAG_SMOKING

@RequiresFlagsEnabled(FLAG_SMOKING)
class NicotineIntakeTest : BaseDataTypeTest<NicotineIntakeRecord>() {

    override val dataTypeString = "Nicotine intake"
    override val dataCategoryString = "Wellness"
    override val permissionString = "Nicotine intake"
    override val permissions = listOf(READ_NICOTINE_INTAKE, WRITE_NICOTINE_INTAKE)

    override val sameCategoryDataTypeString = "Mindfulness"
    override val anotherCategoryString = "Vitals"

    override val hasDetailsScreen = false
    override val expectedRecordDetailsHeader = null
    override val expectedRecordDetailsTitle = null

    override fun createRecord() =
        NicotineIntakeRecord.Builder(
                newEmptyMetadata(),
                YESTERDAY_11AM.toInstant(),
                YESTERDAY_11AM.plusMinutes(15).toInstant(),
                7,
                NICOTINE_INTAKE_TYPE_VAPE,
            )
            .setNicotineIntake(Mass.fromGrams(0.25))
            .build()

    override val expectedRecordHeader =
        if (is24HourFormat(context)) "11:00 - 11:15 • ${context.packageName}"
        else "11:00 AM - 11:15 AM • ${context.packageName}"

    override val expectedRecordTitle = "7 vape puffs • 250 mg"
    override val expectedRecordSubtitle = null

    override fun createRecordToBeDeleted() =
        NicotineIntakeRecord.Builder(
                newEmptyMetadata(),
                YESTERDAY_11AM.plusHours(3).toInstant(),
                YESTERDAY_11AM.plusHours(4).plusMinutes(29).toInstant(),
                5,
                NICOTINE_INTAKE_TYPE_CIGARETTE,
            )
            .setNicotineIntake(Mass.fromGrams(0.4))
            .build()

    override val expectedRecordToBeDeletedHeader =
        if (is24HourFormat(context)) "14:00 - 15:29 • ${context.packageName}"
        else "2:00 PM - 3:29 PM • ${context.packageName}"

    override val expectedRecordToBeDeletedTitle = "5 cigarettes • 400 mg"

    override fun createSameCategoryRecord() =
        MindfulnessSessionRecord.Builder(
                newEmptyMetadata(),
                YESTERDAY_11AM.minusDays(1).minusHours(2).toInstant(),
                YESTERDAY_11AM.minusDays(1).minusHours(1).toInstant(),
                MINDFULNESS_SESSION_TYPE_BREATHING,
            )
            .build()

    override fun createAnotherCategoryRecord() =
        RespiratoryRateRecord.Builder(
                newEmptyMetadata(),
                YESTERDAY_11AM.minusDays(1).minusHours(4).toInstant(),
                14.0,
            )
            .build()
}
