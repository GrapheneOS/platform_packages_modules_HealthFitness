/**
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * ```
 *      http://www.apache.org/licenses/LICENSE-2.0
 * ```
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.healthconnect.controller.tests.dataentries

import androidx.core.os.bundleOf
import androidx.lifecycle.MutableLiveData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.dataentries.DataEntriesFragment
import com.android.healthconnect.controller.dataentries.DataEntriesFragmentViewModel
import com.android.healthconnect.controller.dataentries.DataEntriesFragmentViewModel.DataEntriesFragmentState.Empty
import com.android.healthconnect.controller.dataentries.DataEntriesFragmentViewModel.DataEntriesFragmentState.Loading
import com.android.healthconnect.controller.dataentries.DataEntriesFragmentViewModel.DataEntriesFragmentState.LoadingFailed
import com.android.healthconnect.controller.dataentries.DataEntriesFragmentViewModel.DataEntriesFragmentState.WithData
import com.android.healthconnect.controller.dataentries.FormattedEntry.FormattedDataEntry
import com.android.healthconnect.controller.permissions.data.HealthPermissionType.STEPS
import com.android.healthconnect.controller.permissiontypes.HealthPermissionTypesFragment.Companion.PERMISSION_TYPE_KEY
import com.android.healthconnect.controller.shared.DataType
import com.android.healthconnect.controller.tests.utils.launchFragment
import com.android.healthconnect.controller.tests.utils.withIndex
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito

@HiltAndroidTest
class DataEntriesFragmentTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @BindValue
    val viewModel: DataEntriesFragmentViewModel =
        Mockito.mock(DataEntriesFragmentViewModel::class.java)

    @Before
    fun setup() {
        hiltRule.inject()

        Mockito.`when`(viewModel.currentSelectedDate).thenReturn(MutableLiveData())
    }

    @Test
    fun dataEntriesInit_showsDateNavigationPreference() {
        Mockito.`when`(viewModel.dataEntries).thenReturn(MutableLiveData(WithData(emptyList())))

        launchFragment<DataEntriesFragment>(bundleOf(PERMISSION_TYPE_KEY to STEPS))

        onView(withId(R.id.selected_date)).check(matches(isDisplayed()))
    }

    @Test
    fun dataEntriesInit_noData_showsNoData() {
        Mockito.`when`(viewModel.dataEntries).thenReturn(MutableLiveData(Empty))

        launchFragment<DataEntriesFragment>(bundleOf(PERMISSION_TYPE_KEY to STEPS))

        onView(withId(R.id.no_data_view)).check(matches(isDisplayed()))
    }

    @Test
    fun dataEntriesInit_error_showsNoData() {
        Mockito.`when`(viewModel.dataEntries).thenReturn(MutableLiveData(LoadingFailed))

        launchFragment<DataEntriesFragment>(bundleOf(PERMISSION_TYPE_KEY to STEPS))

        onView(withId(R.id.error_view)).check(matches(isDisplayed()))
    }

    @Test
    fun dataEntriesInit_loading_showsLoading() {
        Mockito.`when`(viewModel.dataEntries).thenReturn(MutableLiveData(Loading))

        launchFragment<DataEntriesFragment>(bundleOf(PERMISSION_TYPE_KEY to STEPS))

        onView(withId(R.id.loading)).check(matches(isDisplayed()))
    }

    @Test
    fun dataEntriesInit_withData_showsListOfEntries() {
        Mockito.`when`(viewModel.dataEntries)
            .thenReturn(MutableLiveData(WithData(FORMATTED_STEPS_LIST)))

        launchFragment<DataEntriesFragment>(bundleOf(PERMISSION_TYPE_KEY to STEPS))

        onView(withText("7:06 AM - 7:06 AM • TEST_APP_NAME")).check(matches(isDisplayed()))
        onView(withText("12 steps")).check(matches(isDisplayed()))
        onView(withText("8:06 AM - 8:06 AM • TEST_APP_NAME")).check(matches(isDisplayed()))
        onView(withText("15 steps")).check(matches(isDisplayed()))
    }

    @Test
    fun dataEntries_withData_showsDeleteAction() {
        Mockito.`when`(viewModel.dataEntries)
            .thenReturn(MutableLiveData(WithData(FORMATTED_STEPS_LIST)))

        launchFragment<DataEntriesFragment>(bundleOf(PERMISSION_TYPE_KEY to STEPS))

        onView(withIndex(withId(R.id.item_data_entry_delete), 0)).check(matches(isDisplayed()))
    }
}

private val FORMATTED_STEPS_LIST =
    listOf(
        FormattedDataEntry(
            uuid = "test_id",
            header = "7:06 AM - 7:06 AM • TEST_APP_NAME",
            headerA11y = "from 7:06 AM to 7:06 AM • TEST_APP_NAME",
            title = "12 steps",
            titleA11y = "12 steps",
            dataType = DataType.STEPS),
        FormattedDataEntry(
            uuid = "test_id",
            header = "8:06 AM - 8:06 AM • TEST_APP_NAME",
            headerA11y = "from 8:06 AM to 8:06 AM • TEST_APP_NAME",
            title = "15 steps",
            titleA11y = "15 steps",
            dataType = DataType.STEPS))
