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

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.health.connect.Constants;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.Log;

import com.android.internal.annotations.GuardedBy;

import java.io.File;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Manager class of the health permissions first grant time.
 *
 * @hide
 */
public class FirstGrantTimeManager implements PackageManager.OnPermissionsChangedListener {
    private static final String TAG = "HealthConnectFirstGrantTimeManager";
    private static final int CURRENT_VERSION = 1;

    private final PackageManager mPackageManager;
    private final HealthPermissionIntentAppsTracker mTracker;

    private final ReentrantReadWriteLock mGrantTimeLock = new ReentrantReadWriteLock();

    @GuardedBy("mGrantTimeLock")
    private final FirstGrantTimeDatastore mDatastore;

    @GuardedBy("mGrantTimeLock")
    private final UidToGrantTimeCache mUidToGrantTimeCache;

    @GuardedBy("mGrantTimeLock")
    private final Set<Integer> mRestoredAndValidatedUsers = new ArraySet<>();

    private final PackageInfoUtils mPackageInfoHelper;

    public FirstGrantTimeManager(
            @NonNull Context context,
            @NonNull HealthPermissionIntentAppsTracker tracker,
            @NonNull FirstGrantTimeDatastore datastore) {
        mTracker = tracker;
        mDatastore = datastore;
        mPackageManager = context.getPackageManager();
        mUidToGrantTimeCache = new UidToGrantTimeCache();
        mPackageInfoHelper = new PackageInfoUtils(context);
        mPackageManager.addOnPermissionsChangeListener(this);
    }

    /** Get the date when the first health permission was granted. */
    @Nullable
    public Instant getFirstGrantTime(@NonNull String packageName, @NonNull UserHandle user)
            throws IllegalArgumentException {

        Integer uid = mPackageInfoHelper.getPackageUid(packageName, user);
        if (uid == null) {
            throw new IllegalArgumentException(
                    "Package name "
                            + packageName
                            + " of user "
                            + user.getIdentifier()
                            + " not found.");
        }
        initAndValidateUserStateIfNeedLocked(user);

        Instant grantTimeDate = getGrantTimeReadLocked(uid);
        if (grantTimeDate == null) {
            // Check and update the state in case health permission has been granted before
            // onPermissionsChanged callback was propagated.
            onPermissionsChanged(mPackageInfoHelper.getPackageUid(packageName, user));
            grantTimeDate = getGrantTimeReadLocked(uid);
        }

        return grantTimeDate;
    }

    /**
     * Sets the provided first grant time for the given {@code packageName}, if it's not set yet.
     */
    public void setFirstGrantTime(
            @NonNull String packageName, @NonNull Instant time, @NonNull UserHandle user) {
        final Integer uid = mPackageInfoHelper.getPackageUid(packageName, user);
        if (uid == null) {
            throw new IllegalArgumentException(
                    "Package name "
                            + packageName
                            + " of user "
                            + user.getIdentifier()
                            + " not found.");
        }

        synchronized (mGrantTimeLock) {
            mUidToGrantTimeCache.put(uid, time);
            mDatastore.writeForUser(mUidToGrantTimeCache.extractUserGrantTimeState(user), user);
        }
    }

    @Override
    public void onPermissionsChanged(int uid) {
        String[] packageNames = mPackageManager.getPackagesForUid(uid);
        if (packageNames == null) {
            Log.w(TAG, "onPermissionsChanged: no known packages for UID: " + uid);
            return;
        }

        UserHandle user = UserHandle.getUserHandleForUid(uid);
        initAndValidateUserStateIfNeedLocked(user);

        if (!checkSupportPermissionsUsageIntent(packageNames, user)) {
            logIfInDebugMode("Can find health intent declaration in ", packageNames[0]);
            return;
        }

        boolean anyHealthPermissionGranted =
                mPackageInfoHelper.hasGrantedHealthPermissions(packageNames, user);

        boolean grantTimeRecorded = (getGrantTimeReadLocked(uid) != null);
        if (grantTimeRecorded != anyHealthPermissionGranted) {
            mGrantTimeLock.writeLock().lock();
            try {
                if (grantTimeRecorded) {
                    // An app doesn't have health permissions anymore, reset its grant time.
                    mUidToGrantTimeCache.remove(uid);
                } else {
                    // An app got new health permission, set current time as it's first grant
                    // time.
                    mUidToGrantTimeCache.put(uid, Instant.now());
                }

                UserGrantTimeState updatedState =
                        mUidToGrantTimeCache.extractUserGrantTimeState(user);
                logIfInDebugMode("State after onPermissionsChanged :", updatedState);
                mDatastore.writeForUser(updatedState, user);
            } finally {
                mGrantTimeLock.writeLock().unlock();
            }
        }
    }

