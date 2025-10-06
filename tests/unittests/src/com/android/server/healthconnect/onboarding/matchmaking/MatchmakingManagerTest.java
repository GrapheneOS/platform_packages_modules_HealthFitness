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
import java.util.Map;
import java.util.Set;

/** Test class for the {@link MatchmakingManager} class. */
@RunWith(AndroidJUnit4.class)
public class MatchmakingManagerTest {

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock private HealthConnectPermissionHelper mHealthConnectPermissionHelper;
    @Mock private PackageInfoUtils mPackageInfoUtils;
    @Mock private MatchmakingDenialStateManager mMatchmakingDenialStateManager;

    private final HealthConnectMappings mHealthConnectMappings = new HealthConnectMappings();

    private MatchmakingManager mMatchmakingManager;

    private static final String PACKAGE_NAME = "com.example.app";
    private static final String PACKAGE_NAME_2 = "com.example.app2";
    private static final String PACKAGE_NAME_3 = "com.example.app3";

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
                        mMatchmakingDenialStateManager);
        when(mMatchmakingDenialStateManager.isMatchmakingPaused(anyString(), anyString(), anyInt()))
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
    public void recordMatchmakingDenial_callsDenialManager() {
        mMatchmakingManager.recordMatchmakingDenial(
                PACKAGE_NAME, List.of(PACKAGE_NAME_2), List.of(WRITE_EXERCISE));

        verify(mMatchmakingDenialStateManager)
                .recordMatchmakingDenial(PACKAGE_NAME, PACKAGE_NAME_2, ACTIVITY);
    }

    @Test
    public void recordMatchmakingDenial_sleepCategory_callsDenialManager() {
        mMatchmakingManager.recordMatchmakingDenial(
                PACKAGE_NAME, List.of(PACKAGE_NAME_2), List.of(WRITE_SLEEP));

        verify(mMatchmakingDenialStateManager)
                .recordMatchmakingDenial(PACKAGE_NAME, PACKAGE_NAME_2, SLEEP);
    }

    @Test
    public void recordMatchmakingDenial_multiplePermissionsSameCategory_callsDenialManagerOnce() {
        mMatchmakingManager.recordMatchmakingDenial(
                PACKAGE_NAME, List.of(PACKAGE_NAME_2), List.of(WRITE_EXERCISE, WRITE_STEPS));

        verify(mMatchmakingDenialStateManager)
                .recordMatchmakingDenial(PACKAGE_NAME, PACKAGE_NAME_2, ACTIVITY);
    }

    @Test
    public void recordMatchmakingDenial_duplicatePermissionsSameCategory_callsDenialManagerOnce() {
        mMatchmakingManager.recordMatchmakingDenial(
                PACKAGE_NAME,
                List.of(PACKAGE_NAME_2),
                List.of(WRITE_EXERCISE, WRITE_STEPS, WRITE_EXERCISE, WRITE_EXERCISE));

        verify(mMatchmakingDenialStateManager)
                .recordMatchmakingDenial(PACKAGE_NAME, PACKAGE_NAME_2, ACTIVITY);
    }

    @Test
    public void recordMatchmakingDenial_multipleCategories_callsForEachCategory() {
        mMatchmakingManager.recordMatchmakingDenial(
                PACKAGE_NAME,
                List.of(PACKAGE_NAME_2),
                List.of(
                        WRITE_EXERCISE,
                        WRITE_SLEEP,
                        WRITE_MENSTRUATION,
                        WRITE_HEART_RATE,
                        WRITE_NUTRITION));

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
                List.of(PACKAGE_NAME_2, PACKAGE_NAME_3),
                List.of(WRITE_EXERCISE, WRITE_SLEEP));

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
                List.of(PACKAGE_NAME_2),
                List.of(WRITE_EXERCISE, READ_STEPS, WRITE_SLEEP, READ_SLEEP));

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
