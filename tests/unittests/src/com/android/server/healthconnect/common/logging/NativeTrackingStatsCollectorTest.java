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

package com.android.server.healthconnect.common.logging;

import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_NATIVE_TRACKING_STATS_REPORTED__LAST_ERROR_CODE__NATIVE_TRACKING_ERROR_CODE_UNSPECIFIED;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_NATIVE_TRACKING_STATS_REPORTED__NATIVE_DATA_TYPES_ACTIVE__NATIVE_TRACKING_DATA_TYPE_STEPS;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_NATIVE_TRACKING_STATS_REPORTED__NATIVE_DATA_TYPES_DISABLED__NATIVE_TRACKING_DATA_TYPE_STEPS;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.health.connect.HealthPermissions;
import android.os.UserHandle;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import com.android.server.healthconnect.common.metadata.AppInfoHelper;
import com.android.server.healthconnect.device.tracker.TrackerManager;
import com.android.server.healthconnect.permission.HealthConnectPermissionHelper;
import com.android.server.healthconnect.permission.PackageInfoUtils;
import com.android.server.healthconnect.storage.TransactionManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class NativeTrackingStatsCollectorTest {

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Mock private PackageInfoUtils mPackageInfoUtils;
    @Mock private Context mContext;
    @Mock private TransactionManager mTransactionManager;
    @Mock private AppInfoHelper mAppInfoHelper;
    @Mock private TrackerManager mTrackerManager;
    @Mock private HealthConnectPermissionHelper mHealthConnectPermissionsHelper;

    private static final String TEST_PACKAGE_NAME_1 = "test.app.1";
    private static final String TEST_PACKAGE_NAME_2 = "test.app.2";
    private NativeTrackingStatsCollector mNativeTrackingStatsCollector;

    @Before
    public void setUp() {
        mContext = spy(InstrumentationRegistry.getInstrumentation().getContext());
        PackageManager packageManager = mContext.getPackageManager();
        when(mContext.getPackageManager()).thenReturn(packageManager);
        mNativeTrackingStatsCollector =
                new NativeTrackingStatsCollector(
                        mPackageInfoUtils,
                        mContext,
                        UserHandle.CURRENT,
                        mTransactionManager,
                        mAppInfoHelper,
                        mTrackerManager,
                        mHealthConnectPermissionsHelper);
    }

    @Test
    public void processStats_stepTrackingActive_logsCorrectly() {
        when(mTrackerManager.isStepTrackingActive()).thenReturn(true);
        when(mTrackerManager.isStepTrackingExplicitlyDisabled()).thenReturn(false);
        PackageInfo packageInfo1 = new PackageInfo();
        packageInfo1.packageName = TEST_PACKAGE_NAME_1;
        PackageInfo packageInfo2 = new PackageInfo();
        packageInfo2.packageName = TEST_PACKAGE_NAME_2;
        when(mPackageInfoUtils.getPackagesHoldingHealthPermissions(any(), any()))
                .thenReturn(List.of(packageInfo1, packageInfo2));
        when(mHealthConnectPermissionsHelper.getGrantedHealthPermissions(
                        eq(packageInfo1.packageName), any()))
                .thenReturn(List.of(HealthPermissions.READ_STEPS));
        when(mHealthConnectPermissionsHelper.getGrantedHealthPermissions(
                        eq(packageInfo2.packageName), any()))
                .thenReturn(List.of(HealthPermissions.WRITE_STEPS));
        when(mAppInfoHelper.getAppInfoId(anyString())).thenReturn(1L);
        when(mTransactionManager.count(any())).thenReturn(10);

        mNativeTrackingStatsCollector.processStats();

        assertThat(mNativeTrackingStatsCollector.getNativeDataTypesActive())
                .asList()
                .containsExactly(
                        HEALTH_CONNECT_NATIVE_TRACKING_STATS_REPORTED__NATIVE_DATA_TYPES_ACTIVE__NATIVE_TRACKING_DATA_TYPE_STEPS);
        assertThat(mNativeTrackingStatsCollector.getNativeDataTypesDisabled()).isEmpty();
        assertThat(mNativeTrackingStatsCollector.getStepsReadersCount()).isEqualTo(1);
        assertThat(mNativeTrackingStatsCollector.getStepsWritersCount()).isEqualTo(1);
        assertThat(mNativeTrackingStatsCollector.getNumberOfWrites()).isEqualTo(10);
    }

    @Test
    public void processStats_stepTrackingDisabled_logsCorrectly() {
        when(mTrackerManager.isStepTrackingActive()).thenReturn(false);
        when(mTrackerManager.isStepTrackingExplicitlyDisabled()).thenReturn(true);
        when(mPackageInfoUtils.getPackagesHoldingHealthPermissions(any(), any()))
                .thenReturn(List.of());
        when(mAppInfoHelper.getAppInfoId(anyString())).thenReturn(1L);
        when(mTransactionManager.count(any())).thenReturn(0);

        mNativeTrackingStatsCollector.processStats();

        assertThat(mNativeTrackingStatsCollector.getNativeDataTypesActive()).isEmpty();
        assertThat(mNativeTrackingStatsCollector.getNativeDataTypesDisabled())
                .asList()
                .containsExactly(
                        HEALTH_CONNECT_NATIVE_TRACKING_STATS_REPORTED__NATIVE_DATA_TYPES_DISABLED__NATIVE_TRACKING_DATA_TYPE_STEPS);
        assertThat(mNativeTrackingStatsCollector.getStepsReadersCount()).isEqualTo(0);
        assertThat(mNativeTrackingStatsCollector.getStepsWritersCount()).isEqualTo(0);
        assertThat(mNativeTrackingStatsCollector.getNumberOfWrites()).isEqualTo(0);
    }

    @Test
    public void processStats_noPermissions_logsZeroCounts() {
        when(mTrackerManager.isStepTrackingActive()).thenReturn(false);
        when(mTrackerManager.isStepTrackingExplicitlyDisabled()).thenReturn(false);
        when(mPackageInfoUtils.getPackagesHoldingHealthPermissions(any(), any()))
                .thenReturn(List.of());
        when(mAppInfoHelper.getAppInfoId(anyString())).thenReturn(1L);
        when(mTransactionManager.count(any())).thenReturn(0);

        mNativeTrackingStatsCollector.processStats();

        assertThat(mNativeTrackingStatsCollector.getNativeDataTypesActive()).isEmpty();
        assertThat(mNativeTrackingStatsCollector.getNativeDataTypesDisabled()).isEmpty();
        assertThat(mNativeTrackingStatsCollector.getStepsReadersCount()).isEqualTo(0);
        assertThat(mNativeTrackingStatsCollector.getStepsWritersCount()).isEqualTo(0);
        assertThat(mNativeTrackingStatsCollector.getNumberOfWrites()).isEqualTo(0);
    }

    @Test
    public void processStats_logsErrorUnspecifiedAsPlaceholder() {
        // TODO(b438130821): track and log the last error code.
        when(mTrackerManager.isStepTrackingActive()).thenReturn(false);
        when(mTrackerManager.isStepTrackingExplicitlyDisabled()).thenReturn(false);
        when(mPackageInfoUtils.getPackagesHoldingHealthPermissions(any(), any()))
                .thenReturn(List.of());
        when(mAppInfoHelper.getAppInfoId(anyString())).thenReturn(1L);
        when(mTransactionManager.count(any())).thenReturn(0);

        mNativeTrackingStatsCollector.processStats();

        assertThat(mNativeTrackingStatsCollector.getLastError())
                .isEqualTo(
                        HEALTH_CONNECT_NATIVE_TRACKING_STATS_REPORTED__LAST_ERROR_CODE__NATIVE_TRACKING_ERROR_CODE_UNSPECIFIED);
    }
}
