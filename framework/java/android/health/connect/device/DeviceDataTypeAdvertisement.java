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
package android.health.connect.device;

import static com.android.healthfitness.flags.Flags.FLAG_DEVICE_DATA_PROVIDERS_API;

import static java.util.Objects.hash;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.health.connect.HealthConnectManager;
import android.health.connect.datatypes.Record;
import android.health.connect.internal.datatypes.utils.HealthConnectMappings;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

/**
 * Represents information about a data type provided by a device.
 *
 * @hide
 */
@FlaggedApi(FLAG_DEVICE_DATA_PROVIDERS_API)
@SystemApi
public final class DeviceDataTypeAdvertisement implements Parcelable {

    private final Class<? extends Record> mDataType;
    private final boolean mIsAvailable;
    private final boolean mIsUserEnabled;
    private final boolean mIsVisibleByDefaultInMatchmaking;

    /**
     * @param dataType The data type provided by a device
     * @param isAvailable Whether this data type is available. This may change depending on e.g.
     *     Bluetooth enablement or the device being in range.
     * @param isUserEnabled Whether the user has enabled writes for this data type.
     * @param isVisibleByDefaultInMatchmaking Whether this data type should show up in matchmaking.
     *     If false, the device + data type combination for the device will only appear in
     *     matchmaking when explicitly requested by a developer.
     */
    private DeviceDataTypeAdvertisement(
            Class<? extends Record> dataType,
            boolean isAvailable,
            boolean isUserEnabled,
            boolean isVisibleByDefaultInMatchmaking) {
        this.mDataType = dataType;
        this.mIsAvailable = isAvailable;
        this.mIsUserEnabled = isUserEnabled;
        this.mIsVisibleByDefaultInMatchmaking = isVisibleByDefaultInMatchmaking;
    }

    /** The data type provided by a device. */
    @NonNull
    public Class<? extends Record> getDataType() {
        return mDataType;
    }

    /**
     * Whether this data type is available.
     *
     * <p>This may change depending on e.g. Bluetooth enablement or the device being in range.
     */
    public boolean isAvailable() {
        return mIsAvailable;
    }

    /** Whether the user has enabled writes for this data type. */
    public boolean isUserEnabled() {
        return mIsUserEnabled;
    }

    // TODO(b/455559886): Add information about the matchmaking api and intent action to use.
    /**
     * Whether this data type should show up in matchmaking.
     *
     * <p>If {@code true}, this device data type source will appear in matchmaking automatically,
     * see {@link HealthConnectManager#createMatchmakingIntent}.
     *
     * <p>If {@code false}, this device data type source will only appear in matchmaking when
     * explicitly requested by a developer.
     */
    public boolean isVisibleByDefaultInMatchmaking() {
        return mIsVisibleByDefaultInMatchmaking;
    }

    /** Builder for {@link DeviceDataTypeAdvertisement}. */
    public static final class Builder {
        private final Class<? extends Record> mDataType;
        private boolean mIsAvailable = true;
        private boolean mIsUserEnabled = false;
        private boolean mIsVisibleByDefaultInMatchmaking = true;

        /**
         * @param dataType The data type provided by a device. This is a required field.
         */
        public Builder(@NonNull Class<? extends Record> dataType) {
            this.mDataType = dataType;
        }

        /**
         * Sets whether the data type is available. Defaults to {@code true}.
         *
         * <p>This may change depending on e.g. Bluetooth enablement or the device being in range.
         */
        @NonNull
        public Builder setAvailable(boolean isAvailable) {
            this.mIsAvailable = isAvailable;
            return this;
        }

        /**
         * Sets whether the user has enabled writes for this data type. Defaults to {@code false}.
         */
        @NonNull
        public Builder setUserEnabled(boolean isUserEnabled) {
            this.mIsUserEnabled = isUserEnabled;
            return this;
        }

        // TODO(b/455559886): Add information about the matchmaking api and intent action to use.
        /**
         * Sets whether this data type should show up by default in matchmaking. Defaults to {@code
         * true}.
         *
         * <p>If {@code true}, this device data type source will appear in matchmaking
         * automatically.
         *
         * <p>If {@code false}, this device data type source will only appear in matchmaking when
         * explicitly requested by a developer.
         *
         * @see HealthConnectManager#createMatchmakingIntent
         */
        @NonNull
        public Builder setVisibleByDefaultInMatchmaking(boolean isVisibleByDefaultInMatchmaking) {
            this.mIsVisibleByDefaultInMatchmaking = isVisibleByDefaultInMatchmaking;
            return this;
        }

        /**
         * Builds and returns a {@link DeviceDataTypeAdvertisement} with the specified parameters.
         */
        @NonNull
        public DeviceDataTypeAdvertisement build() {
            return new DeviceDataTypeAdvertisement(
                    mDataType, mIsAvailable, mIsUserEnabled, mIsVisibleByDefaultInMatchmaking);
        }
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
        dest.writeBoolean(mIsVisibleByDefaultInMatchmaking);
    }

    @NonNull
    public static final Creator<DeviceDataTypeAdvertisement> CREATOR =
            new Creator<DeviceDataTypeAdvertisement>() {
                @Override
                public DeviceDataTypeAdvertisement createFromParcel(Parcel in) {
                    return new DeviceDataTypeAdvertisement(in);
                }

                @Override
                public DeviceDataTypeAdvertisement[] newArray(int size) {
                    return new DeviceDataTypeAdvertisement[size];
                }
            };

    private DeviceDataTypeAdvertisement(Parcel in) {
        mDataType =
                Objects.requireNonNull(
                        HealthConnectMappings.getInstance()
                                .getRecordIdToExternalRecordClassMap()
                                .get(in.readInt()));
        mIsAvailable = in.readBoolean();
        mIsUserEnabled = in.readBoolean();
        mIsVisibleByDefaultInMatchmaking = in.readBoolean();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeviceDataTypeAdvertisement)) return false;
        DeviceDataTypeAdvertisement that = (DeviceDataTypeAdvertisement) o;
        return mIsAvailable == that.mIsAvailable
                && mIsUserEnabled == that.mIsUserEnabled
                && mIsVisibleByDefaultInMatchmaking == that.mIsVisibleByDefaultInMatchmaking
                && java.util.Objects.equals(mDataType, that.mDataType);
    }

    @Override
    public int hashCode() {
        return hash(mDataType, mIsAvailable, mIsUserEnabled, mIsVisibleByDefaultInMatchmaking);
    }
}
