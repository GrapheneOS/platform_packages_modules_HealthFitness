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

package com.android.healthconnect.controller.newHome

import android.content.Context
import android.content.Intent
import android.health.connect.HealthConnectManager
import android.provider.Settings
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.newHome.HomeViewModel.BannerData
import com.android.healthconnect.controller.shared.preference.HealthBannerPreference
import com.android.healthconnect.controller.utils.AttributeResolver
import com.android.healthconnect.controller.utils.LocalDateTimeFormatter
import com.android.healthconnect.controller.utils.SettingsTransitionHelper.createMainlineServiceUpdateSettingsIntent
import com.android.healthconnect.controller.utils.logging.DataRestoreElement
import com.android.healthconnect.controller.utils.logging.HomePageElement
import com.android.healthconnect.controller.utils.logging.MigrationElement
import java.time.Instant

/** Sealed class for banner actions. */
sealed class BannerAction {
    data class Navigate(val destinationId: Int) : BannerAction()

    data class StartActivity(val intent: Intent) : BannerAction()

    data class Dismiss(val banner: BannerData) : BannerAction()

    data class NavigateAndDismiss(val destinationId: Int, val banner: BannerData) : BannerAction()

    data class StartActivityAndDismiss(val intent: Intent, val banner: BannerData) : BannerAction()
}

