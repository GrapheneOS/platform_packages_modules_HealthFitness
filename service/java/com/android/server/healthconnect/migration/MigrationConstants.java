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

/**
 * Migration related constants.
 *
 * @hide
 */
public class MigrationConstants {
    public static final String EXTRA_USER_ID = "userId";

    // TODO(b/268043064): Customize requiredCount configurations for different states
    public static final int COUNT_MIGRATION_STATE_IDLE = 5;
    public static final int COUNT_MIGRATION_STATE_IN_PROGRESS = 5;
    public static final int COUNT_MIGRATION_STATE_ALLOWED = 5;
    public static final int COUNT_DEFAULT = 0;

    // TODO(b/268043064): Replace these with the fallback hours for each migration state and use a
    // combination of the fallback hours and requiredCount to calculate the interval
    public static final int INTERVAL_MIGRATION_STATE_IDLE = 10000;
    public static final int INTERVAL_MIGRATION_STATE_IN_PROGRESS = 10000;
    public static final int INTERVAL_MIGRATION_STATE_ALLOWED = 10000;
    public static final int INTERVAL_DEFAULT = 0;
}
