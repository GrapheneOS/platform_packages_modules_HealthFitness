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
package com.android.healthconnect.testapps.toolbox.ui

import android.app.Activity.RESULT_OK
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.fragment.app.Fragment
import com.android.healthconnect.testapps.toolbox.Constants.FITNESS_PERMISSIONS
import com.android.healthconnect.testapps.toolbox.R

class OnboardingFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_onboarding, container, false)
    }

    private lateinit var mRequestPermissionLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Starting API Level 30 If permission is denied more than once, user doesn't see the dialog
        // asking permissions again unless they grant the permission from settings.
        mRequestPermissionLauncher =
            registerForActivityResult(RequestMultiplePermissions()) { handlePermissionsResult(it) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<Button>(R.id.continue_button).setOnClickListener {
            mRequestPermissionLauncher.launch(FITNESS_PERMISSIONS)
        }
    }

    private fun handlePermissionsResult(permissionMap: Map<String, Boolean>) {

        val numberOfPermissionsGranted = permissionMap.values.count { it }
        val numberOfPermissionsDenied = permissionMap.keys.size - numberOfPermissionsGranted
        Toast.makeText(
                requireContext(),
                "Granted: $numberOfPermissionsGranted Denied: $numberOfPermissionsDenied",
                Toast.LENGTH_LONG,
            )
            .show()
        activity?.setResult(RESULT_OK)
        activity?.finish()
    }
}
