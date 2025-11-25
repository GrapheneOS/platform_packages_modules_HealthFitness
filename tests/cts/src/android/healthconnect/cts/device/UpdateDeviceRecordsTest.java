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
import static android.healthconnect.testing.cts.TestUtils.insertDeviceRecords;
import static android.healthconnect.testing.cts.TestUtils.insertRecords;
import static android.healthconnect.testing.cts.TestUtils.readDeviceRecords;
import static android.healthconnect.testing.cts.TestUtils.updateDeviceRecords;
import static android.healthconnect.testing.shared.DataFactory.getStepsRecord;

import static com.android.healthfitness.flags.Flags.FLAG_DEVELOPMENT_DATABASE_RW;
import static com.android.healthfitness.flags.Flags.FLAG_DEVICE_DATA_PROVIDERS_API;
import static com.android.healthfitness.flags.Flags.FLAG_DEVICE_DATA_PROVIDERS_DB;

import static com.google.common.truth.Truth.assertThat;

import android.health.connect.HealthConnectException;
import android.health.connect.HealthConnectManager;
import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.StepsRecord;
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

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RunWith(AndroidJUnit4.class)
@RequiresFlagsEnabled({
    FLAG_DEVICE_DATA_PROVIDERS_API,
    FLAG_DEVICE_DATA_PROVIDERS_DB,
    FLAG_DEVELOPMENT_DATABASE_RW
})
public class UpdateDeviceRecordsTest {

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    private final String mDeviceId = "Test id";

    private HealthConnectManager mHealthConnectManager;

    @Rule
    public AssumptionCheckerRule mSupportedHardwareRule =
            new AssumptionCheckerRule(
                    DeviceSupportUtils::isHealthConnectFullySupported,
                    "Tests should run on supported hardware only.");

    @Before
    public void before() throws Exception {
        deleteAllDataFromHealthConnect();
        mHealthConnectManager = TestUtils.getHealthConnectManager();
    }

    @After
    public void after() throws InterruptedException {
        deleteAllDataFromHealthConnect();
    }

    @Test
    public void withoutPermission_updateDeviceRecords_throws() throws InterruptedException {
        HealthConnectReceiver<Void> receiver = new HealthConnectReceiver<>();

        mHealthConnectManager.updateDeviceRecords(
                mDeviceId, Collections.emptyList(), outcomeExecutor(), receiver);

        assertThat(receiver.assertAndGetException().getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_SECURITY);
    }

    @Test
    public void withoutAdvertisement_updateDeviceRecords_throws() throws InterruptedException {
        HealthConnectReceiver<Void> receiver = new HealthConnectReceiver<>();

        updateDeviceRecords(mDeviceId, List.of(getStepsRecord()), outcomeExecutor(), receiver);

        assertThat(receiver.assertAndGetException().getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_INVALID_ARGUMENT);
    }

    @Test
    public void withoutExistingRecord_updateDeviceRecords_throws() throws InterruptedException {
        advertiseDevice(mDeviceId, StepsRecord.class);

        StepsRecord updateRecord =
                getStepsRecord(
                        50, new Metadata.Builder().setId(UUID.randomUUID().toString()).build());

        HealthConnectReceiver<Void> receiver = new HealthConnectReceiver<>();
        updateDeviceRecords(mDeviceId, List.of(updateRecord), outcomeExecutor(), receiver);
        assertThat(receiver.assertAndGetException().getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_INVALID_ARGUMENT);
    }

    @Test
    public void withUnknownDeviceId_updateDeviceRecords_throws() throws InterruptedException {
        advertiseDevice(mDeviceId, StepsRecord.class);

        List<StepsRecord> records = List.of(DataFactory.getStepsRecord(123));
        insertDeviceRecords(mDeviceId, records);
        StepsRecord updateRecord =
                getStepsRecord(
                        50, new Metadata.Builder().setId(UUID.randomUUID().toString()).build());

        HealthConnectReceiver<Void> receiver = new HealthConnectReceiver<>();
        updateDeviceRecords("unknown id", List.of(updateRecord), outcomeExecutor(), receiver);
        assertThat(receiver.assertAndGetException().getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_INVALID_ARGUMENT);
    }

    @Test
    public void withAdvertisementAndEmptyUpdatesCall_updateDeviceRecords_doesNotThrow()
            throws InterruptedException {
        advertiseDevice(mDeviceId, StepsRecord.class);

        List<StepsRecord> records = List.of(DataFactory.getStepsRecord(123));
        insertDeviceRecords(mDeviceId, records);

        HealthConnectReceiver<Void> receiver = new HealthConnectReceiver<>();
        updateDeviceRecords(mDeviceId, List.of(), outcomeExecutor(), receiver);
        receiver.verifyNoExceptionOrThrow();
    }

