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

import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_HEART_RATE;
import static android.healthconnect.testing.cts.TestOutcomeReceiver.outcomeExecutor;
import static android.healthconnect.testing.cts.TestUtils.advertiseDevice;
import static android.healthconnect.testing.cts.TestUtils.getCurrentDeviceDataSource;
import static android.healthconnect.testing.cts.TestUtils.getDeviceDataSourceInfos;
import static android.healthconnect.testing.shared.DataFactory.getHeartRateRecord;

import static com.android.healthfitness.flags.Flags.FLAG_DEVICE_DATA_PROVIDERS_API;
import static com.android.healthfitness.flags.Flags.FLAG_DEVICE_DATA_PROVIDERS_DB;
import static com.android.healthfitness.flags.Flags.FLAG_SYMPTOMS;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.health.connect.DeviceDataSource;
import android.health.connect.DeviceDataSourceInfo;
import android.health.connect.HealthConnectException;
import android.health.connect.datatypes.Device;
import android.health.connect.datatypes.DistanceRecord;
import android.health.connect.datatypes.HeartRateRecord;
import android.health.connect.datatypes.SleepSessionRecord;
import android.health.connect.datatypes.StepsRecord;
import android.health.connect.datatypes.SymptomRecord;
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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
@RequiresFlagsEnabled({
    FLAG_DEVICE_DATA_PROVIDERS_API,
    FLAG_DEVICE_DATA_PROVIDERS_DB,
    FLAG_SYMPTOMS
})
public class AdvertiseDeviceDataSourcesTest {

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Rule
    public AssumptionCheckerRule mSupportedHardwareRule =
            new AssumptionCheckerRule(
                    DeviceSupportUtils::isHealthConnectFullySupported,
                    "Tests should run on supported hardware only.");

    private static final Device PHONE_DEVICE =
            new Device.Builder()
                    .setManufacturer("TestManufacturer")
                    .setModel("Some Model")
                    .setDisplayName("Some Name")
                    .setType(Device.DEVICE_TYPE_PHONE)
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
    public void advertiseDeviceDataSources() throws InterruptedException {
        Device device =
                new Device.Builder()
                        .setManufacturer("TestManufacturer")
                        .setModel("TestModel")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .setDisplayName("TestDisplayName")
                        .build();
        String deviceId = "TestDeviceId";
        Set<DeviceDataTypeAdvertisement> deviceDataTypeAdvertisements =
                Set.of(
                        new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                                .setAvailable(true)
                                .build());
        DeviceDataAdvertisement advertisement =
                new DeviceDataAdvertisement(device, deviceId, deviceDataTypeAdvertisements);
        HealthConnectReceiver<Void> receiver = new HealthConnectReceiver<>();

        TestUtils.advertiseDeviceDataSources(Set.of(advertisement), outcomeExecutor(), receiver);

        receiver.verifyNoExceptionOrThrow();
    }

    @Test
    public void advertiseMultipleDataTypes() throws InterruptedException {
        Device device =
                new Device.Builder()
                        .setManufacturer("TestManufacturer")
                        .setModel("TestModel")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .setDisplayName("TestDisplayName")
                        .build();
        String deviceId = "TestDeviceId";
        Set<DeviceDataTypeAdvertisement> deviceDataTypeAdvertisements =
                Set.of(
                        new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                                .setAvailable(true)
                                .build(),
                        new DeviceDataTypeAdvertisement.Builder(DistanceRecord.class)
                                .setAvailable(true)
                                .build(),
                        new DeviceDataTypeAdvertisement.Builder(HeartRateRecord.class)
                                .setAvailable(true)
                                .build());
        DeviceDataAdvertisement advertisement =
                new DeviceDataAdvertisement(device, deviceId, deviceDataTypeAdvertisements);
        HealthConnectReceiver<Void> receiver = new HealthConnectReceiver<>();

        TestUtils.advertiseDeviceDataSources(Set.of(advertisement), outcomeExecutor(), receiver);

        receiver.verifyNoExceptionOrThrow();
    }

