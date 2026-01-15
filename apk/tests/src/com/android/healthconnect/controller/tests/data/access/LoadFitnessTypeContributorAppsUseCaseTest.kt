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
package com.android.healthconnect.controller.tests.data.access

import android.content.Context
import android.health.connect.HealthConnectManager
import android.health.connect.HealthDataCategory
import android.health.connect.HealthPermissionCategory
import android.health.connect.RecordTypeInfoResponse
import android.health.connect.datatypes.HeartRateRecord
import android.health.connect.datatypes.Record
import android.health.connect.datatypes.StepsRecord
import android.health.connect.datatypes.WeightRecord
import android.os.OutcomeReceiver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.data.access.LoadFitnessTypeContributorAppsUseCase
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.shared.app.AppInfoReader
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME_2
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME_3
import com.android.healthconnect.controller.tests.utils.createFakeAppInfoReader
import com.android.healthconnect.controller.tests.utils.getDataOrigin
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.invocation.InvocationOnMock
import org.mockito.kotlin.any

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class LoadFitnessTypeContributorAppsUseCaseTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @BindValue lateinit var appInfoReader: AppInfoReader
    private lateinit var context: Context
    private val healthConnectManager: HealthConnectManager =
        Mockito.mock(HealthConnectManager::class.java)
    private lateinit var loadFitnessTypeContributorAppsUseCase:
        LoadFitnessTypeContributorAppsUseCase

    @Before
    fun setup() = runTest {
        MockitoAnnotations.initMocks(this)
        context = InstrumentationRegistry.getInstrumentation().context
        appInfoReader = createFakeAppInfoReader()
        hiltRule.inject()
        loadFitnessTypeContributorAppsUseCase =
            LoadFitnessTypeContributorAppsUseCase(
                appInfoReader,
                healthConnectManager,
                Dispatchers.Main,
            )
    }

    @Test
    fun loadPermissionTypeContributorAppsUseCase_noRecordsStored_returnsEmptyMap() = runTest {
        Mockito.doAnswer(prepareAnswer(mapOf()))
            .`when`(healthConnectManager)
            .queryAllRecordTypesInfo(any(), any())

        val result = loadFitnessTypeContributorAppsUseCase.invoke(FitnessPermissionType.STEPS)
        val expected = listOf<AppMetadata>()
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun loadPermissionTypeContributorAppsUseCase_returnsCorrectApps() = runTest {
        val recordTypeInfoMap: Map<Class<out Record>, RecordTypeInfoResponse> =
            mapOf(
                StepsRecord::class.java to
                    RecordTypeInfoResponse(
                        HealthPermissionCategory.STEPS,
                        HealthDataCategory.ACTIVITY,
                        listOf(
                            getDataOrigin(TEST_APP_PACKAGE_NAME),
                            getDataOrigin(TEST_APP_PACKAGE_NAME_2),
                        ),
                    ),
                WeightRecord::class.java to
                    RecordTypeInfoResponse(
                        HealthPermissionCategory.WEIGHT,
                        HealthDataCategory.BODY_MEASUREMENTS,
                        listOf((getDataOrigin(TEST_APP_PACKAGE_NAME_2))),
                    ),
                HeartRateRecord::class.java to
                    RecordTypeInfoResponse(
                        HealthPermissionCategory.HEART_RATE,
                        HealthDataCategory.VITALS,
                        listOf((getDataOrigin(TEST_APP_PACKAGE_NAME_3))),
                    ),
            )
        Mockito.doAnswer(prepareAnswer(recordTypeInfoMap))
            .`when`(healthConnectManager)
            .queryAllRecordTypesInfo(any(), any())

        val result = loadFitnessTypeContributorAppsUseCase.invoke(FitnessPermissionType.STEPS)
        assertThat(result.size).isEqualTo(2)
        assertThat(result[0].packageName).isEqualTo(TEST_APP_PACKAGE_NAME)
        assertThat(result[1].packageName).isEqualTo(TEST_APP_PACKAGE_NAME_2)
    }

    private fun prepareAnswer(
        map: Map<Class<out Record>, RecordTypeInfoResponse>
    ): (InvocationOnMock) -> Map<Class<out Record>, RecordTypeInfoResponse> {
        val answer = { args: InvocationOnMock ->
            val receiver =
                args.arguments[1]
                    as OutcomeReceiver<Map<Class<out Record>, RecordTypeInfoResponse>, *>
            receiver.onResult(map)
            map
        }
        return answer
    }
}
