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
package com.android.healthconnect.controller.selectabledeletion

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.shared.dialog.AlertDialogBuilder
import com.android.healthconnect.controller.utils.logging.SuccessDialogElement
import dagger.hilt.android.AndroidEntryPoint


/**
 * A deletion {@link DialogFragment} notifying user about a successful deletion.
 *
 * <p> Does not show the See connected apps negative button if the Connected Apps fragment is
 * already in the stack.
 */
@AndroidEntryPoint(DialogFragment::class)
class SuccessDialogFragment : Hilt_SuccessDialogFragment() {

    private val viewModel: DeletionViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Get the navigation action depending on the deletion type
        val deletionType = viewModel.getDeletionType()
        val navAction =
            when (deletionType) {
                is DeletionType.DeleteHealthPermissionTypes -> {
                    R.id.action_allDataFragment_to_connectedApps
                }
                is DeletionType.DeleteEntries,
                is DeletionType.DeleteInactiveAppData -> {
                    R.id.action_entriesAndAccess_to_connectedApps
                }
                // Connected Apps fragment is already in the stack, no need to navigate.
                is DeletionType.DeleteEntriesFromApp,
                is DeletionType.DeleteHealthPermissionTypesFromApp,
                is DeletionType.DeleteAppData -> {
                    null
                }
            }

        val dialogBuilder =
            AlertDialogBuilder(this, SuccessDialogElement.DELETION_DIALOG_SUCCESS_CONTAINER)
                .setIcon(R.attr.successIcon)
                .setTitle(R.string.delete_dialog_success_title)
                .setMessage(R.string.delete_dialog_success_message)
                .setPositiveButton(
                    R.string.delete_dialog_success_got_it_button,
                    SuccessDialogElement.DELETION_DIALOG_SUCCESS_DONE_BUTTON,
                )

        navAction?.let {
            dialogBuilder.setNegativeButton(
                R.string.delete_dialog_see_connected_apps_button,
                SuccessDialogElement.DELETION_DIALOG_SUCCESS_SEE_CONNECTED_APPS_BUTTON,
                onClickListener = { _, _ ->
                    this.dismiss()
                    val navController = findNavController()
                    if (navController.currentDestination?.id == R.id.entriesAndAccessFragment) {
                        findNavController().navigate(it)
                    }
                },
            )
        }
        return dialogBuilder.create()
    }

    companion object {
        const val TAG = "SuccessDialogFragment"
    }
}
