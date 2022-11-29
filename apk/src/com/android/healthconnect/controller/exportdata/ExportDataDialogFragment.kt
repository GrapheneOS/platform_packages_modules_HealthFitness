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
package com.android.healthconnect.controller.exportdata

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.CompoundButton.OnCheckedChangeListener
import android.widget.CompoundButton.VISIBLE
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.shared.dialog.AlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint(DialogFragment::class)
class ExportDataDialogFragment : Hilt_ExportDataDialogFragment() {

    private lateinit var checkboxView: View
    private lateinit var selectAllCheckBox: CheckBox
    private var selectAllListener: OnCheckedChangeListener =
        OnCheckedChangeListener { checkbox, isChecked ->
            if (checkbox.isPressed) {
                viewModel.setAllCategoriesChecked(isChecked)
            }
        }

    private val viewModel: ExportDataViewModel by viewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        checkboxView = layoutInflater.inflate(R.layout.export_data_picker, null)
        selectAllCheckBox = checkboxView.findViewById(R.id.select_all_check_box)

        selectAllCheckBox.setOnCheckedChangeListener(selectAllListener)

        viewModel.allCategoryStates.observe(this) { renderCheckboxes(it) }

        return AlertDialogBuilder(this)
            .setTitle(R.string.export_data_title)
            .setIcon(R.attr.exportDataIcon)
            .setView(checkboxView)
            .setNegativeButton(android.R.string.cancel)
            .setPositiveButton(R.string.export_data_next_button) { _, _ -> viewModel.startExport() }
            .create()
    }

    /** Renders checkboxes of the available categories and sets check listeners. */
    private fun renderCheckboxes(categoryStates: ArrayList<ExportDataSelectionItem>) {

        val selectAllItemSelection =
            !categoryStates.stream().anyMatch { currentCategory -> !currentCategory.selected }
        updateSelectAllView(selectAllItemSelection)

        for (index in 0 until categoryStates.size) {
            val item = categoryStates[index]
            val checkBox = checkboxView.findViewById<CheckBox>(item.checkboxId)
            checkBox.isChecked = item.selected
            checkBox.visibility = VISIBLE

            checkBox.setOnCheckedChangeListener { _, isChecked ->
                item.selected = isChecked
                categoryStates[index] = item
                viewModel.setCategoryStates(categoryStates)
            }
        }
    }

    /** Updates the state of the (de)select all checkbox and sets the check listener. */
    private fun updateSelectAllView(selectAllItemSelection: Boolean) {
        if (selectAllItemSelection) {
            selectAllCheckBox.text = getString(R.string.export_data_deselect_all)
        } else {
            selectAllCheckBox.text = getString(R.string.export_data_select_all)
        }
        selectAllCheckBox.setOnCheckedChangeListener(null)
        selectAllCheckBox.isChecked = selectAllItemSelection
        selectAllCheckBox.setOnCheckedChangeListener(selectAllListener)
    }

    companion object {
        const val TAG = "ExportDataDialogFragment"
    }
}
