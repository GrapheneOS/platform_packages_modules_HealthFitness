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

package com.android.server.healthconnect.storage;

import static android.health.connect.Constants.DEFAULT_PAGE_SIZE;
import static android.health.connect.accesslog.AccessLog.OperationType.OPERATION_TYPE_DELETE;
import static android.health.connect.accesslog.AccessLog.OperationType.OPERATION_TYPE_READ;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_STEPS;
import static android.healthconnect.cts.utils.DataFactory.getDataOrigin;

import static com.android.healthfitness.flags.Flags.FLAG_ACTIVITY_INTENSITY_DB;
import static com.android.healthfitness.flags.Flags.FLAG_CLOUD_BACKUP_AND_RESTORE_DB;
import static com.android.healthfitness.flags.Flags.FLAG_ECOSYSTEM_METRICS;
import static com.android.healthfitness.flags.Flags.FLAG_ECOSYSTEM_METRICS_DB_CHANGES;
import static com.android.healthfitness.flags.Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE;
import static com.android.server.healthconnect.testing.storage.TransactionTestUtils.createBloodPressureRecord;
import static com.android.server.healthconnect.testing.storage.TransactionTestUtils.createStepsRecord;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.health.connect.DeleteUsingFiltersRequest;
import android.health.connect.PageTokenWrapper;
import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.ReadRecordsRequestUsingIds;
import android.health.connect.RecordIdFilter;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.accesslog.AccessLog;
import android.health.connect.aidl.DeleteUsingFiltersRequestParcel;
import android.health.connect.aidl.RecordIdFiltersParcel;
import android.health.connect.datatypes.BloodPressureRecord;
import android.health.connect.datatypes.HeartRateRecord;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.health.connect.datatypes.StepsRecord;
import android.health.connect.internal.datatypes.RecordInternal;
import android.os.UserHandle;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.util.Pair;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.healthfitness.flags.Flags;
import com.android.server.healthconnect.fitness.FitnessRecordReadHelper;
import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;
import com.android.server.healthconnect.permission.FirstGrantTimeManager;
import com.android.server.healthconnect.permission.HealthPermissionIntentAppsTracker;
import com.android.server.healthconnect.storage.datatypehelpers.AccessLogsHelper;
import com.android.server.healthconnect.storage.datatypehelpers.AppInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.AppOpLogsHelper;
import com.android.server.healthconnect.storage.datatypehelpers.ReadAccessLogsHelper;
import com.android.server.healthconnect.storage.request.DeleteTransactionRequest;
import com.android.server.healthconnect.testing.storage.TransactionTestUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RunWith(AndroidJUnit4.class)
public class TransactionManagerTest {
    private static final String TEST_PACKAGE_NAME = "package.name";

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final TemporaryFolder mEnvironmentDataDir = new TemporaryFolder();

    // TODO(b/373322447): Remove the mock FirstGrantTimeManager
    @Mock private FirstGrantTimeManager mFirstGrantTimeManager;
    // TODO(b/373322447): Remove the mock HealthPermissionIntentAppsTracker
    @Mock private HealthPermissionIntentAppsTracker mPermissionIntentAppsTracker;
    @Mock private AppOpLogsHelper mAppOpLogsHelper;

    private TransactionTestUtils mTransactionTestUtils;
    private TransactionManager mTransactionManager;
    private FitnessRecordReadHelper mFitnessRecordReadHelper;
    private AppInfoHelper mAppInfoHelper;
    private AccessLogsHelper mAccessLogsHelper;
    private ReadAccessLogsHelper mReadAccessLogsHelper;
    private UserHandle mUserHandle;

    @Before
    public void setup() {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectInjector healthConnectInjector =
                HealthConnectInjectorImpl.newBuilderForTest(context)
                        .setFirstGrantTimeManager(mFirstGrantTimeManager)
                        .setHealthPermissionIntentAppsTracker(mPermissionIntentAppsTracker)
                        .setAppOpLogsHelper(mAppOpLogsHelper)
                        .setEnvironmentDataDirectory(mEnvironmentDataDir.getRoot())
                        .build();
        mTransactionManager = healthConnectInjector.getTransactionManager();
        mFitnessRecordReadHelper = healthConnectInjector.getFitnessRecordReadHelper();
        mAppInfoHelper = healthConnectInjector.getAppInfoHelper();
        mAccessLogsHelper = healthConnectInjector.getAccessLogsHelper();
        mReadAccessLogsHelper = spy(healthConnectInjector.getReadAccessLogsHelper());
        mUserHandle = context.getUser();

        mTransactionTestUtils = new TransactionTestUtils(healthConnectInjector);
        mTransactionTestUtils.insertApp(TEST_PACKAGE_NAME);
    }

