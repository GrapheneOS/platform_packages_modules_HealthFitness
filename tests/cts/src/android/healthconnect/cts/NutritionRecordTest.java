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

import android.content.Context;
import android.healthconnect.DeleteUsingFiltersRequest;
import android.healthconnect.RecordIdFilter;
import android.healthconnect.TimeRangeFilter;
import android.healthconnect.datatypes.DataOrigin;
import android.healthconnect.datatypes.Device;
import android.healthconnect.datatypes.Metadata;
import android.healthconnect.datatypes.NutritionRecord;
import android.healthconnect.datatypes.Record;
import android.healthconnect.datatypes.units.Energy;
import android.healthconnect.datatypes.units.Mass;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class NutritionRecordTest {
    private static final String TAG = "NutritionRecordTest";

    @After
    public void tearDown() throws InterruptedException {
        TestUtils.verifyDeleteRecords(
                NutritionRecord.class,
                new TimeRangeFilter.Builder(Instant.EPOCH, Instant.now()).build());
    }

    @Test
    public void testInsertNutritionRecord() throws InterruptedException {
        List<Record> records = List.of(getBaseNutritionRecord(), getCompleteNutritionRecord());
        TestUtils.insertRecords(records);
    }

    @Test
    public void testDeleteNutritionRecord_no_filters() throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompleteNutritionRecord());
        TestUtils.verifyDeleteRecords(new DeleteUsingFiltersRequest.Builder().build());
        TestUtils.assertRecordNotFound(id, NutritionRecord.class);
    }

    @Test
    public void testDeleteNutritionRecord_time_filters() throws InterruptedException {
        TimeRangeFilter timeRangeFilter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(1000)).build();
        String id = TestUtils.insertRecordAndGetId(getCompleteNutritionRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addRecordType(NutritionRecord.class)
                        .setTimeRangeFilter(timeRangeFilter)
                        .build());
        TestUtils.assertRecordNotFound(id, NutritionRecord.class);
    }

    @Test
    public void testDeleteNutritionRecord_recordId_filters() throws InterruptedException {
        List<Record> records = List.of(getBaseNutritionRecord(), getCompleteNutritionRecord());
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
    public void testDeleteNutritionRecord_dataOrigin_filters() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        String id = TestUtils.insertRecordAndGetId(getCompleteNutritionRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(
                                new DataOrigin.Builder()
                                        .setPackageName(context.getPackageName())
                                        .build())
                        .build());
        TestUtils.assertRecordNotFound(id, NutritionRecord.class);
    }

    @Test
    public void testDeleteNutritionRecord_dataOrigin_filter_incorrect()
            throws InterruptedException {
        String id = TestUtils.insertRecordAndGetId(getCompleteNutritionRecord());
        TestUtils.verifyDeleteRecords(
                new DeleteUsingFiltersRequest.Builder()
                        .addDataOrigin(new DataOrigin.Builder().setPackageName("abc").build())
                        .build());
        TestUtils.assertRecordFound(id, NutritionRecord.class);
    }

    @Test
    public void testDeleteNutritionRecord_usingIds() throws InterruptedException {
        List<Record> records = List.of(getBaseNutritionRecord(), getCompleteNutritionRecord());
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
    public void testDeleteNutritionRecord_time_range() throws InterruptedException {
        TimeRangeFilter timeRangeFilter =
                new TimeRangeFilter.Builder(Instant.now(), Instant.now().plusMillis(1000)).build();
        String id = TestUtils.insertRecordAndGetId(getCompleteNutritionRecord());
        TestUtils.verifyDeleteRecords(NutritionRecord.class, timeRangeFilter);
        TestUtils.assertRecordNotFound(id, NutritionRecord.class);
    }

    static NutritionRecord getBaseNutritionRecord() {
        return new NutritionRecord.Builder(
                        new Metadata.Builder().build(), Instant.now(), Instant.now())
                .build();
    }

    static NutritionRecord getCompleteNutritionRecord() {
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
        testMetadataBuilder.setClientRecordId("NTR" + Math.random());

        return new NutritionRecord.Builder(
                        testMetadataBuilder.build(), Instant.now(), Instant.now())
                .setUnsaturatedFat(Mass.fromKilograms(10.0))
                .setPotassium(Mass.fromKilograms(10.0))
                .setThiamin(Mass.fromKilograms(10.0))
                .setMealType(1)
                .setTransFat(Mass.fromKilograms(10.0))
                .setManganese(Mass.fromKilograms(10.0))
                .setEnergyFromFat(Energy.fromJoules(10.0))
                .setCaffeine(Mass.fromKilograms(10.0))
                .setDietaryFiber(Mass.fromKilograms(10.0))
                .setSelenium(Mass.fromKilograms(10.0))
                .setVitaminB6(Mass.fromKilograms(10.0))
                .setProtein(Mass.fromKilograms(10.0))
                .setChloride(Mass.fromKilograms(10.0))
                .setCholesterol(Mass.fromKilograms(10.0))
                .setCopper(Mass.fromKilograms(10.0))
                .setIodine(Mass.fromKilograms(10.0))
                .setVitaminB12(Mass.fromKilograms(10.0))
                .setZinc(Mass.fromKilograms(10.0))
                .setRiboflavin(Mass.fromKilograms(10.0))
                .setEnergy(Energy.fromJoules(10.0))
                .setMolybdenum(Mass.fromKilograms(10.0))
                .setPhosphorus(Mass.fromKilograms(10.0))
                .setChromium(Mass.fromKilograms(10.0))
                .setTotalFat(Mass.fromKilograms(10.0))
                .setCalcium(Mass.fromKilograms(10.0))
                .setVitaminC(Mass.fromKilograms(10.0))
                .setVitaminE(Mass.fromKilograms(10.0))
                .setBiotin(Mass.fromKilograms(10.0))
                .setVitaminD(Mass.fromKilograms(10.0))
                .setNiacin(Mass.fromKilograms(10.0))
                .setMagnesium(Mass.fromKilograms(10.0))
                .setTotalCarbohydrate(Mass.fromKilograms(10.0))
                .setVitaminK(Mass.fromKilograms(10.0))
                .setPolyunsaturatedFat(Mass.fromKilograms(10.0))
                .setSaturatedFat(Mass.fromKilograms(10.0))
                .setSodium(Mass.fromKilograms(10.0))
                .setFolate(Mass.fromKilograms(10.0))
                .setMonounsaturatedFat(Mass.fromKilograms(10.0))
                .setPantothenicAcid(Mass.fromKilograms(10.0))
                .setMealName("Brunch")
                .setIron(Mass.fromKilograms(10.0))
                .setVitaminA(Mass.fromKilograms(10.0))
                .setFolicAcid(Mass.fromKilograms(10.0))
                .setSugar(Mass.fromKilograms(10.0))
                .setStartZoneOffset(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()))
                .setEndZoneOffset(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()))
                .build();
    }
}
