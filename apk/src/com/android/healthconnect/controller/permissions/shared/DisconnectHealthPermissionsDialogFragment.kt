/**
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.permissions.shared

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.permissions.app.AppPermissionViewModel
import com.android.healthconnect.controller.shared.dialog.AlertDialogBuilder
import com.android.healthconnect.controller.utils.AttributeResolver
import com.android.healthconnect.controller.utils.logging.DisconnectAppDialogElement
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.settingslib.widget.SettingsThemeHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * A Dialog Fragment to get confirmation from user for revoking all fitness, medical, or health
 * permissions of an app.
 */
@AndroidEntryPoint(DialogFragment::class)
class DisconnectHealthPermissionsDialogFragment() :
    Hilt_DisconnectHealthPermissionsDialogFragment() {

    private val appPermissionViewModel: AppPermissionViewModel by activityViewModels()

    constructor(
        appName: String,
        enableDeleteData: Boolean = true,
        disconnectType: DisconnectType = DisconnectType.ALL,
    ) : this() {
        this.appName = appName
        this.enableDeleteData = enableDeleteData
        this.disconnectType = disconnectType
    }

    companion object {
        const val TAG = "DisconnectHealthPermissionsDialogFragment"
        const val DISCONNECT_CANCELED_EVENT = "DISCONNECT_CANCELED_EVENT"
        const val DISCONNECT_ALL_EVENT = "DISCONNECT_ALL_EVENT"
        const val KEY_DELETE_DATA = "KEY_DELETE_DATA"
        const val KEY_APP_NAME = "KEY_APP_NAME"
        const val KEY_ENABLE_DELETE_DATA = "KEY_ENABLE_DELETE_DATA"
        const val KEY_INCLUDE_BACKGROUND_READ = "KEY_INCLUDE_BACKGROUND_READ"
        const val KEY_INCLUDE_HISTORY_READ = "KEY_INCLUDE_HISTORY_READ"
        const val KEY_DISCONNECT_TYPE = "KEY_INCLUDE_DISCONNECT_TYPE"
        const val KEY_HAS_MEDICAL_PERMISSIONS = "KEY_HAS_MEDICAL_PERMISSIONS"
    }

    lateinit var appName: String
    private var enableDeleteData: Boolean = true
    private var includeBackgroundRead: Boolean = false
    private var includeHistoryRead: Boolean = false
    private var disconnectType: DisconnectType = DisconnectType.FITNESS
    private var hasMedicalPermissions: Boolean = false

    @Inject lateinit var logger: HealthConnectLogger

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (savedInstanceState != null) {
            appName = savedInstanceState.getString(KEY_APP_NAME, "")
            enableDeleteData = savedInstanceState.getBoolean(KEY_ENABLE_DELETE_DATA, true)

            includeBackgroundRead =
                savedInstanceState.getBoolean(KEY_INCLUDE_BACKGROUND_READ, false)
            includeHistoryRead = savedInstanceState.getBoolean(KEY_INCLUDE_HISTORY_READ, false)

            disconnectType =
                DisconnectType.valueOf(
                    savedInstanceState.getString(KEY_DISCONNECT_TYPE, DisconnectType.FITNESS.name)
                )

            hasMedicalPermissions =
                savedInstanceState.getBoolean(KEY_HAS_MEDICAL_PERMISSIONS, false)
        }

        includeHistoryRead =
            when (disconnectType) {
                DisconnectType.FITNESS -> {
                    appPermissionViewModel.revokeFitnessShouldIncludePastData()
                }
                DisconnectType.MEDICAL -> {
                    appPermissionViewModel.revokeMedicalShouldIncludePastData()
                }
                else -> {
                    appPermissionViewModel.revokeAllShouldIncludePastData()
                }
            }

        includeBackgroundRead =
            when (disconnectType) {
                DisconnectType.FITNESS -> {
                    appPermissionViewModel.revokeFitnessShouldIncludeBackground()
                }
                DisconnectType.MEDICAL -> {
                    appPermissionViewModel.revokeMedicalShouldIncludeBackground()
                }
                DisconnectType.ALL -> {
                    appPermissionViewModel.revokeAllShouldIncludeBackground()
                }
            }

        hasMedicalPermissions =
            appPermissionViewModel.medicalPermissions.value.orEmpty().isNotEmpty()

        val body =
            layoutInflater.inflate(
                if (SettingsThemeHelper.isExpressiveTheme(requireContext()))
                    R.layout.dialog_message_with_checkbox_expressive
                else R.layout.dialog_message_with_checkbox_legacy,
                null,
            )
        body.findViewById<TextView>(R.id.dialog_message).apply { text = buildMessage() }

        body.findViewById<TextView>(R.id.dialog_title).apply { text = buildTitle() }
        val iconView = body.findViewById(R.id.dialog_icon) as ImageView
        val iconDrawable =
            AttributeResolver.getNullableDrawable(body.context, R.attr.disconnectIcon)
        iconDrawable?.let {
            iconView.setImageDrawable(it)
            iconView.visibility = View.VISIBLE
        }
        val checkBox =
            body.findViewById<CheckBox>(R.id.dialog_checkbox).apply {
                text = buildCheckboxText()
                visibility = if (enableDeleteData) View.VISIBLE else View.GONE
            }
        checkBox.setOnCheckedChangeListener { _, _ ->
            logger.logInteraction(DisconnectAppDialogElement.DISCONNECT_APP_DIALOG_DELETE_CHECKBOX)
        }

        val dialog =
            AlertDialogBuilder(this, DisconnectAppDialogElement.DISCONNECT_APP_DIALOG_CONTAINER)
                .setView(body)
                .setNeutralButton(
                    android.R.string.cancel,
                    DisconnectAppDialogElement.DISCONNECT_APP_DIALOG_CANCEL_BUTTON,
                ) { _, _ ->
                    setFragmentResult(DISCONNECT_CANCELED_EVENT, bundleOf())
                }
                .setPositiveButton(
                    R.string.permissions_disconnect_dialog_disconnect,
                    DisconnectAppDialogElement.DISCONNECT_APP_DIALOG_CONFIRM_BUTTON,
                ) { _, _ ->
                    setFragmentResult(
                        DISCONNECT_ALL_EVENT,
                        bundleOf(KEY_DELETE_DATA to checkBox.isChecked),
                    )
                }
                .setAdditionalLogging {
                    logger.logImpression(
                        DisconnectAppDialogElement.DISCONNECT_APP_DIALOG_DELETE_CHECKBOX
                    )
                }
                .create()
        dialog.setCanceledOnTouchOutside(false)
        isCancelable = false
        return dialog
    }

    private fun buildMessage(): String {
        return when (disconnectType) {
            DisconnectType.FITNESS -> {
                if (hasMedicalPermissions) {
                    getDisconnectMessageForFitnessOrMedicalPermissions()
                } else {
                    getDisconnectMessageForFitnessWithoutMedicalPermissions()
                }
            }
            DisconnectType.MEDICAL -> {
                getDisconnectMessageForFitnessOrMedicalPermissions()
            }
            DisconnectType.ALL -> {
                getDisconnectMessageForAllPermissions()
            }
        }
    }

    private fun getDisconnectMessageForFitnessOrMedicalPermissions(): String {
        return if (includeBackgroundRead && includeHistoryRead) {
            getString(
                R.string
                    .disconnect_all_fitness_or_medical_and_additional_permissions_dialog_message,
                appName,
            )
        } else if (includeBackgroundRead) {
            getString(
                R.string
                    .disconnect_all_fitness_or_medical_and_background_permissions_dialog_message,
                appName,
            )
        } else if (includeHistoryRead) {
            getString(
                R.string
                    .disconnect_all_fitness_or_medical_and_historical_permissions_dialog_message,
                appName,
            )
        } else {
            getString(
                R.string.disconnect_all_fitness_or_medical_no_additional_permissions_dialog_message,
                appName,
            )
        }
    }

    private fun getDisconnectMessageForFitnessWithoutMedicalPermissions(): String {
        return if (includeBackgroundRead && includeHistoryRead) {
            getString(R.string.permissions_disconnect_dialog_message_combined, appName)
        } else if (includeBackgroundRead) {
            getString(R.string.permissions_disconnect_dialog_message_background, appName)
        } else if (includeHistoryRead) {
            getString(R.string.permissions_disconnect_dialog_message_history, appName)
        } else {
            getString(R.string.permissions_disconnect_dialog_message, appName)
        }
    }

    private fun getDisconnectMessageForAllPermissions(): String {
        return if (includeBackgroundRead && includeHistoryRead) {
            getString(
                R.string.disconnect_all_health_and_additional_permissions_dialog_message,
                appName,
            )
        } else if (includeBackgroundRead) {
            getString(
                R.string.disconnect_all_health_and_background_permissions_dialog_message,
                appName,
            )
        } else if (includeHistoryRead) {
            getString(
                R.string.disconnect_all_health_and_historical_permissions_dialog_message,
                appName,
            )
        } else {
            getString(
                R.string.disconnect_all_health_no_additional_permissions_dialog_message,
                appName,
            )
        }
    }

    private fun buildTitle(): String {
        return when (disconnectType) {
            DisconnectType.FITNESS -> {
                if (hasMedicalPermissions) {
                    getString(R.string.disconnect_all_fitness_permissions_title)
                } else {
                    getString(R.string.permissions_disconnect_dialog_title)
                }
            }
            DisconnectType.MEDICAL -> {
                getString(R.string.disconnect_all_medical_permissions_title)
            }
            DisconnectType.ALL -> {
                getString(R.string.disconnect_all_health_permissions_title)
            }
        }
    }

    private fun buildCheckboxText(): String {
        return when (disconnectType) {
            DisconnectType.FITNESS -> {
                if (hasMedicalPermissions) {
                    getString(R.string.disconnect_all_fitness_permissions_dialog_checkbox, appName)
                } else {
                    getString(R.string.permissions_disconnect_dialog_checkbox, appName)
                }
            }
            DisconnectType.MEDICAL -> {
                getString(R.string.disconnect_all_medical_permissions_dialog_checkbox, appName)
            }
            DisconnectType.ALL -> {
                getString(R.string.disconnect_all_health_permissions_dialog_checkbox, appName)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_APP_NAME, appName)
        outState.putBoolean(KEY_ENABLE_DELETE_DATA, enableDeleteData)
        outState.putBoolean(KEY_INCLUDE_BACKGROUND_READ, includeBackgroundRead)
        outState.putBoolean(KEY_INCLUDE_HISTORY_READ, includeHistoryRead)
        outState.putString(KEY_DISCONNECT_TYPE, disconnectType.name)
        outState.putBoolean(KEY_HAS_MEDICAL_PERMISSIONS, hasMedicalPermissions)
    }

    enum class DisconnectType {
        MEDICAL,
        FITNESS,
        ALL,
    }
}
