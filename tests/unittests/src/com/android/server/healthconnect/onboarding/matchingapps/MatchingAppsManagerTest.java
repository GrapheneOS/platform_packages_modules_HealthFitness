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
package com.android.server.healthconnect.onboarding.matchingapps;

import static android.content.pm.PackageManager.FLAG_PERMISSION_USER_FIXED;
import static android.content.pm.PackageManager.FLAG_PERMISSION_USER_SET;
import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.health.connect.HealthPermissions.READ_DISTANCE;
import static android.health.connect.HealthPermissions.READ_HEART_RATE;
import static android.health.connect.HealthPermissions.READ_STEPS;
import static android.health.connect.HealthPermissions.WRITE_DISTANCE;
import static android.health.connect.HealthPermissions.WRITE_HEART_RATE;
import static android.health.connect.HealthPermissions.WRITE_STEPS;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.health.connect.datatypes.DistanceRecord;
import android.health.connect.datatypes.ExerciseSessionRecord;
import android.health.connect.datatypes.HeartRateRecord;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.SkinTemperatureRecord;
import android.health.connect.datatypes.StepsRecord;
import android.health.connect.internal.datatypes.utils.HealthConnectMappings;

import androidx.test.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.server.healthconnect.permission.HealthConnectPermissionHelper;
import com.android.server.healthconnect.permission.PackageInfoUtils;
import com.android.server.healthconnect.storage.HealthConnectContext;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/** Test class for the MatchingAppsManager class. */
@RunWith(AndroidJUnit4.class)
public class MatchingAppsManagerTest {

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock private HealthConnectPermissionHelper mHealthConnectPermissionHelper;
    @Mock private PackageInfoUtils mPackageInfoUtils;
    @Mock private PackageManager mPackageManager;

    private final HealthConnectMappings mHealthConnectMappings = new HealthConnectMappings();

    private MatchingAppsManager mMatchingAppsManager;

    private static final String PACKAGE_NAME = "com.example.app";
    private static final String PACKAGE_NAME_2 = "com.example.app2";
    private static final String PACKAGE_NAME_3 = "com.example.app3";

    @Before
    public void setUp() throws PackageManager.NameNotFoundException {
        Context context = InstrumentationRegistry.getTargetContext();
        HealthConnectContext userContext =
                HealthConnectContext.create(context, context.getUser(), null, null);
        mMatchingAppsManager =
                new MatchingAppsManager(
                        userContext,
                        mHealthConnectPermissionHelper,
                        mPackageInfoUtils,
                        mHealthConnectMappings,
                        mPackageManager);
    }

    @After
    public void tearDown() {
        clearInvocations(mHealthConnectPermissionHelper, mPackageInfoUtils, mPackageManager);
    }

    @Test
    public void constructor_initializesFields() {
        assertThat(mMatchingAppsManager).isNotNull();
    }

    @Test
    public void noReadPermissionsRequested_returnsFalse() {
        mockReadingApp(PACKAGE_NAME, Collections.emptyList());

        boolean result =
                mMatchingAppsManager.canConnectMatchingApps(Collections.emptySet(), PACKAGE_NAME);

        assertThat(result).isFalse();
    }

    @Test
    public void noReadPermissionForRequestedRecordType_returnFalse() {
        mockReadingApp(PACKAGE_NAME, ImmutableList.of(READ_STEPS));

        PackageInfo compatibleApp = createPackageInfo(PACKAGE_NAME_2, new String[] {WRITE_STEPS});
        mockCompatibleHealthConnectApps(ImmutableList.of(compatibleApp));
        mockAppSystemStatus(PACKAGE_NAME_2, /* isSystemApp= */ false);
        mockPermissionCheckResult(PACKAGE_NAME_2, WRITE_STEPS, PERMISSION_DENIED);
        mockHealthPermissionFlags(PACKAGE_NAME_2, WRITE_STEPS, 0);

        boolean result =
                mMatchingAppsManager.canConnectMatchingApps(
                        ImmutableSet.of(DistanceRecord.class), PACKAGE_NAME);

        assertThat(result).isFalse();
    }

