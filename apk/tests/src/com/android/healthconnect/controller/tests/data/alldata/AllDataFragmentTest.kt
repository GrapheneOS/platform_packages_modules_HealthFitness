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
package com.android.healthconnect.controller.tests.data.alldata

import android.content.Context
import android.health.connect.HealthConnectManager
import android.health.connect.MedicalResourceTypeInfo
import android.health.connect.RecordTypeInfoResponse
import android.health.connect.datatypes.MedicalDataSource
import android.health.connect.datatypes.Record
import android.os.OutcomeReceiver
import androidx.core.os.bundleOf
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.preference.PreferenceCategory
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onIdle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withTagValue
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.data.alldata.AllDataFragment
import com.android.healthconnect.controller.data.alldata.AllDataFragment.Companion.IS_BROWSE_MEDICAL_DATA_SCREEN
import com.android.healthconnect.controller.data.alldata.AllDataViewModel
import com.android.healthconnect.controller.data.appdata.AllDataUseCase
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.permissions.data.MedicalPermissionType
import com.android.healthconnect.controller.permissions.data.MedicalPermissionType.ALLERGIES_INTOLERANCES
import com.android.healthconnect.controller.permissions.data.MedicalPermissionType.VACCINES
import com.android.healthconnect.controller.permissions.data.toMedicalResourceType
import com.android.healthconnect.controller.selectabledeletion.DeletionDataViewModel.DeletionScreenState.DELETE
import com.android.healthconnect.controller.selectabledeletion.DeletionPermissionTypesPreference
import com.android.healthconnect.controller.selectabledeletion.SelectAllCheckboxPreference
import com.android.healthconnect.controller.service.HealthManagerModule
import com.android.healthconnect.controller.shared.HealthDataCategoryExtensions.fromFitnessPermissionType
import com.android.healthconnect.controller.shared.HealthPermissionToDatatypeMapper
import com.android.healthconnect.controller.shared.children
import com.android.healthconnect.controller.tests.TestActivity
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.TEST_MEDICAL_DATA_SOURCE
import com.android.healthconnect.controller.tests.utils.getDataOrigin
import com.android.healthconnect.controller.tests.utils.launchFragment
import com.android.healthconnect.controller.tests.utils.setLocale
import com.android.healthconnect.controller.utils.logging.AllDataElement
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.PageName
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
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
@UninstallModules(HealthManagerModule::class)
@RunWith(AndroidJUnit4::class)
class AllDataFragmentTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @BindValue val manager: HealthConnectManager = mock()

    private val allDataUseCase: AllDataUseCase = AllDataUseCase(manager, Dispatchers.Main)

    @BindValue val allDataViewModel: AllDataViewModel = AllDataViewModel(allDataUseCase)
    @BindValue val healthConnectLogger: HealthConnectLogger = mock()
    private lateinit var navHostController: TestNavHostController
    private lateinit var context: Context

    @Before
    fun setup() {
        hiltRule.inject()
        context = InstrumentationRegistry.getInstrumentation().context
        navHostController = TestNavHostController(context)
        context.setLocale(Locale.US)
    }

    @After
    fun tearDown() {
        reset(healthConnectLogger)
        // Wait for all threads to complete so the test activity is not called back after teardown
        onIdle()
    }

    @Test
    fun populatedDataTypesDisplayed_impressionsLogged() {
        mockData(
            listOf(
                FitnessPermissionType.STEPS,
                FitnessPermissionType.HEART_RATE,
                FitnessPermissionType.BASAL_BODY_TEMPERATURE,
            )
        )

        launchFragment<AllDataFragment>()

        onView(withText("Steps")).check(matches(isDisplayed()))
        onView(withText("Heart rate")).check(matches(isDisplayed()))
        onView(withText("Basal body temperature")).check(matches(isDisplayed()))
        onView(withText("No data")).check(doesNotExist())
        verify(healthConnectLogger, atLeast(1)).setPageId(PageName.ALL_DATA_PAGE)
        verify(healthConnectLogger).logPageImpression()
        verify(healthConnectLogger, times(3))
            .logImpression(AllDataElement.PERMISSION_TYPE_BUTTON_NO_CHECKBOX)
    }

    @Test
    fun populatedMedicalData_pageImpressionLogged() {
        mockData(listOf(VACCINES, ALLERGIES_INTOLERANCES), setOf(TEST_MEDICAL_DATA_SOURCE))

        launchMedicalAllDataFragment()

        verify(healthConnectLogger, atLeast(1)).setPageId(PageName.ALL_MEDICAL_DATA_PAGE)
        verify(healthConnectLogger).logPageImpression()
    }

    @Test
    fun medicalDataPresent_populatedDataTypesDisplayed() {
        mockData(listOf(VACCINES, ALLERGIES_INTOLERANCES), setOf(TEST_MEDICAL_DATA_SOURCE))

        launchMedicalAllDataFragment()

        onView(withText("Allergies")).check(matches(isDisplayed()))
        onView(withText("Vaccines")).check(matches(isDisplayed()))
        onView(withText("Distance")).check(doesNotExist())
        onView(withText("No data")).check(doesNotExist())
        onView(withText("Select all")).check(doesNotExist())
    }

    @Test
    fun whenNoData_noDataMessageDisplayed() {
        mockData(emptyList())

        launchFragment<AllDataFragment>()

        onView(withText("No data")).check(matches(isDisplayed()))
        onView(withText("Data from apps with access to Health\u00A0Connect will show here"))
            .check(matches(isDisplayed()))
        onView(
                withText(
                    "This includes all the health records synced to and added to Health\u00A0Connect. This might not be your full medical record and does not include a medical description of your health records."
                )
            )
            .check(doesNotExist())
    }

    @Test
    fun whenFitnessDataTypesDisplayed_topIntroNotShown() {
        mockData(
            listOf(
                FitnessPermissionType.STEPS,
                FitnessPermissionType.HEART_RATE,
                FitnessPermissionType.BASAL_BODY_TEMPERATURE,
            )
        )

        launchFragment<AllDataFragment>()

        onView(
                withText(
                    "This includes all the health records synced to and added to Health\u00A0Connect. This might not be your full medical record and does not include a medical description of your health records."
                )
            )
            .check(doesNotExist())
    }

    @Test
    fun whenMedicalDataTypesDisplayed_topIntroShown() {
        mockData(listOf(VACCINES, ALLERGIES_INTOLERANCES), setOf(TEST_MEDICAL_DATA_SOURCE))

        launchMedicalAllDataFragment()

        onView(
                withText(
                    "This includes all the health records synced to and added to Health\u00A0Connect. This might not be your full medical record and does not include a medical description of your health records."
                )
            )
            .check(matches(isDisplayed()))
        onView(withText("About health records")).check(matches(isDisplayed()))
    }

    @Test
    fun navigatesToAllEntries() {
        mockData(listOf(FitnessPermissionType.STEPS))

        launchFragment<AllDataFragment>() {
            navHostController.setGraph(R.navigation.data_nav_graph_new_ia)
            Navigation.setViewNavController(this.requireView(), navHostController)
        }

        onView(withText("Steps")).check(matches(isDisplayed()))
        onView(withText("Steps")).perform(click())
        verify(healthConnectLogger)
            .logInteraction(AllDataElement.PERMISSION_TYPE_BUTTON_NO_CHECKBOX)
        assertThat(navHostController.currentDestination?.id)
            .isEqualTo(R.id.entriesAndAccessFragment)
    }

    @Test
    fun navigatesToMedicalAllEntries() {
        mockData(listOf(VACCINES), setOf(TEST_MEDICAL_DATA_SOURCE))

        launchFragment<AllDataFragment>(bundleOf(IS_BROWSE_MEDICAL_DATA_SCREEN to true)) {
            navHostController.setGraph(R.navigation.medical_data_nav_graph)
            Navigation.setViewNavController(this.requireView(), navHostController)
        }

        onView(withText("Vaccines")).check(matches(isDisplayed()))
        onView(withText("Vaccines")).perform(click())
        verify(healthConnectLogger)
            .logInteraction(AllDataElement.PERMISSION_TYPE_BUTTON_NO_CHECKBOX)
        assertThat(navHostController.currentDestination?.id)
            .isEqualTo(R.id.entriesAndAccessFragment)
    }

    @Test
    fun triggerDeletionState_showsCheckboxes() {
        mockData(listOf(FitnessPermissionType.DISTANCE, FitnessPermissionType.MENSTRUATION))

        val scenario = launchFragment<AllDataFragment>()

        assertCheckboxNotShown("Distance")
        assertCheckboxNotShown("Menstruation")

        // trigger deletion state
        scenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager.findFragmentByTag("")
            (fragment as AllDataFragment).triggerDeletionState(DELETE)
        }

        assertCheckboxShown("Distance")
        assertCheckboxShown("Menstruation")
        verify(healthConnectLogger).logImpression(AllDataElement.SELECT_ALL_BUTTON)
        verify(healthConnectLogger, atLeast(2))
            .logImpression(AllDataElement.PERMISSION_TYPE_BUTTON_WITH_CHECKBOX)
    }

    @Test
    fun triggerDeletionState_medicalData_showsCheckboxes() {
        mockData(listOf(VACCINES, ALLERGIES_INTOLERANCES), setOf(TEST_MEDICAL_DATA_SOURCE))
        val scenario = launchMedicalAllDataFragment()
        assertCheckboxNotShown("Allergies")
        assertCheckboxNotShown("Vaccines")

        scenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager.findFragmentByTag("")
            (fragment as AllDataFragment).triggerDeletionState(DELETE)
        }

        assertCheckboxShown("Allergies")
        assertCheckboxShown("Vaccines")
        verify(healthConnectLogger).logImpression(AllDataElement.SELECT_ALL_BUTTON)
        verify(healthConnectLogger, atLeast(2))
            .logImpression(AllDataElement.PERMISSION_TYPE_BUTTON_WITH_CHECKBOX)
    }

    @Test
    fun inDeletionState_checkedItemsAddedToDeleteSet() {
        mockData(listOf(FitnessPermissionType.DISTANCE, FitnessPermissionType.HEART_RATE))

        val scenario = launchFragment<AllDataFragment>()
        scenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager.findFragmentByTag("")
            (fragment as AllDataFragment).triggerDeletionState(DELETE)
        }

        onView(withText("Distance")).perform(click())
        onIdle()
        assertThat(allDataViewModel.setOfPermissionTypesToBeDeleted.value)
            .containsExactlyElementsIn(setOf(FitnessPermissionType.DISTANCE))
        verify(healthConnectLogger)
            .logInteraction(AllDataElement.PERMISSION_TYPE_BUTTON_WITH_CHECKBOX)
        onView(withText("Distance")).perform(click())
        assertThat(allDataViewModel.setOfPermissionTypesToBeDeleted.value).isEmpty()
    }

    @Test
    fun inDeletionState_medicalData_checkedItemsAddedToDeleteSet() {
        mockData(listOf(VACCINES, ALLERGIES_INTOLERANCES), setOf(TEST_MEDICAL_DATA_SOURCE))

        val scenario = launchMedicalAllDataFragment()
        scenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager.findFragmentByTag("")
            (fragment as AllDataFragment).triggerDeletionState(DELETE)
        }

        onView(withText("Vaccines")).perform(click())
        onIdle()
        assertThat(allDataViewModel.setOfPermissionTypesToBeDeleted.value)
            .containsExactlyElementsIn(setOf(VACCINES))
        verify(healthConnectLogger)
            .logInteraction(AllDataElement.PERMISSION_TYPE_BUTTON_WITH_CHECKBOX)
        onView(withText("Vaccines")).perform(click())
        assertThat(allDataViewModel.setOfPermissionTypesToBeDeleted.value).isEmpty()
    }

    @Test
    fun triggerDeletionState_checkboxesRemainOnOrientationChange() = runTest {
        mockData(listOf(FitnessPermissionType.DISTANCE, FitnessPermissionType.HEART_RATE))

        val scenario = launchFragment<AllDataFragment>()
        scenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager.findFragmentByTag("")
            (fragment as AllDataFragment).triggerDeletionState(DELETE)
        }

        advanceUntilIdle()

        assertCheckboxShown("Distance")
        assertCheckboxShown("Heart rate")
        onView(withText("Distance")).perform(click())

        scenario.recreate()
        scenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager.findFragmentByTag("") as AllDataFragment
            val fitnessCategoryPreference =
                fragment.preferenceScreen.findPreference("key_permission_type")
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
                                    FitnessPermissionType.HEART_RATE
                            ) {
                                assertThat(permissionTypePreference.getIsChecked()).isFalse()
                            }
                        }
                    }
                }
            }
        }

        assertCheckboxShown("Distance")
        assertCheckboxShown("Heart rate")
    }

    @Test
    fun triggerDeletionState_displaysSelectAllButton() {
        mockData(listOf(FitnessPermissionType.DISTANCE, FitnessPermissionType.MENSTRUATION))

        val scenario = launchFragment<AllDataFragment>()

        // trigger deletion state
        scenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager.findFragmentByTag("")
            (fragment as AllDataFragment).triggerDeletionState(DELETE)
        }

        assertCheckboxShown("Select all")
        onView(
                withText(
                    "This includes all the health records synced to and added to Health\u00A0Connect. This might not be your full medical record and does not include a medical description of your health records."
                )
            )
            .check(doesNotExist())
    }

    @Test
    fun triggerDeletionState_medicalData_displaysSelectAllButton() {
        mockData(listOf(VACCINES, ALLERGIES_INTOLERANCES), setOf(TEST_MEDICAL_DATA_SOURCE))
        val scenario = launchMedicalAllDataFragment()

        scenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager.findFragmentByTag("")
            (fragment as AllDataFragment).triggerDeletionState(DELETE)
        }

        assertCheckboxShown("Select all")
    }

    @Test
    fun inDeletionState_onSelectAllChecked_allPermissionTypesChecked() = runTest {
        mockData(listOf(FitnessPermissionType.DISTANCE, FitnessPermissionType.MENSTRUATION))

        val scenario = launchFragment<AllDataFragment>()
        scenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager.findFragmentByTag("")
            (fragment as AllDataFragment).triggerDeletionState(DELETE)
        }

        advanceUntilIdle()

        assertCheckboxShown("Select all")
        onView(withText("Select all")).perform(click())
        assertThat(allDataViewModel.setOfPermissionTypesToBeDeleted.value)
            .containsExactlyElementsIn(
                setOf(FitnessPermissionType.DISTANCE, FitnessPermissionType.MENSTRUATION)
            )
        verify(healthConnectLogger).logInteraction(AllDataElement.SELECT_ALL_BUTTON)
    }

    @Test
    fun inDeletionState_medicalData_onSelectAllChecked_allPermissionTypesChecked() = runTest {
        mockData(listOf(VACCINES, ALLERGIES_INTOLERANCES), setOf(TEST_MEDICAL_DATA_SOURCE))

        val scenario = launchMedicalAllDataFragment()
        scenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager.findFragmentByTag("")
            (fragment as AllDataFragment).triggerDeletionState(DELETE)
        }

        advanceUntilIdle()

        assertCheckboxShown("Select all")
        onView(withText("Select all")).perform(click())
        assertThat(allDataViewModel.setOfPermissionTypesToBeDeleted.value)
            .containsExactlyElementsIn(setOf(VACCINES, ALLERGIES_INTOLERANCES))
        verify(healthConnectLogger).logInteraction(AllDataElement.SELECT_ALL_BUTTON)
    }

    @Test
    fun inDeletionState_onSelectAllUnchecked_allPermissionTypesUnChecked() = runTest {
        mockData(listOf(FitnessPermissionType.DISTANCE, FitnessPermissionType.MENSTRUATION))

        val scenario = launchFragment<AllDataFragment>()
        scenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager.findFragmentByTag("")
            (fragment as AllDataFragment).triggerDeletionState(DELETE)
        }

        advanceUntilIdle()

        assertCheckboxShown("Select all")
        onView(withText("Select all")).perform(click())
        assertThat(allDataViewModel.setOfPermissionTypesToBeDeleted.value)
            .containsExactlyElementsIn(
                setOf(FitnessPermissionType.DISTANCE, FitnessPermissionType.MENSTRUATION)
            )
        onView(withText("Select all")).perform(click())
        assertThat(allDataViewModel.setOfPermissionTypesToBeDeleted.value).isEmpty()
    }

    @Test
    fun inDeletionState_medicalData_onSelectAllUnchecked_allPermissionTypesUnChecked() = runTest {
        mockData(listOf(VACCINES, ALLERGIES_INTOLERANCES), setOf(TEST_MEDICAL_DATA_SOURCE))

        val scenario = launchMedicalAllDataFragment()
        scenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager.findFragmentByTag("")
            (fragment as AllDataFragment).triggerDeletionState(DELETE)
        }

        advanceUntilIdle()

        assertCheckboxShown("Select all")
        onView(withText("Select all")).perform(click())
        assertThat(allDataViewModel.setOfPermissionTypesToBeDeleted.value)
            .containsExactlyElementsIn(setOf(VACCINES, ALLERGIES_INTOLERANCES))
        onView(withText("Select all")).perform(click())
        assertThat(allDataViewModel.setOfPermissionTypesToBeDeleted.value).isEmpty()
    }

    @Test
    fun inDeletionState_allPermissionTypesChecked_selectAllShouldBeChecked() {
        mockData(listOf(FitnessPermissionType.DISTANCE, FitnessPermissionType.MENSTRUATION))

        val scenario = launchFragment<AllDataFragment>()
        scenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager.findFragmentByTag("")
            (fragment as AllDataFragment).triggerDeletionState(DELETE)
        }

        assertCheckboxShown("Distance")
        assertCheckboxShown("Menstruation")
        onView(withText("Distance")).perform(click())
        onView(withText("Menstruation")).perform(click())
        scenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager.findFragmentByTag("") as AllDataFragment
            val selectAllCheckboxPreference =
                fragment.preferenceScreen.findPreference("key_select_all")
                    as SelectAllCheckboxPreference?
            assertThat(selectAllCheckboxPreference?.getIsChecked()).isTrue()
        }
    }

    @Test
    fun inDeletionState_medicalData_allPermissionTypesChecked_selectAllShouldBeChecked() {
        mockData(listOf(VACCINES, ALLERGIES_INTOLERANCES), setOf(TEST_MEDICAL_DATA_SOURCE))

        val scenario = launchMedicalAllDataFragment()
        scenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager.findFragmentByTag("")
            (fragment as AllDataFragment).triggerDeletionState(DELETE)
        }

        assertCheckboxShown("Allergies")
        assertCheckboxShown("Vaccines")
        onView(withText("Allergies")).perform(click())
        onView(withText("Vaccines")).perform(click())
        scenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager.findFragmentByTag("") as AllDataFragment
            val selectAllCheckboxPreference =
                fragment.preferenceScreen.findPreference("key_select_all")
                    as SelectAllCheckboxPreference?
            assertThat(selectAllCheckboxPreference?.getIsChecked()).isTrue()
        }
    }

    @Test
    fun inDeletionState_selectAllChecked_oneUnchecked_selectAllUnchecked() = runTest {
        mockData(listOf(FitnessPermissionType.DISTANCE, FitnessPermissionType.MENSTRUATION))

        val scenario = launchFragment<AllDataFragment>()
        scenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager.findFragmentByTag("")
            (fragment as AllDataFragment).triggerDeletionState(DELETE)
        }

        advanceUntilIdle()

        assertCheckboxShown("Select all")
        onView(withText("Select all")).perform(click())
        onView(withText("Distance")).perform(click())
        scenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager.findFragmentByTag("") as AllDataFragment
            val selectAllCheckboxPreference =
                fragment.preferenceScreen.findPreference("key_select_all")
                    as SelectAllCheckboxPreference?
            assertThat(selectAllCheckboxPreference?.getIsChecked()).isFalse()
        }
    }

    @Test
    fun inDeletionState_medicalData_selectAllChecked_oneUnchecked_selectAllUnchecked() = runTest {
        mockData(listOf(VACCINES, ALLERGIES_INTOLERANCES), setOf(TEST_MEDICAL_DATA_SOURCE))

        val scenario = launchMedicalAllDataFragment()
        scenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager.findFragmentByTag("")
            (fragment as AllDataFragment).triggerDeletionState(DELETE)
        }

        advanceUntilIdle()

        assertCheckboxShown("Select all")
        onView(withText("Select all")).perform(click())
        onView(withText("Allergies")).perform(click())
        scenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager.findFragmentByTag("") as AllDataFragment
            val selectAllCheckboxPreference =
                fragment.preferenceScreen.findPreference("key_select_all")
                    as SelectAllCheckboxPreference?
            assertThat(selectAllCheckboxPreference?.getIsChecked()).isFalse()
        }
    }

    @Test
    fun inDeletionState_selectAllChecked_checkboxesRemainOnOrientationChange() = runTest {
        mockData(listOf(FitnessPermissionType.DISTANCE, FitnessPermissionType.HEART_RATE))

        val scenario = launchFragment<AllDataFragment>()
        scenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager.findFragmentByTag("")
            (fragment as AllDataFragment).triggerDeletionState(DELETE)
        }

        advanceUntilIdle()

        assertCheckboxShown("Select all")
        onView(withText("Select all")).perform(click())

        scenario.recreate()

        onView(withText("Select all")).perform(scrollTo())
        scenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager.findFragmentByTag("") as AllDataFragment
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
        assertCheckboxShown("Heart rate")
        onView(
                withText(
                    "This includes all the health records synced to and added to Health\u00A0Connect. This might not be your full medical record and does not include a medical description of your health records."
                )
            )
            .check(doesNotExist())
    }

    @Test
    fun inDeletionState_medicalData_checkboxesRemainOnOrientationChange() = runTest {
        mockData(listOf(VACCINES, ALLERGIES_INTOLERANCES), setOf(TEST_MEDICAL_DATA_SOURCE))

        val scenario = launchMedicalAllDataFragment()
        scenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager.findFragmentByTag("")
            (fragment as AllDataFragment).triggerDeletionState(DELETE)
        }

        advanceUntilIdle()

        assertCheckboxShown("Select all")
        onView(withText("Select all")).perform(click())

        scenario.recreate()

        onView(withText("Select all")).perform(scrollTo())
        scenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager.findFragmentByTag("") as AllDataFragment
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
        assertCheckboxShown("Allergies")
        assertCheckboxShown("Vaccines")
        onView(
                withText(
                    "This includes all the health records synced to and added to Health\u00A0Connect. This might not be your full medical record and does not include a medical description of your health records."
                )
            )
            .check(doesNotExist())
    }

    private fun assertCheckboxShown(title: String, tag: String = "checkbox") {
        onView(withId(androidx.preference.R.id.recycler_view))
            .check(
                matches(
                    allOf(hasDescendant(withText(title)), hasDescendant(withTagValue(`is`(tag))))
                )
            )
    }

    private fun assertCheckboxNotShown(title: String, tag: String = "checkbox") {
        onView(withId(androidx.preference.R.id.recycler_view))
            .check(
                matches(
                    allOf(
                        hasDescendant(withText(title)),
                        not(hasDescendant(withTagValue(`is`(tag)))),
                    )
                )
            )
    }

    private fun mockData(permissionTypesList: List<FitnessPermissionType>) {
        val recordTypeInfoMap =
            permissionTypesList.associate { fitnessPermissionType ->
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

        doAnswer(prepareAnswer(recordTypeInfoMap))
            .`when`(manager)
            .queryAllRecordTypesInfo(any(), any())
    }

    private fun mockData(
        permissionTypesList: List<MedicalPermissionType>,
        medicalDataSources: Set<MedicalDataSource>,
    ) {
        val medicalResourceTypeResources =
            permissionTypesList.map {
                MedicalResourceTypeInfo(toMedicalResourceType(it), medicalDataSources)
            }

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

    private fun launchMedicalAllDataFragment(): ActivityScenario<TestActivity> =
        launchFragment<AllDataFragment>(bundleOf(IS_BROWSE_MEDICAL_DATA_SCREEN to true))
}
