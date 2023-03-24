/**
 * Copyright (C) 2022 The Android Open Source Project
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
package com.android.healthconnect.controller.permissiontypes.prioritylist

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.permissiontypes.HealthPermissionTypesViewModel
import com.android.healthconnect.controller.shared.app.AppMetadata

/** RecyclerView adapter that holds the list of the priority list. */
class PriorityListAdapter(
    appMetadataList: List<AppMetadata>,
    private val viewModel: HealthPermissionTypesViewModel
) : RecyclerView.Adapter<PriorityListAdapter.PriorityListItemViewHolder?>() {

    private var listener: ItemTouchHelper? = null
    private var appMetadataList = appMetadataList.toMutableList()

    override fun onCreateViewHolder(
        viewGroup: ViewGroup,
        viewType: Int
    ): PriorityListItemViewHolder {
        return PriorityListItemViewHolder(
            LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.priority_item, viewGroup, false),
            listener)
    }

    override fun onBindViewHolder(viewHolder: PriorityListItemViewHolder, position: Int) {
        viewHolder.bind(appMetadataList[position].appName, appMetadataList[position].icon)
    }

    override fun getItemCount(): Int {
        return appMetadataList.size
    }

    fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        val movedAppInfo: AppMetadata = appMetadataList.removeAt(fromPosition)
        appMetadataList.add(
            if (toPosition > fromPosition + 1) toPosition - 1 else toPosition, movedAppInfo)
        notifyItemMoved(fromPosition, toPosition)
        viewModel.setEditedPriorityList(appMetadataList)
        return true
    }

    fun getPackageNameList(): List<String> {
        return appMetadataList.stream().map(AppMetadata::packageName).toList()
    }

    fun setOnItemDragStartedListener(listener: ItemTouchHelper) {
        this.listener = listener
    }

    fun removeOnItemDragStartedListener() {
        listener = null
    }

    /** Shows a single item of the priority list. */
    class PriorityListItemViewHolder(itemView: View, onItemDragStartedListener: ItemTouchHelper?) :
        RecyclerView.ViewHolder(itemView) {
        private val appNameView: TextView
        private val appIconView: ImageView
        private val dragIconView: View

        private val onItemDragStartedListener: ItemTouchHelper?

        init {
            appNameView = itemView.findViewById(R.id.app_name)
            appIconView = itemView.findViewById(R.id.app_icon)
            dragIconView = itemView.findViewById(R.id.drag_icon)
            this.onItemDragStartedListener = onItemDragStartedListener
        }

        // These items are not clickable and so onTouch does not need to reimplement click
        // conditions.
        // Drag&drop in accessibility mode (talk back) is implemented as custom actions.
        @SuppressLint("ClickableViewAccessibility")
        fun bind(appName: String?, appIcon: Drawable?) {
            appNameView.text = appName
            appIconView.setImageDrawable(appIcon)
            dragIconView.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN ||
                    event.action == MotionEvent.ACTION_UP) {
                    onItemDragStartedListener?.startDrag(this)
                }
                false
            }
        }
    }
}
