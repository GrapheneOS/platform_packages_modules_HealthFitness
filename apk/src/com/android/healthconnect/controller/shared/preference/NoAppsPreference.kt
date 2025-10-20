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
import android.widget.TextView
import androidx.preference.PreferenceViewHolder
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.utils.logging.ElementName
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.HealthConnectLoggerEntryPoint
import com.android.healthconnect.controller.utils.logging.UnknownGenericElement
import dagger.hilt.android.EntryPointAccessors

/** A custom preference used to display a message when there are no apps available. */
class NoAppsPreference @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    HealthPreference(context, attrs) {
    private var logger: HealthConnectLogger

    private var learnMoreText = ""
    private var learnMoreAction: View.OnClickListener? = null
    override var logName: ElementName = UnknownGenericElement.UNKNOWN_HEALTH_PREFERENCE
    private var linkLogName: ElementName = UnknownGenericElement.UNKNOWN_HEALTH_PREFERENCE

    init {
        key = "empty_health_apps"
        isSelectable = false
        layoutResource = R.layout.widget_no_apps_preference

        val hiltEntryPoint =
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                HealthConnectLoggerEntryPoint::class.java,
            )
        logger = hiltEntryPoint.logger()
    }

    fun setLogNames(textLogName: ElementName, linkLogName: ElementName) {
        logName = textLogName
        this.linkLogName = linkLogName
    }

    fun setLearnMoreText(text: String) {
        learnMoreText = text
        notifyChanged()
    }

    fun setLearnMoreAction(action: View.OnClickListener) {
        learnMoreAction = action
        notifyChanged()
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val learnMoreSection = holder.itemView.findViewById<TextView>(R.id.settingslib_learn_more)

        if (learnMoreText.isNotEmpty() && learnMoreAction != null) {
            logger.logImpression(linkLogName)
            learnMoreSection.text = learnMoreText
            learnMoreSection.visibility = View.VISIBLE
            learnMoreSection.setOnClickListener { view ->
                logger.logInteraction(linkLogName)
                learnMoreAction?.onClick(view)
            }
        }
    }
}
