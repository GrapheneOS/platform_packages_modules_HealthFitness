/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */

package com.android.healthconnect.controller.permissions.additionalaccess

import android.content.Intent.EXTRA_PACKAGE_NAME
import android.health.connect.HealthPermissions.READ_EXERCISE
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.permissions.app.AppPermissionViewModel
import com.android.healthconnect.controller.permissions.data.HealthPermission.FitnessPermission.Companion.fromPermissionString
import com.android.healthconnect.controller.shared.Constants.EXTRA_APP_NAME
import com.android.healthconnect.controller.shared.dialog.AlertDialogBuilder
import com.android.healthconnect.controller.utils.logging.AdditionalAccessElement.DISABLE_EXERCISE_PERMISSION_DIALOG_CONTAINER
import com.android.healthconnect.controller.utils.logging.AdditionalAccessElement.DISABLE_EXERCISE_PERMISSION_DIALOG_NEGATIVE_BUTTON
import com.android.healthconnect.controller.utils.logging.AdditionalAccessElement.DISABLE_EXERCISE_PERMISSION_DIALOG_POSITIVE_BUTTON
import com.android.healthconnect.controller.utils.logging.ErrorPageElement.UNKNOWN_ELEMENT
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Dialog to ask user to disable [READ_EXERCISE_ROUTES] permission when disabling [READ_EXERCISE]
 */
@AndroidEntryPoint(DialogFragment::class)
class DisableExerciseRoutePermissionDialog : Hilt_DisableExerciseRoutePermissionDialog() {

    @Inject lateinit var logger: HealthConnectLogger

    private lateinit var packageName: String
    private lateinit var appName: String

    private val viewModel: AppPermissionViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): AlertDialog {

        val packageNameExtra = requireArguments().getString(EXTRA_PACKAGE_NAME)
        if (packageNameExtra.isNullOrEmpty()) {
            Log.e(TAG, "Invalid Intent Extras, finishing.")
            return AlertDialogBuilder(this, UNKNOWN_ELEMENT).create()
        }
        packageName = packageNameExtra

        val appNameExtra = requireArguments().getString(EXTRA_APP_NAME)
        if (appNameExtra == null) {
            Log.e(TAG, "Invalid Intent Extras, finishing.")
            return AlertDialogBuilder(this, UNKNOWN_ELEMENT).create()
        }
        appName = appNameExtra

        val dialog =
            AlertDialogBuilder(this, DISABLE_EXERCISE_PERMISSION_DIALOG_CONTAINER)
                .setTitle(R.string.exercise_permission_dialog_disable_title)
                .setMessage(getString(R.string.exercise_permission_dialog_disable_summary, appName))
                .setPositiveButton(
                    R.string.exercise_permission_dialog_positive_button,
                    DISABLE_EXERCISE_PERMISSION_DIALOG_POSITIVE_BUTTON,
                ) { _, _ ->
                    viewModel.disableExerciseRoutePermission(packageName)
                    dismiss()
                }
                .setNegativeButton(
                    R.string.exercise_permission_dialog_negative_button,
                    DISABLE_EXERCISE_PERMISSION_DIALOG_NEGATIVE_BUTTON,
                ) { _, _ ->
                    viewModel.updatePermission(
                        packageName,
                        fromPermissionString(READ_EXERCISE),
                        grant = true,
                    )
                    dismiss()
                }
                .create()
        dialog.setCanceledOnTouchOutside(false)
        setCancelable(false)
        return dialog
    }

    override fun dismiss() {
        viewModel.hideExerciseRoutePermissionDialog()
        super.dismiss()
    }

    companion object {
        private const val TAG = "DisableExercisePermDialog"

        fun createDialog(
            packageName: String,
            appName: String,
        ): DisableExerciseRoutePermissionDialog {
            return DisableExerciseRoutePermissionDialog().apply {
                arguments =
                    Bundle().apply {
                        putString(EXTRA_PACKAGE_NAME, packageName)
                        putString(EXTRA_APP_NAME, appName)
                    }
            }
        }
    }
}
