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

import static android.health.connect.HealthConnectDataState.RESTORE_ERROR_FETCHING_DATA;
import static android.health.connect.HealthConnectDataState.RESTORE_ERROR_NONE;
import static android.health.connect.HealthConnectDataState.RESTORE_ERROR_UNKNOWN;
import static android.health.connect.HealthConnectDataState.RESTORE_ERROR_VERSION_DIFF;
import static android.health.connect.HealthConnectDataState.RESTORE_STATE_IDLE;
import static android.health.connect.HealthConnectManager.DATA_DOWNLOAD_FAILED;
import static android.health.connect.HealthConnectManager.DATA_DOWNLOAD_RETRY;
import static android.health.connect.HealthConnectManager.DATA_DOWNLOAD_STARTED;

import static com.android.healthfitness.flags.AconfigFlagHelper.isDeviceDataProvidersEnabled;
import static com.android.server.healthconnect.backuprestore.BackupRestore.BackupRestoreJobService.BACKUP_RESTORE_JOBS_NAMESPACE;
import static com.android.server.healthconnect.backuprestore.BackupRestore.BackupRestoreJobService.EXTRA_JOB_NAME_KEY;
import static com.android.server.healthconnect.backuprestore.BackupRestore.DATA_DOWNLOAD_STATE_KEY;
import static com.android.server.healthconnect.backuprestore.BackupRestore.DATA_DOWNLOAD_TIMEOUT_CANCELLED_KEY;
import static com.android.server.healthconnect.backuprestore.BackupRestore.DATA_DOWNLOAD_TIMEOUT_INTERVAL_MILLIS;
import static com.android.server.healthconnect.backuprestore.BackupRestore.DATA_DOWNLOAD_TIMEOUT_KEY;
import static com.android.server.healthconnect.backuprestore.BackupRestore.DATA_MERGING_DELAY_MILLIS;
import static com.android.server.healthconnect.backuprestore.BackupRestore.DATA_MERGING_KEY;
import static com.android.server.healthconnect.backuprestore.BackupRestore.DATA_MERGING_RETRY_DELAY_MILLIS;
import static com.android.server.healthconnect.backuprestore.BackupRestore.DATA_MERGING_TIMEOUT_CANCELLED_KEY;
import static com.android.server.healthconnect.backuprestore.BackupRestore.DATA_MERGING_TIMEOUT_INTERVAL_MILLIS;
import static com.android.server.healthconnect.backuprestore.BackupRestore.DATA_MERGING_TIMEOUT_KEY;
import static com.android.server.healthconnect.backuprestore.BackupRestore.DATA_RESTORE_ERROR_KEY;
import static com.android.server.healthconnect.backuprestore.BackupRestore.DATA_RESTORE_STATE_KEY;
import static com.android.server.healthconnect.backuprestore.BackupRestore.DATA_STAGING_TIMEOUT_CANCELLED_KEY;
import static com.android.server.healthconnect.backuprestore.BackupRestore.DATA_STAGING_TIMEOUT_INTERVAL_MILLIS;
import static com.android.server.healthconnect.backuprestore.BackupRestore.DATA_STAGING_TIMEOUT_KEY;
import static com.android.server.healthconnect.backuprestore.BackupRestore.INTERNAL_RESTORE_STATE_MERGING_DONE;
import static com.android.server.healthconnect.backuprestore.BackupRestore.INTERNAL_RESTORE_STATE_MERGING_DONE_OLD_CODE;
import static com.android.server.healthconnect.backuprestore.BackupRestore.INTERNAL_RESTORE_STATE_MERGING_IN_PROGRESS;
import static com.android.server.healthconnect.backuprestore.BackupRestore.INTERNAL_RESTORE_STATE_STAGING_DONE;
import static com.android.server.healthconnect.backuprestore.BackupRestore.INTERNAL_RESTORE_STATE_STAGING_IN_PROGRESS;
import static com.android.server.healthconnect.backuprestore.BackupRestore.INTERNAL_RESTORE_STATE_WAITING_FOR_STAGING;
import static com.android.server.healthconnect.backuprestore.BackupRestore.STAGED_DATABASE_DIR;
import static com.android.server.healthconnect.backuprestore.BackupRestore.STAGED_DATABASE_NAME;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.annotation.Nullable;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.health.connect.HealthConnectManager;
import android.health.connect.restore.BackupFileNamesSet;
import android.health.connect.restore.StageRemoteDataRequest;
import android.healthconnect.testing.unittest.fakes.FakePreferenceHelper;
import android.os.ParcelFileDescriptor;
import android.os.PersistableBundle;
import android.os.UserHandle;
import android.platform.test.flag.junit.SetFlagsRule;
import android.util.ArrayMap;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.server.healthconnect.common.metadata.AppInfoHelper;
import com.android.server.healthconnect.device.DeviceDataProviderManager;
import com.android.server.healthconnect.device.FakeSerialDeviceDataProviderManager;
import com.android.server.healthconnect.fitness.mappings.InternalHealthConnectMappings;
import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;
import com.android.server.healthconnect.migration.MigrationStateManager;
import com.android.server.healthconnect.permission.FirstGrantTimeManager;
import com.android.server.healthconnect.permission.GrantTimeXmlHelper;
import com.android.server.healthconnect.permission.UserGrantTimeState;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.utils.FilesUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Unit test for class {@link BackupRestore} */
@RunWith(AndroidJUnit4.class)
public class BackupRestoreTest {
    private static final String DATABASE_NAME = "healthconnect.db";
    private static final String GRANT_TIME_FILE_NAME = "health-permissions-first-grant-times.xml";

    @Rule(order = 1)
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Rule public final TemporaryFolder mEnvironmentDataDirectory = new TemporaryFolder();

    @Mock Context mServiceContext;
    @Mock private TransactionManager mTransactionManager;
    @Mock private Cursor mCursor;
    @Mock private FirstGrantTimeManager mFirstGrantTimeManager;
    @Mock private MigrationStateManager mMockMigrationStateManager;
    @Mock private Context mContext;
    @Mock private JobScheduler mJobScheduler;
    @Mock private AppInfoHelper mAppInfoHelper;
    @Mock private BackupRestore.BackupRestoreJobScheduler mBackupRestoreJobScheduler;
    @Spy private GrantTimeXmlHelper mGrantTimeXmlHelper = new GrantTimeXmlHelper();
    @Captor ArgumentCaptor<JobInfo> mJobInfoArgumentCaptor;
    private BackupRestore mBackupRestore;
    private final FakePreferenceHelper mFakePreferenceHelper = new FakePreferenceHelper();
    private UserHandle mUserHandle = UserHandle.of(UserHandle.myUserId());
    private File mMockBackedDataDirectory;
    private File mMockStagedDataDirectory;

