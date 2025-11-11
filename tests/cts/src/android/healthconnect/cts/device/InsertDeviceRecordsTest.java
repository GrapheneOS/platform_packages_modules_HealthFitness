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

import static android.health.connect.datatypes.Device.DEVICE_TYPE_UNKNOWN;
import static android.healthconnect.testing.cts.TestOutcomeReceiver.outcomeExecutor;
import static android.healthconnect.testing.cts.TestUtils.advertiseDevice;
import static android.healthconnect.testing.cts.TestUtils.deleteAllDataFromHealthConnect;
import static android.healthconnect.testing.shared.DataFactory.buildDevice;
import static android.healthconnect.testing.shared.DataFactory.getDistanceRecord;
import static android.healthconnect.testing.shared.DataFactory.getHeartRateRecord;
import static android.healthconnect.testing.shared.DataFactory.getStepsRecord;
import static android.healthconnect.testing.shared.DataFactory.getStepsRecordWithEmptyMetaData;

import static com.android.healthfitness.flags.Flags.FLAG_DEVELOPMENT_DATABASE;
import static com.android.healthfitness.flags.Flags.FLAG_DEVICE_DATA_PROVIDERS_API;
import static com.android.healthfitness.flags.Flags.FLAG_DEVICE_DATA_PROVIDERS_DB;

import static com.google.common.truth.Truth.assertThat;

import android.health.connect.HealthConnectException;
import android.health.connect.InsertRecordsResponse;
import android.health.connect.ReadRecordsRequestUsingIds;
import android.health.connect.changelog.ChangeLogTokenRequest;
import android.health.connect.changelog.ChangeLogsRequest;
import android.health.connect.changelog.ChangeLogsResponse;
import android.health.connect.datatypes.Device;
import android.health.connect.datatypes.DistanceRecord;
import android.health.connect.datatypes.HeartRateRecord;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.StepsRecord;
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
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
@RequiresFlagsEnabled({
    FLAG_DEVICE_DATA_PROVIDERS_API,
    FLAG_DEVICE_DATA_PROVIDERS_DB,
    FLAG_DEVELOPMENT_DATABASE
})
// TODO(b/440343237): Add test to verify package name is not equal to writing app once we have
// getDeviceDataSources
public class InsertDeviceRecordsTest {

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Rule
    public AssumptionCheckerRule mSupportedHardwareRule =
            new AssumptionCheckerRule(
                    DeviceSupportUtils::isHealthConnectFullySupported,
                    "Tests should run on supported hardware only.");

    @Before
    public void before() throws Exception {
        deleteAllDataFromHealthConnect();
    }

    @After
    public void after() throws InterruptedException {
        deleteAllDataFromHealthConnect();
    }

    @Test
    public void withoutAdvertisement_insertDeviceRecords_throws() throws InterruptedException {
        String deviceId = "TestDeviceId";
        HealthConnectReceiver<InsertRecordsResponse> receiver = new HealthConnectReceiver<>();

        TestUtils.insertDeviceRecords(
                deviceId, List.of(getStepsRecord()), outcomeExecutor(), receiver);

        assertThat(receiver.assertAndGetException().getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_INVALID_ARGUMENT);
        assertThat(receiver.assertAndGetException().getMessage())
                .isEqualTo(
                        "java.lang.IllegalArgumentException: The device with id "
                                + "TestDeviceId was not found, ensure the device data "
                                + "source has been advertised");
    }

    @Test
    public void afterAdvertisement_insertsDeviceRecords_doesNotThrow() throws InterruptedException {
        String deviceId = "TestDeviceId";
        HealthConnectReceiver<InsertRecordsResponse> receiver = new HealthConnectReceiver<>();
        advertiseDevice(deviceId, StepsRecord.class);

        TestUtils.insertDeviceRecords(
                deviceId, List.of(getStepsRecord()), outcomeExecutor(), receiver);

        receiver.verifyNoExceptionOrThrow();
    }

