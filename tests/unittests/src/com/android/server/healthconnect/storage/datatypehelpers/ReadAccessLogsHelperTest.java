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

import static com.android.server.healthconnect.storage.datatypehelpers.ReadAccessLogsHelper.PAGE_SIZE;
import static com.android.server.healthconnect.storage.datatypehelpers.ReadAccessLogsHelper.ReadAccessLog;
import static com.android.server.healthconnect.testing.storage.TransactionTestUtils.createBloodPressureRecord;
import static com.android.server.healthconnect.testing.storage.TransactionTestUtils.createStepsRecord;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;

import android.health.connect.datatypes.BloodPressureRecord;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.health.connect.datatypes.StepsRecord;
import android.health.connect.internal.datatypes.RecordInternal;
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
import com.android.server.healthconnect.testing.storage.TransactionTestUtils;

import com.google.common.collect.ImmutableList;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

@RunWith(AndroidJUnit4.class)
@EnableFlags({
    Flags.FLAG_ECOSYSTEM_METRICS,
    Flags.FLAG_ECOSYSTEM_METRICS_DB_CHANGES,
})
public class ReadAccessLogsHelperTest {

    private static final String TEST_APP_PACKAGE_READER = "test.app.package.reader";
    private static final String TEST_APP_PACKAGE_WRITER = "test.app.package.writer";
    private long mWriterAppInfoId;
    private long mReaderAppInfoId;

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Rule public final TemporaryFolder mEnvironmentDataDir = new TemporaryFolder();
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    private ReadAccessLogsHelper mReadAccessLogsHelper;
    private TransactionManager mTransactionManager;

    @Before
    public void setup() {
        HealthConnectInjector healthConnectInjector =
                HealthConnectInjectorImpl.newBuilderForTest(
                                ApplicationProvider.getApplicationContext())
                        .setFirstGrantTimeManager(mock(FirstGrantTimeManager.class))
                        .setHealthPermissionIntentAppsTracker(
                                mock(HealthPermissionIntentAppsTracker.class))
                        .setEnvironmentDataDirectory(mEnvironmentDataDir.getRoot())
                        .build();
        mReadAccessLogsHelper = healthConnectInjector.getReadAccessLogsHelper();
        AppInfoHelper appInfoHelper = healthConnectInjector.getAppInfoHelper();
        mTransactionManager = healthConnectInjector.getTransactionManager();
        TransactionTestUtils transactionTestUtils = new TransactionTestUtils(healthConnectInjector);

        transactionTestUtils.insertApp(TEST_APP_PACKAGE_READER);
        transactionTestUtils.insertApp(TEST_APP_PACKAGE_WRITER);
        mWriterAppInfoId = appInfoHelper.getAppInfoId(TEST_APP_PACKAGE_WRITER);
        mReaderAppInfoId = appInfoHelper.getAppInfoId(TEST_APP_PACKAGE_READER);
    }

    @Test
    public void insertReadAccessLogs_queryLogsReturnsAllLogs() {
        RecordInternal<StepsRecord> stepsRecordRecordInternal =
                createStepsRecord(mWriterAppInfoId, 123, Instant.now().toEpochMilli(), 100);
        RecordInternal<BloodPressureRecord> bloodPressureRecordRecordInternal =
                createBloodPressureRecord(mWriterAppInfoId, 1234, 120, 80);
        long readTimeStamp = Instant.now().toEpochMilli();
        List<ReadAccessLog> expectedReadAccessLogs =
                List.of(
                        new ReadAccessLog(
                                /* readerPackage= */ TEST_APP_PACKAGE_READER,
                                /* writerPackage= */ TEST_APP_PACKAGE_WRITER,
                                /* dataType= */ RecordTypeIdentifier.RECORD_TYPE_STEPS,
                                /* readTimeStamp= */ readTimeStamp,
                                /* isRecordWithinPast30Days= */ true),
                        new ReadAccessLog(
                                /* readerPackage= */ TEST_APP_PACKAGE_READER,
                                /* writerPackage= */ TEST_APP_PACKAGE_WRITER,
                                /* dataType= */ RecordTypeIdentifier.RECORD_TYPE_BLOOD_PRESSURE,
                                /* readTimeStamp= */ readTimeStamp,
                                /* isRecordWithinPast30Days= */ false));

        mTransactionManager.runAsTransaction(
                db -> {
                    mReadAccessLogsHelper.recordAccessLogForNonAggregationReads(
                            db,
                            TEST_APP_PACKAGE_READER,
                            /* readTimeStamp= */ readTimeStamp,
                            ImmutableList.of(
                                    stepsRecordRecordInternal, bloodPressureRecordRecordInternal));
                });
        List<ReadAccessLog> readAccessLogs =
                mReadAccessLogsHelper.queryReadAccessLogs(0).getReadAccessLogs();

        assertThat(readAccessLogs).containsExactlyElementsIn(expectedReadAccessLogs);
    }

