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

package com.android.server.healthconnect.storage.request;

import static android.health.connect.HealthDataCategory.ACTIVITY;
import static android.health.connect.accesslog.AccessLog.OperationType.OPERATION_TYPE_READ;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_STEPS;

import static com.android.server.healthconnect.testing.storage.TransactionTestUtils.createStepsRecord;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.health.connect.AggregateRecordsRequest;
import android.health.connect.LocalTimeRangeFilter;
import android.health.connect.TimeRangeFilter;
import android.health.connect.TimeRangeFilterHelper;
import android.health.connect.accesslog.AccessLog;
import android.health.connect.aidl.AggregateDataRequestParcel;
import android.health.connect.datatypes.HeartRateRecord;
import android.health.connect.datatypes.StepsRecord;
import android.os.UserHandle;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

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
import com.android.server.healthconnect.storage.datatypehelpers.HealthDataCategoryPriorityHelper;
import com.android.server.healthconnect.storage.datatypehelpers.ReadAccessLogsHelper;
import com.android.server.healthconnect.storage.utils.InternalHealthConnectMappings;
import com.android.server.healthconnect.testing.storage.TransactionTestUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.time.Instant;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class AggregateTransactionRequestTest {

    private static final String TEST_PACKAGE_NAME = "package.name";

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Rule public final TemporaryFolder mEnvironmentDataDir = new TemporaryFolder();
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    private TransactionManager mTransactionManager;
    private AppInfoHelper mAppInfoHelper;
    private HealthDataCategoryPriorityHelper mHealthDataCategoryPriorityHelper;
    private AccessLogsHelper mAccessLogsHelper;
    private ReadAccessLogsHelper mReadAccessLogsHelper;
    private InternalHealthConnectMappings mInternalHealthConnectMappings;
    private TransactionTestUtils mTransactionTestUtils;
    private UserHandle mUserHandle;

    @Before
    public void setup() {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectInjector healthConnectInjector =
                HealthConnectInjectorImpl.newBuilderForTest(context)
                        .setFirstGrantTimeManager(mock(FirstGrantTimeManager.class))
                        .setHealthPermissionIntentAppsTracker(
                                mock(HealthPermissionIntentAppsTracker.class))
                        .setAppOpLogsHelper(mock(AppOpLogsHelper.class))
                        .setEnvironmentDataDirectory(mEnvironmentDataDir.getRoot())
                        .build();

        mTransactionManager = healthConnectInjector.getTransactionManager();
        mAppInfoHelper = healthConnectInjector.getAppInfoHelper();
        mHealthDataCategoryPriorityHelper =
                healthConnectInjector.getHealthDataCategoryPriorityHelper();
        mAccessLogsHelper = healthConnectInjector.getAccessLogsHelper();
        mReadAccessLogsHelper = spy(healthConnectInjector.getReadAccessLogsHelper());
        mInternalHealthConnectMappings = healthConnectInjector.getInternalHealthConnectMappings();
        mUserHandle = context.getUser();

        mTransactionTestUtils = new TransactionTestUtils(healthConnectInjector);
        mTransactionTestUtils.insertApp(TEST_PACKAGE_NAME);
    }

    @Test
    @EnableFlags(Flags.FLAG_ADD_MISSING_ACCESS_LOGS)
    public void populateWithAggregation_accessLogRecorded() {
        TimeRangeFilter timeRangeFilter =
                new LocalTimeRangeFilter.Builder()
                        .setStartTime(TimeRangeFilterHelper.getLocalTimeFromMillis(123L))
                        .setEndTime(TimeRangeFilterHelper.getLocalTimeFromMillis(456L))
                        .build();
        AggregateRecordsRequest<Long> aggregateRecordsRequest =
                new AggregateRecordsRequest.Builder<Long>(timeRangeFilter)
                        .addAggregationType(HeartRateRecord.BPM_AVG)
                        .build();
        AggregateTransactionRequest aggregateTransactionRequest =
                new AggregateTransactionRequest(
                        TEST_PACKAGE_NAME,
                        new AggregateDataRequestParcel(aggregateRecordsRequest),
                        mTransactionManager,
                        mAppInfoHelper,
                        mHealthDataCategoryPriorityHelper,
                        mAccessLogsHelper,
                        mReadAccessLogsHelper,
                        mInternalHealthConnectMappings,
                        /* startDateAccess= */ 0,
                        /* shouldRecordAccessLog= */ true);
        aggregateTransactionRequest.getAggregateDataResponseParcel();

        List<AccessLog> result = mAccessLogsHelper.queryAccessLogs(mUserHandle);
        AccessLog log = result.get(0);
        assertThat(log.getPackageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(log.getRecordTypes()).containsExactly(HeartRateRecord.class);
        assertThat(log.getOperationType()).isEqualTo(OPERATION_TYPE_READ);
    }

    @Test
    @EnableFlags({
        Flags.FLAG_ECOSYSTEM_METRICS,
        Flags.FLAG_ECOSYSTEM_METRICS_DB_CHANGES,
        Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE,
        Flags.FLAG_ACTIVITY_INTENSITY_DB
    })
    public void populateWithAggregation_flagsEnabled_readAccessLogRecorded() {
        Instant testStartTime = Instant.now();

        String readerPackage = "reader.package";
        mTransactionTestUtils.insertApp(readerPackage);
        mTransactionTestUtils.insertRecords(
                TEST_PACKAGE_NAME,
                createStepsRecord(
                        mAppInfoHelper.getAppInfoId(TEST_PACKAGE_NAME),
                        testStartTime.minusMillis(1000).toEpochMilli(),
                        testStartTime.minusMillis(500).toEpochMilli(),
                        100));

        TimeRangeFilter timeRangeFilter =
                new LocalTimeRangeFilter.Builder()
                        .setStartTime(
                                TimeRangeFilterHelper.getLocalTimeFromMillis(
                                        testStartTime.minusMillis(1500).toEpochMilli()))
                        .build();
        mHealthDataCategoryPriorityHelper.appendToPriorityList(TEST_PACKAGE_NAME, ACTIVITY, false);
        AggregateRecordsRequest<Long> aggregateRecordsRequest =
                new AggregateRecordsRequest.Builder<Long>(timeRangeFilter)
                        .addAggregationType(StepsRecord.STEPS_COUNT_TOTAL)
                        .build();
        AggregateTransactionRequest aggregateTransactionRequest =
                new AggregateTransactionRequest(
                        readerPackage,
                        new AggregateDataRequestParcel(aggregateRecordsRequest),
                        mTransactionManager,
                        mAppInfoHelper,
                        mHealthDataCategoryPriorityHelper,
                        mAccessLogsHelper,
                        mReadAccessLogsHelper,
                        mInternalHealthConnectMappings,
                        /* startDateAccess= */ 0,
                        /* shouldRecordAccessLog= */ true);
        aggregateTransactionRequest.getAggregateDataResponseParcel();

        List<ReadAccessLogsHelper.ReadAccessLog> result =
                mReadAccessLogsHelper.queryReadAccessLogs(0).getReadAccessLogs();
        ReadAccessLogsHelper.ReadAccessLog log = result.get(0);
        assertThat(log.getWriterPackage()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(log.getReaderPackage()).isEqualTo(readerPackage);
        assertThat(log.getRecordWithinPast30Days()).isEqualTo(true);
        assertThat(log.getDataType()).isEqualTo(RECORD_TYPE_STEPS);
        assertThat(log.getReadTimeStamp()).isAtLeast(testStartTime.toEpochMilli());
        assertThat(log.getReadTimeStamp()).isAtMost(Instant.now().toEpochMilli());
    }

    @Test
    @EnableFlags({
        Flags.FLAG_ECOSYSTEM_METRICS,
        Flags.FLAG_ECOSYSTEM_METRICS_DB_CHANGES,
        Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE,
        Flags.FLAG_ACTIVITY_INTENSITY_DB
    })
    public void populateWithAggregation_accessLogDisabled_readAccessLogNotRecorded() {
        String readerPackage = "reader.package";
        mTransactionTestUtils.insertApp(readerPackage);
        mTransactionTestUtils.insertRecords(
                TEST_PACKAGE_NAME,
                createStepsRecord(mAppInfoHelper.getAppInfoId(TEST_PACKAGE_NAME), 123, 345, 100));

        TimeRangeFilter timeRangeFilter =
                new LocalTimeRangeFilter.Builder()
                        .setStartTime(TimeRangeFilterHelper.getLocalTimeFromMillis(123L))
                        .setEndTime(TimeRangeFilterHelper.getLocalTimeFromMillis(456L))
                        .build();
        AggregateRecordsRequest<Long> aggregateRecordsRequest =
                new AggregateRecordsRequest.Builder<Long>(timeRangeFilter)
                        .addAggregationType(StepsRecord.STEPS_COUNT_TOTAL)
                        .build();
        AggregateTransactionRequest aggregateTransactionRequest =
                new AggregateTransactionRequest(
                        readerPackage,
                        new AggregateDataRequestParcel(aggregateRecordsRequest),
                        mTransactionManager,
                        mAppInfoHelper,
                        mHealthDataCategoryPriorityHelper,
                        mAccessLogsHelper,
                        mReadAccessLogsHelper,
                        mInternalHealthConnectMappings,
                        /* startDateAccess= */ 0,
                        /* shouldRecordAccessLog= */ false);
        aggregateTransactionRequest.getAggregateDataResponseParcel();

        verify(mReadAccessLogsHelper, times(0))
                .recordAccessLogForNonAggregationReads(any(), any(), anyLong(), any());
        verify(mReadAccessLogsHelper, times(0))
                .recordAccessLogForAggregationReads(
                        any(), any(), anyLong(), anyInt(), anyLong(), any());
    }

    @Test
    @DisableFlags({Flags.FLAG_ECOSYSTEM_METRICS, Flags.FLAG_ECOSYSTEM_METRICS_DB_CHANGES})
    public void populateWithAggregation_flagsDisabled_readAccessLogNotRecorded() {
        String readerPackage = "reader.package";
        mTransactionTestUtils.insertApp(readerPackage);
        mTransactionTestUtils.insertRecords(
                TEST_PACKAGE_NAME,
                createStepsRecord(mAppInfoHelper.getAppInfoId(TEST_PACKAGE_NAME), 123, 345, 100));

        TimeRangeFilter timeRangeFilter =
                new LocalTimeRangeFilter.Builder()
                        .setStartTime(TimeRangeFilterHelper.getLocalTimeFromMillis(123L))
                        .setEndTime(TimeRangeFilterHelper.getLocalTimeFromMillis(456L))
                        .build();
        AggregateRecordsRequest<Long> aggregateRecordsRequest =
                new AggregateRecordsRequest.Builder<Long>(timeRangeFilter)
                        .addAggregationType(StepsRecord.STEPS_COUNT_TOTAL)
                        .build();
        AggregateTransactionRequest aggregateTransactionRequest =
                new AggregateTransactionRequest(
                        readerPackage,
                        new AggregateDataRequestParcel(aggregateRecordsRequest),
                        mTransactionManager,
                        mAppInfoHelper,
                        mHealthDataCategoryPriorityHelper,
                        mAccessLogsHelper,
                        mReadAccessLogsHelper,
                        mInternalHealthConnectMappings,
                        /* startDateAccess= */ 0,
                        /* shouldRecordAccessLog= */ true);
        aggregateTransactionRequest.getAggregateDataResponseParcel();

        verify(mReadAccessLogsHelper, times(0))
                .recordAccessLogForNonAggregationReads(any(), any(), anyLong(), any());
        verify(mReadAccessLogsHelper, times(0))
                .recordAccessLogForAggregationReads(
                        any(), any(), anyLong(), anyInt(), anyLong(), any());
    }
}
