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

import android.healthconnect.ChangeLogTokenRequest;
import android.platform.test.annotations.AppModeFull;

import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/** CTS test for API provided by HealthConnectManager. */
@AppModeFull(reason = "HealthConnectManager is not accessible to instant apps")
@RunWith(AndroidJUnit4.class)
public class HealthConnectChangeLogsTests {
    @Test
    public void testGetChangeLogToken() throws InterruptedException {
        assertThat(TestUtils.getChangeLogToken(new ChangeLogTokenRequest.Builder().build()))
                .isNotNull();
    }

    // UNCOMMENT THESE TESTS WHEN THE API IS MADE VISIBLE

    //    @Test
    //    public void testChangeLogs_insert_default() throws InterruptedException {
    //        long token = TestUtils.getChangeLogToken(new ChangeLogTokenRequest.Builder().build());
    //        ChangeLogsResponse response = TestUtils.getChangeLogs(token);
    //        int logCount = response.getUpsertedRecords().size() +
    // response.getDeletedRecordIds().size();
    //        assertThat(logCount).isEqualTo(0);
    //
    //        List<Record> testRecord = TestUtils.getTestRecords();
    //        TestUtils.insertRecords(testRecord);
    //        response = TestUtils.getChangeLogs(token);
    //        logCount = response.getUpsertedRecords().size() +
    // response.getDeletedRecordIds().size();
    //        assertThat(logCount).isEqualTo(testRecord.size());
    //    }
    //
    //    @Test
    //    public void testChangeLogs_insert_dataOrigin_filter_incorrect() throws
    // InterruptedException {
    //        long token =
    //                TestUtils.getChangeLogToken(
    //                        new ChangeLogTokenRequest.Builder()
    //                                .addDataOriginFilter(
    //                                        new
    // DataOrigin.Builder().setPackageName("random").build())
    //                                .build());
    //        ChangeLogsResponse response = TestUtils.getChangeLogs(token);
    //        int logCount = response.getUpsertedRecords().size() +
    // response.getDeletedRecordIds().size();
    //        assertThat(logCount).isEqualTo(0);
    //
    //        List<Record> testRecord = TestUtils.getTestRecords();
    //        TestUtils.insertRecords(testRecord);
    //        response = TestUtils.getChangeLogs(token);
    //        logCount = response.getUpsertedRecords().size() +
    // response.getDeletedRecordIds().size();
    //        assertThat(logCount).isEqualTo(0);
    //    }
    //
    //    @Test
    //    public void testChangeLogs_insert_dataOrigin_filter_correct() throws InterruptedException
    // {
    //        Context context = ApplicationProvider.getApplicationContext();
    //        long token =
    //                TestUtils.getChangeLogToken(
    //                        new ChangeLogTokenRequest.Builder()
    //                                .addDataOriginFilter(
    //                                        new DataOrigin.Builder()
    //                                                .setPackageName(context.getPackageName())
    //                                                .build())
    //                                .build());
    //        ChangeLogsResponse response = TestUtils.getChangeLogs(token);
    //        int logCount = response.getUpsertedRecords().size() +
    // response.getDeletedRecordIds().size();
    //        assertThat(logCount).isEqualTo(0);
    //
    //        List<Record> testRecord = TestUtils.getTestRecords();
    //        TestUtils.insertRecords(testRecord);
    //        response = TestUtils.getChangeLogs(token);
    //        logCount = response.getUpsertedRecords().size() +
    // response.getDeletedRecordIds().size();
    //        assertThat(logCount).isEqualTo(testRecord.size());
    //    }
    //
    //    @Test
    //    public void testChangeLogs_insert_record_filter() throws InterruptedException {
    //        Context context = ApplicationProvider.getApplicationContext();
    //        long token =
    //                TestUtils.getChangeLogToken(
    //                        new ChangeLogTokenRequest.Builder()
    //                                .addDataOriginFilter(
    //                                        new DataOrigin.Builder()
    //                                                .setPackageName(context.getPackageName())
    //                                                .build())
    //                                .addRecordType(StepsRecord.class)
    //                                .build());
    //        ChangeLogsResponse response = TestUtils.getChangeLogs(token);
    //        int logCount = response.getUpsertedRecords().size() +
    // response.getDeletedRecordIds().size();
    //        assertThat(logCount).isEqualTo(0);
    //
    //        List<Record> testRecord = Collections.singletonList(TestUtils.getStepsRecord());
    //        TestUtils.insertRecords(testRecord);
    //        response = TestUtils.getChangeLogs(token);
    //        logCount = response.getUpsertedRecords().size() +
    // response.getDeletedRecordIds().size();
    //        assertThat(logCount).isEqualTo(1);
    //        testRecord = Collections.singletonList(TestUtils.getHeartRateRecord());
    //        TestUtils.insertRecords(testRecord);
    //        response = TestUtils.getChangeLogs(token);
    //        logCount = response.getUpsertedRecords().size() +
    // response.getDeletedRecordIds().size();
    //        assertThat(logCount).isEqualTo(1);
    //    }
}
