/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.tests.datasources.appsources

import android.content.Context
import android.health.connect.HealthDataCategory
import android.view.View
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.isVisible
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceViewHolder
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.datasources.DataSourcesViewModel
import com.android.healthconnect.controller.datasources.appsources.AppSourcesPreferenceCategory
import com.android.healthconnect.controller.shared.app.AppUtils
import com.android.healthconnect.controller.shared.preference.RankedActionPreference
import com.android.healthconnect.controller.tests.utils.TEST_APP
import com.android.healthconnect.controller.tests.utils.TEST_APP_2
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.settingslib.widget.SettingsThemeHelper
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AppSourcesPreferenceCategoryTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    private lateinit var context: Context
    private val logger: HealthConnectLogger = mock()
    private val appUtils: AppUtils = mock()
    private val viewModel: DataSourcesViewModel = mock()

    private lateinit var appSourcesPreferenceCategory: AppSourcesPreferenceCategory

    @Before
    fun setup() {
        hiltRule.inject()
        val applicationContext = getApplicationContext<Context>()
        val theme =
            if (SettingsThemeHelper.isExpressiveTheme(applicationContext)) {
                R.style.Theme_HealthConnect_Expressive_Test
            } else {
                R.style.Theme_HealthConnect
            }
        context = ContextThemeWrapper(applicationContext, theme)

        appSourcesPreferenceCategory = AppSourcesPreferenceCategory(context, null)
        // Preferences must be attached to a PreferenceManager
        val preferenceManager = PreferenceManager(context)
        val screen = preferenceManager.createPreferenceScreen(context)
        screen.addPreference(appSourcesPreferenceCategory)
    }

    @Test
    fun initialize_populatesPreferences() {
        whenever(viewModel.getPriorityList()).thenReturn(listOf(TEST_APP, TEST_APP_2))

        appSourcesPreferenceCategory.initialize(
            logger,
            appUtils,
            viewModel,
            HealthDataCategory.ACTIVITY,
        )
        appSourcesPreferenceCategory.updateApps()

        assertThat(appSourcesPreferenceCategory.preferenceCount).isEqualTo(2)
        assertThat(
                bindAndGetAppName(
                    appSourcesPreferenceCategory.getPreference(0) as RankedActionPreference
                )
            )
            .isEqualTo(TEST_APP.appName)
        assertThat(
                bindAndGetAppName(
                    appSourcesPreferenceCategory.getPreference(1) as RankedActionPreference
                )
            )
            .isEqualTo(TEST_APP_2.appName)
    }

    @Test
    fun initialize_withOneApp_hidesActionButtons() {
        whenever(viewModel.getPriorityList()).thenReturn(listOf(TEST_APP))

        appSourcesPreferenceCategory.initialize(
            logger,
            appUtils,
            viewModel,
            HealthDataCategory.ACTIVITY,
        )
        appSourcesPreferenceCategory.updateApps()

        assertThat(appSourcesPreferenceCategory.preferenceCount).isEqualTo(1)
        val preference = appSourcesPreferenceCategory.getPreference(0) as RankedActionPreference

        val holder = PreferenceViewHolder.createInstanceForTests(inflatePreferenceView())
        preference.onBindViewHolder(holder)

        assertThat(holder.findViewById(R.id.action_icon)?.isVisible).isFalse()
    }

    @Test
    fun initialize_withMultipleApps_showsActionButtons() {
        whenever(viewModel.getPriorityList()).thenReturn(listOf(TEST_APP, TEST_APP_2))

        appSourcesPreferenceCategory.initialize(
            logger,
            appUtils,
            viewModel,
            HealthDataCategory.ACTIVITY,
        )
        appSourcesPreferenceCategory.updateApps()

        assertThat(appSourcesPreferenceCategory.preferenceCount).isEqualTo(2)
        val preference1 = appSourcesPreferenceCategory.getPreference(0) as RankedActionPreference
        val preference2 = appSourcesPreferenceCategory.getPreference(1) as RankedActionPreference

        val holder1 = PreferenceViewHolder.createInstanceForTests(inflatePreferenceView())
        preference1.onBindViewHolder(holder1)
        assertThat(holder1.findViewById(R.id.action_icon)?.isVisible).isTrue()

        val holder2 = PreferenceViewHolder.createInstanceForTests(inflatePreferenceView())
        preference2.onBindViewHolder(holder2)
        assertThat(holder2.findViewById(R.id.action_icon)?.isVisible).isTrue()
    }

    @Test
    fun updateApps_clearsExistingPreferences() {
        whenever(viewModel.getPriorityList()).thenReturn(listOf(TEST_APP, TEST_APP_2))
        appSourcesPreferenceCategory.initialize(
            logger,
            appUtils,
            viewModel,
            HealthDataCategory.ACTIVITY,
        )
        appSourcesPreferenceCategory.updateApps()
        assertThat(appSourcesPreferenceCategory.preferenceCount).isEqualTo(2)

        whenever(viewModel.getPriorityList()).thenReturn(listOf(TEST_APP))
        appSourcesPreferenceCategory.updateApps()
        assertThat(appSourcesPreferenceCategory.preferenceCount).isEqualTo(1)
        assertThat(
                bindAndGetAppName(
                    appSourcesPreferenceCategory.getPreference(0) as RankedActionPreference
                )
            )
            .isEqualTo(TEST_APP.appName)
    }

    private fun inflatePreferenceView(): View {
        val layoutRes =
            if (SettingsThemeHelper.isExpressiveTheme(context)) {
                R.layout.widget_app_source_layout_expressive
            } else {
                R.layout.widget_app_source_layout_legacy
            }
        return View.inflate(context, layoutRes, null)
    }

    private fun bindAndGetAppName(preference: RankedActionPreference): String {
        val holder = PreferenceViewHolder.createInstanceForTests(inflatePreferenceView())
        preference.onBindViewHolder(holder)
        return (holder.findViewById(R.id.app_name) as TextView).text.toString()
    }
}
