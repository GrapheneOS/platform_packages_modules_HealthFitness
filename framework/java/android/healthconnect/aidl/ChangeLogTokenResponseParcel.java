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
import android.healthconnect.ChangeLogTokenResponse;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

/**
 * Parcel class for {@link ChangeLogTokenResponse}
 *
 * @hide
 */
public class ChangeLogTokenResponseParcel implements Parcelable {
    public static final Creator<ChangeLogTokenResponseParcel> CREATOR =
            new Creator<>() {
                @Override
                public ChangeLogTokenResponseParcel createFromParcel(Parcel in) {
                    return new ChangeLogTokenResponseParcel(in);
                }

                @Override
                public ChangeLogTokenResponseParcel[] newArray(int size) {
                    return new ChangeLogTokenResponseParcel[size];
                }
            };
    private final String mToken;

    protected ChangeLogTokenResponseParcel(Parcel in) {
        mToken = in.readString();
    }

    public ChangeLogTokenResponseParcel(@NonNull String token) {
        Objects.requireNonNull(token);

        mToken = token;
    }

    /** Returns ChangeLogTokenResponse from ChangeLogTokenResponseParcel. */
    public ChangeLogTokenResponse getChangeLogTokenResponse() {
        return new ChangeLogTokenResponse(mToken);
    }

    @NonNull
    public String getToken() {
        return mToken;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(mToken);
    }
}
