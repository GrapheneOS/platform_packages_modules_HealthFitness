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

package com.android.healthconnect.controller.matchmaking

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.health.connect.HealthConnectManager.EXTRA_RECORD_TYPES
import android.health.connect.datatypes.Record
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceGroupAdapter
import androidx.preference.PreferenceScreen
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.permissions.data.FitnessPermissionStrings
import com.android.healthconnect.controller.shared.HealthDataCategoryExtensions
import com.android.healthconnect.controller.shared.HealthDataCategoryExtensions.icon
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.shared.children
import com.android.healthconnect.controller.shared.preference.ExpandablePreferenceAdapter
import com.android.healthconnect.controller.shared.preference.HealthExpandablePreference
import com.android.healthconnect.controller.shared.preference.HealthMainSwitchPreference
import com.android.healthconnect.controller.shared.preference.HealthPreferenceFragment
import com.android.healthconnect.controller.shared.preference.HealthSwitchPreference
import com.android.healthconnect.controller.utils.DeviceInfoUtils
import com.android.healthconnect.controller.utils.increaseViewTouchTargetSize
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.MatchmakingElement
import com.android.healthconnect.controller.utils.logging.PageName
import com.android.healthconnect.controller.utils.logging.PermissionsElement
import com.android.healthconnect.controller.utils.pref
import com.android.settingslib.widget.FooterPreference
import com.android.settingslib.widget.SettingsThemeHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * A fragment shown to the user to allow them to grant permissions to multiple apps at once, based
 * on a specific record type.
 */
@AndroidEntryPoint(HealthPreferenceFragment::class)
class MatchmakingFragment : Hilt_MatchmakingFragment() {

    companion object {
        private const val HEADER = "matchmaking_header"
        private const val ALLOW_ALL_PREFERENCE = "allow_all_preference"
        private const val MATCHMAKING_APPS_CATEGORY = "matchmaking_apps_category"
        private const val FOOTER = "matchmaking_footer"
        private const val TAG = "Matchmaking"
    }

    private val viewModel: MatchmakingViewModel by activityViewModels()
    private var loadingIndicator: View? = null

    private val header: MatchmakingHeaderPreference by pref(HEADER)
    private val allowAllPreference: HealthMainSwitchPreference by pref(ALLOW_ALL_PREFERENCE)
    private val matchmakingAppsCategory: PreferenceCategory by pref(MATCHMAKING_APPS_CATEGORY)
    private val footerPref: FooterPreference by pref(FOOTER)

    @Inject lateinit var deviceInfoUtils: DeviceInfoUtils
    @Inject lateinit var healthPermissionReader: HealthPermissionReader
    @Inject lateinit var logger: HealthConnectLogger

    private val customStyledPrefs = mutableListOf<Preference>()

