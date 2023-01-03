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

package android.healthconnect.internal.datatypes;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.healthconnect.datatypes.ExerciseSessionRecord;
import android.healthconnect.datatypes.ExerciseSessionType;
import android.healthconnect.datatypes.Identifier;
import android.healthconnect.datatypes.RecordTypeIdentifier;
import android.os.Parcel;

import java.util.Objects;

/**
 * @see android.healthconnect.datatypes.ExerciseSessionRecord
 * @hide
 */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_EXERCISE_SESSION)
public final class ExerciseSessionRecordInternal
        extends IntervalRecordInternal<ExerciseSessionRecord> {
    private String mNotes;
    private int mExerciseType;
    private String mTitle;
    private ExerciseRouteInternal mExerciseRoute;

    @NonNull
    public String getNotes() {
        return mNotes;
    }

    /** returns this object with the specified notes */
    @NonNull
    public ExerciseSessionRecordInternal setNotes(String notes) {
        this.mNotes = notes;
        return this;
    }

    @ExerciseSessionType.ExerciseSessionTypes
    public int getExerciseType() {
        return mExerciseType;
    }

    /** returns this object with the specified exerciseType */
    @NonNull
    public ExerciseSessionRecordInternal setExerciseType(int exerciseType) {
        this.mExerciseType = exerciseType;
        return this;
    }

    @NonNull
    public String getTitle() {
        return mTitle;
    }

    /** returns this object with the specified title */
    @NonNull
    public ExerciseSessionRecordInternal setTitle(String title) {
        this.mTitle = title;
        return this;
    }

    /**
     * @return route of this activity
     */
    @Nullable
    public ExerciseRouteInternal getRoute() {
        return mExerciseRoute;
    }

    /** returns this object with the specified route */
    @Nullable
    public ExerciseSessionRecordInternal setRoute(ExerciseRouteInternal route) {
        this.mExerciseRoute = route;
        return this;
    }

    /**
     * @return if this activity has route
     */
    public boolean hasRoute() {
        return mExerciseRoute != null;
    }

    @NonNull
    @Override
    public ExerciseSessionRecord toExternalRecord() {
        ExerciseSessionRecord.Builder builder =
                new ExerciseSessionRecord.Builder(
                        buildMetaData(), getStartTime(), getEndTime(), getExerciseType());

        if (getStartZoneOffset() != null) {
            builder.setStartZoneOffset(getStartZoneOffset());
        }

        if (getEndZoneOffset() != null) {
            builder.setEndZoneOffset(getEndZoneOffset());
        }

        if (getNotes() != null) {
            builder.setNotes(getNotes());
        }

        if (getTitle() != null) {
            builder.setTitle(getTitle());
        }

        if (hasRoute()) {
            builder.setRoute(mExerciseRoute.toExternalRoute());
        }
        return builder.build();
    }

    @Override
    public void populateIntervalRecordFrom(@NonNull ExerciseSessionRecord exerciseSessionRecord) {
        mExerciseType = exerciseSessionRecord.getExerciseType();

        if (exerciseSessionRecord.getNotes() != null) {
            mNotes = exerciseSessionRecord.getNotes().toString();
        }

        if (exerciseSessionRecord.getTitle() != null) {
            mTitle = exerciseSessionRecord.getTitle().toString();
        }

        if (exerciseSessionRecord.hasRoute()) {
            mExerciseRoute =
                    ExerciseRouteInternal.fromExternalRoute(exerciseSessionRecord.getRoute());
        }
    }

    @Override
    public void populateIntervalRecordTo(@NonNull Parcel parcel) {
        parcel.writeString(mNotes);
        parcel.writeInt(mExerciseType);
        parcel.writeString(mTitle);
        ExerciseRouteInternal.writeToParcel(mExerciseRoute, parcel);
    }

    @Override
    public void populateIntervalRecordFrom(@NonNull Parcel parcel) {
        mNotes = parcel.readString();
        mExerciseType = parcel.readInt();
        mTitle = parcel.readString();
        mExerciseRoute = ExerciseRouteInternal.readFromParcel(parcel);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExerciseSessionRecordInternal)) return false;
        ExerciseSessionRecordInternal that = (ExerciseSessionRecordInternal) o;
        return getExerciseType() == that.getExerciseType()
                && Objects.equals(getNotes(), that.getNotes())
                && Objects.equals(getTitle(), that.getTitle())
                && Objects.equals(getRoute(), that.getRoute());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getNotes(), getExerciseType(), getTitle(), mExerciseRoute);
    }
}