    @Test
    public void readRecordsById_returnsAllRecords() {
        long timeMillis = 456;
        String uuid =
                mTransactionTestUtils
                        .insertRecords(
                                TEST_PACKAGE_NAME,
                                createBloodPressureRecord(timeMillis, 120.0, 80.0))
                        .get(0);

        ReadRecordsRequestUsingIds<BloodPressureRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(BloodPressureRecord.class)
                        .addId(uuid)
                        .build();

        List<RecordInternal<?>> records =
                mFitnessRecordReadHelper.readRecords(
                                mTransactionManager,
                                TEST_PACKAGE_NAME,
                                request.toReadRecordsRequestParcel(),
                                /* startDateAccessMillis= */ 0,
                                /* enforceSelfRead= */ false,
                                /* grantedExtraReadPermissions */ Set.of(),
                                /* isInForeground= */ true,
                                /* shouldRecordAccessLogs */ false,
                                /* packageNamesByAppIds= */ null)
                        .first;
        assertThat(records).hasSize(1);
        assertThat(records.get(0).getUuid()).isEqualTo(UUID.fromString(uuid));
    }

    @Test
    public void readRecordsById_ignoresMissingIds() {
        long timeMillis = 456;
        String uuid =
                mTransactionTestUtils
                        .insertRecords(
                                TEST_PACKAGE_NAME,
                                createBloodPressureRecord(timeMillis, 120.0, 80.0))
                        .get(0);

        ReadRecordsRequestUsingIds<BloodPressureRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(BloodPressureRecord.class)
                        .addId(UUID.randomUUID().toString())
                        .addId(uuid)
                        .addId(UUID.randomUUID().toString())
                        .build();

        List<RecordInternal<?>> records =
                mFitnessRecordReadHelper.readRecords(
                                mTransactionManager,
                                TEST_PACKAGE_NAME,
                                request.toReadRecordsRequestParcel(),
                                /* startDateAccessMillis= */ 0,
                                /* enforceSelfRead= */ false,
                                /* grantedExtraReadPermissions */ Set.of(),
                                /* isInForeground= */ true,
                                /* shouldRecordAccessLogs */ false,
                                /* packageNamesByAppIds= */ null)
                        .first;
        assertThat(records).hasSize(1);
        assertThat(records.get(0).getUuid()).isEqualTo(UUID.fromString(uuid));
    }

    @Test
    public void readRecordsById_multipleRecordTypes_returnsAllRecords() {
        long startTimeMillis = 123;
        long endTimeMillis = 456;
        List<String> uuids =
                mTransactionTestUtils.insertRecords(
                        TEST_PACKAGE_NAME,
                        createStepsRecord(startTimeMillis, endTimeMillis, 100),
                        createBloodPressureRecord(endTimeMillis, 120.0, 80.0));

        List<UUID> stepsUuids = ImmutableList.of(UUID.fromString(uuids.get(0)));
        List<UUID> bloodPressureUuids = ImmutableList.of(UUID.fromString(uuids.get(1)));
        List<RecordInternal<?>> records =
                mFitnessRecordReadHelper.readRecords(
                        mTransactionManager,
                        TEST_PACKAGE_NAME,
                        ImmutableMap.of(
                                RECORD_TYPE_STEPS,
                                stepsUuids,
                                RecordTypeIdentifier.RECORD_TYPE_BLOOD_PRESSURE,
                                bloodPressureUuids),
                        /* startDateAccessMillis= */ 0,
                        /* grantedExtraReadPermissions */ Set.of(),
                        /* isInForeground= */ true,
                        /* shouldRecordAccessLogs */ false,
                        /* isReadingSelfData= */ false);
        mTransactionTestUtils.readRecordsByIds(
                ImmutableMap.of(
                        RECORD_TYPE_STEPS,
                        stepsUuids,
                        RecordTypeIdentifier.RECORD_TYPE_BLOOD_PRESSURE,
                        bloodPressureUuids));

        assertThat(records).hasSize(2);
        assertThat(records.get(0).getUuid()).isEqualTo(UUID.fromString(uuids.get(0)));
        assertThat(records.get(1).getUuid()).isEqualTo(UUID.fromString(uuids.get(1)));
    }

