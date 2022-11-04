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

import java.util.Objects;

/** Represents a unit of power. Supported units: watts */
public final class Power implements Comparable<Power> {
    private final double mInWatts;

    private Power(double value) {
        mInWatts = value;
    }

    /**
     * Creates a Power object with the specified value in Watts.
     *
     * @param value value to be set as watts.
     */
    @NonNull
    public static Power fromWatts(double value) {
        return new Power(value);
    }

    /** Returns power in watts */
    public double getInWatts() {
        return mInWatts;
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
    public int compareTo(@NonNull Power other) {
        return Double.compare(this.mInWatts, other.mInWatts);
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * @param object the reference object with which to compare.
     * @return {@code true} if this object is the same as the object argument; {@code false}
     *     otherwise.
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object instanceof Power) {
            Power other = (Power) object;
            return this.getInWatts() == other.getInWatts();
        }
        return false;
    }

    /**
     * Returns a hash code value for the object.
     *
     * @return a hash code value for this object.
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.getInWatts());
    }

    /**
     * @return a string representation of the object.
     */
    @Override
    public String toString() {
        return mInWatts + " watts";
    }
}
