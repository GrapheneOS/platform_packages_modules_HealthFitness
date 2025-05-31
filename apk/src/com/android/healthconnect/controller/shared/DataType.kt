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
import kotlin.reflect.KClass

typealias DataType = KClass<out Record>

fun getDataTypeForClassName(classSimpleName: String): DataType {
    return SUPPORTED_DATA_TYPES.first { it.java.simpleName == classSimpleName }
}

private val SUPPORTED_DATA_TYPES = getSupportedDataTypes()

private fun getSupportedDataTypes(): List<DataType> {
    return HealthConnectMappings.getInstance()
        .recordIdToExternalRecordClassMap
        .values
        .map { it.kotlin }
        .toList()
}
