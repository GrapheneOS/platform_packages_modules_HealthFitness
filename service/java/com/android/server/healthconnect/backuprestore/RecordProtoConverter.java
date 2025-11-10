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

package com.android.server.healthconnect.backuprestore;

import static android.health.connect.Constants.DEFAULT_DOUBLE;
import static android.health.connect.Constants.DEFAULT_FLOAT;
import static android.health.connect.Constants.DEFAULT_INT;
import static android.health.connect.datatypes.units.Temperature.fromCelsius;

import static java.util.stream.Collectors.toSet;

import android.annotation.SuppressLint;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.health.connect.datatypes.units.Energy;
import android.health.connect.datatypes.units.Length;
import android.health.connect.datatypes.units.Mass;
import android.health.connect.datatypes.units.Power;
import android.health.connect.datatypes.units.Velocity;
import android.health.connect.internal.datatypes.ActiveCaloriesBurnedRecordInternal;
import android.health.connect.internal.datatypes.ActivityIntensityRecordInternal;
import android.health.connect.internal.datatypes.AlcoholConsumptionRecordInternal;
import android.health.connect.internal.datatypes.BasalBodyTemperatureRecordInternal;
import android.health.connect.internal.datatypes.BasalMetabolicRateRecordInternal;
import android.health.connect.internal.datatypes.BloodGlucoseRecordInternal;
import android.health.connect.internal.datatypes.BloodPressureRecordInternal;
import android.health.connect.internal.datatypes.BodyFatRecordInternal;
import android.health.connect.internal.datatypes.BodyTemperatureRecordInternal;
import android.health.connect.internal.datatypes.BodyWaterMassRecordInternal;
import android.health.connect.internal.datatypes.BoneMassRecordInternal;
import android.health.connect.internal.datatypes.CervicalMucusRecordInternal;
import android.health.connect.internal.datatypes.CyclePhasesRecordInternal;
import android.health.connect.internal.datatypes.CyclingPedalingCadenceRecordInternal;
import android.health.connect.internal.datatypes.DistanceRecordInternal;
import android.health.connect.internal.datatypes.ElevationGainedRecordInternal;
import android.health.connect.internal.datatypes.ExerciseCompletionGoalInternal;
import android.health.connect.internal.datatypes.ExerciseLapInternal;
import android.health.connect.internal.datatypes.ExercisePerformanceGoalInternal;
import android.health.connect.internal.datatypes.ExerciseRouteInternal;
import android.health.connect.internal.datatypes.ExerciseRouteInternal.LocationInternal;
import android.health.connect.internal.datatypes.ExerciseSegmentInternal;
import android.health.connect.internal.datatypes.ExerciseSessionRecordInternal;
import android.health.connect.internal.datatypes.FloorsClimbedRecordInternal;
import android.health.connect.internal.datatypes.HeartRateRecordInternal;
import android.health.connect.internal.datatypes.HeartRateVariabilityRmssdRecordInternal;
import android.health.connect.internal.datatypes.HeightRecordInternal;
import android.health.connect.internal.datatypes.HydrationRecordInternal;
import android.health.connect.internal.datatypes.InstantRecordInternal;
import android.health.connect.internal.datatypes.IntermenstrualBleedingRecordInternal;
import android.health.connect.internal.datatypes.IntervalRecordInternal;
import android.health.connect.internal.datatypes.LeanBodyMassRecordInternal;
import android.health.connect.internal.datatypes.MenstruationFlowRecordInternal;
import android.health.connect.internal.datatypes.MenstruationPeriodRecordInternal;
import android.health.connect.internal.datatypes.MindfulnessSessionRecordInternal;
import android.health.connect.internal.datatypes.NicotineIntakeRecordInternal;
import android.health.connect.internal.datatypes.NutritionRecordInternal;
import android.health.connect.internal.datatypes.OvulationTestRecordInternal;
import android.health.connect.internal.datatypes.OxygenSaturationRecordInternal;
import android.health.connect.internal.datatypes.PlannedExerciseBlockInternal;
import android.health.connect.internal.datatypes.PlannedExerciseSessionRecordInternal;
import android.health.connect.internal.datatypes.PlannedExerciseStepInternal;
import android.health.connect.internal.datatypes.PowerRecordInternal;
import android.health.connect.internal.datatypes.RecordInternal;
import android.health.connect.internal.datatypes.RespiratoryRateRecordInternal;
import android.health.connect.internal.datatypes.RestingHeartRateRecordInternal;
import android.health.connect.internal.datatypes.SexualActivityRecordInternal;
import android.health.connect.internal.datatypes.SkinTemperatureRecordInternal;
import android.health.connect.internal.datatypes.SleepSessionRecordInternal;
import android.health.connect.internal.datatypes.SleepStageInternal;
import android.health.connect.internal.datatypes.SpeedRecordInternal;
import android.health.connect.internal.datatypes.StepsCadenceRecordInternal;
import android.health.connect.internal.datatypes.StepsRecordInternal;
import android.health.connect.internal.datatypes.SymptomRecordInternal;
import android.health.connect.internal.datatypes.TotalCaloriesBurnedRecordInternal;
import android.health.connect.internal.datatypes.Vo2MaxRecordInternal;
import android.health.connect.internal.datatypes.WeightRecordInternal;
import android.health.connect.internal.datatypes.WheelchairPushesRecordInternal;
import android.health.connect.internal.datatypes.utils.HealthConnectMappings;

import com.android.healthfitness.flags.AconfigFlagHelper;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.ActiveCaloriesBurned;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.ActivityIntensity;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.AlcoholConsumption;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.BasalBodyTemperature;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.BasalMetabolicRate;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.BloodGlucose;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.BloodPressure;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.BodyFat;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.BodyTemperature;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.BodyWaterMass;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.BoneMass;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.CervicalMucus;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.CyclePhases;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.CyclingPedalingCadence;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.CyclingPedalingCadence.CyclingPedalingCadenceSample;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.Distance;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.ElevationGained;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.ExerciseSession;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.ExerciseSession.ExerciseLap;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.ExerciseSession.ExerciseRoute;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.ExerciseSession.ExerciseRoute.Location;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.ExerciseSession.ExerciseSegment;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.FloorsClimbed;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.HeartRate;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.HeartRateVariabilityRmssd;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.Height;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.Hydration;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.InstantRecord;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.IntermenstrualBleeding;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.IntervalRecord;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.LeanBodyMass;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.MenstruationFlow;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.MenstruationPeriod;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.MindfulnessSession;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.NicotineIntake;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.Nutrition;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.OvulationTest;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.OxygenSaturation;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.PlannedExerciseSession;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.PlannedExerciseSession.PlannedExerciseBlock;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.PlannedExerciseSession.PlannedExerciseBlock.PlannedExerciseStep;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.PlannedExerciseSession.PlannedExerciseBlock.PlannedExerciseStep.ExerciseCompletionGoal;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.PlannedExerciseSession.PlannedExerciseBlock.PlannedExerciseStep.ExercisePerformanceGoal;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.Record;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.RespiratoryRate;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.RestingHeartRate;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.SexualActivity;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.SkinTemperature;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.SleepSession;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.SleepSession.SleepStage;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.Speed;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.Speed.SpeedSample;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.Steps;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.StepsCadence;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.Symptoms;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.TotalCaloriesBurned;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.Vo2Max;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.Weight;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.WheelchairPushes;

import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * A helper class used to create {@link RecordInternal} objects using its proto.
 *
 * @hide
 */
public final class RecordProtoConverter {

    public static final int PROTO_VERSION = 4;

    private final Map<Integer, Class<? extends RecordInternal<?>>> mDataTypeClassMap =
            HealthConnectMappings.getInstance().getRecordIdToInternalRecordClassMap();

    /** Creates a {@link RecordInternal} from the {@link Record} */
    public RecordInternal<?> toRecordInternal(Record recordProto)
            throws NoSuchMethodException,
                    InvocationTargetException,
                    InstantiationException,
                    IllegalAccessException {
        return populateRecordInternal(recordProto);
    }

    /** Creates a {@link Record} from the {@link RecordInternal} */
    public Record toRecordProto(RecordInternal<?> recordInternal) {
        Record.Builder builder =
                Record.newBuilder()
                        .setUuid(
                                recordInternal.getUuid() == null
                                        ? ""
                                        : recordInternal.getUuid().toString())
                        .setLastModifiedTime(recordInternal.getLastModifiedTime())
                        .setClientRecordVersion(recordInternal.getClientRecordVersion())
                        .setDeviceType(recordInternal.getDeviceType())
                        .setRecordingMethod(recordInternal.getRecordingMethod());
        if (recordInternal.getPackageName() != null) {
            builder.setPackageName(recordInternal.getPackageName());
        }
        if (recordInternal.getClientRecordId() != null) {
            builder.setClientRecordId(recordInternal.getClientRecordId());
        }
        if (recordInternal.getManufacturer() != null) {
            builder.setManufacturer(recordInternal.getManufacturer());
        }
        if (recordInternal.getModel() != null) {
            builder.setModel(recordInternal.getModel());
        }

        if (recordInternal instanceof IntervalRecordInternal<?> intervalRecordInternal) {
            builder.setIntervalRecord(toIntervalRecordProto(intervalRecordInternal));
        } else if (recordInternal instanceof InstantRecordInternal<?> instantRecordInternal) {
            builder.setInstantRecord(toInstantRecordProto(instantRecordInternal));
        } else {
            throw new IllegalArgumentException(
                    "Unknown record type " + recordInternal.getClass().getSimpleName());
        }

        return builder.build();
    }

    private IntervalRecord toIntervalRecordProto(IntervalRecordInternal<?> intervalRecordInternal) {
        IntervalRecord.Builder builder =
                IntervalRecord.newBuilder()
                        .setStartTime(intervalRecordInternal.getStartTimeInMillis())
                        .setStartZoneOffset(intervalRecordInternal.getStartZoneOffsetInSeconds())
                        .setEndTime(intervalRecordInternal.getEndTimeInMillis())
                        .setEndZoneOffset(intervalRecordInternal.getEndZoneOffsetInSeconds());

        if (intervalRecordInternal
                instanceof ActiveCaloriesBurnedRecordInternal activeCaloriesBurnedRecordInternal) {
            builder.setActiveCaloriesBurned(
                    toActiveCaloriesBurnedProto(activeCaloriesBurnedRecordInternal));
        } else if (intervalRecordInternal
                instanceof ActivityIntensityRecordInternal activityIntensityRecordInternal) {
            builder.setActivityIntensity(toActivityIntensityProto(activityIntensityRecordInternal));
        } else if (intervalRecordInternal
                instanceof AlcoholConsumptionRecordInternal alcoholConsumptionRecordInternal) {
            builder.setAlcoholConsumption(
                    toAlcoholConsumptionProto(alcoholConsumptionRecordInternal));
        } else if (intervalRecordInternal
                instanceof CyclePhasesRecordInternal cyclePhasesRecordInternal) {
            builder.setCyclePhases(toCyclePhasesProto(cyclePhasesRecordInternal));
        } else if (intervalRecordInternal
                instanceof
                CyclingPedalingCadenceRecordInternal cyclingPedalingCadenceRecordInternal) {
            builder.setCyclingPedalingCadence(
                    toCyclingPedalingCadenceProto(cyclingPedalingCadenceRecordInternal));
        } else if (intervalRecordInternal
                instanceof DistanceRecordInternal distanceRecordInternal) {
            builder.setDistance(toDistanceProto(distanceRecordInternal));
        } else if (intervalRecordInternal
                instanceof ElevationGainedRecordInternal elevationGainedRecordInternal) {
            builder.setElevationGained(toElevationGainedProto(elevationGainedRecordInternal));
        } else if (intervalRecordInternal
                instanceof ExerciseSessionRecordInternal exerciseSessionRecordInternal) {
            builder.setExerciseSession(toExerciseSessionProto(exerciseSessionRecordInternal));
        } else if (intervalRecordInternal
                instanceof FloorsClimbedRecordInternal floorsClimbedRecordInternal) {
            builder.setFloorsClimbed(toFloorsClimbedProto(floorsClimbedRecordInternal));
        } else if (intervalRecordInternal
                instanceof HeartRateRecordInternal heartRateRecordInternal) {
            builder.setHeartRate(toHeartRateProto(heartRateRecordInternal));
        } else if (intervalRecordInternal
                instanceof HydrationRecordInternal hydrationRecordInternal) {
            builder.setHydration(toHydrationProto(hydrationRecordInternal));
        } else if (intervalRecordInternal instanceof MenstruationPeriodRecordInternal) {
            builder.setMenstruationPeriod(MenstruationPeriod.getDefaultInstance());
        } else if (intervalRecordInternal
                instanceof MindfulnessSessionRecordInternal mindfulnessSessionRecordInternal) {
            builder.setMindfulnessSession(
                    toMindfulnessSessionProto(mindfulnessSessionRecordInternal));
        } else if (intervalRecordInternal
                instanceof NicotineIntakeRecordInternal nicotineIntakeRecordInternal) {
            builder.setNicotineIntake(toNicotineIntakeProto(nicotineIntakeRecordInternal));
        } else if (intervalRecordInternal
                instanceof NutritionRecordInternal nutritionRecordInternal) {
            builder.setNutrition(toNutritionProto(nutritionRecordInternal));
        } else if (intervalRecordInternal
                instanceof
                PlannedExerciseSessionRecordInternal plannedExerciseSessionRecordInternal) {
            builder.setPlannedExerciseSession(
                    toPlannedExerciseSessionProto(plannedExerciseSessionRecordInternal));
        } else if (intervalRecordInternal instanceof PowerRecordInternal powerRecordInternal) {
            builder.setPower(toPowerProto(powerRecordInternal));
        } else if (intervalRecordInternal
                instanceof SkinTemperatureRecordInternal skinTemperatureRecordInternal) {
            builder.setSkinTemperature(toSkinTemperatureProto(skinTemperatureRecordInternal));
        } else if (intervalRecordInternal
                instanceof SleepSessionRecordInternal sleepSessionRecordInternal) {
            builder.setSleepSession(toSleepSessionProto(sleepSessionRecordInternal));
        } else if (intervalRecordInternal instanceof SpeedRecordInternal speedRecordInternal) {
            builder.setSpeed(toSpeedProto(speedRecordInternal));
        } else if (intervalRecordInternal instanceof StepsRecordInternal stepsRecordInternal) {
            builder.setSteps(toStepsProto(stepsRecordInternal));
        } else if (intervalRecordInternal
                instanceof StepsCadenceRecordInternal stepsCadenceRecordInternal) {
            builder.setStepsCadence(toStepsCadenceProto(stepsCadenceRecordInternal));
        } else if (intervalRecordInternal
                instanceof TotalCaloriesBurnedRecordInternal totalCaloriesBurnedRecordInternal) {
            builder.setTotalCaloriesBurned(
                    toTotalCaloriesBurnedProto(totalCaloriesBurnedRecordInternal));
        } else if (intervalRecordInternal
                instanceof WheelchairPushesRecordInternal wheelchairPushesRecordInternal) {
            builder.setWheelchairPushes(toWheelchairPushesProto(wheelchairPushesRecordInternal));
        } else if (intervalRecordInternal instanceof SymptomRecordInternal symptomRecordInternal) {
            builder.setSymptoms(toSymptomsProto(symptomRecordInternal));
        } else {
            throw new IllegalArgumentException(
                    "Unknown interval record type "
                            + intervalRecordInternal.getClass().getSimpleName());
        }

        return builder.build();
    }

