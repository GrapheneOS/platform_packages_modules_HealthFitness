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

package com.android.server.healthconnect.device;

import static android.health.connect.datatypes.Device.DEVICE_TYPE_PHONE;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.util.Slog;

import com.android.internal.annotations.VisibleForTesting;

/**
 * Utility methods related to {@link com.android.server.healthconnect.device.DeviceDataSource}.
 *
 * @hide
 */
public class DeviceDataSourceHelper {
    private static final String TAG = "DeviceDataSourceHelper";
    @VisibleForTesting static final int DISPLAY_NAME_MAX_LENGTH = 128;

    /**
     * Populates {@link com.android.server.healthconnect.device.DeviceDataSource} with details of
     * the current device. The display name is acquired from {@link
     * android.provider.Settings.Global#DEVICE_NAME} with fallbacks provided if that is not
     * available.
     *
     * <p>Note: the device ID for the current device is a sensitive value and should not be shared
     * outside of this module. Normally, reading this identifier requires {@code
     * android.permission.READ_PRIVILEGED_PHONE_STATE}.
     *
     * @return The current device
     */
    public DeviceDataSource getCurrentDevice(Context context) {
        return new DeviceDataSource(
                Build.MANUFACTURER,
                Build.MODEL,
                DEVICE_TYPE_PHONE,
                // This is a sensitive value and should not be shared outside of this module.
                getSerial(),
                getDisplayName(context));
    }

    /**
     * Returns the device serial number.
     *
     * <p>Note: the device ID for the current device is a sensitive value and should not be shared
     * outside of this module. Normally, reading this identifier requires {@code
     * android.permission.READ_PRIVILEGED_PHONE_STATE}.
     *
     * <p>This is extracted to a separate method to allow it to be easily overridden in test cases,
     * and should not be used directly.
     */
    @SuppressLint("MissingPermission")
    @VisibleForTesting
    String getSerial() {
        return Build.getSerial();
    }

    // TODO(b/413650602): update the display name in device info table if/when it changes.
    private static String getDisplayName(Context context) {
        // 1. Try Settings.Global.DEVICE_NAME. This has been available since API 25, and on many
        // devices allows the user to set it to a custom value. There is some possibility for this
        // to be null, but generally it should be set. Fallbacks are provided below for robustness.
        ContentResolver resolver = context.getContentResolver();
        try {
            String deviceName = Settings.Global.getString(resolver, Settings.Global.DEVICE_NAME);
            if (isValidDisplayName(deviceName)) {
                return sanitize(deviceName);
            }
        } catch (Exception e) {
            Slog.e(TAG, "Failed to read Settings.Global.DEVICE_NAME", e);
        }

        // 2. Fallback to Build.MODEL. This is often the commercial name of the device, but
        // sometimes is a codename e.g. sm-####, so is less than ideal.
        String deviceName = Build.MODEL;
        if (isValidDisplayName(deviceName)) {
            return sanitize(deviceName);
        }

        // 3. Fallback of last resort. This should not happen.
        Slog.e(TAG, "Could not determine a valid device name.");
        return "Unknown Device";
    }

    @VisibleForTesting
    static boolean isValidDisplayName(String name) {
        return name != null && !sanitize(name).isEmpty();
    }

    /** Caps the max length of the name and trims trailing/leading whitespace. */
    @VisibleForTesting
    static String sanitize(String name) {
        return name.substring(0, Math.min(name.length(), DISPLAY_NAME_MAX_LENGTH)).trim();
    }
}
