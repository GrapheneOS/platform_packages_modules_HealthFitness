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
package com.android.healthconnect.controller.permissions.request

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.utils.AttributeResolver
import com.android.healthconnect.controller.utils.LocalDateTimeFormatter
import com.android.healthconnect.controller.utils.boldAppName
import com.android.healthconnect.controller.utils.convertTextViewIntoLink
import com.android.settingslib.widget.GroupSectionDividerMixin
import com.android.settingslib.widget.SettingsThemeHelper

internal class RequestPermissionHeaderPreference
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0,
) : Preference(context, attrs, defStyleAttr, defStyleRes), GroupSectionDividerMixin {

    private lateinit var title: TextView
    private lateinit var summary: TextView
    private lateinit var detailedPermissions: LinearLayout
    private lateinit var dataAccessType: TextView
    private lateinit var accessInfo: TextView
    private lateinit var privacyPolicy: TextView

    private var appName: String? = null
    private var onRationaleLinkClicked: (() -> Unit)? = null
    private var onAboutHealthRecordsClicked: (() -> Unit)? = null
    private var screenState = RequestPermissionsScreenState()

    private val dateFormatter by lazy { LocalDateTimeFormatter(context) }

    init {
        layoutResource = R.layout.widget_request_permission_header
        isSelectable = false
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val headerArea = holder.findViewById(R.id.header_area) as FrameLayout
        val headerLayoutId =
            if (SettingsThemeHelper.isExpressiveTheme(context)) {
                R.layout.widget_health_setup_header_content_expressive
            } else {
                R.layout.widget_health_setup_header_content_legacy
            }
        headerArea.addView(LayoutInflater.from(context).inflate(headerLayoutId, headerArea, false))
        title = holder.findViewById(R.id.title) as TextView
        summary = holder.findViewById(R.id.summary) as TextView
        detailedPermissions = holder.findViewById(R.id.detailed_permissions) as LinearLayout
        dataAccessType = holder.findViewById(R.id.data_access_type) as TextView
        accessInfo = holder.findViewById(R.id.access_info) as TextView
        privacyPolicy = holder.findViewById(R.id.privacy_policy) as TextView

        updateTitle()
        updateSummary()
        updateDetailedPermissions()
    }

    fun bind(
        appName: String,
        screenState: RequestPermissionsScreenState,
        onRationaleLinkClicked: (() -> Unit)? = null,
        onAboutHealthRecordsClicked: (() -> Unit)? = null,
    ) {
        this.appName = appName
        this.screenState = screenState
        this.onRationaleLinkClicked = onRationaleLinkClicked
        this.onAboutHealthRecordsClicked = onAboutHealthRecordsClicked
        notifyChanged()
    }

    private fun updateTitle() {
        val text =
            when (screenState) {
                is MedicalScreenState -> {
                    context.getString(R.string.medical_request_header, appName)
                }
                is FitnessScreenState -> {
                    if ((screenState as FitnessScreenState).hasMedical) {
                        context.getString(
                            R.string.request_permissions_with_medical_header_title,
                            appName,
                        )
                    } else {
                        context.getString(R.string.request_permissions_header_title, appName)
                    }
                }
                is AdditionalScreenState -> {
                    when (screenState) {
                        is AdditionalScreenState.ShowHistory -> {
                            context.getString(R.string.history_read_single_request_title, appName)
                        }
                        is AdditionalScreenState.ShowBackground -> {
                            context.getString(
                                R.string.background_read_single_request_title,
                                appName,
                            )
                        }
                        is AdditionalScreenState.ShowCombined -> {
                            context.getString(
                                R.string.additional_permissions_combined_request_title,
                                appName,
                            )
                        }
                        else -> {
                            ""
                        }
                    }
                }

                else -> {
                    ""
                }
            }
        title.text = boldAppName(appName, text)
    }

    private fun updateSummary() {
        when (screenState) {
            is MedicalScreenState.ShowMedicalWrite -> {
                summary.visibility = View.VISIBLE
                summary.text = context.getString(R.string.medical_request_summary, appName)
            }
            is AdditionalScreenState -> {
                summary.visibility = View.VISIBLE
                summary.text = getAdditionalScreenStateSummary(screenState as AdditionalScreenState)
            }
            else -> {
                summary.visibility = View.GONE
            }
        }
    }

    private fun getAdditionalScreenStateSummary(screenState: AdditionalScreenState): String {
        return when (screenState) {
            is AdditionalScreenState.ShowHistory -> {
                if (screenState.hasMedical) {
                    if (screenState.dataAccessDate != null) {
                        val formattedDate = dateFormatter.formatLongDate(screenState.dataAccessDate)
                        context.getString(
                            R.string.history_read_medical_single_request_description,
                            formattedDate,
                        )
                    } else {
                        context.getString(
                            R.string.history_read_medical_single_request_description_fallback
                        )
                    }
                } else {
                    if (screenState.dataAccessDate != null) {
                        val formattedDate = dateFormatter.formatLongDate(screenState.dataAccessDate)
                        context.getString(
                            R.string.history_read_single_request_description,
                            formattedDate,
                        )
                    } else {
                        context.getString(R.string.history_read_single_request_description_fallback)
                    }
                }
            }
            is AdditionalScreenState.ShowBackground -> {
                if (screenState.hasMedical) {
                    if (screenState.isMedicalReadGranted && screenState.isFitnessReadGranted) {
                        context.getString(
                            R.string
                                .background_read_medical_single_request_description_both_types_granted
                        )
                    } else if (screenState.isMedicalReadGranted) {
                        context.getString(
                            R.string
                                .background_read_medical_single_request_description_medical_granted
                        )
                    } else {
                        context.getString(
                            R.string
                                .background_read_medical_single_request_description_fitness_granted
                        )
                    }
                } else {
                    context.getString(R.string.background_read_single_request_description)
                }
            }
            is AdditionalScreenState.ShowCombined -> {
                context.getString(
                    R.string.additional_permissions_combined_request_description,
                    screenState.appMetadata.appName,
                )
            }
            else -> {
                ""
            }
        }
    }

    private fun updateDetailedPermissions() {
        detailedPermissions.visibility = View.VISIBLE
        when (screenState) {
            is MedicalScreenState -> {
                updateMedicalDetailedPermissions()
            }
            is FitnessScreenState -> {
                updateFitnessDetailedPermissions()
            }
            else -> {
                // No detailed permissions for additional permissions requests
                detailedPermissions.visibility = View.GONE
            }
        }
    }

    private fun updateMedicalDetailedPermissions() {
        when (screenState) {
            is MedicalScreenState.ShowMedicalReadWrite -> {
                dataAccessType.text =
                    context.getString(R.string.request_permissions_data_access_type_read_write)
                updateMedicalAccessInfo(showWrite = true)
                updatePrivacyPolicy()
            }
            is MedicalScreenState.ShowMedicalRead -> {
                dataAccessType.text =
                    context.getString(R.string.request_permissions_data_access_type_read)
                updateMedicalAccessInfo(showWrite = false)
                updatePrivacyPolicy()
            }
            else -> {
                detailedPermissions.visibility = View.GONE
            }
        }
    }

    private fun updateFitnessDetailedPermissions() {
        when (screenState) {
            is FitnessScreenState.ShowFitnessRead -> {
                dataAccessType.text =
                    context.getString(R.string.request_permissions_data_access_type_read)
                updateFitnessAccessInfo(
                    isHistoryGranted =
                        (screenState as FitnessScreenState.ShowFitnessRead).historyGranted
                )
                updatePrivacyPolicy()
            }
            is FitnessScreenState.ShowFitnessWrite -> {
                dataAccessType.text =
                    context.getString(R.string.request_permissions_data_access_type_write)
                accessInfo.visibility = View.GONE
                updatePrivacyPolicy()
            }
            is FitnessScreenState.ShowFitnessReadWrite -> {
                dataAccessType.text =
                    context.getString(R.string.request_permissions_data_access_type_read_write)
                updateFitnessAccessInfo(
                    isHistoryGranted =
                        (screenState as FitnessScreenState.ShowFitnessReadWrite).historyGranted
                )
                updatePrivacyPolicy()
            }
            else -> {
                detailedPermissions.visibility = View.GONE
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

    private fun updateMedicalAccessInfo(showWrite: Boolean) {
        setAccessInfoIcon(R.attr.medicalServicesIcon)
        val aboutHealthRecordsString =
            context.getString(R.string.medical_request_about_health_records)
        val accessInfoText =
            if (showWrite) {
                context.getString(
                    R.string.medical_request_header_access_info_read_write,
                    aboutHealthRecordsString,
                )
            } else {
                context.getString(
                    R.string.medical_request_header_access_info_read,
                    aboutHealthRecordsString,
                )
            }
        accessInfo.text = accessInfoText
        convertTextViewIntoLink(
            accessInfo,
            accessInfoText,
            accessInfoText.indexOf(aboutHealthRecordsString),
            accessInfoText.indexOf(aboutHealthRecordsString) + aboutHealthRecordsString.length,
        ) {
            onAboutHealthRecordsClicked?.invoke()
        }
    }

    private fun updatePrivacyPolicy() {
        val policyString = context.getString(R.string.request_permissions_privacy_policy)
        val rationaleText =
            context.resources.getString(
                R.string.request_permissions_rationale,
                appName,
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

    private fun setAccessInfoIcon(@AttrRes icon: Int) {
        accessInfo.setCompoundDrawablesRelativeWithIntrinsicBounds(
            AttributeResolver.getNullableDrawable(context, icon),
            null,
            null,
            null,
        )
    }
}
