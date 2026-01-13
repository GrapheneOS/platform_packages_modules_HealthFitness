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
package android.healthconnect.cts.device;

import static android.health.connect.HealthPermissions.READ_DISTANCE;
import static android.health.connect.HealthPermissions.READ_NUTRITION;
import static android.health.connect.HealthPermissions.READ_STEPS;
import static android.health.connect.HealthPermissions.READ_SYMPTOM_COUGH;
import static android.health.connect.datatypes.SymptomRecord.SYMPTOM_TYPE_UNKNOWN;
import static android.healthconnect.testing.cts.HealthConnectReceiver.callAndGetResponseWithShellPermissionIdentity;
import static android.healthconnect.testing.cts.TestOutcomeReceiver.outcomeExecutor;

import static com.android.healthfitness.flags.Flags.FLAG_DEVICE_DATA_PROVIDERS_API;
import static com.android.healthfitness.flags.Flags.FLAG_DEVICE_DATA_PROVIDERS_DB;

import static com.google.common.truth.Truth.assertThat;

import android.health.connect.DeviceDataSource;
import android.health.connect.DeviceDataTypeSource;
import android.health.connect.GetDeviceDataSourcesResponse;
import android.health.connect.HealthPermissions;
import android.health.connect.datatypes.Device;
import android.health.connect.datatypes.NutritionRecord;
import android.health.connect.datatypes.StepsRecord;
import android.health.connect.device.DeviceDataAdvertisement;
import android.health.connect.device.DeviceDataTypeAdvertisement;
import android.healthconnect.testing.cts.HealthConnectReceiver;
import android.healthconnect.testing.cts.TestUtils;
import android.healthconnect.testing.shared.AssumptionCheckerRule;
import android.healthconnect.testing.shared.DeviceSupportUtils;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Optional;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
@RequiresFlagsEnabled({
    FLAG_DEVICE_DATA_PROVIDERS_API,
    FLAG_DEVICE_DATA_PROVIDERS_DB,
})
public class CurrentDeviceDataSourceTest {

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Rule
    public AssumptionCheckerRule mSupportedHardwareRule =
            new AssumptionCheckerRule(
                    DeviceSupportUtils::isHealthConnectFullySupported,
                    "Tests should run on supported hardware only.");

    private static final Device TEST_DEVICE =
            new Device.Builder()
                    .setManufacturer("TestManufacturer")
                    .setModel("TestModel")
                    .setType(Device.DEVICE_TYPE_PHONE)
                    .setDisplayName("TestDisplayName")
                    .build();

    @Before
    public void before() throws Exception {
        TestUtils.deleteAllDataFromHealthConnect();
    }

    @After
    public void after() throws InterruptedException {
        TestUtils.deleteAllDataFromHealthConnect();
    }

    @Test
    public void noCallerAdvertisements_returnsCurrentDeviceWitSystemDataTypeSources()
            throws InterruptedException {
        String currentDeviceId = TestUtils.getCurrentDeviceId();
        TestUtils.verifyGetCurrentDeviceDataSourceWithPermission(
                dataSource -> {
                    assertThat(dataSource.getDeviceDataTypeSources()).hasSize(1);
                    DeviceDataTypeSource currentTypeSource =
                            dataSource.getDeviceDataTypeSources().iterator().next();

                    assertThat(currentTypeSource.isAvailable()).isTrue();
                    assertThat(currentTypeSource.getSymptomType()).isEqualTo(SYMPTOM_TYPE_UNKNOWN);
                    assertThat(currentTypeSource.getDataType()).isEqualTo(StepsRecord.class);

                    assertThat(dataSource.getDeviceDataOrigin().getPackageName())
                            .isEqualTo(currentDeviceId);

                    assertThat(dataSource.getDevice().getModel()).isNotNull();
                    assertThat(dataSource.getDevice().getManufacturer()).isNotNull();
                    assertThat(dataSource.getDevice().getType())
                            .isEqualTo(Device.DEVICE_TYPE_PHONE);
                    assertThat(dataSource.getDevice().getDisplayName()).isNotNull();
                },
                READ_STEPS);
    }

