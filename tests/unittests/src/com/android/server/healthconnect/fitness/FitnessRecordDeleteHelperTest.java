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

package com.android.server.healthconnect.fitness;

import static android.health.connect.Constants.DEFAULT_PAGE_SIZE;
import static android.health.connect.accesslog.AccessLog.OperationType.OPERATION_TYPE_DELETE;

import static com.android.server.healthconnect.testing.storage.TransactionTestUtils.createStepsRecord;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.health.connect.DeleteUsingFiltersRequest;
import android.health.connect.RecordIdFilter;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.accesslog.AccessLog;
import android.health.connect.aidl.DeleteUsingFiltersRequestParcel;
import android.health.connect.aidl.RecordIdFiltersParcel;
import android.health.connect.datatypes.HeartRateRecord;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.health.connect.datatypes.StepsRecord;
import android.health.connect.internal.datatypes.RecordInternal;
import android.os.UserHandle;
import android.platform.test.annotations.EnableFlags;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.healthfitness.flags.Flags;
import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;
import com.android.server.healthconnect.permission.FirstGrantTimeManager;
import com.android.server.healthconnect.permission.HealthPermissionIntentAppsTracker;
import com.android.server.healthconnect.storage.datatypehelpers.AccessLogsHelper;
import com.android.server.healthconnect.storage.datatypehelpers.AppOpLogsHelper;
import com.android.server.healthconnect.storage.datatypehelpers.RecordHelper;
import com.android.server.healthconnect.storage.request.DeleteTableRequest;
import com.android.server.healthconnect.storage.utils.InternalHealthConnectMappings;
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
import java.util.UUID;

@RunWith(AndroidJUnit4.class)
public class FitnessRecordDeleteHelperTest {
    private static final String TEST_PACKAGE_NAME = "package.name";

    private UserHandle mUserHandle;
    private AccessLogsHelper mAccessLogsHelper;
    private FitnessRecordDeleteHelper mFitnessRecordDeleteHelper;
    private InternalHealthConnectMappings mInternalHealthConnectMappings;
    private TransactionTestUtils mTransactionTestUtils;

    @Rule public final TemporaryFolder mEnvironmentDataDir = new TemporaryFolder();
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock private AppOpLogsHelper mAppOpLogsHelper;
    // TODO(b/373322447): Remove the mock FirstGrantTimeManager
    @Mock private FirstGrantTimeManager mFirstGrantTimeManager;
    // TODO(b/373322447): Remove the mock HealthPermissionIntentAppsTracker
    @Mock private HealthPermissionIntentAppsTracker mPermissionIntentAppsTracker;

    @Before
    public void setup() {
        Context context = ApplicationProvider.getApplicationContext();
        mUserHandle = context.getUser();
        HealthConnectInjector injector =
                HealthConnectInjectorImpl.newBuilderForTest(context)
                        .setFirstGrantTimeManager(mFirstGrantTimeManager)
                        .setHealthPermissionIntentAppsTracker(mPermissionIntentAppsTracker)
                        .setAppOpLogsHelper(mAppOpLogsHelper)
                        .setEnvironmentDataDirectory(mEnvironmentDataDir.getRoot())
                        .build();

        mFitnessRecordDeleteHelper = injector.getFitnessRecordDeleteHelper();
        mAccessLogsHelper = injector.getAccessLogsHelper();
        mInternalHealthConnectMappings = injector.getInternalHealthConnectMappings();
        mTransactionTestUtils = new TransactionTestUtils(injector);
        mTransactionTestUtils.insertApp(TEST_PACKAGE_NAME);
    }

    @Test
    public void deleteRecords_byIdFilter_generateChangeLogs() {
        List<String> uuids =
                mTransactionTestUtils.insertRecords(
                        TEST_PACKAGE_NAME, createStepsRecord(123, 456, 100));
        List<RecordIdFilter> ids = List.of(RecordIdFilter.fromId(StepsRecord.class, uuids.get(0)));

        DeleteUsingFiltersRequestParcel request =
                new DeleteUsingFiltersRequestParcel(
                        new RecordIdFiltersParcel(ids), TEST_PACKAGE_NAME);
        mFitnessRecordDeleteHelper.deleteRecords(
                TEST_PACKAGE_NAME,
                request,
                /* holdsDataManagementPermission */ false,
                /* shouldRecordAccessLog= */ false);
        List<UUID> uuidList = mTransactionTestUtils.getAllDeletedUuids();
        assertThat(uuidList).hasSize(1);
        assertThat(uuidList.get(0).toString()).isEqualTo(uuids.get(0));
    }

    @Test
    public void deleteRecords_byTimeFilter_generateChangeLogs() {
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
        mFitnessRecordDeleteHelper.deleteRecords(
                TEST_PACKAGE_NAME,
                new DeleteUsingFiltersRequestParcel(deleteRequest),
                /* holdsDataManagementPermission */ false,
                /* shouldRecordAccessLog= */ false);
        List<UUID> uuidList = mTransactionTestUtils.getAllDeletedUuids();
        assertThat(uuidList).hasSize(1);
        assertThat(uuidList.get(0).toString()).isEqualTo(uuids.get(0));
    }

