/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.server.healthconnect.logging;

import static android.health.connect.HealthPermissions.WRITE_MEDICAL_DATA;

import android.content.pm.PackageInfo;
import android.health.connect.HealthPermissions;

import androidx.annotation.Nullable;

import com.android.internal.annotations.VisibleForTesting;
import com.android.server.healthconnect.common.accesslog.AccessLogsHelper;
import com.android.server.healthconnect.common.preferences.PreferenceHelper;
import com.android.server.healthconnect.common.preferences.PreferencesManager;
import com.android.server.healthconnect.permission.PackageInfoUtils;
import com.android.server.healthconnect.phr.storage.MedicalDataSourceHelper;
import com.android.server.healthconnect.phr.storage.MedicalResourceHelper;
import com.android.server.healthconnect.storage.HealthConnectContext;
import com.android.server.healthconnect.utils.TimeSource;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Collects Health Connect usage stats.
 *
 * @hide
 */
public final class UsageStatsCollector {

    @VisibleForTesting static final String EXPORT_PERIOD_PREFERENCE_KEY = "export_period_key";

    @VisibleForTesting
    static final String USER_MOST_RECENT_ACCESS_LOG_TIME = "USER_MOST_RECENT_ACCESS_LOG_TIME";

    /**
     * A client is considered as "monthly active" by PHR if it has made any read medical resources
     * API call within this number of days.
     */
    private static final long PHR_MONTHLY_ACTIVE_USER_DURATION = 30; // 30 days

    private static final int NUMBER_OF_DAYS_FOR_USER_TO_BE_MONTHLY_ACTIVE = 30;
    private final HealthConnectContext mHcContext;
    private final PreferenceHelper mPreferenceHelper;
    private final PreferencesManager mPreferencesManager;
    private final AccessLogsHelper mAccessLogsHelper;
    private final MedicalDataSourceHelper mMedicalDataSourceHelper;
    private final MedicalResourceHelper mMedicalResourceHelper;
    private final PackageInfoUtils mPackageInfoUtils;
    private final TimeSource mTimeSource;

    @Nullable private Map<String, PackageInfo> mPackageNameToPackageInfo;

    public UsageStatsCollector(
            HealthConnectContext hcContext,
            PreferenceHelper preferenceHelper,
            PreferencesManager preferencesManager,
            AccessLogsHelper accessLogsHelper,
            TimeSource timeSource,
            MedicalResourceHelper medicalResourceHelper,
            MedicalDataSourceHelper medicalDataSourceHelper,
            PackageInfoUtils packageInfoUtils) {
        mHcContext = hcContext;
        mPreferenceHelper = preferenceHelper;
        mPreferencesManager = preferencesManager;
        mAccessLogsHelper = accessLogsHelper;
        mTimeSource = timeSource;
        mMedicalDataSourceHelper = medicalDataSourceHelper;
        mMedicalResourceHelper = medicalResourceHelper;
        mPackageInfoUtils = packageInfoUtils;
    }

    /**
     * Returns the number of apps that can be connected to Health Connect.
     *
     * <p>The apps not necessarily have permissions to read/write data. It just mentions permission
     * in the manifest i.e. if not connected yet, it can be connected to Health Connect.
     *
     * @return Number of apps that can be connected (not necessarily connected) to Health Connect
     */
    int getNumberOfAppsCompatibleWithHealthConnect() {
        return mPackageInfoUtils
                .getPackagesCompatibleWithHealthConnect(mHcContext, mHcContext.getUser())
                .size();
    }

