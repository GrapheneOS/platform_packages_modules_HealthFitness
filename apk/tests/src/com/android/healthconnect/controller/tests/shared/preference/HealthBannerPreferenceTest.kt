/*
 * Copyright (C) 2026 The Android Open Source Project
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
package com.android.healthconnect.controller.tests.shared.preference

import android.content.Context
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.shared.preference.HealthBannerPreference
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.HomePageElement
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class HealthBannerPreferenceTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @BindValue val healthConnectLogger: HealthConnectLogger = mock()

    private lateinit var context: Context

    @Before
    fun setup() {
        hiltRule.inject()
        context = ContextThemeWrapper(getApplicationContext(), R.style.Theme_HealthConnect)
    }

    @Test
    fun logsImpression_onAttached_exactlyOnce() {
        val bannerPreference = HealthBannerPreference(context, HomePageElement.LOCK_SCREEN_BANNER)
        bannerPreference.setPositiveButton("Positive", HomePageElement.LOCK_SCREEN_BANNER_BUTTON) {}

        // Manually trigger onAttached multiple times
        bannerPreference.onAttached()
        bannerPreference.onAttached()
        bannerPreference.onAttached()

        // Verify impression logged exactly once despite multiple onAttached calls
        verify(healthConnectLogger, times(1)).logImpression(HomePageElement.LOCK_SCREEN_BANNER)
        verify(healthConnectLogger, times(1))
            .logImpression(HomePageElement.LOCK_SCREEN_BANNER_BUTTON)
    }
}
