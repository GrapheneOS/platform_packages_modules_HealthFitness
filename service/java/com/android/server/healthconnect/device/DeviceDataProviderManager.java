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
import android.annotation.Nullable;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.health.connect.HealthPermissions;
import android.health.connect.datatypes.Device;
import android.health.connect.device.DeviceDataAdvertisement;
import android.health.connect.internal.datatypes.AppInfoInternal;
import android.health.connect.internal.datatypes.RecordInternal;
import android.os.Build;
import android.util.ArrayMap;
import android.util.Slog;

import com.android.internal.annotations.VisibleForTesting;
import com.android.server.healthconnect.common.metadata.AppInfoHelper;
import com.android.server.healthconnect.common.metadata.DeviceInfoHelper;
import com.android.server.healthconnect.common.metadata.DeviceInfoHelper.DeviceInfo;
import com.android.server.healthconnect.common.metadata.SyntheticPackageNameCreator;
import com.android.server.healthconnect.fitness.FitnessRecordUpsertHelper;
import com.android.server.healthconnect.fitness.helpers.DeviceDataProviderHelper;
import com.android.server.healthconnect.fitness.helpers.DeviceDataProviderMetadataHelper;

import java.security.SecureRandom;
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
    private final DeviceDataProviderMetadataHelper mDeviceDataProviderMetadataHelper;
    private final FitnessRecordUpsertHelper mFitnessRecordUpsertHelper;
    private final SyntheticPackageNameCreator mSyntheticPackageNameCreator;

    @Nullable private String mStableCurrentDeviceId;

    // see {@link #getCurrentDeviceId}
    @Nullable private String mRuntimeCurrentDeviceId;

    public DeviceDataProviderManager(
            @NonNull Context context,
            @NonNull DeviceInfoHelper deviceInfoHelper,
            @NonNull AppInfoHelper appInfoHelper,
            @NonNull DeviceDataProviderHelper deviceDataProviderHelper,
            @NonNull DeviceDataProviderMetadataHelper deviceDataProviderMetadataHelper,
            @NonNull FitnessRecordUpsertHelper fitnessRecordUpsertHelper,
            @NonNull SyntheticPackageNameCreator syntheticPackageNameCreator) {
        mContext = Objects.requireNonNull(context);
        mDeviceInfoHelper = Objects.requireNonNull(deviceInfoHelper);
        mAppInfoHelper = Objects.requireNonNull(appInfoHelper);
        mDeviceDataProviderHelper = Objects.requireNonNull(deviceDataProviderHelper);
        mDeviceDataProviderMetadataHelper =
                Objects.requireNonNull(deviceDataProviderMetadataHelper);
        mFitnessRecordUpsertHelper = Objects.requireNonNull(fitnessRecordUpsertHelper);
        mSyntheticPackageNameCreator = Objects.requireNonNull(syntheticPackageNameCreator);
    }

    /**
     * Handles a {@link DeviceDataAdvertisement}, creating device and app entries if needed, and
     * updating device data provider information.
     *
     * @param advertisements The device data source advertisements.
     * @param callingDdpPackageName The package name of the advertising DDP.
     */
    // TODO(b/440066697): Check if we want to handle advertisements that are no longer present.
    public void handleAdvertisement(
            @NonNull Set<DeviceDataAdvertisement> advertisements,
            @NonNull String callingDdpPackageName) {
        Objects.requireNonNull(advertisements);
        Objects.requireNonNull(callingDdpPackageName);

        for (DeviceDataAdvertisement advertisement : advertisements) {
            handleAdvertisement(advertisement, callingDdpPackageName);
        }
    }

    /**
     * Creates two unique identifiers if non-existent: One persisted, internal ID stored in the
     * database, and a temporary ID that resets when the device is restarted. The internal ID is
     * constructed using the device serial number and a persisted masking salt.
     *
     * <p>Note: the serial number for the current device is a sensitive value and requires {@code
     * android.permission.READ_PRIVILEGED_PHONE_STATE} to read.
     *
     * <p>This method is called at device startup, as the generated ID is required by {@link
     * #getCurrentDeviceId}.
     */
    public void initializeOrRefreshCurrentDeviceIds() {
        mStableCurrentDeviceId =
                mSyntheticPackageNameCreator.createCanonical(Device.DEVICE_TYPE_PHONE, getSerial());
        mAppInfoHelper.addAppInfoIfNoAppInfoEntryExists(mStableCurrentDeviceId, null);

        String runtimeIdentifierSeed = String.valueOf(new SecureRandom().nextInt());
        mRuntimeCurrentDeviceId =
                mSyntheticPackageNameCreator.createCanonical(
                        Device.DEVICE_TYPE_PHONE, runtimeIdentifierSeed);
    }

    /**
     * Retrieves the randomly generated identifier for the current device. This ID is reset upon
     * each device boot.
     */
    @NonNull
    public String getCurrentDeviceId() throws IllegalStateException {
        if (mRuntimeCurrentDeviceId == null) {
            throw new IllegalStateException(
                    "Current device id has not been initialized yet.Ensure to call"
                            + " initializeOrRefreshCurrentDeviceIds before calling this method.");
        }
        return mRuntimeCurrentDeviceId;
    }

    /**
     * Retrieves the internal, unique identifier for the current device. This ID is persisted across
     * restarts and only resets when the device is factory reset.
     */
    @NonNull
    public String getStableCurrentDeviceId() {
        if (mStableCurrentDeviceId == null) {
            throw new IllegalStateException(
                    "Current device id has not been initialized yet.Ensure to call"
                            + " initializeOrRefreshCurrentDeviceIds before calling this method.");
        }
        return mStableCurrentDeviceId;
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

    private void handleAdvertisement(
            @NonNull DeviceDataAdvertisement advertisement, @NonNull String callingDdpPackageName) {
        Objects.requireNonNull(advertisement);
        Objects.requireNonNull(callingDdpPackageName);

        Device device = advertisement.getDevice();
        DeviceInfo deviceInfo =
                new DeviceInfo(
                        device.getManufacturer(),
                        device.getModel(),
                        device.getType(),
                        advertisement.getDeviceId(),
                        device.getDisplayName());
        // TODO(b/440066697): Check how we want to handle display name updates.
        long deviceInfoId = mDeviceInfoHelper.insertIfNotPresent(deviceInfo);
        String spn =
                mSyntheticPackageNameCreator.createCanonical(
                        device.getType(), advertisement.getDeviceId());
        // Synthetic package name for device + device info
        mAppInfoHelper.insertDeviceDataSourceIfNotPresent(spn, deviceInfoId);

        // DDP package name + device info + data type + status
        mDeviceDataProviderHelper.insertOrUpdateAdvertisement(
                callingDdpPackageName, deviceInfoId, advertisement);

        mDeviceDataProviderMetadataHelper.insertIfNotPresent(callingDdpPackageName);
    }

    /**
     * Inserts a list of records associated with a specific device.
     *
     * <p>Note: The device data source must be advertised first through {@link
     * #handleAdvertisement}.
     *
     * @param callingDdpPackageName The package name of the device data provider.
     * @param deviceId The ID of the device.
     * @param records The list of records to insert.
     * @return A list of UUIDs of the inserted records.
     * @throws IllegalArgumentException if the device with the given ID is not found or if any
     *     record type is not advertised.
     * @throws IllegalStateException if the generated syntheticPackageName or deviceInfoId is not
     *     valid
     */
    public List<String> insertDeviceRecords(
            @NonNull String callingDdpPackageName,
            @NonNull String deviceId,
            @NonNull List<RecordInternal<?>> records) {
        Objects.requireNonNull(callingDdpPackageName);
        Objects.requireNonNull(deviceId);
        Objects.requireNonNull(records);
        DeviceInfoHelper.DeviceInfo deviceInfo = mDeviceInfoHelper.getDeviceInfo(deviceId);
        if (deviceInfo == null) {
            String message =
                    censoredDeviceMessage(deviceId)
                            + " was not found, ensure the device data source has been advertised";
            Slog.e(TAG, message);
            throw new IllegalArgumentException(message);
        }

        String syntheticPackageName =
                mSyntheticPackageNameCreator.createCanonical(deviceInfo.getDeviceType(), deviceId);
        long deviceInfoId = getOrThrowDeviceInfoId(deviceInfo, syntheticPackageName);

        List<Integer> advertisedDataTypes =
                mDeviceDataProviderHelper.getAdvertisedDataTypes(
                        callingDdpPackageName, deviceInfoId);
        for (RecordInternal<?> record : records) {
            if (!advertisedDataTypes.contains(record.getRecordType())) {
                // TODO(b/459388902): Use the data type string in the exception.
                throw new IllegalArgumentException(
                        censoredDeviceMessage(deviceId)
                                + " was not advertised for data type "
                                + record.getRecordType());
            }
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

    @NonNull
    private String censoredDeviceMessage(@NonNull String deviceId) {
        // TODO(b/459541943): Handle censoring of canonical SPN on a higher level
        if (SyntheticPackageNameCreator.isCanonicalSpn(deviceId)) {
            return "The current device";
        } else {
            return "The device with id " + deviceId;
        }
    }
}
