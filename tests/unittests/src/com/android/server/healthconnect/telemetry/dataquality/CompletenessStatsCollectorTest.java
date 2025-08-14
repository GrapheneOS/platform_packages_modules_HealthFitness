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

package com.android.server.healthconnect.telemetry.dataquality;

import static android.health.connect.datatypes.Metadata.RECORDING_METHOD_ACTIVELY_RECORDED;
import static android.health.connect.datatypes.Metadata.RECORDING_METHOD_AUTOMATICALLY_RECORDED;
import static android.health.connect.datatypes.Metadata.RECORDING_METHOD_MANUAL_ENTRY;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_BLOOD_PRESSURE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_NUTRITION;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_STEPS;
import static android.healthconnect.testing.unittest.RecordInternalFactory.buildNutritionRecordInternal;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;

import android.content.Context;
import android.health.connect.datatypes.BloodPressureRecord;
import android.health.connect.datatypes.Device;
import android.health.connect.datatypes.NutritionRecord;
import android.health.connect.datatypes.StepsRecord;
import android.health.connect.internal.datatypes.RecordInternal;
import android.healthconnect.testing.unittest.FitnessTestUtils;
import android.healthconnect.testing.unittest.RecordInternalFactory;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.runner.AndroidJUnit4;

import com.android.healthfitness.flags.Flags;
import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;
import com.android.server.healthconnect.permission.FirstGrantTimeManager;
import com.android.server.healthconnect.permission.HealthPermissionIntentAppsTracker;
import com.android.server.healthconnect.telemetry.dataquality.CompletenessStatsCollector.RecordingMethodStat;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@RunWith(AndroidJUnit4.class)
public class CompletenessStatsCollectorTest {
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Rule public final TemporaryFolder mEnvironmentDataDir = new TemporaryFolder();

