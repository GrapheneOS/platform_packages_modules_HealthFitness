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

import static android.health.connect.datatypes.Device.DEVICE_TYPE_PHONE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_STEPS;
import static android.healthconnect.testing.unittest.RecordInternalFactory.buildStepsRecord;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.health.connect.HealthPermissions;
import android.health.connect.datatypes.Device;
import android.health.connect.datatypes.StepsRecord;
import android.health.connect.internal.datatypes.AppInfoInternal;
import android.health.connect.internal.datatypes.RecordInternal;
import android.os.Build;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;

import com.android.healthfitness.flags.Flags;
import com.android.server.healthconnect.common.metadata.AppInfoHelper;
import com.android.server.healthconnect.common.metadata.DeviceInfoHelper;
import com.android.server.healthconnect.fitness.FitnessRecordReadHelper;
import com.android.server.healthconnect.fitness.helpers.DeviceDataProviderHelper;
import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;
import com.android.server.healthconnect.storage.TransactionManager;

import com.google.common.collect.ImmutableMap;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RunWith(AndroidJUnit4.class)
@EnableFlags({
    Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
    Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
    Flags.FLAG_DEVELOPMENT_DATABASE
})
public class DeviceDataProviderManagerTest {

    private static final String PACKAGE_NAME = "com.example.app";
    private static final String DEVICE_ID = "test_device_id";
    private static final String DISPLAY_NAME = "Test Device";
    private static final String MANUFACTURER = "TestManufacturer";
    private static final String MODEL = "TestModel";
    private static final int DEVICE_TYPE = DEVICE_TYPE_PHONE;

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Rule public final TemporaryFolder mEnvironmentDataDir = new TemporaryFolder();

    private DeviceDataProviderHelper mDeviceDataProviderHelper;

    private DeviceInfoHelper mDeviceInfoHelper;
    private AppInfoHelper mAppInfoHelper;
    private DeviceDataProviderManager mDeviceDataProviderManager;
    private FitnessRecordReadHelper mFitnessRecordReadHelper;
    private TransactionManager mTransactionManager;
    private Context mContext;

    @Before
    public void setUp() throws Exception {

        Context applicationContext = ApplicationProvider.getApplicationContext();
        mContext = spy(applicationContext);
        doReturn(mContext).when(mContext).getApplicationContext();
        doReturn(mContext).when(mContext).createContextAsUser(any(), anyInt());
        HealthConnectInjector healthConnectInjector =
                HealthConnectInjectorImpl.newBuilderForTest(mContext)
                        .setEnvironmentDataDirectory(mEnvironmentDataDir.getRoot())
                        .build();

        mDeviceInfoHelper = healthConnectInjector.getDeviceInfoHelper();
        mAppInfoHelper = healthConnectInjector.getAppInfoHelper();
        mDeviceDataProviderHelper = healthConnectInjector.getDeviceDataProviderHelper();
        mDeviceDataProviderManager = healthConnectInjector.getDeviceDataProviderManager();
        mFitnessRecordReadHelper = healthConnectInjector.getFitnessRecordReadHelper();
        mTransactionManager = healthConnectInjector.getTransactionManager();
    }

