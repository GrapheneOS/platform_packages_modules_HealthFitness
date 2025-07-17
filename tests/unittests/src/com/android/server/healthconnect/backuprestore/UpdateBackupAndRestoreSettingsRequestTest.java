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

package android.health.connect.backuprestore;

import static android.health.connect.backuprestore.UpdateBackupAndRestoreSettingsRequest.BACKUPS_ENABLED_TRUE;

import static com.android.healthfitness.flags.Flags.FLAG_CLOUD_BACKUP_AND_RESTORE_INTENT_API;

import static com.google.common.truth.Truth.assertThat;

import android.os.Parcel;
import android.platform.test.annotations.EnableFlags;

import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@EnableFlags(FLAG_CLOUD_BACKUP_AND_RESTORE_INTENT_API)
public class UpdateBackupAndRestoreSettingsRequestTest {

    @Test
    @EnableFlags(FLAG_CLOUD_BACKUP_AND_RESTORE_INTENT_API)
    public void testBuilder_setsAllFields() {
        UpdateBackupAndRestoreSettingsRequest request =
                new UpdateBackupAndRestoreSettingsRequest.Builder()
                        .setBackupSettingsLabel("label")
                        .setTurnOnBackupsInvitationText("invitation")
                        .setAccountName("account")
                        .setBackupsEnabledState(BACKUPS_ENABLED_TRUE)
                        .build();

        assertThat(request.getBackupSettingsLabel()).isEqualTo("label");
        assertThat(request.getTurnOnBackupsInvitationText()).isEqualTo("invitation");
        assertThat(request.getAccountName()).isEqualTo("account");
        assertThat(request.getEnabledWithUserConsent()).isEqualTo(BACKUPS_ENABLED_TRUE);
    }

    @Test
    @EnableFlags(FLAG_CLOUD_BACKUP_AND_RESTORE_INTENT_API)
    public void testParcelable_writesAndReadsCorrectly() {
        UpdateBackupAndRestoreSettingsRequest originalRequest =
                new UpdateBackupAndRestoreSettingsRequest.Builder()
                        .setBackupSettingsLabel("label")
                        .setTurnOnBackupsInvitationText("invitation")
                        .setAccountName("account")
                        .setBackupsEnabledState(BACKUPS_ENABLED_TRUE)
                        .build();

        Parcel parcel = Parcel.obtain();
        originalRequest.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        UpdateBackupAndRestoreSettingsRequest newRequest =
                UpdateBackupAndRestoreSettingsRequest.CREATOR.createFromParcel(parcel);
        parcel.recycle();

        assertThat(newRequest).isEqualTo(originalRequest);
    }

    @Test
    @EnableFlags(FLAG_CLOUD_BACKUP_AND_RESTORE_INTENT_API)
    public void testEqualsAndHashCode() {
        UpdateBackupAndRestoreSettingsRequest request1 =
                new UpdateBackupAndRestoreSettingsRequest.Builder()
                        .setBackupSettingsLabel("label")
                        .setTurnOnBackupsInvitationText("invitation")
                        .setAccountName("account")
                        .setBackupsEnabledState(BACKUPS_ENABLED_TRUE)
                        .build();

        UpdateBackupAndRestoreSettingsRequest request2 =
                new UpdateBackupAndRestoreSettingsRequest.Builder()
                        .setBackupSettingsLabel("label")
                        .setTurnOnBackupsInvitationText("invitation")
                        .setAccountName("account")
                        .setBackupsEnabledState(BACKUPS_ENABLED_TRUE)
                        .build();

        UpdateBackupAndRestoreSettingsRequest request3 =
                new UpdateBackupAndRestoreSettingsRequest.Builder()
                        .setBackupSettingsLabel("different_label")
                        .setTurnOnBackupsInvitationText("invitation")
                        .setAccountName("account")
                        .setBackupsEnabledState(BACKUPS_ENABLED_TRUE)
                        .build();

        assertThat(request1).isEqualTo(request2);
        assertThat(request1.hashCode()).isEqualTo(request2.hashCode());
        assertThat(request1).isNotEqualTo(request3);
        assertThat(request1.hashCode()).isNotEqualTo(request3.hashCode());
    }
}
