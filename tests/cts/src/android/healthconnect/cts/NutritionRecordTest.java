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

import static android.healthconnect.datatypes.NutritionRecord.BIOTIN_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.CAFFEINE_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.CALCIUM_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.CHLORIDE_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.CHOLESTEROL_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.CHROMIUM_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.COPPER_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.DIETARY_FIBER_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.ENERGY_FROM_FAT_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.ENERGY_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.FOLATE_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.FOLIC_ACID_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.IODINE_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.IRON_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.MAGNESIUM_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.MANGANESE_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.MOLYBDENUM_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.MONOUNSATURATED_FAT_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.NIACIN_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.PANTOTHENIC_ACID_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.PHOSPHORUS_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.POLYUNSATURATED_FAT_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.POTASSIUM_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.PROTEIN_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.RIBOFLAVIN_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.SATURATED_FAT_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.SELENIUM_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.SODIUM_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.SUGAR_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.THIAMIN_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.TOTAL_CARBOHYDRATE_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.TOTAL_FAT_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.UNSATURATED_FAT_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.VITAMIN_A_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.VITAMIN_B12_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.VITAMIN_B6_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.VITAMIN_C_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.VITAMIN_D_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.VITAMIN_E_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.VITAMIN_K_TOTAL;
import static android.healthconnect.datatypes.NutritionRecord.ZINC_TOTAL;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.healthconnect.AggregateRecordsRequest;
import android.healthconnect.AggregateRecordsResponse;
import android.healthconnect.DeleteUsingFiltersRequest;
import android.healthconnect.RecordIdFilter;
import android.healthconnect.TimeRangeFilter;
import android.healthconnect.datatypes.AggregationType;
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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
public class NutritionRecordTest {
    private static final String TAG = "NutritionRecordTest";
    private List<AggregationType<Mass>> mMassAggregateTypesList =
            Arrays.asList(
                    BIOTIN_TOTAL,
                    CAFFEINE_TOTAL,
                    CALCIUM_TOTAL,
                    CHLORIDE_TOTAL,
                    CHOLESTEROL_TOTAL,
                    CHROMIUM_TOTAL,
                    COPPER_TOTAL,
                    DIETARY_FIBER_TOTAL,
                    FOLATE_TOTAL,
                    FOLIC_ACID_TOTAL,
                    IODINE_TOTAL,
                    IRON_TOTAL,
                    MAGNESIUM_TOTAL,
                    MANGANESE_TOTAL,
                    MOLYBDENUM_TOTAL,
                    MONOUNSATURATED_FAT_TOTAL,
                    NIACIN_TOTAL,
                    PANTOTHENIC_ACID_TOTAL,
                    PHOSPHORUS_TOTAL,
                    POLYUNSATURATED_FAT_TOTAL,
                    POTASSIUM_TOTAL,
                    PROTEIN_TOTAL,
                    RIBOFLAVIN_TOTAL,
                    SATURATED_FAT_TOTAL,
                    SELENIUM_TOTAL,
                    SODIUM_TOTAL,
                    SUGAR_TOTAL,
                    THIAMIN_TOTAL,
                    TOTAL_CARBOHYDRATE_TOTAL,
                    TOTAL_FAT_TOTAL,
                    UNSATURATED_FAT_TOTAL,
                    VITAMIN_A_TOTAL,
                    VITAMIN_B12_TOTAL,
                    VITAMIN_B6_TOTAL,
                    VITAMIN_C_TOTAL,
                    VITAMIN_D_TOTAL,
                    VITAMIN_E_TOTAL,
                    VITAMIN_K_TOTAL,
                    ZINC_TOTAL);

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
            recordIds.add(RecordIdFilter.fromId(record.getClass(), record.getMetadata().getId()));
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

