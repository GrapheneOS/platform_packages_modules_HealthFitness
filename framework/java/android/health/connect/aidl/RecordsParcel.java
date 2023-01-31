/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.health.connect.aidl;

import android.annotation.NonNull;
import android.health.connect.HealthConnectManager;
import android.health.connect.internal.datatypes.RecordInternal;
import android.health.connect.internal.datatypes.utils.ParcelRecordConverter;
import android.os.Parcel;
import android.os.Parcelable;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * A wrapper to carry a list of entries of type {@link RecordInternal} from and to {@link
 * HealthConnectManager}
 *
 * @hide
 */
public class RecordsParcel implements Parcelable {
    @NonNull
    public static final Creator<RecordsParcel> CREATOR =
            new Creator<RecordsParcel>() {
                @Override
                public RecordsParcel createFromParcel(Parcel in) {
                    return new RecordsParcel(in);
                }

                @Override
                public RecordsParcel[] newArray(int size) {
                    return new RecordsParcel[size];
                }
            };

    private final List<RecordInternal<?>> mRecordInternals;

    public RecordsParcel(@NonNull List<RecordInternal<?>> recordInternals) {
        mRecordInternals = recordInternals;
    }

    private RecordsParcel(@NonNull Parcel in) {
        int size = in.readInt();
        mRecordInternals = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            int identifier = in.readInt();
            try {
                mRecordInternals.add(ParcelRecordConverter.getInstance().getRecord(in, identifier));
            } catch (InstantiationException
                    | IllegalAccessException
                    | NoSuchMethodException
                    | InvocationTargetException e) {
                throw new IllegalArgumentException();
            }
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mRecordInternals.size());
        for (RecordInternal<?> recordInternal : mRecordInternals) {
            dest.writeInt(recordInternal.getRecordType());
            recordInternal.writeToParcel(dest);
        }
    }

    @NonNull
    public List<RecordInternal<?>> getRecords() {
        return mRecordInternals;
    }
}
