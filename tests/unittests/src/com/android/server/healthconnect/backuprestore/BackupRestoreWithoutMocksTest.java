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

package com.android.server.healthconnect.backuprestore;

import static android.healthconnect.testing.unittest.RecordInternalFactory.buildStepsRecord;
import static android.healthconnect.testing.unittest.StorageUtils.queryNumEntries;

import static com.android.healthfitness.flags.AconfigFlagHelper.isDeviceDataProvidersEnabled;
import static com.android.server.healthconnect.backuprestore.BackupRestore.GRANT_TIME_FILE_NAME;
import static com.android.server.healthconnect.backuprestore.BackupRestore.STAGED_DATABASE_DIR;
import static com.android.server.healthconnect.backuprestore.BackupRestore.STAGED_DATABASE_NAME;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.health.connect.datatypes.MedicalDataSource;
import android.health.connect.datatypes.MedicalResource;
import android.health.connect.restore.StageRemoteDataRequest;
import android.healthconnect.testing.shared.phr.PhrDataFactory;
import android.healthconnect.testing.unittest.FitnessTestUtils;
import android.healthconnect.testing.unittest.PhrTestUtils;
import android.healthconnect.testing.unittest.StorageUtils;
import android.healthconnect.testing.unittest.fakes.FakePreferenceHelper;
import android.os.ParcelFileDescriptor;
import android.platform.test.flag.junit.SetFlagsRule;
import android.util.ArrayMap;
import android.util.Pair;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.server.healthconnect.device.DeviceDataProviderManager;
import com.android.server.healthconnect.device.FakeSerialDeviceDataProviderManager;
import com.android.server.healthconnect.fitness.mappings.InternalHealthConnectMappings;
import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;
import com.android.server.healthconnect.permission.FirstGrantTimeManager;
import com.android.server.healthconnect.permission.GrantTimeXmlHelper;
import com.android.server.healthconnect.permission.HealthPermissionIntentAppsTracker;
import com.android.server.healthconnect.permission.UserGrantTimeState;
import com.android.server.healthconnect.storage.HealthConnectContext;
import com.android.server.healthconnect.storage.HealthConnectDatabase;
import com.android.server.healthconnect.storage.TransactionManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
public class BackupRestoreWithoutMocksTest {
    private static final String TEST_PACKAGE_NAME = "package.name";
    private static final String DATA_SOURCE_SUFFIX = "ds1";
    private static final Instant INSTANT_NOW = Instant.now();
    private static final Instant INSTANT_NOW_PLUS_TEN_SEC = INSTANT_NOW.plusSeconds(10);
    private static final Instant INSTANT_NOW_PLUS_TWENTY_SEC = INSTANT_NOW.plusSeconds(20);

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Rule public final TemporaryFolder mEnvironmentDataDirectory = new TemporaryFolder();
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    private Context mContext;
    private StorageUtils mStorageUtils;
    private FitnessTestUtils mFitnessTestUtils;
    private BackupRestore mBackupRestore;
    private PhrTestUtils mPhrTestUtils;
    private GrantTimeXmlHelper mGrantTimeXmlHelper;

    @Mock private FirstGrantTimeManager mFirstGrantTimeManager;
    @Mock private HealthPermissionIntentAppsTracker mPermissionIntentAppsTracker;

