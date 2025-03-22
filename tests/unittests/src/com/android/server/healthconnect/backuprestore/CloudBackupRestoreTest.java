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

package com.android.server.healthconnect.backuprestore;

import static com.android.healthfitness.flags.Flags.FLAG_CLOUD_BACKUP_AND_RESTORE;
import static com.android.healthfitness.flags.Flags.FLAG_CLOUD_BACKUP_AND_RESTORE_DB;
import static com.android.healthfitness.flags.Flags.FLAG_ECOSYSTEM_METRICS_DB_CHANGES;
import static com.android.server.healthconnect.backuprestore.ProtoTestData.TEST_PACKAGE_NAME;
import static com.android.server.healthconnect.testing.storage.TransactionTestUtils.createStepsRecord;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.health.connect.backuprestore.BackupChange;
import android.health.connect.backuprestore.RestoreChange;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.health.connect.datatypes.StepsRecord;
import android.health.connect.internal.datatypes.RecordInternal;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;
import com.android.server.healthconnect.permission.FirstGrantTimeManager;
import com.android.server.healthconnect.permission.HealthPermissionIntentAppsTracker;
import com.android.server.healthconnect.storage.TransactionManager;
import com.android.server.healthconnect.storage.datatypehelpers.AppInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.DatabaseHelper.DatabaseHelpers;
import com.android.server.healthconnect.storage.datatypehelpers.DeviceInfoHelper;
import com.android.server.healthconnect.testing.storage.TransactionTestUtils;

import com.google.common.collect.ImmutableMap;

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
import java.util.List;

@RunWith(AndroidJUnit4.class)
@EnableFlags({
    FLAG_CLOUD_BACKUP_AND_RESTORE,
    FLAG_CLOUD_BACKUP_AND_RESTORE_DB,
    FLAG_ECOSYSTEM_METRICS_DB_CHANGES
})
public final class CloudBackupRestoreTest {

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final TemporaryFolder mEnvironmentDataDir = new TemporaryFolder();

    private AppInfoHelper mAppInfoHelper;
    private DeviceInfoHelper mDeviceInfoHelper;
    private DatabaseHelpers mDatabaseHelpers;
    private TransactionManager mTransactionManager;
    private TransactionTestUtils mTransactionTestUtils;
    private CloudBackupManager mCloudBackupManager;
    private CloudRestoreManager mCloudRestoreManager;
    private RecordProtoConverter mRecordProtoConverter;
    private Instant mTimeStamp;

    // TODO(b/373322447): Remove the mock FirstGrantTimeManager
    @Mock private FirstGrantTimeManager mFirstGrantTimeManager;
    // TODO(b/373322447): Remove the mock HealthPermissionIntentAppsTracker
    @Mock private HealthPermissionIntentAppsTracker mPermissionIntentAppsTracker;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        HealthConnectInjector healthConnectInjector =
                HealthConnectInjectorImpl.newBuilderForTest(context)
                        .setFirstGrantTimeManager(mFirstGrantTimeManager)
                        .setHealthPermissionIntentAppsTracker(mPermissionIntentAppsTracker)
                        .setEnvironmentDataDirectory(mEnvironmentDataDir.getRoot())
                        .build();

        mTransactionManager = healthConnectInjector.getTransactionManager();
        mAppInfoHelper = healthConnectInjector.getAppInfoHelper();
        mDeviceInfoHelper = healthConnectInjector.getDeviceInfoHelper();
        mDatabaseHelpers = healthConnectInjector.getDatabaseHelpers();
        mTransactionTestUtils = new TransactionTestUtils(healthConnectInjector);
        mRecordProtoConverter = new RecordProtoConverter();
        mTransactionTestUtils.insertApp(TEST_PACKAGE_NAME);

        mTimeStamp = Instant.parse("2024-06-04T16:39:12Z");
        Clock fakeClock = Clock.fixed(mTimeStamp, ZoneId.of("UTC"));

        mCloudBackupManager =
                new CloudBackupManager(
                        mTransactionManager,
                        healthConnectInjector.getFitnessRecordReadHelper(),
                        mAppInfoHelper,
                        mDeviceInfoHelper,
                        healthConnectInjector.getHealthConnectMappings(),
                        healthConnectInjector.getInternalHealthConnectMappings(),
                        healthConnectInjector.getChangeLogsHelper(),
                        healthConnectInjector.getChangeLogsRequestHelper(),
                        healthConnectInjector.getHealthDataCategoryPriorityHelper(),
                        healthConnectInjector.getPreferenceHelper(),
                        fakeClock,
                        healthConnectInjector.getBackupRestoreLogger());
        mCloudRestoreManager =
                new CloudRestoreManager(
                        mTransactionManager,
                        healthConnectInjector.getFitnessRecordUpsertHelper(),
                        healthConnectInjector.getFitnessRecordReadHelper(),
                        healthConnectInjector.getInternalHealthConnectMappings(),
                        mDeviceInfoHelper,
                        mAppInfoHelper,
                        healthConnectInjector.getHealthDataCategoryPriorityHelper(),
                        healthConnectInjector.getPreferenceHelper(),
                        fakeClock,
                        healthConnectInjector.getBackupRestoreLogger());
    }

    @Test
    public void backUpAndRestoreChanges_dataIsTheSame() {
        RecordInternal<StepsRecord> stepsRecord = createStepsRecord(123456, 654321, 123);
        mTransactionTestUtils.insertRecords(TEST_PACKAGE_NAME, stepsRecord);

        List<BackupChange> backupChanges =
                mCloudBackupManager.getChangesForBackup(null).getChanges();
        assertThat(backupChanges).hasSize(1);
        mDatabaseHelpers.clearAllData(mTransactionManager);
        mCloudRestoreManager.restoreChanges(
                backupChanges.stream().map(change -> new RestoreChange(change.getData())).toList());

        List<RecordInternal<?>> records =
                mTransactionTestUtils.readRecordsByIds(
                        ImmutableMap.of(
                                RecordTypeIdentifier.RECORD_TYPE_STEPS,
                                List.of(stepsRecord.getUuid())));
        assertThat(records).hasSize(1);
        // Comparing proto representations because internal records don't implement equals
        assertThat(mRecordProtoConverter.toRecordProto(records.get(0)))
                .isEqualTo(mRecordProtoConverter.toRecordProto(stepsRecord));
    }
}
