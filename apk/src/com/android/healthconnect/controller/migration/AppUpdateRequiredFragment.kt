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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.utils.getAppStoreLink
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.MigrationElement
import com.android.healthconnect.controller.utils.logging.PageName
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint(Fragment::class)
class AppUpdateRequiredFragment : Hilt_AppUpdateRequiredFragment() {

    @Inject lateinit var logger: HealthConnectLogger

    companion object {
        private const val TAG = "AppUpdateFragment"
        private const val HC_PACKAGE_NAME = "com.google.apps.healthdata"
    }

    private lateinit var onBackPressedCallback: OnBackPressedCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedCallback =
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    findNavController()
                        .navigate(R.id.action_migrationAppUpdateNeededFragment_to_homeScreen)
                    requireActivity().finish()
                }
            }
        requireActivity().onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        logger.setPageId(PageName.MIGRATION_APP_UPDATE_NEEDED_PAGE)
        return inflater.inflate(R.layout.migration_app_update_needed, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val updateButton = view.findViewById<Button>(R.id.update_button)
        val cancelButton = view.findViewById<Button>(R.id.cancel_button)
        logger.logImpression(MigrationElement.MIGRATION_UPDATE_NEEDED_UPDATE_BUTTON)
        logger.logImpression(MigrationElement.MIGRATION_UPDATE_NEEDED_CANCEL_BUTTON)

        updateButton.setOnClickListener {
            logger.logInteraction(MigrationElement.MIGRATION_UPDATE_NEEDED_UPDATE_BUTTON)
            try {
                val intent = getAppStoreLink(requireContext(), HC_PACKAGE_NAME)
                startActivity(intent!!)
            } catch (exception: Exception) {
                Log.e(TAG, "App store activity does not exist", exception)
                Toast.makeText(requireContext(), R.string.default_error, Toast.LENGTH_SHORT).show()
            }
        }

        cancelButton.setOnClickListener {
            logger.logInteraction(MigrationElement.MIGRATION_UPDATE_NEEDED_CANCEL_BUTTON)
            val sharedPreferences =
                requireActivity()
                    .getSharedPreferences("USER_ACTIVITY_TRACKER", Context.MODE_PRIVATE)
            val appUpdateSeen =
                sharedPreferences.getBoolean(getString(R.string.app_update_needed_seen), false)
            if (!appUpdateSeen) {
                sharedPreferences.edit().apply {
                    putBoolean(getString(R.string.app_update_needed_seen), true)
                    apply()
                }
                findNavController()
                    .navigate(R.id.action_migrationAppUpdateNeededFragment_to_homeScreen)
            }
            requireActivity().finish()
        }
    }

    override fun onResume() {
        super.onResume()
        logger.logPageImpression()
    }
}
