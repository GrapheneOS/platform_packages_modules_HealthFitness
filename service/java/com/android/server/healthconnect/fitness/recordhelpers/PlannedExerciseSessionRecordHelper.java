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

package com.android.server.healthconnect.fitness.recordhelpers;

import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_EXERCISE_SESSION;

import static com.android.server.healthconnect.fitness.recordhelpers.ExerciseSessionRecordHelper.EXERCISE_SESSION_RECORD_TABLE_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.ExerciseSessionRecordHelper.PLANNED_EXERCISE_SESSION_ID_COLUMN_NAME;
import static com.android.server.healthconnect.storage.utils.StorageUtils.BLOB;
import static com.android.server.healthconnect.storage.utils.StorageUtils.BLOB_NULL;
import static com.android.server.healthconnect.storage.utils.StorageUtils.BOOLEAN_FALSE_VALUE;
import static com.android.server.healthconnect.storage.utils.StorageUtils.BOOLEAN_TRUE_VALUE;
import static com.android.server.healthconnect.storage.utils.StorageUtils.INTEGER;
import static com.android.server.healthconnect.storage.utils.StorageUtils.INTEGER_NOT_NULL;
import static com.android.server.healthconnect.storage.utils.StorageUtils.PRIMARY_AUTOINCREMENT;
import static com.android.server.healthconnect.storage.utils.StorageUtils.TEXT_NULL;
import static com.android.server.healthconnect.storage.utils.StorageUtils.convertBytesToDouble;
import static com.android.server.healthconnect.storage.utils.StorageUtils.convertBytesToInt;
import static com.android.server.healthconnect.storage.utils.StorageUtils.convertBytesToLong;
import static com.android.server.healthconnect.storage.utils.StorageUtils.convertDoubleToBytes;
import static com.android.server.healthconnect.storage.utils.StorageUtils.convertIntToBytes;
import static com.android.server.healthconnect.storage.utils.StorageUtils.convertLongToBytes;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorBlob;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorInt;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorString;
import static com.android.server.healthconnect.storage.utils.StorageUtils.getCursorUUID;

import android.content.ContentValues;
import android.database.Cursor;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.health.connect.datatypes.units.Energy;
import android.health.connect.datatypes.units.Length;
import android.health.connect.datatypes.units.Mass;
import android.health.connect.datatypes.units.Power;
import android.health.connect.datatypes.units.Velocity;
import android.health.connect.internal.datatypes.ExerciseCompletionGoalInternal;
import android.health.connect.internal.datatypes.ExerciseCompletionGoalInternal.ActiveCaloriesBurnedGoalInternal;
import android.health.connect.internal.datatypes.ExerciseCompletionGoalInternal.DistanceGoalInternal;
import android.health.connect.internal.datatypes.ExerciseCompletionGoalInternal.DistanceWithVariableRestGoalInternal;
import android.health.connect.internal.datatypes.ExerciseCompletionGoalInternal.DurationGoalInternal;
import android.health.connect.internal.datatypes.ExerciseCompletionGoalInternal.RepetitionsGoalInternal;
import android.health.connect.internal.datatypes.ExerciseCompletionGoalInternal.StepsGoalInternal;
import android.health.connect.internal.datatypes.ExerciseCompletionGoalInternal.TotalCaloriesBurnedGoalInternal;
import android.health.connect.internal.datatypes.ExerciseCompletionGoalInternal.UnspecifiedGoalInternal;
import android.health.connect.internal.datatypes.ExercisePerformanceGoalInternal;
import android.health.connect.internal.datatypes.ExercisePerformanceGoalInternal.AmrapGoalInternal;
import android.health.connect.internal.datatypes.ExercisePerformanceGoalInternal.CadenceGoalInternal;
import android.health.connect.internal.datatypes.ExercisePerformanceGoalInternal.HeartRateGoalInternal;
import android.health.connect.internal.datatypes.ExercisePerformanceGoalInternal.PowerGoalInternal;
import android.health.connect.internal.datatypes.ExercisePerformanceGoalInternal.RateOfPerceivedExertionGoalInternal;
import android.health.connect.internal.datatypes.ExercisePerformanceGoalInternal.SpeedGoalInternal;
import android.health.connect.internal.datatypes.ExercisePerformanceGoalInternal.WeightGoalInternal;
import android.health.connect.internal.datatypes.PlannedExerciseBlockInternal;
import android.health.connect.internal.datatypes.PlannedExerciseSessionRecordInternal;
import android.health.connect.internal.datatypes.PlannedExerciseStepInternal;
import android.util.Pair;

