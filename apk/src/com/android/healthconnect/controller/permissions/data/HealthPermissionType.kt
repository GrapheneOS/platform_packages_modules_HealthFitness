/**
 * Copyright (C) 2024 The Android Open Source Project
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
package com.android.healthconnect.controller.permissions.data

import android.content.Context
import android.graphics.drawable.Drawable

/** Umbrella interface for permission types that are displayed in data browse screens. */
interface HealthPermissionType {
    fun lowerCaseLabel(): Int

    fun upperCaseLabel(): Int

    fun icon(context: Context): Drawable?

    val name: String
}

fun fromPermissionTypeName(name: String): HealthPermissionType {
    return if (isValidFitnessPermissionType(name)) {
        FitnessPermissionType.valueOf(name)
    } else if (isValidMedicalPermissionType(name)) {
        MedicalPermissionType.valueOf(name)
    } else {
        throw IllegalArgumentException(
            "PERMISSION_TYPE_KEY is not a valid HealthPermissionType name: $name"
        )
    }
}
