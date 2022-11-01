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

package android.healthconnect.datatypes.units;

import android.annotation.NonNull;

/** Represents a unit of length. Supported units: meters */
public class Length implements Comparable<Length> {
    private final double mInMeters;

    private Length(double value) {
        mInMeters = value;
    }

    /**
     * Creates a Length object with the specified value in meters.
     *
     * @param value value to be set as meters.
     */
    @NonNull
    public Length fromMeters(double value) {
        return new Length(value);
    }

    /** Returns length in meters */
    public double getInMeters() {
        return mInMeters;
    }

    /**
     * Compares this object with the specified object for order. Returns a negative integer, zero,
     * or a positive integer as this object is less than, equal to, or greater than the specified
     * object.
     *
     * @param other the object to be compared.
     * @return a negative integer, zero, or a positive integer as this object is less than, equal
     *     to, or greater than the specified object.
     * @throws NullPointerException if the specified object is null
     * @throws ClassCastException if the specified object's type prevents it from being compared to
     *     this object.
     */
    @Override
    public int compareTo(@NonNull Length other) {
        return Double.compare(this.mInMeters, other.mInMeters);
    }

    /**
     * @return a string representation of the object.
     */
    @Override
    public String toString() {
        return mInMeters + " meters";
    }
}
