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
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.deletion.DeletionConstants.CONFIRMATION_EVENT
import com.android.healthconnect.controller.deletion.DeletionConstants.GO_BACK_EVENT
import com.android.healthconnect.controller.shared.dialog.AlertDialogBuilder
import com.android.healthconnect.controller.utils.logging.DeletionDialogConfirmationElement
import dagger.hilt.android.AndroidEntryPoint

/**
 * A deletion {@link DialogFragment} asking confirmation from user for deleting data from from the
 * time range chosen on the previous dialog.
 */
@AndroidEntryPoint(DialogFragment::class)
class DeletionConfirmationDialogFragment : Hilt_DeletionConfirmationDialogFragment() {

    private val viewModel: DeletionViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val message = getString(R.string.confirming_question_message)

        val alertDialogBuilder =
            AlertDialogBuilder(this)
                .setLogName(
                    DeletionDialogConfirmationElement.DELETION_DIALOG_CONFIRMATION_CONTAINER)
                .setTitle(buildTitle())
                .setIcon(R.attr.deleteIcon)
                .setMessage(message)
                .setPositiveButton(
                    R.string.confirming_question_delete_button,
                    DeletionDialogConfirmationElement.DELETION_DIALOG_CONFIRMATION_DELETE_BUTTON) {
                        _,
                        _ ->
                        setFragmentResult(CONFIRMATION_EVENT, Bundle())
                    }

        if (viewModel.showTimeRangeDialogFragment) {
            alertDialogBuilder.setNegativeButton(
                R.string.confirming_question_go_back_button,
                DeletionDialogConfirmationElement.DELETION_DIALOG_CONFIRMATION_GO_BACK_BUTTON) {
                    _,
                    _ ->
                    setFragmentResult(GO_BACK_EVENT, Bundle())
                }
        } else {
            alertDialogBuilder.setNegativeButton(
                android.R.string.cancel,
                DeletionDialogConfirmationElement.DELETION_DIALOG_CONFIRMATION_CANCEL_BUTTON) { _, _
                    ->
                }
        }

        return alertDialogBuilder.create()
    }

    private fun buildTitle(): String {
        val deletionParameters = viewModel.deletionParameters.value ?: DeletionParameters()
        val deletionType = deletionParameters.deletionType
        val chosenRange = deletionParameters.chosenRange

        when (deletionType) {
            is DeletionType.DeletionTypeAllData -> {
                return when (chosenRange) {
                    ChosenRange.DELETE_RANGE_LAST_24_HOURS ->
                        getString(R.string.confirming_question_one_day)
                    ChosenRange.DELETE_RANGE_LAST_7_DAYS ->
                        getString(R.string.confirming_question_one_week)
                    ChosenRange.DELETE_RANGE_LAST_30_DAYS ->
                        getString(R.string.confirming_question_one_month)
                    ChosenRange.DELETE_RANGE_ALL_DATA -> getString(R.string.confirming_question_all)
                }
            }
            is DeletionType.DeletionTypeHealthPermissionTypeData -> {
                val permissionTypeLabel = getString(deletionParameters.getPermissionTypeLabel())
                return when (chosenRange) {
                    ChosenRange.DELETE_RANGE_LAST_24_HOURS ->
                        getString(
                            R.string.confirming_question_data_type_one_day, permissionTypeLabel)
                    ChosenRange.DELETE_RANGE_LAST_7_DAYS ->
                        getString(
                            R.string.confirming_question_data_type_one_week, permissionTypeLabel)
                    ChosenRange.DELETE_RANGE_LAST_30_DAYS ->
                        getString(
                            R.string.confirming_question_data_type_one_month, permissionTypeLabel)
                    ChosenRange.DELETE_RANGE_ALL_DATA ->
                        getString(R.string.confirming_question_data_type_all, permissionTypeLabel)
                }
            }
            is DeletionType.DeletionTypeCategoryData -> {
                val categoryLabel = getString(deletionParameters.getCategoryLabel())
                return when (chosenRange) {
                    ChosenRange.DELETE_RANGE_LAST_24_HOURS ->
                        getString(R.string.confirming_question_category_one_day, categoryLabel)
                    ChosenRange.DELETE_RANGE_LAST_7_DAYS ->
                        getString(R.string.confirming_question_category_one_week, categoryLabel)
                    ChosenRange.DELETE_RANGE_LAST_30_DAYS ->
                        getString(R.string.confirming_question_category_one_month, categoryLabel)
                    ChosenRange.DELETE_RANGE_ALL_DATA ->
                        getString(R.string.confirming_question_category_all, categoryLabel)
                }
            }
            is DeletionType.DeleteDataEntry -> {
                return getString(R.string.confirming_question_single_entry)
            }
            is DeletionType.DeletionTypeAppData -> {
                val appName = deletionParameters.getAppName()
                return when (chosenRange) {
                    ChosenRange.DELETE_RANGE_LAST_24_HOURS ->
                        getString(R.string.confirming_question_app_data_one_day, appName)
                    ChosenRange.DELETE_RANGE_LAST_7_DAYS ->
                        getString(R.string.confirming_question_app_data_one_week, appName)
                    ChosenRange.DELETE_RANGE_LAST_30_DAYS ->
                        getString(R.string.confirming_question_app_data_one_month, appName)
                    ChosenRange.DELETE_RANGE_ALL_DATA ->
                        getString(R.string.confirming_question_app_data_all, appName)
                }
            }
            else -> {
                // TODO implement other flows
                throw UnsupportedOperationException("")
            }
        }
    }

    companion object {
        const val TAG = "ConfirmationDialogFragment"
    }
}
