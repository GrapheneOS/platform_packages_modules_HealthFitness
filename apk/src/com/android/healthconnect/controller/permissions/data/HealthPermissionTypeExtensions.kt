/*
 * Copyright (C) 2025 The Android Open Source Project
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
package com.android.healthconnect.controller.permissions.data

import android.health.connect.internal.datatypes.utils.SymptomTypePermissionMapper

/** Returns true if this [HealthPermissionType] belongs to the symptom category. */
fun HealthPermissionType.isSymptom(): Boolean {
    return this is FitnessPermissionType &&
        SymptomTypePermissionMapper.isSymptomCategory(this.category)
}

/** Returns a set of all [FitnessPermissionType]s that are symptoms. */
fun getAllSymptomPermissionTypes(): Set<FitnessPermissionType> {
    return FitnessPermissionType.entries.filter { it.isSymptom() }.toSet()
}