    private static ActiveCaloriesBurned toActiveCaloriesBurnedProto(
            ActiveCaloriesBurnedRecordInternal activeCaloriesBurnedRecordInternal) {
        return ActiveCaloriesBurned.newBuilder()
                .setEnergy(activeCaloriesBurnedRecordInternal.getEnergy())
                .build();
    }

    private static ActivityIntensity toActivityIntensityProto(
            ActivityIntensityRecordInternal activityIntensityRecordInternal) {
        return ActivityIntensity.newBuilder()
                .setActivityIntensityType(
                        activityIntensityRecordInternal.getActivityIntensityType())
                .build();
    }

    private static CyclingPedalingCadence toCyclingPedalingCadenceProto(
            CyclingPedalingCadenceRecordInternal cyclingPedalingCadenceRecordInternal) {
        List<CyclingPedalingCadenceSample> samples =
                cyclingPedalingCadenceRecordInternal.getSamples().stream()
                        .map(
                                sample ->
                                        CyclingPedalingCadenceSample.newBuilder()
                                                .setRevolutionsPerMinute(
                                                        sample.getRevolutionsPerMinute())
                                                .setEpochMillis(sample.getEpochMillis())
                                                .build())
                        .toList();

        return CyclingPedalingCadence.newBuilder().addAllSample(samples).build();
    }

    private static Distance toDistanceProto(DistanceRecordInternal distanceRecordInternal) {
        return Distance.newBuilder().setDistance(distanceRecordInternal.getDistance()).build();
    }

    private static ElevationGained toElevationGainedProto(
            ElevationGainedRecordInternal elevationGainedRecordInternal) {
        return ElevationGained.newBuilder()
                .setElevation(elevationGainedRecordInternal.getElevation())
                .build();
    }

    private static ExerciseSession toExerciseSessionProto(
            ExerciseSessionRecordInternal exerciseSessionRecordInternal) {
        ExerciseSession.Builder builder =
                ExerciseSession.newBuilder()
                        .setExerciseType(exerciseSessionRecordInternal.getExerciseType())
                        .setHasRoute(exerciseSessionRecordInternal.hasRoute());

        if (exerciseSessionRecordInternal.getNotes() != null) {
            builder.setNotes(exerciseSessionRecordInternal.getNotes());
        }
        if (exerciseSessionRecordInternal.getTitle() != null) {
            builder.setTitle(exerciseSessionRecordInternal.getTitle());
        }
        if (exerciseSessionRecordInternal.getRoute() != null) {
            List<Location> routeLocations =
                    exerciseSessionRecordInternal.getRoute().getRouteLocations().stream()
                            .map(RecordProtoConverter::toLocationProto)
                            .toList();
            builder.setRoute(ExerciseRoute.newBuilder().addAllRouteLocation(routeLocations));
        }
        if (exerciseSessionRecordInternal.getLaps() != null) {
            builder.addAllLap(
                    exerciseSessionRecordInternal.getLaps().stream()
                            .map(RecordProtoConverter::toLapProto)
                            .toList());
        }
        if (exerciseSessionRecordInternal.getSegments() != null) {
            builder.addAllSegment(
                    exerciseSessionRecordInternal.getSegments().stream()
                            .map(RecordProtoConverter::toSegmentProto)
                            .toList());
        }
        if (exerciseSessionRecordInternal.getPlannedExerciseSessionId() != null) {
            builder.setPlannedExerciseSessionId(
                    exerciseSessionRecordInternal.getPlannedExerciseSessionId().toString());
        }

        if (AconfigFlagHelper.isExerciseSegmentImprovementsEnabled()
                && exerciseSessionRecordInternal.getRateOfPerceivedExertion() != DEFAULT_FLOAT) {
            builder.setSessionRateOfPerceivedExertion(
                    exerciseSessionRecordInternal.getRateOfPerceivedExertion());
        }

        return builder.build();
    }

    private static Location toLocationProto(LocationInternal locationInternal) {
        return Location.newBuilder()
                .setTime(locationInternal.getTime())
                .setLatitude(locationInternal.getLatitude())
                .setLongitude(locationInternal.getLongitude())
                .setHorizontalAccuracy(locationInternal.getHorizontalAccuracy())
                .setVerticalAccuracy(locationInternal.getVerticalAccuracy())
                .setAltitude(locationInternal.getAltitude())
                .build();
    }

    private static ExerciseLap toLapProto(ExerciseLapInternal lapInternal) {
        return ExerciseLap.newBuilder()
                .setStartTime(lapInternal.getStartTime())
                .setEndTime(lapInternal.getEndTime())
                .setLength(lapInternal.getLength())
                .build();
    }

    private static ExerciseSegment toSegmentProto(ExerciseSegmentInternal segmentInternal) {

        ExerciseSegment.Builder builder = ExerciseSegment.newBuilder();
        builder.setStartTime(segmentInternal.getStartTime())
                .setEndTime(segmentInternal.getEndTime())
                .setSegmentType(segmentInternal.getSegmentType())
                .setRepetitionsCount(segmentInternal.getRepetitionsCount());
        if (AconfigFlagHelper.isExerciseSegmentImprovementsEnabled()) {
            if (segmentInternal.getWeightGrams() != null) {
                builder.setWeight(segmentInternal.getWeightGrams());
            }
            if (segmentInternal.getSetIndex() != DEFAULT_INT) {
                builder.setSetIndex(segmentInternal.getSetIndex());
            }
            if (segmentInternal.getRateOfPerceivedExertion() != DEFAULT_FLOAT) {
                builder.setRateOfPerceivedExertion(segmentInternal.getRateOfPerceivedExertion());
            }
        }
        return builder.build();
    }

    private static FloorsClimbed toFloorsClimbedProto(
            FloorsClimbedRecordInternal floorsClimbedRecordInternal) {
        return FloorsClimbed.newBuilder()
                .setFloors(floorsClimbedRecordInternal.getFloors())
                .build();
    }

    private static HeartRate toHeartRateProto(HeartRateRecordInternal heartRateRecordInternal) {
        List<HeartRate.HeartRateSample> samples =
                heartRateRecordInternal.getSamples().stream()
                        .map(
                                sample ->
                                        HeartRate.HeartRateSample.newBuilder()
                                                .setBeatsPerMinute(sample.getBeatsPerMinute())
                                                .setEpochMillis(sample.getEpochMillis())
                                                .build())
                        .toList();

        return HeartRate.newBuilder().addAllSample(samples).build();
    }

    private static Hydration toHydrationProto(HydrationRecordInternal hydrationRecordInternal) {
        return Hydration.newBuilder().setVolume(hydrationRecordInternal.getVolume()).build();
    }

    private static MindfulnessSession toMindfulnessSessionProto(
            MindfulnessSessionRecordInternal mindfulnessSessionRecordInternal) {
        MindfulnessSession.Builder builder =
                MindfulnessSession.newBuilder()
                        .setMindfulnessSessionType(
                                mindfulnessSessionRecordInternal.getMindfulnessSessionType());
        if (mindfulnessSessionRecordInternal.getTitle() != null) {
            builder.setTitle(mindfulnessSessionRecordInternal.getTitle());
        }
        if (mindfulnessSessionRecordInternal.getNotes() != null) {
            builder.setNotes(mindfulnessSessionRecordInternal.getNotes());
        }

        return builder.build();
    }

    private static NicotineIntake toNicotineIntakeProto(
            NicotineIntakeRecordInternal nicotineIntakeRecordInternal) {
        NicotineIntake.Builder builder =
                NicotineIntake.newBuilder()
                        .setNicotineIntakeType(nicotineIntakeRecordInternal.getNicotineIntakeType())
                        .setQuantity(nicotineIntakeRecordInternal.getQuantity());
        if (nicotineIntakeRecordInternal.getNicotineIntakeGrams() != DEFAULT_DOUBLE) {
            builder.setNicotineIntake(nicotineIntakeRecordInternal.getNicotineIntakeGrams());
        }

        return builder.build();
    }

    private static Nutrition toNutritionProto(NutritionRecordInternal nutritionRecordInternal) {
        Nutrition.Builder builder =
                Nutrition.newBuilder()
                        .setUnsaturatedFat(nutritionRecordInternal.getUnsaturatedFat())
                        .setPotassium(nutritionRecordInternal.getPotassium())
                        .setThiamin(nutritionRecordInternal.getThiamin())
                        .setMealType(nutritionRecordInternal.getMealType())
                        .setTransFat(nutritionRecordInternal.getTransFat())
                        .setManganese(nutritionRecordInternal.getManganese())
                        .setEnergyFromFat(nutritionRecordInternal.getEnergyFromFat())
                        .setCaffeine(nutritionRecordInternal.getCaffeine())
                        .setDietaryFiber(nutritionRecordInternal.getDietaryFiber())
                        .setSelenium(nutritionRecordInternal.getSelenium())
                        .setVitaminB6(nutritionRecordInternal.getVitaminB6())
                        .setProtein(nutritionRecordInternal.getProtein())
                        .setChloride(nutritionRecordInternal.getChloride())
                        .setCholesterol(nutritionRecordInternal.getCholesterol())
                        .setCopper(nutritionRecordInternal.getCopper())
                        .setIodine(nutritionRecordInternal.getIodine())
                        .setVitaminB12(nutritionRecordInternal.getVitaminB12())
                        .setZinc(nutritionRecordInternal.getZinc())
                        .setRiboflavin(nutritionRecordInternal.getRiboflavin())
                        .setEnergy(nutritionRecordInternal.getEnergy())
                        .setMolybdenum(nutritionRecordInternal.getMolybdenum())
                        .setPhosphorus(nutritionRecordInternal.getPhosphorus())
                        .setChromium(nutritionRecordInternal.getChromium())
                        .setTotalFat(nutritionRecordInternal.getTotalFat())
                        .setCalcium(nutritionRecordInternal.getCalcium())
                        .setVitaminC(nutritionRecordInternal.getVitaminC())
                        .setVitaminE(nutritionRecordInternal.getVitaminE())
                        .setBiotin(nutritionRecordInternal.getBiotin())
                        .setVitaminD(nutritionRecordInternal.getVitaminD())
                        .setNiacin(nutritionRecordInternal.getNiacin())
                        .setMagnesium(nutritionRecordInternal.getMagnesium())
                        .setTotalCarbohydrate(nutritionRecordInternal.getTotalCarbohydrate())
                        .setVitaminK(nutritionRecordInternal.getVitaminK())
                        .setPolyunsaturatedFat(nutritionRecordInternal.getPolyunsaturatedFat())
                        .setSaturatedFat(nutritionRecordInternal.getSaturatedFat())
                        .setSodium(nutritionRecordInternal.getSodium())
                        .setFolate(nutritionRecordInternal.getFolate())
                        .setMonounsaturatedFat(nutritionRecordInternal.getMonounsaturatedFat())
                        .setPantothenicAcid(nutritionRecordInternal.getPantothenicAcid())
                        .setIron(nutritionRecordInternal.getIron())
                        .setVitaminA(nutritionRecordInternal.getVitaminA())
                        .setFolicAcid(nutritionRecordInternal.getFolicAcid())
                        .setSugar(nutritionRecordInternal.getSugar());

        if (nutritionRecordInternal.getMealName() != null) {
            builder.setMealName(nutritionRecordInternal.getMealName());
        }

        return builder.build();
    }

