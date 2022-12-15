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
package com.android.healthconnect.controller.tests.utils

import android.healthconnect.datatypes.BasalMetabolicRateRecord
import android.healthconnect.datatypes.DataOrigin
import android.healthconnect.datatypes.Device
import android.healthconnect.datatypes.HeartRateRecord
import android.healthconnect.datatypes.Metadata
import android.healthconnect.datatypes.StepsRecord
import android.healthconnect.datatypes.units.Power
import com.android.healthconnect.controller.dataentries.units.PowerConverter
import com.android.healthconnect.controller.shared.AppMetadata
import java.time.Instant

val NOW: Instant = Instant.parse("2022-10-20T07:06:05.432Z")

fun getHeartRateRecord(heartRateValues: List<Long>, startTime: Instant = NOW): HeartRateRecord {
    return HeartRateRecord.Builder(
            getMetaData(),
            startTime,
            startTime.plusSeconds(2),
            heartRateValues.map { HeartRateRecord.HeartRateSample(it, NOW) })
        .build()
}

fun getStepsRecord(steps: Long, time: Instant = NOW): StepsRecord {
    return StepsRecord.Builder(getMetaData(), time, time.plusSeconds(2), steps).build()
}

fun getBasalMetabolicRateRecord(calories: Long): BasalMetabolicRateRecord {
    val watts = PowerConverter.convertWattsFromCalories(calories)
    return BasalMetabolicRateRecord.Builder(getMetaData(), NOW, Power.fromWatts(watts)).build()
}

fun getMetaData(): Metadata {
    val device: Device =
        Device.Builder().setManufacturer("google").setModel("Pixel4a").setType(2).build()
    val dataOrigin = DataOrigin.Builder().setPackageName(TEST_APP_PACKAGE_NAME).build()
    return Metadata.Builder()
        .setId("test_id")
        .setDevice(device)
        .setDataOrigin(dataOrigin)
        .setClientRecordId("BMR" + Math.random().toString())
        .build()
}

// region apps

const val TEST_APP_PACKAGE_NAME = "android.healthconnect.controller.test.app"
const val TEST_APP_PACKAGE_NAME_2 = "android.healthconnect.controller.test.app2"
const val TEST_APP_NAME = "Health Connect test app"
const val TEST_APP_NAME_2 = "Health Connect test app 2"

val TEST_APP =
    AppMetadata(packageName = TEST_APP_PACKAGE_NAME, appName = TEST_APP_NAME, icon = null)
val TEST_APP_2 =
    AppMetadata(packageName = TEST_APP_PACKAGE_NAME_2, appName = TEST_APP_NAME_2, icon = null)

// endregion
