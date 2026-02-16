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
import android.health.connect.DeviceDataSourceInfo
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceGroup
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.permissions.connectedapps.HealthAppPreference
import com.android.healthconnect.controller.shared.preference.HealthPreferenceFragment
import com.android.healthconnect.controller.utils.asAppMetadata
import com.android.healthconnect.controller.utils.findSystemInfo
import com.android.healthconnect.controller.utils.isDisabledByAllProviders
import com.android.healthconnect.controller.utils.pref
import com.android.settingslib.widget.TopIntroPreference
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/** Fragment for an overview of all advertised devices. */
@AndroidEntryPoint(HealthPreferenceFragment::class)
class DevicesFragment : Hilt_DevicesFragment() {

    companion object {
        private const val ENABLED_CATEGORY = "enabled_devices_category"
        private const val NOT_ENABLED_CATEGORY = "not_enabled_devices_category"
        private const val INTRO_KEY = "devices_intro"
    }

    private val deviceSourcesViewModel: DeviceSourcesViewModel by viewModels()

    private val enabledDevicesCategory: PreferenceGroup by pref(ENABLED_CATEGORY)
    private val notEnabledDevicesCategory: PreferenceGroup by pref(NOT_ENABLED_CATEGORY)
    private val introPreference: TopIntroPreference by pref(INTRO_KEY)

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.new_devices_screen, rootKey)
    }

    override fun onResume() {
        super.onResume()
        deviceSourcesViewModel.loadDeviceSourcesInfos()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // TODO(b/477850701): Hide all elements to avoid "flashes" before loading
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                deviceSourcesViewModel.deviceSourcesState.collect { state ->
                    when (state) {
                        is DeviceSourcesViewModel.DeviceSourcesState.Loading -> {
                            setLoading(true)
                        }
                        is DeviceSourcesViewModel.DeviceSourcesState.Error -> {
                            setError(true)
                        }
                        is DeviceSourcesViewModel.DeviceSourcesState.WithData -> {
                            setLoading(false)
                            updateScreen(state)
                        }
                    }
                }
            }
        }
    }

    private fun updateScreen(state: DeviceSourcesViewModel.DeviceSourcesState.WithData) {
        setPreferenceTitles()

        enabledDevicesCategory.removeAll()
        notEnabledDevicesCategory.removeAll()

        state.deviceSourcesInfos.forEach { deviceSourceInfo ->
            val devicePreference =
                HealthAppPreference(
                        requireContext(),
                        deviceSourceInfo.asAppMetadata(requireContext()),
                    )
                    .apply {
                        if (deviceSourceInfo.isCurrentDevice) {
                            summary = getString(R.string.devices_this_phone)
                        }

                        if (
                            deviceSourceInfo.isCurrentDevice &&
                                deviceSourceInfo.deviceDataProviderInfos.size == 1 &&
                                deviceSourceInfo.findSystemInfo() != null
                        ) {
                            setCurrentDeviceButton(deviceSourceInfo)
                        } else {
                            setDeviceDataProviderButton(deviceSourceInfo)
                        }
                    }

            if (deviceSourceInfo.isDisabledByAllProviders()) {
                notEnabledDevicesCategory.addPreference(devicePreference)
            } else {
                enabledDevicesCategory.addPreference(devicePreference)
            }
        }

        enabledDevicesCategory.isVisible = enabledDevicesCategory.preferenceCount > 0
        notEnabledDevicesCategory.isVisible = notEnabledDevicesCategory.preferenceCount > 0
    }

    private fun setPreferenceTitles() {
        introPreference.title = getString(R.string.devices_screen_description)
        enabledDevicesCategory.title = getString(R.string.devices_screen_enabled_devices_header)
        notEnabledDevicesCategory.title =
            getString(R.string.devices_screen_not_enabled_devices_header)
    }

    private fun HealthAppPreference.setCurrentDeviceButton(deviceSourceInfo: DeviceDataSourceInfo) {
        setOnPreferenceClickListener {
            findNavController()
                .navigate(
                    R.id.action_newDevicesFragment_to_currentDeviceManagementFragment,
                    Bundle().apply {
                        putString(EXTRA_PACKAGE_NAME, deviceSourceInfo.deviceDataOrigin.packageName)
                    },
                )
            true
        }
    }

    private fun HealthAppPreference.setDeviceDataProviderButton(
        deviceSourceInfo: DeviceDataSourceInfo
    ) {
        setOnPreferenceClickListener {
            findNavController()
                .navigate(
                    R.id.action_newDevicesFragment_to_deviceDataProviderFragment,
                    Bundle().apply {
                        putString(EXTRA_PACKAGE_NAME, deviceSourceInfo.deviceDataOrigin.packageName)
                    },
                )
            true
        }
    }
}
