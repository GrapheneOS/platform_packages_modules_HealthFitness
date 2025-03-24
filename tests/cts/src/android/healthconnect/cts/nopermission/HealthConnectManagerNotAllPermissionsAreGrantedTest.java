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

package android.healthconnect.cts.nopermission;

import static android.health.connect.HealthPermissions.READ_DISTANCE;
import static android.health.connect.HealthPermissions.READ_EXERCISE;
import static android.health.connect.HealthPermissions.READ_HEART_RATE;
import static android.health.connect.HealthPermissions.READ_SLEEP;
import static android.health.connect.HealthPermissions.READ_STEPS;
import static android.health.connect.HealthPermissions.READ_TOTAL_CALORIES_BURNED;
import static android.healthconnect.cts.utils.DataFactory.buildExerciseSession;
import static android.healthconnect.cts.utils.DataFactory.buildSleepSession;
import static android.healthconnect.cts.utils.DataFactory.getDistanceRecordWithNonEmptyId;
import static android.healthconnect.cts.utils.DataFactory.getHeartRateRecord;
import static android.healthconnect.cts.utils.DataFactory.getStepsRecord;
import static android.healthconnect.cts.utils.DataFactory.getTotalCaloriesBurnedRecord;
import static android.healthconnect.cts.utils.PermissionHelper.grantHealthPermissions;
import static android.healthconnect.cts.utils.PermissionHelper.revokeAllHealthPermissions;
import static android.healthconnect.cts.utils.PermissionHelper.revokeHealthPermission;
import static android.healthconnect.cts.utils.TestUtils.deleteRecords;
import static android.healthconnect.cts.utils.TestUtils.getChangeLogToken;
import static android.healthconnect.cts.utils.TestUtils.insertRecords;
import static android.healthconnect.cts.utils.TestUtils.updateRecords;

import static com.google.common.truth.Truth.assertThat;

import android.health.connect.HealthConnectException;
import android.health.connect.changelog.ChangeLogTokenRequest;
import android.health.connect.changelog.ChangeLogsRequest;
import android.health.connect.datatypes.DistanceRecord;
import android.health.connect.datatypes.ExerciseSessionRecord;
import android.health.connect.datatypes.HeartRateRecord;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.SleepSessionRecord;
import android.health.connect.datatypes.StepsRecord;
import android.health.connect.datatypes.TotalCaloriesBurnedRecord;
import android.healthconnect.cts.lib.TestAppProxy;
import android.healthconnect.cts.utils.AssumptionCheckerRule;
import android.healthconnect.cts.utils.DeviceSupportUtils;
import android.platform.test.annotations.AppModeFull;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

/** These tests run under an environment which only some HC permissions are granted. */
@AppModeFull(reason = "HealthConnectManager is not accessible to instant apps")
@RunWith(AndroidJUnit4.class)
public class HealthConnectManagerNotAllPermissionsAreGrantedTest {
    private static final TestAppProxy APP_A_WITH_READ_WRITE_PERMS =
            TestAppProxy.forPackageName("android.healthconnect.cts.testapp.readWritePerms.A");

    @Rule
    public AssumptionCheckerRule mSupportedHardwareRule =
            new AssumptionCheckerRule(
                    DeviceSupportUtils::isHealthConnectFullySupported,
                    "Tests should run on supported hardware only.");

    @Test
    public void testInsert_somePermissionsAreNotGranted_expectError() throws InterruptedException {
        try {
            insertRecords(getTestRecords());

            Assert.fail("WRITE_DISTANCE is not granted, this test should fail!");
        } catch (HealthConnectException healthConnectException) {
            assertThat(healthConnectException.getErrorCode())
                    .isEqualTo(HealthConnectException.ERROR_SECURITY);
        }
    }

    @Test
    public void testUpdate_somePermissionsAreNotGranted_expectError() throws InterruptedException {
        try {
            updateRecords(getTestRecords());

            Assert.fail("WRITE_DISTANCE is not granted, this test should fail!");
        } catch (HealthConnectException healthConnectException) {
            assertThat(healthConnectException.getErrorCode())
                    .isEqualTo(HealthConnectException.ERROR_SECURITY);
        }
    }

    @Test
    public void testDeleteUsingIds_somePermissionsAreNotGranted_expectError()
            throws InterruptedException {
        try {
            deleteRecords(getTestRecords());

            Assert.fail("WRITE_DISTANCE is not granted, this test should fail!");
        } catch (HealthConnectException healthConnectException) {
            assertThat(healthConnectException.getErrorCode())
                    .isEqualTo(HealthConnectException.ERROR_SECURITY);
        }
    }

    @Test
    public void testGetChangeLogToken_somePermissionsAreNotGranted_expectError()
            throws InterruptedException {
        try {
            ChangeLogTokenRequest.Builder request = new ChangeLogTokenRequest.Builder();
            for (Record record : getTestRecords()) {
                request.addRecordType(record.getClass());
            }

            getChangeLogToken(request.build());

            Assert.fail("READ_DISTANCE is not granted, this test should fail!");
        } catch (HealthConnectException healthConnectException) {
            assertThat(healthConnectException.getErrorCode())
                    .isEqualTo(HealthConnectException.ERROR_SECURITY);
        }
    }

    @Test
    public void testGetChangeLogs_somePermissionsAreNotGranted_expectError() throws Exception {
        TestAppProxy testApp = APP_A_WITH_READ_WRITE_PERMS;
        String packageName = testApp.getPackageName();
        revokeAllHealthPermissions(packageName, /* reason= */ "for test");
        grantHealthPermissions(
                packageName,
                List.of(
                        READ_STEPS,
                        READ_DISTANCE,
                        READ_HEART_RATE,
                        READ_SLEEP,
                        READ_EXERCISE,
                        READ_TOTAL_CALORIES_BURNED));
        String token =
                testApp.getChangeLogToken(
                        new ChangeLogTokenRequest.Builder()
                                .addRecordType(StepsRecord.class)
                                .addRecordType(DistanceRecord.class)
                                .addRecordType(HeartRateRecord.class)
                                .addRecordType(SleepSessionRecord.class)
                                .addRecordType(ExerciseSessionRecord.class)
                                .addRecordType(TotalCaloriesBurnedRecord.class)
                                .build());

        // revoke one permission which the app needs so it can use the token
        revokeHealthPermission(packageName, READ_DISTANCE);

        try {
            testApp.getChangeLogs(new ChangeLogsRequest.Builder(token).build());

            Assert.fail(
                    String.format(
                            "READ_DISTANCE is not granted to %s, this test should fail!",
                            packageName));
        } catch (HealthConnectException healthConnectException) {
            assertThat(healthConnectException.getErrorCode())
                    .isEqualTo(HealthConnectException.ERROR_SECURITY);
        }
    }

    private static List<Record> getTestRecords() {
        return Arrays.asList(
                getStepsRecord(),
                getHeartRateRecord(),
                buildSleepSession(),
                getDistanceRecordWithNonEmptyId(),
                getTotalCaloriesBurnedRecord("client_id"),
                buildExerciseSession());
    }
}
