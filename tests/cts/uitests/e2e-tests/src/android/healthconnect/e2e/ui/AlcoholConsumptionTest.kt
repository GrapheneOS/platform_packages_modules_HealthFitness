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

import android.health.connect.HealthPermissions.READ_ALCOHOL_CONSUMPTION
import android.health.connect.HealthPermissions.WRITE_ALCOHOL_CONSUMPTION
import android.health.connect.datatypes.AlcoholConsumptionRecord
import android.health.connect.datatypes.AlcoholConsumptionRecord.ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_BEER
import android.health.connect.datatypes.AlcoholConsumptionRecord.ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_WINE
import android.health.connect.datatypes.AlcoholConsumptionRecord.ALCOHOL_CONSUMPTION_SERVING_SIZE_GLASS
import android.health.connect.datatypes.AlcoholConsumptionRecord.ALCOHOL_CONSUMPTION_SERVING_SIZE_PINT
import android.health.connect.datatypes.MindfulnessSessionRecord
import android.health.connect.datatypes.MindfulnessSessionRecord.MINDFULNESS_SESSION_TYPE_BREATHING
import android.health.connect.datatypes.RespiratoryRateRecord
import android.health.connect.datatypes.units.Percentage
import android.health.connect.datatypes.units.Volume
import android.healthconnect.testing.shared.recordfactory.RecordFactory.YESTERDAY_11AM
import android.healthconnect.testing.shared.recordfactory.RecordFactory.newEmptyMetadata
import android.platform.test.annotations.RequiresFlagsEnabled
import android.text.format.DateFormat.is24HourFormat
import com.android.healthfitness.flags.Flags.FLAG_ALCOHOL_CONSUMPTION
import java.time.ZoneOffset

@RequiresFlagsEnabled(FLAG_ALCOHOL_CONSUMPTION)
class AlcoholConsumptionTest : BaseDataTypeTest<AlcoholConsumptionRecord>() {

    override val dataTypeString = "Alcohol consumption"
    override val dataCategoryString = "Wellness"
    override val permissionString = "Alcohol consumption"
    override val permissions = listOf(READ_ALCOHOL_CONSUMPTION, WRITE_ALCOHOL_CONSUMPTION)

    override val sameCategoryDataTypeString = "Mindfulness"
    override val anotherCategoryString = "Vitals"

    override val hasDetailsScreen = true
    override val expectedRecordDetailsHeader = "Serving Volume"
    override val expectedRecordDetailsTitle = "568 ml"

    override fun createRecord() =
        AlcoholConsumptionRecord.Builder(
                newEmptyMetadata(),
                YESTERDAY_11AM.toInstant(),
                YESTERDAY_11AM.plusMinutes(15).toInstant(),
                7,
                ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_BEER,
            )
            .setServingSize(ALCOHOL_CONSUMPTION_SERVING_SIZE_PINT)
            .setStartZoneOffset(ZoneOffset.ofHours(2))
            .setEndZoneOffset(ZoneOffset.ofHours(2))
            .setServingVolume(Volume.fromLiters(0.568))
            .setAlcoholByVolume(Percentage.fromValue(7.0))
            .build()

    override val expectedRecordHeader =
        if (is24HourFormat(context)) "11:00 - 11:15 • ${context.packageName}"
        else "11:00 AM - 11:15 AM • ${context.packageName}"

    override val expectedRecordTitle = "7 • Beer"
    override val expectedRecordSubtitle = null

    override fun createRecordToBeDeleted() =
        AlcoholConsumptionRecord.Builder(
                newEmptyMetadata(),
                YESTERDAY_11AM.plusHours(3).toInstant(),
                YESTERDAY_11AM.plusHours(4).plusMinutes(29).toInstant(),
                5,
                ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_WINE,
            )
            .setServingSize(ALCOHOL_CONSUMPTION_SERVING_SIZE_GLASS)
            .setStartZoneOffset(ZoneOffset.ofHours(2))
            .setEndZoneOffset(ZoneOffset.ofHours(2))
            .setServingVolume(Volume.fromLiters(0.765))
            .setAlcoholByVolume(Percentage.fromValue(12.1))
            .build()

    override val expectedRecordToBeDeletedHeader =
        if (is24HourFormat(context)) "14:00 - 15:29 • ${context.packageName}"
        else "2:00 PM - 3:29 PM • ${context.packageName}"

    override val expectedRecordToBeDeletedTitle = "5 • Wine"

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
