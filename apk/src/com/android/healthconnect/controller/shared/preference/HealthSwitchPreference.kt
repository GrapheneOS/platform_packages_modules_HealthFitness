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
package com.android.healthconnect.controller.shared.preference

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.preference.Preference.OnPreferenceChangeListener
import androidx.preference.PreferenceViewHolder
import androidx.preference.SwitchPreferenceCompat
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.permissions.data.MedicalPermissionType
import com.android.healthconnect.controller.permissions.data.PermissionsAccessType
import com.android.healthconnect.controller.permissions.data.PermissionsAccessTypeInternal
import com.android.healthconnect.controller.utils.logging.ElementName
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.HealthConnectLoggerEntryPoint
import com.android.healthconnect.controller.utils.logging.UIAction
import com.android.healthconnect.controller.utils.logging.UnknownGenericElement
import com.google.android.material.materialswitch.MaterialSwitch
import dagger.hilt.android.EntryPointAccessors

/**
 * A [SwitchPreferenceCompat] that allows logging.
 *
 * @property permission Needed to determine the access type (Read / Write) the switch updates for
 *   Fitness and Medical permissions. This is needed for talkback.
 */
open class HealthSwitchPreference
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null) :
    SwitchPreferenceCompat(context, attrs) {

    var logNameActive: ElementName = UnknownGenericElement.UNKNOWN_SWITCH_ACTIVE_PREFERENCE
    var logNameInactive: ElementName = UnknownGenericElement.UNKNOWN_SWITCH_INACTIVE_PREFERENCE
    var permission: HealthPermission? = null
    private var logger: HealthConnectLogger
    private var loggingClickListener: OnPreferenceChangeListener? = null

    init {
        val hiltEntryPoint =
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                HealthConnectLoggerEntryPoint::class.java,
            )
        logger = hiltEntryPoint.logger()
    }

    override fun onAttached() {
        super.onAttached()
        if (isChecked) {
            logger.logImpression(logNameActive)
        } else {
            logger.logImpression(logNameInactive)
        }
    }

    override fun setOnPreferenceChangeListener(
        onPreferenceChangeListener: OnPreferenceChangeListener?
    ) {
        loggingClickListener = OnPreferenceChangeListener { preference, newValue ->
            if (newValue is Boolean && newValue) {
                logger.logInteraction(logNameInactive, UIAction.ACTION_TOGGLE_ON)
            } else if (newValue is Boolean) {
                logger.logInteraction(logNameActive, UIAction.ACTION_TOGGLE_OFF)
            }
            onPreferenceChangeListener?.onPreferenceChange(preference, newValue)!!
        }
        super.setOnPreferenceChangeListener(loggingClickListener)
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        maybeAttachSwitchFunctionToContentDescription(holder)
    }

    private fun maybeAttachSwitchFunctionToContentDescription(holder: PreferenceViewHolder) {

        if (permission == null) {
            return
        }

        val switchFunction =
            when (getPermissionAccessType(permission!!)) {
                PermissionsAccessTypeInternal.READ -> context.getString(R.string.read_access)
                PermissionsAccessTypeInternal.WRITE -> context.getString(R.string.write_access)
                PermissionsAccessTypeInternal.UNKNOWN -> return
            }
        val currentState =
            if (isChecked) context.getString(R.string.on) else context.getString(R.string.off)

        val contentDescription =
            context.getString(
                R.string.health_switch_content_description,
                title,
                switchFunction,
                currentState,
            )
        holder.itemView.contentDescription = contentDescription

        val switchWidgetContainer = holder.findViewById(android.R.id.widget_frame)
        findSwitchInView(switchWidgetContainer)?.let {
            it.isFocusable = false
            it.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        holder.itemView.isFocusable = true
    }

    private fun getPermissionAccessType(
        permission: HealthPermission
    ): PermissionsAccessTypeInternal {

        return when (permission) {
            is HealthPermission.FitnessPermission -> {
                if (permission.permissionsAccessType == PermissionsAccessType.READ) {
                    PermissionsAccessTypeInternal.READ
                } else {
                    PermissionsAccessTypeInternal.WRITE
                }
            }

            is HealthPermission.MedicalPermission -> {
                if (permission.medicalPermissionType != MedicalPermissionType.ALL_MEDICAL_DATA) {
                    PermissionsAccessTypeInternal.READ
                } else {
                    PermissionsAccessTypeInternal.WRITE
                }
            }

            else -> PermissionsAccessTypeInternal.UNKNOWN
        }
    }

    private fun findSwitchInView(viewGroup: View?): MaterialSwitch? {
        if (viewGroup is MaterialSwitch) {
            return viewGroup
        }
        if (viewGroup is ViewGroup) {
            for (i in 0 until viewGroup.childCount) {
                val child = viewGroup.getChildAt(i)
                val foundSwitch = findSwitchInView(child)
                if (foundSwitch != null) {
                    return foundSwitch
                }
            }
        }
        return null
    }
}
