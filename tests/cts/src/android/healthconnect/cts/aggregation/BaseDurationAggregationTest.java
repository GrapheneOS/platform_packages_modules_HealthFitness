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

import static android.healthconnect.cts.lib.RecordFactory.MIDNIGHT_ONE_WEEK_AGO;
import static android.healthconnect.cts.lib.RecordFactory.YESTERDAY_10AM_LOCAL;
import static android.healthconnect.cts.lib.RecordFactory.YESTERDAY_11AM;
import static android.healthconnect.cts.utils.TestUtils.getAggregateResponse;
import static android.healthconnect.cts.utils.TestUtils.getAggregateResponseGroupByDuration;
import static android.healthconnect.cts.utils.TestUtils.insertRecord;
import static android.healthconnect.cts.utils.TestUtils.insertRecords;
import static android.healthconnect.cts.utils.TestUtils.setupAggregation;

import static com.google.common.truth.Truth.assertThat;

import static java.time.Duration.ofHours;
import static java.time.Duration.ofMinutes;

import android.health.connect.AggregateRecordsGroupedByDurationResponse;
import android.health.connect.AggregateRecordsGroupedByPeriodResponse;
import android.health.connect.AggregateRecordsRequest;
import android.health.connect.AggregateRecordsResponse;
import android.health.connect.LocalTimeRangeFilter;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.datatypes.AggregationType;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.IntervalRecord;
import android.healthconnect.cts.lib.TestAppProxy;
import android.healthconnect.cts.utils.AssumptionCheckerRule;
import android.healthconnect.cts.utils.DeviceSupportUtils;
import android.healthconnect.cts.utils.TestUtils;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;

import com.android.compatibility.common.util.ApiTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

@ApiTest(
        apis = {
            "android.health.connect.HealthConnectManager#aggregate",
            "android.health.connect.HealthConnectManager#aggregateGroupByDuration",
            "android.health.connect.HealthConnectManager#aggregateGroupByPeriod"
        })
abstract class BaseDurationAggregationTest<RecordType extends IntervalRecord, ResultType> {
    static final String TEST_PACKAGE_NAME = getTestPackageName();

    static final TestAppProxy APP_WITH_WRITE_PERMS_ONLY =
            TestAppProxy.forPackageName("android.healthconnect.cts.testapp.writePermsOnly");

    private final AggregationType<ResultType> mAggregationType;
    private final int mHealthDataCategory;

    BaseDurationAggregationTest(
            AggregationType<ResultType> aggregationType, int healthDataCategory) {
        mAggregationType = aggregationType;
        mHealthDataCategory = healthDataCategory;
    }

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Rule
    public AssumptionCheckerRule mSupportedHardwareRule =
            new AssumptionCheckerRule(
                    DeviceSupportUtils::isHealthConnectFullySupported,
                    "Tests should run on supported hardware only.");

    abstract ResultType getExpectedValueFromDuration(Duration duration);

    abstract RecordType createRecord(
            Instant startTime,
            Instant endTime,
            @Nullable ZoneOffset startZoneOffset,
            @Nullable ZoneOffset endZoneOffset);

    final RecordType createRecord(Instant startTime, Instant endTime) {
        return createRecord(startTime, endTime, null, null);
    }

    @Before
    public void setUp() throws Exception {
        TestUtils.deleteAllStagedRemoteData();
    }

    @After
    public void tearDown() throws Exception {
        TestUtils.deleteAllStagedRemoteData();
    }

    @Test
    public void noData_largeWindow_returnsNull() throws Exception {
        setupAggregation(TEST_PACKAGE_NAME, mHealthDataCategory);

        AggregateRecordsResponse<ResultType> response =
                getAggregateResponse(
                        new AggregateRecordsRequest.Builder<ResultType>(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(Instant.EPOCH)
                                                .setEndTime(Instant.now())
                                                .build())
                                .addAggregationType(mAggregationType)
                                .build());

        assertThat(response.get(mAggregationType)).isNull();
        assertThat(response.getZoneOffset(mAggregationType)).isNull();
        assertThat(response.getDataOrigins(mAggregationType)).isEmpty();
    }

