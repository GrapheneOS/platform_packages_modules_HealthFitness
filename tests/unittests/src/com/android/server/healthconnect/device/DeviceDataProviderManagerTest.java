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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
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
import android.health.connect.datatypes.DistanceRecord;
import android.health.connect.datatypes.StepsRecord;
import android.health.connect.device.DeviceDataAdvertisement;
import android.health.connect.device.DeviceDataTypeAdvertisement;
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
import com.android.server.healthconnect.common.metadata.SyntheticPackageNameCreator;
import com.android.server.healthconnect.common.preferences.PreferenceHelper;
import com.android.server.healthconnect.fitness.FitnessRecordReadHelper;
import com.android.server.healthconnect.fitness.helpers.DeviceDataProviderMetadataHelper;
import com.android.server.healthconnect.fitness.helpers.DeviceDataSourcesHelper;
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
    private static final String PREFERENCE_KEY =
            SyntheticPackageNameCreator.SYNTHETIC_PACKAGE_NAME_SALT_PREFERENCE_KEY;
    private static final int DEVICE_TYPE = DEVICE_TYPE_PHONE;

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Rule public final TemporaryFolder mEnvironmentDataDir = new TemporaryFolder();

    private PreferenceHelper mPreferenceHelper;
    private Context mContext;
    private DeviceDataSourcesHelper mDeviceDataSourcesHelper;
    private DeviceDataProviderMetadataHelper mDeviceDataProviderMetadataHelper;
    private DeviceInfoHelper mDeviceInfoHelper;
    private AppInfoHelper mAppInfoHelper;
    private FitnessRecordReadHelper mFitnessRecordReadHelper;
    private TransactionManager mTransactionManager;
    private FakeSerialDeviceDataProviderManager mDeviceDataProviderManager;

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
        mDeviceDataSourcesHelper = healthConnectInjector.getDeviceDataSourcesHelper();
        mDeviceDataProviderMetadataHelper =
                healthConnectInjector.getDeviceDataProviderMetadataHelper();
        mPreferenceHelper = healthConnectInjector.getPreferenceHelper();
        mFitnessRecordReadHelper = healthConnectInjector.getFitnessRecordReadHelper();
        mTransactionManager = healthConnectInjector.getTransactionManager();
        mDeviceDataProviderManager =
                new FakeSerialDeviceDataProviderManager(
                        mContext,
                        mDeviceInfoHelper,
                        mAppInfoHelper,
                        mDeviceDataSourcesHelper,
                        mDeviceDataProviderMetadataHelper,
                        healthConnectInjector.getFitnessRecordUpsertHelper(),
                        healthConnectInjector.getSyntheticPackageNameCreator());
        mPreferenceHelper.insertOrReplacePreference(PREFERENCE_KEY, "Some Salt");
    }

    @Test
    public void handleAdvertisements_insertsNewDeviceAndAppInfoAndMetadata() {
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
                                .build());
        DeviceDataAdvertisement advertisement =
                new DeviceDataAdvertisement(device, DEVICE_ID, deviceDataTypeAdvertisements);
        long expectedDeviceInfoId = 1;

        mDeviceDataProviderManager.handleAdvertisement(Set.of(advertisement), PACKAGE_NAME);
        Map<String, AppInfoInternal> appInfoInternalMap = mAppInfoHelper.getAppInfoMap();
        Map<Long, DeviceDataProviderMetadataHelper.DeviceDataProviderMetadata> metadataInternalMap =
                mDeviceDataProviderMetadataHelper.getIdDeviceDataProviderMetadataMap();

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
        String appInfoKey = "com.android.healthconnect.phone.d917cfe4687a83c6da4ecca162a5ba400";
        assertThat(appInfoInternalMap).containsKey(appInfoKey);
        assertThat(appInfoInternalMap.get(appInfoKey).getDeviceInfoId())
                .isEqualTo(expectedDeviceInfoId);
        assertThat(metadataInternalMap.size()).isEqualTo(1);
        assertThat(metadataInternalMap).containsKey(1L);
        assertThat(metadataInternalMap.get(1L).sourcePackageName()).isEqualTo(PACKAGE_NAME);
        DeviceDataSourcesHelper.DeviceDataProviderKey key =
                new DeviceDataSourcesHelper.DeviceDataProviderKey(
                        PACKAGE_NAME, expectedDeviceInfoId, RECORD_TYPE_STEPS);
        assertThat(mDeviceDataSourcesHelper.getDdpMap().size()).isEqualTo(1);
        assertThat(mDeviceDataSourcesHelper.getDdpMap()).containsKey(key);
        assertThat(mDeviceDataSourcesHelper.getDdpMap().get(key).isAvailable()).isEqualTo(true);
        assertThat(mDeviceDataSourcesHelper.getDdpMap().get(key).isUserEnabled()).isEqualTo(false);
        assertThat(mDeviceDataSourcesHelper.getDdpMap().get(key).isVisibleByDefaultInMatchmaking())
                .isEqualTo(true);
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
        Set<DeviceDataTypeAdvertisement> deviceDataTypeAdvertisements =
                Set.of(
                        new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                                .setAvailable(true)
                                .build());
        DeviceDataAdvertisement advertisement =
                new DeviceDataAdvertisement(device, DEVICE_ID, deviceDataTypeAdvertisements);

        mDeviceDataProviderManager.handleAdvertisement(Set.of(advertisement), PACKAGE_NAME);
        mDeviceDataProviderManager.handleAdvertisement(Set.of(advertisement), PACKAGE_NAME);
        Map<String, AppInfoInternal> appInfoInternalMap = mAppInfoHelper.getAppInfoMap();
        Map<Long, DeviceDataProviderMetadataHelper.DeviceDataProviderMetadata> metadataInternalMap =
                mDeviceDataProviderMetadataHelper.getIdDeviceDataProviderMetadataMap();

        assertThat(mDeviceInfoHelper.getIdDeviceInfoMap().size()).isEqualTo(1);
        assertThat(appInfoInternalMap.size()).isEqualTo(1);
        assertThat(mDeviceDataSourcesHelper.getDdpMap().size()).isEqualTo(1);
        assertThat(metadataInternalMap.size()).isEqualTo(1);
    }

    @Test
    public void handleAdvertisement_deviceIdUsedForDifferentDeviceType_throwsException() {
        Device device1 =
                new Device.Builder()
                        .setManufacturer(MANUFACTURER)
                        .setModel(MODEL)
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .setDisplayName(DISPLAY_NAME)
                        .build();
        Set<DeviceDataTypeAdvertisement> deviceDataTypeAdvertisements1 =
                Set.of(
                        new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                                .setAvailable(true)
                                .build());
        DeviceDataAdvertisement advertisement1 =
                new DeviceDataAdvertisement(device1, DEVICE_ID, deviceDataTypeAdvertisements1);
        mDeviceDataProviderManager.handleAdvertisement(Set.of(advertisement1), PACKAGE_NAME);

        Device device2 =
                new Device.Builder()
                        .setManufacturer(MANUFACTURER)
                        .setModel(MODEL)
                        .setType(Device.DEVICE_TYPE_WATCH)
                        .setDisplayName(DISPLAY_NAME)
                        .build();
        Set<DeviceDataTypeAdvertisement> deviceDataTypeAdvertisements2 =
                Set.of(
                        new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                                .setAvailable(true)
                                .build());
        DeviceDataAdvertisement advertisement2 =
                new DeviceDataAdvertisement(device2, DEVICE_ID, deviceDataTypeAdvertisements2);

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        mDeviceDataProviderManager.handleAdvertisement(
                                Set.of(advertisement2), PACKAGE_NAME));
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
        Set<DeviceDataTypeAdvertisement> deviceDataTypeAdvertisements =
                Set.of(
                        new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                                .setAvailable(true)
                                .build());
        DeviceDataAdvertisement advertisement1 =
                new DeviceDataAdvertisement(device, DEVICE_ID, deviceDataTypeAdvertisements);
        DeviceDataAdvertisement advertisement2 =
                new DeviceDataAdvertisement(renamedDevice, DEVICE_ID, deviceDataTypeAdvertisements);
        long expectedDeviceInfoId1 = 1;
        long expectedDeviceInfoId2 = 2;

        mDeviceDataProviderManager.handleAdvertisement(Set.of(advertisement1), PACKAGE_NAME);
        mDeviceDataProviderManager.handleAdvertisement(Set.of(advertisement2), PACKAGE_NAME);
        Map<String, AppInfoInternal> appInfoInternalMap = mAppInfoHelper.getAppInfoMap();
        Map<Long, DeviceDataProviderMetadataHelper.DeviceDataProviderMetadata> metadataInternalMap =
                mDeviceDataProviderMetadataHelper.getIdDeviceDataProviderMetadataMap();

        assertThat(appInfoInternalMap.size()).isEqualTo(1);
        assertThat(metadataInternalMap.size()).isEqualTo(1);
        assertThat(mDeviceDataSourcesHelper.getDdpMap().size()).isEqualTo(1);
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

    @Test
    public void withoutInit_getStableCurrentDeviceId_throws() {
        assertThrows(
                IllegalStateException.class,
                () -> mDeviceDataProviderManager.getStableCurrentDeviceId());
    }

    @Test
    public void withRegularCall_getStableCurrentDeviceId_doesNotContainSerial() {
        mDeviceDataProviderManager.initializeOrRefreshCurrentDeviceIds();
        String deviceId = mDeviceDataProviderManager.getStableCurrentDeviceId();
        String fakeSerial = FakeSerialDeviceDataProviderManager.TEST_SERIAL_NUMBER;

        assertFalse(deviceId.contains(fakeSerial));
    }

    @Test
    public void withRegularCall_getStableCurrentDeviceId_isCanonicalSpn() {
        mDeviceDataProviderManager.initializeOrRefreshCurrentDeviceIds();
        String deviceId = mDeviceDataProviderManager.getStableCurrentDeviceId();

        assertTrue(SyntheticPackageNameCreator.isCanonicalSpn(deviceId));
    }

    @Test
    public void withMultipleCalls_getStableCurrentDeviceId_returnsSameDeviceId() {
        mDeviceDataProviderManager.initializeOrRefreshCurrentDeviceIds();
        String firstId = mDeviceDataProviderManager.getStableCurrentDeviceId();
        String secondId = mDeviceDataProviderManager.getStableCurrentDeviceId();

        assertEquals(firstId, secondId);
    }

    @Test
    public void withMultipleCallsAndSoftRefresh_getStableCurrentDeviceId_returnsSameDeviceId() {
        mDeviceDataProviderManager.initializeOrRefreshCurrentDeviceIds();
        String firstId = mDeviceDataProviderManager.getStableCurrentDeviceId();

        mDeviceDataProviderManager.initializeOrRefreshCurrentDeviceIds();
        String secondId = mDeviceDataProviderManager.getStableCurrentDeviceId();

        assertEquals(firstId, secondId);
    }

    @Test
    public void withMultipleCallsAndHardReset_getStableCurrentDeviceId_returnsDifferentDeviceId() {
        mDeviceDataProviderManager.initializeOrRefreshCurrentDeviceIds();
        String firstId = mDeviceDataProviderManager.getStableCurrentDeviceId();

        mPreferenceHelper.removeKey(PREFERENCE_KEY);
        assertNull(mPreferenceHelper.getPreference(PREFERENCE_KEY));
        mDeviceDataProviderManager.initializeOrRefreshCurrentDeviceIds();

        String secondId = mDeviceDataProviderManager.getStableCurrentDeviceId();

        assertNotEquals(firstId, secondId);
    }

    @Test
    public void withRegularCall_getCurrentDeviceId_isCanonicalSpn() {
        mDeviceDataProviderManager.initializeOrRefreshCurrentDeviceIds();
        String deviceId = mDeviceDataProviderManager.getCurrentDeviceId();

        assertTrue(SyntheticPackageNameCreator.isCanonicalSpn(deviceId));
    }

    @Test
    public void withMultipleCalls_getCurrentDeviceId_returnsSameDeviceId() {
        mDeviceDataProviderManager.initializeOrRefreshCurrentDeviceIds();

        String firstId = mDeviceDataProviderManager.getCurrentDeviceId();
        String secondId = mDeviceDataProviderManager.getCurrentDeviceId();

        assertEquals(firstId, secondId);
    }

    @Test
    public void withMultipleCallsAndRefresh_getCurrentDeviceId_returnsDifferentDeviceId() {
        mDeviceDataProviderManager.initializeOrRefreshCurrentDeviceIds();
        String firstId = mDeviceDataProviderManager.getCurrentDeviceId();

        mDeviceDataProviderManager.initializeOrRefreshCurrentDeviceIds();
        String secondId = mDeviceDataProviderManager.getCurrentDeviceId();

        assertNotEquals(firstId, secondId);
    }

    @Test
    public void withoutInitialization_getCurrentDeviceId_throws() {
        assertThrows(
                IllegalStateException.class, () -> mDeviceDataProviderManager.getCurrentDeviceId());
    }

    @Test
    public void insertDeviceRecords_insertsRecordCorrectly() {
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
                                .build());
        DeviceDataAdvertisement advertisement =
                new DeviceDataAdvertisement(device, DEVICE_ID, deviceDataTypeAdvertisements);
        List<RecordInternal<?>> records =
                List.of(
                        buildStepsRecord(
                                /* startTimeMillis= */ 1000,
                                /* endTimeMillis= */ 2000,
                                /* stepsCount= */ 100));
        mDeviceDataProviderManager.handleAdvertisement(Set.of(advertisement), PACKAGE_NAME);

        List<String> insertedUuids =
                mDeviceDataProviderManager.insertDeviceRecords(PACKAGE_NAME, DEVICE_ID, records);

        assertThat(insertedUuids).isNotNull();
        assertThat(insertedUuids).hasSize(1);
    }

    @Test
    public void insertDeviceRecords_dataTypeNotAdvertised_throwsException() {
        Device device =
                new Device.Builder()
                        .setManufacturer(MANUFACTURER)
                        .setModel(MODEL)
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .setDisplayName(DISPLAY_NAME)
                        .build();
        Set<DeviceDataTypeAdvertisement> deviceDataTypeAdvertisements =
                Set.of(
                        new DeviceDataTypeAdvertisement.Builder(DistanceRecord.class)
                                .setAvailable(true)
                                .build());
        DeviceDataAdvertisement advertisement =
                new DeviceDataAdvertisement(device, DEVICE_ID, deviceDataTypeAdvertisements);
        mDeviceDataProviderManager.handleAdvertisement(Set.of(advertisement), PACKAGE_NAME);

        List<RecordInternal<?>> records =
                List.of(
                        buildStepsRecord(
                                /* startTimeMillis= */ 1000,
                                /* endTimeMillis= */ 2000,
                                /* stepsCount= */ 100));
        Throwable thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                mDeviceDataProviderManager.insertDeviceRecords(
                                        PACKAGE_NAME, DEVICE_ID, records));

        assertThat(thrown)
                .hasMessageThat()
                .contains("The device with id test_device_id was not advertised for data type 1");
    }

    @Test
    public void insertDeviceRecords_verifiesRecordMetadata() {
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
                                .build());
        DeviceDataAdvertisement advertisement =
                new DeviceDataAdvertisement(device, DEVICE_ID, deviceDataTypeAdvertisements);
        List<RecordInternal<?>> records =
                List.of(
                        buildStepsRecord(
                                /* startTimeMillis= */ 1000,
                                /* endTimeMillis= */ 2000,
                                /* stepsCount= */ 100));
        mDeviceDataProviderManager.handleAdvertisement(Set.of(advertisement), PACKAGE_NAME);

        List<String> insertedUuids =
                mDeviceDataProviderManager.insertDeviceRecords(PACKAGE_NAME, DEVICE_ID, records);
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
        assertTrue(SyntheticPackageNameCreator.isCanonicalSpn(readRecord.getPackageName()));
        // RecordHelper#getRecord doesn't repopulate the deviceInfoId
        assertThat(readRecord.getDeviceInfoId()).isEqualTo(-1L);
        assertThat(readRecord.getManufacturer()).isEqualTo(MANUFACTURER);
        assertThat(readRecord.getModel()).isEqualTo(MODEL);
        assertThat(readRecord.getDeviceType()).isEqualTo(DEVICE_TYPE);
        assertThat(readRecord.getDisplayName()).isEqualTo(DISPLAY_NAME);
        assertThat(readRecord.getDeviceDataProviderId()).isEqualTo(1L);
    }

    @Test
    public void insertDeviceRecords_insertsRecordsCorrectly() {
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
                                .build());
        DeviceDataAdvertisement advertisement =
                new DeviceDataAdvertisement(device, DEVICE_ID, deviceDataTypeAdvertisements);
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
                mDeviceDataProviderManager.insertDeviceRecords(PACKAGE_NAME, DEVICE_ID, records);

        assertThat(insertedUuids).isNotNull();
        assertThat(insertedUuids).hasSize(2);
    }

    @Test
    public void insertDeviceRecords_deviceNotFound_throwsException() {
        List<RecordInternal<?>> records =
                List.of(
                        buildStepsRecord(
                                /* startTimeMillis= */ 1000,
                                /* endTimeMillis= */ 2000,
                                /* stepsCount= */ 100));

        Throwable thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                mDeviceDataProviderManager.insertDeviceRecords(
                                        PACKAGE_NAME, DEVICE_ID, records));

        assertThat(thrown)
                .hasMessageThat()
                .contains(
                        "appInfoId not found for calling package com.example.app, ensure an"
                                + " advertisement has been made");
    }

    @Test
    public void insertDeviceRecords_doesNotCreateNewDeviceOrAppInfoOrMetadata() {
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
                                .build());
        DeviceDataAdvertisement advertisement =
                new DeviceDataAdvertisement(device, DEVICE_ID, deviceDataTypeAdvertisements);
        List<RecordInternal<?>> records =
                List.of(
                        buildStepsRecord(
                                /* startTimeMillis= */ 1000,
                                /* endTimeMillis= */ 2000,
                                /* stepsCount= */ 100));
        mDeviceDataProviderManager.handleAdvertisement(Set.of(advertisement), PACKAGE_NAME);
        int initialDeviceInfoCount = mDeviceInfoHelper.getIdDeviceInfoMap().size();
        int initialAppInfoCount = mAppInfoHelper.getAppInfoMap().size();
        int initialMetadataCount =
                mDeviceDataProviderMetadataHelper.getIdDeviceDataProviderMetadataMap().size();

        mDeviceDataProviderManager.insertDeviceRecords(PACKAGE_NAME, DEVICE_ID, records);

        assertThat(mDeviceInfoHelper.getIdDeviceInfoMap().size()).isEqualTo(initialDeviceInfoCount);
        assertThat(mAppInfoHelper.getAppInfoMap().size()).isEqualTo(initialAppInfoCount);
        assertThat(mDeviceDataProviderMetadataHelper.getIdDeviceDataProviderMetadataMap().size())
                .isEqualTo(initialMetadataCount);
    }

    @Test
    public void advertisementAndNormalInsertion_createsTwoDistinctDeviceInfoEntries() {
        Device device =
                new Device.Builder()
                        .setManufacturer(MANUFACTURER)
                        .setModel(MODEL)
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .setDisplayName(DISPLAY_NAME)
                        .build();
        DeviceInfoHelper.DeviceInfo normalDeviceInfo =
                new DeviceInfoHelper.DeviceInfo(
                        MANUFACTURER, MODEL, DEVICE_TYPE, /* deviceId= */ null, DISPLAY_NAME);
        mDeviceInfoHelper.insertIfNotPresent(normalDeviceInfo);
        Set<DeviceDataTypeAdvertisement> deviceDataTypeAdvertisements =
                Set.of(
                        new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                                .setAvailable(true)
                                .build());
        DeviceDataAdvertisement advertisement =
                new DeviceDataAdvertisement(device, DEVICE_ID, deviceDataTypeAdvertisements);
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
    public void updateDeviceRecords_updatesRecordCorrectly() {
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
                                .build());
        DeviceDataAdvertisement advertisement =
                new DeviceDataAdvertisement(device, DEVICE_ID, deviceDataTypeAdvertisements);
        List<RecordInternal<?>> records =
                List.of(
                        buildStepsRecord(
                                /* startTimeMillis= */ 1000,
                                /* endTimeMillis= */ 2000,
                                /* stepsCount= */ 100));
        mDeviceDataProviderManager.handleAdvertisement(Set.of(advertisement), PACKAGE_NAME);
        List<String> insertedUuids =
                mDeviceDataProviderManager.insertDeviceRecords(PACKAGE_NAME, DEVICE_ID, records);
        assertThat(insertedUuids).isNotNull();
        assertThat(insertedUuids).hasSize(1);

        RecordInternal<?> updatedRecord =
                buildStepsRecord(
                        /* startTimeMillis= */ 1000,
                        /* endTimeMillis= */ 2000,
                        /* stepsCount= */ 200);
        updatedRecord.setUuid(UUID.fromString(insertedUuids.get(0)));

        mDeviceDataProviderManager.updateDeviceRecords(
                PACKAGE_NAME, DEVICE_ID, List.of(updatedRecord));

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
        StepsRecord stepsRecord =
                ((android.health.connect.internal.datatypes.StepsRecordInternal) readRecords.get(0))
                        .toExternalRecord();
        assertThat(stepsRecord.getCount()).isEqualTo(200);
    }

    @Test
    public void updateDeviceRecords_uuidNotFound_throwsException() {
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
                                .build());
        DeviceDataAdvertisement advertisement =
                new DeviceDataAdvertisement(device, DEVICE_ID, deviceDataTypeAdvertisements);
        mDeviceDataProviderManager.handleAdvertisement(Set.of(advertisement), PACKAGE_NAME);

        RecordInternal<?> record =
                buildStepsRecord(
                        /* startTimeMillis= */ 1000,
                        /* endTimeMillis= */ 2000,
                        /* stepsCount= */ 200);
        record.setUuid(UUID.randomUUID());

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        mDeviceDataProviderManager.updateDeviceRecords(
                                PACKAGE_NAME, DEVICE_ID, List.of(record)));
    }

    @Test
    public void updateDeviceRecords_incorrectUuid_throwsException() {
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
                                .build());
        DeviceDataAdvertisement advertisement =
                new DeviceDataAdvertisement(device, DEVICE_ID, deviceDataTypeAdvertisements);
        List<RecordInternal<?>> records =
                List.of(
                        buildStepsRecord(
                                /* startTimeMillis= */ 1000,
                                /* endTimeMillis= */ 2000,
                                /* stepsCount= */ 100));
        mDeviceDataProviderManager.handleAdvertisement(Set.of(advertisement), PACKAGE_NAME);
        mDeviceDataProviderManager.insertDeviceRecords(PACKAGE_NAME, DEVICE_ID, records);

        RecordInternal<?> updatedRecord =
                buildStepsRecord(
                        /* startTimeMillis= */ 1000,
                        /* endTimeMillis= */ 2000,
                        /* stepsCount= */ 200);
        updatedRecord.setUuid(UUID.randomUUID());

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        mDeviceDataProviderManager.updateDeviceRecords(
                                PACKAGE_NAME, DEVICE_ID, List.of(updatedRecord)));
    }

    @Test
    public void updateDeviceRecords_deviceNotFound_throwsException() {
        RecordInternal<?> updatedRecord =
                buildStepsRecord(
                        /* startTimeMillis= */ 1000,
                        /* endTimeMillis= */ 2000,
                        /* stepsCount= */ 200);
        updatedRecord.setUuid(UUID.randomUUID());

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        mDeviceDataProviderManager.updateDeviceRecords(
                                PACKAGE_NAME, "non_existent_device", List.of(updatedRecord)));
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
