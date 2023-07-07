/*
 * Copyright (C) 2023 The Android Open Source Project
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

package android.healthconnect.cts.database;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** ColumnInfo contains information about all the attributes that a column can hold. */
class ColumnInfo {
    private final String mName;
    private final String mDataType;
    private final List<Integer> mConstraints;
    private final List<String> mCheckConstraints;
    private final String mDefaultValue;

    /** ColumnConstraint contains the constraints on a column. */
    @IntDef({UNIQUE_CONSTRAINT, NOT_NULL_CONSTRAINT, AUTO_INCREMENT_CONSTRAINT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ColumnConstraint {}

    public static final int UNIQUE_CONSTRAINT = 0;
    public static final int NOT_NULL_CONSTRAINT = 1;
    public static final int AUTO_INCREMENT_CONSTRAINT = 2;

    /** Creates an instance for ColumnInfo. */
    ColumnInfo(Builder builder) {
        mName = builder.mName;
        mDataType = builder.mDataType;
        mConstraints = builder.mConstraints;
        mCheckConstraints = builder.mCheckConstraints;
        mDefaultValue = builder.mDefaultValue;
    }

    /** Builder pattern for ColumnInfo. */
    public static class Builder {
        private final String mName;
        private final String mDataType;
        private final List<Integer> mConstraints;
        private final List<String> mCheckConstraints;
        private String mDefaultValue;

        Builder(String name, String dataType) {
            mName = name;
            mDataType = dataType;
            mConstraints = new ArrayList<>();
            mCheckConstraints = new ArrayList<>();
        }

        /** Sets the default value of the column. */
        public Builder setDefaultValue(String defaultValue) {
            mDefaultValue = defaultValue;
            return this;
        }

        /** Appends constraint to the existing list of constraint. */
        public Builder addConstraint(@ColumnConstraint int constraint) {
            mConstraints.add(constraint);
            return this;
        }

        /** Appends check constraint to the existing list of check constraint. */
        public Builder addCheckConstraint(@NonNull String checkConstraint) {
            Objects.requireNonNull(checkConstraint);
            mCheckConstraints.add(checkConstraint);
            return this;
        }

        /** Builds the columnInfo object. */
        public ColumnInfo build() {
            return new ColumnInfo(this);
        }
    }

    /**
     * @return name of the column.
     */
    @NonNull
    public String getName() {
        return mName;
    }

    /**
     * @return datatype of the column.
     */
    @NonNull
    public String getDataType() {
        return mDataType;
    }

    /**
     * @return list of all the constraints of the column.
     */
    @Nullable
    public List<Integer> getConstraints() {
        return mConstraints;
    }

    /**
     * @return list of all check constraints of the column.
     */
    @Nullable
    public List<String> getCheckConstraints() {
        return mCheckConstraints;
    }

    /**
     * @return the default value of the column if assigned otherwise null.
     */
    @Nullable
    public String getDefaultValue() {
        return mDefaultValue;
    }

    /**
     * @return true if the objects of the two ColumnInfo are same otherwise false.
     */
    @NonNull
    public Boolean isEqual(ColumnInfo expectedColumn) {
        if (mName.equals(expectedColumn.mName) && mDataType.equals(expectedColumn.mDataType)) {
            if (!Objects.equals(mDefaultValue, expectedColumn.mDefaultValue)) {
                return false;
            }
            List<Integer> constraintList = mConstraints;
            List<Integer> expectedConstraintList = expectedColumn.mConstraints;
            List<String> checkConstraintList = mCheckConstraints;
            List<String> expectedCheckConstraintList = expectedColumn.mCheckConstraints;
            Collections.sort(constraintList);
            Collections.sort(expectedConstraintList);
            Collections.sort(checkConstraintList);
            Collections.sort(expectedCheckConstraintList);
            return constraintList.equals(expectedConstraintList)
                    && checkConstraintList.equals(expectedCheckConstraintList);
        }
        return false;
    }
}