    @Test
    public void handleAdvertisements_insertsNewDeviceAndAppInfo() {
        Device device =
                new Device.Builder()
                        .setManufacturer(MANUFACTURER)
                        .setModel(MODEL)
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .setDisplayName(DISPLAY_NAME)
                        .build();
        Set<DeviceDataSourceState> deviceDataSourceStates =
                Set.of(
                        new DeviceDataSourceState.Builder(StepsRecord.class)
                                .setAvailable(true)
                                .build());
        DeviceDataSourceAdvertisement advertisement =
                new DeviceDataSourceAdvertisement(
                        device, DISPLAY_NAME, DEVICE_ID, deviceDataSourceStates);
        long expectedDeviceInfoId = 1;

        mDeviceDataProviderManager.handleAdvertisement(Set.of(advertisement), PACKAGE_NAME);
        Map<String, AppInfoInternal> appInfoInternalMap = mAppInfoHelper.getAppInfoMap();

        assertThat(mDeviceInfoHelper.getIdDeviceInfoMap().size()).isEqualTo(1);
        assertThat(
                        mDeviceInfoHelper
                                .getIdDeviceInfoMap()
                                .get(expectedDeviceInfoId)
                                .getManufacturer())
                .isEqualTo(MANUFACTURER);
        assertThat(mDeviceInfoHelper.getIdDeviceInfoMap().get(expectedDeviceInfoId).getModel())
                .isEqualTo(MODEL);
        assertThat(mDeviceInfoHelper.getIdDeviceInfoMap().get(expectedDeviceInfoId).getDeviceType())
                .isEqualTo(DEVICE_TYPE);
        assertThat(mDeviceInfoHelper.getIdDeviceInfoMap().get(expectedDeviceInfoId).getDeviceId())
                .isEqualTo(DEVICE_ID);
        assertThat(
                        mDeviceInfoHelper
                                .getIdDeviceInfoMap()
                                .get(expectedDeviceInfoId)
                                .getDisplayName())
                .isEqualTo(DISPLAY_NAME);
        assertThat(appInfoInternalMap.size()).isEqualTo(1);
        String appInfoKey = "com.android.healthconnect.phone.d17ebda88781f35c3bb70a5bdd08efc98";
        assertThat(appInfoInternalMap).containsKey(appInfoKey);
        assertThat(appInfoInternalMap.get(appInfoKey).getDeviceInfoId())
                .isEqualTo(expectedDeviceInfoId);
        DeviceDataProviderHelper.DeviceDataProviderKey key =
                new DeviceDataProviderHelper.DeviceDataProviderKey(
                        PACKAGE_NAME, expectedDeviceInfoId, RECORD_TYPE_STEPS);
        assertThat(mDeviceDataProviderHelper.getDdpMap().size()).isEqualTo(1);
        assertThat(mDeviceDataProviderHelper.getDdpMap()).containsKey(key);
        assertThat(mDeviceDataProviderHelper.getDdpMap().get(key).isAvailable()).isEqualTo(true);
        assertThat(mDeviceDataProviderHelper.getDdpMap().get(key).isUserEnabled()).isEqualTo(false);
        assertThat(mDeviceDataProviderHelper.getDdpMap().get(key).isVisibleByDefaultInMatchmaking())
                .isEqualTo(false);
    }

    @Test
    public void handleMultipleAdvertisements_duplicatesAreIdempotent() {
        Device device =
                new Device.Builder()
                        .setManufacturer(MANUFACTURER)
                        .setModel(MODEL)
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .setDisplayName(DISPLAY_NAME)
                        .build();
        Set<DeviceDataSourceState> deviceDataSourceStates =
                Set.of(
                        new DeviceDataSourceState.Builder(StepsRecord.class)
                                .setAvailable(true)
                                .build());
        DeviceDataSourceAdvertisement advertisement =
                new DeviceDataSourceAdvertisement(
                        device, DISPLAY_NAME, DEVICE_ID, deviceDataSourceStates);

        mDeviceDataProviderManager.handleAdvertisement(Set.of(advertisement), PACKAGE_NAME);
        mDeviceDataProviderManager.handleAdvertisement(Set.of(advertisement), PACKAGE_NAME);
        Map<String, AppInfoInternal> appInfoInternalMap = mAppInfoHelper.getAppInfoMap();

        assertThat(mDeviceInfoHelper.getIdDeviceInfoMap().size()).isEqualTo(1);
        assertThat(appInfoInternalMap.size()).isEqualTo(1);
        assertThat(mDeviceDataProviderHelper.getDdpMap().size()).isEqualTo(1);
    }

