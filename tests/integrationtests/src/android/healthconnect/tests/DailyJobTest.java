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
package android.healthconnect.tests;

import static android.healthconnect.cts.utils.TestUtils.deleteAllStagedRemoteData;
import static android.healthconnect.cts.utils.TestUtils.insertRecords;
import static android.healthconnect.cts.utils.TestUtils.readAllRecords;
import static android.healthconnect.testing.shared.recordfactory.RecordFactory.newEmptyMetadata;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import android.content.Context;
import android.health.connect.HealthPermissions;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.StepsRecord;
import android.healthconnect.cts.utils.AssumptionCheckerRule;
import android.healthconnect.cts.utils.DeviceSupportUtils;
import android.healthconnect.cts.utils.PermissionHelper;
import android.healthconnect.cts.utils.TestUtils;
import android.healthconnect.testing.cts.JobUtils;
import android.healthconnect.testing.shared.recordfactory.RecordFactory;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.compatibility.common.util.SystemUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** Integration tests for Health Connect daily job. */
@RunWith(AndroidJUnit4.class)
public class DailyJobTest {
    private static final String JOB_NAMESPACE = "HEALTH_CONNECT_DAILY_JOB";

    private static final int TIMEOUT_MS = 10000;

    public AssumptionCheckerRule mSupportedHardwareRule =
            new AssumptionCheckerRule(
                    DeviceSupportUtils::isHealthConnectFullySupported,
                    "Tests should run on supported hardware only.");

    @Before
    public void setUp() throws Exception {
        Context mContext = ApplicationProvider.getApplicationContext();
        deleteAllStagedRemoteData();

        PermissionHelper.grantHealthPermission(
                mContext.getPackageName(), HealthPermissions.READ_STEPS);
        PermissionHelper.grantHealthPermission(
                mContext.getPackageName(), HealthPermissions.WRITE_STEPS);
        PermissionHelper.grantHealthPermission(
                mContext.getPackageName(), HealthPermissions.READ_HEALTH_DATA_HISTORY);

        SystemUtil.eventually(
                () ->
                        assertWithMessage("The daily job is expected to be scheduled")
                                .that(JobUtils.isJobScheduled(JOB_NAMESPACE))
                                .isTrue(),
                TIMEOUT_MS);
    }

    @After
    public void tearDown() throws Exception {
        // Set retention period to 0 to disable auto-delete.
        TestUtils.setRecordRetentionPeriodInDays(0);
        deleteAllStagedRemoteData();
    }

    @Test
    public void autoDeleteEnabled_oldDataIsDeleted() throws Exception {
        RecordFactory<? extends Record> recordFactory =
                RecordFactory.forDataType(StepsRecord.class);
        insertRecords(
                // Record within the last 3 months.
                recordFactory.newFullRecord(
                        newEmptyMetadata(),
                        Instant.now().minus(Duration.ofDays(11)),
                        Instant.now().minus(Duration.ofDays(10))),
                // Record older than 3 months.
                recordFactory.newFullRecord(
                        newEmptyMetadata(),
                        Instant.now().minus(Duration.ofDays(101)),
                        Instant.now().minus(Duration.ofDays(100))));
        List<StepsRecord> readRecords = readAllRecords(StepsRecord.class);
        assertThat(readRecords).hasSize(2);

        TestUtils.setRecordRetentionPeriodInDays(90);
        JobUtils.runJobIfScheduled(JOB_NAMESPACE);

        SystemUtil.eventually(
                () ->
                        assertWithMessage("The daily job is expected to be scheduled")
                                .that(readAllRecords(StepsRecord.class))
                                .hasSize(1),
                TIMEOUT_MS);
    }

    @Test
    public void autoDeleteDisabled_noDataIsDeleted() throws Exception {
        RecordFactory<? extends Record> recordFactory =
                RecordFactory.forDataType(StepsRecord.class);
        insertRecords(
                // Record within the last 3 months.
                recordFactory.newFullRecord(
                        newEmptyMetadata(),
                        Instant.now().minus(Duration.ofDays(11)),
                        Instant.now().minus(Duration.ofDays(10))),
                // Record older than 3 months.
                recordFactory.newFullRecord(
                        newEmptyMetadata(),
                        Instant.now().minus(Duration.ofDays(101)),
                        Instant.now().minus(Duration.ofDays(100))));
        List<StepsRecord> readRecords = readAllRecords(StepsRecord.class);
        assertThat(readRecords).hasSize(2);

        TestUtils.setRecordRetentionPeriodInDays(0);
        JobUtils.runJobIfScheduled(JOB_NAMESPACE);

        List<StepsRecord> readRecordsAfterDailyJob = readAllRecords(StepsRecord.class);
        assertThat(readRecordsAfterDailyJob).hasSize(2);
    }
}
