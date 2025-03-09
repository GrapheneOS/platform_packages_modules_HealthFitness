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

import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_ECOSYSTEM_STATS;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_ECOSYSTEM_STATS__READ__BASAL_METABOLIC_RATE;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_ECOSYSTEM_STATS__READ__BLOOD_GLUCOSE;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_ECOSYSTEM_STATS__READ__BLOOD_PRESSURE;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_ECOSYSTEM_STATS__READ__DISTANCE;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_ECOSYSTEM_STATS__READ__HEART_RATE;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_ECOSYSTEM_STATS__READ__HEIGHT;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_ECOSYSTEM_STATS__READ__STEPS;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_PERMISSION_STATS;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_PHR_STORAGE_STATS;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_PHR_USAGE_STATS;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_RESTRICTED_ECOSYSTEM_STATS;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_RESTRICTED_ECOSYSTEM_STATS__DATA_TYPE__DATA_TYPE_UNKNOWN;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_RESTRICTED_ECOSYSTEM_STATS__DATA_TYPE__DISTANCE;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_RESTRICTED_ECOSYSTEM_STATS__DATA_TYPE__HEART_RATE;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_RESTRICTED_ECOSYSTEM_STATS__METRIC_TYPE__METRIC_TYPE_DIRECTIONAL_PAIRING;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_RESTRICTED_ECOSYSTEM_STATS__METRIC_TYPE__METRIC_TYPE_DIRECTIONAL_PAIRING_PER_DATA_TYPE;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_STORAGE_STATS;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_USAGE_STATS;
import static android.health.connect.HealthPermissions.READ_DISTANCE;
import static android.health.connect.HealthPermissions.READ_EXERCISE;
import static android.health.connect.HealthPermissions.READ_STEPS;

import static com.android.healthfitness.flags.Flags.FLAG_ECOSYSTEM_METRICS;
import static com.android.healthfitness.flags.Flags.FLAG_ECOSYSTEM_METRICS_DB_CHANGES;
import static com.android.healthfitness.flags.Flags.FLAG_PERMISSION_METRICS;
import static com.android.healthfitness.flags.Flags.FLAG_PERSONAL_HEALTH_RECORD;
import static com.android.healthfitness.flags.Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE;
import static com.android.healthfitness.flags.Flags.FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.health.HealthFitnessStatsLog;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.server.healthconnect.storage.datatypehelpers.DatabaseStatsCollector;
import com.android.server.healthconnect.storage.datatypehelpers.MedicalDataSourceHelper;
import com.android.server.healthconnect.storage.datatypehelpers.MedicalResourceHelper;
import com.android.server.healthconnect.storage.datatypehelpers.MedicalResourceIndicesHelper;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
public class DailyLoggingServiceTest {

    @Rule(order = 1)
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Rule(order = 2)
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock private UsageStatsCollector mUsageStatsCollector;
    @Mock private DatabaseStatsCollector mDatabaseStatsCollector;
    @Mock private EcosystemStatsCollector mEcosystemStatsCollector;
    @Mock private HealthFitnessStatsLog mHealthFitnessStatsLog;
    @Captor private ArgumentCaptor<List<String>> mStringListCaptor;

    private static final String CONNECTED_APP_PACKAGE_NAME = "connected.app";
    private static final String CONNECTED_APP_TWO_PACKAGE_NAME = "connected.app.two";

    @Test
    public void testDatabaseLogsStats() {
        when(mDatabaseStatsCollector.getDatabaseSize()).thenReturn(1L);
        when(mDatabaseStatsCollector.getNumberOfChangeLogs()).thenReturn(2L);
        when(mDatabaseStatsCollector.getNumberOfInstantRecordRows()).thenReturn(3L);
        when(mDatabaseStatsCollector.getNumberOfIntervalRecordRows()).thenReturn(4L);
        when(mDatabaseStatsCollector.getNumberOfSeriesRecordRows()).thenReturn(5L);

        DailyLoggingService.logDailyMetrics(
                mUsageStatsCollector,
                mDatabaseStatsCollector,
                mEcosystemStatsCollector,
                mHealthFitnessStatsLog);

        verify(mHealthFitnessStatsLog, times(1))
                .write(
                        eq(HEALTH_CONNECT_STORAGE_STATS),
                        /* databaseSize */ eq(1L),
                        /* numberOfInstantRecords */ eq(3L),
                        /* numberOfIntervalRecords */ eq(4L),
                        /* numberOfSeriesRecords */ eq(5L),
                        /* numberOfChangeLogs */ eq(2L));
    }

