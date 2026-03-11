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

package com.android.healthconnect.controller.tests.home

import android.content.Context
import androidx.navigation.NavController
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.home.BannerFactory
import com.android.healthconnect.controller.home.BannerFactory.Companion.DATA_RESTORE_BANNER_PREFERENCE_KEY
import com.android.healthconnect.controller.home.BannerFactory.Companion.EXPORT_ERROR_BANNER_PREFERENCE_KEY
import com.android.healthconnect.controller.home.BannerFactory.Companion.LOCK_SCREEN_BANNER_KEY
import com.android.healthconnect.controller.home.BannerFactory.Companion.MIGRATION_BANNER_PREFERENCE_KEY
import com.android.healthconnect.controller.home.BannerFactory.Companion.NATIVE_STEPS_BANNER_KEY
import com.android.healthconnect.controller.home.BannerFactory.Companion.ONBOARDING_ONE_APP_BANNER_KEY
import com.android.healthconnect.controller.home.BannerFactory.Companion.ONBOARDING_ZERO_APPS_BANNER_KEY
import com.android.healthconnect.controller.home.HomeViewModel
import com.android.healthconnect.controller.home.HomeViewModel.BannerData
import com.android.healthconnect.controller.tests.utils.NOW
import com.android.healthconnect.controller.tests.utils.setLocale
import com.android.healthconnect.controller.utils.LocalDateTimeFormatter
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.ZoneId
import java.util.Locale
import java.util.TimeZone
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class BannerFactoryTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    private lateinit var context: Context
    private lateinit var navController: NavController
    private lateinit var dateFormatter: LocalDateTimeFormatter
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var bannerFactory: BannerFactory

    @Before
    fun setup() {
        hiltRule.inject()
        context = getApplicationContext()

        context.setLocale(Locale.US)
        dateFormatter = LocalDateTimeFormatter(context)
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("UTC")))

        navController = mock()
        homeViewModel = mock()
        bannerFactory = BannerFactory(context, dateFormatter) {}
    }

    @Test
    fun getExportFileAccessErrorBanner_returnsCorrectBanner() {
        val banner = bannerFactory.getBanner(BannerData.ExportErrorBanner(NOW))

        assertThat(banner.title).isEqualTo("Couldn\'t export data")
        assertThat(banner.summary)
            .isEqualTo(
                "There was a problem with the export for October 20, 2022. " +
                    "Please set up a new scheduled export and try again."
            )
        assertThat(banner.key).isEqualTo(EXPORT_ERROR_BANNER_PREFERENCE_KEY)
    }

    @Test
    fun getMigrationBanner_returnsCorrectBanner() {
        val banner = bannerFactory.getBanner(BannerData.MigrationBanner)

        assertThat(banner.title).isEqualTo("Resume integration")
        assertThat(banner.summary)
            .isEqualTo("Tap to continue integrating Health Connect with the Android system.")
        assertThat(banner.key).isEqualTo(MIGRATION_BANNER_PREFERENCE_KEY)
    }

    @Test
    fun getDataRestorePendingBanner_returnsCorrectBanner() {
        val banner = bannerFactory.getBanner(BannerData.DataRestorePendingBanner)

        assertThat(banner.title).isEqualTo("Update needed")
        assertThat(banner.summary)
            .isEqualTo("To continue restoring your data, update your device system.")
        assertThat(banner.key).isEqualTo(DATA_RESTORE_BANNER_PREFERENCE_KEY)
    }

    @Test
    fun getLockScreenBanner_returnsCorrectBanner() {
        val bannerData = BannerData.LockScreenBanner(true, false)
        val banner = bannerFactory.getBanner(bannerData)

        assertThat(banner.title).isEqualTo("Set a screen lock")
        assertThat(banner.summary)
            .isEqualTo(
                "For added security for your health data, " +
                    "set a PIN, pattern, or password for this device"
            )
        assertThat(banner.key).isEqualTo(LOCK_SCREEN_BANNER_KEY)
    }

    @Test
    fun getNativeStepsBanner_returnsCorrectBanner() {
        val bannerData = BannerData.NativeStepsBanner
        val banner = bannerFactory.getBanner(bannerData)

        assertThat(banner.title)
            .isEqualTo("Steps tracked on your phone will appear in Health Connect")
        assertThat(banner.summary)
            .isEqualTo(
                "Steps tracked by this device are now stored in Health Connect for connected apps to access"
            )
        assertThat(banner.key).isEqualTo(NATIVE_STEPS_BANNER_KEY)
    }

    @Test
    fun getZeroAppsOnboardingBanner_returnsCorrectBanner() {
        val bannerData = BannerData.ZeroAppsOnboardingBanner
        val banner = bannerFactory.getBanner(bannerData)

        assertThat(banner.title).isEqualTo("See your health data across apps")
        assertThat(banner.summary)
            .isEqualTo("Start sharing fitness and wellness data between your apps")
        assertThat(banner.key).isEqualTo(ONBOARDING_ZERO_APPS_BANNER_KEY)
    }

    @Test
    fun getOneAppConnectedBanner_returnsCorrectBanner() {
        val bannerData = BannerData.OneAppOnboardingBanner
        val banner = bannerFactory.getBanner(bannerData)

        assertThat(banner.title).isEqualTo("Connect a second app")
        assertThat(banner.summary)
            .isEqualTo("Set up another app so it can start sharing fitness and wellness data")
        assertThat(banner.key).isEqualTo(ONBOARDING_ONE_APP_BANNER_KEY)
    }
}
