/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * ```
 *      http://www.apache.org/licenses/LICENSE-2.0
 * ```
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.tests.utiltests

import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.PageName
import com.android.healthconnect.controller.utils.logging.PageName.ALL_MEDICAL_DATA_PAGE
import com.android.healthconnect.controller.utils.logging.PageName.COMBINED_APP_ACCESS_PAGE
import com.android.healthconnect.controller.utils.logging.PageName.MEDICAL_APP_ACCESS_PAGE
import com.android.healthconnect.controller.utils.logging.PageName.RAW_FHIR_PAGE
import com.android.healthconnect.controller.utils.logging.PageName.REQUEST_MEDICAL_PERMISSIONS_PAGE
import com.android.healthconnect.controller.utils.logging.PageName.REQUEST_WRITE_MEDICAL_PERMISSION_PAGE
import com.android.healthconnect.controller.utils.logging.PageName.SETTINGS_MANAGE_COMBINED_APP_PERMISSIONS_PAGE
import com.android.healthconnect.controller.utils.logging.PageName.SETTINGS_MANAGE_MEDICAL_APP_PERMISSIONS_PAGE
import com.android.healthconnect.controller.utils.logging.PageName.TAB_MEDICAL_ACCESS_PAGE
import com.android.healthconnect.controller.utils.logging.PageName.TAB_MEDICAL_ENTRIES_PAGE
import com.android.healthfitness.flags.Flags
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class HealthConnectLoggerTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)
    @get:Rule val setFlagsRule = SetFlagsRule()

    @Inject lateinit var healthConnectLogger: HealthConnectLogger

    @Before
    fun setup() {
        hiltRule.inject()
    }

    private val phrPages =
        listOf(
            ALL_MEDICAL_DATA_PAGE,
            TAB_MEDICAL_ENTRIES_PAGE,
            TAB_MEDICAL_ACCESS_PAGE,
            RAW_FHIR_PAGE,
            REQUEST_MEDICAL_PERMISSIONS_PAGE,
            COMBINED_APP_ACCESS_PAGE,
            MEDICAL_APP_ACCESS_PAGE,
            SETTINGS_MANAGE_COMBINED_APP_PERMISSIONS_PAGE,
            SETTINGS_MANAGE_MEDICAL_APP_PERMISSIONS_PAGE,
            REQUEST_WRITE_MEDICAL_PERMISSION_PAGE,
        )

    @Test
    @DisableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD_UI_TELEMETRY)
    fun logMedicalPageNames_flagOff_unknownPageNameLogged() {
        phrPages.forEach {
            healthConnectLogger.setPageId(it)

            assertThat(healthConnectLogger.getPageId()).isEqualTo(PageName.UNKNOWN_PAGE)
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD_UI_TELEMETRY)
    fun logMedicalPageNames_flagOn_actualPageNameLogged() {
        phrPages.forEach {
            healthConnectLogger.setPageId(it)

            assertThat(healthConnectLogger.getPageId()).isEqualTo(it)
        }
    }
}
