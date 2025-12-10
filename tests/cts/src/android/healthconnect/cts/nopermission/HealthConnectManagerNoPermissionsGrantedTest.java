/*
 * Copyright (C) 2022 The Android Open Source Project
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

package android.healthconnect.cts.nopermission;

import static android.health.connect.HealthPermissions.READ_DISTANCE;
import static android.health.connect.HealthPermissions.READ_EXERCISE;
import static android.health.connect.HealthPermissions.READ_HEART_RATE;
import static android.health.connect.HealthPermissions.READ_SLEEP;
import static android.health.connect.HealthPermissions.READ_STEPS;
import static android.health.connect.HealthPermissions.READ_TOTAL_CALORIES_BURNED;
import static android.health.connect.datatypes.DistanceRecord.DISTANCE_TOTAL;
import static android.health.connect.datatypes.ExerciseSessionRecord.EXERCISE_DURATION_TOTAL;
import static android.health.connect.datatypes.HeartRateRecord.BPM_MAX;
import static android.health.connect.datatypes.SleepSessionRecord.SLEEP_DURATION_TOTAL;
import static android.health.connect.datatypes.StepsRecord.STEPS_COUNT_TOTAL;
import static android.health.connect.datatypes.TotalCaloriesBurnedRecord.ENERGY_TOTAL;
import static android.healthconnect.testing.cts.TestOutcomeReceiver.outcomeExecutor;
import static android.healthconnect.testing.cts.TestUtils.deleteRecords;
import static android.healthconnect.testing.cts.TestUtils.getAggregateResponse;
import static android.healthconnect.testing.cts.TestUtils.getAggregateResponseGroupByDuration;
import static android.healthconnect.testing.cts.TestUtils.getAggregateResponseGroupByPeriod;
import static android.healthconnect.testing.cts.TestUtils.getChangeLogToken;
import static android.healthconnect.testing.cts.TestUtils.getCurrentDeviceDataSource;
import static android.healthconnect.testing.cts.TestUtils.insertRecords;
import static android.healthconnect.testing.cts.TestUtils.readRecords;
import static android.healthconnect.testing.cts.TestUtils.updateRecords;
import static android.healthconnect.testing.cts.TestUtils.verifyDeleteRecords;
import static android.healthconnect.testing.shared.DataFactory.buildExerciseSession;
import static android.healthconnect.testing.shared.DataFactory.buildSleepSession;
import static android.healthconnect.testing.shared.DataFactory.getDistanceRecord;
import static android.healthconnect.testing.shared.DataFactory.getDistanceRecordWithNonEmptyId;
import static android.healthconnect.testing.shared.DataFactory.getHeartRateRecord;
import static android.healthconnect.testing.shared.DataFactory.getStepsRecord;
import static android.healthconnect.testing.shared.DataFactory.getTotalCaloriesBurnedRecord;
import static android.healthconnect.testing.shared.DataFactory.getTotalCaloriesBurnedRecordWithEmptyMetadata;
import static android.healthconnect.testing.shared.recordfactory.RecordFactory.newEmptyMetadataWithClientId;

import static com.android.healthfitness.flags.Flags.FLAG_DEVICE_DATA_PROVIDERS_API;
import static com.android.healthfitness.flags.Flags.FLAG_DEVICE_DATA_PROVIDERS_DB;

import static com.google.common.truth.Truth.assertThat;

import static java.time.temporal.ChronoUnit.DAYS;

import android.health.connect.AggregateRecordsRequest;
import android.health.connect.DeviceDataSource;
import android.health.connect.HealthConnectException;
import android.health.connect.LocalTimeRangeFilter;
import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.ReadRecordsRequestUsingIds;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.changelog.ChangeLogTokenRequest;
import android.health.connect.changelog.ChangeLogsRequest;
import android.health.connect.datatypes.AggregationType;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.Device;
import android.health.connect.datatypes.DistanceRecord;
import android.health.connect.datatypes.ExerciseSessionRecord;
import android.health.connect.datatypes.HeartRateRecord;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.SleepSessionRecord;
import android.health.connect.datatypes.StepsRecord;
import android.health.connect.datatypes.TotalCaloriesBurnedRecord;
import android.health.connect.device.DeviceDataAdvertisement;
import android.health.connect.device.DeviceDataTypeAdvertisement;
import android.healthconnect.testing.cts.HealthConnectReceiver;
import android.healthconnect.testing.cts.TestUtils;
import android.healthconnect.testing.cts.testapphelpers.TestAppProxy;
import android.healthconnect.testing.cts.testapphelpers.TestAppRule;
import android.healthconnect.testing.shared.AssumptionCheckerRule;
import android.healthconnect.testing.shared.DataFactory;
import android.healthconnect.testing.shared.DeviceSupportUtils;
import android.healthconnect.testing.shared.recordfactory.MindfulnessSessionRecordFactory;
import android.platform.test.annotations.AppModeFull;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.util.Pair;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/** These test run under an environment which has no HC permissions */
@AppModeFull(reason = "HealthConnectManager is not accessible to instant apps")
@RunWith(AndroidJUnit4.class)
public class HealthConnectManagerNoPermissionsGrantedTest {
    private static final MindfulnessSessionRecordFactory MINDFULNESS_SESSION_RECORD_FACTORY =
            new MindfulnessSessionRecordFactory();

