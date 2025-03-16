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

package healthconnect.storage;

import static com.android.healthfitness.flags.Flags.FLAG_ACTIVITY_INTENSITY_DB;
import static com.android.healthfitness.flags.Flags.FLAG_ECOSYSTEM_METRICS;
import static com.android.healthfitness.flags.Flags.FLAG_ECOSYSTEM_METRICS_DB_CHANGES;
import static com.android.healthfitness.flags.Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Process;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.server.healthconnect.exportimport.ExportManager;
import com.android.server.healthconnect.fitness.FitnessRecordDeleteHelper;
import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;
import com.android.server.healthconnect.migration.MigrationUiStateManager;
import com.android.server.healthconnect.permission.FirstGrantTimeManager;
import com.android.server.healthconnect.permission.HealthPermissionIntentAppsTracker;
import com.android.server.healthconnect.storage.DailyCleanupJob;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.datatypehelpers.AccessLogsHelper;
import com.android.server.healthconnect.storage.datatypehelpers.ActivityDateHelper;
import com.android.server.healthconnect.storage.datatypehelpers.AppInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.ChangeLogsHelper;
import com.android.server.healthconnect.storage.datatypehelpers.ChangeLogsRequestHelper;
import com.android.server.healthconnect.storage.datatypehelpers.HealthDataCategoryPriorityHelper;
import com.android.server.healthconnect.storage.datatypehelpers.PreferenceHelper;
import com.android.server.healthconnect.storage.datatypehelpers.ReadAccessLogsHelper;
import com.android.server.healthconnect.storage.request.DeleteTableRequest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
@EnableFlags({
    FLAG_ECOSYSTEM_METRICS,
    FLAG_ECOSYSTEM_METRICS_DB_CHANGES,
    FLAG_PERSONAL_HEALTH_RECORD_DATABASE,
    FLAG_ACTIVITY_INTENSITY_DB
})
public class DailyCleanupJobTest {
    private static final String AUTO_DELETE_DURATION_RECORDS_KEY =
            "auto_delete_duration_records_key";

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock private PreferenceHelper mPreferenceHelper;
    @Mock private TransactionManager mTransactionManager;
    @Mock private FitnessRecordDeleteHelper mFitnessRecordDeleteHelper;
    @Mock private ExportManager mExportManager;

    @Mock private AppInfoHelper mAppInfoHelper;
    @Mock private HealthDataCategoryPriorityHelper mHealthDataCategoryPriorityHelper;
    @Mock private ActivityDateHelper mActivityDateHelper;
    @Mock private MigrationUiStateManager mMigrationUiStateManager;
    @Mock Context mContext;

    // TODO(b/373322447): Remove the mock FirstGrantTimeManager
    @Mock private FirstGrantTimeManager mFirstGrantTimeManager;
    // TODO(b/373322447): Remove the mock HealthPermissionIntentAppsTracker
    @Mock private HealthPermissionIntentAppsTracker mPermissionIntentAppsTracker;

    private HealthConnectInjector mHealthConnectInjector;
    private DailyCleanupJob mDailyCleanupJob;

    @Before
    public void setup() {
        when(mContext.getUser()).thenReturn(Process.myUserHandle());
        mHealthConnectInjector =
                HealthConnectInjectorImpl.newBuilderForTest(mContext)
                        .setHealthDataCategoryPriorityHelper(mHealthDataCategoryPriorityHelper)
                        .setPreferenceHelper(mPreferenceHelper)
                        .setExportManager(mExportManager)
                        .setTransactionManager(mTransactionManager)
                        .setFitnessRecordDeleteHelper(mFitnessRecordDeleteHelper)
                        .setMigrationUiStateManager(mMigrationUiStateManager)
                        .setAppInfoHelper(mAppInfoHelper)
                        .setActivityDateHelper(mActivityDateHelper)
                        .setFirstGrantTimeManager(mFirstGrantTimeManager)
                        .setHealthPermissionIntentAppsTracker(mPermissionIntentAppsTracker)
                        .build();
        mDailyCleanupJob = mHealthConnectInjector.getDailyCleanupJob();
    }

