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
import static android.health.connect.HealthConnectDataState.RESTORE_ERROR_UNKNOWN;
import static android.health.connect.HealthConnectDataState.RESTORE_ERROR_VERSION_DIFF;
import static android.health.connect.HealthConnectDataState.RESTORE_STATE_IDLE;
import static android.health.connect.HealthConnectManager.DATA_DOWNLOAD_FAILED;
import static android.health.connect.HealthConnectManager.DATA_DOWNLOAD_RETRY;
import static android.health.connect.HealthConnectManager.DATA_DOWNLOAD_STARTED;

import static com.android.compatibility.common.util.SystemUtil.eventually;
import static com.android.server.healthconnect.backuprestore.BackupRestore.BackupRestoreJobService.BACKUP_RESTORE_JOBS_NAMESPACE;
import static com.android.server.healthconnect.backuprestore.BackupRestore.BackupRestoreJobService.EXTRA_JOB_NAME_KEY;
import static com.android.server.healthconnect.backuprestore.BackupRestore.DATA_DOWNLOAD_STATE_KEY;
import static com.android.server.healthconnect.backuprestore.BackupRestore.DATA_DOWNLOAD_TIMEOUT_CANCELLED_KEY;
import static com.android.server.healthconnect.backuprestore.BackupRestore.DATA_DOWNLOAD_TIMEOUT_INTERVAL_MILLIS;
import static com.android.server.healthconnect.backuprestore.BackupRestore.DATA_DOWNLOAD_TIMEOUT_KEY;
import static com.android.server.healthconnect.backuprestore.BackupRestore.DATA_MERGING_RETRY_DELAY_MILLIS;
import static com.android.server.healthconnect.backuprestore.BackupRestore.DATA_MERGING_RETRY_KEY;
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
import static com.android.server.healthconnect.backuprestore.BackupRestore.MINIMUM_LATENCY_WINDOW_MILLIS;
import static com.android.server.healthconnect.backuprestore.BackupRestore.STAGED_DATABASE_DIR;
import static com.android.server.healthconnect.backuprestore.BackupRestore.STAGED_DATABASE_NAME;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.annotation.Nullable;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.health.connect.HealthConnectManager;
import android.health.connect.restore.BackupFileNamesSet;
import android.health.connect.restore.StageRemoteDataRequest;
import android.os.ParcelFileDescriptor;
import android.os.PersistableBundle;
import android.os.UserHandle;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.util.ArrayMap;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.dx.mockito.inline.extended.ExtendedMockito;
import com.android.healthfitness.flags.Flags;
import com.android.modules.utils.testing.ExtendedMockitoRule;
import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;
import com.android.server.healthconnect.migration.MigrationStateManager;
import com.android.server.healthconnect.permission.FirstGrantTimeManager;
import com.android.server.healthconnect.permission.GrantTimeXmlHelper;
import com.android.server.healthconnect.permission.HealthPermissionIntentAppsTracker;
import com.android.server.healthconnect.permission.UserGrantTimeState;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.datatypehelpers.PreferenceHelper;
import com.android.server.healthconnect.testing.fakes.FakePreferenceHelper;
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
import org.mockito.quality.Strictness;

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

    @Rule(order = 2)
    public final ExtendedMockitoRule mExtendedMockitoRule =
            new ExtendedMockitoRule.Builder(this)
                    .mockStatic(SQLiteDatabase.class)
                    .spyStatic(GrantTimeXmlHelper.class)
                    .setStrictness(Strictness.LENIENT)
                    .build();

    @Rule public final TemporaryFolder mEnvironmentDataDirectory = new TemporaryFolder();

    @Mock Context mServiceContext;
    @Mock private TransactionManager mTransactionManager;
    @Mock private FirstGrantTimeManager mFirstGrantTimeManager;
    // TODO(b/373322447): Remove the mock HealthPermissionIntentAppsTracker
    @Mock private HealthPermissionIntentAppsTracker mPermissionIntentAppsTracker;
    @Mock private MigrationStateManager mMockMigrationStateManager;
    @Mock private Context mContext;
    @Mock private JobScheduler mJobScheduler;
    @Mock private BackupRestore.BackupRestoreJobScheduler mBackupRestoreJobScheduler;
    @Captor ArgumentCaptor<JobInfo> mJobInfoArgumentCaptor;
    private BackupRestore mBackupRestore;
    private final PreferenceHelper mFakePreferenceHelper = new FakePreferenceHelper();
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

        HealthConnectInjector healthConnectInjector =
                HealthConnectInjectorImpl.newBuilderForTest(mContext)
                        .setPreferenceHelper(mFakePreferenceHelper)
                        .setMigrationStateManager(mMockMigrationStateManager)
                        .setFirstGrantTimeManager(mFirstGrantTimeManager)
                        .setHealthPermissionIntentAppsTracker(mPermissionIntentAppsTracker)
                        .setTransactionManager(mTransactionManager)
                        .setEnvironmentDataDirectory(mEnvironmentDataDirectory.getRoot())
                        .build();

        mBackupRestore =
                new BackupRestore(
                        healthConnectInjector.getAppInfoHelper(),
                        mFirstGrantTimeManager,
                        healthConnectInjector.getMigrationStateManager(),
                        healthConnectInjector.getPreferenceHelper(),
                        healthConnectInjector.getTransactionManager(),
                        healthConnectInjector.getFitnessRecordReadHelper(),
                        mServiceContext,
                        healthConnectInjector.getDeviceInfoHelper(),
                        healthConnectInjector.getHealthDataCategoryPriorityHelper(),
                        healthConnectInjector.getThreadScheduler(),
                        healthConnectInjector.getEnvironmentDataDirectory(),
                        mBackupRestoreJobScheduler);
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
    @DisableFlags({Flags.FLAG_PERSONAL_HEALTH_RECORD_DISABLE_D2D})
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
        assertThat(GrantTimeXmlHelper.parseGrantTime(grantTimeFileBacked).toString())
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
        assertThat(jobInfo.getMaxExecutionDelayMillis())
                .isEqualTo(DATA_DOWNLOAD_TIMEOUT_INTERVAL_MILLIS + MINIMUM_LATENCY_WINDOW_MILLIS);
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
        assertThat(jobInfo.getMaxExecutionDelayMillis()).isEqualTo(MINIMUM_LATENCY_WINDOW_MILLIS);
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
        assertThat(jobInfo.getMaxExecutionDelayMillis())
                .isEqualTo(DATA_STAGING_TIMEOUT_INTERVAL_MILLIS + MINIMUM_LATENCY_WINDOW_MILLIS);
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
        assertThat(jobInfo.getMaxExecutionDelayMillis()).isEqualTo(MINIMUM_LATENCY_WINDOW_MILLIS);
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

        JobInfo jobInfo = findMergeTimeoutJob(mJobInfoArgumentCaptor.getAllValues());
        assertWithMessage("Merging timeout job not found").that(jobInfo).isNotNull();
    }

    @Test
    public void testScheduleAllTimeoutJobs_stagingDone_triggersMergingJob() throws Exception {
        mFakePreferenceHelper.insertOrReplacePreference(
                DATA_RESTORE_STATE_KEY, String.valueOf(INTERNAL_RESTORE_STATE_STAGING_DONE));

        when(mTransactionManager.getDatabaseVersion()).thenReturn(1);

        SQLiteDatabase mockDb = mock(SQLiteDatabase.class);
        when(mockDb.getVersion()).thenReturn(1);
        when(SQLiteDatabase.openDatabase(any(), any())).thenReturn(mockDb);

        mBackupRestore.scheduleAllJobs();

        eventually(
                () ->
                        assertThat(mFakePreferenceHelper.getPreference(DATA_RESTORE_STATE_KEY))
                                .isEqualTo(String.valueOf(INTERNAL_RESTORE_STATE_MERGING_DONE)));
    }

    @Test
    public void testMergingTimeoutJob_mergingLatencyElapsed_usesMergingLatency() {
        mFakePreferenceHelper.insertOrReplacePreference(
                DATA_RESTORE_STATE_KEY, String.valueOf(INTERNAL_RESTORE_STATE_MERGING_IN_PROGRESS));

        mBackupRestore.scheduleAllJobs();
        verify(mBackupRestoreJobScheduler, atLeastOnce())
                .schedule(
                        eq(mServiceContext), mJobInfoArgumentCaptor.capture(), eq(mBackupRestore));

        JobInfo jobInfo = findMergeTimeoutJob(mJobInfoArgumentCaptor.getAllValues());
        assertWithMessage("Merging timeout job not found").that(jobInfo).isNotNull();
        assertThat(jobInfo.getMinLatencyMillis()).isEqualTo(DATA_MERGING_TIMEOUT_INTERVAL_MILLIS);
        assertThat(jobInfo.getMaxExecutionDelayMillis())
                .isEqualTo(DATA_MERGING_TIMEOUT_INTERVAL_MILLIS + MINIMUM_LATENCY_WINDOW_MILLIS);
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

        JobInfo jobInfo = findMergeTimeoutJob(mJobInfoArgumentCaptor.getAllValues());
        assertWithMessage("Merging timeout job not found").that(jobInfo).isNotNull();
        assertThat(jobInfo.getMinLatencyMillis()).isEqualTo(0);
        assertThat(jobInfo.getMaxExecutionDelayMillis()).isEqualTo(MINIMUM_LATENCY_WINDOW_MILLIS);
    }

    @Test
    public void testScheduleAllTimeoutJobs_stagingDoneAndMigration_schedulesRetryMergingJob() {
        mFakePreferenceHelper.insertOrReplacePreference(
                DATA_RESTORE_STATE_KEY, String.valueOf(INTERNAL_RESTORE_STATE_STAGING_DONE));

        when(mMockMigrationStateManager.isMigrationInProgress()).thenReturn(true);

        mBackupRestore.scheduleAllJobs();
        verify(mBackupRestoreJobScheduler, timeout(2000))
                .schedule(
                        eq(mServiceContext), mJobInfoArgumentCaptor.capture(), eq(mBackupRestore));
        JobInfo jobInfo = mJobInfoArgumentCaptor.getValue();
        assertThat(jobInfo.getExtras().getString(EXTRA_JOB_NAME_KEY))
                .isEqualTo(DATA_MERGING_RETRY_KEY);
    }

    @Test
    public void testScheduleAllTimeoutJobs_mergingWithBugAndMigration_schedulesRetryMergingJob() {
        mFakePreferenceHelper.insertOrReplacePreference(
                DATA_RESTORE_STATE_KEY,
                String.valueOf(INTERNAL_RESTORE_STATE_MERGING_DONE_OLD_CODE));

        when(mMockMigrationStateManager.isMigrationInProgress()).thenReturn(true);

        mBackupRestore.scheduleAllJobs();
        verify(mBackupRestoreJobScheduler, timeout(2000))
                .schedule(
                        eq(mServiceContext), mJobInfoArgumentCaptor.capture(), eq(mBackupRestore));
        JobInfo jobInfo = mJobInfoArgumentCaptor.getValue();
        assertThat(jobInfo.getExtras().getString(EXTRA_JOB_NAME_KEY))
                .isEqualTo(DATA_MERGING_RETRY_KEY);
    }

    @Test
    public void testRetryMergingTimeoutJob_retryLatencyElapsed_usesRetryLatency() {
        mFakePreferenceHelper.insertOrReplacePreference(
                DATA_RESTORE_STATE_KEY, String.valueOf(INTERNAL_RESTORE_STATE_STAGING_DONE));

        when(mMockMigrationStateManager.isMigrationInProgress()).thenReturn(true);

        mBackupRestore.scheduleAllJobs();
        verify(mBackupRestoreJobScheduler, timeout(2000))
                .schedule(
                        eq(mServiceContext), mJobInfoArgumentCaptor.capture(), eq(mBackupRestore));
        JobInfo jobInfo = mJobInfoArgumentCaptor.getValue();

        assertThat(jobInfo.getMinLatencyMillis()).isEqualTo(DATA_MERGING_RETRY_DELAY_MILLIS);
        assertThat(jobInfo.getMaxExecutionDelayMillis())
                .isEqualTo(DATA_MERGING_RETRY_DELAY_MILLIS + MINIMUM_LATENCY_WINDOW_MILLIS);
        assertThat(jobInfo.getExtras().getString(EXTRA_JOB_NAME_KEY))
                .isEqualTo(DATA_MERGING_RETRY_KEY);
    }

    @Test
    public void testRetryMergingTimeoutJob_retryLatencyElapsed_usesMinimumLatency() {
        Instant now = Instant.now();
        mFakePreferenceHelper.insertOrReplacePreference(
                DATA_RESTORE_STATE_KEY, String.valueOf(INTERNAL_RESTORE_STATE_STAGING_DONE));
        mFakePreferenceHelper.insertOrReplacePreference(
                DATA_MERGING_RETRY_KEY,
                String.valueOf(
                        now.minusMillis(DATA_MERGING_TIMEOUT_INTERVAL_MILLIS).toEpochMilli()));

        when(mMockMigrationStateManager.isMigrationInProgress()).thenReturn(true);

        mBackupRestore.scheduleAllJobs();
        verify(mBackupRestoreJobScheduler, timeout(2000))
                .schedule(
                        eq(mServiceContext), mJobInfoArgumentCaptor.capture(), eq(mBackupRestore));
        JobInfo jobInfo = mJobInfoArgumentCaptor.getValue();

        assertThat(jobInfo.getMinLatencyMillis()).isEqualTo(0);
        assertThat(jobInfo.getMaxExecutionDelayMillis()).isEqualTo(MINIMUM_LATENCY_WINDOW_MILLIS);
        assertThat(jobInfo.getExtras().getString(EXTRA_JOB_NAME_KEY))
                .isEqualTo(DATA_MERGING_RETRY_KEY);
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
    public void testMerge_restoreStateIsIdle() {
        mFakePreferenceHelper.insertOrReplacePreference(
                DATA_RESTORE_STATE_KEY, String.valueOf(INTERNAL_RESTORE_STATE_STAGING_DONE));
        when(mTransactionManager.getDatabaseVersion()).thenReturn(1);

        SQLiteDatabase mockDb = mock(SQLiteDatabase.class);
        when(mockDb.getVersion()).thenReturn(1);
        when(SQLiteDatabase.openDatabase(any(), any())).thenReturn(mockDb);

        mBackupRestore.merge();
        assertThat(mBackupRestore.getDataRestoreState()).isEqualTo(RESTORE_STATE_IDLE);
    }

    @Test
    public void testMerge_mergingOfGrantTimesIsInvoked() {
        mFakePreferenceHelper.insertOrReplacePreference(
                DATA_RESTORE_STATE_KEY, String.valueOf(INTERNAL_RESTORE_STATE_STAGING_DONE));
        when(mTransactionManager.getDatabaseVersion()).thenReturn(1);

        SQLiteDatabase mockDb = mock(SQLiteDatabase.class);
        when(mockDb.getVersion()).thenReturn(1);
        when(SQLiteDatabase.openDatabase(any(), any())).thenReturn(mockDb);

        mBackupRestore.merge();
        verify(mFirstGrantTimeManager).applyAndStageGrantTimeStateForUser(eq(mUserHandle), any());
    }

    @Test
    public void testMerge_mergingOfGrantTimes_parsesRestoredGrantTimes() {
        ArgumentCaptor<File> restoredGrantTimeFileCaptor = ArgumentCaptor.forClass(File.class);

        mFakePreferenceHelper.insertOrReplacePreference(
                DATA_RESTORE_STATE_KEY, String.valueOf(INTERNAL_RESTORE_STATE_STAGING_DONE));
        when(mTransactionManager.getDatabaseVersion()).thenReturn(1);

        SQLiteDatabase mockDb = mock(SQLiteDatabase.class);
        when(mockDb.getVersion()).thenReturn(1);
        when(SQLiteDatabase.openDatabase(any(), any())).thenReturn(mockDb);

        mBackupRestore.merge();
        ExtendedMockito.verify(
                () -> GrantTimeXmlHelper.parseGrantTime(restoredGrantTimeFileCaptor.capture()));
        assertThat(restoredGrantTimeFileCaptor.getValue()).isNotNull();
        assertThat(restoredGrantTimeFileCaptor.getValue().getName())
                .isEqualTo(GRANT_TIME_FILE_NAME);
    }

    @Test
    public void testMerge_whenModuleVersionBehind_setsVersionDiffError() throws IOException {
        mFakePreferenceHelper.insertOrReplacePreference(
                DATA_RESTORE_STATE_KEY, String.valueOf(INTERNAL_RESTORE_STATE_STAGING_DONE));
        when(mTransactionManager.getDatabaseVersion()).thenReturn(1);

        SQLiteDatabase mockDb = mock(SQLiteDatabase.class);
        when(mockDb.getVersion()).thenReturn(2);
        when(SQLiteDatabase.openDatabase(any(), any())).thenReturn(mockDb);

        File hcDirectory =
                FilesUtil.getDataSystemCeHCDirectoryForUser(
                        mEnvironmentDataDirectory.getRoot(), mUserHandle.getIdentifier());
        File databaseDir = new File(hcDirectory, STAGED_DATABASE_DIR);
        createAndGetEmptyFile(databaseDir, STAGED_DATABASE_NAME);

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
        assertThat(jobInfo.getExtras().getString(EXTRA_JOB_NAME_KEY))
                .isEqualTo(DATA_MERGING_RETRY_KEY);
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
    private static JobInfo findMergeTimeoutJob(List<JobInfo> jobInfos) {
        for (JobInfo jobInfo : jobInfos) {
            if (DATA_MERGING_TIMEOUT_KEY.equals(
                    jobInfo.getExtras().getString(EXTRA_JOB_NAME_KEY))) {
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
}
