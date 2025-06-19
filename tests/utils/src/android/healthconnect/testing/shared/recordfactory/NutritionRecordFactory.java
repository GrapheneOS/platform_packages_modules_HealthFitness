/*
 * Copyright (C) 2025 The Android Open Source Project
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

package android.healthconnect.testing.shared.recordfactory;

import android.health.connect.datatypes.MealType;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.NutritionRecord;
import android.health.connect.datatypes.units.Energy;
import android.health.connect.datatypes.units.Mass;
import android.os.Bundle;

import java.time.Instant;
import java.time.ZoneOffset;

/** Note: This class is AI generated, validate before using, and remove this note */
public final class NutritionRecordFactory extends RecordFactory<NutritionRecord> {
    private static final String KEY_MEAL_TYPE = PREFIX + "MEAL_TYPE";
    private static final String KEY_UNSATURATED_FAT = PREFIX + "UNSATURATED_FAT";
    private static final String KEY_POTASSIUM = PREFIX + "POTASSIUM";
    private static final String KEY_THIAMIN = PREFIX + "THIAMIN";
    private static final String KEY_TRANS_FAT = PREFIX + "TRANS_FAT";
    private static final String KEY_MANGANESE = PREFIX + "MANGANESE";
    private static final String KEY_ENERGY_FROM_FAT = PREFIX + "ENERGY_FROM_FAT";
    private static final String KEY_CAFFEINE = PREFIX + "CAFFEINE";
    private static final String KEY_DIETARY_FIBER = PREFIX + "DIETARY_FIBER";
    private static final String KEY_SELENIUM = PREFIX + "SELENIUM";
    private static final String KEY_VITAMIN_B6 = PREFIX + "VITAMIN_B6";
    private static final String KEY_PROTEIN = PREFIX + "PROTEIN";
    private static final String KEY_CHLORIDE = PREFIX + "CHLORIDE";
    private static final String KEY_CHOLESTEROL = PREFIX + "CHOLESTEROL";
    private static final String KEY_COPPER = PREFIX + "COPPER";
    private static final String KEY_IODINE = PREFIX + "IODINE";
    private static final String KEY_VITAMIN_B12 = PREFIX + "VITAMIN_B12";
    private static final String KEY_ZINC = PREFIX + "ZINC";
    private static final String KEY_RIBOFLAVIN = PREFIX + "RIBOFLAVIN";
    private static final String KEY_ENERGY = PREFIX + "ENERGY";
    private static final String KEY_MOLYBDENUM = PREFIX + "MOLYBDENUM";
    private static final String KEY_PHOSPHORUS = PREFIX + "PHOSPHORUS";
    private static final String KEY_CHROMIUM = PREFIX + "CHROMIUM";
    private static final String KEY_TOTAL_FAT = PREFIX + "TOTAL_FAT";
    private static final String KEY_CALCIUM = PREFIX + "CALCIUM";
    private static final String KEY_VITAMIN_C = PREFIX + "VITAMIN_C";
    private static final String KEY_VITAMIN_E = PREFIX + "VITAMIN_E";
    private static final String KEY_BIOTIN = PREFIX + "BIOTIN";
    private static final String KEY_VITAMIN_D = PREFIX + "VITAMIN_D";
    private static final String KEY_NIACIN = PREFIX + "NIACIN";
    private static final String KEY_MAGNESIUM = PREFIX + "MAGNESIUM";
    private static final String KEY_TOTAL_CARBOHYDRATE = PREFIX + "TOTAL_CARBOHYDRATE";
    private static final String KEY_VITAMIN_K = PREFIX + "VITAMIN_K";
    private static final String KEY_POLYUNSATURATED_FAT = PREFIX + "POLYUNSATURATED_FAT";
    private static final String KEY_SATURATED_FAT = PREFIX + "SATURATED_FAT";
    private static final String KEY_SODIUM = PREFIX + "SODIUM";
    private static final String KEY_FOLATE = PREFIX + "FOLATE";
    private static final String KEY_MONOUNSATURATED_FAT = PREFIX + "MONOUNSATURATED_FAT";
    private static final String KEY_PANTOTHENIC_ACID = PREFIX + "PANTOTHENIC_ACID";
    private static final String KEY_MEAL_NAME = PREFIX + "MEAL_NAME";
    private static final String KEY_IRON = PREFIX + "IRON";
    private static final String KEY_VITAMIN_A = PREFIX + "VITAMIN_A";
    private static final String KEY_FOLIC_ACID = PREFIX + "FOLIC_ACID";
    private static final String KEY_SUGAR = PREFIX + "SUGAR";

