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
package com.android.healthconnect.controller.tests.datasources

import android.health.connect.HealthDataCategory
import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.test.espresso.Espresso.onIdle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.dataentries.FormattedEntry
import com.android.healthconnect.controller.datasources.AggregationCardInfo
import com.android.healthconnect.controller.datasources.DataSourcesFragment
import com.android.healthconnect.controller.datasources.DataSourcesViewModel
import com.android.healthconnect.controller.datasources.DataSourcesViewModel.PotentialAppSourcesState
import com.android.healthconnect.controller.datasources.DataSourcesViewModel.AggregationCardsState
import com.android.healthconnect.controller.permissions.data.HealthPermissionType
import com.android.healthconnect.controller.permissiontypes.HealthPermissionTypesViewModel
import com.android.healthconnect.controller.permissiontypes.HealthPermissionTypesViewModel.NewPriorityListState
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.tests.utils.TEST_APP
import com.android.healthconnect.controller.tests.utils.TEST_APP_2
import com.android.healthconnect.controller.tests.utils.TEST_APP_3
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME_2
import com.android.healthconnect.controller.tests.utils.atPosition
import com.android.healthconnect.controller.tests.utils.launchFragment
import com.android.healthconnect.controller.tests.utils.setLocale
import com.android.healthconnect.controller.tests.utils.whenever
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.hamcrest.Matchers.allOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import java.util.TimeZone

