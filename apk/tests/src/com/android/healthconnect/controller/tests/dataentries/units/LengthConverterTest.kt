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
package com.android.healthconnect.controller.tests.dataentries.units

import com.android.healthconnect.controller.dataentries.units.LengthConverter.convertDistanceFromMeters
import com.android.healthconnect.controller.dataentries.units.LengthUnit.IMPERIAL_LENGTH_UNIT
import com.android.healthconnect.controller.dataentries.units.LengthUnit.METRIC_LENGTH_UNIT
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LengthConverterTest {

    @Test
    fun convertDistanceFromMeters_toMiles() {
        val value = convertDistanceFromMeters(IMPERIAL_LENGTH_UNIT, 1234.0)
        assertThat(value).isWithin(/*tolerance*/ 0.1).of(/*expected*/ 0.8)
    }

    @Test
    fun convertDistanceFromMeters_toKilometers() {
        val value = convertDistanceFromMeters(METRIC_LENGTH_UNIT, 1234.0)
        assertThat(value).isEqualTo(1.234)
    }
}