    @Override
    public NutritionRecord newFullRecord(Metadata metadata, Instant startTime, Instant endTime) {
        return new NutritionRecord.Builder(metadata, startTime, endTime)
                .setMealType(MealType.MEAL_TYPE_BREAKFAST)
                .setUnsaturatedFat(Mass.fromGrams(1.0))
                .setPotassium(Mass.fromGrams(2.0))
                .setThiamin(Mass.fromGrams(3.0))
                .setTransFat(Mass.fromGrams(4.0))
                .setManganese(Mass.fromGrams(5.0))
                .setEnergyFromFat(Energy.fromCalories(6.0))
                .setCaffeine(Mass.fromGrams(7.0))
                .setDietaryFiber(Mass.fromGrams(8.0))
                .setSelenium(Mass.fromGrams(9.0))
                .setVitaminB6(Mass.fromGrams(10.0))
                .setProtein(Mass.fromGrams(11.0))
                .setChloride(Mass.fromGrams(12.0))
                .setCholesterol(Mass.fromGrams(13.0))
                .setCopper(Mass.fromGrams(14.0))
                .setIodine(Mass.fromGrams(15.0))
                .setVitaminB12(Mass.fromGrams(16.0))
                .setZinc(Mass.fromGrams(17.0))
                .setRiboflavin(Mass.fromGrams(18.0))
                .setEnergy(Energy.fromCalories(19.0))
                .setMolybdenum(Mass.fromGrams(20.0))
                .setPhosphorus(Mass.fromGrams(21.0))
                .setChromium(Mass.fromGrams(22.0))
                .setTotalFat(Mass.fromGrams(23.0))
                .setCalcium(Mass.fromGrams(24.0))
                .setVitaminC(Mass.fromGrams(25.0))
                .setVitaminE(Mass.fromGrams(26.0))
                .setBiotin(Mass.fromGrams(27.0))
                .setVitaminD(Mass.fromGrams(28.0))
                .setNiacin(Mass.fromGrams(29.0))
                .setMagnesium(Mass.fromGrams(30.0))
                .setTotalCarbohydrate(Mass.fromGrams(31.0))
                .setVitaminK(Mass.fromGrams(32.0))
                .setPolyunsaturatedFat(Mass.fromGrams(33.0))
                .setSaturatedFat(Mass.fromGrams(34.0))
                .setSodium(Mass.fromGrams(35.0))
                .setFolate(Mass.fromGrams(36.0))
                .setMonounsaturatedFat(Mass.fromGrams(37.0))
                .setPantothenicAcid(Mass.fromGrams(38.0))
                .setMealName("Meal name")
                .setIron(Mass.fromGrams(39.0))
                .setVitaminA(Mass.fromGrams(40.0))
                .setFolicAcid(Mass.fromGrams(41.0))
                .setSugar(Mass.fromGrams(42.0))
                .setStartZoneOffset(ZoneOffset.ofHours(3))
                .setEndZoneOffset(ZoneOffset.ofHours(-2))
                .build();
    }

