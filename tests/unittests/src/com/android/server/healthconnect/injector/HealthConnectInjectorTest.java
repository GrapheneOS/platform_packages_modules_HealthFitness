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

package com.android.server.healthconnect.injector;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;

import androidx.test.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.server.healthconnect.fitness.helpers.HealthDataCategoryPriorityHelper;
import com.android.server.healthconnect.permission.HealthPermissionIntentAppsTracker;
import com.android.server.healthconnect.permission.PackageInfoUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class HealthConnectInjectorTest {

    @Mock private PackageInfoUtils mPackageInfoUtils;
    @Mock private HealthDataCategoryPriorityHelper mHealthDataCategoryPriorityHelper;

    // TODO(b/373322447): Remove the mock FirstGrantTimeManager
    @Mock
    private com.android.server.healthconnect.permission.FirstGrantTimeManager
            mFirstGrantTimeManager;

    // TODO(b/373322447): Remove the mock HealthPermissionIntentAppsTracker
    @Mock private HealthPermissionIntentAppsTracker mPermissionIntentAppsTracker;

    private HealthConnectInjectorImpl.Builder mBuilder;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        Context context = InstrumentationRegistry.getContext();
        mBuilder =
                HealthConnectInjectorImpl.newBuilderForTest(context)
                        .setHealthPermissionIntentAppsTracker(mPermissionIntentAppsTracker)
                        .setFirstGrantTimeManager(mFirstGrantTimeManager);
    }

    @Test
    public void setFakePackageInfoUtils_injectorReturnsFakePackageInfoUtils() {
        HealthConnectInjector healthConnectInjector =
                mBuilder.setPackageInfoUtils(mPackageInfoUtils).build();

        assertThat(healthConnectInjector.getPackageInfoUtils()).isEqualTo(mPackageInfoUtils);
    }

    @Test
    public void testProductionInjector_injectorReturnsOriginalPackageInfoUtils() {
        HealthConnectInjector healthConnectInjector = mBuilder.build();

        assertThat(healthConnectInjector.getPackageInfoUtils()).isNotEqualTo(mPackageInfoUtils);
    }

    @Test
    public void
            setFakeHealthDataCategoryPriorityHelper_injectorReturnsFakeHealthDataCategoryPriorityHelper() {
        HealthConnectInjector healthConnectInjector =
                mBuilder.setHealthDataCategoryPriorityHelper(mHealthDataCategoryPriorityHelper)
                        .build();

        assertThat(healthConnectInjector.getHealthDataCategoryPriorityHelper())
                .isEqualTo(mHealthDataCategoryPriorityHelper);
    }

    @Test
    public void testProductionInjector_injectorReturnsOriginalHealthDataCategoryPriorityHelper() {
        HealthConnectInjector healthConnectInjector = mBuilder.build();

        assertThat(healthConnectInjector.getHealthDataCategoryPriorityHelper())
                .isNotEqualTo(mHealthDataCategoryPriorityHelper);
    }
}
