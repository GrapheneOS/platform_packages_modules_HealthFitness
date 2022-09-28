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
import android.healthconnect.datatypes.Record;
import android.healthconnect.datatypes.RecordTypeIdentifier;
import android.healthconnect.datatypes.StepsRecord;
import android.os.OutcomeReceiver;
import android.platform.test.annotations.AppModeFull;

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

/** CTS test for API provided by HealthConnectManager. */
@AppModeFull(reason = "HealthConnectManager is not accessible to instant apps")
@RunWith(AndroidJUnit4.class)
public class HealthConnectManagerTest {
    @Test
    public void testHCManagerIsAccessible_viaHCManager() {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();
    }

    @Test
    public void testHCManagerIsAccessible_viaContextConstant() {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager service =
                (HealthConnectManager) context.getSystemService(Context.HEALTHCONNECT_SERVICE);
        assertThat(service).isNotNull();
    }

    @Test
    public void testStepsRecord_identifiers() throws Exception {
        StepsRecord stepsRecord = getStepsRecord();

        // Make sure all the data type and its helper have correct identifier
        assertThat(stepsRecord.getRecordType()).isEqualTo(RecordTypeIdentifier.RECORD_TYPE_STEPS);
    }

    @Test
    public void testAddStepsRecord() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        List<Record> records = new ArrayList<>();
        StepsRecord stepsRecord = getStepsRecord();
        records.add(stepsRecord);
        records.add(stepsRecord);
        CountDownLatch latch = new CountDownLatch(1);
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();
        service.insertRecords(
                records,
                Executors.newSingleThreadExecutor(),
                new OutcomeReceiver<InsertRecordsResponse, HealthConnectException>() {
                    @Override
                    public void onResult(InsertRecordsResponse result) {
                        assertThat(result.getRecords()).hasSize(2);
                        latch.countDown();
                    }

                    @Override
                    public void onError(HealthConnectException exception) {
                        assert false;
                    }
                });
        latch.await(3, TimeUnit.SECONDS);
    }

    private StepsRecord getStepsRecord() {
        return new StepsRecord.Builder(
                        new Metadata.Builder().build(), Instant.now(), Instant.now(), 10)
                .build();
    }
}
