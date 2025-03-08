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

package com.android.healthconnect.testapps.toolbox.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.health.connect.CreateMedicalDataSourceRequest
import android.health.connect.GetMedicalDataSourcesRequest
import android.health.connect.HealthConnectException
import android.health.connect.HealthConnectManager
import android.health.connect.ReadMedicalResourcesInitialRequest
import android.health.connect.ReadMedicalResourcesResponse
import android.health.connect.UpsertMedicalResourceRequest
import android.health.connect.datatypes.FhirVersion
import android.health.connect.datatypes.MedicalDataSource
import android.health.connect.datatypes.MedicalResource
import android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES
import android.net.Uri
import android.os.Bundle
import android.os.OutcomeReceiver
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.SpinnerAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.asOutcomeReceiver
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.android.healthconnect.testapps.toolbox.Constants.MEDICAL_PERMISSIONS
import com.android.healthconnect.testapps.toolbox.Constants.WRITE_ALL_MEDICAL_DATA
import com.android.healthconnect.testapps.toolbox.R
import com.android.healthconnect.testapps.toolbox.utils.GeneralUtils.Companion.requireSystemService
import com.android.healthconnect.testapps.toolbox.utils.GeneralUtils.Companion.showMessageDialog
import java.io.IOException
import java.io.InputStream
import kotlin.collections.flatten
import kotlin.math.max
import kotlin.text.get
import kotlin.toString
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject

class PhrOptionsFragment : Fragment(R.layout.fragment_phr_options) {
    companion object {
        private const val PATIENT_A_ASSET_PATH = "patient_a"
        private const val PATIENT_B_ASSET_PATH = "patient_b"
        private const val PATIENT_C_ASSET_PATH = "patient_c"
        private const val CUSTOM_DATA_PATH_PLACE_HOLDER = "custom folder"

        // Chosen to make sure we stay under the 5mb limit
        private const val MAX_BULK_INSERT_BATCH_SIZE = 750

        // HC default number of write calls allowed per 15 minutes
        private const val HC_RATE_LIMIT_15_M = 1000
        private const val MAX_BULK_INSERT_TOTAL_SIZE =
            MAX_BULK_INSERT_BATCH_SIZE * HC_RATE_LIMIT_15_M
    }

    private lateinit var mRequestPermissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var mSelectJSONDirectoryLauncher: ActivityResultLauncher<Intent>
    private val healthConnectManager: HealthConnectManager by lazy {
        requireContext().requireSystemService()
    }
    private lateinit var mSelectedCustomDirFilesToUri: Map<String, Uri>

