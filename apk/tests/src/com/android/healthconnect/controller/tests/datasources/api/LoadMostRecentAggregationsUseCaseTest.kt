package com.android.healthconnect.controller.tests.datasources.api

import android.content.Context
import android.health.connect.HealthConnectException
import android.health.connect.HealthConnectManager
import android.health.connect.HealthDataCategory
import android.health.connect.datatypes.Record
import android.health.connect.datatypes.SleepSessionRecord
import android.os.OutcomeReceiver
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.data.entries.FormattedEntry
import com.android.healthconnect.controller.datasources.AggregationCardInfo
import com.android.healthconnect.controller.datasources.api.LoadMostRecentAggregationsUseCase
import com.android.healthconnect.controller.permissions.data.HealthPermissionType
import com.android.healthconnect.controller.shared.HealthPermissionToDatatypeMapper
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import com.android.healthconnect.controller.tests.utils.di.FakeLoadDataAggregationsUseCase
import com.android.healthconnect.controller.tests.utils.di.FakeLoadSleepDataUseCase
import com.android.healthconnect.controller.tests.utils.getMetaData
import com.android.healthconnect.controller.tests.utils.setLocale
import com.android.healthconnect.controller.utils.atStartOfDay
import com.android.healthconnect.controller.utils.toLocalDate
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.invocation.InvocationOnMock

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
class LoadMostRecentAggregationsUseCaseTest {

    companion object {
        private fun formattedAggregation(aggregation: String) =
            FormattedEntry.FormattedAggregation(
                aggregation = aggregation,
                aggregationA11y = aggregation,
                contributingApps = "Test App")
    }

    @get:Rule val hiltRule = HiltAndroidRule(this)

    private lateinit var context: Context
    private lateinit var loadMostRecentAggregationsUseCase: LoadMostRecentAggregationsUseCase

    private val healthConnectManager: HealthConnectManager =
        Mockito.mock(HealthConnectManager::class.java)
    private val loadDataAggregationsUseCase = FakeLoadDataAggregationsUseCase()
    private val loadSleepDataUseCase = FakeLoadSleepDataUseCase()

    private val STEPS_DATE_1 = Instant.parse("2022-10-24T18:40:13.00Z")
    private val STEPS_DATE_2 = Instant.parse("2022-10-26T13:23:19.00Z")
    private val STEPS_DATE_3 = Instant.parse("2023-04-09T19:45:12.00Z")

    private val DISTANCE_DATE_1 = Instant.parse("2022-05-12T14:15:22.00Z")
    private val DISTANCE_DATE_2 = Instant.parse("2022-11-03T07:20:18.00Z")
    private val DISTANCE_DATE_3 = Instant.parse("2023-02-08T16:42:29.00Z")

    private val CALORIES_DATE_1 = Instant.parse("2022-07-26T11:33:10.00Z")
    private val CALORIES_DATE_2 = Instant.parse("2022-09-30T12:55:44.00Z")
    private val CALORIES_DATE_3 = Instant.parse("2023-04-19T20:25:37.00Z")

    private val SLEEP_DATE_1 = Instant.parse("2022-03-17T12:34:56.00Z")
    private val SLEEP_DATE_2 = Instant.parse("2022-09-21T14:45:37.00Z")
    private val SLEEP_DATE_3 = Instant.parse("2023-02-13T23:00:00.00Z")

    private val stepsAggregation = formattedAggregation("100 steps")
    private val distanceAggregation = formattedAggregation("1.5 km")
    private val caloriesAggregation = formattedAggregation("1590 kcal")
    private val sleepAggregation = formattedAggregation("11h 5m")

