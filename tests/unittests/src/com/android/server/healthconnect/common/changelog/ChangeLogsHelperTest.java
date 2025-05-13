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
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_BLOOD_PRESSURE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_DISTANCE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_STEPS;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.DATA_SOURCE_ID;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.createAllergyMedicalResource;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.createDifferentVaccineMedicalResource;
import static android.healthconnect.testing.shared.phr.PhrDataFactory.createVaccineMedicalResource;
import static android.healthconnect.testing.unittest.TransactionTestUtils.createBloodPressureRecord;
import static android.healthconnect.testing.unittest.TransactionTestUtils.createStepsRecord;

import static com.android.healthfitness.flags.Flags.FLAG_CLOUD_BACKUP_AND_RESTORE;
import static com.android.healthfitness.flags.Flags.FLAG_CLOUD_BACKUP_AND_RESTORE_DB;
import static com.android.healthfitness.flags.Flags.FLAG_DEVELOPMENT_DATABASE;
import static com.android.healthfitness.flags.Flags.FLAG_ECOSYSTEM_METRICS_DB_CHANGES;
import static com.android.healthfitness.flags.Flags.FLAG_PHR_CHANGE_LOGS;
import static com.android.server.healthconnect.common.changelog.ChangeLogsHelper.APP_ID_COLUMN_NAME;
import static com.android.server.healthconnect.common.changelog.ChangeLogsHelper.OPERATION_TYPE_COLUMN_NAME;
import static com.android.server.healthconnect.common.changelog.ChangeLogsHelper.RECORD_TYPE_COLUMN_NAME;
import static com.android.server.healthconnect.common.changelog.ChangeLogsHelper.TIME_COLUMN_NAME;
import static com.android.server.healthconnect.common.changelog.ChangeLogsHelper.UUIDS_COLUMN_NAME;
import static com.android.server.healthconnect.common.changelog.ChangeLogsHelper.toByteArray;
import static com.android.server.healthconnect.common.changelog.ChangeLogsHelper.toMedicalResourceIdList;
import static com.android.server.healthconnect.storage.utils.StorageUtils.bytesToUuids;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;

import android.content.ContentValues;
import android.content.Context;
import android.health.connect.MedicalResourceId;
import android.health.connect.RecordIdFilter;
import android.health.connect.accesslog.AccessLog;
import android.health.connect.changelog.ChangeLogTokenRequest;
import android.health.connect.changelog.ChangeLogsRequest;
import android.health.connect.changelog.ChangeLogsResponse.DeletedLog;
import android.health.connect.changelog.ChangeLogsResponse.DeletedMedicalResource;
import android.health.connect.datatypes.BloodPressureRecord;
import android.health.connect.datatypes.MedicalDataSource;
import android.health.connect.datatypes.MedicalResource;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.health.connect.datatypes.StepsRecord;
import android.healthconnect.testing.unittest.PhrTestUtils;
import android.healthconnect.testing.unittest.TransactionTestUtils;
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

import com.google.common.truth.Correspondence;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RunWith(AndroidJUnit4.class)
public class ChangeLogsHelperTest {

