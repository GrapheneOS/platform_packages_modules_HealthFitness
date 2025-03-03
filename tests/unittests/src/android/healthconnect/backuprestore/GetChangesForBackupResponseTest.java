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

package android.healthconnect.backuprestore;

import static com.google.common.truth.Truth.assertThat;

import android.health.connect.backuprestore.BackupChange;
import android.health.connect.backuprestore.GetChangesForBackupResponse;
import android.os.Parcel;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class GetChangesForBackupResponseTest {

    @Test
    public void getChangesForBackupResponseParcel_propertiesAreIdentical() {
        GetChangesForBackupResponse original =
                new GetChangesForBackupResponse(
                        123,
                        List.of(
                                new BackupChange("uid123", false, new byte[] {1, 2, 3}),
                                new BackupChange("uid234", false, new byte[] {2, 3, 4}),
                                new BackupChange("uid345", false, new byte[] {3, 4, 5})),
                        "changeToken",
                        new byte[] {12, 34, 56});

        Parcel parcel = Parcel.obtain();
        original.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        GetChangesForBackupResponse restoredParcel =
                GetChangesForBackupResponse.CREATOR.createFromParcel(parcel);

        assertThat(restoredParcel.getVersion()).isEqualTo(123);
        assertThat(restoredParcel.getChanges())
                .containsExactly(
                        new BackupChange("uid123", false, new byte[] {1, 2, 3}),
                        new BackupChange("uid234", false, new byte[] {2, 3, 4}),
                        new BackupChange("uid345", false, new byte[] {3, 4, 5}))
                .inOrder();
        assertThat(restoredParcel.getNextChangeToken()).isEqualTo("changeToken");
        assertThat(restoredParcel.getAppInfoMap()).isEqualTo(new byte[] {12, 34, 56});
        parcel.recycle();
    }
}
