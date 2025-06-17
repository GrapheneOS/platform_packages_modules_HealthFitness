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
package com.android.healthconnect.controller.tests.data.formatters.medical

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.data.entries.FormattedEntry
import com.android.healthconnect.controller.data.formatters.medical.DisplayNameExtractor
import com.android.healthconnect.controller.data.formatters.medical.MedicalEntryFormatter
import com.android.healthconnect.controller.shared.app.AppInfoReader
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.shared.app.MedicalDataSourceReader
import com.android.healthconnect.controller.tests.utils.CoroutineTestRule
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.TEST_MEDICAL_DATA_SOURCE
import com.android.healthconnect.controller.tests.utils.TEST_MEDICAL_RESOURCE_IMMUNIZATION_LONG
import com.android.healthconnect.controller.tests.utils.createFakeAppInfoReader
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MedicalEntryFormatterTest {

    @get:Rule val coroutineTestRule = CoroutineTestRule()
    @get:Rule val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var displayNameExtractor: DisplayNameExtractor
    private lateinit var formatter: MedicalEntryFormatter

    private val medicalDataSourceReader: MedicalDataSourceReader =
        Mockito.mock(MedicalDataSourceReader::class.java)

    @BindValue lateinit var appInfoReader: AppInfoReader
    private lateinit var context: Context

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() = runTest {
        MockitoAnnotations.initMocks(this)
        context = InstrumentationRegistry.getInstrumentation().context
        appInfoReader = createFakeAppInfoReader()
        hiltRule.inject()
        formatter =
            MedicalEntryFormatter(
                medicalDataSourceReader,
                appInfoReader,
                displayNameExtractor,
                context,
            )
    }

    @Test
    fun formatRecord_showDataOrigin() = runTest {
        whenever(medicalDataSourceReader.fromDataSourceId(TEST_MEDICAL_DATA_SOURCE.id))
            .thenReturn(listOf(TEST_MEDICAL_DATA_SOURCE))

        val result =
            formatter.formatResource(TEST_MEDICAL_RESOURCE_IMMUNIZATION_LONG, showDataOrigin = true)

        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(result)
            .isEqualTo(
                FormattedEntry.FormattedMedicalDataEntry(
                    header = "Health Connect test app • App A Data Source",
                    headerA11y = "Health Connect test app • App A Data Source",
                    title = "Tdap",
                    titleA11y = "Tdap",
                    medicalResourceId = TEST_MEDICAL_RESOURCE_IMMUNIZATION_LONG.id,
                )
            )
    }

    @Test
    fun formatRecord_hideOrigin() = runTest {
        whenever(medicalDataSourceReader.fromDataSourceId(TEST_MEDICAL_DATA_SOURCE.id))
            .thenReturn(listOf(TEST_MEDICAL_DATA_SOURCE))

        assertThat(
                formatter.formatResource(
                    TEST_MEDICAL_RESOURCE_IMMUNIZATION_LONG,
                    showDataOrigin = false,
                )
            )
            .isEqualTo(
                FormattedEntry.FormattedMedicalDataEntry(
                    header = "App A Data Source",
                    headerA11y = "App A Data Source",
                    title = "Tdap",
                    titleA11y = "Tdap",
                    medicalResourceId = TEST_MEDICAL_RESOURCE_IMMUNIZATION_LONG.id,
                )
            )
    }
}