    @Test
    public void testAggregation_NutritionValuesTotal() throws Exception {
        List<Record> records =
                Arrays.asList(getCompleteNutritionRecord(), getCompleteNutritionRecord());
        AggregateRecordsResponse<Mass> oldResponse =
                TestUtils.getAggregateResponse(
                        new AggregateRecordsRequest.Builder<Mass>(
                                        new TimeRangeFilter.Builder(
                                                        Instant.ofEpochMilli(0),
                                                        Instant.now().plus(1, ChronoUnit.DAYS))
                                                .build())
                                .addAggregationType(BIOTIN_TOTAL)
                                .addAggregationType(CAFFEINE_TOTAL)
                                .addAggregationType(CALCIUM_TOTAL)
                                .addAggregationType(CHLORIDE_TOTAL)
                                .addAggregationType(CHOLESTEROL_TOTAL)
                                .addAggregationType(CHROMIUM_TOTAL)
                                .addAggregationType(COPPER_TOTAL)
                                .addAggregationType(DIETARY_FIBER_TOTAL)
                                .addAggregationType(FOLATE_TOTAL)
                                .addAggregationType(FOLIC_ACID_TOTAL)
                                .addAggregationType(IODINE_TOTAL)
                                .addAggregationType(IRON_TOTAL)
                                .addAggregationType(MAGNESIUM_TOTAL)
                                .addAggregationType(MANGANESE_TOTAL)
                                .addAggregationType(MOLYBDENUM_TOTAL)
                                .addAggregationType(MONOUNSATURATED_FAT_TOTAL)
                                .addAggregationType(NIACIN_TOTAL)
                                .addAggregationType(PANTOTHENIC_ACID_TOTAL)
                                .addAggregationType(PHOSPHORUS_TOTAL)
                                .addAggregationType(POTASSIUM_TOTAL)
                                .addAggregationType(POLYUNSATURATED_FAT_TOTAL)
                                .addAggregationType(PROTEIN_TOTAL)
                                .addAggregationType(RIBOFLAVIN_TOTAL)
                                .addAggregationType(SATURATED_FAT_TOTAL)
                                .addAggregationType(SELENIUM_TOTAL)
                                .addAggregationType(SODIUM_TOTAL)
                                .addAggregationType(SUGAR_TOTAL)
                                .addAggregationType(THIAMIN_TOTAL)
                                .addAggregationType(TOTAL_CARBOHYDRATE_TOTAL)
                                .addAggregationType(TOTAL_FAT_TOTAL)
                                .addAggregationType(UNSATURATED_FAT_TOTAL)
                                .addAggregationType(VITAMIN_A_TOTAL)
                                .addAggregationType(VITAMIN_B12_TOTAL)
                                .addAggregationType(VITAMIN_B6_TOTAL)
                                .addAggregationType(VITAMIN_C_TOTAL)
                                .addAggregationType(VITAMIN_D_TOTAL)
                                .addAggregationType(VITAMIN_E_TOTAL)
                                .addAggregationType(VITAMIN_K_TOTAL)
                                .addAggregationType(ZINC_TOTAL)
                                .build(),
                        records);
        List<Record> recordNew =
                Arrays.asList(getCompleteNutritionRecord(), getCompleteNutritionRecord());
        AggregateRecordsResponse<Mass> newResponse =
                TestUtils.getAggregateResponse(
                        new AggregateRecordsRequest.Builder<Mass>(
                                        new TimeRangeFilter.Builder(
                                                        Instant.ofEpochMilli(0),
                                                        Instant.now().plus(1, ChronoUnit.DAYS))
                                                .build())
                                .addAggregationType(BIOTIN_TOTAL)
                                .addAggregationType(CAFFEINE_TOTAL)
                                .addAggregationType(CALCIUM_TOTAL)
                                .addAggregationType(CHLORIDE_TOTAL)
                                .addAggregationType(CHOLESTEROL_TOTAL)
                                .addAggregationType(CHROMIUM_TOTAL)
                                .addAggregationType(COPPER_TOTAL)
                                .addAggregationType(DIETARY_FIBER_TOTAL)
                                .addAggregationType(FOLATE_TOTAL)
                                .addAggregationType(FOLIC_ACID_TOTAL)
                                .addAggregationType(IODINE_TOTAL)
                                .addAggregationType(IRON_TOTAL)
                                .addAggregationType(MAGNESIUM_TOTAL)
                                .addAggregationType(MANGANESE_TOTAL)
                                .addAggregationType(MOLYBDENUM_TOTAL)
                                .addAggregationType(MONOUNSATURATED_FAT_TOTAL)
                                .addAggregationType(NIACIN_TOTAL)
                                .addAggregationType(PANTOTHENIC_ACID_TOTAL)
                                .addAggregationType(PHOSPHORUS_TOTAL)
                                .addAggregationType(POTASSIUM_TOTAL)
                                .addAggregationType(POLYUNSATURATED_FAT_TOTAL)
                                .addAggregationType(PROTEIN_TOTAL)
                                .addAggregationType(RIBOFLAVIN_TOTAL)
                                .addAggregationType(SATURATED_FAT_TOTAL)
                                .addAggregationType(SELENIUM_TOTAL)
                                .addAggregationType(SODIUM_TOTAL)
                                .addAggregationType(SUGAR_TOTAL)
                                .addAggregationType(THIAMIN_TOTAL)
                                .addAggregationType(TOTAL_CARBOHYDRATE_TOTAL)
                                .addAggregationType(TOTAL_FAT_TOTAL)
                                .addAggregationType(UNSATURATED_FAT_TOTAL)
                                .addAggregationType(VITAMIN_A_TOTAL)
                                .addAggregationType(VITAMIN_B12_TOTAL)
                                .addAggregationType(VITAMIN_B6_TOTAL)
                                .addAggregationType(VITAMIN_C_TOTAL)
                                .addAggregationType(VITAMIN_D_TOTAL)
                                .addAggregationType(VITAMIN_E_TOTAL)
                                .addAggregationType(VITAMIN_K_TOTAL)
                                .addAggregationType(ZINC_TOTAL)
                                .build(),
                        recordNew);
        for (AggregationType<Mass> type : mMassAggregateTypesList) {
            Mass newTotal = newResponse.get(type);
            Mass oldTotal = oldResponse.get(type);
            assertThat(newTotal).isNotNull();
            assertThat(oldTotal).isNotNull();
            assertThat(newTotal.getInKilograms() - oldTotal.getInKilograms()).isEqualTo(20);
            Set<DataOrigin> newDataOrigin = newResponse.getDataOrigins(type);
            for (DataOrigin itr : newDataOrigin) {
                assertThat(itr.getPackageName()).isEqualTo("android.healthconnect.cts");
            }
            Set<DataOrigin> oldDataOrigin = oldResponse.getDataOrigins(type);
            for (DataOrigin itr : oldDataOrigin) {
                assertThat(itr.getPackageName()).isEqualTo("android.healthconnect.cts");
            }
        }
    }

