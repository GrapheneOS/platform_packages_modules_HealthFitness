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

package android.healthconnect.cts.historicaccess;

import static android.health.connect.datatypes.StepsRecord.STEPS_COUNT_TOTAL;
import static android.health.connect.datatypes.WeightRecord.WEIGHT_AVG;
import static android.healthconnect.cts.utils.DataFactory.getStepsRecord;
import static android.healthconnect.cts.utils.DataFactory.getWeightRecord;
import static android.healthconnect.cts.utils.TestUtils.deleteAllStagedRemoteData;
import static android.healthconnect.cts.utils.TestUtils.getChangeLogToken;
import static android.healthconnect.cts.utils.TestUtils.getChangeLogs;
import static android.healthconnect.cts.utils.TestUtils.getRecordIds;
import static android.healthconnect.cts.utils.TestUtils.insertRecordAndGetId;

import static com.google.common.truth.Truth.assertThat;

import static java.time.temporal.ChronoUnit.DAYS;

import android.health.connect.AggregateRecordsRequest;
import android.health.connect.AggregateRecordsResponse;
import android.health.connect.HealthDataCategory;
import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.ReadRecordsRequestUsingIds;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.changelog.ChangeLogTokenRequest;
import android.health.connect.changelog.ChangeLogsRequest;
import android.health.connect.datatypes.StepsRecord;
import android.health.connect.datatypes.WeightRecord;
import android.health.connect.datatypes.units.Mass;
import android.healthconnect.cts.lib.TestAppProxy;
import android.healthconnect.cts.utils.AssumptionCheckerRule;
import android.healthconnect.cts.utils.DeviceSupportUtils;
import android.healthconnect.cts.utils.TestUtils;
import android.platform.test.annotations.AppModeFull;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@AppModeFull(reason = "HealthConnectManager is not accessible to instant apps")
@RunWith(AndroidJUnit4.class)
public class HistoricAccessLimitTest {
    private static final String PACKAGE_NAME = "android.healthconnect.cts";
    private static final String PKG_TEST_APP = "android.healthconnect.cts.testapp.readWritePerms.A";

    private Instant mNow;
    private TestAppProxy mTestApp;

    @Rule
    public AssumptionCheckerRule mSupportedHardwareRule =
            new AssumptionCheckerRule(
                    DeviceSupportUtils::isHealthConnectFullySupported,
                    "Tests should run on supported hardware only.");

    @Before
    public void setUp() throws InterruptedException {
        mNow = Instant.now();
        mTestApp = TestAppProxy.forPackageName(PKG_TEST_APP);
        deleteAllStagedRemoteData();
    }

    @After
    public void tearDown() throws InterruptedException {
        deleteAllStagedRemoteData();
    }

    @Test
    public void testReadIntervalRecordsByFilters_expectCorrectResponse() throws Exception {
        String ownRecordId1 = insertStepsRecord(daysBeforeNow(10), daysBeforeNow(9), 10);
        String ownRecordId2 = insertStepsRecord(daysBeforeNow(11), daysBeforeNow(10), 11);
        String ownRecordId3 = insertStepsRecord(daysBeforeNow(50), daysBeforeNow(40), 12);
        String otherAppsRecordIdAfterHistoricLimit =
                insertStepsRecordViaTestApp(daysBeforeNow(2), daysBeforeNow(1), 13);
        String otherAppsRecordIdBeforeHistoricLimit =
                insertStepsRecordViaTestApp(daysBeforeNow(50), daysBeforeNow(40), 14);

        List<String> stepsRecordsIdsReadByFilters =
                getRecordIds(
                        TestUtils.readRecords(
                                new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                                        .build()));

        assertThat(stepsRecordsIdsReadByFilters)
                .containsExactly(
                        ownRecordId1,
                        ownRecordId2,
                        ownRecordId3,
                        otherAppsRecordIdAfterHistoricLimit);
        assertThat(stepsRecordsIdsReadByFilters)
                .doesNotContain(otherAppsRecordIdBeforeHistoricLimit);
    }