    @Test
    public void insertReadAccessLogsOfSameDataType_insertsOnlyLatestPerDataType() {
        RecordInternal<StepsRecord> stepsRecordRecordInternalOne =
                createStepsRecord(mWriterAppInfoId, 123, 345, 100);
        RecordInternal<StepsRecord> stepsRecordRecordInternalTwo =
                createStepsRecord(mWriterAppInfoId, 123, 350, 100);
        RecordInternal<BloodPressureRecord> bloodPressureRecordRecordInternalOne =
                createBloodPressureRecord(mWriterAppInfoId, 1234, 120, 80);
        RecordInternal<BloodPressureRecord> bloodPressureRecordRecordInternalTwo =
                createBloodPressureRecord(mWriterAppInfoId, Instant.now().toEpochMilli(), 120, 80);
        long readTimeStamp = Instant.now().toEpochMilli();
        List<ReadAccessLog> expectedReadAccessLogs =
                List.of(
                        new ReadAccessLog(
                                /* readerPackage= */ TEST_APP_PACKAGE_READER,
                                /* writerPackage= */ TEST_APP_PACKAGE_WRITER,
                                /* dataType= */ RecordTypeIdentifier.RECORD_TYPE_BLOOD_PRESSURE,
                                /* readTimeStamp= */ readTimeStamp,
                                /* isRecordWithinPast30Days= */ true),
                        new ReadAccessLog(
                                /* readerPackage= */ TEST_APP_PACKAGE_READER,
                                /* writerPackage= */ TEST_APP_PACKAGE_WRITER,
                                /* dataType= */ RecordTypeIdentifier.RECORD_TYPE_STEPS,
                                /* readTimeStamp= */ readTimeStamp,
                                /* isRecordWithinPast30Days= */ false));

        mTransactionManager.runAsTransaction(
                db -> {
                    mReadAccessLogsHelper.recordAccessLogForNonAggregationReads(
                            db,
                            TEST_APP_PACKAGE_READER,
                            /* readTimeStamp= */ readTimeStamp,
                            ImmutableList.of(
                                    stepsRecordRecordInternalOne,
                                    bloodPressureRecordRecordInternalOne,
                                    stepsRecordRecordInternalTwo,
                                    bloodPressureRecordRecordInternalTwo));
                });
        List<ReadAccessLog> readAccessLogs =
                mReadAccessLogsHelper.queryReadAccessLogs(0).getReadAccessLogs();

        assertThat(readAccessLogs).containsExactlyElementsIn(expectedReadAccessLogs);
    }

    @Test
    public void insertReadAccessLogsForAggregation() {
        long endTime = Instant.now().toEpochMilli();
        long readTime = Instant.now().toEpochMilli();
        List<ReadAccessLog> expectedReadAccessLogs =
                List.of(
                        new ReadAccessLog(
                                /* readerPackage= */ TEST_APP_PACKAGE_READER,
                                /* writerPackage= */ TEST_APP_PACKAGE_WRITER,
                                /* dataType= */ RecordTypeIdentifier.RECORD_TYPE_DISTANCE,
                                /* readTimeStamp= */ readTime,
                                /* isRecordWithinPast30Days= */ true));

        mTransactionManager.runAsTransaction(
                db -> {
                    mReadAccessLogsHelper.recordAccessLogForAggregationReads(
                            db,
                            TEST_APP_PACKAGE_READER,
                            /* readTimeStamp= */ readTime,
                            RecordTypeIdentifier.RECORD_TYPE_DISTANCE,
                            /* endTimeStamp= */ endTime,
                            ImmutableList.of(TEST_APP_PACKAGE_WRITER));
                });
        List<ReadAccessLog> readAccessLogs =
                mReadAccessLogsHelper.queryReadAccessLogs(0).getReadAccessLogs();

        assertThat(readAccessLogs).containsExactlyElementsIn(expectedReadAccessLogs);
    }