    @Test
    public void testStartDailyCleanup_getPreferenceReturnNull() {
        when(mPreferenceHelper.getPreference(AUTO_DELETE_DURATION_RECORDS_KEY)).thenReturn(null);

        mDailyCleanupJob.startDailyCleanup();

        verify(mTransactionManager, Mockito.times(2))
                .deleteAll(Mockito.argThat(this::checkTableNames_getPreferenceReturnNull));
        verify(mAppInfoHelper).syncAppInfoRecordTypesUsed();
        verify(mHealthDataCategoryPriorityHelper).reSyncHealthDataPriorityTable();
        verify(mActivityDateHelper, times(1)).reSyncForAllRecords();
    }

    @Test
    public void testStartDailyCleanup_getPreferenceReturnNonNull() {
        when(mPreferenceHelper.getPreference(AUTO_DELETE_DURATION_RECORDS_KEY))
                .thenReturn(String.valueOf(30));

        mDailyCleanupJob.startDailyCleanup();

        verify(mTransactionManager, Mockito.times(2))
                .deleteAll(Mockito.argThat(this::checkTableNames_getPreferenceReturnNonNull));
        verify(mFitnessRecordDeleteHelper)
                .deleteRecordsUnrestricted(
                        Mockito.argThat(this::checkTableNames_getPreferenceReturnNonNull));
        verify(mAppInfoHelper).syncAppInfoRecordTypesUsed();
        verify(mHealthDataCategoryPriorityHelper).reSyncHealthDataPriorityTable();
        verify(mActivityDateHelper, times(1)).reSyncForAllRecords();
    }

    private boolean checkTableNames_getPreferenceReturnNull(List<DeleteTableRequest> list) {
        Set<String> tableNames = new HashSet<>();
        for (DeleteTableRequest request : list) {
            tableNames.add(request.getTableName());
        }
        return (tableNames.equals(getTableNamesForDeletingStaleChangeLogEntries())
                || tableNames.equals(getTableNamesForDeletingStaleAccessLogsEntries()));
    }

    private boolean checkTableNames_getPreferenceReturnNonNull(List<DeleteTableRequest> list) {
        Set<String> tableNames = new HashSet<>();
        for (DeleteTableRequest request : list) {
            tableNames.add(request.getTableName());
        }
        return (tableNames.equals(getTableNamesForDeletingStaleChangeLogEntries())
                || tableNames.equals(getTableNamesForDeletingStaleAccessLogsEntries())
                || tableNames.equals(getTableNamesForDeletingStaleRecordEntries()));
    }

    List<DeleteTableRequest> getDeleteTableRequests(int recordAutoDeletePeriod) {
        List<DeleteTableRequest> deleteTableRequests = new ArrayList<>();

        mHealthConnectInjector
                .getInternalHealthConnectMappings()
                .getRecordHelpers()
                .forEach(
                        (recordHelper) -> {
                            DeleteTableRequest request =
                                    recordHelper.getDeleteRequestForAutoDelete(
                                            recordAutoDeletePeriod);
                            deleteTableRequests.add(request);
                        });

        return deleteTableRequests;
    }

    Set<String> getTableNamesForDeletingStaleRecordEntries() {
        Set<String> tableNames = new HashSet<>();

        for (DeleteTableRequest deleteTableRequest : getDeleteTableRequests(30)) {
            tableNames.add(deleteTableRequest.getTableName());
        }

        return tableNames;
    }

    Set<String> getTableNamesForDeletingStaleChangeLogEntries() {
        Set<String> tableNames = new HashSet<>();

        tableNames.add(ChangeLogsHelper.getDeleteRequestForAutoDelete().getTableName());
        tableNames.add(ChangeLogsRequestHelper.getDeleteRequestForAutoDelete().getTableName());

        return tableNames;
    }

    Set<String> getTableNamesForDeletingStaleAccessLogsEntries() {
        Set<String> tableNames = new HashSet<>();

        tableNames.add(AccessLogsHelper.getDeleteRequestForAutoDelete().getTableName());
        tableNames.add(ReadAccessLogsHelper.getDeleteRequestForAutoDelete().getTableName());

        return tableNames;
    }
}
