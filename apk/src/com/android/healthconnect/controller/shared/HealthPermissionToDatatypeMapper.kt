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
import android.healthconnect.datatypes.BloodGlucoseRecord
import android.healthconnect.datatypes.BloodPressureRecord
import android.healthconnect.datatypes.BodyFatRecord
import android.healthconnect.datatypes.BodyTemperatureRecord
import android.healthconnect.datatypes.BodyWaterMassRecord
import android.healthconnect.datatypes.BoneMassRecord
import android.healthconnect.datatypes.CervicalMucusRecord
import android.healthconnect.datatypes.CyclingPedalingCadenceRecord
import android.healthconnect.datatypes.DistanceRecord
import android.healthconnect.datatypes.ElevationGainedRecord
import android.healthconnect.datatypes.ExerciseSessionRecord
import android.healthconnect.datatypes.FloorsClimbedRecord
import android.healthconnect.datatypes.HeartRateRecord
import android.healthconnect.datatypes.HeartRateVariabilityRmssdRecord
import android.healthconnect.datatypes.HeightRecord
import android.healthconnect.datatypes.HydrationRecord
import android.healthconnect.datatypes.IntermenstrualBleedingRecord
import android.healthconnect.datatypes.LeanBodyMassRecord
import android.healthconnect.datatypes.MenstruationFlowRecord
import android.healthconnect.datatypes.MenstruationPeriodRecord
import android.healthconnect.datatypes.NutritionRecord
import android.healthconnect.datatypes.OvulationTestRecord
import android.healthconnect.datatypes.OxygenSaturationRecord
import android.healthconnect.datatypes.PowerRecord
import android.healthconnect.datatypes.Record
import android.healthconnect.datatypes.RespiratoryRateRecord
import android.healthconnect.datatypes.RestingHeartRateRecord
import android.healthconnect.datatypes.SexualActivityRecord
import android.healthconnect.datatypes.SleepSessionRecord
import android.healthconnect.datatypes.SpeedRecord
import android.healthconnect.datatypes.StepsCadenceRecord
import android.healthconnect.datatypes.StepsRecord
import android.healthconnect.datatypes.TotalCaloriesBurnedRecord
import android.healthconnect.datatypes.Vo2MaxRecord
import android.healthconnect.datatypes.WeightRecord
import android.healthconnect.datatypes.WheelchairPushesRecord
import com.android.healthconnect.controller.permissions.data.HealthPermissionType
import com.android.healthconnect.controller.permissions.data.HealthPermissionType.ACTIVE_CALORIES_BURNED
import com.android.healthconnect.controller.permissions.data.HealthPermissionType.BASAL_BODY_TEMPERATURE
import com.android.healthconnect.controller.permissions.data.HealthPermissionType.BASAL_METABOLIC_RATE
import com.android.healthconnect.controller.permissions.data.HealthPermissionType.BLOOD_GLUCOSE
import com.android.healthconnect.controller.permissions.data.HealthPermissionType.BLOOD_PRESSURE
import com.android.healthconnect.controller.permissions.data.HealthPermissionType.BODY_FAT
import com.android.healthconnect.controller.permissions.data.HealthPermissionType.BODY_TEMPERATURE
import com.android.healthconnect.controller.permissions.data.HealthPermissionType.BODY_WATER_MASS
import com.android.healthconnect.controller.permissions.data.HealthPermissionType.BONE_MASS
import com.android.healthconnect.controller.permissions.data.HealthPermissionType.CERVICAL_MUCUS
import com.android.healthconnect.controller.permissions.data.HealthPermissionType.DISTANCE
import com.android.healthconnect.controller.permissions.data.HealthPermissionType.ELEVATION_GAINED
import com.android.healthconnect.controller.permissions.data.HealthPermissionType.EXERCISE
import com.android.healthconnect.controller.permissions.data.HealthPermissionType.FLOORS_CLIMBED
import com.android.healthconnect.controller.permissions.data.HealthPermissionType.HEART_RATE
import com.android.healthconnect.controller.permissions.data.HealthPermissionType.HEART_RATE_VARIABILITY
import com.android.healthconnect.controller.permissions.data.HealthPermissionType.HEIGHT
import com.android.healthconnect.controller.permissions.data.HealthPermissionType.HYDRATION
import com.android.healthconnect.controller.permissions.data.HealthPermissionType.INTERMENSTRUAL_BLEEDING
import com.android.healthconnect.controller.permissions.data.HealthPermissionType.LEAN_BODY_MASS
import com.android.healthconnect.controller.permissions.data.HealthPermissionType.MENSTRUATION
import com.android.healthconnect.controller.permissions.data.HealthPermissionType.NUTRITION
import com.android.healthconnect.controller.permissions.data.HealthPermissionType.OVULATION_TEST
import com.android.healthconnect.controller.permissions.data.HealthPermissionType.OXYGEN_SATURATION
import com.android.healthconnect.controller.permissions.data.HealthPermissionType.POWER
import com.android.healthconnect.controller.permissions.data.HealthPermissionType.RESPIRATORY_RATE
import com.android.healthconnect.controller.permissions.data.HealthPermissionType.RESTING_HEART_RATE
import com.android.healthconnect.controller.permissions.data.HealthPermissionType.SEXUAL_ACTIVITY
import com.android.healthconnect.controller.permissions.data.HealthPermissionType.SLEEP
import com.android.healthconnect.controller.permissions.data.HealthPermissionType.SPEED
import com.android.healthconnect.controller.permissions.data.HealthPermissionType.STEPS
import com.android.healthconnect.controller.permissions.data.HealthPermissionType.TOTAL_CALORIES_BURNED
import com.android.healthconnect.controller.permissions.data.HealthPermissionType.VO2_MAX
import com.android.healthconnect.controller.permissions.data.HealthPermissionType.WEIGHT
import com.android.healthconnect.controller.permissions.data.HealthPermissionType.WHEELCHAIR_PUSHES
import com.google.common.annotations.VisibleForTesting

