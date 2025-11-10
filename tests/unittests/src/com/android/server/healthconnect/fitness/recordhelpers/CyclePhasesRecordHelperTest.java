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

import static android.health.connect.Constants.DEFAULT_INT;
import static android.health.connect.datatypes.CyclePhasesRecord.PHASE_FOLLICULAR;
import static android.health.connect.datatypes.CyclePhasesRecord.PHASE_LUTEAL;

import static com.android.server.healthconnect.fitness.recordhelpers.CyclePhasesRecordHelper.DAY_OF_CYCLE_COLUMN_NAME;
import static com.android.server.healthconnect.fitness.recordhelpers.CyclePhasesRecordHelper.PHASE_COLUMN_NAME;
import static com.android.server.healthconnect.storage.utils.StorageUtils.INTEGER;
import static com.android.server.healthconnect.storage.utils.StorageUtils.INTEGER_NOT_NULL;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.mock;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.health.connect.internal.datatypes.CyclePhasesRecordInternal;
import android.healthconnect.testing.shared.AssumptionCheckerRule;
import android.healthconnect.testing.shared.DeviceSupportUtils;
import android.healthconnect.testing.unittest.FitnessTestUtils;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.util.Pair;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.healthfitness.flags.AconfigFlagHelper;
import com.android.healthfitness.flags.Flags;
import com.android.server.healthconnect.fitness.mappings.InternalHealthConnectMappings;
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
public class CyclePhasesRecordHelperTest {
    @Rule public final SetFlagsRule mSetFlagRule = new SetFlagsRule();
    @Rule public final TemporaryFolder mEnvironmentDataDir = new TemporaryFolder();
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Rule
    public final AssumptionCheckerRule mSupportedHardwareRule =
            new AssumptionCheckerRule(
                    DeviceSupportUtils::isHealthConnectFullySupported,
                    "Tests should run on supported hardware only.");

    private static final String TEST_PACKAGE_NAME = "package.name";

    private final CyclePhasesRecordHelper mCyclePhasesRecordHelper = new CyclePhasesRecordHelper();
    private TransactionManager mTransactionManager;
    private FitnessTestUtils mFitnessTestUtils;

    @Before
    public void setup() throws Exception {
        Context context = ApplicationProvider.getApplicationContext();
        InternalHealthConnectMappings.resetInstanceForTesting();
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
    public void getMainTableName_returnsTableName() {
        assertThat(new CyclePhasesRecordHelper().getMainTableName())
                .isEqualTo(CyclePhasesRecordHelper.TABLE_NAME);
    }

    @Test
    public void getInstantRecordColumnInfo_returnsColumns() {
        assertThat(new CyclePhasesRecordHelper().getInstantRecordColumnInfo())
                .containsExactly(
                        new Pair<>(PHASE_COLUMN_NAME, INTEGER_NOT_NULL),
                        new Pair<>(DAY_OF_CYCLE_COLUMN_NAME, INTEGER));
    }

    @Test
    public void populateSpecificContentValues_contentValuesUpdated() {
        ContentValues contentValues = new ContentValues();
        CyclePhasesRecordInternal recordInternal =
                new CyclePhasesRecordInternal().setPhase(PHASE_LUTEAL).setDayOfCycle(1);
        mCyclePhasesRecordHelper.populateSpecificContentValues(contentValues, recordInternal);

        assertThat(contentValues.getAsInteger(PHASE_COLUMN_NAME)).isEqualTo(PHASE_LUTEAL);
        assertThat(contentValues.getAsInteger(DAY_OF_CYCLE_COLUMN_NAME)).isEqualTo(1);
    }

    @Test
    public void populateSpecificContentValues_optionalValueNull_notInContentValue() {
        ContentValues contentValues = new ContentValues();
        CyclePhasesRecordInternal recordInternal =
                new CyclePhasesRecordInternal().setPhase(PHASE_FOLLICULAR);
        mCyclePhasesRecordHelper.populateSpecificContentValues(contentValues, recordInternal);

        assertThat(contentValues.getAsInteger(PHASE_COLUMN_NAME)).isEqualTo(PHASE_FOLLICULAR);
        assertThat(contentValues.containsKey(DAY_OF_CYCLE_COLUMN_NAME)).isFalse();
    }

    @Test
    @EnableFlags({
        Flags.FLAG_CYCLE_PHASES_FLAG,
        Flags.FLAG_CYCLE_PHASES_DB,
        Flags.FLAG_SMOKING_DB,
        Flags.FLAG_SYMPTOMS_DB,
        Flags.FLAG_ALCOHOL_CONSUMPTION_DB
    })
    public void populateSpecificRecordValue_returnsRecordInternal() {
        assumeTrue(
                "Skipping tests because cycle phases is disabled",
                AconfigFlagHelper.isCyclePhasesEnabled());
        CyclePhasesRecordInternal insertedRecord =
                new CyclePhasesRecordInternal().setPhase(PHASE_FOLLICULAR).setDayOfCycle(5);
        mFitnessTestUtils.insertRecords(TEST_PACKAGE_NAME, insertedRecord);

        ReadTableRequest request = new ReadTableRequest(CyclePhasesRecordHelper.TABLE_NAME);
        try (Cursor cursor = mTransactionManager.read(request)) {
            assertThat(cursor.moveToNext()).isTrue();
            CyclePhasesRecordInternal readRecord =
                    mCyclePhasesRecordHelper.populateSpecificRecordValue(cursor);

            assertThat(readRecord.getPhase()).isEqualTo(PHASE_FOLLICULAR);
            assertThat(readRecord.getDayOfCycle()).isEqualTo(5);
        }
    }

    @Test
    @EnableFlags({
        Flags.FLAG_CYCLE_PHASES_FLAG,
        Flags.FLAG_CYCLE_PHASES_DB,
        Flags.FLAG_SMOKING_DB,
        Flags.FLAG_SYMPTOMS_DB,
        Flags.FLAG_ALCOHOL_CONSUMPTION_DB
    })
    public void populateSpecificRecordValue_optionalValueNotSet_containsDefaultValue() {
        assumeTrue(
                "Skipping tests because cycle phases is disabled",
                AconfigFlagHelper.isCyclePhasesEnabled());
        CyclePhasesRecordInternal insertedRecord =
                new CyclePhasesRecordInternal().setPhase(PHASE_LUTEAL);
        mFitnessTestUtils.insertRecords(TEST_PACKAGE_NAME, insertedRecord);

        ReadTableRequest request = new ReadTableRequest(CyclePhasesRecordHelper.TABLE_NAME);
        try (Cursor cursor = mTransactionManager.read(request)) {
            assertThat(cursor.moveToNext()).isTrue();
            CyclePhasesRecordInternal readRecord =
                    mCyclePhasesRecordHelper.populateSpecificRecordValue(cursor);

            assertThat(readRecord.getPhase()).isEqualTo(PHASE_LUTEAL);
            assertThat(readRecord.getDayOfCycle()).isEqualTo(DEFAULT_INT);
        }
    }
}
