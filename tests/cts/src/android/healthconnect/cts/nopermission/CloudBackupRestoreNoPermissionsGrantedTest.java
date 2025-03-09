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

package android.healthconnect.cts.nopermission;

import static android.healthconnect.cts.utils.TestUtils.deleteAllStagedRemoteData;
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
import android.healthconnect.cts.utils.AssumptionCheckerRule;
import android.healthconnect.cts.utils.HealthConnectReceiver;
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
import java.util.concurrent.Executors;

@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
@RequiresFlagsEnabled({
    FLAG_CLOUD_BACKUP_AND_RESTORE,
    FLAG_CLOUD_BACKUP_AND_RESTORE_DB,
    FLAG_ECOSYSTEM_METRICS_DB_CHANGES,
    FLAG_HEALTH_CONNECT_BACKUP_RESTORE_PERMISSION_ENABLED
})
public class CloudBackupRestoreNoPermissionsGrantedTest {
    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Rule
    public AssumptionCheckerRule mSupportedHardwareRule =
            new AssumptionCheckerRule(
                    TestUtils::isHealthConnectFullySupported,
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
    public void getSettingsForBackup_noPermission_securityException() throws InterruptedException {
        HealthConnectReceiver<GetSettingsForBackupResponse> receiver =
                new HealthConnectReceiver<>();
        mManager.getSettingsForBackup(Executors.newSingleThreadExecutor(), receiver);

        HealthConnectException e = receiver.assertAndGetException();
        assertThat(e.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
    }

    @Test
    public void getChangesForBackup_noPermission_securityException() throws InterruptedException {
        HealthConnectReceiver<GetChangesForBackupResponse> receiver = new HealthConnectReceiver<>();
        mManager.getChangesForBackup(
                /* changeToken= */ null, Executors.newSingleThreadExecutor(), receiver);

        HealthConnectException e = receiver.assertAndGetException();
        assertThat(e.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
    }

    @Test
    public void canRestore_noPermission_securityException() throws InterruptedException {
        HealthConnectReceiver<Boolean> receiver = new HealthConnectReceiver<>();
        mManager.canRestore(/* version= */ 1, Executors.newSingleThreadExecutor(), receiver);

        HealthConnectException e = receiver.assertAndGetException();
        assertThat(e.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
    }

    @Test
    public void restoreChanges_noPermission_securityException() throws InterruptedException {
        HealthConnectReceiver<Void> receiver = new HealthConnectReceiver<>();
        mManager.restoreChanges(
                List.of(), new byte[0], Executors.newSingleThreadExecutor(), receiver);

        HealthConnectException e = receiver.assertAndGetException();
        assertThat(e.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
    }

    @Test
    public void restoreSettings_noPermission_securityException() throws InterruptedException {
        HealthConnectReceiver<Void> receiver = new HealthConnectReceiver<>();
        mManager.restoreSettings(
                new BackupSettings(new byte[0]), Executors.newSingleThreadExecutor(), receiver);

        HealthConnectException e = receiver.assertAndGetException();
        assertThat(e.getErrorCode()).isEqualTo(HealthConnectException.ERROR_SECURITY);
    }
}
