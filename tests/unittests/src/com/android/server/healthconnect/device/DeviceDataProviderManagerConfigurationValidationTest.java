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
package com.android.server.healthconnect.device;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.health.connect.HealthConnectManager;
import android.health.connect.HealthPermissions;
import android.health.connect.datatypes.Device;
import android.health.connect.datatypes.StepsRecord;
import android.health.connect.device.DeviceDataAdvertisement;
import android.health.connect.device.DeviceDataTypeAdvertisement;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.healthfitness.flags.Flags;
import com.android.server.healthconnect.common.accesslog.AppOpLogsHelper;
import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Set;

@RunWith(AndroidJUnit4.class)
@EnableFlags({Flags.FLAG_DEVICE_DATA_PROVIDERS_API, Flags.FLAG_DEVICE_DATA_PROVIDERS_DB})
public class DeviceDataProviderManagerConfigurationValidationTest {
    private static final String PACKAGE_NAME = "com.example.app";

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Rule public final TemporaryFolder mEnvironmentDataDir = new TemporaryFolder();

    private FakeSerialDeviceDataProviderManager mDeviceDataProviderManager;
    @Mock private PackageManager mPackageManager;
    @Mock private AppOpLogsHelper mAppOpLogsHelper;

    @Before
    public void setUp() throws Exception {
        Context applicationContext = ApplicationProvider.getApplicationContext();
        Context context = Mockito.spy(applicationContext);
        doReturn(mPackageManager).when(context).getPackageManager();
        doReturn(context).when(context).getApplicationContext();
        doReturn(context).when(context).createContextAsUser(any(), anyInt());
        HealthConnectInjector healthConnectInjector =
                HealthConnectInjectorImpl.newBuilderForTest(context)
                        .setAppOpLogsHelper(mAppOpLogsHelper)
                        .setEnvironmentDataDirectory(mEnvironmentDataDir.getRoot())
                        .build();

        mDeviceDataProviderManager =
                new FakeSerialDeviceDataProviderManager(
                        context,
                        healthConnectInjector.getDeviceInfoHelper(),
                        healthConnectInjector.getAppInfoHelper(),
                        new FakeSerialDeviceDataSourceHelper(),
                        healthConnectInjector.getDeviceDataSourcesHelper(),
                        healthConnectInjector.getDeviceDataProviderMetadataHelper(),
                        healthConnectInjector.getFitnessRecordUpsertHelper(),
                        healthConnectInjector.getFitnessRecordReadHelper(),
                        healthConnectInjector.getFitnessRecordDeleteHelper(),
                        healthConnectInjector.getSyntheticPackageNameCreator(),
                        false);

        mDeviceDataProviderManager.initializeOrRefreshCurrentDeviceIds();
    }

    @Test
    public void handleAdvertisement_validConfiguration_succeeds() {
        mockResolveActivity(
                HealthConnectManager.ACTION_SHOW_DEVICE_ONBOARDING,
                true,
                true,
                HealthPermissions.MANAGE_HEALTH_DATA_PERMISSION);
        mockResolveActivity(
                HealthConnectManager.ACTION_SHOW_DEVICE_MANAGEMENT,
                true,
                true,
                HealthPermissions.MANAGE_HEALTH_DATA_PERMISSION);

        advertiseDevice();
    }

    @Test
    public void handleAdvertisement_missingOnboardingActivity_throws() {
        mockResolveActivity(
                HealthConnectManager.ACTION_SHOW_DEVICE_ONBOARDING,
                false,
                true,
                HealthPermissions.MANAGE_HEALTH_DATA_PERMISSION);
        mockResolveActivity(
                HealthConnectManager.ACTION_SHOW_DEVICE_MANAGEMENT,
                true,
                true,
                HealthPermissions.MANAGE_HEALTH_DATA_PERMISSION);

        IllegalArgumentException e =
                assertThrows(IllegalArgumentException.class, this::advertiseDevice);
        assertThat(e)
                .hasMessageThat()
                .contains(
                        "must export an activity that handles "
                                + HealthConnectManager.ACTION_SHOW_DEVICE_ONBOARDING);
    }

    @Test
    public void handleAdvertisement_missingManagementActivity_throws() {
        mockResolveActivity(
                HealthConnectManager.ACTION_SHOW_DEVICE_ONBOARDING,
                true,
                true,
                HealthPermissions.MANAGE_HEALTH_DATA_PERMISSION);
        mockResolveActivity(
                HealthConnectManager.ACTION_SHOW_DEVICE_MANAGEMENT,
                false,
                true,
                HealthPermissions.MANAGE_HEALTH_DATA_PERMISSION);

        IllegalArgumentException e =
                assertThrows(IllegalArgumentException.class, this::advertiseDevice);
        assertThat(e)
                .hasMessageThat()
                .contains(
                        "must export an activity that handles "
                                + HealthConnectManager.ACTION_SHOW_DEVICE_MANAGEMENT);
    }

