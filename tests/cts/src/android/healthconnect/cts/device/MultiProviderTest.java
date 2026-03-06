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

import static android.health.connect.HealthPermissions.MANAGE_HEALTH_DATA_PERMISSION;
import static android.healthconnect.testing.cts.TestUtils.getCurrentDeviceId;
import static android.healthconnect.testing.cts.TestUtils.isMaskedSyntheticPackageName;
import static android.healthconnect.testing.cts.TestUtils.readDeviceRecords;
import static android.healthconnect.testing.shared.DataFactory.getStepsRecord;

import static com.android.compatibility.common.util.SystemUtil.runShellCommand;
import static com.android.healthfitness.flags.Flags.FLAG_DEVICE_DATA_PROVIDERS_API;
import static com.android.healthfitness.flags.Flags.FLAG_DEVICE_DATA_PROVIDERS_DB;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import static java.util.Collections.emptySet;

import android.health.connect.DeviceDataProviderInfo;
import android.health.connect.DeviceDataSourceInfo;
import android.health.connect.HealthConnectException;
import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.datatypes.Device;
import android.health.connect.datatypes.DistanceRecord;
import android.health.connect.datatypes.FloorsClimbedRecord;
import android.health.connect.datatypes.HeartRateRecord;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.StepsRecord;
import android.health.connect.device.DeviceDataTypeAdvertisement;
import android.healthconnect.testing.cts.TestUtils;
import android.healthconnect.testing.shared.AssumptionCheckerRule;
import android.healthconnect.testing.shared.DeviceSupportUtils;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RunWith(AndroidJUnit4.class)
@RequiresFlagsEnabled({FLAG_DEVICE_DATA_PROVIDERS_API, FLAG_DEVICE_DATA_PROVIDERS_DB})
public class MultiProviderTest {

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Rule public final TemporaryFolder mEnvironmentDataDir = new TemporaryFolder();

    @Rule
    public AssumptionCheckerRule mSupportedHardwareRule =
            new AssumptionCheckerRule(
                    DeviceSupportUtils::isHealthConnectFullySupported,
                    "Tests should run on supported hardware only.");

    // On a multi-user setup, the terminal will be executed by the secondary user. As HC API calls
    // enforce that calls are coming from the foreground user (user 0), the tests fail.
    // See b/486393299 for more details.
    @Rule
    public AssumptionCheckerRule mSystemUserRule =
            new AssumptionCheckerRule(
                    DeviceSupportUtils::isSystemUser, "Tests should run on system user only.");

    private static final String TEST_APP_NAME = "android.healthconnect.cts";
    private static final String SHELL_PACKAGE_NAME = "com.android.shell";
    private static final String DEVICE_DATA_PROVIDER_PACKAGE = "android";
    private static final String SHELL_DEVICE_ID = "ShellDeviceId";
    private static final String ALTERNATIVE_DEVICE_ID = "OtherDeviceId";

    @Before
    public void before() throws InterruptedException {
        TestUtils.deleteAllDataFromHealthConnect();
    }

    @After
    public void after() throws InterruptedException {
        TestUtils.deleteAllDataFromHealthConnect();
    }

    @Test
    public void getCurrentDeviceId_differentPerCaller() throws Exception {
        String testAppDeviceId = TestUtils.getCurrentDeviceId();
        String shellDeviceId =
                runShellCommand(
                                InstrumentationRegistry.getInstrumentation(),
                                DEVICE_DATA_PROVIDER_COMMAND.GET_CURRENT_DEVICE_ID
                                        .getShellCommand())
                        .trim();

        assertThat(isMaskedSyntheticPackageName(shellDeviceId)).isTrue();
        assertThat(testAppDeviceId).isNotEqualTo(shellDeviceId);
    }

