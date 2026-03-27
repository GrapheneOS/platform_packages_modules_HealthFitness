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

package com.android.healthconnect.controller.tests.matchmaking

import android.content.Context
import android.text.SpannableString
import android.view.LayoutInflater
import android.widget.TextView
import androidx.preference.PreferenceViewHolder
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.matchmaking.MatchmakingPrivacyFooterPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MatchmakingPrivacyFooterPreferenceTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.setTheme(R.style.Theme_HealthConnect)
    }

    @Test
    fun isSelectable_returnsFalse() {
        val preference = MatchmakingPrivacyFooterPreference(context)
        assertThat(preference.isSelectable).isFalse()
    }

    @Test
    fun onBindViewHolder_disablesSummaryClickAndFocus() {
        val preference = MatchmakingPrivacyFooterPreference(context)
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.matchmaking_privacy_footer, null)
        val holder = PreferenceViewHolder.createInstanceForTests(view)

        preference.setAppName("Test App") {}
        preference.onBindViewHolder(holder)

        val summaryView = view.findViewById<TextView>(R.id.summary)
        assertThat(summaryView.isClickable).isFalse()
        assertThat(summaryView.isFocusable).isFalse()
    }

    @Test
    fun onBindViewHolder_setsClickableSpan() {
        val preference = MatchmakingPrivacyFooterPreference(context)
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.matchmaking_privacy_footer, null)
        val holder = PreferenceViewHolder.createInstanceForTests(view)

        preference.setAppName("Test App") {}
        preference.onBindViewHolder(holder)

        val summaryView = view.findViewById<TextView>(R.id.summary)
        val text = summaryView.text as SpannableString
        val spans = text.getSpans(0, text.length, android.text.style.ClickableSpan::class.java)
        assertThat(spans.size).isEqualTo(1)
    }
}
