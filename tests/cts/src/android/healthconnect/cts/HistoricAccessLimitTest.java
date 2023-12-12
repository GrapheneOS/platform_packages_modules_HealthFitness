package android.healthconnect.cts;

import static android.health.connect.datatypes.StepsRecord.STEPS_COUNT_TOTAL;
import static android.health.connect.datatypes.WeightRecord.WEIGHT_AVG;
import static android.healthconnect.cts.utils.TestUtils.deleteAllStagedRemoteData;
import static android.healthconnect.cts.utils.TestUtils.sendCommandToTestAppReceiver;
import static android.healthconnect.test.app.TestAppReceiver.ACTION_INSERT_STEPS_RECORDS;
import static android.healthconnect.test.app.TestAppReceiver.ACTION_INSERT_WEIGHT_RECORDS;
import static android.healthconnect.test.app.TestAppReceiver.EXTRA_END_TIMES;
import static android.healthconnect.test.app.TestAppReceiver.EXTRA_RECORD_IDS;
import static android.healthconnect.test.app.TestAppReceiver.EXTRA_RECORD_VALUES;
import static android.healthconnect.test.app.TestAppReceiver.EXTRA_TIMES;

import static com.google.common.truth.Truth.assertThat;

import static java.time.temporal.ChronoUnit.DAYS;

import android.content.Context;
import android.health.connect.AggregateRecordsRequest;
import android.health.connect.AggregateRecordsResponse;
import android.health.connect.HealthDataCategory;
import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.ReadRecordsRequestUsingIds;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.StepsRecord;
import android.health.connect.datatypes.WeightRecord;
import android.health.connect.datatypes.units.Mass;
import android.healthconnect.cts.utils.TestReceiver;
import android.healthconnect.cts.utils.TestUtils;
import android.os.Bundle;
import android.platform.test.annotations.AppModeFull;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@AppModeFull(reason = "HealthConnectManager is not accessible to instant apps")
@RunWith(AndroidJUnit4.class)
public class HistoricAccessLimitTest {
    private Context mContext;
    private Instant mNow;

    private static final String PACKAGE_NAME = "android.healthconnect.cts";

    @Before
    public void setUp() throws InterruptedException {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        mNow = Instant.now();
        deleteAllStagedRemoteData();
        TestReceiver.reset();
    }

    @After
    public void tearDown() throws InterruptedException {
        deleteAllStagedRemoteData();
    }

    @Test
    public void testReadIntervalRecordsByFilters_expectCorrectResponse()
            throws InterruptedException {
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
    public void testReadInstantRecordsByFilters_expectCorrectResponse()
            throws InterruptedException {
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
    public void testReadIntervalRecordsByIds_expectCorrectResponse() throws InterruptedException {
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
    public void testReadInstantRecordsByIds_expectCorrectResponse() throws InterruptedException {
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
    public void testAggregateIntervalRecords_expectCorrectResponse() throws InterruptedException {
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
                daysBeforeNow(50), daysBeforeNow(40), otherAppsRecordValueBeforeHistoricLimit);
        // Add the other app to the priority list
        TestUtils.updatePriorityWithManageHealthDataPermission(
                HealthDataCategory.ACTIVITY,
                Arrays.asList(PACKAGE_NAME, "android.healthconnect.test.app"));
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
    public void testAggregateInstantRecords_expectCorrectResponse() throws InterruptedException {
        TestUtils.setupAggregation(PACKAGE_NAME, HealthDataCategory.BODY_MEASUREMENTS);
        long ownRecordValueAfterHistoricLimit = 20;
        long ownRecordValueBeforeHistoricLimit = 300;
        long otherAppsRecordValueAfterHistoricLimit = 4_000;
        long otherAppsRecordValueBeforeHistoricLimit = 50_000;
        insertWeightRecord(daysBeforeNow(10), ownRecordValueAfterHistoricLimit);
        insertWeightRecord(daysBeforeNow(50), ownRecordValueBeforeHistoricLimit);
        insertWeightRecordViaTestApp(daysBeforeNow(2), otherAppsRecordValueAfterHistoricLimit);
        insertWeightRecordViaTestApp(daysBeforeNow(50), otherAppsRecordValueBeforeHistoricLimit);
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

    private String insertStepsRecord(Instant startTime, Instant endTime, long value)
            throws InterruptedException {
        return TestUtils.insertRecordAndGetId(
                new StepsRecord.Builder(new Metadata.Builder().build(), startTime, endTime, value)
                        .build());
    }

    private String insertWeightRecord(Instant time, long value) throws InterruptedException {
        return TestUtils.insertRecordAndGetId(
                new WeightRecord.Builder(
                                new Metadata.Builder().build(),
                                time,
                                Mass.fromGrams((double) value))
                        .build());
    }

    private String insertStepsRecordViaTestApp(Instant startTime, Instant endTime, long value) {
        Bundle bundle = new Bundle();
        bundle.putLongArray(EXTRA_TIMES, new long[] {startTime.toEpochMilli()});
        bundle.putLongArray(EXTRA_END_TIMES, new long[] {endTime.toEpochMilli()});
        bundle.putLongArray(EXTRA_RECORD_VALUES, new long[] {value});
        TestReceiver.reset();
        sendCommandToTestAppReceiver(mContext, ACTION_INSERT_STEPS_RECORDS, bundle);
        return TestReceiver.getResult().getStringArrayList(EXTRA_RECORD_IDS).get(0);
    }

    private String insertWeightRecordViaTestApp(Instant startTime, long value) {
        Bundle bundle = new Bundle();
        bundle.putLongArray(EXTRA_TIMES, new long[] {startTime.toEpochMilli()});
        bundle.putLongArray(EXTRA_RECORD_VALUES, new long[] {value});
        TestReceiver.reset();
        sendCommandToTestAppReceiver(mContext, ACTION_INSERT_WEIGHT_RECORDS, bundle);
        return TestReceiver.getResult().getStringArrayList(EXTRA_RECORD_IDS).get(0);
    }

    private Instant daysBeforeNow(int days) {
        return mNow.minus(days, DAYS);
    }

    private static List<String> getRecordIds(List<? extends Record> records) {
        return records.stream().map(Record::getMetadata).map(Metadata::getId).toList();
    }
}
