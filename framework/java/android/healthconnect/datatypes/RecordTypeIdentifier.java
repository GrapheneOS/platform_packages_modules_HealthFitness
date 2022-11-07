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

package android.healthconnect.datatypes;

import android.annotation.SystemApi;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Identifier for each data type, as returned by {@link Record#getRecordType()}. This is used at
 * various places to efficiently determine operations to perform on a data type.
 *
 * @hide
 */
@SystemApi
public final class RecordTypeIdentifier {
    public static final int RECORD_TYPE_UNKNOWN = 0;

    // Interval Records
    public static final int RECORD_TYPE_STEPS = 1;
    // Series Records
    public static final int RECORD_TYPE_HEART_RATE = 17;
    public static final int RECORD_TYPE_CYCLING_PEDALING_CADENCE = 18;
    public static final int RECORD_TYPE_POWER = 19;
    public static final int RECORD_TYPE_SPEED = 20;
    public static final int RECORD_TYPE_STEPS_CADENCE = 21;
    // Instant records
    public static final int RECORD_TYPE_BASAL_METABOLIC_RATE = 22;

    private RecordTypeIdentifier() {}

    /** @hide */
    @IntDef({
        RECORD_TYPE_UNKNOWN,
        RECORD_TYPE_STEPS,
        RECORD_TYPE_HEART_RATE,
        RECORD_TYPE_BASAL_METABOLIC_RATE,
        RECORD_TYPE_CYCLING_PEDALING_CADENCE,
        RECORD_TYPE_POWER,
        RECORD_TYPE_SPEED,
        RECORD_TYPE_STEPS_CADENCE
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface RecordType {}
}
