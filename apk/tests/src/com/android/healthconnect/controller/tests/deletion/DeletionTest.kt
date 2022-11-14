package com.android.healthconnect.controller.tests.deletion

import android.os.Bundle
import com.android.healthconnect.controller.categories.HealthDataCategory
import com.android.healthconnect.controller.deletion.ChosenRange
import com.android.healthconnect.controller.deletion.Deletion
import com.android.healthconnect.controller.deletion.DeletionState
import com.android.healthconnect.controller.deletion.DeletionType
import com.android.healthconnect.controller.permissions.data.HealthPermissionType
import java.time.Instant
import junit.framework.Assert.assertTrue
import org.junit.Test

class DeletionTest {

    @Test
    fun deletionToParcel() {

        val deletion =
            Deletion(
                chosenRange = ChosenRange.DELETE_RANGE_LAST_24_HOURS,
                deletionType = DeletionType.DeletionTypeAllData(),
            )

        val bundle = Bundle()
        bundle.putParcelable("DELETION_TEST", deletion)

        val outValue = bundle.getParcelable("DELETION_TEST") as Deletion?

        assertTrue(outValue != null)
        assertTrue(deletion.chosenRange == outValue?.chosenRange)
        assertTrue(deletion.deletionType == outValue?.deletionType)
    }

    @Test
    fun deletionTypeHealthPermissionTypeData_toParcel() {
        val startTime = Instant.parse("2022-11-11T20:00:00.000Z")
        val endTime = Instant.parse("2022-11-14T20:00:00.000Z")
        val deletionType =
            DeletionType.DeletionTypeHealthPermissionTypeData(
                HealthPermissionType.ACTIVE_CALORIES_BURNED)
        val deletion =
            Deletion(
                chosenRange = ChosenRange.DELETE_RANGE_LAST_30_DAYS,
                startTimeMs = startTime.toEpochMilli(),
                endTimeMs = endTime.toEpochMilli(),
                deletionType = deletionType,
                deletionState = DeletionState.STATE_DELETION_STARTED,
                showTimeRangePickerDialog = false)

        val bundle = Bundle()
        bundle.putParcelable("DELETION_TEST", deletion)

        val outValue = bundle.getParcelable("DELETION_TEST") as Deletion?

        assertTrue(outValue != null)
        assertTrue(deletion.chosenRange == outValue?.chosenRange)
        assertTrue(deletion.startTimeMs == outValue?.startTimeMs)
        assertTrue(deletion.endTimeMs == outValue?.endTimeMs)
        assertTrue(deletion.deletionType == outValue?.deletionType)
        assertTrue(
            deletionType.healthPermissionType ==
                (outValue?.deletionType as DeletionType.DeletionTypeHealthPermissionTypeData)
                    .healthPermissionType)
        assertTrue(deletion.deletionState == outValue.deletionState)
        assertTrue(deletion.showTimeRangePickerDialog == outValue.showTimeRangePickerDialog)
    }

    @Test
    fun deletionTypeCategoryData_toParcel() {
        val startTime = Instant.parse("2022-11-11T20:00:00.000Z")
        val endTime = Instant.parse("2022-11-14T20:00:00.000Z")
        val deletionType = DeletionType.DeletionTypeCategoryData(HealthDataCategory.ACTIVITY)
        val deletion =
            Deletion(
                chosenRange = ChosenRange.DELETE_RANGE_LAST_30_DAYS,
                startTimeMs = startTime.toEpochMilli(),
                endTimeMs = endTime.toEpochMilli(),
                deletionType = deletionType,
                deletionState = DeletionState.STATE_DELETION_STARTED,
                showTimeRangePickerDialog = false)

        val bundle = Bundle()
        bundle.putParcelable("DELETION_TEST", deletion)

        val outValue = bundle.getParcelable("DELETION_TEST") as Deletion?

        assertTrue(outValue != null)
        assertTrue(deletion.chosenRange == outValue?.chosenRange)
        assertTrue(deletion.startTimeMs == outValue?.startTimeMs)
        assertTrue(deletion.endTimeMs == outValue?.endTimeMs)
        assertTrue(deletion.deletionType == outValue?.deletionType)
        assertTrue(
            deletionType.category ==
                (outValue?.deletionType as DeletionType.DeletionTypeCategoryData).category)
        assertTrue(deletion.deletionState == outValue.deletionState)
        assertTrue(deletion.showTimeRangePickerDialog == outValue.showTimeRangePickerDialog)
    }
}