    @Rule(order = 0)
    public AssumptionCheckerRule mSupportedHardwareRule =
            new AssumptionCheckerRule(
                    DeviceSupportUtils::isHealthConnectFullySupported,
                    "Tests should run on supported hardware only.");

    @Rule(order = 1)
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Rule(order = 2)
    public final TestAppRule mTestAppRule =
            new TestAppRule.Builder("android.healthconnect.cts.testapp.readWritePerms.A").build();

    private final TestAppProxy mTestApp = mTestAppRule.getProxy();
    private final Instant mNow = DataFactory.now();

    @Test
    public void testInsert_noPermissions_expectError() throws InterruptedException {
        for (Record testRecord : getTestRecords()) {
            try {
                insertRecords(Collections.singletonList(testRecord));
                Assert.fail("Insert must be not allowed without right HC permission");
            } catch (HealthConnectException healthConnectException) {
                assertThat(healthConnectException.getErrorCode())
                        .isEqualTo(HealthConnectException.ERROR_SECURITY);
            }
        }
    }

    @Test
    public void testUpdate_noPermissions_expectError() throws InterruptedException {
        for (Record testRecord : getTestRecords()) {
            try {
                updateRecords(Collections.singletonList(testRecord));
                Assert.fail("Update must be not allowed without right HC permission");
            } catch (HealthConnectException healthConnectException) {
                assertThat(healthConnectException.getErrorCode())
                        .isEqualTo(HealthConnectException.ERROR_SECURITY);
            }
        }
    }

    @Test
    public void testDeleteUsingId_noPermissions_expectError() throws InterruptedException {
        for (Record testRecord : getTestRecords()) {
            try {
                deleteRecords(Collections.singletonList(testRecord));
                Assert.fail("Delete using ids must be not allowed without right HC permission");
            } catch (HealthConnectException healthConnectException) {
                assertThat(healthConnectException.getErrorCode())
                        .isEqualTo(HealthConnectException.ERROR_SECURITY);
            }
        }
    }

    @Test
    public void testDeleteUsingFilter_noPermissions_expectError() throws InterruptedException {
        for (Record testRecord : getTestRecords()) {
            try {
                verifyDeleteRecords(
                        testRecord.getClass(),
                        new TimeInstantRangeFilter.Builder()
                                .setStartTime(Instant.now())
                                .setEndTime(Instant.now().plusMillis(1000))
                                .build());
                Assert.fail("Delete using filters must be not allowed without right HC permission");
            } catch (HealthConnectException healthConnectException) {
                assertThat(healthConnectException.getErrorCode())
                        .isEqualTo(HealthConnectException.ERROR_SECURITY);
            }
        }
    }

    @Test
    public void testChangeLogsToken_noPermissions_expectError() throws InterruptedException {
        for (Record testRecord : getTestRecords()) {
            try {
                getChangeLogToken(
                        new ChangeLogTokenRequest.Builder()
                                .addRecordType(testRecord.getClass())
                                .build());
                Assert.fail(
                        "Getting change log token must be not allowed without right HC permission");
            } catch (HealthConnectException healthConnectException) {
                assertThat(healthConnectException.getErrorCode())
                        .isEqualTo(HealthConnectException.ERROR_SECURITY);
            }
        }
    }

    @Test
    public void testGetChangeLogs_noPermissions_expectError() throws Exception {
        mTestAppRule.revokeAllHealthPermissions();
        List<Pair<String, Class<? extends Record>>> permissionAndRecordClassPairs =
                List.of(
                        new Pair<>(READ_STEPS, StepsRecord.class),
                        new Pair<>(READ_DISTANCE, DistanceRecord.class),
                        new Pair<>(READ_HEART_RATE, HeartRateRecord.class),
                        new Pair<>(READ_SLEEP, SleepSessionRecord.class),
                        new Pair<>(READ_EXERCISE, ExerciseSessionRecord.class),
                        new Pair<>(READ_TOTAL_CALORIES_BURNED, TotalCaloriesBurnedRecord.class));

        for (var permissionAndRecordClass : permissionAndRecordClassPairs) {
            String permission = permissionAndRecordClass.first;
            Class<? extends Record> recordClass = permissionAndRecordClass.second;
            mTestAppRule.grantHealthPermission(permission);
            String token =
                    mTestApp.getChangeLogToken(
                            new ChangeLogTokenRequest.Builder().addRecordType(recordClass).build());
            mTestAppRule.revokeAllHealthPermissions();

            try {
                mTestApp.getChangeLogs(new ChangeLogsRequest.Builder(token).build());

                Assert.fail(
                        String.format(
                                "Getting change logs for %s must be not allowed with %s permission",
                                recordClass.getSimpleName(), permission));
            } catch (HealthConnectException healthConnectException) {
                assertThat(healthConnectException.getErrorCode())
                        .isEqualTo(HealthConnectException.ERROR_SECURITY);
            }
        }
    }

