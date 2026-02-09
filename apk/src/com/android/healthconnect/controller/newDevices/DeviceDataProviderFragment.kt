/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.healthconnect.controller.newDevices

import android.content.Intent.EXTRA_PACKAGE_NAME
import android.health.connect.DeviceDataProviderInfo
import android.health.connect.DeviceDataSourceInfo
import android.health.connect.datatypes.SymptomRecord
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceCategory
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.data.appdata.AppDataFragment.Companion.EXTRA_DATA_LABEL
import com.android.healthconnect.controller.shared.Constants.DEVICE_DATA_PROVIDER_PACKAGE
import com.android.healthconnect.controller.shared.Constants.EXTRA_APP_NAME
import com.android.healthconnect.controller.shared.HealthDataCategoryExtensions.icon
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.shared.preference.HealthPreference
import com.android.healthconnect.controller.shared.preference.HealthPreferenceFragment
import com.android.healthconnect.controller.shared.preference.addIntroOrPermissionHeaderPreference
import com.android.healthconnect.controller.utils.asAppMetadata
import com.android.healthconnect.controller.utils.enabledAdsCount
import com.android.healthconnect.controller.utils.pref
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.collections.forEach
import kotlin.getValue
import kotlinx.coroutines.launch

@AndroidEntryPoint(HealthPreferenceFragment::class)
class DeviceDataProviderFragment : Hilt_DeviceDataProviderFragment() {
    companion object {
        private const val TAG = "DeviceDataProviderFragment"
        private const val HEADER_KEY = "device_header_category"
        private const val MANAGE_DEVICE_CATEGORY = "manage_device_category"
    }

    @Inject lateinit var healthPermissionReader: HealthPermissionReader

    private val deviceSourcesViewModel: DeviceSourcesViewModel by viewModels()

    private val headerGroup: PreferenceCategory by pref(HEADER_KEY)
    private val manageDeviceCategory: PreferenceCategory by pref(MANAGE_DEVICE_CATEGORY)

    private var packageName: String = ""

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.device_data_provider_screen, rootKey)

        if (
            requireArguments().containsKey(EXTRA_PACKAGE_NAME) &&
                requireArguments().getString(EXTRA_PACKAGE_NAME) != null
        ) {
            packageName = requireArguments().getString(EXTRA_PACKAGE_NAME)!!
        }
    }

    override fun onResume() {
        super.onResume()
        deviceSourcesViewModel.loadSelectedDeviceSourceInfo(packageName)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // TODO(b/477850701): Hide all elements to avoid "flashes" before loading
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                deviceSourcesViewModel.selectedDeviceSourceInfoState.collect { state ->
                    when (state) {
                        is DeviceSourcesViewModel.SelectedDeviceSourceInfoState.Loading -> {
                            setLoading(true)
                        }
                        is DeviceSourcesViewModel.SelectedDeviceSourceInfoState.Error -> {
                            setError(true)
                        }
                        is DeviceSourcesViewModel.SelectedDeviceSourceInfoState.WithData -> {
                            setLoading(false)
                            updateScreen(state.selectedDeviceSourceInfo)
                        }
                    }
                }
            }
        }
    }

    private fun updateScreen(selectedDeviceInfo: DeviceDataSourceInfo) {
        headerGroup.removeAll()
        manageDeviceCategory.removeAll()

        val deviceAppMetadata = selectedDeviceInfo.asAppMetadata(requireContext())

        updateHeader(deviceAppMetadata)

        manageDeviceCategory.title = getString(R.string.device_data_label)

        updateManageDeviceCategory(selectedDeviceInfo.deviceDataProviderInfos, deviceAppMetadata)

        updateDeviceDataButton(deviceAppMetadata)
    }

    private fun updateHeader(deviceAppMetadata: AppMetadata) =
        addIntroOrPermissionHeaderPreference(
            preferenceScreen,
            requireContext(),
            deviceAppMetadata.appName,
            deviceAppMetadata.icon,
            "",
        )

    private fun updateManageDeviceCategory(
        deviceDataProviderInfos: List<DeviceDataProviderInfo>,
        deviceMetadata: AppMetadata,
    ) {

        val hasSingleProvider = deviceDataProviderInfos.size == 1
        deviceDataProviderInfos.forEach { provider ->
            val settingsButton =
                HealthPreference(requireContext()).apply {
                    title =
                        // Managing the current device is not an extra activity, so the label
                        // in the system advertisement is empty
                        if (
                            hasSingleProvider ||
                                provider.packageName == DEVICE_DATA_PROVIDER_PACKAGE
                        ) {
                            getString(R.string.manage_device_settings, deviceMetadata.appName)
                        } else {
                            provider.managementActivityLabel
                        }

                    summary =
                        getString(
                            R.string.device_data_provider_enabled_types,
                            provider.enabledAdsCount(),
                            provider.deviceDataTypeAdvertisements.size,
                        )

                    setDeviceProviderOnClickListener(provider)
                }

            manageDeviceCategory.addPreference(settingsButton)
        }
    }

    private fun updateDeviceDataButton(deviceAppMetadata: AppMetadata) {
        val deviceDataButton =
            HealthPreference(requireContext()).apply {
                title = getString(R.string.device_data_button)
                setOnPreferenceClickListener {
                    // TODO(b/477202157): Include legacy "android" app data when browsing current
                    // device
                    findNavController()
                        .navigate(
                            R.id.action_deviceDataProviderFragment_to_appData,
                            Bundle().apply {
                                putString(EXTRA_PACKAGE_NAME, deviceAppMetadata.packageName)
                                putString(EXTRA_APP_NAME, deviceAppMetadata.appName)
                                putInt(EXTRA_DATA_LABEL, R.string.device_data_screen_title)
                            },
                        )
                    true
                }
            }

        manageDeviceCategory.addPreference(deviceDataButton)
    }

    private fun HealthPreference.setDeviceProviderOnClickListener(
        deviceProvider: DeviceDataProviderInfo
    ) {
        setOnPreferenceClickListener {
            // When the provider is Health Connect itself (i.e. native trackers), we don't launch an
            // intent to an external activity and instead simply navigate to the appropriate
            // internal management screen.
            if (deviceProvider.packageName == DEVICE_DATA_PROVIDER_PACKAGE) {
                findNavController()
                    .navigate(
                        R.id.action_deviceDataProviderFragment_to_currentDeviceManagementFragment,
                        Bundle().apply { putString(EXTRA_PACKAGE_NAME, packageName) },
                    )
            } else {
                try {
                    val intent =
                        healthPermissionReader.getDeviceManagementActivityIntent(
                            requireContext(),
                            deviceProvider.packageName,
                            deviceProvider.deviceId,
                            ArrayList(
                                deviceProvider.deviceDataTypeAdvertisements.map { it.dataType }
                            ),
                            ArrayList(
                                deviceProvider.deviceDataTypeAdvertisements
                                    .filter {
                                        SymptomRecord::class.java.isAssignableFrom(it.dataType)
                                    }
                                    .map { it.symptomType }
                            ),
                        )
                    activity?.startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Error launching DDP activity ", e)
                }
            }
            true
        }
    }
}