    @Test
    // TODO(b/440066697): Check how we want to handle display name updates.
    public void handleAdvertisementWithNewDeviceName_savesNewDevice() {
        Device device =
                new Device.Builder()
                        .setManufacturer(MANUFACTURER)
                        .setModel(MODEL)
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .setDisplayName(DISPLAY_NAME)
                        .build();
        String renamedDisplayName = DISPLAY_NAME + "_2";
        Device renamedDevice =
                new Device.Builder()
                        .setManufacturer(MANUFACTURER)
                        .setModel(MODEL)
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .setDisplayName(renamedDisplayName)
                        .build();
        Set<DeviceDataSourceState> deviceDataSourceStates =
                Set.of(
                        new DeviceDataSourceState.Builder(StepsRecord.class)
                                .setAvailable(true)
                                .build());
        DeviceDataSourceAdvertisement advertisement1 =
                new DeviceDataSourceAdvertisement(
                        device, DISPLAY_NAME, DEVICE_ID, deviceDataSourceStates);
        DeviceDataSourceAdvertisement advertisement2 =
                new DeviceDataSourceAdvertisement(
                        renamedDevice, renamedDisplayName, DEVICE_ID, deviceDataSourceStates);
        long expectedDeviceInfoId1 = 1;
        long expectedDeviceInfoId2 = 2;

        mDeviceDataProviderManager.handleAdvertisement(Set.of(advertisement1), PACKAGE_NAME);
        mDeviceDataProviderManager.handleAdvertisement(Set.of(advertisement2), PACKAGE_NAME);
        Map<String, AppInfoInternal> appInfoInternalMap = mAppInfoHelper.getAppInfoMap();

        assertThat(appInfoInternalMap.size()).isEqualTo(1);
        assertThat(mDeviceDataProviderHelper.getDdpMap().size()).isEqualTo(2);
        assertThat(mDeviceInfoHelper.getIdDeviceInfoMap().size()).isEqualTo(2);
        assertThat(
                        mDeviceInfoHelper
                                .getIdDeviceInfoMap()
                                .get(expectedDeviceInfoId1)
                                .getManufacturer())
                .isEqualTo(MANUFACTURER);
        assertThat(mDeviceInfoHelper.getIdDeviceInfoMap().get(expectedDeviceInfoId1).getModel())
                .isEqualTo(MODEL);
        assertThat(
                        mDeviceInfoHelper
                                .getIdDeviceInfoMap()
                                .get(expectedDeviceInfoId1)
                                .getDeviceType())
                .isEqualTo(DEVICE_TYPE);
        assertThat(mDeviceInfoHelper.getIdDeviceInfoMap().get(expectedDeviceInfoId1).getDeviceId())
                .isEqualTo(DEVICE_ID);
        assertThat(
                        mDeviceInfoHelper
                                .getIdDeviceInfoMap()
                                .get(expectedDeviceInfoId1)
                                .getDisplayName())
                .isEqualTo(DISPLAY_NAME);
        assertThat(
                        mDeviceInfoHelper
                                .getIdDeviceInfoMap()
                                .get(expectedDeviceInfoId2)
                                .getManufacturer())
                .isEqualTo(MANUFACTURER);
        assertThat(mDeviceInfoHelper.getIdDeviceInfoMap().get(expectedDeviceInfoId2).getModel())
                .isEqualTo(MODEL);
        assertThat(
                        mDeviceInfoHelper
                                .getIdDeviceInfoMap()
                                .get(expectedDeviceInfoId2)
                                .getDeviceType())
                .isEqualTo(DEVICE_TYPE);
        assertThat(mDeviceInfoHelper.getIdDeviceInfoMap().get(expectedDeviceInfoId2).getDeviceId())
                .isEqualTo(DEVICE_ID);
        assertThat(
                        mDeviceInfoHelper
                                .getIdDeviceInfoMap()
                                .get(expectedDeviceInfoId2)
                                .getDisplayName())
                .isEqualTo(renamedDisplayName);
    }