    @Before
    public void setUp() throws Exception {
        mContext = ApplicationProvider.getApplicationContext();
        HealthConnectInjector healthConnectInjectorTemp =
                HealthConnectInjectorImpl.newBuilderForTest(mContext)
                        .setPreferenceHelper(new FakePreferenceHelper())
                        .setHealthPermissionIntentAppsTracker(mPermissionIntentAppsTracker)
                        .setFirstGrantTimeManager(mFirstGrantTimeManager)
                        .setEnvironmentDataDirectory(mEnvironmentDataDirectory.getRoot())
                        .build();

        DeviceDataProviderManager fakeDeviceDataProviderManager =
                new FakeSerialDeviceDataProviderManager(
                        mContext,
                        healthConnectInjectorTemp.getDeviceInfoHelper(),
                        healthConnectInjectorTemp.getAppInfoHelper(),
                        healthConnectInjectorTemp.getDeviceDataSourceHelper(),
                        healthConnectInjectorTemp.getDeviceDataSourcesHelper(),
                        healthConnectInjectorTemp.getDeviceDataProviderMetadataHelper(),
                        healthConnectInjectorTemp.getFitnessRecordUpsertHelper(),
                        healthConnectInjectorTemp.getFitnessRecordReadHelper(),
                        healthConnectInjectorTemp.getFitnessRecordDeleteHelper(),
                        healthConnectInjectorTemp.getSyntheticPackageNameCreator(),
                        healthConnectInjectorTemp.getPreferenceHelper(),
                        healthConnectInjectorTemp.getHealthDataCategoryPriorityHelper(),
                        InternalHealthConnectMappings.getInstance(),
                        true);

        HealthConnectInjector healthConnectInjector =
                HealthConnectInjectorImpl.newBuilderForTest(mContext)
                        .setPreferenceHelper(new FakePreferenceHelper())
                        .setHealthPermissionIntentAppsTracker(mPermissionIntentAppsTracker)
                        .setFirstGrantTimeManager(mFirstGrantTimeManager)
                        .setEnvironmentDataDirectory(mEnvironmentDataDirectory.getRoot())
                        .setDeviceDataProviderManager(fakeDeviceDataProviderManager)
                        .build();

        if (isDeviceDataProvidersEnabled()) {
            healthConnectInjector
                    .getDeviceDataProviderManager()
                    .initializeOrRefreshCurrentDeviceIds();
        }

        mStorageUtils = new StorageUtils(healthConnectInjector);
        mFitnessTestUtils = new FitnessTestUtils(healthConnectInjector);
        mFitnessTestUtils.insertApp(TEST_PACKAGE_NAME);

        TransactionManager transactionManager = healthConnectInjector.getTransactionManager();
        mGrantTimeXmlHelper = healthConnectInjector.getGrantTimeXmlHelper();
        mBackupRestore =
                new BackupRestore(
                        mFirstGrantTimeManager,
                        healthConnectInjector.getMigrationStateManager(),
                        healthConnectInjector.getPreferenceHelper(),
                        transactionManager,
                        mContext,
                        healthConnectInjector.getThreadScheduler(),
                        healthConnectInjector.getEnvironmentDataDirectory(),
                        mGrantTimeXmlHelper,
                        // Don't actually schedule jobs
                        mock(BackupRestore.BackupRestoreJobScheduler.class),
                        healthConnectInjector.getDatabaseMerger());

        mPhrTestUtils = new PhrTestUtils(healthConnectInjector);
    }

    @Test
    public void testGetAllDataForBackup_copiesAllDataIncludingPhr() throws Exception {
        // Insert a MedicalDataSource and MedicalResource.
        MedicalDataSource dataSource =
                mPhrTestUtils.insertR4MedicalDataSource("ds", TEST_PACKAGE_NAME);
        mPhrTestUtils.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource);
        // Insert a Step record.
        mFitnessTestUtils.insertRecords(TEST_PACKAGE_NAME, buildStepsRecord(123, 456, 7));
        // Ensure the original database contains the inserted data above.
        assertThat(mStorageUtils.queryNumEntries("medical_data_source_table")).isEqualTo(1);
        assertThat(mStorageUtils.queryNumEntries("medical_resource_table")).isEqualTo(1);
        assertThat(mStorageUtils.queryNumEntries("steps_record_table")).isEqualTo(1);

        // Create the files where the database and the grant time files will be backed up to.
        HealthConnectContext dbContext =
                HealthConnectContext.create(
                        mContext,
                        mContext.getUser(),
                        /* databaseDirName= */ null,
                        mEnvironmentDataDirectory.getRoot());
        File dbFileBacked = createAndGetEmptyFile(dbContext.getDataDir(), STAGED_DATABASE_NAME);
        File grantTimeFileBacked =
                createAndGetEmptyFile(dbContext.getDataDir(), GRANT_TIME_FILE_NAME);
        UserGrantTimeState userGrantTimeState =
                new UserGrantTimeState(Map.of("package", Instant.now()), Map.of(), 1);
        when(mFirstGrantTimeManager.getGrantTimeStateForUser(mContext.getUser()))
                .thenReturn(userGrantTimeState);
        // Prepare the pfds where the database and the grant time files are backed up to.
        Map<String, ParcelFileDescriptor> pfdsByFileName = new ArrayMap<>();
        pfdsByFileName.put(
                dbFileBacked.getName(),
                ParcelFileDescriptor.open(dbFileBacked, ParcelFileDescriptor.MODE_READ_WRITE));
        pfdsByFileName.put(
                grantTimeFileBacked.getName(),
                ParcelFileDescriptor.open(
                        grantTimeFileBacked, ParcelFileDescriptor.MODE_READ_WRITE));

