/*
 * Copyright (C) 2022 The Android Open Source Project
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

package android.healthconnect.cts;

import static android.healthconnect.datatypes.HeartRateRecord.BPM_MAX;
import static android.healthconnect.datatypes.HeartRateRecord.BPM_MIN;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.healthconnect.AggregateRecordsGroupedByDurationResponse;
import android.healthconnect.AggregateRecordsGroupedByPeriodResponse;
import android.healthconnect.AggregateRecordsRequest;
import android.healthconnect.AggregateRecordsResponse;
import android.healthconnect.TimeRangeFilter;
import android.healthconnect.datatypes.DataOrigin;
import android.healthconnect.datatypes.Record;
import android.platform.test.annotations.AppModeFull;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Duration;
import java.time.Instant;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

/** CTS test for API provided by HealthConnectManager. */
@AppModeFull(reason = "HealthConnectManager is not accessible to instant apps")
@RunWith(AndroidJUnit4.class)
public class HealthConnectAggregateTests {
    @Test
    public void testBpmAggregation_timeRange_all() throws Exception {
        List<Record> records =
                Arrays.asList(
                        TestUtils.getHeartRateRecord(71),
                        TestUtils.getHeartRateRecord(72),
                        TestUtils.getHeartRateRecord(73));
        AggregateRecordsResponse<Long> response =
                TestUtils.getAggregateResponse(
                        new AggregateRecordsRequest.Builder<Long>(
                                        new TimeRangeFilter.Builder(
                                                        Instant.ofEpochMilli(0), Instant.now())
                                                .build())
                                .addAggregationType(BPM_MAX)
                                .addAggregationType(BPM_MIN)
                                .build(),
                        records);
        assertThat(response.get(BPM_MAX)).isNotNull();
        assertThat(response.get(BPM_MAX)).isEqualTo(73);
        assertThat(response.getZoneOffset(BPM_MAX))
                .isEqualTo(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()));
        assertThat(response.get(BPM_MIN)).isNotNull();
        assertThat(response.get(BPM_MIN)).isEqualTo(71);
        assertThat(response.getZoneOffset(BPM_MIN))
                .isEqualTo(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()));
    }

    @Test
    public void testBpmAggregation_timeRange_not_present() throws Exception {
        List<Record> records =
                Arrays.asList(
                        TestUtils.getHeartRateRecord(71),
                        TestUtils.getHeartRateRecord(72),
                        TestUtils.getHeartRateRecord(73));
        AggregateRecordsResponse<Long> response =
                TestUtils.getAggregateResponse(
                        new AggregateRecordsRequest.Builder<Long>(
                                        new TimeRangeFilter.Builder(
                                                        Instant.now().plusMillis(1000),
                                                        Instant.now().plusMillis(2000))
                                                .build())
                                .addAggregationType(BPM_MAX)
                                .addAggregationType(BPM_MIN)
                                .build(),
                        records);
        assertThat(response.get(BPM_MAX)).isNull();
        assertThat(response.getZoneOffset(BPM_MAX)).isNull();
        assertThat(response.get(BPM_MIN)).isNull();
        assertThat(response.getZoneOffset(BPM_MIN)).isNull();
    }

    @Test
    public void testBpmAggregation_withDataOrigin_correct() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        List<Record> records =
                Arrays.asList(
                        TestUtils.getHeartRateRecord(71),
                        TestUtils.getHeartRateRecord(72),
                        TestUtils.getHeartRateRecord(73));
        AggregateRecordsResponse<Long> response =
                TestUtils.getAggregateResponse(
                        new AggregateRecordsRequest.Builder<Long>(
                                        new TimeRangeFilter.Builder(
                                                        Instant.ofEpochMilli(0), Instant.now())
                                                .build())
                                .addAggregationType(BPM_MAX)
                                .addAggregationType(BPM_MIN)
                                .addDataOriginsFilter(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build(),
                        records);
        assertThat(response.get(BPM_MAX)).isNotNull();
        assertThat(response.get(BPM_MAX)).isEqualTo(73);
        assertThat(response.getZoneOffset(BPM_MAX))
                .isEqualTo(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()));
        assertThat(response.get(BPM_MIN)).isNotNull();
        assertThat(response.get(BPM_MIN)).isEqualTo(71);
        assertThat(response.getZoneOffset(BPM_MIN))
                .isEqualTo(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()));
    }

    @Test
    public void testBpmAggregation_withDataOrigin_incorrect() throws Exception {
        List<Record> records =
                Arrays.asList(
                        TestUtils.getHeartRateRecord(71),
                        TestUtils.getHeartRateRecord(72),
                        TestUtils.getHeartRateRecord(73));
        AggregateRecordsResponse<Long> response =
                TestUtils.getAggregateResponse(
                        new AggregateRecordsRequest.Builder<Long>(
                                        new TimeRangeFilter.Builder(
                                                        Instant.ofEpochMilli(0), Instant.now())
                                                .build())
                                .addAggregationType(BPM_MAX)
                                .addAggregationType(BPM_MIN)
                                .addDataOriginsFilter(
                                        new DataOrigin.Builder().setPackageName("abc").build())
                                .build(),
                        records);
        assertThat(response.get(BPM_MAX)).isNull();
        assertThat(response.getZoneOffset(BPM_MAX)).isNull();
        assertThat(response.get(BPM_MIN)).isNull();
        assertThat(response.getZoneOffset(BPM_MIN)).isNull();
    }

    @Test
    public void testBpmAggregation_groupBy_Duration() throws Exception {
        Instant start = Instant.now().minusMillis(500);
        Instant end = Instant.now().plusMillis(2500);
        insertHeartRateRecordsWithDelay(1000, 3);
        List<AggregateRecordsGroupedByDurationResponse<Long>> responses =
                TestUtils.getAggregateResponseGroupByDuration(
                        new AggregateRecordsRequest.Builder<Long>(
                                        new TimeRangeFilter.Builder(start, end).build())
                                .addAggregationType(BPM_MAX)
                                .addAggregationType(BPM_MIN)
                                .build(),
                        Duration.ofSeconds(1));
        assertThat(responses.size()).isEqualTo(3);
        for (AggregateRecordsGroupedByDurationResponse<Long> response : responses) {
            assertThat(response.get(BPM_MAX)).isNotNull();
            assertThat(response.get(BPM_MAX)).isEqualTo(73);
            assertThat(response.getZoneOffset(BPM_MAX))
                    .isEqualTo(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()));
            assertThat(response.get(BPM_MIN)).isNotNull();
            assertThat(response.get(BPM_MIN)).isEqualTo(71);
            assertThat(response.getZoneOffset(BPM_MIN))
                    .isEqualTo(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()));
            assertThat(response.getStartTime()).isNotNull();
            assertThat(response.getEndTime()).isNotNull();
            start = start.plus(1, ChronoUnit.SECONDS);
        }
    }

    @Test
    public void testBpmAggregation_groupBy_Period() throws Exception {
        Instant start = Instant.now().minus(3, ChronoUnit.DAYS);
        Instant end = start.plus(3, ChronoUnit.DAYS);
        insertHeartRateRecordsInPastDays(4);
        List<AggregateRecordsGroupedByPeriodResponse<Long>> responses =
                TestUtils.getAggregateResponseGroupByPeriod(
                        new AggregateRecordsRequest.Builder<Long>(
                                        new TimeRangeFilter.Builder(start, end).build())
                                .addAggregationType(BPM_MAX)
                                .addAggregationType(BPM_MIN)
                                .build(),
                        Period.ofDays(1));
        assertThat(responses.size()).isAtLeast(3);
        for (AggregateRecordsGroupedByPeriodResponse<Long> response : responses) {
            if (start.toEpochMilli()
                    < response.getStartTime()
                            .atZone(ZoneOffset.systemDefault())
                            .toInstant()
                            .toEpochMilli()) {
                Long bpm = response.get(BPM_MAX);
                ZoneOffset zoneOffset = response.getZoneOffset(BPM_MAX);

                if (bpm == null) {
                    assertThat(zoneOffset).isNull();
                } else {
                    assertThat(zoneOffset).isNotNull();
                }
                // skip the check if our request doesn't fall with in the instant time we inserted
                // the record
                continue;
            }

            if (end.toEpochMilli()
                    > response.getEndTime()
                            .atZone(ZoneOffset.systemDefault())
                            .toInstant()
                            .toEpochMilli()) {
                Long bpm = response.get(BPM_MAX);
                ZoneOffset zoneOffset = response.getZoneOffset(BPM_MAX);

                if (bpm == null) {
                    assertThat(zoneOffset).isNull();
                } else {
                    assertThat(zoneOffset).isNotNull();
                }
                // skip the check if our request doesn't fall with in the instant time we inserted
                // the record
                continue;
            }

            assertThat(response.get(BPM_MAX)).isNotNull();
            assertThat(response.get(BPM_MAX)).isEqualTo(73);
            assertThat(response.getZoneOffset(BPM_MAX))
                    .isEqualTo(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()));
            assertThat(response.get(BPM_MIN)).isNotNull();
            assertThat(response.get(BPM_MIN)).isEqualTo(71);
            assertThat(response.getZoneOffset(BPM_MIN))
                    .isEqualTo(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()));
        }
    }

    private void insertHeartRateRecordsWithDelay(long delayInMillis, int times)
            throws InterruptedException {
        for (int i = 0; i < times; i++) {
            List<Record> records =
                    Arrays.asList(
                            TestUtils.getHeartRateRecord(71),
                            TestUtils.getHeartRateRecord(72),
                            TestUtils.getHeartRateRecord(73));

            TestUtils.insertRecords(records);
            Thread.sleep(delayInMillis);
        }
    }

    private void insertHeartRateRecordsInPastDays(int numDays) throws InterruptedException {
        for (int i = numDays; i > 0; i--) {
            List<Record> records =
                    Arrays.asList(
                            TestUtils.getHeartRateRecord(
                                    71, Instant.now().minus(i, ChronoUnit.DAYS)),
                            TestUtils.getHeartRateRecord(
                                    72, Instant.now().minus(i, ChronoUnit.DAYS)),
                            TestUtils.getHeartRateRecord(
                                    73, Instant.now().minus(i, ChronoUnit.DAYS)));

            TestUtils.insertRecords(records);
        }
    }
}
