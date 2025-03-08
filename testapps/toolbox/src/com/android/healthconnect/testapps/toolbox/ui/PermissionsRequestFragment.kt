/**
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * ```
 *    http://www.apache.org/licenses/LICENSE-2.0
 * ```
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.testapps.toolbox.ui

import android.content.pm.PackageManager
import android.health.connect.HealthConnectManager
import android.health.connect.HealthPermissions.READ_EXERCISE_ROUTES
import android.health.connect.HealthPermissions.READ_HEALTH_DATA_HISTORY
import android.health.connect.HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND
import android.health.connect.HealthPermissions.WRITE_MEDICAL_DATA
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.IdRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.android.healthconnect.testapps.toolbox.R
import kotlin.system.exitProcess

class PermissionsRequestFragment : Fragment(R.layout.fragment_permissions_request) {
    private val readPermissionsPrefix = "android.permission.health.READ_"
    private val writePermissionsPrefix = "android.permission.health.WRITE_"
    private val medicalReadPermissionsPrefix = "android.permission.health.READ_MEDICAL_DATA_"

    private val tag = PermissionsRequestFragment::class.java.simpleName
    private lateinit var mRequestPermissionLauncher: ActivityResultLauncher<Array<String>>
    private val tree: TreeNode by lazy {
        TreeNode(
            checkBoxId = R.id.all_permissions_checkbox,
            buttonId = null,
            counterId = R.id.all_permissions_counter,
            children =
                listOf(
                    TreeNode(
                        checkBoxId = R.id.fitness_permissions_checkbox,
                        buttonId = R.id.fitness_permissions_button,
                        counterId = R.id.fitness_permissions_counter,
                        children =
                            listOf(
                                TreeNode(
                                    checkBoxId = R.id.fitness_read_permissions_checkbox,
                                    buttonId = R.id.fitness_read_permissions_button,
                                    counterId = R.id.fitness_read_permissions_counter,
                                    permissions =
                                        getDeclaredHealthPermissions {
                                            isReadPermission(it) && isFitnessPermission(it)
                                        },
                                ),
                                TreeNode(
                                    checkBoxId = R.id.fitness_write_permissions_checkbox,
                                    buttonId = R.id.fitness_write_permissions_button,
                                    counterId = R.id.fitness_write_permissions_counter,
                                    permissions =
                                        getDeclaredHealthPermissions {
                                            isWritePermission(it) && isFitnessPermission(it)
                                        },
                                ),
                            ),
                    ),
                    TreeNode(
                        checkBoxId = R.id.personal_health_record_permissions_checkbox,
                        buttonId = R.id.personal_health_record_permissions_button,
                        counterId = R.id.personal_health_record_permissions_counter,
                        children =
                            listOf(
                                TreeNode(
                                    checkBoxId =
                                        R.id.personal_health_record_read_permissions_checkbox,
                                    buttonId = R.id.personal_health_record_read_permissions_button,
                                    counterId =
                                        R.id.personal_health_record_read_permissions_counter,
                                    permissions =
                                        getDeclaredHealthPermissions { isMedicalReadPermission(it) },
                                ),
                                TreeNode(
                                    checkBoxId =
                                        R.id.personal_health_record_write_permissions_checkbox,
                                    buttonId = R.id.personal_health_record_write_permissions_button,
                                    counterId =
                                        R.id.personal_health_record_write_permissions_counter,
                                    permissions = listOf(WRITE_MEDICAL_DATA),
                                ),
                            ),
                    ),
                    TreeNode(
                        checkBoxId = R.id.additional_permissions_checkbox,
                        buttonId = R.id.additional_permissions_button,
                        counterId = R.id.additional_permissions_counter,
                        children =
                            listOf(
                                TreeNode(
                                    checkBoxId = R.id.history_read_permission_checkbox,
                                    buttonId = R.id.history_read_permission_button,
                                    counterId = R.id.history_read_permission_counter,
                                    permissions = listOf(READ_HEALTH_DATA_IN_BACKGROUND),
                                ),
                                TreeNode(
                                    checkBoxId = R.id.background_read_permission_checkbox,
                                    buttonId = R.id.background_read_permission_button,
                                    counterId = R.id.background_read_permission_counter,
                                    permissions = listOf(READ_HEALTH_DATA_HISTORY),
                                ),
                            ),
                    ),
                ),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Starting API Level 30 If permission is denied more than once, user doesn't see the dialog
        // asking permissions again unless they grant the permission from settings.
        mRequestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { it ->
                handlePermissionsResult(it)
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_permissions_request, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tree.forEach {
            it.checkBoxId.getView<CheckBox>().setOnCheckedChangeListener(getCheckBoxListener(it))
            it.buttonId?.getView<Button>()?.setOnClickListener { _ ->
                tree.forEach { setCheckBoxChecked(it, false) }
                it.checkBoxId.getView<CheckBox>().isChecked = true
            }
        }
        updateCounters(tree)
        view.requireViewById<Button>(R.id.exit_process_button).setOnClickListener {
            exitProcess(status = 0)
        }
        view.requireViewById<Button>(R.id.revoke_selected_permissions).setOnClickListener {
            val permissionsToRevoke = getCheckedPermissions()
            val permissionsToRevokeSize =
                permissionsToRevoke.size - permissionsToRevoke.count { !isPermissionGranted(it) }
            if (permissionsToRevoke.isNotEmpty())
                requireContext().revokeSelfPermissionsOnKill(permissionsToRevoke)
            Log.i(tag, "Revoking ${permissionsToRevoke.size} permissions")
            val text =
                if (permissionsToRevoke.any { isPermissionGranted(it) }) {
                    "$permissionsToRevokeSize permissions will be revoked on next application kill"
                } else {
                    "Permissions already revoked"
                }
            Toast.makeText(this.requireContext(), text, Toast.LENGTH_LONG).show()
        }

        view.requireViewById<Button>(R.id.request_selected_permissions).setOnClickListener {
            val permissionsToGrant = getCheckedPermissions()
            Log.i(tag, "Requesting ${permissionsToGrant.size} permissions")
            if (permissionsToGrant.all { isPermissionGranted(it) }) {
                Toast.makeText(
                    this.requireContext(),
                    getString(R.string.all_permissions_already_granted_toast),
                    Toast.LENGTH_LONG,
                )
                    .show()
            } else {
                mRequestPermissionLauncher.launch(permissionsToGrant.toTypedArray())
            }
        }
    }

    private fun getCheckedPermissions(): List<String> {
        val checkedPermissions = mutableListOf<String>()
        tree.forEach {
            if (it.checkBoxId.getView<CheckBox>().isChecked) {
                checkedPermissions.addAll(it.permissions)
            }
        }
        return checkedPermissions
    }

    private fun isMedicalPermission(permission: String) =
        isMedicalReadPermission(permission) || permission == WRITE_MEDICAL_DATA

    private fun isAdditionalPermission(permission: String) = "HEALTH" in permission

    private fun isWritePermission(permission: String) =
        permission.startsWith(writePermissionsPrefix)

    private fun isReadPermission(permission: String) = permission.startsWith(readPermissionsPrefix)

    private fun isMedicalReadPermission(permission: String) =
        permission.startsWith(medicalReadPermissionsPrefix)

    private fun isFitnessPermission(permission: String) =
        !(isMedicalPermission(permission) ||
                isAdditionalPermission(permission) ||
                READ_EXERCISE_ROUTES == permission)

    private fun handlePermissionsResult(permissionMap: Map<String, Boolean>) {
        val numberOfPermissionsGranted = permissionMap.values.count { it }
        val numberOfPermissionsDenied = permissionMap.keys.size - numberOfPermissionsGranted
        Toast.makeText(
            this.requireContext(),
            "Granted: $numberOfPermissionsGranted Denied: $numberOfPermissionsDenied",
            Toast.LENGTH_LONG,
        )
            .show()
        updateCounters(tree)
    }

    private fun isPermissionGranted(permission: String) =
        ContextCompat.checkSelfPermission(requireContext(), permission) ==
                PackageManager.PERMISSION_GRANTED

    private class PermissionCounter(val grantedPermissions: Int, val totalPermissions: Int)

    private fun updateCounters(treeNode: TreeNode): PermissionCounter {
        var grantedPermissions = 0
        var totalPermissions = 0
        for (child in treeNode.children) {
            val counter = updateCounters(child)
            grantedPermissions += counter.grantedPermissions
            totalPermissions += counter.totalPermissions
        }
        grantedPermissions += treeNode.permissions.count { isPermissionGranted(it) }
        totalPermissions += treeNode.permissions.size
        treeNode.counterId.getView<TextView>().text =
            String.format("%s/%s", grantedPermissions, totalPermissions)
        return PermissionCounter(grantedPermissions, totalPermissions)
    }

    private class TreeNode(
        @IdRes val checkBoxId: Int,
        @IdRes val buttonId: Int?,
        @IdRes val counterId: Int,
        val permissions: List<String> = listOf<String>(),
        val children: List<TreeNode> = listOf<TreeNode>(),
    ) {
        init {
            children.forEach { it.parent = this }
        }

        var parent: TreeNode? = null

        fun forEach(operation: (TreeNode) -> Unit) {
            operation(this)
            children.forEach { it.forEach(operation) }
        }
    }

    fun <T : View> @receiver:IdRes Int.getView(): T = requireView().requireViewById<T>(this)

    private fun checkBoxPressed(treeNode: TreeNode, checked: Boolean) {
        treeNode.forEach { setCheckBoxChecked(it, checked) }
        var treeNode: TreeNode? = treeNode.parent
        while (treeNode != null) {
            setCheckBoxChecked(
                treeNode,
                treeNode.children.all { it.checkBoxId.getView<CheckBox>().isChecked },
            )
            treeNode = treeNode.parent
        }
        setButtonsEnabled(atLeastOneIsChecked(tree))
    }

    private fun atLeastOneIsChecked(treeNode: TreeNode): Boolean {
        return treeNode.checkBoxId.getView<CheckBox>().isChecked ||
                treeNode.children.any { atLeastOneIsChecked(it) }
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        R.id.revoke_selected_permissions.getView<Button>().isEnabled = enabled
        R.id.request_selected_permissions.getView<Button>().isEnabled = enabled
    }

    private fun setCheckBoxChecked(treeNode: TreeNode, checked: Boolean) {
        val checkBox = treeNode.checkBoxId.getView<CheckBox>()
        checkBox.setOnCheckedChangeListener(null)
        checkBox.isChecked = checked
        checkBox.setOnCheckedChangeListener(getCheckBoxListener(treeNode))
    }

    private fun getCheckBoxListener(treeNode: TreeNode) =
        CompoundButton.OnCheckedChangeListener { _, isChecked ->
            checkBoxPressed(treeNode, isChecked)
        }

    private fun getDeclaredHealthPermissions(
        permissionsFilter: (String) -> Boolean = { true }
    ): List<String> {
        val context = requireContext()

        val packageInfo =
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()),
            )
        val requestedPermissions = packageInfo.requestedPermissions
        return requestedPermissions
            ?.filter { HealthConnectManager.isHealthPermission(context, it) }
            ?.filter(permissionsFilter) ?: listOf()
    }
}