    @Test
    public void advertiseWithUserEnabled() throws InterruptedException {
        Device device =
                new Device.Builder()
                        .setManufacturer("TestManufacturer")
                        .setModel("TestModel")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .setDisplayName("TestDisplayName")
                        .build();
        String deviceId = "TestDeviceId";
        Set<DeviceDataTypeAdvertisement> deviceDataTypeAdvertisements =
                Set.of(
                        new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                                .setAvailable(true)
                                .setUserEnabled(true)
                                .build());
        DeviceDataAdvertisement advertisement =
                new DeviceDataAdvertisement(device, deviceId, deviceDataTypeAdvertisements);
        HealthConnectReceiver<Void> receiver = new HealthConnectReceiver<>();

        TestUtils.advertiseDeviceDataSources(Set.of(advertisement), outcomeExecutor(), receiver);

        receiver.verifyNoExceptionOrThrow();
    }

    @Test
    public void advertiseWithIsVisibleInMatchmakingEnabled() throws InterruptedException {
        Device device =
                new Device.Builder()
                        .setManufacturer("TestManufacturer")
                        .setModel("TestModel")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .setDisplayName("TestDisplayName")
                        .build();
        String deviceId = "TestDeviceId";
        Set<DeviceDataTypeAdvertisement> deviceDataTypeAdvertisements =
                Set.of(
                        new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                                .setAvailable(true)
                                .setVisibleByDefaultInMatchmaking(true)
                                .build());
        DeviceDataAdvertisement advertisement =
                new DeviceDataAdvertisement(device, deviceId, deviceDataTypeAdvertisements);
        HealthConnectReceiver<Void> receiver = new HealthConnectReceiver<>();

        TestUtils.advertiseDeviceDataSources(Set.of(advertisement), outcomeExecutor(), receiver);

        receiver.verifyNoExceptionOrThrow();
    }

    @Test
    public void advertiseWithDataTypeNotAvailable() throws InterruptedException {
        Device device =
                new Device.Builder()
                        .setManufacturer("TestManufacturer")
                        .setModel("TestModel")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .setDisplayName("TestDisplayName")
                        .build();
        String deviceId = "TestDeviceId";
        Set<DeviceDataTypeAdvertisement> deviceDataTypeAdvertisements =
                Set.of(
                        new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                                .setAvailable(false)
                                .build());
        DeviceDataAdvertisement advertisement =
                new DeviceDataAdvertisement(device, deviceId, deviceDataTypeAdvertisements);
        HealthConnectReceiver<Void> receiver = new HealthConnectReceiver<>();

        TestUtils.advertiseDeviceDataSources(Set.of(advertisement), outcomeExecutor(), receiver);

        receiver.verifyNoExceptionOrThrow();
    }

    @Test
    public void advertiseWithSymptomType() throws InterruptedException {
        Device device =
                new Device.Builder()
                        .setManufacturer("TestManufacturer")
                        .setModel("TestModel")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .setDisplayName("TestDisplayName")
                        .build();
        String deviceId = "TestDeviceId";
        Set<DeviceDataTypeAdvertisement> deviceDataTypeAdvertisements =
                Set.of(
                        new DeviceDataTypeAdvertisement.Builder(SymptomRecord.class)
                                .setAvailable(true)
                                .setSymptomType(SymptomRecord.SYMPTOM_TYPE_COUGH)
                                .build());
        DeviceDataAdvertisement advertisement =
                new DeviceDataAdvertisement(device, deviceId, deviceDataTypeAdvertisements);
        HealthConnectReceiver<Void> receiver = new HealthConnectReceiver<>();

        TestUtils.advertiseDeviceDataSources(Set.of(advertisement), outcomeExecutor(), receiver);

        receiver.verifyNoExceptionOrThrow();
    }

    @Test
    public void testThrowsException_withCanonicalSpn_redacts() throws InterruptedException {
        String deviceId = TestUtils.getCurrentDeviceId();
        TestUtils.advertiseDevice(deviceId, StepsRecord.class);

        List<HeartRateRecord> records = Collections.singletonList(getHeartRateRecord());

        try {
            TestUtils.insertDeviceRecords(deviceId, records);
            Assert.fail();
        } catch (Exception exception) {
            assertThat(exception.getMessage())
                    .isEqualTo(
                            "java.lang.IllegalArgumentException: The device with id"
                                    + " com.android.healthconnect.phone was not advertised for data"
                                    + " type "
                                    + RECORD_TYPE_HEART_RATE);
            assertThat(exception.toString())
                    .isEqualTo(
                            "android.health.connect.HealthConnectException:"
                                    + " java.lang.IllegalArgumentException: The device with id"
                                    + " com.android.healthconnect.phone was not advertised for data"
                                    + " type "
                                    + RECORD_TYPE_HEART_RATE);
        }
    }

