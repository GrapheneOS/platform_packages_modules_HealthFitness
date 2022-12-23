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

import static android.healthconnect.datatypes.ElevationGainedRecord.ELEVATION_GAINED_TOTAL;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.healthconnect.AggregateRecordsRequest;
import android.healthconnect.AggregateRecordsResponse;
import android.healthconnect.DeleteUsingFiltersRequest;
import android.healthconnect.ReadRecordsRequestUsingFilters;
import android.healthconnect.ReadRecordsRequestUsingIds;
import android.healthconnect.RecordIdFilter;
import android.healthconnect.TimeInstantRangeFilter;
import android.healthconnect.datatypes.DataOrigin;
import android.healthconnect.datatypes.Device;
import android.healthconnect.datatypes.ElevationGainedRecord;
import android.healthconnect.datatypes.Metadata;
import android.healthconnect.datatypes.Record;
import android.healthconnect.datatypes.units.Length;
import android.platform.test.annotations.AppModeFull;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@AppModeFull(reason = "HealthConnectManager is not accessible to instant apps")
@RunWith(AndroidJUnit4.class)
public class ElevationGainedRecordTest {
    private static final String TAG = "ElevationGainedRecordTest";

    @After
    public void tearDown() throws InterruptedException {
        TestUtils.verifyDeleteRecords(
                ElevationGainedRecord.class,
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.EPOCH)
                        .setEndTime(Instant.now())
                        .build());
    }

    @Test
    public void testInsertElevationGainedRecord() throws InterruptedException {
        List<Record> records =
                List.of(getBaseElevationGainedRecord(), getCompleteElevationGainedRecord());
        TestUtils.insertRecords(records);
    }

    @Test
    public void testReadElevationGainedRecord_usingIds() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(
                        getCompleteElevationGainedRecord(), getCompleteElevationGainedRecord());
        readElevationGainedRecordUsingIds(recordList);
    }

    @Test
    public void testReadElevationGainedRecord_invalidIds() throws InterruptedException {
        ReadRecordsRequestUsingIds<ElevationGainedRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(ElevationGainedRecord.class)
                        .addId("abc")
                        .build();
        List<ElevationGainedRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadElevationGainedRecord_usingClientRecordIds() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(
                        getCompleteElevationGainedRecord(), getCompleteElevationGainedRecord());
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        readElevationGainedRecordUsingClientId(insertedRecords);
    }

    @Test
    public void testReadElevationGainedRecord_invalidClientRecordIds() throws InterruptedException {
        ReadRecordsRequestUsingIds<ElevationGainedRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(ElevationGainedRecord.class)
                        .addClientRecordId("abc")
                        .build();
        List<ElevationGainedRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadElevationGainedRecordUsingFilters_default() throws InterruptedException {
        List<ElevationGainedRecord> oldElevationGainedRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(ElevationGainedRecord.class)
                                .build());
        ElevationGainedRecord testRecord = getCompleteElevationGainedRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<ElevationGainedRecord> newElevationGainedRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(ElevationGainedRecord.class)
                                .build());
        assertThat(newElevationGainedRecords.size())
                .isEqualTo(oldElevationGainedRecords.size() + 1);
        assertThat(
                        newElevationGainedRecords
                                .get(newElevationGainedRecords.size() - 1)
                                .equals(testRecord))
                .isTrue();
    }

    @Test
    public void testReadElevationGainedRecordUsingFilters_timeFilter() throws InterruptedException {
        TimeInstantRangeFilter filter =
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.now())
                        .setEndTime(Instant.now().plusMillis(3000))
                        .build();
        ElevationGainedRecord testRecord = getCompleteElevationGainedRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<ElevationGainedRecord> newElevationGainedRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(ElevationGainedRecord.class)
                                .setTimeRangeFilter(filter)
                                .build());
        assertThat(newElevationGainedRecords.size()).isEqualTo(1);
        assertThat(
                        newElevationGainedRecords
                                .get(newElevationGainedRecords.size() - 1)
                                .equals(testRecord))
                .isTrue();
    }

    @Test
    public void testReadElevationGainedRecordUsingFilters_dataFilter_correct()
            throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        List<ElevationGainedRecord> oldElevationGainedRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(ElevationGainedRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        ElevationGainedRecord testRecord = getCompleteElevationGainedRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<ElevationGainedRecord> newElevationGainedRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(ElevationGainedRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        assertThat(newElevationGainedRecords.size() - oldElevationGainedRecords.size())
                .isEqualTo(1);
        ElevationGainedRecord newRecord =
                newElevationGainedRecords.get(newElevationGainedRecords.size() - 1);
        assertThat(newRecord.equals(testRecord)).isTrue();
    }

    @Test
    public void testReadElevationGainedRecordUsingFilters_dataFilter_incorrect()
            throws InterruptedException {
        TestUtils.insertRecords(Collections.singletonList(getCompleteElevationGainedRecord()));
        List<ElevationGainedRecord> newElevationGainedRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(ElevationGainedRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder().setPackageName("abc").build())
                                .build());
        assertThat(newElevationGainedRecords.size()).isEqualTo(0);
    }

    @Test
    public void testDeleteElevationGainedRecord_no_filters() throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompleteElevationGainedRecord());
        TestUtils.verifyDeleteRecords(new DeleteUsingFiltersRequest.Builder().build());
        TestUtils.assertRecordNotFound(id, ElevationGainedRecord.class);
    }

    @Test
    public void testDeleteElevationGainedRecord_time_filters() throws InterruptedException {
        TimeInstantRangeFilter timeRangeFilter =
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.now())
                        .setEndTime(Instant.now().plusMillis(1000))
                        .build();
        String id = TestUtils.insertRecordAndGetId(getCompleteElevationGainedRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addRecordType(ElevationGainedRecord.class)
                        .setTimeRangeFilter(timeRangeFilter)
                        .build());
        TestUtils.assertRecordNotFound(id, ElevationGainedRecord.class);
    }

    @Test
    public void testDeleteElevationGainedRecord_recordId_filters() throws InterruptedException {
        List<Record> records =
                List.of(getBaseElevationGainedRecord(), getCompleteElevationGainedRecord());
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
    public void testDeleteElevationGainedRecord_dataOrigin_filters() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        String id = TestUtils.insertRecordAndGetId(getCompleteElevationGainedRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(
                                new DataOrigin.Builder()
                                        .setPackageName(context.getPackageName())
                                        .build())
                        .build());
        TestUtils.assertRecordNotFound(id, ElevationGainedRecord.class);
    }

    @Test
    public void testDeleteElevationGainedRecord_dataOrigin_filter_incorrect()
            throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompleteElevationGainedRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(new DataOrigin.Builder().setPackageName("abc").build())
                        .build());
        TestUtils.assertRecordFound(id, ElevationGainedRecord.class);
    }

    @Test
    public void testDeleteElevationGainedRecord_usingIds() throws InterruptedException {
        List<Record> records =
                List.of(getBaseElevationGainedRecord(), getCompleteElevationGainedRecord());
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
    public void testDeleteElevationGainedRecord_time_range() throws InterruptedException {
        TimeInstantRangeFilter timeRangeFilter =
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.now())
                        .setEndTime(Instant.now().plusMillis(1000))
                        .build();
        String id = TestUtils.insertRecordAndGetId(getCompleteElevationGainedRecord());
        TestUtils.verifyDeleteRecords(ElevationGainedRecord.class, timeRangeFilter);
        TestUtils.assertRecordNotFound(id, ElevationGainedRecord.class);
    }

    private void readElevationGainedRecordUsingClientId(List<Record> insertedRecord)
            throws InterruptedException {
        ReadRecordsRequestUsingIds.Builder<ElevationGainedRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(ElevationGainedRecord.class);
        for (Record record : insertedRecord) {
            request.addClientRecordId(record.getMetadata().getClientRecordId());
        }
        List<ElevationGainedRecord> result = TestUtils.readRecords(request.build());
        result.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));
        insertedRecord.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));

        for (int i = 0; i < result.size(); i++) {
            ElevationGainedRecord other = (ElevationGainedRecord) insertedRecord.get(i);
            assertThat(result.get(i).equals(other)).isTrue();
        }
    }

    private void readElevationGainedRecordUsingIds(List<Record> recordList)
            throws InterruptedException {
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        ReadRecordsRequestUsingIds.Builder<ElevationGainedRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(ElevationGainedRecord.class);
        for (Record record : insertedRecords) {
            request.addId(record.getMetadata().getId());
        }
        ReadRecordsRequestUsingIds requestUsingIds = request.build();
        assertThat(requestUsingIds.getRecordType()).isEqualTo(ElevationGainedRecord.class);
        assertThat(requestUsingIds.getRecordIdFilters()).isNotNull();
        List<ElevationGainedRecord> result = TestUtils.readRecords(requestUsingIds);
        assertThat(result).hasSize(insertedRecords.size());
        result.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));
        insertedRecords.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));

        for (int i = 0; i < result.size(); i++) {
            ElevationGainedRecord other = (ElevationGainedRecord) insertedRecords.get(i);
            assertThat(result.get(i).equals(other)).isTrue();
        }
    }

    @Test
    public void testAggregation_ElevationTotal() throws Exception {
        List<Record> records =
                Arrays.asList(
                        ElevationGainedRecordTest.getElevationGainedRecord(74.0),
                        ElevationGainedRecordTest.getElevationGainedRecord(100.5));
        AggregateRecordsResponse<Length> oldResponse =
                TestUtils.getAggregateResponse(
                        new AggregateRecordsRequest.Builder<Length>(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(Instant.ofEpochMilli(0))
                                                .setEndTime(Instant.now().plus(1, ChronoUnit.DAYS))
                                                .build())
                                .addAggregationType(ELEVATION_GAINED_TOTAL)
                                .build(),
                        records);
        List<Record> recordNew =
                Arrays.asList(ElevationGainedRecordTest.getElevationGainedRecord(100.5));
        AggregateRecordsResponse<Length> newResponse =
                TestUtils.getAggregateResponse(
                        new AggregateRecordsRequest.Builder<Length>(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(Instant.ofEpochMilli(0))
                                                .setEndTime(Instant.now().plus(1, ChronoUnit.DAYS))
                                                .build())
                                .addAggregationType(ELEVATION_GAINED_TOTAL)
                                .build(),
                        recordNew);
        Length newElevation = newResponse.get(ELEVATION_GAINED_TOTAL);
        Length oldElevation = oldResponse.get(ELEVATION_GAINED_TOTAL);
        assertThat(newElevation).isNotNull();
        assertThat(oldElevation).isNotNull();
        assertThat(newElevation.getInMeters() - oldElevation.getInMeters()).isEqualTo(100.5);
        Set<DataOrigin> newDataOrigin = newResponse.getDataOrigins(ELEVATION_GAINED_TOTAL);
        for (DataOrigin itr : newDataOrigin) {
            assertThat(itr.getPackageName()).isEqualTo("android.healthconnect.cts");
        }
        Set<DataOrigin> oldDataOrigin = oldResponse.getDataOrigins(ELEVATION_GAINED_TOTAL);
        for (DataOrigin itr : oldDataOrigin) {
            assertThat(itr.getPackageName()).isEqualTo("android.healthconnect.cts");
        }
    }

    @Test
    public void testZoneOffsets() {
        final ZoneOffset defaultZoneOffset =
                ZoneOffset.systemDefault().getRules().getOffset(Instant.now());
        final ZoneOffset startZoneOffset = ZoneOffset.UTC;
        final ZoneOffset endZoneOffset = ZoneOffset.MAX;
        ElevationGainedRecord.Builder builder =
                new ElevationGainedRecord.Builder(
                        new Metadata.Builder().build(),
                        Instant.now(),
                        Instant.now(),
                        Length.fromMeters(10.0));

        assertThat(builder.setStartZoneOffset(startZoneOffset).build().getStartZoneOffset())
                .isEqualTo(startZoneOffset);
        assertThat(builder.setEndZoneOffset(endZoneOffset).build().getEndZoneOffset())
                .isEqualTo(endZoneOffset);
        assertThat(builder.clearStartZoneOffset().build().getStartZoneOffset())
                .isEqualTo(defaultZoneOffset);
        assertThat(builder.clearEndZoneOffset().build().getEndZoneOffset())
                .isEqualTo(defaultZoneOffset);
    }

    static ElevationGainedRecord getBaseElevationGainedRecord() {
        return new ElevationGainedRecord.Builder(
                        new Metadata.Builder().build(),
                        Instant.now(),
                        Instant.now(),
                        Length.fromMeters(10.0))
                .build();
    }

    static ElevationGainedRecord getElevationGainedRecord(double elevation) {
        return new ElevationGainedRecord.Builder(
                        new Metadata.Builder().build(),
                        Instant.now(),
                        Instant.now(),
                        Length.fromMeters(elevation))
                .build();
    }

    static ElevationGainedRecord getCompleteElevationGainedRecord() {

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
        testMetadataBuilder.setClientRecordId("EGR" + Math.random());

        return new ElevationGainedRecord.Builder(
                        testMetadataBuilder.build(),
                        Instant.now(),
                        Instant.now(),
                        Length.fromMeters(10.0))
                .setStartZoneOffset(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()))
                .setEndZoneOffset(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()))
                .build();
    }
}
