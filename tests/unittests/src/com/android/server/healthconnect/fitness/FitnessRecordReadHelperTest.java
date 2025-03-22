/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.server.healthconnect.fitness;

import static android.health.connect.HealthPermissions.WRITE_EXERCISE_ROUTE;
import static android.health.connect.accesslog.AccessLog.OperationType.OPERATION_TYPE_READ;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_STEPS;
import static android.healthconnect.cts.utils.DataFactory.getDataOrigin;

import static com.android.healthfitness.flags.Flags.FLAG_ACTIVITY_INTENSITY_DB;
import static com.android.healthfitness.flags.Flags.FLAG_CLOUD_BACKUP_AND_RESTORE_DB;
import static com.android.healthfitness.flags.Flags.FLAG_ECOSYSTEM_METRICS;
import static com.android.healthfitness.flags.Flags.FLAG_ECOSYSTEM_METRICS_DB_CHANGES;
import static com.android.healthfitness.flags.Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE;
import static com.android.server.healthconnect.testing.storage.TransactionTestUtils.createBloodPressureRecord;
import static com.android.server.healthconnect.testing.storage.TransactionTestUtils.createExerciseSessionRecordWithRoute;
import static com.android.server.healthconnect.testing.storage.TransactionTestUtils.createStepsRecord;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.health.connect.HealthPermissions;
import android.health.connect.PageTokenWrapper;
import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.ReadRecordsRequestUsingIds;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.accesslog.AccessLog;
import android.health.connect.aidl.ReadRecordsRequestParcel;
import android.health.connect.datatypes.BloodPressureRecord;
import android.health.connect.datatypes.ExerciseSessionRecord;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.health.connect.datatypes.StepsRecord;
import android.health.connect.internal.datatypes.ExerciseSessionRecordInternal;
import android.health.connect.internal.datatypes.RecordInternal;
import android.os.UserHandle;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.util.Pair;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.healthfitness.flags.Flags;
import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;
import com.android.server.healthconnect.permission.FirstGrantTimeManager;
import com.android.server.healthconnect.permission.HealthPermissionIntentAppsTracker;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.datatypehelpers.AccessLogsHelper;
import com.android.server.healthconnect.storage.datatypehelpers.AppInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.AppOpLogsHelper;
import com.android.server.healthconnect.storage.datatypehelpers.ReadAccessLogsHelper;
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
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RunWith(AndroidJUnit4.class)
public class FitnessRecordReadHelperTest {

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final TemporaryFolder mEnvironmentDataDir = new TemporaryFolder();

    private static final String TEST_PACKAGE_NAME = "package.name";
    private static final String FOO_PACKAGE_NAME = "package.foo";
    private static final String BAR_PACKAGE_NAME = "package.bar";
    private static final String UNKNOWN_PACKAGE_NAME = "package.unknown";
    private static final Set<String> WRITE_EXERCISE_ROUTE_EXTRA_PERM = Set.of(WRITE_EXERCISE_ROUTE);

    @Mock private FirstGrantTimeManager mFirstGrantTimeManager;
    @Mock private HealthPermissionIntentAppsTracker mPermissionIntentAppsTracker;
    @Mock private AppOpLogsHelper mAppOpLogsHelper;

    private UserHandle mUserHandle;
    private TransactionManager mTransactionManager;
    private FitnessRecordReadHelper mFitnessRecordReadHelper;
    private AppInfoHelper mAppInfoHelper;
    private AccessLogsHelper mAccessLogsHelper;
    private ReadAccessLogsHelper mReadAccessLogsHelper;
    private TransactionTestUtils mTransactionTestUtils;

    @Before
    public void setup() {
        Context context = ApplicationProvider.getApplicationContext();
        mUserHandle = context.getUser();
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
        mTransactionTestUtils = new TransactionTestUtils(healthConnectInjector);

        mTransactionTestUtils.insertApp(TEST_PACKAGE_NAME);
        mTransactionTestUtils.insertApp(FOO_PACKAGE_NAME);
        mTransactionTestUtils.insertApp(BAR_PACKAGE_NAME);
    }

