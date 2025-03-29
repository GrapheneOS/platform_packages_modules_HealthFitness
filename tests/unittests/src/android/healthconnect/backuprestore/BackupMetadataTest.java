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

import android.health.connect.backuprestore.BackupMetadata;
import android.os.Parcel;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class BackupMetadataTest {

    @Test
    public void backupMetadataParcel_propertiesAreIdentical() {
        BackupMetadata original = new BackupMetadata(new byte[] {1, 2, 3});

        Parcel parcel = Parcel.obtain();
        original.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        BackupMetadata restoredParcel = BackupMetadata.CREATOR.createFromParcel(parcel);

        assertThat(restoredParcel.getData()).isEqualTo(new byte[] {1, 2, 3});
        parcel.recycle();
    }
}
