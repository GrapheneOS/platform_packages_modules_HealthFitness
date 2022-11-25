/**
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * ```
 *      http://www.apache.org/licenses/LICENSE-2.0
 * ```
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.tests.dataentries.formatters

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.dataentries.formatters.PowerFormatter
import com.android.healthconnect.controller.dataentries.units.UnitPreferences
import com.android.healthconnect.controller.tests.utils.getBasalMetabolicRateRecord
import com.android.healthconnect.controller.tests.utils.setLocale
import com.google.common.truth.Truth.*
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.ZoneId
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class PowerFormatterTest {
    @get:Rule val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var formatter: PowerFormatter
    @Inject lateinit var preferences: UnitPreferences
    private lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().context
        context.setLocale(Locale.US)
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("UTC")))

        hiltRule.inject()
    }

    @Test
    fun formatValue_returnsPowerValue() {
        val record = getBasalMetabolicRateRecord(10.2)
        runBlocking { assertThat(formatter.formatValue(record, preferences)).isEqualTo("10.2 W") }
    }

    @Test
    fun formatA11yValue_pluralValue_returnsA11yPowerValues() {
        val record = getBasalMetabolicRateRecord(10.1)
        runBlocking {
            assertThat(formatter.formatA11yValue(record, preferences)).isEqualTo("10.1 watts")
        }
    }

    @Test
    fun formatA11yValue_singleValue_returnsA11yPowerValues() {
        val record = getBasalMetabolicRateRecord(1.0)
        runBlocking {
            assertThat(formatter.formatA11yValue(record, preferences)).isEqualTo("1 watt")
        }
    }
}
