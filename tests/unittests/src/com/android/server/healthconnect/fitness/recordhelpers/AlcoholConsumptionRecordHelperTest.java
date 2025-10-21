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

import static android.health.connect.Constants.DEFAULT_DOUBLE;
import static android.health.connect.datatypes.AlcoholConsumptionRecord.ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_BEER;
import static android.health.connect.datatypes.AlcoholConsumptionRecord.ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_OTHER;
import static android.health.connect.datatypes.AlcoholConsumptionRecord.ALCOHOL_CONSUMPTION_SERVING_SIZE_PINT;
import static android.health.connect.datatypes.AlcoholConsumptionRecord.RECORD_TEMPORAL_TYPE_INTERVAL;
import static android.healthconnect.testing.unittest.RecordInternalFactory.buildAlcoholConsumptionRecordInternal;

import static com.android.server.healthconnect.fitness.recordhelpers.AlcoholConsumptionRecordHelper.ALCOHOL_BY_VOLUME_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.AlcoholConsumptionRecordHelper.ALCOHOL_CONSUMPTION_RECORD_TABLE_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.AlcoholConsumptionRecordHelper.BEVERAGE_TYPE_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.AlcoholConsumptionRecordHelper.NOTE_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.AlcoholConsumptionRecordHelper.SERVING_COUNT_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.AlcoholConsumptionRecordHelper.SERVING_SIZE_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.AlcoholConsumptionRecordHelper.SERVING_VOLUME_LITERS_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.AlcoholConsumptionRecordHelper.TEMPORAL_TYPE_COLUMN_NAME;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.health.connect.internal.datatypes.AlcoholConsumptionRecordInternal;
import android.healthconnect.testing.shared.AssumptionCheckerRule;
import android.healthconnect.testing.shared.DeviceSupportUtils;
import android.healthconnect.testing.unittest.FitnessTestUtils;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.healthfitness.flags.Flags;
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
@RequiresFlagsEnabled({Flags.FLAG_ALCOHOL_CONSUMPTION, Flags.FLAG_ALCOHOL_CONSUMPTION_DB})
public class AlcoholConsumptionRecordHelperTest {
    private static final String TEST_PACKAGE_NAME = "package.name";
    @Rule public final TemporaryFolder mEnvironmentDataDir = new TemporaryFolder();
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Rule
    public final AssumptionCheckerRule mSupportedHardwareRule =
            new AssumptionCheckerRule(
                    DeviceSupportUtils::isHealthConnectFullySupported,
                    "Tests should run on supported hardware only.");

