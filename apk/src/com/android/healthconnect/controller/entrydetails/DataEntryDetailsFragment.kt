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
package com.android.healthconnect.controller.entrydetails

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.dataentries.FormattedEntry.FormattedSessionDetail
import com.android.healthconnect.controller.dataentries.FormattedEntry.FormattedSessionEntry
import com.android.healthconnect.controller.dataentries.SessionItemViewBinder
import com.android.healthconnect.controller.deletion.DeletionConstants
import com.android.healthconnect.controller.deletion.DeletionType
import com.android.healthconnect.controller.entrydetails.DataEntryDetailsViewModel.DateEntryFragmentState
import com.android.healthconnect.controller.entrydetails.DataEntryDetailsViewModel.DateEntryFragmentState.Loading
import com.android.healthconnect.controller.entrydetails.DataEntryDetailsViewModel.DateEntryFragmentState.LoadingFailed
import com.android.healthconnect.controller.entrydetails.DataEntryDetailsViewModel.DateEntryFragmentState.WithData
import com.android.healthconnect.controller.permissions.data.HealthPermissionType
import com.android.healthconnect.controller.permissiontypes.HealthPermissionTypesFragment.Companion.PERMISSION_TYPE_KEY
import com.android.healthconnect.controller.shared.DataType
import com.android.healthconnect.controller.shared.recyclerview.RecyclerViewAdapter
import com.android.healthconnect.controller.utils.setTitle
import com.android.healthconnect.controller.utils.setupMenu
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint(Fragment::class)
class DataEntryDetailsFragment : Hilt_DataEntryDetailsFragment() {

    companion object {
        private const val ENTRY_ID_KEY = "entry_id_key"

        fun createBundle(permissionType: HealthPermissionType, entryId: String): Bundle {
            return bundleOf(PERMISSION_TYPE_KEY to permissionType, ENTRY_ID_KEY to entryId)
        }
    }

    private val viewModel: DataEntryDetailsViewModel by viewModels()

    private lateinit var permissionType: HealthPermissionType
    private lateinit var recyclerView: RecyclerView
    private lateinit var entryId: String
    private lateinit var loadingView: View
    private lateinit var errorView: View
    private lateinit var detailsAdapter: RecyclerViewAdapter
    private val sessionViewBinder by lazy {
        SessionItemViewBinder(
            showSecondAction = false,
            onItemClickedListener = null,
            onDeleteEntryClicked =
                object : SessionItemViewBinder.OnDeleteEntryClicked {
                    override fun onDeleteEntryClicked(
                        dataEntry: FormattedSessionEntry,
                        index: Int
                    ) {
                        deleteEntry(dataEntry.uuid, dataEntry.dataType, index)
                    }
                })
    }
    private val sessionDetailViewBinder by lazy { SessionDetailViewBinder() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_data_entry_details, container, false)
        permissionType =
            requireArguments()
                .getSerializable(PERMISSION_TYPE_KEY, HealthPermissionType::class.java)
                ?: throw IllegalArgumentException("PERMISSION_TYPE_KEY can't be null!")

        entryId =
            requireArguments().getString(ENTRY_ID_KEY)
                ?: throw IllegalArgumentException("ENTRY_ID_KEY can't be null!")
        errorView = view.findViewById(R.id.error_view)
        loadingView = view.findViewById(R.id.loading)
        detailsAdapter =
            RecyclerViewAdapter.Builder()
                .setViewBinder(FormattedSessionEntry::class.java, sessionViewBinder)
                .setViewBinder(FormattedSessionDetail::class.java, sessionDetailViewBinder)
                .build()
        recyclerView =
            view.findViewById<RecyclerView?>(R.id.data_entries_list).apply {
                layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
                adapter = detailsAdapter
            }
        viewModel.loadEntryData(permissionType, entryId)
        setTitle(R.string.entry_details_title)
        setupMenu(R.menu.data_entries, viewLifecycleOwner) { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_open_units -> {
                    findNavController()
                        .navigate(R.id.action_dataEntryDetailsFragment_to_unitFragment)
                    true
                }
                else -> false
            }
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.sessionData.observe(viewLifecycleOwner) { state -> updateUI(state) }
    }

    private fun updateUI(state: DateEntryFragmentState) {
        when (state) {
            is Loading -> {
                loadingView.isVisible = true
                errorView.isVisible = false
                recyclerView.isVisible = false
            }
            is LoadingFailed -> {
                errorView.isVisible = true
                loadingView.isVisible = false
                recyclerView.isVisible = false
            }
            is WithData -> {
                recyclerView.isVisible = true
                detailsAdapter.updateData(state.data)
                errorView.isVisible = false
                loadingView.isVisible = false
            }
        }
    }

    private fun deleteEntry(uuid: String, dataType: DataType, index: Int) {
        val deletionType = DeletionType.DeleteDataEntry(uuid, dataType, index)
        childFragmentManager.setFragmentResult(
            DeletionConstants.START_DELETION_EVENT,
            bundleOf(DeletionConstants.DELETION_TYPE to deletionType))
    }
}
