/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.server.healthconnect.common.changelog;

import static android.health.connect.Constants.DEFAULT_PAGE_SIZE;
import static android.health.connect.Constants.DELETE;
import static android.health.connect.Constants.UPSERT;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_BLOOD_PRESSURE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_DISTANCE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_STEPS;

import static com.android.healthfitness.flags.Flags.FLAG_CLOUD_BACKUP_AND_RESTORE;
import static com.android.healthfitness.flags.Flags.FLAG_CLOUD_BACKUP_AND_RESTORE_DB;
import static com.android.healthfitness.flags.Flags.FLAG_ECOSYSTEM_METRICS_DB_CHANGES;
import static com.android.server.healthconnect.common.changelog.ChangeLogsHelper.APP_ID_COLUMN_NAME;
import static com.android.server.healthconnect.common.changelog.ChangeLogsHelper.OPERATION_TYPE_COLUMN_NAME;
import static com.android.server.healthconnect.common.changelog.ChangeLogsHelper.RECORD_TYPE_COLUMN_NAME;
import static com.android.server.healthconnect.common.changelog.ChangeLogsHelper.TIME_COLUMN_NAME;
import static com.android.server.healthconnect.common.changelog.ChangeLogsHelper.UUIDS_COLUMN_NAME;
import static com.android.server.healthconnect.storage.utils.StorageUtils.bytesToUuids;
import static com.android.server.healthconnect.testing.storage.TransactionTestUtils.createBloodPressureRecord;
import static com.android.server.healthconnect.testing.storage.TransactionTestUtils.createStepsRecord;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;

import android.content.ContentValues;
import android.content.Context;
import android.health.connect.RecordIdFilter;
import android.health.connect.accesslog.AccessLog;
import android.health.connect.changelog.ChangeLogTokenRequest;
import android.health.connect.changelog.ChangeLogsRequest;
import android.health.connect.datatypes.BloodPressureRecord;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.health.connect.datatypes.StepsRecord;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.server.healthconnect.common.changelog.ChangeLogsHelper.ChangeLogsTableRequests;
import com.android.server.healthconnect.common.metadata.AppInfoHelper;
import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;
import com.android.server.healthconnect.permission.FirstGrantTimeManager;
import com.android.server.healthconnect.permission.HealthPermissionIntentAppsTracker;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.request.UpsertTableRequest;
import com.android.server.healthconnect.storage.utils.StorageUtils;
import com.android.server.healthconnect.testing.storage.TransactionTestUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@RunWith(AndroidJUnit4.class)
public class ChangeLogsHelperTest {

    private static final String PACKAGE_NAME = "package.name";

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Rule public final TemporaryFolder mEnvironmentDataDir = new TemporaryFolder();
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    private ChangeLogsHelper mChangeLogsHelper;
    private ChangeLogsRequestHelper mChangeLogsRequestHelper;
    private TransactionManager mTransactionManager;
    private AppInfoHelper mAppInfoHelper;
    private TransactionTestUtils mTransactionTestUtils;

    @Before
    public void setup() {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectInjector healthConnectInjector =
                HealthConnectInjectorImpl.newBuilderForTest(context)
                        .setFirstGrantTimeManager(mock(FirstGrantTimeManager.class))
                        .setHealthPermissionIntentAppsTracker(
                                mock(HealthPermissionIntentAppsTracker.class))
                        .setEnvironmentDataDirectory(mEnvironmentDataDir.getRoot())
                        .build();
        mChangeLogsHelper = healthConnectInjector.getChangeLogsHelper();
        mChangeLogsRequestHelper = healthConnectInjector.getChangeLogsRequestHelper();
        mTransactionManager = healthConnectInjector.getTransactionManager();
        mAppInfoHelper = healthConnectInjector.getAppInfoHelper();
        mTransactionTestUtils = new TransactionTestUtils(healthConnectInjector);
        mTransactionTestUtils.insertApp(PACKAGE_NAME);
    }

    @Test
    public void changeLogs_getUpsertTableRequests_listLessThanDefaultPageSize() {
        ChangeLogsTableRequests tableRequests = ChangeLogsTableRequests.ofDeletion(Instant.now());
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        tableRequests.addRecordInfo(RECORD_TYPE_STEPS, 0, uuid1);
        tableRequests.addRecordInfo(RECORD_TYPE_STEPS, 0, uuid2);
        List<UpsertTableRequest> requests = tableRequests.getUpsertTableRequests();

        assertThat(requests).hasSize(1);
        List<UUID> uuidList =
                bytesToUuids((byte[]) requests.get(0).getContentValues().get(UUIDS_COLUMN_NAME));
        assertThat(uuidList).containsExactly(uuid1, uuid2);
    }

