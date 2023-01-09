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
package android.healthconnect.internal.datatypes;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.graphics.Bitmap;
import android.healthconnect.datatypes.AppInfo;

/**
 * @hide
 * @see AppInfo
 */
public final class AppInfoInternal {
    private long mId;
    private final String mPackageName;
    private final String mName;
    private final Bitmap mIcon;

    public AppInfoInternal(
            @NonNull long id,
            @NonNull String packageName,
            @Nullable String name,
            @Nullable Bitmap icon) {
        mId = id;
        mPackageName = packageName;
        mName = name;
        mIcon = icon;
    }

    @NonNull
    public long getId() {
        return mId;
    }

    /** returns this object with the specified id */
    @NonNull
    public AppInfoInternal setId(long id) {
        mId = id;
        return this;
    }

    @NonNull
    public String getPackageName() {
        return mPackageName;
    }

    @Nullable
    public String getName() {
        return mName;
    }

    @Nullable
    public Bitmap getIcon() {
        return mIcon;
    }

    /** returns a new {@link AppInfo} object from this object */
    @NonNull
    public AppInfo toExternal() {
        return new AppInfo.Builder(getPackageName(), getName(), getIcon()).build();
    }
}