    @Test
    public void noMatchingWritePermission_returnsFalse() {
        mockReadingApp(PACKAGE_NAME, ImmutableList.of(READ_STEPS));

        PackageInfo nonMatchingApp =
                createPackageInfo(PACKAGE_NAME_2, new String[] {WRITE_HEART_RATE});
        mockCompatibleHealthConnectApps(ImmutableList.of(nonMatchingApp));
        mockAppSystemStatus(PACKAGE_NAME_2, /* isSystemApp= */ false);
        mockPermissionCheckResult(PACKAGE_NAME_2, WRITE_HEART_RATE, PERMISSION_DENIED);
        mockHealthPermissionFlags(PACKAGE_NAME_2, WRITE_HEART_RATE, 0);

        boolean result =
                mMatchingAppsManager.canConnectMatchingApps(Collections.emptySet(), PACKAGE_NAME);

        assertThat(result).isFalse();
    }

    @Test
    public void matchExists_returnsTrue() {
        mockReadingApp(PACKAGE_NAME, ImmutableList.of(READ_STEPS));

        PackageInfo matchingApp = createPackageInfo(PACKAGE_NAME_2, new String[] {WRITE_STEPS});
        mockCompatibleHealthConnectApps(ImmutableList.of(matchingApp));
        mockAppSystemStatus(PACKAGE_NAME_2, /* isSystemApp= */ false);
        mockPermissionCheckResult(PACKAGE_NAME_2, WRITE_STEPS, PERMISSION_DENIED);
        mockHealthPermissionFlags(PACKAGE_NAME_2, WRITE_STEPS, 0);

        boolean result =
                mMatchingAppsManager.canConnectMatchingApps(Collections.emptySet(), PACKAGE_NAME);

        assertThat(result).isTrue();
    }

    @Test
    public void systemAppWouldMatch_returnsFalse() {
        mockReadingApp(PACKAGE_NAME, ImmutableList.of(READ_STEPS));

        PackageInfo matchingApp = createPackageInfo(PACKAGE_NAME_2, new String[] {WRITE_STEPS});
        mockCompatibleHealthConnectApps(ImmutableList.of(matchingApp));
        mockAppSystemStatus(PACKAGE_NAME_2, /* isSystemApp= */ true);
        mockPermissionCheckResult(PACKAGE_NAME_2, WRITE_STEPS, PERMISSION_DENIED);
        mockHealthPermissionFlags(PACKAGE_NAME_2, WRITE_STEPS, 0);

        boolean result =
                mMatchingAppsManager.canConnectMatchingApps(Collections.emptySet(), PACKAGE_NAME);

        assertThat(result).isFalse();
    }

    @Test
    public void matchingWritePermissionAlreadyGranted_returnsFalse() {
        mockReadingApp(PACKAGE_NAME, ImmutableList.of(READ_STEPS));

        PackageInfo matchingApp = createPackageInfo(PACKAGE_NAME_2, new String[] {WRITE_STEPS});
        mockCompatibleHealthConnectApps(ImmutableList.of(matchingApp));
        mockAppSystemStatus(PACKAGE_NAME_2, /* isSystemApp= */ false);
        mockPermissionCheckResult(PACKAGE_NAME_2, WRITE_STEPS, PERMISSION_GRANTED);
        mockHealthPermissionFlags(PACKAGE_NAME_2, WRITE_STEPS, 0);

        boolean result =
                mMatchingAppsManager.canConnectMatchingApps(Collections.emptySet(), PACKAGE_NAME);

        assertThat(result).isFalse();
    }

