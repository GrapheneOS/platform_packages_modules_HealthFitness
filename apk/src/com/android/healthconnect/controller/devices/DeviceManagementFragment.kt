/*
 * Copyright (C) 2024 The Android Open Source Project
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

import android.content.Intent.EXTRA_PACKAGE_NAME
import android.health.connect.HealthDataCategory
import android.health.connect.datatypes.StepsRecord
import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.data.appdata.AppDataFragment.Companion.EXTRA_DATA_LABEL
import com.android.healthconnect.controller.shared.Constants.DEVICE_DATA_PROVIDER_PACKAGE
import com.android.healthconnect.controller.shared.Constants.EXTRA_APP_NAME
import com.android.healthconnect.controller.shared.HealthDataCategoryExtensions.icon
import com.android.healthconnect.controller.shared.preference.HealthPreference
import com.android.healthconnect.controller.shared.preference.HealthPreferenceFragment
import com.android.healthconnect.controller.shared.preference.HealthSwitchPreference
import com.android.healthconnect.controller.shared.preference.WarningPreference
import com.android.healthconnect.controller.shared.preference.addIntroOrPermissionHeaderPreference
import com.android.healthconnect.controller.utils.AttributeResolver
import com.android.healthconnect.controller.utils.pref
import com.android.settingslib.widget.FooterPreference
import com.android.settingslib.widget.SettingsThemeHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlin.getValue

/** Fragment for managing a specific device. */
@AndroidEntryPoint(HealthPreferenceFragment::class)
class DeviceManagementFragment : Hilt_DeviceManagementFragment() {
    companion object {
        private const val HEADER_KEY = "device_header_category"
        private const val FOOTER_KEY = "connected_app_footer"
        private const val DEVICE_DATA_BUTTON = "device_data_button"
    }

    private val viewModel: ConnectedDevicesViewModel by activityViewModels()
    private val headerGroup: PreferenceCategory by pref(HEADER_KEY)
    private val connectedAppFooter: FooterPreference by pref(FOOTER_KEY)
    private val deviceDataButton: HealthPreference by pref(DEVICE_DATA_BUTTON)

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.device_management_screen, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // TODO(b/429618933): add logging
        val stepTrackingSwitch: HealthSwitchPreference? = findPreference("step_tracking_switch")
        stepTrackingSwitch?.icon = HealthDataCategory.ACTIVITY.icon(requireContext())

        viewModel.selectedDevice.observe(viewLifecycleOwner) { device ->
            addIntroOrPermissionHeaderPreference(
                preferenceScreen,
                requireContext(),
                device.deviceName,
                AttributeResolver.getDrawable(requireContext(), R.attr.devicePhoneIcon),
                "",
            )
            stepTrackingSwitch?.isChecked =
                device.trackerStatus.getOrDefault(StepsRecord::class.java, false)
            stepTrackingSwitch?.setOnPreferenceChangeListener { _, newValue ->
                viewModel.setTrackingEnabled(StepsRecord::class.java, newValue as Boolean)
                true
            }
            deviceDataButton.setOnPreferenceClickListener {
                findNavController()
                    .navigate(
                        R.id.action_deviceManagementFragment_to_appData,
                        Bundle().apply {
                            putString(EXTRA_PACKAGE_NAME, DEVICE_DATA_PROVIDER_PACKAGE)
                            putString(EXTRA_APP_NAME, device.deviceName)
                            putInt(EXTRA_DATA_LABEL, R.string.device_data_screen_title)
                        },
                    )
                true
            }
        }

        viewModel.hasStepsSensor.observe(viewLifecycleOwner) { hasStepsSensor ->
            if (!hasStepsSensor) {
                stepTrackingSwitch?.isEnabled = false
                setUpNoSensorHeader()
            }
            setUpFooter(hasStepsSensor)
        }

        setUpFooter(true)
        viewModel.loadHasStepsSensor()
    }

    private fun setUpFooter(hasStepsSensor: Boolean) {
        // TODO(b/436184641): Display different messages depending on device type and tracker state
        // Right now, the texts assume that tracking is enabled and have data stored
        val title =
            if (hasStepsSensor) getString(R.string.device_management_screen_footer)
            else getString(R.string.device_management_screen_no_step_sensor_footer)
        connectedAppFooter.title = title
    }

    private fun setUpNoSensorHeader() {
        headerGroup.removeAll()
        headerGroup.order = -1

        val isExpressiveThemeEnabled = SettingsThemeHelper.isExpressiveTheme(requireContext())
        val deviceHeaderPreference = getHeaderPreference(isExpressiveThemeEnabled)

        headerGroup.addPreference(deviceHeaderPreference)
    }

    private fun getHeaderPreference(isExpressiveThemeEnabled: Boolean): Preference {
        return if (isExpressiveThemeEnabled) {
            HealthPreference(requireContext()).also {
                it.title = getString(R.string.device_management_screen_no_step_sensor_header)
                it.icon = AttributeResolver.getDrawable(requireContext(), R.attr.infoIcon)
            }
        } else {
            WarningPreference(requireContext()).also {
                setDivider(null)
                it.setTitle(getString(R.string.device_management_screen_no_step_sensor_header))
                it.setGravity(android.view.Gravity.START or android.view.Gravity.CENTER_VERTICAL)
            }
        }
    }
}
