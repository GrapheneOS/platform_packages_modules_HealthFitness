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

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * An exception thrown when an error encountered during migration.
 *
 * @hide
 */
public final class MigrationException extends Throwable implements Parcelable {
    @MigrationErrorCode private final int mErrorCode;
    private final String mErrorMessage;
    private final String mFailedEntityId;

    public MigrationException(
            @MigrationErrorCode int errorCode,
            @Nullable String errorMessage,
            @Nullable String failedEntityId) {
        mErrorCode = errorCode;
        mErrorMessage = errorMessage;
        mFailedEntityId = failedEntityId;
    }

    /**
     * Returns the migration error code. Must be one of the values in {@link MigrationErrorCode}.
     */
    @MigrationErrorCode
    public int getErrorCode() {
        return mErrorCode;
    }

    /** Returns an optional error message for this error. */
    @Nullable
    public String getErrorMessage() {
        return mErrorMessage;
    }

    /** Returns an optional id of the first failed entity */
    @Nullable
    public String getFailedEntityId() {
        return mFailedEntityId;
    }

    private MigrationException(@NonNull Parcel in) {
        mErrorCode = in.readInt();
        mErrorMessage = in.readString();
        mFailedEntityId = in.readString();
    }

    @NonNull
    public static final Creator<MigrationException> CREATOR =
            new Creator<>() {
                @Override
                public MigrationException createFromParcel(Parcel in) {
                    return new MigrationException(in);
                }

                @Override
                public MigrationException[] newArray(int size) {
                    return new MigrationException[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mErrorCode);
        dest.writeString(mErrorMessage);
        dest.writeString(mFailedEntityId);
    }

    /** List of possible error codes returned by the migration APIs. */
    public static final int UNKNOWN_ERROR = 0;

    @IntDef({UNKNOWN_ERROR})
    @interface MigrationErrorCode {}
}
