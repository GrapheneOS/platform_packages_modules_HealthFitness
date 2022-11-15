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
package com.android.healthconnect.controller.autodelete

import android.app.Dialog
import android.icu.text.MessageFormat
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.shared.dialog.AlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

/** A {@link DialogFragment} to get confirmation from user to turn auto-delete on. */
@AndroidEntryPoint(DialogFragment::class)
class AutoDeleteConfirmationDialogFragment(private val autoDeleteRange: AutoDeleteRange) :
    Hilt_AutoDeleteConfirmationDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        check(autoDeleteRange != AutoDeleteRange.AUTO_DELETE_RANGE_NEVER) {
            "ConfirmationDialog not supported for AUTO_DELETE_RANGE_NEVER."
        }
        return AlertDialogBuilder(this)
            .setTitle(buildTitle())
            .setIcon(R.attr.deleteSettingsIcon)
            .setMessage(buildMessage())
            // TODO(b/246773887): Set fragment result via negative and positive button to let parent
            // fragment know whether the user accepted the next range.
            .setNegativeButton(android.R.string.cancel)
            .setPositiveButton(R.string.set_auto_delete_button)
            .create()
    }
    companion object {
        const val TAG = "AutoDeleteConfirmationDialogFragment"
    }

    private fun buildTitle(): String {
        val count = numberOfMonths(autoDeleteRange)
        return MessageFormat.format(
            requireContext().getString(R.string.confirming_question_x_months),
            mapOf("count" to count))
    }

    private fun buildMessage(): String {
        val count = numberOfMonths(autoDeleteRange)
        return MessageFormat.format(
            requireContext().getString(R.string.confirming_message_x_months),
            mapOf("count" to count))
    }
}