    @Test
    public void matchingWritePermissionUserFixed_returnsFalse() {
        mockReadingApp(PACKAGE_NAME, ImmutableList.of(READ_STEPS));

        PackageInfo matchingApp = createPackageInfo(PACKAGE_NAME_2, new String[] {WRITE_STEPS});
        mockCompatibleHealthConnectApps(ImmutableList.of(matchingApp));
        mockAppSystemStatus(PACKAGE_NAME_2, /* isSystemApp= */ false);
        mockPermissionCheckResult(PACKAGE_NAME_2, WRITE_STEPS, PERMISSION_DENIED);
        mockHealthPermissionFlags(PACKAGE_NAME_2, WRITE_STEPS, FLAG_PERMISSION_USER_FIXED);

        boolean result =
                mMatchingAppsManager.canConnectMatchingApps(Collections.emptySet(), PACKAGE_NAME);

        assertThat(result).isFalse();
    }

    @Test
    public void matchingWritePermissionUserSet_returnsTrue() {
        mockReadingApp(PACKAGE_NAME, ImmutableList.of(READ_STEPS));

        PackageInfo matchingApp = createPackageInfo(PACKAGE_NAME_2, new String[] {WRITE_STEPS});
        mockCompatibleHealthConnectApps(ImmutableList.of(matchingApp));
        mockAppSystemStatus(PACKAGE_NAME_2, /* isSystemApp= */ false);
        mockPermissionCheckResult(PACKAGE_NAME_2, WRITE_STEPS, PERMISSION_DENIED);
        mockHealthPermissionFlags(PACKAGE_NAME_2, WRITE_STEPS, FLAG_PERMISSION_USER_SET);

        boolean result =
                mMatchingAppsManager.canConnectMatchingApps(Collections.emptySet(), PACKAGE_NAME);

        assertThat(result).isTrue();
    }

    @Test
    public void callingAppCannotReadSomeRecordTypes_returnTrue() {
        // Calling app can read HEART_RATE.
        mockReadingApp(PACKAGE_NAME, ImmutableList.of(READ_HEART_RATE, WRITE_STEPS));

        // Writing app could write STEPS and HEART_RATE.
        PackageInfo matchingApp =
                createPackageInfo(PACKAGE_NAME_2, new String[] {WRITE_STEPS, WRITE_HEART_RATE});
        mockCompatibleHealthConnectApps(ImmutableList.of(matchingApp));
        mockAppSystemStatus(PACKAGE_NAME_2, /* isSystemApp= */ false);
        mockPermissionCheckResult(PACKAGE_NAME_2, WRITE_STEPS, PERMISSION_DENIED);
        mockHealthPermissionFlags(PACKAGE_NAME_2, WRITE_STEPS, 0);
        mockPermissionCheckResult(PACKAGE_NAME_2, WRITE_HEART_RATE, PERMISSION_DENIED);
        mockHealthPermissionFlags(PACKAGE_NAME_2, WRITE_HEART_RATE, 0);

        boolean result =
                mMatchingAppsManager.canConnectMatchingApps(Collections.emptySet(), PACKAGE_NAME);

        assertThat(result).isTrue();
    }

    @Test
    public void queryOneRecordType_matchExists_returnTrue() {
        // Calling app can read HEART_RATE, DISTANCE and STEPS.
        mockReadingApp(
                PACKAGE_NAME,
                ImmutableList.of(
                        READ_HEART_RATE, READ_DISTANCE, READ_STEPS, WRITE_STEPS, WRITE_DISTANCE));

        // Writing app could write HEART_RATE, DISTANCE and STEPS.
        PackageInfo matchingApp =
                createPackageInfo(
                        PACKAGE_NAME_2,
                        new String[] {WRITE_STEPS, READ_STEPS, WRITE_DISTANCE, WRITE_HEART_RATE});
        mockCompatibleHealthConnectApps(ImmutableList.of(matchingApp));
        mockAppSystemStatus(PACKAGE_NAME_2, /* isSystemApp= */ false);
        mockPermissionCheckResult(PACKAGE_NAME_2, WRITE_STEPS, PERMISSION_DENIED);
        mockHealthPermissionFlags(PACKAGE_NAME_2, WRITE_STEPS, 0);
        mockPermissionCheckResult(PACKAGE_NAME_2, WRITE_DISTANCE, PERMISSION_DENIED);
        mockHealthPermissionFlags(PACKAGE_NAME_2, WRITE_DISTANCE, 0);
        mockPermissionCheckResult(PACKAGE_NAME_2, WRITE_HEART_RATE, PERMISSION_DENIED);
        mockHealthPermissionFlags(PACKAGE_NAME_2, WRITE_HEART_RATE, 0);

        // STEPS requested.
        boolean result =
                mMatchingAppsManager.canConnectMatchingApps(
                        Set.of(StepsRecord.class), PACKAGE_NAME);

        assertThat(result).isTrue();
    }