    @Test
    public void insertReadAccessLogsOfSamePackage_insertsOnlyForDifferentPackage() {
        long endTime = Instant.now().toEpochMilli();
        long readTime = Instant.now().toEpochMilli();
        List<ReadAccessLog> expectedReadAccessLogs =
                List.of(
                        new ReadAccessLog(
                                /* readerPackage= */ TEST_APP_PACKAGE_READER,
                                /* writerPackage= */ TEST_APP_PACKAGE_WRITER,
                                /* dataType= */ RecordTypeIdentifier.RECORD_TYPE_DISTANCE,
                                /* readTimeStamp= */ readTime,
                                /* isRecordWithinPast30Days= */ true));

        mTransactionManager.runAsTransaction(
                db -> {
                    mReadAccessLogsHelper.recordAccessLogForAggregationReads(
                            db,
                            TEST_APP_PACKAGE_READER,
                            /* readTimeStamp= */ readTime,
                            RecordTypeIdentifier.RECORD_TYPE_DISTANCE,
                            /* endTimeStamp= */ endTime,
                            ImmutableList.of(TEST_APP_PACKAGE_WRITER, TEST_APP_PACKAGE_READER));
                });
        List<ReadAccessLog> readAccessLogs =
                mReadAccessLogsHelper.queryReadAccessLogs(0).getReadAccessLogs();

        assertThat(readAccessLogs).containsExactlyElementsIn(expectedReadAccessLogs);
    }

    @Test
    public void testInsertReadAccessLogForDifferentReaderAndWriterPackage() {
        RecordInternal<StepsRecord> stepsRecordRecordInternalOne =
                createStepsRecord(mWriterAppInfoId, 123, Instant.now().toEpochMilli(), 100);
        RecordInternal<BloodPressureRecord> bloodPressureRecordRecordInternalTwo =
                createBloodPressureRecord(mReaderAppInfoId, Instant.now().toEpochMilli(), 120, 80);
        long readTimeStamp = Instant.now().toEpochMilli();
        List<ReadAccessLog> expectedReadAccessLogs =
                List.of(
                        new ReadAccessLog(
                                /* readerPackage= */ TEST_APP_PACKAGE_READER,
                                /* writerPackage= */ TEST_APP_PACKAGE_WRITER,
                                /* dataType= */ RecordTypeIdentifier.RECORD_TYPE_STEPS,
                                /* readTimeStamp= */ readTimeStamp,
                                /* isRecordWithinPast30Days= */ true));

        mTransactionManager.runAsTransaction(
                db -> {
                    mReadAccessLogsHelper.recordAccessLogForNonAggregationReads(
                            db,
                            TEST_APP_PACKAGE_READER,
                            /* readTimeStamp= */ readTimeStamp,
                            ImmutableList.of(
                                    stepsRecordRecordInternalOne,
                                    bloodPressureRecordRecordInternalTwo));
                });
        List<ReadAccessLog> readAccessLogs =
                mReadAccessLogsHelper.queryReadAccessLogs(0).getReadAccessLogs();

        assertThat(readAccessLogs).containsExactlyElementsIn(expectedReadAccessLogs);
    }

    @Test
    public void testReadAccessLogsCountLessThanPageLimit() {
        int count = PAGE_SIZE - 10;
        RecordInternal<StepsRecord> stepsRecordRecordInternalOne =
                createStepsRecord(mWriterAppInfoId, 123, Instant.now().toEpochMilli(), 100);
        long readTimeStamp = Instant.now().toEpochMilli();
        ReadAccessLog expectedReadAccessLog =
                new ReadAccessLog(
                        /* readerPackage= */ TEST_APP_PACKAGE_READER,
                        /* writerPackage= */ TEST_APP_PACKAGE_WRITER,
                        /* dataType= */ RecordTypeIdentifier.RECORD_TYPE_STEPS,
                        /* readTimeStamp= */ readTimeStamp,
                        /* isRecordWithinPast30Days= */ true);

        insertReadAccessLogNTimes(stepsRecordRecordInternalOne, count, readTimeStamp);
        ReadAccessLogsHelper.ReadAccessLogsResponse readAccessLogsResponse =
                mReadAccessLogsHelper.queryReadAccessLogs(0);

        assertThat(readAccessLogsResponse.getReadAccessLogs())
                .containsExactlyElementsIn(Collections.nCopies(count, expectedReadAccessLog));
        assertThat(readAccessLogsResponse.hasMoreRecords()).isFalse();
        assertThat(readAccessLogsResponse.getNextRowId()).isEqualTo(count + 1);
    }

