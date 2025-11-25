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

import static android.healthconnect.testing.cts.TestOutcomeReceiver.outcomeExecutor;
import static android.healthconnect.testing.cts.TestUtils.advertiseDevice;
import static android.healthconnect.testing.cts.TestUtils.advertiseDevices;
import static android.healthconnect.testing.cts.TestUtils.deleteAllDataFromHealthConnect;
import static android.healthconnect.testing.cts.TestUtils.deleteDeviceRecords;
import static android.healthconnect.testing.cts.TestUtils.insertDeviceRecords;
import static android.healthconnect.testing.cts.TestUtils.insertRecords;
import static android.healthconnect.testing.cts.TestUtils.readAllRecords;
import static android.healthconnect.testing.cts.TestUtils.readDeviceRecords;
import static android.healthconnect.testing.shared.DataFactory.buildDevice;

import static com.android.healthfitness.flags.Flags.FLAG_DEVELOPMENT_DATABASE_RW;
import static com.android.healthfitness.flags.Flags.FLAG_DEVICE_DATA_PROVIDERS_API;
import static com.android.healthfitness.flags.Flags.FLAG_DEVICE_DATA_PROVIDERS_DB;

import static com.google.common.truth.Truth.assertThat;

import android.health.connect.HealthConnectException;
import android.health.connect.HealthConnectManager;
import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.datatypes.BasalMetabolicRateRecord;
import android.health.connect.datatypes.ExerciseSessionRecord;
import android.health.connect.datatypes.HeartRateRecord;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.SleepSessionRecord;
import android.health.connect.datatypes.StepsRecord;
import android.health.connect.device.DeviceDataTypeAdvertisement;
import android.healthconnect.testing.cts.HealthConnectReceiver;
import android.healthconnect.testing.cts.TestUtils;
import android.healthconnect.testing.shared.AssumptionCheckerRule;
import android.healthconnect.testing.shared.DataFactory;
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

import java.time.Instant;
import java.util.List;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
@RequiresFlagsEnabled({
    FLAG_DEVICE_DATA_PROVIDERS_API,
    FLAG_DEVICE_DATA_PROVIDERS_DB,
    FLAG_DEVELOPMENT_DATABASE_RW
})
public class DeleteDeviceRecordsTest {

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    private final String mDeviceId = "Test id";
    private Instant mDefaultStartTime;

    private HealthConnectManager mHealthConnectManager;

    @Rule
    public AssumptionCheckerRule mSupportedHardwareRule =
            new AssumptionCheckerRule(
                    DeviceSupportUtils::isHealthConnectFullySupported,
                    "Tests should run on supported hardware only.");

    @Before
    public void before() throws Exception {
        deleteAllDataFromHealthConnect();
        mDefaultStartTime = Instant.now().minusSeconds(1000);
        mHealthConnectManager = TestUtils.getHealthConnectManager();
    }

    @After
    public void after() throws InterruptedException {
        deleteAllDataFromHealthConnect();
    }

    @Test
    public void withoutPermission_deleteDeviceRecords_throws() throws InterruptedException {
        HealthConnectReceiver<Void> receiver = new HealthConnectReceiver<>();
        advertiseDevice(mDeviceId, StepsRecord.class);

        mHealthConnectManager.deleteDeviceRecords(
                mDeviceId,
                StepsRecord.class,
                new TimeInstantRangeFilter.Builder().setStartTime(mDefaultStartTime).build(),
                outcomeExecutor(),
                receiver);

        assertThat(receiver.assertAndGetException().getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_SECURITY);
    }

    @Test
    public void withoutAdvertisement_deleteDeviceRecords_throws() throws InterruptedException {
        HealthConnectReceiver<Void> receiver = new HealthConnectReceiver<>();

        deleteDeviceRecords(
                mDeviceId,
                StepsRecord.class,
                new TimeInstantRangeFilter.Builder().setStartTime(mDefaultStartTime).build(),
                outcomeExecutor(),
                receiver);

        assertThat(receiver.assertAndGetException().getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_INVALID_ARGUMENT);
    }

