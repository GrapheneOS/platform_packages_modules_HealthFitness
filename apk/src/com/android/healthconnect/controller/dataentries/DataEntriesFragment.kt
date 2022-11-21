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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.dataentries.DataEntriesFragmentViewModel.DataEntriesFragmentState.Empty
import com.android.healthconnect.controller.dataentries.DataEntriesFragmentViewModel.DataEntriesFragmentState.Loading
import com.android.healthconnect.controller.dataentries.DataEntriesFragmentViewModel.DataEntriesFragmentState.WithData
import com.android.healthconnect.controller.permissions.data.HealthPermissionType
import com.android.healthconnect.controller.permissiontypes.HealthPermissionTypesFragment.Companion.PERMISSION_TYPE_KEY
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant

/** Fragment to show health data entries by date. */
@AndroidEntryPoint(Fragment::class)
class DataEntriesFragment : Hilt_DataEntriesFragment() {

    private lateinit var permissionType: HealthPermissionType
    private val viewModel: DataEntriesFragmentViewModel by viewModels()

    private lateinit var dataNavigationView: DateNavigationView
    private lateinit var entriesRecyclerView: RecyclerView
    private lateinit var noDataView: TextView
    private lateinit var loadingView: View
    private val adapter = DataEntryAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_data_entries, container, false)

        permissionType =
            requireArguments().getSerializable(PERMISSION_TYPE_KEY) as HealthPermissionType

        dataNavigationView = view.findViewById(R.id.date_navigation_view)
        noDataView = view.findViewById(R.id.no_data_view)
        loadingView = view.findViewById(R.id.loading)
        entriesRecyclerView =
            view.findViewById<RecyclerView?>(R.id.data_entries_list).also {
                it.adapter = adapter
                it.layoutManager = LinearLayoutManager(context, VERTICAL, false)
            }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dataNavigationView.setDateChangedListener(
            object : DateNavigationView.OnDateChangedListener {
                override fun onDateChanged(selectedDate: Instant) {
                    viewModel.loadData(permissionType, selectedDate)
                }
            })
        viewModel.loadData(permissionType, dataNavigationView.getDate())
        viewModel.dataEntries.observe(viewLifecycleOwner) { state ->
            when (state) {
                is Loading -> {
                    loadingView.isVisible = true
                    noDataView.isVisible = false
                    entriesRecyclerView.isVisible = false
                }
                is Empty -> {
                    noDataView.isVisible = true
                    loadingView.isVisible = false
                    entriesRecyclerView.isVisible = false
                }
                is WithData -> {
                    entriesRecyclerView.isVisible = true
                    adapter.updateData(state.entries)
                    noDataView.isVisible = false
                    loadingView.isVisible = false
                }
            }
        }
    }
}
