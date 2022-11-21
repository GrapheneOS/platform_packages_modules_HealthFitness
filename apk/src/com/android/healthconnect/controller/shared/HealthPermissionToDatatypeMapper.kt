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

import android.healthconnect.datatypes.BasalMetabolicRateRecord
import android.healthconnect.datatypes.HeartRateRecord
import android.healthconnect.datatypes.Record
import android.healthconnect.datatypes.StepsRecord
import com.android.healthconnect.controller.permissions.data.HealthPermissionType

object HealthPermissionToDatatypeMapper {
    private val map =
        mapOf(
            HealthPermissionType.STEPS to listOf(StepsRecord::class.java),
            HealthPermissionType.HEART_RATE to listOf(HeartRateRecord::class.java),
            HealthPermissionType.BASAL_METABOLIC_RATE to
                listOf(BasalMetabolicRateRecord::class.java))

    fun getDataTypes(permissionType: HealthPermissionType): List<Class<out Record>> {
        return map[permissionType].orEmpty()
    }
}
