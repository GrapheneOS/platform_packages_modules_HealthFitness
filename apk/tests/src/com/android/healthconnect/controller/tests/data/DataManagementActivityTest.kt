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

import android.content.Context
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ActivityScenario.launch
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.autodelete.AutoDeleteRange
import com.android.healthconnect.controller.autodelete.AutoDeleteViewModel
import com.android.healthconnect.controller.categories.HealthDataCategoryViewModel
import com.android.healthconnect.controller.data.DataManagementActivity
import com.android.healthconnect.controller.migration.MigrationViewModel
import com.android.healthconnect.controller.migration.api.MigrationState
import com.android.healthconnect.controller.tests.utils.di.FakeFeatureUtils
import com.android.healthconnect.controller.tests.utils.showOnboarding
import com.android.healthconnect.controller.tests.utils.whenever
import com.android.healthconnect.controller.utils.FeatureUtils
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito

@HiltAndroidTest
@OptIn(ExperimentalCoroutinesApi::class)
class DataManagementActivityTest {
    @get:Rule val hiltRule = HiltAndroidRule(this)

    @BindValue
    val migrationViewModel: MigrationViewModel = Mockito.mock(MigrationViewModel::class.java)

    @BindValue
    val categoryViewModel: HealthDataCategoryViewModel =
        Mockito.mock(HealthDataCategoryViewModel::class.java)

    @BindValue
    val autoDeleteViewModel: AutoDeleteViewModel = Mockito.mock(AutoDeleteViewModel::class.java)

    @Inject lateinit var fakeFeatureUtils: FeatureUtils

    private lateinit var context: Context

    @Before
    fun setup() {
        hiltRule.inject()
        context = InstrumentationRegistry.getInstrumentation().context
        (fakeFeatureUtils as FakeFeatureUtils).setIsNewInformationArchitectureEnabled(false)

        showOnboarding(context, show = false)
        whenever(autoDeleteViewModel.storedAutoDeleteRange).then {
            MutableLiveData(
                AutoDeleteViewModel.AutoDeleteState.WithData(
                    AutoDeleteRange.AUTO_DELETE_RANGE_NEVER))
        }
        whenever(categoryViewModel.categoriesData).then {
            MutableLiveData<HealthDataCategoryViewModel.CategoriesFragmentState>(
                HealthDataCategoryViewModel.CategoriesFragmentState.WithData(emptyList()))
        }
    }

    @Test
    fun manageDataIntent_onboardingDone_launchesDataManagementActivity() = runTest {
        whenever(migrationViewModel.getCurrentMigrationUiState()).then {
            MigrationState.COMPLETE_IDLE
        }
        whenever(migrationViewModel.migrationState).then {
            MutableLiveData(
                MigrationViewModel.MigrationFragmentState.WithData(MigrationState.COMPLETE_IDLE))
        }

        val startActivityIntent = Intent(context, DataManagementActivity::class.java)

        launch<DataManagementActivity>(startActivityIntent)
        onView(withText("Browse data")).check(matches(isDisplayed()))
    }

    @Test
    fun manageDataIntent_onboardingNotDone_redirectsToOnboarding() = runTest {
        showOnboarding(context, true)
        whenever(migrationViewModel.getCurrentMigrationUiState()).then {
            MigrationState.COMPLETE_IDLE
        }
        whenever(migrationViewModel.migrationState).then {
            MutableLiveData(
                MigrationViewModel.MigrationFragmentState.WithData(MigrationState.COMPLETE_IDLE))
        }

        val startActivityIntent = Intent(context, DataManagementActivity::class.java)

        launch<DataManagementActivity>(startActivityIntent)

        onView(withText("Share data with your apps"))
            .perform(ViewActions.scrollTo())
            .check(matches(isDisplayed()))
    }

    @Test
    fun manageDataIntent_migrationInProgress_redirectsToMigrationScreen() = runTest {
        showOnboarding(context, false)
        whenever(migrationViewModel.getCurrentMigrationUiState()).then {
            MigrationState.IN_PROGRESS
        }
        whenever(migrationViewModel.migrationState).then {
            MutableLiveData(
                MigrationViewModel.MigrationFragmentState.WithData(MigrationState.IN_PROGRESS))
        }

        val startActivityIntent = Intent(context, DataManagementActivity::class.java)

        launch<DataManagementActivity>(startActivityIntent)

        onView(withText("Integration in progress")).check(matches(isDisplayed()))
    }

    @After
    fun tearDown() {
        showOnboarding(context, false)
    }
}
