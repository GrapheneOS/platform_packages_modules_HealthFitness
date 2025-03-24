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

package com.android.server.healthconnect.permission;

import static android.Manifest.permission.INTERACT_ACROSS_USERS_FULL;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.health.connect.HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND;
import static android.health.connect.HealthPermissions.READ_HEART_RATE;

import android.annotation.Nullable;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.health.connect.HealthConnectManager;
import android.health.connect.HealthPermissions;
import android.health.connect.internal.datatypes.utils.HealthConnectMappings;
import android.os.Binder;
import android.os.Build;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.ArraySet;

import com.android.healthfitness.flags.Flags;
import com.android.server.healthconnect.storage.datatypehelpers.AppInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.HealthDataCategoryPriorityHelper;

import java.time.Instant;
import java.time.Period;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A handler for HealthConnect permission-related logic.
 *
 * @hide
 */
public final class HealthConnectPermissionHelper {
    private static final Period GRANT_TIME_TO_START_ACCESS_DATE_PERIOD = Period.ofDays(30);
    private static final String TAG = "HealthConnectPermissionHelper";
    private static final String UNKNOWN_REASON = "Unknown Reason";

    private static final int MASK_PERMISSION_FLAGS =
            PackageManager.FLAG_PERMISSION_USER_SET
                    | PackageManager.FLAG_PERMISSION_USER_FIXED
                    | PackageManager.FLAG_PERMISSION_AUTO_REVOKED;

    private final Context mContext;
    private final PackageManager mPackageManager;
    private final HealthPermissionIntentAppsTracker mPermissionIntentAppsTracker;
    private final FirstGrantTimeManager mFirstGrantTimeManager;
    private final HealthDataCategoryPriorityHelper mHealthDataCategoryPriorityHelper;
    private final AppInfoHelper mAppInfoHelper;
    private final HealthConnectMappings mHealthConnectMappings;

    /**
     * Constructs a {@link HealthConnectPermissionHelper}.
     *
     * @param context the service context.
     * @param packageManager a {@link PackageManager} instance.
     * @param permissionIntentTracker a {@link
     *     com.android.server.healthconnect.permission.HealthPermissionIntentAppsTracker} instance
     *     that tracks apps allowed to request health permissions.
     */
    public HealthConnectPermissionHelper(
            Context context,
            PackageManager packageManager,
            HealthPermissionIntentAppsTracker permissionIntentTracker,
            FirstGrantTimeManager firstGrantTimeManager,
            HealthDataCategoryPriorityHelper healthDataCategoryPriorityHelper,
            AppInfoHelper appInfoHelper,
            HealthConnectMappings healthConnectMappings) {
        mContext = context;
        mPackageManager = packageManager;
        mPermissionIntentAppsTracker = permissionIntentTracker;
        mFirstGrantTimeManager = firstGrantTimeManager;
        mHealthDataCategoryPriorityHelper = healthDataCategoryPriorityHelper;
        mAppInfoHelper = appInfoHelper;
        mHealthConnectMappings = healthConnectMappings;
    }

