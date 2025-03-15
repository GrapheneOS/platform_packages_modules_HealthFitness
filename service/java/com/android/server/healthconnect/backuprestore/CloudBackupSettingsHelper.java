/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.server.healthconnect.backuprestore;

import android.util.Slog;

import com.android.internal.annotations.VisibleForTesting;
import com.android.server.healthconnect.proto.backuprestore.Settings;
import com.android.server.healthconnect.proto.backuprestore.Settings.AppInfo;
import com.android.server.healthconnect.proto.backuprestore.Settings.AutoDeleteFrequencyProto;
import com.android.server.healthconnect.proto.backuprestore.Settings.DistanceUnitProto;
import com.android.server.healthconnect.proto.backuprestore.Settings.EnergyUnitProto;
import com.android.server.healthconnect.proto.backuprestore.Settings.HeightUnitProto;
import com.android.server.healthconnect.proto.backuprestore.Settings.PriorityList;
import com.android.server.healthconnect.proto.backuprestore.Settings.TemperatureUnitProto;
import com.android.server.healthconnect.proto.backuprestore.Settings.WeightUnitProto;
import com.android.server.healthconnect.storage.datatypehelpers.AppInfoHelper;
import com.android.server.healthconnect.storage.datatypehelpers.HealthDataCategoryPriorityHelper;
import com.android.server.healthconnect.storage.datatypehelpers.PreferenceHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Class that manages compiling the user settings into a proto.
 *
 * @hide
 */
public final class CloudBackupSettingsHelper {

    private final HealthDataCategoryPriorityHelper mPriorityHelper;
    private final PreferenceHelper mPreferenceHelper;
    private final AppInfoHelper mAppInfoHelper;

    public static final String TAG = "CloudBackupSettingsHelper";

    public static final String ENERGY_UNIT_PREF_KEY = "ENERGY_UNIT_KEY";
    public static final String TEMPERATURE_UNIT_PREF_KEY = "TEMPERATURE_UNIT_KEY";
    public static final String HEIGHT_UNIT_PREF_KEY = "HEIGHT_UNIT_KEY";
    public static final String WEIGHT_UNIT_PREF_KEY = "WEIGHT_UNIT_KEY";
    public static final String DISTANCE_UNIT_PREF_KEY = "DISTANCE_UNIT_KEY";
    public static final String AUTO_DELETE_PREF_KEY = "auto_delete_range_picker";

    public CloudBackupSettingsHelper(
            HealthDataCategoryPriorityHelper priorityHelper,
            PreferenceHelper preferenceHelper,
            AppInfoHelper appInfoHelper) {
        mPriorityHelper = priorityHelper;
        mPreferenceHelper = preferenceHelper;
        mAppInfoHelper = appInfoHelper;
    }

    /**
     * Collate the user's priority list and unit preferences into a single object.
     *
     * @return the user's settings as a {@code Settings} object
     */
    public Settings collectUserSettings() {
        Settings.Builder builder =
                Settings.newBuilder()
                        .putAllPriorityList(getPriorityList())
                        .putAllAppInfo(getAppInfo())
                        .setAutoDeleteFrequency(getAutoDeleteSetting())
                        .setEnergyUnitSetting(getEnergyPreference())
                        .setTemperatureUnitSetting(getTemperaturePreference())
                        .setHeightUnitSetting(getHeightPreference())
                        .setWeightUnitSetting(getWeightPreference())
                        .setDistanceUnitSetting(getDistancePreference());
        return builder.build();
    }

    /**
     * Override the current settings with the provided new user settings, with the exception of the
     * priority list which should be a merged version of the old and new priority list.
     */
    public void restoreUserSettings(Settings newUserSettings) {
        restoreAppInfo(newUserSettings.getAppInfoMap());
        mergePriorityLists(newUserSettings.getPriorityListMap());
        AutoDeleteFrequencyProto newAutoDeleteFrequency = newUserSettings.getAutoDeleteFrequency();
        if (newAutoDeleteFrequency != AutoDeleteFrequencyProto.AUTO_DELETE_RANGE_UNSPECIFIED
                && newAutoDeleteFrequency != AutoDeleteFrequencyProto.UNRECOGNIZED) {
            mPreferenceHelper.insertOrReplacePreference(
                    AUTO_DELETE_PREF_KEY, newAutoDeleteFrequency.name());
        }
        EnergyUnitProto newEnergyUnit = newUserSettings.getEnergyUnitSetting();
        if (newEnergyUnit != EnergyUnitProto.ENERGY_UNIT_UNSPECIFIED
                && newEnergyUnit != EnergyUnitProto.UNRECOGNIZED) {
            mPreferenceHelper.insertOrReplacePreference(ENERGY_UNIT_PREF_KEY, newEnergyUnit.name());
        }
        TemperatureUnitProto newTemperatureUnit = newUserSettings.getTemperatureUnitSetting();
        if (newTemperatureUnit != TemperatureUnitProto.TEMPERATURE_UNIT_UNSPECIFIED
                && newTemperatureUnit != TemperatureUnitProto.UNRECOGNIZED) {
            mPreferenceHelper.insertOrReplacePreference(
                    TEMPERATURE_UNIT_PREF_KEY, newTemperatureUnit.name());
        }
        HeightUnitProto newHeightUnit = newUserSettings.getHeightUnitSetting();
        if (newHeightUnit != HeightUnitProto.HEIGHT_UNIT_UNSPECIFIED
                && newHeightUnit != HeightUnitProto.UNRECOGNIZED) {
            mPreferenceHelper.insertOrReplacePreference(HEIGHT_UNIT_PREF_KEY, newHeightUnit.name());
        }
        WeightUnitProto newWeightUnit = newUserSettings.getWeightUnitSetting();
        if (newWeightUnit != WeightUnitProto.WEIGHT_UNIT_UNSPECIFIED
                && newWeightUnit != WeightUnitProto.UNRECOGNIZED) {
            mPreferenceHelper.insertOrReplacePreference(WEIGHT_UNIT_PREF_KEY, newWeightUnit.name());
        }
        DistanceUnitProto newDistanceUnit = newUserSettings.getDistanceUnitSetting();
        if (newDistanceUnit != DistanceUnitProto.DISTANCE_UNIT_UNSPECIFIED
                && newDistanceUnit != DistanceUnitProto.UNRECOGNIZED) {
            mPreferenceHelper.insertOrReplacePreference(
                    DISTANCE_UNIT_PREF_KEY, newDistanceUnit.name());
        }
    }

