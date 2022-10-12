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

import android.healthconnect.AggregateRecordsResponse;
import android.healthconnect.internal.datatypes.utils.AggregationTypeIdMapper;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.ArrayMap;

import androidx.annotation.NonNull;

import java.util.Map;

/** @hide */
public class AggregateDataResponseParcel implements Parcelable {
    public static final Creator<AggregateDataResponseParcel> CREATOR =
            new Creator<>() {
                @Override
                public AggregateDataResponseParcel createFromParcel(Parcel in) {
                    return new AggregateDataResponseParcel(in);
                }

                @Override
                public AggregateDataResponseParcel[] newArray(int size) {
                    return new AggregateDataResponseParcel[size];
                }
            };
    private final AggregateRecordsResponse<?> mAggregateRecordsResponse;

    public AggregateDataResponseParcel(AggregateRecordsResponse<?> aggregateRecordsResponse) {
        mAggregateRecordsResponse = aggregateRecordsResponse;
    }

    protected AggregateDataResponseParcel(Parcel in) {
        final int size = in.readInt();
        Map<Integer, AggregateRecordsResponse.AggregateResult<?>> result = new ArrayMap<>(size);

        for (int i = 0; i < size; i++) {
            int id = in.readInt();
            AggregateRecordsResponse.AggregateResult<?> aggregateResult =
                    AggregationTypeIdMapper.getInstance().getAggregateResultFor(id, in);
            result.put(id, aggregateResult);
        }

        mAggregateRecordsResponse = new AggregateRecordsResponse<>(result);
    }

    public AggregateRecordsResponse<?> getAggregateDataResponse() {
        return mAggregateRecordsResponse;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mAggregateRecordsResponse.getAggregateResults().size());
        mAggregateRecordsResponse
                .getAggregateResults()
                .forEach(
                        (key, val) -> {
                            dest.writeInt(key.getAggregationTypeIdentifier());
                            val.putToParcel(dest);
                        });
    }
}
