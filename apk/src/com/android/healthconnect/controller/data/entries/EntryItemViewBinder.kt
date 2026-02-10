/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import com.android.healthconnect.controller.data.entries.FormattedEntry.FormattedDataEntry
import com.android.healthconnect.controller.shared.recyclerview.DeletionViewBinder
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.HealthConnectLoggerEntryPoint
import com.android.healthconnect.controller.utils.setupAccessibilityDelegateForCheckbox
import dagger.hilt.android.EntryPointAccessors

/** ViewBinder for FormattedDataEntry. */
class EntryItemViewBinder(private val onSelectEntryListener: OnSelectEntryListener? = null) :
    DeletionViewBinder<FormattedDataEntry, View> {

    private lateinit var logger: HealthConnectLogger

    override fun newView(parent: ViewGroup): View {
        val context = parent.context.applicationContext
        val hiltEntryPoint =
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                HealthConnectLoggerEntryPoint::class.java,
            )
        logger = hiltEntryPoint.logger()
        return LayoutInflater.from(parent.context)
            .inflate(R.layout.item_data_entry_new_ia, parent, false)
    }

    override fun bind(
        view: View,
        data: FormattedDataEntry,
        index: Int,
        isDeletionState: Boolean,
        isChecked: Boolean,
    ) {
        val container = view.findViewById<LinearLayout>(R.id.item_data_entry_container)
        val header = view.findViewById<TextView>(R.id.item_data_entry_header)
        val title = view.findViewById<TextView>(R.id.item_data_entry_title)
        val checkBox = view.findViewById<CheckBox>(R.id.item_checkbox_button)

        if (isDeletionState) {
            container.isClickable = true
            container.setOnClickListener {
                onSelectEntryListener?.onSelectEntry(data.uuid, data.dataType, index)
                checkBox.toggle()
                logger.logInteraction(logNameWithCheckbox)
            }
        } else {
            container.isClickable = false
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
            logger.logInteraction(logNameWithCheckbox)
        }
        checkBox.tag = if (isDeletionState) "checkbox" else ""
        checkBox.contentDescription = null
        checkBox.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        checkBox.isFocusable = false

        title.text = data.title
        header.text = data.header

        container.contentDescription = "${data.headerA11y}, ${data.titleA11y}"
        container.isFocusable = true
        setupAccessibilityDelegateForCheckbox(
            container,
            isDeletionState,
            isChecked,
            container.context.getString(R.string.a11y_action_select),
        )
    }
}
