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

package com.android.server.healthconnect.common.logging;

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
import static android.health.connect.HealthPermissions.READ_MEDICAL_DATA_CONDITIONS;
import static android.health.connect.HealthPermissions.READ_STEPS;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.health.HealthFitnessStatsLog;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.server.healthconnect.phr.storage.MedicalDataSourceHelper;
import com.android.server.healthconnect.phr.storage.MedicalResourceHelper;
import com.android.server.healthconnect.phr.storage.MedicalResourceIndicesHelper;

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
    @Mock private NativeTrackingStatsCollector mNativeTrackingStatsCollector;
    @Mock private HealthFitnessStatsLog mHealthFitnessStatsLog;
    @Captor private ArgumentCaptor<List<String>> mStringListCaptor;

    private static final String CONNECTED_APP_PACKAGE_NAME = "connected.app";
    private static final String CONNECTED_APP_TWO_PACKAGE_NAME = "connected.app.two";
    private static final String NOT_CONNECTED_APP_PACKAGE_NAME = "not.connected.app";

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
                mNativeTrackingStatsCollector,
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
        when(mUsageStatsCollector.getPackagesCompatibleWithHealthConnect())
                .thenReturn(
                        Map.of(
                                CONNECTED_APP_PACKAGE_NAME,
                                List.of(READ_DISTANCE),
                                NOT_CONNECTED_APP_PACKAGE_NAME,
                                List.of()));
        when(mUsageStatsCollector.isUserMonthlyActive()).thenReturn(false);

        DailyLoggingService.logDailyMetrics(
                mUsageStatsCollector,
                mDatabaseStatsCollector,
                mEcosystemStatsCollector,
                mNativeTrackingStatsCollector,
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
        when(mUsageStatsCollector.getPackagesCompatibleWithHealthConnect())
                .thenReturn(
                        Map.of(
                                CONNECTED_APP_PACKAGE_NAME,
                                List.of(READ_DISTANCE),
                                NOT_CONNECTED_APP_PACKAGE_NAME,
                                List.of()));
        when(mUsageStatsCollector.isUserMonthlyActive()).thenReturn(true);

        DailyLoggingService.logDailyMetrics(
                mUsageStatsCollector,
                mDatabaseStatsCollector,
                mEcosystemStatsCollector,
                mNativeTrackingStatsCollector,
                mHealthFitnessStatsLog);

        verify(mHealthFitnessStatsLog, times(1))
                .write(
                        eq(HEALTH_CONNECT_USAGE_STATS), /* connectedAppsCount */
                        eq(1), /* availableAppsCount */
                        eq(2), /* isUserMonthlyActive */
                        eq(true));
    }

    public void permissionMetricsEnabled_twoConnectedApps_testPermissionsStatsLogs() {
        when(mUsageStatsCollector.getPackagesCompatibleWithHealthConnect())
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
                mNativeTrackingStatsCollector,
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
    public void phrStats_isMonthlyActiveUser_expectCorrectLogs() {
        when(mUsageStatsCollector.getPackagesCompatibleWithHealthConnect())
                .thenReturn(
                        Map.of(CONNECTED_APP_PACKAGE_NAME, List.of(READ_MEDICAL_DATA_CONDITIONS)));
        when(mUsageStatsCollector.isPhrMonthlyActiveUser()).thenReturn(true);
        when(mUsageStatsCollector.getMedicalDataSourcesCount()).thenReturn(101);
        when(mUsageStatsCollector.getMedicalResourcesCount()).thenReturn(204);
        when(mUsageStatsCollector.getGrantedPhrAppsCount()).thenReturn(1L);

        DailyLoggingService.logDailyMetrics(
                mUsageStatsCollector,
                mDatabaseStatsCollector,
                mEcosystemStatsCollector,
                mNativeTrackingStatsCollector,
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
    public void phrStats_isNotMonthlyActiveUser_expectCorrectLogs() {
        when(mUsageStatsCollector.getPackagesCompatibleWithHealthConnect())
                .thenReturn(
                        Map.of(CONNECTED_APP_PACKAGE_NAME, List.of(READ_MEDICAL_DATA_CONDITIONS)));
        when(mUsageStatsCollector.isPhrMonthlyActiveUser()).thenReturn(false);
        when(mUsageStatsCollector.getMedicalDataSourcesCount()).thenReturn(101);
        when(mUsageStatsCollector.getMedicalResourcesCount()).thenReturn(204);
        when(mUsageStatsCollector.getGrantedPhrAppsCount()).thenReturn(0L);

        DailyLoggingService.logDailyMetrics(
                mUsageStatsCollector,
                mDatabaseStatsCollector,
                mEcosystemStatsCollector,
                mNativeTrackingStatsCollector,
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
    public void phrStats_phrDataExists_expectCorrectPhrDbStatsLogs() {
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
                mNativeTrackingStatsCollector,
                mHealthFitnessStatsLog);

        verify(mHealthFitnessStatsLog, times(1))
                .write(eq(HEALTH_CONNECT_PHR_STORAGE_STATS), eq(101L));
    }

    @Test
    public void phrStats_noPhRdata_expectNoPhrDbStatsLogs() {
        when(mDatabaseStatsCollector.getFileBytes(mStringListCaptor.capture())).thenReturn(101L);
        when(mUsageStatsCollector.getMedicalResourcesCount()).thenReturn(0);
        when(mUsageStatsCollector.getMedicalDataSourcesCount()).thenReturn(0);

        DailyLoggingService.logDailyMetrics(
                mUsageStatsCollector,
                mDatabaseStatsCollector,
                mEcosystemStatsCollector,
                mNativeTrackingStatsCollector,
                mHealthFitnessStatsLog);

        Mockito.verify(mDatabaseStatsCollector, never()).getFileBytes(any());
        verify(mHealthFitnessStatsLog, never())
                .write(eq(HEALTH_CONNECT_PHR_STORAGE_STATS), anyInt());
    }

    @Test
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
                mNativeTrackingStatsCollector,
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
                mNativeTrackingStatsCollector,
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
    public void testNativeTrackingLogsStats() {
        when(mNativeTrackingStatsCollector.getNativeDataTypesActive()).thenReturn(new int[] {1});
        when(mNativeTrackingStatsCollector.getNativeDataTypesDisabled()).thenReturn(new int[] {2});
        when(mNativeTrackingStatsCollector.getNumberOfWrites()).thenReturn(3);
        when(mNativeTrackingStatsCollector.getLastErrorCode()).thenReturn(4);
        when(mNativeTrackingStatsCollector.getStepsReadersCount()).thenReturn(5);
        when(mNativeTrackingStatsCollector.getStepsWritersCount()).thenReturn(6);

        DailyLoggingService.logDailyMetrics(
                mUsageStatsCollector,
                mDatabaseStatsCollector,
                mEcosystemStatsCollector,
                mNativeTrackingStatsCollector,
                mHealthFitnessStatsLog);

        verify(mHealthFitnessStatsLog, times(1))
                .write(
                        eq(HealthFitnessStatsLog.HEALTH_CONNECT_NATIVE_TRACKING_STATS_REPORTED),
                        eq(new int[] {1}),
                        eq(new int[] {2}),
                        eq(3),
                        anyInt(),
                        eq(4),
                        eq(5),
                        eq(6));
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
