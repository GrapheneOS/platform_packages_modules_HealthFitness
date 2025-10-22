/**
 * Copyright (C) 2023 The Android Open Source Project
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
package com.android.healthconnect.controller.tests.data

import android.Manifest
import android.content.Context
import android.content.Intent
import android.health.connect.HealthDataCategory
import android.platform.test.flag.junit.SetFlagsRule
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ActivityScenario.launch
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.data.DataManagementActivity
import com.android.healthconnect.controller.data.alldata.AllDataViewModel
import com.android.healthconnect.controller.data.appdata.PermissionTypesPerCategory
import com.android.healthconnect.controller.migration.MigrationViewModel
import com.android.healthconnect.controller.migration.api.MigrationRestoreState
import com.android.healthconnect.controller.migration.api.MigrationRestoreState.DataRestoreUiError
import com.android.healthconnect.controller.migration.api.MigrationRestoreState.DataRestoreUiState
import com.android.healthconnect.controller.migration.api.MigrationRestoreState.MigrationUiState
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.selectabledeletion.DeletionDataViewModel
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.whenever

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class DataManagementActivityTest {
    @get:Rule val hiltRule = HiltAndroidRule(this)
    @get:Rule val setFlagsRule = SetFlagsRule()

    @BindValue
    val migrationViewModel: MigrationViewModel = Mockito.mock(MigrationViewModel::class.java)

    @BindValue val allDataViewModel: AllDataViewModel = Mockito.mock(AllDataViewModel::class.java)

    private lateinit var activityScenario: ActivityScenario<DataManagementActivity>
    private lateinit var context: Context

    @Before
    fun setup() {
        hiltRule.inject()
        // Required for aconfig flag reading for tests run on pre V devices
        InstrumentationRegistry.getInstrumentation()
            .getUiAutomation()
            .adoptShellPermissionIdentity(Manifest.permission.READ_DEVICE_CONFIG)

        context = InstrumentationRegistry.getInstrumentation().context

        whenever(allDataViewModel.allData).then {
            MutableLiveData<AllDataViewModel.AllDataState>(
                AllDataViewModel.AllDataState.WithData(
                    listOf(
                        PermissionTypesPerCategory(
                            HealthDataCategory.ACTIVITY,
                            listOf(FitnessPermissionType.STEPS),
                        )
                    )
                )
            )
        }
        whenever(allDataViewModel.setOfPermissionTypesToBeDeleted).then {
            MutableLiveData<Set<FitnessPermissionType>>(emptySet())
        }
        whenever(allDataViewModel.deletionScreenState).then {
            MutableLiveData(DeletionDataViewModel.DeletionScreenState.VIEW)
        }
        whenever(allDataViewModel.getDeletionScreenStateValue())
            .thenReturn(DeletionDataViewModel.DeletionScreenState.VIEW)
    }

    @Test
    fun showsAllDataFragment() {
        whenever(migrationViewModel.getCurrentMigrationUiState()).then {
            MigrationRestoreState(
                migrationUiState = MigrationUiState.IDLE,
                dataRestoreState = DataRestoreUiState.IDLE,
                dataRestoreError = DataRestoreUiError.ERROR_NONE,
            )
        }
        whenever(migrationViewModel.migrationState).then {
            MutableLiveData(
                MigrationViewModel.MigrationFragmentState.WithData(
                    MigrationRestoreState(
                        migrationUiState = MigrationUiState.IDLE,
                        dataRestoreState = DataRestoreUiState.IDLE,
                        dataRestoreError = DataRestoreUiError.ERROR_NONE,
                    )
                )
            )
        }
        val startActivityIntent = Intent(context, DataManagementActivity::class.java)

        activityScenario = launch<DataManagementActivity>(startActivityIntent)
        onView(withText("Activity")).check(matches(isDisplayed()))
        onView(withText("Steps")).check(matches(isDisplayed()))
    }

    @Test
    fun manageDataIntent_migrationInProgress_redirectsToMigrationInProgress() = runTest {
        whenever(migrationViewModel.getCurrentMigrationUiState()).then {
            MigrationRestoreState(
                migrationUiState = MigrationUiState.IN_PROGRESS,
                dataRestoreState = DataRestoreUiState.IDLE,
                dataRestoreError = DataRestoreUiError.ERROR_NONE,
            )
        }
        whenever(migrationViewModel.migrationState).then {
            MutableLiveData(
                MigrationViewModel.MigrationFragmentState.WithData(
                    MigrationRestoreState(
                        migrationUiState = MigrationUiState.IN_PROGRESS,
                        dataRestoreState = DataRestoreUiState.IDLE,
                        dataRestoreError = DataRestoreUiError.ERROR_NONE,
                    )
                )
            )
        }

        val startActivityIntent = Intent(context, DataManagementActivity::class.java)

        activityScenario = launch<DataManagementActivity>(startActivityIntent)

        onView(withText("Integration in progress")).check(matches(isDisplayed()))
    }

    @Test
    fun manageDataIntent_dataRestoreInProgress_redirectsToDataRestoreInProgress() = runTest {
        whenever(migrationViewModel.getCurrentMigrationUiState()).then {
            MigrationRestoreState(
                migrationUiState = MigrationUiState.IDLE,
                dataRestoreState = DataRestoreUiState.IN_PROGRESS,
                dataRestoreError = DataRestoreUiError.ERROR_NONE,
            )
        }
        whenever(migrationViewModel.migrationState).then {
            MutableLiveData(
                MigrationViewModel.MigrationFragmentState.WithData(
                    MigrationRestoreState(
                        migrationUiState = MigrationUiState.IDLE,
                        dataRestoreState = DataRestoreUiState.IN_PROGRESS,
                        dataRestoreError = DataRestoreUiError.ERROR_NONE,
                    )
                )
            )
        }

        val startActivityIntent = Intent(context, DataManagementActivity::class.java)

        activityScenario = launch<DataManagementActivity>(startActivityIntent)

        onView(withText("Restore in progress")).check(matches(isDisplayed()))
    }

    @Test
    fun manageDataIntent_migrationComplete_showsDialog() = runTest {
        whenever(migrationViewModel.getCurrentMigrationUiState()).then {
            MigrationRestoreState(
                migrationUiState = MigrationUiState.COMPLETE,
                dataRestoreState = DataRestoreUiState.IDLE,
                dataRestoreError = DataRestoreUiError.ERROR_NONE,
            )
        }
        whenever(migrationViewModel.migrationState).then {
            MutableLiveData(
                MigrationViewModel.MigrationFragmentState.WithData(
                    MigrationRestoreState(
                        migrationUiState = MigrationUiState.COMPLETE,
                        dataRestoreState = DataRestoreUiState.IDLE,
                        dataRestoreError = DataRestoreUiError.ERROR_NONE,
                    )
                )
            )
        }

        val startActivityIntent = Intent(context, DataManagementActivity::class.java)

        activityScenario = launch<DataManagementActivity>(startActivityIntent)

        onView(withText("What's new")).inRoot(RootMatchers.isDialog()).check(matches(isDisplayed()))
        onView(
                withText(
                    "You can now access Health Connect directly from your settings. Uninstall the Health Connect app any time to free up storage space."
                )
            )
            .inRoot(RootMatchers.isDialog())
            .check(matches(isDisplayed()))
        onView(withText("Got it")).inRoot(RootMatchers.isDialog()).check(matches(isDisplayed()))

        onView(withText("Got it")).inRoot(RootMatchers.isDialog()).perform(ViewActions.click())

        activityScenario.onActivity { activity ->
            val preferences =
                activity.getSharedPreferences("USER_ACTIVITY_TRACKER", Context.MODE_PRIVATE)
            assertThat(preferences.getBoolean("Whats New Seen", false)).isTrue()
        }
    }

    @After
    fun tearDown() {
        activityScenario.close()
    }
}