    @Test
    public void oneRecord_largeWindow_returnsRecordDuration() throws Exception {
        Instant startTime = YESTERDAY_11AM.toInstant();
        Instant endTime = YESTERDAY_11AM.plusMinutes(30).toInstant();
        RecordType record = createRecord(startTime, endTime);
        insertRecord(record);
        setupAggregation(List.of(TEST_PACKAGE_NAME), mHealthDataCategory);

        AggregateRecordsResponse<ResultType> response =
                getAggregateResponse(
                        new AggregateRecordsRequest.Builder<ResultType>(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(Instant.EPOCH)
                                                .setEndTime(Instant.now())
                                                .build())
                                .addAggregationType(mAggregationType)
                                .build());

        assertThat(response.get(mAggregationType)).isNotNull();
        assertThat(response.get(mAggregationType))
                .isEqualTo(getExpectedValueFromDuration(getRecordDuration(record)));
        assertThat(response.getZoneOffset(mAggregationType)).isEqualTo(record.getStartZoneOffset());
        assertThat(response.getDataOrigins(mAggregationType))
                .containsExactly(
                        new DataOrigin.Builder().setPackageName(TEST_PACKAGE_NAME).build());
    }

    @Test
    public void oneRecord_startsBeforeWindowStart_endsOnWindowEnd_returnsOverlapDuration()
            throws Exception {
        Instant startTime = YESTERDAY_11AM.toInstant();
        Instant endTime = YESTERDAY_11AM.plusMinutes(30).toInstant();
        RecordType record = createRecord(startTime, endTime);
        insertRecord(record);
        setupAggregation(List.of(TEST_PACKAGE_NAME), mHealthDataCategory);

        AggregateRecordsResponse<ResultType> response =
                getAggregateResponse(
                        new AggregateRecordsRequest.Builder<ResultType>(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(
                                                        record.getStartTime().plus(ofMinutes(5)))
                                                .setEndTime(record.getEndTime())
                                                .build())
                                .addAggregationType(mAggregationType)
                                .build());

        assertThat(response.get(mAggregationType)).isNotNull();
        assertThat(response.get(mAggregationType))
                .isEqualTo(getExpectedValueFromDuration(ofMinutes(25)));
        assertThat(response.getZoneOffset(mAggregationType)).isEqualTo(record.getStartZoneOffset());
    }

    @Test
    public void oneRecord_startsOnWindowStart_endsAfterWindowEnd_returnsOverlapDuration()
            throws Exception {
        Instant startTime = YESTERDAY_11AM.toInstant();
        Instant endTime = YESTERDAY_11AM.plusMinutes(30).toInstant();
        RecordType record = createRecord(startTime, endTime);
        insertRecord(record);
        setupAggregation(List.of(TEST_PACKAGE_NAME), mHealthDataCategory);

        AggregateRecordsResponse<ResultType> response =
                getAggregateResponse(
                        new AggregateRecordsRequest.Builder<ResultType>(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(record.getStartTime())
                                                .setEndTime(record.getEndTime().minus(ofMinutes(3)))
                                                .build())
                                .addAggregationType(mAggregationType)
                                .build());

        assertThat(response.get(mAggregationType)).isNotNull();
        assertThat(response.get(mAggregationType))
                .isEqualTo(getExpectedValueFromDuration(ofMinutes(27)));
        assertThat(response.getZoneOffset(mAggregationType)).isEqualTo(record.getStartZoneOffset());
    }

    @Test
    public void oneRecord_startsBeforeWindowStart_endsAfterWindowEnd_returnsOverlapDuration()
            throws Exception {

        Instant startTime = YESTERDAY_11AM.toInstant();
        Instant endTime = YESTERDAY_11AM.plusMinutes(30).toInstant();
        RecordType record = createRecord(startTime, endTime);
        insertRecord(record);
        setupAggregation(List.of(TEST_PACKAGE_NAME), mHealthDataCategory);

        AggregateRecordsResponse<ResultType> response =
                getAggregateResponse(
                        new AggregateRecordsRequest.Builder<ResultType>(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(
                                                        record.getStartTime().plus(ofMinutes(6)))
                                                .setEndTime(record.getEndTime().minus(ofMinutes(3)))
                                                .build())
                                .addAggregationType(mAggregationType)
                                .build());

        assertThat(response.get(mAggregationType)).isNotNull();
        assertThat(response.get(mAggregationType))
                .isEqualTo(getExpectedValueFromDuration(ofMinutes(21)));
        assertThat(response.getZoneOffset(mAggregationType)).isEqualTo(record.getStartZoneOffset());
    }