import com.android.server.healthconnect.fitness.RecordReadTableRequest;
import com.android.server.healthconnect.fitness.mappings.InternalHealthConnectMappings;
import com.android.server.healthconnect.storage.request.AlterTableRequest;
import com.android.server.healthconnect.storage.request.CreateTableRequest;
import com.android.server.healthconnect.storage.request.ReadTableRequest;
import com.android.server.healthconnect.storage.request.UpsertTableRequest;
import com.android.server.healthconnect.storage.utils.SqlJoin;
import com.android.server.healthconnect.storage.utils.StorageUtils;
import com.android.server.healthconnect.storage.utils.TableColumnPair;
import com.android.server.healthconnect.storage.utils.WhereClauses;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Helper class for PlannedExerciseSessionRecord.
 *
 * @hide
 */
public final class PlannedExerciseSessionRecordHelper
        extends IntervalRecordHelper<PlannedExerciseSessionRecordInternal> {
    // Tables.
    public static final String PLANNED_EXERCISE_SESSION_RECORD_TABLE_NAME =
            "planned_exercise_session_record_table";
    private static final String PLANNED_EXERCISE_SESSION_BLOCKS_TABLE_NAME =
            "planned_exercise_session_blocks_table";
    private static final String PLANNED_EXERCISE_SESSION_STEPS_TABLE_NAME =
            "planned_exercise_session_steps_table";
    private static final String PLANNED_EXERCISE_SESSION_GOALS_TABLE_NAME =
            "planned_exercise_session_goals_table";

    // Planned exercise record columns.
    private static final String NOTES_COLUMN_NAME = "notes";
    private static final String EXERCISE_TYPE_COLUMN_NAME = "exercise_type";
    private static final String TITLE_COLUMN_NAME = "title";
    private static final String HAS_EXPLICIT_TIME_COLUMN_NAME = "has_explicit_time";
    public static final String COMPLETED_SESSION_ID_COLUMN_NAME = "completed_session_id";

    // Exercise block columns.
    private static final String BLOCK_ROW_ID_COLUMN_NAME = "block_row_id";
    private static final String BLOCK_PARENT_ID_COLUMN_NAME = "block_parent_id";
    private static final String BLOCK_DESCRIPTION_COLUMN_NAME = "block_description";
    private static final String BLOCK_REPETITIONS_COLUMN_NAME = "repetitions";

    // Exercise step columns.
    private static final String STEP_ROW_ID_COLUMN_NAME = "step_row_id";
    private static final String STEP_PARENT_ID_COLUMN_NAME = "step_parent_id";
    private static final String STEP_DESCRIPTION_COLUMN_NAME = "step_description";
    private static final String STEP_CATEGORY_COLUMN_NAME = "category";
    private static final String STEP_EXERCISE_TYPE_COLUMN_NAME = "step_exercise_type";

    // Exercise goal columns.
    private static final String GOAL_ROW_ID_COLUMN_NAME = "goal_row_id";
    private static final String GOAL_PARENT_ID_COLUMN_NAME = "goal_parent_id";
    private static final String GOAL_TYPE_ID_COLUMN_NAME = "type_id";
    private static final String GOAL_MIN_COLUMN_NAME = "goal_min";
    private static final String GOAL_MAX_COLUMN_NAME = "goal_max";

    public PlannedExerciseSessionRecordHelper() {
        super(RecordTypeIdentifier.RECORD_TYPE_PLANNED_EXERCISE_SESSION);
    }

    /** Returns the table name to be created corresponding to this helper */
    @Override
    public String getMainTableName() {
        return PLANNED_EXERCISE_SESSION_RECORD_TABLE_NAME;
    }

    @Override
    protected List<Pair<String, String>> getIntervalRecordColumnInfo() {
        return Arrays.asList(
                new Pair<>(NOTES_COLUMN_NAME, TEXT_NULL),
                new Pair<>(EXERCISE_TYPE_COLUMN_NAME, INTEGER),
                new Pair<>(TITLE_COLUMN_NAME, TEXT_NULL),
                new Pair<>(HAS_EXPLICIT_TIME_COLUMN_NAME, INTEGER));
        // We add the completed exercise session ID column  separately as it has a foreign key
        // relationship with a different table.
    }

    /** Adds the required table for planned exercise sessions. */
    public AlterTableRequest getAlterTableRequestForPlannedExerciseFeature() {
        List<Pair<String, String>> columnInfo = new ArrayList<>();
        columnInfo.add(new Pair<>(COMPLETED_SESSION_ID_COLUMN_NAME, BLOB_NULL));
        AlterTableRequest result = new AlterTableRequest(getMainTableName(), columnInfo);
        result.addForeignKeyConstraint(
                COMPLETED_SESSION_ID_COLUMN_NAME,
                EXERCISE_SESSION_RECORD_TABLE_NAME,
                UUID_COLUMN_NAME);
        return result;
    }

    @Override
    List<CreateTableRequest> getChildTableCreateRequests() {
        return Arrays.asList(
                new CreateTableRequest(
                                PLANNED_EXERCISE_SESSION_BLOCKS_TABLE_NAME,
                                Arrays.asList(
                                        new Pair<>(BLOCK_ROW_ID_COLUMN_NAME, PRIMARY_AUTOINCREMENT),
                                        new Pair<>(BLOCK_PARENT_ID_COLUMN_NAME, INTEGER_NOT_NULL),
                                        new Pair<>(BLOCK_DESCRIPTION_COLUMN_NAME, TEXT_NULL),
                                        new Pair<>(
                                                BLOCK_REPETITIONS_COLUMN_NAME, INTEGER_NOT_NULL)))
                        .addForeignKey(
                                PLANNED_EXERCISE_SESSION_RECORD_TABLE_NAME,
                                Collections.singletonList(BLOCK_PARENT_ID_COLUMN_NAME),
                                Collections.singletonList(PRIMARY_COLUMN_NAME)),
                new CreateTableRequest(
                                PLANNED_EXERCISE_SESSION_STEPS_TABLE_NAME,
                                Arrays.asList(
                                        new Pair<>(STEP_ROW_ID_COLUMN_NAME, PRIMARY_AUTOINCREMENT),
                                        new Pair<>(STEP_PARENT_ID_COLUMN_NAME, INTEGER_NOT_NULL),
                                        new Pair<>(STEP_DESCRIPTION_COLUMN_NAME, TEXT_NULL),
                                        new Pair<>(STEP_CATEGORY_COLUMN_NAME, INTEGER_NOT_NULL),
                                        new Pair<>(
                                                STEP_EXERCISE_TYPE_COLUMN_NAME, INTEGER_NOT_NULL)))
                        .addForeignKey(
                                PLANNED_EXERCISE_SESSION_BLOCKS_TABLE_NAME,
                                Collections.singletonList(STEP_PARENT_ID_COLUMN_NAME),
                                Collections.singletonList(BLOCK_ROW_ID_COLUMN_NAME)),
                new CreateTableRequest(
                                PLANNED_EXERCISE_SESSION_GOALS_TABLE_NAME,
                                Arrays.asList(
                                        new Pair<>(GOAL_ROW_ID_COLUMN_NAME, PRIMARY_AUTOINCREMENT),
                                        new Pair<>(GOAL_PARENT_ID_COLUMN_NAME, INTEGER_NOT_NULL),
                                        new Pair<>(GOAL_TYPE_ID_COLUMN_NAME, INTEGER_NOT_NULL),
                                        new Pair<>(GOAL_MIN_COLUMN_NAME, BLOB),
                                        new Pair<>(GOAL_MAX_COLUMN_NAME, BLOB)))
                        .addForeignKey(
                                PLANNED_EXERCISE_SESSION_STEPS_TABLE_NAME,
                                Collections.singletonList(GOAL_PARENT_ID_COLUMN_NAME),
                                Collections.singletonList(STEP_ROW_ID_COLUMN_NAME)));
    }

    @Override
    SqlJoin getJoinForReadRequest() {
        return new SqlJoin(
                        PLANNED_EXERCISE_SESSION_RECORD_TABLE_NAME,
                        PLANNED_EXERCISE_SESSION_BLOCKS_TABLE_NAME,
                        PRIMARY_COLUMN_NAME,
                        BLOCK_PARENT_ID_COLUMN_NAME)
                .setJoinType(SqlJoin.SQL_JOIN_LEFT)
                .attachJoin(
                        new SqlJoin(
                                        PLANNED_EXERCISE_SESSION_BLOCKS_TABLE_NAME,
                                        PLANNED_EXERCISE_SESSION_STEPS_TABLE_NAME,
                                        BLOCK_ROW_ID_COLUMN_NAME,
                                        STEP_PARENT_ID_COLUMN_NAME)
                                .setJoinType(SqlJoin.SQL_JOIN_LEFT))
                .attachJoin(
                        new SqlJoin(
                                        PLANNED_EXERCISE_SESSION_STEPS_TABLE_NAME,
                                        PLANNED_EXERCISE_SESSION_GOALS_TABLE_NAME,
                                        STEP_ROW_ID_COLUMN_NAME,
                                        GOAL_PARENT_ID_COLUMN_NAME)
                                .setJoinType(SqlJoin.SQL_JOIN_LEFT));
    }

    @Override
    PlannedExerciseSessionRecordInternal populateSpecificRecordValue(Cursor cursor) {
        PlannedExerciseSessionRecordInternal plannedExerciseSessionRecord =
                new PlannedExerciseSessionRecordInternal(extractBlocks(cursor));
        plannedExerciseSessionRecord.setNotes(getCursorString(cursor, NOTES_COLUMN_NAME));
        plannedExerciseSessionRecord.setExerciseType(
                getCursorInt(cursor, EXERCISE_TYPE_COLUMN_NAME));
        plannedExerciseSessionRecord.setTitle(getCursorString(cursor, TITLE_COLUMN_NAME));
        plannedExerciseSessionRecord.setHasExplicitTime(
                getCursorInt(cursor, HAS_EXPLICIT_TIME_COLUMN_NAME) != BOOLEAN_FALSE_VALUE);
        if (!StorageUtils.isNullValue(cursor, COMPLETED_SESSION_ID_COLUMN_NAME)) {
            plannedExerciseSessionRecord.setCompletedExerciseSessionId(
                    getCursorUUID(cursor, COMPLETED_SESSION_ID_COLUMN_NAME));
        }
        return plannedExerciseSessionRecord;
    }

    private List<PlannedExerciseBlockInternal> extractBlocks(Cursor cursor) {
        // In the case where there are *no* blocks in a planned session, the joined columns from the
        // blocks table will be null.
        if (cursor.isNull(cursor.getColumnIndex(BLOCK_REPETITIONS_COLUMN_NAME))) {
            return Collections.emptyList();
        }
        List<PlannedExerciseBlockInternal> result = new ArrayList<>();
        UUID uuid = getCursorUUID(cursor, UUID_COLUMN_NAME);
        do {
            // Populate blocks from each row.
            PlannedExerciseBlockInternal block =
                    new PlannedExerciseBlockInternal(
                            getCursorInt(cursor, BLOCK_REPETITIONS_COLUMN_NAME));
            block.setDescription(getCursorString(cursor, BLOCK_DESCRIPTION_COLUMN_NAME));
            block.setExerciseSteps(extractSteps(cursor));
            result.add(block);
        } while (cursor.moveToNext() && uuid.equals(getCursorUUID(cursor, UUID_COLUMN_NAME)));
        // In case we hit another record, move the cursor back to read next record in outer
        // RecordHelper#getInternalRecords loop.
        cursor.moveToPrevious();
        return result;
    }

    private List<PlannedExerciseStepInternal> extractSteps(Cursor cursor) {
        // In the case where there are *no* steps in a block, the joined columns from the steps
        // table will be null.
        if (cursor.isNull(cursor.getColumnIndex(STEP_EXERCISE_TYPE_COLUMN_NAME))) {
            return Collections.emptyList();
        }
        List<PlannedExerciseStepInternal> result = new ArrayList<>();
        long currentBlockId = getCursorInt(cursor, BLOCK_ROW_ID_COLUMN_NAME);
        do {
            long currentStepId = getCursorInt(cursor, STEP_ROW_ID_COLUMN_NAME);
            // Populate steps from each row.
            PlannedExerciseStepInternal step =
                    new PlannedExerciseStepInternal(
                            getCursorInt(cursor, STEP_EXERCISE_TYPE_COLUMN_NAME),
                            getCursorInt(cursor, STEP_CATEGORY_COLUMN_NAME),
                            extractCompletionGoal(cursor));
            step.setDescription(getCursorString(cursor, STEP_DESCRIPTION_COLUMN_NAME));
            List<ExercisePerformanceGoalInternal> performanceGoals = new ArrayList<>();
            while (cursor.moveToNext()
                    && getCursorInt(cursor, STEP_ROW_ID_COLUMN_NAME) == currentStepId) {
                performanceGoals.add(extractPerformanceGoal(cursor));
            }
            step.setPerformanceGoals(performanceGoals);
            cursor.moveToPrevious();
            result.add(step);
        } while (cursor.moveToNext()
                && currentBlockId == getCursorInt(cursor, STEP_PARENT_ID_COLUMN_NAME));
        // In case we hit another block, move the cursor back to current block.
        cursor.moveToPrevious();
        return result;
    }

    private ExerciseCompletionGoalInternal extractCompletionGoal(Cursor cursor) {
        int goalTypeId = getCursorInt(cursor, GOAL_TYPE_ID_COLUMN_NAME);
        switch (goalTypeId) {
            case UnspecifiedGoalInternal.UNSPECIFIED_GOAL_TYPE_ID:
                return UnspecifiedGoalInternal.INSTANCE;
            case DistanceGoalInternal.DISTANCE_GOAL_TYPE_ID:
                return new DistanceGoalInternal(
                        Length.fromMeters(
                                convertBytesToDouble(getCursorBlob(cursor, GOAL_MIN_COLUMN_NAME))));
            case StepsGoalInternal.STEPS_GOAL_TYPE_ID:
                return new StepsGoalInternal(
                        convertBytesToInt(getCursorBlob(cursor, GOAL_MIN_COLUMN_NAME)));
            case DurationGoalInternal.DURATION_GOAL_TYPE_ID:
                return new DurationGoalInternal(
                        Duration.ofMillis(
                                convertBytesToLong(getCursorBlob(cursor, GOAL_MIN_COLUMN_NAME))));
            case RepetitionsGoalInternal.REPETITIONS_GOAL_TYPE_ID:
                return new RepetitionsGoalInternal(
                        convertBytesToInt(getCursorBlob(cursor, GOAL_MIN_COLUMN_NAME)));
            case TotalCaloriesBurnedGoalInternal.TOTAL_CALORIES_BURNED_GOAL_TYPE_ID:
                return new TotalCaloriesBurnedGoalInternal(
                        Energy.fromCalories(
                                convertBytesToDouble(getCursorBlob(cursor, GOAL_MIN_COLUMN_NAME))));
            case ActiveCaloriesBurnedGoalInternal.ACTIVE_CALORIES_BURNED_GOAL_TYPE_ID:
                return new ActiveCaloriesBurnedGoalInternal(
                        Energy.fromCalories(
                                convertBytesToDouble(getCursorBlob(cursor, GOAL_MIN_COLUMN_NAME))));
            case DistanceWithVariableRestGoalInternal.DISTANCE_WITH_VARIABLE_REST_GOAL_TYPE_ID:
                return extractDistanceWithVariableRestGoal(cursor);
            case ExerciseCompletionGoalInternal.UnknownGoalInternal.UNKNOWN_GOAL_TYPE_ID:
            // Fall through.
            default:
                return ExerciseCompletionGoalInternal.UnknownGoalInternal.INSTANCE;
        }
    }

    private ExercisePerformanceGoalInternal extractPerformanceGoal(Cursor cursor) {
        int goalTypeId = getCursorInt(cursor, GOAL_TYPE_ID_COLUMN_NAME);
        switch (goalTypeId) {
            case PowerGoalInternal.POWER_GOAL_TYPE_ID:
                return new PowerGoalInternal(
                        Power.fromWatts(
                                convertBytesToDouble(getCursorBlob(cursor, GOAL_MIN_COLUMN_NAME))),
                        Power.fromWatts(
                                convertBytesToDouble(getCursorBlob(cursor, GOAL_MAX_COLUMN_NAME))));
            case SpeedGoalInternal.SPEED_GOAL_TYPE_ID:
                return new SpeedGoalInternal(
                        Velocity.fromMetersPerSecond(
                                convertBytesToDouble(getCursorBlob(cursor, GOAL_MIN_COLUMN_NAME))),
                        Velocity.fromMetersPerSecond(
                                convertBytesToDouble(getCursorBlob(cursor, GOAL_MAX_COLUMN_NAME))));
            case CadenceGoalInternal.CADENCE_GOAL_TYPE_ID:
                return new CadenceGoalInternal(
                        convertBytesToDouble(getCursorBlob(cursor, GOAL_MIN_COLUMN_NAME)),
                        convertBytesToDouble(getCursorBlob(cursor, GOAL_MAX_COLUMN_NAME)));
            case HeartRateGoalInternal.HEART_RATE_GOAL_TYPE_ID:
                return new HeartRateGoalInternal(
                        convertBytesToInt(getCursorBlob(cursor, GOAL_MIN_COLUMN_NAME)),
                        convertBytesToInt(getCursorBlob(cursor, GOAL_MAX_COLUMN_NAME)));
            case WeightGoalInternal.WEIGHT_GOAL_TYPE_ID:
                return new WeightGoalInternal(
                        Mass.fromGrams(
                                convertBytesToDouble(getCursorBlob(cursor, GOAL_MIN_COLUMN_NAME))));
            case RateOfPerceivedExertionGoalInternal.RATE_OF_PERCEIVED_EXERTION_TYPE_ID:
                return new RateOfPerceivedExertionGoalInternal(
                        convertBytesToInt(getCursorBlob(cursor, GOAL_MIN_COLUMN_NAME)));
            case AmrapGoalInternal.AMRAP_GOAL_TYPE_ID:
                return AmrapGoalInternal.INSTANCE;
            case ExercisePerformanceGoalInternal.UnknownGoalInternal.UNKNOWN_GOAL_TYPE_ID:
            // Fall through.
            default:
                return ExercisePerformanceGoalInternal.UnknownGoalInternal.INSTANCE;
        }
    }

    @Override
    void populateSpecificContentValues(
            ContentValues contentValues,
            PlannedExerciseSessionRecordInternal exerciseSessionRecord) {
        contentValues.put(NOTES_COLUMN_NAME, exerciseSessionRecord.getNotes());
        contentValues.put(EXERCISE_TYPE_COLUMN_NAME, exerciseSessionRecord.getExerciseType());
        contentValues.put(TITLE_COLUMN_NAME, exerciseSessionRecord.getTitle());
        if (exerciseSessionRecord.getCompletedExerciseSessionId() != null) {
            contentValues.put(
                    COMPLETED_SESSION_ID_COLUMN_NAME,
                    exerciseSessionRecord.getCompletedExerciseSessionId().toString());
        }
        contentValues.put(
                HAS_EXPLICIT_TIME_COLUMN_NAME,
                exerciseSessionRecord.getHasExplicitTime()
                        ? BOOLEAN_TRUE_VALUE
                        : BOOLEAN_FALSE_VALUE);
    }

    @Override
    List<UpsertTableRequest> getChildTableUpsertRequests(
            PlannedExerciseSessionRecordInternal record) {
        List<UpsertTableRequest> blockUpsertRequests = new ArrayList<>();
        for (PlannedExerciseBlockInternal exerciseBlock : record.getExerciseBlocks()) {
            blockUpsertRequests.add(getBlockUpsertRequest(exerciseBlock));
        }

        return blockUpsertRequests;
    }

    private UpsertTableRequest getBlockUpsertRequest(PlannedExerciseBlockInternal exerciseBlock) {
        ContentValues blockContentValues = new ContentValues();
        blockContentValues.put(BLOCK_REPETITIONS_COLUMN_NAME, exerciseBlock.getRepetitions());
        blockContentValues.put(BLOCK_DESCRIPTION_COLUMN_NAME, exerciseBlock.getDescription());
        UpsertTableRequest blockUpsertRequest =
                new UpsertTableRequest(
                                PLANNED_EXERCISE_SESSION_BLOCKS_TABLE_NAME, blockContentValues)
                        .setParentColumnForChildTables(BLOCK_PARENT_ID_COLUMN_NAME);

        List<UpsertTableRequest> stepUpsertRequests = new ArrayList<>();
        for (PlannedExerciseStepInternal exerciseStep : exerciseBlock.getExerciseSteps()) {
            stepUpsertRequests.add(getStepUpsert(exerciseStep));
        }

        blockUpsertRequest.setChildTableRequests(stepUpsertRequests);
        return blockUpsertRequest;
    }

    private UpsertTableRequest getStepUpsert(PlannedExerciseStepInternal exerciseStep) {
        ContentValues stepContentValues = new ContentValues();
        stepContentValues.put(STEP_DESCRIPTION_COLUMN_NAME, exerciseStep.getDescription());
        stepContentValues.put(STEP_CATEGORY_COLUMN_NAME, exerciseStep.getExerciseCategory());
        stepContentValues.put(STEP_EXERCISE_TYPE_COLUMN_NAME, exerciseStep.getExerciseType());

        UpsertTableRequest stepUpsertRequest =
                new UpsertTableRequest(PLANNED_EXERCISE_SESSION_STEPS_TABLE_NAME, stepContentValues)
                        .setParentColumnForChildTables(STEP_PARENT_ID_COLUMN_NAME);

        List<UpsertTableRequest> goalUpsertRequests = new ArrayList<>();
        goalUpsertRequests.add(getCompletionGoalUpsert(exerciseStep.getCompletionGoal()));
        for (ExercisePerformanceGoalInternal performanceGoal : exerciseStep.getPerformanceGoals()) {
            goalUpsertRequests.add(getPerformanceGoalUpsert(performanceGoal));
        }
        stepUpsertRequest.setChildTableRequests(goalUpsertRequests);
        return stepUpsertRequest;
    }

    private UpsertTableRequest getCompletionGoalUpsert(
            ExerciseCompletionGoalInternal completionGoal) {

        ContentValues completionGoalContentValues = new ContentValues();
        completionGoalContentValues.put(GOAL_TYPE_ID_COLUMN_NAME, completionGoal.getTypeId());
        if (completionGoal instanceof DistanceGoalInternal) {
            completionGoalContentValues.put(
                    GOAL_MIN_COLUMN_NAME,
                    convertDoubleToBytes(
                            ((DistanceGoalInternal) completionGoal).getDistance().getInMeters()));
        } else if (completionGoal instanceof StepsGoalInternal) {
            completionGoalContentValues.put(
                    GOAL_MIN_COLUMN_NAME,
                    convertIntToBytes(((StepsGoalInternal) completionGoal).getSteps()));

        } else if (completionGoal instanceof DurationGoalInternal) {
            completionGoalContentValues.put(
                    GOAL_MIN_COLUMN_NAME,
                    convertLongToBytes(
                            ((DurationGoalInternal) completionGoal).getDuration().toMillis()));
        } else if (completionGoal instanceof RepetitionsGoalInternal) {
            completionGoalContentValues.put(
                    GOAL_MIN_COLUMN_NAME,
                    convertIntToBytes(((RepetitionsGoalInternal) completionGoal).getReps()));
        } else if (completionGoal instanceof TotalCaloriesBurnedGoalInternal) {
            completionGoalContentValues.put(
                    GOAL_MIN_COLUMN_NAME,
                    convertDoubleToBytes(
                            ((TotalCaloriesBurnedGoalInternal) completionGoal)
                                    .getTotalCalories()
                                    .getInCalories()));
        } else if (completionGoal instanceof ActiveCaloriesBurnedGoalInternal) {
            completionGoalContentValues.put(
                    GOAL_MIN_COLUMN_NAME,
                    convertDoubleToBytes(
                            ((ActiveCaloriesBurnedGoalInternal) completionGoal)
                                    .getActiveCalories()
                                    .getInCalories()));
        } else if (completionGoal instanceof DistanceWithVariableRestGoalInternal) {
            populateContentValuesForDistanceWithVariableRestGoal(
                    completionGoalContentValues,
                    (DistanceWithVariableRestGoalInternal) completionGoal);
        }
        return new UpsertTableRequest(
                        PLANNED_EXERCISE_SESSION_GOALS_TABLE_NAME, completionGoalContentValues)
                .setParentColumnForChildTables(GOAL_PARENT_ID_COLUMN_NAME);
    }

    private UpsertTableRequest getPerformanceGoalUpsert(
            ExercisePerformanceGoalInternal performanceGoal) {
        ContentValues performanceGoalContentValues = new ContentValues();
        performanceGoalContentValues.put(GOAL_TYPE_ID_COLUMN_NAME, performanceGoal.getTypeId());
        if (performanceGoal instanceof PowerGoalInternal) {
            performanceGoalContentValues.put(
                    GOAL_MIN_COLUMN_NAME,
                    convertDoubleToBytes(
                            ((PowerGoalInternal) performanceGoal).getMinPower().getInWatts()));
            performanceGoalContentValues.put(
                    GOAL_MAX_COLUMN_NAME,
                    convertDoubleToBytes(
                            ((PowerGoalInternal) performanceGoal).getMaxPower().getInWatts()));
        } else if (performanceGoal instanceof SpeedGoalInternal) {
            performanceGoalContentValues.put(
                    GOAL_MIN_COLUMN_NAME,
                    convertDoubleToBytes(
                            ((SpeedGoalInternal) performanceGoal)
                                    .getMinSpeed()
                                    .getInMetersPerSecond()));
            performanceGoalContentValues.put(
                    GOAL_MAX_COLUMN_NAME,
                    convertDoubleToBytes(
                            ((SpeedGoalInternal) performanceGoal)
                                    .getMaxSpeed()
                                    .getInMetersPerSecond()));
        } else if (performanceGoal instanceof CadenceGoalInternal) {
            performanceGoalContentValues.put(
                    GOAL_MIN_COLUMN_NAME,
                    convertDoubleToBytes(((CadenceGoalInternal) performanceGoal).getMinRpm()));
            performanceGoalContentValues.put(
                    GOAL_MAX_COLUMN_NAME,
                    convertDoubleToBytes(((CadenceGoalInternal) performanceGoal).getMaxRpm()));
        } else if (performanceGoal instanceof HeartRateGoalInternal) {
            performanceGoalContentValues.put(
                    GOAL_MIN_COLUMN_NAME,
                    convertIntToBytes(((HeartRateGoalInternal) performanceGoal).getMinBpm()));
            performanceGoalContentValues.put(
                    GOAL_MAX_COLUMN_NAME,
                    convertIntToBytes(((HeartRateGoalInternal) performanceGoal).getMaxBpm()));
        } else if (performanceGoal instanceof WeightGoalInternal) {
            performanceGoalContentValues.put(
                    GOAL_MIN_COLUMN_NAME,
                    convertDoubleToBytes(
                            ((WeightGoalInternal) performanceGoal).getMass().getInGrams()));
        } else if (performanceGoal instanceof RateOfPerceivedExertionGoalInternal) {
            performanceGoalContentValues.put(
                    GOAL_MIN_COLUMN_NAME,
                    convertIntToBytes(
                            ((RateOfPerceivedExertionGoalInternal) performanceGoal).getRpe()));
        }
        return new UpsertTableRequest(
                        PLANNED_EXERCISE_SESSION_GOALS_TABLE_NAME, performanceGoalContentValues)
                .setParentColumnForChildTables(GOAL_PARENT_ID_COLUMN_NAME);
    }

    private static DistanceWithVariableRestGoalInternal extractDistanceWithVariableRestGoal(
            Cursor cursor) {
        byte[] bytes = getCursorBlob(cursor, GOAL_MIN_COLUMN_NAME);
        Length distance =
                Length.fromMeters(convertBytesToDouble(Arrays.copyOfRange(bytes, 0, Double.BYTES)));
        Duration duration =
                Duration.ofMillis(
                        convertBytesToLong(
                                Arrays.copyOfRange(
                                        bytes, Double.BYTES, Double.BYTES + Long.BYTES)));
        return new DistanceWithVariableRestGoalInternal(distance, duration);
    }

    private static void populateContentValuesForDistanceWithVariableRestGoal(
            ContentValues contentValues,
            DistanceWithVariableRestGoalInternal distanceWithVariableRestGoalInternal) {
        byte[] distanceBytes =
                convertDoubleToBytes(
                        distanceWithVariableRestGoalInternal.getDistance().getInMeters());
        byte[] durationBytes =
                convertLongToBytes(distanceWithVariableRestGoalInternal.getDuration().toMillis());
        byte[] bytes = new byte[16];
        System.arraycopy(distanceBytes, 0, bytes, 0, Double.BYTES);
        System.arraycopy(durationBytes, 0, bytes, Double.BYTES, Long.BYTES);
        contentValues.put(GOAL_MIN_COLUMN_NAME, bytes);
    }

    @Override
    public List<TableColumnPair> getChildTablesWithRowsToBeDeletedDuringUpdate(
            Set<String> grantedPerRecordWritePermissions) {
        // Children of the block table will get automatically deleted via cascades.
        return Collections.singletonList(
                new TableColumnPair(
                        PLANNED_EXERCISE_SESSION_BLOCKS_TABLE_NAME, BLOCK_PARENT_ID_COLUMN_NAME));
    }

    @Override
    public List<RecordReadTableRequest> getReadRequestsForRecordsModifiedByDeletion(
            UUID deletedRecordUuid) {
        ReadTableRequest affectedExerciseSessionsReadRequest =
                new ReadTableRequest(EXERCISE_SESSION_RECORD_TABLE_NAME);
        affectedExerciseSessionsReadRequest.setColumnNames(
                Arrays.asList(
                        UUID_COLUMN_NAME,
                        APP_INFO_ID_COLUMN_NAME,
                        PLANNED_EXERCISE_SESSION_ID_COLUMN_NAME));
        WhereClauses whereStatement = new WhereClauses(WhereClauses.LogicalOperator.AND);
        whereStatement.addWhereEqualsClause(
                PLANNED_EXERCISE_SESSION_ID_COLUMN_NAME,
                StorageUtils.getHexString(deletedRecordUuid));
        affectedExerciseSessionsReadRequest.setWhereClause(whereStatement);
        return Collections.singletonList(
                new RecordReadTableRequest(
                        affectedExerciseSessionsReadRequest,
                        InternalHealthConnectMappings.getInstance()
                                .getRecordHelper(RECORD_TYPE_EXERCISE_SESSION)));
    }
}
