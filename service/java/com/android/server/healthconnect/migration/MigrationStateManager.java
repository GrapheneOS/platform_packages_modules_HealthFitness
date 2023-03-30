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

package com.android.server.healthconnect.migration;

import static android.health.connect.HealthConnectDataState.MIGRATION_STATE_ALLOWED;
import static android.health.connect.HealthConnectDataState.MIGRATION_STATE_APP_UPGRADE_REQUIRED;
import static android.health.connect.HealthConnectDataState.MIGRATION_STATE_COMPLETE;
import static android.health.connect.HealthConnectDataState.MIGRATION_STATE_IDLE;
import static android.health.connect.HealthConnectDataState.MIGRATION_STATE_IN_PROGRESS;
import static android.health.connect.HealthConnectDataState.MIGRATION_STATE_MODULE_UPGRADE_REQUIRED;

import static com.android.server.healthconnect.migration.MigrationConstants.ALLOWED_STATE_TIMEOUT_KEY;
import static com.android.server.healthconnect.migration.MigrationConstants.CURRENT_STATE_START_TIME_KEY;
import static com.android.server.healthconnect.migration.MigrationConstants.HAVE_CANCELED_OLD_MIGRATION_JOBS_KEY;
import static com.android.server.healthconnect.migration.MigrationConstants.HC_PACKAGE_NAME_CONFIG_NAME;
import static com.android.server.healthconnect.migration.MigrationConstants.HC_RELEASE_CERT_CONFIG_NAME;
import static com.android.server.healthconnect.migration.MigrationConstants.MAX_START_MIGRATION_CALLS_ALLOWED;
import static com.android.server.healthconnect.migration.MigrationConstants.MIGRATION_COMPLETE_JOB_NAME;
import static com.android.server.healthconnect.migration.MigrationConstants.MIGRATION_PAUSE_JOB_NAME;
import static com.android.server.healthconnect.migration.MigrationConstants.MIGRATION_STARTS_COUNT_KEY;
import static com.android.server.healthconnect.migration.MigrationConstants.MIGRATION_STATE_PREFERENCE_KEY;
import static com.android.server.healthconnect.migration.MigrationConstants.MIN_DATA_MIGRATION_SDK_EXTENSION_VERSION_KEY;
import static com.android.server.healthconnect.migration.MigrationConstants.NON_IDLE_STATE_TIMEOUT_PERIOD;
import static com.android.server.healthconnect.migration.MigrationUtils.filterIntent;
import static com.android.server.healthconnect.migration.MigrationUtils.filterPermissions;

import android.annotation.NonNull;
import android.annotation.UserIdInt;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.res.Resources;
import android.health.connect.Constants;
import android.health.connect.HealthConnectDataState;
import android.os.Build;
import android.os.ext.SdkExtensions;
import android.util.Slog;

import com.android.internal.annotations.GuardedBy;
import com.android.server.healthconnect.HealthConnectThreadScheduler;
import com.android.server.healthconnect.storage.datatypehelpers.PreferenceHelper;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * A database operations helper for migration states management.
 *
 * @hide
 */
public final class MigrationStateManager {
    @GuardedBy("sInstanceLock")
    private static MigrationStateManager sMigrationStateManager;

    private static final Object sInstanceLock = new Object();
    private static final String TAG = "MigrationStateManager";

    @GuardedBy("mLock")
    private final Set<StateChangedListener> mStateChangedListeners = new CopyOnWriteArraySet<>();

    private final Object mLock = new Object();
    private volatile MigrationBroadcastScheduler mMigrationBroadcastScheduler;
    private int mUserId;

    private MigrationStateManager(@UserIdInt int userId) {
        mUserId = userId;
    }

    /**
     * Initialises {@link MigrationStateManager} with the provided arguments and returns the
     * instance.
     */
    @NonNull
    public static MigrationStateManager initializeInstance(@UserIdInt int userId) {
        synchronized (sInstanceLock) {
            if (Objects.isNull(sMigrationStateManager)) {
                sMigrationStateManager = new MigrationStateManager(userId);
            }

            return sMigrationStateManager;
        }
    }

