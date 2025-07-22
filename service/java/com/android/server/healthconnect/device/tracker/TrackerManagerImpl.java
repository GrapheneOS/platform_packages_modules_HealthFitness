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

package com.android.server.healthconnect.device.tracker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.health.connect.HealthDataCategory;
import android.health.connect.HealthPermissions;
import android.os.UserManager;
import android.util.Slog;

import com.android.healthfitness.flags.Flags;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.healthconnect.HealthConnectThreadScheduler;
import com.android.server.healthconnect.common.preferences.PreferenceHelper;
import com.android.server.healthconnect.device.DeviceDataSourcesHelper;
import com.android.server.healthconnect.device.DeviceRecordHelper;
import com.android.server.healthconnect.fitness.helpers.HealthDataCategoryPriorityHelper;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Tracker that subscribes to SensorManager pedometer.
 *
 * @hide
 */
public class TrackerManagerImpl implements TrackerManager {

    private static final String TAG = "HealthConnectTrackerManagerImpl";

    private static final int SAMPLING_PERIOD_US = 60_000_000; // 60 seconds in microseconds
    private static final int MAX_REPORT_LATENCY_US = 60_000_000; // 60 seconds in microseconds

    /**
     * Key that stores if the user has enabled or disabled native tracking for steps.
     *
     * <p>This constant is from the prefix {@code HealthConnectManager#TRACKING_PREFERENCE_PREFIX}
     * and suffix from the {@code RecordTypeIdentifier} for {@code StepsRecord.class}
     *
     * @hide
     */
    private static final String STEP_TRACKING_PREFERENCE_KEY = "TRACKING_PREF_1";

    static final String HAS_DEVICE_PACKAGE_BEEN_APPENDED_TO_PRIORITY_LIST_KEY =
            "has_device_package_been_appended_to_priority_list_key";

    private final Context mContext;
    private final HealthDataCategoryPriorityHelper mHealthDataCategoryPriorityHelper;
    private final UserManager mUserManager;
    private final PackageManager mPackageManager;
    private final PreferenceHelper mPreferenceHelper;

    private boolean mSubscribed = false;
    private Optional<PackageManager.OnPermissionsChangedListener> mPermissionListenerOptional =
            Optional.empty();

    @VisibleForTesting StepSensorEventListener mListener;

