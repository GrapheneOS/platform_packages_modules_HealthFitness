/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.healthconnect.controller.data.shared

import android.health.connect.HealthDataCategory
import android.health.connect.RecordTypeInfoResponse
import android.health.connect.datatypes.Record
import android.health.connect.datatypes.SymptomRecord
import androidx.annotation.VisibleForTesting
import com.android.healthconnect.controller.data.api.PermissionTypesPerCategory
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.permissions.data.HealthPermissionType
import com.android.healthconnect.controller.permissions.data.fromHealthPermissionCategory
import com.android.healthconnect.controller.shared.Constants.DEVICE_DATA_PROVIDER_PACKAGE
import com.android.healthconnect.controller.shared.HealthDataCategoryExtensions.healthPermissionTypes
import com.android.healthconnect.controller.shared.HealthDataCategoryInt
import com.android.healthfitness.flags.Flags.deviceDataProvidersApi

/**
 * Returns those [HealthPermissionType]s that have some data written by the given [packageName] app.
 * If the is no app provided then return all data.
 */
fun getPermissionTypesPerCategory(
    category: @HealthDataCategoryInt Int,
    recordTypeInfoMap: Map<Class<out Record>, RecordTypeInfoResponse>,
    packageName: String?,
    currentDeviceId: String?,
): PermissionTypesPerCategory {
    if (category == HealthDataCategory.SYMPTOMS) {
        return getSymptomPermissionTypes(recordTypeInfoMap, packageName)
    }

    val permissionTypes = category.healthPermissionTypes()
    val filteredPermissions =
        permissionTypes.filter {
            if (packageName == null) {
                hasData(it, recordTypeInfoMap)
            } else if (
                deviceDataProvidersApi() &&
                    currentDeviceId != null &&
                    (packageName == currentDeviceId || packageName == DEVICE_DATA_PROVIDER_PACKAGE)
            ) {
                hasDataByApp(it, recordTypeInfoMap, currentDeviceId) ||
                    hasDataByApp(it, recordTypeInfoMap, DEVICE_DATA_PROVIDER_PACKAGE)
            } else {
                hasDataByApp(it, recordTypeInfoMap, packageName)
            }
        }
    return PermissionTypesPerCategory(category, filteredPermissions)
}

@VisibleForTesting
fun getSymptomPermissionTypes(
    recordTypeInfoMap: Map<Class<out Record>, RecordTypeInfoResponse>,
    packageName: String?,
): PermissionTypesPerCategory {
    val symptomRecordInfo = recordTypeInfoMap[SymptomRecord::class.java]
    val hasAnySymptomData =
        if (packageName == null) {
            symptomRecordInfo?.contributingPackages?.isNotEmpty() == true
        } else {
            symptomRecordInfo?.contributingPackages?.any { it.packageName == packageName } == true
        }

    return if (hasAnySymptomData) {
        PermissionTypesPerCategory(
            HealthDataCategory.SYMPTOMS,
            // Use SYMPTOM_ABDOMINAL_PAIN to represent the Symptoms category.
            listOf(FitnessPermissionType.SYMPTOM_ABDOMINAL_PAIN),
        )
    } else {
        PermissionTypesPerCategory(HealthDataCategory.SYMPTOMS, emptyList())
    }
}

@VisibleForTesting
fun hasData(
    permissionType: HealthPermissionType,
    recordTypeInfoMap: Map<Class<out Record>, RecordTypeInfoResponse>,
): Boolean =
    recordTypeInfoMap.values.any { response ->
        response.permissionCategories.any { fromHealthPermissionCategory(it) == permissionType } &&
            response.contributingPackages.isNotEmpty()
    }

@VisibleForTesting
fun hasDataByApp(
    permissionType: HealthPermissionType,
    recordTypeInfoMap: Map<Class<out Record>, RecordTypeInfoResponse>,
    packageName: String,
): Boolean =
    recordTypeInfoMap.values.any { response ->
        response.permissionCategories.any { fromHealthPermissionCategory(it) == permissionType } &&
            response.contributingPackages.isNotEmpty() &&
            response.contributingPackages.any { it.packageName == packageName }
    }
