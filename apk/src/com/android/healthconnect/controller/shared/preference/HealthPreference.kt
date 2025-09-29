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
import androidx.preference.Preference
import com.android.healthconnect.controller.permissions.connectedapps.ComparablePreference
import com.android.healthconnect.controller.utils.logging.ElementName
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.UnknownGenericElement
import com.android.settingslib.widget.GroupSectionDividerMixin

/** A [Preference] that allows logging. */
open class HealthPreference
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null) :
    Preference(context, attrs), ComparablePreference {

    private var logger: HealthConnectLogger = HealthPreferenceUtils.initializeLogger(context)
    open var logName: ElementName = UnknownGenericElement.UNKNOWN_HEALTH_PREFERENCE

    override fun onAttached() {
        super.onAttached()
        logger.logImpression(logName)
    }

    override fun setOnPreferenceClickListener(
        onPreferenceClickListener: OnPreferenceClickListener?
    ) {
        super.setOnPreferenceClickListener(
            HealthPreferenceUtils.loggingPreferenceClickListener(
                logger,
                logName,
                onPreferenceClickListener,
            )
        )
    }

    override fun isSameItem(preference: Preference): Boolean =
        HealthPreferenceUtils.isSameItem(preference, this)

    override fun hasSameContents(preference: Preference): Boolean =
        HealthPreferenceUtils.hasSameContents(preference, this)
}

/** A [HealthPreference] without an expressive background. */
class HealthPreferenceNoBg(context: Context, attrs: AttributeSet? = null) :
    HealthPreference(context, attrs), GroupSectionDividerMixin
