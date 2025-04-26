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
import androidx.preference.Preference
import androidx.preference.PreferenceScreen

object PreferenceScreenExtensions {
    /** Updates the title of the top intro preference or creates and adds it if not present. */
    fun PreferenceScreen.updateTopIntro(context: Context, key: String, title: String) {
        var topIntroPref: Preference? = findPreference(key)

        if (topIntroPref != null) {
            topIntroPref.title = title
        } else {
            topIntroPref =
                topIntroPreference(preferenceKey = key, context = context, preferenceTitle = title)
            addPreference(topIntroPref)
        }
    }

    /** Updates the visibility of the preference. If the preference does not exist, no-op. */
    fun PreferenceScreen.updateVisibility(key: String, isVisible: Boolean) {
        findPreference<Preference>(key)?.isVisible = isVisible
    }
}
