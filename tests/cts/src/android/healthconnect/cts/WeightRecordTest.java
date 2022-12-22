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

import static android.healthconnect.datatypes.WeightRecord.WEIGHT_AVG;
import static android.healthconnect.datatypes.WeightRecord.WEIGHT_MAX;
import static android.healthconnect.datatypes.WeightRecord.WEIGHT_MIN;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.healthconnect.AggregateRecordsRequest;
import android.healthconnect.AggregateRecordsResponse;
import android.healthconnect.HealthConnectException;
import android.healthconnect.HealthConnectManager;
import android.healthconnect.InsertRecordsResponse;
import android.healthconnect.TimeRangeFilter;
import android.healthconnect.datatypes.DataOrigin;
import android.healthconnect.datatypes.Metadata;
import android.healthconnect.datatypes.Record;
import android.healthconnect.datatypes.WeightRecord;
import android.healthconnect.datatypes.units.Mass;
import android.os.OutcomeReceiver;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class WeightRecordTest {
    private static final String TAG = "WeightRecordTest";

    @Test
    public void testInsertWeightRecord() throws InterruptedException {
        List<Record> records = new ArrayList<>();
        records.add(getBaseWeightRecord());
        records.add(getCompleteWeightRecord());
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

    @Test
    public void testAggregation_weight() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        List<Record> records =
                Arrays.asList(
                        getBaseWeightRecord(5.0),
                        getBaseWeightRecord(10.0),
                        getBaseWeightRecord(15.0));
        AggregateRecordsResponse<Mass> response =
                TestUtils.getAggregateResponse(
                        new AggregateRecordsRequest.Builder<Mass>(
                                        new TimeRangeFilter.Builder(
                                                        Instant.ofEpochMilli(0),
                                                        Instant.now().plus(1, ChronoUnit.DAYS))
                                                .build())
                                .addAggregationType(WEIGHT_AVG)
                                .addAggregationType(WEIGHT_MAX)
                                .addAggregationType(WEIGHT_MIN)
                                .addDataOriginsFilter(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build(),
                        records);
        Mass maxWeight = response.get(WEIGHT_MAX);
        Mass minWeight = response.get(WEIGHT_MIN);
        Mass avgWeight = response.get(WEIGHT_AVG);
        assertThat(maxWeight).isNotNull();
        assertThat(maxWeight.getInKilograms()).isEqualTo(15.0);
        assertThat(minWeight).isNotNull();
        assertThat(minWeight.getInKilograms()).isEqualTo(5.0);
        assertThat(avgWeight).isNotNull();
        assertThat(avgWeight.getInKilograms()).isEqualTo(10.0);
    }

    static WeightRecord getBaseWeightRecord() {
        return new WeightRecord.Builder(
                        new Metadata.Builder().build(), Instant.now(), Mass.fromKilograms(10.0))
                .build();
    }

    static WeightRecord getBaseWeightRecord(double weight) {
        return new WeightRecord.Builder(
                        new Metadata.Builder().build(), Instant.now(), Mass.fromKilograms(weight))
                .build();
    }

    static WeightRecord getCompleteWeightRecord() {
        return new WeightRecord.Builder(
                        new Metadata.Builder().build(), Instant.now(), Mass.fromKilograms(10.0))
                .setZoneOffset(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()))
                .build();
    }
}
