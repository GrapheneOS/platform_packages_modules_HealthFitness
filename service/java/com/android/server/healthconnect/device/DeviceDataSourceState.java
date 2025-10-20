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
package com.android.server.healthconnect.device;

import static com.android.healthfitness.flags.Flags.FLAG_DEVICE_DATA_PROVIDERS_API;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.health.connect.datatypes.Record;

/**
 * Represents the state of a device data source for a specific data type.
 *
 * @hide
 */
@FlaggedApi(FLAG_DEVICE_DATA_PROVIDERS_API)
@SystemApi
public final class DeviceDataSourceState {

    private final Class<? extends Record> mDataType;
    private final boolean mIsAvailable;
    private final boolean mIsUserEnabled;
    private final boolean mIsVisibleByDefaultInMatchmaking;

    /**
     * @param dataType The data type this state represents.
     * @param isAvailable Whether the device is available. This may change depending on e.g.
     *     Bluetooth enablement or the device being in range.
     * @param isUserEnabled Whether the user has enabled this data type. This is set by the DDP and
     *     is used for informational purposes in the UI.
     * @param isVisibleByDefaultInMatchmaking Whether the device data source should show up by
     *     default in app matchmaking.
     */
    private DeviceDataSourceState(
            Class<? extends Record> dataType,
            boolean isAvailable,
            boolean isUserEnabled,
            boolean isVisibleByDefaultInMatchmaking) {
        this.mDataType = dataType;
        this.mIsAvailable = isAvailable;
        this.mIsUserEnabled = isUserEnabled;
        this.mIsVisibleByDefaultInMatchmaking = isVisibleByDefaultInMatchmaking;
    }

    /**
     * Returns the data type.
     *
     * @hide
     */
    @NonNull
    public Class<? extends Record> getDataType() {
        return mDataType;
    }

    /**
     * Returns whether the device is available.
     *
     * @hide
     */
    public boolean isAvailable() {
        return mIsAvailable;
    }

    /**
     * Returns whether the user has enabled this data type.
     *
     * @hide
     */
    public boolean isUserEnabled() {
        return mIsUserEnabled;
    }

    /**
     * Returns whether the device data source should show up by default in app matchmaking.
     *
     * @hide
     */
    public boolean isVisibleByDefaultInMatchmaking() {
        return mIsVisibleByDefaultInMatchmaking;
    }

    /**
     * Builder for {@link DeviceDataSourceState}.
     *
     * @hide
     */
    public static final class Builder {
        private final Class<? extends Record> mDataType;
        private boolean mIsAvailable = false;
        private boolean mIsUserEnabled = false;
        private boolean mIsVisibleByDefaultInMatchmaking = false;

        /**
         * @param dataType The data type for which the state is being built. This is a required
         *     field.
         */
        public Builder(Class<? extends Record> dataType) {
            this.mDataType = dataType;
        }

        /** Sets whether the device is available. Defaults to false. */
        public Builder setAvailable(boolean isAvailable) {
            this.mIsAvailable = isAvailable;
            return this;
        }

        /** Sets whether the user has enabled this data type. Defaults to false. */
        public Builder setUserEnabled(boolean isUserEnabled) {
            this.mIsUserEnabled = isUserEnabled;
            return this;
        }

        /**
         * Sets whether the device data source should show up by default in app matchmaking.
         * Defaults to false.
         */
        public Builder setVisibleByDefaultInMatchmaking(boolean isVisibleByDefaultInMatchmaking) {
            this.mIsVisibleByDefaultInMatchmaking = isVisibleByDefaultInMatchmaking;
            return this;
        }

        /** Builds and returns a {@link DeviceDataSourceState} with the specified parameters. */
        public DeviceDataSourceState build() {
            return new DeviceDataSourceState(
                    mDataType, mIsAvailable, mIsUserEnabled, mIsVisibleByDefaultInMatchmaking);
        }
    }
}
