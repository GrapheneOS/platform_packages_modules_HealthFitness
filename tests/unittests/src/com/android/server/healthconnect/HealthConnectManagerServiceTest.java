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

package com.android.server.healthconnect;

import static com.android.healthfitness.flags.Flags.FLAG_ENABLE_HARDWARE_SUPPORT_CHECK;
import static com.android.healthfitness.flags.Flags.FLAG_LATENCY_METRICS_FLAG;
import static com.android.healthfitness.flags.Flags.FLAG_ONBOARDING;
import static com.android.server.healthconnect.backuprestore.BackupRestore.BackupRestoreJobService.BACKUP_RESTORE_JOBS_NAMESPACE;
import static com.android.server.healthconnect.onboarding.OnboardingNotificationJob.ONBOARDING_NOTIFICATION_JOB_NAMESPACE;
import static com.android.server.healthconnect.telemetry.dataquality.DataQualityTelemetryJobScheduler.HC_DATA_QUALITY_TELEMETRY_JOBS_NAMESPACE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.permission.PermissionManager;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.server.SystemService;
import com.android.server.appop.AppOpsManagerLocal;
import com.android.server.healthconnect.device.tracker.TrackerManager;
import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;
import com.android.server.healthconnect.migration.MigrationStateChangeJob;
import com.android.server.healthconnect.permission.PermissionPackageChangesOrchestrator;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
public class HealthConnectManagerServiceTest {

    private static final String HEALTH_CONNECT_DAILY_JOB_NAMESPACE = "HEALTH_CONNECT_DAILY_JOB";
    private static final String HEALTH_CONNECT_IMPORT_EXPORT_JOBS_NAMESPACE =
            "HEALTH_CONNECT_IMPORT_EXPORT_JOBS";
    private static final String ANDROID_SERVER_PACKAGE_NAME = "com.android.server";

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final TemporaryFolder mEnvironmentDataDir = new TemporaryFolder();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Mock private Context mContext;
    @Mock private SystemService.TargetUser mMockTargetUser;
    @Mock private JobScheduler mMainJobScheduler;
    @Mock private JobScheduler mDailyJobScheduler;
    @Mock private JobScheduler mImportExportJobScheduler;
    @Mock private JobScheduler mMigrationJobScheduler;
    @Mock private JobScheduler mBackupRestoreJobScheduler;
    @Mock private JobScheduler mOnboardingNotificationJobScheduler;
    @Mock private JobScheduler mDataQualityTelemetryJobScheduler;
    @Mock private UserManager mUserManager;
    @Mock private PackageManager mPackageManager;
    @Mock private PermissionManager mPermissionManager;
    @Mock private AppOpsManagerLocal mAppOpsManagerLocal;

