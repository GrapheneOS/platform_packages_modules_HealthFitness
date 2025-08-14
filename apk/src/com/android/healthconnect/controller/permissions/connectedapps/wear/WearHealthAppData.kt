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

package com.android.healthconnect.controller.permissions.connectedapps.wear

import com.android.healthconnect.controller.permissions.app.HealthPermissionStatus
import com.android.healthconnect.controller.permissions.data.FitnessPermissionStrings
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.AdditionalPermission.Companion.READ_HEALTH_DATA_IN_BACKGROUND
import com.android.healthconnect.controller.permissions.data.HealthPermissionType
import com.android.healthconnect.controller.shared.app.AppMetadata
import java.time.Instant

/**
 * Data class that holds all information about an app displayed in the Wear Health Connect
 * permissions screen.
 */
data class WearHealthAppData(
    val packageName: String,
    val appMetadata: AppMetadata,
    val healthPermissionStatus: List<HealthPermissionStatus>,
    val lastAccessTime: Instant? = null,
    val accessLogs: List<PermissionsLastAccess> = emptyList(),
) {
    fun getAllowedFitnessPermissions(): List<HealthPermission.FitnessPermission> {
        val res =
            healthPermissionStatus
                .filter { it.isGranted }
                .map { it.healthPermission }
                .filterIsInstance<HealthPermission.FitnessPermission>()
        return res
    }

    fun getAllowedFitnessPermissionsStringResources(): List<Int> {
        return getAllowedFitnessPermissions().map {
            FitnessPermissionStrings.fromPermissionType(it.fitnessPermissionType).lowercaseLabel
        }
    }

    fun isPermissionRequested(permission: HealthPermission): Boolean {
        return healthPermissionStatus.any { it.healthPermission == permission }
    }

    fun isPermissionAllowed(permission: HealthPermission): Boolean {
        return healthPermissionStatus.any {
            it.healthPermission == permission && it.isGranted == true
        }
    }

    fun anyFitnessPermissionsAllowed(): Boolean {
        return getAllowedFitnessPermissions().isNotEmpty()
    }

    fun isBackgroundPermissionRequested(): Boolean {
        return isPermissionRequested(READ_HEALTH_DATA_IN_BACKGROUND)
    }

    fun isBackgroundPermissionGranted(): Boolean {
        return isPermissionAllowed(READ_HEALTH_DATA_IN_BACKGROUND)
    }

    fun anyFitnessPermissionUsed(): Boolean {
        return accessLogs.isNotEmpty()
    }

    /** Returns true if there is at least one access log for the given fitness permission. */
    fun wasFitnessPermissionUsed(fitnessPermission: HealthPermission.FitnessPermission): Boolean {
        return accessLogs.any { access ->
            access.healthPermissionType == fitnessPermission.fitnessPermissionType
        }
    }

    /**
     * Returns true if the given permission is the last granted fitness read permission. This means
     * that we should also revoke the background permission when we revoke the given permission.
     */
    fun shouldRevokeBackgroundReadAlongWith(healthPermission: HealthPermission): Boolean {
        if (healthPermission !is HealthPermission.FitnessPermission) {
            return false
        }
        // if this is the last granted read permission, then also revoke bgr
        val grantedPermissions = this.healthPermissionStatus.filter { it.isGranted }
        val isThisPermissionStillGranted =
            grantedPermissions.any { it.healthPermission == healthPermission }
        val isThisTheLastFitnessReadPermission =
            grantedPermissions
                .filter { it.healthPermission is HealthPermission.FitnessPermission }
                .size == 1

        return isBackgroundPermissionGranted() &&
            isThisTheLastFitnessReadPermission &&
            isThisPermissionStillGranted
    }
}

/** Returns a list of [WearHealthAppData] apps for which the given fitness permission is granted. */
fun List<WearHealthAppData>.getAllowedApps(
    fitnessPermission: HealthPermission.FitnessPermission,
    includeSystem: Boolean = false,
): List<WearHealthAppData> {
    val allowedApps = this.filter { app -> app.isPermissionAllowed(fitnessPermission) }
    return if (includeSystem) {
        allowedApps
    } else {
        allowedApps.filterNot { app -> app.appMetadata.isSystem }
    }
}

/** Returns a list of [WearHealthAppData] apps for which the given fitness permission is denied. */
fun List<WearHealthAppData>.getDeniedApps(
    fitnessPermission: HealthPermission.FitnessPermission,
    includeSystem: Boolean = false,
): List<WearHealthAppData> {
    val deniedApps =
        this.filter { app -> app.isPermissionRequested(fitnessPermission) }
            .filterNot { app -> app.isPermissionAllowed(fitnessPermission) }
    return if (includeSystem) {
        deniedApps
    } else {
        deniedApps.filterNot { app -> app.appMetadata.isSystem }
    }
}

/**
 * Returns the number of non-system apps that have requested and granted the given fitness
 * permission.
 */
fun List<WearHealthAppData>.getNumberOfAllowedAppsForFitnessPermission(
    fitnessPermission: HealthPermission.FitnessPermission
): Int {
    return this.getAllowedApps(fitnessPermission, includeSystem = false).size
}

/**
 * Returns the number of non-system apps that have requested and denied the given fitness
 * permission.
 */
fun List<WearHealthAppData>.getNumberOfDeniedAppsForFitnessPermission(
    fitnessPermission: HealthPermission.FitnessPermission
): Int {
    return this.getDeniedApps(fitnessPermission, includeSystem = false).size
}

/**
 * Returns the number of non-system apps that have requested and used the given fitness permission.
 */
fun List<WearHealthAppData>.getNumberOfUsedAppsForFitnessPermission(
    fitnessPermission: HealthPermission.FitnessPermission
): Int {
    return this.filterNot { app -> app.appMetadata.isSystem }
        .filter { app -> app.isPermissionRequested(fitnessPermission) }
        .filter { app -> app.wasFitnessPermissionUsed(fitnessPermission) }
        .size
}

/** Returns true if any of the given apps have requested the given permission. */
fun List<WearHealthAppData>.isPermissionRequested(fitnessPermission: HealthPermission): Boolean {
    return this.any { it.isPermissionRequested(fitnessPermission) }
}

/** Used only for Wear permissions, which are only READ. */
data class PermissionsLastAccess(
    val healthPermissionType: HealthPermissionType,
    val lastAccessTime: Instant,
)
