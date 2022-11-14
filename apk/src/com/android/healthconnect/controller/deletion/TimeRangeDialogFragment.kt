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
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.shared.dialog.AlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.internal.managers.FragmentComponentManager
import java.util.Locale

/** A {@link DialogFragment} for choosing the deletion time range. */
@AndroidEntryPoint(DialogFragment::class)
class TimeRangeDialogFragment : Hilt_TimeRangeDialogFragment() {

    private lateinit var deletionParameters: Deletion
    var onNextClickListener: DialogInterface.OnClickListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val deletionBundle = requireArguments().getParcelable("DELETION_PARAMETERS") as Deletion?
        deletionParameters = deletionBundle ?: Deletion()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view: View = layoutInflater.inflate(R.layout.time_range_picker, null)
        val messageView: TextView = view.findViewById(R.id.time_range_message)
        messageView.text = buildMessage()

        return AlertDialogBuilder(this)
            .setTitle(R.string.time_range_title)
            .setIcon(R.attr.deleteSettingsIcon)
            .setView(view)
            .setNegativeButton(android.R.string.cancel)
            .setPositiveButton(R.string.time_range_next_button, onNextClickListener)
            .create()
    }

    private fun buildMessage(): String {
        val deletionType: DeletionType = deletionParameters.deletionType

        return when (deletionType) {
            is DeletionType.DeletionTypeAllData ->
                resources.getString(R.string.time_range_message_all)
            is DeletionType.DeletionTypeHealthPermissionTypeData,
            is DeletionType.DeletionTypeHealthPermissionTypeFromApp -> {
                val permissionTypeLabel =
                    getString(deletionParameters.getPermissionTypeLabel())
                        .lowercase(Locale.getDefault())
                resources.getString(R.string.time_range_message_data_type, permissionTypeLabel)
            }
            is DeletionType.DeletionTypeCategoryData -> {
                val categoryLabel =
                    getString(deletionParameters.getCategoryLabel()).lowercase(Locale.getDefault())
                resources.getString(R.string.time_range_message_category, categoryLabel)
            }
            is DeletionType.DeletionTypeAppData ->
                resources.getString(R.string.time_range_message_app_data)
            else ->
                throw UnsupportedOperationException(
                    "This Deletion type does not support configurable time range. DataTypeFromApp automatically" +
                        " deletes from all time.")
        }
    }

    fun setClickListener(mOnNextClickListener: DialogInterface.OnClickListener) {
        this.onNextClickListener = mOnNextClickListener
    }

    companion object {
        const val TAG = "TimeRangeDialogFragment"
        @JvmStatic
        fun create(deletionParameters: Deletion): TimeRangeDialogFragment {
            val fragment = TimeRangeDialogFragment()
            FragmentComponentManager.initializeArguments(fragment)
            val bundle = Bundle()
            bundle.putParcelable("DELETION_PARAMETERS", deletionParameters)
            fragment.arguments = bundle
            return fragment
        }
    }
}
