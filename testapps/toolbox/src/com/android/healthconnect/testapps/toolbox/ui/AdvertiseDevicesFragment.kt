/**
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * ```
 *    http://www.apache.org/licenses/LICENSE-2.0
 * ```
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.testapps.toolbox.ui

import android.health.connect.HealthConnectException
import android.health.connect.HealthConnectManager
import android.health.connect.datatypes.Device
import android.health.connect.datatypes.SymptomRecord
import android.health.connect.device.DeviceDataAdvertisement
import android.health.connect.device.DeviceDataTypeAdvertisement
import android.os.Bundle
import android.os.OutcomeReceiver
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.android.healthconnect.testapps.toolbox.Constants
import com.android.healthconnect.testapps.toolbox.R
import com.android.healthconnect.testapps.toolbox.viewmodels.AdvertiseDevicesViewModel
import com.android.healthconnect.testapps.toolbox.viewmodels.AdvertiseDevicesViewModel.DataTypeConfig
import com.android.healthconnect.testapps.toolbox.viewmodels.AdvertiseDevicesViewModel.DeviceAdvertisementConfig
import java.util.concurrent.Executors

class AdvertiseDevicesFragment : Fragment() {

    private val viewModel: AdvertiseDevicesViewModel by activityViewModels()
    private lateinit var devicesListContainer: LinearLayout

    private val manager by lazy {
        requireContext().getSystemService(HealthConnectManager::class.java)
    }

    private val symptomTypesMap =
        mapOf(
            "Unknown" to SymptomRecord.SYMPTOM_TYPE_UNKNOWN,
            "Abdominal Pain" to SymptomRecord.SYMPTOM_TYPE_ABDOMINAL_PAIN,
            "Acne" to SymptomRecord.SYMPTOM_TYPE_ACNE,
            "Back Pain" to SymptomRecord.SYMPTOM_TYPE_BACK_PAIN,
            "Bloating" to SymptomRecord.SYMPTOM_TYPE_BLOATING,
            "Cough" to SymptomRecord.SYMPTOM_TYPE_COUGH,
            "Diarrhea" to SymptomRecord.SYMPTOM_TYPE_DIARRHEA,
            "Fatigue" to SymptomRecord.SYMPTOM_TYPE_FATIGUE,
            "Fever" to SymptomRecord.SYMPTOM_TYPE_FEVER,
            "Headache" to SymptomRecord.SYMPTOM_TYPE_HEADACHE,
            "Insomnia" to SymptomRecord.SYMPTOM_TYPE_INSOMNIA,
            "Nausea" to SymptomRecord.SYMPTOM_TYPE_NAUSEA,
            "Sore Throat" to SymptomRecord.SYMPTOM_TYPE_SORE_THROAT,
            "Vomiting" to SymptomRecord.SYMPTOM_TYPE_VOMITING,
        )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_advertise_devices, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        devicesListContainer = view.requireViewById(R.id.devices_list)

        viewModel.deviceConfigs.observe(viewLifecycleOwner) { configs ->
            val needsUpdate =
                devicesListContainer.childCount != configs.size ||
                    configs.indices.any { i ->
                        val deviceView = devicesListContainer.getChildAt(i)
                        val dataTypesContainer =
                            deviceView?.findViewById<LinearLayout>(R.id.data_types_container)
                        if (dataTypesContainer?.childCount != configs[i].advertisedDataTypes.size) {
                            true
                        } else {
                            configs[i].advertisedDataTypes.indices.any { j ->
                                val dataTypeView = dataTypesContainer.getChildAt(j)
                                val autoComplete =
                                    dataTypeView?.findViewById<AutoCompleteTextView>(
                                        R.id.data_type_auto_complete
                                    )
                                autoComplete?.text?.toString() !=
                                    configs[i].advertisedDataTypes[j].advertisedDataType.simpleName
                            }
                        }
                    }
            if (needsUpdate) {
                updateDeviceList(configs)
            }
        }

        view.requireViewById<Button>(R.id.add_device_button).setOnClickListener {
            viewModel.addDevice()
        }

        view.requireViewById<Button>(R.id.advertise_button).setOnClickListener {
            advertiseDevices()
        }
    }

    private fun updateDeviceList(configs: List<DeviceAdvertisementConfig>) {
        // Simple update logic for now.
        // We might want to optimize this to avoid re-inflating everything if possible.
        devicesListContainer.removeAllViews()
        configs.forEachIndexed { index, config ->
            val itemView =
                LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_device_advertisement, devicesListContainer, false)
            setupDeviceItemView(itemView, index, config)
            devicesListContainer.addView(itemView)
        }
    }

    private fun setupDeviceItemView(view: View, index: Int, config: DeviceAdvertisementConfig) {
        val enabledCheckbox = view.requireViewById<CheckBox>(R.id.device_enabled_checkbox)
        val manufacturerEdit = view.requireViewById<EditText>(R.id.manufacturer_edit_text)
        val modelEdit = view.requireViewById<EditText>(R.id.model_edit_text)
        val displayNameEdit = view.requireViewById<EditText>(R.id.display_name_edit_text)
        val deviceIdEdit = view.requireViewById<EditText>(R.id.device_id_edit_text)
        val typeAutoComplete =
            view.requireViewById<AutoCompleteTextView>(R.id.device_type_auto_complete)
        val dataTypesContainer = view.requireViewById<LinearLayout>(R.id.data_types_container)
        val addDataTypeButton = view.requireViewById<Button>(R.id.add_data_type_button)
        val removeButton = view.requireViewById<Button>(R.id.remove_device_button)

        enabledCheckbox.isChecked = config.isEnabled
        manufacturerEdit.setText(config.manufacturer)
        modelEdit.setText(config.model)
        displayNameEdit.setText(config.displayName)
        deviceIdEdit.setText(config.deviceId)

        val deviceTypes =
            mapOf(
                "Watch" to Device.DEVICE_TYPE_WATCH,
                "Phone" to Device.DEVICE_TYPE_PHONE,
                "Scale" to Device.DEVICE_TYPE_SCALE,
                "Ring" to Device.DEVICE_TYPE_RING,
                "Chest Strap" to Device.DEVICE_TYPE_CHEST_STRAP,
                "Unknown" to Device.DEVICE_TYPE_UNKNOWN,
            )
        val typeAdapter =
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                deviceTypes.keys.toList(),
            )
        typeAutoComplete.setAdapter(typeAdapter)
        typeAutoComplete.setText(deviceTypes.entries.find { it.value == config.type }?.key, false)

        enabledCheckbox.setOnCheckedChangeListener { _, isChecked ->
            config.isEnabled = isChecked
            viewModel.updateDevice(index, config)
        }
        manufacturerEdit.doAfterTextChanged {
            config.manufacturer = it?.toString()
            viewModel.updateDevice(index, config)
        }
        modelEdit.doAfterTextChanged {
            config.model = it?.toString()
            viewModel.updateDevice(index, config)
        }
        displayNameEdit.doAfterTextChanged {
            config.displayName = it?.toString()
            viewModel.updateDevice(index, config)
        }
        deviceIdEdit.doAfterTextChanged {
            config.deviceId = it?.toString() ?: ""
            viewModel.updateDevice(index, config)
        }

        typeAutoComplete.setOnItemClickListener { _, _, position, _ ->
            val selectedType = typeAdapter.getItem(position)
            config.type = deviceTypes[selectedType] ?: Device.DEVICE_TYPE_UNKNOWN
            viewModel.updateDevice(index, config)
        }

        dataTypesContainer.removeAllViews()
        config.advertisedDataTypes.forEachIndexed { dataTypeIndex, dataTypeConfig ->
            val dataTypeView =
                LayoutInflater.from(requireContext())
                    .inflate(R.layout.item_data_type_advertisement, dataTypesContainer, false)
            setupDataTypeItemView(dataTypeView, index, dataTypeIndex, dataTypeConfig)
            dataTypesContainer.addView(dataTypeView)
        }

        addDataTypeButton.setOnClickListener { viewModel.addDataType(index) }

        removeButton.setOnClickListener { viewModel.removeDevice(index) }
    }

    private fun setupDataTypeItemView(
        view: View,
        deviceIndex: Int,
        dataTypeIndex: Int,
        config: DataTypeConfig,
    ) {
        val dataTypeAutoComplete =
            view.requireViewById<AutoCompleteTextView>(R.id.data_type_auto_complete)
        val isAvailableCheckbox = view.requireViewById<CheckBox>(R.id.is_available_checkbox)
        val isUserEnabledCheckbox = view.requireViewById<CheckBox>(R.id.is_user_enabled_checkbox)
        val isVisibleInMatchmakingCheckbox =
            view.requireViewById<CheckBox>(R.id.is_visible_in_matchmaking_checkbox)
        val symptomTypeLayout = view.requireViewById<View>(R.id.symptom_type_layout)
        val symptomTypeAutoComplete =
            view.requireViewById<AutoCompleteTextView>(R.id.symptom_type_auto_complete)
        val removeButton = view.requireViewById<Button>(R.id.remove_data_type_button)

        val recordClasses =
            Constants.HealthPermissionType.entries.mapNotNull { it.recordClass?.java }
        val dataTypes = recordClasses.associateBy { it.simpleName }
        val dataAdapter =
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                dataTypes.keys.toList(),
            )
        dataTypeAutoComplete.setAdapter(dataAdapter)
        dataTypeAutoComplete.setText(config.advertisedDataType.simpleName, false)

        isAvailableCheckbox.isChecked = config.isAvailable
        isUserEnabledCheckbox.isChecked = config.isUserEnabled
        isVisibleInMatchmakingCheckbox.isChecked = config.isVisibleByDefaultInMatchmaking

        val isSymptom = SymptomRecord::class.java.isAssignableFrom(config.advertisedDataType)
        symptomTypeLayout.isVisible = isSymptom
        if (isSymptom) {
            val symptomTypeAdapter =
                ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    symptomTypesMap.keys.toList(),
                )
            symptomTypeAutoComplete.setAdapter(symptomTypeAdapter)
            symptomTypeAutoComplete.setText(
                symptomTypesMap.entries.find { it.value == config.symptomType }?.key,
                false,
            )
            symptomTypeAutoComplete.setOnItemClickListener { _, _, position, _ ->
                val selectedSymptom = symptomTypeAdapter.getItem(position)
                config.symptomType =
                    symptomTypesMap[selectedSymptom] ?: SymptomRecord.SYMPTOM_TYPE_UNKNOWN
                viewModel.updateDevice(deviceIndex, viewModel.deviceConfigs.value!![deviceIndex])
            }
        }

        dataTypeAutoComplete.setOnItemClickListener { _, _, position, _ ->
            val selectedName = dataAdapter.getItem(position)
            config.advertisedDataType = dataTypes[selectedName] ?: SymptomRecord::class.java
            viewModel.updateDevice(deviceIndex, viewModel.deviceConfigs.value!![deviceIndex])
        }

        isAvailableCheckbox.setOnCheckedChangeListener { _, isChecked ->
            config.isAvailable = isChecked
            viewModel.updateDevice(deviceIndex, viewModel.deviceConfigs.value!![deviceIndex])
        }

        isUserEnabledCheckbox.setOnCheckedChangeListener { _, isChecked ->
            config.isUserEnabled = isChecked
            viewModel.updateDevice(deviceIndex, viewModel.deviceConfigs.value!![deviceIndex])
        }

        isVisibleInMatchmakingCheckbox.setOnCheckedChangeListener { _, isChecked ->
            config.isVisibleByDefaultInMatchmaking = isChecked
            viewModel.updateDevice(deviceIndex, viewModel.deviceConfigs.value!![deviceIndex])
        }

        removeButton.setOnClickListener { viewModel.removeDataType(deviceIndex, dataTypeIndex) }
    }

    private fun advertiseDevices() {
        val configs = viewModel.deviceConfigs.value ?: return
        val advertisements =
            configs
                .filter { it.isEnabled }
                .map { config ->
                    val device =
                        Device.Builder()
                            .setManufacturer(config.manufacturer)
                            .setModel(config.model)
                            .setType(config.type)
                            .setDisplayName(config.displayName)
                            .build()
                    val deviceDataTypeAdvertisements =
                        config.advertisedDataTypes.map { dataTypeConfig ->
                            val builder =
                                DeviceDataTypeAdvertisement.Builder(
                                        dataTypeConfig.advertisedDataType
                                    )
                                    .setAvailable(dataTypeConfig.isAvailable)
                                    .setUserEnabled(dataTypeConfig.isUserEnabled)
                                    .setVisibleByDefaultInMatchmaking(
                                        dataTypeConfig.isVisibleByDefaultInMatchmaking
                                    )
                            if (
                                SymptomRecord::class
                                    .java
                                    .isAssignableFrom(dataTypeConfig.advertisedDataType)
                            ) {
                                builder.setSymptomType(dataTypeConfig.symptomType)
                            }
                            builder.build()
                        }
                    DeviceDataAdvertisement(
                        device,
                        config.deviceId,
                        deviceDataTypeAdvertisements.toSet(),
                    )
                }

        if (advertisements.isEmpty()) {
            Toast.makeText(context, "No devices enabled to advertise", Toast.LENGTH_SHORT).show()
            return
        }

        val executor = Executors.newSingleThreadExecutor()
        val receiver =
            object : OutcomeReceiver<Void, HealthConnectException> {
                override fun onResult(result: Void?) {
                    activity?.runOnUiThread {
                        Toast.makeText(context, "Devices Advertised!", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onError(error: HealthConnectException) {
                    activity?.runOnUiThread {
                        Toast.makeText(
                                context,
                                "Advertisement Failed: ${error.message}",
                                Toast.LENGTH_SHORT,
                            )
                            .show()
                    }
                }
            }

        manager.advertiseDeviceDataSources(advertisements.toSet(), executor, receiver)
    }
}