    @Test
    public void handleAdvertisement_onboardingNotExported_throws() {
        mockResolveActivity(
                HealthConnectManager.ACTION_SHOW_DEVICE_ONBOARDING,
                true,
                false,
                HealthPermissions.MANAGE_HEALTH_DATA_PERMISSION);
        mockResolveActivity(
                HealthConnectManager.ACTION_SHOW_DEVICE_MANAGEMENT,
                true,
                true,
                HealthPermissions.MANAGE_HEALTH_DATA_PERMISSION);

        IllegalArgumentException e =
                assertThrows(IllegalArgumentException.class, this::advertiseDevice);
        assertThat(e).hasMessageThat().contains("must be exported");
    }

    @Test
    public void handleAdvertisement_managementNotExported_throws() {
        mockResolveActivity(
                HealthConnectManager.ACTION_SHOW_DEVICE_ONBOARDING,
                true,
                true,
                HealthPermissions.MANAGE_HEALTH_DATA_PERMISSION);
        mockResolveActivity(
                HealthConnectManager.ACTION_SHOW_DEVICE_MANAGEMENT,
                true,
                false,
                HealthPermissions.MANAGE_HEALTH_DATA_PERMISSION);

        IllegalArgumentException e =
                assertThrows(IllegalArgumentException.class, this::advertiseDevice);
        assertThat(e).hasMessageThat().contains("must be exported");
    }

    @Test
    public void handleAdvertisement_onboardingWrongPermission_throws() {
        mockResolveActivity(
                HealthConnectManager.ACTION_SHOW_DEVICE_ONBOARDING, true, true, "wrong.permission");
        mockResolveActivity(
                HealthConnectManager.ACTION_SHOW_DEVICE_MANAGEMENT,
                true,
                true,
                HealthPermissions.MANAGE_HEALTH_DATA_PERMISSION);

        IllegalArgumentException e =
                assertThrows(IllegalArgumentException.class, this::advertiseDevice);
        assertThat(e).hasMessageThat().contains("must be exported and permission guarded");
    }

    @Test
    public void handleAdvertisement_managementWrongPermission_throws() {
        mockResolveActivity(
                HealthConnectManager.ACTION_SHOW_DEVICE_ONBOARDING,
                true,
                true,
                HealthPermissions.MANAGE_HEALTH_DATA_PERMISSION);
        mockResolveActivity(
                HealthConnectManager.ACTION_SHOW_DEVICE_MANAGEMENT, true, true, "wrong.permission");

        IllegalArgumentException e =
                assertThrows(IllegalArgumentException.class, this::advertiseDevice);
        assertThat(e).hasMessageThat().contains("must be exported and permission guarded");
    }

    @Test
    public void handleAdvertisement_onboardingNoPermission_throws() {
        mockResolveActivity(HealthConnectManager.ACTION_SHOW_DEVICE_ONBOARDING, true, true, null);
        mockResolveActivity(
                HealthConnectManager.ACTION_SHOW_DEVICE_MANAGEMENT,
                true,
                true,
                HealthPermissions.MANAGE_HEALTH_DATA_PERMISSION);

        IllegalArgumentException e =
                assertThrows(IllegalArgumentException.class, this::advertiseDevice);
        assertThat(e).hasMessageThat().contains("must be exported and permission guarded");
    }

    @Test
    public void handleAdvertisement_managementNoPermission_throws() {
        mockResolveActivity(
                HealthConnectManager.ACTION_SHOW_DEVICE_ONBOARDING,
                true,
                true,
                HealthPermissions.MANAGE_HEALTH_DATA_PERMISSION);
        mockResolveActivity(HealthConnectManager.ACTION_SHOW_DEVICE_MANAGEMENT, true, true, null);

        IllegalArgumentException e =
                assertThrows(IllegalArgumentException.class, this::advertiseDevice);
        assertThat(e).hasMessageThat().contains("must be exported and permission guarded by");
    }

    private void mockResolveActivity(
            String action, boolean present, boolean exported, String permission) {
        if (!present) {
            when(mPackageManager.resolveActivity(
                            argThat(
                                    intent ->
                                            intent != null
                                                    && action.equals(intent.getAction())
                                                    && PACKAGE_NAME.equals(intent.getPackage())),
                            eq(0)))
                    .thenReturn(null);
            return;
        }

        ResolveInfo info = new ResolveInfo();
        info.activityInfo = new ActivityInfo();
        info.activityInfo.exported = exported;
        info.activityInfo.permission = permission;

        when(mPackageManager.resolveActivity(
                        argThat(
                                intent ->
                                        intent != null
                                                && action.equals(intent.getAction())
                                                && PACKAGE_NAME.equals(intent.getPackage())),
                        eq(0)))
                .thenReturn(info);
    }

    private void advertiseDevice() {
        Device device =
                new Device.Builder()
                        .setManufacturer("TestManufacturer")
                        .setModel("TestModel")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .setDisplayName("Test Device")
                        .build();
        DeviceDataTypeAdvertisement.Builder advertisementBuilder =
                new DeviceDataTypeAdvertisement.Builder(StepsRecord.class).setAvailable(true);
        Set<DeviceDataTypeAdvertisement> deviceDataTypeAdvertisement =
                Set.of(advertisementBuilder.build());
        DeviceDataAdvertisement advertisement =
                new DeviceDataAdvertisement(device, "device id", deviceDataTypeAdvertisement);

        mDeviceDataProviderManager.handleAdvertisement(Set.of(advertisement), PACKAGE_NAME);
    }
}