    public void insertDeviceRecords_insertsRecordCorrectly() {
        Device device =
                new Device.Builder()
                        .setManufacturer(MANUFACTURER)
                        .setModel(MODEL)
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .setDisplayName(DISPLAY_NAME)
                        .build();
        Set<DeviceDataSourceState> deviceDataSourceStates =
                Set.of(
                        new DeviceDataSourceState.Builder(StepsRecord.class)
                                .setAvailable(true)
                                .build());
        DeviceDataSourceAdvertisement advertisement =
                new DeviceDataSourceAdvertisement(
                        device, DISPLAY_NAME, DEVICE_ID, deviceDataSourceStates);
        List<RecordInternal<?>> records =
                List.of(
                        buildStepsRecord(
                                /* startTimeMillis= */ 1000,
                                /* endTimeMillis= */ 2000,
                                /* stepsCount= */ 100));
        mDeviceDataProviderManager.handleAdvertisement(Set.of(advertisement), PACKAGE_NAME);

        List<String> insertedUuids =
                mDeviceDataProviderManager.insertDeviceRecords(DEVICE_ID, records);

        assertThat(insertedUuids).isNotNull();
        assertThat(insertedUuids).hasSize(1);
    }

    public void insertDeviceRecords_verifiesRecordMetadata() {
        Device device =
                new Device.Builder()
                        .setManufacturer(MANUFACTURER)
                        .setModel(MODEL)
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .setDisplayName(DISPLAY_NAME)
                        .build();
        Set<DeviceDataSourceState> deviceDataSourceStates =
                Set.of(
                        new DeviceDataSourceState.Builder(StepsRecord.class)
                                .setAvailable(true)
                                .build());
        DeviceDataSourceAdvertisement advertisement =
                new DeviceDataSourceAdvertisement(
                        device, DISPLAY_NAME, DEVICE_ID, deviceDataSourceStates);
        List<RecordInternal<?>> records =
                List.of(
                        buildStepsRecord(
                                /* startTimeMillis= */ 1000,
                                /* endTimeMillis= */ 2000,
                                /* stepsCount= */ 100));
        mDeviceDataProviderManager.handleAdvertisement(Set.of(advertisement), PACKAGE_NAME);

        List<String> insertedUuids =
                mDeviceDataProviderManager.insertDeviceRecords(DEVICE_ID, records);
        assertThat(insertedUuids).isNotNull();
        assertThat(insertedUuids).hasSize(1);
        List<RecordInternal<?>> readRecords =
                mFitnessRecordReadHelper.readRecords(
                        mTransactionManager,
                        PACKAGE_NAME,
                        ImmutableMap.of(
                                RECORD_TYPE_STEPS,
                                insertedUuids.stream().map(UUID::fromString).toList()),
                        /* grantedExtraReadPermissions= */ Set.of(),
                        /* grantedGranularPermissions= */ Set.of(),
                        /* startDateAccessMillis= */ 0,
                        /* isInForeground= */ false,
                        /* shouldRecordAccessLogs= */ false);

        assertThat(readRecords).hasSize(1);
        RecordInternal<?> readRecord = readRecords.get(0);
        assertThat(readRecord.getRecordType()).isEqualTo(RECORD_TYPE_STEPS);
        assertThat(readRecord.getPackageName())
                .isEqualTo("com.android.healthconnect.phone.d17ebda88781f35c3bb70a5bdd08efc98");
        // RecordHelper#getRecord doesn't repopulate the deviceInfoId
        assertThat(readRecord.getDeviceInfoId()).isEqualTo(-1L);
        assertThat(readRecord.getManufacturer()).isEqualTo(MANUFACTURER);
        assertThat(readRecord.getModel()).isEqualTo(MODEL);
        assertThat(readRecord.getDeviceType()).isEqualTo(DEVICE_TYPE);
        assertThat(readRecord.getDisplayName()).isEqualTo(DISPLAY_NAME);
    }

