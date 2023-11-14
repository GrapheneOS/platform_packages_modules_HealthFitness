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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.health.connect.datatypes.DataOrigin;
import android.healthconnect.cts.utils.TestUtils;
import android.os.Bundle;

import com.android.cts.install.lib.TestApp;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

public class MultiAppTestUtils {
    static final String TAG = "HealthConnectTest";
    public static final String QUERY_TYPE = "android.healthconnect.cts.queryType";
    public static final String INTENT_EXTRA_CALLING_PKG = "android.healthconnect.cts.calling_pkg";
    public static final String APP_PKG_NAME_USED_IN_DATA_ORIGIN =
            "android.healthconnect.cts.pkg.usedInDataOrigin";
    public static final String INSERT_RECORD_QUERY = "android.healthconnect.cts.insertRecord";
    public static final String READ_RECORDS_QUERY = "android.healthconnect.cts.readRecords";
    public static final String READ_RECORDS_SIZE = "android.healthconnect.cts.readRecordsNumber";
    public static final String READ_USING_DATA_ORIGIN_FILTERS =
            "android.healthconnect.cts.readUsingDataOriginFilters";

    public static final String DATA_ORIGIN_FILTER_PACKAGE_NAMES =
            "android.healthconnect.cts.dataOriginFilterPackageNames";
    public static final String READ_RECORD_CLASS_NAME =
            "android.healthconnect.cts.readRecordsClass";
    public static final String READ_CHANGE_LOGS_QUERY = "android.healthconnect.cts.readChangeLogs";
    public static final String CHANGE_LOGS_RESPONSE =
            "android.healthconnect.cts.changeLogsResponse";
    public static final String CHANGE_LOG_TOKEN = "android.healthconnect.cts.changeLogToken";
    public static final String SUCCESS = "android.healthconnect.cts.success";
    public static final String CLIENT_ID = "android.healthconnect.cts.clientId";
    public static final String RECORD_IDS = "android.healthconnect.cts.records";
    public static final String DELETE_RECORDS_QUERY = "android.healthconnect.cts.deleteRecords";
    public static final String UPDATE_RECORDS_QUERY = "android.healthconnect.cts.updateRecords";
    public static final String UPDATE_EXERCISE_ROUTE = "android.healthconnect.cts.updateRoute";

    public static final String UPSERT_EXERCISE_ROUTE = "android.healthconnect.cts.upsertRoute";
    public static final String GET_CHANGE_LOG_TOKEN_QUERY =
            "android.healthconnect.cts.getChangeLogToken";
    public static final String RECORD_TYPE = "android.healthconnect.cts.recordType";
    public static final String STEPS_RECORD = "android.healthconnect.cts.stepsRecord";
    public static final String EXERCISE_SESSION = "android.healthconnect.cts.exerciseSession";
    public static final String START_TIME = "android.healthconnect.cts.startTime";
    public static final String END_TIME = "android.healthconnect.cts.endTime";
    public static final String STEPS_COUNT = "android.healthconnect.cts.stepsCount";
    public static final String PAUSE_START = "android.healthconnect.cts.pauseStart";
    public static final String PAUSE_END = "android.healthconnect.cts.pauseEnd";
    public static final String INTENT_EXCEPTION = "android.healthconnect.cts.exception";
    private static final long POLLING_TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(20);

    public static Bundle insertRecordAs(TestApp testApp) throws Exception {
        Bundle bundle = new Bundle();
        bundle.putString(QUERY_TYPE, INSERT_RECORD_QUERY);

        return getFromTestApp(testApp, bundle);
    }

    public static Bundle deleteRecordsAs(
            TestApp testApp, List<TestUtils.RecordTypeAndRecordIds> listOfRecordIdsAndClass)
            throws Exception {
        Bundle bundle = new Bundle();
        bundle.putString(QUERY_TYPE, DELETE_RECORDS_QUERY);
        bundle.putSerializable(RECORD_IDS, (Serializable) listOfRecordIdsAndClass);

        return getFromTestApp(testApp, bundle);
    }

    public static Bundle updateRecordsAs(
            TestApp testAppToUpdateData,
            List<TestUtils.RecordTypeAndRecordIds> listOfRecordIdsAndClass)
            throws Exception {
        Bundle bundle = new Bundle();
        bundle.putString(QUERY_TYPE, UPDATE_RECORDS_QUERY);
        bundle.putSerializable(RECORD_IDS, (Serializable) listOfRecordIdsAndClass);

        return getFromTestApp(testAppToUpdateData, bundle);
    }

    public static Bundle updateRouteAs(TestApp testAppToUpdateData) throws Exception {
        Bundle bundle = new Bundle();
        bundle.putString(QUERY_TYPE, UPDATE_EXERCISE_ROUTE);
        return getFromTestApp(testAppToUpdateData, bundle);
    }

    public static Bundle insertSessionNoRouteAs(TestApp testAppToUpdateData) throws Exception {
        Bundle bundle = new Bundle();
        bundle.putString(QUERY_TYPE, UPSERT_EXERCISE_ROUTE);
        return getFromTestApp(testAppToUpdateData, bundle);
    }

