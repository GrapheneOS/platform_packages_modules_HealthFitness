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
import android.health.connect.datatypes.ActiveCaloriesBurnedRecord;
import android.health.connect.datatypes.CyclingPedalingCadenceRecord;
import android.health.connect.datatypes.DistanceRecord;
import android.health.connect.datatypes.ElevationGainedRecord;
import android.health.connect.datatypes.ExerciseSessionRecord;
import android.health.connect.datatypes.ExerciseSessionType;
import android.health.connect.datatypes.HeartRateRecord;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.PowerRecord;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.health.connect.datatypes.SkinTemperatureRecord;
import android.health.connect.datatypes.SpeedRecord;
import android.health.connect.datatypes.StepsCadenceRecord;
import android.health.connect.datatypes.StepsRecord;
import android.health.connect.datatypes.TotalCaloriesBurnedRecord;
import android.health.connect.datatypes.units.Energy;
import android.health.connect.datatypes.units.Length;
import android.health.connect.datatypes.units.Power;
import android.health.connect.datatypes.units.TemperatureDelta;
import android.health.connect.datatypes.units.Velocity;
import android.health.connect.internal.datatypes.RecordInternal;
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
                mDataGranularityStatsCollector.getLastWeekExerciseSessionsGranularityStats();

        assertThat(stats).isEmpty();
    }

    @Test
    @EnableFlags(FLAG_LATENCY_METRICS_FLAG)
    public void getLastWeekActiveDataSessionsGranularityStats_noSeriesData_returnsEmptyList() {
        Instant sessionStartTime = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant sessionEndTime = sessionStartTime.plus(1, ChronoUnit.HOURS);
        insertExerciseSession(sessionStartTime, sessionEndTime, TEST_PACKAGE_NAME);

        List<DataGranularityStatsCollector.GranularityStats> stats =
                mDataGranularityStatsCollector.getLastWeekExerciseSessionsGranularityStats();

        assertThat(stats).hasSize(0);
    }

    @Test
    @EnableFlags(FLAG_LATENCY_METRICS_FLAG)
    public void
            getLastWeekExerciseSessionsGranularityStats_oneSeriesSample_returnsSessionDurationAsGranularity() {
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
                mDataGranularityStatsCollector.getLastWeekExerciseSessionsGranularityStats();

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
                mDataGranularityStatsCollector.getLastWeekExerciseSessionsGranularityStats();

        List<DataGranularityStatsCollector.GranularityStats> expectedStats =
                List.of(
                        new DataGranularityStatsCollector.GranularityStats(
                                TEST_PACKAGE_NAME,
                                RecordTypeIdentifier.RECORD_TYPE_HEART_RATE,
                                /* granularity= */ sessionDurationMillis / 180),
                        new DataGranularityStatsCollector.GranularityStats(
                                TEST_PACKAGE_NAME,
                                RecordTypeIdentifier.RECORD_TYPE_SPEED,
                                /* granularity= */ sessionDurationMillis / 90),
                        new DataGranularityStatsCollector.GranularityStats(
                                TEST_PACKAGE_NAME,
                                RecordTypeIdentifier.RECORD_TYPE_POWER,
                                /* granularity= */ sessionDurationMillis / 2),
                        new DataGranularityStatsCollector.GranularityStats(
                                TEST_PACKAGE_NAME,
                                RecordTypeIdentifier.RECORD_TYPE_SKIN_TEMPERATURE,
                                /* granularity= */ sessionDurationMillis / 2),
                        new DataGranularityStatsCollector.GranularityStats(
                                TEST_PACKAGE_NAME,
                                RecordTypeIdentifier.RECORD_TYPE_STEPS_CADENCE,
                                /* granularity= */ sessionDurationMillis / 2),
                        new DataGranularityStatsCollector.GranularityStats(
                                TEST_PACKAGE_NAME,
                                RecordTypeIdentifier.RECORD_TYPE_CYCLING_PEDALING_CADENCE,
                                /* granularity= */ sessionDurationMillis / 2));

        assertThat(stats).containsExactlyElementsIn(expectedStats);
    }

    @Test
    @EnableFlags(FLAG_LATENCY_METRICS_FLAG)
    public void
            getLastWeekExerciseSessionsGranularityStats_multipleSessions_calculatesGranularity() {
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
                mDataGranularityStatsCollector.getLastWeekExerciseSessionsGranularityStats();

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
                mDataGranularityStatsCollector.getLastWeekExerciseSessionsGranularityStats();

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
                mDataGranularityStatsCollector.getLastWeekExerciseSessionsGranularityStats();
        long granularity = sessionDuration / 5; // number of series inserted during session

        assertThat(stats).hasSize(1); // Only Heart Rate is present
        for (DataGranularityStatsCollector.GranularityStats stat : stats) {
            if (stat.recordIdentifier() != RecordTypeIdentifier.RECORD_TYPE_HEART_RATE) {
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
                mDataGranularityStatsCollector.getLastWeekExerciseSessionsGranularityStats();
        long granularity = sessionDuration / 5;

        assertThat(stats).hasSize(1); // Only HR data from valid package
        for (DataGranularityStatsCollector.GranularityStats stat : stats) {
            assertThat(stat.granularity()).isEqualTo(granularity);
            assertThat(stat.packageName()).isEqualTo(TEST_PACKAGE_NAME_TWO);
        }
    }

    @Test
    @EnableFlags(FLAG_LATENCY_METRICS_FLAG)
    public void getLastWeekExercise_intervalData_calculatesGranularity() {
        Instant sessionStartTime = Instant.now().minus(2, ChronoUnit.DAYS);
        Instant sessionEndTime = sessionStartTime.plus(30, ChronoUnit.MINUTES);
        insertExerciseSession(sessionStartTime, sessionEndTime, TEST_PACKAGE_NAME);
        insertStepsRecord(
                sessionStartTime,
                TEST_PACKAGE_NAME,
                /* durationOfEachRecordInMillis= */ 1000L,
                /* numberOfRecordsToInsert= */ 9);
        insertDistanceRecord(
                sessionStartTime,
                TEST_PACKAGE_NAME,
                /* durationOfEachRecordInMillis= */ 2000L,
                /* numberOfRecordsToInsert= */ 10);
        insertActiveCaloriesBurnedRecord(
                sessionStartTime,
                TEST_PACKAGE_NAME,
                /* durationOfEachRecordInMillis= */ 3000L,
                /* numberOfRecordsToInsert= */ 5);
        insertTotalCaloriesBurnedRecord(
                sessionStartTime,
                TEST_PACKAGE_NAME,
                /* durationOfEachRecordInMillis= */ 4000L,
                /* numberOfRecordsToInsert= */ 4);
        insertElevationGainedRecord(
                sessionStartTime,
                TEST_PACKAGE_NAME,
                /* durationOfEachRecordInMillis= */ 5000L,
                /* numberOfRecordsToInsert= */ 3);

        List<DataGranularityStatsCollector.GranularityStats> stats =
                mDataGranularityStatsCollector.getLastWeekExerciseSessionsGranularityStats();

        List<DataGranularityStatsCollector.GranularityStats> expectedStats =
                List.of(
                        new DataGranularityStatsCollector.GranularityStats(
                                TEST_PACKAGE_NAME,
                                RecordTypeIdentifier.RECORD_TYPE_STEPS,
                                /* granularity= */ 1000L),
                        new DataGranularityStatsCollector.GranularityStats(
                                TEST_PACKAGE_NAME,
                                RecordTypeIdentifier.RECORD_TYPE_DISTANCE,
                                /* granularity= */ 2000L),
                        new DataGranularityStatsCollector.GranularityStats(
                                TEST_PACKAGE_NAME,
                                RecordTypeIdentifier.RECORD_TYPE_ACTIVE_CALORIES_BURNED,
                                /* granularity= */ 3000L),
                        new DataGranularityStatsCollector.GranularityStats(
                                TEST_PACKAGE_NAME,
                                RecordTypeIdentifier.RECORD_TYPE_TOTAL_CALORIES_BURNED,
                                /* granularity= */ 4000L),
                        new DataGranularityStatsCollector.GranularityStats(
                                TEST_PACKAGE_NAME,
                                RecordTypeIdentifier.RECORD_TYPE_ELEVATION_GAINED,
                                /* granularity= */ 5000L));

        assertThat(stats).containsExactlyElementsIn(expectedStats);
    }

    @Test
    @EnableFlags(FLAG_LATENCY_METRICS_FLAG)
    public void getLastWeekExerciseSessionsGranularityStats_sessionOlderThanAWeek_isIgnored() {
        Instant sessionStartTime = Instant.now().minus(8, ChronoUnit.DAYS);
        Instant sessionEndTime = sessionStartTime.plus(1, ChronoUnit.HOURS);
        insertExerciseSession(sessionStartTime, sessionEndTime, TEST_PACKAGE_NAME);
        insertSeriesData(
                sessionStartTime,
                sessionEndTime,
                TEST_PACKAGE_NAME,
                /* numberOfSamplesToInsert= */ 10);

        List<DataGranularityStatsCollector.GranularityStats> stats =
                mDataGranularityStatsCollector.getLastWeekExerciseSessionsGranularityStats();

        assertThat(stats).isEmpty();
    }

    @Test
    @EnableFlags(FLAG_LATENCY_METRICS_FLAG)
    public void
            getLastWeekExerciseSessionsGranularityStats_withSeriesAndIntervalData_calculatesGranularity() {
        Instant sessionStartTime = Instant.now().minus(2, ChronoUnit.DAYS);
        Instant sessionEndTime = sessionStartTime.plus(30, ChronoUnit.MINUTES);
        long sessionDurationMillis =
                sessionEndTime.toEpochMilli() - sessionStartTime.toEpochMilli();
        insertExerciseSession(sessionStartTime, sessionEndTime, TEST_PACKAGE_NAME);

        // Series data
        insertHeartRateSeriesData(
                sessionStartTime,
                sessionEndTime,
                TEST_PACKAGE_NAME,
                /* numberOfSamplesToInsert= */ 180);

        // Interval data
        insertStepsRecord(
                sessionStartTime,
                TEST_PACKAGE_NAME,
                /* durationOfEachRecordInMillis= */ 1000L,
                /* numberOfRecordsToInsert= */ 9);
        insertDistanceRecord(
                sessionStartTime,
                TEST_PACKAGE_NAME,
                /* durationOfEachRecordInMillis= */ 2000L,
                /* numberOfRecordsToInsert= */ 10);

        List<DataGranularityStatsCollector.GranularityStats> stats =
                mDataGranularityStatsCollector.getLastWeekExerciseSessionsGranularityStats();

        List<DataGranularityStatsCollector.GranularityStats> expectedStats =
                List.of(
                        new DataGranularityStatsCollector.GranularityStats(
                                TEST_PACKAGE_NAME,
                                RecordTypeIdentifier.RECORD_TYPE_HEART_RATE,
                                /* granularity= */ sessionDurationMillis / 180),
                        new DataGranularityStatsCollector.GranularityStats(
                                TEST_PACKAGE_NAME,
                                RecordTypeIdentifier.RECORD_TYPE_STEPS,
                                /* granularity= */ 1000L),
                        new DataGranularityStatsCollector.GranularityStats(
                                TEST_PACKAGE_NAME,
                                RecordTypeIdentifier.RECORD_TYPE_DISTANCE,
                                /* granularity= */ 2000L));

        assertThat(stats).containsExactlyElementsIn(expectedStats);
    }

    @Test
    @EnableFlags(FLAG_LATENCY_METRICS_FLAG)
    public void getLastWeekExercise_intervalDataFromOtherPackage_isIgnored() {
        Instant sessionStartTime = Instant.now().minus(2, ChronoUnit.DAYS);
        Instant sessionEndTime = sessionStartTime.plus(30, ChronoUnit.MINUTES);
        insertExerciseSession(sessionStartTime, sessionEndTime, TEST_PACKAGE_NAME);
        insertStepsRecord(
                sessionStartTime,
                TEST_PACKAGE_NAME,
                /* durationOfEachRecordInMillis= */ 1000L,
                /* numberOfRecordsToInsert= */ 9);
        insertDistanceRecord(
                sessionStartTime,
                TEST_PACKAGE_NAME_TWO, // Different package
                /* durationOfEachRecordInMillis= */ 2000L,
                /* numberOfRecordsToInsert= */ 10);

        List<DataGranularityStatsCollector.GranularityStats> stats =
                mDataGranularityStatsCollector.getLastWeekExerciseSessionsGranularityStats();

        assertThat(stats).hasSize(1);
        DataGranularityStatsCollector.GranularityStats stat = stats.get(0);
        assertThat(stat.packageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(stat.recordIdentifier()).isEqualTo(RecordTypeIdentifier.RECORD_TYPE_STEPS);
        assertThat(stat.granularity()).isEqualTo(1000L);
    }

    @Test
    @EnableFlags(FLAG_LATENCY_METRICS_FLAG)
    public void getLastWeekExercise_intervalDataOutsideSession_isIgnored() {
        Instant sessionStartTime = Instant.now().minus(2, ChronoUnit.DAYS);
        Instant sessionEndTime = sessionStartTime.plus(30, ChronoUnit.MINUTES);
        insertExerciseSession(sessionStartTime, sessionEndTime, TEST_PACKAGE_NAME);

        // This one is inside the session
        insertStepsRecord(
                sessionStartTime,
                TEST_PACKAGE_NAME,
                /* durationOfEachRecordInMillis= */ 1000L,
                /* numberOfRecordsToInsert= */ 1);

        // This one starts before the session
        insertDistanceRecord(
                sessionStartTime.minus(1, ChronoUnit.MINUTES),
                TEST_PACKAGE_NAME,
                /* durationOfEachRecordInMillis= */ 2000L,
                /* numberOfRecordsToInsert= */ 1);

        // This one starts during, but ends after
        insertActiveCaloriesBurnedRecord(
                sessionEndTime.minus(1, ChronoUnit.MINUTES),
                TEST_PACKAGE_NAME,
                /* durationOfEachRecordInMillis= */ 2 * 60 * 1000L,
                // 2 minutes, so ends after session
                /* numberOfRecordsToInsert= */ 1);

        // This one starts after the session
        insertTotalCaloriesBurnedRecord(
                sessionEndTime.plus(1, ChronoUnit.MINUTES),
                TEST_PACKAGE_NAME,
                /* durationOfEachRecordInMillis= */ 1000L,
                /* numberOfRecordsToInsert= */ 1);

        List<DataGranularityStatsCollector.GranularityStats> stats =
                mDataGranularityStatsCollector.getLastWeekExerciseSessionsGranularityStats();

        assertThat(stats).hasSize(1);
        DataGranularityStatsCollector.GranularityStats stat = stats.get(0);
        assertThat(stat.packageName()).isEqualTo(TEST_PACKAGE_NAME);
        assertThat(stat.recordIdentifier()).isEqualTo(RecordTypeIdentifier.RECORD_TYPE_STEPS);
        assertThat(stat.granularity()).isEqualTo(1000L);
    }

    @Test
    @EnableFlags(FLAG_LATENCY_METRICS_FLAG)
    public void
            getLastWeekExerciseSessionsGranularityStats_multipleSessionsSamePackage_calculatesGranularity() {
        // Session 1
        Instant sessionOneStartTime = Instant.now().minus(3, ChronoUnit.DAYS);
        Instant sessionOneEndTime = sessionOneStartTime.plus(1, ChronoUnit.HOURS);
        long sessionOneDuration =
                sessionOneEndTime.toEpochMilli() - sessionOneStartTime.toEpochMilli();
        insertExerciseSession(sessionOneStartTime, sessionOneEndTime, TEST_PACKAGE_NAME);
        insertHeartRateSeriesData(
                sessionOneStartTime,
                sessionOneEndTime,
                TEST_PACKAGE_NAME,
                /* numberOfSamplesToInsert= */ 20);
        insertStepsRecord(
                sessionOneStartTime,
                TEST_PACKAGE_NAME,
                /* durationOfEachRecordInMillis= */ 1000L,
                /* numberOfRecordsToInsert= */ 10);

        // Session 2
        Instant sessionTwoStartTime = Instant.now().minus(4, ChronoUnit.DAYS);
        Instant sessionTwoEndTime = sessionTwoStartTime.plus(2, ChronoUnit.HOURS);
        long sessionTwoDuration =
                sessionTwoEndTime.toEpochMilli() - sessionTwoStartTime.toEpochMilli();
        insertExerciseSession(sessionTwoStartTime, sessionTwoEndTime, TEST_PACKAGE_NAME);
        insertHeartRateSeriesData(
                sessionTwoStartTime,
                sessionTwoEndTime,
                TEST_PACKAGE_NAME,
                /* numberOfSamplesToInsert= */ 50);
        insertStepsRecord(
                sessionTwoStartTime,
                TEST_PACKAGE_NAME,
                /* durationOfEachRecordInMillis= */ 2000L,
                /* numberOfRecordsToInsert= */ 5);

        List<DataGranularityStatsCollector.GranularityStats> stats =
                mDataGranularityStatsCollector.getLastWeekExerciseSessionsGranularityStats();

        long sessionOneHrGranularity = sessionOneDuration / 20;
        long sessionTwoHrGranularity = sessionTwoDuration / 50;

        List<DataGranularityStatsCollector.GranularityStats> expectedStats =
                List.of(
                        new DataGranularityStatsCollector.GranularityStats(
                                TEST_PACKAGE_NAME,
                                RecordTypeIdentifier.RECORD_TYPE_HEART_RATE,
                                /* granularity= */ sessionOneHrGranularity),
                        new DataGranularityStatsCollector.GranularityStats(
                                TEST_PACKAGE_NAME,
                                RecordTypeIdentifier.RECORD_TYPE_STEPS,
                                /* granularity= */ 1000L),
                        new DataGranularityStatsCollector.GranularityStats(
                                TEST_PACKAGE_NAME,
                                RecordTypeIdentifier.RECORD_TYPE_HEART_RATE,
                                sessionTwoHrGranularity),
                        new DataGranularityStatsCollector.GranularityStats(
                                TEST_PACKAGE_NAME,
                                RecordTypeIdentifier.RECORD_TYPE_STEPS,
                                /* granularity= */ 2000L));

        assertThat(stats).containsExactlyElementsIn(expectedStats);
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

    private void insertStepsRecord(
            Instant startTime,
            String packageName,
            long durationOfEachRecordInMillis,
            int numberOfRecordsToInsert) {
        List<RecordInternal<?>> stepsRecords = new ArrayList<>(numberOfRecordsToInsert);
        for (int i = 0; i < numberOfRecordsToInsert; i++) {
            Instant intervalStartTime = startTime.plusMillis(durationOfEachRecordInMillis * i);
            Instant intervalEndTime = intervalStartTime.plusMillis(durationOfEachRecordInMillis);
            stepsRecords.add(
                    new StepsRecord.Builder(
                                    new Metadata.Builder().build(),
                                    intervalStartTime,
                                    intervalEndTime,
                                    /* count= */ 100)
                            .build()
                            .toRecordInternal());
        }

        mFitnessTestUtils.insertRecords(packageName, stepsRecords);
    }

    private void insertDistanceRecord(
            Instant startTime,
            String packageName,
            long durationOfEachRecordInMillis,
            int numberOfRecordsToInsert) {
        List<RecordInternal<?>> records = new ArrayList<>(numberOfRecordsToInsert);
        for (int i = 0; i < numberOfRecordsToInsert; i++) {
            Instant intervalStartTime = startTime.plusMillis(durationOfEachRecordInMillis * i);
            Instant intervalEndTime = intervalStartTime.plusMillis(durationOfEachRecordInMillis);
            records.add(
                    new DistanceRecord.Builder(
                                    new Metadata.Builder().build(),
                                    intervalStartTime,
                                    intervalEndTime,
                                    Length.fromMeters(100))
                            .build()
                            .toRecordInternal());
        }
        mFitnessTestUtils.insertRecords(packageName, records);
    }

    private void insertActiveCaloriesBurnedRecord(
            Instant startTime,
            String packageName,
            long durationOfEachRecordInMillis,
            int numberOfRecordsToInsert) {
        List<RecordInternal<?>> records = new ArrayList<>(numberOfRecordsToInsert);
        for (int i = 0; i < numberOfRecordsToInsert; i++) {
            Instant intervalStartTime = startTime.plusMillis(durationOfEachRecordInMillis * i);
            Instant intervalEndTime = intervalStartTime.plusMillis(durationOfEachRecordInMillis);
            records.add(
                    new ActiveCaloriesBurnedRecord.Builder(
                                    new Metadata.Builder().build(),
                                    intervalStartTime,
                                    intervalEndTime,
                                    Energy.fromCalories(100))
                            .build()
                            .toRecordInternal());
        }
        mFitnessTestUtils.insertRecords(packageName, records);
    }

    private void insertTotalCaloriesBurnedRecord(
            Instant startTime,
            String packageName,
            long durationOfEachRecordInMillis,
            int numberOfRecordsToInsert) {
        List<RecordInternal<?>> records = new ArrayList<>(numberOfRecordsToInsert);
        for (int i = 0; i < numberOfRecordsToInsert; i++) {
            Instant intervalStartTime = startTime.plusMillis(durationOfEachRecordInMillis * i);
            Instant intervalEndTime = intervalStartTime.plusMillis(durationOfEachRecordInMillis);
            records.add(
                    new TotalCaloriesBurnedRecord.Builder(
                                    new Metadata.Builder().build(),
                                    intervalStartTime,
                                    intervalEndTime,
                                    Energy.fromCalories(150))
                            .build()
                            .toRecordInternal());
        }
        mFitnessTestUtils.insertRecords(packageName, records);
    }

    private void insertElevationGainedRecord(
            Instant startTime,
            String packageName,
            long durationOfEachRecordInMillis,
            int numberOfRecordsToInsert) {
        List<RecordInternal<?>> records = new ArrayList<>(numberOfRecordsToInsert);
        for (int i = 0; i < numberOfRecordsToInsert; i++) {
            Instant intervalStartTime = startTime.plusMillis(durationOfEachRecordInMillis * i);
            Instant intervalEndTime = intervalStartTime.plusMillis(durationOfEachRecordInMillis);
            records.add(
                    new ElevationGainedRecord.Builder(
                                    new Metadata.Builder().build(),
                                    intervalStartTime,
                                    intervalEndTime,
                                    Length.fromMeters(20))
                            .build()
                            .toRecordInternal());
        }
        mFitnessTestUtils.insertRecords(packageName, records);
    }
}