    @Test
    @EnableFlags(Flags.FLAG_ADD_MISSING_ACCESS_LOGS)
    public void readRecordsById_isReadingSelfData_NoAccessLog() {
        // TODO(b/366149374): Fix the read by uuid case and add is not reading self data test case
        // Read by id requests are always reading self data. Clients are not allowed to read other
        // apps' data by client id
        ReadRecordsRequestUsingIds<BloodPressureRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(BloodPressureRecord.class)
                        .addClientRecordId("id")
                        .build();
        mFitnessRecordReadHelper.readRecords(
                mTransactionManager,
                TEST_PACKAGE_NAME,
                request.toReadRecordsRequestParcel(),
                /* startDateAccessMillis= */ 0,
                /* enforceSelfRead= */ false,
                /* grantedExtraReadPermissions */ Set.of(),
                /* isInForeground= */ true,
                /* shouldRecordAccessLogs */ false,
                /* packageNamesByAppIds= */ null);

        List<AccessLog> result = mAccessLogsHelper.queryAccessLogs(mUserHandle);
        assertThat(result).isEmpty();
    }

    @Test
    public void readRecordsAndPageToken_returnsRecordsAndPageToken() {
        List<String> uuids =
                mTransactionTestUtils.insertRecords(
                        TEST_PACKAGE_NAME,
                        createStepsRecord(400, 500, 100),
                        createStepsRecord(500, 600, 100));

        ReadRecordsRequestUsingFilters<StepsRecord> request =
                new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                        .setTimeRangeFilter(
                                new TimeInstantRangeFilter.Builder()
                                        .setStartTime(Instant.EPOCH)
                                        .setEndTime(Instant.ofEpochMilli(1000))
                                        .build())
                        .setPageSize(1)
                        .build();
        PageTokenWrapper expectedToken =
                PageTokenWrapper.of(
                        /* isAscending= */ true, /* timeMillis= */ 500, /* offset= */ 0);

        Pair<List<RecordInternal<?>>, PageTokenWrapper> result =
                mFitnessRecordReadHelper.readRecords(
                        mTransactionManager,
                        TEST_PACKAGE_NAME,
                        request.toReadRecordsRequestParcel(),
                        /* startDateAccessMillis= */ 0,
                        /* enforceSelfRead= */ false,
                        /* grantedExtraReadPermissions */ Set.of(),
                        /* isInForeground= */ true,
                        /* shouldRecordAccessLogs */ false,
                        /* packageNamesByAppIds= */ null);
        List<RecordInternal<?>> records = result.first;
        assertThat(records).hasSize(1);
        assertThat(result.first.get(0).getUuid()).isEqualTo(UUID.fromString(uuids.get(0)));
        assertThat(result.second).isEqualTo(expectedToken);
    }

    @Test
    @EnableFlags(Flags.FLAG_ADD_MISSING_ACCESS_LOGS)
    public void readRecordsAndPageToken_isNotReadingSelfData_accessLogRecorded() {
        ReadRecordsRequestUsingFilters<StepsRecord> request =
                new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class).build();

        mFitnessRecordReadHelper.readRecords(
                mTransactionManager,
                TEST_PACKAGE_NAME,
                request.toReadRecordsRequestParcel(),
                /* startDateAccessMillis= */ 0,
                /* enforceSelfRead= */ false,
                /* grantedExtraReadPermissions */ Set.of(),
                /* isInForeground= */ true,
                /* shouldRecordAccessLogs */ true,
                /* packageNamesByAppIds= */ null);

        List<AccessLog> result = mAccessLogsHelper.queryAccessLogs(mUserHandle);
        assertThat(result).hasSize(1);
        AccessLog log = result.get(0);
        assertThat(log.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(log.getRecordTypes()).containsExactly(StepsRecord.class);
        assertThat(log.getOperationType()).isEqualTo(OPERATION_TYPE_READ);
    }

