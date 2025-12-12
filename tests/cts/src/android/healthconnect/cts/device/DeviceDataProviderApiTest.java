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

import static android.healthconnect.testing.cts.TestUtils.advertiseDevice;
import static android.healthconnect.testing.cts.TestUtils.deleteDeviceRecords;
import static android.healthconnect.testing.cts.TestUtils.getCurrentDeviceId;
import static android.healthconnect.testing.cts.TestUtils.getDeviceDataSourceInfos;
import static android.healthconnect.testing.cts.TestUtils.hasUserEnabledTracking;
import static android.healthconnect.testing.cts.TestUtils.insertDeviceRecords;
import static android.healthconnect.testing.cts.TestUtils.isCanonicalSyntheticPackageName;
import static android.healthconnect.testing.cts.TestUtils.isMaskedSyntheticPackageName;
import static android.healthconnect.testing.cts.TestUtils.queryAccessLogs;
import static android.healthconnect.testing.cts.TestUtils.readDeviceRecords;
import static android.healthconnect.testing.cts.TestUtils.updateDeviceRecords;
import static android.healthconnect.testing.shared.DataFactory.getStepsRecord;

import static com.android.healthfitness.flags.Flags.FLAG_DEVELOPMENT_DATABASE_RW;
import static com.android.healthfitness.flags.Flags.FLAG_DEVICE_DATA_PROVIDERS_API;
import static com.android.healthfitness.flags.Flags.FLAG_DEVICE_DATA_PROVIDERS_DB;

import static com.google.common.truth.Truth.assertThat;

import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;

import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.RecordIdFilter;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.accesslog.AccessLog;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.StepsRecord;
import android.healthconnect.testing.cts.TestUtils;
import android.healthconnect.testing.shared.AssumptionCheckerRule;
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

@RunWith(AndroidJUnit4.class)
@RequiresFlagsEnabled({
    FLAG_DEVICE_DATA_PROVIDERS_API,
    FLAG_DEVICE_DATA_PROVIDERS_DB,
    FLAG_DEVELOPMENT_DATABASE_RW
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
}
