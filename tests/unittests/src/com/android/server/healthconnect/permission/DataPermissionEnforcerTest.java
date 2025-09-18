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

package com.android.server.healthconnect.permission;

import static android.content.pm.PackageManager.PERMISSION_DENIED;
import static android.health.connect.HealthPermissions.READ_ACTIVE_CALORIES_BURNED;
import static android.health.connect.HealthPermissions.READ_EXERCISE_ROUTE;
import static android.health.connect.HealthPermissions.READ_EXERCISE_ROUTES;
import static android.health.connect.HealthPermissions.READ_HEART_RATE;
import static android.health.connect.HealthPermissions.READ_STEPS;
import static android.health.connect.HealthPermissions.WRITE_ACTIVE_CALORIES_BURNED;
import static android.health.connect.HealthPermissions.WRITE_EXERCISE;
import static android.health.connect.HealthPermissions.WRITE_EXERCISE_ROUTE;
import static android.health.connect.HealthPermissions.WRITE_STEPS;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_ACTIVE_CALORIES_BURNED;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_EXERCISE_SESSION;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_HEART_RATE;
import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_STEPS;
import static android.permission.PermissionManager.PERMISSION_GRANTED;
import static android.permission.PermissionManager.PERMISSION_HARD_DENIED;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.AttributionSource;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.health.connect.datatypes.StepsRecord;
import android.health.connect.internal.datatypes.ExerciseRouteInternal;
import android.health.connect.internal.datatypes.ExerciseSessionRecordInternal;
import android.health.connect.internal.datatypes.utils.HealthConnectMappings;
import android.os.Build;
import android.os.UserHandle;
import android.permission.PermissionManager;
import android.platform.test.flag.junit.SetFlagsRule;
import android.util.ArrayMap;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;

import com.android.server.healthconnect.fitness.mappings.InternalHealthConnectMappings;
import com.android.server.healthconnect.fitness.recordhelpers.RecordHelper;
import com.android.server.healthconnect.injector.HealthConnectInjector;
import com.android.server.healthconnect.injector.HealthConnectInjectorImpl;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
public class DataPermissionEnforcerTest {

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Mock private PermissionManager mPermissionManager;
    @Mock private PackageManager mPackageManager;
    @Mock private Context mContext;
    @Mock private HealthConnectMappings mMockHealthConnectMappings;
    @Mock private RecordHelper mRecordHelper;

    private InternalHealthConnectMappings mSpyInternalHealthConnectMappings;
    private AttributionSource mAttributionSource;

    private DataPermissionEnforcer mDataPermissionEnforcer;
    private HealthConnectInjector mHealthConnectInjector;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mAttributionSource = buildAttributionSource();

