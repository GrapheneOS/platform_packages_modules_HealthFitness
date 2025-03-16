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

import android.content.Context
import android.view.View
import androidx.annotation.DrawableRes
import androidx.lifecycle.ViewModel
import com.android.healthconnect.controller.shared.recyclerview.RecyclerViewItemDisplayType.GROUP_ITEM
import com.android.healthconnect.controller.shared.recyclerview.RecyclerViewItemDisplayType.HEADER_ITEM
import com.android.healthconnect.controller.shared.recyclerview.RecyclerViewItemDisplayType.STANDALONE_ITEM
import com.android.healthconnect.controller.shared.recyclerview.ViewBinder
import com.android.settingslib.widget.theme.R

/**
 * {@link RecyclerView.Adapter} that handles binding objects of entries list view. it handles
 * rendering grouping background.
 */
class ExpressiveEntriesAdapter(
    context: Context,
    itemClassToItemViewTypeMap: Map<Class<*>, Int>,
    itemViewTypeToViewBinderMap: Map<Int, ViewBinder<*, out View>>,
    viewModel: ViewModel,
) : EntriesAdapter(itemClassToItemViewTypeMap, itemViewTypeToViewBinderMap, viewModel) {

    private var cornerMappingList = mutableListOf<Int>()
    private var mNormalPaddingStart = 0
    private var mGroupPaddingStart = 0
    private var mNormalPaddingEnd = 0
    private var mGroupPaddingEnd = 0

    init {
        mNormalPaddingStart =
            context.resources.getDimensionPixelSize(R.dimen.settingslib_expressive_space_small1)
        mGroupPaddingStart = mNormalPaddingStart * 2
        mNormalPaddingEnd =
            context.resources.getDimensionPixelSize(R.dimen.settingslib_expressive_space_small1)
        mGroupPaddingEnd = mNormalPaddingEnd * 2
    }

    override fun updateData(entries: List<Any>) {
        super.updateData(entries)
        cornerMappingList = mutableListOf()
        mapCorners(cornerMappingList, entries)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        updateBackground(holder, position)
    }

    /** handle roundCorner background */
    private fun updateBackground(holder: ViewHolder, position: Int) {
        val backgroundRes = getRoundCornerDrawableRes(position)
        val v = holder.itemView
        val (paddingStart, paddingEnd) = getStartEndPadding(backgroundRes)
        v.setPaddingRelative(paddingStart, v.paddingTop, paddingEnd, v.paddingBottom)
        v.clipToOutline = backgroundRes != 0
        holder.itemView.setBackgroundResource(backgroundRes)
    }

    private fun getStartEndPadding(backgroundRes: Int): Pair<Int, Int> {
        return when {
            backgroundRes == 0 -> mNormalPaddingStart to mNormalPaddingEnd

            // Other items are suppose to have group padding.
            else -> mGroupPaddingStart to mGroupPaddingEnd
        }
    }

    @DrawableRes
    private fun getRoundCornerDrawableRes(position: Int): Int {
        val cornerType = cornerMappingList[position]

        if ((cornerType and ROUND_CORNER_CENTER) == 0) {
            return 0
        }

        val item =
            when {
                (cornerType and ROUND_CORNER_TOP) != 0 &&
                    (cornerType and ROUND_CORNER_BOTTOM) == 0 -> {
                    // the first
                    R.drawable.settingslib_round_background_top
                }

                (cornerType and ROUND_CORNER_BOTTOM) != 0 &&
                    (cornerType and ROUND_CORNER_TOP) == 0 -> {
                    // the last
                    R.drawable.settingslib_round_background_bottom
                }

                (cornerType and ROUND_CORNER_TOP) != 0 &&
                    (cornerType and ROUND_CORNER_BOTTOM) != 0 -> {
                    // the only one preference
                    R.drawable.settingslib_round_background
                }

                else -> {
                    // in the center
                    R.drawable.settingslib_round_background_center
                }
            }

        return item
    }

    private fun mapCorners(cornerStyles: MutableList<Int>, data: List<Any>) {
        var startItem = -1
        for (i in 0 until data.count()) {
            val item: FormattedEntry = getItem(i)
            val style =
                when (item.displayType) {
                    HEADER_ITEM -> {
                        startItem = -1 // reset start item
                        0
                    }

                    STANDALONE_ITEM -> {
                        startItem = -1 // reset start item
                        ROUND_CORNER_CENTER or ROUND_CORNER_TOP or ROUND_CORNER_BOTTOM
                    }

                    GROUP_ITEM -> {
                        var corner = ROUND_CORNER_CENTER
                        if (startItem == -1) {
                            startItem = i
                            corner = corner or ROUND_CORNER_TOP
                        }
                        if (i == data.count() - 1 || getItem(i + 1).displayType != GROUP_ITEM) {
                            corner = corner or ROUND_CORNER_BOTTOM
                        }
                        corner
                    }

                    else -> {
                        0
                    }
                }
            cornerStyles.add(style)
        }
    }

    companion object {
        private const val ROUND_CORNER_CENTER: Int = 1
        private const val ROUND_CORNER_TOP: Int = 1 shl 1
        private const val ROUND_CORNER_BOTTOM: Int = 1 shl 2
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

        fun build(context: Context) =
            ExpressiveEntriesAdapter(
                context,
                itemClassToItemViewTypeMap,
                itemViewTypeToViewBinderMap,
                viewModel,
            )
    }
}
