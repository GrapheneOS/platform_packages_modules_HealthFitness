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

package android.healthconnect.cts.device;

import static android.healthconnect.cts.utils.DataFactory.getEmptyMetadata;
import static android.healthconnect.cts.utils.TestUtils.verifyDeleteRecords;

import static com.google.common.truth.Truth.assertThat;

import static java.time.Duration.ofMinutes;

import android.health.connect.ReadRecordsRequestUsingIds;
import android.health.connect.RecordIdFilter;
import android.health.connect.TimeInstantRangeFilter;
import android.health.connect.changelog.ChangeLogTokenRequest;
import android.health.connect.changelog.ChangeLogsRequest;
import android.health.connect.changelog.ChangeLogsResponse;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.StepsRecord;
import android.healthconnect.cts.lib.TestAppProxy;
import android.healthconnect.cts.utils.AssumptionCheckerRule;
import android.healthconnect.cts.utils.DeviceSupportUtils;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.common.truth.Correspondence;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@RunWith(AndroidJUnit4.class)
public class HealthConnectChangeLogsDeviceTests {

    private static final TestAppProxy APP_A_WITH_READ_WRITE_PERMS =
            TestAppProxy.forPackageName("android.healthconnect.cts.testapp.readWritePerms.A");

    private static final TestAppProxy APP_B_WITH_READ_WRITE_PERMS =
            TestAppProxy.forPackageName("android.healthconnect.cts.testapp.readWritePerms.B");

    private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Correspondence<ChangeLogsResponse.DeletedLog, String>
            DELETED_LOG_TO_STRING_ID_CORRESPONDENCE =
                    Correspondence.from(
                            (deletedLog, stringId) ->
                                    deletedLog.getDeletedRecordId().equals(stringId),
                            "has matching string id");

    @Rule
    public AssumptionCheckerRule mSupportedHardwareRule =
            new AssumptionCheckerRule(
                    DeviceSupportUtils::isHealthConnectFullySupported,
                    "Tests should run on supported hardware only.");

    @After
    public void tearDown() throws InterruptedException {
        verifyDeleteRecords(
                StepsRecord.class, new TimeInstantRangeFilter.Builder().setEndTime(NOW).build());
    }

    @Test
    public void testChangeLogs_insert_multipleApps_noFilter_returnsUpsertLogsForAllApps()
            throws Exception {
        String changeLogToken =
                APP_A_WITH_READ_WRITE_PERMS.getChangeLogToken(
                        new ChangeLogTokenRequest.Builder()
                                .addRecordType(StepsRecord.class)
                                .build());
        ChangeLogsRequest changeLogsRequest = new ChangeLogsRequest.Builder(changeLogToken).build();

        String recordIdInsertedByAppA = APP_A_WITH_READ_WRITE_PERMS.insertRecord(getStepsRecord());
        String recordIdInsertedByAppB = APP_B_WITH_READ_WRITE_PERMS.insertRecord(getStepsRecord());
        StepsRecord recordInsertedByAppA =
                APP_A_WITH_READ_WRITE_PERMS
                        .readRecords(
                                new ReadRecordsRequestUsingIds.Builder<>(StepsRecord.class)
                                        .addId(recordIdInsertedByAppA)
                                        .build())
                        .get(0);
        StepsRecord recordInsertedByAppB =
                APP_B_WITH_READ_WRITE_PERMS
                        .readRecords(
                                new ReadRecordsRequestUsingIds.Builder<>(StepsRecord.class)
                                        .addId(recordIdInsertedByAppB)
                                        .build())
                        .get(0);
        ChangeLogsResponse response = APP_A_WITH_READ_WRITE_PERMS.getChangeLogs(changeLogsRequest);

        assertThat(response.getUpsertedRecords())
                .containsExactly(recordInsertedByAppA, recordInsertedByAppB);
        assertThat(response.getDeletedLogs()).isEmpty();
    }

