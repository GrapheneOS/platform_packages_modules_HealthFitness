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
import android.os.Bundle;

import java.time.Instant;
import java.time.ZoneOffset;

/** Note: This class is AI generated, validate before using, and remove this note */
public final class NutritionRecordFactory extends RecordFactory<NutritionRecord> {
    private static final String KEY_MEAL_TYPE = PREFIX + "MEAL_TYPE";

    @Override
    public NutritionRecord newFullRecord(Metadata metadata, Instant startTime, Instant endTime) {
        return new NutritionRecord.Builder(metadata, startTime, endTime)
                .setMealType(MealType.MEAL_TYPE_BREAKFAST)
                .setEnergy(Energy.fromCalories(100.0))
                .setStartZoneOffset(ZoneOffset.ofHours(3))
                .setEndZoneOffset(ZoneOffset.ofHours(-2))
                .build();
    }

    @Override
    public NutritionRecord anotherFullRecord(
            Metadata metadata, Instant startTime, Instant endTime) {
        return new NutritionRecord.Builder(metadata, startTime, endTime)
                .setMealType(MealType.MEAL_TYPE_DINNER)
                .setEnergy(Energy.fromCalories(200.0))
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
        return new NutritionRecord.Builder(metadata, startTime, endTime)
                .setMealType(bundle.getInt(KEY_MEAL_TYPE))
                .setStartZoneOffset(startZoneOffset)
                .setEndZoneOffset(endZoneOffset)
                .build();
    }
}
