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

import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_STEPS;
import static android.healthconnect.cts.utils.DataFactory.NOW;

import static com.android.healthfitness.flags.DatabaseVersions.LAST_ROLLED_OUT_DB_VERSION;
import static com.android.healthfitness.flags.Flags.FLAG_ACTIVITY_INTENSITY_DB;
import static com.android.healthfitness.flags.Flags.FLAG_CLOUD_BACKUP_AND_RESTORE_DB;
import static com.android.healthfitness.flags.Flags.FLAG_DEVELOPMENT_DATABASE;
import static com.android.healthfitness.flags.Flags.FLAG_INFRA_TO_GUARD_DB_CHANGES;
import static com.android.healthfitness.flags.Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE;
import static com.android.server.healthconnect.storage.DatabaseTestUtils.NUM_OF_TABLES;
import static com.android.server.healthconnect.storage.DatabaseTestUtils.assertNumberOfTables;
import static com.android.server.healthconnect.storage.utils.StorageUtils.checkTableExists;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.health.connect.datatypes.MedicalDataSource;
import android.health.connect.datatypes.StepsRecord;
import android.health.connect.internal.datatypes.RecordInternal;
import android.healthconnect.cts.phr.utils.PhrDataFactory;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.healthfitness.flags.AconfigFlagHelper;
import com.android.healthfitness.flags.Flags;
import com.android.modules.utils.testing.ExtendedMockitoRule;
import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;
import com.android.server.healthconnect.logging.ExportImportLogger;
import com.android.server.healthconnect.permission.FirstGrantTimeManager;
import com.android.server.healthconnect.permission.HealthPermissionIntentAppsTracker;
import com.android.server.healthconnect.storage.datatypehelpers.MedicalDataSourceHelper;
import com.android.server.healthconnect.storage.datatypehelpers.MedicalResourceHelper;
import com.android.server.healthconnect.storage.datatypehelpers.MedicalResourceIndicesHelper;
import com.android.server.healthconnect.testing.fixtures.EnvironmentFixture;
import com.android.server.healthconnect.testing.storage.TransactionTestUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.quality.Strictness;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RunWith(AndroidJUnit4.class)
public class HealthConnectDatabaseTest {
    private static final String TEST_PACKAGE_NAME = "package.test";

    private Context mContext;

    @Rule(order = 0)
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Rule(order = 1)
    public final ExtendedMockitoRule mExtendedMockitoRule =
            new ExtendedMockitoRule.Builder(this)
                    .mockStatic(ExportImportLogger.class)
                    .setStrictness(Strictness.LENIENT)
                    .addStaticMockFixtures(EnvironmentFixture::new)
                    .build();

    @Before
    public void setup() {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
    }

    @Test
    @DisableFlags({
        FLAG_DEVELOPMENT_DATABASE,
        FLAG_PERSONAL_HEALTH_RECORD_DATABASE,
        FLAG_ACTIVITY_INTENSITY_DB,
        Flags.FLAG_ECOSYSTEM_METRICS_DB_CHANGES,
        FLAG_CLOUD_BACKUP_AND_RESTORE_DB,
    })
    public void onCreate_dbWithLatestSchemaCreated() {
        SQLiteDatabase sqliteDatabase =
                initializeEmptyHealthConnectDatabase().getWritableDatabase();

        assertThat(sqliteDatabase).isNotNull();
        assertNumberOfTables(sqliteDatabase, NUM_OF_TABLES);
        assertThat(sqliteDatabase.getVersion()).isEqualTo(LAST_ROLLED_OUT_DB_VERSION);
    }

    @Test
    @DisableFlags(FLAG_INFRA_TO_GUARD_DB_CHANGES)
    public void onCreate_infraFlagDisabled_expectCorrectDbVersion() {
        SQLiteDatabase sqliteDatabase =
                initializeEmptyHealthConnectDatabase().getWritableDatabase();

        assertThat(sqliteDatabase.getVersion()).isAtMost(AconfigFlagHelper.getDbVersion());
    }

    @Test
    @EnableFlags(FLAG_INFRA_TO_GUARD_DB_CHANGES)
    public void onCreate_infraFlagEnabled_expectCorrectDbVersion() {
        SQLiteDatabase sqliteDatabase =
                initializeEmptyHealthConnectDatabase().getWritableDatabase();

        assertThat(sqliteDatabase.getVersion()).isAtMost(AconfigFlagHelper.getDbVersion());
    }