    @Test
    public void testReadInstantRecordsByFilters_expectCorrectResponse() throws Exception {
        String ownRecordId1 = insertWeightRecord(daysBeforeNow(10), 10);
        String ownRecordId2 = insertWeightRecord(daysBeforeNow(11), 11);
        String ownRecordId3 = insertWeightRecord(daysBeforeNow(50), 12);
        String otherAppsRecordIdAfterHistoricLimit =
                insertWeightRecordViaTestApp(daysBeforeNow(2), 13);
        String otherAppsRecordIdBeforeHistoricLimit =
                insertWeightRecordViaTestApp(daysBeforeNow(50), 14);

        List<String> weightRecordsIdsReadByFilters =
                getRecordIds(
                        TestUtils.readRecords(
                                new ReadRecordsRequestUsingFilters.Builder<>(WeightRecord.class)
                                        .build()));

        assertThat(weightRecordsIdsReadByFilters)
                .containsExactly(
                        ownRecordId1,
                        ownRecordId2,
                        ownRecordId3,
                        otherAppsRecordIdAfterHistoricLimit);
        assertThat(weightRecordsIdsReadByFilters)
                .doesNotContain(otherAppsRecordIdBeforeHistoricLimit);
    }

    @Test
    public void testReadIntervalRecordsByIds_expectCorrectResponse() throws Exception {
        String otherAppsRecordIdBeforeHistoricLimit =
                insertStepsRecordViaTestApp(daysBeforeNow(50), daysBeforeNow(40), 14);
        List<String> insertedRecordIds =
                List.of(
                        insertStepsRecord(daysBeforeNow(10), daysBeforeNow(9), 10),
                        insertStepsRecord(daysBeforeNow(11), daysBeforeNow(10), 11),
                        insertStepsRecord(daysBeforeNow(50), daysBeforeNow(40), 12),
                        insertStepsRecordViaTestApp(daysBeforeNow(2), daysBeforeNow(1), 13),
                        otherAppsRecordIdBeforeHistoricLimit);

        ReadRecordsRequestUsingIds.Builder<StepsRecord> readUsingIdsRequest =
                new ReadRecordsRequestUsingIds.Builder<>(StepsRecord.class);
        insertedRecordIds.stream().forEach(readUsingIdsRequest::addId);
        List<String> recordIdsReadByIds =
                getRecordIds(TestUtils.readRecords(readUsingIdsRequest.build()));

        List<String> insertedRecordIdsWithoutOtherAppsRecordBeforeHistoricLimit =
                new ArrayList<>(insertedRecordIds);
        insertedRecordIdsWithoutOtherAppsRecordBeforeHistoricLimit.remove(
                otherAppsRecordIdBeforeHistoricLimit);
        assertThat(recordIdsReadByIds)
                .containsExactlyElementsIn(
                        insertedRecordIdsWithoutOtherAppsRecordBeforeHistoricLimit);
    }

    @Test
    public void testReadInstantRecordsByIds_expectCorrectResponse() throws Exception {
        String otherAppsRecordIdBeforeHistoricLimit =
                insertWeightRecordViaTestApp(daysBeforeNow(50), 14);
        List<String> insertedRecordIds =
                List.of(
                        insertWeightRecord(daysBeforeNow(10), 10),
                        insertWeightRecord(daysBeforeNow(11), 11),
                        insertWeightRecord(daysBeforeNow(50), 12),
                        insertWeightRecordViaTestApp(daysBeforeNow(2), 13),
                        otherAppsRecordIdBeforeHistoricLimit);

        ReadRecordsRequestUsingIds.Builder<WeightRecord> readUsingIdsRequest =
                new ReadRecordsRequestUsingIds.Builder<>(WeightRecord.class);
        insertedRecordIds.stream().forEach(readUsingIdsRequest::addId);
        List<String> recordIdsReadByIds =
                getRecordIds(TestUtils.readRecords(readUsingIdsRequest.build()));

        List<String> insertedRecordIdsWithoutOtherAppsRecordBeforeHistoricLimit =
                new ArrayList<>(insertedRecordIds);
        insertedRecordIdsWithoutOtherAppsRecordBeforeHistoricLimit.remove(
                otherAppsRecordIdBeforeHistoricLimit);
        assertThat(recordIdsReadByIds)
                .containsExactlyElementsIn(
                        insertedRecordIdsWithoutOtherAppsRecordBeforeHistoricLimit);
    }