    @Test
    public void withUnknownDeviceId_deleteDeviceRecords_throws() throws InterruptedException {
        HealthConnectReceiver<Void> receiver = new HealthConnectReceiver<>();
        advertiseDevice(mDeviceId, StepsRecord.class);

        deleteDeviceRecords(
                "unknown",
                StepsRecord.class,
                new TimeInstantRangeFilter.Builder().setStartTime(mDefaultStartTime).build(),
                outcomeExecutor(),
                receiver);

        assertThat(receiver.assertAndGetException().getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_INVALID_ARGUMENT);
    }

    @Test
    public void withoutAnyRecords_deleteDeviceRecords_doesNotThrow() throws InterruptedException {
        advertiseDevice(mDeviceId, StepsRecord.class);

        HealthConnectReceiver<Void> receiver = new HealthConnectReceiver<>();
        deleteDeviceRecords(
                mDeviceId,
                StepsRecord.class,
                new TimeInstantRangeFilter.Builder().setStartTime(mDefaultStartTime).build(),
                outcomeExecutor(),
                receiver);

        receiver.verifyNoExceptionOrThrow();
    }

    @Test
    public void withAdvertisementAndRecord_deleteDeviceRecords_deletesRecord()
            throws InterruptedException {
        advertiseDevice(mDeviceId, StepsRecord.class);

        List<StepsRecord> records = List.of(DataFactory.getStepsRecord(123));
        insertDeviceRecords(mDeviceId, records);

        deleteDeviceRecords(
                mDeviceId,
                StepsRecord.class,
                new TimeInstantRangeFilter.Builder().setStartTime(mDefaultStartTime).build());

        List<StepsRecord> readRecords =
                readDeviceRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                                .setDeviceId(mDeviceId)
                                .build());