    @Test
    public void testDatabaseLogsStats_userDoesNotUseHealthConnect() {
        when(mDatabaseStatsCollector.getDatabaseSize()).thenReturn(0L);
        when(mDatabaseStatsCollector.getNumberOfChangeLogs()).thenReturn(0L);
        when(mDatabaseStatsCollector.getNumberOfInstantRecordRows()).thenReturn(0L);
        when(mDatabaseStatsCollector.getNumberOfIntervalRecordRows()).thenReturn(0L);
        when(mDatabaseStatsCollector.getNumberOfSeriesRecordRows()).thenReturn(0L);

        verify(mHealthFitnessStatsLog, never())
                .write(
                        eq(HEALTH_CONNECT_STORAGE_STATS),
                        anyLong(),
                        anyLong(),
                        anyLong(),
                        anyLong(),
                        anyLong());
    }

    @Test
    public void testDailyUsageStatsLogs_oneConnectedApp_twoAvailableApps_userNotMonthlyActive() {

        when(mUsageStatsCollector.getNumberOfAppsCompatibleWithHealthConnect()).thenReturn(2);
        when(mUsageStatsCollector.getPackagesHoldingHealthPermissions())
                .thenReturn(Map.of(CONNECTED_APP_PACKAGE_NAME, List.of(READ_DISTANCE)));
        when(mUsageStatsCollector.isUserMonthlyActive()).thenReturn(false);

        DailyLoggingService.logDailyMetrics(
                mUsageStatsCollector,
                mDatabaseStatsCollector,
                mEcosystemStatsCollector,
                mHealthFitnessStatsLog);

        verify(mHealthFitnessStatsLog, times(1))
                .write(
                        eq(HEALTH_CONNECT_USAGE_STATS), /* connectedAppsCount */
                        eq(1), /* availableAppsCount */
                        eq(2), /* isUserMonthlyActive */
                        eq(false));
    }

    @Test
    public void testDailyUsageStatsLogs_oneConnectedApp_twoAvailableApps_userMonthlyActive() {

        when(mUsageStatsCollector.getNumberOfAppsCompatibleWithHealthConnect()).thenReturn(2);
        when(mUsageStatsCollector.getPackagesHoldingHealthPermissions())
                .thenReturn(Map.of(CONNECTED_APP_PACKAGE_NAME, List.of(READ_DISTANCE)));
        when(mUsageStatsCollector.isUserMonthlyActive()).thenReturn(true);

        DailyLoggingService.logDailyMetrics(
                mUsageStatsCollector,
                mDatabaseStatsCollector,
                mEcosystemStatsCollector,
                mHealthFitnessStatsLog);

        verify(mHealthFitnessStatsLog, times(1))
                .write(
                        eq(HEALTH_CONNECT_USAGE_STATS), /* connectedAppsCount */
                        eq(1), /* availableAppsCount */
                        eq(2), /* isUserMonthlyActive */
                        eq(true));
    }

    @Test
    @EnableFlags(FLAG_PERMISSION_METRICS)
    public void permissionMetricsEnabled_twoConnectedApps_testPermissionsStatsLogs() {

        when(mUsageStatsCollector.getPackagesHoldingHealthPermissions())
                .thenReturn(
                        Map.of(
                                CONNECTED_APP_PACKAGE_NAME,
                                List.of(READ_DISTANCE, READ_EXERCISE),
                                CONNECTED_APP_TWO_PACKAGE_NAME,
                                List.of(READ_STEPS)));

        DailyLoggingService.logDailyMetrics(
                mUsageStatsCollector,
                mDatabaseStatsCollector,
                mEcosystemStatsCollector,
                mHealthFitnessStatsLog);

        verify(mHealthFitnessStatsLog, times(1))
                .write(
                        eq(HEALTH_CONNECT_PERMISSION_STATS),
                        eq(CONNECTED_APP_PACKAGE_NAME),
                        eq(new String[] {"READ_DISTANCE", "READ_EXERCISE"}));
        verify(mHealthFitnessStatsLog, times(1))
                .write(
                        eq(HEALTH_CONNECT_PERMISSION_STATS),
                        eq(CONNECTED_APP_TWO_PACKAGE_NAME),
                        eq(
                                new String[] {
                                    "READ_STEPS",
                                }));
    }