    /** Re-initialize this class instance with the new user */
    public void onUserSwitching(@NonNull Context context, @UserIdInt int userId) {
        synchronized (mLock) {
            MigrationStateChangeJob.cancelAllJobs(context);
            mUserId = userId;
        }
    }

    /** Returns initialised instance of this class. */
    @NonNull
    public static MigrationStateManager getInitialisedInstance() {
        synchronized (sInstanceLock) {
            Objects.requireNonNull(sMigrationStateManager);
            return sMigrationStateManager;
        }
    }

    /** Registers {@link StateChangedListener} for observing migration state changes. */
    public void addStateChangedListener(@NonNull StateChangedListener listener) {
        synchronized (mLock) {
            mStateChangedListeners.add(listener);
        }
    }

    public void setMigrationBroadcastScheduler(
            MigrationBroadcastScheduler migrationBroadcastScheduler) {
        mMigrationBroadcastScheduler = migrationBroadcastScheduler;
    }

    /**
     * Adds the min data migration sdk and updates the migration state to pending.
     *
     * @param minVersion the desired sdk version.
     */
    public void setMinDataMigrationSdkExtensionVersion(@NonNull Context context, int minVersion) {
        synchronized (mLock) {
            if (minVersion <= getUdcSdkExtensionVersion()) {
                updateMigrationState(context, MIGRATION_STATE_ALLOWED);
                return;
            }
            PreferenceHelper.getInstance()
                    .insertOrReplacePreference(
                            MIN_DATA_MIGRATION_SDK_EXTENSION_VERSION_KEY,
                            String.valueOf(minVersion));
            updateMigrationState(context, MIGRATION_STATE_MODULE_UPGRADE_REQUIRED);
        }
    }

    /**
     * @return true when the migration state is in_progress.
     */
    public boolean isMigrationInProgress() {
        return getMigrationState() == MIGRATION_STATE_IN_PROGRESS;
    }

    @HealthConnectDataState.DataMigrationState
    public int getMigrationState() {
        String migrationState =
                PreferenceHelper.getInstance().getPreference(MIGRATION_STATE_PREFERENCE_KEY);
        if (Objects.isNull(migrationState)) {
            return MIGRATION_STATE_IDLE;
        }

        return Integer.parseInt(migrationState);
    }

    public void switchToSetupForUser(@NonNull Context context) {
        synchronized (mLock) {
            cleanupOldPersistentMigrationJobsIfNeeded(context);
            MigrationStateChangeJob.cancelAllJobs(context);
            reconcilePackageChangesWithStates(context);
            reconcileStateChangeJob(context);
        }
    }

    /** Updates the migration state. */
    public void updateMigrationState(
            @NonNull Context context, @HealthConnectDataState.DataMigrationState int state) {
        synchronized (mLock) {
            updateMigrationStateGuarded(context, state);
        }
    }

    @GuardedBy("mLock")
    private void updateMigrationStateGuarded(
            @NonNull Context context, @HealthConnectDataState.DataMigrationState int state) {
        if (state == getMigrationState()) {
            if (Constants.DEBUG) {
                Slog.d(TAG, "The new state same as the current state.");
            }
            return;
        }

        switch (state) {
            case MIGRATION_STATE_APP_UPGRADE_REQUIRED:
            case MIGRATION_STATE_MODULE_UPGRADE_REQUIRED:
                MigrationStateChangeJob.cancelAllJobs(context);
                updateMigrationStatePreference(context, state);
                MigrationStateChangeJob.scheduleMigrationCompletionJob(context, mUserId);
                return;
            case MIGRATION_STATE_IN_PROGRESS:
                MigrationStateChangeJob.cancelAllJobs(context);
                updateMigrationStatePreference(context, MIGRATION_STATE_IN_PROGRESS);
                MigrationStateChangeJob.scheduleMigrationPauseJob(context, mUserId);
                updateMigrationStartsCount();
                return;
            case MIGRATION_STATE_ALLOWED:
                if (hasAllowedStateTimedOut()
                        || getStartMigrationCount() >= MAX_START_MIGRATION_CALLS_ALLOWED) {
                    updateMigrationState(context, MIGRATION_STATE_COMPLETE);
                    return;
                }
                MigrationStateChangeJob.cancelAllJobs(context);
                updateMigrationStatePreference(context, MIGRATION_STATE_ALLOWED);
                MigrationStateChangeJob.scheduleMigrationCompletionJob(context, mUserId);
                return;
            case MIGRATION_STATE_COMPLETE:
                updateMigrationStatePreference(context, MIGRATION_STATE_COMPLETE);
                MigrationStateChangeJob.cancelAllJobs(context);
                return;
            default:
                throw new IllegalArgumentException(
                        "Cannot updated migration state. Unknown state: " + state);
        }
    }

