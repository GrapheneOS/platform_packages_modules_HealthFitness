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
import android.view.View.OnClickListener
import androidx.preference.PreferenceViewHolder
import com.android.healthconnect.controller.utils.logging.ElementName
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.HealthConnectLoggerEntryPoint
import com.android.settingslib.widget.BannerMessagePreference
import dagger.hilt.android.EntryPointAccessors

class HealthBannerPreference(context: Context, private val logName: ElementName) :
    BannerMessagePreference(context) {

    private var logger: HealthConnectLogger
    private var positiveButtonLogName: ElementName? = null
    private var negativeButtonLogName: ElementName? = null
    private var dismissButtonLogName: ElementName? = null

    init {
        val hiltEntryPoint =
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                HealthConnectLoggerEntryPoint::class.java,
            )
        logger = hiltEntryPoint.logger()
        this.setAttentionLevel(AttentionLevel.NORMAL)
    }

    fun setPositiveButton(text: String, logName: ElementName, onClickListener: OnClickListener) {
        positiveButtonLogName = logName
        setPositiveButtonText(text)
        setPositiveButtonOnClickListener(onClickListener)
    }

    fun setNegativeButton(text: String, logName: ElementName, onClickListener: OnClickListener) {
        negativeButtonLogName = logName
        setNegativeButtonText(text)
        setNegativeButtonOnClickListener(onClickListener)
    }

    fun setDismissButton(logName: ElementName, onClickListener: OnClickListener) {
        dismissButtonLogName = logName
        setDismissButtonOnClickListener(onClickListener)
    }

    override fun setPositiveButtonOnClickListener(
        listener: OnClickListener?
    ): BannerMessagePreference {
        return super.setPositiveButtonOnClickListener {
            positiveButtonLogName?.let { logName -> logger.logInteraction(logName) }
            listener?.onClick(it)
        }
    }

    override fun setNegativeButtonOnClickListener(
        listener: OnClickListener?
    ): BannerMessagePreference {
        return super.setNegativeButtonOnClickListener {
            negativeButtonLogName?.let { logName -> logger.logInteraction(logName) }
            listener?.onClick(it)
        }
    }

    override fun setDismissButtonOnClickListener(
        listener: OnClickListener?
    ): BannerMessagePreference {
        return super.setDismissButtonOnClickListener {
            dismissButtonLogName?.let { logName -> logger.logInteraction(logName) }
            listener?.onClick(it)
        }
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        logger.logImpression(logName)
        positiveButtonLogName?.let { logger.logImpression(it) }
        negativeButtonLogName?.let { logger.logImpression(it) }
        dismissButtonLogName?.let { logger.logImpression(it) }
    }
}