    public static Bundle insertRecordWithAnotherAppPackageName(
            TestApp testAppToInsertData, TestApp testAppPkgNameUsed) throws Exception {
        Bundle bundle = new Bundle();
        bundle.putString(QUERY_TYPE, INSERT_RECORD_QUERY);
        bundle.putString(APP_PKG_NAME_USED_IN_DATA_ORIGIN, testAppPkgNameUsed.getPackageName());

        return getFromTestApp(testAppToInsertData, bundle);
    }

    public static Bundle readRecordsAs(TestApp testApp, ArrayList<String> recordClassesToRead)
            throws Exception {
        return readRecordsAs(
                testApp, recordClassesToRead, /* dataOriginFilterPackageNames= */ Optional.empty());
    }

    public static Bundle readRecordsAs(
            TestApp testApp,
            ArrayList<String> recordClassesToRead,
            Optional<List<String>> dataOriginFilterPackageNames)
            throws Exception {
        Bundle bundle = new Bundle();
        bundle.putString(QUERY_TYPE, READ_RECORDS_QUERY);
        bundle.putStringArrayList(READ_RECORD_CLASS_NAME, recordClassesToRead);
        if (!dataOriginFilterPackageNames.isEmpty()) {
            ArrayList<String> dataOrigins = new ArrayList<>();
            dataOrigins.addAll(dataOriginFilterPackageNames.get());
            bundle.putBoolean(READ_USING_DATA_ORIGIN_FILTERS, true);
            bundle.putStringArrayList(DATA_ORIGIN_FILTER_PACKAGE_NAMES, dataOrigins);
        }
        return getFromTestApp(testApp, bundle);
    }

    public static Bundle insertRecordWithGivenClientId(TestApp testApp, double clientId)
            throws Exception {
        Bundle bundle = new Bundle();
        bundle.putString(QUERY_TYPE, INSERT_RECORD_QUERY);
        bundle.putDouble(CLIENT_ID, clientId);

        return getFromTestApp(testApp, bundle);
    }

    public static Bundle readRecordsUsingDataOriginFiltersAs(
            TestApp testApp, ArrayList<String> recordClassesToRead) throws Exception {
        Bundle bundle = new Bundle();
        bundle.putString(QUERY_TYPE, READ_RECORDS_QUERY);
        bundle.putStringArrayList(READ_RECORD_CLASS_NAME, recordClassesToRead);
        bundle.putBoolean(READ_USING_DATA_ORIGIN_FILTERS, true);

        return getFromTestApp(testApp, bundle);
    }

    public static Bundle readChangeLogsUsingDataOriginFiltersAs(
            TestApp testApp, String changeLogToken) throws Exception {
        Bundle bundle = new Bundle();
        bundle.putString(QUERY_TYPE, READ_CHANGE_LOGS_QUERY);
        bundle.putString(CHANGE_LOG_TOKEN, changeLogToken);
        bundle.putBoolean(READ_USING_DATA_ORIGIN_FILTERS, true);

        return getFromTestApp(testApp, bundle);
    }

    public static Bundle getChangeLogTokenAs(
            TestApp testApp, String pkgName, ArrayList<String> recordClassesToRead)
            throws Exception {
        Bundle bundle = new Bundle();
        bundle.putString(QUERY_TYPE, GET_CHANGE_LOG_TOKEN_QUERY);
        bundle.putString(APP_PKG_NAME_USED_IN_DATA_ORIGIN, pkgName);

        if (recordClassesToRead != null) {
            bundle.putStringArrayList(READ_RECORD_CLASS_NAME, recordClassesToRead);
        }
        return getFromTestApp(testApp, bundle);
    }

    public static Bundle insertStepsRecordAs(
            TestApp testApp, String startTime, String endTime, int stepsCount) throws Exception {
        Bundle bundle = new Bundle();
        bundle.putString(QUERY_TYPE, INSERT_RECORD_QUERY);
        bundle.putString(RECORD_TYPE, STEPS_RECORD);
        bundle.putString(START_TIME, startTime);
        bundle.putString(END_TIME, endTime);
        bundle.putInt(STEPS_COUNT, stepsCount);

        return getFromTestApp(testApp, bundle);
    }

    public static Bundle insertExerciseSessionAs(
            TestApp testApp,
            String sessionStartTime,
            String sessionEndTime,
            String pauseStart,
            String pauseEnd)
            throws Exception {
        Bundle bundle = new Bundle();
        bundle.putString(QUERY_TYPE, INSERT_RECORD_QUERY);
        bundle.putString(RECORD_TYPE, EXERCISE_SESSION);
        bundle.putString(START_TIME, sessionStartTime);
        bundle.putString(END_TIME, sessionEndTime);
        bundle.putString(PAUSE_START, pauseStart);
        bundle.putString(PAUSE_END, pauseEnd);

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
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(INTENT_EXTRA_CALLING_PKG, getContext().getPackageName());
        intent.putExtras(bundleToCreateIntent);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.putExtras(bundleToCreateIntent);

        Thread.sleep(500);
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

    public static List<DataOrigin> getDataOriginPriorityOrder(TestApp testAppA, TestApp testAppB) {
        return List.of(
                new DataOrigin.Builder().setPackageName(testAppA.getPackageName()).build(),
                new DataOrigin.Builder().setPackageName(testAppB.getPackageName()).build());
    }
}
