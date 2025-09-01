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

package com.android.healthconnect.testapps.toolbox.read.dataentries.formatters

import android.content.Context
import android.health.connect.datatypes.NicotineIntakeRecord
import android.health.connect.datatypes.NicotineIntakeRecord.NICOTINE_INTAKE_TYPE_CIGARETTE
import android.health.connect.datatypes.NicotineIntakeRecord.NICOTINE_INTAKE_TYPE_VAPE
import android.health.connect.datatypes.units.Mass.fromGrams
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.testapps.toolbox.utils.GeneralUtils.Companion.getMetaData
import com.google.common.truth.Truth.assertThat
import java.time.Instant
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NicotineIntakeFormatterTest {
    private val formatter = NicotineIntakeFormatter()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val NOW: Instant = Instant.parse("2024-04-10T15:36:18.000Z")

    @Test
    fun formatAllNicotineIntakeValues_returnsCorrectValue() {
        val record =
            NicotineIntakeRecord.Builder(
                    getMetaData(context),
                    NOW,
                    NOW.plusSeconds(30),
                    1,
                    NICOTINE_INTAKE_TYPE_CIGARETTE,
                )
                .setNicotineIntake(fromGrams(0.001))
                .build()

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry.value).isEqualTo("1 Cigarette 1.0 mg")
    }

    @Test
    fun formatMandatoryNicotineIntakeValues_returnsCorrectValue() {
        val record =
            NicotineIntakeRecord.Builder(
                    getMetaData(context),
                    NOW,
                    NOW.plusSeconds(30),
                    3,
                    NICOTINE_INTAKE_TYPE_VAPE,
                )
                .build()

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry.value).isEqualTo("3 Vape 0.0 mg")
    }

    @Test
    fun formatNicotineIntakeValue_returnsCorrectRoundedValue() {
        val record =
            NicotineIntakeRecord.Builder(
                    getMetaData(context),
                    NOW,
                    NOW.plusSeconds(30),
                    2,
                    NICOTINE_INTAKE_TYPE_VAPE,
                )
                .setNicotineIntake(fromGrams(0.00345678))
                .build()

        val formattedEntry = formatter.format(record, context)

        assertThat(formattedEntry.value).isEqualTo("2 Vape 3.457 mg")
    }
}