    @Test
    public void deleteRecords_byTimeFilter_bulkDelete_generateChangeLogs() {
        ImmutableList.Builder<RecordInternal<?>> records = new ImmutableList.Builder<>();
        for (int i = 0; i <= DEFAULT_PAGE_SIZE; i++) {
            records.add(createStepsRecord(i * 1000L, (i + 1) * 1000L, 9527));
        }
        mTransactionTestUtils.insertRecords(TEST_PACKAGE_NAME, records.build());

        DeleteUsingFiltersRequest deleteRequest =
                new DeleteUsingFiltersRequest.Builder()
                        .setTimeRangeFilter(
                                new TimeInstantRangeFilter.Builder()
                                        .setStartTime(Instant.EPOCH)
                                        .build())
                        .build();
        mFitnessRecordDeleteHelper.deleteRecords(
                TEST_PACKAGE_NAME,
                new DeleteUsingFiltersRequestParcel(deleteRequest),
                /* holdsDataManagementPermission */ false,
                /* shouldRecordAccessLog= */ false);

        List<UUID> uuidList = mTransactionTestUtils.getAllDeletedUuids();
        assertThat(uuidList).hasSize(DEFAULT_PAGE_SIZE + 1);
    }

    @Test
    @EnableFlags(Flags.FLAG_ADD_MISSING_ACCESS_LOGS)
    public void deleteRecords_shouldRecordAccessLog_logged() {
        DeleteUsingFiltersRequest deleteRequest =
                new DeleteUsingFiltersRequest.Builder()
                        .addRecordType(StepsRecord.class)
                        .addRecordType(HeartRateRecord.class)
                        .setTimeRangeFilter(
                                new TimeInstantRangeFilter.Builder()
                                        .setStartTime(Instant.EPOCH)
                                        .build())
                        .build();
        mFitnessRecordDeleteHelper.deleteRecords(
                TEST_PACKAGE_NAME,
                new DeleteUsingFiltersRequestParcel(deleteRequest),
                /* holdsDataManagementPermission */ false,
                /* shouldRecordAccessLog= */ true);

        List<AccessLog> result = mAccessLogsHelper.queryAccessLogs(mUserHandle);
        assertThat(result).hasSize(1);
        AccessLog log = result.get(0);
        assertThat(log.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(log.getRecordTypes()).containsExactly(StepsRecord.class, HeartRateRecord.class);
        assertThat(log.getOperationType()).isEqualTo(OPERATION_TYPE_DELETE);
    }

    @Test
    @EnableFlags(Flags.FLAG_ADD_MISSING_ACCESS_LOGS)
    public void deleteRecords_shouldNotRecordAccessLog_noLog() {
        DeleteUsingFiltersRequest deleteRequest =
                new DeleteUsingFiltersRequest.Builder()
                        .addRecordType(StepsRecord.class)
                        .addRecordType(HeartRateRecord.class)
                        .setTimeRangeFilter(
                                new TimeInstantRangeFilter.Builder()
                                        .setStartTime(Instant.EPOCH)
                                        .build())
                        .build();
        mFitnessRecordDeleteHelper.deleteRecords(
                TEST_PACKAGE_NAME,
                new DeleteUsingFiltersRequestParcel(deleteRequest),
                /* holdsDataManagementPermission */ false,
                /* shouldRecordAccessLog= */ false);

        List<AccessLog> result = mAccessLogsHelper.queryAccessLogs(mUserHandle);
        assertThat(result).isEmpty();
    }

    @Test
    public void deleteRecordsUnrestricted() {
        RecordInternal<StepsRecord> stepsRecord = createStepsRecord(123456, 654321, 123);
        mTransactionTestUtils.insertRecords(TEST_PACKAGE_NAME, stepsRecord);

        List<RecordInternal<?>> records =
                mTransactionTestUtils.readRecordsByIds(
                        ImmutableMap.of(
                                RecordTypeIdentifier.RECORD_TYPE_STEPS,
                                List.of(stepsRecord.getUuid())));
        assertThat(records).hasSize(1);

        RecordHelper<?> recordHelper =
                mInternalHealthConnectMappings.getRecordHelper(
                        RecordTypeIdentifier.RECORD_TYPE_STEPS);
        DeleteTableRequest deleteTableRequest =
                new DeleteTableRequest(
                                recordHelper.getMainTableName(),
                                RecordTypeIdentifier.RECORD_TYPE_STEPS)
                        .setPackageFilter(RecordHelper.APP_INFO_ID_COLUMN_NAME, List.of())
                        .setRequiresUuId(RecordHelper.UUID_COLUMN_NAME);
        mFitnessRecordDeleteHelper.deleteRecordsUnrestricted(List.of(deleteTableRequest));

        records =
                mTransactionTestUtils.readRecordsByIds(
                        ImmutableMap.of(
                                RecordTypeIdentifier.RECORD_TYPE_STEPS,
                                List.of(stepsRecord.getUuid())));
        assertThat(records).hasSize(0);
    }

    @Test
    @EnableFlags(Flags.FLAG_ADD_MISSING_ACCESS_LOGS)
    public void deleteRecordsUnrestricted_noAccessLogs() {
        RecordHelper<?> recordHelper =
                mInternalHealthConnectMappings.getRecordHelper(
                        RecordTypeIdentifier.RECORD_TYPE_STEPS);
        DeleteTableRequest deleteTableRequest =
                new DeleteTableRequest(
                                recordHelper.getMainTableName(),
                                RecordTypeIdentifier.RECORD_TYPE_STEPS)
                        .setPackageFilter(RecordHelper.APP_INFO_ID_COLUMN_NAME, List.of())
                        .setRequiresUuId(RecordHelper.UUID_COLUMN_NAME);
        mFitnessRecordDeleteHelper.deleteRecordsUnrestricted(List.of(deleteTableRequest));

        List<AccessLog> result = mAccessLogsHelper.queryAccessLogs(mUserHandle);
        assertThat(result).isEmpty();
    }
}
