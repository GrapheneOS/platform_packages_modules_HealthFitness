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
import android.health.connect.AggregateRecordsResponse
import android.health.connect.AggregateResult
import android.health.connect.HealthConnectManager
import android.health.connect.datatypes.AggregationType
import android.health.connect.datatypes.units.Energy
import android.health.connect.datatypes.units.Length
import android.os.OutcomeReceiver
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
import com.android.healthconnect.controller.shared.app.AppInfoReader
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.createFakeAppInfoReader
import com.android.healthconnect.controller.tests.utils.setLocale
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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.invocation.InvocationOnMock
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@UninstallModules(HealthManagerModule::class)
@RunWith(AndroidJUnit4::class)
class LoadDataAggregationsUseCaseTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    private lateinit var context: Context
    @BindValue lateinit var appInfoReader: AppInfoReader
    @BindValue
    val healthConnectManager: HealthConnectManager = Mockito.mock(HealthConnectManager::class.java)
    private lateinit var loadDataAggregationsUseCase: LoadDataAggregationsUseCase

    @Inject lateinit var loadEntriesHelper: LoadEntriesHelper

    @Inject lateinit var stepsFormatter: StepsFormatter

    @Inject lateinit var totalCaloriesBurnedFormatter: TotalCaloriesBurnedFormatter

    @Inject lateinit var distanceFormatter: DistanceFormatter

    @Inject lateinit var sleepSessionFormatter: SleepSessionFormatter

    @Inject lateinit var mindfulnessSessionFormatter: MindfulnessSessionFormatter

    @Before
    fun setup() = runTest {
        MockitoAnnotations.initMocks(this)
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
                Dispatchers.Main,
            )
    }

    @Test
    fun loadDataAggregationsUseCase_withPeriodAggregationForSteps_returnsFormattedStepsAggregation() =
        runTest {
            doAnswer(prepareStepsAggregationAnswer())
                .whenever(healthConnectManager)
                .aggregate<Long>(any(), any(), any())

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
            doAnswer(prepareDistanceAggregationAnswer())
                .whenever(healthConnectManager)
                .aggregate<Length>(any(), any(), any())

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
            doAnswer(prepareCaloriesAggregationAnswer())
                .whenever(healthConnectManager)
                .aggregate<Energy>(any(), any(), any())

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
            doAnswer(prepareSleepAggregationAnswer())
                .whenever(healthConnectManager)
                .aggregate<Long>(any(), any(), any())

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
            doAnswer(prepareMindfulnessAggregationAnswer(Duration.ofHours(6).plusMinutes(15)))
                .whenever(healthConnectManager)
                .aggregate<Long>(any(), any(), any())

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

    private fun prepareStepsAggregationAnswer(): (InvocationOnMock) -> Unit {
        val answer = { args: InvocationOnMock ->
            val receiver = args.arguments[2] as OutcomeReceiver<AggregateRecordsResponse<Long>, *>
            receiver.onResult(getStepsAggregationResponse())
        }
        return answer
    }

    private fun prepareDistanceAggregationAnswer(): (InvocationOnMock) -> Unit {
        val answer = { args: InvocationOnMock ->
            val receiver = args.arguments[2] as OutcomeReceiver<AggregateRecordsResponse<Length>, *>
            receiver.onResult(getDistanceAggregationResponse())
        }
        return answer
    }

    private fun prepareCaloriesAggregationAnswer(): (InvocationOnMock) -> Unit {
        val answer = { args: InvocationOnMock ->
            val receiver = args.arguments[2] as OutcomeReceiver<AggregateRecordsResponse<Energy>, *>
            receiver.onResult(getCaloriesAggregationResponse())
        }
        return answer
    }

    private fun prepareSleepAggregationAnswer(): (InvocationOnMock) -> Unit {
        val answer = { args: InvocationOnMock ->
            val receiver = args.arguments[2] as OutcomeReceiver<AggregateRecordsResponse<Long>, *>
            receiver.onResult(getSleepAggregationResponse())
        }
        return answer
    }

    private fun prepareMindfulnessAggregationAnswer(
        duration: Duration
    ): (InvocationOnMock) -> Unit {
        val answer = { args: InvocationOnMock ->
            val receiver = args.arguments[2] as OutcomeReceiver<AggregateRecordsResponse<Long>, *>
            receiver.onResult(getMindfulnessAggregationResponse(duration))
        }
        return answer
    }

    private fun getStepsAggregationResponse(): AggregateRecordsResponse<Long> {
        val aggregationResult =
            AggregateResult<Long>(
                100,
                /* zoneOffset = */ null,
                AggregateResult.convertDataOrigins(listOf(TEST_APP_PACKAGE_NAME)),
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
