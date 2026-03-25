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

package android.healthconnect.tests.permissions;

import static android.health.connect.HealthPermissions.MANAGE_HEALTH_PERMISSIONS;
import static android.healthconnect.testing.integration.IntegrationTestUtils.grantHealthPermissions;
import static android.healthconnect.testing.integration.IntegrationTestUtils.revokeHealthPermissions;

import static com.android.compatibility.common.util.SystemUtil.eventually;
import static com.android.compatibility.common.util.SystemUtil.runWithShellPermissionIdentity;
import static com.android.healthfitness.flags.Flags.FLAG_HEALTH_PERMISSION_READER_IMPROVEMENTS;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.fail;

import android.Manifest;
import android.content.Context;
import android.health.connect.HealthConnectManager;
import android.health.connect.HealthPermissions;
import android.healthconnect.testing.cts.TestUtils;
import android.healthconnect.testing.shared.AssumptionCheckerRule;
import android.platform.test.annotations.RequiresFlagsDisabled;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;

import androidx.test.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.time.Period;
import java.util.List;

/**
 * Integration tests for {@link HealthConnectManager#getHealthDataHistoricalAccessStartDate}
 * internal API.
 */
@RunWith(AndroidJUnit4.class)
public class GrantTimeIntegrationTest {

    private static final String DEFAULT_APP_PACKAGE = "android.healthconnect.test.app";
    private static final String SHARED_USER_APP = "android.healthconnect.test.app3";

    private static final String DEFAULT_PERM = HealthPermissions.READ_ACTIVE_CALORIES_BURNED;
    private static final String DEFAULT_PERM_2 = HealthPermissions.WRITE_ACTIVE_CALORIES_BURNED;

    private static final Period GRANT_TIME_TO_START_ACCESS_DATE = Period.ofDays(30);

    private Context mContext;
    private HealthConnectManager mHealthConnectManager;

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Rule
    public AssumptionCheckerRule mSupportedHardwareRule =
            new AssumptionCheckerRule(
                    TestUtils::areHealthPermissionsSupported,
                    "Tests should run on supported hardware only.");

    @Before
    public void setUp() throws Exception {
        mContext = InstrumentationRegistry.getTargetContext();
        mHealthConnectManager = mContext.getSystemService(HealthConnectManager.class);

        revokePermissionWithDelay(DEFAULT_APP_PACKAGE, DEFAULT_PERM);
        revokePermissionWithDelay(DEFAULT_APP_PACKAGE, DEFAULT_PERM_2);

        revokePermissionWithDelay(SHARED_USER_APP, DEFAULT_PERM);
        revokePermissionWithDelay(SHARED_USER_APP, DEFAULT_PERM_2);
    }

    @Test(expected = NullPointerException.class)
    @RequiresFlagsDisabled(FLAG_HEALTH_PERMISSION_READER_IMPROVEMENTS)
    public void testGrantHealthPermission_nullPackage_throwsNullPointerException()
            throws Exception {
        getHealthDataHistoricalAccessStartDate(null);
        fail("Expected NullPointerException due to package name is null.");
    }

    @Test(expected = IllegalArgumentException.class)
    @RequiresFlagsDisabled(FLAG_HEALTH_PERMISSION_READER_IMPROVEMENTS)
    public void testGrantHealthPermission_packageNotInstalled_returnsNull() throws Exception {
        getHealthDataHistoricalAccessStartDate("android.invalid.package");
        fail("Expected IllegalArgumentException due to package is not installed.");
    }

    @Test
    @RequiresFlagsDisabled(FLAG_HEALTH_PERMISSION_READER_IMPROVEMENTS)
    public void testGrantHealthPermission_noPermissionsGranted_returnsNull() throws Exception {
        Instant grantTime = getHealthDataHistoricalAccessStartDate(DEFAULT_APP_PACKAGE);
        assertThat(grantTime).isNull();
    }

