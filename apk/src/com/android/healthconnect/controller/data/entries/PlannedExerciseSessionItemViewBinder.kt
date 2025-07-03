/**
 * Copyright (C) 2024 The Android Open Source Project
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
package com.android.healthconnect.controller.data.entries

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.shared.recyclerview.DeletionViewBinder
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.HealthConnectLoggerEntryPoint
import dagger.hilt.android.EntryPointAccessors

/** ViewBinder for PlannedExerciseSessionEntry. */
class PlannedExerciseSessionItemViewBinder(
    private val onItemClickedListener: OnClickEntryListener?,
    private val onSelectEntryListener: OnSelectEntryListener? = null,
) : DeletionViewBinder<FormattedEntry.PlannedExerciseSessionEntry, View> {
    private lateinit var logger: HealthConnectLogger

    override fun newView(parent: ViewGroup): View {
        val context = parent.context.applicationContext
        val hiltEntryPoint =
            EntryPointAccessors.fromApplication(context, HealthConnectLoggerEntryPoint::class.java)
        logger = hiltEntryPoint.logger()
        return LayoutInflater.from(parent.context)
            .inflate(R.layout.item_data_entry_new_ia, parent, false)
    }

    override fun bind(
        view: View,
        data: FormattedEntry.PlannedExerciseSessionEntry,
        index: Int,
        isDeletionState: Boolean,
        isChecked: Boolean,
    ) {
        val container = view.findViewById<LinearLayout>(R.id.item_data_entry_container)
        val header = view.findViewById<TextView>(R.id.item_data_entry_header)
        val title = view.findViewById<TextView>(R.id.item_data_entry_title)
        val checkBox = view.findViewById<CheckBox>(R.id.item_checkbox_button)
        header.text = data.header
        header.contentDescription = data.headerA11y

        container.setOnClickListener {
            if (isDeletionState) {
                onSelectEntryListener?.onSelectEntry(data.uuid, data.dataType, index)
                checkBox.toggle()
                title.contentDescription = data.titleA11y
                logger.logInteraction(logNameWithCheckbox)
            } else {
                logger.logInteraction(logNameWithoutCheckbox)
                onItemClickedListener?.onItemClicked(data.uuid, index)
            }
        }

        checkBox.isVisible = isDeletionState
        if (isDeletionState) {
            logger.logImpression(logNameWithCheckbox)
        } else {
            logger.logImpression(logNameWithoutCheckbox)
        }
        checkBox.isChecked = isChecked
        checkBox.setOnClickListener {
            onSelectEntryListener?.onSelectEntry(data.uuid, data.dataType, index)
            title.contentDescription = data.titleA11y
            logger.logInteraction(logNameWithCheckbox)
        }

        title.text = data.title
        title.contentDescription = data.titleA11y
    }
}
