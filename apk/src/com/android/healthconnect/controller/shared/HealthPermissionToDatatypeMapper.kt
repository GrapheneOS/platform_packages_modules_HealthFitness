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

import android.health.connect.datatypes.Record
import android.health.connect.internal.datatypes.utils.HealthConnectMappings
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.permissions.data.fromHealthPermissionCategory

object HealthPermissionToDatatypeMapper {
    private val map = createMap()

    fun getDataTypes(permissionType: FitnessPermissionType): List<Class<out Record>> {
        return map[permissionType].orEmpty()
    }

    fun getAllDataTypes(): Map<FitnessPermissionType, List<Class<out Record>>> {
        return map
    }

    fun getPermissionType(dataType: Class<out Record>): FitnessPermissionType? {
        return map.entries.firstOrNull { it.value.contains(dataType) }?.key
    }

    private fun createMap(): Map<FitnessPermissionType, List<Class<out Record>>> {
        val healthConnectMappings = HealthConnectMappings.getInstance()

        return healthConnectMappings.allRecordTypeIdentifiers
            .map { recordTypeId ->
                fromHealthPermissionCategory(
                    healthConnectMappings.getHealthPermissionCategoryForRecordType(recordTypeId)
                ) to healthConnectMappings.recordIdToExternalRecordClassMap[recordTypeId]!!
            }
            .groupBy({ it.first as FitnessPermissionType }, { it.second })
    }
}
