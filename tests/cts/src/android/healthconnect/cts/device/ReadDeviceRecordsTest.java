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

import static android.health.connect.HealthPermissions.MANAGE_HEALTH_DATA_PERMISSION;
import static android.healthconnect.testing.cts.TestOutcomeReceiver.outcomeExecutor;
import static android.healthconnect.testing.cts.TestUtils.advertiseDevice;
import static android.healthconnect.testing.cts.TestUtils.advertiseDeviceDataSources;
import static android.healthconnect.testing.cts.TestUtils.insertDeviceRecords;
import static android.healthconnect.testing.cts.TestUtils.insertRecords;
import static android.healthconnect.testing.cts.TestUtils.readDeviceRecords;
import static android.healthconnect.testing.shared.DataFactory.getStepsRecord;

import static com.android.compatibility.common.util.SystemUtil.runWithShellPermissionIdentity;
import static com.android.healthfitness.flags.Flags.FLAG_DEVELOPMENT_DATABASE_RW;
import static com.android.healthfitness.flags.Flags.FLAG_DEVICE_DATA_PROVIDERS_API;
import static com.android.healthfitness.flags.Flags.FLAG_DEVICE_DATA_PROVIDERS_DB;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.health.connect.HealthConnectException;
import android.health.connect.HealthConnectManager;
import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.ReadRecordsRequestUsingIds;
import android.health.connect.ReadRecordsResponse;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.Device;
import android.health.connect.datatypes.SleepSessionRecord;
import android.health.connect.datatypes.StepsRecord;
import android.health.connect.device.DeviceDataAdvertisement;
import android.health.connect.device.DeviceDataTypeAdvertisement;
import android.healthconnect.testing.cts.HealthConnectReceiver;
import android.healthconnect.testing.cts.TestUtils;
import android.healthconnect.testing.shared.AssumptionCheckerRule;
import android.healthconnect.testing.shared.DataFactory;
import android.healthconnect.testing.shared.DeviceSupportUtils;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RunWith(AndroidJUnit4.class)
@RequiresFlagsEnabled({
    FLAG_DEVICE_DATA_PROVIDERS_API,
    FLAG_DEVICE_DATA_PROVIDERS_DB,
    FLAG_DEVELOPMENT_DATABASE_RW
})
public class ReadDeviceRecordsTest {
    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    private final String mDeviceId = "Test id";

    @Rule
    public AssumptionCheckerRule mSupportedHardwareRule =
            new AssumptionCheckerRule(
                    DeviceSupportUtils::isHealthConnectFullySupported,
                    "Tests should run on supported hardware only.");

    private HealthConnectManager mHealthConnectManager;

    @Before
    public void before() throws Exception {
        TestUtils.deleteAllDataFromHealthConnect();
        mHealthConnectManager = TestUtils.getHealthConnectManager();
    }

    @After
    public void after() throws InterruptedException {
        TestUtils.deleteAllDataFromHealthConnect();
    }

    @Test
    public void withUnknownDeviceId_readDeviceRecords_error() {
        HealthConnectReceiver<ReadRecordsResponse<StepsRecord>> receiver =
                new HealthConnectReceiver<>();

        HealthConnectException exception =
                runWithShellPermissionIdentity(
                        () -> {
                            mHealthConnectManager.readDeviceRecords(
                                    new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                                            .setDeviceId("unknown")
                                            .build(),
                                    outcomeExecutor(),
                                    receiver);
                            return receiver.assertAndGetException();
                        },
                        MANAGE_HEALTH_DATA_PERMISSION);

        assertThat(exception.getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_INVALID_ARGUMENT);
    }

