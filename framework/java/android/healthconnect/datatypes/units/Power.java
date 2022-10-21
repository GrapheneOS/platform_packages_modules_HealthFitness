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
public class Power implements Comparable<Power> {

    /** Returns the power in Watts. */
    public final double inWatts;

    private Power(@NonNull double value) {
        inWatts = value;
    }

    /** Creates [Power] with the specified value in Watts. */
    @NonNull
    public static Power watts(@NonNull double value) {
        return new Power(value);
    }

    /** Compares this Power object with the given Power parameter". */
    @NonNull
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Power)) return false;
        Power power = (Power) o;
        return Double.compare(power.inWatts, inWatts) == 0;
    }

    /** Returns the Power object in watts as a string. */
    @Override
    public String toString() {
        return "Power{" + "inWatts=" + inWatts + '}';
    }

    /** Returns a hash of the Power value. */
    @Override
    public int hashCode() {
        return Objects.hash(inWatts);
    }

    /** Compares this object with the parameter Power object */
    @Override
    @NonNull
    public int compareTo(@NonNull Power other) {
        return Double.compare(this.inWatts, other.inWatts);
    }
}