    @Override
    public NutritionRecord anotherFullRecord(
            Metadata metadata, Instant startTime, Instant endTime) {
        return new NutritionRecord.Builder(metadata, startTime, endTime)
                .setMealType(MealType.MEAL_TYPE_DINNER)
                .setUnsaturatedFat(Mass.fromGrams(101.0))
                .setPotassium(Mass.fromGrams(102.0))
                .setThiamin(Mass.fromGrams(103.0))
                .setTransFat(Mass.fromGrams(104.0))
                .setManganese(Mass.fromGrams(105.0))
                .setEnergyFromFat(Energy.fromCalories(106.0))
                .setCaffeine(Mass.fromGrams(107.0))
                .setDietaryFiber(Mass.fromGrams(108.0))
                .setSelenium(Mass.fromGrams(109.0))
                .setVitaminB6(Mass.fromGrams(110.0))
                .setProtein(Mass.fromGrams(111.0))
                .setChloride(Mass.fromGrams(112.0))
                .setCholesterol(Mass.fromGrams(113.0))
                .setCopper(Mass.fromGrams(114.0))
                .setIodine(Mass.fromGrams(115.0))
                .setVitaminB12(Mass.fromGrams(116.0))
                .setZinc(Mass.fromGrams(117.0))
                .setRiboflavin(Mass.fromGrams(118.0))
                .setEnergy(Energy.fromCalories(119.0))
                .setMolybdenum(Mass.fromGrams(120.0))
                .setPhosphorus(Mass.fromGrams(121.0))
                .setChromium(Mass.fromGrams(122.0))
                .setTotalFat(Mass.fromGrams(123.0))
                .setCalcium(Mass.fromGrams(124.0))
                .setVitaminC(Mass.fromGrams(125.0))
                .setVitaminE(Mass.fromGrams(126.0))
                .setBiotin(Mass.fromGrams(127.0))
                .setVitaminD(Mass.fromGrams(128.0))
                .setNiacin(Mass.fromGrams(129.0))
                .setMagnesium(Mass.fromGrams(130.0))
                .setTotalCarbohydrate(Mass.fromGrams(131.0))
                .setVitaminK(Mass.fromGrams(132.0))
                .setPolyunsaturatedFat(Mass.fromGrams(133.0))
                .setSaturatedFat(Mass.fromGrams(134.0))
                .setSodium(Mass.fromGrams(135.0))
                .setFolate(Mass.fromGrams(136.0))
                .setMonounsaturatedFat(Mass.fromGrams(137.0))
                .setPantothenicAcid(Mass.fromGrams(138.0))
                .setMealName("Another meal name")
                .setIron(Mass.fromGrams(139.0))
                .setVitaminA(Mass.fromGrams(140.0))
                .setFolicAcid(Mass.fromGrams(141.0))
                .setSugar(Mass.fromGrams(142.0))
                .setStartZoneOffset(ZoneOffset.ofHours(-1))
                .setEndZoneOffset(ZoneOffset.ofHours(2))
                .build();
    }

    @Override
    public NutritionRecord newEmptyRecord(Metadata metadata, Instant startTime, Instant endTime) {
        return new NutritionRecord.Builder(metadata, startTime, endTime)
                .setMealType(MealType.MEAL_TYPE_SNACK)
                .build();
    }

