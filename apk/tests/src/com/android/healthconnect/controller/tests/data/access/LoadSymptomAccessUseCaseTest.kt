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
package com.android.healthconnect.controller.tests.data.access

import android.content.Context
import android.health.connect.HealthConnectException
import android.health.connect.HealthConnectManager
import android.health.connect.HealthPermissions
import android.health.connect.RecordTypeInfoResponse
import android.health.connect.datatypes.Record
import android.health.connect.datatypes.SymptomRecord
import android.os.OutcomeReceiver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.data.access.AppAccessMetadata
import com.android.healthconnect.controller.data.access.AppAccessState
import com.android.healthconnect.controller.data.access.LoadSymptomAccessUseCase
import com.android.healthconnect.controller.permissions.shared.IQueryRecentAccessLogsUseCase
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.shared.app.AppInfoReader
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.shared.app.AppPermissionsType
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import com.android.healthconnect.controller.tests.utils.InstantTaskExecutorRule
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME_2
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME_2
import com.android.healthconnect.controller.tests.utils.createFakeAppInfoReader
import com.android.healthconnect.controller.tests.utils.di.FakeGetGrantedHealthPermissionsUseCase
import com.android.healthconnect.controller.tests.utils.di.FakeQueryRecentAccessLogsUseCase
import com.android.healthconnect.controller.tests.utils.getDataOrigin
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.Executor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class LoadSymptomAccessUseCaseTest {

    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
    @get:Rule(order = 1) val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val healthConnectManager: HealthConnectManager = mock()

    private lateinit var appInfoReader: AppInfoReader

    private val healthPermissionReader: HealthPermissionReader = mock()
    private val fakeGetGrantedHealthPermissionsUseCase = FakeGetGrantedHealthPermissionsUseCase()
    private val recentAccessLogsUseCase: IQueryRecentAccessLogsUseCase =
        FakeQueryRecentAccessLogsUseCase()

    private lateinit var useCase: LoadSymptomAccessUseCase
    private lateinit var context: Context

    private val now = Instant.now()
    private val recentTime = now.minus(1, ChronoUnit.DAYS)
    private val oldTime = now.minus(100, ChronoUnit.DAYS)

    private val app1 = AppMetadata(TEST_APP_PACKAGE_NAME, TEST_APP_NAME, null)
    private val app2 = AppMetadata(TEST_APP_PACKAGE_NAME_2, TEST_APP_NAME_2, null)

    @Before
    fun setup() = runTest {
        hiltRule.inject()
        context = InstrumentationRegistry.getInstrumentation().context
        appInfoReader = createFakeAppInfoReader()

        whenever(healthPermissionReader.getAppPermissionsType(any<String>()))
            .thenReturn(AppPermissionsType.FITNESS_PERMISSIONS_ONLY)

        useCase =
            LoadSymptomAccessUseCase(
                healthConnectManager,
                fakeGetGrantedHealthPermissionsUseCase,
                healthPermissionReader,
                appInfoReader,
                Dispatchers.Main,
            )
    }

    private fun mockContributingApps(packageNames: List<String>) {
        val recordTypeInfoResponse = mock<RecordTypeInfoResponse>()
        val dataOriginList = packageNames.map { getDataOrigin(it) }

        whenever(recordTypeInfoResponse.contributingPackages).thenReturn(dataOriginList)

        val answerMap =
            mapOf<Class<out Record>, RecordTypeInfoResponse>(
                SymptomRecord::class.java to recordTypeInfoResponse
            )

        doAnswer { invocation ->
                val receiver =
                    invocation.arguments[1]
                        as
                        OutcomeReceiver<
                            Map<Class<out Record>, RecordTypeInfoResponse>,
                            HealthConnectException,
                        >
                receiver.onResult(answerMap)
                null
            }
            .whenever(healthConnectManager)
            .queryAllRecordTypesInfo(
                any<Executor>(),
                any<
                    OutcomeReceiver<
                        Map<Class<out Record>, RecordTypeInfoResponse>,
                        HealthConnectException,
                    >
                >(),
            )
    }

    @Test
    fun execute_noApps_returnsEmpty() = runTest {
        whenever(healthPermissionReader.getAppsWithFitnessPermissions()).thenReturn(emptyList())
        mockContributingApps(emptyList())
        (recentAccessLogsUseCase as FakeQueryRecentAccessLogsUseCase).recentAccessMap(emptyMap())

        val result = useCase.invoke(Unit)

        assertThat(result).isInstanceOf(UseCaseResults.Success::class.java)
        val data = (result as UseCaseResults.Success).data
        assertThat(data[AppAccessState.Read]).isEmpty()
        assertThat(data[AppAccessState.Write]).isEmpty()
        assertThat(data[AppAccessState.Inactive]).isEmpty()
    }

    @Test
    fun execute_appWithReadSymptom_active_returnsInRead() = runTest {
        whenever(healthPermissionReader.getAppsWithFitnessPermissions())
            .thenReturn(listOf(TEST_APP_PACKAGE_NAME))
        mockContributingApps(listOf(TEST_APP_PACKAGE_NAME))
        fakeGetGrantedHealthPermissionsUseCase.updateData(
            TEST_APP_PACKAGE_NAME,
            listOf(HealthPermissions.READ_SYMPTOM_ABDOMINAL_PAIN),
        )
        (recentAccessLogsUseCase as FakeQueryRecentAccessLogsUseCase).recentAccessMap(
            mapOf(TEST_APP_PACKAGE_NAME to recentTime)
        )

        val result = useCase.invoke(Unit)

        assertThat(result).isInstanceOf(UseCaseResults.Success::class.java)
        val data = (result as UseCaseResults.Success).data
        assertThat(data[AppAccessState.Read])
            .containsExactly(AppAccessMetadata(app1, AppPermissionsType.FITNESS_PERMISSIONS_ONLY))
        assertThat(data[AppAccessState.Write]).isEmpty()
        assertThat(data[AppAccessState.Inactive]).isEmpty()
    }

    @Test
    fun execute_appWithWriteSymptom_active_returnsInWrite() = runTest {
        whenever(healthPermissionReader.getAppsWithFitnessPermissions())
            .thenReturn(listOf(TEST_APP_PACKAGE_NAME))
        mockContributingApps(listOf(TEST_APP_PACKAGE_NAME))
        fakeGetGrantedHealthPermissionsUseCase.updateData(
            TEST_APP_PACKAGE_NAME,
            listOf(HealthPermissions.WRITE_SYMPTOM_ACNE),
        )
        (recentAccessLogsUseCase as FakeQueryRecentAccessLogsUseCase).recentAccessMap(
            mapOf(TEST_APP_PACKAGE_NAME to recentTime)
        )

        val result = useCase.invoke(Unit)

        assertThat(result).isInstanceOf(UseCaseResults.Success::class.java)
        val data = (result as UseCaseResults.Success).data
        assertThat(data[AppAccessState.Read]).isEmpty()
        assertThat(data[AppAccessState.Write])
            .containsExactly(AppAccessMetadata(app1, AppPermissionsType.FITNESS_PERMISSIONS_ONLY))
        assertThat(data[AppAccessState.Inactive]).isEmpty()
    }

    @Test
    fun execute_appWithReadWriteSymptom_active_returnsInReadWrite() = runTest {
        whenever(healthPermissionReader.getAppsWithFitnessPermissions())
            .thenReturn(listOf(TEST_APP_PACKAGE_NAME))
        mockContributingApps(listOf(TEST_APP_PACKAGE_NAME))
        fakeGetGrantedHealthPermissionsUseCase.updateData(
            TEST_APP_PACKAGE_NAME,
            listOf(
                HealthPermissions.READ_SYMPTOM_BACK_PAIN,
                HealthPermissions.WRITE_SYMPTOM_BACK_PAIN,
            ),
        )
        (recentAccessLogsUseCase as FakeQueryRecentAccessLogsUseCase).recentAccessMap(
            mapOf(TEST_APP_PACKAGE_NAME to recentTime)
        )

        val result = useCase.invoke(Unit)

        assertThat(result).isInstanceOf(UseCaseResults.Success::class.java)
        val data = (result as UseCaseResults.Success).data
        assertThat(data[AppAccessState.Read])
            .containsExactly(AppAccessMetadata(app1, AppPermissionsType.FITNESS_PERMISSIONS_ONLY))
        assertThat(data[AppAccessState.Write])
            .containsExactly(AppAccessMetadata(app1, AppPermissionsType.FITNESS_PERMISSIONS_ONLY))
        assertThat(data[AppAccessState.Inactive]).isEmpty()
    }

    @Test
    fun execute_appWithNonSymptomPerms_returnsEmpty() = runTest {
        whenever(healthPermissionReader.getAppsWithFitnessPermissions())
            .thenReturn(listOf(TEST_APP_PACKAGE_NAME))
        mockContributingApps(emptyList())
        fakeGetGrantedHealthPermissionsUseCase.updateData(
            TEST_APP_PACKAGE_NAME,
            listOf(HealthPermissions.READ_STEPS, HealthPermissions.WRITE_WEIGHT),
        )
        (recentAccessLogsUseCase as FakeQueryRecentAccessLogsUseCase).recentAccessMap(
            mapOf(TEST_APP_PACKAGE_NAME to recentTime)
        )

        val result = useCase.invoke(Unit)

        assertThat(result).isInstanceOf(UseCaseResults.Success::class.java)
        val data = (result as UseCaseResults.Success).data
        assertThat(data[AppAccessState.Read]).isEmpty()
        assertThat(data[AppAccessState.Write]).isEmpty()
        assertThat(data[AppAccessState.Inactive]).isEmpty()
    }

    @Test
    fun execute_inactiveAppWithNoPermissions_AddsToInactive() = runTest {
        whenever(healthPermissionReader.getAppsWithFitnessPermissions()).thenReturn(emptyList())
        mockContributingApps(listOf(TEST_APP_PACKAGE_NAME))
        fakeGetGrantedHealthPermissionsUseCase.updateData(TEST_APP_PACKAGE_NAME, emptyList())

        val result = useCase.invoke(Unit)

        assertThat(result).isInstanceOf(UseCaseResults.Success::class.java)
        val data = (result as UseCaseResults.Success).data
        assertThat(data[AppAccessState.Read]).isEmpty()
        assertThat(data[AppAccessState.Write]).isEmpty()
        assertThat(data[AppAccessState.Inactive]).containsExactly(AppAccessMetadata(app1))
    }

    @Test
    fun execute_contributingAppWithPermissions_notInactive() = runTest {
        whenever(healthPermissionReader.getAppsWithFitnessPermissions())
            .thenReturn(listOf(TEST_APP_PACKAGE_NAME))
        mockContributingApps(listOf(TEST_APP_PACKAGE_NAME))
        fakeGetGrantedHealthPermissionsUseCase.updateData(
            TEST_APP_PACKAGE_NAME,
            listOf(HealthPermissions.READ_SYMPTOM_COUGH),
        )

        val result = useCase.invoke(Unit)

        assertThat(result).isInstanceOf(UseCaseResults.Success::class.java)
        val data = (result as UseCaseResults.Success).data
        assertThat(data[AppAccessState.Inactive]).isEmpty()
    }

    @Test
    fun execute_multipleApps_categorizesCorrectly() = runTest {
        whenever(healthPermissionReader.getAppsWithFitnessPermissions())
            .thenReturn(listOf(TEST_APP_PACKAGE_NAME, TEST_APP_PACKAGE_NAME_2))
        mockContributingApps(listOf(TEST_APP_PACKAGE_NAME, TEST_APP_PACKAGE_NAME_2))
        fakeGetGrantedHealthPermissionsUseCase.updateData(
            TEST_APP_PACKAGE_NAME,
            listOf(HealthPermissions.READ_SYMPTOM_COUGH),
        )
        fakeGetGrantedHealthPermissionsUseCase.updateData(
            TEST_APP_PACKAGE_NAME_2,
            listOf(HealthPermissions.WRITE_SYMPTOM_DIZZINESS),
        )
        (recentAccessLogsUseCase as FakeQueryRecentAccessLogsUseCase).recentAccessMap(
            mapOf(TEST_APP_PACKAGE_NAME to recentTime, TEST_APP_PACKAGE_NAME_2 to recentTime)
        )

        val result = useCase.invoke(Unit)

        assertThat(result).isInstanceOf(UseCaseResults.Success::class.java)
        val data = (result as UseCaseResults.Success).data
        assertThat(data[AppAccessState.Read])
            .containsExactly(AppAccessMetadata(app1, AppPermissionsType.FITNESS_PERMISSIONS_ONLY))
        assertThat(data[AppAccessState.Write])
            .containsExactly(AppAccessMetadata(app2, AppPermissionsType.FITNESS_PERMISSIONS_ONLY))
        assertThat(data[AppAccessState.Inactive]).isEmpty()
    }
}