    @Test
    public void getCurrentDeviceId_differentProviders_canAdvertise() throws Exception {
        TestUtils.advertiseDevice(
                getCurrentDeviceId(), buildTestAppDevice(), FloorsClimbedRecord.class);
        runShellCommand(
                InstrumentationRegistry.getInstrumentation(),
                DEVICE_DATA_PROVIDER_COMMAND.ADVERTISE_CURRENT_DEVICE.getShellCommand());

        List<DeviceDataSourceInfo> infos = TestUtils.getDeviceDataSourceInfos();

        // One device (Current Device)
        assertThat(infos).hasSize(1);
        // Three sources: Android (System), TestApp, Shell
        assertThat(infos.get(0).getDeviceDataProviderInfos()).hasSize(3);

        assertThat(
                        infos.get(0).getDeviceDataProviderInfos().stream()
                                .map(DeviceDataProviderInfo::getPackageName))
                .containsExactly(DEVICE_DATA_PROVIDER_PACKAGE, TEST_APP_NAME, SHELL_PACKAGE_NAME);

        assertThat(
                        infos.get(0).getDeviceDataProviderInfos().stream()
                                .map(
                                        info ->
                                                info.getDeviceDataTypeAdvertisements().stream()
                                                        .map(
                                                                DeviceDataTypeAdvertisement
                                                                        ::getDataType)
                                                        .collect(Collectors.toSet())))
                .containsExactly(
                        Set.of(StepsRecord.class),
                        Set.of(FloorsClimbedRecord.class),
                        Set.of(StepsRecord.class));
    }

    @Test
    public void getCurrentDeviceId_differentProviders_canInsert() throws Exception {
        String currentDeviceId = getCurrentDeviceId();
        TestUtils.advertiseDevice(currentDeviceId, buildTestAppDevice(), StepsRecord.class);
        runShellCommand(
                InstrumentationRegistry.getInstrumentation(),
                DEVICE_DATA_PROVIDER_COMMAND.ADVERTISE_CURRENT_DEVICE.getShellCommand());

        // Test App inserts
        insertStepsRecordFromApp(currentDeviceId);

        // Shell inserts
        runShellCommand(
                InstrumentationRegistry.getInstrumentation(),
                DEVICE_DATA_PROVIDER_COMMAND.INSERT_CURRENT_DEVICE_RECORDS.getShellCommand());

        // Verify records exist
        List<StepsRecord> records =
                TestUtils.readRecordsWithManagePermission(
                        new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class).build());