    @Test
    @DisableFlags(FLAG_PERMISSION_METRICS)
    public void permissionMetricsDisabled_oneConnectedApps_testPermissionsStatsDoNotLog() {
        when(mUsageStatsCollector.getPackagesHoldingHealthPermissions())
                .thenReturn(
                        Map.of(
                                CONNECTED_APP_PACKAGE_NAME,
                                List.of(READ_DISTANCE, READ_EXERCISE),
                                CONNECTED_APP_TWO_PACKAGE_NAME,
                                List.of(READ_STEPS)));

        DailyLoggingService.logDailyMetrics(
                mUsageStatsCollector,
                mDatabaseStatsCollector,
                mEcosystemStatsCollector,
                mHealthFitnessStatsLog);

        verify(mHealthFitnessStatsLog, never())
                .write(
                        eq(HEALTH_CONNECT_PERMISSION_STATS),
                        eq(CONNECTED_APP_PACKAGE_NAME),
                        eq(new String[] {"READ_DISTANCE"}));
    }

    @Test
    @DisableFlags(FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY)
    public void phrStats_flagDisabled_expectNoLogs() {
        when(mUsageStatsCollector.getNumberOfAppsCompatibleWithHealthConnect()).thenReturn(1);

        DailyLoggingService.logDailyMetrics(
                mUsageStatsCollector,
                mDatabaseStatsCollector,
                mEcosystemStatsCollector,
                mHealthFitnessStatsLog);

        verify(mHealthFitnessStatsLog, never())
                .write(eq(HEALTH_CONNECT_PHR_USAGE_STATS), anyInt(), anyInt(), anyInt(), anyInt());

        verify(mHealthFitnessStatsLog, never())
                .write(eq(HEALTH_CONNECT_PHR_STORAGE_STATS), anyInt());
    }

    @Test
    @EnableFlags({
        FLAG_PERSONAL_HEALTH_RECORD,
        FLAG_PERSONAL_HEALTH_RECORD_DATABASE,
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY
    })
    public void phrStats_flagEnabledAndIsMonthlyActiveUser_expectCorrectLogs() {
        when(mUsageStatsCollector.getNumberOfAppsCompatibleWithHealthConnect()).thenReturn(1);
        when(mUsageStatsCollector.isPhrMonthlyActiveUser()).thenReturn(true);
        when(mUsageStatsCollector.getMedicalDataSourcesCount()).thenReturn(101);
        when(mUsageStatsCollector.getMedicalResourcesCount()).thenReturn(204);
        when(mUsageStatsCollector.getGrantedPhrAppsCount()).thenReturn(1L);

        DailyLoggingService.logDailyMetrics(
                mUsageStatsCollector,
                mDatabaseStatsCollector,
                mEcosystemStatsCollector,
                mHealthFitnessStatsLog);

        verify(mHealthFitnessStatsLog, times(1))
                .write(
                        eq(HEALTH_CONNECT_PHR_USAGE_STATS),
                        /* medicalDataSourcesCount */ eq(101),
                        /* medicalResourcesCount */ eq(204),
                        /* isPhrMonthlyActiveUser */ eq(true),
                        /* phrAppsCount */ eq(1));
    }

    @Test
    @EnableFlags({
        FLAG_PERSONAL_HEALTH_RECORD,
        FLAG_PERSONAL_HEALTH_RECORD_DATABASE,
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY
    })
    public void phrStats_flagEnabledAndIsNotMonthlyActiveUser_expectCorrectLogs() {
        when(mUsageStatsCollector.getNumberOfAppsCompatibleWithHealthConnect()).thenReturn(1);
        when(mUsageStatsCollector.isPhrMonthlyActiveUser()).thenReturn(false);
        when(mUsageStatsCollector.getMedicalDataSourcesCount()).thenReturn(101);
        when(mUsageStatsCollector.getMedicalResourcesCount()).thenReturn(204);
        when(mUsageStatsCollector.getGrantedPhrAppsCount()).thenReturn(0L);

        DailyLoggingService.logDailyMetrics(
                mUsageStatsCollector,
                mDatabaseStatsCollector,
                mEcosystemStatsCollector,
                mHealthFitnessStatsLog);

        verify(mHealthFitnessStatsLog, times(1))
                .write(
                        eq(HEALTH_CONNECT_PHR_USAGE_STATS),
                        /* medicalDataSourcesCount */ eq(101),
                        /* medicalResourcesCount */ eq(204),
                        /* isPhrMonthlyActiveUser */ eq(false),
                        /* phrAppsCount */ eq(0));
    }

