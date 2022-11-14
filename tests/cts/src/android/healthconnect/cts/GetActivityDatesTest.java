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

import static android.healthconnect.cts.TestUtils.MANAGE_HEALTH_DATA;

import static com.google.common.truth.Truth.assertThat;

import android.app.UiAutomation;
import android.content.Context;
import android.healthconnect.HealthConnectException;
import android.healthconnect.HealthConnectManager;
import android.healthconnect.InsertRecordsResponse;
import android.healthconnect.datatypes.BasalMetabolicRateRecord;
import android.healthconnect.datatypes.InstantRecord;
import android.healthconnect.datatypes.IntervalRecord;
import android.healthconnect.datatypes.Metadata;
import android.healthconnect.datatypes.Record;
import android.healthconnect.datatypes.StepsRecord;
import android.healthconnect.datatypes.units.Power;
import android.os.OutcomeReceiver;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@RunWith(AndroidJUnit4.class)
public class GetActivityDatesTest {
    private static final String TAG = "GetActivityDatesTest";

    // TODO(b/257796081): Test the response size after database clean up is implemented
    //    @Test
    //    public void testEmptyActivityDates() throws InterruptedException {
    //        List<Record> records = getTestRecords();

    //         List<LocalDate> activityDates = getActivityDates(
    //                 records.stream().map(Record::getClass).collect(Collectors.toList()));
    //
    //        assertThat(activityDates).hasSize(0);
    //    }

    @Test
    public void testActivityDates() throws InterruptedException {

        List<Record> records = getTestRecords();
        insertRecords(records);
        List<LocalDate> activityDates =
                getActivityDates(
                        records.stream().map(Record::getClass).collect(Collectors.toList()));
        assertThat(activityDates).hasSize(2);
        assertThat(activityDates)
                .containsAtLeastElementsIn(
                        records.stream().map(this::getRecordDate).collect(Collectors.toSet()));
    }

    private void insertRecords(List<Record> records) throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        CountDownLatch latch = new CountDownLatch(1);
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();
        AtomicReference<List<Record>> response = new AtomicReference<>();
        service.insertRecords(
                records,
                Executors.newSingleThreadExecutor(),
                new OutcomeReceiver<>() {
                    @Override
                    public void onResult(InsertRecordsResponse result) {
                        response.set(result.getRecords());
                        latch.countDown();
                    }

                    @Override
                    public void onError(HealthConnectException exception) {
                        Log.e(TAG, exception.getMessage());
                    }
                });
        assertThat(latch.await(3, TimeUnit.SECONDS)).isEqualTo(true);
        assertThat(response.get()).hasSize(records.size());
    }

    private List<LocalDate> getActivityDates(List<Class<? extends Record>> recordTypes)
            throws InterruptedException {
        UiAutomation uiAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        uiAutomation.adoptShellPermissionIdentity(MANAGE_HEALTH_DATA);
        try {
            Context context = ApplicationProvider.getApplicationContext();
            HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
            CountDownLatch latch = new CountDownLatch(1);
            assertThat(service).isNotNull();
            AtomicReference<List<LocalDate>> response = new AtomicReference<>();
            service.queryActivityDates(
                    recordTypes,
                    Executors.newSingleThreadExecutor(),
                    new OutcomeReceiver<>() {
                        @Override
                        public void onResult(List<LocalDate> result) {
                            response.set(result);
                            latch.countDown();
                        }

                        @Override
                        public void onError(HealthConnectException exception) {
                            Log.e(TAG, exception.getMessage());
                        }
                    });

            assertThat(latch.await(3, TimeUnit.SECONDS)).isEqualTo(true);
            return response.get();
        } finally {
            uiAutomation.dropShellPermissionIdentity();
        }
    }

    private List<Record> getTestRecords() {
        return new ArrayList<>(
                Arrays.asList(
                        new StepsRecord.Builder(
                                        new Metadata.Builder().build(),
                                        Instant.now(),
                                        Instant.now(),
                                        10)
                                .build(),
                        new BasalMetabolicRateRecord.Builder(
                                        new Metadata.Builder().build(),
                                        Instant.now().minus(3, ChronoUnit.DAYS),
                                        Power.fromWatts(100.0))
                                .build()));
    }

    private LocalDate getRecordDate(Record record) {
        LocalDate activityDate;
        if (record instanceof IntervalRecord) {
            activityDate =
                    LocalDate.ofInstant(
                            ((IntervalRecord) record).getStartTime(),
                            ((IntervalRecord) record).getStartZoneOffset());
        } else if (record instanceof InstantRecord) {
            activityDate =
                    LocalDate.ofInstant(
                            ((InstantRecord) record).getTime(),
                            ((InstantRecord) record).getZoneOffset());
        } else {
            activityDate =
                    LocalDate.ofInstant(
                            Instant.now(),
                            ZoneOffset.systemDefault().getRules().getOffset(Instant.now()));
        }
        return activityDate;
    }
}
