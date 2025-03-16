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

package com.android.server.healthconnect.logging;

import static com.android.healthfitness.flags.Flags.FLAG_ECOSYSTEM_METRICS;
import static com.android.healthfitness.flags.Flags.FLAG_ECOSYSTEM_METRICS_DB_CHANGES;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;
import com.android.server.healthconnect.permission.FirstGrantTimeManager;
import com.android.server.healthconnect.storage.datatypehelpers.ChangeLogsHelper;
import com.android.server.healthconnect.storage.datatypehelpers.ReadAccessLogsHelper;
import com.android.server.healthconnect.storage.datatypehelpers.ReadAccessLogsHelper.ReadAccessLog;
import com.android.server.healthconnect.testing.storage.TransactionTestUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
public class EcosystemStatsCollectorTest {

    private static final String TEST_APP_PACKAGE_READER = "test.app.package.reader";
    private static final String TEST_APP_PACKAGE_READER_TWO = "test.app.package.reader.two";
    private static final String TEST_APP_PACKAGE_READER_THREE = "test.app.package.xyz";
    private static final String TEST_APP_PACKAGE_WRITER = "test.app.package.writer";
    private static final String TEST_APP_PACKAGE_WRITER_TWO = "test.app.package.writer.two";
    private static final String TEST_APP_PACKAGE_WRITER_THREE = "test.app.package.writer.three";

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Rule public final TemporaryFolder mTemporaryFolder = new TemporaryFolder();
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock private ReadAccessLogsHelper mReadAccessLogsHelper;
    @Mock private ChangeLogsHelper mChangeLogsHelper;
    private EcosystemStatsCollector mEcosystemStatsCollector;

    @Before
    public void setup() {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectInjector healthConnectInjector =
                HealthConnectInjectorImpl.newBuilderForTest(context)
                        .setReadAccessLogsHelper(mReadAccessLogsHelper)
                        .setChangeLogsHelper(mChangeLogsHelper)
                        .setFirstGrantTimeManager(mock(FirstGrantTimeManager.class))
                        .setEnvironmentDataDirectory(mTemporaryFolder.getRoot())
                        .build();
        mReadAccessLogsHelper = healthConnectInjector.getReadAccessLogsHelper();
        mEcosystemStatsCollector =
                new EcosystemStatsCollector(
                        healthConnectInjector.getReadAccessLogsHelper(),
                        healthConnectInjector.getChangeLogsHelper());
        TransactionTestUtils transactionTestUtils = new TransactionTestUtils(healthConnectInjector);

        transactionTestUtils.insertApp(TEST_APP_PACKAGE_READER);
        transactionTestUtils.insertApp(TEST_APP_PACKAGE_READER_TWO);
        transactionTestUtils.insertApp(TEST_APP_PACKAGE_READER_THREE);
        transactionTestUtils.insertApp(TEST_APP_PACKAGE_WRITER);
        transactionTestUtils.insertApp(TEST_APP_PACKAGE_WRITER_TWO);
        transactionTestUtils.insertApp(TEST_APP_PACKAGE_WRITER_THREE);
    }

    @Test
    @DisableFlags({FLAG_ECOSYSTEM_METRICS, FLAG_ECOSYSTEM_METRICS_DB_CHANGES})
    public void flagsDisabled_doNotQueryAccessLogs() {
        mEcosystemStatsCollector.processReadAccessLogs();

        verify(mReadAccessLogsHelper, times(0)).queryReadAccessLogs(anyInt());
    }

