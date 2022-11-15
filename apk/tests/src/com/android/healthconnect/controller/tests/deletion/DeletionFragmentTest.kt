package com.android.healthconnect.controller.tests.deletion

import android.os.Bundle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.categories.HealthDataCategory
import com.android.healthconnect.controller.deletion.Deletion
import com.android.healthconnect.controller.deletion.DeletionFragment
import com.android.healthconnect.controller.deletion.DeletionType
import com.android.healthconnect.controller.permissions.data.HealthPermissionType
import com.android.healthconnect.controller.tests.utils.launchFragment
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class DeletionFragmentTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @Test
    fun deletionFragment_deleteAllData_showsFirstDialog() {

        val deletion =
            Deletion(
                deletionType = DeletionType.DeletionTypeAllData(), showTimeRangePickerDialog = true)

        launchFragment<DeletionFragment>(Bundle()) {
            (this as DeletionFragment).startDataDeletion(deletion)
        }

        onView(withText(R.string.time_range_title)).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withText(R.string.time_range_message_all))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
    }

    @Test
    fun deletionFragment_deleteAllCategoryData_showsFirstDialog() {

        val deletion =
            Deletion(
                deletionType =
                    DeletionType.DeletionTypeCategoryData(category = HealthDataCategory.ACTIVITY),
                showTimeRangePickerDialog = true)

        launchFragment<DeletionFragment>(Bundle()) {
            (this as DeletionFragment).startDataDeletion(deletion)
        }

        onView(withText(R.string.time_range_title)).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(
                withText(
                    "This permanently deletes activity data " +
                        "added to Health Connect in the chosen time period"))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
    }

    @Test
    fun deletionFragment_deleteAllPermissionTypeData_showsFirstDialog() {

        val deletion =
            Deletion(
                deletionType =
                    DeletionType.DeletionTypeHealthPermissionTypeData(
                        healthPermissionType = HealthPermissionType.BLOOD_GLUCOSE),
                showTimeRangePickerDialog = true)

        launchFragment<DeletionFragment>(Bundle()) {
            (this as DeletionFragment).startDataDeletion(deletion)
        }

        onView(withText(R.string.time_range_title)).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(
                withText(
                    "This permanently deletes blood glucose data " +
                        "added to Health Connect in the chosen time period"))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
    }
}