    private static PlannedExerciseSession toPlannedExerciseSessionProto(
            PlannedExerciseSessionRecordInternal plannedExerciseSessionRecordInternal) {
        PlannedExerciseSession.Builder builder =
                PlannedExerciseSession.newBuilder()
                        .setExerciseType(plannedExerciseSessionRecordInternal.getExerciseType())
                        .setHasExplicitTime(
                                plannedExerciseSessionRecordInternal.getHasExplicitTime())
                        .addAllExerciseBlock(
                                plannedExerciseSessionRecordInternal.getExerciseBlocks().stream()
                                        .map(RecordProtoConverter::toPlannedExerciseBlockProto)
                                        .toList());
        if (plannedExerciseSessionRecordInternal.getNotes() != null) {
            builder.setNotes(plannedExerciseSessionRecordInternal.getNotes());
        }
        if (plannedExerciseSessionRecordInternal.getTitle() != null) {
            builder.setTitle(plannedExerciseSessionRecordInternal.getTitle());
        }

        return builder.build();
    }

    private static PlannedExerciseBlock toPlannedExerciseBlockProto(
            PlannedExerciseBlockInternal plannedExerciseBlockInternal) {
        PlannedExerciseBlock.Builder builder =
                PlannedExerciseBlock.newBuilder()
                        .setRepetitions(plannedExerciseBlockInternal.getRepetitions())
                        .addAllStep(
                                plannedExerciseBlockInternal.getExerciseSteps().stream()
                                        .map(RecordProtoConverter::toPlannedExerciseStepProto)
                                        .toList());
        if (plannedExerciseBlockInternal.getDescription() != null) {
            builder.setDescription(plannedExerciseBlockInternal.getDescription());
        }

        return builder.build();
    }

    private static PlannedExerciseStep toPlannedExerciseStepProto(
            PlannedExerciseStepInternal plannedExerciseStepInternal) {
        PlannedExerciseStep.Builder builder =
                PlannedExerciseStep.newBuilder()
                        .setExerciseType(plannedExerciseStepInternal.getExerciseType())
                        .setExerciseCategory(plannedExerciseStepInternal.getExerciseCategory());
        if (plannedExerciseStepInternal.getDescription() != null) {
            builder.setDescription(plannedExerciseStepInternal.getDescription());
        }
        if (plannedExerciseStepInternal.getCompletionGoal() != null) {
            builder.setCompletionGoal(
                    toCompletionGoalProto(plannedExerciseStepInternal.getCompletionGoal()));
        }
        if (plannedExerciseStepInternal.getPerformanceGoals() != null) {
            builder.addAllPerformanceGoal(
                    plannedExerciseStepInternal.getPerformanceGoals().stream()
                            .map(RecordProtoConverter::toPerformanceGoalProto)
                            .toList());
        }

        return builder.build();
    }

    private static ExercisePerformanceGoal toPerformanceGoalProto(
            ExercisePerformanceGoalInternal performanceGoalInternal) {
        if (performanceGoalInternal
                instanceof ExercisePerformanceGoalInternal.PowerGoalInternal powerGoalInternal) {
            return ExercisePerformanceGoal.newBuilder()
                    .setPowerGoal(
                            ExercisePerformanceGoal.PowerGoal.newBuilder()
                                    .setMinPower(powerGoalInternal.getMinPower().getInWatts())
                                    .setMaxPower(powerGoalInternal.getMaxPower().getInWatts()))
                    .build();
        } else if (performanceGoalInternal
                instanceof ExercisePerformanceGoalInternal.SpeedGoalInternal speedGoalInternal) {
            return ExercisePerformanceGoal.newBuilder()
                    .setSpeedGoal(
                            ExercisePerformanceGoal.SpeedGoal.newBuilder()
                                    .setMinSpeed(
                                            speedGoalInternal.getMinSpeed().getInMetersPerSecond())
                                    .setMaxSpeed(
                                            speedGoalInternal.getMaxSpeed().getInMetersPerSecond()))
                    .build();
        } else if (performanceGoalInternal
                instanceof
                ExercisePerformanceGoalInternal.CadenceGoalInternal cadenceGoalInternal) {
            return ExercisePerformanceGoal.newBuilder()
                    .setCadenceGoal(
                            ExercisePerformanceGoal.CadenceGoal.newBuilder()
                                    .setMinRpm(cadenceGoalInternal.getMinRpm())
                                    .setMaxRpm(cadenceGoalInternal.getMaxRpm()))
                    .build();
        } else if (performanceGoalInternal
                instanceof
                ExercisePerformanceGoalInternal.HeartRateGoalInternal heartRateGoalInternal) {
            return ExercisePerformanceGoal.newBuilder()
                    .setHeartRateGoal(
                            ExercisePerformanceGoal.HeartRateGoal.newBuilder()
                                    .setMinBpm(heartRateGoalInternal.getMinBpm())
                                    .setMaxBpm(heartRateGoalInternal.getMaxBpm()))
                    .build();
        } else if (performanceGoalInternal
                instanceof ExercisePerformanceGoalInternal.WeightGoalInternal weightGoalInternal) {
            return ExercisePerformanceGoal.newBuilder()
                    .setWeightGoal(
                            ExercisePerformanceGoal.WeightGoal.newBuilder()
                                    .setMass(weightGoalInternal.getMass().getInGrams()))
                    .build();
        } else if (performanceGoalInternal
                instanceof
                ExercisePerformanceGoalInternal.RateOfPerceivedExertionGoalInternal
                        rateOfPerceivedExertionGoalInternal) {
            return ExercisePerformanceGoal.newBuilder()
                    .setRateOfPerceivedExertionGoal(
                            ExercisePerformanceGoal.RateOfPerceivedExertionGoal.newBuilder()
                                    .setRpe(rateOfPerceivedExertionGoalInternal.getRpe()))
                    .build();
        } else if (performanceGoalInternal
                instanceof ExercisePerformanceGoalInternal.AmrapGoalInternal) {
            return ExercisePerformanceGoal.newBuilder()
                    .setAmrapGoal(ExercisePerformanceGoal.AmrapGoal.getDefaultInstance())
                    .build();
        } else if (performanceGoalInternal
                instanceof ExercisePerformanceGoalInternal.UnknownGoalInternal) {
            return ExercisePerformanceGoal.newBuilder()
                    .setUnknownGoal(ExercisePerformanceGoal.UnknownGoal.getDefaultInstance())
                    .build();
        } else {
            throw new IllegalArgumentException(
                    "Unknown performance goal type "
                            + performanceGoalInternal.getClass().getSimpleName());
        }
    }

    private static ExerciseCompletionGoal toCompletionGoalProto(
            ExerciseCompletionGoalInternal completionGoalInternal) {
        if (completionGoalInternal
                instanceof
                ExerciseCompletionGoalInternal.DistanceGoalInternal distanceGoalInternal) {
            return ExerciseCompletionGoal.newBuilder()
                    .setDistanceGoal(
                            ExerciseCompletionGoal.DistanceGoal.newBuilder()
                                    .setDistance(distanceGoalInternal.getDistance().getInMeters()))
                    .build();
        } else if (completionGoalInternal
                instanceof ExerciseCompletionGoalInternal.StepsGoalInternal stepsGoalInternal) {
            return ExerciseCompletionGoal.newBuilder()
                    .setStepsGoal(
                            ExerciseCompletionGoal.StepsGoal.newBuilder()
                                    .setSteps(stepsGoalInternal.getSteps()))
                    .build();
        } else if (completionGoalInternal
                instanceof
                ExerciseCompletionGoalInternal.DurationGoalInternal durationGoalInternal) {
            return ExerciseCompletionGoal.newBuilder()
                    .setDurationGoal(
                            ExerciseCompletionGoal.DurationGoal.newBuilder()
                                    .setDuration(durationGoalInternal.getDuration().toMillis()))
                    .build();
        } else if (completionGoalInternal
                instanceof
                ExerciseCompletionGoalInternal.RepetitionsGoalInternal repetitionsGoalInternal) {
            return ExerciseCompletionGoal.newBuilder()
                    .setRepetitionsGoal(
                            ExerciseCompletionGoal.RepetitionsGoal.newBuilder()
                                    .setRepetitions(repetitionsGoalInternal.getReps()))
                    .build();
        } else if (completionGoalInternal
                instanceof
                ExerciseCompletionGoalInternal.TotalCaloriesBurnedGoalInternal
                        totalCaloriesBurnedGoalInternal) {
            return ExerciseCompletionGoal.newBuilder()
                    .setTotalCaloriesBurnedGoal(
                            ExerciseCompletionGoal.TotalCaloriesBurnedGoal.newBuilder()
                                    .setTotalCalories(
                                            totalCaloriesBurnedGoalInternal
                                                    .getTotalCalories()
                                                    .getInCalories()))
                    .build();
        } else if (completionGoalInternal
                instanceof
                ExerciseCompletionGoalInternal.ActiveCaloriesBurnedGoalInternal
                        activeCaloriesBurnedGoalInternal) {
            return ExerciseCompletionGoal.newBuilder()
                    .setActiveCaloriesBurnedGoal(
                            ExerciseCompletionGoal.ActiveCaloriesBurnedGoal.newBuilder()
                                    .setActiveCalories(
                                            activeCaloriesBurnedGoalInternal
                                                    .getActiveCalories()
                                                    .getInCalories()))
                    .build();
        } else if (completionGoalInternal
                instanceof
                ExerciseCompletionGoalInternal.DistanceWithVariableRestGoalInternal
                        distanceWithVariableRestGoalInternal) {
            return ExerciseCompletionGoal.newBuilder()
                    .setDistanceWithVariableRestGoal(
                            ExerciseCompletionGoal.DistanceWithVariableRestGoal.newBuilder()
                                    .setDistance(
                                            distanceWithVariableRestGoalInternal
                                                    .getDistance()
                                                    .getInMeters())
                                    .setDuration(
                                            distanceWithVariableRestGoalInternal
                                                    .getDuration()
                                                    .toMillis()))
                    .build();
        } else if (completionGoalInternal
                instanceof ExerciseCompletionGoalInternal.UnspecifiedGoalInternal) {
            return ExerciseCompletionGoal.newBuilder()
                    .setUnspecifiedGoal(ExerciseCompletionGoal.UnspecifiedGoal.getDefaultInstance())
                    .build();
        } else if (completionGoalInternal
                instanceof ExerciseCompletionGoalInternal.UnknownGoalInternal) {
            return ExerciseCompletionGoal.newBuilder()
                    .setUnknownGoal(ExerciseCompletionGoal.UnknownGoal.getDefaultInstance())
                    .build();
        } else {
            throw new IllegalArgumentException(
                    "Unknown completion goal type "
                            + completionGoalInternal.getClass().getSimpleName());
        }
    }

    private static com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.Power
            toPowerProto(PowerRecordInternal powerRecordInternal) {
        List<
                        com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto
                                .Power.PowerSample>
                samples =
                        powerRecordInternal.getSamples().stream()
                                .map(
                                        sample ->
                                                com.android.server.healthconnect.proto.backuprestore
                                                        .BackupRestoreProto.Power.PowerSample
                                                        .newBuilder()
                                                        .setPower(sample.getPower())
                                                        .setEpochMillis(sample.getEpochMillis())
                                                        .build())
                                .toList();

        return com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.Power
                .newBuilder()
                .addAllSample(samples)
                .build();
    }

