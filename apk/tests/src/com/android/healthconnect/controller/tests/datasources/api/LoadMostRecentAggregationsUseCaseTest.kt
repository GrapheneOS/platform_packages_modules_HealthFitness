package com.android.healthconnect.controller.tests.datasources.api

import android.content.Context
import android.health.connect.HealthConnectException
import android.health.connect.HealthConnectManager
import android.os.OutcomeReceiver
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.data.entries.FormattedEntry
import com.android.healthconnect.controller.datasources.AggregationCardInfo
import com.android.healthconnect.controller.datasources.api.LoadMostRecentAggregationsUseCase
import com.android.healthconnect.controller.permissions.data.HealthPermissionType
import com.android.healthconnect.controller.shared.HealthPermissionToDatatypeMapper
import com.android.healthconnect.controller.shared.usecase.UseCaseResults
import com.android.healthconnect.controller.tests.utils.di.FakeLoadDataAggregationsUseCase
import com.android.healthconnect.controller.tests.utils.setLocale
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoAnnotations
import org.mockito.invocation.InvocationOnMock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

@HiltAndroidTest
class LoadMostRecentAggregationsUseCaseTest {

    companion object {
        private fun formattedAggregation(aggregation: String) =
            FormattedEntry.FormattedAggregation(
                aggregation = aggregation,
                aggregationA11y = aggregation,
                contributingApps = "Test App")
    }

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    private lateinit var context: Context
    private lateinit var loadMostRecentAggregationsUseCase: LoadMostRecentAggregationsUseCase

    private val healthConnectManager: HealthConnectManager =
        Mockito.mock(HealthConnectManager::class.java)
    private val loadDataAggregationsUseCase = FakeLoadDataAggregationsUseCase()

    private val STEPS_DATE_1 = Instant.parse("2022-10-24T18:40:13.00Z")
    private val STEPS_DATE_2 = Instant.parse("2022-10-26T13:23:19.00Z")
    private val STEPS_DATE_3 = Instant.parse("2023-04-09T19:45:12.00Z")

    private val DISTANCE_DATE_1 = Instant.parse("2022-05-12T14:15:22.00Z")
    private val DISTANCE_DATE_2 = Instant.parse("2022-11-03T07:20:18.00Z")
    private val DISTANCE_DATE_3 = Instant.parse("2023-02-08T16:42:29.00Z")

    private val CALORIES_DATE_1 = Instant.parse("2022-07-26T11:33:10.00Z")
    private val CALORIES_DATE_2 = Instant.parse("2022-09-30T12:55:44.00Z")
    private val CALORIES_DATE_3 = Instant.parse("2023-04-19T20:25:37.00Z")

    private val stepsAggregation = formattedAggregation("100 steps")
    private val distanceAggregation = formattedAggregation("1.5 km")
    private val caloriesAggregation = formattedAggregation("1590 kcal")

    private val stepsRecordTypes =
        HealthPermissionToDatatypeMapper.getDataTypes(HealthPermissionType.STEPS)
    private val distanceRecordTypes =
        HealthPermissionToDatatypeMapper.getDataTypes(HealthPermissionType.DISTANCE)
    private val caloriesRecordTypes =
        HealthPermissionToDatatypeMapper.getDataTypes(HealthPermissionType.TOTAL_CALORIES_BURNED)

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        context = InstrumentationRegistry.getInstrumentation().context
        context.setLocale(Locale.US)
        hiltRule.inject()
        context = InstrumentationRegistry.getInstrumentation().context
        loadMostRecentAggregationsUseCase =
            LoadMostRecentAggregationsUseCase(
                healthConnectManager,
                loadDataAggregationsUseCase,
                Dispatchers.Main)
    }

    @After
    fun tearDown() {
        loadDataAggregationsUseCase.reset()
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

        loadDataAggregationsUseCase.updateAggregationResponses(
            listOf(stepsAggregation, distanceAggregation, caloriesAggregation))

        val result = loadMostRecentAggregationsUseCase.invoke()
        assertThat(result is UseCaseResults.Success).isTrue()
        assertThat((result as UseCaseResults.Success).data).isEqualTo(listOf(
            AggregationCardInfo(
                HealthPermissionType.STEPS,
                stepsAggregation,
                instantAtStartOfDay(STEPS_DATE_3)
            ),
            AggregationCardInfo(
                HealthPermissionType.DISTANCE,
                distanceAggregation,
                instantAtStartOfDay(DISTANCE_DATE_3)
            ),
            AggregationCardInfo(
                HealthPermissionType.TOTAL_CALORIES_BURNED,
                caloriesAggregation,
                instantAtStartOfDay(CALORIES_DATE_3)
            ),

        ))
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

        loadDataAggregationsUseCase.updateAggregationResponses(
            listOf(stepsAggregation, distanceAggregation, caloriesAggregation))

        val result = loadMostRecentAggregationsUseCase.invoke()
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

        loadDataAggregationsUseCase.updateErrorResponse()

        val result = loadMostRecentAggregationsUseCase.invoke()
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

        val result = loadMostRecentAggregationsUseCase.invoke()
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
            val receiver = args.arguments[2] as OutcomeReceiver<List<LocalDate>, HealthConnectException>
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

    private fun prepareEmptyAnswer(): (InvocationOnMock) -> Nothing? {
        val answer = { args: InvocationOnMock ->
            val receiver = args.arguments[2] as OutcomeReceiver<List<LocalDate>, *>
            receiver.onResult(listOf())
            null
        }
        return answer
    }

    private fun getStepsDates() : List<LocalDate> =
        listOf(
            STEPS_DATE_1.atZone(ZoneId.systemDefault()).toLocalDate(),
            STEPS_DATE_2.atZone(ZoneId.systemDefault()).toLocalDate(),
            STEPS_DATE_3.atZone(ZoneId.systemDefault()).toLocalDate())

    private fun getDistanceDates() : List<LocalDate> =
        listOf(
            DISTANCE_DATE_1.atZone(ZoneId.systemDefault()).toLocalDate(),
            DISTANCE_DATE_2.atZone(ZoneId.systemDefault()).toLocalDate(),
            DISTANCE_DATE_3.atZone(ZoneId.systemDefault()).toLocalDate())

    private fun getCaloriesDates() : List<LocalDate> =
        listOf(
            CALORIES_DATE_1.atZone(ZoneId.systemDefault()).toLocalDate(),
            CALORIES_DATE_2.atZone(ZoneId.systemDefault()).toLocalDate(),
            CALORIES_DATE_3.atZone(ZoneId.systemDefault()).toLocalDate())

    private fun instantAtStartOfDay(instant: Instant): Instant =
        instant.atZone(ZoneId.systemDefault()).toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant()
}