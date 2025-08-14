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

import static com.android.healthfitness.flags.Flags.FLAG_LATENCY_METRICS_FLAG;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.health.connect.datatypes.CyclingPedalingCadenceRecord;
import android.health.connect.datatypes.ExerciseSessionRecord;
import android.health.connect.datatypes.ExerciseSessionType;
import android.health.connect.datatypes.HeartRateRecord;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.PowerRecord;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.health.connect.datatypes.SkinTemperatureRecord;
import android.health.connect.datatypes.SpeedRecord;
import android.health.connect.datatypes.StepsCadenceRecord;
import android.health.connect.datatypes.units.Power;
import android.health.connect.datatypes.units.TemperatureDelta;
import android.health.connect.datatypes.units.Velocity;
import android.healthconnect.testing.unittest.FitnessTestUtils;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;
import com.android.server.healthconnect.permission.FirstGrantTimeManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class DataGranularityStatsCollectorTest {

    private static final String TEST_PACKAGE_NAME = "test.package.name";
    private static final String TEST_PACKAGE_NAME_TWO = "test.package.name.two";

    @Rule public final TemporaryFolder mEnvironmentDataDir = new TemporaryFolder();
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Mock private FirstGrantTimeManager mFirstGrantTimeManager;
    private DataGranularityStatsCollector mDataGranularityStatsCollector;
    private FitnessTestUtils mFitnessTestUtils;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectInjector healthConnectInjector =
                HealthConnectInjectorImpl.newBuilderForTest(context)
                        .setFirstGrantTimeManager(mFirstGrantTimeManager)
                        .setEnvironmentDataDirectory(mEnvironmentDataDir.getRoot())
                        .build();
        mDataGranularityStatsCollector = healthConnectInjector.getDataGranularityStatsCollector();
        mFitnessTestUtils = new FitnessTestUtils(healthConnectInjector);

        mFitnessTestUtils.insertApp(TEST_PACKAGE_NAME);
        mFitnessTestUtils.insertApp(TEST_PACKAGE_NAME_TWO);
    }

    @Test
    @EnableFlags(FLAG_LATENCY_METRICS_FLAG)
    public void getLastWeekActiveDataSessionsGranularityStats_noSessions_returnsEmptyList() {
        List<DataGranularityStatsCollector.GranularityStats> stats =
                mDataGranularityStatsCollector.getLastWeekActiveDataSessionsGranularityStats();

        assertThat(stats).isEmpty();
    }

    @Test
    @EnableFlags(FLAG_LATENCY_METRICS_FLAG)
    public void getLastWeekActiveDataSessionsGranularityStats_noSeriesData_returnsEmptyList() {
        Instant sessionStartTime = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant sessionEndTime = sessionStartTime.plus(1, ChronoUnit.HOURS);
        insertExerciseSession(sessionStartTime, sessionEndTime, TEST_PACKAGE_NAME);

        List<DataGranularityStatsCollector.GranularityStats> stats =
                mDataGranularityStatsCollector.getLastWeekActiveDataSessionsGranularityStats();

        assertThat(stats).hasSize(0);
    }

    @Test
    @EnableFlags(FLAG_LATENCY_METRICS_FLAG)
    public void
            getLastWeekActiveDataSessionsGranularityStats_oneSeriesSample_returnsSessionDurationAsGranularity() {
        Instant sessionStartTime = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant sessionEndTime = sessionStartTime.plus(8, ChronoUnit.HOURS);
        long sessionDuration = sessionEndTime.toEpochMilli() - sessionStartTime.toEpochMilli();
        insertExerciseSession(sessionStartTime, sessionEndTime, TEST_PACKAGE_NAME_TWO);
        insertSeriesData(
                sessionStartTime,
                sessionEndTime,
                TEST_PACKAGE_NAME_TWO,
                /* numberOfSamplesToInsert= */ 1);

        List<DataGranularityStatsCollector.GranularityStats> stats =
                mDataGranularityStatsCollector.getLastWeekActiveDataSessionsGranularityStats();

        assertThat(stats).hasSize(6);
        for (DataGranularityStatsCollector.GranularityStats stat : stats) {
            assertThat(stat.granularity()).isEqualTo(sessionDuration);
            assertThat(stat.packageName()).isEqualTo(TEST_PACKAGE_NAME_TWO);
        }
    }

    @Test
    @EnableFlags(FLAG_LATENCY_METRICS_FLAG)
    public void
            getLastWeekActiveDataSessionsGranularityStats_withSeriesData_calculatesGranularity() {
        Instant sessionStartTime = Instant.now().minus(2, ChronoUnit.DAYS);
        Instant sessionEndTime = sessionStartTime.plus(30, ChronoUnit.MINUTES);
        long sessionDurationMillis =
                sessionEndTime.toEpochMilli() - sessionStartTime.toEpochMilli();
        insertExerciseSession(sessionStartTime, sessionEndTime, TEST_PACKAGE_NAME);
        insertHeartRateSeriesData(
                sessionStartTime,
                sessionEndTime,
                TEST_PACKAGE_NAME,
                /* numberOfSamplesToInsert= */ 180);
        insertSpeedSeriesData(
                sessionStartTime,
                sessionEndTime,
                TEST_PACKAGE_NAME,
                /* numberOfSamplesToInsert= */ 90);
        insertPowerSeriesData(
                sessionStartTime,
                sessionEndTime,
                TEST_PACKAGE_NAME,
                /* numberOfSamplesToInsert= */ 2);
        insertSkinTemperatureSeriesData(
                sessionStartTime,
                sessionEndTime,
                TEST_PACKAGE_NAME,
                /* numberOfSamplesToInsert= */ 2);
        insertStepsCadenceSeriesData(
                sessionStartTime,
                sessionEndTime,
                TEST_PACKAGE_NAME,
                /* numberOfSamplesToInsert= */ 2);
        insertCyclingPedalingCadenceSeriesData(
                sessionStartTime,
                sessionEndTime,
                TEST_PACKAGE_NAME,
                /* numberOfSamplesToInsert= */ 2);

        List<DataGranularityStatsCollector.GranularityStats> stats =
                mDataGranularityStatsCollector.getLastWeekActiveDataSessionsGranularityStats();

        assertThat(stats).hasSize(6);
        for (DataGranularityStatsCollector.GranularityStats stat : stats) {
            assertThat(stat.packageName()).isEqualTo(TEST_PACKAGE_NAME);
            switch (stat.seriesRecordIdentifier()) {
                case RecordTypeIdentifier.RECORD_TYPE_HEART_RATE ->
                        assertThat(stat.granularity()).isEqualTo(sessionDurationMillis / 180);
                case RecordTypeIdentifier.RECORD_TYPE_SPEED ->
                        assertThat(stat.granularity()).isEqualTo(sessionDurationMillis / 90);
                default -> assertThat(stat.granularity()).isEqualTo(sessionDurationMillis / 2);
            }
        }
    }

    @Test
    @EnableFlags(FLAG_LATENCY_METRICS_FLAG)
    public void
            getLastWeekActiveDataSessionsGranularityStats_multipleSessions_calculatesGranularity() {
        Instant sessionOneStartTime = Instant.now().minus(3, ChronoUnit.DAYS);
        Instant sessionOneEndTime = sessionOneStartTime.plus(1, ChronoUnit.HOURS);
        long sessionOneDuration =
                sessionOneEndTime.toEpochMilli() - sessionOneStartTime.toEpochMilli();
        insertExerciseSession(sessionOneStartTime, sessionOneEndTime, TEST_PACKAGE_NAME);
        insertSeriesData(
                sessionOneStartTime,
                sessionOneEndTime,
                TEST_PACKAGE_NAME,
                /* numberOfSamplesToInsert= */ 20);

        Instant sessionTwoStartTime = Instant.now().minus(4, ChronoUnit.DAYS);
        Instant sessionTwoEndTime = sessionTwoStartTime.plus(2, ChronoUnit.HOURS);
        long sessionTwoDuration =
                sessionTwoEndTime.toEpochMilli() - sessionTwoStartTime.toEpochMilli();
        insertExerciseSession(sessionTwoStartTime, sessionTwoEndTime, TEST_PACKAGE_NAME_TWO);
        insertSeriesData(
                sessionTwoStartTime,
                sessionTwoEndTime,
                TEST_PACKAGE_NAME_TWO,
                /* numberOfSamplesToInsert= */ 50);

        List<DataGranularityStatsCollector.GranularityStats> stats =
                mDataGranularityStatsCollector.getLastWeekActiveDataSessionsGranularityStats();

        // 2 sessions * 6 data types = 12 stats
        assertThat(stats).hasSize(12);

        long sessionOneGranularity = sessionOneDuration / 20;
        long sessionTwoGranularity = sessionTwoDuration / 50;

        int sessionOneCount = 0;
        int sessionTwoCount = 0;

        for (DataGranularityStatsCollector.GranularityStats stat : stats) {
            if (stat.granularity() == sessionOneGranularity) {
                sessionOneCount++;
                assertThat(stat.packageName()).isEqualTo(TEST_PACKAGE_NAME);
            } else if (stat.granularity() == sessionTwoGranularity) {
                sessionTwoCount++;
                assertThat(stat.packageName()).isEqualTo(TEST_PACKAGE_NAME_TWO);
            }
        }

        assertThat(sessionOneCount).isEqualTo(6);
        assertThat(sessionTwoCount).isEqualTo(6);
    }

    @Test
    @DisableFlags(FLAG_LATENCY_METRICS_FLAG)
    public void flagDisabled_getLastWeekActiveDataSessionsGranularityStats_emptyListReturned() {
        Instant sessionStartTime = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant sessionEndTime = sessionStartTime.plus(8, ChronoUnit.HOURS);
        insertExerciseSession(sessionStartTime, sessionEndTime, TEST_PACKAGE_NAME_TWO);
        insertSeriesData(
                sessionStartTime,
                sessionEndTime,
                TEST_PACKAGE_NAME_TWO,
                /* numberOfSamplesToInsert= */ 1);

        List<DataGranularityStatsCollector.GranularityStats> stats =
                mDataGranularityStatsCollector.getLastWeekActiveDataSessionsGranularityStats();

        assertThat(stats).hasSize(0);
    }

    @Test
    @EnableFlags(FLAG_LATENCY_METRICS_FLAG)
    public void getLastWeekActiveDataSessionsGranularityStats_seriesOutsideSessionIsIgnored() {
        Instant sessionStartTime = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant sessionEndTime = sessionStartTime.plus(8, ChronoUnit.HOURS);
        long sessionDuration = sessionEndTime.toEpochMilli() - sessionStartTime.toEpochMilli();
        insertExerciseSession(sessionStartTime, sessionEndTime, TEST_PACKAGE_NAME_TWO);
        // Insert before start time
        insertHeartRateSeriesData(
                /* startTime= */ sessionStartTime.minusMillis(1000),
                /* endTime= */ sessionStartTime.minusMillis(10),
                TEST_PACKAGE_NAME_TWO,
                /* numberOfSamplesToInsert= */ 2);
        insertHeartRateSeriesData(
                sessionStartTime,
                sessionEndTime,
                TEST_PACKAGE_NAME_TWO,
                /* numberOfSamplesToInsert= */ 5);
        // Insert after end time
        insertHeartRateSeriesData(
                /* startTime= */ sessionEndTime.plusMillis(10),
                /* endTime= */ sessionEndTime.plusMillis(1000),
                TEST_PACKAGE_NAME_TWO,
                /* numberOfSamplesToInsert= */ 2);

        List<DataGranularityStatsCollector.GranularityStats> stats =
                mDataGranularityStatsCollector.getLastWeekActiveDataSessionsGranularityStats();
        long granularity = sessionDuration / 5; // number of series inserted during session

        assertThat(stats).hasSize(1); // Only Heart Rate is present
        for (DataGranularityStatsCollector.GranularityStats stat : stats) {
            if (stat.seriesRecordIdentifier() != RecordTypeIdentifier.RECORD_TYPE_HEART_RATE) {
                continue;
            }
            assertThat(stat.granularity()).isEqualTo(granularity);
            assertThat(stat.packageName()).isEqualTo(TEST_PACKAGE_NAME_TWO);
        }
    }

    @Test
    @EnableFlags(FLAG_LATENCY_METRICS_FLAG)
    public void getLastWeekActiveDataSessionsGranularityStats_seriesFromOtherPackagesIsIgnored() {
        Instant sessionStartTime = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant sessionEndTime = sessionStartTime.plus(8, ChronoUnit.HOURS);
        long sessionDuration = sessionEndTime.toEpochMilli() - sessionStartTime.toEpochMilli();
        insertExerciseSession(sessionStartTime, sessionEndTime, TEST_PACKAGE_NAME_TWO);
        insertSeriesData(
                sessionStartTime,
                sessionEndTime,
                TEST_PACKAGE_NAME,
                /* numberOfSamplesToInsert= */ 5);
        insertHeartRateSeriesData(
                sessionStartTime,
                sessionEndTime,
                TEST_PACKAGE_NAME_TWO,
                /* numberOfSamplesToInsert= */ 5);

        List<DataGranularityStatsCollector.GranularityStats> stats =
                mDataGranularityStatsCollector.getLastWeekActiveDataSessionsGranularityStats();
        long granularity = sessionDuration / 5;

        assertThat(stats).hasSize(1); // Only HR data from valid package
        for (DataGranularityStatsCollector.GranularityStats stat : stats) {
            assertThat(stat.granularity()).isEqualTo(granularity);
            assertThat(stat.packageName()).isEqualTo(TEST_PACKAGE_NAME_TWO);
        }
    }

    private void insertExerciseSession(Instant startTime, Instant endTime, String packageName) {
        ExerciseSessionRecord exerciseSessionRecord =
                new ExerciseSessionRecord.Builder(
                                new Metadata.Builder().build(),
                                startTime,
                                endTime,
                                ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING)
                        .build();

        mFitnessTestUtils.insertRecords(packageName, exerciseSessionRecord.toRecordInternal());
    }

    private void insertSeriesData(
            Instant startTime, Instant endTime, String packageName, int numberOfSamplesToInsert) {
        insertHeartRateSeriesData(startTime, endTime, packageName, numberOfSamplesToInsert);
        insertSpeedSeriesData(startTime, endTime, packageName, numberOfSamplesToInsert);
        insertPowerSeriesData(startTime, endTime, packageName, numberOfSamplesToInsert);
        insertStepsCadenceSeriesData(startTime, endTime, packageName, numberOfSamplesToInsert);
        insertCyclingPedalingCadenceSeriesData(
                startTime, endTime, packageName, numberOfSamplesToInsert);
        insertSkinTemperatureSeriesData(startTime, endTime, packageName, numberOfSamplesToInsert);
    }

    private void insertHeartRateSeriesData(
            Instant startTime, Instant endTime, String packageName, int numberOfSamplesToInsert) {
        List<HeartRateRecord.HeartRateSample> heartRateSamples = new ArrayList<>();
        for (int i = 0; i < numberOfSamplesToInsert; i++) {
            heartRateSamples.add(
                    new HeartRateRecord.HeartRateSample(
                            /* beatsPerMinute= */ 72, startTime.plusMillis(i + 10)));
        }
        HeartRateRecord heartRateRecord =
                new HeartRateRecord.Builder(
                                new Metadata.Builder().build(),
                                startTime,
                                endTime,
                                heartRateSamples)
                        .build();
        mFitnessTestUtils.insertRecords(packageName, heartRateRecord.toRecordInternal());
    }

    private void insertSpeedSeriesData(
            Instant startTime, Instant endTime, String packageName, int numberOfSamplesToInsert) {
        List<SpeedRecord.SpeedRecordSample> speedSamples = new ArrayList<>();
        for (int i = 0; i < numberOfSamplesToInsert; i++) {
            speedSamples.add(
                    new SpeedRecord.SpeedRecordSample(
                            Velocity.fromMetersPerSecond(20), startTime.plusMillis(i + 10)));
        }
        SpeedRecord speedRecord =
                new SpeedRecord.Builder(
                                new Metadata.Builder().build(), startTime, endTime, speedSamples)
                        .build();
        mFitnessTestUtils.insertRecords(packageName, speedRecord.toRecordInternal());
    }

    private void insertPowerSeriesData(
            Instant startTime, Instant endTime, String packageName, int numberOfSamplesToInsert) {
        List<PowerRecord.PowerRecordSample> powerSamples = new ArrayList<>();
        for (int i = 0; i < numberOfSamplesToInsert; i++) {
            powerSamples.add(
                    new PowerRecord.PowerRecordSample(
                            Power.fromWatts(15), startTime.plusMillis(i + 10)));
        }
        PowerRecord powerRecord =
                new PowerRecord.Builder(
                                new Metadata.Builder().build(), startTime, endTime, powerSamples)
                        .build();
        mFitnessTestUtils.insertRecords(packageName, powerRecord.toRecordInternal());
    }

    private void insertStepsCadenceSeriesData(
            Instant startTime, Instant endTime, String packageName, int numberOfSamplesToInsert) {
        List<StepsCadenceRecord.StepsCadenceRecordSample> stepsCadenceSamples = new ArrayList<>();
        for (int i = 0; i < numberOfSamplesToInsert; i++) {
            stepsCadenceSamples.add(
                    new StepsCadenceRecord.StepsCadenceRecordSample(
                            /* rate= */ 90.0, startTime.plusMillis(i + 10)));
        }
        StepsCadenceRecord stepsCadenceRecord =
                new StepsCadenceRecord.Builder(
                                new Metadata.Builder().build(),
                                startTime,
                                endTime,
                                stepsCadenceSamples)
                        .build();
        mFitnessTestUtils.insertRecords(packageName, stepsCadenceRecord.toRecordInternal());
    }

    private void insertCyclingPedalingCadenceSeriesData(
            Instant startTime, Instant endTime, String packageName, int numberOfSamplesToInsert) {
        List<CyclingPedalingCadenceRecord.CyclingPedalingCadenceRecordSample>
                cyclingPedalingCadenceSamples = new ArrayList<>();
        for (int i = 0; i < numberOfSamplesToInsert; i++) {
            cyclingPedalingCadenceSamples.add(
                    new CyclingPedalingCadenceRecord.CyclingPedalingCadenceRecordSample(
                            /* revolutionsPerMinute= */ 80.0, startTime.plusMillis(i + 10)));
        }
        CyclingPedalingCadenceRecord cyclingPedalingCadenceRecord =
                new CyclingPedalingCadenceRecord.Builder(
                                new Metadata.Builder().build(),
                                startTime,
                                endTime,
                                cyclingPedalingCadenceSamples)
                        .build();
        mFitnessTestUtils.insertRecords(
                packageName, cyclingPedalingCadenceRecord.toRecordInternal());
    }

    private void insertSkinTemperatureSeriesData(
            Instant startTime, Instant endTime, String packageName, int numberOfSamplesToInsert) {
        List<SkinTemperatureRecord.Delta> skinTemperatureSamples = new ArrayList<>();
        for (int i = 0; i < numberOfSamplesToInsert; i++) {
            skinTemperatureSamples.add(
                    new SkinTemperatureRecord.Delta(
                            TemperatureDelta.fromCelsius(1), startTime.plusMillis(i + 10)));
        }
        SkinTemperatureRecord skinTemperatureRecord =
                new SkinTemperatureRecord.Builder(
                                new Metadata.Builder().build(), startTime, endTime)
                        .setDeltas(skinTemperatureSamples)
                        .build();
        mFitnessTestUtils.insertRecords(packageName, skinTemperatureRecord.toRecordInternal());
    }
}
