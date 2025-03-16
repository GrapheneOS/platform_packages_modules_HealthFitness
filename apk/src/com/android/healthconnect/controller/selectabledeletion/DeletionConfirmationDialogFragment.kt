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
package com.android.healthconnect.controller.selectabledeletion

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.data.entries.datenavigation.DateNavigationPeriod
import com.android.healthconnect.controller.selectabledeletion.DeletionConstants.CONFIRMATION_KEY
import com.android.healthconnect.controller.shared.dialog.AlertDialogBuilder
import com.android.healthconnect.controller.utils.AttributeResolver
import com.android.healthconnect.controller.utils.LocalDateTimeFormatter
import com.android.healthconnect.controller.utils.TimeSource
import com.android.healthconnect.controller.utils.formatDateTimeForTimePeriod
import com.android.healthconnect.controller.utils.logging.DeletionDialogConfirmationElement
import com.android.settingslib.widget.SettingsThemeHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint(DialogFragment::class)
class DeletionConfirmationDialogFragment : Hilt_DeletionConfirmationDialogFragment() {
    @Inject lateinit var timeSource: TimeSource
    private val viewModel: DeletionViewModel by activityViewModels()
    // TODO (b/384028690) replace after pagination implementation
    private val PAGE_SIZE = 1000

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view: View =
            layoutInflater.inflate(
                if (SettingsThemeHelper.isExpressiveTheme(requireContext()))
                    R.layout.dialog_expressive_layout
                else R.layout.dialog_legacy_layout,
                null,
            )
        val title: TextView = view.findViewById(R.id.dialog_title)
        val message: TextView = view.findViewById(R.id.dialog_custom_message)
        val icon: ImageView = view.findViewById(R.id.dialog_icon)
        val checkbox: CheckBox = view.findViewById(R.id.dialog_checkbox)
        val iconDrawable = AttributeResolver.getNullableDrawable(view.context, R.attr.deleteIcon)

        title.text = buildTitle()
        message.setText(R.string.deletion_confirmation_dialog_body)
        iconDrawable?.let {
            icon.setImageDrawable(it)
            icon.visibility = View.VISIBLE
        }

        setupCheckbox(checkbox)

        val alertDialogBuilder =
            AlertDialogBuilder(
                    this,
                    DeletionDialogConfirmationElement.DELETION_DIALOG_CONFIRMATION_CONTAINER,
                )
                .setView(view)
                .setPositiveButton(
                    R.string.confirming_question_delete_button,
                    DeletionDialogConfirmationElement.DELETION_DIALOG_CONFIRMATION_DELETE_BUTTON,
                ) { _, _ ->
                    viewModel.removePermissions = checkbox.isChecked
                    setFragmentResult(CONFIRMATION_KEY, Bundle())
                }
                .setNeutralButton(
                    android.R.string.cancel,
                    DeletionDialogConfirmationElement.DELETION_DIALOG_CONFIRMATION_CANCEL_BUTTON,
                )

