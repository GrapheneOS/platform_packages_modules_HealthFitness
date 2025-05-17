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

package com.android.healthconnect.controller.onboarding

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.onboarding.FitnessAppOnboardingViewModel.FitnessAppOnboardingFragmentState
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.utils.AttributeResolver
import com.android.healthconnect.controller.utils.convertTextViewIntoLink
import com.android.settingslib.widget.GroupSectionDividerMixin
import com.android.settingslib.widget.SettingsThemeHelper

/**
 * Shows details of permissions that an app is requesting from Health Connect and what happens if
 * the user grants them.
 */
class PermissionDetailsPreference
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0,
) : Preference(context, attrs, defStyleAttr, defStyleRes), GroupSectionDividerMixin {

    init {
        layoutResource = R.layout.widget_request_permissions_details
        isSelectable = false
    }

    private lateinit var dataAccessType: TextView
    private lateinit var accessInfo: TextView
    private lateinit var privacyPolicy: TextView
    private var appMetadata: AppMetadata? = null
    private var onRationaleLinkClicked: (() -> Unit)? = null
    private var onLearnMoreClicked: (() -> Unit)? = null

    private var screenState: FitnessAppOnboardingFragmentState =
        FitnessAppOnboardingFragmentState.NoFitnessData

    fun bind(
        appMetadata: AppMetadata,
        screenState: FitnessAppOnboardingFragmentState,
        onRationaleLinkClicked: (() -> Unit)? = null,
        onLearnMoreClicked: (() -> Unit)? = null,
    ) {
        this.appMetadata = appMetadata
        this.screenState = screenState
        this.onRationaleLinkClicked = onRationaleLinkClicked
        this.onLearnMoreClicked = onLearnMoreClicked
        notifyChanged()
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        dataAccessType = holder.findViewById(R.id.data_access_type) as TextView
        accessInfo = holder.findViewById(R.id.access_info) as TextView
        privacyPolicy = holder.findViewById(R.id.privacy_policy) as TextView

        updateDetailedPermissions()
    }

    private fun updateDetailedPermissions() {
        val bulletPoints = listOf(dataAccessType, accessInfo, privacyPolicy)
        bulletPoints.forEach {
            it.setTextAppearance(AttributeResolver.getResource(context, R.attr.headerDetails))
        }
        if (SettingsThemeHelper.isExpressiveTheme(context)) {
            bulletPoints.forEach {
                it.setPadding(
                    0,
                    0,
                    0,
                    /* bottom= */ context.resources.getDimension(R.dimen.spacing_normal).toInt(),
                )
            }
        }

        when (screenState) {
            is FitnessAppOnboardingFragmentState.ShowFitnessRead -> {
                dataAccessType.text =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA)
                        context.getString(
                            R.string.request_fitness_permissions_data_access_type_read,
                            context.getString(R.string.request_learn_more),
                        )
                    else
                        context.getString(
                            R.string.request_fitness_permissions_data_access_type_read
                        )
                updateFitnessAccessInfo(
                    isHistoryGranted =
                        (screenState as FitnessAppOnboardingFragmentState.ShowFitnessRead)
                            .historyGranted
                )
                updatePrivacyPolicy()
            }
            is FitnessAppOnboardingFragmentState.ShowFitnessWrite -> {
                dataAccessType.text =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA)
                        context.getString(
                            R.string.request_fitness_permissions_data_access_type_write,
                            context.getString(R.string.request_learn_more),
                        )
                    else
                        context.getString(
                            R.string.request_fitness_permissions_data_access_type_write
                        )
                accessInfo.visibility = View.GONE
                updatePrivacyPolicy()
            }
            is FitnessAppOnboardingFragmentState.ShowFitnessReadWrite -> {
                dataAccessType.text =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA)
                        context.getString(
                            R.string.request_fitness_permissions_data_access_type_read_write,
                            context.getString(R.string.request_learn_more),
                        )
                    else
                        context.getString(
                            R.string.request_fitness_permissions_data_access_type_read_write
                        )
                updateFitnessAccessInfo(
                    isHistoryGranted =
                        (screenState as FitnessAppOnboardingFragmentState.ShowFitnessReadWrite)
                            .historyGranted
                )
                updatePrivacyPolicy()
            }
            else -> {
                // Do nothing
            }
        }

        // TODO (b/417702175) double check this should not also be the case for legacy
        // For Baklava platform and above, "learn more" links to a help center page.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA && this.appMetadata != null) {
            val learnMoreString = context.getString(R.string.request_learn_more)
            val dataAccessTypeText = dataAccessType.text!!.toString()
            convertTextViewIntoLink(
                dataAccessType,
                dataAccessTypeText,
                dataAccessTypeText.indexOf(learnMoreString),
                dataAccessTypeText.indexOf(learnMoreString) + learnMoreString.length,
            ) {
                onLearnMoreClicked?.invoke()
            }
        }
    }

    private fun updateFitnessAccessInfo(isHistoryGranted: Boolean) {
        accessInfo.visibility = View.VISIBLE
        setAccessInfoIcon(R.attr.accessHistoryIcon)
        accessInfo.text =
            if (isHistoryGranted) {
                context.getString(R.string.request_permissions_header_time_frame_history_desc)
            } else {
                context.getString(R.string.request_permissions_header_time_frame_desc)
            }
    }

    private fun setAccessInfoIcon(@AttrRes icon: Int) {
        accessInfo.setCompoundDrawablesRelativeWithIntrinsicBounds(
            AttributeResolver.getNullableDrawable(context, icon),
            null,
            null,
            null,
        )
    }

    private fun updatePrivacyPolicy() {
        val policyString = context.getString(R.string.request_permissions_privacy_policy)
        val rationaleText =
            context.resources.getString(
                R.string.request_permissions_rationale,
                appMetadata?.appName,
                policyString,
            )
        convertTextViewIntoLink(
            privacyPolicy,
            rationaleText,
            rationaleText.indexOf(policyString),
            rationaleText.indexOf(policyString) + policyString.length,
        ) {
            onRationaleLinkClicked?.invoke()
        }
    }
}
