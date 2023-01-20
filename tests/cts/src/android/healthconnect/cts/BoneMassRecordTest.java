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
import android.healthconnect.datatypes.BoneMassRecord;
import android.healthconnect.datatypes.DataOrigin;
import android.healthconnect.datatypes.Device;
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
public class BoneMassRecordTest {
    private static final String TAG = "BoneMassRecordTest";

    @Test
    public void testInsertBoneMassRecord() throws InterruptedException {
        List<Record> records = List.of(getBaseBoneMassRecord(), getCompleteBoneMassRecord());
        TestUtils.insertRecords(records);
    }

    @Test
    public void testReadBoneMassRecord_usingIds() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(getCompleteBoneMassRecord(), getCompleteBoneMassRecord());
        readBoneMassRecordUsingIds(recordList);
    }

    @Test
    public void testReadBoneMassRecord_invalidIds() throws InterruptedException {
        ReadRecordsRequestUsingIds<BoneMassRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(BoneMassRecord.class).addId("abc").build();
        List<BoneMassRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadBoneMassRecord_usingClientRecordIds() throws InterruptedException {
        List<Record> recordList =
                Arrays.asList(getCompleteBoneMassRecord(), getCompleteBoneMassRecord());
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        readBoneMassRecordUsingClientId(insertedRecords);
    }

    @Test
    public void testReadBoneMassRecord_invalidClientRecordIds() throws InterruptedException {
        ReadRecordsRequestUsingIds<BoneMassRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(BoneMassRecord.class)
                        .addClientRecordId("abc")
                        .build();
        List<BoneMassRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadBoneMassRecordUsingFilters_default() throws InterruptedException {
        List<BoneMassRecord> oldBoneMassRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(BoneMassRecord.class).build());
        BoneMassRecord testRecord = getCompleteBoneMassRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<BoneMassRecord> newBoneMassRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(BoneMassRecord.class).build());
        assertThat(newBoneMassRecords.size()).isEqualTo(oldBoneMassRecords.size() + 1);
        assertThat(newBoneMassRecords.get(newBoneMassRecords.size() - 1).equals(testRecord))
                .isTrue();
    }

    @Test
    public void testReadBoneMassRecordUsingFilters_timeFilter() throws InterruptedException {
        TimeRangeFilter filter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(3000)).build();
        BoneMassRecord testRecord = getCompleteBoneMassRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<BoneMassRecord> newBoneMassRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(BoneMassRecord.class)
                                .setTimeRangeFilter(filter)
                                .build());
        assertThat(newBoneMassRecords.size()).isEqualTo(1);
        assertThat(newBoneMassRecords.get(newBoneMassRecords.size() - 1).equals(testRecord))
                .isTrue();
    }

    @Test
    public void testReadBoneMassRecordUsingFilters_dataFilter_correct()
            throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        List<BoneMassRecord> oldBoneMassRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(BoneMassRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        BoneMassRecord testRecord = getCompleteBoneMassRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<BoneMassRecord> newBoneMassRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(BoneMassRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        assertThat(newBoneMassRecords.size() - oldBoneMassRecords.size()).isEqualTo(1);
        BoneMassRecord newRecord = newBoneMassRecords.get(newBoneMassRecords.size() - 1);
        assertThat(newRecord.equals(testRecord)).isTrue();
    }

    @Test
    public void testReadBoneMassRecordUsingFilters_dataFilter_incorrect()
            throws InterruptedException {
        TestUtils.insertRecords(Collections.singletonList(getCompleteBoneMassRecord()));
        List<BoneMassRecord> newBoneMassRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(BoneMassRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder().setPackageName("abc").build())
                                .build());
        assertThat(newBoneMassRecords.size()).isEqualTo(0);
    }

    private void readBoneMassRecordUsingClientId(List<Record> insertedRecord)
            throws InterruptedException {
        ReadRecordsRequestUsingIds.Builder<BoneMassRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(BoneMassRecord.class);
        for (Record record : insertedRecord) {
            request.addClientRecordId(record.getMetadata().getClientRecordId());
        }
        List<BoneMassRecord> result = TestUtils.readRecords(request.build());
        result.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));
        insertedRecord.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));

        for (int i = 0; i < result.size(); i++) {
            BoneMassRecord other = (BoneMassRecord) insertedRecord.get(i);
            assertThat(result.get(i).equals(other)).isTrue();
        }
    }

    private void readBoneMassRecordUsingIds(List<Record> recordList) throws InterruptedException {
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        ReadRecordsRequestUsingIds.Builder<BoneMassRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(BoneMassRecord.class);
        for (Record record : insertedRecords) {
            request.addId(record.getMetadata().getId());
        }
        ReadRecordsRequestUsingIds requestUsingIds = request.build();
        assertThat(requestUsingIds.getRecordType()).isEqualTo(BoneMassRecord.class);
        assertThat(requestUsingIds.getRecordIdFilters()).isNotNull();
        List<BoneMassRecord> result = TestUtils.readRecords(requestUsingIds);
        assertThat(result).hasSize(insertedRecords.size());
        result.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));
        insertedRecords.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));

        for (int i = 0; i < result.size(); i++) {
            BoneMassRecord other = (BoneMassRecord) insertedRecords.get(i);
            assertThat(result.get(i).equals(other)).isTrue();
        }
    }

    @Test
    public void testDeleteBoneMassRecord_no_filters() throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompleteBoneMassRecord());
        TestUtils.verifyDeleteRecords(new DeleteUsingFiltersRequest.Builder().build());
        TestUtils.assertRecordNotFound(id, BoneMassRecord.class);
    }

    @Test
    public void testDeleteBoneMassRecord_time_filters() throws InterruptedException {
        TimeRangeFilter timeRangeFilter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(1000)).build();
        String id = TestUtils.insertRecordAndGetId(getCompleteBoneMassRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addRecordType(BoneMassRecord.class)
                        .setTimeRangeFilter(timeRangeFilter)
                        .build());
        TestUtils.assertRecordNotFound(id, BoneMassRecord.class);
    }

    @Test
    public void testDeleteBoneMassRecord_recordId_filters() throws InterruptedException {
        List<Record> records = List.of(getBaseBoneMassRecord(), getCompleteBoneMassRecord());
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
    public void testDeleteBoneMassRecord_dataOrigin_filters() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        String id = TestUtils.insertRecordAndGetId(getCompleteBoneMassRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(
                                new DataOrigin.Builder()
                                        .setPackageName(context.getPackageName())
                                        .build())
                        .build());
        TestUtils.assertRecordNotFound(id, BoneMassRecord.class);
    }

    @Test
    public void testDeleteBoneMassRecord_dataOrigin_filter_incorrect() throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompleteBoneMassRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(new DataOrigin.Builder().setPackageName("abc").build())
                        .build());
        TestUtils.assertRecordFound(id, BoneMassRecord.class);
    }

    @Test
    public void testDeleteBoneMassRecord_usingIds() throws InterruptedException {
        List<Record> records = List.of(getBaseBoneMassRecord(), getCompleteBoneMassRecord());
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
    public void testDeleteBoneMassRecord_time_range() throws InterruptedException {
        TimeRangeFilter timeRangeFilter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(1000)).build();
        String id = TestUtils.insertRecordAndGetId(getCompleteBoneMassRecord());
        TestUtils.verifyDeleteRecords(BoneMassRecord.class, timeRangeFilter);
        TestUtils.assertRecordNotFound(id, BoneMassRecord.class);
    }

    @Test
    public void testZoneOffsets() {
        final ZoneOffset defaultZoneOffset =
                ZoneOffset.systemDefault().getRules().getOffset(Instant.now());
        final ZoneOffset zoneOffset = ZoneOffset.UTC;
        BoneMassRecord.Builder builder =
                new BoneMassRecord.Builder(
                        new Metadata.Builder().build(), Instant.now(), Mass.fromKilograms(10.0));

        assertThat(builder.setZoneOffset(zoneOffset).build().getZoneOffset()).isEqualTo(zoneOffset);
        assertThat(builder.clearZoneOffset().build().getZoneOffset()).isEqualTo(defaultZoneOffset);
    }

    private static BoneMassRecord getBaseBoneMassRecord() {
        return new BoneMassRecord.Builder(
                        new Metadata.Builder().build(), Instant.now(), Mass.fromKilograms(10.0))
                .build();
    }

    private static BoneMassRecord getCompleteBoneMassRecord() {
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
        testMetadataBuilder.setClientRecordId("BMR" + Math.random());

        return new BoneMassRecord.Builder(
                        testMetadataBuilder.build(), Instant.now(), Mass.fromKilograms(10.0))
                .setZoneOffset(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()))
                .build();
    }
}
