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
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.commitNow
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.dataentries.DataEntriesFragmentViewModel.DataEntriesFragmentState.Empty
import com.android.healthconnect.controller.dataentries.DataEntriesFragmentViewModel.DataEntriesFragmentState.Loading
import com.android.healthconnect.controller.dataentries.DataEntriesFragmentViewModel.DataEntriesFragmentState.LoadingFailed
import com.android.healthconnect.controller.dataentries.DataEntriesFragmentViewModel.DataEntriesFragmentState.WithData
import com.android.healthconnect.controller.deletion.DeletionConstants.DELETION_TYPE
import com.android.healthconnect.controller.deletion.DeletionConstants.FRAGMENT_TAG_DELETION
import com.android.healthconnect.controller.deletion.DeletionConstants.START_DELETION_EVENT
import com.android.healthconnect.controller.deletion.DeletionFragment
import com.android.healthconnect.controller.deletion.DeletionType
import com.android.healthconnect.controller.permissions.data.HealthPermissionStrings.Companion.fromPermissionType
import com.android.healthconnect.controller.permissions.data.HealthPermissionType
import com.android.healthconnect.controller.permissiontypes.HealthPermissionTypesFragment.Companion.PERMISSION_TYPE_KEY
import com.android.healthconnect.controller.utils.setTitle
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
    private lateinit var errorView: View
    private val adapter = DataEntryAdapter()
    private val menuProvider =
        object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.data_entries, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.menu_open_units -> {
                        findNavController()
                            .navigate(R.id.action_dataEntriesFragment_to_unitsFragment)
                        true
                    }
                    else -> false
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_data_entries, container, false)
        if (requireArguments().containsKey(PERMISSION_TYPE_KEY)) {
            permissionType =
                arguments?.getSerializable(PERMISSION_TYPE_KEY, HealthPermissionType::class.java)
                    ?: throw IllegalArgumentException("PERMISSION_TYPE_KEY can't be null!")
        }
        setTitle(fromPermissionType(permissionType).uppercaseLabel)
        setupMenu()
        dataNavigationView = view.findViewById(R.id.date_navigation_view)
        noDataView = view.findViewById(R.id.no_data_view)
        errorView = view.findViewById(R.id.error_view)
        loadingView = view.findViewById(R.id.loading)
        entriesRecyclerView =
            view.findViewById<RecyclerView?>(R.id.data_entries_list).also {
                it.adapter = adapter
                it.layoutManager = LinearLayoutManager(context, VERTICAL, false)
            }

        if (childFragmentManager.findFragmentByTag(FRAGMENT_TAG_DELETION) == null) {
            childFragmentManager.commitNow { add(DeletionFragment(), FRAGMENT_TAG_DELETION) }
        }

        return view
    }

    private fun setupMenu() {
        (activity as MenuHost).addMenuProvider(
            menuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter.setOnDeleteEntrySelected(
            object : DataEntryAdapter.OnDeleteEntrySelected {
                override fun onDeleteEntrySelected(dataEntry: FormattedDataEntry) {
                    val deletionType =
                        DeletionType.DeleteDataEntry(dataEntry.uuid, dataEntry.dataType)
                    childFragmentManager.setFragmentResult(
                        START_DELETION_EVENT, bundleOf(DELETION_TYPE to deletionType))
                }
            })

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
                    errorView.isVisible = false
                    entriesRecyclerView.isVisible = false
                }
                is Empty -> {
                    noDataView.isVisible = true
                    loadingView.isVisible = false
                    errorView.isVisible = false
                    entriesRecyclerView.isVisible = false
                }
                is WithData -> {
                    entriesRecyclerView.isVisible = true
                    adapter.updateData(state.entries)
                    errorView.isVisible = false
                    noDataView.isVisible = false
                    loadingView.isVisible = false
                }
                is LoadingFailed -> {
                    errorView.isVisible = true
                    loadingView.isVisible = false
                    noDataView.isVisible = false
                    entriesRecyclerView.isVisible = false
                }
            }
        }
    }
}
