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

package com.android.healthconnect.controller.tests.onboarding

import android.content.Context
import android.os.Build
import android.view.View
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.isVisible
import androidx.preference.PreferenceViewHolder
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.onboarding.FitnessAppOnboardingViewModel
import com.android.healthconnect.controller.onboarding.PermissionDetailsPreference
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType.DISTANCE
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.permissions.data.PermissionsAccessType.READ
import com.android.healthconnect.controller.permissions.data.PermissionsAccessType.WRITE
import com.android.healthconnect.controller.tests.utils.TEST_APP
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class PermissionDetailsPreferenceTest {
    @get:Rule val hiltRule = HiltAndroidRule(this)
    private lateinit var context: Context
    private lateinit var holder: PreferenceViewHolder

    @Before
    fun setup() {
        hiltRule.inject()
        context = ContextThemeWrapper(getApplicationContext(), R.style.Theme_HealthConnect)
        holder =
            PreferenceViewHolder.createInstanceForTests(
                View.inflate(
                    context,
                    R.layout.widget_request_permissions_details,
                    /* parent= */ null,
                )
            )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
    @Test
    fun readOnly_noHistory_baklava_showsCorrectText() {
        // TODO: b/425856998 - remove when robolectric supports @SdkSuppress
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA)
        val pref = PermissionDetailsPreference(context)
        pref.bind(
            appMetadata = TEST_APP,
            screenState =
                FitnessAppOnboardingViewModel.FitnessAppOnboardingFragmentState.ShowFitnessRead(
                    appMetadata = TEST_APP,
                    permissionsMap =
                        mapOf(HealthPermission.FitnessPermission(DISTANCE, READ) to false),
                    historyGranted = false,
                ),
        )
        pref.onBindViewHolder(holder)
        assertThat(getText(R.id.data_access_type))
            .isEqualTo(
                "Choose which fitness and wellness data this app can access. This includes data tracked and stored on this device, learn more about how your data is accessed"
            )
        assertThat(getText(R.id.access_info))
            .isEqualTo(
                "If you give read access, the app can read new data and data from the past 30 days"
            )
        assertThat(getText(R.id.privacy_policy))
            .isEqualTo("You can learn how $TEST_APP_NAME handles your data in their privacy policy")
    }

    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @Test
    fun readOnly_noHistory_legacy_showsCorrectText() {
        // TODO: b/425856998 - remove when robolectric supports @SdkSuppress
        assumeTrue(Build.VERSION.SDK_INT <= Build.VERSION_CODES.VANILLA_ICE_CREAM)
        val pref = PermissionDetailsPreference(context)
        pref.bind(
            appMetadata = TEST_APP,
            screenState =
                FitnessAppOnboardingViewModel.FitnessAppOnboardingFragmentState.ShowFitnessRead(
                    appMetadata = TEST_APP,
                    permissionsMap =
                        mapOf(HealthPermission.FitnessPermission(DISTANCE, READ) to false),
                    historyGranted = false,
                ),
        )
        pref.onBindViewHolder(holder)
        assertThat(getText(R.id.data_access_type))
            .isEqualTo("Choose data you want this app to read from Health\u00A0Connect")
        assertThat(getText(R.id.access_info))
            .isEqualTo(
                "If you give read access, the app can read new data and data from the past 30 days"
            )
        assertThat(getText(R.id.privacy_policy))
            .isEqualTo("You can learn how $TEST_APP_NAME handles your data in their privacy policy")
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
    @Test
    fun readOnly_withHistory_baklava_showsCorrectText() {
        // TODO: b/425856998 - remove when robolectric supports @SdkSuppress
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA)
        val pref = PermissionDetailsPreference(context)
        pref.bind(
            appMetadata = TEST_APP,
            screenState =
                FitnessAppOnboardingViewModel.FitnessAppOnboardingFragmentState.ShowFitnessRead(
                    appMetadata = TEST_APP,
                    permissionsMap =
                        mapOf(HealthPermission.FitnessPermission(DISTANCE, READ) to false),
                    historyGranted = true,
                ),
        )
        pref.onBindViewHolder(holder)
        assertThat(getText(R.id.data_access_type))
            .isEqualTo(
                "Choose which fitness and wellness data this app can access. This includes data tracked and stored on this device, learn more about how your data is accessed"
            )
        assertThat(getText(R.id.access_info))
            .isEqualTo("If you give read access, the app can read new and past data")
        assertThat(getText(R.id.privacy_policy))
            .isEqualTo("You can learn how $TEST_APP_NAME handles your data in their privacy policy")
    }

    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @Test
    fun readOnly_withHistory_legacy_showsCorrectText() {
        // TODO: b/425856998 - remove when robolectric supports @SdkSuppress
        assumeTrue(Build.VERSION.SDK_INT <= Build.VERSION_CODES.VANILLA_ICE_CREAM)
        val pref = PermissionDetailsPreference(context)
        pref.bind(
            appMetadata = TEST_APP,
            screenState =
                FitnessAppOnboardingViewModel.FitnessAppOnboardingFragmentState.ShowFitnessRead(
                    appMetadata = TEST_APP,
                    permissionsMap =
                        mapOf(HealthPermission.FitnessPermission(DISTANCE, READ) to false),
                    historyGranted = true,
                ),
        )
        pref.onBindViewHolder(holder)
        assertThat(getText(R.id.data_access_type))
            .isEqualTo("Choose data you want this app to read from Health\u00A0Connect")
        assertThat(getText(R.id.access_info))
            .isEqualTo("If you give read access, the app can read new and past data")
        assertThat(getText(R.id.privacy_policy))
            .isEqualTo("You can learn how $TEST_APP_NAME handles your data in their privacy policy")
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
    @Test
    fun writeOnly_baklava_showsCorrectText() {
        // TODO: b/425856998 - remove when robolectric supports @SdkSuppress
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA)
        val pref = PermissionDetailsPreference(context)
        pref.bind(
            appMetadata = TEST_APP,
            screenState =
                FitnessAppOnboardingViewModel.FitnessAppOnboardingFragmentState.ShowFitnessWrite(
                    appMetadata = TEST_APP,
                    permissionsMap =
                        mapOf(HealthPermission.FitnessPermission(DISTANCE, WRITE) to false),
                ),
        )
        pref.onBindViewHolder(holder)
        assertThat(getText(R.id.data_access_type))
            .isEqualTo(
                "Choose which fitness and wellness data this app can access. This includes data tracked and stored on this device, learn more about how your data is accessed"
            )
        assertThat((holder.findViewById(R.id.access_info))?.isVisible).isFalse()
        assertThat(getText(R.id.privacy_policy))
            .isEqualTo("You can learn how $TEST_APP_NAME handles your data in their privacy policy")
    }

    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @Test
    fun writeOnly_legacy_showsCorrectText() {
        // TODO: b/425856998 - remove when robolectric supports @SdkSuppress
        assumeTrue(Build.VERSION.SDK_INT <= Build.VERSION_CODES.VANILLA_ICE_CREAM)
        val pref = PermissionDetailsPreference(context)
        pref.bind(
            appMetadata = TEST_APP,
            screenState =
                FitnessAppOnboardingViewModel.FitnessAppOnboardingFragmentState.ShowFitnessWrite(
                    appMetadata = TEST_APP,
                    permissionsMap =
                        mapOf(HealthPermission.FitnessPermission(DISTANCE, WRITE) to false),
                ),
        )
        pref.onBindViewHolder(holder)
        assertThat(getText(R.id.data_access_type))
            .isEqualTo("Choose data you want this app to write to Health\u00A0Connect")
        assertThat((holder.findViewById(R.id.access_info))?.isVisible).isFalse()
        assertThat(getText(R.id.privacy_policy))
            .isEqualTo("You can learn how $TEST_APP_NAME handles your data in their privacy policy")
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
    @Test
    fun readWrite_noHistory_baklava_showsCorrectText() {
        // TODO: b/425856998 - remove when robolectric supports @SdkSuppress
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA)
        val pref = PermissionDetailsPreference(context)
        pref.bind(
            appMetadata = TEST_APP,
            screenState =
                FitnessAppOnboardingViewModel.FitnessAppOnboardingFragmentState
                    .ShowFitnessReadWrite(
                        appMetadata = TEST_APP,
                        permissionsMap =
                            mapOf(
                                HealthPermission.FitnessPermission(DISTANCE, READ) to false,
                                HealthPermission.FitnessPermission(DISTANCE, WRITE) to false,
                            ),
                        historyGranted = false,
                    ),
        )
        pref.onBindViewHolder(holder)
        assertThat(getText(R.id.data_access_type))
            .isEqualTo(
                "Choose which fitness and wellness data this app can access. This includes data tracked and stored on this device, learn more about how your data is accessed"
            )
        assertThat(getText(R.id.access_info))
            .isEqualTo(
                "If you give read access, the app can read new data and data from the past 30 days"
            )
        assertThat(getText(R.id.privacy_policy))
            .isEqualTo("You can learn how $TEST_APP_NAME handles your data in their privacy policy")
    }

    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @Test
    fun readWrite_noHistory_legacy_showsCorrectText() {
        // TODO: b/425856998 - remove when robolectric supports @SdkSuppress
        assumeTrue(Build.VERSION.SDK_INT <= Build.VERSION_CODES.VANILLA_ICE_CREAM)
        val pref = PermissionDetailsPreference(context)
        pref.bind(
            appMetadata = TEST_APP,
            screenState =
                FitnessAppOnboardingViewModel.FitnessAppOnboardingFragmentState
                    .ShowFitnessReadWrite(
                        appMetadata = TEST_APP,
                        permissionsMap =
                            mapOf(
                                HealthPermission.FitnessPermission(DISTANCE, READ) to false,
                                HealthPermission.FitnessPermission(DISTANCE, WRITE) to false,
                            ),
                        historyGranted = false,
                    ),
        )
        pref.onBindViewHolder(holder)
        assertThat(getText(R.id.data_access_type))
            .isEqualTo("Choose data you want this app to read or write to Health\u00A0Connect")
        assertThat(getText(R.id.access_info))
            .isEqualTo(
                "If you give read access, the app can read new data and data from the past 30 days"
            )
        assertThat(getText(R.id.privacy_policy))
            .isEqualTo("You can learn how $TEST_APP_NAME handles your data in their privacy policy")
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
    @Test
    fun readWrite_withHistory_baklava_showsCorrectText() {
        // TODO: b/425856998 - remove when robolectric supports @SdkSuppress
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA)
        val pref = PermissionDetailsPreference(context)
        pref.bind(
            appMetadata = TEST_APP,
            screenState =
                FitnessAppOnboardingViewModel.FitnessAppOnboardingFragmentState
                    .ShowFitnessReadWrite(
                        appMetadata = TEST_APP,
                        permissionsMap =
                            mapOf(
                                HealthPermission.FitnessPermission(DISTANCE, READ) to false,
                                HealthPermission.FitnessPermission(DISTANCE, WRITE) to false,
                            ),
                        historyGranted = true,
                    ),
        )
        pref.onBindViewHolder(holder)
        assertThat(getText(R.id.data_access_type))
            .isEqualTo(
                "Choose which fitness and wellness data this app can access. This includes data tracked and stored on this device, learn more about how your data is accessed"
            )
        assertThat(getText(R.id.access_info))
            .isEqualTo("If you give read access, the app can read new and past data")
        assertThat(getText(R.id.privacy_policy))
            .isEqualTo("You can learn how $TEST_APP_NAME handles your data in their privacy policy")
    }

    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @Test
    fun readWrite_withHistory_legacy_showsCorrectText() {
        // TODO: b/425856998 - remove when robolectric supports @SdkSuppress
        assumeTrue(Build.VERSION.SDK_INT <= Build.VERSION_CODES.VANILLA_ICE_CREAM)
        val pref = PermissionDetailsPreference(context)
        pref.bind(
            appMetadata = TEST_APP,
            screenState =
                FitnessAppOnboardingViewModel.FitnessAppOnboardingFragmentState
                    .ShowFitnessReadWrite(
                        appMetadata = TEST_APP,
                        permissionsMap =
                            mapOf(
                                HealthPermission.FitnessPermission(DISTANCE, READ) to false,
                                HealthPermission.FitnessPermission(DISTANCE, WRITE) to false,
                            ),
                        historyGranted = true,
                    ),
        )
        pref.onBindViewHolder(holder)
        assertThat(getText(R.id.data_access_type))
            .isEqualTo("Choose data you want this app to read or write to Health\u00A0Connect")
        assertThat(getText(R.id.access_info))
            .isEqualTo("If you give read access, the app can read new and past data")
        assertThat(getText(R.id.privacy_policy))
            .isEqualTo("You can learn how $TEST_APP_NAME handles your data in their privacy policy")
    }

    private fun getText(viewId: Int): String =
        (holder.findViewById(viewId) as TextView).text.toString()
}
