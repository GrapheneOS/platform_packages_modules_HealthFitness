/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.server.healthconnect.storage.datatypehelpers;

import static com.android.server.healthconnect.testing.TestUtils.TEST_USER;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;
import com.android.server.healthconnect.permission.FirstGrantTimeManager;
import com.android.server.healthconnect.permission.HealthPermissionIntentAppsTracker;
import com.android.server.healthconnect.testing.storage.TransactionTestUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
public class AppInfoHelperTest {

    private static final String TEST_PACKAGE_NAME = "test.package.name";
    private static final String TEST_APP_NAME = "testAppName";

    @Rule public final TemporaryFolder mEnvironmentDataDir = new TemporaryFolder();
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock private Context mContext;
    @Mock private Drawable mDrawable;
    @Mock private PackageManager mPackageManager;

    private AppInfoHelper mAppInfoHelper;
    private TransactionTestUtils mTransactionTestUtils;

    @Before
    public void setup() throws PackageManager.NameNotFoundException {
        when(mContext.getUser()).thenReturn(TEST_USER);
        when(mContext.createContextAsUser(any(), anyInt())).thenReturn(mContext);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);

        when(mPackageManager.getDefaultActivityIcon()).thenReturn(mDrawable);

        when(mDrawable.getIntrinsicHeight()).thenReturn(200);
        when(mDrawable.getIntrinsicWidth()).thenReturn(200);

