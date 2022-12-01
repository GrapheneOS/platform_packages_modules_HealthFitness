package com.android.healthconnect.controller.deletion

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.deletion.DeletionConstants.CONFIRMATION_EVENT
import com.android.healthconnect.controller.deletion.DeletionConstants.GO_BACK_EVENT
import com.android.healthconnect.controller.shared.dialog.AlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

/**
 * A deletion {@link DialogFragment} asking confirmation from user for deleting data from from the
 * time range chosen on the previous dialog.
 */
@AndroidEntryPoint(DialogFragment::class)
class DeletionConfirmationDialogFragment : Hilt_DeletionConfirmationDialogFragment() {

    private val viewModel: DeletionViewModel by viewModels({ requireParentFragment() })

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val message = getString(R.string.confirming_question_message)

        return AlertDialogBuilder(this)
            .setTitle(buildTitle())
            .setIcon(R.attr.deleteIcon)
            .setMessage(message)
            .setPositiveButton(R.string.confirming_question_delete_button) { _, _ ->
                setFragmentResult(CONFIRMATION_EVENT, Bundle())
            }
            .setNegativeButton(R.string.confirming_question_go_back_button) { _, _ ->
                setFragmentResult(GO_BACK_EVENT, Bundle())
            }
            .create()
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