    /** Returns the name of the file used for storing the data. */
    public File getFile(@NonNull UserHandle user) {
        return mDatastore.getFile(user);
    }

    void onPackageRemoved(
            @NonNull String packageName, int removedPackageUid, @NonNull UserHandle userHandle) {
        String[] leftSharedUidPackages =
                mPackageInfoHelper.getPackagesForUid(removedPackageUid, userHandle);
        if (leftSharedUidPackages != null && leftSharedUidPackages.length > 0) {
            // There are installed packages left with given UID,
            // don't need to update grant time state.
            return;
        }

        initAndValidateUserStateIfNeedLocked(userHandle);

        if (getGrantTimeReadLocked(removedPackageUid) != null) {
            mGrantTimeLock.writeLock().lock();
            try {
                mUidToGrantTimeCache.remove(removedPackageUid);
                UserGrantTimeState updatedState =
                        mUidToGrantTimeCache.extractUserGrantTimeState(userHandle);
                logIfInDebugMode("State after package " + packageName + " removed: ", updatedState);
                mDatastore.writeForUser(updatedState, userHandle);
            } finally {
                mGrantTimeLock.writeLock().unlock();
            }
        }
    }

    private Instant getGrantTimeReadLocked(Integer uid) {
        mGrantTimeLock.readLock().lock();
        try {
            return mUidToGrantTimeCache.get(uid);
        } finally {
            mGrantTimeLock.readLock().unlock();
        }
    }

    /** Initialize first grant time state for given user. */
    private void initAndValidateUserStateIfNeedLocked(UserHandle user) {
        if (userStateIsInitializedReadLocked(user)) {
            // This user state is already inited and validated
            return;
        }

        mGrantTimeLock.writeLock().lock();
        try {
            Log.i(
                    TAG,
                    "State for user: "
                            + user.getIdentifier()
                            + " has not been restored and validated.");
            UserGrantTimeState restoredState = restoreUserStateLocked(user);

            List<PackageInfo> validHealthApps =
                    mPackageInfoHelper.getPackagesHoldingHealthPermissions(user);
            logIfInDebugMode(
                    "Packages holding health perms of user " + user + " :", validHealthApps);

            // TODO(b/260585595): validate in B&R scenario.
            validateAndCorrectRecordedStateForUser(restoredState, validHealthApps, user);

            // TODO(b/260691599): consider removing mapping when getUidForSharedUser is
            Map<String, Set<Integer>> sharedUserNamesToUid =
                    mPackageInfoHelper.collectSharedUserNameToUidsMappingForUser(
                            validHealthApps, user);

            mUidToGrantTimeCache.populateFromUserGrantTimeState(
                    restoredState, sharedUserNamesToUid, user);

            mRestoredAndValidatedUsers.add(user.getIdentifier());
            logIfInDebugMode("State after init: ", restoredState);
            logIfInDebugMode("Cache after init: ", mUidToGrantTimeCache);
        } finally {
            mGrantTimeLock.writeLock().unlock();
        }
    }

    private boolean userStateIsInitializedReadLocked(UserHandle user) {
        mGrantTimeLock.readLock().lock();
        try {
            return mRestoredAndValidatedUsers.contains(user.getIdentifier());
        } finally {
            mGrantTimeLock.readLock().unlock();
        }
    }

    @GuardedBy("mGrantTimeLock")
    private UserGrantTimeState restoreUserStateLocked(UserHandle userHandle) {
        try {
            UserGrantTimeState restoredState = mDatastore.readForUser(userHandle);
            if (restoredState == null) {
                restoredState = new UserGrantTimeState(CURRENT_VERSION);
            }
            return restoredState;
        } catch (Exception e) {
            Log.e(TAG, "Error while reading from datastore: " + e);
            return new UserGrantTimeState(CURRENT_VERSION);
        }
    }