    @Before
    public void setUp() throws PackageManager.NameNotFoundException {
        when(mMainJobScheduler.forNamespace(HEALTH_CONNECT_DAILY_JOB_NAMESPACE))
                .thenReturn(mDailyJobScheduler);
        when(mMainJobScheduler.forNamespace(MigrationStateChangeJob.class.toString()))
                .thenReturn(mMigrationJobScheduler);
        when(mMainJobScheduler.forNamespace(HEALTH_CONNECT_IMPORT_EXPORT_JOBS_NAMESPACE))
                .thenReturn(mImportExportJobScheduler);
        when(mMainJobScheduler.forNamespace(BACKUP_RESTORE_JOBS_NAMESPACE))
                .thenReturn(mBackupRestoreJobScheduler);
        when(mMainJobScheduler.forNamespace(ONBOARDING_NOTIFICATION_JOB_NAMESPACE))
                .thenReturn(mOnboardingNotificationJobScheduler);
        when(mMainJobScheduler.forNamespace(HC_DATA_QUALITY_TELEMETRY_JOBS_NAMESPACE))
                .thenReturn(mDataQualityTelemetryJobScheduler);

        when(mPackageManager.getPermissionGroupInfo(
                        eq(android.health.connect.HealthPermissions.HEALTH_PERMISSION_GROUP),
                        eq(0)))
                .thenThrow(new PackageManager.NameNotFoundException());
        when(mPackageManager.getPackageInfo(
                        anyString(),
                        eq(PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS))))
                .thenThrow(new PackageManager.NameNotFoundException());
        when(mContext.getSystemService(JobScheduler.class)).thenReturn(mMainJobScheduler);
        when(mContext.getSystemService(UserManager.class)).thenReturn(mUserManager);
        when(mContext.getSystemService(PackageManager.class)).thenReturn(mPackageManager);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mContext.getSystemService(PermissionManager.class)).thenReturn(mPermissionManager);
        when(mContext.getSystemService(AppOpsManagerLocal.class)).thenReturn(mAppOpsManagerLocal);
        when(mContext.getUser()).thenReturn(UserHandle.CURRENT);
        when(mContext.getPackageName()).thenReturn(ANDROID_SERVER_PACKAGE_NAME);
        when(mContext.getDatabasePath(anyString())).thenReturn(mEnvironmentDataDir.getRoot());
        when(mContext.createContextAsUser(any(), anyInt())).thenReturn(mContext);
        when(mMockTargetUser.getUserHandle()).thenReturn(UserHandle.CURRENT);
        when(mContext.getApplicationContext()).thenReturn(mContext);
        HealthConnectInjector.resetInstanceForTest();
    }

    @Test
    public void testCreateService() {
        // Deliberately don't use injector for this test to check that the path where the
        // default constructor is called succeeds.
        HealthConnectManagerService service = new HealthConnectManagerService(mContext);
        assertThat(service).isNotNull();
    }

    @Test
    public void testUserSupport() {
        HealthConnectManagerService service = makeServiceWithTemporaryDir();

        when(mUserManager.isProfile()).thenReturn(true);
        assertThat(service.isUserSupported(mMockTargetUser)).isFalse();
        when(mUserManager.isProfile()).thenReturn(false);
        assertThat(service.isUserSupported(mMockTargetUser)).isTrue();
    }

    @Test
    @EnableFlags({FLAG_ONBOARDING, FLAG_LATENCY_METRICS_FLAG})
    public void testUserSwitch_jobsCancelled() {
        HealthConnectManagerService service = makeServiceWithTemporaryDir();
        when(mUserManager.isUserUnlocked(any(UserHandle.class))).thenReturn(false);

        service.onUserSwitching(mMockTargetUser, mMockTargetUser);

        verify(mDailyJobScheduler).cancelAll();
        verify(mMigrationJobScheduler).cancelAll();
        verify(mOnboardingNotificationJobScheduler).cancelAll();
        verify(mDataQualityTelemetryJobScheduler).cancelAll();
    }

    @Test
    @EnableFlags({FLAG_ONBOARDING, FLAG_LATENCY_METRICS_FLAG})
    public void testUserSwitch_userUnlocked() {
        HealthConnectManagerService service = makeServiceWithTemporaryDir();
        when(mUserManager.isUserUnlocked(any(UserHandle.class))).thenReturn(true);

        service.onUserSwitching(mMockTargetUser, mMockTargetUser);

        verify(mDailyJobScheduler).cancelAll();
        verify(mDailyJobScheduler, timeout(5000)).schedule(any(JobInfo.class));
        verify(mBackupRestoreJobScheduler).cancelAll();
        verify(mOnboardingNotificationJobScheduler).cancelAll();
        verify(mDataQualityTelemetryJobScheduler).cancelAll();
    }

    @Test
    @DisableFlags(FLAG_ONBOARDING)
    public void testUserSwitch_onboardingFlagDisabled_notCancelJob() {
        HealthConnectManagerService service = makeServiceWithTemporaryDir();

        service.onUserSwitching(mMockTargetUser, mMockTargetUser);
        verify(mOnboardingNotificationJobScheduler, never()).cancelAll();
    }

    @Test
    @DisableFlags(FLAG_LATENCY_METRICS_FLAG)
    public void testUserSwitch_latencyFlagDisabled_notCancelJob() {
        HealthConnectManagerService service = makeServiceWithTemporaryDir();

        service.onUserSwitching(mMockTargetUser, mMockTargetUser);
        verify(mDataQualityTelemetryJobScheduler, never()).cancelAll();
    }

    @Test
    public void testUserSwitch_callsClearTracker() {
        HealthConnectInjector injector =
                HealthConnectInjectorImpl.newBuilderForTest(mContext)
                        .setEnvironmentDataDirectory(mEnvironmentDataDir.getRoot())
                        .build();
        HealthConnectInjector spiedInjector = Mockito.spy(injector);
        TrackerManager mockTrackerManager = Mockito.mock(TrackerManager.class);
        Mockito.doReturn(mockTrackerManager).when(spiedInjector).getTrackerManager();
        HealthConnectManagerService service =
                new HealthConnectManagerService(mContext, spiedInjector);

        service.onUserSwitching(mMockTargetUser, mMockTargetUser);

        verify(mockTrackerManager, times(1)).clearTracker();
    }

    @Test
    @EnableFlags({FLAG_ENABLE_HARDWARE_SUPPORT_CHECK})
    public void onStart_flagOn_autoDeviceNotSupported_doesNotStartListeners() {
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)).thenReturn(true);
        HealthConnectInjector injector =
                HealthConnectInjectorImpl.newBuilderForTest(mContext)
                        .setEnvironmentDataDirectory(mEnvironmentDataDir.getRoot())
                        .build();
        HealthConnectInjector spiedInjector = Mockito.spy(injector);
        PermissionPackageChangesOrchestrator orchestrator =
                Mockito.mock(PermissionPackageChangesOrchestrator.class);
        Mockito.doReturn(orchestrator)
                .when(spiedInjector)
                .getPermissionPackageChangesOrchestrator();

        HealthConnectManagerService service = makeServiceWithSpy(mContext, spiedInjector);
        service.onStart();

        verify(orchestrator, never()).registerBroadcastReceiver(any());
    }

    @Test
    @EnableFlags({FLAG_ENABLE_HARDWARE_SUPPORT_CHECK})
    public void onStart_flagOn_tvDeviceNotSupported_doesNotStartListeners() {
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)).thenReturn(true);
        HealthConnectInjector injector =
                HealthConnectInjectorImpl.newBuilderForTest(mContext)
                        .setEnvironmentDataDirectory(mEnvironmentDataDir.getRoot())
                        .build();
        HealthConnectInjector spiedInjector = Mockito.spy(injector);
        PermissionPackageChangesOrchestrator orchestrator =
                Mockito.mock(PermissionPackageChangesOrchestrator.class);
        Mockito.doReturn(orchestrator)
                .when(spiedInjector)
                .getPermissionPackageChangesOrchestrator();

        HealthConnectManagerService service = makeServiceWithSpy(mContext, spiedInjector);
        service.onStart();

        verify(orchestrator, never()).registerBroadcastReceiver(any());
    }

    @Test
    @EnableFlags({FLAG_ENABLE_HARDWARE_SUPPORT_CHECK})
    public void onStart_flagOn_embeddedDeviceNotSupported_doesNotStartListeners() {
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_EMBEDDED)).thenReturn(true);
        HealthConnectInjector injector =
                HealthConnectInjectorImpl.newBuilderForTest(mContext)
                        .setEnvironmentDataDirectory(mEnvironmentDataDir.getRoot())
                        .build();
        HealthConnectInjector spiedInjector = Mockito.spy(injector);
        PermissionPackageChangesOrchestrator orchestrator =
                Mockito.mock(PermissionPackageChangesOrchestrator.class);
        Mockito.doReturn(orchestrator)
                .when(spiedInjector)
                .getPermissionPackageChangesOrchestrator();

        HealthConnectManagerService service = makeServiceWithSpy(mContext, spiedInjector);
        service.onStart();

        verify(orchestrator, never()).registerBroadcastReceiver(any());
    }

    @Test
    @EnableFlags({FLAG_ENABLE_HARDWARE_SUPPORT_CHECK})
    public void onStart_flagOn_deviceSupported_startsListeners() {
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)).thenReturn(false);
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)).thenReturn(false);
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_EMBEDDED)).thenReturn(false);

        HealthConnectInjector injector =
                HealthConnectInjectorImpl.newBuilderForTest(mContext)
                        .setEnvironmentDataDirectory(mEnvironmentDataDir.getRoot())
                        .build();
        HealthConnectInjector spiedInjector = Mockito.spy(injector);
        PermissionPackageChangesOrchestrator orchestrator =
                Mockito.mock(PermissionPackageChangesOrchestrator.class);
        Mockito.doReturn(orchestrator)
                .when(spiedInjector)
                .getPermissionPackageChangesOrchestrator();

        HealthConnectManagerService service = makeServiceWithSpy(mContext, spiedInjector);
        service.onStart();

        verify(orchestrator).registerBroadcastReceiver(mContext);
    }

    @Test
    @DisableFlags({FLAG_ENABLE_HARDWARE_SUPPORT_CHECK})
    public void onStart_flagOff_deviceNotSupported_stillStartsListeners() {
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE))
                .thenReturn(true); // Device NOT supported

        HealthConnectInjector injector =
                HealthConnectInjectorImpl.newBuilderForTest(mContext)
                        .setEnvironmentDataDirectory(mEnvironmentDataDir.getRoot())
                        .build();
        HealthConnectInjector spiedInjector = Mockito.spy(injector);
        PermissionPackageChangesOrchestrator orchestrator =
                Mockito.mock(PermissionPackageChangesOrchestrator.class);
        Mockito.doReturn(orchestrator)
                .when(spiedInjector)
                .getPermissionPackageChangesOrchestrator();

        HealthConnectManagerService service = makeServiceWithSpy(mContext, spiedInjector);
        service.onStart();

        // Verification: Listeners SHOULD start because the flag is OFF
        verify(orchestrator).registerBroadcastReceiver(mContext);
    }

    @Test
    @DisableFlags({FLAG_ENABLE_HARDWARE_SUPPORT_CHECK})
    public void onStart_flagOff_deviceSupported_startsListeners() {
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE))
                .thenReturn(false); // Device supported

        HealthConnectInjector injector =
                HealthConnectInjectorImpl.newBuilderForTest(mContext)
                        .setEnvironmentDataDirectory(mEnvironmentDataDir.getRoot())
                        .build();
        HealthConnectInjector spiedInjector = Mockito.spy(injector);
        PermissionPackageChangesOrchestrator orchestrator =
                Mockito.mock(PermissionPackageChangesOrchestrator.class);
        Mockito.doReturn(orchestrator)
                .when(spiedInjector)
                .getPermissionPackageChangesOrchestrator();

        HealthConnectManagerService service = makeServiceWithSpy(mContext, spiedInjector);
        service.onStart();

        // Verification: Listeners SHOULD start because the flag is OFF
        verify(orchestrator).registerBroadcastReceiver(mContext);
    }

    @Test
    @EnableFlags({FLAG_ENABLE_HARDWARE_SUPPORT_CHECK})
    public void onUserUnlocked_flagOn_deviceNotSupported_noOps() {
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)).thenReturn(true);
        HealthConnectManagerService service = makeServiceWithTemporaryDir();

        service.onUserUnlocked(mMockTargetUser);

        verify(mDailyJobScheduler, never()).schedule(any());
    }

    @Test
    @EnableFlags({FLAG_ENABLE_HARDWARE_SUPPORT_CHECK})
    public void onUserSwitching_flagOn_deviceNotSupported_noOps() {
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)).thenReturn(true);
        HealthConnectManagerService service = makeServiceWithTemporaryDir();

        service.onUserSwitching(mMockTargetUser, mMockTargetUser);

        verify(mDailyJobScheduler, never()).cancelAll();
    }

    private HealthConnectManagerService makeServiceWithTemporaryDir() {
        HealthConnectInjector injector =
                HealthConnectInjectorImpl.newBuilderForTest(mContext)
                        .setEnvironmentDataDirectory(mEnvironmentDataDir.getRoot())
                        .build();
        return new HealthConnectManagerService(mContext, injector);
    }

    private HealthConnectManagerService makeServiceWithSpy(
            Context context, HealthConnectInjector injector) {
        return new HealthConnectManagerService(context, injector) {
            @Override
            public void publishService(String name, android.os.IBinder service) {
                // Do nothing
            }
        };
    }
}
