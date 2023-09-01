/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.healthconnect.tests.storage;

import static android.health.connect.datatypes.StepsRecord.STEPS_COUNT_TOTAL;

import static com.google.common.truth.Truth.assertThat;

import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.DAYS;

import android.app.UiAutomation;
import android.content.Context;
import android.health.connect.AggregateRecordsGroupedByPeriodResponse;
import android.health.connect.AggregateRecordsRequest;
import android.health.connect.HealthConnectException;
import android.health.connect.HealthConnectManager;
import android.health.connect.HealthPermissions;
import android.health.connect.LocalTimeRangeFilter;
import android.os.OutcomeReceiver;

import androidx.annotation.NonNull;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class AggregationTest {
    private static final String MANAGE_HEALTH_DATA =
            HealthPermissions.MANAGE_HEALTH_DATA_PERMISSION;
    private static final int TIMEOUT_SECONDS = 5;
    private Context mContext;
    private HealthConnectManager mService;

    @Before
    public void setup() {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        mService = mContext.getSystemService(HealthConnectManager.class);
        UiAutomation uiAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        uiAutomation.adoptShellPermissionIdentity(MANAGE_HEALTH_DATA);
    }

    @Test
    // TODO(b/298249754): Add this test in CTS
    public void groupByPeriod_expectCorrectSlices() throws Exception {
        // TODO(b/298249754): Add some test data and check it's correct
        Instant startTime = Instant.now().minus(40, DAYS);
        LocalDateTime startLocalTime =
                LocalDateTime.ofInstant(Instant.ofEpochMilli(startTime.toEpochMilli()), UTC);
        Instant endTime = startTime.plus(35, DAYS);
        LocalDateTime endLocalTime =
                LocalDateTime.ofInstant(Instant.ofEpochMilli(endTime.toEpochMilli()), UTC);

        // Due to the Parcel implementation, we have to set local time at UTC zone
        AggregateRecordsRequest<Long> request =
                new AggregateRecordsRequest.Builder<Long>(
                                new LocalTimeRangeFilter.Builder()
                                        .setStartTime(startLocalTime)
                                        .setEndTime(endLocalTime)
                                        .build())
                        .addAggregationType(STEPS_COUNT_TOTAL)
                        .build();
        List<AggregateRecordsGroupedByPeriodResponse<Long>> response =
                getAggregateResponseGroupByPeriod(request, Period.ofMonths(1));

        assertThat(response.size()).isEqualTo(2);
        assertThat(response.get(0).getStartTime()).isEqualTo(startLocalTime);
        assertThat(response.get(0).getEndTime()).isEqualTo(startLocalTime.plusMonths(1));
        assertThat(response.get(1).getStartTime()).isEqualTo(startLocalTime.plusMonths(1));
        assertThat(response.get(1).getEndTime()).isEqualTo(endLocalTime);
    }

    private <T> List<AggregateRecordsGroupedByPeriodResponse<T>> getAggregateResponseGroupByPeriod(
            AggregateRecordsRequest<T> request, Period period) throws InterruptedException {
        AtomicReference<List<AggregateRecordsGroupedByPeriodResponse<T>>> response =
                new AtomicReference<>();
        AtomicReference<HealthConnectException> healthConnectExceptionAtomicReference =
                new AtomicReference<>();

        CountDownLatch latch = new CountDownLatch(1);
        mService.aggregateGroupByPeriod(
                request,
                period,
                Executors.newSingleThreadExecutor(),
                new OutcomeReceiver<>() {
                    @Override
                    public void onResult(List<AggregateRecordsGroupedByPeriodResponse<T>> result) {
                        response.set(result);
                        latch.countDown();
                    }

                    @Override
                    public void onError(@NonNull HealthConnectException healthConnectException) {
                        healthConnectExceptionAtomicReference.set(healthConnectException);
                        latch.countDown();
                    }
                });
        assertThat(latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
        if (healthConnectExceptionAtomicReference.get() != null) {
            throw healthConnectExceptionAtomicReference.get();
        }

        return response.get();
    }
}