    private var mExistingDataSourceNamesToId: MutableMap<String, String> = mutableMapOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Starting API Level 30 If permission is denied more than once, user doesn't see the dialog
        // asking permissions again unless they grant the permission from settings.
        mRequestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissionMap: Map<String, Boolean> ->
                requestPermissionResultHandler(permissionMap)
            }

        mSelectJSONDirectoryLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                selectJsonDirResultHandler(result)
            }

        // Fetch existing data sources from DB
        fetchDataSourcesAndPopulateSpinner()
    }

    private fun requestPermissionResultHandler(permissionMap: Map<String, Boolean>) {
        var numberOfPermissionsMissing = MEDICAL_PERMISSIONS.size
        for (value in permissionMap.values) {
            if (value) {
                numberOfPermissionsMissing--
            }
        }

        if (numberOfPermissionsMissing == 0) {
            Toast.makeText(
                this.requireContext(),
                R.string.all_medical_permissions_success,
                Toast.LENGTH_SHORT,
            )
                .show()
        } else {
            Toast.makeText(
                this.requireContext(),
                getString(
                    R.string.number_of_medical_permissions_not_granted,
                    numberOfPermissionsMissing,
                ),
                Toast.LENGTH_SHORT,
            )
                .show()
        }

        // Fetch data sources in case they could not be fetched before
        fetchDataSourcesAndPopulateSpinner()

    }

    private fun selectJsonDirResultHandler(result: ActivityResult) {
        val view = requireView()
        val uri = result.data?.data
        if (result.resultCode != Activity.RESULT_OK || uri == null) {
            Toast.makeText(
                this.requireContext(),
                "Failed to get directory access.",
                Toast.LENGTH_SHORT
            ).show()

            val spinner = view.findViewById<Spinner>(R.id.phr_patient_spinner)
            spinner.setSelection(0)
            return
        }

        val customPathText = view.findViewById<TextView>(R.id.phr_options_custom_path)
        customPathText.text = "Selected dir: ${uri.path}"
        customPathText.visibility = View.VISIBLE
        setUpFhirResourceFromSpinner(view, uri)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.requireViewById<Button>(R.id.phr_create_data_source_button).setOnClickListener {
            executeAndShowMessage {
                createMedicalDataSource(view)
            }
        }

        view.requireViewById<Button>(R.id.phr_read_by_id_button).setOnClickListener {
            executeAndShowMessage { readImmunization(view) }
        }

        view.requireViewById<Button>(R.id.phr_seed_fhir_jsons_button).setOnClickListener {
            executeAndShowMessage { insertAllFhirResources(view) }
        }

        view.requireViewById<Button>(R.id.phr_seed_x_fhir_jsons_button).setOnClickListener {
            executeAndShowMessage {
                insertSelectedNumberOfFhirResources(view)
            }
        }

        view.requireViewById<Button>(R.id.phr_insert_resource_button).setOnClickListener {
            executeAndShowMessage { insertPastedResource(view) }
        }

        view
            .requireViewById<Button>(R.id.phr_request_read_and_write_medical_data_button)
            .setOnClickListener { requestMedicalPermissions() }

        setUpDataSourceSpinner(view, mExistingDataSourceNamesToId.keys.toList())

        setUpPatientContextSpinner(view)
    }

    private fun executeAndShowMessage(block: suspend () -> String) {
        lifecycleScope.launch {
            val result =
                try {
                    block()
                } catch (e: Exception) {
                    e.toString()
                }

            requireContext().showMessageDialog(result)
        }
    }

    private suspend fun insertPastedResource(view: View): String {
        val pastedResource =
            view.findViewById<EditText>(R.id.phr_pasted_resource_text).getText().toString()
        Log.d("INSERT_RESOURCE", "Writing resource $pastedResource")
        return insertResource(view, pastedResource)
    }

    private fun fetchDataSourcesAndPopulateSpinner() {
        lifecycleScope.launch {
            mExistingDataSourceNamesToId = mutableMapOf()
            try {
                getMedicalDataSources().forEach {
                    mExistingDataSourceNamesToId.put(it.displayName, it.id)
                }
            } catch (e: HealthConnectException) {
                Log.d("GET_DATA_SOURCE", "Error getting data sources: $e")
                var errorMessage = "Failed to fetch data sources: "
                if (e.errorCode == HealthConnectException.ERROR_SECURITY) {
                    errorMessage += "Missing permissions"
                } else {
                    errorMessage += e.localizedMessage
                }
                Toast.makeText(
                    requireContext(), errorMessage, Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Log.d("GET_DATA_SOURCE", "Error getting data sources: $e")
                Toast.makeText(
                    requireContext(),
                    "Failed to fetch data sources: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }

            if (mExistingDataSourceNamesToId.isNotEmpty()) {
                view?.let { setUpDataSourceSpinner(it, mExistingDataSourceNamesToId.keys.toList()) }
            }
        }
    }

    private fun setUpDataSourceSpinner(
        rootView: View,
        dataSources: List<String> = listOf(),
        selectPosition: Int = 0
    ) {
        val spinnerOptions =
            listOf(getString(R.string.data_source_spinner_default_message)) + dataSources

        val spinner = rootView.findViewById<Spinner>(R.id.phr_data_source_spinner)
        spinner.adapter = createSpinnerAdapter(spinnerOptions)
        spinner.setSelection(selectPosition)
    }

    private fun updateDataSourceSpinnerOptions(
        view: View,
        existingDataSources: List<String>,
        newDataSource: String
    ) {
        setUpDataSourceSpinner(view, listOf(newDataSource) + existingDataSources, 1)
    }

    private fun getDataSourceIdFromSpinner(view: View): String? {
        val dataSourceSpinner = view.findViewById<Spinner>(R.id.phr_data_source_spinner)
        return mExistingDataSourceNamesToId[dataSourceSpinner.selectedItem.toString()]
    }

    private fun setUpPatientContextSpinner(rootView: View) {
        val spinnerOptions =
            listOf(
                PATIENT_A_ASSET_PATH,
                PATIENT_B_ASSET_PATH,
                PATIENT_C_ASSET_PATH,
                CUSTOM_DATA_PATH_PLACE_HOLDER
            )

        val spinner = rootView.findViewById<Spinner>(R.id.phr_patient_spinner)
        spinner.adapter = createSpinnerAdapter(spinnerOptions)

        spinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                    // Clear pasted resource text
                    rootView.findViewById<EditText>(R.id.phr_pasted_resource_text).text = null
                    // Clear selected custom path in case it was present
                    val customPathText =
                        rootView.findViewById<TextView>(R.id.phr_options_custom_path)
                    customPathText.text = null
                    customPathText.visibility = View.GONE

                    if (position == 3) {
                        // let user select directory to read from. The callback of this will then
                        // also set up the fhirResourceSpinner
                        mSelectJSONDirectoryLauncher.launch(
                            Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                        )
                        return
                    }

                    // Otherwise update resource spinner based on the selected patient
                    val selectedPatientContext = spinnerOptions[position]
                    setUpFhirResourceFromSpinner(rootView, selectedPatientContext)
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    // No-op.
                }
            }

    }

    private fun setUpFhirResourceFromSpinner(rootView: View, selectedCustomDirUri: Uri) {
        val selectedDirectory =
            DocumentFile.fromTreeUri(this.requireContext(), selectedCustomDirUri)
        if (selectedDirectory == null) {
            Toast.makeText(
                this.requireContext(),
                "Failed to access selected dir: $selectedCustomDirUri.",
                Toast.LENGTH_LONG
            )
                .show()
            return
        }

        mSelectedCustomDirFilesToUri = selectedDirectory.listFiles()
            .filter { file: DocumentFile -> file.isFile && file.name != null }
            .associate { file: DocumentFile -> Pair(file.name ?: "unknown", file.uri) }
            .filter { (filename, uri) -> uri.toString().endsWith(".json") }
        Log.d("SET_UP_SPINNER", "Found json files $mSelectedCustomDirFilesToUri")

        val spinnerOptions =
            listOf(getString(R.string.spinner_default_message)) +
                    (mSelectedCustomDirFilesToUri?.keys ?: emptyList())

        setUpFhirResourceSpinner(rootView, spinnerOptions, true)
    }

    private fun setUpFhirResourceFromSpinner(rootView: View, assetSubDir: String) {
        val jsonFiles = listFhirJSONAssetFiles(requireContext(), assetSubDir)
        val spinnerOptions = listOf(getString(R.string.spinner_default_message)) + jsonFiles

        setUpFhirResourceSpinner(rootView, spinnerOptions, false, assetSubDir)
    }

    private fun setUpFhirResourceSpinner(
        rootView: View,
        spinnerOptions: List<String>,
        customPatientContext: Boolean,
        assetSubDir: String = ""
    ) {
        val spinner = rootView.findViewById<Spinner>(R.id.phr_spinner)
        spinner.adapter = createSpinnerAdapter(spinnerOptions)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long,
            ) {
                if (position == 0) { // Ignore "Select resource" default message
                    return
                }

                var selectedResource = ""
                val selectedFile = spinnerOptions[position]

                if (customPatientContext) {
                    val resourceUri = mSelectedCustomDirFilesToUri?.get(selectedFile) ?: return
                    selectedResource =
                        loadJSONFromContentUri(requireContext(), resourceUri) ?: return
                } else {
                    selectedResource =
                        loadJSONFromAsset(requireContext(), assetSubDir, selectedFile) ?: return
                }

                rootView.findViewById<EditText>(R.id.phr_pasted_resource_text).setText(
                    selectedResource
                )
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // No-op.
            }
        }
    }

    private fun createSpinnerAdapter(spinnerOptions: List<String>): ArrayAdapter<String?> {
        val spinnerAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, spinnerOptions)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        return spinnerAdapter
    }

    private suspend fun insertResource(view: View, resource: String): String {
        val dataSourceId = getDataSourceIdFromSpinner(view) ?: return "No data source selected"
        val insertedResources =
            upsertMedicalResources(
                listOf(
                    UpsertMedicalResourceRequest.Builder(
                        dataSourceId,
                        FhirVersion.parseFhirVersion("4.0.1"),
                        resource,
                    )
                        .build()
                )
            )
        return insertedResources.joinToString(
            separator = "\n",
            transform = MedicalResource::toString,
        )
    }

    private suspend fun insertAllFhirResources(view: View): String {
        val allResources = loadAllFhirJSONs(view)
        Log.d("INSERT_ALL", "Writing ${allResources.size} FHIR resources")
        val insertedDataSourceId = getDataSourceIdFromSpinner(view)
            ?: return "No data source selected"
        val insertedResources =
            upsertMedicalResources(
                allResources.map {
                    UpsertMedicalResourceRequest.Builder(
                        insertedDataSourceId,
                        FhirVersion.parseFhirVersion("4.0.1"),
                        it,
                    )
                        .build()
                }
            )
        return "SUCCESSFUL DATA UPSERT \n\nUpserted data:\n" +
                insertedResources.joinToString(
                    separator = "\n",
                    transform = MedicalResource::toString
                )
    }

    private suspend fun insertSelectedNumberOfFhirResources(view: View): String {
        val insertedDataSourceId = getDataSourceIdFromSpinner(view)
            ?: return "No data source selected"
        val numResourcesToInsert =
            view.findViewById<EditText>(R.id.phr_number_of_inserts_id_text)
                .getText()
                .toString()
                .toIntOrNull() ?: return "Please enter a number"
        if (numResourcesToInsert > MAX_BULK_INSERT_TOTAL_SIZE)
            return "Please enter a number under $MAX_BULK_INSERT_TOTAL_SIZE"
        val numberOfBatches = Math.ceilDiv(numResourcesToInsert, MAX_BULK_INSERT_BATCH_SIZE)
        Log.d(
            "INSERT_MANY",
            "Inserting $numResourcesToInsert FHIR resources in batches of "
                    + "$MAX_BULK_INSERT_BATCH_SIZE, calculated $numberOfBatches batches"
        )

        // Disable button while insert is running
        view.requireViewById<Button>(R.id.phr_seed_x_fhir_jsons_button).isEnabled = false
        // Show progress bar
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)
        progressBar.visibility = View.VISIBLE

        var numInsertedResources = 0

        try {
            val allResources = loadAllFhirJSONs(view)
            if (allResources.isEmpty()) return "Found no resource files to insert. Please select " +
                    "a patient context with FHIR resources."

            for (i in 0..numberOfBatches - 1) {
                val upsertRequests: MutableList<UpsertMedicalResourceRequest> = mutableListOf()
                val isLastBatch = i == numberOfBatches - 1
                val numResources = if (isLastBatch)
                    numResourcesToInsert - i * MAX_BULK_INSERT_BATCH_SIZE
                else MAX_BULK_INSERT_BATCH_SIZE

                for (j in 1..numResources) {
                    val resourceNumber = i * MAX_BULK_INSERT_BATCH_SIZE + j
                    var resourceJson = allResources[resourceNumber % allResources.size]
                    resourceJson =
                        JSONObject(resourceJson).put("id", resourceNumber.toString()).toString()
                    upsertRequests.add(
                        UpsertMedicalResourceRequest.Builder(
                            insertedDataSourceId,
                            FhirVersion.parseFhirVersion("4.0.1"),
                            resourceJson,
                        ).build()
                    )
                }

                numInsertedResources += upsertMedicalResources(upsertRequests).size
                val progress: Int = numInsertedResources * 100 / numResourcesToInsert
                progressBar.setProgress(progress, true)
            }

            return "SUCCESSFUL DATA UPSERT \n\nUpserted $numInsertedResources resources."
        } finally {
            progressBar.visibility = View.GONE
            progressBar.progress = 0
            view.requireViewById<Button>(R.id.phr_seed_x_fhir_jsons_button).isEnabled = true
        }
    }

    private suspend fun upsertMedicalResources(
        requests: List<UpsertMedicalResourceRequest>
    ): List<MedicalResource> {
        Log.d("UPSERT_RESOURCES", "Writing ${requests.size} resources")
        val resources =
            suspendCancellableCoroutine<List<MedicalResource>> { continuation ->
                healthConnectManager.upsertMedicalResources(
                    requests,
                    Runnable::run,
                    continuation.asOutcomeReceiver(),
                )
            }
        Log.d("UPSERT_RESOURCES", "Wrote ${resources.size} resources")
        return resources
    }

    private suspend fun getMedicalDataSources(): List<MedicalDataSource> {
        val dataSources = suspendCancellableCoroutine<List<MedicalDataSource>> {
            continuation ->
                healthConnectManager.getMedicalDataSources(
                    GetMedicalDataSourcesRequest.Builder()
                        .addPackageName(requireContext().packageName)
                        .build(),
                    Runnable::run,
                    continuation.asOutcomeReceiver(),
                )
            }
        Log.d("GETTING_DATA_SOURCE", "Got sources: $dataSources")
        return dataSources
    }

    private suspend fun createMedicalDataSource(
        view: View
    ): String {
        val userEnteredDataSourceDisplayName =
            view.findViewById<EditText>(R.id.phr_data_source_display_name_text)
        val displayName = userEnteredDataSourceDisplayName.text.toString().ifEmpty {
            "My Hospital " + (0..1000).random() }

        val dataSource =
            suspendCancellableCoroutine<MedicalDataSource> { continuation ->
                healthConnectManager.createMedicalDataSource(
                    CreateMedicalDataSourceRequest.Builder(
                        Uri.parse("https://example.fhir.com/R4/123"),
                        displayName,
                        FhirVersion.parseFhirVersion("4.0.1"))
                        .build(),
                    Runnable::run,
                    continuation.asOutcomeReceiver(),
                )
            }
        Log.d("CREATE_DATA_SOURCE", "Created source: $dataSource")
        updateDataSourceSpinnerOptions(
            view, mExistingDataSourceNamesToId.keys.toList(), dataSource.displayName
        )
        mExistingDataSourceNamesToId.put(dataSource.displayName, dataSource.id)
        userEnteredDataSourceDisplayName.text = null
        return "Created data source: $displayName"
    }

    private suspend fun readImmunization(view: View): String {
        return readImmunization()
            .joinToString(separator = "\n", transform = MedicalResource::toString)
    }

    private suspend fun readImmunization(): List<MedicalResource> {

        var receiver: OutcomeReceiver<ReadMedicalResourcesResponse, HealthConnectException>
        val request: ReadMedicalResourcesInitialRequest =
            ReadMedicalResourcesInitialRequest.Builder(MEDICAL_RESOURCE_TYPE_VACCINES).build()
        val resources =
            suspendCancellableCoroutine<ReadMedicalResourcesResponse> { continuation ->
                receiver = continuation.asOutcomeReceiver()
                healthConnectManager.readMedicalResources(request, Runnable::run, receiver)
            }
                .medicalResources
        Log.d("READ_MEDICAL_RESOURCES", "Read ${resources.size} resources")
        return resources
    }

    private fun loadAllFhirJSONs(view: View): List<String> {
        val patientContext =
            view.findViewById<Spinner>(R.id.phr_patient_spinner).selectedItem.toString()
        val allResources = if (patientContext == CUSTOM_DATA_PATH_PLACE_HOLDER)
            loadAllFhirJSONsFromContentUris(mSelectedCustomDirFilesToUri?.values.orEmpty().toList())
        else loadAllFhirJSONsFromAssets(patientContext)

        return allResources
    }

    private fun listFhirJSONAssetFiles(context: Context, path: String): List<String> {
        val assetManager = context.assets
        return try {
            assetManager.list(path)?.filter { it.endsWith(".json") } ?: emptyList()
        } catch (e: IOException) {
            Log.e("listFhirJSONAssetFiles", "Error listing assets in path $path", e)
            Toast.makeText(
                context,
                "Error listing JSON files: ${e.localizedMessage}",
                Toast.LENGTH_SHORT,
            )
                .show()
            emptyList()
        }
    }

    private fun loadAllFhirJSONsFromAssets(path: String): List<String> {
        val jsonFiles = listFhirJSONAssetFiles(requireContext(), path)

        return jsonFiles
            .mapNotNull {
                val jsonString = loadJSONFromAsset(requireContext(), path, it)
                Log.i("loadAllFhirJSONsAssets", "$it: $jsonString")
                jsonString
            }
    }

    fun loadJSONFromAsset(context: Context, path: String, fileName: String): String? {
        return try {
            val inputStream = context.assets.open("$path/$fileName")
            readJSONFromInputStream(inputStream)
        } catch (e: IOException) {
            Log.e("loadJSONFromAsset", "Error reading JSON file", e)
            Toast.makeText(
                context,
                "Error reading JSON file from Asset: ${e.localizedMessage}",
                Toast.LENGTH_SHORT,
            )
                .show()
            null
        }
    }

    private fun loadAllFhirJSONsFromContentUris(uris: List<Uri>): List<String> {
        return uris
            .mapNotNull {
                val jsonString = loadJSONFromContentUri(requireContext(), it)
                Log.i("loadAllFhirJSONsUris", "$it: $jsonString")
                jsonString
            }
    }

    fun loadJSONFromContentUri(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            readJSONFromInputStream(inputStream!!)
        } catch (e: IOException) {
            Log.e("loadJSONFromContentUri", "Error reading JSON file", e)
            Toast.makeText(
                context,
                "Error reading JSON file from content Uri: ${e.localizedMessage}",
                Toast.LENGTH_SHORT,
            )
                .show()
            null
        }

        val inputStream = context.contentResolver.openInputStream(uri)
        return readJSONFromInputStream(inputStream!!)
    }

    fun readJSONFromInputStream(inputStream: InputStream): String {
        val buffer = ByteArray(inputStream.available())
        inputStream.read(buffer)
        inputStream.close()
        return buffer.toString(Charsets.UTF_8)
    }

    private fun requestMedicalPermissions() {
        mRequestPermissionLauncher.launch(MEDICAL_PERMISSIONS)
    }
}
