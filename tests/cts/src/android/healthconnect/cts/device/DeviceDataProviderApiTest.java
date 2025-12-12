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

import static android.health.connect.HealthPermissions.READ_BLOOD_GLUCOSE;
import static android.health.connect.HealthPermissions.READ_OVULATION_TEST;
import static android.healthconnect.testing.cts.TestOutcomeReceiver.outcomeExecutor;
import static android.healthconnect.testing.cts.TestUtils.advertiseDevice;
import static android.healthconnect.testing.cts.TestUtils.deleteDeviceRecords;
import static android.healthconnect.testing.cts.TestUtils.getCurrentDeviceId;
import static android.healthconnect.testing.cts.TestUtils.getDeviceDataSourceInfos;
import static android.healthconnect.testing.cts.TestUtils.getHealthConnectManager;
import static android.healthconnect.testing.cts.TestUtils.hasUserEnabledTracking;
import static android.healthconnect.testing.cts.TestUtils.insertDeviceRecords;
import static android.healthconnect.testing.cts.TestUtils.isCanonicalSyntheticPackageName;
import static android.healthconnect.testing.cts.TestUtils.isMaskedSyntheticPackageName;
import static android.healthconnect.testing.cts.TestUtils.queryAccessLogs;
import static android.healthconnect.testing.cts.TestUtils.readDeviceRecords;
import static android.healthconnect.testing.cts.TestUtils.updateDeviceRecords;
import static android.healthconnect.testing.shared.DataFactory.buildDevice;
import static android.healthconnect.testing.shared.DataFactory.getStepsRecord;

import static com.android.healthfitness.flags.Flags.FLAG_DEVICE_DATA_PROVIDERS_API;
import static com.android.healthfitness.flags.Flags.FLAG_DEVICE_DATA_PROVIDERS_DB;

import static com.google.common.truth.Truth.assertThat;

import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;

import android.app.UiAutomation;
import android.health.connect.DeviceDataProviderInfo;
import android.health.connect.DeviceDataSourceCapabilities;
import android.health.connect.DeviceDataSourceInfo;
import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.RecordIdFilter;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.accesslog.AccessLog;
import android.health.connect.datatypes.BasalBodyTemperatureRecord;
import android.health.connect.datatypes.BloodGlucoseRecord;
import android.health.connect.datatypes.HydrationRecord;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.OvulationTestRecord;
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
import androidx.test.platform.app.InstrumentationRegistry;

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
})
public class DeviceDataProviderApiTest {
    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    private final String mDeviceId = "Test id";

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
    public void withAllCrudMethods_noDeviceAccessLogged() throws InterruptedException {
        advertiseDevice(mDeviceId, StepsRecord.class);

        String insertedId =
                insertDeviceRecords(
                                mDeviceId,
                                List.of(
                                        getStepsRecord(
                                                1000,
                                                Instant.now().minus(1, HOURS),
                                                Instant.now().minus(1, MINUTES))))
                        .get(0)
                        .getMetadata()
                        .getId();

        StepsRecord updateRecord =
                getStepsRecord(50, new Metadata.Builder().setId(insertedId).build());
        updateDeviceRecords(mDeviceId, List.of(updateRecord));

        deleteDeviceRecords(
                mDeviceId,
                StepsRecord.class,
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.now().minus(1, HOURS))
                        .build());

        deleteDeviceRecords(
                mDeviceId,
                List.of(
                        RecordIdFilter.fromId(
                                StepsRecord.class, updateRecord.getMetadata().getId())));

        readDeviceRecords(
                new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                        .setDeviceId(mDeviceId)
                        .build());