    @Test
    public void testAggregateIntervalRecords_expectCorrectResponse() throws Exception {
        TestUtils.setupAggregation(PACKAGE_NAME, HealthDataCategory.ACTIVITY);
        long ownRecordValueAfterHistoricLimit = 20;
        long ownRecordValueBeforeHistoricLimit = 300;
        long otherAppsRecordValueAfterHistoricLimit = 4_000;
        long otherAppsRecordValueBeforeHistoricLimit = 50_000;
        insertStepsRecord(daysBeforeNow(10), daysBeforeNow(9), ownRecordValueAfterHistoricLimit);
        insertStepsRecord(daysBeforeNow(50), daysBeforeNow(40), ownRecordValueBeforeHistoricLimit);
        insertStepsRecordViaTestApp(
                daysBeforeNow(2), daysBeforeNow(1), otherAppsRecordValueAfterHistoricLimit);
        insertStepsRecordViaTestApp(
                daysBeforeNow(60), daysBeforeNow(50), otherAppsRecordValueBeforeHistoricLimit);
        // Add the other app to the priority list
        TestUtils.updatePriorityWithManageHealthDataPermission(
                HealthDataCategory.ACTIVITY, Arrays.asList(PACKAGE_NAME, PKG_TEST_APP));
        TimeInstantRangeFilter timeFilter =
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(daysBeforeNow(1000))
                        .setEndTime(mNow.plus(1000, DAYS))
                        .build();

        AggregateRecordsResponse<Long> totalStepsCountAggregation =
                TestUtils.getAggregateResponse(
                        new AggregateRecordsRequest.Builder<Long>(timeFilter)
                                .addAggregationType(STEPS_COUNT_TOTAL)
                                .build());

        assertThat(totalStepsCountAggregation.get(STEPS_COUNT_TOTAL))
                .isEqualTo(
                        ownRecordValueAfterHistoricLimit
                                + ownRecordValueBeforeHistoricLimit
                                + otherAppsRecordValueAfterHistoricLimit);
    }

    @Test
    public void testAggregateInstantRecords_expectCorrectResponse() throws Exception {
        TestUtils.setupAggregation(PACKAGE_NAME, HealthDataCategory.BODY_MEASUREMENTS);
        double ownRecordValueAfterHistoricLimit = 20;
        double ownRecordValueBeforeHistoricLimit = 300;
        double otherAppsRecordValueAfterHistoricLimit = 4_000;
        double otherAppsRecordValueBeforeHistoricLimit = 50_000;
        insertWeightRecord(daysBeforeNow(10), ownRecordValueAfterHistoricLimit);
        insertWeightRecord(daysBeforeNow(50), ownRecordValueBeforeHistoricLimit);
        insertWeightRecordViaTestApp(daysBeforeNow(2), otherAppsRecordValueAfterHistoricLimit);
        insertWeightRecordViaTestApp(daysBeforeNow(60), otherAppsRecordValueBeforeHistoricLimit);
        TimeInstantRangeFilter timeFilter =
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(daysBeforeNow(1000))
                        .setEndTime(mNow.plus(1000, DAYS))
                        .build();

        AggregateRecordsResponse<Mass> averageWeightAggregation =
                TestUtils.getAggregateResponse(
                        new AggregateRecordsRequest.Builder<Mass>(timeFilter)
                                .addAggregationType(WEIGHT_AVG)
                                .build());

        assertThat(averageWeightAggregation.get(WEIGHT_AVG).getInGrams())
                .isEqualTo(
                        (ownRecordValueAfterHistoricLimit
                                        + ownRecordValueBeforeHistoricLimit
                                        + otherAppsRecordValueAfterHistoricLimit)
                                / 3d);
    }

