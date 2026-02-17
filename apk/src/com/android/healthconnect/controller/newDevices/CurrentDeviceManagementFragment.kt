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
import android.health.connect.datatypes.Record
import android.health.connect.device.DeviceDataTypeAdvertisement
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.data.appdata.AppDataFragment.Companion.EXTRA_DATA_LABEL
import com.android.healthconnect.controller.permissions.data.FitnessPermissionStrings
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.shared.Constants.EXTRA_APP_NAME
import com.android.healthconnect.controller.shared.HealthDataCategoryExtensions.icon
import com.android.healthconnect.controller.shared.HealthPermissionToDatatypeMapper
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.shared.dataTypeToCategory
import com.android.healthconnect.controller.shared.preference.HealthPreference
import com.android.healthconnect.controller.shared.preference.HealthPreferenceFragment
import com.android.healthconnect.controller.shared.preference.HealthSwitchPreference
import com.android.healthconnect.controller.shared.preference.WarningPreference
import com.android.healthconnect.controller.shared.preference.addIntroOrPermissionHeaderPreference
import com.android.healthconnect.controller.utils.AttributeResolver
import com.android.healthconnect.controller.utils.ToastManager
import com.android.healthconnect.controller.utils.asAppMetadata
import com.android.healthconnect.controller.utils.findSystemInfo
import com.android.healthconnect.controller.utils.pref
import com.android.healthconnect.controller.utils.providesNativeSteps
import com.android.settingslib.widget.FooterPreference
import com.android.settingslib.widget.SettingsThemeHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.getValue
import kotlinx.coroutines.launch

/**
 * Fragment for managing the current device which is not handled by external Device Data Providers
 * but by Health Connect itself.
 */
@AndroidEntryPoint(HealthPreferenceFragment::class)
class CurrentDeviceManagementFragment : Hilt_CurrentDeviceManagementFragment() {
    companion object {
        private const val HEADER_KEY = "device_header_category"
        private const val DEVICE_WRITE_CATEGORY = "device_write_category"
        private const val DEVICE_DATA_CATEGORY = "device_data_category"
        private const val DEVICE_DATA_BUTTON = "device_data_button"
        private const val FOOTER_KEY = "connected_app_footer"
    }

    @Inject lateinit var toastManager: ToastManager

    private val deviceSourcesViewModel: DeviceSourcesViewModel by viewModels()

    private val headerGroup: PreferenceCategory by pref(HEADER_KEY)
    private val deviceWriteCategory: PreferenceCategory by pref(DEVICE_WRITE_CATEGORY)
    private val deviceDataCategory: PreferenceCategory by pref(DEVICE_DATA_CATEGORY)
    private val deviceDataButton: HealthPreference by pref(DEVICE_DATA_BUTTON)
    private val connectedAppFooter: FooterPreference by pref(FOOTER_KEY)

    private var packageName: String = ""

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.current_device_management_screen, rootKey)

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
                            if (state.selectedDeviceSourceInfo.findSystemInfo() == null) {
                                setError(true)
                            } else {
                                setLoading(false)
                                updateScreen(state.selectedDeviceSourceInfo)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateScreen(selectedDeviceInfo: DeviceDataSourceInfo) {
        setPreferenceTitles()

        headerGroup.removeAll()
        deviceWriteCategory.removeAll()

        val deviceAppMetadata = selectedDeviceInfo.asAppMetadata(requireContext())
        val hasStepsSensor = selectedDeviceInfo.providesNativeSteps()

        if (!hasStepsSensor) {
            setUpNoSensorHeader()
        }
        updateHeader(deviceAppMetadata)

        // TODO(b/476947851): Add sync to Health Connect button

        updateDeviceWriteCategory(
            selectedDeviceInfo.findSystemInfo()!!.deviceDataTypeAdvertisements
        )

        updateDeviceDataButton(deviceAppMetadata)

        updateFooter(hasStepsSensor)
    }

    private fun setPreferenceTitles() {
        deviceWriteCategory.title = getString(R.string.device_settings_label)
        deviceDataCategory.title = getString(R.string.device_data_label)
        deviceDataButton.title = getString(R.string.device_data_button)
    }

    private fun updateHeader(deviceAppMetadata: AppMetadata) =
        addIntroOrPermissionHeaderPreference(
            preferenceScreen,
            requireContext(),
            deviceAppMetadata.appName,
            deviceAppMetadata.icon,
            "",
        )

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

    private fun updateDeviceWriteCategory(typeAdvertisements: Set<DeviceDataTypeAdvertisement>) {
        typeAdvertisements.forEach {
            val typeTrackingSwitch =
                HealthSwitchPreference(requireContext()).apply {
                    isPersistent = false
                    key = it.dataType.name
                    title = getDisplayName(it.dataType)
                    icon = dataTypeToCategory(it.dataType).icon(requireContext())
                    isEnabled = it.isAvailable
                    isChecked = it.isUserEnabled

                    setOnPreferenceChangeListener { _, newValue ->
                        val isChecked = newValue as Boolean
                        viewLifecycleOwner.lifecycleScope.launch {
                            val wasSuccessful =
                                deviceSourcesViewModel.setNativeTrackingEnabled(
                                    it.dataType,
                                    newValue,
                                )
                            if (wasSuccessful) {
                                val activePref =
                                    deviceWriteCategory.findPreference<HealthSwitchPreference>(
                                        it.dataType.name
                                    )
                                (activePref as HealthSwitchPreference).isChecked = isChecked
                            } else {
                                toastManager.showToast(
                                    requireContext(),
                                    R.string.default_error,
                                    Toast.LENGTH_SHORT,
                                )
                            }
                        }
                        false
                    }
                }

            deviceWriteCategory.addPreference(typeTrackingSwitch)
        }
    }

    private fun getDisplayName(recordType: Class<out Record>): String? {
        val permissionType =
            HealthPermissionToDatatypeMapper.getPermissionType(recordType)
                ?: FitnessPermissionType.STEPS
        val strings = FitnessPermissionStrings.fromPermissionType(permissionType)
        return requireContext().getString(strings.uppercaseLabel)
    }

    private fun updateDeviceDataButton(deviceAppMetadata: AppMetadata) {
        deviceDataButton.setOnPreferenceClickListener {
            findNavController()
                .navigate(
                    R.id.action_currentDeviceManagementFragment_to_appData,
                    Bundle().apply {
                        putString(EXTRA_PACKAGE_NAME, deviceAppMetadata.packageName)
                        putString(EXTRA_APP_NAME, deviceAppMetadata.appName)
                        putInt(EXTRA_DATA_LABEL, R.string.device_data_screen_title)
                    },
                )
            true
        }
    }

    private fun updateFooter(enabledSteps: Boolean) {
        // TODO(b/436184641): Display different messages depending on device type and tracker state
        // Right now, the texts assume that tracking is enabled and have data stored
        val title =
            if (enabledSteps) getString(R.string.device_management_screen_footer)
            else getString(R.string.device_management_screen_no_step_sensor_footer)
        connectedAppFooter.title = title
    }
}
