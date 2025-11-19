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

import static android.health.connect.Constants.DEFAULT_LONG;

import android.Manifest;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.health.connect.HealthPermissions;
import android.health.connect.PageTokenWrapper;
import android.health.connect.aidl.DeleteUsingFiltersRequestParcel;
import android.health.connect.aidl.ReadRecordsRequestParcel;
import android.health.connect.datatypes.Device;
import android.health.connect.device.DeviceDataAdvertisement;
import android.health.connect.internal.datatypes.AppInfoInternal;
import android.health.connect.internal.datatypes.RecordInternal;
import android.os.Build;
import android.util.ArrayMap;
import android.util.Pair;
import android.util.Slog;

import com.android.internal.annotations.VisibleForTesting;
import com.android.server.healthconnect.common.metadata.AppInfoHelper;
import com.android.server.healthconnect.common.metadata.DeviceInfoHelper;
import com.android.server.healthconnect.common.metadata.DeviceInfoHelper.DeviceInfo;
import com.android.server.healthconnect.common.metadata.SyntheticPackageNameCreator;
import com.android.server.healthconnect.fitness.FitnessRecordDeleteHelper;
import com.android.server.healthconnect.fitness.FitnessRecordReadHelper;
import com.android.server.healthconnect.fitness.FitnessRecordUpsertHelper;
import com.android.server.healthconnect.fitness.helpers.DeviceDataProviderMetadataHelper;
import com.android.server.healthconnect.fitness.helpers.DeviceDataSourcesHelper;
import com.android.server.healthconnect.fitness.mappings.InternalHealthConnectMappings;
import com.android.server.healthconnect.storage.TransactionManager;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final DeviceDataSourcesHelper mDeviceDataSourcesHelper;
    private final DeviceDataProviderMetadataHelper mDeviceDataProviderMetadataHelper;
    private final FitnessRecordUpsertHelper mFitnessRecordUpsertHelper;
    private final FitnessRecordReadHelper mFitnessRecordReadHelper;
    private final FitnessRecordDeleteHelper mFitnessRecordDeleteHelper;
    private final SyntheticPackageNameCreator mSyntheticPackageNameCreator;

    @Nullable private String mStableCurrentDeviceId;

    // see {@link #getCurrentDeviceId}
    @Nullable private String mRuntimeCurrentDeviceId;

    public DeviceDataProviderManager(
            @NonNull Context context,
            @NonNull DeviceInfoHelper deviceInfoHelper,
            @NonNull AppInfoHelper appInfoHelper,
            @NonNull DeviceDataSourcesHelper deviceDataSourcesHelper,
            @NonNull DeviceDataProviderMetadataHelper deviceDataProviderMetadataHelper,
            @NonNull FitnessRecordUpsertHelper fitnessRecordUpsertHelper,
            @NonNull FitnessRecordReadHelper fitnessRecordReadHelper,
            @NonNull FitnessRecordDeleteHelper fitnessRecordDeleteHelper,
            @NonNull SyntheticPackageNameCreator syntheticPackageNameCreator) {
        mContext = Objects.requireNonNull(context);
        mDeviceInfoHelper = Objects.requireNonNull(deviceInfoHelper);
        mAppInfoHelper = Objects.requireNonNull(appInfoHelper);
        mDeviceDataSourcesHelper = Objects.requireNonNull(deviceDataSourcesHelper);
        mDeviceDataProviderMetadataHelper =
                Objects.requireNonNull(deviceDataProviderMetadataHelper);
        mFitnessRecordUpsertHelper = Objects.requireNonNull(fitnessRecordUpsertHelper);
        mFitnessRecordReadHelper = fitnessRecordReadHelper;
        mFitnessRecordDeleteHelper = Objects.requireNonNull(fitnessRecordDeleteHelper);
        mSyntheticPackageNameCreator = Objects.requireNonNull(syntheticPackageNameCreator);
    }

    /**
     * Handles a {@link DeviceDataAdvertisement}, creating device and app entries if needed, and
     * updating device data provider information.
     *
     * @param advertisements The device data source advertisements.
     * @param callingDdpPackageName The package name of the advertising DDP.
     * @throws IllegalArgumentException if the given deviceId has already been used for a different
     *     device type.
     */
    // TODO(b/440066697): Check if we want to handle advertisements that are no longer present.
    // TODO(b/459404842): Update API documentation with IllegalArgumentException information when
    //  a deviceId is already being used by a different device type and add a CTS test.
    public void handleAdvertisement(
            @NonNull Set<DeviceDataAdvertisement> advertisements,
            @NonNull String callingDdpPackageName) {
        Objects.requireNonNull(advertisements);
        Objects.requireNonNull(callingDdpPackageName);

        List<Long> existingAppInfoIds =
                mDeviceDataSourcesHelper.getAppInfoIds(callingDdpPackageName);
        Set<Long> currentAppInfoIds = new HashSet<>();

        for (DeviceDataAdvertisement advertisement : advertisements) {
            currentAppInfoIds.add(handleAdvertisement(advertisement, callingDdpPackageName));
        }

        for (Long appInfoId : existingAppInfoIds) {
            if (!currentAppInfoIds.contains(appInfoId)) {
                mDeviceDataSourcesHelper.deleteAdvertisements(callingDdpPackageName, appInfoId);
            }
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

    private long handleAdvertisement(
            @NonNull DeviceDataAdvertisement advertisement, @NonNull String callingDdpPackageName) {
        Objects.requireNonNull(advertisement);
        Objects.requireNonNull(callingDdpPackageName);

        Device device = advertisement.getDevice();
        String deviceId = advertisement.getDeviceId();
        int deviceType = device.getType();
        throwIfDeviceIdUsedByDifferentDeviceType(deviceId, deviceType);

        DeviceInfo deviceInfo =
                new DeviceInfo(
                        device.getManufacturer(),
                        device.getModel(),
                        deviceType,
                        deviceId,
                        device.getDisplayName());
        // TODO(b/440066697): Check how we want to handle display name updates.
        long deviceInfoId = mDeviceInfoHelper.insertIfNotPresent(deviceInfo);
        String spn = mSyntheticPackageNameCreator.createCanonical(device.getType(), deviceId);
        // Synthetic package name for device + device info
        long appInfoId = mAppInfoHelper.insertOrUpdateDeviceDataSource(spn, deviceInfoId);

        // DDP package name + app info id + data type + status
        mDeviceDataSourcesHelper.insertOrUpdateAdvertisement(
                callingDdpPackageName, appInfoId, advertisement);

        mDeviceDataProviderMetadataHelper.insertIfNotPresent(callingDdpPackageName);

        return appInfoId;
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
     * @throws IllegalStateException if the generated syntheticPackageName or appInfoId is not valid
     */
    public List<String> insertDeviceRecords(
            @NonNull String callingDdpPackageName,
            @NonNull String deviceId,
            @NonNull List<RecordInternal<?>> records) {
        Objects.requireNonNull(callingDdpPackageName);
        Objects.requireNonNull(deviceId);
        Objects.requireNonNull(records);

        long appInfoId = getOrThrowAppInfoId(callingDdpPackageName, deviceId);
        String syntheticPackageName = getOrThrowSyntheticPackageName(appInfoId);
        populateOrThrowRecords(
                callingDdpPackageName, deviceId, records, syntheticPackageName, appInfoId);

        return mFitnessRecordUpsertHelper.insertRecords(
                syntheticPackageName,
                records,
                EMPTY_EXTRA_PERMISSION_MAPPING,
                /* shouldGenerateAccessLogs= */ false);
    }

    /**
     * Reads and returns a list of records for the given request and previously advertised device,
     * along with the next page token.
     *
     * <p>Make sure to verify permissions with {@link #isPermittedToProvideDeviceData} beforehand.
     *
     * @param transactionManager The TransactionManager to be used to perform this request.
     * @param callingDdpPackageName The name of the device data provider requesting the read.
     * @param request The read request describing what to read.
     * @throws IllegalArgumentException if the parcel is filtering by package names and contains
     *     more than one package filter, or the device with the provided device Id can not be found.
     * @throws IllegalStateException if the generated syntheticPackageName or appInfoId is not valid
     */
    public Pair<List<RecordInternal<?>>, PageTokenWrapper> readDeviceRecords(
            TransactionManager transactionManager,
            String callingDdpPackageName,
            ReadRecordsRequestParcel request) {
        Objects.requireNonNull(transactionManager);
        Objects.requireNonNull(callingDdpPackageName);
        Objects.requireNonNull(request);

        String callingPackageNameForRequest =
                getOrThrowReadDeviceRecordsCaller(callingDdpPackageName, request);
        ReadRecordsRequestParcel requestToUse =
                request.toDdpRequestParcel(callingPackageNameForRequest);

        Pair<Set<String>, Set<String>> allReadPermissions =
                getAllReadPermissionsForRequest(requestToUse);

        Pair<List<RecordInternal<?>>, PageTokenWrapper> result =
                mFitnessRecordReadHelper.readRecords(
                        transactionManager,
                        callingPackageNameForRequest,
                        requestToUse,
                        allReadPermissions.first,
                        allReadPermissions.second,
                        DEFAULT_LONG,
                        /* isForeground= */ true,
                        /* shouldRecordAccessLog= */ false,
                        /* enforceSelfRead= */ !request.getPackageFilters().isEmpty(),
                        /* packageNamesByAppIds= */ null);

        // As multiple DDPs can contribute to the same device, i.e., the same SPN, filter for
        // the records that were actually contributed by the calling DDP
        long callingDdpPackageId =
                mDeviceDataProviderMetadataHelper.getDeviceDataProviderMetadataId(
                        callingDdpPackageName);

        List<RecordInternal<?>> ddpFilteredRecords =
                result.first.stream()
                        .filter(
                                record ->
                                        Objects.equals(
                                                callingDdpPackageId,
                                                record.getDeviceDataProviderId()))
                        .toList();

        return Pair.create(ddpFilteredRecords, result.second);
    }

    /**
     * Updates {@code records} from a device data source in the Health Connect database.
     *
     * @param deviceId The ID of the device.
     * @param records The list of records to update.
     * @return A list of UUIDs of the inserted records.
     * @throws IllegalArgumentException if the device with the given ID is not found
     * @throws IllegalStateException if the generated syntheticPackageName or deviceInfoId is not
     *     valid
     */
    public List<String> updateDeviceRecords(
            @NonNull String callingDdpPackageName,
            @NonNull String deviceId,
            @NonNull List<RecordInternal<?>> records) {
        Objects.requireNonNull(callingDdpPackageName);
        Objects.requireNonNull(deviceId);
        Objects.requireNonNull(records);

        long appInfoId = getOrThrowAppInfoId(callingDdpPackageName, deviceId);
        String syntheticPackageName = getOrThrowSyntheticPackageName(appInfoId);
        populateOrThrowRecords(
                callingDdpPackageName, deviceId, records, syntheticPackageName, appInfoId);

        return mFitnessRecordUpsertHelper.updateRecords(
                syntheticPackageName, records, EMPTY_EXTRA_PERMISSION_MAPPING);
    }

    /**
     * Deletes records associated with a specific device.
     *
     * <p>The given delete request may not contain package name or id filters, as this method is
     * intended to delete records associated with {@code deviceId} only.
     *
     * <p>Note: The device data source must be advertised first through {@link
     * #handleAdvertisement}.
     *
     * @param callingDdpPackageName The package name of the device data provider.
     * @param deviceId The ID of the device.
     * @param request The request containing filters for records to delete.
     * @throws IllegalArgumentException if the parcel has package name filters set, or the device
     *     with the provided device Id can not be found.
     * @throws IllegalStateException if the generated syntheticPackageName or appInfoId is not valid
     */
    public void deleteDeviceRecords(
            String callingDdpPackageName,
            String deviceId,
            DeleteUsingFiltersRequestParcel request) {
        Objects.requireNonNull(callingDdpPackageName);
        Objects.requireNonNull(deviceId);
        Objects.requireNonNull(request);

        verifyDeleteRequestOrThrow(request);

        long appInfoId = getOrThrowAppInfoId(callingDdpPackageName, deviceId);
        String syntheticPackageName = getOrThrowSyntheticPackageName(appInfoId);
        long callingDdpPackageId =
                mDeviceDataProviderMetadataHelper.getDeviceDataProviderMetadataId(
                        callingDdpPackageName);

        mFitnessRecordDeleteHelper.deleteDeviceRecords(
                syntheticPackageName,
                callingDdpPackageId,
                request,
                getAllGranularWritePermissions());
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

    private void throwIfDeviceIdUsedByDifferentDeviceType(String deviceId, int deviceType) {
        Integer existingDeviceType = mDeviceInfoHelper.getDeviceType(deviceId);
        if (existingDeviceType != null && existingDeviceType != deviceType) {
            String message =
                    censoredDeviceMessage(deviceId)
                            + " has already been used for a different device type.";
            Slog.e(TAG, message);
            throw new IllegalArgumentException(message);
        }
    }

    private void populateOrThrowRecords(
            String callingDdpPackageName,
            String deviceId,
            List<RecordInternal<?>> records,
            String syntheticPackageName,
            long appInfoId) {
        AppInfoInternal appInfo = getOrThrowAppInfo(syntheticPackageName);
        long deviceInfoId = Objects.requireNonNull(appInfo.getDeviceInfoId());

        List<Integer> advertisedDataTypes =
                mDeviceDataSourcesHelper.getAdvertisedDataTypes(callingDdpPackageName, appInfoId);

        long deviceDataProviderId =
                mDeviceDataProviderMetadataHelper.getDeviceDataProviderMetadataId(
                        callingDdpPackageName);

        for (RecordInternal<?> record : records) {
            throwIfDataTypeNotAdvertised(advertisedDataTypes, deviceId, record.getRecordType());

            mDeviceInfoHelper.populateRecordWithValue(deviceInfoId, record);
            record.setDeviceInfoId(deviceInfoId);
            record.setPackageName(syntheticPackageName);
            record.setDeviceDataProviderId(deviceDataProviderId);
        }
    }

    /** Get the app info id for the caller and device, or throw if not found */
    @VisibleForTesting
    public long getOrThrowAppInfoId(String callingDdpPackageName, String deviceId) {
        List<Long> appInfoIds = mDeviceDataSourcesHelper.getAppInfoIds(callingDdpPackageName);
        if (appInfoIds.isEmpty()) {
            // TODO(b/459388902): Use the data type string in the exception.
            throw new IllegalArgumentException(
                    "appInfoId not found for calling package "
                            + callingDdpPackageName
                            + ", ensure an advertisement has been made");
        }
        for (Long appInfoId : appInfoIds) {
            String syntheticPackageName = getOrThrowSyntheticPackageName(appInfoId);
            AppInfoInternal appInfo = getOrThrowAppInfo(syntheticPackageName);
            long deviceInfoId = Objects.requireNonNull(appInfo.getDeviceInfoId());
            DeviceInfoHelper.DeviceInfo deviceInfo = mDeviceInfoHelper.getDeviceInfo(deviceInfoId);
            if (deviceInfo != null && deviceId.equals(deviceInfo.getDeviceId())) {
                return appInfoId;
            }
        }

        String message =
                censoredDeviceMessage(deviceId)
                        + " was not found, ensure the device data source has been advertised";
        Slog.e(TAG, message);
        throw new IllegalArgumentException(message);
    }

    /** Acquire the SPN for the given device app info id or throw */
    @VisibleForTesting
    public String getOrThrowSyntheticPackageName(long appInfoId) {
        try {
            return mAppInfoHelper.getPackageName(appInfoId);
        } catch (PackageManager.NameNotFoundException e) {
            throw new IllegalStateException(
                    "syntheticPackageName not found in application_info_table");
        }
    }

    private String getOrThrowReadDeviceRecordsCaller(
            String callingDdpPackageName, ReadRecordsRequestParcel request) {
        // For read requests using IDs we want to read all data, thus we set the caller
        // as empty to mimic an internal call, see {@link FitnessRecordReadHelper#readRecords}
        if (request.getRecordIdFiltersParcel() != null || request.getPackageFilters().isEmpty()) {
            return "";
        }

        // Otherwise, we retrieve the deviceId from the parcel
        if (request.getPackageFilters().size() != 1) {
            throw new IllegalArgumentException(
                    "Read records request must contain exactly one package filter");
        }
        String deviceId = request.getPackageFilters().get(0);
        long appInfoId = getOrThrowAppInfoId(callingDdpPackageName, deviceId);
        return getOrThrowSyntheticPackageName(appInfoId);
    }

    private AppInfoInternal getOrThrowAppInfo(String syntheticPackageName) {
        AppInfoInternal appInfo = mAppInfoHelper.getAppInfoMap().get(syntheticPackageName);
        if (appInfo != null) {
            return appInfo;
        }

        throw new IllegalStateException("syntheticPackageName not found in app_info_table");
    }

    private void throwIfDataTypeNotAdvertised(
            List<Integer> advertisedDataTypes, String deviceId, int recordType) {
        if (!advertisedDataTypes.contains(recordType)) {
            // TODO(b/459388902): Use the data type string in the exception.
            throw new IllegalArgumentException(
                    censoredDeviceMessage(deviceId)
                            + " was not advertised for data type "
                            + recordType);
        }
    }

    private String censoredDeviceMessage(String deviceId) {
        // TODO(b/459541943): Handle censoring of canonical SPN on a higher level
        if (SyntheticPackageNameCreator.isCanonicalSpn(deviceId)) {
            return "The current device";
        } else {
            return "The device with id " + deviceId;
        }
    }

    private Pair<Set<String>, Set<String>> getAllReadPermissionsForRequest(
            ReadRecordsRequestParcel request) {
        Set<String> grantedExtraReadPermissions =
                new HashSet<>(
                        InternalHealthConnectMappings.getInstance()
                                .getRecordHelper(request.getRecordType())
                                .getExtraReadPermissions());

        Set<String> grantedGranularReadPermissions =
                InternalHealthConnectMappings.getInstance()
                        .getRecordHelper(request.getRecordType())
                        .getGranularReadPermissions();

        return new Pair<>(grantedExtraReadPermissions, grantedGranularReadPermissions);
    }

    private Set<String> getAllGranularWritePermissions() {
        // conceptually DDPs operate outside the granular permission system, and thus we give all
        // permissions, regardless of which data types they're actually interacting with
        return InternalHealthConnectMappings.getInstance().getRecordHelpers().stream()
                .flatMap(
                        recordHelper ->
                                recordHelper.getAllGranularWritePermissionsForHelper().stream())
                .collect(Collectors.toSet());
    }

    private void verifyDeleteRequestOrThrow(DeleteUsingFiltersRequestParcel request) {
        if (!request.getPackageNameFilters().isEmpty() || request.usesIdFilters()) {
            throw new IllegalArgumentException(
                    "Package name and ID filters must be empty for device delete requests.");
        }
    }
}