    @Test
    public void testGetChangeLogs_expectCorrectResponse() throws Exception {
        String token =
                getChangeLogToken(
                                new ChangeLogTokenRequest.Builder()
                                        .addRecordType(WeightRecord.class)
                                        .build())
                        .getToken();
        List<String> insertedRecentRecordIds =
                List.of(
                        insertWeightRecord(daysBeforeNow(10), 10),
                        insertWeightRecord(daysBeforeNow(11), 11),
                        insertWeightRecordViaTestApp(daysBeforeNow(2), 13));
        insertWeightRecord(daysBeforeNow(50), 12);
        insertWeightRecordViaTestApp(daysBeforeNow(60), 14);

        List<String> logsRecordIds =
                getRecordIds(
                        getChangeLogs(new ChangeLogsRequest.Builder(token).build())
                                .getUpsertedRecords());

        assertThat(logsRecordIds).containsExactlyElementsIn(insertedRecentRecordIds);
    }

    @Test
    public void testReadAnotherAppsRecordsByIds_recordsAreAfterCutOff_expectAllRecordsReturned()
            throws Exception {
        String ownRecordId1 = insertWeightRecordViaTestApp(daysBeforeNow(29), 10);
        String ownRecordId2 = insertWeightRecordViaTestApp(daysBeforeNow(11), 11);
        String ownRecordId3 = insertWeightRecordViaTestApp(daysBeforeNow(1), 12);

        List<String> readIds =
                getRecordIds(
                        TestUtils.readRecords(
                                new ReadRecordsRequestUsingIds.Builder<>(WeightRecord.class)
                                        .addId(ownRecordId1)
                                        .addId(ownRecordId2)
                                        .addId(ownRecordId3)
                                        .build()));

        assertThat(readIds).containsExactly(ownRecordId1, ownRecordId2, ownRecordId3);
    }

    @Test
    public void testReadAnotherAppsRecordsByIds_recordsAreBeforeCutOff_expectNoRecordsReturned()
            throws Exception {
        String ownRecordId1 = insertWeightRecordViaTestApp(daysBeforeNow(31), 10);
        String ownRecordId2 = insertWeightRecordViaTestApp(daysBeforeNow(60), 11);
        String ownRecordId3 = insertWeightRecordViaTestApp(daysBeforeNow(100), 12);

        List<String> readIds =
                getRecordIds(
                        TestUtils.readRecords(
                                new ReadRecordsRequestUsingIds.Builder<>(WeightRecord.class)
                                        .addId(ownRecordId1)
                                        .addId(ownRecordId2)
                                        .addId(ownRecordId3)
                                        .build()));

        assertThat(readIds).isEmpty();
    }

    @Test
    public void testReadAnotherAppsRecordsByFilters_recordsAreAfterCutOff_expectAllRecordsReturned()
            throws Exception {
        String ownRecordId1 = insertWeightRecordViaTestApp(daysBeforeNow(29), 10);
        String ownRecordId2 = insertWeightRecordViaTestApp(daysBeforeNow(11), 11);
        String ownRecordId3 = insertWeightRecordViaTestApp(daysBeforeNow(1), 12);

        List<String> readIds =
                getRecordIds(
                        TestUtils.readRecords(
                                new ReadRecordsRequestUsingFilters.Builder<>(WeightRecord.class)
                                        .build()));

        assertThat(readIds).containsExactly(ownRecordId1, ownRecordId2, ownRecordId3);
    }

    @Test
    public void testReadAnotherAppsRecordsByFilters_recordsAreBeforeCutOff_expectNoRecordsReturned()
            throws Exception {
        insertWeightRecordViaTestApp(daysBeforeNow(31), 10);
        insertWeightRecordViaTestApp(daysBeforeNow(60), 11);
        insertWeightRecordViaTestApp(daysBeforeNow(100), 12);

        List<String> readIds =
                getRecordIds(
                        TestUtils.readRecords(
                                new ReadRecordsRequestUsingFilters.Builder<>(WeightRecord.class)
                                        .build()));

        assertThat(readIds).isEmpty();
    }

