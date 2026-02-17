/*
 * Copyright (C) 2026 The Android Open Source Project
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

import static android.health.connect.HealthDataCategory.ACTIVITY;
import static android.health.connect.HealthDataCategory.SLEEP;
import static android.health.connect.HealthPermissions.MANAGE_HEALTH_DATA_PERMISSION;
import static android.healthconnect.testing.cts.HealthConnectReceiver.callAndGetResponseWithShellPermissionIdentity;
import static android.healthconnect.testing.cts.TestOutcomeReceiver.outcomeExecutor;
import static android.healthconnect.testing.cts.TestUtils.advertiseDevice;
import static android.healthconnect.testing.cts.TestUtils.advertiseDeviceDataSources;
import static android.healthconnect.testing.cts.TestUtils.deleteAllDataFromHealthConnect;
import static android.healthconnect.testing.cts.TestUtils.deleteDeviceRecords;
import static android.healthconnect.testing.cts.TestUtils.insertDeviceRecords;

import static com.android.healthfitness.flags.Flags.FLAG_DEVICE_DATA_PROVIDERS_API;
import static com.android.healthfitness.flags.Flags.FLAG_DEVICE_DATA_PROVIDERS_DB;

import static com.google.common.truth.Truth.assertThat;

import static java.util.Objects.requireNonNull;

import android.content.Context;
import android.health.connect.FetchDataOriginsPriorityOrderResponse;
import android.health.connect.HealthConnectManager;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.TimeRangeFilter;
import android.health.connect.datatypes.Device;
import android.health.connect.datatypes.SleepSessionRecord;
import android.health.connect.datatypes.StepsRecord;
import android.health.connect.device.DeviceDataAdvertisement;
import android.health.connect.device.DeviceDataTypeAdvertisement;
import android.healthconnect.testing.cts.HealthConnectReceiver;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
@RequiresFlagsEnabled({FLAG_DEVICE_DATA_PROVIDERS_API, FLAG_DEVICE_DATA_PROVIDERS_DB})
public class DeviceDataPriorityListTest {

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Rule
    public AssumptionCheckerRule mSupportedHardwareRule =
            new AssumptionCheckerRule(
                    DeviceSupportUtils::isHealthConnectFullySupported,
                    "Tests should run on supported hardware only.");

    private HealthConnectManager mManager;

    private static final String DEVICE_ID = "TestDeviceId";
    private static final Device DEVICE =
            new Device.Builder()
                    .setManufacturer("TestManufacturer")
                    .setModel("TestModel")
                    .setType(Device.DEVICE_TYPE_PHONE)
                    .setDisplayName("TestDisplayName")
                    .build();

    @Before
    public void setUp() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        mManager = requireNonNull(context.getSystemService(HealthConnectManager.class));
        deleteAllDataFromHealthConnect();
    }

    @After
    public void tearDown() throws InterruptedException {
        deleteAllDataFromHealthConnect();
    }

    @Test
    public void afterDeviceAdvertisement_noData_notAddedToPriorityList() throws Exception {
        advertiseDevice(DEVICE_ID, DEVICE, StepsRecord.class);

        assertThat(isPackageInPriorityList(ACTIVITY)).isFalse();
    }

    @Test
    public void advertiseDeviceWithNoTypes_notAddedToPriorityList() throws Exception {
        HealthConnectReceiver<Void> adReceiver = new HealthConnectReceiver<>();
        DeviceDataAdvertisement advertisement =
                new DeviceDataAdvertisement(DEVICE, DEVICE_ID, Collections.emptySet());
        advertiseDeviceDataSources(Set.of(advertisement), outcomeExecutor(), adReceiver);
        adReceiver.verifyNoExceptionOrThrow();

        assertThat(isPackageInPriorityList(ACTIVITY)).isFalse();
    }

    @Test
    public void afterDeviceAdvertisement_addData_addsToPriorityList() throws Exception {
        advertiseDevice(DEVICE_ID, DEVICE, StepsRecord.class);
        insertDeviceRecords(DEVICE_ID, Collections.singletonList(createStepsRecord()));

        assertThat(isPackageInPriorityList(ACTIVITY)).isTrue();
    }

    @Test
    public void removeDeviceAdvertisement_withData_doesNotRemoveFromPriorityList()
            throws Exception {
        advertiseDevice(DEVICE_ID, DEVICE, StepsRecord.class);
        insertDeviceRecords(DEVICE_ID, Collections.singletonList(createStepsRecord()));

        HealthConnectReceiver<Void> adReceiver = new HealthConnectReceiver<>();
        advertiseDeviceDataSources(Collections.emptySet(), outcomeExecutor(), adReceiver);
        adReceiver.verifyNoExceptionOrThrow();

        assertThat(isPackageInPriorityList(ACTIVITY)).isTrue();
    }

    @Test
    public void removeDeviceAdvertisement_noData_removesFromPriorityList() throws Exception {
        advertiseDevice(DEVICE_ID, DEVICE, StepsRecord.class);
        insertDeviceRecords(DEVICE_ID, Collections.singletonList(createStepsRecord()));
        TimeRangeFilter timeRangeFilter =
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.EPOCH)
                        .setEndTime(Instant.now().plus(1, ChronoUnit.DAYS))
                        .build();

        deleteDeviceRecords(DEVICE_ID, StepsRecord.class, timeRangeFilter);
        HealthConnectReceiver<Void> adReceiver = new HealthConnectReceiver<>();
        advertiseDeviceDataSources(Collections.emptySet(), outcomeExecutor(), adReceiver);
        adReceiver.verifyNoExceptionOrThrow();

        assertThat(isPackageInPriorityList(ACTIVITY)).isFalse();
    }

    @Test
    // Advertise Steps (ACTIVITY) and Sleep (SLEEP), insert data, delete Steps data.
    public void removeDataTypeAdvertisement_noData_removesFromCategoryList() throws Exception {
        setupMultipleCategories(/* insertSteps */ true, /* insertSleep */ true);
        TimeRangeFilter timeRangeFilter =
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.EPOCH)
                        .setEndTime(Instant.now().plus(1, ChronoUnit.DAYS))
                        .build();
        deleteDeviceRecords(DEVICE_ID, StepsRecord.class, timeRangeFilter);

        updateAdvertisementToSleepOnly();

        assertThat(isPackageInPriorityList(ACTIVITY)).isFalse();
    }

    @Test
    // Advertise Steps (ACTIVITY) and Sleep (SLEEP), insert data, delete Steps data.
    public void removeDataTypeAdvertisement_otherCategoryHasData_remainsInOtherCategoryList()
            throws Exception {
        setupMultipleCategories(/* insertSteps */ true, /* insertSleep */ true);
        TimeRangeFilter timeRangeFilter =
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.EPOCH)
                        .setEndTime(Instant.now().plus(1, ChronoUnit.DAYS))
                        .build();
        deleteDeviceRecords(DEVICE_ID, StepsRecord.class, timeRangeFilter);

        updateAdvertisementToSleepOnly();

        assertThat(isPackageInPriorityList(SLEEP)).isTrue();
    }

    private void setupMultipleCategories(boolean insertSteps, boolean insertSleep)
            throws InterruptedException {
        Set<DeviceDataTypeAdvertisement> initialAdvertisements =
                Set.of(
                        new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                                .setAvailable(true)
                                .build(),
                        new DeviceDataTypeAdvertisement.Builder(SleepSessionRecord.class)
                                .setAvailable(true)
                                .build());
        DeviceDataAdvertisement initialAd =
                new DeviceDataAdvertisement(DEVICE, DEVICE_ID, initialAdvertisements);

        HealthConnectReceiver<Void> adReceiver = new HealthConnectReceiver<>();
        advertiseDeviceDataSources(Set.of(initialAd), outcomeExecutor(), adReceiver);
        adReceiver.verifyNoExceptionOrThrow();

        if (insertSteps) {
            insertDeviceRecords(DEVICE_ID, Collections.singletonList(createStepsRecord()));
        }
        if (insertSleep) {
            insertDeviceRecords(
                    DEVICE_ID,
                    Collections.singletonList(DataFactory.buildSleepSessionWithEmptyMetadata()));
        }
    }

    private void updateAdvertisementToSleepOnly() throws InterruptedException {
        Set<DeviceDataTypeAdvertisement> updatedAdvertisements =
                Set.of(
                        new DeviceDataTypeAdvertisement.Builder(SleepSessionRecord.class)
                                .setAvailable(true)
                                .build());
        DeviceDataAdvertisement updatedAd =
                new DeviceDataAdvertisement(DEVICE, DEVICE_ID, updatedAdvertisements);

        HealthConnectReceiver<Void> updateReceiver = new HealthConnectReceiver<>();
        advertiseDeviceDataSources(Set.of(updatedAd), outcomeExecutor(), updateReceiver);
        updateReceiver.verifyNoExceptionOrThrow();
    }

    private boolean isPackageInPriorityList(int category) throws Exception {
        FetchDataOriginsPriorityOrderResponse priority =
                callAndGetResponseWithShellPermissionIdentity(
                        (executor, receiver) -> {
                            mManager.fetchDataOriginsPriorityOrder(category, executor, receiver);
                            try {
                                receiver.verifyNoExceptionOrThrow();
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        },
                        MANAGE_HEALTH_DATA_PERMISSION);

        // Since we clear all data before each test, our device data should be the only origin.
        return !priority.getDataOriginsPriorityOrder().isEmpty();
    }

    private StepsRecord createStepsRecord() {
        return DataFactory.getStepsRecord(
                50, Instant.now().minus(1, ChronoUnit.HOURS), Instant.now());
    }
}