    private static final String TEST_PACKAGE_NAME_1 = "package.name.1";
    private static final String TEST_PACKAGE_NAME_2 = "package.name.2";
    private static final Instant NOW = Instant.parse("2024-07-31T15:39:12Z");
    private FitnessTestUtils mFitnessTestUtils;
    private CompletenessStatsCollector mCompletenessStatsCollector;
    private final Clock mFakeClock = Clock.fixed(NOW, ZoneId.of("UTC"));

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectInjector injector =
                HealthConnectInjectorImpl.newBuilderForTest(context)
                        .setFirstGrantTimeManager(mock(FirstGrantTimeManager.class))
                        .setHealthPermissionIntentAppsTracker(
                                mock(HealthPermissionIntentAppsTracker.class))
                        .setClock(mFakeClock)
                        .setEnvironmentDataDirectory(mEnvironmentDataDir.getRoot())
                        .build();
        mFitnessTestUtils = new FitnessTestUtils(injector);
        mFitnessTestUtils.insertApp(TEST_PACKAGE_NAME_1);
        mFitnessTestUtils.insertApp(TEST_PACKAGE_NAME_2);
        mCompletenessStatsCollector = injector.getCompletenessStatsCollector();
    }

    @Test
    @DisableFlags(Flags.FLAG_LATENCY_METRICS_FLAG)
    public void readRecordingMethodStats_flagDisabled_returnsEmpty() {
        RecordInternal<StepsRecord> stepsRecord =
                buildStepsRecord().setRecordingMethod(RECORDING_METHOD_AUTOMATICALLY_RECORDED);
        mFitnessTestUtils.insertRecords(TEST_PACKAGE_NAME_1, stepsRecord);

        assertThat(mCompletenessStatsCollector.readRecordingMethodStats()).isEmpty();
    }

    @Test
    @DisableFlags(Flags.FLAG_LATENCY_METRICS_FLAG)
    public void readDeviceInfoStats_flagDisabled_returnsEmpty() {
        RecordInternal<StepsRecord> stepsRecord = buildStepsRecord();
        mFitnessTestUtils.insertRecords(TEST_PACKAGE_NAME_1, stepsRecord);

        assertThat(mCompletenessStatsCollector.readDeviceInfoStats()).isEmpty();
    }

    @Test
    @EnableFlags(Flags.FLAG_LATENCY_METRICS_FLAG)
    public void readRecordingMethodStats_flagEnabled_returnsStats() {
        RecordInternal<StepsRecord> stepsRecord =
                buildStepsRecord().setRecordingMethod(RECORDING_METHOD_ACTIVELY_RECORDED);
        RecordInternal<NutritionRecord> nutritionRecord =
                buildNutritionRecordInternal(
                                NOW.minusMillis(1000).toEpochMilli(), NOW.toEpochMilli())
                        .setRecordingMethod(RECORDING_METHOD_MANUAL_ENTRY);

        mFitnessTestUtils.insertRecords(TEST_PACKAGE_NAME_1, stepsRecord);
        mFitnessTestUtils.insertRecords(TEST_PACKAGE_NAME_2, nutritionRecord);

        assertThat(mCompletenessStatsCollector.readRecordingMethodStats())
                .containsExactly(
                        new RecordingMethodStat(
                                TEST_PACKAGE_NAME_1,
                                RECORD_TYPE_STEPS,
                                RECORDING_METHOD_ACTIVELY_RECORDED),
                        new RecordingMethodStat(
                                TEST_PACKAGE_NAME_2,
                                RECORD_TYPE_NUTRITION,
                                RECORDING_METHOD_MANUAL_ENTRY));
    }

    @Test
    @EnableFlags(Flags.FLAG_LATENCY_METRICS_FLAG)
    public void readRecordingMethodStats_sameTypePackageMethod_returnsOneStat() {
        RecordInternal<StepsRecord> stepsRecord1 =
                buildStepsRecord().setRecordingMethod(RECORDING_METHOD_AUTOMATICALLY_RECORDED);
        RecordInternal<StepsRecord> stepsRecord2 =
                RecordInternalFactory.buildStepsRecord(
                                NOW.minusMillis(2000).toEpochMilli(), NOW.toEpochMilli(), 456)
                        .setRecordingMethod(RECORDING_METHOD_AUTOMATICALLY_RECORDED);
        mFitnessTestUtils.insertRecords(TEST_PACKAGE_NAME_1, stepsRecord1, stepsRecord2);

        assertThat(mCompletenessStatsCollector.readRecordingMethodStats())
                .containsExactly(
                        new RecordingMethodStat(
                                TEST_PACKAGE_NAME_1,
                                RECORD_TYPE_STEPS,
                                RECORDING_METHOD_AUTOMATICALLY_RECORDED));
    }

    @Test
    @EnableFlags(Flags.FLAG_LATENCY_METRICS_FLAG)
    public void readRecordingMethodStats_recordTooOld_returnsEmpty() {
        RecordInternal<StepsRecord> stepsRecord =
                buildStepsRecord()
                        .setUuid(UUID.randomUUID())
                        .setLastModifiedTime(NOW.minus(10, ChronoUnit.DAYS).toEpochMilli())
                        .setPackageName(TEST_PACKAGE_NAME_1)
                        .setRecordingMethod(RECORDING_METHOD_ACTIVELY_RECORDED);
        mFitnessTestUtils.insertRecordsUnrestricted(stepsRecord);

        assertThat(mCompletenessStatsCollector.readRecordingMethodStats()).isEmpty();
    }

    @Test
    @EnableFlags(Flags.FLAG_LATENCY_METRICS_FLAG)
    public void readDeviceInfoStats_recordTooOld_returnsEmpty() {
        RecordInternal<StepsRecord> stepsRecord =
                buildStepsRecord()
                        .setUuid(UUID.randomUUID())
                        .setLastModifiedTime(NOW.minus(10, ChronoUnit.DAYS).toEpochMilli())
                        .setPackageName(TEST_PACKAGE_NAME_1);
        mFitnessTestUtils.insertRecordsUnrestricted(stepsRecord);

        assertThat(mCompletenessStatsCollector.readDeviceInfoStats()).isEmpty();
    }

    @Test
    @EnableFlags(Flags.FLAG_LATENCY_METRICS_FLAG)
    public void readDeviceInfoStats_variousRecords_returnsCorrectStats() {
        Device device1 =
                new Device.Builder()
                        .setManufacturer("Google")
                        .setModel("Pixel Watch")
                        .setType(Device.DEVICE_TYPE_WATCH)
                        .build();
        Device device2 =
                new Device.Builder()
                        .setManufacturer("Samsung")
                        .setModel("Galaxy S25")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .build();
        Device device3 = new Device.Builder().setType(Device.DEVICE_TYPE_PHONE).build();
        Device incompleteDevice = new Device.Builder().setManufacturer(null).setModel("").build();

        RecordInternal<StepsRecord> stepsRecord1 = buildStepsRecord(device1);
        mFitnessTestUtils.insertRecords(TEST_PACKAGE_NAME_1, stepsRecord1);

        RecordInternal<StepsRecord> stepsRecord2 = buildStepsRecord(device2);
        mFitnessTestUtils.insertRecords(TEST_PACKAGE_NAME_1, stepsRecord2);

        RecordInternal<StepsRecord> stepsRecord3 =
                buildStepsRecord(device2).setDeviceType(Device.DEVICE_TYPE_UNKNOWN);
        mFitnessTestUtils.insertRecords(TEST_PACKAGE_NAME_2, stepsRecord3);

        RecordInternal<StepsRecord> stepsRecord4 = buildStepsRecord(incompleteDevice);
        mFitnessTestUtils.insertRecords(TEST_PACKAGE_NAME_1, stepsRecord4);

        RecordInternal<BloodPressureRecord> bloodPressureRecord = buildBloodPressureRecord(device3);
        mFitnessTestUtils.insertRecords(TEST_PACKAGE_NAME_1, bloodPressureRecord);

        assertThat(mCompletenessStatsCollector.readDeviceInfoStats())
                .containsExactly(
                        // stepsRecord 1 & 2
                        new CompletenessStatsCollector.DeviceInfoStat(
                                TEST_PACKAGE_NAME_1, RECORD_TYPE_STEPS, true, true, true),
                        // stepsRecord 3
                        new CompletenessStatsCollector.DeviceInfoStat(
                                TEST_PACKAGE_NAME_2, RECORD_TYPE_STEPS, true, true, false),
                        // stepsRecord 4
                        new CompletenessStatsCollector.DeviceInfoStat(
                                TEST_PACKAGE_NAME_1, RECORD_TYPE_STEPS, false, false, false),
                        new CompletenessStatsCollector.DeviceInfoStat(
                                TEST_PACKAGE_NAME_1,
                                RECORD_TYPE_BLOOD_PRESSURE,
                                false,
                                false,
                                true));
    }

    private RecordInternal<StepsRecord> buildStepsRecord() {
        return RecordInternalFactory.buildStepsRecord(
                NOW.minusMillis(1000).toEpochMilli(), NOW.toEpochMilli(), 123);
    }

    private RecordInternal<StepsRecord> buildStepsRecord(Device device) {
        return RecordInternalFactory.buildStepsRecord(
                        NOW.minusMillis(1000).toEpochMilli(), NOW.toEpochMilli(), 123)
                .setDeviceType(device.getType())
                .setManufacturer(device.getManufacturer())
                .setModel(device.getModel());
    }

    private RecordInternal<BloodPressureRecord> buildBloodPressureRecord(Device device) {
        return RecordInternalFactory.buildBloodPressureRecord(NOW.toEpochMilli(), 80, 120)
                .setDeviceType(device.getType())
                .setManufacturer(device.getManufacturer())
                .setModel(device.getModel());
    }
}
