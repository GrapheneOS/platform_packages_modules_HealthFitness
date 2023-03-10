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
import android.health.connect.AggregateRecordsRequest;
import android.health.connect.AggregateRecordsResponse;
import android.health.connect.DeleteUsingFiltersRequest;
import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.ReadRecordsRequestUsingIds;
import android.health.connect.RecordIdFilter;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.Device;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.RestingHeartRateRecord;
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
public class RestingHeartRateRecordTest {
    private static final String TAG = "RestingHeartRateRecordTest";

    @After
    public void tearDown() throws InterruptedException {
        TestUtils.verifyDeleteRecords(
                RestingHeartRateRecord.class,
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.EPOCH)
                        .setEndTime(Instant.now())
                        .build());
    }

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
        TimeInstantRangeFilter filter =
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.now())
                        .setEndTime(Instant.now().plusMillis(3000))
                        .build();
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
        assertThat(newRecord.getBeatsPerMinute()).isEqualTo(testRecord.getBeatsPerMinute());
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
        ReadRecordsRequestUsingIds requestUsingIds = request.build();
        assertThat(requestUsingIds.getRecordType()).isEqualTo(RestingHeartRateRecord.class);
        assertThat(requestUsingIds.getRecordIdFilters()).isNotNull();
        List<RestingHeartRateRecord> result = TestUtils.readRecords(requestUsingIds);
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
        TimeInstantRangeFilter timeRangeFilter =
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.now())
                        .setEndTime(Instant.now().plusMillis(1000))
                        .build();
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
            recordIds.add(RecordIdFilter.fromId(record.getClass(), record.getMetadata().getId()));
        }

        TestUtils.verifyDeleteRecords(recordIds);
        for (Record record : records) {
            TestUtils.assertRecordNotFound(record.getMetadata().getId(), record.getClass());
        }
    }

    @Test
    public void testDeleteRestingHeartRateRecord_time_range() throws InterruptedException {
        TimeInstantRangeFilter timeRangeFilter =
                new TimeInstantRangeFilter.Builder()
                        .setStartTime(Instant.now())
                        .setEndTime(Instant.now().plusMillis(1000))
                        .build();
        String id = TestUtils.insertRecordAndGetId(getCompleteRestingHeartRateRecord());
        TestUtils.verifyDeleteRecords(RestingHeartRateRecord.class, timeRangeFilter);
        TestUtils.assertRecordNotFound(id, RestingHeartRateRecord.class);
    }

    @Test
    public void testBpmAggregation_timeRange_all() throws Exception {
        List<Record> records =
                Arrays.asList(
                        getBaseRestingHeartRateRecord(3),
                        getBaseRestingHeartRateRecord(5),
                        getBaseRestingHeartRateRecord(10));
        AggregateRecordsResponse<Long> response =
                TestUtils.getAggregateResponse(
                        new AggregateRecordsRequest.Builder<Long>(
                                        new TimeInstantRangeFilter.Builder()
                                                .setStartTime(Instant.ofEpochMilli(0))
                                                .setEndTime(Instant.now().plus(1, ChronoUnit.DAYS))
                                                .build())
                                .addAggregationType(RestingHeartRateRecord.BPM_MAX)
                                .addAggregationType(RestingHeartRateRecord.BPM_MIN)
                                .addAggregationType(RestingHeartRateRecord.BPM_AVG)
                                .build(),
                        records);
        assertThat(response.get(RestingHeartRateRecord.BPM_MAX)).isNotNull();
        assertThat(response.get(RestingHeartRateRecord.BPM_MAX)).isEqualTo(10);
        assertThat(response.getZoneOffset(RestingHeartRateRecord.BPM_MAX))
                .isEqualTo(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()));
        assertThat(response.get(RestingHeartRateRecord.BPM_MIN)).isNotNull();
        assertThat(response.get(RestingHeartRateRecord.BPM_MIN)).isEqualTo(3);
        assertThat(response.getZoneOffset(RestingHeartRateRecord.BPM_MIN))
                .isEqualTo(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()));
        assertThat(response.get(RestingHeartRateRecord.BPM_AVG)).isNotNull();
        assertThat(response.get(RestingHeartRateRecord.BPM_AVG)).isEqualTo(6);
        assertThat(response.getZoneOffset(RestingHeartRateRecord.BPM_AVG))
                .isEqualTo(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()));
        Set<DataOrigin> dataOrigins = response.getDataOrigins(RestingHeartRateRecord.BPM_MIN);
        for (DataOrigin itr : dataOrigins) {
            assertThat(itr.getPackageName()).isEqualTo("android.healthconnect.cts");
        }
    }

    @Test
    public void testZoneOffsets() {
        final ZoneOffset defaultZoneOffset =
                ZoneOffset.systemDefault().getRules().getOffset(Instant.now());
        final ZoneOffset zoneOffset = ZoneOffset.UTC;
        RestingHeartRateRecord.Builder builder =
                new RestingHeartRateRecord.Builder(
                        new Metadata.Builder().build(), Instant.now(), 1);

        assertThat(builder.setZoneOffset(zoneOffset).build().getZoneOffset()).isEqualTo(zoneOffset);
        assertThat(builder.clearZoneOffset().build().getZoneOffset()).isEqualTo(defaultZoneOffset);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateRestingHeartRateRecord_invalidValue() {
        new RestingHeartRateRecord.Builder(new Metadata.Builder().build(), Instant.now(), 500)
                .build();
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
        testMetadataBuilder.setRecordingMethod(Metadata.RECORDING_METHOD_ACTIVELY_RECORDED);

        return new RestingHeartRateRecord.Builder(testMetadataBuilder.build(), Instant.now(), 1)
                .setZoneOffset(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()))
                .build();
    }
}
