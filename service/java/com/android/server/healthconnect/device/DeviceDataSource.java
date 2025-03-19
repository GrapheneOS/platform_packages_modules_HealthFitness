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

import com.android.server.healthconnect.common.metadata.DeviceInfoHelper.DeviceInfo;

/**
 * Represents a source of data originating from a device.
 *
 * @hide
 */
public class DeviceDataSource {
    private final DeviceInfo mDeviceInfo;
    private final String mDeviceId;
    private final String mDisplayName;

    public DeviceDataSource(DeviceInfo deviceInfo, String deviceId, String displayName) {
        this.mDeviceInfo = deviceInfo;
        this.mDeviceId = deviceId;
        this.mDisplayName = displayName;
    }

    public DeviceInfo getDeviceInfo() {
        return mDeviceInfo;
    }

    public String getDeviceId() {
        return mDeviceId;
    }

    public String getDisplayName() {
        return mDisplayName;
    }
}
