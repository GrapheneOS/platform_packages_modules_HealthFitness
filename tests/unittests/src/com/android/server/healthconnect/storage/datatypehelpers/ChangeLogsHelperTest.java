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

package com.android.server.healthconnect.storage.datatypehelpers;

import static android.health.connect.Constants.DEFAULT_PAGE_SIZE;
import static android.health.connect.Constants.DELETE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_BLOOD_PRESSURE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_DISTANCE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_STEPS;

import static com.android.healthfitness.flags.Flags.FLAG_CLOUD_BACKUP_AND_RESTORE;
import static com.android.healthfitness.flags.Flags.FLAG_CLOUD_BACKUP_AND_RESTORE_DB;
import static com.android.healthfitness.flags.Flags.FLAG_ECOSYSTEM_METRICS_DB_CHANGES;
import static com.android.server.healthconnect.storage.datatypehelpers.ChangeLogsHelper.APP_ID_COLUMN_NAME;
import static com.android.server.healthconnect.storage.datatypehelpers.ChangeLogsHelper.OPERATION_TYPE_COLUMN_NAME;
import static com.android.server.healthconnect.storage.datatypehelpers.ChangeLogsHelper.RECORD_TYPE_COLUMN_NAME;
import static com.android.server.healthconnect.storage.datatypehelpers.ChangeLogsHelper.TIME_COLUMN_NAME;
import static com.android.server.healthconnect.storage.datatypehelpers.ChangeLogsHelper.UUIDS_COLUMN_NAME;
import static com.android.server.healthconnect.storage.utils.StorageUtils.bytesToUuids;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;

import android.content.ContentValues;
import android.content.Context;
import android.health.connect.accesslog.AccessLog;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.modules.utils.testing.ExtendedMockitoRule;
import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;
import com.android.server.healthconnect.permission.FirstGrantTimeManager;
import com.android.server.healthconnect.permission.HealthPermissionIntentAppsTracker;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.request.UpsertTableRequest;
import com.android.server.healthconnect.storage.utils.StorageUtils;
import com.android.server.healthconnect.testing.fixtures.EnvironmentFixture;
import com.android.server.healthconnect.testing.fixtures.SQLiteDatabaseFixture;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@RunWith(AndroidJUnit4.class)
public class ChangeLogsHelperTest {

    @Rule(order = 1)
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Rule(order = 2)
    public final ExtendedMockitoRule mExtendedMockitoRule =
            new ExtendedMockitoRule.Builder(this)
                    .addStaticMockFixtures(EnvironmentFixture::new, SQLiteDatabaseFixture::new)
                    .setStrictness(Strictness.LENIENT)
                    .build();

    private ChangeLogsHelper mChangeLogsHelper;
    private TransactionManager mTransactionManager;

    @Before
    public void setup() {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectInjector healthConnectInjector =
                HealthConnectInjectorImpl.newBuilderForTest(context)
                        .setFirstGrantTimeManager(mock(FirstGrantTimeManager.class))
                        .setHealthPermissionIntentAppsTracker(
                                mock(HealthPermissionIntentAppsTracker.class))
                        .build();
        mChangeLogsHelper = healthConnectInjector.getChangeLogsHelper();
        mTransactionManager = healthConnectInjector.getTransactionManager();
    }

    @Test
    public void changeLogs_getUpsertTableRequests_listLessThanDefaultPageSize() {
        ChangeLogsHelper.ChangeLogs changeLogs =
                new ChangeLogsHelper.ChangeLogs(DELETE, Instant.now().toEpochMilli());
        UUID uuid = UUID.randomUUID();
        changeLogs.addUUID(RECORD_TYPE_STEPS, 0, uuid);
        List<UpsertTableRequest> requests = changeLogs.getUpsertTableRequests();

        assertThat(requests).hasSize(1);
        List<UUID> uuidList =
                bytesToUuids((byte[]) requests.get(0).getContentValues().get(UUIDS_COLUMN_NAME));
        assertThat(uuidList).containsExactly(uuid);
    }

    @Test
    public void changeLogs_getUpsertTableRequests_listMoreThanDefaultPageSize() {
        ChangeLogsHelper.ChangeLogs changeLogs =
                new ChangeLogsHelper.ChangeLogs(DELETE, Instant.now().toEpochMilli());
        for (int i = 0; i <= DEFAULT_PAGE_SIZE; i++) {
            UUID uuid = UUID.randomUUID();
            changeLogs.addUUID(RECORD_TYPE_STEPS, 0, uuid);
        }
        List<UpsertTableRequest> requests = changeLogs.getUpsertTableRequests();

        assertThat(requests).hasSize(2);
        List<UUID> uuidList1 =
                bytesToUuids((byte[]) requests.get(0).getContentValues().get(UUIDS_COLUMN_NAME));
        assertThat(uuidList1).hasSize(DEFAULT_PAGE_SIZE);
        List<UUID> uuidList2 =
                bytesToUuids((byte[]) requests.get(1).getContentValues().get(UUIDS_COLUMN_NAME));
        assertThat(uuidList2).hasSize(1);
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
        mTransactionManager.insert(
                new UpsertTableRequest(ChangeLogsHelper.TABLE_NAME, contentValues));
    }
}