    /**
     * Restores a user's AppInfo settings from the passed in {@code Map<String, AppInfo>} object.
     */
    void restoreAppInfo(Map<String, AppInfo> appInfoMap) {
        for (var appInfoEntry : appInfoMap.entrySet()) {
            String packageName = appInfoEntry.getKey();
            AppInfo appInfo = appInfoEntry.getValue();
            String appName = appInfo.hasAppName() ? appInfo.getAppName() : null;
            mAppInfoHelper.addOrUpdateAppInfoIfNoAppInfoEntryExists(packageName, appName);
        }
    }

    /**
     * Take two priority lists and merge them, removing any duplicate entries, and replace the
     * existing priority list settings with this newly merged version.
     *
     * @param imported the new priority list being restored
     */
    @VisibleForTesting
    void mergePriorityLists(Map<Integer, PriorityList> imported) {
        imported.forEach(
                (category, priorityListProto) -> {
                    var packageNameList = priorityListProto.getPackageNameList();
                    if (packageNameList.isEmpty()) {
                        return;
                    }

                    List<String> currentPriorityList =
                            mAppInfoHelper.getPackageNames(
                                    mPriorityHelper.getAppIdPriorityOrder(category));
                    List<String> newPriorityList =
                            Stream.concat(currentPriorityList.stream(), packageNameList.stream())
                                    .distinct()
                                    .toList();
                    mPriorityHelper.setPriorityOrder(category, newPriorityList);
                    Slog.d(
                            TAG,
                            "Added "
                                    + packageNameList.size()
                                    + " apps to priority list of category "
                                    + category);
                });
    }

    private Map<Integer, PriorityList> getPriorityList() {
        Map<Integer, List<Long>> priorityListMap =
                mPriorityHelper.getHealthDataCategoryToAppIdPriorityMapImmutable();
        if (priorityListMap.isEmpty()) {
            Slog.d(TAG, "Priority list is empty.");
            return Map.of();
        }
        Map<Integer, PriorityList> protoFormattedPriorityList = new HashMap<>();
        priorityListMap.forEach(
                (category, priorityList) -> {
                    protoFormattedPriorityList.put(
                            category,
                            PriorityList.newBuilder()
                                    .addAllPackageName(mAppInfoHelper.getPackageNames(priorityList))
                                    .build());
                });
        return protoFormattedPriorityList;
    }

    Map<String, AppInfo> getAppInfo() {
        Map<String, AppInfo> appInfoMap = new HashMap<>();
        for (var appInfoEntry : mAppInfoHelper.getAppInfoMap().entrySet()) {
            String appName = appInfoEntry.getValue().getName();
            AppInfo.Builder appInfoBuilder = AppInfo.newBuilder();
            if (appName != null) {
                appInfoBuilder.setAppName(appName);
            }
            appInfoMap.putIfAbsent(appInfoEntry.getKey(), appInfoBuilder.build());
        }
        return appInfoMap;
    }

    private AutoDeleteFrequencyProto getAutoDeleteSetting() {
        String preference = mPreferenceHelper.getPreference(AUTO_DELETE_PREF_KEY);
        return preference == null
                ? AutoDeleteFrequencyProto.AUTO_DELETE_RANGE_UNSPECIFIED
                : AutoDeleteFrequencyProto.valueOf(preference);
    }

    private TemperatureUnitProto getTemperaturePreference() {
        String preference = mPreferenceHelper.getPreference(TEMPERATURE_UNIT_PREF_KEY);
        return preference == null
                ? TemperatureUnitProto.TEMPERATURE_UNIT_UNSPECIFIED
                : TemperatureUnitProto.valueOf(preference);
    }

    private EnergyUnitProto getEnergyPreference() {
        String preference = mPreferenceHelper.getPreference(ENERGY_UNIT_PREF_KEY);
        return preference == null
                ? EnergyUnitProto.ENERGY_UNIT_UNSPECIFIED
                : EnergyUnitProto.valueOf(preference);
    }

    private HeightUnitProto getHeightPreference() {
        String preference = mPreferenceHelper.getPreference(HEIGHT_UNIT_PREF_KEY);
        return preference == null
                ? HeightUnitProto.HEIGHT_UNIT_UNSPECIFIED
                : HeightUnitProto.valueOf(preference);
    }

    private WeightUnitProto getWeightPreference() {
        String preference = mPreferenceHelper.getPreference(WEIGHT_UNIT_PREF_KEY);
        return preference == null
                ? WeightUnitProto.WEIGHT_UNIT_UNSPECIFIED
                : WeightUnitProto.valueOf(preference);
    }

    private DistanceUnitProto getDistancePreference() {
        String preference = mPreferenceHelper.getPreference(DISTANCE_UNIT_PREF_KEY);
        return preference == null
                ? DistanceUnitProto.DISTANCE_UNIT_UNSPECIFIED
                : DistanceUnitProto.valueOf(preference);
    }
}