    @Test
    public void withReadPermission_returnsDevice() throws InterruptedException {
        String currentDeviceId = TestUtils.getCurrentDeviceId();
        TestUtils.verifyGetCurrentDeviceDataSourceWithPermission(
                dataSource -> {
                    assertThat(dataSource.getDeviceDataOrigin().getPackageName())
                            .isEqualTo(currentDeviceId);

                    assertThat(dataSource.getDevice().getModel()).isNotNull();
                    assertThat(dataSource.getDevice().getManufacturer()).isNotNull();
                    assertThat(dataSource.getDevice().getType())
                            .isEqualTo(Device.DEVICE_TYPE_PHONE);
                    assertThat(dataSource.getDevice().getDisplayName()).isNotNull();

                    DeviceDataTypeSource typeSource =
                            dataSource.getDeviceDataTypeSources().iterator().next();
                    assertThat(typeSource.getDataType()).isEqualTo(StepsRecord.class);
                    assertThat(typeSource.isAvailable()).isTrue();
                    assertThat(typeSource.isUserEnabled()).isTrue();
                },
                READ_STEPS);
    }

    @Test
    public void withExtraPermissions_returnsDevice() throws InterruptedException {
        String currentDeviceId = TestUtils.getCurrentDeviceId();
        Set<DeviceDataTypeAdvertisement> ads =
                Set.of(
                        new DeviceDataTypeAdvertisement.Builder(NutritionRecord.class)
                                .setAvailable(true)
                                .setUserEnabled(true)
                                .build());
        DeviceDataAdvertisement advertisement =
                new DeviceDataAdvertisement(TEST_DEVICE, currentDeviceId, ads);
        HealthConnectReceiver<Void> receiver = new HealthConnectReceiver<>();
        TestUtils.advertiseDeviceDataSources(Set.of(advertisement), outcomeExecutor(), receiver);

        TestUtils.verifyGetCurrentDeviceDataSourceWithPermission(
                dataSource -> {
                    assertThat(dataSource.getDevice().getModel()).isNotNull();
                    assertThat(dataSource.getDevice().getManufacturer()).isNotNull();
                    assertThat(dataSource.getDevice().getType())
                            .isEqualTo(Device.DEVICE_TYPE_PHONE);
                    assertThat(dataSource.getDevice().getDisplayName()).isNotNull();

                    assertThat(dataSource.getDeviceDataTypeSources()).hasSize(2);

                    Optional<DeviceDataTypeSource> typeSource =
                            dataSource.getDeviceDataTypeSources().stream()
                                    .filter(source -> source.getDataType() == NutritionRecord.class)
                                    .findAny();

                    assertThat(typeSource.isPresent()).isTrue();
                    assertThat(typeSource.get().getDataType()).isEqualTo(NutritionRecord.class);
                    assertThat(typeSource.get().isAvailable()).isTrue();
                    assertThat(typeSource.get().isUserEnabled()).isTrue();
                },
                READ_DISTANCE,
                READ_NUTRITION,
                READ_SYMPTOM_COUGH);
    }

