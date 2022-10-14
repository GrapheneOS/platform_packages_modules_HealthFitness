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

package android.healthconnect.aidl;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

/**
 * A Parcel to carry read request to {@see HealthConnectManager#readRecords}
 *
 * @hide
 */
public class ReadRecordsRequestParcel implements Parcelable {
    public static final Creator<ReadRecordsRequestParcel> CREATOR =
            new Creator<ReadRecordsRequestParcel>() {
                @Override
                public ReadRecordsRequestParcel createFromParcel(Parcel in) {
                    return new ReadRecordsRequestParcel(in);
                }

                @Override
                public ReadRecordsRequestParcel[] newArray(int size) {
                    return new ReadRecordsRequestParcel[size];
                }
            };

    private final RecordIdFiltersParcel mRecordIdFiltersParcel;

    protected ReadRecordsRequestParcel(Parcel in) {
        mRecordIdFiltersParcel =
                in.readParcelable(
                        RecordsParcel.class.getClassLoader(), RecordIdFiltersParcel.class);
    }

    public ReadRecordsRequestParcel(RecordIdFiltersParcel recordIdFiltersParcel) {
        mRecordIdFiltersParcel = recordIdFiltersParcel;
    }

    public RecordIdFiltersParcel getRecordIdFiltersParcel() {
        return mRecordIdFiltersParcel;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeParcelable(mRecordIdFiltersParcel, 0);
    }
}