@HiltAndroidTest
class DataSourcesFragmentTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @BindValue
    val healthPermissionTypesViewModel: HealthPermissionTypesViewModel =
            Mockito.mock(HealthPermissionTypesViewModel::class.java)
    @BindValue
    val dataSourcesViewModel: DataSourcesViewModel =
            Mockito.mock(DataSourcesViewModel::class.java)

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().context
        context.setLocale(Locale.US)
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("UTC")))
        hiltRule.inject()
        whenever(dataSourcesViewModel.getCurrentSelection()).then {
            HealthDataCategory.ACTIVITY
        }
    }

    @Test
    fun twoSources_noDataTotals_isDisplayed() {
        whenever(healthPermissionTypesViewModel.newPriorityList).then {
            MutableLiveData<NewPriorityListState>(
                    NewPriorityListState.WithData(true,
                            listOf(TEST_APP, TEST_APP_2)))
        }
        whenever(healthPermissionTypesViewModel.editedPriorityList).then {
            MutableLiveData(listOf(TEST_APP, TEST_APP_2))
        }
        whenever(dataSourcesViewModel.aggregationCardsData).then {
            MutableLiveData<AggregationCardsState>(AggregationCardsState.WithData(listOf()))
        }
        whenever(dataSourcesViewModel.potentialAppSources).then {
            MutableLiveData<PotentialAppSourcesState>(PotentialAppSourcesState.WithData(listOf()))
        }
        launchFragment<DataSourcesFragment>(Bundle())
        onIdle()

        onView(withText("Activity")).check(matches(isDisplayed()))
        onView(withText("Data totals")).check(doesNotExist())
        onView(withText("App sources")).check(matches(isDisplayed()))
        onView(withText("Add an app")).check(doesNotExist())
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
    fun twoSources_oneDataTotal_withinLastYear_isDisplayed() {
        whenever(healthPermissionTypesViewModel.newPriorityList).then {
            MutableLiveData<NewPriorityListState>(
                    NewPriorityListState.WithData(true,
                            listOf(TEST_APP, TEST_APP_2)))
        }
        whenever(healthPermissionTypesViewModel.editedPriorityList).then {
            MutableLiveData(listOf(TEST_APP, TEST_APP_2))
        }
        whenever(dataSourcesViewModel.aggregationCardsData).then {
            MutableLiveData<AggregationCardsState>(AggregationCardsState.WithData(listOf(AggregationCardInfo(
                    HealthPermissionType.STEPS,
                    FormattedEntry.FormattedAggregation("1234 steps", "1234 steps", "TestApp"),
                    Instant.parse("2022-10-19T07:06:05.432Z")
            ))))
        }
        whenever(dataSourcesViewModel.potentialAppSources).then {
            MutableLiveData<PotentialAppSourcesState>(PotentialAppSourcesState.WithData(listOf()))
        }
        launchFragment<DataSourcesFragment>(Bundle())

        onView(withText("Activity")).check(matches(isDisplayed()))
        onView(withText("Data totals")).check(matches(isDisplayed()))
        onView(withText("1234 steps")).check(matches(isDisplayed()))
        onView(withText("October 19")).check(matches(isDisplayed()))
        onView(withText("App sources")).check(matches(isDisplayed()))
        onView(withText("Add an app")).check(doesNotExist())
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
    fun oneDataTotal_olderThanOneYear_displaysYear() {
        whenever(healthPermissionTypesViewModel.newPriorityList).then {
            MutableLiveData<NewPriorityListState>(
                NewPriorityListState.WithData(true,
                    listOf(TEST_APP, TEST_APP_2)))
        }
        whenever(healthPermissionTypesViewModel.editedPriorityList).then {
            MutableLiveData(listOf(TEST_APP, TEST_APP_2))
        }
        whenever(dataSourcesViewModel.aggregationCardsData).then {
            MutableLiveData<AggregationCardsState>(AggregationCardsState.WithData(listOf(AggregationCardInfo(
                HealthPermissionType.STEPS,
                FormattedEntry.FormattedAggregation("1234 steps", "1234 steps", "TestApp"),
                Instant.parse("2020-10-19T07:06:05.432Z")
            ))))
        }
        whenever(dataSourcesViewModel.potentialAppSources).then {
            MutableLiveData<PotentialAppSourcesState>(PotentialAppSourcesState.WithData(listOf()))
        }
        launchFragment<DataSourcesFragment>(Bundle())
        onView(withText("Data totals")).check(matches(isDisplayed()))
        onView(withText("1234 steps")).check(matches(isDisplayed()))
        onView(withText("October 19, 2020")).check(matches(isDisplayed()))
    }

    @Test
    fun noSources_displaysEmptyState() {
        whenever(healthPermissionTypesViewModel.newPriorityList).then {
            MutableLiveData<NewPriorityListState>(
                    NewPriorityListState.WithData(true, listOf()))
        }
        whenever(healthPermissionTypesViewModel.editedPriorityList).then {
            MutableLiveData<List<AppMetadata>>(listOf())
        }
        whenever(dataSourcesViewModel.aggregationCardsData).then {
            MutableLiveData<AggregationCardsState>(AggregationCardsState.WithData(listOf()))
        }
        whenever(dataSourcesViewModel.potentialAppSources).then {
            MutableLiveData<PotentialAppSourcesState>(PotentialAppSourcesState.WithData(listOf()))
        }
        launchFragment<DataSourcesFragment>(Bundle())

        onView(withText("Activity")).check(matches(isDisplayed()))
        onView(withText("No app sources")).check(matches(isDisplayed()))
        onView(withText("Once you give app permissions to write activity data, sources will show here."))
            .check(matches(isDisplayed()))
        onView(withText("How sources & prioritization work"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun addAnApp_shownWhenPotentialAppsExist() {
        whenever(healthPermissionTypesViewModel.newPriorityList).then {
            MutableLiveData<NewPriorityListState>(
                NewPriorityListState.WithData(true,
                    listOf(TEST_APP, TEST_APP_2)))
        }
        whenever(healthPermissionTypesViewModel.editedPriorityList).then {
            MutableLiveData(listOf(TEST_APP, TEST_APP_2))
        }
        whenever(dataSourcesViewModel.aggregationCardsData).then {
            MutableLiveData<AggregationCardsState>(AggregationCardsState.WithData(listOf()))
        }
        whenever(dataSourcesViewModel.potentialAppSources).then {
            MutableLiveData<PotentialAppSourcesState>(PotentialAppSourcesState.WithData(listOf(TEST_APP_3)))
        }
        launchFragment<DataSourcesFragment>(Bundle())
        onIdle()

        onView(withText("Activity")).check(matches(isDisplayed()))
        onView(withText("Data totals")).check(doesNotExist())
        onView(withText("App sources")).check(matches(isDisplayed()))
        onView(withText("Add an app")).check(matches(isDisplayed()))
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
    fun atLeastOneSourceLoading_showsLoading() {
        whenever(healthPermissionTypesViewModel.newPriorityList).then {
            MutableLiveData<NewPriorityListState>(
                NewPriorityListState.WithData(true,
                    listOf(TEST_APP, TEST_APP_2)))
        }
        whenever(healthPermissionTypesViewModel.editedPriorityList).then {
            MutableLiveData(listOf(TEST_APP, TEST_APP_2))
        }
        whenever(dataSourcesViewModel.aggregationCardsData).then {
            MutableLiveData<AggregationCardsState>(AggregationCardsState.Loading)
        }
        whenever(dataSourcesViewModel.potentialAppSources).then {
            MutableLiveData<PotentialAppSourcesState>(PotentialAppSourcesState.WithData(listOf(TEST_APP_3)))
        }
        launchFragment<DataSourcesFragment>(Bundle())

        onView(withId(R.id.progress_indicator)).check(matches(isDisplayed()))
    }

    @Test
    fun atLeastOneSourceLoadingFailed_showsError() {
        whenever(healthPermissionTypesViewModel.newPriorityList).then {
            MutableLiveData<NewPriorityListState>(
                NewPriorityListState.LoadingFailed(true))
        }
        whenever(healthPermissionTypesViewModel.editedPriorityList).then {
            MutableLiveData(listOf(TEST_APP, TEST_APP_2))
        }
        whenever(dataSourcesViewModel.aggregationCardsData).then {
            MutableLiveData<AggregationCardsState>(AggregationCardsState.WithData(listOf()))
        }
        whenever(dataSourcesViewModel.potentialAppSources).then {
            MutableLiveData<PotentialAppSourcesState>(PotentialAppSourcesState.WithData(listOf(TEST_APP_3)))
        }
        launchFragment<DataSourcesFragment>(Bundle())
        onIdle()

        onView(withId(R.id.error_view)).check(matches(isDisplayed()))    }
}