    @Test
    public void changeLogs_getUpsertTableRequests_listMoreThanDefaultPageSize() {
        ChangeLogsTableRequests tableRequests = ChangeLogsTableRequests.ofDeletion(Instant.now());
        for (int i = 0; i <= DEFAULT_PAGE_SIZE; i++) {
            UUID uuid = UUID.randomUUID();
            tableRequests.addRecordInfo(RECORD_TYPE_STEPS, 0, uuid);
        }
        List<UpsertTableRequest> requests = tableRequests.getUpsertTableRequests();

        assertThat(requests).hasSize(2);
        List<UUID> uuidList1 =
                bytesToUuids((byte[]) requests.get(0).getContentValues().get(UUIDS_COLUMN_NAME));
        assertThat(uuidList1).hasSize(DEFAULT_PAGE_SIZE);
        List<UUID> uuidList2 =
                bytesToUuids((byte[]) requests.get(1).getContentValues().get(UUIDS_COLUMN_NAME));
        assertThat(uuidList2).hasSize(1);
    }

    @Test
    public void changeLogs_getUpsertTableRequests_multipleRecordTypes_ofDeletion() {
        ChangeLogsTableRequests tableRequests = ChangeLogsTableRequests.ofDeletion(Instant.now());
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        tableRequests.addRecordInfo(RECORD_TYPE_STEPS, 0, uuid1);
        tableRequests.addRecordInfo(RECORD_TYPE_DISTANCE, 0, uuid2);
        List<UpsertTableRequest> requests = tableRequests.getUpsertTableRequests();

        assertThat(requests).hasSize(2);
        var firstContentValues = requests.get(0).getContentValues();
        List<UUID> uuidList1 = bytesToUuids((byte[]) firstContentValues.get(UUIDS_COLUMN_NAME));
        assertThat(uuidList1).containsExactly(uuid1);
        assertThat(firstContentValues.get(OPERATION_TYPE_COLUMN_NAME)).isEqualTo(DELETE);
        var secondContentValues = requests.get(1).getContentValues();
        List<UUID> uuidList2 = bytesToUuids((byte[]) secondContentValues.get(UUIDS_COLUMN_NAME));
        assertThat(uuidList2).containsExactly(uuid2);
        assertThat(secondContentValues.get(OPERATION_TYPE_COLUMN_NAME)).isEqualTo(DELETE);
    }

    @Test
    public void changeLogs_getUpsertTableRequests_multipleRecordTypes_ofUpsertion() {
        ChangeLogsTableRequests tableRequests = ChangeLogsTableRequests.ofUpsertion(Instant.now());
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        tableRequests.addRecordInfo(RECORD_TYPE_STEPS, 0, uuid1);
        tableRequests.addRecordInfo(RECORD_TYPE_DISTANCE, 0, uuid2);
        List<UpsertTableRequest> requests = tableRequests.getUpsertTableRequests();

        assertThat(requests).hasSize(2);
        var firstContentValues = requests.get(0).getContentValues();
        List<UUID> uuidList1 = bytesToUuids((byte[]) firstContentValues.get(UUIDS_COLUMN_NAME));
        assertThat(uuidList1).containsExactly(uuid1);
        assertThat(firstContentValues.get(OPERATION_TYPE_COLUMN_NAME)).isEqualTo(UPSERT);
        var secondContentValues = requests.get(1).getContentValues();
        List<UUID> uuidList2 = bytesToUuids((byte[]) secondContentValues.get(UUIDS_COLUMN_NAME));
        assertThat(uuidList2).containsExactly(uuid2);
        assertThat(secondContentValues.get(OPERATION_TYPE_COLUMN_NAME)).isEqualTo(UPSERT);
    }

