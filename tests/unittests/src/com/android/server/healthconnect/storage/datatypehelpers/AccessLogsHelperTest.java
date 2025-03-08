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

package com.android.server.healthconnect.storage.datatypehelpers;

import static android.health.connect.accesslog.AccessLog.OperationType.OPERATION_TYPE_DELETE;
import static android.health.connect.accesslog.AccessLog.OperationType.OPERATION_TYPE_READ;
import static android.health.connect.accesslog.AccessLog.OperationType.OPERATION_TYPE_UPSERT;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_BLOOD_PRESSURE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_BODY_FAT;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_DISTANCE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_HEART_RATE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_HEIGHT;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_SKIN_TEMPERATURE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_STEPS;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_STEPS_CADENCE;
import static android.healthconnect.cts.phr.utils.PhrDataFactory.DATA_SOURCE_PACKAGE_NAME;

import static com.android.healthfitness.flags.Flags.FLAG_PERSONAL_HEALTH_RECORD;
import static com.android.healthfitness.flags.Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE;
import static com.android.server.healthconnect.storage.datatypehelpers.AccessLogsHelper.MEDICAL_DATA_SOURCE_ACCESSED_COLUMN_NAME;
import static com.android.server.healthconnect.storage.datatypehelpers.AccessLogsHelper.MEDICAL_RESOURCE_TYPE_COLUMN_NAME;
import static com.android.server.healthconnect.storage.datatypehelpers.AccessLogsHelper.getAlterTableRequestForPhrAccessLogs;
import static com.android.server.healthconnect.storage.datatypehelpers.AccessLogsHelper.populateCommonColumns;
import static com.android.server.healthconnect.storage.utils.StorageUtils.INTEGER;
import static com.android.server.healthconnect.storage.utils.StorageUtils.TEXT_NULL;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.ContentValues;
import android.content.Context;
import android.health.connect.accesslog.AccessLog;
import android.health.connect.datatypes.BloodPressureRecord;
import android.health.connect.datatypes.BodyFatRecord;
import android.health.connect.datatypes.DistanceRecord;
import android.health.connect.datatypes.HeartRateRecord;
import android.health.connect.datatypes.HeightRecord;
import android.health.connect.datatypes.SkinTemperatureRecord;
import android.health.connect.datatypes.StepsCadenceRecord;
import android.health.connect.datatypes.StepsRecord;
import android.os.UserHandle;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.util.Pair;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;
import com.android.server.healthconnect.permission.FirstGrantTimeManager;
import com.android.server.healthconnect.permission.HealthPermissionIntentAppsTracker;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.request.AlterTableRequest;
import com.android.server.healthconnect.storage.request.UpsertTableRequest;
import com.android.server.healthconnect.testing.fakes.FakePreferenceHelper;
import com.android.server.healthconnect.testing.storage.TransactionTestUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.List;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
public class AccessLogsHelperTest {

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Rule public final TemporaryFolder mEnvironmentDataDir = new TemporaryFolder();
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    private TransactionManager mTransactionManager;
    private AccessLogsHelper mAccessLogsHelper;
    private UserHandle mUserHandle;

    // TODO(b/373322447): Remove the mock FirstGrantTimeManager
    @Mock private FirstGrantTimeManager mFirstGrantTimeManager;
    // TODO(b/373322447): Remove the mock HealthPermissionIntentAppsTracker
    @Mock private HealthPermissionIntentAppsTracker mPermissionIntentAppsTracker;
    @Mock private AppOpLogsHelper mAppOpLogsHelper;

    @Before
    public void setup() {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectInjector healthConnectInjector =
                HealthConnectInjectorImpl.newBuilderForTest(context)
                        .setPreferenceHelper(new FakePreferenceHelper())
                        .setFirstGrantTimeManager(mFirstGrantTimeManager)
                        .setHealthPermissionIntentAppsTracker(mPermissionIntentAppsTracker)
                        .setAppOpLogsHelper(mAppOpLogsHelper)
                        .setEnvironmentDataDirectory(mEnvironmentDataDir.getRoot())
                        .build();
        mTransactionManager = healthConnectInjector.getTransactionManager();
        mAccessLogsHelper = healthConnectInjector.getAccessLogsHelper();
        mUserHandle = context.getUser();

        TransactionTestUtils transactionTestUtils = new TransactionTestUtils(healthConnectInjector);
        transactionTestUtils.insertApp(DATA_SOURCE_PACKAGE_NAME);
    }

