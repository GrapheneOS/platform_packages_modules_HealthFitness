/*
 * Copyright (C) 2024 The Android Open Source Project
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

import static android.health.connect.Constants.DEFAULT_PAGE_SIZE;
import static android.health.connect.PageTokenWrapper.EMPTY_PAGE_TOKEN;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_UNKNOWN;
import static android.healthconnect.testing.unittest.RecordInternalFactory.buildBloodPressureRecord;
import static android.healthconnect.testing.unittest.RecordInternalFactory.buildStepsRecord;

import static com.android.healthfitness.flags.Flags.FLAG_CLOUD_BACKUP_AND_RESTORE;
import static com.android.healthfitness.flags.Flags.FLAG_CLOUD_BACKUP_AND_RESTORE_DB;
import static com.android.healthfitness.flags.Flags.FLAG_ECOSYSTEM_METRICS_DB_CHANGES;
import static com.android.server.healthconnect.backuprestore.RecordProtoConverter.PROTO_VERSION;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import android.content.Context;
import android.database.sqlite.SQLiteException;
import android.health.connect.RecordIdFilter;
import android.health.connect.backuprestore.GetChangesForBackupResponse;
import android.health.connect.datatypes.StepsRecord;
import android.health.connect.internal.datatypes.RecordInternal;
import android.health.connect.internal.datatypes.StepsRecordInternal;
import android.health.connect.internal.datatypes.utils.HealthConnectMappings;
import android.healthconnect.testing.unittest.FitnessTestUtils;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.server.healthconnect.common.changelog.ChangeLogsHelper;
import com.android.server.healthconnect.common.changelog.ChangeLogsRequestHelper;
import com.android.server.healthconnect.common.metadata.AppInfoHelper;
import com.android.server.healthconnect.common.metadata.DeviceInfoHelper;
import com.android.server.healthconnect.common.preferences.PreferenceHelper;
import com.android.server.healthconnect.fitness.helpers.HealthDataCategoryPriorityHelper;
import com.android.server.healthconnect.fitness.mappings.InternalHealthConnectMappings;
import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;
import com.android.server.healthconnect.permission.FirstGrantTimeManager;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.BackupData;
import com.android.server.healthconnect.storage.HealthConnectContext;
import com.android.server.healthconnect.storage.HealthConnectDatabase;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.request.DeleteTableRequest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/** Unit test for class {@link CloudBackupManager}. */
@RunWith(AndroidJUnit4.class)
@EnableFlags({
    FLAG_CLOUD_BACKUP_AND_RESTORE,
    FLAG_CLOUD_BACKUP_AND_RESTORE_DB,
    FLAG_ECOSYSTEM_METRICS_DB_CHANGES
})
public class CloudBackupManagerTest {
    private static final String TEST_PACKAGE_NAME = "test.package.name";
    private static final long TEST_START_TIME_IN_MILLIS = 2000;
    private static final long TEST_END_TIME_IN_MILLIS = 3000;
    private static final int TEST_STEP_COUNT = 1345;
    private static final int TEST_TIME_IN_MILLIS = 1234;
    private static final double TEST_SYSTOLIC = 60.2;
    private static final double TEST_DIASTOLIC = 92.6;

    @Rule(order = 1)
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Rule public final TemporaryFolder mEnvironmentDataDirectory = new TemporaryFolder();

    private Context mContext;
    private TransactionManager mTransactionManager;
    private FitnessTestUtils mFitnessTestUtils;
    private CloudBackupManager mCloudBackupManager;
    private RecordProtoConverter mRecordProtoConverter;
    private Instant mTimeStamp;

    // TODO(b/373322447): Remove the mock FirstGrantTimeManager
    @Mock private FirstGrantTimeManager mFirstGrantTimeManager;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        HealthConnectInjector healthConnectInjector =
                HealthConnectInjectorImpl.newBuilderForTest(mContext)
                        .setFirstGrantTimeManager(mFirstGrantTimeManager)
                        .setEnvironmentDataDirectory(mEnvironmentDataDirectory.getRoot())
                        .build();

