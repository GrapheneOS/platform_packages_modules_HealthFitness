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

import static com.android.server.healthconnect.common.preferences.PreferencesManager.AUTO_DELETE_DURATION_RECORDS_KEY;

import android.util.Slog;

import com.android.internal.annotations.VisibleForTesting;
import com.android.server.healthconnect.common.metadata.AppInfoHelper;
import com.android.server.healthconnect.common.preferences.PreferenceHelper;
import com.android.server.healthconnect.fitness.helpers.HealthDataCategoryPriorityHelper;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.Settings;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.Settings.AppInfo;
import com.android.server.healthconnect.proto.backuprestore.BackupRestoreProto.Settings.PriorityList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Class that manages compiling the subset of user settings that is supported for backup and restore
 * into a proto.
 *
 * @hide
 */
public final class CloudBackupSettingsHelper {

    private final HealthDataCategoryPriorityHelper mPriorityHelper;
    private final PreferenceHelper mPreferenceHelper;
    private final AppInfoHelper mAppInfoHelper;

    public static final String TAG = "CloudBackupSettingsHelper";

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
                        .putAllAppInfo(getAppInfo())
                        .putAllPriorityList(getPriorityList());
        maybeSetAutoDeleteFrequencyInDays(builder);
        return builder.build();
    }

    /**
     * Override the current settings with the provided new user settings, with the exception of the
     * priority list which should be a merged version of the old and new priority list.
     */
    public void restoreUserSettings(Settings newUserSettings) {
        restoreAppInfo(newUserSettings.getAppInfoMap());
        mergePriorityLists(newUserSettings.getPriorityListMap());
        if (newUserSettings.hasAutoDeleteFrequencyInDays()) {
            mPreferenceHelper.insertOrReplacePreference(
                    AUTO_DELETE_DURATION_RECORDS_KEY,
                    newUserSettings.getAutoDeleteFrequencyInDays());
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
            mAppInfoHelper.restoreAppInfo(packageName, appName);
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

    private void maybeSetAutoDeleteFrequencyInDays(Settings.Builder builder) {
        String preference = mPreferenceHelper.getPreference(AUTO_DELETE_DURATION_RECORDS_KEY);
        if (preference != null) {
            builder.setAutoDeleteFrequencyInDays(preference);
        }
    }
}
