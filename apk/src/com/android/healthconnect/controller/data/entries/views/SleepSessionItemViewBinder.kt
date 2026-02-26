/*
 * Copyright (C) 2026 The Android Open Source Project
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
package com.android.healthconnect.controller.data.entries.views

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.data.entries.FormattedEntry.SleepSessionEntry
import com.android.healthconnect.controller.data.entries.OnClickEntryListener
import com.android.healthconnect.controller.data.entries.OnSelectEntryListener
import com.android.healthconnect.controller.shared.recyclerview.DeletionViewBinder
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.HealthConnectLoggerEntryPoint
import com.android.healthconnect.controller.utils.setupAccessibilityDelegateForCheckbox
import dagger.hilt.android.EntryPointAccessors

/** ViewBinder for [SleepSessionEntry]. */
class SleepSessionItemViewBinder(
    private val onItemClickedListener: OnClickEntryListener?,
    private val onSelectEntryListener: OnSelectEntryListener? = null,
) : DeletionViewBinder<SleepSessionEntry, View> {

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
            .inflate(R.layout.item_sleep_session_entry_new_ia, parent, false)
    }

    override fun bind(
        view: View,
        data: SleepSessionEntry,
        index: Int,
        isDeletionState: Boolean,
        isChecked: Boolean,
    ) {
        val divider = view.findViewById<LinearLayout>(R.id.item_data_entry_divider)
        val header = view.findViewById<TextView>(R.id.item_data_entry_header)
        val title = view.findViewById<TextView>(R.id.item_data_entry_title)
        val notes = view.findViewById<TextView>(R.id.item_data_entry_notes)
        val checkBox = view.findViewById<CheckBox>(R.id.item_checkbox_button)

        header.text = data.header
        notes.isVisible = !data.notes.isNullOrBlank()
        notes.text = data.notes
        divider.isVisible = false
        view.setOnClickListener {
            if (isDeletionState) {
                onSelectEntryListener?.onSelectEntry(data.uuid, data.dataType, index)
                checkBox.toggle()
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
            logger.logInteraction(logNameWithCheckbox)
        }
        checkBox.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        checkBox.isFocusable = false
        checkBox.contentDescription = null

        title.text = data.title

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
