/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */

package com.android.healthconnect.controller.permissions.additionalaccess

import android.content.Intent.EXTRA_PACKAGE_NAME
import android.health.connect.HealthPermissions
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.permissions.additionalaccess.api.PermissionUiState
import com.android.healthconnect.controller.permissions.additionalaccess.api.PermissionUiState.ALWAYS_ALLOW
import com.android.healthconnect.controller.permissions.additionalaccess.api.PermissionUiState.ASK_EVERY_TIME
import com.android.healthconnect.controller.permissions.additionalaccess.api.PermissionUiState.NOT_DECLARED
import com.android.healthconnect.controller.permissions.app.AppPermissionViewModel
import com.android.healthconnect.controller.permissions.data.AccessType
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.permissions.data.additionalPermissionString
import com.android.healthconnect.controller.shared.Constants.EXTRA_APP_NAME
import com.android.healthconnect.controller.shared.Constants.SHOW_MANAGE_APP_SECTION
import com.android.healthconnect.controller.shared.preference.HealthPreference
import com.android.healthconnect.controller.shared.preference.HealthPreferenceFragment
import com.android.healthconnect.controller.shared.preference.HealthSwitchPreference
import com.android.healthconnect.controller.shared.preference.WarningPreference
import com.android.healthconnect.controller.shared.preference.addIntroOrAppHeaderPreference
import com.android.healthconnect.controller.utils.LocalDateTimeFormatter
import com.android.healthconnect.controller.utils.logging.AdditionalAccessElement.BACKGROUND_READ_BUTTON
import com.android.healthconnect.controller.utils.logging.AdditionalAccessElement.EXERCISE_ROUTES_BUTTON
import com.android.healthconnect.controller.utils.logging.AdditionalAccessElement.HISTORY_READ_BUTTON
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.PageName
import com.android.healthconnect.controller.utils.navigateSafe
import com.android.healthconnect.controller.utils.pref
import com.android.settingslib.widget.FooterPreference
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Fragment that contains additional app permission access. */
@AndroidEntryPoint(HealthPreferenceFragment::class)
class AdditionalAccessFragment : Hilt_AdditionalAccessFragment() {

    @Inject lateinit var healthConnectLogger: HealthConnectLogger
    private val permissionsViewModel: AppPermissionViewModel by activityViewModels()
    private val viewModel: AdditionalAccessViewModel by activityViewModels()

    private val exerciseRoutePref: HealthPreference by pref(KEY_EXERCISE_ROUTES_PERMISSION)
    private val historicReadPref: HealthSwitchPreference by pref(KEY_HISTORY_READ_PERMISSION)
    private val backgroundReadPref: HealthSwitchPreference by pref(KEY_BACKGROUND_READ_PERMISSION)
    private val footerPref: FooterPreference by pref(KEY_FOOTER)
    private val warningPref: WarningPreference by pref(KEY_WARNING)

    private val dateFormatter by lazy { LocalDateTimeFormatter(requireContext()) }

    lateinit var packageName: String