    @Test
    public void withEmptyDeviceIdInFiltersRequest_readDeviceRecords_returnsEmpty()
            throws InterruptedException {
        advertiseDevice(mDeviceId, StepsRecord.class);

        List<StepsRecord> readRecords =
                readDeviceRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class).build());

        assertThat(readRecords.size()).isEqualTo(0);
    }

    @Test
    public void withDataOrigin_readDeviceRecords_error() {
        HealthConnectReceiver<ReadRecordsResponse<StepsRecord>> receiver =
                new HealthConnectReceiver<>();

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        mHealthConnectManager.readDeviceRecords(
                                new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                                        .addDataOrigins(
                                                new DataOrigin.Builder()
                                                        .setPackageName("unknown")
                                                        .build())
                                        .build(),
                                outcomeExecutor(),
                                receiver));
    }

    @Test
    public void withoutPermissions_readDeviceRecords_error() throws InterruptedException {
        HealthConnectReceiver<ReadRecordsResponse<StepsRecord>> receiver =
                new HealthConnectReceiver<>();

        mHealthConnectManager.readDeviceRecords(
                new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                        .setDeviceId("unknown")
                        .build(),
                outcomeExecutor(),
                receiver);

        assertThat(receiver.assertAndGetException().getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_SECURITY);
    }

    @Test
    public void withoutAnyData_readDeviceRecords_returnsEmpty() throws InterruptedException {
        advertiseDevice(mDeviceId, StepsRecord.class);

        List<StepsRecord> readRecords =
                readDeviceRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                                .setDeviceId(mDeviceId)
                                .build());

        assertThat(readRecords.size()).isEqualTo(0);
    }

    @Test
    public void withoutAnySleepsData_readDeviceRecords_returnsEmpty() throws InterruptedException {
        // Insert a sets of test records for StepRecords, ExerciseSessionRecord, HeartRateRecord,
        // BasalMetabolicRateRecord.
        insertRecords(DataFactory.getTestRecords());

        advertiseDevice(mDeviceId, SleepSessionRecord.class);

        List<SleepSessionRecord> readRecords =
                readDeviceRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(SleepSessionRecord.class)
                                .setDeviceId(mDeviceId)
                                .build());

        assertThat(readRecords.size()).isEqualTo(0);
    }

    @Test
    public void withDifferentDataAndSources_readDeviceRecords_returnsOnlyDeviceSteps()
            throws InterruptedException {
        // Insert a sets of test records for StepRecords, ExerciseSessionRecord, HeartRateRecord,
        // BasalMetabolicRateRecord.
        insertRecords(DataFactory.getTestRecords());

        advertiseDevice(mDeviceId, StepsRecord.class);

        List<StepsRecord> records = List.of(getStepsRecord(123));
        insertDeviceRecords(mDeviceId, records).get(0);

        List<StepsRecord> readRecords =
                readDeviceRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                                .setDeviceId(mDeviceId)
                                .build());

        assertThat(readRecords.size()).isEqualTo(1);
        assertThat(readRecords.get(0).getCount()).isEqualTo(123);
    }

    @Test
    public void withDifferentDataSourcesAndMultipleDataOrigins_readDeviceRecords_error()
            throws InterruptedException {
        // Insert a sets of test records for StepRecords, ExerciseSessionRecord, HeartRateRecord,
        // BasalMetabolicRateRecord.
        insertRecords(DataFactory.getTestRecords());
        String appRecordsPackageName =
                ApplicationProvider.getApplicationContext().getAttributionSource().getPackageName();

        advertiseDevice(mDeviceId, StepsRecord.class);

        List<StepsRecord> records = List.of(getStepsRecord(123));
        insertDeviceRecords(mDeviceId, records).get(0);

        HealthConnectReceiver<ReadRecordsResponse<StepsRecord>> receiver =
                new HealthConnectReceiver<>();

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        mHealthConnectManager.readDeviceRecords(
                                new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                                        .addDataOrigins(
                                                new DataOrigin.Builder()
                                                        .setPackageName(mDeviceId)
                                                        .build())
                                        .addDataOrigins(
                                                new DataOrigin.Builder()
                                                        .setPackageName(appRecordsPackageName)
                                                        .build())
                                        .build(),
                                outcomeExecutor(),
                                receiver));
    }

    @Test
    public void withDifferentRecordTypes_readDeviceRecords_returnsOnlyRequestedType()
            throws InterruptedException {
        Device device = DataFactory.buildDevice();
        Set<DeviceDataTypeAdvertisement> deviceDataTypeAdvertisements =
                Set.of(
                        new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                                .setAvailable(true)
                                .build(),
                        new DeviceDataTypeAdvertisement.Builder(SleepSessionRecord.class)
                                .setAvailable(true)
                                .build());
        DeviceDataAdvertisement advertisement =
                new DeviceDataAdvertisement(device, mDeviceId, deviceDataTypeAdvertisements);
        HealthConnectReceiver<Void> receiver = new HealthConnectReceiver<>();

        advertiseDeviceDataSources(Set.of(advertisement), outcomeExecutor(), receiver);

        receiver.verifyNoExceptionOrThrow();

        List<StepsRecord> stepsRecords = List.of(getStepsRecord(123));
        List<SleepSessionRecord> sleepRecords = List.of(DataFactory.buildSleepSession());
        insertDeviceRecords(mDeviceId, stepsRecords);
        insertDeviceRecords(mDeviceId, sleepRecords);

        List<StepsRecord> readRecords =
                readDeviceRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                                .setDeviceId(mDeviceId)
                                .build());

        assertThat(readRecords.size()).isEqualTo(1);
        assertThat(readRecords.get(0).getClass()).isEqualTo(StepsRecord.class);
    }

    @Test
    public void withRequestUsingId_readDeviceRecords_success() throws InterruptedException {
        advertiseDevice(mDeviceId, StepsRecord.class);

        List<StepsRecord> stepsRecords = List.of(getStepsRecord(123));
        String uuid = insertDeviceRecords(mDeviceId, stepsRecords).get(0).getMetadata().getId();

        ReadRecordsRequestUsingIds<StepsRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(StepsRecord.class).addId(uuid).build();

        List<StepsRecord> readRecords = readDeviceRecords(request);

        assertThat(readRecords.size()).isEqualTo(1);
        assertThat(readRecords.get(0).getCount()).isEqualTo(123);
    }

    @Test
    public void withRequestUsingIds_readDeviceRecords_ignoresMissingIds()
            throws InterruptedException {
        advertiseDevice(mDeviceId, StepsRecord.class);

        List<StepsRecord> stepsRecords = List.of(getStepsRecord(123));
        String uuid = insertDeviceRecords(mDeviceId, stepsRecords).get(0).getMetadata().getId();

        ReadRecordsRequestUsingIds<StepsRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(StepsRecord.class)
                        .addId(UUID.randomUUID().toString())
                        .addId(uuid)
                        .addId(UUID.randomUUID().toString())
                        .build();

        List<StepsRecord> readRecords = readDeviceRecords(request);

        assertThat(readRecords.size()).isEqualTo(1);
        assertThat(readRecords.get(0).getCount()).isEqualTo(123);
    }

    @Test
    public void withMultipleAdvertisements_readDeviceRecords_onlyReadsOwnRecords()
            throws InterruptedException {
        String deviceIdOne = "Hello";
        String deviceIdTwo = "World";

        TestUtils.advertiseDevices(Set.of(deviceIdOne, deviceIdTwo));

        List<StepsRecord> stepsRecordsOne = List.of(getStepsRecord(111));
        List<StepsRecord> stepsRecordsTwo = List.of(getStepsRecord(222));

        insertDeviceRecords(deviceIdOne, stepsRecordsOne);
        insertDeviceRecords(deviceIdTwo, stepsRecordsTwo);

        List<StepsRecord> readRecordsOne =
                readDeviceRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                                .setDeviceId(deviceIdOne)
                                .build());

        assertThat(readRecordsOne.size()).isEqualTo(1);
        assertThat(readRecordsOne.get(0).getCount()).isEqualTo(111);

        List<StepsRecord> readRecordsTwo =
                readDeviceRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                                .setDeviceId(deviceIdTwo)
                                .build());

        assertThat(readRecordsTwo.size()).isEqualTo(1);
        assertThat(readRecordsTwo.get(0).getCount()).isEqualTo(222);
    }

    @Test
    public void withMultipleAdvertisementsAndEmptyRequest_readDeviceRecords_returnsAllRecords()
            throws InterruptedException {
        String deviceIdOne = "Hello";
        String deviceIdTwo = "World";

        TestUtils.advertiseDevices(Set.of(deviceIdOne, deviceIdTwo));

        List<StepsRecord> stepsRecordsOne = List.of(getStepsRecord(111));
        List<StepsRecord> stepsRecordsTwo = List.of(getStepsRecord(222));

        insertDeviceRecords(deviceIdOne, stepsRecordsOne);
        insertDeviceRecords(deviceIdTwo, stepsRecordsTwo);

        List<StepsRecord> readRecords =
                readDeviceRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class).build());

        assertThat(readRecords.size()).isEqualTo(2);
        assertThat(readRecords.get(0).getCount()).isEqualTo(111);
        assertThat(readRecords.get(1).getCount()).isEqualTo(222);
    }
}
