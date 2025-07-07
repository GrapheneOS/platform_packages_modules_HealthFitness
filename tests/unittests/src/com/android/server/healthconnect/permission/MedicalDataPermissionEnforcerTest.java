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

package com.android.server.healthconnect.permission;

import static android.health.connect.HealthPermissions.READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES;
import static android.health.connect.HealthPermissions.READ_MEDICAL_DATA_CONDITIONS;
import static android.health.connect.HealthPermissions.READ_MEDICAL_DATA_LABORATORY_RESULTS;
import static android.health.connect.HealthPermissions.READ_MEDICAL_DATA_MEDICATIONS;
import static android.health.connect.HealthPermissions.READ_MEDICAL_DATA_PERSONAL_DETAILS;
import static android.health.connect.HealthPermissions.READ_MEDICAL_DATA_PRACTITIONER_DETAILS;
import static android.health.connect.HealthPermissions.READ_MEDICAL_DATA_PREGNANCY;
import static android.health.connect.HealthPermissions.READ_MEDICAL_DATA_PROCEDURES;
import static android.health.connect.HealthPermissions.READ_MEDICAL_DATA_SOCIAL_HISTORY;
import static android.health.connect.HealthPermissions.READ_MEDICAL_DATA_VACCINES;
import static android.health.connect.HealthPermissions.READ_MEDICAL_DATA_VISITS;
import static android.health.connect.HealthPermissions.READ_MEDICAL_DATA_VITAL_SIGNS;
import static android.health.connect.HealthPermissions.WRITE_MEDICAL_DATA;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_CONDITIONS;
import static android.health.connect.datatypes.MedicalResource.MEDICAL_RESOURCE_TYPE_VACCINES;
import static android.permission.PermissionManager.PERMISSION_GRANTED;
import static android.permission.PermissionManager.PERMISSION_HARD_DENIED;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.AttributionSource;
import android.permission.PermissionManager;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
public class MedicalDataPermissionEnforcerTest {
    @Mock private PermissionManager mPermissionManager;

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private AttributionSource mAttributionSource;

