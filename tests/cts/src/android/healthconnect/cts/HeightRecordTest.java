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

import static android.healthconnect.datatypes.HeightRecord.HEIGHT_AVG;
import static android.healthconnect.datatypes.HeightRecord.HEIGHT_MAX;
import static android.healthconnect.datatypes.HeightRecord.HEIGHT_MIN;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.healthconnect.AggregateRecordsRequest;
import android.healthconnect.AggregateRecordsResponse;
import android.healthconnect.HealthConnectException;
import android.healthconnect.HealthConnectManager;
import android.healthconnect.InsertRecordsResponse;
import android.healthconnect.TimeRangeFilter;
import android.healthconnect.datatypes.DataOrigin;
import android.healthconnect.datatypes.HeightRecord;
import android.healthconnect.datatypes.Metadata;
import android.healthconnect.datatypes.Record;
import android.healthconnect.datatypes.units.Length;
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
public class HeightRecordTest {
    private static final String TAG = "HeightRecordTest";

    @Test
    public void testInsertHeightRecord() throws InterruptedException {
        List<Record> records = new ArrayList<>();
        records.add(getBaseHeightRecord());
        records.add(getCompleteHeightRecord());
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
    public void testAggregation_Height() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        List<Record> records =
                Arrays.asList(
                        getBaseHeightRecord(5.0),
                        getBaseHeightRecord(10.0),
                        getBaseHeightRecord(15.0));
        AggregateRecordsResponse<Length> response =
                TestUtils.getAggregateResponse(
                        new AggregateRecordsRequest.Builder<Length>(
                                        new TimeRangeFilter.Builder(
                                                        Instant.ofEpochMilli(0),
                                                        Instant.now().plus(1, ChronoUnit.DAYS))
                                                .build())
                                .addAggregationType(HEIGHT_MAX)
                                .addAggregationType(HEIGHT_MIN)
                                .addAggregationType(HEIGHT_AVG)
                                .addDataOriginsFilter(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build(),
                        records);
        Length maxHeight = response.get(HEIGHT_MAX);
        Length minHeight = response.get(HEIGHT_MIN);
        Length avgHeight = response.get(HEIGHT_AVG);
        assertThat(maxHeight).isNotNull();
        assertThat(maxHeight.getInMeters()).isEqualTo(15.0);
        assertThat(minHeight).isNotNull();
        assertThat(minHeight.getInMeters()).isEqualTo(5.0);
        assertThat(avgHeight).isNotNull();
        assertThat(avgHeight.getInMeters()).isEqualTo(10.0);
    }

    static HeightRecord getBaseHeightRecord() {
        return new HeightRecord.Builder(
                        new Metadata.Builder().build(), Instant.now(), Length.fromMeters(10.0))
                .build();
    }

    static HeightRecord getBaseHeightRecord(double height) {
        return new HeightRecord.Builder(
                        new Metadata.Builder().build(), Instant.now(), Length.fromMeters(height))
                .build();
    }

    static HeightRecord getCompleteHeightRecord() {
        return new HeightRecord.Builder(
                        new Metadata.Builder().build(), Instant.now(), Length.fromMeters(10.0))
                .setZoneOffset(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()))
                .build();
    }
}