    @Test
    @EnableFlags({
        FLAG_PERSONAL_HEALTH_RECORD,
        FLAG_PERSONAL_HEALTH_RECORD_DATABASE,
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY
    })
    public void phrStats_flagEnabled_phrDataExists_expectCorrectPhrDbStatsLogs() {
        when(mDatabaseStatsCollector.getFileBytes(
                        eq(
                                Set.of(
                                        MedicalDataSourceHelper.getMainTableName(),
                                        MedicalResourceHelper.getMainTableName(),
                                        MedicalResourceIndicesHelper.getTableName()))))
                .thenReturn(101L);
        when(mUsageStatsCollector.getMedicalResourcesCount()).thenReturn(1);
        DailyLoggingService.logDailyMetrics(
                mUsageStatsCollector,
                mDatabaseStatsCollector,
                mEcosystemStatsCollector,
                mHealthFitnessStatsLog);

        verify(mHealthFitnessStatsLog, times(1))
                .write(eq(HEALTH_CONNECT_PHR_STORAGE_STATS), eq(101L));
    }

    @Test
    @EnableFlags({
        FLAG_PERSONAL_HEALTH_RECORD,
        FLAG_PERSONAL_HEALTH_RECORD_DATABASE,
        FLAG_PERSONAL_HEALTH_RECORD_TELEMETRY
    })
    public void phrStats_flagEnabled_noPhRdata_expectNoPhrDbStatsLogs() {
        when(mDatabaseStatsCollector.getFileBytes(mStringListCaptor.capture())).thenReturn(101L);
        when(mUsageStatsCollector.getMedicalResourcesCount()).thenReturn(0);
        when(mUsageStatsCollector.getMedicalDataSourcesCount()).thenReturn(0);

        DailyLoggingService.logDailyMetrics(
                mUsageStatsCollector,
                mDatabaseStatsCollector,
                mEcosystemStatsCollector,
                mHealthFitnessStatsLog);

        Mockito.verify(mDatabaseStatsCollector, never()).getFileBytes(any());
        verify(mHealthFitnessStatsLog, never())
                .write(eq(HEALTH_CONNECT_PHR_STORAGE_STATS), anyInt());
    }

    @Test
    @EnableFlags({
        FLAG_ECOSYSTEM_METRICS,
        FLAG_ECOSYSTEM_METRICS_DB_CHANGES,
    })
    public void flagsEnabled_testEcosystemMetrics_regularLogging() {
        when(mEcosystemStatsCollector.getDataTypesReadOrWritten())
                .thenReturn(
                        Set.of(
                                RecordTypeIdentifier.RECORD_TYPE_BASAL_METABOLIC_RATE,
                                RecordTypeIdentifier.RECORD_TYPE_HEIGHT));
        when(mEcosystemStatsCollector.getDataTypesRead())
                .thenReturn(
                        Set.of(
                                RecordTypeIdentifier.RECORD_TYPE_BLOOD_GLUCOSE,
                                RecordTypeIdentifier.RECORD_TYPE_BLOOD_PRESSURE));
        when(mEcosystemStatsCollector.getDataTypesWritten())
                .thenReturn(
                        Set.of(
                                RecordTypeIdentifier.RECORD_TYPE_HEART_RATE,
                                RecordTypeIdentifier.RECORD_TYPE_HEIGHT));
        when(mEcosystemStatsCollector.getDataTypeShared())
                .thenReturn(
                        Set.of(
                                RecordTypeIdentifier.RECORD_TYPE_STEPS,
                                RecordTypeIdentifier.RECORD_TYPE_DISTANCE));
        when(mEcosystemStatsCollector.getNumberOfAppPairings()).thenReturn(5);

        DailyLoggingService.logDailyMetrics(
                mUsageStatsCollector,
                mDatabaseStatsCollector,
                mEcosystemStatsCollector,
                mHealthFitnessStatsLog);

        verify(mHealthFitnessStatsLog, times(1))
                .write(
                        eq(HEALTH_CONNECT_ECOSYSTEM_STATS),
                        argThat(
                                new ArrayMatcher(
                                        new int[] {
                                            HEALTH_CONNECT_ECOSYSTEM_STATS__READ__HEIGHT,
                                            HEALTH_CONNECT_ECOSYSTEM_STATS__READ__BASAL_METABOLIC_RATE
                                        })),
                        argThat(
                                new ArrayMatcher(
                                        new int[] {
                                            HEALTH_CONNECT_ECOSYSTEM_STATS__READ__BLOOD_PRESSURE,
                                            HEALTH_CONNECT_ECOSYSTEM_STATS__READ__BLOOD_GLUCOSE
                                        })),
                        argThat(
                                new ArrayMatcher(
                                        new int[] {
                                            HEALTH_CONNECT_ECOSYSTEM_STATS__READ__HEIGHT,
                                            HEALTH_CONNECT_ECOSYSTEM_STATS__READ__HEART_RATE
                                        })),
                        argThat(
                                new ArrayMatcher(
                                        new int[] {
                                            HEALTH_CONNECT_ECOSYSTEM_STATS__READ__DISTANCE,
                                            HEALTH_CONNECT_ECOSYSTEM_STATS__READ__STEPS
                                        })),
                        eq(5));
    }

