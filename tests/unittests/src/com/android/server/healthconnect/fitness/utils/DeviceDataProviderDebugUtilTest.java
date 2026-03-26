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

package com.android.server.healthconnect.fitness.utils;

import static android.health.connect.datatypes.Device.DEVICE_TYPE_PHONE;
import static android.healthconnect.testing.unittest.RecordInternalFactory.buildStepsRecord;

import static com.android.healthfitness.flags.Flags.FLAG_DDP_DEBUG_UTIL;
import static com.android.healthfitness.flags.Flags.FLAG_DEVICE_DATA_PROVIDERS_API;
import static com.android.healthfitness.flags.Flags.FLAG_DEVICE_DATA_PROVIDERS_DB;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.graphics.drawable.Drawable;
import android.health.connect.HealthPermissions;
import android.health.connect.datatypes.Device;
import android.health.connect.datatypes.StepsRecord;
import android.health.connect.device.DeviceDataAdvertisement;
import android.health.connect.device.DeviceDataTypeAdvertisement;
import android.health.connect.device.SyntheticPackageNameMatcher;
import android.healthconnect.testing.unittest.FitnessTestUtils;
import android.os.Process;
import android.os.UserHandle;
import android.permission.PermissionManager;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.server.healthconnect.common.metadata.AppInfoHelper;
import com.android.server.healthconnect.common.metadata.SyntheticPackageNameCreator;
import com.android.server.healthconnect.common.preferences.PreferenceHelper;
import com.android.server.healthconnect.device.DeviceDataProviderDebugUtil;
import com.android.server.healthconnect.device.DeviceDataProviderManager;
import com.android.server.healthconnect.device.FakeSerialDeviceDataProviderManager;
import com.android.server.healthconnect.device.FakeSerialDeviceDataSourceHelper;
import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;
import com.android.server.healthconnect.storage.HealthConnectContext;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Unit test class for {@link DeviceDataProviderDebugUtil} */
@RunWith(AndroidJUnit4.class)
@EnableFlags({FLAG_DEVICE_DATA_PROVIDERS_DB, FLAG_DEVICE_DATA_PROVIDERS_API})
public class DeviceDataProviderDebugUtilTest {

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final TemporaryFolder mEnvironmentDataDir = new TemporaryFolder();
    private static final String TEST_APP_NAME = "testAppName";
    private static final String DEVICE_PROVIDER_PACKAGE_NAME = "com.test.app";
    private static final String PREFERENCE_KEY =
            SyntheticPackageNameCreator.SYNTHETIC_PACKAGE_NAME_SALT_PREFERENCE_KEY;
    private static final String HC_PACKAGE_NAME = "com.android.healthconnect";
    private static final String MANUFACTURER = "testManufacturerName";
    private static final String MODEL = "testModelName";
    private static final String DISPLAY_NAME = "testDisplayName";
    @Mock private Context mServiceContext;
    @Mock private PreferenceHelper mPreferenceHelper;
    @Mock private PackageManager mPackageManager;
    @Mock private PermissionManager mPermissionManager;
    @Mock private Drawable mDrawable;
    private DeviceDataProviderManager mDeviceDataProviderManager;
    private final Instant mNow = Instant.now();
    private AppInfoHelper mAppInfoHelper;
    private DeviceDataProviderDebugUtil mDeviceDataProviderDebugUtil;
    private String mTestCanonicalSpn;
    private FitnessTestUtils mFitnessTestUtils;

    @Before
    public void setUp() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        UserHandle userHandle = context.getUser();
        when(mPackageManager.getPackageUid(anyString(), anyInt())).thenReturn(Process.myUid());
        when(mServiceContext.getApplicationContext()).thenReturn(mServiceContext);
        when(mServiceContext.getPackageManager()).thenReturn(mPackageManager);
        when(mServiceContext.getUser()).thenReturn(userHandle);
        when(mServiceContext.createContextAsUser(userHandle, 0)).thenReturn(mServiceContext);
        when(mServiceContext.getSystemService(ActivityManager.class))
                .thenReturn(context.getSystemService(ActivityManager.class));
        when(mServiceContext.getSystemService(PermissionManager.class))
                .thenReturn(mPermissionManager);
        setUpHealthPermissions();
        when(mDrawable.getIntrinsicHeight()).thenReturn(200);
        when(mDrawable.getIntrinsicWidth()).thenReturn(200);
        when(mPackageManager.getApplicationIcon(anyString()))
                .thenThrow(new PackageManager.NameNotFoundException());
        when(mPackageManager.getDefaultActivityIcon()).thenReturn(mDrawable);