        mBackupRestore.getAllDataForBackup(
                new StageRemoteDataRequest(pfdsByFileName), mContext.getUser());

        // Ensure the backed up database does not contain PHR data but includes everything else.
        try (HealthConnectDatabase backupDatabase =
                new HealthConnectDatabase(dbContext, dbFileBacked.getName())) {
            assertThat(queryNumEntries(backupDatabase, "medical_data_source_table")).isEqualTo(1);
            assertThat(queryNumEntries(backupDatabase, "medical_resource_table")).isEqualTo(1);
            assertThat(queryNumEntries(backupDatabase, "steps_record_table")).isEqualTo(1);
        }
        assertThat(mGrantTimeXmlHelper.parseGrantTime(grantTimeFileBacked).toString())
                .isEqualTo(userGrantTimeState.toString());
    }

    @Test
    public void testMerge_1000Resources_copiesAllPhrData() throws Exception {
        HealthConnectContext dbContext =
                HealthConnectContext.create(
                        mContext,
                        mContext.getUser(),
                        STAGED_DATABASE_DIR,
                        mEnvironmentDataDirectory.getRoot());
        createAndGetEmptyFile(dbContext.getDataDir(), STAGED_DATABASE_NAME);
        HealthConnectDatabase stagedDb = new HealthConnectDatabase(dbContext, STAGED_DATABASE_NAME);
        mFitnessTestUtils.insertApp(stagedDb, TEST_PACKAGE_NAME);
        Pair<Long, String> rowIdUuidPair =
                mPhrTestUtils.insertMedicalDataSource(
                        stagedDb, dbContext, DATA_SOURCE_SUFFIX, TEST_PACKAGE_NAME, INSTANT_NOW);
        int numOfResources = 1000;
        mPhrTestUtils.insertMedicalResources(
                stagedDb,
                PhrDataFactory::createVaccineMedicalResources,
                rowIdUuidPair.second,
                rowIdUuidPair.first,
                INSTANT_NOW_PLUS_TEN_SEC,
                numOfResources);
        assertThat(queryNumEntries(stagedDb, "medical_data_source_table")).isEqualTo(1);
        assertThat(queryNumEntries(stagedDb, "medical_resource_table")).isEqualTo(numOfResources);
        assertThat(queryNumEntries(stagedDb, "medical_resource_indices_table"))
                .isEqualTo(numOfResources);
        // Read the dataSources and lastModifiedTimestamps.
        List<Pair<MedicalDataSource, Long>> dataSourceRowsStaged =
                PhrTestUtils.readMedicalDataSources(stagedDb);
        // Read the medicalResources and lastModifiedTimestamps.
        List<Pair<MedicalResource, Long>> medicalResourceRowsStaged =
                PhrTestUtils.readAllMedicalResources(stagedDb);

        mBackupRestore.merge();

        assertThat(mStorageUtils.queryNumEntries("medical_data_source_table")).isEqualTo(1);
        assertThat(mStorageUtils.queryNumEntries("medical_resource_table"))
                .isEqualTo(numOfResources);
        assertThat(mStorageUtils.queryNumEntries("medical_resource_indices_table"))
                .isEqualTo(numOfResources);
        // Read the dataSources and lastModifiedTimestamps of original db after merge.
        List<Pair<MedicalDataSource, Long>> dataSourceRowsOriginal =
                mPhrTestUtils.readMedicalDataSources();
        // Assert dataSources and their timestamps of the staged db is the same as original db.
        assertThat(dataSourceRowsOriginal).isEqualTo(dataSourceRowsStaged);
        // Read the medicalResources and lastModifiedTimestamps of original db after merge.
        List<Pair<MedicalResource, Long>> medicalResourceRowsOriginal =
                mPhrTestUtils.readAllMedicalResources();
        // Assert medicalResources and their timestamps of the staged db is the same as original db.
        assertThat(medicalResourceRowsOriginal).hasSize(numOfResources);
        assertThat(medicalResourceRowsOriginal).isEqualTo(medicalResourceRowsStaged);
    }

    @Test
    public void testMerge_copiesAllPhrData() throws Exception {
        HealthConnectContext dbContext =
                HealthConnectContext.create(
                        mContext,
                        mContext.getUser(),
                        STAGED_DATABASE_DIR,
                        mEnvironmentDataDirectory.getRoot());
        createAndGetEmptyFile(dbContext.getDataDir(), STAGED_DATABASE_NAME);
        HealthConnectDatabase stagedDb = new HealthConnectDatabase(dbContext, STAGED_DATABASE_NAME);
        mFitnessTestUtils.insertApp(stagedDb, TEST_PACKAGE_NAME);
        Pair<Long, String> rowIdUuidPair =
                mPhrTestUtils.insertMedicalDataSource(
                        stagedDb, dbContext, DATA_SOURCE_SUFFIX, TEST_PACKAGE_NAME, INSTANT_NOW);
        mPhrTestUtils.insertMedicalResource(
                stagedDb,
                PhrDataFactory::createVaccineMedicalResource,
                rowIdUuidPair.second,
                rowIdUuidPair.first,
                INSTANT_NOW_PLUS_TEN_SEC);
        mPhrTestUtils.insertMedicalResource(
                stagedDb,
                PhrDataFactory::createDifferentVaccineMedicalResource,
                rowIdUuidPair.second,
                rowIdUuidPair.first,
                INSTANT_NOW_PLUS_TWENTY_SEC);
        assertThat(queryNumEntries(stagedDb, "medical_data_source_table")).isEqualTo(1);
        assertThat(queryNumEntries(stagedDb, "medical_resource_table")).isEqualTo(2);
        assertThat(queryNumEntries(stagedDb, "medical_resource_indices_table")).isEqualTo(2);
        // Read the dataSources and lastModifiedTimestamps.
        List<Pair<MedicalDataSource, Long>> dataSourceRowsStaged =
                PhrTestUtils.readMedicalDataSources(stagedDb);
        // Read the medicalResources and lastModifiedTimestamps.
        List<Pair<MedicalResource, Long>> medicalResourceRowsStaged =
                PhrTestUtils.readAllMedicalResources(stagedDb);

        mBackupRestore.merge();

        assertThat(mStorageUtils.queryNumEntries("medical_data_source_table")).isEqualTo(1);
        assertThat(mStorageUtils.queryNumEntries("medical_resource_table")).isEqualTo(2);
        assertThat(mStorageUtils.queryNumEntries("medical_resource_indices_table")).isEqualTo(2);
        // Read the dataSources and lastModifiedTimestamps of original db after merge.
        List<Pair<MedicalDataSource, Long>> dataSourceRowsOriginal =
                mPhrTestUtils.readMedicalDataSources();
        // Assert dataSources and their timestamps of the staged db is the same as original db.
        assertThat(dataSourceRowsOriginal).isEqualTo(dataSourceRowsStaged);
        // Read the medicalResources and lastModifiedTimestamps of original db after merge.
        List<Pair<MedicalResource, Long>> medicalResourceRowsOriginal =
                mPhrTestUtils.readAllMedicalResources();
        // Assert medicalResources and their timestamps of the staged db is the same as original db.
        assertThat(medicalResourceRowsOriginal).hasSize(2);
        assertThat(medicalResourceRowsOriginal).isEqualTo(medicalResourceRowsStaged);
    }

    @Test
    public void testMerge_doesNotCopyMedicalDataSourceDuplicates() throws Exception {
        // Insert a dataSource with display name using DATA_SOURCE_SUFFIX and TEST_PACKAGE_NAME.
        MedicalDataSource dataSource =
                mPhrTestUtils.insertR4MedicalDataSource(DATA_SOURCE_SUFFIX, TEST_PACKAGE_NAME);
        // Insert an allergy medicalResource.
        mPhrTestUtils.upsertResource(PhrDataFactory::createAllergyMedicalResource, dataSource);
        // Verify data exists.
        assertThat(mStorageUtils.queryNumEntries("medical_data_source_table")).isEqualTo(1);
        assertThat(mStorageUtils.queryNumEntries("medical_resource_table")).isEqualTo(1);
        assertThat(mStorageUtils.queryNumEntries("medical_resource_indices_table")).isEqualTo(1);
        // Create the staged db file.
        HealthConnectContext dbContext =
                HealthConnectContext.create(
                        mContext,
                        mContext.getUser(),
                        STAGED_DATABASE_DIR,
                        mEnvironmentDataDirectory.getRoot());
        createAndGetEmptyFile(dbContext.getDataDir(), STAGED_DATABASE_NAME);
        HealthConnectDatabase stagedDb = new HealthConnectDatabase(dbContext, STAGED_DATABASE_NAME);
        mFitnessTestUtils.insertApp(stagedDb, TEST_PACKAGE_NAME);
        // Insert a dataSource with the same unique ids (displayName, appId) into the
        // staged database.
        Pair<Long, String> rowIdUuidPair =
                mPhrTestUtils.insertMedicalDataSource(
                        stagedDb, dbContext, DATA_SOURCE_SUFFIX, TEST_PACKAGE_NAME, INSTANT_NOW);
        // Insert two different vaccine medicalResources associated with the dataSource we just
        // created.
        mPhrTestUtils.insertMedicalResource(
                stagedDb,
                PhrDataFactory::createVaccineMedicalResource,
                rowIdUuidPair.second,
                rowIdUuidPair.first,
                INSTANT_NOW_PLUS_TEN_SEC);
        mPhrTestUtils.insertMedicalResource(
                stagedDb,
                PhrDataFactory::createDifferentVaccineMedicalResource,
                rowIdUuidPair.second,
                rowIdUuidPair.first,
                INSTANT_NOW_PLUS_TWENTY_SEC);
        assertThat(queryNumEntries(stagedDb, "medical_data_source_table")).isEqualTo(1);
        assertThat(queryNumEntries(stagedDb, "medical_resource_table")).isEqualTo(2);
        assertThat(queryNumEntries(stagedDb, "medical_resource_indices_table")).isEqualTo(2);
        // Read the medicalResources of original db before merge.
        List<MedicalResource> medicalResourcesOriginalBeforeMerge =
                mPhrTestUtils.readAllMedicalResources().stream().map(pair -> pair.first).toList();
        // Read the medicalResources of staged db before merge.
        List<MedicalResource> medicalResourcesStagedBeforeMerge =
                PhrTestUtils.readAllMedicalResources(stagedDb).stream()
                        .map(pair -> pair.first)
                        .toList();

        mBackupRestore.merge();

        // We expect the medical_data_source table to contain 1 dataSource. Even though there was
        // 1 dataSource in original database and 1 in the staged database, they both have the
        // same unique ids so the one in the stagedDatabase will be ignored.
        List<MedicalDataSource> medicalDataSourcesAfterMerge =
                mPhrTestUtils.readMedicalDataSources().stream()
                        .map(pair -> pair.first)
                        .map(
                                ds ->
                                        new MedicalDataSource.Builder(ds)
                                                .setLastDataUpdateTime(null)
                                                .build())
                        .toList();
        assertThat(medicalDataSourcesAfterMerge).containsExactly(dataSource);
        assertThat(mStorageUtils.queryNumEntries("medical_data_source_table")).isEqualTo(1);
        // We expect 3 rows in both medical_resource and medical_resource_indices tables,
        // since there was 1 medicalResource in the original database and 2 medicalResources
        // in the staged database.
        List<MedicalResource> medicalResourcesAfterMerge =
                mPhrTestUtils.readAllMedicalResources().stream().map(pair -> pair.first).toList();
        assertThat(medicalResourcesAfterMerge).hasSize(3);
        assertThat(medicalResourcesAfterMerge)
                .containsAtLeastElementsIn(medicalResourcesOriginalBeforeMerge);
        assertThat(medicalResourcesAfterMerge)
                .containsAtLeastElementsIn(
                        medicalResourcesStagedBeforeMerge.stream()
                                // original data source was used during merge, so staged medical
                                // resources before merge needs to be updated to have the original
                                // data source id.
                                .map(
                                        ms ->
                                                new MedicalResource.Builder(ms)
                                                        .setDataSourceId(dataSource.getId())
                                                        .build())
                                .toList());
        // Assert the number of rows in the medical_resource and medical_resource_indices tables.
        assertThat(mStorageUtils.queryNumEntries("medical_resource_table")).isEqualTo(3);
        assertThat(mStorageUtils.queryNumEntries("medical_resource_indices_table")).isEqualTo(3);
    }

    @Test
    public void testMerge_doesNotCopyMedicalResourceDuplicates() throws Exception {
        // Insert a dataSource with display name using DATA_SOURCE_SUFFIX and TEST_PACKAGE_NAME.
        MedicalDataSource dataSource =
                mPhrTestUtils.insertR4MedicalDataSource(DATA_SOURCE_SUFFIX, TEST_PACKAGE_NAME);
        // Insert a vaccine medicalResource.
        mPhrTestUtils.upsertResource(PhrDataFactory::createVaccineMedicalResource, dataSource);
        // Verify data exists.
        assertThat(mStorageUtils.queryNumEntries("medical_data_source_table")).isEqualTo(1);
        assertThat(mStorageUtils.queryNumEntries("medical_resource_table")).isEqualTo(1);
        assertThat(mStorageUtils.queryNumEntries("medical_resource_indices_table")).isEqualTo(1);
        // Create the staged db file.
        HealthConnectContext dbContext =
                HealthConnectContext.create(
                        mContext,
                        mContext.getUser(),
                        STAGED_DATABASE_DIR,
                        mEnvironmentDataDirectory.getRoot());
        createAndGetEmptyFile(dbContext.getDataDir(), STAGED_DATABASE_NAME);
        HealthConnectDatabase stagedDb = new HealthConnectDatabase(dbContext, STAGED_DATABASE_NAME);
        mFitnessTestUtils.insertApp(stagedDb, TEST_PACKAGE_NAME);
        // Insert a dataSource with the same unique ids (displayName, appId) into the
        // staged database.
        Pair<Long, String> rowIdUuidPair =
                mPhrTestUtils.insertMedicalDataSource(
                        stagedDb, dbContext, DATA_SOURCE_SUFFIX, TEST_PACKAGE_NAME, INSTANT_NOW);
        // Insert the same vaccine resource as the one in the original database.
        mPhrTestUtils.insertMedicalResource(
                stagedDb,
                PhrDataFactory::createVaccineMedicalResource,
                rowIdUuidPair.second,
                rowIdUuidPair.first,
                INSTANT_NOW_PLUS_TEN_SEC);
        // Insert a different vaccine resource.
        mPhrTestUtils.insertMedicalResource(
                stagedDb,
                PhrDataFactory::createDifferentVaccineMedicalResource,
                rowIdUuidPair.second,
                rowIdUuidPair.first,
                INSTANT_NOW_PLUS_TWENTY_SEC);
        assertThat(queryNumEntries(stagedDb, "medical_data_source_table")).isEqualTo(1);
        assertThat(queryNumEntries(stagedDb, "medical_resource_table")).isEqualTo(2);
        assertThat(queryNumEntries(stagedDb, "medical_resource_indices_table")).isEqualTo(2);

        mBackupRestore.merge();

        // We expect the medical_data_source table to contain 1 dataSource. Even though there was
        // 1 dataSource in original database and 1 in the staged database, they both have the
        // same unique ids so the one in the stagedDatabase will be ignored.
        assertThat(mStorageUtils.queryNumEntries("medical_data_source_table")).isEqualTo(1);
        // Overall we have 3 medicalResources in both original and staged database but
        // we expect 2 rows in both medical_resource and medical_resource_indices tables after merge
        // since one of the vaccine resources in the stagedDatabase is a duplicate of an existing
        // resource in the original database.
        assertThat(mStorageUtils.queryNumEntries("medical_resource_table")).isEqualTo(2);
        assertThat(mStorageUtils.queryNumEntries("medical_resource_indices_table")).isEqualTo(2);
    }

    private static File createAndGetEmptyFile(File dir, String fileName) throws IOException {
        dir.mkdirs();
        File file = new File(dir, fileName);
        file.createNewFile();
        return file;
    }
}
