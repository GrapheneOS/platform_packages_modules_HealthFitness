/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.server.healthconnect.common.logging;

import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_ECOSYSTEM_STATS;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_RESTRICTED_ECOSYSTEM_STATS;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_RESTRICTED_ECOSYSTEM_STATS__DATA_TYPE__DATA_TYPE_UNKNOWN;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_RESTRICTED_ECOSYSTEM_STATS__METRIC_TYPE__METRIC_TYPE_DIRECTIONAL_PAIRING;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_RESTRICTED_ECOSYSTEM_STATS__METRIC_TYPE__METRIC_TYPE_DIRECTIONAL_PAIRING_PER_DATA_TYPE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_HEART_RATE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_STEPS;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import android.health.HealthFitnessStatsLog;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.server.healthconnect.fitness.mappings.InternalHealthConnectMappings;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
public class EcosystemStatsLoggerTest {

    private static final String TEST_PACKAGE_READER = "test.package.reader";
    private static final String TEST_PACKAGE_WRITER = "test.package.writer";
    private static final String TEST_SPN_PACKAGE_CANONICAL =
            "com.android.healthconnect.phone.d59341472a9253c16b986840a324ec594";
    private static final String TEST_SPN_PACKAGE = "com.android.healthconnect.phone";

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock private HealthFitnessStatsLog mHealthFitnessStatsLog;
    @Mock private EcosystemStatsCollector mEcosystemStatsCollector;

    private EcosystemStatsLogger mEcosystemStatsLogger;

    @Before
    public void setUp() throws Exception {
        openMocks(this).close();
        mEcosystemStatsLogger = new EcosystemStatsLogger(mHealthFitnessStatsLog);

        // Default empty responses from collector
        when(mEcosystemStatsCollector.getDataTypesReadOrWritten())
                .thenReturn(Collections.emptySet());
        when(mEcosystemStatsCollector.getDataTypesRead()).thenReturn(Collections.emptySet());
        when(mEcosystemStatsCollector.getDataTypesWritten()).thenReturn(Collections.emptySet());
        when(mEcosystemStatsCollector.getDataTypeShared()).thenReturn(Collections.emptySet());
        when(mEcosystemStatsCollector.getDirectionalAppPairings())
                .thenReturn(Collections.emptyMap());
        when(mEcosystemStatsCollector.getDirectionalAppPairingsPerDataType())
                .thenReturn(Collections.emptyMap());
    }

    @Test
    public void log_callsProcessReadAccessLogs() {
        mEcosystemStatsLogger.log(mEcosystemStatsCollector);
        verify(mEcosystemStatsCollector).processReadAccessLogs();
    }

    @Test
    public void logsEcosystemStats() {
        Set<Integer> readOrWritten = new LinkedHashSet<>();
        readOrWritten.add(RECORD_TYPE_STEPS);
        readOrWritten.add(RECORD_TYPE_HEART_RATE);

        Set<Integer> read = new LinkedHashSet<>();
        read.add(RECORD_TYPE_STEPS);

        Set<Integer> written = new LinkedHashSet<>();
        written.add(RECORD_TYPE_HEART_RATE);

        Set<Integer> shared = new LinkedHashSet<>();
        shared.add(RECORD_TYPE_STEPS);

        when(mEcosystemStatsCollector.getDataTypesReadOrWritten()).thenReturn(readOrWritten);
        when(mEcosystemStatsCollector.getDataTypesRead()).thenReturn(read);
        when(mEcosystemStatsCollector.getDataTypesWritten()).thenReturn(written);
        when(mEcosystemStatsCollector.getDataTypeShared()).thenReturn(shared);
        when(mEcosystemStatsCollector.getNumberOfAppPairings()).thenReturn(10);

        mEcosystemStatsLogger.log(mEcosystemStatsCollector);

        verify(mHealthFitnessStatsLog)
                .write(
                        eq(HEALTH_CONNECT_ECOSYSTEM_STATS),
                        eq(
                                new int[] {
                                    getLoggedRecordTypeId(RECORD_TYPE_STEPS),
                                    getLoggedRecordTypeId(RECORD_TYPE_HEART_RATE)
                                }),
                        eq(new int[] {getLoggedRecordTypeId(RECORD_TYPE_STEPS)}),
                        eq(new int[] {getLoggedRecordTypeId(RECORD_TYPE_HEART_RATE)}),
                        eq(new int[] {getLoggedRecordTypeId(RECORD_TYPE_STEPS)}),
                        eq(10));
    }

    @Test
    public void logsDirectionalPairings_replacesSpn() {
        Map<String, Set<String>> pairings = new HashMap<>();
        pairings.put(TEST_SPN_PACKAGE_CANONICAL, Collections.singleton(TEST_PACKAGE_READER));
        when(mEcosystemStatsCollector.getDirectionalAppPairings()).thenReturn(pairings);

        mEcosystemStatsLogger.log(mEcosystemStatsCollector);

        verify(mHealthFitnessStatsLog)
                .write(
                        HEALTH_CONNECT_RESTRICTED_ECOSYSTEM_STATS,
                        TEST_SPN_PACKAGE,
                        TEST_PACKAGE_READER,
                        HEALTH_CONNECT_RESTRICTED_ECOSYSTEM_STATS__DATA_TYPE__DATA_TYPE_UNKNOWN,
                        HEALTH_CONNECT_RESTRICTED_ECOSYSTEM_STATS__METRIC_TYPE__METRIC_TYPE_DIRECTIONAL_PAIRING);
    }

    @Test
    public void logsDirectionalPairingsPerDataType_replacesSpn() {
        Map<String, Map<Integer, Set<String>>> pairings = new HashMap<>();
        Map<Integer, Set<String>> dataTypeToReaders = new HashMap<>();
        dataTypeToReaders.put(RECORD_TYPE_STEPS, Collections.singleton(TEST_SPN_PACKAGE_CANONICAL));
        pairings.put(TEST_PACKAGE_WRITER, dataTypeToReaders);

        when(mEcosystemStatsCollector.getDirectionalAppPairingsPerDataType()).thenReturn(pairings);

        mEcosystemStatsLogger.log(mEcosystemStatsCollector);

        verify(mHealthFitnessStatsLog)
                .write(
                        HEALTH_CONNECT_RESTRICTED_ECOSYSTEM_STATS,
                        TEST_PACKAGE_WRITER,
                        TEST_SPN_PACKAGE,
                        getLoggedRecordTypeId(RECORD_TYPE_STEPS),
                        HEALTH_CONNECT_RESTRICTED_ECOSYSTEM_STATS__METRIC_TYPE__METRIC_TYPE_DIRECTIONAL_PAIRING_PER_DATA_TYPE);
    }

    private int getLoggedRecordTypeId(int recordTypeId) {
        return InternalHealthConnectMappings.getInstance()
                .getLoggingEnumForRecordTypeId(recordTypeId);
    }
}
