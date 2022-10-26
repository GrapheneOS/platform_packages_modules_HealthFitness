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

import android.healthconnect.DeleteUsingFiltersRequest;
import android.healthconnect.datatypes.DataOrigin;
import android.healthconnect.internal.datatypes.utils.RecordMapper;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @see DeleteUsingFiltersRequest
 * @hide
 */
public class DeleteUsingFiltersRequestParcel implements Parcelable {
    public static final Creator<DeleteUsingFiltersRequestParcel> CREATOR =
            new Creator<DeleteUsingFiltersRequestParcel>() {
                @Override
                public DeleteUsingFiltersRequestParcel createFromParcel(Parcel in) {
                    return new DeleteUsingFiltersRequestParcel(in);
                }

                @Override
                public DeleteUsingFiltersRequestParcel[] newArray(int size) {
                    return new DeleteUsingFiltersRequestParcel[size];
                }
            };
    private final List<String> mPackageNameFilters;
    private final int[] mRecordTypeFilters;
    private final long mStartTime;
    private final long mEndTime;

    protected DeleteUsingFiltersRequestParcel(Parcel in) {
        mPackageNameFilters = in.createStringArrayList();
        mRecordTypeFilters = in.createIntArray();
        mStartTime = in.readLong();
        mEndTime = in.readLong();
    }

    public DeleteUsingFiltersRequestParcel(DeleteUsingFiltersRequest request) {
        mPackageNameFilters =
                request.getDataOrigins().stream()
                        .map(DataOrigin::getPackageName)
                        .collect(Collectors.toList());
        mRecordTypeFilters =
                request.getRecordTypes().stream()
                        .mapToInt(
                                recordType -> RecordMapper.getInstance().getRecordType(recordType))
                        .toArray();

        if (request.getTimeRangeFilter() == null) {
            // Use defaults values to signal filters not set
            mStartTime = DEFAULT_LONG;
            mEndTime = DEFAULT_LONG;
        } else {
            mStartTime = request.getTimeRangeFilter().getStartTime().toEpochMilli();
            mEndTime = request.getTimeRangeFilter().getEndTime().toEpochMilli();
        }
    }

    public List<String> getPackageNameFilters() {
        return mPackageNameFilters;
    }

    public List<Integer> getRecordTypeFilters() {
        return Arrays.stream(mRecordTypeFilters).boxed().toList();
    }

    public long getStartTime() {
        return mStartTime;
    }

    public long getEndTime() {
        return mEndTime;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeStringList(mPackageNameFilters);
        dest.writeIntArray(mRecordTypeFilters);
        dest.writeLong(mStartTime);
        dest.writeLong(mEndTime);
    }
}
