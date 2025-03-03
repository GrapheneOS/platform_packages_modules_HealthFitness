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

package com.android.server.healthconnect.exportimport;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.health.connect.exportimport.ScheduledExportSettings;
import android.net.Uri;
import android.os.PersistableBundle;
import android.os.UserHandle;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.healthfitness.flags.Flags;
import com.android.server.healthconnect.storage.ExportImportSettingsStorage;
import com.android.server.healthconnect.storage.datatypehelpers.PreferenceHelper;
import com.android.server.healthconnect.testing.fakes.FakePreferenceHelper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class ExportImportJobsTest {

    private static final String ANDROID_SERVER_PACKAGE_NAME = "com.android.server";

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Mock Context mContext;
    @Mock private JobScheduler mJobScheduler;
    @Mock private ExportManager mExportManager;

    @Captor ArgumentCaptor<JobInfo> mJobInfoCaptor;
    private final PreferenceHelper mFakePreferenceHelper = new FakePreferenceHelper();

    private ExportImportSettingsStorage mExportImportSettingsStorage;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mExportImportSettingsStorage = new ExportImportSettingsStorage(mFakePreferenceHelper);

        when(mJobScheduler.forNamespace(ExportImportJobs.NAMESPACE)).thenReturn(mJobScheduler);
        when(mContext.getSystemService(JobScheduler.class)).thenReturn(mJobScheduler);
        when(mContext.getPackageName()).thenReturn(ANDROID_SERVER_PACKAGE_NAME);
        when(mContext.createContextAsUser(any(), anyInt())).thenReturn(mContext);
    }

    @Test
    public void schedulePeriodicExportJob_withPeriodZero_doesNotScheduleExportJob() {
        mExportImportSettingsStorage.configure(
                new ScheduledExportSettings.Builder().setPeriodInDays(0).build());

        ExportImportJobs.schedulePeriodicExportJob(
                UserHandle.CURRENT, mContext, mExportImportSettingsStorage, mExportManager);

        verify(mJobScheduler, times(0)).schedule(any());
    }

    @Test
    @EnableFlags({Flags.FLAG_EXPORT_IMPORT_FAST_FOLLOW})
    public void schedulePeriodicExportJob_cancelsPreviousJob() {
        mExportImportSettingsStorage.configure(
                new ScheduledExportSettings.Builder().setPeriodInDays(0).build());

        ExportImportJobs.schedulePeriodicExportJob(
                UserHandle.CURRENT, mContext, mExportImportSettingsStorage, mExportManager);

        verify(mJobScheduler, times(1)).cancelAll();
    }

    @Test
    public void schedulePeriodicExportJob_withPeriodGreaterThanZero_schedulesExportJob() {
        mExportImportSettingsStorage.configure(
                new ScheduledExportSettings.Builder().setPeriodInDays(1).build());

        ExportImportJobs.schedulePeriodicExportJob(
                UserHandle.CURRENT, mContext, mExportImportSettingsStorage, mExportManager);
        verify(mJobScheduler, times(1)).schedule(any());

        ExportImportJobs.schedulePeriodicExportJob(
                UserHandle.CURRENT, mContext, mExportImportSettingsStorage, mExportManager);
        verify(mJobScheduler, times(2)).schedule(any());
    }

    @Test
    @EnableFlags({Flags.FLAG_EXPORT_IMPORT_FAST_FOLLOW})
    public void schedulePeriodicExportJob_withPeriodGreaterThanZero_cancelsPreviousJob() {
        mExportImportSettingsStorage.configure(
                new ScheduledExportSettings.Builder().setPeriodInDays(1).build());

        ExportImportJobs.schedulePeriodicExportJob(
                UserHandle.CURRENT, mContext, mExportImportSettingsStorage, mExportManager);
        verify(mJobScheduler, times(1)).schedule(any());
        verify(mJobScheduler, times(1)).cancelAll();
    }

    @Test
    public void schedulePeriodicExportJob_noExportYet_schedulesHourlyJob() {
        mExportImportSettingsStorage.configure(
                new ScheduledExportSettings.Builder().setPeriodInDays(7).build());

        ExportImportJobs.schedulePeriodicExportJob(
                UserHandle.CURRENT, mContext, mExportImportSettingsStorage, mExportManager);
        verify(mJobScheduler, times(1)).schedule(mJobInfoCaptor.capture());
        assertThat(mJobInfoCaptor.getValue().getIntervalMillis())
                .isEqualTo(Duration.ofHours(1).toMillis());
        assertThat(
                        mJobInfoCaptor
                                .getValue()
                                .getExtras()
                                .getBoolean(ExportImportJobs.IS_FIRST_EXPORT))
                .isTrue();
    }

    @Test
    @EnableFlags({Flags.FLAG_EXPORT_IMPORT_FAST_FOLLOW})
    public void schedulePeriodicExportJob_withPeriodGreaterThanZero_persistsExportJob() {
        mExportImportSettingsStorage.configure(
                new ScheduledExportSettings.Builder().setPeriodInDays(1).build());

        ExportImportJobs.schedulePeriodicExportJob(
                UserHandle.CURRENT, mContext, mExportImportSettingsStorage, mExportManager);

        verify(mJobScheduler, times(1)).schedule(mJobInfoCaptor.capture());
        assertThat(mJobInfoCaptor.getValue().isPersisted()).isTrue();
    }

    @Test
    public void schedulePeriodicExportJob_exportAlreadyDone_schedulesJobWithPeriod() {
        Uri uri = Uri.parse("abc");
        mExportImportSettingsStorage.setLastSuccessfulExport(Instant.now(), uri);
        mExportImportSettingsStorage.configure(
                new ScheduledExportSettings.Builder().setPeriodInDays(7).setUri(uri).build());

        ExportImportJobs.schedulePeriodicExportJob(
                UserHandle.CURRENT, mContext, mExportImportSettingsStorage, mExportManager);
        verify(mJobScheduler, times(1)).schedule(mJobInfoCaptor.capture());
        assertThat(mJobInfoCaptor.getValue().getIntervalMillis())
                .isEqualTo(Duration.ofDays(7).toMillis());
        assertThat(
                        mJobInfoCaptor
                                .getValue()
                                .getExtras()
                                .getBoolean(ExportImportJobs.IS_FIRST_EXPORT))
                .isFalse();
    }

    @Test
    public void schedulePeriodicExportJob_exportAlreadyDone_newUri_schedulesJobWithPeriod() {
        mExportImportSettingsStorage.setLastSuccessfulExport(Instant.now(), Uri.parse("last_uri"));
        mExportImportSettingsStorage.configure(
                new ScheduledExportSettings.Builder()
                        .setUri(Uri.parse("new_uri"))
                        .setPeriodInDays(7)
                        .build());

        ExportImportJobs.schedulePeriodicExportJob(
                UserHandle.CURRENT, mContext, mExportImportSettingsStorage, mExportManager);
        verify(mJobScheduler, times(1)).schedule(mJobInfoCaptor.capture());
        assertThat(mJobInfoCaptor.getValue().getIntervalMillis())
                .isEqualTo(Duration.ofHours(1).toMillis());
        assertThat(
                        mJobInfoCaptor
                                .getValue()
                                .getExtras()
                                .getBoolean(ExportImportJobs.IS_FIRST_EXPORT))
                .isTrue();
    }

    @Test
    public void executePeriodicExportJob_withPeriodZero_doesNotRunExport() {
        mExportImportSettingsStorage.configure(
                new ScheduledExportSettings.Builder().setPeriodInDays(0).build());

        ExportImportJobs.executePeriodicExportJob(
                mContext,
                UserHandle.CURRENT,
                new PersistableBundle(),
                mExportManager,
                mExportImportSettingsStorage);

        verify(mExportManager, times(0)).runExport(UserHandle.CURRENT);
    }

    @Test
    public void executePeriodicExportJob_withPeriodZero_returnsTrue() {
        mExportImportSettingsStorage.configure(
                new ScheduledExportSettings.Builder().setPeriodInDays(0).build());

        boolean isExportSuccessful =
                ExportImportJobs.executePeriodicExportJob(
                        mContext,
                        UserHandle.CURRENT,
                        new PersistableBundle(),
                        mExportManager,
                        mExportImportSettingsStorage);

        assertThat(isExportSuccessful).isTrue();
    }

    @Test
    public void executePeriodicExportJob_withPeriodGreaterThanZero_runsExport() {
        mExportImportSettingsStorage.configure(
                new ScheduledExportSettings.Builder().setPeriodInDays(1).build());

        ExportImportJobs.executePeriodicExportJob(
                mContext,
                UserHandle.CURRENT,
                new PersistableBundle(),
                mExportManager,
                mExportImportSettingsStorage);

        verify(mExportManager, times(1)).runExport(UserHandle.CURRENT);
    }

    @Test
    public void executePeriodicExportJob_successfulExport_returnsTrue() {
        mExportImportSettingsStorage.configure(
                new ScheduledExportSettings.Builder().setPeriodInDays(1).build());
        when(mExportManager.runExport(UserHandle.CURRENT)).thenReturn(true);

        boolean isExportSuccessful =
                ExportImportJobs.executePeriodicExportJob(
                        mContext,
                        UserHandle.CURRENT,
                        new PersistableBundle(),
                        mExportManager,
                        mExportImportSettingsStorage);

        assertThat(isExportSuccessful).isTrue();
        verify(mJobScheduler, times(0)).schedule(any());
    }

    @Test
    public void executePeriodicExportJob_failedExport_returnsFalse() {
        mExportImportSettingsStorage.configure(
                new ScheduledExportSettings.Builder().setPeriodInDays(1).build());
        when(mExportManager.runExport(UserHandle.CURRENT)).thenReturn(false);

        boolean isExportSuccessful =
                ExportImportJobs.executePeriodicExportJob(
                        mContext,
                        UserHandle.CURRENT,
                        new PersistableBundle(),
                        mExportManager,
                        mExportImportSettingsStorage);

        assertThat(isExportSuccessful).isFalse();
    }

    @Test
    public void executePeriodicExportJob_successfulFirstExport_reschedulesJob() {
        mExportImportSettingsStorage.configure(
                new ScheduledExportSettings.Builder().setPeriodInDays(1).build());
        when(mExportManager.runExport(UserHandle.CURRENT)).thenReturn(true);

        PersistableBundle extras = new PersistableBundle();
        extras.putBoolean(ExportImportJobs.IS_FIRST_EXPORT, true);
        boolean isExportSuccessful =
                ExportImportJobs.executePeriodicExportJob(
                        mContext,
                        UserHandle.CURRENT,
                        extras,
                        mExportManager,
                        mExportImportSettingsStorage);

        assertThat(isExportSuccessful).isTrue();
        verify(mJobScheduler, times(1)).schedule(any());
    }

    @Test
    @DisableFlags({Flags.FLAG_EXPORT_IMPORT_FAST_FOLLOW})
    public void schedulePeriodicJobIfNotScheduled_fastFollowFlagNotEnabled_reschedules() {
        mExportImportSettingsStorage.configure(
                new ScheduledExportSettings.Builder().setPeriodInDays(1).build());

        ExportImportJobs.schedulePeriodicJobIfNotScheduled(
                UserHandle.CURRENT, mContext, mExportImportSettingsStorage, mExportManager);

        verify(mJobScheduler, times(1)).schedule(any());
    }

    @Test
    @EnableFlags({Flags.FLAG_EXPORT_IMPORT_FAST_FOLLOW})
    public void
            schedulePeriodicJobIfNotScheduled_whenPeriodIsNonZeroAndNoPendingJobs_reschedules() {
        mExportImportSettingsStorage.configure(
                new ScheduledExportSettings.Builder().setPeriodInDays(1).build());

        ExportImportJobs.schedulePeriodicJobIfNotScheduled(
                UserHandle.CURRENT, mContext, mExportImportSettingsStorage, mExportManager);

        verify(mJobScheduler, times(1)).schedule(any());
    }

    @Test
    @EnableFlags({Flags.FLAG_EXPORT_IMPORT_FAST_FOLLOW})
    public void schedulePeriodicJobIfNotScheduled_whenPendingJobsExist_shouldNotReschedule() {
        mExportImportSettingsStorage.configure(
                new ScheduledExportSettings.Builder().setPeriodInDays(1).build());
        JobInfo jobInfo =
                new JobInfo.Builder(
                                1234356, new ComponentName(mContext, ExportImportJobsTest.class))
                        .build();
        when(mJobScheduler.getAllPendingJobs()).thenReturn(List.of(jobInfo));

        ExportImportJobs.schedulePeriodicJobIfNotScheduled(
                UserHandle.CURRENT, mContext, mExportImportSettingsStorage, mExportManager);

        verify(mJobScheduler, times(0)).schedule(any());
    }

    @Test
    @EnableFlags({Flags.FLAG_EXPORT_IMPORT_FAST_FOLLOW})
    public void schedulePeriodicJobIfNotScheduled_whenPeriodIsZero_shouldNotReschedule() {
        mExportImportSettingsStorage.configure(
                new ScheduledExportSettings.Builder().setPeriodInDays(0).build());

        ExportImportJobs.schedulePeriodicJobIfNotScheduled(
                UserHandle.CURRENT, mContext, mExportImportSettingsStorage, mExportManager);

        verify(mJobScheduler, times(0)).schedule(any());
    }
}
