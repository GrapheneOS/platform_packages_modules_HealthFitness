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

import android.content.Context
import android.content.Intent
import android.content.Intent.EXTRA_PACKAGE_NAME
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commitNow
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceGroup
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.permissions.connectedapps.ConnectedAppsViewModel.DisconnectAllState
import com.android.healthconnect.controller.permissions.shared.HelpAndFeedbackFragment.Companion.APP_INTEGRATION_REQUEST_BUCKET_ID
import com.android.healthconnect.controller.permissions.shared.HelpAndFeedbackFragment.Companion.FEEDBACK_INTENT_RESULT_CODE
import com.android.healthconnect.controller.selectabledeletion.DeletionConstants.START_DELETION_KEY
import com.android.healthconnect.controller.selectabledeletion.DeletionFragment
import com.android.healthconnect.controller.selectabledeletion.DeletionType.DeleteAppData
import com.android.healthconnect.controller.selectabledeletion.DeletionViewModel
import com.android.healthconnect.controller.shared.Constants.APP_UPDATE_NEEDED_BANNER_SEEN
import com.android.healthconnect.controller.shared.Constants.EXTRA_APP_NAME
import com.android.healthconnect.controller.shared.Constants.USER_ACTIVITY_TRACKER
import com.android.healthconnect.controller.shared.app.AppPermissionsType
import com.android.healthconnect.controller.shared.app.ConnectedAppMetadata
import com.android.healthconnect.controller.shared.app.ConnectedAppStatus.ALLOWED
import com.android.healthconnect.controller.shared.app.ConnectedAppStatus.DENIED
import com.android.healthconnect.controller.shared.app.ConnectedAppStatus.INACTIVE
import com.android.healthconnect.controller.shared.app.ConnectedAppStatus.NEEDS_UPDATE
import com.android.healthconnect.controller.shared.dialog.AlertDialogBuilder
import com.android.healthconnect.controller.shared.inactiveapp.InactiveAppPreference
import com.android.healthconnect.controller.shared.preference.HealthBannerPreference
import com.android.healthconnect.controller.shared.preference.HealthPreference
import com.android.healthconnect.controller.shared.preference.HealthPreferenceFragment
import com.android.healthconnect.controller.utils.AppStoreUtils
import com.android.healthconnect.controller.utils.AttributeResolver
import com.android.healthconnect.controller.utils.DeviceInfoUtils
import com.android.healthconnect.controller.utils.NavigationUtils
import com.android.healthconnect.controller.utils.dismissLoadingDialog
import com.android.healthconnect.controller.utils.logging.AppPermissionsElement
import com.android.healthconnect.controller.utils.logging.DisconnectAllAppsDialogElement
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.MigrationElement
import com.android.healthconnect.controller.utils.logging.PageName
import com.android.healthconnect.controller.utils.pref
import com.android.healthconnect.controller.utils.setupMenu
import com.android.healthconnect.controller.utils.setupSharedMenu
import com.android.healthconnect.controller.utils.showLoadingDialog
import com.android.settingslib.widget.BannerMessagePreferenceGroup
import com.android.settingslib.widget.SettingsThemeHelper
import com.android.settingslib.widget.TopIntroPreference
import com.android.settingslib.widget.ZeroStatePreference
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Fragment for connected apps screen. */
@AndroidEntryPoint(HealthPreferenceFragment::class)
class ConnectedAppsFragment : Hilt_ConnectedAppsFragment() {

    companion object {
        private const val TOP_INTRO = "connected_apps_top_intro"
        const val ALLOWED_APPS_CATEGORY = "allowed_apps"
        private const val NOT_ALLOWED_APPS = "not_allowed_apps"
        private const val INACTIVE_APPS = "inactive_apps"
        private const val NEED_UPDATE_APPS = "need_update_apps"
        private const val KEY_ZERO_STATE = "zero_state_preference"
        private const val THINGS_TO_TRY = "things_to_try_app_permissions_screen"
        private const val SETTINGS_AND_HELP = "settings_and_help"
        private const val BANNER_PREFERENCE_KEY = "banner_preference"
        private const val FRAGMENT_TAG_DELETION = "FRAGMENT_TAG_DELETION"
        private const val BANNER_GROUP = "banner_group"
    }