    @Before
    public void setUp() throws Exception {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mMockBackedDataDirectory = mContext.getDir("mock_backed_data", Context.MODE_PRIVATE);
        mMockStagedDataDirectory = mContext.getDir("mock_staged_data", Context.MODE_PRIVATE);

        when(mJobScheduler.forNamespace(BACKUP_RESTORE_JOBS_NAMESPACE)).thenReturn(mJobScheduler);
        when(mServiceContext.getUser()).thenReturn(mUserHandle);
        when(mServiceContext.getSystemService(JobScheduler.class)).thenReturn(mJobScheduler);
        when(mServiceContext.getPackageName()).thenReturn("packageName");
        when(mTransactionManager.read(any(), any())).thenReturn(mCursor);
        when(mTransactionManager.read(any())).thenReturn(mCursor);

        HealthConnectInjector healthConnectInjector = createHealthConnectInjector();

        if (isDeviceDataProvidersEnabled()) {
            healthConnectInjector
                    .getDeviceDataProviderManager()
                    .initializeOrRefreshCurrentDeviceIds();
        }

        mBackupRestore =
                new BackupRestore(
                        mAppInfoHelper,
                        mFirstGrantTimeManager,
                        healthConnectInjector.getMigrationStateManager(),
                        healthConnectInjector.getPreferenceHelper(),
                        healthConnectInjector.getTransactionManager(),
                        healthConnectInjector.getFitnessRecordUpsertHelper(),
                        healthConnectInjector.getFitnessRecordReadHelper(),
                        mServiceContext,
                        healthConnectInjector.getDeviceInfoHelper(),
                        healthConnectInjector.getDeviceDataProviderMetadataHelper(),
                        healthConnectInjector.getSyntheticPackageNameCreator(),
                        healthConnectInjector.getHealthDataCategoryPriorityHelper(),
                        healthConnectInjector.getThreadScheduler(),
                        healthConnectInjector.getEnvironmentDataDirectory(),
                        mGrantTimeXmlHelper,
                        mBackupRestoreJobScheduler);
    }

    private HealthConnectInjector createHealthConnectInjector() {
        HealthConnectInjector injectorTemp =
                HealthConnectInjectorImpl.newBuilderForTest(mContext)
                        .setPreferenceHelper(mFakePreferenceHelper)
                        .setMigrationStateManager(mMockMigrationStateManager)
                        .setFirstGrantTimeManager(mFirstGrantTimeManager)
                        .setTransactionManager(mTransactionManager)
                        .setEnvironmentDataDirectory(mEnvironmentDataDirectory.getRoot())
                        .build();

        DeviceDataProviderManager fakeDeviceDataProviderManager =
                new FakeSerialDeviceDataProviderManager(
                        mContext,
                        injectorTemp.getDeviceInfoHelper(),
                        injectorTemp.getAppInfoHelper(),
                        injectorTemp.getDeviceDataSourceHelper(),
                        injectorTemp.getDeviceDataSourcesHelper(),
                        injectorTemp.getDeviceDataProviderMetadataHelper(),
                        injectorTemp.getFitnessRecordUpsertHelper(),
                        injectorTemp.getFitnessRecordReadHelper(),
                        injectorTemp.getFitnessRecordDeleteHelper(),
                        injectorTemp.getSyntheticPackageNameCreator(),
                        injectorTemp.getPreferenceHelper(),
                        injectorTemp.getHealthDataCategoryPriorityHelper(),
                        InternalHealthConnectMappings.getInstance(),
                        true);

        return HealthConnectInjectorImpl.newBuilderForTest(mContext)
                .setPreferenceHelper(mFakePreferenceHelper)
                .setMigrationStateManager(mMockMigrationStateManager)
                .setFirstGrantTimeManager(mFirstGrantTimeManager)
                .setTransactionManager(mTransactionManager)
                .setEnvironmentDataDirectory(mEnvironmentDataDirectory.getRoot())
                .setDeviceDataProviderManager(fakeDeviceDataProviderManager)
                .build();
    }

    @After
    public void tearDown() {
        FilesUtil.deleteDir(mMockBackedDataDirectory);
        FilesUtil.deleteDir(mMockStagedDataDirectory);
        mFakePreferenceHelper.clearCache();
    }

    @Test
    public void testGetAllBackupFileNames_forDeviceToDevice_returnsAllFileNames() throws Exception {
        BackupFileNamesSet backupFileNamesSet = mBackupRestore.getAllBackupFileNames(true);

        assertThat(backupFileNamesSet).isNotNull();
        assertThat(backupFileNamesSet.getFileNames()).hasSize(2);
        assertThat(backupFileNamesSet.getFileNames()).contains(STAGED_DATABASE_NAME);
        assertThat(backupFileNamesSet.getFileNames()).contains(GRANT_TIME_FILE_NAME);
    }

    @Test
    public void testGetAllBackupFileNames_forNonDeviceToDevice_returnsSmallFileNames()
            throws Exception {
        BackupFileNamesSet backupFileNamesSet = mBackupRestore.getAllBackupFileNames(false);

        assertThat(backupFileNamesSet).isNotNull();
        assertThat(backupFileNamesSet.getFileNames()).hasSize(1);
        assertThat(backupFileNamesSet.getFileNames()).contains(GRANT_TIME_FILE_NAME);
    }

    @Test
    public void testGetAllBackupData_forDeviceToDevice_copiesAllData() throws Exception {
        File dbFileToBackup =
                createAndGetNonEmptyFile(mEnvironmentDataDirectory.getRoot(), DATABASE_NAME);
        File dbFileBacked = createAndGetEmptyFile(mMockBackedDataDirectory, STAGED_DATABASE_NAME);
        File grantTimeFileBacked =
                createAndGetEmptyFile(mMockBackedDataDirectory, GRANT_TIME_FILE_NAME);

        when(mTransactionManager.getDatabasePath()).thenReturn(dbFileToBackup);
        UserGrantTimeState userGrantTimeState =
                new UserGrantTimeState(Map.of("package", Instant.now()), Map.of(), 1);
        when(mFirstGrantTimeManager.getGrantTimeStateForUser(mUserHandle))
                .thenReturn(userGrantTimeState);

        Map<String, ParcelFileDescriptor> pfdsByFileName = new ArrayMap<>();
        pfdsByFileName.put(
                dbFileBacked.getName(),
                ParcelFileDescriptor.open(dbFileBacked, ParcelFileDescriptor.MODE_READ_WRITE));
        pfdsByFileName.put(
                grantTimeFileBacked.getName(),
                ParcelFileDescriptor.open(
                        grantTimeFileBacked, ParcelFileDescriptor.MODE_READ_WRITE));

        mBackupRestore.getAllDataForBackup(new StageRemoteDataRequest(pfdsByFileName), mUserHandle);

        assertThat(dbFileBacked.length()).isEqualTo(dbFileToBackup.length());
        assertThat(mGrantTimeXmlHelper.parseGrantTime(grantTimeFileBacked).toString())
                .isEqualTo(userGrantTimeState.toString());
    }

