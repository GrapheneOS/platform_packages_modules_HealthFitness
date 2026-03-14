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
package com.android.healthconnect.controller.tests.data.entries.api

import android.content.Context
import android.health.connect.AggregateRecordsRequest
import android.health.connect.AggregateRecordsResponse
import android.health.connect.AggregateResult
import android.health.connect.HealthConnectManager
import android.health.connect.datatypes.AggregationType
import android.health.connect.datatypes.DataOrigin
import android.health.connect.datatypes.units.Energy
import android.health.connect.datatypes.units.Length
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.data.entries.FormattedEntry
import com.android.healthconnect.controller.data.entries.api.LoadAggregationInput
import com.android.healthconnect.controller.data.entries.api.LoadDataAggregationsUseCase
import com.android.healthconnect.controller.data.entries.api.LoadEntriesHelper
import com.android.healthconnect.controller.data.entries.datenavigation.DateNavigationPeriod
import com.android.healthconnect.controller.data.formatters.DistanceFormatter
import com.android.healthconnect.controller.data.formatters.MindfulnessSessionFormatter
import com.android.healthconnect.controller.data.formatters.SleepSessionFormatter
import com.android.healthconnect.controller.data.formatters.StepsFormatter
import com.android.healthconnect.controller.data.formatters.TotalCaloriesBurnedFormatter
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.service.HealthManagerModule
import com.android.healthconnect.controller.shared.Constants.DEVICE_DATA_PROVIDER_PACKAGE
import com.android.healthconnect.controller.shared.app.AppInfoReader
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import com.android.healthconnect.controller.tests.devices.api.FakeGetCurrentDeviceIdUseCase
import com.android.healthconnect.controller.tests.utils.FakeUseCaseRule
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.createFakeAppInfoReader
import com.android.healthconnect.controller.tests.utils.doReturnResult
import com.android.healthconnect.controller.tests.utils.setLocale
import com.android.healthfitness.flags.Flags
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import java.time.Duration
import java.time.Instant
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@UninstallModules(HealthManagerModule::class)
@RunWith(AndroidJUnit4::class)
class LoadDataAggregationsUseCaseTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)
    @get:Rule val checkFlagsRule = SetFlagsRule()
    @get:Rule val fakeUseCaseRule = FakeUseCaseRule()
    private lateinit var context: Context
    @BindValue lateinit var appInfoReader: AppInfoReader
    @BindValue val healthConnectManager: HealthConnectManager = mock()
    private lateinit var loadDataAggregationsUseCase: LoadDataAggregationsUseCase

    private val fakeGetCurrentDeviceIdUseCase =
        fakeUseCaseRule.watch(FakeGetCurrentDeviceIdUseCase())

    @Inject lateinit var loadEntriesHelper: LoadEntriesHelper

    @Inject lateinit var stepsFormatter: StepsFormatter

    @Inject lateinit var totalCaloriesBurnedFormatter: TotalCaloriesBurnedFormatter

    @Inject lateinit var distanceFormatter: DistanceFormatter

    @Inject lateinit var sleepSessionFormatter: SleepSessionFormatter

    @Inject lateinit var mindfulnessSessionFormatter: MindfulnessSessionFormatter

    @Before
    fun setup() = runTest {
        context = InstrumentationRegistry.getInstrumentation().context
        context.setLocale(Locale.US)
        appInfoReader = createFakeAppInfoReader()
        hiltRule.inject()
        loadDataAggregationsUseCase =
            LoadDataAggregationsUseCase(
                loadEntriesHelper,
                stepsFormatter,
                totalCaloriesBurnedFormatter,
                distanceFormatter,
                sleepSessionFormatter,
                mindfulnessSessionFormatter,
                healthConnectManager,
                appInfoReader,
                fakeGetCurrentDeviceIdUseCase,
                Dispatchers.Main,
            )
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    fun loadDataAggregationsUseCase_flagsOn_withCurrentDevicePackage_addsAndroidPackageToFilter() =
        runTest {
            val deviceId = "test_device_id"
            fakeGetCurrentDeviceIdUseCase.updateDeviceId(deviceId)
            healthConnectManager.stub {
                on { aggregate<Long>(any(), any(), any()) } doReturnResult
                    Result.success(getStepsAggregationResponse())
            }

            val input =
                LoadAggregationInput.PeriodAggregation(
                    FitnessPermissionType.STEPS,
                    deviceId,
                    displayedStartTime = Instant.now(),
                    period = DateNavigationPeriod.PERIOD_DAY,
                    showDataOrigin = true,
                )

            loadDataAggregationsUseCase.invoke(input)
            val captor = argumentCaptor<AggregateRecordsRequest<Long>>()
            verify(healthConnectManager).aggregate<Long>(captor.capture(), any(), any())

            val request = captor.firstValue
            assertThat(request.dataOriginsFilters)
                .containsExactly(
                    DataOrigin.Builder().setPackageName(DEVICE_DATA_PROVIDER_PACKAGE).build(),
                    DataOrigin.Builder().setPackageName(deviceId).build(),
                )
        }

    @Test
    @DisableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    fun loadDataAggregationsUseCase_flagsOff_withCurrentDevicePackage_doesNotAddAndroidPackage() =
        runTest {
            val deviceId = "test_device_id"
            fakeGetCurrentDeviceIdUseCase.updateDeviceId(deviceId)
            healthConnectManager.stub {
                on { aggregate<Long>(any(), any(), any()) } doReturnResult
                    Result.success(getStepsAggregationResponse())
            }

            val input =
                LoadAggregationInput.PeriodAggregation(
                    FitnessPermissionType.STEPS,
                    deviceId,
                    displayedStartTime = Instant.now(),
                    period = DateNavigationPeriod.PERIOD_DAY,
                    showDataOrigin = true,
                )

            loadDataAggregationsUseCase.invoke(input)
            val captor = argumentCaptor<AggregateRecordsRequest<Long>>()
            verify(healthConnectManager).aggregate<Long>(captor.capture(), any(), any())

            val request = captor.firstValue
            assertThat(request.dataOriginsFilters)
                .containsExactly(DataOrigin.Builder().setPackageName(deviceId).build())
        }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    fun loadDataAggregationsUseCase_withRandomDevice_doesNotAddAndroidPackageToFilter() = runTest {
        val deviceId = "test_device_id"
        fakeGetCurrentDeviceIdUseCase.updateDeviceId("not_test_device_id")
        healthConnectManager.stub {
            on { aggregate<Long>(any(), any(), any()) } doReturnResult
                Result.success(getStepsAggregationResponse())
        }

        val input =
            LoadAggregationInput.PeriodAggregation(
                FitnessPermissionType.STEPS,
                deviceId,
                displayedStartTime = Instant.now(),
                period = DateNavigationPeriod.PERIOD_DAY,
                showDataOrigin = true,
            )

        loadDataAggregationsUseCase.invoke(input)
        val captor = argumentCaptor<AggregateRecordsRequest<Long>>()
        verify(healthConnectManager).aggregate<Long>(captor.capture(), any(), any())

        val request = captor.firstValue
        assertThat(request.dataOriginsFilters)
            .containsExactly(DataOrigin.Builder().setPackageName(deviceId).build())
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    fun loadDataAggregationsUseCase_flagsOn_withAndroid_addsCurrentDeviceToFilter() = runTest {
        val currentDeviceId = "test_device_id"
        fakeGetCurrentDeviceIdUseCase.updateDeviceId(currentDeviceId)
        healthConnectManager.stub {
            on { aggregate<Long>(any(), any(), any()) } doReturnResult
                Result.success(getStepsAggregationResponse())
        }

        val input =
            LoadAggregationInput.PeriodAggregation(
                FitnessPermissionType.STEPS,
                DEVICE_DATA_PROVIDER_PACKAGE,
                displayedStartTime = Instant.now(),
                period = DateNavigationPeriod.PERIOD_DAY,
                showDataOrigin = true,
            )

        loadDataAggregationsUseCase.invoke(input)
        val captor = argumentCaptor<AggregateRecordsRequest<Long>>()
        verify(healthConnectManager).aggregate<Long>(captor.capture(), any(), any())

        val request = captor.firstValue
        assertThat(request.dataOriginsFilters)
            .containsExactly(
                DataOrigin.Builder().setPackageName(DEVICE_DATA_PROVIDER_PACKAGE).build(),
                DataOrigin.Builder().setPackageName(currentDeviceId).build(),
            )
    }

    @Test
    @DisableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    fun loadDataAggregationsUseCase_flagsOff_withAndroid_doesNotAddCurrentDevice() = runTest {
        val currentDeviceId = "test_device_id"
        fakeGetCurrentDeviceIdUseCase.updateDeviceId(currentDeviceId)
        healthConnectManager.stub {
            on { aggregate<Long>(any(), any(), any()) } doReturnResult
                Result.success(getStepsAggregationResponse())
        }

        val input =
            LoadAggregationInput.PeriodAggregation(
                FitnessPermissionType.STEPS,
                DEVICE_DATA_PROVIDER_PACKAGE,
                displayedStartTime = Instant.now(),
                period = DateNavigationPeriod.PERIOD_DAY,
                showDataOrigin = true,
            )

        loadDataAggregationsUseCase.invoke(input)
        val captor = argumentCaptor<AggregateRecordsRequest<Long>>()
        verify(healthConnectManager).aggregate<Long>(captor.capture(), any(), any())

        val request = captor.firstValue
        assertThat(request.dataOriginsFilters)
            .containsExactly(
                DataOrigin.Builder().setPackageName(DEVICE_DATA_PROVIDER_PACKAGE).build()
            )
    }

    @Test
    @DisableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    fun loadDataAggregationsUseCase_ddpFlagsOff_withAndroidAndCurrentDevice_doesNotFilterAndroid() =
        runTest {
            val currentDeviceId = "test_device_id"
            fakeGetCurrentDeviceIdUseCase.updateDeviceId(currentDeviceId)
            whenever(appInfoReader.getAppMetadata(DEVICE_DATA_PROVIDER_PACKAGE))
                .thenReturn(AppMetadata(DEVICE_DATA_PROVIDER_PACKAGE, "My Device", null))
            whenever(appInfoReader.getAppMetadata(currentDeviceId))
                .thenReturn(AppMetadata(currentDeviceId, "My Device", null))

            healthConnectManager.stub {
                on { aggregate<Long>(any(), any(), any()) } doReturnResult
                    Result.success(
                        getStepsAggregationResponse(
                            listOf(DEVICE_DATA_PROVIDER_PACKAGE, currentDeviceId)
                        )
                    )
            }

            val input =
                LoadAggregationInput.PeriodAggregation(
                    FitnessPermissionType.STEPS,
                    null,
                    displayedStartTime = Instant.now(),
                    period = DateNavigationPeriod.PERIOD_DAY,
                    showDataOrigin = true,
                )

            val result = loadDataAggregationsUseCase.invoke(input)
            assertThat(result is UseCaseResults.Success).isTrue()
            val contributors = (result as UseCaseResults.Success).data.contributingApps
            assertThat(contributors).isEqualTo("My Device, My Device")
        }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    fun loadDataAggregationsUseCase_ddpFlagsOn_withAndroidButNoCurrentDevice_doesNotFilterAndroid() =
        runTest {
            val currentDeviceId = "test_device_id"
            fakeGetCurrentDeviceIdUseCase.updateDeviceId(currentDeviceId)
            whenever(appInfoReader.getAppMetadata(DEVICE_DATA_PROVIDER_PACKAGE))
                .thenReturn(AppMetadata(DEVICE_DATA_PROVIDER_PACKAGE, "My Device", null))
            whenever(appInfoReader.getAppMetadata(TEST_APP_PACKAGE_NAME))
                .thenReturn(AppMetadata(TEST_APP_PACKAGE_NAME, TEST_APP_NAME, null))

            healthConnectManager.stub {
                on { aggregate<Long>(any(), any(), any()) } doReturnResult
                    Result.success(
                        getStepsAggregationResponse(
                            listOf(DEVICE_DATA_PROVIDER_PACKAGE, TEST_APP_PACKAGE_NAME)
                        )
                    )
            }

            val input =
                LoadAggregationInput.PeriodAggregation(
                    FitnessPermissionType.STEPS,
                    null,
                    displayedStartTime = Instant.now(),
                    period = DateNavigationPeriod.PERIOD_DAY,
                    showDataOrigin = true,
                )

            val result = loadDataAggregationsUseCase.invoke(input)
            assertThat(result is UseCaseResults.Success).isTrue()
            // We cannot guarantee order so checking for contains
            val actualApps = (result as UseCaseResults.Success).data.contributingApps
            assertThat(actualApps).contains("My Device")
            assertThat(actualApps).contains(TEST_APP_NAME)
        }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    fun loadDataAggregationsUseCase_ddpFlagsOn_withAndroidAndCurrentDevice_filtersAndroid() =
        runTest {
            val currentDeviceId = "test_device_id"
            fakeGetCurrentDeviceIdUseCase.updateDeviceId(currentDeviceId)
            whenever(appInfoReader.getAppMetadata(DEVICE_DATA_PROVIDER_PACKAGE))
                .thenReturn(AppMetadata(DEVICE_DATA_PROVIDER_PACKAGE, "My Device", null))
            whenever(appInfoReader.getAppMetadata(currentDeviceId))
                .thenReturn(AppMetadata(currentDeviceId, "My Device", null))

            healthConnectManager.stub {
                on { aggregate<Long>(any(), any(), any()) } doReturnResult
                    Result.success(
                        getStepsAggregationResponse(
                            listOf(DEVICE_DATA_PROVIDER_PACKAGE, currentDeviceId)
                        )
                    )
            }

            val input =
                LoadAggregationInput.PeriodAggregation(
                    FitnessPermissionType.STEPS,
                    null,
                    displayedStartTime = Instant.now(),
                    period = DateNavigationPeriod.PERIOD_DAY,
                    showDataOrigin = true,
                )

            val result = loadDataAggregationsUseCase.invoke(input)
            assertThat(result is UseCaseResults.Success).isTrue()
            assertThat((result as UseCaseResults.Success).data.contributingApps)
                .isEqualTo("My Device")
        }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    fun loadDataAggregationsUseCase_ddpFlagsOn_withCurrentDeviceAndNoAndroid_doesNotFilterAnything() =
        runTest {
            val currentDeviceId = "test_device_id"
            fakeGetCurrentDeviceIdUseCase.updateDeviceId(currentDeviceId)
            whenever(appInfoReader.getAppMetadata(currentDeviceId))
                .thenReturn(AppMetadata(currentDeviceId, "My Device", null))
            whenever(appInfoReader.getAppMetadata(TEST_APP_PACKAGE_NAME))
                .thenReturn(AppMetadata(TEST_APP_PACKAGE_NAME, TEST_APP_NAME, null))

            healthConnectManager.stub {
                on { aggregate<Long>(any(), any(), any()) } doReturnResult
                    Result.success(
                        getStepsAggregationResponse(listOf(currentDeviceId, TEST_APP_PACKAGE_NAME))
                    )
            }

            val input =
                LoadAggregationInput.PeriodAggregation(
                    FitnessPermissionType.STEPS,
                    null,
                    displayedStartTime = Instant.now(),
                    period = DateNavigationPeriod.PERIOD_DAY,
                    showDataOrigin = true,
                )

            val result = loadDataAggregationsUseCase.invoke(input)
            assertThat(result is UseCaseResults.Success).isTrue()
            // We cannot guarantee order so checking for contains
            val actualApps = (result as UseCaseResults.Success).data.contributingApps
            assertThat(actualApps).contains("My Device")
            assertThat(actualApps).contains(TEST_APP_NAME)
        }

    @Test
    fun loadDataAggregationsUseCase_withPeriodAggregationForSteps_returnsFormattedStepsAggregation() =
        runTest {
            healthConnectManager.stub {
                on { aggregate<Long>(any(), any(), any()) } doReturnResult
                    Result.success(getStepsAggregationResponse())
            }

            val input =
                LoadAggregationInput.PeriodAggregation(
                    FitnessPermissionType.STEPS,
                    TEST_APP_PACKAGE_NAME,
                    displayedStartTime = Instant.now(),
                    period = DateNavigationPeriod.PERIOD_DAY,
                    showDataOrigin = true,
                )

            val result = loadDataAggregationsUseCase.invoke(input)
            val expected =
                FormattedEntry.FormattedAggregation("100 steps", "100 steps", TEST_APP_NAME)
            assertThat(result is UseCaseResults.Success).isTrue()
            assertThat((result as UseCaseResults.Success).data).isEqualTo(expected)
        }

    @Test
    fun loadDataAggregationsUseCase_withPeriodAggregationForDistance_returnsFormattedDistanceAggregation() =
        runTest {
            healthConnectManager.stub {
                on { aggregate<Length>(any(), any(), any()) } doReturnResult
                    Result.success(getDistanceAggregationResponse())
            }

            val input =
                LoadAggregationInput.PeriodAggregation(
                    FitnessPermissionType.DISTANCE,
                    TEST_APP_PACKAGE_NAME,
                    displayedStartTime = Instant.now(),
                    period = DateNavigationPeriod.PERIOD_DAY,
                    showDataOrigin = true,
                )

            val result = loadDataAggregationsUseCase.invoke(input)
            val expected =
                FormattedEntry.FormattedAggregation("0.621 miles", "0.621 miles", TEST_APP_NAME)
            assertThat(result is UseCaseResults.Success).isTrue()
            assertThat((result as UseCaseResults.Success).data).isEqualTo(expected)
        }

    @Test
    fun loadDataAggregationsUseCase_withPeriodAggregationForCalories_returnsFormattedCaloriesAggregation() =
        runTest {
            healthConnectManager.stub {
                on { aggregate<Energy>(any(), any(), any()) } doReturnResult
                    Result.success(getCaloriesAggregationResponse())
            }

            val input =
                LoadAggregationInput.PeriodAggregation(
                    FitnessPermissionType.TOTAL_CALORIES_BURNED,
                    TEST_APP_PACKAGE_NAME,
                    displayedStartTime = Instant.now(),
                    period = DateNavigationPeriod.PERIOD_DAY,
                    showDataOrigin = true,
                )

            val result = loadDataAggregationsUseCase.invoke(input)
            val expected =
                FormattedEntry.FormattedAggregation("1,500 Cal", "1,500 calories", TEST_APP_NAME)
            assertThat(result is UseCaseResults.Success).isTrue()
            assertThat((result as UseCaseResults.Success).data).isEqualTo(expected)
        }

    @Test
    fun loadDataAggregationsUseCase_withCustomAggregationForSleep_returnsFormattedSleepAggregation() =
        runTest {
            healthConnectManager.stub {
                on { aggregate<Long>(any(), any(), any()) } doReturnResult
                    Result.success(getSleepAggregationResponse())
            }

            val input =
                LoadAggregationInput.CustomAggregation(
                    FitnessPermissionType.SLEEP,
                    TEST_APP_PACKAGE_NAME,
                    startTime = Instant.now(),
                    endTime = Instant.now(),
                    showDataOrigin = true,
                )

            val result = loadDataAggregationsUseCase.invoke(input)
            val expected =
                FormattedEntry.FormattedAggregation(
                    "11h${NBSP}5m",
                    "11 hours 5 minutes",
                    TEST_APP_NAME,
                )
            assertThat(result is UseCaseResults.Success).isTrue()
            assertThat((result as UseCaseResults.Success).data).isEqualTo(expected)
        }

    @Test
    fun loadDataAggregationsUseCase_withCustomAggregationForMindfulness_returnsFormattedMindfulnessAggregation() =
        runTest {
            healthConnectManager.stub {
                on { aggregate<Long>(any(), any(), any()) } doReturnResult
                    Result.success(
                        getMindfulnessAggregationResponse(Duration.ofHours(6).plusMinutes(15))
                    )
            }

            val input =
                LoadAggregationInput.CustomAggregation(
                    FitnessPermissionType.MINDFULNESS,
                    TEST_APP_PACKAGE_NAME,
                    startTime = Instant.now(),
                    endTime = Instant.now(),
                    showDataOrigin = true,
                )

            val result = loadDataAggregationsUseCase.invoke(input)

            val expected =
                FormattedEntry.FormattedAggregation(
                    "6h${NBSP}15m",
                    "Total mindfulness time of 6 hours 15 minutes",
                    TEST_APP_NAME,
                )
            assertThat(result is UseCaseResults.Success).isTrue()
            assertThat((result as UseCaseResults.Success).data).isEqualTo(expected)
        }

    @Test
    fun loadDataAggregationsUseCase_withShowDataOriginFalse_returnsEmptyContributingApps() =
        runTest {
            healthConnectManager.stub {
                on { aggregate<Long>(any(), any(), any()) } doReturnResult
                    Result.success(getStepsAggregationResponse())
            }

            val input =
                LoadAggregationInput.PeriodAggregation(
                    FitnessPermissionType.STEPS,
                    TEST_APP_PACKAGE_NAME,
                    displayedStartTime = Instant.now(),
                    period = DateNavigationPeriod.PERIOD_DAY,
                    showDataOrigin = false, // Data origin is disabled here
                )

            val result = loadDataAggregationsUseCase.invoke(input)
            assertThat(result is UseCaseResults.Success).isTrue()
            assertThat((result as UseCaseResults.Success).data.contributingApps).isEmpty()
        }

    @Test
    fun loadDataAggregationsUseCase_withUnsupportedType_returnsFailure() = runTest {
        val input =
            LoadAggregationInput.PeriodAggregation(
                FitnessPermissionType
                    .HEART_RATE, // Heart rate is not supported for aggregation in this usecase
                TEST_APP_PACKAGE_NAME,
                displayedStartTime = Instant.now(),
                period = DateNavigationPeriod.PERIOD_DAY,
                showDataOrigin = true,
            )

        val result = loadDataAggregationsUseCase.invoke(input)
        assertThat(result is UseCaseResults.Failed).isTrue()
        assertThat((result as UseCaseResults.Failed).exception)
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    private fun getStepsAggregationResponse(
        packages: List<String> = listOf(TEST_APP_PACKAGE_NAME)
    ): AggregateRecordsResponse<Long> {
        val aggregationResult =
            AggregateResult<Long>(
                100,
                /* zoneOffset = */ null,
                AggregateResult.convertDataOrigins(packages),
            )
        return AggregateRecordsResponse<Long>(
            mapOf(
                AggregationType.AggregationTypeIdentifier.STEPS_RECORD_COUNT_TOTAL to
                    aggregationResult
            )
        )
    }

    private fun getDistanceAggregationResponse(): AggregateRecordsResponse<Length> {
        val aggregationResult =
            AggregateResult(
                Length.fromMeters(1000.0),
                /* zoneOffset = */ null,
                AggregateResult.convertDataOrigins(listOf(TEST_APP_PACKAGE_NAME)),
            )
        return AggregateRecordsResponse<Length>(
            mapOf(
                AggregationType.AggregationTypeIdentifier.DISTANCE_RECORD_DISTANCE_TOTAL to
                    aggregationResult
            )
        )
    }

    private fun getCaloriesAggregationResponse(): AggregateRecordsResponse<Energy> {
        val aggregationResult =
            AggregateResult(
                Energy.fromCalories(1500000.0),
                /* zoneOffset = */ null,
                AggregateResult.convertDataOrigins(listOf(TEST_APP_PACKAGE_NAME)),
            )
        return AggregateRecordsResponse<Energy>(
            mapOf(
                AggregationType.AggregationTypeIdentifier
                    .TOTAL_CALORIES_BURNED_RECORD_ENERGY_TOTAL to aggregationResult
            )
        )
    }

    private fun getSleepAggregationResponse(): AggregateRecordsResponse<Long> {
        val aggregationResult =
            AggregateResult(
                Duration.ofHours(11).plus(Duration.ofMinutes(5)).toMillis(),
                /* zoneOffset = */ null,
                AggregateResult.convertDataOrigins(listOf(TEST_APP_PACKAGE_NAME)),
            )
        return AggregateRecordsResponse<Long>(
            mapOf(
                AggregationType.AggregationTypeIdentifier.SLEEP_SESSION_DURATION_TOTAL to
                    aggregationResult
            )
        )
    }

    private fun getMindfulnessAggregationResponse(
        duration: Duration
    ): AggregateRecordsResponse<Long> {
        val aggregationResult =
            AggregateResult(
                duration.toMillis(),
                /* zoneOffset = */ null,
                AggregateResult.convertDataOrigins(listOf(TEST_APP_PACKAGE_NAME)),
            )
        return AggregateRecordsResponse<Long>(
            mapOf(
                AggregationType.AggregationTypeIdentifier.MINDFULNESS_SESSION_DURATION_TOTAL to
                    aggregationResult
            )
        )
    }

    private companion object {
        const val NBSP = "\u00A0" // no break space
    }
}