        HealthConnectInjector healthConnectInjector =
                HealthConnectInjectorImpl.newBuilderForTest(mServiceContext)
                        .setPreferenceHelper(mPreferenceHelper)
                        .setEnvironmentDataDirectory(mEnvironmentDataDir.getRoot())
                        .setDeviceDataSourceHelper(new FakeSerialDeviceDataSourceHelper())
                        .build();

        mAppInfoHelper = healthConnectInjector.getAppInfoHelper();
        mDeviceDataProviderManager =
                new FakeSerialDeviceDataProviderManager(
                        HealthConnectContext.create(
                                mServiceContext, userHandle, null, mEnvironmentDataDir.getRoot()),
                        healthConnectInjector.getDeviceInfoHelper(),
                        mAppInfoHelper,
                        healthConnectInjector.getDeviceDataSourceHelper(),
                        healthConnectInjector.getDeviceDataSourcesHelper(),
                        healthConnectInjector.getDeviceDataProviderMetadataHelper(),
                        healthConnectInjector.getFitnessRecordUpsertHelper(),
                        healthConnectInjector.getFitnessRecordReadHelper(),
                        healthConnectInjector.getFitnessRecordDeleteHelper(),
                        healthConnectInjector.getSyntheticPackageNameCreator(),
                        mPreferenceHelper,
                        healthConnectInjector.getHealthDataCategoryPriorityHelper(),
                        healthConnectInjector.getInternalHealthConnectMappings(),
                        true);