    @Test
    public void upgradeToPhrWithExistingHcData_expectExistingDataIntact() {
        // Disable the flag with `disableFlags()` so it can be enabled later in this test. That's
        // not allowed if FLAG_PERSONAL_HEALTH_RECORD_DATABASE is added to @DisableFlags.
        mSetFlagsRule.disableFlags(FLAG_PERSONAL_HEALTH_RECORD_DATABASE);
        HealthConnectInjector injector = getHealthConnectInjector(mContext);
        TransactionManager transactionManager = injector.getTransactionManager();
        TransactionTestUtils transactionTestUtils = new TransactionTestUtils(injector);
        // insert a StepsRecord with TEST_PACKAGE_NAME
        transactionTestUtils.insertApp(TEST_PACKAGE_NAME);
        RecordInternal<StepsRecord> originalStepsRecordInternal =
                TransactionTestUtils.createStepsRecord(
                        NOW.toEpochMilli(), NOW.plusMillis(1000).toEpochMilli(), 2);
        List<UUID> originalStepsRecordUuids =
                transactionTestUtils
                        .insertRecords(TEST_PACKAGE_NAME, originalStepsRecordInternal)
                        .stream()
                        .map(UUID::fromString)
                        .toList();
        assertPhrTablesNotExist(transactionManager);

        // Enable the flag and re-initialize all dependencies including TransactionManager.
        // When a new TransactionManager is created, it will recreate HealthConnectDatabase. Then
        // When a transaction is executed on that database for the first time, the new value of the
        // flag will be taken into account.
        mSetFlagsRule.enableFlags(FLAG_PERSONAL_HEALTH_RECORD_DATABASE);
        injector = getHealthConnectInjector(mContext);
        transactionManager = injector.getTransactionManager();
        transactionTestUtils = new TransactionTestUtils(injector);

        assertPhrTablesExist(transactionManager);
        // read the StepsRecord and assert that it's intact
        List<RecordInternal<?>> recordInternals =
                transactionManager.readRecordsByIds(
                        transactionTestUtils.getReadTransactionRequest(
                                TEST_PACKAGE_NAME,
                                Map.of(RECORD_TYPE_STEPS, originalStepsRecordUuids)),
                        injector.getAppInfoHelper(),
                        injector.getDeviceInfoHelper(),
                        injector.getAccessLogsHelper(),
                        injector.getReadAccessLogsHelper(),
                        false);
        assertThat(recordInternals).hasSize(1);
        assertThat(recordInternals.get(0).toExternalRecord())
                .isEqualTo(originalStepsRecordInternal.toExternalRecord());
    }

    @Test
    public void upgradeToPhrWithExistingHcData_expectPhrFunctionsWorkProperly() {
        // Disable the flag with `disableFlags()` so it can be enabled later in this test. That's
        // not allowed if FLAG_PERSONAL_HEALTH_RECORD_DATABASE is added to @DisableFlags.
        mSetFlagsRule.disableFlags(FLAG_PERSONAL_HEALTH_RECORD_DATABASE);
        HealthConnectInjector injector = getHealthConnectInjector(mContext);
        TransactionManager transactionManager = injector.getTransactionManager();
        TransactionTestUtils transactionTestUtils = new TransactionTestUtils(injector);
        // insert a StepsRecord with TEST_PACKAGE_NAME
        transactionTestUtils.insertApp(TEST_PACKAGE_NAME);
        RecordInternal<StepsRecord> originalStepsRecordInternal =
                TransactionTestUtils.createStepsRecord(
                        NOW.toEpochMilli(), NOW.plusMillis(1000).toEpochMilli(), 2);
        transactionTestUtils.insertRecords(TEST_PACKAGE_NAME, originalStepsRecordInternal);
        assertPhrTablesNotExist(transactionManager);

        // Enable the flag and re-initialize all dependencies including TransactionManager.
        // When a new TransactionManager is created, it will recreate HealthConnectDatabase. Then
        // When a transaction is executed on that database for the first time, the new value of the
        // flag will be taken into account.
        mSetFlagsRule.enableFlags(FLAG_PERSONAL_HEALTH_RECORD_DATABASE);
        injector = getHealthConnectInjector(mContext);
        transactionManager = injector.getTransactionManager();

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
                        HealthConnectContext.create(mContext, mContext.getUser()));

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

    private static void assertPhrTablesNotExist(TransactionManager transactionManager) {
        transactionManager.runAsTransaction(
                db -> {
                    assertThat(checkTableExists(db, MedicalDataSourceHelper.getMainTableName()))
                            .isFalse();
                    assertThat(checkTableExists(db, MedicalResourceHelper.getMainTableName()))
                            .isFalse();
                    assertThat(checkTableExists(db, MedicalResourceIndicesHelper.getTableName()))
                            .isFalse();
                });
    }

    private static HealthConnectInjector getHealthConnectInjector(Context context) {
        return HealthConnectInjectorImpl.newBuilderForTest(context)
                .setHealthPermissionIntentAppsTracker(mock(HealthPermissionIntentAppsTracker.class))
                .setFirstGrantTimeManager(mock(FirstGrantTimeManager.class))
                .build();
    }
}
