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
import android.util.AttributeSet
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.preference.Preference.OnPreferenceClickListener
import androidx.preference.PreferenceViewHolder
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.shared.preference.HealthPreference

/** Custom preference that displays a checkbox and allows the user to select all items */
class SelectAllCheckboxPreference
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null) : HealthPreference(context, attrs) {

    private var widgetFrame: ViewGroup? = null
    private var checkBox: CheckBox? = null
    private var checkboxButtonListener: OnClickListener? = null
    private var onPreferenceClickListener: OnPreferenceClickListener? = null
    private var isChecked: Boolean = false

    init {
        widgetLayoutResource = R.layout.widget_checkbox
        isSelectable = true
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        widgetFrame = holder.findViewById(android.R.id.widget_frame) as ViewGroup?
        widgetFrame?.tag = "checkbox"

        checkBox = holder.findViewById(R.id.checkbox_button) as CheckBox

        checkBox?.isChecked = isChecked

        checkBox?.setOnClickListener(checkboxButtonListener)

        val widgetFrameParent: ViewGroup? = widgetFrame?.parent as ViewGroup?
        widgetFrameParent?.setPaddingRelative(
            widgetFrameParent.paddingStart,
            widgetFrameParent.paddingTop,
            widgetFrameParent.paddingEnd,
            widgetFrameParent.paddingBottom,
        )
    }

    fun setOnPreferenceClickListenerWithCheckbox(method: () -> Unit) {
        val clickListener = OnPreferenceClickListener {
            checkBox?.toggle()
            setIsChecked(checkBox?.isChecked ?: false)
            method()
            true
        }

        checkboxButtonListener = OnClickListener {
            setIsChecked(checkBox?.isChecked ?: false)
            method()
        }

        if (onPreferenceClickListener == null) {
            onPreferenceClickListener = clickListener
        }

        super.setOnPreferenceClickListener(clickListener)
        notifyChanged()
    }

    fun removeOnPreferenceClickListener() {
        if (checkboxButtonListener != null) {
            checkboxButtonListener = null
        }

        if (onPreferenceClickListener != null) {
            onPreferenceClickListener = null
        }
    }

    fun setIsChecked(checked: Boolean) {
        isChecked = checked
    }

    fun getIsChecked(): Boolean {
        return isChecked
    }
}
