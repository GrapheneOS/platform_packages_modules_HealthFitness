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
package com.android.server.healthconnect.onboarding.matchmaking;

import static android.content.pm.PackageManager.FLAG_PERMISSION_USER_FIXED;
import static android.content.pm.PackageManager.FLAG_PERMISSION_USER_SET;
import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.health.connect.HealthDataCategory.ACTIVITY;
import static android.health.connect.HealthDataCategory.CYCLE_TRACKING;
import static android.health.connect.HealthDataCategory.NUTRITION;
import static android.health.connect.HealthDataCategory.SLEEP;
import static android.health.connect.HealthDataCategory.VITALS;
import static android.health.connect.HealthPermissions.READ_DISTANCE;
import static android.health.connect.HealthPermissions.READ_HEART_RATE;
import static android.health.connect.HealthPermissions.READ_SLEEP;
import static android.health.connect.HealthPermissions.READ_STEPS;
import static android.health.connect.HealthPermissions.WRITE_DISTANCE;
import static android.health.connect.HealthPermissions.WRITE_EXERCISE;
import static android.health.connect.HealthPermissions.WRITE_HEART_RATE;
import static android.health.connect.HealthPermissions.WRITE_MENSTRUATION;
import static android.health.connect.HealthPermissions.WRITE_NUTRITION;
import static android.health.connect.HealthPermissions.WRITE_SLEEP;
import static android.health.connect.HealthPermissions.WRITE_STEPS;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.health.connect.DeviceDataProviderInfo;
import android.health.connect.DeviceDataSourceInfo;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.Device;
import android.health.connect.datatypes.DistanceRecord;
import android.health.connect.datatypes.ExerciseSessionRecord;
import android.health.connect.datatypes.HeartRateRecord;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.SkinTemperatureRecord;
import android.health.connect.datatypes.SleepSessionRecord;
import android.health.connect.datatypes.StepsRecord;
import android.health.connect.device.DeviceDataTypeAdvertisement;
import android.health.connect.internal.datatypes.utils.HealthConnectMappings;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.healthfitness.flags.Flags;
import com.android.server.healthconnect.device.DeviceDataProviderManager;
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
import java.util.Map;
import java.util.Set;

/** Test class for the {@link MatchmakingManager} class. */
@RunWith(AndroidJUnit4.class)
public class MatchmakingManagerTest {

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Mock private HealthConnectPermissionHelper mHealthConnectPermissionHelper;
    @Mock private PackageInfoUtils mPackageInfoUtils;
    @Mock private MatchmakingDenialStateManager mMatchmakingDenialStateManager;

    private final HealthConnectMappings mHealthConnectMappings = new HealthConnectMappings();

    private MatchmakingManager mMatchmakingManager;
    @Mock private DeviceDataProviderManager mDeviceDataProviderManager;

    private static final String PACKAGE_NAME = "com.example.app";
    private static final String PACKAGE_NAME_2 = "com.example.app2";
    private static final String PACKAGE_NAME_3 = "com.example.app3";

    private static final String DEVICE_PACKAGE_NAME = "device.package.name";
    private static final String DEVICE_PACKAGE_NAME_2 = "device.package.name2";
    private static final String DEVICE_PACKAGE_NAME_3 = "device.package.name3";

    private static final String DEVICE_DATA_PROVIDER_PACKAGE_NAME = "ddp.package.name";
    private static final String DEVICE_DATA_PROVIDER_PACKAGE_NAME_2 = "ddp.package.name2";
    private static final String DEVICE_DATA_PROVIDER_PACKAGE_NAME_3 = "ddp.package.name3";

    private static final String DEVICE_ID = "device.id";
    private static final String DEVICE_ID_2 = "device.id2";
    private static final String DEVICE_ID_3 = "device.id3";

    @Before
    public void setUp() throws PackageManager.NameNotFoundException {
        Context context = InstrumentationRegistry.getTargetContext();
        HealthConnectContext userContext =
                HealthConnectContext.create(context, context.getUser(), null, null);
        mMatchmakingManager =
                new MatchmakingManager(
                        userContext,
                        mHealthConnectPermissionHelper,
                        mPackageInfoUtils,
                        mHealthConnectMappings,
                        mMatchmakingDenialStateManager,
                        mDeviceDataProviderManager);
        when(mMatchmakingDenialStateManager.isMatchmakingPaused(anyString(), anyString(), anyInt()))
                .thenReturn(false);
        when(mMatchmakingDenialStateManager.isMatchmakingForDevicePaused(anyString(), anyString()))
                .thenReturn(false);
    }

    @After
    public void tearDown() {
        clearInvocations(mHealthConnectPermissionHelper, mPackageInfoUtils);
    }

    @Test
    public void constructor_initializesFields() {
        assertThat(mMatchmakingManager).isNotNull();
    }

    @Test
    public void fetchMatchingApps_noReadPermissionsRequested_returnsEmpty() {
        mockReadingApp(PACKAGE_NAME, Collections.emptyList());

        Map<String, Set<String>> result =
                mMatchmakingManager.fetchMatchingApps(Collections.emptySet(), PACKAGE_NAME);

        assertThat(result).isEmpty();
    }

    @Test
    public void fetchMatchingApps_noMatchingWritePermission_returnsEmpty() {
        mockReadingApp(PACKAGE_NAME, ImmutableList.of(READ_STEPS));

        PackageInfo nonMatchingApp =
                createPackageInfo(PACKAGE_NAME_2, new String[] {WRITE_HEART_RATE});
        mockCompatibleHealthConnectApps(ImmutableList.of(nonMatchingApp));
        mockAppSystemStatus(PACKAGE_NAME_2, /* isSystemApp= */ false);
        mockPermissionCheckResult(PACKAGE_NAME_2, WRITE_HEART_RATE, PERMISSION_DENIED);
        mockHealthPermissionFlags(PACKAGE_NAME_2, WRITE_HEART_RATE, 0);

        Map<String, Set<String>> result =
                mMatchmakingManager.fetchMatchingApps(Collections.emptySet(), PACKAGE_NAME);

        assertThat(result).isEmpty();
    }

    @Test
    public void fetchMatchingApps_matchExists_returnsMatchingApp() {
        mockReadingApp(PACKAGE_NAME, ImmutableList.of(READ_STEPS));
        PackageInfo matchingApp = createPackageInfo(PACKAGE_NAME_2, new String[] {WRITE_STEPS});
        mockCompatibleHealthConnectApps(ImmutableList.of(matchingApp));
        mockAppSystemStatus(PACKAGE_NAME_2, /* isSystemApp= */ false);
        mockPermissionCheckResult(PACKAGE_NAME_2, WRITE_STEPS, PERMISSION_DENIED);
        mockHealthPermissionFlags(PACKAGE_NAME_2, WRITE_STEPS, 0);

        Map<String, Set<String>> result =
                mMatchmakingManager.fetchMatchingApps(Collections.emptySet(), PACKAGE_NAME);

        assertThat(result).containsExactly(PACKAGE_NAME_2, ImmutableSet.of(WRITE_STEPS));
    }

    @Test
    public void fetchMatchingApps_matchExists_paused_returnsEmpty() {
        mockReadingApp(PACKAGE_NAME, ImmutableList.of(READ_STEPS));
        when(mMatchmakingDenialStateManager.isMatchmakingPaused(
                        PACKAGE_NAME, PACKAGE_NAME_2, ACTIVITY))
                .thenReturn(true);
        PackageInfo matchingApp = createPackageInfo(PACKAGE_NAME_2, new String[] {WRITE_STEPS});
        mockCompatibleHealthConnectApps(ImmutableList.of(matchingApp));
        mockAppSystemStatus(PACKAGE_NAME_2, /* isSystemApp= */ false);
        mockPermissionCheckResult(PACKAGE_NAME_2, WRITE_STEPS, PERMISSION_DENIED);
        mockHealthPermissionFlags(PACKAGE_NAME_2, WRITE_STEPS, 0);

        Map<String, Set<String>> result =
                mMatchmakingManager.fetchMatchingApps(Collections.emptySet(), PACKAGE_NAME);

        assertThat(result).isEmpty();
    }

    @Test
    public void fetchMatchingApps_systemAppWouldMatch_returnsEmpty() {
        mockReadingApp(PACKAGE_NAME, ImmutableList.of(READ_STEPS));
        PackageInfo matchingApp = createPackageInfo(PACKAGE_NAME_2, new String[] {WRITE_STEPS});
        mockCompatibleHealthConnectApps(ImmutableList.of(matchingApp));
        mockAppSystemStatus(PACKAGE_NAME_2, /* isSystemApp= */ true);
        mockPermissionCheckResult(PACKAGE_NAME_2, WRITE_STEPS, PERMISSION_DENIED);
        mockHealthPermissionFlags(PACKAGE_NAME_2, WRITE_STEPS, 0);

        Map<String, Set<String>> result =
                mMatchmakingManager.fetchMatchingApps(Collections.emptySet(), PACKAGE_NAME);

        assertThat(result).isEmpty();
    }

