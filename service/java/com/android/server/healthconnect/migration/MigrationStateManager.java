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
import static com.android.server.healthconnect.migration.MigrationConstants.HC_PACKAGE_NAME_CONFIG_NAME;
import static com.android.server.healthconnect.migration.MigrationConstants.HC_RELEASE_CERT_CONFIG_NAME;
import static com.android.server.healthconnect.migration.MigrationConstants.MAX_START_MIGRATION_CALLS_ALLOWED;
import static com.android.server.healthconnect.migration.MigrationConstants.MIGRATION_COMPLETE_JOB_NAME;
import static com.android.server.healthconnect.migration.MigrationConstants.MIGRATION_PAUSE_JOB_NAME;
import static com.android.server.healthconnect.migration.MigrationConstants.NON_IDLE_STATE_TIMEOUT_PERIOD;
import static com.android.server.healthconnect.migration.MigrationUtils.filterIntent;
import static com.android.server.healthconnect.migration.MigrationUtils.filterPermissions;

import android.annotation.NonNull;
import android.annotation.UserIdInt;
import android.app.job.JobInfo;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.res.Resources;
import android.health.connect.Constants;
import android.health.connect.HealthConnectDataState;
import android.health.connect.HealthConnectManager;
import android.os.Binder;
import android.os.Build;
import android.os.ext.SdkExtensions;
import android.util.Slog;

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

/**
 * A database operations helper for migration states management.
 *
 * @hide
 */
public final class MigrationStateManager {
    private static volatile MigrationStateManager sMigrationStateManager;
    private static final String MIGRATION_STATE_PREFERENCE_KEY = "migration_state";
    private static final String MIN_DATA_MIGRATION_SDK_EXTENSION_VERSION_KEY =
            "min_data_migration_sdk_extension_version";
    private static final String TAG = "MigrationStateManager";
    private static final String MIGRATION_STARTS_COUNT_KEY = "migration_starts_count";
    private volatile MigrationBroadcastScheduler mMigrationBroadcastScheduler;
    private final int mUserId;
    private final Context mContext;

    private MigrationStateManager(@NonNull Context context, @UserIdInt int userId) {
        Objects.requireNonNull(context);
        mUserId = userId;
        mContext = context;
    }

    public static void initializeInstance(@NonNull Context context, @UserIdInt int userId) {
        if (Objects.isNull(sMigrationStateManager)) {
            sMigrationStateManager = new MigrationStateManager(context, userId);
        }
    }

    /** Re-initialize this class instance with the new user */
    public void onUserSwitching(@UserIdInt int userId) {
        MigrationStateChangeJob.cancelAllPendingJobs(mContext, mUserId);
        sMigrationStateManager = new MigrationStateManager(mContext, userId);
    }

