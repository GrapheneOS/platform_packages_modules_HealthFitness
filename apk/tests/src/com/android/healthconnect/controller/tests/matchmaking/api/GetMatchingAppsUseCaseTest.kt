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
 * distributed under the License is an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.healthconnect.controller.tests.matchmaking.api

import android.health.connect.HealthConnectException
import android.health.connect.HealthConnectManager
import android.health.connect.HealthPermissions.WRITE_STEPS
import android.health.connect.datatypes.StepsRecord
import android.os.OutcomeReceiver
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.matchmaking.MatchmakingAppData
import com.android.healthconnect.controller.matchmaking.api.GetMatchingAppsUseCase
import com.android.healthconnect.controller.matchmaking.api.GetMatchingAppsUseCase.GetMatchMakingAppsInput
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.shared.app.AppInfoReader
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME_2
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME_2
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class GetMatchingAppsUseCaseTest {

    @get:Rule val hiltRule = dagger.hilt.android.testing.HiltAndroidRule(this)

    private val healthConnectManager: HealthConnectManager = mock()
    private val appInfoReader: AppInfoReader = mock()

    private lateinit var useCase: GetMatchingAppsUseCase

    @Before
    fun setup() {
        hiltRule.inject()
        useCase = GetMatchingAppsUseCase(healthConnectManager, appInfoReader, Dispatchers.Main)
    }

    @Test
    fun invoke_whenSuccessful_returnsSuccess() = runTest {
        val packageName = TEST_APP_PACKAGE_NAME
        val recordTypes = setOf(StepsRecord::class.java)
        val matchingAppsResponse = mapOf(TEST_APP_PACKAGE_NAME_2 to setOf(WRITE_STEPS))
        val appMetadata = AppMetadata(TEST_APP_NAME_2, TEST_APP_PACKAGE_NAME_2, null)
        val expected =
            setOf(
                MatchmakingAppData(
                    appMetadata,
                    setOf(
                        HealthPermission.fromPermissionString(WRITE_STEPS)
                            as HealthPermission.FitnessPermission
                    ),
                )
            )

        doAnswer {
                val receiver =
                    it.arguments[3]
                        as OutcomeReceiver<Map<String, Set<String>>, HealthConnectException>
                receiver.onResult(matchingAppsResponse)
                null
            }
            .whenever(healthConnectManager)
            .getMatchingApps(any(), any(), any(), any())
        whenever(appInfoReader.getAppMetadata(any(), any())).thenReturn(appMetadata)

        val result = useCase.invoke(GetMatchMakingAppsInput(packageName, recordTypes))

        assertThat(result).isInstanceOf(UseCaseResults.Success::class.java)
        assertThat((result as UseCaseResults.Success).data).isEqualTo(expected)
    }

    @Test
    fun invoke_whenException_returnsFailed() = runTest {
        val packageName = TEST_APP_PACKAGE_NAME
        val recordTypes = setOf(StepsRecord::class.java)
        val exception = HealthConnectException(HealthConnectException.ERROR_UNKNOWN)

        doAnswer {
                val receiver =
                    it.arguments[3]
                        as OutcomeReceiver<Map<String, Set<String>>, HealthConnectException>
                receiver.onError(exception)
                null
            }
            .whenever(healthConnectManager)
            .getMatchingApps(any(), any(), any(), any())

        val result = useCase.invoke(GetMatchMakingAppsInput(packageName, recordTypes))

        assertThat(result).isInstanceOf(UseCaseResults.Failed::class.java)
        assertThat((result as UseCaseResults.Failed).exception).isEqualTo(exception)
    }
}