        mFitnessTestUtils = new FitnessTestUtils(healthConnectInjector);
        mDeviceDataProviderDebugUtil = healthConnectInjector.getDeviceDataProviderDebugUtil();
        when(mPreferenceHelper.getPreference(PREFERENCE_KEY)).thenReturn("Some Salt");
        mDeviceDataProviderManager.initializeOrRefreshCurrentDeviceIds();
        SyntheticPackageNameCreator syntheticPackageNameCreator =
                new SyntheticPackageNameCreator(mPreferenceHelper);
        mTestCanonicalSpn = syntheticPackageNameCreator.createCanonical(DEVICE_TYPE_PHONE, "foo");
    }

    @Test
    @EnableFlags({FLAG_DDP_DEBUG_UTIL})
    public void testDump_doesNotCrash() {
        mDeviceDataProviderDebugUtil.dump(
                new PrintWriter(
                        new OutputStream() {
                            @Override
                            public void write(int i) {}
                        }));
    }

    @Test
    @EnableFlags({FLAG_DDP_DEBUG_UTIL})
    public void testDump_noDevices() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream));

        mDeviceDataProviderDebugUtil.dump(writer);
        writer.close();
        String output = outputStream.toString();

        assertThat(output).contains("No devices");
    }

    @Test
    @EnableFlags({FLAG_DDP_DEBUG_UTIL})
    public void testDump_outputsNoApps() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream));

        mDeviceDataProviderDebugUtil.dump(writer);
        writer.close();
        String output = outputStream.toString();

        assertThat(output).contains("No apps");
    }

    @Test
    @EnableFlags({FLAG_DDP_DEBUG_UTIL})
    public void testDump_outputsAppInfo_withNoDeviceInfoId()
            throws PackageManager.NameNotFoundException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream));
        String TestPackageName = SyntheticPackageNameCreator.createMasked(mTestCanonicalSpn, "foo");
        setAppAsNotInstalled(TestPackageName);
        mFitnessTestUtils.insertApp(TestPackageName);
        mAppInfoHelper.updateAppInfoIfNotInstalled(TestPackageName, TEST_APP_NAME, null);
        mAppInfoHelper.clearCache();

        mFitnessTestUtils.insertRecords(
                TestPackageName,
                List.of(
                        buildStepsRecord(
                                mNow.plusSeconds(10).toEpochMilli(),
                                mNow.plusSeconds(11).toEpochMilli(),
                                200)));

        mDeviceDataProviderDebugUtil.dump(writer);
        writer.close();
        String output = outputStream.toString();

        assertThat(output)
                .contains(
                        """
                          App info id: 1
                          App name: testAppName
                        """);
        assertThat(output)
                .contains(
                        """
                          Device info id: null
                          Record types used: null
                        """);
        assertThat(output)
                .contains(
                        "Package name: "
                                + SyntheticPackageNameCreator.createMasked(
                                        mTestCanonicalSpn, "foo"));
    }

    @Test
    @DisableFlags({FLAG_DDP_DEBUG_UTIL})
    public void testDump_ddpFlagDisabled_outputsNoDeviceInfo() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream));
        Device device =
                new Device.Builder()
                        .setManufacturer(MANUFACTURER)
                        .setModel(MODEL)
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .setDisplayName(DISPLAY_NAME)
                        .build();
        Set<DeviceDataTypeAdvertisement> deviceDataTypeAdvertisements =
                Set.of(
                        new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                                .setAvailable(true)
                                .setUserEnabled(true)
                                .build());
        String deviceId = mDeviceDataProviderManager.getCurrentDeviceId();

        DeviceDataAdvertisement advertisement =
                new DeviceDataAdvertisement(device, deviceId, deviceDataTypeAdvertisements);
        mDeviceDataProviderManager.handleAdvertisement(
                Set.of(advertisement), DEVICE_PROVIDER_PACKAGE_NAME);
        mDeviceDataProviderDebugUtil.dump(writer);
        writer.close();
        String output = outputStream.toString();

        assertThat(output)
                .doesNotContain(
                        """
                          Display name: testDisplayName
                          Model: testModelName
                          Device type: 2
                          Manufacturer: testManufacturerName
                          Device id:\
                        """);
    }

    @Test
    @EnableFlags({FLAG_DDP_DEBUG_UTIL})
    public void testDump_outputsAdvertisedDeviceInfo() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream));
        Device device =
                new Device.Builder()
                        .setManufacturer(MANUFACTURER)
                        .setModel(MODEL)
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .setDisplayName(DISPLAY_NAME)
                        .build();
        Set<DeviceDataTypeAdvertisement> deviceDataTypeAdvertisements =
                Set.of(
                        new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                                .setAvailable(true)
                                .setUserEnabled(true)
                                .build());
        String deviceId = mDeviceDataProviderManager.getCurrentDeviceId();

        DeviceDataAdvertisement advertisement =
                new DeviceDataAdvertisement(device, deviceId, deviceDataTypeAdvertisements);
        mDeviceDataProviderManager.handleAdvertisement(
                Set.of(advertisement), DEVICE_PROVIDER_PACKAGE_NAME);
        mDeviceDataProviderDebugUtil.dump(writer);
        writer.close();
        String output = outputStream.toString();

        assertThat(output)
                .contains(
                        """
                          Display name: testDisplayName
                          Model: testModelName
                          Device type: 2
                          Manufacturer: testManufacturerName
                          Device id:\
                        """);

        Optional<String> deviceIdLine =
                output.lines().filter(line -> line.startsWith("Device id:")).findFirst();
        if (deviceIdLine.isPresent()) {
            deviceIdLine = Optional.of(deviceIdLine.get().substring(11));
            assertThat(SyntheticPackageNameMatcher.matches(deviceIdLine.get())).isTrue();
        }
    }

    private void setUpHealthPermissions() throws PackageManager.NameNotFoundException {
        PermissionGroupInfo info = new PermissionGroupInfo();
        info.packageName = HC_PACKAGE_NAME;
        when(mPackageManager.getPermissionGroupInfo(
                        eq(HealthPermissions.HEALTH_PERMISSION_GROUP), eq(0)))
                .thenReturn(info);

        PackageInfo mockPackageInfo = new PackageInfo();
        // For now add a few of the HealthPermissions just for the test.
        mockPackageInfo.permissions =
                new PermissionInfo[] {
                    createPermissionInfo(HealthPermissions.READ_HEART_RATE),
                    createPermissionInfo(HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND),
                    createPermissionInfo(HealthPermissions.READ_SKIN_TEMPERATURE),
                    createPermissionInfo(HealthPermissions.READ_OXYGEN_SATURATION),
                };
        when(mPackageManager.getPackageInfo(eq(HC_PACKAGE_NAME), any()))
                .thenReturn(mockPackageInfo);
    }

    private PermissionInfo createPermissionInfo(String permissionName) {
        PermissionInfo permissionInfo = new PermissionInfo();
        permissionInfo.name = permissionName;
        permissionInfo.group = HealthPermissions.HEALTH_PERMISSION_GROUP;
        return permissionInfo;
    }

    private void setAppAsNotInstalled(String packageName)
            throws PackageManager.NameNotFoundException {
        when(mPackageManager.getApplicationInfo(
                        eq(packageName), any(PackageManager.ApplicationInfoFlags.class)))
                .thenThrow(new PackageManager.NameNotFoundException());
        when(mPackageManager.getApplicationIcon(eq(packageName)))
                .thenThrow(new PackageManager.NameNotFoundException());
    }
}
