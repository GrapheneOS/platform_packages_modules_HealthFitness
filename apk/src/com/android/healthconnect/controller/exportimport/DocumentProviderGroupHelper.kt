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
package com.android.healthconnect.controller.exportimport

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LiveData
import androidx.preference.PreferenceScreen
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.exportimport.api.DocumentProvider
import com.android.healthconnect.controller.exportimport.api.DocumentProviderInfo
import com.android.healthconnect.controller.exportimport.api.DocumentProviderRoot
import com.android.healthconnect.controller.shared.dialog.AlertDialogBuilder
import com.android.healthconnect.controller.shared.preference.RadioButtonPreferenceCategory
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.UnknownGenericElement
import com.android.settingslib.widget.FooterPreference
import com.android.settingslib.widget.SelectorWithWidgetPreference

/** A helper class that sets up the radio group preference and footer for Import/Export screens. */
class DocumentProviderGroupHelper(
    private val context: Context,
    private val preferenceScreen: PreferenceScreen,
    private val logger: HealthConnectLogger,
    private val childFragmentManager: FragmentManager,
    private val isPlayStoreAvailable: Boolean,
    private val onSelectionChanged:
        (provider: DocumentProviderInfo, root: DocumentProviderRoot) -> Unit,
) {
    companion object {
        val RADIO_GROUP_PREF = "radio_group"
        val FOOTER_PREF = "footer_pref"
    }

    fun setupRadioButtons(
        documentProviders: List<DocumentProvider>,
        selectedDocumentProvider: DocumentProviderInfo?,
        selectedDocumentProviderRoot: DocumentProviderRoot?,
        selectedRootsForDocumentProviders: LiveData<MutableMap<String, DocumentProviderRoot?>>,
    ): RadioButtonPreferenceCategory {
        var currentlySelectedKey = selectedDocumentProvider?.title ?: ""
        val options = mutableListOf<RadioButtonPreferenceCategory.RadioButtonOption>()

        documentProviders.forEach { documentProvider ->
            val key = documentProvider.info.title
            val title = documentProvider.info.title
            val icon =
                loadPackageIcon(documentProvider.info.authority, documentProvider.info.iconResource)

            val listener =
                createSelectionListener(documentProvider, selectedRootsForDocumentProviders)
            val summary =
                createSummary(
                    documentProvider,
                    documentProviders,
                    selectedDocumentProvider,
                    selectedDocumentProviderRoot,
                )

            // Only automatically select option if it's the only document provider with the only
            // root
            if (documentProviders.size == 1 && documentProvider.roots.size == 1) {
                currentlySelectedKey = key
                updateSelection(documentProviderInfo = documentProvider.info)
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
                    context = context,
                    childFragmentManager = childFragmentManager,
                    options = options,
                    logger = logger,
                    preferenceKey = RADIO_GROUP_PREF,
                    currentSelectedKey = currentlySelectedKey,
                )
                .also { it.order = 2 }
        return group
    }

    private fun updateSelection(documentProviderInfo: DocumentProviderInfo) {
        val group = preferenceScreen.findPreference<RadioButtonPreferenceCategory>(RADIO_GROUP_PREF)
        if (group != null && documentProviderInfo.title.isNotEmpty()) {
            group.updateSelectedOption(documentProviderInfo.title)
        }
    }

    private fun updateSummary(documentProviderInfo: DocumentProviderInfo, newSummary: String) {
        val group = preferenceScreen.findPreference<RadioButtonPreferenceCategory>(RADIO_GROUP_PREF)
        if (group != null && documentProviderInfo.title.isNotEmpty()) {
            group.updateSummary(documentProviderInfo.title, newSummary)
        }
    }

    fun updateSelectedOption(provider: DocumentProviderInfo) {
        val group = preferenceScreen.findPreference<RadioButtonPreferenceCategory>(RADIO_GROUP_PREF)
        if (group != null && provider.title.isNotEmpty()) {
            group.updateSelectedOption(provider.title)
        }
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
                    LayoutInflater.from(context),
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
            documentProviders.size == 1 ->
                context.getString(R.string.export_import_tap_to_choose_account)
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

    private fun loadPackageIcon(authority: String, icon: Int): Drawable? {
        val info = context.packageManager.resolveContentProvider(authority, 0)
        if (info != null) {
            return context.packageManager.getDrawable(info.packageName, icon, info.applicationInfo)
        }

        return null
    }

    fun setupFooter(providers: List<DocumentProvider>, navigationAction: View.OnClickListener) {
        if (providers.size > 1) {
            // no footer if more than one document provider
            return
        }

        val footerPreference =
            FooterPreference(context).also {
                it.key = FOOTER_PREF
                it.order = 10
            }

        if (providers.isEmpty()) {
            footerPreference.setIconVisibility(GONE)
            footerPreference.title = context.getString(R.string.export_import_no_apps_text)
            setFooterPreferenceLearnMore(footerPreference, navigationAction)
        } else {
            footerPreference.setIconVisibility(VISIBLE)
            footerPreference.title = context.getString(R.string.export_import_install_apps_text)
            setFooterPreferenceLearnMore(footerPreference, navigationAction)
        }

        preferenceScreen.addPreference(footerPreference)
    }

    private fun setFooterPreferenceLearnMore(
        footerPreference: FooterPreference,
        navigationAction: View.OnClickListener,
    ) {
        if (!isPlayStoreAvailable) {
            return
        }

        footerPreference.setLearnMoreText(
            context.getString(R.string.export_import_go_to_play_store_text)
        )
        footerPreference.setLearnMoreAction(navigationAction)
    }
}