    @Test
    public void withAdvertisementAndRecord_updateDeviceRecords_updatesRecord()
            throws InterruptedException {
        advertiseDevice(mDeviceId, StepsRecord.class);

        List<StepsRecord> records = List.of(DataFactory.getStepsRecord(123));
        String uuid = insertDeviceRecords(mDeviceId, records).get(0).getMetadata().getId();
        StepsRecord updateRecord = getStepsRecord(50, new Metadata.Builder().setId(uuid).build());

        updateDeviceRecords(mDeviceId, List.of(updateRecord));

        List<StepsRecord> readRecords =
                readDeviceRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                                .setDeviceId(mDeviceId)
                                .build());

        assertThat(readRecords.size()).isEqualTo(1);
        assertThat(readRecords.get(0).getCount()).isEqualTo(50);
    }

    @Test
    public void withAppSources_updateDeviceRecords_throwsWhenTryingToUpdateOther()
            throws InterruptedException {
        // Insert a sets of test records for StepRecords, ExerciseSessionRecord, HeartRateRecord,
        // BasalMetabolicRateRecord.
        List<Record> insertedAppRecords = insertRecords(DataFactory.getTestRecords());
        String stepUuid = insertedAppRecords.get(0).getMetadata().getId();

        advertiseDevice(mDeviceId, StepsRecord.class);

        StepsRecord updateRecord =
                getStepsRecord(50, new Metadata.Builder().setId(stepUuid).build());

        HealthConnectReceiver<Void> receiver = new HealthConnectReceiver<>();
        updateDeviceRecords(mDeviceId, List.of(updateRecord), outcomeExecutor(), receiver);
        assertThat(receiver.assertAndGetException().getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_INVALID_ARGUMENT);
    }

    @Test
    public void withMultipleAdvertisements_updateDeviceRecords_throwsWhenTryingToUpdateOther()
            throws InterruptedException {
        String deviceIdOne = "Hello";
        String deviceIdTwo = "World";

        advertiseDevices(Set.of(deviceIdOne, deviceIdTwo));

        List<StepsRecord> stepsRecordsOne = List.of(DataFactory.getStepsRecord(111));
        List<StepsRecord> stepsRecordsTwo = List.of(DataFactory.getStepsRecord(222));

        String uuidOne =
                insertDeviceRecords(deviceIdOne, stepsRecordsOne).get(0).getMetadata().getId();
        insertDeviceRecords(deviceIdTwo, stepsRecordsTwo);

        StepsRecord updateRecord =
                getStepsRecord(50, new Metadata.Builder().setId(uuidOne).build());

        HealthConnectReceiver<Void> receiver = new HealthConnectReceiver<>();
        updateDeviceRecords(deviceIdTwo, List.of(updateRecord), outcomeExecutor(), receiver);
        assertThat(receiver.assertAndGetException().getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_INVALID_ARGUMENT);
    }

    @Test
    public void withMultipleAdvertisements_updateDeviceRecords_updatesOwnRecord()
            throws InterruptedException {
        String deviceIdOne = "Hello";
        String deviceIdTwo = "World";

        advertiseDevices(Set.of(deviceIdOne, deviceIdTwo));

        List<StepsRecord> stepsRecordsOne = List.of(DataFactory.getStepsRecord(111));
        List<StepsRecord> stepsRecordsTwo = List.of(DataFactory.getStepsRecord(222));

        insertDeviceRecords(deviceIdOne, stepsRecordsOne);
        String uuidTwo =
                insertDeviceRecords(deviceIdTwo, stepsRecordsTwo).get(0).getMetadata().getId();

        StepsRecord updateRecord =
                getStepsRecord(50, new Metadata.Builder().setId(uuidTwo).build());

        updateDeviceRecords(deviceIdTwo, List.of(updateRecord));

        List<StepsRecord> readRecordsOne =
                TestUtils.readDeviceRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                                .setDeviceId(deviceIdOne)
                                .build());

        assertThat(readRecordsOne.size()).isEqualTo(1);
        assertThat(readRecordsOne.get(0).getCount()).isEqualTo(111);

        List<StepsRecord> readRecordsTwo =
                TestUtils.readDeviceRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                                .setDeviceId(deviceIdTwo)
                                .build());

        assertThat(readRecordsTwo.size()).isEqualTo(1);
        assertThat(readRecordsTwo.get(0).getCount()).isEqualTo(50);
    }
}
