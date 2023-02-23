/**
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * ```
 *      http://www.apache.org/licenses/LICENSE-2.0
 * ```
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.permissions.connectedapps

import android.content.Intent
import android.content.Intent.EXTRA_PACKAGE_NAME
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.annotation.StringRes
import androidx.core.os.bundleOf
import androidx.fragment.app.commitNow
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceGroup
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.deletion.DeletionConstants
import com.android.healthconnect.controller.deletion.DeletionConstants.DELETION_TYPE
import com.android.healthconnect.controller.deletion.DeletionConstants.FRAGMENT_TAG_DELETION
import com.android.healthconnect.controller.deletion.DeletionFragment
import com.android.healthconnect.controller.deletion.DeletionType
import com.android.healthconnect.controller.permissions.connectedapps.ConnectedAppsViewModel.DisconnectAllState
import com.android.healthconnect.controller.permissions.shared.Constants.EXTRA_APP_NAME
import com.android.healthconnect.controller.shared.app.ConnectedAppMetadata
import com.android.healthconnect.controller.shared.app.ConnectedAppStatus.ALLOWED
import com.android.healthconnect.controller.shared.app.ConnectedAppStatus.DENIED
import com.android.healthconnect.controller.shared.app.ConnectedAppStatus.INACTIVE
import com.android.healthconnect.controller.shared.dialog.AlertDialogBuilder
import com.android.healthconnect.controller.shared.inactiveapp.InactiveAppPreference
import com.android.healthconnect.controller.shared.preference.HealthPreferenceFragment
import com.android.healthconnect.controller.utils.dismissLoadingDialog
import com.android.healthconnect.controller.utils.setupMenu
import com.android.healthconnect.controller.utils.setupSharedMenu
import com.android.healthconnect.controller.utils.showLoadingDialog
import com.android.settingslib.widget.AppPreference
import com.android.settingslib.widget.TopIntroPreference
import dagger.hilt.android.AndroidEntryPoint

/** Fragment for connected apps screen. */
@AndroidEntryPoint(HealthPreferenceFragment::class)
class ConnectedAppsFragment : Hilt_ConnectedAppsFragment() {

    companion object {
        private const val TOP_INTRO = "connected_apps_top_intro"
        const val ALLOWED_APPS_CATEGORY = "allowed_apps"
        private const val NOT_ALLOWED_APPS = "not_allowed_apps"
        private const val HELP_AND_FEEDBACK = "help_and_feedback"
        private const val REMOVE_ALL_APPS = "remove_all_apps"
        private const val INACTIVE_APPS = "inactive_apps"
        private const val THINGS_TO_TRY = "things_to_try_app_permissions_screen"
        private const val SETTINGS_AND_HELP = "settings_and_help"
        private const val CHECK_FOR_UPDATES = "check_for_updates_app_permissions_screen"
        private const val SEE_ALL_COMPATIBLE_APPS = "see_all_compatible_apps_app_permissions_screen"
        private const val SEND_FEEDBACK = "send_feedback_app_permissions_screen"
    }

    private val viewModel: ConnectedAppsViewModel by viewModels()
    private lateinit var searchMenuItem: MenuItem

    private val mTopIntro: TopIntroPreference? by lazy {
        preferenceScreen.findPreference(TOP_INTRO)
    }

    private val mAllowedAppsCategory: PreferenceGroup? by lazy {
        preferenceScreen.findPreference(ALLOWED_APPS_CATEGORY)
    }

    private val mNotAllowedAppsCategory: PreferenceGroup? by lazy {
        preferenceScreen.findPreference(NOT_ALLOWED_APPS)
    }

    private val mInactiveAppsPreference: PreferenceGroup? by lazy {
        preferenceScreen.findPreference(INACTIVE_APPS)
    }

    private val mThingsToTryCategory: PreferenceGroup? by lazy {
        preferenceScreen.findPreference(THINGS_TO_TRY)
    }

    private val mSettingAndHelpCategory: PreferenceGroup? by lazy {
        preferenceScreen.findPreference(SETTINGS_AND_HELP)
    }

    private val mHelpAndFeedbackPreference: Preference? by lazy {
        preferenceScreen.findPreference(HELP_AND_FEEDBACK)
    }

    private val mRemoveAllApps: Preference? by lazy {
        preferenceScreen.findPreference(REMOVE_ALL_APPS)
    }

    private val mCheckForUpdates: Preference? by lazy {
        preferenceScreen.findPreference(CHECK_FOR_UPDATES)
    }

    private val mSeeAllCompatibleApps: Preference? by lazy {
        preferenceScreen.findPreference(SEE_ALL_COMPATIBLE_APPS)
    }

