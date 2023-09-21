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
package com.android.healthconnect.controller.datasources.appsources

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.permissiontypes.HealthPermissionTypesViewModel
import com.android.healthconnect.controller.shared.HealthDataCategoryInt
import com.android.healthconnect.controller.shared.app.AppMetadata
import java.text.NumberFormat

/** RecyclerView adapter that holds the list of app sources for this [HealthDataCategory]. */
class AppSourcesAdapter(
        appMetadataList: List<AppMetadata>,
        private val viewModel: HealthPermissionTypesViewModel,
        private val category: @HealthDataCategoryInt Int
) : RecyclerView.Adapter<AppSourcesAdapter.AppSourcesItemViewHolder?>() {

    private var listener: ItemTouchHelper? = null
    private var appMetadataList = appMetadataList.toMutableList()

    private val POSITION_CHANGED_PAYLOAD = Any()

    override fun onCreateViewHolder(
            viewGroup: ViewGroup,
            viewType: Int
    ): AppSourcesItemViewHolder {
        return AppSourcesItemViewHolder(
                LayoutInflater.from(viewGroup.context)
                        .inflate(R.layout.widget_app_source_layout, viewGroup, false),
                listener)
    }

    override fun onBindViewHolder(viewHolder: AppSourcesItemViewHolder, position: Int) {
        viewHolder.bind(position, appMetadataList[position].appName, isOnlyApp = appMetadataList.size == 1)
    }

    override fun getItemCount(): Int {
        return appMetadataList.size
    }

    fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        val movedAppInfo: AppMetadata = appMetadataList.removeAt(fromPosition)
        appMetadataList.add(
                if (toPosition > fromPosition + 1) toPosition - 1 else toPosition, movedAppInfo)
        notifyItemMoved(fromPosition, toPosition)
        if (toPosition < fromPosition) {
            notifyItemRangeChanged(
                    toPosition, fromPosition - toPosition + 1, POSITION_CHANGED_PAYLOAD)
        } else {
            notifyItemRangeChanged(
                    fromPosition, toPosition - fromPosition + 1, POSITION_CHANGED_PAYLOAD)
        }
        viewModel.updatePriorityList(category, appMetadataList.map { it.packageName })
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
    class AppSourcesItemViewHolder(itemView: View, onItemDragStartedListener: ItemTouchHelper?) :
            RecyclerView.ViewHolder(itemView) {
        private val appPositionView: TextView
        private val appNameView: TextView
        private val dragIconView: View

        private val onItemDragStartedListener: ItemTouchHelper?

        init {
            appPositionView = itemView.findViewById(R.id.app_position)
            appNameView = itemView.findViewById(R.id.app_name)
            dragIconView = itemView.findViewById(R.id.drag_icon)
            this.onItemDragStartedListener = onItemDragStartedListener
        }

        // These items are not clickable and so onTouch does not need to reimplement click
        // conditions.
        // Drag&drop in accessibility mode (talk back) is implemented as custom actions.
        @SuppressLint("ClickableViewAccessibility")
        fun bind(appPosition: Int, appName: String?, isOnlyApp: Boolean) {
            // Adding 1 to position as position starts from 0 but should show to the user starting
            // from 1.
            val positionString: String = NumberFormat.getIntegerInstance().format(appPosition + 1)
            appPositionView.text = positionString
            appNameView.text = appName
            // Hide drag icon if this is the only app in the list
            if (isOnlyApp) {
                dragIconView.visibility = View.INVISIBLE
                dragIconView.isClickable = false
            } else {
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
}
