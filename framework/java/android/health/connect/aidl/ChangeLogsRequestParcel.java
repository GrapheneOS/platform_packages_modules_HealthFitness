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

package android.health.connect.aidl;

import android.annotation.NonNull;
import android.health.connect.ChangeLogsRequest;
import android.os.Parcel;
import android.os.Parcelable;

/** @hide */
public class ChangeLogsRequestParcel implements Parcelable {
    public static final Creator<ChangeLogsRequestParcel> CREATOR =
            new Creator<ChangeLogsRequestParcel>() {
                @Override
                public ChangeLogsRequestParcel createFromParcel(Parcel in) {
                    return new ChangeLogsRequestParcel(in);
                }

                @Override
                public ChangeLogsRequestParcel[] newArray(int size) {
                    return new ChangeLogsRequestParcel[size];
                }
            };
    private final String mToken;
    private final int mPageSize;

    protected ChangeLogsRequestParcel(Parcel in) {
        mToken = in.readString();
        mPageSize = in.readInt();
    }

    public ChangeLogsRequestParcel(ChangeLogsRequest request) {
        mToken = request.getToken();
        mPageSize = request.getPageSize();
    }

    @NonNull
    public String getToken() {
        return mToken;
    }

    public int getPageSize() {
        return mPageSize;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(mToken);
        dest.writeInt(mPageSize);
    }
}
