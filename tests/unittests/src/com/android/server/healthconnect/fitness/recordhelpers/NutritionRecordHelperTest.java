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

package com.android.server.healthconnect.fitness.recordhelpers;

import static android.healthconnect.testing.unittest.RecordInternalFactory.buildNutritionRecordInternal;

import static com.android.server.healthconnect.fitness.recordhelpers.NutritionRecordHelper.BIOTIN_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.NutritionRecordHelper.CAFFEINE_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.NutritionRecordHelper.CALCIUM_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.NutritionRecordHelper.CHLORIDE_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.NutritionRecordHelper.CHOLESTEROL_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.NutritionRecordHelper.CHROMIUM_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.NutritionRecordHelper.COPPER_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.NutritionRecordHelper.DIETARY_FIBER_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.NutritionRecordHelper.ENERGY_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.NutritionRecordHelper.ENERGY_FROM_FAT_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.NutritionRecordHelper.FOLATE_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.NutritionRecordHelper.FOLIC_ACID_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.NutritionRecordHelper.IODINE_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.NutritionRecordHelper.IRON_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.NutritionRecordHelper.MAGNESIUM_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.NutritionRecordHelper.MANGANESE_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.NutritionRecordHelper.MOLYBDENUM_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.NutritionRecordHelper.MONOUNSATURATED_FAT_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.NutritionRecordHelper.NIACIN_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.NutritionRecordHelper.NUTRITION_RECORD_TABLE_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.NutritionRecordHelper.PANTOTHENIC_ACID_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.NutritionRecordHelper.PHOSPHORUS_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.NutritionRecordHelper.POLYUNSATURATED_FAT_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.NutritionRecordHelper.POTASSIUM_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.NutritionRecordHelper.PROTEIN_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.NutritionRecordHelper.RIBOFLAVIN_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.NutritionRecordHelper.SATURATED_FAT_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.NutritionRecordHelper.SELENIUM_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.NutritionRecordHelper.SODIUM_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.NutritionRecordHelper.SUGAR_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.NutritionRecordHelper.THIAMIN_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.NutritionRecordHelper.TOTAL_CARBOHYDRATE_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.NutritionRecordHelper.TOTAL_FAT_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.NutritionRecordHelper.TRANS_FAT_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.NutritionRecordHelper.UNSATURATED_FAT_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.NutritionRecordHelper.VITAMIN_A_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.NutritionRecordHelper.VITAMIN_B12_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.NutritionRecordHelper.VITAMIN_B6_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.NutritionRecordHelper.VITAMIN_C_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.NutritionRecordHelper.VITAMIN_D_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.NutritionRecordHelper.VITAMIN_E_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.NutritionRecordHelper.VITAMIN_K_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.NutritionRecordHelper.ZINC_COLUMN_NAME;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.health.connect.internal.datatypes.NutritionRecordInternal;
import android.healthconnect.testing.shared.AssumptionCheckerRule;
import android.healthconnect.testing.shared.DeviceSupportUtils;
import android.healthconnect.testing.unittest.FitnessTestUtils;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;
import com.android.server.healthconnect.permission.FirstGrantTimeManager;
import com.android.server.healthconnect.permission.HealthPermissionIntentAppsTracker;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.request.ReadTableRequest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
public class NutritionRecordHelperTest {
    private static final String TEST_PACKAGE_NAME = "package.name";
    @Rule public final TemporaryFolder mEnvironmentDataDir = new TemporaryFolder();
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Rule
    public final AssumptionCheckerRule mSupportedHardwareRule =
            new AssumptionCheckerRule(
                    DeviceSupportUtils::isHealthConnectFullySupported,
                    "Tests should run on supported hardware only.");

    private TransactionManager mTransactionManager;
    private FitnessTestUtils mFitnessTestUtils;

    @Before
    public void setup() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectInjector healthConnectInjector =
                HealthConnectInjectorImpl.newBuilderForTest(context)
                        .setFirstGrantTimeManager(mock(FirstGrantTimeManager.class))
                        .setHealthPermissionIntentAppsTracker(
                                mock(HealthPermissionIntentAppsTracker.class))
                        .setEnvironmentDataDirectory(mEnvironmentDataDir.getRoot())
                        .build();