        List<AccessLog> accessLogs = queryAccessLogs();
        assertThat(
                        accessLogs.stream()
                                .filter(
                                        log ->
                                                isMaskedSyntheticPackageName(log.getPackageName())
                                                        || isCanonicalSyntheticPackageName(
                                                                log.getPackageName()))
                                .findAny())
                .isEmpty();
    }

    @Test
    public void getDeviceDataSourceInfos_noDeviceAccessLogged() throws InterruptedException {
        getDeviceDataSourceInfos();

        List<AccessLog> accessLogs = queryAccessLogs();
        assertThat(
                        accessLogs.stream()
                                .filter(
                                        log ->
                                                isMaskedSyntheticPackageName(log.getPackageName())
                                                        || isCanonicalSyntheticPackageName(
                                                                log.getPackageName()))
                                .findAny())
                .isEmpty();
    }

    @Test
    public void getCurrentDeviceId_noDeviceAccessLogged() throws InterruptedException {
        getCurrentDeviceId();

        List<AccessLog> accessLogs = queryAccessLogs();
        assertThat(
                        accessLogs.stream()
                                .filter(
                                        log ->
                                                isMaskedSyntheticPackageName(log.getPackageName())
                                                        || isCanonicalSyntheticPackageName(
                                                                log.getPackageName()))
                                .findAny())
                .isEmpty();
    }

    @Test
    public void hasUserEnabledTracking_withStartCondition_returnsTrueAsDefault()
            throws InterruptedException {
        boolean actual = hasUserEnabledTracking(StepsRecord.class);

        assertThat(actual).isTrue();
    }

    @Test
    public void onStartup_advertisesCurrentDeviceCapabilities() throws InterruptedException {
        List<DeviceDataSourceInfo> response = getDeviceDataSourceInfos();
        String currentDeviceId = getCurrentDeviceId();

        assertThat(response).hasSize(1);
        DeviceDataSourceInfo sourceInfo = response.get(0);
        assertThat(sourceInfo.getDeviceDataOrigin().getPackageName()).isEqualTo(currentDeviceId);
        assertThat(sourceInfo.isCurrentDevice()).isTrue();

        assertThat(sourceInfo.getDeviceDataProviderInfos()).hasSize(1);
        DeviceDataProviderInfo deviceDataProviderInfo =
                sourceInfo.getDeviceDataProviderInfos().iterator().next();

        assertThat(deviceDataProviderInfo.getPackageName()).isEqualTo("android");
        assertThat(deviceDataProviderInfo.getDeviceId()).isEqualTo(currentDeviceId);
        assertThat(deviceDataProviderInfo.getOnboardingActivityLabel()).isEqualTo("");
        assertThat(deviceDataProviderInfo.getManagementActivityLabel()).isEqualTo("");

        assertThat(deviceDataProviderInfo.getDeviceDataTypeAdvertisements()).hasSize(1);
        DeviceDataTypeAdvertisement expectedAd =
                new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                        .setAvailable(true)
                        // TODO(b/468250208): Set to preference
                        .setUserEnabled(true)
                        // TODO(b/469717403): Decide Matchmaking behavior
                        .setVisibleByDefaultInMatchmaking(true)
                        .build();
        assertThat(deviceDataProviderInfo.getDeviceDataTypeAdvertisements().iterator().next())
                .isEqualTo(expectedAd);
    }

    @Test
    public void getDeviceDataSourceCapabilities_withSensitiveTypes_onlyReturnsPermitted()
            throws InterruptedException {
        Set<DeviceDataTypeAdvertisement> deviceDataTypeAdvertisements =
                Set.of(
                        // Non-sensitive
                        new DeviceDataTypeAdvertisement.Builder(HydrationRecord.class)
                                .setAvailable(true)
                                .build(),
                        // Sensitive
                        new DeviceDataTypeAdvertisement.Builder(BasalBodyTemperatureRecord.class)
                                .setAvailable(true)
                                .build(),
                        new DeviceDataTypeAdvertisement.Builder(OvulationTestRecord.class)
                                .setAvailable(true)
                                .build(),
                        new DeviceDataTypeAdvertisement.Builder(BloodGlucoseRecord.class)
                                .setAvailable(true)
                                .build());
        advertiseDevice("testId", buildDevice(), deviceDataTypeAdvertisements);

        // TODO(b/464299514): Instead of adopting shell identity, replace with multi app
        UiAutomation uiAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        uiAutomation.adoptShellPermissionIdentity(READ_OVULATION_TEST, READ_BLOOD_GLUCOSE);
        HealthConnectReceiver<DeviceDataSourceCapabilities> receiver =
                new HealthConnectReceiver<>();

        try {
            getHealthConnectManager().getDeviceDataSourceCapabilities(outcomeExecutor(), receiver);
            receiver.awaitUnchecked();
            DeviceDataSourceCapabilities response = receiver.getResponse();

            // Response should include
            // - steps (is always included because Health Connect can provide passive steps)
            // - hydration (non-sensitive)
            // - basal body (sensitive, but has read permissions)
            assertThat(response.getRecordTypes())
                    .containsExactly(
                            StepsRecord.class,
                            HydrationRecord.class,
                            BasalBodyTemperatureRecord.class);
        } finally {
            uiAutomation.dropShellPermissionIdentity();
        }
    }
}
