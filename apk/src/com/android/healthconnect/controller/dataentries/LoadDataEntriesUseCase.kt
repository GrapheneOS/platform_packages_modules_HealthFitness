/**
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * ```
 *      http://www.apache.org/licenses/LICENSE-2.0
 * ```
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.dataentries

import android.healthconnect.datatypes.BasalMetabolicRateRecord
import android.healthconnect.datatypes.DataOrigin
import android.healthconnect.datatypes.Device
import android.healthconnect.datatypes.HeartRateRecord
import android.healthconnect.datatypes.Metadata
import android.healthconnect.datatypes.Record
import android.healthconnect.datatypes.StepsRecord
import android.healthconnect.datatypes.units.Power
import com.android.healthconnect.controller.dataentries.formatters.HealthDataEntryFormatter
import com.android.healthconnect.controller.permissions.data.HealthPermissionType
import java.time.Instant
import javax.inject.Inject

class LoadDataEntriesUseCase
@Inject
constructor(private val healthDataEntryFormatter: HealthDataEntryFormatter) {
    suspend operator fun invoke(
        type: HealthPermissionType,
        selectedDate: Instant
    ): List<FormattedDataEntry> {

        val entries: List<Record> =
            when (type) {
                HealthPermissionType.STEPS -> STEPS_MOCKED_LIST
                HealthPermissionType.HEART_RATE -> HEART_RATE_MOCKED_LIST
                HealthPermissionType.BASAL_METABOLIC_RATE -> BASEL_METABOLIC_RATE_LIST
                else -> emptyList()
            }
        return entries.map { record -> healthDataEntryFormatter.format(record) }
    }
}

// TODO(magdi) remove after calling hc apis
private val STEPS_MOCKED_LIST =
    listOf(
        getStepsRecord(10),
        getStepsRecord(200),
        getStepsRecord(31),
    )

// TODO(magdi) remove after calling hc apis
private val HEART_RATE_MOCKED_LIST =
    listOf(
        getHeartRateRecord(listOf(80, 85, 90)),
        getHeartRateRecord(listOf(100, 110, 102)),
        getHeartRateRecord(listOf(60, 65, 50)),
    )

private val BASEL_METABOLIC_RATE_LIST =
    listOf(
        getBasalMetabolicRateRecord(100.0),
        getBasalMetabolicRateRecord(90.3),
        getBasalMetabolicRateRecord(100.3))

private fun getHeartRateRecord(heartRateValues: List<Long>): HeartRateRecord {
    return HeartRateRecord.Builder(
            getMetaData(),
            Instant.now(),
            Instant.now().plusSeconds(2),
            heartRateValues.map { HeartRateRecord.HeartRateSample(it, Instant.now()) })
        .build()
}

private fun getStepsRecord(steps: Long): StepsRecord {
    return StepsRecord.Builder(getMetaData(), Instant.now(), Instant.now().plusSeconds(2), steps)
        .build()
}

private fun getBasalMetabolicRateRecord(record: Double): BasalMetabolicRateRecord {
    return BasalMetabolicRateRecord.Builder(getMetaData(), Instant.now(), Power.fromWatts(record))
        .build()
}

private fun getMetaData(): Metadata {
    val device: Device =
        Device.Builder().setManufacturer("google").setModel("Pixel4a").setType(2).build()
    val dataOrigin =
        DataOrigin.Builder().setPackageName("android.healthconnect.controller.test.app").build()
    return Metadata.Builder()
        .setDevice(device)
        .setDataOrigin(dataOrigin)
        .setClientRecordId("BMR" + Math.random().toString())
        .build()
}