    @Test
    public void withSameDeviceId_differentDeviceTypes_sameDataType_advertise_throws()
            throws InterruptedException {
        String deviceId = "TestDeviceId";
        Device device2 =
                new Device.Builder()
                        .setManufacturer("TestManufacturer")
                        .setModel("Some Model")
                        .setDisplayName("Some Name")
                        .setType(Device.DEVICE_TYPE_CHEST_STRAP)
                        .build();

        advertiseDevice(deviceId, PHONE_DEVICE, StepsRecord.class);

        HealthConnectException exception =
                assertThrows(
                        HealthConnectException.class,
                        () -> advertiseDevice(deviceId, device2, StepsRecord.class));

        assertThat(exception.getMessage())
                .contains(
                        "The device with id "
                                + deviceId
                                + " has already been used for a different device type.");
    }

    @Test
    public void withSameDeviceId_differentDeviceTypes_differentDataTypes_advertise_throws()
            throws InterruptedException {
        String deviceId = "TestDeviceId";
        Device device2 =
                new Device.Builder()
                        .setManufacturer("TestManufacturer")
                        .setModel("Some Model")
                        .setDisplayName("Some Name")
                        .setType(Device.DEVICE_TYPE_CHEST_STRAP)
                        .build();

        advertiseDevice(deviceId, PHONE_DEVICE, StepsRecord.class);

        HealthConnectException exception =
                assertThrows(
                        HealthConnectException.class,
                        () -> advertiseDevice(deviceId, device2, DistanceRecord.class));

        assertThat(exception.getMessage())
                .contains(
                        "The device with id "
                                + deviceId
                                + " has already been used for a different device type.");
    }

    @Test
    public void withSameDeviceId_sameDeviceTypes_differentDataTypes_advertise_doesNotThrow()
            throws InterruptedException {
        String deviceId = "TestDeviceId";
        Device device2 =
                new Device.Builder()
                        .setManufacturer("Other TestManufacturer")
                        .setModel("Some Model")
                        .setDisplayName("Some Name")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .build();

        advertiseDevice(deviceId, PHONE_DEVICE, StepsRecord.class);

        Set<DeviceDataTypeAdvertisement> deviceDataTypeAdvertisements =
                Set.of(
                        new DeviceDataTypeAdvertisement.Builder(SymptomRecord.class)
                                .setAvailable(true)
                                .setSymptomType(SymptomRecord.SYMPTOM_TYPE_COUGH)
                                .build());
        DeviceDataAdvertisement advertisement =
                new DeviceDataAdvertisement(device2, deviceId, deviceDataTypeAdvertisements);
        HealthConnectReceiver<Void> receiver = new HealthConnectReceiver<>();

        TestUtils.advertiseDeviceDataSources(Set.of(advertisement), outcomeExecutor(), receiver);

        receiver.verifyNoExceptionOrThrow();
    }

    @Test
    public void withSameDeviceId_sameDeviceTypes_sameDataTypes_advertise_doesNotThrow()
            throws InterruptedException {
        String deviceId = "TestDeviceId";
        Device device2 =
                new Device.Builder()
                        .setManufacturer("Other TestManufacturer")
                        .setModel("Some Model")
                        .setDisplayName("Some Name")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .build();

        advertiseDevice(deviceId, PHONE_DEVICE, StepsRecord.class);

        Set<DeviceDataTypeAdvertisement> deviceDataTypeAdvertisements =
                Set.of(
                        new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                                .setAvailable(true)
                                .build());
        DeviceDataAdvertisement advertisement =
                new DeviceDataAdvertisement(device2, deviceId, deviceDataTypeAdvertisements);
        HealthConnectReceiver<Void> receiver = new HealthConnectReceiver<>();

        TestUtils.advertiseDeviceDataSources(Set.of(advertisement), outcomeExecutor(), receiver);

        receiver.verifyNoExceptionOrThrow();
    }