    /** Returns initialised instance of this class. */
    @NonNull
    public static MigrationStateManager getInitialisedInstance() {
        Objects.requireNonNull(sMigrationStateManager);
        return sMigrationStateManager;
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
    public void setMinDataMigrationSdkExtensionVersion(int minVersion) {
        if (minVersion <= getUdcSdkExtensionVersion()) {
            updateMigrationState(MIGRATION_STATE_ALLOWED);
            return;
        }
        PreferenceHelper.getInstance()
                .insertOrReplacePreference(
                        MIN_DATA_MIGRATION_SDK_EXTENSION_VERSION_KEY, String.valueOf(minVersion));
        updateMigrationState(MIGRATION_STATE_MODULE_UPGRADE_REQUIRED);
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

    // TODO(b/272745797): Check if we are in non-idle state and apk is no longer available, move
    // migration to complete
    public void switchToSetupForUser() {
        reconcileStateChangeJob();
    }

    public synchronized void updateMigrationState(
            @HealthConnectDataState.DataMigrationState int state) {
        if (state == getMigrationState()) {
            Slog.e(TAG, "The new state same as the current state.");
            return;
        }

        switch (state) {
            case MIGRATION_STATE_APP_UPGRADE_REQUIRED:
            case MIGRATION_STATE_MODULE_UPGRADE_REQUIRED:
                MigrationStateChangeJob.cancelPendingJob(
                        mContext, mUserId, MIGRATION_COMPLETE_JOB_NAME);
                updateMigrationStatePreference(state);
                MigrationStateChangeJob.scheduleMigrationCompletionJob(mContext, mUserId);
                return;
            case MIGRATION_STATE_IN_PROGRESS:
                MigrationStateChangeJob.cancelPendingJob(
                        mContext, mUserId, MIGRATION_COMPLETE_JOB_NAME);
                updateMigrationStatePreference(MIGRATION_STATE_IN_PROGRESS);
                MigrationStateChangeJob.scheduleMigrationPauseJob(mContext, mUserId);
                updateMigrationStartsCount();
                return;
            case MIGRATION_STATE_ALLOWED:
                MigrationStateChangeJob.cancelAllPendingJobs(mContext, mUserId);
                if (hasAllowedStateTimedOut()) {
                    updateMigrationState(MIGRATION_STATE_COMPLETE);
                    return;
                }
                updateMigrationStatePreference(MIGRATION_STATE_ALLOWED);
                MigrationStateChangeJob.scheduleMigrationCompletionJob(mContext, mUserId);
                return;
            case MIGRATION_STATE_COMPLETE:
                updateMigrationStatePreference(MIGRATION_STATE_COMPLETE);
                MigrationStateChangeJob.cancelAllPendingJobs(mContext, mUserId);
        }
    }

    public void clearCaches() {
        PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();
        updateMigrationStatePreference(MIGRATION_STATE_IDLE);
        preferenceHelper.insertOrReplacePreference(MIGRATION_STARTS_COUNT_KEY, String.valueOf(0));
        preferenceHelper.insertOrReplacePreference(
                ALLOWED_STATE_TIMEOUT_KEY,
                Instant.now().plusMillis(NON_IDLE_STATE_TIMEOUT_PERIOD.toMillis()).toString());
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
        throwIfMigrationIsComplete();
        if (getMigrationState() == MIGRATION_STATE_IN_PROGRESS) {
            return;
        }

        if (getMigrationState() == MIGRATION_STATE_APP_UPGRADE_REQUIRED) {
            throw new IllegalMigrationStateException(
                    "Cannot start migration from the current state.");
        }

        PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();
        int migrationStartsCount =
                Integer.parseInt(
                        Optional.ofNullable(
                                        preferenceHelper.getPreference(MIGRATION_STARTS_COUNT_KEY))
                                .orElse("0"));

        if (migrationStartsCount > MAX_START_MIGRATION_CALLS_ALLOWED) {
            throw new IllegalMigrationStateException(
                    "Caller has exceeded the number of startMigration calls allowed. Migration is"
                            + " marked Complete now.");
        }

        preferenceHelper.insertOrReplacePreference(
                MIGRATION_STARTS_COUNT_KEY, String.valueOf(migrationStartsCount));
    }

    /**
     * Throws {@link IllegalMigrationStateException} if the migration can not be finished in the
     * current state.
     */
    public void validateFinishMigration() throws IllegalMigrationStateException {
        throwIfMigrationIsComplete();
        if (getMigrationState() != MIGRATION_STATE_IN_PROGRESS
                && getMigrationState() != MIGRATION_STATE_ALLOWED) {
            throw new IllegalMigrationStateException("Migration is not started.");
        }
    }

    /**
     * Throws {@link IllegalMigrationStateException} if the migration can not be performed in the
     * current state.
     */
    public void validateWriteMigrationData() throws IllegalMigrationStateException {
        throwIfMigrationIsComplete();
        if (getMigrationState() != MIGRATION_STATE_IN_PROGRESS) {
            throw new IllegalMigrationStateException("Migration is not started.");
        }
    }

    /**
     * Throws {@link IllegalMigrationStateException} if the sdk extension version can not be set in
     * the current state.
     */
    public void validateSetMinSdkVersion() throws IllegalMigrationStateException {
        throwIfMigrationIsComplete();
        if (getMigrationState() == MIGRATION_STATE_IN_PROGRESS
                && getMigrationState() == MIGRATION_STATE_ALLOWED) {
            throw new IllegalMigrationStateException(
                    "Cannot set the sdk extension version. Migration already in progress.");
        }
    }

    void onPackageInstalledOrChanged(@NonNull Context context, @NonNull String packageName) {
        String hcMigratorPackage = getDataMigratorPackageName(context);
        if (!Objects.equals(hcMigratorPackage, packageName)) {
            return;
        }

        if (isMigrationAware(context, hcMigratorPackage)) {
            updateMigrationState(MIGRATION_STATE_ALLOWED);
            return;
        }

        if (hasMigratorPackageKnownSignerSignature(context, hcMigratorPackage)) {
            // apk needs to upgrade
            updateMigrationState(MIGRATION_STATE_APP_UPGRADE_REQUIRED);
        }
    }

    void onPackageRemoved(@NonNull Context context, @NonNull String packageName) {

        String hcMigratorPackage = getDataMigratorPackageName(context);
        if (!Objects.equals(hcMigratorPackage, packageName)) {
            return;
        }

        if (getMigrationState() != MIGRATION_STATE_COMPLETE) {
            updateMigrationState(MIGRATION_STATE_COMPLETE);
        }
    }

    private void updateMigrationStatePreference(
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
                            mMigrationBroadcastScheduler.prescheduleNewJobs(mContext);
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

    /** Reconcile the current state with its appropriate state change job. */
    private void reconcileStateChangeJob() {
        switch (getMigrationState()) {
            case MIGRATION_STATE_IDLE:
            case MIGRATION_STATE_APP_UPGRADE_REQUIRED:
            case MIGRATION_STATE_ALLOWED:
                rescheduleCompleteJobIfNoneFound();
                return;
            case MIGRATION_STATE_MODULE_UPGRADE_REQUIRED:
                handleIsUpgradeStillRequired();
                return;

            case MIGRATION_STATE_IN_PROGRESS:
                handleInProgressState();
                return;

            case MIGRATION_STATE_COMPLETE:
                MigrationStateChangeJob.cancelAllPendingJobs(mContext, mUserId);
        }
    }

    /**
     * Handles migration state change jobs if migration state is {@link MIGRATION_STATE_IN_PROGRESS}
     * on user unlock
     */
    private void handleInProgressState() {
        JobInfo jobInfo =
                MigrationStateChangeJob.getPendingJob(
                        mContext, Binder.getCallingUid(), MIGRATION_PAUSE_JOB_NAME);
        if (Objects.isNull(jobInfo)) {
            MigrationStateChangeJob.scheduleMigrationPauseJob(mContext, mUserId);
        }
    }

    /**
     * Checks if the version set by the migrator apk is the current module version and send a {@link
     * HealthConnectManager.ACTION_HEALTH_CONNECT_MIGRATION_READY intent. If not, re-sync the state
     * update job.}
     */
    private void handleIsUpgradeStillRequired() {
        if (Integer.parseInt(
                        PreferenceHelper.getInstance()
                                .getPreference(MIN_DATA_MIGRATION_SDK_EXTENSION_VERSION_KEY))
                <= getUdcSdkExtensionVersion()) {
            updateMigrationState(MIGRATION_STATE_ALLOWED);
        } else {
            rescheduleCompleteJobIfNoneFound();
        }
    }

    private void rescheduleCompleteJobIfNoneFound() {
        JobInfo jobInfo =
                MigrationStateChangeJob.getPendingJob(
                        mContext, Binder.getCallingUid(), MIGRATION_COMPLETE_JOB_NAME);

        if (Objects.isNull(jobInfo)) {
            MigrationStateChangeJob.scheduleMigrationCompletionJob(mContext, mUserId);
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
            Signature[] packageSignatures =
                    context.getPackageManager()
                            .getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                            .signingInfo
                            .getApkContentsSigners();
            stringSignatures =
                    Arrays.stream(packageSignatures).map(Signature::toCharsString).toList();
        } catch (PackageManager.NameNotFoundException e) {
            Slog.i(TAG, "Could not get package signatures. Package not found");
            return false;
        }

        if (stringSignatures.isEmpty()) {
            return false;
        }
        String[] certs =
                context.getResources()
                        .getStringArray(
                                Resources.getSystem()
                                        .getIdentifier(HC_RELEASE_CERT_CONFIG_NAME, null, null));

        return !Collections.disjoint(stringSignatures, Arrays.stream(certs).toList());
    }

    private int getUdcSdkExtensionVersion() {
        return SdkExtensions.getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE);
    }
}