    @Test
    @RequiresFlagsDisabled(FLAG_HEALTH_PERMISSION_READER_IMPROVEMENTS)
    public void testGrantHealthPermission_permissionGranted_returnsAdequateTime() throws Exception {
        grantHealthPermission(DEFAULT_APP_PACKAGE, DEFAULT_PERM);
        Instant grantTime = getHealthDataHistoricalAccessStartDate(DEFAULT_APP_PACKAGE);
        assertStartAccessDateIsAdequate(grantTime);
    }

    @Test
    @RequiresFlagsDisabled(FLAG_HEALTH_PERMISSION_READER_IMPROVEMENTS)
    public void testGrantHealthPermission_permGrantedViaPackageManager_returnsAdequateTime()
            throws Exception {
        runWithShellPermissionIdentity(
                () ->
                        mContext.getPackageManager()
                                .grantRuntimePermission(
                                        DEFAULT_APP_PACKAGE, DEFAULT_PERM, mContext.getUser()),
                Manifest.permission.GRANT_RUNTIME_PERMISSIONS);
        Instant grantTime = getHealthDataHistoricalAccessStartDate(DEFAULT_APP_PACKAGE);
        assertStartAccessDateIsAdequate(grantTime);
    }

    @Test
    @RequiresFlagsDisabled(FLAG_HEALTH_PERMISSION_READER_IMPROVEMENTS)
    public void testGrantHealthPermission_permissionGrantedToSharedUser_returnsAdequateTime()
            throws Exception {
        grantHealthPermission(DEFAULT_APP_PACKAGE, DEFAULT_PERM);
        Instant grantTime = getHealthDataHistoricalAccessStartDate(DEFAULT_APP_PACKAGE);
        assertStartAccessDateIsAdequate(grantTime);
    }

    @Test
    @RequiresFlagsDisabled(FLAG_HEALTH_PERMISSION_READER_IMPROVEMENTS)
    public void testGrantHealthPermission_permissionGrantedForSharedUser_returnsAdequateTime()
            throws Exception {
        grantHealthPermission(SHARED_USER_APP, DEFAULT_PERM);
        Instant grantTime = getHealthDataHistoricalAccessStartDate(SHARED_USER_APP);
        assertThat(grantTime).isNotNull();
        assertThat(grantTime.compareTo(Instant.now())).isLessThan(0);
    }

    @Test
    @RequiresFlagsDisabled(FLAG_HEALTH_PERMISSION_READER_IMPROVEMENTS)
    public void testGrantHealthPermission_twoPermissionsGranted_returnsTheSameTime()
            throws Exception {
        grantHealthPermission(DEFAULT_APP_PACKAGE, DEFAULT_PERM);
        Instant grantTime = getHealthDataHistoricalAccessStartDate(DEFAULT_APP_PACKAGE);
        grantHealthPermission(DEFAULT_APP_PACKAGE, DEFAULT_PERM_2);
        Instant grantTime2 = getHealthDataHistoricalAccessStartDate(DEFAULT_APP_PACKAGE);
        assertThat(grantTime).isEqualTo(grantTime2);
    }

    @Test
    @RequiresFlagsDisabled(FLAG_HEALTH_PERMISSION_READER_IMPROVEMENTS)
    public void testGrantHealthPermission_notAllPermissionsRevoked_returnsTheSameTime()
            throws Exception {
        grantHealthPermission(DEFAULT_APP_PACKAGE, DEFAULT_PERM);
        Instant grantTime = getHealthDataHistoricalAccessStartDate(DEFAULT_APP_PACKAGE);
        grantHealthPermission(DEFAULT_APP_PACKAGE, DEFAULT_PERM_2);
        Instant grantTime2 = getHealthDataHistoricalAccessStartDate(DEFAULT_APP_PACKAGE);
        assertThat(grantTime).isEqualTo(grantTime2);
        revokePermissionWithDelay(DEFAULT_APP_PACKAGE, DEFAULT_PERM);
        Instant grantTime3 = getHealthDataHistoricalAccessStartDate(DEFAULT_APP_PACKAGE);
        assertThat(grantTime3).isEqualTo(grantTime);
    }

