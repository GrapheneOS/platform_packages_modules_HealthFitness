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

package android.healthconnect.cts.lib;

import static androidx.test.InstrumentationRegistry.getContext;

import static com.google.common.truth.Truth.assertThat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.health.connect.HealthConnectException;
import android.health.connect.HealthConnectManager;
import android.health.connect.InsertRecordsResponse;
import android.health.connect.datatypes.BasalMetabolicRateRecord;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.Device;
import android.health.connect.datatypes.HeartRateRecord;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.StepsRecord;
import android.health.connect.datatypes.units.Power;
import android.os.Bundle;
import android.os.OutcomeReceiver;
import android.util.Log;

import com.android.cts.install.lib.TestApp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

public class TestUtils {
    static final String TAG = "HealthConnectTest";
    public static final String QUERY_TYPE = "android.healthconnect.cts.queryType";
    public static final String INTENT_EXTRA_CALLING_PKG = "android.healthconnect.cts.calling_pkg";
    public static final String INSERT_RECORD_QUERY = "android.healthconnect.cts.insertRecord";
    public static final String INTENT_EXCEPTION = "android.healthconnect.cts.exception";
    public static final String SUCCESS = "android.healthconnect.cts.success";
    private static final long POLLING_TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(20);

    public static Bundle insertRecordAs(TestApp testApp) throws Exception {
        Bundle bundle = new Bundle();
        bundle.putString(QUERY_TYPE, INSERT_RECORD_QUERY);
        return getFromTestApp(testApp, bundle);
    }

    private static Bundle getFromTestApp(TestApp testApp, Bundle bundleToCreateIntent)
            throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Bundle> response = new AtomicReference<>();
        AtomicReference<Exception> exceptionAtomicReference = new AtomicReference<>();
        BroadcastReceiver broadcastReceiver =
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if (intent.hasExtra(INTENT_EXCEPTION)) {
                            exceptionAtomicReference.set(
                                    (Exception) (intent.getSerializableExtra(INTENT_EXCEPTION)));
                        } else {
                            response.set(intent.getExtras());
                        }
                        latch.countDown();
                    }
                };

        launchTestApp(testApp, bundleToCreateIntent, broadcastReceiver, latch);
        if (exceptionAtomicReference.get() != null) {
            throw exceptionAtomicReference.get();
        }
        return response.get();
    }

    private static void launchTestApp(
            TestApp testApp,
            Bundle bundleToCreateIntent,
            BroadcastReceiver broadcastReceiver,
            CountDownLatch latch)
            throws Exception {

        // Register broadcast receiver
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(bundleToCreateIntent.getString(QUERY_TYPE));
        intentFilter.addCategory(Intent.CATEGORY_DEFAULT);
        getContext().registerReceiver(broadcastReceiver, intentFilter, Context.RECEIVER_EXPORTED);

        // Launch the test app.
        final Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setPackage(testApp.getPackageName());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(INTENT_EXTRA_CALLING_PKG, getContext().getPackageName());
        intent.putExtras(bundleToCreateIntent);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        getContext().startActivity(intent);
        if (!latch.await(POLLING_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
            final String errorMessage =
                    "Timed out while waiting to receive "
                            + bundleToCreateIntent.getString(QUERY_TYPE)
                            + " intent from "
                            + testApp.getPackageName();
            throw new TimeoutException(errorMessage);
        }
        getContext().unregisterReceiver(broadcastReceiver);
    }

    public static String insertRecordAndGetId(Record record, Context context)
            throws InterruptedException {
        return insertRecords(Collections.singletonList(record), context)
                .get(0)
                .getMetadata()
                .getId();
    }

    public static List<Record> insertRecords(List<Record> records, Context context)
            throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();
        AtomicReference<List<Record>> response = new AtomicReference<>();
        AtomicReference<HealthConnectException> exceptionAtomicReference = new AtomicReference<>();
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
                        exceptionAtomicReference.set(exception);
                        latch.countDown();
                    }
                });
        assertThat(latch.await(3, TimeUnit.SECONDS)).isTrue();
        if (exceptionAtomicReference.get() != null) {
            throw exceptionAtomicReference.get();
        }
        assertThat(response.get()).hasSize(records.size());

        return response.get();
    }

    public static List<Record> getTestRecords(String packageName) {
        return Arrays.asList(
                getStepsRecord(packageName),
                getHeartRateRecord(packageName),
                getBasalMetabolicRateRecord(packageName));
    }

    public static StepsRecord getStepsRecord(String packageName) {
        Device device =
                new Device.Builder().setManufacturer("google").setModel("Pixel").setType(1).build();
        DataOrigin dataOrigin = new DataOrigin.Builder().setPackageName(packageName).build();
        return new StepsRecord.Builder(
                        new Metadata.Builder()
                                .setDevice(device)
                                .setDataOrigin(dataOrigin)
                                .setClientRecordId("SR" + Math.random())
                                .build(),
                        Instant.now(),
                        Instant.now().plusMillis(1000),
                        10)
                .build();
    }

    public static HeartRateRecord getHeartRateRecord(String packageName) {
        HeartRateRecord.HeartRateSample heartRateSample =
                new HeartRateRecord.HeartRateSample(72, Instant.now().plusMillis(100));
        ArrayList<HeartRateRecord.HeartRateSample> heartRateSamples = new ArrayList<>();
        heartRateSamples.add(heartRateSample);
        heartRateSamples.add(heartRateSample);
        Device device =
                new Device.Builder().setManufacturer("google").setModel("Pixel").setType(1).build();
        DataOrigin dataOrigin = new DataOrigin.Builder().setPackageName(packageName).build();

        return new HeartRateRecord.Builder(
                        new Metadata.Builder()
                                .setDevice(device)
                                .setDataOrigin(dataOrigin)
                                .setClientRecordId("HR" + Math.random())
                                .build(),
                        Instant.now(),
                        Instant.now().plusMillis(500),
                        heartRateSamples)
                .build();
    }

    public static BasalMetabolicRateRecord getBasalMetabolicRateRecord(String packageName) {
        Device device =
                new Device.Builder()
                        .setManufacturer("google")
                        .setModel("Pixel4a")
                        .setType(2)
                        .build();
        DataOrigin dataOrigin = new DataOrigin.Builder().setPackageName(packageName).build();
        return new BasalMetabolicRateRecord.Builder(
                        new Metadata.Builder()
                                .setDevice(device)
                                .setDataOrigin(dataOrigin)
                                .setClientRecordId("BMR" + Math.random())
                                .build(),
                        Instant.now(),
                        Power.fromWatts(100.0))
                .build();
    }
}
