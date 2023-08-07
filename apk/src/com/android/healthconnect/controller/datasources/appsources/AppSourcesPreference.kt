package com.android.healthconnect.controller.datasources.appsources

import android.content.Context
import android.util.Log
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.permissiontypes.HealthPermissionTypesViewModel
import com.android.healthconnect.controller.shared.HealthDataCategoryInt
import com.android.healthconnect.controller.shared.app.AppMetadata

class AppSourcesPreference
constructor(
        context: Context,
        val viewModel: HealthPermissionTypesViewModel,
        val category: @HealthDataCategoryInt Int): Preference(context) {

    init {
        layoutResource = R.layout.widget_linear_layout_preference
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val priorityList: List<AppMetadata> = viewModel.editedPriorityList.value ?: emptyList()
        val priorityListView = holder.findViewById(R.id.linear_layout_recycle_view) as RecyclerView
        val adapter = AppSourcesAdapter(priorityList, viewModel, category)

        priorityListView.layoutManager = AppSourcesLinearLayoutManager(context, adapter)
        priorityListView.adapter = adapter
        val callback = AppSourcesItemMoveCallback(adapter)
        val priorityListMover = ItemTouchHelper(callback)
        adapter.setOnItemDragStartedListener(priorityListMover)
        priorityListMover.attachToRecyclerView(priorityListView)

    }
}