    init {
        this.setPageName(PageName.APP_PERMISSIONS_PAGE)
    }

    @Inject lateinit var logger: HealthConnectLogger
    @Inject lateinit var appStoreUtils: AppStoreUtils
    @Inject lateinit var deviceInfoUtils: DeviceInfoUtils
    @Inject lateinit var navigationUtils: NavigationUtils

    private val viewModel: ConnectedAppsViewModel by viewModels()
    private val deletionViewModel: DeletionViewModel by activityViewModels()
    private lateinit var searchMenuItem: MenuItem
    private lateinit var removeAllAppsDialog: AlertDialog

    private val topIntroPreference: TopIntroPreference by pref(TOP_INTRO)
    private val allowedAppsCategory: PreferenceGroup by pref(ALLOWED_APPS_CATEGORY)
    private val notAllowedAppsCategory: PreferenceGroup by pref(NOT_ALLOWED_APPS)
    private val inactiveAppsCategory: PreferenceGroup by pref(INACTIVE_APPS)
    private val needUpdateAppsCategory: PreferenceGroup by pref(NEED_UPDATE_APPS)
    private val zeroStatePreference: ZeroStatePreference by pref(KEY_ZERO_STATE)
    private val thingsToTryCategory: PreferenceGroup by pref(THINGS_TO_TRY)
    private val settingsAndHelpCategory: PreferenceGroup by pref(SETTINGS_AND_HELP)
    private val bannerGroup: BannerMessagePreferenceGroup by pref(BANNER_GROUP)

    private fun createRemoveAllAppsAccessDialog(apps: List<ConnectedAppMetadata>) {
        val body =
            layoutInflater.inflate(
                if (SettingsThemeHelper.isExpressiveTheme(requireContext()))
                    R.layout.dialog_message_with_checkbox_expressive
                else R.layout.dialog_message_with_checkbox_legacy,
                null,
            )
        body.findViewById<TextView>(R.id.dialog_message).apply {
            text = getString(R.string.permissions_disconnect_all_dialog_message)
        }
        body.findViewById<TextView>(R.id.dialog_title).apply {
            text = getString(R.string.permissions_disconnect_all_dialog_title)
        }

        val imageIcon = body.findViewById(R.id.dialog_icon) as ImageView
        imageIcon.setImageDrawable(
            AttributeResolver.getNullableDrawable(requireContext(), R.attr.disconnectAllIcon)
        )
        imageIcon.visibility = View.VISIBLE

        val checkBox =
            body.findViewById<CheckBox>(R.id.dialog_checkbox).apply {
                text = getString(R.string.disconnect_all_app_permissions_dialog_checkbox)
            }

        removeAllAppsDialog =
            AlertDialogBuilder(
                    this,
                    DisconnectAllAppsDialogElement.DISCONNECT_ALL_APPS_DIALOG_CONTAINER,
                )
                .setView(body)
                .setCancelable(false)
                .setNeutralButton(
                    android.R.string.cancel,
                    DisconnectAllAppsDialogElement.DISCONNECT_ALL_APPS_DIALOG_CANCEL_BUTTON,
                ) { _, _ ->
                    viewModel.setAlertDialogStatus(false)
                    viewModel.setAlertDialogCheckBoxChecked(false)
                }
                .setPositiveButton(
                    R.string.permissions_disconnect_all_dialog_disconnect,
                    DisconnectAllAppsDialogElement.DISCONNECT_ALL_APPS_DIALOG_REMOVE_ALL_BUTTON,
                ) { _, _ ->
                    if (!viewModel.disconnectAllApps(apps)) {
                        Toast.makeText(requireContext(), R.string.default_error, Toast.LENGTH_SHORT)
                            .show()
                    }
                    if (checkBox.isChecked) {
                        viewModel.deleteAllData()
                    }
                }
                .create()
                .apply {
                    setOnShowListener {
                        checkBox.setOnCheckedChangeListener(null)
                        checkBox.isChecked = viewModel.alertDialogCheckBoxChecked.value ?: false
                        checkBox.setOnCheckedChangeListener { _, isChecked ->
                            viewModel.setAlertDialogCheckBoxChecked(isChecked)
                            logger.logInteraction(
                                DisconnectAllAppsDialogElement
                                    .DISCONNECT_ALL_APPS_DIALOG_DELETE_CHECKBOX
                            )
                        }
                    }
                }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.connected_apps_screen, rootKey)

