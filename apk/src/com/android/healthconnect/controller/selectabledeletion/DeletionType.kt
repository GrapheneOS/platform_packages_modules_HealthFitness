/*
 * Copyright (C) 2023 The Android Open Source Project
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
package com.android.healthconnect.controller.selectabledeletion

import com.android.healthconnect.controller.data.entries.datenavigation.DateNavigationPeriod
import com.android.healthconnect.controller.permissions.data.HealthPermissionType
import com.android.healthconnect.controller.shared.DataType
import java.time.Instant

/** Represents the types of deletion that the user can perform. */
sealed class DeletionType {
    data class DeleteHealthPermissionTypes(
        val healthPermissionTypes: Set<HealthPermissionType>,
        val totalPermissionTypes: Int,
    ) : DeletionType()

    data class DeleteHealthPermissionTypesFromApp(
        val healthPermissionTypes: Set<HealthPermissionType>,
        val totalPermissionTypes: Int,
        val packageName: String,
        val appName: String,
    ) : DeletionType()

    data class DeleteEntries(
        val idsToDataTypes: Map<String, DataType>,
        val totalEntries: Int,
        val period: DateNavigationPeriod,
        val startTime: Instant,
    ) : DeletionType()

    data class DeleteEntriesFromApp(
        val idsToDataTypes: Map<String, DataType>,
        val packageName: String,
        val appName: String,
        val totalEntries: Int,
        val period: DateNavigationPeriod,
        val startTime: Instant,
    ) : DeletionType() {
        fun toDeleteEntries(): DeleteEntries =
            DeleteEntries(idsToDataTypes, totalEntries, period, startTime)
    }

    data class DeleteInactiveAppData(
        val packageName: String,
        val appName: String,
        val healthPermissionType: HealthPermissionType,
    ) : DeletionType()

    data class DeleteAllSymptomsDataFromInactiveApp(val packageName: String, val appName: String) :
        DeletionType()

    data class DeleteAppData(val packageName: String, val appName: String) : DeletionType()
}