    @Test
    public void testSetDataDownloadState_downloadStarted_schedulesDownloadTimeoutJob() {
        @HealthConnectManager.DataDownloadState int testDownloadStateSet = DATA_DOWNLOAD_STARTED;
        mFakePreferenceHelper.insertOrReplacePreference(
                DATA_DOWNLOAD_STATE_KEY, String.valueOf(testDownloadStateSet));

        mBackupRestore.updateDataDownloadState(testDownloadStateSet);

        verify(mBackupRestoreJobScheduler)
                .schedule(
                        eq(mServiceContext), mJobInfoArgumentCaptor.capture(), eq(mBackupRestore));

        assertThat(mFakePreferenceHelper.getPreference(DATA_DOWNLOAD_STATE_KEY))
                .isEqualTo(String.valueOf(testDownloadStateSet));
        JobInfo jobInfo = mJobInfoArgumentCaptor.getValue();
        assertThat(jobInfo.getExtras().getString(EXTRA_JOB_NAME_KEY))
                .isEqualTo(DATA_DOWNLOAD_TIMEOUT_KEY);
    }

    @Test
    public void testSetDataDownloadState_downloadRetry_schedulesDownloadTimeoutJob() {
        @HealthConnectManager.DataDownloadState int testDownloadStateSet = DATA_DOWNLOAD_RETRY;
        mFakePreferenceHelper.insertOrReplacePreference(
                DATA_DOWNLOAD_STATE_KEY, String.valueOf(testDownloadStateSet));

        mBackupRestore.updateDataDownloadState(testDownloadStateSet);
        verify(mBackupRestoreJobScheduler)
                .schedule(
                        eq(mServiceContext), mJobInfoArgumentCaptor.capture(), eq(mBackupRestore));
        assertThat(mFakePreferenceHelper.getPreference(DATA_DOWNLOAD_STATE_KEY))
                .isEqualTo(String.valueOf(testDownloadStateSet));
        JobInfo jobInfo = mJobInfoArgumentCaptor.getValue();
        assertThat(jobInfo.getExtras().getString(EXTRA_JOB_NAME_KEY))
                .isEqualTo(DATA_DOWNLOAD_TIMEOUT_KEY);
    }

    @Test
    public void testSetInternalRestoreState_waitingForStaging_schedulesStagingTimeoutJob() {
        mFakePreferenceHelper.insertOrReplacePreference(
                DATA_RESTORE_STATE_KEY, String.valueOf(INTERNAL_RESTORE_STATE_WAITING_FOR_STAGING));

        // forcing the state because we want to this state to set even when it's already set.
        mBackupRestore.setInternalRestoreState(INTERNAL_RESTORE_STATE_WAITING_FOR_STAGING, true);
        verify(mBackupRestoreJobScheduler)
                .schedule(
                        eq(mServiceContext), mJobInfoArgumentCaptor.capture(), eq(mBackupRestore));
        assertThat(mFakePreferenceHelper.getPreference(DATA_RESTORE_STATE_KEY))
                .isEqualTo(String.valueOf(INTERNAL_RESTORE_STATE_WAITING_FOR_STAGING));
        JobInfo jobInfo = mJobInfoArgumentCaptor.getValue();
        assertThat(jobInfo.getExtras().getString(EXTRA_JOB_NAME_KEY))
                .isEqualTo(DATA_STAGING_TIMEOUT_KEY);
    }

    @Test
    public void testSetInternalRestoreState_stagingInProgress_schedulesStagingTimeoutJob() {
        mFakePreferenceHelper.insertOrReplacePreference(
                DATA_RESTORE_STATE_KEY, String.valueOf(INTERNAL_RESTORE_STATE_STAGING_IN_PROGRESS));

        // forcing the state because we want to this state to set even when it's already set.
        mBackupRestore.setInternalRestoreState(INTERNAL_RESTORE_STATE_STAGING_IN_PROGRESS, true);
        verify(mBackupRestoreJobScheduler)
                .schedule(
                        eq(mServiceContext), mJobInfoArgumentCaptor.capture(), eq(mBackupRestore));
        assertThat(mFakePreferenceHelper.getPreference(DATA_RESTORE_STATE_KEY))
                .isEqualTo(String.valueOf(INTERNAL_RESTORE_STATE_STAGING_IN_PROGRESS));
        JobInfo jobInfo = mJobInfoArgumentCaptor.getValue();
        assertThat(jobInfo.getExtras().getString(EXTRA_JOB_NAME_KEY))
                .isEqualTo(DATA_STAGING_TIMEOUT_KEY);
    }

    @Test
    public void testSetInternalRestoreState_mergingInProgress_schedulesMergingTimeoutJob() {
        mFakePreferenceHelper.insertOrReplacePreference(
                DATA_RESTORE_STATE_KEY, String.valueOf(INTERNAL_RESTORE_STATE_MERGING_IN_PROGRESS));

        // forcing the state because we want to this state to set even when it's already set.
        mBackupRestore.setInternalRestoreState(INTERNAL_RESTORE_STATE_MERGING_IN_PROGRESS, true);
        verify(mBackupRestoreJobScheduler)
                .schedule(
                        eq(mServiceContext), mJobInfoArgumentCaptor.capture(), eq(mBackupRestore));
        assertThat(mFakePreferenceHelper.getPreference(DATA_RESTORE_STATE_KEY))
                .isEqualTo(String.valueOf(INTERNAL_RESTORE_STATE_MERGING_IN_PROGRESS));
        JobInfo jobInfo = mJobInfoArgumentCaptor.getValue();
        assertThat(jobInfo.getExtras().getString(EXTRA_JOB_NAME_KEY))
                .isEqualTo(DATA_MERGING_TIMEOUT_KEY);
    }

    @Test
    public void testScheduleAllPendingJobs_downloadStarted_schedulesDownloadTimeoutJob() {
        @HealthConnectManager.DataDownloadState int testDownloadStateSet = DATA_DOWNLOAD_STARTED;
        mFakePreferenceHelper.insertOrReplacePreference(
                DATA_DOWNLOAD_STATE_KEY, String.valueOf(testDownloadStateSet));

        mBackupRestore.scheduleAllJobs();
        verify(mBackupRestoreJobScheduler)
                .schedule(
                        eq(mServiceContext), mJobInfoArgumentCaptor.capture(), eq(mBackupRestore));
        JobInfo jobInfo = mJobInfoArgumentCaptor.getValue();
        assertThat(jobInfo.getExtras().getString(EXTRA_JOB_NAME_KEY))
                .isEqualTo(DATA_DOWNLOAD_TIMEOUT_KEY);
    }