    init {
        setPageName(PageName.ADDITIONAL_ACCESS_PAGE)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        setPreferencesFromResource(R.xml.additional_access_screen, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val packageNameExtra = requireArguments().getString(EXTRA_PACKAGE_NAME)
        if (packageNameExtra.isNullOrEmpty()) {
            Log.e(TAG, "AdditionalAccessFragment is missing $EXTRA_PACKAGE_NAME intent!")
            requireActivity().finish()
            return
        }
        packageName = packageNameExtra

        viewModel.showEnableExerciseEvent.observe(viewLifecycleOwner) { state ->
            if (state.shouldShowDialog) {
                EnableExercisePermissionDialog.createDialog(packageName, state.appName)
                    .show(childFragmentManager, ENABLE_EXERCISE_DIALOG_TAG)
            }
        }

        viewModel.screenState.observe(viewLifecycleOwner) { screenState ->
            setupAdditionalPrefs(screenState)
            maybeShowFooter(screenState.state, screenState.showMedicalPastDataFooter)
        }

        permissionsViewModel.appInfo.observe(viewLifecycleOwner) { appMetadata ->
            addIntroOrAppHeaderPreference(preferenceScreen, requireContext(), appMetadata)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadAdditionalAccessPreferences(packageName)
    }

    private fun setupExerciseRoutePref(state: PermissionUiState) {
        exerciseRoutePref.isVisible = state != NOT_DECLARED
        if (state == NOT_DECLARED) {
            return
        }

        healthConnectLogger.logImpression(EXERCISE_ROUTES_BUTTON)
        exerciseRoutePref.apply {
            logName = EXERCISE_ROUTES_BUTTON
            exerciseRoutePref.setSummary(
                when (state) {
                    ASK_EVERY_TIME -> R.string.route_permissions_ask
                    ALWAYS_ALLOW -> R.string.route_permissions_always_allow
                    else -> R.string.route_permissions_deny
                }
            )
            exerciseRoutePref.setOnPreferenceClickListener {
                val dialog = ExerciseRoutesPermissionDialogFragment.createDialog(packageName)
                dialog.show(childFragmentManager, EXERCISE_ROUTES_DIALOG_TAG)
                true
            }
        }
    }

    private fun maybeShowFooter(
        state: AdditionalAccessViewModel.State,
        showMedicalPastDataFooter: Boolean,
    ) {
        // We show a different message when medical read is on and history read is available
        // to let the user know that medical data is already read from the past
        val shouldShow = state.showEnableReadFooter() || showMedicalPastDataFooter

        if (!shouldShow) {
            footerPref.isVisible = false
            return
        }

        if (showMedicalPastDataFooter) {
            footerPref.isVisible = true
            val appName = permissionsViewModel.appInfo.value!!.appName
            footerPref.title = getString(R.string.additional_access_medical_read_footer, appName)
            footerPref.setLearnMoreText(
                getString(R.string.additional_access_medical_read_footer_link)
            )
            footerPref.setLearnMoreAction {
                findNavController()
                    .navigateSafe(
                        R.id.additionalAccessFragment,
                        R.id.action_additionalAccess_to_medicalApp,
                        Bundle().apply {
                            putString(EXTRA_PACKAGE_NAME, packageName)
                            putString(EXTRA_APP_NAME, appName)
                            putBoolean(SHOW_MANAGE_APP_SECTION, false)
                        },
                    )
            }
            return
        }

        val title =
            if (
                state.isAdditionalPermissionDisabled(state.historyReadUIState) &&
                    state.isAdditionalPermissionDisabled(state.backgroundReadUIState)
            ) {
                R.string.additional_access_combined_footer
            } else if (state.isAdditionalPermissionDisabled(state.backgroundReadUIState)) {
                R.string.additional_access_background_footer
            } else {
                R.string.additional_access_history_footer
            }

        footerPref.title = getString(title)
        footerPref.isVisible = true
    }

    private fun setupAdditionalPrefs(screenState: AdditionalAccessViewModel.ScreenState) {
        setupExerciseRoutePref(screenState.state.exerciseRoutePermissionUIState)

        if (screenState.state.historyReadUIState.isDeclared) {
            val appName = permissionsViewModel.appInfo.value!!.appName

            val historyReadStrings =
                additionalPermissionString(
                    HealthPermission.AdditionalPermission.READ_HEALTH_DATA_HISTORY,
                    type = AccessType.ACCESS,
                    hasMedicalPermissions = screenState.appHasDeclaredMedicalPermissions,
                    isMedicalReadGranted = false, // doesn't matter here
                    isFitnessReadGranted = false, // doesn't matter here
                )

            val dataAccessDate = viewModel.loadAccessDate(packageName)
            val summary =
                if (dataAccessDate != null) {
                    val formattedDate = dateFormatter.formatLongDate(dataAccessDate)
                    getString(historyReadStrings.description, formattedDate)
                } else {
                    getString(historyReadStrings.descriptionFallback)
                }

            historicReadPref.title = getString(historyReadStrings.title)
            historicReadPref.isVisible = true
            healthConnectLogger.logImpression(HISTORY_READ_BUTTON)
            historicReadPref.isChecked = screenState.state.historyReadUIState.isGranted
            historicReadPref.isEnabled = screenState.state.historyReadUIState.isEnabled
            historicReadPref.summary = summary
            historicReadPref.logNameActive = HISTORY_READ_BUTTON
            historicReadPref.logNameInactive = HISTORY_READ_BUTTON
            historicReadPref.setOnPreferenceChangeListener { _, isGranted ->
                viewModel.updatePermission(
                    packageName,
                    HealthPermissions.READ_HEALTH_DATA_HISTORY,
                    isGranted as Boolean,
                )

                updateWarningPreference(
                    appName,
                    screenState.state.historyReadUIState.isEnabled,
                    isGranted,
                    screenState.appHasGrantedFitnessReadPermission,
                )
                true
            }

            updateWarningPreference(
                appName,
                screenState.state.historyReadUIState.isEnabled,
                screenState.state.historyReadUIState.isGranted,
                screenState.appHasGrantedFitnessReadPermission,
            )
        }

        if (screenState.state.backgroundReadUIState.isDeclared) {
            val backgroundReadString =
                additionalPermissionString(
                    HealthPermission.AdditionalPermission.READ_HEALTH_DATA_IN_BACKGROUND,
                    type = AccessType.ACCESS,
                    hasMedicalPermissions = screenState.appHasDeclaredMedicalPermissions,
                    isMedicalReadGranted = screenState.showMedicalPastDataFooter,
                    isFitnessReadGranted = screenState.appHasGrantedFitnessReadPermission,
                )

            backgroundReadPref.title = getString(backgroundReadString.title)
            backgroundReadPref.summary = getString(backgroundReadString.description)
            backgroundReadPref.isVisible = true
            healthConnectLogger.logImpression(BACKGROUND_READ_BUTTON)
            backgroundReadPref.isChecked = screenState.state.backgroundReadUIState.isGranted
            backgroundReadPref.isEnabled = screenState.state.backgroundReadUIState.isEnabled
            historicReadPref.logNameActive = BACKGROUND_READ_BUTTON
            historicReadPref.logNameInactive = BACKGROUND_READ_BUTTON
            backgroundReadPref.setOnPreferenceChangeListener { _, newValue ->
                viewModel.updatePermission(
                    packageName,
                    HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND,
                    newValue as Boolean,
                )
                true
            }
        }
    }

    private fun updateWarningPreference(
        appName: String,
        isEnabled: Boolean,
        isGranted: Boolean,
        anyFitnessReadPermissionGranted: Boolean,
    ) {
        warningPref.setTitle(getString(R.string.history_read_medical_access_read_warning, appName))
        warningPref.isVisible = isGranted && isEnabled && !anyFitnessReadPermissionGranted
    }

    companion object {
        private const val TAG = "AdditionalAccessFragmen"
        private const val KEY_EXERCISE_ROUTES_PERMISSION = "key_exercise_routes_permission"
        private const val EXERCISE_ROUTES_DIALOG_TAG = "ExerciseRoutesPermissionDialogFragment"
        private const val ENABLE_EXERCISE_DIALOG_TAG = "EnableExercisePermissionDialog"
        private const val KEY_BACKGROUND_READ_PERMISSION = "key_background_read"
        private const val KEY_HISTORY_READ_PERMISSION = "key_history_read"
        private const val KEY_WARNING = "key_warning"
        private const val KEY_FOOTER = "key_additional_access_footer"
    }
}
