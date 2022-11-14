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

package android.healthconnect;

import android.annotation.NonNull;
import android.os.Parcel;

import java.time.ZoneOffset;
import java.util.Objects;

/**
 * A class to represent the results of {@link HealthConnectManager} aggregate APIs
 *
 * @hide
 */
public final class AggregateResult<T> {
    private final T mResult;
    private ZoneOffset mZoneOffset;

    public AggregateResult(T result) {
        mResult = result;
    }

    public void putToParcel(@NonNull Parcel parcel) {
        if (mResult instanceof Long) {
            parcel.writeLong((Long) mResult);
        }
    }

    /**
     * @return {@link ZoneOffset} for the underlying record, null if aggregation was derived from
     *     multiple records
     */
    @NonNull
    public ZoneOffset getZoneOffset() {
        return mZoneOffset;
    }

    /** Sets the {@link ZoneOffset} for the underlying record. */
    public AggregateResult<T> setZoneOffset(@NonNull ZoneOffset zoneOffset) {
        Objects.requireNonNull(zoneOffset);

        mZoneOffset = zoneOffset;
        return this;
    }

    /**
     * @return an Object representing the result of an aggregation.
     */
    @NonNull
    T getResult() {
        return mResult;
    }
}
