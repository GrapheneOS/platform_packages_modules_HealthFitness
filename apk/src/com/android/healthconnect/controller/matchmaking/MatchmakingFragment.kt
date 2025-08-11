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
import android.health.connect.HealthConnectManager.EXTRA_RECORD_TYPES
import android.health.connect.datatypes.Record
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.permissions.data.FitnessPermissionStrings
import com.android.healthconnect.controller.shared.HealthDataCategoryExtensions
import com.android.healthconnect.controller.shared.HealthDataCategoryExtensions.icon
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.shared.children
import com.android.healthconnect.controller.shared.preference.HealthExpandablePreference
import com.android.healthconnect.controller.shared.preference.HealthMainSwitchPreference
import com.android.healthconnect.controller.shared.preference.HealthSwitchPreference
import com.android.healthconnect.controller.utils.DeviceInfoUtils
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.PermissionsElement
import com.android.healthconnect.controller.utils.pref
import com.android.settingslib.widget.FooterPreference
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint(PreferenceFragmentCompat::class)
class MatchmakingFragment : Hilt_MatchmakingFragment() {

    companion object {
        private const val HEADER = "matchmaking_header"
        private const val ALLOW_ALL_PREFERENCE = "allow_all_preference"
        private const val MATCHMAKING_APPS_CATEGORY = "matchmaking_apps_category"
        private const val FOOTER = "matchmaking_footer"
    }

    private val viewModel: MatchmakingViewModel by activityViewModels()
    private var loadingIndicator: View? = null

    private val header: MatchmakingHeaderPreference by pref(HEADER)
    private val allowAllPreference: HealthMainSwitchPreference by pref(ALLOW_ALL_PREFERENCE)
    private val matchmakingAppsCategory: PreferenceCategory by pref(MATCHMAKING_APPS_CATEGORY)
    private val footer: FooterPreference by pref(FOOTER)

    @Inject lateinit var deviceInfoUtils: DeviceInfoUtils
    @Inject lateinit var healthPermissionReader: HealthPermissionReader
    @Inject lateinit var logger: HealthConnectLogger

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadingIndicator = activity?.findViewById(R.id.loading)

        viewModel.allPermissionsGranted.observe(viewLifecycleOwner) { allGranted ->
            allowAllPreference.isChecked = allGranted
        }

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

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.matchmaking_fragment, rootKey)
        preferenceScreen.isVisible = false

        val packageName = activity?.callingPackage
        val recordTypeNames = activity?.intent?.getStringArrayExtra(EXTRA_RECORD_TYPES)

        if (recordTypeNames == null) {
            activity?.apply {
                setResult(RESULT_CANCELED)
                finish()
            }
        }

        val recordTypes: Set<Class<out Record>> =
            recordTypeNames!!
                .map { Class.forName(it) }
                .filterIsInstance<Class<out Record>>()
                .toSet()

        if (packageName != null) {
            viewModel.loadMatchmakingApps(packageName, recordTypes)
        }

        allowAllPreference.setOnPreferenceChangeListener { _, newValue ->
            if (newValue as Boolean) {
                viewModel.addAllPermissionsToGrantedList()
            } else {
                viewModel.removeAllPermissionsFromGrantedList()
            }
            true
        }

        viewModel.matchmakingState.observe(this) { state ->
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
                    preferenceScreen.isVisible = true
                    bindHeader(state.appName)
                    buildAppList(state.apps)
                    bindFooter()
                }
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        loadingIndicator?.isVisible = isLoading
    }

    private fun bindHeader(appName: String?) {
        header.headerTitle = getString(R.string.matchmaking_screen_title)
        header.headerSummary = getString(R.string.matchmaking_screen_summary, appName)
    }

    private fun buildAppList(apps: List<MatchmakingAppData>) {
        matchmakingAppsCategory.removeAll()
        apps.forEach { appData -> addAppPreference(appData) }
    }

    private fun addAppPreference(appData: MatchmakingAppData) {
        val expandablePreference = createExpandablePreference(appData)
        matchmakingAppsCategory.addPreference(expandablePreference)

        if (appData.permissions.isNotEmpty()) {
            addPermissionSwitches(appData, expandablePreference)
            addPrivacyPolicyFooter(appData, expandablePreference)
        }
    }

    private fun createExpandablePreference(
        appData: MatchmakingAppData
    ): HealthExpandablePreference {
        return HealthExpandablePreference(requireContext(), null).apply {
            title =
                context.getString(
                    R.string.matchmaking_screen_data_from_app,
                    appData.metadata.appName,
                )
            icon = appData.metadata.icon
            key = appData.metadata.packageName
            setExpanded(
                viewModel.expandedPreferenceKeys.value?.contains(appData.metadata.packageName) ==
                    true
            )
            setOnExpandChangeListener { isExpanded ->
                viewModel.updateExpandedPreferenceKey(key, isExpanded)
            }
        }
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
    }

    private fun bindFooter() {
        footer.summary = getString(R.string.matchmaking_screen_footer)
        footer.setLearnMoreText(getString(R.string.more_about_health_connect))
        footer.setLearnMoreAction {
            deviceInfoUtils.openHealthFitnessPermissionsLearnMoreLink(requireActivity())
        }
    }

    private fun getPermissionKey(packageName: String, permission: String) =
        "${packageName}-${permission}"
}
