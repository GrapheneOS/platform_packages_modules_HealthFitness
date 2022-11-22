/*
 * Copyright (C) 2022 The Android Open Source Project
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

package android.healthconnect.cts;

import static com.google.common.truth.Truth.assertThat;

import android.healthconnect.AccessLog;
import android.healthconnect.HealthConnectManager;
import android.healthconnect.ReadRecordsRequestUsingFilters;
import android.healthconnect.datatypes.BasalMetabolicRateRecord;
import android.healthconnect.datatypes.HeartRateRecord;
import android.healthconnect.datatypes.Record;
import android.healthconnect.datatypes.StepsRecord;
import android.platform.test.annotations.AppModeFull;

import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.List;

/** CTS test for {@link HealthConnectManager#queryAccessLogs} API. */
@AppModeFull(reason = "HealthConnectManager is not accessible to instant apps")
@RunWith(AndroidJUnit4.class)
public class HealthConnectAccessLogsTest {
    @Test
    public void testAccessLogs_read_singleRecordType() throws InterruptedException {
        List<AccessLog> oldAccessLogsResponse = TestUtils.queryAccessLogs();
        List<Record> testRecord = Collections.singletonList(TestUtils.getStepsRecord());
        TestUtils.insertRecords(testRecord);
        TestUtils.readRecords(
                new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class).build());
        List<AccessLog> newAccessLogsResponse = TestUtils.queryAccessLogs();
        assertThat(newAccessLogsResponse.size() - oldAccessLogsResponse.size()).isEqualTo(2);
        int size = newAccessLogsResponse.size();
        AccessLog accessLog = newAccessLogsResponse.get(size - 1);
        assertThat(accessLog.getRecordTypes()).contains(StepsRecord.class);
        assertThat(accessLog.getOperationType()).isEqualTo(2);
        assertThat(accessLog.getPackageName()).isEqualTo("android.healthconnect.cts");
        assertThat(accessLog.getAccessTime()).isNotNull();
    }

    @Test
    public void testAccessLogs_read_multipleRecordTypes() throws InterruptedException {
        List<AccessLog> oldAccessLogsResponse = TestUtils.queryAccessLogs();
        List<Record> testRecord = TestUtils.getTestRecords();
        TestUtils.insertRecords(testRecord);
        TestUtils.readRecords(
                new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class).build());
        TestUtils.readRecords(
                new ReadRecordsRequestUsingFilters.Builder<>(HeartRateRecord.class).build());
        TestUtils.readRecords(
                new ReadRecordsRequestUsingFilters.Builder<>(BasalMetabolicRateRecord.class)
                        .build());
        List<AccessLog> newAccessLogsResponse = TestUtils.queryAccessLogs();
        assertThat(newAccessLogsResponse.size() - oldAccessLogsResponse.size()).isEqualTo(4);
    }

    @Test
    public void testAccessLogs_afterInsert() throws InterruptedException {
        List<AccessLog> oldAccessLogsResponse = TestUtils.queryAccessLogs();
        List<Record> testRecord = TestUtils.getTestRecords();
        TestUtils.insertRecords(testRecord);
        List<AccessLog> newAccessLogsResponse = TestUtils.queryAccessLogs();
        int size = newAccessLogsResponse.size();
        assertThat(size).isEqualTo(oldAccessLogsResponse.size() + 1);
        assertThat(newAccessLogsResponse.get(size - 1).getOperationType()).isEqualTo(0);
        assertThat(newAccessLogsResponse.get(size - 1).getRecordTypes())
                .contains(StepsRecord.class);
        assertThat(newAccessLogsResponse.get(size - 1).getRecordTypes())
                .contains(HeartRateRecord.class);
        assertThat(newAccessLogsResponse.get(size - 1).getRecordTypes())
                .contains(BasalMetabolicRateRecord.class);
        assertThat(newAccessLogsResponse.get(size - 1).getAccessTime()).isNotNull();
    }
}
