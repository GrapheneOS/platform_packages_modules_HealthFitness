/**
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * ```
 *    http://www.apache.org/licenses/LICENSE-2.0
 * ```
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.testapps.toolbox.ui

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.android.healthconnect.testapps.toolbox.Constants.ALL_PERMISSIONS
import com.android.healthconnect.testapps.toolbox.R

/** Home fragment for Health Connect Toolbox. */
class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    private lateinit var mRequestPermissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var mNavigationController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Starting API Level 30 If permission is denied more than once, user doesn't see the dialog
        // asking permissions again unless they grant the permission from settings.
        mRequestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                permissionMap: Map<String, Boolean> ->
                requestPermissionResultHandler(permissionMap)
            }
    }

    private fun requestPermissionResultHandler(permissionMap: Map<String, Boolean>) {
        var numberOfPermissionsMissing = ALL_PERMISSIONS.size
        for (value in permissionMap.values) {
            if (value) {
                numberOfPermissionsMissing--
            }
        }

        if (numberOfPermissionsMissing == 0) {
            Toast.makeText(
                    this.requireContext(), R.string.all_permissions_success, Toast.LENGTH_SHORT)
                .show()
        } else {
            Toast.makeText(
                    this.requireContext(),
                    getString(
                        R.string.number_of_permissions_not_granted, numberOfPermissionsMissing),
                    Toast.LENGTH_SHORT)
                .show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<Button>(R.id.request_permissions_button).setOnClickListener {
            requestPermissions()
        }
        view.findViewById<Button>(R.id.insert_update_data_button).setOnClickListener {
            goToCategoryListPage()
        }
        mNavigationController = findNavController()
    }

    private fun isPermissionMissing(): Boolean {
        for (permission in ALL_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this.requireContext(), permission) !=
                PackageManager.PERMISSION_GRANTED) {
                return true
            }
        }
        return false
    }

    private fun requestPermissions() {
        if (isPermissionMissing()) {
            mRequestPermissionLauncher.launch(ALL_PERMISSIONS)
            return
        }
        Toast.makeText(
                this.requireContext(),
                R.string.all_permissions_already_granted_toast,
                Toast.LENGTH_LONG)
            .show()
    }

    private fun goToCategoryListPage() {
        mNavigationController.navigate(R.id.action_homeFragment_to_categoryList)
    }
}