    @Test
    public void testAggregateAnotherAppsRecords_recordsAreAfterCutOff_expectCorrectAggregation()
            throws Exception {
        insertWeightRecordViaTestApp(daysBeforeNow(29), 10);
        insertWeightRecordViaTestApp(daysBeforeNow(11), 11);
        insertWeightRecordViaTestApp(daysBeforeNow(1), 12);
        TimeInstantRangeFilter timeFilter =
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(daysBeforeNow(1000))
                        .setEndTime(mNow.plus(1000, DAYS))
                        .build();

        AggregateRecordsResponse<Mass> response =
                TestUtils.getAggregateResponse(
                        new AggregateRecordsRequest.Builder<Mass>(timeFilter)
                                .addAggregationType(WEIGHT_AVG)
                                .build());

        assertThat(response.get(WEIGHT_AVG).getInGrams()).isEqualTo(11.0d);
    }

    @Test
    public void testAggregateAnotherAppsRecords_recordsAreBeforeCutOff_expectNoAggregation()
            throws Exception {
        insertWeightRecordViaTestApp(daysBeforeNow(31), 10);
        insertWeightRecordViaTestApp(daysBeforeNow(60), 11);
        insertWeightRecordViaTestApp(daysBeforeNow(100), 12);
        TimeInstantRangeFilter timeFilter =
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(daysBeforeNow(1000))
                        .setEndTime(mNow.plus(1000, DAYS))
                        .build();

        AggregateRecordsResponse<Mass> response =
                TestUtils.getAggregateResponse(
                        new AggregateRecordsRequest.Builder<Mass>(timeFilter)
                                .addAggregationType(WEIGHT_AVG)
                                .build());

        assertThat(response.get(WEIGHT_AVG)).isNull();
    }

    @Test
    public void testGetAnotherAppsChangeLogs_recordsAreAfterCutOff_expectAllRecordsReturned()
            throws Exception {
        String token =
                getChangeLogToken(
                                new ChangeLogTokenRequest.Builder()
                                        .addRecordType(WeightRecord.class)
                                        .build())
                        .getToken();
        String ownRecordId1 = insertWeightRecordViaTestApp(daysBeforeNow(29), 10);
        String ownRecordId2 = insertWeightRecordViaTestApp(daysBeforeNow(11), 11);
        String ownRecordId3 = insertWeightRecordViaTestApp(daysBeforeNow(1), 12);

        List<String> logsRecordIds =
                getRecordIds(
                        getChangeLogs(new ChangeLogsRequest.Builder(token).build())
                                .getUpsertedRecords());

        assertThat(logsRecordIds).containsExactly(ownRecordId1, ownRecordId2, ownRecordId3);
    }

    @Test
    public void testGetAnotherAppsChangeLogs_recordsAreBeforeCutOff_expectNoRecordsReturned()
            throws Exception {
        String token =
                getChangeLogToken(
                                new ChangeLogTokenRequest.Builder()
                                        .addRecordType(WeightRecord.class)
                                        .build())
                        .getToken();
        insertWeightRecordViaTestApp(daysBeforeNow(31), 10);
        insertWeightRecordViaTestApp(daysBeforeNow(60), 11);
        insertWeightRecordViaTestApp(daysBeforeNow(100), 12);

        List<String> logsRecordIds =
                getRecordIds(
                        getChangeLogs(new ChangeLogsRequest.Builder(token).build())
                                .getUpsertedRecords());

        assertThat(logsRecordIds).isEmpty();
    }

    private String insertStepsRecord(Instant startTime, Instant endTime, long value)
            throws InterruptedException {
        return insertRecordAndGetId(getStepsRecord(value, startTime, endTime));
    }

    private String insertWeightRecord(Instant time, double value) throws InterruptedException {
        return insertRecordAndGetId(getWeightRecord(value, time));
    }

    private String insertStepsRecordViaTestApp(Instant startTime, Instant endTime, long value)
            throws Exception {
        return mTestApp.insertRecord(getStepsRecord(value, startTime, endTime));
    }

    private String insertWeightRecordViaTestApp(Instant time, double value) throws Exception {
        return mTestApp.insertRecord(getWeightRecord(value, time));
    }

    private Instant daysBeforeNow(int days) {
        return mNow.minus(days, DAYS);
    }
}
