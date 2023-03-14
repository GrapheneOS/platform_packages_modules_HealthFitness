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

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.MIGRATE_HEALTH_CONNECT_DATA;
import static android.health.connect.HealthConnectDataState.MIGRATION_STATE_IDLE;
import static android.health.connect.HealthConnectDataState.RESTORE_ERROR_FETCHING_DATA;
import static android.health.connect.HealthConnectDataState.RESTORE_ERROR_NONE;
import static android.health.connect.HealthConnectDataState.RESTORE_STATE_IDLE;
import static android.health.connect.HealthConnectDataState.RESTORE_STATE_PENDING;
import static android.health.connect.HealthConnectManager.DATA_DOWNLOAD_COMPLETE;
import static android.health.connect.HealthConnectManager.DATA_DOWNLOAD_FAILED;
import static android.health.connect.HealthConnectManager.DATA_DOWNLOAD_STARTED;
import static android.health.connect.HealthConnectManager.isHealthPermission;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_BASAL_METABOLIC_RATE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_HEART_RATE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_STEPS;
import static android.health.connect.datatypes.StepsRecord.STEPS_COUNT_TOTAL;
import static android.healthconnect.cts.TestUtils.MANAGE_HEALTH_DATA;

import static com.android.compatibility.common.util.SystemUtil.runWithShellPermissionIdentity;

import static com.google.common.truth.Truth.assertThat;

import android.Manifest;
import android.app.UiAutomation;
import android.content.Context;
import android.health.connect.AggregateRecordsRequest;
import android.health.connect.HealthConnectDataState;
import android.health.connect.HealthConnectException;
import android.health.connect.HealthConnectManager;
import android.health.connect.HealthPermissions;
import android.health.connect.ReadRecordsRequestUsingIds;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.changelog.ChangeLogTokenRequest;
import android.health.connect.changelog.ChangeLogsRequest;
import android.health.connect.datatypes.BasalMetabolicRateRecord;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.Device;
import android.health.connect.datatypes.HeartRateRecord;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.StepsRecord;
import android.health.connect.datatypes.units.Power;
import android.health.connect.restore.StageRemoteDataException;
import android.os.OutcomeReceiver;
import android.os.ParcelFileDescriptor;
import android.platform.test.annotations.AppModeFull;
import android.util.ArrayMap;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.time.Instant;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** CTS test for API provided by HealthConnectManager. */
@AppModeFull(reason = "HealthConnectManager is not accessible to instant apps")
@RunWith(AndroidJUnit4.class)
public class HealthConnectManagerTest {
    private static final String TAG = "HealthConnectManagerTest";

    @Before
    public void setUp() {
        TestUtils.deleteAllStagedRemoteData();
    }

    @After
    public void tearDown() {
        TestUtils.deleteAllStagedRemoteData();
    }

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
    public void testRecordIdentifiers() {
        for (TestUtils.RecordAndIdentifier recordAndIdentifier :
                TestUtils.getRecordsAndIdentifiers()) {
            assertThat(recordAndIdentifier.getRecordClass().getRecordType())
                    .isEqualTo(recordAndIdentifier.getId());
        }
    }