    @Test
    @EnableFlags({
        FLAG_ECOSYSTEM_METRICS,
        FLAG_ECOSYSTEM_METRICS_DB_CHANGES,
    })
    public void testDataTypesReadOrWritten() {
        List<ReadAccessLog> readAccessLogList =
                List.of(
                        new ReadAccessLog(
                                /* readerPackage= */ TEST_APP_PACKAGE_READER,
                                /* writerPackage= */ TEST_APP_PACKAGE_WRITER,
                                /* dataType= */ RecordTypeIdentifier.RECORD_TYPE_STEPS,
                                /* readTimeStamp= */ Instant.now().toEpochMilli(),
                                /* isRecordWithinPast30Days= */ true),
                        new ReadAccessLog(
                                /* readerPackage= */ TEST_APP_PACKAGE_READER,
                                /* writerPackage= */ TEST_APP_PACKAGE_WRITER,
                                /* dataType= */ RecordTypeIdentifier.RECORD_TYPE_BLOOD_PRESSURE,
                                /* readTimeStamp= */ Instant.now().toEpochMilli(),
                                /* isRecordWithinPast30Days= */ true),
                        new ReadAccessLog(
                                /* readerPackage= */ TEST_APP_PACKAGE_READER,
                                /* writerPackage= */ TEST_APP_PACKAGE_WRITER,
                                /* dataType= */ RecordTypeIdentifier.RECORD_TYPE_DISTANCE,
                                /* readTimeStamp= */ Instant.now()
                                        .minus(30, ChronoUnit.DAYS)
                                        .toEpochMilli(),
                                /* isRecordWithinPast30Days= */ true));
        when(mReadAccessLogsHelper.queryReadAccessLogs(/* rowId= */ 0))
                .thenReturn(
                        new ReadAccessLogsHelper.ReadAccessLogsResponse(
                                readAccessLogList,
                                /* nextRowId= */ 4,
                                /* hasMoreRecords= */ false));
        when(mChangeLogsHelper.getRecordTypesWrittenInPast30Days())
                .thenReturn(
                        Set.of(
                                RecordTypeIdentifier.RECORD_TYPE_DISTANCE,
                                RecordTypeIdentifier.RECORD_TYPE_HEART_RATE,
                                RecordTypeIdentifier.RECORD_TYPE_STEPS));

        mEcosystemStatsCollector.processReadAccessLogs();

        assertThat(mEcosystemStatsCollector.getDataTypesReadOrWritten())
                .containsExactly(
                        RecordTypeIdentifier.RECORD_TYPE_STEPS,
                        RecordTypeIdentifier.RECORD_TYPE_HEART_RATE,
                        RecordTypeIdentifier.RECORD_TYPE_DISTANCE,
                        RecordTypeIdentifier.RECORD_TYPE_BLOOD_PRESSURE);
    }

    @Test
    @EnableFlags({
        FLAG_ECOSYSTEM_METRICS,
        FLAG_ECOSYSTEM_METRICS_DB_CHANGES,
    })
    public void testDataTypesWritten() {
        when(mChangeLogsHelper.getRecordTypesWrittenInPast30Days())
                .thenReturn(
                        Set.of(
                                RecordTypeIdentifier.RECORD_TYPE_DISTANCE,
                                RecordTypeIdentifier.RECORD_TYPE_HEART_RATE,
                                RecordTypeIdentifier.RECORD_TYPE_BLOOD_PRESSURE));
        when(mReadAccessLogsHelper.queryReadAccessLogs(/* rowId= */ 0))
                .thenReturn(
                        new ReadAccessLogsHelper.ReadAccessLogsResponse(
                                Collections.emptyList(),
                                /* nextRowId= */ 1,
                                /* hasMoreRecords= */ false));

        mEcosystemStatsCollector.processReadAccessLogs();

        assertThat(mEcosystemStatsCollector.getDataTypesWritten())
                .containsExactly(
                        RecordTypeIdentifier.RECORD_TYPE_HEART_RATE,
                        RecordTypeIdentifier.RECORD_TYPE_DISTANCE,
                        RecordTypeIdentifier.RECORD_TYPE_BLOOD_PRESSURE);
    }

