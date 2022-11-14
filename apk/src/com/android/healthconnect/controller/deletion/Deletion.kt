/**
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * ```
 *      http://www.apache.org/licenses/LICENSE-2.0
 * ```
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.deletion

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.StringRes
import com.android.healthconnect.controller.permissions.data.HealthPermissionStrings

/** Represents deletion parameters chosen by the user in the deletion dialogs. */
data class Deletion(
    val chosenRange: ChosenRange = ChosenRange.DELETE_RANGE_LAST_24_HOURS,
    val startTimeMs: Long = -1L,
    val endTimeMs: Long = -1L,
    var deletionType: DeletionType = DeletionType.DeletionTypeAllData(),
    val deletionState: DeletionState = DeletionState.STATE_NO_DELETION_IN_PROGRESS,
    val showTimeRangePickerDialog: Boolean = true
) : Parcelable {

    constructor(
        parcel: Parcel
    ) : this(
        ChosenRange.valueOf(
            parcel.readString() ?: ChosenRange.DELETE_RANGE_LAST_24_HOURS.toString()),
        parcel.readLong(),
        parcel.readLong(),
        parcel.readParcelable(DeletionType::class.java.classLoader)
            ?: DeletionType.DeletionTypeAllData(),
        DeletionState.valueOf(
            parcel.readString() ?: DeletionState.STATE_NO_DELETION_IN_PROGRESS.toString()),
        parcel.readByte() != 0.toByte())

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(this.chosenRange.toString())
        parcel.writeLong(startTimeMs)
        parcel.writeLong(endTimeMs)
        parcel.writeString(this.deletionType.toString())
        parcel.writeString(this.deletionState.toString())
        parcel.writeByte(if (showTimeRangePickerDialog) 1 else 0)
    }

    companion object CREATOR : Parcelable.Creator<Deletion> {
        override fun createFromParcel(parcel: Parcel): Deletion {
            return Deletion(parcel)
        }

        override fun newArray(size: Int): Array<Deletion?> {
            return arrayOfNulls(size)
        }
    }

    @StringRes
    fun getPermissionTypeLabel(): Int {
        check(deletionType.hasPermissionType) {
            "Permission type label not supported for this Deletion parameter"
        }

        val healthPermissionType =
            if (deletionType is DeletionType.DeletionTypeHealthPermissionTypeData)
                (deletionType as DeletionType.DeletionTypeHealthPermissionTypeData)
                    .healthPermissionType
            else
                (deletionType as DeletionType.DeletionTypeHealthPermissionTypeFromApp)
                    .healthPermissionType

        return HealthPermissionStrings.fromPermissionType(healthPermissionType).label
    }

    @StringRes
    fun getCategoryLabel(): Int {
        check(deletionType.hasCategory) {
            "Category label not supported for this Deletion parameter"
        }

        val category = (deletionType as DeletionType.DeletionTypeCategoryData).category

        return category.title
    }
}

enum class ChosenRange {
    DELETE_RANGE_LAST_24_HOURS,
    DELETE_RANGE_LAST_7_DAYS,
    DELETE_RANGE_LAST_30_DAYS,
    DELETE_RANGE_ALL_DATA
}

enum class DeletionState {
    STATE_NO_DELETION_IN_PROGRESS,
    STATE_DELETION_STARTED,
    STATE_PROGRESS_INDICATOR_STARTED,
    STATE_PROGRESS_INDICATOR_CAN_END,
    STATE_DELETION_SUCCESSFUL,
    STATE_DELETION_FAILED
}