    @Override
    protected NutritionRecord recordWithMetadata(NutritionRecord record, Metadata metadata) {
        return new NutritionRecord.Builder(metadata, record.getStartTime(), record.getEndTime())
                .setMealType(record.getMealType())
                .setUnsaturatedFat(record.getUnsaturatedFat())
                .setPotassium(record.getPotassium())
                .setThiamin(record.getThiamin())
                .setTransFat(record.getTransFat())
                .setManganese(record.getManganese())
                .setEnergyFromFat(record.getEnergyFromFat())
                .setCaffeine(record.getCaffeine())
                .setDietaryFiber(record.getDietaryFiber())
                .setSelenium(record.getSelenium())
                .setVitaminB6(record.getVitaminB6())
                .setProtein(record.getProtein())
                .setChloride(record.getChloride())
                .setCholesterol(record.getCholesterol())
                .setCopper(record.getCopper())
                .setIodine(record.getIodine())
                .setVitaminB12(record.getVitaminB12())
                .setZinc(record.getZinc())
                .setRiboflavin(record.getRiboflavin())
                .setEnergy(record.getEnergy())
                .setMolybdenum(record.getMolybdenum())
                .setPhosphorus(record.getPhosphorus())
                .setChromium(record.getChromium())
                .setTotalFat(record.getTotalFat())
                .setCalcium(record.getCalcium())
                .setVitaminC(record.getVitaminC())
                .setVitaminE(record.getVitaminE())
                .setBiotin(record.getBiotin())
                .setVitaminD(record.getVitaminD())
                .setNiacin(record.getNiacin())
                .setMagnesium(record.getMagnesium())
                .setTotalCarbohydrate(record.getTotalCarbohydrate())
                .setVitaminK(record.getVitaminK())
                .setPolyunsaturatedFat(record.getPolyunsaturatedFat())
                .setSaturatedFat(record.getSaturatedFat())
                .setSodium(record.getSodium())
                .setFolate(record.getFolate())
                .setMonounsaturatedFat(record.getMonounsaturatedFat())
                .setPantothenicAcid(record.getPantothenicAcid())
                .setMealName(record.getMealName())
                .setIron(record.getIron())
                .setVitaminA(record.getVitaminA())
                .setFolicAcid(record.getFolicAcid())
                .setSugar(record.getSugar())
                .setStartZoneOffset(record.getStartZoneOffset())
                .setEndZoneOffset(record.getEndZoneOffset())
                .build();
    }