    @Test
    public void getRecordTypesWrittenInPast30Days_ignoresOperationsOtherThanUpsert() {
        insertChangeLog(
                /* recordType= */ RECORD_TYPE_STEPS,
                /* appInfoId= */ 1,
                /* operationType= */ AccessLog.OperationType.OPERATION_TYPE_UPSERT,
                /* timeStamp= */ Instant.now().toEpochMilli());
        insertChangeLog(
                /* recordType= */ RECORD_TYPE_DISTANCE,
                /* appInfoId= */ 1,
                /* operationType= */ AccessLog.OperationType.OPERATION_TYPE_UPSERT,
                /* timeStamp= */ Instant.now().minus(20, ChronoUnit.DAYS).toEpochMilli());
        insertChangeLog(
                /* recordType= */ RECORD_TYPE_BLOOD_PRESSURE,
                /* appInfoId= */ 1,
                /* operationType= */ AccessLog.OperationType.OPERATION_TYPE_DELETE,
                /* timeStamp= */ Instant.now().minus(20, ChronoUnit.DAYS).toEpochMilli());

        assertThat(mChangeLogsHelper.getRecordTypesWrittenInPast30Days())
                .containsExactly(RECORD_TYPE_STEPS, RECORD_TYPE_DISTANCE);
    }

    @Test
    public void getRecordTypesWrittenInPast30Days_ignoresDataWrittenMoreThan30DaysAgo() {
        insertChangeLog(
                /* recordType= */ RECORD_TYPE_STEPS,
                /* appInfoId= */ 1,
                /* operationType= */ AccessLog.OperationType.OPERATION_TYPE_UPSERT,
                /* timeStamp= */ Instant.now().toEpochMilli());
        insertChangeLog(
                /* recordType= */ RECORD_TYPE_DISTANCE,
                /* appInfoId= */ 2,
                /* operationType= */ AccessLog.OperationType.OPERATION_TYPE_UPSERT,
                /* timeStamp= */ Instant.now().minus(31, ChronoUnit.DAYS).toEpochMilli());

        assertThat(mChangeLogsHelper.getRecordTypesWrittenInPast30Days())
                .containsExactly(RECORD_TYPE_STEPS);
    }

    @Test
    @DisableFlags(FLAG_CLOUD_BACKUP_AND_RESTORE)
    public void getDeleteRequestForAutoDelete_byDefault_removeChangeLogsMoreThan32DaysOld() {
        insertChangeLog(
                /* recordType= */ RECORD_TYPE_STEPS,
                /* appInfoId= */ 1,
                /* operationType= */ AccessLog.OperationType.OPERATION_TYPE_UPSERT,
                /* timeStamp= */ Instant.now().minus(60, ChronoUnit.DAYS).toEpochMilli());

        mTransactionManager.deleteAll(List.of(ChangeLogsHelper.getDeleteRequestForAutoDelete()));

        assertThat(mChangeLogsHelper.getLatestRowId()).isEqualTo(0);
    }

    @Test
    public void getDeleteRequestForAutoDelete_byDefault_notRemoveChangeLogsLessThan32DaysOld() {
        insertChangeLog(
                /* recordType= */ RECORD_TYPE_STEPS,
                /* appInfoId= */ 1,
                /* operationType= */ AccessLog.OperationType.OPERATION_TYPE_UPSERT,
                /* timeStamp= */ Instant.now().minus(10, ChronoUnit.DAYS).toEpochMilli());

        mTransactionManager.deleteAll(List.of(ChangeLogsHelper.getDeleteRequestForAutoDelete()));

        assertThat(mChangeLogsHelper.getLatestRowId()).isEqualTo(1);
    }

    @Test
    @EnableFlags({
        FLAG_CLOUD_BACKUP_AND_RESTORE,
        FLAG_CLOUD_BACKUP_AND_RESTORE_DB,
        FLAG_ECOSYSTEM_METRICS_DB_CHANGES
    })
    public void getDeleteRequestForAutoDelete_doesNotRemoveChangeLogsLessThan90DaysOld() {
        insertChangeLog(
                /* recordType= */ RECORD_TYPE_STEPS,
                /* appInfoId= */ 1,
                /* operationType= */ AccessLog.OperationType.OPERATION_TYPE_UPSERT,
                /* timeStamp= */ Instant.now().minus(60, ChronoUnit.DAYS).toEpochMilli());

        mTransactionManager.deleteAll(List.of(ChangeLogsHelper.getDeleteRequestForAutoDelete()));

        assertThat(mChangeLogsHelper.getLatestRowId()).isEqualTo(1);
    }

