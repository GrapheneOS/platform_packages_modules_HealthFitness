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

package com.android.healthconnect.controller.tests.data.formatters.medical

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.data.formatters.medical.PrettyFhirFormatter
import com.android.healthconnect.controller.data.formatters.medical.PrettyJsonGroup
import com.android.healthconnect.controller.data.formatters.medical.PrettyJsonLine
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class PrettyFhirFormatterTest {

    private lateinit var prettyFhirFormatter: PrettyFhirFormatter
    @get:Rule val hiltRule = HiltAndroidRule(this)
    private lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().context
        prettyFhirFormatter = PrettyFhirFormatter(context)
        hiltRule.inject()
    }

    @Test
    fun validJsonGroup_returnsValidFormattedPrettyFhir() {
        val prettyJsonGroup =
            PrettyJsonGroup(
                nestedLines =
                    listOf(
                        PrettyJsonLine(depth = 0, line = "Clinical Status:"),
                        PrettyJsonLine(depth = 1, line = "Coding:"),
                        PrettyJsonLine(depth = 2, line = "System: someUrl"),
                        PrettyJsonLine(depth = 2, line = "Code: Active"),
                        PrettyJsonLine(depth = 3, line = "Display: Active"),
                    )
            )

        val formattedPrettyFhir = prettyFhirFormatter.format(prettyJsonGroup)

        assert(formattedPrettyFhir.header == "Clinical Status:")
        assert(
            formattedPrettyFhir.content.nestedLines ==
                listOf(
                    PrettyJsonLine(depth = 1, line = "Coding:"),
                    PrettyJsonLine(depth = 2, line = "System: someUrl"),
                    PrettyJsonLine(depth = 2, line = "Code: Active"),
                    PrettyJsonLine(depth = 3, line = "Display: Active"),
                )
        )
    }
}
