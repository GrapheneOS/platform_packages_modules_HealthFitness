/**
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.matchmaking.api

import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.shared.app.AppMetadata

/**
 * Data class to hold matchmaking app data.
 *
 * @param metadata The metadata of the app.
 * @param permissions The list of permissions for the app.
 */
data class MatchmakingAppData(
    val metadata: AppMetadata,
    val permissions: List<HealthPermission.FitnessPermission>,
)
