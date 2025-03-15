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

import android.view.View
import androidx.lifecycle.ViewModel
import com.android.healthconnect.controller.shared.DataType
import com.android.healthconnect.controller.shared.recyclerview.DeletionViewBinder
import com.android.healthconnect.controller.shared.recyclerview.RecyclerViewAdapter
import com.android.healthconnect.controller.shared.recyclerview.ViewBinder

/**
 * {@link RecyclerView.Adapter} that handles binding objects of entries list view. it handles
 * 1. showing/hiding checkboxes used for deletion
 * 2. adding/ removing select all option in top of the list
 * 3. showing / hiding aggregation.
 */
open class EntriesAdapter(
    itemClassToItemViewTypeMap: Map<Class<*>, Int>,
    private val itemViewTypeToViewBinderMap: Map<Int, ViewBinder<*, out View>>,
    private val viewModel: ViewModel,
) : RecyclerViewAdapter(itemClassToItemViewTypeMap, itemViewTypeToViewBinderMap) {

    private var isSelectAllChecked: Boolean = false
    private var isDeletionState = false
    private var isChecked = false
    private var deleteMap: Map<String, DataType> = emptyMap()

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        val viewBinder: ViewBinder<Any, View> =
            checkNotNull(itemViewTypeToViewBinderMap[getItemViewType(position)])
                as ViewBinder<Any, View>

        val item: FormattedEntry = getItem(position)

        if (item is FormattedEntry.SelectAllHeader) {
            (viewBinder as DeletionViewBinder).bind(
                holder.itemView,
                item,
                position,
                isDeletionState,
                isSelectAllChecked,
            )
        } else if (viewBinder is DeletionViewBinder && viewModel is EntriesViewModel) {
            this.deleteMap = viewModel.mapOfEntriesToBeDeleted.value.orEmpty()
            isChecked = item.uuid in deleteMap
            viewBinder.bind(holder.itemView, item, position, isDeletionState, isChecked)
        }
    }

    fun insertSelectAll(selectAll: FormattedEntry.SelectAllHeader) {
        val entriesList = (viewModel as EntriesViewModel).getEntriesList()
        if (entriesList.isNotEmpty() && entriesList.first() !is FormattedEntry.SelectAllHeader) {
            entriesList.add(0, selectAll)
            updateData(entriesList)
        }
    }

    fun removeSelectAll() {
        val entriesList = (viewModel as EntriesViewModel).getEntriesList()
        if (entriesList.isNotEmpty() && entriesList.first() is FormattedEntry.SelectAllHeader) {
            entriesList.removeAt(0)
            updateData(entriesList)
        }
        // to prevent Select all from being checked the next time we trigger deletion
        this.isSelectAllChecked = false
    }

    fun checkSelectAll(isChecked: Boolean) {
        this.isSelectAllChecked = isChecked
        notifyDataSetChanged()
    }

    fun showCheckBox(isDeletionState: Boolean) {
        this.isDeletionState = isDeletionState
        notifyDataSetChanged()
    }

    fun insertAggregation(aggregation: FormattedEntry.FormattedAggregation) {
        val entriesList = (viewModel as EntriesViewModel).getEntriesList()
        if (
            entriesList.isNotEmpty() && entriesList.first() !is FormattedEntry.FormattedAggregation
        ) {
            entriesList.add(0, aggregation)
            updateData(entriesList)
        }
    }

    fun removeAggregation() {
        val entriesList = (viewModel as EntriesViewModel).getEntriesList()
        if (
            entriesList.isNotEmpty() && entriesList.first() is FormattedEntry.FormattedAggregation
        ) {
            entriesList.removeAt(0)
            updateData(entriesList)
        }
    }

    override fun getItem(position: Int): FormattedEntry {
        val item = super.getItem(position)
        if (item !is FormattedEntry) {
            throw IllegalStateException("Entries adapter can only render FormattedEntry!")
        }
        return item
    }

    class Builder {
        companion object {
            // Base item view type to use when setting a view binder for objects of a specific class
            private const val BASE_ITEM_VIEW_TYPE = 100
        }

        private var nextItemType = BASE_ITEM_VIEW_TYPE
        private val itemClassToItemViewTypeMap: MutableMap<Class<*>, Int> = mutableMapOf()
        private val itemViewTypeToViewBinderMap: MutableMap<Int, ViewBinder<*, out View>> =
            mutableMapOf()
        private lateinit var viewModel: ViewModel

        fun <T> setViewBinder(clazz: Class<T>, viewBinder: ViewBinder<T, out View>): Builder {
            itemClassToItemViewTypeMap[clazz] = nextItemType
            itemViewTypeToViewBinderMap[nextItemType] = viewBinder
            nextItemType++
            return this
        }

        fun setViewModel(viewModel: ViewModel): Builder {
            this.viewModel = viewModel
            return this
        }

        fun build() =
            EntriesAdapter(itemClassToItemViewTypeMap, itemViewTypeToViewBinderMap, viewModel)
    }
}