    @Test
    public void
            withSameDeviceId_sameDeviceTypes_differentDataTypes_advertise_overwritesPreviousDevice()
                    throws InterruptedException {
        List<DeviceDataSourceInfo> startSources = getDeviceDataSourceInfos();
        assertThat(startSources.stream().map(DeviceDataSourceInfo::getDevice))
                .doesNotContain(PHONE_DEVICE);

        String deviceId = "TestDeviceId";
        Set<DeviceDataTypeAdvertisement> stepAd =
                Set.of(
                        new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                                .setAvailable(true)
                                .build());
        DeviceDataAdvertisement advertisementOne =
                new DeviceDataAdvertisement(PHONE_DEVICE, deviceId, stepAd);

        Device device2 =
                new Device.Builder()
                        .setManufacturer("Other TestManufacturer")
                        .setModel("Some Model")
                        .setDisplayName("Some Name")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .build();
        Set<DeviceDataTypeAdvertisement> sleepAd =
                Set.of(
                        new DeviceDataTypeAdvertisement.Builder(SleepSessionRecord.class)
                                .setAvailable(true)
                                .build());
        DeviceDataAdvertisement advertisementTwo =
                new DeviceDataAdvertisement(device2, deviceId, sleepAd);

        // Make first advertisement.
        HealthConnectReceiver<Void> receiver = new HealthConnectReceiver<>();
        TestUtils.advertiseDeviceDataSources(Set.of(advertisementOne), outcomeExecutor(), receiver);
        List<DeviceDataSourceInfo> newSources = getDeviceDataSourceInfos();
        assertThat(newSources.stream().map(DeviceDataSourceInfo::getDevice)).contains(PHONE_DEVICE);

        // Make second advertisement with same deviceId but modified device metadata.
        receiver = new HealthConnectReceiver<>();
        TestUtils.advertiseDeviceDataSources(Set.of(advertisementTwo), outcomeExecutor(), receiver);
        newSources = getDeviceDataSourceInfos();

        // Contains only the second device
        assertThat(newSources.stream().map(DeviceDataSourceInfo::getDevice))
                .doesNotContain(PHONE_DEVICE);
        assertThat(newSources.stream().map(DeviceDataSourceInfo::getDevice)).contains(device2);

        DeviceDataSourceInfo newSource =
                newSources.stream()
                        .filter(
                                source ->
                                        Objects.equals(
                                                "Other TestManufacturer",
                                                source.getDevice().getManufacturer()))
                        .findAny()
                        .get();

        // The new source has only one provider
        assertThat(newSource.getDeviceDataProviderInfos()).hasSize(1);
        assertThat(newSource.getDeviceDataProviderInfos().get(0).getDeviceDataTypeAdvertisements())
                .hasSize(1);
        DeviceDataTypeAdvertisement newAd =
                newSource
                        .getDeviceDataProviderInfos()
                        .get(0)
                        .getDeviceDataTypeAdvertisements()
                        .iterator()
                        .next();
        assertThat(newAd.getDataType()).isEqualTo(SleepSessionRecord.class);
    }

    @Test
    public void advertiseDevice_hasDisplayName() throws InterruptedException {
        String deviceId = "TestDeviceId";
        Device device =
                new Device.Builder()
                        .setManufacturer("TestManufacturer")
                        .setModel("TestModel")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .setDisplayName("OriginalDisplayName")
                        .build();

        advertiseDevice(deviceId, device, StepsRecord.class);
        List<DeviceDataSourceInfo> sources = getDeviceDataSourceInfos();

        assertThat(
                        sources.stream()
                                .filter(s -> s.getDevice().equals(device))
                                .findFirst()
                                .get()
                                .getDevice()
                                .getDisplayName())
                .isEqualTo("OriginalDisplayName");
    }

    @Test
    public void advertiseDevice_updateDisplayName_updatesSuccessfully()
            throws InterruptedException {
        String deviceId = "TestDeviceId";
        Device device1 =
                new Device.Builder()
                        .setManufacturer("TestManufacturer")
                        .setModel("TestModel")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .setDisplayName("OriginalDisplayName")
                        .build();
        advertiseDevice(deviceId, device1, StepsRecord.class);
        List<DeviceDataSourceInfo> sources1 = getDeviceDataSourceInfos();
        assertThat(
                        sources1.stream()
                                .filter(s -> s.getDevice().equals(device1))
                                .findFirst()
                                .get()
                                .getDevice()
                                .getDisplayName())
                .isEqualTo("OriginalDisplayName");
        Device device2 =
                new Device.Builder()
                        .setManufacturer("TestManufacturer")
                        .setModel("TestModel")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .setDisplayName("UpdatedDisplayName")
                        .build();

        advertiseDevice(deviceId, device2, StepsRecord.class);
        List<DeviceDataSourceInfo> sources2 = getDeviceDataSourceInfos();

        assertThat(
                        sources2.stream()
                                .filter(s -> s.getDevice().equals(device2))
                                .findFirst()
                                .get()
                                .getDevice()
                                .getDisplayName())
                .isEqualTo("UpdatedDisplayName");
    }

