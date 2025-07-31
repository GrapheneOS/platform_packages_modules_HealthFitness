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

package com.android.server.healthconnect.telemetry;

import static com.android.server.healthconnect.telemetry.TelemetryJobService.EXTRA_USER_ID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.job.JobParameters;
import android.content.Context;
import android.os.PersistableBundle;
import android.os.UserHandle;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.runner.AndroidJUnit4;

import com.android.server.healthconnect.HealthConnectThreadScheduler;
import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;
import com.android.server.healthconnect.permission.FirstGrantTimeManager;
import com.android.server.healthconnect.permission.HealthPermissionIntentAppsTracker;
import com.android.server.healthconnect.telemetry.dataquality.LatencyMetricsLogger;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
public class TelemetryJobServiceTest {
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Mock private JobParameters mJobParameters;
    @Mock private HealthConnectThreadScheduler mHealthConnectThreadScheduler;
    @Mock private LatencyMetricsLogger mLatencyMetricsLogger;

    private TelemetryJobService mTelemetryJobService;
    private static final UserHandle USER_HANDLE = UserHandle.of(1);
    private static final int USER_ID = USER_HANDLE.getIdentifier();

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        MockitoAnnotations.initMocks(this);

        mTelemetryJobService = Mockito.spy(new TelemetryJobService());
        doCallRealMethod().when(mTelemetryJobService).onStartJob(any(JobParameters.class));
        doCallRealMethod().when(mTelemetryJobService).onStopJob(any());
        doNothing().when(mTelemetryJobService).jobFinished(any(), anyBoolean());

        HealthConnectInjector.resetInstanceForTest();
        HealthConnectInjector.setInstance(
                HealthConnectInjectorImpl.newBuilderForTest(context)
                        .setFirstGrantTimeManager(mock(FirstGrantTimeManager.class))
                        .setHealthPermissionIntentAppsTracker(
                                mock(HealthPermissionIntentAppsTracker.class))
                        .setThreadScheduler(mHealthConnectThreadScheduler)
                        .setLatencyMetricsLogger(mLatencyMetricsLogger)
                        .build());
        doAnswer(
                        invocation -> {
                            Runnable task = invocation.getArgument(0);
                            task.run();
                            return null;
                        })
                .when(mHealthConnectThreadScheduler)
                .scheduleInternalTask(any(Runnable.class));
    }

    @Test
    public void onStartJob_noUser_returnsFalse() {
        when(mJobParameters.getExtras()).thenReturn(new PersistableBundle());
        assertFalse(mTelemetryJobService.onStartJob(mJobParameters));
    }

    @Test
    public void onStartJob_nonActiveUser_returnsFalse() {
        PersistableBundle bundle = new PersistableBundle();
        bundle.putInt(EXTRA_USER_ID, USER_ID + 1);
        when(mJobParameters.getExtras()).thenReturn(bundle);
        TelemetryJobService.setCurrentUser(USER_HANDLE);
        assertFalse(mTelemetryJobService.onStartJob(mJobParameters));
    }

    @Test
    public void onStartJob_validUser_schedulesJobAndReturnsTrue() {
        PersistableBundle bundle = new PersistableBundle();
        bundle.putInt(EXTRA_USER_ID, USER_ID);
        when(mJobParameters.getExtras()).thenReturn(bundle);
        doNothing().when(mTelemetryJobService).jobFinished(any(), anyBoolean());
        TelemetryJobService.setCurrentUser(USER_HANDLE);

        assertTrue(mTelemetryJobService.onStartJob(mJobParameters));
        verify(mHealthConnectThreadScheduler).scheduleInternalTask(any(Runnable.class));
        verify(mLatencyMetricsLogger).log();
        verify(mTelemetryJobService).jobFinished(eq(mJobParameters), eq(false));
    }

    @Test
    public void onStopJob_returnsFalse() {
        assertFalse(mTelemetryJobService.onStopJob(mJobParameters));
    }
}
