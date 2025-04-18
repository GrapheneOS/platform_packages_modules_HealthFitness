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
import com.android.healthconnect.controller.shared.Constants.INTEGRATION_PAUSED_SEEN_KEY
import com.android.healthconnect.controller.shared.Constants.USER_ACTIVITY_TRACKER
import com.android.healthconnect.controller.shared.preference.HealthSetupFragment
import com.android.healthconnect.controller.shared.preference.HealthSetupHeaderPreference
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.MigrationElement
import com.android.healthconnect.controller.utils.logging.PageName
import com.android.healthconnect.controller.utils.pref
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint(HealthSetupFragment::class)
class MigrationPausedFragment : Hilt_MigrationPausedFragment() {

    @Inject lateinit var logger: HealthConnectLogger
    private val header: HealthSetupHeaderPreference by pref(HEADER)

    companion object {
        private const val TAG = "MigrationPausedFragment"
        private const val HEADER = "header_pref"
    }

    init {
        this.setPageName(PageName.MIGRATION_PAUSED_PAGE)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.migration_update_screen, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        header.setTitle(getString(R.string.migration_paused_screen_title))
        header.setSummary(getString(R.string.migration_paused_screen_details))

        val resumeButton: Button = getPrimaryButtonFull()
        val cancelButton: Button = getSecondaryButton()

        logger.logImpression(MigrationElement.MIGRATION_PAUSED_CONTINUE_BUTTON)
        logger.logImpression(MigrationElement.MIGRATION_UPDATE_NEEDED_CANCEL_BUTTON)

        resumeButton.text = getString(R.string.resume_button)
        cancelButton.text = getString(R.string.export_cancel_button)

        resumeButton.setOnClickListener {
            logger.logInteraction(MigrationElement.MIGRATION_PAUSED_CONTINUE_BUTTON)
            try {
                findNavController().navigate(R.id.action_migrationPausedFragment_to_migrationApk)
            } catch (exception: Exception) {
                Log.e(TAG, "Migration APK does not exist", exception)
                Toast.makeText(requireContext(), R.string.default_error, Toast.LENGTH_SHORT).show()
            }
        }

        cancelButton.setOnClickListener {
            logger.logInteraction(MigrationElement.MIGRATION_UPDATE_NEEDED_CANCEL_BUTTON)
            val sharedPreferences =
                requireActivity().getSharedPreferences(USER_ACTIVITY_TRACKER, Context.MODE_PRIVATE)
            val integrationPausedSeen =
                sharedPreferences.getBoolean(INTEGRATION_PAUSED_SEEN_KEY, false)
            if (!integrationPausedSeen) {
                sharedPreferences.edit().apply {
                    putBoolean(INTEGRATION_PAUSED_SEEN_KEY, true)
                    apply()
                }
                findNavController().navigate(R.id.action_migrationPausedFragment_to_homeScreen)
            }
            requireActivity().finish()
        }
    }
}
