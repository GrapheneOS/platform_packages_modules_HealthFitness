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

package android.health.connect.datatypes;

import android.annotation.NonNull;
import android.annotation.Nullable;

import java.time.Instant;
import java.util.Objects;

/** Set of shared metadata fields for {@link Record} */
public final class Metadata {
    private final Device mDevice;
    private final DataOrigin mDataOrigin;
    private final Instant mLastModifiedTime;
    private final String mClientRecordId;
    private final long mClientRecordVersion;
    private String mId;

    /**
     * @param device Optional client supplied device information associated with the data.
     * @param dataOrigin Where the data comes from, such as application information originally
     *     generated this data. When {@link Record} is created before insertion, this contains a
     *     sentinel value, any assigned value will be ignored. After insertion, this will be
     *     populated with inserted application.
     * @param id Unique identifier of this data, assigned by the Android Health Platform at
     *     insertion time. When {@link Record} is created before insertion, this takes a sentinel
     *     value, any assigned value will be ignored.
     * @param lastModifiedTime Automatically populated to when data was last modified (or originally
     *     created). When {@link Record} is created before inserted, this contains a sentinel value,
     *     any assigned value will be ignored.
     * @param clientRecordId Optional client supplied record unique data identifier associated with
     *     the data. There is guaranteed a single entry for any type of data with same client
     *     provided identifier for a given client. Any new insertions with the same client provided
     *     identifier will either replace or be ignored depending on associated {@code
     *     clientRecordVersion}. @see clientRecordVersion
     * @param clientRecordVersion Optional client supplied version associated with the data. This
     *     determines conflict resolution outcome when there are multiple insertions of the same
     *     {@code clientRecordId}. Data with the highest {@code clientRecordVersion} takes
     *     precedence. {@code clientRecordVersion} starts with 0. @see clientRecordId
     */
    private Metadata(
            Device device,
            DataOrigin dataOrigin,
            String id,
            Instant lastModifiedTime,
            String clientRecordId,
            long clientRecordVersion) {
        mDevice = device;
        mDataOrigin = dataOrigin;
        mId = id;
        mLastModifiedTime = lastModifiedTime;
        mClientRecordId = clientRecordId;
        mClientRecordVersion = clientRecordVersion;
    }

    /**
     * @return Client record ID if set, null otherwise
     */
    @Nullable
    public String getClientRecordId() {
        return mClientRecordId;
    }

    /**
     * @return Client record version if set, 0 otherwise
     */
    public long getClientRecordVersion() {
        return mClientRecordVersion;
    }

    /**
     * @return Corresponds to package name if set. If no data origin is set {@code
     *     getDataOrigin().getPackageName()} will return null
     */
    @NonNull
    public DataOrigin getDataOrigin() {
        return mDataOrigin;
    }

    /**
     * @return Record identifier if set, empty string otherwise
     */
    @NonNull
    public String getId() {
        return mId;
    }

    /**
     * Sets record identifier
     *
     * @hide
     */
    public void setId(@NonNull String id) {
        Objects.requireNonNull(id);

        mId = id;
    }

    /**
     * @return Record's last modified time if set, Instant.EPOCH otherwise
     */
    @NonNull
    public Instant getLastModifiedTime() {
        return mLastModifiedTime;
    }

    /**
     * @return The device details that contributed to this record
     */
    @NonNull
    public Device getDevice() {
        return mDevice;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * @param object the reference object with which to compare.
     * @return {@code true} if this object is the same as the obj
     */
    @Override
    public boolean equals(@NonNull Object object) {
        if (this == object) return true;
        if (object instanceof Metadata) {
            Metadata other = (Metadata) object;
            return getDevice().equals(other.getDevice())
                    && getDataOrigin().equals(other.getDataOrigin())
                    && getId().equals(other.getId())
                    && Objects.equals(getClientRecordId(), other.getClientRecordId())
                    && getClientRecordVersion() == other.getClientRecordVersion();
        }
        return false;
    }

    /**
     * Returns a hash code value for the object.
     *
     * @return a hash code value for this object.
     */
    @Override
    public int hashCode() {
        return Objects.hash(
                getDevice(),
                getDataOrigin(),
                getId(),
                getClientRecordId(),
                getClientRecordVersion(),
                getLastModifiedTime());
    }

    /**
     * @see Metadata
     */
    public static final class Builder {
        private Device mDevice = new Device.Builder().build();
        private DataOrigin mDataOrigin = new DataOrigin.Builder().build();
        private String mId = "";
        private Instant mLastModifiedTime = Instant.EPOCH;
        private String mClientRecordId;
        private long mClientRecordVersion = 0;

        public Builder() {}

        /** Sets optional client supplied device information associated with the data. */
        @NonNull
        public Builder setDevice(@NonNull Device device) {
            Objects.requireNonNull(device);

            mDevice = device;
            return this;
        }

        /**
         * Sets where the data comes from, such as application information originally generated this
         * data. When {@link Record} is created before insertion, this contains a sentinel value,
         * any assigned value will be ignored. After insertion, this will be populated with inserted
         * application.
         */
        @NonNull
        public Builder setDataOrigin(@NonNull DataOrigin dataOrigin) {
            Objects.requireNonNull(dataOrigin);

            mDataOrigin = dataOrigin;
            return this;
        }

        /**
         * Sets unique identifier of this data, assigned by the Android Health Platform at insertion
         * time. When {@link Record} is created before insertion, this takes a sentinel value, any
         * assigned value will be ignored.
         */
        @NonNull
        public Builder setId(@NonNull String id) {
            Objects.requireNonNull(id);

            mId = id;
            return this;
        }

        /**
         * Sets when data was last modified (or originally created). When {@link Record} is created
         * before inserted, this contains a sentinel value, any assigned value will be ignored.
         */
        @NonNull
        public Builder setLastModifiedTime(@NonNull Instant lastModifiedTime) {
            Objects.requireNonNull(lastModifiedTime);

            mLastModifiedTime = lastModifiedTime;
            return this;
        }

        /**
         * Sets optional client supplied record unique data identifier associated with the data.
         * There is guaranteed a single entry for any type of data with same client provided
         * identifier for a given client. Any new insertions with the same client provided
         * identifier will either replace or be ignored depending on associated {@code
         * clientRecordVersion}. @see clientRecordVersion
         *
         * <p>A null value means that no clientRecordId is set
         */
        @NonNull
        public Builder setClientRecordId(@Nullable String clientRecordId) {
            mClientRecordId = clientRecordId;
            return this;
        }

        /**
         * Sets optional client supplied version associated with the data. This determines conflict
         * resolution outcome when there are multiple insertions of the same {@code clientRecordId}.
         * Data with the highest {@code clientRecordVersion} takes precedence. {@code
         * clientRecordVersion} starts with 0. @see clientRecordId
         */
        @NonNull
        public Builder setClientRecordVersion(long clientRecordVersion) {
            mClientRecordVersion = clientRecordVersion;
            return this;
        }

        /**
         * @return {@link Metadata} object
         */
        @NonNull
        public Metadata build() {
            return new Metadata(
                    mDevice,
                    mDataOrigin,
                    mId,
                    mLastModifiedTime,
                    mClientRecordId,
                    mClientRecordVersion);
        }
    }
}