    @Test
    public void testReadByFilters_noPermissions_expectError() throws InterruptedException {
        for (Record testRecord : getTestRecords()) {
            try {
                readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(testRecord.getClass())
                                .build());
                Assert.fail(
                        "Read records by filters must be not allowed without right HC permission");
            } catch (HealthConnectException healthConnectException) {
                assertThat(healthConnectException.getErrorCode())
                        .isEqualTo(HealthConnectException.ERROR_SECURITY);
            }
        }
    }

    @Test
    public void testReadByRecordIds_noPermissions_expectError() throws InterruptedException {
        for (Record testRecord : getTestRecords()) {
            try {
                readRecords(
                        new ReadRecordsRequestUsingIds.Builder<>(testRecord.getClass())
                                .addId("id")
                                .build());
                Assert.fail(
                        "Read records by record ids must be not allowed without right HC "
                                + "permission");
            } catch (HealthConnectException healthConnectException) {
                assertThat(healthConnectException.getErrorCode())
                        .isEqualTo(HealthConnectException.ERROR_SECURITY);
            }
        }
    }

    @Test
    public void testReadByClientIds_noPermissions_expectError() throws InterruptedException {
        for (Record testRecord : getTestRecords()) {
            try {
                readRecords(
                        new ReadRecordsRequestUsingIds.Builder<>(testRecord.getClass())
                                .addClientRecordId("client_id")
                                .build());
                Assert.fail(
                        "Read records by client ids must be not allowed without right HC "
                                + "permission");
            } catch (HealthConnectException healthConnectException) {
                assertThat(healthConnectException.getErrorCode())
                        .isEqualTo(HealthConnectException.ERROR_SECURITY);
            }
        }
    }

    @Test
    public void testAggregate_noPermissions_expectError() throws InterruptedException {
        List<Pair<Record, AggregationType<?>>> recordAndAggregationTypePairs =
                List.of(
                        new Pair<>(getHeartRateRecord(), BPM_MAX),
                        new Pair<>(getStepsRecord(), STEPS_COUNT_TOTAL),
                        new Pair<>(getDistanceRecord(), DISTANCE_TOTAL),
                        new Pair<>(getTotalCaloriesBurnedRecordWithEmptyMetadata(), ENERGY_TOTAL),
                        new Pair<>(buildSleepSession(), SLEEP_DURATION_TOTAL),
                        new Pair<>(buildExerciseSession(), EXERCISE_DURATION_TOTAL));
        for (var recordAndAggregationType : recordAndAggregationTypePairs) {
            try {
                List<Record> records = List.of(recordAndAggregationType.first);
                AggregationType<?> aggregationType = recordAndAggregationType.second;
                TimeInstantRangeFilter timeInstantRangeFilter =
                        new TimeInstantRangeFilter.Builder()
                                .setStartTime(Instant.ofEpochMilli(0))
                                .setEndTime(mNow.plus(1000, DAYS))
                                .build();
                getAggregateResponse(
                        new AggregateRecordsRequest.Builder<>(timeInstantRangeFilter)
                                .addAggregationType((AggregationType<Object>) aggregationType)
                                .addDataOriginsFilter(
                                        new DataOrigin.Builder().setPackageName("abc").build())
                                .build(),
                        records);
                Assert.fail("Get Aggregations must be not allowed without right HC permission");
            } catch (HealthConnectException healthConnectException) {
                assertThat(healthConnectException.getErrorCode())
                        .isEqualTo(HealthConnectException.ERROR_SECURITY);
            }
        }
    }

    @Test
    public void testAggregateGroupByDuration_noPermissions_expectError()
            throws InterruptedException {
        List<AggregationType<?>> aggregationTypes =
                List.of(
                        BPM_MAX,
                        STEPS_COUNT_TOTAL,
                        DISTANCE_TOTAL,
                        ENERGY_TOTAL,
                        SLEEP_DURATION_TOTAL,
                        EXERCISE_DURATION_TOTAL);
        for (var aggregationType : aggregationTypes) {
            try {
                TimeInstantRangeFilter timeInstantRangeFilter =
                        new TimeInstantRangeFilter.Builder()
                                .setStartTime(mNow.minusMillis(500))
                                .setEndTime(mNow.plusMillis(2500))
                                .build();
                getAggregateResponseGroupByDuration(
                        new AggregateRecordsRequest.Builder<>(timeInstantRangeFilter)
                                .addAggregationType((AggregationType<Object>) aggregationType)
                                .addDataOriginsFilter(
                                        new DataOrigin.Builder().setPackageName("abc").build())
                                .build(),
                        Duration.ofSeconds(1));
                Assert.fail(
                        "Aggregations group by duration must be not allowed without right HC"
                                + " permission");
            } catch (HealthConnectException healthConnectException) {
                assertThat(healthConnectException.getErrorCode())
                        .isEqualTo(HealthConnectException.ERROR_SECURITY);
            }
        }
    }

    @Test
    public void testAggregateGroupByPeriod_noPermissions_expectError() throws InterruptedException {
        List<AggregationType<?>> aggregationTypes =
                List.of(
                        BPM_MAX,
                        STEPS_COUNT_TOTAL,
                        DISTANCE_TOTAL,
                        ENERGY_TOTAL,
                        SLEEP_DURATION_TOTAL,
                        EXERCISE_DURATION_TOTAL);
        for (var aggregationType : aggregationTypes) {
            try {
                Instant start = mNow.minus(3, DAYS);
                Instant end = start.plus(3, DAYS);
                LocalTimeRangeFilter localTimeRangeFilter =
                        new LocalTimeRangeFilter.Builder()
                                .setStartTime(LocalDateTime.ofInstant(start, ZoneOffset.UTC))
                                .setEndTime(LocalDateTime.ofInstant(end, ZoneOffset.UTC))
                                .build();
                getAggregateResponseGroupByPeriod(
                        new AggregateRecordsRequest.Builder<>(localTimeRangeFilter)
                                .addAggregationType((AggregationType<Object>) aggregationType)
                                .build(),
                        Period.ofDays(1));
                Assert.fail(
                        "Aggregation group by period must be not allowed without right HC"
                                + " permission");
            } catch (HealthConnectException healthConnectException) {
                assertThat(healthConnectException.getErrorCode())
                        .isEqualTo(HealthConnectException.ERROR_SECURITY);
            }
        }
    }

    @Test
    @RequiresFlagsEnabled({
        FLAG_DEVICE_DATA_PROVIDERS_API,
        FLAG_DEVICE_DATA_PROVIDERS_DB,
    })
    public void getCurrentDeviceDataSource_withNoPermission_throwSecurityException()
            throws InterruptedException {
        String currentDeviceId = TestUtils.getCurrentDeviceId();
        Set<DeviceDataTypeAdvertisement> ads =
                Set.of(
                        new DeviceDataTypeAdvertisement.Builder(DistanceRecord.class)
                                .setAvailable(true)
                                .setUserEnabled(true)
                                .build());
        Device testDevice =
                new Device.Builder()
                        .setManufacturer("TestManufacturer")
                        .setModel("TestModel")
                        .setType(Device.DEVICE_TYPE_PHONE)
                        .setDisplayName("TestDisplayName")
                        .build();
        DeviceDataAdvertisement advertisement =
                new DeviceDataAdvertisement(testDevice, currentDeviceId, ads);
        HealthConnectReceiver<Void> adReceiver = new HealthConnectReceiver<>();
        TestUtils.advertiseDeviceDataSources(Set.of(advertisement), outcomeExecutor(), adReceiver);

        HealthConnectReceiver<DeviceDataSource> receiver = new HealthConnectReceiver<>();
        getCurrentDeviceDataSource(outcomeExecutor(), receiver);

        assertThat(receiver.assertAndGetException().getErrorCode())
                .isEqualTo(HealthConnectException.ERROR_SECURITY);
        assertThat(receiver.assertAndGetException().getMessage())
                .isEqualTo(
                        "java.lang.SecurityException: Caller must hold at least one Health Connect"
                                + " permission");
    }

    private List<Record> getTestRecords() {
        return Arrays.asList(
                getStepsRecord(),
                getHeartRateRecord(),
                buildSleepSession(),
                getDistanceRecordWithNonEmptyId(),
                getTotalCaloriesBurnedRecord("client_id"),
                buildExerciseSession(),
                MINDFULNESS_SESSION_RECORD_FACTORY.newEmptyRecord(
                        newEmptyMetadataWithClientId("mindfulness-client-id"),
                        mNow.minus(Duration.ofMinutes(20)),
                        mNow.minus(Duration.ofMinutes(10))));
    }
}
