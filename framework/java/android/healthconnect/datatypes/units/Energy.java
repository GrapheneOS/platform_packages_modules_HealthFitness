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

/** Represents a unit of energy. Supported units: joules */
public class Energy implements Comparable<Energy> {

    private final double mInJoules;

    private Energy(double value) {
        mInJoules = value;
    }

    /**
     * Creates an Energy object with the specified value in joules.
     *
     * @param value value to be set as joules.
     */
    @NonNull
    public static Energy fromJoules(double value) {
        return new Energy(value);
    }

    /** Returns energy in joules */
    public double getInJoules() {
        return mInJoules;
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
    public int compareTo(@NonNull Energy other) {
        return Double.compare(this.mInJoules, other.mInJoules);
    }

    /**
     * @return a string representation of the object.
     */
    @Override
    public String toString() {
        return mInJoules + " J";
    }
}
