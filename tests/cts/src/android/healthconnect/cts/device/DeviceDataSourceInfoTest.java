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
import static android.healthconnect.testing.cts.TestUtils.getCurrentDeviceId;

import static com.android.healthfitness.flags.Flags.FLAG_DEVICE_DATA_PROVIDERS_API;
import static com.android.healthfitness.flags.Flags.FLAG_DEVICE_DATA_PROVIDERS_DB;

import static com.google.common.truth.Truth.assertThat;

import android.health.connect.DeviceDataProviderInfo;
import android.health.connect.DeviceDataSourceInfo;
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

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.common.collect.Iterables;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
@RequiresFlagsEnabled({FLAG_DEVICE_DATA_PROVIDERS_API, FLAG_DEVICE_DATA_PROVIDERS_DB})
public class DeviceDataSourceInfoTest {
    private static final String SELF_PACKAGE_NAME = "android.healthconnect.cts";

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
    public void getDeviceDataSourceInfos_returnsCorrectData() throws Exception {
        Device device =
                new Device.Builder()
                        .setManufacturer("TestManufacturer")
                        .setModel("TestModel")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .setDisplayName("TestDisplayName")
                        .build();
        String deviceId = "TestDeviceId";
        DeviceDataTypeAdvertisement stepsAd =
                new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                        .setAvailable(true)
                        .build();
        Set<DeviceDataTypeAdvertisement> deviceDataTypeAdvertisements = Set.of(stepsAd);
        DeviceDataAdvertisement advertisement =
                new DeviceDataAdvertisement(device, deviceId, deviceDataTypeAdvertisements);
        HealthConnectReceiver<Void> advertiseReceiver = new HealthConnectReceiver<>();

        TestUtils.advertiseDeviceDataSources(
                Set.of(advertisement), outcomeExecutor(), advertiseReceiver);
        advertiseReceiver.verifyNoExceptionOrThrow();

        List<DeviceDataSourceInfo> result = TestUtils.getDeviceDataSourceInfos();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getDeviceDataOrigin().getPackageName())
                .isEqualTo(getCurrentDeviceId());
        DeviceDataSourceInfo info = result.get(1);
        assertThat(info.getDevice()).isEqualTo(device);
        assertThat(info.getDeviceDataProviderInfos()).hasSize(1);
        assertThat(info.isCurrentDevice()).isFalse();
        DeviceDataProviderInfo providerInfo = info.getDeviceDataProviderInfos().get(0);
        assertThat(providerInfo.getDeviceId()).isEqualTo(deviceId);
        assertThat(providerInfo.getDeviceDataTypeAdvertisements()).containsExactly(stepsAd);
    }

    @Test
    public void getDeviceDataSourceInfos_multipleDataTypes() throws Exception {
        Device device =
                new Device.Builder()
                        .setManufacturer("TestManufacturer")
                        .setModel("TestModel")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .setDisplayName("TestDisplayName")
                        .build();
        String deviceId = "TestDeviceId";
        DeviceDataTypeAdvertisement stepsAd =
                new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                        .setAvailable(true)
                        .build();
        DeviceDataTypeAdvertisement distanceAd =
                new DeviceDataTypeAdvertisement.Builder(DistanceRecord.class)
                        .setAvailable(true)
                        .build();
        Set<DeviceDataTypeAdvertisement> ads = Set.of(stepsAd, distanceAd);
        DeviceDataAdvertisement advertisement = new DeviceDataAdvertisement(device, deviceId, ads);
        HealthConnectReceiver<Void> advertiseReceiver = new HealthConnectReceiver<>();

        TestUtils.advertiseDeviceDataSources(
                Set.of(advertisement), outcomeExecutor(), advertiseReceiver);
        advertiseReceiver.verifyNoExceptionOrThrow();

        List<DeviceDataSourceInfo> result = TestUtils.getDeviceDataSourceInfos();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getDeviceDataOrigin().getPackageName())
                .isEqualTo(getCurrentDeviceId());
        DeviceDataSourceInfo info = result.get(1);
        assertThat(info.getDevice()).isEqualTo(device);
        DeviceDataProviderInfo providerInfo = info.getDeviceDataProviderInfos().get(0);
        assertThat(providerInfo.getDeviceDataTypeAdvertisements()).containsExactlyElementsIn(ads);
    }

    @Test
    public void getDeviceDataSourceInfos_userEnabled() throws Exception {
        Device device =
                new Device.Builder()
                        .setManufacturer("TestManufacturer")
                        .setModel("TestModel")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .setDisplayName("TestDisplayName")
                        .build();
        String deviceId = "TestDeviceId";
        DeviceDataTypeAdvertisement ad =
                new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                        .setAvailable(true)
                        .setUserEnabled(true)
                        .build();
        DeviceDataAdvertisement advertisement =
                new DeviceDataAdvertisement(device, deviceId, Set.of(ad));
        HealthConnectReceiver<Void> advertiseReceiver = new HealthConnectReceiver<>();

        TestUtils.advertiseDeviceDataSources(
                Set.of(advertisement), outcomeExecutor(), advertiseReceiver);
        advertiseReceiver.verifyNoExceptionOrThrow();

        List<DeviceDataSourceInfo> result = TestUtils.getDeviceDataSourceInfos();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getDeviceDataOrigin().getPackageName())
                .isEqualTo(getCurrentDeviceId());
        DeviceDataProviderInfo providerInfo = result.get(1).getDeviceDataProviderInfos().get(0);
        assertThat(
                        Iterables.getOnlyElement(providerInfo.getDeviceDataTypeAdvertisements())
                                .isUserEnabled())
                .isTrue();
    }

    @Test
    @Ignore("b/454589699 - add test coverage for multiple DDP scenarios")
    public void getDeviceDataSourceInfos_multipleDdpsSameDevice() {}

    @Test
    public void getDeviceDataSourceInfos_singleDdpMultipleDevices() throws Exception {
        Device device1 =
                new Device.Builder()
                        .setManufacturer("Man1")
                        .setModel("Mod1")
                        .setType(Device.DEVICE_TYPE_WATCH)
                        .build();
        String deviceId1 = "id1";

        Device device2 =
                new Device.Builder()
                        .setManufacturer("Man2")
                        .setModel("Mod2")
                        .setType(Device.DEVICE_TYPE_FITNESS_BAND)
                        .build();
        String deviceId2 = "id2";

        DeviceDataTypeAdvertisement ad =
                new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                        .setAvailable(true)
                        .build();

        DeviceDataAdvertisement advertisement1 =
                new DeviceDataAdvertisement(device1, deviceId1, Set.of(ad));
        DeviceDataAdvertisement advertisement2 =
                new DeviceDataAdvertisement(device2, deviceId2, Set.of(ad));

        HealthConnectReceiver<Void> advertiseReceiver = new HealthConnectReceiver<>();
        TestUtils.advertiseDeviceDataSources(
                Set.of(advertisement1, advertisement2), outcomeExecutor(), advertiseReceiver);
        advertiseReceiver.verifyNoExceptionOrThrow();

        List<DeviceDataSourceInfo> result = TestUtils.getDeviceDataSourceInfos();

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getDeviceDataOrigin().getPackageName())
                .isEqualTo(getCurrentDeviceId());

        // Verify we can find both devices
        boolean found1 = false;
        boolean found2 = false;

        for (DeviceDataSourceInfo info : result) {
            Device dev = info.getDevice();
            List<DeviceDataProviderInfo> providers = info.getDeviceDataProviderInfos();
            if (dev.equals(device1)) {
                found1 = true;
                assertThat(providers).hasSize(1);
                assertThat(providers.get(0).getDeviceId()).isEqualTo(deviceId1);
            } else if (dev.equals(device2)) {
                found2 = true;
                assertThat(providers).hasSize(1);
                assertThat(providers.get(0).getDeviceId()).isEqualTo(deviceId2);
            }
        }

        assertThat(found1).isTrue();
        assertThat(found2).isTrue();
    }

    @Test
    public void getDeviceDataSourceInfos_deviceNoLongerAdvertisedButHasData_returnsDevice()
            throws Exception {
        Device device =
                new Device.Builder()
                        .setManufacturer("TestManufacturer")
                        .setModel("TestModel")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .setDisplayName("TestDisplayName")
                        .build();
        String deviceId = "TestDeviceId";
        DeviceDataTypeAdvertisement stepsAd =
                new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                        .setAvailable(true)
                        .build();
        Set<DeviceDataTypeAdvertisement> deviceDataTypeAdvertisements = Set.of(stepsAd);
        DeviceDataAdvertisement advertisement =
                new DeviceDataAdvertisement(device, deviceId, deviceDataTypeAdvertisements);

        // 1. Advertise DEVICE_1
        HealthConnectReceiver<Void> advertiseReceiver = new HealthConnectReceiver<>();
        TestUtils.advertiseDeviceDataSources(
                Set.of(advertisement), outcomeExecutor(), advertiseReceiver);
        advertiseReceiver.verifyNoExceptionOrThrow();

        // 2. Insert data for DEVICE_1
        StepsRecord record =
                new StepsRecord.Builder(
                                new android.health.connect.datatypes.Metadata.Builder().build(),
                                Instant.now().minusSeconds(100),
                                Instant.now(),
                                100)
                        .build();
        TestUtils.insertDeviceRecords(deviceId, List.of(record));

        // 3. Verify it's in getDeviceDataSourceInfos
        List<DeviceDataSourceInfo> infosBefore = TestUtils.getDeviceDataSourceInfos();
        assertThat(infosBefore.size()).isAtLeast(2);
        assertThat(
                        infosBefore.stream()
                                .anyMatch(
                                        info ->
                                                info.getDevice()
                                                                .getManufacturer()
                                                                .equals("TestManufacturer")
                                                        && info.getDevice()
                                                                .getModel()
                                                                .equals("TestModel")))
                .isTrue();

        // 4. Stop advertising DEVICE_1
        advertiseReceiver = new HealthConnectReceiver<>();
        TestUtils.advertiseDeviceDataSources(Set.of(), outcomeExecutor(), advertiseReceiver);
        advertiseReceiver.verifyNoExceptionOrThrow();

        // 5. Verify it's STILL in getDeviceDataSourceInfos
        List<DeviceDataSourceInfo> infosAfter = TestUtils.getDeviceDataSourceInfos();
        assertThat(infosAfter.size()).isAtLeast(2);
        assertThat(
                        infosAfter.stream()
                                .anyMatch(
                                        info ->
                                                info.getDevice()
                                                                .getManufacturer()
                                                                .equals("TestManufacturer")
                                                        && info.getDevice()
                                                                .getModel()
                                                                .equals("TestModel")))
                .isTrue();
    }

    @Test
    public void getDeviceDataSourceInfos_deviceNoLongerAdvertisedAndNoData_deviceRemoved()
            throws Exception {
        Device device =
                new Device.Builder()
                        .setManufacturer("NoDataManufacturer")
                        .setModel("NoDataModel")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .setDisplayName("NoDataDisplayName")
                        .build();
        String deviceId = "NoDataDeviceId";
        DeviceDataTypeAdvertisement stepsAd =
                new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                        .setAvailable(true)
                        .build();
        DeviceDataAdvertisement advertisement =
                new DeviceDataAdvertisement(device, deviceId, Set.of(stepsAd));

        // 1. Advertise DEVICE_2
        HealthConnectReceiver<Void> advertiseReceiver = new HealthConnectReceiver<>();
        TestUtils.advertiseDeviceDataSources(
                Set.of(advertisement), outcomeExecutor(), advertiseReceiver);
        advertiseReceiver.verifyNoExceptionOrThrow();

        // 2. Verify it's in getDeviceDataSourceInfos
        List<DeviceDataSourceInfo> infosBefore = TestUtils.getDeviceDataSourceInfos();
        assertThat(
                        infosBefore.stream()
                                .anyMatch(
                                        info ->
                                                info.getDevice()
                                                                .getManufacturer()
                                                                .equals("NoDataManufacturer")
                                                        && info.getDevice()
                                                                .getModel()
                                                                .equals("NoDataModel")))
                .isTrue();

        // 3. Stop advertising DEVICE_2 (WITHOUT inserting any data)
        advertiseReceiver = new HealthConnectReceiver<>();
        TestUtils.advertiseDeviceDataSources(Set.of(), outcomeExecutor(), advertiseReceiver);
        advertiseReceiver.verifyNoExceptionOrThrow();

        // 4. Verify it's REMOVED from getDeviceDataSourceInfos
        List<DeviceDataSourceInfo> infosAfter = TestUtils.getDeviceDataSourceInfos();
        assertThat(
                        infosAfter.stream()
                                .anyMatch(
                                        info ->
                                                info.getDevice()
                                                                .getManufacturer()
                                                                .equals("NoDataManufacturer")
                                                        && info.getDevice()
                                                                .getModel()
                                                                .equals("NoDataModel")))
                .isFalse();
    }

    @Test
    public void getDeviceDataSourceInfos_ddpRemovesDeviceButHasData_returnsDevice()
            throws Exception {
        Device device =
                new Device.Builder()
                        .setManufacturer("Man1")
                        .setModel("Mod1")
                        .setType(Device.DEVICE_TYPE_WATCH)
                        .build();
        String deviceId = "id1";
        DeviceDataTypeAdvertisement ad =
                new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                        .setAvailable(true)
                        .build();

        DeviceDataAdvertisement advertisement =
                new DeviceDataAdvertisement(device, deviceId, Set.of(ad));

        // 1. Advertise the device
        HealthConnectReceiver<Void> advertiseReceiver = new HealthConnectReceiver<>();
        TestUtils.advertiseDeviceDataSources(
                Set.of(advertisement), outcomeExecutor(), advertiseReceiver);
        advertiseReceiver.verifyNoExceptionOrThrow();

        // 2. Insert data for the device so it persists as a historical source
        StepsRecord record =
                new StepsRecord.Builder(
                                new android.health.connect.datatypes.Metadata.Builder().build(),
                                java.time.Instant.now().minusSeconds(100),
                                java.time.Instant.now(),
                                100)
                        .build();
        TestUtils.insertDeviceRecords(deviceId, List.of(record));

        // Verify it exists
        List<DeviceDataSourceInfo> result = TestUtils.getDeviceDataSourceInfos();
        assertThat(result).hasSize(2);
        assertThat(result.stream().anyMatch(info -> info.getDevice().equals(device))).isTrue();

        // 3. Advertise EMPTY set (removes the advertisement)
        advertiseReceiver = new HealthConnectReceiver<>();
        TestUtils.advertiseDeviceDataSources(Set.of(), outcomeExecutor(), advertiseReceiver);
        advertiseReceiver.verifyNoExceptionOrThrow();

        // 4. Verify the device still exists (due to data), but provider info is empty.
        result = TestUtils.getDeviceDataSourceInfos();
        assertThat(result).hasSize(2);
        DeviceDataSourceInfo info =
                result.stream().filter(i -> i.getDevice().equals(device)).findFirst().orElseThrow();
        assertThat(info.getDevice()).isEqualTo(device);
        // The list of providers should be empty now
        assertThat(info.getDeviceDataProviderInfos()).isEmpty();
    }
}