    @Test
    public void oneRecord_startsOnWindowEnd_returnsNull() throws Exception {
        Instant startTime = YESTERDAY_11AM.toInstant();
        Instant endTime = YESTERDAY_11AM.plusMinutes(30).toInstant();
        RecordType record = createRecord(startTime, endTime);
        insertRecord(record);
        setupAggregation(List.of(TEST_PACKAGE_NAME), mHealthDataCategory);

        AggregateRecordsResponse<ResultType> response =
                getAggregateResponse(
                        new AggregateRecordsRequest.Builder<ResultType>(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(Instant.EPOCH)
                                                .setEndTime(record.getStartTime())
                                                .build())
                                .addAggregationType(mAggregationType)
                                .build());

        assertThat(response.get(mAggregationType)).isNull();
    }

    @Test
    public void oneRecord_endsOnWindowStart_returnsNull() throws Exception {
        Instant startTime = YESTERDAY_11AM.toInstant();
        Instant endTime = YESTERDAY_11AM.plusMinutes(30).toInstant();
        RecordType record = createRecord(startTime, endTime);
        insertRecord(record);
        setupAggregation(List.of(TEST_PACKAGE_NAME), mHealthDataCategory);

        AggregateRecordsResponse<ResultType> response =
                getAggregateResponse(
                        new AggregateRecordsRequest.Builder<ResultType>(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(record.getEndTime())
                                                .setEndTime(Instant.now())
                                                .build())
                                .addAggregationType(mAggregationType)
                                .build());

        assertThat(response.get(mAggregationType)).isNull();
    }

    @Test
    public void aggregate_multipleOverlappingRecords_takesOverlapsIntoAccount() throws Exception {
        Instant startTime = YESTERDAY_11AM.plusHours(1).plusMinutes(10).toInstant();
        Instant endTime = YESTERDAY_11AM.plusHours(1).plusMinutes(35).toInstant();
        Instant startTime1 = YESTERDAY_11AM.plusMinutes(40).toInstant();
        Instant endTime1 = YESTERDAY_11AM.plusMinutes(50).toInstant();
        Instant startTime2 = YESTERDAY_11AM.plusMinutes(30).toInstant();
        Instant endTime2 = YESTERDAY_11AM.plusHours(1).plusMinutes(20).toInstant();
        Instant startTime3 = YESTERDAY_11AM.toInstant();
        Instant endTime3 = YESTERDAY_11AM.plusHours(1).toInstant();
        List<RecordType> records =
                List.of(
                        createRecord(startTime3, endTime3),
                        createRecord(startTime2, endTime2),
                        createRecord(startTime1, endTime1),
                        createRecord(startTime, endTime));
        insertRecords(records);
        setupAggregation(List.of(TEST_PACKAGE_NAME), mHealthDataCategory);

        AggregateRecordsResponse<ResultType> response =
                getAggregateResponse(
                        new AggregateRecordsRequest.Builder<ResultType>(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(Instant.EPOCH)
                                                .setEndTime(records.get(3).getEndTime())
                                                .build())
                                .addAggregationType(mAggregationType)
                                .build());

        assertThat(response.get(mAggregationType))
                .isEqualTo(
                        getExpectedValueFromDuration(
                                Duration.between(
                                        records.get(0).getStartTime(),
                                        records.get(3).getEndTime())));
    }