    private static final String PACKAGE_NAME = "package.name";
    private static final String DATA_SOURCE_NAME = "dataSource";

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Rule public final TemporaryFolder mEnvironmentDataDir = new TemporaryFolder();
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    private ChangeLogsHelper mChangeLogsHelper;
    private ChangeLogsRequestHelper mChangeLogsRequestHelper;
    private TransactionManager mTransactionManager;
    private AppInfoHelper mAppInfoHelper;
    private TransactionTestUtils mTransactionTestUtils;
    private PhrTestUtils mPhrTestUtils;
    private MedicalDataSource mDataSource;

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
        mPhrTestUtils = new PhrTestUtils(healthConnectInjector);
        mDataSource = mPhrTestUtils.insertR4MedicalDataSource(DATA_SOURCE_NAME, PACKAGE_NAME);
    }

    @Test
    public void changeLogs_records_getUpsertTableRequests_listLessThanDefaultPageSize() {
        ChangeLogsTableRequests tableRequests = ChangeLogsTableRequests.ofDeletion(Instant.now());
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        tableRequests.addRecordInfo(RECORD_TYPE_STEPS, 0, uuid1);
        tableRequests.addRecordInfo(RECORD_TYPE_STEPS, 0, uuid2);
        List<UpsertTableRequest> requests = tableRequests.getUpsertTableRequests();

        assertThat(requests)
                .comparingElementsUsing(upsertTableRequestToUuidListCorrespondence(DELETE))
                .containsExactly(List.of(uuid1, uuid2))
                .inOrder();
    }

    @Test
    public void changeLogs_records_getUpsertTableRequests_listMoreThanDefaultPageSize() {
        ChangeLogsTableRequests tableRequests = ChangeLogsTableRequests.ofDeletion(Instant.now());
        List<UUID> insertedUuids = new ArrayList<>();
        for (int i = 0; i <= DEFAULT_PAGE_SIZE; i++) {
            UUID uuid = UUID.randomUUID();
            tableRequests.addRecordInfo(RECORD_TYPE_STEPS, 0, uuid);
            insertedUuids.add(uuid);
        }
        List<UpsertTableRequest> requests = tableRequests.getUpsertTableRequests();

        assertThat(requests)
                .comparingElementsUsing(upsertTableRequestToUuidListCorrespondence(DELETE))
                .containsExactly(
                        insertedUuids.subList(0, DEFAULT_PAGE_SIZE),
                        insertedUuids.subList(DEFAULT_PAGE_SIZE, DEFAULT_PAGE_SIZE + 1))
                .inOrder();
    }

    @Test
    public void changeLogs_medicalResources_getUpsertTableRequests_listLessThanDefaultPageSize()
            throws Exception {
        ChangeLogsTableRequests tableRequests = ChangeLogsTableRequests.ofDeletion(Instant.now());
        MedicalResourceId medicalResourceId1 = createVaccineMedicalResource(DATA_SOURCE_ID).getId();
        MedicalResourceId medicalResourceId2 =
                createDifferentVaccineMedicalResource(DATA_SOURCE_ID).getId();
        tableRequests.addMedicalResourceInfo(MEDICAL_RESOURCE_TYPE_VACCINES, 0, medicalResourceId1);
        tableRequests.addMedicalResourceInfo(MEDICAL_RESOURCE_TYPE_VACCINES, 0, medicalResourceId2);
        List<UpsertTableRequest> requests = tableRequests.getUpsertTableRequests();

        assertThat(requests)
                .comparingElementsUsing(
                        upsertTableRequestToMedicalResourceListCorrespondence(DELETE))
                .containsExactly(List.of(medicalResourceId1, medicalResourceId2))
                .inOrder();
    }

    @Test
    public void changeLogs_medicalResources_getUpsertTableRequests_listMoreThanDefaultPageSize()
            throws Exception {
        ChangeLogsTableRequests tableRequests = ChangeLogsTableRequests.ofDeletion(Instant.now());
        List<MedicalResourceId> insertedMedicalResourceIds = new ArrayList<>();
        for (int i = 0; i <= DEFAULT_PAGE_SIZE; i++) {
            MedicalResourceId medicalResourceId =
                    createVaccineMedicalResource(DATA_SOURCE_ID).getId();
            tableRequests.addMedicalResourceInfo(
                    MEDICAL_RESOURCE_TYPE_VACCINES, 0, medicalResourceId);
            insertedMedicalResourceIds.add(medicalResourceId);
        }
        List<UpsertTableRequest> requests = tableRequests.getUpsertTableRequests();

        assertThat(requests)
                .comparingElementsUsing(
                        upsertTableRequestToMedicalResourceListCorrespondence(DELETE))
                .containsExactly(
                        insertedMedicalResourceIds.subList(0, DEFAULT_PAGE_SIZE),
                        insertedMedicalResourceIds.subList(
                                DEFAULT_PAGE_SIZE, DEFAULT_PAGE_SIZE + 1))
                .inOrder();
    }

    @Test
    public void changeLogs_records_getUpsertTableRequests_multipleTypes_ofDeletion() {
        ChangeLogsTableRequests tableRequests = ChangeLogsTableRequests.ofDeletion(Instant.now());
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        tableRequests.addRecordInfo(RECORD_TYPE_STEPS, 0, uuid1);
        tableRequests.addRecordInfo(RECORD_TYPE_DISTANCE, 0, uuid2);
        List<UpsertTableRequest> requests = tableRequests.getUpsertTableRequests();

        assertThat(requests)
                .comparingElementsUsing(upsertTableRequestToUuidListCorrespondence(DELETE))
                .containsExactly(List.of(uuid1), List.of(uuid2))
                .inOrder();
    }

    @Test
    public void changeLogs_records_getUpsertTableRequests_multipleTypes_ofUpsertion() {
        ChangeLogsTableRequests tableRequests = ChangeLogsTableRequests.ofUpsertion(Instant.now());
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        tableRequests.addRecordInfo(RECORD_TYPE_STEPS, 0, uuid1);
        tableRequests.addRecordInfo(RECORD_TYPE_DISTANCE, 0, uuid2);
        List<UpsertTableRequest> requests = tableRequests.getUpsertTableRequests();

        assertThat(requests)
                .comparingElementsUsing(upsertTableRequestToUuidListCorrespondence(UPSERT))
                .containsExactly(List.of(uuid1), List.of(uuid2))
                .inOrder();
    }

    @Test
    public void changeLogs_medicalResources_getUpsertTableRequests_multipleTypes_ofDeletion() {
        ChangeLogsTableRequests tableRequests = ChangeLogsTableRequests.ofDeletion(Instant.now());
        MedicalResourceId medicalResourceId1 = createVaccineMedicalResource(DATA_SOURCE_ID).getId();
        MedicalResourceId medicalResourceId2 = createAllergyMedicalResource(DATA_SOURCE_ID).getId();
        tableRequests.addMedicalResourceInfo(MEDICAL_RESOURCE_TYPE_VACCINES, 0, medicalResourceId1);
        tableRequests.addMedicalResourceInfo(
                MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES, 0, medicalResourceId2);
        List<UpsertTableRequest> requests = tableRequests.getUpsertTableRequests();

        assertThat(requests)
                .comparingElementsUsing(
                        upsertTableRequestToMedicalResourceListCorrespondence(DELETE))
                .containsExactly(List.of(medicalResourceId1), List.of(medicalResourceId2))
                .inOrder();
    }

    @Test
    public void changeLogs_medicalResources_getUpsertTableRequests_multipleTypes_ofUpsertion() {
        ChangeLogsTableRequests tableRequests = ChangeLogsTableRequests.ofUpsertion(Instant.now());
        MedicalResourceId medicalResourceId1 = createVaccineMedicalResource(DATA_SOURCE_ID).getId();
        MedicalResourceId medicalResourceId2 = createAllergyMedicalResource(DATA_SOURCE_ID).getId();
        tableRequests.addMedicalResourceInfo(MEDICAL_RESOURCE_TYPE_VACCINES, 0, medicalResourceId1);
        tableRequests.addMedicalResourceInfo(
                MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES, 0, medicalResourceId2);
        List<UpsertTableRequest> requests = tableRequests.getUpsertTableRequests();

        assertThat(requests)
                .comparingElementsUsing(
                        upsertTableRequestToMedicalResourceListCorrespondence(UPSERT))
                .containsExactly(List.of(medicalResourceId1), List.of(medicalResourceId2))
                .inOrder();
    }

    @Test
    public void getRecordTypesWrittenInPast30Days_ignoresOperationsOtherThanUpsert() {
        insertRecordChangeLog(
                /* recordType= */ RECORD_TYPE_STEPS,
                /* appInfoId= */ 1,
                /* operationType= */ AccessLog.OperationType.OPERATION_TYPE_UPSERT,
                /* timeStamp= */ Instant.now().toEpochMilli());
        insertRecordChangeLog(
                /* recordType= */ RECORD_TYPE_DISTANCE,
                /* appInfoId= */ 1,
                /* operationType= */ AccessLog.OperationType.OPERATION_TYPE_UPSERT,
                /* timeStamp= */ Instant.now().minus(20, ChronoUnit.DAYS).toEpochMilli());
        insertRecordChangeLog(
                /* recordType= */ RECORD_TYPE_BLOOD_PRESSURE,
                /* appInfoId= */ 1,
                /* operationType= */ AccessLog.OperationType.OPERATION_TYPE_DELETE,
                /* timeStamp= */ Instant.now().minus(20, ChronoUnit.DAYS).toEpochMilli());

        assertThat(mChangeLogsHelper.getRecordTypesWrittenInPast30Days())
                .containsExactly(RECORD_TYPE_STEPS, RECORD_TYPE_DISTANCE);
    }

    @Test
    public void getRecordTypesWrittenInPast30Days_ignoresDataWrittenMoreThan30DaysAgo() {
        insertRecordChangeLog(
                /* recordType= */ RECORD_TYPE_STEPS,
                /* appInfoId= */ 1,
                /* operationType= */ AccessLog.OperationType.OPERATION_TYPE_UPSERT,
                /* timeStamp= */ Instant.now().toEpochMilli());
        insertRecordChangeLog(
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
        insertRecordChangeLog(
                /* recordType= */ RECORD_TYPE_STEPS,
                /* appInfoId= */ 1,
                /* operationType= */ AccessLog.OperationType.OPERATION_TYPE_UPSERT,
                /* timeStamp= */ Instant.now().minus(60, ChronoUnit.DAYS).toEpochMilli());

        mTransactionManager.deleteAll(List.of(ChangeLogsHelper.getDeleteRequestForAutoDelete()));

        assertThat(mChangeLogsHelper.getLatestRowId()).isEqualTo(0);
    }

    @Test
    public void getDeleteRequestForAutoDelete_byDefault_notRemoveChangeLogsLessThan32DaysOld() {
        insertRecordChangeLog(
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
        insertRecordChangeLog(
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
        insertRecordChangeLog(
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

        assertThat(changeLogsResponse.getRecordTypeToUpsertedUuids())
                .isEqualTo(
                        Map.of(
                                RECORD_TYPE_STEPS,
                                List.of(
                                        UUID.fromString(insertedRecords.get(0)),
                                        UUID.fromString(insertedRecords.get(1)))));
        assertThat(changeLogsResponse.getDeletedLogs())
                .comparingElementsUsing(DELETED_LOG_CORRESPONDENCE)
                .containsExactly(insertedRecords.get(0));
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

        assertThat(changeLogsResponse.getRecordTypeToUpsertedUuids())
                .isEqualTo(
                        Map.of(
                                RECORD_TYPE_STEPS,
                                List.of(
                                        UUID.fromString(insertedRecords.get(0)),
                                        UUID.fromString(insertedRecords.get(1))),
                                RECORD_TYPE_BLOOD_PRESSURE,
                                List.of(UUID.fromString(insertedRecords.get(2)))));
        assertThat(changeLogsResponse.getDeletedLogs())
                .comparingElementsUsing(DELETED_LOG_CORRESPONDENCE)
                .containsExactly(insertedRecords.get(0));
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

        assertThat(firstChangeLogsResponse.getRecordTypeToUpsertedUuids())
                .isEqualTo(
                        Map.of(
                                RECORD_TYPE_STEPS,
                                List.of(
                                        UUID.fromString(insertedRecords.get(0)),
                                        UUID.fromString(insertedRecords.get(1)))));
        assertThat(firstChangeLogsResponse.getDeletedLogs()).isEmpty();
        assertThat(firstChangeLogsResponse.hasMorePages()).isTrue();
        assertThat(firstChangeLogsResponse.getNextPageToken()).isNotEqualTo(token);

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

        assertThat(secondChangeLogsResponse.getRecordTypeToUpsertedUuids())
                .isEqualTo(
                        Map.of(
                                RECORD_TYPE_BLOOD_PRESSURE,
                                List.of(UUID.fromString(insertedRecords.get(2)))));
        assertThat(secondChangeLogsResponse.getDeletedLogs()).isEmpty();
        assertThat(secondChangeLogsResponse.hasMorePages()).isTrue();
        assertThat(secondChangeLogsResponse.getNextPageToken())
                .isNotEqualTo(firstChangeLogsResponse.getNextPageToken());

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

        assertThat(thirdChangeLogsResponse.getRecordTypeToUpsertedUuids()).isEmpty();
        assertThat(thirdChangeLogsResponse.getDeletedLogs())
                .comparingElementsUsing(DELETED_LOG_CORRESPONDENCE)
                .containsExactly(insertedRecords.get(0));
        assertThat(thirdChangeLogsResponse.hasMorePages()).isFalse();
        assertThat(thirdChangeLogsResponse.getNextPageToken())
                .isNotEqualTo(secondChangeLogsResponse.getNextPageToken());
    }

    @Test
    @EnableFlags({FLAG_PHR_CHANGE_LOGS, FLAG_DEVELOPMENT_DATABASE})
    public void getChangeLogs_medicalResources_skipsNotRequestedDataTypes() {
        var token =
                mChangeLogsRequestHelper.getToken(
                        -1,
                        PACKAGE_NAME,
                        new ChangeLogTokenRequest.Builder()
                                .addMedicalResourceType(MEDICAL_RESOURCE_TYPE_VACCINES)
                                .build());
        var insertedResources =
                mPhrTestUtils.upsertResources(
                        List.of(
                                createVaccineMedicalResource(mDataSource.getId()),
                                createDifferentVaccineMedicalResource(mDataSource.getId()),
                                createAllergyMedicalResource(mDataSource.getId())),
                        PACKAGE_NAME);
        mPhrTestUtils.deleteResource(insertedResources.get(0));

        var tokenRequest = mChangeLogsRequestHelper.getRequest(PACKAGE_NAME, token);
        var changeLogsResponse =
                mChangeLogsHelper.getChangeLogs(
                        mAppInfoHelper,
                        tokenRequest,
                        new ChangeLogsRequest.Builder(token).build(),
                        mChangeLogsRequestHelper);

        assertThat(changeLogsResponse.getUpsertedMedicalResourceIds())
                .containsExactly(
                        insertedResources.get(0).getId(), insertedResources.get(1).getId());
        assertThat(changeLogsResponse.getDeletedMedicalResources())
                .comparingElementsUsing(DELETED_MEDICAL_RESOURCE_CORRESPONDENCE)
                .containsExactly(insertedResources.get(0));
    }

    @Test
    @EnableFlags({FLAG_PHR_CHANGE_LOGS, FLAG_DEVELOPMENT_DATABASE})
    public void getChangeLogs_medicalResources_returnsChangeLogs() {
        var token =
                mChangeLogsRequestHelper.getToken(
                        -1,
                        PACKAGE_NAME,
                        new ChangeLogTokenRequest.Builder()
                                .addMedicalResourceType(MEDICAL_RESOURCE_TYPE_VACCINES)
                                .addMedicalResourceType(
                                        MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES)
                                .build());
        var insertedResources =
                mPhrTestUtils.upsertResources(
                        List.of(
                                createVaccineMedicalResource(mDataSource.getId()),
                                createDifferentVaccineMedicalResource(mDataSource.getId()),
                                createAllergyMedicalResource(mDataSource.getId())),
                        PACKAGE_NAME);
        mPhrTestUtils.deleteResource(insertedResources.get(0));

        var tokenRequest = mChangeLogsRequestHelper.getRequest(PACKAGE_NAME, token);
        var changeLogsResponse =
                mChangeLogsHelper.getChangeLogs(
                        mAppInfoHelper,
                        tokenRequest,
                        new ChangeLogsRequest.Builder(token).build(),
                        mChangeLogsRequestHelper);

        assertThat(changeLogsResponse.getUpsertedMedicalResourceIds())
                .isEqualTo(insertedResources.stream().map(MedicalResource::getId).toList());
        assertThat(changeLogsResponse.getDeletedMedicalResources())
                .comparingElementsUsing(DELETED_MEDICAL_RESOURCE_CORRESPONDENCE)
                .containsExactly(insertedResources.get(0));
    }

    @Test
    @EnableFlags({FLAG_PHR_CHANGE_LOGS, FLAG_DEVELOPMENT_DATABASE})
    public void getChangeLogs_medicalResources_withPageSize_returnsChangeLogs() {
        var token =
                mChangeLogsRequestHelper.getToken(
                        -1,
                        PACKAGE_NAME,
                        new ChangeLogTokenRequest.Builder()
                                .addMedicalResourceType(MEDICAL_RESOURCE_TYPE_VACCINES)
                                .addMedicalResourceType(
                                        MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES)
                                .build());
        var insertedResources =
                mPhrTestUtils.upsertResources(
                        List.of(
                                createVaccineMedicalResource(mDataSource.getId()),
                                createDifferentVaccineMedicalResource(mDataSource.getId()),
                                createAllergyMedicalResource(mDataSource.getId())),
                        PACKAGE_NAME);
        mPhrTestUtils.deleteResource(insertedResources.get(0));

        // First page (vaccines upsertion)
        var firstTokenRequest = mChangeLogsRequestHelper.getRequest(PACKAGE_NAME, token);
        var firstChangeLogsResponse =
                mChangeLogsHelper.getChangeLogs(
                        mAppInfoHelper,
                        firstTokenRequest,
                        new ChangeLogsRequest.Builder(token).setPageSize(1).build(),
                        mChangeLogsRequestHelper);

        assertThat(firstChangeLogsResponse.getUpsertedMedicalResourceIds())
                .containsExactly(
                        insertedResources.get(0).getId(), insertedResources.get(1).getId());
        assertThat(firstChangeLogsResponse.getDeletedMedicalResources()).isEmpty();
        assertThat(firstChangeLogsResponse.hasMorePages()).isTrue();
        assertThat(firstChangeLogsResponse.getNextPageToken()).isNotEqualTo(token);

        // Second page (allergies upsertion)
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

        assertThat(secondChangeLogsResponse.getUpsertedMedicalResourceIds())
                .containsExactly(insertedResources.get(2).getId());
        assertThat(secondChangeLogsResponse.getDeletedMedicalResources()).isEmpty();
        assertThat(secondChangeLogsResponse.hasMorePages()).isTrue();
        assertThat(secondChangeLogsResponse.getNextPageToken())
                .isNotEqualTo(firstChangeLogsResponse.getNextPageToken());

        // Third page (deletion)
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

        assertThat(thirdChangeLogsResponse.getUpsertedMedicalResourceIds()).isEmpty();
        assertThat(thirdChangeLogsResponse.getDeletedMedicalResources())
                .comparingElementsUsing(DELETED_MEDICAL_RESOURCE_CORRESPONDENCE)
                .containsExactly(insertedResources.get(0));
        assertThat(thirdChangeLogsResponse.hasMorePages()).isFalse();
        assertThat(thirdChangeLogsResponse.getNextPageToken())
                .isNotEqualTo(secondChangeLogsResponse.getNextPageToken());
    }

    @Test
    public void toByteArrayAndBack_emptyList_returnsEmptyList() {
        byte[] byteArray = toByteArray(Collections.emptyList());
        List<MedicalResourceId> resultList = toMedicalResourceIdList(byteArray);
        assertThat(resultList).isEmpty();
    }

    @Test
    public void toByteArrayAndBack_singleItem_returnsSameItem() {
        MedicalResourceId resourceId = createVaccineMedicalResource(DATA_SOURCE_ID).getId();
        List<MedicalResourceId> singleItemList = List.of(resourceId);

        byte[] byteArray = toByteArray(singleItemList);
        List<MedicalResourceId> resultList = toMedicalResourceIdList(byteArray);

        assertThat(resultList).isEqualTo(singleItemList);
    }

    @Test
    public void toByteArrayAndBack_multipleItems_returnsSameItems() {
        MedicalResourceId resourceId1 = createVaccineMedicalResource(DATA_SOURCE_ID).getId();
        MedicalResourceId resourceId2 =
                createDifferentVaccineMedicalResource(DATA_SOURCE_ID).getId();
        MedicalResourceId resourceId3 = createAllergyMedicalResource(DATA_SOURCE_ID).getId();
        List<MedicalResourceId> multipleItemList = List.of(resourceId1, resourceId2, resourceId3);

        byte[] byteArray = toByteArray(multipleItemList);
        List<MedicalResourceId> resultList = toMedicalResourceIdList(byteArray);

        assertThat(resultList).isEqualTo(multipleItemList);
    }

    @Test
    public void toMedicalResourceIdList_invalidByteArray_throwsIllegalArgumentException() {
        byte[] invalidByteArray = new byte[] {0x1, 0x2, 0x3};

        assertThrows(
                IllegalArgumentException.class, () -> toMedicalResourceIdList(invalidByteArray));
    }

    @Test
    public void toMedicalResourceIdList_nullByteArray_throwsException() {
        assertThrows(Exception.class, () -> toMedicalResourceIdList(null));
    }

    private void insertRecordChangeLog(
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

    private static Correspondence<UpsertTableRequest, List<UUID>>
            upsertTableRequestToUuidListCorrespondence(int operationType) {
        return Correspondence.from(
                (upsertTableRequest, uuidList) ->
                        bytesToUuids(
                                                (byte[])
                                                        upsertTableRequest
                                                                .getContentValues()
                                                                .get(UUIDS_COLUMN_NAME))
                                        .equals(uuidList)
                                && upsertTableRequest
                                        .getContentValues()
                                        .get(OPERATION_TYPE_COLUMN_NAME)
                                        .equals(operationType),
                "has matching record ids and operation type");
    }

    private static Correspondence<UpsertTableRequest, List<MedicalResourceId>>
            upsertTableRequestToMedicalResourceListCorrespondence(int operationType) {
        return Correspondence.from(
                (upsertTableRequest, medicalResourceIdList) ->
                        toMedicalResourceIdList(
                                                (byte[])
                                                        upsertTableRequest
                                                                .getContentValues()
                                                                .get(UUIDS_COLUMN_NAME))
                                        .equals(medicalResourceIdList)
                                && upsertTableRequest
                                        .getContentValues()
                                        .get(OPERATION_TYPE_COLUMN_NAME)
                                        .equals(operationType),
                "has matching medical resource ids and operation type");
    }

    private static final Correspondence<DeletedLog, String> DELETED_LOG_CORRESPONDENCE =
            Correspondence.from(
                    (deletedLog, recordId) -> deletedLog.getDeletedRecordId().equals(recordId),
                    "has matching record id");

    private static final Correspondence<DeletedMedicalResource, MedicalResource>
            DELETED_MEDICAL_RESOURCE_CORRESPONDENCE =
                    Correspondence.from(
                            (deletedMedicalResource, medicalResource) ->
                                    deletedMedicalResource
                                                    .getDeletedMedicalResourceId()
                                                    .equals(medicalResource.getId())
                                            && deletedMedicalResource
                                                    .getDataSourceId()
                                                    .equals(medicalResource.getDataSourceId()),
                            "has matching medical resource id and data source id");
}
