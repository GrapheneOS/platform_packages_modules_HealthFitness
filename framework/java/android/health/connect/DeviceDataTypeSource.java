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

package android.health.connect;

import static com.android.healthfitness.flags.Flags.FLAG_DEVICE_DATA_PROVIDERS_API;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.health.connect.datatypes.Record;
import android.health.connect.internal.datatypes.utils.HealthConnectMappings;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

/** A data type provided by a device. */
@FlaggedApi(FLAG_DEVICE_DATA_PROVIDERS_API)
public final class DeviceDataTypeSource implements Parcelable {
    private final Class<? extends Record> mDataType;
    private final boolean mIsAvailable;
    private final boolean mIsUserEnabled;

    /**
     * @param dataType The data type provided by the device.
     * @param isAvailable Whether this data type is currently available from the device. This may be
     *     false if for example, the device is disconnected or out of range.
     * @param isUserEnabled Whether the user has enabled this data type for the device.
     */
    public DeviceDataTypeSource(
            @NonNull Class<? extends Record> dataType, boolean isAvailable, boolean isUserEnabled) {
        Objects.requireNonNull(dataType);
        mDataType = dataType;
        mIsAvailable = isAvailable;
        mIsUserEnabled = isUserEnabled;
    }

    /** Returns the data type provided by the device. */
    @NonNull
    public Class<? extends Record> getDataType() {
        return mDataType;
    }

    /**
     * Returns whether this data type is currently available from the device.
     *
     * <p>This may be {@code false} if, for example, the device is disconnected or out of range.
     */
    public boolean isAvailable() {
        return mIsAvailable;
    }

    /** Returns whether the user has enabled this data type for the device. */
    public boolean isUserEnabled() {
        return mIsUserEnabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeviceDataTypeSource that)) return false;
        return mIsAvailable == that.mIsAvailable
                && mIsUserEnabled == that.mIsUserEnabled
                && Objects.equals(mDataType, that.mDataType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mDataType, mIsAvailable, mIsUserEnabled);
    }

    @Override
    public String toString() {
        return "DeviceDataTypeSource{"
                + "mDataType="
                + mDataType
                + ", mIsAvailable="
                + mIsAvailable
                + ", mIsUserEnabled="
                + mIsUserEnabled
                + '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(HealthConnectMappings.getInstance().getRecordType(mDataType));
        dest.writeBoolean(mIsAvailable);
        dest.writeBoolean(mIsUserEnabled);
    }

    @NonNull
    public static final Creator<DeviceDataTypeSource> CREATOR =
            new Creator<>() {
                @Override
                public DeviceDataTypeSource createFromParcel(Parcel in) {
                    return new DeviceDataTypeSource(in);
                }

                @Override
                public DeviceDataTypeSource[] newArray(int size) {
                    return new DeviceDataTypeSource[size];
                }
            };

    private DeviceDataTypeSource(Parcel in) {
        mDataType =
                Objects.requireNonNull(
                        HealthConnectMappings.getInstance()
                                .getRecordIdToExternalRecordClassMap()
                                .get(in.readInt()));
        mIsAvailable = in.readBoolean();
        mIsUserEnabled = in.readBoolean();
    }
}
