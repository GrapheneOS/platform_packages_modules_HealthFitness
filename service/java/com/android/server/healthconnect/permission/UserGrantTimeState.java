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

package com.android.server.healthconnect.permission;

import android.annotation.NonNull;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/** State of user health permissions first grant times. Used by {@link FirstGrantTimeDatastore}. */
class UserGrantTimeState {
    /** Special value for {@link #mVersion} to indicate that no version was read. */
    public static final int NO_VERSION = -1;

    /** The first grant times by packages. */
    @NonNull private final Map<String, Instant> mPackagePermissions;

    /** The first grant time of shared users. */
    @NonNull private final Map<String, Instant> mSharedUserPermissions;

    /** The version of the grant times state. */
    private final int mVersion;

    UserGrantTimeState(
            @NonNull Map<String, Instant> packagePermissions,
            @NonNull Map<String, Instant> sharedUserPermissions,
            @NonNull int version) {
        mPackagePermissions = packagePermissions;
        mSharedUserPermissions = sharedUserPermissions;
        mVersion = version;
    }

    @NonNull
    Map<String, Instant> getPackageGrantTimes() {
        return mPackagePermissions;
    }

    @NonNull
    Map<String, Instant> getSharedUserGrantTimes() {
        return mSharedUserPermissions;
    }

    int getVersion() {
        return mVersion;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof UserGrantTimeState)) {
            return false;
        }

        UserGrantTimeState that = (UserGrantTimeState) object;
        return Objects.equals(mPackagePermissions, that.mPackagePermissions)
                && Objects.equals(mSharedUserPermissions, that.mSharedUserPermissions)
                && (mVersion == that.mVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mPackagePermissions, mSharedUserPermissions, mVersion);
    }
}
