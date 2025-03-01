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

package healthconnect.logging;

import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_DATA_BACKUP_INVOKED;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_DATA_BACKUP_INVOKED__BACKUP_TYPE__DATA_BACKUP_TYPE_INCREMENTAL;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_DATA_BACKUP_INVOKED__STATUS__DATA_BACKUP_STATUS_ERROR_PARTIAL_BACKUP;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_DATA_RESTORE_INVOKED;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_DATA_RESTORE_INVOKED__STATUS__DATA_RESTORE_STATUS_ERROR_NONE;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_RESTORE_ELIGIBILITY_CHECKED;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_SETTINGS_BACKUP_INVOKED;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_SETTINGS_BACKUP_INVOKED__STATUS__SETTINGS_BACKUP_STATUS_ERROR_COLLATION_FAILED;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_SETTINGS_RESTORE_INVOKED;
import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_SETTINGS_RESTORE_INVOKED__STATUS__SETTINGS_RESTORE_STATUS_ERROR_NONE;

import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;

import android.health.HealthFitnessStatsLog;

import com.android.dx.mockito.inline.extended.ExtendedMockito;
import com.android.modules.utils.testing.ExtendedMockitoRule;
import com.android.server.healthconnect.logging.BackupRestoreLogger;

import org.junit.Rule;
import org.junit.Test;

public class BackupRestoreLoggerTest {

    @Rule
    public final ExtendedMockitoRule mExtendedMockitoRule =
            new ExtendedMockitoRule.Builder(this).mockStatic(HealthFitnessStatsLog.class).build();

    @Test
    public void test_logDataBackupStatus() {
        // variables created to comply with java formatter line length
        int statusPartialBackup =
                HEALTH_CONNECT_DATA_BACKUP_INVOKED__STATUS__DATA_BACKUP_STATUS_ERROR_PARTIAL_BACKUP;
        int backupTypeIncremental =
                HEALTH_CONNECT_DATA_BACKUP_INVOKED__BACKUP_TYPE__DATA_BACKUP_TYPE_INCREMENTAL;

        BackupRestoreLogger.logDataBackupStatus(
                statusPartialBackup, 100, 2000, backupTypeIncremental);
        ExtendedMockito.verify(
                () ->
                        HealthFitnessStatsLog.write(
                                eq(HEALTH_CONNECT_DATA_BACKUP_INVOKED),
                                eq(statusPartialBackup),
                                eq(100),
                                eq(2000),
                                eq(backupTypeIncremental)),
                times(1));
    }

    @Test
    public void test_logSettingsBackupStatus() {
        // variable to comply with java formatter line length
        int statusSettingsBackup =
                HEALTH_CONNECT_SETTINGS_BACKUP_INVOKED__STATUS__SETTINGS_BACKUP_STATUS_ERROR_COLLATION_FAILED;

        BackupRestoreLogger.logSettingsBackupStatus(statusSettingsBackup, 100, 2000);
        ExtendedMockito.verify(
                () ->
                        HealthFitnessStatsLog.write(
                                eq(HEALTH_CONNECT_SETTINGS_BACKUP_INVOKED),
                                eq(statusSettingsBackup),
                                eq(100),
                                eq(2000)),
                times(1));
    }

    @Test
    public void test_logDataRestoreStatus() {
        // variable to comply with java formatter line length
        int statusDataRestore =
                HEALTH_CONNECT_DATA_RESTORE_INVOKED__STATUS__DATA_RESTORE_STATUS_ERROR_NONE;

        BackupRestoreLogger.logDataRestoreStatus(statusDataRestore, 100, 2000);
        ExtendedMockito.verify(
                () ->
                        HealthFitnessStatsLog.write(
                                eq(HEALTH_CONNECT_DATA_RESTORE_INVOKED),
                                eq(statusDataRestore),
                                eq(100),
                                eq(2000)),
                times(1));
    }

    @Test
    public void test_logSettingsRestoreStatus() {
        // variable to comply with java formatter line length
        int statusSettingsRestore =
                HEALTH_CONNECT_SETTINGS_RESTORE_INVOKED__STATUS__SETTINGS_RESTORE_STATUS_ERROR_NONE;

        BackupRestoreLogger.logSettingsRestoreStatus(statusSettingsRestore, 100, 2000);
        ExtendedMockito.verify(
                () ->
                        HealthFitnessStatsLog.write(
                                eq(HEALTH_CONNECT_SETTINGS_RESTORE_INVOKED),
                                eq(statusSettingsRestore),
                                eq(100),
                                eq(2000)),
                times(1));
    }

    @Test
    public void test_logRestoreEligibility() {
        BackupRestoreLogger.logRestoreEligibility(true);
        ExtendedMockito.verify(
                () ->
                        HealthFitnessStatsLog.write(
                                eq(HEALTH_CONNECT_RESTORE_ELIGIBILITY_CHECKED), eq(true)),
                times(1));
    }
}
