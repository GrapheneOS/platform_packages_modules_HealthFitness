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

package com.android.server.healthconnect.device.tracker;

import static com.android.healthfitness.flags.Flags.FLAG_STEP_TRACKING_ENABLED;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.health.connect.HealthPermissions;
import android.os.Build;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.modules.utils.testing.ExtendedMockitoRule;
import com.android.server.healthconnect.HealthConnectThreadScheduler;
import com.android.server.healthconnect.device.DeviceDataSourcesHelper;
import com.android.server.healthconnect.device.DeviceRecordHelper;
import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;
import com.android.server.healthconnect.permission.FirstGrantTimeManager;
import com.android.server.healthconnect.permission.HealthConnectPermissionHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.quality.Strictness;

import java.util.List;

/** Unit tests for {@link TrackerManagerImpl} */
@RunWith(AndroidJUnit4.class)
public class TrackerManagerImplTest {

    private static final String TEST_PACKAGE_NAME = "com.test.app";

    @Rule
    public final ExtendedMockitoRule mExtendedMockitoRule =
            new ExtendedMockitoRule.Builder(this)
                    .mockStatic(Build.class)
                    .setStrictness(Strictness.LENIENT)
                    .build();

    @Mock private Context mContext;
    @Mock private PackageManager mPackageManager;
    @Mock private HealthConnectPermissionHelper mPermissionHelper;

    private HealthConnectThreadScheduler mThreadScheduler;
    private DeviceRecordHelper mDeviceRecordHelper;
    private DeviceDataSourcesHelper mDeviceDataSourcesHelper;

    @Before
    public void setup() throws PackageManager.NameNotFoundException {
        mContext = spy(InstrumentationRegistry.getInstrumentation().getContext());
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(Build.getSerial()).thenReturn("TEST_SERIAL_NUMBER");
        HealthConnectInjector healthConnectInjector =
                HealthConnectInjectorImpl.newBuilderForTest(mContext)
                        .setFirstGrantTimeManager(mock(FirstGrantTimeManager.class))
                        .build();
        mThreadScheduler = healthConnectInjector.getThreadScheduler();
        mDeviceRecordHelper = healthConnectInjector.getDeviceRecordHelper();
        mDeviceDataSourcesHelper = healthConnectInjector.getDeviceDataSourcesHelper();
    }

    @After
    public void tearDown() throws Exception {
        reset(mContext, mPackageManager);
    }

    @Test
    @EnableFlags({FLAG_STEP_TRACKING_ENABLED})
    public void stepTrackingEnabled_initialize_doesNotThrow() {
        TrackerManager manager =
                new TrackerManagerImpl(
                        mContext,
                        mPermissionHelper,
                        mThreadScheduler,
                        mDeviceRecordHelper,
                        mDeviceDataSourcesHelper);
        manager.initialize();
    }

    @Test
    @DisableFlags({FLAG_STEP_TRACKING_ENABLED})
    public void stepTrackingDisabled_initialize_doesNotThrow() {
        TrackerManager manager =
                new TrackerManagerImpl(
                        mContext,
                        mPermissionHelper,
                        mThreadScheduler,
                        mDeviceRecordHelper,
                        mDeviceDataSourcesHelper);
        manager.initialize();
    }

    @Test
    @EnableFlags({FLAG_STEP_TRACKING_ENABLED})
    public void stepTrackingEnabled_setStepTrackingEnabled_doesNotThrow() {
        TrackerManager manager =
                new TrackerManagerImpl(
                        mContext,
                        mPermissionHelper,
                        mThreadScheduler,
                        mDeviceRecordHelper,
                        mDeviceDataSourcesHelper);
        manager.setStepTrackingEnabled(true);
        manager.setStepTrackingEnabled(false);
    }

    @Test
    @DisableFlags({FLAG_STEP_TRACKING_ENABLED})
    public void stepTrackingDisabled_setStepTrackingEnabled_doesNotThrow() {
        TrackerManager manager =
                new TrackerManagerImpl(
                        mContext,
                        mPermissionHelper,
                        mThreadScheduler,
                        mDeviceRecordHelper,
                        mDeviceDataSourcesHelper);
        manager.setStepTrackingEnabled(true);
        manager.setStepTrackingEnabled(false);
    }

    @Test
    public void noAppsGrantedReadSteps_noPackagesReturned() {
        List<String> packages =
                TrackerManagerImpl.packagesEligibleForStepTracking(mContext, mPermissionHelper);
        mockInstallAndGrantPermissions(List.of());

        assertThat(packages).isEmpty();
    }

    @Test
    public void appGrantedReadStepsPermission_isReturnedInList() {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = TEST_PACKAGE_NAME;
        mockInstallAndGrantPermissions(List.of(packageInfo));

        List<String> packages =
                TrackerManagerImpl.packagesEligibleForStepTracking(mContext, mPermissionHelper);

        assertThat(packages).containsExactly(TEST_PACKAGE_NAME);
    }

    @Test
    public void appPregrantedReadStepsPermission_notReturnedInList() {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = TEST_PACKAGE_NAME;
        mockInstallAndGrantPermissions(List.of(packageInfo));
        setAsPregrantedApp(TEST_PACKAGE_NAME);

        List<String> packages =
                TrackerManagerImpl.packagesEligibleForStepTracking(mContext, mPermissionHelper);

        assertThat(packages).isEmpty();
    }

    private void mockInstallAndGrantPermissions(List<PackageInfo> packageInfos) {
        when(mPackageManager.getPackagesHoldingPermissions(
                        eq(new String[] {HealthPermissions.READ_STEPS}),
                        argThat(flag -> (flag.getValue() == 0))))
                .thenReturn(packageInfos);
    }

    private void setAsPregrantedApp(String packageName) {
        when(mPermissionHelper.getHealthPermissionFlags(
                        eq(packageName), any(), eq(HealthPermissions.READ_STEPS)))
                .thenReturn(PackageManager.FLAG_PERMISSION_GRANTED_BY_DEFAULT);
    }
}