        HealthConnectInjector healthConnectInjector =
                HealthConnectInjectorImpl.newBuilderForTest(mContext)
                        .setFirstGrantTimeManager(mock(FirstGrantTimeManager.class))
                        .setHealthPermissionIntentAppsTracker(
                                mock(HealthPermissionIntentAppsTracker.class))
                        .setAppOpLogsHelper(mock(AppOpLogsHelper.class))
                        .setEnvironmentDataDirectory(mEnvironmentDataDir.getRoot())
                        .build();
        mAppInfoHelper = healthConnectInjector.getAppInfoHelper();
        mTransactionTestUtils = new TransactionTestUtils(healthConnectInjector);
    }

    @After
    public void tearDown() throws Exception {
        reset(mDrawable, mContext, mPackageManager);
    }

    @Test
    public void testUpdateAppInfoIfNotInstalled_withoutIcon_getIconFromPackageName()
            throws PackageManager.NameNotFoundException {
        setAppAsNotInstalled();
        mTransactionTestUtils.insertApp(TEST_PACKAGE_NAME);

        mAppInfoHelper.updateAppInfoIfNotInstalled(TEST_PACKAGE_NAME, TEST_APP_NAME, null);

        verify(mPackageManager).getApplicationIcon(TEST_PACKAGE_NAME);
        assertThat(mAppInfoHelper.getAppInfoMap().get(TEST_PACKAGE_NAME).getName())
                .isEqualTo(TEST_APP_NAME);
    }

    @Test
    public void testUpdateAppInfoIfNotInstalled_withoutIcon_getDefaultIconIfPackageIsNotFound()
            throws PackageManager.NameNotFoundException {
        setAppAsNotInstalled();
        mTransactionTestUtils.insertApp(TEST_PACKAGE_NAME);

        mAppInfoHelper.updateAppInfoIfNotInstalled(TEST_PACKAGE_NAME, TEST_APP_NAME, null);

        verify(mPackageManager).getDefaultActivityIcon();
        assertThat(mAppInfoHelper.getAppInfoMap().get(TEST_PACKAGE_NAME).getName())
                .isEqualTo(TEST_APP_NAME);
    }

    @Test
    public void testUpdateAppInfoIfNotInstalled_appInstalled_noChangeMade()
            throws PackageManager.NameNotFoundException {
        setAppAsInstalled();
        mTransactionTestUtils.insertApp(TEST_PACKAGE_NAME);

        mAppInfoHelper.updateAppInfoIfNotInstalled(TEST_PACKAGE_NAME, TEST_APP_NAME, null);

        verify(mPackageManager, times(1)).getApplicationInfo(eq(TEST_PACKAGE_NAME), any());
        verify(mPackageManager, times(0)).getApplicationIcon(TEST_PACKAGE_NAME);
        assertThat(mAppInfoHelper.getAppInfoMap().get(TEST_PACKAGE_NAME).getName()).isNull();
    }

    @Test
    public void testRestoreAppInfo_appNotInstalled_updatesName()
            throws PackageManager.NameNotFoundException {
        setAppAsNotInstalled();
        mTransactionTestUtils.insertApp(TEST_PACKAGE_NAME);

        mAppInfoHelper.restoreAppInfo(TEST_PACKAGE_NAME, TEST_APP_NAME);
        assertThat(mAppInfoHelper.getAppInfoMap().get(TEST_PACKAGE_NAME).getName())
                .isEqualTo(TEST_APP_NAME);
    }

    @Test
    public void testRestoreAppInfo_appNotInstalled_noPreviousEntry_addsEntry()
            throws PackageManager.NameNotFoundException {
        setAppAsNotInstalled();

        mAppInfoHelper.restoreAppInfo(TEST_PACKAGE_NAME, TEST_APP_NAME);
        assertThat(mAppInfoHelper.getAppInfoMap().get(TEST_PACKAGE_NAME).getName())
                .isEqualTo(TEST_APP_NAME);
    }

    @Test
    public void testRestoreAppInfo_appInstalled_noChangeMade()
            throws PackageManager.NameNotFoundException {
        setAppAsInstalled();
        mTransactionTestUtils.insertApp(TEST_PACKAGE_NAME);

        mAppInfoHelper.restoreAppInfo(TEST_PACKAGE_NAME, TEST_APP_NAME);
        assertThat(mAppInfoHelper.getAppInfoMap().get(TEST_PACKAGE_NAME).getName()).isNull();
    }

    @Test
    public void
            testAddAppInfoIfNoRecordExists_appNotInstalledNoRecordExists_successfullyAddsRecord()
                    throws PackageManager.NameNotFoundException {
        setAppAsNotInstalled();

        assertThat(doesRecordExistForPackage()).isFalse();

        mAppInfoHelper.addAppInfoIfNoAppInfoEntryExists(TEST_PACKAGE_NAME, TEST_APP_NAME);

        assertThat(doesRecordExistForPackage()).isTrue();
    }

    @Test
    public void testAddAppInfoIfNoRecordExists_appInstalledNoRecordExists_noNewRecordAdded()
            throws PackageManager.NameNotFoundException {
        setAppAsInstalled();
        mTransactionTestUtils.insertApp(TEST_PACKAGE_NAME);

        assertThat(doesRecordExistForPackage()).isTrue();

        mAppInfoHelper.addAppInfoIfNoAppInfoEntryExists(TEST_PACKAGE_NAME, TEST_APP_NAME);

        verify(mPackageManager, times(0)).getApplicationInfo(eq(TEST_PACKAGE_NAME), any());
        verify(mPackageManager, times(0)).getApplicationIcon(TEST_PACKAGE_NAME);
    }

    private void setAppAsNotInstalled() throws PackageManager.NameNotFoundException {
        when(mPackageManager.getApplicationInfo(eq(TEST_PACKAGE_NAME), any()))
                .thenThrow(new PackageManager.NameNotFoundException());
        when(mPackageManager.getApplicationIcon(TEST_PACKAGE_NAME))
                .thenThrow(new PackageManager.NameNotFoundException());
    }

    private void setAppAsInstalled() throws PackageManager.NameNotFoundException {
        ApplicationInfo expectedAppInfo = new ApplicationInfo();
        expectedAppInfo.packageName = TEST_PACKAGE_NAME;
        expectedAppInfo.flags = 0;

        when(mPackageManager.getApplicationInfo(eq(TEST_PACKAGE_NAME), any()))
                .thenReturn(expectedAppInfo);
        when(mPackageManager.getApplicationIcon(TEST_PACKAGE_NAME)).thenReturn(mDrawable);
    }

    private boolean doesRecordExistForPackage() {
        return mAppInfoHelper.getAppInfoId(TEST_PACKAGE_NAME) != -1;
    }
}
