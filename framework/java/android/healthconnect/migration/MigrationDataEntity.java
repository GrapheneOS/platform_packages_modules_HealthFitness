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

import android.annotation.NonNull;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * An entity containing data or info being migrated.
 *
 * @hide
 */
public final class MigrationDataEntity implements Parcelable {
    private final int mTypeId;
    private final String mEntityId;
    private final Bundle mPayload;

    public MigrationDataEntity(int typeId, @NonNull String entityId, @NonNull Bundle payload) {
        mTypeId = typeId;
        mEntityId = entityId;
        mPayload = payload;
    }

    /**
     * Returns an integer representing type ID of the entity such as permission, data point,
     * application record, etc.
     */
    @NonNull
    public int getTypeId() {
        return mTypeId;
    }

    /**
     * Returns a string representing a unique identifier among all migration entities, the
     * uniqueness is guaranteed by the APK.
     */
    @NonNull
    public String getEntityId() {
        return mEntityId;
    }

    /**
     * Returns a free form data that the APK wants to migrate to the module. The module will base on
     * {@link #mEntityId} to parse this payload.
     */
    @NonNull
    public Bundle getPayload() {
        return mPayload;
    }

    private MigrationDataEntity(@NonNull Parcel in) {
        mTypeId = in.readInt();
        mEntityId = in.readString();
        mPayload = in.readBundle(getClass().getClassLoader());
    }

    @NonNull
    public static final Creator<MigrationDataEntity> CREATOR =
            new Creator<>() {
                @Override
                public MigrationDataEntity createFromParcel(Parcel in) {
                    return new MigrationDataEntity(in);
                }

                @Override
                public MigrationDataEntity[] newArray(int size) {
                    return new MigrationDataEntity[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mTypeId);
        dest.writeString(mEntityId);
        dest.writeBundle(mPayload);
    }
}