        mTransactionManager = healthConnectInjector.getTransactionManager();
        mFitnessTestUtils = new FitnessTestUtils(healthConnectInjector);
        mFitnessTestUtils.insertApp(TEST_PACKAGE_NAME);
    }

    @Test
    public void populateSpecificContentValues_nullValues() {
        ContentValues contentValues = new ContentValues();
        NutritionRecordHelper helper = new NutritionRecordHelper();
        helper.populateSpecificContentValues(
                contentValues, buildNutritionRecordInternal(500, 1000));

        assertThat(contentValues.get(UNSATURATED_FAT_COLUMN_NAME)).isNull();
        assertThat(contentValues.get(POTASSIUM_COLUMN_NAME)).isNull();
        assertThat(contentValues.get(THIAMIN_COLUMN_NAME)).isNull();
        assertThat(contentValues.get(TRANS_FAT_COLUMN_NAME)).isNull();
        assertThat(contentValues.get(MANGANESE_COLUMN_NAME)).isNull();
        assertThat(contentValues.get(ENERGY_FROM_FAT_COLUMN_NAME)).isNull();
        assertThat(contentValues.get(CAFFEINE_COLUMN_NAME)).isNull();
        assertThat(contentValues.get(DIETARY_FIBER_COLUMN_NAME)).isNull();
        assertThat(contentValues.get(SELENIUM_COLUMN_NAME)).isNull();
        assertThat(contentValues.get(VITAMIN_B6_COLUMN_NAME)).isNull();
        assertThat(contentValues.get(PROTEIN_COLUMN_NAME)).isNull();
        assertThat(contentValues.get(CHLORIDE_COLUMN_NAME)).isNull();
        assertThat(contentValues.get(CHOLESTEROL_COLUMN_NAME)).isNull();
        assertThat(contentValues.get(COPPER_COLUMN_NAME)).isNull();
        assertThat(contentValues.get(IODINE_COLUMN_NAME)).isNull();
        assertThat(contentValues.get(VITAMIN_B12_COLUMN_NAME)).isNull();
        assertThat(contentValues.get(ZINC_COLUMN_NAME)).isNull();
        assertThat(contentValues.get(RIBOFLAVIN_COLUMN_NAME)).isNull();
        assertThat(contentValues.get(ENERGY_COLUMN_NAME)).isNull();
        assertThat(contentValues.get(MOLYBDENUM_COLUMN_NAME)).isNull();
        assertThat(contentValues.get(PHOSPHORUS_COLUMN_NAME)).isNull();
        assertThat(contentValues.get(CHROMIUM_COLUMN_NAME)).isNull();
        assertThat(contentValues.get(TOTAL_FAT_COLUMN_NAME)).isNull();
        assertThat(contentValues.get(CALCIUM_COLUMN_NAME)).isNull();
        assertThat(contentValues.get(VITAMIN_C_COLUMN_NAME)).isNull();
        assertThat(contentValues.get(VITAMIN_E_COLUMN_NAME)).isNull();
        assertThat(contentValues.get(BIOTIN_COLUMN_NAME)).isNull();
        assertThat(contentValues.get(VITAMIN_D_COLUMN_NAME)).isNull();
        assertThat(contentValues.get(NIACIN_COLUMN_NAME)).isNull();
        assertThat(contentValues.get(MAGNESIUM_COLUMN_NAME)).isNull();
        assertThat(contentValues.get(TOTAL_CARBOHYDRATE_COLUMN_NAME)).isNull();
        assertThat(contentValues.get(VITAMIN_K_COLUMN_NAME)).isNull();
        assertThat(contentValues.get(POLYUNSATURATED_FAT_COLUMN_NAME)).isNull();
        assertThat(contentValues.get(SATURATED_FAT_COLUMN_NAME)).isNull();
        assertThat(contentValues.get(SODIUM_COLUMN_NAME)).isNull();
        assertThat(contentValues.get(FOLATE_COLUMN_NAME)).isNull();
        assertThat(contentValues.get(MONOUNSATURATED_FAT_COLUMN_NAME)).isNull();
        assertThat(contentValues.get(PANTOTHENIC_ACID_COLUMN_NAME)).isNull();
        assertThat(contentValues.get(IRON_COLUMN_NAME)).isNull();
        assertThat(contentValues.get(VITAMIN_A_COLUMN_NAME)).isNull();
        assertThat(contentValues.get(FOLIC_ACID_COLUMN_NAME)).isNull();
        assertThat(contentValues.get(SUGAR_COLUMN_NAME)).isNull();
    }

    @Test
    public void populateSpecificRecordValue_nullValues() {
        mFitnessTestUtils.insertRecords(TEST_PACKAGE_NAME, buildNutritionRecordInternal(500, 1000));
        ReadTableRequest request = new ReadTableRequest(NUTRITION_RECORD_TABLE_NAME);
        NutritionRecordHelper helper = new NutritionRecordHelper();
        try (Cursor cursor = mTransactionManager.read(request)) {
            cursor.moveToNext();
            NutritionRecordInternal nutritionRecord = helper.populateSpecificRecordValue(cursor);
            assertThat(nutritionRecord.getUnsaturatedFat()).isNull();
            assertThat(nutritionRecord.getPotassium()).isNull();
            assertThat(nutritionRecord.getThiamin()).isNull();
            assertThat(nutritionRecord.getTransFat()).isNull();
            assertThat(nutritionRecord.getManganese()).isNull();
            assertThat(nutritionRecord.getEnergyFromFat()).isNull();
            assertThat(nutritionRecord.getCaffeine()).isNull();
            assertThat(nutritionRecord.getDietaryFiber()).isNull();
            assertThat(nutritionRecord.getSelenium()).isNull();
            assertThat(nutritionRecord.getVitaminB6()).isNull();
            assertThat(nutritionRecord.getProtein()).isNull();
            assertThat(nutritionRecord.getChloride()).isNull();
            assertThat(nutritionRecord.getCholesterol()).isNull();
            assertThat(nutritionRecord.getCopper()).isNull();
            assertThat(nutritionRecord.getIodine()).isNull();
            assertThat(nutritionRecord.getVitaminB12()).isNull();
            assertThat(nutritionRecord.getZinc()).isNull();
            assertThat(nutritionRecord.getRiboflavin()).isNull();
            assertThat(nutritionRecord.getEnergy()).isNull();
            assertThat(nutritionRecord.getMolybdenum()).isNull();
            assertThat(nutritionRecord.getPhosphorus()).isNull();
            assertThat(nutritionRecord.getChromium()).isNull();
            assertThat(nutritionRecord.getTotalFat()).isNull();
            assertThat(nutritionRecord.getCalcium()).isNull();
            assertThat(nutritionRecord.getVitaminC()).isNull();
            assertThat(nutritionRecord.getVitaminE()).isNull();
            assertThat(nutritionRecord.getBiotin()).isNull();
            assertThat(nutritionRecord.getVitaminD()).isNull();
            assertThat(nutritionRecord.getNiacin()).isNull();
            assertThat(nutritionRecord.getMagnesium()).isNull();
            assertThat(nutritionRecord.getTotalCarbohydrate()).isNull();
            assertThat(nutritionRecord.getVitaminK()).isNull();
            assertThat(nutritionRecord.getPolyunsaturatedFat()).isNull();
            assertThat(nutritionRecord.getSaturatedFat()).isNull();
            assertThat(nutritionRecord.getSodium()).isNull();
            assertThat(nutritionRecord.getFolate()).isNull();
            assertThat(nutritionRecord.getMonounsaturatedFat()).isNull();
            assertThat(nutritionRecord.getPantothenicAcid()).isNull();
            assertThat(nutritionRecord.getIron()).isNull();
            assertThat(nutritionRecord.getVitaminA()).isNull();
            assertThat(nutritionRecord.getFolicAcid()).isNull();
            assertThat(nutritionRecord.getSugar()).isNull();
        }
    }
}