    private TransactionManager mTransactionManager;
    private FitnessTestUtils mFitnessTestUtils;
    private AlcoholConsumptionRecordHelper mAlcoholConsumptionRecordHelper;

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
        mAlcoholConsumptionRecordHelper = new AlcoholConsumptionRecordHelper();
        mFitnessTestUtils.insertApp(TEST_PACKAGE_NAME);
    }

    @Test
    public void populateSpecificContentValues_allValues() {
        ContentValues contentValues = new ContentValues();
        AlcoholConsumptionRecordInternal record =
                buildAlcoholConsumptionRecordInternal(
                        500,
                        1000,
                        RECORD_TEMPORAL_TYPE_INTERVAL,
                        /* servingCount= */ 2,
                        ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_BEER,
                        ALCOHOL_CONSUMPTION_SERVING_SIZE_PINT,
                        /* servingVolume= */ 0.5,
                        /* alcoholByVolume= */ 5.0,
                        "note");
        mAlcoholConsumptionRecordHelper.populateSpecificContentValues(contentValues, record);

        assertThat(contentValues.getAsInteger(TEMPORAL_TYPE_COLUMN_NAME))
                .isEqualTo(RECORD_TEMPORAL_TYPE_INTERVAL);
        assertThat(contentValues.getAsInteger(SERVING_COUNT_COLUMN_NAME))
                .isEqualTo(record.getServingCount());
        assertThat(contentValues.getAsInteger(BEVERAGE_TYPE_COLUMN_NAME))
                .isEqualTo(record.getBeverageType());
        assertThat(contentValues.getAsInteger(SERVING_SIZE_COLUMN_NAME))
                .isEqualTo(record.getServingSize());
        assertThat(contentValues.getAsDouble(SERVING_VOLUME_LITERS_COLUMN_NAME))
                .isEqualTo(record.getServingVolumeLiters());
        assertThat(contentValues.getAsDouble(ALCOHOL_BY_VOLUME_COLUMN_NAME))
                .isEqualTo(record.getAlcoholByVolume());
        assertThat(contentValues.getAsString(NOTE_COLUMN_NAME)).isEqualTo(record.getNote());
    }

    @Test
    public void populateSpecificRecordValue_allValues() {
        AlcoholConsumptionRecordInternal insertedRecord =
                buildAlcoholConsumptionRecordInternal(
                        500,
                        1000,
                        RECORD_TEMPORAL_TYPE_INTERVAL,
                        /* servingCount= */ 2,
                        ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_BEER,
                        ALCOHOL_CONSUMPTION_SERVING_SIZE_PINT,
                        /* servingVolume= */ 0.5,
                        /* alcoholByVolume= */ 5.0,
                        "note");
        mFitnessTestUtils.insertRecords(TEST_PACKAGE_NAME, insertedRecord);
        ReadTableRequest request = new ReadTableRequest(ALCOHOL_CONSUMPTION_RECORD_TABLE_NAME);
        try (Cursor cursor = mTransactionManager.read(request)) {
            assertThat(cursor.moveToNext()).isTrue();
            AlcoholConsumptionRecordInternal readRecord =
                    mAlcoholConsumptionRecordHelper.populateSpecificRecordValue(cursor);
            assertThat(readRecord.getTemporalType()).isEqualTo(insertedRecord.getTemporalType());
            assertThat(readRecord.getServingCount()).isEqualTo(insertedRecord.getServingCount());
            assertThat(readRecord.getBeverageType()).isEqualTo(insertedRecord.getBeverageType());
            assertThat(readRecord.getServingSize()).isEqualTo(insertedRecord.getServingSize());
            assertThat(readRecord.getServingVolumeLiters())
                    .isEqualTo(insertedRecord.getServingVolumeLiters());
            assertThat(readRecord.getAlcoholByVolume())
                    .isEqualTo(insertedRecord.getAlcoholByVolume());
            assertThat(readRecord.getNote()).isEqualTo(insertedRecord.getNote());
        }
    }

    @Test
    public void populate_optionalFieldsNotSet() {
        AlcoholConsumptionRecordInternal insertedRecord =
                buildMinimalAlcoholConsumptionRecordInternal(500, 1000);
        mFitnessTestUtils.insertRecords(TEST_PACKAGE_NAME, insertedRecord);
        ReadTableRequest request = new ReadTableRequest(ALCOHOL_CONSUMPTION_RECORD_TABLE_NAME);
        try (Cursor cursor = mTransactionManager.read(request)) {
            assertThat(cursor.moveToNext()).isTrue();
            AlcoholConsumptionRecordInternal readRecord =
                    mAlcoholConsumptionRecordHelper.populateSpecificRecordValue(cursor);
            assertThat(readRecord.getServingCount()).isEqualTo(insertedRecord.getServingCount());
            assertThat(readRecord.getBeverageType()).isEqualTo(insertedRecord.getBeverageType());
            assertThat(readRecord.getServingSize()).isEqualTo(0);
            assertThat(readRecord.getServingVolumeLiters()).isEqualTo(DEFAULT_DOUBLE);
            assertThat(readRecord.getAlcoholByVolume()).isEqualTo(DEFAULT_DOUBLE);
            assertThat(readRecord.getNote()).isNull();
        }
    }

    private AlcoholConsumptionRecordInternal buildMinimalAlcoholConsumptionRecordInternal(
            long startTimeMillis, long endTimeMillis) {
        return (AlcoholConsumptionRecordInternal)
                new AlcoholConsumptionRecordInternal()
                        .setServingCount(1)
                        .setBeverageType(ALCOHOL_CONSUMPTION_BEVERAGE_TYPE_OTHER)
                        .setStartTime(startTimeMillis)
                        .setEndTime(endTimeMillis);
    }
}
