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

package com.android.server.healthconnect.storage;

import android.annotation.Nullable;

import com.android.server.healthconnect.common.preferences.PreferenceHelper;

/**
 * Stores user settings for the cloud backup and restore.
 *
 * @hide
 */
public final class CloudBackupSettingsStorage {
    public static final String BACKUP_SETTINGS_LABEL_PREFERENCE_KEY = "backup_settings_label_key";
    public static final String TURN_ON_BACKUPS_INVITATION_TEXT_PREFERENCE_KEY =
            "turn_on_backups_invitation_text_key";
    public static final String ACCOUNT_NAME_PREFERENCE_KEY = "account_name_key";
    public static final String ENABLED_WITH_USER_CONSENT_PREFERENCE_KEY =
            "enabled_with_user_consent_key";

    private final PreferenceHelper mPreferenceHelper;

    public CloudBackupSettingsStorage(PreferenceHelper preferenceHelper) {
        mPreferenceHelper = preferenceHelper;
    }

    /**
     * Configures the settings for the cloud backup of Health Connect data.
     *
     * @param backupSettingsLabel Translated label for the UI item in Health Connect settings that
     *     the user can click to change their backup settings.
     * @param turnOnBackupsInvitationText Translated text used in Health Connect Settings to
     *     encourage users who don't have backups enabled to enable cloud backups.
     * @param accountName Account name currently used for backups.
     * @param enabledWithUserConsent Indicates whether the user enabled cloud backups.
     */
    public void configure(
            @Nullable String backupSettingsLabel,
            @Nullable String turnOnBackupsInvitationText,
            @Nullable String accountName,
            int enabledWithUserConsent) {
        if (backupSettingsLabel != null) {
            mPreferenceHelper.insertOrReplacePreference(
                    BACKUP_SETTINGS_LABEL_PREFERENCE_KEY, backupSettingsLabel);
        }

        if (turnOnBackupsInvitationText != null) {
            mPreferenceHelper.insertOrReplacePreference(
                    TURN_ON_BACKUPS_INVITATION_TEXT_PREFERENCE_KEY, turnOnBackupsInvitationText);
        }

        if (accountName != null) {
            mPreferenceHelper.insertOrReplacePreference(ACCOUNT_NAME_PREFERENCE_KEY, accountName);
        }

        // TODO: b/427454317 Use BackupRestoreEnabledSetting when ag/34205235 is submitted
        if (enabledWithUserConsent != 0) {
            mPreferenceHelper.insertOrReplacePreference(
                    ENABLED_WITH_USER_CONSENT_PREFERENCE_KEY,
                    String.valueOf(enabledWithUserConsent));
        }
    }
}
