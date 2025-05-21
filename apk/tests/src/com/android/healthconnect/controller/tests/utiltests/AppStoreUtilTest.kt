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

package com.android.healthconnect.controller.tests.utiltests

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.utils.AppStoreUtils
import com.android.healthconnect.controller.utils.DeviceInfoUtils
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AppStoreUtilTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var appStoreUtils: AppStoreUtils
    @Inject lateinit var deviceInfoUtils: DeviceInfoUtils
    private lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().context
        hiltRule.inject()
    }

    @Test
    fun getAppStoreLink_validPackage_returnsCorrectIntent() {
        // skip the test on AOSP devices
        if (!deviceInfoUtils.isPlayStoreAvailable(context)) {
            return
        }

        val intent = appStoreUtils.getAppStoreLink(TEST_APP_PACKAGE_NAME)

        assertThat(intent).isNotNull()
        assertThat(intent!!.action).isEqualTo("android.intent.action.SHOW_APP_INFO")
        assertThat(intent.extras?.get("android.intent.extra.PACKAGE_NAME"))
            .isEqualTo(TEST_APP_PACKAGE_NAME)
    }
}