    @Test
    public void insertDeviceRecord_incorrectDeviceIdInserted_throws() throws InterruptedException {
        String deviceId = "TestDeviceId";
        String incorrectDeviceId = "IncorrectTestDeviceId";
        HealthConnectReceiver<InsertRecordsResponse> receiver = new HealthConnectReceiver<>();
        advertiseDevice(deviceId, StepsRecord.class);

        TestUtils.insertDeviceRecords(
                incorrectDeviceId, List.of(getStepsRecord()), outcomeExecutor(), receiver);

        assertThat(receiver.assertAndGetException().getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_INVALID_ARGUMENT);
        assertThat(receiver.assertAndGetException().getMessage())
                .isEqualTo(
                        "java.lang.IllegalArgumentException: The device with id "
                                + "IncorrectTestDeviceId was not found, "
                                + "ensure the device data source has been advertised");
    }

    @Test
    public void insertsDeviceRecords_recordIsCorrect() throws InterruptedException {
        String deviceId = "TestDeviceId";
        HealthConnectReceiver<InsertRecordsResponse> receiver = new HealthConnectReceiver<>();
        advertiseDevice(deviceId, StepsRecord.class);
        StepsRecord stepsRecord = getStepsRecord();
        TestUtils.insertDeviceRecords(deviceId, List.of(stepsRecord), outcomeExecutor(), receiver);
        receiver.verifyNoExceptionOrThrow();

        String uuid = receiver.getResponse().getRecords().get(0).getMetadata().getId();
        ReadRecordsRequestUsingIds<StepsRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(StepsRecord.class).addId(uuid).build();
        List<StepsRecord> insertedRecords = TestUtils.readRecords(request);

        assertThat(insertedRecords).hasSize(1);
        StepsRecord insertedRecord = insertedRecords.get(0);
        assertThat(insertedRecord.getCount()).isEqualTo(stepsRecord.getCount());
        assertThat(insertedRecord.getStartTime()).isEqualTo(stepsRecord.getStartTime());
        assertThat(insertedRecord.getEndTime()).isEqualTo(stepsRecord.getEndTime());
        assertThat(insertedRecord.getStartZoneOffset()).isEqualTo(stepsRecord.getStartZoneOffset());
        assertThat(insertedRecord.getEndZoneOffset()).isEqualTo(stepsRecord.getEndZoneOffset());
        assertThat(insertedRecord.getMetadata().getDevice())
                .isEqualTo(stepsRecord.getMetadata().getDevice());
        assertThat(insertedRecord.getMetadata().getDevice()).isEqualTo(buildDevice());
        assertThat(stepsRecord.getMetadata().getDataOrigin().getPackageName())
                .isEqualTo("android.healthconnect.cts");
        assertThat(
                        TestUtils.isMaskedSyntheticPackageName(
                                insertedRecord.getMetadata().getDataOrigin().getPackageName()))
                .isTrue();
    }

    @Test
    public void insertsDeviceRecords_noDeviceInRecordMetadata_devicePopulatedFromAdvertisement()
            throws InterruptedException {
        String deviceId = "TestDeviceId";
        HealthConnectReceiver<InsertRecordsResponse> receiver = new HealthConnectReceiver<>();
        advertiseDevice(deviceId, StepsRecord.class);
        StepsRecord stepsRecord = getStepsRecordWithEmptyMetaData();
        TestUtils.insertDeviceRecords(deviceId, List.of(stepsRecord), outcomeExecutor(), receiver);
        receiver.verifyNoExceptionOrThrow();

        String uuid = receiver.getResponse().getRecords().get(0).getMetadata().getId();
        ReadRecordsRequestUsingIds<StepsRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(StepsRecord.class).addId(uuid).build();
        List<StepsRecord> insertedRecords = TestUtils.readRecords(request);

        assertThat(insertedRecords).hasSize(1);
        StepsRecord insertedRecord = insertedRecords.get(0);
        assertThat(insertedRecord.getCount()).isEqualTo(stepsRecord.getCount());
        assertThat(insertedRecord.getStartTime()).isEqualTo(stepsRecord.getStartTime());
        assertThat(insertedRecord.getEndTime()).isEqualTo(stepsRecord.getEndTime());
        assertThat(insertedRecord.getStartZoneOffset()).isEqualTo(stepsRecord.getStartZoneOffset());
        assertThat(insertedRecord.getEndZoneOffset()).isEqualTo(stepsRecord.getEndZoneOffset());
        assertThat(stepsRecord.getMetadata().getDevice().getType()).isEqualTo(DEVICE_TYPE_UNKNOWN);
        assertThat(insertedRecord.getMetadata().getDevice()).isEqualTo(buildDevice());
        assertThat(
                        TestUtils.isMaskedSyntheticPackageName(
                                insertedRecord.getMetadata().getDataOrigin().getPackageName()))
                .isTrue();
    }

