/*
 * Copyright (C) 2026 The Android Open Source Project
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
package com.android.healthconnect.controller.tests.data.entries.api

import android.content.Context
import android.health.connect.GetMedicalDataSourcesRequest
import android.health.connect.HealthConnectException
import android.health.connect.HealthConnectManager
import android.health.connect.ReadMedicalResourcesInitialRequest
import android.health.connect.ReadMedicalResourcesResponse
import android.health.connect.ReadRecordsRequest
import android.health.connect.ReadRecordsRequestUsingFilters
import android.health.connect.ReadRecordsResponse
import android.health.connect.TimeInstantRangeFilter
import android.health.connect.datatypes.ActiveCaloriesBurnedRecord
import android.health.connect.datatypes.BodyTemperatureMeasurementLocation
import android.health.connect.datatypes.BodyTemperatureRecord
import android.health.connect.datatypes.BodyWaterMassRecord
import android.health.connect.datatypes.DataOrigin
import android.health.connect.datatypes.DistanceRecord
import android.health.connect.datatypes.FloorsClimbedRecord
import android.health.connect.datatypes.HydrationRecord
import android.health.connect.datatypes.IntermenstrualBleedingRecord
import android.health.connect.datatypes.MedicalDataSource
import android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES
import android.health.connect.datatypes.MenstruationFlowRecord
import android.health.connect.datatypes.OxygenSaturationRecord
import android.health.connect.datatypes.Record
import android.health.connect.datatypes.SleepSessionRecord
import android.health.connect.datatypes.StepsCadenceRecord
import android.health.connect.datatypes.StepsRecord
import android.health.connect.datatypes.WeightRecord
import android.health.connect.datatypes.units.Temperature
import android.os.OutcomeReceiver
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.data.entries.FormattedEntry
import com.android.healthconnect.controller.data.entries.api.LoadDataEntriesInput
import com.android.healthconnect.controller.data.entries.api.LoadEntriesHelper
import com.android.healthconnect.controller.data.entries.api.LoadLatestEntryDateInput
import com.android.healthconnect.controller.data.entries.api.LoadMedicalEntriesInput
import com.android.healthconnect.controller.data.entries.datenavigation.DateNavigationPeriod
import com.android.healthconnect.controller.data.formatters.MenstruationPeriodFormatter
import com.android.healthconnect.controller.data.formatters.shared.HealthDataEntryFormatter
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.permissions.data.MedicalPermissionType
import com.android.healthconnect.controller.service.HealthManagerModule
import com.android.healthconnect.controller.shared.Constants.DEVICE_DATA_PROVIDER_PACKAGE
import com.android.healthconnect.controller.shared.app.AppInfoReader
import com.android.healthconnect.controller.shared.app.MedicalDataSourceReader
import com.android.healthconnect.controller.tests.devices.api.FakeGetCurrentDeviceIdUseCase
import com.android.healthconnect.controller.tests.utils.BODYTEMPERATURE_MONTH
import com.android.healthconnect.controller.tests.utils.BODYWATERMASS_WEEK
import com.android.healthconnect.controller.tests.utils.DISTANCE_STARTDATE_1500
import com.android.healthconnect.controller.tests.utils.FakeUseCaseRule
import com.android.healthconnect.controller.tests.utils.HYDRATION_MONTH
import com.android.healthconnect.controller.tests.utils.HYDRATION_MONTH2
import com.android.healthconnect.controller.tests.utils.HYDRATION_MONTH3
import com.android.healthconnect.controller.tests.utils.INSTANT_DAY
import com.android.healthconnect.controller.tests.utils.INSTANT_MONTH3
import com.android.healthconnect.controller.tests.utils.INSTANT_WEEK
import com.android.healthconnect.controller.tests.utils.INTERMENSTRUAL_BLEEDING_DAY
import com.android.healthconnect.controller.tests.utils.MENSTRUATION_PERIOD_5D
import com.android.healthconnect.controller.tests.utils.NOW
import com.android.healthconnect.controller.tests.utils.OXYGENSATURATION_DAY
import com.android.healthconnect.controller.tests.utils.OXYGENSATURATION_DAY2
import com.android.healthconnect.controller.tests.utils.SLEEP_DAY_0H20
import com.android.healthconnect.controller.tests.utils.SLEEP_DAY_1H45
import com.android.healthconnect.controller.tests.utils.SLEEP_DAY_9H15
import com.android.healthconnect.controller.tests.utils.SLEEP_MONTH_81H15
import com.android.healthconnect.controller.tests.utils.SLEEP_WEEK_33H15
import com.android.healthconnect.controller.tests.utils.SLEEP_WEEK_9H15
import com.android.healthconnect.controller.tests.utils.START_TIME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.TEST_DATASOURCE_ID
import com.android.healthconnect.controller.tests.utils.TEST_MEDICAL_DATA_SOURCE
import com.android.healthconnect.controller.tests.utils.TEST_MEDICAL_DATA_SOURCE_2
import com.android.healthconnect.controller.tests.utils.TEST_MEDICAL_RESOURCE_IMMUNIZATION
import com.android.healthconnect.controller.tests.utils.TestTimeSource
import com.android.healthconnect.controller.tests.utils.WEIGHT_DAY_100
import com.android.healthconnect.controller.tests.utils.WEIGHT_MONTH_100
import com.android.healthconnect.controller.tests.utils.WEIGHT_WEEK_100
import com.android.healthconnect.controller.tests.utils.createFakeAppInfoReader
import com.android.healthconnect.controller.tests.utils.doReturnResult
import com.android.healthconnect.controller.tests.utils.getMixedRecordsAcrossThreeDays
import com.android.healthconnect.controller.tests.utils.getMixedRecordsAcrossTwoDays
import com.android.healthconnect.controller.tests.utils.getStepsCadenceRecord
import com.android.healthconnect.controller.tests.utils.getStepsRecord
import com.android.healthconnect.controller.tests.utils.setLocale
import com.android.healthconnect.controller.tests.utils.verifyBodyWaterMassListsEqual
import com.android.healthconnect.controller.tests.utils.verifyHydrationListsEqual
import com.android.healthconnect.controller.tests.utils.verifyOxygenSaturationListsEqual
import com.android.healthconnect.controller.tests.utils.verifySleepSessionListsEqual
import com.android.healthconnect.controller.utils.LocalDateTimeFormatter
import com.android.healthconnect.controller.utils.atStartOfDay
import com.android.healthconnect.controller.utils.toInstant
import com.android.healthfitness.flags.Flags
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@UninstallModules(HealthManagerModule::class)
@RunWith(AndroidJUnit4::class)
class LoadEntriesHelperUseCaseTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)
    @get:Rule val checkFlagsRule = SetFlagsRule()
    @get:Rule val fakeUseCaseRule = FakeUseCaseRule()
    @BindValue @JvmField val timeSource = TestTimeSource

    private val defaultStartTime: Instant = START_TIME

    @BindValue lateinit var appInfoReader: AppInfoReader
    @BindValue val healthConnectManager: HealthConnectManager = mock()
    @Inject lateinit var healthDataEntryFormatter: HealthDataEntryFormatter
    @Inject lateinit var menstruationPeriodFormatter: MenstruationPeriodFormatter
    @Inject lateinit var dataSourceReader: MedicalDataSourceReader

    private lateinit var context: Context
    private lateinit var loadEntriesHelper: LoadEntriesHelper
    private val fakeGetCurrentDeviceIdUseCase =
        fakeUseCaseRule.watch(FakeGetCurrentDeviceIdUseCase())

    @Before
    fun setup() = runTest {
        appInfoReader = createFakeAppInfoReader()
        hiltRule.inject()
        context = InstrumentationRegistry.getInstrumentation().context
        context.setLocale(Locale.US)
        loadEntriesHelper =
            LoadEntriesHelper(
                context,
                Dispatchers.Main,
                healthDataEntryFormatter,
                menstruationPeriodFormatter,
                healthConnectManager,
                dataSourceReader,
                fakeGetCurrentDeviceIdUseCase,
                timeSource,
            )
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("UTC")))
    }

    @After
    fun teardown() {
        timeSource.reset()
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    fun readRecords_withCurrentDevicePackage_addsAndroidPackageToFilter() = runTest {
        val deviceId = "test_device_id"
        fakeGetCurrentDeviceIdUseCase.updateDeviceId(deviceId)

        val input =
            LoadDataEntriesInput(
                displayedStartTime = NOW.atStartOfDay(),
                packageName = deviceId,
                period = DateNavigationPeriod.PERIOD_DAY,
                showDataOrigin = true,
                permissionType = FitnessPermissionType.STEPS,
            )

        healthConnectManager.stub {
            on {
                readRecords(
                    argThat<ReadRecordsRequestUsingFilters<Record>> { request ->
                        request
                            ?.dataOrigins
                            ?.contains(
                                DataOrigin.Builder()
                                    .setPackageName(DEVICE_DATA_PROVIDER_PACKAGE)
                                    .build()
                            ) == true &&
                            request.dataOrigins.contains(
                                DataOrigin.Builder().setPackageName(deviceId).build()
                            )
                    },
                    any(),
                    any(),
                )
            } doReturnResult Result.success(ReadRecordsResponse(emptyList(), -1))
        }

        loadEntriesHelper.readRecords(input)
    }

    @Test
    @DisableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    fun readRecords_flagsOff_withCurrentDevicePackage_doesNotAddAndroidPackage() = runTest {
        val deviceId = "test_device_id"
        fakeGetCurrentDeviceIdUseCase.updateDeviceId(deviceId)

        val input =
            LoadDataEntriesInput(
                displayedStartTime = NOW.atStartOfDay(),
                packageName = deviceId,
                period = DateNavigationPeriod.PERIOD_DAY,
                showDataOrigin = true,
                permissionType = FitnessPermissionType.STEPS,
            )

        healthConnectManager.stub {
            on {
                readRecords(
                    argThat<ReadRecordsRequestUsingFilters<Record>> { request ->
                        request
                            ?.dataOrigins
                            ?.contains(
                                DataOrigin.Builder()
                                    .setPackageName(DEVICE_DATA_PROVIDER_PACKAGE)
                                    .build()
                            ) == false &&
                            request.dataOrigins.contains(
                                DataOrigin.Builder().setPackageName(deviceId).build()
                            )
                    },
                    any(),
                    any(),
                )
            } doReturnResult Result.success(ReadRecordsResponse(emptyList(), -1))
        }

        loadEntriesHelper.readRecords(input)
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    fun readRecords_withDevicePackage_doesNotAddAndroidPackage() = runTest {
        val deviceId = "test_device_id"
        fakeGetCurrentDeviceIdUseCase.updateDeviceId("not_test_device_id")

        val input =
            LoadDataEntriesInput(
                displayedStartTime = NOW.atStartOfDay(),
                packageName = deviceId,
                period = DateNavigationPeriod.PERIOD_DAY,
                showDataOrigin = true,
                permissionType = FitnessPermissionType.STEPS,
            )

        healthConnectManager.stub {
            on {
                readRecords(
                    argThat<ReadRecordsRequestUsingFilters<Record>> { request ->
                        request
                            ?.dataOrigins
                            ?.contains(
                                DataOrigin.Builder()
                                    .setPackageName(DEVICE_DATA_PROVIDER_PACKAGE)
                                    .build()
                            ) == false &&
                            request.dataOrigins.contains(
                                DataOrigin.Builder().setPackageName(deviceId).build()
                            )
                    },
                    any(),
                    any(),
                )
            } doReturnResult Result.success(ReadRecordsResponse(emptyList(), -1))
        }

        loadEntriesHelper.readRecords(input)
    }

    @Test
    @EnableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    fun readRecords_withAndroid_addsCurrentDeviceToFilter() = runTest {
        val currentDeviceId = "test_device_id"
        fakeGetCurrentDeviceIdUseCase.updateDeviceId(currentDeviceId)

        val input =
            LoadDataEntriesInput(
                displayedStartTime = NOW.atStartOfDay(),
                packageName = DEVICE_DATA_PROVIDER_PACKAGE,
                period = DateNavigationPeriod.PERIOD_DAY,
                showDataOrigin = true,
                permissionType = FitnessPermissionType.STEPS,
            )

        healthConnectManager.stub {
            on {
                readRecords(
                    argThat<ReadRecordsRequestUsingFilters<Record>> { request ->
                        request
                            ?.dataOrigins
                            ?.contains(
                                DataOrigin.Builder()
                                    .setPackageName(DEVICE_DATA_PROVIDER_PACKAGE)
                                    .build()
                            ) == true &&
                            request.dataOrigins.contains(
                                DataOrigin.Builder().setPackageName(currentDeviceId).build()
                            )
                    },
                    any(),
                    any(),
                )
            } doReturnResult Result.success(ReadRecordsResponse(emptyList(), -1))
        }

        loadEntriesHelper.readRecords(input)
    }

    @Test
    @DisableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    fun readRecords_flagsOff_withAndroid_doesNotAddCurrentDevicePackage() = runTest {
        val currentDeviceId = "test_device_id"
        fakeGetCurrentDeviceIdUseCase.updateDeviceId(currentDeviceId)

        val input =
            LoadDataEntriesInput(
                displayedStartTime = NOW.atStartOfDay(),
                packageName = DEVICE_DATA_PROVIDER_PACKAGE,
                period = DateNavigationPeriod.PERIOD_DAY,
                showDataOrigin = true,
                permissionType = FitnessPermissionType.STEPS,
            )

        healthConnectManager.stub {
            on {
                readRecords(
                    argThat<ReadRecordsRequestUsingFilters<Record>> { request ->
                        request
                            ?.dataOrigins
                            ?.contains(
                                DataOrigin.Builder()
                                    .setPackageName(DEVICE_DATA_PROVIDER_PACKAGE)
                                    .build()
                            ) == true &&
                            request.dataOrigins.contains(
                                DataOrigin.Builder().setPackageName(currentDeviceId).build()
                            ) == false
                    },
                    any(),
                    any(),
                )
            } doReturnResult Result.success(ReadRecordsResponse(emptyList(), -1))
        }

        loadEntriesHelper.readRecords(input)
    }

    @Test
    fun loadSleepData_withinDay_returnsListOfRecords_sortedByDescendingStartTime() = runTest {
        val (input, timeRangeFilter) =
            setupReadRecordTest(DateNavigationPeriod.PERIOD_DAY, FitnessPermissionType.SLEEP)

        val actual = loadEntriesHelper.readRecords(input)

        val expected = listOf(SLEEP_DAY_9H15, SLEEP_DAY_0H20, SLEEP_DAY_1H45)

        assertReadRecordsRequest(timeRangeFilter, SleepSessionRecord::class.java)
        verifySleepSessionListsEqual(actual, expected)
    }

    @Test
    fun loadSleepDataUseCase_withinWeek_returnsListOfRecords_sortedByDescendingStartTime() =
        runTest {
            val (input, timeRangeFilter) =
                setupReadRecordTest(DateNavigationPeriod.PERIOD_WEEK, FitnessPermissionType.SLEEP)

            val actual = loadEntriesHelper.readRecords(input)
            val expected =
                listOf(
                    SLEEP_WEEK_9H15,
                    SLEEP_DAY_9H15,
                    SLEEP_DAY_0H20,
                    SLEEP_DAY_1H45,
                    SLEEP_WEEK_33H15,
                )

            assertReadRecordsRequest(timeRangeFilter, SleepSessionRecord::class.java)
            verifySleepSessionListsEqual(actual, expected)
        }

    @Test
    fun loadSleepDataUseCase_withinMonth_returnsListOfRecords_sortedByDescendingStartTime() =
        runTest {
            val (input, timeRangeFilter) =
                setupReadRecordTest(DateNavigationPeriod.PERIOD_MONTH, FitnessPermissionType.SLEEP)

            val actual = loadEntriesHelper.readRecords(input)
            val expected =
                listOf(
                    SLEEP_MONTH_81H15,
                    SLEEP_WEEK_9H15,
                    SLEEP_DAY_9H15,
                    SLEEP_DAY_0H20,
                    SLEEP_DAY_1H45,
                    SLEEP_WEEK_33H15,
                )

            assertReadRecordsRequest(timeRangeFilter, SleepSessionRecord::class.java)
            verifySleepSessionListsEqual(actual, expected)
        }

    @Test
    fun loadSleepData_withinDay_returnsStartTime_skipsNoDataDays() = runTest {
        val input =
            LoadLatestEntryDateInput(
                displayedStartTime = defaultStartTime.atStartOfDay(),
                permissionType = FitnessPermissionType.SLEEP,
            )

        val timeRangeFilter =
            TimeInstantRangeFilter.Builder().setEndTime(defaultStartTime.atStartOfDay()).build()

        setupReadRecordTest(DateNavigationPeriod.PERIOD_MONTH, FitnessPermissionType.SLEEP)

        val actual = loadEntriesHelper.readLatestRecordDate(input)
        val expected = SLEEP_MONTH_81H15.startTime

        assertReadRecordsRequest(timeRangeFilter, SleepSessionRecord::class.java, pageSize = 1)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun loadStepsDataUseCase_withinDay_returnsListOfStepsAndCadenceRecords_sortedByDescendingStartTime() =
        runTest {
            val timePeriod = DateNavigationPeriod.PERIOD_DAY
            val input =
                LoadDataEntriesInput(
                    displayedStartTime = NOW.atStartOfDay(),
                    packageName = null,
                    period = timePeriod,
                    showDataOrigin = true,
                    permissionType = FitnessPermissionType.STEPS,
                )

            val stepRecord1 = getStepsRecord(100, NOW.plusSeconds(10))
            val stepRecord2 = getStepsRecord(50, NOW.plusSeconds(5))
            val stepCadenceRecord1 = getStepsCadenceRecord(NOW.plusSeconds(8))
            val stepCadenceRecord2 = getStepsCadenceRecord(NOW.plusSeconds(6))

            healthConnectManager.stub {
                on {
                    readRecords(
                        argThat<ReadRecordsRequestUsingFilters<Record>> { request ->
                            request?.recordType == StepsRecord::class.java
                        },
                        any(),
                        any(),
                    )
                } doReturnResult
                    Result.success(ReadRecordsResponse(listOf(stepRecord1, stepRecord2), -1))

                on {
                    readRecords(
                        argThat<ReadRecordsRequestUsingFilters<Record>> { request ->
                            request?.recordType == StepsCadenceRecord::class.java
                        },
                        any(),
                        any(),
                    )
                } doReturnResult
                    Result.success(
                        ReadRecordsResponse(listOf(stepCadenceRecord1, stepCadenceRecord2), -1)
                    )
            }

            val actual = loadEntriesHelper.readRecords(input)
            val expected = listOf(stepRecord1, stepCadenceRecord1, stepCadenceRecord2, stepRecord2)

            assertThat(actual).containsExactlyElementsIn(expected)
        }

    @Test
    fun loadHydrationUseCase_withinWeek_returnsListOfRecords_sortedByDescendingStartTime() =
        runTest {
            val (input, timeRangeFilter) =
                setupReadRecordTest(
                    DateNavigationPeriod.PERIOD_WEEK,
                    FitnessPermissionType.HYDRATION,
                )

            val actual = loadEntriesHelper.readRecords(input)
            val expected = listOf(HYDRATION_MONTH3, HYDRATION_MONTH2, HYDRATION_MONTH)

            assertReadRecordsRequest(timeRangeFilter, HydrationRecord::class.java)
            verifyHydrationListsEqual(actual, expected)
        }

    @Test
    fun loadOxygenSaturationUseCase_withinDay_returnsListOfRecords_sortedByDescendingStartTime() =
        runTest {
            val (input, timeRangeFilter) =
                setupReadRecordTest(
                    DateNavigationPeriod.PERIOD_DAY,
                    FitnessPermissionType.OXYGEN_SATURATION,
                )

            val actual = loadEntriesHelper.readRecords(input)
            val expected = listOf(OXYGENSATURATION_DAY2, OXYGENSATURATION_DAY)

            assertReadRecordsRequest(timeRangeFilter, OxygenSaturationRecord::class.java)
            verifyOxygenSaturationListsEqual(actual, expected)
        }

    @Test
    fun loadFloorsClimbedUseCase_withinMonth_returnsEmptyListOfRecords() = runTest {
        val (input, timeRangeFilter) =
            setupReadRecordTest(
                DateNavigationPeriod.PERIOD_MONTH,
                FitnessPermissionType.FLOORS_CLIMBED,
            )

        val actual = loadEntriesHelper.readRecords(input)

        assertThat(actual.size).isEqualTo(0)
        assertReadRecordsRequest(timeRangeFilter, FloorsClimbedRecord::class.java)
    }

    @Test
    fun loadBodyWaterMass_withinWeek_singleRecord_lastRecordAndGetRecordsReturnsSame() = runTest {
        val (input, timeRangeFilter) =
            setupReadRecordTest(
                DateNavigationPeriod.PERIOD_WEEK,
                FitnessPermissionType.BODY_WATER_MASS,
            )

        val expectedGetRecords = loadEntriesHelper.readRecords(input)
        val expectedGetLastRecord = loadEntriesHelper.readLastRecord(input)
        val actual = listOf(BODYWATERMASS_WEEK)

        assertReadRecordsRequest(
            timeRangeFilter,
            BodyWaterMassRecord::class.java,
            wantedInvocationCount = 2,
            pageSize = 1,
            ascending = false,
        )
        verifyBodyWaterMassListsEqual(expectedGetRecords, actual)
        verifyBodyWaterMassListsEqual(expectedGetLastRecord, actual)
        verifyBodyWaterMassListsEqual(expectedGetLastRecord, expectedGetRecords)
    }

    @Test
    fun readLastRecord_forBodyTemperature_returnsListOfOneRecord() = runTest {
        val (input, timeRangeFilter) =
            setupReadRecordTest(
                DateNavigationPeriod.PERIOD_MONTH,
                FitnessPermissionType.BODY_TEMPERATURE,
            )

        val expected = loadEntriesHelper.readLastRecord(input)
        val actual = listOf(BODYTEMPERATURE_MONTH)

        assertReadRecordsRequest(
            timeRangeFilter,
            BodyTemperatureRecord::class.java,
            pageSize = 1,
            ascending = false,
        )
        assertThat(actual.size).isEqualTo(expected.size)
        assertThat(actual[0].time).isEqualTo(INSTANT_MONTH3)
        assertThat(actual[0].measurementLocation)
            .isEqualTo(BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_MOUTH)
        assertThat(actual[0].temperature).isEqualTo(Temperature.fromCelsius(100.0))
    }

    @Test
    fun readLastRecord_forDistance_returnsListOfOneRecord() = runTest {
        val (input, timeRangeFilter) =
            setupReadRecordTest(DateNavigationPeriod.PERIOD_MONTH, FitnessPermissionType.DISTANCE)

        val actual = loadEntriesHelper.readLastRecord(input)

        val expected = listOf(DISTANCE_STARTDATE_1500)

        assertReadRecordsRequest(
            timeRangeFilter,
            DistanceRecord::class.java,
            pageSize = 1,
            ascending = false,
        )
        assertThat(actual.size).isEqualTo(expected.size)
        assertThat((actual[0] as DistanceRecord).distance).isEqualTo(expected[0].distance)
        assertThat((actual[0] as DistanceRecord).startTime).isEqualTo(defaultStartTime)
        assertThat((actual[0] as DistanceRecord).endTime).isEqualTo(expected[0].endTime)
    }

    @Test
    fun readLastRecord_forIntermenstrualBleeding_returnsListOfOneRecord() = runTest {
        val (input, timeRangeFilter) =
            setupReadRecordTest(
                DateNavigationPeriod.PERIOD_DAY,
                FitnessPermissionType.INTERMENSTRUAL_BLEEDING,
            )

        val actual = loadEntriesHelper.readLastRecord(input)
        val expected = listOf(INTERMENSTRUAL_BLEEDING_DAY)

        assertReadRecordsRequest(
            timeRangeFilter,
            IntermenstrualBleedingRecord::class.java,
            pageSize = 1,
            ascending = false,
        )
        assertThat(actual.size).isEqualTo(expected.size)
        assertThat((actual[0] as IntermenstrualBleedingRecord).time).isEqualTo(INSTANT_DAY)
    }

    @Test
    fun loadMenstruationData_returnsEndTime_skipsNoDataDays() = runTest {
        val input =
            LoadLatestEntryDateInput(
                displayedStartTime = defaultStartTime.atStartOfDay(),
                permissionType = FitnessPermissionType.MENSTRUATION,
            )

        val timeRangeFilter =
            TimeInstantRangeFilter.Builder().setEndTime(defaultStartTime.atStartOfDay()).build()

        setupReadRecordTest(DateNavigationPeriod.PERIOD_DAY, FitnessPermissionType.MENSTRUATION)

        val actual = loadEntriesHelper.readLatestRecordDate(input)
        val expected = MENSTRUATION_PERIOD_5D.endTime

        assertReadRecordsRequest(
            timeRangeFilter,
            MenstruationFlowRecord::class
                .java, // It checks both, but one is enough to verify filter
            wantedInvocationCount = 2,
            pageSize = 1,
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun readLastRecord_forWeight_returnsListOfOneRecord() = runTest {
        val (input, timeRangeFilter) =
            setupReadRecordTest(DateNavigationPeriod.PERIOD_WEEK, FitnessPermissionType.WEIGHT)

        val actual = loadEntriesHelper.readLastRecord(input)
        val expected = listOf(WEIGHT_WEEK_100)

        assertReadRecordsRequest(
            timeRangeFilter,
            WeightRecord::class.java,
            pageSize = 1,
            ascending = false,
        )
        assertThat(actual.size).isEqualTo(expected.size)
        assertThat((actual[0] as WeightRecord).weight).isEqualTo(expected[0].weight)
        assertThat((actual[0] as WeightRecord).time).isEqualTo(INSTANT_WEEK)
    }

    @Test
    fun readLastRecord_forTotalCaloriesBurned_whenNoData_returnsEmptyList() = runTest {
        val (input, timeRangeFilter) =
            setupReadRecordTest(
                DateNavigationPeriod.PERIOD_MONTH,
                FitnessPermissionType.ACTIVE_CALORIES_BURNED,
            )

        val actual = loadEntriesHelper.readLastRecord(input)

        assertThat(actual.size).isEqualTo(0)
        assertReadRecordsRequest(
            timeRangeFilter,
            ActiveCaloriesBurnedRecord::class.java,
            pageSize = 1,
            ascending = false,
        )
    }

    @Test
    fun readRecordsFromDifferentDays_twoSequentialDays_sectionHeadersInserted() = runTest {
        timeSource.setNow(START_TIME)

        val recordList: List<Record> = getMixedRecordsAcrossTwoDays(timeSource)

        val formattedEntry: List<FormattedEntry> =
            loadEntriesHelper.maybeAddDateSectionHeaders(
                recordList,
                DateNavigationPeriod.PERIOD_WEEK,
                true,
            )

        val potentialHeaderToday = formattedEntry[0]
        val potentialHeaderYesterday = formattedEntry[3]
        assertThat(formattedEntry.size).isEqualTo(6)
        assertThat(potentialHeaderToday)
            .isInstanceOf(FormattedEntry.EntryDateSectionHeader::class.java)
        assertThat((potentialHeaderToday as FormattedEntry.EntryDateSectionHeader).date)
            .isEqualTo("Today")
        assertThat(potentialHeaderYesterday)
            .isInstanceOf(FormattedEntry.EntryDateSectionHeader::class.java)
        assertThat((potentialHeaderYesterday as FormattedEntry.EntryDateSectionHeader).date)
            .isEqualTo("Yesterday")
    }

    @Test
    fun readRecordsFromDifferentDays_threeSequentialDays_sectionHeadersInserted() = runTest {
        timeSource.setNow(START_TIME)
        val recordList: List<Record> = getMixedRecordsAcrossThreeDays(timeSource)

        val formattedEntry: List<FormattedEntry> =
            loadEntriesHelper.maybeAddDateSectionHeaders(
                recordList,
                DateNavigationPeriod.PERIOD_WEEK,
                true,
            )

        val potentialHeaderToday = formattedEntry[0]
        val potentialHeaderYesterday = formattedEntry[3]
        val potentialHeaderTwoDaysAgo = formattedEntry[6]
        assertThat(formattedEntry.size).isEqualTo(9)
        assertThat(potentialHeaderToday)
            .isInstanceOf(FormattedEntry.EntryDateSectionHeader::class.java)
        assertThat((potentialHeaderToday as FormattedEntry.EntryDateSectionHeader).date)
            .isEqualTo("Today")
        assertThat(potentialHeaderYesterday)
            .isInstanceOf(FormattedEntry.EntryDateSectionHeader::class.java)
        assertThat((potentialHeaderYesterday as FormattedEntry.EntryDateSectionHeader).date)
            .isEqualTo("Yesterday")
        assertThat(potentialHeaderTwoDaysAgo)
            .isInstanceOf(FormattedEntry.EntryDateSectionHeader::class.java)
        assertThat((potentialHeaderTwoDaysAgo as FormattedEntry.EntryDateSectionHeader).date)
            .isEqualTo(
                LocalDateTimeFormatter(context)
                    .formatLongDate(timeSource.currentLocalDateTime().minusDays(2).toInstant())
            )
    }

    @Test
    fun readMedicalResources_allMedicalData_returnsEmptyList() = runTest {
        val input = setupReadMedicalResourceTest(MedicalPermissionType.ALL_MEDICAL_DATA)
        val actual = loadEntriesHelper.readMedicalRecords(input)

        assertThat(actual.size).isEqualTo(0)
    }

    @Test
    fun readMedicalResources_allImmunization() = runTest {
        val input = setupReadMedicalResourceTest(MedicalPermissionType.VACCINES)
        val actual = loadEntriesHelper.readMedicalRecords(input)

        assertReadMedicalResourcesRequest()
        assertThat(actual.size).isEqualTo(1)
        assertThat(actual[0].dataSourceId).isEqualTo(TEST_DATASOURCE_ID)
    }

    @Test
    fun readMedicalResources_immunizationFromApp() = runTest {
        healthConnectManager.stub {
            on {
                getMedicalDataSources(any<GetMedicalDataSourcesRequest>(), any(), any())
            } doReturnResult
                Result.success<List<MedicalDataSource>>(
                    listOf(TEST_MEDICAL_DATA_SOURCE, TEST_MEDICAL_DATA_SOURCE_2)
                )
        }

        val input =
            setupReadMedicalResourceTest(MedicalPermissionType.VACCINES, TEST_APP_PACKAGE_NAME)
        val actual = loadEntriesHelper.readMedicalRecords(input)

        assertReadMedicalResourcesRequest()
        assertThat(actual.size).isEqualTo(1)
        assertThat(actual[0].dataSourceId).isEqualTo(TEST_DATASOURCE_ID)
    }

    private fun getSleepRecords(
        timePeriod: DateNavigationPeriod
    ): ReadRecordsResponse<SleepSessionRecord> {
        return when (timePeriod) {
            DateNavigationPeriod.PERIOD_DAY ->
                ReadRecordsResponse<SleepSessionRecord>(
                    listOf(SLEEP_DAY_9H15, SLEEP_DAY_0H20, SLEEP_DAY_1H45),
                    -1,
                )
            DateNavigationPeriod.PERIOD_WEEK ->
                ReadRecordsResponse<SleepSessionRecord>(
                    listOf(
                        SLEEP_DAY_9H15,
                        SLEEP_DAY_0H20,
                        SLEEP_DAY_1H45,
                        SLEEP_WEEK_33H15,
                        SLEEP_WEEK_9H15,
                    ),
                    -1,
                )
            DateNavigationPeriod.PERIOD_MONTH ->
                ReadRecordsResponse<SleepSessionRecord>(
                    listOf(
                        SLEEP_DAY_9H15,
                        SLEEP_DAY_0H20,
                        SLEEP_DAY_1H45,
                        SLEEP_WEEK_33H15,
                        SLEEP_WEEK_9H15,
                        SLEEP_MONTH_81H15,
                    ),
                    -1,
                )
            else -> throw IllegalArgumentException("DateNavigationPeriod $timePeriod not supported")
        }
    }

    private fun setupReadRecordTest(
        timePeriod: DateNavigationPeriod,
        permissionType: FitnessPermissionType,
    ): Pair<LoadDataEntriesInput, TimeInstantRangeFilter> {
        val input =
            LoadDataEntriesInput(
                displayedStartTime = defaultStartTime.atStartOfDay(),
                packageName = null,
                period = timePeriod,
                showDataOrigin = true,
                permissionType = permissionType,
            )
        val timeRangeFilter =
            loadEntriesHelper.getTimeFilter(defaultStartTime.atStartOfDay(), timePeriod, true)

        val response =
            when (permissionType) {
                FitnessPermissionType.ACTIVE_CALORIES_BURNED ->
                    Result.success(ReadRecordsResponse<Record>(emptyList(), -1))
                FitnessPermissionType.SLEEP ->
                    Result.success(getSleepRecords(timePeriod) as ReadRecordsResponse<Record>)
                FitnessPermissionType.WEIGHT ->
                    Result.success(
                        ReadRecordsResponse<Record>(
                            when (timePeriod) {
                                DateNavigationPeriod.PERIOD_DAY -> listOf(WEIGHT_DAY_100)
                                DateNavigationPeriod.PERIOD_WEEK -> listOf(WEIGHT_WEEK_100)
                                DateNavigationPeriod.PERIOD_MONTH -> listOf(WEIGHT_MONTH_100)
                            },
                            -1,
                        )
                    )
                FitnessPermissionType.DISTANCE ->
                    Result.success(ReadRecordsResponse<Record>(listOf(DISTANCE_STARTDATE_1500), -1))
                FitnessPermissionType.MENSTRUATION ->
                    Result.success(ReadRecordsResponse<Record>(listOf(MENSTRUATION_PERIOD_5D), -1))
                FitnessPermissionType.INTERMENSTRUAL_BLEEDING ->
                    Result.success(
                        ReadRecordsResponse<Record>(listOf(INTERMENSTRUAL_BLEEDING_DAY), -1)
                    )
                FitnessPermissionType.BODY_TEMPERATURE ->
                    Result.success(ReadRecordsResponse<Record>(listOf(BODYTEMPERATURE_MONTH), -1))
                FitnessPermissionType.OXYGEN_SATURATION ->
                    Result.success(
                        ReadRecordsResponse<Record>(
                            listOf(OXYGENSATURATION_DAY2, OXYGENSATURATION_DAY),
                            -1,
                        )
                    )
                FitnessPermissionType.HYDRATION ->
                    Result.success(
                        ReadRecordsResponse<Record>(
                            listOf(HYDRATION_MONTH3, HYDRATION_MONTH2, HYDRATION_MONTH),
                            -1,
                        )
                    )
                FitnessPermissionType.FLOORS_CLIMBED ->
                    Result.success(ReadRecordsResponse<Record>(emptyList(), -1))
                FitnessPermissionType.BODY_WATER_MASS ->
                    Result.success(ReadRecordsResponse<Record>(listOf(BODYWATERMASS_WEEK), -1))
                else ->
                    throw IllegalArgumentException(
                        "HealthPermissionType $permissionType not supported"
                    )
            }

        healthConnectManager.stub {
            on {
                readRecords<Record>(
                    any<ReadRecordsRequest<Record>>(),
                    any<java.util.concurrent.Executor>(),
                    any<OutcomeReceiver<ReadRecordsResponse<Record>, HealthConnectException>>(),
                )
            } doReturnResult response
        }

        return Pair(input, timeRangeFilter)
    }

    private fun assertReadRecordsRequest(
        timeRangeFilter: TimeInstantRangeFilter,
        recordType: Class<out Record>,
        wantedInvocationCount: Int = 1,
        pageSize: Int = 1000,
        ascending: Boolean = false,
    ) {
        val captor = argumentCaptor<ReadRecordsRequest<Record>>()
        verify(healthConnectManager, times(wantedInvocationCount))
            .readRecords(captor.capture(), any(), any())

        val lastRequest = captor.lastValue as ReadRecordsRequestUsingFilters<Record>
        assertThat(lastRequest.recordType).isEqualTo(recordType)
        assertThat(lastRequest.pageSize).isEqualTo(pageSize)
        assertThat(lastRequest.isAscending).isEqualTo(ascending)
        assertThat((lastRequest.timeRangeFilter as TimeInstantRangeFilter).startTime)
            .isEqualTo(timeRangeFilter.startTime)
        assertThat((lastRequest.timeRangeFilter as TimeInstantRangeFilter).endTime)
            .isEqualTo(timeRangeFilter.endTime)
    }

    private fun setupReadMedicalResourceTest(
        permissionType: MedicalPermissionType,
        packageName: String? = null,
    ): LoadMedicalEntriesInput {
        val input =
            LoadMedicalEntriesInput(
                packageName = packageName,
                showDataOrigin = true,
                medicalPermissionType = permissionType,
            )

        val response =
            when (permissionType) {
                MedicalPermissionType.VACCINES ->
                    Result.success(
                        ReadMedicalResourcesResponse(
                            listOf(TEST_MEDICAL_RESOURCE_IMMUNIZATION),
                            "nextPageToken",
                            1,
                        )
                    )
                MedicalPermissionType.ALL_MEDICAL_DATA ->
                    Result.success(ReadMedicalResourcesResponse(emptyList(), "nextPageToken", 1))
                else ->
                    throw IllegalArgumentException(
                        "MedicalPermissionType $permissionType not supported"
                    )
            }

        healthConnectManager.stub {
            on {
                readMedicalResources(
                    any<ReadMedicalResourcesInitialRequest>(),
                    any<java.util.concurrent.Executor>(),
                    any<OutcomeReceiver<ReadMedicalResourcesResponse, HealthConnectException>>(),
                )
            } doReturnResult response
        }

        return input
    }

    private fun assertReadMedicalResourcesRequest() {
        val captor = argumentCaptor<ReadMedicalResourcesInitialRequest>()
        verify(healthConnectManager).readMedicalResources(captor.capture(), any(), any())
        assertThat(captor.firstValue.medicalResourceType).isEqualTo(MEDICAL_RESOURCE_TYPE_VACCINES)
    }
}