    public TrackerManagerImpl(
            Context context,
            HealthConnectThreadScheduler threadScheduler,
            DeviceRecordHelper deviceRecordHelper,
            DeviceDataSourcesHelper deviceDataSourcesHelper,
            HealthDataCategoryPriorityHelper healthDataCategoryPriorityHelper,
            UserManager userManager,
            PreferenceHelper preferenceHelper) {
        mContext = context;
        mHealthDataCategoryPriorityHelper = healthDataCategoryPriorityHelper;
        mListener =
                new StepSensorEventListener(
                        mContext, threadScheduler, deviceRecordHelper, deviceDataSourcesHelper);
        mUserManager = userManager;
        mPackageManager = context.getPackageManager();
        mPreferenceHelper = preferenceHelper;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void initializeOrRefresh() {
        if (!Flags.stepTrackingEnabled()) {
            Slog.d(TAG, "Step tracking flag disabled. Aborting initialization.");
            return;
        }

        if (mPackageManager.hasSystemFeature(PackageManager.FEATURE_WATCH)) {
            // Health Connect runs on Wear for permission management but we don't want to enable
            // passive step tracking for it
            return;
        }

        // Initialization should only be triggered when the user is unlocked.
        if (!mUserManager.isUserUnlocked()) {
            Slog.e(TAG, "User was expected to be unlocked but is not. Aborting initialization.");
            return;
        }

        // Check up front if a step sensor is available. If not, there's no point listening for
        // permission changes.
        if (!isStepSensorAvailable()) {
            Slog.w(TAG, "No step sensor found. Aborting initialization.");
            return;
        }

        refreshTrackerStatus();
    }

    @Override
    public void setStepTrackingEnabled(boolean enabled) {
        if (Flags.stepTrackingEnabled()) {
            // Implementation goes here. Do nothing for now.
        }
    }

    @Override
    public void clearTracker() {
        if (!Flags.stepTrackingEnabled()) {
            return;
        }

        if (mPackageManager.hasSystemFeature(PackageManager.FEATURE_WATCH)) {
            // Health Connect runs on Wear for permission management but we don't want to enable
            // passive step tracking for it
            return;
        }

        unregisterPermissionListener();
        unsubscribeFromSensorManager();
    }

    /** Updates the Sensor Manager subscription in case app permissions have changed. */
    // TODO(b/397419957): Call this when an app is uninstalled in case we want to disable tracking
    private void refreshTrackerStatus() {
        // If a preference has been set and step tracking has been disabled, don't start tracking.
        String stepTrackingPreferenceEnabled =
                mPreferenceHelper.getPreference(STEP_TRACKING_PREFERENCE_KEY);
        if (stepTrackingPreferenceEnabled != null
                && !Boolean.parseBoolean(stepTrackingPreferenceEnabled)) {
            Slog.d(TAG, "Tracking disabled, aborting initialization.");
            unregisterPermissionListener();
            unsubscribeFromSensorManager();
            return;
        }

        if (mPermissionListenerOptional.isEmpty()) {
            mPermissionListenerOptional = Optional.of(this::onPermissionsChanged);
            mPackageManager.addOnPermissionsChangeListener(mPermissionListenerOptional.get());
        }

        if (packagesEligibleForStepTracking(mContext, mPackageManager).isEmpty()) {
            Slog.d(TAG, "No packages eligible for step tracking. Aborting initialization.");
            unsubscribeFromSensorManager();
            return;
        }

        if (!Boolean.parseBoolean(
                mPreferenceHelper.getPreference(
                        HAS_DEVICE_PACKAGE_BEEN_APPENDED_TO_PRIORITY_LIST_KEY))) {
            // Normally, this is carried out whenever an app is granted permissions. Since no
            // permissions are involved for step tracking, we need to do it here.
            // This also has the effect of adding the "android" package to the app info table.
            // Note: this is idempotent and can be called for every initialization.
            Slog.d(TAG, "Adding device data provider package to app priority list.");
            mHealthDataCategoryPriorityHelper.appendToPriorityList(
                    DeviceRecordHelper.DEVICE_DATA_PROVIDER_PACKAGE,
                    HealthDataCategory.ACTIVITY,
                    mContext.getUser());
            mPreferenceHelper.insertOrReplacePreference(
                    HAS_DEVICE_PACKAGE_BEEN_APPENDED_TO_PRIORITY_LIST_KEY, Boolean.toString(true));
        }

        subscribeToSensorManager();
    }

    /**
     * Returns the package names of applications which hold permission {@link
     * HealthPermissions.READ_STEPS}.
     *
     * @return List of app package names which hold the {@link HealthPermissions.READ_STEPS}
     *     permission.
     */
    @VisibleForTesting
    static List<String> packagesEligibleForStepTracking(
            Context context, PackageManager packageManager) {
        if (android.health.connect.Constants.DEBUG) {
            Slog.d(TAG, "Calling packagesEligibleForStepTracking()");
        }

        String[] permissions = new String[] {HealthPermissions.READ_STEPS};
        List<PackageInfo> packageInfos =
                packageManager.getPackagesHoldingPermissions(
                        permissions, PackageManager.PackageInfoFlags.of(0));

        // Get app package names and filter out any system apps pre-granted READ_STEPS as step
        // tracking is initialized for them separately.
        List<String> permissionFilteredPackages =
                packageInfos.stream()
                        .map(info -> info.packageName)
                        .filter(
                                packageName ->
                                        hasUserGrantedStepsPermission(
                                                context, packageName, packageManager))
                        .collect(Collectors.toList());

        if (android.health.connect.Constants.DEBUG) {
            Slog.d(TAG, "permissionFilteredPackages : " + permissionFilteredPackages);
        }

        return permissionFilteredPackages;
    }

    /**
     * Checks that the {@link HealthPermissions.READ_STEPS} permission is not pre-granted by the
     * system for the specified package.
     *
     * <p>The flag {@link PackageManager.FLAG_PERMISSION_GRANTED_BY_DEFAULT} is set for
     * pre-installed apps pre-granted with the permission so if this flag is set, the method will
     * return false.
     */
    private static boolean hasUserGrantedStepsPermission(
            Context context, String packageName, PackageManager packageManager) {
        try {
            @SuppressLint("MissingPermission")
            int flag =
                    packageManager.getPermissionFlags(
                            HealthPermissions.READ_STEPS, packageName, context.getUser());
            boolean isPregrantedPermission =
                    (flag & PackageManager.FLAG_PERMISSION_GRANTED_BY_DEFAULT) != 0;

            if (android.health.connect.Constants.DEBUG && isPregrantedPermission) {
                Slog.d(TAG, "Filtering out pre-granted package : " + packageName);
            }

            return !isPregrantedPermission;
        } catch (IllegalArgumentException e) {
            Slog.e(
                    TAG,
                    "Failed to obtain permission flags. Package likely uninstalled immediately"
                            + " after permission change.",
                    e);
        }
        return false;
    }

    @VisibleForTesting
    void unsubscribeFromSensorManager() {
        if (android.health.connect.Constants.DEBUG) {
            Slog.d(TAG, "Calling unsubscribeFromSensorManager()");
        }

        if (!mSubscribed) {
            return;
        }

        // TODO(b/413703946): Check that the sensor service is always initialised before this call.
        SensorManager sensorManager = mContext.getSystemService(SensorManager.class);
        if (sensorManager == null) {
            Slog.e(TAG, "SensorManager is null");
            return;
        }

        sensorManager.unregisterListener(mListener);
        mListener.reset();
        mSubscribed = false;
    }

    private void subscribeToSensorManager() {
        if (android.health.connect.Constants.DEBUG) {
            Slog.d(TAG, "Calling subscribeToSensorManager()");
        }

        if (mSubscribed) {
            return;
        }

        // TODO(b/413703946): Check that the sensor service is always initialised before this call.
        SensorManager sensorManager = mContext.getSystemService(SensorManager.class);
        if (sensorManager == null) {
            Slog.e(TAG, "SensorManager is null");
            return;
        }

        Sensor stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (stepCounterSensor == null) {
            Slog.d(TAG, "No step sensor found.");
            return;
        }

        boolean subscribeSuccessful =
                sensorManager.registerListener(
                        mListener, stepCounterSensor, SAMPLING_PERIOD_US, MAX_REPORT_LATENCY_US);
        // Flush immediately so that a baseline step count can be set ASAP.
        sensorManager.flush(mListener);

        if (!subscribeSuccessful) {
            Slog.e(TAG, "Failed to subscribe to step sensor");
        }
        mSubscribed = subscribeSuccessful;
    }

    private boolean isStepSensorAvailable() {
        SensorManager sensorManager = mContext.getSystemService(SensorManager.class);
        if (sensorManager == null) {
            return false;
        }
        Sensor stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        return stepCounterSensor != null;
    }

    private void onPermissionsChanged(int uid) {
        try {
            if (android.health.connect.Constants.DEBUG) {
                Slog.d(TAG, "Permissions changed, refreshing tracker status");
            }
            // If tracking wasn't enabled and an app gets the READ_STEPS permission,
            // we'll start tracking. If tracking was enabled and READ_STEPS was revoked
            // for all apps, we'll disable tracking.
            refreshTrackerStatus();
        } catch (RuntimeException e) {
            Slog.e(TAG, "Unhandled failure in permissions change listener", e);
        }
    }

    private void unregisterPermissionListener() {
        mPermissionListenerOptional.ifPresent(mPackageManager::removeOnPermissionsChangeListener);
        mPermissionListenerOptional = Optional.empty();
    }
}