    @Test
    public void insertsDeviceRecords_differentDeviceInRecordMetadata_overriddenByAdvertisedDevice()
            throws InterruptedException {
        String deviceId = "TestDeviceId";
        Device advertisedDevice =
                new Device.Builder()
                        .setManufacturer("google")
                        .setModel("Pixel4a")
                        .setDisplayName("My phone")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .build();
        Device differentDevice =
                new Device.Builder()
                        .setManufacturer("google")
                        .setModel("Pixel5")
                        .setDisplayName("My phone")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .build();
        HealthConnectReceiver<InsertRecordsResponse> receiver = new HealthConnectReceiver<>();
        advertiseDevice(deviceId, advertisedDevice, StepsRecord.class);
        StepsRecord stepsRecord =
                getStepsRecord(10, new Metadata.Builder().setDevice(differentDevice).build());
        TestUtils.insertDeviceRecords(deviceId, List.of(stepsRecord), outcomeExecutor(), receiver);
        receiver.verifyNoExceptionOrThrow();

        String uuid = receiver.getResponse().getRecords().get(0).getMetadata().getId();
        ReadRecordsRequestUsingIds<StepsRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(StepsRecord.class).addId(uuid).build();
        List<StepsRecord> insertedRecords = TestUtils.readRecords(request);

        assertThat(insertedRecords).hasSize(1);
        StepsRecord insertedRecord = insertedRecords.get(0);
        assertThat(insertedRecord.getCount()).isEqualTo(stepsRecord.getCount());
        assertThat(insertedRecord.getStartTime()).isEqualTo(stepsRecord.getStartTime());
        assertThat(insertedRecord.getEndTime()).isEqualTo(stepsRecord.getEndTime());
        assertThat(insertedRecord.getStartZoneOffset()).isEqualTo(stepsRecord.getStartZoneOffset());
        assertThat(insertedRecord.getEndZoneOffset()).isEqualTo(stepsRecord.getEndZoneOffset());
        assertThat(stepsRecord.getMetadata().getDevice().getModel()).isEqualTo("Pixel5");
        assertThat(insertedRecord.getMetadata().getDevice().getModel()).isEqualTo("Pixel4a");
        assertThat(insertedRecord.getMetadata().getDevice()).isEqualTo(advertisedDevice);
        assertThat(
                        TestUtils.isMaskedSyntheticPackageName(
                                insertedRecord.getMetadata().getDataOrigin().getPackageName()))
                .isTrue();
    }

    @Test
    public void
            insertsDeviceRecords_differentDeviceTypeInRecordMetadata_overriddenByAdvertisedDevice()
                    throws InterruptedException {
        String deviceId = "TestDeviceId";
        Device advertisedDevice =
                new Device.Builder()
                        .setManufacturer("google")
                        .setModel("Pixel")
                        .setDisplayName("My device")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .build();
        Device differentDeviceType =
                new Device.Builder()
                        .setManufacturer("google")
                        .setModel("Pixel")
                        .setDisplayName("My device")
                        .setType(Device.DEVICE_TYPE_WATCH)
                        .build();
        HealthConnectReceiver<InsertRecordsResponse> receiver = new HealthConnectReceiver<>();
        advertiseDevice(deviceId, advertisedDevice, StepsRecord.class);
        StepsRecord stepsRecord =
                getStepsRecord(10, new Metadata.Builder().setDevice(differentDeviceType).build());
        TestUtils.insertDeviceRecords(deviceId, List.of(stepsRecord), outcomeExecutor(), receiver);
        receiver.verifyNoExceptionOrThrow();

        String uuid = receiver.getResponse().getRecords().get(0).getMetadata().getId();
        ReadRecordsRequestUsingIds<StepsRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(StepsRecord.class).addId(uuid).build();
        List<StepsRecord> insertedRecords = TestUtils.readRecords(request);

        assertThat(insertedRecords).hasSize(1);
        StepsRecord insertedRecord = insertedRecords.get(0);
        assertThat(insertedRecord.getCount()).isEqualTo(stepsRecord.getCount());
        assertThat(insertedRecord.getStartTime()).isEqualTo(stepsRecord.getStartTime());
        assertThat(insertedRecord.getEndTime()).isEqualTo(stepsRecord.getEndTime());
        assertThat(insertedRecord.getStartZoneOffset()).isEqualTo(stepsRecord.getStartZoneOffset());
        assertThat(insertedRecord.getEndZoneOffset()).isEqualTo(stepsRecord.getEndZoneOffset());
        assertThat(stepsRecord.getMetadata().getDevice().getType())
                .isEqualTo(Device.DEVICE_TYPE_WATCH);
        assertThat(insertedRecord.getMetadata().getDevice().getType())
                .isEqualTo(Device.DEVICE_TYPE_PHONE);
        assertThat(insertedRecord.getMetadata().getDevice()).isEqualTo(advertisedDevice);
        assertThat(
                        TestUtils.isMaskedSyntheticPackageName(
                                insertedRecord.getMetadata().getDataOrigin().getPackageName()))
                .isTrue();
    }