    @Test
    public void testScheduleAllPendingJobs_downloadRetry_schedulesDownloadTimeoutJob() {
        @HealthConnectManager.DataDownloadState int testDownloadStateSet = DATA_DOWNLOAD_RETRY;
        mFakePreferenceHelper.insertOrReplacePreference(
                DATA_DOWNLOAD_STATE_KEY, String.valueOf(testDownloadStateSet));

        mBackupRestore.scheduleAllJobs();
        verify(mBackupRestoreJobScheduler)
                .schedule(
                        eq(mServiceContext), mJobInfoArgumentCaptor.capture(), eq(mBackupRestore));
        JobInfo jobInfo = mJobInfoArgumentCaptor.getValue();
        assertThat(jobInfo.getExtras().getString(EXTRA_JOB_NAME_KEY))
                .isEqualTo(DATA_DOWNLOAD_TIMEOUT_KEY);
    }

    @Test
    public void testDownloadTimeoutJob_downloadLatencyNotElapsed_usesDefaultLatency() {
        mFakePreferenceHelper.insertOrReplacePreference(
                DATA_DOWNLOAD_STATE_KEY, String.valueOf(DATA_DOWNLOAD_STARTED));

        mBackupRestore.scheduleAllJobs();
        verify(mBackupRestoreJobScheduler)
                .schedule(
                        eq(mServiceContext), mJobInfoArgumentCaptor.capture(), eq(mBackupRestore));
        JobInfo jobInfo = mJobInfoArgumentCaptor.getValue();

        assertThat(jobInfo.getMinLatencyMillis()).isEqualTo(DATA_DOWNLOAD_TIMEOUT_INTERVAL_MILLIS);
        assertThat(jobInfo.getExtras().getString(EXTRA_JOB_NAME_KEY))
                .isEqualTo(DATA_DOWNLOAD_TIMEOUT_KEY);
    }

    @Test
    public void testDownloadTimeoutJob_downloadLatencyElapsed_usesMinimumLatency() {
        Instant now = Instant.now();
        mFakePreferenceHelper.insertOrReplacePreference(
                DATA_DOWNLOAD_STATE_KEY, String.valueOf(DATA_DOWNLOAD_STARTED));
        mFakePreferenceHelper.insertOrReplacePreference(
                DATA_DOWNLOAD_TIMEOUT_KEY,
                String.valueOf(
                        now.minusMillis(DATA_DOWNLOAD_TIMEOUT_INTERVAL_MILLIS).toEpochMilli()));

        mBackupRestore.scheduleAllJobs();
        verify(mBackupRestoreJobScheduler)
                .schedule(
                        eq(mServiceContext), mJobInfoArgumentCaptor.capture(), eq(mBackupRestore));
        JobInfo jobInfo = mJobInfoArgumentCaptor.getValue();

        assertThat(jobInfo.getMinLatencyMillis()).isEqualTo(0);
        assertThat(jobInfo.getExtras().getString(EXTRA_JOB_NAME_KEY))
                .isEqualTo(DATA_DOWNLOAD_TIMEOUT_KEY);
    }

    @Test
    public void testScheduleAllPendingJobs_waitingForStaging_schedulesStagingTimeoutJob() {
        mFakePreferenceHelper.insertOrReplacePreference(
                DATA_RESTORE_STATE_KEY, String.valueOf(INTERNAL_RESTORE_STATE_WAITING_FOR_STAGING));

        mBackupRestore.scheduleAllJobs();
        verify(mBackupRestoreJobScheduler)
                .schedule(
                        eq(mServiceContext), mJobInfoArgumentCaptor.capture(), eq(mBackupRestore));
        JobInfo jobInfo = mJobInfoArgumentCaptor.getValue();
        assertThat(jobInfo.getExtras().getString(EXTRA_JOB_NAME_KEY))
                .isEqualTo(DATA_STAGING_TIMEOUT_KEY);
    }

    @Test
    public void testScheduleAllPendingJobs_stagingInProgress_schedulesStagingTimeoutJob() {
        mFakePreferenceHelper.insertOrReplacePreference(
                DATA_RESTORE_STATE_KEY, String.valueOf(INTERNAL_RESTORE_STATE_STAGING_IN_PROGRESS));

        mBackupRestore.scheduleAllJobs();
        verify(mBackupRestoreJobScheduler)
                .schedule(
                        eq(mServiceContext), mJobInfoArgumentCaptor.capture(), eq(mBackupRestore));
        JobInfo jobInfo = mJobInfoArgumentCaptor.getValue();
        assertThat(jobInfo.getExtras().getString(EXTRA_JOB_NAME_KEY))
                .isEqualTo(DATA_STAGING_TIMEOUT_KEY);
    }

    @Test
    public void testStagingTimeoutJob_stagingLatencyNotElapsed_usesStagingLatency() {
        mFakePreferenceHelper.insertOrReplacePreference(
                DATA_RESTORE_STATE_KEY, String.valueOf(INTERNAL_RESTORE_STATE_WAITING_FOR_STAGING));

        mBackupRestore.scheduleAllJobs();
        verify(mBackupRestoreJobScheduler)
                .schedule(
                        eq(mServiceContext), mJobInfoArgumentCaptor.capture(), eq(mBackupRestore));
        JobInfo jobInfo = mJobInfoArgumentCaptor.getValue();

        assertThat(jobInfo.getMinLatencyMillis()).isEqualTo(DATA_STAGING_TIMEOUT_INTERVAL_MILLIS);
        assertThat(jobInfo.getExtras().getString(EXTRA_JOB_NAME_KEY))
                .isEqualTo(DATA_STAGING_TIMEOUT_KEY);
    }

    @Test
    public void testStagingTimeoutJob_stagingLatencyElapsed_usesMinimumLatency() {
        Instant now = Instant.now();
        mFakePreferenceHelper.insertOrReplacePreference(
                DATA_RESTORE_STATE_KEY, String.valueOf(INTERNAL_RESTORE_STATE_WAITING_FOR_STAGING));
        mFakePreferenceHelper.insertOrReplacePreference(
                DATA_STAGING_TIMEOUT_KEY,
                String.valueOf(
                        now.minusMillis(DATA_STAGING_TIMEOUT_INTERVAL_MILLIS).toEpochMilli()));

        mBackupRestore.scheduleAllJobs();
        verify(mBackupRestoreJobScheduler)
                .schedule(
                        eq(mServiceContext), mJobInfoArgumentCaptor.capture(), eq(mBackupRestore));
        JobInfo jobInfo = mJobInfoArgumentCaptor.getValue();

        assertThat(jobInfo.getMinLatencyMillis()).isEqualTo(0);
        assertThat(jobInfo.getExtras().getString(EXTRA_JOB_NAME_KEY))
                .isEqualTo(DATA_STAGING_TIMEOUT_KEY);
    }