    private MedicalDataPermissionEnforcer mMedicalDataPermissionEnforcer;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mAttributionSource = buildAttributionSource();
        mMedicalDataPermissionEnforcer = new MedicalDataPermissionEnforcer(mPermissionManager);
    }

    /** enforceWriteMedicalDataPermission */
    @Test
    public void testEnforceWriteMedicalDataPermission_permissionGranted_doesNotThrow() {
        when(mPermissionManager.checkPermissionForDataDelivery(
                        WRITE_MEDICAL_DATA, mAttributionSource, null))
                .thenReturn(PERMISSION_GRANTED);

        mMedicalDataPermissionEnforcer.enforceWriteMedicalDataPermission(mAttributionSource);
    }

    @Test(expected = SecurityException.class)
    public void testEnforceWriteMedicalDataPermission_permissionDenied_throws() {
        when(mPermissionManager.checkPermissionForDataDelivery(
                        WRITE_MEDICAL_DATA, mAttributionSource, null))
                .thenReturn(PERMISSION_HARD_DENIED);

        mMedicalDataPermissionEnforcer.enforceWriteMedicalDataPermission(mAttributionSource);
    }

    /** enforceMedicalReadAccessAndGetEnforceSelfRead */
    @Test
    public void testEnforceMedicalReadAccessAndGetEnforceSelfRead_permissionGranted_returnsFalse() {
        when(mPermissionManager.checkPermissionForDataDelivery(
                        READ_MEDICAL_DATA_VACCINES, mAttributionSource, null))
                .thenReturn(PERMISSION_GRANTED);

        boolean selfRead =
                mMedicalDataPermissionEnforcer.enforceMedicalReadAccessAndGetEnforceSelfRead(
                        MEDICAL_RESOURCE_TYPE_VACCINES, mAttributionSource);

        assertThat(selfRead).isFalse();
    }

    @Test
    public void testEnforceMedicalReadAccessAndGetEnforceSelfRead_onlyWriteGranted_returnsTrue() {
        when(mPermissionManager.checkPermissionForDataDelivery(
                        READ_MEDICAL_DATA_VACCINES, mAttributionSource, null))
                .thenReturn(PERMISSION_HARD_DENIED);
        when(mPermissionManager.checkPermissionForDataDelivery(
                        WRITE_MEDICAL_DATA, mAttributionSource, null))
                .thenReturn(PERMISSION_GRANTED);

        boolean selfRead =
                mMedicalDataPermissionEnforcer.enforceMedicalReadAccessAndGetEnforceSelfRead(
                        MEDICAL_RESOURCE_TYPE_VACCINES, mAttributionSource);

        assertThat(selfRead).isTrue();
    }

    /** getGrantedMedicalPermissions */
    @Test
    public void testGetGrantedMedicalPermissions_allPermissionsGranted_returnsAllPermissions() {
        when(mPermissionManager.checkPermissionForPreflight(anyString(), eq(mAttributionSource)))
                .thenReturn(PERMISSION_GRANTED);

        Set<String> permissions =
                mMedicalDataPermissionEnforcer.getGrantedMedicalPermissionsForPreflight(
                        mAttributionSource);

        assertThat(permissions)
                .containsExactly(
                        READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES,
                        READ_MEDICAL_DATA_LABORATORY_RESULTS,
                        READ_MEDICAL_DATA_MEDICATIONS,
                        READ_MEDICAL_DATA_PERSONAL_DETAILS,
                        READ_MEDICAL_DATA_PRACTITIONER_DETAILS,
                        READ_MEDICAL_DATA_PREGNANCY,
                        READ_MEDICAL_DATA_CONDITIONS,
                        READ_MEDICAL_DATA_PROCEDURES,
                        READ_MEDICAL_DATA_SOCIAL_HISTORY,
                        READ_MEDICAL_DATA_VACCINES,
                        READ_MEDICAL_DATA_VISITS,
                        READ_MEDICAL_DATA_VITAL_SIGNS,
                        WRITE_MEDICAL_DATA);
    }

    @Test
    public void testGetGrantedMedicalPermissions_onePermissionGranted_returnsOnePermission() {
        // For all other permissions, deny.
        when(mPermissionManager.checkPermissionForPreflight(anyString(), eq(mAttributionSource)))
                .thenReturn(PERMISSION_HARD_DENIED);
        when(mPermissionManager.checkPermissionForPreflight(
                        eq(READ_MEDICAL_DATA_VACCINES), eq(mAttributionSource)))
                .thenReturn(PERMISSION_GRANTED);

        Set<String> permissions =
                mMedicalDataPermissionEnforcer.getGrantedMedicalPermissionsForPreflight(
                        mAttributionSource);

        assertThat(permissions).containsExactly(READ_MEDICAL_DATA_VACCINES);
    }

    @Test
    public void testGetGrantedMedicalPermissions_permissionDenied_returnsEmpty() {
        when(mPermissionManager.checkPermissionForPreflight(
                        anyString(), any(AttributionSource.class)))
                .thenReturn(PERMISSION_HARD_DENIED);

        Set<String> permissions =
                mMedicalDataPermissionEnforcer.getGrantedMedicalPermissionsForPreflight(
                        mAttributionSource);

        assertThat(permissions).isEmpty();
    }

    @Test
    public void testEnforceMedicalResourceTypesReadPermissions_emptyList_doesNothing() {
        mMedicalDataPermissionEnforcer.enforceMedicalResourceTypesReadPermissions(
                Collections.emptyList(), mAttributionSource);
        // Verify that checkPermissionForDataDelivery was never called
        verify(mPermissionManager, never())
                .checkPermissionForDataDelivery(anyString(), eq(mAttributionSource), any());
    }

    @Test
    public void testEnforceMedicalResourceTypesReadPermissions_singleTypeGranted_doesNotThrow() {
        List<Integer> resourceTypes = List.of(MEDICAL_RESOURCE_TYPE_VACCINES);
        when(mPermissionManager.checkPermissionForDataDelivery(
                        READ_MEDICAL_DATA_VACCINES, mAttributionSource, null))
                .thenReturn(PERMISSION_GRANTED);

        mMedicalDataPermissionEnforcer.enforceMedicalResourceTypesReadPermissions(
                resourceTypes, mAttributionSource);

        // Verify check was made once for the specific permission
        verify(mPermissionManager, times(1))
                .checkPermissionForDataDelivery(
                        READ_MEDICAL_DATA_VACCINES, mAttributionSource, null);
    }

    @Test
    public void testEnforceMedicalResourceTypesReadPermissions_singleTypeDenied_throws() {
        List<Integer> resourceTypes = List.of(MEDICAL_RESOURCE_TYPE_VACCINES);
        when(mPermissionManager.checkPermissionForDataDelivery(
                        READ_MEDICAL_DATA_VACCINES, mAttributionSource, null))
                .thenReturn(PERMISSION_HARD_DENIED);

        SecurityException exception =
                assertThrows(
                        SecurityException.class,
                        () ->
                                mMedicalDataPermissionEnforcer
                                        .enforceMedicalResourceTypesReadPermissions(
                                                resourceTypes, mAttributionSource));

        // Check the exception message
        assertThat(exception.getMessage())
                .isEqualTo("Calling uid 123 does not have " + READ_MEDICAL_DATA_VACCINES);
        // Verify check was made
        verify(mPermissionManager)
                .checkPermissionForDataDelivery(
                        READ_MEDICAL_DATA_VACCINES, mAttributionSource, null);
    }

    @Test
    public void testEnforceMedicalResourceTypesReadPermissions_multipleTypesGranted_doesNotThrow() {
        List<Integer> resourceTypes =
                List.of(
                        MEDICAL_RESOURCE_TYPE_VACCINES,
                        MEDICAL_RESOURCE_TYPE_CONDITIONS,
                        MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES);
        // Grant all necessary permissions
        when(mPermissionManager.checkPermissionForDataDelivery(
                        READ_MEDICAL_DATA_VACCINES, mAttributionSource, null))
                .thenReturn(PERMISSION_GRANTED);
        when(mPermissionManager.checkPermissionForDataDelivery(
                        READ_MEDICAL_DATA_CONDITIONS, mAttributionSource, null))
                .thenReturn(PERMISSION_GRANTED);
        when(mPermissionManager.checkPermissionForDataDelivery(
                        READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES, mAttributionSource, null))
                .thenReturn(PERMISSION_GRANTED);

        mMedicalDataPermissionEnforcer.enforceMedicalResourceTypesReadPermissions(
                resourceTypes, mAttributionSource);

        // Verify checks were made for each permission
        verify(mPermissionManager)
                .checkPermissionForDataDelivery(
                        READ_MEDICAL_DATA_VACCINES, mAttributionSource, null);
        verify(mPermissionManager)
                .checkPermissionForDataDelivery(
                        READ_MEDICAL_DATA_CONDITIONS, mAttributionSource, null);
        verify(mPermissionManager)
                .checkPermissionForDataDelivery(
                        READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES, mAttributionSource, null);
    }

    @Test
    public void testEnforceMedicalResourceTypesReadPermissions_multipleTypesOneDenied_throws() {
        List<Integer> resourceTypes =
                List.of(
                        MEDICAL_RESOURCE_TYPE_VACCINES, // Granted
                        MEDICAL_RESOURCE_TYPE_CONDITIONS, // Denied
                        MEDICAL_RESOURCE_TYPE_ALLERGIES_INTOLERANCES); // Granted (won't be checked)

        when(mPermissionManager.checkPermissionForDataDelivery(
                        READ_MEDICAL_DATA_VACCINES, mAttributionSource, null))
                .thenReturn(PERMISSION_GRANTED);
        when(mPermissionManager.checkPermissionForDataDelivery(
                        READ_MEDICAL_DATA_CONDITIONS, mAttributionSource, null))
                .thenReturn(PERMISSION_HARD_DENIED); // Deny this one
        when(mPermissionManager.checkPermissionForDataDelivery(
                        READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES, mAttributionSource, null))
                .thenReturn(PERMISSION_GRANTED);

        SecurityException exception =
                assertThrows(
                        SecurityException.class,
                        () ->
                                mMedicalDataPermissionEnforcer
                                        .enforceMedicalResourceTypesReadPermissions(
                                                resourceTypes, mAttributionSource));

        // Check the exception message for the denied permission
        assertThat(exception.getMessage())
                .isEqualTo("Calling uid 123 does not have " + READ_MEDICAL_DATA_CONDITIONS);

        // Verify checks were made up to the point of failure
        verify(mPermissionManager)
                .checkPermissionForDataDelivery(
                        READ_MEDICAL_DATA_VACCINES, mAttributionSource, null);
        verify(mPermissionManager)
                .checkPermissionForDataDelivery(
                        READ_MEDICAL_DATA_CONDITIONS, mAttributionSource, null);
        // Verify the check for ALLERGIES was *not* made because the loop terminated early
        verify(mPermissionManager, never())
                .checkPermissionForDataDelivery(
                        READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES, mAttributionSource, null);
    }

    private static AttributionSource buildAttributionSource() {
        int uid = 123;
        return new AttributionSource.Builder(uid)
                .setPackageName("package")
                .setAttributionTag("tag")
                .build();
    }
}
