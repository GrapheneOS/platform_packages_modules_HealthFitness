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

package com.android.healthconnect.controller.exportimport

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.exportimport.DocumentProviderGroupHelper.Companion.FOOTER_PREF
import com.android.healthconnect.controller.exportimport.DocumentProviderGroupHelper.Companion.RADIO_GROUP_PREF
import com.android.healthconnect.controller.exportimport.api.DocumentProviderInfo
import com.android.healthconnect.controller.exportimport.api.DocumentProviderRoot
import com.android.healthconnect.controller.exportimport.api.DocumentProviders
import com.android.healthconnect.controller.exportimport.api.ExportSettingsViewModel
import com.android.healthconnect.controller.exportimport.api.isLocalFile
import com.android.healthconnect.controller.shared.preference.HealthSetupFragment
import com.android.healthconnect.controller.utils.DeviceInfoUtils
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.ImportSourceLocationElement
import com.android.healthconnect.controller.utils.logging.PageName
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Fragment to allow the user to find and select the backup file to import and restore. */
@AndroidEntryPoint(HealthSetupFragment::class)
class ImportSourceLocationFragment : Hilt_ImportSourceLocationFragment() {
    private val contract = ActivityResultContracts.StartActivityForResult()
    private val saveResultLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(contract, ::onSave)

    private val viewModel: ExportSettingsViewModel by viewModels()
    private lateinit var nextButton: Button

    @Inject lateinit var deviceInfoUtils: DeviceInfoUtils
    @Inject lateinit var logger: HealthConnectLogger

    init {
        this.setPageName(PageName.IMPORT_SOURCE_LOCATION_PAGE)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.import_source_location_screen, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        nextButton = getPrimaryButtonFull()
        val cancelButton: Button = getSecondaryButton()

        cancelButton.text = getString(R.string.export_cancel_button)
        nextButton.text = getString(R.string.export_next_button)
        logger.logImpression(ImportSourceLocationElement.IMPORT_SOURCE_LOCATION_CANCEL_BUTTON)
        logger.logImpression(ImportSourceLocationElement.IMPORT_SOURCE_LOCATION_NEXT_BUTTON)

        cancelButton.setOnClickListener {
            logger.logInteraction(ImportSourceLocationElement.IMPORT_SOURCE_LOCATION_CANCEL_BUTTON)
            requireActivity().finish()
        }

        val documentHelper =
            DocumentProviderGroupHelper(
                requireContext(),
                preferenceScreen = preferenceScreen,
                logger = logger,
                childFragmentManager = childFragmentManager,
                isPlayStoreAvailable = deviceInfoUtils.isPlayStoreAvailable(requireContext()),
                ::onSelectionChanged,
            )

        viewModel.documentProviders.observe(viewLifecycleOwner) { providers: DocumentProviders ->
            preferenceScreen.removePreferenceRecursively(RADIO_GROUP_PREF)
            preferenceScreen.removePreferenceRecursively(FOOTER_PREF)
            nextButton.setOnClickListener {}
            nextButton.isEnabled = false

            when (providers) {
                is DocumentProviders.Loading -> {
                    // Do nothing
                }

                is DocumentProviders.LoadingFailed -> {
                    Toast.makeText(activity, R.string.default_error, Toast.LENGTH_LONG).show()
                }

                is DocumentProviders.WithData -> {
                    val radioGroup =
                        documentHelper.setupRadioButtons(
                            documentProviders = providers.providers,
                            selectedDocumentProvider = viewModel.selectedDocumentProvider.value,
                            selectedDocumentProviderRoot =
                                viewModel.selectedDocumentProviderRoot.value,
                            selectedRootsForDocumentProviders =
                                viewModel.selectedRootsForDocumentProviders,
                        )

                    preferenceScreen.addPreference(radioGroup)

                    documentHelper.setupFooter(providers.providers) {
                        findNavController()
                            .navigate(R.id.action_importSourceLocationFragment_to_playStore)
                    }
                }
            }
        }

        viewModel.selectedDocumentProvider.observe(viewLifecycleOwner) { documentProvider ->
            documentProvider?.let { provider -> documentHelper.updateSelectedOption(provider) }
        }
    }

    private fun onSelectionChanged(provider: DocumentProviderInfo, root: DocumentProviderRoot) {
        viewModel.updateSelectedDocumentProvider(provider, root)
        nextButton.setOnClickListener {
            logger.logInteraction(ImportSourceLocationElement.IMPORT_SOURCE_LOCATION_NEXT_BUTTON)
            saveResultLauncher.launch(
                Intent(Intent.ACTION_OPEN_DOCUMENT)
                    .addFlags(
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                    .setType("application/zip")
                    .addCategory(Intent.CATEGORY_OPENABLE)
                    .putExtra(DocumentsContract.EXTRA_INITIAL_URI, root.uri)
            )
        }
        nextButton.isEnabled = true
    }

    private fun onSave(result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK) {
            val fileUri = result.data?.data ?: return
            if (isLocalFile(fileUri)) {
                Toast.makeText(activity, R.string.import_invalid_storage, Toast.LENGTH_LONG).show()
            } else {
                // TODO: b/339189778 - Add test when import API is done.
                val bundle = Bundle()
                bundle.putString(
                    ImportConfirmationDialogFragment.IMPORT_FILE_URI_KEY,
                    fileUri.toString(),
                )
                val dialogFragment = ImportConfirmationDialogFragment()
                dialogFragment.arguments = bundle
                dialogFragment.show(childFragmentManager, ImportConfirmationDialogFragment.TAG)
            }
        }
    }
}