    @Test
    public void fetchMatchingApps_matchingWritePermissionAlreadyGranted_returnsEmpty() {
        mockReadingApp(PACKAGE_NAME, ImmutableList.of(READ_STEPS));
        PackageInfo matchingApp = createPackageInfo(PACKAGE_NAME_2, new String[] {WRITE_STEPS});
        mockCompatibleHealthConnectApps(ImmutableList.of(matchingApp));
        mockAppSystemStatus(PACKAGE_NAME_2, /* isSystemApp= */ false);
        mockPermissionCheckResult(PACKAGE_NAME_2, WRITE_STEPS, PERMISSION_GRANTED);
        mockHealthPermissionFlags(PACKAGE_NAME_2, WRITE_STEPS, 0);

        Map<String, Set<String>> result =
                mMatchmakingManager.fetchMatchingApps(Collections.emptySet(), PACKAGE_NAME);

        assertThat(result).isEmpty();
    }

    @Test
    public void fetchMatchingApps_matchingWritePermissionUserFixed_returnsEmpty() {
        mockReadingApp(PACKAGE_NAME, ImmutableList.of(READ_STEPS));
        PackageInfo matchingApp = createPackageInfo(PACKAGE_NAME_2, new String[] {WRITE_STEPS});
        mockCompatibleHealthConnectApps(ImmutableList.of(matchingApp));
        mockAppSystemStatus(PACKAGE_NAME_2, /* isSystemApp= */ false);
        mockPermissionCheckResult(PACKAGE_NAME_2, WRITE_STEPS, PERMISSION_DENIED);
        mockHealthPermissionFlags(PACKAGE_NAME_2, WRITE_STEPS, FLAG_PERMISSION_USER_FIXED);

        Map<String, Set<String>> result =
                mMatchmakingManager.fetchMatchingApps(Collections.emptySet(), PACKAGE_NAME);

        assertThat(result).isEmpty();
    }

    @Test
    public void fetchMatchingApps_matchingWritePermissionUserSet_returnsMatchingApp() {
        mockReadingApp(PACKAGE_NAME, ImmutableList.of(READ_STEPS));
        PackageInfo matchingApp = createPackageInfo(PACKAGE_NAME_2, new String[] {WRITE_STEPS});
        mockCompatibleHealthConnectApps(ImmutableList.of(matchingApp));
        mockAppSystemStatus(PACKAGE_NAME_2, /* isSystemApp= */ false);
        mockPermissionCheckResult(PACKAGE_NAME_2, WRITE_STEPS, PERMISSION_DENIED);
        mockHealthPermissionFlags(PACKAGE_NAME_2, WRITE_STEPS, FLAG_PERMISSION_USER_SET);

        Map<String, Set<String>> result =
                mMatchmakingManager.fetchMatchingApps(Collections.emptySet(), PACKAGE_NAME);

        assertThat(result).containsExactly(PACKAGE_NAME_2, ImmutableSet.of(WRITE_STEPS));
    }

    @Test
    public void fetchMatchingApps_queryOneRecordType_matchExists_returnMatchingApp() {
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
        Map<String, Set<String>> result =
                mMatchmakingManager.fetchMatchingApps(Set.of(StepsRecord.class), PACKAGE_NAME);

        assertThat(result).containsExactly(PACKAGE_NAME_2, ImmutableSet.of(WRITE_STEPS));
    }

    @Test
    public void fetchMatchingApps_queryMultipleRecordTypes_matchExists_returnMatchingApp() {
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
                Set.of(
                        StepsRecord.class,
                        ExerciseSessionRecord.class,
                        SkinTemperatureRecord.class,
                        HeartRateRecord.class,
                        DistanceRecord.class);
        Map<String, Set<String>> result =
                mMatchmakingManager.fetchMatchingApps(queriedRecordTypes, PACKAGE_NAME);

        assertThat(result)
                .containsExactly(
                        PACKAGE_NAME_2,
                        ImmutableSet.of(WRITE_STEPS, WRITE_DISTANCE, WRITE_HEART_RATE));
    }

    @Test
    public void fetchMatchingApps_multipleWritingApps_oneMatching_returnMatchingApp() {
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
        Map<String, Set<String>> result =
                mMatchmakingManager.fetchMatchingApps(
                        Set.of(StepsRecord.class, DistanceRecord.class, HeartRateRecord.class),
                        PACKAGE_NAME);

        // Match exists for HEART_RATE.
        assertThat(result).containsExactly(PACKAGE_NAME_2, ImmutableSet.of(WRITE_HEART_RATE));
    }

    @Test
    public void fetchMatchingApps_multipleWritingApps_multipleMatching_returnMatchingApps() {
        // Calling app can read HEART_RATE and DISTANCE.
        mockReadingApp(PACKAGE_NAME, ImmutableList.of(READ_HEART_RATE, READ_DISTANCE, READ_STEPS));

        PackageInfo matchingApp =
                createPackageInfo(
                        PACKAGE_NAME_2,
                        new String[] {WRITE_HEART_RATE, WRITE_STEPS, READ_DISTANCE});
        PackageInfo matchingApp2 = createPackageInfo(PACKAGE_NAME_3, new String[] {WRITE_DISTANCE});
        mockCompatibleHealthConnectApps(ImmutableList.of(matchingApp, matchingApp2));

        // Matching app could write HEART_RATE.
        mockAppSystemStatus(PACKAGE_NAME_2, /* isSystemApp= */ false);
        mockPermissionCheckResult(PACKAGE_NAME_2, WRITE_HEART_RATE, PERMISSION_DENIED);
        mockHealthPermissionFlags(PACKAGE_NAME_2, WRITE_HEART_RATE, 0);
        mockPermissionCheckResult(PACKAGE_NAME_2, WRITE_STEPS, PERMISSION_DENIED);
        mockHealthPermissionFlags(PACKAGE_NAME_2, WRITE_STEPS, 0);

        // Matching app 2 could write DISTANCE.
        mockAppSystemStatus(PACKAGE_NAME_3, /* isSystemApp= */ false);
        mockPermissionCheckResult(PACKAGE_NAME_3, WRITE_DISTANCE, PERMISSION_DENIED);
        mockHealthPermissionFlags(PACKAGE_NAME_3, WRITE_DISTANCE, 0);

        // Querying STEPS, DISTANCE, HEART_RATE.
        Map<String, Set<String>> result =
                mMatchmakingManager.fetchMatchingApps(
                        Set.of(StepsRecord.class, DistanceRecord.class, HeartRateRecord.class),
                        PACKAGE_NAME);

        // Match exists for HEART_RATE.
        assertThat(result)
                .containsExactly(
                        PACKAGE_NAME_2,
                        ImmutableSet.of(WRITE_HEART_RATE, WRITE_STEPS),
                        PACKAGE_NAME_3,
                        ImmutableSet.of(WRITE_DISTANCE));
    }

    @Test
    public void fetchMatchingApps_oneAppMorePermissions_oneUserFixed_returnsFilteredPermissions() {
        // Calling app can read HEART_RATE and DISTANCE.
        mockReadingApp(PACKAGE_NAME, ImmutableList.of(READ_STEPS, READ_HEART_RATE, READ_DISTANCE));

        PackageInfo matchingApp =
                createPackageInfo(
                        PACKAGE_NAME_2,
                        new String[] {WRITE_HEART_RATE, WRITE_STEPS, WRITE_DISTANCE});
        mockCompatibleHealthConnectApps(ImmutableList.of(matchingApp));

        // Matching app could write HEART_RATE and DISTANCE.
        mockAppSystemStatus(PACKAGE_NAME_2, /* isSystemApp= */ false);
        mockPermissionCheckResult(PACKAGE_NAME_2, WRITE_HEART_RATE, PERMISSION_DENIED);
        mockHealthPermissionFlags(PACKAGE_NAME_2, WRITE_HEART_RATE, 0);
        mockPermissionCheckResult(PACKAGE_NAME_2, WRITE_STEPS, PERMISSION_DENIED);
        mockHealthPermissionFlags(PACKAGE_NAME_2, WRITE_STEPS, 0);
        mockPermissionCheckResult(PACKAGE_NAME_2, WRITE_DISTANCE, PERMISSION_DENIED);
        mockHealthPermissionFlags(PACKAGE_NAME_2, WRITE_DISTANCE, FLAG_PERMISSION_USER_FIXED);

        // Querying DISTANCE, HEART_RATE, and STEPS.
        Map<String, Set<String>> result =
                mMatchmakingManager.fetchMatchingApps(
                        Set.of(DistanceRecord.class, HeartRateRecord.class, StepsRecord.class),
                        PACKAGE_NAME);

        // Match exists for HEART_RATE.
        assertThat(result)
                .containsExactly(PACKAGE_NAME_2, ImmutableSet.of(WRITE_HEART_RATE, WRITE_STEPS));
    }

