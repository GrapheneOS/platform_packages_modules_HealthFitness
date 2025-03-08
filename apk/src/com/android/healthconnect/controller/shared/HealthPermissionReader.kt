/**
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * ```
 *      http://www.apache.org/licenses/LICENSE-2.0
 * ```
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.shared

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.content.pm.PackageManager.PackageInfoFlags
import android.content.pm.PackageManager.ResolveInfoFlags
import android.health.connect.HealthConnectManager
import android.health.connect.HealthPermissions
import android.os.Process
import com.android.healthconnect.controller.permissions.api.GetHealthPermissionsFlagsUseCase
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.AdditionalPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.Companion.isAdditionalPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.Companion.isFitnessReadPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.Companion.isMedicalReadPermission
import com.android.healthconnect.controller.shared.app.AppPermissionsType
import com.android.healthfitness.flags.AconfigFlagHelper
import com.android.healthfitness.flags.AconfigFlagHelper.isPersonalHealthRecordEnabled
import com.android.healthfitness.flags.Flags
import com.google.common.annotations.VisibleForTesting
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Class that reads permissions declared by Health Connect clients as a string array in their XML
 * resources. See android.health.connect.HealthPermissions
 */
@Singleton
class HealthPermissionReader
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val getHealthPermissionsFlagsUseCase: GetHealthPermissionsFlagsUseCase,
) {

    companion object {
        private const val HEALTH_PERMISSION_GROUP = "android.permission-group.HEALTH"
        private const val RESOLVE_INFO_FLAG: Long = PackageManager.MATCH_ALL.toLong()
        private const val PACKAGE_INFO_PERMISSIONS_FLAG: Long =
            PackageManager.GET_PERMISSIONS.toLong()

        private val medicalPermissions =
            setOf(
                HealthPermissions.WRITE_MEDICAL_DATA,
                HealthPermissions.READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES,
                HealthPermissions.READ_MEDICAL_DATA_CONDITIONS,
                HealthPermissions.READ_MEDICAL_DATA_LABORATORY_RESULTS,
                HealthPermissions.READ_MEDICAL_DATA_MEDICATIONS,
                HealthPermissions.READ_MEDICAL_DATA_PERSONAL_DETAILS,
                HealthPermissions.READ_MEDICAL_DATA_PRACTITIONER_DETAILS,
                HealthPermissions.READ_MEDICAL_DATA_PREGNANCY,
                HealthPermissions.READ_MEDICAL_DATA_PROCEDURES,
                HealthPermissions.READ_MEDICAL_DATA_SOCIAL_HISTORY,
                HealthPermissions.READ_MEDICAL_DATA_VACCINES,
                HealthPermissions.READ_MEDICAL_DATA_VISITS,
                HealthPermissions.READ_MEDICAL_DATA_VITAL_SIGNS,
            )

        /**
         * Determines if an app's permission group is user-sensitive. If an app is not user
         * sensitive, then it is considered a system app, and hidden in the UI by default.
         *
         * This logic is copied from PermissionController/AppPermGroupUiInfoLiveData because we want
         * to achieve consistent numbers as showed in Settings->PermissionManager.
         *
         * @param permFlags the permission flags corresponding to the permissions requested by a
         *   given app
         * @param packageFlags flag of
         *   [android.R.styleable#AndroidManifestUsesPermission&lt;uses-permission&gt;] tag included
         *   under &lt;manifest&gt
         * @return Whether or not this package requests a user sensitive permission
         */
        private fun isUserSensitive(permFlags: Int?, packageFlags: Int?): Boolean {
            if (permFlags == null || packageFlags == null) {
                return true
            }
            val granted =
                packageFlags and PackageInfo.REQUESTED_PERMISSION_GRANTED != 0 &&
                    permFlags and PackageManager.FLAG_PERMISSION_REVOKED_COMPAT == 0
            return if (granted) {
                permFlags and PackageManager.FLAG_PERMISSION_USER_SENSITIVE_WHEN_GRANTED != 0
            } else {
                permFlags and PackageManager.FLAG_PERMISSION_USER_SENSITIVE_WHEN_DENIED != 0
            }
        }
    }

    /**
     * Returns a list of app packageNames that have declared at least one health permission
     * (additional or data type).
     */
    fun getAppsWithHealthPermissions(): List<String> {
        if (
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_WATCH) &&
                Flags.replaceBodySensorPermissionEnabled()
        ) {
            // On Wear, do not depend on intent filter, instead, query apps by requested permissions
            // and filter out system apps.
            return getPackagesRequestingSystemHealthPermissions()
        }
        return try {
            val healthApps = mutableListOf<String>()
            healthApps.addAll(
                appsWithDeclaredIntent().filter { getValidHealthPermissions(it).isNotEmpty() }
            )
            if (Flags.replaceBodySensorPermissionEnabled()) {
                healthApps.addAll(getPackagesRequestingSplitBodySensorPermissions())
            }
            healthApps.distinct()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Identifies apps that have system health permissions requested.
     *
     * This function queries all apps and search for non-system apps that have requested at least
     * one health permissions. This function does not rely on health rationale intent filter. The
     * processing time of this function will be longer than the intent filter approach.
     *
     * @return a list of app package names that have requested at least one health permission and
     *   that are not system apps
     */
    private fun getPackagesRequestingSystemHealthPermissions(): List<String> {
        val packages =
            context.packageManager.getInstalledPackagesAsUser(
                PackageManager.GET_PERMISSIONS,
                Process.myUserHandle().getIdentifier(),
            )
        val healthApps = mutableListOf<String>()
        val systemHealthPermissions = getSystemHealthPermissions()

        for (info in packages) {
            val packageName = info.packageName
            val requestedPermissions = info.requestedPermissions ?: continue

            // Create a subset of requestedPermissions, where only system health permissions are
            // included. This is because HealthConnect service enforceValidHealthPermissions before
            // getPermissionFlags, and we're only interested in system health permissions in
            // displaying wear UI.
            val requestedSystemHealthPermissions =
                requestedPermissions
                    .withIndex()
                    .filter { (_, permissionName) ->
                        systemHealthPermissions.contains(permissionName)
                    }
                    .associate { (index, permissionName) -> index to permissionName }
            if (requestedSystemHealthPermissions.isEmpty()) {
                continue
            }

            // Only display non-system apps who are considered user-sensitive for health permission
            // group. Use permission flags to determine whether an app is user-sensitive.
            // This is a HealthConnect service call to get permission flags.
            val allPermFlags =
                getHealthPermissionsFlagsUseCase(
                    packageName,
                    requestedSystemHealthPermissions.values.toList(),
                )
            if (
                requestedSystemHealthPermissions.any { (index, permissionName) ->
                    isUserSensitive(
                        allPermFlags[permissionName],
                        info.requestedPermissionsFlags?.getOrNull(index),
                    )
                }
            ) {
                healthApps.add(packageName)
            }
        }
        return healthApps
    }

    /**
     * Identifies apps that are requesting health permissions as a result of a split-permission
     * upgrade from their use of the legacy body sensors permission.
     *
     * This function queries all apps and search for non-system apps that have requested at least
     * one health permissions. This function does not rely on health rationale intent filter. The
     * processing time of this function will be longer than the intent filter approach.
     *
     * @return a list of apps that use health permissions as a results of a split-permission upgrade
     *   from the legacy body sensors permission.
     */
    private fun getPackagesRequestingSplitBodySensorPermissions(): List<String> {
        val packages =
            context.packageManager.getInstalledPackagesAsUser(
                PackageManager.GET_PERMISSIONS,
                Process.myUserHandle().getIdentifier(),
            )
        val healthPermissions = getHealthPermissions()
        val healthApps = mutableListOf<String>()

        for (info in packages) {
            val splitPermissionAppClassification =
                getSplitPermissionAppClassification(info, healthPermissions)
            if (
                splitPermissionAppClassification ==
                    SplitPermissionAppClassification.NOT_SPLIT_PERMISSION_APP
            ) {
                continue
            }
            // TODO: b/379937107 - For now, filter out the system apps.
            if (
                splitPermissionAppClassification ==
                    SplitPermissionAppClassification.SPLIT_PERMISSION_SYSTEM_APP
            ) {
                continue
            }

            healthApps.add(info.packageName)
        }
        return healthApps
    }

    /**
     * Returns whether the app is considered a "split-permission" app (i.e. an app that is only
     * using health permissions as a result of a split-permission auto-migration of the legacy
     * body-sensor permission).
     */
    public fun isBodySensorSplitPermissionApp(packageName: String): Boolean {
        if (!Flags.replaceBodySensorPermissionEnabled()) {
            return false
        }

        try {
            val appInfo =
                context.packageManager.getPackageInfo(
                    packageName,
                    PackageInfoFlags.of(PACKAGE_INFO_PERMISSIONS_FLAG),
                )
            val healthPermissions = getHealthPermissions()
            val splitPermissionAppClassification =
                getSplitPermissionAppClassification(appInfo, healthPermissions)
            return splitPermissionAppClassification !=
                SplitPermissionAppClassification.NOT_SPLIT_PERMISSION_APP
        } catch (e: NameNotFoundException) {
            return false
        }
    }

    enum class SplitPermissionAppClassification {
        NOT_SPLIT_PERMISSION_APP,
        SPLIT_PERMISSION_SYSTEM_APP,
        SPLIT_PERMISSION_NON_SYSTEM_APP,
    }

    /** Returns the split-permission classification of the app. */
    private fun getSplitPermissionAppClassification(
        info: PackageInfo,
        healthPermissions: List<String>,
    ): SplitPermissionAppClassification {
        val packageName = info.packageName
        val requestedPermissions =
            info.requestedPermissions
                ?: return SplitPermissionAppClassification.NOT_SPLIT_PERMISSION_APP
        val requestedHealthPermissions = requestedPermissions.filter { it in healthPermissions }

        val indexOfReadHr = requestedPermissions.indexOf(HealthPermissions.READ_HEART_RATE)
        // Split permission only applies to READ_HEART_RATE.
        if (indexOfReadHr < 0) {
            return SplitPermissionAppClassification.NOT_SPLIT_PERMISSION_APP
        }

        // If there are other health permissions (other than READ_HEALTH_DATA_IN_BACKGROUND)
        // don't consider this a pure split-permission request.
        if (requestedHealthPermissions.size > 2) {
            return SplitPermissionAppClassification.NOT_SPLIT_PERMISSION_APP
        }

        val indexOfReadBackground =
            requestedPermissions.indexOf(HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND)
        val declaresBackgroundPermission = indexOfReadBackground >= 0
        // If there are two health permissions declared, make sure the other is
        // READ_HEALTH_DATA_IN_BACKGROUND.
        if (requestedHealthPermissions.size == 2 && !declaresBackgroundPermission) {
            return SplitPermissionAppClassification.NOT_SPLIT_PERMISSION_APP
        }

        val permissionsToCheck =
            if (declaresBackgroundPermission) {
                listOf(
                    HealthPermissions.READ_HEART_RATE,
                    HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND,
                )
            } else {
                listOf(HealthPermissions.READ_HEART_RATE)
            }

        // Check the READ_HEART_RATE permission flag to see if it's a split-permission.
        val permissionToFlags = getHealthPermissionsFlagsUseCase(packageName, permissionsToCheck)

        if (declaresBackgroundPermission) {
            // READ_HEALTH_DATA_IN_BACKGROUND is not due to split-permission.
            if (
                permissionToFlags.get(HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND)?.let { flags
                    ->
                    (flags and PackageManager.FLAG_PERMISSION_REVOKE_WHEN_REQUESTED) == 0
                } ?: true
            ) {
                return SplitPermissionAppClassification.NOT_SPLIT_PERMISSION_APP
            }
        }

        // READ_HEART_RATE is not due to split-permission.
        if (
            permissionToFlags.get(HealthPermissions.READ_HEART_RATE)?.let { flags ->
                (flags and PackageManager.FLAG_PERMISSION_REVOKE_WHEN_REQUESTED) == 0
            } ?: true
        ) {
            return SplitPermissionAppClassification.NOT_SPLIT_PERMISSION_APP
        }

        // Filter out system apps.
        val backgroundPermissionUserSensitive =
            if (declaresBackgroundPermission) {
                isUserSensitive(
                    permissionToFlags[HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND],
                    info.requestedPermissionsFlags?.getOrNull(indexOfReadBackground),
                )
            } else {
                false
            }

        val heartRatePermissionUserSensitive =
            isUserSensitive(
                permissionToFlags[HealthPermissions.READ_HEART_RATE],
                info.requestedPermissionsFlags?.getOrNull(indexOfReadHr),
            )
        val isSystemApp = !heartRatePermissionUserSensitive && !backgroundPermissionUserSensitive

        // Made it through the gauntlet! This is a split-permission app.
        return if (isSystemApp) {
            SplitPermissionAppClassification.SPLIT_PERMISSION_SYSTEM_APP
        } else {
            SplitPermissionAppClassification.SPLIT_PERMISSION_NON_SYSTEM_APP
        }
    }

    fun getAppsWithFitnessPermissions(): List<String> {
        return try {
            // TODO: b/400346245 - Should we include the split permission apps?
            appsWithDeclaredIntent().filter {
                getValidHealthPermissions(it)
                    .filterIsInstance<HealthPermission.FitnessPermission>()
                    .isNotEmpty()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getAppsWithMedicalPermissions(): List<String> {
        return try {
            appsWithDeclaredIntent().filter {
                getValidHealthPermissions(it)
                    .filterIsInstance<HealthPermission.MedicalPermission>()
                    .isNotEmpty()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun appsWithDeclaredIntent(): List<String> {
        return context.packageManager
            .queryIntentActivities(getRationaleIntent(), ResolveInfoFlags.of(RESOLVE_INFO_FLAG))
            .map { it.activityInfo.packageName }
            .distinct()
    }

    /**
     * Identifies apps that have the old permissions declared - they need to update before
     * continuing to sync with Health Connect.
     */
    fun getAppsWithOldHealthPermissions(): List<String> {
        return try {
            val oldPermissionsRationale = "androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE"
            val oldPermissionsMetaDataKey = "health_permissions"
            val intent = Intent(oldPermissionsRationale)
            val resolveInfoList =
                context.packageManager
                    .queryIntentActivities(intent, PackageManager.GET_META_DATA)
                    .filter { resolveInfo -> resolveInfo.activityInfo != null }
                    .filter { resolveInfo -> resolveInfo.activityInfo.metaData != null }
                    .filter { resolveInfo ->
                        resolveInfo.activityInfo.metaData.getInt(oldPermissionsMetaDataKey) != -1
                    }

            resolveInfoList.map { it.activityInfo.packageName }.distinct()
        } catch (e: NameNotFoundException) {
            emptyList()
        }
    }

    /**
     * Returns a list of health permissions declared by an app that can be rendered in our UI. This
     * also filters out invalid additional permissions.
     */
    fun getValidHealthPermissions(packageName: String): List<HealthPermission> {
        return try {
            val permissions = getDeclaredHealthPermissions(packageName)
            val declaredPermissions =
                permissions.mapNotNull { permission -> parsePermission(permission) }
            if (isPersonalHealthRecordEnabled()) {
                maybeFilterOutAdditionalIfNotValid(declaredPermissions)
            } else {
                declaredPermissions
            }
        } catch (e: NameNotFoundException) {
            emptyList()
        }
    }

    /**
     * Filers out invalid additional permissions. READ_HEALTH_DATA_HISTORY is valid if at least one
     * FITNESS READ permission is declared. READ_HEALTH_DATA_IN_BACKGROUND is valid if at least one
     * HEALTH READ permission is declared.
     */
    @VisibleForTesting
    fun maybeFilterOutAdditionalIfNotValid(
        declaredPermissions: List<HealthPermission>
    ): List<HealthPermission> {
        val historyReadDeclared =
            declaredPermissions.filterIsInstance<AdditionalPermission>().any {
                it == AdditionalPermission.READ_HEALTH_DATA_HISTORY
            }
        val backgroundReadDeclared =
            declaredPermissions.filterIsInstance<AdditionalPermission>().any {
                it == AdditionalPermission.READ_HEALTH_DATA_IN_BACKGROUND
            }
        val atLeastOneFitnessReadDeclared = declaredPermissions.any { isFitnessReadPermission(it) }
        val atLeastOneMedicalReadDeclared = declaredPermissions.any { isMedicalReadPermission(it) }
        val atLeastOneHealthReadDeclared =
            atLeastOneFitnessReadDeclared || atLeastOneMedicalReadDeclared

        var result = declaredPermissions.toMutableList()
        if (historyReadDeclared && !atLeastOneFitnessReadDeclared) {
            result =
                result
                    .filterNot { it == AdditionalPermission.READ_HEALTH_DATA_HISTORY }
                    .toMutableList()
        }
        if (backgroundReadDeclared && !atLeastOneHealthReadDeclared) {
            result =
                result
                    .filterNot { it == AdditionalPermission.READ_HEALTH_DATA_IN_BACKGROUND }
                    .toMutableList()
        }
        return result.toList()
    }

    /** Returns a list of health permissions that are declared by an app. */
    fun getDeclaredHealthPermissions(packageName: String): List<String> {
        return try {
            val appInfo =
                context.packageManager.getPackageInfo(
                    packageName,
                    PackageInfoFlags.of(PACKAGE_INFO_PERMISSIONS_FLAG),
                )
            val healthPermissions = getHealthPermissions()
            appInfo.requestedPermissions?.filter { it in healthPermissions }.orEmpty()
        } catch (e: NameNotFoundException) {
            emptyList()
        }
    }

    fun getAppPermissionsType(packageName: String): AppPermissionsType {
        val permissions = getValidHealthPermissions(packageName)
        val hasAtLeastOneFitnessPermission =
            permissions.firstOrNull { it is HealthPermission.FitnessPermission } != null
        val hasAtLeastOneMedicalPermission =
            permissions.firstOrNull { it is HealthPermission.MedicalPermission } != null

        return if (hasAtLeastOneFitnessPermission && hasAtLeastOneMedicalPermission) {
            AppPermissionsType.COMBINED_PERMISSIONS
        } else if (hasAtLeastOneFitnessPermission) {
            AppPermissionsType.FITNESS_PERMISSIONS_ONLY
        } else if (hasAtLeastOneMedicalPermission) {
            AppPermissionsType.MEDICAL_PERMISSIONS_ONLY
        } else {
            // All Fitness, Medical and Combined screens handle the empty state so any of those can
            // be returned here.
            AppPermissionsType.FITNESS_PERMISSIONS_ONLY
        }
    }

    /**
     * When PHR flag is on, returns valid additional permissions that we can display in our UI. An
     * additional permission is valid if the correct read permissions are declared.
     *
     * When PHR flag is off, returns additional permissions that are declared.
     */
    fun getAdditionalPermissions(packageName: String): List<String> {
        return if (isPersonalHealthRecordEnabled()) {
            getValidHealthPermissions(packageName)
                .map { it.toString() }
                .filter { perm -> isAdditionalPermission(perm) && !shouldHidePermission(perm) }
        } else {
            getDeclaredHealthPermissions(packageName).filter { perm ->
                isAdditionalPermission(perm) && !shouldHidePermission(perm)
            }
        }
    }

    fun isRationaleIntentDeclared(packageName: String): Boolean {
        val intent = getRationaleIntent(packageName)
        val resolvedInfo =
            context.packageManager.queryIntentActivities(
                intent,
                ResolveInfoFlags.of(RESOLVE_INFO_FLAG),
            )
        return resolvedInfo.any { info -> info.activityInfo.packageName == packageName }
    }

    fun getApplicationRationaleIntent(packageName: String): Intent {
        val intent = getRationaleIntent(packageName)
        val resolvedInfo =
            context.packageManager.queryIntentActivities(
                intent,
                ResolveInfoFlags.of(RESOLVE_INFO_FLAG),
            )
        resolvedInfo.forEach { info -> intent.setClassName(packageName, info.activityInfo.name) }
        return intent
    }

    private fun parsePermission(permission: String): HealthPermission? {
        return try {
            HealthPermission.fromPermissionString(permission)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    /** Returns a list of all health permissions in the HEALTH permission group. */
    @VisibleForTesting
    fun getHealthPermissions(): List<String> {
        val permissions =
            context.packageManager.queryPermissionsByGroup(HEALTH_PERMISSION_GROUP, 0).map {
                permissionInfo ->
                permissionInfo.name
            }
        return permissions.filterNot { permission -> shouldHidePermission(permission) }
    }

    /** Returns a list of all system health permissions in the HEALTH permission group. */
    fun getSystemHealthPermissions(): List<String> {
        val permissions =
            context.packageManager
                .queryPermissionsByGroup(HEALTH_PERMISSION_GROUP, 0)
                .map { permissionInfo -> permissionInfo.name }
                .filter { permissionName ->
                    val appOp = AppOpsManager.permissionToOp(permissionName)
                    appOp != null && !appOp.equals(AppOpsManager.OPSTR_READ_WRITE_HEALTH_DATA)
                }
        return permissions
    }

    fun shouldHidePermission(permission: String): Boolean {
        return when (permission) {
            in medicalPermissions -> !isPersonalHealthRecordEnabled()
            HealthPermissions.READ_ACTIVITY_INTENSITY,
            HealthPermissions.WRITE_ACTIVITY_INTENSITY ->
                !AconfigFlagHelper.isActivityIntensityEnabled()
            else -> false
        }
    }

    private fun getRationaleIntent(packageName: String? = null): Intent {
        val intent =
            Intent(Intent.ACTION_VIEW_PERMISSION_USAGE).apply {
                addCategory(HealthConnectManager.CATEGORY_HEALTH_PERMISSIONS)
                if (packageName != null) {
                    setPackage(packageName)
                }
            }
        return intent
    }
}
