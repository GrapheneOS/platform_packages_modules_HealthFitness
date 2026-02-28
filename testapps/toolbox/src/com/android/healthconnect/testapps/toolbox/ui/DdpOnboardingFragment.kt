/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.android.healthconnect.testapps.toolboxcombined
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.healthconnect.testapps.toolbox.ui

import android.app.Activity
import android.health.connect.DeviceDataSourceInfo
import android.health.connect.HealthConnectException
import android.health.connect.HealthConnectManager
import android.health.connect.device.DeviceDataAdvertisement
import android.health.connect.device.DeviceDataTypeAdvertisement
import android.os.Bundle
import android.os.OutcomeReceiver
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.android.healthconnect.testapps.toolbox.DeviceOnboardingActivity.Companion.TOOLBOX_APP_NAME
import com.android.healthconnect.testapps.toolbox.R
import com.android.healthconnect.testapps.toolbox.viewmodels.AdvertiseDevicesViewModel
import java.util.concurrent.Executors

class DdpOnboardingFragment : Fragment() {

    companion object {
        // Defined locally as these SystemAPIs are not yet visible in stubs for this target.
        private const val RESULT_DEVICE_ONBOARDING_ALLOWED = Activity.RESULT_FIRST_USER
        private const val RESULT_DEVICE_ONBOARDING_DENIED = Activity.RESULT_FIRST_USER + 1
        private const val RESULT_DEVICE_ONBOARDING_ABORTED = Activity.RESULT_FIRST_USER + 2

        private const val EXTRA_CALLING_PACKAGE_NAME =
            "android.health.connect.extra.CALLING_PACKAGE_NAME"
        private const val EXTRA_DEVICE_RECORD_TYPES =
            "android.health.connect.extra.DEVICE_RECORD_TYPES"
    }

    private val viewModel: AdvertiseDevicesViewModel by activityViewModels()
    private val typeToggles = mutableMapOf<String, SwitchCompat>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_ddp_onboarding, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val deviceNameView = view.findViewById<TextView>(R.id.device_name)
        val deviceInfoView = view.findViewById<TextView>(R.id.device_info)
        val dataTypesContainer = view.findViewById<LinearLayout>(R.id.data_types_container)
        val confirmButton = view.findViewById<Button>(R.id.confirm_and_enable_button)
        val denyButton = view.findViewById<Button>(R.id.deny_button)
        val abortButton = view.findViewById<Button>(R.id.abort_button)

        val healthConnectManager =
            requireContext().getSystemService(HealthConnectManager::class.java)!!

        val intent = activity?.intent
        val currentDeviceId = intent?.getStringExtra(HealthConnectManager.EXTRA_DEVICE_ID)
        val recordTypeNames =
            intent?.getStringArrayExtra(EXTRA_DEVICE_RECORD_TYPES)
                ?: intent?.getStringArrayListExtra(EXTRA_DEVICE_RECORD_TYPES)?.toTypedArray()

        val info = viewModel.selectedDeviceDataSourceInfo.value
        if (info != null) {
            deviceNameView.text = info.device.displayName ?: info.device.model ?: currentDeviceId
            deviceInfoView.text = "${info.device.manufacturer} • ${info.device.model}"

            dataTypesContainer.removeAllViews()
            typeToggles.clear()

            val toolboxProvider =
                info.deviceDataProviderInfos.find { it.packageName == TOOLBOX_APP_NAME }

            toolboxProvider?.deviceDataTypeAdvertisements?.forEach { ad ->
                // Only show toggles for record types that were matched in the session
                val isMatched = recordTypeNames == null || ad.dataType.name in recordTypeNames
                if (!isMatched) {
                    return@forEach
                }

                val itemPadding = (8 * resources.displayMetrics.density).toInt()

                val row =
                    LinearLayout(requireContext()).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(0, itemPadding, 0, itemPadding)
                    }

                val label =
                    TextView(requireContext()).apply {
                        text = ad.dataType.simpleName
                        textSize = 18f
                        layoutParams =
                            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    }

                val toggle =
                    SwitchCompat(requireContext()).apply {
                        isChecked = true // Default to enabled for requested matched types
                    }

                typeToggles[ad.dataType.name] = toggle
                row.addView(label)
                row.addView(toggle)
                dataTypesContainer.addView(row)
            }
        } else {
            deviceNameView.text = currentDeviceId ?: "Generic Watch"
            deviceInfoView.text = "Unknown Manufacturer • Smartwatch"
        }

        denyButton.setOnClickListener {
            activity?.setResult(RESULT_DEVICE_ONBOARDING_DENIED)
            activity?.finish()
        }

        abortButton.setOnClickListener {
            activity?.setResult(RESULT_DEVICE_ONBOARDING_ABORTED)
            activity?.finish()
        }

        confirmButton.setOnClickListener {
            // 1. Get all current devices for the Toolbox from Health Connect
            healthConnectManager.getDeviceDataSourceInfos(
                Executors.newSingleThreadExecutor(),
                object : OutcomeReceiver<List<DeviceDataSourceInfo>, HealthConnectException> {
                    override fun onResult(result: List<DeviceDataSourceInfo>) {
                        val advertisements =
                            result.mapNotNull { info ->
                                val providerInfo =
                                    info.deviceDataProviderInfos.find {
                                        it.packageName == TOOLBOX_APP_NAME
                                    } ?: return@mapNotNull null
                                val isCurrentDevice = providerInfo.deviceId == currentDeviceId

                                val adSet =
                                    providerInfo.deviceDataTypeAdvertisements
                                        .map { ad ->
                                            val toggle = typeToggles[ad.dataType.name]

                                            // Final state is toggled state for the current device
                                            // and matched types,
                                            // or the current system state for everything else.
                                            val finalEnabledState =
                                                (if (isCurrentDevice) toggle?.isChecked else null)
                                                    ?: ad.isUserEnabled

                                            DeviceDataTypeAdvertisement.Builder(ad.dataType)
                                                .setAvailable(ad.isAvailable)
                                                .setUserEnabled(finalEnabledState)
                                                .setVisibleByDefaultInMatchmaking(
                                                    ad.isVisibleByDefaultInMatchmaking
                                                )
                                                .build()
                                        }
                                        .toSet()

                                DeviceDataAdvertisement(info.device, providerInfo.deviceId, adSet)
                            }

                        performAdvertisement(advertisements, healthConnectManager)
                    }

                    override fun onError(error: HealthConnectException) {
                        activity?.runOnUiThread {
                            activity?.setResult(RESULT_DEVICE_ONBOARDING_DENIED)
                            activity?.finish()
                        }
                    }
                },
            )
        }
    }

    private fun performAdvertisement(
        advertisements: List<DeviceDataAdvertisement>,
        healthConnectManager: HealthConnectManager,
    ) {
        healthConnectManager.advertiseDeviceDataSources(
            advertisements.toSet(),
            Executors.newSingleThreadExecutor(),
            object : OutcomeReceiver<Void, HealthConnectException> {
                override fun onResult(result: Void?) {
                    activity?.runOnUiThread {
                        activity?.setResult(RESULT_DEVICE_ONBOARDING_ALLOWED)
                        activity?.finish()
                    }
                }

                override fun onError(error: HealthConnectException) {
                    activity?.runOnUiThread {
                        activity?.setResult(RESULT_DEVICE_ONBOARDING_DENIED)
                        activity?.finish()
                    }
                }
            },
        )
    }
}