    @Test
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testGetAlterTableRequestForPhrAccessLogs_success() {
        List<Pair<String, String>> columnInfo =
                List.of(
                        Pair.create(MEDICAL_RESOURCE_TYPE_COLUMN_NAME, TEXT_NULL),
                        Pair.create(MEDICAL_DATA_SOURCE_ACCESSED_COLUMN_NAME, INTEGER));
        AlterTableRequest expected = new AlterTableRequest(AccessLogsHelper.TABLE_NAME, columnInfo);

        AlterTableRequest result = getAlterTableRequestForPhrAccessLogs();

        assertThat(result.getAlterTableAddColumnsCommands())
                .isEqualTo(expected.getAlterTableAddColumnsCommands());
    }

    @Test
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testAddAccessLogsPhr_accessedSingleMedicalResourceType_success() {
        mTransactionManager.runAsTransaction(
                (TransactionManager.Runnable<RuntimeException>)
                        db ->
                                mAccessLogsHelper.addAccessLog(
                                        db,
                                        DATA_SOURCE_PACKAGE_NAME,
                                        Set.of(MEDICAL_RESOURCE_TYPE_VACCINES),
                                        OPERATION_TYPE_READ,
                                        /* accessedMedicalDataSource= */ false));

        List<AccessLog> result = mAccessLogsHelper.queryAccessLogs(mUserHandle);
        AccessLog accessLog = result.get(0);

        assertThat(result).hasSize(1);
        assertThat(accessLog.getPackageName()).isEqualTo(DATA_SOURCE_PACKAGE_NAME);
        assertThat(accessLog.getMedicalResourceTypes())
                .isEqualTo(Set.of(MEDICAL_RESOURCE_TYPE_VACCINES));
        assertThat(accessLog.getRecordTypes()).isEmpty();
        assertThat(accessLog.getOperationType()).isEqualTo(OPERATION_TYPE_READ);
        assertThat(accessLog.isMedicalDataSourceAccessed()).isFalse();
        assertThat(accessLog.getAccessTime()).isNotNull();
    }

    @Test
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testAddAccessLogsPhr_accessedMultipleMedicalResourceTypes_success() {
        mTransactionManager.runAsTransaction(
                (TransactionManager.Runnable<RuntimeException>)
                        db ->
                                mAccessLogsHelper.addAccessLog(
                                        db,
                                        DATA_SOURCE_PACKAGE_NAME,
                                        Set.of(
                                                MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES,
                                                MEDICAL_RESOURCE_TYPE_VACCINES),
                                        OPERATION_TYPE_READ,
                                        /* accessedMedicalDataSource= */ false));

        List<AccessLog> result = mAccessLogsHelper.queryAccessLogs(mUserHandle);
        AccessLog accessLog = result.get(0);

        assertThat(result).hasSize(1);
        assertThat(accessLog.getPackageName()).isEqualTo(DATA_SOURCE_PACKAGE_NAME);
        assertThat(accessLog.getMedicalResourceTypes())
                .isEqualTo(
                        Set.of(
                                MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES,
                                MEDICAL_RESOURCE_TYPE_VACCINES));
        assertThat(accessLog.getRecordTypes()).isEmpty();
        assertThat(accessLog.getOperationType()).isEqualTo(OPERATION_TYPE_READ);
        assertThat(accessLog.isMedicalDataSourceAccessed()).isFalse();
        assertThat(accessLog.getAccessTime()).isNotNull();
    }

