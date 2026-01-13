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

package com.android.health.connect.backuprestore;

import static android.health.HealthFitnessStatsLog.HEALTH_CONNECT_BACKUP_AGENT_EVENT_REPORTED;

import static org.mockito.Mockito.verify;

import android.health.HealthFitnessStatsLog;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/** Unit tests for {@link BackupAgentLogger}. */
@RunWith(AndroidJUnit4.class)
public class BackupAgentLoggerTest {

    @Mock private HealthFitnessStatsLog mStatsLog;

    private BackupAgentLogger mBackupAgentLogger;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mBackupAgentLogger = new BackupAgentLogger(mStatsLog);
    }

    @Test
    public void testLogStarted_logsStartedEvent() {
        mBackupAgentLogger.logStarted(BackupAgentLogger.BACKUP_AGENT_OPERATION_BACKUP);
        verify(mStatsLog)
                .write(
                        HEALTH_CONNECT_BACKUP_AGENT_EVENT_REPORTED,
                        BackupAgentLogger.BACKUP_AGENT_OPERATION_BACKUP,
                        BackupAgentLogger.BACKUP_AGENT_STATUS_STARTED,
                        BackupAgentLogger.BACKUP_AGENT_ERROR_UNKNOWN,
                        /*latency*/ 0);
    }

    @Test
    public void testLogSuccess_logsSuccessEvent() {
        int latency = 100;
        mBackupAgentLogger.logSuccess(BackupAgentLogger.BACKUP_AGENT_OPERATION_RESTORE, latency);
        verify(mStatsLog)
                .write(
                        HEALTH_CONNECT_BACKUP_AGENT_EVENT_REPORTED,
                        BackupAgentLogger.BACKUP_AGENT_OPERATION_RESTORE,
                        BackupAgentLogger.BACKUP_AGENT_STATUS_SUCCESS,
                        /*error*/ 0,
                        latency);
    }

    @Test
    public void testLogFailed_logsFailedEvent() {
        int latency = 200;
        mBackupAgentLogger.logFailed(
                BackupAgentLogger.BACKUP_AGENT_OPERATION_FULL_BACKUP,
                BackupAgentLogger.BACKUP_AGENT_ERROR_STAGING_FAILED_EXCEPTION,
                latency);
        verify(mStatsLog)
                .write(
                        HEALTH_CONNECT_BACKUP_AGENT_EVENT_REPORTED,
                        BackupAgentLogger.BACKUP_AGENT_OPERATION_FULL_BACKUP,
                        BackupAgentLogger.BACKUP_AGENT_STATUS_FAILURE,
                        BackupAgentLogger.BACKUP_AGENT_ERROR_STAGING_FAILED_EXCEPTION,
                        latency);
    }
}
