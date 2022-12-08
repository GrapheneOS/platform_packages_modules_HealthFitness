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
import android.healthconnect.HealthConnectException;
import android.healthconnect.HealthConnectManager;
import android.healthconnect.InsertRecordsResponse;
import android.healthconnect.datatypes.Metadata;
import android.healthconnect.datatypes.NutritionRecord;
import android.healthconnect.datatypes.Record;
import android.healthconnect.datatypes.units.Energy;
import android.healthconnect.datatypes.units.Mass;
import android.os.OutcomeReceiver;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class NutritionRecordTest {
    private static final String TAG = "NutritionRecordTest";

    static NutritionRecord getBaseNutritionRecord() {
        return new NutritionRecord.Builder(
                        new Metadata.Builder().build(), Instant.now(), Instant.now())
                .build();
    }

    static NutritionRecord getPartialNutritionRecord() {
        return new NutritionRecord.Builder(
                        new Metadata.Builder().build(), Instant.now(), Instant.now())
                .setCalcium(Mass.fromKilograms(10))
                .setChloride(Mass.fromKilograms(20))
                .build();
    }

    static NutritionRecord getCompleteNutritionRecord() {
        return new NutritionRecord.Builder(
                        new Metadata.Builder().build(), Instant.now(), Instant.now())
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
                .setMealName("")
                .setIron(Mass.fromKilograms(10.0))
                .setVitaminA(Mass.fromKilograms(10.0))
                .setFolicAcid(Mass.fromKilograms(10.0))
                .setSugar(Mass.fromKilograms(10.0))
                .setStartZoneOffset(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()))
                .setEndZoneOffset(ZoneOffset.systemDefault().getRules().getOffset(Instant.now()))
                .build();
    }

    @Test
    public void testInsertNutritionRecord() throws InterruptedException {
        List<Record> records = new ArrayList<>();
        records.add(getBaseNutritionRecord());
        records.add(getCompleteNutritionRecord());
        Context context = ApplicationProvider.getApplicationContext();
        CountDownLatch latch = new CountDownLatch(1);
        HealthConnectManager service = context.getSystemService(HealthConnectManager.class);
        assertThat(service).isNotNull();
        AtomicReference<List<Record>> response = new AtomicReference<>();
        service.insertRecords(
                records,
                Executors.newSingleThreadExecutor(),
                new OutcomeReceiver<>() {
                    @Override
                    public void onResult(InsertRecordsResponse result) {
                        response.set(result.getRecords());
                        latch.countDown();
                    }

                    @Override
                    public void onError(HealthConnectException exception) {
                        Log.e(TAG, exception.getMessage());
                    }
                });
        assertThat(latch.await(3, TimeUnit.SECONDS)).isEqualTo(true);
        assertThat(response.get()).hasSize(records.size());
    }
}
