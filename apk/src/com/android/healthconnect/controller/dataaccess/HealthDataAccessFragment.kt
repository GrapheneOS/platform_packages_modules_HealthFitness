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
package com.android.healthconnect.controller.dataaccess

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.commitNow
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.dataaccess.HealthDataAccessViewModel.DataAccessAppState
import com.android.healthconnect.controller.deletion.DeletionConstants.DELETION_TYPE
import com.android.healthconnect.controller.deletion.DeletionConstants.FRAGMENT_TAG_DELETION
import com.android.healthconnect.controller.deletion.DeletionConstants.START_DELETION_EVENT
import com.android.healthconnect.controller.deletion.DeletionFragment
import com.android.healthconnect.controller.deletion.DeletionType
import com.android.healthconnect.controller.permissions.data.HealthPermissionStrings.Companion.fromPermissionType
import com.android.healthconnect.controller.permissions.data.HealthPermissionType
import com.android.healthconnect.controller.permissiontypes.HealthPermissionTypesFragment.Companion.PERMISSION_TYPE_KEY
import com.android.healthconnect.controller.utils.setTitle
import com.android.settingslib.widget.TopIntroPreference
import dagger.hilt.android.AndroidEntryPoint

/** Fragment displaying health data access information. */
@AndroidEntryPoint(PreferenceFragmentCompat::class)
class HealthDataAccessFragment : Hilt_HealthDataAccessFragment() {

    companion object {
        private const val PERMISSION_TYPE_DESCRIPTION = "permission_type_description"
        private const val CAN_READ_SECTION = "can_read"
        private const val CAN_WRITE_SECTION = "can_write"
        private const val INACTIVE_SECTION = "inactive"
        private const val ALL_ENTRIES_BUTTON = "all_entries_button"
        private const val DELETE_PERMISSION_TYPE_DATA_BUTTON = "delete_permission_type_data"
    }
    private val viewModel: HealthDataAccessViewModel by viewModels()

    private lateinit var permissionType: HealthPermissionType

    private val mPermissionTypeDescription: TopIntroPreference? by lazy {
        preferenceScreen.findPreference(PERMISSION_TYPE_DESCRIPTION)
    }

    private val mCanReadSection: PreferenceGroup? by lazy {
        preferenceScreen.findPreference(CAN_READ_SECTION)
    }

    private val mCanWriteSection: PreferenceGroup? by lazy {
        preferenceScreen.findPreference(CAN_WRITE_SECTION)
    }

    private val mInactiveSection: PreferenceGroup? by lazy {
        preferenceScreen.findPreference(INACTIVE_SECTION)
    }

    private val mAllEntriesButton: Preference? by lazy {
        preferenceScreen.findPreference(ALL_ENTRIES_BUTTON)
    }

    private val mDeletePermissionTypeData: Preference? by lazy {
        preferenceScreen.findPreference(DELETE_PERMISSION_TYPE_DATA_BUTTON)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.health_data_access_screen, rootKey)
        if (requireArguments().containsKey(PERMISSION_TYPE_KEY) &&
            (requireArguments().get(PERMISSION_TYPE_KEY) != null)) {
            permissionType = (requireArguments().get(PERMISSION_TYPE_KEY)!!) as HealthPermissionType
        }
        maybeShowPermissionTypeDescription()
        mCanReadSection?.title =
            getString(R.string.can_read, getString(fromPermissionType(permissionType).label))
        mCanWriteSection?.title =
            getString(R.string.can_write, getString(fromPermissionType(permissionType).label))
        if (childFragmentManager.findFragmentByTag(FRAGMENT_TAG_DELETION) == null) {
            childFragmentManager.commitNow { add(DeletionFragment(), FRAGMENT_TAG_DELETION) }
        }

        mAllEntriesButton?.setOnPreferenceClickListener {
            findNavController()
                .navigate(
                    R.id.action_healthDataAccess_to_dataEntries,
                    bundleOf(PERMISSION_TYPE_KEY to permissionType))
            true
        }
        mDeletePermissionTypeData?.setOnPreferenceClickListener {
            val deletionType =
                DeletionType.DeletionTypeHealthPermissionTypeData(
                    healthPermissionType = permissionType)
            childFragmentManager.setFragmentResult(
                START_DELETION_EVENT, bundleOf(DELETION_TYPE to deletionType))
            true
        }
    }

    private fun maybeShowPermissionTypeDescription() {
        mPermissionTypeDescription?.isVisible = false
        if (permissionType == HealthPermissionType.EXERCISE) {
            mPermissionTypeDescription?.isVisible = true
            mPermissionTypeDescription?.setTitle(R.string.data_access_exercise_description)
        }
        if (permissionType == HealthPermissionType.SLEEP) {
            mPermissionTypeDescription?.isVisible = true
            mPermissionTypeDescription?.setTitle(R.string.data_access_sleep_description)
        }
    }

    override fun onResume() {
        super.onResume()
        setTitle(fromPermissionType(permissionType).label)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.appInfoMaps.observe(viewLifecycleOwner) { appInfoMap ->
            updateDataAccess(appInfoMap)
        }
    }

    private fun updateDataAccess(appInfoMap: Map<DataAccessAppState, List<AppInfo>>) {
        // TODO(b/245513815): Add inactive apps.
        // TODO(b/245513815): Add empty page.
        mCanReadSection?.removeAll()
        mCanWriteSection?.removeAll()
        mInactiveSection?.removeAll()

        if (appInfoMap.containsKey(DataAccessAppState.Read) &&
            appInfoMap[DataAccessAppState.Read]!!.isEmpty()) {
            mCanReadSection?.isVisible = false
        } else {
            mCanReadSection?.isVisible = true
            appInfoMap[DataAccessAppState.Write]!!.forEach { _appInfo ->
                mCanReadSection?.addPreference(
                    Preference(requireContext()).also {
                        it.setTitle(_appInfo.appName)
                        it.setIcon(_appInfo.icon)
                        // TODO(b/245513815): Navigate to App access page.
                    })
            }
        }
        if (appInfoMap.containsKey(DataAccessAppState.Write) &&
            appInfoMap[DataAccessAppState.Write]!!.isEmpty()) {
            mCanWriteSection?.isVisible = false
        } else {
            mCanWriteSection?.isVisible = true
            appInfoMap[DataAccessAppState.Write]!!.forEach { _appInfo ->
                mCanWriteSection?.addPreference(
                    Preference(requireContext()).also {
                        it.setTitle(_appInfo.appName)
                        it.setIcon(_appInfo.icon)
                    })
            }
        }
        if (appInfoMap.containsKey(DataAccessAppState.Inactive) &&
            appInfoMap[DataAccessAppState.Inactive]!!.isEmpty()) {
            mInactiveSection?.isVisible = false
        } else {
            mInactiveSection?.isVisible = true
            mInactiveSection?.addPreference(
                Preference(requireContext()).also {
                    it.summary =
                        getString(
                            R.string.inactive_apps_message,
                            getString(fromPermissionType(permissionType).label))
                })
            appInfoMap[DataAccessAppState.Inactive]!!.forEach { _appInfo ->
                mInactiveSection?.addPreference(
                    Preference(requireContext()).also {
                        it.setTitle(_appInfo.appName)
                        it.setIcon(_appInfo.icon)
                    })
            }
        }
    }
}
