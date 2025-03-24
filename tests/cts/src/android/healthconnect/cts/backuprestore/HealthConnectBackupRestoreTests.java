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

package android.healthconnect.cts.backuprestore;

import static android.Manifest.permission.BACKUP_HEALTH_CONNECT_DATA_AND_SETTINGS;
import static android.Manifest.permission.RESTORE_HEALTH_CONNECT_DATA_AND_SETTINGS;
import static android.healthconnect.cts.utils.HealthConnectReceiver.callAndGetResponseWithShellPermissionIdentity;
import static android.healthconnect.cts.utils.TestUtils.deleteAllStagedRemoteData;
import static android.healthconnect.cts.utils.TestUtils.insertRecords;
import static android.permission.flags.Flags.FLAG_HEALTH_CONNECT_BACKUP_RESTORE_PERMISSION_ENABLED;

import static com.android.healthfitness.flags.Flags.FLAG_CLOUD_BACKUP_AND_RESTORE;
import static com.android.healthfitness.flags.Flags.FLAG_CLOUD_BACKUP_AND_RESTORE_DB;
import static com.android.healthfitness.flags.Flags.FLAG_ECOSYSTEM_METRICS_DB_CHANGES;

import static com.google.common.truth.Truth.assertThat;

import android.health.connect.HealthConnectException;
import android.health.connect.HealthConnectManager;
import android.health.connect.backuprestore.BackupSettings;
import android.health.connect.backuprestore.GetChangesForBackupResponse;
import android.health.connect.backuprestore.GetSettingsForBackupResponse;
import android.health.connect.datatypes.Record;
import android.healthconnect.cts.utils.AssumptionCheckerRule;
import android.healthconnect.cts.utils.DataFactory;
import android.healthconnect.cts.utils.DeviceSupportUtils;
import android.healthconnect.cts.utils.TestUtils;
import android.os.Build;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

/** CTS tests for {@link HealthConnectManager} backup & restore related APIs. */
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
@RequiresFlagsEnabled({
    FLAG_ECOSYSTEM_METRICS_DB_CHANGES,
    FLAG_CLOUD_BACKUP_AND_RESTORE,
    FLAG_CLOUD_BACKUP_AND_RESTORE_DB,
    FLAG_HEALTH_CONNECT_BACKUP_RESTORE_PERMISSION_ENABLED
})
public class HealthConnectBackupRestoreTests {
    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Rule
    public AssumptionCheckerRule mSupportedHardwareRule =
            new AssumptionCheckerRule(
                    DeviceSupportUtils::isHealthConnectFullySupported,
                    "Tests should run on supported hardware only.");

    private HealthConnectManager mManager;

    @Before
    public void setup() {
        deleteAllStagedRemoteData();
        mManager = TestUtils.getHealthConnectManager();
    }

    @After
    public void teardown() {
        deleteAllStagedRemoteData();
    }

    @Test
    public void getSettingsForBackup_success() throws InterruptedException {
        GetSettingsForBackupResponse response =
                callAndGetResponseWithShellPermissionIdentity(
                        mManager::getSettingsForBackup, BACKUP_HEALTH_CONNECT_DATA_AND_SETTINGS);

        assertThat(response.getSettings()).isNotNull();
    }

    @Test
    public void getChangesForBackup_success() throws InterruptedException {
        List<android.health.connect.datatypes.Record> testRecords = DataFactory.getTestRecords();
        List<Record> insertedRecords = insertRecords(testRecords);
        GetChangesForBackupResponse response =
                callAndGetResponseWithShellPermissionIdentity(
                        (executor, receiver) ->
                                mManager.getChangesForBackup(null, executor, receiver),
                        BACKUP_HEALTH_CONNECT_DATA_AND_SETTINGS);

        assertThat(response.getChanges().size()).isEqualTo(insertedRecords.size());
    }

    @Test
    public void canRestore_success() throws InterruptedException {
        Boolean canRestore =
                callAndGetResponseWithShellPermissionIdentity(
                        (executor, receiver) -> mManager.canRestore(1, executor, receiver),
                        RESTORE_HEALTH_CONNECT_DATA_AND_SETTINGS);

        assertThat(canRestore).isTrue();
    }

    @Test
    public void restoreSettings_success() throws InterruptedException {
        // TODO: b/392853668 - add assertions once the test plan is in place.
        Void unused =
                callAndGetResponseWithShellPermissionIdentity(
                        HealthConnectException.class,
                        (executor, receiver) ->
                                mManager.restoreSettings(
                                        new BackupSettings(new byte[0]), executor, receiver),
                        RESTORE_HEALTH_CONNECT_DATA_AND_SETTINGS);
    }
}
