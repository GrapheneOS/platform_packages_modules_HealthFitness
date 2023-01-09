/**
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * ```
 *      http://www.apache.org/licenses/LICENSE-2.0
 * ```
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.shared

import android.healthconnect.datatypes.ActiveCaloriesBurnedRecord
import android.healthconnect.datatypes.BasalBodyTemperatureRecord
import android.healthconnect.datatypes.BasalMetabolicRateRecord
import android.healthconnect.datatypes.BodyFatRecord
import android.healthconnect.datatypes.BodyTemperatureRecord
import android.healthconnect.datatypes.DistanceRecord
import android.healthconnect.datatypes.ElevationGainedRecord
import android.healthconnect.datatypes.FloorsClimbedRecord
import android.healthconnect.datatypes.HeartRateRecord
import android.healthconnect.datatypes.HeightRecord
import android.healthconnect.datatypes.HydrationRecord
import android.healthconnect.datatypes.OxygenSaturationRecord
import android.healthconnect.datatypes.PowerRecord
import android.healthconnect.datatypes.Record
import android.healthconnect.datatypes.RespiratoryRateRecord
import android.healthconnect.datatypes.RestingHeartRateRecord
import android.healthconnect.datatypes.SpeedRecord
import android.healthconnect.datatypes.StepsCadenceRecord
import android.healthconnect.datatypes.StepsRecord
import android.healthconnect.datatypes.TotalCaloriesBurnedRecord
import android.healthconnect.datatypes.WheelchairPushesRecord

enum class DataType(val recordClass: Class<out Record>) {
    ACTIVE_CALORIES_BURNED(ActiveCaloriesBurnedRecord::class.java),
    BASAL_METABOLIC_RATE(BasalMetabolicRateRecord::class.java),
    DISTANCE(DistanceRecord::class.java),
    HEART_RATE(HeartRateRecord::class.java),
    POWER(PowerRecord::class.java),
    SPEED(SpeedRecord::class.java),
    STEPS(StepsRecord::class.java),
    STEPS_CADENCE(StepsCadenceRecord::class.java),
    TOTAL_CALORIES_BURNED(TotalCaloriesBurnedRecord::class.java),
    HEIGHT(HeightRecord::class.java),
    BODY_FAT(BodyFatRecord::class.java),
    OXYGEN_SATURATION(OxygenSaturationRecord::class.java),
    BODY_TEMPERATURE(BodyTemperatureRecord::class.java),
    BASAL_BODY_TEMPERATURE(BasalBodyTemperatureRecord::class.java),
    WHEELCHAIR_PUSHES(WheelchairPushesRecord::class.java),
    RESTING_HEART_RATE(RestingHeartRateRecord::class.java),
    RESPIRATORY_RATE(RespiratoryRateRecord::class.java),
    HYDRATION(HydrationRecord::class.java),
    FLOORS_CLIMBED(FloorsClimbedRecord::class.java),
    ELEVATION_GAINED(ElevationGainedRecord::class.java),
}
