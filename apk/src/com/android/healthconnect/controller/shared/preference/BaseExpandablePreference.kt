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
import android.widget.ImageView
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.preference.Preference
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceViewHolder
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.utils.logging.ElementName
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.HealthConnectLoggerEntryPoint
import com.android.healthconnect.controller.utils.logging.UIAction
import com.android.healthconnect.controller.utils.logging.UnknownGenericElement
import dagger.hilt.android.EntryPointAccessors

/**
 * A base class for [PreferenceGroup] that can be expanded and collapsed.
 *
 * This class provides core logic but does not handle view inflation. Subclasses are responsible for
 * inflating and managing the specific layout for the expandable view type.
 */
abstract class BaseExpandablePreference
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null) : PreferenceGroup(context, attrs) {

    var mIsExpanded = false
    protected var mOnExpandChangeListener: OnExpandChangeListener? = null
    protected var logger: HealthConnectLogger
    var logName: ElementName = UnknownGenericElement.UNKNOWN_HEALTH_PREFERENCE

    init {
        val hiltEntryPoint =
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                HealthConnectLoggerEntryPoint::class.java,
            )
        logger = hiltEntryPoint.logger()
    }

    /**
     * Interface definition for a callback to be invoked when the expansion state of this preference
     * changes.
     */
    fun interface OnExpandChangeListener {
        /**
         * Called when the expansion state of this preference has changed.
         *
         * @param isExpanded The new expansion state.
         */
        fun onExpandChanged(isExpanded: Boolean)
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        holder.isDividerAllowedBelow = false
        holder.isDividerAllowedAbove = false

        val arrow = holder.findViewById(getDropDownIconId()) as? ImageView
        arrow?.rotation = if (mIsExpanded) 180f else 0f

        val accessibilityAction =
            if (mIsExpanded) {
                context.getString(R.string.expandable_preference_collapse_talkback_description)
            } else {
                context.getString(R.string.expandable_preference_expand_talkback_description)
            }
        ViewCompat.replaceAccessibilityAction(
            holder.itemView,
            AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK,
            accessibilityAction,
            null,
        )

        holder.itemView.setOnClickListener {
            logger.logInteraction(logName, UIAction.ACTION_CLICK)
            setExpanded(!mIsExpanded)
            mOnExpandChangeListener?.onExpandChanged(mIsExpanded)
        }
        updateChildPreferences()
    }

    override fun onAttached() {
        super.onAttached()
        logger.logImpression(logName)
    }

    override fun addPreference(preference: Preference): Boolean {
        val result = super.addPreference(preference)
        notifyChanged()
        return result
    }

    /** Sets the expansion state of this preference. */
    fun setExpanded(isExpanded: Boolean) {
        if (mIsExpanded != isExpanded) {
            mIsExpanded = isExpanded
            notifyChanged()
        }
    }

    /** Sets a callback to be invoked when the expansion state of this preference changes. */
    fun setOnExpandChangeListener(listener: OnExpandChangeListener?) {
        mOnExpandChangeListener = listener
    }

    private fun updateChildPreferences() {
        for (i in 0 until preferenceCount) {
            getPreference(i).isVisible = mIsExpanded
        }
    }

    abstract fun getDropDownIconId(): Int
}
