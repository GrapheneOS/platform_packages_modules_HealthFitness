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
package com.android.healthconnect.testapps.toolbox

import android.health.connect.DeviceDataSourceInfo
import android.health.connect.HealthConnectException
import android.health.connect.HealthConnectManager
import android.os.Bundle
import android.os.OutcomeReceiver
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.android.healthconnect.testapps.toolbox.ui.DdpOnboardingFragment
import com.android.healthconnect.testapps.toolbox.viewmodels.AdvertiseDevicesViewModel
import com.android.healthconnect.testapps.toolbox.viewmodels.AdvertiseDevicesViewModel.DataTypeConfig
import java.util.concurrent.Executors

class DeviceOnboardingActivity : AppCompatActivity() {

    companion object {
        const val TOOLBOX_APP_NAME = "com.android.healthconnect.testapps.toolboxcombined"
    }

    private val healthConnectManager by lazy {
        this.getSystemService(HealthConnectManager::class.java)!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_onboarding)

        val deviceId = intent.getStringExtra(HealthConnectManager.EXTRA_DEVICE_ID)

        if (deviceId == null) {
            finish()
            return
        }

        healthConnectManager.getDeviceDataSourceInfos(
            Executors.newSingleThreadExecutor(),
            object : OutcomeReceiver<List<DeviceDataSourceInfo>, HealthConnectException> {
                override fun onResult(result: List<DeviceDataSourceInfo>) {
                    val info = findToolboxSourceForDeviceId(result, deviceId)

                    if (info == null) {
                        runOnUiThread {
                            Toast.makeText(
                                    this@DeviceOnboardingActivity,
                                    "No advertisement found for $deviceId by toolbox!",
                                    Toast.LENGTH_SHORT,
                                )
                                .show()

                            finish()
                        }
                    } else {
                        runOnUiThread { setupFragment(info, deviceId) }
                    }
                }

                override fun onError(error: HealthConnectException) {
                    error.printStackTrace()
                    runOnUiThread { finish() }
                }
            },
        )
    }

    private fun findToolboxSourceForDeviceId(
        sources: List<DeviceDataSourceInfo>,
        deviceId: String,
    ): DeviceDataSourceInfo? {
        return sources.find { source ->
            source.deviceDataProviderInfos.find { providerInfo ->
                providerInfo.packageName.equals(TOOLBOX_APP_NAME) &&
                    providerInfo.deviceId.equals(deviceId)
            } != null
        }
    }

    private fun setupFragment(info: DeviceDataSourceInfo, deviceId: String) {
        val viewModel = ViewModelProvider(this)[AdvertiseDevicesViewModel::class.java]
        viewModel.setSelectedDeviceDataSourceInfo(info)

        val toolboxProviderInfo =
            info.deviceDataProviderInfos.find { it.packageName == TOOLBOX_APP_NAME }!!

        val advertisedDataTypes = mutableListOf<DataTypeConfig>()

        val extraRequestedTypes =
            intent.getStringArrayListExtra(HealthConnectManager.EXTRA_DEVICE_RECORD_TYPES)

        toolboxProviderInfo.deviceDataTypeAdvertisements
            // TODO(b/474282806): Extend filtering with symptom types once available as extra
            .filter { ad -> extraRequestedTypes == null || ad.dataType.name in extraRequestedTypes }
            .forEach { ad ->
                advertisedDataTypes.add(
                    DataTypeConfig(
                        advertisedDataType = ad.dataType,
                        isAvailable = ad.isAvailable,
                        isUserEnabled = ad.isUserEnabled,
                        isVisibleByDefaultInMatchmaking = ad.isVisibleByDefaultInMatchmaking,
                        symptomType = ad.symptomType,
                    )
                )
            }

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, DdpOnboardingFragment())
            .commit()
    }
}