    @Test
    public void testReadAccessLogsCountEqualToPageLimit() {
        RecordInternal<StepsRecord> stepsRecordRecordInternalOne =
                createStepsRecord(mWriterAppInfoId, 123, Instant.now().toEpochMilli(), 100);
        long readTimeStamp = Instant.now().toEpochMilli();
        ReadAccessLog expectedReadAccessLog =
                new ReadAccessLog(
                        /* readerPackage= */ TEST_APP_PACKAGE_READER,
                        /* writerPackage= */ TEST_APP_PACKAGE_WRITER,
                        /* dataType= */ RecordTypeIdentifier.RECORD_TYPE_STEPS,
                        /* readTimeStamp= */ readTimeStamp,
                        /* isRecordWithinPast30Days= */ true);

        insertReadAccessLogNTimes(stepsRecordRecordInternalOne, PAGE_SIZE, readTimeStamp);
        ReadAccessLogsHelper.ReadAccessLogsResponse readAccessLogsResponse =
                mReadAccessLogsHelper.queryReadAccessLogs(0);

        assertThat(readAccessLogsResponse.getReadAccessLogs())
                .containsExactlyElementsIn(Collections.nCopies(PAGE_SIZE, expectedReadAccessLog));
        assertThat(readAccessLogsResponse.hasMoreRecords()).isFalse();
        assertThat(readAccessLogsResponse.getNextRowId()).isEqualTo(PAGE_SIZE + 1);
    }

    @Test
    public void testReadAccessLogsCountGreaterThanPageLimit() {
        int count = PAGE_SIZE + 10;
        RecordInternal<StepsRecord> stepsRecordRecordInternalOne =
                createStepsRecord(mWriterAppInfoId, 123, Instant.now().toEpochMilli(), 100);
        long readTimeStamp = Instant.now().toEpochMilli();
        ReadAccessLog expectedReadAccessLog =
                new ReadAccessLog(
                        /* readerPackage= */ TEST_APP_PACKAGE_READER,
                        /* writerPackage= */ TEST_APP_PACKAGE_WRITER,
                        /* dataType= */ RecordTypeIdentifier.RECORD_TYPE_STEPS,
                        /* readTimeStamp= */ readTimeStamp,
                        /* isRecordWithinPast30Days= */ true);

        insertReadAccessLogNTimes(stepsRecordRecordInternalOne, count, readTimeStamp);
        ReadAccessLogsHelper.ReadAccessLogsResponse readAccessLogsResponseOne =
                mReadAccessLogsHelper.queryReadAccessLogs(0);
        ReadAccessLogsHelper.ReadAccessLogsResponse readAccessLogsResponseTwo =
                mReadAccessLogsHelper.queryReadAccessLogs(readAccessLogsResponseOne.getNextRowId());

        assertThat(readAccessLogsResponseOne.getReadAccessLogs())
                .containsExactlyElementsIn(Collections.nCopies(PAGE_SIZE, expectedReadAccessLog));
        assertThat(readAccessLogsResponseOne.hasMoreRecords()).isTrue();
        assertThat(readAccessLogsResponseOne.getNextRowId()).isEqualTo(PAGE_SIZE + 1);
        assertThat(readAccessLogsResponseTwo.getReadAccessLogs())
                .containsExactlyElementsIn(
                        Collections.nCopies(count - PAGE_SIZE, expectedReadAccessLog));
        assertThat(readAccessLogsResponseTwo.hasMoreRecords()).isFalse();
        assertThat(readAccessLogsResponseTwo.getNextRowId()).isEqualTo(count + 1);
    }

    private void insertReadAccessLogNTimes(
            RecordInternal<?> recordInternal, int count, long readTimeStamp) {
        mTransactionManager.runAsTransaction(
                db -> {
                    for (int i = 0; i < count; i++) {
                        mReadAccessLogsHelper.recordAccessLogForNonAggregationReads(
                                db,
                                TEST_APP_PACKAGE_READER,
                                /* readTimeStamp= */ readTimeStamp,
                                ImmutableList.of(recordInternal));
                    }
                });
    }
}