    @Test
    @RequiresFlagsDisabled(FLAG_HEALTH_PERMISSION_READER_IMPROVEMENTS)
    public void testGrantHealthPermission_permissionGrantedAndRevoked_resetGrantTime()
            throws Exception {
        grantHealthPermission(DEFAULT_APP_PACKAGE, DEFAULT_PERM);
        revokePermissionWithDelay(DEFAULT_APP_PACKAGE, DEFAULT_PERM);
        Instant grantTime = getHealthDataHistoricalAccessStartDate(DEFAULT_APP_PACKAGE);
        assertThat(grantTime).isNull();
    }

    @Test
    @RequiresFlagsDisabled(FLAG_HEALTH_PERMISSION_READER_IMPROVEMENTS)
    public void testGrantHealthPermission_permissionWasRegranted_timeChanged() throws Exception {
        grantHealthPermission(DEFAULT_APP_PACKAGE, DEFAULT_PERM);
        Instant grantTime = getHealthDataHistoricalAccessStartDate(DEFAULT_APP_PACKAGE);
        revokePermissionWithDelay(DEFAULT_APP_PACKAGE, DEFAULT_PERM);
        grantHealthPermission(DEFAULT_APP_PACKAGE, DEFAULT_PERM);
        Instant grantTime2 = getHealthDataHistoricalAccessStartDate(DEFAULT_APP_PACKAGE);

        eventually(() -> assertThat(grantTime.isBefore(grantTime2)).isTrue());
    }

    @Test(expected = NullPointerException.class)
    @RequiresFlagsEnabled(FLAG_HEALTH_PERMISSION_READER_IMPROVEMENTS)
    public void testGrantHealthPermissions_nullPackage_throwsNullPointerException()
            throws Exception {
        grantHealthPermissions(null, List.of(DEFAULT_PERM));
        fail("Expected NullPointerException due to package name is null.");
    }

    @Test(expected = IllegalArgumentException.class)
    @RequiresFlagsEnabled(FLAG_HEALTH_PERMISSION_READER_IMPROVEMENTS)
    public void testGrantHealthPermissions_packageNotInstalled_throwsIllegalArgumentException()
            throws Exception {
        grantHealthPermissions("android.invalid.package", List.of(DEFAULT_PERM));
        fail("Expected IllegalArgumentException due to uninstalled package.");
    }

    @Test(expected = NullPointerException.class)
    @RequiresFlagsEnabled(FLAG_HEALTH_PERMISSION_READER_IMPROVEMENTS)
    public void testRevokeHealthPermissions_nullPackage_throwsNullPointerException()
            throws Exception {
        revokeHealthPermissions(null, List.of(DEFAULT_PERM), "");
        fail("Expected NullPointerException due to package name is null.");
    }

    @Test(expected = IllegalArgumentException.class)
    @RequiresFlagsEnabled(FLAG_HEALTH_PERMISSION_READER_IMPROVEMENTS)
    public void testRevokeHealthPermissions_packageNotInstalled_throwsIllegalArgumentException()
            throws Exception {
        revokeHealthPermissions("android.invalid.package", List.of(DEFAULT_PERM), "");
        fail("Expected IllegalArgumentException due to uninstalled package.");
    }

    @Test
    @RequiresFlagsEnabled(FLAG_HEALTH_PERMISSION_READER_IMPROVEMENTS)
    public void testGrantHealthPermissions_permissionGranted_returnsAdequateTime()
            throws Exception {
        grantHealthPermissions(DEFAULT_APP_PACKAGE, List.of(DEFAULT_PERM));
        Instant grantTime = getHealthDataHistoricalAccessStartDate(DEFAULT_APP_PACKAGE);
        assertStartAccessDateIsAdequate(grantTime);
    }

