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
package com.android.healthconnect.testapps.toolbox.data

import android.content.Context
import android.content.pm.PackageManager
import android.health.connect.HealthConnectManager
import android.health.connect.HealthPermissions.READ_EXERCISE_ROUTES
import android.health.connect.HealthPermissions.READ_HEALTH_DATA_HISTORY
import android.health.connect.HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND
import android.health.connect.HealthPermissions.WRITE_MEDICAL_DATA
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.state.ToggleableState
import com.android.healthconnect.testapps.toolbox.R

class PermissionsRepository(context: Context) {
    private val readPermissionsPrefix = "android.permission.health.READ_"
    private val writePermissionsPrefix = "android.permission.health.WRITE_"
    private val medicalReadPermissionsPrefix = "android.permission.health.READ_MEDICAL_DATA_"
    val tree: TreeNode =
        TreeNode(
            nameId = R.string.all_permissions,
            children =
                listOf(
                    TreeNode(
                        nameId = R.string.health_permissions,
                        children =
                            listOf(
                                TreeNode(
                                    nameId = R.string.read_permission,
                                    permissions =
                                        getDeclaredHealthPermissions(context) {
                                            isReadPermission(it) && isFitnessPermission(it)
                                        },
                                ),
                                TreeNode(
                                    nameId = R.string.write_permission,
                                    permissions =
                                        getDeclaredHealthPermissions(context) {
                                            isWritePermission(it) && isFitnessPermission(it)
                                        },
                                ),
                            ),
                    ),
                    TreeNode(
                        nameId = R.string.medical,
                        children =
                            listOf(
                                TreeNode(
                                    nameId = R.string.read_permission,
                                    permissions =
                                        getDeclaredHealthPermissions(context) {
                                            isMedicalReadPermission(it)
                                        },
                                ),
                                TreeNode(
                                    nameId = R.string.write_permission,
                                    permissions = listOf(WRITE_MEDICAL_DATA),
                                ),
                            ),
                    ),
                    TreeNode(
                        nameId = R.string.additional_permissions,
                        children =
                            listOf(
                                TreeNode(
                                    nameId = R.string.history_read_permission,
                                    permissions = listOf(READ_HEALTH_DATA_HISTORY),
                                ),
                                TreeNode(
                                    nameId = R.string.background_read_permission,
                                    permissions = listOf(READ_HEALTH_DATA_IN_BACKGROUND),
                                ),
                                TreeNode(
                                    nameId = R.string.write_device_udi_permission,
                                    permissions =
                                        listOf("android.permission.health.WRITE_DEVICE_UDI"),
                                ),
                            ),
                    ),
                ),
        )

    data class TreeNode(
        val nameId: Int,
        val permissions: List<String> = listOf(),
        val children: List<TreeNode> = listOf(),
    ) {
        var counter by mutableStateOf("0/0")
        var toggleableState by mutableStateOf(ToggleableState.On)
        var parent: TreeNode? = null

        init {
            children.forEach { it.parent = this }
        }

        fun forEach(operation: (TreeNode) -> Unit) {
            operation(this)
            children.forEach { it.forEach(operation) }
        }
    }

    private fun isMedicalPermission(permission: String) =
        isMedicalReadPermission(permission) || permission == WRITE_MEDICAL_DATA

    private fun isAdditionalPermission(permission: String) =
        permission in listOf(READ_HEALTH_DATA_HISTORY, READ_HEALTH_DATA_IN_BACKGROUND)

    private fun isWritePermission(permission: String) =
        permission.startsWith(writePermissionsPrefix)

    private fun isReadPermission(permission: String) = permission.startsWith(readPermissionsPrefix)

    private fun isMedicalReadPermission(permission: String) =
        permission.startsWith(medicalReadPermissionsPrefix)

    private fun isFitnessPermission(permission: String) =
        !(isMedicalPermission(permission) ||
            isAdditionalPermission(permission) ||
            READ_EXERCISE_ROUTES == permission)

    private fun getDeclaredHealthPermissions(
        context: Context,
        permissionsFilter: (String) -> Boolean = { true },
    ): List<String> {
        val packageInfo =
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()),
            )

        return packageInfo.requestedPermissions
            ?.filter { HealthConnectManager.isHealthPermission(context, it) }
            ?.filter(permissionsFilter) ?: listOf()
    }
}
