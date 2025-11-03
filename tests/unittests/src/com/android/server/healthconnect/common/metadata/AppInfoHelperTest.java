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

package com.android.server.healthconnect.common.metadata;

import static android.health.connect.Constants.DEFAULT_LONG;
import static android.healthconnect.testing.unittest.RecordInternalFactory.buildStepsRecord;
import static android.healthconnect.testing.unittest.TaskUtils.TEST_USER;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
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
import android.content.pm.PackageManager.ApplicationInfoFlags;
import android.graphics.drawable.Drawable;
import android.health.connect.datatypes.AppInfo;
import android.health.connect.internal.datatypes.AppInfoInternal;
import android.health.connect.internal.datatypes.RecordInternal;
import android.healthconnect.testing.unittest.FitnessTestUtils;
import android.healthconnect.testing.unittest.RecordInternalFactory;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.healthfitness.flags.Flags;
import com.android.server.healthconnect.common.accesslog.AppOpLogsHelper;
import com.android.server.healthconnect.device.DeviceDataSource;
import com.android.server.healthconnect.device.DeviceDataSourcesHelper;
import com.android.server.healthconnect.device.DeviceRecordHelper;
import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;
import com.android.server.healthconnect.permission.FirstGrantTimeManager;
import com.android.server.healthconnect.permission.HealthPermissionIntentAppsTracker;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RunWith(AndroidJUnit4.class)
public class AppInfoHelperTest {

    private static final String TEST_PACKAGE_NAME = "test.package.name";
    private static final String TEST_APP_NAME = "testAppName";

    @Rule public final TemporaryFolder mEnvironmentDataDir = new TemporaryFolder();
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Mock private Context mContext;
    @Mock private Drawable mDrawable;
    @Mock private PackageManager mPackageManager;
    @Mock private DeviceDataSourcesHelper mMockDeviceDataSourcesHelper;
    @Mock private DeviceDataSource mMockDeviceDataSource;

    private static final String DEVICE_PROVIDER_PACKAGE_NAME =
            DeviceRecordHelper.DEVICE_DATA_PROVIDER_PACKAGE;
    private static final String ORIGINAL_DEVICE_APP_NAME = "Original Android OS";
    private static final String EXPECTED_DEVICE_APP_NAME = "My Pixel Watch";

    private AppInfoHelper mAppInfoHelper;
    private DeviceInfoHelper mDeviceInfoHelper;
    private FitnessTestUtils mFitnessTestUtils;

    @Before
    public void setup() throws PackageManager.NameNotFoundException {
        when(mContext.getUser()).thenReturn(TEST_USER);
        when(mContext.createContextAsUser(any(), anyInt())).thenReturn(mContext);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);

        when(mPackageManager.getDefaultActivityIcon()).thenReturn(mDrawable);

        when(mDrawable.getIntrinsicHeight()).thenReturn(200);
        when(mDrawable.getIntrinsicWidth()).thenReturn(200);

        mMockDeviceDataSourcesHelper = mock(DeviceDataSourcesHelper.class);
        mMockDeviceDataSource = mock(DeviceDataSource.class);
        when(mMockDeviceDataSourcesHelper.getCurrentDevice(any()))
                .thenReturn(mMockDeviceDataSource);
        when(mMockDeviceDataSource.getDisplayName()).thenReturn(EXPECTED_DEVICE_APP_NAME);