    public void clearCaches(@NonNull Context context) {
        synchronized (mLock) {
            PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();
            updateMigrationStatePreference(context, MIGRATION_STATE_IDLE);
            preferenceHelper.insertOrReplacePreference(
                    MIGRATION_STARTS_COUNT_KEY, String.valueOf(0));
            preferenceHelper.insertOrReplacePreference(
                    ALLOWED_STATE_TIMEOUT_KEY,
                    Instant.now().plusMillis(NON_IDLE_STATE_TIMEOUT_PERIOD.toMillis()).toString());
        }
    }

    /** Thrown when an illegal migration state is detected. */
    public static final class IllegalMigrationStateException extends Exception {
        public IllegalMigrationStateException(String message) {
            super(message);
        }
    }

    /**
     * Throws {@link IllegalMigrationStateException} if the migration can not be started in the
     * current state.
     */
    public void validateStartMigration() throws IllegalMigrationStateException {
        synchronized (mLock) {
            validateStartMigrationGuarded();
        }
    }

    @GuardedBy("mLock")
    private void validateStartMigrationGuarded() throws IllegalMigrationStateException {
        throwIfMigrationIsComplete();
    }

    /**
     * Throws {@link IllegalMigrationStateException} if the migration can not be finished in the
     * current state.
     */
    public void validateFinishMigration() throws IllegalMigrationStateException {
        synchronized (mLock) {
            throwIfMigrationIsComplete();
            if (getMigrationState() != MIGRATION_STATE_IN_PROGRESS
                    && getMigrationState() != MIGRATION_STATE_ALLOWED) {
                throw new IllegalMigrationStateException("Migration is not started.");
            }
        }
    }

    /**
     * Throws {@link IllegalMigrationStateException} if the migration can not be performed in the
     * current state.
     */
    public void validateWriteMigrationData() throws IllegalMigrationStateException {
        synchronized (mLock) {
            throwIfMigrationIsComplete();
            if (getMigrationState() != MIGRATION_STATE_IN_PROGRESS) {
                throw new IllegalMigrationStateException("Migration is not started.");
            }
        }
    }

    /**
     * Throws {@link IllegalMigrationStateException} if the sdk extension version can not be set in
     * the current state.
     */
    public void validateSetMinSdkVersion() throws IllegalMigrationStateException {
        synchronized (mLock) {
            throwIfMigrationIsComplete();
            if (getMigrationState() == MIGRATION_STATE_IN_PROGRESS
                    && getMigrationState() == MIGRATION_STATE_ALLOWED) {
                throw new IllegalMigrationStateException(
                        "Cannot set the sdk extension version. Migration already in progress.");
            }
        }
    }

    void onPackageInstalledOrChanged(@NonNull Context context, @NonNull String packageName) {
        synchronized (mLock) {
            onPackageInstalledOrChangedGuarded(context, packageName);
        }
    }

    @GuardedBy("mLock")
    private void onPackageInstalledOrChangedGuarded(
            @NonNull Context context, @NonNull String packageName) {
        String hcMigratorPackage = getDataMigratorPackageName(context);
        if (!Objects.equals(hcMigratorPackage, packageName)) {
            return;
        }

        int migrationState = getMigrationState();
        if ((migrationState == MIGRATION_STATE_IDLE
                        || migrationState == MIGRATION_STATE_APP_UPGRADE_REQUIRED)
                && isMigrationAware(context, packageName)) {
            updateMigrationState(context, MIGRATION_STATE_ALLOWED);
            return;
        }

        if (migrationState == MIGRATION_STATE_IDLE
                && hasMigratorPackageKnownSignerSignature(context, packageName)) {
            // apk needs to upgrade
            updateMigrationState(context, MIGRATION_STATE_APP_UPGRADE_REQUIRED);
        }
    }

