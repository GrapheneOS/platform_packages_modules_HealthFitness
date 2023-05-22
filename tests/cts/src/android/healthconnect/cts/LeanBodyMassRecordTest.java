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

package android.healthconnect.cts;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.health.connect.DeleteUsingFiltersRequest;
import android.health.connect.HealthConnectException;
import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.ReadRecordsRequestUsingIds;
import android.health.connect.RecordIdFilter;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.changelog.ChangeLogTokenRequest;
import android.health.connect.changelog.ChangeLogTokenResponse;
import android.health.connect.changelog.ChangeLogsRequest;
import android.health.connect.changelog.ChangeLogsResponse;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.Device;
import android.health.connect.datatypes.LeanBodyMassRecord;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.units.Mass;
import android.platform.test.annotations.AppModeFull;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@AppModeFull(reason = "HealthConnectManager is not accessible to instant apps")
@RunWith(AndroidJUnit4.class)
public class LeanBodyMassRecordTest {
    private static final String TAG = "LeanBodyMassRecordTest";

    @Before
    public void setUp() {
        // TODO(b/283737434): Update the HC code to use user aware context on permission change.
        // Temporary fix to set firstGrantTime for the correct user in HSUM.
        TestUtils.deleteAllStagedRemoteData();
    }

    @After
    public void tearDown() throws InterruptedException {
        TestUtils.verifyDeleteRecords(
                LeanBodyMassRecord.class,
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.EPOCH)
                        .setEndTime(Instant.now())
                        .build());
        TestUtils.deleteAllStagedRemoteData();
    }

    @Test
    public void testInsertLeanBodyMassRecord() throws InterruptedException {
        List<Record> records =
                List.of(getBaseLeanBodyMassRecord(), getCompleteLeanBodyMassRecord());
        TestUtils.insertRecords(records);
    }

    @Test
    public void testReadLeanBodyMassRecord_usingIds() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(getCompleteLeanBodyMassRecord(), getCompleteLeanBodyMassRecord());
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        readLeanBodyMassRecordUsingIds(insertedRecords);
    }

    @Test
    public void testReadLeanBodyMassRecord_invalidIds() throws InterruptedException {
        ReadRecordsRequestUsingIds<LeanBodyMassRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(LeanBodyMassRecord.class)
                        .addId(UUID.randomUUID().toString())
                        .build();
        List<LeanBodyMassRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadLeanBodyMassRecord_usingClientRecordIds() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(getCompleteLeanBodyMassRecord(), getCompleteLeanBodyMassRecord());
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        readLeanBodyMassRecordUsingClientId(insertedRecords);
    }

    @Test
    public void testReadLeanBodyMassRecord_invalidClientRecordIds() throws InterruptedException {
        ReadRecordsRequestUsingIds<LeanBodyMassRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(LeanBodyMassRecord.class)
                        .addClientRecordId("abc")
                        .build();
        List<LeanBodyMassRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadLeanBodyMassRecordUsingFilters_default() throws InterruptedException {
        List<LeanBodyMassRecord> oldLeanBodyMassRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(LeanBodyMassRecord.class)
                                .build());
        LeanBodyMassRecord testRecord = getCompleteLeanBodyMassRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<LeanBodyMassRecord> newLeanBodyMassRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(LeanBodyMassRecord.class)
                                .build());
        assertThat(newLeanBodyMassRecords.size()).isEqualTo(oldLeanBodyMassRecords.size() + 1);
        assertThat(newLeanBodyMassRecords.get(newLeanBodyMassRecords.size() - 1).equals(testRecord))
                .isTrue();
    }

    @Test
    public void testReadLeanBodyMassRecordUsingFilters_timeFilter() throws InterruptedException {
        TimeInstantRangeFilter filter =
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.now())
                        .setEndTime(Instant.now().plusMillis(3000))
                        .build();
        LeanBodyMassRecord testRecord = getCompleteLeanBodyMassRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<LeanBodyMassRecord> newLeanBodyMassRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(LeanBodyMassRecord.class)
                                .setTimeRangeFilter(filter)
                                .build());
        assertThat(newLeanBodyMassRecords.size()).isEqualTo(1);
        assertThat(newLeanBodyMassRecords.get(newLeanBodyMassRecords.size() - 1).equals(testRecord))
                .isTrue();
    }

    @Test
    public void testReadLeanBodyMassRecordUsingFilters_dataFilter_correct()
            throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        List<LeanBodyMassRecord> oldLeanBodyMassRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(LeanBodyMassRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        LeanBodyMassRecord testRecord = getCompleteLeanBodyMassRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<LeanBodyMassRecord> newLeanBodyMassRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(LeanBodyMassRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        assertThat(newLeanBodyMassRecords.size() - oldLeanBodyMassRecords.size()).isEqualTo(1);
        LeanBodyMassRecord newRecord =
                newLeanBodyMassRecords.get(newLeanBodyMassRecords.size() - 1);
        assertThat(newRecord.equals(testRecord)).isTrue();
        assertThat(newRecord.getMass()).isEqualTo(testRecord.getMass());
    }

    @Test
    public void testReadLeanBodyMassRecordUsingFilters_dataFilter_incorrect()
            throws InterruptedException {
        TestUtils.insertRecords(Collections.singletonList(getCompleteLeanBodyMassRecord()));
        List<LeanBodyMassRecord> newLeanBodyMassRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(LeanBodyMassRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder().setPackageName("abc").build())
                                .build());
        assertThat(newLeanBodyMassRecords.size()).isEqualTo(0);
    }

    private void readLeanBodyMassRecordUsingClientId(List<Record> insertedRecord)
            throws InterruptedException {
        ReadRecordsRequestUsingIds.Builder<LeanBodyMassRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(LeanBodyMassRecord.class);
        for (Record record : insertedRecord) {
            request.addClientRecordId(record.getMetadata().getClientRecordId());
        }
        List<LeanBodyMassRecord> result = TestUtils.readRecords(request.build());
        assertThat(result.size()).isEqualTo(insertedRecord.size());
        assertThat(result).containsExactlyElementsIn(insertedRecord);
    }

    private void readLeanBodyMassRecordUsingIds(List<Record> recordList)
            throws InterruptedException {
        ReadRecordsRequestUsingIds.Builder<LeanBodyMassRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(LeanBodyMassRecord.class);
        for (Record record : recordList) {
            request.addId(record.getMetadata().getId());
        }
        ReadRecordsRequestUsingIds requestUsingIds = request.build();
        assertThat(requestUsingIds.getRecordType()).isEqualTo(LeanBodyMassRecord.class);
        assertThat(requestUsingIds.getRecordIdFilters()).isNotNull();
        List<LeanBodyMassRecord> result = TestUtils.readRecords(requestUsingIds);
        assertThat(result).hasSize(recordList.size());
        assertThat(result).containsExactlyElementsIn(recordList);
    }

    @Test
    public void testDeleteLeanBodyMassRecord_no_filters() throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompleteLeanBodyMassRecord());
        TestUtils.verifyDeleteRecords(new DeleteUsingFiltersRequest.Builder().build());
        TestUtils.assertRecordNotFound(id, LeanBodyMassRecord.class);
    }

    @Test
    public void testDeleteLeanBodyMassRecord_time_filters() throws InterruptedException {
        TimeInstantRangeFilter timeRangeFilter =
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.now())
                        .setEndTime(Instant.now().plusMillis(1000))
                        .build();
        String id = TestUtils.insertRecordAndGetId(getCompleteLeanBodyMassRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addRecordType(LeanBodyMassRecord.class)
                        .setTimeRangeFilter(timeRangeFilter)
                        .build());
        TestUtils.assertRecordNotFound(id, LeanBodyMassRecord.class);
    }

    @Test
    public void testDeleteLeanBodyMassRecord_recordId_filters() throws InterruptedException {
        List<Record> records =
                List.of(getBaseLeanBodyMassRecord(), getCompleteLeanBodyMassRecord());
        TestUtils.insertRecords(records);

        for (Record record : records) {
            TestUtils.verifyDeleteRecords(
                    new DeleteUsingFiltersRequest.Builder()
                            .addRecordType(record.getClass())
                            .build());
            TestUtils.assertRecordNotFound(record.getMetadata().getId(), record.getClass());
        }
    }

    @Test
    public void testDeleteLeanBodyMassRecord_dataOrigin_filters() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        String id = TestUtils.insertRecordAndGetId(getCompleteLeanBodyMassRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(
                                new DataOrigin.Builder()
                                        .setPackageName(context.getPackageName())
                                        .build())
                        .build());
        TestUtils.assertRecordNotFound(id, LeanBodyMassRecord.class);
    }

    @Test
    public void testDeleteLeanBodyMassRecord_dataOrigin_filter_incorrect()
            throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompleteLeanBodyMassRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(new DataOrigin.Builder().setPackageName("abc").build())
                        .build());
        TestUtils.assertRecordFound(id, LeanBodyMassRecord.class);
    }

    @Test
    public void testDeleteLeanBodyMassRecord_usingIds() throws InterruptedException {
        List<Record> records =
                List.of(getBaseLeanBodyMassRecord(), getCompleteLeanBodyMassRecord());
        List<Record> insertedRecord = TestUtils.insertRecords(records);
        List<RecordIdFilter> recordIds = new ArrayList<>(records.size());
        for (Record record : insertedRecord) {
            recordIds.add(RecordIdFilter.fromId(record.getClass(), record.getMetadata().getId()));
        }

        TestUtils.verifyDeleteRecords(recordIds);
        for (Record record : records) {
            TestUtils.assertRecordNotFound(record.getMetadata().getId(), record.getClass());
        }
    }

    @Test
    public void testDeleteLeanBodyMassRecord_time_range() throws InterruptedException {
        TimeInstantRangeFilter timeRangeFilter =
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.now())
                        .setEndTime(Instant.now().plusMillis(1000))
                        .build();
        String id = TestUtils.insertRecordAndGetId(getCompleteLeanBodyMassRecord());
        TestUtils.verifyDeleteRecords(LeanBodyMassRecord.class, timeRangeFilter);
        TestUtils.assertRecordNotFound(id, LeanBodyMassRecord.class);
    }

    @Test
    public void testZoneOffsets() {
        final ZoneOffset defaultZoneOffset =
                ZoneOffset.systemDefault().getRules().getOffset(Instant.now());
        final ZoneOffset zoneOffset = ZoneOffset.UTC;
        LeanBodyMassRecord.Builder builder =
                new LeanBodyMassRecord.Builder(
                        new Metadata.Builder().build(), Instant.now(), Mass.fromGrams(10.0));

        assertThat(builder.setZoneOffset(zoneOffset).build().getZoneOffset()).isEqualTo(zoneOffset);
        assertThat(builder.clearZoneOffset().build().getZoneOffset()).isEqualTo(defaultZoneOffset);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateLeanBodyMassRecord_invalidValue() {
        new LeanBodyMassRecord.Builder(
                        new Metadata.Builder().build(), Instant.now(), Mass.fromGrams(1000000.1))
                .build();
    }

    @Test
    public void testUpdateRecords_validInput_dataBaseUpdatedSuccessfully()
            throws InterruptedException {

        List<Record> insertedRecords =
                TestUtils.insertRecords(
                        Arrays.asList(
                                getCompleteLeanBodyMassRecord(), getCompleteLeanBodyMassRecord()));

        // read inserted records and verify that the data is same as inserted.
        readLeanBodyMassRecordUsingIds(insertedRecords);

        // Generate a new set of records that will be used to perform the update operation.
        List<Record> updateRecords =
                Arrays.asList(getCompleteLeanBodyMassRecord(), getCompleteLeanBodyMassRecord());

        // Modify the uid of the updateRecords to the uuid that was present in the insert records.
        for (int itr = 0; itr < updateRecords.size(); itr++) {
            updateRecords.set(
                    itr,
                    getLeanBodyMassRecord_update(
                            updateRecords.get(itr),
                            insertedRecords.get(itr).getMetadata().getId(),
                            insertedRecords.get(itr).getMetadata().getClientRecordId()));
        }

        TestUtils.updateRecords(updateRecords);

        // assert the inserted data has been modified by reading the data.
        readLeanBodyMassRecordUsingIds(updateRecords);
    }

    @Test
    public void testUpdateRecords_invalidInputRecords_noChangeInDataBase()
            throws InterruptedException {
        List<Record> insertedRecords =
                TestUtils.insertRecords(
                        Arrays.asList(
                                getCompleteLeanBodyMassRecord(), getCompleteLeanBodyMassRecord()));

        // read inserted records and verify that the data is same as inserted.
        readLeanBodyMassRecordUsingIds(insertedRecords);

        // Generate a second set of records that will be used to perform the update operation.
        List<Record> updateRecords =
                Arrays.asList(getCompleteLeanBodyMassRecord(), getCompleteLeanBodyMassRecord());

        // Modify the Uid of the updateRecords to the UUID that was present in the insert records,
        // leaving out alternate records so that they have a new UUID which is not present in the
        // dataBase.
        for (int itr = 0; itr < updateRecords.size(); itr++) {
            updateRecords.set(
                    itr,
                    getLeanBodyMassRecord_update(
                            updateRecords.get(itr),
                            itr % 2 == 0
                                    ? insertedRecords.get(itr).getMetadata().getId()
                                    : UUID.randomUUID().toString(),
                            itr % 2 == 0
                                    ? insertedRecords.get(itr).getMetadata().getId()
                                    : UUID.randomUUID().toString()));
        }

        try {
            TestUtils.updateRecords(updateRecords);
            Assert.fail("Expected to fail due to invalid records ids.");
        } catch (HealthConnectException exception) {
            assertThat(exception.getErrorCode())
                    .isEqualTo(HealthConnectException.ERROR_INVALID_ARGUMENT);
        }

        // assert the inserted data has not been modified by reading the data.
        readLeanBodyMassRecordUsingIds(insertedRecords);
    }

    @Test
    public void testUpdateRecords_recordWithInvalidPackageName_noChangeInDataBase()
            throws InterruptedException {
        List<Record> insertedRecords =
                TestUtils.insertRecords(
                        Arrays.asList(
                                getCompleteLeanBodyMassRecord(), getCompleteLeanBodyMassRecord()));

        // read inserted records and verify that the data is same as inserted.
        readLeanBodyMassRecordUsingIds(insertedRecords);

        // Generate a second set of records that will be used to perform the update operation.
        List<Record> updateRecords =
                Arrays.asList(getCompleteLeanBodyMassRecord(), getCompleteLeanBodyMassRecord());

        // Modify the Uuid of the updateRecords to the uuid that was present in the insert records.
        for (int itr = 0; itr < updateRecords.size(); itr++) {
            updateRecords.set(
                    itr,
                    getLeanBodyMassRecord_update(
                            updateRecords.get(itr),
                            insertedRecords.get(itr).getMetadata().getId(),
                            insertedRecords.get(itr).getMetadata().getClientRecordId()));
            //             adding an entry with invalid packageName.
            updateRecords.set(itr, getCompleteLeanBodyMassRecord());
        }

        try {
            TestUtils.updateRecords(updateRecords);
            Assert.fail("Expected to fail due to invalid package.");
        } catch (Exception exception) {
            // verify that the testcase failed due to invalid argument exception.
            assertThat(exception).isNotNull();
        }

        // assert the inserted data has not been modified by reading the data.
        readLeanBodyMassRecordUsingIds(insertedRecords);
    }

    @Test
    public void testInsertAndDeleteRecord_changelogs() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        ChangeLogTokenResponse tokenResponse =
                TestUtils.getChangeLogToken(
                        new ChangeLogTokenRequest.Builder()
                                .addDataOriginFilter(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .addRecordType(LeanBodyMassRecord.class)
                                .build());
        ChangeLogsRequest changeLogsRequest =
                new ChangeLogsRequest.Builder(tokenResponse.getToken()).build();
        ChangeLogsResponse response = TestUtils.getChangeLogs(changeLogsRequest);
        assertThat(response.getUpsertedRecords().size()).isEqualTo(0);
        assertThat(response.getDeletedLogs().size()).isEqualTo(0);

        List<Record> testRecord = Collections.singletonList(getCompleteLeanBodyMassRecord());
        TestUtils.insertRecords(testRecord);
        response = TestUtils.getChangeLogs(changeLogsRequest);
        assertThat(response.getUpsertedRecords().size()).isEqualTo(1);
        assertThat(
                        response.getUpsertedRecords().stream()
                                .map(Record::getMetadata)
                                .map(Metadata::getId)
                                .toList())
                .containsExactlyElementsIn(
                        testRecord.stream().map(Record::getMetadata).map(Metadata::getId).toList());
        assertThat(response.getDeletedLogs().size()).isEqualTo(0);

        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addRecordType(LeanBodyMassRecord.class)
                        .build());
        response = TestUtils.getChangeLogs(changeLogsRequest);
        assertThat(response.getDeletedLogs()).isEmpty();
    }

    LeanBodyMassRecord getLeanBodyMassRecord_update(
            Record record, String id, String clientRecordId) {
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
        return new LeanBodyMassRecord.Builder(metadataWithId, Instant.now(), Mass.fromGrams(20.0))
                .setZoneOffset(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()))
                .build();
    }

    private static LeanBodyMassRecord getBaseLeanBodyMassRecord() {
        return new LeanBodyMassRecord.Builder(
                        new Metadata.Builder().build(), Instant.now(), Mass.fromGrams(10.0))
                .build();
    }

    static LeanBodyMassRecord getBaseLeanBodyMassRecord(Instant time, double value) {
        return new LeanBodyMassRecord.Builder(
                        new Metadata.Builder().build(), time, Mass.fromGrams(value))
                .build();
    }

    static LeanBodyMassRecord getCompleteLeanBodyMassRecord() {
        Device device =
                new Device.Builder()
                        .setManufacturer("google")
                        .setModel("Pixel4a")
                        .setType(2)
                        .build();
        DataOrigin dataOrigin =
                new DataOrigin.Builder().setPackageName("android.healthconnect.cts").build();
        Metadata.Builder testMetadataBuilder = new Metadata.Builder();
        testMetadataBuilder.setDevice(device).setDataOrigin(dataOrigin);
        testMetadataBuilder.setClientRecordId("LBMR" + Math.random());
        testMetadataBuilder.setRecordingMethod(Metadata.RECORDING_METHOD_ACTIVELY_RECORDED);

        return new LeanBodyMassRecord.Builder(
                        testMetadataBuilder.build(), Instant.now(), Mass.fromGrams(10.0))
                .setZoneOffset(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()))
                .build();
    }
}