    @Test
    @EnableFlags(Flags.FLAG_ADD_MISSING_ACCESS_LOGS)
    public void readRecordsAndPageToken_isReadingSelfData_noAccessLog() {
        ReadRecordsRequestUsingFilters<StepsRecord> request =
                new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                        .addDataOrigins(getDataOrigin(TEST_PACKAGE_NAME))
                        .build();

        mFitnessRecordReadHelper.readRecords(
                mTransactionManager,
                TEST_PACKAGE_NAME,
                request.toReadRecordsRequestParcel(),
                /* startDateAccessMillis= */ 0,
                /* enforceSelfRead= */ false,
                /* grantedExtraReadPermissions */ Set.of(),
                /* isInForeground= */ true,
                /* shouldRecordAccessLogs */ true,
                /* packageNamesByAppIds= */ null);

        List<AccessLog> result = mAccessLogsHelper.queryAccessLogs(mUserHandle);
        assertThat(result).isEmpty();
    }

    @Test
    public void deleteAll_byId_generateChangeLogs() {
        List<String> uuids =
                mTransactionTestUtils.insertRecords(
                        TEST_PACKAGE_NAME, createStepsRecord(123, 456, 100));
        List<RecordIdFilter> ids = List.of(RecordIdFilter.fromId(StepsRecord.class, uuids.get(0)));

        DeleteUsingFiltersRequestParcel parcel =
                new DeleteUsingFiltersRequestParcel(
                        new RecordIdFiltersParcel(ids), TEST_PACKAGE_NAME);
        assertThat(parcel.usesIdFilters()).isTrue();
        mTransactionManager.deleteAllRecords(
                new DeleteTransactionRequest(TEST_PACKAGE_NAME, parcel, mAppInfoHelper),
                /* shouldRecordDeleteAccessLogs= */ false,
                mAccessLogsHelper);
        List<UUID> uuidList = mTransactionTestUtils.getAllDeletedUuids();
        assertThat(uuidList).hasSize(1);
        assertThat(uuidList.get(0).toString()).isEqualTo(uuids.get(0));
    }

    @Test
    public void deleteAll_byTimeFilter_generateChangeLogs() {
        List<String> uuids =
                mTransactionTestUtils.insertRecords(
                        TEST_PACKAGE_NAME, createStepsRecord(123, 456, 100));

        DeleteUsingFiltersRequest deleteRequest =
                new DeleteUsingFiltersRequest.Builder()
                        .setTimeRangeFilter(
                                new TimeInstantRangeFilter.Builder()
                                        .setStartTime(Instant.EPOCH)
                                        .build())
                        .build();
        DeleteUsingFiltersRequestParcel parcel = new DeleteUsingFiltersRequestParcel(deleteRequest);
        assertThat(parcel.usesIdFilters()).isFalse();
        mTransactionManager.deleteAllRecords(
                new DeleteTransactionRequest(TEST_PACKAGE_NAME, parcel, mAppInfoHelper),
                /* shouldRecordDeleteAccessLogs= */ false,
                mAccessLogsHelper);
        List<UUID> uuidList = mTransactionTestUtils.getAllDeletedUuids();
        assertThat(uuidList).hasSize(1);
        assertThat(uuidList.get(0).toString()).isEqualTo(uuids.get(0));
    }

    @Test
    public void deleteAll_bulkDeleteByTimeFilter_generateChangeLogs() {
        ImmutableList.Builder<RecordInternal<?>> records = new ImmutableList.Builder<>();
        for (int i = 0; i <= DEFAULT_PAGE_SIZE; i++) {
            records.add(createStepsRecord(i * 1000, (i + 1) * 1000, 9527));
        }
        mTransactionTestUtils.insertRecords(TEST_PACKAGE_NAME, records.build());

        DeleteUsingFiltersRequest deleteRequest =
                new DeleteUsingFiltersRequest.Builder()
                        .setTimeRangeFilter(
                                new TimeInstantRangeFilter.Builder()
                                        .setStartTime(Instant.EPOCH)
                                        .build())
                        .build();
        DeleteUsingFiltersRequestParcel parcel = new DeleteUsingFiltersRequestParcel(deleteRequest);
        mTransactionManager.deleteAllRecords(
                new DeleteTransactionRequest(TEST_PACKAGE_NAME, parcel, mAppInfoHelper),
                /* shouldRecordDeleteAccessLogs= */ false,
                mAccessLogsHelper);

        List<UUID> uuidList = mTransactionTestUtils.getAllDeletedUuids();
        assertThat(uuidList).hasSize(DEFAULT_PAGE_SIZE + 1);
    }

