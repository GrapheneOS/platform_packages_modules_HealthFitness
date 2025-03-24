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

package android.healthconnect.cts.datatypes;

import static android.healthconnect.cts.utils.DataFactory.SESSION_END_TIME;
import static android.healthconnect.cts.utils.DataFactory.SESSION_START_TIME;
import static android.healthconnect.cts.utils.TestUtils.insertRecord;
import static android.healthconnect.cts.utils.TestUtils.insertRecordAndGetId;
import static android.healthconnect.cts.utils.TestUtils.insertRecords;
import static android.healthconnect.cts.utils.TestUtils.readAllRecords;
import static android.healthconnect.cts.utils.TestUtils.verifyDeleteRecords;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import static java.time.Month.APRIL;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;

import android.health.connect.HealthConnectException;
import android.health.connect.RecordIdFilter;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.changelog.ChangeLogTokenRequest;
import android.health.connect.changelog.ChangeLogsRequest;
import android.health.connect.changelog.ChangeLogsResponse;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.ExerciseCompletionGoal;
import android.health.connect.datatypes.ExercisePerformanceGoal;
import android.health.connect.datatypes.ExerciseSessionRecord;
import android.health.connect.datatypes.ExerciseSessionType;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.PlannedExerciseBlock;
import android.health.connect.datatypes.PlannedExerciseSessionRecord;
import android.health.connect.datatypes.PlannedExerciseStep;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.units.Length;
import android.health.connect.datatypes.units.Mass;
import android.health.connect.datatypes.units.Power;
import android.healthconnect.cts.lib.TestAppProxy;
import android.healthconnect.cts.utils.AssumptionCheckerRule;
import android.healthconnect.cts.utils.DeviceSupportUtils;
import android.healthconnect.cts.utils.TestUtils;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.common.collect.Iterables;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@RunWith(AndroidJUnit4.class)
public class PlannedExerciseSessionRecordTest {
    private static final String PKG_TEST_APP = "android.healthconnect.cts.testapp.readWritePerms.A";

    private TestAppProxy mTestApp;

    @Rule
    public AssumptionCheckerRule mSupportedHardwareRule =
            new AssumptionCheckerRule(
                    DeviceSupportUtils::isHealthConnectFullySupported,
                    "Tests should run on supported hardware only.");

    @Before
    public void setUp() throws InterruptedException {
        mTestApp = TestAppProxy.forPackageName(PKG_TEST_APP);
        TestUtils.deleteAllStagedRemoteData();
    }

