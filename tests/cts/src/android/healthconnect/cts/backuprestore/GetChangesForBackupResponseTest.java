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
import android.health.connect.backuprestore.GetChangesForBackupResponse;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class GetChangesForBackupResponseTest {
    @Test
    public void createGetChangesForBackupResponse() {
        BackupChange deletionChange = BackupChange.ofDeletion("changeId1");
        BackupChange upsertionChange =
                BackupChange.ofUpsertion("changeId2", new byte[] {23, 43, 12});
        GetChangesForBackupResponse getChangesForBackupResponse =
                new GetChangesForBackupResponse(2, List.of(deletionChange, upsertionChange), "234");

        assertThat(getChangesForBackupResponse.getCurrentVersion()).isEqualTo(2);
        assertThat(getChangesForBackupResponse.getChanges())
                .isEqualTo(List.of(deletionChange, upsertionChange));
        assertThat(getChangesForBackupResponse.getNextChangeToken()).isEqualTo("234");
    }
}
