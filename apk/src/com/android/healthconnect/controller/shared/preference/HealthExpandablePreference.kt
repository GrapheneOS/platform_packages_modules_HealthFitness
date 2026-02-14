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

package com.android.healthconnect.controller.shared.preference

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.isVisible
import androidx.preference.PreferenceViewHolder
import com.android.healthconnect.controller.R
import com.android.settingslib.widget.SettingsThemeHelper

class HealthExpandablePreference
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null) :
    BaseExpandablePreference(context, attrs) {

    private var onCheckedChangeListener: ((Boolean) -> Unit)? = null
    var isChecked: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                notifyChanged()
            }
        }

    var switchContentDescription: CharSequence? = null
        set(value) {
            field = value
            notifyChanged()
        }

    init {
        layoutResource =
            if (SettingsThemeHelper.isExpressiveTheme(context)) {
                R.layout.preference_expressive_expandable_switch
            } else {
                R.layout.preference_non_expressive_expandable_switch
            }
        widgetLayoutResource =
            if (SettingsThemeHelper.isExpressiveTheme(context)) {
                R.layout.expressive_expand_arrow_switch_widget
            } else {
                R.layout.non_expressive_expand_arrow_switch_widget
            }
    }

    fun setOnSwitchChangeListener(listener: ((Boolean) -> Unit)?) {
        onCheckedChangeListener = listener
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val expandArrow = holder.findViewById(R.id.expand_arrow) as? ImageView
        val twoTargetDivider = holder.findViewById(R.id.two_target_divider) as? View
        val switch = holder.findViewById(R.id.switch_widget) as? SwitchCompat
        switch?.isClickable = true
        switch?.contentDescription = switchContentDescription

        switch?.setOnCheckedChangeListener(null)
        switch?.isChecked = isChecked
        switch?.setOnCheckedChangeListener { _, newIsChecked ->
            if (isChecked != newIsChecked) {
                isChecked = newIsChecked
                onCheckedChangeListener?.invoke(newIsChecked)
            }
        }

        val hasChildren = preferenceCount > 0
        expandArrow?.isVisible = hasChildren
        twoTargetDivider?.isVisible = hasChildren

        expandArrow?.rotation = if (mIsExpanded) 180f else 0f

        if (hasChildren) {
            holder.itemView.setOnClickListener {
                logger.logInteraction(
                    logName,
                    com.android.healthconnect.controller.utils.logging.UIAction.ACTION_CLICK,
                )
                setExpanded(!mIsExpanded)
                mOnExpandChangeListener?.onExpandChanged(mIsExpanded)
                notifyChanged()
            }
        } else {
            holder.itemView.setOnClickListener(null)
        }
    }

    override fun getDropDownIconId(): Int {
        return R.drawable.ic_expand_more
    }
}
