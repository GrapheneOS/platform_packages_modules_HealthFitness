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

package com.android.healthfitness.flags;

import static com.android.healthfitness.flags.DatabaseVersions.DB_VERSION_ACTIVITY_INTENSITY;
import static com.android.healthfitness.flags.DatabaseVersions.DB_VERSION_CLOUD_BACKUP_AND_RESTORE;
import static com.android.healthfitness.flags.DatabaseVersions.DB_VERSION_EXERCISE_SEGMENT_IMPROVEMENTS;
import static com.android.healthfitness.flags.DatabaseVersions.DB_VERSION_NICOTINE_INTAKE;
import static com.android.healthfitness.flags.DatabaseVersions.DB_VERSION_PHR_CHANGE_LOGS;
import static com.android.healthfitness.flags.DatabaseVersions.LAST_ROLLED_OUT_DB_VERSION;
import static com.android.internal.annotations.VisibleForTesting.Visibility.PRIVATE;

import com.android.internal.annotations.VisibleForTesting;

import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.BooleanSupplier;

/**
 * A helper class to act as the source of truth for whether a feature is enabled or not by taking
 * into account both feature flag and DB flag. See go/hc-aconfig-and-db.
 *
 * @hide
 */
public final class AconfigFlagHelper {

    private static final DatabaseVersionSupplier sDatabaseVersionSupplier =
            new DatabaseVersionSupplier(LAST_ROLLED_OUT_DB_VERSION, getDbVersionToDbFlagMap());

    /**
     * Returns the DB version based on DB flag values, this DB version is used to initialize {@link
     * android.database.sqlite.SQLiteOpenHelper} to dictate which DB upgrades will be executed.
     */
    public static int getDbVersion() {
        return sDatabaseVersionSupplier.get();
    }

    /**
     * Returns whether the DB flag of a feature is enabled.
     *
     * <p>A DB flag is deemed to be enabled if and only if the DB flag as well as all other features
     * with smaller version numbers have their DB flags enabled.
     *
     * <p>For example, if DB_VERSION_TO_DB_FLAG_MAP contains these:
     *
     * <pre>{@code
     * DB_F1 = true
     * DB_F2 = true
     * DB_F3 = true
     * DB_F4 = false
     * }</pre>
     *
     * Then isDbFlagEnabled(3) will return true and isDbFlagEnabled(4) will return false.
     *
     * <p>In case the map contains a disconnected line of "true"s before the last "false" like this:
     *
     * <pre>{@code
     * DB_F1 = true
     * DB_F2 = false
     * DB_F3 = true
     * DB_F4 = false
     * }</pre>
     *
     * Then isDbFlagEnabled(3) will return false even though DB_F3 is mapped to true.
     *
     * @see #getDbVersion()
     * @see ag/28760234 for example of how to use this method
     */
    private static boolean isDbFlagEnabled(int dbVersion) {
        return getDbVersion() >= dbVersion;
    }

    private AconfigFlagHelper() {}

    // =============================================================================================
    // Only things in below this comment should be updated when we move DB schema changes of a
    // feature from "under development" to "finalized". "finalized" here means the DB schema changes
    // won't be changed again, they will be assigned a DB version and a DB flag, if further changes
    // are required to the DB schema, then new DB version and DB flag are required.
    // =============================================================================================

    /**
     * Returns a map of DB version => DB flag with the DB versions being keys and ordered.
     *
     * <p>Flags values are intentionally not memoized to ensure that tests can change the flag
     * values, for example via the {@code @EnableFlags} annotation.
     */
    // For testing purposes, this field needs to be made public instead of package-private so the
    // unit tests can access it. This is because tests don't run in the same classloader as the
    // framework. See
    // https://groups.google.com/a/google.com/g/android-chatty-eng/c/TymmRzs3UcY/m/_JeFcynRBwAJ.
    @VisibleForTesting(visibility = PRIVATE)
    public static SortedMap<Integer, BooleanSupplier> getDbVersionToDbFlagMap() {
        TreeMap<Integer, BooleanSupplier> map = new TreeMap<>();
        map.put(DB_VERSION_ACTIVITY_INTENSITY, Flags::activityIntensityDb);
        map.put(DB_VERSION_CLOUD_BACKUP_AND_RESTORE, Flags::cloudBackupAndRestoreDb);
        map.put(DB_VERSION_EXERCISE_SEGMENT_IMPROVEMENTS, Flags::exerciseSegmentImprovementsDb);
        map.put(DB_VERSION_PHR_CHANGE_LOGS, Flags::phrChangeLogsDb);
        map.put(DB_VERSION_NICOTINE_INTAKE, Flags::smokingDb);

        return map;
    }

    /** Returns a boolean indicating whether Activity Intensity data type is enabled. */
    public static boolean isActivityIntensityEnabled() {
        return Flags.activityIntensity() && isDbFlagEnabled(DB_VERSION_ACTIVITY_INTENSITY);
    }

    /** Returns a boolean indicating whether cloud backup & restore is enabled. */
    public static boolean isCloudBackupRestoreEnabled() {
        return Flags.cloudBackupAndRestore()
                && isDbFlagEnabled(DB_VERSION_CLOUD_BACKUP_AND_RESTORE);
    }

    /** Returns a boolean indicating whether Nicotine Intake data type is enabled. */
    public static boolean isNicotineIntakeEnabled() {
        return Flags.smoking() && isDbFlagEnabled(DB_VERSION_NICOTINE_INTAKE);
    }

    /** Returns a boolean indicating whether exercise segment improvements are enabled. */
    public static boolean isExerciseSegmentImprovementsEnabled() {
        return Flags.exerciseSegmentImprovements()
                && isDbFlagEnabled(DB_VERSION_EXERCISE_SEGMENT_IMPROVEMENTS);
    }

    /** Returns a boolean indicating whether PHR change logs feature is enabled. */
    public static boolean isPhrChangeLogsEnabled() {
        return Flags.phrChangeLogs() && isDbFlagEnabled(DB_VERSION_PHR_CHANGE_LOGS);
    }

    /** Returns a boolean indicating whether Alcohol Consumption data type is enabled. */
    public static boolean isAlcoholConsumptionEnabled() {
        return Flags.alcoholConsumption() && Flags.alcoholConsumptionDb();
    }
}
