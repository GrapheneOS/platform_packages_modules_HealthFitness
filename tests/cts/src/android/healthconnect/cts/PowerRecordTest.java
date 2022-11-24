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
import android.healthconnect.datatypes.Metadata;
import android.healthconnect.datatypes.PowerRecord;
import android.healthconnect.datatypes.Record;
import android.healthconnect.datatypes.units.Power;
import android.os.OutcomeReceiver;
import android.platform.test.annotations.AppModeFull;
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

@AppModeFull(reason = "HealthConnectManager is not accessible to instant apps")
@RunWith(AndroidJUnit4.class)
public class PowerRecordTest {

    private static final String TAG = "PowerRecordTest";

    static PowerRecord getBasePowerRecord() {
        PowerRecord.PowerRecordSample powerRecord =
                new PowerRecord.PowerRecordSample(Power.fromWatts(10.0), Instant.now());
        ArrayList<PowerRecord.PowerRecordSample> powerRecords = new ArrayList<>();
        powerRecords.add(powerRecord);
        powerRecords.add(powerRecord);

        return new PowerRecord.Builder(
                        new Metadata.Builder().build(), Instant.now(), Instant.now(), powerRecords)
                .build();
    }

    static PowerRecord getBasePowerRecord(double power) {
        PowerRecord.PowerRecordSample powerRecord =
                new PowerRecord.PowerRecordSample(Power.fromWatts(power), Instant.now());
        ArrayList<PowerRecord.PowerRecordSample> powerRecords = new ArrayList<>();
        powerRecords.add(powerRecord);
        powerRecords.add(powerRecord);

        return new PowerRecord.Builder(
                        new Metadata.Builder().build(), Instant.now(), Instant.now(), powerRecords)
                .build();
    }

    @Test
    public void testInsertPowerRecord() throws InterruptedException {
        List<Record> records = new ArrayList<>();
        records.add(getBasePowerRecord());
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
