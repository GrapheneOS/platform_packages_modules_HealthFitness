/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.healthconnect.controller.devices

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceGroup
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.devices.ConnectedDevicesViewModel.ConnectedDevicesState
import com.android.healthconnect.controller.shared.preference.HealthPreference
import com.android.healthconnect.controller.shared.preference.HealthPreferenceFragment
import com.android.healthconnect.controller.utils.AttributeResolver
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.navigateSafe
import com.android.healthconnect.controller.utils.pref
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Fragment for connected devices screen. */
@AndroidEntryPoint(HealthPreferenceFragment::class)
class ConnectedDevicesFragment : Hilt_ConnectedDevicesFragment() {

    companion object {
        private const val CONNECTED_DEVICES_CATEGORY = "connected_devices_category"
    }

    @Inject lateinit var logger: HealthConnectLogger

    private val viewModel: ConnectedDevicesViewModel by activityViewModels()
    private val devicesCategory: PreferenceGroup by pref(CONNECTED_DEVICES_CATEGORY)

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.connected_devices_screen, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // TODO(b/429618933): add logging
        viewModel.connectedDevicesState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ConnectedDevicesState.Loading -> {
                    setLoading(true)
                }
                is ConnectedDevicesState.Error -> {
                    setError(true)
                }
                is ConnectedDevicesState.Success -> {
                    setLoading(false)
                    devicesCategory.removeAll()
                    for (device in state.deviceDataSources) {
                        devicesCategory.addPreference(
                            HealthPreference(requireContext()).apply {
                                icon =
                                    AttributeResolver.getDrawable(
                                        requireContext(),
                                        R.attr.devicePhoneIcon,
                                    )
                                title = device.deviceName
                                if (device.isCurrentDevice) {
                                    summary = getString(R.string.devices_current_device)
                                }
                                setOnPreferenceClickListener {
                                    viewModel.setSelectedDevice(device)
                                    findNavController()
                                        .navigateSafe(
                                            R.id.connectedDevicesFragment,
                                            R.id
                                                .action_connectedDevicesFragment_to_deviceManagementFragment,
                                        )
                                    true
                                }
                            }
                        )
                    }
                }
            }
        }
        viewModel.loadDeviceDataSources()
    }
}
