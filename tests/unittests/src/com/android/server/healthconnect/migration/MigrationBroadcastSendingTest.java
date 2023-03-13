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

package com.android.server.healthconnect.migration;

import static junit.framework.Assert.fail;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.health.connect.HealthConnectManager;
import android.os.UserHandle;
import android.os.UserManager;

import androidx.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.verification.VerificationMode;

import java.util.ArrayList;

/** Unit tests for broadcast sending logic in {@link MigrationBroadcast} */
@RunWith(AndroidJUnit4.class)
public class MigrationBroadcastSendingTest {
    private static final String[] PERMISSIONS_TO_CHECK =
            new String[] {Manifest.permission.MIGRATE_HEALTH_CONNECT_DATA};
    private static final String MOCK_CONFIGURED_PACKAGE = "com.configured.app";
    private static final String MOCK_UNCONFIGURED_PACKAGE_ONE = "com.unconfigured.app";
    private static final String MOCK_UNCONFIGURED_PACKAGE_TWO = "com.unconfigured.apptwo";

    @Mock private Context mContext;
    @Mock private Context mUserContext;
    @Mock private PackageManager mPackageManager;
    @Mock private PackageManager mUserContextPackageManager;
    @Mock private Resources mResources;
    @Mock private UserHandle mUser;
    @Mock private UserManager mUserManager;

    private MigrationBroadcast mMigrationBroadcast;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        when(mContext.getResources()).thenReturn(mResources);
        when(mResources.getIdentifier(
                        eq("config_healthConnectMigratorPackageName"), eq("string"), eq("android")))
                .thenReturn(1);
        when(mResources.getString(anyInt())).thenReturn(MOCK_CONFIGURED_PACKAGE);

        when(mContext.getPackageManager()).thenReturn(mPackageManager);

        when(mContext.createContextAsUser(eq(mUser), eq(0))).thenReturn(mUserContext);
        when(mUserContext.getPackageManager()).thenReturn(mUserContextPackageManager);

        when(mContext.getSystemService(UserManager.class)).thenReturn(mUserManager);