    public void insertDeviceRecords_insertsRecordsCorrectly() {
        Device device =
                new Device.Builder()
                        .setManufacturer(MANUFACTURER)
                        .setModel(MODEL)
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .setDisplayName(DISPLAY_NAME)
                        .build();
        Set<DeviceDataSourceState> deviceDataSourceStates =
                Set.of(
                        new DeviceDataSourceState.Builder(StepsRecord.class)
                                .setAvailable(true)
                                .build());
        DeviceDataSourceAdvertisement advertisement =
                new DeviceDataSourceAdvertisement(
                        device, DISPLAY_NAME, DEVICE_ID, deviceDataSourceStates);
        List<RecordInternal<?>> records =
                List.of(
                        buildStepsRecord(
                                /* startTimeMillis= */ 1000,
                                /* endTimeMillis= */ 2000,
                                /* stepsCount= */ 100),
                        buildStepsRecord(
                                /* startTimeMillis= */ 2000,
                                /* endTimeMillis= */ 3000,
                                /* stepsCount= */ 200));
        mDeviceDataProviderManager.handleAdvertisement(Set.of(advertisement), PACKAGE_NAME);

        List<String> insertedUuids =
                mDeviceDataProviderManager.insertDeviceRecords(DEVICE_ID, records);

        assertThat(insertedUuids).isNotNull();
        assertThat(insertedUuids).hasSize(2);
    }

    public void insertDeviceRecords_deviceNotFound_throwsException() {
        List<RecordInternal<?>> records = Collections.emptyList();

        Throwable thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                mDeviceDataProviderManager.insertDeviceRecords(
                                        /* deviceId= */ "non_existent_device", records));