    /**
     * See {@link HealthConnectManager#grantHealthPermission}.
     *
     * <p>NOTE: Once permission grant is successful, the package name will also be appended to the
     * end of the priority list corresponding to {@code permissionName}'s health permission
     * category.
     */
    public void grantHealthPermission(String packageName, String permissionName, UserHandle user) {
        enforceManageHealthPermissions(/* message= */ "grantHealthPermission");
        enforceValidHealthPermission(permissionName);
        UserHandle checkedUser = UserHandle.of(handleIncomingUser(user.getIdentifier()));
        enforceValidPackage(packageName, checkedUser);
        enforceSupportPermissionsUsageIntent(packageName, checkedUser, permissionName);
        final long token = Binder.clearCallingIdentity();
        try {
            mPackageManager.grantRuntimePermission(packageName, permissionName, checkedUser);
            mPackageManager.updatePermissionFlags(
                    permissionName,
                    packageName,
                    MASK_PERMISSION_FLAGS,
                    PackageManager.FLAG_PERMISSION_USER_SET,
                    checkedUser);

            // If is split permission, automatically grant BODY_SENSORS or BACKGROUND.
            if ((permissionName.equals(READ_HEART_RATE)
                            || permissionName.equals(READ_HEALTH_DATA_IN_BACKGROUND))
                    && isAppRequestingPermissionWithOutdatedTargetSdk(
                            packageName,
                            user,
                            toLegacyPermission(permissionName),
                            Build.VERSION_CODES.BAKLAVA)) {
                grantRuntimePermissionAndUpdateFlags(
                        packageName,
                        user,
                        toLegacyPermission(permissionName),
                        PackageManager.FLAG_PERMISSION_USER_SET);
            }
            mAppInfoHelper.getOrInsertAppInfoId(packageName);
            addToPriorityListIfRequired(packageName, permissionName, user);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /** See {@link HealthConnectManager#revokeHealthPermission}. */
    public void revokeHealthPermission(
            String packageName, String permissionName, @Nullable String reason, UserHandle user) {
        enforceManageHealthPermissions(/* message= */ "revokeHealthPermission");
        enforceValidHealthPermission(permissionName);
        UserHandle checkedUser = UserHandle.of(handleIncomingUser(user.getIdentifier()));
        enforceValidPackage(packageName, checkedUser);
        final long token = Binder.clearCallingIdentity();
        try {
            // checkPermission doesn't have a variant that accepts user, get the packageManager for
            // the user.
            boolean isAlreadyDenied =
                    mContext.createContextAsUser(checkedUser, /* flags */ 0)
                                    .getPackageManager()
                                    .checkPermission(permissionName, packageName)
                            == PackageManager.PERMISSION_DENIED;
            int permissionFlags =
                    mPackageManager.getPermissionFlags(permissionName, packageName, checkedUser);
            if (!isAlreadyDenied) {
                revokeRuntimePermission(packageName, checkedUser, permissionName, reason);
            }
            if (isAlreadyDenied
                    && (permissionFlags & PackageManager.FLAG_PERMISSION_USER_SET) != 0) {
                permissionFlags = permissionFlags | PackageManager.FLAG_PERMISSION_USER_FIXED;
            } else {
                permissionFlags = permissionFlags | PackageManager.FLAG_PERMISSION_USER_SET;
            }
            permissionFlags = permissionFlags & ~PackageManager.FLAG_PERMISSION_AUTO_REVOKED;
            mPackageManager.updatePermissionFlags(
                    permissionName,
                    packageName,
                    MASK_PERMISSION_FLAGS,
                    permissionFlags,
                    checkedUser);
            // If is from split permission, automatically revoke BODY_SENSORS or BACKGROUND.
            if ((permissionName.equals(READ_HEART_RATE)
                            || permissionName.equals(READ_HEALTH_DATA_IN_BACKGROUND))
                    && isAppRequestingPermissionWithOutdatedTargetSdk(
                            packageName,
                            user,
                            toLegacyPermission(permissionName),
                            Build.VERSION_CODES.BAKLAVA)) {
                revokeRuntimePermissionAndUpdateFlags(
                        packageName,
                        user,
                        toLegacyPermission(permissionName),
                        permissionFlags,
                        reason);
            }

            removeFromPriorityListIfRequired(packageName, permissionName, user);

        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /**
     * See {@link HealthConnectManager#revokeAllHealthPermissions}.
     *
     * @return {@code true} if any health permissions were revoked, {@code false} otherwise
     */
    public boolean revokeAllHealthPermissions(
            String packageName, @Nullable String reason, UserHandle user) {
        enforceManageHealthPermissions(/* message= */ "revokeAllHealthPermissions");
        UserHandle checkedUser = UserHandle.of(handleIncomingUser(user.getIdentifier()));
        enforceValidPackage(packageName, checkedUser);
        final long token = Binder.clearCallingIdentity();
        try {
            return revokeAllHealthPermissionsUnchecked(packageName, checkedUser, reason);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /** See {@link HealthConnectManager#getGrantedHealthPermissions}. */
    public List<String> getGrantedHealthPermissions(String packageName, UserHandle user) {
        enforceManageHealthPermissions(/* message= */ "getGrantedHealthPermissions");
        UserHandle checkedUser = UserHandle.of(handleIncomingUser(user.getIdentifier()));
        enforceValidPackage(packageName, checkedUser);
        final long token = Binder.clearCallingIdentity();
        try {
            return PackageInfoUtils.getGrantedHealthPermissions(mContext, packageName, checkedUser);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /** See {@link HealthConnectManager#getHealthPermissionsFlags(String, List)}. */
    public Map<String, Integer> getHealthPermissionsFlags(
            String packageName, UserHandle user, List<String> permissions) {
        enforceManageHealthPermissions(/* message= */ "getHealthPermissionsFlags");
        UserHandle checkedUser = UserHandle.of(handleIncomingUser(user.getIdentifier()));
        enforceValidPackage(packageName, checkedUser);
        final long token = Binder.clearCallingIdentity();
        try {
            return getHealthPermissionsFlagsUnchecked(packageName, checkedUser, permissions);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /** See {@link HealthConnectManager#setHealthPermissionsUserFixedFlagValue(String, List)}. */
    public void setHealthPermissionsUserFixedFlagValue(
            String packageName, UserHandle user, List<String> permissions, boolean value) {
        enforceManageHealthPermissions(/* message= */ "setHealthPermissionsUserFixedFlagValue");
        UserHandle checkedUser = UserHandle.of(handleIncomingUser(user.getIdentifier()));
        enforceValidPackage(packageName, checkedUser);
        final long token = Binder.clearCallingIdentity();
        try {
            setHealthPermissionsUserFixedFlagValueUnchecked(
                    packageName, checkedUser, permissions, value);
        } finally {
            Binder.restoreCallingIdentity(token);
        }
    }

    /**
     * Returns {@code true} if there is at least one granted permission for the provided {@code
     * packageName}, {@code false} otherwise.
     */
    public boolean hasGrantedHealthPermissions(String packageName, UserHandle user) {
        return !getGrantedHealthPermissions(packageName, user).isEmpty();
    }

    /**
     * Returns the date from which an app can read / write health data. See {@link
     * HealthConnectManager#getHealthDataHistoricalAccessStartDate}
     */
    public Optional<Instant> getHealthDataStartDateAccess(String packageName, UserHandle user)
            throws IllegalArgumentException {
        enforceManageHealthPermissions(/* message= */ "getHealthDataStartDateAccess");
        UserHandle checkedUser = UserHandle.of(handleIncomingUser(user.getIdentifier()));
        enforceValidPackage(packageName, checkedUser);

        return mFirstGrantTimeManager
                .getFirstGrantTime(packageName, checkedUser)
                .map(grantTime -> grantTime.minus(GRANT_TIME_TO_START_ACCESS_DATE_PERIOD))
                .or(Optional::empty);
    }

    /**
     * Same as {@link #getHealthDataStartDateAccess(String, UserHandle)} except this method also
     * throws {@link IllegalAccessException} if health permission is in an incorrect state where
     * first grant time can't be fetched.
     */
    public Instant getHealthDataStartDateAccessOrThrow(String packageName, UserHandle user) {
        Optional<Instant> startDateAccess = getHealthDataStartDateAccess(packageName, user);
        if (startDateAccess.isEmpty()) {
            throwExceptionIncorrectPermissionState();
        }
        return startDateAccess.get();
    }

    /**
     * Returns whether the given package is explicitly requesting health permissions (i.e. not as a
     * result of a split permission platform migration).
     */
    private boolean isPackageExplicitlyRequestingHealthPermission(
            String packageName, UserHandle userHandle) {
        PackageInfo packageInfo;
        try {
            packageInfo =
                    PackageInfoUtils.getPackageInfoUnchecked(
                            packageName,
                            userHandle,
                            PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS),
                            mContext);
        } catch (IllegalArgumentException e) {
            // If the package can't be found, be conservative and assume they
            // are explicitly requesting a health permission.
            return true;
        }
        Set<String> requestedPermissions = new ArraySet<>(packageInfo.requestedPermissions);

        Set<String> healthPermissions = HealthConnectManager.getHealthPermissions(mContext);
        List<String> requestedHealthPermissions =
                requestedPermissions.stream()
                        .filter(
                                requestedPermission ->
                                        healthPermissions.contains(requestedPermission))
                        .collect(Collectors.toList());
        if (requestedHealthPermissions.isEmpty()) {
            return false;
        }

        if (!canPotentiallyBeSplitPermissions(requestedHealthPermissions)
                || packageInfo.applicationInfo == null) {
            return true;
        }
        // Check the permission flags to see if these permissions are requested
        // as a result of a split-permission due to a platform upgrade.
        Map<String, Integer> permissionFlags;
        try {
            permissionFlags =
                    getHealthPermissionsFlags(packageName, userHandle, requestedHealthPermissions);
        } catch (IllegalArgumentException e) {
            // If the package can't be found, assume it's asking for the health
            // permissions explicitly.
            return true;
        }

        // Permissions that aren't from split permission are explicitly
        // requested by the app.
        int targetSdkVersion = packageInfo.applicationInfo.targetSdkVersion;
        return requestedHealthPermissions.stream()
                .anyMatch(
                        requestedPermission ->
                                !isFromSplitPermission(
                                        permissionFlags.getOrDefault(requestedPermission, 0),
                                        targetSdkVersion));
    }

    /** Returns true if we should enforce permission usage intent for this package. */
    public boolean shouldEnforcePermissionUsageIntent(String packageName, UserHandle userHandle) {
        // When flag is disabled, always enforce permission usage intent.
        if (!Flags.replaceBodySensorPermissionEnabled()) {
            return true;
        }

        // The rationale intent is not currently required on Wear devices.
        if (mPackageManager.hasSystemFeature(PackageManager.FEATURE_WATCH)) {
            return false;
        }

        // We only need to enforce the rationale intent if the app is explicitly
        // requesting at least one health permission. If the app isn't
        // requesting any health permissions, or is only requesting them as a
        // result of a split permission platform migration, then we don't need
        // to enforce the rationale intent.
        return isPackageExplicitlyRequestingHealthPermission(packageName, userHandle);
    }

    /**
     * Returns true if we should enforce permission usage intent for the given package to be granted
     * the given permission.
     */
    private boolean shouldEnforcePermissionUsageIntent(
            String packageName, UserHandle userHandle, String permissionName) {
        // When flag is disabled, always enforce permission usage intent.
        if (!Flags.replaceBodySensorPermissionEnabled()) {
            return true;
        }

        // The rationale intent is not currently required on Wear devices.
        if (mPackageManager.hasSystemFeature(PackageManager.FEATURE_WATCH)) {
            return false;
        }

        // When flag is enabled, and is requesting split permission, do not enforce
        // permission usage intent on Phone.
        return !isRequestingSplitPermission(packageName, userHandle, permissionName);
    }

    /**
     * Returns true if {@code permissionFlag} indicates the permission is implicit from permission
     * split.
     */
    public static boolean isFromSplitPermission(int permissionFlag, int targetSdk) {
        return (targetSdk >= Build.VERSION_CODES.M)
                ? (permissionFlag & PackageManager.FLAG_PERMISSION_REVOKE_WHEN_REQUESTED) != 0
                : (permissionFlag & PackageManager.FLAG_PERMISSION_REVIEW_REQUIRED) != 0;
    }

    private static boolean canPotentiallyBeSplitPermissions(List<String> permissions) {
        return (permissions.size() == 1 && permissions.contains(HealthPermissions.READ_HEART_RATE))
                || (permissions.size() == 2
                        && permissions.contains(HealthPermissions.READ_HEART_RATE)
                        && permissions.contains(HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND));
    }

    /**
     * Returns true if the app is requesting the given permission as a result of a split-permission
     * platform migration.
     */
    private boolean isRequestingSplitPermission(
            String packageName, UserHandle userHandle, String permissionName) {
        // BODY_SENSORS split permission.
        if (!permissionName.equals(HealthPermissions.READ_HEART_RATE)
                && !permissionName.equals(HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND)) {
            return false;
        }

        PackageInfo packageInfo;
        try {
            packageInfo =
                    PackageInfoUtils.getPackageInfoUnchecked(
                            packageName,
                            userHandle,
                            PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS),
                            mContext);
        } catch (IllegalArgumentException e) {
            // If the package can't be found, default to consider as not containing split
            // permission.
            return false;
        }

        // BODY_SENSORS permission split only applies to apps targeting SDK < B.
        int targetSdkVersion = packageInfo.applicationInfo.targetSdkVersion;
        if (targetSdkVersion >= Build.VERSION_CODES.BAKLAVA) {
            return false;
        }

        // Check the permission flags to see if these permissions are requested
        // as a result of a split-permission due to a platform upgrade.
        List<String> permissionsToCheck = List.of(permissionName);
        Map<String, Integer> permissionFlags;
        try {
            permissionFlags =
                    getHealthPermissionsFlags(packageName, userHandle, permissionsToCheck);
        } catch (IllegalArgumentException e) {
            // If the package can't be found, default to consider as not containing split
            // permission.
            return false;
        }

        // Check if given permission is from a split-permission.
        int permissionFlag = permissionFlags.getOrDefault(permissionName, 0);
        return isFromSplitPermission(permissionFlag, targetSdkVersion);
    }

    /** Returns if the app is targeting SDK 35 and requesting the given permission. */
    private boolean isAppRequestingPermissionWithOutdatedTargetSdk(
            String packageName, UserHandle userHandle, String permission, int buildVersion) {
        if (!Flags.replaceBodySensorPermissionEnabled()) {
            return false;
        }

        // Only applies if current build is the same or newer than build version.
        if (Build.VERSION.SDK_INT < buildVersion) {
            return false;
        }

        PackageInfo packageInfo;
        try {
            packageInfo =
                    PackageInfoUtils.getPackageInfoUnchecked(
                            packageName,
                            userHandle,
                            PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS),
                            mContext);
        } catch (IllegalArgumentException e) {
            // If the package can't be found, default to consider as not containing split
            // permission.
            return false;
        }

        // If the app is targeting the given build version or newer, then they
        // are not using an outdated target SDK.
        if (packageInfo.applicationInfo.targetSdkVersion >= buildVersion) {
            return false;
        }

        Set<String> requestedPermissions = new ArraySet<>(packageInfo.requestedPermissions);
        return requestedPermissions.contains(permission);
    }

    private void throwExceptionIncorrectPermissionState() {
        throw new IllegalStateException(
                "Incorrect health permission state, likely"
                        + " because the calling application's manifest does not specify handling "
                        + Intent.ACTION_VIEW_PERMISSION_USAGE
                        + " with "
                        + HealthConnectManager.CATEGORY_HEALTH_PERMISSIONS);
    }

    private void addToPriorityListIfRequired(
            String packageName, String permissionName, UserHandle user) {
        if (mHealthConnectMappings.isWritePermission(permissionName)) {
            mHealthDataCategoryPriorityHelper.appendToPriorityList(
                    packageName,
                    mHealthConnectMappings.getHealthDataCategoryForWritePermission(permissionName),
                    user);
        }
    }

    private void removeFromPriorityListIfRequired(
            String packageName, String permissionName, UserHandle user) {
        if (mHealthConnectMappings.isWritePermission(permissionName)) {
            mHealthDataCategoryPriorityHelper.maybeRemoveAppFromPriorityList(
                    packageName,
                    mHealthConnectMappings.getHealthDataCategoryForWritePermission(permissionName),
                    user);
        }
    }

    private Map<String, Integer> getHealthPermissionsFlagsUnchecked(
            String packageName, UserHandle user, List<String> permissions) {
        enforceValidHealthPermissions(packageName, user, permissions);

        Map<String, Integer> result = new ArrayMap<>();

        for (String permission : permissions) {
            result.put(
                    permission, mPackageManager.getPermissionFlags(permission, packageName, user));
        }

        return result;
    }

    private void setHealthPermissionsUserFixedFlagValueUnchecked(
            String packageName, UserHandle user, List<String> permissions, boolean value) {
        enforceValidHealthPermissions(packageName, user, permissions);

        int flagMask = PackageManager.FLAG_PERMISSION_USER_FIXED;
        int flagValues = value ? PackageManager.FLAG_PERMISSION_USER_FIXED : 0;

        for (String permission : permissions) {
            mPackageManager.updatePermissionFlags(
                    permission, packageName, flagMask, flagValues, user);
        }
    }

    private boolean revokeAllHealthPermissionsUnchecked(
            String packageName, UserHandle user, @Nullable String reason) {
        List<String> grantedHealthPermissions =
                PackageInfoUtils.getGrantedHealthPermissions(mContext, packageName, user);
        for (String perm : grantedHealthPermissions) {
            revokeRuntimePermission(packageName, user, perm, reason);
            mPackageManager.updatePermissionFlags(
                    perm,
                    packageName,
                    MASK_PERMISSION_FLAGS,
                    PackageManager.FLAG_PERMISSION_USER_SET,
                    user);
            removeFromPriorityListIfRequired(packageName, perm, user);
        }
        boolean revoked = !grantedHealthPermissions.isEmpty();
        // If for legacy app, automatically deny BODY_SENSORS and BACKGROUND.
        if (isAppRequestingPermissionWithOutdatedTargetSdk(
                packageName,
                user,
                android.Manifest.permission.BODY_SENSORS,
                Build.VERSION_CODES.BAKLAVA)) {
            revoked |=
                    revokeRuntimePermissionAndUpdateFlags(
                            packageName,
                            user,
                            android.Manifest.permission.BODY_SENSORS,
                            PackageManager.FLAG_PERMISSION_USER_SET,
                            reason);
        }
        if (isAppRequestingPermissionWithOutdatedTargetSdk(
                packageName,
                user,
                android.Manifest.permission.BODY_SENSORS_BACKGROUND,
                Build.VERSION_CODES.BAKLAVA)) {
            revoked |=
                    revokeRuntimePermissionAndUpdateFlags(
                            packageName,
                            user,
                            android.Manifest.permission.BODY_SENSORS_BACKGROUND,
                            PackageManager.FLAG_PERMISSION_USER_SET,
                            reason);
        }
        return revoked;
    }

    private void revokeRuntimePermission(
            String packageName, UserHandle user, String permission, @Nullable String reason) {
        mPackageManager.revokeRuntimePermission(
                packageName, permission, user, reason == null ? UNKNOWN_REASON : reason);
    }

    private void enforceValidHealthPermission(String permissionName) {
        if (!HealthConnectManager.getHealthPermissions(mContext).contains(permissionName)) {
            throw new IllegalArgumentException("invalid health permission");
        }
    }

    private void enforceValidPackage(String packageName, UserHandle user) {
        PackageInfoUtils.getPackageInfoUnchecked(
                packageName, user, PackageManager.PackageInfoFlags.of(0), mContext);
    }

    private void enforceManageHealthPermissions(String message) {
        mContext.enforceCallingOrSelfPermission(
                HealthPermissions.MANAGE_HEALTH_PERMISSIONS, message);
    }

    private void enforceSupportPermissionsUsageIntent(
            String packageName, UserHandle userHandle, String permission) {
        if (!shouldEnforcePermissionUsageIntent(packageName, userHandle, permission)) {
            return;
        }

        if (!mPermissionIntentAppsTracker.supportsPermissionUsageIntent(packageName, userHandle)) {
            throw new SecurityException(
                    "Package "
                            + packageName
                            + " for "
                            + userHandle.toString()
                            + " doesn't support health permissions usage intent.");
        }
    }

    /**
     * Checks input user id and converts it to positive id if needed, returns converted user id.
     *
     * @throws java.lang.SecurityException if the caller is affecting different users without
     *     holding the {@link INTERACT_ACROSS_USERS_FULL} permission.
     */
    private int handleIncomingUser(int userId) {
        int callingUserId = UserHandle.getUserHandleForUid(Binder.getCallingUid()).getIdentifier();
        if (userId == callingUserId) {
            return userId;
        }

        boolean canInteractAcrossUsersFull =
                mContext.checkCallingOrSelfPermission(INTERACT_ACROSS_USERS_FULL)
                        == PERMISSION_GRANTED;
        if (canInteractAcrossUsersFull) {
            // If the UserHandle.CURRENT has been passed (negative value),
            // convert it to positive userId.
            if (userId == UserHandle.CURRENT.getIdentifier()) {
                return ActivityManager.getCurrentUser();
            }
            return userId;
        }

        throw new SecurityException(
                "Permission denied. Need to run as either the calling user id ("
                        + callingUserId
                        + "), or with "
                        + INTERACT_ACROSS_USERS_FULL
                        + " permission");
    }

    private void enforceValidHealthPermissions(
            String packageName, UserHandle user, List<String> permissions) {
        PackageInfo packageInfo =
                PackageInfoUtils.getPackageInfoUnchecked(
                        packageName,
                        user,
                        PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS),
                        mContext);

        Set<String> requestedPermissions = new ArraySet<>(packageInfo.requestedPermissions);

        for (String permission : permissions) {
            if (!requestedPermissions.contains(permission)) {
                throw new IllegalArgumentException(
                        "undeclared permission " + permission + " for package " + packageName);
            }

            enforceValidHealthPermission(permission);
        }
    }

    /** Sync permission granted status and flag between BODY_SENSORS and READ_HEART_RATE. */
    private void grantRuntimePermissionAndUpdateFlags(
            String packageName, UserHandle user, String legacyPermission, int permissionFlags) {
        mPackageManager.grantRuntimePermission(packageName, legacyPermission, user);
        mPackageManager.updatePermissionFlags(
                legacyPermission, packageName, MASK_PERMISSION_FLAGS, permissionFlags, user);
    }

    /** Sync permission denied status and flag between BODY_SENSORS and READ_HEART_RATE. */
    private boolean revokeRuntimePermissionAndUpdateFlags(
            String packageName,
            UserHandle user,
            String legacyPermission,
            int permissionFlags,
            @Nullable String reason) {
        boolean revoked = false;
        if (mPackageManager.checkPermission(legacyPermission, packageName)
                != PackageManager.PERMISSION_DENIED) {
            mPackageManager.revokeRuntimePermission(packageName, legacyPermission, user, reason);
            revoked = true;
        }
        mPackageManager.updatePermissionFlags(
                legacyPermission, packageName, MASK_PERMISSION_FLAGS, permissionFlags, user);
        return revoked;
    }

    /**
     * Returns legacy body sensor permission for split heart rate permission.
     *
     * @throws IllegalArgumentException if {@code permissionName} is neither READ_HEART_RATE nor
     *     READ_HEALTH_DATE_IN_BACKGROUND
     */
    private static String toLegacyPermission(String permissionName)
            throws IllegalArgumentException {
        if (permissionName.equals(READ_HEART_RATE)) {
            return android.Manifest.permission.BODY_SENSORS;
        }
        if (permissionName.equals(READ_HEALTH_DATA_IN_BACKGROUND)) {
            return android.Manifest.permission.BODY_SENSORS_BACKGROUND;
        }
        throw new IllegalArgumentException(
                "toLegacyPermission() encounters unexpected permission "
                        + permissionName
                        + ", should be one of READ_HEART_RATE and READ_HEALTH_DATA_IN_BACKGROUND");
    }
}
