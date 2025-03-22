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
package android.healthconnect.cts.utils;

import android.content.Context;
import android.content.pm.PackageManager;

import androidx.test.core.app.ApplicationProvider;

/**
 * Helper class to check if HealthConnect is supported on this device, to enable tests to be skipped
 * if not.
 */
public class DeviceSupportUtils {

    /** Returns true if HealthConnect is fully supported on this device. */
    public static boolean isHealthConnectFullySupported() {
        return isHealthConnectFullySupported(ApplicationProvider.getApplicationContext());
    }

    /** Returns true if HealthConnect is fully supported using information from this context. */
    public static boolean isHealthConnectFullySupported(Context context) {
        PackageManager pm = context.getPackageManager();
        return (!pm.hasSystemFeature(PackageManager.FEATURE_EMBEDDED)
                && !pm.hasSystemFeature(PackageManager.FEATURE_WATCH)
                && !pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
                && !pm.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE));
    }
}