    @Test
    @EnableFlags({
        FLAG_ECOSYSTEM_METRICS,
        FLAG_ECOSYSTEM_METRICS_DB_CHANGES,
    })
    public void testDataTypesRead_onlyReturnsDataTypesReadWithin30Days() {
        List<ReadAccessLog> readAccessLogList =
                List.of(
                        new ReadAccessLog(
                                /* readerPackage= */ TEST_APP_PACKAGE_READER,
                                /* writerPackage= */ TEST_APP_PACKAGE_WRITER,
                                /* dataType= */ RecordTypeIdentifier.RECORD_TYPE_STEPS,
                                /* readTimeStamp= */ Instant.now().toEpochMilli(),
                                /* isRecordWithinPast30Days= */ true),
                        new ReadAccessLog(
                                /* readerPackage= */ TEST_APP_PACKAGE_READER,
                                /* writerPackage= */ TEST_APP_PACKAGE_WRITER,
                                /* dataType= */ RecordTypeIdentifier.RECORD_TYPE_BLOOD_PRESSURE,
                                /* readTimeStamp= */ Instant.now().toEpochMilli(),
                                /* isRecordWithinPast30Days= */ true),
                        new ReadAccessLog(
                                /* readerPackage= */ TEST_APP_PACKAGE_READER,
                                /* writerPackage= */ TEST_APP_PACKAGE_WRITER,
                                /* dataType= */ RecordTypeIdentifier.RECORD_TYPE_DISTANCE,
                                /* readTimeStamp= */ Instant.now()
                                        .minus(30, ChronoUnit.DAYS)
                                        .toEpochMilli(),
                                /* isRecordWithinPast30Days= */ true));
        when(mReadAccessLogsHelper.queryReadAccessLogs(/* rowId= */ 0))
                .thenReturn(
                        new ReadAccessLogsHelper.ReadAccessLogsResponse(
                                readAccessLogList,
                                /* nextRowId= */ 4,
                                /* hasMoreRecords= */ false));

        mEcosystemStatsCollector.processReadAccessLogs();

        assertThat(mEcosystemStatsCollector.getDataTypesRead())
                .containsExactly(
                        RecordTypeIdentifier.RECORD_TYPE_STEPS,
                        RecordTypeIdentifier.RECORD_TYPE_BLOOD_PRESSURE);
    }

    @Test
    @EnableFlags({
        FLAG_ECOSYSTEM_METRICS,
        FLAG_ECOSYSTEM_METRICS_DB_CHANGES,
    })
    public void testDataTypesShared_onlyReturnsDataTypesReadAndWrittenWithin30Days() {
        List<ReadAccessLog> readAccessLogList =
                List.of(
                        new ReadAccessLog(
                                /* readerPackage= */ TEST_APP_PACKAGE_READER,
                                /* writerPackage= */ TEST_APP_PACKAGE_WRITER,
                                /* dataType= */ RecordTypeIdentifier.RECORD_TYPE_STEPS,
                                /* readTimeStamp= */ Instant.now().toEpochMilli(),
                                /* isRecordWithinPast30Days= */ true),
                        new ReadAccessLog(
                                /* readerPackage= */ TEST_APP_PACKAGE_READER,
                                /* writerPackage= */ TEST_APP_PACKAGE_WRITER,
                                /* dataType= */ RecordTypeIdentifier.RECORD_TYPE_BLOOD_PRESSURE,
                                Instant.now().minus(31, ChronoUnit.DAYS).toEpochMilli(),
                                /* isRecordWithinPast30Days= */ true),
                        new ReadAccessLog(
                                /* readerPackage= */ TEST_APP_PACKAGE_READER,
                                /* writerPackage= */ TEST_APP_PACKAGE_WRITER,
                                /* dataType= */ RecordTypeIdentifier.RECORD_TYPE_DISTANCE,
                                /* readTimeStamp= */ Instant.now()
                                        .minus(20, ChronoUnit.DAYS)
                                        .toEpochMilli(),
                                /* isRecordWithinPast30Days= */ true));
        when(mReadAccessLogsHelper.queryReadAccessLogs(/* rowId= */ 0))
                .thenReturn(
                        new ReadAccessLogsHelper.ReadAccessLogsResponse(
                                readAccessLogList,
                                /* nextRowId= */ 4,
                                /* hasMoreRecords= */ false));

        mEcosystemStatsCollector.processReadAccessLogs();

        assertThat(mEcosystemStatsCollector.getDataTypeShared())
                .containsExactly(
                        RecordTypeIdentifier.RECORD_TYPE_STEPS,
                        RecordTypeIdentifier.RECORD_TYPE_DISTANCE);
    }