    init {
        this.setPageName(PageName.MATCHMAKING_PAGE)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val rootView = inflater.inflate(R.layout.fragment_setup, container, false)
        rootView.background =
            ContextCompat.getDrawable(requireContext(), R.drawable.rounded_background_transparent)
        val buttonLayoutId =
            if (SettingsThemeHelper.isExpressiveTheme(requireContext())) {
                R.layout.widget_setup_bottom_button_bar_expressive
            } else {
                R.layout.widget_setup_bottom_button_bar_legacy
            }

        val buttonArea = rootView.findViewById<FrameLayout>(R.id.action_container)
        val buttons = inflater.inflate(buttonLayoutId, buttonArea, false)
        buttonArea.addView(buttons)

        val preferenceArea = rootView.findViewById<ViewGroup>(R.id.preference_container)
        val preferenceView = super.onCreateView(inflater, container, savedInstanceState)
        preferenceArea.addView(preferenceView)

        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadingIndicator = activity?.findViewById(R.id.loading)

        viewModel.matchmakingState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is MatchmakingViewModel.MatchmakingState.Loading -> {
                    setLoading(true)
                }
                is MatchmakingViewModel.MatchmakingState.LoadingFailed -> {
                    setLoading(false)
                    activity?.finish()
                }
                is MatchmakingViewModel.MatchmakingState.WithData -> {
                    setLoading(false)
                    bindHeader(state.callingAppMetaData, state.matchingApps)
                    buildAppList(state.matchingApps)
                    bindFooter()
                    setupButtons(view)
                }
            }
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.matchmaking_fragment, rootKey)

        allowAllPreference.logNameActive = PermissionsElement.ALLOW_ALL_SWITCH
        allowAllPreference.logNameInactive = PermissionsElement.ALLOW_ALL_SWITCH
        val packageName = activity?.callingPackage
        val recordTypeNames = activity?.intent?.getStringArrayExtra(EXTRA_RECORD_TYPES)

        if (packageName == null) {
            Log.i(
                TAG,
                "Calling package is null. Make sure you are using registerForActivityResult() to launch the Matchmaking intent.",
            )
            activity?.apply {
                setResult(RESULT_CANCELED)
                finish()
            }
            return
        }

        val recordTypes = parseRecordTypeNames(recordTypeNames)
        viewModel.loadMatchmakingApps(packageName, recordTypes)
    }

    override fun onCreateAdapter(preferenceScreen: PreferenceScreen): PreferenceGroupAdapter {
        return ExpandablePreferenceAdapter(preferenceScreen, customStyledPrefs)
    }

    private fun setLoading(isLoading: Boolean) {
        loadingIndicator?.isVisible = isLoading
    }

    private fun bindHeader(
        callingAppMetaData: AppMetadata,
        matchingApps: List<MatchmakingAppData>,
    ) {
        if (matchingApps.isEmpty()) {
            header.isIconViewVisible = false
        }
        logger.logImpression(MatchmakingElement.MATCHMAKING_SCREEN_HEADER)
        logger.logImpression(MatchmakingElement.MATCHMAKING_SCREEN_HEADER_ICON_VIEW)
        header.headerTitle = getString(R.string.matchmaking_screen_title)
        header.headerSummary =
            getString(R.string.matchmaking_screen_summary, callingAppMetaData.appName)
        header.requestingAppIcon = callingAppMetaData.icon
        header.matchedAppIcons = matchingApps.mapNotNull { it.metadata.icon }
    }

    private fun buildAppList(apps: List<MatchmakingAppData>) {
        customStyledPrefs.clear()
        matchmakingAppsCategory.removeAll()
        apps.forEach { appData -> addAppPreference(appData, apps.size) }
    }

    private fun addAppPreference(appData: MatchmakingAppData, appListSize: Int) {
        val expandablePreference = createExpandablePreference(appData, appListSize)
        matchmakingAppsCategory.addPreference(expandablePreference)
        customStyledPrefs.add(expandablePreference)

        if (appData.permissions.isNotEmpty()) {
            addPermissionSwitches(appData, expandablePreference)
            addPrivacyPolicyFooter(appData, expandablePreference)
        }

        viewModel.grantedPermissions.observe(viewLifecycleOwner) { grantedPermissionsMap ->
            val granted = grantedPermissionsMap[appData.metadata.packageName]?.size ?: 0
            val total = appData.permissions.size
            expandablePreference.summary =
                requireContext().getString(R.string.app_permissions_granted_summary, granted, total)
        }
    }

    private fun createExpandablePreference(
        appData: MatchmakingAppData,
        appListSize: Int,
    ): HealthExpandablePreference {
        return HealthExpandablePreference(requireContext(), null).apply {
            title =
                context.getString(
                    R.string.matchmaking_screen_data_from_app,
                    appData.metadata.appName,
                )
            icon = appData.metadata.icon
            key = appData.metadata.packageName
            logName = MatchmakingElement.MATCHMAKING_EXPANDABLE_PREFERENCE
            setExpanded(isInitiallyExpanded(appData, appListSize))
            setOnExpandChangeListener { isExpanded ->
                viewModel.updateExpandedPreferenceKey(key, isExpanded)
            }
        }
    }

    private fun isInitiallyExpanded(appData: MatchmakingAppData, appListSize: Int): Boolean {
        return appListSize == 1 ||
            viewModel.expandedPreferenceKeys.value?.contains(appData.metadata.packageName) == true
    }

    private fun addPermissionSwitches(
        appData: MatchmakingAppData,
        healthExpandablePreference: HealthExpandablePreference,
    ) {
        appData.permissions.forEach { permission ->
            val switch =
                HealthSwitchPreference(requireContext()).also {
                    val healthCategory =
                        HealthDataCategoryExtensions.fromFitnessPermissionType(
                            permission.fitnessPermissionType
                        )
                    it.icon = healthCategory.icon(requireContext())
                    it.setTitle(
                        FitnessPermissionStrings.fromPermissionType(
                                permission.fitnessPermissionType
                            )
                            .uppercaseLabel
                    )
                    it.key = getPermissionKey(appData.metadata.packageName, permission.toString())
                    it.logNameActive = PermissionsElement.PERMISSION_SWITCH
                    it.logNameInactive = PermissionsElement.PERMISSION_SWITCH
                    it.permission = permission
                    it.setOnPreferenceChangeListener { _, newValue ->
                        if (newValue as? Boolean == true) {
                            viewModel.addPermissionToGrantedList(
                                appData.metadata.packageName,
                                permission,
                            )
                        } else {
                            viewModel.removePermissionFromGrantedList(
                                appData.metadata.packageName,
                                permission,
                            )
                        }
                        true
                    }
                }
            healthExpandablePreference.addPreference(switch)
            customStyledPrefs.add(switch)
        }
    }

    private fun addPrivacyPolicyFooter(
        appData: MatchmakingAppData,
        healthExpandablePreference: HealthExpandablePreference,
    ) {
        val privacyFooter =
            MatchmakingPrivacyFooterPreference(requireContext()).apply {
                setAppName(appData.metadata.appName) {
                    val startRationaleIntent =
                        healthPermissionReader.getApplicationRationaleIntent(
                            appData.metadata.packageName
                        )
                    logger.logInteraction(PermissionsElement.APP_RATIONALE_LINK)
                    startActivity(startRationaleIntent)
                }
            }
        healthExpandablePreference.addPreference(privacyFooter)
        customStyledPrefs.add(privacyFooter)
    }

    private fun bindFooter() {
        logger.logImpression(MatchmakingElement.MATCHMAKING_SCREEN_FOOTER)
        footerPref.summary = getString(R.string.matchmaking_screen_footer)
        footerPref.setLearnMoreText(getString(R.string.more_about_health_connect))
        logger.logImpression(MatchmakingElement.MATCHMAKING_SCREEN_FOOTER_LINK)
        footerPref.setLearnMoreAction {
            logger.logInteraction(MatchmakingElement.MATCHMAKING_SCREEN_FOOTER_LINK)
            deviceInfoUtils.openHealthFitnessPermissionsLearnMoreLink(requireActivity())
        }
    }

    private fun getPermissionKey(packageName: String, permission: String) =
        "${packageName}-${permission}"

    private fun setupButtons(view: View) {
        setupAllowAll()
        setupActionButtons(view)

        viewModel.grantedPermissions.observe(viewLifecycleOwner) { grantedPermissionsMap ->
            matchmakingAppsCategory.children.forEach { preference ->
                if (preference is HealthExpandablePreference) {
                    val packageName = preference.key
                    val grantedPermissions = grantedPermissionsMap[packageName] ?: emptySet()
                    preference.children.forEach { child ->
                        if (child is HealthSwitchPreference) {
                            child.isChecked =
                                grantedPermissions.any {
                                    getPermissionKey(packageName, it.toString()) == child.key
                                }
                        }
                    }
                }
            }
        }
    }

    private fun setupAllowAll() {
        val onChecked = suspend {
            toggleAllMatchmakingPermissions(true)
            true
        }
        val onUnchecked = suspend {
            toggleAllMatchmakingPermissions(false)
            true
        }

        allowAllPreference.setUpStateManagement(
            viewLifecycleOwner,
            viewModel.allPermissionsGranted,
            onChecked,
            onUnchecked,
        )
    }

    private fun toggleAllMatchmakingPermissions(isChecked: Boolean) {
        matchmakingAppsCategory.children.forEach { preference ->
            if (preference is HealthExpandablePreference) {
                preference.children.forEach { child ->
                    if (child is HealthSwitchPreference) {
                        child.isChecked = isChecked
                    }
                }
            }
        }
        if (isChecked) {
            viewModel.addAllPermissionsToGrantedList()
        } else {
            viewModel.removeAllPermissionsFromGrantedList()
        }
        // Notify the adapter that the data has changed to force a redraw of the visible items.
        // This is crucial because PreferenceFragmentCompat uses a RecyclerView.
        (listView.adapter as? PreferenceGroupAdapter)?.notifyDataSetChanged()
    }

    private fun setupActionButtons(view: View) {
        val allowButton = view.findViewById<Button>(R.id.primary_button_outline)
        val allowButtonFull = view.findViewById<Button>(R.id.primary_button_full)
        val dontAllowButton = view.findViewById<Button>(R.id.secondary_button)
        logger.logImpression(PermissionsElement.ALLOW_PERMISSIONS_BUTTON)
        logger.logImpression(PermissionsElement.CANCEL_PERMISSIONS_BUTTON)

        allowButtonFull.visibility = View.GONE
        allowButton.visibility = View.VISIBLE

        allowButton.setText(R.string.request_permissions_allow)
        dontAllowButton.setText(R.string.request_permissions_dont_allow)

        val allowParentView = allowButton.parent.parent as View
        increaseViewTouchTargetSize(requireContext(), allowButton, allowParentView)

        val dontAllowParentView = dontAllowButton.parent as View
        increaseViewTouchTargetSize(requireContext(), dontAllowButton, dontAllowParentView)

        allowButton.setOnClickListener {
            logger.logInteraction(PermissionsElement.ALLOW_PERMISSIONS_BUTTON)
            viewModel.grantPermissions()
            activity?.setResult(RESULT_OK)
            activity?.finish()
        }

        dontAllowButton.setOnClickListener {
            logger.logInteraction(PermissionsElement.CANCEL_PERMISSIONS_BUTTON)
            viewModel.recordMatchmakingDenial()
            viewModel.removeAllPermissionsFromGrantedList()
            activity?.setResult(RESULT_CANCELED)
            activity?.finish()
        }

        viewModel.atLeastOnePermissionGranted.observe(viewLifecycleOwner) { isEnabled ->
            allowButton.isEnabled = isEnabled
        }
    }

    /**
     * Parses an array of record type names into a set of `Class<out Record>`.
     *
     * @param recordTypeNames An array of class names for `Record` types, or null.
     * @return A set of `Class<out Record>` corresponding to the valid record type names, filtering
     *   out invalid names, or an empty set if `recordTypeNames` is null or empty.
     */
    private fun parseRecordTypeNames(recordTypeNames: Array<String>?): Set<Class<out Record>> {
        return (recordTypeNames ?: emptyArray())
            .mapNotNull {
                try {
                    Class.forName(it)
                } catch (e: ClassNotFoundException) {
                    null
                }
            }
            .filterIsInstance<Class<out Record>>()
            .toSet()
    }
}