    private val stepsRecordTypes =
        HealthPermissionToDatatypeMapper.getDataTypes(HealthPermissionType.STEPS)
    private val distanceRecordTypes =
        HealthPermissionToDatatypeMapper.getDataTypes(HealthPermissionType.DISTANCE)
    private val caloriesRecordTypes =
        HealthPermissionToDatatypeMapper.getDataTypes(HealthPermissionType.TOTAL_CALORIES_BURNED)
    private val sleepRecordTypes =
        HealthPermissionToDatatypeMapper.getDataTypes(HealthPermissionType.SLEEP)

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        context = InstrumentationRegistry.getInstrumentation().context
        context.setLocale(Locale.US)
        hiltRule.inject()
        context = InstrumentationRegistry.getInstrumentation().context
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("UTC")))
        loadMostRecentAggregationsUseCase =
            LoadMostRecentAggregationsUseCase(
                healthConnectManager,
                loadDataAggregationsUseCase,
                loadSleepDataUseCase,
                Dispatchers.Main)
    }

    @After
    fun tearDown() {
        loadDataAggregationsUseCase.reset()
        loadSleepDataUseCase.reset()
    }

    @Test
    fun loadMostRecentAggregations_forActivity_returnsMostRecent_stepsDistanceCalories() = runTest {
        Mockito.doAnswer(prepareStepsAnswer())
            .`when`(healthConnectManager)
            .queryActivityDates(eq(stepsRecordTypes), any(), any())

        Mockito.doAnswer(prepareDistanceAnswer())
            .`when`(healthConnectManager)
            .queryActivityDates(eq(distanceRecordTypes), any(), any())

        Mockito.doAnswer(prepareCaloriesAnswer())
            .`when`(healthConnectManager)
            .queryActivityDates(eq(caloriesRecordTypes), any(), any())

        Mockito.doAnswer(prepareEmptyAnswer())
            .`when`(healthConnectManager)
            .queryActivityDates(eq(sleepRecordTypes), any(), any())

        loadDataAggregationsUseCase.updateAggregationResponses(
            listOf(stepsAggregation, distanceAggregation, caloriesAggregation))

        val result = loadMostRecentAggregationsUseCase.invoke(HealthDataCategory.ACTIVITY)
        assertThat(result is UseCaseResults.Success).isTrue()
        assertThat((result as UseCaseResults.Success).data)
            .isEqualTo(
                listOf(
                    AggregationCardInfo(
                        HealthPermissionType.STEPS, stepsAggregation, STEPS_DATE_3.atStartOfDay()),
                    AggregationCardInfo(
                        HealthPermissionType.DISTANCE,
                        distanceAggregation,
                        DISTANCE_DATE_3.atStartOfDay()),
                    AggregationCardInfo(
                        HealthPermissionType.TOTAL_CALORIES_BURNED,
                        caloriesAggregation,
                        CALORIES_DATE_3.atStartOfDay()),
                ))
    }

    // Case 1 - start and end times on same day
    @Test
    fun loadMostRecentAggregations_forSleep_allSessionStartAndEndOnSameDay() = runTest {
        Mockito.doAnswer(prepareEmptyAnswer())
            .`when`(healthConnectManager)
            .queryActivityDates(eq(stepsRecordTypes), any(), any())

        Mockito.doAnswer(prepareEmptyAnswer())
            .`when`(healthConnectManager)
            .queryActivityDates(eq(distanceRecordTypes), any(), any())

        Mockito.doAnswer(prepareEmptyAnswer())
            .`when`(healthConnectManager)
            .queryActivityDates(eq(caloriesRecordTypes), any(), any())

        Mockito.doAnswer(prepareSleepAnswer())
            .`when`(healthConnectManager)
            .queryActivityDates(eq(sleepRecordTypes), any(), any())

        // lastDayWithSleepData = 2023-02-13

        // 2h
        val SLEEP_SESSION_1_START_DATE = Instant.parse("2023-02-13T16:00:00.00Z")
        val SLEEP_SESSION_1_END_DATE = Instant.parse("2023-02-13T18:00:00.00Z")

        // 5h 45m
        val SLEEP_SESSION_2_START_DATE = Instant.parse("2023-02-13T17:30:00.00Z")
        val SLEEP_SESSION_2_END_DATE = Instant.parse("2023-02-13T23:15:00.00Z")

        // 7h 20m
        val SLEEP_SESSION_3_START_DATE = Instant.parse("2023-02-13T01:00:00.00Z")
        val SLEEP_SESSION_3_END_DATE = Instant.parse("2023-02-13T08:20:00.00Z")

        loadSleepDataUseCase.updateSleepData(
            SLEEP_SESSION_1_START_DATE.toLocalDate(),
            getSleepSessionRecords(
                listOf(
                    Pair(SLEEP_SESSION_1_START_DATE, SLEEP_SESSION_1_END_DATE),
                    Pair(SLEEP_SESSION_2_START_DATE, SLEEP_SESSION_2_END_DATE),
                    Pair(SLEEP_SESSION_3_START_DATE, SLEEP_SESSION_3_END_DATE))))

        val expectedSleepAggregation = formattedAggregation("14h 5m")
        loadDataAggregationsUseCase.updateAggregationResponses(listOf(expectedSleepAggregation))

        val result = loadMostRecentAggregationsUseCase.invoke(HealthDataCategory.SLEEP)
        assertThat(result is UseCaseResults.Success).isTrue()
        assertThat((result as UseCaseResults.Success).data)
            .isEqualTo(
                listOf(
                    AggregationCardInfo(
                        HealthPermissionType.SLEEP,
                        expectedSleepAggregation,
                        SLEEP_SESSION_1_START_DATE.atStartOfDay(),
                        SLEEP_SESSION_1_END_DATE.atStartOfDay())))
    }

    // Case 1 - start and end times on same day
    // Edge case - additional sleep session starts on past date (not day before)
    // And finishes on last day with data
    @Test
    fun loadMostRecentAggregations_forSleep_allSessionStartAndEndOnSameDay_withSessionUnknownStart() =
        runTest {
            Mockito.doAnswer(prepareEmptyAnswer())
                .`when`(healthConnectManager)
                .queryActivityDates(eq(stepsRecordTypes), any(), any())

            Mockito.doAnswer(prepareEmptyAnswer())
                .`when`(healthConnectManager)
                .queryActivityDates(eq(distanceRecordTypes), any(), any())

            Mockito.doAnswer(prepareEmptyAnswer())
                .`when`(healthConnectManager)
                .queryActivityDates(eq(caloriesRecordTypes), any(), any())

            Mockito.doAnswer(prepareSleepAnswer())
                .`when`(healthConnectManager)
                .queryActivityDates(eq(sleepRecordTypes), any(), any())

            // lastDayWithSleepData = 2023-02-13

            // 2h
            val SLEEP_SESSION_1_START_DATE = Instant.parse("2023-02-13T16:00:00.00Z")
            val SLEEP_SESSION_1_END_DATE = Instant.parse("2023-02-13T18:00:00.00Z")

            // 5h 45m
            val SLEEP_SESSION_2_START_DATE = Instant.parse("2023-02-13T17:30:00.00Z")
            val SLEEP_SESSION_2_END_DATE = Instant.parse("2023-02-13T23:15:00.00Z")

            // 7h 20m
            val SLEEP_SESSION_3_START_DATE = Instant.parse("2023-02-13T01:00:00.00Z")
            val SLEEP_SESSION_3_END_DATE = Instant.parse("2023-02-13T08:20:00.00Z")

            // Past sleep session ending on lastDayWithData, overlaps with above data by 1 hour
            // 3d 7h 20m
            val SLEEP_SESSION_4_START_DATE = Instant.parse("2023-02-10T01:00:00.00Z")
            val SLEEP_SESSION_4_END_DATE = Instant.parse("2023-02-13T08:20:00.00Z")

            loadSleepDataUseCase.updateSleepData(
                SLEEP_SESSION_1_START_DATE.toLocalDate(),
                getSleepSessionRecords(
                    listOf(
                        Pair(SLEEP_SESSION_1_START_DATE, SLEEP_SESSION_1_END_DATE),
                        Pair(SLEEP_SESSION_2_START_DATE, SLEEP_SESSION_2_END_DATE),
                        Pair(SLEEP_SESSION_3_START_DATE, SLEEP_SESSION_3_END_DATE))))

            loadSleepDataUseCase.updateSleepData(
                SLEEP_SESSION_4_START_DATE.toLocalDate(),
                getSleepSessionRecords(
                    listOf(Pair(SLEEP_SESSION_4_START_DATE, SLEEP_SESSION_4_END_DATE))))

            val expectedSleepAggregation = formattedAggregation("15h 5m")
            loadDataAggregationsUseCase.updateAggregationResponses(listOf(expectedSleepAggregation))

            val result = loadMostRecentAggregationsUseCase.invoke(HealthDataCategory.SLEEP)
            assertThat(result is UseCaseResults.Success).isTrue()
            assertThat((result as UseCaseResults.Success).data)
                .isEqualTo(
                    listOf(
                        AggregationCardInfo(
                            HealthPermissionType.SLEEP,
                            expectedSleepAggregation,
                            SLEEP_SESSION_1_START_DATE.atStartOfDay(),
                            SLEEP_SESSION_1_END_DATE.atStartOfDay())))
        }

    // Case 1 - start and end times on same day
    // Edge case - additional sleep session starts on past date (not day before)
    // And finishes on future date
    @Test
    fun loadMostRecentAggregations_forSleep_allSessionStartAndEndOnSameDay_withSessionUnknownStartEnd() =
        runTest {
            Mockito.doAnswer(prepareEmptyAnswer())
                .`when`(healthConnectManager)
                .queryActivityDates(eq(stepsRecordTypes), any(), any())

            Mockito.doAnswer(prepareEmptyAnswer())
                .`when`(healthConnectManager)
                .queryActivityDates(eq(distanceRecordTypes), any(), any())

            Mockito.doAnswer(prepareEmptyAnswer())
                .`when`(healthConnectManager)
                .queryActivityDates(eq(caloriesRecordTypes), any(), any())

            Mockito.doAnswer(prepareSleepAnswer())
                .`when`(healthConnectManager)
                .queryActivityDates(eq(sleepRecordTypes), any(), any())

            // lastDayWithSleepData = 2023-02-13

            // 2h
            val SLEEP_SESSION_1_START_DATE = Instant.parse("2023-02-13T16:00:00.00Z")
            val SLEEP_SESSION_1_END_DATE = Instant.parse("2023-02-13T18:00:00.00Z")

            // 5h 45m
            val SLEEP_SESSION_2_START_DATE = Instant.parse("2023-02-13T17:30:00.00Z")
            val SLEEP_SESSION_2_END_DATE = Instant.parse("2023-02-13T23:15:00.00Z")

            // 7h 20m
            val SLEEP_SESSION_3_START_DATE = Instant.parse("2023-02-13T01:00:00.00Z")
            val SLEEP_SESSION_3_END_DATE = Instant.parse("2023-02-13T08:20:00.00Z")

            // Past sleep session, overlaps completely with above data
            // 5d 7h 20m
            val SLEEP_SESSION_4_START_DATE = Instant.parse("2023-02-10T01:00:00.00Z")
            val SLEEP_SESSION_4_END_DATE = Instant.parse("2023-02-15T08:20:00.00Z")

            loadSleepDataUseCase.updateSleepData(
                SLEEP_SESSION_1_START_DATE.toLocalDate(),
                getSleepSessionRecords(
                    listOf(
                        Pair(SLEEP_SESSION_1_START_DATE, SLEEP_SESSION_1_END_DATE),
                        Pair(SLEEP_SESSION_2_START_DATE, SLEEP_SESSION_2_END_DATE),
                        Pair(SLEEP_SESSION_3_START_DATE, SLEEP_SESSION_3_END_DATE))))

            loadSleepDataUseCase.updateSleepData(
                SLEEP_SESSION_4_START_DATE.toLocalDate(),
                getSleepSessionRecords(
                    listOf(Pair(SLEEP_SESSION_4_START_DATE, SLEEP_SESSION_4_END_DATE))))

            // minStartTime = SLEEP_SESSION_3_START_DATE
            // maxEndTime = SLEEP_SESSION_2_END_DATE
            // Total time = 1am to 23:15 = 22h 15m
            val expectedSleepAggregation = formattedAggregation("22h 15m")
            loadDataAggregationsUseCase.updateAggregationResponses(listOf(expectedSleepAggregation))

            val result = loadMostRecentAggregationsUseCase.invoke(HealthDataCategory.SLEEP)
            assertThat(result is UseCaseResults.Success).isTrue()
            assertThat((result as UseCaseResults.Success).data)
                .isEqualTo(
                    listOf(
                        AggregationCardInfo(
                            HealthPermissionType.SLEEP,
                            expectedSleepAggregation,
                            SLEEP_SESSION_1_START_DATE.atStartOfDay(),
                            SLEEP_SESSION_1_END_DATE.atStartOfDay())))
        }

    // Case 2 - At least one session starts on Day 1 and finishes on Day 2 or later
    @Test
    fun loadMostRecentAggregations_forSleep_atLeastOneSessionStartsYesterdayAndEndsToday() =
        runTest {
            Mockito.doAnswer(prepareEmptyAnswer())
                .`when`(healthConnectManager)
                .queryActivityDates(eq(stepsRecordTypes), any(), any())

            Mockito.doAnswer(prepareEmptyAnswer())
                .`when`(healthConnectManager)
                .queryActivityDates(eq(distanceRecordTypes), any(), any())

            Mockito.doAnswer(prepareEmptyAnswer())
                .`when`(healthConnectManager)
                .queryActivityDates(eq(caloriesRecordTypes), any(), any())

            Mockito.doAnswer(prepareSleepAnswer())
                .`when`(healthConnectManager)
                .queryActivityDates(eq(sleepRecordTypes), any(), any())

            // lastDayWithSleepData = 2023-02-13

            // 2h
            val SLEEP_SESSION_1_START_DATE = Instant.parse("2023-02-13T16:00:00.00Z")
            val SLEEP_SESSION_1_END_DATE = Instant.parse("2023-02-13T18:00:00.00Z")

            // 5h 45m
            val SLEEP_SESSION_2_START_DATE = Instant.parse("2023-02-13T17:30:00.00Z")
            val SLEEP_SESSION_2_END_DATE = Instant.parse("2023-02-13T23:15:00.00Z")

            // 9h 20m
            val SLEEP_SESSION_3_START_DATE = Instant.parse("2023-02-12T23:00:00.00Z")
            val SLEEP_SESSION_3_END_DATE = Instant.parse("2023-02-13T08:20:00.00Z")

            // Should be partially included in aggregation
            val SLEEP_SESSION_4_START_DATE = Instant.parse("2023-02-12T16:00:00.00Z")
            val SLEEP_SESSION_4_END_DATE = Instant.parse("2023-02-12T23:20:00.00Z")

            // Should not be included in aggregation
            val SLEEP_SESSION_5_START_DATE = Instant.parse("2023-02-12T12:00:00.00Z")
            val SLEEP_SESSION_5_END_DATE = Instant.parse("2023-02-12T14:20:00.00Z")

            loadSleepDataUseCase.updateSleepData(
                SLEEP_SESSION_1_START_DATE.toLocalDate(),
                getSleepSessionRecords(
                    listOf(
                        Pair(SLEEP_SESSION_1_START_DATE, SLEEP_SESSION_1_END_DATE),
                        Pair(SLEEP_SESSION_2_START_DATE, SLEEP_SESSION_2_END_DATE))))

            loadSleepDataUseCase.updateSleepData(
                SLEEP_SESSION_3_START_DATE.toLocalDate(),
                getSleepSessionRecords(
                    listOf(
                        Pair(SLEEP_SESSION_3_START_DATE, SLEEP_SESSION_3_END_DATE),
                        Pair(SLEEP_SESSION_4_START_DATE, SLEEP_SESSION_4_END_DATE),
                        Pair(SLEEP_SESSION_5_START_DATE, SLEEP_SESSION_5_END_DATE))))

            // minStartTime = SLEEP_SESSION_3_START_DATE
            // maxEndTime = SLEEP_SESSION_2_END_DATE
            // Total time = 12 Feb, 23:00 - 13 Feb 23:15 = 24h 15m
            val expectedSleepAggregation = formattedAggregation("24h 15m")
            loadDataAggregationsUseCase.updateAggregationResponses(listOf(expectedSleepAggregation))

            val result = loadMostRecentAggregationsUseCase.invoke(HealthDataCategory.SLEEP)
            assertThat(result is UseCaseResults.Success).isTrue()
            assertThat((result as UseCaseResults.Success).data)
                .isEqualTo(
                    listOf(
                        AggregationCardInfo(
                            HealthPermissionType.SLEEP,
                            expectedSleepAggregation,
                            SLEEP_SESSION_3_START_DATE.atStartOfDay(),
                            SLEEP_SESSION_1_END_DATE.atStartOfDay())))
        }

    // Case 2 - At least one session starts on Day 1 and finishes on Day 2 or later
    // with gaps
    @Test
    fun loadMostRecentAggregations_forSleep_atLeastOneSessionStartsYesterdayAndEndsToday_withGaps() =
        runTest {
            Mockito.doAnswer(prepareEmptyAnswer())
                .`when`(healthConnectManager)
                .queryActivityDates(eq(stepsRecordTypes), any(), any())

            Mockito.doAnswer(prepareEmptyAnswer())
                .`when`(healthConnectManager)
                .queryActivityDates(eq(distanceRecordTypes), any(), any())

            Mockito.doAnswer(prepareEmptyAnswer())
                .`when`(healthConnectManager)
                .queryActivityDates(eq(caloriesRecordTypes), any(), any())

            Mockito.doAnswer(prepareSleepAnswer())
                .`when`(healthConnectManager)
                .queryActivityDates(eq(sleepRecordTypes), any(), any())

            // lastDayWithSleepData = 2023-02-13

            // 2h
            val SLEEP_SESSION_1_START_DATE = Instant.parse("2023-02-13T18:00:00.00Z")
            val SLEEP_SESSION_1_END_DATE = Instant.parse("2023-02-13T20:00:00.00Z")

            // 2h 15m
            val SLEEP_SESSION_2_START_DATE = Instant.parse("2023-02-13T12:30:00.00Z")
            val SLEEP_SESSION_2_END_DATE = Instant.parse("2023-02-13T14:45:00.00Z")

            // 5h 20m
            val SLEEP_SESSION_3_START_DATE = Instant.parse("2023-02-12T20:00:00.00Z")
            val SLEEP_SESSION_3_END_DATE = Instant.parse("2023-02-13T01:20:00.00Z")

            // Should be partially included in aggregation
            val SLEEP_SESSION_4_START_DATE = Instant.parse("2023-02-12T16:00:00.00Z")
            val SLEEP_SESSION_4_END_DATE = Instant.parse("2023-02-12T23:20:00.00Z")

            loadSleepDataUseCase.updateSleepData(
                SLEEP_SESSION_1_START_DATE.toLocalDate(),
                getSleepSessionRecords(
                    listOf(
                        Pair(SLEEP_SESSION_1_START_DATE, SLEEP_SESSION_1_END_DATE),
                        Pair(SLEEP_SESSION_2_START_DATE, SLEEP_SESSION_2_END_DATE))))

            loadSleepDataUseCase.updateSleepData(
                SLEEP_SESSION_3_START_DATE.toLocalDate(),
                getSleepSessionRecords(
                    listOf(
                        Pair(SLEEP_SESSION_3_START_DATE, SLEEP_SESSION_3_END_DATE),
                        Pair(SLEEP_SESSION_4_START_DATE, SLEEP_SESSION_4_END_DATE))))

            // minStartTime = SLEEP_SESSION_3_START_DATE
            // maxEndTime = SLEEP_SESSION_1_END_DATE
            // Total time = 2h + 2h 15m + 5h 20m = 9h 35m
            val expectedSleepAggregation = formattedAggregation("9h 35m")
            loadDataAggregationsUseCase.updateAggregationResponses(listOf(expectedSleepAggregation))

            val result = loadMostRecentAggregationsUseCase.invoke(HealthDataCategory.SLEEP)
            assertThat(result is UseCaseResults.Success).isTrue()
            assertThat((result as UseCaseResults.Success).data)
                .isEqualTo(
                    listOf(
                        AggregationCardInfo(
                            HealthPermissionType.SLEEP,
                            expectedSleepAggregation,
                            SLEEP_SESSION_3_START_DATE.atStartOfDay(),
                            SLEEP_SESSION_1_END_DATE.atStartOfDay())))
        }

    // Case 2 - At least one session starts on Day 1 and finishes on Day 2 or later
    // Edge case - additional sleep session starts on past date
    // and finishes on lastDayWithData
    @Test
    fun loadMostRecentAggregations_forSleep_atLeastOneSessionStartsYesterdayAndEndsToday_withSessionUnknownStart() =
        runTest {
            Mockito.doAnswer(prepareEmptyAnswer())
                .`when`(healthConnectManager)
                .queryActivityDates(eq(stepsRecordTypes), any(), any())

            Mockito.doAnswer(prepareEmptyAnswer())
                .`when`(healthConnectManager)
                .queryActivityDates(eq(distanceRecordTypes), any(), any())

            Mockito.doAnswer(prepareEmptyAnswer())
                .`when`(healthConnectManager)
                .queryActivityDates(eq(caloriesRecordTypes), any(), any())

            Mockito.doAnswer(prepareSleepAnswer())
                .`when`(healthConnectManager)
                .queryActivityDates(eq(sleepRecordTypes), any(), any())

            // secondToLastDayWithSleepData = 2023-02-12
            // lastDayWithSleepData = 2023-02-13

            // 2h
            val SLEEP_SESSION_1_START_DATE = Instant.parse("2023-02-13T16:00:00.00Z")
            val SLEEP_SESSION_1_END_DATE = Instant.parse("2023-02-13T18:00:00.00Z")

            // 5h 45m
            val SLEEP_SESSION_2_START_DATE = Instant.parse("2023-02-13T17:30:00.00Z")
            val SLEEP_SESSION_2_END_DATE = Instant.parse("2023-02-13T23:15:00.00Z")

            // 10h 20m
            val SLEEP_SESSION_3_START_DATE = Instant.parse("2023-02-12T22:00:00.00Z")
            val SLEEP_SESSION_3_END_DATE = Instant.parse("2023-02-13T08:20:00.00Z")

            // Should be partially included in aggregation
            val SLEEP_SESSION_4_START_DATE = Instant.parse("2023-02-12T16:00:00.00Z")
            val SLEEP_SESSION_4_END_DATE = Instant.parse("2023-02-12T23:20:00.00Z")

            // Should be partially included in aggregation
            val SLEEP_SESSION_5_START_DATE = Instant.parse("2023-02-10T12:00:00.00Z")
            val SLEEP_SESSION_5_END_DATE = Instant.parse("2023-02-13T14:20:00.00Z")

            loadSleepDataUseCase.updateSleepData(
                SLEEP_SESSION_1_START_DATE.toLocalDate(),
                getSleepSessionRecords(
                    listOf(
                        Pair(SLEEP_SESSION_1_START_DATE, SLEEP_SESSION_1_END_DATE),
                        Pair(SLEEP_SESSION_2_START_DATE, SLEEP_SESSION_2_END_DATE))))

            loadSleepDataUseCase.updateSleepData(
                SLEEP_SESSION_3_START_DATE.toLocalDate(),
                getSleepSessionRecords(
                    listOf(
                        Pair(SLEEP_SESSION_3_START_DATE, SLEEP_SESSION_3_END_DATE),
                        Pair(SLEEP_SESSION_4_START_DATE, SLEEP_SESSION_4_END_DATE))))

            loadSleepDataUseCase.updateSleepData(
                SLEEP_SESSION_5_START_DATE.toLocalDate(),
                getSleepSessionRecords(
                    listOf(Pair(SLEEP_SESSION_5_START_DATE, SLEEP_SESSION_5_END_DATE))))

            // minStartTime = SLEEP_SESSION_3_START_DATE
            // maxEndTime = SLEEP_SESSION_2_END_DATE
            // Total time = 12 Feb, 22:00 - 13 Feb 23:15 = 25h 15m
            val expectedSleepAggregation = formattedAggregation("25h 15m")
            loadDataAggregationsUseCase.updateAggregationResponses(listOf(expectedSleepAggregation))

            val result = loadMostRecentAggregationsUseCase.invoke(HealthDataCategory.SLEEP)
            assertThat(result is UseCaseResults.Success).isTrue()
            assertThat((result as UseCaseResults.Success).data)
                .isEqualTo(
                    listOf(
                        AggregationCardInfo(
                            HealthPermissionType.SLEEP,
                            expectedSleepAggregation,
                            SLEEP_SESSION_3_START_DATE.atStartOfDay(),
                            SLEEP_SESSION_2_END_DATE.atStartOfDay())))
        }

    // Case 2 - At least one session starts on Day 1 and finishes on Day 2 or later
    // Edge case - additional sleep session starts on Day 1
    // and finishes on unknown date
    @Test
    fun loadMostRecentAggregations_forSleep_atLeastOneSessionStartsYesterdayAndEndsToday_withSessionUnknownEnd() =
        runTest {
            Mockito.doAnswer(prepareEmptyAnswer())
                .`when`(healthConnectManager)
                .queryActivityDates(eq(stepsRecordTypes), any(), any())

            Mockito.doAnswer(prepareEmptyAnswer())
                .`when`(healthConnectManager)
                .queryActivityDates(eq(distanceRecordTypes), any(), any())

            Mockito.doAnswer(prepareEmptyAnswer())
                .`when`(healthConnectManager)
                .queryActivityDates(eq(caloriesRecordTypes), any(), any())

            Mockito.doAnswer(prepareSleepAnswer())
                .`when`(healthConnectManager)
                .queryActivityDates(eq(sleepRecordTypes), any(), any())

            // secondToLastDayWithSleepData = 2023-02-12
            // lastDayWithSleepData = 2023-02-13

            // 2h
            val SLEEP_SESSION_1_START_DATE = Instant.parse("2023-02-13T16:00:00.00Z")
            val SLEEP_SESSION_1_END_DATE = Instant.parse("2023-02-13T18:00:00.00Z")

            // 5h 45m
            val SLEEP_SESSION_2_START_DATE = Instant.parse("2023-02-13T17:30:00.00Z")
            val SLEEP_SESSION_2_END_DATE = Instant.parse("2023-02-13T23:15:00.00Z")

            // 10h 20m
            val SLEEP_SESSION_3_START_DATE = Instant.parse("2023-02-12T22:00:00.00Z")
            val SLEEP_SESSION_3_END_DATE = Instant.parse("2023-02-13T08:20:00.00Z")

            // Should be partially included in aggregation
            val SLEEP_SESSION_4_START_DATE = Instant.parse("2023-02-12T16:00:00.00Z")
            val SLEEP_SESSION_4_END_DATE = Instant.parse("2023-02-12T23:20:00.00Z")

            // Should be partially included in aggregation up to 2023-02-14T00:00
            val SLEEP_SESSION_5_START_DATE = Instant.parse("2023-02-12T12:00:00.00Z")
            val SLEEP_SESSION_5_END_DATE = Instant.parse("2023-02-18T14:20:00.00Z")

            val maxDate = Instant.parse("2023-02-14T00:00:00.00Z")

            loadSleepDataUseCase.updateSleepData(
                SLEEP_SESSION_1_START_DATE.toLocalDate(),
                getSleepSessionRecords(
                    listOf(
                        Pair(SLEEP_SESSION_1_START_DATE, SLEEP_SESSION_1_END_DATE),
                        Pair(SLEEP_SESSION_2_START_DATE, SLEEP_SESSION_2_END_DATE))))

            loadSleepDataUseCase.updateSleepData(
                SLEEP_SESSION_3_START_DATE.toLocalDate(),
                getSleepSessionRecords(
                    listOf(
                        Pair(SLEEP_SESSION_3_START_DATE, SLEEP_SESSION_3_END_DATE),
                        Pair(SLEEP_SESSION_4_START_DATE, SLEEP_SESSION_4_END_DATE),
                        Pair(SLEEP_SESSION_5_START_DATE, SLEEP_SESSION_5_END_DATE))))

            // minStartTime = SLEEP_SESSION_5_START_DATE
            // maxEndTime = 2023-02-14T00:00
            // Total time = 12 Feb, 12:00 - 14 Feb 00:00 = 36h
            val expectedSleepAggregation = formattedAggregation("36h")
            loadDataAggregationsUseCase.updateAggregationResponses(listOf(expectedSleepAggregation))

            val result = loadMostRecentAggregationsUseCase.invoke(HealthDataCategory.SLEEP)
            assertThat(result is UseCaseResults.Success).isTrue()
            assertThat((result as UseCaseResults.Success).data)
                .isEqualTo(
                    listOf(
                        AggregationCardInfo(
                            HealthPermissionType.SLEEP,
                            expectedSleepAggregation,
                            SLEEP_SESSION_5_START_DATE.atStartOfDay(),
                            maxDate.atStartOfDay())))
        }

    // Case 2 - At least one session starts on Day 1 and finishes on Day 2 or later
    // Edge case - additional sleep session starts and finishes on unknown date
    @Test
    fun loadMostRecentAggregations_forSleep_atLeastOneSessionStartsYesterdayAndEndsToday_withSessionUnknownStartAndEnd() =
        runTest {
            Mockito.doAnswer(prepareEmptyAnswer())
                .`when`(healthConnectManager)
                .queryActivityDates(eq(stepsRecordTypes), any(), any())

            Mockito.doAnswer(prepareEmptyAnswer())
                .`when`(healthConnectManager)
                .queryActivityDates(eq(distanceRecordTypes), any(), any())

            Mockito.doAnswer(prepareEmptyAnswer())
                .`when`(healthConnectManager)
                .queryActivityDates(eq(caloriesRecordTypes), any(), any())

            Mockito.doAnswer(prepareSleepAnswer())
                .`when`(healthConnectManager)
                .queryActivityDates(eq(sleepRecordTypes), any(), any())

            // secondToLastDayWithSleepData = 2023-02-12
            // lastDayWithSleepData = 2023-02-13

            // 2h
            val SLEEP_SESSION_1_START_DATE = Instant.parse("2023-02-13T18:00:00.00Z")
            val SLEEP_SESSION_1_END_DATE = Instant.parse("2023-02-13T20:00:00.00Z")

            // 2h 15m
            val SLEEP_SESSION_2_START_DATE = Instant.parse("2023-02-13T12:30:00.00Z")
            val SLEEP_SESSION_2_END_DATE = Instant.parse("2023-02-13T14:45:00.00Z")

            // 5h 20m
            val SLEEP_SESSION_3_START_DATE = Instant.parse("2023-02-12T20:00:00.00Z")
            val SLEEP_SESSION_3_END_DATE = Instant.parse("2023-02-13T01:20:00.00Z")

            // Should be partially included in aggregation
            val SLEEP_SESSION_4_START_DATE = Instant.parse("2023-02-10T16:00:00.00Z")
            val SLEEP_SESSION_4_END_DATE = Instant.parse("2023-02-20T23:20:00.00Z")

            loadSleepDataUseCase.updateSleepData(
                SLEEP_SESSION_1_START_DATE.toLocalDate(),
                getSleepSessionRecords(
                    listOf(
                        Pair(SLEEP_SESSION_1_START_DATE, SLEEP_SESSION_1_END_DATE),
                        Pair(SLEEP_SESSION_2_START_DATE, SLEEP_SESSION_2_END_DATE))))

            loadSleepDataUseCase.updateSleepData(
                SLEEP_SESSION_3_START_DATE.toLocalDate(),
                getSleepSessionRecords(
                    listOf(Pair(SLEEP_SESSION_3_START_DATE, SLEEP_SESSION_3_END_DATE))))

            loadSleepDataUseCase.updateSleepData(
                SLEEP_SESSION_4_START_DATE.toLocalDate(),
                getSleepSessionRecords(
                    listOf(Pair(SLEEP_SESSION_4_START_DATE, SLEEP_SESSION_4_END_DATE))))

            // minStartTime = SLEEP_SESSION_3_START_DATE
            // maxEndTime = SLEEP_SESSION_1_END_DATE
            // Total time = 12 Oct 20:00 - 13 Oct 20:00 = 24h
            val expectedSleepAggregation = formattedAggregation("24h")
            loadDataAggregationsUseCase.updateAggregationResponses(listOf(expectedSleepAggregation))

            val result = loadMostRecentAggregationsUseCase.invoke(HealthDataCategory.SLEEP)
            assertThat(result is UseCaseResults.Success).isTrue()
            assertThat((result as UseCaseResults.Success).data)
                .isEqualTo(
                    listOf(
                        AggregationCardInfo(
                            HealthPermissionType.SLEEP,
                            expectedSleepAggregation,
                            SLEEP_SESSION_3_START_DATE.atStartOfDay(),
                            SLEEP_SESSION_1_END_DATE.atStartOfDay())))
        }

    // Case 3 - The sessions from lastDayWithData cross midnight into the next day
    @Test
    fun loadMostRecentAggregations_forSleep_atLeastOneSessionFinishesTomorrow() = runTest {
        Mockito.doAnswer(prepareEmptyAnswer())
            .`when`(healthConnectManager)
            .queryActivityDates(eq(stepsRecordTypes), any(), any())

        Mockito.doAnswer(prepareEmptyAnswer())
            .`when`(healthConnectManager)
            .queryActivityDates(eq(distanceRecordTypes), any(), any())

        Mockito.doAnswer(prepareEmptyAnswer())
            .`when`(healthConnectManager)
            .queryActivityDates(eq(caloriesRecordTypes), any(), any())

        Mockito.doAnswer(prepareSleepAnswer())
            .`when`(healthConnectManager)
            .queryActivityDates(eq(sleepRecordTypes), any(), any())

        // lastDayWithSleepData = 2023-02-13

        // 2h
        val SLEEP_SESSION_1_START_DATE = Instant.parse("2023-02-13T18:00:00.00Z")
        val SLEEP_SESSION_1_END_DATE = Instant.parse("2023-02-13T20:00:00.00Z")

        // 10h 15m
        val SLEEP_SESSION_2_START_DATE = Instant.parse("2023-02-13T22:30:00.00Z")
        val SLEEP_SESSION_2_END_DATE = Instant.parse("2023-02-14T08:45:00.00Z")

        // 2h 20m
        val SLEEP_SESSION_3_START_DATE = Instant.parse("2023-02-13T16:00:00.00Z")
        val SLEEP_SESSION_3_END_DATE = Instant.parse("2023-02-13T18:20:00.00Z")

        // 10h
        val SLEEP_SESSION_4_START_DATE = Instant.parse("2023-02-13T22:00:00.00Z")
        val SLEEP_SESSION_4_END_DATE = Instant.parse("2023-02-14T08:00:00.00Z")

        loadSleepDataUseCase.updateSleepData(
            SLEEP_SESSION_1_START_DATE.toLocalDate(),
            getSleepSessionRecords(
                listOf(
                    Pair(SLEEP_SESSION_1_START_DATE, SLEEP_SESSION_1_END_DATE),
                    Pair(SLEEP_SESSION_2_START_DATE, SLEEP_SESSION_2_END_DATE))))

        loadSleepDataUseCase.updateSleepData(
            SLEEP_SESSION_3_START_DATE.toLocalDate(),
            getSleepSessionRecords(
                listOf(
                    Pair(SLEEP_SESSION_3_START_DATE, SLEEP_SESSION_3_END_DATE),
                    Pair(SLEEP_SESSION_4_START_DATE, SLEEP_SESSION_4_END_DATE))))

        // minStartTime = SLEEP_SESSION_4_START_DATE
        // maxEndTime = SLEEP_SESSION_2_END_DATE
        // Total time = 13 Feb 22:00 - 14 Feb 08:45 = 10h 45m
        val expectedSleepAggregation = formattedAggregation("10h 45m")
        loadDataAggregationsUseCase.updateAggregationResponses(listOf(expectedSleepAggregation))

        val result = loadMostRecentAggregationsUseCase.invoke(HealthDataCategory.SLEEP)
        assertThat(result is UseCaseResults.Success).isTrue()
        assertThat((result as UseCaseResults.Success).data)
            .isEqualTo(
                listOf(
                    AggregationCardInfo(
                        HealthPermissionType.SLEEP,
                        expectedSleepAggregation,
                        SLEEP_SESSION_4_START_DATE.atStartOfDay(),
                        SLEEP_SESSION_2_END_DATE.atStartOfDay())))
    }

    // Case 3 - The sessions from lastDayWithData cross midnight into the next day
    // Edge case - additional sleep session starts on unknown date
    // and finishes within range
    @Test
    fun loadMostRecentAggregations_forSleep_atLeastOneSessionFinishesTomorrow_withSessionUnknownStart() =
        runTest {
            Mockito.doAnswer(prepareEmptyAnswer())
                .`when`(healthConnectManager)
                .queryActivityDates(eq(stepsRecordTypes), any(), any())

            Mockito.doAnswer(prepareEmptyAnswer())
                .`when`(healthConnectManager)
                .queryActivityDates(eq(distanceRecordTypes), any(), any())

            Mockito.doAnswer(prepareEmptyAnswer())
                .`when`(healthConnectManager)
                .queryActivityDates(eq(caloriesRecordTypes), any(), any())

            Mockito.doAnswer(prepareSleepAnswer())
                .`when`(healthConnectManager)
                .queryActivityDates(eq(sleepRecordTypes), any(), any())

            // lastDayWithSleepData = 2023-02-13

            // 2h
            val SLEEP_SESSION_1_START_DATE = Instant.parse("2023-02-13T18:00:00.00Z")
            val SLEEP_SESSION_1_END_DATE = Instant.parse("2023-02-13T20:00:00.00Z")

            // 10h 15m
            val SLEEP_SESSION_2_START_DATE = Instant.parse("2023-02-13T22:30:00.00Z")
            val SLEEP_SESSION_2_END_DATE = Instant.parse("2023-02-14T08:45:00.00Z")

            // 2h 20m
            val SLEEP_SESSION_3_START_DATE = Instant.parse("2023-02-13T16:00:00.00Z")
            val SLEEP_SESSION_3_END_DATE = Instant.parse("2023-02-13T18:20:00.00Z")

            // 10h
            val SLEEP_SESSION_4_START_DATE = Instant.parse("2023-02-13T22:00:00.00Z")
            val SLEEP_SESSION_4_END_DATE = Instant.parse("2023-02-14T08:00:00.00Z")

            // 2d 11h - should not have an effect on the aggregation
            val SLEEP_SESSION_5_START_DATE = Instant.parse("2023-02-11T22:00:00.00Z")
            val SLEEP_SESSION_5_END_DATE = Instant.parse("2023-02-14T09:00:00.00Z")

            loadSleepDataUseCase.updateSleepData(
                SLEEP_SESSION_1_START_DATE.toLocalDate(),
                getSleepSessionRecords(
                    listOf(
                        Pair(SLEEP_SESSION_1_START_DATE, SLEEP_SESSION_1_END_DATE),
                        Pair(SLEEP_SESSION_2_START_DATE, SLEEP_SESSION_2_END_DATE),
                        Pair(SLEEP_SESSION_3_START_DATE, SLEEP_SESSION_3_END_DATE),
                        Pair(SLEEP_SESSION_4_START_DATE, SLEEP_SESSION_4_END_DATE))))

            loadSleepDataUseCase.updateSleepData(
                SLEEP_SESSION_5_START_DATE.toLocalDate(),
                getSleepSessionRecords(
                    listOf(Pair(SLEEP_SESSION_5_START_DATE, SLEEP_SESSION_5_END_DATE))))

            // minStartTime = SLEEP_SESSION_4_START_DATE
            // maxEndTime = SLEEP_SESSION_2_END_DATE
            // Total time = 13 Feb 22:00 - 14 Feb 08:45 = 10h 45m
            val expectedSleepAggregation = formattedAggregation("10h 45m")
            loadDataAggregationsUseCase.updateAggregationResponses(listOf(expectedSleepAggregation))

            val result = loadMostRecentAggregationsUseCase.invoke(HealthDataCategory.SLEEP)
            assertThat(result is UseCaseResults.Success).isTrue()
            assertThat((result as UseCaseResults.Success).data)
                .isEqualTo(
                    listOf(
                        AggregationCardInfo(
                            HealthPermissionType.SLEEP,
                            expectedSleepAggregation,
                            SLEEP_SESSION_4_START_DATE.atStartOfDay(),
                            SLEEP_SESSION_2_END_DATE.atStartOfDay())))
        }

    // Case 3 - The sessions from lastDayWithData cross midnight into the next day
    // Edge case - additional sleep session starts on last day with data
    // and finishes in the future
    @Test
    fun loadMostRecentAggregations_forSleep_atLeastOneSessionFinishesTomorrow_withSessionUnknownEnd() =
        runTest {
            Mockito.doAnswer(prepareEmptyAnswer())
                .`when`(healthConnectManager)
                .queryActivityDates(eq(stepsRecordTypes), any(), any())

            Mockito.doAnswer(prepareEmptyAnswer())
                .`when`(healthConnectManager)
                .queryActivityDates(eq(distanceRecordTypes), any(), any())

            Mockito.doAnswer(prepareEmptyAnswer())
                .`when`(healthConnectManager)
                .queryActivityDates(eq(caloriesRecordTypes), any(), any())

            Mockito.doAnswer(prepareSleepAnswer())
                .`when`(healthConnectManager)
                .queryActivityDates(eq(sleepRecordTypes), any(), any())

            // lastDayWithSleepData = 2023-02-13

            // 2h
            val SLEEP_SESSION_1_START_DATE = Instant.parse("2023-02-13T18:00:00.00Z")
            val SLEEP_SESSION_1_END_DATE = Instant.parse("2023-02-13T20:00:00.00Z")

            // 10h 15m
            val SLEEP_SESSION_2_START_DATE = Instant.parse("2023-02-13T22:30:00.00Z")
            val SLEEP_SESSION_2_END_DATE = Instant.parse("2023-02-14T08:45:00.00Z")

            // 2h 20m
            val SLEEP_SESSION_3_START_DATE = Instant.parse("2023-02-13T16:00:00.00Z")
            val SLEEP_SESSION_3_END_DATE = Instant.parse("2023-02-13T18:20:00.00Z")

            // 10h
            val SLEEP_SESSION_4_START_DATE = Instant.parse("2023-02-13T22:00:00.00Z")
            val SLEEP_SESSION_4_END_DATE = Instant.parse("2023-02-14T08:00:00.00Z")

            // 3d 11h - determines maxEndTime
            val SLEEP_SESSION_5_START_DATE = Instant.parse("2023-02-13T22:00:00.00Z")
            val SLEEP_SESSION_5_END_DATE = Instant.parse("2023-02-16T09:00:00.00Z")

            val maxEndTime = Instant.parse("2023-02-15T00:00:00.00Z")

            loadSleepDataUseCase.updateSleepData(
                SLEEP_SESSION_1_START_DATE.toLocalDate(),
                getSleepSessionRecords(
                    listOf(
                        Pair(SLEEP_SESSION_1_START_DATE, SLEEP_SESSION_1_END_DATE),
                        Pair(SLEEP_SESSION_2_START_DATE, SLEEP_SESSION_2_END_DATE),
                        Pair(SLEEP_SESSION_3_START_DATE, SLEEP_SESSION_3_END_DATE),
                        Pair(SLEEP_SESSION_4_START_DATE, SLEEP_SESSION_4_END_DATE),
                        Pair(SLEEP_SESSION_5_START_DATE, SLEEP_SESSION_5_END_DATE))))

            // minStartTime = SLEEP_SESSION_4_START_DATE
            // maxEndTime = 15 Feb 00:00
            // Total time = 13 Feb 22:00 - 15 Feb 00:00 = 26h
            val expectedSleepAggregation = formattedAggregation("26h")
            loadDataAggregationsUseCase.updateAggregationResponses(listOf(expectedSleepAggregation))

            val result = loadMostRecentAggregationsUseCase.invoke(HealthDataCategory.SLEEP)
            assertThat(result is UseCaseResults.Success).isTrue()
            assertThat((result as UseCaseResults.Success).data)
                .isEqualTo(
                    listOf(
                        AggregationCardInfo(
                            HealthPermissionType.SLEEP,
                            expectedSleepAggregation,
                            SLEEP_SESSION_4_START_DATE.atStartOfDay(),
                            maxEndTime.atStartOfDay())))
        }

    // Case 3 - The sessions from lastDayWithData cross midnight into the next day
    // Edge case - additional sleep session starts and ends on unknown date
    @Test
    fun loadMostRecentAggregations_forSleep_atLeastOneSessionFinishesTomorrow_withSessionUnknownStartAndEnd() =
        runTest {
            Mockito.doAnswer(prepareEmptyAnswer())
                .`when`(healthConnectManager)
                .queryActivityDates(eq(stepsRecordTypes), any(), any())

            Mockito.doAnswer(prepareEmptyAnswer())
                .`when`(healthConnectManager)
                .queryActivityDates(eq(distanceRecordTypes), any(), any())

            Mockito.doAnswer(prepareEmptyAnswer())
                .`when`(healthConnectManager)
                .queryActivityDates(eq(caloriesRecordTypes), any(), any())

            Mockito.doAnswer(prepareSleepAnswer())
                .`when`(healthConnectManager)
                .queryActivityDates(eq(sleepRecordTypes), any(), any())

            // lastDayWithSleepData = 2023-02-13

            // 2h
            val SLEEP_SESSION_1_START_DATE = Instant.parse("2023-02-13T18:00:00.00Z")
            val SLEEP_SESSION_1_END_DATE = Instant.parse("2023-02-13T20:00:00.00Z")

            // 10h 15m
            val SLEEP_SESSION_2_START_DATE = Instant.parse("2023-02-13T22:30:00.00Z")
            val SLEEP_SESSION_2_END_DATE = Instant.parse("2023-02-14T08:45:00.00Z")

            // 2h 20m
            val SLEEP_SESSION_3_START_DATE = Instant.parse("2023-02-13T16:00:00.00Z")
            val SLEEP_SESSION_3_END_DATE = Instant.parse("2023-02-13T18:20:00.00Z")

            // 10h
            val SLEEP_SESSION_4_START_DATE = Instant.parse("2023-02-13T22:00:00.00Z")
            val SLEEP_SESSION_4_END_DATE = Instant.parse("2023-02-14T08:00:00.00Z")

            // 5d 11h - Should not affect aggregation
            val SLEEP_SESSION_5_START_DATE = Instant.parse("2023-02-11T22:00:00.00Z")
            val SLEEP_SESSION_5_END_DATE = Instant.parse("2023-02-16T09:00:00.00Z")

            loadSleepDataUseCase.updateSleepData(
                SLEEP_SESSION_1_START_DATE.toLocalDate(),
                getSleepSessionRecords(
                    listOf(
                        Pair(SLEEP_SESSION_1_START_DATE, SLEEP_SESSION_1_END_DATE),
                        Pair(SLEEP_SESSION_2_START_DATE, SLEEP_SESSION_2_END_DATE),
                        Pair(SLEEP_SESSION_3_START_DATE, SLEEP_SESSION_3_END_DATE),
                        Pair(SLEEP_SESSION_4_START_DATE, SLEEP_SESSION_4_END_DATE))))

            loadSleepDataUseCase.updateSleepData(
                SLEEP_SESSION_5_START_DATE.toLocalDate(),
                getSleepSessionRecords(
                    listOf(Pair(SLEEP_SESSION_5_START_DATE, SLEEP_SESSION_5_END_DATE))))

            // minStartTime = SLEEP_SESSION_4_START_DATE
            // maxEndTime = SLEEP_SESSION_2_END_DATE
            // Total time = 13 Feb 22:00 - 14 Feb 08:45 = 10h 45m
            val expectedSleepAggregation = formattedAggregation("10h 45m")
            loadDataAggregationsUseCase.updateAggregationResponses(listOf(expectedSleepAggregation))

            val result = loadMostRecentAggregationsUseCase.invoke(HealthDataCategory.SLEEP)
            assertThat(result is UseCaseResults.Success).isTrue()
            assertThat((result as UseCaseResults.Success).data)
                .isEqualTo(
                    listOf(
                        AggregationCardInfo(
                            HealthPermissionType.SLEEP,
                            expectedSleepAggregation,
                            SLEEP_SESSION_4_START_DATE.atStartOfDay(),
                            SLEEP_SESSION_2_END_DATE.atStartOfDay())))
        }

    @Test
    fun loadMostRecentAggregations_forSleep_returnsMostRecent_sleepSessions() = runTest {
        Mockito.doAnswer(prepareEmptyAnswer())
            .`when`(healthConnectManager)
            .queryActivityDates(eq(stepsRecordTypes), any(), any())

        Mockito.doAnswer(prepareEmptyAnswer())
            .`when`(healthConnectManager)
            .queryActivityDates(eq(distanceRecordTypes), any(), any())

        Mockito.doAnswer(prepareEmptyAnswer())
            .`when`(healthConnectManager)
            .queryActivityDates(eq(caloriesRecordTypes), any(), any())

        Mockito.doAnswer(prepareSleepAnswer())
            .`when`(healthConnectManager)
            .queryActivityDates(eq(sleepRecordTypes), any(), any())

        // 2h
        val SLEEP_SESSION_1_START_DATE = Instant.parse("2023-02-13T16:00:00.00Z")
        val SLEEP_SESSION_1_END_DATE = Instant.parse("2023-02-13T18:00:00.00Z")

        // 10h 45m
        val SLEEP_SESSION_2_START_DATE = Instant.parse("2023-02-13T22:30:00.00Z")
        val SLEEP_SESSION_2_END_DATE = Instant.parse("2023-02-14T08:15:00.00Z")

        // 9h 20m
        val SLEEP_SESSION_3_START_DATE = Instant.parse("2023-02-13T23:00:00.00Z")
        val SLEEP_SESSION_3_END_DATE = Instant.parse("2023-02-14T08:20:00.00Z")

        loadDataAggregationsUseCase.updateAggregationResponses(listOf(sleepAggregation))
        loadSleepDataUseCase.updateSleepData(
            SLEEP_SESSION_1_START_DATE.toLocalDate(),
            getSleepSessionRecords(
                listOf(
                    Pair(SLEEP_SESSION_1_START_DATE, SLEEP_SESSION_1_END_DATE),
                    Pair(SLEEP_SESSION_2_START_DATE, SLEEP_SESSION_2_END_DATE),
                    Pair(SLEEP_SESSION_3_START_DATE, SLEEP_SESSION_3_END_DATE))))

        val result = loadMostRecentAggregationsUseCase.invoke(HealthDataCategory.SLEEP)
        assertThat(result is UseCaseResults.Success).isTrue()
        assertThat((result as UseCaseResults.Success).data)
            .isEqualTo(
                listOf(
                    AggregationCardInfo(
                        HealthPermissionType.SLEEP,
                        sleepAggregation,
                        SLEEP_SESSION_1_START_DATE.atStartOfDay(),
                        SLEEP_SESSION_3_END_DATE.atStartOfDay())))
    }

    @Test
    fun loadMostRecentAggregations_ifQueryActivityDatesFails_returnsFailure() = runTest {
        Mockito.doAnswer(prepareStepsAnswer())
            .`when`(healthConnectManager)
            .queryActivityDates(eq(stepsRecordTypes), any(), any())

        Mockito.doAnswer(prepareFailedDistanceAnswer())
            .`when`(healthConnectManager)
            .queryActivityDates(eq(distanceRecordTypes), any(), any())

        Mockito.doAnswer(prepareCaloriesAnswer())
            .`when`(healthConnectManager)
            .queryActivityDates(eq(caloriesRecordTypes), any(), any())

        Mockito.doAnswer(prepareEmptyAnswer())
            .`when`(healthConnectManager)
            .queryActivityDates(eq(sleepRecordTypes), any(), any())

        loadDataAggregationsUseCase.updateAggregationResponses(
            listOf(stepsAggregation, distanceAggregation, caloriesAggregation))

        val result = loadMostRecentAggregationsUseCase.invoke(HealthDataCategory.ACTIVITY)
        assertThat(result is UseCaseResults.Failed).isTrue()
    }

    @Test
    fun loadMostRecentAggregations_ifAggregationRequestFails_returnsEmptyList() = runTest {
        Mockito.doAnswer(prepareStepsAnswer())
            .`when`(healthConnectManager)
            .queryActivityDates(eq(stepsRecordTypes), any(), any())

        Mockito.doAnswer(prepareDistanceAnswer())
            .`when`(healthConnectManager)
            .queryActivityDates(eq(distanceRecordTypes), any(), any())

        Mockito.doAnswer(prepareCaloriesAnswer())
            .`when`(healthConnectManager)
            .queryActivityDates(eq(caloriesRecordTypes), any(), any())

        Mockito.doAnswer(prepareEmptyAnswer())
            .`when`(healthConnectManager)
            .queryActivityDates(eq(sleepRecordTypes), any(), any())

        loadDataAggregationsUseCase.updateErrorResponse()

        val result = loadMostRecentAggregationsUseCase.invoke(HealthDataCategory.ACTIVITY)
        assertThat(result is UseCaseResults.Success).isTrue()
        assertThat((result as UseCaseResults.Success).data).isEmpty()
    }

    @Test
    fun loadMostRecentAggregations_ifNoData_returnsEmptyList() = runTest {
        Mockito.doAnswer(prepareEmptyAnswer())
            .`when`(healthConnectManager)
            .queryActivityDates(eq(stepsRecordTypes), any(), any())

        Mockito.doAnswer(prepareEmptyAnswer())
            .`when`(healthConnectManager)
            .queryActivityDates(eq(distanceRecordTypes), any(), any())

        Mockito.doAnswer(prepareEmptyAnswer())
            .`when`(healthConnectManager)
            .queryActivityDates(eq(caloriesRecordTypes), any(), any())

        Mockito.doAnswer(prepareEmptyAnswer())
            .`when`(healthConnectManager)
            .queryActivityDates(eq(sleepRecordTypes), any(), any())

        val result = loadMostRecentAggregationsUseCase.invoke(HealthDataCategory.ACTIVITY)
        assertThat(result is UseCaseResults.Success).isTrue()
        assertThat((result as UseCaseResults.Success).data).isEmpty()
    }

    private fun prepareStepsAnswer(): (InvocationOnMock) -> Nothing? {
        val answer = { args: InvocationOnMock ->
            val receiver = args.arguments[2] as OutcomeReceiver<List<LocalDate>, *>
            receiver.onResult(getStepsDates())
            null
        }
        return answer
    }

    private fun prepareDistanceAnswer(): (InvocationOnMock) -> Nothing? {
        val answer = { args: InvocationOnMock ->
            val receiver = args.arguments[2] as OutcomeReceiver<List<LocalDate>, *>
            receiver.onResult(getDistanceDates())
            null
        }
        return answer
    }

    private fun prepareFailedDistanceAnswer(): (InvocationOnMock) -> Nothing? {
        val answer = { args: InvocationOnMock ->
            val receiver =
                args.arguments[2] as OutcomeReceiver<List<LocalDate>, HealthConnectException>
            receiver.onError(HealthConnectException(HealthConnectException.ERROR_INTERNAL))
            null
        }
        return answer
    }

    private fun prepareCaloriesAnswer(): (InvocationOnMock) -> Nothing? {
        val answer = { args: InvocationOnMock ->
            val receiver = args.arguments[2] as OutcomeReceiver<List<LocalDate>, *>
            receiver.onResult(getCaloriesDates())
            null
        }
        return answer
    }

    private fun prepareSleepAnswer(): (InvocationOnMock) -> Nothing? {
        val answer = { args: InvocationOnMock ->
            val receiver = args.arguments[2] as OutcomeReceiver<List<LocalDate>, *>
            receiver.onResult(getSleepDates())
            null
        }
        return answer
    }

    private fun prepareEmptyAnswer(): (InvocationOnMock) -> Nothing? {
        val answer = { args: InvocationOnMock ->
            val receiver = args.arguments[2] as OutcomeReceiver<List<LocalDate>, *>
            receiver.onResult(listOf())
            null
        }
        return answer
    }

    private fun getStepsDates(): List<LocalDate> =
        listOf(STEPS_DATE_1.toLocalDate(), STEPS_DATE_2.toLocalDate(), STEPS_DATE_3.toLocalDate())

    private fun getDistanceDates(): List<LocalDate> =
        listOf(
            DISTANCE_DATE_1.toLocalDate(),
            DISTANCE_DATE_2.toLocalDate(),
            DISTANCE_DATE_3.toLocalDate())

    private fun getCaloriesDates(): List<LocalDate> =
        listOf(
            CALORIES_DATE_1.toLocalDate(),
            CALORIES_DATE_2.toLocalDate(),
            CALORIES_DATE_3.toLocalDate())

    private fun getSleepDates(): List<LocalDate> =
        listOf(SLEEP_DATE_1.toLocalDate(), SLEEP_DATE_2.toLocalDate(), SLEEP_DATE_3.toLocalDate())

    private fun getSleepSessionRecords(inputDates: List<Pair<Instant, Instant>>): List<Record> {
        val result = arrayListOf<Record>()
        inputDates.forEach { (startTime, endTime) ->
            result.add(SleepSessionRecord.Builder(getMetaData(), startTime, endTime).build())
        }

        return result
    }
}
