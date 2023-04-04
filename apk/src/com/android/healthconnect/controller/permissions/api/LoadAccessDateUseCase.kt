/**
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.permissions.api

import android.util.Log
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoadAccessDateUseCase
@Inject
constructor(private val healthPermissionManager: HealthPermissionManager) {

    companion object {
        private const val TAG = "LoadAccessDate"
    }

    operator fun invoke(packageName: String?): Instant? {
        packageName?.let {
            return try {
                healthPermissionManager.loadStartAccessDate(it)
            } catch (ex: Exception) {
                Log.e(TAG, "GetGrantedHealthPermissionsUseCase.invoke", ex)
                null
            }
        }
        return null
    }
}
