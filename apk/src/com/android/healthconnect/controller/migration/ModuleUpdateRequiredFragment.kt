/*
 * Copyright (C) 2023 The Android Open Source Project
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
package com.android.healthconnect.controller.migration

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.shared.Constants.MODULE_UPDATE_NEEDED_SEEN
import com.android.healthconnect.controller.shared.Constants.USER_ACTIVITY_TRACKER
import com.android.healthconnect.controller.shared.preference.HealthSetupFragment
import com.android.healthconnect.controller.shared.preference.HealthSetupHeaderPreference
import com.android.healthconnect.controller.utils.SettingsTransitionHelper.createMainlineServiceUpdateSettingsIntent
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.MigrationElement
import com.android.healthconnect.controller.utils.logging.PageName
import com.android.healthconnect.controller.utils.pref
import com.android.settingslib.widget.FooterPreference
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint(HealthSetupFragment::class)
class ModuleUpdateRequiredFragment : Hilt_ModuleUpdateRequiredFragment() {

    @Inject lateinit var logger: HealthConnectLogger

    companion object {
        private const val FRAGMENT_TAG = "ModuleUpdateRequiredFragment"
        private const val HEADER = "header_pref"
        private const val FOOTER = "footer_pref"
    }

    private val header: HealthSetupHeaderPreference by pref(HEADER)
    private val footer: FooterPreference by pref(FOOTER)

    init {
        this.setPageName(PageName.MIGRATION_MODULE_UPDATE_NEEDED_PAGE)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.migration_update_screen, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        header.setSummary(
            getString(
                R.string.migration_update_needed_header,
                getString(R.string.migration_module_update_needed_action),
            )
        )

        footer.isVisible = true

        val updateButton: Button = getPrimaryButtonFull()
        val cancelButton: Button = getSecondaryButton()

        updateButton.text = getString(R.string.update_button)
        cancelButton.text = getString(R.string.export_cancel_button)

        logger.logImpression(MigrationElement.MIGRATION_UPDATE_NEEDED_UPDATE_BUTTON)
        logger.logImpression(MigrationElement.MIGRATION_UPDATE_NEEDED_CANCEL_BUTTON)

        updateButton.setOnClickListener {
            logger.logInteraction(MigrationElement.MIGRATION_UPDATE_NEEDED_UPDATE_BUTTON)
            try {
                startActivity(requireContext().createMainlineServiceUpdateSettingsIntent())
            } catch (exception: Exception) {
                Log.e(FRAGMENT_TAG, "System update activity does not exist", exception)
                Toast.makeText(requireContext(), R.string.default_error, Toast.LENGTH_SHORT).show()
            }
        }

        cancelButton.setOnClickListener {
            logger.logInteraction(MigrationElement.MIGRATION_UPDATE_NEEDED_CANCEL_BUTTON)
            val sharedPreferences =
                requireActivity().getSharedPreferences(USER_ACTIVITY_TRACKER, Context.MODE_PRIVATE)
            val moduleUpdateSeen = sharedPreferences.getBoolean(MODULE_UPDATE_NEEDED_SEEN, false)

            if (!moduleUpdateSeen) {
                sharedPreferences.edit().apply {
                    putBoolean(MODULE_UPDATE_NEEDED_SEEN, true)
                    apply()
                }
                findNavController()
                    .navigate(R.id.action_migrationModuleUpdateNeededFragment_to_homeScreen)
            }

            requireActivity().finish()
        }
    }
}
