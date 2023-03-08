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
import static android.health.connect.HealthConnectDataState.MIGRATION_STATE_COMPLETE;
import static android.health.connect.HealthConnectDataState.MIGRATION_STATE_IDLE;
import static android.health.connect.HealthConnectDataState.MIGRATION_STATE_IN_PROGRESS;

import android.annotation.NonNull;
import android.content.Context;
import android.health.connect.Constants;
import android.health.connect.HealthConnectDataState;
import android.util.Slog;

import com.android.server.healthconnect.HealthConnectThreadScheduler;
import com.android.server.healthconnect.storage.datatypehelpers.PreferenceHelper;

import java.util.Objects;

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
    private volatile MigrationBroadcastScheduler mMigrationBroadcastScheduler;

    /**
     * @return an initialized instance of this helper.
     */
    @NonNull
    public static synchronized MigrationStateManager getInstance() {
        if (sMigrationStateManager == null) {
            sMigrationStateManager = new MigrationStateManager();
        }
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
        PreferenceHelper.getInstance()
                .insertOrReplacePreference(
                        MIN_DATA_MIGRATION_SDK_EXTENSION_VERSION_KEY, String.valueOf(minVersion));
    }

    /**
     * @return true when the migration state is in_progress.
     */
    public boolean isMigrationInProgress() {
        return getMigrationState() == MIGRATION_STATE_IN_PROGRESS;
    }

    /**
     * Throws {@link IllegalMigrationStateException} if the migration can not be started in the
     * current state.
     */
    public void validateStartMigration() throws IllegalMigrationStateException {
        throwIfMigrationIsComplete();
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

    @HealthConnectDataState.DataMigrationState
    public int getMigrationState() {
        String migrationState =
                PreferenceHelper.getInstance().getPreference(MIGRATION_STATE_PREFERENCE_KEY);
        if (Objects.isNull(migrationState)) {
            return MIGRATION_STATE_IDLE;
        }

        return Integer.parseInt(migrationState);
    }

    public void updateMigrationState(
            @HealthConnectDataState.DataMigrationState int migrationState, Context context) {
        PreferenceHelper.getInstance()
                .insertOrReplacePreference(
                        MIGRATION_STATE_PREFERENCE_KEY, String.valueOf(migrationState));

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
    }

    private void throwIfMigrationIsComplete() throws IllegalMigrationStateException {
        if (getMigrationState() == MIGRATION_STATE_COMPLETE) {
            throw new IllegalMigrationStateException("Migration already marked complete.");
        }
    }

    /** Thrown when an illegal migration state is detected. */
    public static final class IllegalMigrationStateException extends Exception {
        public IllegalMigrationStateException(String message) {
            super(message);
        }
    }
}
