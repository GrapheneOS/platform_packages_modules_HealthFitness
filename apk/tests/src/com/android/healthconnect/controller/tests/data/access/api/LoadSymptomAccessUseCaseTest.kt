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
package com.android.healthconnect.controller.tests.data.access.api

import android.content.Context
import android.health.connect.DeviceDataProviderInfo
import android.health.connect.DeviceDataSourceInfo
import android.health.connect.HealthPermissions
import android.health.connect.datatypes.DataOrigin
import android.health.connect.datatypes.Device
import android.health.connect.datatypes.StepsRecord
import android.health.connect.datatypes.SymptomRecord
import android.health.connect.device.DeviceDataTypeAdvertisement
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.data.access.AppAccessMetadata
import com.android.healthconnect.controller.data.access.AppAccessState
import com.android.healthconnect.controller.data.access.api.LoadSymptomAccessUseCase
import com.android.healthconnect.controller.permissions.shared.IQueryRecentAccessLogsUseCase
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.shared.app.AppInfoReader
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.shared.app.AppPermissionsType
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import com.android.healthconnect.controller.tests.devices.api.FakeGetDeviceDataSourcesInfoUseCase
import com.android.healthconnect.controller.tests.utils.FakeUseCaseRule
import com.android.healthconnect.controller.tests.utils.InstantTaskExecutorRule
import com.android.healthconnect.controller.tests.utils.TEST_APP
import com.android.healthconnect.controller.tests.utils.TEST_APP_2
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME_2
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME_2
import com.android.healthconnect.controller.tests.utils.TEST_PHONE_APP
import com.android.healthconnect.controller.tests.utils.TEST_PHONE_SPN
import com.android.healthconnect.controller.tests.utils.TEST_WATCH_SPN
import com.android.healthconnect.controller.tests.utils.createFakeAppInfoReader
import com.android.healthconnect.controller.tests.utils.di.FakeGetGrantedHealthPermissionsUseCase
import com.android.healthconnect.controller.tests.utils.di.FakeQueryRecentAccessLogsUseCase
import com.android.healthfitness.flags.Flags
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class LoadSymptomAccessUseCaseTest {

    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
    @get:Rule(order = 1) val instantTaskExecutorRule = InstantTaskExecutorRule()
    @get:Rule(order = 2) val fakeUseCaseRule = FakeUseCaseRule()
    @get:Rule(order = 3) val setFlagsRule = SetFlagsRule()

    private lateinit var appInfoReader: AppInfoReader

    private val healthPermissionReader: HealthPermissionReader = mock()
    private val fakeGetGrantedHealthPermissionsUseCase = FakeGetGrantedHealthPermissionsUseCase()
    private val fakeLoadSymptomContributorAppsUseCase =
        fakeUseCaseRule.watch(FakeLoadSymptomTypeContributorAppsUseCase())
    private val recentAccessLogsUseCase: IQueryRecentAccessLogsUseCase =
        FakeQueryRecentAccessLogsUseCase()
    private val fakeLoadDeviceDataSourcesInfosUseCase =
        fakeUseCaseRule.watch(FakeGetDeviceDataSourcesInfoUseCase())

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
                fakeGetGrantedHealthPermissionsUseCase,
                fakeLoadSymptomContributorAppsUseCase,
                fakeLoadDeviceDataSourcesInfosUseCase,
                healthPermissionReader,
                appInfoReader,
                context,
                Dispatchers.Main,
            )
    }

    @Test
    fun execute_noApps_returnsEmpty() = runTest {
        whenever(healthPermissionReader.getAppsWithFitnessPermissions()).thenReturn(emptyList())
        fakeLoadSymptomContributorAppsUseCase.updateList(emptyList())
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
        fakeLoadSymptomContributorAppsUseCase.updateList(listOf(TEST_APP))

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
        fakeLoadSymptomContributorAppsUseCase.updateList(listOf(TEST_APP))
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
        fakeLoadSymptomContributorAppsUseCase.updateList(listOf(TEST_APP))
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
        fakeLoadSymptomContributorAppsUseCase.updateList(emptyList())
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
        fakeLoadSymptomContributorAppsUseCase.updateList(listOf(TEST_APP))
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
        fakeLoadSymptomContributorAppsUseCase.updateList(listOf(TEST_APP))
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
        fakeLoadSymptomContributorAppsUseCase.updateList(listOf(TEST_APP, TEST_APP_2))
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

    @Test
    @DisableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    fun devices_ddpFlagOff_devicesAreSeenAsNormalApps() = runTest {
        whenever(healthPermissionReader.getAppsWithFitnessPermissions()).thenReturn(emptyList())
        whenever(appInfoReader.getAppMetadata(eq(TEST_PHONE_SPN))).thenReturn(TEST_PHONE_APP)
        fakeGetGrantedHealthPermissionsUseCase.updateData(TEST_PHONE_SPN, emptyList())
        fakeLoadSymptomContributorAppsUseCase.updateList(listOf(TEST_PHONE_APP))

        val result = useCase.invoke(Unit)

        assertThat(result).isInstanceOf(UseCaseResults.Success::class.java)
        val data = (result as UseCaseResults.Success).data

        assertThat(data[AppAccessState.Write]).isEmpty()
        assertThat(data[AppAccessState.Read]).isEmpty()
        assertThat(data[AppAccessState.Inactive]).isNotNull()
        assertThat(data[AppAccessState.Inactive]!!).hasSize(1)
        assertThat(data[AppAccessState.Inactive]!![0].appMetadata.packageName)
            .isEqualTo(TEST_PHONE_SPN)
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    fun devices_ddpFlagOn_devicesHaveSpecialHandling() = runTest {
        val devices =
            setOf(
                DeviceDataSourceInfo(
                    DataOrigin.Builder().setPackageName(TEST_PHONE_SPN).build(),
                    Device.Builder().setType(Device.DEVICE_TYPE_PHONE).build(),
                    false,
                    listOf(
                        DeviceDataProviderInfo(
                            "device_id",
                            "provider_id",
                            "label",
                            "description",
                            setOf(
                                DeviceDataTypeAdvertisement.Builder(SymptomRecord::class.java)
                                    .setSymptomType(SymptomRecord.SYMPTOM_TYPE_COUGH)
                                    .setAvailable(true)
                                    .setUserEnabled(true)
                                    .build()
                            ),
                        )
                    ),
                ),
                DeviceDataSourceInfo(
                    DataOrigin.Builder().setPackageName(TEST_WATCH_SPN).build(),
                    Device.Builder().setType(Device.DEVICE_TYPE_WATCH).build(),
                    false,
                    listOf(
                        DeviceDataProviderInfo(
                            "device_id_2",
                            "provider_id_2",
                            "label",
                            "description",
                            setOf(
                                DeviceDataTypeAdvertisement.Builder(StepsRecord::class.java)
                                    .setAvailable(true)
                                    .setUserEnabled(true)
                                    .build()
                            ),
                        )
                    ),
                ),
            )

        whenever(healthPermissionReader.getAppsWithFitnessPermissions()).thenReturn(emptyList())
        fakeGetGrantedHealthPermissionsUseCase.updateData(TEST_PHONE_SPN, emptyList())
        fakeGetGrantedHealthPermissionsUseCase.updateData(TEST_WATCH_SPN, emptyList())
        fakeLoadSymptomContributorAppsUseCase.updateList(listOf())

        whenever(appInfoReader.getAppMetadata(eq(TEST_PHONE_SPN))).thenReturn(TEST_PHONE_APP)
        whenever(appInfoReader.getAppMetadata(eq(TEST_WATCH_SPN)))
            .thenReturn(AppMetadata(TEST_WATCH_SPN, "Watch", null))

        fakeLoadDeviceDataSourcesInfosUseCase.updateSet(devices)

        val result = useCase.invoke(Unit)

        assertThat(result).isInstanceOf(UseCaseResults.Success::class.java)
        val data = (result as UseCaseResults.Success).data

        // phone has Symptoms advertised -> In Write category
        assertThat(data[AppAccessState.Write]).isNotNull()
        assertThat(data[AppAccessState.Write]!!).hasSize(1)
        assertThat(data[AppAccessState.Write]!![0].appMetadata.packageName)
            .isEqualTo(TEST_PHONE_SPN)

        // Watch does not have Symptoms advertised -> Not included
        // No devices in Read
        assertThat(data[AppAccessState.Read]).isEmpty()

        // Watch is not a contributor for Symptoms -> Not included in Inactive
        assertThat(data[AppAccessState.Inactive]).isNotNull()
    }
}
