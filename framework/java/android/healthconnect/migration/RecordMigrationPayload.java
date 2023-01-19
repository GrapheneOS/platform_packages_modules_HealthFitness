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
import android.healthconnect.datatypes.Record;
import android.healthconnect.internal.datatypes.RecordInternal;
import android.healthconnect.internal.datatypes.utils.InternalExternalRecordConverter;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Holds record migration data payload along with any migration-specific overrides.
 *
 * @hide
 */
public final class RecordMigrationPayload extends MigrationPayload implements Parcelable {

    @NonNull
    public static final Creator<RecordMigrationPayload> CREATOR =
            new Creator<>() {
                @Override
                public RecordMigrationPayload createFromParcel(Parcel in) {
                    return new RecordMigrationPayload(in);
                }

                @Override
                public RecordMigrationPayload[] newArray(int size) {
                    return new RecordMigrationPayload[size];
                }
            };

    private final RecordInternal<?> mRecord;

    private RecordMigrationPayload(
            @NonNull String originPackageName,
            @NonNull String originAppName,
            @NonNull Record record) {
        mRecord = InternalExternalRecordConverter.getInstance().getInternalRecord(record);
        mRecord.setPackageName(originPackageName);
        mRecord.setAppName(originAppName);
    }

    private RecordMigrationPayload(@NonNull Parcel in) {
        mRecord = InternalExternalRecordConverter.getInstance().newInternalRecord(in.readInt());
        mRecord.populateUsing(in);
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mRecord.getRecordType());
        mRecord.writeToParcel(dest);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /** @hide */
    public RecordInternal<?> getRecord() {
        return mRecord;
    }

    /** Builder for {@link RecordMigrationPayload}. */
    public static final class Builder {
        private String mOriginPackageName;
        private String mOriginAppName;
        private Record mRecord;

        /**
         * @param originPackageName package name of the application authored the record.
         * @param originAppName name of the application authored the record.
         * @param record a record to migrate.
         */
        public Builder(
                @NonNull String originPackageName,
                @NonNull String originAppName,
                @NonNull Record record) {
            requireNonNull(originPackageName);
            requireNonNull(originAppName);
            requireNonNull(record);

            mOriginPackageName = originPackageName;
            mOriginAppName = originAppName;
            mRecord = record;
        }

        /** Sets {@code originPackageName} to the specified value. */
        @NonNull
        public Builder setOriginPackageName(@NonNull String originPackageName) {
            requireNonNull(originPackageName);
            mOriginPackageName = originPackageName;
            return this;
        }

        /** Sets {@code originAppName} to the specified value. */
        @NonNull
        public Builder setOriginAppName(@NonNull String originAppName) {
            requireNonNull(originAppName);
            mOriginAppName = originAppName;
            return this;
        }

        /** Sets {@code record} to the specified value. */
        @NonNull
        public Builder setRecord(@NonNull Record record) {
            requireNonNull(record);
            mRecord = record;
            return this;
        }

        /**
         * Creates a new instance of {@link RecordMigrationPayload} with the specified arguments.
         */
        @NonNull
        public RecordMigrationPayload build() {
            return new RecordMigrationPayload(mOriginPackageName, mOriginAppName, mRecord);
        }
    }
}
