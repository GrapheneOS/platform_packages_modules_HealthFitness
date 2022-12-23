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

package android.healthconnect.cts.nopermission;

import static android.healthconnect.datatypes.HeartRateRecord.BPM_MAX;
import static android.healthconnect.datatypes.HeartRateRecord.BPM_MIN;

import static com.google.common.truth.Truth.assertThat;

import android.healthconnect.AggregateRecordsRequest;
import android.healthconnect.ChangeLogTokenRequest;
import android.healthconnect.ChangeLogTokenResponse;
import android.healthconnect.ChangeLogsRequest;
import android.healthconnect.ReadRecordsRequestUsingFilters;
import android.healthconnect.TimeInstantRangeFilter;
import android.healthconnect.cts.TestUtils;
import android.healthconnect.datatypes.DataOrigin;
import android.healthconnect.datatypes.Record;
import android.platform.test.annotations.AppModeFull;

import androidx.test.runner.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Duration;
import java.time.Instant;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** These test run under an environment which has no HC permissions */
@AppModeFull(reason = "HealthConnectManager is not accessible to instant apps")
@RunWith(AndroidJUnit4.class)
public class HealthConnectManagerNoPermissionsGrantedTest {
    @Test
    public void testInsertNotAllowed() throws InterruptedException {
        for (Record testRecord : TestUtils.getTestRecords()) {
            try {
                TestUtils.insertRecords(Collections.singletonList(testRecord));
                Assert.fail();
            } catch (SecurityException securityException) {
                assertThat(true).isTrue();
                assertThat(securityException).isNotNull();
            }
        }
    }

    @Test
    public void testUpdateNotAllowed() throws InterruptedException {
        for (Record testRecord : TestUtils.getTestRecords()) {
            try {
                TestUtils.updateRecords(Collections.singletonList(testRecord));
                Assert.fail();
            } catch (SecurityException securityException) {
                assertThat(true).isTrue();
                assertThat(securityException).isNotNull();
            }
        }
    }

    @Test
    public void testDeleteUsingIdNotAllowed() throws InterruptedException {
        for (Record testRecord : TestUtils.getTestRecords()) {
            try {
                TestUtils.deleteRecords(Collections.singletonList(testRecord));
                Assert.fail();
            } catch (SecurityException securityException) {
                assertThat(true).isTrue();
                assertThat(securityException).isNotNull();
            }
        }
    }

    @Test
    public void testDeleteUsingFilterNotAllowed() throws InterruptedException {
        for (Record testRecord : TestUtils.getTestRecords()) {
            try {
                TestUtils.verifyDeleteRecords(
                        testRecord.getClass(),
                        new TimeInstantRangeFilter.Builder()
                                .setStartTime(Instant.now())
                                .setEndTime(Instant.now())
                                .build());
                Assert.fail();
            } catch (SecurityException securityException) {
                assertThat(true).isTrue();
                assertThat(securityException).isNotNull();
            }
        }
    }

    @Test
    public void testChangeLogsNotAllowed() throws InterruptedException {
        for (Record testRecord : TestUtils.getTestRecords()) {
            try {
                ChangeLogTokenResponse tokenResponse =
                        TestUtils.getChangeLogToken(
                                new ChangeLogTokenRequest.Builder()
                                        .addRecordType(testRecord.getClass())
                                        .build());
                TestUtils.getChangeLogs(
                        new ChangeLogsRequest.Builder(tokenResponse.getToken()).build());
                Assert.fail();
            } catch (SecurityException securityException) {
                assertThat(true).isTrue();
                assertThat(securityException).isNotNull();
            }
        }
    }

    @Test
    public void testReadNotAllowed() throws InterruptedException {
        for (Record testRecord : TestUtils.getTestRecords()) {
            try {
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(testRecord.getClass())
                                .build());
                Assert.fail();
            } catch (SecurityException securityException) {
                assertThat(true).isTrue();
                assertThat(securityException).isNotNull();
            }
        }
    }

    @Test
    public void testAggregateNotAllowed() throws InterruptedException {
        try {
            List<Record> records =
                    Arrays.asList(
                            TestUtils.getHeartRateRecord(71),
                            TestUtils.getHeartRateRecord(72),
                            TestUtils.getHeartRateRecord(73));
            TestUtils.getAggregateResponse(
                    new AggregateRecordsRequest.Builder<Long>(
                                    new TimeInstantRangeFilter.Builder()
                                            .setStartTime(Instant.ofEpochMilli(0))
                                            .setEndTime(Instant.now())
                                            .build())
                            .addAggregationType(BPM_MAX)
                            .addAggregationType(BPM_MIN)
                            .addDataOriginsFilter(
                                    new DataOrigin.Builder().setPackageName("abc").build())
                            .build(),
                    records);
            Assert.fail();
        } catch (SecurityException securityException) {
            assertThat(true).isTrue();
            assertThat(securityException).isNotNull();
        }
    }

    @Test
    public void testAggregateGroupByDurationNotAllowed() throws InterruptedException {
        try {
            Instant start = Instant.now().minusMillis(500);
            Instant end = Instant.now().plusMillis(2500);
            TestUtils.getAggregateResponseGroupByDuration(
                    new AggregateRecordsRequest.Builder<Long>(
                                    new TimeInstantRangeFilter.Builder()
                                            .setStartTime(start)
                                            .setEndTime(end)
                                            .build())
                            .addAggregationType(BPM_MAX)
                            .addAggregationType(BPM_MIN)
                            .build(),
                    Duration.ofSeconds(1));
            Assert.fail();
        } catch (SecurityException securityException) {
            assertThat(true).isTrue();
            assertThat(securityException).isNotNull();
        }
    }

    @Test
    public void testAggregateGroupByPeriodNotAllowed() throws InterruptedException {
        try {
            Instant start = Instant.now().minus(3, ChronoUnit.DAYS);
            Instant end = start.plus(3, ChronoUnit.DAYS);
            TestUtils.getAggregateResponseGroupByPeriod(
                    new AggregateRecordsRequest.Builder<Long>(
                                    new TimeInstantRangeFilter.Builder()
                                            .setStartTime(start)
                                            .setEndTime(end)
                                            .build())
                            .addAggregationType(BPM_MAX)
                            .addAggregationType(BPM_MIN)
                            .build(),
                    Period.ofDays(1));
            Assert.fail();
        } catch (SecurityException securityException) {
            assertThat(true).isTrue();
            assertThat(securityException).isNotNull();
        }
    }
}