        HealthConnectInjector healthConnectInjector =
                HealthConnectInjectorImpl.newBuilderForTest(mContext)
                        .setFirstGrantTimeManager(mock(FirstGrantTimeManager.class))
                        .setHealthPermissionIntentAppsTracker(
                                mock(HealthPermissionIntentAppsTracker.class))
                        .setAppOpLogsHelper(mock(AppOpLogsHelper.class))
                        .setDeviceDataSourcesHelper(mMockDeviceDataSourcesHelper)
                        .setEnvironmentDataDirectory(mEnvironmentDataDir.getRoot())
                        .build();
        mAppInfoHelper = healthConnectInjector.getAppInfoHelper();
        mDeviceInfoHelper = healthConnectInjector.getDeviceInfoHelper();
        mFitnessTestUtils = new FitnessTestUtils(healthConnectInjector);
    }

    @After
    public void tearDown() throws Exception {
        reset(
                mDrawable,
                mContext,
                mPackageManager,
                mMockDeviceDataSourcesHelper,
                mMockDeviceDataSource);
    }

    @Test
    @EnableFlags(Flags.FLAG_STEP_TRACKING_ENABLED)
    public void deviceDataProvider_addedToAppInfo_cacheRepopulated_deviceDisplayNameUsed()
            throws PackageManager.NameNotFoundException {
        setAppAsNotInstalled(DEVICE_PROVIDER_PACKAGE_NAME);
        setAppAsNotInstalled(TEST_PACKAGE_NAME);
        mFitnessTestUtils.insertApp(DEVICE_PROVIDER_PACKAGE_NAME);
        mAppInfoHelper.updateAppInfoIfNotInstalled(
                DEVICE_PROVIDER_PACKAGE_NAME, ORIGINAL_DEVICE_APP_NAME, null);
        mFitnessTestUtils.insertApp(TEST_PACKAGE_NAME);
        mAppInfoHelper.updateAppInfoIfNotInstalled(TEST_PACKAGE_NAME, TEST_APP_NAME, null);
        Instant now = Instant.now();
        mFitnessTestUtils.insertRecords(
                DEVICE_PROVIDER_PACKAGE_NAME,
                List.of(
                        RecordInternalFactory.buildStepsRecord(
                                UUID.randomUUID().toString(),
                                now.toEpochMilli(),
                                now.plusSeconds(1).toEpochMilli(),
                                100)));
        mFitnessTestUtils.insertRecords(
                TEST_PACKAGE_NAME,
                List.of(
                        RecordInternalFactory.buildStepsRecord(
                                UUID.randomUUID().toString(),
                                now.plusSeconds(10).toEpochMilli(),
                                now.plusSeconds(11).toEpochMilli(),
                                200)));

        mAppInfoHelper.clearCache();
        Map<String, AppInfoInternal> appInfoInternalMap = mAppInfoHelper.getAppInfoMap();

        assertThat(appInfoInternalMap).containsKey(DEVICE_PROVIDER_PACKAGE_NAME);
        AppInfo deviceAppInfo = appInfoInternalMap.get(DEVICE_PROVIDER_PACKAGE_NAME).toExternal();
        assertThat(deviceAppInfo.getName()).isEqualTo(EXPECTED_DEVICE_APP_NAME);

        assertThat(appInfoInternalMap).containsKey(TEST_PACKAGE_NAME);
        AppInfo testAppInfo = appInfoInternalMap.get(TEST_PACKAGE_NAME).toExternal();
        assertThat(testAppInfo.getName()).isEqualTo(TEST_APP_NAME);
    }

    @Test
    @EnableFlags(Flags.FLAG_STEP_TRACKING_ENABLED)
    public void deviceDataProvider_addedToAppInfo_cacheAlreadyPopulated_deviceDisplayNameUsed()
            throws PackageManager.NameNotFoundException {
        setAppAsNotInstalled(DEVICE_PROVIDER_PACKAGE_NAME);
        setAppAsNotInstalled(TEST_PACKAGE_NAME);
        mFitnessTestUtils.insertApp(DEVICE_PROVIDER_PACKAGE_NAME);
        mAppInfoHelper.updateAppInfoIfNotInstalled(
                DEVICE_PROVIDER_PACKAGE_NAME, ORIGINAL_DEVICE_APP_NAME, null);
        mFitnessTestUtils.insertApp(TEST_PACKAGE_NAME);
        mAppInfoHelper.updateAppInfoIfNotInstalled(TEST_PACKAGE_NAME, TEST_APP_NAME, null);
        Instant now = Instant.now();
        mFitnessTestUtils.insertRecords(
                DEVICE_PROVIDER_PACKAGE_NAME,
                List.of(
                        RecordInternalFactory.buildStepsRecord(
                                UUID.randomUUID().toString(),
                                now.toEpochMilli(),
                                now.plusSeconds(1).toEpochMilli(),
                                100)));
        mFitnessTestUtils.insertRecords(
                TEST_PACKAGE_NAME,
                List.of(
                        RecordInternalFactory.buildStepsRecord(
                                UUID.randomUUID().toString(),
                                now.plusSeconds(10).toEpochMilli(),
                                now.plusSeconds(11).toEpochMilli(),
                                200)));

        Map<String, AppInfoInternal> appInfoInternalMap = mAppInfoHelper.getAppInfoMap();

        assertThat(appInfoInternalMap).containsKey(DEVICE_PROVIDER_PACKAGE_NAME);
        AppInfo deviceAppInfo = appInfoInternalMap.get(DEVICE_PROVIDER_PACKAGE_NAME).toExternal();
        assertThat(deviceAppInfo.getName()).isEqualTo(EXPECTED_DEVICE_APP_NAME);

        assertThat(appInfoInternalMap).containsKey(TEST_PACKAGE_NAME);
        AppInfo testAppInfo = appInfoInternalMap.get(TEST_PACKAGE_NAME).toExternal();
        assertThat(testAppInfo.getName()).isEqualTo(TEST_APP_NAME);
    }

    @Test
    @DisableFlags(Flags.FLAG_STEP_TRACKING_ENABLED)
    public void getAppInfoMap_deviceProviderNameNotUpdated_whenFlagDisabled()
            throws PackageManager.NameNotFoundException {
        setAppAsNotInstalled(DEVICE_PROVIDER_PACKAGE_NAME);
        setAppAsNotInstalled(TEST_PACKAGE_NAME);

        mFitnessTestUtils.insertApp(DEVICE_PROVIDER_PACKAGE_NAME);
        mAppInfoHelper.updateAppInfoIfNotInstalled(
                DEVICE_PROVIDER_PACKAGE_NAME, ORIGINAL_DEVICE_APP_NAME, null);
        mFitnessTestUtils.insertApp(TEST_PACKAGE_NAME);
        mAppInfoHelper.updateAppInfoIfNotInstalled(TEST_PACKAGE_NAME, TEST_APP_NAME, null);

        Instant now = Instant.now();
        mFitnessTestUtils.insertRecords(
                DEVICE_PROVIDER_PACKAGE_NAME,
                List.of(
                        RecordInternalFactory.buildStepsRecord(
                                UUID.randomUUID().toString(),
                                now.toEpochMilli(),
                                now.plusSeconds(1).toEpochMilli(),
                                100)));
        mFitnessTestUtils.insertRecords(
                TEST_PACKAGE_NAME,
                List.of(
                        RecordInternalFactory.buildStepsRecord(
                                UUID.randomUUID().toString(),
                                now.plusSeconds(10).toEpochMilli(),
                                now.plusSeconds(11).toEpochMilli(),
                                200)));

        mAppInfoHelper.clearCache();
        Map<String, AppInfoInternal> appInfoInternalMap = mAppInfoHelper.getAppInfoMap();

        assertThat(appInfoInternalMap).containsKey(DEVICE_PROVIDER_PACKAGE_NAME);
        AppInfo deviceAppInfo = appInfoInternalMap.get(DEVICE_PROVIDER_PACKAGE_NAME).toExternal();
        assertThat(deviceAppInfo.getName()).isEqualTo(ORIGINAL_DEVICE_APP_NAME);

        assertThat(appInfoInternalMap).containsKey(TEST_PACKAGE_NAME);
        AppInfo testAppInfo = appInfoInternalMap.get(TEST_PACKAGE_NAME).toExternal();
        assertThat(testAppInfo.getName()).isEqualTo(TEST_APP_NAME);
    }

    @Test
    @EnableFlags(Flags.FLAG_STEP_TRACKING_ENABLED)
    public void getAppInfoMap_regularPackageNameUnchanged_whenFlagEnabled()
            throws PackageManager.NameNotFoundException {
        setAppAsNotInstalled(TEST_PACKAGE_NAME);

        mFitnessTestUtils.insertApp(TEST_PACKAGE_NAME);
        mAppInfoHelper.updateAppInfoIfNotInstalled(TEST_PACKAGE_NAME, TEST_APP_NAME, null);
        Instant now = Instant.now();
        mFitnessTestUtils.insertRecords(
                TEST_PACKAGE_NAME,
                List.of(
                        RecordInternalFactory.buildStepsRecord(
                                UUID.randomUUID().toString(),
                                now.toEpochMilli(),
                                now.plusSeconds(1).toEpochMilli(),
                                200)));

        mAppInfoHelper.clearCache();
        Map<String, AppInfoInternal> appInfoInternalMap = mAppInfoHelper.getAppInfoMap();

        assertThat(appInfoInternalMap).containsKey(TEST_PACKAGE_NAME);
        AppInfo testAppInfo = appInfoInternalMap.get(TEST_PACKAGE_NAME).toExternal();
        assertThat(testAppInfo.getName()).isEqualTo(TEST_APP_NAME);
    }

    @Test
    public void testUpdateAppInfoIfNotInstalled_withoutIcon_getIconFromPackageName()
            throws PackageManager.NameNotFoundException {
        setAppAsNotInstalled(TEST_PACKAGE_NAME);
        mFitnessTestUtils.insertApp(TEST_PACKAGE_NAME);

        mAppInfoHelper.updateAppInfoIfNotInstalled(TEST_PACKAGE_NAME, TEST_APP_NAME, null);

        verify(mPackageManager).getApplicationIcon(TEST_PACKAGE_NAME);
        assertThat(mAppInfoHelper.getAppInfoMap().get(TEST_PACKAGE_NAME).getName())
                .isEqualTo(TEST_APP_NAME);
    }

    @Test
    public void testUpdateAppInfoIfNotInstalled_withoutIcon_getDefaultIconIfPackageIsNotFound()
            throws PackageManager.NameNotFoundException {
        setAppAsNotInstalled(TEST_PACKAGE_NAME);
        mFitnessTestUtils.insertApp(TEST_PACKAGE_NAME);

        mAppInfoHelper.updateAppInfoIfNotInstalled(TEST_PACKAGE_NAME, TEST_APP_NAME, null);

        verify(mPackageManager).getDefaultActivityIcon();
        assertThat(mAppInfoHelper.getAppInfoMap().get(TEST_PACKAGE_NAME).getName())
                .isEqualTo(TEST_APP_NAME);
    }

    @Test
    public void testUpdateAppInfoIfNotInstalled_appInstalled_noChangeMade()
            throws PackageManager.NameNotFoundException {
        setAppAsInstalled();
        mFitnessTestUtils.insertApp(TEST_PACKAGE_NAME);

        mAppInfoHelper.updateAppInfoIfNotInstalled(TEST_PACKAGE_NAME, TEST_APP_NAME, null);

        verify(mPackageManager, times(1))
                .getApplicationInfo(eq(TEST_PACKAGE_NAME), any(ApplicationInfoFlags.class));
        verify(mPackageManager, times(0)).getApplicationIcon(TEST_PACKAGE_NAME);
        assertThat(mAppInfoHelper.getAppInfoMap().get(TEST_PACKAGE_NAME).getName()).isNull();
    }

    @Test
    public void testRestoreAppInfo_appNotInstalled_updatesName()
            throws PackageManager.NameNotFoundException {
        setAppAsNotInstalled(TEST_PACKAGE_NAME);
        mFitnessTestUtils.insertApp(TEST_PACKAGE_NAME);

        mAppInfoHelper.restoreAppInfo(TEST_PACKAGE_NAME, TEST_APP_NAME);
        assertThat(mAppInfoHelper.getAppInfoMap().get(TEST_PACKAGE_NAME).getName())
                .isEqualTo(TEST_APP_NAME);
    }

    @Test
    public void testRestoreAppInfo_appNotInstalled_noPreviousEntry_addsEntry()
            throws PackageManager.NameNotFoundException {
        setAppAsNotInstalled(TEST_PACKAGE_NAME);

        mAppInfoHelper.restoreAppInfo(TEST_PACKAGE_NAME, TEST_APP_NAME);
        assertThat(mAppInfoHelper.getAppInfoMap().get(TEST_PACKAGE_NAME).getName())
                .isEqualTo(TEST_APP_NAME);
    }

    @Test
    public void testRestoreAppInfo_appInstalled_noChangeMade()
            throws PackageManager.NameNotFoundException {
        setAppAsInstalled();
        mFitnessTestUtils.insertApp(TEST_PACKAGE_NAME);

        mAppInfoHelper.restoreAppInfo(TEST_PACKAGE_NAME, TEST_APP_NAME);
        assertThat(mAppInfoHelper.getAppInfoMap().get(TEST_PACKAGE_NAME).getName()).isNull();
    }

    @Test
    public void
            testAddAppInfoIfNoRecordExists_appNotInstalledNoRecordExists_successfullyAddsRecord()
                    throws PackageManager.NameNotFoundException {
        setAppAsNotInstalled(TEST_PACKAGE_NAME);

        assertThat(doesRecordExistForPackage()).isFalse();

        mAppInfoHelper.addAppInfoIfNoAppInfoEntryExists(TEST_PACKAGE_NAME, TEST_APP_NAME);

        assertThat(doesRecordExistForPackage()).isTrue();
    }

    @Test
    public void testAddAppInfoIfNoRecordExists_appInstalledNoRecordExists_noNewRecordAdded()
            throws PackageManager.NameNotFoundException {
        setAppAsInstalled();
        mFitnessTestUtils.insertApp(TEST_PACKAGE_NAME);

        assertThat(doesRecordExistForPackage()).isTrue();

        mAppInfoHelper.addAppInfoIfNoAppInfoEntryExists(TEST_PACKAGE_NAME, TEST_APP_NAME);

        verify(mPackageManager, times(0))
                .getApplicationInfo(eq(TEST_PACKAGE_NAME), any(ApplicationInfoFlags.class));
        verify(mPackageManager, times(0)).getApplicationIcon(TEST_PACKAGE_NAME);
    }

    @Test
    public void testGetOrInsertAppInfoIdNoThrow_appInstalled_returnsAppInfoId() throws Exception {
        setAppAsInstalled();
        assertThat(mAppInfoHelper.getOrInsertAppInfoIdNoThrow(TEST_PACKAGE_NAME))
                .isNotEqualTo(DEFAULT_LONG);
    }

    @Test
    public void testGetOrInsertAppInfoIdNoThrow_appNotInstalled_returnsDefaultLong()
            throws Exception {
        setAppAsNotInstalled(TEST_PACKAGE_NAME);
        assertThat(mAppInfoHelper.getOrInsertAppInfoIdNoThrow(TEST_PACKAGE_NAME))
                .isEqualTo(DEFAULT_LONG);
    }

    @Test
    public void populateAppInfoId_spnNotAdvertised_throwsIllegalStateException() {
        String canonicalSpn = SyntheticPackageNameCreator.createCanonical(1, "testDeviceId");
        RecordInternal<?> recordInternal =
                buildStepsRecord(
                        /* startTimeMillis= */ 1000,
                        /* endTimeMillis= */ 2000,
                        /* stepsCount= */ 100);
        recordInternal.setPackageName(canonicalSpn);

        assertThrows(
                IllegalStateException.class,
                () -> mAppInfoHelper.populateAppInfoId(recordInternal, true));
    }

    @Test
    @EnableFlags({
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVELOPMENT_DATABASE
    })
    public void insertsDeviceDataSource() {
        String canonicalSpn = SyntheticPackageNameCreator.createCanonical(1, "testDeviceId");
        long deviceInfoId = 1L;

        RecordInternal<?> recordInternal =
                buildStepsRecord(
                        /* startTimeMillis= */ 1000,
                        /* endTimeMillis= */ 2000,
                        /* stepsCount= */ 100);
        recordInternal.setManufacturer("Google");
        recordInternal.setModel("Pixel");
        recordInternal.setDeviceType(1);
        mDeviceInfoHelper.populateDeviceInfoId(recordInternal);
        assertThat(recordInternal.getDeviceInfoId()).isEqualTo(deviceInfoId);

        mAppInfoHelper.insertDeviceDataSourceIfNotPresent(canonicalSpn, deviceInfoId);

        assertThat(mAppInfoHelper.getAppInfoMap()).containsKey(canonicalSpn);
        assertThat(mAppInfoHelper.getAppInfoMap().get(canonicalSpn).getDeviceInfoId())
                .isEqualTo(deviceInfoId);
        assertThat(mAppInfoHelper.getAppInfoMap().get(canonicalSpn).getId()).isEqualTo(1L);
        assertThat(mAppInfoHelper.getAppInfoMap().get(canonicalSpn).getPackageName())
                .isEqualTo(canonicalSpn);
        assertThat(mAppInfoHelper.getAppInfoMap().get(canonicalSpn).getIcon()).isEqualTo(null);
        assertThat(mAppInfoHelper.getAppInfoMap().get(canonicalSpn).getName()).isEqualTo(null);
        assertThat(mAppInfoHelper.getAppInfoMap().get(canonicalSpn).getRecordTypesUsed())
                .isEqualTo(null);
    }

    @Test
    @EnableFlags({
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVELOPMENT_DATABASE
    })
    public void spnAlreadyPresent_populateAppInfoId_skipsPopulatingAppInfo() {
        String canonicalSpn = SyntheticPackageNameCreator.createCanonical(1, "testDeviceId");
        long deviceInfoId = 1L;

        RecordInternal<?> recordInternal =
                buildStepsRecord(
                        /* startTimeMillis= */ 1000,
                        /* endTimeMillis= */ 2000,
                        /* stepsCount= */ 100);
        recordInternal.setManufacturer("Google");
        recordInternal.setModel("Pixel");
        recordInternal.setDeviceType(1);
        mDeviceInfoHelper.populateDeviceInfoId(recordInternal);
        assertThat(recordInternal.getDeviceInfoId()).isEqualTo(deviceInfoId);

        mAppInfoHelper.insertDeviceDataSourceIfNotPresent(canonicalSpn, deviceInfoId);
        recordInternal.setPackageName(canonicalSpn);

        mAppInfoHelper.populateAppInfoId(recordInternal, true);

        assertThat(recordInternal.getAppInfoId()).isEqualTo(1L);
        assertThat(mAppInfoHelper.getAppInfoMap()).containsKey(canonicalSpn);
        assertThat(mAppInfoHelper.getAppInfoMap().get(canonicalSpn).getDeviceInfoId())
                .isEqualTo(deviceInfoId);
        assertThat(mAppInfoHelper.getAppInfoMap().get(canonicalSpn).getId()).isEqualTo(1L);
        assertThat(mAppInfoHelper.getAppInfoMap().get(canonicalSpn).getPackageName())
                .isEqualTo(canonicalSpn);
        assertThat(mAppInfoHelper.getAppInfoMap().get(canonicalSpn).getIcon()).isEqualTo(null);
        assertThat(mAppInfoHelper.getAppInfoMap().get(canonicalSpn).getName()).isEqualTo(null);
        assertThat(mAppInfoHelper.getAppInfoMap().get(canonicalSpn).getRecordTypesUsed())
                .isEqualTo(null);
    }

    private void setAppAsNotInstalled(String packageName)
            throws PackageManager.NameNotFoundException {
        when(mPackageManager.getApplicationInfo(eq(packageName), any(ApplicationInfoFlags.class)))
                .thenThrow(new PackageManager.NameNotFoundException());
        when(mPackageManager.getApplicationIcon(eq(packageName)))
                .thenThrow(new PackageManager.NameNotFoundException());
    }

    private void setAppAsInstalled() throws PackageManager.NameNotFoundException {
        ApplicationInfo expectedAppInfo = new ApplicationInfo();
        expectedAppInfo.packageName = TEST_PACKAGE_NAME;
        when(mPackageManager.getApplicationInfo(
                        eq(TEST_PACKAGE_NAME), any(ApplicationInfoFlags.class)))
                .thenReturn(expectedAppInfo);
        when(mPackageManager.getApplicationLabel(expectedAppInfo)).thenReturn("Test package");
        when(mPackageManager.getApplicationIcon(expectedAppInfo)).thenReturn(mDrawable);
        when(mPackageManager.getApplicationIcon(TEST_PACKAGE_NAME)).thenReturn(mDrawable);
    }

    private boolean doesRecordExistForPackage() {
        return mAppInfoHelper.getAppInfoId(TEST_PACKAGE_NAME) != -1;
    }
}
