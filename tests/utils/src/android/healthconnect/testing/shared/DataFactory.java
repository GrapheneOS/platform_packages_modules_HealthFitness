/*
 * Copyright (C) 2024 The Android Open Source Project
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

package android.healthconnect.testing.shared;

import static android.health.connect.datatypes.Metadata.RECORDING_METHOD_ACTIVELY_RECORDED;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.health.connect.changelog.ChangeLogTokenRequest;
import android.health.connect.datatypes.ActiveCaloriesBurnedRecord;
import android.health.connect.datatypes.BasalMetabolicRateRecord;
import android.health.connect.datatypes.BloodPressureRecord;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.Device;
import android.health.connect.datatypes.DistanceRecord;
import android.health.connect.datatypes.ExerciseCompletionGoal;
import android.health.connect.datatypes.ExerciseLap;
import android.health.connect.datatypes.ExerciseRoute;
import android.health.connect.datatypes.ExerciseSegment;
import android.health.connect.datatypes.ExerciseSegmentType;
import android.health.connect.datatypes.ExerciseSessionRecord;
import android.health.connect.datatypes.ExerciseSessionType;
import android.health.connect.datatypes.HeartRateRecord;
import android.health.connect.datatypes.HeightRecord;
import android.health.connect.datatypes.LeanBodyMassRecord;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.PlannedExerciseBlock;
import android.health.connect.datatypes.PlannedExerciseSessionRecord;
import android.health.connect.datatypes.PlannedExerciseStep;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.SleepSessionRecord;
import android.health.connect.datatypes.StepsRecord;
import android.health.connect.datatypes.TotalCaloriesBurnedRecord;
import android.health.connect.datatypes.WeightRecord;
import android.health.connect.datatypes.units.Energy;
import android.health.connect.datatypes.units.Length;
import android.health.connect.datatypes.units.Mass;
import android.health.connect.datatypes.units.Power;
import android.health.connect.datatypes.units.Pressure;

import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public final class DataFactory {

    /**
     * Returns the current time truncated to MILLIS because HC does, to reduce flakiness in some
     * tests.
     *
     * <p>We still need to use the actual current time because a fixed timestamp doesn't work with
     * historical read restrictions without extensive refactoring of tests.
     */
    public static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    /** Generates a session start time. */
    public static Instant sessionStartTime(Instant now) {
        return now.minus(10, ChronoUnit.DAYS);
    }

    /** Generates a session end time. */
    public static Instant sessionEndTime(Instant now) {
        return sessionStartTime(now).plus(1, ChronoUnit.HOURS);
    }

    public static final long DEFAULT_LONG = -1;
    public static final int DEFAULT_PAGE_SIZE = 1000;
    public static final int MINIMUM_PAGE_SIZE = 1;
    public static final int MAXIMUM_PAGE_SIZE = 5000;

    public static Device buildDevice() {
        return new Device.Builder()
                .setManufacturer("google")
                .setModel("Pixel4a")
                .setType(Device.DEVICE_TYPE_PHONE)
                .build();
    }

    public static Metadata generateMetadata() {
        return generateMetadata(UUID.randomUUID().toString());
    }

    /** Generates a {@link Metadata} with specific {@code id}. */
    public static Metadata generateMetadata(String id) {
        return generateMetadata(id, "clientRecordId" + Math.random());
    }

    /** Generates a {@link Metadata} with specific {@code id} and {@code clientId}. */
    public static Metadata generateMetadata(String id, String clientId) {
        Context context = ApplicationProvider.getApplicationContext();
        return new Metadata.Builder()
                .setDevice(buildDevice())
                .setId(id)
                .setClientRecordId(clientId)
                .setDataOrigin(
                        new DataOrigin.Builder().setPackageName(context.getPackageName()).build())
                .setRecordingMethod(Metadata.RECORDING_METHOD_UNKNOWN)
                .build();
    }

    /** Generates a {@link Metadata} with a specific {@code clientId}. */
    public static Metadata generateMetadataWithClientId(String clientId) {
        return generateMetadata(UUID.randomUUID().toString(), clientId);
    }

    public static Metadata getEmptyMetadata() {
        return new Metadata.Builder().build();
    }

    /** Creates a {@link Metadata} with the given record id. */
    public static Metadata getMetadataForId(String id) {
        return new Metadata.Builder().setId(id).build();
    }

    /** Creates a {@link Metadata} with the given record id and data origin. */
    public static Metadata getMetadataForId(String id, DataOrigin dataOrigin) {
        return new Metadata.Builder().setId(id).setDataOrigin(dataOrigin).build();
    }

    /** Creates a {@link Metadata} with the given client record id. */
    public static Metadata getMetadataForClientId(String clientId) {
        return new Metadata.Builder().setClientRecordId(clientId).build();
    }

    /** Creates a {@link Metadata} with the given client record id. */
    public static Metadata getMetadataForClientId(String clientId, DataOrigin dataOrigin) {
        return new Metadata.Builder().setClientRecordId(clientId).setDataOrigin(dataOrigin).build();
    }

    /** Creates a {@link Metadata} with the given client record id. */
    public static Metadata getMetadataForClientIdAndVersion(String clientId, long clientVersion) {
        return new Metadata.Builder()
                .setClientRecordId(clientId)
                .setClientRecordVersion(clientVersion)
                .build();
    }

    /** Creates a {@link Metadata} with the given data origin. */
    public static Metadata getMetadata(DataOrigin dataOrigin) {
        return new Metadata.Builder().setDataOrigin(dataOrigin).build();
    }

    /** Creates a {@link DataOrigin} with the given package name. */
    public static DataOrigin getDataOrigin(String packageName) {
        return new DataOrigin.Builder().setPackageName(packageName).build();
    }

    /** Creates a list of {@link DataOrigin} from a list of package names. */
    public static List<DataOrigin> getDataOrigins(String... packageNames) {
        return Arrays.stream(packageNames).map(DataFactory::getDataOrigin).toList();
    }

    public static SleepSessionRecord buildSleepSession() {
        return buildSleepSession(generateMetadata());
    }

    /** Builds a {@link SleepSessionRecord} with empty {@link Metadata}. */
    public static SleepSessionRecord buildSleepSessionWithEmptyMetadata() {
        return buildSleepSession(getEmptyMetadata());
    }

    /** Builds a {@link SleepSessionRecord} with a specific {@link Metadata}. */
    public static SleepSessionRecord buildSleepSession(Metadata metadata) {
        Instant instant = now();
        return new SleepSessionRecord.Builder(
                        metadata, sessionStartTime(instant), sessionEndTime(instant))
                .setNotes("warm")
                .setTitle("Afternoon nap")
                .setStages(
                        List.of(
                                new SleepSessionRecord.Stage(
                                        sessionStartTime(instant),
                                        sessionStartTime(instant).plusSeconds(300),
                                        SleepSessionRecord.StageType.STAGE_TYPE_SLEEPING_LIGHT),
                                new SleepSessionRecord.Stage(
                                        sessionStartTime(instant).plusSeconds(300),
                                        sessionStartTime(instant).plusSeconds(600),
                                        SleepSessionRecord.StageType.STAGE_TYPE_SLEEPING_REM),
                                new SleepSessionRecord.Stage(
                                        sessionStartTime(instant).plusSeconds(900),
                                        sessionStartTime(instant).plusSeconds(1200),
                                        SleepSessionRecord.StageType.STAGE_TYPE_SLEEPING_DEEP)))
                .build();
    }

    /** Builds a {@link ExerciseSessionRecord} with {@link #generateMetadata()}. */
    public static ExerciseSessionRecord buildExerciseSession() {
        return buildExerciseSession(generateMetadata());
    }

    /** Builds a {@link ExerciseSessionRecord} with an empty {@link Metadata}. */
    public static ExerciseSessionRecord buildExerciseSessionWithEmptyMetadata() {
        return buildExerciseSession(getEmptyMetadata());
    }

    /** Builds a {@link ExerciseSessionRecord} with a specific {@code clientId}. */
    public static ExerciseSessionRecord buildExerciseSessionWithClientId(String clientId) {
        return buildExerciseSession(generateMetadataWithClientId(clientId));
    }

    /** Builds a {@link ExerciseSessionRecord} with a specific {@link Metadata}. */
    public static ExerciseSessionRecord buildExerciseSession(Metadata metadata) {
        Instant instant = now();
        return new ExerciseSessionRecord.Builder(
                        metadata,
                        sessionStartTime(instant),
                        sessionEndTime(instant),
                        ExerciseSessionType.EXERCISE_SESSION_TYPE_OTHER_WORKOUT)
                .setRoute(buildExerciseRoute())
                .setLaps(
                        List.of(
                                new ExerciseLap.Builder(
                                                sessionStartTime(instant),
                                                sessionStartTime(instant).plusSeconds(20))
                                        .setLength(Length.fromMeters(10))
                                        .build(),
                                new ExerciseLap.Builder(
                                                sessionEndTime(instant).minusSeconds(20),
                                                sessionEndTime(instant))
                                        .build()))
                .setSegments(
                        List.of(
                                new ExerciseSegment.Builder(
                                                sessionStartTime(instant).plusSeconds(1),
                                                sessionStartTime(instant).plusSeconds(10),
                                                ExerciseSegmentType
                                                        .EXERCISE_SEGMENT_TYPE_BENCH_PRESS)
                                        .build(),
                                new ExerciseSegment.Builder(
                                                sessionStartTime(instant).plusSeconds(21),
                                                sessionStartTime(instant).plusSeconds(124),
                                                ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_BURPEE)
                                        .setRepetitionsCount(15)
                                        .build()))
                .setEndZoneOffset(ZoneOffset.MAX)
                .setStartZoneOffset(ZoneOffset.MIN)
                .setNotes("rain")
                .setTitle("Morning training")
                .build();
    }

    public static ExerciseRoute buildExerciseRoute() {
        Instant instant = now();
        return new ExerciseRoute(
                List.of(
                        buildLocationTimePoint(sessionStartTime(instant)),
                        buildLocationTimePoint(sessionStartTime(instant)),
                        buildLocationTimePoint(sessionStartTime(instant))));
    }

    public static ExerciseRoute.Location buildLocationTimePoint(Instant startTime) {
        return new ExerciseRoute.Location.Builder(
                        Instant.ofEpochMilli(
                                (long) (startTime.toEpochMilli() + 10 + Math.random() * 50)),
                        Math.random() * 50,
                        Math.random() * 50)
                .build();
    }

    /** Returns a training plan builder, prepopulated with test data. */
    public static PlannedExerciseSessionRecord.Builder plannedExerciseSession(Metadata metadata) {
        Instant instant = now();
        PlannedExerciseSessionRecord.Builder sessionBuilder =
                new PlannedExerciseSessionRecord.Builder(
                        metadata,
                        ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING,
                        sessionStartTime(instant),
                        sessionEndTime(instant));
        sessionBuilder.setNotes("Some notes");
        sessionBuilder.setTitle("Some training plan");
        sessionBuilder.setStartZoneOffset(ZoneOffset.UTC);
        sessionBuilder.setEndZoneOffset(ZoneOffset.UTC);
        var stepBuilder =
                new PlannedExerciseStep.Builder(
                        ExerciseSegmentType.EXERCISE_SEGMENT_TYPE_BIKING,
                        PlannedExerciseStep.EXERCISE_CATEGORY_ACTIVE,
                        new ExerciseCompletionGoal.DistanceGoal(Length.fromMeters(100)));
        var blockBuilder = new PlannedExerciseBlock.Builder(3).setDescription("Main set");
        blockBuilder.setSteps(List.of(stepBuilder.build()));
        sessionBuilder.setBlocks(List.of(blockBuilder.build()));

        return sessionBuilder;
    }

    public static HeartRateRecord getHeartRateRecord() {
        return getHeartRateRecord(72);
    }

    /** Gets a {@link HeartRateRecord} with an empty {@link Metadata}. */
    public static HeartRateRecord getHeartRateRecordWithEmptyMetadata() {
        return getHeartRateRecord(72, getEmptyMetadata());
    }

    /** Gets a {@link HeartRateRecord} with a specific heart rate and {@link Metadata}. */
    public static HeartRateRecord getHeartRateRecord(int heartRate, Metadata metadata) {
        Instant instant = now();
        HeartRateRecord.HeartRateSample heartRateSample =
                new HeartRateRecord.HeartRateSample(heartRate, instant.plusMillis(10));
        return new HeartRateRecord.Builder(
                        metadata, instant, instant.plusMillis(1000), List.of(heartRateSample))
                .build();
    }

    public static HeartRateRecord getHeartRateRecord(int heartRate, String clientId) {
        return getHeartRateRecord(heartRate, now().plusMillis(100), clientId);
    }

    public static HeartRateRecord getHeartRateRecord(int heartRate) {
        return getHeartRateRecord(heartRate, now().plusMillis(100));
    }

    public static HeartRateRecord getHeartRateRecord(int heartRate, Instant instant) {
        return getHeartRateRecord(heartRate, instant, "HR" + Math.random());
    }

    public static HeartRateRecord getHeartRateRecord(
            List<HeartRateRecord.HeartRateSample> samples, Instant start, Instant end) {
        return new HeartRateRecord.Builder(getEmptyMetadata(), start, end, samples).build();
    }

    public static HeartRateRecord getHeartRateRecord(
            int heartRate, Instant instant, String clientId) {
        String packageName = ApplicationProvider.getApplicationContext().getPackageName();
        HeartRateRecord.HeartRateSample heartRateSample =
                new HeartRateRecord.HeartRateSample(heartRate, instant);
        ArrayList<HeartRateRecord.HeartRateSample> heartRateSamples = new ArrayList<>();
        heartRateSamples.add(heartRateSample);
        heartRateSamples.add(heartRateSample);
        Device device = buildDevice();
        DataOrigin dataOrigin = new DataOrigin.Builder().setPackageName(packageName).build();

        return new HeartRateRecord.Builder(
                        new Metadata.Builder()
                                .setDevice(device)
                                .setDataOrigin(dataOrigin)
                                .setClientRecordId(clientId)
                                .build(),
                        instant.minusMillis(100),
                        instant.plusMillis(100),
                        heartRateSamples)
                .build();
    }

    /** Creates and returns a {@link WeightRecord} with the specified arguments. */
    public static WeightRecord getWeightRecord(double grams, Instant time) {
        return new WeightRecord.Builder(new Metadata.Builder().build(), time, Mass.fromGrams(grams))
                .build();
    }

    public static WeightRecord getWeightRecord(double weight, Instant time, ZoneOffset offset) {
        return new WeightRecord.Builder(getEmptyMetadata(), time, Mass.fromGrams(weight))
                .setZoneOffset(offset)
                .build();
    }

    /** Returns a new weight record with the specified fields. */
    public static WeightRecord getWeightRecord(double grams, Instant time, String clientId) {
        return new WeightRecord.Builder(
                        getMetadataForClientId(clientId), time, Mass.fromGrams(grams))
                .build();
    }

    public static StepsRecord getStepsRecordWithEmptyMetaData() {
        return getStepsRecord(10, getEmptyMetadata());
    }

    public static StepsRecord getStepsRecord() {
        return getStepsRecord(10);
    }

    /** Creates and returns a {@link StepsRecord} with the specified arguments. */
    public static StepsRecord getStepsRecord(long steps) {
        return getStepsRecord(steps, generateMetadata());
    }

    /** Creates and returns a {@link StepsRecord} with the specified arguments. */
    public static StepsRecord getStepsRecord(long steps, String clientId) {
        return getStepsRecord(steps, getMetadataForClientId(clientId));
    }

    /** Creates and returns a {@link StepsRecord} with the specified metadata. */
    public static StepsRecord getStepsRecord(long steps, Metadata metadata) {
        Instant instant = now();
        return new StepsRecord.Builder(metadata, instant, instant.plusMillis(1000), steps).build();
    }

    /** Creates and returns a {@link StepsRecord} with the specified arguments. */
    public static StepsRecord getStepsRecord(long steps, Instant start, Instant end) {
        return new StepsRecord.Builder(getEmptyMetadata(), start, end, steps).build();
    }

    /** Creates and returns a {@link StepsRecord} with the specified arguments. */
    public static StepsRecord getStepsRecord(
            long steps, Instant start, Instant end, String clientId) {
        return new StepsRecord.Builder(getMetadataForClientId(clientId), start, end, steps).build();
    }

    public static StepsRecord getStepsRecord(String id) {
        Instant instant = now();
        return new StepsRecord.Builder(generateMetadata(id), instant, instant.plusMillis(1000), 10)
                .build();
    }

    /** Creates and returns a {@link StepsRecord} with default arguments. */
    public static StepsRecord getCompleteStepsRecord() {
        Instant instant = now();
        return getCompleteStepsRecord(
                instant, instant.plusMillis(1000), /* clientRecordId= */ "SR" + Math.random());
    }

    /** Creates and returns a {@link StepsRecord} with the specified arguments. */
    public static StepsRecord getCompleteStepsRecord(
            Instant startTime, Instant endTime, String clientRecordId) {
        return getCompleteStepsRecord(startTime, endTime, clientRecordId, /* count= */ 10);
    }

    /** Creates and returns a {@link StepsRecord} with the specified arguments. */
    public static StepsRecord getCompleteStepsRecord(
            String id, Instant startTime, Instant endTime, long count) {
        return getCompleteStepsRecord(
                id,
                startTime,
                endTime,
                /* clientRecordId= */ null,
                /* clientRecordVersion= */ 0L,
                count);
    }

    /** Creates and returns a {@link StepsRecord} with the specified arguments. */
    public static StepsRecord getCompleteStepsRecord(
            Instant startTime, Instant endTime, long count) {
        return getCompleteStepsRecord(
                startTime,
                endTime,
                /* clientRecordId= */ null,
                /* clientRecordVersion= */ 0L,
                count);
    }

    /** Creates and returns a {@link StepsRecord} with the specified arguments. */
    public static StepsRecord getCompleteStepsRecord(
            Instant startTime, Instant endTime, String clientRecordId, long count) {
        return getCompleteStepsRecord(
                startTime, endTime, clientRecordId, /* clientRecordVersion= */ 0L, count);
    }

    /** Creates and returns a {@link StepsRecord} with the specified arguments. */
    public static StepsRecord getCompleteStepsRecord(
            Instant startTime,
            Instant endTime,
            String clientRecordId,
            long clientRecordVersion,
            long count) {
        return getCompleteStepsRecord(
                /* id= */ null, startTime, endTime, clientRecordId, clientRecordVersion, count);
    }

    /** Creates and returns a {@link StepsRecord} with the specified arguments. */
    public static StepsRecord getCompleteStepsRecord(
            String id,
            Instant startTime,
            Instant endTime,
            String clientRecordId,
            long clientRecordVersion,
            long count) {
        Device device =
                new Device.Builder()
                        .setManufacturer("google")
                        .setModel("Pixel")
                        .setType(Device.DEVICE_TYPE_WATCH)
                        .build();
        DataOrigin dataOrigin =
                new DataOrigin.Builder().setPackageName("android.healthconnect.cts").build();

        Metadata.Builder testMetadataBuilder = new Metadata.Builder();
        if (id != null) {
            testMetadataBuilder.setId(id);
        }
        testMetadataBuilder.setDevice(device).setDataOrigin(dataOrigin);
        testMetadataBuilder.setClientRecordId(clientRecordId);
        testMetadataBuilder.setClientRecordVersion(clientRecordVersion);
        testMetadataBuilder.setRecordingMethod(RECORDING_METHOD_ACTIVELY_RECORDED);
        Metadata testMetaData = testMetadataBuilder.build();
        assertThat(testMetaData.getRecordingMethod()).isEqualTo(RECORDING_METHOD_ACTIVELY_RECORDED);
        return new StepsRecord.Builder(testMetaData, startTime, endTime, count).build();
    }

    public static StepsRecord getUpdatedStepsRecord(
            Record record, String id, String clientRecordId) {
        Instant instant = now();
        Metadata metadata = record.getMetadata();
        Metadata metadataWithId =
                new Metadata.Builder()
                        .setId(id)
                        .setClientRecordId(clientRecordId)
                        .setClientRecordVersion(metadata.getClientRecordVersion())
                        .setDataOrigin(metadata.getDataOrigin())
                        .setDevice(metadata.getDevice())
                        .setLastModifiedTime(metadata.getLastModifiedTime())
                        .build();
        return new StepsRecord.Builder(metadataWithId, instant, instant.plusMillis(2000), 20)
                .setStartZoneOffset(ZoneOffset.systemDefault().getRules().getOffset(instant))
                .setEndZoneOffset(ZoneOffset.systemDefault().getRules().getOffset(instant))
                .build();
    }

    /** Creates a {@link DistanceRecord}. */
    public static DistanceRecord getDistanceRecord() {
        Instant instant = now();
        return getDistanceRecord(10.0, instant, instant.plusMillis(1000));
    }

    /** Creates a {@link DistanceRecord} with a specified {@code clientId}. */
    public static DistanceRecord getDistanceRecordWithClientId(String clientId) {
        Instant instant = now();
        return getDistanceRecord(
                10,
                instant,
                instant.plusMillis(1000),
                /* startZoneOffset= */ null,
                /* endZoneOffset= */ null,
                generateMetadataWithClientId(clientId));
    }

    /** Create a {@link DistanceRecord} with non empty record ID. */
    public static DistanceRecord getDistanceRecordWithNonEmptyId() {
        Instant instant = now();
        return getDistanceRecord(
                10,
                instant,
                instant.plusMillis(1000),
                /* startZoneOffset= */ null,
                /* endZoneOffset= */ null,
                generateMetadata());
    }

    /** Create a {@link DistanceRecord} with empty {@link Metadata}. */
    public static DistanceRecord getDistanceRecordWithEmptyMetadata() {
        Instant instant = now();
        return getDistanceRecord(
                10,
                instant,
                instant.plusMillis(1000),
                /* startZoneOffset= */ null,
                /* endZoneOffset= */ null,
                getEmptyMetadata());
    }

    /** Create a {@link DistanceRecord} with the specified arguments. */
    public static DistanceRecord getDistanceRecord(double distance, Instant start, Instant end) {
        return getDistanceRecord(
                distance,
                start,
                end,
                /* startZoneOffset= */ null,
                /* endZoneOffset= */ null,
                getEmptyMetadata());
    }

    /** Create a {@link DistanceRecord} with the specified arguments. */
    public static DistanceRecord getDistanceRecord(
            double distance, Instant start, Instant end, ZoneOffset offset) {
        return getDistanceRecord(distance, start, end, offset, offset, getEmptyMetadata());
    }

    /** Create a {@link DistanceRecord} with the specified arguments. */
    public static DistanceRecord getDistanceRecord(
            double distance, Instant start, Instant end, String clientId) {
        return getDistanceRecord(
                distance,
                start,
                end,
                /* startZoneOffset= */ null,
                /* endZoneOffset= */ null,
                getMetadataForClientId(clientId));
    }

    /** Create a {@link DistanceRecord} with the specified arguments. */
    public static DistanceRecord getDistanceRecord(
            double distance,
            Instant start,
            Instant end,
            @Nullable ZoneOffset startZoneOffset,
            @Nullable ZoneOffset endZoneOffset,
            Metadata metadata) {
        DistanceRecord.Builder builder =
                new DistanceRecord.Builder(metadata, start, end, Length.fromMeters(distance));
        if (startZoneOffset != null) {
            builder.setStartZoneOffset(startZoneOffset);
        }
        if (endZoneOffset != null) {
            builder.setEndZoneOffset(endZoneOffset);
        }
        return builder.build();
    }

    /** Gets a {@link TotalCaloriesBurnedRecord} with a specific {@code clientId}. */
    public static TotalCaloriesBurnedRecord getTotalCaloriesBurnedRecord(String clientId) {
        return getTotalCaloriesBurnedRecord(getMetadataForClientId(clientId));
    }

    /** Gets a {@link TotalCaloriesBurnedRecord} with a specific {@link Metadata}. */
    public static TotalCaloriesBurnedRecord getTotalCaloriesBurnedRecord(Metadata metadata) {
        Instant instant = now();
        return new TotalCaloriesBurnedRecord.Builder(
                        metadata, instant, instant.plusMillis(1000), Energy.fromCalories(10.0))
                .build();
    }

    public static TotalCaloriesBurnedRecord getTotalCaloriesBurnedRecordWithEmptyMetadata() {
        return getTotalCaloriesBurnedRecord(getEmptyMetadata());
    }

    public static List<Record> getTestRecords() {
        return Arrays.asList(
                getStepsRecord(),
                getHeartRateRecord(),
                getBasalMetabolicRateRecord(),
                buildExerciseSession());
    }

    public static ChangeLogTokenRequest.Builder getChangeLogTokenRequestForTestRecordTypes() {
        return new ChangeLogTokenRequest.Builder()
                .addRecordType(StepsRecord.class)
                .addRecordType(HeartRateRecord.class)
                .addRecordType(BasalMetabolicRateRecord.class)
                .addRecordType(ExerciseSessionRecord.class);
    }

    public static BasalMetabolicRateRecord getBasalMetabolicRateRecord() {
        return new BasalMetabolicRateRecord.Builder(
                        generateMetadata(), now(), Power.fromWatts(100.0))
                .build();
    }

    /**
     * Returns a {@link ActiveCaloriesBurnedRecord} with given {@code energy} between the given
     * {@code start} and {@code end} time.
     */
    public static ActiveCaloriesBurnedRecord getActiveCaloriesBurnedRecord(
            double energy, Instant start, Instant end) {
        return new ActiveCaloriesBurnedRecord.Builder(
                        getEmptyMetadata(), start, end, Energy.fromCalories(energy))
                .build();
    }

    /**
     * Returns a {@link BasalMetabolicRateRecord} with given {@code power} at the given {@code
     * time}.
     */
    public static BasalMetabolicRateRecord getBasalMetabolicRateRecord(double power, Instant time) {
        return new BasalMetabolicRateRecord.Builder(
                        new Metadata.Builder().build(), time, Power.fromWatts(power))
                .build();
    }

    /**
     * Returns a {@link BasalMetabolicRateRecord} with given {@code power} at the given {@code time}
     * at the given {@code offset}.
     */
    public static BasalMetabolicRateRecord getBasalMetabolicRateRecord(
            double power, Instant time, ZoneOffset offset) {
        return new BasalMetabolicRateRecord.Builder(
                        new Metadata.Builder().build(), time, Power.fromWatts(power))
                .setZoneOffset(offset)
                .build();
    }

    public static LeanBodyMassRecord getBaseLeanBodyMassRecord(Instant time, double grams) {
        return new LeanBodyMassRecord.Builder(getEmptyMetadata(), time, Mass.fromGrams(grams))
                .build();
    }

    public static HeightRecord getHeightRecord() {
        return getBaseHeightRecord(now(), 1.9);
    }

    public static HeightRecord getBaseHeightRecord(Instant time, double heightMeter) {
        return new HeightRecord.Builder(getEmptyMetadata(), time, Length.fromMeters(heightMeter))
                .build();
    }

    public static WeightRecord getBaseWeightRecord(Instant time, double weightKg) {
        return new WeightRecord.Builder(getEmptyMetadata(), time, Mass.fromGrams(weightKg * 1000))
                .build();
    }

    public static BloodPressureRecord getBloodPressureRecord() {
        return new BloodPressureRecord.Builder(
                        getEmptyMetadata(),
                        now(),
                        BloodPressureRecord.BloodPressureMeasurementLocation
                                .BLOOD_PRESSURE_MEASUREMENT_LOCATION_LEFT_WRIST,
                        Pressure.fromMillimetersOfMercury(22.0),
                        Pressure.fromMillimetersOfMercury(24.0),
                        BloodPressureRecord.BodyPosition.BODY_POSITION_STANDING_UP)
                .build();
    }
}