        if (childFragmentManager.findFragmentByTag(FRAGMENT_TAG_DELETION) == null) {
            childFragmentManager.commitNow { add(DeletionFragment(), FRAGMENT_TAG_DELETION) }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadConnectedApps()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeConnectedApps()
        observeRevokeAllAppsPermissions()

        deletionViewModel.connectedAppsReloadNeeded.observe(viewLifecycleOwner) { isReloadNeeded ->
            if (isReloadNeeded) {
                viewModel.loadConnectedApps()
                deletionViewModel.resetPermissionTypesReloadNeeded()
            }
        }
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
            clearAllCategories()
            if (connectedApps.isEmpty()) {
                setupSharedMenu(viewLifecycleOwner, logger)
                setUpEmptyState()
            } else {
                setupMenu(R.menu.connected_apps, viewLifecycleOwner, logger) { menuItem ->
                    when (menuItem.itemId) {
                        R.id.menu_search -> {
                            searchMenuItem = menuItem
                            logger.logInteraction(AppPermissionsElement.SEARCH_BUTTON)
                            findNavController().navigate(R.id.action_connectedApps_to_searchApps)
                            true
                        }
                        else -> false
                    }
                }
                logger.logImpression(AppPermissionsElement.SEARCH_BUTTON)

                topIntroPreference.title = getString(R.string.connected_apps_text)
                zeroStatePreference.isVisible = false
                thingsToTryCategory.isVisible = false
                setAppAndSettingsCategoriesVisibility(true)

                val connectedAppsGroup = connectedApps.groupBy { it.status }
                val allowedApps = connectedAppsGroup[ALLOWED].orEmpty()
                val notAllowedApps = connectedAppsGroup[DENIED].orEmpty()
                val needUpdateApps = connectedAppsGroup[NEEDS_UPDATE].orEmpty()
                val activeApps: MutableList<ConnectedAppMetadata> = allowedApps.toMutableList()
                activeApps.addAll(notAllowedApps)
                createRemoveAllAppsAccessDialog(activeApps)

                settingsAndHelpCategory.addPreference(
                    getRemoveAccessForAllAppsPreference().apply {
                        isEnabled = allowedApps.isNotEmpty()
                        setOnPreferenceClickListener {
                            viewModel.setAlertDialogStatus(true)
                            true
                        }
                    }
                )

                if (
                    deviceInfoUtils.isPlayStoreAvailable(requireContext()) ||
                        deviceInfoUtils.isSendFeedbackAvailable(requireContext())
                ) {
                    settingsAndHelpCategory.addPreference(getHelpAndFeedbackPreference())
                }

                updateAllowedApps(allowedApps)
                updateDeniedApps(notAllowedApps)
                updateInactiveApps(connectedAppsGroup[INACTIVE].orEmpty())
                updateNeedUpdateApps(needUpdateApps)

                viewModel.alertDialogActive.observe(viewLifecycleOwner) { state ->
                    if (state) {
                        removeAllAppsDialog.show()
                    } else {
                        if (removeAllAppsDialog.isShowing) {
                            removeAllAppsDialog.dismiss()
                        }
                    }
                }
            }
        }
    }

    private fun updateInactiveApps(appsList: List<ConnectedAppMetadata>) {
        if (appsList.isEmpty()) {
            preferenceScreen.removePreference(inactiveAppsCategory)
        } else {
            appsList
                .sortedBy { it.appMetadata.appName }
                .forEach { app ->
                    val inactiveAppPreference =
                        InactiveAppPreference(requireContext()).also {
                            it.title = app.appMetadata.appName
                            it.icon = app.appMetadata.icon
                            it.logName = AppPermissionsElement.INACTIVE_APP_BUTTON
                            it.setOnDeleteButtonClickListener {
                                val packageName = app.appMetadata.packageName
                                val appName = app.appMetadata.appName
                                deleteData(packageName, appName)
                            }
                        }
                    inactiveAppsCategory.addPreference(inactiveAppPreference)
                }
        }
    }

    private fun deleteData(packageName: String, appName: String) {
        deletionViewModel.setDeletionType(DeleteAppData(packageName, appName))
        childFragmentManager.setFragmentResult(START_DELETION_KEY, bundleOf())
    }

    private fun updateNeedUpdateApps(appsList: List<ConnectedAppMetadata>) {
        if (appsList.isEmpty()) {
            needUpdateAppsCategory.isVisible = false
            return
        }

        needUpdateAppsCategory.isVisible = true
        appsList
            .sortedBy { it.appMetadata.appName }
            .forEach { app ->
                val intent = appStoreUtils.getAppStoreLink(app.appMetadata.packageName)
                if (intent == null) {
                    needUpdateAppsCategory.addPreference(
                        getAppPreference(app).also { it.isSelectable = false }
                    )
                } else {
                    needUpdateAppsCategory.addPreference(
                        getAppPreference(app) { navigationUtils.startActivity(this, intent) }
                    )
                }
            }

        val sharedPreference =
            requireActivity().getSharedPreferences(USER_ACTIVITY_TRACKER, Context.MODE_PRIVATE)
        val bannerSeen = sharedPreference.getBoolean(APP_UPDATE_NEEDED_BANNER_SEEN, false)

        if (!bannerSeen) {
            val banner = getAppUpdateNeededBanner(appsList)
            bannerGroup.removePreferenceRecursively(BANNER_PREFERENCE_KEY)
            bannerGroup.addPreference(banner)
        }
    }

    private fun updateAllowedApps(appsList: List<ConnectedAppMetadata>) {
        if (appsList.isEmpty()) {
            allowedAppsCategory.addPreference(getNoAppsPreference(R.string.no_apps_allowed))
        } else {
            appsList
                .sortedBy { it.appMetadata.appName }
                .forEach { app ->
                    allowedAppsCategory.addPreference(
                        getAppPreference(app) { navigateToAppInfoScreen(app) }
                    )
                }
        }
    }

    private fun updateDeniedApps(appsList: List<ConnectedAppMetadata>) {
        if (appsList.isEmpty()) {
            notAllowedAppsCategory.addPreference(getNoAppsPreference(R.string.no_apps_denied))
        } else {
            appsList
                .sortedBy { it.appMetadata.appName }
                .forEach { app ->
                    notAllowedAppsCategory.addPreference(
                        getAppPreference(app) { navigateToAppInfoScreen(app) }
                    )
                }
        }
    }

    private fun navigateToAppInfoScreen(app: ConnectedAppMetadata) {
        val navigationId =
            when (app.permissionsType) {
                AppPermissionsType.FITNESS_PERMISSIONS_ONLY ->
                    R.id.action_connectedApps_to_fitnessApp
                AppPermissionsType.MEDICAL_PERMISSIONS_ONLY ->
                    R.id.action_connectedApps_to_medicalApp
                AppPermissionsType.COMBINED_PERMISSIONS ->
                    R.id.action_connectedApps_to_combinedPermissions
            }
        findNavController()
            .navigate(
                navigationId,
                bundleOf(
                    EXTRA_PACKAGE_NAME to app.appMetadata.packageName,
                    EXTRA_APP_NAME to app.appMetadata.appName,
                ),
            )
    }

    private fun getNoAppsPreference(@StringRes res: Int): Preference {
        return Preference(requireContext()).also {
            it.setTitle(res)
            it.isSelectable = false
        }
    }

    private fun getAppPreference(
        app: ConnectedAppMetadata,
        onClick: (() -> Unit)? = null,
    ): HealthAppPreference {
        return HealthAppPreference(requireContext(), app.appMetadata).also {
            if (app.status == ALLOWED) {
                it.logName = AppPermissionsElement.CONNECTED_APP_BUTTON
            } else if (app.status == DENIED) {
                it.logName = AppPermissionsElement.NOT_CONNECTED_APP_BUTTON
            } else if (app.status == NEEDS_UPDATE) {
                it.logName = AppPermissionsElement.NEEDS_UPDATE_APP_BUTTON
            }
            it.setOnPreferenceClickListener {
                onClick?.invoke()
                true
            }
        }
    }

    private fun getRemoveAccessForAllAppsPreference(): HealthPreference {
        return HealthPreference(requireContext()).also {
            it.title = resources.getString(R.string.disconnect_all_apps)
            it.icon =
                AttributeResolver.getDrawable(requireContext(), R.attr.removeAccessForAllAppsIcon)
            it.logName = AppPermissionsElement.REMOVE_ALL_APPS_PERMISSIONS_BUTTON
        }
    }

    private fun getHelpAndFeedbackPreference(): HealthPreference {
        return HealthPreference(requireContext()).also {
            it.title = resources.getString(R.string.help_and_feedback)
            it.icon = AttributeResolver.getDrawable(requireContext(), R.attr.helpAndFeedbackIcon)
            it.logName = AppPermissionsElement.HELP_AND_FEEDBACK_BUTTON
            it.setOnPreferenceClickListener {
                findNavController().navigate(R.id.action_connectedApps_to_helpAndFeedback)
                true
            }
        }
    }

    private fun getCheckForUpdatesPreference(): HealthPreference {
        return HealthPreference(requireContext()).also {
            it.title = resources.getString(R.string.check_for_updates)
            it.icon = AttributeResolver.getDrawable(requireContext(), R.attr.checkForUpdatesIcon)
            it.summary = resources.getString(R.string.check_for_updates_description)
            it.logName = AppPermissionsElement.CHECK_FOR_UPDATES_BUTTON
            it.setOnPreferenceClickListener {
                findNavController().navigate(R.id.action_connected_apps_to_updated_apps)
                true
            }
        }
    }

    private fun getSeeAllCompatibleAppsPreference(): HealthPreference {
        return HealthPreference(requireContext()).also {
            it.title = resources.getString(R.string.see_all_compatible_apps)
            it.icon =
                AttributeResolver.getDrawable(requireContext(), R.attr.seeAllCompatibleAppsIcon)
            it.summary = resources.getString(R.string.see_all_compatible_apps_description)
            it.logName = AppPermissionsElement.SEE_ALL_COMPATIBLE_APPS_BUTTON
            it.setOnPreferenceClickListener {
                findNavController().navigate(R.id.action_connected_apps_to_play_store)
                true
            }
        }
    }

    private fun getSendFeedbackPreference(): Preference {
        return HealthPreference(requireContext()).also {
            it.title = resources.getString(R.string.send_feedback)
            it.icon = AttributeResolver.getDrawable(requireContext(), R.attr.sendFeedbackIcon)
            it.summary = resources.getString(R.string.send_feedback_description)
            it.logName = AppPermissionsElement.SEND_FEEDBACK_BUTTON
            it.setOnPreferenceClickListener {
                val intent = Intent(Intent.ACTION_BUG_REPORT)
                intent.putExtra("category_tag", APP_INTEGRATION_REQUEST_BUCKET_ID)
                activity?.startActivityForResult(intent, FEEDBACK_INTENT_RESULT_CODE)
                true
            }
        }
    }

    private fun getAppUpdateNeededBanner(
        appsList: List<ConnectedAppMetadata>
    ): HealthBannerPreference {
        return HealthBannerPreference(
                requireContext(),
                MigrationElement.MIGRATION_APP_UPDATE_BANNER,
            )
            .also { banner ->
                if (deviceInfoUtils.isPlayStoreAvailable(requireContext())) {
                    banner.setPositiveButton(
                        resources.getString(R.string.app_update_needed_banner_button),
                        MigrationElement.MIGRATION_APP_UPDATE_BUTTON,
                    ) {
                        findNavController().navigate(R.id.action_connected_apps_to_updated_apps)
                    }
                }

                banner.setNegativeButton(
                    resources.getString(R.string.app_update_needed_banner_learn_more_button),
                    MigrationElement.MIGRATION_APP_UPDATE_LEARN_MORE_BUTTON,
                ) {
                    deviceInfoUtils.openHCGetStartedLink(requireActivity())
                }
                banner.title = resources.getString(R.string.app_update_needed_banner_title)

                if (appsList.size > 1) {
                    banner.summary =
                        resources.getString(R.string.app_update_needed_banner_description_multiple)
                } else {
                    banner.summary =
                        resources.getString(
                            R.string.app_update_needed_banner_description_single,
                            appsList[0].appMetadata.appName,
                        )
                }

                banner.key = BANNER_PREFERENCE_KEY
                banner.setIcon(R.drawable.ic_apps_outage)

                banner.setDismissButton(
                    MigrationElement.MIGRATION_APP_UPDATE_BANNER_DISMISS_BUTTON
                ) {
                    val sharedPreference =
                        requireActivity()
                            .getSharedPreferences(USER_ACTIVITY_TRACKER, Context.MODE_PRIVATE)
                    sharedPreference.edit().apply {
                        putBoolean(APP_UPDATE_NEEDED_BANNER_SEEN, true)
                        apply()
                    }
                    bannerGroup.removePreference(banner)
                }
            }
    }

    private fun setUpEmptyState() {
        if (SettingsThemeHelper.isExpressiveTheme(requireContext())) {
            topIntroPreference.isVisible = false
            zeroStatePreference.isVisible = true
        } else {
            topIntroPreference.isVisible = true
            topIntroPreference.title = getString(R.string.connected_apps_empty_list_section_title)
            zeroStatePreference.isVisible = false
        }
        if (
            deviceInfoUtils.isPlayStoreAvailable(requireContext()) ||
                deviceInfoUtils.isSendFeedbackAvailable(requireContext())
        ) {
            thingsToTryCategory.isVisible = true
        }
        if (deviceInfoUtils.isPlayStoreAvailable(requireContext())) {
            thingsToTryCategory.addPreference(getCheckForUpdatesPreference())
            thingsToTryCategory.addPreference(getSeeAllCompatibleAppsPreference())
        }
        if (deviceInfoUtils.isSendFeedbackAvailable(requireContext())) {
            thingsToTryCategory.addPreference(getSendFeedbackPreference())
        }
        setAppAndSettingsCategoriesVisibility(false)
    }

    private fun setAppAndSettingsCategoriesVisibility(isVisible: Boolean) {
        inactiveAppsCategory.isVisible = isVisible
        allowedAppsCategory.isVisible = isVisible
        needUpdateAppsCategory.isVisible = isVisible
        notAllowedAppsCategory.isVisible = isVisible
        settingsAndHelpCategory.isVisible = isVisible
    }

    private fun clearAllCategories() {
        thingsToTryCategory.removeAll()
        allowedAppsCategory.removeAll()
        notAllowedAppsCategory.removeAll()
        needUpdateAppsCategory.removeAll()
        inactiveAppsCategory.removeAll()
        settingsAndHelpCategory.removeAll()
    }
}