    @Override
    protected Bundle getValuesBundleForRecord(NutritionRecord record) {
        Bundle values = new Bundle();
        values.putInt(KEY_MEAL_TYPE, record.getMealType());
        if (record.getUnsaturatedFat() != null) {
            values.putDouble(KEY_UNSATURATED_FAT, record.getUnsaturatedFat().getInGrams());
        }
        if (record.getPotassium() != null) {
            values.putDouble(KEY_POTASSIUM, record.getPotassium().getInGrams());
        }
        if (record.getThiamin() != null) {
            values.putDouble(KEY_THIAMIN, record.getThiamin().getInGrams());
        }
        if (record.getTransFat() != null) {
            values.putDouble(KEY_TRANS_FAT, record.getTransFat().getInGrams());
        }
        if (record.getManganese() != null) {
            values.putDouble(KEY_MANGANESE, record.getManganese().getInGrams());
        }
        if (record.getEnergyFromFat() != null) {
            values.putDouble(KEY_ENERGY_FROM_FAT, record.getEnergyFromFat().getInCalories());
        }
        if (record.getCaffeine() != null) {
            values.putDouble(KEY_CAFFEINE, record.getCaffeine().getInGrams());
        }
        if (record.getDietaryFiber() != null) {
            values.putDouble(KEY_DIETARY_FIBER, record.getDietaryFiber().getInGrams());
        }
        if (record.getSelenium() != null) {
            values.putDouble(KEY_SELENIUM, record.getSelenium().getInGrams());
        }
        if (record.getVitaminB6() != null) {
            values.putDouble(KEY_VITAMIN_B6, record.getVitaminB6().getInGrams());
        }
        if (record.getProtein() != null) {
            values.putDouble(KEY_PROTEIN, record.getProtein().getInGrams());
        }
        if (record.getChloride() != null) {
            values.putDouble(KEY_CHLORIDE, record.getChloride().getInGrams());
        }
        if (record.getCholesterol() != null) {
            values.putDouble(KEY_CHOLESTEROL, record.getCholesterol().getInGrams());
        }
        if (record.getCopper() != null) {
            values.putDouble(KEY_COPPER, record.getCopper().getInGrams());
        }
        if (record.getIodine() != null) {
            values.putDouble(KEY_IODINE, record.getIodine().getInGrams());
        }
        if (record.getVitaminB12() != null) {
            values.putDouble(KEY_VITAMIN_B12, record.getVitaminB12().getInGrams());
        }
        if (record.getZinc() != null) {
            values.putDouble(KEY_ZINC, record.getZinc().getInGrams());
        }
        if (record.getRiboflavin() != null) {
            values.putDouble(KEY_RIBOFLAVIN, record.getRiboflavin().getInGrams());
        }
        if (record.getEnergy() != null) {
            values.putDouble(KEY_ENERGY, record.getEnergy().getInCalories());
        }
        if (record.getMolybdenum() != null) {
            values.putDouble(KEY_MOLYBDENUM, record.getMolybdenum().getInGrams());
        }
        if (record.getPhosphorus() != null) {
            values.putDouble(KEY_PHOSPHORUS, record.getPhosphorus().getInGrams());
        }
        if (record.getChromium() != null) {
            values.putDouble(KEY_CHROMIUM, record.getChromium().getInGrams());
        }
        if (record.getTotalFat() != null) {
            values.putDouble(KEY_TOTAL_FAT, record.getTotalFat().getInGrams());
        }
        if (record.getCalcium() != null) {
            values.putDouble(KEY_CALCIUM, record.getCalcium().getInGrams());
        }
        if (record.getVitaminC() != null) {
            values.putDouble(KEY_VITAMIN_C, record.getVitaminC().getInGrams());
        }
        if (record.getVitaminE() != null) {
            values.putDouble(KEY_VITAMIN_E, record.getVitaminE().getInGrams());
        }
        if (record.getBiotin() != null) {
            values.putDouble(KEY_BIOTIN, record.getBiotin().getInGrams());
        }
        if (record.getVitaminD() != null) {
            values.putDouble(KEY_VITAMIN_D, record.getVitaminD().getInGrams());
        }
        if (record.getNiacin() != null) {
            values.putDouble(KEY_NIACIN, record.getNiacin().getInGrams());
        }
        if (record.getMagnesium() != null) {
            values.putDouble(KEY_MAGNESIUM, record.getMagnesium().getInGrams());
        }
        if (record.getTotalCarbohydrate() != null) {
            values.putDouble(KEY_TOTAL_CARBOHYDRATE, record.getTotalCarbohydrate().getInGrams());
        }
        if (record.getVitaminK() != null) {
            values.putDouble(KEY_VITAMIN_K, record.getVitaminK().getInGrams());
        }
        if (record.getPolyunsaturatedFat() != null) {
            values.putDouble(KEY_POLYUNSATURATED_FAT, record.getPolyunsaturatedFat().getInGrams());
        }
        if (record.getSaturatedFat() != null) {
            values.putDouble(KEY_SATURATED_FAT, record.getSaturatedFat().getInGrams());
        }
        if (record.getSodium() != null) {
            values.putDouble(KEY_SODIUM, record.getSodium().getInGrams());
        }
        if (record.getFolate() != null) {
            values.putDouble(KEY_FOLATE, record.getFolate().getInGrams());
        }
        if (record.getMonounsaturatedFat() != null) {
            values.putDouble(KEY_MONOUNSATURATED_FAT, record.getMonounsaturatedFat().getInGrams());
        }
        if (record.getPantothenicAcid() != null) {
            values.putDouble(KEY_PANTOTHENIC_ACID, record.getPantothenicAcid().getInGrams());
        }
        if (record.getMealName() != null) {
            values.putString(KEY_MEAL_NAME, record.getMealName());
        }
        if (record.getIron() != null) {
            values.putDouble(KEY_IRON, record.getIron().getInGrams());
        }
        if (record.getVitaminA() != null) {
            values.putDouble(KEY_VITAMIN_A, record.getVitaminA().getInGrams());
        }
        if (record.getFolicAcid() != null) {
            values.putDouble(KEY_FOLIC_ACID, record.getFolicAcid().getInGrams());
        }
        if (record.getSugar() != null) {
            values.putDouble(KEY_SUGAR, record.getSugar().getInGrams());
        }
        return values;
    }