    /**
     * Returns the list of apps that are connected to Health Connect.
     *
     * @return Map of package name to permissions granted for apps that are connected (have
     *     read/write) to Health Connect
     */
    public Map<String, List<String>> getPackagesHoldingHealthPermissions() {
        Map<String, List<String>> packageNameToPermissionsGranted = new HashMap<>();
        List<PackageInfo> packagesConnectedToHealthConnect =
                mPackageInfoUtils.getPackagesHoldingHealthPermissions(
                        mHcContext.getUser(), mHcContext);
        for (PackageInfo info : packagesConnectedToHealthConnect) {
            List<String> grantedHealthPermissions =
                    PackageInfoUtils.getGrantedHealthPermissions(mHcContext, info);
            if (!grantedHealthPermissions.isEmpty()) {
                packageNameToPermissionsGranted.put(info.packageName, grantedHealthPermissions);
            }
        }
        return packageNameToPermissionsGranted;
    }

    /** Returns whether the current user is considered as a PHR monthly active user. */
    public boolean isPhrMonthlyActiveUser() {
        Instant lastReadMedicalResourcesApiTimeStamp =
                mPreferencesManager.getPhrLastReadMedicalResourcesApiTimeStamp();
        if (lastReadMedicalResourcesApiTimeStamp == null) {
            return false;
        }
        return mTimeSource
                .getInstantNow()
                .minus(PHR_MONTHLY_ACTIVE_USER_DURATION, ChronoUnit.DAYS)
                .isBefore(lastReadMedicalResourcesApiTimeStamp);
    }

    /** Returns the number of clients that are granted at least one PHR <b>read</b> permission. */
    public long getGrantedPhrAppsCount() {
        Map<String, List<String>> packageNameToPermissionsGranted =
                getPackagesHoldingHealthPermissions();
        // note that this set includes WRITE_MEDICAL_DATA
        Set<String> medicalPermissions = HealthPermissions.getAllMedicalPermissions();

        return packageNameToPermissionsGranted.values().stream()
                .map(
                        grantedPermissions -> {
                            for (String grantedPerm : grantedPermissions) {
                                // excluding WRITE_MEDICAL_DATA because we are only counting read
                                // perms
                                if (WRITE_MEDICAL_DATA.equals(grantedPerm)) {
                                    continue;
                                }
                                if (medicalPermissions.contains(grantedPerm)) {
                                    // we just need to find one granted medical perm that is not
                                    // WRITE, hence returning early.
                                    return 1;
                                }
                            }
                            return 0;
                        })
                .filter(count -> count > 0)
                .count();
    }

    /**
     * Returns the configured export frequency of the user.
     *
     * @return Export frequency of the current user.
     */
    int getExportFrequency() {
        String result = mPreferenceHelper.getPreference(EXPORT_PERIOD_PREFERENCE_KEY);
        if (result == null) {
            return 0;
        }
        return Integer.parseInt(result);
    }

    boolean isUserMonthlyActive() {
        String latestAccessLogTimeStampString =
                mPreferenceHelper.getPreference(USER_MOST_RECENT_ACCESS_LOG_TIME);

        // Return false if preference is empty and make sure latest access was within past
        // 30 days.
        return latestAccessLogTimeStampString != null
                && Instant.now()
                                .minus(
                                        NUMBER_OF_DAYS_FOR_USER_TO_BE_MONTHLY_ACTIVE,
                                        ChronoUnit.DAYS)
                                .toEpochMilli()
                        <= Long.parseLong(latestAccessLogTimeStampString);
    }

    void upsertLastAccessLogTimeStamp() {

        long latestAccessLogTimeStamp =
                mAccessLogsHelper.getLatestUpsertOrReadOperationAccessLogTimeStamp();

        // Access logs are only stored for 7 days, therefore only update this value if there is an
        // access log. Last access timestamp can be before 7 days and might already exist in
        // preference and in that case we should not overwrite the existing value.
        if (latestAccessLogTimeStamp != Long.MIN_VALUE) {
            mPreferenceHelper.insertOrReplacePreference(
                    USER_MOST_RECENT_ACCESS_LOG_TIME, String.valueOf(latestAccessLogTimeStamp));
        }
    }

    int getMedicalResourcesCount() {
        return mMedicalResourceHelper.getMedicalResourcesCount();
    }

    int getMedicalDataSourcesCount() {
        return mMedicalDataSourceHelper.getMedicalDataSourcesCount();
    }
}