    @Test
    @EnableFlags({
        FLAG_CLOUD_BACKUP_AND_RESTORE,
        FLAG_CLOUD_BACKUP_AND_RESTORE_DB,
        FLAG_ECOSYSTEM_METRICS_DB_CHANGES
    })
    public void getDeleteRequestForAutoDelete_removeChangeLogsMoreThan90DaysOld() {
        insertChangeLog(
                /* recordType= */ RECORD_TYPE_STEPS,
                /* appInfoId= */ 1,
                /* operationType= */ AccessLog.OperationType.OPERATION_TYPE_UPSERT,
                /* timeStamp= */ Instant.now().minus(120, ChronoUnit.DAYS).toEpochMilli());

        mTransactionManager.deleteAll(List.of(ChangeLogsHelper.getDeleteRequestForAutoDelete()));

        assertThat(mChangeLogsHelper.getLatestRowId()).isEqualTo(0);
    }

    @Test
    public void getChangeLogs_skipsNotRequestedDataTypes() {
        var token =
                mChangeLogsRequestHelper.getToken(
                        -1,
                        PACKAGE_NAME,
                        new ChangeLogTokenRequest.Builder()
                                .addRecordType(StepsRecord.class)
                                .build());
        var insertedRecords =
                mTransactionTestUtils.insertRecords(
                        PACKAGE_NAME,
                        createStepsRecord(12345, 54321, 100),
                        createStepsRecord(123456, 654321, 100),
                        createBloodPressureRecord(12345678, 100, 100));
        mTransactionTestUtils.deleteRecords(
                PACKAGE_NAME, RecordIdFilter.fromId(StepsRecord.class, insertedRecords.get(0)));

        var tokenRequest = mChangeLogsRequestHelper.getRequest(PACKAGE_NAME, token);
        var changeLogsResponse =
                mChangeLogsHelper.getChangeLogs(
                        mAppInfoHelper,
                        tokenRequest,
                        new ChangeLogsRequest.Builder(token).build(),
                        mChangeLogsRequestHelper);

        assertThat(changeLogsResponse.getRecordTypeToUpsertedUuids()).hasSize(1);
        assertThat(changeLogsResponse.getRecordTypeToUpsertedUuids().get(RECORD_TYPE_STEPS))
                .containsExactly(
                        UUID.fromString(insertedRecords.get(0)),
                        UUID.fromString(insertedRecords.get(1)));
        assertThat(
                        changeLogsResponse
                                .getRecordTypeToUpsertedUuids()
                                .containsKey(RECORD_TYPE_BLOOD_PRESSURE))
                .isFalse();
        assertThat(changeLogsResponse.getDeletedLogs()).hasSize(1);
        assertThat(changeLogsResponse.getDeletedLogs().get(0).getDeletedRecordId())
                .isEqualTo(insertedRecords.get(0));
    }

    @Test
    public void getChangeLogs_returnsChangeLogs() {
        var token =
                mChangeLogsRequestHelper.getToken(
                        -1,
                        PACKAGE_NAME,
                        new ChangeLogTokenRequest.Builder()
                                .addRecordType(StepsRecord.class)
                                .addRecordType(BloodPressureRecord.class)
                                .build());
        var insertedRecords =
                mTransactionTestUtils.insertRecords(
                        PACKAGE_NAME,
                        createStepsRecord(12345, 54321, 100),
                        createStepsRecord(123456, 654321, 100),
                        createBloodPressureRecord(12345678, 100, 100));
        mTransactionTestUtils.deleteRecords(
                PACKAGE_NAME, RecordIdFilter.fromId(StepsRecord.class, insertedRecords.get(0)));

        var tokenRequest = mChangeLogsRequestHelper.getRequest(PACKAGE_NAME, token);
        var changeLogsResponse =
                mChangeLogsHelper.getChangeLogs(
                        mAppInfoHelper,
                        tokenRequest,
                        new ChangeLogsRequest.Builder(token).build(),
                        mChangeLogsRequestHelper);

        assertThat(changeLogsResponse.getRecordTypeToUpsertedUuids()).hasSize(2);
        assertThat(changeLogsResponse.getRecordTypeToUpsertedUuids().get(RECORD_TYPE_STEPS))
                .containsExactly(
                        UUID.fromString(insertedRecords.get(0)),
                        UUID.fromString(insertedRecords.get(1)));
        assertThat(
                        changeLogsResponse
                                .getRecordTypeToUpsertedUuids()
                                .get(RECORD_TYPE_BLOOD_PRESSURE))
                .containsExactly(UUID.fromString(insertedRecords.get(2)));
        assertThat(changeLogsResponse.getDeletedLogs()).hasSize(1);
        assertThat(changeLogsResponse.getDeletedLogs().get(0).getDeletedRecordId())
                .isEqualTo(insertedRecords.get(0));
    }