    void onPackageRemoved(@NonNull Context context, @NonNull String packageName) {
        synchronized (mLock) {
            onPackageRemovedGuarded(context, packageName);
        }
    }

    @GuardedBy("mLock")
    private void onPackageRemovedGuarded(@NonNull Context context, @NonNull String packageName) {
        String hcMigratorPackage = getDataMigratorPackageName(context);
        if (!Objects.equals(hcMigratorPackage, packageName)) {
            return;
        }

        if (getMigrationState() != MIGRATION_STATE_COMPLETE) {
            if (Constants.DEBUG) {
                Slog.d(TAG, "Migrator package uninstalled. Marking migration complete.");
            }
            updateMigrationState(context, MIGRATION_STATE_COMPLETE);
        }
    }

    @GuardedBy("mLock")
    private void updateMigrationStatePreference(
            @NonNull Context context,
            @HealthConnectDataState.DataMigrationState int migrationState) {
        HashMap<String, String> preferences =
                new HashMap<>(
                        Map.of(
                                MIGRATION_STATE_PREFERENCE_KEY, String.valueOf(migrationState),
                                CURRENT_STATE_START_TIME_KEY, Instant.now().toString()));

        // If we are setting the migration state to ALLOWED for the first time.
        if (migrationState == MIGRATION_STATE_ALLOWED && Objects.isNull(getAllowedStateTimeout())) {
            preferences.put(
                    ALLOWED_STATE_TIMEOUT_KEY,
                    Instant.now().plusMillis(NON_IDLE_STATE_TIMEOUT_PERIOD.toMillis()).toString());
        }
        PreferenceHelper.getInstance().insertOrReplacePreferencesTransaction(preferences);

        if (mMigrationBroadcastScheduler != null) {
            HealthConnectThreadScheduler.scheduleInternalTask(
                    () -> {
                        try {
                            mMigrationBroadcastScheduler.prescheduleNewJobs(context);
                        } catch (Exception e) {
                            Slog.e(TAG, "Migration broadcast schedule failed", e);
                        }
                    });
        } else if (Constants.DEBUG) {
            Slog.d(
                    TAG,
                    "Unable to schedule migration broadcasts: "
                            + "MigrationBroadcastScheduler object is null");
        }

        for (StateChangedListener listener : mStateChangedListeners) {
            listener.onChanged(migrationState);
        }
    }

    /**
     * Checks if the original {@link MIGRATION_STATE_ALLOWED} timeout period has passed. We do not
     * want to reset the ALLOWED_STATE timeout everytime state changes to this state, hence
     * persisting the original timeout time.
     */
    private boolean hasAllowedStateTimedOut() {
        String allowedStateTimeout = getAllowedStateTimeout();
        if (!Objects.isNull(allowedStateTimeout)
                && Instant.now().isAfter(Instant.parse(allowedStateTimeout))) {
            Slog.e(TAG, "Allowed state period has timed out.");
            return true;
        }
        return false;
    }

    /**
     * Reconcile migration state to the current migrator package status in case we missed a package
     * change broadcast.
     */
    @GuardedBy("mLock")
    private void reconcilePackageChangesWithStates(Context context) {
        int migrationState = getMigrationState();
        if (migrationState == MIGRATION_STATE_APP_UPGRADE_REQUIRED
                && existsMigrationAwarePackage(context)) {
            updateMigrationState(context, MIGRATION_STATE_ALLOWED);
            return;
        }

        if (migrationState == MIGRATION_STATE_IDLE) {
            if (existsMigrationAwarePackage(context)) {
                updateMigrationState(context, MIGRATION_STATE_ALLOWED);
                return;
            }

            if (existsMigratorPackage(context)) {
                updateMigrationState(context, MIGRATION_STATE_APP_UPGRADE_REQUIRED);
                return;
            }
        }
        if (migrationState != MIGRATION_STATE_IDLE && migrationState != MIGRATION_STATE_COMPLETE) {
            completeMigrationIfNoMigratorPackageAvailable(context);
        }
    }

