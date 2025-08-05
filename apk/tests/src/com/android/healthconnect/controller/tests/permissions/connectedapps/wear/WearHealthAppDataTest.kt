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

package com.android.healthconnect.controller.tests.permissions.connectedapps.wear

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.permissions.connectedapps.wear.PermissionsLastAccess
import com.android.healthconnect.controller.permissions.connectedapps.wear.WearHealthAppData
import com.android.healthconnect.controller.permissions.connectedapps.wear.getAllowedApps
import com.android.healthconnect.controller.permissions.connectedapps.wear.getDeniedApps
import com.android.healthconnect.controller.permissions.connectedapps.wear.getNumberOfAllowedAppsForFitnessPermission
import com.android.healthconnect.controller.permissions.connectedapps.wear.getNumberOfDeniedAppsForFitnessPermission
import com.android.healthconnect.controller.permissions.connectedapps.wear.getNumberOfUsedAppsForFitnessPermission
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.permissions.data.HealthPermission.AdditionalPermission.Companion.READ_HEALTH_DATA_IN_BACKGROUND
import com.android.healthconnect.controller.tests.utils.NOW
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WearHealthAppDataTest {

    @Test
    fun getAllowedDataTypes_returnsOnlyAllowedFitnessTypes() {
        val wearAppData =
            WearHealthAppData(
                packageName = "package.name",
                appMetadata = appMetadataOne,
                healthPermissionStatus =
                    listOf(
                        GRANTED_READ_HEART_RATE_PERMISSION,
                        DENIED_READ_SKIN_TEMPERATURE_PERMISSION,
                        DENIED_READ_OXYGEN_SATURATION_PERMISSION,
                        GRANTED_READ_HEALTH_DATA_IN_BACKGROUND_PERMISSION,
                    ),
            )

        val actual = wearAppData.getAllowedFitnessPermissions()
        assertThat(actual).containsExactly(READ_HEART_RATE_PERMISSION)
    }

    @Test
    fun getAllowedDataTypeStringResources_returnsOnlyAllowedFitnessTypes() {
        val wearAppData =
            WearHealthAppData(
                packageName = "package.name",
                appMetadata = appMetadataOne,
                healthPermissionStatus =
                    listOf(
                        DENIED_READ_HEART_RATE_PERMISSION,
                        GRANTED_READ_SKIN_TEMPERATURE_PERMISSION,
                        DENIED_READ_OXYGEN_SATURATION_PERMISSION,
                        GRANTED_READ_HEALTH_DATA_IN_BACKGROUND_PERMISSION,
                    ),
            )

        val actual = wearAppData.getAllowedFitnessPermissionsStringResources()
        assertThat(actual).containsExactly(R.string.skin_temperature_lowercase_label)
    }

    @Test
    fun anyFitnessPermissionsAllowed_whenFitnessAllowed_returnsTrue() {
        val wearAppData =
            WearHealthAppData(
                packageName = "package.name",
                appMetadata = appMetadataOne,
                healthPermissionStatus =
                    listOf(
                        DENIED_READ_HEART_RATE_PERMISSION,
                        DENIED_READ_SKIN_TEMPERATURE_PERMISSION,
                        GRANTED_READ_OXYGEN_SATURATION_PERMISSION,
                        DENIED_READ_HEALTH_DATA_IN_BACKGROUND_PERMISSION,
                    ),
            )

        assertThat(wearAppData.anyFitnessPermissionsAllowed()).isTrue()
    }

    @Test
    fun anyFitnessPermissionsAllowed_whenFitnessNotAllowed_returnsFalse() {
        val wearAppData =
            WearHealthAppData(
                packageName = "package.name",
                appMetadata = appMetadataOne,
                healthPermissionStatus =
                    listOf(
                        DENIED_READ_HEART_RATE_PERMISSION,
                        DENIED_READ_SKIN_TEMPERATURE_PERMISSION,
                        DENIED_READ_OXYGEN_SATURATION_PERMISSION,
                        GRANTED_READ_HEALTH_DATA_IN_BACKGROUND_PERMISSION,
                    ),
            )

        assertThat(wearAppData.anyFitnessPermissionsAllowed()).isFalse()
    }

    @Test
    fun isPermissionRequested_whenRequested_returnsTrue() {
        val wearAppData =
            WearHealthAppData(
                packageName = "package.name",
                appMetadata = appMetadataOne,
                healthPermissionStatus =
                    listOf(
                        DENIED_READ_HEART_RATE_PERMISSION,
                        GRANTED_READ_HEALTH_DATA_IN_BACKGROUND_PERMISSION,
                    ),
            )

        assertThat(wearAppData.isPermissionRequested(READ_HEART_RATE_PERMISSION)).isTrue()
        assertThat(wearAppData.isPermissionRequested(READ_HEALTH_DATA_IN_BACKGROUND)).isTrue()
    }

    @Test
    fun isPermissionRequested_whenNotRequested_returnsFalse() {
        val wearAppData =
            WearHealthAppData(
                packageName = "package.name",
                appMetadata = appMetadataOne,
                healthPermissionStatus =
                    listOf(
                        DENIED_READ_HEART_RATE_PERMISSION,
                        GRANTED_READ_SKIN_TEMPERATURE_PERMISSION,
                    ),
            )

        assertThat(wearAppData.isPermissionRequested(READ_OXYGEN_SATURATION_PERMISSION)).isFalse()
        assertThat(wearAppData.isPermissionRequested(READ_HEALTH_DATA_IN_BACKGROUND)).isFalse()
    }

    @Test
    fun isPermissionAllowed_whenAllowed_returnsTrue() {
        val wearAppData =
            WearHealthAppData(
                packageName = "package.name",
                appMetadata = appMetadataOne,
                healthPermissionStatus =
                    listOf(
                        DENIED_READ_HEART_RATE_PERMISSION,
                        GRANTED_READ_SKIN_TEMPERATURE_PERMISSION,
                        DENIED_READ_OXYGEN_SATURATION_PERMISSION,
                        GRANTED_READ_HEALTH_DATA_IN_BACKGROUND_PERMISSION,
                    ),
            )

        assertThat(wearAppData.isPermissionAllowed(READ_SKIN_TEMPERATURE_PERMISSION)).isTrue()
        assertThat(wearAppData.isPermissionAllowed(READ_HEALTH_DATA_IN_BACKGROUND)).isTrue()
    }

    @Test
    fun isPermissionAllowed_whenNotAllowed_returnsFalse() {
        val wearAppData =
            WearHealthAppData(
                packageName = "package.name",
                appMetadata = appMetadataOne,
                healthPermissionStatus =
                    listOf(
                        DENIED_READ_HEART_RATE_PERMISSION,
                        GRANTED_READ_SKIN_TEMPERATURE_PERMISSION,
                        GRANTED_READ_OXYGEN_SATURATION_PERMISSION,
                        DENIED_READ_HEALTH_DATA_IN_BACKGROUND_PERMISSION,
                    ),
            )

        assertThat(wearAppData.isPermissionAllowed(READ_HEART_RATE_PERMISSION)).isFalse()
        assertThat(wearAppData.isPermissionAllowed(READ_HEALTH_DATA_IN_BACKGROUND)).isFalse()
    }

    @Test
    fun anyDataTypesAllowed_whenNoFitnessPermissions_returnsFalse() {
        val wearAppData =
            WearHealthAppData(
                packageName = "package.name",
                appMetadata = appMetadataOne,
                healthPermissionStatus = emptyList(),
            )

        assertThat(wearAppData.anyFitnessPermissionsAllowed()).isFalse()
    }

    @Test
    fun anyFitnessPermissionsAllowed_whenNoneAllowed_returnsFalse() {
        val wearAppData =
            WearHealthAppData(
                packageName = "package.name",
                appMetadata = appMetadataOne,
                healthPermissionStatus =
                    listOf(
                        DENIED_READ_HEART_RATE_PERMISSION,
                        DENIED_READ_SKIN_TEMPERATURE_PERMISSION,
                        DENIED_READ_OXYGEN_SATURATION_PERMISSION,
                        DENIED_READ_HEALTH_DATA_IN_BACKGROUND_PERMISSION,
                    ),
            )
        assertThat(wearAppData.anyFitnessPermissionsAllowed()).isFalse()
    }

    @Test
    fun anyFitnessPermissionsAllowed_whenOneAllowed_returnsTrue() {
        val wearAppData =
            WearHealthAppData(
                packageName = "package.name",
                appMetadata = appMetadataOne,
                healthPermissionStatus =
                    listOf(
                        DENIED_READ_HEART_RATE_PERMISSION,
                        GRANTED_READ_SKIN_TEMPERATURE_PERMISSION,
                        DENIED_READ_OXYGEN_SATURATION_PERMISSION,
                        GRANTED_READ_HEALTH_DATA_IN_BACKGROUND_PERMISSION,
                    ),
            )
        assertThat(wearAppData.anyFitnessPermissionsAllowed()).isTrue()
    }

    @Test
    fun anyFitnessPermissionsAllowed_whenOnlyBackgroundAllowed_returnsFalse() {
        val wearAppData =
            WearHealthAppData(
                packageName = "package.name",
                appMetadata = appMetadataOne,
                healthPermissionStatus =
                    listOf(
                        DENIED_READ_HEART_RATE_PERMISSION,
                        DENIED_READ_SKIN_TEMPERATURE_PERMISSION,
                        DENIED_READ_OXYGEN_SATURATION_PERMISSION,
                        GRANTED_READ_HEALTH_DATA_IN_BACKGROUND_PERMISSION,
                    ),
            )
        assertThat(wearAppData.anyFitnessPermissionsAllowed()).isFalse()
    }

    @Test
    fun isBackgroundPermissionRequested_whenRequested_returnsTrue() {
        val wearAppData =
            WearHealthAppData(
                packageName = "package.name",
                appMetadata = appMetadataOne,
                healthPermissionStatus =
                    listOf(
                        DENIED_READ_HEART_RATE_PERMISSION,
                        DENIED_READ_SKIN_TEMPERATURE_PERMISSION,
                        DENIED_READ_OXYGEN_SATURATION_PERMISSION,
                        GRANTED_READ_HEALTH_DATA_IN_BACKGROUND_PERMISSION,
                    ),
            )
        assertThat(wearAppData.isBackgroundPermissionRequested()).isTrue()
    }

    @Test
    fun isBackgroundPermissionRequested_whenNotRequested_returnsFalse() {
        val wearAppData =
            WearHealthAppData(
                packageName = "package.name",
                appMetadata = appMetadataOne,
                healthPermissionStatus =
                    listOf(
                        DENIED_READ_HEART_RATE_PERMISSION,
                        DENIED_READ_SKIN_TEMPERATURE_PERMISSION,
                    ),
            )
        assertThat(wearAppData.isBackgroundPermissionRequested()).isFalse()
    }

    @Test
    fun isBackgroundPermissionGranted_whenGranted_returnsTrue() {
        val wearAppData =
            WearHealthAppData(
                packageName = "package.name",
                appMetadata = appMetadataOne,
                healthPermissionStatus =
                    listOf(
                        GRANTED_READ_HEART_RATE_PERMISSION,
                        DENIED_READ_SKIN_TEMPERATURE_PERMISSION,
                        GRANTED_READ_HEALTH_DATA_IN_BACKGROUND_PERMISSION,
                    ),
            )
        assertThat(wearAppData.isBackgroundPermissionGranted()).isTrue()
    }

    @Test
    fun isBackgroundPermissionGranted_whenNotGranted_returnsFalse() {
        val wearAppData =
            WearHealthAppData(
                packageName = "package.name",
                appMetadata = appMetadataOne,
                healthPermissionStatus =
                    listOf(
                        GRANTED_READ_HEART_RATE_PERMISSION,
                        DENIED_READ_SKIN_TEMPERATURE_PERMISSION,
                        DENIED_READ_HEALTH_DATA_IN_BACKGROUND_PERMISSION,
                    ),
            )
        assertThat(wearAppData.isBackgroundPermissionGranted()).isFalse()
    }

    @Test
    fun anyFitnessPermissionUsed_whenAtLeastOneAccessLog_returnsTrue() {
        val wearAppData =
            WearHealthAppData(
                packageName = "package.name",
                appMetadata = appMetadataOne,
                healthPermissionStatus =
                    listOf(
                        GRANTED_READ_HEART_RATE_PERMISSION,
                        DENIED_READ_SKIN_TEMPERATURE_PERMISSION,
                        DENIED_READ_HEALTH_DATA_IN_BACKGROUND_PERMISSION,
                    ),
                lastAccessTime = NOW,
                accessLogs = listOf(PermissionsLastAccess(FitnessPermissionType.HEART_RATE, NOW)),
            )
        assertThat(wearAppData.anyFitnessPermissionUsed()).isTrue()
    }

    @Test
    fun anyFitnessPermissionUsed_whenNoAccessLog_returnsFalse() {
        val wearAppData =
            WearHealthAppData(
                packageName = "package.name",
                appMetadata = appMetadataOne,
                healthPermissionStatus =
                    listOf(
                        GRANTED_READ_HEART_RATE_PERMISSION,
                        DENIED_READ_SKIN_TEMPERATURE_PERMISSION,
                        DENIED_READ_HEALTH_DATA_IN_BACKGROUND_PERMISSION,
                    ),
                lastAccessTime = NOW,
                accessLogs = listOf(),
            )
        assertThat(wearAppData.anyFitnessPermissionUsed()).isFalse()
    }

    @Test
    fun wasFitnessPermissionUsed_whenAtLeastOneAccessLog_returnsTrue() {
        val wearAppData =
            WearHealthAppData(
                packageName = "package.name",
                appMetadata = appMetadataOne,
                healthPermissionStatus =
                    listOf(
                        GRANTED_READ_HEART_RATE_PERMISSION,
                        DENIED_READ_SKIN_TEMPERATURE_PERMISSION,
                        DENIED_READ_HEALTH_DATA_IN_BACKGROUND_PERMISSION,
                    ),
                lastAccessTime = NOW,
                accessLogs =
                    listOf(
                        PermissionsLastAccess(FitnessPermissionType.HEART_RATE, NOW),
                        PermissionsLastAccess(
                            FitnessPermissionType.HEART_RATE,
                            NOW.plusMillis(1000),
                        ),
                    ),
            )
        assertThat(wearAppData.wasFitnessPermissionUsed(READ_HEART_RATE_PERMISSION)).isTrue()
    }

    @Test
    fun wasFitnessPermissionUsed_whenNoAccessLog_returnsFalse() {
        val wearAppData =
            WearHealthAppData(
                packageName = "package.name",
                appMetadata = appMetadataOne,
                healthPermissionStatus =
                    listOf(
                        GRANTED_READ_HEART_RATE_PERMISSION,
                        DENIED_READ_SKIN_TEMPERATURE_PERMISSION,
                        DENIED_READ_HEALTH_DATA_IN_BACKGROUND_PERMISSION,
                    ),
                lastAccessTime = NOW,
                accessLogs =
                    listOf(
                        PermissionsLastAccess(FitnessPermissionType.HEART_RATE, NOW),
                        PermissionsLastAccess(
                            FitnessPermissionType.OXYGEN_SATURATION,
                            NOW.plusMillis(1000),
                        ),
                    ),
            )
        assertThat(wearAppData.wasFitnessPermissionUsed(READ_SKIN_TEMPERATURE_PERMISSION)).isFalse()
    }

    @Test
    fun shouldRevokeBackgroundReadAlongWith_whenLastReadPermission_returnsTrue() {
        val wearAppData =
            WearHealthAppData(
                packageName = "package.name",
                appMetadata = appMetadataOne,
                healthPermissionStatus =
                    listOf(
                        GRANTED_READ_HEART_RATE_PERMISSION,
                        DENIED_READ_SKIN_TEMPERATURE_PERMISSION,
                        GRANTED_READ_HEALTH_DATA_IN_BACKGROUND_PERMISSION,
                    ),
            )

        assertThat(wearAppData.shouldRevokeBackgroundReadAlongWith(READ_HEART_RATE_PERMISSION))
            .isTrue()
    }

    @Test
    fun shouldRevokeBackgroundReadAlongWith_whenNotLastReadPermission_returnsFalse() {
        val wearAppData =
            WearHealthAppData(
                packageName = "package.name",
                appMetadata = appMetadataOne,
                healthPermissionStatus =
                    listOf(
                        GRANTED_READ_HEART_RATE_PERMISSION,
                        GRANTED_READ_SKIN_TEMPERATURE_PERMISSION,
                        GRANTED_READ_HEALTH_DATA_IN_BACKGROUND_PERMISSION,
                    ),
            )

        assertThat(wearAppData.shouldRevokeBackgroundReadAlongWith(READ_HEART_RATE_PERMISSION))
            .isFalse()
    }

    @Test
    fun shouldRevokeBackgroundReadAlongWith_whenBackgroundNotGranted_returnsFalse() {
        val wearAppData =
            WearHealthAppData(
                packageName = "package.name",
                appMetadata = appMetadataOne,
                healthPermissionStatus =
                    listOf(
                        GRANTED_READ_HEART_RATE_PERMISSION,
                        GRANTED_READ_SKIN_TEMPERATURE_PERMISSION,
                        DENIED_READ_HEALTH_DATA_IN_BACKGROUND_PERMISSION,
                    ),
            )

        assertThat(wearAppData.shouldRevokeBackgroundReadAlongWith(READ_HEART_RATE_PERMISSION))
            .isFalse()
    }

    @Test
    fun shouldRevokeBackgroundReadAlongWith_whenBackgroundNotDeclared_returnsFalse() {
        val wearAppData =
            WearHealthAppData(
                packageName = "package.name",
                appMetadata = appMetadataOne,
                healthPermissionStatus =
                    listOf(
                        GRANTED_READ_HEART_RATE_PERMISSION,
                        GRANTED_READ_SKIN_TEMPERATURE_PERMISSION,
                    ),
            )

        assertThat(wearAppData.shouldRevokeBackgroundReadAlongWith(READ_HEART_RATE_PERMISSION))
            .isFalse()
    }

    @Test
    fun shouldRevokeBackgroundReadAlongWith_nonFitnessPermission_returnsFalse() {
        val wearAppData =
            WearHealthAppData(
                packageName = "package.name",
                appMetadata = appMetadataOne,
                healthPermissionStatus =
                    listOf(
                        DENIED_READ_HEART_RATE_PERMISSION,
                        GRANTED_READ_SKIN_TEMPERATURE_PERMISSION,
                        GRANTED_READ_HEALTH_DATA_IN_BACKGROUND_PERMISSION,
                    ),
            )

        assertThat(wearAppData.shouldRevokeBackgroundReadAlongWith(READ_HEALTH_DATA_IN_BACKGROUND))
            .isFalse()
    }

    @Test
    fun getNumberOfAllowedAppsForFitnessPermission_excludesSystemApps_returnsCorrectCount() {
        val wearAppDataOne =
            WearHealthAppData(
                packageName = "package.name",
                appMetadata = appMetadataOne,
                healthPermissionStatus =
                    listOf(
                        DENIED_READ_HEART_RATE_PERMISSION,
                        GRANTED_READ_SKIN_TEMPERATURE_PERMISSION,
                        GRANTED_READ_HEALTH_DATA_IN_BACKGROUND_PERMISSION,
                    ),
            )
        val wearAppDataTwo =
            WearHealthAppData(
                packageName = "package.name",
                appMetadata = appMetadataTwo,
                healthPermissionStatus =
                    listOf(
                        DENIED_READ_HEART_RATE_PERMISSION,
                        GRANTED_READ_OXYGEN_SATURATION_PERMISSION,
                    ),
            )
        val wearAppDataThree =
            WearHealthAppData(
                packageName = "package.name",
                appMetadata = appMetadataThree,
                healthPermissionStatus =
                    listOf(
                        GRANTED_READ_OXYGEN_SATURATION_PERMISSION,
                        GRANTED_READ_HEALTH_DATA_IN_BACKGROUND_PERMISSION,
                    ),
            )
        val systemWearAppData =
            WearHealthAppData(
                packageName = "package.name",
                appMetadata = systemAppMetadataOne,
                healthPermissionStatus =
                    listOf(
                        GRANTED_READ_HEART_RATE_PERMISSION,
                        GRANTED_READ_SKIN_TEMPERATURE_PERMISSION,
                    ),
            )

        val wearAppDataList =
            listOf(wearAppDataOne, wearAppDataTwo, wearAppDataThree, systemWearAppData)
        assertThat(
                wearAppDataList.getNumberOfAllowedAppsForFitnessPermission(
                    READ_HEART_RATE_PERMISSION
                )
            )
            .isEqualTo(0)
        assertThat(
                wearAppDataList.getNumberOfAllowedAppsForFitnessPermission(
                    READ_SKIN_TEMPERATURE_PERMISSION
                )
            )
            .isEqualTo(1)
        assertThat(
                wearAppDataList.getNumberOfAllowedAppsForFitnessPermission(
                    READ_OXYGEN_SATURATION_PERMISSION
                )
            )
            .isEqualTo(2)
    }

    @Test
    fun getNumberOfDisallowedAppsForDataType_excludesSystemApps_returnsCorrectCount() {
        val wearAppDataOne =
            WearHealthAppData(
                packageName = "package.name",
                appMetadata = appMetadataOne,
                healthPermissionStatus =
                    listOf(
                        DENIED_READ_HEART_RATE_PERMISSION,
                        GRANTED_READ_SKIN_TEMPERATURE_PERMISSION,
                        GRANTED_READ_HEALTH_DATA_IN_BACKGROUND_PERMISSION,
                    ),
            )
        val wearAppDataTwo =
            WearHealthAppData(
                packageName = "package.name",
                appMetadata = appMetadataTwo,
                healthPermissionStatus =
                    listOf(
                        DENIED_READ_HEART_RATE_PERMISSION,
                        GRANTED_READ_OXYGEN_SATURATION_PERMISSION,
                    ),
            )
        val wearAppDataThree =
            WearHealthAppData(
                packageName = "package.name",
                appMetadata = appMetadataThree,
                healthPermissionStatus =
                    listOf(
                        GRANTED_READ_OXYGEN_SATURATION_PERMISSION,
                        GRANTED_READ_HEALTH_DATA_IN_BACKGROUND_PERMISSION,
                    ),
            )
        val systemWearAppData =
            WearHealthAppData(
                packageName = "package.name",
                appMetadata = systemAppMetadataOne,
                healthPermissionStatus =
                    listOf(
                        GRANTED_READ_HEART_RATE_PERMISSION,
                        GRANTED_READ_SKIN_TEMPERATURE_PERMISSION,
                    ),
            )

        val wearAppDataList =
            listOf(wearAppDataOne, wearAppDataTwo, wearAppDataThree, systemWearAppData)
        assertThat(
                wearAppDataList.getNumberOfDeniedAppsForFitnessPermission(
                    READ_HEART_RATE_PERMISSION
                )
            )
            .isEqualTo(2)
        assertThat(
                wearAppDataList.getNumberOfDeniedAppsForFitnessPermission(
                    READ_SKIN_TEMPERATURE_PERMISSION
                )
            )
            .isEqualTo(0)
        assertThat(
                wearAppDataList.getNumberOfDeniedAppsForFitnessPermission(
                    READ_OXYGEN_SATURATION_PERMISSION
                )
            )
            .isEqualTo(0)
    }

    @Test
    fun getAllowedApps_whenIncludeSystem_returnsAllAllowedApps() {
        val wearAppDataOne =
            WearHealthAppData(
                packageName = "package.name",
                appMetadata = appMetadataOne,
                healthPermissionStatus =
                    listOf(
                        DENIED_READ_HEART_RATE_PERMISSION,
                        GRANTED_READ_SKIN_TEMPERATURE_PERMISSION,
                        GRANTED_READ_HEALTH_DATA_IN_BACKGROUND_PERMISSION,
                    ),
            )
        val wearAppDataTwo =
            WearHealthAppData(
                packageName = "package.name",
                appMetadata = appMetadataTwo,
                healthPermissionStatus =
                    listOf(
                        DENIED_READ_HEART_RATE_PERMISSION,
                        GRANTED_READ_OXYGEN_SATURATION_PERMISSION,
                    ),
            )
        val wearAppDataThree =
            WearHealthAppData(
                packageName = "package.name",
                appMetadata = appMetadataThree,
                healthPermissionStatus =
                    listOf(
                        DENIED_READ_OXYGEN_SATURATION_PERMISSION,
                        DENIED_READ_HEALTH_DATA_IN_BACKGROUND_PERMISSION,
                    ),
            )
        val systemWearAppData =
            WearHealthAppData(
                packageName = "package.name",
                appMetadata = systemAppMetadataOne,
                healthPermissionStatus =
                    listOf(
                        GRANTED_READ_HEART_RATE_PERMISSION,
                        GRANTED_READ_SKIN_TEMPERATURE_PERMISSION,
                    ),
            )

        val wearAppDataList =
            listOf(wearAppDataOne, wearAppDataTwo, wearAppDataThree, systemWearAppData)
        assertThat(wearAppDataList.getAllowedApps(READ_HEART_RATE_PERMISSION, includeSystem = true))
            .containsExactly(systemWearAppData)
        assertThat(
                wearAppDataList.getAllowedApps(
                    READ_SKIN_TEMPERATURE_PERMISSION,
                    includeSystem = true,
                )
            )
            .containsExactly(wearAppDataOne, systemWearAppData)
        assertThat(
                wearAppDataList.getAllowedApps(
                    READ_OXYGEN_SATURATION_PERMISSION,
                    includeSystem = true,
                )
            )
            .containsExactly(wearAppDataTwo)
    }

    @Test
    fun getAllowedApps_whenNotIncludingSystem_returnsNonSystemAllowedApps() {
        val wearAppDataOne =
            WearHealthAppData(
                packageName = "package.name",
                appMetadata = appMetadataOne,
                healthPermissionStatus =
                    listOf(
                        DENIED_READ_HEART_RATE_PERMISSION,
                        GRANTED_READ_SKIN_TEMPERATURE_PERMISSION,
                        GRANTED_READ_HEALTH_DATA_IN_BACKGROUND_PERMISSION,
                    ),
            )
        val wearAppDataTwo =
            WearHealthAppData(
                packageName = "package.name",
                appMetadata = appMetadataTwo,
                healthPermissionStatus =
                    listOf(
                        DENIED_READ_HEART_RATE_PERMISSION,
                        GRANTED_READ_OXYGEN_SATURATION_PERMISSION,
                    ),
            )
        val wearAppDataThree =
            WearHealthAppData(
                packageName = "package.name",
                appMetadata = appMetadataThree,
                healthPermissionStatus =
                    listOf(
                        DENIED_READ_OXYGEN_SATURATION_PERMISSION,
                        DENIED_READ_HEALTH_DATA_IN_BACKGROUND_PERMISSION,
                    ),
            )
        val systemWearAppData =
            WearHealthAppData(
                packageName = "package.name",
                appMetadata = systemAppMetadataOne,
                healthPermissionStatus =
                    listOf(
                        GRANTED_READ_HEART_RATE_PERMISSION,
                        GRANTED_READ_SKIN_TEMPERATURE_PERMISSION,
                    ),
            )

        val wearAppDataList =
            listOf(wearAppDataOne, wearAppDataTwo, wearAppDataThree, systemWearAppData)
        assertThat(
                wearAppDataList.getAllowedApps(READ_HEART_RATE_PERMISSION, includeSystem = false)
            )
            .isEmpty()
        assertThat(
                wearAppDataList.getAllowedApps(
                    READ_SKIN_TEMPERATURE_PERMISSION,
                    includeSystem = false,
                )
            )
            .containsExactly(wearAppDataOne)
        assertThat(
                wearAppDataList.getAllowedApps(
                    READ_OXYGEN_SATURATION_PERMISSION,
                    includeSystem = false,
                )
            )
            .containsExactly(wearAppDataTwo)
    }

    @Test
    fun getDeniedApps_whenIncludeSystem_returnsAllDeniedApps() {
        val wearAppDataOne =
            WearHealthAppData(
                packageName = "package.name",
                appMetadata = appMetadataOne,
                healthPermissionStatus =
                    listOf(
                        DENIED_READ_HEART_RATE_PERMISSION,
                        GRANTED_READ_SKIN_TEMPERATURE_PERMISSION,
                        GRANTED_READ_HEALTH_DATA_IN_BACKGROUND_PERMISSION,
                    ),
            )
        val wearAppDataTwo =
            WearHealthAppData(
                packageName = "package.name",
                appMetadata = appMetadataTwo,
                healthPermissionStatus =
                    listOf(
                        DENIED_READ_HEART_RATE_PERMISSION,
                        GRANTED_READ_OXYGEN_SATURATION_PERMISSION,
                    ),
            )
        val wearAppDataThree =
            WearHealthAppData(
                packageName = "package.name",
                appMetadata = appMetadataThree,
                healthPermissionStatus =
                    listOf(
                        DENIED_READ_OXYGEN_SATURATION_PERMISSION,
                        DENIED_READ_HEALTH_DATA_IN_BACKGROUND_PERMISSION,
                    ),
            )
        val systemWearAppData =
            WearHealthAppData(
                packageName = "package.name",
                appMetadata = systemAppMetadataOne,
                healthPermissionStatus =
                    listOf(
                        GRANTED_READ_HEART_RATE_PERMISSION,
                        GRANTED_READ_SKIN_TEMPERATURE_PERMISSION,
                        DENIED_READ_OXYGEN_SATURATION_PERMISSION,
                    ),
            )

        val wearAppDataList =
            listOf(wearAppDataOne, wearAppDataTwo, wearAppDataThree, systemWearAppData)
        assertThat(wearAppDataList.getDeniedApps(READ_HEART_RATE_PERMISSION, includeSystem = true))
            .containsExactly(wearAppDataOne, wearAppDataTwo)
        assertThat(
                wearAppDataList.getDeniedApps(
                    READ_SKIN_TEMPERATURE_PERMISSION,
                    includeSystem = true,
                )
            )
            .isEmpty()
        assertThat(
                wearAppDataList.getDeniedApps(
                    READ_OXYGEN_SATURATION_PERMISSION,
                    includeSystem = true,
                )
            )
            .containsExactly(wearAppDataThree, systemWearAppData)
    }

    @Test
    fun getNumberOfUsedAppsForDataType_excludesSystemApps_returnsCorrectCount() {
        val wearAppDataOne =
            WearHealthAppData(
                packageName = "package.name",
                appMetadata = appMetadataOne,
                healthPermissionStatus =
                    listOf(
                        DENIED_READ_HEART_RATE_PERMISSION,
                        GRANTED_READ_SKIN_TEMPERATURE_PERMISSION,
                        GRANTED_READ_HEALTH_DATA_IN_BACKGROUND_PERMISSION,
                    ),
                lastAccessTime = NOW,
                accessLogs =
                    listOf(
                        PermissionsLastAccess(FitnessPermissionType.HEART_RATE, NOW),
                        PermissionsLastAccess(
                            FitnessPermissionType.SKIN_TEMPERATURE,
                            NOW.plusMillis(1000),
                        ),
                    ),
            )
        val wearAppDataTwo =
            WearHealthAppData(
                packageName = "package.name",
                appMetadata = appMetadataTwo,
                healthPermissionStatus =
                    listOf(
                        DENIED_READ_HEART_RATE_PERMISSION,
                        GRANTED_READ_OXYGEN_SATURATION_PERMISSION,
                    ),
                lastAccessTime = NOW,
                accessLogs =
                    listOf(
                        PermissionsLastAccess(
                            FitnessPermissionType.SKIN_TEMPERATURE,
                            NOW.plusMillis(1000),
                        )
                    ),
            )
        val wearAppDataThree =
            WearHealthAppData(
                packageName = "package.name",
                appMetadata = appMetadataThree,
                healthPermissionStatus =
                    listOf(
                        DENIED_READ_OXYGEN_SATURATION_PERMISSION,
                        DENIED_READ_HEALTH_DATA_IN_BACKGROUND_PERMISSION,
                    ),
                lastAccessTime = NOW,
                accessLogs =
                    listOf(
                        PermissionsLastAccess(FitnessPermissionType.OXYGEN_SATURATION, NOW),
                        PermissionsLastAccess(
                            FitnessPermissionType.OXYGEN_SATURATION,
                            NOW.plusMillis(1000),
                        ),
                    ),
            )
        val systemWearAppData =
            WearHealthAppData(
                packageName = "package.name",
                appMetadata = systemAppMetadataOne,
                healthPermissionStatus =
                    listOf(
                        GRANTED_READ_HEART_RATE_PERMISSION,
                        GRANTED_READ_SKIN_TEMPERATURE_PERMISSION,
                        DENIED_READ_OXYGEN_SATURATION_PERMISSION,
                    ),
                accessLogs =
                    listOf(
                        PermissionsLastAccess(FitnessPermissionType.OXYGEN_SATURATION, NOW),
                        PermissionsLastAccess(
                            FitnessPermissionType.HEART_RATE,
                            NOW.plusMillis(1000),
                        ),
                        PermissionsLastAccess(
                            FitnessPermissionType.SKIN_TEMPERATURE,
                            NOW.plusMillis(1000),
                        ),
                    ),
            )

        val wearAppDataList =
            listOf(wearAppDataOne, wearAppDataTwo, wearAppDataThree, systemWearAppData)
        assertThat(
                wearAppDataList.getNumberOfUsedAppsForFitnessPermission(READ_HEART_RATE_PERMISSION)
            )
            .isEqualTo(1)
        assertThat(
                wearAppDataList.getNumberOfUsedAppsForFitnessPermission(
                    READ_SKIN_TEMPERATURE_PERMISSION
                )
            )
            .isEqualTo(1)
        assertThat(
                wearAppDataList.getNumberOfUsedAppsForFitnessPermission(
                    READ_OXYGEN_SATURATION_PERMISSION
                )
            )
            .isEqualTo(1)
    }
}
