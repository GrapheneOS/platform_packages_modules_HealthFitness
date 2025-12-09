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

import static android.health.connect.HealthPermissions.READ_STEPS;
import static android.healthconnect.testing.cts.TestOutcomeReceiver.outcomeExecutor;

import static com.android.healthfitness.flags.Flags.FLAG_DEVELOPMENT_DATABASE_RW;
import static com.android.healthfitness.flags.Flags.FLAG_DEVICE_DATA_PROVIDERS_API;
import static com.android.healthfitness.flags.Flags.FLAG_DEVICE_DATA_PROVIDERS_DB;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.health.connect.DeviceDataSource;
import android.health.connect.DeviceDataTypeSource;
import android.health.connect.GetDeviceDataSourcesResponse;
import android.health.connect.HealthConnectManager;
import android.health.connect.datatypes.Device;
import android.health.connect.datatypes.DistanceRecord;
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

import androidx.test.core.app.ApplicationProvider;
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
    FLAG_DEVELOPMENT_DATABASE_RW
})
public class DeviceDataSourceTest {

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Rule
    public AssumptionCheckerRule mSupportedHardwareRule =
            new AssumptionCheckerRule(
                    DeviceSupportUtils::isHealthConnectFullySupported,
                    "Tests should run on supported hardware only.");

    private HealthConnectManager mManager;

    private static final Device TEST_DEVICE =
            new Device.Builder()
                    .setManufacturer("TestManufacturer")
                    .setModel("TestModel")
                    .setType(Device.DEVICE_TYPE_PHONE)
                    .setDisplayName("TestDisplayName")
                    .build();
    private static final String TEST_DEVICE_ID = "TestDeviceId";

    private DeviceDataAdvertisement createAdvertisement(Set<DeviceDataTypeAdvertisement> ads) {
        return new DeviceDataAdvertisement(TEST_DEVICE, TEST_DEVICE_ID, ads);
    }

    @Before
    public void before() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        mManager = context.getSystemService(HealthConnectManager.class);
        TestUtils.deleteAllDataFromHealthConnect();
    }

    @After
    public void after() throws InterruptedException {
        TestUtils.deleteAllDataFromHealthConnect();
    }

    @Test
    public void getDeviceDataSources_noAdvertisements_returnsEmpty() throws InterruptedException {
        List<DeviceDataSource> dataSources =
                HealthConnectReceiver.<GetDeviceDataSourcesResponse>callAndGetResponse(
                                (executor, receiver) ->
                                        mManager.getDeviceDataSources(executor, receiver))
                        .getDeviceDataSources();
        assertThat(dataSources).isEmpty();
    }

    @Test
    public void getDeviceDataSources_withReadPermission_returnsDevice()
            throws InterruptedException {
        Set<DeviceDataTypeAdvertisement> ads =
                Set.of(
                        new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                                .setAvailable(true)
                                .setUserEnabled(true)
                                .build());
        DeviceDataAdvertisement advertisement = createAdvertisement(ads);
        HealthConnectReceiver<Void> receiver = new HealthConnectReceiver<>();

        TestUtils.advertiseDeviceDataSources(Set.of(advertisement), outcomeExecutor(), receiver);

        TestUtils.verifyGetDeviceDataSourcesWithPermission(
                READ_STEPS,
                dataSources -> {
                    assertThat(dataSources).hasSize(1);
                    DeviceDataSource dataSource = dataSources.get(0);
                    assertThat(dataSource.getDevice().getManufacturer())
                            .isEqualTo("TestManufacturer");
                    assertThat(dataSource.getDevice().getModel()).isEqualTo("TestModel");

                    assertThat(dataSource.getDeviceDataTypeSources()).hasSize(1);
                    DeviceDataTypeSource typeSource =
                            dataSource.getDeviceDataTypeSources().iterator().next();
                    assertThat(typeSource.getDataType()).isEqualTo(StepsRecord.class);
                    assertThat(typeSource.isAvailable()).isTrue();
                    assertThat(typeSource.isUserEnabled()).isTrue();
                });
    }

    @Test
    @Ignore("b/464299514 - once we have TestAppProxy support we can test this.")
    public void getDeviceDataSources_withoutReadPermission_returnsEmpty()
            throws InterruptedException {
        Set<DeviceDataTypeAdvertisement> ads =
                Set.of(
                        new DeviceDataTypeAdvertisement.Builder(DistanceRecord.class)
                                .setAvailable(true)
                                .setUserEnabled(true)
                                .build());
        DeviceDataAdvertisement advertisement = createAdvertisement(ads);
        HealthConnectReceiver<Void> receiver = new HealthConnectReceiver<>();

        TestUtils.advertiseDeviceDataSources(Set.of(advertisement), outcomeExecutor(), receiver);

        // TODO delegate to separate app via TestAppProxy to test this.
        TestUtils.verifyGetDeviceDataSourcesWithPermission(
                null,
                dataSources -> {
                    assertThat(dataSources).isEmpty();
                });
    }

    @Test
    public void getDeviceDataSources_partialPermissions_returnsAllDataTypes()
            throws InterruptedException {
        Set<DeviceDataTypeAdvertisement> ads =
                Set.of(
                        new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                                .setAvailable(true)
                                .build(),
                        new DeviceDataTypeAdvertisement.Builder(DistanceRecord.class)
                                .setAvailable(true)
                                .build());
        DeviceDataAdvertisement advertisement = createAdvertisement(ads);
        HealthConnectReceiver<Void> receiver = new HealthConnectReceiver<>();

        TestUtils.advertiseDeviceDataSources(Set.of(advertisement), outcomeExecutor(), receiver);

        // Grant ONLY READ_STEPS. We should still see the other data type (distance).
        TestUtils.verifyGetDeviceDataSourcesWithPermission(
                READ_STEPS,
                dataSources -> {
                    assertThat(dataSources).hasSize(1);
                    DeviceDataSource dataSource = dataSources.get(0);

                    // Should see BOTH Steps and Distance
                    assertThat(dataSource.getDeviceDataTypeSources()).hasSize(2);
                    boolean hasSteps =
                            dataSource.getDeviceDataTypeSources().stream()
                                    .anyMatch(s -> s.getDataType().equals(StepsRecord.class));
                    boolean hasDistance =
                            dataSource.getDeviceDataTypeSources().stream()
                                    .anyMatch(s -> s.getDataType().equals(DistanceRecord.class));

                    assertThat(hasSteps).isTrue();
                    assertThat(hasDistance).isTrue();
                });
    }

    // TODO(b/464300453) ensure native step tracker is included even when no advertisements made.
}