    @Test
    @RequiresFlagsEnabled(FLAG_HEALTH_PERMISSION_READER_IMPROVEMENTS)
    public void testGrantHealthPermissions_multiplePermissionsGranted_returnsAdequateTime()
            throws Exception {
        grantHealthPermissions(DEFAULT_APP_PACKAGE, List.of(DEFAULT_PERM, DEFAULT_PERM_2));
        Instant grantTime = getHealthDataHistoricalAccessStartDate(DEFAULT_APP_PACKAGE);
        assertStartAccessDateIsAdequate(grantTime);
    }

    @Test
    @RequiresFlagsEnabled(FLAG_HEALTH_PERMISSION_READER_IMPROVEMENTS)
    public void testGrantHealthPermissions_permissionsGrantedToSharedUser_returnsAdequateTime()
            throws Exception {
        grantHealthPermissions(DEFAULT_APP_PACKAGE, List.of(DEFAULT_PERM));
        Instant grantTime = getHealthDataHistoricalAccessStartDate(DEFAULT_APP_PACKAGE);
        assertStartAccessDateIsAdequate(grantTime);
    }

    @Test
    @RequiresFlagsEnabled(FLAG_HEALTH_PERMISSION_READER_IMPROVEMENTS)
    public void testGrantHealthPermissions_permissionsGrantedForSharedUser_returnsAdequateTime()
            throws Exception {
        grantHealthPermissions(SHARED_USER_APP, List.of(DEFAULT_PERM));
        Instant grantTime = getHealthDataHistoricalAccessStartDate(SHARED_USER_APP);
        assertThat(grantTime).isNotNull();
        assertThat(grantTime.compareTo(Instant.now())).isLessThan(0);
    }

    @Test
    @RequiresFlagsEnabled(FLAG_HEALTH_PERMISSION_READER_IMPROVEMENTS)
    public void testGrantHealthPermissions_twoPermissionsGranted_returnsTheSameTime()
            throws Exception {
        grantHealthPermissions(DEFAULT_APP_PACKAGE, List.of(DEFAULT_PERM));
        Instant grantTime = getHealthDataHistoricalAccessStartDate(DEFAULT_APP_PACKAGE);
        grantHealthPermissions(DEFAULT_APP_PACKAGE, List.of(DEFAULT_PERM_2));
        Instant grantTime2 = getHealthDataHistoricalAccessStartDate(DEFAULT_APP_PACKAGE);
        assertThat(grantTime).isEqualTo(grantTime2);
    }

    @Test
    @RequiresFlagsEnabled(FLAG_HEALTH_PERMISSION_READER_IMPROVEMENTS)
    public void testGrantHealthPermissions_notAllPermissionsRevoked_returnsTheSameTime()
            throws Exception {
        grantHealthPermissions(DEFAULT_APP_PACKAGE, List.of(DEFAULT_PERM, DEFAULT_PERM_2));
        Instant grantTime = getHealthDataHistoricalAccessStartDate(DEFAULT_APP_PACKAGE);
        revokePermissionsWithDelay(DEFAULT_APP_PACKAGE, List.of(DEFAULT_PERM));

        Instant grantTime2 = getHealthDataHistoricalAccessStartDate(DEFAULT_APP_PACKAGE);
        assertThat(grantTime2).isEqualTo(grantTime);
    }

    @Test
    @RequiresFlagsEnabled(FLAG_HEALTH_PERMISSION_READER_IMPROVEMENTS)
    public void testGrantHealthPermissions_allPermissionsRevoked_resetGrantTime() throws Exception {
        grantHealthPermissions(DEFAULT_APP_PACKAGE, List.of(DEFAULT_PERM, DEFAULT_PERM_2));
        revokePermissionsWithDelay(DEFAULT_APP_PACKAGE, List.of(DEFAULT_PERM, DEFAULT_PERM_2));

        Instant grantTime = getHealthDataHistoricalAccessStartDate(DEFAULT_APP_PACKAGE);
        assertThat(grantTime).isNull();
    }