    @Test
    @EnableFlags({
        FLAG_ECOSYSTEM_METRICS,
        FLAG_ECOSYSTEM_METRICS_DB_CHANGES,
    })
    public void testGetDirectionalAppPairings_doNotCountIfDataNotWrittenWithin30Days() {
        List<ReadAccessLog> readAccessLogList =
                List.of(
                        new ReadAccessLog(
                                /* readerPackage= */ TEST_APP_PACKAGE_READER,
                                /* writerPackage= */ TEST_APP_PACKAGE_WRITER,
                                /* dataType= */ RecordTypeIdentifier.RECORD_TYPE_STEPS,
                                /* readTimeStamp= */ Instant.now().toEpochMilli(),
                                /* isRecordWithinPast30Days= */ true),
                        new ReadAccessLog(
                                /* readerPackage= */ TEST_APP_PACKAGE_READER,
                                /* writerPackage= */ TEST_APP_PACKAGE_WRITER_TWO,
                                /* dataType= */ RecordTypeIdentifier.RECORD_TYPE_BLOOD_PRESSURE,
                                Instant.now().minus(10, ChronoUnit.DAYS).toEpochMilli(),
                                /* isRecordWithinPast30Days= */ false),
                        new ReadAccessLog(
                                /* readerPackage= */ TEST_APP_PACKAGE_READER,
                                /* writerPackage= */ TEST_APP_PACKAGE_WRITER_THREE,
                                /* dataType= */ RecordTypeIdentifier.RECORD_TYPE_HEART_RATE,
                                /* readTimeStamp= */ Instant.now().toEpochMilli(),
                                /* isRecordWithinPast30Days= */ true),
                        new ReadAccessLog(
                                /* readerPackage= */ TEST_APP_PACKAGE_READER_TWO,
                                /* writerPackage= */ TEST_APP_PACKAGE_WRITER_THREE,
                                /* dataType= */ RecordTypeIdentifier.RECORD_TYPE_HEART_RATE,
                                /* readTimeStamp= */ Instant.now().toEpochMilli(),
                                /* isRecordWithinPast30Days= */ true),
                        new ReadAccessLog(
                                /* readerPackage= */ TEST_APP_PACKAGE_READER_TWO,
                                /* writerPackage= */ TEST_APP_PACKAGE_WRITER_TWO,
                                /* dataType= */ RecordTypeIdentifier.RECORD_TYPE_BLOOD_PRESSURE,
                                Instant.now().minus(10, ChronoUnit.DAYS).toEpochMilli(),
                                /* isRecordWithinPast30Days= */ true),
                        new ReadAccessLog(
                                /* readerPackage= */ TEST_APP_PACKAGE_READER_TWO,
                                /* writerPackage= */ TEST_APP_PACKAGE_WRITER,
                                /* dataType= */ RecordTypeIdentifier.RECORD_TYPE_BLOOD_GLUCOSE,
                                Instant.now().minus(10, ChronoUnit.DAYS).toEpochMilli(),
                                /* isRecordWithinPast30Days= */ false));
        when(mReadAccessLogsHelper.queryReadAccessLogs(/* rowId= */ 0))
                .thenReturn(
                        new ReadAccessLogsHelper.ReadAccessLogsResponse(
                                readAccessLogList,
                                /* nextRowId= */ 4,
                                /* hasMoreRecords= */ false));

        mEcosystemStatsCollector.processReadAccessLogs();

        Map<String, Set<String>> expectedPairings =
                Map.of(
                        TEST_APP_PACKAGE_READER,
                        Set.of(TEST_APP_PACKAGE_WRITER, TEST_APP_PACKAGE_WRITER_THREE),
                        TEST_APP_PACKAGE_READER_TWO,
                        Set.of(TEST_APP_PACKAGE_WRITER_TWO, TEST_APP_PACKAGE_WRITER_THREE));
        assertThat(mEcosystemStatsCollector.getDirectionalAppPairings())
                .isEqualTo(expectedPairings);
    }