    private val mSendFeedback: Preference? by lazy {
        preferenceScreen.findPreference(SEND_FEEDBACK)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.connected_apps_screen, rootKey)
        mHelpAndFeedbackPreference?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.action_connectedApps_to_helpAndFeedback)
            true
        }

        if (childFragmentManager.findFragmentByTag(FRAGMENT_TAG_DELETION) == null) {
            childFragmentManager.commitNow { add(DeletionFragment(), FRAGMENT_TAG_DELETION) }
        }
        mCheckForUpdates?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.action_connected_apps_to_updated_apps)
            true
        }

        mSeeAllCompatibleApps?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.action_connected_apps_to_play_store)
            true
        }

        mSendFeedback?.setOnPreferenceClickListener {
            val intent = Intent(Intent.ACTION_BUG_REPORT)
            requireActivity().startActivity(intent)
            true
        }
    }

    private fun openRemoveAllAppsAccessDialog(apps: List<ConnectedAppMetadata>) {
        AlertDialogBuilder(this)
            .setIcon(R.attr.disconnectAllIcon)
            .setTitle(R.string.permissions_disconnect_all_dialog_title)
            .setMessage(R.string.permissions_disconnect_all_dialog_message)
            .setNegativeButton(android.R.string.cancel)
            .setPositiveButton(R.string.permissions_disconnect_all_dialog_disconnect) { _, _ ->
                viewModel.disconnectAllApps(apps)
            }
            .create()
            .show()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadConnectedApps()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeConnectedApps()
        observeRevokeAllAppsPermissions()
    }

    private fun observeRevokeAllAppsPermissions() {
        viewModel.disconnectAllState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is DisconnectAllState.Loading -> {
                    showLoadingDialog()
                }
                else -> {
                    dismissLoadingDialog()
                }
            }
        }
    }

    private fun observeConnectedApps() {
        viewModel.connectedApps.observe(viewLifecycleOwner) { connectedApps ->
            if (connectedApps.isEmpty()) {
                setupSharedMenu(viewLifecycleOwner)
                mTopIntro?.title = getString(R.string.connected_apps_empty_list_section_title)
                mThingsToTryCategory?.isVisible = true
                mInactiveAppsPreference?.isVisible = false
                mAllowedAppsCategory?.isVisible = false
                mNotAllowedAppsCategory?.isVisible = false
                mSettingAndHelpCategory?.isVisible = false
            } else {
                setupMenu(R.menu.connected_apps, viewLifecycleOwner) { menuItem ->
                    when (menuItem.itemId) {
                        R.id.menu_search -> {
                            searchMenuItem = menuItem
                            findNavController().navigate(R.id.action_connectedApps_to_searchApps)
                            true
                        }
                        else -> false
                    }
                }
                mTopIntro?.title = getString(R.string.connected_apps_text)
                mThingsToTryCategory?.isVisible = false
                mInactiveAppsPreference?.isVisible = true
                mAllowedAppsCategory?.isVisible = true
                mNotAllowedAppsCategory?.isVisible = true
                mSettingAndHelpCategory?.isVisible = true
                val connectedAppsGroup = connectedApps.groupBy { it.status }
                val allowedApps = connectedAppsGroup[ALLOWED].orEmpty()
                val notAllowedApps = connectedAppsGroup[DENIED].orEmpty()
                updateAllowedApps(allowedApps)
                updateDeniedApps(notAllowedApps)
                updateInactiveApps(connectedAppsGroup[INACTIVE].orEmpty())

                val activeApps: MutableList<ConnectedAppMetadata> = allowedApps.toMutableList()
                activeApps.addAll(notAllowedApps)
                mRemoveAllApps?.isEnabled = allowedApps.isNotEmpty()
                mRemoveAllApps?.setOnPreferenceClickListener {
                    openRemoveAllAppsAccessDialog(activeApps)
                    true
                }
            }
        }
    }

    private fun updateInactiveApps(appsList: List<ConnectedAppMetadata>) {
        mInactiveAppsPreference?.removeAll()
        if (appsList.isEmpty()) {
            preferenceScreen.removePreference(mInactiveAppsPreference)
        } else {
            appsList.forEach { app ->
                val inactiveAppPreference =
                    InactiveAppPreference(requireContext()).also {
                        it.title = app.appMetadata.appName
                        it.icon = app.appMetadata.icon
                        it.setOnDeleteButtonClickListener {
                            val appDeletionType =
                                DeletionType.DeletionTypeAppData(
                                    app.appMetadata.packageName, app.appMetadata.appName)
                            childFragmentManager.setFragmentResult(
                                DeletionConstants.START_INACTIVE_APP_DELETION_EVENT,
                                bundleOf(DELETION_TYPE to appDeletionType))
                        }
                    }
                mInactiveAppsPreference?.addPreference(inactiveAppPreference)
            }
        }
    }

    private fun updateAllowedApps(appsList: List<ConnectedAppMetadata>) {
        mAllowedAppsCategory?.removeAll()
        if (appsList.isEmpty()) {
            mAllowedAppsCategory?.addPreference(getNoAppsPreference(R.string.no_apps_allowed))
        } else {
            appsList.forEach { app ->
                mAllowedAppsCategory?.addPreference(
                    getAppPreference(app) { navigateToAppInfoScreen(app) })
            }
        }
    }

    private fun updateDeniedApps(appsList: List<ConnectedAppMetadata>) {
        mNotAllowedAppsCategory?.removeAll()

        if (appsList.isEmpty()) {
            mNotAllowedAppsCategory?.addPreference(getNoAppsPreference(R.string.no_apps_denied))
        } else {
            appsList.forEach { app ->
                mNotAllowedAppsCategory?.addPreference(
                    getAppPreference(app) { navigateToAppInfoScreen(app) })
            }
        }
    }

    private fun navigateToAppInfoScreen(app: ConnectedAppMetadata) {
        findNavController()
            .navigate(
                R.id.action_connectedApps_to_connectedApp,
                bundleOf(
                    EXTRA_PACKAGE_NAME to app.appMetadata.packageName,
                    EXTRA_APP_NAME to app.appMetadata.appName))
    }

    private fun getNoAppsPreference(@StringRes res: Int): Preference {
        return Preference(context).also {
            it.setTitle(res)
            it.isSelectable = false
        }
    }

    private fun getAppPreference(
        app: ConnectedAppMetadata,
        onClick: (() -> Unit)? = null
    ): AppPreference {
        return HealthAppPreference(requireContext(), app.appMetadata).also {
            it.setOnPreferenceClickListener {
                onClick?.invoke()
                true
            }
        }
    }
}