    @Test
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testAddAccessLogsPhr_accessedMedicalDataSource_success() {
        mTransactionManager.runAsTransaction(
                (TransactionManager.Runnable<RuntimeException>)
                        db ->
                                mAccessLogsHelper.addAccessLog(
                                        db,
                                        DATA_SOURCE_PACKAGE_NAME,
                                        /* medicalResourceTypes= */ Set.of(),
                                        OPERATION_TYPE_READ,
                                        /* accessedMedicalDataSource= */ true));

        List<AccessLog> result = mAccessLogsHelper.queryAccessLogs(mUserHandle);
        AccessLog accessLog = result.get(0);

        assertThat(result).hasSize(1);
        assertThat(accessLog.getPackageName()).isEqualTo(DATA_SOURCE_PACKAGE_NAME);
        assertThat(accessLog.getMedicalResourceTypes()).isEmpty();
        assertThat(accessLog.getRecordTypes()).isEmpty();
        assertThat(accessLog.getOperationType()).isEqualTo(OPERATION_TYPE_READ);
        assertThat(accessLog.isMedicalDataSourceAccessed()).isTrue();
        assertThat(accessLog.getAccessTime()).isNotNull();
    }

    @Test
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testAddAccessLogsForHCRecordType_queryAccessLogs_expectCorrectResult() {
        mAccessLogsHelper.addAccessLog(
                DATA_SOURCE_PACKAGE_NAME,
                /* recordTypeList= */ List.of(RECORD_TYPE_STEPS),
                OPERATION_TYPE_READ);

        List<AccessLog> result = mAccessLogsHelper.queryAccessLogs(mUserHandle);
        AccessLog accessLog = result.get(0);

        assertThat(result).hasSize(1);
        assertThat(accessLog.getPackageName()).isEqualTo(DATA_SOURCE_PACKAGE_NAME);
        assertThat(accessLog.getMedicalResourceTypes()).isEmpty();
        assertThat(accessLog.getRecordTypes()).isEqualTo(List.of(StepsRecord.class));
        assertThat(accessLog.getOperationType()).isEqualTo(OPERATION_TYPE_READ);
        assertThat(accessLog.isMedicalDataSourceAccessed()).isFalse();
        assertThat(accessLog.getAccessTime()).isNotNull();
    }

    @Test
    @EnableFlags({FLAG_PERSONAL_HEALTH_RECORD, FLAG_PERSONAL_HEALTH_RECORD_DATABASE})
    public void testAddAccessLogsPhr_multipleAccessLogs_success() {
        mTransactionManager.runAsTransaction(
                db -> {
                    mAccessLogsHelper.addAccessLog(
                            db,
                            DATA_SOURCE_PACKAGE_NAME,
                            /* medicalResourceTypes= */ Set.of(),
                            OPERATION_TYPE_READ,
                            /* accessedMedicalDataSource= */ true);
                    mAccessLogsHelper.addAccessLog(
                            db,
                            DATA_SOURCE_PACKAGE_NAME,
                            Set.of(MEDICAL_RESOURCE_TYPE_VACCINES),
                            OPERATION_TYPE_UPSERT,
                            /* accessedMedicalDataSource= */ false);
                });

        List<AccessLog> result = mAccessLogsHelper.queryAccessLogs(mUserHandle);
        AccessLog accessLog1 = result.get(0);
        AccessLog accessLog2 = result.get(1);

        assertThat(result).hasSize(2);

        assertThat(accessLog1.getPackageName()).isEqualTo(DATA_SOURCE_PACKAGE_NAME);
        assertThat(accessLog1.getMedicalResourceTypes()).isEmpty();
        assertThat(accessLog1.getRecordTypes()).isEmpty();
        assertThat(accessLog1.getOperationType()).isEqualTo(OPERATION_TYPE_READ);
        assertThat(accessLog1.isMedicalDataSourceAccessed()).isTrue();
        assertThat(accessLog1.getAccessTime()).isNotNull();

        assertThat(accessLog2.getPackageName()).isEqualTo(DATA_SOURCE_PACKAGE_NAME);
        assertThat(accessLog2.getMedicalResourceTypes())
                .isEqualTo(Set.of(MEDICAL_RESOURCE_TYPE_VACCINES));
        assertThat(accessLog2.getRecordTypes()).isEmpty();
        assertThat(accessLog2.getOperationType()).isEqualTo(OPERATION_TYPE_UPSERT);
        assertThat(accessLog2.isMedicalDataSourceAccessed()).isFalse();
        assertThat(accessLog2.getAccessTime()).isNotNull();
    }