        mTransactionManager = healthConnectInjector.getTransactionManager();
        mFitnessTestUtils = new FitnessTestUtils(healthConnectInjector);
        mFitnessTestUtils.insertApp(TEST_PACKAGE_NAME);
        AppInfoHelper appInfoHelper = healthConnectInjector.getAppInfoHelper();
        DeviceInfoHelper deviceInfoHelper = healthConnectInjector.getDeviceInfoHelper();
        HealthDataCategoryPriorityHelper priorityHelper =
                healthConnectInjector.getHealthDataCategoryPriorityHelper();
        PreferenceHelper preferenceHelper = healthConnectInjector.getPreferenceHelper();
        HealthConnectMappings healthConnectMappings =
                healthConnectInjector.getHealthConnectMappings();
        InternalHealthConnectMappings internalHealthConnectMappings =
                healthConnectInjector.getInternalHealthConnectMappings();
        ChangeLogsHelper changeLogsHelper = healthConnectInjector.getChangeLogsHelper();
        ChangeLogsRequestHelper changeLogsRequestHelper =
                healthConnectInjector.getChangeLogsRequestHelper();

        mTimeStamp = Instant.parse("2024-06-04T16:39:12Z");
        Clock fakeClock = Clock.fixed(mTimeStamp, ZoneId.of("UTC"));

        mCloudBackupManager =
                new CloudBackupManager(
                        mTransactionManager,
                        healthConnectInjector.getFitnessRecordReadHelper(),
                        appInfoHelper,
                        deviceInfoHelper,
                        healthConnectMappings,
                        internalHealthConnectMappings,
                        changeLogsHelper,
                        changeLogsRequestHelper,
                        priorityHelper,
                        preferenceHelper,
                        fakeClock,
                        healthConnectInjector.getBackupRestoreLogger());

