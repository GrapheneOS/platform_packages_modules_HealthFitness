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

import android.annotation.NonNull;
import android.healthconnect.HealthConnectManager;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Class to represent aggregation types in {@link Record} classes.
 *
 * <p>New objects of this class cannot be created.
 *
 * <p>Pre-created (defined in health {@link Record} types) objects of this class can be used to
 * query and fetch aggregate results using aggregate APIs in {@link HealthConnectManager}
 *
 * @see HealthConnectManager#aggregate
 */
public final class AggregationType<T> {
    /** @hide */
    public static final int MAX = 0;
    /** @hide */
    public static final int MIN = 1;
    /** @hide */
    public static final int AVG = 2;
    /** @hide */
    public static final int SUM = 3;

    @AggregationTypeIdentifier.Id private final int mId;
    @AggregateOperationType private final int mType;
    private final List<Integer> mApplicableRecordTypes;
    private final Class<T> mClass;
    /** @hide */
    AggregationType(
            @AggregationTypeIdentifier.Id int id,
            @AggregateOperationType int type,
            @NonNull List<Integer> applicableRecordTypes,
            Class<T> templateClass) {
        Objects.requireNonNull(applicableRecordTypes);

        mId = id;
        mType = type;
        mApplicableRecordTypes = applicableRecordTypes;
        mClass = templateClass;
    }

    /** @hide */
    AggregationType(
            @AggregationTypeIdentifier.Id int id,
            @AggregateOperationType int type,
            @NonNull @RecordTypeIdentifier.RecordType int applicableRecordType,
            Class<T> templateClass) {

        mId = id;
        mType = type;
        mApplicableRecordTypes = Collections.singletonList(applicableRecordType);
        mClass = templateClass;
    }

    /** @hide */
    @AggregationTypeIdentifier.Id
    public int getAggregationTypeIdentifier() {
        return mId;
    }

    /** @hide */
    @NonNull
    public List<Integer> getApplicableRecordTypeIds() {
        return mApplicableRecordTypes;
    }

    /** @hide */
    @AggregateOperationType
    public int getAggregateOperationType() {
        return mType;
    }

    /**
     * Identifier for each aggregate type, as returned by {@link
     * AggregationType#getAggregationTypeIdentifier()}. This is used at various places to determine
     * operations to perform on aggregate type.
     *
     * @hide
     */
    public @interface AggregationTypeIdentifier {
        int HEART_RATE_RECORD_BPM_MAX = 0;
        int HEART_RATE_RECORD_BPM_MIN = 1;

        /** @hide */
        @IntDef({HEART_RATE_RECORD_BPM_MAX, HEART_RATE_RECORD_BPM_MIN})
        @Retention(RetentionPolicy.SOURCE)
        @interface Id {}
    }

    /** @hide */
    @IntDef({MAX, MIN, AVG, SUM})
    @Retention(RetentionPolicy.SOURCE)
    public @interface AggregateOperationType {}
}
