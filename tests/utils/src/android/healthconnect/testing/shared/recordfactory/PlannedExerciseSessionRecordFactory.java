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

package android.healthconnect.testing.shared.recordfactory;

import android.health.connect.datatypes.ExerciseCompletionGoal;
import android.health.connect.datatypes.ExercisePerformanceGoal;
import android.health.connect.datatypes.ExerciseSessionType;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.PlannedExerciseBlock;
import android.health.connect.datatypes.PlannedExerciseSessionRecord;
import android.health.connect.datatypes.PlannedExerciseStep;
import android.health.connect.datatypes.units.Energy;
import android.health.connect.datatypes.units.Length;
import android.health.connect.datatypes.units.Mass;
import android.health.connect.datatypes.units.Power;
import android.health.connect.datatypes.units.Velocity;
import android.os.Bundle;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/** Note: This class is AI generated, validate before using, and remove this note */
public final class PlannedExerciseSessionRecordFactory
        extends RecordFactory<PlannedExerciseSessionRecord> {
    private static final String KEY_EXERCISE_TYPE = PREFIX + "EXERCISE_TYPE";
    private static final String KEY_TITLE = PREFIX + "TITLE";
    private static final String KEY_NOTES = PREFIX + "NOTES";
    private static final String KEY_BLOCKS = PREFIX + "BLOCKS";
    private static final String KEY_REPS = PREFIX + "REPS";
    private static final String KEY_DESCRIPTION = PREFIX + "DESCRIPTION";
    private static final String KEY_STEPS = PREFIX + "STEPS";
    private static final String KEY_EXERCISE_CATEGORY = PREFIX + "EXERCISE_CATEGORY";
    private static final String KEY_COMPLETION_GOAL = PREFIX + "COMPLETION_GOAL";
    private static final String KEY_PERFORMANCE_GOALS = PREFIX + "PERFORMANCE_GOALS";
    private static final String KEY_TYPE = PREFIX + "TYPE";
    private static final String KEY_DISTANCE = PREFIX + "DISTANCE";
    private static final String KEY_DURATION = PREFIX + "DURATION";
    private static final String KEY_TOTAL_CALORIES = PREFIX + "TOTAL_CALORIES";
    private static final String KEY_ACTIVE_CALORIES = PREFIX + "ACTIVE_CALORIES";
    private static final String KEY_MIN_POWER = PREFIX + "MIN_POWER";
    private static final String KEY_MAX_POWER = PREFIX + "MAX_POWER";
    private static final String KEY_MIN_SPEED = PREFIX + "MIN_SPEED";
    private static final String KEY_MAX_SPEED = PREFIX + "MAX_SPEED";
    private static final String KEY_MIN_RPM = PREFIX + "MIN_RPM";
    private static final String KEY_MAX_RPM = PREFIX + "MAX_RPM";
    private static final String KEY_MIN_BPM = PREFIX + "MIN_BPM";
    private static final String KEY_MAX_BPM = PREFIX + "MAX_BPM";
    private static final String KEY_MASS = PREFIX + "MASS";
    private static final String KEY_RPE = PREFIX + "RPE";

    @Override
    public PlannedExerciseSessionRecord newFullRecord(
            Metadata metadata, Instant startTime, Instant endTime) {
        return new PlannedExerciseSessionRecord.Builder(
                        metadata,
                        ExerciseSessionType.EXERCISE_SESSION_TYPE_WALKING,
                        startTime,
                        endTime)
                .setStartZoneOffset(ZoneOffset.ofHours(3))
                .setEndZoneOffset(ZoneOffset.ofHours(-2))
                .setTitle("My walking session")
                .setNotes("A long walk in the park")
                .setBlocks(
                        List.of(
                                new PlannedExerciseBlock.Builder(3)
                                        .setDescription("description")
                                        .build()))
                .build();
    }

    @Override
    public PlannedExerciseSessionRecord anotherFullRecord(
            Metadata metadata, Instant startTime, Instant endTime) {
        return new PlannedExerciseSessionRecord.Builder(
                        metadata,
                        ExerciseSessionType.EXERCISE_SESSION_TYPE_RUNNING,
                        startTime,
                        endTime)
                .setStartZoneOffset(ZoneOffset.ofHours(-1))
                .setEndZoneOffset(ZoneOffset.ofHours(2))
                .setTitle("My running session")
                .setNotes("A quick run around the block")
                .setBlocks(
                        List.of(
                                new PlannedExerciseBlock.Builder(4)
                                        .setDescription("another description")
                                        .build()))
                .build();
    }

    @Override
    public PlannedExerciseSessionRecord newEmptyRecord(
            Metadata metadata, Instant startTime, Instant endTime) {
        return new PlannedExerciseSessionRecord.Builder(
                        metadata,
                        ExerciseSessionType.EXERCISE_SESSION_TYPE_WALKING,
                        startTime,
                        endTime)
                .build();
    }

    @Override
    protected PlannedExerciseSessionRecord recordWithMetadata(
            PlannedExerciseSessionRecord record, Metadata metadata) {
        return new PlannedExerciseSessionRecord.Builder(
                        metadata,
                        record.getExerciseType(),
                        record.getStartTime(),
                        record.getEndTime())
                .setStartZoneOffset(record.getStartZoneOffset())
                .setEndZoneOffset(record.getEndZoneOffset())
                .setTitle(record.getTitle())
                .setNotes(record.getNotes())
                .setBlocks(record.getBlocks())
                .build();
    }

    @Override
    protected Bundle getValuesBundleForRecord(PlannedExerciseSessionRecord record) {
        Bundle values = new Bundle();
        values.putInt(KEY_EXERCISE_TYPE, record.getExerciseType());
        values.putCharSequence(KEY_TITLE, record.getTitle());
        values.putCharSequence(KEY_NOTES, record.getNotes());

        ArrayList<Bundle> blockBundles = new ArrayList<>();
        for (PlannedExerciseBlock block : record.getBlocks()) {
            blockBundles.add(getBundleForBlock(block));
        }
        values.putParcelableArrayList(KEY_BLOCKS, blockBundles);

        return values;
    }

    @Override
    public PlannedExerciseSessionRecord newRecordFromValuesBundle(
            Metadata metadata,
            Instant startTime,
            Instant endTime,
            ZoneOffset startZoneOffset,
            ZoneOffset endZoneOffset,
            Bundle bundle) {
        PlannedExerciseSessionRecord.Builder builder =
                new PlannedExerciseSessionRecord.Builder(
                                metadata,
                                bundle.getInt(
                                        KEY_EXERCISE_TYPE,
                                        ExerciseSessionType.EXERCISE_SESSION_TYPE_UNKNOWN),
                                startTime,
                                endTime)
                        .setStartZoneOffset(startZoneOffset)
                        .setEndZoneOffset(endZoneOffset);
        if (bundle.containsKey(KEY_TITLE)) {
            builder.setTitle(bundle.getString(KEY_TITLE));
        }
        if (bundle.containsKey(KEY_NOTES)) {
            builder.setNotes(bundle.getString(KEY_NOTES));
        }

        ArrayList<Bundle> blockBundles = bundle.getParcelableArrayList(KEY_BLOCKS, Bundle.class);
        if (blockBundles != null) {
            List<PlannedExerciseBlock> blocks = new ArrayList<>();
            for (Bundle blockBundle : blockBundles) {
                blocks.add(getPlannedExerciseBlockFromBundle(blockBundle));
            }
            builder.setBlocks(blocks);
        }

        return builder.build();
    }

    private static Bundle getBundleForBlock(PlannedExerciseBlock block) {
        Bundle bundle = new Bundle();
        bundle.putInt(KEY_REPS, block.getRepetitions());
        bundle.putCharSequence(KEY_DESCRIPTION, block.getDescription());
        ArrayList<Bundle> stepBundles = new ArrayList<>();
        for (PlannedExerciseStep step : block.getSteps()) {
            stepBundles.add(getBundleForStep(step));
        }
        bundle.putParcelableArrayList(KEY_STEPS, stepBundles);
        return bundle;
    }

    private static PlannedExerciseBlock getPlannedExerciseBlockFromBundle(Bundle bundle) {
        PlannedExerciseBlock.Builder builder =
                new PlannedExerciseBlock.Builder(bundle.getInt(KEY_REPS))
                        .setDescription(bundle.getCharSequence(KEY_DESCRIPTION));
        ArrayList<Bundle> stepBundles = bundle.getParcelableArrayList(KEY_STEPS);
        if (stepBundles != null) {
            for (Bundle stepBundle : stepBundles) {
                builder.addStep(getPlannedExerciseStepFromBundle(stepBundle));
            }
        }
        return builder.build();
    }

    private static Bundle getBundleForStep(PlannedExerciseStep step) {
        Bundle bundle = new Bundle();
        bundle.putInt(KEY_EXERCISE_TYPE, step.getExerciseType());
        bundle.putCharSequence(KEY_DESCRIPTION, step.getDescription());
        bundle.putInt(KEY_EXERCISE_CATEGORY, step.getExerciseCategory());
        bundle.putBundle(KEY_COMPLETION_GOAL, getBundleForCompletionGoal(step.getCompletionGoal()));
        ArrayList<Bundle> performanceGoalBundles = new ArrayList<>();
        for (ExercisePerformanceGoal goal : step.getPerformanceGoals()) {
            performanceGoalBundles.add(getBundleForPerformanceGoal(goal));
        }
        bundle.putParcelableArrayList(KEY_PERFORMANCE_GOALS, performanceGoalBundles);
        return bundle;
    }

    private static PlannedExerciseStep getPlannedExerciseStepFromBundle(Bundle bundle) {
        PlannedExerciseStep.Builder builder =
                new PlannedExerciseStep.Builder(
                                bundle.getInt(KEY_EXERCISE_TYPE),
                                bundle.getInt(KEY_EXERCISE_CATEGORY),
                                getCompletionGoalFromBundle(bundle.getBundle(KEY_COMPLETION_GOAL)))
                        .setDescription(bundle.getCharSequence(KEY_DESCRIPTION));
        ArrayList<Bundle> performanceGoalBundles =
                bundle.getParcelableArrayList(KEY_PERFORMANCE_GOALS);
        if (performanceGoalBundles != null) {
            for (Bundle performanceGoalBundle : performanceGoalBundles) {
                builder.addPerformanceGoal(getPerformanceGoalFromBundle(performanceGoalBundle));
            }
        }
        return builder.build();
    }

    private static Bundle getBundleForCompletionGoal(ExerciseCompletionGoal goal) {
        Bundle bundle = new Bundle();
        if (goal instanceof ExerciseCompletionGoal.DistanceGoal) {
            bundle.putString(KEY_TYPE, "distance");
            bundle.putDouble(
                    KEY_DISTANCE,
                    ((ExerciseCompletionGoal.DistanceGoal) goal).getDistance().getInMeters());
        } else if (goal instanceof ExerciseCompletionGoal.DistanceWithVariableRestGoal) {
            bundle.putString(KEY_TYPE, "distance_with_variable_rest");
            bundle.putDouble(
                    KEY_DISTANCE,
                    ((ExerciseCompletionGoal.DistanceWithVariableRestGoal) goal)
                            .getDistance()
                            .getInMeters());
            bundle.putLong(
                    KEY_DURATION,
                    ((ExerciseCompletionGoal.DistanceWithVariableRestGoal) goal)
                            .getDuration()
                            .toMillis());
        } else if (goal instanceof ExerciseCompletionGoal.StepsGoal) {
            bundle.putString(KEY_TYPE, "steps");
            bundle.putInt(KEY_STEPS, ((ExerciseCompletionGoal.StepsGoal) goal).getSteps());
        } else if (goal instanceof ExerciseCompletionGoal.DurationGoal) {
            bundle.putString(KEY_TYPE, "duration");
            bundle.putLong(
                    KEY_DURATION,
                    ((ExerciseCompletionGoal.DurationGoal) goal).getDuration().toMillis());
        } else if (goal instanceof ExerciseCompletionGoal.RepetitionsGoal) {
            bundle.putString(KEY_TYPE, "repetitions");
            bundle.putInt(
                    KEY_REPS, ((ExerciseCompletionGoal.RepetitionsGoal) goal).getRepetitions());
        } else if (goal instanceof ExerciseCompletionGoal.TotalCaloriesBurnedGoal) {
            bundle.putString(KEY_TYPE, "total_calories_burned");
            bundle.putDouble(
                    KEY_TOTAL_CALORIES,
                    ((ExerciseCompletionGoal.TotalCaloriesBurnedGoal) goal)
                            .getTotalCalories()
                            .getInCalories());
        } else if (goal instanceof ExerciseCompletionGoal.ActiveCaloriesBurnedGoal) {
            bundle.putString(KEY_TYPE, "active_calories_burned");
            bundle.putDouble(
                    KEY_ACTIVE_CALORIES,
                    ((ExerciseCompletionGoal.ActiveCaloriesBurnedGoal) goal)
                            .getActiveCalories()
                            .getInCalories());
        } else if (goal instanceof ExerciseCompletionGoal.UnknownGoal) {
            bundle.putString(KEY_TYPE, "unknown");
        } else if (goal instanceof ExerciseCompletionGoal.UnspecifiedGoal) {
            bundle.putString(KEY_TYPE, "unspecified");
        }
        return bundle;
    }

    private static ExerciseCompletionGoal getCompletionGoalFromBundle(Bundle bundle) {
        switch (bundle.getString(KEY_TYPE)) {
            case "distance":
                return new ExerciseCompletionGoal.DistanceGoal(
                        Length.fromMeters(bundle.getDouble(KEY_DISTANCE)));
            case "distance_with_variable_rest":
                return new ExerciseCompletionGoal.DistanceWithVariableRestGoal(
                        Length.fromMeters(bundle.getDouble(KEY_DISTANCE)),
                        Duration.ofMillis(bundle.getLong(KEY_DURATION)));
            case "steps":
                return new ExerciseCompletionGoal.StepsGoal(bundle.getInt(KEY_STEPS));
            case "duration":
                return new ExerciseCompletionGoal.DurationGoal(
                        Duration.ofMillis(bundle.getLong(KEY_DURATION)));
            case "repetitions":
                return new ExerciseCompletionGoal.RepetitionsGoal(bundle.getInt(KEY_REPS));
            case "total_calories_burned":
                return new ExerciseCompletionGoal.TotalCaloriesBurnedGoal(
                        Energy.fromCalories(bundle.getDouble(KEY_TOTAL_CALORIES)));
            case "active_calories_burned":
                return new ExerciseCompletionGoal.ActiveCaloriesBurnedGoal(
                        Energy.fromCalories(bundle.getDouble(KEY_ACTIVE_CALORIES)));
            case "unknown":
                return ExerciseCompletionGoal.UnknownGoal.INSTANCE;
            case "unspecified":
                return ExerciseCompletionGoal.UnspecifiedGoal.INSTANCE;
            default:
                throw new IllegalArgumentException("Unknown completion goal type");
        }
    }

    private static Bundle getBundleForPerformanceGoal(ExercisePerformanceGoal goal) {
        Bundle bundle = new Bundle();
        if (goal instanceof ExercisePerformanceGoal.PowerGoal) {
            bundle.putString(KEY_TYPE, "power");
            bundle.putDouble(
                    KEY_MIN_POWER,
                    ((ExercisePerformanceGoal.PowerGoal) goal).getMinPower().getInWatts());
            bundle.putDouble(
                    KEY_MAX_POWER,
                    ((ExercisePerformanceGoal.PowerGoal) goal).getMaxPower().getInWatts());
        } else if (goal instanceof ExercisePerformanceGoal.SpeedGoal) {
            bundle.putString(KEY_TYPE, "speed");
            bundle.putDouble(
                    KEY_MIN_SPEED,
                    ((ExercisePerformanceGoal.SpeedGoal) goal)
                            .getMinSpeed()
                            .getInMetersPerSecond());
            bundle.putDouble(
                    KEY_MAX_SPEED,
                    ((ExercisePerformanceGoal.SpeedGoal) goal)
                            .getMaxSpeed()
                            .getInMetersPerSecond());
        } else if (goal instanceof ExercisePerformanceGoal.CadenceGoal) {
            bundle.putString(KEY_TYPE, "cadence");
            bundle.putDouble(KEY_MIN_RPM, ((ExercisePerformanceGoal.CadenceGoal) goal).getMinRpm());
            bundle.putDouble(KEY_MAX_RPM, ((ExercisePerformanceGoal.CadenceGoal) goal).getMaxRpm());
        } else if (goal instanceof ExercisePerformanceGoal.HeartRateGoal) {
            bundle.putString(KEY_TYPE, "heart_rate");
            bundle.putInt(KEY_MIN_BPM, ((ExercisePerformanceGoal.HeartRateGoal) goal).getMinBpm());
            bundle.putInt(KEY_MAX_BPM, ((ExercisePerformanceGoal.HeartRateGoal) goal).getMaxBpm());
        } else if (goal instanceof ExercisePerformanceGoal.WeightGoal) {
            bundle.putString(KEY_TYPE, "weight");
            bundle.putDouble(
                    KEY_MASS, ((ExercisePerformanceGoal.WeightGoal) goal).getMass().getInGrams());
        } else if (goal instanceof ExercisePerformanceGoal.RateOfPerceivedExertionGoal) {
            bundle.putString(KEY_TYPE, "rpe");
            bundle.putInt(
                    KEY_RPE, ((ExercisePerformanceGoal.RateOfPerceivedExertionGoal) goal).getRpe());
        } else if (goal instanceof ExercisePerformanceGoal.AmrapGoal) {
            bundle.putString(KEY_TYPE, "amrap");
        } else if (goal instanceof ExercisePerformanceGoal.UnknownGoal) {
            bundle.putString(KEY_TYPE, "unknown");
        }
        return bundle;
    }

    private static ExercisePerformanceGoal getPerformanceGoalFromBundle(Bundle bundle) {
        switch (bundle.getString(KEY_TYPE)) {
            case "power":
                return new ExercisePerformanceGoal.PowerGoal(
                        Power.fromWatts(bundle.getDouble(KEY_MIN_POWER)),
                        Power.fromWatts(bundle.getDouble(KEY_MAX_POWER)));
            case "speed":
                return new ExercisePerformanceGoal.SpeedGoal(
                        Velocity.fromMetersPerSecond(bundle.getDouble(KEY_MIN_SPEED)),
                        Velocity.fromMetersPerSecond(bundle.getDouble(KEY_MAX_SPEED)));
            case "cadence":
                return new ExercisePerformanceGoal.CadenceGoal(
                        bundle.getDouble(KEY_MIN_RPM), bundle.getDouble(KEY_MAX_RPM));
            case "heart_rate":
                return new ExercisePerformanceGoal.HeartRateGoal(
                        bundle.getInt(KEY_MIN_BPM), bundle.getInt(KEY_MAX_BPM));
            case "weight":
                return new ExercisePerformanceGoal.WeightGoal(
                        Mass.fromGrams(bundle.getDouble(KEY_MASS)));
            case "rpe":
                return new ExercisePerformanceGoal.RateOfPerceivedExertionGoal(
                        bundle.getInt(KEY_RPE));
            case "amrap":
                return ExercisePerformanceGoal.AmrapGoal.INSTANCE;
            case "unknown":
                return ExercisePerformanceGoal.UnknownGoal.INSTANCE;
            default:
                throw new IllegalArgumentException("Unknown performance goal type");
        }
    }
}