        mRecordProtoConverter = new RecordProtoConverter();
    }

    @Test
    public void getChangesForBackup_noMoreChangeLogs_correctResponseReturned() {
        mFitnessTestUtils.insertRecords(
                TEST_PACKAGE_NAME,
                buildStepsRecord(
                        TEST_START_TIME_IN_MILLIS, TEST_END_TIME_IN_MILLIS, TEST_STEP_COUNT));
        GetChangesForBackupResponse response = mCloudBackupManager.getChangesForBackup(null);
        BackupChangeTokenHelper.BackupChangeToken firstBackupToken =
                BackupChangeTokenHelper.getBackupChangeToken(
                        mTransactionManager, response.getNextChangeToken());

        GetChangesForBackupResponse secondResponse =
                mCloudBackupManager.getChangesForBackup(response.getNextChangeToken());

        assertThat(secondResponse.getChanges().size()).isEqualTo(0);
        BackupChangeTokenHelper.BackupChangeToken secondBackupChangeToken =
                BackupChangeTokenHelper.getBackupChangeToken(
                        mTransactionManager, secondResponse.getNextChangeToken());
        assertThat(secondBackupChangeToken.getRecordType()).isEqualTo(RECORD_TYPE_UNKNOWN);
        assertThat(secondBackupChangeToken.getDataTablePageToken())
                .isEqualTo(EMPTY_PAGE_TOKEN.encode());
        // Same change logs token so the next incremental call will start from the same point.
        assertThat(secondBackupChangeToken.getChangeLogsRequestToken())
                .isEqualTo(firstBackupToken.getChangeLogsRequestToken());
    }

    @Test
    public void getChangesForBackup_dataTableIsNotNull_succeed() {
        List<RecordInternal<?>> records = new ArrayList<>();
        for (int recordNumber = 0; recordNumber < DEFAULT_PAGE_SIZE + 1; recordNumber++) {
            records.add(
                    buildStepsRecord(
                            // Add offsets to start time and end time for distinguishing different
                            // records.
                            TEST_START_TIME_IN_MILLIS + recordNumber,
                            TEST_END_TIME_IN_MILLIS + recordNumber,
                            TEST_STEP_COUNT));
        }
        mFitnessTestUtils.insertRecords(TEST_PACKAGE_NAME, records);

        GetChangesForBackupResponse response = mCloudBackupManager.getChangesForBackup(null);

        GetChangesForBackupResponse secondResponse =
                mCloudBackupManager.getChangesForBackup(response.getNextChangeToken());
        assertThat(secondResponse.getChanges().size()).isEqualTo(1);
    }

    @Test
    public void getChangesForBackup_changeLogsTokenInvalid_throwsException() {
        List<RecordInternal<?>> records = new ArrayList<>();
        // Use DEFAULT_PAGE_SIZE + 1 to make sure the returned change token, which to be used for
        // the second call of getChangesForBackup, is not empty.
        for (int recordNumber = 0; recordNumber < DEFAULT_PAGE_SIZE + 1; recordNumber++) {
            records.add(
                    buildStepsRecord(
                            // Add offsets to start time and end time for distinguishing different
                            // records.
                            TEST_START_TIME_IN_MILLIS + recordNumber,
                            TEST_END_TIME_IN_MILLIS + recordNumber,
                            TEST_STEP_COUNT));
        }
        mFitnessTestUtils.insertRecords(TEST_PACKAGE_NAME, records);
        GetChangesForBackupResponse response = mCloudBackupManager.getChangesForBackup(null);
        // Delete change logs.
        mTransactionManager.delete(new DeleteTableRequest(ChangeLogsHelper.TABLE_NAME));

        assertThrows(
                IllegalArgumentException.class,
                () -> mCloudBackupManager.getChangesForBackup(response.getNextChangeToken()));
    }

    @Test
    public void getChangesForBackup_noMoreChanges() {
        GetChangesForBackupResponse prevResponse = mCloudBackupManager.getChangesForBackup(null);
        mFitnessTestUtils.insertRecords(
                TEST_PACKAGE_NAME,
                buildStepsRecord(
                        TEST_START_TIME_IN_MILLIS, TEST_END_TIME_IN_MILLIS, TEST_STEP_COUNT));
        GetChangesForBackupResponse response =
                mCloudBackupManager.getChangesForBackup(prevResponse.getNextChangeToken());

        // Call getChangesForBackup two times to make sure no changes are returned properly in
        // the end and tokens are still valid.
        response = mCloudBackupManager.getChangesForBackup(response.getNextChangeToken());
        response = mCloudBackupManager.getChangesForBackup(response.getNextChangeToken());

        assertThat(response.getChanges()).isEmpty();
    }

    @Test
    public void getChangesForBackup_noMoreChangesForIncrementalBackup() {
        mFitnessTestUtils.insertRecords(
                TEST_PACKAGE_NAME,
                buildStepsRecord(
                        TEST_START_TIME_IN_MILLIS, TEST_END_TIME_IN_MILLIS, TEST_STEP_COUNT));
        GetChangesForBackupResponse response = mCloudBackupManager.getChangesForBackup(null);

        while (!response.getChanges().isEmpty()) {
            response = mCloudBackupManager.getChangesForBackup(response.getNextChangeToken());
        }

        assertThat(response.getChanges()).isEmpty();
    }

    @Test
    public void getChangesForBackup_changeTokenIsNull_succeed() {
        mFitnessTestUtils.insertRecords(
                TEST_PACKAGE_NAME,
                buildStepsRecord(
                        TEST_START_TIME_IN_MILLIS, TEST_END_TIME_IN_MILLIS, TEST_STEP_COUNT));

        GetChangesForBackupResponse response = mCloudBackupManager.getChangesForBackup(null);

        assertThat(response.getChanges().size()).isEqualTo(1);
        String nextChangeToken = response.getNextChangeToken();
        assertThat(nextChangeToken).isEqualTo("1");
    }

    @Test
    public void getChangesForBackup_throwsDatabaseException() {
        mFitnessTestUtils.insertRecords(
                TEST_PACKAGE_NAME,
                buildStepsRecord(
                        TEST_START_TIME_IN_MILLIS, TEST_END_TIME_IN_MILLIS, TEST_STEP_COUNT));

        // Delete backup_change_token_table.
        HealthConnectContext dbContext =
                HealthConnectContext.create(
                        mContext,
                        mContext.getUser(),
                        /* databaseDirName= */ null,
                        mEnvironmentDataDirectory.getRoot());
        try (HealthConnectDatabase database = new HealthConnectDatabase(dbContext)) {
            database.getWritableDatabase()
                    .execSQL("DROP TABLE IF EXISTS " + BackupChangeTokenHelper.getTableName());

            assertThrows(
                    SQLiteException.class, () -> mCloudBackupManager.getChangesForBackup(null));
            // Add backup_change_token_table back to not affect other tests.
            BackupChangeTokenHelper.applyBackupTokenUpgrade(database.getWritableDatabase());
        }
    }

    @Test
    public void getSettingsForBackup_returnsProtoVersion() {
        var response = mCloudBackupManager.getSettingsForBackup();
        assertThat(response.getCurrentVersion()).isEqualTo(PROTO_VERSION);
    }

    @Test
    public void insertRecordsDuringBackup_insertedRecordsReturnedInIncrementalBackup() {
        // Full data has to be done with two pages.
        List<RecordInternal<?>> records = createStepRecords(DEFAULT_PAGE_SIZE + 1);
        mFitnessTestUtils.insertRecords(TEST_PACKAGE_NAME, records);
        // First full data backup call
        GetChangesForBackupResponse firstResponse = mCloudBackupManager.getChangesForBackup(null);
        // Insert one more record during the backup
        var bloodPressureRecord =
                buildBloodPressureRecord(TEST_TIME_IN_MILLIS, TEST_SYSTOLIC, TEST_DIASTOLIC);
        mFitnessTestUtils.insertRecords(TEST_PACKAGE_NAME, bloodPressureRecord);
        // Second full data backup call
        GetChangesForBackupResponse secondResponse =
                mCloudBackupManager.getChangesForBackup(firstResponse.getNextChangeToken());

        // Incremental backup call
        GetChangesForBackupResponse thirdResponse =
                mCloudBackupManager.getChangesForBackup(secondResponse.getNextChangeToken());

        // Inserted record should be returned in the second response.
        assertThat(secondResponse.getChanges().size()).isEqualTo(2);
        assertThat(thirdResponse.getChanges().size()).isEqualTo(1);
        assertThat(thirdResponse.getChanges().get(0).getChangeId())
                .isEqualTo(bloodPressureRecord.getUuid().toString());
    }

    @Test
    public void updatesRecordsDuringBackup_updatedRecordsReturnedInIncrementalBackup() {
        // Full data has to be done with two pages.
        List<RecordInternal<?>> records = createStepRecords(DEFAULT_PAGE_SIZE + 1);
        mFitnessTestUtils.insertRecords(TEST_PACKAGE_NAME, records);
        // First full data backup call
        GetChangesForBackupResponse firstResponse = mCloudBackupManager.getChangesForBackup(null);
        // Modifies one record during the backup
        var modifiedRecord = ((StepsRecordInternal) records.get(DEFAULT_PAGE_SIZE)).setCount(2);
        mFitnessTestUtils.updateRecords(TEST_PACKAGE_NAME, modifiedRecord);
        // Second full data backup call
        GetChangesForBackupResponse secondResponse =
                mCloudBackupManager.getChangesForBackup(firstResponse.getNextChangeToken());

        // Incremental backup call
        GetChangesForBackupResponse thirdResponse =
                mCloudBackupManager.getChangesForBackup(secondResponse.getNextChangeToken());

        // Modified record should be returned in the second and the third response.
        assertThat(secondResponse.getChanges().size()).isEqualTo(1);
        assertThat(thirdResponse.getChanges().size()).isEqualTo(1);
        assertThat(thirdResponse.getChanges().get(0)).isEqualTo(secondResponse.getChanges().get(0));
    }

    @Test
    public void deletesRecordsDuringBackup_deletedRecordsReturnedInIncrementalBackup() {
        // Full data has to be done with two pages.
        List<RecordInternal<?>> records = createStepRecords(DEFAULT_PAGE_SIZE + 2);
        mFitnessTestUtils.insertRecords(TEST_PACKAGE_NAME, records);
        // First full data backup call
        GetChangesForBackupResponse firstResponse = mCloudBackupManager.getChangesForBackup(null);
        // Delete one record during the backup
        mFitnessTestUtils.deleteRecords(
                TEST_PACKAGE_NAME,
                RecordIdFilter.fromId(
                        StepsRecord.class, records.get(DEFAULT_PAGE_SIZE).getUuid().toString()));

        // Second full data backup call
        GetChangesForBackupResponse secondResponse =
                mCloudBackupManager.getChangesForBackup(firstResponse.getNextChangeToken());

        // Incremental backup call
        GetChangesForBackupResponse thirdResponse =
                mCloudBackupManager.getChangesForBackup(secondResponse.getNextChangeToken());

        // Modified record should be returned in the second and the third response.
        assertThat(secondResponse.getChanges().size()).isEqualTo(1);
        assertThat(thirdResponse.getChanges().size()).isEqualTo(1);
        assertThat(thirdResponse.getChanges().get(0).isDeletion()).isTrue();
    }

    @Test
    public void multipleIncrementalBackup_correctResponsesReturned() throws Exception {
        List<RecordInternal<?>> initialRecords = createStepRecords(2);
        mFitnessTestUtils.insertRecords(TEST_PACKAGE_NAME, initialRecords);
        // First full data backup call
        GetChangesForBackupResponse firstResponse = mCloudBackupManager.getChangesForBackup(null);

        // Insert records and backup
        var recordToBeInserted =
                buildBloodPressureRecord(TEST_TIME_IN_MILLIS, TEST_SYSTOLIC, TEST_DIASTOLIC);
        mFitnessTestUtils.insertRecords(TEST_PACKAGE_NAME, recordToBeInserted);
        GetChangesForBackupResponse secondResponse =
                mCloudBackupManager.getChangesForBackup(firstResponse.getNextChangeToken());
        assertThat(secondResponse.getChanges().size()).isEqualTo(1);
        assertThat(secondResponse.getChanges().get(0).getData())
                .isEqualTo(serializeRecordInternal(recordToBeInserted));

        // Modifies one record during the backup
        var modifiedRecord = ((StepsRecordInternal) initialRecords.get(0)).setCount(2);
        mFitnessTestUtils.updateRecords(TEST_PACKAGE_NAME, modifiedRecord);
        GetChangesForBackupResponse thirdResponse =
                mCloudBackupManager.getChangesForBackup(secondResponse.getNextChangeToken());
        assertThat(thirdResponse.getChanges().size()).isEqualTo(1);
        assertThat(thirdResponse.getChanges().get(0).getData())
                .isEqualTo(serializeRecordInternal(modifiedRecord));

        // Delete one record during the backup
        mFitnessTestUtils.deleteRecords(
                TEST_PACKAGE_NAME,
                RecordIdFilter.fromId(
                        StepsRecord.class, initialRecords.get(1).getUuid().toString()));
        GetChangesForBackupResponse fourthResponse =
                mCloudBackupManager.getChangesForBackup(thirdResponse.getNextChangeToken());
        assertThat(fourthResponse.getChanges().size()).isEqualTo(1);
        assertThat(fourthResponse.getChanges().get(0).isDeletion()).isTrue();
    }

    private List<RecordInternal<?>> createStepRecords(int recordSize) {
        List<RecordInternal<?>> records = new ArrayList<>();
        for (int recordNumber = 0; recordNumber < recordSize; recordNumber++) {
            records.add(
                    buildStepsRecord(
                            // Add offsets to start time and end time for distinguishing different
                            // records.
                            TEST_START_TIME_IN_MILLIS + recordNumber,
                            TEST_END_TIME_IN_MILLIS + recordNumber,
                            TEST_STEP_COUNT));
        }
        return records;
    }

    private byte[] serializeRecordInternal(RecordInternal<?> recordInternal) {
        return BackupData.newBuilder()
                .setRecord(mRecordProtoConverter.toRecordProto(recordInternal))
                .build()
                .toByteArray();
    }
}
