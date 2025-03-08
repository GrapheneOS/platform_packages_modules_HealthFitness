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

import static com.android.healthfitness.flags.Flags.FLAG_STEP_TRACKING_ENABLED;

import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Unit tests for {@link TrackerManagerImpl} */
@RunWith(AndroidJUnit4.class)
public class TrackerManagerImplTest {

    @Test
    @EnableFlags({FLAG_STEP_TRACKING_ENABLED})
    public void stepTrackingEnabled_initialize_doesNotThrow() {
        TrackerManager manager = new TrackerManagerImpl();
        manager.initialize();
    }

    @Test
    @DisableFlags({FLAG_STEP_TRACKING_ENABLED})
    public void stepTrackingDisabled_initialize_doesNotThrow() {
        TrackerManager manager = new TrackerManagerImpl();
        manager.initialize();
    }

    @Test
    @EnableFlags({FLAG_STEP_TRACKING_ENABLED})
    public void stepTrackingEnabled_setStepTrackingEnabled_doesNotThrow() {
        TrackerManager manager = new TrackerManagerImpl();
        manager.setStepTrackingEnabled(true);
        manager.setStepTrackingEnabled(false);
    }

    @Test
    @DisableFlags({FLAG_STEP_TRACKING_ENABLED})
    public void stepTrackingDisabled_setStepTrackingEnabled_doesNotThrow() {
        TrackerManager manager = new TrackerManagerImpl();
        manager.setStepTrackingEnabled(true);
        manager.setStepTrackingEnabled(false);
    }
}
