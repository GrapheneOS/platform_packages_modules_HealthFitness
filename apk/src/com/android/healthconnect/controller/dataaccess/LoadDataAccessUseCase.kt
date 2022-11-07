/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.healthconnect.controller.dataaccess

import com.android.healthconnect.controller.permissions.data.PermissionsAccessType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoadDataAccessUseCase @Inject constructor() {
    /** Temporary mock data, returns a map of apps with read and write permissions. */
    suspend operator fun invoke(): Map<PermissionsAccessType, List<AppInfo>> {
        val dataAccessMap: MutableMap<PermissionsAccessType, List<AppInfo>> = hashMapOf()
        dataAccessMap[PermissionsAccessType.READ] = EXAMPLE_APPS
        dataAccessMap[PermissionsAccessType.WRITE] = EXAMPLE_APPS
        return dataAccessMap
    }
}
