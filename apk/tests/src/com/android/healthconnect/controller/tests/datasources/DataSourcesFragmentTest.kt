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
import androidx.core.os.bundleOf
import androidx.lifecycle.MutableLiveData
import androidx.test.espresso.Espresso.onIdle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasSibling
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.data.entries.FormattedEntry
import com.android.healthconnect.controller.datasources.AggregationCardInfo
import com.android.healthconnect.controller.datasources.DataSourcesFragment
import com.android.healthconnect.controller.datasources.DataSourcesViewModel
import com.android.healthconnect.controller.datasources.DataSourcesViewModel.AggregationCardsState
import com.android.healthconnect.controller.datasources.DataSourcesViewModel.DataSourcesAndAggregationsInfo
import com.android.healthconnect.controller.datasources.DataSourcesViewModel.PotentialAppSourcesState
import com.android.healthconnect.controller.datasources.DataSourcesViewModel.PriorityListState
import com.android.healthconnect.controller.navigation.CATEGORY_KEY
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.shared.app.AppUtils
import com.android.healthconnect.controller.shared.app.AppUtilsModule
import com.android.healthconnect.controller.tests.utils.TEST_APP
import com.android.healthconnect.controller.tests.utils.TEST_APP_2
import com.android.healthconnect.controller.tests.utils.TEST_APP_3
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME_2
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.di.FakeAppUtils
import com.android.healthconnect.controller.tests.utils.hasIndirectSibling
import com.android.healthconnect.controller.tests.utils.launchFragment
import com.android.healthconnect.controller.tests.utils.setLocale
import com.android.healthconnect.controller.tests.utils.toggleAnimation
import com.android.healthconnect.controller.utils.logging.DataSourcesElement
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.PageName
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import java.util.TimeZone
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@UninstallModules(AppUtilsModule::class)
@HiltAndroidTest
class DataSourcesFragmentTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @BindValue
    val dataSourcesViewModel: DataSourcesViewModel = Mockito.mock(DataSourcesViewModel::class.java)
    @BindValue val appUtils: AppUtils = FakeAppUtils()
    @BindValue val healthConnectLogger: HealthConnectLogger = mock()

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().context
        context.setLocale(Locale.US)
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("UTC")))
        hiltRule.inject()
        whenever(dataSourcesViewModel.getCurrentSelection()).then { HealthDataCategory.ACTIVITY }
        whenever(dataSourcesViewModel.shouldShowAddAnAppButton).then { MutableLiveData(false) }
        toggleAnimation(false)
    }

    @After
    fun tearDown() {
        (appUtils as FakeAppUtils).reset()
        reset(healthConnectLogger)
        toggleAnimation(true)
    }

    @Test
    fun dataSourcesFragmentLogging_impressionsLogged() {
        whenever(dataSourcesViewModel.dataSourcesAndAggregationsInfo).then {
            MutableLiveData(
                DataSourcesAndAggregationsInfo(
                    priorityListState =
                        PriorityListState.WithData(true, listOf(TEST_APP, TEST_APP_2)),
                    potentialAppSourcesState =
                        PotentialAppSourcesState.WithData(true, listOf(TEST_APP_3)),
                    aggregationCardsState =
                        AggregationCardsState.WithData(
                            true,
                            listOf(
                                AggregationCardInfo(
                                    FitnessPermissionType.STEPS,
                                    FormattedEntry.FormattedAggregation(
                                        "1234 steps",
                                        "1234 steps",
                                        "TestApp",
                                    ),
                                    Instant.parse("2022-10-19T07:06:05.432Z"),
                                )
                            ),
                        ),
                )
            )
        }

        whenever(dataSourcesViewModel.getPriorityList()).thenReturn(listOf(TEST_APP, TEST_APP_2))
        whenever(dataSourcesViewModel.updatedAggregationCardsData).then {
            MutableLiveData(
                AggregationCardsState.WithData(
                    true,
                    listOf(
                        AggregationCardInfo(
                            FitnessPermissionType.STEPS,
                            FormattedEntry.FormattedAggregation(
                                "1234 steps",
                                "1234 steps",
                                "TestApp",
                            ),
                            Instant.parse("2022-10-19T07:06:05.432Z"),
                        )
                    ),
                )
            )
        }
        launchFragment<DataSourcesFragment>(bundleOf(CATEGORY_KEY to HealthDataCategory.ACTIVITY))

        onIdle()
        verify(healthConnectLogger, atLeast(1)).setPageId(PageName.DATA_SOURCES_PAGE)
        verify(healthConnectLogger).logPageImpression()
        verify(healthConnectLogger).logImpression(DataSourcesElement.DATA_TOTALS_CARD)
        verify(healthConnectLogger).logImpression(DataSourcesElement.DATA_TYPE_SPINNER)
        verify(healthConnectLogger, times(2)).logImpression(DataSourcesElement.APP_SOURCE_BUTTON)
        verify(healthConnectLogger, times(2))
            .logImpression(DataSourcesElement.OPEN_APP_SOURCE_MENU_BUTTON)
        verify(healthConnectLogger).logImpression(DataSourcesElement.ADD_AN_APP_BUTTON)
    }

    @Test
    fun twoActivitySources_noDataTotals_isDisplayed() {
        whenever(dataSourcesViewModel.dataSourcesAndAggregationsInfo).then {
            MutableLiveData(
                DataSourcesAndAggregationsInfo(
                    priorityListState =
                        PriorityListState.WithData(true, listOf(TEST_APP, TEST_APP_2)),
                    potentialAppSourcesState = PotentialAppSourcesState.WithData(true, listOf()),
                    aggregationCardsState = AggregationCardsState.WithData(true, listOf()),
                )
            )
        }
        whenever(dataSourcesViewModel.getPriorityList()).then { listOf(TEST_APP, TEST_APP_2) }
        whenever(dataSourcesViewModel.updatedAggregationCardsData).then {
            MutableLiveData(AggregationCardsState.WithData(true, listOf()))
        }
        launchFragment<DataSourcesFragment>(bundleOf(CATEGORY_KEY to HealthDataCategory.ACTIVITY))
        onIdle()

        onView(withText("Activity")).check(matches(isDisplayed()))
        onView(withText("Data totals")).check(doesNotExist())
        onView(withText("App sources")).check(matches(isDisplayed()))
        onView(withText("Add an app")).check(doesNotExist())
        onView(
                withText(
                    "Add app sources to the list to see how the data " +
                        "totals can change. Removing an app from this list will stop it " +
                        "from contributing to totals, but it will still have write permissions."
                )
            )
            .check(matches(isDisplayed()))
        verifyTestApp()
        verifyTestApp2()
        verify(healthConnectLogger, atLeast(1)).setPageId(PageName.DATA_SOURCES_PAGE)
        verify(healthConnectLogger, atLeast(1)).logPageImpression()
    }

    @Test
    fun twoActivitySources_oneDataTotal_withinLastYear_isDisplayed() {
        whenever(dataSourcesViewModel.dataSourcesAndAggregationsInfo).then {
            MutableLiveData(
                DataSourcesAndAggregationsInfo(
                    priorityListState =
                        PriorityListState.WithData(true, listOf(TEST_APP, TEST_APP_2)),
                    potentialAppSourcesState = PotentialAppSourcesState.WithData(true, listOf()),
                    aggregationCardsState =
                        AggregationCardsState.WithData(
                            true,
                            listOf(
                                AggregationCardInfo(
                                    FitnessPermissionType.STEPS,
                                    FormattedEntry.FormattedAggregation(
                                        "1234 steps",
                                        "1234 steps",
                                        "TestApp",
                                    ),
                                    Instant.parse("2022-10-19T07:06:05.432Z"),
                                )
                            ),
                        ),
                )
            )
        }

        whenever(dataSourcesViewModel.getPriorityList()).thenReturn(listOf(TEST_APP, TEST_APP_2))
        whenever(dataSourcesViewModel.updatedAggregationCardsData).then {
            MutableLiveData(
                AggregationCardsState.WithData(
                    true,
                    listOf(
                        AggregationCardInfo(
                            FitnessPermissionType.STEPS,
                            FormattedEntry.FormattedAggregation(
                                "1234 steps",
                                "1234 steps",
                                "TestApp",
                            ),
                            Instant.parse("2022-10-19T07:06:05.432Z"),
                        )
                    ),
                )
            )
        }
        launchFragment<DataSourcesFragment>(bundleOf(CATEGORY_KEY to HealthDataCategory.ACTIVITY))

        onView(withText("Activity")).check(matches(isDisplayed()))
        onView(withText("Data totals")).check(matches(isDisplayed()))
        onView(withText("1234 steps")).check(matches(isDisplayed()))
        onView(withText("October 19")).check(matches(isDisplayed()))
        onView(withText("App sources")).check(matches(isDisplayed()))
        onView(withText("Add an app")).check(doesNotExist())
        onView(
                withText(
                    "Add app sources to the list to see how the data " +
                        "totals can change. Removing an app from this list will stop it " +
                        "from contributing to totals, but it will still have write permissions."
                )
            )
            .check(matches(isDisplayed()))

        verifyTestApp()
        verifyTestApp2()
    }

    @Test
    fun oneActivityDataTotal_olderThanOneYear_displaysYear() {
        whenever(dataSourcesViewModel.dataSourcesAndAggregationsInfo).then {
            MutableLiveData(
                DataSourcesAndAggregationsInfo(
                    priorityListState =
                        PriorityListState.WithData(true, listOf(TEST_APP, TEST_APP_2)),
                    potentialAppSourcesState = PotentialAppSourcesState.WithData(true, listOf()),
                    aggregationCardsState =
                        AggregationCardsState.WithData(
                            true,
                            listOf(
                                AggregationCardInfo(
                                    FitnessPermissionType.STEPS,
                                    FormattedEntry.FormattedAggregation(
                                        "1234 steps",
                                        "1234 steps",
                                        "TestApp",
                                    ),
                                    Instant.parse("2020-10-19T07:06:05.432Z"),
                                )
                            ),
                        ),
                )
            )
        }

        whenever(dataSourcesViewModel.getPriorityList()).thenReturn(listOf(TEST_APP, TEST_APP_2))
        whenever(dataSourcesViewModel.updatedAggregationCardsData).then {
            MutableLiveData(
                AggregationCardsState.WithData(
                    true,
                    listOf(
                        AggregationCardInfo(
                            FitnessPermissionType.STEPS,
                            FormattedEntry.FormattedAggregation(
                                "1234 steps",
                                "1234 steps",
                                "TestApp",
                            ),
                            Instant.parse("2020-10-19T07:06:05.432Z"),
                        )
                    ),
                )
            )
        }
        launchFragment<DataSourcesFragment>(bundleOf(CATEGORY_KEY to HealthDataCategory.ACTIVITY))
        onView(withText("Data totals")).check(matches(isDisplayed()))
        onView(withText("1234 steps")).check(matches(isDisplayed()))
        onView(withText("October 19, 2020")).check(matches(isDisplayed()))
    }

    @Test
    fun twoSleepSources_oneDataTotal_withinLastYear_isDisplayed() {
        whenever(dataSourcesViewModel.getCurrentSelection()).then { HealthDataCategory.SLEEP }
        whenever(dataSourcesViewModel.dataSourcesAndAggregationsInfo).then {
            MutableLiveData(
                DataSourcesAndAggregationsInfo(
                    priorityListState =
                        PriorityListState.WithData(true, listOf(TEST_APP, TEST_APP_2)),
                    potentialAppSourcesState = PotentialAppSourcesState.WithData(true, listOf()),
                    aggregationCardsState =
                        AggregationCardsState.WithData(
                            true,
                            listOf(
                                AggregationCardInfo(
                                    FitnessPermissionType.SLEEP,
                                    FormattedEntry.FormattedAggregation(
                                        "11h 5m",
                                        "11h 5m",
                                        "TestApp",
                                    ),
                                    Instant.parse("2022-10-18T21:00:00.00Z"),
                                    Instant.parse("2022-10-19T08:05:00.00Z"),
                                )
                            ),
                        ),
                )
            )
        }

        whenever(dataSourcesViewModel.getPriorityList()).thenReturn(listOf(TEST_APP, TEST_APP_2))
        whenever(dataSourcesViewModel.updatedAggregationCardsData).then {
            MutableLiveData(
                AggregationCardsState.WithData(
                    true,
                    listOf(
                        AggregationCardInfo(
                            FitnessPermissionType.SLEEP,
                            FormattedEntry.FormattedAggregation("11h 5m", "11h 5m", "TestApp"),
                            Instant.parse("2022-10-18T21:00:00.00Z"),
                            Instant.parse("2022-10-19T08:05:00.00Z"),
                        )
                    ),
                )
            )
        }

        launchFragment<DataSourcesFragment>(bundleOf(CATEGORY_KEY to HealthDataCategory.SLEEP))

        onView(withText("Sleep")).check(matches(isDisplayed()))
        onView(withText("Data totals")).check(matches(isDisplayed()))
        onView(withText("11h 5m")).check(matches(isDisplayed()))
        onView(withText("Oct 18 – 19")).check(matches(isDisplayed()))
        onView(withText("App sources")).check(matches(isDisplayed()))
        onView(withText("Add an app")).check(doesNotExist())
        onView(
                withText(
                    "Add app sources to the list to see how the data " +
                        "totals can change. Removing an app from this list will stop it " +
                        "from contributing to totals, but it will still have write permissions."
                )
            )
            .check(matches(isDisplayed()))
        verifyTestApp()
        verifyTestApp2()
    }

    @Test
    fun twoSleepSources_oneDataTotal_olderThanOneYear_isDisplayed() {
        whenever(dataSourcesViewModel.getCurrentSelection()).then { HealthDataCategory.SLEEP }
        whenever(dataSourcesViewModel.dataSourcesAndAggregationsInfo).then {
            MutableLiveData(
                DataSourcesAndAggregationsInfo(
                    priorityListState =
                        PriorityListState.WithData(true, listOf(TEST_APP, TEST_APP_2)),
                    potentialAppSourcesState = PotentialAppSourcesState.WithData(true, listOf()),
                    aggregationCardsState =
                        AggregationCardsState.WithData(
                            true,
                            listOf(
                                AggregationCardInfo(
                                    FitnessPermissionType.SLEEP,
                                    FormattedEntry.FormattedAggregation(
                                        "11h 5m",
                                        "11h 5m",
                                        "TestApp",
                                    ),
                                    Instant.parse("2020-10-18T21:00:00.00Z"),
                                    Instant.parse("2020-10-19T08:05:00.00Z"),
                                )
                            ),
                        ),
                )
            )
        }

        whenever(dataSourcesViewModel.getPriorityList()).thenReturn(listOf(TEST_APP, TEST_APP_2))
        whenever(dataSourcesViewModel.updatedAggregationCardsData).then {
            MutableLiveData(
                AggregationCardsState.WithData(
                    true,
                    listOf(
                        AggregationCardInfo(
                            FitnessPermissionType.SLEEP,
                            FormattedEntry.FormattedAggregation("11h 5m", "11h 5m", "TestApp"),
                            Instant.parse("2020-10-18T21:00:00.00Z"),
                            Instant.parse("2020-10-19T08:05:00.00Z"),
                        )
                    ),
                )
            )
        }

        launchFragment<DataSourcesFragment>(bundleOf(CATEGORY_KEY to HealthDataCategory.SLEEP))

        onView(withText("Sleep")).check(matches(isDisplayed()))
        onView(withText("Data totals")).check(matches(isDisplayed()))
        onView(withText("11h 5m")).check(matches(isDisplayed()))
        onView(withText("Oct 18 – 19, 2020")).check(matches(isDisplayed()))
        onView(withText("App sources")).check(matches(isDisplayed()))
        onView(withText("Add an app")).check(doesNotExist())
        onView(
                withText(
                    "Add app sources to the list to see how the data " +
                        "totals can change. Removing an app from this list will stop it " +
                        "from contributing to totals, but it will still have write permissions."
                )
            )
            .check(matches(isDisplayed()))
        verifyTestApp()
        verifyTestApp2()
    }

    @Test
    fun twoSleepSources_oneDataTotal_startTimeOlderThanOneYear_endTimeWithinLastYear_isDisplayed() {
        whenever(dataSourcesViewModel.getCurrentSelection()).then { HealthDataCategory.SLEEP }
        whenever(dataSourcesViewModel.dataSourcesAndAggregationsInfo).then {
            MutableLiveData(
                DataSourcesAndAggregationsInfo(
                    priorityListState =
                        PriorityListState.WithData(true, listOf(TEST_APP, TEST_APP_2)),
                    potentialAppSourcesState = PotentialAppSourcesState.WithData(true, listOf()),
                    aggregationCardsState =
                        AggregationCardsState.WithData(
                            true,
                            listOf(
                                AggregationCardInfo(
                                    FitnessPermissionType.SLEEP,
                                    FormattedEntry.FormattedAggregation(
                                        "11h 5m",
                                        "11h 5m",
                                        "TestApp",
                                    ),
                                    Instant.parse("2020-12-31T21:00:00.00Z"),
                                    Instant.parse("2021-01-01T08:05:00.00Z"),
                                )
                            ),
                        ),
                )
            )
        }

        whenever(dataSourcesViewModel.getPriorityList()).thenReturn(listOf(TEST_APP, TEST_APP_2))
        whenever(dataSourcesViewModel.updatedAggregationCardsData).then {
            MutableLiveData(
                AggregationCardsState.WithData(
                    true,
                    listOf(
                        AggregationCardInfo(
                            FitnessPermissionType.SLEEP,
                            FormattedEntry.FormattedAggregation("11h 5m", "11h 5m", "TestApp"),
                            Instant.parse("2020-12-31T21:00:00.00Z"),
                            Instant.parse("2021-01-01T08:05:00.00Z"),
                        )
                    ),
                )
            )
        }

        launchFragment<DataSourcesFragment>(bundleOf(CATEGORY_KEY to HealthDataCategory.SLEEP))

        onView(withText("Sleep")).check(matches(isDisplayed()))
        onView(withText("Data totals")).check(matches(isDisplayed()))
        onView(withText("11h 5m")).check(matches(isDisplayed()))
        onView(withText("Dec 31, 2020 – Jan 1, 2021")).check(matches(isDisplayed()))
        onView(withText("App sources")).check(matches(isDisplayed()))
        onView(withText("Add an app")).check(doesNotExist())
        onView(
                withText(
                    "Add app sources to the list to see how the data " +
                        "totals can change. Removing an app from this list will stop it " +
                        "from contributing to totals, but it will still have write permissions."
                )
            )
            .check(matches(isDisplayed()))
        verifyTestApp()
        verifyTestApp2()
    }

    @Test
    fun noSources_displaysEmptyState() {
        whenever(dataSourcesViewModel.dataSourcesAndAggregationsInfo).then {
            MutableLiveData(
                DataSourcesAndAggregationsInfo(
                    priorityListState = PriorityListState.WithData(true, listOf()),
                    potentialAppSourcesState = PotentialAppSourcesState.WithData(true, listOf()),
                    aggregationCardsState = AggregationCardsState.WithData(true, listOf()),
                )
            )
        }
        whenever(dataSourcesViewModel.updatedAggregationCardsData).then {
            MutableLiveData(AggregationCardsState.WithData(true, listOf()))
        }
        launchFragment<DataSourcesFragment>(bundleOf(CATEGORY_KEY to HealthDataCategory.ACTIVITY))

        onView(withText("Activity")).check(matches(isDisplayed()))
        onView(withText("No app sources")).check(matches(isDisplayed()))
        onView(
                withText(
                    "Once you give app permissions to write activity data, sources will show here."
                )
            )
            .check(matches(isDisplayed()))
        onView(withText("How sources & prioritization work")).check(matches(isDisplayed()))
    }

    @Test
    fun addAnApp_shownWhenPotentialAppsExist() {
        whenever(dataSourcesViewModel.dataSourcesAndAggregationsInfo).then {
            MutableLiveData(
                DataSourcesAndAggregationsInfo(
                    priorityListState =
                        PriorityListState.WithData(true, listOf(TEST_APP, TEST_APP_2)),
                    potentialAppSourcesState =
                        PotentialAppSourcesState.WithData(true, listOf(TEST_APP_3)),
                    aggregationCardsState = AggregationCardsState.WithData(true, listOf()),
                )
            )
        }
        whenever(dataSourcesViewModel.getPriorityList()).thenReturn(listOf(TEST_APP, TEST_APP_2))
        whenever(dataSourcesViewModel.updatedAggregationCardsData).then {
            MutableLiveData(AggregationCardsState.WithData(true, listOf()))
        }
        launchFragment<DataSourcesFragment>(bundleOf(CATEGORY_KEY to HealthDataCategory.ACTIVITY))
        onIdle()

        onView(withText("Activity")).check(matches(isDisplayed()))
        onView(withText("Data totals")).check(doesNotExist())
        onView(withText("App sources")).check(matches(isDisplayed()))
        onView(withText("Add an app")).check(matches(isDisplayed()))
        onView(
                withText(
                    "Add app sources to the list to see how the data " +
                        "totals can change. Removing an app from this list will stop it " +
                        "from contributing to totals, but it will still have write permissions."
                )
            )
            .check(matches(isDisplayed()))

        verifyTestApp()
        verifyTestApp2()
    }

    @Test
    fun appOnPriorityList_whenDefaultApp_showsAsDeviceDefault() {
        (appUtils as FakeAppUtils).setDefaultApp(TEST_APP_PACKAGE_NAME)
        launchFragment(listOf(TEST_APP, TEST_APP_2))
        onIdle()

        onView(withText("Activity")).check(matches(isDisplayed()))
        onView(withText("Data totals")).check(matches(isDisplayed()))
        onView(withText("App sources")).check(matches(isDisplayed()))
        onView(withText("Add an app")).check(doesNotExist())
        onView(
                withText(
                    "Add app sources to the list to see how the data " +
                        "totals can change. Removing an app from this list will stop it " +
                        "from contributing to totals, but it will still have write permissions."
                )
            )
            .perform(scrollTo())
            .check(matches(isDisplayed()))

        onView(
                allOf(
                    withText(TEST_APP_NAME),
                    hasIndirectSibling(withText("1")),
                    hasSibling(withText("Device default")),
                )
            )
            .perform(scrollTo())
            .check(matches(isDisplayed()))

        onView(
                allOf(
                    withText(TEST_APP_NAME_2),
                    hasIndirectSibling(withText("2")),
                    hasSibling(not(withText("Device default"))),
                )
            )
            .perform(scrollTo())
            .check(matches(isDisplayed()))
    }

    @Test
    fun atLeastOneSourceLoading_showsLoading() {
        whenever(dataSourcesViewModel.dataSourcesAndAggregationsInfo).then {
            MutableLiveData(
                DataSourcesAndAggregationsInfo(
                    priorityListState =
                        PriorityListState.WithData(true, listOf(TEST_APP, TEST_APP_2)),
                    potentialAppSourcesState = PotentialAppSourcesState.WithData(true, listOf()),
                    aggregationCardsState = AggregationCardsState.Loading(true),
                )
            )
        }

        whenever(dataSourcesViewModel.getPriorityList()).thenReturn(listOf(TEST_APP, TEST_APP_2))
        whenever(dataSourcesViewModel.updatedAggregationCardsData).then {
            MutableLiveData(AggregationCardsState.Loading(false))
        }
        launchFragment<DataSourcesFragment>(bundleOf(CATEGORY_KEY to HealthDataCategory.ACTIVITY))

        onView(withId(R.id.progress_indicator)).check(matches(isDisplayed()))
    }

    @Test
    fun atLeastOneSourceLoadingFailed_showsError() {
        whenever(dataSourcesViewModel.dataSourcesAndAggregationsInfo).then {
            MutableLiveData(
                DataSourcesAndAggregationsInfo(
                    priorityListState = PriorityListState.LoadingFailed(true),
                    potentialAppSourcesState = PotentialAppSourcesState.WithData(true, listOf()),
                    aggregationCardsState = AggregationCardsState.WithData(true, listOf()),
                )
            )
        }

        whenever(dataSourcesViewModel.getPriorityList()).thenReturn(listOf(TEST_APP, TEST_APP_2))
        whenever(dataSourcesViewModel.updatedAggregationCardsData).then {
            MutableLiveData(AggregationCardsState.WithData(true, listOf()))
        }
        launchFragment<DataSourcesFragment>(bundleOf(CATEGORY_KEY to HealthDataCategory.ACTIVITY))
        onIdle()

        onView(withId(R.id.error_view)).check(matches(isDisplayed()))
    }

    @Test
    fun clickOnTopAppSource_menuHasCorrectOptions() {
        launchFragment(listOf(TEST_APP, TEST_APP_2, TEST_APP_3))

        onView(
                withContentDescription(
                    "Button to reorder or remove Health Connect test app from the app sources list"
                )
            )
            .perform(scrollTo())
            .perform(click())
        onIdle()

        verify(healthConnectLogger).logInteraction(DataSourcesElement.OPEN_APP_SOURCE_MENU_BUTTON)
        onView(withText("Move up")).check(doesNotExist())
        onView(withText("Move down")).check(matches(isDisplayed()))
        onView(withText("Remove")).check(matches(isDisplayed()))
        verify(healthConnectLogger)
            .logImpression(DataSourcesElement.MOVE_APP_SOURCE_DOWN_MENU_BUTTON)
        verify(healthConnectLogger).logImpression(DataSourcesElement.REMOVE_APP_SOURCE_MENU_BUTTON)
    }

    @Test
    fun clickOnMiddleAppSource_menuHasCorrectOptions() {
        launchFragment(listOf(TEST_APP, TEST_APP_2, TEST_APP_3))

        onView(
                withContentDescription(
                    "Button to reorder or remove Health Connect test app 2 from the app sources list"
                )
            )
            .perform(scrollTo())
            .perform(click())
        onIdle()

        verify(healthConnectLogger).logInteraction(DataSourcesElement.OPEN_APP_SOURCE_MENU_BUTTON)
        onView(withText("Move up")).check(matches(isDisplayed()))
        onView(withText("Move down")).check(matches(isDisplayed()))
        onView(withText("Remove")).check(matches(isDisplayed()))
        verify(healthConnectLogger).logImpression(DataSourcesElement.MOVE_APP_SOURCE_UP_MENU_BUTTON)
        verify(healthConnectLogger)
            .logImpression(DataSourcesElement.MOVE_APP_SOURCE_DOWN_MENU_BUTTON)
        verify(healthConnectLogger).logImpression(DataSourcesElement.REMOVE_APP_SOURCE_MENU_BUTTON)
    }

    @Test
    fun clickOnBottomAppSource_menuHasCorrectOptions() {
        launchFragment(listOf(TEST_APP, TEST_APP_2, TEST_APP_3))

        onView(
                withContentDescription(
                    "Button to reorder or remove Health Connect test app 3 from the app sources list"
                )
            )
            .perform(scrollTo())
            .perform(click())
        onIdle()

        verify(healthConnectLogger).logInteraction(DataSourcesElement.OPEN_APP_SOURCE_MENU_BUTTON)
        onView(withText("Move up")).check(matches(isDisplayed()))
        onView(withText("Move down")).check(doesNotExist())
        onView(withText("Remove")).check(matches(isDisplayed()))
        verify(healthConnectLogger).logImpression(DataSourcesElement.MOVE_APP_SOURCE_UP_MENU_BUTTON)
        verify(healthConnectLogger).logImpression(DataSourcesElement.REMOVE_APP_SOURCE_MENU_BUTTON)
    }

    private fun launchFragment(priorityList: List<AppMetadata>) {
        whenever(dataSourcesViewModel.dataSourcesAndAggregationsInfo).then {
            MutableLiveData(
                DataSourcesAndAggregationsInfo(
                    priorityListState = PriorityListState.WithData(true, priorityList),
                    potentialAppSourcesState = PotentialAppSourcesState.WithData(true, listOf()),
                    aggregationCardsState =
                        AggregationCardsState.WithData(
                            true,
                            listOf(
                                AggregationCardInfo(
                                    FitnessPermissionType.STEPS,
                                    FormattedEntry.FormattedAggregation(
                                        "1234 steps",
                                        "1234 steps",
                                        "TestApp",
                                    ),
                                    Instant.parse("2022-10-19T07:06:05.432Z"),
                                )
                            ),
                        ),
                )
            )
        }
        whenever(dataSourcesViewModel.getPriorityList()).thenReturn(priorityList)
        whenever(dataSourcesViewModel.updatedAggregationCardsData).then {
            MutableLiveData(
                AggregationCardsState.WithData(
                    true,
                    listOf(
                        AggregationCardInfo(
                            FitnessPermissionType.STEPS,
                            FormattedEntry.FormattedAggregation(
                                "1234 steps",
                                "1234 steps",
                                "TestApp",
                            ),
                            Instant.parse("2022-10-19T07:06:05.432Z"),
                        )
                    ),
                )
            )
        }
        launchFragment<DataSourcesFragment>(bundleOf(CATEGORY_KEY to HealthDataCategory.ACTIVITY))
    }

    private fun verifyTestApp() {
        onView(allOf(withText(TEST_APP_NAME), hasIndirectSibling(withText("1"))))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
    }

    private fun verifyTestApp2() {
        onView(allOf(withText(TEST_APP_NAME_2), hasIndirectSibling(withText("2"))))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
    }
}