    @Override
    public NutritionRecord newRecordFromValuesBundle(
            Metadata metadata,
            Instant startTime,
            Instant endTime,
            ZoneOffset startZoneOffset,
            ZoneOffset endZoneOffset,
            Bundle bundle) {
        NutritionRecord.Builder builder =
                new NutritionRecord.Builder(metadata, startTime, endTime)
                        .setMealType(bundle.getInt(KEY_MEAL_TYPE))
                        .setStartZoneOffset(startZoneOffset)
                        .setEndZoneOffset(endZoneOffset);
        if (bundle.containsKey(KEY_UNSATURATED_FAT)) {
            builder.setUnsaturatedFat(Mass.fromGrams(bundle.getDouble(KEY_UNSATURATED_FAT)));
        }
        if (bundle.containsKey(KEY_POTASSIUM)) {
            builder.setPotassium(Mass.fromGrams(bundle.getDouble(KEY_POTASSIUM)));
        }
        if (bundle.containsKey(KEY_THIAMIN)) {
            builder.setThiamin(Mass.fromGrams(bundle.getDouble(KEY_THIAMIN)));
        }
        if (bundle.containsKey(KEY_TRANS_FAT)) {
            builder.setTransFat(Mass.fromGrams(bundle.getDouble(KEY_TRANS_FAT)));
        }
        if (bundle.containsKey(KEY_MANGANESE)) {
            builder.setManganese(Mass.fromGrams(bundle.getDouble(KEY_MANGANESE)));
        }
        if (bundle.containsKey(KEY_ENERGY_FROM_FAT)) {
            builder.setEnergyFromFat(Energy.fromCalories(bundle.getDouble(KEY_ENERGY_FROM_FAT)));
        }
        if (bundle.containsKey(KEY_CAFFEINE)) {
            builder.setCaffeine(Mass.fromGrams(bundle.getDouble(KEY_CAFFEINE)));
        }
        if (bundle.containsKey(KEY_DIETARY_FIBER)) {
            builder.setDietaryFiber(Mass.fromGrams(bundle.getDouble(KEY_DIETARY_FIBER)));
        }
        if (bundle.containsKey(KEY_SELENIUM)) {
            builder.setSelenium(Mass.fromGrams(bundle.getDouble(KEY_SELENIUM)));
        }
        if (bundle.containsKey(KEY_VITAMIN_B6)) {
            builder.setVitaminB6(Mass.fromGrams(bundle.getDouble(KEY_VITAMIN_B6)));
        }
        if (bundle.containsKey(KEY_PROTEIN)) {
            builder.setProtein(Mass.fromGrams(bundle.getDouble(KEY_PROTEIN)));
        }
        if (bundle.containsKey(KEY_CHLORIDE)) {
            builder.setChloride(Mass.fromGrams(bundle.getDouble(KEY_CHLORIDE)));
        }
        if (bundle.containsKey(KEY_CHOLESTEROL)) {
            builder.setCholesterol(Mass.fromGrams(bundle.getDouble(KEY_CHOLESTEROL)));
        }
        if (bundle.containsKey(KEY_COPPER)) {
            builder.setCopper(Mass.fromGrams(bundle.getDouble(KEY_COPPER)));
        }
        if (bundle.containsKey(KEY_IODINE)) {
            builder.setIodine(Mass.fromGrams(bundle.getDouble(KEY_IODINE)));
        }
        if (bundle.containsKey(KEY_VITAMIN_B12)) {
            builder.setVitaminB12(Mass.fromGrams(bundle.getDouble(KEY_VITAMIN_B12)));
        }
        if (bundle.containsKey(KEY_ZINC)) {
            builder.setZinc(Mass.fromGrams(bundle.getDouble(KEY_ZINC)));
        }
        if (bundle.containsKey(KEY_RIBOFLAVIN)) {
            builder.setRiboflavin(Mass.fromGrams(bundle.getDouble(KEY_RIBOFLAVIN)));
        }
        if (bundle.containsKey(KEY_ENERGY)) {
            builder.setEnergy(Energy.fromCalories(bundle.getDouble(KEY_ENERGY)));
        }
        if (bundle.containsKey(KEY_MOLYBDENUM)) {
            builder.setMolybdenum(Mass.fromGrams(bundle.getDouble(KEY_MOLYBDENUM)));
        }
        if (bundle.containsKey(KEY_PHOSPHORUS)) {
            builder.setPhosphorus(Mass.fromGrams(bundle.getDouble(KEY_PHOSPHORUS)));
        }
        if (bundle.containsKey(KEY_CHROMIUM)) {
            builder.setChromium(Mass.fromGrams(bundle.getDouble(KEY_CHROMIUM)));
        }
        if (bundle.containsKey(KEY_TOTAL_FAT)) {
            builder.setTotalFat(Mass.fromGrams(bundle.getDouble(KEY_TOTAL_FAT)));
        }
        if (bundle.containsKey(KEY_CALCIUM)) {
            builder.setCalcium(Mass.fromGrams(bundle.getDouble(KEY_CALCIUM)));
        }
        if (bundle.containsKey(KEY_VITAMIN_C)) {
            builder.setVitaminC(Mass.fromGrams(bundle.getDouble(KEY_VITAMIN_C)));
        }
        if (bundle.containsKey(KEY_VITAMIN_E)) {
            builder.setVitaminE(Mass.fromGrams(bundle.getDouble(KEY_VITAMIN_E)));
        }
        if (bundle.containsKey(KEY_BIOTIN)) {
            builder.setBiotin(Mass.fromGrams(bundle.getDouble(KEY_BIOTIN)));
        }
        if (bundle.containsKey(KEY_VITAMIN_D)) {
            builder.setVitaminD(Mass.fromGrams(bundle.getDouble(KEY_VITAMIN_D)));
        }
        if (bundle.containsKey(KEY_NIACIN)) {
            builder.setNiacin(Mass.fromGrams(bundle.getDouble(KEY_NIACIN)));
        }
        if (bundle.containsKey(KEY_MAGNESIUM)) {
            builder.setMagnesium(Mass.fromGrams(bundle.getDouble(KEY_MAGNESIUM)));
        }
        if (bundle.containsKey(KEY_TOTAL_CARBOHYDRATE)) {
            builder.setTotalCarbohydrate(Mass.fromGrams(bundle.getDouble(KEY_TOTAL_CARBOHYDRATE)));
        }
        if (bundle.containsKey(KEY_VITAMIN_K)) {
            builder.setVitaminK(Mass.fromGrams(bundle.getDouble(KEY_VITAMIN_K)));
        }
        if (bundle.containsKey(KEY_POLYUNSATURATED_FAT)) {
            builder.setPolyunsaturatedFat(
                    Mass.fromGrams(bundle.getDouble(KEY_POLYUNSATURATED_FAT)));
        }
        if (bundle.containsKey(KEY_SATURATED_FAT)) {
            builder.setSaturatedFat(Mass.fromGrams(bundle.getDouble(KEY_SATURATED_FAT)));
        }
        if (bundle.containsKey(KEY_SODIUM)) {
            builder.setSodium(Mass.fromGrams(bundle.getDouble(KEY_SODIUM)));
        }
        if (bundle.containsKey(KEY_FOLATE)) {
            builder.setFolate(Mass.fromGrams(bundle.getDouble(KEY_FOLATE)));
        }
        if (bundle.containsKey(KEY_MONOUNSATURATED_FAT)) {
            builder.setMonounsaturatedFat(
                    Mass.fromGrams(bundle.getDouble(KEY_MONOUNSATURATED_FAT)));
        }
        if (bundle.containsKey(KEY_PANTOTHENIC_ACID)) {
            builder.setPantothenicAcid(Mass.fromGrams(bundle.getDouble(KEY_PANTOTHENIC_ACID)));
        }
        if (bundle.containsKey(KEY_MEAL_NAME)) {
            builder.setMealName(bundle.getString(KEY_MEAL_NAME));
        }
        if (bundle.containsKey(KEY_IRON)) {
            builder.setIron(Mass.fromGrams(bundle.getDouble(KEY_IRON)));
        }
        if (bundle.containsKey(KEY_VITAMIN_A)) {
            builder.setVitaminA(Mass.fromGrams(bundle.getDouble(KEY_VITAMIN_A)));
        }
        if (bundle.containsKey(KEY_FOLIC_ACID)) {
            builder.setFolicAcid(Mass.fromGrams(bundle.getDouble(KEY_FOLIC_ACID)));
        }
        if (bundle.containsKey(KEY_SUGAR)) {
            builder.setSugar(Mass.fromGrams(bundle.getDouble(KEY_SUGAR)));
        }
        return builder.build();
    }