    @Test
    public void queryAccessLogs_invalidAppId_skipped() {
        mTransactionManager.runAsTransaction(
                db -> {
                    mAccessLogsHelper.recordDeleteAccessLog(
                            db, DATA_SOURCE_PACKAGE_NAME, Set.of(RECORD_TYPE_BLOOD_PRESSURE));
                    mAccessLogsHelper.recordReadAccessLog(
                            db, "invalid.package", Set.of(RECORD_TYPE_STEPS_CADENCE));
                    mAccessLogsHelper.recordDeleteAccessLog(
                            db, DATA_SOURCE_PACKAGE_NAME, Set.of(RECORD_TYPE_HEIGHT));
                });

        List<AccessLog> result = mAccessLogsHelper.queryAccessLogs(mUserHandle);
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getRecordTypes()).containsExactly(BloodPressureRecord.class);
        assertThat(result.get(1).getRecordTypes()).containsExactly(HeightRecord.class);
    }

    @Test
    public void queryAccessLogs_invalidAppId_excluded() {
        ContentValues contentValues =
                populateCommonColumns(-2, List.of(RECORD_TYPE_BLOOD_PRESSURE), OPERATION_TYPE_READ);
        UpsertTableRequest request =
                new UpsertTableRequest(AccessLogsHelper.TABLE_NAME, contentValues);
        mTransactionManager.insert(request);

        List<AccessLog> result = mAccessLogsHelper.queryAccessLogs(mUserHandle);
        assertThat(result).isEmpty();
    }

    @Test
    public void queryAccessLogs_readsFromAppOpsHelper_success() {
        when(mAppOpLogsHelper.getAccessLogsFromAppOps(mUserHandle))
                .thenReturn(
                        List.of(
                                new AccessLog(
                                        DATA_SOURCE_PACKAGE_NAME,
                                        List.of(RECORD_TYPE_HEART_RATE),
                                        /* accessTime= */ 1000,
                                        OPERATION_TYPE_READ)));

        List<AccessLog> result = mAccessLogsHelper.queryAccessLogs(mUserHandle);
        assertThat(result).hasSize(1);
        AccessLog log = result.get(0);
        assertThat(log.getPackageName()).isEqualTo(DATA_SOURCE_PACKAGE_NAME);
        assertThat(log.getRecordTypes()).containsExactly(HeartRateRecord.class);
        assertThat(log.getOperationType()).isEqualTo(OPERATION_TYPE_READ);
    }

    @Test
    public void queryAccessLogs_readsFromDbAndAppOpsHelper_success() {
        Set<Integer> recordTypeIds = Set.of(RECORD_TYPE_DISTANCE, RECORD_TYPE_STEPS);
        mTransactionManager.runAsTransaction(
                db -> {
                    mAccessLogsHelper.recordReadAccessLog(
                            db, DATA_SOURCE_PACKAGE_NAME, recordTypeIds);
                });
        when(mAppOpLogsHelper.getAccessLogsFromAppOps(mUserHandle))
                .thenReturn(
                        List.of(
                                new AccessLog(
                                        DATA_SOURCE_PACKAGE_NAME,
                                        List.of(RECORD_TYPE_HEART_RATE),
                                        /* accessTime= */ 1000,
                                        OPERATION_TYPE_READ)));

        List<AccessLog> result = mAccessLogsHelper.queryAccessLogs(mUserHandle);
        assertThat(result).hasSize(2);
        AccessLog dbLog = result.get(0);
        assertThat(dbLog.getPackageName()).isEqualTo(DATA_SOURCE_PACKAGE_NAME);
        assertThat(dbLog.getRecordTypes()).containsExactly(DistanceRecord.class, StepsRecord.class);
        assertThat(dbLog.getOperationType()).isEqualTo(OPERATION_TYPE_READ);

        AccessLog appOpLog = result.get(1);
        assertThat(appOpLog.getPackageName()).isEqualTo(DATA_SOURCE_PACKAGE_NAME);
        assertThat(appOpLog.getRecordTypes()).containsExactly(HeartRateRecord.class);
        assertThat(appOpLog.getOperationType()).isEqualTo(OPERATION_TYPE_READ);
    }

    @Test
    public void recordDeleteAccessLog_success() {
        Set<Integer> recordTypeIds = Set.of(RECORD_TYPE_STEPS_CADENCE);
        mTransactionManager.runAsTransaction(
                db -> {
                    mAccessLogsHelper.recordDeleteAccessLog(
                            db, DATA_SOURCE_PACKAGE_NAME, recordTypeIds);
                });

        List<AccessLog> result = mAccessLogsHelper.queryAccessLogs(mUserHandle);
        assertThat(result).hasSize(1);
        AccessLog log = result.get(0);
        assertThat(log.getPackageName()).isEqualTo(DATA_SOURCE_PACKAGE_NAME);
        assertThat(log.getRecordTypes()).containsExactly(StepsCadenceRecord.class);
        assertThat(log.getOperationType()).isEqualTo(OPERATION_TYPE_DELETE);
    }

    @Test
    public void recordDeleteAccessLog_packageNameNotFound_noOp() {
        Set<Integer> recordTypeIds = Set.of(RECORD_TYPE_STEPS_CADENCE);
        mTransactionManager.runAsTransaction(
                db -> {
                    mAccessLogsHelper.recordDeleteAccessLog(db, "unknown.app", recordTypeIds);
                });

        List<AccessLog> result = mAccessLogsHelper.queryAccessLogs(mUserHandle);
        assertThat(result).isEmpty();
    }

    @Test
    public void recordReadAccessLog_success() {
        Set<Integer> recordTypeIds = Set.of(RECORD_TYPE_DISTANCE, RECORD_TYPE_SKIN_TEMPERATURE);
        mTransactionManager.runAsTransaction(
                db -> {
                    mAccessLogsHelper.recordReadAccessLog(
                            db, DATA_SOURCE_PACKAGE_NAME, recordTypeIds);
                });

        List<AccessLog> result = mAccessLogsHelper.queryAccessLogs(mUserHandle);
        assertThat(result).hasSize(1);
        AccessLog log = result.get(0);
        assertThat(log.getPackageName()).isEqualTo(DATA_SOURCE_PACKAGE_NAME);
        assertThat(log.getRecordTypes())
                .containsExactly(DistanceRecord.class, SkinTemperatureRecord.class);
        assertThat(log.getOperationType()).isEqualTo(OPERATION_TYPE_READ);
    }

    @Test
    public void recordReadAccessLog_granularAppOpsFilterOut_remainingRecordWritten() {
        // Filter out HR record since there is a granular app op for it.
        when(mAppOpLogsHelper.getRecordsWithSystemAppOps())
                .thenReturn(Set.of(RECORD_TYPE_HEART_RATE));

        Set<Integer> recordTypeIds = Set.of(RECORD_TYPE_DISTANCE, RECORD_TYPE_HEART_RATE);
        mTransactionManager.runAsTransaction(
                db -> {
                    mAccessLogsHelper.recordReadAccessLog(
                            db, DATA_SOURCE_PACKAGE_NAME, recordTypeIds);
                });

        List<AccessLog> result = mAccessLogsHelper.queryAccessLogs(mUserHandle);
        assertThat(result).hasSize(1);
        AccessLog log = result.get(0);
        assertThat(log.getPackageName()).isEqualTo(DATA_SOURCE_PACKAGE_NAME);
        assertThat(log.getRecordTypes()).containsExactly(DistanceRecord.class);
        assertThat(log.getOperationType()).isEqualTo(OPERATION_TYPE_READ);
    }

    @Test
    public void recordReadAccessLog_granularAppOpsFilterOut_emptyRecordNotWritten() {
        when(mAppOpLogsHelper.getRecordsWithSystemAppOps())
                .thenReturn(Set.of(RECORD_TYPE_HEART_RATE));

        Set<Integer> recordTypeIds = Set.of(RECORD_TYPE_HEART_RATE);
        mTransactionManager.runAsTransaction(
                db -> {
                    mAccessLogsHelper.recordReadAccessLog(
                            db, DATA_SOURCE_PACKAGE_NAME, recordTypeIds);
                });

        List<AccessLog> result = mAccessLogsHelper.queryAccessLogs(mUserHandle);
        assertThat(result).isEmpty();
    }

    @Test
    public void recordReadAccessLog_packageNameNotFound_noOp() {
        Set<Integer> recordTypeIds = Set.of(RECORD_TYPE_DISTANCE, RECORD_TYPE_SKIN_TEMPERATURE);
        mTransactionManager.runAsTransaction(
                db -> {
                    mAccessLogsHelper.recordReadAccessLog(db, "unknown.app", recordTypeIds);
                });

        List<AccessLog> result = mAccessLogsHelper.queryAccessLogs(mUserHandle);
        assertThat(result).isEmpty();
    }

    @Test
    public void recordUpsertAccessLog_success() {
        Set<Integer> recordTypeIds = Set.of(RECORD_TYPE_BODY_FAT, RECORD_TYPE_HEIGHT);
        mTransactionManager.runAsTransaction(
                db -> {
                    mAccessLogsHelper.recordUpsertAccessLog(
                            db, DATA_SOURCE_PACKAGE_NAME, recordTypeIds);
                });

        List<AccessLog> result = mAccessLogsHelper.queryAccessLogs(mUserHandle);
        assertThat(result).hasSize(1);
        AccessLog log = result.get(0);
        assertThat(log.getPackageName()).isEqualTo(DATA_SOURCE_PACKAGE_NAME);
        assertThat(log.getRecordTypes()).containsExactly(BodyFatRecord.class, HeightRecord.class);
        assertThat(log.getOperationType()).isEqualTo(OPERATION_TYPE_UPSERT);
    }

    @Test
    public void recordUpsertAccessLog_packageNameNotFound_noOp() {
        Set<Integer> recordTypeIds = Set.of(RECORD_TYPE_BODY_FAT, RECORD_TYPE_HEIGHT);
        mTransactionManager.runAsTransaction(
                db -> {
                    mAccessLogsHelper.recordUpsertAccessLog(db, "unknown.app", recordTypeIds);
                });

        List<AccessLog> result = mAccessLogsHelper.queryAccessLogs(mUserHandle);
        assertThat(result).isEmpty();
    }

    @Test
    public void testAddAccessLogsForDelete_getLatestAccessLogTimeStampForMAU_expectCorrectResult() {
        Set<Integer> recordTypeIds = Set.of(RECORD_TYPE_DISTANCE, RECORD_TYPE_SKIN_TEMPERATURE);
        mTransactionManager.runAsTransaction(
                db -> {
                    mAccessLogsHelper.recordDeleteAccessLog(
                            db, DATA_SOURCE_PACKAGE_NAME, recordTypeIds);
                });

        long result = mAccessLogsHelper.getLatestUpsertOrReadOperationAccessLogTimeStamp();

        assertThat(result).isEqualTo(Long.MIN_VALUE);
    }

    @Test
    public void testAddAccessLogsForUpsert_getLatestAccessLogTimeStampForMAU_expectCorrectResult() {
        Set<Integer> recordTypeIds = Set.of(RECORD_TYPE_DISTANCE, RECORD_TYPE_SKIN_TEMPERATURE);
        mTransactionManager.runAsTransaction(
                db -> {
                    mAccessLogsHelper.recordUpsertAccessLog(
                            db, DATA_SOURCE_PACKAGE_NAME, recordTypeIds);
                });

        long result = mAccessLogsHelper.getLatestUpsertOrReadOperationAccessLogTimeStamp();

        assertThat(result).isNotEqualTo(Long.MIN_VALUE);
    }

    @Test
    public void testAddAccessLogsForRead_getLatestAccessLogTimeStampForMAU_expectCorrectResult() {
        Set<Integer> recordTypeIds = Set.of(RECORD_TYPE_DISTANCE, RECORD_TYPE_SKIN_TEMPERATURE);
        mTransactionManager.runAsTransaction(
                db -> {
                    mAccessLogsHelper.recordReadAccessLog(
                            db, DATA_SOURCE_PACKAGE_NAME, recordTypeIds);
                });

        long result = mAccessLogsHelper.getLatestUpsertOrReadOperationAccessLogTimeStamp();

        assertThat(result).isNotEqualTo(Long.MIN_VALUE);
    }
}