    @Test
    public void testScheduleAllPendingJobs_mergingInProgress_schedulesMergingTimeoutJob() {
        mFakePreferenceHelper.insertOrReplacePreference(
                DATA_RESTORE_STATE_KEY, String.valueOf(INTERNAL_RESTORE_STATE_MERGING_IN_PROGRESS));

        mBackupRestore.scheduleAllJobs();
        verify(mBackupRestoreJobScheduler, atLeastOnce())
                .schedule(
                        eq(mServiceContext), mJobInfoArgumentCaptor.capture(), eq(mBackupRestore));

        JobInfo jobInfo = findJob(mJobInfoArgumentCaptor.getAllValues(), DATA_MERGING_TIMEOUT_KEY);
        assertWithMessage("Merging timeout job not found").that(jobInfo).isNotNull();
    }

    @Test
    public void testScheduleAllTimeoutJobs_stagingDone_schedulesMergingJob() throws Exception {
        mFakePreferenceHelper.insertOrReplacePreference(
                DATA_RESTORE_STATE_KEY, String.valueOf(INTERNAL_RESTORE_STATE_STAGING_DONE));

        when(mTransactionManager.getDatabaseVersion()).thenReturn(1);

        createStagedDb(/* version= */ 1);

        mBackupRestore.scheduleAllJobs();

        mBackupRestore.scheduleAllJobs();
        verify(mBackupRestoreJobScheduler, atLeastOnce())
                .schedule(
                        eq(mServiceContext), mJobInfoArgumentCaptor.capture(), eq(mBackupRestore));

        JobInfo jobInfo = findJob(mJobInfoArgumentCaptor.getAllValues(), DATA_MERGING_KEY);
        assertWithMessage("Merging timeout job not found").that(jobInfo).isNotNull();
    }

    @Test
    public void testMergingTimeoutJob_mergingLatencyElapsed_usesMergingLatency() {
        mFakePreferenceHelper.insertOrReplacePreference(
                DATA_RESTORE_STATE_KEY, String.valueOf(INTERNAL_RESTORE_STATE_MERGING_IN_PROGRESS));

        mBackupRestore.scheduleAllJobs();
        verify(mBackupRestoreJobScheduler, atLeastOnce())
                .schedule(
                        eq(mServiceContext), mJobInfoArgumentCaptor.capture(), eq(mBackupRestore));

        JobInfo jobInfo = findJob(mJobInfoArgumentCaptor.getAllValues(), DATA_MERGING_TIMEOUT_KEY);
        assertWithMessage("Merging timeout job not found").that(jobInfo).isNotNull();
        assertThat(jobInfo.getMinLatencyMillis()).isEqualTo(DATA_MERGING_TIMEOUT_INTERVAL_MILLIS);
    }

    @Test
    public void testMergingTimeoutJob_mergingLatencyElapsed_usesMinimumLatency() {
        Instant now = Instant.now();
        mFakePreferenceHelper.insertOrReplacePreference(
                DATA_RESTORE_STATE_KEY, String.valueOf(INTERNAL_RESTORE_STATE_MERGING_IN_PROGRESS));
        mFakePreferenceHelper.insertOrReplacePreference(
                DATA_MERGING_TIMEOUT_KEY,
                String.valueOf(
                        now.minusMillis(DATA_MERGING_TIMEOUT_INTERVAL_MILLIS).toEpochMilli()));

        mBackupRestore.scheduleAllJobs();
        verify(mBackupRestoreJobScheduler, atLeastOnce())
                .schedule(
                        eq(mServiceContext), mJobInfoArgumentCaptor.capture(), eq(mBackupRestore));

        JobInfo jobInfo = findJob(mJobInfoArgumentCaptor.getAllValues(), DATA_MERGING_TIMEOUT_KEY);
        assertWithMessage("Merging timeout job not found").that(jobInfo).isNotNull();
        assertThat(jobInfo.getMinLatencyMillis()).isEqualTo(0);
    }

    @Test
    public void testScheduleAllTimeoutJobs_stagingDoneAndMigration_schedulesRetryMergingJob() {
        mFakePreferenceHelper.insertOrReplacePreference(
                DATA_RESTORE_STATE_KEY, String.valueOf(INTERNAL_RESTORE_STATE_STAGING_DONE));

        when(mMockMigrationStateManager.isMigrationInProgress()).thenReturn(true);

        // Schedule the first merge job (with 5 min delay)
        mBackupRestore.scheduleAllJobs();
        verify(mBackupRestoreJobScheduler, timeout(2000))
                .schedule(
                        eq(mServiceContext), mJobInfoArgumentCaptor.capture(), eq(mBackupRestore));
        JobInfo firstJobInfo = mJobInfoArgumentCaptor.getValue();

        // Run the first job, which will schedule the second one i.e. when migration in progress
        mBackupRestore.handleJob(firstJobInfo.getExtras());
        verify(mBackupRestoreJobScheduler, timeout(2000).times(2))
                .schedule(
                        eq(mServiceContext), mJobInfoArgumentCaptor.capture(), eq(mBackupRestore));
        JobInfo secondJobInfo = mJobInfoArgumentCaptor.getAllValues().get(1);

        assertThat(secondJobInfo.getExtras().getString(EXTRA_JOB_NAME_KEY))
                .isEqualTo(DATA_MERGING_KEY);
    }

    @Test
    public void testScheduleAllTimeoutJobs_mergingWithBugAndMigration_schedulesRetryMergingJob() {
        mFakePreferenceHelper.insertOrReplacePreference(
                DATA_RESTORE_STATE_KEY,
                String.valueOf(INTERNAL_RESTORE_STATE_MERGING_DONE_OLD_CODE));

        when(mMockMigrationStateManager.isMigrationInProgress()).thenReturn(true);

        // Schedule the first merge job (with 5 min delay)
        mBackupRestore.scheduleAllJobs();
        verify(mBackupRestoreJobScheduler, timeout(2000))
                .schedule(
                        eq(mServiceContext), mJobInfoArgumentCaptor.capture(), eq(mBackupRestore));
        JobInfo firstJobInfo = mJobInfoArgumentCaptor.getValue();

        // Run the first job, which will schedule the second one i.e. when migration in progress
        mBackupRestore.handleJob(firstJobInfo.getExtras());
        verify(mBackupRestoreJobScheduler, timeout(2000).times(2))
                .schedule(
                        eq(mServiceContext), mJobInfoArgumentCaptor.capture(), eq(mBackupRestore));
        JobInfo secondJobInfo = mJobInfoArgumentCaptor.getAllValues().get(1);

        assertThat(secondJobInfo.getExtras().getString(EXTRA_JOB_NAME_KEY))
                .isEqualTo(DATA_MERGING_KEY);
    }