    @Test
    public void advertiseDevice_updateDisplayName_oldDeviceEntyRemoved()
            throws InterruptedException {
        String deviceId = "TestDeviceId";
        Device device1 =
                new Device.Builder()
                        .setManufacturer("TestManufacturer")
                        .setModel("TestModel")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .setDisplayName("OriginalDisplayName")
                        .build();
        advertiseDevice(deviceId, device1, StepsRecord.class);
        Device device2 =
                new Device.Builder()
                        .setManufacturer("TestManufacturer")
                        .setModel("TestModel")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .setDisplayName("UpdatedDisplayName")
                        .build();

        advertiseDevice(deviceId, device2, StepsRecord.class);
        List<DeviceDataSourceInfo> sources = getDeviceDataSourceInfos();

        assertThat(sources.stream().filter(s -> s.getDevice().equals(device1)).findAny().isEmpty())
                .isTrue();
    }

    @Test
    public void advertiseDevice_updateDisplayNameToNull_updatesSuccessfully()
            throws InterruptedException {
        String deviceId = "TestDeviceId";
        Device device1 =
                new Device.Builder()
                        .setManufacturer("TestManufacturer")
                        .setModel("TestModel")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .setDisplayName("OriginalDisplayName")
                        .build();
        advertiseDevice(deviceId, device1, StepsRecord.class);

        Device device2 =
                new Device.Builder()
                        .setManufacturer("TestManufacturer")
                        .setModel("TestModel")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .setDisplayName(null)
                        .build();
        advertiseDevice(deviceId, device2, StepsRecord.class);
        List<DeviceDataSourceInfo> sources = getDeviceDataSourceInfos();

        assertThat(
                        sources.stream()
                                .filter(s -> s.getDevice().equals(device2))
                                .findFirst()
                                .get()
                                .getDevice()
                                .getDisplayName())
                .isNull();
    }

    @Test
    public void advertiseDevice_updateDisplayNameToEmpty_updatesSuccessfully()
            throws InterruptedException {
        String deviceId = "TestDeviceId";
        Device device1 =
                new Device.Builder()
                        .setManufacturer("TestManufacturer")
                        .setModel("TestModel")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .setDisplayName("OriginalDisplayName")
                        .build();
        advertiseDevice(deviceId, device1, StepsRecord.class);

        Device device2 =
                new Device.Builder()
                        .setManufacturer("TestManufacturer")
                        .setModel("TestModel")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .setDisplayName("")
                        .build();
        advertiseDevice(deviceId, device2, StepsRecord.class);

        List<DeviceDataSourceInfo> sources2 = getDeviceDataSourceInfos();
        assertThat(
                        sources2.stream()
                                .filter(s -> s.getDevice().equals(device2))
                                .findFirst()
                                .get()
                                .getDevice()
                                .getDisplayName())
                .isEqualTo("");
    }

    @Test
    public void withCurrentDeviceId_differentDeviceType_advertise_throws()
            throws InterruptedException {
        HealthConnectReceiver<DeviceDataSource> dataSourceReceiver = new HealthConnectReceiver<>();
        getCurrentDeviceDataSource(outcomeExecutor(), dataSourceReceiver);
        dataSourceReceiver.verifyNoExceptionOrThrow();

        DeviceDataSource currentDeviceDataSource = dataSourceReceiver.getResponse();
        Device currentDeviceWithDifferentType =
                new Device.Builder()
                        .setManufacturer(currentDeviceDataSource.getDevice().getManufacturer())
                        .setModel(currentDeviceDataSource.getDevice().getModel())
                        .setType(Device.DEVICE_TYPE_CHEST_STRAP)
                        .setDisplayName(currentDeviceDataSource.getDevice().getDisplayName())
                        .build();
        String currentDeviceId = TestUtils.getCurrentDeviceId();

        HealthConnectException exception =
                assertThrows(
                        HealthConnectException.class,
                        () ->
                                advertiseDevice(
                                        currentDeviceId,
                                        currentDeviceWithDifferentType,
                                        SleepSessionRecord.class));

        assertThat(exception.getMessage())
                .contains(
                        "The device with id com.android.healthconnect.phone"
                                + " has already been used for a different device type.");
    }
}
