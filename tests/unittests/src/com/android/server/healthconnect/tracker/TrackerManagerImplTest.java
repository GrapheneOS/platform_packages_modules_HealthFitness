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

package com.android.server.healthconnect.tracker;

import static com.android.healthfitness.flags.Flags.FLAG_STEP_TRACKING_ENABLED;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.health.connect.HealthPermissions;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.List;

/** Unit tests for {@link TrackerManagerImpl} */
@RunWith(AndroidJUnit4.class)
public class TrackerManagerImplTest {

    private static final String TEST_PACKAGE_NAME = "com.test.app";

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock private Context mContext;
    @Mock private PackageManager mPackageManager;

    @Before
    public void setup() throws PackageManager.NameNotFoundException {
        mContext = spy(InstrumentationRegistry.getInstrumentation().getContext());
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
    }

    @After
    public void tearDown() throws Exception {
        reset(mContext, mPackageManager);
    }

    @Test
    @EnableFlags({FLAG_STEP_TRACKING_ENABLED})
    public void stepTrackingEnabled_initialize_doesNotThrow() {
        TrackerManager manager = new TrackerManagerImpl();
        manager.initialize();
    }

    @Test
    @DisableFlags({FLAG_STEP_TRACKING_ENABLED})
    public void stepTrackingDisabled_initialize_doesNotThrow() {
        TrackerManager manager = new TrackerManagerImpl();
        manager.initialize();
    }

    @Test
    @EnableFlags({FLAG_STEP_TRACKING_ENABLED})
    public void stepTrackingEnabled_setStepTrackingEnabled_doesNotThrow() {
        TrackerManager manager = new TrackerManagerImpl();
        manager.setStepTrackingEnabled(true);
        manager.setStepTrackingEnabled(false);
    }

    @Test
    @DisableFlags({FLAG_STEP_TRACKING_ENABLED})
    public void stepTrackingDisabled_setStepTrackingEnabled_doesNotThrow() {
        TrackerManager manager = new TrackerManagerImpl();
        manager.setStepTrackingEnabled(true);
        manager.setStepTrackingEnabled(false);
    }

    @Test
    public void noAppsGrantedReadSteps_noPackagesReturned() {
        List<String> packages = TrackerManagerImpl.packagesEligibleForStepTracking(mContext);
        mockInstallAndGrantPermissions(List.of());

        assertThat(packages).isEmpty();
    }

    @Test
    public void appGrantedReadStepsPermission_appReturnedInList() {
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.packageName = TEST_PACKAGE_NAME;
        mockInstallAndGrantPermissions(List.of(packageInfo));

        List<String> packages = TrackerManagerImpl.packagesEligibleForStepTracking(mContext);

        assertThat(packages).containsExactly(TEST_PACKAGE_NAME);
    }

    private void mockInstallAndGrantPermissions(List<PackageInfo> packageInfos) {
        when(mPackageManager.getPackagesHoldingPermissions(
                        eq(new String[] {HealthPermissions.READ_STEPS}),
                        argThat(flag -> (flag.getValue() == 0))))
                .thenReturn(packageInfos);
    }
}