object HealthPermissionToDatatypeMapper {
    private val map =
        mapOf(
            STEPS to listOf(StepsRecord::class.java, StepsCadenceRecord::class.java),
            HEART_RATE to listOf(HeartRateRecord::class.java),
            BASAL_METABOLIC_RATE to listOf(BasalMetabolicRateRecord::class.java),
            SPEED to listOf(SpeedRecord::class.java),
            DISTANCE to listOf(DistanceRecord::class.java),
            POWER to listOf(PowerRecord::class.java),
            ACTIVE_CALORIES_BURNED to listOf(ActiveCaloriesBurnedRecord::class.java),
            TOTAL_CALORIES_BURNED to listOf(TotalCaloriesBurnedRecord::class.java),
            HEIGHT to listOf(HeightRecord::class.java),
            BODY_FAT to listOf(BodyFatRecord::class.java),
            OXYGEN_SATURATION to listOf(OxygenSaturationRecord::class.java),
            BODY_TEMPERATURE to listOf(BodyTemperatureRecord::class.java),
            BASAL_BODY_TEMPERATURE to listOf(BasalBodyTemperatureRecord::class.java),
            WHEELCHAIR_PUSHES to listOf(WheelchairPushesRecord::class.java),
            RESTING_HEART_RATE to listOf(RestingHeartRateRecord::class.java),
            RESPIRATORY_RATE to listOf(RespiratoryRateRecord::class.java),
            HYDRATION to listOf(HydrationRecord::class.java),
            FLOORS_CLIMBED to listOf(FloorsClimbedRecord::class.java),
            ELEVATION_GAINED to listOf(ElevationGainedRecord::class.java),
            BONE_MASS to listOf(BoneMassRecord::class.java),
            LEAN_BODY_MASS to listOf(LeanBodyMassRecord::class.java),
            WEIGHT to listOf(WeightRecord::class.java),
            BLOOD_GLUCOSE to listOf(BloodGlucoseRecord::class.java),
            NUTRITION to listOf(NutritionRecord::class.java),
            BLOOD_PRESSURE to listOf(BloodPressureRecord::class.java),
            VO2_MAX to listOf(Vo2MaxRecord::class.java),
            EXERCISE to
                listOf(ExerciseSessionRecord::class.java, CyclingPedalingCadenceRecord::class.java),
            CERVICAL_MUCUS to listOf(CervicalMucusRecord::class.java),
            SEXUAL_ACTIVITY to listOf(SexualActivityRecord::class.java),
            OVULATION_TEST to listOf(OvulationTestRecord::class.java),
            MENSTRUATION to
                listOf(MenstruationFlowRecord::class.java, MenstruationPeriodRecord::class.java),
            SLEEP to listOf(SleepSessionRecord::class.java),
            BODY_WATER_MASS to listOf(BodyWaterMassRecord::class.java),
            INTERMENSTRUAL_BLEEDING to listOf(IntermenstrualBleedingRecord::class.java),
            HEART_RATE_VARIABILITY to listOf(HeartRateVariabilityRmssdRecord::class.java),
        )

    fun getDataTypes(permissionType: HealthPermissionType): List<Class<out Record>> {
        return map[permissionType].orEmpty()
    }

    @VisibleForTesting
    fun getAllDataTypes(): Map<HealthPermissionType, List<Class<out Record>>> {
        return map
    }
}