    @Test
    public void fromMultipleDevices_returnsCorrectCurrentDevice() throws InterruptedException {
        String currentDeviceId = TestUtils.getCurrentDeviceId();
        Set<DeviceDataTypeAdvertisement> ads =
                Set.of(
                        new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                                .setAvailable(true)
                                .setUserEnabled(true)
                                .build());
        Device testDevice1 =
                new Device.Builder()
                        .setManufacturer("TestManufacturer1")
                        .setModel("TestModel1")
                        .setType(Device.DEVICE_TYPE_WATCH)
                        .setDisplayName("TestDisplayName1")
                        .build();
        Device testDevice2 =
                new Device.Builder()
                        .setManufacturer("TestManufacturer2")
                        .setModel("TestModel2")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .setDisplayName("TestDisplayName2")
                        .build();
        Device testDevice3 =
                new Device.Builder()
                        .setManufacturer("TestManufacturer3")
                        .setModel("TestModel")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .setDisplayName("TestDisplayName")
                        .build();
        DeviceDataAdvertisement advertisement1 =
                new DeviceDataAdvertisement(testDevice1, "device_id", ads);
        DeviceDataAdvertisement advertisement2 =
                new DeviceDataAdvertisement(testDevice2, currentDeviceId, ads);
        DeviceDataAdvertisement advertisement3 =
                new DeviceDataAdvertisement(testDevice3, "different_device_id", ads);
        HealthConnectReceiver<Void> receiver = new HealthConnectReceiver<>();
        TestUtils.advertiseDeviceDataSources(
                Set.of(advertisement1, advertisement2, advertisement3),
                outcomeExecutor(),
                receiver);

        TestUtils.verifyGetCurrentDeviceDataSourceWithPermission(
                dataSource -> {
                    assertThat(dataSource.getDevice().getManufacturer())
                            .isEqualTo("TestManufacturer2");
                    assertThat(dataSource.getDevice().getModel()).isEqualTo("TestModel2");
                    assertThat(dataSource.getDevice().getDisplayName())
                            .isEqualTo("TestDisplayName2");

                    assertThat(dataSource.getDeviceDataTypeSources()).hasSize(1);
                    DeviceDataTypeSource typeSource =
                            dataSource.getDeviceDataTypeSources().iterator().next();
                    assertThat(typeSource.getDataType()).isEqualTo(StepsRecord.class);
                    assertThat(typeSource.isAvailable()).isTrue();
                    assertThat(typeSource.isUserEnabled()).isTrue();
                },
                READ_STEPS);
    }

    @Test
    public void getCurrentDeviceDataSource_matchesGetDeviceDataSource()
            throws InterruptedException {
        String currentDeviceId = TestUtils.getCurrentDeviceId();
        Set<DeviceDataTypeAdvertisement> ads =
                Set.of(
                        new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                                .setAvailable(true)
                                .setUserEnabled(true)
                                .build());
        DeviceDataAdvertisement advertisement =
                new DeviceDataAdvertisement(TEST_DEVICE, currentDeviceId, ads);
        HealthConnectReceiver<Void> adReceiver = new HealthConnectReceiver<>();
        TestUtils.advertiseDeviceDataSources(Set.of(advertisement), outcomeExecutor(), adReceiver);
        adReceiver.verifyNoExceptionOrThrow();

        DeviceDataSource currentDataSource =
                callAndGetResponseWithShellPermissionIdentity(
                        (executor, currentDeviceReceiver) ->
                                TestUtils.getHealthConnectManager()
                                        .getCurrentDeviceDataSource(
                                                executor, currentDeviceReceiver),
                        HealthPermissions.READ_STEPS);
        GetDeviceDataSourcesResponse dataSources =
                callAndGetResponseWithShellPermissionIdentity(
                        (executor, dataSourcesReceiver) ->
                                TestUtils.getHealthConnectManager()
                                        .getDeviceDataSources(executor, dataSourcesReceiver),
                        HealthPermissions.READ_STEPS);

        assertThat(dataSources.getDeviceDataSources().size()).isEqualTo(1);
        assertThat(dataSources.getDeviceDataSources().get(0)).isEqualTo(currentDataSource);
    }

    @Test
    public void getCurrentDeviceDataSource_usesDisplayName() throws InterruptedException {
        String currentDeviceId = TestUtils.getCurrentDeviceId();
        Device device =
                new Device.Builder()
                        .setManufacturer("TestManufacturer")
                        .setModel("TestModel")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .setDisplayName("OriginalDisplayName")
                        .build();
        Set<DeviceDataTypeAdvertisement> ads =
                Set.of(
                        new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                                .setAvailable(true)
                                .setUserEnabled(true)
                                .build());
        DeviceDataAdvertisement advertisement =
                new DeviceDataAdvertisement(device, currentDeviceId, ads);
        HealthConnectReceiver<Void> adReceiver = new HealthConnectReceiver<>();
        TestUtils.advertiseDeviceDataSources(Set.of(advertisement), outcomeExecutor(), adReceiver);
        adReceiver.verifyNoExceptionOrThrow();

        DeviceDataSource currentDataSource =
                callAndGetResponseWithShellPermissionIdentity(
                        (executor, currentDeviceReceiver) ->
                                TestUtils.getHealthConnectManager()
                                        .getCurrentDeviceDataSource(
                                                executor, currentDeviceReceiver),
                        HealthPermissions.READ_STEPS);

        assertThat(currentDataSource.getDevice().getDisplayName()).isEqualTo("OriginalDisplayName");
    }

