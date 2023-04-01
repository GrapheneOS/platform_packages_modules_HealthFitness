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

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Migration related constants.
 *
 * @hide
 */
public class MigrationConstants {
    public static final String EXTRA_USER_ID = "userId";

    static final int COUNT_MIGRATION_STATE_IDLE = 5;
    static final int COUNT_MIGRATION_STATE_IN_PROGRESS = 5;
    static final int COUNT_MIGRATION_STATE_ALLOWED = 5;
    static final int COUNT_DEFAULT = 0;

    static final int MAX_START_MIGRATION_CALLS_ALLOWED = 3;

    // TODO(b/270562874) Refactor the timeouts to be server-side configurable.
    static final Duration IDLE_STATE_TIMEOUT_PERIOD = Duration.ofDays(30);
    static final Duration NON_IDLE_STATE_TIMEOUT_PERIOD = Duration.ofDays(15);
    static final Duration IN_PROGRESS_STATE_TIMEOUT_PERIOD = Duration.ofHours(12);
    static final long EXECUTION_TIME_BUFFER = TimeUnit.MINUTES.toMillis(30);
    static final long MIGRATION_COMPLETION_JOB_RUN_INTERVAL = TimeUnit.DAYS.toMillis(1);
    static final long MIGRATION_PAUSE_JOB_RUN_INTERVAL = TimeUnit.HOURS.toMillis(4);
    static final int INTERVAL_DEFAULT = 0;

    static final String HC_RELEASE_CERT_CONFIG_NAME =
            "android:array/config_healthConnectMigrationKnownSigners";
    public static final String HC_PACKAGE_NAME_CONFIG_NAME =
            "android:string/config_healthConnectMigratorPackageName";
    public static final String MIGRATION_COMPLETE_JOB_NAME = "migration_completion_job";
    public static final String MIGRATION_PAUSE_JOB_NAME = "migration_pause_job";
    static final String CURRENT_STATE_START_TIME_KEY = "current_state_start_time";
    static final String ALLOWED_STATE_TIMEOUT_KEY = "allowed_state_timeout_key";
    static final String MIGRATION_STATE_PREFERENCE_KEY = "migration_state";
    static final String MIN_DATA_MIGRATION_SDK_EXTENSION_VERSION_KEY =
            "min_data_migration_sdk_extension_version";
    static final String MIGRATION_STARTS_COUNT_KEY = "migration_starts_count";
    static final String HAVE_CANCELED_OLD_MIGRATION_JOBS_KEY = "have_canceled_old_migration_jobs";
    static final String MIGRATION_STATE_CHANGE_NAMESPACE = MigrationStateChangeJob.class.toString();
    static final boolean ENABLE_STATE_CHANGE_JOBS = true;
}
