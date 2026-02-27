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

package com.android.healthconnect.controller.data.api

import com.android.healthconnect.controller.permissions.data.HealthPermissionType
import com.android.healthconnect.controller.shared.HealthDataCategoryInt

/**
 * Represents Health Category group to be shown in health connect screens.
 *
 * @param category Category id
 * @param data [HealthPermissionType]s within the category that have data written by given app.
 */
// TODO (b/487233053) investigate if still needed
data class PermissionTypesPerCategory(
    val category: @HealthDataCategoryInt Int,
    val data: List<HealthPermissionType>,
)