    /** Reconcile the current state with its appropriate state change job. */
    @GuardedBy("mLock")
    private void reconcileStateChangeJob(@NonNull Context context) {
        switch (getMigrationState()) {
            case MIGRATION_STATE_IDLE:
            case MIGRATION_STATE_APP_UPGRADE_REQUIRED:
            case MIGRATION_STATE_ALLOWED:
                if (!MigrationStateChangeJob.existsAStateChangeJob(
                        context, MIGRATION_COMPLETE_JOB_NAME)) {
                    MigrationStateChangeJob.scheduleMigrationCompletionJob(context, mUserId);
                }
                return;
            case MIGRATION_STATE_MODULE_UPGRADE_REQUIRED:
                handleIsUpgradeStillRequired(context);
                return;

            case MIGRATION_STATE_IN_PROGRESS:
                if (!MigrationStateChangeJob.existsAStateChangeJob(
                        context, MIGRATION_PAUSE_JOB_NAME)) {
                    MigrationStateChangeJob.scheduleMigrationPauseJob(context, mUserId);
                }
                return;

            case MIGRATION_STATE_COMPLETE:
                MigrationStateChangeJob.cancelAllJobs(context);
        }
    }

    /**
     * Checks if the version set by the migrator apk is the current module version and send a {@link
     * HealthConnectManager.ACTION_HEALTH_CONNECT_MIGRATION_READY intent. If not, re-sync the state
     * update job.}
     */
    @GuardedBy("mLock")
    private void handleIsUpgradeStillRequired(@NonNull Context context) {
        if (Integer.parseInt(
                        PreferenceHelper.getInstance()
                                .getPreference(MIN_DATA_MIGRATION_SDK_EXTENSION_VERSION_KEY))
                <= getUdcSdkExtensionVersion()) {
            updateMigrationState(context, MIGRATION_STATE_ALLOWED);
            return;
        }
        if (!MigrationStateChangeJob.existsAStateChangeJob(context, MIGRATION_COMPLETE_JOB_NAME)) {
            MigrationStateChangeJob.scheduleMigrationCompletionJob(context, mUserId);
        }
    }

    private String getAllowedStateTimeout() {
        return PreferenceHelper.getInstance().getPreference(ALLOWED_STATE_TIMEOUT_KEY);
    }

    private void throwIfMigrationIsComplete() throws IllegalMigrationStateException {
        if (getMigrationState() == MIGRATION_STATE_COMPLETE) {
            throw new IllegalMigrationStateException("Migration already marked complete.");
        }
    }

    /**
     * Tracks the number of times migration is started from {@link MIGRATION_STATE_ALLOWED}. If more
     * than 3 times, the migration is marked as complete
     */
    @GuardedBy("mLock")
    private void updateMigrationStartsCount() {
        PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();
        String migrationStartsCount =
                Optional.ofNullable(preferenceHelper.getPreference(MIGRATION_STARTS_COUNT_KEY))
                        .orElse("0");

        preferenceHelper.insertOrReplacePreference(
                MIGRATION_STARTS_COUNT_KEY,
                String.valueOf(Integer.parseInt(migrationStartsCount) + 1));
    }

    private String getDataMigratorPackageName(@NonNull Context context) {
        return context.getString(
                context.getResources().getIdentifier(HC_PACKAGE_NAME_CONFIG_NAME, null, null));
    }

    private void completeMigrationIfNoMigratorPackageAvailable(@NonNull Context context) {
        if (existsMigrationAwarePackage(context)) {
            if (Constants.DEBUG) {
                Slog.d(TAG, "There is a migration aware package.");
            }
            return;
        }

        if (existsMigratorPackage(context)) {
            if (Constants.DEBUG) {
                Slog.d(TAG, "There is a package with migration known signers certificate.");
            }
            return;
        }

        if (Constants.DEBUG) {
            Slog.d(
                    TAG,
                    "There is no migration aware package or any package with migration known "
                            + "signers certificate. Marking migration as complete.");
        }
        updateMigrationState(context, MIGRATION_STATE_COMPLETE);
    }

