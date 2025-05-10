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

package android.healthconnect.cts.ui

import android.health.connect.HealthPermissions.READ_ACTIVITY_INTENSITY
import android.health.connect.HealthPermissions.WRITE_ACTIVITY_INTENSITY
import android.health.connect.datatypes.ActivityIntensityRecord
import android.health.connect.datatypes.RespiratoryRateRecord
import android.health.connect.datatypes.StepsRecord
import android.healthconnect.testing.shared.recordfactory.RecordFactory.YESTERDAY_11AM
import android.healthconnect.testing.shared.recordfactory.RecordFactory.newEmptyMetadata
import android.platform.test.annotations.RequiresFlagsEnabled
import android.text.format.DateFormat.is24HourFormat
import com.android.healthfitness.flags.Flags.FLAG_ACTIVITY_INTENSITY

@RequiresFlagsEnabled(FLAG_ACTIVITY_INTENSITY)
class ActivityIntensityTest : BaseDataTypeTest<ActivityIntensityRecord>() {

    override val dataTypeString = "Activity intensity"
    override val dataCategoryString = "Activity"
    override val permissionString = "Activity intensity"
    override val permissions = listOf(READ_ACTIVITY_INTENSITY, WRITE_ACTIVITY_INTENSITY)

    override val sameCategoryDataTypeString = "Steps"
    override val anotherCategoryString = "Vitals"

    override fun createRecord() =
        ActivityIntensityRecord.Builder(
                newEmptyMetadata(),
                YESTERDAY_11AM.toInstant(),
                YESTERDAY_11AM.plusMinutes(15).toInstant(),
                ActivityIntensityRecord.ACTIVITY_INTENSITY_TYPE_VIGOROUS,
            )
            .build()

    override val expectedRecordHeader =
        if (is24HourFormat(context)) "11:00 - 11:15 • ${context.packageName}"
        else "11:00 AM - 11:15 AM • ${context.packageName}"

    override val expectedRecordTitle = "Vigorous"
    override val expectedRecordSubtitle = null

    override fun createRecordToBeDeleted() =
        ActivityIntensityRecord.Builder(
                newEmptyMetadata(),
                YESTERDAY_11AM.plusHours(3).toInstant(),
                YESTERDAY_11AM.plusHours(4).plusMinutes(29).toInstant(),
                ActivityIntensityRecord.ACTIVITY_INTENSITY_TYPE_MODERATE,
            )
            .build()

    override val expectedRecordToBeDeletedHeader =
        if (is24HourFormat(context)) "14:00 - 15:29 • ${context.packageName}"
        else "2:00 PM - 3:29 PM • ${context.packageName}"

    override val expectedRecordToBeDeletedTitle = "Moderate"

    override fun createSameCategoryRecord() =
        StepsRecord.Builder(
                newEmptyMetadata(),
                YESTERDAY_11AM.minusDays(1).minusHours(2).toInstant(),
                YESTERDAY_11AM.minusDays(1).minusHours(1).toInstant(),
                50,
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
