/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.android.healthconnect.controller.tests.selectabledeletion

import android.content.Context
import android.health.connect.datatypes.StepsCadenceRecord
import android.health.connect.datatypes.StepsRecord
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.MutableLiveData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.data.entries.datenavigation.DateNavigationPeriod
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.selectabledeletion.DeletionConstants.START_DELETION_KEY
import com.android.healthconnect.controller.selectabledeletion.DeletionFragment
import com.android.healthconnect.controller.selectabledeletion.DeletionType
import com.android.healthconnect.controller.selectabledeletion.DeletionViewModel
import com.android.healthconnect.controller.tests.utils.DEVICE_DATA_PROVIDER_APP_NAME
import com.android.healthconnect.controller.tests.utils.DEVICE_DATA_PROVIDER_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.TestTimeSource
import com.android.healthconnect.controller.tests.utils.launchFragment
import com.android.healthconnect.controller.tests.utils.setLocale
import com.android.healthconnect.controller.utils.TimeSource
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import org.hamcrest.Matchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.whenever

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class DeletionFragmentTest {
    @get:Rule val hiltRule = HiltAndroidRule(this)

    @BindValue val viewModel: DeletionViewModel = Mockito.mock(DeletionViewModel::class.java)
    private lateinit var context: Context
    @Inject lateinit var testTimeSource: TimeSource

    @Before
    fun setup() {
        hiltRule.inject()
        context = InstrumentationRegistry.getInstrumentation().context
        context.setLocale(Locale.US)
        whenever(viewModel.getDeletionType()).then {
            DeletionType.DeleteHealthPermissionTypes(
                setOf(FitnessPermissionType.STEPS, FitnessPermissionType.SPEED),
                totalPermissionTypes = 10,
            )
        }
        whenever(viewModel.removePermissions).thenReturn(false)
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("UTC")))
    }

    // region DeletePermissionTypes
    @Test
    fun deletePermissionTypes_oneSelected_confirmationDialog_showsCorrectText() {
        whenever(viewModel.deletionProgress).then {
            MutableLiveData(DeletionViewModel.DeletionProgress.NOT_STARTED)
        }
        whenever(viewModel.getDeletionType()).then {
            DeletionType.DeleteHealthPermissionTypes(
                setOf(FitnessPermissionType.STEPS),
                totalPermissionTypes = 10,
            )
        }

        launchFragment<DeletionFragment>(Bundle()) {
                (this as DeletionFragment)
                    .parentFragmentManager
                    .setFragmentResult(START_DELETION_KEY, bundleOf())
            }
            .use {
                onView(withText("Permanently delete selected data?"))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Connected apps will no longer be able to read this data from Health\u00A0Connect"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(withText("Cancel")).inRoot(isDialog()).check(matches(isDisplayed()))
                onView(withText("Delete")).inRoot(isDialog()).check(matches(isDisplayed()))
            }
    }

    @Test
    fun deletePermissionTypes_someSelected_confirmationDialog_showsCorrectText() {
        whenever(viewModel.deletionProgress).then {
            MutableLiveData(DeletionViewModel.DeletionProgress.NOT_STARTED)
        }
        whenever(viewModel.getDeletionType()).then {
            DeletionType.DeleteHealthPermissionTypes(
                setOf(FitnessPermissionType.STEPS, FitnessPermissionType.SPEED),
                totalPermissionTypes = 10,
            )
        }

        launchFragment<DeletionFragment>(Bundle()) {
                (this as DeletionFragment)
                    .parentFragmentManager
                    .setFragmentResult(START_DELETION_KEY, bundleOf())
            }
            .use {
                onView(withText("Permanently delete selected data?"))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Connected apps will no longer be able to read this data from Health\u00A0Connect"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(withText("Cancel")).inRoot(isDialog()).check(matches(isDisplayed()))
                onView(withText("Delete")).inRoot(isDialog()).check(matches(isDisplayed()))
            }
    }

    @Test
    fun deletePermissionTypes_allSelected_confirmationDialog_showsCorrectText() {
        whenever(viewModel.deletionProgress).then {
            MutableLiveData(DeletionViewModel.DeletionProgress.NOT_STARTED)
        }
        whenever(viewModel.getDeletionType()).then {
            DeletionType.DeleteHealthPermissionTypes(
                setOf(FitnessPermissionType.STEPS, FitnessPermissionType.SPEED),
                totalPermissionTypes = 2,
            )
        }

        launchFragment<DeletionFragment>(Bundle()) {
                (this as DeletionFragment)
                    .parentFragmentManager
                    .setFragmentResult(START_DELETION_KEY, bundleOf())
            }
            .use {
                onView(withText("Permanently delete all data?"))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Connected apps will no longer be able to read this data from Health\u00A0Connect"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(withText("Cancel")).inRoot(isDialog()).check(matches(isDisplayed()))
                onView(withText("Delete")).inRoot(isDialog()).check(matches(isDisplayed()))
            }
    }

    // endregion

    // region DeletePermissionTypesFromApp
    @Test
    fun deletePermissionTypesFromApp_oneSelected_confirmationDialog_showsCorrectText() {
        whenever(viewModel.deletionProgress).then {
            MutableLiveData(DeletionViewModel.DeletionProgress.NOT_STARTED)
        }
        whenever(viewModel.getDeletionType()).then {
            DeletionType.DeleteHealthPermissionTypesFromApp(
                setOf(FitnessPermissionType.STEPS),
                totalPermissionTypes = 10,
                appName = TEST_APP_NAME,
                packageName = TEST_APP_PACKAGE_NAME,
            )
        }

        launchFragment<DeletionFragment>(Bundle()) {
                (this as DeletionFragment)
                    .parentFragmentManager
                    .setFragmentResult(START_DELETION_KEY, bundleOf())
            }
            .use {
                onView(withText("Permanently delete selected $TEST_APP_NAME data?"))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Connected apps will no longer be able to read this data from Health\u00A0Connect"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(withText("Also remove all $TEST_APP_NAME permissions from Health Connect"))
                    .check(matches(not(isDisplayed())))
                onView(withText("Cancel")).inRoot(isDialog()).check(matches(isDisplayed()))
                onView(withText("Delete")).inRoot(isDialog()).check(matches(isDisplayed()))
            }
    }

    @Test
    fun deletePermissionTypesFromApp_someSelected_confirmationDialog_showsCorrectText() {
        whenever(viewModel.deletionProgress).then {
            MutableLiveData(DeletionViewModel.DeletionProgress.NOT_STARTED)
        }
        whenever(viewModel.getDeletionType()).then {
            DeletionType.DeleteHealthPermissionTypesFromApp(
                setOf(FitnessPermissionType.STEPS, FitnessPermissionType.SPEED),
                totalPermissionTypes = 10,
                appName = TEST_APP_NAME,
                packageName = TEST_APP_PACKAGE_NAME,
            )
        }

        launchFragment<DeletionFragment>(Bundle()) {
                (this as DeletionFragment)
                    .parentFragmentManager
                    .setFragmentResult(START_DELETION_KEY, bundleOf())
            }
            .use {
                onView(withText("Permanently delete selected $TEST_APP_NAME data?"))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Connected apps will no longer be able to read this data from Health\u00A0Connect"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(withText("Also remove all $TEST_APP_NAME permissions from Health Connect"))
                    .check(matches(not(isDisplayed())))
                onView(withText("Cancel")).inRoot(isDialog()).check(matches(isDisplayed()))
                onView(withText("Delete")).inRoot(isDialog()).check(matches(isDisplayed()))
            }
    }

    @Test
    fun deletePermissionTypesFromApp_allSelected_confirmationDialog_showsCorrectText() {
        whenever(viewModel.deletionProgress).then {
            MutableLiveData(DeletionViewModel.DeletionProgress.NOT_STARTED)
        }
        whenever(viewModel.getDeletionType()).then {
            DeletionType.DeleteHealthPermissionTypesFromApp(
                setOf(FitnessPermissionType.STEPS, FitnessPermissionType.SPEED),
                totalPermissionTypes = 2,
                appName = TEST_APP_NAME,
                packageName = TEST_APP_PACKAGE_NAME,
            )
        }

        launchFragment<DeletionFragment>(Bundle()) {
                (this as DeletionFragment)
                    .parentFragmentManager
                    .setFragmentResult(START_DELETION_KEY, bundleOf())
            }
            .use {
                onView(withText("Permanently delete all $TEST_APP_NAME data?"))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Connected apps will no longer be able to read this data from Health\u00A0Connect"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(withText("Also remove all $TEST_APP_NAME permissions from Health Connect"))
                    .check(matches(isDisplayed()))
                onView(withText("Cancel")).inRoot(isDialog()).check(matches(isDisplayed()))
                onView(withText("Delete")).inRoot(isDialog()).check(matches(isDisplayed()))
            }
    }

    @Test
    fun deletePermissionTypesFromDDP_allSelected_confirmationDialog_showsCorrectTextNoCheckbox() {
        whenever(viewModel.deletionProgress).then {
            MutableLiveData(DeletionViewModel.DeletionProgress.NOT_STARTED)
        }
        whenever(viewModel.getDeletionType()).then {
            DeletionType.DeleteHealthPermissionTypesFromApp(
                setOf(FitnessPermissionType.STEPS),
                totalPermissionTypes = 1,
                appName = DEVICE_DATA_PROVIDER_APP_NAME,
                packageName = DEVICE_DATA_PROVIDER_PACKAGE_NAME,
            )
        }

        launchFragment<DeletionFragment>(Bundle()) {
                (this as DeletionFragment)
                    .parentFragmentManager
                    .setFragmentResult(START_DELETION_KEY, bundleOf())
            }
            .use {
                onView(withText("Permanently delete all $DEVICE_DATA_PROVIDER_APP_NAME data?"))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Connected apps will no longer be able to read this data from Health\u00A0Connect"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(withId(R.id.dialog_checkbox))
                    .inRoot(isDialog())
                    .check(matches(not(isDisplayed())))
                onView(withText("Cancel")).inRoot(isDialog()).check(matches(isDisplayed()))
                onView(withText("Delete")).inRoot(isDialog()).check(matches(isDisplayed()))
            }
    }

    // endregion

    // region DeleteEntries
    // region day
    @Test
    fun deleteEntries_fromDay_oneSelected_confirmationDialog_showsCorrectText() {
        val now = Instant.parse("2022-09-20T20:00:00.000Z")
        val selectedDay = Instant.parse("2022-09-19T20:00:00.000Z")
        (testTimeSource as TestTimeSource).setNow(now)
        whenever(viewModel.deletionProgress).then {
            MutableLiveData(DeletionViewModel.DeletionProgress.NOT_STARTED)
        }
        whenever(viewModel.getDeletionType()).then {
            DeletionType.DeleteEntries(
                mapOf("test_id_1" to StepsRecord::class),
                totalEntries = 10,
                period = DateNavigationPeriod.PERIOD_DAY,
                startTime = selectedDay,
            )
        }

        launchFragment<DeletionFragment>(Bundle()) {
                (this as DeletionFragment)
                    .parentFragmentManager
                    .setFragmentResult(START_DELETION_KEY, bundleOf())
            }
            .use {
                onView(withText("Permanently delete this entry?"))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Connected apps will no longer be able to read this data from Health\u00A0Connect"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(withText("Cancel")).inRoot(isDialog()).check(matches(isDisplayed()))
                onView(withText("Delete")).inRoot(isDialog()).check(matches(isDisplayed()))
            }
    }

    @Test
    fun deleteEntries_fromDayWithinYear_someSelected_confirmationDialog_showsCorrectText() {
        val now = Instant.parse("2022-09-20T20:00:00.000Z")
        val selectedDay = Instant.parse("2022-09-19T20:00:00.000Z")
        (testTimeSource as TestTimeSource).setNow(now)
        whenever(viewModel.deletionProgress).then {
            MutableLiveData(DeletionViewModel.DeletionProgress.NOT_STARTED)
        }
        whenever(viewModel.getDeletionType()).then {
            DeletionType.DeleteEntries(
                mapOf("test_id_1" to StepsRecord::class, "test_id_2" to StepsCadenceRecord::class),
                totalEntries = 10,
                period = DateNavigationPeriod.PERIOD_DAY,
                startTime = selectedDay,
            )
        }

        launchFragment<DeletionFragment>(Bundle()) {
                (this as DeletionFragment)
                    .parentFragmentManager
                    .setFragmentResult(START_DELETION_KEY, bundleOf())
            }
            .use {
                onView(withText("Permanently delete selected entries for Sep 19?"))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Connected apps will no longer be able to read this data from Health\u00A0Connect"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(withText("Cancel")).inRoot(isDialog()).check(matches(isDisplayed()))
                onView(withText("Delete")).inRoot(isDialog()).check(matches(isDisplayed()))
            }
    }

    @Test
    fun deleteEntries_fromDayWithinYear_1000Selected_confirmationDialog_showsCorrectText() {
        val now = Instant.parse("2022-09-20T20:00:00.000Z")
        val selectedDay = Instant.parse("2022-09-19T20:00:00.000Z")
        (testTimeSource as TestTimeSource).setNow(now)
        whenever(viewModel.deletionProgress).then {
            MutableLiveData(DeletionViewModel.DeletionProgress.NOT_STARTED)
        }
        whenever(viewModel.getDeletionType()).then {
            DeletionType.DeleteEntries(
                (1..1000).associate { "test_id_$it" to StepsRecord::class },
                totalEntries = 1000,
                period = DateNavigationPeriod.PERIOD_DAY,
                startTime = selectedDay,
            )
        }

        launchFragment<DeletionFragment>(Bundle()) {
                (this as DeletionFragment)
                    .parentFragmentManager
                    .setFragmentResult(START_DELETION_KEY, bundleOf())
            }
            .use {
                onView(withText("Permanently delete selected entries for Sep 19?"))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Connected apps will no longer be able to read this data from Health\u00A0Connect"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(withText("Cancel")).inRoot(isDialog()).check(matches(isDisplayed()))
                onView(withText("Delete")).inRoot(isDialog()).check(matches(isDisplayed()))
            }
    }

    @Test
    fun deleteEntries_fromDayPastYear_someSelected_confirmationDialog_showsCorrectText() {
        val now = Instant.parse("2022-09-20T20:00:00.000Z")
        val selectedDay = Instant.parse("2021-09-19T20:00:00.000Z")
        (testTimeSource as TestTimeSource).setNow(now)
        whenever(viewModel.deletionProgress).then {
            MutableLiveData(DeletionViewModel.DeletionProgress.NOT_STARTED)
        }
        whenever(viewModel.getDeletionType()).then {
            DeletionType.DeleteEntries(
                mapOf("test_id_1" to StepsRecord::class, "test_id_2" to StepsCadenceRecord::class),
                totalEntries = 10,
                period = DateNavigationPeriod.PERIOD_DAY,
                startTime = selectedDay,
            )
        }

        launchFragment<DeletionFragment>(Bundle()) {
                (this as DeletionFragment)
                    .parentFragmentManager
                    .setFragmentResult(START_DELETION_KEY, bundleOf())
            }
            .use {
                onView(withText("Permanently delete selected entries for Sep 19, 2021?"))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Connected apps will no longer be able to read this data from Health\u00A0Connect"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(withText("Cancel")).inRoot(isDialog()).check(matches(isDisplayed()))
                onView(withText("Delete")).inRoot(isDialog()).check(matches(isDisplayed()))
            }
    }

    @Test
    fun deleteEntries_fromDayPastYear_1000Selected_confirmationDialog_showsCorrectText() {
        val now = Instant.parse("2022-09-20T20:00:00.000Z")
        val selectedDay = Instant.parse("2021-09-19T20:00:00.000Z")
        (testTimeSource as TestTimeSource).setNow(now)
        whenever(viewModel.deletionProgress).then {
            MutableLiveData(DeletionViewModel.DeletionProgress.NOT_STARTED)
        }
        whenever(viewModel.getDeletionType()).then {
            DeletionType.DeleteEntries(
                (1..1000).associate { "test_id_$it" to StepsRecord::class },
                totalEntries = 1000,
                period = DateNavigationPeriod.PERIOD_DAY,
                startTime = selectedDay,
            )
        }

        launchFragment<DeletionFragment>(Bundle()) {
                (this as DeletionFragment)
                    .parentFragmentManager
                    .setFragmentResult(START_DELETION_KEY, bundleOf())
            }
            .use {
                onView(withText("Permanently delete selected entries for Sep 19, 2021?"))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Connected apps will no longer be able to read this data from Health\u00A0Connect"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(withText("Cancel")).inRoot(isDialog()).check(matches(isDisplayed()))
                onView(withText("Delete")).inRoot(isDialog()).check(matches(isDisplayed()))
            }
    }

    @Test
    fun deleteEntries_fromDayWithinYear_allSelected_confirmationDialog_showsCorrectText() {
        val now = Instant.parse("2022-09-20T20:00:00.000Z")
        val selectedDay = Instant.parse("2022-09-19T20:00:00.000Z")
        (testTimeSource as TestTimeSource).setNow(now)
        whenever(viewModel.deletionProgress).then {
            MutableLiveData(DeletionViewModel.DeletionProgress.NOT_STARTED)
        }
        whenever(viewModel.getDeletionType()).then {
            DeletionType.DeleteEntries(
                mapOf("test_id_1" to StepsRecord::class, "test_id_2" to StepsCadenceRecord::class),
                totalEntries = 2,
                period = DateNavigationPeriod.PERIOD_DAY,
                startTime = selectedDay,
            )
        }

        launchFragment<DeletionFragment>(Bundle()) {
                (this as DeletionFragment)
                    .parentFragmentManager
                    .setFragmentResult(START_DELETION_KEY, bundleOf())
            }
            .use {
                onView(withText("Permanently delete all entries for Sep 19?"))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Connected apps will no longer be able to read this data from Health\u00A0Connect"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(withText("Cancel")).inRoot(isDialog()).check(matches(isDisplayed()))
                onView(withText("Delete")).inRoot(isDialog()).check(matches(isDisplayed()))
            }
    }

    @Test
    fun deleteEntries_fromDayPastYear_allSelected_confirmationDialog_showsCorrectText() {
        val now = Instant.parse("2022-09-20T20:00:00.000Z")
        val selectedDay = Instant.parse("2021-09-19T20:00:00.000Z")
        (testTimeSource as TestTimeSource).setNow(now)
        whenever(viewModel.deletionProgress).then {
            MutableLiveData(DeletionViewModel.DeletionProgress.NOT_STARTED)
        }
        whenever(viewModel.getDeletionType()).then {
            DeletionType.DeleteEntries(
                mapOf("test_id_1" to StepsRecord::class, "test_id_2" to StepsCadenceRecord::class),
                totalEntries = 2,
                period = DateNavigationPeriod.PERIOD_DAY,
                startTime = selectedDay,
            )
        }

        launchFragment<DeletionFragment>(Bundle()) {
                (this as DeletionFragment)
                    .parentFragmentManager
                    .setFragmentResult(START_DELETION_KEY, bundleOf())
            }
            .use {
                onView(withText("Permanently delete all entries for Sep 19, 2021?"))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Connected apps will no longer be able to read this data from Health\u00A0Connect"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(withText("Cancel")).inRoot(isDialog()).check(matches(isDisplayed()))
                onView(withText("Delete")).inRoot(isDialog()).check(matches(isDisplayed()))
            }
    }

    // endregion

    // region week
    @Test
    fun deleteEntries_fromWeek_oneSelected_confirmationDialog_showsCorrectText() {
        val now = Instant.parse("2022-09-20T20:00:00.000Z")
        val selectedDay = Instant.parse("2022-09-19T20:00:00.000Z")
        (testTimeSource as TestTimeSource).setNow(now)
        whenever(viewModel.deletionProgress).then {
            MutableLiveData(DeletionViewModel.DeletionProgress.NOT_STARTED)
        }
        whenever(viewModel.getDeletionType()).then {
            DeletionType.DeleteEntries(
                mapOf("test_id_1" to StepsRecord::class),
                totalEntries = 10,
                period = DateNavigationPeriod.PERIOD_WEEK,
                startTime = selectedDay,
            )
        }

        launchFragment<DeletionFragment>(Bundle()) {
                (this as DeletionFragment)
                    .parentFragmentManager
                    .setFragmentResult(START_DELETION_KEY, bundleOf())
            }
            .use {
                onView(withText("Permanently delete this entry?"))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Connected apps will no longer be able to read this data from Health\u00A0Connect"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(withText("Cancel")).inRoot(isDialog()).check(matches(isDisplayed()))
                onView(withText("Delete")).inRoot(isDialog()).check(matches(isDisplayed()))
            }
    }

    @Test
    fun deleteEntries_fromWeekWithinYear_someSelected_confirmationDialog_showsCorrectText() {
        val now = Instant.parse("2022-09-19T20:00:00.000Z")
        // This date is on a Sunday. The dialog should show the current week range,
        // which starts on this Sunday
        val selectedDay = Instant.parse("2022-09-18T20:00:00.000Z")
        (testTimeSource as TestTimeSource).setNow(now)
        whenever(viewModel.deletionProgress).then {
            MutableLiveData(DeletionViewModel.DeletionProgress.NOT_STARTED)
        }
        whenever(viewModel.getDeletionType()).then {
            DeletionType.DeleteEntries(
                mapOf("test_id_1" to StepsRecord::class, "test_id_2" to StepsCadenceRecord::class),
                totalEntries = 10,
                period = DateNavigationPeriod.PERIOD_WEEK,
                startTime = selectedDay,
            )
        }

        launchFragment<DeletionFragment>(Bundle()) {
                (this as DeletionFragment)
                    .parentFragmentManager
                    .setFragmentResult(START_DELETION_KEY, bundleOf())
            }
            .use {
                onView(withText("Permanently delete selected entries for the week of Sep 18 – 24?"))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Connected apps will no longer be able to read this data from Health\u00A0Connect"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(withText("Cancel")).inRoot(isDialog()).check(matches(isDisplayed()))
                onView(withText("Delete")).inRoot(isDialog()).check(matches(isDisplayed()))
            }
    }

    @Test
    fun deleteEntries_fromWeekWithinYear_1000Selected_confirmationDialog_showsCorrectText() {
        val now = Instant.parse("2022-09-19T20:00:00.000Z")
        // This date is on a Sunday. The dialog should show the current week range,
        // which starts on this Sunday
        val selectedDay = Instant.parse("2022-09-18T20:00:00.000Z")
        (testTimeSource as TestTimeSource).setNow(now)
        whenever(viewModel.deletionProgress).then {
            MutableLiveData(DeletionViewModel.DeletionProgress.NOT_STARTED)
        }
        whenever(viewModel.getDeletionType()).then {
            DeletionType.DeleteEntries(
                (1..1000).associate { "test_id_$it" to StepsRecord::class },
                totalEntries = 1000,
                period = DateNavigationPeriod.PERIOD_WEEK,
                startTime = selectedDay,
            )
        }

        launchFragment<DeletionFragment>(Bundle()) {
                (this as DeletionFragment)
                    .parentFragmentManager
                    .setFragmentResult(START_DELETION_KEY, bundleOf())
            }
            .use {
                onView(withText("Permanently delete selected entries for the week of Sep 18 – 24?"))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Connected apps will no longer be able to read this data from Health\u00A0Connect"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(withText("Cancel")).inRoot(isDialog()).check(matches(isDisplayed()))
                onView(withText("Delete")).inRoot(isDialog()).check(matches(isDisplayed()))
            }
    }

    @Test
    fun deleteEntries_fromWeekPastYear_someSelected_confirmationDialog_showsCorrectText() {
        val now = Instant.parse("2022-09-19T20:00:00.000Z")
        // This date is on a Saturday. The dialog should show the current week range,
        // which ends on this Saturday
        val selectedDay = Instant.parse("2021-09-18T20:00:00.000Z")
        (testTimeSource as TestTimeSource).setNow(now)
        whenever(viewModel.deletionProgress).then {
            MutableLiveData(DeletionViewModel.DeletionProgress.NOT_STARTED)
        }
        whenever(viewModel.getDeletionType()).then {
            DeletionType.DeleteEntries(
                mapOf("test_id_1" to StepsRecord::class, "test_id_2" to StepsCadenceRecord::class),
                totalEntries = 10,
                period = DateNavigationPeriod.PERIOD_WEEK,
                startTime = selectedDay,
            )
        }

        launchFragment<DeletionFragment>(Bundle()) {
                (this as DeletionFragment)
                    .parentFragmentManager
                    .setFragmentResult(START_DELETION_KEY, bundleOf())
            }
            .use {
                onView(
                        withText(
                            "Permanently delete selected entries for the week of Sep 12 – 18, 2021?"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Connected apps will no longer be able to read this data from Health\u00A0Connect"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(withText("Cancel")).inRoot(isDialog()).check(matches(isDisplayed()))
                onView(withText("Delete")).inRoot(isDialog()).check(matches(isDisplayed()))
            }
    }

    @Test
    fun deleteEntries_fromWeekPastYear_1000Selected_confirmationDialog_showsCorrectText() {
        val now = Instant.parse("2022-09-19T20:00:00.000Z")
        // This date is on a Saturday. The dialog should show the current week range,
        // which ends on this Saturday
        val selectedDay = Instant.parse("2021-09-18T20:00:00.000Z")
        (testTimeSource as TestTimeSource).setNow(now)
        whenever(viewModel.deletionProgress).then {
            MutableLiveData(DeletionViewModel.DeletionProgress.NOT_STARTED)
        }
        whenever(viewModel.getDeletionType()).then {
            DeletionType.DeleteEntries(
                (1..1000).associate { "test_id_$it" to StepsRecord::class },
                totalEntries = 1000,
                period = DateNavigationPeriod.PERIOD_WEEK,
                startTime = selectedDay,
            )
        }

        launchFragment<DeletionFragment>(Bundle()) {
                (this as DeletionFragment)
                    .parentFragmentManager
                    .setFragmentResult(START_DELETION_KEY, bundleOf())
            }
            .use {
                onView(
                        withText(
                            "Permanently delete selected entries for the week of Sep 12 – 18, 2021?"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Connected apps will no longer be able to read this data from Health\u00A0Connect"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(withText("Cancel")).inRoot(isDialog()).check(matches(isDisplayed()))
                onView(withText("Delete")).inRoot(isDialog()).check(matches(isDisplayed()))
            }
    }

    @Test
    fun deleteEntries_fromWeekWithinYear_allSelected_confirmationDialog_showsCorrectText() {
        val now = Instant.parse("2022-09-19T20:00:00.000Z")
        // This date is on a Sunday. The dialog should show the current week range,
        // which starts on this Sunday
        val selectedDay = Instant.parse("2022-09-18T20:00:00.000Z")
        (testTimeSource as TestTimeSource).setNow(now)
        whenever(viewModel.deletionProgress).then {
            MutableLiveData(DeletionViewModel.DeletionProgress.NOT_STARTED)
        }
        whenever(viewModel.getDeletionType()).then {
            DeletionType.DeleteEntries(
                mapOf("test_id_1" to StepsRecord::class, "test_id_2" to StepsCadenceRecord::class),
                totalEntries = 2,
                period = DateNavigationPeriod.PERIOD_WEEK,
                startTime = selectedDay,
            )
        }

        launchFragment<DeletionFragment>(Bundle()) {
                (this as DeletionFragment)
                    .parentFragmentManager
                    .setFragmentResult(START_DELETION_KEY, bundleOf())
            }
            .use {
                onView(withText("Permanently delete all entries for the week of Sep 18 – 24?"))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Connected apps will no longer be able to read this data from Health\u00A0Connect"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(withText("Cancel")).inRoot(isDialog()).check(matches(isDisplayed()))
                onView(withText("Delete")).inRoot(isDialog()).check(matches(isDisplayed()))
            }
    }

    @Test
    fun deleteEntries_fromWeekPastYear_allSelected_confirmationDialog_showsCorrectText() {
        val now = Instant.parse("2022-09-19T20:00:00.000Z")
        // This date is on a Saturday. The dialog should show the current week range,
        // which ends on this Saturday
        val selectedDay = Instant.parse("2021-09-18T20:00:00.000Z")
        (testTimeSource as TestTimeSource).setNow(now)
        whenever(viewModel.deletionProgress).then {
            MutableLiveData(DeletionViewModel.DeletionProgress.NOT_STARTED)
        }
        whenever(viewModel.getDeletionType()).then {
            DeletionType.DeleteEntries(
                mapOf("test_id_1" to StepsRecord::class, "test_id_2" to StepsCadenceRecord::class),
                totalEntries = 2,
                period = DateNavigationPeriod.PERIOD_WEEK,
                startTime = selectedDay,
            )
        }

        launchFragment<DeletionFragment>(Bundle()) {
                (this as DeletionFragment)
                    .parentFragmentManager
                    .setFragmentResult(START_DELETION_KEY, bundleOf())
            }
            .use {
                onView(
                        withText(
                            "Permanently delete all entries for the week of Sep 12 – 18, 2021?"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Connected apps will no longer be able to read this data from Health\u00A0Connect"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(withText("Cancel")).inRoot(isDialog()).check(matches(isDisplayed()))
                onView(withText("Delete")).inRoot(isDialog()).check(matches(isDisplayed()))
            }
    }

    // endregion

    // region month
    @Test
    fun deleteEntries_fromMonth_oneSelected_confirmationDialog_showsCorrectText() {
        val now = Instant.parse("2022-09-20T20:00:00.000Z")
        val selectedDay = Instant.parse("2022-09-19T20:00:00.000Z")
        (testTimeSource as TestTimeSource).setNow(now)
        whenever(viewModel.deletionProgress).then {
            MutableLiveData(DeletionViewModel.DeletionProgress.NOT_STARTED)
        }
        whenever(viewModel.getDeletionType()).then {
            DeletionType.DeleteEntries(
                mapOf("test_id_1" to StepsRecord::class),
                totalEntries = 10,
                period = DateNavigationPeriod.PERIOD_MONTH,
                startTime = selectedDay,
            )
        }

        launchFragment<DeletionFragment>(Bundle()) {
                (this as DeletionFragment)
                    .parentFragmentManager
                    .setFragmentResult(START_DELETION_KEY, bundleOf())
            }
            .use {
                onView(withText("Permanently delete this entry?"))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Connected apps will no longer be able to read this data from Health\u00A0Connect"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(withText("Cancel")).inRoot(isDialog()).check(matches(isDisplayed()))
                onView(withText("Delete")).inRoot(isDialog()).check(matches(isDisplayed()))
            }
    }

    @Test
    fun deleteEntries_fromMonthWithinYear_someSelected_confirmationDialog_showsCorrectText() {
        val now = Instant.parse("2022-09-20T20:00:00.000Z")
        val selectedDay = Instant.parse("2022-09-19T20:00:00.000Z")
        (testTimeSource as TestTimeSource).setNow(now)
        whenever(viewModel.deletionProgress).then {
            MutableLiveData(DeletionViewModel.DeletionProgress.NOT_STARTED)
        }
        whenever(viewModel.getDeletionType()).then {
            DeletionType.DeleteEntries(
                mapOf("test_id_1" to StepsRecord::class, "test_id_2" to StepsCadenceRecord::class),
                totalEntries = 10,
                period = DateNavigationPeriod.PERIOD_MONTH,
                startTime = selectedDay,
            )
        }

        launchFragment<DeletionFragment>(Bundle()) {
                (this as DeletionFragment)
                    .parentFragmentManager
                    .setFragmentResult(START_DELETION_KEY, bundleOf())
            }
            .use {
                onView(withText("Permanently delete selected entries for September?"))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Connected apps will no longer be able to read this data from Health\u00A0Connect"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(withText("Cancel")).inRoot(isDialog()).check(matches(isDisplayed()))
                onView(withText("Delete")).inRoot(isDialog()).check(matches(isDisplayed()))
            }
    }

    @Test
    fun deleteEntries_fromMonthWithinYear_1000Selected_confirmationDialog_showsCorrectText() {
        val now = Instant.parse("2022-09-20T20:00:00.000Z")
        val selectedDay = Instant.parse("2022-09-19T20:00:00.000Z")
        (testTimeSource as TestTimeSource).setNow(now)
        whenever(viewModel.deletionProgress).then {
            MutableLiveData(DeletionViewModel.DeletionProgress.NOT_STARTED)
        }
        whenever(viewModel.getDeletionType()).then {
            DeletionType.DeleteEntries(
                (1..1000).associate { "test_id_$it" to StepsRecord::class },
                totalEntries = 1000,
                period = DateNavigationPeriod.PERIOD_MONTH,
                startTime = selectedDay,
            )
        }

        launchFragment<DeletionFragment>(Bundle()) {
                (this as DeletionFragment)
                    .parentFragmentManager
                    .setFragmentResult(START_DELETION_KEY, bundleOf())
            }
            .use {
                onView(withText("Permanently delete selected entries for September?"))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Connected apps will no longer be able to read this data from Health\u00A0Connect"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(withText("Cancel")).inRoot(isDialog()).check(matches(isDisplayed()))
                onView(withText("Delete")).inRoot(isDialog()).check(matches(isDisplayed()))
            }
    }

    @Test
    fun deleteEntries_fromMonthPastYear_someSelected_confirmationDialog_showsCorrectText() {
        val now = Instant.parse("2022-09-20T20:00:00.000Z")
        val selectedDay = Instant.parse("2021-09-19T20:00:00.000Z")
        (testTimeSource as TestTimeSource).setNow(now)
        whenever(viewModel.deletionProgress).then {
            MutableLiveData(DeletionViewModel.DeletionProgress.NOT_STARTED)
        }
        whenever(viewModel.getDeletionType()).then {
            DeletionType.DeleteEntries(
                mapOf("test_id_1" to StepsRecord::class, "test_id_2" to StepsCadenceRecord::class),
                totalEntries = 10,
                period = DateNavigationPeriod.PERIOD_MONTH,
                startTime = selectedDay,
            )
        }

        launchFragment<DeletionFragment>(Bundle()) {
                (this as DeletionFragment)
                    .parentFragmentManager
                    .setFragmentResult(START_DELETION_KEY, bundleOf())
            }
            .use {
                onView(withText("Permanently delete selected entries for September 2021?"))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Connected apps will no longer be able to read this data from Health\u00A0Connect"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(withText("Cancel")).inRoot(isDialog()).check(matches(isDisplayed()))
                onView(withText("Delete")).inRoot(isDialog()).check(matches(isDisplayed()))
            }
    }

    @Test
    fun deleteEntries_fromMonthPastYear_1000Selected_confirmationDialog_showsCorrectText() {
        val now = Instant.parse("2022-09-20T20:00:00.000Z")
        val selectedDay = Instant.parse("2021-09-19T20:00:00.000Z")
        (testTimeSource as TestTimeSource).setNow(now)
        whenever(viewModel.deletionProgress).then {
            MutableLiveData(DeletionViewModel.DeletionProgress.NOT_STARTED)
        }
        whenever(viewModel.getDeletionType()).then {
            DeletionType.DeleteEntries(
                (1..1000).associate { "test_id_$it" to StepsRecord::class },
                totalEntries = 1000,
                period = DateNavigationPeriod.PERIOD_MONTH,
                startTime = selectedDay,
            )
        }

        launchFragment<DeletionFragment>(Bundle()) {
                (this as DeletionFragment)
                    .parentFragmentManager
                    .setFragmentResult(START_DELETION_KEY, bundleOf())
            }
            .use {
                onView(withText("Permanently delete selected entries for September 2021?"))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Connected apps will no longer be able to read this data from Health\u00A0Connect"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(withText("Cancel")).inRoot(isDialog()).check(matches(isDisplayed()))
                onView(withText("Delete")).inRoot(isDialog()).check(matches(isDisplayed()))
            }
    }

    @Test
    fun deleteEntries_fromMonthWithinYear_allSelected_confirmationDialog_showsCorrectText() {
        val now = Instant.parse("2022-09-20T20:00:00.000Z")
        val selectedDay = Instant.parse("2022-09-19T20:00:00.000Z")
        (testTimeSource as TestTimeSource).setNow(now)
        whenever(viewModel.deletionProgress).then {
            MutableLiveData(DeletionViewModel.DeletionProgress.NOT_STARTED)
        }
        whenever(viewModel.getDeletionType()).then {
            DeletionType.DeleteEntries(
                mapOf("test_id_1" to StepsRecord::class, "test_id_2" to StepsCadenceRecord::class),
                totalEntries = 2,
                period = DateNavigationPeriod.PERIOD_MONTH,
                startTime = selectedDay,
            )
        }

        launchFragment<DeletionFragment>(Bundle()) {
                (this as DeletionFragment)
                    .parentFragmentManager
                    .setFragmentResult(START_DELETION_KEY, bundleOf())
            }
            .use {
                onView(withText("Permanently delete all entries for September?"))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Connected apps will no longer be able to read this data from Health\u00A0Connect"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(withText("Cancel")).inRoot(isDialog()).check(matches(isDisplayed()))
                onView(withText("Delete")).inRoot(isDialog()).check(matches(isDisplayed()))
            }
    }

    @Test
    fun deleteEntries_fromMonthPastYear_allSelected_confirmationDialog_showsCorrectText() {
        val now = Instant.parse("2022-09-20T20:00:00.000Z")
        val selectedDay = Instant.parse("2021-09-19T20:00:00.000Z")
        (testTimeSource as TestTimeSource).setNow(now)
        whenever(viewModel.deletionProgress).then {
            MutableLiveData(DeletionViewModel.DeletionProgress.NOT_STARTED)
        }
        whenever(viewModel.getDeletionType()).then {
            DeletionType.DeleteEntries(
                mapOf("test_id_1" to StepsRecord::class, "test_id_2" to StepsCadenceRecord::class),
                totalEntries = 2,
                period = DateNavigationPeriod.PERIOD_MONTH,
                startTime = selectedDay,
            )
        }

        launchFragment<DeletionFragment>(Bundle()) {
                (this as DeletionFragment)
                    .parentFragmentManager
                    .setFragmentResult(START_DELETION_KEY, bundleOf())
            }
            .use {
                onView(withText("Permanently delete all entries for September 2021?"))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Connected apps will no longer be able to read this data from Health\u00A0Connect"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(withText("Cancel")).inRoot(isDialog()).check(matches(isDisplayed()))
                onView(withText("Delete")).inRoot(isDialog()).check(matches(isDisplayed()))
            }
    }

    // endregion

    // endregion

    // region DeleteEntriesFromApp

    // region day
    @Test
    fun deleteAppEntries_fromDay_oneSelected_confirmationDialog_showsCorrectText() {
        val now = Instant.parse("2022-09-20T20:00:00.000Z")
        val selectedDay = Instant.parse("2022-09-19T20:00:00.000Z")
        (testTimeSource as TestTimeSource).setNow(now)
        whenever(viewModel.deletionProgress).then {
            MutableLiveData(DeletionViewModel.DeletionProgress.NOT_STARTED)
        }
        whenever(viewModel.getDeletionType()).then {
            DeletionType.DeleteEntriesFromApp(
                mapOf("test_id_1" to StepsRecord::class),
                TEST_APP_PACKAGE_NAME,
                TEST_APP_NAME,
                totalEntries = 10,
                period = DateNavigationPeriod.PERIOD_DAY,
                startTime = selectedDay,
            )
        }

        launchFragment<DeletionFragment>(Bundle()) {
                (this as DeletionFragment)
                    .parentFragmentManager
                    .setFragmentResult(START_DELETION_KEY, bundleOf())
            }
            .use {
                onView(withText("Permanently delete this $TEST_APP_NAME entry?"))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Connected apps will no longer be able to read this data from Health\u00A0Connect"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(withText("Cancel")).inRoot(isDialog()).check(matches(isDisplayed()))
                onView(withText("Delete")).inRoot(isDialog()).check(matches(isDisplayed()))
            }
    }

    @Test
    fun deleteAppEntries_fromDayWithinYear_someSelected_confirmationDialog_showsCorrectText() {
        val now = Instant.parse("2022-09-20T20:00:00.000Z")
        val selectedDay = Instant.parse("2022-09-19T20:00:00.000Z")
        (testTimeSource as TestTimeSource).setNow(now)
        whenever(viewModel.deletionProgress).then {
            MutableLiveData(DeletionViewModel.DeletionProgress.NOT_STARTED)
        }
        whenever(viewModel.getDeletionType()).then {
            DeletionType.DeleteEntriesFromApp(
                mapOf("test_id_1" to StepsRecord::class, "test_id_2" to StepsCadenceRecord::class),
                TEST_APP_PACKAGE_NAME,
                TEST_APP_NAME,
                totalEntries = 10,
                period = DateNavigationPeriod.PERIOD_DAY,
                startTime = selectedDay,
            )
        }

        launchFragment<DeletionFragment>(Bundle()) {
                (this as DeletionFragment)
                    .parentFragmentManager
                    .setFragmentResult(START_DELETION_KEY, bundleOf())
            }
            .use {
                onView(withText("Permanently delete selected $TEST_APP_NAME entries for Sep 19?"))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Connected apps will no longer be able to read this data from Health\u00A0Connect"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(withText("Cancel")).inRoot(isDialog()).check(matches(isDisplayed()))
                onView(withText("Delete")).inRoot(isDialog()).check(matches(isDisplayed()))
            }
    }

    @Test
    fun deleteAppEntries_fromDayWithinYear_1000Selected_confirmationDialog_showsCorrectText() {
        val now = Instant.parse("2022-09-20T20:00:00.000Z")
        val selectedDay = Instant.parse("2022-09-19T20:00:00.000Z")
        (testTimeSource as TestTimeSource).setNow(now)
        whenever(viewModel.deletionProgress).then {
            MutableLiveData(DeletionViewModel.DeletionProgress.NOT_STARTED)
        }
        whenever(viewModel.getDeletionType()).then {
            DeletionType.DeleteEntriesFromApp(
                (1..1000).associate { "test_id_$it" to StepsRecord::class },
                TEST_APP_PACKAGE_NAME,
                TEST_APP_NAME,
                totalEntries = 1000,
                period = DateNavigationPeriod.PERIOD_DAY,
                startTime = selectedDay,
            )
        }

        launchFragment<DeletionFragment>(Bundle()) {
                (this as DeletionFragment)
                    .parentFragmentManager
                    .setFragmentResult(START_DELETION_KEY, bundleOf())
            }
            .use {
                onView(withText("Permanently delete selected $TEST_APP_NAME entries for Sep 19?"))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Connected apps will no longer be able to read this data from Health\u00A0Connect"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(withText("Cancel")).inRoot(isDialog()).check(matches(isDisplayed()))
                onView(withText("Delete")).inRoot(isDialog()).check(matches(isDisplayed()))
            }
    }

    @Test
    fun deleteAppEntries_fromDayPastYear_someSelected_confirmationDialog_showsCorrectText() {
        val now = Instant.parse("2022-09-20T20:00:00.000Z")
        val selectedDay = Instant.parse("2021-09-19T20:00:00.000Z")
        (testTimeSource as TestTimeSource).setNow(now)
        whenever(viewModel.deletionProgress).then {
            MutableLiveData(DeletionViewModel.DeletionProgress.NOT_STARTED)
        }
        whenever(viewModel.getDeletionType()).then {
            DeletionType.DeleteEntriesFromApp(
                mapOf("test_id_1" to StepsRecord::class, "test_id_2" to StepsCadenceRecord::class),
                TEST_APP_PACKAGE_NAME,
                TEST_APP_NAME,
                totalEntries = 10,
                period = DateNavigationPeriod.PERIOD_DAY,
                startTime = selectedDay,
            )
        }

        launchFragment<DeletionFragment>(Bundle()) {
                (this as DeletionFragment)
                    .parentFragmentManager
                    .setFragmentResult(START_DELETION_KEY, bundleOf())
            }
            .use {
                onView(
                        withText(
                            "Permanently delete selected $TEST_APP_NAME entries for Sep 19, 2021?"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Connected apps will no longer be able to read this data from Health\u00A0Connect"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(withText("Cancel")).inRoot(isDialog()).check(matches(isDisplayed()))
                onView(withText("Delete")).inRoot(isDialog()).check(matches(isDisplayed()))
            }
    }

    @Test
    fun deleteAppEntries_fromDayPastYear_1000Selected_confirmationDialog_showsCorrectText() {
        val now = Instant.parse("2022-09-20T20:00:00.000Z")
        val selectedDay = Instant.parse("2021-09-19T20:00:00.000Z")
        (testTimeSource as TestTimeSource).setNow(now)
        whenever(viewModel.deletionProgress).then {
            MutableLiveData(DeletionViewModel.DeletionProgress.NOT_STARTED)
        }
        whenever(viewModel.getDeletionType()).then {
            DeletionType.DeleteEntriesFromApp(
                (1..1000).associate { "test_id_$it" to StepsRecord::class },
                TEST_APP_PACKAGE_NAME,
                TEST_APP_NAME,
                totalEntries = 1000,
                period = DateNavigationPeriod.PERIOD_DAY,
                startTime = selectedDay,
            )
        }

        launchFragment<DeletionFragment>(Bundle()) {
                (this as DeletionFragment)
                    .parentFragmentManager
                    .setFragmentResult(START_DELETION_KEY, bundleOf())
            }
            .use {
                onView(
                        withText(
                            "Permanently delete selected $TEST_APP_NAME entries for Sep 19, 2021?"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Connected apps will no longer be able to read this data from Health\u00A0Connect"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(withText("Cancel")).inRoot(isDialog()).check(matches(isDisplayed()))
                onView(withText("Delete")).inRoot(isDialog()).check(matches(isDisplayed()))
            }
    }

    @Test
    fun deleteAppEntries_fromDayWithinYear_allSelected_confirmationDialog_showsCorrectText() {
        val now = Instant.parse("2022-09-20T20:00:00.000Z")
        val selectedDay = Instant.parse("2022-09-19T20:00:00.000Z")
        (testTimeSource as TestTimeSource).setNow(now)
        whenever(viewModel.deletionProgress).then {
            MutableLiveData(DeletionViewModel.DeletionProgress.NOT_STARTED)
        }
        whenever(viewModel.getDeletionType()).then {
            DeletionType.DeleteEntriesFromApp(
                mapOf("test_id_1" to StepsRecord::class, "test_id_2" to StepsCadenceRecord::class),
                TEST_APP_PACKAGE_NAME,
                TEST_APP_NAME,
                totalEntries = 2,
                period = DateNavigationPeriod.PERIOD_DAY,
                startTime = selectedDay,
            )
        }

        launchFragment<DeletionFragment>(Bundle()) {
                (this as DeletionFragment)
                    .parentFragmentManager
                    .setFragmentResult(START_DELETION_KEY, bundleOf())
            }
            .use {
                onView(withText("Permanently delete all $TEST_APP_NAME entries for Sep 19?"))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Connected apps will no longer be able to read this data from Health\u00A0Connect"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(withText("Cancel")).inRoot(isDialog()).check(matches(isDisplayed()))
                onView(withText("Delete")).inRoot(isDialog()).check(matches(isDisplayed()))
            }
    }

    @Test
    fun deleteAppEntries_fromDayPastYear_allSelected_confirmationDialog_showsCorrectText() {
        val now = Instant.parse("2022-09-20T20:00:00.000Z")
        val selectedDay = Instant.parse("2021-09-19T20:00:00.000Z")
        (testTimeSource as TestTimeSource).setNow(now)
        whenever(viewModel.deletionProgress).then {
            MutableLiveData(DeletionViewModel.DeletionProgress.NOT_STARTED)
        }
        whenever(viewModel.getDeletionType()).then {
            DeletionType.DeleteEntriesFromApp(
                mapOf("test_id_1" to StepsRecord::class, "test_id_2" to StepsCadenceRecord::class),
                TEST_APP_PACKAGE_NAME,
                TEST_APP_NAME,
                totalEntries = 2,
                period = DateNavigationPeriod.PERIOD_DAY,
                startTime = selectedDay,
            )
        }

        launchFragment<DeletionFragment>(Bundle()) {
                (this as DeletionFragment)
                    .parentFragmentManager
                    .setFragmentResult(START_DELETION_KEY, bundleOf())
            }
            .use {
                onView(withText("Permanently delete all $TEST_APP_NAME entries for Sep 19, 2021?"))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Connected apps will no longer be able to read this data from Health\u00A0Connect"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(withText("Cancel")).inRoot(isDialog()).check(matches(isDisplayed()))
                onView(withText("Delete")).inRoot(isDialog()).check(matches(isDisplayed()))
            }
    }

    // endregion

    // region week
    @Test
    fun deleteAppEntries_fromWeek_oneSelected_confirmationDialog_showsCorrectText() {
        val now = Instant.parse("2022-09-20T20:00:00.000Z")
        val selectedDay = Instant.parse("2022-09-19T20:00:00.000Z")
        (testTimeSource as TestTimeSource).setNow(now)
        whenever(viewModel.deletionProgress).then {
            MutableLiveData(DeletionViewModel.DeletionProgress.NOT_STARTED)
        }
        whenever(viewModel.getDeletionType()).then {
            DeletionType.DeleteEntriesFromApp(
                mapOf("test_id_1" to StepsRecord::class),
                TEST_APP_PACKAGE_NAME,
                TEST_APP_NAME,
                totalEntries = 10,
                period = DateNavigationPeriod.PERIOD_WEEK,
                startTime = selectedDay,
            )
        }

        launchFragment<DeletionFragment>(Bundle()) {
                (this as DeletionFragment)
                    .parentFragmentManager
                    .setFragmentResult(START_DELETION_KEY, bundleOf())
            }
            .use {
                onView(withText("Permanently delete this $TEST_APP_NAME entry?"))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Connected apps will no longer be able to read this data from Health\u00A0Connect"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(withText("Cancel")).inRoot(isDialog()).check(matches(isDisplayed()))
                onView(withText("Delete")).inRoot(isDialog()).check(matches(isDisplayed()))
            }
    }

    @Test
    fun deleteAppEntries_fromWeekWithinYear_someSelected_confirmationDialog_showsCorrectText() {
        val now = Instant.parse("2022-09-19T20:00:00.000Z")
        // This date is on a Sunday. The dialog should show the current week range,
        // which starts on this SUnday
        val selectedDay = Instant.parse("2022-09-18T20:00:00.000Z")
        (testTimeSource as TestTimeSource).setNow(now)
        whenever(viewModel.deletionProgress).then {
            MutableLiveData(DeletionViewModel.DeletionProgress.NOT_STARTED)
        }
        whenever(viewModel.getDeletionType()).then {
            DeletionType.DeleteEntriesFromApp(
                mapOf("test_id_1" to StepsRecord::class, "test_id_2" to StepsCadenceRecord::class),
                TEST_APP_PACKAGE_NAME,
                TEST_APP_NAME,
                totalEntries = 10,
                period = DateNavigationPeriod.PERIOD_WEEK,
                startTime = selectedDay,
            )
        }

        launchFragment<DeletionFragment>(Bundle()) {
                (this as DeletionFragment)
                    .parentFragmentManager
                    .setFragmentResult(START_DELETION_KEY, bundleOf())
            }
            .use {
                onView(
                        withText(
                            "Permanently delete selected $TEST_APP_NAME entries for the week of Sep 18 – 24?"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Connected apps will no longer be able to read this data from Health\u00A0Connect"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(withText("Cancel")).inRoot(isDialog()).check(matches(isDisplayed()))
                onView(withText("Delete")).inRoot(isDialog()).check(matches(isDisplayed()))
            }
    }

    @Test
    fun deleteAppEntries_fromWeekWithinYear_1000Selected_confirmationDialog_showsCorrectText() {
        val now = Instant.parse("2022-09-19T20:00:00.000Z")
        // This date is on a Sunday. The dialog should show the current week range,
        // which starts on this Sunday
        val selectedDay = Instant.parse("2022-09-18T20:00:00.000Z")
        (testTimeSource as TestTimeSource).setNow(now)
        whenever(viewModel.deletionProgress).then {
            MutableLiveData(DeletionViewModel.DeletionProgress.NOT_STARTED)
        }
        whenever(viewModel.getDeletionType()).then {
            DeletionType.DeleteEntriesFromApp(
                (1..1000).associate { "test_id_$it" to StepsRecord::class },
                TEST_APP_PACKAGE_NAME,
                TEST_APP_NAME,
                totalEntries = 1000,
                period = DateNavigationPeriod.PERIOD_WEEK,
                startTime = selectedDay,
            )
        }

        launchFragment<DeletionFragment>(Bundle()) {
                (this as DeletionFragment)
                    .parentFragmentManager
                    .setFragmentResult(START_DELETION_KEY, bundleOf())
            }
            .use {
                onView(
                        withText(
                            "Permanently delete selected $TEST_APP_NAME entries for the week of Sep 18 – 24?"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Connected apps will no longer be able to read this data from Health\u00A0Connect"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(withText("Cancel")).inRoot(isDialog()).check(matches(isDisplayed()))
                onView(withText("Delete")).inRoot(isDialog()).check(matches(isDisplayed()))
            }
    }

    @Test
    fun deleteAppEntries_fromWeekPastYear_someSelected_confirmationDialog_showsCorrectText() {
        val now = Instant.parse("2022-09-19T20:00:00.000Z")
        // This date is on a Saturday. The dialog should show the current week range,
        // which ends on this Saturday
        val selectedDay = Instant.parse("2021-09-18T20:00:00.000Z")
        // This needs to be start of period in the local time
        (testTimeSource as TestTimeSource).setNow(now)
        whenever(viewModel.deletionProgress).then {
            MutableLiveData(DeletionViewModel.DeletionProgress.NOT_STARTED)
        }
        whenever(viewModel.getDeletionType()).then {
            DeletionType.DeleteEntriesFromApp(
                mapOf("test_id_1" to StepsRecord::class, "test_id_2" to StepsCadenceRecord::class),
                TEST_APP_PACKAGE_NAME,
                TEST_APP_NAME,
                totalEntries = 10,
                period = DateNavigationPeriod.PERIOD_WEEK,
                startTime = selectedDay,
            )
        }

        launchFragment<DeletionFragment>(Bundle()) {
                (this as DeletionFragment)
                    .parentFragmentManager
                    .setFragmentResult(START_DELETION_KEY, bundleOf())
            }
            .use {
                onView(
                        withText(
                            "Permanently delete selected $TEST_APP_NAME entries for the week of Sep 12 – 18, 2021?"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Connected apps will no longer be able to read this data from Health\u00A0Connect"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(withText("Cancel")).inRoot(isDialog()).check(matches(isDisplayed()))
                onView(withText("Delete")).inRoot(isDialog()).check(matches(isDisplayed()))
            }
    }

    @Test
    fun deleteAppEntries_fromWeekPastYear_1000Selected_confirmationDialog_showsCorrectText() {
        val now = Instant.parse("2022-09-19T20:00:00.000Z")
        // This date is on a Saturday. The dialog should show the current week range,
        // which ends on this Satrday
        val selectedDay = Instant.parse("2021-09-18T20:00:00.000Z")
        // This needs to be start of period in the local time
        (testTimeSource as TestTimeSource).setNow(now)
        whenever(viewModel.deletionProgress).then {
            MutableLiveData(DeletionViewModel.DeletionProgress.NOT_STARTED)
        }
        whenever(viewModel.getDeletionType()).then {
            DeletionType.DeleteEntriesFromApp(
                (1..1000).associate { "test_id_$it" to StepsRecord::class },
                TEST_APP_PACKAGE_NAME,
                TEST_APP_NAME,
                totalEntries = 1000,
                period = DateNavigationPeriod.PERIOD_WEEK,
                startTime = selectedDay,
            )
        }

        launchFragment<DeletionFragment>(Bundle()) {
                (this as DeletionFragment)
                    .parentFragmentManager
                    .setFragmentResult(START_DELETION_KEY, bundleOf())
            }
            .use {
                onView(
                        withText(
                            "Permanently delete selected $TEST_APP_NAME entries for the week of Sep 12 – 18, 2021?"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Connected apps will no longer be able to read this data from Health\u00A0Connect"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(withText("Cancel")).inRoot(isDialog()).check(matches(isDisplayed()))
                onView(withText("Delete")).inRoot(isDialog()).check(matches(isDisplayed()))
            }
    }

    @Test
    fun deleteAppEntries_fromWeekWithinYear_allSelected_confirmationDialog_showsCorrectText() {
        val now = Instant.parse("2022-09-19T20:00:00.000Z")
        // This date is on a Sunday. The dialog should show the current week range,
        // which starts on this Sunday
        val selectedDay = Instant.parse("2022-09-18T20:00:00.000Z")
        (testTimeSource as TestTimeSource).setNow(now)
        whenever(viewModel.deletionProgress).then {
            MutableLiveData(DeletionViewModel.DeletionProgress.NOT_STARTED)
        }
        whenever(viewModel.getDeletionType()).then {
            DeletionType.DeleteEntriesFromApp(
                mapOf("test_id_1" to StepsRecord::class, "test_id_2" to StepsCadenceRecord::class),
                TEST_APP_PACKAGE_NAME,
                TEST_APP_NAME,
                totalEntries = 2,
                period = DateNavigationPeriod.PERIOD_WEEK,
                startTime = selectedDay,
            )
        }

        launchFragment<DeletionFragment>(Bundle()) {
                (this as DeletionFragment)
                    .parentFragmentManager
                    .setFragmentResult(START_DELETION_KEY, bundleOf())
            }
            .use {
                onView(
                        withText(
                            "Permanently delete all $TEST_APP_NAME entries for the week of Sep 18 – 24?"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Connected apps will no longer be able to read this data from Health\u00A0Connect"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(withText("Cancel")).inRoot(isDialog()).check(matches(isDisplayed()))
                onView(withText("Delete")).inRoot(isDialog()).check(matches(isDisplayed()))
            }
    }

    @Test
    fun deleteAppEntries_fromWeekPastYear_allSelected_confirmationDialog_showsCorrectText() {
        val now = Instant.parse("2022-09-19T20:00:00.000Z")
        // This date is on a Saturday. The dialog should show the current week range,
        // which ends on this Saturday
        val selectedDay = Instant.parse("2021-09-18T20:00:00.000Z")
        (testTimeSource as TestTimeSource).setNow(now)
        whenever(viewModel.deletionProgress).then {
            MutableLiveData(DeletionViewModel.DeletionProgress.NOT_STARTED)
        }
        whenever(viewModel.getDeletionType()).then {
            DeletionType.DeleteEntriesFromApp(
                mapOf("test_id_1" to StepsRecord::class, "test_id_2" to StepsCadenceRecord::class),
                TEST_APP_PACKAGE_NAME,
                TEST_APP_NAME,
                totalEntries = 2,
                period = DateNavigationPeriod.PERIOD_WEEK,
                startTime = selectedDay,
            )
        }

        launchFragment<DeletionFragment>(Bundle()) {
                (this as DeletionFragment)
                    .parentFragmentManager
                    .setFragmentResult(START_DELETION_KEY, bundleOf())
            }
            .use {
                onView(
                        withText(
                            "Permanently delete all $TEST_APP_NAME entries for the week of Sep 12 – 18, 2021?"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Connected apps will no longer be able to read this data from Health\u00A0Connect"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(withText("Cancel")).inRoot(isDialog()).check(matches(isDisplayed()))
                onView(withText("Delete")).inRoot(isDialog()).check(matches(isDisplayed()))
            }
    }

    // endregion

    // region month
    @Test
    fun deleteAppEntries_fromMonth_oneSelected_confirmationDialog_showsCorrectText() {
        val now = Instant.parse("2022-09-20T20:00:00.000Z")
        val selectedDay = Instant.parse("2022-09-19T20:00:00.000Z")
        (testTimeSource as TestTimeSource).setNow(now)
        whenever(viewModel.deletionProgress).then {
            MutableLiveData(DeletionViewModel.DeletionProgress.NOT_STARTED)
        }
        whenever(viewModel.getDeletionType()).then {
            DeletionType.DeleteEntriesFromApp(
                mapOf("test_id_1" to StepsRecord::class),
                TEST_APP_PACKAGE_NAME,
                TEST_APP_NAME,
                totalEntries = 10,
                period = DateNavigationPeriod.PERIOD_MONTH,
                startTime = selectedDay,
            )
        }

        launchFragment<DeletionFragment>(Bundle()) {
                (this as DeletionFragment)
                    .parentFragmentManager
                    .setFragmentResult(START_DELETION_KEY, bundleOf())
            }
            .use {
                onView(withText("Permanently delete this $TEST_APP_NAME entry?"))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Connected apps will no longer be able to read this data from Health\u00A0Connect"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(withText("Cancel")).inRoot(isDialog()).check(matches(isDisplayed()))
                onView(withText("Delete")).inRoot(isDialog()).check(matches(isDisplayed()))
            }
    }

    @Test
    fun deleteAppEntries_fromMonthWithinYear_someSelected_confirmationDialog_showsCorrectText() {
        val now = Instant.parse("2022-09-20T20:00:00.000Z")
        val selectedDay = Instant.parse("2022-09-19T20:00:00.000Z")
        (testTimeSource as TestTimeSource).setNow(now)
        whenever(viewModel.deletionProgress).then {
            MutableLiveData(DeletionViewModel.DeletionProgress.NOT_STARTED)
        }
        whenever(viewModel.getDeletionType()).then {
            DeletionType.DeleteEntriesFromApp(
                mapOf("test_id_1" to StepsRecord::class, "test_id_2" to StepsCadenceRecord::class),
                TEST_APP_PACKAGE_NAME,
                TEST_APP_NAME,
                totalEntries = 10,
                period = DateNavigationPeriod.PERIOD_MONTH,
                startTime = selectedDay,
            )
        }

        launchFragment<DeletionFragment>(Bundle()) {
                (this as DeletionFragment)
                    .parentFragmentManager
                    .setFragmentResult(START_DELETION_KEY, bundleOf())
            }
            .use {
                onView(
                        withText(
                            "Permanently delete selected $TEST_APP_NAME entries for September?"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Connected apps will no longer be able to read this data from Health\u00A0Connect"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(withText("Cancel")).inRoot(isDialog()).check(matches(isDisplayed()))
                onView(withText("Delete")).inRoot(isDialog()).check(matches(isDisplayed()))
            }
    }

    @Test
    fun deleteAppEntries_fromMonthWithinYear_1000Selected_confirmationDialog_showsCorrectText() {
        val now = Instant.parse("2022-09-20T20:00:00.000Z")
        val selectedDay = Instant.parse("2022-09-19T20:00:00.000Z")
        (testTimeSource as TestTimeSource).setNow(now)
        whenever(viewModel.deletionProgress).then {
            MutableLiveData(DeletionViewModel.DeletionProgress.NOT_STARTED)
        }
        whenever(viewModel.getDeletionType()).then {
            DeletionType.DeleteEntriesFromApp(
                (1..1000).associate { "test_id_$it" to StepsRecord::class },
                TEST_APP_PACKAGE_NAME,
                TEST_APP_NAME,
                totalEntries = 1000,
                period = DateNavigationPeriod.PERIOD_MONTH,
                startTime = selectedDay,
            )
        }

        launchFragment<DeletionFragment>(Bundle()) {
                (this as DeletionFragment)
                    .parentFragmentManager
                    .setFragmentResult(START_DELETION_KEY, bundleOf())
            }
            .use {
                onView(
                        withText(
                            "Permanently delete selected $TEST_APP_NAME entries for September?"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Connected apps will no longer be able to read this data from Health\u00A0Connect"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(withText("Cancel")).inRoot(isDialog()).check(matches(isDisplayed()))
                onView(withText("Delete")).inRoot(isDialog()).check(matches(isDisplayed()))
            }
    }

    @Test
    fun deleteAppEntries_fromMonthPastYear_someSelected_confirmationDialog_showsCorrectText() {
        val now = Instant.parse("2022-09-20T20:00:00.000Z")
        val selectedDay = Instant.parse("2021-09-19T20:00:00.000Z")
        (testTimeSource as TestTimeSource).setNow(now)
        whenever(viewModel.deletionProgress).then {
            MutableLiveData(DeletionViewModel.DeletionProgress.NOT_STARTED)
        }
        whenever(viewModel.getDeletionType()).then {
            DeletionType.DeleteEntriesFromApp(
                mapOf("test_id_1" to StepsRecord::class, "test_id_2" to StepsCadenceRecord::class),
                TEST_APP_PACKAGE_NAME,
                TEST_APP_NAME,
                totalEntries = 10,
                period = DateNavigationPeriod.PERIOD_MONTH,
                startTime = selectedDay,
            )
        }

        launchFragment<DeletionFragment>(Bundle()) {
                (this as DeletionFragment)
                    .parentFragmentManager
                    .setFragmentResult(START_DELETION_KEY, bundleOf())
            }
            .use {
                onView(
                        withText(
                            "Permanently delete selected $TEST_APP_NAME entries for September 2021?"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Connected apps will no longer be able to read this data from Health\u00A0Connect"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(withText("Cancel")).inRoot(isDialog()).check(matches(isDisplayed()))
                onView(withText("Delete")).inRoot(isDialog()).check(matches(isDisplayed()))
            }
    }

    @Test
    fun deleteAppEntries_fromMonthPastYear_1000Selected_confirmationDialog_showsCorrectText() {
        val now = Instant.parse("2022-09-20T20:00:00.000Z")
        val selectedDay = Instant.parse("2021-09-19T20:00:00.000Z")
        (testTimeSource as TestTimeSource).setNow(now)
        whenever(viewModel.deletionProgress).then {
            MutableLiveData(DeletionViewModel.DeletionProgress.NOT_STARTED)
        }
        whenever(viewModel.getDeletionType()).then {
            DeletionType.DeleteEntriesFromApp(
                (1..1000).associate { "test_id_$it" to StepsRecord::class },
                TEST_APP_PACKAGE_NAME,
                TEST_APP_NAME,
                totalEntries = 1000,
                period = DateNavigationPeriod.PERIOD_MONTH,
                startTime = selectedDay,
            )
        }

        launchFragment<DeletionFragment>(Bundle()) {
                (this as DeletionFragment)
                    .parentFragmentManager
                    .setFragmentResult(START_DELETION_KEY, bundleOf())
            }
            .use {
                onView(
                        withText(
                            "Permanently delete selected $TEST_APP_NAME entries for September 2021?"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Connected apps will no longer be able to read this data from Health\u00A0Connect"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(withText("Cancel")).inRoot(isDialog()).check(matches(isDisplayed()))
                onView(withText("Delete")).inRoot(isDialog()).check(matches(isDisplayed()))
            }
    }

    @Test
    fun deleteAppEntries_fromMonthWithinYear_allSelected_confirmationDialog_showsCorrectText() {
        val now = Instant.parse("2022-09-20T20:00:00.000Z")
        val selectedDay = Instant.parse("2022-09-19T20:00:00.000Z")
        (testTimeSource as TestTimeSource).setNow(now)
        whenever(viewModel.deletionProgress).then {
            MutableLiveData(DeletionViewModel.DeletionProgress.NOT_STARTED)
        }
        whenever(viewModel.getDeletionType()).then {
            DeletionType.DeleteEntriesFromApp(
                mapOf("test_id_1" to StepsRecord::class, "test_id_2" to StepsCadenceRecord::class),
                TEST_APP_PACKAGE_NAME,
                TEST_APP_NAME,
                totalEntries = 2,
                period = DateNavigationPeriod.PERIOD_MONTH,
                startTime = selectedDay,
            )
        }

        launchFragment<DeletionFragment>(Bundle()) {
                (this as DeletionFragment)
                    .parentFragmentManager
                    .setFragmentResult(START_DELETION_KEY, bundleOf())
            }
            .use {
                onView(withText("Permanently delete all $TEST_APP_NAME entries for September?"))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Connected apps will no longer be able to read this data from Health\u00A0Connect"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(withText("Cancel")).inRoot(isDialog()).check(matches(isDisplayed()))
                onView(withText("Delete")).inRoot(isDialog()).check(matches(isDisplayed()))
            }
    }

    @Test
    fun deleteAppEntries_fromMonthPastYear_allSelected_confirmationDialog_showsCorrectText() {
        val now = Instant.parse("2022-09-20T20:00:00.000Z")
        val selectedDay = Instant.parse("2021-09-19T20:00:00.000Z")
        (testTimeSource as TestTimeSource).setNow(now)
        whenever(viewModel.deletionProgress).then {
            MutableLiveData(DeletionViewModel.DeletionProgress.NOT_STARTED)
        }
        whenever(viewModel.getDeletionType()).then {
            DeletionType.DeleteEntriesFromApp(
                mapOf("test_id_1" to StepsRecord::class, "test_id_2" to StepsCadenceRecord::class),
                TEST_APP_PACKAGE_NAME,
                TEST_APP_NAME,
                totalEntries = 2,
                period = DateNavigationPeriod.PERIOD_MONTH,
                startTime = selectedDay,
            )
        }

        launchFragment<DeletionFragment>(Bundle()) {
                (this as DeletionFragment)
                    .parentFragmentManager
                    .setFragmentResult(START_DELETION_KEY, bundleOf())
            }
            .use {
                onView(
                        withText(
                            "Permanently delete all $TEST_APP_NAME entries for September 2021?"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Connected apps will no longer be able to read this data from Health\u00A0Connect"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(withText("Cancel")).inRoot(isDialog()).check(matches(isDisplayed()))
                onView(withText("Delete")).inRoot(isDialog()).check(matches(isDisplayed()))
            }
    }

    // endregion

    // endregion

    // region inactive app
    @Test
    fun deleteInactiveAppHealthPermissionTypeData_showsCorrectText() {
        whenever(viewModel.deletionProgress).then {
            MutableLiveData(DeletionViewModel.DeletionProgress.NOT_STARTED)
        }
        whenever(viewModel.getDeletionType()).then {
            DeletionType.DeleteInactiveAppData(
                appName = TEST_APP_NAME,
                packageName = TEST_APP_PACKAGE_NAME,
                healthPermissionType = FitnessPermissionType.STEPS,
            )
        }

        launchFragment<DeletionFragment>(Bundle()) {
                (this as DeletionFragment)
                    .parentFragmentManager
                    .setFragmentResult(START_DELETION_KEY, bundleOf())
            }
            .use {
                onView(withText("Permanently delete steps data for $TEST_APP_NAME?"))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Connected apps will no longer be able to read this data from Health\u00A0Connect"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(withText("Cancel")).inRoot(isDialog()).check(matches(isDisplayed()))
                onView(withText("Delete")).inRoot(isDialog()).check(matches(isDisplayed()))
            }
    }

    // endregion

    // region confirmation dialog buttons
    @Test
    fun confirmationDialog_cancelButton_exitsDialog() {
        whenever(viewModel.deletionProgress).then {
            MutableLiveData(DeletionViewModel.DeletionProgress.NOT_STARTED)
        }

        launchFragment<DeletionFragment>(Bundle()) {
                (this as DeletionFragment)
                    .parentFragmentManager
                    .setFragmentResult(START_DELETION_KEY, bundleOf())
            }
            .use {
                onView(withText("Permanently delete selected data?"))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Connected apps will no longer be able to read this data from Health\u00A0Connect"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(withText("Cancel")).inRoot(isDialog()).perform(click())

                onView(withText("Permanently delete selected data?")).check(doesNotExist())
            }
    }

    // endregion

    // region deletion states
    @Test
    fun whenProgressIndicatorCanStartState_progressDialogShown() {
        whenever(viewModel.deletionProgress).then {
            MutableLiveData(DeletionViewModel.DeletionProgress.PROGRESS_INDICATOR_CAN_START)
        }

        launchFragment<DeletionFragment>(Bundle()) {
                (this as DeletionFragment)
                    .parentFragmentManager
                    .setFragmentResult(START_DELETION_KEY, bundleOf())
            }
            .use {
                onView(withText("Permanently delete selected data?"))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Connected apps will no longer be able to read this data from Health\u00A0Connect"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))

                onView(withText("Delete")).inRoot(isDialog()).perform(click())

                onView(withText("Deleting your data"))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
            }
    }

    @Test
    fun whenCompletedState_progressDialogDisappears() {
        whenever(viewModel.deletionProgress).then {
            MutableLiveData(DeletionViewModel.DeletionProgress.COMPLETED)
        }

        launchFragment<DeletionFragment>(Bundle()) {
                (this as DeletionFragment)
                    .parentFragmentManager
                    .setFragmentResult(START_DELETION_KEY, bundleOf())
            }
            .use {
                onView(withText("Permanently delete selected data?"))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Connected apps will no longer be able to read this data from Health\u00A0Connect"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(withText("Delete")).inRoot(isDialog()).perform(click())
                onView(withText("Deleting your data")).inRoot(isDialog()).check(doesNotExist())
            }
    }

    @Test
    fun whenCompletedState_successDialogShown() {
        whenever(viewModel.deletionProgress).then {
            MutableLiveData(DeletionViewModel.DeletionProgress.COMPLETED)
        }

        launchFragment<DeletionFragment>(Bundle()) {
                (this as DeletionFragment)
                    .parentFragmentManager
                    .setFragmentResult(START_DELETION_KEY, bundleOf())
            }
            .use {
                onView(withText("Permanently delete selected data?"))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Connected apps will no longer be able to read this data from Health\u00A0Connect"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(withText("Delete")).inRoot(isDialog()).perform(click())
                onView(withText("Data deleted from Health Connect"))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "If you want to completely delete the data from your connected apps, check each app where your data may be saved."
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(withText("Done")).inRoot(isDialog()).check(matches(isDisplayed()))
                onView(withText("See connected apps"))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
            }
    }

    @Test
    fun whenFailedState_failureDialogShown() {
        whenever(viewModel.deletionProgress).then {
            MutableLiveData(DeletionViewModel.DeletionProgress.FAILED)
        }

        launchFragment<DeletionFragment>(Bundle()) {
                (this as DeletionFragment)
                    .parentFragmentManager
                    .setFragmentResult(START_DELETION_KEY, bundleOf())
            }
            .use {
                onView(withText("Permanently delete selected data?"))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Connected apps will no longer be able to read this data from Health\u00A0Connect"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(withText("Delete")).inRoot(isDialog()).perform(click())
                onView(withText("Deleting your data")).inRoot(isDialog()).check(doesNotExist())
                onView(withText("Couldn't delete data"))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Something went wrong and Health\u00A0Connect couldn't delete your data"
                        )
                    )
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
            }
    }

    // endregion
}
