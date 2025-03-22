/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.healthconnect.cts.backgroundread;

import static android.health.connect.HealthConnectException.ERROR_SECURITY;
import static android.health.connect.HealthDataCategory.ACTIVITY;
import static android.health.connect.HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND;
import static android.healthconnect.cts.utils.DataFactory.NOW;
import static android.healthconnect.cts.utils.DataFactory.getStepsRecord;
import static android.healthconnect.cts.utils.DataFactory.getStepsRecordWithEmptyMetaData;
import static android.healthconnect.cts.utils.TestUtils.deleteAllStagedRemoteData;
import static android.healthconnect.cts.utils.TestUtils.getRecordIds;
import static android.healthconnect.cts.utils.TestUtils.setupAggregation;

import static com.android.compatibility.common.util.SystemUtil.eventually;
import static com.android.compatibility.common.util.SystemUtil.runWithShellPermissionIdentity;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.Objects.requireNonNull;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.health.connect.HealthConnectException;
import android.health.connect.HealthConnectManager;
import android.health.connect.InsertRecordsResponse;
import android.health.connect.ReadRecordsRequestUsingFilters;
import android.health.connect.ReadRecordsRequestUsingIds;
import android.health.connect.changelog.ChangeLogTokenRequest;
import android.health.connect.changelog.ChangeLogsRequest;
import android.health.connect.datatypes.ActiveCaloriesBurnedRecord;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.StepsRecord;
import android.healthconnect.cts.lib.TestAppProxy;
import android.healthconnect.cts.utils.AssumptionCheckerRule;
import android.healthconnect.cts.utils.DeviceSupportUtils;
import android.healthconnect.cts.utils.HealthConnectReceiver;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class BackgroundReadTest {

    private static final String PKG_TEST_APP = "android.healthconnect.cts.testapp.readWritePerms.A";

    private Context mContext;
    private PackageManager mPackageManager;
    private HealthConnectManager mManager;
    private TestAppProxy mTestApp;

    @Rule
    public AssumptionCheckerRule mSupportedHardwareRule =
            new AssumptionCheckerRule(
                    DeviceSupportUtils::isHealthConnectFullySupported,
                    "Tests should run on supported hardware only.");

    @Before
    public void setUp() throws Exception {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        mPackageManager = mContext.getPackageManager();
        mManager = requireNonNull(mContext.getSystemService(HealthConnectManager.class));
        mTestApp = TestAppProxy.forPackageNameInBackground(PKG_TEST_APP);

        // Ensure that App Ops considers the test app to be in the background. This may take a few
        // seconds if another test has recently launched it in the foreground.
        AppOpsManager appOpsManager =
                requireNonNull(mContext.getSystemService(AppOpsManager.class));
        int uid = mPackageManager.getPackageUid(PKG_TEST_APP, /* flags= */ 0);
        eventually(
                () ->
                        assertThat(
                                        appOpsManager.unsafeCheckOp(
                                                AppOpsManager.OPSTR_FINE_LOCATION,
                                                uid,
                                                PKG_TEST_APP))
                                .isEqualTo(AppOpsManager.MODE_IGNORED));

        deleteAllStagedRemoteData();
    }

    @After
    public void tearDown() throws Exception {
        deleteAllStagedRemoteData();
    }

    @Test
    public void testReadRecordsByFilters_inBackgroundWithoutPermission_cannotReadOtherAppsData()
            throws Exception {
        revokeBackgroundReadPermissionForTestApp();
        insertStepsRecordsDirectly(List.of(getStepsRecordWithEmptyMetaData()));

        // test app will try to read the step record inserted by this test
        ReadRecordsRequestUsingFilters<StepsRecord> request =
                new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                        .addDataOrigins(
                                new DataOrigin.Builder()
                                        .setPackageName(mContext.getPackageName())
                                        .build())
                        .build();
        HealthConnectException thrown =
                assertThrows(HealthConnectException.class, () -> mTestApp.readRecords(request));
        assertThat(thrown.getErrorCode()).isEqualTo(ERROR_SECURITY);
    }

    @Test
    public void testReadRecordsByFilters_inBackgroundWithoutPermission_canReadOwnData()
            throws Exception {
        revokeBackgroundReadPermissionForTestApp();
        String insertedId = mTestApp.insertRecord(getStepsRecord(10, NOW, NOW.plus(1, MINUTES)));

        // test app will try to read the step record inserted by itself
        ReadRecordsRequestUsingFilters<StepsRecord> request =
                new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                        .addDataOrigins(
                                new DataOrigin.Builder().setPackageName(PKG_TEST_APP).build())
                        .build();
        List<StepsRecord> records = mTestApp.readRecords(request);

        assertThat(records.stream().map(r -> r.getMetadata().getId())).containsExactly(insertedId);
    }

    @Test
    public void testReadRecordsByFilters_inBackgroundWithPermission_canReadBothOwnAndOtherAppsData()
            throws Exception {
        grantBackgroundReadPermissionForTestApp();
        String idInsertedByThisTest =
                insertStepsRecordsDirectly(List.of(getStepsRecordWithEmptyMetaData())).get(0);
        String idInsertedByTestApp =
                mTestApp.insertRecord(getStepsRecord(10, NOW, NOW.plus(1, MINUTES)));

        // test app will try to read the step record inserted by both this test and the test app
        ReadRecordsRequestUsingFilters<StepsRecord> request =
                new ReadRecordsRequestUsingFilters.Builder<>(StepsRecord.class)
                        .addDataOrigins(
                                new DataOrigin.Builder()
                                        .setPackageName(mContext.getPackageName())
                                        .build())
                        .addDataOrigins(
                                new DataOrigin.Builder().setPackageName(PKG_TEST_APP).build())
                        .build();
        List<StepsRecord> records = mTestApp.readRecords(request);

        assertThat(records.stream().map(r -> r.getMetadata().getId()))
                .containsExactly(idInsertedByThisTest, idInsertedByTestApp);
    }

    @Test
    public void testReadRecordsByIds_inBackgroundWithoutPermission_canReadOnlyOwnData()
            throws Exception {
        revokeBackgroundReadPermissionForTestApp();
        String idInsertedByThisTest =
                insertStepsRecordsDirectly(List.of(getStepsRecordWithEmptyMetaData())).get(0);
        String idInsertedByTestApp =
                mTestApp.insertRecord(getStepsRecord(10, NOW, NOW.plus(1, MINUTES)));

        // test app will try to read the step record inserted by both this test and the test app
        ReadRecordsRequestUsingIds<StepsRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(StepsRecord.class)
                        .addId(idInsertedByTestApp)
                        .addId(idInsertedByThisTest)
                        .build();
        List<StepsRecord> records = mTestApp.readRecords(request);

        assertThat(records.stream().map(r -> r.getMetadata().getId()))
                .containsExactly(idInsertedByTestApp);
    }

    @Test
    public void testReadRecordsByIds_inBackgroundWithPermission_canReadBothOwnAndOtherAppsData()
            throws Exception {
        grantBackgroundReadPermissionForTestApp();
        String idInsertedByThisTest =
                insertStepsRecordsDirectly(List.of(getStepsRecordWithEmptyMetaData())).get(0);
        String idInsertedByTestApp =
                mTestApp.insertRecord(getStepsRecord(10, NOW, NOW.plus(1, MINUTES)));

        // test app will try to read the step record inserted by both this test and the test app
        ReadRecordsRequestUsingIds<StepsRecord> request =
                new ReadRecordsRequestUsingIds.Builder<>(StepsRecord.class)
                        .addId(idInsertedByTestApp)
                        .addId(idInsertedByThisTest)
                        .build();
        List<StepsRecord> records = mTestApp.readRecords(request);

        assertThat(records.stream().map(r -> r.getMetadata().getId()))
                .containsExactly(idInsertedByThisTest, idInsertedByTestApp);
    }

    // TODO(b/309776578): once this bug b/309776578 is fixed, this test should be broken down into
    // two tests, one for aggregating own data which should succeed, and one for aggregating other
    // apps' data which should fail.
    @Test
    public void testAggregate_inBackgroundWithoutPermission_expectSecurityError() throws Exception {
        revokeBackgroundReadPermissionForTestApp();
        insertStepsRecordsDirectly(List.of(getStepsRecordWithEmptyMetaData())).get(0);
        mTestApp.insertRecord(getStepsRecord(10, NOW, NOW.plus(1, MINUTES)));

        HealthConnectException thrown =
                assertThrows(
                        HealthConnectException.class,
                        () ->
                                mTestApp.aggregateStepsCountTotal(
                                        Instant.EPOCH,
                                        Instant.now().plus(10, HOURS),
                                        List.of(mContext.getPackageName(), PKG_TEST_APP)));
        assertThat(thrown.getErrorCode()).isEqualTo(ERROR_SECURITY);
    }

    @Test
    public void testAggregate_inBackgroundWithPermission_canAggregateBothOwnAndOtherAppsData()
            throws Exception {
        grantBackgroundReadPermissionForTestApp();
        long value1 = 10;
        long value2 = 5;
        StepsRecord stepsRecord1 = getStepsRecord(value1);
        insertStepsRecordsDirectly(List.of(stepsRecord1)).get(0);
        mTestApp.insertRecord(
                getStepsRecord(
                        value2,
                        stepsRecord1.getStartTime().minus(10, HOURS),
                        stepsRecord1.getEndTime().minus(10, HOURS)));
        setupAggregation(List.of(mContext.getPackageName(), PKG_TEST_APP), ACTIVITY);

        long result =
                mTestApp.aggregateStepsCountTotal(
                        Instant.EPOCH,
                        Instant.now().plus(10, HOURS),
                        List.of(mContext.getPackageName(), PKG_TEST_APP));
        assertThat(result).isEqualTo(value1 + value2);
    }

    @Test
    public void testGetChangeLogs_inBackgroundWithoutPermission_securityError() throws Exception {
        revokeBackgroundReadPermissionForTestApp();

        ChangeLogsRequest request = new ChangeLogsRequest.Builder("token").build();
        HealthConnectException thrown =
                assertThrows(HealthConnectException.class, () -> mTestApp.getChangeLogs(request));

        assertThat(thrown.getErrorCode()).isEqualTo(ERROR_SECURITY);
    }

    @Test
    public void testGetChangeLogs_inBackgroundWithPermission_success() throws Exception {
        revokeBackgroundReadPermissionForTestApp();

        ChangeLogTokenRequest tokenRequest =
                new ChangeLogTokenRequest.Builder()
                        .addRecordType(ActiveCaloriesBurnedRecord.class)
                        .build();
        String token = mTestApp.getChangeLogToken(tokenRequest);

        grantBackgroundReadPermissionForTestApp();

        ChangeLogsRequest changeLogsRequest = new ChangeLogsRequest.Builder(token).build();
        mTestApp.getChangeLogs(changeLogsRequest);
    }

    private List<String> insertStepsRecordsDirectly(List<Record> recordsToInsert)
            throws InterruptedException {
        InsertRecordsResponse response =
                HealthConnectReceiver.callAndGetResponse(
                        (executor, receiver) ->
                                mManager.insertRecords(recordsToInsert, executor, receiver));
        return getRecordIds(response.getRecords());
    }

    private void grantBackgroundReadPermissionForTestApp() {
        runWithShellPermissionIdentity(
                () ->
                        mPackageManager.grantRuntimePermission(
                                PKG_TEST_APP, READ_HEALTH_DATA_IN_BACKGROUND, mContext.getUser()));
    }

    private void revokeBackgroundReadPermissionForTestApp() throws InterruptedException {
        runWithShellPermissionIdentity(
                () ->
                        mPackageManager.revokeRuntimePermission(
                                PKG_TEST_APP, READ_HEALTH_DATA_IN_BACKGROUND, mContext.getUser()));

        // Wait a bit for the process to be killed
        Thread.sleep(500);
    }
}
