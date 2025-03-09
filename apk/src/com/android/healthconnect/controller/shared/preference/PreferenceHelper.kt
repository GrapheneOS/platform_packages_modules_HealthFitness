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
package com.android.healthconnect.controller.shared.preference

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View.OnClickListener
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceClickListener
import androidx.preference.PreferenceScreen
import com.android.healthconnect.controller.permissions.connectedapps.PermissionHeaderPreference
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.utils.logging.ElementName
import com.android.settingslib.widget.AppHeaderPreference
import com.android.settingslib.widget.IntroPreference
import com.android.settingslib.widget.SettingsThemeHelper
import com.android.settingslib.widget.TopIntroPreference

private const val APP_HEADER_PREFERENCE_KEY = "app_header_preference"
private const val INTRO_PREFERENCE_KEY = "intro_preference"
private const val PERMISSION_HEADER_PREFERENCE_KEY = "permission_header_preference"

fun topIntroPreference(
    context: Context,
    preferenceKey: String? = null,
    preferenceTitle: String? = null,
    learnMoreText: String? = null,
    learnMoreAction: OnClickListener? = null,
    preferenceOrder: Int = 0,
): Preference {
    // TODO(b/378469065): Remove isExpressive check once TopIntroPreference is fixed. At the moment
    // it does not display the learn more link when expressive theming is off.
    return if (isExpressiveAndBuildVersion(context)) {
        TopIntroPreference(context).apply {
            preferenceKey?.let { key = it }
            preferenceTitle?.let { title = it }
            learnMoreText?.let { setLearnMoreText(it) }
            learnMoreAction?.let { setLearnMoreAction(it) }
            order = preferenceOrder
        }
    } else {
        LegacyTopIntroPreference(context).apply {
            preferenceKey?.let { key = it }
            preferenceTitle?.let { setTitle(it) }
            learnMoreText?.let { setLearnMoreText(it) }
            learnMoreAction?.let { setLearnMoreAction(it) }
            order = preferenceOrder
        }
    }
}

/**
 * Ensures an IntroPreference (expressive) or AppHeaderPreference (non expressive) is added to the
 * top of the preference screen. If the preference already exists, it updates its properties.
 *
 * @param preferenceScreen The screen where the preference should be added.
 * @param context The context used to create the preference.
 * @param appMetadata AppMetadata containing the app icon and name.
 */
fun addIntroOrAppHeaderPreference(
    preferenceScreen: PreferenceScreen,
    context: Context,
    appMetadata: AppMetadata,
) {
    val isExpressiveTheme = isExpressiveAndBuildVersion(context)

    val (keyToUpsert, keyToRemove) =
        if (isExpressiveTheme) {
            Pair(INTRO_PREFERENCE_KEY, APP_HEADER_PREFERENCE_KEY)
        } else {
            Pair(APP_HEADER_PREFERENCE_KEY, INTRO_PREFERENCE_KEY)
        }

    updateOrCreatePreference(preferenceScreen, context, appMetadata, keyToUpsert)
    safelyRemovePreference(preferenceScreen, keyToRemove)
}

fun addIntroOrPermissionHeaderPreference(
    preferenceScreen: PreferenceScreen,
    context: Context,
    title: String,
    icon: Drawable?,
    summary: String,
) {
    val isExpressiveTheme = isExpressiveAndBuildVersion(context)

    val (keyToUpsert, keyToRemove) =
        if (isExpressiveTheme) {
            Pair(INTRO_PREFERENCE_KEY, PERMISSION_HEADER_PREFERENCE_KEY)
        } else {
            Pair(PERMISSION_HEADER_PREFERENCE_KEY, INTRO_PREFERENCE_KEY)
        }

    updateOrCreatePreference(preferenceScreen, context, title, icon, summary, keyToUpsert)
    safelyRemovePreference(preferenceScreen, keyToRemove)
}

private fun updateOrCreatePreference(
    preferenceScreen: PreferenceScreen,
    context: Context,
    appMetadata: AppMetadata,
    key: String,
) {
    val existingPreference = preferenceScreen.findPreference<Preference>(key)
    if (existingPreference != null) {
        existingPreference.apply {
            icon = appMetadata.icon
            title = appMetadata.appName
        }
    } else {
        val newPreference =
            createPreference(context, key).apply {
                icon = appMetadata.icon
                title = appMetadata.appName
                this.key = key
                order = 0
            }

        preferenceScreen.addPreference(newPreference)
    }
}

private fun updateOrCreatePreference(
    preferenceScreen: PreferenceScreen,
    context: Context,
    title: String,
    icon: Drawable?,
    summary: String,
    key: String,
) {
    val existingPreference = preferenceScreen.findPreference<Preference>(key)
    if (existingPreference != null) {
        existingPreference.apply {
            setIcon(icon)
            setTitle(title)
            setSummary(summary)
        }
    } else {
        val newPreference =
            createPreference(context, key).apply {
                setIcon(icon)
                setTitle(title)
                setSummary(summary)
                this.key = key
                order = 0
            }

        preferenceScreen.addPreference(newPreference)
    }
}

private fun createPreference(context: Context, key: String): Preference {
    return when (key) {
        INTRO_PREFERENCE_KEY ->
            IntroPreference(context).also {
                it.setCollapsable(false)
                it.setMinLines(4)
            }
        APP_HEADER_PREFERENCE_KEY -> AppHeaderPreference(context)
        PERMISSION_HEADER_PREFERENCE_KEY -> PermissionHeaderPreference(context)
        else -> Preference(context)
    }
}

private fun safelyRemovePreference(preferenceScreen: PreferenceScreen, preferenceKey: String) {
    val preference = preferenceScreen.findPreference<Preference>(preferenceKey)
    if (preference != null) {
        preferenceScreen.removePreferenceRecursively(preferenceKey)
    }
}

fun buttonPreference(
    context: Context,
    icon: Drawable?,
    title: String?,
    logName: ElementName?,
    key: String?,
    order: Int?,
    listener: (() -> Unit)?,
): Preference {
    return if (SettingsThemeHelper.isExpressiveTheme(context)) {
        HealthButtonPreference(context).also { preference ->
            icon?.let { preference.icon = it }
            title?.let { preference.title = it }
            logName?.let { preference.logName = it }
            key?.let { preference.key = it }
            order?.let { preference.order = it }
            listener?.let { (preference).setOnClickListener { it() } }
        }
    } else {
        HealthPreference(context).also { preference ->
            icon?.let { preference.icon = it }
            title?.let { preference.title = it }
            logName?.let { preference.logName = it }
            key?.let { preference.key = it }
            order?.let { preference.order = it }
            listener?.let {
                preference.onPreferenceClickListener = OnPreferenceClickListener {
                    it()
                    true
                }
            }
        }
    }
}

private fun isExpressiveAndBuildVersion(context: Context) =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM &&
        SettingsThemeHelper.isExpressiveTheme(context)