    @Test
    public void testRetryMergingJob_alwaysUsesRetryMergeLatency() {
        mFakePreferenceHelper.insertOrReplacePreference(
                DATA_RESTORE_STATE_KEY, String.valueOf(INTERNAL_RESTORE_STATE_STAGING_DONE));

        when(mMockMigrationStateManager.isMigrationInProgress()).thenReturn(true);

        // Schedule the first merge job (with 5 min delay)
        mBackupRestore.scheduleAllJobs();
        verify(mBackupRestoreJobScheduler, timeout(2000))
                .schedule(
                        eq(mServiceContext), mJobInfoArgumentCaptor.capture(), eq(mBackupRestore));
        JobInfo firstJobInfo = mJobInfoArgumentCaptor.getValue();

        // Run the first job, which will schedule the second one i.e. when migration in progress
        mBackupRestore.handleJob(firstJobInfo.getExtras());
        verify(mBackupRestoreJobScheduler, timeout(2000).times(2))
                .schedule(
                        eq(mServiceContext), mJobInfoArgumentCaptor.capture(), eq(mBackupRestore));
        JobInfo secondJobInfo = mJobInfoArgumentCaptor.getAllValues().get(1);

        // Assert the retry job has the latency is bit below default retry latency
        // The difference between DATA_MERGING_RETRY_DELAY_MILLIS and min latency would be the time
        // taken by the merge job to schedule the retry (ideally couple of milliseconds)
        // Merge and retry are part of same job so it is merge itself is responsible for scheduling
        // a merge job when migration is in progress.
        assertThat(secondJobInfo.getMinLatencyMillis()).isLessThan(DATA_MERGING_RETRY_DELAY_MILLIS);
        assertThat(secondJobInfo.getMinLatencyMillis()).isGreaterThan(0);
        assertThat(secondJobInfo.getExtras().getString(EXTRA_JOB_NAME_KEY))
                .isEqualTo(DATA_MERGING_KEY);
    }

    @Test
    public void testMergingJob_usesDefaultLatency() {
        mFakePreferenceHelper.insertOrReplacePreference(
                DATA_RESTORE_STATE_KEY, String.valueOf(INTERNAL_RESTORE_STATE_STAGING_DONE));
        mFakePreferenceHelper.insertOrReplacePreference(
                DATA_MERGING_KEY, String.valueOf(Instant.now()));

        when(mMockMigrationStateManager.isMigrationInProgress()).thenReturn(true);

        // Schedule the first merge job (with 5 min delay)
        mBackupRestore.scheduleAllJobs();
        verify(mBackupRestoreJobScheduler, timeout(2000))
                .schedule(
                        eq(mServiceContext), mJobInfoArgumentCaptor.capture(), eq(mBackupRestore));
        JobInfo jobInfo = mJobInfoArgumentCaptor.getValue();

        assertThat(jobInfo.getMinLatencyMillis()).isEqualTo(DATA_MERGING_DELAY_MILLIS);
        assertThat(jobInfo.getExtras().getString(EXTRA_JOB_NAME_KEY)).isEqualTo(DATA_MERGING_KEY);
    }

    @Test
    public void testRetryMergingJob_retryLatencyElapsed_usesMinimumLatency() {
        Instant now = Instant.now();
        mFakePreferenceHelper.insertOrReplacePreference(
                DATA_RESTORE_STATE_KEY, String.valueOf(INTERNAL_RESTORE_STATE_STAGING_DONE));
        mFakePreferenceHelper.insertOrReplacePreference(
                DATA_MERGING_KEY,
                String.valueOf(
                        now.minusMillis(DATA_MERGING_TIMEOUT_INTERVAL_MILLIS).toEpochMilli()));

        when(mMockMigrationStateManager.isMigrationInProgress()).thenReturn(true);

        // Call merge directly to avoid reset of merge delay clock and ensure that the retry latency
        // has elapsed. Scheduling merge again would reset the merge latency.
        mBackupRestore.merge();
        verify(mBackupRestoreJobScheduler, timeout(2000).times(1))
                .schedule(
                        eq(mServiceContext), mJobInfoArgumentCaptor.capture(), eq(mBackupRestore));
        JobInfo jobInfo = mJobInfoArgumentCaptor.getValue();

        assertThat(jobInfo.getMinLatencyMillis()).isEqualTo(0);
        assertThat(jobInfo.getExtras().getString(EXTRA_JOB_NAME_KEY)).isEqualTo(DATA_MERGING_KEY);
    }

    @Test
    public void testCancelAllJobs_cancelsAllJobs() {
        mBackupRestore.cancelAllJobs();
        verify(mBackupRestoreJobScheduler).cancelAllJobs(eq(mServiceContext));
    }

    @Test
    public void testOnStartJob_forDownloadStartedJob_executesDownloadJob() {
        mFakePreferenceHelper.insertOrReplacePreference(
                DATA_DOWNLOAD_STATE_KEY, String.valueOf(DATA_DOWNLOAD_STARTED));
        mFakePreferenceHelper.insertOrReplacePreference(
                DATA_DOWNLOAD_TIMEOUT_KEY, String.valueOf(Instant.now().toEpochMilli()));

        PersistableBundle extras = new PersistableBundle();
        extras.putString(EXTRA_JOB_NAME_KEY, DATA_DOWNLOAD_TIMEOUT_KEY);
        mBackupRestore.handleJob(extras);

        assertThat(mFakePreferenceHelper.getPreference(DATA_DOWNLOAD_STATE_KEY))
                .isEqualTo(String.valueOf(DATA_DOWNLOAD_FAILED));
        assertThat(mFakePreferenceHelper.getPreference(DATA_RESTORE_ERROR_KEY))
                .isEqualTo(String.valueOf(RESTORE_ERROR_FETCHING_DATA));
        assertThat(mFakePreferenceHelper.getPreference(DATA_DOWNLOAD_TIMEOUT_KEY)).isEqualTo("");
        assertThat(mFakePreferenceHelper.getPreference(DATA_DOWNLOAD_TIMEOUT_CANCELLED_KEY))
                .isEqualTo("");
    }

