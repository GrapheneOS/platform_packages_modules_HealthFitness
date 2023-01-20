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

import static android.healthconnect.datatypes.PowerRecord.POWER_AVG;
import static android.healthconnect.datatypes.PowerRecord.POWER_MAX;
import static android.healthconnect.datatypes.PowerRecord.POWER_MIN;

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
import android.healthconnect.datatypes.PowerRecord;
import android.healthconnect.datatypes.Record;
import android.healthconnect.datatypes.units.Power;
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
public class PowerRecordTest {
    private static final String TAG = "PowerRecordTest";

    @After
    public void tearDown() throws InterruptedException {
        TestUtils.verifyDeleteRecords(
                PowerRecord.class,
                new TimeRangeFilter.Builder(Instant.EPOCH, Instant.now()).build());
    }

    @Test
    public void testInsertPowerRecord() throws InterruptedException {
        TestUtils.insertRecords(Arrays.asList(getBasePowerRecord(), getCompletePowerRecord()));
    }

    @Test
    public void testReadPowerRecord_usingIds() throws InterruptedException {
        testReadPowerRecordIds();
    }

    @Test
    public void testReadPowerRecord_invalidIds() throws InterruptedException {
        ReadRecordsRequestUsingIds<PowerRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(PowerRecord.class).addId("abc").build();
        List<PowerRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadPowerRecord_usingClientRecordIds() throws InterruptedException {
        List<Record> recordList = Arrays.asList(getCompletePowerRecord(), getCompletePowerRecord());
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        readPowerRecordUsingClientId(insertedRecords);
    }

    @Test
    public void testReadPowerRecord_invalidClientRecordIds() throws InterruptedException {
        ReadRecordsRequestUsingIds<PowerRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(PowerRecord.class)
                        .addClientRecordId("abc")
                        .build();
        List<PowerRecord> result = TestUtils.readRecords(request);
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    public void testReadPowerRecordUsingFilters_default() throws InterruptedException {
        List<PowerRecord> oldPowerRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(PowerRecord.class).build());
        PowerRecord testRecord = getCompletePowerRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<PowerRecord> newPowerRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(PowerRecord.class).build());
        assertThat(newPowerRecords.size()).isEqualTo(oldPowerRecords.size() + 1);
        assertThat(newPowerRecords.get(newPowerRecords.size() - 1).equals(testRecord)).isTrue();
    }