    @Test
    @EnableFlags({
        FLAG_ECOSYSTEM_METRICS,
        FLAG_ECOSYSTEM_METRICS_DB_CHANGES,
    })
    public void flagsEnabled_testEcosystemMetrics_privateLogging() {
        when(mEcosystemStatsCollector.getDirectionalAppPairings())
                .thenReturn(
                        Map.of(
                                CONNECTED_APP_PACKAGE_NAME,
                                Set.of(CONNECTED_APP_TWO_PACKAGE_NAME),
                                CONNECTED_APP_TWO_PACKAGE_NAME,
                                Set.of(CONNECTED_APP_PACKAGE_NAME)));
        when(mEcosystemStatsCollector.getDirectionalAppPairingsPerDataType())
                .thenReturn(
                        Map.of(
                                CONNECTED_APP_PACKAGE_NAME,
                                Map.of(
                                        RecordTypeIdentifier.RECORD_TYPE_DISTANCE,
                                        Set.of(CONNECTED_APP_TWO_PACKAGE_NAME)),
                                CONNECTED_APP_TWO_PACKAGE_NAME,
                                Map.of(
                                        RecordTypeIdentifier.RECORD_TYPE_HEART_RATE,
                                        Set.of(CONNECTED_APP_PACKAGE_NAME))));

        DailyLoggingService.logDailyMetrics(
                mUsageStatsCollector,
                mDatabaseStatsCollector,
                mEcosystemStatsCollector,
                mHealthFitnessStatsLog);

        verify(mHealthFitnessStatsLog, times(1))
                .write(
                        eq(HEALTH_CONNECT_RESTRICTED_ECOSYSTEM_STATS),
                        eq(CONNECTED_APP_PACKAGE_NAME),
                        eq(CONNECTED_APP_TWO_PACKAGE_NAME),
                        eq(HEALTH_CONNECT_RESTRICTED_ECOSYSTEM_STATS__DATA_TYPE__DATA_TYPE_UNKNOWN),
                        eq(
                                HEALTH_CONNECT_RESTRICTED_ECOSYSTEM_STATS__METRIC_TYPE__METRIC_TYPE_DIRECTIONAL_PAIRING));
        verify(mHealthFitnessStatsLog, times(1))
                .write(
                        eq(HEALTH_CONNECT_RESTRICTED_ECOSYSTEM_STATS),
                        eq(CONNECTED_APP_PACKAGE_NAME),
                        eq(CONNECTED_APP_TWO_PACKAGE_NAME),
                        eq(HEALTH_CONNECT_RESTRICTED_ECOSYSTEM_STATS__DATA_TYPE__DISTANCE),
                        eq(
                                HEALTH_CONNECT_RESTRICTED_ECOSYSTEM_STATS__METRIC_TYPE__METRIC_TYPE_DIRECTIONAL_PAIRING_PER_DATA_TYPE));
        verify(mHealthFitnessStatsLog, times(1))
                .write(
                        eq(HEALTH_CONNECT_RESTRICTED_ECOSYSTEM_STATS),
                        eq(CONNECTED_APP_TWO_PACKAGE_NAME),
                        eq(CONNECTED_APP_PACKAGE_NAME),
                        eq(HEALTH_CONNECT_RESTRICTED_ECOSYSTEM_STATS__DATA_TYPE__DATA_TYPE_UNKNOWN),
                        eq(
                                HEALTH_CONNECT_RESTRICTED_ECOSYSTEM_STATS__METRIC_TYPE__METRIC_TYPE_DIRECTIONAL_PAIRING));
        verify(mHealthFitnessStatsLog, times(1))
                .write(
                        eq(HEALTH_CONNECT_RESTRICTED_ECOSYSTEM_STATS),
                        eq(CONNECTED_APP_TWO_PACKAGE_NAME),
                        eq(CONNECTED_APP_PACKAGE_NAME),
                        eq(HEALTH_CONNECT_RESTRICTED_ECOSYSTEM_STATS__DATA_TYPE__HEART_RATE),
                        eq(
                                HEALTH_CONNECT_RESTRICTED_ECOSYSTEM_STATS__METRIC_TYPE__METRIC_TYPE_DIRECTIONAL_PAIRING_PER_DATA_TYPE));
    }

