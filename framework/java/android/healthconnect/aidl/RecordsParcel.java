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

import android.annotation.NonNull;
import android.healthconnect.internal.datatypes.RecordInternal;
import android.healthconnect.internal.datatypes.utils.ParcelRecordConverter;
import android.os.Parcel;
import android.os.Parcelable;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A wrapper to carry a list of entries of type {@link RecordInternal} from and to {@link
 * android.healthconnect.HealthConnectManager}
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

    private List<RecordInternal<?>> mRecordInternals;
    private byte[] mDataBlob;

    public RecordsParcel(@NonNull List<RecordInternal<?>> recordInternals) {
        mRecordInternals = recordInternals;
    }

    private RecordsParcel(@NonNull Parcel in) {
        this(Objects.requireNonNull(in.readBlob()));
    }

    private RecordsParcel(@NonNull byte[] dataBlob) {
        // Avoid unmarshall here to avoid doing this on a binder thread as much as possible
        mDataBlob = dataBlob;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeBlob(serializeToByteArray());
    }

    @NonNull
    public List<RecordInternal<?>> getRecords()
            throws InvocationTargetException, InstantiationException, IllegalAccessException,
                    NoSuchMethodException {
        if (mDataBlob != null) {
            createRecordsFromParcel();
            mDataBlob = null;
        }
        return mRecordInternals;
    }

    @NonNull
    private byte[] serializeToByteArray() {
        Parcel recordsParcel = Parcel.obtain();
        try {
            // Save the number documents to the temporary Parcel object.
            recordsParcel.writeInt(mRecordInternals.size());
            // Save all document's bundle to the temporary Parcel object.
            for (RecordInternal<?> recordInternal : mRecordInternals) {
                Parcel recordParcel = Parcel.obtain();
                recordInternal.writeToParcel(recordParcel);
                byte[] parcelByte = recordParcel.marshall();
                recordsParcel.writeInt(recordInternal.getRecordType());
                recordsParcel.writeInt(parcelByte.length);
                recordsParcel.writeByteArray(parcelByte);
            }
            return recordsParcel.marshall();
        } finally {
            recordsParcel.recycle();
        }
    }

    private void createRecordsFromParcel()
            throws InvocationTargetException, InstantiationException, IllegalAccessException,
                    NoSuchMethodException {
        Parcel unmarshallParcel = Parcel.obtain();
        try {
            unmarshallParcel.unmarshall(mDataBlob, 0, mDataBlob.length);
            unmarshallParcel.setDataPosition(0);
            // read the number of document that stored in here.
            int size = unmarshallParcel.readInt();
            mRecordInternals = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                int identifier = unmarshallParcel.readInt();
                int parcelSize = unmarshallParcel.readInt();
                Parcel unmarshallParcelRecord = Parcel.obtain();
                byte[] parcel = new byte[parcelSize];
                unmarshallParcel.readByteArray(parcel);
                unmarshallParcelRecord.unmarshall(parcel, 0, parcelSize);
                mRecordInternals.add(
                        ParcelRecordConverter.getInstance()
                                .getRecord(unmarshallParcelRecord, identifier));
            }
        } finally {
            unmarshallParcel.recycle();
        }
    }
}