    @Test
    public void getChangeLogs_withPageSize_returnsChangeLogs() {
        var token =
                mChangeLogsRequestHelper.getToken(
                        -1,
                        PACKAGE_NAME,
                        new ChangeLogTokenRequest.Builder()
                                .addRecordType(StepsRecord.class)
                                .addRecordType(BloodPressureRecord.class)
                                .build());
        var insertedRecords =
                mTransactionTestUtils.insertRecords(
                        PACKAGE_NAME,
                        createStepsRecord(12345, 54321, 100),
                        createStepsRecord(123456, 654321, 100),
                        createBloodPressureRecord(12345678, 100, 100));
        mTransactionTestUtils.deleteRecords(
                PACKAGE_NAME, RecordIdFilter.fromId(StepsRecord.class, insertedRecords.get(0)));

        var firstTokenRequest = mChangeLogsRequestHelper.getRequest(PACKAGE_NAME, token);
        var firstChangeLogsResponse =
                mChangeLogsHelper.getChangeLogs(
                        mAppInfoHelper,
                        firstTokenRequest,
                        new ChangeLogsRequest.Builder(token).setPageSize(1).build(),
                        mChangeLogsRequestHelper);

        assertThat(firstChangeLogsResponse.getRecordTypeToUpsertedUuids()).hasSize(1);
        assertThat(firstChangeLogsResponse.getRecordTypeToUpsertedUuids().get(RECORD_TYPE_STEPS))
                .containsExactly(
                        UUID.fromString(insertedRecords.get(0)),
                        UUID.fromString(insertedRecords.get(1)));
        assertThat(firstChangeLogsResponse.getDeletedLogs()).hasSize(0);

        var secondTokenRequest =
                mChangeLogsRequestHelper.getRequest(
                        PACKAGE_NAME, firstChangeLogsResponse.getNextPageToken());
        var secondChangeLogsResponse =
                mChangeLogsHelper.getChangeLogs(
                        mAppInfoHelper,
                        secondTokenRequest,
                        new ChangeLogsRequest.Builder(firstChangeLogsResponse.getNextPageToken())
                                .setPageSize(1)
                                .build(),
                        mChangeLogsRequestHelper);

        assertThat(secondChangeLogsResponse.getRecordTypeToUpsertedUuids()).hasSize(1);
        assertThat(
                        secondChangeLogsResponse
                                .getRecordTypeToUpsertedUuids()
                                .get(RECORD_TYPE_BLOOD_PRESSURE))
                .containsExactly(UUID.fromString(insertedRecords.get(2)));
        assertThat(secondChangeLogsResponse.getDeletedLogs()).hasSize(0);

        var thirdTokenRequest =
                mChangeLogsRequestHelper.getRequest(
                        PACKAGE_NAME, secondChangeLogsResponse.getNextPageToken());
        var thirdChangeLogsResponse =
                mChangeLogsHelper.getChangeLogs(
                        mAppInfoHelper,
                        thirdTokenRequest,
                        new ChangeLogsRequest.Builder(secondChangeLogsResponse.getNextPageToken())
                                .setPageSize(1)
                                .build(),
                        mChangeLogsRequestHelper);

        assertThat(thirdChangeLogsResponse.getRecordTypeToUpsertedUuids()).hasSize(0);
        assertThat(thirdChangeLogsResponse.getDeletedLogs()).hasSize(1);
        assertThat(thirdChangeLogsResponse.getDeletedLogs().get(0).getDeletedRecordId())
                .isEqualTo(insertedRecords.get(0));
    }

    private void insertChangeLog(
            @RecordTypeIdentifier.RecordType int recordType,
            int appInfoId,
            @AccessLog.OperationType.OperationTypes int operationType,
            long timeStamp) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(RECORD_TYPE_COLUMN_NAME, recordType);
        contentValues.put(APP_ID_COLUMN_NAME, appInfoId);
        contentValues.put(OPERATION_TYPE_COLUMN_NAME, operationType);
        contentValues.put(TIME_COLUMN_NAME, timeStamp);
        contentValues.put(
                UUIDS_COLUMN_NAME, StorageUtils.getSingleByteArray(Collections.emptyList()));
        mTransactionManager.insertOrThrowOnConflict(
                new UpsertTableRequest(ChangeLogsHelper.TABLE_NAME, contentValues));
    }
}