    @Test
    @EnableFlags({
        FLAG_ECOSYSTEM_METRICS,
        FLAG_ECOSYSTEM_METRICS_DB_CHANGES,
    })
    public void testGetDirectionalAppPairingsPerDataType_doNotCountIfDataNotWrittenWithin30Days() {
        List<ReadAccessLog> readAccessLogList =
                List.of(
                        new ReadAccessLog(
                                /* readerPackage= */ TEST_APP_PACKAGE_READER,
                                /* writerPackage= */ TEST_APP_PACKAGE_WRITER,
                                /* dataType= */ RecordTypeIdentifier.RECORD_TYPE_STEPS,
                                /* readTimeStamp= */ Instant.now().toEpochMilli(),
                                /* isRecordWithinPast30Days= */ true),
                        new ReadAccessLog(
                                /* readerPackage= */ TEST_APP_PACKAGE_READER,
                                /* writerPackage= */ TEST_APP_PACKAGE_WRITER_THREE,
                                /* dataType= */ RecordTypeIdentifier.RECORD_TYPE_STEPS,
                                /* readTimeStamp= */ Instant.now().toEpochMilli(),
                                /* isRecordWithinPast30Days= */ true),
                        new ReadAccessLog(
                                /* readerPackage= */ TEST_APP_PACKAGE_READER,
                                /* writerPackage= */ TEST_APP_PACKAGE_WRITER_TWO,
                                /* dataType= */ RecordTypeIdentifier.RECORD_TYPE_BLOOD_PRESSURE,
                                Instant.now().minus(10, ChronoUnit.DAYS).toEpochMilli(),
                                /* isRecordWithinPast30Days= */ false),
                        new ReadAccessLog(
                                /* readerPackage= */ TEST_APP_PACKAGE_READER,
                                /* writerPackage= */ TEST_APP_PACKAGE_WRITER_THREE,
                                /* dataType= */ RecordTypeIdentifier.RECORD_TYPE_HEART_RATE,
                                /* readTimeStamp= */ Instant.now().toEpochMilli(),
                                /* isRecordWithinPast30Days= */ true),
                        new ReadAccessLog(
                                /* readerPackage= */ TEST_APP_PACKAGE_READER_TWO,
                                /* writerPackage= */ TEST_APP_PACKAGE_WRITER_THREE,
                                /* dataType= */ RecordTypeIdentifier.RECORD_TYPE_HEART_RATE,
                                /* readTimeStamp= */ Instant.now().toEpochMilli(),
                                /* isRecordWithinPast30Days= */ true),
                        new ReadAccessLog(
                                /* readerPackage= */ TEST_APP_PACKAGE_READER_TWO,
                                /* writerPackage= */ TEST_APP_PACKAGE_WRITER_TWO,
                                /* dataType= */ RecordTypeIdentifier.RECORD_TYPE_BLOOD_PRESSURE,
                                Instant.now().minus(10, ChronoUnit.DAYS).toEpochMilli(),
                                /* isRecordWithinPast30Days= */ true),
                        new ReadAccessLog(
                                /* readerPackage= */ TEST_APP_PACKAGE_READER_TWO,
                                /* writerPackage= */ TEST_APP_PACKAGE_WRITER,
                                /* dataType= */ RecordTypeIdentifier.RECORD_TYPE_BLOOD_GLUCOSE,
                                Instant.now().minus(10, ChronoUnit.DAYS).toEpochMilli(),
                                /* isRecordWithinPast30Days= */ false));
        when(mReadAccessLogsHelper.queryReadAccessLogs(/* rowId= */ 0))
                .thenReturn(
                        new ReadAccessLogsHelper.ReadAccessLogsResponse(
                                readAccessLogList,
                                /* nextRowId= */ 4,
                                /* hasMoreRecords= */ false));

        mEcosystemStatsCollector.processReadAccessLogs();

        Map<String, Map<Integer, Set<String>>> expectedPairings =
                Map.of(
                        TEST_APP_PACKAGE_READER,
                        Map.of(
                                RecordTypeIdentifier.RECORD_TYPE_STEPS,
                                Set.of(TEST_APP_PACKAGE_WRITER, TEST_APP_PACKAGE_WRITER_THREE),
                                RecordTypeIdentifier.RECORD_TYPE_HEART_RATE,
                                Set.of(TEST_APP_PACKAGE_WRITER_THREE)),
                        TEST_APP_PACKAGE_READER_TWO,
                        Map.of(
                                RecordTypeIdentifier.RECORD_TYPE_HEART_RATE,
                                Set.of(TEST_APP_PACKAGE_WRITER_THREE),
                                RecordTypeIdentifier.RECORD_TYPE_BLOOD_PRESSURE,
                                Set.of(TEST_APP_PACKAGE_WRITER_TWO)));
        assertThat(mEcosystemStatsCollector.getDirectionalAppPairingsPerDataType())
                .isEqualTo(expectedPairings);
    }

