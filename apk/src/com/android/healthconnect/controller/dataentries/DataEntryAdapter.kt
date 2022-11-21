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
package com.android.healthconnect.controller.dataentries

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.healthconnect.controller.R

class DataEntryAdapter : RecyclerView.Adapter<DataEntryAdapter.DataEntryViewHolder>() {

    private var entriesList: List<FormattedDataEntry> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DataEntryViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_data_entry, parent, false)
        return DataEntryViewHolder(view)
    }

    override fun onBindViewHolder(holder: DataEntryViewHolder, position: Int) {
        holder.bind(entriesList[position])
    }

    override fun getItemCount(): Int {
        return entriesList.size
    }

    fun updateData(entriesList: List<FormattedDataEntry>) {
        this.entriesList = entriesList
        notifyDataSetChanged()
    }

    class DataEntryViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        private val header: TextView by lazy { view.findViewById(R.id.item_data_entry_header) }
        private val title: TextView by lazy { view.findViewById(R.id.item_data_entry_title) }

        fun bind(dataEntry: FormattedDataEntry) {
            header.text = dataEntry.header
            header.contentDescription = dataEntry.headerA11y

            title.text = dataEntry.title
            title.contentDescription = dataEntry.titleA11y
        }
    }
}