    @Test
    public void testIsHealthPermission_forHealthPermission_returnsTrue() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        assertThat(isHealthPermission(context, HealthPermissions.READ_ACTIVE_CALORIES_BURNED))
                .isTrue();
        assertThat(isHealthPermission(context, HealthPermissions.READ_ACTIVE_CALORIES_BURNED))
                .isTrue();
    }

    @Test
    public void testIsHealthPermission_forNonHealthGroupPermission_returnsFalse() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        assertThat(isHealthPermission(context, HealthPermissions.MANAGE_HEALTH_PERMISSIONS))
                .isFalse();
        assertThat(isHealthPermission(context, CAMERA)).isFalse();
    }

    /*
    @Test
    public void test_getRecordTypeInfo() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        CountDownLatch latch = new CountDownLatch(1);
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();
        AtomicReference<Map<Class<? extends Record>, RecordTypeInfoResponse>> response =
                new AtomicReference<>();
        AtomicReference<HealthConnectException> responseException = new AtomicReference<>();

        service.queryAllRecordTypesInfo(
                Executors.newSingleThreadExecutor(),
                new OutcomeReceiver<
                        Map<Class<? extends Record>, RecordTypeInfoResponse>,
                        HealthConnectException>() {
                    @Override
                    public void onResult(
                            Map<Class<? extends Record>, RecordTypeInfoResponse> result) {
                        response.set(result);
                        latch.countDown();
                    }

                    @Override
                    public void onError(HealthConnectException exception) {
                        responseException.set(exception);
                        latch.countDown();
                    }
                });
        assertThat(latch.await(3, TimeUnit.SECONDS)).isEqualTo(true);
        assertThat(responseException.get()).isNull();
        assertThat(response).isNotNull();
        Log.d(TAG, "GetDataTypeInfoResponse : \n");
        response.get()
                .forEach(
                        (recordTypeClass, recordTypeInfoResponse) -> {
                            StringBuilder builder =
                                    new StringBuilder(recordTypeClass.getTypeName() + " : ");
                            builder.append(
                                    " HealthPermissionCategory : "
                                            + recordTypeInfoResponse.getPermissionCategory());
                            builder.append(
                                    " HealthDataCategory : "
                                            + recordTypeInfoResponse.getDataCategory());
                            builder.append(
                                    " Contributing Packages : "
                                            + String.join(
                                                    ",",
                                                    recordTypeInfoResponse
                                                            .getContributingPackages()
                                                            .stream()
                                                            .map(DataOrigin::getPackageName)
                                                            .collect(Collectors.toList())));
                            builder.append("\n");
                            Log.d(TAG, builder.toString());
                        });
    }
    */

    /**
     * Test to verify the working of {@link HealthConnectManager#updateRecords(java.util.List,
     * java.util.concurrent.Executor, android.os.OutcomeReceiver)}.
     *
     * <p>Insert a sample record of each dataType, update them and check by reading them.
     */
    @Test
    public void testUpdateRecords_validInput_dataBaseUpdatedSuccessfully()
            throws InterruptedException,
                    InvocationTargetException,
                    InstantiationException,
                    IllegalAccessException,
                    NoSuchMethodException {

        Context context = ApplicationProvider.getApplicationContext();
        CountDownLatch latch = new CountDownLatch(1);
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();
        AtomicReference<HealthConnectException> responseException = new AtomicReference<>();

        // Insert a sample record of each data type.
        List<Record> insertRecords = TestUtils.insertRecords(TestUtils.getTestRecords());

        // read inserted records and verify that the data is same as inserted.

        // Generate a second set of records that will be used to perform the update operation.
        List<Record> updateRecords = getTestRecords(/* isSetClientRecordId */ false);

        // Modify the Uid of the updateRecords to the uuid that was present in the insert records.
        for (int itr = 0; itr < updateRecords.size(); itr++) {
            updateRecords.set(
                    itr,
                    setTestRecordId(
                            updateRecords.get(itr), insertRecords.get(itr).getMetadata().getId()));
        }

        service.updateRecords(
                updateRecords,
                Executors.newSingleThreadExecutor(),
                new OutcomeReceiver<Void, HealthConnectException>() {
                    @Override
                    public void onResult(Void result) {
                        latch.countDown();
                    }

                    @Override
                    public void onError(HealthConnectException exception) {
                        responseException.set(exception);
                        latch.countDown();
                        Log.e(
                                TAG,
                                "Exception: "
                                        + exception.getMessage()
                                        + ", error code: "
                                        + exception.getErrorCode());
                    }
                });

        // assert the inserted data has been modified per the updateRecords.
        assertThat(latch.await(3, TimeUnit.SECONDS)).isEqualTo(true);

        // assert the inserted data has been modified by reading the data.
        // TODO(b/260181009): Modify and complete Tests for Update API in HealthConnectManagerTest
        //  using read API

        assertThat(responseException.get()).isNull();
    }

    /**
     * Test to verify the working of {@link HealthConnectManager#updateRecords(java.util.List,
     * java.util.concurrent.Executor, android.os.OutcomeReceiver)}.
     *
     * <p>Insert a sample record of each dataType, while updating provide input with a few invalid
     * records. These records will have UUIDs that are not present in the table. Since this record
     * won't be updated, the transaction should fail and revert and no other record(even though
     * valid inputs) should not be modified either.
     */
    @Test
    public void testUpdateRecords_invalidInputRecords_noChangeInDataBase()
            throws InterruptedException,
                    InvocationTargetException,
                    InstantiationException,
                    IllegalAccessException,
                    NoSuchMethodException {

        Context context = ApplicationProvider.getApplicationContext();
        CountDownLatch latch = new CountDownLatch(1);
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();
        AtomicReference<HealthConnectException> responseException = new AtomicReference<>();

        // Insert a sample record of each data type.
        List<Record> insertRecords = TestUtils.insertRecords(TestUtils.getTestRecords());

        // read inserted records and verify that the data is same as inserted.

        // Generate a second set of records that will be used to perform the update operation.
        List<Record> updateRecords = getTestRecords(/* isSetClientRecordId */ false);

        // Modify the Uid of the updateRecords to the UUID that was present in the insert records,
        // leaving out alternate records so that they have a new UUID which is not present in the
        // dataBase.
        for (int itr = 0; itr < updateRecords.size(); itr++) {
            updateRecords.set(
                    itr,
                    setTestRecordId(
                            updateRecords.get(itr),
                            itr % 2 == 0
                                    ? insertRecords.get(itr).getMetadata().getId()
                                    : String.valueOf(Math.random())));
        }

        // perform the update operation.
        service.updateRecords(
                updateRecords,
                Executors.newSingleThreadExecutor(),
                new OutcomeReceiver<Void, HealthConnectException>() {
                    @Override
                    public void onResult(Void result) {}

                    @Override
                    public void onError(HealthConnectException exception) {
                        responseException.set(exception);
                        latch.countDown();
                        Log.e(
                                TAG,
                                "Exception: "
                                        + exception.getMessage()
                                        + ", error code: "
                                        + exception.getErrorCode());
                    }
                });

        assertThat(latch.await(/* timeout */ 3, TimeUnit.SECONDS)).isEqualTo(true);

        // assert the inserted data has not been modified by reading the data.
        // TODO(b/260181009): Modify and complete Tests for Update API in HealthConnectManagerTest
        //  using read API

        // verify that the testcase failed due to invalid argument exception.
        assertThat(responseException.get()).isNotNull();
        assertThat(responseException.get().getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_INVALID_ARGUMENT);
    }

    /**
     * Test to verify the working of {@link HealthConnectManager#updateRecords(java.util.List,
     * java.util.concurrent.Executor, android.os.OutcomeReceiver)}.
     *
     * <p>Insert a sample record of each dataType, while updating add an input record with an
     * invalid packageName. Since this is an invalid record the transaction should fail and revert
     * and no other record(even though valid inputs) should not be modified either.
     */
    @Test
    public void testUpdateRecords_recordWithInvalidPackageName_noChangeInDataBase()
            throws InterruptedException,
                    InvocationTargetException,
                    InstantiationException,
                    IllegalAccessException,
                    NoSuchMethodException {

        Context context = ApplicationProvider.getApplicationContext();
        CountDownLatch latch = new CountDownLatch(1);
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();
        AtomicReference<Exception> responseException = new AtomicReference<>();

        // Insert a sample record of each data type.
        List<Record> insertRecords = TestUtils.insertRecords(TestUtils.getTestRecords());

        // read inserted records and verify that the data is same as inserted.

        // Generate a second set of records that will be used to perform the update operation.
        List<Record> updateRecords = getTestRecords(/* isSetClientRecordId */ false);

        // Modify the Uuid of the updateRecords to the uuid that was present in the insert records.
        for (int itr = 0; itr < updateRecords.size(); itr++) {
            updateRecords.set(
                    itr,
                    setTestRecordId(
                            updateRecords.get(itr), insertRecords.get(itr).getMetadata().getId()));
            //             adding an entry with invalid packageName.
            if (updateRecords.get(itr).getRecordType() == RECORD_TYPE_STEPS) {
                updateRecords.set(
                        itr, getStepsRecord(false, /* incorrectPackageName */ "abc.xyz.pqr"));
            }
        }

        try {
            // perform the update operation.
            service.updateRecords(
                    updateRecords,
                    Executors.newSingleThreadExecutor(),
                    new OutcomeReceiver<Void, HealthConnectException>() {
                        @Override
                        public void onResult(Void result) {
                            latch.countDown();
                        }

                        @Override
                        public void onError(HealthConnectException exception) {
                            responseException.set(exception);
                            latch.countDown();
                            Log.e(
                                    TAG,
                                    "Exception: "
                                            + exception.getMessage()
                                            + ", error code: "
                                            + exception.getErrorCode());
                        }
                    });

        } catch (Exception exception) {
            latch.countDown();
            responseException.set(exception);
        }
        assertThat(latch.await(/* timeout */ 3, TimeUnit.SECONDS)).isEqualTo(true);

        // assert the inserted data has not been modified by reading the data.
        // TODO(b/260181009): Modify and complete Tests for Update API in HealthConnectManagerTest
        //  using read API

        // verify that the testcase failed due to invalid argument exception.
        assertThat(responseException.get()).isNotNull();
        assertThat(responseException.get().getClass()).isEqualTo(IllegalArgumentException.class);
    }

    @Test
    public void testAutoDeleteApis() throws InterruptedException {
        UiAutomation uiAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation();

        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();
        TestUtils.setAutoDeletePeriod(30);
        uiAutomation.adoptShellPermissionIdentity(MANAGE_HEALTH_DATA);
        try {
            assertThat(service.getRecordRetentionPeriodInDays()).isEqualTo(30);
        } finally {
            uiAutomation.dropShellPermissionIdentity();
        }

        TestUtils.setAutoDeletePeriod(0);
        uiAutomation.adoptShellPermissionIdentity(MANAGE_HEALTH_DATA);
        try {
            assertThat(service.getRecordRetentionPeriodInDays()).isEqualTo(0);
        } finally {
            uiAutomation.dropShellPermissionIdentity();
        }
    }

    @Test
    public void testStageRemoteData_withValidInput_noExceptionsReturned() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();

        try {
            File dataDir = context.getDataDir();
            File testRestoreFile1 = createAndGetNonEmptyFile(dataDir, "testRestoreFile1");
            File testRestoreFile2 = createAndGetNonEmptyFile(dataDir, "testRestoreFile2");

            assertThat(testRestoreFile1.exists()).isTrue();
            assertThat(testRestoreFile2.exists()).isTrue();

            Map<String, ParcelFileDescriptor> pfdsByFileName = new ArrayMap<>();
            pfdsByFileName.put(
                    testRestoreFile1.getName(),
                    ParcelFileDescriptor.open(
                            testRestoreFile1, ParcelFileDescriptor.MODE_READ_ONLY));
            pfdsByFileName.put(
                    testRestoreFile2.getName(),
                    ParcelFileDescriptor.open(
                            testRestoreFile2, ParcelFileDescriptor.MODE_READ_ONLY));

            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .adoptShellPermissionIdentity(
                            "android.permission.STAGE_HEALTH_CONNECT_REMOTE_DATA");
            service.stageAllHealthConnectRemoteData(
                    pfdsByFileName,
                    Executors.newSingleThreadExecutor(),
                    new OutcomeReceiver<>() {
                        @Override
                        public void onResult(Void result) {
                            latch.countDown();
                        }

                        @Override
                        public void onError(@NonNull StageRemoteDataException error) {}
                    });
        } catch (IOException e) {
            Log.e(TAG, "Error creating / writing to test files.", e);
        } finally {
            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .dropShellPermissionIdentity();
        }

        assertThat(latch.await(10, TimeUnit.SECONDS)).isEqualTo(true);

        deleteAllStagedRemoteData();
    }

    @Test
    public void testStageRemoteData_whenNotReadMode_errorIoReturned() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Map<String, HealthConnectException>> observedExceptionsByFileName =
                new AtomicReference<>();
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();

        try {
            File dataDir = context.getDataDir();
            File testRestoreFile1 = createAndGetNonEmptyFile(dataDir, "testRestoreFile1");
            File testRestoreFile2 = createAndGetNonEmptyFile(dataDir, "testRestoreFile2");

            assertThat(testRestoreFile1.exists()).isTrue();
            assertThat(testRestoreFile2.exists()).isTrue();

            Map<String, ParcelFileDescriptor> pfdsByFileName = new ArrayMap<>();
            pfdsByFileName.put(
                    testRestoreFile1.getName(),
                    ParcelFileDescriptor.open(
                            testRestoreFile1, ParcelFileDescriptor.MODE_WRITE_ONLY));
            pfdsByFileName.put(
                    testRestoreFile2.getName(),
                    ParcelFileDescriptor.open(
                            testRestoreFile2, ParcelFileDescriptor.MODE_READ_ONLY));

            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .adoptShellPermissionIdentity(
                            "android.permission.STAGE_HEALTH_CONNECT_REMOTE_DATA");
            service.stageAllHealthConnectRemoteData(
                    pfdsByFileName,
                    Executors.newSingleThreadExecutor(),
                    new OutcomeReceiver<>() {
                        @Override
                        public void onResult(Void unused) {}

                        @Override
                        public void onError(@NonNull StageRemoteDataException error) {
                            observedExceptionsByFileName.set(error.getExceptionsByFileNames());
                            latch.countDown();
                        }
                    });
        } catch (IOException e) {
            Log.e(TAG, "Error creating / writing to test files.", e);
        } finally {
            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .dropShellPermissionIdentity();
        }

        assertThat(latch.await(10, TimeUnit.SECONDS)).isEqualTo(true);
        assertThat(observedExceptionsByFileName.get()).isNotNull();
        assertThat(observedExceptionsByFileName.get().size()).isEqualTo(1);
        assertThat(
                        observedExceptionsByFileName.get().entrySet().stream()
                                .findFirst()
                                .get()
                                .getKey())
                .isEqualTo("testRestoreFile1");
        assertThat(
                        observedExceptionsByFileName.get().entrySet().stream()
                                .findFirst()
                                .get()
                                .getValue()
                                .getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_IO);

        deleteAllStagedRemoteData();
    }

    @Test
    public void testStageRemoteData_whenStagingStagedData_noExceptionsReturned() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch retryLatch = new CountDownLatch(1);
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();

        try {
            File dataDir = context.getDataDir();
            File testRestoreFile1 = createAndGetNonEmptyFile(dataDir, "testRestoreFile1");
            File testRestoreFile2 = createAndGetNonEmptyFile(dataDir, "testRestoreFile2");

            assertThat(testRestoreFile1.exists()).isTrue();
            assertThat(testRestoreFile2.exists()).isTrue();

            Map<String, ParcelFileDescriptor> pfdsByFileName = new ArrayMap<>();
            pfdsByFileName.put(
                    testRestoreFile1.getName(),
                    ParcelFileDescriptor.open(
                            testRestoreFile1, ParcelFileDescriptor.MODE_READ_ONLY));
            pfdsByFileName.put(
                    testRestoreFile2.getName(),
                    ParcelFileDescriptor.open(
                            testRestoreFile2, ParcelFileDescriptor.MODE_READ_ONLY));

            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .adoptShellPermissionIdentity(
                            "android.permission.STAGE_HEALTH_CONNECT_REMOTE_DATA");
            service.stageAllHealthConnectRemoteData(
                    pfdsByFileName,
                    Executors.newSingleThreadExecutor(),
                    new OutcomeReceiver<>() {
                        @Override
                        public void onResult(Void result) {
                            latch.countDown();
                        }

                        @Override
                        public void onError(@NonNull StageRemoteDataException error) {}
                    });
            assertThat(latch.await(10, TimeUnit.SECONDS)).isEqualTo(true);

            // send the files again
            service.stageAllHealthConnectRemoteData(
                    pfdsByFileName,
                    Executors.newSingleThreadExecutor(),
                    new OutcomeReceiver<>() {
                        @Override
                        public void onResult(Void result) {
                            retryLatch.countDown();
                        }

                        @Override
                        public void onError(@NonNull StageRemoteDataException error) {}
                    });
        } catch (IOException e) {
            Log.e(TAG, "Error creating / writing to test files.", e);
        } finally {
            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .dropShellPermissionIdentity();
        }

        assertThat(retryLatch.await(10, TimeUnit.SECONDS)).isEqualTo(true);

        deleteAllStagedRemoteData();
    }

    @Test
    public void testStageRemoteData_withoutPermission_throwsSecurityException() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();

        try {
            File dataDir = context.getDataDir();
            File testRestoreFile1 = createAndGetNonEmptyFile(dataDir, "testRestoreFile1");
            File testRestoreFile2 = createAndGetNonEmptyFile(dataDir, "testRestoreFile2");

            assertThat(testRestoreFile1.exists()).isTrue();
            assertThat(testRestoreFile2.exists()).isTrue();

            Map<String, ParcelFileDescriptor> pfdsByFileName = new ArrayMap<>();
            pfdsByFileName.put(
                    testRestoreFile1.getName(),
                    ParcelFileDescriptor.open(
                            testRestoreFile1, ParcelFileDescriptor.MODE_READ_ONLY));
            pfdsByFileName.put(
                    testRestoreFile2.getName(),
                    ParcelFileDescriptor.open(
                            testRestoreFile2, ParcelFileDescriptor.MODE_READ_ONLY));
            service.stageAllHealthConnectRemoteData(
                    pfdsByFileName,
                    Executors.newSingleThreadExecutor(),
                    new OutcomeReceiver<>() {
                        @Override
                        public void onResult(Void result) {}

                        @Override
                        public void onError(@NonNull StageRemoteDataException error) {}
                    });
        } catch (SecurityException e) {
            /* pass */
        } catch (IOException e) {
            Log.e(TAG, "Error creating / writing to test files.", e);
        }

        deleteAllStagedRemoteData();
    }

    @Test
    public void testUpdateDataDownloadState_withoutPermission_throwsSecurityException() {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();

        try {
            service.updateDataDownloadState(DATA_DOWNLOAD_STARTED);
        } catch (SecurityException e) {
            /* pass */
        }
    }

    @Test
    public void testGetHealthConnectDataState_beforeDownload_returnsIdleState() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<HealthConnectDataState> returnedHealthConnectDataState =
                new AtomicReference<>();
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();

        try {
            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .adoptShellPermissionIdentity(MANAGE_HEALTH_DATA);
            service.getHealthConnectDataState(
                    Executors.newSingleThreadExecutor(),
                    new OutcomeReceiver<>() {
                        @Override
                        public void onResult(HealthConnectDataState healthConnectDataState) {
                            returnedHealthConnectDataState.set(healthConnectDataState);
                            latch.countDown();
                        }

                        @Override
                        public void onError(@NonNull HealthConnectException e) {}
                    });
        } finally {
            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .dropShellPermissionIdentity();
        }
        assertThat(latch.await(10, TimeUnit.SECONDS)).isEqualTo(true);
        assertThat(returnedHealthConnectDataState.get().getDataRestoreState())
                .isEqualTo(RESTORE_STATE_IDLE);
        deleteAllStagedRemoteData();
    }

    @Test
    public void
            testGetHealthConnectDataState_beforeDownload_withMigrationPermission_returnsIdleState()
                    throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<HealthConnectDataState> returnedHealthConnectDataState =
                new AtomicReference<>();
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();

        try {
            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .adoptShellPermissionIdentity(MIGRATE_HEALTH_CONNECT_DATA);
            service.getHealthConnectDataState(
                    Executors.newSingleThreadExecutor(),
                    new OutcomeReceiver<>() {
                        @Override
                        public void onResult(HealthConnectDataState healthConnectDataState) {
                            returnedHealthConnectDataState.set(healthConnectDataState);
                            latch.countDown();
                        }

                        @Override
                        public void onError(@NonNull HealthConnectException e) {}
                    });
        } finally {
            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .dropShellPermissionIdentity();
        }
        assertThat(latch.await(10, TimeUnit.SECONDS)).isEqualTo(true);
        assertThat(returnedHealthConnectDataState.get().getDataRestoreState())
                .isEqualTo(RESTORE_STATE_IDLE);
        deleteAllStagedRemoteData();
    }

    @Test
    public void testGetHealthConnectDataState_duringDownload_returnsRestorePendingState()
            throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<HealthConnectDataState> returnedHealthConnectDataState =
                new AtomicReference<>();
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();

        try {
            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .adoptShellPermissionIdentity(
                            "android.permission.STAGE_HEALTH_CONNECT_REMOTE_DATA",
                            MANAGE_HEALTH_DATA);
            service.updateDataDownloadState(DATA_DOWNLOAD_STARTED);
            service.getHealthConnectDataState(
                    Executors.newSingleThreadExecutor(),
                    new OutcomeReceiver<>() {
                        @Override
                        public void onResult(HealthConnectDataState healthConnectDataState) {
                            returnedHealthConnectDataState.set(healthConnectDataState);
                            latch.countDown();
                        }

                        @Override
                        public void onError(@NonNull HealthConnectException e) {}
                    });
        } finally {
            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .dropShellPermissionIdentity();
        }

        assertThat(latch.await(10, TimeUnit.SECONDS)).isEqualTo(true);
        assertThat(returnedHealthConnectDataState.get().getDataRestoreState())
                .isEqualTo(RESTORE_STATE_PENDING);
        deleteAllStagedRemoteData();
    }

    @Test
    public void testGetHealthConnectDataState_whenDownloadDone_returnsRestorePendingState()
            throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<HealthConnectDataState> returnedHealthConnectDataState =
                new AtomicReference<>();
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();

        try {
            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .adoptShellPermissionIdentity(
                            "android.permission.STAGE_HEALTH_CONNECT_REMOTE_DATA",
                            MANAGE_HEALTH_DATA);
            service.updateDataDownloadState(DATA_DOWNLOAD_COMPLETE);
            service.getHealthConnectDataState(
                    Executors.newSingleThreadExecutor(),
                    new OutcomeReceiver<>() {
                        @Override
                        public void onResult(HealthConnectDataState healthConnectDataState) {
                            returnedHealthConnectDataState.set(healthConnectDataState);
                            latch.countDown();
                        }

                        @Override
                        public void onError(@NonNull HealthConnectException e) {}
                    });
        } finally {
            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .dropShellPermissionIdentity();
        }

        assertThat(latch.await(10, TimeUnit.SECONDS)).isEqualTo(true);
        assertThat(returnedHealthConnectDataState.get().getDataRestoreState())
                .isEqualTo(RESTORE_STATE_PENDING);
        deleteAllStagedRemoteData();
    }

    @Test
    public void testGetHealthConnectDataState_whenDownloadFailed_returnsIdleState()
            throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<HealthConnectDataState> returnedHealthConnectDataState =
                new AtomicReference<>();
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();

        try {
            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .adoptShellPermissionIdentity(
                            "android.permission.STAGE_HEALTH_CONNECT_REMOTE_DATA",
                            MANAGE_HEALTH_DATA);
            service.updateDataDownloadState(DATA_DOWNLOAD_FAILED);
            service.getHealthConnectDataState(
                    Executors.newSingleThreadExecutor(),
                    new OutcomeReceiver<>() {
                        @Override
                        public void onResult(HealthConnectDataState healthConnectDataState) {
                            returnedHealthConnectDataState.set(healthConnectDataState);
                            latch.countDown();
                        }

                        @Override
                        public void onError(@NonNull HealthConnectException e) {}
                    });
        } finally {
            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .dropShellPermissionIdentity();
        }
        assertThat(latch.await(10, TimeUnit.SECONDS)).isEqualTo(true);
        assertThat(returnedHealthConnectDataState.get().getDataRestoreState())
                .isEqualTo(RESTORE_STATE_IDLE);

        deleteAllStagedRemoteData();
    }

    @Test
    public void testGetHealthConnectDataState_afterStaging_returnsRestorePendingState()
            throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch stateLatch = new CountDownLatch(1);
        AtomicReference<HealthConnectDataState> returnedHealthConnectDataState =
                new AtomicReference<>();
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();

        try {
            File dataDir = context.getDataDir();
            File testRestoreFile1 = createAndGetNonEmptyFile(dataDir, "testRestoreFile1");
            File testRestoreFile2 = createAndGetNonEmptyFile(dataDir, "testRestoreFile2");

            assertThat(testRestoreFile1.exists()).isTrue();
            assertThat(testRestoreFile2.exists()).isTrue();

            Map<String, ParcelFileDescriptor> pfdsByFileName = new ArrayMap<>();
            pfdsByFileName.put(
                    testRestoreFile1.getName(),
                    ParcelFileDescriptor.open(
                            testRestoreFile1, ParcelFileDescriptor.MODE_READ_ONLY));
            pfdsByFileName.put(
                    testRestoreFile2.getName(),
                    ParcelFileDescriptor.open(
                            testRestoreFile2, ParcelFileDescriptor.MODE_READ_ONLY));

            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .adoptShellPermissionIdentity(
                            "android.permission.STAGE_HEALTH_CONNECT_REMOTE_DATA",
                            MANAGE_HEALTH_DATA);
            service.stageAllHealthConnectRemoteData(
                    pfdsByFileName,
                    Executors.newSingleThreadExecutor(),
                    new OutcomeReceiver<>() {
                        @Override
                        public void onResult(Void result) {
                            latch.countDown();
                        }

                        @Override
                        public void onError(@NonNull StageRemoteDataException error) {}
                    });
            assertThat(latch.await(10, TimeUnit.SECONDS)).isEqualTo(true);
            service.getHealthConnectDataState(
                    Executors.newSingleThreadExecutor(),
                    new OutcomeReceiver<>() {
                        @Override
                        public void onResult(HealthConnectDataState healthConnectDataState) {
                            returnedHealthConnectDataState.set(healthConnectDataState);
                            stateLatch.countDown();
                        }

                        @Override
                        public void onError(@NonNull HealthConnectException e) {}
                    });
        } catch (IOException e) {
            Log.e(TAG, "Error creating / writing to test files.", e);
        } finally {
            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .dropShellPermissionIdentity();
        }
        assertThat(stateLatch.await(10, TimeUnit.SECONDS)).isEqualTo(true);
        assertThat(returnedHealthConnectDataState.get().getDataRestoreState())
                .isEqualTo(RESTORE_STATE_PENDING);

        deleteAllStagedRemoteData();
    }

    @Test
    public void testGetDataRestoreError_onErrorDuringStaging_returnsErrorFetching()
            throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch stateLatch = new CountDownLatch(1);
        AtomicReference<HealthConnectDataState> returnedHealthConnectDataState =
                new AtomicReference<>();
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();

        try {
            File dataDir = context.getDataDir();
            File testRestoreFile1 = createAndGetNonEmptyFile(dataDir, "testRestoreFile1");
            File testRestoreFile2 = createAndGetNonEmptyFile(dataDir, "testRestoreFile2");

            assertThat(testRestoreFile1.exists()).isTrue();
            assertThat(testRestoreFile2.exists()).isTrue();

            Map<String, ParcelFileDescriptor> pfdsByFileName = new ArrayMap<>();
            pfdsByFileName.put(
                    testRestoreFile1.getName(),
                    ParcelFileDescriptor.open(
                            testRestoreFile1, ParcelFileDescriptor.MODE_WRITE_ONLY));
            pfdsByFileName.put(
                    testRestoreFile2.getName(),
                    ParcelFileDescriptor.open(
                            testRestoreFile2, ParcelFileDescriptor.MODE_READ_ONLY));

            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .adoptShellPermissionIdentity(
                            "android.permission.STAGE_HEALTH_CONNECT_REMOTE_DATA",
                            MANAGE_HEALTH_DATA);
            service.stageAllHealthConnectRemoteData(
                    pfdsByFileName,
                    Executors.newSingleThreadExecutor(),
                    new OutcomeReceiver<>() {
                        @Override
                        public void onResult(Void result) {}

                        @Override
                        public void onError(@NonNull StageRemoteDataException error) {
                            latch.countDown();
                        }
                    });
            assertThat(latch.await(10, TimeUnit.SECONDS)).isEqualTo(true);
            service.getHealthConnectDataState(
                    Executors.newSingleThreadExecutor(),
                    new OutcomeReceiver<>() {
                        @Override
                        public void onResult(HealthConnectDataState healthConnectDataState) {
                            returnedHealthConnectDataState.set(healthConnectDataState);
                            stateLatch.countDown();
                        }

                        @Override
                        public void onError(@NonNull HealthConnectException e) {}
                    });
        } catch (IOException e) {
            Log.e(TAG, "Error creating / writing to test files.", e);
        } finally {
            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .dropShellPermissionIdentity();
        }

        assertThat(stateLatch.await(10, TimeUnit.SECONDS)).isEqualTo(true);
        assertThat(returnedHealthConnectDataState.get().getDataRestoreError())
                .isEqualTo(RESTORE_ERROR_FETCHING_DATA);
        deleteAllStagedRemoteData();
    }

    @Test
    public void testGetDataRestoreError_onDownloadFailed_returnsErrorFetching() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<HealthConnectDataState> returnedHealthConnectDataState =
                new AtomicReference<>();
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();

        try {
            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .adoptShellPermissionIdentity(
                            "android.permission.STAGE_HEALTH_CONNECT_REMOTE_DATA",
                            MANAGE_HEALTH_DATA);
            service.updateDataDownloadState(DATA_DOWNLOAD_FAILED);
            service.getHealthConnectDataState(
                    Executors.newSingleThreadExecutor(),
                    new OutcomeReceiver<>() {
                        @Override
                        public void onResult(HealthConnectDataState healthConnectDataState) {
                            returnedHealthConnectDataState.set(healthConnectDataState);
                            latch.countDown();
                        }

                        @Override
                        public void onError(@NonNull HealthConnectException e) {}
                    });
        } finally {
            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .dropShellPermissionIdentity();
        }

        assertThat(latch.await(10, TimeUnit.SECONDS)).isEqualTo(true);
        assertThat(returnedHealthConnectDataState.get().getDataRestoreError())
                .isEqualTo(RESTORE_ERROR_FETCHING_DATA);
        deleteAllStagedRemoteData();
    }

    @Test
    public void testGetDataRestoreError_onNoErrorDuringRestore_returnsNoError() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch stateLatch = new CountDownLatch(1);
        AtomicReference<HealthConnectDataState> returnedHealthConnectDataState =
                new AtomicReference<>();
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();

        try {
            File dataDir = context.getDataDir();
            File testRestoreFile1 = createAndGetNonEmptyFile(dataDir, "testRestoreFile1");
            File testRestoreFile2 = createAndGetNonEmptyFile(dataDir, "testRestoreFile2");

            assertThat(testRestoreFile1.exists()).isTrue();
            assertThat(testRestoreFile2.exists()).isTrue();

            Map<String, ParcelFileDescriptor> pfdsByFileName = new ArrayMap<>();
            pfdsByFileName.put(
                    testRestoreFile1.getName(),
                    ParcelFileDescriptor.open(
                            testRestoreFile1, ParcelFileDescriptor.MODE_READ_ONLY));
            pfdsByFileName.put(
                    testRestoreFile2.getName(),
                    ParcelFileDescriptor.open(
                            testRestoreFile2, ParcelFileDescriptor.MODE_READ_ONLY));

            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .adoptShellPermissionIdentity(
                            "android.permission.STAGE_HEALTH_CONNECT_REMOTE_DATA",
                            MANAGE_HEALTH_DATA);
            service.stageAllHealthConnectRemoteData(
                    pfdsByFileName,
                    Executors.newSingleThreadExecutor(),
                    new OutcomeReceiver<>() {
                        @Override
                        public void onResult(Void result) {
                            latch.countDown();
                        }

                        @Override
                        public void onError(@NonNull StageRemoteDataException error) {}
                    });
            assertThat(latch.await(10, TimeUnit.SECONDS)).isEqualTo(true);
            service.getHealthConnectDataState(
                    Executors.newSingleThreadExecutor(),
                    new OutcomeReceiver<>() {
                        @Override
                        public void onResult(HealthConnectDataState healthConnectDataState) {
                            returnedHealthConnectDataState.set(healthConnectDataState);
                            stateLatch.countDown();
                        }

                        @Override
                        public void onError(@NonNull HealthConnectException e) {}
                    });
        } catch (IOException e) {
            Log.e(TAG, "Error creating / writing to test files.", e);
        } finally {
            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .dropShellPermissionIdentity();
        }

        assertThat(stateLatch.await(10, TimeUnit.SECONDS)).isEqualTo(true);
        assertThat(returnedHealthConnectDataState.get().getDataRestoreError())
                .isEqualTo(RESTORE_ERROR_NONE);
        deleteAllStagedRemoteData();
    }

    @Test
    public void testDataMigrationState_byDefault_returnsIdleState() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<HealthConnectDataState> returnedHealthConnectDataState =
                new AtomicReference<>();
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();

        try {
            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .adoptShellPermissionIdentity(MANAGE_HEALTH_DATA);
            service.getHealthConnectDataState(
                    Executors.newSingleThreadExecutor(),
                    new OutcomeReceiver<>() {
                        @Override
                        public void onResult(HealthConnectDataState healthConnectDataState) {
                            returnedHealthConnectDataState.set(healthConnectDataState);
                            latch.countDown();
                        }

                        @Override
                        public void onError(@NonNull HealthConnectException e) {}
                    });
        } finally {
            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .dropShellPermissionIdentity();
        }
        assertThat(latch.await(10, TimeUnit.SECONDS)).isEqualTo(true);
        assertThat(returnedHealthConnectDataState.get().getDataMigrationState())
                .isEqualTo(MIGRATION_STATE_IDLE);
        deleteAllStagedRemoteData();
    }

    @Test
    public void testGetHealthConnectDataState_withoutPermission_throwsSecurityException() {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();

        try {
            service.getHealthConnectDataState(
                    Executors.newSingleThreadExecutor(), healthConnectDataState -> {});
        } catch (SecurityException e) {
            /* pass */
        }
    }

    @Test
    public void testDataApis_migrationInProgress_apisBlocked() throws InterruptedException {
        UiAutomation uiAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        uiAutomation.adoptShellPermissionIdentity(MANAGE_HEALTH_DATA);

        runWithShellPermissionIdentity(
                () -> {
                    TestUtils.startMigration();
                    assertThat(TestUtils.getHealthConnectDataMigrationState())
                            .isEqualTo(HealthConnectDataState.MIGRATION_STATE_IN_PROGRESS);
                },
                Manifest.permission.MIGRATE_HEALTH_CONNECT_DATA);

        StepsRecord testRecord = TestUtils.getStepsRecord();

        try {
            TestUtils.insertRecords(Collections.singletonList(testRecord));
            Assert.fail();
        } catch (HealthConnectException exception) {
            assertThat(exception).isNotNull();
            assertThat(exception.getErrorCode())
                    .isEqualTo(HealthConnectException.ERROR_DATA_SYNC_IN_PROGRESS);
        }

        try {
            ReadRecordsRequestUsingIds<StepsRecord> request =
                    new ReadRecordsRequestUsingIds.Builder<>(StepsRecord.class)
                            .addId(testRecord.getMetadata().getId())
                            .build();
            TestUtils.readRecords(request);
            Assert.fail();
        } catch (HealthConnectException exception) {
            assertThat(exception).isNotNull();
            assertThat(exception.getErrorCode())
                    .isEqualTo(HealthConnectException.ERROR_DATA_SYNC_IN_PROGRESS);
        }

        try {
            TestUtils.updateRecords(Collections.singletonList(testRecord));
            Assert.fail();
        } catch (HealthConnectException exception) {
            assertThat(exception).isNotNull();
            assertThat(exception.getErrorCode())
                    .isEqualTo(HealthConnectException.ERROR_DATA_SYNC_IN_PROGRESS);
        }

        try {
            TestUtils.deleteRecords(Collections.singletonList(testRecord));
            Assert.fail();
        } catch (HealthConnectException exception) {
            assertThat(exception).isNotNull();
            assertThat(exception.getErrorCode())
                    .isEqualTo(HealthConnectException.ERROR_DATA_SYNC_IN_PROGRESS);
        }

        try {
            TestUtils.getActivityDates(Collections.singletonList(testRecord.getClass()));
            Assert.fail();
        } catch (HealthConnectException exception) {
            assertThat(exception).isNotNull();
            assertThat(exception.getErrorCode())
                    .isEqualTo(HealthConnectException.ERROR_DATA_SYNC_IN_PROGRESS);
        }

        try {
            uiAutomation.adoptShellPermissionIdentity(MANAGE_HEALTH_DATA);
            TestUtils.getApplicationInfo();
            Assert.fail();
        } catch (HealthConnectException exception) {
            assertThat(exception).isNotNull();
            assertThat(exception.getErrorCode())
                    .isEqualTo(HealthConnectException.ERROR_DATA_SYNC_IN_PROGRESS);
            uiAutomation.dropShellPermissionIdentity();
        }

        try {
            uiAutomation.adoptShellPermissionIdentity(MANAGE_HEALTH_DATA);

            TestUtils.queryAccessLogs();
            Assert.fail();
        } catch (HealthConnectException exception) {
            assertThat(exception).isNotNull();
            assertThat(exception.getErrorCode())
                    .isEqualTo(HealthConnectException.ERROR_DATA_SYNC_IN_PROGRESS);
            uiAutomation.dropShellPermissionIdentity();
        }

        try {
            uiAutomation.adoptShellPermissionIdentity(MANAGE_HEALTH_DATA);

            TestUtils.setAutoDeletePeriod(1);
            Assert.fail();
        } catch (HealthConnectException exception) {
            assertThat(exception).isNotNull();
            assertThat(exception.getErrorCode())
                    .isEqualTo(HealthConnectException.ERROR_DATA_SYNC_IN_PROGRESS);
            uiAutomation.dropShellPermissionIdentity();
        }

        try {
            TestUtils.getChangeLogToken(new ChangeLogTokenRequest.Builder().build());
            Assert.fail();
        } catch (HealthConnectException exception) {
            assertThat(exception).isNotNull();
            assertThat(exception.getErrorCode())
                    .isEqualTo(HealthConnectException.ERROR_DATA_SYNC_IN_PROGRESS);
        }

        try {

            TestUtils.getChangeLogs(new ChangeLogsRequest.Builder(/* token */ "").build());
            Assert.fail();
        } catch (HealthConnectException exception) {
            assertThat(exception).isNotNull();
            assertThat(exception.getErrorCode())
                    .isEqualTo(HealthConnectException.ERROR_DATA_SYNC_IN_PROGRESS);
        }

        AggregateRecordsRequest<Long> aggregateRecordsRequest =
                new AggregateRecordsRequest.Builder<Long>(
                                new TimeInstantRangeFilter.Builder()
                                        .setStartTime(Instant.now().minus(3, ChronoUnit.DAYS))
                                        .setEndTime(Instant.now())
                                        .build())
                        .addAggregationType(STEPS_COUNT_TOTAL)
                        .build();

        try {
            TestUtils.getAggregateResponse(
                    aggregateRecordsRequest, Collections.singletonList(testRecord));
            Assert.fail();
        } catch (HealthConnectException exception) {
            assertThat(exception).isNotNull();
            assertThat(exception.getErrorCode())
                    .isEqualTo(HealthConnectException.ERROR_DATA_SYNC_IN_PROGRESS);
        }

        try {
            TestUtils.getAggregateResponseGroupByDuration(
                    aggregateRecordsRequest, Duration.ofDays(1));
            Assert.fail();
        } catch (HealthConnectException exception) {
            assertThat(exception).isNotNull();
            assertThat(exception.getErrorCode())
                    .isEqualTo(HealthConnectException.ERROR_DATA_SYNC_IN_PROGRESS);
        }

        try {
            TestUtils.getAggregateResponseGroupByPeriod(aggregateRecordsRequest, Period.ofDays(1));
            Assert.fail();
        } catch (HealthConnectException exception) {
            assertThat(exception).isNotNull();
            assertThat(exception.getErrorCode())
                    .isEqualTo(HealthConnectException.ERROR_DATA_SYNC_IN_PROGRESS);
        }

        runWithShellPermissionIdentity(
                TestUtils::finishMigration, Manifest.permission.MIGRATE_HEALTH_CONNECT_DATA);
    }

    private void deleteAllStagedRemoteData()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        try {
            Context context = ApplicationProvider.getApplicationContext();
            HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
            assertThat(service).isNotNull();

            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .adoptShellPermissionIdentity(
                            "android.permission.DELETE_STAGED_HEALTH_CONNECT_REMOTE_DATA");
            // TODO(b/241542162): Avoid using reflection as a workaround once test apis can be
            //  run in CTS tests.
            service.getClass().getMethod("deleteAllStagedRemoteData").invoke(service);
        } finally {
            InstrumentationRegistry.getInstrumentation()
                    .getUiAutomation()
                    .dropShellPermissionIdentity();
        }
    }

    private static File createAndGetNonEmptyFile(File dir, String fileName) throws IOException {
        File file = new File(dir, fileName);
        FileWriter fileWriter = new FileWriter(file);
        fileWriter.write("Contents of file " + fileName);
        fileWriter.close();
        return file;
    }

    private List<Record> getTestRecords(boolean isSetClientRecordId) {
        return Arrays.asList(
                getStepsRecord(isSetClientRecordId, ""),
                getHeartRateRecord(isSetClientRecordId),
                getBasalMetabolicRateRecord(isSetClientRecordId));
    }

    private Record setTestRecordId(Record record, String id) {
        Metadata metadata = record.getMetadata();
        Metadata metadataWithId =
                new Metadata.Builder()
                        .setId(id)
                        .setClientRecordId(metadata.getClientRecordId())
                        .setClientRecordVersion(metadata.getClientRecordVersion())
                        .setDataOrigin(metadata.getDataOrigin())
                        .setDevice(metadata.getDevice())
                        .setLastModifiedTime(metadata.getLastModifiedTime())
                        .build();
        switch (record.getRecordType()) {
            case RECORD_TYPE_STEPS:
                return new StepsRecord.Builder(
                                metadataWithId, Instant.now(), Instant.now().plusMillis(1000), 10)
                        .build();
            case RECORD_TYPE_HEART_RATE:
                HeartRateRecord.HeartRateSample heartRateSample =
                        new HeartRateRecord.HeartRateSample(72, Instant.now().plusMillis(100));
                ArrayList<HeartRateRecord.HeartRateSample> heartRateSamples = new ArrayList<>();
                heartRateSamples.add(heartRateSample);
                heartRateSamples.add(heartRateSample);
                return new HeartRateRecord.Builder(
                                metadataWithId,
                                Instant.now(),
                                Instant.now().plusMillis(1000),
                                heartRateSamples)
                        .build();
            case RECORD_TYPE_BASAL_METABOLIC_RATE:
                return new BasalMetabolicRateRecord.Builder(
                                metadataWithId, Instant.now(), Power.fromWatts(100.0))
                        .setZoneOffset(
                                ZoneOffset.systemDefault().getRules().getOffset(Instant.now()))
                        .build();
            default:
                throw new IllegalStateException("Invalid record type.");
        }
    }

    private StepsRecord getStepsRecord(boolean isSetClientRecordId, String packageName) {
        Device device =
                new Device.Builder().setManufacturer("google").setModel("Pixel").setType(1).build();
        DataOrigin dataOrigin =
                new DataOrigin.Builder()
                        .setPackageName(
                                packageName.isEmpty() ? "android.healthconnect.cts" : packageName)
                        .build();

        Metadata.Builder testMetadataBuilder = new Metadata.Builder();
        testMetadataBuilder.setDevice(device).setDataOrigin(dataOrigin);
        if (isSetClientRecordId) {
            testMetadataBuilder.setClientRecordId("SR" + Math.random());
        }
        return new StepsRecord.Builder(
                        testMetadataBuilder.build(),
                        Instant.now(),
                        Instant.now().plusMillis(1000),
                        10)
                .build();
    }

    private HeartRateRecord getHeartRateRecord(boolean isSetClientRecordId) {
        HeartRateRecord.HeartRateSample heartRateSample =
                new HeartRateRecord.HeartRateSample(72, Instant.now().plusMillis(100));
        ArrayList<HeartRateRecord.HeartRateSample> heartRateSamples = new ArrayList<>();
        heartRateSamples.add(heartRateSample);
        heartRateSamples.add(heartRateSample);
        Device device =
                new Device.Builder().setManufacturer("google").setModel("Pixel").setType(1).build();
        DataOrigin dataOrigin =
                new DataOrigin.Builder().setPackageName("android.healthconnect.cts").build();
        Metadata.Builder testMetadataBuilder = new Metadata.Builder();
        testMetadataBuilder.setDevice(device).setDataOrigin(dataOrigin);
        if (isSetClientRecordId) {
            testMetadataBuilder.setClientRecordId("HR" + Math.random());
        }
        return new HeartRateRecord.Builder(
                        testMetadataBuilder.build(),
                        Instant.now(),
                        Instant.now().plusMillis(1000),
                        heartRateSamples)
                .build();
    }

    private BasalMetabolicRateRecord getBasalMetabolicRateRecord(boolean isSetClientRecordId) {
        Device device =
                new Device.Builder()
                        .setManufacturer("google")
                        .setModel("Pixel4a")
                        .setType(2)
                        .build();
        DataOrigin dataOrigin =
                new DataOrigin.Builder().setPackageName("android.healthconnect.cts").build();
        Metadata.Builder testMetadataBuilder = new Metadata.Builder();
        testMetadataBuilder.setDevice(device).setDataOrigin(dataOrigin);
        if (isSetClientRecordId) {
            testMetadataBuilder.setClientRecordId("BMR" + Math.random());
        }
        return new BasalMetabolicRateRecord.Builder(
                        testMetadataBuilder.build(), Instant.now(), Power.fromWatts(100.0))
                .setZoneOffset(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()))
                .build();
    }
}
