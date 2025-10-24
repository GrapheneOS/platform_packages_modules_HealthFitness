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

import android.Manifest;
import android.annotation.NonNull;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.health.connect.HealthPermissions;
import android.health.connect.datatypes.Device;
import android.health.connect.internal.datatypes.AppInfoInternal;
import android.health.connect.internal.datatypes.RecordInternal;
import android.os.Build;
import android.util.ArrayMap;
import android.util.Slog;

import com.android.server.healthconnect.common.metadata.AppInfoHelper;
import com.android.server.healthconnect.common.metadata.DeviceInfoHelper;
import com.android.server.healthconnect.common.metadata.DeviceInfoHelper.DeviceInfo;
import com.android.server.healthconnect.common.metadata.SyntheticPackageNameCreator;
import com.android.server.healthconnect.fitness.FitnessRecordUpsertHelper;
import com.android.server.healthconnect.fitness.helpers.DeviceDataProviderHelper;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Manages device data providers, handling advertisements and updating device, app, and DDP info in
 * the database.
 *
 * @hide
 */
public class DeviceDataProviderManager {

    private static final String TAG = "DeviceDataProviderManager";
    private static final ArrayMap<String, Boolean> EMPTY_EXTRA_PERMISSION_MAPPING =
            new ArrayMap<>();

    private final Context mContext;
    private final DeviceInfoHelper mDeviceInfoHelper;
    private final AppInfoHelper mAppInfoHelper;
    private final DeviceDataProviderHelper mDeviceDataProviderHelper;
    private final FitnessRecordUpsertHelper mFitnessRecordUpsertHelper;

    public DeviceDataProviderManager(
            @NonNull Context context,
            @NonNull DeviceInfoHelper deviceInfoHelper,
            @NonNull AppInfoHelper appInfoHelper,
            @NonNull DeviceDataProviderHelper deviceDataProviderHelper,
            @NonNull FitnessRecordUpsertHelper fitnessRecordUpsertHelper) {
        mContext = context;
        mDeviceInfoHelper = Objects.requireNonNull(deviceInfoHelper);
        mAppInfoHelper = Objects.requireNonNull(appInfoHelper);
        mDeviceDataProviderHelper = Objects.requireNonNull(deviceDataProviderHelper);
        mFitnessRecordUpsertHelper = Objects.requireNonNull(fitnessRecordUpsertHelper);
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

    /**
     * Inserts a list of records associated with a specific device.
     *
     * <p>Note: The device data source must be advertised first through {@link
     * #handleAdvertisement}.
     *
     * @param deviceId The ID of the device.
     * @param records The list of records to insert.
     * @return A list of UUIDs of the inserted records.
     * @throws IllegalArgumentException if the device with the given ID is not found
     * @throws IllegalStateException if the generated syntheticPackageName or deviceInfoId is not
     *     valid
     */
    public List<String> insertDeviceRecords(
            @NonNull String deviceId, @NonNull List<RecordInternal<?>> records) {
        Objects.requireNonNull(deviceId);
        Objects.requireNonNull(records);
        DeviceInfoHelper.DeviceInfo deviceInfo = mDeviceInfoHelper.getDeviceInfo(deviceId);
        if (deviceInfo == null) {
            Slog.e(
                    TAG,
                    "Device with ID "
                            + deviceId
                            + " not found, ensure the device data source has been advertised");
            throw new IllegalArgumentException(
                    "Device with ID "
                            + deviceId
                            + " not found, ensure the device data source has been advertised");
        }

        String syntheticPackageName =
                SyntheticPackageNameCreator.createCanonical(deviceInfo.getDeviceType(), deviceId);
        long deviceInfoId = getOrThrowDeviceInfoId(deviceInfo, syntheticPackageName);

        for (RecordInternal<?> record : records) {
            mDeviceInfoHelper.populateRecordWithValue(deviceInfoId, record);
            record.setDeviceInfoId(deviceInfoId);
            record.setPackageName(syntheticPackageName);
        }

        return mFitnessRecordUpsertHelper.insertRecords(
                syntheticPackageName,
                records,
                EMPTY_EXTRA_PERMISSION_MAPPING,
                /* shouldGenerateAccessLogs= */ false);
    }

    private long getOrThrowDeviceInfoId(DeviceInfo deviceInfo, String syntheticPackageName) {
        AppInfoInternal appInfo = mAppInfoHelper.getAppInfoMap().get(syntheticPackageName);
        if (appInfo == null) {
            throw new IllegalStateException(
                    "syntheticPackageName not found in application_info_table");
        }
        Long deviceInfoIdFromAppInfoDb = appInfo.getDeviceInfoId();
        Long deviceInfoIdFromDeviceInfoDb = mDeviceInfoHelper.getDeviceInfoId(deviceInfo);
        if (deviceInfoIdFromAppInfoDb == null) {
            throw new IllegalStateException("deviceInfoId not found in application_info_table");
        }
        if (deviceInfoIdFromDeviceInfoDb == null) {
            throw new IllegalStateException("deviceInfoId not found in device_info_table");
        }
        // Check that the inferred synthetic package name is the correct one for the deviceId
        if (!deviceInfoIdFromDeviceInfoDb.equals(deviceInfoIdFromAppInfoDb)) {
            throw new IllegalStateException(
                    "deviceInfoId in device_info_table does not match application_info_table");
        }
        return deviceInfoIdFromDeviceInfoDb;
    }

    /**
     * Checks whether a package is permitted to act as a device data provider.
     *
     * <p>On Android versions after Baklava where {@code
     * android.permission.PROVIDE_HEALTH_CONNECT_DEVICE_DATA} is available, this will return {@code
     * true} when that permission is held by the calling package.
     *
     * <p>On Android versions Baklava or earlier, which predate the device data provider permission,
     * alternative checks will be made, returning {@code true} for either of the following:
     *
     * <ul>
     *   <li>Is the package listed in config_systemActivityRecognizer
     *   <li>Does the package hold android.permission.MANAGE_HEALTH_DATA
     * </ul>
     *
     * <p>See b/315116545 for details of these fallback checks.
     */
    public boolean isPermittedToProvideDeviceData(
            @NonNull String callingPackageName, int uid, int pid) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.BAKLAVA) {
            // For details of this fallback see b/315116545
            final int resourceId =
                    Resources.getSystem()
                            .getIdentifier("config_systemActivityRecognizer", "string", "android");
            if (resourceId != 0
                    && callingPackageName.equals(Resources.getSystem().getString(resourceId))) {
                return true;
            }

            // This fallback caters primarily for test environments as the shell holds this
            // permission from Android U upwards.
            return mContext.checkPermission(
                            HealthPermissions.MANAGE_HEALTH_DATA_PERMISSION, pid, uid)
                    == PackageManager.PERMISSION_GRANTED;
        } else {
            return mContext.checkPermission(
                            Manifest.permission.PROVIDE_HEALTH_CONNECT_DEVICE_DATA, pid, uid)
                    == PackageManager.PERMISSION_GRANTED;
        }
    }
}
