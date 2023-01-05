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
import android.healthconnect.AggregateRecordsRequest;
import android.healthconnect.AggregateRecordsResponse;
import android.healthconnect.DeleteUsingFiltersRequest;
import android.healthconnect.ReadRecordsRequestUsingFilters;
import android.healthconnect.ReadRecordsRequestUsingIds;
import android.healthconnect.RecordIdFilter;
import android.healthconnect.TimeRangeFilter;
import android.healthconnect.datatypes.DataOrigin;
import android.healthconnect.datatypes.Device;
import android.healthconnect.datatypes.Metadata;
import android.healthconnect.datatypes.Record;
import android.healthconnect.datatypes.RestingHeartRateRecord;
import android.platform.test.annotations.AppModeFull;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.runner.AndroidJUnit4;

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

@AppModeFull(reason = "HealthConnectManager is not accessible to instant apps")
@RunWith(AndroidJUnit4.class)
public class RestingHeartRateRecordTest {
    private static final String TAG = "RestingHeartRateRecordTest";

    @Test
    public void testInsertRestingHeartRateRecord() throws InterruptedException {
        List<Record> records =
                List.of(getBaseRestingHeartRateRecord(), getCompleteRestingHeartRateRecord());
        TestUtils.insertRecords(records);
    }

    @Test
    public void testReadRestingHeartRateRecord_usingIds() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(
                        getCompleteRestingHeartRateRecord(), getCompleteRestingHeartRateRecord());
        readRestingHeartRateRecordUsingIds(recordList);
    }

    @Test
    public void testReadRestingHeartRateRecord_invalidIds() throws InterruptedException {
        ReadRecordsRequestUsingIds<RestingHeartRateRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(RestingHeartRateRecord.class)
                        .addId("abc")
                        .build();
        List<RestingHeartRateRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadRestingHeartRateRecord_usingClientRecordIds() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(
                        getCompleteRestingHeartRateRecord(), getCompleteRestingHeartRateRecord());
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        readRestingHeartRateRecordUsingClientId(insertedRecords);
    }

    @Test
    public void testReadRestingHeartRateRecord_invalidClientRecordIds()
            throws InterruptedException {
        ReadRecordsRequestUsingIds<RestingHeartRateRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(RestingHeartRateRecord.class)
                        .addClientRecordId("abc")
                        .build();
        List<RestingHeartRateRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadRestingHeartRateRecordUsingFilters_default() throws InterruptedException {
        List<RestingHeartRateRecord> oldRestingHeartRateRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(RestingHeartRateRecord.class)
                                .build());
        RestingHeartRateRecord testRecord = getCompleteRestingHeartRateRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<RestingHeartRateRecord> newRestingHeartRateRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(RestingHeartRateRecord.class)
                                .build());
        assertThat(newRestingHeartRateRecords.size())
                .isEqualTo(oldRestingHeartRateRecords.size() + 1);
        assertThat(
                        newRestingHeartRateRecords
                                .get(newRestingHeartRateRecords.size() - 1)
                                .equals(testRecord))
                .isTrue();
    }

    @Test
    public void testReadRestingHeartRateRecordUsingFilters_timeFilter()
            throws InterruptedException {
        TimeRangeFilter filter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(3000)).build();
        RestingHeartRateRecord testRecord = getCompleteRestingHeartRateRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<RestingHeartRateRecord> newRestingHeartRateRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(RestingHeartRateRecord.class)
                                .setTimeRangeFilter(filter)
                                .build());
        assertThat(newRestingHeartRateRecords.size()).isEqualTo(1);
        assertThat(
                        newRestingHeartRateRecords
                                .get(newRestingHeartRateRecords.size() - 1)
                                .equals(testRecord))
                .isTrue();
    }

    @Test
    public void testReadRestingHeartRateRecordUsingFilters_dataFilter_correct()
            throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        List<RestingHeartRateRecord> oldRestingHeartRateRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(RestingHeartRateRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        RestingHeartRateRecord testRecord = getCompleteRestingHeartRateRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<RestingHeartRateRecord> newRestingHeartRateRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(RestingHeartRateRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        assertThat(newRestingHeartRateRecords.size() - oldRestingHeartRateRecords.size())
                .isEqualTo(1);
        RestingHeartRateRecord newRecord =
                newRestingHeartRateRecords.get(newRestingHeartRateRecords.size() - 1);
        assertThat(newRecord.equals(testRecord)).isTrue();
    }

    @Test
    public void testReadRestingHeartRateRecordUsingFilters_dataFilter_incorrect()
            throws InterruptedException {
        TestUtils.insertRecords(Collections.singletonList(getCompleteRestingHeartRateRecord()));
        List<RestingHeartRateRecord> newRestingHeartRateRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(RestingHeartRateRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder().setPackageName("abc").build())
                                .build());
        assertThat(newRestingHeartRateRecords.size()).isEqualTo(0);
    }

    private void readRestingHeartRateRecordUsingClientId(List<Record> insertedRecord)
            throws InterruptedException {
        ReadRecordsRequestUsingIds.Builder<RestingHeartRateRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(RestingHeartRateRecord.class);
        for (Record record : insertedRecord) {
            request.addClientRecordId(record.getMetadata().getClientRecordId());
        }
        List<RestingHeartRateRecord> result = TestUtils.readRecords(request.build());
        result.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));
        insertedRecord.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));

        for (int i = 0; i < result.size(); i++) {
            RestingHeartRateRecord other = (RestingHeartRateRecord) insertedRecord.get(i);
            assertThat(result.get(i).equals(other)).isTrue();
        }
    }

    private void readRestingHeartRateRecordUsingIds(List<Record> recordList)
            throws InterruptedException {
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        ReadRecordsRequestUsingIds.Builder<RestingHeartRateRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(RestingHeartRateRecord.class);
        for (Record record : insertedRecords) {
            request.addId(record.getMetadata().getId());
        }
        List<RestingHeartRateRecord> result = TestUtils.readRecords(request.build());
        assertThat(result).hasSize(insertedRecords.size());
        result.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));
        insertedRecords.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));

        for (int i = 0; i < result.size(); i++) {
            RestingHeartRateRecord other = (RestingHeartRateRecord) insertedRecords.get(i);
            assertThat(result.get(i).equals(other)).isTrue();
        }
    }

    @Test
    public void testDeleteRestingHeartRateRecord_no_filters() throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompleteRestingHeartRateRecord());
        TestUtils.verifyDeleteRecords(new DeleteUsingFiltersRequest.Builder().build());
        TestUtils.assertRecordNotFound(id, RestingHeartRateRecord.class);
    }

    @Test
    public void testDeleteRestingHeartRateRecord_time_filters() throws InterruptedException {
        TimeRangeFilter timeRangeFilter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(1000)).build();
        String id = TestUtils.insertRecordAndGetId(getCompleteRestingHeartRateRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addRecordType(RestingHeartRateRecord.class)
                        .setTimeRangeFilter(timeRangeFilter)
                        .build());
        TestUtils.assertRecordNotFound(id, RestingHeartRateRecord.class);
    }

    @Test
    public void testDeleteRestingHeartRateRecord_recordId_filters() throws InterruptedException {
        List<Record> records =
                List.of(getBaseRestingHeartRateRecord(), getCompleteRestingHeartRateRecord());
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
    public void testDeleteRestingHeartRateRecord_dataOrigin_filters() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        String id = TestUtils.insertRecordAndGetId(getCompleteRestingHeartRateRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(
                                new DataOrigin.Builder()
                                        .setPackageName(context.getPackageName())
                                        .build())
                        .build());
        TestUtils.assertRecordNotFound(id, RestingHeartRateRecord.class);
    }

    @Test
    public void testDeleteRestingHeartRateRecord_dataOrigin_filter_incorrect()
            throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompleteRestingHeartRateRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(new DataOrigin.Builder().setPackageName("abc").build())
                        .build());
        TestUtils.assertRecordFound(id, RestingHeartRateRecord.class);
    }

    @Test
    public void testDeleteRestingHeartRateRecord_usingIds() throws InterruptedException {
        List<Record> records =
                List.of(getBaseRestingHeartRateRecord(), getCompleteRestingHeartRateRecord());
        List<Record> insertedRecord = TestUtils.insertRecords(records);
        List<RecordIdFilter> recordIds = new ArrayList<>(records.size());
        for (Record record : insertedRecord) {
            recordIds.add(
                    new RecordIdFilter.Builder(record.getClass())
                            .setId(record.getMetadata().getId())
                            .build());
        }

        TestUtils.verifyDeleteRecords(recordIds);
        for (Record record : records) {
            TestUtils.assertRecordNotFound(record.getMetadata().getId(), record.getClass());
        }
    }

    @Test
    public void testDeleteRestingHeartRateRecord_time_range() throws InterruptedException {
        TimeRangeFilter timeRangeFilter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(1000)).build();
        String id = TestUtils.insertRecordAndGetId(getCompleteRestingHeartRateRecord());
        TestUtils.verifyDeleteRecords(RestingHeartRateRecord.class, timeRangeFilter);
        TestUtils.assertRecordNotFound(id, RestingHeartRateRecord.class);
    }

    @Test
    public void testBpmAggregation_timeRange_all() throws Exception {
        List<Record> records =
                Arrays.asList(
                        getBaseRestingHeartRateRecord(1),
                        getBaseRestingHeartRateRecord(5),
                        getBaseRestingHeartRateRecord(10));
        AggregateRecordsResponse<Long> response =
                TestUtils.getAggregateResponse(
                        new AggregateRecordsRequest.Builder<Long>(
                                        new TimeRangeFilter.Builder(
                                                        Instant.ofEpochMilli(0),
                                                        Instant.now().plus(1, ChronoUnit.DAYS))
                                                .build())
                                .addAggregationType(RestingHeartRateRecord.BPM_MAX)
                                .addAggregationType(RestingHeartRateRecord.BPM_MIN)
                                .build(),
                        records);
        assertThat(response.get(RestingHeartRateRecord.BPM_MAX)).isNotNull();
        assertThat(response.get(RestingHeartRateRecord.BPM_MAX)).isEqualTo(10);
        assertThat(response.getZoneOffset(RestingHeartRateRecord.BPM_MAX))
                .isEqualTo(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()));
        assertThat(response.get(RestingHeartRateRecord.BPM_MIN)).isNotNull();
        assertThat(response.get(RestingHeartRateRecord.BPM_MIN)).isEqualTo(1);
        assertThat(response.getZoneOffset(RestingHeartRateRecord.BPM_MIN))
                .isEqualTo(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()));
    }

    private static RestingHeartRateRecord getBaseRestingHeartRateRecord() {
        return new RestingHeartRateRecord.Builder(new Metadata.Builder().build(), Instant.now(), 1)
                .build();
    }

    static RestingHeartRateRecord getBaseRestingHeartRateRecord(int beats) {
        return new RestingHeartRateRecord.Builder(
                        new Metadata.Builder().build(), Instant.now(), beats)
                .build();
    }

    private static RestingHeartRateRecord getCompleteRestingHeartRateRecord() {
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
        testMetadataBuilder.setClientRecordId("RHRR" + Math.random());

        return new RestingHeartRateRecord.Builder(testMetadataBuilder.build(), Instant.now(), 1)
                .setZoneOffset(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()))
                .build();
    }
}