    @Test
    public void insertsDeviceRecords_differentDataTypeAdvertised_throws()
            throws InterruptedException {
        String deviceId = "TestDeviceId";
        HealthConnectReceiver<InsertRecordsResponse> receiver = new HealthConnectReceiver<>();

        advertiseDevice(deviceId, DistanceRecord.class);
        StepsRecord stepsRecord = getStepsRecord();
        TestUtils.insertDeviceRecords(deviceId, List.of(stepsRecord), outcomeExecutor(), receiver);

        assertThat(receiver.assertAndGetException().getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_INVALID_ARGUMENT);
        assertThat(receiver.assertAndGetException().getMessage())
                .isEqualTo(
                        "java.lang.IllegalArgumentException: The device with id "
                                + "TestDeviceId was not advertised for data type 1");
    }

    @Test
    public void insertsDeviceRecords_extraDataTypeNotAdvertised_throws()
            throws InterruptedException {
        String deviceId = "TestDeviceId";
        HealthConnectReceiver<InsertRecordsResponse> receiver = new HealthConnectReceiver<>();

        advertiseDevice(deviceId, StepsRecord.class);
        StepsRecord stepsRecord = getStepsRecord();
        DistanceRecord distanceRecord = getDistanceRecord();
        TestUtils.insertDeviceRecords(
                deviceId, List.of(stepsRecord, distanceRecord), outcomeExecutor(), receiver);

        assertThat(receiver.assertAndGetException().getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_INVALID_ARGUMENT);
        assertThat(receiver.assertAndGetException().getMessage())
                .isEqualTo(
                        "java.lang.IllegalArgumentException: The device with id "
                                + "TestDeviceId was not advertised for data type 7");
    }

    @Test
    public void insertsDeviceRecords_changelogsCreated() throws InterruptedException {
        String deviceId = "TestDeviceId";
        HealthConnectReceiver<InsertRecordsResponse> receiver = new HealthConnectReceiver<>();
        String token =
                TestUtils.getChangeLogToken(
                                new ChangeLogTokenRequest.Builder()
                                        .addRecordType(StepsRecord.class)
                                        .build())
                        .getToken();

        advertiseDevice(deviceId, StepsRecord.class);
        TestUtils.insertDeviceRecords(
                deviceId, List.of(getStepsRecord()), outcomeExecutor(), receiver);
        receiver.verifyNoExceptionOrThrow();
        ChangeLogsResponse response =
                TestUtils.getChangeLogs(new ChangeLogsRequest.Builder(token).build());

        assertThat(response.getUpsertedRecords()).hasSize(1);
        assertThat(response.getDeletedLogs()).isEmpty();
        Record changedRecord = response.getUpsertedRecords().get(0);
        assertThat(changedRecord).isInstanceOf(StepsRecord.class);
        assertThat(
                        TestUtils.isMaskedSyntheticPackageName(
                                changedRecord.getMetadata().getDataOrigin().getPackageName()))
                .isTrue();
    }