        assertThat(records).hasSize(2);
    }

    @Test
    public void advertiseDeviceDataSources_differentProviders_sameDevice_canAdvertise()
            throws Exception {
        TestUtils.advertiseDevice(SHELL_DEVICE_ID, buildTestAppDevice(), StepsRecord.class);
        runShellCommand(
                InstrumentationRegistry.getInstrumentation(),
                DEVICE_DATA_PROVIDER_COMMAND.ADVERTISE_DEVICE.getShellCommand());

        List<DeviceDataSourceInfo> infos = TestUtils.getDeviceDataSourceInfos();

        // Two devices (Current Device + Shell Device)
        assertThat(infos).hasSize(2);

        DeviceDataSourceInfo shellDevice =
                infos.stream().filter(info -> !info.isCurrentDevice()).findFirst().orElseThrow();

        assertThat(shellDevice.getDeviceDataProviderInfos()).hasSize(2);
        assertThat(
                        shellDevice.getDeviceDataProviderInfos().stream()
                                .map(DeviceDataProviderInfo::getPackageName))
                .containsExactly(TEST_APP_NAME, SHELL_PACKAGE_NAME);
    }

    @Test
    public void advertiseDeviceDataSources_differentProviders_oneOmitsType_returnsTwoProviders()
            throws Exception {
        // Test App advertises Floors + heart rate
        // Shell advertises Steps + Distance (STANDARD)
        TestUtils.advertiseDevice(
                SHELL_DEVICE_ID,
                buildTestAppDevice(),
                Set.of(
                        new DeviceDataTypeAdvertisement.Builder(FloorsClimbedRecord.class)
                                .setAvailable(true)
                                .build(),
                        new DeviceDataTypeAdvertisement.Builder(HeartRateRecord.class)
                                .setAvailable(true)
                                .build()));
        runShellCommand(
                InstrumentationRegistry.getInstrumentation(),
                DEVICE_DATA_PROVIDER_COMMAND.ADVERTISE_DEVICE.getShellCommand());

        // Test App re-advertises Floors only
        TestUtils.advertiseDevice(SHELL_DEVICE_ID, buildTestAppDevice(), FloorsClimbedRecord.class);

        List<DeviceDataSourceInfo> currentInfos = TestUtils.getDeviceDataSourceInfos();

        // Two devices (Current Device + Shell Device)
        assertThat(currentInfos).hasSize(2);

        DeviceDataSourceInfo shellDevice =
                currentInfos.stream()
                        .filter(info -> !info.isCurrentDevice())
                        .findFirst()
                        .orElseThrow();

        assertThat(shellDevice.getDeviceDataProviderInfos()).hasSize(2);
        assertThat(
                        shellDevice.getDeviceDataProviderInfos().stream()
                                .map(
                                        providerInfo ->
                                                providerInfo
                                                        .getDeviceDataTypeAdvertisements()
                                                        .stream()
                                                        .map(
                                                                DeviceDataTypeAdvertisement
                                                                        ::getDataType)
                                                        .collect(Collectors.toSet())))
                .containsExactly(
                        Set.of(FloorsClimbedRecord.class),
                        Set.of(StepsRecord.class, DistanceRecord.class));
    }

    @Test
    public void advertiseDeviceDataSources_differentProviders_oneOmitsAdvertisement_omitsProvider()
            throws Exception {
        TestUtils.advertiseDevice(SHELL_DEVICE_ID, buildTestAppDevice(), StepsRecord.class);
        runShellCommand(
                InstrumentationRegistry.getInstrumentation(),
                DEVICE_DATA_PROVIDER_COMMAND.ADVERTISE_DEVICE.getShellCommand());

        // Test App advertises empty
        TestUtils.advertiseDevice(SHELL_DEVICE_ID, buildTestAppDevice(), emptySet());

        List<DeviceDataSourceInfo> currentInfos = TestUtils.getDeviceDataSourceInfos();

        // Two devices (Current Device + Shell Device)
        assertThat(currentInfos).hasSize(2);

        DeviceDataSourceInfo shellDevice =
                currentInfos.stream()
                        .filter(info -> !info.isCurrentDevice())
                        .findFirst()
                        .orElseThrow();

        assertThat(shellDevice.getDeviceDataProviderInfos()).hasSize(1);
        assertThat(shellDevice.getDeviceDataProviderInfos().get(0).getPackageName())
                .isEqualTo(SHELL_PACKAGE_NAME);
    }

    @Test
    public void advertiseDeviceDataSources_sameIdButDifferentDeviceType_invalid() throws Exception {
        runShellCommand(
                InstrumentationRegistry.getInstrumentation(),
                DEVICE_DATA_PROVIDER_COMMAND.ADVERTISE_DEVICE.getShellCommand());

        // Test app tries to advertise same ID with SCALE type
        assertThrows(
                HealthConnectException.class,
                () ->
                        TestUtils.advertiseDevice(
                                SHELL_DEVICE_ID,
                                new Device.Builder()
                                        .setManufacturer("Some Manufacturer")
                                        .setModel("Some Model")
                                        .setDisplayName("My Device")
                                        .setType(Device.DEVICE_TYPE_SCALE)
                                        .build(),
                                StepsRecord.class));
    }

    @Test
    public void crud_differentProviders_sameDevice_insert_addsBothRecords() throws Exception {
        TestUtils.advertiseDevice(SHELL_DEVICE_ID, buildTestAppDevice(), StepsRecord.class);
        runShellCommand(
                InstrumentationRegistry.getInstrumentation(),
                DEVICE_DATA_PROVIDER_COMMAND.ADVERTISE_DEVICE.getShellCommand());

        insertStepsRecordFromApp(SHELL_DEVICE_ID);
        runShellCommand(
                InstrumentationRegistry.getInstrumentation(),
                DEVICE_DATA_PROVIDER_COMMAND.INSERT_RECORDS.getShellCommand());

        List<StepsRecord> records =
                TestUtils.readRecordsWithManagePermission(
                        new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class).build());

        // Should have 2 records (one from each)
        assertThat(records).hasSize(2);
        assertThat(records.stream().map(StepsRecord::getCount)).contains(100L);
        assertThat(records.stream().map(StepsRecord::getCount)).contains(500L);
    }

    @Test
    public void crud_differentProviders_sameDevice_insert_delete_deleteIsIsolated()
            throws Exception {
        TestUtils.advertiseDevice(SHELL_DEVICE_ID, buildTestAppDevice(), StepsRecord.class);
        runShellCommand(
                InstrumentationRegistry.getInstrumentation(),
                DEVICE_DATA_PROVIDER_COMMAND.ADVERTISE_DEVICE.getShellCommand());

        insertStepsRecordFromApp(SHELL_DEVICE_ID);
        List<StepsRecord> preDeleteRecords =
                TestUtils.readDeviceRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                                .setDeviceId(SHELL_DEVICE_ID)
                                .build());
        assertThat(preDeleteRecords).hasSize(1);

        // Shell requests delete
        runShellCommand(
                InstrumentationRegistry.getInstrumentation(),
                DEVICE_DATA_PROVIDER_COMMAND.DELETE_RECORDS.getShellCommand());

        // Verify Test App data still exists
        List<StepsRecord> postDeleteRecords =
                TestUtils.readDeviceRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                                .setDeviceId(SHELL_DEVICE_ID)
                                .build());
        assertThat(postDeleteRecords).hasSize(1);
        assertThat(postDeleteRecords.get(0).getCount()).isEqualTo(100);
    }

    @Test
    public void crud_differentProviders_sameDevice_testAppInserts_shellReads_dataIsolated()
            throws Exception {
        TestUtils.advertiseDevice(SHELL_DEVICE_ID, buildTestAppDevice(), StepsRecord.class);
        runShellCommand(
                InstrumentationRegistry.getInstrumentation(),
                DEVICE_DATA_PROVIDER_COMMAND.ADVERTISE_DEVICE.getShellCommand());

        insertStepsRecordFromApp(SHELL_DEVICE_ID);

        // Shell reads
        String count =
                runShellCommand(
                                InstrumentationRegistry.getInstrumentation(),
                                DEVICE_DATA_PROVIDER_COMMAND.READ_RECORDS.getShellCommand())
                        .trim();

        // Shell should see 0 records
        assertThat(Integer.parseInt(count)).isEqualTo(0);
    }

    @Test
    public void crud_differentProviders_sameDevice_bothInsert_testAppReads_dataIsolated()
            throws Exception {
        TestUtils.advertiseDevice(SHELL_DEVICE_ID, buildTestAppDevice(), StepsRecord.class);
        runShellCommand(
                InstrumentationRegistry.getInstrumentation(),
                DEVICE_DATA_PROVIDER_COMMAND.ADVERTISE_DEVICE.getShellCommand());

        insertStepsRecordFromApp(SHELL_DEVICE_ID);
        runShellCommand(
                InstrumentationRegistry.getInstrumentation(),
                DEVICE_DATA_PROVIDER_COMMAND.INSERT_RECORDS.getShellCommand());

        // Test app reads
        List<StepsRecord> records =
                readDeviceRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                                .setDeviceId(SHELL_DEVICE_ID)
                                .build());

        // Test app should see 1 record
        assertThat(records).hasSize(1);
        assertThat(records.get(0).getCount()).isEqualTo(100);
    }

    @Test
    public void crud_differentProviders_sameDevice_insert_update_dataIsolated() throws Exception {
        TestUtils.advertiseDevice(SHELL_DEVICE_ID, buildTestAppDevice(), StepsRecord.class);
        runShellCommand(
                InstrumentationRegistry.getInstrumentation(),
                DEVICE_DATA_PROVIDER_COMMAND.ADVERTISE_DEVICE.getShellCommand());

        // Shell inserts 500 steps
        runShellCommand(
                InstrumentationRegistry.getInstrumentation(),
                DEVICE_DATA_PROVIDER_COMMAND.INSERT_RECORDS.getShellCommand());
        List<StepsRecord> preUpdateRecords =
                TestUtils.readRecordsWithManagePermission(
                        new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class).build());
        assertThat(preUpdateRecords).hasSize(1);
        assertThat(preUpdateRecords.get(0).getCount()).isEqualTo(500);

        StepsRecord updateRecord =
                getStepsRecord(
                        10,
                        new Metadata.Builder()
                                .setId(preUpdateRecords.get(0).getMetadata().getId())
                                .build());

        // Test app attempts to update
        assertThrows(
                HealthConnectException.class,
                () -> TestUtils.updateDeviceRecords(SHELL_DEVICE_ID, List.of(updateRecord)));

        // Verify shell record is unchanged
        List<StepsRecord> postUpdateRecords =
                TestUtils.readRecordsWithManagePermission(
                        new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class).build());
        assertThat(postUpdateRecords).hasSize(1);
        assertThat(postUpdateRecords.get(0).getCount()).isEqualTo(500);
    }

    @Test
    public void crud_differentProviders_twoDevices_delete_deleteIsIsolated() throws Exception {
        TestUtils.advertiseDevice(ALTERNATIVE_DEVICE_ID, buildTestAppDevice(), StepsRecord.class);
        runShellCommand(
                InstrumentationRegistry.getInstrumentation(),
                DEVICE_DATA_PROVIDER_COMMAND.ADVERTISE_DEVICE.getShellCommand());

        runShellCommand(
                InstrumentationRegistry.getInstrumentation(),
                DEVICE_DATA_PROVIDER_COMMAND.INSERT_RECORDS.getShellCommand());
        List<StepsRecord> preDeleteRecords =
                TestUtils.readRecordsWithManagePermission(
                        new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class).build());
        assertThat(preDeleteRecords).hasSize(1);
        assertThat(preDeleteRecords.get(0).getCount()).isEqualTo(500);

        // Test App attempts to delete shell device data
        assertThrows(
                HealthConnectException.class,
                () ->
                        TestUtils.deleteDeviceRecords(
                                SHELL_DEVICE_ID,
                                StepsRecord.class,
                                new TimeInstantRangeFilter.Builder()
                                        .setStartTime(Instant.EPOCH)
                                        .setEndTime(Instant.now().plus(1, ChronoUnit.DAYS))
                                        .build()));

        // Verify Shell Data still exists
        List<StepsRecord> postDeleteRecords =
                TestUtils.readRecordsWithManagePermission(
                        new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class).build());

        // Should have 1 record left (from Shell)
        assertThat(postDeleteRecords).hasSize(1);
        assertThat(postDeleteRecords.get(0).getCount()).isEqualTo(500);
    }

    @Test
    public void getDeviceDataSources_differentPermissions_returnsDifferentResults()
            throws Exception {
        TestUtils.advertiseDevice(ALTERNATIVE_DEVICE_ID, buildTestAppDevice(), StepsRecord.class);
        runShellCommand(
                InstrumentationRegistry.getInstrumentation(),
                DEVICE_DATA_PROVIDER_COMMAND.ADVERTISE_DEVICE.getShellCommand());

        TestUtils.verifyGetDeviceDataSourcesWithPermission(
                MANAGE_HEALTH_DATA_PERMISSION, dataSources -> assertThat(dataSources).hasSize(3));

        String count =
                runShellCommand(
                                InstrumentationRegistry.getInstrumentation(),
                                DEVICE_DATA_PROVIDER_COMMAND.GET_DEVICE_DATA_SOURCES
                                        .getShellCommand())
                        .trim();

        // shell package has not been granted any READ permissions
        assertThat(Integer.parseInt(count)).isEqualTo(0);
    }

    private void insertStepsRecordFromApp(String deviceId) throws Exception {
        TestUtils.insertDeviceRecords(deviceId, List.of(getStepsRecord(100)));
    }

    private static Device buildTestAppDevice() {
        return new Device.Builder()
                .setManufacturer("google")
                .setModel("Pixel4a")
                .setDisplayName("My Device")
                .setType(Device.DEVICE_TYPE_PHONE)
                .build();
    }

    private enum DEVICE_DATA_PROVIDER_COMMAND {
        GET_CURRENT_DEVICE_ID("cmd healthconnect get-current-device-id"),
        ADVERTISE_CURRENT_DEVICE("cmd healthconnect advertise-current-device"),
        INSERT_CURRENT_DEVICE_RECORDS("cmd healthconnect insert-current-device-records"),
        ADVERTISE_DEVICE("cmd healthconnect advertise-device"),
        INSERT_RECORDS("cmd healthconnect insert-records"),
        READ_RECORDS("cmd healthconnect read-records"),
        DELETE_RECORDS("cmd healthconnect delete-records"),
        GET_DEVICE_DATA_SOURCES("cmd healthconnect get-device-data-sources");

        private final String mShellCommand;

        DEVICE_DATA_PROVIDER_COMMAND(String shellCommand) {
            this.mShellCommand = shellCommand;
        }

        public String getShellCommand() {
            return this.mShellCommand;
        }
    }
}
