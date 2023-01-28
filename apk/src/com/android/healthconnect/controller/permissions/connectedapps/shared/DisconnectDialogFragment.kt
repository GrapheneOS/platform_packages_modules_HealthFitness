/**
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * ```
 *      http://www.apache.org/licenses/LICENSE-2.0
 * ```
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.permissions.connectedapps.shared

import android.app.Dialog
import android.os.Bundle
import android.widget.CheckBox
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.shared.dialog.AlertDialogBuilder

/** A Dialog Fragment to get confirmation from user for disconnecting from Health Connect. */
class DisconnectDialogFragment(
    private val appName: String,
    private val enableDeleteData: Boolean = true
) : DialogFragment() {

    companion object {
        const val TAG = "DisconnectDialogFragment"
        const val DISCONNECT_CANCELED_EVENT = "DISCONNECT_CANCELED_EVENT"
        const val DISCONNECT_ALL_EVENT = "DISCONNECT_ALL_EVENT"
        const val KEY_DELETE_DATA = "KEY_DELETE_DATA"
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val body = layoutInflater.inflate(R.layout.dialog_message_with_checkbox, null)
        body.findViewById<TextView>(R.id.dialog_message).apply {
            text = getString(R.string.permissions_disconnect_dialog_message, appName)
        }
        val checkBox =
            body.findViewById<CheckBox>(R.id.dialog_checkbox).apply {
                text = getString(R.string.permissions_disconnect_dialog_checkbox, appName)
                isVisible = enableDeleteData
            }

        val dialog =
            AlertDialogBuilder(this)
                .setIcon(R.attr.disconnectIcon)
                .setTitle(R.string.permissions_disconnect_dialog_title)
                .setView(body)
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                    setFragmentResult(DISCONNECT_CANCELED_EVENT, bundleOf())
                }
                .setPositiveButton(R.string.permissions_disconnect_dialog_disconnect) { _, _ ->
                    setFragmentResult(
                        DISCONNECT_ALL_EVENT, bundleOf(KEY_DELETE_DATA to checkBox.isChecked))
                }
                .create()
        dialog.setCanceledOnTouchOutside(false)
        return dialog
    }
}