        assertThat(thrown)
                .hasMessageThat()
                .contains(
                        "Device with ID non_existent_device not found, ensure the device data"
                                + " source has been advertised");
    }

    public void insertDeviceRecords_doesNotCreateNewDeviceOrAppInfo() {
        Device device =
                new Device.Builder()
                        .setManufacturer(MANUFACTURER)
                        .setModel(MODEL)
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .setDisplayName(DISPLAY_NAME)
                        .build();
        Set<DeviceDataSourceState> deviceDataSourceStates =
                Set.of(
                        new DeviceDataSourceState.Builder(StepsRecord.class)
                                .setAvailable(true)
                                .build());
        DeviceDataSourceAdvertisement advertisement =
                new DeviceDataSourceAdvertisement(
                        device, DISPLAY_NAME, DEVICE_ID, deviceDataSourceStates);
        List<RecordInternal<?>> records =
                List.of(
                        buildStepsRecord(
                                /* startTimeMillis= */ 1000,
                                /* endTimeMillis= */ 2000,
                                /* stepsCount= */ 100));
        mDeviceDataProviderManager.handleAdvertisement(Set.of(advertisement), PACKAGE_NAME);
        int initialDeviceInfoCount = mDeviceInfoHelper.getIdDeviceInfoMap().size();
        int initialAppInfoCount = mAppInfoHelper.getAppInfoMap().size();

        mDeviceDataProviderManager.insertDeviceRecords(DEVICE_ID, records);

        assertThat(mDeviceInfoHelper.getIdDeviceInfoMap().size()).isEqualTo(initialDeviceInfoCount);
        assertThat(mAppInfoHelper.getAppInfoMap().size()).isEqualTo(initialAppInfoCount);
    }

    public void advertisementAndNormalInsertion_createsTwoDistinctDeviceInfoEntries() {
        Device device =
                new Device.Builder()
                        .setManufacturer(MANUFACTURER)
                        .setModel(MODEL)
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .setDisplayName(DISPLAY_NAME)
                        .build();
        // Simulate a normal insertion (without a device ID)
        DeviceInfoHelper.DeviceInfo normalDeviceInfo =
                new DeviceInfoHelper.DeviceInfo(
                        MANUFACTURER, MODEL, DEVICE_TYPE, /* deviceId= */ null, DISPLAY_NAME);
        mDeviceInfoHelper.insertIfNotPresent(normalDeviceInfo);
        // Simulate a DDP insertion (with a device ID)
        Set<DeviceDataSourceState> deviceDataSourceStates =
                Set.of(
                        new DeviceDataSourceState.Builder(StepsRecord.class)
                                .setAvailable(true)
                                .build());
        DeviceDataSourceAdvertisement advertisement =
                new DeviceDataSourceAdvertisement(
                        device, DISPLAY_NAME, DEVICE_ID, deviceDataSourceStates);
        mDeviceDataProviderManager.handleAdvertisement(Set.of(advertisement), PACKAGE_NAME);

        assertThat(mDeviceInfoHelper.getIdDeviceInfoMap().size()).isEqualTo(2);
        List<DeviceInfoHelper.DeviceInfo> deviceInfos =
                mDeviceInfoHelper.getIdDeviceInfoMap().values().stream().toList();
        DeviceInfoHelper.DeviceInfo deviceInfo1 = deviceInfos.get(0);
        DeviceInfoHelper.DeviceInfo deviceInfo2 = deviceInfos.get(1);

        assertThat(deviceInfo1.getDeviceId()).isNull();
        assertThat(deviceInfo2.getDeviceId()).isEqualTo(DEVICE_ID);
        assertThat(deviceInfo1.getManufacturer()).isEqualTo(MANUFACTURER);
        assertThat(deviceInfo1.getModel()).isEqualTo(MODEL);
        assertThat(deviceInfo1.getDeviceType()).isEqualTo(DEVICE_TYPE);
        assertThat(deviceInfo1.getDisplayName()).isEqualTo(DISPLAY_NAME);
        assertThat(deviceInfo2.getManufacturer()).isEqualTo(MANUFACTURER);
        assertThat(deviceInfo2.getModel()).isEqualTo(MODEL);
        assertThat(deviceInfo2.getDeviceType()).isEqualTo(DEVICE_TYPE);
        assertThat(deviceInfo2.getDisplayName()).isEqualTo(DISPLAY_NAME);
    }

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.BAKLAVA)
    public void isPermittedToProvideDeviceData_baklavaAndLower_withManagePermission_returnsTrue() {
        doReturn(PackageManager.PERMISSION_DENIED)
                .when(mContext)
                .checkPermission(
                        Manifest.permission.PROVIDE_HEALTH_CONNECT_DEVICE_DATA,
                        /* pid= */ 0,
                        /* uid= */ 0);
        doReturn(PackageManager.PERMISSION_GRANTED)
                .when(mContext)
                .checkPermission(
                        HealthPermissions.MANAGE_HEALTH_DATA_PERMISSION,
                        /* pid= */ 0,
                        /* uid= */ 0);

        assertThat(
                        mDeviceDataProviderManager.isPermittedToProvideDeviceData(
                                PACKAGE_NAME, /* uid= */ 0, /* pid= */ 0))
                .isTrue();
    }

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.BAKLAVA)
    public void isPermittedToProvideDeviceData_baklavaAndLower_noPermission_returnsFalse() {
        doReturn(PackageManager.PERMISSION_DENIED)
                .when(mContext)
                .checkPermission(
                        Manifest.permission.PROVIDE_HEALTH_CONNECT_DEVICE_DATA,
                        /* pid= */ 0,
                        /* uid= */ 0);
        doReturn(PackageManager.PERMISSION_DENIED)
                .when(mContext)
                .checkPermission(
                        HealthPermissions.MANAGE_HEALTH_DATA_PERMISSION,
                        /* pid= */ 0,
                        /* uid= */ 0);

        assertThat(
                        mDeviceDataProviderManager.isPermittedToProvideDeviceData(
                                PACKAGE_NAME, /* uid= */ 0, /* pid= */ 0))
                .isFalse();
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA + 1)
    public void isPermittedToProvideDeviceData_postBaklava_withProvidePermission_returnsTrue() {
        doReturn(PackageManager.PERMISSION_GRANTED)
                .when(mContext)
                .checkPermission(
                        Manifest.permission.PROVIDE_HEALTH_CONNECT_DEVICE_DATA,
                        /* pid= */ 0,
                        /* uid= */ 0);
        doReturn(PackageManager.PERMISSION_DENIED)
                .when(mContext)
                .checkPermission(
                        HealthPermissions.MANAGE_HEALTH_DATA_PERMISSION,
                        /* pid= */ 0,
                        /* uid= */ 0);

        assertThat(
                        mDeviceDataProviderManager.isPermittedToProvideDeviceData(
                                PACKAGE_NAME, /* uid= */ 0, /* pid= */ 0))
                .isTrue();
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA + 1)
    public void isPermittedToProvideDeviceData_postBaklava_withManagePermission_returnsFalse() {
        doReturn(PackageManager.PERMISSION_DENIED)
                .when(mContext)
                .checkPermission(
                        Manifest.permission.PROVIDE_HEALTH_CONNECT_DEVICE_DATA,
                        /* pid= */ 0,
                        /* uid= */ 0);
        doReturn(PackageManager.PERMISSION_GRANTED)
                .when(mContext)
                .checkPermission(
                        HealthPermissions.MANAGE_HEALTH_DATA_PERMISSION,
                        /* pid= */ 0,
                        /* uid= */ 0);

        assertThat(
                        mDeviceDataProviderManager.isPermittedToProvideDeviceData(
                                PACKAGE_NAME, /* uid= */ 0, /* pid= */ 0))
                .isFalse();
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA + 1)
    public void isPermittedToProvideDeviceData_postBaklava_noPermission_returnsFalse() {
        doReturn(PackageManager.PERMISSION_DENIED)
                .when(mContext)
                .checkPermission(
                        Manifest.permission.PROVIDE_HEALTH_CONNECT_DEVICE_DATA,
                        /* pid= */ 0,
                        /* uid= */ 0);
        doReturn(PackageManager.PERMISSION_DENIED)
                .when(mContext)
                .checkPermission(
                        HealthPermissions.MANAGE_HEALTH_DATA_PERMISSION,
                        /* pid= */ 0,
                        /* uid= */ 0);

        assertThat(
                        mDeviceDataProviderManager.isPermittedToProvideDeviceData(
                                PACKAGE_NAME, /* uid= */ 0, /* pid= */ 0))
                .isFalse();
    }

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.BAKLAVA)
    public void isPermittedToProvideDeviceData_baklavaAndLower_aRPackage_returnsTrue() {
        doReturn(PackageManager.PERMISSION_DENIED)
                .when(mContext)
                .checkPermission(
                        HealthPermissions.MANAGE_HEALTH_DATA_PERMISSION,
                        /* pid= */ 0,
                        /* uid= */ 0);

        final String systemActivityRecognizerPackage;
        final int resourceId =
                Resources.getSystem()
                        .getIdentifier("config_systemActivityRecognizer", "string", "android");
        if (resourceId != 0) {
            systemActivityRecognizerPackage = Resources.getSystem().getString(resourceId);
        } else {
            systemActivityRecognizerPackage = null;
        }

        if (systemActivityRecognizerPackage == null || systemActivityRecognizerPackage.isEmpty()) {
            return; // Test passes if there is no package to check
        }

        assertThat(
                        mDeviceDataProviderManager.isPermittedToProvideDeviceData(
                                systemActivityRecognizerPackage, /* uid= */ 0, /* pid= */ 0))
                .isTrue();
    }
}