    @Test
    public void testReadPowerRecordUsingFilters_timeFilter() throws InterruptedException {
        TimeRangeFilter filter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(3000)).build();
        PowerRecord testRecord = getCompletePowerRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<PowerRecord> newPowerRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(PowerRecord.class)
                                .setTimeRangeFilter(filter)
                                .build());
        assertThat(newPowerRecords.get(newPowerRecords.size() - 1).equals(testRecord)).isTrue();
    }

    @Test
    public void testReadPowerRecordUsingFilters_dataFilter_correct() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        List<PowerRecord> oldPowerRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(PowerRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        PowerRecord testRecord = getCompletePowerRecord();
        TestUtils.insertRecords(Collections.singletonList(testRecord));
        List<PowerRecord> newPowerRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(PowerRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build());
        assertThat(newPowerRecords.size() - oldPowerRecords.size()).isEqualTo(1);
        assertThat(newPowerRecords.get(newPowerRecords.size() - 1).equals(testRecord)).isTrue();
    }

    @Test
    public void testReadPowerRecordUsingFilters_dataFilter_incorrect() throws InterruptedException {
        TestUtils.insertRecords(Collections.singletonList(getCompletePowerRecord()));
        List<PowerRecord> newPowerRecords =
                TestUtils.readRecords(
                        new ReadRecordsRequestUsingFilters.Builder<>(PowerRecord.class)
                                .addDataOrigins(
                                        new DataOrigin.Builder().setPackageName("abc").build())
                                .build());
        assertThat(newPowerRecords.size()).isEqualTo(0);
    }

    @Test
    public void testDeletePowerRecord_no_filters() throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompletePowerRecord());
        TestUtils.verifyDeleteRecords(new DeleteUsingFiltersRequest.Builder().build());
        TestUtils.assertRecordNotFound(id, PowerRecord.class);
    }

    @Test
    public void testDeletePowerRecord_time_filters() throws InterruptedException {
        TimeRangeFilter timeRangeFilter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(1000)).build();
        String id = TestUtils.insertRecordAndGetId(getCompletePowerRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addRecordType(PowerRecord.class)
                        .setTimeRangeFilter(timeRangeFilter)
                        .build());
        TestUtils.assertRecordNotFound(id, PowerRecord.class);
    }

    @Test
    public void testDeletePowerRecord_recordId_filters() throws InterruptedException {
        List<Record> records = List.of(getBasePowerRecord(), getCompletePowerRecord());
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
    public void testDeletePowerRecord_dataOrigin_filters() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        String id = TestUtils.insertRecordAndGetId(getCompletePowerRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(
                                new DataOrigin.Builder()
                                        .setPackageName(context.getPackageName())
                                        .build())
                        .build());
        TestUtils.assertRecordNotFound(id, PowerRecord.class);
    }

    @Test
    public void testDeletePowerRecord_dataOrigin_filter_incorrect() throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompletePowerRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(new DataOrigin.Builder().setPackageName("abc").build())
                        .build());
        TestUtils.assertRecordFound(id, PowerRecord.class);
    }

    @Test
    public void testDeletePowerRecord_usingIds() throws InterruptedException {
        List<Record> records = List.of(getBasePowerRecord(), getCompletePowerRecord());
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
    public void testDeletePowerRecord_time_range() throws InterruptedException {
        TimeRangeFilter timeRangeFilter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(1000)).build();
        String id = TestUtils.insertRecordAndGetId(getCompletePowerRecord());
        TestUtils.verifyDeleteRecords(PowerRecord.class, timeRangeFilter);
        TestUtils.assertRecordNotFound(id, PowerRecord.class);
    }

    @Test
    public void testAggregation_power() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        List<Record> records =
                Arrays.asList(getPowerRecord(5.0), getPowerRecord(10.0), getPowerRecord(15.0));
        AggregateRecordsResponse<Power> response =
                TestUtils.getAggregateResponse(
                        new AggregateRecordsRequest.Builder<Power>(
                                        new TimeRangeFilter.Builder(
                                                        Instant.ofEpochMilli(0),
                                                        Instant.now().plus(1, ChronoUnit.DAYS))
                                                .build())
                                .addAggregationType(POWER_MAX)
                                .addAggregationType(POWER_MIN)
                                .addAggregationType(POWER_AVG)
                                .addDataOriginsFilter(
                                        new DataOrigin.Builder()
                                                .setPackageName(context.getPackageName())
                                                .build())
                                .build(),
                        records);
        Power maxPower = response.get(POWER_MAX);
        Power minPower = response.get(POWER_MIN);
        Power avgPower = response.get(POWER_AVG);
        assertThat(maxPower).isNotNull();
        assertThat(maxPower.getInWatts()).isEqualTo(15.0);
        assertThat(minPower).isNotNull();
        assertThat(minPower.getInWatts()).isEqualTo(5.0);
        assertThat(avgPower).isNotNull();
        assertThat(avgPower.getInWatts()).isEqualTo(10.0);
        Set<DataOrigin> newDataOrigin = response.getDataOrigins(POWER_AVG);
        for (DataOrigin itr : newDataOrigin) {
            assertThat(itr.getPackageName()).isEqualTo("android.healthconnect.cts");
        }
    }

    @Test
    public void testZoneOffsets() {
        final ZoneOffset defaultZoneOffset =
                ZoneOffset.systemDefault().getRules().getOffset(Instant.now());
        final ZoneOffset startZoneOffset = ZoneOffset.UTC;
        final ZoneOffset endZoneOffset = ZoneOffset.MAX;
        PowerRecord.Builder builder =
                new PowerRecord.Builder(
                        new Metadata.Builder().build(),
                        Instant.now(),
                        Instant.now(),
                        Collections.emptyList());

        assertThat(builder.setStartZoneOffset(startZoneOffset).build().getStartZoneOffset())
                .isEqualTo(startZoneOffset);
        assertThat(builder.setEndZoneOffset(endZoneOffset).build().getEndZoneOffset())
                .isEqualTo(endZoneOffset);
        assertThat(builder.clearStartZoneOffset().build().getStartZoneOffset())
                .isEqualTo(defaultZoneOffset);
        assertThat(builder.clearEndZoneOffset().build().getEndZoneOffset())
                .isEqualTo(defaultZoneOffset);
    }

    private void testReadPowerRecordIds() throws InterruptedException {
        List<Record> recordList = Arrays.asList(getCompletePowerRecord(), getCompletePowerRecord());
        readPowerRecordUsingIds(recordList);
    }

    private void readPowerRecordUsingClientId(List<Record> insertedRecord)
            throws InterruptedException {
        ReadRecordsRequestUsingIds.Builder<PowerRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(PowerRecord.class);
        for (Record record : insertedRecord) {
            request.addClientRecordId(record.getMetadata().getClientRecordId());
        }
        List<PowerRecord> result = TestUtils.readRecords(request.build());
        result.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));
        insertedRecord.sort(Comparator.comparing(item -> item.getMetadata().getClientRecordId()));

        for (int i = 0; i < result.size(); i++) {
            PowerRecord other = (PowerRecord) insertedRecord.get(i);
            assertThat(result.get(i).equals(other)).isTrue();
        }
    }

    private void readPowerRecordUsingIds(List<Record> recordList) throws InterruptedException {
        List<Record> insertedRecords = TestUtils.insertRecords(recordList);
        ReadRecordsRequestUsingIds.Builder<PowerRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(PowerRecord.class);
        for (Record record : insertedRecords) {
            request.addId(record.getMetadata().getId());
        }
        ReadRecordsRequestUsingIds requestUsingIds = request.build();
        assertThat(requestUsingIds.getRecordType()).isEqualTo(PowerRecord.class);
        assertThat(requestUsingIds.getRecordIdFilters()).isNotNull();
        List<PowerRecord> result = TestUtils.readRecords(requestUsingIds);
        assertThat(result).hasSize(insertedRecords.size());
        for (int i = 0; i < result.size(); i++) {
            assertThat(result.get(i).equals(insertedRecords.get(i))).isTrue();
        }
    }

    private static PowerRecord getBasePowerRecord() {
        PowerRecord.PowerRecordSample powerRecord =
                new PowerRecord.PowerRecordSample(Power.fromWatts(10.0), Instant.now());
        ArrayList<PowerRecord.PowerRecordSample> powerRecords = new ArrayList<>();
        powerRecords.add(powerRecord);
        powerRecords.add(powerRecord);

        return new PowerRecord.Builder(
                        new Metadata.Builder().build(), Instant.now(), Instant.now(), powerRecords)
                .build();
    }

    static PowerRecord getPowerRecord(double power) {
        PowerRecord.PowerRecordSample powerRecord =
                new PowerRecord.PowerRecordSample(Power.fromWatts(power), Instant.now());
        ArrayList<PowerRecord.PowerRecordSample> powerRecords = new ArrayList<>();
        powerRecords.add(powerRecord);
        powerRecords.add(powerRecord);

        return new PowerRecord.Builder(
                        new Metadata.Builder().build(), Instant.now(), Instant.now(), powerRecords)
                .build();
    }

    private static PowerRecord getCompletePowerRecord() {

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
        testMetadataBuilder.setClientRecordId("PR" + Math.random());

        PowerRecord.PowerRecordSample powerRecord =
                new PowerRecord.PowerRecordSample(Power.fromWatts(10.0), Instant.now());

        ArrayList<PowerRecord.PowerRecordSample> powerRecords = new ArrayList<>();
        powerRecords.add(powerRecord);
        powerRecords.add(powerRecord);

        return new PowerRecord.Builder(
                        testMetadataBuilder.build(), Instant.now(), Instant.now(), powerRecords)
                .build();
    }
}
