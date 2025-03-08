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

package com.android.server.healthconnect.tracker;

/**
 * Provides functionality for native tracking of data types within Health Connect.
 *
 * @hide
 */
public interface TrackerManager {
    /**
     * Called at startup. This will start continuous trackers e.g. step tracking if requirements are
     * met.
     */
    void initialize();

    /**
     * Explicitly enable/disable step tracking. This should be done when a connected app has been
     * granted {@code android.permission.health.READ_STEPS}.
     */
    void setStepTrackingEnabled(boolean enabled);
}