    private static SkinTemperature toSkinTemperatureProto(
            SkinTemperatureRecordInternal skinTemperatureRecordInternal) {
        List<SkinTemperature.SkinTemperatureDeltaSample> samples =
                skinTemperatureRecordInternal.getSamples().stream()
                        .map(
                                sample ->
                                        SkinTemperature.SkinTemperatureDeltaSample.newBuilder()
                                                .setTemperatureDeltaInCelsius(
                                                        sample.mTemperatureDeltaInCelsius())
                                                .setEpochMillis(sample.mEpochMillis())
                                                .build())
                        .toList();

        return SkinTemperature.newBuilder()
                .setMeasurementLocation(skinTemperatureRecordInternal.getMeasurementLocation())
                .setBaseline(skinTemperatureRecordInternal.getBaseline().getInCelsius())
                .addAllSample(samples)
                .build();
    }

    private static SleepSession toSleepSessionProto(
            SleepSessionRecordInternal sleepSessionRecordInternal) {
        SleepSession.Builder builder = SleepSession.newBuilder();

        if (sleepSessionRecordInternal.getNotes() != null) {
            builder.setNotes(sleepSessionRecordInternal.getNotes());
        }
        if (sleepSessionRecordInternal.getTitle() != null) {
            builder.setTitle(sleepSessionRecordInternal.getTitle());
        }
        if (sleepSessionRecordInternal.getSleepStages() != null) {
            List<SleepStage> stages =
                    sleepSessionRecordInternal.getSleepStages().stream()
                            .map(RecordProtoConverter::toSleepStageProto)
                            .toList();
            builder.addAllStage(stages);
        }

        return builder.build();
    }

    private static SleepStage toSleepStageProto(SleepStageInternal sleepStageInternal) {
        return SleepStage.newBuilder()
                .setStartTime(sleepStageInternal.getStartTime())
                .setEndTime(sleepStageInternal.getEndTime())
                .setStageType(sleepStageInternal.getStageType())
                .build();
    }

    private static Speed toSpeedProto(SpeedRecordInternal speedRecordInternal) {
        List<SpeedSample> samples =
                speedRecordInternal.getSamples().stream()
                        .map(
                                sample ->
                                        SpeedSample.newBuilder()
                                                .setSpeed(sample.getSpeed())
                                                .setEpochMillis(sample.getEpochMillis())
                                                .build())
                        .toList();

        return Speed.newBuilder().addAllSample(samples).build();
    }

    private static Steps toStepsProto(StepsRecordInternal stepsRecordInternal) {
        return Steps.newBuilder().setCount(stepsRecordInternal.getCount()).build();
    }

    private static StepsCadence toStepsCadenceProto(
            StepsCadenceRecordInternal stepsCadenceRecordInternal) {
        List<StepsCadence.StepsCadenceSample> samples =
                stepsCadenceRecordInternal.getSamples().stream()
                        .map(
                                sample ->
                                        StepsCadence.StepsCadenceSample.newBuilder()
                                                .setRate(sample.getRate())
                                                .setEpochMillis(sample.getEpochMillis())
                                                .build())
                        .toList();

        return StepsCadence.newBuilder().addAllSample(samples).build();
    }

    private static TotalCaloriesBurned toTotalCaloriesBurnedProto(
            TotalCaloriesBurnedRecordInternal totalCaloriesBurnedRecordInternal) {
        return TotalCaloriesBurned.newBuilder()
                .setEnergy(totalCaloriesBurnedRecordInternal.getEnergy())
                .build();
    }

    private static WheelchairPushes toWheelchairPushesProto(
            WheelchairPushesRecordInternal wheelchairPushesRecordInternal) {
        return WheelchairPushes.newBuilder()
                .setCount((int) wheelchairPushesRecordInternal.getCount())
                .build();
    }

    private static Symptoms toSymptomsProto(SymptomRecordInternal symptomRecordInternal) {
        Symptoms.Builder builder =
                Symptoms.newBuilder()
                        .setSymptomType(symptomRecordInternal.getSymptomType())
                        .setSeverity(symptomRecordInternal.getSeverity())
                        .setCount(symptomRecordInternal.getCount())
                        .setTemporalType(symptomRecordInternal.getTemporalType());
        if (symptomRecordInternal.getNotes() != null) {
            builder.setNotes(symptomRecordInternal.getNotes());
        }
        return builder.build();
    }

    private InstantRecord toInstantRecordProto(InstantRecordInternal<?> instantRecordInternal) {
        InstantRecord.Builder builder =
                InstantRecord.newBuilder()
                        .setTime(instantRecordInternal.getTimeInMillis())
                        .setZoneOffset(instantRecordInternal.getZoneOffsetInSeconds());

        if (instantRecordInternal
                instanceof BasalBodyTemperatureRecordInternal basalBodyTemperatureRecordInternal) {
            builder.setBasalBodyTemperature(
                    toBasalBodyTemperatureProto(basalBodyTemperatureRecordInternal));
        } else if (instantRecordInternal
                instanceof BasalMetabolicRateRecordInternal basalMetabolicRateRecordInternal) {
            builder.setBasalMetabolicRate(
                    toBasalMetabolicRateProto(basalMetabolicRateRecordInternal));
        } else if (instantRecordInternal
                instanceof BloodGlucoseRecordInternal bloodGlucoseRecordInternal) {
            builder.setBloodGlucose(toBloodGlucoseProto(bloodGlucoseRecordInternal));
        } else if (instantRecordInternal
                instanceof BloodPressureRecordInternal bloodPressureRecordInternal) {
            builder.setBloodPressure(toBloodPressureProto(bloodPressureRecordInternal));
        } else if (instantRecordInternal instanceof BodyFatRecordInternal bodyFatRecordInternal) {
            builder.setBodyFat(toBodyFatProto(bodyFatRecordInternal));
        } else if (instantRecordInternal
                instanceof BodyTemperatureRecordInternal bodyTemperatureRecordInternal) {
            builder.setBodyTemperature(toBodyTemperatureProto(bodyTemperatureRecordInternal));
        } else if (instantRecordInternal
                instanceof BodyWaterMassRecordInternal bodyWaterMassRecordInternal) {
            builder.setBodyWaterMass(toBodyWaterMassProto(bodyWaterMassRecordInternal));
        } else if (instantRecordInternal instanceof BoneMassRecordInternal boneMassRecordInternal) {
            builder.setBoneMass(toBoneMassProto(boneMassRecordInternal));
        } else if (instantRecordInternal
                instanceof CervicalMucusRecordInternal cervicalMucusRecordInternal) {
            builder.setCervicalMucus(toCervicalMucusProto(cervicalMucusRecordInternal));

        } else if (instantRecordInternal
                instanceof
                HeartRateVariabilityRmssdRecordInternal heartRateVariabilityRmssdRecordInternal) {
            builder.setHeartRateVariabilityRmssd(
                    toHeartRateVariabilityRmssdProto(heartRateVariabilityRmssdRecordInternal));
        } else if (instantRecordInternal instanceof HeightRecordInternal heightRecordInternal) {
            builder.setHeight(toHeightProto(heightRecordInternal));
        } else if (instantRecordInternal instanceof IntermenstrualBleedingRecordInternal) {
            builder.setIntermenstrualBleeding(IntermenstrualBleeding.getDefaultInstance());
        } else if (instantRecordInternal
                instanceof LeanBodyMassRecordInternal leanBodyMassRecordInternal) {
            builder.setLeanBodyMass(toLeanBodyMassProto(leanBodyMassRecordInternal));
        } else if (instantRecordInternal
                instanceof MenstruationFlowRecordInternal menstruationFlowRecordInternal) {
            builder.setMenstruationFlow(toMenstruationFlowProto(menstruationFlowRecordInternal));
        } else if (instantRecordInternal
                instanceof OvulationTestRecordInternal ovulationTestRecordInternal) {
            builder.setOvulationTest(toOvulationTestProto(ovulationTestRecordInternal));
        } else if (instantRecordInternal
                instanceof OxygenSaturationRecordInternal oxygenSaturationRecordInternal) {
            builder.setOxygenSaturation(toOxygenSaturationProto(oxygenSaturationRecordInternal));
        } else if (instantRecordInternal
                instanceof RespiratoryRateRecordInternal respiratoryRateRecordInternal) {
            builder.setRespiratoryRate(toRespiratoryRateProto(respiratoryRateRecordInternal));
        } else if (instantRecordInternal
                instanceof RestingHeartRateRecordInternal restingHeartRateRecordInternal) {
            builder.setRestingHeartRate(toRestingHeartRateProto(restingHeartRateRecordInternal));
        } else if (instantRecordInternal
                instanceof SexualActivityRecordInternal sexualActivityRecordInternal) {
            builder.setSexualActivity(toSexualActivityProto(sexualActivityRecordInternal));
        } else if (instantRecordInternal instanceof Vo2MaxRecordInternal vo2MaxRecordInternal) {
            builder.setVo2Max(toVo2MaxProto(vo2MaxRecordInternal));
        } else if (instantRecordInternal instanceof WeightRecordInternal weightRecordInternal) {
            builder.setWeight(toWeightProto(weightRecordInternal));
        } else {
            throw new IllegalArgumentException(
                    "Unknown instant record type "
                            + instantRecordInternal.getClass().getSimpleName());
        }

        return builder.build();
    }

    private static AlcoholConsumption toAlcoholConsumptionProto(
            AlcoholConsumptionRecordInternal alcoholConsumptionRecordInternal) {
        AlcoholConsumption.Builder builder =
                AlcoholConsumption.newBuilder()
                        .setBeverageType(alcoholConsumptionRecordInternal.getBeverageType())
                        .setServingVolume(alcoholConsumptionRecordInternal.getServingVolumeLiters())
                        .setAlcoholByVolume(alcoholConsumptionRecordInternal.getAlcoholByVolume())
                        .setTemporalType(alcoholConsumptionRecordInternal.getTemporalType());
        if (alcoholConsumptionRecordInternal.getNotes() != null) {
            builder.setNotes(alcoholConsumptionRecordInternal.getNotes().toString());
        }
        return builder.build();
    }

    private static BasalBodyTemperature toBasalBodyTemperatureProto(
            BasalBodyTemperatureRecordInternal basalBodyTemperatureRecordInternal) {
        return BasalBodyTemperature.newBuilder()
                .setMeasurementLocation(basalBodyTemperatureRecordInternal.getMeasurementLocation())
                .setTemperature(basalBodyTemperatureRecordInternal.getTemperature())
                .build();
    }

    private static BasalMetabolicRate toBasalMetabolicRateProto(
            BasalMetabolicRateRecordInternal basalMetabolicRateRecordInternal) {
        return BasalMetabolicRate.newBuilder()
                .setBasalMetabolicRate(basalMetabolicRateRecordInternal.getBasalMetabolicRate())
                .build();
    }

    private static BloodGlucose toBloodGlucoseProto(
            BloodGlucoseRecordInternal bloodGlucoseRecordInternal) {
        return BloodGlucose.newBuilder()
                .setSpecimenSource(bloodGlucoseRecordInternal.getSpecimenSource())
                .setLevel(bloodGlucoseRecordInternal.getLevel())
                .setRelationToMeal(bloodGlucoseRecordInternal.getRelationToMeal())
                .setMealType(bloodGlucoseRecordInternal.getMealType())
                .build();
    }

    private static BloodPressure toBloodPressureProto(
            BloodPressureRecordInternal bloodPressureRecordInternal) {
        return BloodPressure.newBuilder()
                .setMeasurementLocation(bloodPressureRecordInternal.getMeasurementLocation())
                .setSystolic(bloodPressureRecordInternal.getSystolic())
                .setDiastolic(bloodPressureRecordInternal.getDiastolic())
                .setBodyPosition(bloodPressureRecordInternal.getBodyPosition())
                .build();
    }

    private static BodyFat toBodyFatProto(BodyFatRecordInternal bodyFatRecordInternal) {
        return BodyFat.newBuilder().setPercentage(bodyFatRecordInternal.getPercentage()).build();
    }

    private static BodyTemperature toBodyTemperatureProto(
            BodyTemperatureRecordInternal bodyTemperatureRecordInternal) {
        return BodyTemperature.newBuilder()
                .setMeasurementLocation(bodyTemperatureRecordInternal.getMeasurementLocation())
                .setTemperature(bodyTemperatureRecordInternal.getTemperature())
                .build();
    }

    private static BodyWaterMass toBodyWaterMassProto(
            BodyWaterMassRecordInternal bodyWaterMassRecordInternal) {
        return BodyWaterMass.newBuilder()
                .setBodyWaterMass(bodyWaterMassRecordInternal.getBodyWaterMass())
                .build();
    }

    private static BoneMass toBoneMassProto(BoneMassRecordInternal boneMassRecordInternal) {
        return BoneMass.newBuilder().setMass(boneMassRecordInternal.getMass()).build();
    }

    private static CervicalMucus toCervicalMucusProto(
            CervicalMucusRecordInternal cervicalMucusRecordInternal) {
        return CervicalMucus.newBuilder()
                .setSensation(cervicalMucusRecordInternal.getSensation())
                .setAppearance(cervicalMucusRecordInternal.getAppearance())
                .build();
    }