    @Test
    @EnableFlags({
        FLAG_ECOSYSTEM_METRICS,
        FLAG_ECOSYSTEM_METRICS_DB_CHANGES,
    })
    public void testGetNumberOfAppPairings_doNotCountPairingIfDataNotWrittenWithin30Days() {
        List<ReadAccessLog> readAccessLogList =
                List.of(
                        new ReadAccessLog(
                                /* readerPackage= */ TEST_APP_PACKAGE_READER,
                                /* writerPackage= */ TEST_APP_PACKAGE_WRITER,
                                /* dataType= */ RecordTypeIdentifier.RECORD_TYPE_STEPS,
                                /* readTimeStamp= */ Instant.now().toEpochMilli(),
                                /* isRecordWithinPast30Days= */ true),
                        new ReadAccessLog(
                                /* readerPackage= */ TEST_APP_PACKAGE_READER,
                                /* writerPackage= */ TEST_APP_PACKAGE_WRITER,
                                /* dataType= */ RecordTypeIdentifier.RECORD_TYPE_DISTANCE,
                                /* readTimeStamp= */ Instant.now().toEpochMilli(),
                                /* isRecordWithinPast30Days= */ true),
                        new ReadAccessLog(
                                /* readerPackage= */ TEST_APP_PACKAGE_READER,
                                /* writerPackage= */ TEST_APP_PACKAGE_WRITER_THREE,
                                /* dataType= */ RecordTypeIdentifier.RECORD_TYPE_STEPS,
                                /* readTimeStamp= */ Instant.now().toEpochMilli(),
                                /* isRecordWithinPast30Days= */ true),
                        new ReadAccessLog(
                                /* readerPackage= */ TEST_APP_PACKAGE_READER,
                                /* writerPackage= */ TEST_APP_PACKAGE_WRITER_TWO,
                                /* dataType= */ RecordTypeIdentifier.RECORD_TYPE_BLOOD_PRESSURE,
                                Instant.now().minus(10, ChronoUnit.DAYS).toEpochMilli(),
                                /* isRecordWithinPast30Days= */ false),
                        new ReadAccessLog(
                                /* readerPackage= */ TEST_APP_PACKAGE_READER,
                                /* writerPackage= */ TEST_APP_PACKAGE_WRITER_THREE,
                                /* dataType= */ RecordTypeIdentifier.RECORD_TYPE_HEART_RATE,
                                /* readTimeStamp= */ Instant.now().toEpochMilli(),
                                /* isRecordWithinPast30Days= */ true),
                        new ReadAccessLog(
                                /* readerPackage= */ TEST_APP_PACKAGE_READER_TWO,
                                /* writerPackage= */ TEST_APP_PACKAGE_WRITER_THREE,
                                /* dataType= */ RecordTypeIdentifier.RECORD_TYPE_HEART_RATE,
                                /* readTimeStamp= */ Instant.now().toEpochMilli(),
                                /* isRecordWithinPast30Days= */ true),
                        new ReadAccessLog(
                                /* readerPackage= */ TEST_APP_PACKAGE_READER_TWO,
                                /* writerPackage= */ TEST_APP_PACKAGE_WRITER_TWO,
                                /* dataType= */ RecordTypeIdentifier.RECORD_TYPE_BLOOD_PRESSURE,
                                Instant.now().minus(10, ChronoUnit.DAYS).toEpochMilli(),
                                /* isRecordWithinPast30Days= */ true),
                        new ReadAccessLog(
                                /* readerPackage= */ TEST_APP_PACKAGE_READER_TWO,
                                /* writerPackage= */ TEST_APP_PACKAGE_WRITER,
                                /* dataType= */ RecordTypeIdentifier.RECORD_TYPE_BLOOD_GLUCOSE,
                                Instant.now().minus(10, ChronoUnit.DAYS).toEpochMilli(),
                                /* isRecordWithinPast30Days= */ false),
                        new ReadAccessLog(
                                /* readerPackage= */ TEST_APP_PACKAGE_READER_THREE,
                                /* writerPackage= */ TEST_APP_PACKAGE_WRITER_TWO,
                                /* dataType= */ RecordTypeIdentifier.RECORD_TYPE_BLOOD_PRESSURE,
                                Instant.now().minus(10, ChronoUnit.DAYS).toEpochMilli(),
                                /* isRecordWithinPast30Days= */ true));
        when(mReadAccessLogsHelper.queryReadAccessLogs(/* rowId= */ 0))
                .thenReturn(
                        new ReadAccessLogsHelper.ReadAccessLogsResponse(
                                readAccessLogList,
                                /* nextRowId= */ 4,
                                /* hasMoreRecords= */ false));

        mEcosystemStatsCollector.processReadAccessLogs();

        assertThat(mEcosystemStatsCollector.getNumberOfAppPairings()).isEqualTo(5);
    }