    @Test
    public void queryTooMantRecordTypes_matchExistsForOne_returnTrue() {
        // Calling app can read HEART_RATE, DISTANCE and STEPS.
        mockReadingApp(
                PACKAGE_NAME,
                ImmutableList.of(
                        READ_HEART_RATE, READ_DISTANCE, READ_STEPS, WRITE_STEPS, WRITE_DISTANCE));

        // Writing app could write HEART_RATE, DISTANCE and STEPS.
        PackageInfo matchingApp =
                createPackageInfo(
                        PACKAGE_NAME_2,
                        new String[] {WRITE_STEPS, READ_STEPS, WRITE_DISTANCE, WRITE_HEART_RATE});
        mockCompatibleHealthConnectApps(ImmutableList.of(matchingApp));
        mockAppSystemStatus(PACKAGE_NAME_2, /* isSystemApp= */ false);
        mockPermissionCheckResult(PACKAGE_NAME_2, WRITE_STEPS, PERMISSION_DENIED);
        mockHealthPermissionFlags(PACKAGE_NAME_2, WRITE_STEPS, 0);
        mockPermissionCheckResult(PACKAGE_NAME_2, WRITE_DISTANCE, PERMISSION_DENIED);
        mockHealthPermissionFlags(PACKAGE_NAME_2, WRITE_DISTANCE, 0);
        mockPermissionCheckResult(PACKAGE_NAME_2, WRITE_HEART_RATE, PERMISSION_DENIED);
        mockHealthPermissionFlags(PACKAGE_NAME_2, WRITE_HEART_RATE, 0);

        Set<Class<? extends Record>> queriedRecordTypes =
                Set.of(StepsRecord.class, ExerciseSessionRecord.class, SkinTemperatureRecord.class);
        boolean result =
                mMatchingAppsManager.canConnectMatchingApps(queriedRecordTypes, PACKAGE_NAME);

        assertThat(result).isTrue();
    }

    @Test
    public void queryNonEmptySet_oneUserFixedTwoMatchingWritePermissions_returnTrue() {
        // Calling app can read HEART_RATE, DISTANCE and STEPS.
        mockReadingApp(
                PACKAGE_NAME,
                ImmutableList.of(
                        READ_HEART_RATE, READ_DISTANCE, READ_STEPS, WRITE_STEPS, WRITE_DISTANCE));

        // Writing app could write HEART_RATE, DISTANCE and STEPS.
        PackageInfo matchingApp =
                createPackageInfo(
                        PACKAGE_NAME_2,
                        new String[] {WRITE_STEPS, READ_STEPS, WRITE_DISTANCE, WRITE_HEART_RATE});
        mockCompatibleHealthConnectApps(ImmutableList.of(matchingApp));
        mockAppSystemStatus(PACKAGE_NAME_2, /* isSystemApp= */ false);
        mockPermissionCheckResult(PACKAGE_NAME_2, WRITE_STEPS, PERMISSION_DENIED);
        mockHealthPermissionFlags(PACKAGE_NAME_2, WRITE_STEPS, 0);
        mockPermissionCheckResult(PACKAGE_NAME_2, WRITE_DISTANCE, PERMISSION_DENIED);
        mockHealthPermissionFlags(PACKAGE_NAME_2, WRITE_DISTANCE, FLAG_PERMISSION_USER_FIXED);
        mockPermissionCheckResult(PACKAGE_NAME_2, WRITE_HEART_RATE, PERMISSION_DENIED);
        mockHealthPermissionFlags(PACKAGE_NAME_2, WRITE_HEART_RATE, FLAG_PERMISSION_USER_FIXED);

        boolean result =
                mMatchingAppsManager.canConnectMatchingApps(
                        Set.of(StepsRecord.class, DistanceRecord.class, HeartRateRecord.class),
                        PACKAGE_NAME);
        assertThat(result).isTrue();
    }

