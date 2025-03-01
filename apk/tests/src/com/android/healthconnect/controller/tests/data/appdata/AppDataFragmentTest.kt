/*
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
package com.android.healthconnect.controller.tests.data.appdata

import android.content.Context
import android.content.Intent
import android.health.connect.HealthConnectManager
import android.health.connect.MedicalResourceTypeInfo
import android.health.connect.RecordTypeInfoResponse
import android.health.connect.datatypes.Record
import android.os.OutcomeReceiver
import androidx.core.os.bundleOf
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.preference.PreferenceCategory
import androidx.test.espresso.Espresso.onIdle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.data.appdata.AllDataUseCase
import com.android.healthconnect.controller.data.appdata.AppDataFragment
import com.android.healthconnect.controller.data.appdata.AppDataViewModel
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.permissions.data.HealthPermissionType
import com.android.healthconnect.controller.permissions.data.MedicalPermissionType
import com.android.healthconnect.controller.permissions.data.toMedicalResourceType
import com.android.healthconnect.controller.selectabledeletion.DeletionDataViewModel.DeletionScreenState.DELETE
import com.android.healthconnect.controller.selectabledeletion.DeletionPermissionTypesPreference
import com.android.healthconnect.controller.selectabledeletion.SelectAllCheckboxPreference
import com.android.healthconnect.controller.shared.Constants
import com.android.healthconnect.controller.shared.HealthDataCategoryExtensions.fromFitnessPermissionType
import com.android.healthconnect.controller.shared.HealthPermissionToDatatypeMapper
import com.android.healthconnect.controller.shared.app.AppInfoReader
import com.android.healthconnect.controller.shared.children
import com.android.healthconnect.controller.shared.preference.EmptyPreferenceCategory
import com.android.healthconnect.controller.tests.utils.InstantTaskExecutorRule
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.TEST_MEDICAL_DATA_SOURCE
import com.android.healthconnect.controller.tests.utils.getDataOrigin
import com.android.healthconnect.controller.tests.utils.launchFragment
import com.android.healthconnect.controller.tests.utils.toggleAnimation
import com.android.healthconnect.controller.utils.logging.AppDataElement
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.PageName
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import org.mockito.invocation.InvocationOnMock
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
class AppDataFragmentTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)
    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()
    private val testDispatcher = UnconfinedTestDispatcher()

    var manager: HealthConnectManager = Mockito.mock(HealthConnectManager::class.java)
    @Inject lateinit var appInfoReader: AppInfoReader
    @BindValue lateinit var appDataViewModel: AppDataViewModel
    @BindValue val healthConnectLogger: HealthConnectLogger = mock()
    private lateinit var navHostController: TestNavHostController
    private lateinit var context: Context

    @Before
    fun setup() {
        hiltRule.inject()
        Dispatchers.setMain(testDispatcher)
        context = InstrumentationRegistry.getInstrumentation().context
        navHostController = TestNavHostController(context)
        val allDataUseCase = AllDataUseCase(manager, Dispatchers.Main)
        appDataViewModel = AppDataViewModel(appInfoReader, allDataUseCase)
        toggleAnimation(false)
    }

    @After
    fun tearDown() {
        toggleAnimation(true)
        Dispatchers.resetMain()
        reset(healthConnectLogger)
    }

    @Test
    fun whenNoData_noDataMessageDisplayed() = runTest {
        mockData(emptyList())

        launchFragment<AppDataFragment>(
            bundleOf(
                Intent.EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                Constants.EXTRA_APP_NAME to TEST_APP_NAME,
            )
        )

        onView(withText("No data")).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withText("Data from Health Connect test app will show here"))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
    }

    @Test
    fun fitnessDataPresent_populatedDataTypesDisplayed() = runTest {
        mockData(
            listOf(
                FitnessPermissionType.DISTANCE,
                FitnessPermissionType.EXERCISE,
                FitnessPermissionType.STEPS,
                FitnessPermissionType.MENSTRUATION,
                FitnessPermissionType.SEXUAL_ACTIVITY,
            )
        )
        launchFragment<AppDataFragment>(
            bundleOf(
                Intent.EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                Constants.EXTRA_APP_NAME to TEST_APP_NAME,
            )
        )

        onView(withText("Activity")).check(matches(isDisplayed()))
        onView(withText("Distance")).check(matches(isDisplayed()))
        onView(withText("Exercise")).check(matches(isDisplayed()))
        onView(withText("Steps")).check(matches(isDisplayed()))

        onView(withText("Cycle tracking")).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withText("Menstruation")).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withText("Sexual activity")).perform(scrollTo()).check(matches(isDisplayed()))

        onView(withText("Body measurements")).check(doesNotExist())
        onView(withText("Nutrition")).check(doesNotExist())
        onView(withText("Sleep")).check(doesNotExist())
        onView(withText("Vitals")).check(doesNotExist())
        onView(withText("Health records")).check(doesNotExist())
        onView(withText("Vaccines")).check(doesNotExist())
        onView(withText("No data")).check(doesNotExist())
        onView(withText("Data from Health Connect test app will show here")).check(doesNotExist())

        verify(healthConnectLogger, atLeast(1)).setPageId(PageName.APP_DATA_PAGE)
        verify(healthConnectLogger).logPageImpression()
        verify(healthConnectLogger, times(5))
            .logImpression(AppDataElement.PERMISSION_TYPE_BUTTON_NO_CHECKBOX)
    }

    @Test
    fun navigatesToAppEntries() {
        mockData(
            listOf(
                FitnessPermissionType.DISTANCE,
                FitnessPermissionType.EXERCISE,
                FitnessPermissionType.STEPS,
                FitnessPermissionType.MENSTRUATION,
                FitnessPermissionType.SEXUAL_ACTIVITY,
            )
        )
        launchFragment<AppDataFragment>(
            bundleOf(
                Intent.EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                Constants.EXTRA_APP_NAME to TEST_APP_NAME,
            )
        ) {
            navHostController.setGraph(R.navigation.app_data_nav_graph)
            Navigation.setViewNavController(this.requireView(), navHostController)
        }
        onView(withText("Steps")).check(matches(isDisplayed()))
        onView(withText("Steps")).perform(click())
        verify(healthConnectLogger)
            .logInteraction(AppDataElement.PERMISSION_TYPE_BUTTON_NO_CHECKBOX)
        assertThat(navHostController.currentDestination?.id).isEqualTo(R.id.appEntriesFragment)
    }

    @Test
    fun navigatesToMedicalAppEntries() {
        mockData(listOf(MedicalPermissionType.VACCINES))
        launchFragment<AppDataFragment>(
            bundleOf(
                Intent.EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                Constants.EXTRA_APP_NAME to TEST_APP_NAME,
            )
        ) {
            navHostController.setGraph(R.navigation.app_data_nav_graph)
            Navigation.setViewNavController(this.requireView(), navHostController)
        }

        onView(withText("Vaccines")).check(matches(isDisplayed()))
        onView(withText("Vaccines")).perform(click())
        verify(healthConnectLogger)
            .logInteraction(AppDataElement.PERMISSION_TYPE_BUTTON_NO_CHECKBOX)
        assertThat(navHostController.currentDestination?.id).isEqualTo(R.id.appEntriesFragment)
    }

    @Test
    fun fitnessAndMedicalData_fitnessAndMedicalDataShown() = runTest {
        mockData(
            listOf(
                FitnessPermissionType.DISTANCE,
                FitnessPermissionType.EXERCISE,
                MedicalPermissionType.VACCINES,
            )
        )

        launchFragment<AppDataFragment>(
            bundleOf(
                Intent.EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                Constants.EXTRA_APP_NAME to TEST_APP_NAME,
            )
        )

        onView(withText("Activity")).check(matches(isDisplayed()))
        onView(withText("Distance")).check(matches(isDisplayed()))
        onView(withText("Exercise")).check(matches(isDisplayed()))

        onView(withText("Health records")).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withText("Vaccines")).perform(scrollTo()).check(matches(isDisplayed()))

        onView(withText("Steps")).check(doesNotExist())
        onView(withText("Body measurements")).check(doesNotExist())
        onView(withText("Cycle tracking")).check(doesNotExist())
        onView(withText("No data")).check(doesNotExist())
        onView(withText("Data from Health Connect test app will show here")).check(doesNotExist())
    }

    @Test
    fun medicalDataOnly_populatedDataTypesDisplayed() = runTest {
        mockData(listOf(MedicalPermissionType.VACCINES))
        launchFragment<AppDataFragment>(
            bundleOf(
                Intent.EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                Constants.EXTRA_APP_NAME to TEST_APP_NAME,
            )
        )

        onView(withText("Activity")).check(doesNotExist())
        onView(withText("Distance")).check(doesNotExist())

        onView(withText("Health records")).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withText("Vaccines")).perform(scrollTo()).check(matches(isDisplayed()))

        onView(withText("Body measurements")).check(doesNotExist())
    }

    @Test
    fun inDeletionState_showsCheckboxes() = runTest {
        mockData(listOf(FitnessPermissionType.DISTANCE, FitnessPermissionType.STEPS))

        val scenario =
            launchFragment<AppDataFragment>(
                bundleOf(
                    Intent.EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                    Constants.EXTRA_APP_NAME to TEST_APP_NAME,
                )
            )

        assertCheckboxNotShown("Distance")
        assertCheckboxNotShown("Steps")

        scenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager.findFragmentByTag("")
            (fragment as AppDataFragment).triggerDeletionState(DELETE)
        }

        assertCheckboxShown("Distance")
        assertCheckboxShown("Steps")

        verify(healthConnectLogger).logImpression(AppDataElement.SELECT_ALL_BUTTON)
        verify(healthConnectLogger, atLeast(2))
            .logImpression(AppDataElement.PERMISSION_TYPE_BUTTON_WITH_CHECKBOX)
    }

    @Test
    fun inDeletionState_withMedicalData_showsCheckboxes() {
        mockData(
            listOf(
                FitnessPermissionType.DISTANCE,
                FitnessPermissionType.STEPS,
                MedicalPermissionType.ALLERGIES_INTOLERANCES,
                MedicalPermissionType.VACCINES,
            )
        )

        val scenario =
            launchFragment<AppDataFragment>(
                bundleOf(
                    Intent.EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                    Constants.EXTRA_APP_NAME to TEST_APP_NAME,
                )
            )

        assertCheckboxNotShown("Distance")
        assertCheckboxNotShown("Steps")
        assertCheckboxNotShown("Allergies")
        assertCheckboxNotShown("Vaccines")

        scenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager.findFragmentByTag("")
            (fragment as AppDataFragment).triggerDeletionState(DELETE)
        }

        assertCheckboxShown("Allergies")
        assertCheckboxShown("Vaccines")
        assertCheckboxShown("Distance")
        assertCheckboxShown("Steps")
        verify(healthConnectLogger).logImpression(AppDataElement.SELECT_ALL_BUTTON)
        verify(healthConnectLogger, atLeast(4))
            .logImpression(AppDataElement.PERMISSION_TYPE_BUTTON_WITH_CHECKBOX)
    }

    @Test
    fun inDeletionState_checkedItemsAddedToDeleteSet() = runTest {
        mockData(listOf(FitnessPermissionType.DISTANCE, FitnessPermissionType.STEPS))

        val scenario =
            launchFragment<AppDataFragment>(
                bundleOf(
                    Intent.EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                    Constants.EXTRA_APP_NAME to TEST_APP_NAME,
                )
            )
        scenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager.findFragmentByTag("")
            (fragment as AppDataFragment).triggerDeletionState(DELETE)
        }

        onView(withText("Distance")).perform(click())
        onIdle()
        assertThat(appDataViewModel.setOfPermissionTypesToBeDeleted.value)
            .containsExactlyElementsIn(setOf(FitnessPermissionType.DISTANCE))
        verify(healthConnectLogger)
            .logInteraction(AppDataElement.PERMISSION_TYPE_BUTTON_WITH_CHECKBOX)
        onView(withText("Distance")).perform(click())
        assertThat(appDataViewModel.setOfPermissionTypesToBeDeleted.value).isEmpty()
    }

    @Test
    fun inDeletionState_withMedicalData_checkedItemsAddedToDeleteSet() {
        mockData(
            listOf(
                FitnessPermissionType.DISTANCE,
                FitnessPermissionType.STEPS,
                MedicalPermissionType.ALLERGIES_INTOLERANCES,
                MedicalPermissionType.VACCINES,
            )
        )

        val scenario =
            launchFragment<AppDataFragment>(
                bundleOf(
                    Intent.EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                    Constants.EXTRA_APP_NAME to TEST_APP_NAME,
                )
            )
        scenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager.findFragmentByTag("")
            (fragment as AppDataFragment).triggerDeletionState(DELETE)
        }

        onView(withText("Vaccines")).perform(scrollTo()).perform(click())
        onIdle()
        assertThat(appDataViewModel.setOfPermissionTypesToBeDeleted.value)
            .containsExactlyElementsIn(setOf(MedicalPermissionType.VACCINES))
        verify(healthConnectLogger)
            .logInteraction(AppDataElement.PERMISSION_TYPE_BUTTON_WITH_CHECKBOX)
        onView(withText("Vaccines")).perform(click())
        assertThat(appDataViewModel.setOfPermissionTypesToBeDeleted.value).isEmpty()
    }

    @Test
    fun inDeletionState_checkboxesRemainOnOrientationChange() = runTest {
        mockData(listOf(FitnessPermissionType.DISTANCE, FitnessPermissionType.STEPS))

        val scenario =
            launchFragment<AppDataFragment>(
                bundleOf(
                    Intent.EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                    Constants.EXTRA_APP_NAME to TEST_APP_NAME,
                )
            )
        scenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager.findFragmentByTag("")
            (fragment as AppDataFragment).triggerDeletionState(DELETE)
        }

        assertCheckboxShown("Distance")
        assertCheckboxShown("Steps")
        onView(withText("Distance")).perform(click())

        scenario.recreate()
        scenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager.findFragmentByTag("") as AppDataFragment
            val fitnessCategoryPreference =
                fragment.preferenceScreen.findPreference("key_permission_types")
                    as PreferenceCategory?
            fitnessCategoryPreference?.children?.forEach { preference ->
                if (preference is PreferenceCategory) {
                    preference.children.forEach { permissionTypePreference ->
                        if (permissionTypePreference is DeletionPermissionTypesPreference) {
                            if (
                                permissionTypePreference.getHealthPermissionType() ==
                                    FitnessPermissionType.DISTANCE
                            ) {
                                assertThat(permissionTypePreference.getIsChecked()).isTrue()
                            } else if (
                                permissionTypePreference.getHealthPermissionType() ==
                                    FitnessPermissionType.STEPS
                            ) {
                                assertThat(permissionTypePreference.getIsChecked()).isFalse()
                            }
                        }
                    }
                }
            }
        }
        assertCheckboxShown("Distance")
        assertCheckboxShown("Steps")
    }

    @Test
    fun inDeletionState_withMedicalData_checkboxesRemainOnOrientationChange() = runTest {
        mockData(
            listOf(
                FitnessPermissionType.DISTANCE,
                FitnessPermissionType.STEPS,
                MedicalPermissionType.PREGNANCY,
            )
        )

        val scenario =
            launchFragment<AppDataFragment>(
                bundleOf(
                    Intent.EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                    Constants.EXTRA_APP_NAME to TEST_APP_NAME,
                )
            )
        scenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager.findFragmentByTag("")
            (fragment as AppDataFragment).triggerDeletionState(DELETE)
        }

        assertCheckboxShown("Distance")
        assertCheckboxShown("Steps")
        assertCheckboxShown("Pregnancy")
        onView(withText("Distance")).perform(click())
        onView(withText("Pregnancy")).perform(scrollTo()).perform(click())

        scenario.recreate()
        scenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager.findFragmentByTag("") as AppDataFragment
            val fitnessCategoryPreference =
                fragment.preferenceScreen.findPreference("key_permission_types")
                    as PreferenceCategory?
            fitnessCategoryPreference?.children?.forEach { preference ->
                if (preference is PreferenceCategory) {
                    preference.children.forEach { permissionTypePreference ->
                        if (permissionTypePreference is DeletionPermissionTypesPreference) {
                            if (
                                permissionTypePreference.getHealthPermissionType() ==
                                    FitnessPermissionType.DISTANCE
                            ) {
                                assertThat(permissionTypePreference.getIsChecked()).isTrue()
                            } else if (
                                permissionTypePreference.getHealthPermissionType() ==
                                    FitnessPermissionType.STEPS
                            ) {
                                assertThat(permissionTypePreference.getIsChecked()).isFalse()
                            } else if (
                                permissionTypePreference.getHealthPermissionType() ==
                                    MedicalPermissionType.PREGNANCY
                            ) {
                                assertThat(permissionTypePreference.getIsChecked()).isTrue()
                            }
                        }
                    }
                }
            }
        }
        assertCheckboxShown("Distance")
        assertCheckboxShown("Steps")
        assertCheckboxShown("Pregnancy")
    }

    @Test
    fun inDeletionState_displaysSelectAllButton() = runTest {
        mockData(listOf(FitnessPermissionType.DISTANCE, FitnessPermissionType.STEPS))

        val scenario =
            launchFragment<AppDataFragment>(
                bundleOf(
                    Intent.EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                    Constants.EXTRA_APP_NAME to TEST_APP_NAME,
                )
            )
        scenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager.findFragmentByTag("")
            (fragment as AppDataFragment).triggerDeletionState(DELETE)
        }

        assertCheckboxShown("Select all")
    }

    @Test
    fun inDeletionState_withMedicalData_displaysSelectAllButton() = runTest {
        mockData(
            listOf(
                FitnessPermissionType.DISTANCE,
                FitnessPermissionType.STEPS,
                MedicalPermissionType.SOCIAL_HISTORY,
                MedicalPermissionType.PROCEDURES,
            )
        )

        val scenario =
            launchFragment<AppDataFragment>(
                bundleOf(
                    Intent.EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                    Constants.EXTRA_APP_NAME to TEST_APP_NAME,
                )
            )
        scenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager.findFragmentByTag("")
            (fragment as AppDataFragment).triggerDeletionState(DELETE)
        }

        assertCheckboxShown("Select all")
    }

    @Test
    fun inDeletionState_onSelectAllChecked_allPermissionTypesChecked() = runTest {
        mockData(listOf(FitnessPermissionType.DISTANCE, FitnessPermissionType.STEPS))
        val scenario =
            launchFragment<AppDataFragment>(
                bundleOf(
                    Intent.EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                    Constants.EXTRA_APP_NAME to TEST_APP_NAME,
                )
            )
        scenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager.findFragmentByTag("") as AppDataFragment
            fragment.triggerDeletionState(DELETE)
            val permissionTypesGroupPreference =
                fragment.preferenceScreen.findPreference("key_permission_types")
                    as EmptyPreferenceCategory?

            permissionTypesGroupPreference?.children?.forEach { preference ->
                if (preference is PreferenceCategory) {
                    preference.children.forEach { permissionTypePreference ->
                        if (permissionTypePreference is DeletionPermissionTypesPreference) {
                            if (
                                permissionTypePreference.getHealthPermissionType() in
                                    listOf(
                                        FitnessPermissionType.DISTANCE,
                                        FitnessPermissionType.STEPS,
                                    )
                            ) {
                                assertThat(permissionTypePreference.getIsChecked()).isFalse()
                            }
                        }
                    }
                }
            }
        }

        assertCheckboxShown("Select all")
        onView(withText("Select all")).perform(click())
        advanceUntilIdle()

        scenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager.findFragmentByTag("") as AppDataFragment
            val permissionTypesGroupPreference =
                fragment.preferenceScreen.findPreference("key_permission_types")
                    as EmptyPreferenceCategory?

            permissionTypesGroupPreference?.children?.forEach { preference ->
                if (preference is PreferenceCategory) {
                    preference.children.forEach { permissionTypePreference ->
                        if (permissionTypePreference is DeletionPermissionTypesPreference) {
                            if (
                                permissionTypePreference.getHealthPermissionType() in
                                    listOf(
                                        FitnessPermissionType.DISTANCE,
                                        FitnessPermissionType.STEPS,
                                    )
                            ) {
                                assertThat(permissionTypePreference.getIsChecked()).isTrue()
                            }
                        }
                    }
                }
            }
        }

        assertThat(appDataViewModel.setOfPermissionTypesToBeDeleted.value)
            .containsExactlyElementsIn(
                setOf(FitnessPermissionType.DISTANCE, FitnessPermissionType.STEPS)
            )
        verify(healthConnectLogger).logInteraction(AppDataElement.SELECT_ALL_BUTTON)
    }

    @Test
    fun inDeletionState_withMedicalData_onSelectAllChecked_allPermissionTypesChecked() = runTest {
        mockData(
            listOf(
                FitnessPermissionType.DISTANCE,
                FitnessPermissionType.STEPS,
                MedicalPermissionType.VITAL_SIGNS,
                MedicalPermissionType.LABORATORY_RESULTS,
            )
        )
        val scenario =
            launchFragment<AppDataFragment>(
                bundleOf(
                    Intent.EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                    Constants.EXTRA_APP_NAME to TEST_APP_NAME,
                )
            )
        scenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager.findFragmentByTag("") as AppDataFragment
            fragment.triggerDeletionState(DELETE)
            val permissionTypesGroupPreference =
                fragment.preferenceScreen.findPreference("key_permission_types")
                    as EmptyPreferenceCategory?

            permissionTypesGroupPreference?.children?.forEach { preference ->
                if (preference is PreferenceCategory) {
                    preference.children.forEach { permissionTypePreference ->
                        if (permissionTypePreference is DeletionPermissionTypesPreference) {
                            if (
                                permissionTypePreference.getHealthPermissionType() in
                                    listOf(
                                        FitnessPermissionType.DISTANCE,
                                        FitnessPermissionType.STEPS,
                                        MedicalPermissionType.VITAL_SIGNS,
                                        MedicalPermissionType.LABORATORY_RESULTS,
                                    )
                            ) {
                                assertThat(permissionTypePreference.getIsChecked()).isFalse()
                            }
                        }
                    }
                }
            }
        }

        assertCheckboxShown("Select all")
        onView(withText("Select all")).perform(click())
        advanceUntilIdle()

        scenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager.findFragmentByTag("") as AppDataFragment
            val permissionTypesGroupPreference =
                fragment.preferenceScreen.findPreference("key_permission_types")
                    as EmptyPreferenceCategory?

            permissionTypesGroupPreference?.children?.forEach { preference ->
                if (preference is PreferenceCategory) {
                    preference.children.forEach { permissionTypePreference ->
                        if (permissionTypePreference is DeletionPermissionTypesPreference) {
                            if (
                                permissionTypePreference.getHealthPermissionType() in
                                    listOf(
                                        FitnessPermissionType.DISTANCE,
                                        FitnessPermissionType.STEPS,
                                        MedicalPermissionType.VITAL_SIGNS,
                                        MedicalPermissionType.LABORATORY_RESULTS,
                                    )
                            ) {
                                assertThat(permissionTypePreference.getIsChecked()).isTrue()
                            }
                        }
                    }
                }
            }
        }

        assertThat(appDataViewModel.setOfPermissionTypesToBeDeleted.value)
            .containsExactlyElementsIn(
                setOf(
                    FitnessPermissionType.DISTANCE,
                    FitnessPermissionType.STEPS,
                    MedicalPermissionType.LABORATORY_RESULTS,
                    MedicalPermissionType.VITAL_SIGNS,
                )
            )
        verify(healthConnectLogger).logInteraction(AppDataElement.SELECT_ALL_BUTTON)
    }

    @Test
    fun inDeletionState_onSelectAllUnchecked_allPermissionTypesUnchecked() = runTest {
        mockData(listOf(FitnessPermissionType.DISTANCE, FitnessPermissionType.STEPS))
        val scenario =
            launchFragment<AppDataFragment>(
                bundleOf(
                    Intent.EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                    Constants.EXTRA_APP_NAME to TEST_APP_NAME,
                )
            )
        scenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager.findFragmentByTag("") as AppDataFragment
            fragment.triggerDeletionState(DELETE)
        }
        assertCheckboxShown("Select all")
        onView(withText("Select all")).perform(click())
        assertThat(appDataViewModel.setOfPermissionTypesToBeDeleted.value)
            .containsExactlyElementsIn(
                setOf(FitnessPermissionType.DISTANCE, FitnessPermissionType.STEPS)
            )
        onView(withText("Select all")).perform(click())

        scenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager.findFragmentByTag("") as AppDataFragment
            val permissionTypesGroupPreference =
                fragment.preferenceScreen.findPreference("key_permission_types")
                    as EmptyPreferenceCategory?

            permissionTypesGroupPreference?.children?.forEach { preference ->
                if (preference is PreferenceCategory) {
                    preference.children.forEach { permissionTypePreference ->
                        if (permissionTypePreference is DeletionPermissionTypesPreference) {
                            if (
                                permissionTypePreference.getHealthPermissionType() in
                                    listOf(
                                        FitnessPermissionType.DISTANCE,
                                        FitnessPermissionType.STEPS,
                                    )
                            ) {
                                assertThat(permissionTypePreference.getIsChecked()).isFalse()
                            }
                        }
                    }
                }
            }
        }
        assertThat(appDataViewModel.setOfPermissionTypesToBeDeleted.value).isEmpty()
    }

    @Test
    fun inDeletionState_withMedicalData_onSelectAllUnchecked_allPermissionTypesUnchecked() =
        runTest {
            mockData(
                listOf(
                    FitnessPermissionType.DISTANCE,
                    FitnessPermissionType.STEPS,
                    MedicalPermissionType.ALLERGIES_INTOLERANCES,
                    MedicalPermissionType.VACCINES,
                )
            )
            val scenario =
                launchFragment<AppDataFragment>(
                    bundleOf(
                        Intent.EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                        Constants.EXTRA_APP_NAME to TEST_APP_NAME,
                    )
                )
            scenario.onActivity { activity ->
                val fragment =
                    activity.supportFragmentManager.findFragmentByTag("") as AppDataFragment
                fragment.triggerDeletionState(DELETE)
            }
            assertCheckboxShown("Select all")
            onView(withText("Select all")).perform(click())
            assertThat(appDataViewModel.setOfPermissionTypesToBeDeleted.value)
                .containsExactlyElementsIn(
                    setOf(
                        FitnessPermissionType.DISTANCE,
                        FitnessPermissionType.STEPS,
                        MedicalPermissionType.ALLERGIES_INTOLERANCES,
                        MedicalPermissionType.VACCINES,
                    )
                )
            onView(withText("Select all")).perform(click())

            scenario.onActivity { activity ->
                val fragment =
                    activity.supportFragmentManager.findFragmentByTag("") as AppDataFragment
                val permissionTypesGroupPreference =
                    fragment.preferenceScreen.findPreference("key_permission_types")
                        as EmptyPreferenceCategory?

                permissionTypesGroupPreference?.children?.forEach { preference ->
                    if (preference is PreferenceCategory) {
                        preference.children.forEach { permissionTypePreference ->
                            if (permissionTypePreference is DeletionPermissionTypesPreference) {
                                if (
                                    permissionTypePreference.getHealthPermissionType() in
                                        listOf(
                                            FitnessPermissionType.DISTANCE,
                                            FitnessPermissionType.STEPS,
                                            MedicalPermissionType.ALLERGIES_INTOLERANCES,
                                            MedicalPermissionType.VACCINES,
                                        )
                                ) {
                                    assertThat(permissionTypePreference.getIsChecked()).isFalse()
                                }
                            }
                        }
                    }
                }
            }
            assertThat(appDataViewModel.setOfPermissionTypesToBeDeleted.value).isEmpty()
        }

    @Test
    fun inDeletionState_selectAllChecked_checkboxesRemainOnOrientationChange() = runTest {
        mockData(listOf(FitnessPermissionType.DISTANCE, FitnessPermissionType.STEPS))

        val scenario =
            launchFragment<AppDataFragment>(
                bundleOf(
                    Intent.EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                    Constants.EXTRA_APP_NAME to TEST_APP_NAME,
                )
            )
        scenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager.findFragmentByTag("") as AppDataFragment
            fragment.triggerDeletionState(DELETE)
        }

        assertCheckboxShown("Select all")
        onView(withText("Select all")).perform(click())

        scenario.recreate()
        onView(withText("Select all")).perform(scrollTo())
        scenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager.findFragmentByTag("") as AppDataFragment
            val selectAllCheckboxPreference =
                fragment.preferenceScreen.findPreference("key_select_all")
                    as SelectAllCheckboxPreference?
            assertThat(selectAllCheckboxPreference?.getIsChecked()).isTrue()
            fragment.preferenceScreen.children.forEach { preference ->
                if (preference is PreferenceCategory) {
                    preference.children.forEach { permissionTypePreference ->
                        if (permissionTypePreference is DeletionPermissionTypesPreference) {
                            assertThat(permissionTypePreference.getIsChecked()).isTrue()
                        }
                    }
                }
            }
        }
        assertCheckboxShown("Distance")
        assertCheckboxShown("Steps")
    }

    @Test
    fun inDeletionState_withMedicalData_selectAllChecked_checkboxesRemainOnOrientationChange() =
        runTest {
            mockData(
                listOf(
                    FitnessPermissionType.DISTANCE,
                    FitnessPermissionType.STEPS,
                    MedicalPermissionType.VITAL_SIGNS,
                    MedicalPermissionType.ALLERGIES_INTOLERANCES,
                )
            )

            val scenario =
                launchFragment<AppDataFragment>(
                    bundleOf(
                        Intent.EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                        Constants.EXTRA_APP_NAME to TEST_APP_NAME,
                    )
                )
            scenario.onActivity { activity ->
                val fragment =
                    activity.supportFragmentManager.findFragmentByTag("") as AppDataFragment
                fragment.triggerDeletionState(DELETE)
            }

            assertCheckboxShown("Select all")
            onView(withText("Select all")).perform(click())

            scenario.recreate()
            onView(withText("Select all")).perform(scrollTo())
            scenario.onActivity { activity ->
                val fragment =
                    activity.supportFragmentManager.findFragmentByTag("") as AppDataFragment
                val selectAllCheckboxPreference =
                    fragment.preferenceScreen.findPreference("key_select_all")
                        as SelectAllCheckboxPreference?
                assertThat(selectAllCheckboxPreference?.getIsChecked()).isTrue()
                fragment.preferenceScreen.children.forEach { preference ->
                    if (preference is PreferenceCategory) {
                        preference.children.forEach { permissionTypePreference ->
                            if (permissionTypePreference is DeletionPermissionTypesPreference) {
                                assertThat(permissionTypePreference.getIsChecked()).isTrue()
                            }
                        }
                    }
                }
            }
            assertCheckboxShown("Distance")
            assertCheckboxShown("Steps")
            assertCheckboxShown("Allergies")
            assertCheckboxShown("Vital signs")
        }

    @Test
    fun inDeletionState_selectAllChecked_oneUnchecked_selectAllUnchecked() = runTest {
        mockData(listOf(FitnessPermissionType.DISTANCE, FitnessPermissionType.MENSTRUATION))

        val scenario =
            launchFragment<AppDataFragment>(
                bundleOf(
                    Intent.EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                    Constants.EXTRA_APP_NAME to TEST_APP_NAME,
                )
            )
        scenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager.findFragmentByTag("") as AppDataFragment
            fragment.triggerDeletionState(DELETE)
        }
        advanceUntilIdle()
        assertCheckboxShown("Select all")
        onView(withText("Select all")).perform(click())
        onView(withText("Distance")).perform(click())
        scenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager.findFragmentByTag("") as AppDataFragment
            val selectAllCheckboxPreference =
                fragment.preferenceScreen.findPreference("key_select_all")
                    as SelectAllCheckboxPreference?
            assertThat(selectAllCheckboxPreference?.getIsChecked()).isFalse()
        }
    }

    @Test
    fun inDeletionState_withMedicalData_selectAllChecked_oneUnchecked_selectAllUnchecked() =
        runTest {
            mockData(
                listOf(
                    FitnessPermissionType.DISTANCE,
                    FitnessPermissionType.MENSTRUATION,
                    MedicalPermissionType.PREGNANCY,
                )
            )

            val scenario =
                launchFragment<AppDataFragment>(
                    bundleOf(
                        Intent.EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                        Constants.EXTRA_APP_NAME to TEST_APP_NAME,
                    )
                )
            scenario.onActivity { activity ->
                val fragment =
                    activity.supportFragmentManager.findFragmentByTag("") as AppDataFragment
                fragment.triggerDeletionState(DELETE)
            }
            advanceUntilIdle()
            assertCheckboxShown("Select all")
            onView(withText("Select all")).perform(click())
            onView(withText("Pregnancy")).perform(click())
            scenario.onActivity { activity ->
                val fragment =
                    activity.supportFragmentManager.findFragmentByTag("") as AppDataFragment
                val selectAllCheckboxPreference =
                    fragment.preferenceScreen.findPreference("key_select_all")
                        as SelectAllCheckboxPreference?
                assertThat(selectAllCheckboxPreference?.getIsChecked()).isFalse()
            }
        }

    @Test
    fun inDeletionState_allPermissionTypesChecked_selectAllShouldBeChecked() = runTest {
        mockData(listOf(FitnessPermissionType.DISTANCE, FitnessPermissionType.MENSTRUATION))

        val scenario =
            launchFragment<AppDataFragment>(
                bundleOf(
                    Intent.EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                    Constants.EXTRA_APP_NAME to TEST_APP_NAME,
                )
            )
        scenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager.findFragmentByTag("") as AppDataFragment
            fragment.triggerDeletionState(DELETE)
        }
        advanceUntilIdle()

        assertCheckboxShown("Distance")
        assertCheckboxShown("Menstruation")
        onView(withText("Distance")).perform(click())
        onView(withText("Menstruation")).perform(click())
        scenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager.findFragmentByTag("") as AppDataFragment
            val selectAllCheckboxPreference =
                fragment.preferenceScreen.findPreference("key_select_all")
                    as SelectAllCheckboxPreference?
            assertThat(selectAllCheckboxPreference?.getIsChecked()).isTrue()
        }
    }

    @Test
    fun inDeletionState_withMedicalData_allPermissionTypesChecked_selectAllShouldBeChecked() =
        runTest {
            mockData(
                listOf(
                    FitnessPermissionType.DISTANCE,
                    FitnessPermissionType.MENSTRUATION,
                    MedicalPermissionType.SOCIAL_HISTORY,
                )
            )

            val scenario =
                launchFragment<AppDataFragment>(
                    bundleOf(
                        Intent.EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                        Constants.EXTRA_APP_NAME to TEST_APP_NAME,
                    )
                )
            scenario.onActivity { activity ->
                val fragment =
                    activity.supportFragmentManager.findFragmentByTag("") as AppDataFragment
                fragment.triggerDeletionState(DELETE)
            }
            advanceUntilIdle()

            assertCheckboxShown("Distance")
            assertCheckboxShown("Menstruation")
            assertCheckboxShown("Social history")
            onView(withText("Distance")).perform(click())
            onView(withText("Menstruation")).perform(scrollTo()).perform(click())
            onView(withText("Social history")).perform(scrollTo()).perform(click())
            scenario.onActivity { activity ->
                val fragment =
                    activity.supportFragmentManager.findFragmentByTag("") as AppDataFragment
                val selectAllCheckboxPreference =
                    fragment.preferenceScreen.findPreference("key_select_all")
                        as SelectAllCheckboxPreference?
                assertThat(selectAllCheckboxPreference?.getIsChecked()).isTrue()
            }
        }

    private fun mockData(permissionTypesList: List<HealthPermissionType>) {
        val recordTypeInfoMap =
            permissionTypesList.filterIsInstance<FitnessPermissionType>().associate {
                fitnessPermissionType ->
                val permissionCategory = fitnessPermissionType.category
                val healthCategory = fromFitnessPermissionType(fitnessPermissionType)
                val dataType =
                    HealthPermissionToDatatypeMapper.getDataTypes(fitnessPermissionType)[0]

                dataType to
                    RecordTypeInfoResponse(
                        permissionCategory,
                        healthCategory,
                        listOf(getDataOrigin(TEST_APP_PACKAGE_NAME)),
                    )
            }

        val medicalResourceTypeResources =
            permissionTypesList.filterIsInstance<MedicalPermissionType>().map {
                MedicalResourceTypeInfo(toMedicalResourceType(it), setOf(TEST_MEDICAL_DATA_SOURCE))
            }

        doAnswer(prepareAnswer(recordTypeInfoMap))
            .`when`(manager)
            .queryAllRecordTypesInfo(any(), any())

        doAnswer(prepareAnswer(medicalResourceTypeResources))
            .`when`(manager)
            .queryAllMedicalResourceTypeInfos(any(), any())
    }

    private fun prepareAnswer(
        recordTypeInfoMap: Map<Class<out Record>, RecordTypeInfoResponse>
    ): (InvocationOnMock) -> Map<Class<out Record>, RecordTypeInfoResponse> {
        val answer = { args: InvocationOnMock ->
            val receiver = args.arguments[1] as OutcomeReceiver<Any?, *>
            receiver.onResult(recordTypeInfoMap)
            recordTypeInfoMap
        }
        return answer
    }

    private fun prepareAnswer(
        medicalResourceTypeInfo: List<MedicalResourceTypeInfo>
    ): (InvocationOnMock) -> List<MedicalResourceTypeInfo> {
        val answer = { args: InvocationOnMock ->
            val receiver = args.arguments[1] as OutcomeReceiver<Any?, *>
            receiver.onResult(medicalResourceTypeInfo)
            medicalResourceTypeInfo
        }
        return answer
    }

    private fun assertCheckboxShown(title: String, tag: String = "checkbox") {
        onView(ViewMatchers.withId(androidx.preference.R.id.recycler_view))
            .check(
                matches(
                    Matchers.allOf(
                        ViewMatchers.hasDescendant(withText(title)),
                        ViewMatchers.hasDescendant(ViewMatchers.withTagValue(Matchers.`is`(tag))),
                    )
                )
            )
    }

    private fun assertCheckboxNotShown(title: String, tag: String = "checkbox") {
        onView(ViewMatchers.withId(androidx.preference.R.id.recycler_view))
            .check(
                matches(
                    Matchers.allOf(
                        ViewMatchers.hasDescendant(withText(title)),
                        Matchers.not(
                            ViewMatchers.hasDescendant(
                                ViewMatchers.withTagValue(Matchers.`is`(tag))
                            )
                        ),
                    )
                )
            )
    }
}