    @Test
    public void testOnStartJob_forDownloadRetryJob_executesDownloadJob() {
        mFakePreferenceHelper.insertOrReplacePreference(
                DATA_DOWNLOAD_STATE_KEY, String.valueOf(DATA_DOWNLOAD_RETRY));
        mFakePreferenceHelper.insertOrReplacePreference(
                DATA_DOWNLOAD_TIMEOUT_KEY, String.valueOf(Instant.now().toEpochMilli()));

        PersistableBundle extras = new PersistableBundle();
        extras.putString(EXTRA_JOB_NAME_KEY, DATA_DOWNLOAD_TIMEOUT_KEY);
        mBackupRestore.handleJob(extras);

        assertThat(mFakePreferenceHelper.getPreference(DATA_DOWNLOAD_STATE_KEY))
                .isEqualTo(String.valueOf(DATA_DOWNLOAD_FAILED));
        assertThat(mFakePreferenceHelper.getPreference(DATA_RESTORE_ERROR_KEY))
                .isEqualTo(String.valueOf(RESTORE_ERROR_FETCHING_DATA));
        assertThat(mFakePreferenceHelper.getPreference(DATA_DOWNLOAD_TIMEOUT_KEY)).isEqualTo("");
        assertThat(mFakePreferenceHelper.getPreference(DATA_DOWNLOAD_TIMEOUT_CANCELLED_KEY))
                .isEqualTo("");
    }

    @Test
    public void testOnStartJob_forWaitingStagingJob_executesStagingJob() {
        mFakePreferenceHelper.insertOrReplacePreference(
                DATA_RESTORE_STATE_KEY, String.valueOf(INTERNAL_RESTORE_STATE_WAITING_FOR_STAGING));
        mFakePreferenceHelper.insertOrReplacePreference(
                DATA_STAGING_TIMEOUT_KEY, String.valueOf(Instant.now().toEpochMilli()));

        PersistableBundle extras = new PersistableBundle();
        extras.putString(EXTRA_JOB_NAME_KEY, DATA_STAGING_TIMEOUT_KEY);
        mBackupRestore.handleJob(extras);

        assertThat(mFakePreferenceHelper.getPreference(DATA_RESTORE_STATE_KEY))
                .isEqualTo(String.valueOf(INTERNAL_RESTORE_STATE_MERGING_DONE));
        assertThat(mFakePreferenceHelper.getPreference(DATA_RESTORE_ERROR_KEY))
                .isEqualTo(String.valueOf(RESTORE_ERROR_UNKNOWN));
        assertThat(mFakePreferenceHelper.getPreference(DATA_STAGING_TIMEOUT_KEY)).isEqualTo("");
        assertThat(mFakePreferenceHelper.getPreference(DATA_STAGING_TIMEOUT_CANCELLED_KEY))
                .isEqualTo("");
    }

    @Test
    public void testOnStartJob_forStagingProgressJob_executesStagingJob() {
        mFakePreferenceHelper.insertOrReplacePreference(
                DATA_RESTORE_STATE_KEY, String.valueOf(INTERNAL_RESTORE_STATE_STAGING_IN_PROGRESS));
        mFakePreferenceHelper.insertOrReplacePreference(
                DATA_STAGING_TIMEOUT_KEY, String.valueOf(Instant.now().toEpochMilli()));

        PersistableBundle extras = new PersistableBundle();
        extras.putString(EXTRA_JOB_NAME_KEY, DATA_STAGING_TIMEOUT_KEY);
        mBackupRestore.handleJob(extras);

        assertThat(mFakePreferenceHelper.getPreference(DATA_RESTORE_STATE_KEY))
                .isEqualTo(String.valueOf(INTERNAL_RESTORE_STATE_MERGING_DONE));
        assertThat(mFakePreferenceHelper.getPreference(DATA_RESTORE_ERROR_KEY))
                .isEqualTo(String.valueOf(RESTORE_ERROR_UNKNOWN));
        assertThat(mFakePreferenceHelper.getPreference(DATA_STAGING_TIMEOUT_KEY)).isEqualTo("");
        assertThat(mFakePreferenceHelper.getPreference(DATA_STAGING_TIMEOUT_CANCELLED_KEY))
                .isEqualTo("");
    }

    @Test
    public void testOnStartJob_forMergingProgressJob_executesMergingJob() {
        mFakePreferenceHelper.insertOrReplacePreference(
                DATA_RESTORE_STATE_KEY, String.valueOf(INTERNAL_RESTORE_STATE_MERGING_IN_PROGRESS));
        mFakePreferenceHelper.insertOrReplacePreference(
                DATA_STAGING_TIMEOUT_KEY, String.valueOf(Instant.now().toEpochMilli()));

        PersistableBundle extras = new PersistableBundle();
        extras.putString(EXTRA_JOB_NAME_KEY, DATA_MERGING_TIMEOUT_KEY);
        mBackupRestore.handleJob(extras);

        assertThat(mFakePreferenceHelper.getPreference(DATA_RESTORE_STATE_KEY))
                .isEqualTo(String.valueOf(INTERNAL_RESTORE_STATE_MERGING_DONE));
        assertThat(mFakePreferenceHelper.getPreference(DATA_RESTORE_ERROR_KEY))
                .isEqualTo(String.valueOf(RESTORE_ERROR_UNKNOWN));
        assertThat(mFakePreferenceHelper.getPreference(DATA_MERGING_TIMEOUT_KEY)).isEqualTo("");
        assertThat(mFakePreferenceHelper.getPreference(DATA_MERGING_TIMEOUT_CANCELLED_KEY))
                .isEqualTo("");
    }

    @Test
    public void testMerge_restoreStateIsIdle() throws Exception {
        mFakePreferenceHelper.insertOrReplacePreference(
                DATA_RESTORE_STATE_KEY, String.valueOf(INTERNAL_RESTORE_STATE_STAGING_DONE));
        when(mTransactionManager.getDatabaseVersion()).thenReturn(1);
        createStagedDb(/* version= */ 1);

        mBackupRestore.merge();

        assertThat(mBackupRestore.getDataRestoreState()).isEqualTo(RESTORE_STATE_IDLE);
        assertThat(mBackupRestore.getDataRestoreError()).isEqualTo(RESTORE_ERROR_NONE);
    }

    @Test
    public void testMerge_mergingOfGrantTimesIsInvoked() throws Exception {
        mFakePreferenceHelper.insertOrReplacePreference(
                DATA_RESTORE_STATE_KEY, String.valueOf(INTERNAL_RESTORE_STATE_STAGING_DONE));
        when(mTransactionManager.getDatabaseVersion()).thenReturn(1);
        UserGrantTimeState userGrantTimeState =
                new UserGrantTimeState(Map.of("package", Instant.now()), Map.of(), 1);
        when(mFirstGrantTimeManager.getGrantTimeStateForUser(mUserHandle))
                .thenReturn(userGrantTimeState);
        doReturn(userGrantTimeState).when(mGrantTimeXmlHelper).parseGrantTime(any());

        createStagedDb(/* version= */ 1);

        mBackupRestore.merge();

        verify(mFirstGrantTimeManager).applyAndStageGrantTimeStateForUser(eq(mUserHandle), any());
    }