    @Test
    public void twoInsertions_sameDevice_recordsHaveSamePackageAndMetadata()
            throws InterruptedException {
        String deviceId = "TestDeviceId";
        HealthConnectReceiver<InsertRecordsResponse> receiver = new HealthConnectReceiver<>();
        HealthConnectReceiver<InsertRecordsResponse> receiver2 = new HealthConnectReceiver<>();

        advertiseDevice(deviceId, StepsRecord.class);
        TestUtils.insertDeviceRecords(
                deviceId, List.of(getStepsRecord()), outcomeExecutor(), receiver);
        receiver.verifyNoExceptionOrThrow();
        TestUtils.insertDeviceRecords(
                deviceId, List.of(getStepsRecord()), outcomeExecutor(), receiver2);
        receiver2.verifyNoExceptionOrThrow();
        String uuid1 = receiver.getResponse().getRecords().get(0).getMetadata().getId();
        String uuid2 = receiver2.getResponse().getRecords().get(0).getMetadata().getId();
        ReadRecordsRequestUsingIds<StepsRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(StepsRecord.class)
                        .addId(uuid1)
                        .addId(uuid2)
                        .build();
        List<StepsRecord> insertedRecords = TestUtils.readRecords(request);

        assertThat(insertedRecords).hasSize(2);
        StepsRecord record1 = insertedRecords.get(0);
        StepsRecord record2 = insertedRecords.get(1);
        assertThat(record1.getMetadata().getDataOrigin().getPackageName())
                .isEqualTo(record2.getMetadata().getDataOrigin().getPackageName());
        assertThat(record1.getMetadata().getDevice()).isEqualTo(record2.getMetadata().getDevice());
    }

    @Test
    public void twoInsertions_differentDeviceIds_recordsHaveDifferentPackageAndMetadata()
            throws InterruptedException {
        String deviceId1 = "TestDeviceId1";
        String deviceId2 = "TestDeviceId2";
        HealthConnectReceiver<InsertRecordsResponse> receiver = new HealthConnectReceiver<>();
        HealthConnectReceiver<InsertRecordsResponse> receiver2 = new HealthConnectReceiver<>();

        advertiseDevice(deviceId1, StepsRecord.class);
        advertiseDevice(deviceId2, StepsRecord.class);
        TestUtils.insertDeviceRecords(
                deviceId1, List.of(getStepsRecord()), outcomeExecutor(), receiver);
        receiver.verifyNoExceptionOrThrow();
        TestUtils.insertDeviceRecords(
                deviceId2, List.of(getStepsRecord()), outcomeExecutor(), receiver2);
        receiver2.verifyNoExceptionOrThrow();
        String uuid1 = receiver.getResponse().getRecords().get(0).getMetadata().getId();
        String uuid2 = receiver2.getResponse().getRecords().get(0).getMetadata().getId();
        ReadRecordsRequestUsingIds<StepsRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(StepsRecord.class)
                        .addId(uuid1)
                        .addId(uuid2)
                        .build();
        List<StepsRecord> insertedRecords = TestUtils.readRecords(request);

        assertThat(insertedRecords).hasSize(2);
        StepsRecord record1 = insertedRecords.get(0);
        StepsRecord record2 = insertedRecords.get(1);
        assertThat(record1.getMetadata().getDataOrigin().getPackageName())
                .isNotEqualTo(record2.getMetadata().getDataOrigin().getPackageName());
        assertThat(record1.getMetadata().getDevice()).isEqualTo(record2.getMetadata().getDevice());
    }

    @Test
    @Ignore("TODO(b/458001956): Update this test once the latest advertisement is being used.")
    // TODO(b/458001956): Update this test once the latest advertisement is being used.
    public void updateDeviceInAdvertisement_twoInsertions_recordsHaveDifferentPackageAndMetadata()
            throws InterruptedException {
        String deviceId = "TestDeviceId";
        Device device1 =
                new Device.Builder()
                        .setManufacturer("google")
                        .setModel("Pixel4a")
                        .setDisplayName("My phone")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .build();
        Device device2 =
                new Device.Builder()
                        .setManufacturer("google")
                        .setModel("Pixel5")
                        .setDisplayName("My phone")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .build();
        HealthConnectReceiver<InsertRecordsResponse> receiver = new HealthConnectReceiver<>();
        HealthConnectReceiver<InsertRecordsResponse> receiver2 = new HealthConnectReceiver<>();

        advertiseDevice(deviceId, device1, StepsRecord.class);
        advertiseDevice(deviceId, device2, StepsRecord.class);
        TestUtils.insertDeviceRecords(
                deviceId, List.of(getStepsRecord()), outcomeExecutor(), receiver);
        receiver.verifyNoExceptionOrThrow();
        TestUtils.insertDeviceRecords(
                deviceId, List.of(getStepsRecord()), outcomeExecutor(), receiver2);
        receiver2.verifyNoExceptionOrThrow();
        String uuid1 = receiver.getResponse().getRecords().get(0).getMetadata().getId();
        String uuid2 = receiver2.getResponse().getRecords().get(0).getMetadata().getId();
        ReadRecordsRequestUsingIds<StepsRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(StepsRecord.class)
                        .addId(uuid1)
                        .addId(uuid2)
                        .build();
        List<StepsRecord> insertedRecords = TestUtils.readRecords(request);

        assertThat(insertedRecords).hasSize(2);
        StepsRecord record1 = insertedRecords.get(0);
        StepsRecord record2 = insertedRecords.get(1);
        assertThat(record1.getMetadata().getDataOrigin().getPackageName())
                .isNotEqualTo(record2.getMetadata().getDataOrigin().getPackageName());
        assertThat(record1.getMetadata().getDevice())
                .isNotEqualTo(record2.getMetadata().getDevice());
        assertThat(record1.getMetadata().getDevice()).isEqualTo(device1);
        assertThat(record2.getMetadata().getDevice()).isEqualTo(device2);
    }

