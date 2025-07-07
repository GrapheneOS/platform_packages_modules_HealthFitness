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

import static android.health.connect.HealthPermissions.WRITE_MEDICAL_DATA;
import static android.health.connect.internal.datatypes.utils.MedicalResourceTypePermissionMapper.getMedicalReadPermission;
import static android.permission.PermissionManager.PERMISSION_GRANTED;

import android.content.AttributionSource;
import android.health.connect.HealthPermissions;
import android.health.connect.datatypes.MedicalResource;
import android.permission.PermissionManager;
import android.util.ArraySet;

import java.util.Collection;
import java.util.Set;

/**
 * Helper class to force caller of medical data apis to hold api required permissions.
 *
 * @hide
 */
public class MedicalDataPermissionEnforcer {
    private final PermissionManager mPermissionManager;

    public MedicalDataPermissionEnforcer(PermissionManager permissionManager) {
        mPermissionManager = permissionManager;
    }

    /**
     * Enforces that caller has write permission for medical data.
     *
     * @throws SecurityException if the app does not have write permission.
     */
    public void enforceWriteMedicalDataPermission(AttributionSource attributionSource) {
        if (!isPermissionGranted(WRITE_MEDICAL_DATA, attributionSource)) {
            throw new SecurityException(
                    "Calling uid "
                            + attributionSource.getUid()
                            + " does not have "
                            + WRITE_MEDICAL_DATA);
        }
    }

    /**
     * Enforces that caller has either read or write permissions for given medicalResourceType.
     * Returns {@code true} if the caller is allowed to read only records written by itself, false
     * if the caller is allowed to read records written by any apps including itself.
     *
     * @throws SecurityException if the app has neither read nor write permissions for any of the
     *     specified medical resources.
     */
    public boolean enforceMedicalReadAccessAndGetEnforceSelfRead(
            @MedicalResource.MedicalResourceType int medicalResourceType,
            AttributionSource attributionSource) {
        String readPermissionName = getMedicalReadPermission(medicalResourceType);

        if (isPermissionGranted(readPermissionName, attributionSource)) {
            return false;
        }

        if (isPermissionGranted(WRITE_MEDICAL_DATA, attributionSource)) {
            // Apps are always allowed to read self data if they have insert permission.
            return true;
        }
        throw new SecurityException(
                "Calling uid "
                        + attributionSource.getUid()
                        + " does not have "
                        + readPermissionName);
    }

    /**
     * Returns a list of read or write medical permissions the caller has been granted permission to
     * access. Uses checkPermissionForPreflight as this method is not used for delivering data but
     * checking permission state.
     */
    public Set<String> getGrantedMedicalPermissionsForPreflight(
            AttributionSource attributionSource) {
        Set<String> grantedPermissions = new ArraySet<>();

        for (String permission : HealthPermissions.getAllMedicalPermissions()) {
            if (mPermissionManager.checkPermissionForPreflight(permission, attributionSource)
                    == PERMISSION_GRANTED) {
                grantedPermissions.add(permission);
            }
        }
        return grantedPermissions;
    }

    /** Enforces read permissions for given medical resource types */
    public void enforceMedicalResourceTypesReadPermissions(
            Collection<Integer> medicalResourceTypes, AttributionSource attributionSource) {
        medicalResourceTypes.forEach(
                medicalResourceType ->
                        enforceMedicalReadAccess(medicalResourceType, attributionSource));
    }

    private void enforceMedicalReadAccess(
            @MedicalResource.MedicalResourceType int medicalResourceType,
            AttributionSource attributionSource) {
        String readPermissionName = getMedicalReadPermission(medicalResourceType);

        if (!isPermissionGranted(readPermissionName, attributionSource)) {
            throw new SecurityException(
                    "Calling uid "
                            + attributionSource.getUid()
                            + " does not have "
                            + readPermissionName);
        }
    }

    private boolean isPermissionGranted(
            String permissionName, AttributionSource attributionSource) {
        return mPermissionManager.checkPermissionForDataDelivery(
                        permissionName, attributionSource, null)
                == PERMISSION_GRANTED;
    }
}