    @Test
    @RequiresFlagsEnabled(FLAG_HEALTH_PERMISSION_READER_IMPROVEMENTS)
    public void testGrantHealthPermissions_permissionGrantedAndRevoked_resetGrantTime()
            throws Exception {
        grantHealthPermissions(DEFAULT_APP_PACKAGE, List.of(DEFAULT_PERM));
        revokePermissionsWithDelay(DEFAULT_APP_PACKAGE, List.of(DEFAULT_PERM));

        Instant grantTime = getHealthDataHistoricalAccessStartDate(DEFAULT_APP_PACKAGE);
        assertThat(grantTime).isNull();
    }

    @Test
    @RequiresFlagsEnabled(FLAG_HEALTH_PERMISSION_READER_IMPROVEMENTS)
    public void testGrantHealthPermissions_permissionWasRegranted_timeChanged() throws Exception {
        grantHealthPermissions(DEFAULT_APP_PACKAGE, List.of(DEFAULT_PERM));
        Instant grantTime = getHealthDataHistoricalAccessStartDate(DEFAULT_APP_PACKAGE);
        revokeHealthPermissions(DEFAULT_APP_PACKAGE, List.of(DEFAULT_PERM), "");
        grantHealthPermissions(DEFAULT_APP_PACKAGE, List.of(DEFAULT_PERM));

        eventually(
                () -> {
                    Instant grantTime2 =
                            getHealthDataHistoricalAccessStartDate(DEFAULT_APP_PACKAGE);
                    assertThat(grantTime.isBefore(grantTime2)).isTrue();
                });
    }

    void assertStartAccessDateIsAdequate(Instant firstGrantTime) {
        assertThat(firstGrantTime).isNotNull();
        Instant currentTime = Instant.now();
        Period lowerBound = GRANT_TIME_TO_START_ACCESS_DATE.minusDays(1);
        Period upperBound = GRANT_TIME_TO_START_ACCESS_DATE.plusDays(1);
        assertThat(currentTime.minus(lowerBound).isAfter(firstGrantTime)).isTrue();
        assertThat(currentTime.minus(upperBound).isBefore(firstGrantTime)).isTrue();
    }

    private void grantHealthPermission(String packageName, String permName) {
        try {
            runWithShellPermissionIdentity(
                    () -> {
                        mHealthConnectManager.grantHealthPermission(packageName, permName);
                    },
                    MANAGE_HEALTH_PERMISSIONS);
        } catch (RuntimeException e) {
            // runWithShellPermissionIdentity wraps and rethrows all exceptions as RuntimeException,
            // but we need the original RuntimeException if there is one.
            final Throwable cause = e.getCause();
            throw cause instanceof RuntimeException ? (RuntimeException) cause : e;
        }
    }

    private Instant getHealthDataHistoricalAccessStartDate(String packageName) {
        try {
            return runWithShellPermissionIdentity(
                    () -> mHealthConnectManager.getHealthDataHistoricalAccessStartDate(packageName),
                    MANAGE_HEALTH_PERMISSIONS);
        } catch (RuntimeException e) {
            // runWithShellPermissionIdentity wraps and rethrows all exceptions as RuntimeException,
            // but we need the original RuntimeException if there is one.
            final Throwable cause = e.getCause();
            throw cause instanceof RuntimeException ? (RuntimeException) cause : e;
        }
    }

    private void revokePermissionWithDelay(String packageName, String permName) throws Exception {
        try {
            runWithShellPermissionIdentity(
                    () -> {
                        mHealthConnectManager.revokeHealthPermission(packageName, permName, "");
                    },
                    MANAGE_HEALTH_PERMISSIONS);
        } catch (RuntimeException e) {
            // runWithShellPermissionIdentity wraps and rethrows all exceptions as RuntimeException,
            // but we need the original RuntimeException if there is one.
            final Throwable cause = e.getCause();
            throw cause instanceof RuntimeException ? (RuntimeException) cause : e;
        }
        // Wait some time for callbacks to propagate.
        Thread.sleep(50);
    }

    private void revokePermissionsWithDelay(String packageName, List<String> permNames)
            throws Exception {
        revokeHealthPermissions(packageName, permNames, "");
        // Wait some time for callbacks to propagate.
        Thread.sleep(50);
    }
}