    @Test
    @EnableFlags({
        FLAG_ECOSYSTEM_METRICS,
        FLAG_ECOSYSTEM_METRICS_DB_CHANGES,
    })
    public void testEcosystemMetrics_moreThanOnePageOfReadAccessLogs() {
        ReadAccessLog readAccessLogOne =
                new ReadAccessLog(
                        /* readerPackage= */ TEST_APP_PACKAGE_READER,
                        /* writerPackage= */ TEST_APP_PACKAGE_WRITER,
                        /* dataType= */ RecordTypeIdentifier.RECORD_TYPE_STEPS,
                        /* readTimeStamp= */ Instant.now().toEpochMilli(),
                        /* isRecordWithinPast30Days= */ true);
        ReadAccessLog readAccessLogTwo =
                new ReadAccessLog(
                        /* readerPackage= */ TEST_APP_PACKAGE_READER,
                        /* writerPackage= */ TEST_APP_PACKAGE_WRITER_TWO,
                        /* dataType= */ RecordTypeIdentifier.RECORD_TYPE_DISTANCE,
                        /* readTimeStamp= */ Instant.now().toEpochMilli(),
                        /* isRecordWithinPast30Days= */ true);
        when(mReadAccessLogsHelper.queryReadAccessLogs(/* rowId= */ 0))
                .thenReturn(
                        new ReadAccessLogsHelper.ReadAccessLogsResponse(
                                List.of(readAccessLogOne),
                                /* nextRowId= */ 2,
                                /* hasMoreRecords= */ true));
        when(mReadAccessLogsHelper.queryReadAccessLogs(/* rowId= */ 2))
                .thenReturn(
                        new ReadAccessLogsHelper.ReadAccessLogsResponse(
                                List.of(readAccessLogTwo),
                                /* nextRowId= */ 3,
                                /* hasMoreRecords= */ false));

        mEcosystemStatsCollector.processReadAccessLogs();

        assertThat(mEcosystemStatsCollector.getNumberOfAppPairings()).isEqualTo(2);
    }
}