    @Test
    public void aggregate_multipleNotOverlappingRecords_returnsSumOfDurations() throws Exception {
        Instant startTime = YESTERDAY_11AM.plusMinutes(40).toInstant();
        Instant endTime = YESTERDAY_11AM.plusMinutes(51).toInstant();
        Instant startTime1 = YESTERDAY_11AM.plusMinutes(20).toInstant();
        Instant endTime1 = YESTERDAY_11AM.plusMinutes(34).toInstant();
        Instant startTime2 = YESTERDAY_11AM.toInstant();
        Instant endTime2 = YESTERDAY_11AM.plusMinutes(17).toInstant();
        List<RecordType> records =
                List.of(
                        createRecord(startTime2, endTime2),
                        createRecord(startTime1, endTime1),
                        createRecord(startTime, endTime));
        insertRecords(records);
        setupAggregation(List.of(TEST_PACKAGE_NAME), mHealthDataCategory);

        AggregateRecordsResponse<ResultType> response =
                getAggregateResponse(
                        new AggregateRecordsRequest.Builder<ResultType>(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(Instant.EPOCH)
                                                .setEndTime(records.get(2).getEndTime())
                                                .build())
                                .addAggregationType(mAggregationType)
                                .build());

        assertThat(response.get(mAggregationType))
                .isEqualTo(
                        getExpectedValueFromDuration(
                                getRecordDuration(records.get(0))
                                        .plus(getRecordDuration(records.get(1)))
                                        .plus(getRecordDuration(records.get(2)))));
    }

    @Test
    public void aggregate_localTimeFilter_recordsEqualsToWindow_returnsDuration() throws Exception {
        ZoneOffset startZoneOffset = ZoneOffset.ofHours(1);
        ZoneOffset endZoneOffset = ZoneOffset.ofHours(1);
        RecordType record =
                createRecord(
                        YESTERDAY_10AM_LOCAL.toInstant(startZoneOffset),
                        YESTERDAY_10AM_LOCAL.plusMinutes(23).toInstant(endZoneOffset),
                        startZoneOffset,
                        endZoneOffset);
        insertRecord(record);
        setupAggregation(List.of(TEST_PACKAGE_NAME), mHealthDataCategory);

        AggregateRecordsResponse<ResultType> response =
                getAggregateResponse(
                        new AggregateRecordsRequest.Builder<ResultType>(
                                        new LocalTimeRangeFilter.Builder()
                                                .setStartTime(YESTERDAY_10AM_LOCAL)
                                                .setEndTime(YESTERDAY_10AM_LOCAL.plusHours(1))
                                                .build())
                                .addAggregationType(mAggregationType)
                                .build());

        assertThat(response.get(mAggregationType))
                .isEqualTo(getExpectedValueFromDuration(ofMinutes(23)));
        assertThat(response.getZoneOffset(mAggregationType)).isEqualTo(startZoneOffset);
    }

    @Test
    public void aggregate_localTimeFilter_minMaxZoneOffsets() throws Exception {
        ZoneOffset startZoneOffset = ZoneOffset.MAX;
        ZoneOffset endZoneOffset = ZoneOffset.MIN;
        RecordType record =
                createRecord(
                        YESTERDAY_10AM_LOCAL.toInstant(startZoneOffset),
                        YESTERDAY_10AM_LOCAL.plusMinutes(37).toInstant(endZoneOffset),
                        startZoneOffset,
                        endZoneOffset);
        insertRecord(record);
        setupAggregation(List.of(TEST_PACKAGE_NAME), mHealthDataCategory);

        AggregateRecordsResponse<ResultType> response =
                getAggregateResponse(
                        new AggregateRecordsRequest.Builder<ResultType>(
                                        new LocalTimeRangeFilter.Builder()
                                                .setStartTime(YESTERDAY_10AM_LOCAL)
                                                .setEndTime(YESTERDAY_10AM_LOCAL.plusHours(1))
                                                .build())
                                .addAggregationType(mAggregationType)
                                .build());

        assertThat(response.get(mAggregationType))
                .isEqualTo(getExpectedValueFromDuration(ofMinutes(37)));
        assertThat(response.getZoneOffset(mAggregationType)).isEqualTo(startZoneOffset);
    }

    @Test
    public void aggregate_localTimeFilter_startOffsetGreaterThanEndOffset() throws Exception {
        ZoneOffset startZoneOffset = ZoneOffset.ofHours(4);
        ZoneOffset endZoneOffset = ZoneOffset.ofHours(2);
        RecordType record =
                createRecord(
                        YESTERDAY_10AM_LOCAL.toInstant(startZoneOffset),
                        YESTERDAY_10AM_LOCAL.plusMinutes(37).toInstant(endZoneOffset),
                        startZoneOffset,
                        endZoneOffset);
        insertRecord(record);
        setupAggregation(List.of(TEST_PACKAGE_NAME), mHealthDataCategory);

        AggregateRecordsResponse<ResultType> response =
                getAggregateResponse(
                        new AggregateRecordsRequest.Builder<ResultType>(
                                        new LocalTimeRangeFilter.Builder()
                                                .setStartTime(YESTERDAY_10AM_LOCAL)
                                                .setEndTime(YESTERDAY_10AM_LOCAL.plusHours(1))
                                                .build())
                                .addAggregationType(mAggregationType)
                                .build());

        assertThat(response.get(mAggregationType))
                .isEqualTo(getExpectedValueFromDuration(ofMinutes(37)));
        assertThat(response.getZoneOffset(mAggregationType)).isEqualTo(startZoneOffset);
    }

