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

import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_BASAL_METABOLIC_RATE;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_HEART_RATE;
import static android.healthconnect.datatypes.RecordTypeIdentifier.RECORD_TYPE_STEPS;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.healthconnect.HealthConnectException;
import android.healthconnect.HealthConnectManager;
import android.healthconnect.InsertRecordsResponse;
import android.healthconnect.datatypes.BasalMetabolicRateRecord;
import android.healthconnect.datatypes.HeartRateRecord;
import android.healthconnect.datatypes.Metadata;
import android.healthconnect.datatypes.Record;
import android.healthconnect.datatypes.RecordTypeIdentifier;
import android.healthconnect.datatypes.StepsRecord;
import android.healthconnect.datatypes.units.Power;
import android.os.OutcomeReceiver;
import android.platform.test.annotations.AppModeFull;
import android.util.ArraySet;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** CTS test for API provided by HealthConnectManager. */
@AppModeFull(reason = "HealthConnectManager is not accessible to instant apps")
@RunWith(AndroidJUnit4.class)
public class HealthConnectManagerTest {
    static final class RecordAndIdentifier {
        private final int id;
        private final Record recordClass;

        public RecordAndIdentifier(int id, Record recordClass) {
            this.id = id;
            this.recordClass = recordClass;
        }

        public int getId() {
            return id;
        }

        public Record getRecordClass() {
            return recordClass;
        }
    }

    private static final String TAG = "HealthConnectManagerTest";

    @Test
    public void testHCManagerIsAccessible_viaHCManager() {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();
    }

    @Test
    public void testHCManagerIsAccessible_viaContextConstant() {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();
    }

    @Test
    public void testAllRecordsAreBeingTested() {
        List<Record> records = getTestRecords();
        assertThat(records.size()).isEqualTo(RecordTypeIdentifier.class.getDeclaredFields().length);
        Set<Integer> recordIdSet = new ArraySet<>();
        records.forEach(
                (record -> {
                    assertThat(recordIdSet.contains(record.getRecordType())).isEqualTo(false);
                    recordIdSet.add(record.getRecordType());
                }));
    }

    @Test
    public void testAllIdentifiersAreBeingTested() {
        List<RecordAndIdentifier> recordAndIdentifiers = getRecordsAndIdentifiers();
        assertThat(recordAndIdentifiers.size())
                .isEqualTo(RecordTypeIdentifier.class.getDeclaredFields().length);
    }

    @Test
    public void testRecordIdentifiers() {
        for (RecordAndIdentifier recordAndIdentifier : getRecordsAndIdentifiers()) {
            assertThat(recordAndIdentifier.getRecordClass().getRecordType())
                    .isEqualTo(recordAndIdentifier.getId());
        }
    }

    @Test
    public void testInsertRecords() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        List<Record> records = getTestRecords();
        CountDownLatch latch = new CountDownLatch(1);
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();
        service.insertRecords(
                records,
                Executors.newSingleThreadExecutor(),
                new OutcomeReceiver<InsertRecordsResponse, HealthConnectException>() {
                    @Override
                    public void onResult(InsertRecordsResponse result) {
                        assertThat(result.getRecords()).hasSize(records.size());
                        latch.countDown();
                    }

                    @Override
                    public void onError(HealthConnectException exception) {
                        Log.e(TAG, exception.getMessage());
                    }
                });
        assertThat(latch.await(3, TimeUnit.SECONDS)).isEqualTo(true);
    }

    private List<Record> getTestRecords() {
        return Arrays.asList(getStepsRecord(), getHeartRateRecord(), getBasalMetabolicRateRecord());
    }

    private List<RecordAndIdentifier> getRecordsAndIdentifiers() {
        return Arrays.asList(
                new RecordAndIdentifier(RECORD_TYPE_STEPS, getStepsRecord()),
                new RecordAndIdentifier(RECORD_TYPE_HEART_RATE, getHeartRateRecord()),
                new RecordAndIdentifier(
                        RECORD_TYPE_BASAL_METABOLIC_RATE, getBasalMetabolicRateRecord()));
    }

    private StepsRecord getStepsRecord() {
        return new StepsRecord.Builder(
                        new Metadata.Builder().build(), Instant.now(), Instant.now(), 10)
                .build();
    }

    private HeartRateRecord getHeartRateRecord() {
        HeartRateRecord.HeartRateSample heartRateSample =
                new HeartRateRecord.HeartRateSample(72, Instant.now());
        ArrayList<HeartRateRecord.HeartRateSample> heartRateSamples = new ArrayList<>();
        heartRateSamples.add(heartRateSample);
        heartRateSamples.add(heartRateSample);

        return new HeartRateRecord.Builder(
                        new Metadata.Builder().build(),
                        Instant.now(),
                        Instant.now(),
                        heartRateSamples)
                .build();
    }

    private BasalMetabolicRateRecord getBasalMetabolicRateRecord() {
        return new BasalMetabolicRateRecord.Builder(
                        new Metadata.Builder().build(), Instant.now(), Power.fromWatts(100.0))
                .build();
    }
}
