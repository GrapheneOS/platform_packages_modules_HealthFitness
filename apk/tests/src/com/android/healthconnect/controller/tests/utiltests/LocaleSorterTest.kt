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

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.tests.utils.setLocale
import com.android.healthconnect.controller.utils.LocaleSorter.sortByLocale
import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocaleSorterTest {
    private lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().context
        context.setLocale(Locale.US)
    }

    @After
    fun tearDown() {
        context.setLocale(Locale.US)
    }

    @Test
    fun testSortByLocale_usLocale() {
        val list = listOf("Vitals", "Medications", "Conditions", "Allergies")
        val actual = list.sortByLocale { it }
        assertEquals(listOf("Allergies", "Conditions", "Medications", "Vitals"), actual)
    }

    @Test
    fun testSortByLocale_hunLocale() {
        context.setLocale(Locale("hu", "HU"))
        val list = listOf("Eljárások", "Életfunkciók", "Állapotok", "Allergia")
        val actual = list.sortByLocale { it }
        assertEquals(listOf("Állapotok", "Allergia", "Életfunkciók", "Eljárások"), actual)
    }

    @Test
    fun testSortByLocale_emptyList() {
        val list = emptyList<String>()
        val actual = list.sortByLocale { it }
        assertEquals(emptyList<String>(), actual)
    }
}