    @Test
    public void aggregate_localTimeFilter_startOffsetLessThanEndOffset() throws Exception {
        ZoneOffset startZoneOffset = ZoneOffset.ofHours(-2);
        ZoneOffset endZoneOffset = ZoneOffset.ofHours(-1);
        RecordType record =
                createRecord(
                        YESTERDAY_10AM_LOCAL.toInstant(startZoneOffset),
                        YESTERDAY_10AM_LOCAL.plusHours(2).plusMinutes(37).toInstant(endZoneOffset),
                        startZoneOffset,
                        endZoneOffset);
        insertRecord(record);
        setupAggregation(List.of(TEST_PACKAGE_NAME), mHealthDataCategory);

        AggregateRecordsResponse<ResultType> response =
                getAggregateResponse(
                        new AggregateRecordsRequest.Builder<ResultType>(
                                        new LocalTimeRangeFilter.Builder()
                                                .setStartTime(YESTERDAY_10AM_LOCAL.minusDays(1))
                                                .setEndTime(YESTERDAY_10AM_LOCAL.plusDays(1))
                                                .build())
                                .addAggregationType(mAggregationType)
                                .build());

        assertThat(response.get(mAggregationType))
                .isEqualTo(getExpectedValueFromDuration(ofHours(2).plus(ofMinutes(37))));
        assertThat(response.getZoneOffset(mAggregationType)).isEqualTo(startZoneOffset);
    }

    @Test
    public void aggregate_localTimeFilter_recordEndsBeforeWindow_returnsNull() throws Exception {
        ZoneOffset startZoneOffset = ZoneOffset.ofHours(4);
        ZoneOffset endZoneOffset = ZoneOffset.ofHours(2);
        RecordType record =
                createRecord(
                        YESTERDAY_10AM_LOCAL.toInstant(startZoneOffset),
                        YESTERDAY_10AM_LOCAL.plusHours(1).toInstant(endZoneOffset),
                        startZoneOffset,
                        endZoneOffset);
        insertRecord(record);
        setupAggregation(List.of(TEST_PACKAGE_NAME), mHealthDataCategory);

        AggregateRecordsResponse<ResultType> response =
                getAggregateResponse(
                        new AggregateRecordsRequest.Builder<ResultType>(
                                        new LocalTimeRangeFilter.Builder()
                                                .setStartTime(YESTERDAY_10AM_LOCAL.plusHours(1))
                                                .setEndTime(YESTERDAY_10AM_LOCAL.plusDays(10))
                                                .build())
                                .addAggregationType(mAggregationType)
                                .build());

        assertThat(response.get(mAggregationType)).isNull();
        assertThat(response.getZoneOffset(mAggregationType)).isNull();
    }

    @Test
    public void aggregate_localTimeFilter_recordStartsAfterWindow_returnsNull() throws Exception {
        ZoneOffset startZoneOffset = ZoneOffset.ofHours(4);
        ZoneOffset endZoneOffset = ZoneOffset.ofHours(2);
        RecordType record =
                createRecord(
                        YESTERDAY_10AM_LOCAL.toInstant(startZoneOffset),
                        YESTERDAY_10AM_LOCAL.plusHours(1).toInstant(endZoneOffset),
                        startZoneOffset,
                        endZoneOffset);
        insertRecord(record);
        setupAggregation(List.of(TEST_PACKAGE_NAME), mHealthDataCategory);

        AggregateRecordsResponse<ResultType> response =
                getAggregateResponse(
                        new AggregateRecordsRequest.Builder<ResultType>(
                                        new LocalTimeRangeFilter.Builder()
                                                .setStartTime(YESTERDAY_10AM_LOCAL.minusDays(10))
                                                .setEndTime(YESTERDAY_10AM_LOCAL)
                                                .build())
                                .addAggregationType(mAggregationType)
                                .build());

        assertThat(response.get(mAggregationType)).isNull();
        assertThat(response.getZoneOffset(mAggregationType)).isNull();
    }

