/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.server.healthconnect.common.preferences;

import android.annotation.Nullable;
import android.health.connect.Constants;
import android.util.Slog;

import java.time.Instant;

/**
 * Manages entries in {@link PreferenceHelper} which do not belong to a specific class.
 *
 * @hide
 */
public class PreferencesManager {
    private static final String TAG = "HCPreferencesManager";

    public static final String AUTO_DELETE_DURATION_RECORDS_KEY =
            "auto_delete_duration_records_key";

    /**
     * Key to store timestamp of the last time any PHR <b>read medical resources</b> API is called.
     */
    private static final String PREFS_KEY_PHR_LAST_READ_MEDICAL_RESOURCES_API =
            "phr_last_read_medical_resources_api";

    private final PreferenceHelper mPreferenceHelper;

    public PreferencesManager(PreferenceHelper preferenceHelper) {
        mPreferenceHelper = preferenceHelper;
    }

    /** Gets auto delete period for automatically deleting record entries */
    public int getRecordRetentionPeriodInDays() {
        String result = mPreferenceHelper.getPreference(AUTO_DELETE_DURATION_RECORDS_KEY);

        if (result == null) return 0;
        return Integer.parseInt(result);
    }

    /** Sets auto delete period for automatically deleting record entries */
    public void setRecordRetentionPeriodInDays(int days) {
        mPreferenceHelper.insertOrReplacePreference(
                AUTO_DELETE_DURATION_RECORDS_KEY, String.valueOf(days));
    }

    /** Sets timestamp of the last time any PHR <b>read medical resources</b> API is called. */
    public void setLastPhrReadMedicalResourcesApiTimeStamp(Instant instant) {
        mPreferenceHelper.insertOrReplacePreference(
                PREFS_KEY_PHR_LAST_READ_MEDICAL_RESOURCES_API,
                String.valueOf(instant.toEpochMilli()));
    }

    /**
     * Returns timestamp of the last time any PHR <b>read medical resources</b> API is called,
     * <code>null</code> if it's not stored or the stored value is unfetchable for some reason.
     */
    @Nullable
    public Instant getPhrLastReadMedicalResourcesApiTimeStamp() {
        String epochMilliString =
                mPreferenceHelper.getPreference(PREFS_KEY_PHR_LAST_READ_MEDICAL_RESOURCES_API);
        if (epochMilliString == null) {
            return null;
        }
        try {
            long epochMilli = Long.parseLong(epochMilliString);
            return Instant.ofEpochMilli(epochMilli);
        } catch (Exception exception) {
            if (Constants.DEBUG) {
                Slog.e(
                        TAG,
                        "Stored epoch milli for \""
                                + PREFS_KEY_PHR_LAST_READ_MEDICAL_RESOURCES_API
                                + "\" is corrupted, it cannot be converted to an Instant. Its"
                                + "string value is: "
                                + epochMilliString,
                        exception);
            }
            return null;
        }
    }
}
