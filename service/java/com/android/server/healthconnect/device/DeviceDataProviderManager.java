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

import android.annotation.NonNull;
import android.health.connect.datatypes.Device;

import com.android.server.healthconnect.common.metadata.AppInfoHelper;
import com.android.server.healthconnect.common.metadata.DeviceInfoHelper;
import com.android.server.healthconnect.common.metadata.DeviceInfoHelper.DeviceInfo;
import com.android.server.healthconnect.common.metadata.SyntheticPackageNameCreator;
import com.android.server.healthconnect.fitness.helpers.DeviceDataProviderHelper;

import java.util.Objects;
import java.util.Set;

/**
 * Manages device data providers, handling advertisements and updating device, app, and DDP info in
 * the database.
 *
 * @hide
 */
public class DeviceDataProviderManager {

    private final DeviceInfoHelper mDeviceInfoHelper;
    private final AppInfoHelper mAppInfoHelper;
    private final DeviceDataProviderHelper mDeviceDataProviderHelper;

    public DeviceDataProviderManager(
            @NonNull DeviceInfoHelper deviceInfoHelper,
            @NonNull AppInfoHelper appInfoHelper,
            @NonNull DeviceDataProviderHelper deviceDataProviderHelper) {
        mDeviceInfoHelper = Objects.requireNonNull(deviceInfoHelper);
        mAppInfoHelper = Objects.requireNonNull(appInfoHelper);
        mDeviceDataProviderHelper = Objects.requireNonNull(deviceDataProviderHelper);
    }

    /**
     * Handles a {@link DeviceDataSourceAdvertisement}, creating device and app entries if needed,
     * and updating device data provider information.
     *
     * @param advertisements The device data source advertisements.
     * @param ddpPackageName The package name of the advertising DDP.
     */
    // TODO(b/440066697): Check if we want to handle advertisements that are no longer present.
    public void handleAdvertisement(
            @NonNull Set<DeviceDataSourceAdvertisement> advertisements,
            @NonNull String ddpPackageName) {
        Objects.requireNonNull(advertisements);
        Objects.requireNonNull(ddpPackageName);

        for (DeviceDataSourceAdvertisement advertisement : advertisements) {
            handleAdvertisement(advertisement, ddpPackageName);
        }
    }

    private void handleAdvertisement(
            @NonNull DeviceDataSourceAdvertisement advertisement, @NonNull String ddpPackageName) {
        Objects.requireNonNull(advertisement);
        Objects.requireNonNull(ddpPackageName);

        Device device = advertisement.getDevice();
        DeviceInfo deviceInfo =
                new DeviceInfo(
                        device.getManufacturer(),
                        device.getModel(),
                        device.getType(),
                        advertisement.getDeviceId(),
                        advertisement.getDisplayName());
        // TODO(b/440066697): Check how we want to handle display name updates.
        long deviceInfoId = mDeviceInfoHelper.insertIfNotPresent(deviceInfo);
        String spn =
                SyntheticPackageNameCreator.createCanonical(
                        device.getType(), advertisement.getDeviceId());
        // Synthetic package name for device + device info
        mAppInfoHelper.insertDeviceDataSourceIfNotPresent(spn, deviceInfoId);

        // DDP package name + device info + data type + status
        mDeviceDataProviderHelper.insertOrUpdateAdvertisement(
                ddpPackageName, deviceInfoId, advertisement);
    }
}
