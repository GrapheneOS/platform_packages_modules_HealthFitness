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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.healthconnect.controller.utils.logging.ElementName
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.HealthConnectLoggerEntryPoint
import com.android.healthconnect.controller.utils.logging.UIAction
import com.android.healthconnect.controller.utils.logging.UnknownGenericElement
import com.android.settingslib.widget.MainSwitchPreference
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * A [MainSwitchPreference] that allows logging.
 *
 * Use the method [setUpStateManagement] to set up the state management, logging and to define
 * functions to be called when the switch is checked or unchecked.
 */
class HealthMainSwitchPreference
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null) : MainSwitchPreference(context, attrs) {

    private var logger: HealthConnectLogger
    var logNameActive: ElementName = UnknownGenericElement.UNKNOWN_SWITCH_ACTIVE_PREFERENCE
    var logNameInactive: ElementName = UnknownGenericElement.UNKNOWN_SWITCH_INACTIVE_PREFERENCE

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

    /**
     * Sets up the state management, logging and functions for the switch.
     *
     * @param onCheckedAction A function that is called when the switch is checked.
     * @param onUncheckedAction A function that is called when the switch is unchecked.
     */
    fun setUpStateManagement(
        lifecycleOwner: LifecycleOwner,
        observedLiveData: LiveData<Boolean>,
        onCheckedAction: suspend () -> Boolean,
        onUncheckedAction: suspend () -> Boolean,
    ) {

        onPreferenceChangeListener = null
        onPreferenceChangeListener = OnPreferenceChangeListener { _, newValue ->
            val isCheckedByUser = newValue as Boolean
            if (isCheckedByUser) {
                logger.logInteraction(logNameInactive, UIAction.ACTION_TOGGLE_ON)
            } else {
                logger.logInteraction(logNameActive, UIAction.ACTION_TOGGLE_OFF)
            }

            lifecycleOwner.lifecycleScope.launch {
                val success =
                    if (isCheckedByUser) {
                        onCheckedAction()
                    } else {
                        onUncheckedAction()
                    }
            }
            true
        }

        observedLiveData.observe(lifecycleOwner) { isObservedChecked ->
            val originalListener = onPreferenceChangeListener
            onPreferenceChangeListener = null
            isChecked = isObservedChecked
            onPreferenceChangeListener = originalListener
        }
    }

    /**
     * Sets up the state management, logging and functions for the switch.
     *
     * @param onCheckedAction A function that is called when the switch is checked.
     * @param onUncheckedAction A function that is called when the switch is unchecked.
     */
    fun setUpStateManagement(
        lifecycleOwner: LifecycleOwner,
        observedFlow: StateFlow<Boolean>,
        onCheckedAction: suspend () -> Boolean,
        onUncheckedAction: suspend () -> Boolean,
    ) {

        onPreferenceChangeListener = null
        onPreferenceChangeListener = OnPreferenceChangeListener { _, newValue ->
            val isCheckedByUser = newValue as Boolean
            if (isCheckedByUser) {
                logger.logInteraction(logNameInactive, UIAction.ACTION_TOGGLE_ON)
            } else {
                logger.logInteraction(logNameActive, UIAction.ACTION_TOGGLE_OFF)
            }

            lifecycleOwner.lifecycleScope.launch {
                if (isCheckedByUser) {
                    onCheckedAction()
                } else {
                    onUncheckedAction()
                }
            }
            true
        }

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                observedFlow.collect { isObservedChecked ->
                    val originalListener = onPreferenceChangeListener
                    onPreferenceChangeListener = null
                    isChecked = isObservedChecked
                    onPreferenceChangeListener = originalListener
                }
            }
        }
    }
}
