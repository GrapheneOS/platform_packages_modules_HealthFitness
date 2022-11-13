/**
 * Copyright (C) 2022 The Android Open Source Project
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
package com.android.healthconnect.controller.tests.utils

import android.os.Bundle
import com.android.healthconnect.controller.dataentries.DataEntriesFragment
import com.android.healthconnect.controller.utils.DatePickerFactory
import com.android.healthconnect.controller.utils.toLocaleDate
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.Duration.ofDays

@HiltAndroidTest
class DatePickerFactoryTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)


    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun create_respectSelectedDate() {
        launchFragment<DataEntriesFragment>(Bundle()) {
            val selectedDate = NOW
            val maxDate = NOW.plus(ofDays(10))

            val dialog = DatePickerFactory.create(requireContext(), selectedDate, maxDate)

            val localSelectedDate = selectedDate.toLocaleDate()
            assertThat(dialog.datePicker.year).isEqualTo(localSelectedDate.year)
            // date picker saves month starting from 0, local date saves month starting from 1
            assertThat(dialog.datePicker.month).isEqualTo(localSelectedDate.month.value - 1)
            assertThat(dialog.datePicker.dayOfMonth).isEqualTo(localSelectedDate.dayOfMonth)
        }
    }

    @Test
    fun create_respectMaxDate() {
        launchFragment<DataEntriesFragment>(Bundle()) {
            val selectedDate = NOW
            val maxDate = NOW.plus(ofDays(10))

            val dialog = DatePickerFactory.create(requireContext(), selectedDate, maxDate)

            assertThat(dialog.datePicker.maxDate).isEqualTo(maxDate.toEpochMilli())
        }
    }
}