        return alertDialogBuilder.create()
    }

    private fun setupCheckbox(checkBox: CheckBox) {
        val deletionType = viewModel.getDeletionType()
        if (deletionType is DeletionType.DeleteHealthPermissionTypesFromApp) {
            if (deletionType.healthPermissionTypes.size == deletionType.totalPermissionTypes) {
                checkBox.visibility = View.VISIBLE
            } else {
                checkBox.visibility = View.GONE
            }
            val appName = deletionType.appName
            checkBox.text =
                getString(R.string.confirming_question_app_remove_all_permissions, appName)
        } else {
            checkBox.visibility = View.GONE
        }
    }

    private fun buildTitle(): String {
        return when (val deletionType = viewModel.getDeletionType()) {
            is DeletionType.DeleteHealthPermissionTypes ->
                if (deletionType.healthPermissionTypes.size < deletionType.totalPermissionTypes) {
                    getString(R.string.some_data_selected_deletion_confirmation_dialog)
                } else {
                    getString(R.string.all_data_selected_deletion_confirmation_dialog)
                }
            is DeletionType.DeleteHealthPermissionTypesFromApp -> {
                val appName = deletionType.appName
                if (deletionType.healthPermissionTypes.size < deletionType.totalPermissionTypes) {
                    getString(R.string.some_app_data_selected_deletion_confirmation_dialog, appName)
                } else {
                    getString(R.string.all_app_data_selected_deletion_confirmation_dialog, appName)
                }
            }
            is DeletionType.DeleteEntries -> {
                val deletionMapSize = deletionType.idsToDataTypes.size
                if (deletionMapSize == 1) {
                    return getString(R.string.one_entry_selected_deletion_confirmation_dialog)
                }
                val selectedPeriod = deletionType.period
                val startTime = deletionType.startTime
                val displayString =
                    formatDateTimeForTimePeriod(
                        startTime,
                        selectedPeriod,
                        LocalDateTimeFormatter(requireContext()),
                        timeSource,
                        false,
                    )
                if (selectedPeriod == DateNavigationPeriod.PERIOD_DAY) {
                    if (
                        deletionMapSize < deletionType.totalEntries || deletionMapSize == PAGE_SIZE
                    ) {
                        getString(
                            R.string.some_entries_selected_day_deletion_confirmation_dialog,
                            displayString,
                        )
                    } else {
                        getString(
                            R.string.all_entries_selected_day_deletion_confirmation_dialog,
                            displayString,
                        )
                    }
                } else if (selectedPeriod == DateNavigationPeriod.PERIOD_WEEK) {
                    if (
                        deletionMapSize < deletionType.totalEntries || deletionMapSize == PAGE_SIZE
                    ) {
                        getString(
                            R.string.some_entries_selected_week_deletion_confirmation_dialog,
                            displayString,
                        )
                    } else {
                        getString(
                            R.string.all_entries_selected_week_deletion_confirmation_dialog,
                            displayString,
                        )
                    }
                } else {
                    if (
                        deletionMapSize < deletionType.totalEntries || deletionMapSize == PAGE_SIZE
                    ) {
                        getString(
                            R.string.some_entries_selected_month_deletion_confirmation_dialog,
                            displayString,
                        )
                    } else {
                        getString(
                            R.string.all_entries_selected_month_deletion_confirmation_dialog,
                            displayString,
                        )
                    }
                }
            }
            is DeletionType.DeleteEntriesFromApp -> {
                val deletionMapSize = deletionType.idsToDataTypes.size
                val appName = deletionType.appName
                if (deletionMapSize == 1) {
                    return getString(
                        R.string.one_app_entry_selected_deletion_confirmation_dialog,
                        appName,
                    )
                }
                val selectedPeriod = deletionType.period
                val startTime = deletionType.startTime
                val displayString =
                    formatDateTimeForTimePeriod(
                        startTime,
                        selectedPeriod,
                        LocalDateTimeFormatter(requireContext()),
                        timeSource,
                        false,
                    )

                if (selectedPeriod == DateNavigationPeriod.PERIOD_DAY) {
                    if (
                        deletionMapSize < deletionType.totalEntries || deletionMapSize == PAGE_SIZE
                    ) {
                        getString(
                            R.string.some_app_entries_selected_day_deletion_confirmation_dialog,
                            appName,
                            displayString,
                        )
                    } else {
                        getString(
                            R.string.all_app_entries_selected_day_deletion_confirmation_dialog,
                            appName,
                            displayString,
                        )
                    }
                } else if (selectedPeriod == DateNavigationPeriod.PERIOD_WEEK) {
                    if (
                        deletionMapSize < deletionType.totalEntries || deletionMapSize == PAGE_SIZE
                    ) {
                        getString(
                            R.string.some_app_entries_selected_week_deletion_confirmation_dialog,
                            appName,
                            displayString,
                        )
                    } else {
                        getString(
                            R.string.all_app_entries_selected_week_deletion_confirmation_dialog,
                            appName,
                            displayString,
                        )
                    }
                } else {
                    if (
                        deletionMapSize < deletionType.totalEntries || deletionMapSize == PAGE_SIZE
                    ) {
                        getString(
                            R.string.some_app_entries_selected_month_deletion_confirmation_dialog,
                            appName,
                            displayString,
                        )
                    } else {
                        getString(
                            R.string.all_app_entries_selected_month_deletion_confirmation_dialog,
                            appName,
                            displayString,
                        )
                    }
                }
            }
            is DeletionType.DeleteAppData -> {
                val appName = deletionType.appName
                getString(R.string.all_app_data_selected_deletion_confirmation_dialog, appName)
            }
            is DeletionType.DeleteInactiveAppData -> {
                val appName = deletionType.appName
                val healthPermissionType =
                    getString(deletionType.healthPermissionType.lowerCaseLabel())
                getString(
                    R.string.inactive_app_data_selected_deletion_confirmation_dialog,
                    healthPermissionType,
                    appName,
                )
            }
        }
    }

    companion object {
        const val TAG = "NewDeletionConfirmationDialog"
    }
}