    @Test
    public void aggregate_localTimeFilter_recordOverlapsWindow_returnsOverlapDuration()
            throws Exception {
        ZoneOffset startZoneOffset = ZoneOffset.ofHours(4);
        ZoneOffset endZoneOffset = ZoneOffset.ofHours(2);
        RecordType record =
                createRecord(
                        YESTERDAY_10AM_LOCAL.toInstant(startZoneOffset),
                        YESTERDAY_10AM_LOCAL.plusHours(1).toInstant(endZoneOffset),
                        startZoneOffset,
                        endZoneOffset);
        insertRecord(record);
        setupAggregation(List.of(TEST_PACKAGE_NAME), mHealthDataCategory);

        AggregateRecordsResponse<ResultType> response =
                getAggregateResponse(
                        new AggregateRecordsRequest.Builder<ResultType>(
                                        new LocalTimeRangeFilter.Builder()
                                                .setStartTime(YESTERDAY_10AM_LOCAL.plusMinutes(47))
                                                .setEndTime(YESTERDAY_10AM_LOCAL.plusHours(5))
                                                .build())
                                .addAggregationType(mAggregationType)
                                .build());

        assertThat(response.get(mAggregationType))
                .isEqualTo(getExpectedValueFromDuration(ofMinutes(13)));
        assertThat(response.getZoneOffset(mAggregationType)).isEqualTo(startZoneOffset);
    }

    @Test
    public void aggregate_localTimeFilter_overlappingRecordsWithDifferentOffsets()
            throws Exception {
        List<RecordType> records =
                List.of(
                        createRecord(
                                YESTERDAY_10AM_LOCAL.toInstant(ZoneOffset.ofHours(2)),
                                YESTERDAY_10AM_LOCAL
                                        .plusMinutes(52)
                                        .toInstant(ZoneOffset.ofHours(1)),
                                ZoneOffset.ofHours(2),
                                ZoneOffset.ofHours(1)),
                        createRecord(
                                YESTERDAY_10AM_LOCAL
                                        .plusMinutes(35)
                                        .toInstant(ZoneOffset.ofHours(4)),
                                YESTERDAY_10AM_LOCAL.plusHours(1).toInstant(ZoneOffset.ofHours(3)),
                                ZoneOffset.ofHours(4),
                                ZoneOffset.ofHours(3)));

        insertRecords(records);
        setupAggregation(List.of(TEST_PACKAGE_NAME), mHealthDataCategory);

        AggregateRecordsResponse<ResultType> response =
                getAggregateResponse(
                        new AggregateRecordsRequest.Builder<ResultType>(
                                        new LocalTimeRangeFilter.Builder()
                                                .setStartTime(YESTERDAY_10AM_LOCAL.minusDays(1))
                                                .setEndTime(YESTERDAY_10AM_LOCAL.plusDays(1))
                                                .build())
                                .addAggregationType(mAggregationType)
                                .build());
        assertThat(response.get(mAggregationType))
                .isEqualTo(getExpectedValueFromDuration(ofHours(1)));
        assertThat(response.getZoneOffset(mAggregationType)).isEqualTo(ZoneOffset.ofHours(2));
    }

