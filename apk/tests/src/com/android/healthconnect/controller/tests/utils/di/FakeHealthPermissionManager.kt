/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.healthconnect.controller.tests.utils.di

import android.content.pm.PackageManager.FLAG_PERMISSION_USER_FIXED
import android.content.pm.PackageManager.FLAG_PERMISSION_USER_SET
import android.content.pm.PackageManager.PERMISSION_GRANTED
import com.android.healthconnect.controller.permissions.api.HealthPermissionManager
import com.android.healthconnect.controller.tests.utils.NOW
import java.time.Instant

class FakeHealthPermissionManager : HealthPermissionManager {

    private val grantedPermissions = mutableMapOf<String, MutableList<String>>()
    private val healthPermissionFlags = mutableMapOf<String, Map<String, Int>>()

    var revokeHealthPermissionInvocations = 0
    var grantHealthPermissionInvocations = 0

    fun reset() {
        revokeHealthPermissionInvocations = 0
        grantHealthPermissionInvocations = 0
        grantedPermissions.clear()
        healthPermissionFlags.clear()
    }

    fun setHealthPermissionFlags(packageName: String, flags: Map<String, Int>) {
        healthPermissionFlags[packageName] = flags
    }

    fun setGrantedPermissionsForTest(packageName: String, permissions: List<String>) {
        grantedPermissions[packageName] = permissions.toMutableList()
    }

    override fun getGrantedHealthPermissions(packageName: String): List<String> {
        return grantedPermissions.getOrDefault(packageName, emptyList()).toSet().toList()
    }

    override fun getHealthPermissionsFlags(
        packageName: String,
        permissions: List<String>,
    ): Map<String, Int> {
        return healthPermissionFlags.getOrDefault(packageName, mapOf())
    }

    override fun setHealthPermissionsUserFixedFlagValue(
        packageName: String,
        permissions: List<String>,
        value: Boolean,
    ) {
        // do nothing
    }

    override fun grantHealthPermission(packageName: String, permissionName: String) {
        val permissions = grantedPermissions.getOrDefault(packageName, mutableListOf())
        permissions.add(permissionName)
        grantedPermissions[packageName] = permissions

        val flags = getHealthPermissionsFlags(packageName, permissions).toMutableMap()
        flags[permissionName] = PERMISSION_GRANTED
        setHealthPermissionFlags(packageName, flags.toMap())
        grantHealthPermissionInvocations += 1
    }

    override fun grantHealthPermissions(
        packageName: String,
        permissions: List<String>,
    ): List<String> {
        permissions.forEach { grantHealthPermission(packageName, it) }

        return permissions
    }

    override fun revokeHealthPermission(packageName: String, permissionName: String) {
        val permissions = grantedPermissions.getOrDefault(packageName, mutableListOf())
        permissions.remove(permissionName)
        grantedPermissions[packageName] = permissions

        val flags = getHealthPermissionsFlags(packageName, permissions).toMutableMap()
        if (flags.containsKey(permissionName)) {
            val currentFlag = flags[permissionName]
            if (currentFlag == PERMISSION_GRANTED) {
                flags[permissionName] = FLAG_PERMISSION_USER_SET
            } else if (currentFlag == FLAG_PERMISSION_USER_SET) {
                flags[permissionName] = FLAG_PERMISSION_USER_FIXED
            }
        } else {
            flags[permissionName] = FLAG_PERMISSION_USER_SET
        }
        setHealthPermissionFlags(packageName, flags.toMap())
        revokeHealthPermissionInvocations += 1
    }

    override fun revokeHealthPermissions(
        packageName: String,
        permissions: List<String>,
    ): List<String> {
        permissions.forEach { revokeHealthPermission(packageName, it) }

        return permissions
    }

    override fun revokeAllHealthPermissions(packageName: String) {
        grantedPermissions[packageName] = mutableListOf()
    }

    override fun loadStartAccessDate(packageName: String?): Instant? {
        return NOW
    }
}
