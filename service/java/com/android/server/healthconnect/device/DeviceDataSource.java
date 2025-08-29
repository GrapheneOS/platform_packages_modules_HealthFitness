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

import android.content.Context;
import android.health.connect.datatypes.Device;

/**
 * Represents a source of data originating from a device.
 *
 * @hide
 */
public class DeviceDataSource {

    private final String mManufacturer;
    private final String mModel;
    @Device.DeviceType private final int mDeviceType;
    private final String mDeviceId;
    private final String mDisplayName;

    public DeviceDataSource(
            String manufacturer,
            String model,
            @Device.DeviceType int deviceType,
            String deviceId,
            String displayName) {
        mManufacturer = manufacturer;
        mModel = model;
        mDeviceType = deviceType;
        this.mDeviceId = deviceId;
        this.mDisplayName = displayName;
    }

    public String getManufacturer() {
        return mManufacturer;
    }

    public String getModel() {
        return mModel;
    }

    public int getDeviceType() {
        return mDeviceType;
    }

    /**
     * Note: this ID may be sensitive, particularly in the case of the current device.
     *
     * <p>See {@link DeviceDataSourcesHelper#getCurrentDevice(Context)}.
     */
    public String getDeviceId() {
        return mDeviceId;
    }

    public String getDisplayName() {
        return mDisplayName;
    }
}