    @Test
    public void aggregateGroupByDuration_multipleOverlappingRecords() throws Exception {
        Instant startTime = YESTERDAY_11AM.plusHours(1).plusMinutes(10).toInstant();
        Instant endTime = YESTERDAY_11AM.plusHours(1).plusMinutes(35).toInstant();
        Instant startTime1 = YESTERDAY_11AM.plusMinutes(40).toInstant();
        Instant endTime1 = YESTERDAY_11AM.plusMinutes(50).toInstant();
        Instant startTime2 = YESTERDAY_11AM.plusMinutes(30).toInstant();
        Instant endTime2 = YESTERDAY_11AM.plusHours(1).plusMinutes(20).toInstant();
        Instant startTime3 = YESTERDAY_11AM.toInstant();
        Instant endTime3 = YESTERDAY_11AM.plusHours(1).toInstant();
        List<RecordType> records =
                List.of(
                        createRecord(startTime3, endTime3),
                        createRecord(startTime2, endTime2),
                        createRecord(startTime1, endTime1),
                        createRecord(startTime, endTime));
        insertRecords(records);
        setupAggregation(List.of(TEST_PACKAGE_NAME), mHealthDataCategory);

        List<AggregateRecordsGroupedByDurationResponse<ResultType>> responses =
                getAggregateResponseGroupByDuration(
                        new AggregateRecordsRequest.Builder<ResultType>(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(
                                                        YESTERDAY_11AM.minusMinutes(30).toInstant())
                                                .setEndTime(YESTERDAY_11AM.plusHours(2).toInstant())
                                                .build())
                                .addAggregationType(mAggregationType)
                                .build(),
                        Duration.of(30, ChronoUnit.MINUTES));

        assertThat(responses).hasSize(5);
        assertThat(responses.get(0).get(mAggregationType)).isNull();
        assertThat(responses.get(1).get(mAggregationType))
                .isEqualTo(getExpectedValueFromDuration(ofMinutes(30)));
        assertThat(responses.get(2).get(mAggregationType))
                .isEqualTo(getExpectedValueFromDuration(ofMinutes(30)));
        assertThat(responses.get(3).get(mAggregationType))
                .isEqualTo(getExpectedValueFromDuration(ofMinutes(30)));
        assertThat(responses.get(4).get(mAggregationType))
                .isEqualTo(getExpectedValueFromDuration(ofMinutes(5)));
    }

    @Test
    public void aggregateGroupByPeriod_returnsResponsePerGroup() throws Exception {
        Instant startTime = MIDNIGHT_ONE_WEEK_AGO.plusDays(6).minusMinutes(40).toInstant();
        Instant endTime = MIDNIGHT_ONE_WEEK_AGO.plusDays(6).plusMinutes(35).toInstant();
        Instant startTime1 =
                MIDNIGHT_ONE_WEEK_AGO.plusDays(4).plusHours(10).plusMinutes(30).toInstant();
        Instant endTime1 =
                MIDNIGHT_ONE_WEEK_AGO.plusDays(4).plusHours(11).plusMinutes(13).toInstant();
        Instant startTime2 = MIDNIGHT_ONE_WEEK_AGO.plusDays(4).plusHours(10).toInstant();
        Instant endTime2 = MIDNIGHT_ONE_WEEK_AGO.plusDays(4).plusHours(11).toInstant();
        Instant startTime3 = MIDNIGHT_ONE_WEEK_AGO.plusDays(3).plusHours(10).toInstant();
        Instant endTime3 = MIDNIGHT_ONE_WEEK_AGO.plusDays(3).plusHours(13).toInstant();
        Instant startTime4 = MIDNIGHT_ONE_WEEK_AGO.plusDays(3).plusHours(11).toInstant();
        Instant endTime4 =
                MIDNIGHT_ONE_WEEK_AGO.plusDays(3).plusHours(11).plusMinutes(55).toInstant();
        Instant startTime5 = MIDNIGHT_ONE_WEEK_AGO.plusDays(1).plusHours(16).toInstant();
        Instant endTime5 =
                MIDNIGHT_ONE_WEEK_AGO.plusDays(1).plusHours(16).plusMinutes(32).toInstant();
        Instant startTime6 = MIDNIGHT_ONE_WEEK_AGO.plusDays(1).plusHours(10).toInstant();
        Instant endTime6 = MIDNIGHT_ONE_WEEK_AGO.plusDays(1).plusHours(11).toInstant();
        Instant startTime7 = MIDNIGHT_ONE_WEEK_AGO.plusHours(10).toInstant();
        Instant endTime7 = MIDNIGHT_ONE_WEEK_AGO.plusHours(11).toInstant();
        List<RecordType> records =
                List.of(
                        createRecord(startTime7, endTime7),
                        createRecord(startTime6, endTime6),
                        createRecord(startTime5, endTime5),
                        createRecord(startTime4, endTime4),
                        createRecord(startTime3, endTime3),
                        createRecord(startTime2, endTime2),
                        createRecord(startTime1, endTime1),
                        createRecord(startTime, endTime));
        insertRecords(records);
        setupAggregation(List.of(TEST_PACKAGE_NAME), mHealthDataCategory);

        List<AggregateRecordsGroupedByPeriodResponse<ResultType>> responses =
                TestUtils.getAggregateResponseGroupByPeriod(
                        new AggregateRecordsRequest.Builder<ResultType>(
                                        new LocalTimeRangeFilter.Builder()
                                                .setStartTime(
                                                        MIDNIGHT_ONE_WEEK_AGO.toLocalDateTime())
                                                .setEndTime(
                                                        MIDNIGHT_ONE_WEEK_AGO
                                                                .plusDays(7)
                                                                .toLocalDateTime())
                                                .build())
                                .addAggregationType(mAggregationType)
                                .build(),
                        Period.ofDays(1));

        assertThat(responses).hasSize(7);
        assertThat(responses.get(0).get(mAggregationType))
                .isEqualTo(getExpectedValueFromDuration(getRecordDuration(records.get(0))));
        assertThat(responses.get(1).get(mAggregationType))
                .isEqualTo(
                        getExpectedValueFromDuration(
                                getRecordDuration(records.get(1))
                                        .plus(getRecordDuration(records.get(2)))));
        assertThat(responses.get(2).get(mAggregationType)).isNull();
        assertThat(responses.get(3).get(mAggregationType))
                .isEqualTo(getExpectedValueFromDuration(getRecordDuration(records.get(4))));
        assertThat(responses.get(4).get(mAggregationType))
                .isEqualTo(
                        getExpectedValueFromDuration(
                                Duration.between(
                                        records.get(5).getStartTime(),
                                        records.get(6).getEndTime())));
        assertThat(responses.get(5).get(mAggregationType))
                .isEqualTo(
                        getExpectedValueFromDuration(
                                Duration.between(
                                        records.get(7).getStartTime(),
                                        MIDNIGHT_ONE_WEEK_AGO.plusDays(6).toInstant())));
        assertThat(responses.get(6).get(mAggregationType))
                .isEqualTo(
                        getExpectedValueFromDuration(
                                Duration.between(
                                        MIDNIGHT_ONE_WEEK_AGO.plusDays(6).toInstant(),
                                        records.get(7).getEndTime())));
    }