    @Test
    public void testChangeLogs_insert_multipleApps_filterDataOrigin_returnsUpsertLogs()
            throws Exception {
        String changeLogToken =
                APP_A_WITH_READ_WRITE_PERMS.getChangeLogToken(
                        new ChangeLogTokenRequest.Builder()
                                .addRecordType(StepsRecord.class)
                                .addDataOriginFilter(
                                        new DataOrigin.Builder()
                                                .setPackageName(
                                                        APP_A_WITH_READ_WRITE_PERMS
                                                                .getPackageName())
                                                .build())
                                .build());
        ChangeLogsRequest changeLogsRequest = new ChangeLogsRequest.Builder(changeLogToken).build();

        String recordIdInsertedByAppA = APP_A_WITH_READ_WRITE_PERMS.insertRecord(getStepsRecord());
        APP_A_WITH_READ_WRITE_PERMS.insertRecord(getStepsRecord());
        StepsRecord recordInsertedByAppA =
                APP_A_WITH_READ_WRITE_PERMS
                        .readRecords(
                                new ReadRecordsRequestUsingIds.Builder<>(StepsRecord.class)
                                        .addId(recordIdInsertedByAppA)
                                        .build())
                        .get(0);
        ChangeLogsResponse response = APP_A_WITH_READ_WRITE_PERMS.getChangeLogs(changeLogsRequest);

        assertThat(response.getUpsertedRecords()).containsExactly(recordInsertedByAppA);
        assertThat(response.getDeletedLogs()).isEmpty();
    }

    @Test
    public void testChangeLogs_insertAndDelete_multipleApps_noFilter_returnsDeletedLogsForAllApps()
            throws Exception {
        String changeLogToken =
                APP_A_WITH_READ_WRITE_PERMS.getChangeLogToken(
                        new ChangeLogTokenRequest.Builder()
                                .addRecordType(StepsRecord.class)
                                .build());
        ChangeLogsRequest changeLogsRequest = new ChangeLogsRequest.Builder(changeLogToken).build();

        String recordIdInsertedByAppA = APP_A_WITH_READ_WRITE_PERMS.insertRecord(getStepsRecord());
        String recordIdInsertedByAppB = APP_B_WITH_READ_WRITE_PERMS.insertRecord(getStepsRecord());
        APP_A_WITH_READ_WRITE_PERMS.deleteRecords(
                RecordIdFilter.fromId(StepsRecord.class, recordIdInsertedByAppA));
        APP_B_WITH_READ_WRITE_PERMS.deleteRecords(
                RecordIdFilter.fromId(StepsRecord.class, recordIdInsertedByAppB));
        ChangeLogsResponse response = APP_A_WITH_READ_WRITE_PERMS.getChangeLogs(changeLogsRequest);

        assertThat(response.getDeletedLogs())
                .comparingElementsUsing(DELETED_LOG_TO_STRING_ID_CORRESPONDENCE)
                .containsExactly(recordIdInsertedByAppA, recordIdInsertedByAppB);
        assertThat(response.getUpsertedRecords()).isEmpty();
    }

    @Test
    public void testChangeLogs_insertAndDelete_multipleApps_filterDataOrigin_returnsDeletedLogs()
            throws Exception {
        String changeLogToken =
                APP_A_WITH_READ_WRITE_PERMS.getChangeLogToken(
                        new ChangeLogTokenRequest.Builder()
                                .addRecordType(StepsRecord.class)
                                .addDataOriginFilter(
                                        new DataOrigin.Builder()
                                                .setPackageName(
                                                        APP_B_WITH_READ_WRITE_PERMS
                                                                .getPackageName())
                                                .build())
                                .build());
        ChangeLogsRequest changeLogsRequest = new ChangeLogsRequest.Builder(changeLogToken).build();

        String recordIdInsertedByAppA = APP_A_WITH_READ_WRITE_PERMS.insertRecord(getStepsRecord());
        String recordIdInsertedByAppB = APP_B_WITH_READ_WRITE_PERMS.insertRecord(getStepsRecord());
        APP_A_WITH_READ_WRITE_PERMS.deleteRecords(
                RecordIdFilter.fromId(StepsRecord.class, recordIdInsertedByAppA));
        APP_B_WITH_READ_WRITE_PERMS.deleteRecords(
                RecordIdFilter.fromId(StepsRecord.class, recordIdInsertedByAppB));
        ChangeLogsResponse response = APP_A_WITH_READ_WRITE_PERMS.getChangeLogs(changeLogsRequest);

        assertThat(response.getDeletedLogs())
                .comparingElementsUsing(DELETED_LOG_TO_STRING_ID_CORRESPONDENCE)
                .containsExactly(recordIdInsertedByAppB);
        assertThat(response.getUpsertedRecords()).isEmpty();
    }

    private static StepsRecord getStepsRecord() {
        Instant startTime = NOW.minus(ofMinutes(10));
        Instant endTime = NOW.minus(ofMinutes(5));
        return new StepsRecord.Builder(getEmptyMetadata(), startTime, endTime, 155).build();
    }
}