    @Test
    @DisableFlags({
        FLAG_ECOSYSTEM_METRICS,
        FLAG_ECOSYSTEM_METRICS_DB_CHANGES,
    })
    public void flagsDisabled_doNotLogEcosystemMetrics() {
        when(mEcosystemStatsCollector.getDataTypesReadOrWritten())
                .thenReturn(
                        Set.of(
                                RecordTypeIdentifier.RECORD_TYPE_BASAL_METABOLIC_RATE,
                                RecordTypeIdentifier.RECORD_TYPE_HEIGHT));
        when(mEcosystemStatsCollector.getDataTypesRead())
                .thenReturn(
                        Set.of(
                                RecordTypeIdentifier.RECORD_TYPE_BLOOD_GLUCOSE,
                                RecordTypeIdentifier.RECORD_TYPE_BLOOD_PRESSURE));
        when(mEcosystemStatsCollector.getDataTypesWritten())
                .thenReturn(
                        Set.of(
                                RecordTypeIdentifier.RECORD_TYPE_HEART_RATE,
                                RecordTypeIdentifier.RECORD_TYPE_HEIGHT));
        when(mEcosystemStatsCollector.getDataTypeShared())
                .thenReturn(
                        Set.of(
                                RecordTypeIdentifier.RECORD_TYPE_STEPS,
                                RecordTypeIdentifier.RECORD_TYPE_DISTANCE));
        when(mEcosystemStatsCollector.getNumberOfAppPairings()).thenReturn(5);
        when(mEcosystemStatsCollector.getDirectionalAppPairings())
                .thenReturn(
                        Map.of(
                                CONNECTED_APP_PACKAGE_NAME,
                                Set.of(CONNECTED_APP_TWO_PACKAGE_NAME),
                                CONNECTED_APP_TWO_PACKAGE_NAME,
                                Set.of(CONNECTED_APP_PACKAGE_NAME)));
        when(mEcosystemStatsCollector.getDirectionalAppPairingsPerDataType())
                .thenReturn(
                        Map.of(
                                CONNECTED_APP_PACKAGE_NAME,
                                Map.of(
                                        RecordTypeIdentifier.RECORD_TYPE_DISTANCE,
                                        Set.of(CONNECTED_APP_TWO_PACKAGE_NAME)),
                                CONNECTED_APP_TWO_PACKAGE_NAME,
                                Map.of(
                                        RecordTypeIdentifier.RECORD_TYPE_HEART_RATE,
                                        Set.of(CONNECTED_APP_PACKAGE_NAME))));

        DailyLoggingService.logDailyMetrics(
                mUsageStatsCollector,
                mDatabaseStatsCollector,
                mEcosystemStatsCollector,
                mHealthFitnessStatsLog);

        verify(mHealthFitnessStatsLog, never())
                .write(eq(HEALTH_CONNECT_ECOSYSTEM_STATS), any(), any(), any(), any(), anyInt());
        verify(mHealthFitnessStatsLog, never())
                .write(
                        eq(HEALTH_CONNECT_RESTRICTED_ECOSYSTEM_STATS),
                        anyString(),
                        anyString(),
                        anyInt(),
                        anyInt());
    }

    public static class ArrayMatcher implements ArgumentMatcher<int[]> {
        private final int[] expectedArray;

        public ArrayMatcher(int[] expectedArray) {
            this.expectedArray = expectedArray;
        }

        @Override
        public boolean matches(int[] actualArray) {
            Arrays.sort(actualArray);
            Arrays.sort(expectedArray);
            return Arrays.equals(expectedArray, actualArray);
        }
    }
}
