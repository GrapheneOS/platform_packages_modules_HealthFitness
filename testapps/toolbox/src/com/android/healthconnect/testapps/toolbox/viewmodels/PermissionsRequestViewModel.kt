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
package com.android.healthconnect.testapps.toolbox.viewmodels

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.ui.state.ToggleableState
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.android.healthconnect.testapps.toolbox.data.PermissionsRepository
import com.android.healthconnect.testapps.toolbox.data.PermissionsRepository.TreeNode
import com.android.healthconnect.testapps.toolbox.ui.PermissionsRequestFragment

class PermissionsRequestViewModel(private val context: Context, val tree: TreeNode) : ViewModel() {
    private val tag = PermissionsRequestFragment::class.java.simpleName

    init {
        updateCounters(tree)
    }

    fun onCheck(treeNode: TreeNode) {
        val newState =
            if (treeNode.toggleableState != ToggleableState.On) ToggleableState.On
            else ToggleableState.Off
        treeNode.toggleableState = newState
        treeNode.forEach { it.toggleableState = newState }
        var parentNode = treeNode.parent
        while (parentNode != null) {
            parentNode.toggleableState =
                if (parentNode.children.all { it.toggleableState == ToggleableState.On })
                    ToggleableState.On
                else if (parentNode.children.all { it.toggleableState == ToggleableState.Off })
                    ToggleableState.Off
                else {
                    ToggleableState.Indeterminate
                }
            parentNode = parentNode.parent
        }
    }

    private fun getCheckedPermissions(): List<String> {
        val checkedPermissions = mutableListOf<String>()
        tree.forEach {
            if (it.toggleableState == ToggleableState.On) {
                checkedPermissions.addAll(it.permissions)
            }
        }
        return checkedPermissions
    }

    fun getRequestedPermissions(): List<String> {
        val permissionsToGrant = getCheckedPermissions().filter { !isPermissionGranted(it) }
        Log.i(tag, "Requesting ${permissionsToGrant.size} permissions")
        return permissionsToGrant
    }

    fun revokePermissions(): String {
        val permissionsToRevoke = getCheckedPermissions().filter { isPermissionGranted(it) }
        val permissionsToRevokeSize = permissionsToRevoke.size
        if (permissionsToRevoke.isNotEmpty())
            context.revokeSelfPermissionsOnKill(permissionsToRevoke)
        Log.i(tag, "Revoking $permissionsToRevokeSize permissions")
        val text =
            if (permissionsToRevoke.any { isPermissionGranted(it) }) {
                "$permissionsToRevokeSize permissions will be revoked on next application kill"
            } else {
                "Permissions already revoked"
            }
        return text
    }

    fun isPermissionGranted(permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    data class PermissionCounter(val grantedPermissions: Int, val totalPermissions: Int)

    fun updateCounters(treeNode: TreeNode): PermissionCounter {
        var grantedPermissions = 0
        var totalPermissions = 0
        for (child in treeNode.children) {
            val counter = updateCounters(child)
            grantedPermissions += counter.grantedPermissions
            totalPermissions += counter.totalPermissions
        }
        grantedPermissions += treeNode.permissions.count { isPermissionGranted(it) }
        totalPermissions += treeNode.permissions.size
        treeNode.counter = "$grantedPermissions/$totalPermissions"
        return PermissionCounter(grantedPermissions, totalPermissions)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as Application
                val context = application.applicationContext
                val tree: TreeNode = PermissionsRepository(context).tree
                PermissionsRequestViewModel(tree = tree, context = context)
            }
        }
    }
}
