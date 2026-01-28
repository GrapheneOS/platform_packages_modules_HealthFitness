/*
 * Copyright (C) 2023 The Android Open Source Project
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
package com.android.healthconnect.controller.datasources

import android.health.connect.DeviceDataSourceInfo
import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.datasources.DataSourcesViewModel.PotentialAppSourcesState
import com.android.healthconnect.controller.datasources.DataSourcesViewModel.PriorityListState
import com.android.healthconnect.controller.navigation.CATEGORY_KEY
import com.android.healthconnect.controller.permissions.connectedapps.HealthAppPreference
import com.android.healthconnect.controller.shared.Constants
import com.android.healthconnect.controller.shared.HealthDataCategoryInt
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.shared.preference.HealthPreferenceFragment
import com.android.healthconnect.controller.utils.asAppMetadata
import com.android.healthconnect.controller.utils.findCurrentDeviceId
import com.android.healthconnect.controller.utils.logging.AddAnAppElement
import com.android.healthconnect.controller.utils.logging.PageName
import com.android.healthfitness.flags.Flags.deviceDataProvidersApi
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint(HealthPreferenceFragment::class)
class AddAnAppFragment : Hilt_AddAnAppFragment() {

    private val dataSourcesViewModel: DataSourcesViewModel by activityViewModels()
    @HealthDataCategoryInt private var category: Int = 0

    private var currentPriority: List<AppMetadata> = listOf()

    init {
        this.setPageName(PageName.ADD_AN_APP_PAGE)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.add_an_app_screen, rootKey)
        if (requireArguments().containsKey(CATEGORY_KEY)) {
            category = requireArguments().getInt(CATEGORY_KEY)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dataSourcesViewModel.loadData(category)
        dataSourcesViewModel.dataSourcesInfo.observe(viewLifecycleOwner) { dataSourcesInfoState ->
            if (dataSourcesInfoState.isLoading()) {
                setLoading(true)
            } else if (dataSourcesInfoState.isLoadingFailed()) {
                setLoading(false)
                setError(true)
            } else if (dataSourcesInfoState.isWithData()) {
                setLoading(false)
                val currentPriorityList =
                    (dataSourcesInfoState.priorityListState as PriorityListState.WithData)
                        .priorityList
                val potentialAppSources =
                    (dataSourcesInfoState.potentialAppSourcesState
                            as PotentialAppSourcesState.WithData)
                        .appSources
                val deviceDataSourcesInfo =
                    if (deviceDataProvidersApi())
                        (dataSourcesInfoState.deviceDataSourcesState
                                as DataSourcesViewModel.DeviceDataSourcesState.WithData)
                            .deviceDataSourcesInfo
                    else emptySet()
                currentPriorityList.let { currentPriority = it }
                updateAppsList(potentialAppSources, deviceDataSourcesInfo)
            }
        }
    }

    private fun updateAppsList(
        appSources: List<AppMetadata>,
        deviceDataSourcesInfo: Set<DeviceDataSourceInfo>,
    ) {
        preferenceScreen.removeAll()
        appSources
            .sortedBy { it.appName }
            .map { appMetadata ->
                if (!deviceDataProvidersApi()) return@map appMetadata

                val deviceInfo =
                    deviceDataSourcesInfo.find {
                        it.deviceDataOrigin.packageName == appMetadata.packageName
                    }
                deviceInfo?.asAppMetadata(requireContext()) ?: appMetadata
            }
            .forEach { appMetadata ->
                preferenceScreen.addPreference(
                    HealthAppPreference(requireContext(), appMetadata).also { preference ->
                        preference.logName = AddAnAppElement.POTENTIAL_PRIORITY_APP_BUTTON
                        preference.setOnPreferenceClickListener {
                            // add this app to the bottom of the priority list
                            val newPriority =
                                currentPriority
                                    .toMutableList()
                                    .also { it.add(appMetadata) }
                                    .toList()
                            dataSourcesViewModel.updatePriorityList(
                                newPriority.map { it.packageName }.toList(),
                                category,
                            )
                            findNavController().popBackStack()
                            true
                        }
                        // TODO(b/477226135): Handle two sources for current device
                        if (appMetadata.packageName == Constants.DEVICE_DATA_PROVIDER_PACKAGE) {
                            preference.summary = getString(R.string.devices_current_device)
                        }
                        if (deviceDataProvidersApi()) {
                            val currentDevicePackageName =
                                deviceDataSourcesInfo.findCurrentDeviceId()
                                    ?: Constants.DEVICE_DATA_PROVIDER_PACKAGE
                            if (appMetadata.packageName == currentDevicePackageName) {
                                preference.summary = getString(R.string.devices_this_phone)
                            }
                        }
                    }
                )
            }
    }
}