    private static CyclePhases toCyclePhasesProto(CyclePhasesRecordInternal cyclePhasesInternal) {
        CyclePhases.Builder builder =
                CyclePhases.newBuilder().setPhase(cyclePhasesInternal.getPhase());
        if (cyclePhasesInternal.getDayOfCycle() != DEFAULT_INT) {
            builder.setDayOfCycle(cyclePhasesInternal.getDayOfCycle());
        }
        return builder.build();
    }

    private static HeartRateVariabilityRmssd toHeartRateVariabilityRmssdProto(
            HeartRateVariabilityRmssdRecordInternal heartRateVariabilityRmssdRecordInternal) {
        return HeartRateVariabilityRmssd.newBuilder()
                .setHeartRateVariabilityMillis(
                        heartRateVariabilityRmssdRecordInternal.getHeartRateVariabilityMillis())
                .build();
    }

    private static Height toHeightProto(HeightRecordInternal heightRecordInternal) {
        return Height.newBuilder().setHeight(heightRecordInternal.getHeight()).build();
    }

    private static LeanBodyMass toLeanBodyMassProto(
            LeanBodyMassRecordInternal leanBodyMassRecordInternal) {
        return LeanBodyMass.newBuilder().setMass(leanBodyMassRecordInternal.getMass()).build();
    }

    private static MenstruationFlow toMenstruationFlowProto(
            MenstruationFlowRecordInternal menstruationFlowRecordInternal) {
        return MenstruationFlow.newBuilder()
                .setFlow(menstruationFlowRecordInternal.getFlow())
                .build();
    }

    private static OvulationTest toOvulationTestProto(
            OvulationTestRecordInternal ovulationTestRecordInternal) {
        return OvulationTest.newBuilder()
                .setResult(ovulationTestRecordInternal.getResult())
                .build();
    }

    private static OxygenSaturation toOxygenSaturationProto(
            OxygenSaturationRecordInternal oxygenSaturationRecordInternal) {
        return OxygenSaturation.newBuilder()
                .setPercentage(oxygenSaturationRecordInternal.getPercentage())
                .build();
    }

    private static RespiratoryRate toRespiratoryRateProto(
            RespiratoryRateRecordInternal respiratoryRateRecordInternal) {
        return RespiratoryRate.newBuilder()
                .setRate(respiratoryRateRecordInternal.getRate())
                .build();
    }

    private static RestingHeartRate toRestingHeartRateProto(
            RestingHeartRateRecordInternal restingHeartRateRecordInternal) {
        return RestingHeartRate.newBuilder()
                .setBeatsPerMinute(restingHeartRateRecordInternal.getBeatsPerMinute())
                .build();
    }

    private static SexualActivity toSexualActivityProto(
            SexualActivityRecordInternal sexualActivityRecordInternal) {
        return SexualActivity.newBuilder()
                .setProtectionUsed(sexualActivityRecordInternal.getProtectionUsed())
                .build();
    }

    private static Vo2Max toVo2MaxProto(Vo2MaxRecordInternal vo2MaxRecordInternal) {
        return Vo2Max.newBuilder()
                .setMeasurementMethod(vo2MaxRecordInternal.getMeasurementMethod())
                .setVo2MillilitersPerMinuteKilogram(
                        vo2MaxRecordInternal.getVo2MillilitersPerMinuteKilogram())
                .build();
    }

    private static Weight toWeightProto(WeightRecordInternal weightRecordInternal) {
        return Weight.newBuilder().setWeight(weightRecordInternal.getWeight()).build();
    }

    @SuppressLint("WrongConstant") // Proto doesn't know about the device type & rec method IntDefs
    private RecordInternal<?> populateRecordInternal(Record recordProto)
            throws NoSuchMethodException,
                    InvocationTargetException,
                    InstantiationException,
                    IllegalAccessException {
        RecordInternal<?> recordInternal;
        switch (recordProto.getSubRecordCase()) {
            case INTERVAL_RECORD ->
                    recordInternal =
                            populateIntervalRecordInternal(recordProto.getIntervalRecord());
            case INSTANT_RECORD -> {
                int recordTypeId = getRecordTypeId(recordProto);
                Class<? extends RecordInternal<?>> recordClass =
                        mDataTypeClassMap.get(recordTypeId);
                Objects.requireNonNull(recordClass);
                recordInternal = recordClass.getConstructor().newInstance();
                populateInstantRecordInternal(
                        recordProto.getInstantRecord(), (InstantRecordInternal<?>) recordInternal);
            }
            default ->
                    throw new IllegalArgumentException(
                            "Unknown record type " + recordProto.getSubRecordCase());
        }
        String uuidString = recordProto.getUuid();
        if (!uuidString.isEmpty()) {
            recordInternal.setUuid(UUID.fromString(uuidString));
        }
        if (recordProto.hasPackageName()) {
            recordInternal.setPackageName(recordProto.getPackageName());
        }
        recordInternal.setLastModifiedTime(recordProto.getLastModifiedTime());
        if (recordProto.hasClientRecordId()) {
            recordInternal.setClientRecordId(recordProto.getClientRecordId());
        }
        recordInternal.setClientRecordVersion(recordProto.getClientRecordVersion());
        if (recordProto.hasManufacturer()) {
            recordInternal.setManufacturer(recordProto.getManufacturer());
        }
        if (recordProto.hasModel()) {
            recordInternal.setModel(recordProto.getModel());
        }
        recordInternal.setDeviceType(recordProto.getDeviceType());
        recordInternal.setRecordingMethod(recordProto.getRecordingMethod());
        return recordInternal;
    }

    private static IntervalRecordInternal<?> populateIntervalRecordInternal(
            IntervalRecord intervalRecordProto) {
        IntervalRecordInternal<?> intervalRecordInternal;

        switch (intervalRecordProto.getDataCase()) {
            case ACTIVE_CALORIES_BURNED ->
                    intervalRecordInternal =
                            populateActiveCaloriesBurnedRecordInternal(
                                    intervalRecordProto.getActiveCaloriesBurned());
            case ACTIVITY_INTENSITY ->
                    intervalRecordInternal =
                            populateActivityIntensityRecordInternal(
                                    intervalRecordProto.getActivityIntensity());
            case ALCOHOL_CONSUMPTION ->
                    intervalRecordInternal =
                            populateAlcoholConsumptionRecordInternal(
                                    intervalRecordProto.getAlcoholConsumption());
            case CYCLING_PEDALING_CADENCE ->
                    intervalRecordInternal =
                            populateCyclingPedalingCadenceRecordInternal(
                                    intervalRecordProto.getCyclingPedalingCadence());
            case CYCLE_PHASES ->
                    intervalRecordInternal =
                            populateCyclePhasesRecordInternal(intervalRecordProto.getCyclePhases());
            case DISTANCE ->
                    intervalRecordInternal =
                            populateDistanceRecordInternal(intervalRecordProto.getDistance());
            case ELEVATION_GAINED ->
                    intervalRecordInternal =
                            populateElevationGainedRecordInternal(
                                    intervalRecordProto.getElevationGained());
            case EXERCISE_SESSION ->
                    intervalRecordInternal =
                            populateExerciseSessionRecordInternal(
                                    intervalRecordProto.getExerciseSession());
            case FLOORS_CLIMBED ->
                    intervalRecordInternal =
                            populateFloorsClimbedRecordInternal(
                                    intervalRecordProto.getFloorsClimbed());
            case HEART_RATE ->
                    intervalRecordInternal =
                            populateHeartRateRecordInternal(intervalRecordProto.getHeartRate());
            case HYDRATION ->
                    intervalRecordInternal =
                            populateHydrationRecordInternal(intervalRecordProto.getHydration());
            case MENSTRUATION_PERIOD -> {
                intervalRecordInternal = new MenstruationPeriodRecordInternal();
            }
            case MINDFULNESS_SESSION ->
                    intervalRecordInternal =
                            populateMindfulnessSessionRecordInternal(
                                    intervalRecordProto.getMindfulnessSession());
            case NICOTINE_INTAKE ->
                    intervalRecordInternal =
                            populateNicotineIntakeRecordInternal(
                                    intervalRecordProto.getNicotineIntake());
            case NUTRITION ->
                    intervalRecordInternal =
                            populateNutritionRecordInternal(intervalRecordProto.getNutrition());
            case PLANNED_EXERCISE_SESSION ->
                    intervalRecordInternal =
                            populatePlannedExerciseSessionRecordInternal(
                                    intervalRecordProto.getPlannedExerciseSession());
            case POWER ->
                    intervalRecordInternal =
                            populatePowerRecordInternal(intervalRecordProto.getPower());
            case SKIN_TEMPERATURE ->
                    intervalRecordInternal =
                            populateSkinTemperatureRecordInternal(
                                    intervalRecordProto.getSkinTemperature());
            case SLEEP_SESSION ->
                    intervalRecordInternal =
                            populateSleepSessionRecordInternal(
                                    intervalRecordProto.getSleepSession());
            case SPEED ->
                    intervalRecordInternal =
                            populateSpeedRecordInternal(intervalRecordProto.getSpeed());
            case STEPS ->
                    intervalRecordInternal =
                            populateStepsRecordInternal(intervalRecordProto.getSteps());
            case STEPS_CADENCE ->
                    intervalRecordInternal =
                            populateStepsCadenceRecordInternal(
                                    intervalRecordProto.getStepsCadence());
            case TOTAL_CALORIES_BURNED ->
                    intervalRecordInternal =
                            populateTotalCaloriesBurnedRecordInternal(
                                    intervalRecordProto.getTotalCaloriesBurned());
            case WHEELCHAIR_PUSHES ->
                    intervalRecordInternal =
                            populateWheelchairPushesRecordInternal(
                                    intervalRecordProto.getWheelchairPushes());
            case SYMPTOMS ->
                    intervalRecordInternal =
                            populateSymptomRecordInternal(intervalRecordProto.getSymptoms());
            default ->
                    throw new IllegalArgumentException(
                            "Unknown record type " + intervalRecordProto.getDataCase());
        }
        intervalRecordInternal
                .setStartTime(intervalRecordProto.getStartTime())
                .setStartZoneOffset(intervalRecordProto.getStartZoneOffset())
                .setEndTime(intervalRecordProto.getEndTime())
                .setEndZoneOffset(intervalRecordProto.getEndZoneOffset());
        return intervalRecordInternal;
    }

    private static ActiveCaloriesBurnedRecordInternal populateActiveCaloriesBurnedRecordInternal(
            ActiveCaloriesBurned activeCaloriesBurnedProto) {
        return new ActiveCaloriesBurnedRecordInternal()
                .setEnergy(activeCaloriesBurnedProto.getEnergy());
    }

    private static ActivityIntensityRecordInternal populateActivityIntensityRecordInternal(
            ActivityIntensity activityIntensityProto) {
        return new ActivityIntensityRecordInternal()
                .setActivityIntensityType(activityIntensityProto.getActivityIntensityType());
    }

    private static AlcoholConsumptionRecordInternal populateAlcoholConsumptionRecordInternal(
            AlcoholConsumption alcoholConsumptionProto) {
        AlcoholConsumptionRecordInternal record =
                new AlcoholConsumptionRecordInternal()
                        .setTemporalType(alcoholConsumptionProto.getTemporalType())
                        .setBeverageType(alcoholConsumptionProto.getBeverageType());
        if (alcoholConsumptionProto.hasAlcoholByVolume()) {
            record.setAlcoholByVolume(alcoholConsumptionProto.getAlcoholByVolume());
        }
        if (alcoholConsumptionProto.hasServingVolume()) {
            record.setServingVolumeLiters(alcoholConsumptionProto.getServingVolume());
        }
        if (alcoholConsumptionProto.hasNotes()) {
            record.setNotes(alcoholConsumptionProto.getNotes());
        }
        return record;
    }

    private static CyclingPedalingCadenceRecordInternal
            populateCyclingPedalingCadenceRecordInternal(
                    CyclingPedalingCadence cyclingPedalingCadenceProto) {
        return new CyclingPedalingCadenceRecordInternal(
                cyclingPedalingCadenceProto.getSampleList().stream()
                        .map(
                                sample ->
                                        new CyclingPedalingCadenceRecordInternal
                                                .CyclingPedalingCadenceRecordSample(
                                                sample.getRevolutionsPerMinute(),
                                                sample.getEpochMillis()))
                        .collect(toSet()));
    }

    private static CyclePhasesRecordInternal populateCyclePhasesRecordInternal(
            CyclePhases cyclePhasesProto) {
        CyclePhasesRecordInternal record = new CyclePhasesRecordInternal();
        record.setPhase(cyclePhasesProto.getPhase());
        if (cyclePhasesProto.hasDayOfCycle()) {
            record.setDayOfCycle(cyclePhasesProto.getDayOfCycle());
        }
        return record;
    }