    private boolean existsMigrationAwarePackage(@NonNull Context context) {
        List<String> filteredPackages = filterIntent(context, filterPermissions(context));
        String dataMigratorPackageName = getDataMigratorPackageName(context);
        List<String> filteredDataMigratorPackageNames =
                filteredPackages.stream()
                        .filter(packageName -> packageName.equals(dataMigratorPackageName))
                        .toList();
        return filteredDataMigratorPackageNames.size() != 0;
    }

    private boolean existsMigratorPackage(@NonNull Context context) {
        // Search through all packages by known signer certificate.
        List<PackageInfo> allPackages =
                context.getPackageManager()
                        .getInstalledPackages(PackageManager.GET_SIGNING_CERTIFICATES);
        String[] knownSignerCerts = getMigrationKnownSignerCertificates(context);

        for (PackageInfo packageInfo : allPackages) {
            if (hasMatchingSignatures(getPackageSignatures(packageInfo), knownSignerCerts)) {
                return true;
            }
        }
        return false;
    }

    private boolean isMigrationAware(@NonNull Context context, @NonNull String packageName) {
        List<String> permissionFilteredPackages = filterPermissions(context);
        List<String> filteredPackages = filterIntent(context, permissionFilteredPackages);
        int numPackages = filteredPackages.size();

        if (numPackages == 0) {
            Slog.i(TAG, "There are no migration aware apps");
        } else if (numPackages == 1) {
            return Objects.equals(filteredPackages.get(0), packageName);
        }
        return false;
    }

    private static boolean hasMigratorPackageKnownSignerSignature(
            @NonNull Context context, @NonNull String packageName) {
        List<String> stringSignatures;
        try {
            stringSignatures =
                    getPackageSignatures(
                            context.getPackageManager()
                                    .getPackageInfo(
                                            packageName, PackageManager.GET_SIGNING_CERTIFICATES));
        } catch (PackageManager.NameNotFoundException e) {
            Slog.i(TAG, "Could not get package signatures. Package not found");
            return false;
        }

        if (stringSignatures.isEmpty()) {
            return false;
        }
        return hasMatchingSignatures(
                stringSignatures, getMigrationKnownSignerCertificates(context));
    }

    private static boolean hasMatchingSignatures(
            List<String> stringSignatures, String[] migrationKnownSignerCertificates) {
        return !Collections.disjoint(
                stringSignatures.stream().map(String::toLowerCase).toList(),
                Arrays.stream(migrationKnownSignerCertificates).map(String::toLowerCase).toList());
    }

    private static String[] getMigrationKnownSignerCertificates(Context context) {
        return context.getResources()
                .getStringArray(
                        Resources.getSystem()
                                .getIdentifier(HC_RELEASE_CERT_CONFIG_NAME, null, null));
    }

    private static List<String> getPackageSignatures(PackageInfo packageInfo) {
        return Arrays.stream(packageInfo.signingInfo.getApkContentsSigners())
                .map(Signature::toCharsString)
                .toList();
    }

    private int getUdcSdkExtensionVersion() {
        return SdkExtensions.getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE);
    }

    private int getStartMigrationCount() {
        return Integer.parseInt(
                Optional.ofNullable(
                                PreferenceHelper.getInstance()
                                        .getPreference(MIGRATION_STARTS_COUNT_KEY))
                        .orElse("0"));
    }

    private void cleanupOldPersistentMigrationJobsIfNeeded(@NonNull Context context) {
        PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();

        if (!Boolean.parseBoolean(
                preferenceHelper.getPreference(HAVE_CANCELED_OLD_MIGRATION_JOBS_KEY))) {
            MigrationStateChangeJob.cleanupOldPersistentMigrationJobs(context);
            preferenceHelper.insertOrReplacePreference(
                    HAVE_CANCELED_OLD_MIGRATION_JOBS_KEY, String.valueOf(true));
        }
    }

    /**
     * A listener for observing migration state changes.
     *
     * @see MigrationStateManager#addStateChangedListener(StateChangedListener)
     */
    public interface StateChangedListener {

        /**
         * Called on every migration state change.
         *
         * @param state the new migration state.
         */
        void onChanged(@HealthConnectDataState.DataMigrationState int state);
    }
}