    @Test
    public void getCurrentDeviceDataSource_afterDisplayNameUpdate_getsUpdatedDisplayName()
            throws InterruptedException {
        String currentDeviceId = TestUtils.getCurrentDeviceId();
        Device device1 =
                new Device.Builder()
                        .setManufacturer("TestManufacturer")
                        .setModel("TestModel")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .setDisplayName("OriginalDisplayName")
                        .build();
        Set<DeviceDataTypeAdvertisement> ads =
                Set.of(
                        new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                                .setAvailable(true)
                                .setUserEnabled(true)
                                .build());
        DeviceDataAdvertisement advertisement1 =
                new DeviceDataAdvertisement(device1, currentDeviceId, ads);
        HealthConnectReceiver<Void> adReceiver1 = new HealthConnectReceiver<>();
        TestUtils.advertiseDeviceDataSources(
                Set.of(advertisement1), outcomeExecutor(), adReceiver1);
        adReceiver1.verifyNoExceptionOrThrow();

        Device device2 =
                new Device.Builder()
                        .setManufacturer("TestManufacturer")
                        .setModel("TestModel")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .setDisplayName("UpdatedDisplayName")
                        .build();
        DeviceDataAdvertisement advertisement2 =
                new DeviceDataAdvertisement(device2, currentDeviceId, ads);
        HealthConnectReceiver<Void> adReceiver2 = new HealthConnectReceiver<>();
        TestUtils.advertiseDeviceDataSources(
                Set.of(advertisement2), outcomeExecutor(), adReceiver2);
        adReceiver2.verifyNoExceptionOrThrow();

        DeviceDataSource currentDataSource2 =
                callAndGetResponseWithShellPermissionIdentity(
                        (executor, currentDeviceReceiver) ->
                                TestUtils.getHealthConnectManager()
                                        .getCurrentDeviceDataSource(
                                                executor, currentDeviceReceiver),
                        HealthPermissions.READ_STEPS);
        assertThat(currentDataSource2.getDevice().getDisplayName()).isEqualTo("UpdatedDisplayName");
    }

    @Test
    public void getCurrentDeviceDataSource_canUpdateDisplayNameToNull()
            throws InterruptedException {
        String currentDeviceId = TestUtils.getCurrentDeviceId();
        Device device1 =
                new Device.Builder()
                        .setManufacturer("TestManufacturer")
                        .setModel("TestModel")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .setDisplayName("OriginalDisplayName")
                        .build();
        Set<DeviceDataTypeAdvertisement> ads =
                Set.of(
                        new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                                .setAvailable(true)
                                .setUserEnabled(true)
                                .build());
        DeviceDataAdvertisement advertisement1 =
                new DeviceDataAdvertisement(device1, currentDeviceId, ads);
        HealthConnectReceiver<Void> adReceiver1 = new HealthConnectReceiver<>();
        TestUtils.advertiseDeviceDataSources(
                Set.of(advertisement1), outcomeExecutor(), adReceiver1);
        adReceiver1.verifyNoExceptionOrThrow();

        Device device2 =
                new Device.Builder()
                        .setManufacturer("TestManufacturer")
                        .setModel("TestModel")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .setDisplayName(null)
                        .build();
        DeviceDataAdvertisement advertisement2 =
                new DeviceDataAdvertisement(device2, currentDeviceId, ads);
        HealthConnectReceiver<Void> adReceiver2 = new HealthConnectReceiver<>();
        TestUtils.advertiseDeviceDataSources(
                Set.of(advertisement2), outcomeExecutor(), adReceiver2);
        adReceiver2.verifyNoExceptionOrThrow();

        DeviceDataSource currentDataSource2 =
                callAndGetResponseWithShellPermissionIdentity(
                        (executor, currentDeviceReceiver) ->
                                TestUtils.getHealthConnectManager()
                                        .getCurrentDeviceDataSource(
                                                executor, currentDeviceReceiver),
                        HealthPermissions.READ_STEPS);
        assertThat(currentDataSource2.getDevice().getDisplayName()).isEqualTo(null);
    }
}
