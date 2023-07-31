package com.android.healthconnect.controller.tests.datasources

import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.datasources.DataSourcesFragment
import com.android.healthconnect.controller.permissiontypes.HealthPermissionTypesViewModel
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.tests.utils.TEST_APP
import com.android.healthconnect.controller.tests.utils.TEST_APP_2
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME_2
import com.android.healthconnect.controller.tests.utils.atPosition
import com.android.healthconnect.controller.tests.utils.launchFragment
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.hamcrest.Matchers.allOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito

@HiltAndroidTest
class DataSourcesFragmentTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @BindValue
    val viewModel: HealthPermissionTypesViewModel =
            Mockito.mock(HealthPermissionTypesViewModel::class.java)

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun dataSourcesFragment_withTwoSources_isDisplayed() {
        Mockito.`when`(viewModel.priorityList).then {
            MutableLiveData<HealthPermissionTypesViewModel.PriorityListState>(
                    HealthPermissionTypesViewModel.PriorityListState.WithData(
                            listOf(TEST_APP, TEST_APP_2)))
        }
        Mockito.`when`(viewModel.editedPriorityList).then {
            MutableLiveData<List<AppMetadata>>(listOf(TEST_APP, TEST_APP_2))
        }
        launchFragment<DataSourcesFragment>(Bundle())

        onView(withText("Activity")).check(matches(isDisplayed()))
        onView(withText("App sources")).check(matches(isDisplayed()))
        onView(withText("Add app sources to the list to see how the data " +
                "totals can change. Removing an app from this list will stop it " +
                "from contributing to totals, but it will still have write permissions."))
                .check(matches(isDisplayed()))

        onView(withId(R.id.linear_layout_recycle_view))
                .check(matches(atPosition(0, allOf(hasDescendant(withText("1")),
                        hasDescendant(withText(TEST_APP_NAME))))))
        onView(withId(R.id.linear_layout_recycle_view))
                .check(matches(atPosition(1, allOf(hasDescendant(withText("2")),
                        hasDescendant(withText(TEST_APP_NAME_2))))))
    }

    @Test
    fun dataSourcesFragment_withNoSources_displaysEmptyState() {
        Mockito.`when`(viewModel.priorityList).then {
            MutableLiveData<HealthPermissionTypesViewModel.PriorityListState>(
                    HealthPermissionTypesViewModel.PriorityListState.WithData(listOf()))
        }
        Mockito.`when`(viewModel.editedPriorityList).then {
            MutableLiveData<List<AppMetadata>>(listOf())
        }
        launchFragment<DataSourcesFragment>(Bundle())

        onView(withText("Activity")).check(matches(isDisplayed()))
        onView(withText("No app sources.\nOnce you give app permissions to " +
                "write activity data, sources will show here.")).check(matches(isDisplayed()))
    }
}