    @After
    public void tearDown() throws InterruptedException {
        verifyDeleteRecords(
                PlannedExerciseSessionRecord.class,
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.EPOCH)
                        .setEndTime(Instant.now())
                        .build());
        verifyDeleteRecords(
                ExerciseSessionRecord.class,
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.EPOCH)
                        .setEndTime(Instant.now())
                        .build());
        TestUtils.deleteAllStagedRemoteData();
    }

    @Test
    public void insertRecord_startTimeInFuture_bypassesFutureTimeRestrictions()
            throws InterruptedException {
        PlannedExerciseSessionRecord.Builder builder =
                basePlannedExerciseSession(ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING)
                        .setStartTime(Instant.now().plus(30, DAYS))
                        .setEndTime(Instant.now().plus(30, DAYS).plus(1, HOURS));
        PlannedExerciseBlock.Builder blockBuilder = new PlannedExerciseBlock.Builder(3);
        blockBuilder.setDescription("Some description");

        Record insertedRecord = verifyInsertSucceeds(builder.build());

        verifyReadReturnsSameRecords(insertedRecord);
    }

    @Test
    public void insertRecord_noBlocks() throws InterruptedException {
        PlannedExerciseSessionRecord.Builder builder =
                basePlannedExerciseSession(ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING);
        PlannedExerciseBlock.Builder blockBuilder = new PlannedExerciseBlock.Builder(3);
        blockBuilder.setDescription("Some description");

        Record insertedRecord = verifyInsertSucceeds(builder.build());

        verifyReadReturnsSameRecords(insertedRecord);
    }

    @Test
    public void insertRecord_singleBlock() throws InterruptedException {
        PlannedExerciseSessionRecord.Builder builder =
                basePlannedExerciseSession(ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING);
        PlannedExerciseBlock.Builder blockBuilder = new PlannedExerciseBlock.Builder(3);
        blockBuilder.setDescription("Some description");
        builder.addBlock(blockBuilder.build());

        Record insertedRecord = verifyInsertSucceeds(builder.build());

        verifyReadReturnsSameRecords(insertedRecord);
    }

    @Test
    public void insertRecord_multipleBlocks() throws InterruptedException {
        PlannedExerciseSessionRecord.Builder builder =
                basePlannedExerciseSession(ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING);
        PlannedExerciseBlock.Builder blockBuilder = new PlannedExerciseBlock.Builder(3);
        blockBuilder.setDescription("Some description");
        builder.addBlock(blockBuilder.build());
        blockBuilder.setDescription("Another description");
        builder.addBlock(blockBuilder.build());

        Record insertedRecord = verifyInsertSucceeds(builder.build());

        System.err.println("DMCK: inserted record UUID: " + insertedRecord.getMetadata().getId());

        PlannedExerciseSessionRecord insertedPlan =
                Iterables.getOnlyElement(readAllRecords(PlannedExerciseSessionRecord.class));
        System.err.println("DMCK: read back record UUID: " + insertedPlan.getMetadata().getId());
        verifyReadReturnsSameRecords(insertedRecord);
    }

    @Test
    public void insertRecord_singleStep() throws InterruptedException {
        PlannedExerciseSessionRecord.Builder builder =
                basePlannedExerciseSession(ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING);
        PlannedExerciseBlock.Builder blockBuilder = new PlannedExerciseBlock.Builder(3);
        blockBuilder.setDescription("Block one");
        PlannedExerciseStep.Builder stepBuilder = baseExerciseStep();
        stepBuilder.setDescription("Step one");
        stepBuilder.addPerformanceGoal(
                new ExercisePerformanceGoal.PowerGoal(Power.fromWatts(200), Power.fromWatts(240)));
        blockBuilder.addStep(stepBuilder.build());
        builder.addBlock(blockBuilder.build());
        blockBuilder.clearSteps();
        blockBuilder.setDescription("Block two");
        stepBuilder.setDescription("Step two");
        blockBuilder.addStep(stepBuilder.build());
        builder.addBlock(blockBuilder.build());

        Record insertedRecord = verifyInsertSucceeds(builder.build());

        verifyReadReturnsSameRecords(insertedRecord);
    }

    @Test
    public void insertRecord_multipleSteps() throws InterruptedException {
        PlannedExerciseSessionRecord.Builder builder =
                basePlannedExerciseSession(ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING);
        PlannedExerciseBlock.Builder blockBuilder = new PlannedExerciseBlock.Builder(3);
        blockBuilder.setDescription("Block one");
        PlannedExerciseStep.Builder stepBuilder = baseExerciseStep();
        stepBuilder.setCompletionGoal(new ExerciseCompletionGoal.StepsGoal(2000));
        stepBuilder.setDescription("Step one");
        stepBuilder.addPerformanceGoal(
                new ExercisePerformanceGoal.PowerGoal(Power.fromWatts(200), Power.fromWatts(240)));
        stepBuilder.addPerformanceGoal(
                new ExercisePerformanceGoal.WeightGoal(Mass.fromGrams(10_000)));
        blockBuilder.addStep(stepBuilder.build());
        blockBuilder.addStep(stepBuilder.build());
        builder.addBlock(blockBuilder.build());
        blockBuilder.clearSteps();
        blockBuilder.setDescription("Block two");
        blockBuilder.addStep(stepBuilder.setDescription("Step one").build());
        blockBuilder.addStep(stepBuilder.setDescription("Step two").build());
        blockBuilder.addStep(stepBuilder.setDescription("Step three").build());
        builder.addBlock(blockBuilder.build());

        Record insertedRecord = verifyInsertSucceeds(builder.build());

        verifyReadReturnsSameRecords(insertedRecord);
    }

    @Test
    public void insertMultipleRecords() throws InterruptedException {
        PlannedExerciseSessionRecord record1 =
                basePlannedExerciseSession(ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING)
                        .build();
        PlannedExerciseSessionRecord record2 =
                basePlannedExerciseSession(ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING)
                        .setStartTime(SESSION_START_TIME.plus(2, DAYS))
                        .setEndTime(SESSION_END_TIME.plus(2, DAYS))
                        .build();

        List<Record> insertedRecords = verifyInsertSucceeds(Arrays.asList(record1, record2));

        verifyReadReturnsSameRecords(insertedRecords);
    }

    @Test
    public void updateTrainingPlan() throws InterruptedException {
        PlannedExerciseSessionRecord.Builder builder =
                basePlannedExerciseSession(ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING);
        PlannedExerciseBlock.Builder blockBuilder = new PlannedExerciseBlock.Builder(3);
        blockBuilder.setDescription("Block one");
        PlannedExerciseStep.Builder stepBuilder = baseExerciseStep();
        stepBuilder.setCompletionGoal(new ExerciseCompletionGoal.StepsGoal(2000));
        stepBuilder.setDescription("Step one");
        stepBuilder.addPerformanceGoal(
                new ExercisePerformanceGoal.PowerGoal(Power.fromWatts(200), Power.fromWatts(240)));
        stepBuilder.addPerformanceGoal(
                new ExercisePerformanceGoal.WeightGoal(Mass.fromGrams(10_000)));
        blockBuilder.addStep(stepBuilder.build());
        blockBuilder.addStep(stepBuilder.build());
        builder.addBlock(blockBuilder.build());
        blockBuilder.clearSteps();
        blockBuilder.setDescription("Block two");
        blockBuilder.addStep(stepBuilder.setDescription("Step one").build());
        blockBuilder.addStep(stepBuilder.setDescription("Step two").build());
        blockBuilder.addStep(stepBuilder.setDescription("Step three").build());
        builder.addBlock(blockBuilder.build());

        Record insertedRecord = verifyInsertSucceeds(builder.build());
        verifyReadReturnsSameRecords(insertedRecord);

        builder.setMetadata(insertedRecord.getMetadata());
        builder.setTitle("Updated training plan");
        blockBuilder.setDescription("Updated block description");
        stepBuilder.setDescription("Updated step");
        blockBuilder.setSteps(Collections.singletonList(stepBuilder.build()));
        builder.setBlocks(Collections.singletonList(blockBuilder.build()));

        TestUtils.updateRecords(Collections.singletonList(builder.build()));
        verifyReadReturnsSameRecords(builder.build());
    }

    @Test
    public void updateExerciseSession_dropTheReferenceToTrainingPlan_trainingPlanAlsoUpdated()
            throws InterruptedException {
        // Insert a training plan.
        PlannedExerciseSessionRecord.Builder builder =
                basePlannedExerciseSession(ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING);
        PlannedExerciseBlock.Builder blockBuilder = new PlannedExerciseBlock.Builder(3);
        blockBuilder.setDescription("Some description");

        Record insertedPlannedSession = verifyInsertSucceeds(builder.build());

        verifyReadReturnsSameRecords(insertedPlannedSession);

        // Now insert an exercise session that completes it.
        ExerciseSessionRecord.Builder exerciseSession =
                new ExerciseSessionRecord.Builder(
                                buildMetadata(null),
                                SESSION_START_TIME,
                                SESSION_END_TIME,
                                ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING)
                        .setPlannedExerciseSessionId(insertedPlannedSession.getMetadata().getId());
        insertRecordAndGetId(exerciseSession.build());

        // Now update the exercise session to drop the reference to the training plan.
        ExerciseSessionRecord insertedExerciseSession =
                Iterables.getOnlyElement(readAllRecords(ExerciseSessionRecord.class));
        exerciseSession =
                new ExerciseSessionRecord.Builder(
                                buildMetadataWithUuid(
                                        insertedExerciseSession.getMetadata().getId()),
                                SESSION_START_TIME,
                                SESSION_END_TIME,
                                ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING)
                        .setPlannedExerciseSessionId(null);
        TestUtils.updateRecords(Collections.singletonList(exerciseSession.build()));
        ExerciseSessionRecord updatedExerciseSession =
                Iterables.getOnlyElement(readAllRecords(ExerciseSessionRecord.class));
        assertThat(updatedExerciseSession.getPlannedExerciseSessionId()).isNull();
        // The training plan's reference to this exercise session should also be nullified.
        PlannedExerciseSessionRecord updatedPlannedSession =
                Iterables.getOnlyElement(readAllRecords(PlannedExerciseSessionRecord.class));
        assertThat(updatedPlannedSession.getCompletedExerciseSessionId()).isNull();
    }

    @Test
    public void updateExerciseSession_introduceReferenceToTrainingPlan_trainingPlanAlsoUpdated()
            throws InterruptedException {
        // Insert a training plan.
        PlannedExerciseSessionRecord.Builder builder =
                basePlannedExerciseSession(ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING);
        PlannedExerciseBlock.Builder blockBuilder = new PlannedExerciseBlock.Builder(3);
        blockBuilder.setDescription("Some description");

        Record insertedPlannedSession = verifyInsertSucceeds(builder.build());

        verifyReadReturnsSameRecords(insertedPlannedSession);

        // Now insert an exercise session, but don't complete the training plan.
        ExerciseSessionRecord.Builder exerciseSession =
                new ExerciseSessionRecord.Builder(
                        buildMetadata(null),
                        SESSION_START_TIME,
                        SESSION_END_TIME,
                        ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING);
        insertRecordAndGetId(exerciseSession.build());

        // Now update the exercise session to include a reference to the training plan.
        ExerciseSessionRecord insertedExerciseSession =
                Iterables.getOnlyElement(readAllRecords(ExerciseSessionRecord.class));
        exerciseSession =
                new ExerciseSessionRecord.Builder(
                                buildMetadataWithUuid(
                                        insertedExerciseSession.getMetadata().getId()),
                                SESSION_START_TIME,
                                SESSION_END_TIME,
                                ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING)
                        .setPlannedExerciseSessionId(insertedPlannedSession.getMetadata().getId());
        TestUtils.updateRecords(Collections.singletonList(exerciseSession.build()));
        ExerciseSessionRecord updatedExerciseSession =
                Iterables.getOnlyElement(readAllRecords(ExerciseSessionRecord.class));
        assertThat(updatedExerciseSession.getPlannedExerciseSessionId()).isNotNull();
        // The training plan's reference to this exercise session should be non-null.
        PlannedExerciseSessionRecord updatedPlannedSession =
                Iterables.getOnlyElement(readAllRecords(PlannedExerciseSessionRecord.class));
        assertThat(updatedPlannedSession.getCompletedExerciseSessionId())
                .isEqualTo(updatedExerciseSession.getMetadata().getId());
    }

    @Test
    public void deleteRecords() throws InterruptedException {
        PlannedExerciseSessionRecord.Builder builder =
                basePlannedExerciseSession(ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING);
        PlannedExerciseBlock.Builder blockBuilder = new PlannedExerciseBlock.Builder(3);
        blockBuilder.setDescription("Block one");
        PlannedExerciseStep.Builder stepBuilder = baseExerciseStep();
        stepBuilder.setCompletionGoal(new ExerciseCompletionGoal.StepsGoal(2000));
        stepBuilder.setDescription("Step one");
        stepBuilder.addPerformanceGoal(
                new ExercisePerformanceGoal.PowerGoal(Power.fromWatts(200), Power.fromWatts(240)));
        stepBuilder.addPerformanceGoal(
                new ExercisePerformanceGoal.WeightGoal(Mass.fromGrams(10_000)));
        blockBuilder.addStep(stepBuilder.build());
        blockBuilder.addStep(stepBuilder.build());
        builder.addBlock(blockBuilder.build());
        blockBuilder.clearSteps();
        blockBuilder.setDescription("Block two");
        blockBuilder.addStep(stepBuilder.setDescription("Step one").build());
        blockBuilder.addStep(stepBuilder.setDescription("Step two").build());
        blockBuilder.addStep(stepBuilder.setDescription("Step three").build());
        builder.addBlock(blockBuilder.build());

        Record inserted = verifyInsertSucceeds(builder.build());

        verifyDeleteRecords(
                Arrays.asList(
                        RecordIdFilter.fromId(
                                PlannedExerciseSessionRecord.class,
                                inserted.getMetadata().getId())));
    }

    @Test
    public void insertSession_sessionReferencesPlannedExerciseSession_idReferencesCreatedBothWays()
            throws InterruptedException {
        PlannedExerciseSessionRecord plannedSession =
                basePlannedExerciseSession(ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING)
                        .build();
        Record insertedPlannedSession = verifyInsertSucceeds(plannedSession);
        ExerciseSessionRecord exerciseSession =
                new ExerciseSessionRecord.Builder(
                                buildMetadata(null),
                                SESSION_START_TIME,
                                SESSION_END_TIME,
                                ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING)
                        .setPlannedExerciseSessionId(insertedPlannedSession.getMetadata().getId())
                        .build();

        String exerciseSessionId = insertRecordAndGetId(exerciseSession);
        ExerciseSessionRecord insertedExerciseSession =
                Iterables.getOnlyElement(readAllRecords(ExerciseSessionRecord.class));
        assertThat(insertedExerciseSession.getPlannedExerciseSessionId())
                .isEqualTo(insertedPlannedSession.getMetadata().getId());
        PlannedExerciseSessionRecord updatedPlannedSession =
                Iterables.getOnlyElement(readAllRecords(PlannedExerciseSessionRecord.class));

        assertThat(updatedPlannedSession.getCompletedExerciseSessionId()).isNotNull();
        assertThat(updatedPlannedSession.getCompletedExerciseSessionId())
                .isEqualTo(exerciseSessionId);
    }

    @Test(expected = HealthConnectException.class)
    public void insertSession_referenceToNonExistentTrainingPlan_insertionFails()
            throws InterruptedException {
        String someMadeUpUuid = new UUID(42L, 42L).toString();
        ExerciseSessionRecord exerciseSession =
                new ExerciseSessionRecord.Builder(
                                buildMetadata(null),
                                SESSION_START_TIME,
                                SESSION_END_TIME,
                                ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING)
                        .setPlannedExerciseSessionId(someMadeUpUuid)
                        .build();

        // Fails with "Conflict found, but couldn't read the entry".
        insertRecords(Collections.singletonList(exerciseSession));
    }

    @Test
    public void bidirectionalReference_nullifiedWhenTrainingPlanDeleted()
            throws InterruptedException {
        PlannedExerciseSessionRecord plannedSession =
                basePlannedExerciseSession(ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING)
                        .build();
        Record insertedPlannedSession = verifyInsertSucceeds(plannedSession);
        ExerciseSessionRecord exerciseSession =
                new ExerciseSessionRecord.Builder(
                                buildMetadata(null),
                                SESSION_START_TIME,
                                SESSION_END_TIME,
                                ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING)
                        .setPlannedExerciseSessionId(insertedPlannedSession.getMetadata().getId())
                        .build();

        insertRecords(Collections.singletonList(exerciseSession));
        verifyDeleteRecords(
                PlannedExerciseSessionRecord.class,
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.EPOCH)
                        .setEndTime(Instant.now())
                        .build());
        ExerciseSessionRecord updatedExerciseSession =
                Iterables.getOnlyElement(readAllRecords(ExerciseSessionRecord.class));
        assertThat(updatedExerciseSession.getPlannedExerciseSessionId()).isNull();
    }

    @Test
    public void bidirectionalReference_keptWhenTrainingPlanUpdated() throws InterruptedException {
        PlannedExerciseSessionRecord.Builder plannedSession =
                basePlannedExerciseSession(ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING);
        PlannedExerciseSessionRecord insertedPlannedSession =
                (PlannedExerciseSessionRecord) verifyInsertSucceeds(plannedSession.build());
        ExerciseSessionRecord exerciseSession =
                new ExerciseSessionRecord.Builder(
                                buildMetadata(null),
                                SESSION_START_TIME,
                                SESSION_END_TIME,
                                ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING)
                        .setPlannedExerciseSessionId(insertedPlannedSession.getMetadata().getId())
                        .build();

        insertRecords(Collections.singletonList(exerciseSession));

        plannedSession = plannedExerciseSessionRecordToBuilder(insertedPlannedSession);
        plannedSession.setTitle("Updated training plan");
        TestUtils.updateRecords(Collections.singletonList(plannedSession.build()));
        ExerciseSessionRecord updatedExerciseSession =
                Iterables.getOnlyElement(readAllRecords(ExerciseSessionRecord.class));
        assertThat(updatedExerciseSession.getPlannedExerciseSessionId())
                .isEqualTo(insertedPlannedSession.getMetadata().getId());
    }

    @Test
    public void bidirectionalReference_keptWhenExerciseSessionUpdated()
            throws InterruptedException {
        PlannedExerciseSessionRecord.Builder plannedSession =
                basePlannedExerciseSession(ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING);
        Record insertedPlannedSession = verifyInsertSucceeds(plannedSession.build());
        ExerciseSessionRecord.Builder exerciseSession =
                new ExerciseSessionRecord.Builder(
                                buildMetadata(null),
                                SESSION_START_TIME,
                                SESSION_END_TIME,
                                ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING)
                        .setPlannedExerciseSessionId(insertedPlannedSession.getMetadata().getId());

        exerciseSession =
                exerciseSessionRecordToBuilder(
                        (ExerciseSessionRecord) insertRecord(exerciseSession.build()));

        exerciseSession.setTitle("Updated exercise session");
        TestUtils.updateRecords(Collections.singletonList(exerciseSession.build()));
        ExerciseSessionRecord updatedExerciseSession =
                Iterables.getOnlyElement(readAllRecords(ExerciseSessionRecord.class));
        PlannedExerciseSessionRecord updatedTrainingPlan =
                Iterables.getOnlyElement(readAllRecords(PlannedExerciseSessionRecord.class));

        assertThat(updatedExerciseSession.getPlannedExerciseSessionId())
                .isEqualTo(insertedPlannedSession.getMetadata().getId());
        assertThat(updatedTrainingPlan.getCompletedExerciseSessionId())
                .isEqualTo(updatedExerciseSession.getMetadata().getId());
    }

    @Test
    public void bidirectionalReference_nullifiedWhenExerciseSessionDeleted()
            throws InterruptedException {
        PlannedExerciseSessionRecord plannedSession =
                basePlannedExerciseSession(ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING)
                        .build();
        Record insertedPlannedSession = verifyInsertSucceeds(plannedSession);
        ExerciseSessionRecord exerciseSession =
                new ExerciseSessionRecord.Builder(
                                buildMetadata(null),
                                SESSION_START_TIME,
                                SESSION_END_TIME,
                                ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING)
                        .setPlannedExerciseSessionId(insertedPlannedSession.getMetadata().getId())
                        .build();

        insertRecords(Collections.singletonList(exerciseSession));
        verifyDeleteRecords(
                ExerciseSessionRecord.class,
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.EPOCH)
                        .setEndTime(Instant.now())
                        .build());
        PlannedExerciseSessionRecord updatedPlannedExercise =
                Iterables.getOnlyElement(readAllRecords(PlannedExerciseSessionRecord.class));
        assertThat(updatedPlannedExercise.getCompletedExerciseSessionId()).isNull();
    }

    @Test
    public void
            bidirectionalReference_multipleExerciseSessionsCompletePlannedExercise_lastWriteWins()
                    throws InterruptedException {
        PlannedExerciseSessionRecord plannedSession =
                basePlannedExerciseSession(ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING)
                        .build();
        Record insertedPlannedSession = verifyInsertSucceeds(plannedSession);
        ExerciseSessionRecord exerciseSessionOne =
                new ExerciseSessionRecord.Builder(
                                buildMetadata(null),
                                SESSION_START_TIME,
                                SESSION_END_TIME,
                                ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING)
                        .setPlannedExerciseSessionId(insertedPlannedSession.getMetadata().getId())
                        .build();
        ExerciseSessionRecord exerciseSessionTwo =
                new ExerciseSessionRecord.Builder(
                                buildMetadata(null),
                                SESSION_START_TIME.plus(2, DAYS),
                                SESSION_END_TIME.plus(2, DAYS),
                                ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING)
                        .setPlannedExerciseSessionId(insertedPlannedSession.getMetadata().getId())
                        .build();

        insertRecords(Collections.singletonList(exerciseSessionOne));
        String latestExerciseId =
                Iterables.getOnlyElement(
                                insertRecords(Collections.singletonList(exerciseSessionTwo)))
                        .getMetadata()
                        .getId();
        PlannedExerciseSessionRecord updatedPlannedExercise =
                Iterables.getOnlyElement(readAllRecords(PlannedExerciseSessionRecord.class));
        assertThat(updatedPlannedExercise.getCompletedExerciseSessionId()).isNotNull();
        assertThat(updatedPlannedExercise.getCompletedExerciseSessionId())
                .isEqualTo(latestExerciseId);
    }

    @Test
    public void insertTrainingPlan_changelogCreated() throws InterruptedException {
        ChangeLogTokenRequest tokenRequest =
                new ChangeLogTokenRequest.Builder()
                        .addRecordType(PlannedExerciseSessionRecord.class)
                        .build();
        String token = TestUtils.getChangeLogToken(tokenRequest).getToken();
        PlannedExerciseSessionRecord.Builder builder =
                basePlannedExerciseSession(ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING);
        PlannedExerciseBlock.Builder blockBuilder = new PlannedExerciseBlock.Builder(3);
        blockBuilder.setDescription("Some description");

        PlannedExerciseSessionRecord inserted =
                (PlannedExerciseSessionRecord) verifyInsertSucceeds(builder.build());

        verifyChangelogCreatedForUpsert(inserted, token);
    }

    @Test
    public void insertExerciseSession_completesTrainingPlan_changelogGeneratedForTrainingPlan()
            throws InterruptedException {
        // Create and insert training plan.
        ChangeLogTokenRequest tokenRequest =
                new ChangeLogTokenRequest.Builder()
                        .addRecordType(PlannedExerciseSessionRecord.class)
                        .addRecordType(ExerciseSessionRecord.class)
                        .build();
        String changeToken = TestUtils.getChangeLogToken(tokenRequest).getToken();
        PlannedExerciseSessionRecord.Builder builder =
                basePlannedExerciseSession(ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING);
        PlannedExerciseBlock.Builder blockBuilder = new PlannedExerciseBlock.Builder(3);
        blockBuilder.setDescription("Some description");

        verifyInsertSucceeds(builder.build());
        PlannedExerciseSessionRecord originalTrainingPlan =
                Iterables.getOnlyElement(readAllRecords(PlannedExerciseSessionRecord.class));
        verifyChangelogCreatedForUpsert(originalTrainingPlan, changeToken);

        // Create and insert exercise session that completes previous training plan.
        ExerciseSessionRecord exerciseSession =
                new ExerciseSessionRecord.Builder(
                                buildMetadata(null),
                                SESSION_START_TIME,
                                SESSION_END_TIME,
                                ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING)
                        .setPlannedExerciseSessionId(originalTrainingPlan.getMetadata().getId())
                        .build();

        changeToken = TestUtils.getChangeLogToken(tokenRequest).getToken();
        ExerciseSessionRecord inserted =
                (ExerciseSessionRecord)
                        insertRecords(Collections.singletonList(exerciseSession)).get(0);
        verifyChangelogCreatedForUpsert(inserted, changeToken);

        // Verify the updated training plan (now with a link to the previous exercise session) is
        // included in the changelogs.
        PlannedExerciseSessionRecord updatedTrainingPlan =
                Iterables.getOnlyElement(readAllRecords(PlannedExerciseSessionRecord.class));
        verifyChangelogCreatedForUpsert(updatedTrainingPlan, changeToken);
    }

    @Test
    public void updateExerciseSession_completesTrainingPlan_changelogGeneratedForTrainingPlan()
            throws InterruptedException {
        // Create and insert training plan.
        ChangeLogTokenRequest tokenRequest =
                new ChangeLogTokenRequest.Builder()
                        .addRecordType(PlannedExerciseSessionRecord.class)
                        .addRecordType(ExerciseSessionRecord.class)
                        .build();
        String changeToken = TestUtils.getChangeLogToken(tokenRequest).getToken();
        PlannedExerciseSessionRecord.Builder builder =
                basePlannedExerciseSession(ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING);
        PlannedExerciseBlock.Builder blockBuilder = new PlannedExerciseBlock.Builder(3);
        blockBuilder.setDescription("Some description");

        verifyInsertSucceeds(builder.build());
        PlannedExerciseSessionRecord originalTrainingPlan =
                Iterables.getOnlyElement(readAllRecords(PlannedExerciseSessionRecord.class));
        verifyChangelogCreatedForUpsert(originalTrainingPlan, changeToken);

        // Create and insert exercise session, but don't complete the training plan yet.
        ExerciseSessionRecord.Builder exerciseSession =
                new ExerciseSessionRecord.Builder(
                        buildMetadata(null),
                        SESSION_START_TIME,
                        SESSION_END_TIME,
                        ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING);

        ExerciseSessionRecord inserted =
                (ExerciseSessionRecord)
                        insertRecords(Collections.singletonList(exerciseSession.build())).get(0);

        // Now update the exercise session to complete the training plan.
        changeToken = TestUtils.getChangeLogToken(tokenRequest).getToken();
        TestUtils.updateRecords(
                Collections.singletonList(
                        exerciseSessionRecordToBuilder(inserted)
                                .setPlannedExerciseSessionId(
                                        originalTrainingPlan.getMetadata().getId())
                                .build()));

        // Verify the updated training plan (now with a link to the previous exercise session) is
        // included in the changelogs.
        PlannedExerciseSessionRecord updatedTrainingPlan =
                Iterables.getOnlyElement(readAllRecords(PlannedExerciseSessionRecord.class));
        assertThat(updatedTrainingPlan.getCompletedExerciseSessionId()).isNotNull();
        verifyChangelogCreatedForUpsert(updatedTrainingPlan, changeToken);
    }

    @Test
    public void
            updateExerciseSession_removesReferenceToTrainingPlan_changelogGeneratedForTrainingPlan()
                    throws InterruptedException {
        // Create and insert training plan.
        ChangeLogTokenRequest tokenRequest =
                new ChangeLogTokenRequest.Builder()
                        .addRecordType(PlannedExerciseSessionRecord.class)
                        .addRecordType(ExerciseSessionRecord.class)
                        .build();
        String changeToken = TestUtils.getChangeLogToken(tokenRequest).getToken();
        PlannedExerciseSessionRecord.Builder builder =
                basePlannedExerciseSession(ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING);
        PlannedExerciseBlock.Builder blockBuilder = new PlannedExerciseBlock.Builder(3);
        blockBuilder.setDescription("Some description");

        verifyInsertSucceeds(builder.build());
        PlannedExerciseSessionRecord originalTrainingPlan =
                Iterables.getOnlyElement(readAllRecords(PlannedExerciseSessionRecord.class));
        verifyChangelogCreatedForUpsert(originalTrainingPlan, changeToken);

        // Create and insert exercise session completing the training plan.
        ExerciseSessionRecord.Builder exerciseSession =
                new ExerciseSessionRecord.Builder(
                                buildMetadata(null),
                                SESSION_START_TIME,
                                SESSION_END_TIME,
                                ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING)
                        .setPlannedExerciseSessionId(originalTrainingPlan.getMetadata().getId());
        exerciseSession =
                exerciseSessionRecordToBuilder(
                        (ExerciseSessionRecord)
                                insertRecords(Collections.singletonList(exerciseSession.build()))
                                        .get(0));

        // Now update the exercise session to nullify the reference to the training plan.
        changeToken = TestUtils.getChangeLogToken(tokenRequest).getToken();
        exerciseSession.setPlannedExerciseSessionId(null);
        TestUtils.updateRecords(Collections.singletonList(exerciseSession.build()));
        // Verify the updated training plan (now without a link to the previous exercise session) is
        // included in the changelogs.
        PlannedExerciseSessionRecord updatedTrainingPlan =
                Iterables.getOnlyElement(readAllRecords(PlannedExerciseSessionRecord.class));
        assertThat(updatedTrainingPlan.getCompletedExerciseSessionId()).isNull();
        verifyChangelogCreatedForUpsert(updatedTrainingPlan, changeToken);
    }

    @Test
    public void
            updateExerciseSession_changesTrainingPlan_changelogGeneratedForPreviousAndCurrentPlan()
                    throws InterruptedException {
        // Create and insert two training plans.
        ChangeLogTokenRequest tokenRequest =
                new ChangeLogTokenRequest.Builder()
                        .addRecordType(PlannedExerciseSessionRecord.class)
                        .addRecordType(ExerciseSessionRecord.class)
                        .build();
        String changeToken = TestUtils.getChangeLogToken(tokenRequest).getToken();
        PlannedExerciseSessionRecord.Builder builder =
                basePlannedExerciseSession(ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING);
        PlannedExerciseBlock.Builder blockBuilder = new PlannedExerciseBlock.Builder(3);
        blockBuilder.setDescription("Some description");

        verifyInsertSucceeds(builder.build());
        builder.setTitle("Second training plan");
        builder.setStartTime(SESSION_END_TIME.plus(Duration.ofHours(1)));
        builder.setEndTime(SESSION_END_TIME.plus(Duration.ofHours(2)));
        verifyInsertSucceeds(builder.build());
        PlannedExerciseSessionRecord firstTrainingPlan =
                readAllRecords(PlannedExerciseSessionRecord.class).get(0);
        PlannedExerciseSessionRecord secondTrainingPlan =
                readAllRecords(PlannedExerciseSessionRecord.class).get(1);
        verifyChangelogCreatedForUpsert(firstTrainingPlan, changeToken);

        // Create and insert exercise session completing the first training plan.
        ExerciseSessionRecord.Builder exerciseSession =
                new ExerciseSessionRecord.Builder(
                                buildMetadata(null),
                                SESSION_START_TIME,
                                SESSION_END_TIME,
                                ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING)
                        .setPlannedExerciseSessionId(firstTrainingPlan.getMetadata().getId());
        ExerciseSessionRecord inserted =
                (ExerciseSessionRecord)
                        insertRecords(Collections.singletonList(exerciseSession.build())).get(0);

        // Now update the exercise session to change the reference to the second training plan.
        changeToken = TestUtils.getChangeLogToken(tokenRequest).getToken();

        TestUtils.updateRecords(
                Collections.singletonList(
                        exerciseSessionRecordToBuilder(inserted)
                                .setPlannedExerciseSessionId(
                                        secondTrainingPlan.getMetadata().getId())
                                .build()));
        // Verify that both the first and second training plans are included in the changelogs.
        firstTrainingPlan = readAllRecords(PlannedExerciseSessionRecord.class).get(0);
        secondTrainingPlan = readAllRecords(PlannedExerciseSessionRecord.class).get(1);
        verifyChangelogCreatedForUpsert(firstTrainingPlan, changeToken);
        verifyChangelogCreatedForUpsert(secondTrainingPlan, changeToken);
    }

    @Test
    public void referenceToTrainingPlanNullified_changelogsBelongToOwnersOfRespectiveRecords()
            throws Exception {
        // Create and insert training plan using a package (test app) that is different to the CTS
        // tests.
        PlannedExerciseSessionRecord plannedExerciseSession =
                new PlannedExerciseSessionRecord.Builder(
                                new Metadata.Builder().build(),
                                ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING,
                                SESSION_START_TIME,
                                SESSION_END_TIME)
                        .build();
        mTestApp.insertRecord(plannedExerciseSession);
        PlannedExerciseSessionRecord insertedTrainingPlan =
                Iterables.getOnlyElement(readAllRecords(PlannedExerciseSessionRecord.class));

        // Create and insert exercise session completing the training plan.
        ExerciseSessionRecord.Builder exerciseSession =
                new ExerciseSessionRecord.Builder(
                                buildMetadata(null),
                                SESSION_START_TIME,
                                SESSION_END_TIME,
                                ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING)
                        .setPlannedExerciseSessionId(insertedTrainingPlan.getMetadata().getId());
        ExerciseSessionRecord inserted =
                (ExerciseSessionRecord)
                        insertRecords(Collections.singletonList(exerciseSession.build())).get(0);

        // Filter for changelogs belonging to the testapp.
        ChangeLogTokenRequest tokenRequest =
                new ChangeLogTokenRequest.Builder()
                        .addDataOriginFilter(
                                new DataOrigin.Builder().setPackageName(PKG_TEST_APP).build())
                        .addRecordType(PlannedExerciseSessionRecord.class)
                        .addRecordType(ExerciseSessionRecord.class)
                        .build();
        String changeToken = TestUtils.getChangeLogToken(tokenRequest).getToken();
        // Now update the exercise session to nullify the reference to the training plan.
        TestUtils.updateRecords(
                Collections.singletonList(
                        exerciseSessionRecordToBuilder(inserted)
                                .setPlannedExerciseSessionId(null)
                                .build()));
        // Verify that the changelog for the affected training plan belongs to the test app.
        PlannedExerciseSessionRecord updatedTrainingPlan =
                Iterables.getOnlyElement(readAllRecords(PlannedExerciseSessionRecord.class));
        assertThat(updatedTrainingPlan.getCompletedExerciseSessionId()).isNull();
        verifyChangelogCreatedForUpsert(updatedTrainingPlan, changeToken);
    }

    @Test
    public void trainingPlanDeleted_changelogGeneratedForLinkedExerciseSession()
            throws InterruptedException {
        // Create and insert training plan.
        ChangeLogTokenRequest tokenRequest =
                new ChangeLogTokenRequest.Builder()
                        .addRecordType(PlannedExerciseSessionRecord.class)
                        .addRecordType(ExerciseSessionRecord.class)
                        .build();
        String changeToken = TestUtils.getChangeLogToken(tokenRequest).getToken();
        PlannedExerciseSessionRecord.Builder builder =
                basePlannedExerciseSession(ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING);
        PlannedExerciseBlock.Builder blockBuilder = new PlannedExerciseBlock.Builder(3);
        blockBuilder.setDescription("Some description");
        verifyInsertSucceeds(builder.build());

        PlannedExerciseSessionRecord originalTrainingPlan =
                Iterables.getOnlyElement(readAllRecords(PlannedExerciseSessionRecord.class));
        verifyChangelogCreatedForUpsert(originalTrainingPlan, changeToken);

        // Create and insert exercise that completes training plan.
        ExerciseSessionRecord exerciseSession =
                new ExerciseSessionRecord.Builder(
                                buildMetadata(null),
                                SESSION_START_TIME,
                                SESSION_END_TIME,
                                ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING)
                        .setPlannedExerciseSessionId(originalTrainingPlan.getMetadata().getId())
                        .build();
        exerciseSession =
                (ExerciseSessionRecord)
                        insertRecords(Collections.singletonList(exerciseSession)).get(0);
        verifyChangelogCreatedForUpsert(exerciseSession, changeToken);

        // Delete the training plan.
        changeToken = TestUtils.getChangeLogToken(tokenRequest).getToken();
        TestUtils.deleteRecords(Collections.singletonList(originalTrainingPlan));
        ExerciseSessionRecord updatedExerciseSession =
                Iterables.getOnlyElement(readAllRecords(ExerciseSessionRecord.class));
        assertThat(updatedExerciseSession.getPlannedExerciseSessionId()).isNull();
        verifyChangelogCreatedForUpsert(updatedExerciseSession, changeToken);
    }

    @Test
    public void trainingPlanDeleted_changelogsBelongToOwnersOfRespectiveRecords() throws Exception {
        // Create and insert training plan.
        PlannedExerciseSessionRecord.Builder builder =
                basePlannedExerciseSession(ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING);
        PlannedExerciseBlock.Builder blockBuilder = new PlannedExerciseBlock.Builder(3);
        blockBuilder.setDescription("Some description");
        verifyInsertSucceeds(builder.build());

        PlannedExerciseSessionRecord originalTrainingPlan =
                Iterables.getOnlyElement(readAllRecords(PlannedExerciseSessionRecord.class));

        // Create and insert exercise that completes training plan, but insert via the test app
        // (different package).
        ExerciseSessionRecord exerciseSession =
                new ExerciseSessionRecord.Builder(
                                new Metadata.Builder().build(),
                                SESSION_START_TIME,
                                SESSION_END_TIME,
                                ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING)
                        .setPlannedExerciseSessionId(originalTrainingPlan.getMetadata().getId())
                        .build();
        mTestApp.insertRecord(exerciseSession);

        // Filter for changelogs belonging to the test app only.
        ChangeLogTokenRequest tokenRequest =
                new ChangeLogTokenRequest.Builder()
                        .addDataOriginFilter(
                                new DataOrigin.Builder().setPackageName(PKG_TEST_APP).build())
                        .addRecordType(PlannedExerciseSessionRecord.class)
                        .addRecordType(ExerciseSessionRecord.class)
                        .build();
        String changeToken = TestUtils.getChangeLogToken(tokenRequest).getToken();
        // Delete the training plan.
        TestUtils.deleteRecords(Collections.singletonList(originalTrainingPlan));
        ExerciseSessionRecord updatedExerciseSession =
                Iterables.getOnlyElement(readAllRecords(ExerciseSessionRecord.class));
        assertThat(updatedExerciseSession.getPlannedExerciseSessionId()).isNull();
        verifyChangelogCreatedForUpsert(updatedExerciseSession, changeToken);
    }

    @Test
    public void trainingPlanDeleted_multipleLinkedExerciseSessions_changelogsGeneratedForEach()
            throws InterruptedException {
        // Create and insert training plan.
        ChangeLogTokenRequest tokenRequest =
                new ChangeLogTokenRequest.Builder()
                        .addRecordType(PlannedExerciseSessionRecord.class)
                        .addRecordType(ExerciseSessionRecord.class)
                        .build();
        String changeToken = TestUtils.getChangeLogToken(tokenRequest).getToken();
        PlannedExerciseSessionRecord.Builder builder =
                basePlannedExerciseSession(ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING);
        PlannedExerciseBlock.Builder blockBuilder = new PlannedExerciseBlock.Builder(3);
        blockBuilder.setDescription("Some description");
        verifyInsertSucceeds(builder.build());

        PlannedExerciseSessionRecord originalTrainingPlan =
                Iterables.getOnlyElement(readAllRecords(PlannedExerciseSessionRecord.class));
        verifyChangelogCreatedForUpsert(originalTrainingPlan, changeToken);

        // Create and insert exercise that completes the training plan.
        ExerciseSessionRecord exerciseSession =
                new ExerciseSessionRecord.Builder(
                                buildMetadata(null),
                                SESSION_START_TIME,
                                SESSION_END_TIME,
                                ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING)
                        .setPlannedExerciseSessionId(originalTrainingPlan.getMetadata().getId())
                        .build();
        exerciseSession =
                (ExerciseSessionRecord)
                        insertRecords(Collections.singletonList(exerciseSession)).get(0);
        verifyChangelogCreatedForUpsert(exerciseSession, changeToken);

        // Create and insert another exercise that completes the training plan.
        ExerciseSessionRecord anotherExerciseSession =
                new ExerciseSessionRecord.Builder(
                                buildMetadata(null),
                                SESSION_START_TIME.plus(3, HOURS),
                                SESSION_END_TIME.plus(3, HOURS),
                                ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING)
                        .setPlannedExerciseSessionId(originalTrainingPlan.getMetadata().getId())
                        .build();
        anotherExerciseSession =
                (ExerciseSessionRecord)
                        insertRecords(Collections.singletonList(anotherExerciseSession)).get(0);
        verifyChangelogCreatedForUpsert(anotherExerciseSession, changeToken);

        // Delete the training plan.
        changeToken = TestUtils.getChangeLogToken(tokenRequest).getToken();
        TestUtils.deleteRecords(Collections.singletonList(originalTrainingPlan));
        // Verify changelogs created for *both* exercise sessions.
        List<ExerciseSessionRecord> exerciseSessionRecords =
                readAllRecords(ExerciseSessionRecord.class);
        assertThat(exerciseSessionRecords).hasSize(2);
        for (ExerciseSessionRecord exerciseSessionRecord : exerciseSessionRecords) {
            verifyChangelogCreatedForUpsert(exerciseSessionRecord, changeToken);
        }
    }

    @Test
    public void exerciseSessionDeleted_changelogGeneratedForLinkedTrainingPlan()
            throws InterruptedException {
        // Create and insert training plan.
        ChangeLogTokenRequest tokenRequest =
                new ChangeLogTokenRequest.Builder()
                        .addRecordType(PlannedExerciseSessionRecord.class)
                        .addRecordType(ExerciseSessionRecord.class)
                        .build();
        String changeToken = TestUtils.getChangeLogToken(tokenRequest).getToken();
        PlannedExerciseSessionRecord.Builder builder =
                basePlannedExerciseSession(ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING);
        PlannedExerciseBlock.Builder blockBuilder = new PlannedExerciseBlock.Builder(3);
        blockBuilder.setDescription("Some description");
        verifyInsertSucceeds(builder.build());

        PlannedExerciseSessionRecord originalTrainingPlan =
                Iterables.getOnlyElement(readAllRecords(PlannedExerciseSessionRecord.class));
        verifyChangelogCreatedForUpsert(originalTrainingPlan, changeToken);

        // Create and insert exercise that completes training plan.
        ExerciseSessionRecord exerciseSession =
                new ExerciseSessionRecord.Builder(
                                buildMetadata(null),
                                SESSION_START_TIME,
                                SESSION_END_TIME,
                                ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING)
                        .setPlannedExerciseSessionId(originalTrainingPlan.getMetadata().getId())
                        .build();

        changeToken = TestUtils.getChangeLogToken(tokenRequest).getToken();
        exerciseSession =
                (ExerciseSessionRecord)
                        insertRecords(Collections.singletonList(exerciseSession)).get(0);
        verifyChangelogCreatedForUpsert(exerciseSession, changeToken);

        // Delete the exercise session.
        changeToken = TestUtils.getChangeLogToken(tokenRequest).getToken();
        TestUtils.deleteRecords(Collections.singletonList(exerciseSession));
        PlannedExerciseSessionRecord updatedTrainingPlan =
                Iterables.getOnlyElement(readAllRecords(PlannedExerciseSessionRecord.class));
        assertThat(updatedTrainingPlan.getCompletedExerciseSessionId()).isNull();
        verifyChangelogCreatedForUpsert(updatedTrainingPlan, changeToken);
    }

    @Test
    public void hashCodeAndEquals_sensitiveToAllFields() {
        // Go through each field, first check hashCode/equals the same when field the same. Then
        // change field value for
        // one of the instances and check that hashCode/equals now differ.
        PlannedExerciseSessionRecord.Builder builder =
                basePlannedExerciseSession(ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING);

        int exerciseX = ExerciseSessionType.EXERCISE_SESSION_TYPE_BASEBALL;
        int exerciseY = ExerciseSessionType.EXERCISE_SESSION_TYPE_BASKETBALL;
        assertThat(builder.setExerciseType(exerciseX).build())
                .isNotEqualTo(builder.setExerciseType(exerciseY).build());
        assertThat(builder.setExerciseType(exerciseX).build().hashCode())
                .isNotEqualTo(builder.setExerciseType(exerciseY).build().hashCode());
        assertThat(builder.setExerciseType(exerciseX).build())
                .isEqualTo(builder.setExerciseType(exerciseX).build());
        assertThat(builder.setExerciseType(exerciseX).build().hashCode())
                .isEqualTo(builder.setExerciseType(exerciseX).build().hashCode());

        assertThat(builder.setTitle("X").build()).isNotEqualTo(builder.setTitle("Y").build());
        assertThat(builder.setTitle("X").build().hashCode())
                .isNotEqualTo(builder.setTitle("Y").build().hashCode());
        assertThat(builder.setTitle("X").build()).isEqualTo(builder.setTitle("X").build());
        assertThat(builder.setTitle("X").build().hashCode())
                .isEqualTo(builder.setTitle("X").build().hashCode());

        assertThat(builder.setNotes("X").build()).isNotEqualTo(builder.setNotes("Y").build());
        assertThat(builder.setNotes("X").build().hashCode())
                .isNotEqualTo(builder.setNotes("Y").build().hashCode());
        assertThat(builder.setNotes("X").build()).isEqualTo(builder.setNotes("X").build());
        assertThat(builder.setNotes("X").build().hashCode())
                .isEqualTo(builder.setNotes("X").build().hashCode());

        PlannedExerciseBlock.Builder block = baseExerciseBlock();
        List<PlannedExerciseBlock> blocksX = List.of(block.setDescription("X").build());
        List<PlannedExerciseBlock> blocksY = List.of(block.setDescription("Y").build());
        assertThat(builder.setBlocks(blocksX).build())
                .isNotEqualTo(builder.setBlocks(blocksY).build());
        assertThat(builder.setBlocks(blocksX).build().hashCode())
                .isNotEqualTo(builder.setBlocks(blocksY).build().hashCode());
        assertThat(builder.setBlocks(blocksX).build())
                .isEqualTo(builder.setBlocks(blocksX).build());
        assertThat(builder.setBlocks(blocksX).build().hashCode())
                .isEqualTo(builder.setBlocks(blocksX).build().hashCode());

        LocalDate startDateX = LocalDate.of(2000, Month.JANUARY, 1);
        LocalDate startDateY = LocalDate.of(2000, Month.JANUARY, 2);

        PlannedExerciseSessionRecord.Builder builderWithStartDateX =
                new PlannedExerciseSessionRecord.Builder(
                        buildMetadata("some_client_record_id"),
                        exerciseX,
                        startDateX,
                        Duration.ofHours(1));
        PlannedExerciseSessionRecord.Builder builderWithStartDateY =
                new PlannedExerciseSessionRecord.Builder(
                        buildMetadata("some_client_record_id"),
                        exerciseX,
                        startDateY,
                        Duration.ofHours(1));
        assertThat(builderWithStartDateX.build()).isNotEqualTo(builderWithStartDateY.build());
        assertThat(builderWithStartDateX.build().hashCode())
                .isNotEqualTo(builderWithStartDateY.build().hashCode());
        assertThat(builderWithStartDateX.build()).isEqualTo(builderWithStartDateX.build());
        assertThat(builderWithStartDateX.build().hashCode())
                .isEqualTo(builderWithStartDateX.build().hashCode());
    }

    @Test
    public void builtRecord_fieldsMatchValuesSpecifiedViaBuilder() {
        PlannedExerciseSessionRecord.Builder builder =
                new PlannedExerciseSessionRecord.Builder(
                        buildMetadata("some client record ID"),
                        ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING,
                        SESSION_START_TIME,
                        SESSION_END_TIME);
        builder.setExerciseType(ExerciseSessionType.EXERCISE_SESSION_TYPE_BOXING);
        builder.setTitle("Some title");
        builder.setNotes("Some notes");
        builder.setBlocks(List.of(baseExerciseBlock().build()));

        PlannedExerciseSessionRecord builtRecord = builder.build();

        assertThat(builtRecord.getExerciseType())
                .isEqualTo(ExerciseSessionType.EXERCISE_SESSION_TYPE_BOXING);
        assertThat(builtRecord.getTitle()).isEqualTo("Some title");
        assertThat(builtRecord.getNotes()).isEqualTo("Some notes");
        assertThat(builtRecord.getStartTime()).isEqualTo(SESSION_START_TIME);
        assertThat(builtRecord.getEndTime()).isEqualTo(SESSION_END_TIME);
        assertThat(builtRecord.getBlocks()).isEqualTo(List.of(baseExerciseBlock().build()));
    }

    @Test
    public void noExplicitTimeSpecified_hasExplicitTimeIsFalse() {
        PlannedExerciseSessionRecord.Builder builder =
                new PlannedExerciseSessionRecord.Builder(
                        buildMetadata("some client record ID"),
                        ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING,
                        LocalDate.of(2007, APRIL, 5),
                        Duration.of(1, HOURS));

        assertThat(builder.build().hasExplicitTime()).isFalse();
    }

    @Test
    public void noExplicitTimeSpecified_startTimeSetToStartOfDay_endTimeDeterminedFromDuration() {
        PlannedExerciseSessionRecord.Builder builder =
                new PlannedExerciseSessionRecord.Builder(
                        buildMetadata("some client record ID"),
                        ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING,
                        LocalDate.of(2007, APRIL, 5),
                        Duration.of(1, HOURS));

        assertThat(builder.build().getStartTime())
                .isEqualTo(
                        LocalDate.of(2007, APRIL, 5)
                                .atTime(LocalTime.NOON)
                                .atZone(ZoneId.systemDefault())
                                .toInstant());
        assertThat(builder.build().getEndTime())
                .isEqualTo(
                        LocalDate.of(2007, APRIL, 5)
                                .atTime(LocalTime.NOON)
                                .atZone(ZoneId.systemDefault())
                                .toInstant()
                                .plus(Duration.of(1, HOURS)));
    }

    @Test
    public void getStartDate_returnsLocalDateFromTime() {
        PlannedExerciseSessionRecord.Builder builder =
                new PlannedExerciseSessionRecord.Builder(
                        buildMetadata("some client record ID"),
                        ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING,
                        LocalDate.of(2007, APRIL, 5),
                        Duration.of(1, HOURS));

        assertThat(builder.build().getStartDate()).isEqualTo(LocalDate.of(2007, APRIL, 5));
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidExerciseType_throwsException() {
        new PlannedExerciseSessionRecord.Builder(
                        buildMetadata("some_client_record_id"),
                        -1,
                        SESSION_START_TIME,
                        SESSION_END_TIME)
                .build();
    }

    private ExerciseSessionRecord.Builder exerciseSessionRecordToBuilder(
            ExerciseSessionRecord record) {
        ExerciseSessionRecord.Builder builder =
                new ExerciseSessionRecord.Builder(
                        record.getMetadata(),
                        record.getStartTime(),
                        record.getEndTime(),
                        record.getExerciseType());
        builder.setTitle(record.getTitle());
        builder.setPlannedExerciseSessionId(record.getPlannedExerciseSessionId());
        return builder;
    }

    private PlannedExerciseSessionRecord.Builder plannedExerciseSessionRecordToBuilder(
            PlannedExerciseSessionRecord record) {
        PlannedExerciseSessionRecord.Builder builder =
                new PlannedExerciseSessionRecord.Builder(
                        record.getMetadata(),
                        record.getExerciseType(),
                        record.getStartTime(),
                        record.getEndTime());
        builder.setTitle(record.getTitle());
        builder.setNotes(record.getNotes());
        builder.setStartZoneOffset(record.getStartZoneOffset());
        builder.setEndZoneOffset(record.getEndZoneOffset());
        record.getBlocks().stream().forEach(block -> builder.addBlock(block));
        return builder;
    }

    @Test
    public void builder_clearStartZoneOffset() {
        PlannedExerciseSessionRecord.Builder builder =
                new PlannedExerciseSessionRecord.Builder(
                        buildMetadata("some client record ID"),
                        ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING,
                        LocalDate.of(2007, APRIL, 5),
                        Duration.of(1, HOURS));

        builder.setStartZoneOffset(ZoneOffset.MIN);
        assertThat(builder.build().getStartZoneOffset()).isEqualTo(ZoneOffset.MIN);
        builder.clearStartZoneOffset();
        assertThat(builder.build().getStartZoneOffset())
                .isEqualTo(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()));
    }

    @Test
    public void builder_clearEndZoneOffset() {
        PlannedExerciseSessionRecord.Builder builder =
                new PlannedExerciseSessionRecord.Builder(
                        buildMetadata("some client record ID"),
                        ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING,
                        LocalDate.of(2007, APRIL, 5),
                        Duration.of(1, HOURS));

        builder.setEndZoneOffset(ZoneOffset.MIN);
        assertThat(builder.build().getEndZoneOffset()).isEqualTo(ZoneOffset.MIN);
        builder.clearEndZoneOffset();
        assertThat(builder.build().getEndZoneOffset())
                .isEqualTo(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()));
    }

    @Test
    public void getDuration() {
        PlannedExerciseSessionRecord.Builder builder =
                new PlannedExerciseSessionRecord.Builder(
                        buildMetadata("some client record ID"),
                        ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING,
                        LocalDate.of(2007, APRIL, 5),
                        Duration.ofHours(2));

        assertThat(builder.build().getDuration()).isEqualTo(Duration.ofHours(2));
    }

    @Test
    public void builder_setMetadata() {
        Metadata originalMetadata = buildMetadata("some client record ID");
        PlannedExerciseSessionRecord.Builder builder =
                new PlannedExerciseSessionRecord.Builder(
                        originalMetadata,
                        ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING,
                        LocalDate.of(2007, APRIL, 5),
                        Duration.ofHours(2));

        assertThat(builder.build().getMetadata()).isEqualTo(originalMetadata);

        Metadata replacementMetadata = buildMetadata("a different client record ID");
        builder.setMetadata(replacementMetadata);

        assertThat(builder.build().getMetadata()).isEqualTo(replacementMetadata);
    }

    @Test
    public void builder_setEndTime() {
        Instant originalStartTime = SESSION_START_TIME;
        PlannedExerciseSessionRecord.Builder builder =
                new PlannedExerciseSessionRecord.Builder(
                        buildMetadata("some client record ID"),
                        ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING,
                        originalStartTime,
                        originalStartTime.plus(1, HOURS));

        assertThat(builder.build().getStartTime()).isEqualTo(originalStartTime);

        Instant replacementStartTime = SESSION_START_TIME.plus(3, HOURS);
        builder.setStartTime(replacementStartTime);
        builder.setEndTime(replacementStartTime.plus(1, HOURS));

        assertThat(builder.build().getStartTime()).isEqualTo(replacementStartTime);
    }

    @Test
    public void builder_clearBlocks() {
        PlannedExerciseSessionRecord.Builder builder =
                basePlannedExerciseSession(ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING);
        PlannedExerciseBlock.Builder blockBuilder = new PlannedExerciseBlock.Builder(3);
        blockBuilder.setDescription("Some description");
        builder.addBlock(blockBuilder.build());

        assertThat(builder.build().getBlocks()).hasSize(1);

        builder.clearBlocks();

        assertThat(builder.build().getBlocks()).isEmpty();
    }

    private PlannedExerciseSessionRecord.Builder basePlannedExerciseSession(int exerciseType) {
        PlannedExerciseSessionRecord.Builder builder =
                new PlannedExerciseSessionRecord.Builder(
                        buildMetadata(null), exerciseType, SESSION_START_TIME, SESSION_END_TIME);
        builder.setNotes("Some notes");
        builder.setTitle("Some training plan");
        builder.setStartZoneOffset(ZoneOffset.UTC);
        builder.setEndZoneOffset(ZoneOffset.UTC);
        return builder;
    }

    private static Metadata buildMetadata(String clientRecordId) {
        return new Metadata.Builder()
                .setDataOrigin(
                        new DataOrigin.Builder()
                                .setPackageName("android.healthconnect.cts")
                                .build())
                .setId(UUID.randomUUID().toString())
                .setClientRecordId(clientRecordId)
                .setRecordingMethod(Metadata.RECORDING_METHOD_ACTIVELY_RECORDED)
                .build();
    }

    private static Metadata buildMetadataWithUuid(String uuid) {
        return new Metadata.Builder()
                .setDataOrigin(
                        new DataOrigin.Builder()
                                .setPackageName("android.healthconnect.cts")
                                .build())
                .setId(uuid)
                .setRecordingMethod(Metadata.RECORDING_METHOD_ACTIVELY_RECORDED)
                .build();
    }

    private static PlannedExerciseBlock.Builder baseExerciseBlock() {
        return new PlannedExerciseBlock.Builder(1);
    }

    private static PlannedExerciseStep.Builder baseExerciseStep() {
        return new PlannedExerciseStep.Builder(
                ExerciseSessionType.EXERCISE_SESSION_TYPE_BIKING,
                PlannedExerciseStep.EXERCISE_CATEGORY_ACTIVE,
                new ExerciseCompletionGoal.DistanceGoal(Length.fromMeters(50)));
    }

    private Record verifyInsertSucceeds(PlannedExerciseSessionRecord record)
            throws InterruptedException {
        return Iterables.getOnlyElement(verifyInsertSucceeds(Collections.singletonList(record)));
    }

    private List<Record> verifyInsertSucceeds(List<PlannedExerciseSessionRecord> records)
            throws InterruptedException {
        List<Record> insertedRecords = insertRecords(records);

        for (int i = 0; i < records.size(); i++) {
            String id = insertedRecords.get(i).getMetadata().getId();
            PlannedExerciseSessionRecord.Builder testRecord =
                    plannedExerciseSessionRecordToBuilder(records.get(i));
            testRecord.setMetadata(buildMetadataWithUuid(id));

            assertThat(testRecord.build()).isEqualTo(insertedRecords.get(i));
        }
        assertThat(records.size()).isEqualTo(insertedRecords.size());

        return insertedRecords;
    }

    private void verifyReadReturnsSameRecords(Record inserted) throws InterruptedException {
        verifyReadReturnsSameRecords(Collections.singletonList(inserted));
    }

    private void verifyChangelogCreatedForUpsert(Record record, String token)
            throws InterruptedException {
        ChangeLogsRequest request = new ChangeLogsRequest.Builder(token).build();

        ChangeLogsResponse response = TestUtils.getChangeLogs(request);
        List<Record> upsertedRecords = response.getUpsertedRecords();
        assertThat(upsertedRecords).contains(record);
    }

    private void verifyReadReturnsSameRecords(List<Record> inserted) throws InterruptedException {
        List<PlannedExerciseSessionRecord> readBack =
                readAllRecords(PlannedExerciseSessionRecord.class);
        assertWithMessage("inserted record count equals read back record count")
                .that(readBack.size())
                .isEqualTo(inserted.size());
        for (int i = 0; i < inserted.size(); i++) {
            assertRecordsEqual(readBack.get(i), (PlannedExerciseSessionRecord) inserted.get(i));
        }
    }

    private void assertRecordsEqual(
            PlannedExerciseSessionRecord actual, PlannedExerciseSessionRecord expected) {
        // First, check equality in a human readable manner.
        assertWithMessage("metadata").that(actual.getMetadata()).isEqualTo(expected.getMetadata());
        assertWithMessage("startTimeMillis")
                .that(actual.getStartTime().toEpochMilli())
                .isEqualTo(expected.getStartTime().toEpochMilli());
        assertWithMessage("startZoneOffset")
                .that(actual.getStartZoneOffset())
                .isEqualTo(expected.getStartZoneOffset());
        assertWithMessage("endTimeMillis")
                .that(actual.getEndTime().toEpochMilli())
                .isEqualTo(expected.getEndTime().toEpochMilli());
        assertWithMessage("endZoneOffset")
                .that(actual.getEndZoneOffset())
                .isEqualTo(expected.getEndZoneOffset());
        assertWithMessage("hasExplicitTime")
                .that(actual.hasExplicitTime())
                .isEqualTo(expected.hasExplicitTime());
        assertWithMessage("title").that(actual.getTitle()).isEqualTo(expected.getTitle());
        assertWithMessage("notes").that(actual.getNotes()).isEqualTo(expected.getNotes());
        assertWithMessage("exerciseType")
                .that(actual.getExerciseType())
                .isEqualTo(expected.getExerciseType());
        assertWithMessage("completedSessionId")
                .that(actual.getCompletedExerciseSessionId())
                .isEqualTo(expected.getCompletedExerciseSessionId());
        assertWithMessage("block count")
                .that(actual.getBlocks().size())
                .isEqualTo(expected.getBlocks().size());
        for (int i = 0; i < actual.getBlocks().size(); i++) {
            PlannedExerciseBlock firstBlock = actual.getBlocks().get(i);
            PlannedExerciseBlock secondBlock = expected.getBlocks().get(i);
            assertWithMessage("block repetitions")
                    .that(firstBlock.getRepetitions())
                    .isEqualTo(secondBlock.getRepetitions());
            assertWithMessage("block description")
                    .that(firstBlock.getDescription())
                    .isEqualTo(secondBlock.getDescription());
            assertWithMessage("step count")
                    .that(firstBlock.getSteps().size())
                    .isEqualTo(secondBlock.getSteps().size());
            for (int j = 0; j < firstBlock.getSteps().size(); j++) {
                PlannedExerciseStep firstStep = firstBlock.getSteps().get(j);
                PlannedExerciseStep secondStep = secondBlock.getSteps().get(j);
                assertWithMessage("step exerciseType")
                        .that(firstStep.getExerciseType())
                        .isEqualTo(secondStep.getExerciseType());
                assertWithMessage("step description")
                        .that(firstStep.getDescription())
                        .isEqualTo(secondStep.getDescription());
                assertWithMessage("step category")
                        .that(firstStep.getExerciseCategory())
                        .isEqualTo(secondStep.getExerciseCategory());
                assertWithMessage("step completion goal")
                        .that(firstStep.getCompletionGoal())
                        .isEqualTo(secondStep.getCompletionGoal());
                assertWithMessage("performance goals count")
                        .that(firstStep.getPerformanceGoals().size())
                        .isEqualTo(secondStep.getPerformanceGoals().size());
                for (int k = 0; k < firstStep.getPerformanceGoals().size(); k++) {
                    ExercisePerformanceGoal firstGoal = firstStep.getPerformanceGoals().get(k);
                    ExercisePerformanceGoal secondGoal = secondStep.getPerformanceGoals().get(k);
                    assertWithMessage("step performance goal")
                            .that(firstGoal)
                            .isEqualTo(secondGoal);
                }
            }
        }

        // Finally, check according to actual equals implementation.
        assertThat(actual).isEqualTo(expected);
    }
}
