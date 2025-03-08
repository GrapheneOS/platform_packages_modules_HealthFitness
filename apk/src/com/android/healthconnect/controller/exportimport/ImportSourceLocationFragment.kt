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
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.navigation.fragment.findNavController
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.exportimport.api.DocumentProvider
import com.android.healthconnect.controller.exportimport.api.DocumentProviderInfo
import com.android.healthconnect.controller.exportimport.api.DocumentProviderRoot
import com.android.healthconnect.controller.exportimport.api.DocumentProviders
import com.android.healthconnect.controller.exportimport.api.ExportSettingsViewModel
import com.android.healthconnect.controller.exportimport.api.isLocalFile
import com.android.healthconnect.controller.shared.dialog.AlertDialogBuilder
import com.android.healthconnect.controller.shared.preference.HealthSetupFragment
import com.android.healthconnect.controller.shared.preference.RadioButtonPreferenceCategory
import com.android.healthconnect.controller.utils.DeviceInfoUtils
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.ImportSourceLocationElement
import com.android.healthconnect.controller.utils.logging.PageName
import com.android.healthconnect.controller.utils.logging.UnknownGenericElement
import com.android.settingslib.widget.FooterPreference
import com.android.settingslib.widget.SelectorWithWidgetPreference
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

    companion object {
        private val IMPORT_GROUP = "import_group"
        private val FOOTER_PREF = "footer_pref"
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

        viewModel.documentProviders.observe(viewLifecycleOwner) { providers: DocumentProviders ->
            preferenceScreen.removePreferenceRecursively(IMPORT_GROUP)
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
                    setupRadioButtons(
                        documentProviders = providers.providers,
                        selectedDocumentProvider = viewModel.selectedDocumentProvider.value,
                        selectedDocumentProviderRoot = viewModel.selectedDocumentProviderRoot.value,
                        selectedRootsForDocumentProviders =
                            viewModel.selectedRootsForDocumentProviders,
                    )

                    setupFooter(providers.providers)
                }
            }
        }

        viewModel.selectedDocumentProvider.observe(viewLifecycleOwner) { documentProvider ->
            documentProvider?.let { updateSelection(it) }
        }
    }

    private fun setupFooter(providers: List<DocumentProvider>) {
        if (providers.size > 1) {
            // no footer if more than one document provider
            return
        }

        val footerPreference =
            FooterPreference(requireContext()).also {
                it.key = FOOTER_PREF
                it.order = 10
            }

        if (providers.isEmpty()) {
            footerPreference.setIconVisibility(GONE)
            footerPreference.title = getString(R.string.export_import_no_apps_text)
            setFooterPreferenceLearnMore(footerPreference)
        } else {
            footerPreference.setIconVisibility(VISIBLE)
            footerPreference.title = getString(R.string.export_import_install_apps_text)
            setFooterPreferenceLearnMore(footerPreference)
        }

        preferenceScreen.addPreference(footerPreference)
    }

    private fun setFooterPreferenceLearnMore(footerPreference: FooterPreference) {
        if (!deviceInfoUtils.isPlayStoreAvailable(requireContext())) {
            return
        }

        footerPreference.setLearnMoreText(getString(R.string.export_import_go_to_play_store_text))
        footerPreference.setLearnMoreAction {
            findNavController().navigate(R.id.action_importSourceLocationFragment_to_playStore)
        }
    }

    private fun updateSelection(documentProviderInfo: DocumentProviderInfo) {
        val group = preferenceScreen.findPreference<RadioButtonPreferenceCategory>(IMPORT_GROUP)
        if (group != null && documentProviderInfo.title.isNotEmpty()) {
            group.updateSelectedOption(documentProviderInfo.title)
        }
    }

    private fun updateSummary(documentProviderInfo: DocumentProviderInfo, newSummary: String) {
        val group = preferenceScreen.findPreference<RadioButtonPreferenceCategory>(IMPORT_GROUP)
        if (group != null && documentProviderInfo.title.isNotEmpty()) {
            group.updateSummary(documentProviderInfo.title, newSummary)
        }
    }

    private fun setupRadioButtons(
        documentProviders: List<DocumentProvider>,
        selectedDocumentProvider: DocumentProviderInfo?,
        selectedDocumentProviderRoot: DocumentProviderRoot?,
        selectedRootsForDocumentProviders: LiveData<MutableMap<String, DocumentProviderRoot?>>,
    ) {
        var currentlySelectedKey = selectedDocumentProvider?.title ?: ""
        val options = mutableListOf<RadioButtonPreferenceCategory.RadioButtonOption>()

        documentProviders.forEach { documentProvider ->
            val key = documentProvider.info.title
            val title = documentProvider.info.title
            val icon =
                loadPackageIcon(
                    requireContext(),
                    documentProvider.info.authority,
                    documentProvider.info.iconResource,
                )

            val listener =
                createSelectionListener(documentProvider, selectedRootsForDocumentProviders)
            val summary =
                createSummary(
                    documentProvider,
                    documentProviders,
                    selectedDocumentProvider,
                    selectedDocumentProviderRoot,
                )

            if (documentProviders.size == 1) {
                currentlySelectedKey = key
                updateSelection(documentProvider.info)
                onSelectionChanged(documentProvider.info, documentProvider.roots[0])
            }

            if (
                documentProvider.info == selectedDocumentProvider &&
                    selectedDocumentProvider != null
            ) {
                currentlySelectedKey = key
                updateSelection(documentProvider.info)
                selectedDocumentProviderRoot?.let { onSelectionChanged(documentProvider.info, it) }
            }

            options.add(
                RadioButtonPreferenceCategory.RadioButtonOption(
                    key = key,
                    title = title,
                    element = UnknownGenericElement.UNKNOWN_BUTTON,
                    summary = summary,
                    icon = icon,
                    listener = listener,
                )
            )
        }

        val group =
            RadioButtonPreferenceCategory(
                    context = requireContext(),
                    childFragmentManager = childFragmentManager,
                    options = options,
                    logger = logger,
                    preferenceKey = IMPORT_GROUP,
                    currentSelectedKey = currentlySelectedKey,
                )
                .also { it.order = 2 }
        preferenceScreen.addPreference(group)
    }

    private fun createSelectionListener(
        documentProvider: DocumentProvider,
        selectedRootsForDocumentProviders: LiveData<MutableMap<String, DocumentProviderRoot?>>,
    ): SelectorWithWidgetPreference.OnClickListener {
        return if (documentProvider.roots.size == 1) {
            SelectorWithWidgetPreference.OnClickListener {
                updateSelection(documentProvider.info)
                onSelectionChanged(documentProvider.info, documentProvider.roots[0])
            }
        } else {
            SelectorWithWidgetPreference.OnClickListener {
                showChooseAccountDialog(
                    LayoutInflater.from(requireContext()),
                    documentProvider.roots,
                    selectedRootsForDocumentProviders,
                    documentProvider.info.title,
                ) { root ->
                    updateSelection(documentProvider.info)
                    updateSummary(documentProvider.info, root.summary)
                    onSelectionChanged(documentProvider.info, root)
                }
            }
        }
    }

    private fun createSummary(
        documentProvider: DocumentProvider,
        documentProviders: List<DocumentProvider>,
        selectedDocumentProvider: DocumentProviderInfo?,
        selectedDocumentProviderRoot: DocumentProviderRoot?,
    ): String? {
        return when {
            selectedDocumentProvider != null && selectedDocumentProvider == documentProvider.info ->
                selectedDocumentProviderRoot?.summary
            documentProvider.roots.size == 1 -> documentProvider.roots[0].summary.ifEmpty { null }
            documentProviders.size == 1 -> getString(R.string.export_import_tap_to_choose_account)
            else -> null
        }
    }

    private fun showChooseAccountDialog(
        inflater: LayoutInflater,
        roots: List<DocumentProviderRoot>,
        selectedRootsForDocumentProviders: LiveData<MutableMap<String, DocumentProviderRoot?>>,
        title: String,
        onSelectionChanged: (root: DocumentProviderRoot) -> Unit,
    ) {
        val view = inflater.inflate(R.layout.dialog_export_import_account, null)

        val radioGroup = view.findViewById<RadioGroup>(R.id.radio_group_account)

        // Use reference to the live data so could retrieve updated stored roots app
        val preselectedSummary = selectedRootsForDocumentProviders.value?.get(title)?.summary

        for (i in roots.indices) {
            val radioButton =
                inflater.inflate(R.layout.item_document_provider_account, radioGroup, false)
                    as RadioButton
            radioButton.text = roots[i].summary
            radioButton.id = i
            if (
                (preselectedSummary == null && i == 0) ||
                    (preselectedSummary != null && preselectedSummary == roots[i].summary)
            ) {
                radioButton.isChecked = true
            }
            radioGroup.addView(radioButton)
        }

        // TODO: b/339189778 - Add proper logging for the account picker dialog.
        AlertDialogBuilder(inflater.context, UnknownGenericElement.UNKNOWN_DIALOG)
            .setView(view)
            .setNeutralButton(
                R.string.export_import_choose_account_cancel_button,
                UnknownGenericElement.UNKNOWN_DIALOG_NEUTRAL_BUTTON,
            )
            .setPositiveButton(
                R.string.export_import_choose_account_done_button,
                UnknownGenericElement.UNKNOWN_DIALOG_POSITIVE_BUTTON,
            ) { _, _ ->
                onSelectionChanged(roots[radioGroup.checkedRadioButtonId])
            }
            .create()
            .show()
    }

    private fun loadPackageIcon(context: Context, authority: String, icon: Int): Drawable? {
        val info = context.packageManager.resolveContentProvider(authority, 0)
        if (info != null) {
            return context.packageManager.getDrawable(info.packageName, icon, info.applicationInfo)
        }

        return null
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
