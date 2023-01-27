/**
 * Copyright (C) 2022 The Android Open Source Project
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
package com.android.healthconnect.controller.tests.dataentries.formatters

import android.content.Context
import android.health.connect.datatypes.ElevationGainedRecord
import android.health.connect.datatypes.units.Length
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.dataentries.formatters.ElevationGainedFormatter
import com.android.healthconnect.controller.dataentries.units.UnitPreferences
import com.android.healthconnect.controller.tests.utils.NOW
import com.android.healthconnect.controller.tests.utils.getMetaData
import com.android.healthconnect.controller.tests.utils.setLocale
import com.google.common.truth.Truth.assertThat
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
class ElevationFormatterTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var formatter: ElevationGainedFormatter
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
    fun testFormatValue() = runBlocking {
        assertThat(formatter.formatValue(getRecord(1500.0), preferences)).isEqualTo("1,500 m")
    }

    @Test
    fun testFormatValue_small() = runBlocking {
        assertThat(formatter.formatValue(getRecord(0.3), preferences)).isEqualTo("0.3 m")
    }

    @Test
    fun testFormatValue_one() = runBlocking {
        assertThat(formatter.formatValue(getRecord(1.0), preferences)).isEqualTo("1 m")
    }

    @Test
    fun testFormatA11yValue() = runBlocking {
        assertThat(formatter.formatA11yValue(getRecord(1500.0), preferences))
            .isEqualTo("1,500 meters")
    }

    @Test
    fun testFormatA11yValue_small() = runBlocking {
        assertThat(formatter.formatA11yValue(getRecord(0.3), preferences)).isEqualTo("0.3 meters")
    }

    @Test
    fun testFormatA11yValue_one() = runBlocking {
        assertThat(formatter.formatA11yValue(getRecord(1.0), preferences)).isEqualTo("1 meter")
    }

    private fun getRecord(meters: Double): ElevationGainedRecord {
        return ElevationGainedRecord.Builder(
                getMetaData(), NOW, NOW.plusSeconds(2), Length.fromMeters(meters))
            .build()
    }
}
