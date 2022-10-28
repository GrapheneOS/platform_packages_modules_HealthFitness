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

import android.healthconnect.RecordId;
import android.healthconnect.internal.datatypes.utils.RecordMapper;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

/** @hide */
public final class RecordIdsParcel implements Parcelable {
    public static final Creator<RecordIdsParcel> CREATOR =
            new Creator<RecordIdsParcel>() {
                @Override
                public RecordIdsParcel createFromParcel(Parcel in) {
                    return new RecordIdsParcel(in);
                }

                @Override
                public RecordIdsParcel[] newArray(int size) {
                    return new RecordIdsParcel[size];
                }
            };
    private final List<RecordId> mRecordIds;

    public RecordIdsParcel(List<RecordId> recordIds) {
        mRecordIds = recordIds;
    }

    private RecordIdsParcel(Parcel in) {
        int size = in.readInt();
        mRecordIds = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            mRecordIds.add(
                    new RecordId.Builder(
                                    RecordMapper.getInstance()
                                            .getRecordIdToExternalRecordClassMap()
                                            .get(in.readInt()))
                            .setId(in.readString())
                            .setClientRecordId(in.readString())
                            .build());
        }
    }

    public List<RecordId> getRecordIds() {
        return mRecordIds;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mRecordIds.size());
        mRecordIds.forEach(
                (recordId -> {
                    dest.writeInt(
                            RecordMapper.getInstance().getRecordType(recordId.getRecordType()));
                    dest.writeString(recordId.getId());
                    dest.writeString(recordId.getClientRecordId());
                }));
    }
}
