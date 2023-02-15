/**
 * Copyright (C) 2023 The Android Open Source Project
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
package com.android.healthconnect.controller.dataentries

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.dataentries.FormattedEntry.SleepSessionEntry
import com.android.healthconnect.controller.shared.recyclerview.ViewBinder

/** ViewBinder for SleepSessionEntry. */
class SleepSessionItemViewBinder(
    private val showSecondAction: Boolean = true,
    private val onItemClickedListener: OnClickEntryListener?,
    private val onDeleteEntryListenerClicked: OnDeleteEntryListener?,
) : ViewBinder<SleepSessionEntry, View> {

    override fun newView(parent: ViewGroup): View {
        return LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sleep_session_entry, parent, false)
    }

    override fun bind(view: View, data: SleepSessionEntry, index: Int) {
        val container = view.findViewById<RelativeLayout>(R.id.item_data_entry_container)
        val divider = view.findViewById<LinearLayout>(R.id.item_data_entry_divider)
        val header = view.findViewById<TextView>(R.id.item_data_entry_header)
        val title = view.findViewById<TextView>(R.id.item_data_entry_title)
        val notes = view.findViewById<TextView>(R.id.item_data_entry_notes)
        val deleteButton = view.findViewById<ImageButton>(R.id.item_data_entry_delete)

        title.text = data.title
        title.contentDescription = data.titleA11y
        header.text = data.header
        header.contentDescription = data.headerA11y
        notes.isVisible = !data.notes.isNullOrBlank()
        notes.text = data.notes
        deleteButton.isVisible = showSecondAction
        divider.isVisible = showSecondAction

        deleteButton.setOnClickListener {
            onDeleteEntryListenerClicked?.onDeleteEntry(data.uuid, data.dataType, index)
        }
        container.setOnClickListener { onItemClickedListener?.onItemClicked(data.uuid, index) }
    }
}
