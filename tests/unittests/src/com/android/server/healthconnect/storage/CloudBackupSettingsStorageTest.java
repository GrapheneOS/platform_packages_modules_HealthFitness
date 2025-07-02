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

import static com.google.common.truth.Truth.assertThat;

import android.healthconnect.testing.unittest.fakes.FakePreferenceHelper;

import org.junit.Before;
import org.junit.Test;

public class CloudBackupSettingsStorageTest {

    private FakePreferenceHelper mFakePreferenceHelper;
    private CloudBackupSettingsStorage mCloudBackupSettingsStorage;

    @Before
    public void setUp() {
        mFakePreferenceHelper = new FakePreferenceHelper();
        mCloudBackupSettingsStorage = new CloudBackupSettingsStorage(mFakePreferenceHelper);
    }

    @Test
    public void testConfigure_backupSettingsLabel() {
        mCloudBackupSettingsStorage.configure("label", "invitation", "account", 1);
        assertThat(
                        mFakePreferenceHelper.getPreference(
                                CloudBackupSettingsStorage.BACKUP_SETTINGS_LABEL_PREFERENCE_KEY))
                .isEqualTo("label");

        // A null value should not overwrite existing values
        mCloudBackupSettingsStorage.configure(null, "new_invitation", "new_account", 2);
        assertThat(
                        mFakePreferenceHelper.getPreference(
                                CloudBackupSettingsStorage.BACKUP_SETTINGS_LABEL_PREFERENCE_KEY))
                .isEqualTo("label");
    }

    @Test
    public void testConfigure_turnOnBackupsInvitation() {
        mCloudBackupSettingsStorage.configure("label", "invitation", "account", 1);
        assertThat(
                        mFakePreferenceHelper.getPreference(
                                CloudBackupSettingsStorage
                                        .TURN_ON_BACKUPS_INVITATION_TEXT_PREFERENCE_KEY))
                .isEqualTo("invitation");

        // A null value should not overwrite existing values
        mCloudBackupSettingsStorage.configure("new_label", null, "new_account", 2);
        assertThat(
                        mFakePreferenceHelper.getPreference(
                                CloudBackupSettingsStorage
                                        .TURN_ON_BACKUPS_INVITATION_TEXT_PREFERENCE_KEY))
                .isEqualTo("invitation");
    }

    @Test
    public void testConfigure_accountName() {
        mCloudBackupSettingsStorage.configure("label", "invitation", "account", 1);
        assertThat(
                        mFakePreferenceHelper.getPreference(
                                CloudBackupSettingsStorage.ACCOUNT_NAME_PREFERENCE_KEY))
                .isEqualTo("account");

        // A null value should not overwrite existing values
        mCloudBackupSettingsStorage.configure("new_label", "new_invitation", null, 2);
        assertThat(
                        mFakePreferenceHelper.getPreference(
                                CloudBackupSettingsStorage.ACCOUNT_NAME_PREFERENCE_KEY))
                .isEqualTo("account");
    }

    @Test
    public void testConfigure_enabledWithUserConsent() {
        mCloudBackupSettingsStorage.configure("label", "invitation", "account", 1);
        assertThat(
                        mFakePreferenceHelper.getPreference(
                                CloudBackupSettingsStorage
                                        .ENABLED_WITH_USER_CONSENT_PREFERENCE_KEY))
                .isEqualTo("1");

        // A null value should not overwrite existing values
        mCloudBackupSettingsStorage.configure("new_label", "new_invitation", "new_account", 0);
        assertThat(
                        mFakePreferenceHelper.getPreference(
                                CloudBackupSettingsStorage
                                        .ENABLED_WITH_USER_CONSENT_PREFERENCE_KEY))
                .isEqualTo("1");
    }
}
