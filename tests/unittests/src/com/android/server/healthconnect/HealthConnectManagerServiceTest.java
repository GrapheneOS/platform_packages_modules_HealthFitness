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

import static com.android.server.healthconnect.backuprestore.BackupRestore.BackupRestoreJobService.BACKUP_RESTORE_JOBS_NAMESPACE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.job.JobScheduler;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.os.UserHandle;
import android.os.UserManager;
import android.permission.PermissionManager;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.server.SystemService;
import com.android.server.appop.AppOpsManagerLocal;
import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;
import com.android.server.healthconnect.migration.MigrationStateChangeJob;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
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

    @Mock Context mContext;
    @Mock private SystemService.TargetUser mMockTargetUser;
    @Mock private JobScheduler mMainJobScheduler;
    @Mock private JobScheduler mDailyJobScheduler;
    @Mock private JobScheduler mImportExportJobScheduler;
    @Mock private JobScheduler mMigrationJobScheduler;
    @Mock private JobScheduler mBackupRestoreJobScheduler;
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
        PermissionGroupInfo permissionGroupInfo = new PermissionGroupInfo();
        permissionGroupInfo.packageName = "test";
        PackageInfo mockPackageInfo = new PackageInfo();
        mockPackageInfo.permissions = new PermissionInfo[1];
        mockPackageInfo.permissions[0] = new PermissionInfo();

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
    public void testUserSwitch_userLocked() {
        HealthConnectManagerService service = makeServiceWithTemporaryDir();
        when(mUserManager.isUserUnlocked(any())).thenReturn(false);

        service.onUserSwitching(mMockTargetUser, mMockTargetUser);

        verify(mDailyJobScheduler, times(1)).cancelAll();
        verify(mMigrationJobScheduler, times(1)).cancelAll();
    }

    @Test
    public void testUserSwitch_userUnlocked() {
        HealthConnectManagerService service = makeServiceWithTemporaryDir();
        when(mUserManager.isUserUnlocked(any())).thenReturn(true);

        service.onUserSwitching(mMockTargetUser, mMockTargetUser);

        verify(mDailyJobScheduler, times(1)).cancelAll();
        verify(mDailyJobScheduler, timeout(5000).times(1)).schedule(any());
        verify(mBackupRestoreJobScheduler, times(1)).cancelAll();
    }

    private HealthConnectManagerService makeServiceWithTemporaryDir() {
        HealthConnectInjector injector =
                HealthConnectInjectorImpl.newBuilderForTest(mContext)
                        .setEnvironmentDataDirectory(mEnvironmentDataDir.getRoot())
                        .build();
        return new HealthConnectManagerService(mContext, injector);
    }
}
