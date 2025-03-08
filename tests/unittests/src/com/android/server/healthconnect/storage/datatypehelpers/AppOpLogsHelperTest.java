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

package com.android.server.healthconnect.storage.datatypehelpers;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import android.app.AppOpsManager;
import android.app.AppOpsManager.HistoricalOps;
import android.content.Context;
import android.content.pm.PackageManager;
import android.health.connect.HealthConnectManager;
import android.health.connect.accesslog.AccessLog;
import android.health.connect.datatypes.HeartRateRecord;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.os.UserHandle;
import android.permission.flags.Flags;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

@RunWith(AndroidJUnit4.class)
public class AppOpLogsHelperTest {

    private static final String TEST_APP_PACKAGE_NAME = "android.health.fitness.app";

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Mock private AppOpsManager mAppOpsManager;
    @Mock private PackageManager mPackageManager;

    private final Context context = ApplicationProvider.getApplicationContext();
    private final Set<String> healthPermissions =
            HealthConnectManager.getHealthPermissions(context);

    @Test
    public void opNameToRecordType_everyGranularOpHasKnownRecordType() {
        for (String healthPermission : healthPermissions) {
            // Skip non-granular permissions.
            String appOp = AppOpsManager.permissionToOp(healthPermission);
            if (appOp.equals(AppOpsManager.OPSTR_READ_WRITE_HEALTH_DATA)) {
                continue;
            }

            // If this test fails, a new granular health AppOp may have been
            // added without adding it to the opNameToRecordType mapping.
            assertThat(AppOpLogsHelper.opNameToRecordType(appOp))
                    .isNotEqualTo(RecordTypeIdentifier.RECORD_TYPE_UNKNOWN);
        }
    }

    @Test
    @RequiresFlagsEnabled({Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED})
    public void getRecordsWithSystemAppOps_returnsRecordsWithSystemAppOps() {
        AppOpLogsHelper appOpLogsHelper =
                new AppOpLogsHelper(mAppOpsManager, mPackageManager, healthPermissions);
        Set<Integer> recordsWithSystemAppOps = appOpLogsHelper.getRecordsWithSystemAppOps();

        assertThat(recordsWithSystemAppOps)
                .containsExactly(
                        RecordTypeIdentifier.RECORD_TYPE_HEART_RATE,
                        RecordTypeIdentifier.RECORD_TYPE_OXYGEN_SATURATION,
                        RecordTypeIdentifier.RECORD_TYPE_SKIN_TEMPERATURE);
    }

    @Test
    @RequiresFlagsEnabled({Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED})
    public void getAccessLogsFromAppOps_returnsAccessLogs() {
        doAnswer(
                        invocation -> {
                            Consumer<HistoricalOps> consumer = invocation.getArgument(2);
                            HistoricalOps historicalOps = new HistoricalOps(1000L, 2000L);
                            historicalOps.addDiscreteAccess(
                                    AppOpsManager.strOpToOp(AppOpsManager.OPSTR_READ_HEART_RATE),
                                    /* uid= */ 10,
                                    TEST_APP_PACKAGE_NAME,
                                    /* attributionTag= */ null,
                                    AppOpsManager.UID_STATE_FOREGROUND_SERVICE,
                                    AppOpsManager.OP_FLAG_TRUSTED_PROXY,
                                    /* discreteAccessTime= */ 1500L,
                                    /* discreteAccessDuration= */ -1);
                            consumer.accept(historicalOps);
                            return null;
                        })
                .when(mAppOpsManager)
                .getHistoricalOps(any(), any(), any());
        UserHandle userHandle = UserHandle.getUserHandleForUid(10);

        AppOpLogsHelper appOpLogsHelper =
                new AppOpLogsHelper(mAppOpsManager, mPackageManager, healthPermissions);
        List<AccessLog> accessLogs = appOpLogsHelper.getAccessLogsFromAppOps(userHandle);

        assertThat(accessLogs.size()).isEqualTo(1);
        AccessLog accessLog = accessLogs.get(0);
        assertThat(accessLog.getPackageName()).isEqualTo(TEST_APP_PACKAGE_NAME);
        assertThat(accessLog.getRecordTypes()).containsExactly(HeartRateRecord.class);
        assertThat(accessLog.getAccessTime()).isEqualTo(Instant.ofEpochMilli(1500L));
        assertThat(accessLog.getOperationType())
                .isEqualTo(AccessLog.OperationType.OPERATION_TYPE_READ);
    }

    @Test
    @RequiresFlagsEnabled({Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED})
    public void getAccessLogsFromAppOps_filtersAgainstUserHandlereturnsAccessLogs() {
        doAnswer(
                        invocation -> {
                            Consumer<HistoricalOps> consumer = invocation.getArgument(2);
                            HistoricalOps historicalOps = new HistoricalOps(1000L, 2000L);
                            historicalOps.addDiscreteAccess(
                                    AppOpsManager.strOpToOp(AppOpsManager.OPSTR_READ_HEART_RATE),
                                    /* uid= */ 10,
                                    TEST_APP_PACKAGE_NAME,
                                    /* attributionTag= */ null,
                                    AppOpsManager.UID_STATE_FOREGROUND_SERVICE,
                                    AppOpsManager.OP_FLAG_TRUSTED_PROXY,
                                    /* discreteAccessTime= */ 1500L,
                                    /* discreteAccessDuration= */ -1);
                            historicalOps.addDiscreteAccess(
                                    AppOpsManager.strOpToOp(AppOpsManager.OPSTR_READ_HEART_RATE),
                                    /* uid= */ 200000,
                                    TEST_APP_PACKAGE_NAME,
                                    /* attributionTag= */ null,
                                    AppOpsManager.UID_STATE_FOREGROUND_SERVICE,
                                    AppOpsManager.OP_FLAG_TRUSTED_PROXY,
                                    /* discreteAccessTime= */ 1000L,
                                    /* discreteAccessDuration= */ -1);
                            consumer.accept(historicalOps);
                            return null;
                        })
                .when(mAppOpsManager)
                .getHistoricalOps(any(), any(), any());
        UserHandle userHandle = UserHandle.getUserHandleForUid(10);

        AppOpLogsHelper appOpLogsHelper =
                new AppOpLogsHelper(mAppOpsManager, mPackageManager, healthPermissions);
        List<AccessLog> accessLogs = appOpLogsHelper.getAccessLogsFromAppOps(userHandle);

        assertThat(accessLogs.size()).isEqualTo(1);
        AccessLog accessLog = accessLogs.get(0);
        assertThat(accessLog.getPackageName()).isEqualTo(TEST_APP_PACKAGE_NAME);
        assertThat(accessLog.getRecordTypes()).containsExactly(HeartRateRecord.class);
        assertThat(accessLog.getAccessTime()).isEqualTo(Instant.ofEpochMilli(1500L));
        assertThat(accessLog.getOperationType())
                .isEqualTo(AccessLog.OperationType.OPERATION_TYPE_READ);
    }
}