    private static DistanceRecordInternal populateDistanceRecordInternal(Distance distanceProto) {
        return new DistanceRecordInternal().setDistance(distanceProto.getDistance());
    }

    private static ElevationGainedRecordInternal populateElevationGainedRecordInternal(
            ElevationGained elevationGainedProto) {
        return new ElevationGainedRecordInternal()
                .setElevation(elevationGainedProto.getElevation());
    }

    private static ExerciseSessionRecordInternal populateExerciseSessionRecordInternal(
            ExerciseSession exerciseSessionProto) {
        ExerciseSessionRecordInternal exerciseSessionRecordInternal =
                new ExerciseSessionRecordInternal();
        if (exerciseSessionProto.hasNotes()) {
            exerciseSessionRecordInternal.setNotes(exerciseSessionProto.getNotes());
        }
        exerciseSessionRecordInternal.setExerciseType(exerciseSessionProto.getExerciseType());
        if (exerciseSessionProto.hasTitle()) {
            exerciseSessionRecordInternal.setTitle(exerciseSessionProto.getTitle());
        }
        exerciseSessionRecordInternal.setHasRoute(exerciseSessionProto.getHasRoute());
        if (exerciseSessionProto.hasRoute()) {
            List<LocationInternal> locations =
                    exerciseSessionProto.getRoute().getRouteLocationList().stream()
                            .map(
                                    location ->
                                            new LocationInternal()
                                                    .setTime(location.getTime())
                                                    .setLatitude(location.getLatitude())
                                                    .setLongitude(location.getLongitude())
                                                    .setHorizontalAccuracy(
                                                            location.getHorizontalAccuracy())
                                                    .setVerticalAccuracy(
                                                            location.getVerticalAccuracy())
                                                    .setAltitude(location.getAltitude()))
                            .toList();
            exerciseSessionRecordInternal.setRoute(new ExerciseRouteInternal(locations));
        }
        exerciseSessionRecordInternal.setExerciseLaps(
                exerciseSessionProto.getLapList().stream()
                        .map(
                                lap ->
                                        new ExerciseLapInternal()
                                                .setStartTime(lap.getStartTime())
                                                .setEndTime(lap.getEndTime())
                                                .setLength(lap.getLength()))
                        .toList());
        exerciseSessionRecordInternal.setExerciseSegments(
                exerciseSessionProto.getSegmentList().stream()
                        .map(
                                (ExerciseSegment segment) -> {
                                    ExerciseSegmentInternal segmentInternal =
                                            new ExerciseSegmentInternal();
                                    segmentInternal
                                            .setStartTime(segment.getStartTime())
                                            .setEndTime(segment.getEndTime())
                                            .setSegmentType(segment.getSegmentType())
                                            .setRepetitionsCount(segment.getRepetitionsCount());
                                    if (AconfigFlagHelper.isExerciseSegmentImprovementsEnabled()) {
                                        if (segment.hasWeight()) {
                                            segmentInternal.setWeightGrams(segment.getWeight());
                                        }
                                        if (segment.hasSetIndex()) {
                                            segmentInternal.setSetIndex(segment.getSetIndex());
                                        }
                                        if (segment.hasRateOfPerceivedExertion()) {
                                            segmentInternal.setRateOfPerceivedExertion(
                                                    segment.getRateOfPerceivedExertion());
                                        }
                                    }
                                    return segmentInternal;
                                })
                        .toList());
        if (exerciseSessionProto.hasPlannedExerciseSessionId()) {
            exerciseSessionRecordInternal.setPlannedExerciseSessionId(
                    UUID.fromString(exerciseSessionProto.getPlannedExerciseSessionId()));
        }
        if (exerciseSessionProto.hasSessionRateOfPerceivedExertion()) {
            exerciseSessionRecordInternal.setRateOfPerceivedExertion(
                    exerciseSessionProto.getSessionRateOfPerceivedExertion());
        }
        return exerciseSessionRecordInternal;
    }

    private static FloorsClimbedRecordInternal populateFloorsClimbedRecordInternal(
            FloorsClimbed floorsClimbedProto) {
        return new FloorsClimbedRecordInternal().setFloors(floorsClimbedProto.getFloors());
    }

    private static HeartRateRecordInternal populateHeartRateRecordInternal(
            HeartRate heartRateProto) {
        return new HeartRateRecordInternal(
                heartRateProto.getSampleList().stream()
                        .map(
                                sample ->
                                        new HeartRateRecordInternal.HeartRateSample(
                                                sample.getBeatsPerMinute(),
                                                sample.getEpochMillis()))
                        .collect(toSet()));
    }

    private static HydrationRecordInternal populateHydrationRecordInternal(
            Hydration hydrationProto) {
        return new HydrationRecordInternal().setVolume(hydrationProto.getVolume());
    }

    private static MindfulnessSessionRecordInternal populateMindfulnessSessionRecordInternal(
            MindfulnessSession mindfulnessSessionProto) {
        MindfulnessSessionRecordInternal mindfulnessSessionRecordInternal =
                new MindfulnessSessionRecordInternal();
        mindfulnessSessionRecordInternal.setMindfulnessSessionType(
                mindfulnessSessionProto.getMindfulnessSessionType());
        if (mindfulnessSessionProto.hasTitle()) {
            mindfulnessSessionRecordInternal.setTitle(mindfulnessSessionProto.getTitle());
        }
        if (mindfulnessSessionProto.hasNotes()) {
            mindfulnessSessionRecordInternal.setNotes(mindfulnessSessionProto.getNotes());
        }
        return mindfulnessSessionRecordInternal;
    }

    private static NicotineIntakeRecordInternal populateNicotineIntakeRecordInternal(
            NicotineIntake nicotineIntakeProto) {
        NicotineIntakeRecordInternal nicotineIntakeRecordInternal =
                new NicotineIntakeRecordInternal();
        nicotineIntakeRecordInternal
                .setNicotineIntakeType(nicotineIntakeProto.getNicotineIntakeType())
                .setQuantity(nicotineIntakeProto.getQuantity());
        if (nicotineIntakeProto.hasNicotineIntake()) {
            nicotineIntakeRecordInternal.setNicotineIntakeGrams(
                    nicotineIntakeProto.getNicotineIntake());
        }
        return nicotineIntakeRecordInternal;
    }

    private static NutritionRecordInternal populateNutritionRecordInternal(
            Nutrition nutritionProto) {
        NutritionRecordInternal nutritionRecordInternal = new NutritionRecordInternal();
        nutritionRecordInternal
                .setUnsaturatedFat(nutritionProto.getUnsaturatedFat())
                .setPotassium(nutritionProto.getPotassium())
                .setThiamin(nutritionProto.getThiamin())
                .setMealType(nutritionProto.getMealType())
                .setTransFat(nutritionProto.getTransFat())
                .setManganese(nutritionProto.getManganese())
                .setEnergyFromFat(nutritionProto.getEnergyFromFat())
                .setCaffeine(nutritionProto.getCaffeine())
                .setDietaryFiber(nutritionProto.getDietaryFiber())
                .setSelenium(nutritionProto.getSelenium())
                .setVitaminB6(nutritionProto.getVitaminB6())
                .setProtein(nutritionProto.getProtein())
                .setChloride(nutritionProto.getChloride())
                .setCholesterol(nutritionProto.getCholesterol())
                .setCopper(nutritionProto.getCopper())
                .setIodine(nutritionProto.getIodine())
                .setVitaminB12(nutritionProto.getVitaminB12())
                .setZinc(nutritionProto.getZinc())
                .setRiboflavin(nutritionProto.getRiboflavin())
                .setEnergy(nutritionProto.getEnergy())
                .setMolybdenum(nutritionProto.getMolybdenum())
                .setPhosphorus(nutritionProto.getPhosphorus())
                .setChromium(nutritionProto.getChromium())
                .setTotalFat(nutritionProto.getTotalFat())
                .setCalcium(nutritionProto.getCalcium())
                .setVitaminC(nutritionProto.getVitaminC())
                .setVitaminE(nutritionProto.getVitaminE())
                .setBiotin(nutritionProto.getBiotin())
                .setVitaminD(nutritionProto.getVitaminD())
                .setNiacin(nutritionProto.getNiacin())
                .setMagnesium(nutritionProto.getMagnesium())
                .setTotalCarbohydrate(nutritionProto.getTotalCarbohydrate())
                .setVitaminK(nutritionProto.getVitaminK())
                .setPolyunsaturatedFat(nutritionProto.getPolyunsaturatedFat())
                .setSaturatedFat(nutritionProto.getSaturatedFat())
                .setSodium(nutritionProto.getSodium())
                .setFolate(nutritionProto.getFolate())
                .setMonounsaturatedFat(nutritionProto.getMonounsaturatedFat())
                .setPantothenicAcid(nutritionProto.getPantothenicAcid())
                .setIron(nutritionProto.getIron())
                .setVitaminA(nutritionProto.getVitaminA())
                .setFolicAcid(nutritionProto.getFolicAcid())
                .setSugar(nutritionProto.getSugar());

        if (nutritionProto.hasMealName()) {
            nutritionRecordInternal.setMealName(nutritionProto.getMealName());
        }
        return nutritionRecordInternal;
    }

    private static PlannedExerciseSessionRecordInternal
            populatePlannedExerciseSessionRecordInternal(
                    PlannedExerciseSession plannedExerciseSessionProto) {
        PlannedExerciseSessionRecordInternal result =
                new PlannedExerciseSessionRecordInternal(
                        plannedExerciseSessionProto.getExerciseBlockList().stream()
                                .map(RecordProtoConverter::convertToPlannedExerciseBlockInternal)
                                .toList());
        if (plannedExerciseSessionProto.hasNotes()) {
            result.setNotes(plannedExerciseSessionProto.getNotes());
        }
        result.setExerciseType(plannedExerciseSessionProto.getExerciseType());
        if (plannedExerciseSessionProto.hasTitle()) {
            result.setTitle(plannedExerciseSessionProto.getTitle());
        }
        result.setHasExplicitTime(plannedExerciseSessionProto.getHasExplicitTime());
        return result;
    }

    private static PlannedExerciseBlockInternal convertToPlannedExerciseBlockInternal(
            PlannedExerciseBlock plannedExerciseBlockProto) {
        var plannedExerciseBlockInternal =
                new PlannedExerciseBlockInternal(plannedExerciseBlockProto.getRepetitions());

        if (plannedExerciseBlockProto.hasDescription()) {
            plannedExerciseBlockInternal.setDescription(plannedExerciseBlockProto.getDescription());
        }
        plannedExerciseBlockInternal.setExerciseSteps(
                plannedExerciseBlockProto.getStepList().stream()
                        .map(RecordProtoConverter::convertToPlannedExerciseStepInternal)
                        .toList());

        return plannedExerciseBlockInternal;
    }

    private static PlannedExerciseStepInternal convertToPlannedExerciseStepInternal(
            PlannedExerciseStep plannedExerciseStepProto) {
        var plannedExerciseStepInternal =
                new PlannedExerciseStepInternal(
                        plannedExerciseStepProto.getExerciseType(),
                        plannedExerciseStepProto.getExerciseCategory(),
                        convertToExerciseCompletionGoalInternal(
                                plannedExerciseStepProto.getCompletionGoal()));

        if (plannedExerciseStepProto.hasDescription()) {
            plannedExerciseStepInternal.setDescription(plannedExerciseStepProto.getDescription());
        }
        plannedExerciseStepInternal.setPerformanceGoals(
                plannedExerciseStepProto.getPerformanceGoalList().stream()
                        .map(RecordProtoConverter::convertToExercisePerformanceGoalInternal)
                        .toList());

        return plannedExerciseStepInternal;
    }