/** A class that constructs banners for the HomeFragment */
class BannerFactory(
    private val context: Context,
    private val dateFormatter: LocalDateTimeFormatter,
    private val onAction: (BannerAction) -> Unit,
) {

    companion object {
        const val MIGRATION_BANNER_PREFERENCE_KEY = "migration_banner"
        const val DATA_RESTORE_BANNER_PREFERENCE_KEY = "data_restore_banner"
        const val EXPORT_ERROR_BANNER_PREFERENCE_KEY = "export_error_banner"
        const val LOCK_SCREEN_BANNER_KEY = "lock_screen_banner"
        const val ONBOARDING_ZERO_APPS_BANNER_KEY = "onboarding_zero_apps_banner_key"
        const val ONBOARDING_ONE_APP_BANNER_KEY = "onboarding_one_app_banner_key"
        const val NATIVE_STEPS_BANNER_KEY = "native_steps_banner_key"
        private val securitySettingsIntent = Intent(Settings.ACTION_SECURITY_SETTINGS)
        private val onboardingActivityIntent = Intent(HealthConnectManager.ACTION_SYNC_MORE_APPS)
    }

    fun getBanner(bannerData: BannerData): HealthBannerPreference {
        return when (bannerData) {
            is BannerData.LockScreenBanner -> getLockScreenBanner(bannerData)
            is BannerData.NativeStepsBanner -> getNativeStepsBanner(bannerData)
            is BannerData.ZeroAppsOnboardingBanner -> getZeroAppsOnboardingBanner(bannerData)
            is BannerData.OneAppOnboardingBanner -> getOneAppConnectedBanner(bannerData)
            is BannerData.MigrationBanner -> getMigrationBanner()
            is BannerData.DataRestorePendingBanner -> getDataRestorePendingBanner()
            is BannerData.ExportErrorBanner ->
                getExportFileAccessErrorBanner(bannerData.lastFailedExportTime)
        }
    }

    private fun getExportFileAccessErrorBanner(
        lastFailedExportTime: Instant
    ): HealthBannerPreference {
        return HealthBannerPreference(context, HomePageElement.EXPORT_ERROR_BANNER).also { banner ->
            banner.setPositiveButton(
                text = context.getString(R.string.export_file_access_error_banner_button),
                logName = HomePageElement.EXPORT_ERROR_BANNER_BUTTON,
            ) {
                onAction(BannerAction.Navigate(R.id.action_newHomeFragment_to_exportSetupActivity))
            }

            banner.title = context.getString(R.string.export_file_access_error_banner_title)
            banner.summary =
                context.getString(
                    R.string.export_file_access_error_banner_summary,
                    dateFormatter.formatLongDate(lastFailedExportTime),
                )
            banner.icon = AttributeResolver.getNullableDrawable(context, R.attr.warningIcon)
            banner.key = EXPORT_ERROR_BANNER_PREFERENCE_KEY
        }
    }

    private fun getMigrationBanner(): HealthBannerPreference {
        return HealthBannerPreference(context, MigrationElement.MIGRATION_RESUME_BANNER).also {
            banner ->
            banner.setPositiveButton(
                text = context.getString(R.string.resume_migration_banner_button),
                logName = MigrationElement.MIGRATION_RESUME_BANNER_BUTTON,
            ) {
                onAction(BannerAction.Navigate(R.id.action_newHomeFragment_to_migrationActivity))
            }

            banner.icon = AttributeResolver.getNullableDrawable(context, R.attr.settingsAlertIcon)
            banner.title = context.getString(R.string.resume_migration_banner_title)
            banner.summary =
                context.getString(R.string.resume_migration_banner_description_fallback)
            banner.key = MIGRATION_BANNER_PREFERENCE_KEY
        }
    }

    private fun getDataRestorePendingBanner(): HealthBannerPreference {
        return HealthBannerPreference(context, DataRestoreElement.RESTORE_PENDING_BANNER).also {
            banner ->
            banner.setPositiveButton(
                text = context.getString(R.string.data_restore_pending_banner_button),
                logName = DataRestoreElement.RESTORE_PENDING_BANNER_UPDATE_BUTTON,
            ) {
                val intent = context.createMainlineServiceUpdateSettingsIntent()
                onAction(BannerAction.StartActivity(intent))
            }

            banner.icon = AttributeResolver.getNullableDrawable(context, R.attr.updateNeededIcon)
            banner.title = context.getString(R.string.data_restore_pending_banner_title)
            banner.summary = context.getString(R.string.data_restore_pending_banner_content)
            banner.key = DATA_RESTORE_BANNER_PREFERENCE_KEY
        }
    }

    private fun getLockScreenBanner(
        bannerData: BannerData.LockScreenBanner
    ): HealthBannerPreference {
        return HealthBannerPreference(context, HomePageElement.LOCK_SCREEN_BANNER).also { banner ->
            banner.title = context.getString(R.string.lock_screen_banner_title)
            banner.summary = context.getString(R.string.lock_screen_banner_content)
            banner.icon = AttributeResolver.getNullableDrawable(context, R.attr.lockIcon)
            banner.key = LOCK_SCREEN_BANNER_KEY

            banner.setPositiveButton(
                text = context.getString(R.string.lock_screen_banner_button),
                logName = HomePageElement.LOCK_SCREEN_BANNER_BUTTON,
            ) {
                onAction(BannerAction.StartActivityAndDismiss(securitySettingsIntent, bannerData))
            }

            banner.setNegativeButton(
                text = context.getString(R.string.banner_default_dismiss_button),
                logName = HomePageElement.LOCK_SCREEN_BANNER_DISMISS_BUTTON,
            ) {
                onAction(BannerAction.Dismiss(bannerData))
            }
        }
    }

    private fun getNativeStepsBanner(
        bannerData: BannerData.NativeStepsBanner
    ): HealthBannerPreference {
        return HealthBannerPreference(context, HomePageElement.NATIVE_STEPS_BANNER).also { banner ->
            banner.setPositiveButton(
                text = context.getString(R.string.native_steps_banner_review_button),
                logName = HomePageElement.NATIVE_STEPS_BANNER_REVIEW_BUTTON,
            ) {
                onAction(
                    BannerAction.NavigateAndDismiss(
                        R.id.action_newHomeFragment_to_connectedDevicesFragment,
                        bannerData,
                    )
                )
            }

            banner.setNegativeButton(
                text = context.getString(R.string.native_steps_banner_dismiss_button),
                logName = HomePageElement.NATIVE_STEPS_BANNER_DISMISS_BUTTON,
            ) {
                onAction(BannerAction.Dismiss(bannerData))
            }

            banner.title = context.getString(R.string.native_steps_banner_title)
            banner.summary = context.getString(R.string.native_steps_banner_summary)
            banner.icon = AttributeResolver.getNullableDrawable(context, R.attr.healthConnectIcon)
            banner.key = NATIVE_STEPS_BANNER_KEY
        }
    }

    private fun getZeroAppsOnboardingBanner(
        bannerData: BannerData.ZeroAppsOnboardingBanner
    ): HealthBannerPreference {
        return HealthBannerPreference(context, HomePageElement.ZERO_APPS_CONNECTED_BANNER).also {
            banner ->
            banner.setPositiveButton(
                text = context.getString(R.string.zero_apps_onboarding_banner_button),
                logName = HomePageElement.ZERO_APPS_CONNECTED_BANNER_SET_UP_BUTTON,
            ) {
                onAction(BannerAction.StartActivity(onboardingActivityIntent))
            }

            banner.setNegativeButton(
                text = context.getString(R.string.banner_default_dismiss_button),
                logName = HomePageElement.ZERO_APPS_CONNECTED_BANNER_DISMISS_BUTTON,
            ) {
                onAction(BannerAction.Dismiss(bannerData))
            }
            banner.title = context.getString(R.string.zero_apps_onboarding_banner_title)
            banner.summary = context.getString(R.string.zero_apps_onboarding_banner_summary)
            banner.icon = AttributeResolver.getNullableDrawable(context, R.attr.healthConnectIcon)
            banner.key = ONBOARDING_ZERO_APPS_BANNER_KEY
        }
    }

    private fun getOneAppConnectedBanner(
        bannerData: BannerData.OneAppOnboardingBanner
    ): HealthBannerPreference {
        return HealthBannerPreference(context, HomePageElement.ONE_APP_CONNECTED_BANNER).also {
            banner ->
            banner.setPositiveButton(
                text = context.getString(R.string.one_app_onboarding_banner_button),
                logName = HomePageElement.ONE_APP_CONNECTED_BANNER_SET_UP_BUTTON,
            ) {
                onAction(BannerAction.StartActivity(onboardingActivityIntent))
            }

            banner.setNegativeButton(
                text = context.getString(R.string.banner_default_dismiss_button),
                logName = HomePageElement.ONE_APP_CONNECTED_BANNER_DISMISS_BUTTON,
            ) {
                onAction(BannerAction.Dismiss(bannerData))
            }
            banner.title = context.getString(R.string.one_app_onboarding_banner_title)
            banner.summary = context.getString(R.string.one_app_onboarding_banner_summary)
            banner.icon = AttributeResolver.getNullableDrawable(context, R.attr.syncIcon)
            banner.key = ONBOARDING_ONE_APP_BANNER_KEY
        }
    }
}
