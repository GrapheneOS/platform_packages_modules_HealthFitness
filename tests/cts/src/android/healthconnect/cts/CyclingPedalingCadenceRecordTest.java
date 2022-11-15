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

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.healthconnect.HealthConnectException;
import android.healthconnect.HealthConnectManager;
import android.healthconnect.InsertRecordsResponse;
import android.healthconnect.datatypes.CyclingPedalingCadenceRecord;
import android.healthconnect.datatypes.Metadata;
import android.healthconnect.datatypes.Record;
import android.os.OutcomeReceiver;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class CyclingPedalingCadenceRecordTest {

    private static final String TAG = "CyclingPedalingCadenceRecordTest";

    static CyclingPedalingCadenceRecord getBaseCyclingPedalingCadenceRecord() {
        CyclingPedalingCadenceRecord.CyclingPedalingCadenceRecordSample
                cyclingPedalingCadenceRecord =
                        new CyclingPedalingCadenceRecord.CyclingPedalingCadenceRecordSample(
                                10, Instant.now());
        ArrayList<CyclingPedalingCadenceRecord.CyclingPedalingCadenceRecordSample>
                cyclingPedalingCadenceRecords = new ArrayList<>();
        cyclingPedalingCadenceRecords.add(cyclingPedalingCadenceRecord);
        cyclingPedalingCadenceRecords.add(cyclingPedalingCadenceRecord);

        return new CyclingPedalingCadenceRecord.Builder(
                        new Metadata.Builder().build(),
                        Instant.now(),
                        Instant.now(),
                        cyclingPedalingCadenceRecords)
                .build();
    }

    @Test
    public void testInsertCyclingPedalingCadenceRecord() throws InterruptedException {
        List<Record> records = new ArrayList<>();
        records.add(getBaseCyclingPedalingCadenceRecord());
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
}
