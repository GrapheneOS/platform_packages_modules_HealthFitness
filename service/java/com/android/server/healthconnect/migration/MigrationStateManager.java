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
import android.health.connect.HealthConnectDataState;
import android.health.connect.migration.MigrationException;

import com.android.server.healthconnect.storage.datatypehelpers.PreferenceHelper;

import java.util.Objects;

/**
 * A database operations helper for migration states management.
 *
 * @hide
 */
public final class MigrationStateManager {
    private static MigrationStateManager sMigrationStateManager;
    private static final String MIGRATION_STATE_PREFERENCE_KEY = "migration_state";
    private static final String MIN_DATA_MIGRATION_SDK_EXTENSION_VERSION_KEY =
            "min_data_migration_sdk_extension_version";

    /**
     * @return an initialized instance of this helper.
     */
    @NonNull
    public static MigrationStateManager getInstance() {
        if (sMigrationStateManager == null) {
            sMigrationStateManager = new MigrationStateManager();
        }
        return sMigrationStateManager;
    }

    /**
     * Adds the min data migration sdk and updates the migration state to pending.
     *
     * @param minVersion the desired sdk version.
     */
    public void setMinDataMigrationSdkExtensionVersion(int minVersion) {
        PreferenceHelper.getInstance()
                .insertPreference(
                        MIN_DATA_MIGRATION_SDK_EXTENSION_VERSION_KEY, String.valueOf(minVersion));
    }

    /**
     * @return true when the migration state is in_progress.
     */
    public boolean isMigrationInProgress() {
        return getMigrationState() == MIGRATION_STATE_IN_PROGRESS;
    }

    public void validateStartMigration() {
        throwIfMigrationIsComplete();
    }

    public void validateFinishMigration() {
        throwIfMigrationIsComplete();
        if (getMigrationState() != MIGRATION_STATE_IN_PROGRESS
                && getMigrationState() != MIGRATION_STATE_ALLOWED) {
            throw new MigrationException(
                    MigrationException.ERROR_INTERNAL,
                    "Cannot finish migration. Migration not started.",
                    null);
        }
    }

    public void validateSetMinSdkVersion() {
        throwIfMigrationIsComplete();
        if (getMigrationState() == MIGRATION_STATE_IN_PROGRESS
                && getMigrationState() == MIGRATION_STATE_ALLOWED) {
            throw new MigrationException(
                    MigrationException.ERROR_INTERNAL,
                    "Cannot set the sdk extension version. Migration already in progress.",
                    null);
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
            @HealthConnectDataState.DataMigrationState int migrationState) {
        PreferenceHelper.getInstance()
                .insertPreference(MIGRATION_STATE_PREFERENCE_KEY, String.valueOf(migrationState));
    }

    private void throwIfMigrationIsComplete() {
        if (getMigrationState() == MIGRATION_STATE_COMPLETE) {
            throw new MigrationException(
                    MigrationException.ERROR_INTERNAL, "Migration already marked complete.", null);
        }
    }
}
