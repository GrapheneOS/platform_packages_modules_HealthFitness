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

package android.health.connect.accesslog;

import android.annotation.NonNull;
import android.health.connect.internal.ParcelUtils;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SharedMemory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** @hide */
public final class AccessLogsResponseParcel implements Parcelable {
    @NonNull
    public static final Creator<AccessLogsResponseParcel> CREATOR =
            new Creator<>() {
                @Override
                public AccessLogsResponseParcel createFromParcel(Parcel in) {
                    return new AccessLogsResponseParcel(in);
                }

                @Override
                public AccessLogsResponseParcel[] newArray(int size) {
                    return new AccessLogsResponseParcel[size];
                }
            };

    private final List<AccessLog> mAccessLogsList;

    private AccessLogsResponseParcel(@NonNull Parcel in) {
        int parcelType = in.readInt();
        if (parcelType == ParcelUtils.USING_SHARED_MEMORY) {
            in = ParcelUtils.getParcelForSharedMemory(in);
        }

        mAccessLogsList = new ArrayList<>();
        in.readParcelableList(mAccessLogsList, AccessLog.class.getClassLoader(), AccessLog.class);
    }

    public AccessLogsResponseParcel(@NonNull List<AccessLog> accessLogs) {
        Objects.requireNonNull(accessLogs);

        mAccessLogsList = accessLogs;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        final Parcel dataParcel = Parcel.obtain();
        writeToAccessLogParcel(dataParcel);
        final int dataParcelSize = dataParcel.dataSize();
        if (dataParcelSize > ParcelUtils.KBS_750) {
            SharedMemory sharedMemory =
                    ParcelUtils.getSharedMemoryForParcel(dataParcel, dataParcelSize);
            dest.writeInt(ParcelUtils.USING_SHARED_MEMORY);
            sharedMemory.writeToParcel(dest, flags);
        } else {
            dest.writeInt(ParcelUtils.USING_PARCEL);
            writeToAccessLogParcel(dest);
        }
    }

    private void writeToAccessLogParcel(@NonNull Parcel dest) {
        dest.writeParcelableList(mAccessLogsList, 0);
    }

    @NonNull
    public List<AccessLog> getAccessLogs() {
        return mAccessLogsList;
    }
}
