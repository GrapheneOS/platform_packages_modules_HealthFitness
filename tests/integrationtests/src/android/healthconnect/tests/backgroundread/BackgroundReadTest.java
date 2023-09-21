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

package android.healthconnect.tests.backgroundread;

import static android.health.connect.HealthConnectException.ERROR_SECURITY;
import static android.health.connect.HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND;
import static android.healthconnect.test.app.TestAppReceiver.ACTION_AGGREGATE;
import static android.healthconnect.test.app.TestAppReceiver.ACTION_GET_CHANGE_LOGS;
import static android.healthconnect.test.app.TestAppReceiver.ACTION_GET_CHANGE_LOG_TOKEN;
import static android.healthconnect.test.app.TestAppReceiver.ACTION_READ_RECORDS_FOR_OTHER_APP;
import static android.healthconnect.test.app.TestAppReceiver.EXTRA_RECORD_COUNT;
import static android.healthconnect.test.app.TestAppReceiver.EXTRA_TOKEN;
import static android.healthconnect.tests.TestUtils.deleteAllStagedRemoteData;
import static android.healthconnect.tests.TestUtils.getDeviceConfigValue;
import static android.healthconnect.tests.TestUtils.setDeviceConfigValue;

import static com.android.compatibility.common.util.SystemUtil.runWithShellPermissionIdentity;

import static com.google.common.truth.Truth.assertThat;

import static java.util.Objects.requireNonNull;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.health.connect.HealthConnectManager;
import android.health.connect.InsertRecordsResponse;
import android.health.connect.datatypes.ActiveCaloriesBurnedRecord;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.units.Energy;
import android.healthconnect.test.app.BlockingOutcomeReceiver;
import android.healthconnect.test.app.TestAppReceiver;
import android.healthconnect.tests.TestReceiver;
import android.os.Bundle;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;

@RunWith(AndroidJUnit4.class)
public class BackgroundReadTest {

    private static final String PKG_TEST_APP = "android.healthconnect.test.app";
    private static final String TEST_APP_RECEIVER =
            PKG_TEST_APP + "." + TestAppReceiver.class.getSimpleName();
    private static final String FEATURE_FLAG = "background_read_enable";

    private Context mContext;
    private PackageManager mPackageManager;
    private HealthConnectManager mManager;
    private String mInitialFeatureFlagValue;

    @Before
    public void setUp() throws Exception {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        mPackageManager = mContext.getPackageManager();
        mManager = requireNonNull(mContext.getSystemService(HealthConnectManager.class));
        mInitialFeatureFlagValue = getDeviceConfigValue(FEATURE_FLAG);

        setDeviceConfigValue(FEATURE_FLAG, "true");
        deleteAllStagedRemoteData();
        TestReceiver.reset();
    }

    @After
    public void tearDown() throws Exception {
        deleteAllStagedRemoteData();
        setDeviceConfigValue(FEATURE_FLAG, mInitialFeatureFlagValue);
    }

    @Test
    public void testReadRecords_inBackgroundWithoutPermission_cantReadRecordsForOtherApp() {
        revokeBackgroundReadPermissionForTestApp();
        insertRecords();

        sendCommandToTestAppReceiver(ACTION_READ_RECORDS_FOR_OTHER_APP);

        final Bundle result = TestReceiver.getResult();
        assertThat(result).isNotNull();

        // Other apps' data is simply not returned when reading in background
        assertThat(result.getInt(EXTRA_RECORD_COUNT)).isEqualTo(0);
    }

    @Test
    public void testReadRecords_inBackgroundWithPermission_canReadRecordsForOtherApp() {
        grantBackgroundReadPermissionForTestApp();
        insertRecords();

        sendCommandToTestAppReceiver(ACTION_READ_RECORDS_FOR_OTHER_APP);

        final Bundle result = TestReceiver.getResult();
        assertThat(result).isNotNull();
        assertThat(result.getInt(EXTRA_RECORD_COUNT)).isGreaterThan(0);
    }

    private void insertRecords() {
        final BlockingOutcomeReceiver<InsertRecordsResponse> outcomeReceiver =
                new BlockingOutcomeReceiver<>();

        mManager.insertRecords(
                List.of(
                        new ActiveCaloriesBurnedRecord.Builder(
                                        new Metadata.Builder().build(),
                                        Instant.now().minusSeconds(60),
                                        Instant.now(),
                                        Energy.fromCalories(100.0))
                                .build()),
                Executors.newSingleThreadExecutor(),
                outcomeReceiver);

        assertThat(outcomeReceiver.getError()).isNull();
    }

    @Test
    public void testAggregate_inBackgroundWithoutPermission_securityError() {
        revokeBackgroundReadPermissionForTestApp();

        sendCommandToTestAppReceiver(ACTION_AGGREGATE);

        assertSecurityError();
    }

    @Test
    public void testAggregate_inBackgroundWithPermission_success() {
        grantBackgroundReadPermissionForTestApp();

        sendCommandToTestAppReceiver(ACTION_AGGREGATE);

        assertSuccess();
    }

    @Test
    public void testGetChangeLogs_inBackgroundWithoutPermission_securityError() {
        revokeBackgroundReadPermissionForTestApp();

        final Bundle extras = new Bundle();
        extras.putString(EXTRA_TOKEN, "token");
        sendCommandToTestAppReceiver(ACTION_GET_CHANGE_LOGS, extras);

        assertSecurityError();
    }

    @Test
    public void testGetChangeLogs_inBackgroundWithPermission_success() {
        revokeBackgroundReadPermissionForTestApp();
        sendCommandToTestAppReceiver(ACTION_GET_CHANGE_LOG_TOKEN);
        final String token = requireNonNull(TestReceiver.getResult()).getString(EXTRA_TOKEN);
        grantBackgroundReadPermissionForTestApp();

        final Bundle extras = new Bundle();
        extras.putString(EXTRA_TOKEN, token);
        sendCommandToTestAppReceiver(ACTION_GET_CHANGE_LOGS, extras);

        assertSuccess();
    }

    private void sendCommandToTestAppReceiver(String action) {
        sendCommandToTestAppReceiver(action, /*extras=*/ null);
    }

    private void sendCommandToTestAppReceiver(String action, Bundle extras) {
        final Intent intent = new Intent(action).setClassName(PKG_TEST_APP, TEST_APP_RECEIVER);
        if (extras != null) {
            intent.putExtras(extras);
        }
        mContext.sendBroadcast(intent);
    }

    private void grantBackgroundReadPermissionForTestApp() {
        runWithShellPermissionIdentity(
                () ->
                        mPackageManager.grantRuntimePermission(
                                PKG_TEST_APP, READ_HEALTH_DATA_IN_BACKGROUND, mContext.getUser()));
    }

    private void revokeBackgroundReadPermissionForTestApp() {
        runWithShellPermissionIdentity(
                () ->
                        mPackageManager.revokeRuntimePermission(
                                PKG_TEST_APP, READ_HEALTH_DATA_IN_BACKGROUND, mContext.getUser()));
    }

    private void assertSecurityError() {
        assertThat(TestReceiver.getErrorCode()).isEqualTo(ERROR_SECURITY);
        assertThat(TestReceiver.getErrorMessage()).contains(READ_HEALTH_DATA_IN_BACKGROUND);
    }

    private void assertSuccess() {
        assertThat(TestReceiver.getErrorCode()).isNull();
        assertThat(TestReceiver.getErrorMessage()).isNull();
    }
}
