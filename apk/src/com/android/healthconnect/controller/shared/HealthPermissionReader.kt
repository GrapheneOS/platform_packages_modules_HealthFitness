/**
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * ```
 *      http://www.apache.org/licenses/LICENSE-2.0
 * ```
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.shared

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import com.android.healthconnect.controller.permissions.data.HealthPermission
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Class that read permissions declared by Health Connect clients as a string array in their XML
 * resources. see android.healthconnect.HealthPermissions
 */
class HealthPermissionReader @Inject constructor(@ApplicationContext private val context: Context) {

    suspend fun getAppsWithHealthPermissions(): List<String> {
        return context.packageManager
            .queryIntentActivities(getRationalIntent(), 0 or PackageManager.MATCH_ALL)
            .map { it.activityInfo.packageName }
    }

    suspend fun getDeclaredPermissions(packageName: String): List<HealthPermission> {
        return try {
            val appInfo =
                context.packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            val healthPermissions = getHealthPermissions()
            appInfo.requestedPermissions
                ?.filter { it in healthPermissions }
                ?.mapNotNull { permission -> parsePermission(permission) }
                .orEmpty()
        } catch (e: NameNotFoundException) {
            emptyList()
        }
    }

    private fun parsePermission(permission: String): HealthPermission? {
        return HealthPermission.fromPermissionString(permission)
    }

    private fun getHealthPermissions(): List<String> =
        context.packageManager.queryPermissionsByGroup("android.permission-group.HEALTH", 0).map {
            permissionInfo ->
            permissionInfo.name
        }

    private fun getRationalIntent(packageName: String? = null): Intent {
        val intent = Intent("android.intent.action.VIEW_PERMISSION_USAGE")
        intent.addCategory("android.intent.category.HEALTH_PERMISSIONS")
        if (packageName != null) {
            intent.setPackage(packageName)
        }
        return intent
    }
}
