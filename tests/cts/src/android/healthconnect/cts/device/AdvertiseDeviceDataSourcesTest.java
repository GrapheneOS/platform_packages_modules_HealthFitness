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
import static android.healthconnect.testing.shared.DataFactory.getHeartRateRecord;

import static com.android.healthfitness.flags.Flags.FLAG_DEVELOPMENT_DATABASE_RW;
import static com.android.healthfitness.flags.Flags.FLAG_DEVICE_DATA_PROVIDERS_API;
import static com.android.healthfitness.flags.Flags.FLAG_DEVICE_DATA_PROVIDERS_DB;
import static com.android.healthfitness.flags.Flags.FLAG_SYMPTOMS;

import static com.google.common.truth.Truth.assertThat;

import android.health.connect.datatypes.Device;
import android.health.connect.datatypes.DistanceRecord;
import android.health.connect.datatypes.HeartRateRecord;
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
import java.util.Set;

@RunWith(AndroidJUnit4.class)
@RequiresFlagsEnabled({
    FLAG_DEVICE_DATA_PROVIDERS_API,
    FLAG_DEVICE_DATA_PROVIDERS_DB,
    FLAG_DEVELOPMENT_DATABASE_RW,
    FLAG_SYMPTOMS
})
// TODO(b/455564575): Update this test when we can read back the advertisement.
public class AdvertiseDeviceDataSourcesTest {

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Rule
    public AssumptionCheckerRule mSupportedHardwareRule =
            new AssumptionCheckerRule(
                    DeviceSupportUtils::isHealthConnectFullySupported,
                    "Tests should run on supported hardware only.");

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
    public void advertiseWithDisplayName() throws InterruptedException {
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
}
