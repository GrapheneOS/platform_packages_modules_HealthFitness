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

package android.healthconnect.migration;

import static java.util.Objects.requireNonNull;

import android.annotation.NonNull;
import android.os.Parcel;
import android.os.Parcelable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Holds permission migration data payload.
 *
 * @hide
 */
public final class PermissionMigrationPayload extends MigrationPayload implements Parcelable {

    @NonNull
    public static final Creator<PermissionMigrationPayload> CREATOR =
            new Creator<>() {
                @Override
                public PermissionMigrationPayload createFromParcel(Parcel in) {
                    return new PermissionMigrationPayload(in);
                }

                @Override
                public PermissionMigrationPayload[] newArray(int size) {
                    return new PermissionMigrationPayload[size];
                }
            };

    private final String mHoldingPackageName;
    private final Instant mFirstGrantTime;
    private final List<String> mPermissions;

    private PermissionMigrationPayload(
            @NonNull String holdingPackageName,
            @NonNull Instant firstGrantTime,
            @NonNull List<String> permissions) {
        mHoldingPackageName = holdingPackageName;
        mFirstGrantTime = firstGrantTime;
        mPermissions = permissions;
    }

    private PermissionMigrationPayload(@NonNull Parcel in) {
        mHoldingPackageName = in.readString();
        mFirstGrantTime = in.readSerializable(Instant.class.getClassLoader(), Instant.class);
        mPermissions = in.createStringArrayList();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(mHoldingPackageName);
        dest.writeSerializable(mFirstGrantTime);
        dest.writeStringList(mPermissions);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /** @hide */
    public String getHoldingPackageName() {
        return mHoldingPackageName;
    }

    /** @hide */
    public Instant getFirstGrantTime() {
        return mFirstGrantTime;
    }

    /** @hide */
    public List<String> getPermissions() {
        return mPermissions;
    }

    /** Builder for {@link PermissionMigrationPayload}. */
    public static final class Builder {
        private final List<String> mPermissions = new ArrayList<>();
        private String mHoldingPackageName;
        private Instant mFirstGrantTime;

        /**
         * @param holdingPackageName a package name of an application holding the permissions.
         * @param firstGrantTime {@link Instant} time when the permission was first granted.
         */
        public Builder(@NonNull String holdingPackageName, @NonNull Instant firstGrantTime) {
            requireNonNull(holdingPackageName);
            requireNonNull(firstGrantTime);

            mHoldingPackageName = holdingPackageName;
            mFirstGrantTime = firstGrantTime;
        }

        /** Sets {@code holdingPackageName} to the specified value. */
        @NonNull
        public Builder setHoldingPackageName(@NonNull String holdingPackageName) {
            requireNonNull(holdingPackageName);
            mHoldingPackageName = holdingPackageName;
            return this;
        }

        /** Sets {@code firstGrantTime} to the specified value. */
        @NonNull
        public Builder setFirstGrantTime(@NonNull Instant firstGrantTime) {
            requireNonNull(firstGrantTime);
            mFirstGrantTime = firstGrantTime;
            return this;
        }

        /** Adds the specified {@code permission} to the list. */
        @NonNull
        public Builder addPermission(@NonNull String permission) {
            requireNonNull(permission);
            mPermissions.add(permission);
            return this;
        }

        /**
         * Creates a new instance of {@link PermissionMigrationPayload} with the specified
         * arguments.
         */
        @NonNull
        public PermissionMigrationPayload build() {
            return new PermissionMigrationPayload(
                    mHoldingPackageName, mFirstGrantTime, mPermissions);
        }
    }
}
