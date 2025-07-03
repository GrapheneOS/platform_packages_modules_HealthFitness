/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.android.healthconnect.controller.selectabledeletion

import android.content.Context
import android.view.View.GONE
import android.view.View.OnClickListener
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceClickListener
import androidx.preference.PreferenceViewHolder
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.permissions.connectedapps.ComparablePreference
import com.android.healthconnect.controller.permissions.data.HealthPermissionType
import com.android.healthconnect.controller.utils.logging.ElementName
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.HealthConnectLoggerEntryPoint
import dagger.hilt.android.EntryPointAccessors

/** Custom preference for displaying checkboxes where the user can delete their data */
class DeletionPermissionTypesPreference(
    context: Context,
    private val viewModel: DeletionDataViewModel,
    private val onPreferenceClickListener: OnPreferenceClickListener,
) : Preference(context), ComparablePreference {
    private var logger: HealthConnectLogger

    private var showCheckbox: Boolean = false
    private var isChecked: Boolean = false

    private lateinit var mHealthPermissionType: HealthPermissionType
    private lateinit var logNameNoCheckbox: ElementName
    private lateinit var logNameCheckbox: ElementName

    init {
        widgetLayoutResource = R.layout.widget_checkbox
        isSelectable = true

        val hiltEntryPoint =
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                HealthConnectLoggerEntryPoint::class.java,
            )
        logger = hiltEntryPoint.logger()
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val widgetFrame = holder.findViewById(android.R.id.widget_frame) as ViewGroup

        val checkBox = holder.findViewById(R.id.checkbox_button) as CheckBox
        showOrHideCheckbox(showCheckbox, widgetFrame)

        checkBox.isChecked = this.isChecked

        checkBox.contentDescription = context.getString(mHealthPermissionType.upperCaseLabel())

        checkBox.setOnClickListener(getCheckboxClickListenerWrapper())
        setOnPreferenceClickListener(checkBox, widgetFrame)

        val widgetFrameParent: ViewGroup? = widgetFrame.parent as ViewGroup?
        widgetFrameParent?.setPaddingRelative(
            widgetFrameParent.paddingStart,
            widgetFrameParent.paddingTop,
            widgetFrameParent.paddingEnd,
            widgetFrameParent.paddingBottom,
        )
    }

    private fun getCheckboxClickListenerWrapper() = OnClickListener {
        isChecked = !isChecked
        onDeletionMethod()
        logger.logInteraction(logNameCheckbox)
    }

    private fun onDeletionMethod() {
        if (mHealthPermissionType !in viewModel.setOfPermissionTypesToBeDeleted.value.orEmpty()) {
            viewModel.addToDeletionSet(mHealthPermissionType)
        } else {
            viewModel.removeFromDeletionSet(mHealthPermissionType)
        }
    }

    /** Set a click listener to check the checkbox */
    private fun setOnPreferenceClickListener(checkBox: CheckBox, widgetFrame: ViewGroup) {
        val clickListenerWrapper = OnPreferenceClickListener {
            if (showCheckbox) {
                // If we are in deletion mode, clicking on the preference should check the checkbox
                checkBox.toggle()
                isChecked = checkBox.isChecked
                onDeletionMethod()
                logger.logInteraction(logNameCheckbox)
            } else {
                // Otherwise, invoke the normal click listener
                onPreferenceClickListener.onPreferenceClick(it)
                logger.logInteraction(logNameNoCheckbox)
            }
            true
        }

        super.setOnPreferenceClickListener(clickListenerWrapper)
    }

    fun setHealthPermissionType(healthPermissionType: HealthPermissionType) {
        this.mHealthPermissionType = healthPermissionType
    }

    fun getHealthPermissionType(): HealthPermissionType {
        return mHealthPermissionType
    }

    fun setIsChecked(isChecked: Boolean) {
        this.isChecked = isChecked
        notifyChanged()
    }

    fun getIsChecked(): Boolean {
        return isChecked
    }

    fun setShowCheckbox(showCheckbox: Boolean) {
        this.showCheckbox = showCheckbox
        notifyChanged()
    }

    fun setLogNameNoCheckbox(logName: ElementName) {
        logNameNoCheckbox = logName
    }

    fun setLogNameCheckbox(logName: ElementName) {
        logNameCheckbox = logName
    }

    private fun showOrHideCheckbox(showCheckbox: Boolean, widgetFrame: ViewGroup) {
        widgetFrame.visibility = if (showCheckbox) VISIBLE else GONE
        widgetFrame.tag = if (showCheckbox) "checkbox" else ""

        if (showCheckbox) {
            logger.logImpression(logNameCheckbox)
        } else {
            logger.logImpression(logNameNoCheckbox)
        }
    }

    override fun hasSameContents(preference: Preference): Boolean {
        return preference is DeletionPermissionTypesPreference &&
            this.title == preference.title &&
            this.summary == preference.summary &&
            this.icon == preference.icon &&
            this.isChecked == preference.isChecked
    }

    override fun isSameItem(preference: Preference): Boolean {
        return preference == this
    }
}