    @Test
    public void fetchMatchingApps_oneCategoryPaused_returnsSuggestionsForOtherCategories() {
        mockReadingApp(PACKAGE_NAME, ImmutableList.of(READ_STEPS, READ_SLEEP));
        // Matchmaking is paused for ACTIVITY category.
        when(mMatchmakingDenialStateManager.isMatchmakingPaused(
                        PACKAGE_NAME, PACKAGE_NAME_2, ACTIVITY))
                .thenReturn(true);
        PackageInfo matchingApp =
                createPackageInfo(PACKAGE_NAME_2, new String[] {WRITE_STEPS, WRITE_SLEEP});
        mockCompatibleHealthConnectApps(ImmutableList.of(matchingApp));
        mockAppSystemStatus(PACKAGE_NAME_2, /* isSystemApp= */ false);
        mockPermissionCheckResult(PACKAGE_NAME_2, WRITE_STEPS, PERMISSION_DENIED);
        mockHealthPermissionFlags(PACKAGE_NAME_2, WRITE_STEPS, 0);
        mockPermissionCheckResult(PACKAGE_NAME_2, WRITE_SLEEP, PERMISSION_DENIED);
        mockHealthPermissionFlags(PACKAGE_NAME_2, WRITE_SLEEP, 0);

        Map<String, Set<String>> result =
                mMatchmakingManager.fetchMatchingApps(Collections.emptySet(), PACKAGE_NAME);

        // STEPS is filtered out as ACTIVITY category is paused.
        assertThat(result).containsExactly(PACKAGE_NAME_2, ImmutableSet.of(WRITE_SLEEP));
    }

    @Test
    public void fetchMatchingApps_allCategoriesPaused_returnsEmpty() {
        mockReadingApp(PACKAGE_NAME, ImmutableList.of(READ_STEPS, READ_SLEEP));
        when(mMatchmakingDenialStateManager.isMatchmakingPaused(
                        eq(PACKAGE_NAME), eq(PACKAGE_NAME_2), anyInt()))
                .thenReturn(true);
        PackageInfo matchingApp =
                createPackageInfo(PACKAGE_NAME_2, new String[] {WRITE_STEPS, WRITE_SLEEP});
        mockCompatibleHealthConnectApps(ImmutableList.of(matchingApp));
        mockAppSystemStatus(PACKAGE_NAME_2, /* isSystemApp= */ false);
        mockPermissionCheckResult(PACKAGE_NAME_2, WRITE_STEPS, PERMISSION_DENIED);
        mockHealthPermissionFlags(PACKAGE_NAME_2, WRITE_STEPS, 0);
        mockPermissionCheckResult(PACKAGE_NAME_2, WRITE_SLEEP, PERMISSION_DENIED);
        mockHealthPermissionFlags(PACKAGE_NAME_2, WRITE_SLEEP, 0);

        Map<String, Set<String>> result =
                mMatchmakingManager.fetchMatchingApps(Collections.emptySet(), PACKAGE_NAME);

        assertThat(result).isEmpty();
    }