    @Test
    public void testMerge_mergingOfGrantTimes_parsesRestoredGrantTimes() throws Exception {
        ArgumentCaptor<File> restoredGrantTimeFileCaptor = ArgumentCaptor.forClass(File.class);

        mFakePreferenceHelper.insertOrReplacePreference(
                DATA_RESTORE_STATE_KEY, String.valueOf(INTERNAL_RESTORE_STATE_STAGING_DONE));
        when(mTransactionManager.getDatabaseVersion()).thenReturn(1);
        createStagedDb(/* version= */ 1);

        mBackupRestore.merge();
        verify(mGrantTimeXmlHelper).parseGrantTime(restoredGrantTimeFileCaptor.capture());
        assertThat(restoredGrantTimeFileCaptor.getValue()).isNotNull();
        assertThat(restoredGrantTimeFileCaptor.getValue().getName())
                .isEqualTo(GRANT_TIME_FILE_NAME);
    }

    @Test
    public void testMerge_whenModuleVersionBehind_setsVersionDiffError() throws IOException {
        mFakePreferenceHelper.insertOrReplacePreference(
                DATA_RESTORE_STATE_KEY, String.valueOf(INTERNAL_RESTORE_STATE_STAGING_DONE));
        when(mTransactionManager.getDatabaseVersion()).thenReturn(1);
        createStagedDb(/* version= */ 2);

        mBackupRestore.merge();

        assertThat(mBackupRestore.getDataRestoreError()).isEqualTo(RESTORE_ERROR_VERSION_DIFF);
        verify(mFirstGrantTimeManager, never())
                .applyAndStageGrantTimeStateForUser(eq(mUserHandle), any());
    }

    @Test
    public void testMerge_whenMigrationInProgress_schedulesRetryMergingJob() {
        mFakePreferenceHelper.insertOrReplacePreference(
                DATA_RESTORE_STATE_KEY, String.valueOf(INTERNAL_RESTORE_STATE_STAGING_DONE));
        mFakePreferenceHelper.insertOrReplacePreference(
                DATA_MERGING_TIMEOUT_KEY, String.valueOf(Instant.now().toEpochMilli()));
        when(mTransactionManager.getDatabaseVersion()).thenReturn(1);

        when(mMockMigrationStateManager.isMigrationInProgress()).thenReturn(true);

        mBackupRestore.merge();
        verify(mBackupRestoreJobScheduler, timeout(2000))
                .schedule(
                        eq(mServiceContext), mJobInfoArgumentCaptor.capture(), eq(mBackupRestore));
        JobInfo jobInfo = mJobInfoArgumentCaptor.getValue();
        assertThat(jobInfo.getExtras().getString(EXTRA_JOB_NAME_KEY)).isEqualTo(DATA_MERGING_KEY);
        verify(mFirstGrantTimeManager, never())
                .applyAndStageGrantTimeStateForUser(eq(mUserHandle), any());
    }

    @Test
    public void testShouldAttemptMerging_whenInStagingDone_returnsTrue() {
        mFakePreferenceHelper.insertOrReplacePreference(
                DATA_RESTORE_STATE_KEY, String.valueOf(INTERNAL_RESTORE_STATE_STAGING_DONE));

        boolean result = mBackupRestore.shouldAttemptMerging();
        assertThat(result).isTrue();
    }

    @Test
    public void testShouldAttemptMerging_whenInMergingProgress_returnsTrue() {
        mFakePreferenceHelper.insertOrReplacePreference(
                DATA_RESTORE_STATE_KEY, String.valueOf(INTERNAL_RESTORE_STATE_MERGING_IN_PROGRESS));

        boolean result = mBackupRestore.shouldAttemptMerging();
        assertThat(result).isTrue();
    }

    @Test
    public void testShouldAttemptMerging_whenInMergingDoneWithBug_returnsTrue() {
        mFakePreferenceHelper.insertOrReplacePreference(
                DATA_RESTORE_STATE_KEY,
                String.valueOf(INTERNAL_RESTORE_STATE_MERGING_DONE_OLD_CODE));

        boolean result = mBackupRestore.shouldAttemptMerging();
        assertThat(result).isTrue();
    }

    @Test
    public void testShouldAttemptMerging_whenInMergingDone_returnsFalse() {
        mFakePreferenceHelper.insertOrReplacePreference(
                DATA_RESTORE_STATE_KEY, String.valueOf(INTERNAL_RESTORE_STATE_MERGING_DONE));

        boolean result = mBackupRestore.shouldAttemptMerging();
        assertThat(result).isFalse();
    }

    @Test
    public void testShouldAttemptMerging_whenInStagingProgress_returnsFalse() {
        mFakePreferenceHelper.insertOrReplacePreference(
                DATA_RESTORE_STATE_KEY, String.valueOf(INTERNAL_RESTORE_STATE_STAGING_IN_PROGRESS));

        boolean result = mBackupRestore.shouldAttemptMerging();
        assertThat(result).isFalse();
    }

    @Nullable
    private static JobInfo findJob(List<JobInfo> jobInfos, String jobKey) {
        for (JobInfo jobInfo : jobInfos) {
            if (jobKey.equals(jobInfo.getExtras().getString(EXTRA_JOB_NAME_KEY))) {
                return jobInfo;
            }
        }
        return null;
    }

    private static File createAndGetNonEmptyFile(File dir, String fileName) throws IOException {
        File file = new File(dir, fileName);
        FileWriter fileWriter = new FileWriter(file);
        fileWriter.write("Contents of file " + fileName);
        fileWriter.close();
        return file;
    }

    private static File createAndGetEmptyFile(File dir, String fileName) throws IOException {
        dir.mkdirs();
        File file = new File(dir, fileName);
        file.createNewFile();
        return file;
    }

    private File createStagedDb(int version) throws IOException {
        File hcDirectory =
                FilesUtil.getDataSystemCeHCDirectoryForUser(
                        mEnvironmentDataDirectory.getRoot(), mUserHandle.getIdentifier());
        File dir = new File(hcDirectory, STAGED_DATABASE_DIR);
        dir.mkdirs();
        File dbFile = new File(dir, STAGED_DATABASE_NAME);
        try (SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(dbFile, null)) {
            db.setVersion(version);
        }
        return dbFile;
    }
}