    @Test
    public void afterAdvertisement_insertDeviceRecords_withCurrentDeviceId_insertsRecord()
            throws InterruptedException {
        HealthConnectReceiver<InsertRecordsResponse> receiver = new HealthConnectReceiver<>();
        advertiseDevice(TestUtils.getCurrentDeviceId(), StepsRecord.class);
        StepsRecord stepsRecord = getStepsRecord();

        TestUtils.insertDeviceRecords(
                TestUtils.getCurrentDeviceId(), List.of(stepsRecord), outcomeExecutor(), receiver);

        receiver.verifyNoExceptionOrThrow();
        String uuid = receiver.getResponse().getRecords().get(0).getMetadata().getId();
        ReadRecordsRequestUsingIds<StepsRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(StepsRecord.class).addId(uuid).build();
        List<StepsRecord> insertedRecords = TestUtils.readRecords(request);

        assertThat(insertedRecords).hasSize(1);
        StepsRecord insertedRecord = insertedRecords.get(0);
        assertThat(insertedRecord.getCount()).isEqualTo(stepsRecord.getCount());
        assertThat(insertedRecord.getStartTime()).isEqualTo(stepsRecord.getStartTime());
        assertThat(insertedRecord.getEndTime()).isEqualTo(stepsRecord.getEndTime());
        assertThat(insertedRecord.getMetadata().getDataOrigin().getPackageName())
                .isNotEqualTo(stepsRecord.getMetadata().getDataOrigin().getPackageName());
        assertThat(stepsRecord.getMetadata().getDataOrigin().getPackageName())
                .isEqualTo("android.healthconnect.cts");
        assertThat(
                        TestUtils.isMaskedSyntheticPackageName(
                                insertedRecord.getMetadata().getDataOrigin().getPackageName()))
                .isTrue();
        assertThat(insertedRecord.getMetadata().getDevice())
                .isEqualTo(stepsRecord.getMetadata().getDevice());
    }

    @Test
    public void withoutAdvertisement_insertDeviceRecords_withCurrentDeviceId_throws()
            throws InterruptedException {
        HealthConnectReceiver<InsertRecordsResponse> receiver = new HealthConnectReceiver<>();
        StepsRecord stepsRecord = getStepsRecord();

        TestUtils.insertDeviceRecords(
                TestUtils.getCurrentDeviceId(), List.of(stepsRecord), outcomeExecutor(), receiver);

        assertThat(receiver.assertAndGetException().getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_INVALID_ARGUMENT);
        assertThat(receiver.assertAndGetException().getMessage())
                .startsWith("java.lang.IllegalArgumentException: The current device was");
        assertThat(receiver.assertAndGetException().getMessage())
                .endsWith(" not found, ensure the device data source has been advertised");
    }