    @Test
    public void testAggregation_NutritionEnergyValuesTotal() throws Exception {
        List<Record> records = Arrays.asList(getCompleteNutritionRecord());
        AggregateRecordsResponse<Energy> oldResponse =
                TestUtils.getAggregateResponse(
                        new AggregateRecordsRequest.Builder<Energy>(
                                        new TimeRangeFilter.Builder(
                                                        Instant.ofEpochMilli(0),
                                                        Instant.now().plus(1, ChronoUnit.DAYS))
                                                .build())
                                .addAggregationType(ENERGY_TOTAL)
                                .addAggregationType(ENERGY_FROM_FAT_TOTAL)
                                .build(),
                        records);
        List<Record> newRecords = Arrays.asList(getCompleteNutritionRecord());
        AggregateRecordsResponse<Energy> newResponse =
                TestUtils.getAggregateResponse(
                        new AggregateRecordsRequest.Builder<Energy>(
                                        new TimeRangeFilter.Builder(
                                                        Instant.ofEpochMilli(0),
                                                        Instant.now().plus(1, ChronoUnit.DAYS))
                                                .build())
                                .addAggregationType(ENERGY_TOTAL)
                                .addAggregationType(ENERGY_FROM_FAT_TOTAL)
                                .build(),
                        newRecords);
        Energy newEnergy = newResponse.get(ENERGY_TOTAL);
        Energy oldEnergy = oldResponse.get(ENERGY_TOTAL);
        Energy newFatEnergy = newResponse.get(ENERGY_FROM_FAT_TOTAL);
        Energy oldFatEnergy = oldResponse.get(ENERGY_FROM_FAT_TOTAL);
        assertThat(newEnergy).isNotNull();
        assertThat(oldEnergy).isNotNull();
        assertThat(newFatEnergy).isNotNull();
        assertThat(oldFatEnergy).isNotNull();
        assertThat(newEnergy.getInJoules() - oldEnergy.getInJoules()).isEqualTo(10);
        assertThat(newFatEnergy.getInJoules() - oldFatEnergy.getInJoules()).isEqualTo(10);
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

    @Test
    public void testZoneOffsets() {
        final ZoneOffset defaultZoneOffset =
                ZoneOffset.systemDefault().getRules().getOffset(Instant.now());
        final ZoneOffset startZoneOffset = ZoneOffset.UTC;
        final ZoneOffset endZoneOffset = ZoneOffset.MAX;
        NutritionRecord.Builder builder =
                new NutritionRecord.Builder(
                        new Metadata.Builder().build(), Instant.now(), Instant.now());

        assertThat(builder.setStartZoneOffset(startZoneOffset).build().getStartZoneOffset())
                .isEqualTo(startZoneOffset);
        assertThat(builder.setEndZoneOffset(endZoneOffset).build().getEndZoneOffset())
                .isEqualTo(endZoneOffset);
        assertThat(builder.clearStartZoneOffset().build().getStartZoneOffset())
                .isEqualTo(defaultZoneOffset);
        assertThat(builder.clearEndZoneOffset().build().getEndZoneOffset())
                .isEqualTo(defaultZoneOffset);
    }
}
