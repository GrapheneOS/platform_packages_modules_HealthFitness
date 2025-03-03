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

package android.health.connect.backuprestore;

import static com.android.healthfitness.flags.Flags.FLAG_CLOUD_BACKUP_AND_RESTORE;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

/**
 * Response of the user settings from the Health Connect service to the Health Connect backup agent.
 *
 * @hide
 */
@FlaggedApi(FLAG_CLOUD_BACKUP_AND_RESTORE)
@SystemApi
public final class GetSettingsForBackupResponse implements Parcelable {

    // Version how the data was encoded.
    private final int mVersion;

    @NonNull private final BackupSettings mSettings;

    /**
     * @param version The version of the settings contained in the response, with which the settings
     *     are serialized.
     * @param settings The settings to be backed up.
     */
    public GetSettingsForBackupResponse(int version, @NonNull BackupSettings settings) {
        mVersion = version;
        mSettings = settings;
    }

    private GetSettingsForBackupResponse(Parcel in) {
        mVersion = in.readInt();
        mSettings = in.readParcelable(BackupSettings.class.getClassLoader());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GetSettingsForBackupResponse that)) return false;
        return mVersion == that.mVersion && mSettings.equals(that.mSettings);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(mVersion);
        result = 31 * result + Objects.hash(mSettings);
        return result;
    }

    @NonNull
    public static final Creator<GetSettingsForBackupResponse> CREATOR =
            new Creator<GetSettingsForBackupResponse>() {
                @Override
                public GetSettingsForBackupResponse createFromParcel(Parcel in) {
                    return new GetSettingsForBackupResponse(in);
                }

                @Override
                public GetSettingsForBackupResponse[] newArray(int size) {
                    return new GetSettingsForBackupResponse[size];
                }
            };

    /** Returns the version with which the settings are serialized. */
    @NonNull
    public int getVersion() {
        return mVersion;
    }

    @NonNull
    public BackupSettings getSettings() {
        return mSettings;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mVersion);
        dest.writeParcelable(mSettings, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