    /**
     * Validate current state and remove apps which are not present / hold health permissions, set
     * new grant time to apps which doesn't have grant time but installed and hold health
     * permissions. It should mitigate situation e.g. when permission mainline module did roll-back
     * and some health permissions got granted/revoked without onPermissionsChanged callback.
     *
     * @param recordedState restored state
     * @param healthPackagesInfos packageInfos of apps which currently hold health permissions
     * @param user UserHandle for whom to perform validation
     */
    @GuardedBy("mGrantTimeLock")
    private void validateAndCorrectRecordedStateForUser(
            @NonNull UserGrantTimeState recordedState,
            @NonNull List<PackageInfo> healthPackagesInfos,
            @NonNull UserHandle user) {
        Set<String> validPackagesPerUser = new ArraySet<>();
        Set<String> validSharedUsersPerUser = new ArraySet<>();

        boolean stateChanged = false;
        logIfInDebugMode("Valid apps for " + user + ": ", healthPackagesInfos);

        // If package holds health permissions but doesn't have recorded grant
        // time (e.g. because of permissions rollback), set current time as the first grant time.
        for (PackageInfo info : healthPackagesInfos) {
            if (info.sharedUserId == null) {
                stateChanged |= setPackageGrantTimeIfNotRecorded(recordedState, info.packageName);
                validPackagesPerUser.add(info.packageName);
            } else {
                stateChanged |=
                        setSharedUserGrantTimeIfNotRecorded(recordedState, info.sharedUserId);
                validSharedUsersPerUser.add(info.sharedUserId);
            }
        }

        // If package is not installed / doesn't hold health permissions
        // but has recorded first grant time, remove it from grant time state.
        stateChanged |=
                removeInvalidPackagesFromGrantTimeStateForUser(recordedState, validPackagesPerUser);

        stateChanged |=
                removeInvalidSharedUsersFromGrantTimeStateForUser(
                        recordedState, validSharedUsersPerUser);

        if (stateChanged) {
            logIfInDebugMode("Changed state after validation for " + user + ": ", recordedState);
            mDatastore.writeForUser(recordedState, user);
        }
    }

    @GuardedBy("mGrantTimeLock")
    private boolean setPackageGrantTimeIfNotRecorded(
            @NonNull UserGrantTimeState grantTimeState, @NonNull String packageName) {
        if (!grantTimeState.containsPackageGrantTime(packageName)) {
            Log.w(
                    TAG,
                    "No recorded grant time for package:"
                            + packageName
                            + ". Assigning current time as the first grant time.");
            grantTimeState.setPackageGrantTime(packageName, Instant.now());
            return true;
        }
        return false;
    }

    @GuardedBy("mGrantTimeLock")
    private boolean setSharedUserGrantTimeIfNotRecorded(
            @NonNull UserGrantTimeState grantTimeState, @NonNull String sharedUserIdName) {
        if (!grantTimeState.containsSharedUserGrantTime(sharedUserIdName)) {
            Log.w(
                    TAG,
                    "No recorded grant time for shared user:"
                            + sharedUserIdName
                            + ". Assigning current time as first grant time.");
            grantTimeState.setSharedUserGrantTime(sharedUserIdName, Instant.now());
            return true;
        }
        return false;
    }

    @GuardedBy("mGrantTimeLock")
    private boolean removeInvalidPackagesFromGrantTimeStateForUser(
            @NonNull UserGrantTimeState recordedState, @NonNull Set<String> validApps) {
        Set<String> recordedButNotValid =
                new ArraySet<>(recordedState.getPackageGrantTimes().keySet());
        if (validApps != null) {
            recordedButNotValid.removeAll(validApps);
        }

        if (!recordedButNotValid.isEmpty()) {
            Log.w(
                    TAG,
                    "Packages "
                            + recordedButNotValid
                            + " have recorded  grant times, but not installed or hold health "
                            + "permissions anymore. Removing them from the grant time state.");
            recordedState.getPackageGrantTimes().keySet().removeAll(recordedButNotValid);
            return true;
        }
        return false;
    }

