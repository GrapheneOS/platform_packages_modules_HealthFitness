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
import android.healthconnect.DeleteUsingFiltersRequest;
import android.healthconnect.ReadRecordsRequestUsingFilters;
import android.healthconnect.ReadRecordsRequestUsingIds;
import android.healthconnect.RecordIdFilter;
import android.healthconnect.TimeRangeFilter;
import android.healthconnect.datatypes.DataOrigin;
import android.healthconnect.datatypes.Device;
import android.healthconnect.datatypes.LeanBodyMassRecord;
import android.healthconnect.datatypes.Metadata;
import android.healthconnect.datatypes.Record;
import android.healthconnect.datatypes.units.Mass;
import android.platform.test.annotations.AppModeFull;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@AppModeFull(reason = "HealthConnectManager is not accessible to instant apps")
@RunWith(AndroidJUnit4.class)
public class LeanBodyMassRecordTest {
    private static final String TAG = "LeanBodyMassRecordTest";

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
        readLeanBodyMassRecordUsingIds(recordList);
    }

    @Test
    public void testReadLeanBodyMassRecord_invalidIds() throws InterruptedException {
        ReadRecordsRequestUsingIds<LeanBodyMassRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(LeanBodyMassRecord.class)
                        .addId("abc")
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
        TimeRangeFilter filter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(3000)).build();
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
        result.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));
        insertedRecord.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));

        for (int i = 0; i < result.size(); i++) {
            LeanBodyMassRecord other = (LeanBodyMassRecord) insertedRecord.get(i);
            assertThat(result.get(i).equals(other)).isTrue();
        }
    }

    private void readLeanBodyMassRecordUsingIds(List<Record> recordList)
            throws InterruptedException {
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        ReadRecordsRequestUsingIds.Builder<LeanBodyMassRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(LeanBodyMassRecord.class);
        for (Record record : insertedRecords) {
            request.addId(record.getMetadata().getId());
        }
        List<LeanBodyMassRecord> result = TestUtils.readRecords(request.build());
        assertThat(result).hasSize(insertedRecords.size());
        result.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));
        insertedRecords.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));

        for (int i = 0; i < result.size(); i++) {
            LeanBodyMassRecord other = (LeanBodyMassRecord) insertedRecords.get(i);
            assertThat(result.get(i).equals(other)).isTrue();
        }
    }

    @Test
    public void testDeleteLeanBodyMassRecord_no_filters() throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompleteLeanBodyMassRecord());
        TestUtils.verifyDeleteRecords(new DeleteUsingFiltersRequest.Builder().build());
        TestUtils.assertRecordNotFound(id, LeanBodyMassRecord.class);
    }

    @Test
    public void testDeleteLeanBodyMassRecord_time_filters() throws InterruptedException {
        TimeRangeFilter timeRangeFilter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(1000)).build();
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
        TimeRangeFilter timeRangeFilter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(1000)).build();
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
                        new Metadata.Builder().build(), Instant.now(), Mass.fromKilograms(10.0));

        assertThat(builder.setZoneOffset(zoneOffset).build().getZoneOffset()).isEqualTo(zoneOffset);
        assertThat(builder.clearZoneOffset().build().getZoneOffset()).isEqualTo(defaultZoneOffset);
    }

    private static LeanBodyMassRecord getBaseLeanBodyMassRecord() {
        return new LeanBodyMassRecord.Builder(
                        new Metadata.Builder().build(), Instant.now(), Mass.fromKilograms(10.0))
                .build();
    }

    private static LeanBodyMassRecord getCompleteLeanBodyMassRecord() {
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

        return new LeanBodyMassRecord.Builder(
                        testMetadataBuilder.build(), Instant.now(), Mass.fromKilograms(10.0))
                .setZoneOffset(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()))
                .build();
    }
}