    private static ExerciseCompletionGoalInternal convertToExerciseCompletionGoalInternal(
            ExerciseCompletionGoal exerciseCompletionGoalProto) {
        return switch (exerciseCompletionGoalProto.getGoalCase()) {
            case DISTANCE_GOAL ->
                    new ExerciseCompletionGoalInternal.DistanceGoalInternal(
                            Length.fromMeters(
                                    exerciseCompletionGoalProto.getDistanceGoal().getDistance()));
            case STEPS_GOAL ->
                    new ExerciseCompletionGoalInternal.StepsGoalInternal(
                            exerciseCompletionGoalProto.getStepsGoal().getSteps());
            case DURATION_GOAL ->
                    new ExerciseCompletionGoalInternal.DurationGoalInternal(
                            Duration.ofMillis(
                                    exerciseCompletionGoalProto.getDurationGoal().getDuration()));
            case REPETITIONS_GOAL ->
                    new ExerciseCompletionGoalInternal.RepetitionsGoalInternal(
                            exerciseCompletionGoalProto.getRepetitionsGoal().getRepetitions());
            case TOTAL_CALORIES_BURNED_GOAL ->
                    new ExerciseCompletionGoalInternal.TotalCaloriesBurnedGoalInternal(
                            Energy.fromCalories(
                                    exerciseCompletionGoalProto
                                            .getTotalCaloriesBurnedGoal()
                                            .getTotalCalories()));
            case ACTIVE_CALORIES_BURNED_GOAL ->
                    new ExerciseCompletionGoalInternal.ActiveCaloriesBurnedGoalInternal(
                            Energy.fromCalories(
                                    exerciseCompletionGoalProto
                                            .getActiveCaloriesBurnedGoal()
                                            .getActiveCalories()));
            case DISTANCE_WITH_VARIABLE_REST_GOAL ->
                    new ExerciseCompletionGoalInternal.DistanceWithVariableRestGoalInternal(
                            Length.fromMeters(
                                    exerciseCompletionGoalProto
                                            .getDistanceWithVariableRestGoal()
                                            .getDistance()),
                            Duration.ofMillis(
                                    exerciseCompletionGoalProto
                                            .getDistanceWithVariableRestGoal()
                                            .getDuration()));
            case UNSPECIFIED_GOAL ->
                    ExerciseCompletionGoalInternal.UnspecifiedGoalInternal.INSTANCE;
            case UNKNOWN_GOAL, GOAL_NOT_SET ->
                    ExerciseCompletionGoalInternal.UnknownGoalInternal.INSTANCE;
        };
    }

    private static ExercisePerformanceGoalInternal convertToExercisePerformanceGoalInternal(
            ExercisePerformanceGoal exercisePerformanceGoalProto) {
        return switch (exercisePerformanceGoalProto.getGoalCase()) {
            case POWER_GOAL ->
                    new ExercisePerformanceGoalInternal.PowerGoalInternal(
                            Power.fromWatts(
                                    exercisePerformanceGoalProto.getPowerGoal().getMinPower()),
                            Power.fromWatts(
                                    exercisePerformanceGoalProto.getPowerGoal().getMaxPower()));
            case SPEED_GOAL ->
                    new ExercisePerformanceGoalInternal.SpeedGoalInternal(
                            Velocity.fromMetersPerSecond(
                                    exercisePerformanceGoalProto.getSpeedGoal().getMinSpeed()),
                            Velocity.fromMetersPerSecond(
                                    exercisePerformanceGoalProto.getSpeedGoal().getMaxSpeed()));
            case CADENCE_GOAL ->
                    new ExercisePerformanceGoalInternal.CadenceGoalInternal(
                            exercisePerformanceGoalProto.getCadenceGoal().getMinRpm(),
                            exercisePerformanceGoalProto.getCadenceGoal().getMaxRpm());
            case HEART_RATE_GOAL ->
                    new ExercisePerformanceGoalInternal.HeartRateGoalInternal(
                            exercisePerformanceGoalProto.getHeartRateGoal().getMinBpm(),
                            exercisePerformanceGoalProto.getHeartRateGoal().getMaxBpm());
            case WEIGHT_GOAL ->
                    new ExercisePerformanceGoalInternal.WeightGoalInternal(
                            Mass.fromGrams(exercisePerformanceGoalProto.getWeightGoal().getMass()));
            case RATE_OF_PERCEIVED_EXERTION_GOAL ->
                    new ExercisePerformanceGoalInternal.RateOfPerceivedExertionGoalInternal(
                            exercisePerformanceGoalProto.getRateOfPerceivedExertionGoal().getRpe());
            case AMRAP_GOAL -> ExercisePerformanceGoalInternal.AmrapGoalInternal.INSTANCE;
            case UNKNOWN_GOAL, GOAL_NOT_SET ->
                    ExercisePerformanceGoalInternal.UnknownGoalInternal.INSTANCE;
        };
    }

    private static PowerRecordInternal populatePowerRecordInternal(
            com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.Power
                    powerProto) {
        return new PowerRecordInternal(
                powerProto.getSampleList().stream()
                        .map(
                                sample ->
                                        new PowerRecordInternal.PowerRecordSample(
                                                sample.getPower(), sample.getEpochMillis()))
                        .collect(toSet()));
    }

    private static SkinTemperatureRecordInternal populateSkinTemperatureRecordInternal(
            SkinTemperature skinTemperatureProto) {
        return new SkinTemperatureRecordInternal(
                        skinTemperatureProto.getSampleList().stream()
                                .map(
                                        sample ->
                                                new SkinTemperatureRecordInternal
                                                        .SkinTemperatureDeltaSample(
                                                        sample.getTemperatureDeltaInCelsius(),
                                                        sample.getEpochMillis()))
                                .collect(toSet()))
                .setMeasurementLocation(skinTemperatureProto.getMeasurementLocation())
                .setBaseline(fromCelsius(skinTemperatureProto.getBaseline()));
    }

    @SuppressLint("WrongConstant")
    private static SleepSessionRecordInternal populateSleepSessionRecordInternal(
            SleepSession sleepSessionProto) {
        SleepSessionRecordInternal sleepSessionRecordInternal = new SleepSessionRecordInternal();
        if (sleepSessionProto.hasNotes()) {
            sleepSessionRecordInternal.setNotes(sleepSessionProto.getNotes());
        }
        if (sleepSessionProto.hasTitle()) {
            sleepSessionRecordInternal.setTitle(sleepSessionProto.getTitle());
        }
        if (sleepSessionProto.getStageCount() > 0) {
            sleepSessionRecordInternal.setSleepStages(
                    sleepSessionProto.getStageList().stream()
                            .map(
                                    sleepStage ->
                                            new SleepStageInternal()
                                                    .setStartTime(sleepStage.getStartTime())
                                                    .setEndTime(sleepStage.getEndTime())
                                                    .setStageType(sleepStage.getStageType()))
                            .toList());
        }
        return sleepSessionRecordInternal;
    }

    private static SpeedRecordInternal populateSpeedRecordInternal(Speed speedProto) {
        return new SpeedRecordInternal(
                speedProto.getSampleList().stream()
                        .map(
                                sample ->
                                        new SpeedRecordInternal.SpeedRecordSample(
                                                sample.getSpeed(), sample.getEpochMillis()))
                        .collect(toSet()));
    }

    private static StepsRecordInternal populateStepsRecordInternal(Steps stepsProto) {
        return new StepsRecordInternal().setCount(stepsProto.getCount());
    }

    private static TotalCaloriesBurnedRecordInternal populateTotalCaloriesBurnedRecordInternal(
            TotalCaloriesBurned totalCaloriesBurnedProto) {
        return new TotalCaloriesBurnedRecordInternal()
                .setEnergy(totalCaloriesBurnedProto.getEnergy());
    }

    private static StepsCadenceRecordInternal populateStepsCadenceRecordInternal(
            StepsCadence stepsCadenceProto) {
        return new StepsCadenceRecordInternal(
                stepsCadenceProto.getSampleList().stream()
                        .map(
                                sample ->
                                        new StepsCadenceRecordInternal.StepsCadenceRecordSample(
                                                sample.getRate(), sample.getEpochMillis()))
                        .collect(toSet()));
    }

    private static WheelchairPushesRecordInternal populateWheelchairPushesRecordInternal(
            WheelchairPushes wheelchairPushesProto) {
        return new WheelchairPushesRecordInternal().setCount(wheelchairPushesProto.getCount());
    }

    private static SymptomRecordInternal populateSymptomRecordInternal(Symptoms symptomsProto) {
        SymptomRecordInternal record =
                new SymptomRecordInternal()
                        .setSymptomType(symptomsProto.getSymptomType())
                        .setSeverity(symptomsProto.getSeverity())
                        .setCount(symptomsProto.getCount())
                        .setTemporalType(symptomsProto.getTemporalType());
        if (symptomsProto.hasNotes()) {
            record.setNotes(symptomsProto.getNotes());
        }
        return record;
    }

    private static void populateInstantRecordInternal(
            InstantRecord instantRecordProto, InstantRecordInternal<?> instantRecordInternal) {
        instantRecordInternal
                .setTime(instantRecordProto.getTime())
                .setZoneOffset(instantRecordProto.getZoneOffset());

        switch (instantRecordProto.getDataCase()) {
            case BASAL_BODY_TEMPERATURE ->
                    populateBasalBodyTemperatureRecordInternal(
                            instantRecordProto.getBasalBodyTemperature(),
                            (BasalBodyTemperatureRecordInternal) instantRecordInternal);
            case BASAL_METABOLIC_RATE ->
                    populateBasalMetabolicRateRecordInternal(
                            instantRecordProto.getBasalMetabolicRate(),
                            (BasalMetabolicRateRecordInternal) instantRecordInternal);
            case BLOOD_GLUCOSE ->
                    populateBloodGlucoseRecordInternal(
                            instantRecordProto.getBloodGlucose(),
                            (BloodGlucoseRecordInternal) instantRecordInternal);
            case BLOOD_PRESSURE ->
                    populateBloodPressureRecordInternal(
                            instantRecordProto.getBloodPressure(),
                            (BloodPressureRecordInternal) instantRecordInternal);
            case BODY_FAT ->
                    populateBodyFatRecordInternal(
                            instantRecordProto.getBodyFat(),
                            (BodyFatRecordInternal) instantRecordInternal);
            case BODY_TEMPERATURE ->
                    populateBodyTemperatureRecordInternal(
                            instantRecordProto.getBodyTemperature(),
                            (BodyTemperatureRecordInternal) instantRecordInternal);
            case BODY_WATER_MASS ->
                    populateBodyWaterMassRecordInternal(
                            instantRecordProto.getBodyWaterMass(),
                            (BodyWaterMassRecordInternal) instantRecordInternal);
            case BONE_MASS ->
                    populateBoneMassRecordInternal(
                            instantRecordProto.getBoneMass(),
                            (BoneMassRecordInternal) instantRecordInternal);
            case CERVICAL_MUCUS ->
                    populateCervicalMucusRecordInternal(
                            instantRecordProto.getCervicalMucus(),
                            (CervicalMucusRecordInternal) instantRecordInternal);
            case HEART_RATE_VARIABILITY_RMSSD ->
                    populateHeartRateVariabilityRmssdRecordInternal(
                            instantRecordProto.getHeartRateVariabilityRmssd(),
                            (HeartRateVariabilityRmssdRecordInternal) instantRecordInternal);
            case HEIGHT ->
                    populateHeightRecordInternal(
                            instantRecordProto.getHeight(),
                            (HeightRecordInternal) instantRecordInternal);
            case INTERMENSTRUAL_BLEEDING -> {
                // No data to populate.
            }
            case LEAN_BODY_MASS ->
                    populateLeanBodyMassRecordInternal(
                            instantRecordProto.getLeanBodyMass(),
                            (LeanBodyMassRecordInternal) instantRecordInternal);
            case MENSTRUATION_FLOW ->
                    populateMenstruationFlowRecordInternal(
                            instantRecordProto.getMenstruationFlow(),
                            (MenstruationFlowRecordInternal) instantRecordInternal);
            case OVULATION_TEST ->
                    populateOvulationTestRecordInternal(
                            instantRecordProto.getOvulationTest(),
                            (OvulationTestRecordInternal) instantRecordInternal);
            case OXYGEN_SATURATION ->
                    populateOxygenSaturationRecordInternal(
                            instantRecordProto.getOxygenSaturation(),
                            (OxygenSaturationRecordInternal) instantRecordInternal);
            case RESPIRATORY_RATE ->
                    populateRespiratoryRateRecordInternal(
                            instantRecordProto.getRespiratoryRate(),
                            (RespiratoryRateRecordInternal) instantRecordInternal);
            case RESTING_HEART_RATE ->
                    populateRestingHeartRateRecordInternal(
                            instantRecordProto.getRestingHeartRate(),
                            (RestingHeartRateRecordInternal) instantRecordInternal);
            case SEXUAL_ACTIVITY ->
                    populateSexualActivityRecordInternal(
                            instantRecordProto.getSexualActivity(),
                            (SexualActivityRecordInternal) instantRecordInternal);
            case VO2_MAX ->
                    populateVo2MaxRecordInternal(
                            instantRecordProto.getVo2Max(),
                            (Vo2MaxRecordInternal) instantRecordInternal);
            case WEIGHT ->
                    populateWeightRecordInternal(
                            instantRecordProto.getWeight(),
                            (WeightRecordInternal) instantRecordInternal);
            default ->
                    throw new IllegalArgumentException(
                            "Unknown record type " + instantRecordProto.getDataCase());
        }
    }

    private static void populateBasalBodyTemperatureRecordInternal(
            BasalBodyTemperature basalBodyTemperatureProto,
            BasalBodyTemperatureRecordInternal basalBodyTemperatureRecordInternal) {
        basalBodyTemperatureRecordInternal
                .setTemperature(basalBodyTemperatureProto.getTemperature())
                .setMeasurementLocation(basalBodyTemperatureProto.getMeasurementLocation());
    }