    @Override
    public String recordToString(NutritionRecord record) {
        return "NutritionRecord{"
                + "\n\tstartTime = "
                + record.getStartTime()
                + ",\n\tendTime = "
                + record.getEndTime()
                + ",\n\tstartZoneOffset = "
                + record.getStartZoneOffset()
                + ",\n\tendZoneOffset = "
                + record.getEndZoneOffset()
                + ",\n\tmetadata = "
                + metadataToString(record.getMetadata())
                + ",\n\tmealType = "
                + record.getMealType()
                + ",\n\tunsaturatedFat = "
                + record.getUnsaturatedFat()
                + ",\n\tpotassium = "
                + record.getPotassium()
                + ",\n\tthiamin = "
                + record.getThiamin()
                + ",\n\ttransFat = "
                + record.getTransFat()
                + ",\n\tmanganese = "
                + record.getManganese()
                + ",\n\tenergyFromFat = "
                + record.getEnergyFromFat()
                + ",\n\tcaffeine = "
                + record.getCaffeine()
                + ",\n\tdietaryFiber = "
                + record.getDietaryFiber()
                + ",\n\tselenium = "
                + record.getSelenium()
                + ",\n\tvitaminB6 = "
                + record.getVitaminB6()
                + ",\n\tprotein = "
                + record.getProtein()
                + ",\n\tchloride = "
                + record.getChloride()
                + ",\n\tcholesterol = "
                + record.getCholesterol()
                + ",\n\tcopper = "
                + record.getCopper()
                + ",\n\tiodine = "
                + record.getIodine()
                + ",\n\tvitaminB12 = "
                + record.getVitaminB12()
                + ",\n\tzinc = "
                + record.getZinc()
                + ",\n\triboflavin = "
                + record.getRiboflavin()
                + ",\n\tenergy = "
                + record.getEnergy()
                + ",\n\tmolybdenum = "
                + record.getMolybdenum()
                + ",\n\tphosphorus = "
                + record.getPhosphorus()
                + ",\n\tchromium = "
                + record.getChromium()
                + ",\n\ttotalFat = "
                + record.getTotalFat()
                + ",\n\tcalcium = "
                + record.getCalcium()
                + ",\n\tvitaminC = "
                + record.getVitaminC()
                + ",\n\tvitaminE = "
                + record.getVitaminE()
                + ",\n\tbiotin = "
                + record.getBiotin()
                + ",\n\tvitaminD = "
                + record.getVitaminD()
                + ",\n\tniacin = "
                + record.getNiacin()
                + ",\n\tmagnesium = "
                + record.getMagnesium()
                + ",\n\ttotalCarbohydrate = "
                + record.getTotalCarbohydrate()
                + ",\n\tvitaminK = "
                + record.getVitaminK()
                + ",\n\tpolyunsaturatedFat = "
                + record.getPolyunsaturatedFat()
                + ",\n\tsaturatedFat = "
                + record.getSaturatedFat()
                + ",\n\tsodium = "
                + record.getSodium()
                + ",\n\tfolate = "
                + record.getFolate()
                + ",\n\tmonounsaturatedFat = "
                + record.getMonounsaturatedFat()
                + ",\n\tpantothenicAcid = "
                + record.getPantothenicAcid()
                + ",\n\tmealName = "
                + record.getMealName()
                + ",\n\tiron = "
                + record.getIron()
                + ",\n\tvitaminA = "
                + record.getVitaminA()
                + ",\n\tfolicAcid = "
                + record.getFolicAcid()
                + ",\n\tsugar = "
                + record.getSugar()
                + "\n}";
    }
}