        when(mContext.getUser()).thenReturn(UserHandle.CURRENT);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mContext.createContextAsUser(any(), anyInt())).thenReturn(mContext);
        mHealthConnectInjector =
                HealthConnectInjectorImpl.newBuilderForTest(getInstrumentation().getContext())
                        .build();
        mSpyInternalHealthConnectMappings =
                spy(mHealthConnectInjector.getInternalHealthConnectMappings());

        mDataPermissionEnforcer =
                new DataPermissionEnforcer(
                        mPermissionManager, mContext, mSpyInternalHealthConnectMappings);
    }

    private void setUpMocksForMultiPermTests() {
        doReturn(mMockHealthConnectMappings)
                .when(mSpyInternalHealthConnectMappings)
                .getExternalMappings();
        when(mMockHealthConnectMappings.getRecordIdToExternalRecordClassMap())
                .thenReturn(ImmutableMap.of(TEST_RECORD_TYPE, StepsRecord.class));

        mDataPermissionEnforcer =
                new DataPermissionEnforcer(
                        mPermissionManager, mContext, mSpyInternalHealthConnectMappings);
    }

    private static final int TEST_RECORD_TYPE = -1;
    private static final int PERMISSION_CATEGORY_1 = 1000;
    private static final int PERMISSION_CATEGORY_2 = 1001;
    private static final String READ_PERM_1 = "READ_PERM_1";
    private static final String WRITE_PERM_1 = "WRITE_PERM_1";
    private static final String READ_PERM_2 = "READ_PERM_2";
    private static final String WRITE_PERM_2 = "WRITE_PERM_2";

    /** enforceRecordIdsWritePermissions */
    @Test
    public void testEnforceRecordIdsWritePermissions_permissionGranted_doesNotThrow() {
        when(mPermissionManager.checkPermissionForDataDelivery(
                        WRITE_STEPS, mAttributionSource, null))
                .thenReturn(PERMISSION_GRANTED);

        mDataPermissionEnforcer.enforceRecordIdsWritePermissions(
                List.of(RECORD_TYPE_STEPS), mAttributionSource);
    }

    @Test(expected = SecurityException.class)
    public void testEnforceRecordIdsWritePermissions_permissionDenied_throwsSecurityException() {
        when(mPermissionManager.checkPermissionForDataDelivery(
                        WRITE_STEPS, mAttributionSource, null))
                .thenReturn(PERMISSION_HARD_DENIED);

        mDataPermissionEnforcer.enforceRecordIdsWritePermissions(
                List.of(RECORD_TYPE_STEPS), mAttributionSource);
    }

    @Test(expected = SecurityException.class)
    public void testEnforceRecordIdsWritePermissions_onePermissionDenied_throwsSecurityException() {
        when(mPermissionManager.checkPermissionForDataDelivery(
                        WRITE_STEPS, mAttributionSource, null))
                .thenReturn(PERMISSION_GRANTED);
        when(mPermissionManager.checkPermissionForDataDelivery(
                        WRITE_ACTIVE_CALORIES_BURNED, mAttributionSource, null))
                .thenReturn(PERMISSION_HARD_DENIED);

        mDataPermissionEnforcer.enforceRecordIdsWritePermissions(
                List.of(RECORD_TYPE_STEPS, RECORD_TYPE_ACTIVE_CALORIES_BURNED), mAttributionSource);
    }

    @Test
    public void testEnforceRecordIdsWritePermissions_multipleRecordTypes_oneDenied_throws() {
        when(mPermissionManager.checkPermissionForDataDelivery(
                        WRITE_STEPS, mAttributionSource, null))
                .thenReturn(PERMISSION_GRANTED);
        when(mPermissionManager.checkPermissionForDataDelivery(
                        WRITE_ACTIVE_CALORIES_BURNED, mAttributionSource, null))
                .thenReturn(PERMISSION_HARD_DENIED);

        assertThrows(
                SecurityException.class,
                () ->
                        mDataPermissionEnforcer.enforceRecordIdsWritePermissions(
                                List.of(RECORD_TYPE_STEPS, RECORD_TYPE_ACTIVE_CALORIES_BURNED),
                                mAttributionSource));
    }

    /** enforceRecordIdsReadPermissions */
    @Test
    public void testEnforceRecordIdsReadPermissions_permissionGranted_doesNotThrow() {
        when(mPermissionManager.checkPermissionForDataDelivery(
                        READ_STEPS, mAttributionSource, null))
                .thenReturn(PERMISSION_GRANTED);

        mDataPermissionEnforcer.enforceRecordIdsReadPermissions(
                List.of(RECORD_TYPE_STEPS), mAttributionSource);
    }

    @Test(expected = SecurityException.class)
    public void testEnforceRecordIdsReadPermissions_permissionDenied_throwsSecurityException() {
        when(mPermissionManager.checkPermissionForDataDelivery(
                        READ_STEPS, mAttributionSource, null))
                .thenReturn(PERMISSION_HARD_DENIED);

        mDataPermissionEnforcer.enforceRecordIdsReadPermissions(
                List.of(RECORD_TYPE_STEPS), mAttributionSource);
    }

    @Test(expected = SecurityException.class)
    public void testEnforceRecordIdsReadPermissions_onePermissionDenied_throwsSecurityException() {
        when(mPermissionManager.checkPermissionForDataDelivery(
                        READ_STEPS, mAttributionSource, null))
                .thenReturn(PERMISSION_GRANTED);
        when(mPermissionManager.checkPermissionForDataDelivery(
                        READ_ACTIVE_CALORIES_BURNED, mAttributionSource, null))
                .thenReturn(PERMISSION_HARD_DENIED);

        mDataPermissionEnforcer.enforceRecordIdsReadPermissions(
                List.of(RECORD_TYPE_STEPS, RECORD_TYPE_ACTIVE_CALORIES_BURNED), mAttributionSource);
    }

    @Test
    public void testEnforceRecordIdsReadPermissions_multipleRecordTypes_oneDenied_throws() {
        when(mPermissionManager.checkPermissionForDataDelivery(
                        READ_STEPS, mAttributionSource, null))
                .thenReturn(PERMISSION_GRANTED);
        when(mPermissionManager.checkPermissionForDataDelivery(
                        READ_ACTIVE_CALORIES_BURNED, mAttributionSource, null))
                .thenReturn(PERMISSION_HARD_DENIED);

        assertThrows(
                SecurityException.class,
                () ->
                        mDataPermissionEnforcer.enforceRecordIdsReadPermissions(
                                List.of(RECORD_TYPE_STEPS, RECORD_TYPE_ACTIVE_CALORIES_BURNED),
                                mAttributionSource));
    }

    /** Tests for multi-permission logic */
    @Test
    public void testReadPermissions_multiplePermissions_noneGranted_throws() {
        setUpMocksForMultiPermTests();
        when(mMockHealthConnectMappings.getHealthPermissionCategoriesForRecordType(
                        TEST_RECORD_TYPE))
                .thenReturn(ImmutableSet.of(PERMISSION_CATEGORY_1, PERMISSION_CATEGORY_2));
        when(mMockHealthConnectMappings.getHealthReadPermission(PERMISSION_CATEGORY_1))
                .thenReturn(READ_PERM_1);
        when(mMockHealthConnectMappings.getHealthReadPermission(PERMISSION_CATEGORY_2))
                .thenReturn(READ_PERM_2);
        when(mPermissionManager.checkPermissionForDataDelivery(
                        READ_PERM_1, mAttributionSource, null))
                .thenReturn(PERMISSION_HARD_DENIED);
        when(mPermissionManager.checkPermissionForDataDelivery(
                        READ_PERM_2, mAttributionSource, null))
                .thenReturn(PERMISSION_HARD_DENIED);

        assertThrows(
                SecurityException.class,
                () ->
                        mDataPermissionEnforcer.enforceRecordIdsReadPermissions(
                                List.of(TEST_RECORD_TYPE), mAttributionSource));
    }

    @Test
    public void testReadPermissions_multiplePermissions_oneGranted_doesNotThrow() {
        setUpMocksForMultiPermTests();
        when(mMockHealthConnectMappings.getHealthPermissionCategoriesForRecordType(
                        TEST_RECORD_TYPE))
                .thenReturn(ImmutableSet.of(PERMISSION_CATEGORY_1, PERMISSION_CATEGORY_2));
        when(mMockHealthConnectMappings.getHealthReadPermission(PERMISSION_CATEGORY_1))
                .thenReturn(READ_PERM_1);
        when(mMockHealthConnectMappings.getHealthReadPermission(PERMISSION_CATEGORY_2))
                .thenReturn(READ_PERM_2);
        when(mPermissionManager.checkPermissionForDataDelivery(
                        READ_PERM_1, mAttributionSource, null))
                .thenReturn(PERMISSION_HARD_DENIED);
        when(mPermissionManager.checkPermissionForDataDelivery(
                        READ_PERM_2, mAttributionSource, null))
                .thenReturn(PERMISSION_GRANTED);

        mDataPermissionEnforcer.enforceRecordIdsReadPermissions(
                List.of(TEST_RECORD_TYPE), mAttributionSource); // Should not throw
    }

    @Test
    public void testReadPermissions_multiplePermissions_allGranted_doesNotThrow() {
        setUpMocksForMultiPermTests();
        when(mMockHealthConnectMappings.getHealthPermissionCategoriesForRecordType(
                        TEST_RECORD_TYPE))
                .thenReturn(ImmutableSet.of(PERMISSION_CATEGORY_1, PERMISSION_CATEGORY_2));
        when(mMockHealthConnectMappings.getHealthReadPermission(PERMISSION_CATEGORY_1))
                .thenReturn(READ_PERM_1);
        when(mMockHealthConnectMappings.getHealthReadPermission(PERMISSION_CATEGORY_2))
                .thenReturn(READ_PERM_2);
        when(mPermissionManager.checkPermissionForDataDelivery(
                        READ_PERM_1, mAttributionSource, null))
                .thenReturn(PERMISSION_GRANTED);
        when(mPermissionManager.checkPermissionForDataDelivery(
                        READ_PERM_2, mAttributionSource, null))
                .thenReturn(PERMISSION_GRANTED);

        mDataPermissionEnforcer.enforceRecordIdsReadPermissions(
                List.of(TEST_RECORD_TYPE), mAttributionSource); // Should not throw
    }

    @Test
    public void testWritePermissions_multiplePermissions_noneGranted_throws() {
        setUpMocksForMultiPermTests();
        when(mMockHealthConnectMappings.getHealthPermissionCategoriesForRecordType(
                        TEST_RECORD_TYPE))
                .thenReturn(ImmutableSet.of(PERMISSION_CATEGORY_1, PERMISSION_CATEGORY_2));
        when(mMockHealthConnectMappings.getHealthWritePermission(PERMISSION_CATEGORY_1))
                .thenReturn(WRITE_PERM_1);
        when(mMockHealthConnectMappings.getHealthWritePermission(PERMISSION_CATEGORY_2))
                .thenReturn(WRITE_PERM_2);
        when(mPermissionManager.checkPermissionForDataDelivery(
                        WRITE_PERM_1, mAttributionSource, null))
                .thenReturn(PERMISSION_HARD_DENIED);
        when(mPermissionManager.checkPermissionForDataDelivery(
                        WRITE_PERM_2, mAttributionSource, null))
                .thenReturn(PERMISSION_HARD_DENIED);

        assertThrows(
                SecurityException.class,
                () ->
                        mDataPermissionEnforcer.enforceRecordIdsWritePermissions(
                                List.of(TEST_RECORD_TYPE), mAttributionSource));
    }

    @Test
    public void testWritePermissions_multiplePermissions_oneGranted_doesNotThrow() {
        setUpMocksForMultiPermTests();
        when(mMockHealthConnectMappings.getHealthPermissionCategoriesForRecordType(
                        TEST_RECORD_TYPE))
                .thenReturn(ImmutableSet.of(PERMISSION_CATEGORY_1, PERMISSION_CATEGORY_2));
        when(mMockHealthConnectMappings.getHealthWritePermission(PERMISSION_CATEGORY_1))
                .thenReturn(WRITE_PERM_1);
        when(mMockHealthConnectMappings.getHealthWritePermission(PERMISSION_CATEGORY_2))
                .thenReturn(WRITE_PERM_2);
        when(mPermissionManager.checkPermissionForDataDelivery(
                        WRITE_PERM_1, mAttributionSource, null))
                .thenReturn(PERMISSION_GRANTED);
        when(mPermissionManager.checkPermissionForDataDelivery(
                        WRITE_PERM_2, mAttributionSource, null))
                .thenReturn(PERMISSION_HARD_DENIED);

        mDataPermissionEnforcer.enforceRecordIdsWritePermissions(
                List.of(TEST_RECORD_TYPE), mAttributionSource); // Should not throw
    }

    @Test
    public void testWritePermissions_multiplePermissions_allGranted_doesNotThrow() {
        setUpMocksForMultiPermTests();
        when(mMockHealthConnectMappings.getHealthPermissionCategoriesForRecordType(
                        TEST_RECORD_TYPE))
                .thenReturn(ImmutableSet.of(PERMISSION_CATEGORY_1, PERMISSION_CATEGORY_2));
        when(mMockHealthConnectMappings.getHealthWritePermission(PERMISSION_CATEGORY_1))
                .thenReturn(WRITE_PERM_1);
        when(mMockHealthConnectMappings.getHealthWritePermission(PERMISSION_CATEGORY_2))
                .thenReturn(WRITE_PERM_2);
        when(mPermissionManager.checkPermissionForDataDelivery(
                        WRITE_PERM_1, mAttributionSource, null))
                .thenReturn(PERMISSION_GRANTED);
        when(mPermissionManager.checkPermissionForDataDelivery(
                        WRITE_PERM_2, mAttributionSource, null))
                .thenReturn(PERMISSION_GRANTED);

        mDataPermissionEnforcer.enforceRecordIdsWritePermissions(
                List.of(TEST_RECORD_TYPE), mAttributionSource); // Should not throw
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
    @Test(expected = SecurityException.class)
    public void
            testEnforceRecordIdsReadPermissions_permissionGranted_heartRateFromSplitPermission_throwsSecurityException() {
        when(mPermissionManager.checkPermissionForDataDelivery(
                        READ_HEART_RATE, mAttributionSource, null))
                .thenReturn(PERMISSION_GRANTED);
        try {
            when(mPackageManager.getPackageInfo(eq(mAttributionSource.getPackageName()), any()))
                    .thenReturn(
                            buildPackageInfo(
                                    mAttributionSource.getPackageName(), /* targetSdk= */ 34));
        } catch (NameNotFoundException e) {
            fail("PackageManager.getPackageInfo threw NameNotFoundException: " + e.getMessage());
        }
        when(mPackageManager.getPermissionFlags(
                        eq(READ_HEART_RATE), eq(mAttributionSource.getPackageName()), any()))
                .thenReturn(PackageManager.FLAG_PERMISSION_REVOKE_WHEN_REQUESTED);

        mDataPermissionEnforcer.enforceRecordIdsReadPermissions(
                List.of(RECORD_TYPE_HEART_RATE), mAttributionSource);
    }

    /** enforceReadAccessAndGetEnforceSelfRead */
    @Test
    public void testEnforceReadAccessAndGetEnforceSelfRead_readPermissionGranted_returnsFalse() {
        when(mPermissionManager.checkPermissionForDataDelivery(
                        READ_STEPS, mAttributionSource, null))
                .thenReturn(PERMISSION_GRANTED);

        boolean enforceSelfRead =
                mDataPermissionEnforcer.enforceReadAccessAndGetEnforceSelfRead(
                        RECORD_TYPE_STEPS, mAttributionSource);

        assertThat(enforceSelfRead).isFalse();
    }

    @Test
    public void
            testEnforceReadAccessAndGetEnforceSelfRead_onlyWritePermissionGranted_returnsTrue() {
        when(mPermissionManager.checkPermissionForDataDelivery(
                        READ_STEPS, mAttributionSource, null))
                .thenReturn(PERMISSION_HARD_DENIED);
        when(mPermissionManager.checkPermissionForDataDelivery(
                        WRITE_STEPS, mAttributionSource, null))
                .thenReturn(PERMISSION_GRANTED);

        boolean enforceSelfRead =
                mDataPermissionEnforcer.enforceReadAccessAndGetEnforceSelfRead(
                        RECORD_TYPE_STEPS, mAttributionSource);

        assertThat(enforceSelfRead).isTrue();
    }

    @Test(expected = SecurityException.class)
    public void
            testEnforceReadAccessAndGetEnforceSelfRead_permissionsDenied_throwsSecurityException() {
        when(mPermissionManager.checkPermissionForDataDelivery(
                        READ_STEPS, mAttributionSource, null))
                .thenReturn(PERMISSION_HARD_DENIED);
        when(mPermissionManager.checkPermissionForDataDelivery(
                        WRITE_STEPS, mAttributionSource, null))
                .thenReturn(PERMISSION_HARD_DENIED);

        mDataPermissionEnforcer.enforceReadAccessAndGetEnforceSelfRead(
                RECORD_TYPE_STEPS, mAttributionSource);
    }

    @Test
    public void
            testEnforceReadAccessAndGetEnforceSelfReadList_oneWritePermissionGranted_returnsTrue() {
        when(mPermissionManager.checkPermissionForDataDelivery(
                        READ_STEPS, mAttributionSource, null))
                .thenReturn(PERMISSION_HARD_DENIED);
        when(mPermissionManager.checkPermissionForDataDelivery(
                        WRITE_STEPS, mAttributionSource, null))
                .thenReturn(PERMISSION_GRANTED);
        when(mPermissionManager.checkPermissionForDataDelivery(
                        READ_ACTIVE_CALORIES_BURNED, mAttributionSource, null))
                .thenReturn(PERMISSION_GRANTED);

        boolean enforceSelfRead =
                mDataPermissionEnforcer.enforceReadAccessAndGetEnforceSelfRead(
                        List.of(RECORD_TYPE_STEPS, RECORD_TYPE_ACTIVE_CALORIES_BURNED),
                        mAttributionSource);

        assertThat(enforceSelfRead).isTrue();
    }

    /** enforceRecordsWritePermissions */
    @Test(expected = SecurityException.class)
    public void
            testEnforceRecordsWritePermissions_onlyMainPermissionGranted_throwsSecurityException() {
        when(mPermissionManager.checkPermissionForDataDelivery(
                        WRITE_EXERCISE, mAttributionSource, null))
                .thenReturn(PERMISSION_GRANTED);
        when(mPermissionManager.checkPermissionForDataDelivery(
                        WRITE_EXERCISE_ROUTE, mAttributionSource, null))
                .thenReturn(PERMISSION_HARD_DENIED);
        ExerciseRouteInternal.LocationInternal locationInternal =
                new ExerciseRouteInternal.LocationInternal();
        ExerciseRouteInternal route = new ExerciseRouteInternal(List.of(locationInternal));
        ExerciseSessionRecordInternal record = new ExerciseSessionRecordInternal().setRoute(route);

        mDataPermissionEnforcer.enforceRecordsWritePermissions(List.of(record), mAttributionSource);
    }

    @Test(expected = SecurityException.class)
    public void
            testEnforceRecordsWritePermissions_extraPermissionGranted_throwsSecurityException() {
        when(mPermissionManager.checkPermissionForDataDelivery(
                        WRITE_EXERCISE, mAttributionSource, null))
                .thenReturn(PERMISSION_HARD_DENIED);
        when(mPermissionManager.checkPermissionForDataDelivery(
                        WRITE_EXERCISE_ROUTE, mAttributionSource, null))
                .thenReturn(PERMISSION_GRANTED);

        ExerciseSessionRecordInternal record = new ExerciseSessionRecordInternal();
        mDataPermissionEnforcer.enforceRecordsWritePermissions(List.of(record), mAttributionSource);
    }

    @Test
    public void testEnforceRecordsWritePermissions_allPermissionsGranted_doesNotThrow() {
        when(mPermissionManager.checkPermissionForDataDelivery(
                        WRITE_EXERCISE, mAttributionSource, null))
                .thenReturn(PERMISSION_GRANTED);
        when(mPermissionManager.checkPermissionForDataDelivery(
                        WRITE_EXERCISE_ROUTE, mAttributionSource, null))
                .thenReturn(PERMISSION_GRANTED);
        when(mPermissionManager.checkPermissionForDataDelivery(
                        eq("GRANULAR_PERMISSION"), any(), any()))
                .thenReturn(PERMISSION_GRANTED);
        doReturn(mRecordHelper).when(mSpyInternalHealthConnectMappings).getRecordHelper(anyInt());
        when(mRecordHelper.getGranularWritePermissions(any()))
                .thenReturn(Set.of("GRANULAR_PERMISSION"));

        ExerciseSessionRecordInternal record = new ExerciseSessionRecordInternal();
        mDataPermissionEnforcer.enforceRecordsWritePermissions(List.of(record), mAttributionSource);

        verify(mRecordHelper).getGranularWritePermissions(record);
    }

    @Test
    public void
            testEnforceRecordsWritePermissions_granularPermissionDenied_throwsSecurityException() {
        when(mPermissionManager.checkPermissionForDataDelivery(
                        WRITE_EXERCISE, mAttributionSource, null))
                .thenReturn(PERMISSION_GRANTED);
        when(mPermissionManager.checkPermissionForDataDelivery(
                        WRITE_EXERCISE_ROUTE, mAttributionSource, null))
                .thenReturn(PERMISSION_GRANTED);
        when(mPermissionManager.checkPermissionForDataDelivery(
                        eq("GRANULAR_PERMISSION"), any(), any()))
                .thenReturn(PERMISSION_DENIED);
        doReturn(mRecordHelper).when(mSpyInternalHealthConnectMappings).getRecordHelper(anyInt());
        when(mRecordHelper.getGranularWritePermissions(any()))
                .thenReturn(Set.of("GRANULAR_PERMISSION"));

        ExerciseSessionRecordInternal record = new ExerciseSessionRecordInternal();
        assertThrows(
                SecurityException.class,
                () ->
                        mDataPermissionEnforcer.enforceRecordsWritePermissions(
                                List.of(record), mAttributionSource));
    }

    /** enforceAnyOfPermissions */
    @Test
    public void testEnforceAnyOfPermissions_onePermissionsGranted_doesNotThrow() {
        when(mContext.checkCallingPermission(READ_STEPS)).thenReturn(PERMISSION_DENIED);
        when(mContext.checkCallingPermission(WRITE_STEPS)).thenReturn(PERMISSION_GRANTED);

        mDataPermissionEnforcer.enforceAnyOfPermissions(READ_STEPS, WRITE_STEPS);
    }

    @Test(expected = SecurityException.class)
    public void testEnforceAnyOfPermissions_allPermissionsDenied_throwsSecurityException() {
        when(mContext.checkCallingPermission(READ_STEPS)).thenReturn(PERMISSION_DENIED);
        when(mContext.checkCallingPermission(WRITE_STEPS)).thenReturn(PERMISSION_DENIED);

        mDataPermissionEnforcer.enforceAnyOfPermissions(READ_STEPS, WRITE_STEPS);
    }

    /** collectGrantedExtraReadPermissions */
    @Test
    public void testCollectGrantedExtraReadPermissions_permissionsGranted_returnsPermissions() {
        when(mPermissionManager.checkPermissionForDataDelivery(
                        READ_EXERCISE_ROUTES, mAttributionSource, null))
                .thenReturn(PERMISSION_GRANTED);
        when(mPermissionManager.checkPermissionForDataDelivery(
                        READ_EXERCISE_ROUTE, mAttributionSource, null))
                .thenReturn(PERMISSION_GRANTED);
        when(mPermissionManager.checkPermissionForDataDelivery(
                        WRITE_EXERCISE_ROUTE, mAttributionSource, null))
                .thenReturn(PERMISSION_GRANTED);

        Set<String> permissions =
                mDataPermissionEnforcer.collectGrantedExtraReadPermissions(
                        Set.of(RECORD_TYPE_EXERCISE_SESSION), mAttributionSource);

        assertThat(permissions)
                .containsExactly(READ_EXERCISE_ROUTES, READ_EXERCISE_ROUTE, WRITE_EXERCISE_ROUTE);
    }

    @Test
    public void testCollectGrantedExtraReadPermissions_permissionDenied_removesPermission() {
        when(mPermissionManager.checkPermissionForDataDelivery(
                        READ_EXERCISE_ROUTES, mAttributionSource, null))
                .thenReturn(PERMISSION_HARD_DENIED);
        when(mPermissionManager.checkPermissionForDataDelivery(
                        READ_EXERCISE_ROUTE, mAttributionSource, null))
                .thenReturn(PERMISSION_GRANTED);
        when(mPermissionManager.checkPermissionForDataDelivery(
                        WRITE_EXERCISE_ROUTE, mAttributionSource, null))
                .thenReturn(PERMISSION_HARD_DENIED);

        Set<String> permissions =
                mDataPermissionEnforcer.collectGrantedExtraReadPermissions(
                        Set.of(RECORD_TYPE_EXERCISE_SESSION), mAttributionSource);

        assertThat(permissions).containsExactly(READ_EXERCISE_ROUTE);
    }

    /** collectExtraWritePermissionStateMapping */
    @Test
    public void
            testCollectExtraWritePermissionStateMapping_permissionsGranted_permissionsMarkedTrue() {
        ExerciseSessionRecordInternal record = new ExerciseSessionRecordInternal();
        when(mPermissionManager.checkPermissionForDataDelivery(
                        WRITE_EXERCISE_ROUTE, mAttributionSource, null))
                .thenReturn(PERMISSION_GRANTED);

        Map<String, Boolean> permissionState =
                mDataPermissionEnforcer.collectExtraWritePermissionStateMapping(
                        List.of(record), mAttributionSource);

        Map<String, Boolean> expected = new ArrayMap<>();
        expected.put(WRITE_EXERCISE_ROUTE, true);
        assertThat(permissionState).containsExactlyEntriesIn(expected);
    }

    @Test
    public void
            testCollectExtraWritePermissionStateMapping_permissionDenied_permissionsMarkedFalse() {
        ExerciseSessionRecordInternal record = new ExerciseSessionRecordInternal();
        when(mPermissionManager.checkPermissionForDataDelivery(
                        WRITE_EXERCISE_ROUTE, mAttributionSource, null))
                .thenReturn(PERMISSION_HARD_DENIED);

        Map<String, Boolean> permissionState =
                mDataPermissionEnforcer.collectExtraWritePermissionStateMapping(
                        List.of(record), mAttributionSource);

        Map<String, Boolean> expected = new ArrayMap<>();
        expected.put(WRITE_EXERCISE_ROUTE, false);
        assertThat(permissionState).containsExactlyEntriesIn(expected);
    }

    private static AttributionSource buildAttributionSource() {
        int uid = 123;
        return new AttributionSource.Builder(uid)
                .setPackageName("package")
                .setAttributionTag("tag")
                .build();
    }

    private static PackageInfo buildPackageInfo(String packageName, int targetSdk) {
        PackageInfo info = new PackageInfo();
        ApplicationInfo aInfo = new ApplicationInfo();
        aInfo.targetSdkVersion = targetSdk;
        info.applicationInfo = aInfo;
        info.packageName = info.applicationInfo.packageName = packageName;
        return info;
    }
}