    private static void populateBasalMetabolicRateRecordInternal(
            BasalMetabolicRate basalMetabolicRateProto,
            BasalMetabolicRateRecordInternal basalMetabolicRateRecordInternal) {
        basalMetabolicRateRecordInternal.setBasalMetabolicRate(
                basalMetabolicRateProto.getBasalMetabolicRate());
    }

    private static void populateBloodGlucoseRecordInternal(
            BloodGlucose bloodGlucoseProto, BloodGlucoseRecordInternal bloodGlucoseRecordInternal) {
        bloodGlucoseRecordInternal
                .setSpecimenSource(bloodGlucoseProto.getSpecimenSource())
                .setLevel(bloodGlucoseProto.getLevel())
                .setRelationToMeal(bloodGlucoseProto.getRelationToMeal())
                .setMealType(bloodGlucoseProto.getMealType());
    }

    private static void populateBloodPressureRecordInternal(
            BloodPressure bloodPressureProto,
            BloodPressureRecordInternal bloodPressureRecordInternal) {
        bloodPressureRecordInternal
                .setMeasurementLocation(bloodPressureProto.getMeasurementLocation())
                .setSystolic(bloodPressureProto.getSystolic())
                .setDiastolic(bloodPressureProto.getDiastolic())
                .setBodyPosition(bloodPressureProto.getBodyPosition());
    }

    private static void populateBodyFatRecordInternal(
            BodyFat bodyFatProto, BodyFatRecordInternal bodyFatRecordInternal) {
        bodyFatRecordInternal.setPercentage(bodyFatProto.getPercentage());
    }

    private static void populateBodyTemperatureRecordInternal(
            BodyTemperature bodyTemperatureProto,
            BodyTemperatureRecordInternal bodyTemperatureRecordInternal) {
        bodyTemperatureRecordInternal
                .setTemperature(bodyTemperatureProto.getTemperature())
                .setMeasurementLocation(bodyTemperatureProto.getMeasurementLocation());
    }

    private static void populateBodyWaterMassRecordInternal(
            BodyWaterMass bodyWaterMassProto,
            BodyWaterMassRecordInternal bodyWaterMassRecordInternal) {
        bodyWaterMassRecordInternal.setBodyWaterMass(bodyWaterMassProto.getBodyWaterMass());
    }

    private static void populateBoneMassRecordInternal(
            BoneMass boneMassProto, BoneMassRecordInternal boneMassRecordInternal) {
        boneMassRecordInternal.setMass(boneMassProto.getMass());
    }

    private static void populateCervicalMucusRecordInternal(
            CervicalMucus cervicalMucusProto,
            CervicalMucusRecordInternal cervicalMucusRecordInternal) {
        cervicalMucusRecordInternal
                .setSensation(cervicalMucusProto.getSensation())
                .setAppearance(cervicalMucusProto.getAppearance());
    }

    private static void populateMenstruationFlowRecordInternal(
            MenstruationFlow menstruationFlowProto,
            MenstruationFlowRecordInternal menstruationFlowRecordInternal) {
        menstruationFlowRecordInternal.setFlow(menstruationFlowProto.getFlow());
    }

    private static void populateOvulationTestRecordInternal(
            OvulationTest ovulationTestProto,
            OvulationTestRecordInternal ovulationTestRecordInternal) {
        ovulationTestRecordInternal.setResult(ovulationTestProto.getResult());
    }

    private static void populateOxygenSaturationRecordInternal(
            OxygenSaturation oxygenSaturationProto,
            OxygenSaturationRecordInternal oxygenSaturationRecordInternal) {
        oxygenSaturationRecordInternal.setPercentage(oxygenSaturationProto.getPercentage());
    }

    private static void populateHeartRateVariabilityRmssdRecordInternal(
            HeartRateVariabilityRmssd heartRateVariabilityRmssdProto,
            HeartRateVariabilityRmssdRecordInternal heartRateVariabilityRmssdRecordInternal) {
        heartRateVariabilityRmssdRecordInternal.setHeartRateVariabilityMillis(
                heartRateVariabilityRmssdProto.getHeartRateVariabilityMillis());
    }

    private static void populateHeightRecordInternal(
            Height heightProto, HeightRecordInternal heightRecordInternal) {
        heightRecordInternal.setHeight(heightProto.getHeight());
    }

    private static void populateLeanBodyMassRecordInternal(
            LeanBodyMass leanBodyMassProto, LeanBodyMassRecordInternal leanBodyMassRecordInternal) {
        leanBodyMassRecordInternal.setMass(leanBodyMassProto.getMass());
    }

    private static void populateRespiratoryRateRecordInternal(
            RespiratoryRate respiratoryRateProto,
            RespiratoryRateRecordInternal respiratoryRateRecordInternal) {
        respiratoryRateRecordInternal.setRate(respiratoryRateProto.getRate());
    }

    private static void populateRestingHeartRateRecordInternal(
            RestingHeartRate restingHeartRateProto,
            RestingHeartRateRecordInternal restingHeartRateRecordInternal) {
        restingHeartRateRecordInternal.setBeatsPerMinute(restingHeartRateProto.getBeatsPerMinute());
    }

    private static void populateSexualActivityRecordInternal(
            SexualActivity sexualActivityProto,
            SexualActivityRecordInternal sexualActivityRecordInternal) {
        sexualActivityRecordInternal.setProtectionUsed(sexualActivityProto.getProtectionUsed());
    }

    private static void populateVo2MaxRecordInternal(
            Vo2Max vo2MaxProto, Vo2MaxRecordInternal vo2MaxRecordInternal) {
        vo2MaxRecordInternal
                .setMeasurementMethod(vo2MaxProto.getMeasurementMethod())
                .setVo2MillilitersPerMinuteKilogram(
                        vo2MaxProto.getVo2MillilitersPerMinuteKilogram());
    }

    private static void populateWeightRecordInternal(
            Weight weightProto, WeightRecordInternal weightRecordInternal) {
        weightRecordInternal.setWeight(weightProto.getWeight());
    }

    @RecordTypeIdentifier.RecordType
    int getRecordTypeId(Record protoRecord) {
        if (protoRecord.hasIntervalRecord()) {
            return getIntervalRecordId(protoRecord.getIntervalRecord());
        }
        if (protoRecord.hasInstantRecord()) {
            return getInstantRecordId(protoRecord.getInstantRecord());
        }
        throw new IllegalArgumentException("Record proto doesn't have a sub record defined.");
    }

    @RecordTypeIdentifier.RecordType
    private int getIntervalRecordId(IntervalRecord protoRecord) {
        return switch (protoRecord.getDataCase()) {
            case ACTIVE_CALORIES_BURNED -> RecordTypeIdentifier.RECORD_TYPE_ACTIVE_CALORIES_BURNED;
            case ACTIVITY_INTENSITY -> RecordTypeIdentifier.RECORD_TYPE_ACTIVITY_INTENSITY;
            case ALCOHOL_CONSUMPTION -> RecordTypeIdentifier.RECORD_TYPE_ALCOHOL_CONSUMPTION;
            case CYCLE_PHASES -> RecordTypeIdentifier.RECORD_TYPE_CYCLE_PHASES;
            case CYCLING_PEDALING_CADENCE ->
                    RecordTypeIdentifier.RECORD_TYPE_CYCLING_PEDALING_CADENCE;
            case DISTANCE -> RecordTypeIdentifier.RECORD_TYPE_DISTANCE;
            case ELEVATION_GAINED -> RecordTypeIdentifier.RECORD_TYPE_ELEVATION_GAINED;
            case EXERCISE_SESSION -> RecordTypeIdentifier.RECORD_TYPE_EXERCISE_SESSION;
            case FLOORS_CLIMBED -> RecordTypeIdentifier.RECORD_TYPE_FLOORS_CLIMBED;
            case HEART_RATE -> RecordTypeIdentifier.RECORD_TYPE_HEART_RATE;
            case HYDRATION -> RecordTypeIdentifier.RECORD_TYPE_HYDRATION;
            case MENSTRUATION_PERIOD -> RecordTypeIdentifier.RECORD_TYPE_MENSTRUATION_PERIOD;
            case MINDFULNESS_SESSION -> RecordTypeIdentifier.RECORD_TYPE_MINDFULNESS_SESSION;
            case NICOTINE_INTAKE -> RecordTypeIdentifier.RECORD_TYPE_NICOTINE_INTAKE;
            case NUTRITION -> RecordTypeIdentifier.RECORD_TYPE_NUTRITION;
            case PLANNED_EXERCISE_SESSION ->
                    RecordTypeIdentifier.RECORD_TYPE_PLANNED_EXERCISE_SESSION;
            case POWER -> RecordTypeIdentifier.RECORD_TYPE_POWER;
            case SKIN_TEMPERATURE -> RecordTypeIdentifier.RECORD_TYPE_SKIN_TEMPERATURE;
            case SLEEP_SESSION -> RecordTypeIdentifier.RECORD_TYPE_SLEEP_SESSION;
            case SPEED -> RecordTypeIdentifier.RECORD_TYPE_SPEED;
            case STEPS -> RecordTypeIdentifier.RECORD_TYPE_STEPS;
            case STEPS_CADENCE -> RecordTypeIdentifier.RECORD_TYPE_STEPS_CADENCE;
            case TOTAL_CALORIES_BURNED -> RecordTypeIdentifier.RECORD_TYPE_TOTAL_CALORIES_BURNED;
            case WHEELCHAIR_PUSHES -> RecordTypeIdentifier.RECORD_TYPE_WHEELCHAIR_PUSHES;
            case SYMPTOMS -> RecordTypeIdentifier.RECORD_TYPE_SYMPTOM;
            case DATA_NOT_SET -> throw new IllegalArgumentException("Interval record not set");
        };
    }

    @RecordTypeIdentifier.RecordType
    private int getInstantRecordId(InstantRecord protoRecord) {
        return switch (protoRecord.getDataCase()) {
            case BASAL_BODY_TEMPERATURE -> RecordTypeIdentifier.RECORD_TYPE_BASAL_BODY_TEMPERATURE;
            case BASAL_METABOLIC_RATE -> RecordTypeIdentifier.RECORD_TYPE_BASAL_METABOLIC_RATE;
            case BLOOD_GLUCOSE -> RecordTypeIdentifier.RECORD_TYPE_BLOOD_GLUCOSE;
            case BLOOD_PRESSURE -> RecordTypeIdentifier.RECORD_TYPE_BLOOD_PRESSURE;
            case BODY_FAT -> RecordTypeIdentifier.RECORD_TYPE_BODY_FAT;
            case BODY_TEMPERATURE -> RecordTypeIdentifier.RECORD_TYPE_BODY_TEMPERATURE;
            case BODY_WATER_MASS -> RecordTypeIdentifier.RECORD_TYPE_BODY_WATER_MASS;
            case BONE_MASS -> RecordTypeIdentifier.RECORD_TYPE_BONE_MASS;
            case CERVICAL_MUCUS -> RecordTypeIdentifier.RECORD_TYPE_CERVICAL_MUCUS;
            case HEART_RATE_VARIABILITY_RMSSD ->
                    RecordTypeIdentifier.RECORD_TYPE_HEART_RATE_VARIABILITY_RMSSD;
            case HEIGHT -> RecordTypeIdentifier.RECORD_TYPE_HEIGHT;
            case INTERMENSTRUAL_BLEEDING ->
                    RecordTypeIdentifier.RECORD_TYPE_INTERMENSTRUAL_BLEEDING;
            case LEAN_BODY_MASS -> RecordTypeIdentifier.RECORD_TYPE_LEAN_BODY_MASS;
            case MENSTRUATION_FLOW -> RecordTypeIdentifier.RECORD_TYPE_MENSTRUATION_FLOW;
            case OVULATION_TEST -> RecordTypeIdentifier.RECORD_TYPE_OVULATION_TEST;
            case OXYGEN_SATURATION -> RecordTypeIdentifier.RECORD_TYPE_OXYGEN_SATURATION;
            case RESPIRATORY_RATE -> RecordTypeIdentifier.RECORD_TYPE_RESPIRATORY_RATE;
            case RESTING_HEART_RATE -> RecordTypeIdentifier.RECORD_TYPE_RESTING_HEART_RATE;
            case SEXUAL_ACTIVITY -> RecordTypeIdentifier.RECORD_TYPE_SEXUAL_ACTIVITY;
            case VO2_MAX -> RecordTypeIdentifier.RECORD_TYPE_VO2_MAX;
            case WEIGHT -> RecordTypeIdentifier.RECORD_TYPE_WEIGHT;
            case DATA_NOT_SET -> throw new IllegalArgumentException("Instant record not set");
        };
    }
}