    @Test
    public void insertTwoDataTypes_recordsAreCorrect() throws InterruptedException {
        String deviceId = "TestDeviceId";
        HealthConnectReceiver<InsertRecordsResponse> receiver = new HealthConnectReceiver<>();
        Set<DeviceDataTypeAdvertisement> deviceDataTypeAdvertisements =
                Set.of(
                        new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                                .setAvailable(true)
                                .build(),
                        new DeviceDataTypeAdvertisement.Builder(HeartRateRecord.class)
                                .setAvailable(true)
                                .build());
        advertiseDevice(deviceId, buildDevice(), deviceDataTypeAdvertisements);
        StepsRecord stepsRecord = getStepsRecord();
        HeartRateRecord heartRateRecord = getHeartRateRecord();
        TestUtils.insertDeviceRecords(
                deviceId, List.of(stepsRecord, heartRateRecord), outcomeExecutor(), receiver);
        receiver.verifyNoExceptionOrThrow();

        String uuid1 = receiver.getResponse().getRecords().get(0).getMetadata().getId();
        String uuid2 = receiver.getResponse().getRecords().get(1).getMetadata().getId();
        ReadRecordsRequestUsingIds<StepsRecord> stepsRequest =
                new ReadRecordsRequestUsingIds.Builder<>(StepsRecord.class).addId(uuid1).build();
        ReadRecordsRequestUsingIds<HeartRateRecord> heartRateRequest =
                new ReadRecordsRequestUsingIds.Builder<>(HeartRateRecord.class)
                        .addId(uuid2)
                        .build();
        List<StepsRecord> insertedStepsRecords = TestUtils.readRecords(stepsRequest);

        assertThat(insertedStepsRecords).hasSize(1);
        StepsRecord insertedStepsRecord = insertedStepsRecords.get(0);
        List<HeartRateRecord> insertedHeartRateRecords = TestUtils.readRecords(heartRateRequest);
        assertThat(insertedHeartRateRecords).hasSize(1);
        HeartRateRecord insertedHeartRateRecord = insertedHeartRateRecords.get(0);
        assertThat(insertedStepsRecord.getCount()).isEqualTo(stepsRecord.getCount());
        assertThat(insertedStepsRecord.getStartTime()).isEqualTo(stepsRecord.getStartTime());
        assertThat(insertedStepsRecord.getEndTime()).isEqualTo(stepsRecord.getEndTime());
        assertThat(insertedStepsRecord.getStartZoneOffset())
                .isEqualTo(stepsRecord.getStartZoneOffset());
        assertThat(insertedStepsRecord.getEndZoneOffset())
                .isEqualTo(stepsRecord.getEndZoneOffset());
        assertThat(insertedStepsRecord.getMetadata().getDevice())
                .isEqualTo(stepsRecord.getMetadata().getDevice());
        assertThat(insertedStepsRecord.getMetadata().getDevice()).isEqualTo(buildDevice());
        assertThat(stepsRecord.getMetadata().getDataOrigin().getPackageName())
                .isEqualTo("android.healthconnect.cts");
        assertThat(
                        TestUtils.isMaskedSyntheticPackageName(
                                insertedStepsRecord.getMetadata().getDataOrigin().getPackageName()))
                .isTrue();
        assertThat(insertedHeartRateRecord.getSamples()).isEqualTo(heartRateRecord.getSamples());
        assertThat(insertedHeartRateRecord.getStartTime())
                .isEqualTo(heartRateRecord.getStartTime());
        assertThat(insertedHeartRateRecord.getEndTime()).isEqualTo(heartRateRecord.getEndTime());
        assertThat(insertedHeartRateRecord.getStartZoneOffset())
                .isEqualTo(heartRateRecord.getStartZoneOffset());
        assertThat(insertedHeartRateRecord.getEndZoneOffset())
                .isEqualTo(heartRateRecord.getEndZoneOffset());
        assertThat(insertedHeartRateRecord.getMetadata().getDevice())
                .isEqualTo(heartRateRecord.getMetadata().getDevice());
        assertThat(insertedHeartRateRecord.getMetadata().getDevice()).isEqualTo(buildDevice());
        assertThat(heartRateRecord.getMetadata().getDataOrigin().getPackageName())
                .isEqualTo("android.healthconnect.cts");
        assertThat(
                        TestUtils.isMaskedSyntheticPackageName(
                                insertedHeartRateRecord
                                        .getMetadata()
                                        .getDataOrigin()
                                        .getPackageName()))
                .isTrue();
        assertThat(insertedHeartRateRecord.getMetadata().getDevice())
                .isEqualTo(insertedStepsRecord.getMetadata().getDevice());
    }
}