    @Test
    @EnableFlags(Flags.FLAG_ADD_MISSING_ACCESS_LOGS)
    public void deleteAll_shouldRecordAccessLog_logged() {
        DeleteUsingFiltersRequest deleteRequest =
                new DeleteUsingFiltersRequest.Builder()
                        .addRecordType(StepsRecord.class)
                        .addRecordType(HeartRateRecord.class)
                        .setTimeRangeFilter(
                                new TimeInstantRangeFilter.Builder()
                                        .setStartTime(Instant.EPOCH)
                                        .build())
                        .build();
        DeleteUsingFiltersRequestParcel parcel = new DeleteUsingFiltersRequestParcel(deleteRequest);
        mTransactionManager.deleteAllRecords(
                new DeleteTransactionRequest(TEST_PACKAGE_NAME, parcel, mAppInfoHelper),
                /* shouldRecordDeleteAccessLogs= */ true,
                mAccessLogsHelper);

        List<AccessLog> result = mAccessLogsHelper.queryAccessLogs(mUserHandle);
        assertThat(result).hasSize(1);
        AccessLog log = result.get(0);
        assertThat(log.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(log.getRecordTypes()).containsExactly(StepsRecord.class, HeartRateRecord.class);
        assertThat(log.getOperationType()).isEqualTo(OPERATION_TYPE_DELETE);
    }

    @Test
    @EnableFlags(Flags.FLAG_ADD_MISSING_ACCESS_LOGS)
    public void deleteAll_shouldNotRecordAccessLog_noLog() {
        DeleteUsingFiltersRequest deleteRequest =
                new DeleteUsingFiltersRequest.Builder()
                        .addRecordType(StepsRecord.class)
                        .addRecordType(HeartRateRecord.class)
                        .setTimeRangeFilter(
                                new TimeInstantRangeFilter.Builder()
                                        .setStartTime(Instant.EPOCH)
                                        .build())
                        .build();
        DeleteUsingFiltersRequestParcel parcel = new DeleteUsingFiltersRequestParcel(deleteRequest);
        mTransactionManager.deleteAllRecords(
                new DeleteTransactionRequest(TEST_PACKAGE_NAME, parcel, mAppInfoHelper),
                /* shouldRecordDeleteAccessLogs= */ false,
                mAccessLogsHelper);

        List<AccessLog> result = mAccessLogsHelper.queryAccessLogs(mUserHandle);
        assertThat(result).isEmpty();
    }

    @Test
    @EnableFlags({
        FLAG_ECOSYSTEM_METRICS,
        FLAG_ECOSYSTEM_METRICS_DB_CHANGES,
        FLAG_PERSONAL_HEALTH_RECORD_DATABASE,
        FLAG_ACTIVITY_INTENSITY_DB
    })
    public void flagsEnabled_readRecordsById_addReadAccessLog() {
        String readerPackage = "reader.package";
        mTransactionTestUtils.insertApp(readerPackage);
        String uuid =
                mTransactionTestUtils
                        .insertRecords(
                                TEST_PACKAGE_NAME,
                                createStepsRecord(
                                        mAppInfoHelper.getAppInfoId(TEST_PACKAGE_NAME),
                                        Instant.now().toEpochMilli(),
                                        Instant.now().toEpochMilli(),
                                        100))
                        .get(0);

        mFitnessRecordReadHelper.readRecords(
                mTransactionManager,
                readerPackage,
                ImmutableMap.of(RECORD_TYPE_STEPS, ImmutableList.of(UUID.fromString(uuid))),
                /* startDateAccessMillis= */ 0,
                /* grantedExtraReadPermissions */ Set.of(),
                /* isInForeground= */ true,
                /* shouldRecordAccessLogs */ true,
                /* isReadingSelfData= */ false);

        List<ReadAccessLogsHelper.ReadAccessLog> readAccessLogs =
                mReadAccessLogsHelper.queryReadAccessLogs(0).getReadAccessLogs();
        assertThat(readAccessLogs.size()).isEqualTo(1);
        ReadAccessLogsHelper.ReadAccessLog readAccessLog = readAccessLogs.get(0);
        assertThat(readAccessLog.getRecordWithinPast30Days()).isEqualTo(true);
        assertThat(readAccessLog.getWriterPackage()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(readAccessLog.getDataType()).isEqualTo(RecordTypeIdentifier.RECORD_TYPE_STEPS);
        assertThat(readAccessLog.getReaderPackage()).isEqualTo(readerPackage);
    }

    @Test
    @EnableFlags({
        FLAG_ECOSYSTEM_METRICS,
        FLAG_ECOSYSTEM_METRICS_DB_CHANGES,
        FLAG_PERSONAL_HEALTH_RECORD_DATABASE,
        FLAG_ACTIVITY_INTENSITY_DB,
        FLAG_CLOUD_BACKUP_AND_RESTORE_DB
    })
    public void flagsEnabled_readSelfData_readRecordsById_doNotAddReadAccessLog() {
        String readerPackage = "reader.package";
        mTransactionTestUtils.insertApp(readerPackage);
        mTransactionTestUtils.insertRecords(
                TEST_PACKAGE_NAME,
                createStepsRecord(
                        mAppInfoHelper.getAppInfoId(TEST_PACKAGE_NAME),
                        Instant.now().toEpochMilli(),
                        Instant.now().toEpochMilli(),
                        100));
        // TODO(b/366149374): Fix the read by uuid case and add is not reading self data test case
        // Read by id requests are always reading self data. Clients are not allowed to read other
        // apps' data by client id

        mFitnessRecordReadHelper.readRecords(
                mTransactionManager,
                readerPackage,
                new ReadRecordsRequestUsingIds.Builder<>(StepsRecord.class)
                        .addClientRecordId("id")
                        .build()
                        .toReadRecordsRequestParcel(),
                /* startDateAccessMillis= */ 0,
                /* enforceSelfRead= */ false,
                /* grantedExtraReadPermissions */ Set.of(),
                /* isInForeground= */ true,
                /* shouldRecordAccessLogs */ true,
                /* packageNamesByAppIds= */ null);

        verify(mReadAccessLogsHelper, times(0))
                .recordAccessLogForNonAggregationReads(any(), any(), anyLong(), any());
        verify(mReadAccessLogsHelper, times(0))
                .recordAccessLogForAggregationReads(
                        any(), any(), anyLong(), anyInt(), anyLong(), any());
    }

    @Test
    @EnableFlags({
        FLAG_ECOSYSTEM_METRICS,
        FLAG_ECOSYSTEM_METRICS_DB_CHANGES,
        FLAG_PERSONAL_HEALTH_RECORD_DATABASE,
        FLAG_ACTIVITY_INTENSITY_DB
    })
    public void flagsEnabled_doNotRecordAccessLogs_readRecordsById_doNotAddReadAccessLog() {
        String readerPackage = "reader.package";
        mTransactionTestUtils.insertApp(readerPackage);
        String uuid =
                mTransactionTestUtils
                        .insertRecords(
                                TEST_PACKAGE_NAME,
                                createStepsRecord(
                                        mAppInfoHelper.getAppInfoId(TEST_PACKAGE_NAME),
                                        Instant.now().toEpochMilli(),
                                        Instant.now().toEpochMilli(),
                                        100))
                        .get(0);

        mFitnessRecordReadHelper.readRecords(
                mTransactionManager,
                readerPackage,
                ImmutableMap.of(RECORD_TYPE_STEPS, ImmutableList.of(UUID.fromString(uuid))),
                /* startDateAccessMillis= */ 0,
                /* grantedExtraReadPermissions */ Set.of(),
                /* isInForeground= */ true,
                /* shouldRecordAccessLogs */ false,
                /* isReadingSelfData= */ false);

        verify(mReadAccessLogsHelper, times(0))
                .recordAccessLogForNonAggregationReads(any(), any(), anyLong(), any());
        verify(mReadAccessLogsHelper, times(0))
                .recordAccessLogForAggregationReads(
                        any(), any(), anyLong(), anyInt(), anyLong(), any());
    }

    @Test
    @DisableFlags({
        FLAG_ECOSYSTEM_METRICS,
        FLAG_ECOSYSTEM_METRICS_DB_CHANGES,
        FLAG_PERSONAL_HEALTH_RECORD_DATABASE,
        FLAG_ACTIVITY_INTENSITY_DB
    })
    public void flagsDisabled_readRecordsById_doNotAddReadAccessLog() {
        String readerPackage = "reader.package";
        mTransactionTestUtils.insertApp(readerPackage);
        String uuid =
                mTransactionTestUtils
                        .insertRecords(
                                TEST_PACKAGE_NAME,
                                createStepsRecord(
                                        mAppInfoHelper.getAppInfoId(TEST_PACKAGE_NAME),
                                        Instant.now().toEpochMilli(),
                                        Instant.now().toEpochMilli(),
                                        100))
                        .get(0);

        mFitnessRecordReadHelper.readRecords(
                mTransactionManager,
                readerPackage,
                ImmutableMap.of(RECORD_TYPE_STEPS, ImmutableList.of(UUID.fromString(uuid))),
                /* startDateAccessMillis= */ 0,
                /* grantedExtraReadPermissions */ Set.of(),
                /* isInForeground= */ true,
                /* shouldRecordAccessLogs */ true,
                /* isReadingSelfData= */ false);

        verify(mReadAccessLogsHelper, times(0))
                .recordAccessLogForNonAggregationReads(any(), any(), anyLong(), any());
        verify(mReadAccessLogsHelper, times(0))
                .recordAccessLogForAggregationReads(
                        any(), any(), anyLong(), anyInt(), anyLong(), any());
    }

    @Test
    @EnableFlags({
        FLAG_ECOSYSTEM_METRICS,
        FLAG_ECOSYSTEM_METRICS_DB_CHANGES,
        FLAG_PERSONAL_HEALTH_RECORD_DATABASE,
        FLAG_ACTIVITY_INTENSITY_DB
    })
    public void flagsEnabled_readRecordsAndPageToken_addReadAccessLog() {
        String readerPackage = "reader.package";
        mTransactionTestUtils.insertApp(readerPackage);
        mTransactionTestUtils.insertRecords(
                TEST_PACKAGE_NAME,
                createStepsRecord(
                        mAppInfoHelper.getAppInfoId(TEST_PACKAGE_NAME),
                        /* startTimeMillis= */ Instant.now().minusSeconds(1000).toEpochMilli(),
                        /* endTimeMillis= */ Instant.now().minusSeconds(500).toEpochMilli(),
                        100));
        ReadRecordsRequestUsingFilters<StepsRecord> request =
                new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                        .setTimeRangeFilter(
                                new TimeInstantRangeFilter.Builder()
                                        .setStartTime(Instant.EPOCH)
                                        .setEndTime(Instant.now())
                                        .build())
                        .setPageSize(1)
                        .build();
        mFitnessRecordReadHelper.readRecords(
                mTransactionManager,
                readerPackage,
                request.toReadRecordsRequestParcel(),
                /* startDateAccessMillis= */ 0,
                /* enforceSelfRead= */ false,
                /* grantedExtraReadPermissions */ Set.of(),
                /* isInForeground= */ true,
                /* shouldRecordAccessLogs */ true,
                /* packageNamesByAppIds= */ null);

        List<ReadAccessLogsHelper.ReadAccessLog> readAccessLogs =
                mReadAccessLogsHelper.queryReadAccessLogs(0).getReadAccessLogs();
        assertThat(readAccessLogs.size()).isEqualTo(1);
        ReadAccessLogsHelper.ReadAccessLog readAccessLog = readAccessLogs.get(0);
        assertThat(readAccessLog.getRecordWithinPast30Days()).isEqualTo(true);
        assertThat(readAccessLog.getWriterPackage()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(readAccessLog.getDataType()).isEqualTo(RecordTypeIdentifier.RECORD_TYPE_STEPS);
        assertThat(readAccessLog.getReaderPackage()).isEqualTo(readerPackage);
    }

    @Test
    @EnableFlags({
        FLAG_ECOSYSTEM_METRICS,
        FLAG_ECOSYSTEM_METRICS_DB_CHANGES,
        FLAG_PERSONAL_HEALTH_RECORD_DATABASE,
        FLAG_ACTIVITY_INTENSITY_DB
    })
    public void flagsEnabled_doNotRecordAccessLogs_readRecordsAndPageToken_doNotReadAccessLog() {
        String readerPackage = "reader.package";
        mTransactionTestUtils.insertApp(readerPackage);
        mTransactionTestUtils.insertRecords(
                TEST_PACKAGE_NAME,
                createStepsRecord(
                        mAppInfoHelper.getAppInfoId(TEST_PACKAGE_NAME),
                        Instant.now().minusMillis(1000).toEpochMilli(),
                        Instant.now().minusMillis(500).toEpochMilli(),
                        100));
        ReadRecordsRequestUsingFilters<StepsRecord> request =
                new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                        .setTimeRangeFilter(
                                new TimeInstantRangeFilter.Builder()
                                        .setStartTime(Instant.EPOCH)
                                        .setEndTime(Instant.now())
                                        .build())
                        .setPageSize(1)
                        .build();
        mFitnessRecordReadHelper.readRecords(
                mTransactionManager,
                readerPackage,
                request.toReadRecordsRequestParcel(),
                /* startDateAccessMillis= */ 0,
                /* enforceSelfRead= */ false,
                /* grantedExtraReadPermissions */ Set.of(),
                /* isInForeground= */ true,
                /* shouldRecordAccessLogs */ false,
                /* packageNamesByAppIds= */ null);

        verify(mReadAccessLogsHelper, times(0))
                .recordAccessLogForNonAggregationReads(any(), any(), anyLong(), any());
        verify(mReadAccessLogsHelper, times(0))
                .recordAccessLogForAggregationReads(
                        any(), any(), anyLong(), anyInt(), anyLong(), any());
    }

    @Test
    @DisableFlags({
        FLAG_ECOSYSTEM_METRICS,
        FLAG_ECOSYSTEM_METRICS_DB_CHANGES,
        FLAG_PERSONAL_HEALTH_RECORD_DATABASE,
        FLAG_ACTIVITY_INTENSITY_DB
    })
    public void flagsDisabled_readRecordsAndPageToken_doNotReadAccessLog() {
        String readerPackage = "reader.package";
        mTransactionTestUtils.insertApp(readerPackage);
        mTransactionTestUtils.insertRecords(
                TEST_PACKAGE_NAME,
                createStepsRecord(
                        mAppInfoHelper.getAppInfoId(TEST_PACKAGE_NAME),
                        Instant.now().minusMillis(1000).toEpochMilli(),
                        Instant.now().minusMillis(500).toEpochMilli(),
                        100));
        ReadRecordsRequestUsingFilters<StepsRecord> request =
                new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                        .setTimeRangeFilter(
                                new TimeInstantRangeFilter.Builder()
                                        .setStartTime(Instant.EPOCH)
                                        .setEndTime(Instant.now())
                                        .build())
                        .setPageSize(1)
                        .build();
        mFitnessRecordReadHelper.readRecords(
                mTransactionManager,
                readerPackage,
                request.toReadRecordsRequestParcel(),
                /* startDateAccessMillis= */ 0,
                /* enforceSelfRead= */ false,
                /* grantedExtraReadPermissions */ Set.of(),
                /* isInForeground= */ true,
                /* shouldRecordAccessLogs */ true,
                /* packageNamesByAppIds= */ null);

        verify(mReadAccessLogsHelper, times(0))
                .recordAccessLogForNonAggregationReads(any(), any(), anyLong(), any());
        verify(mReadAccessLogsHelper, times(0))
                .recordAccessLogForAggregationReads(
                        any(), any(), anyLong(), anyInt(), anyLong(), any());
    }

    @Test
    @EnableFlags({
        FLAG_ECOSYSTEM_METRICS,
        FLAG_ECOSYSTEM_METRICS_DB_CHANGES,
        FLAG_PERSONAL_HEALTH_RECORD_DATABASE,
        FLAG_ACTIVITY_INTENSITY_DB
    })
    public void flagsEnabled_readSelfData_readRecordsAndPageToken_doNotAddReadAccessLog() {
        mTransactionTestUtils.insertRecords(
                TEST_PACKAGE_NAME,
                createStepsRecord(
                        mAppInfoHelper.getAppInfoId(TEST_PACKAGE_NAME),
                        /* startTimeMillis= */ Instant.now().minusSeconds(1000).toEpochMilli(),
                        /* endTimeMillis= */ Instant.now().minusSeconds(500).toEpochMilli(),
                        100));

        ReadRecordsRequestUsingFilters<StepsRecord> request =
                new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                        .addDataOrigins(getDataOrigin(TEST_PACKAGE_NAME))
                        .build();
        mFitnessRecordReadHelper.readRecords(
                mTransactionManager,
                TEST_PACKAGE_NAME,
                request.toReadRecordsRequestParcel(),
                /* startDateAccessMillis= */ 0,
                /* enforceSelfRead= */ false,
                /* grantedExtraReadPermissions */ Set.of(),
                /* isInForeground= */ true,
                /* shouldRecordAccessLogs */ true,
                /* packageNamesByAppIds= */ null);

        verify(mReadAccessLogsHelper, times(0))
                .recordAccessLogForNonAggregationReads(any(), any(), anyLong(), any());
        verify(mReadAccessLogsHelper, times(0))
                .recordAccessLogForAggregationReads(
                        any(), any(), anyLong(), anyInt(), anyLong(), any());
    }
}