    @Test
    public void readRecordsByIdRequest_returnsAllRecords() {
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
                                /* grantedExtraReadPermissions= */ Set.of(),
                                /* startDateAccessMillis= */ 0,
                                /* isInForeground= */ false,
                                /* shouldRecordAccessLogs= */ false,
                                /* enforceSelfRead */ false,
                                /* packageNamesByAppIds= */ null)
                        .first;
        assertThat(records).hasSize(1);
        assertThat(records.get(0).getUuid()).isEqualTo(UUID.fromString(uuid));
    }

    @Test
    public void readRecordsByIdRequest_ignoresMissingIds() {
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
                                /* grantedExtraReadPermissions= */ Set.of(),
                                /* startDateAccessMillis= */ 0,
                                /* isInForeground= */ false,
                                /* shouldRecordAccessLogs= */ false,
                                /* enforceSelfRead */ false,
                                /* packageNamesByAppIds= */ null)
                        .first;
        assertThat(records).hasSize(1);
        assertThat(records.get(0).getUuid()).isEqualTo(UUID.fromString(uuid));
    }

    @Test
    @EnableFlags(Flags.FLAG_ADD_MISSING_ACCESS_LOGS)
    public void readRecordsByIdRequest_accessLogged() {
        ReadRecordsRequestUsingIds<BloodPressureRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(BloodPressureRecord.class)
                        .addClientRecordId("id")
                        .build();
        mFitnessRecordReadHelper.readRecords(
                mTransactionManager,
                TEST_PACKAGE_NAME,
                request.toReadRecordsRequestParcel(),
                /* grantedExtraReadPermissions= */ Set.of(),
                /* startDateAccessMillis= */ 0,
                /* isInForeground= */ false,
                /* shouldRecordAccessLogs= */ true,
                /* enforceSelfRead */ false,
                /* packageNamesByAppIds= */ null);

        List<AccessLog> result = mAccessLogsHelper.queryAccessLogs(mUserHandle);
        assertThat(result).hasSize(1);
        AccessLog log = result.get(0);
        assertThat(log.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(log.getRecordTypes()).containsExactly(BloodPressureRecord.class);
        assertThat(log.getOperationType()).isEqualTo(OPERATION_TYPE_READ);
    }

    @Test
    public void readRecordsByFilterRequest_returnsRecordsAndPageToken() {
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
                        /* grantedExtraReadPermissions= */ Set.of(),
                        /* startDateAccessMillis= */ 0,
                        /* isInForeground= */ false,
                        /* shouldRecordAccessLogs= */ true,
                        /* enforceSelfRead */ false,
                        /* packageNamesByAppIds= */ null);

        List<RecordInternal<?>> records = result.first;
        assertThat(records).hasSize(1);
        assertThat(result.first.get(0).getUuid()).isEqualTo(UUID.fromString(uuids.get(0)));
        assertThat(result.second).isEqualTo(expectedToken);
    }

    @Test
    @EnableFlags(Flags.FLAG_ADD_MISSING_ACCESS_LOGS)
    public void readRecordsByFilterRequest_shouldRecordAccessLogs_accessLogRecorded() {
        ReadRecordsRequestUsingFilters<StepsRecord> request =
                new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class).build();

        mFitnessRecordReadHelper.readRecords(
                mTransactionManager,
                TEST_PACKAGE_NAME,
                request.toReadRecordsRequestParcel(),
                /* grantedExtraReadPermissions= */ Set.of(),
                /* startDateAccessMillis= */ 0,
                /* isInForeground= */ false,
                /* shouldRecordAccessLogs= */ true,
                /* enforceSelfRead */ false,
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
    public void readRecordsByFilterRequest_filterForSelf_accessLogRecorded() {
        ReadRecordsRequestUsingFilters<StepsRecord> request =
                new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                        .addDataOrigins(getDataOrigin(TEST_PACKAGE_NAME))
                        .build();

        mFitnessRecordReadHelper.readRecords(
                mTransactionManager,
                TEST_PACKAGE_NAME,
                request.toReadRecordsRequestParcel(),
                /* grantedExtraReadPermissions= */ Set.of(),
                /* startDateAccessMillis= */ 0,
                /* isInForeground= */ false,
                /* shouldRecordAccessLogs= */ true,
                /* enforceSelfRead */ false,
                /* packageNamesByAppIds= */ null);

        List<AccessLog> result = mAccessLogsHelper.queryAccessLogs(mUserHandle);
        assertThat(result).hasSize(1);
        AccessLog log = result.get(0);
        assertThat(log.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(log.getRecordTypes()).containsExactly(StepsRecord.class);
        assertThat(log.getOperationType()).isEqualTo(OPERATION_TYPE_READ);
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
                        /* grantedExtraReadPermissions= */ Set.of(),
                        /* startDateAccessMillis= */ 0,
                        /* isInForeground= */ false,
                        /* shouldRecordAccessLogs= */ false);

        assertThat(records).hasSize(2);
        assertThat(records.get(0).getUuid()).isEqualTo(UUID.fromString(uuids.get(0)));
        assertThat(records.get(1).getUuid()).isEqualTo(UUID.fromString(uuids.get(1)));
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
                /* grantedExtraReadPermissions= */ Set.of(),
                /* startDateAccessMillis= */ 0,
                /* isInForeground= */ true,
                /* shouldRecordAccessLogs */ true);

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
    // TODO(b/366149374): Fix this test to start recording read access log.
    public void flagsEnabled_readRecordsByIdRequest_shouldRecordAccessLogs_doNotAddReadAccessLog() {
        String readerPackage = "reader.package";
        mTransactionTestUtils.insertApp(readerPackage);
        mTransactionTestUtils.insertRecords(
                TEST_PACKAGE_NAME,
                createStepsRecord(
                        mAppInfoHelper.getAppInfoId(TEST_PACKAGE_NAME),
                        Instant.now().toEpochMilli(),
                        Instant.now().toEpochMilli(),
                        100));

        mFitnessRecordReadHelper.readRecords(
                mTransactionManager,
                readerPackage,
                new ReadRecordsRequestUsingIds.Builder<>(StepsRecord.class)
                        .addClientRecordId("id")
                        .build()
                        .toReadRecordsRequestParcel(),
                /* grantedExtraReadPermissions= */ Set.of(),
                /* startDateAccessMillis= */ 0,
                /* isInForeground= */ true,
                /* shouldRecordAccessLogs */ true,
                /* enforceSelfRead= */ false,
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
    public void flagsEnabled_readRecordsById_shouldNotRecordAccessLogs_doNotAddReadAccessLog() {
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
                /* grantedExtraReadPermissions= */ Set.of(),
                /* startDateAccessMillis= */ 0,
                /* isInForeground= */ true,
                /* shouldRecordAccessLogs */ false);

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
                /* grantedExtraReadPermissions= */ Set.of(),
                /* startDateAccessMillis= */ 0,
                /* isInForeground= */ true,
                /* shouldRecordAccessLogs */ true);

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
                /* grantedExtraReadPermissions= */ Set.of(),
                /* startDateAccessMillis= */ 0,
                /* isInForeground= */ true,
                /* shouldRecordAccessLogs */ true,
                /* enforceSelfRead= */ false,
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
                /* grantedExtraReadPermissions= */ Set.of(),
                /* startDateAccessMillis= */ 0,
                /* isInForeground= */ true,
                /* shouldRecordAccessLogs */ false,
                /* enforceSelfRead= */ false,
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
                /* grantedExtraReadPermissions= */ Set.of(),
                /* startDateAccessMillis= */ 0,
                /* isInForeground= */ true,
                /* shouldRecordAccessLogs */ true,
                /* enforceSelfRead= */ false,
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
                /* grantedExtraReadPermissions= */ Set.of(),
                /* startDateAccessMillis= */ 0,
                /* isInForeground= */ true,
                /* shouldRecordAccessLogs */ true,
                /* enforceSelfRead= */ false,
                /* packageNamesByAppIds= */ null);

        verify(mReadAccessLogsHelper, times(0))
                .recordAccessLogForNonAggregationReads(any(), any(), anyLong(), any());
        verify(mReadAccessLogsHelper, times(0))
                .recordAccessLogForAggregationReads(
                        any(), any(), anyLong(), anyInt(), anyLong(), any());
    }

    @Test
    public void readRecordsByIds_onlyWriteRoutePermission_doesNotReturnRoutesOfOtherApps() {
        ExerciseSessionRecordInternal fooSession =
                createExerciseSessionRecordWithRoute(Instant.ofEpochSecond(10000));
        ExerciseSessionRecordInternal barSession =
                createExerciseSessionRecordWithRoute(Instant.ofEpochSecond(11000));
        ExerciseSessionRecordInternal ownSession =
                createExerciseSessionRecordWithRoute(Instant.ofEpochSecond(12000));
        String fooUuid = mTransactionTestUtils.insertRecords(FOO_PACKAGE_NAME, fooSession).get(0);
        String barUuid = mTransactionTestUtils.insertRecords(BAR_PACKAGE_NAME, barSession).get(0);
        String ownUuid = mTransactionTestUtils.insertRecords(TEST_PACKAGE_NAME, ownSession).get(0);
        List<UUID> allUuids = Stream.of(fooUuid, barUuid, ownUuid).map(UUID::fromString).toList();

        List<RecordInternal<?>> returnedRecords =
                mFitnessRecordReadHelper.readRecords(
                        mTransactionManager,
                        TEST_PACKAGE_NAME,
                        ImmutableMap.of(
                                RecordTypeIdentifier.RECORD_TYPE_EXERCISE_SESSION, allUuids),
                        WRITE_EXERCISE_ROUTE_EXTRA_PERM,
                        /* startDateAccessMillis= */ 0,
                        /* isInForeground= */ true,
                        /* shouldRecordAccessLogs= */ false);

        Map<String, ExerciseSessionRecordInternal> idToSessionMap =
                returnedRecords.stream()
                        .collect(
                                Collectors.toMap(
                                        record -> record.getUuid().toString(),
                                        ExerciseSessionRecordInternal.class::cast));
        assertThat(idToSessionMap.get(fooUuid).getRoute()).isNull();
        assertThat(idToSessionMap.get(barUuid).getRoute()).isNull();
        assertThat(idToSessionMap.get(ownUuid).getRoute()).isEqualTo(ownSession.getRoute());
        assertThat(idToSessionMap.get(fooUuid).hasRoute()).isTrue();
        assertThat(idToSessionMap.get(barUuid).hasRoute()).isTrue();
        assertThat(idToSessionMap.get(ownUuid).hasRoute()).isTrue();
    }

    @Test
    public void readRecordsByIds_unknownApp_doesNotReturnRoute() {
        ExerciseSessionRecordInternal session =
                createExerciseSessionRecordWithRoute(Instant.ofEpochSecond(12000));
        UUID uuid =
                UUID.fromString(
                        mTransactionTestUtils.insertRecords(TEST_PACKAGE_NAME, session).get(0));

        List<RecordInternal<?>> returnedRecords =
                mFitnessRecordReadHelper.readRecords(
                        mTransactionManager,
                        UNKNOWN_PACKAGE_NAME,
                        ImmutableMap.of(
                                RecordTypeIdentifier.RECORD_TYPE_EXERCISE_SESSION, List.of(uuid)),
                        /* startDateAccessMillis= */ WRITE_EXERCISE_ROUTE_EXTRA_PERM,
                        0,
                        /* isInForeground= */ true,
                        /* shouldRecordAccessLogs= */ false);

        assertThat(returnedRecords).hasSize(1);
        ExerciseSessionRecordInternal returnedRecord =
                (ExerciseSessionRecordInternal) returnedRecords.get(0);
        assertThat(returnedRecord.hasRoute()).isTrue();
        assertThat(returnedRecord.getRoute()).isNull();
    }

    @Test
    public void readRecordsByIds_unknownApp_withReadRoutePermission_returnsRoute() {
        ExerciseSessionRecordInternal session =
                createExerciseSessionRecordWithRoute(Instant.ofEpochSecond(12000));
        UUID uuid =
                UUID.fromString(
                        mTransactionTestUtils.insertRecords(TEST_PACKAGE_NAME, session).get(0));
        List<RecordInternal<?>> returnedRecords =
                mFitnessRecordReadHelper.readRecords(
                        mTransactionManager,
                        UNKNOWN_PACKAGE_NAME,
                        ImmutableMap.of(
                                RecordTypeIdentifier.RECORD_TYPE_EXERCISE_SESSION, List.of(uuid)),
                        Set.of(HealthPermissions.READ_EXERCISE_ROUTE),
                        /* startDateAccessMillis= */ 0,
                        /* isInForeground= */ true,
                        /* shouldRecordAccessLogs= */ false);

        assertThat(returnedRecords).hasSize(1);
        ExerciseSessionRecordInternal returnedRecord =
                (ExerciseSessionRecordInternal) returnedRecords.get(0);
        assertThat(returnedRecord.hasRoute()).isTrue();
        assertThat(returnedRecord.getRoute()).isEqualTo(session.getRoute());
    }

    @Test
    public void readRecordsAndPageToken_byFilters_doesNotReturnRoutesOfOtherApps() {
        ExerciseSessionRecordInternal fooSession =
                createExerciseSessionRecordWithRoute(Instant.ofEpochSecond(10000));
        ExerciseSessionRecordInternal barSession =
                createExerciseSessionRecordWithRoute(Instant.ofEpochSecond(11000));
        ExerciseSessionRecordInternal ownSession =
                createExerciseSessionRecordWithRoute(Instant.ofEpochSecond(12000));
        String fooUuid = mTransactionTestUtils.insertRecords(FOO_PACKAGE_NAME, fooSession).get(0);
        String barUuid = mTransactionTestUtils.insertRecords(BAR_PACKAGE_NAME, barSession).get(0);
        String ownUuid = mTransactionTestUtils.insertRecords(TEST_PACKAGE_NAME, ownSession).get(0);

        ReadRecordsRequestParcel request =
                new ReadRecordsRequestUsingFilters.Builder<>(ExerciseSessionRecord.class)
                        .setTimeRangeFilter(
                                new TimeInstantRangeFilter.Builder()
                                        .setStartTime(Instant.EPOCH)
                                        .setEndTime(Instant.ofEpochSecond(100000))
                                        .build())
                        .build()
                        .toReadRecordsRequestParcel();
        List<RecordInternal<?>> returnedRecords =
                mFitnessRecordReadHelper.readRecords(
                                mTransactionManager,
                                TEST_PACKAGE_NAME,
                                request,
                                WRITE_EXERCISE_ROUTE_EXTRA_PERM,
                                /* startDateAccessMillis= */ 0,
                                /* isInForeground= */ true,
                                /* shouldRecordAccessLogs */ false,
                                /* enforceSelfRead= */ false,
                                /* packageNamesByAppIds= */ null)
                        .first;

        Map<String, ExerciseSessionRecordInternal> idToSessionMap =
                returnedRecords.stream()
                        .collect(
                                Collectors.toMap(
                                        record -> record.getUuid().toString(),
                                        ExerciseSessionRecordInternal.class::cast));

        assertThat(idToSessionMap.get(fooUuid).getRoute()).isNull();
        assertThat(idToSessionMap.get(barUuid).getRoute()).isNull();
        assertThat(idToSessionMap.get(ownUuid).getRoute()).isEqualTo(ownSession.getRoute());
        assertThat(idToSessionMap.get(fooUuid).hasRoute()).isTrue();
        assertThat(idToSessionMap.get(barUuid).hasRoute()).isTrue();
        assertThat(idToSessionMap.get(ownUuid).hasRoute()).isTrue();
    }

    @Test
    public void readRecordsAndPageToken_byFilters_unknownApp_doesNotReturnRoute() {
        ExerciseSessionRecordInternal session =
                createExerciseSessionRecordWithRoute(Instant.ofEpochSecond(12000));
        mTransactionTestUtils.insertRecords(TEST_PACKAGE_NAME, session);

        ReadRecordsRequestParcel request =
                new ReadRecordsRequestUsingFilters.Builder<>(ExerciseSessionRecord.class)
                        .setTimeRangeFilter(
                                new TimeInstantRangeFilter.Builder()
                                        .setStartTime(Instant.EPOCH)
                                        .setEndTime(Instant.ofEpochSecond(100000))
                                        .build())
                        .build()
                        .toReadRecordsRequestParcel();
        List<RecordInternal<?>> returnedRecords =
                mFitnessRecordReadHelper.readRecords(
                                mTransactionManager,
                                UNKNOWN_PACKAGE_NAME,
                                request,
                                WRITE_EXERCISE_ROUTE_EXTRA_PERM,
                                /* startDateAccessMillis= */ 0,
                                /* isInForeground= */ true,
                                /* shouldRecordAccessLogs */ false,
                                /* enforceSelfRead= */ false,
                                /* packageNamesByAppIds= */ null)
                        .first;

        assertThat(returnedRecords).hasSize(1);
        ExerciseSessionRecordInternal returnedRecord =
                (ExerciseSessionRecordInternal) returnedRecords.get(0);
        assertThat(returnedRecord.hasRoute()).isTrue();
        assertThat(returnedRecord.getRoute()).isNull();
    }

    @Test
    public void readRecordsAndPageToken_byFilters_withReadRoutePermission_returnsRoute() {
        ExerciseSessionRecordInternal session =
                createExerciseSessionRecordWithRoute(Instant.ofEpochSecond(12000));
        mTransactionTestUtils.insertRecords(TEST_PACKAGE_NAME, session);

        ReadRecordsRequestParcel request =
                new ReadRecordsRequestUsingFilters.Builder<>(ExerciseSessionRecord.class)
                        .setTimeRangeFilter(
                                new TimeInstantRangeFilter.Builder()
                                        .setStartTime(Instant.EPOCH)
                                        .setEndTime(Instant.ofEpochSecond(100000))
                                        .build())
                        .build()
                        .toReadRecordsRequestParcel();
        List<RecordInternal<?>> returnedRecords =
                mFitnessRecordReadHelper.readRecords(
                                mTransactionManager,
                                TEST_PACKAGE_NAME,
                                request,
                                Set.of(HealthPermissions.READ_EXERCISE_ROUTE),
                                /* startDateAccessMillis= */ 0,
                                /* isInForeground= */ true,
                                /* shouldRecordAccessLogs */ false,
                                /* enforceSelfRead= */ false,
                                /* isInForeground= */
                                /* shouldRecordAccessLogs */
                                /* packageNamesByAppIds= */ null)
                        .first;

        assertThat(returnedRecords).hasSize(1);
        ExerciseSessionRecordInternal returnedRecord =
                (ExerciseSessionRecordInternal) returnedRecords.get(0);
        assertThat(returnedRecord.hasRoute()).isTrue();
        assertThat(returnedRecord.getRoute()).isEqualTo(session.getRoute());
    }
}
