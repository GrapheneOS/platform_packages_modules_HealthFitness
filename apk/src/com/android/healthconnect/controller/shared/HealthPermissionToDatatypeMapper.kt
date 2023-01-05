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
package com.android.healthconnect.controller.shared

import android.healthconnect.datatypes.ActiveCaloriesBurnedRecord
import android.healthconnect.datatypes.BasalMetabolicRateRecord
import android.healthconnect.datatypes.BodyFatRecord
import android.healthconnect.datatypes.DistanceRecord
import android.healthconnect.datatypes.HeartRateRecord
import android.healthconnect.datatypes.HeightRecord
import android.healthconnect.datatypes.OxygenSaturationRecord
import android.healthconnect.datatypes.PowerRecord
import android.healthconnect.datatypes.Record
import android.healthconnect.datatypes.SpeedRecord
import android.healthconnect.datatypes.StepsCadenceRecord
import android.healthconnect.datatypes.StepsRecord
import android.healthconnect.datatypes.TotalCaloriesBurnedRecord
import com.android.healthconnect.controller.permissions.data.HealthPermissionType
import com.android.healthconnect.controller.permissions.data.HealthPermissionType.ACTIVE_CALORIES_BURNED
import com.android.healthconnect.controller.permissions.data.HealthPermissionType.BASAL_METABOLIC_RATE
import com.android.healthconnect.controller.permissions.data.HealthPermissionType.BODY_FAT
import com.android.healthconnect.controller.permissions.data.HealthPermissionType.DISTANCE
import com.android.healthconnect.controller.permissions.data.HealthPermissionType.HEART_RATE
import com.android.healthconnect.controller.permissions.data.HealthPermissionType.POWER
import com.android.healthconnect.controller.permissions.data.HealthPermissionType.SPEED
import com.android.healthconnect.controller.permissions.data.HealthPermissionType.STEPS
import com.android.healthconnect.controller.permissions.data.HealthPermissionType.TOTAL_CALORIES_BURNED
import com.android.healthconnect.controller.permissions.data.HealthPermissionType.HEIGHT
import com.android.healthconnect.controller.permissions.data.HealthPermissionType.OXYGEN_SATURATION
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
        )

    fun getDataTypes(permissionType: HealthPermissionType): List<Class<out Record>> {
        return map[permissionType].orEmpty()
    }

    @VisibleForTesting
    fun getAllDataTypes(): Map<HealthPermissionType, List<Class<out Record>>> {
        return map
    }
}
