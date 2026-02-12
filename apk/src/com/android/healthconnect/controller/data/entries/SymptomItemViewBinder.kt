/*
 * Copyright (C) 2025 The Android Open Source Project
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
package com.android.healthconnect.controller.data.entries

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.core.view.isVisible
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.data.entries.FormattedEntry.SymptomEntry
import com.android.healthconnect.controller.shared.recyclerview.DeletionViewBinder
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.HealthConnectLoggerEntryPoint
import com.android.healthconnect.controller.utils.setupAccessibilityDelegateForCheckbox
import dagger.hilt.android.EntryPointAccessors

// TODO(b/446846882): Refactor view binders so that we have a single view binder for similar entries
//  with notes like symptoms and mindfulness.
class SymptomItemViewBinder(private val onSelectEntryListener: OnSelectEntryListener) :
    DeletionViewBinder<SymptomEntry, View> {

    private lateinit var logger: HealthConnectLogger

    override fun newView(parent: ViewGroup): View {
        val context = parent.context.applicationContext
        val hiltEntryPoint =
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                HealthConnectLoggerEntryPoint::class.java,
            )
        logger = hiltEntryPoint.logger()
        val inflater = LayoutInflater.from(parent.context)
        return inflater.inflate(R.layout.item_symptom_entry, parent, false)
    }

    override fun bind(
        view: View,
        data: SymptomEntry,
        index: Int,
        isDeletionState: Boolean,
        isChecked: Boolean,
    ) {
        val header = view.findViewById<TextView>(R.id.item_data_entry_header)
        val title = view.findViewById<TextView>(R.id.item_data_entry_title)
        header.text = data.header
        title.text = data.title

        val notesView = view.findViewById<TextView>(R.id.item_data_entry_notes)
        if (!data.notes.isNullOrBlank()) {
            notesView.text = data.notes
            notesView.visibility = View.VISIBLE
        } else {
            notesView.visibility = View.GONE
        }

        val checkbox = view.findViewById<CheckBox>(R.id.item_checkbox_button)
        checkbox.isVisible = isDeletionState
        checkbox.isChecked = isChecked
        checkbox.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        checkbox.isFocusable = false
        checkbox.contentDescription = null

        val divider = view.findViewById<View>(R.id.item_data_entry_divider)
        divider.isVisible = false // Always hide for symptom entries

        // The view is only clickable in deletion state.
        view.isClickable = isDeletionState
        if (isDeletionState) {
            view.setOnClickListener {
                logger.logInteraction(logNameWithCheckbox)
                checkbox.toggle()
                onSelectEntryListener.onSelectEntry(
                    id = data.uuid,
                    dataType = data.dataType,
                    index = index,
                )
            }
            checkbox.setOnClickListener {
                onSelectEntryListener.onSelectEntry(
                    id = data.uuid,
                    dataType = data.dataType,
                    index = index,
                )
                logger.logInteraction(logNameWithCheckbox)
            }
        } else {
            // Remove any previous listeners to ensure it's not clickable.
            view.setOnClickListener(null)
            checkbox.setOnClickListener(null)
        }

        view.contentDescription = "${data.headerA11y}, ${data.titleA11y}"
        view.isFocusable = true
        setupAccessibilityDelegateForCheckbox(
            view,
            isDeletionState,
            isChecked,
            view.context.getString(R.string.a11y_action_select),
        )
    }
}
