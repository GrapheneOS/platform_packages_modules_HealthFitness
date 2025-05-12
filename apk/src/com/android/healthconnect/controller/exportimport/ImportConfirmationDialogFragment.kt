/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.healthconnect.controller.exportimport

import android.app.Activity
import android.content.ContentResolver
import android.content.DialogInterface
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Slog
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.shared.dialog.AlertDialogBuilder
import com.android.healthconnect.controller.utils.logging.ImportConfirmationDialogElement
import dagger.hilt.android.AndroidEntryPoint

private const val SELECTED_URI_KEY = "selectedUri"

/** Fragment to get the user to confirm that they have selected the right import file. */
@AndroidEntryPoint(DialogFragment::class)
class ImportConfirmationDialogFragment : Hilt_ImportConfirmationDialogFragment() {

    companion object {
        const val IMPORT_FILE_URI_KEY = "importFileUri"
        const val TAG = "ImportConfirmationDialogFragment"
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): AlertDialog {
        val importFileUriString = arguments?.getString(IMPORT_FILE_URI_KEY) ?: ""
        val importFileName = getFileName(importFileUriString)
        val importMessage =
            requireContext().getString(R.string.import_confirmation_dialog_text, importFileName)

        return AlertDialogBuilder(
                requireContext(),
                ImportConfirmationDialogElement.IMPORT_CONFIRMATION_CONTAINER,
            )
            .setIcon(R.attr.importIcon)
            .setTitle(R.string.import_confirmation_dialog_title)
            .setMessage(importMessage)
            .setPositiveButton(
                R.string.import_confirmation_dialog_import_button,
                ImportConfirmationDialogElement.IMPORT_CONFIRMATION_DONE_BUTTON,
            ) { _: DialogInterface, _: Int ->
                Slog.i(TAG, "positive button clicked")
                Slog.i(TAG, importFileUriString)
                val returnIntent: Intent = Intent().putExtra(SELECTED_URI_KEY, importFileUriString)
                requireActivity().setResult(Activity.RESULT_OK, returnIntent)
                requireActivity().finish()
            }
            .setNegativeButton(
                R.string.import_confirmation_dialog_cancel_button,
                ImportConfirmationDialogElement.IMPORT_CONFIRMATION_CANCEL_BUTTON,
            ) { _: DialogInterface, _: Int ->
                Slog.i(TAG, "neutral button clicked")
            }
            .create()
    }

    // TODO(b/344888909): move off UI thread to avoid blocking
    private fun getFileName(uriString: String): String {
        val uri: Uri = Uri.parse(uriString)
        var fileName: String? = null
        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
            cursor.use { c: Cursor? ->
                if (c != null) {
                    if (c.moveToFirst()) {
                        fileName = c.getString(c.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                    }
                }
            }
        }
        return fileName ?: uri.lastPathSegment.toString()
    }
}
