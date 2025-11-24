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
import static android.healthconnect.testing.shared.DataFactory.getDataOrigin;
import static android.healthconnect.testing.unittest.RecordInternalFactory.buildBloodPressureRecord;
import static android.healthconnect.testing.unittest.RecordInternalFactory.buildExerciseSessionRecordWithRoute;
import static android.healthconnect.testing.unittest.RecordInternalFactory.buildExerciseSessionRecordWithSegment;
import static android.healthconnect.testing.unittest.RecordInternalFactory.buildStepsRecord;

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
import android.health.connect.datatypes.SymptomRecord;
import android.health.connect.internal.datatypes.ExerciseSessionRecordInternal;
import android.health.connect.internal.datatypes.RecordInternal;
import android.health.connect.internal.datatypes.SymptomRecordInternal;
import android.healthconnect.testing.unittest.FitnessTestUtils;
import android.os.UserHandle;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.util.Pair;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.healthfitness.flags.Flags;
import com.android.server.healthconnect.common.accesslog.AccessLogsHelper;
import com.android.server.healthconnect.common.accesslog.AppOpLogsHelper;
import com.android.server.healthconnect.common.accesslog.ReadAccessLogsHelper;
import com.android.server.healthconnect.common.metadata.AppInfoHelper;
import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;
import com.android.server.healthconnect.permission.FirstGrantTimeManager;
import com.android.server.healthconnect.permission.HealthPermissionIntentAppsTracker;
import com.android.server.healthconnect.storage.TransactionManager;

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

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final TemporaryFolder mEnvironmentDataDir = new TemporaryFolder();

    private static final String TEST_PACKAGE_NAME = "package.name";
    private static final String FOO_PACKAGE_NAME = "package.foo";
    private static final String BAR_PACKAGE_NAME = "package.bar";
    private static final String UNKNOWN_PACKAGE_NAME = "package.unknown";
    private static final Set<String> WRITE_EXERCISE_ROUTE_EXTRA_PERM = Set.of(WRITE_EXERCISE_ROUTE);
    private static final Set<String> NO_GRANULAR_PERMS = Set.of();

    @Mock private FirstGrantTimeManager mFirstGrantTimeManager;
    @Mock private HealthPermissionIntentAppsTracker mPermissionIntentAppsTracker;
    @Mock private AppOpLogsHelper mAppOpLogsHelper;

    private UserHandle mUserHandle;
    private TransactionManager mTransactionManager;
    private FitnessRecordReadHelper mFitnessRecordReadHelper;
    private AppInfoHelper mAppInfoHelper;
    private AccessLogsHelper mAccessLogsHelper;
    private ReadAccessLogsHelper mReadAccessLogsHelper;
    private FitnessTestUtils mFitnessTestUtils;

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
        mFitnessTestUtils = new FitnessTestUtils(healthConnectInjector);

        mFitnessTestUtils.insertApp(TEST_PACKAGE_NAME);
        mFitnessTestUtils.insertApp(FOO_PACKAGE_NAME);
        mFitnessTestUtils.insertApp(BAR_PACKAGE_NAME);
    }

    @Test
    public void readRecordsByIdRequest_returnsAllRecords() {
        long timeMillis = 456;
        String uuid =
                mFitnessTestUtils
                        .insertRecords(
                                TEST_PACKAGE_NAME,
                                buildBloodPressureRecord(timeMillis, 120.0, 80.0))
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
                                NO_GRANULAR_PERMS,
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
    public void readRecords_sessionWithChild_readsChild() {
        long timeMillis = 456;
        String uuid =
                mFitnessTestUtils
                        .insertRecords(
                                TEST_PACKAGE_NAME,
                                buildExerciseSessionRecordWithSegment(
                                        Instant.ofEpochSecond(timeMillis)))
                        .get(0);

        ReadRecordsRequestUsingIds<ExerciseSessionRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(ExerciseSessionRecord.class)
                        .addId(uuid)
                        .build();

        List<RecordInternal<?>> records =
                mFitnessRecordReadHelper.readRecords(
                                mTransactionManager,
                                TEST_PACKAGE_NAME,
                                request.toReadRecordsRequestParcel(),
                                /* grantedExtraReadPermissions= */ Set.of(),
                                NO_GRANULAR_PERMS,
                                /* startDateAccessMillis= */ 0,
                                /* isInForeground= */ false,
                                /* shouldRecordAccessLogs= */ false,
                                /* enforceSelfRead */ false,
                                /* packageNamesByAppIds= */ null)
                        .first;
        assertThat(records).hasSize(1);
        ExerciseSessionRecordInternal readRecord = (ExerciseSessionRecordInternal) records.get(0);
        assertThat(readRecord.getUuid()).isEqualTo(UUID.fromString(uuid));
        assertThat(readRecord.getSegments()).hasSize(1);
    }

    @Test
    public void readRecordsByIdRequest_ignoresMissingIds() {
        long timeMillis = 456;
        String uuid =
                mFitnessTestUtils
                        .insertRecords(
                                TEST_PACKAGE_NAME,
                                buildBloodPressureRecord(timeMillis, 120.0, 80.0))
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
                                NO_GRANULAR_PERMS,
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
                NO_GRANULAR_PERMS,
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
                mFitnessTestUtils.insertRecords(
                        TEST_PACKAGE_NAME,
                        buildStepsRecord(400, 500, 100),
                        buildStepsRecord(500, 600, 100));

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
                        NO_GRANULAR_PERMS,
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
    public void readRecordsByFilterRequest_shouldRecordAccessLogs_accessLogRecorded() {
        ReadRecordsRequestUsingFilters<StepsRecord> request =
                new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class).build();

        mFitnessRecordReadHelper.readRecords(
                mTransactionManager,
                TEST_PACKAGE_NAME,
                request.toReadRecordsRequestParcel(),
                /* grantedExtraReadPermissions= */ Set.of(),
                NO_GRANULAR_PERMS,
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
                NO_GRANULAR_PERMS,
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
                mFitnessTestUtils.insertRecords(
                        TEST_PACKAGE_NAME,
                        buildStepsRecord(startTimeMillis, endTimeMillis, 100),
                        buildBloodPressureRecord(endTimeMillis, 120.0, 80.0));

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
                        NO_GRANULAR_PERMS,
                        /* startDateAccessMillis= */ 0,
                        /* isInForeground= */ false,
                        /* shouldRecordAccessLogs= */ false);

        assertThat(records).hasSize(2);
        assertThat(records.get(0).getUuid()).isEqualTo(UUID.fromString(uuids.get(0)));
        assertThat(records.get(1).getUuid()).isEqualTo(UUID.fromString(uuids.get(1)));
    }

    @Test
    public void readRecordsById_missingRecords_returnsExistingRecords() {
        long startTimeMillis = 123;
        long endTimeMillis = 456;
        List<String> uuids =
                mFitnessTestUtils.insertRecords(
                        TEST_PACKAGE_NAME,
                        buildStepsRecord(startTimeMillis, endTimeMillis, 100),
                        buildBloodPressureRecord(endTimeMillis, 120.0, 80.0));

        List<UUID> stepsUuids = ImmutableList.of(UUID.fromString(uuids.get(0)));
        // Add an extra non-existent id.
        List<UUID> bloodPressureUuids =
                ImmutableList.of(UUID.fromString(uuids.get(1)), UUID.randomUUID());
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
                        NO_GRANULAR_PERMS,
                        /* startDateAccessMillis= */ 0,
                        /* isInForeground= */ false,
                        /* shouldRecordAccessLogs= */ false);

        assertThat(records).hasSize(2);
        assertThat(records.get(0).getUuid()).isEqualTo(UUID.fromString(uuids.get(0)));
        assertThat(records.get(1).getUuid()).isEqualTo(UUID.fromString(uuids.get(1)));
    }

    @Test
    public void flagsEnabled_readRecordsById_addReadAccessLog() {
        String readerPackage = "reader.package";
        mFitnessTestUtils.insertApp(readerPackage);
        String uuid =
                mFitnessTestUtils
                        .insertRecords(
                                TEST_PACKAGE_NAME,
                                buildStepsRecord(
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
                NO_GRANULAR_PERMS,
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

    // TODO(b/366149374): Fix this test to start recording read access log.
    public void flagsEnabled_readRecordsByIdRequest_shouldRecordAccessLogs_doNotAddReadAccessLog() {
        String readerPackage = "reader.package";
        mFitnessTestUtils.insertApp(readerPackage);
        mFitnessTestUtils.insertRecords(
                TEST_PACKAGE_NAME,
                buildStepsRecord(
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
                NO_GRANULAR_PERMS,
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
    public void flagsEnabled_readRecordsById_shouldNotRecordAccessLogs_doNotAddReadAccessLog() {
        String readerPackage = "reader.package";
        mFitnessTestUtils.insertApp(readerPackage);
        String uuid =
                mFitnessTestUtils
                        .insertRecords(
                                TEST_PACKAGE_NAME,
                                buildStepsRecord(
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
                NO_GRANULAR_PERMS,
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
    public void flagsEnabled_readRecordsAndPageToken_addReadAccessLog() {
        String readerPackage = "reader.package";
        mFitnessTestUtils.insertApp(readerPackage);
        mFitnessTestUtils.insertRecords(
                TEST_PACKAGE_NAME,
                buildStepsRecord(
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
                NO_GRANULAR_PERMS,
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
    public void flagsEnabled_doNotRecordAccessLogs_readRecordsAndPageToken_doNotReadAccessLog() {
        String readerPackage = "reader.package";
        mFitnessTestUtils.insertApp(readerPackage);
        mFitnessTestUtils.insertRecords(
                TEST_PACKAGE_NAME,
                buildStepsRecord(
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
                NO_GRANULAR_PERMS,
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
    public void flagsEnabled_readSelfData_readRecordsAndPageToken_doNotAddReadAccessLog() {
        mFitnessTestUtils.insertRecords(
                TEST_PACKAGE_NAME,
                buildStepsRecord(
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
                NO_GRANULAR_PERMS,
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
    public void readRecordsByIds_emptyIds_returnsEmptyList() {
        buildExerciseSessionRecordWithRoute(Instant.ofEpochSecond(10000));

        List<RecordInternal<?>> returnedRecords =
                mFitnessRecordReadHelper.readRecords(
                        mTransactionManager,
                        TEST_PACKAGE_NAME,
                        ImmutableMap.of(
                                RecordTypeIdentifier.RECORD_TYPE_EXERCISE_SESSION,
                                ImmutableList.of()),
                        WRITE_EXERCISE_ROUTE_EXTRA_PERM,
                        NO_GRANULAR_PERMS,
                        /* startDateAccessMillis= */ 0,
                        /* isInForeground= */ true,
                        /* shouldRecordAccessLogs= */ false);

        assertThat(returnedRecords).isEmpty();
    }

    @Test
    public void readRecordsByIds_onlyWriteRoutePermission_doesNotReturnRoutesOfOtherApps() {
        ExerciseSessionRecordInternal fooSession =
                buildExerciseSessionRecordWithRoute(Instant.ofEpochSecond(10000));
        ExerciseSessionRecordInternal barSession =
                buildExerciseSessionRecordWithRoute(Instant.ofEpochSecond(11000));
        ExerciseSessionRecordInternal ownSession =
                buildExerciseSessionRecordWithRoute(Instant.ofEpochSecond(12000));
        String fooUuid = mFitnessTestUtils.insertRecords(FOO_PACKAGE_NAME, fooSession).get(0);
        String barUuid = mFitnessTestUtils.insertRecords(BAR_PACKAGE_NAME, barSession).get(0);
        String ownUuid = mFitnessTestUtils.insertRecords(TEST_PACKAGE_NAME, ownSession).get(0);
        List<UUID> allUuids = Stream.of(fooUuid, barUuid, ownUuid).map(UUID::fromString).toList();

        List<RecordInternal<?>> returnedRecords =
                mFitnessRecordReadHelper.readRecords(
                        mTransactionManager,
                        TEST_PACKAGE_NAME,
                        ImmutableMap.of(
                                RecordTypeIdentifier.RECORD_TYPE_EXERCISE_SESSION, allUuids),
                        WRITE_EXERCISE_ROUTE_EXTRA_PERM,
                        NO_GRANULAR_PERMS,
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
                buildExerciseSessionRecordWithRoute(Instant.ofEpochSecond(12000));
        UUID uuid =
                UUID.fromString(mFitnessTestUtils.insertRecords(TEST_PACKAGE_NAME, session).get(0));

        List<RecordInternal<?>> returnedRecords =
                mFitnessRecordReadHelper.readRecords(
                        mTransactionManager,
                        UNKNOWN_PACKAGE_NAME,
                        ImmutableMap.of(
                                RecordTypeIdentifier.RECORD_TYPE_EXERCISE_SESSION, List.of(uuid)),
                        /* grantedExtraReadPermissions= */ WRITE_EXERCISE_ROUTE_EXTRA_PERM,
                        NO_GRANULAR_PERMS,
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
                buildExerciseSessionRecordWithRoute(Instant.ofEpochSecond(12000));
        UUID uuid =
                UUID.fromString(mFitnessTestUtils.insertRecords(TEST_PACKAGE_NAME, session).get(0));
        List<RecordInternal<?>> returnedRecords =
                mFitnessRecordReadHelper.readRecords(
                        mTransactionManager,
                        UNKNOWN_PACKAGE_NAME,
                        ImmutableMap.of(
                                RecordTypeIdentifier.RECORD_TYPE_EXERCISE_SESSION, List.of(uuid)),
                        Set.of(HealthPermissions.READ_EXERCISE_ROUTE),
                        NO_GRANULAR_PERMS,
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
                buildExerciseSessionRecordWithRoute(Instant.ofEpochSecond(10000));
        ExerciseSessionRecordInternal barSession =
                buildExerciseSessionRecordWithRoute(Instant.ofEpochSecond(11000));
        ExerciseSessionRecordInternal ownSession =
                buildExerciseSessionRecordWithRoute(Instant.ofEpochSecond(12000));
        String fooUuid = mFitnessTestUtils.insertRecords(FOO_PACKAGE_NAME, fooSession).get(0);
        String barUuid = mFitnessTestUtils.insertRecords(BAR_PACKAGE_NAME, barSession).get(0);
        String ownUuid = mFitnessTestUtils.insertRecords(TEST_PACKAGE_NAME, ownSession).get(0);

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
                                NO_GRANULAR_PERMS,
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
    public void readRecordsAndPageToken_byFilters_withReadRoutePermission_returnsRoute() {
        ExerciseSessionRecordInternal session =
                buildExerciseSessionRecordWithRoute(Instant.ofEpochSecond(12000));
        mFitnessTestUtils.insertRecords(TEST_PACKAGE_NAME, session);

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
                                NO_GRANULAR_PERMS,
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
        assertThat(returnedRecord.getRoute()).isEqualTo(session.getRoute());
    }

    @Test
    public void readRecordsAndPageToken_byFilters_unknownApp_inBackground_doesntReturnRoute() {
        ExerciseSessionRecordInternal session =
                buildExerciseSessionRecordWithRoute(Instant.ofEpochSecond(12000));
        mFitnessTestUtils.insertRecords(TEST_PACKAGE_NAME, session);

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
                                NO_GRANULAR_PERMS,
                                /* startDateAccessMillis= */ 0,
                                /* isInForeground= */ false,
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
    @RequiresFlagsEnabled({Flags.FLAG_SYMPTOMS, Flags.FLAG_SYMPTOMS_DB})
    public void readRecords_withGranularPermissions_filtersCorrectly() {
        mFitnessTestUtils.insertRecords(
                TEST_PACKAGE_NAME,
                new SymptomRecordInternal()
                        .setSymptomType(SymptomRecord.SYMPTOM_TYPE_COUGH)
                        .setStartTime(1000L)
                        .setEndTime(2000L),
                new SymptomRecordInternal()
                        .setSymptomType(SymptomRecord.SYMPTOM_TYPE_FEVER)
                        .setStartTime(3000L)
                        .setEndTime(4000L));
        ReadRecordsRequestParcel request =
                new ReadRecordsRequestUsingFilters.Builder<>(SymptomRecord.class)
                        .setTimeRangeFilter(
                                new TimeInstantRangeFilter.Builder()
                                        .setStartTime(Instant.EPOCH)
                                        .setEndTime(Instant.now())
                                        .build())
                        .build()
                        .toReadRecordsRequestParcel();

        Pair<List<RecordInternal<?>>, PageTokenWrapper> result =
                mFitnessRecordReadHelper.readRecords(
                        mTransactionManager,
                        TEST_PACKAGE_NAME,
                        request,
                        /* grantedExtraReadPermissions= */ Set.of(),
                        /* grantedGranularPermissions= */ Set.of(
                                HealthPermissions.READ_SYMPTOM_COUGH),
                        /* startDateAccessMillis= */ 0,
                        /* isInForeground= */ true,
                        /* shouldRecordAccessLogs */ false,
                        /* enforceSelfRead= */ false,
                        /* packageNamesByAppIds= */ null);

        assertThat(result.first).hasSize(1);
        SymptomRecordInternal returnedRecord = (SymptomRecordInternal) result.first.get(0);
        assertThat(returnedRecord.getSymptomType()).isEqualTo(SymptomRecord.SYMPTOM_TYPE_COUGH);
    }
}
