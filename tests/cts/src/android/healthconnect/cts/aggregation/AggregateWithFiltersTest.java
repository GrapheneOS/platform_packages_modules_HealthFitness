/*
 * Copyright (C) 2024 The Android Open Source Project
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

package android.healthconnect.cts.aggregation;

import static android.health.connect.HealthDataCategory.ACTIVITY;
import static android.health.connect.datatypes.ElevationGainedRecord.ELEVATION_GAINED_TOTAL;
import static android.health.connect.datatypes.HeartRateRecord.BPM_AVG;
import static android.health.connect.datatypes.HeartRateRecord.BPM_MAX;
import static android.health.connect.datatypes.HeartRateRecord.BPM_MIN;
import static android.health.connect.datatypes.HeartRateRecord.HEART_MEASUREMENTS_COUNT;
import static android.health.connect.datatypes.StepsRecord.STEPS_COUNT_TOTAL;
import static android.health.connect.datatypes.WeightRecord.WEIGHT_MAX;
import static android.health.connect.datatypes.WeightRecord.WEIGHT_MIN;
import static android.healthconnect.cts.aggregation.DataFactory.getOpenEndTimeFilter;
import static android.healthconnect.cts.aggregation.DataFactory.getOpenStartTimeFilter;
import static android.healthconnect.cts.aggregation.DataFactory.getTimeFilter;
import static android.healthconnect.cts.utils.DataFactory.getDataOrigin;
import static android.healthconnect.cts.utils.DataFactory.getDistanceRecord;
import static android.healthconnect.cts.utils.DataFactory.getHeartRateRecord;
import static android.healthconnect.cts.utils.DataFactory.getStepsRecord;
import static android.healthconnect.cts.utils.DataFactory.getWeightRecord;
import static android.healthconnect.cts.utils.TestUtils.deleteAllStagedRemoteData;
import static android.healthconnect.cts.utils.TestUtils.getAggregateResponse;
import static android.healthconnect.cts.utils.TestUtils.getAggregateResponseGroupByDuration;
import static android.healthconnect.cts.utils.TestUtils.getAggregateResponseGroupByPeriod;
import static android.healthconnect.cts.utils.TestUtils.insertRecords;
import static android.healthconnect.cts.utils.TestUtils.setupAggregation;
import static android.healthconnect.cts.utils.TestUtils.updatePriorityWithManageHealthDataPermission;

import static com.google.common.truth.Truth.assertThat;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;

import android.content.Context;
import android.health.connect.AggregateRecordsGroupedByDurationResponse;
import android.health.connect.AggregateRecordsGroupedByPeriodResponse;
import android.health.connect.AggregateRecordsRequest;
import android.health.connect.AggregateRecordsResponse;
import android.health.connect.LocalTimeRangeFilter;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.datatypes.HeartRateRecord;
import android.health.connect.datatypes.units.Length;
import android.health.connect.datatypes.units.Mass;
import android.healthconnect.cts.lib.TestAppProxy;
import android.healthconnect.cts.utils.AssumptionCheckerRule;
import android.healthconnect.cts.utils.DeviceSupportUtils;

import androidx.test.core.app.ApplicationProvider;

import com.google.common.collect.ImmutableList;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.List;

public class AggregateWithFiltersTest {
    private static final String PKG_TEST_APP = "android.healthconnect.cts.testapp.readWritePerms.A";

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final String mPackageName = mContext.getPackageName();
    private final ZoneOffset mCurrentZone =
            ZoneOffset.systemDefault().getRules().getOffset(Instant.now());
    private final TestAppProxy mTestApp = TestAppProxy.forPackageName(PKG_TEST_APP);

    @Rule
    public AssumptionCheckerRule mSupportedHardwareRule =
            new AssumptionCheckerRule(
                    DeviceSupportUtils::isHealthConnectFullySupported,
                    "Tests should run on supported hardware only.");

    @Before
    public void setUp() throws InterruptedException {
        deleteAllStagedRemoteData();
        setupAggregation(mPackageName, ACTIVITY);
    }

    @After
    public void tearDown() {
        deleteAllStagedRemoteData();
    }

    @Test
    public void dataOriginFilter_noFilter_everythingIncluded() throws Exception {
        Instant startTime = Instant.now().minus(1, DAYS);
        mTestApp.insertRecord(getStepsRecord(50, startTime, startTime.plusMillis(1000)));
        insertRecords(
                List.of(
                        getStepsRecord(
                                100, startTime.plusMillis(1000), startTime.plusMillis(2000))));
        updatePriorityWithManageHealthDataPermission(
                ACTIVITY, ImmutableList.of(PKG_TEST_APP, mPackageName));

        AggregateRecordsRequest<Long> request =
                new AggregateRecordsRequest.Builder<Long>(
                                getTimeFilter(startTime.minus(1, HOURS), startTime.plus(1, HOURS)))
                        .addAggregationType(STEPS_COUNT_TOTAL)
                        .build();

        AggregateRecordsResponse<Long> response = getAggregateResponse(request);
        assertThat(response.get(STEPS_COUNT_TOTAL)).isEqualTo(150);
        assertThat(response.getDataOrigins(STEPS_COUNT_TOTAL))
                .containsExactly(getDataOrigin(mPackageName), getDataOrigin(PKG_TEST_APP));
    }

    @Test
    public void dataOriginFilter_validFilter_onlyDataFromFilteredApps() throws Exception {
        Instant startTime = Instant.now().minus(1, DAYS);
        mTestApp.insertRecord(getStepsRecord(50, startTime, startTime.plusMillis(1000)));
        insertRecords(
                List.of(
                        getStepsRecord(
                                100, startTime.plusMillis(1000), startTime.plusMillis(2000))));
        updatePriorityWithManageHealthDataPermission(
                ACTIVITY, ImmutableList.of(PKG_TEST_APP, mPackageName));

        AggregateRecordsRequest<Long> request =
                new AggregateRecordsRequest.Builder<Long>(
                                getTimeFilter(startTime.minus(1, HOURS), startTime.plus(1, HOURS)))
                        .addAggregationType(STEPS_COUNT_TOTAL)
                        .addDataOriginsFilter(getDataOrigin(PKG_TEST_APP))
                        .build();

        AggregateRecordsResponse<Long> response = getAggregateResponse(request);
        assertThat(response.get(STEPS_COUNT_TOTAL)).isEqualTo(50);
        assertThat(response.getDataOrigins(STEPS_COUNT_TOTAL))
                .containsExactly(getDataOrigin(PKG_TEST_APP));
    }

    @Test
    public void dataOriginFilter_invalidApp_noDataReturned() throws Exception {
        Instant startTime = Instant.now().minus(1, DAYS);
        LocalDateTime localTime = startTime.atOffset(mCurrentZone).toLocalDateTime();
        mTestApp.insertRecord(getStepsRecord(50, startTime, startTime.plusMillis(1000)));
        insertRecords(
                List.of(
                        getStepsRecord(
                                100,
                                startTime.plusMillis(1000),
                                startTime.plusMillis(2000),
                                /* clientId= */ "own_steps")));
        updatePriorityWithManageHealthDataPermission(
                ACTIVITY, ImmutableList.of(PKG_TEST_APP, mPackageName));

        TimeInstantRangeFilter instantFilter =
                getTimeFilter(startTime.minus(1, HOURS), startTime.plus(1, HOURS));
        AggregateRecordsRequest<Long> requestWithInstantFilter =
                new AggregateRecordsRequest.Builder<Long>(instantFilter)
                        .addAggregationType(STEPS_COUNT_TOTAL)
                        .addDataOriginsFilter(getDataOrigin("invalid.app.pkg"))
                        .build();
        LocalTimeRangeFilter localTimeFilter =
                getTimeFilter(localTime.minusHours(1), localTime.plusHours(1));
        AggregateRecordsRequest<Long> requestWithLocalTimeFilter =
                new AggregateRecordsRequest.Builder<Long>(localTimeFilter)
                        .addAggregationType(STEPS_COUNT_TOTAL)
                        .addDataOriginsFilter(getDataOrigin("invalid.app.pkg"))
                        .build();

        AggregateRecordsResponse<Long> aggregateResponse =
                getAggregateResponse(requestWithInstantFilter);
        assertThat(aggregateResponse.get(STEPS_COUNT_TOTAL)).isNull();
        assertThat(aggregateResponse.getDataOrigins(STEPS_COUNT_TOTAL)).isEmpty();

        List<AggregateRecordsGroupedByDurationResponse<Long>> groupedByDurationResponses =
                getAggregateResponseGroupByDuration(requestWithInstantFilter, Duration.ofDays(1));
        assertThat(groupedByDurationResponses).hasSize(1);
        assertThat(groupedByDurationResponses.get(0).get(STEPS_COUNT_TOTAL)).isNull();
        assertThat(groupedByDurationResponses.get(0).getDataOrigins(STEPS_COUNT_TOTAL)).isEmpty();

        List<AggregateRecordsGroupedByPeriodResponse<Long>> groupedByPeriodResponses =
                getAggregateResponseGroupByPeriod(requestWithLocalTimeFilter, Period.ofDays(1));
        assertThat(groupedByPeriodResponses).hasSize(1);
        assertThat(groupedByPeriodResponses.get(0).get(STEPS_COUNT_TOTAL)).isNull();
        assertThat(groupedByPeriodResponses.get(0).getDataOrigins(STEPS_COUNT_TOTAL)).isEmpty();
    }

    @Test
    public void dataOriginFilter_appNotInPriorityList_noDataReturned() throws Exception {
        Instant startTime = Instant.now().minus(1, DAYS);
        LocalDateTime localTime = startTime.atOffset(mCurrentZone).toLocalDateTime();
        mTestApp.insertRecord(getStepsRecord(50, startTime, startTime.plusMillis(1000)));
        insertRecords(
                List.of(
                        getStepsRecord(
                                100, startTime.plusMillis(1000), startTime.plusMillis(2000))));
        // remove mPackageName from priority list
        updatePriorityWithManageHealthDataPermission(ACTIVITY, ImmutableList.of(PKG_TEST_APP));

        TimeInstantRangeFilter instantFilter =
                getTimeFilter(startTime.minus(1, HOURS), startTime.plus(1, HOURS));
        AggregateRecordsRequest<Long> requestWithInstantFilter =
                new AggregateRecordsRequest.Builder<Long>(instantFilter)
                        .addAggregationType(STEPS_COUNT_TOTAL)
                        .addDataOriginsFilter(getDataOrigin(mPackageName))
                        .build();
        LocalTimeRangeFilter localTimeFilter =
                getTimeFilter(localTime.minusHours(1), localTime.plusHours(1));
        AggregateRecordsRequest<Long> requestWithLocalTimeFilter =
                new AggregateRecordsRequest.Builder<Long>(localTimeFilter)
                        .addAggregationType(STEPS_COUNT_TOTAL)
                        .addDataOriginsFilter(getDataOrigin(mPackageName))
                        .build();

        AggregateRecordsResponse<Long> aggregateResponse =
                getAggregateResponse(requestWithInstantFilter);
        assertThat(aggregateResponse.get(STEPS_COUNT_TOTAL)).isNull();
        assertThat(aggregateResponse.getDataOrigins(STEPS_COUNT_TOTAL)).isEmpty();

        List<AggregateRecordsGroupedByDurationResponse<Long>> groupedByDurationResponses =
                getAggregateResponseGroupByDuration(requestWithInstantFilter, Duration.ofDays(1));
        assertThat(groupedByDurationResponses).hasSize(1);
        assertThat(groupedByDurationResponses.get(0).get(STEPS_COUNT_TOTAL)).isNull();
        assertThat(groupedByDurationResponses.get(0).getDataOrigins(STEPS_COUNT_TOTAL)).isEmpty();

        List<AggregateRecordsGroupedByPeriodResponse<Long>> groupedByPeriodResponses =
                getAggregateResponseGroupByPeriod(requestWithLocalTimeFilter, Period.ofDays(1));
        assertThat(groupedByPeriodResponses).hasSize(1);
        assertThat(groupedByPeriodResponses.get(0).get(STEPS_COUNT_TOTAL)).isNull();
        assertThat(groupedByPeriodResponses.get(0).getDataOrigins(STEPS_COUNT_TOTAL)).isEmpty();
    }

    @Test
    public void dataOriginFilter_noDataFromFilteredApps_noDataReturned() throws Exception {
        Instant startTime = Instant.now().minus(1, DAYS);
        LocalDateTime localTime = startTime.atOffset(mCurrentZone).toLocalDateTime();
        mTestApp.insertRecord(getStepsRecord(50, startTime, startTime.plusMillis(1000)));
        updatePriorityWithManageHealthDataPermission(
                ACTIVITY, ImmutableList.of(PKG_TEST_APP, mPackageName));

        TimeInstantRangeFilter instantFilter =
                getTimeFilter(startTime.minus(1, HOURS), startTime.plus(1, HOURS));
        AggregateRecordsRequest<Long> requestWithInstantFilter =
                new AggregateRecordsRequest.Builder<Long>(instantFilter)
                        .addAggregationType(STEPS_COUNT_TOTAL)
                        .addDataOriginsFilter(getDataOrigin(mPackageName))
                        .build();
        LocalTimeRangeFilter localTimeFilter =
                getTimeFilter(localTime.minusHours(1), localTime.plusHours(1));
        AggregateRecordsRequest<Long> requestWithLocalTimeFilter =
                new AggregateRecordsRequest.Builder<Long>(localTimeFilter)
                        .addAggregationType(STEPS_COUNT_TOTAL)
                        .addDataOriginsFilter(getDataOrigin(mPackageName))
                        .build();

        AggregateRecordsResponse<Long> aggregateResponse =
                getAggregateResponse(requestWithInstantFilter);
        assertThat(aggregateResponse.get(STEPS_COUNT_TOTAL)).isNull();
        assertThat(aggregateResponse.getDataOrigins(STEPS_COUNT_TOTAL)).isEmpty();

        List<AggregateRecordsGroupedByDurationResponse<Long>> groupedByDurationResponses =
                getAggregateResponseGroupByDuration(requestWithInstantFilter, Duration.ofDays(1));
        assertThat(groupedByDurationResponses).hasSize(1);
        assertThat(groupedByDurationResponses.get(0).get(STEPS_COUNT_TOTAL)).isNull();
        assertThat(groupedByDurationResponses.get(0).getDataOrigins(STEPS_COUNT_TOTAL)).isEmpty();

        List<AggregateRecordsGroupedByPeriodResponse<Long>> groupedByPeriodResponses =
                getAggregateResponseGroupByPeriod(requestWithLocalTimeFilter, Period.ofDays(1));
        assertThat(groupedByPeriodResponses).hasSize(1);
        assertThat(groupedByPeriodResponses.get(0).get(STEPS_COUNT_TOTAL)).isNull();
        assertThat(groupedByPeriodResponses.get(0).getDataOrigins(STEPS_COUNT_TOTAL)).isEmpty();
    }

    @Test
    public void timeRangeFilter_noDataWithinTimeInterval_noDataReturned() throws Exception {
        Instant startTime = Instant.now().minus(1, DAYS);
        LocalDateTime localTime = startTime.atOffset(mCurrentZone).toLocalDateTime();
        insertRecords(
                List.of(
                        getStepsRecord(
                                100, startTime.plusMillis(1000), startTime.plusMillis(2000))));

        TimeInstantRangeFilter instantFilter = getTimeFilter(startTime.minus(1, HOURS), startTime);
        AggregateRecordsRequest<Long> requestWithInstantFilter =
                new AggregateRecordsRequest.Builder<Long>(instantFilter)
                        .addAggregationType(STEPS_COUNT_TOTAL)
                        .build();
        LocalTimeRangeFilter localTimeFilter = getTimeFilter(localTime.minusHours(1), localTime);
        AggregateRecordsRequest<Long> requestWithLocalTimeFilter =
                new AggregateRecordsRequest.Builder<Long>(localTimeFilter)
                        .addAggregationType(STEPS_COUNT_TOTAL)
                        .build();

        AggregateRecordsResponse<Long> aggregateResponse =
                getAggregateResponse(requestWithInstantFilter);
        assertThat(aggregateResponse.get(STEPS_COUNT_TOTAL)).isNull();
        assertThat(aggregateResponse.getDataOrigins(STEPS_COUNT_TOTAL)).isEmpty();

        List<AggregateRecordsGroupedByDurationResponse<Long>> groupedByDurationResponses =
                getAggregateResponseGroupByDuration(requestWithInstantFilter, Duration.ofDays(1));
        assertThat(groupedByDurationResponses).hasSize(1);
        assertThat(groupedByDurationResponses.get(0).get(STEPS_COUNT_TOTAL)).isNull();
        assertThat(groupedByDurationResponses.get(0).getDataOrigins(STEPS_COUNT_TOTAL)).isEmpty();

        List<AggregateRecordsGroupedByPeriodResponse<Long>> groupedByPeriodResponses =
                getAggregateResponseGroupByPeriod(requestWithLocalTimeFilter, Period.ofDays(1));
        assertThat(groupedByPeriodResponses).hasSize(1);
        assertThat(groupedByPeriodResponses.get(0).get(STEPS_COUNT_TOTAL)).isNull();
        assertThat(groupedByPeriodResponses.get(0).getDataOrigins(STEPS_COUNT_TOTAL)).isEmpty();
    }

    @Test
    public void timeRangeFilter_priorityType_outOfRangePriorityDataTrimmed() throws Exception {
        Instant startTime = Instant.now().minus(1, DAYS);
        Instant endTime = startTime.plus(1, HOURS);
        ZoneOffset dataZone = ZoneOffset.systemDefault().getRules().getOffset(startTime);
        LocalDateTime startLocalTime = startTime.atOffset(dataZone).toLocalDateTime();
        LocalDateTime endLocalTime = endTime.atOffset(dataZone).toLocalDateTime();
        mTestApp.insertRecord(
                getStepsRecord(1000, startTime.minusMillis(1000), startTime.plusMillis(1000)));
        insertRecords(
                List.of(getStepsRecord(10, endTime.minusMillis(1000), endTime.plusMillis(1000))));
        updatePriorityWithManageHealthDataPermission(
                ACTIVITY, ImmutableList.of(PKG_TEST_APP, mPackageName));

        AggregateRecordsRequest<Long> requestWithInstantFilter =
                new AggregateRecordsRequest.Builder<Long>(getTimeFilter(startTime, endTime))
                        .addAggregationType(STEPS_COUNT_TOTAL)
                        .build();

        AggregateRecordsResponse<Long> aggregateResponse =
                getAggregateResponse(requestWithInstantFilter);
        assertThat(aggregateResponse.get(STEPS_COUNT_TOTAL)).isEqualTo(505);
        assertThat(aggregateResponse.getDataOrigins(STEPS_COUNT_TOTAL))
                .containsExactly(getDataOrigin(mPackageName), getDataOrigin(PKG_TEST_APP));

        AggregateRecordsRequest<Long> requestWithLocalTimeFilter =
                new AggregateRecordsRequest.Builder<Long>(
                                getTimeFilter(startLocalTime, endLocalTime))
                        .addAggregationType(STEPS_COUNT_TOTAL)
                        .build();
        AggregateRecordsResponse<Long> aggregateResponse2 =
                getAggregateResponse(requestWithLocalTimeFilter);
        assertThat(aggregateResponse2.get(STEPS_COUNT_TOTAL)).isEqualTo(505);
        assertThat(aggregateResponse2.getDataOrigins(STEPS_COUNT_TOTAL))
                .containsExactly(getDataOrigin(mPackageName), getDataOrigin(PKG_TEST_APP));
    }

    @Test
    public void timeRangeFilter_nonPriorityType_outOfRangeNonPriorityDataExcluded()
            throws Exception {
        Instant startTime = Instant.now().minus(1, DAYS);
        Instant endTime = startTime.plus(1, HOURS);
        ZoneOffset dataZone = ZoneOffset.systemDefault().getRules().getOffset(startTime);
        LocalDateTime startLocalTime = startTime.atOffset(dataZone).toLocalDateTime();
        LocalDateTime endLocalTime = endTime.atOffset(dataZone).toLocalDateTime();
        mTestApp.insertRecord(getWeightRecord(70, startTime.minusMillis(1)));
        insertRecords(
                List.of(getWeightRecord(80, endTime), getWeightRecord(77, endTime.minusMillis(1))));

        AggregateRecordsRequest<Mass> requestWithInstantFilter =
                new AggregateRecordsRequest.Builder<Mass>(getTimeFilter(startTime, endTime))
                        .addAggregationType(WEIGHT_MAX)
                        .build();

        AggregateRecordsResponse<Mass> aggregateResponse =
                getAggregateResponse(requestWithInstantFilter);
        assertThat(aggregateResponse.get(WEIGHT_MAX)).isEqualTo(Mass.fromGrams(77));
        assertThat(aggregateResponse.getDataOrigins(WEIGHT_MAX))
                .containsExactly(getDataOrigin(mPackageName));

        AggregateRecordsRequest<Mass> requestWithLocalTimeFilter =
                new AggregateRecordsRequest.Builder<Mass>(
                                getTimeFilter(startLocalTime, endLocalTime))
                        .addAggregationType(WEIGHT_MIN)
                        .build();
        AggregateRecordsResponse<Mass> aggregateResponse2 =
                getAggregateResponse(requestWithLocalTimeFilter);
        assertThat(aggregateResponse2.get(WEIGHT_MIN)).isEqualTo(Mass.fromGrams(77));
        assertThat(aggregateResponse2.getDataOrigins(WEIGHT_MIN))
                .containsExactly(getDataOrigin(mPackageName));
    }

    @Test
    public void timeRangeFilter_nonPriorityType_expectPriorityListToBeIgnored() throws Exception {
        Instant startTime = Instant.now().minus(1, DAYS);
        Instant endTime = startTime.plus(1, HOURS);
        ZoneOffset dataZone = ZoneOffset.systemDefault().getRules().getOffset(startTime);
        LocalDateTime startLocalTime = startTime.atOffset(dataZone).toLocalDateTime();
        LocalDateTime endLocalTime = endTime.atOffset(dataZone).toLocalDateTime();
        mTestApp.insertRecord(getWeightRecord(70, startTime.plusMillis(1)));
        insertRecords(
                List.of(getWeightRecord(80, endTime), getWeightRecord(77, endTime.minusMillis(1))));

        AggregateRecordsRequest<Mass> requestWithInstantFilter =
                new AggregateRecordsRequest.Builder<Mass>(getTimeFilter(startTime, endTime))
                        .addAggregationType(WEIGHT_MAX)
                        .build();

        AggregateRecordsResponse<Mass> aggregateResponse =
                getAggregateResponse(requestWithInstantFilter);
        assertThat(aggregateResponse.get(WEIGHT_MAX)).isEqualTo(Mass.fromGrams(77));
        assertThat(aggregateResponse.getDataOrigins(WEIGHT_MAX))
                .containsExactly(getDataOrigin(mPackageName), getDataOrigin(PKG_TEST_APP));

        AggregateRecordsRequest<Mass> requestWithLocalTimeFilter =
                new AggregateRecordsRequest.Builder<Mass>(
                                getTimeFilter(startLocalTime, endLocalTime))
                        .addAggregationType(WEIGHT_MIN)
                        .build();
        AggregateRecordsResponse<Mass> aggregateResponse2 =
                getAggregateResponse(requestWithLocalTimeFilter);
        assertThat(aggregateResponse2.get(WEIGHT_MIN)).isEqualTo(Mass.fromGrams(70));
        assertThat(aggregateResponse2.getDataOrigins(WEIGHT_MIN))
                .containsExactly(getDataOrigin(mPackageName), getDataOrigin(PKG_TEST_APP));
    }

    @Test
    public void timeRangeFilter_priorityType_ifPriorityListHasMultipleItemsExpectMultipleSources()
            throws Exception {
        Instant startTime = Instant.now().minus(1, DAYS);
        Instant endTime = startTime.plus(1, HOURS);
        ZoneOffset dataZone = ZoneOffset.systemDefault().getRules().getOffset(startTime);
        LocalDateTime startLocalTime = startTime.atOffset(dataZone).toLocalDateTime();
        LocalDateTime endLocalTime = endTime.atOffset(dataZone).toLocalDateTime();
        mTestApp.insertRecord(
                getStepsRecord(70, startTime.plusMillis(1), startTime.plusMillis(400)));
        insertRecords(
                List.of(
                        // fully in range
                        getStepsRecord(123, Instant.EPOCH, Instant.ofEpochSecond(456)),
                        // partially in range (29/30)
                        getStepsRecord(30000, endTime.minus(29, HOURS), endTime.plus(1, HOURS))));
        updatePriorityWithManageHealthDataPermission(
                ACTIVITY, ImmutableList.of(PKG_TEST_APP, mPackageName));

        AggregateRecordsRequest<Long> requestWithInstantFilter =
                new AggregateRecordsRequest.Builder<Long>(getTimeFilter(startTime, endTime))
                        .addAggregationType(STEPS_COUNT_TOTAL)
                        .build();

        AggregateRecordsResponse<Long> aggregateResponse =
                getAggregateResponse(requestWithInstantFilter);
        assertThat(aggregateResponse.get(STEPS_COUNT_TOTAL)).isEqualTo(1069);
        assertThat(aggregateResponse.getDataOrigins(STEPS_COUNT_TOTAL))
                .containsExactly(getDataOrigin(mPackageName), getDataOrigin(PKG_TEST_APP));

        AggregateRecordsRequest<Long> requestWithLocalTimeFilter =
                new AggregateRecordsRequest.Builder<Long>(
                                getTimeFilter(startLocalTime, endLocalTime))
                        .addAggregationType(STEPS_COUNT_TOTAL)
                        .build();
        AggregateRecordsResponse<Long> aggregateResponse2 =
                getAggregateResponse(requestWithLocalTimeFilter);
        assertThat(aggregateResponse2.get(STEPS_COUNT_TOTAL)).isEqualTo(1069);
        assertThat(aggregateResponse2.getDataOrigins(STEPS_COUNT_TOTAL))
                .containsExactly(getDataOrigin(mPackageName), getDataOrigin(PKG_TEST_APP));
    }

    @Test
    public void timeRangeFilter_priorityType_ifPriorityListHasNoItemsExpectEmptyDataOrigins()
            throws Exception {
        Instant startTime = Instant.now().minus(1, DAYS);
        Instant endTime = startTime.plus(1, HOURS);
        mTestApp.insertRecord(
                getStepsRecord(70, startTime.plusMillis(1), startTime.plusMillis(400)));
        insertRecords(
                List.of(
                        // fully in range
                        getStepsRecord(123, Instant.EPOCH, Instant.ofEpochSecond(456)),
                        // partially in range (29/30)
                        getStepsRecord(30000, endTime.minus(29, HOURS), endTime.plus(1, HOURS))));
        updatePriorityWithManageHealthDataPermission(ACTIVITY, ImmutableList.of());

        AggregateRecordsRequest<Long> requestWithInstantFilter =
                new AggregateRecordsRequest.Builder<Long>(getTimeFilter(startTime, endTime))
                        .addAggregationType(STEPS_COUNT_TOTAL)
                        .build();

        AggregateRecordsResponse<Long> aggregateResponse =
                getAggregateResponse(requestWithInstantFilter);
        assertThat(aggregateResponse.get(STEPS_COUNT_TOTAL)).isEqualTo(null);
        assertThat(aggregateResponse.getDataOrigins(STEPS_COUNT_TOTAL)).isEmpty();

        AggregateRecordsRequest<Long> requestWithLocalTimeFilter =
                new AggregateRecordsRequest.Builder<Long>(getTimeFilter(startTime, endTime))
                        .addAggregationType(STEPS_COUNT_TOTAL)
                        .build();
        AggregateRecordsResponse<Long> aggregateResponse2 =
                getAggregateResponse(requestWithLocalTimeFilter);
        assertThat(aggregateResponse2.get(STEPS_COUNT_TOTAL)).isEqualTo(null);
        assertThat(aggregateResponse2.getDataOrigins(STEPS_COUNT_TOTAL)).isEmpty();
    }

    @Test
    public void timeRangeFilter_openStartTime_aggregateDataFromEpoch() throws Exception {
        Instant filterEndTime = Instant.now().minus(1, DAYS);
        ZoneOffset dataZone = ZoneOffset.systemDefault().getRules().getOffset(filterEndTime);
        LocalDateTime endLocalTime = filterEndTime.atOffset(dataZone).toLocalDateTime();
        insertRecords(
                List.of(
                        // fully in range
                        getStepsRecord(123, Instant.EPOCH, Instant.ofEpochSecond(456)),
                        // partially in range (29/30)
                        getStepsRecord(
                                30000,
                                filterEndTime.minus(29, HOURS),
                                filterEndTime.plus(1, HOURS))));

        AggregateRecordsRequest<Long> requestWithInstantFilter =
                new AggregateRecordsRequest.Builder<Long>(getOpenStartTimeFilter(filterEndTime))
                        .addAggregationType(STEPS_COUNT_TOTAL)
                        .build();
        AggregateRecordsResponse<Long> aggregateResponse =
                getAggregateResponse(requestWithInstantFilter);
        assertThat(aggregateResponse.get(STEPS_COUNT_TOTAL)).isEqualTo(29123);

        AggregateRecordsRequest<Long> requestWithLocalFilter =
                new AggregateRecordsRequest.Builder<Long>(getOpenStartTimeFilter(endLocalTime))
                        .addAggregationType(STEPS_COUNT_TOTAL)
                        .build();
        AggregateRecordsResponse<Long> aggregateResponse2 =
                getAggregateResponse(requestWithLocalFilter);
        assertThat(aggregateResponse2.get(STEPS_COUNT_TOTAL)).isEqualTo(29123);
    }

    @Test
    public void timeRangeFilter_openEndTime_aggregateDataUntilFuture() throws Exception {
        Instant now = Instant.now();
        Instant filterStartTime = Instant.now().minus(1, DAYS);
        ZoneOffset dataZone = ZoneOffset.systemDefault().getRules().getOffset(filterStartTime);
        LocalDateTime startLocalTime = filterStartTime.atOffset(dataZone).toLocalDateTime();
        insertRecords(
                List.of(
                        getStepsRecord(666, now.minusMillis(1000), now.plusMillis(1000)),
                        getStepsRecord(
                                2000,
                                filterStartTime.minusMillis(1000),
                                filterStartTime.plusMillis(1000))));

        AggregateRecordsRequest<Long> requestWithInstantFilter =
                new AggregateRecordsRequest.Builder<Long>(getOpenEndTimeFilter(filterStartTime))
                        .addAggregationType(STEPS_COUNT_TOTAL)
                        .build();
        AggregateRecordsResponse<Long> aggregateResponse =
                getAggregateResponse(requestWithInstantFilter);
        assertThat(aggregateResponse.get(STEPS_COUNT_TOTAL)).isEqualTo(1666);

        AggregateRecordsRequest<Long> requestWithLocalFilter =
                new AggregateRecordsRequest.Builder<Long>(getOpenEndTimeFilter(startLocalTime))
                        .addAggregationType(STEPS_COUNT_TOTAL)
                        .build();
        AggregateRecordsResponse<Long> aggregateResponse2 =
                getAggregateResponse(requestWithLocalFilter);
        assertThat(aggregateResponse2.get(STEPS_COUNT_TOTAL)).isEqualTo(1666);
    }

    @Test
    public void aggregationTypeFilter_multipleTypesForDifferentRecordTypes_correctResults()
            throws Exception {
        Instant startTime = Instant.now().minus(1, DAYS);
        ImmutableList.Builder<HeartRateRecord.HeartRateSample> inRangeSamples =
                new ImmutableList.Builder<>();
        inRangeSamples.add(new HeartRateRecord.HeartRateSample(123, startTime.plusMillis(1000)));
        inRangeSamples.add(new HeartRateRecord.HeartRateSample(80, startTime.plusMillis(2000)));
        inRangeSamples.add(new HeartRateRecord.HeartRateSample(75, startTime.plusMillis(3000)));
        inRangeSamples.add(new HeartRateRecord.HeartRateSample(96, startTime.plusMillis(4000)));
        ImmutableList.Builder<HeartRateRecord.HeartRateSample> outOfRangeSamples =
                new ImmutableList.Builder<>();
        outOfRangeSamples.add(new HeartRateRecord.HeartRateSample(55, startTime.plusMillis(6000)));
        outOfRangeSamples.add(new HeartRateRecord.HeartRateSample(147, startTime.plusMillis(7000)));
        insertRecords(
                List.of(
                        getStepsRecord(43, startTime, startTime.plusMillis(1000)),
                        getStepsRecord(76, startTime.plusMillis(3000), startTime.plusMillis(4000)),
                        // out of query interval
                        getStepsRecord(76, startTime.plusMillis(8000), startTime.plusMillis(9000)),
                        getHeartRateRecord(
                                inRangeSamples.build(), startTime, startTime.plusMillis(5000)),
                        // out of query interval
                        getHeartRateRecord(
                                outOfRangeSamples.build(),
                                startTime.plusMillis(6000),
                                startTime.plusMillis(8000))));

        AggregateRecordsRequest<Long> request =
                new AggregateRecordsRequest.Builder<Long>(
                                getTimeFilter(startTime, startTime.plusMillis(6000)))
                        .addAggregationType(STEPS_COUNT_TOTAL)
                        .addAggregationType(HEART_MEASUREMENTS_COUNT)
                        .addAggregationType(BPM_MAX)
                        .addAggregationType(BPM_AVG)
                        .addAggregationType(BPM_MIN)
                        .build();
        AggregateRecordsResponse<Long> response = getAggregateResponse(request);
        assertThat(response.get(STEPS_COUNT_TOTAL)).isEqualTo(119); // 43 + 76
        assertThat(response.get(HEART_MEASUREMENTS_COUNT)).isEqualTo(inRangeSamples.build().size());
        assertThat(response.get(BPM_MAX)).isEqualTo(123);
        assertThat(response.get(BPM_AVG)).isEqualTo(93); // avg(123, 80, 75, 96)
        assertThat(response.get(BPM_MIN)).isEqualTo(75);
    }

    @Test
    public void aggregationTypeFilter_noDataForFilteredType_noDataReturned() throws Exception {
        Instant startTime = Instant.now().minus(1, DAYS);
        LocalDateTime localTime = startTime.atOffset(mCurrentZone).toLocalDateTime();
        insertRecords(List.of(getDistanceRecord(123.4, startTime, startTime.plusMillis(1000))));

        TimeInstantRangeFilter instantFilter =
                getTimeFilter(startTime.minus(1, HOURS), startTime.plus(1, HOURS));
        AggregateRecordsRequest<Length> requestWithInstantFilter =
                new AggregateRecordsRequest.Builder<Length>(instantFilter)
                        .addAggregationType(ELEVATION_GAINED_TOTAL)
                        .build();
        LocalTimeRangeFilter localTimeFilter =
                getTimeFilter(localTime.minusHours(1), localTime.plusHours(1));
        AggregateRecordsRequest<Length> requestWithLocalTimeFilter =
                new AggregateRecordsRequest.Builder<Length>(localTimeFilter)
                        .addAggregationType(ELEVATION_GAINED_TOTAL)
                        .build();

        AggregateRecordsResponse<Length> aggregateResponse =
                getAggregateResponse(requestWithInstantFilter);
        assertThat(aggregateResponse.get(ELEVATION_GAINED_TOTAL)).isNull();
        assertThat(aggregateResponse.getDataOrigins(ELEVATION_GAINED_TOTAL)).isEmpty();

        List<AggregateRecordsGroupedByDurationResponse<Length>> groupedByDurationResponses =
                getAggregateResponseGroupByDuration(requestWithInstantFilter, Duration.ofDays(1));
        assertThat(groupedByDurationResponses).hasSize(1);
        assertThat(groupedByDurationResponses.get(0).get(ELEVATION_GAINED_TOTAL)).isNull();
        assertThat(groupedByDurationResponses.get(0).getDataOrigins(ELEVATION_GAINED_TOTAL))
                .isEmpty();

        List<AggregateRecordsGroupedByPeriodResponse<Length>> groupedByPeriodResponses =
                getAggregateResponseGroupByPeriod(requestWithLocalTimeFilter, Period.ofDays(1));
        assertThat(groupedByPeriodResponses).hasSize(1);
        assertThat(groupedByPeriodResponses.get(0).get(ELEVATION_GAINED_TOTAL)).isNull();
        assertThat(groupedByPeriodResponses.get(0).getDataOrigins(ELEVATION_GAINED_TOTAL))
                .isEmpty();
    }
}
