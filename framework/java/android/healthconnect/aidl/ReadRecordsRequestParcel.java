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

import static android.healthconnect.Constants.DEFAULT_LONG;

import android.healthconnect.ReadRecordsRequestUsingFilters;
import android.healthconnect.ReadRecordsRequestUsingIds;
import android.healthconnect.datatypes.DataOrigin;
import android.healthconnect.datatypes.RecordTypeIdentifier;
import android.healthconnect.internal.datatypes.utils.RecordMapper;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A Parcel to carry read request to {@see HealthConnectManager#readRecords}
 *
 * @hide
 */
public class ReadRecordsRequestParcel implements Parcelable {
    public static final Creator<ReadRecordsRequestParcel> CREATOR =
            new Creator<ReadRecordsRequestParcel>() {
                @Override
                public ReadRecordsRequestParcel createFromParcel(Parcel in) {
                    return new ReadRecordsRequestParcel(in);
                }

                @Override
                public ReadRecordsRequestParcel[] newArray(int size) {
                    return new ReadRecordsRequestParcel[size];
                }
            };

    private final RecordIdFiltersParcel mRecordIdFiltersParcel;
    @RecordTypeIdentifier.RecordType private final int mRecordType;
    private final List<String> mPackageFilters;
    private final long mStartTime;
    private final long mEndTime;

    protected ReadRecordsRequestParcel(Parcel in) {
        mRecordType = in.readInt();
        mStartTime = in.readLong();
        mEndTime = in.readLong();
        mPackageFilters = in.createStringArrayList();
        mRecordIdFiltersParcel =
                in.readParcelable(
                        RecordIdFiltersParcel.class.getClassLoader(), RecordIdFiltersParcel.class);
    }

    public ReadRecordsRequestParcel(ReadRecordsRequestUsingIds<?> request) {
        mPackageFilters = Collections.emptyList();
        mRecordIdFiltersParcel = new RecordIdFiltersParcel(request.getRecordIdFilters());
        mStartTime = DEFAULT_LONG;
        mEndTime = DEFAULT_LONG;
        mRecordType = RecordMapper.getInstance().getRecordType(request.getRecordType());
    }

    public ReadRecordsRequestParcel(ReadRecordsRequestUsingFilters<?> request) {
        mPackageFilters =
                request.getDataOrigins().stream()
                        .map(DataOrigin::getPackageName)
                        .collect(Collectors.toList());
        mRecordIdFiltersParcel = null;
        if (request.getTimeRangeFilter() == null) {
            // Use defaults values to signal filters not set
            mStartTime = DEFAULT_LONG;
            mEndTime = DEFAULT_LONG;
        } else {
            mStartTime = request.getTimeRangeFilter().getStartTime().toEpochMilli();
            mEndTime = request.getTimeRangeFilter().getEndTime().toEpochMilli();
        }
        mRecordType = RecordMapper.getInstance().getRecordType(request.getRecordType());
    }

    public int getRecordType() {
        return mRecordType;
    }

    public List<String> getPackageFilters() {
        return mPackageFilters;
    }

    public long getStartTime() {
        return mStartTime;
    }

    public long getEndTime() {
        return mEndTime;
    }

    public RecordIdFiltersParcel getRecordIdFiltersParcel() {
        return mRecordIdFiltersParcel;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mRecordType);
        dest.writeLong(mStartTime);
        dest.writeLong(mEndTime);
        dest.writeStringList(mPackageFilters);
        dest.writeParcelable(mRecordIdFiltersParcel, 0);
    }
}
