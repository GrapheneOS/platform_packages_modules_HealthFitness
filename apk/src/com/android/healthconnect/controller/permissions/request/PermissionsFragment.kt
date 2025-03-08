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
 *
 *
 */

package com.android.healthconnect.controller.permissions.request

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.permissions.data.PermissionState
import com.android.healthconnect.controller.shared.preference.HealthSetupFragment
import com.android.healthconnect.controller.utils.increaseViewTouchTargetSize

/** Base fragment class for permission request screens. */
abstract class PermissionsFragment : HealthSetupFragment() {

    private lateinit var allowButton: Button
    private lateinit var dontAllowButton: Button

    // Places the preference fragment inside the preference container and allows us
    // to have the action buttons in the same fragment
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val rootView = super.onCreateView(inflater, container, savedInstanceState)
        allowButton = getPrimaryButtonOutline()
        dontAllowButton = getSecondaryButton()

        allowButton.text = getString(R.string.request_permissions_allow)
        dontAllowButton.text = getString(R.string.request_permissions_dont_allow)

        val allowParentView = allowButton.parent.parent as View
        increaseViewTouchTargetSize(requireContext(), allowButton, allowParentView)

        val dontAllowParentView = dontAllowButton.parent as View
        increaseViewTouchTargetSize(requireContext(), dontAllowButton, dontAllowParentView)

        return rootView
    }

    fun getPackageNameExtra(): String {
        return requireActivity().intent.getStringExtra(Intent.EXTRA_PACKAGE_NAME).orEmpty()
    }

    fun getAllowButton(): Button = allowButton

    fun getDontAllowButton(): Button = dontAllowButton

    fun handlePermissionResults(results: MutableMap<HealthPermission, PermissionState>) {
        val grants = mutableListOf<Int>()
        val permissionStrings = mutableListOf<String>()

        for ((permission, state) in results) {
            if (state == PermissionState.GRANTED) {
                grants.add(PackageManager.PERMISSION_GRANTED)
            } else {
                grants.add(PackageManager.PERMISSION_DENIED)
            }

            permissionStrings.add(permission.toString())
        }

        val result = Intent()
        result.putExtra(
            PackageManager.EXTRA_REQUEST_PERMISSIONS_NAMES,
            permissionStrings.toTypedArray(),
        )
        result.putExtra(PackageManager.EXTRA_REQUEST_PERMISSIONS_RESULTS, grants.toIntArray())
        requireActivity().setResult(RESULT_OK, result)
        requireActivity().finish()
    }

    private fun getPermissionStrings(): Array<out String> {
        return requireActivity()
            .intent
            .getStringArrayExtra(PackageManager.EXTRA_REQUEST_PERMISSIONS_NAMES)
            .orEmpty()
    }
}
