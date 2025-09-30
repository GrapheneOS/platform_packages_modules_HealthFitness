/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.server.healthconnect.storage;

import static android.healthconnect.testing.unittest.StorageUtils.assertNumberOfTables;

import static com.android.healthfitness.flags.DatabaseVersions.LAST_ROLLED_OUT_DB_VERSION;
import static com.android.healthfitness.flags.Flags.FLAG_ACTIVITY_INTENSITY_DB;
import static com.android.healthfitness.flags.Flags.FLAG_CLOUD_BACKUP_AND_RESTORE_DB;
import static com.android.healthfitness.flags.Flags.FLAG_DEVELOPMENT_DATABASE;
import static com.android.healthfitness.flags.Flags.FLAG_SMOKING_DB;
import static com.android.healthfitness.flags.Flags.FLAG_SYMPTOMS_DB;
import static com.android.healthfitness.flags.Flags.FLAG_WRITE_AHEAD_LOGGING_DB;
import static com.android.server.healthconnect.storage.utils.StorageUtils.checkTableExists;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.health.connect.datatypes.MedicalDataSource;
import android.healthconnect.testing.shared.phr.PhrDataFactory;
import android.healthconnect.testing.unittest.FitnessTestUtils;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.healthfitness.flags.AconfigFlagHelper;
import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;
import com.android.server.healthconnect.permission.FirstGrantTimeManager;
import com.android.server.healthconnect.permission.HealthPermissionIntentAppsTracker;
import com.android.server.healthconnect.phr.storage.MedicalDataSourceHelper;
import com.android.server.healthconnect.phr.storage.MedicalResourceHelper;
import com.android.server.healthconnect.phr.storage.MedicalResourceIndicesHelper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.util.List;
import java.util.UUID;

@RunWith(AndroidJUnit4.class)
public class HealthConnectDatabaseTest {
    // The number of table we released to the public. This number can only increase, as we are not
    // allowed to make changes that remove tables or columns.
    // Development tables that haven't reached prod are excluded.
    static final int NUM_OF_TABLES = 67;
    private static final String TEST_PACKAGE_NAME = "package.test";

    private Context mContext;

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final TemporaryFolder mEnvironmentDataDirectory = new TemporaryFolder();

    @Before
    public void setup() {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
    }

    @Test
    @DisableFlags({
        FLAG_DEVELOPMENT_DATABASE,
        FLAG_ACTIVITY_INTENSITY_DB,
        FLAG_CLOUD_BACKUP_AND_RESTORE_DB,
        FLAG_SMOKING_DB,
        FLAG_SYMPTOMS_DB
    })
    public void onCreate_dbWithLatestSchemaCreated() {
        SQLiteDatabase sqliteDatabase =
                initializeEmptyHealthConnectDatabase().getWritableDatabase();

        assertThat(sqliteDatabase).isNotNull();
        assertNumberOfTables(sqliteDatabase, NUM_OF_TABLES);
        assertThat(sqliteDatabase.getVersion()).isEqualTo(LAST_ROLLED_OUT_DB_VERSION);
    }

    @Test
    public void onCreate_infraFlagEnabled_expectCorrectDbVersion() {
        SQLiteDatabase sqliteDatabase =
                initializeEmptyHealthConnectDatabase().getWritableDatabase();

        assertThat(sqliteDatabase.getVersion()).isAtMost(AconfigFlagHelper.getDbVersion());
    }

    @Test
    @EnableFlags(FLAG_WRITE_AHEAD_LOGGING_DB)
    public void onCreate_writeAheadLoggingFlagEnabled_expectWriteAheadLoggingEnabled() {
        SQLiteDatabase sqliteDatabase =
                initializeEmptyHealthConnectDatabase().getWritableDatabase();

        assertThat(sqliteDatabase.isWriteAheadLoggingEnabled()).isTrue();
    }

    @Test
    @DisableFlags(FLAG_WRITE_AHEAD_LOGGING_DB)
    public void onCreate_writeAheadLoggingFlagDisabled_expectWriteAheadLoggingDisabled() {
        SQLiteDatabase sqliteDatabase =
                initializeEmptyHealthConnectDatabase().getWritableDatabase();

        assertThat(sqliteDatabase.isWriteAheadLoggingEnabled()).isFalse();
    }

    @Test
    public void assertPhrDatabaseWorkingAsExpected() {
        HealthConnectInjector injector = getHealthConnectInjector(mContext);
        TransactionManager transactionManager = injector.getTransactionManager();
        FitnessTestUtils fitnessTestUtils = new FitnessTestUtils(injector);
        fitnessTestUtils.insertApp(TEST_PACKAGE_NAME);

        assertPhrTablesExist(transactionManager);
        // PHR functions should work properly.
        MedicalDataSourceHelper medicalDataSourceHelper = injector.getMedicalDataSourceHelper();
        MedicalDataSource originalMedicalDataSource =
                medicalDataSourceHelper.createMedicalDataSource(
                        PhrDataFactory.getCreateMedicalDataSourceRequest(), TEST_PACKAGE_NAME);
        List<MedicalDataSource> readMedicalDataSources =
                medicalDataSourceHelper.getMedicalDataSourcesByIdsWithoutPermissionChecks(
                        List.of(UUID.fromString(originalMedicalDataSource.getId())));
        assertThat(readMedicalDataSources).hasSize(1);
        assertThat(originalMedicalDataSource).isEqualTo(readMedicalDataSources.get(0));
    }

    // The database needs to be initialized after the flags have been set by the annotations,
    // hence this methods needs to be called in individual tests rather than in @Before method.
    private HealthConnectDatabase initializeEmptyHealthConnectDatabase() {
        HealthConnectDatabase healthConnectDatabase =
                new HealthConnectDatabase(
                        HealthConnectContext.create(
                                mContext,
                                mContext.getUser(),
                                /* databaseDirName= */ null,
                                mEnvironmentDataDirectory.getRoot()));

        // Make sure there is nothing there already.
        File databasePath = healthConnectDatabase.getDatabasePath();
        if (databasePath.exists()) {
            checkState(databasePath.delete());
        }

        return healthConnectDatabase;
    }

    private static void assertPhrTablesExist(TransactionManager transactionManager) {
        transactionManager.runAsTransaction(
                db -> {
                    assertThat(checkTableExists(db, MedicalDataSourceHelper.getMainTableName()))
                            .isTrue();
                    assertThat(checkTableExists(db, MedicalResourceHelper.getMainTableName()))
                            .isTrue();
                    assertThat(checkTableExists(db, MedicalResourceIndicesHelper.getTableName()))
                            .isTrue();
                });
    }

    private HealthConnectInjector getHealthConnectInjector(Context context) {
        return HealthConnectInjectorImpl.newBuilderForTest(context)
                .setHealthPermissionIntentAppsTracker(mock(HealthPermissionIntentAppsTracker.class))
                .setFirstGrantTimeManager(mock(FirstGrantTimeManager.class))
                .setEnvironmentDataDirectory(mEnvironmentDataDirectory.getRoot())
                .build();
    }
}
