/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.server.healthconnect.device.tracker;

/**
 * Provides functionality for native tracking of data types within Health Connect.
 *
 * @hide
 */
public interface TrackerManager {
    /**
     * Called at startup or when tracking may need to start or stop. This will start continuous
     * trackers e.g. step tracking if requirements are met and disable continuous trackers if
     * requirements are no longer met e.g. an app with {@code android.permission.health.READ_STEPS}
     * is uninstalled.
     */
    void initializeOrRefresh();

    /**
     * Resets the state of the trackers, unsubscribing from new sensor events and clearing any
     * cached data or pending tasks.
     */
    void clearTracker();

    /** Is the tracker currently running and subscribed to steps. */
    boolean isStepTrackingActive();

    /** Has the user explicitly turned off step tracking. */
    boolean isStepTrackingExplicitlyDisabled();
}
