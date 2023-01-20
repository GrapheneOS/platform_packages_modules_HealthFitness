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
import android.annotation.Nullable;
import android.healthconnect.HealthDataCategory;
import android.healthconnect.datatypes.DataOrigin;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds app priority migration data payload.
 *
 * @hide
 */
public final class PriorityMigrationPayload extends MigrationPayload implements Parcelable {

    @NonNull
    public static final Creator<PriorityMigrationPayload> CREATOR =
            new Creator<>() {
                @Override
                public PriorityMigrationPayload createFromParcel(Parcel in) {
                    return new PriorityMigrationPayload(in);
                }

                @Override
                public PriorityMigrationPayload[] newArray(int size) {
                    return new PriorityMigrationPayload[size];
                }
            };

    private final int mDataCategory;
    private final List<DataOrigin> mDataOrigins;

    private PriorityMigrationPayload(
            @HealthDataCategory.Type int dataCategory, @NonNull List<DataOrigin> dataOrigins) {
        requireNonNull(dataOrigins);

        mDataCategory = dataCategory;
        mDataOrigins = dataOrigins;
    }

    private PriorityMigrationPayload(@NonNull Parcel in) {
        mDataCategory = in.readInt();

        mDataOrigins =
                requireNonNull(in.createStringArrayList()).stream()
                        .map(PriorityMigrationPayload::dataOriginOf)
                        .toList();
    }

    private static DataOrigin dataOriginOf(@Nullable String packageName) {
        return new DataOrigin.Builder().setPackageName(packageName).build();
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mDataCategory);
        dest.writeStringList(mDataOrigins.stream().map(DataOrigin::getPackageName).toList());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /** @hide */
    public int getDataCategory() {
        return mDataCategory;
    }

    /** @hide */
    @NonNull
    public List<DataOrigin> getDataOrigins() {
        return mDataOrigins;
    }

    /** Builder for {@link RecordMigrationPayload}. */
    public static final class Builder {
        private final List<DataOrigin> mDataOrigins = new ArrayList<>();
        private int mDataCategory = HealthDataCategory.UNKNOWN;

        /** Sets {@code dataCategory} to the specified value. */
        @NonNull
        public Builder setDataCategory(@HealthDataCategory.Type int dataCategory) {
            mDataCategory = dataCategory;
            return this;
        }

        /** Adds the specified {@code dataOrigin} to the list. */
        @NonNull
        public Builder addDataOrigin(@NonNull DataOrigin dataOrigin) {
            requireNonNull(dataOrigin);
            mDataOrigins.add(dataOrigin);
            return this;
        }

        /**
         * Creates a new instance of {@link PriorityMigrationPayload} with the specified arguments.
         */
        @NonNull
        public PriorityMigrationPayload build() {
            return new PriorityMigrationPayload(mDataCategory, mDataOrigins);
        }
    }
}