    @GuardedBy("mGrantTimeLock")
    private boolean removeInvalidSharedUsersFromGrantTimeStateForUser(
            @NonNull UserGrantTimeState recordedState, @NonNull Set<String> validSharedUsers) {
        Set<String> recordedButNotValid =
                new ArraySet<>(recordedState.getSharedUserGrantTimes().keySet());
        if (validSharedUsers != null) {
            recordedButNotValid.removeAll(validSharedUsers);
        }

        if (!recordedButNotValid.isEmpty()) {
            Log.w(
                    TAG,
                    "Shared users "
                            + recordedButNotValid
                            + " have recorded  grant times, but not installed or hold health "
                            + "permissions anymore. Removing them from the grant time state.");
            recordedState.getSharedUserGrantTimes().keySet().removeAll(recordedButNotValid);
            return true;
        }
        return false;
    }

    private boolean checkSupportPermissionsUsageIntent(
            @NonNull String[] names, @NonNull UserHandle user) {
        for (String packageName : names) {
            if (mTracker.supportsPermissionUsageIntent(packageName, user)) {
                return true;
            }
        }
        return false;
    }

    private void logIfInDebugMode(@NonNull String prefixMessage, @NonNull Object objectToLog) {
        if (Constants.DEBUG) {
            Log.d(TAG, prefixMessage + objectToLog.toString());
        }
    }

    private class UidToGrantTimeCache {
        private final Map<Integer, Instant> mUidToGrantTime;

        UidToGrantTimeCache() {
            mUidToGrantTime = new ArrayMap<>();
        }

        @Override
        public String toString() {
            return mUidToGrantTime.toString();
        }

        @Nullable
        Instant remove(@Nullable Integer uid) {
            if (uid == null) {
                return null;
            }
            return mUidToGrantTime.remove(uid);
        }

        @Nullable
        Instant get(Integer uid) {
            return mUidToGrantTime.get(uid);
        }

        boolean containsKey(@Nullable Integer uid) {
            if (uid == null) {
                return false;
            }
            return mUidToGrantTime.containsKey(uid);
        }

        @Nullable
        Instant put(@NonNull Integer uid, @NonNull Instant time) {
            return mUidToGrantTime.put(uid, time);
        }

        @NonNull
        UserGrantTimeState extractUserGrantTimeState(@NonNull UserHandle user) {
            Map<String, Instant> sharedUserToGrantTime = new ArrayMap<>();
            Map<String, Instant> packageNameToGrantTime = new ArrayMap<>();

            for (Map.Entry<Integer, Instant> entry : mUidToGrantTime.entrySet()) {
                Integer uid = entry.getKey();
                Instant time = entry.getValue();

                if (!UserHandle.getUserHandleForUid(uid).equals(user)) {
                    continue;
                }

                String sharedUserName = mPackageInfoHelper.getSharedUserNameFromUid(uid);
                if (sharedUserName != null) {
                    sharedUserToGrantTime.put(sharedUserName, time);
                } else {
                    String packageName = mPackageInfoHelper.getPackageNameFromUid(uid);
                    if (packageName != null) {
                        packageNameToGrantTime.put(packageName, time);
                    }
                }
            }

            return new UserGrantTimeState(
                    packageNameToGrantTime, sharedUserToGrantTime, CURRENT_VERSION);
        }

        void populateFromUserGrantTimeState(
                @NonNull UserGrantTimeState grantTimeState,
                @NonNull Map<String, Set<Integer>> sharedUserNameToUids,
                @NonNull UserHandle user) {
            for (Map.Entry<String, Instant> entry :
                    grantTimeState.getSharedUserGrantTimes().entrySet()) {
                String sharedUserName = entry.getKey();
                Instant time = entry.getValue();

                if (sharedUserNameToUids.get(sharedUserName) == null) {
                    continue;
                }

                for (Integer uid : sharedUserNameToUids.get(sharedUserName)) {
                    put(uid, time);
                }
            }

            for (Map.Entry<String, Instant> entry :
                    grantTimeState.getPackageGrantTimes().entrySet()) {
                String packageName = entry.getKey();
                Instant time = entry.getValue();

                Integer uid = mPackageInfoHelper.getPackageUid(packageName, user);
                if (uid != null) {
                    put(uid, time);
                }
            }
        }
    }
}
