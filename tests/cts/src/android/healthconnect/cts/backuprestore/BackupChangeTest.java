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

package android.healthconnect.cts.backuprestore;

import static com.google.common.truth.Truth.assertThat;

import android.health.connect.backuprestore.BackupChange;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class BackupChangeTest {

    private static final String CHANGE_ID = "change_id";
    private static final byte[] BYTE_ARRAY_DATA = new byte[] {23, 42, 23};

    @Test
    public void createBackupChange_ofUpsertion() {
        BackupChange backupChange = BackupChange.ofUpsertion(CHANGE_ID, BYTE_ARRAY_DATA);

        assertThat(backupChange.getChangeId()).isEqualTo(CHANGE_ID);
        assertThat(backupChange.isDeletion()).isFalse();
        assertThat(backupChange.getData()).isEqualTo(BYTE_ARRAY_DATA);
    }

    @Test
    public void createBackupChange_ofDeletion() {
        BackupChange backupChange = BackupChange.ofDeletion(CHANGE_ID);

        assertThat(backupChange.getChangeId()).isEqualTo(CHANGE_ID);
        assertThat(backupChange.isDeletion()).isTrue();
        assertThat(backupChange.getData()).isNull();
    }
}