        assertThat(readRecords.size()).isEqualTo(0);
    }

    @Test
    public void withAppSources_deleteDeviceRecords_doesNotDeleteOtherApps()
            throws InterruptedException {
        // Insert a sets of test records for StepRecords, ExerciseSessionRecord, HeartRateRecord,
        // BasalMetabolicRateRecord.
        insertRecords(DataFactory.getTestRecords());

        advertiseDevice(mDeviceId, StepsRecord.class);

        assertThat(readAllRecords(StepsRecord.class).size()).isEqualTo(1);
        assertThat(readAllRecords(ExerciseSessionRecord.class).size()).isEqualTo(1);
        assertThat(readAllRecords(HeartRateRecord.class).size()).isEqualTo(1);
        assertThat(readAllRecords(BasalMetabolicRateRecord.class).size()).isEqualTo(1);

        deleteDeviceRecords(
                mDeviceId,
                StepsRecord.class,
                new TimeInstantRangeFilter.Builder().setStartTime(mDefaultStartTime).build());

        assertThat(readAllRecords(StepsRecord.class).size()).isEqualTo(1);
        assertThat(readAllRecords(ExerciseSessionRecord.class).size()).isEqualTo(1);
        assertThat(readAllRecords(HeartRateRecord.class).size()).isEqualTo(1);
        assertThat(readAllRecords(BasalMetabolicRateRecord.class).size()).isEqualTo(1);
    }

    @Test
    public void withMultipleRecordTypes_deleteDeviceRecords_deletesOnlyFilteredType()
            throws InterruptedException {
        Set<DeviceDataTypeAdvertisement> deviceDataTypeAdvertisements =
                Set.of(
                        new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                                .setAvailable(true)
                                .build(),
                        new DeviceDataTypeAdvertisement.Builder(SleepSessionRecord.class)
                                .setAvailable(true)
                                .build());

        advertiseDevice(mDeviceId, buildDevice(), deviceDataTypeAdvertisements);

        List<Record> records =
                List.of(DataFactory.getStepsRecord(111), DataFactory.buildSleepSession());
        insertDeviceRecords(mDeviceId, records);

        assertThat(readAllRecords(StepsRecord.class).size()).isEqualTo(1);
        assertThat(readAllRecords(SleepSessionRecord.class).size()).isEqualTo(1);

        deleteDeviceRecords(
                mDeviceId,
                StepsRecord.class,
                new TimeInstantRangeFilter.Builder().setStartTime(mDefaultStartTime).build());

        assertThat(readAllRecords(StepsRecord.class).size()).isEqualTo(0);
        assertThat(readAllRecords(SleepSessionRecord.class).size()).isEqualTo(1);
    }

    @Test
    public void withMultipleAdvertisements_deleteDeviceRecords_deletesOwnRecord()
            throws InterruptedException {
        String deviceIdOne = "Hello";
        String deviceIdTwo = "World";

        advertiseDevices(Set.of(deviceIdOne, deviceIdTwo));

        List<StepsRecord> stepsRecordsOne = List.of(DataFactory.getStepsRecord(111));
        List<StepsRecord> stepsRecordsTwo = List.of(DataFactory.getStepsRecord(222));

        insertDeviceRecords(deviceIdOne, stepsRecordsOne);
        insertDeviceRecords(deviceIdTwo, stepsRecordsTwo);

        ReadRecordsRequestUsingFilters<StepsRecord> readRequestOne =
                new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                        .setDeviceId(deviceIdOne)
                        .build();

        ReadRecordsRequestUsingFilters<StepsRecord> readRequestTwo =
                new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                        .setDeviceId(deviceIdTwo)
                        .build();

        assertThat(TestUtils.readDeviceRecords(readRequestOne).size()).isEqualTo(1);
        assertThat(TestUtils.readDeviceRecords(readRequestTwo).size()).isEqualTo(1);

        deleteDeviceRecords(
                deviceIdOne,
                StepsRecord.class,
                new TimeInstantRangeFilter.Builder().setStartTime(mDefaultStartTime).build());

        assertThat(TestUtils.readDeviceRecords(readRequestOne).size()).isEqualTo(0);
        assertThat(TestUtils.readDeviceRecords(readRequestTwo).size()).isEqualTo(1);
    }

    @Test
    public void withTimeFilter_deleteDeviceRecords_deletesForGivenTime()
            throws InterruptedException {
        advertiseDevice(mDeviceId, StepsRecord.class);

        Instant now = Instant.now();

        List<Record> records =
                List.of(
                        new StepsRecord.Builder(
                                        new Metadata.Builder().build(),
                                        now.minusSeconds(25),
                                        now.minusSeconds(20),
                                        100)
                                .build(),
                        new StepsRecord.Builder(
                                        new Metadata.Builder().build(),
                                        now.minusSeconds(15),
                                        now.minusSeconds(10),
                                        200)
                                .build(),
                        new StepsRecord.Builder(
                                        new Metadata.Builder().build(),
                                        now.minusSeconds(5),
                                        now,
                                        300)
                                .build());

        insertDeviceRecords(mDeviceId, records);
        assertThat(readAllRecords(StepsRecord.class).size()).isEqualTo(3);

        deleteDeviceRecords(
                mDeviceId,
                StepsRecord.class,
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(now.minusSeconds(17))
                        .setEndTime(now.minusSeconds(8))
                        .build());

        assertThat(readAllRecords(StepsRecord.class).size()).isEqualTo(2);
        StepsRecord firstRecord = readAllRecords(StepsRecord.class).get(0);
        StepsRecord lastRecord = readAllRecords(StepsRecord.class).get(1);
        assertThat(firstRecord.getCount()).isEqualTo(100);
        assertThat(lastRecord.getCount()).isEqualTo(300);
    }
}
