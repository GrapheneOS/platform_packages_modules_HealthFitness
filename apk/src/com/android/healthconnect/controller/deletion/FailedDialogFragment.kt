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
package com.android.healthconnect.controller.deletion

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.deletion.DeletionConstants.TRY_AGAIN_EVENT
import com.android.healthconnect.controller.shared.dialog.AlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

/** A deletion {@link DialogFragment} notifying user about a failed deletion. */
@AndroidEntryPoint(DialogFragment::class)
class FailedDialogFragment : Hilt_FailedDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialogBuilder(this)
            .setIcon(R.attr.failureIcon)
            .setTitle(R.string.delete_dialog_failure_title)
            .setMessage(R.string.delete_dialog_failure_message)
            .setPositiveButton(R.string.delete_dialog_failure_try_again_button) { _, _ ->
                setFragmentResult(TRY_AGAIN_EVENT, Bundle())
            }
            .setNegativeButton(R.string.delete_dialog_failure_close_button)
            .create()
    }

    companion object {
        const val TAG = "FailedDialogFragment"
    }
}