    @Test
    public void multipleWritingApps_oneMatching_returnTrue() {
        // Calling app can read HEART_RATE and DISTANCE.
        mockReadingApp(PACKAGE_NAME, ImmutableList.of(READ_HEART_RATE, READ_DISTANCE));

        PackageInfo matchingApp =
                createPackageInfo(PACKAGE_NAME_2, new String[] {WRITE_HEART_RATE});
        PackageInfo nonMatchingApp = createPackageInfo(PACKAGE_NAME_3, new String[] {WRITE_STEPS});
        mockCompatibleHealthConnectApps(ImmutableList.of(matchingApp, nonMatchingApp));

        // Matching app could write HEART_RATE.
        mockAppSystemStatus(PACKAGE_NAME_2, /* isSystemApp= */ false);
        mockPermissionCheckResult(PACKAGE_NAME_2, WRITE_HEART_RATE, PERMISSION_DENIED);
        mockHealthPermissionFlags(PACKAGE_NAME_2, WRITE_HEART_RATE, 0);

        // Non matching app could write STEPS.
        mockAppSystemStatus(PACKAGE_NAME_3, /* isSystemApp= */ false);
        mockPermissionCheckResult(PACKAGE_NAME_3, WRITE_STEPS, PERMISSION_DENIED);
        mockHealthPermissionFlags(PACKAGE_NAME_3, WRITE_STEPS, 0);

        // Querying STEPS, DISTANCE, HEART_RATE.
        boolean result =
                mMatchingAppsManager.canConnectMatchingApps(
                        Set.of(StepsRecord.class, DistanceRecord.class, HeartRateRecord.class),
                        PACKAGE_NAME);

        // Match exists for HEART_RATE.
        assertThat(result).isTrue();
    }

    private void mockReadingApp(String packageName, List<String> permissions) {
        when(mHealthConnectPermissionHelper.getGrantedHealthPermissions(eq(packageName), any()))
                .thenReturn(permissions);
    }

    private void mockCompatibleHealthConnectApps(List<PackageInfo> apps) {
        when(mPackageInfoUtils.getPackagesCompatibleWithHealthConnect(any(), any()))
                .thenReturn(apps);
        apps.forEach(
                app ->
                        when(mHealthConnectPermissionHelper.isRequestingFitnessPermission(app))
                                .thenReturn(true));
    }

    private void mockAppSystemStatus(String packageName, boolean isSystemApp) {
        when(mHealthConnectPermissionHelper.hasNonUserSensitiveHealthPermission(
                        eq(packageName), any(), any()))
                .thenReturn(isSystemApp);
    }

    private void mockPermissionCheckResult(String packageName, String permission, int result) {
        when(mPackageManager.checkPermission(eq(permission), eq(packageName))).thenReturn(result);
    }

    private void mockHealthPermissionFlags(String packageName, String permission, int flags) {
        when(mHealthConnectPermissionHelper.getHealthPermissionFlags(
                        eq(packageName), any(), eq(permission)))
                .thenReturn(flags);
    }

    private PackageInfo createPackageInfo(String packageName, String[] requestedPermissions) {
        PackageInfo pkgInfo = new PackageInfo();
        pkgInfo.packageName = packageName;
        pkgInfo.firstInstallTime = 0;
        pkgInfo.requestedPermissions = requestedPermissions;
        return pkgInfo;
    }
}