    @Test
    public void multiApp_overlappingRecords_takesOverlapsIntoAccount() throws Exception {
        RecordType higherPriorityRecord =
                createRecord(YESTERDAY_11AM.toInstant(), YESTERDAY_11AM.plusHours(1).toInstant());
        RecordType lowerPriorityRecord =
                createRecord(
                        YESTERDAY_11AM.minusMinutes(17).toInstant(),
                        YESTERDAY_11AM.plusMinutes(30).toInstant());
        insertRecord(higherPriorityRecord);
        APP_WITH_WRITE_PERMS_ONLY.insertRecords(lowerPriorityRecord);
        setupAggregation(
                List.of(TEST_PACKAGE_NAME, APP_WITH_WRITE_PERMS_ONLY.getPackageName()),
                mHealthDataCategory);

        AggregateRecordsResponse<ResultType> response =
                getAggregateResponse(
                        new AggregateRecordsRequest.Builder<ResultType>(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(Instant.EPOCH)
                                                .setEndTime(Instant.now())
                                                .build())
                                .addAggregationType(mAggregationType)
                                .build());

        assertThat(response.get(mAggregationType)).isNotNull();
        assertThat(response.get(mAggregationType))
                .isEqualTo(
                        getExpectedValueFromDuration(
                                Duration.between(
                                        lowerPriorityRecord.getStartTime(),
                                        higherPriorityRecord.getEndTime())));
        assertThat(response.getDataOrigins(mAggregationType))
                .containsExactly(
                        new DataOrigin.Builder().setPackageName(TEST_PACKAGE_NAME).build(),
                        new DataOrigin.Builder()
                                .setPackageName(APP_WITH_WRITE_PERMS_ONLY.getPackageName())
                                .build());
    }

    static String getTestPackageName() {
        return ApplicationProvider.getApplicationContext().getPackageName();
    }

    static Duration getRecordDuration(IntervalRecord record) {
        return Duration.between(record.getStartTime(), record.getEndTime());
    }
}
