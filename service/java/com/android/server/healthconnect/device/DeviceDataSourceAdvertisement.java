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

import static com.android.healthfitness.flags.Flags.FLAG_DEVICE_DATA_PROVIDERS_API;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.health.connect.datatypes.Device;

import java.util.Set;

/**
 * A device oriented source of data. This contains metadata about the device, and statuses for each
 * data type supported by the device.
 *
 * <p>The status information is set by the DDP, not Health Connect. This allows the DDP to set them
 * to whichever values make the most sense according to their own logic/rules.
 *
 * @hide
 */
@FlaggedApi(FLAG_DEVICE_DATA_PROVIDERS_API)
@SystemApi
public class DeviceDataSourceAdvertisement {

    private final Device mDevice;
    private final String mDeviceId;
    private final String mDisplayName;
    private final Set<DeviceDataSourceState> mDeviceDataSourceState;

    public DeviceDataSourceAdvertisement(
            @NonNull Device device,
            @NonNull String displayName,
            @NonNull String deviceId,
            @NonNull Set<DeviceDataSourceState> deviceDataSourceState) {
        mDevice = device;
        mDeviceId = deviceId;
        mDisplayName = displayName;
        mDeviceDataSourceState = Set.copyOf(deviceDataSourceState);
    }

    @NonNull
    public Device getDevice() {
        return mDevice;
    }

    public String getDeviceId() {
        return mDeviceId;
    }

    public String getDisplayName() {
        return mDisplayName;
    }

    @NonNull
    public Set<DeviceDataSourceState> getDeviceDataSourceState() {
        return mDeviceDataSourceState;
    }
}
