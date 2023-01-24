/**
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
package com.android.healthconnect.controller.dataentries

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.shared.recyclerview.ViewBinder

/** ViewBinder for FormattedDataEntry. */
class EntryItemViewBinder(private val onDeleteEntryClicked: OnDeleteEntryClicked) :
    ViewBinder<FormattedDataEntry, View> {

    override fun newView(parent: ViewGroup): View {
        return LayoutInflater.from(parent.context).inflate(R.layout.item_data_entry, parent, false)
    }

    override fun bind(view: View, data: FormattedDataEntry, index: Int) {
        val header = view.findViewById<TextView>(R.id.item_data_entry_header)
        val title = view.findViewById<TextView>(R.id.item_data_entry_title)
        val deleteButton = view.findViewById<ImageButton>(R.id.item_data_entry_delete)

        title.text = data.title
        title.contentDescription = data.titleA11y

        header.text = data.header
        header.contentDescription = data.headerA11y

        deleteButton.setOnClickListener { onDeleteEntryClicked.onDeleteEntryClicked(data, index) }
    }

    interface OnDeleteEntryClicked {
        fun onDeleteEntryClicked(dataEntry: FormattedDataEntry, index: Int)
    }
}