        mMigrationBroadcast = new MigrationBroadcast(mContext, mUser);
    }

    @Test
    public void testSendInvocationBroadcast_noPermissionMatchingApps_noBroadcastSent()
            throws Exception {
        when(mPackageManager.getPackagesHoldingPermissions(
                        eq(PERMISSIONS_TO_CHECK), argThat(flag -> (flag.getValue() == 0))))
                .thenReturn(new ArrayList<PackageInfo>());

        mMigrationBroadcast.sendInvocationBroadcast();

        verifyInvocations(never());
    }

    @Test
    public void testSendInvocationBroadcast_noIntentMatchingApps_noBroadcastSent()
            throws Exception {
        ArrayList<PackageInfo> packageInfoArray = createPackageInfoArray(MOCK_CONFIGURED_PACKAGE);
        when(mPackageManager.getPackagesHoldingPermissions(
                        eq(PERMISSIONS_TO_CHECK), argThat(flag -> (flag.getValue() == 0))))
                .thenReturn(packageInfoArray);
        getResolveActivityResult(null);

        mMigrationBroadcast.sendInvocationBroadcast();

        verifyInvocations(never());
    }

    @Test
    public void
            testSendInvocationBroadcast_oneMigrationAwareConfiguredAppInstalledUserRunning_broadcastSentToConfiguredApp()
                    throws Exception {
        ArrayList<PackageInfo> packageInfoArray = createPackageInfoArray(MOCK_CONFIGURED_PACKAGE);
        when(mPackageManager.getPackagesHoldingPermissions(
                        eq(PERMISSIONS_TO_CHECK), argThat(flag -> (flag.getValue() == 0))))
                .thenReturn(packageInfoArray);
        getResolveActivityResult(new ResolveInfo());
        when(mUserContextPackageManager.getPackageInfo(
                        anyString(), argThat(flag -> (flag.getValue() == 0))))
                .thenReturn(new PackageInfo());
        when(mUserManager.isUserRunning(eq(mUser))).thenReturn(true);

        mMigrationBroadcast.sendInvocationBroadcast();

        verifyInvocations(times(1));
    }

    @Test
    public void
            testSendInvocationBroadcast_oneMigrationAwareConfiguredAppInstalledUserNotRunning_noBroadcastSent()
                    throws Exception {
        ArrayList<PackageInfo> packageInfoArray = createPackageInfoArray(MOCK_CONFIGURED_PACKAGE);
        when(mPackageManager.getPackagesHoldingPermissions(
                        eq(PERMISSIONS_TO_CHECK), argThat(flag -> (flag.getValue() == 0))))
                .thenReturn(packageInfoArray);
        getResolveActivityResult(new ResolveInfo());
        when(mUserContextPackageManager.getPackageInfo(
                        anyString(), argThat(flag -> (flag.getValue() == 0))))
                .thenReturn(new PackageInfo());
        when(mUserManager.isUserRunning(eq(mUser))).thenReturn(false);

        mMigrationBroadcast.sendInvocationBroadcast();

        verifyInvocations(never());
    }

    @Test
    public void
            testSendInvocationBroadcast_oneMigrationAwareConfiguredAppNotInstalledOnUser_noBroadcastSent()
                    throws Exception {
        ArrayList<PackageInfo> packageInfoArray = createPackageInfoArray(MOCK_CONFIGURED_PACKAGE);
        when(mPackageManager.getPackagesHoldingPermissions(
                        eq(PERMISSIONS_TO_CHECK), argThat(flag -> (flag.getValue() == 0))))
                .thenReturn(packageInfoArray);
        getResolveActivityResult(new ResolveInfo());
        when(mUserContextPackageManager.getPackageInfo(
                        anyString(), argThat(flag -> (flag.getValue() == 0))))
                .thenThrow(PackageManager.NameNotFoundException.class);

        mMigrationBroadcast.sendInvocationBroadcast();

        verifyInvocations(never());
    }

    @Test(expected = Exception.class)
    public void testSendInvocationBroadcast_oneMigrationAwareNotConfiguredApp_exceptionThrown()
            throws Exception {
        ArrayList<PackageInfo> packageInfoArray =
                createPackageInfoArray(MOCK_UNCONFIGURED_PACKAGE_ONE);
        when(mPackageManager.getPackagesHoldingPermissions(
                        eq(PERMISSIONS_TO_CHECK), argThat(flag -> (flag.getValue() == 0))))
                .thenReturn(packageInfoArray);
        getResolveActivityResult(new ResolveInfo());

        try {
            mMigrationBroadcast.sendInvocationBroadcast();
            fail("Expected Exception");
        } finally {
            verifyInvocations(never());
        }
    }

    @Test
    public void
            testSendInvocationBroadcast_multipleAppsIncludingConfiguredAppInstalledUserRunning_broadcastSentToConfiguredApp()
                    throws Exception {
        ArrayList<PackageInfo> packageInfoArray =
                createPackageInfoArray(MOCK_CONFIGURED_PACKAGE, MOCK_UNCONFIGURED_PACKAGE_ONE);
        when(mPackageManager.getPackagesHoldingPermissions(
                        eq(PERMISSIONS_TO_CHECK), argThat(flag -> (flag.getValue() == 0))))
                .thenReturn(packageInfoArray);
        getResolveActivityResult(new ResolveInfo());
        when(mUserContextPackageManager.getPackageInfo(
                        anyString(), argThat(flag -> (flag.getValue() == 0))))
                .thenReturn(new PackageInfo());
        when(mUserManager.isUserRunning(eq(mUser))).thenReturn(true);

        mMigrationBroadcast.sendInvocationBroadcast();

        verifyInvocations(times(1));
    }

    @Test
    public void
            testSendInvocationBroadcast_multipleAppsIncludingConfiguredAppInstalledUserNotRunning_noBroadcastSent()
                    throws Exception {
        ArrayList<PackageInfo> packageInfoArray =
                createPackageInfoArray(MOCK_CONFIGURED_PACKAGE, MOCK_UNCONFIGURED_PACKAGE_ONE);
        when(mPackageManager.getPackagesHoldingPermissions(
                        eq(PERMISSIONS_TO_CHECK), argThat(flag -> (flag.getValue() == 0))))
                .thenReturn(packageInfoArray);
        getResolveActivityResult(new ResolveInfo());
        when(mUserContextPackageManager.getPackageInfo(
                        anyString(), argThat(flag -> (flag.getValue() == 0))))
                .thenReturn(new PackageInfo());
        when(mUserManager.isUserRunning(eq(mUser))).thenReturn(false);

        mMigrationBroadcast.sendInvocationBroadcast();

        verifyInvocations(never());
    }

    @Test
    public void
            testSendInvocationBroadcast_multipleAppsIncludingConfiguredAppNotInstalledOnUser_noBroadcastSent()
                    throws Exception {
        ArrayList<PackageInfo> packageInfoArray =
                createPackageInfoArray(MOCK_CONFIGURED_PACKAGE, MOCK_UNCONFIGURED_PACKAGE_ONE);
        when(mPackageManager.getPackagesHoldingPermissions(
                        eq(PERMISSIONS_TO_CHECK), argThat(flag -> (flag.getValue() == 0))))
                .thenReturn(packageInfoArray);
        getResolveActivityResult(new ResolveInfo());
        when(mUserContextPackageManager.getPackageInfo(
                        anyString(), argThat(flag -> (flag.getValue() == 0))))
                .thenThrow(PackageManager.NameNotFoundException.class);

        mMigrationBroadcast.sendInvocationBroadcast();

        verifyInvocations(never());
    }

    @Test(expected = Exception.class)
    public void testSendInvocationBroadcast_multipleAppsExcludingConfiguredApp_exceptionThrown()
            throws Exception {
        ArrayList<PackageInfo> packageInfoArray =
                createPackageInfoArray(
                        MOCK_UNCONFIGURED_PACKAGE_ONE, MOCK_UNCONFIGURED_PACKAGE_TWO);
        when(mPackageManager.getPackagesHoldingPermissions(
                        eq(PERMISSIONS_TO_CHECK), argThat(flag -> (flag.getValue() == 0))))
                .thenReturn(packageInfoArray);
        getResolveActivityResult(new ResolveInfo());

        try {
            mMigrationBroadcast.sendInvocationBroadcast();
            fail("Expected Exception");
        } finally {
            verifyInvocations(never());
        }
    }

    private ArrayList<PackageInfo> createPackageInfoArray(String... packageNames) {
        ArrayList<PackageInfo> packageInfoArray = new ArrayList<PackageInfo>();
        for (String packageName : packageNames) {
            PackageInfo packageInfo = new PackageInfo();
            packageInfo.packageName = packageName;
            packageInfoArray.add(packageInfo);
        }
        return packageInfoArray;
    }

    private void getResolveActivityResult(ResolveInfo result) {
        when(mPackageManager.resolveActivity(
                        argThat(
                                intent ->
                                        (HealthConnectManager.ACTION_SHOW_MIGRATION_INFO.equals(
                                                intent.getAction()))),
                        argThat(flag -> (flag.getValue() == PackageManager.MATCH_ALL))))
                .thenReturn(result);
    }

    private void verifyInvocations(VerificationMode verificationMode) {
        verify(mContext, verificationMode)
                .sendBroadcastAsUser(
                        argThat(
                                intent ->
                                        (HealthConnectManager.ACTION_HEALTH_CONNECT_MIGRATION_READY
                                                        .equals(intent.getAction())
                                                && (MOCK_CONFIGURED_PACKAGE.equals(
                                                        intent.getPackage())))),
                        eq(mUser));
    }
}
