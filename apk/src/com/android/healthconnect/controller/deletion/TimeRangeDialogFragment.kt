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
package com.android.healthconnect.controller.deletion

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.RadioGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.deletion.DeletionConstants.TIME_RANGE_SELECTION_EVENT
import com.android.healthconnect.controller.shared.dialog.AlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import java.util.Locale

/** A {@link DialogFragment} for choosing the deletion time range. */
@AndroidEntryPoint(DialogFragment::class)
class TimeRangeDialogFragment : Hilt_TimeRangeDialogFragment() {

    private val viewModel: DeletionViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view: View = layoutInflater.inflate(R.layout.dialog_message_time_range_picker, null)
        val radioGroup: RadioGroup = view.findViewById(R.id.radio_group)
        val messageView: TextView = view.findViewById(R.id.time_range_message)
        messageView.text = buildMessage()

        return AlertDialogBuilder(this)
            .setTitle(R.string.time_range_title)
            .setIcon(R.attr.deleteSettingsIcon)
            .setView(view)
            .setNegativeButton(android.R.string.cancel)
            .setPositiveButton(R.string.time_range_next_button) { _, _ ->
                val chosenRange = getChosenRange(radioGroup)
                viewModel.setChosenRange(chosenRange)
                viewModel.setEndTime(Instant.now())
                setFragmentResult(TIME_RANGE_SELECTION_EVENT, Bundle())
            }
            .create()
    }

    private fun buildMessage(): String {
        val deletionParameters = viewModel.deletionParameters.value ?: DeletionParameters()
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
            is DeletionType.DeletionTypeAppData -> {
                val appName = deletionParameters.getAppName()
                resources.getString(R.string.time_range_message_app_data, appName)
            }
            else ->
                throw UnsupportedOperationException(
                    "This Deletion type does not support configurable time range. DataTypeFromApp automatically" +
                        " deletes from all time.")
        }
    }

    private fun getChosenRange(radioGroup: RadioGroup): ChosenRange {
        return when (radioGroup.checkedRadioButtonId) {
            R.id.radio_button_one_day -> ChosenRange.DELETE_RANGE_LAST_24_HOURS
            R.id.radio_button_one_week -> ChosenRange.DELETE_RANGE_LAST_7_DAYS
            R.id.radio_button_one_month -> ChosenRange.DELETE_RANGE_LAST_30_DAYS
            else -> ChosenRange.DELETE_RANGE_ALL_DATA
        }
    }

    companion object {
        const val TAG = "TimeRangeDialogFragment"
    }
}