    @Test
    public void fetchMatchingApps_includeAndExcludeFiltersProvided_throws() {
        Set<DataOrigin> includeDataSources =
                Set.of(
                        new DataOrigin.Builder().setPackageName(PACKAGE_NAME).build(),
                        new DataOrigin.Builder().setPackageName(PACKAGE_NAME_2).build());
        Set<DataOrigin> excludeDataSources =
                Set.of(new DataOrigin.Builder().setPackageName(PACKAGE_NAME_3).build());

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        mMatchmakingManager.fetchMatchingApps(
                                Collections.emptySet(),
                                PACKAGE_NAME,
                                includeDataSources,
                                excludeDataSources));
    }

    @Test
    public void fetchMatchingApps_includeFilterProvided_returnsOnlyAppsInIncludeFilter() {
        mockReadingApp(PACKAGE_NAME, ImmutableList.of(READ_STEPS, READ_HEART_RATE));

        PackageInfo matchingApp1 = createPackageInfo(PACKAGE_NAME_2, new String[] {WRITE_STEPS});
        PackageInfo matchingApp2 =
                createPackageInfo(PACKAGE_NAME_3, new String[] {WRITE_HEART_RATE});
        mockCompatibleHealthConnectApps(ImmutableList.of(matchingApp1, matchingApp2));
        mockPackageVisibility(PACKAGE_NAME, PACKAGE_NAME_2, true);
        mockPackageVisibility(PACKAGE_NAME, PACKAGE_NAME_3, true);

        mockAppSystemStatus(PACKAGE_NAME_2, /* isSystemApp= */ false);
        mockPermissionCheckResult(PACKAGE_NAME_2, WRITE_STEPS, PERMISSION_DENIED);
        mockHealthPermissionFlags(PACKAGE_NAME_2, WRITE_STEPS, 0);

        mockAppSystemStatus(PACKAGE_NAME_3, /* isSystemApp= */ false);
        mockPermissionCheckResult(PACKAGE_NAME_3, WRITE_HEART_RATE, PERMISSION_DENIED);
        mockHealthPermissionFlags(PACKAGE_NAME_3, WRITE_HEART_RATE, 0);

        Set<DataOrigin> includeDataSources =
                Set.of(new DataOrigin.Builder().setPackageName(PACKAGE_NAME_2).build());

        Map<String, Set<String>> result =
                mMatchmakingManager.fetchMatchingApps(
                        Collections.emptySet(), PACKAGE_NAME, includeDataSources, Set.of());

        assertThat(result).containsExactly(PACKAGE_NAME_2, ImmutableSet.of(WRITE_STEPS));
    }

    @Test
    public void fetchMatchingApps_includeFilterProvided_noMatchingAppsReturned() {
        mockReadingApp(PACKAGE_NAME, ImmutableList.of(READ_STEPS, READ_HEART_RATE));

        PackageInfo matchingApp1 = createPackageInfo(PACKAGE_NAME_2, new String[] {WRITE_STEPS});
        PackageInfo matchingApp2 =
                createPackageInfo(PACKAGE_NAME_3, new String[] {WRITE_HEART_RATE});
        mockCompatibleHealthConnectApps(ImmutableList.of(matchingApp1, matchingApp2));
        mockPackageVisibility(PACKAGE_NAME, PACKAGE_NAME_2, true);
        mockPackageVisibility(PACKAGE_NAME, PACKAGE_NAME_3, true);
        mockPackageVisibility(PACKAGE_NAME, "another.package.name", true);

        mockAppSystemStatus(PACKAGE_NAME_2, /* isSystemApp= */ false);
        mockPermissionCheckResult(PACKAGE_NAME_2, WRITE_STEPS, PERMISSION_DENIED);
        mockHealthPermissionFlags(PACKAGE_NAME_2, WRITE_STEPS, 0);

        mockAppSystemStatus(PACKAGE_NAME_3, /* isSystemApp= */ false);
        mockPermissionCheckResult(PACKAGE_NAME_3, WRITE_HEART_RATE, PERMISSION_DENIED);
        mockHealthPermissionFlags(PACKAGE_NAME_3, WRITE_HEART_RATE, 0);

        Set<DataOrigin> includeDataSources =
                Set.of(new DataOrigin.Builder().setPackageName("another.package.name").build());

        Map<String, Set<String>> result =
                mMatchmakingManager.fetchMatchingApps(
                        Collections.emptySet(), PACKAGE_NAME, includeDataSources, Set.of());

        assertThat(result).isEmpty();
    }

    @Test
    public void fetchMatchingApps_includeFilterProvided_hidesPackagesWithoutVisibility() {
        mockReadingApp(PACKAGE_NAME, ImmutableList.of(READ_STEPS, READ_HEART_RATE));

        PackageInfo matchingApp1 = createPackageInfo(PACKAGE_NAME_2, new String[] {WRITE_STEPS});
        PackageInfo matchingApp2 =
                createPackageInfo(PACKAGE_NAME_3, new String[] {WRITE_HEART_RATE});
        mockCompatibleHealthConnectApps(ImmutableList.of(matchingApp1, matchingApp2));
        mockPackageVisibility(PACKAGE_NAME, PACKAGE_NAME_2, true);
        // No visibility over this package, so do not return it as a match
        mockPackageVisibility(PACKAGE_NAME, PACKAGE_NAME_3, false);

        mockAppSystemStatus(PACKAGE_NAME_2, /* isSystemApp= */ false);
        mockPermissionCheckResult(PACKAGE_NAME_2, WRITE_STEPS, PERMISSION_DENIED);
        mockHealthPermissionFlags(PACKAGE_NAME_2, WRITE_STEPS, 0);

        mockAppSystemStatus(PACKAGE_NAME_3, /* isSystemApp= */ false);
        mockPermissionCheckResult(PACKAGE_NAME_3, WRITE_HEART_RATE, PERMISSION_DENIED);
        mockHealthPermissionFlags(PACKAGE_NAME_3, WRITE_HEART_RATE, 0);

        Set<DataOrigin> includeDataSources =
                Set.of(new DataOrigin.Builder().setPackageName(PACKAGE_NAME_2).build());

        Map<String, Set<String>> result =
                mMatchmakingManager.fetchMatchingApps(
                        Collections.emptySet(), PACKAGE_NAME, includeDataSources, Set.of());

        assertThat(result).containsExactly(PACKAGE_NAME_2, ImmutableSet.of(WRITE_STEPS));
    }

    @Test
    public void fetchMatchingApps_excludeFilterProvided_skipsAppsInFilter() {
        mockReadingApp(PACKAGE_NAME, ImmutableList.of(READ_STEPS, READ_HEART_RATE));

        PackageInfo matchingApp1 = createPackageInfo(PACKAGE_NAME_2, new String[] {WRITE_STEPS});
        PackageInfo matchingApp2 =
                createPackageInfo(PACKAGE_NAME_3, new String[] {WRITE_HEART_RATE});
        mockCompatibleHealthConnectApps(ImmutableList.of(matchingApp1, matchingApp2));
        mockPackageVisibility(PACKAGE_NAME, PACKAGE_NAME_2, true);
        mockPackageVisibility(PACKAGE_NAME, PACKAGE_NAME_3, true);

        mockAppSystemStatus(PACKAGE_NAME_2, /* isSystemApp= */ false);
        mockPermissionCheckResult(PACKAGE_NAME_2, WRITE_STEPS, PERMISSION_DENIED);
        mockHealthPermissionFlags(PACKAGE_NAME_2, WRITE_STEPS, 0);

        mockAppSystemStatus(PACKAGE_NAME_3, /* isSystemApp= */ false);
        mockPermissionCheckResult(PACKAGE_NAME_3, WRITE_HEART_RATE, PERMISSION_DENIED);
        mockHealthPermissionFlags(PACKAGE_NAME_3, WRITE_HEART_RATE, 0);

        Set<DataOrigin> excludeDataSources =
                Set.of(new DataOrigin.Builder().setPackageName(PACKAGE_NAME_2).build());

        Map<String, Set<String>> result =
                mMatchmakingManager.fetchMatchingApps(
                        Collections.emptySet(), PACKAGE_NAME, Set.of(), excludeDataSources);

        assertThat(result).containsExactly(PACKAGE_NAME_3, ImmutableSet.of(WRITE_HEART_RATE));
    }

    @Test
    public void fetchMatchingApps_excludeFilterProvided_noMatchingApps() {
        mockReadingApp(PACKAGE_NAME, ImmutableList.of(READ_STEPS, READ_HEART_RATE));

        PackageInfo matchingApp1 = createPackageInfo(PACKAGE_NAME_2, new String[] {WRITE_STEPS});
        PackageInfo matchingApp2 =
                createPackageInfo(PACKAGE_NAME_3, new String[] {WRITE_HEART_RATE});
        mockCompatibleHealthConnectApps(ImmutableList.of(matchingApp1, matchingApp2));
        mockPackageVisibility(PACKAGE_NAME, PACKAGE_NAME_2, true);
        mockPackageVisibility(PACKAGE_NAME, PACKAGE_NAME_3, true);

        mockAppSystemStatus(PACKAGE_NAME_2, /* isSystemApp= */ false);
        mockPermissionCheckResult(PACKAGE_NAME_2, WRITE_STEPS, PERMISSION_DENIED);
        mockHealthPermissionFlags(PACKAGE_NAME_2, WRITE_STEPS, 0);

        mockAppSystemStatus(PACKAGE_NAME_3, /* isSystemApp= */ false);
        mockPermissionCheckResult(PACKAGE_NAME_3, WRITE_HEART_RATE, PERMISSION_DENIED);
        mockHealthPermissionFlags(PACKAGE_NAME_3, WRITE_HEART_RATE, 0);

        Set<DataOrigin> excludeDataSources =
                Set.of(
                        new DataOrigin.Builder().setPackageName(PACKAGE_NAME_2).build(),
                        new DataOrigin.Builder().setPackageName(PACKAGE_NAME_3).build());

        Map<String, Set<String>> result =
                mMatchmakingManager.fetchMatchingApps(
                        Collections.emptySet(), PACKAGE_NAME, Set.of(), excludeDataSources);

        assertThat(result).isEmpty();
    }

    @Test
    public void fetchMatchingApps_excludeFilterProvided_returnsPackagesWithoutVisibility() {
        mockReadingApp(PACKAGE_NAME, ImmutableList.of(READ_STEPS, READ_HEART_RATE));

        PackageInfo matchingApp1 = createPackageInfo(PACKAGE_NAME_2, new String[] {WRITE_STEPS});
        PackageInfo matchingApp2 =
                createPackageInfo(PACKAGE_NAME_3, new String[] {WRITE_HEART_RATE});
        mockCompatibleHealthConnectApps(ImmutableList.of(matchingApp1, matchingApp2));
        // No visibility over PACKAGE_NAME_2, so even if it's present in the exclude filter, we will
        // return it as a match
        mockPackageVisibility(PACKAGE_NAME, PACKAGE_NAME_2, false);
        mockPackageVisibility(PACKAGE_NAME, PACKAGE_NAME_3, true);

        mockAppSystemStatus(PACKAGE_NAME_2, /* isSystemApp= */ false);
        mockPermissionCheckResult(PACKAGE_NAME_2, WRITE_STEPS, PERMISSION_DENIED);
        mockHealthPermissionFlags(PACKAGE_NAME_2, WRITE_STEPS, 0);

        mockAppSystemStatus(PACKAGE_NAME_3, /* isSystemApp= */ false);
        mockPermissionCheckResult(PACKAGE_NAME_3, WRITE_HEART_RATE, PERMISSION_DENIED);
        mockHealthPermissionFlags(PACKAGE_NAME_3, WRITE_HEART_RATE, 0);

        Set<DataOrigin> excludeDataSources =
                Set.of(
                        new DataOrigin.Builder().setPackageName(PACKAGE_NAME_2).build(),
                        new DataOrigin.Builder().setPackageName(PACKAGE_NAME_3).build());

        Map<String, Set<String>> result =
                mMatchmakingManager.fetchMatchingApps(
                        Collections.emptySet(), PACKAGE_NAME, Set.of(), excludeDataSources);

        assertThat(result).containsExactly(PACKAGE_NAME_2, ImmutableSet.of(WRITE_STEPS));
    }

    @Test
    @EnableFlags({
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void fetchMatchingDevices_includeAndExcludeFiltersProvided_throws() {
        Set<DataOrigin> includeDataSources =
                Set.of(
                        new DataOrigin.Builder().setPackageName(PACKAGE_NAME).build(),
                        new DataOrigin.Builder().setPackageName(PACKAGE_NAME_2).build());
        Set<DataOrigin> excludeDataSources =
                Set.of(new DataOrigin.Builder().setPackageName(PACKAGE_NAME_3).build());

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        mMatchmakingManager.fetchMatchingDevices(
                                Collections.emptySet(),
                                PACKAGE_NAME,
                                includeDataSources,
                                excludeDataSources));
    }

    @Test
    @EnableFlags({
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void fetchMatchingDevices_noReadPermissionsRequested_returnsEmpty() {
        mockReadingApp(PACKAGE_NAME, Collections.emptyList());

        Map<String, Set<String>> result =
                mMatchmakingManager.fetchMatchingDevices(
                        Collections.emptySet(), PACKAGE_NAME, Set.of(), Set.of());

        assertThat(result).isEmpty();
    }

    @Test
    @EnableFlags({
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void fetchMatchingDevices_noMatchingWritePermissions_returnsEmpty() {
        mockReadingApp(PACKAGE_NAME, ImmutableList.of(READ_SLEEP));

        Device device = new Device.Builder().setManufacturer("Man1").build();
        DataOrigin origin = new DataOrigin.Builder().setPackageName("device.package").build();
        DeviceDataTypeAdvertisement stepsAdvertisement =
                new DeviceDataTypeAdvertisement.Builder(StepsRecord.class).build();

        DeviceDataProviderInfo providerInfo =
                new DeviceDataProviderInfo(
                        DEVICE_DATA_PROVIDER_PACKAGE_NAME,
                        DEVICE_ID,
                        "",
                        "",
                        ImmutableSet.of(stepsAdvertisement));

        DeviceDataSourceInfo nonMatchingDeviceInfo =
                new DeviceDataSourceInfo(origin, device, true, List.of(providerInfo));

        mockCompatibleDevices(ImmutableList.of(nonMatchingDeviceInfo));

        Map<String, Set<String>> result =
                mMatchmakingManager.fetchMatchingDevices(
                        Collections.emptySet(), PACKAGE_NAME, Set.of(), Set.of());

        assertThat(result).isEmpty();
    }

    // TODO (b/462180668) fetchMatchingDevices_noMatchingWritePermissionsForSymptom_returnsEmpty

    @Test
    @EnableFlags({
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void fetchMatchingDevices_noDevicesExist_returnsEmpty() {
        mockReadingApp(PACKAGE_NAME, ImmutableList.of(READ_SLEEP));
        mockCompatibleDevices(ImmutableList.of());
        Map<String, Set<String>> result =
                mMatchmakingManager.fetchMatchingDevices(
                        Collections.emptySet(), PACKAGE_NAME, Set.of(), Set.of());

        assertThat(result).isEmpty();
    }

    @Test
    @DisableFlags(Flags.FLAG_DEVICE_DATA_PROVIDERS_API)
    public void fetchMatchingDevices_flagOff_returnsEmpty() {
        mockReadingApp(PACKAGE_NAME, ImmutableList.of(READ_SLEEP));
        Device device = new Device.Builder().setManufacturer("Man1").build();
        DataOrigin origin = new DataOrigin.Builder().setPackageName(DEVICE_PACKAGE_NAME).build();
        DeviceDataTypeAdvertisement stepsAdvertisement =
                new DeviceDataTypeAdvertisement.Builder(StepsRecord.class).build();

        DeviceDataProviderInfo providerInfo =
                new DeviceDataProviderInfo(
                        DEVICE_DATA_PROVIDER_PACKAGE_NAME,
                        DEVICE_ID,
                        "",
                        "",
                        ImmutableSet.of(stepsAdvertisement));

        DeviceDataSourceInfo nonMatchingDeviceInfo =
                new DeviceDataSourceInfo(origin, device, true, List.of(providerInfo));

        mockCompatibleDevices(ImmutableList.of(nonMatchingDeviceInfo));

        Map<String, Set<String>> result =
                mMatchmakingManager.fetchMatchingDevices(
                        Collections.emptySet(), PACKAGE_NAME, Set.of(), Set.of());

        assertThat(result).isEmpty();
    }

    @Test
    @EnableFlags({
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void fetchMatchingDevices_matchingWritePermissionsAreUserEnabled_returnsEmpty() {
        mockReadingApp(PACKAGE_NAME, ImmutableList.of(READ_STEPS, READ_DISTANCE));
        Device device = new Device.Builder().setManufacturer("Man1").build();
        DataOrigin origin = new DataOrigin.Builder().setPackageName(DEVICE_PACKAGE_NAME).build();
        DeviceDataTypeAdvertisement stepsAdvertisement =
                new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                        .setUserEnabled(true)
                        .build();
        DeviceDataTypeAdvertisement distanceAdvertisement =
                new DeviceDataTypeAdvertisement.Builder(DistanceRecord.class)
                        .setUserEnabled(true)
                        .build();

        DeviceDataProviderInfo providerInfo =
                new DeviceDataProviderInfo(
                        DEVICE_DATA_PROVIDER_PACKAGE_NAME,
                        DEVICE_ID,
                        "",
                        "",
                        ImmutableSet.of(stepsAdvertisement, distanceAdvertisement));

        DeviceDataSourceInfo matchingDeviceInfo =
                new DeviceDataSourceInfo(origin, device, true, List.of(providerInfo));

        mockCompatibleDevices(ImmutableList.of(matchingDeviceInfo));

        Map<String, Set<String>> result =
                mMatchmakingManager.fetchMatchingDevices(
                        Collections.emptySet(), PACKAGE_NAME, Set.of(), Set.of());
        assertThat(result).isEmpty();
    }

    // TODO (b/466983701)
    // fetchMatchingDevices_matchingWritePermissionsNotVisibleByDefault_noIncludeFilter_returnsEmpty
    // TODO (b/466983701)
    // fetchMatchingDevices_matchingWritePermissionsNotVisibleByDefault_withIncludeFilter_returnsMatch

    @Test
    @EnableFlags({
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void fetchMatchingDevices_matchExists_returnsMatchingDevice() {
        mockReadingApp(PACKAGE_NAME, ImmutableList.of(READ_STEPS, READ_DISTANCE));
        Device device = new Device.Builder().setManufacturer("Man1").build();
        DataOrigin origin = new DataOrigin.Builder().setPackageName(DEVICE_PACKAGE_NAME).build();
        DeviceDataTypeAdvertisement stepsAdvertisement =
                new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                        .setUserEnabled(false)
                        .build();
        DeviceDataTypeAdvertisement distanceAdvertisement =
                new DeviceDataTypeAdvertisement.Builder(DistanceRecord.class)
                        .setUserEnabled(false)
                        .build();

        DeviceDataProviderInfo providerInfo =
                new DeviceDataProviderInfo(
                        DEVICE_DATA_PROVIDER_PACKAGE_NAME,
                        DEVICE_ID,
                        "",
                        "",
                        ImmutableSet.of(stepsAdvertisement, distanceAdvertisement));

        DeviceDataSourceInfo matchingDeviceInfo =
                new DeviceDataSourceInfo(origin, device, true, List.of(providerInfo));

        mockCompatibleDevices(ImmutableList.of(matchingDeviceInfo));

        Map<String, Set<String>> result =
                mMatchmakingManager.fetchMatchingDevices(
                        Collections.emptySet(), PACKAGE_NAME, Set.of(), Set.of());
        assertThat(result)
                .containsExactly(DEVICE_PACKAGE_NAME, Set.of(WRITE_STEPS, WRITE_DISTANCE));
    }

    @Test
    @EnableFlags({
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void fetchMatchingDevices_matchExists_paused_returnsEmpty() {
        mockReadingApp(PACKAGE_NAME, ImmutableList.of(READ_STEPS, READ_DISTANCE));
        Device device = new Device.Builder().setManufacturer("Man1").build();
        DataOrigin origin = new DataOrigin.Builder().setPackageName(DEVICE_PACKAGE_NAME).build();
        DeviceDataTypeAdvertisement stepsAdvertisement =
                new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                        .setUserEnabled(false)
                        .build();
        DeviceDataTypeAdvertisement distanceAdvertisement =
                new DeviceDataTypeAdvertisement.Builder(DistanceRecord.class)
                        .setUserEnabled(false)
                        .build();

        DeviceDataProviderInfo providerInfo =
                new DeviceDataProviderInfo(
                        DEVICE_DATA_PROVIDER_PACKAGE_NAME,
                        DEVICE_ID,
                        "",
                        "",
                        ImmutableSet.of(stepsAdvertisement, distanceAdvertisement));

        DeviceDataSourceInfo matchingDeviceInfo =
                new DeviceDataSourceInfo(origin, device, true, List.of(providerInfo));
        mockCompatibleDevices(ImmutableList.of(matchingDeviceInfo));
        when(mMatchmakingDenialStateManager.isMatchmakingForDevicePaused(anyString(), anyString()))
                .thenReturn(true);

        Map<String, Set<String>> result =
                mMatchmakingManager.fetchMatchingDevices(
                        Collections.emptySet(), PACKAGE_NAME, Set.of(), Set.of());
        assertThat(result).isEmpty();
    }

    @Test
    @EnableFlags({
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void fetchMatchingDevices_queryOneRecordType_multipleMatches_returnsMatchingDevices() {
        mockReadingApp(PACKAGE_NAME, ImmutableList.of(READ_DISTANCE));
        Device device1 = new Device.Builder().setManufacturer("Man1").build();
        DataOrigin origin1 = new DataOrigin.Builder().setPackageName(DEVICE_PACKAGE_NAME).build();
        DeviceDataTypeAdvertisement stepsAdvertisement =
                new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                        .setUserEnabled(false)
                        .build();
        DeviceDataTypeAdvertisement distanceAdvertisement =
                new DeviceDataTypeAdvertisement.Builder(DistanceRecord.class)
                        .setUserEnabled(false)
                        .build();

        DeviceDataProviderInfo providerInfo1 =
                new DeviceDataProviderInfo(
                        DEVICE_DATA_PROVIDER_PACKAGE_NAME,
                        DEVICE_ID,
                        "",
                        "",
                        ImmutableSet.of(stepsAdvertisement, distanceAdvertisement));

        Device device2 = new Device.Builder().setManufacturer("Man2").build();
        DataOrigin origin2 = new DataOrigin.Builder().setPackageName(DEVICE_PACKAGE_NAME_2).build();

        DeviceDataProviderInfo providerInfo2 =
                new DeviceDataProviderInfo(
                        DEVICE_DATA_PROVIDER_PACKAGE_NAME_2,
                        DEVICE_ID_2,
                        "",
                        "",
                        ImmutableSet.of(stepsAdvertisement, distanceAdvertisement));

        DeviceDataSourceInfo matchingDeviceInfo1 =
                new DeviceDataSourceInfo(origin1, device1, true, List.of(providerInfo1));
        DeviceDataSourceInfo matchingDeviceInfo2 =
                new DeviceDataSourceInfo(origin2, device2, true, List.of(providerInfo2));

        mockCompatibleDevices(ImmutableList.of(matchingDeviceInfo1, matchingDeviceInfo2));

        Map<String, Set<String>> result =
                mMatchmakingManager.fetchMatchingDevices(
                        Collections.emptySet(), PACKAGE_NAME, Set.of(), Set.of());
        assertThat(result)
                .containsExactly(
                        DEVICE_PACKAGE_NAME,
                        Set.of(WRITE_DISTANCE),
                        DEVICE_PACKAGE_NAME_2,
                        Set.of(WRITE_DISTANCE));
    }

    @Test
    @EnableFlags({
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void fetchMatchingDevices_multipleDDPsPerDevice_returnsMatchingDevices() {
        mockReadingApp(PACKAGE_NAME, ImmutableList.of(READ_STEPS, READ_DISTANCE));
        Device device1 = new Device.Builder().setManufacturer("Man1").build();
        DataOrigin origin1 = new DataOrigin.Builder().setPackageName(DEVICE_PACKAGE_NAME).build();
        DeviceDataTypeAdvertisement stepsAdvertisement =
                new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                        .setUserEnabled(false)
                        .build();
        DeviceDataTypeAdvertisement distanceAdvertisement =
                new DeviceDataTypeAdvertisement.Builder(DistanceRecord.class)
                        .setUserEnabled(false)
                        .build();

        DeviceDataProviderInfo providerInfo1 =
                new DeviceDataProviderInfo(
                        DEVICE_DATA_PROVIDER_PACKAGE_NAME,
                        DEVICE_ID,
                        "",
                        "",
                        ImmutableSet.of(stepsAdvertisement));

        Device device2 = new Device.Builder().setManufacturer("Man2").build();
        DataOrigin origin2 = new DataOrigin.Builder().setPackageName(DEVICE_PACKAGE_NAME_2).build();

        DeviceDataProviderInfo providerInfo2 =
                new DeviceDataProviderInfo(
                        DEVICE_DATA_PROVIDER_PACKAGE_NAME_2,
                        DEVICE_ID_2,
                        "",
                        "",
                        ImmutableSet.of(distanceAdvertisement));

        DeviceDataSourceInfo matchingDeviceInfo1 =
                new DeviceDataSourceInfo(origin1, device1, true, List.of(providerInfo1));
        DeviceDataSourceInfo matchingDeviceInfo2 =
                new DeviceDataSourceInfo(origin2, device2, true, List.of(providerInfo2));

        mockCompatibleDevices(ImmutableList.of(matchingDeviceInfo1, matchingDeviceInfo2));

        Map<String, Set<String>> result =
                mMatchmakingManager.fetchMatchingDevices(
                        Collections.emptySet(), PACKAGE_NAME, Set.of(), Set.of());
        assertThat(result)
                .containsExactly(
                        DEVICE_PACKAGE_NAME,
                        Set.of(WRITE_STEPS),
                        DEVICE_PACKAGE_NAME_2,
                        Set.of(WRITE_DISTANCE));
    }

    @Test
    @EnableFlags({
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void fetchMatchingDevices_multipleDDPsPerDevice_excludesDeviceWhenNoMatchingDDP() {
        mockReadingApp(PACKAGE_NAME, ImmutableList.of(READ_STEPS, READ_SLEEP));
        Device device1 = new Device.Builder().setManufacturer("Man1").build();
        DataOrigin origin1 = new DataOrigin.Builder().setPackageName(DEVICE_PACKAGE_NAME).build();
        DeviceDataTypeAdvertisement stepsAdvertisement =
                new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                        .setUserEnabled(true)
                        .build();
        DeviceDataTypeAdvertisement distanceAdvertisement =
                new DeviceDataTypeAdvertisement.Builder(DistanceRecord.class)
                        .setUserEnabled(false)
                        .build();
        DeviceDataTypeAdvertisement sleepAdvertisement =
                new DeviceDataTypeAdvertisement.Builder(SleepSessionRecord.class)
                        .setUserEnabled(false)
                        .build();

        DeviceDataProviderInfo providerInfo1 =
                new DeviceDataProviderInfo(
                        DEVICE_DATA_PROVIDER_PACKAGE_NAME,
                        DEVICE_ID,
                        "",
                        "",
                        ImmutableSet.of(stepsAdvertisement));

        Device device2 = new Device.Builder().setManufacturer("Man2").build();
        DataOrigin origin2 = new DataOrigin.Builder().setPackageName(DEVICE_PACKAGE_NAME_2).build();

        DeviceDataProviderInfo providerInfo2 =
                new DeviceDataProviderInfo(
                        DEVICE_DATA_PROVIDER_PACKAGE_NAME_2,
                        DEVICE_ID_2,
                        "",
                        "",
                        ImmutableSet.of(distanceAdvertisement));

        Device device3 = new Device.Builder().setManufacturer("Man3").build();
        DataOrigin origin3 = new DataOrigin.Builder().setPackageName(DEVICE_PACKAGE_NAME_3).build();
        DeviceDataProviderInfo providerInfo3 =
                new DeviceDataProviderInfo(
                        DEVICE_DATA_PROVIDER_PACKAGE_NAME_3,
                        DEVICE_ID_3,
                        "",
                        "",
                        ImmutableSet.of(sleepAdvertisement));

        // Should not return because the DDP steps config is already enabled
        DeviceDataSourceInfo matchingDeviceInfo1 =
                new DeviceDataSourceInfo(origin1, device1, true, List.of(providerInfo1));
        // Should not return because DPP does not have matching permissions
        DeviceDataSourceInfo matchingDeviceInfo2 =
                new DeviceDataSourceInfo(origin2, device2, true, List.of(providerInfo2));
        // Should return because DDP is a match
        DeviceDataSourceInfo matchingDeviceInfo3 =
                new DeviceDataSourceInfo(origin3, device3, true, List.of(providerInfo3));

        mockCompatibleDevices(
                ImmutableList.of(matchingDeviceInfo1, matchingDeviceInfo2, matchingDeviceInfo3));

        Map<String, Set<String>> result =
                mMatchmakingManager.fetchMatchingDevices(
                        Collections.emptySet(), PACKAGE_NAME, Set.of(), Set.of());
        assertThat(result).containsExactly(DEVICE_PACKAGE_NAME_3, Set.of(WRITE_SLEEP));
    }

    @Test
    @EnableFlags({
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void fetchMatchingDevices_includeOneDataSource_matchExists() {
        mockReadingApp(PACKAGE_NAME, ImmutableList.of(READ_DISTANCE));
        Device device1 = new Device.Builder().setManufacturer("Man1").build();
        DataOrigin origin1 = new DataOrigin.Builder().setPackageName(DEVICE_PACKAGE_NAME).build();
        DeviceDataTypeAdvertisement stepsAdvertisement =
                new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                        .setUserEnabled(false)
                        .build();
        DeviceDataTypeAdvertisement distanceAdvertisement =
                new DeviceDataTypeAdvertisement.Builder(DistanceRecord.class)
                        .setUserEnabled(false)
                        .build();

        DeviceDataProviderInfo providerInfo1 =
                new DeviceDataProviderInfo(
                        DEVICE_DATA_PROVIDER_PACKAGE_NAME,
                        DEVICE_ID,
                        "",
                        "",
                        ImmutableSet.of(stepsAdvertisement, distanceAdvertisement));

        Device device2 = new Device.Builder().setManufacturer("Man2").build();
        DataOrigin origin2 = new DataOrigin.Builder().setPackageName(DEVICE_PACKAGE_NAME_2).build();

        DeviceDataProviderInfo providerInfo2 =
                new DeviceDataProviderInfo(
                        DEVICE_DATA_PROVIDER_PACKAGE_NAME_2,
                        DEVICE_ID_2,
                        "",
                        "",
                        ImmutableSet.of(stepsAdvertisement, distanceAdvertisement));

        DeviceDataSourceInfo matchingDeviceInfo1 =
                new DeviceDataSourceInfo(origin1, device1, true, List.of(providerInfo1));
        // Should not return because not in the include filter
        DeviceDataSourceInfo matchingDeviceInfo2 =
                new DeviceDataSourceInfo(origin2, device2, true, List.of(providerInfo2));

        mockCompatibleDevices(ImmutableList.of(matchingDeviceInfo1, matchingDeviceInfo2));
        DataOrigin includeDataSource =
                new DataOrigin.Builder().setPackageName(DEVICE_PACKAGE_NAME).build();

        Map<String, Set<String>> result =
                mMatchmakingManager.fetchMatchingDevices(
                        Collections.emptySet(), PACKAGE_NAME, Set.of(includeDataSource), Set.of());
        assertThat(result).containsExactly(DEVICE_PACKAGE_NAME, Set.of(WRITE_DISTANCE));
    }

    @Test
    @EnableFlags({
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void fetchMatchingDevices_includeMultipleDataSources_matchExists() {
        mockReadingApp(PACKAGE_NAME, ImmutableList.of(READ_DISTANCE));
        Device device1 = new Device.Builder().setManufacturer("Man1").build();
        DataOrigin origin1 = new DataOrigin.Builder().setPackageName(DEVICE_PACKAGE_NAME).build();
        DeviceDataTypeAdvertisement stepsAdvertisement =
                new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                        .setUserEnabled(false)
                        .build();
        DeviceDataTypeAdvertisement distanceAdvertisement =
                new DeviceDataTypeAdvertisement.Builder(DistanceRecord.class)
                        .setUserEnabled(false)
                        .build();

        DeviceDataProviderInfo providerInfo1 =
                new DeviceDataProviderInfo(
                        DEVICE_DATA_PROVIDER_PACKAGE_NAME,
                        DEVICE_ID,
                        "",
                        "",
                        ImmutableSet.of(stepsAdvertisement, distanceAdvertisement));

        Device device2 = new Device.Builder().setManufacturer("Man2").build();
        DataOrigin origin2 = new DataOrigin.Builder().setPackageName(DEVICE_PACKAGE_NAME_2).build();

        DeviceDataProviderInfo providerInfo2 =
                new DeviceDataProviderInfo(
                        DEVICE_DATA_PROVIDER_PACKAGE_NAME_2,
                        DEVICE_ID_2,
                        "",
                        "",
                        ImmutableSet.of(stepsAdvertisement, distanceAdvertisement));

        DeviceDataSourceInfo matchingDeviceInfo1 =
                new DeviceDataSourceInfo(origin1, device1, true, List.of(providerInfo1));
        DeviceDataSourceInfo matchingDeviceInfo2 =
                new DeviceDataSourceInfo(origin2, device2, true, List.of(providerInfo2));

        mockCompatibleDevices(ImmutableList.of(matchingDeviceInfo1, matchingDeviceInfo2));
        Set<DataOrigin> includeDataSources =
                Set.of(
                        new DataOrigin.Builder().setPackageName(DEVICE_PACKAGE_NAME).build(),
                        new DataOrigin.Builder().setPackageName(DEVICE_PACKAGE_NAME_2).build());

        Map<String, Set<String>> result =
                mMatchmakingManager.fetchMatchingDevices(
                        Collections.emptySet(), PACKAGE_NAME, includeDataSources, Set.of());
        assertThat(result)
                .containsExactly(
                        DEVICE_PACKAGE_NAME,
                        Set.of(WRITE_DISTANCE),
                        DEVICE_PACKAGE_NAME_2,
                        Set.of(WRITE_DISTANCE));
    }

    @Test
    @EnableFlags({
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void fetchMatchingDevices_includeMultipleDataSources_noMatch() {
        mockReadingApp(PACKAGE_NAME, ImmutableList.of(READ_DISTANCE));
        Device device1 = new Device.Builder().setManufacturer("Man1").build();
        DataOrigin origin1 = new DataOrigin.Builder().setPackageName(DEVICE_PACKAGE_NAME).build();
        DeviceDataTypeAdvertisement stepsAdvertisement =
                new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                        .setUserEnabled(false)
                        .build();
        DeviceDataTypeAdvertisement distanceAdvertisement =
                new DeviceDataTypeAdvertisement.Builder(DistanceRecord.class)
                        .setUserEnabled(false)
                        .build();

        DeviceDataProviderInfo providerInfo1 =
                new DeviceDataProviderInfo(
                        DEVICE_DATA_PROVIDER_PACKAGE_NAME,
                        DEVICE_ID,
                        "",
                        "",
                        ImmutableSet.of(stepsAdvertisement, distanceAdvertisement));

        Device device2 = new Device.Builder().setManufacturer("Man2").build();
        DataOrigin origin2 = new DataOrigin.Builder().setPackageName(DEVICE_PACKAGE_NAME_2).build();

        DeviceDataProviderInfo providerInfo2 =
                new DeviceDataProviderInfo(
                        DEVICE_DATA_PROVIDER_PACKAGE_NAME_2,
                        DEVICE_ID_2,
                        "",
                        "",
                        ImmutableSet.of(stepsAdvertisement, distanceAdvertisement));

        DeviceDataSourceInfo matchingDeviceInfo1 =
                new DeviceDataSourceInfo(origin1, device1, true, List.of(providerInfo1));
        DeviceDataSourceInfo matchingDeviceInfo2 =
                new DeviceDataSourceInfo(origin2, device2, true, List.of(providerInfo2));

        mockCompatibleDevices(ImmutableList.of(matchingDeviceInfo1, matchingDeviceInfo2));
        Set<DataOrigin> includeDataSources =
                Set.of(
                        new DataOrigin.Builder().setPackageName(DEVICE_PACKAGE_NAME_3).build(),
                        new DataOrigin.Builder().setPackageName("some.other.device").build());

        Map<String, Set<String>> result =
                mMatchmakingManager.fetchMatchingDevices(
                        Collections.emptySet(), PACKAGE_NAME, includeDataSources, Set.of());
        assertThat(result).isEmpty();
    }

    @Test
    @EnableFlags({
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void fetchMatchingDevices_excludeOneDataSource_returnsMatches() {
        mockReadingApp(PACKAGE_NAME, ImmutableList.of(READ_DISTANCE));
        Device device1 = new Device.Builder().setManufacturer("Man1").build();
        DataOrigin origin1 = new DataOrigin.Builder().setPackageName(DEVICE_PACKAGE_NAME).build();
        DeviceDataTypeAdvertisement stepsAdvertisement =
                new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                        .setUserEnabled(false)
                        .build();
        DeviceDataTypeAdvertisement distanceAdvertisement =
                new DeviceDataTypeAdvertisement.Builder(DistanceRecord.class)
                        .setUserEnabled(false)
                        .build();

        DeviceDataProviderInfo providerInfo1 =
                new DeviceDataProviderInfo(
                        DEVICE_DATA_PROVIDER_PACKAGE_NAME,
                        DEVICE_ID,
                        "",
                        "",
                        ImmutableSet.of(stepsAdvertisement, distanceAdvertisement));

        Device device2 = new Device.Builder().setManufacturer("Man2").build();
        DataOrigin origin2 = new DataOrigin.Builder().setPackageName(DEVICE_PACKAGE_NAME_2).build();

        DeviceDataProviderInfo providerInfo2 =
                new DeviceDataProviderInfo(
                        DEVICE_DATA_PROVIDER_PACKAGE_NAME_2,
                        DEVICE_ID_2,
                        "",
                        "",
                        ImmutableSet.of(stepsAdvertisement, distanceAdvertisement));

        DeviceDataSourceInfo matchingDeviceInfo1 =
                new DeviceDataSourceInfo(origin1, device1, true, List.of(providerInfo1));
        DeviceDataSourceInfo matchingDeviceInfo2 =
                new DeviceDataSourceInfo(origin2, device2, true, List.of(providerInfo2));

        mockCompatibleDevices(ImmutableList.of(matchingDeviceInfo1, matchingDeviceInfo2));
        Set<DataOrigin> excludeDataSources =
                Set.of(new DataOrigin.Builder().setPackageName(DEVICE_PACKAGE_NAME_2).build());

        Map<String, Set<String>> result =
                mMatchmakingManager.fetchMatchingDevices(
                        Collections.emptySet(), PACKAGE_NAME, Set.of(), excludeDataSources);
        assertThat(result).containsExactly(DEVICE_PACKAGE_NAME, Set.of(WRITE_DISTANCE));
    }

    @Test
    @EnableFlags({
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void fetchMatchingDevices_excludeMultipleDataSources_returnsMatches() {
        mockReadingApp(PACKAGE_NAME, ImmutableList.of(READ_DISTANCE));
        Device device1 = new Device.Builder().setManufacturer("Man1").build();
        DataOrigin origin1 = new DataOrigin.Builder().setPackageName(DEVICE_PACKAGE_NAME).build();
        DeviceDataTypeAdvertisement stepsAdvertisement =
                new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                        .setUserEnabled(false)
                        .build();
        DeviceDataTypeAdvertisement distanceAdvertisement =
                new DeviceDataTypeAdvertisement.Builder(DistanceRecord.class)
                        .setUserEnabled(false)
                        .build();

        DeviceDataProviderInfo providerInfo1 =
                new DeviceDataProviderInfo(
                        DEVICE_DATA_PROVIDER_PACKAGE_NAME,
                        DEVICE_ID,
                        "",
                        "",
                        ImmutableSet.of(stepsAdvertisement, distanceAdvertisement));

        Device device2 = new Device.Builder().setManufacturer("Man2").build();
        DataOrigin origin2 = new DataOrigin.Builder().setPackageName(DEVICE_PACKAGE_NAME_2).build();

        DeviceDataProviderInfo providerInfo2 =
                new DeviceDataProviderInfo(
                        DEVICE_DATA_PROVIDER_PACKAGE_NAME_2,
                        DEVICE_ID_2,
                        "",
                        "",
                        ImmutableSet.of(stepsAdvertisement, distanceAdvertisement));

        DeviceDataSourceInfo matchingDeviceInfo1 =
                new DeviceDataSourceInfo(origin1, device1, true, List.of(providerInfo1));
        DeviceDataSourceInfo matchingDeviceInfo2 =
                new DeviceDataSourceInfo(origin2, device2, true, List.of(providerInfo2));

        mockCompatibleDevices(ImmutableList.of(matchingDeviceInfo1, matchingDeviceInfo2));
        Set<DataOrigin> excludeDataSources =
                Set.of(
                        new DataOrigin.Builder().setPackageName(DEVICE_PACKAGE_NAME).build(),
                        new DataOrigin.Builder().setPackageName("another.device").build());

        Map<String, Set<String>> result =
                mMatchmakingManager.fetchMatchingDevices(
                        Collections.emptySet(), PACKAGE_NAME, Set.of(), excludeDataSources);
        assertThat(result).containsExactly(DEVICE_PACKAGE_NAME_2, Set.of(WRITE_DISTANCE));
    }

    @Test
    @EnableFlags({
        Flags.FLAG_DEVICE_DATA_PROVIDERS_API,
        Flags.FLAG_DEVICE_DATA_PROVIDERS_DB,
        Flags.FLAG_DEVELOPMENT_DATABASE_RW
    })
    public void fetchMatchingDevices_excludeMultipleDataSources_returnsEmpty() {
        mockReadingApp(PACKAGE_NAME, ImmutableList.of(READ_DISTANCE));
        Device device1 = new Device.Builder().setManufacturer("Man1").build();
        DataOrigin origin1 = new DataOrigin.Builder().setPackageName(DEVICE_PACKAGE_NAME).build();
        DeviceDataTypeAdvertisement stepsAdvertisement =
                new DeviceDataTypeAdvertisement.Builder(StepsRecord.class)
                        .setUserEnabled(false)
                        .build();
        DeviceDataTypeAdvertisement distanceAdvertisement =
                new DeviceDataTypeAdvertisement.Builder(DistanceRecord.class)
                        .setUserEnabled(false)
                        .build();

        DeviceDataProviderInfo providerInfo1 =
                new DeviceDataProviderInfo(
                        DEVICE_DATA_PROVIDER_PACKAGE_NAME,
                        DEVICE_ID,
                        "",
                        "",
                        ImmutableSet.of(stepsAdvertisement, distanceAdvertisement));

        Device device2 = new Device.Builder().setManufacturer("Man2").build();
        DataOrigin origin2 = new DataOrigin.Builder().setPackageName(DEVICE_PACKAGE_NAME_2).build();

        DeviceDataProviderInfo providerInfo2 =
                new DeviceDataProviderInfo(
                        DEVICE_DATA_PROVIDER_PACKAGE_NAME_2,
                        DEVICE_ID_2,
                        "",
                        "",
                        ImmutableSet.of(stepsAdvertisement, distanceAdvertisement));

        DeviceDataSourceInfo matchingDeviceInfo1 =
                new DeviceDataSourceInfo(origin1, device1, true, List.of(providerInfo1));
        DeviceDataSourceInfo matchingDeviceInfo2 =
                new DeviceDataSourceInfo(origin2, device2, true, List.of(providerInfo2));

        mockCompatibleDevices(ImmutableList.of(matchingDeviceInfo1, matchingDeviceInfo2));
        Set<DataOrigin> excludeDataSources =
                Set.of(
                        new DataOrigin.Builder().setPackageName(DEVICE_PACKAGE_NAME).build(),
                        new DataOrigin.Builder().setPackageName(DEVICE_PACKAGE_NAME_2).build());

        Map<String, Set<String>> result =
                mMatchmakingManager.fetchMatchingDevices(
                        Collections.emptySet(), PACKAGE_NAME, Set.of(), excludeDataSources);
        assertThat(result).isEmpty();
    }

    @Test
    public void recordMatchmakingDenial_callsDenialManager() {
        mMatchmakingManager.recordMatchmakingDenial(
                PACKAGE_NAME, Map.of(PACKAGE_NAME_2, List.of(WRITE_EXERCISE)));

        verify(mMatchmakingDenialStateManager)
                .recordMatchmakingDenial(PACKAGE_NAME, PACKAGE_NAME_2, ACTIVITY);
    }

    @Test
    public void recordMatchmakingDenial_sleepCategory_callsDenialManager() {
        mMatchmakingManager.recordMatchmakingDenial(
                PACKAGE_NAME, Map.of(PACKAGE_NAME_2, List.of(WRITE_SLEEP)));

        verify(mMatchmakingDenialStateManager)
                .recordMatchmakingDenial(PACKAGE_NAME, PACKAGE_NAME_2, SLEEP);
    }

    @Test
    public void recordMatchmakingDenial_multiplePermissionsSameCategory_callsDenialManagerOnce() {
        mMatchmakingManager.recordMatchmakingDenial(
                PACKAGE_NAME, Map.of(PACKAGE_NAME_2, List.of(WRITE_EXERCISE, WRITE_STEPS)));

        verify(mMatchmakingDenialStateManager)
                .recordMatchmakingDenial(PACKAGE_NAME, PACKAGE_NAME_2, ACTIVITY);
    }

    @Test
    public void recordMatchmakingDenial_duplicatePermissionsSameCategory_callsDenialManagerOnce() {
        mMatchmakingManager.recordMatchmakingDenial(
                PACKAGE_NAME,
                Map.of(
                        PACKAGE_NAME_2,
                        List.of(WRITE_EXERCISE, WRITE_STEPS, WRITE_EXERCISE, WRITE_EXERCISE)));

        verify(mMatchmakingDenialStateManager)
                .recordMatchmakingDenial(PACKAGE_NAME, PACKAGE_NAME_2, ACTIVITY);
    }

    @Test
    public void recordMatchmakingDenial_multipleCategories_callsForEachCategory() {
        mMatchmakingManager.recordMatchmakingDenial(
                PACKAGE_NAME,
                Map.of(
                        PACKAGE_NAME_2,
                        List.of(
                                WRITE_EXERCISE,
                                WRITE_SLEEP,
                                WRITE_MENSTRUATION,
                                WRITE_HEART_RATE,
                                WRITE_NUTRITION)));

        verify(mMatchmakingDenialStateManager)
                .recordMatchmakingDenial(PACKAGE_NAME, PACKAGE_NAME_2, ACTIVITY);
        verify(mMatchmakingDenialStateManager)
                .recordMatchmakingDenial(PACKAGE_NAME, PACKAGE_NAME_2, SLEEP);
        verify(mMatchmakingDenialStateManager)
                .recordMatchmakingDenial(PACKAGE_NAME, PACKAGE_NAME_2, CYCLE_TRACKING);
        verify(mMatchmakingDenialStateManager)
                .recordMatchmakingDenial(PACKAGE_NAME, PACKAGE_NAME_2, VITALS);
        verify(mMatchmakingDenialStateManager)
                .recordMatchmakingDenial(PACKAGE_NAME, PACKAGE_NAME_2, NUTRITION);
    }

    @Test
    public void recordMatchmakingDenial_multipleMatchingApps_callsForEachApp() {
        mMatchmakingManager.recordMatchmakingDenial(
                PACKAGE_NAME,
                Map.of(
                        PACKAGE_NAME_2,
                        List.of(WRITE_EXERCISE, WRITE_SLEEP),
                        PACKAGE_NAME_3,
                        List.of(WRITE_EXERCISE, WRITE_SLEEP)));

        verify(mMatchmakingDenialStateManager)
                .recordMatchmakingDenial(PACKAGE_NAME, PACKAGE_NAME_2, ACTIVITY);
        verify(mMatchmakingDenialStateManager)
                .recordMatchmakingDenial(PACKAGE_NAME, PACKAGE_NAME_2, SLEEP);
        verify(mMatchmakingDenialStateManager)
                .recordMatchmakingDenial(PACKAGE_NAME, PACKAGE_NAME_3, ACTIVITY);
        verify(mMatchmakingDenialStateManager)
                .recordMatchmakingDenial(PACKAGE_NAME, PACKAGE_NAME_3, SLEEP);
    }

    @Test
    public void recordMatchmakingDenial_filtersNonWritePermissions() {
        mMatchmakingManager.recordMatchmakingDenial(
                PACKAGE_NAME,
                Map.of(
                        PACKAGE_NAME_2,
                        List.of(WRITE_EXERCISE, READ_STEPS, WRITE_SLEEP, READ_SLEEP)));

        verify(mMatchmakingDenialStateManager)
                .recordMatchmakingDenial(PACKAGE_NAME, PACKAGE_NAME_2, ACTIVITY);
        verify(mMatchmakingDenialStateManager)
                .recordMatchmakingDenial(PACKAGE_NAME, PACKAGE_NAME_2, SLEEP);
    }

    @Test
    public void fetchMatchingApps_readingPackageFilteredOut() {
        mockReadingApp(PACKAGE_NAME, ImmutableList.of(READ_STEPS));
        PackageInfo matchingApp = createPackageInfo(PACKAGE_NAME, new String[] {WRITE_STEPS});
        mockCompatibleHealthConnectApps(ImmutableList.of(matchingApp));
        mockAppSystemStatus(PACKAGE_NAME, /* isSystemApp= */ false);
        mockPermissionCheckResult(PACKAGE_NAME, WRITE_STEPS, PERMISSION_DENIED);
        mockHealthPermissionFlags(PACKAGE_NAME, WRITE_STEPS, 0);

        Map<String, Set<String>> result =
                mMatchmakingManager.fetchMatchingApps(Collections.emptySet(), PACKAGE_NAME);

        assertThat(result).isEmpty();
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

    private void mockCompatibleDevices(List<DeviceDataSourceInfo> devices) {
        when(mDeviceDataProviderManager.getDeviceDataSourceInfos()).thenReturn(devices);
    }

    private void mockPackageVisibility(
            String callingPackage, String requestedPackage, boolean hasVisibility) {
        when(mPackageInfoUtils.hasPackageVisibility(
                        eq(callingPackage), eq(requestedPackage), any(), any()))
                .thenReturn(hasVisibility);
    }

    private void mockAppSystemStatus(String packageName, boolean isSystemApp) {
        when(mHealthConnectPermissionHelper.hasNonUserSensitiveHealthPermission(
                        eq(packageName), any(), any()))
                .thenReturn(isSystemApp);
    }

    private void mockPermissionCheckResult(String packageName, String permission, int result) {
        when(mPackageInfoUtils.checkPermission(any(), any(), eq(permission), eq(packageName)))
                .thenReturn(result);
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
