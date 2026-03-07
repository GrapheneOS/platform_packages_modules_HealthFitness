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
import android.health.connect.HealthDataCategory
import android.health.connect.HealthPermissionCategory
import android.health.connect.MedicalResourceTypeInfo
import android.health.connect.ReadRecordsResponse
import android.health.connect.RecordTypeInfoResponse
import android.health.connect.datatypes.MedicalDataSource
import android.health.connect.datatypes.Record
import android.health.connect.datatypes.SymptomRecord
import android.os.Bundle
import android.platform.test.annotations.RequiresFlagsDisabled
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.preference.PreferenceCategory
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onIdle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
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
import com.android.healthconnect.controller.data.alldata.api.GetFitnessPermissionTypesWithDataUseCase
import com.android.healthconnect.controller.data.alldata.api.GetMedicalPermissionTypesWithDataUseCase
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType.BASAL_BODY_TEMPERATURE
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType.DISTANCE
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType.HEART_RATE
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType.HYDRATION
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType.MENSTRUATION
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType.STEPS
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
import com.android.healthconnect.controller.tests.devices.api.FakeGetCurrentDeviceIdUseCase
import com.android.healthconnect.controller.tests.utils.FakeUseCaseRule
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.TEST_MEDICAL_DATA_SOURCE
import com.android.healthconnect.controller.tests.utils.checkTextIsDisplayed
import com.android.healthconnect.controller.tests.utils.doReturnResult
import com.android.healthconnect.controller.tests.utils.getDataOrigin
import com.android.healthconnect.controller.tests.utils.launchFragment
import com.android.healthconnect.controller.tests.utils.scrollToText
import com.android.healthconnect.controller.tests.utils.scrollToTextAndClick
import com.android.healthconnect.controller.tests.utils.scrollToTopOfPreferenceScreen
import com.android.healthconnect.controller.tests.utils.setLocale
import com.android.healthconnect.controller.utils.logging.AllDataElement
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.PageName
import com.android.healthfitness.flags.Flags
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@UninstallModules(HealthManagerModule::class)
@RunWith(AndroidJUnit4::class)
class AllDataFragmentIntegrationTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)
    @get:Rule val checkFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()
    @get:Rule val fakeUseCaseRule = FakeUseCaseRule()

    @BindValue val manager: HealthConnectManager = mock()
    private val fakeGetCurrentDeviceIdUseCase =
        fakeUseCaseRule.watch(FakeGetCurrentDeviceIdUseCase())

    private val getFitnessPermissionTypesWithDataUseCase =
        GetFitnessPermissionTypesWithDataUseCase(
            manager,
            fakeGetCurrentDeviceIdUseCase,
            Dispatchers.Main,
        )
    private val getMedicalPermissionTypesWithDataUseCase =
        GetMedicalPermissionTypesWithDataUseCase(manager, Dispatchers.Main)

    @BindValue
    val allDataViewModel: AllDataViewModel =
        AllDataViewModel(
            getFitnessPermissionTypesWithDataUseCase,
            getMedicalPermissionTypesWithDataUseCase,
        )
    @BindValue val healthConnectLogger: HealthConnectLogger = mock()
    private lateinit var navHostController: TestNavHostController
    private lateinit var context: Context

    @Before
    fun setup() {
        hiltRule.inject()
        context = InstrumentationRegistry.getInstrumentation().context
        navHostController = TestNavHostController(context)
        context.setLocale(Locale.US)

        manager.stub {
            on { readRecords<Record>(any(), any(), any()) } doReturnResult
                Result.success(ReadRecordsResponse<Record>(emptyList(), -1))
        }
        fakeGetCurrentDeviceIdUseCase.updateDeviceId("test_device_id")

        mockData(listOf())
        mockData(listOf(), setOf())
    }

    @After
    fun tearDown() {
        reset(healthConnectLogger)
        // Wait for all threads to complete so the test activity is not called back after teardown
        onIdle()
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_NEW_HOME_SCREEN)
    fun populatedFitnessDataTypesDisplayed_impressionsLogged() {
        mockData(listOf(STEPS, HEART_RATE, BASAL_BODY_TEMPERATURE))

        launchFragment<AllDataFragment>().use {
            checkTextIsDisplayed("Steps")
            checkTextIsDisplayed("Heart rate")
            checkTextIsDisplayed("Basal body temperature")
            onView(withText("No data")).check(doesNotExist())
            verify(healthConnectLogger, atLeast(1)).setPageId(PageName.ALL_DATA_PAGE)
            verify(healthConnectLogger).logPageImpression()
            verify(healthConnectLogger, atLeast(3))
                .logImpression(AllDataElement.PERMISSION_TYPE_BUTTON_NO_CHECKBOX)
        }
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_NEW_HOME_SCREEN)
    fun whenCombinedData_populatedFitnessDataTypesDisplayed_impressionsLogged() {
        mockData(listOf(STEPS, HEART_RATE, BASAL_BODY_TEMPERATURE))

        launchFragment<AllDataFragment>().use {
            checkTextIsDisplayed("Steps")
            checkTextIsDisplayed("Heart rate")
            checkTextIsDisplayed("Basal body temperature")
            onView(withText("No data")).check(doesNotExist())
            verify(healthConnectLogger, atLeast(1)).setPageId(PageName.COMBINED_ALL_DATA_PAGE)
            verify(healthConnectLogger).logPageImpression()
            verify(healthConnectLogger, atLeast(3))
                .logImpression(AllDataElement.PERMISSION_TYPE_BUTTON_NO_CHECKBOX)
        }
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_NEW_HOME_SCREEN)
    fun populatedCombinedDataTypesDisplayed_impressionsLogged() {
        mockData(listOf(STEPS, HEART_RATE, HYDRATION))
        mockData(listOf(VACCINES, ALLERGIES_INTOLERANCES), setOf(TEST_MEDICAL_DATA_SOURCE))

        launchFragment<AllDataFragment>().use {
            checkTextIsDisplayed("Steps")
            checkTextIsDisplayed("Heart rate")
            checkTextIsDisplayed("Hydration")
            checkTextIsDisplayed(
                "This includes all the medical records synced to and added " +
                    "to Health\u00A0Connect. This might not be your full medical record and does not include a medical description of your medical records."
            )
            checkTextIsDisplayed("Allergies")
            checkTextIsDisplayed("Vaccines")
            verify(healthConnectLogger, atLeast(1)).setPageId(PageName.COMBINED_ALL_DATA_PAGE)
            verify(healthConnectLogger).logPageImpression()
            verify(healthConnectLogger, atLeast(5))
                .logImpression(AllDataElement.PERMISSION_TYPE_BUTTON_NO_CHECKBOX)
            verify(healthConnectLogger).logImpression(AllDataElement.MEDICAL_RECORDS_HEADER)
            verify(healthConnectLogger).logImpression(AllDataElement.MEDICAL_RECORDS_HEADER_LINK)
        }
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_NEW_HOME_SCREEN)
    fun populatedMedicalData_pageImpressionLogged() {
        mockData(listOf(VACCINES, ALLERGIES_INTOLERANCES), setOf(TEST_MEDICAL_DATA_SOURCE))

        launchMedicalAllDataFragment().use {
            verify(healthConnectLogger, atLeast(1)).setPageId(PageName.ALL_MEDICAL_DATA_PAGE)
            verify(healthConnectLogger).logPageImpression()
        }
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_NEW_HOME_SCREEN)
    fun populatedCombinedDataTypesDisplayed_onlyMedicalAvailable_impressionsLogged() {
        mockData(listOf(VACCINES, ALLERGIES_INTOLERANCES), setOf(TEST_MEDICAL_DATA_SOURCE))

        launchFragment<AllDataFragment>().use {
            checkTextIsDisplayed("Medical records")
            checkTextIsDisplayed(
                "This includes all the medical records synced to and added " +
                    "to Health\u00A0Connect. This might not be your full medical record and does not include a medical description of your medical records."
            )
            checkTextIsDisplayed("Allergies")
            checkTextIsDisplayed("Vaccines")

            verify(healthConnectLogger, atLeast(1)).setPageId(PageName.COMBINED_ALL_DATA_PAGE)
            verify(healthConnectLogger).logPageImpression()
            verify(healthConnectLogger, atLeast(2))
                .logImpression(AllDataElement.PERMISSION_TYPE_BUTTON_NO_CHECKBOX)
            verify(healthConnectLogger).logImpression(AllDataElement.MEDICAL_RECORDS_HEADER)
            verify(healthConnectLogger).logImpression(AllDataElement.MEDICAL_RECORDS_HEADER_LINK)
        }
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_NEW_HOME_SCREEN)
    fun medicalDataPresent_populatedDataTypesDisplayed() {
        mockData(listOf(VACCINES, ALLERGIES_INTOLERANCES), setOf(TEST_MEDICAL_DATA_SOURCE))

        launchMedicalAllDataFragment().use {
            checkTextIsDisplayed("Allergies")
            checkTextIsDisplayed("Vaccines")
            onView(withText("Distance")).check(doesNotExist())
            onView(withText("No data")).check(doesNotExist())
            onView(withText("Select all")).check(doesNotExist())
        }
    }

    @Test
    fun whenNoData_noDataMessageDisplayed() {
        mockData(emptyList())

        launchFragment<AllDataFragment>().use {
            onIdle()
            onView(withText("No data")).check(matches(isDisplayed()))
            onView(withText("Data from apps with access to Health\u00A0Connect will show here"))
                .check(matches(isDisplayed()))
            onView(
                    withText(
                        "This includes all the medical records synced to and added to Health\u00A0Connect. This might not be your full medical record and does not include a medical description of your medical records."
                    )
                )
                .check(doesNotExist())
        }
    }

    @Test
    fun whenFitnessDataTypesDisplayed_topIntroNotShown() {
        mockData(listOf(STEPS, HEART_RATE, BASAL_BODY_TEMPERATURE))

        launchFragment<AllDataFragment>().use {
            onView(
                    withText(
                        "This includes all the medical records synced to and added to Health\u00A0Connect. This might not be your full medical record and does not include a medical description of your medical records."
                    )
                )
                .check(doesNotExist())
        }
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_NEW_HOME_SCREEN)
    fun whenOnlyMedicalDataTypesDisplayed_topIntroShown() {
        mockData(listOf(VACCINES, ALLERGIES_INTOLERANCES), setOf(TEST_MEDICAL_DATA_SOURCE))

        launchMedicalAllDataFragment().use {
            onView(withText("Medical records")).check(doesNotExist())
            checkTextIsDisplayed(
                "This includes all the medical records synced to and added to Health\u00A0Connect. This might not be your full medical record and does not include a medical description of your medical records."
            )
            checkTextIsDisplayed("About medical records")
        }
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_NEW_HOME_SCREEN)
    fun whenCombinedData_andOnlyMedicalDataTypesDisplayed_topIntroShown() {
        mockData(listOf(VACCINES, ALLERGIES_INTOLERANCES), setOf(TEST_MEDICAL_DATA_SOURCE))

        launchFragment<AllDataFragment>().use {
            checkTextIsDisplayed("Medical records")
            checkTextIsDisplayed(
                "This includes all the medical records synced to and added to Health\u00A0Connect. " +
                    "This might not be your full medical record and does not include a medical " +
                    "description of your medical records."
            )
            checkTextIsDisplayed("About medical records")
        }
    }

    @Test
    fun whenFitnessShown_navigatesToAllEntries() {
        mockData(listOf(STEPS))

        launchFragment<AllDataFragment> {
                navHostController.setGraph(R.navigation.data_nav_graph_new_ia)
                Navigation.setViewNavController(this.requireView(), navHostController)
            }
            .use {
                checkTextIsDisplayed("Steps")
                scrollToTextAndClick("Steps")
                verify(healthConnectLogger)
                    .logInteraction(AllDataElement.PERMISSION_TYPE_BUTTON_NO_CHECKBOX)
                assertThat(navHostController.currentDestination?.id)
                    .isEqualTo(R.id.entriesAndAccessFragment)
            }
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_NEW_HOME_SCREEN)
    fun whenMedicalShown_navigatesToMedicalAllEntries() {
        mockData(listOf(VACCINES), setOf(TEST_MEDICAL_DATA_SOURCE))

        launchFragment<AllDataFragment>(
                Bundle().apply { putBoolean(IS_BROWSE_MEDICAL_DATA_SCREEN, true) }
            ) {
                navHostController.setGraph(R.navigation.medical_data_nav_graph)
                Navigation.setViewNavController(this.requireView(), navHostController)
            }
            .use {
                checkTextIsDisplayed("Vaccines")
                scrollToTextAndClick("Vaccines")
                verify(healthConnectLogger)
                    .logInteraction(AllDataElement.PERMISSION_TYPE_BUTTON_NO_CHECKBOX)
                assertThat(navHostController.currentDestination?.id)
                    .isEqualTo(R.id.entriesAndAccessFragment)
            }
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_NEW_HOME_SCREEN)
    fun whenCombinedData_onlyMedicalShown_navigatesToMedicalAllEntries() {
        mockData(listOf(VACCINES), setOf(TEST_MEDICAL_DATA_SOURCE))

        launchFragment<AllDataFragment> {
                navHostController.setGraph(R.navigation.data_nav_graph_new_ia)
                Navigation.setViewNavController(this.requireView(), navHostController)
            }
            .use {
                checkTextIsDisplayed("Vaccines")
                scrollToTextAndClick("Vaccines")
                verify(healthConnectLogger)
                    .logInteraction(AllDataElement.PERMISSION_TYPE_BUTTON_NO_CHECKBOX)
                assertThat(navHostController.currentDestination?.id)
                    .isEqualTo(R.id.entriesAndAccessFragment)
            }
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_SYMPTOMS, Flags.FLAG_SYMPTOMS_DB)
    fun whenSymptomsShown_navigatesToAllSymptoms() {
        val recordTypeInfoMap: Map<Class<out Record>, RecordTypeInfoResponse> =
            mapOf(
                SymptomRecord::class.java to
                    RecordTypeInfoResponse(
                        setOf(HealthPermissionCategory.SYMPTOM_SNORE), // Placeholder
                        HealthDataCategory.SYMPTOMS,
                        listOf(getDataOrigin(TEST_APP_PACKAGE_NAME)),
                    )
            )
        manager.stub {
            on { queryAllRecordTypesInfo(any(), any()) } doReturnResult
                Result.success(recordTypeInfoMap)
        }

        launchFragment<AllDataFragment> {
                navHostController.setGraph(R.navigation.data_nav_graph_new_ia)
                Navigation.setViewNavController(this.requireView(), navHostController)
            }
            .use {
                checkTextIsDisplayed("All symptoms")
                scrollToTextAndClick("All symptoms")
                verify(healthConnectLogger)
                    .logInteraction(AllDataElement.PERMISSION_TYPE_BUTTON_NO_CHECKBOX)
                assertThat(navHostController.currentDestination?.id)
                    .isEqualTo(R.id.entriesAndAccessFragment)
                val args = navHostController.backStack.last().arguments
                assertThat(args?.getString("permission_type_name_key"))
                    .isEqualTo(FitnessPermissionType.SYMPTOM_ABDOMINAL_PAIN.name)
            }
    }

    @Test
    fun triggerDeletionState_showsCheckboxes() {
        mockData(listOf(DISTANCE, MENSTRUATION))

        launchFragment<AllDataFragment>().use { scenario ->
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
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_NEW_HOME_SCREEN)
    fun triggerDeletionState_medicalData_showsCheckboxes() {
        mockData(listOf(VACCINES, ALLERGIES_INTOLERANCES), setOf(TEST_MEDICAL_DATA_SOURCE))
        launchMedicalAllDataFragment().use { scenario ->
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
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_NEW_HOME_SCREEN)
    fun triggerDeletionState_combinedData_onlyMedicalShown_showsCheckboxes() {
        mockData(listOf(VACCINES, ALLERGIES_INTOLERANCES), setOf(TEST_MEDICAL_DATA_SOURCE))
        launchFragment<AllDataFragment>().use { scenario ->
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
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_NEW_HOME_SCREEN)
    fun triggerDeletionState_combinedData_showsCheckboxes() {
        mockData(listOf(DISTANCE, MENSTRUATION))
        mockData(listOf(VACCINES, ALLERGIES_INTOLERANCES), setOf(TEST_MEDICAL_DATA_SOURCE))
        launchFragment<AllDataFragment>().use { scenario ->
            assertCheckboxNotShown("Distance")
            assertCheckboxNotShown("Menstruation")
            assertCheckboxNotShown("Allergies")
            assertCheckboxNotShown("Vaccines")

            scenario.onActivity { activity ->
                val fragment = activity.supportFragmentManager.findFragmentByTag("")
                (fragment as AllDataFragment).triggerDeletionState(DELETE)
            }

            assertCheckboxShown("Distance")
            assertCheckboxShown("Menstruation")
            assertCheckboxShown("Allergies")
            assertCheckboxShown("Vaccines")
            verify(healthConnectLogger).logImpression(AllDataElement.SELECT_ALL_BUTTON)
            verify(healthConnectLogger, atLeast(4))
                .logImpression(AllDataElement.PERMISSION_TYPE_BUTTON_WITH_CHECKBOX)
        }
    }

    @Test
    fun inDeletionState_checkedItemsAddedToDeleteSet() {
        mockData(listOf(DISTANCE, HEART_RATE))

        launchFragment<AllDataFragment>().use { scenario ->
            scenario.onActivity { activity ->
                val fragment = activity.supportFragmentManager.findFragmentByTag("")
                (fragment as AllDataFragment).triggerDeletionState(DELETE)
            }

            scrollToTextAndClick("Distance")
            onIdle()
            assertThat(allDataViewModel.setOfPermissionTypesToBeDeleted.value)
                .containsExactlyElementsIn(setOf(DISTANCE))
            verify(healthConnectLogger)
                .logInteraction(AllDataElement.PERMISSION_TYPE_BUTTON_WITH_CHECKBOX)
            scrollToTextAndClick("Distance")
            assertThat(allDataViewModel.setOfPermissionTypesToBeDeleted.value).isEmpty()
        }
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_NEW_HOME_SCREEN)
    fun inDeletionState_medicalData_checkedItemsAddedToDeleteSet() {
        mockData(listOf(VACCINES, ALLERGIES_INTOLERANCES), setOf(TEST_MEDICAL_DATA_SOURCE))

        launchMedicalAllDataFragment().use { scenario ->
            scenario.onActivity { activity ->
                val fragment = activity.supportFragmentManager.findFragmentByTag("")
                (fragment as AllDataFragment).triggerDeletionState(DELETE)
            }

            scrollToTextAndClick("Vaccines")
            onIdle()
            assertThat(allDataViewModel.setOfPermissionTypesToBeDeleted.value)
                .containsExactlyElementsIn(setOf(VACCINES))
            verify(healthConnectLogger)
                .logInteraction(AllDataElement.PERMISSION_TYPE_BUTTON_WITH_CHECKBOX)
            scrollToTextAndClick("Vaccines")
            assertThat(allDataViewModel.setOfPermissionTypesToBeDeleted.value).isEmpty()
        }
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_NEW_HOME_SCREEN)
    fun inDeletionState_combinedData_onlyMedicalShown_checkedItemsAddedToDeleteSet() = runTest {
        mockData(listOf(VACCINES, ALLERGIES_INTOLERANCES), setOf(TEST_MEDICAL_DATA_SOURCE))

        launchFragment<AllDataFragment>().use { scenario ->
            scenario.onActivity { activity ->
                val fragment = activity.supportFragmentManager.findFragmentByTag("")
                (fragment as AllDataFragment).triggerDeletionState(DELETE)
            }

            scrollToTextAndClick("Allergies")
            scrollToTextAndClick("Vaccines")
            onIdle()
            assertThat(allDataViewModel.setOfPermissionTypesToBeDeleted.value)
                .containsExactlyElementsIn(setOf(ALLERGIES_INTOLERANCES, VACCINES))
            verify(healthConnectLogger, atLeast(2))
                .logInteraction(AllDataElement.PERMISSION_TYPE_BUTTON_WITH_CHECKBOX)
            scrollToTextAndClick("Allergies")
            scrollToTextAndClick("Vaccines")
            onIdle()
            assertThat(allDataViewModel.setOfPermissionTypesToBeDeleted.value).isEmpty()
        }
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_NEW_HOME_SCREEN)
    fun inDeletionState_combinedData_checkedItemsAddedToDeleteSet() {
        mockData(listOf(DISTANCE, HEART_RATE))
        mockData(listOf(VACCINES, ALLERGIES_INTOLERANCES), setOf(TEST_MEDICAL_DATA_SOURCE))

        launchFragment<AllDataFragment>().use { scenario ->
            scenario.onActivity { activity ->
                val fragment = activity.supportFragmentManager.findFragmentByTag("")
                (fragment as AllDataFragment).triggerDeletionState(DELETE)
            }

            scrollToTextAndClick("Distance")
            scrollToTextAndClick("Vaccines")
            onIdle()
            assertThat(allDataViewModel.setOfPermissionTypesToBeDeleted.value)
                .containsExactlyElementsIn(setOf(DISTANCE, VACCINES))
            verify(healthConnectLogger, atLeast(2))
                .logInteraction(AllDataElement.PERMISSION_TYPE_BUTTON_WITH_CHECKBOX)
            scrollToTextAndClick("Distance")
            scrollToTextAndClick("Vaccines")
            assertThat(allDataViewModel.setOfPermissionTypesToBeDeleted.value).isEmpty()
        }
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_SYMPTOMS, Flags.FLAG_SYMPTOMS_DB)
    fun inDeletionState_clickAllSymptoms_togglesRepresentativeTypeInSet() = runTest {
        val recordTypeInfoMap: Map<Class<out Record>, RecordTypeInfoResponse> =
            mapOf(
                SymptomRecord::class.java to
                    RecordTypeInfoResponse(
                        setOf(HealthPermissionCategory.SYMPTOM_SNORE), // Placeholder
                        HealthDataCategory.SYMPTOMS,
                        listOf(getDataOrigin(TEST_APP_PACKAGE_NAME)),
                    )
            )
        manager.stub {
            on { queryAllRecordTypesInfo(any(), any()) } doReturnResult
                Result.success(recordTypeInfoMap)
        }

        launchFragment<AllDataFragment>().use { scenario ->
            scenario.onActivity { activity ->
                val fragment =
                    activity.supportFragmentManager.fragments.first { it is AllDataFragment }
                        as AllDataFragment
                fragment.triggerDeletionState(DELETE)
            }
            onIdle()

            // Select "All symptoms"
            scrollToTextAndClick("All symptoms")
            onIdle()
            assertThat(allDataViewModel.setOfPermissionTypesToBeDeleted.value)
                .contains(FitnessPermissionType.SYMPTOM_ABDOMINAL_PAIN)

            // Deselect "All symptoms"
            scrollToTextAndClick("All symptoms")
            onIdle()
            assertThat(allDataViewModel.setOfPermissionTypesToBeDeleted.value)
                .doesNotContain(FitnessPermissionType.SYMPTOM_ABDOMINAL_PAIN)
        }
    }

    @Test
    fun triggerDeletionState_checkboxesRemainOnOrientationChange() = runTest {
        mockData(listOf(DISTANCE, HEART_RATE))

        launchFragment<AllDataFragment>().use { scenario ->
            scenario.onActivity { activity ->
                val fragment = activity.supportFragmentManager.findFragmentByTag("")
                (fragment as AllDataFragment).triggerDeletionState(DELETE)
            }

            onIdle()

            assertCheckboxShown("Distance")
            assertCheckboxShown("Heart rate")
            scrollToTextAndClick("Distance")

            scenario.recreate()
            scenario.onActivity { activity ->
                val fragment =
                    activity.supportFragmentManager.findFragmentByTag("") as AllDataFragment
                val fitnessCategoryPreference =
                    fragment.preferenceScreen.findPreference("key_permission_type")
                        as PreferenceCategory?
                fitnessCategoryPreference?.children?.forEach { preference ->
                    if (preference is PreferenceCategory) {
                        preference.children.forEach { permissionTypePreference ->
                            if (permissionTypePreference is DeletionPermissionTypesPreference) {
                                if (
                                    permissionTypePreference.getHealthPermissionType() == DISTANCE
                                ) {
                                    assertThat(permissionTypePreference.getIsChecked()).isTrue()
                                } else if (
                                    permissionTypePreference.getHealthPermissionType() == HEART_RATE
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
    }

    @Test
    fun triggerDeletionState_displaysSelectAllButton() {
        mockData(listOf(DISTANCE, MENSTRUATION))

        launchFragment<AllDataFragment>().use { scenario ->
            // trigger deletion state
            scenario.onActivity { activity ->
                val fragment = activity.supportFragmentManager.findFragmentByTag("")
                (fragment as AllDataFragment).triggerDeletionState(DELETE)
            }
            scrollToTopOfPreferenceScreen()
            assertCheckboxShown("Select all")
            onView(
                    withText(
                        "This includes all the medical records synced to and added to Health\u00A0Connect. This might not be your full medical record and does not include a medical description of your medical records."
                    )
                )
                .check(doesNotExist())
        }
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_NEW_HOME_SCREEN)
    fun triggerDeletionState_medicalData_displaysSelectAllButton() {
        mockData(listOf(VACCINES, ALLERGIES_INTOLERANCES), setOf(TEST_MEDICAL_DATA_SOURCE))
        launchMedicalAllDataFragment().use { scenario ->
            scenario.onActivity { activity ->
                val fragment = activity.supportFragmentManager.findFragmentByTag("")
                (fragment as AllDataFragment).triggerDeletionState(DELETE)
            }
            scrollToTopOfPreferenceScreen()
            assertCheckboxShown("Select all")
        }
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_NEW_HOME_SCREEN)
    fun triggerDeletionState_combinedData_onlyMedicalShown_displaysSelectAllButton() {
        mockData(listOf(VACCINES, ALLERGIES_INTOLERANCES), setOf(TEST_MEDICAL_DATA_SOURCE))
        launchFragment<AllDataFragment>().use { scenario ->
            scenario.onActivity { activity ->
                val fragment = activity.supportFragmentManager.findFragmentByTag("")
                (fragment as AllDataFragment).triggerDeletionState(DELETE)
            }
            scrollToTopOfPreferenceScreen()
            assertCheckboxShown("Select all")
        }
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_NEW_HOME_SCREEN)
    fun triggerDeletionState_combinedData_displaysSelectAllButton() = runTest {
        mockData(listOf(DISTANCE, MENSTRUATION))
        mockData(listOf(VACCINES, ALLERGIES_INTOLERANCES), setOf(TEST_MEDICAL_DATA_SOURCE))
        launchFragment<AllDataFragment>().use { scenario ->
            scenario.onActivity { activity ->
                val fragment = activity.supportFragmentManager.findFragmentByTag("")
                (fragment as AllDataFragment).triggerDeletionState(DELETE)
            }
            scrollToTopOfPreferenceScreen()
            assertCheckboxShown("Select all")
        }
    }

    @Test
    fun inDeletionState_onSelectAllChecked_allPermissionTypesChecked() = runTest {
        mockData(listOf(DISTANCE, MENSTRUATION))

        launchFragment<AllDataFragment>().use { scenario ->
            scenario.onActivity { activity ->
                val fragment = activity.supportFragmentManager.findFragmentByTag("")
                (fragment as AllDataFragment).triggerDeletionState(DELETE)
            }

            onIdle()
            scrollToTopOfPreferenceScreen()
            assertCheckboxShown("Select all")
            onView(withText("Select all")).perform(click())
            assertThat(allDataViewModel.setOfPermissionTypesToBeDeleted.value)
                .containsExactlyElementsIn(setOf(DISTANCE, MENSTRUATION))
            verify(healthConnectLogger).logInteraction(AllDataElement.SELECT_ALL_BUTTON)
        }
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_NEW_HOME_SCREEN)
    fun inDeletionState_medicalData_onSelectAllChecked_allPermissionTypesChecked() = runTest {
        mockData(listOf(VACCINES, ALLERGIES_INTOLERANCES), setOf(TEST_MEDICAL_DATA_SOURCE))

        launchMedicalAllDataFragment().use { scenario ->
            scenario.onActivity { activity ->
                val fragment = activity.supportFragmentManager.findFragmentByTag("")
                (fragment as AllDataFragment).triggerDeletionState(DELETE)
            }

            onIdle()
            scrollToTopOfPreferenceScreen()
            assertCheckboxShown("Select all")
            onView(withText("Select all")).perform(click())
            assertThat(allDataViewModel.setOfPermissionTypesToBeDeleted.value)
                .containsExactlyElementsIn(setOf(VACCINES, ALLERGIES_INTOLERANCES))
            verify(healthConnectLogger).logInteraction(AllDataElement.SELECT_ALL_BUTTON)
        }
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_NEW_HOME_SCREEN)
    fun inDeletionState_combinedData_onlyMedicalShown_onSelectAllChecked_allPermissionTypesChecked() =
        runTest {
            mockData(listOf(VACCINES, ALLERGIES_INTOLERANCES), setOf(TEST_MEDICAL_DATA_SOURCE))

            launchFragment<AllDataFragment>().use { scenario ->
                scenario.onActivity { activity ->
                    val fragment = activity.supportFragmentManager.findFragmentByTag("")
                    (fragment as AllDataFragment).triggerDeletionState(DELETE)
                }

                onIdle()
                scrollToTopOfPreferenceScreen()
                assertCheckboxShown("Select all")
                onView(withText("Select all")).perform(click())
                assertThat(allDataViewModel.setOfPermissionTypesToBeDeleted.value)
                    .containsExactlyElementsIn(setOf(VACCINES, ALLERGIES_INTOLERANCES))
                verify(healthConnectLogger).logInteraction(AllDataElement.SELECT_ALL_BUTTON)
            }
        }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_NEW_HOME_SCREEN)
    fun inDeletionState_combinedData_onSelectAllChecked_allPermissionTypesChecked() = runTest {
        mockData(listOf(DISTANCE, MENSTRUATION))
        mockData(listOf(VACCINES, ALLERGIES_INTOLERANCES), setOf(TEST_MEDICAL_DATA_SOURCE))

        launchFragment<AllDataFragment>().use { scenario ->
            scenario.onActivity { activity ->
                val fragment = activity.supportFragmentManager.findFragmentByTag("")
                (fragment as AllDataFragment).triggerDeletionState(DELETE)
            }

            onIdle()
            scrollToTopOfPreferenceScreen()
            assertCheckboxShown("Select all")
            onView(withText("Select all")).perform(click())
            assertThat(allDataViewModel.setOfPermissionTypesToBeDeleted.value)
                .containsExactlyElementsIn(
                    setOf(VACCINES, ALLERGIES_INTOLERANCES, DISTANCE, MENSTRUATION)
                )
            verify(healthConnectLogger).logInteraction(AllDataElement.SELECT_ALL_BUTTON)
        }
    }

    @Test
    fun inDeletionState_onSelectAllUnchecked_allPermissionTypesUnChecked() = runTest {
        mockData(listOf(DISTANCE, MENSTRUATION))

        launchFragment<AllDataFragment>().use { scenario ->
            scenario.onActivity { activity ->
                val fragment = activity.supportFragmentManager.findFragmentByTag("")
                (fragment as AllDataFragment).triggerDeletionState(DELETE)
            }

            onIdle()
            scrollToTopOfPreferenceScreen()
            assertCheckboxShown("Select all")
            onView(withText("Select all")).perform(click())
            assertThat(allDataViewModel.setOfPermissionTypesToBeDeleted.value)
                .containsExactlyElementsIn(setOf(DISTANCE, MENSTRUATION))
            onView(withText("Select all")).perform(click())
            assertThat(allDataViewModel.setOfPermissionTypesToBeDeleted.value).isEmpty()
        }
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_NEW_HOME_SCREEN)
    fun inDeletionState_medicalData_onSelectAllUnchecked_allPermissionTypesUnChecked() = runTest {
        mockData(listOf(VACCINES, ALLERGIES_INTOLERANCES), setOf(TEST_MEDICAL_DATA_SOURCE))

        launchMedicalAllDataFragment().use { scenario ->
            scenario.onActivity { activity ->
                val fragment = activity.supportFragmentManager.findFragmentByTag("")
                (fragment as AllDataFragment).triggerDeletionState(DELETE)
            }

            onIdle()
            scrollToTopOfPreferenceScreen()
            assertCheckboxShown("Select all")
            onView(withText("Select all")).perform(click())
            assertThat(allDataViewModel.setOfPermissionTypesToBeDeleted.value)
                .containsExactlyElementsIn(setOf(VACCINES, ALLERGIES_INTOLERANCES))
            onView(withText("Select all")).perform(click())
            assertThat(allDataViewModel.setOfPermissionTypesToBeDeleted.value).isEmpty()
        }
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_NEW_HOME_SCREEN)
    fun inDeletionState_combinedData_onlyMedicalShown_onSelectAllUnchecked_allPermissionTypesUnChecked() =
        runTest {
            mockData(listOf(VACCINES, ALLERGIES_INTOLERANCES), setOf(TEST_MEDICAL_DATA_SOURCE))

            launchFragment<AllDataFragment>().use { scenario ->
                scenario.onActivity { activity ->
                    val fragment = activity.supportFragmentManager.findFragmentByTag("")
                    (fragment as AllDataFragment).triggerDeletionState(DELETE)
                }

                onIdle()
                scrollToTopOfPreferenceScreen()
                assertCheckboxShown("Select all")
                onView(withText("Select all")).perform(click())
                assertThat(allDataViewModel.setOfPermissionTypesToBeDeleted.value)
                    .containsExactlyElementsIn(setOf(VACCINES, ALLERGIES_INTOLERANCES))
                onView(withText("Select all")).perform(click())
                assertThat(allDataViewModel.setOfPermissionTypesToBeDeleted.value).isEmpty()
            }
        }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_NEW_HOME_SCREEN)
    fun inDeletionState_combinedData_onSelectAllUnchecked_allPermissionTypesUnChecked() = runTest {
        mockData(listOf(DISTANCE, MENSTRUATION))
        mockData(listOf(VACCINES, ALLERGIES_INTOLERANCES), setOf(TEST_MEDICAL_DATA_SOURCE))

        launchFragment<AllDataFragment>().use { scenario ->
            scenario.onActivity { activity ->
                val fragment = activity.supportFragmentManager.findFragmentByTag("")
                (fragment as AllDataFragment).triggerDeletionState(DELETE)
            }

            onIdle()
            scrollToTopOfPreferenceScreen()
            assertCheckboxShown("Select all")
            onView(withText("Select all")).perform(click())
            assertThat(allDataViewModel.setOfPermissionTypesToBeDeleted.value)
                .containsExactlyElementsIn(
                    setOf(VACCINES, ALLERGIES_INTOLERANCES, DISTANCE, MENSTRUATION)
                )
            onView(withText("Select all")).perform(click())
            assertThat(allDataViewModel.setOfPermissionTypesToBeDeleted.value).isEmpty()
        }
    }

    @Test
    fun inDeletionState_allPermissionTypesChecked_selectAllShouldBeChecked() = runTest {
        mockData(listOf(DISTANCE, MENSTRUATION))

        launchFragment<AllDataFragment>().use { scenario ->
            scenario.onActivity { activity ->
                val fragment = activity.supportFragmentManager.findFragmentByTag("")
                (fragment as AllDataFragment).triggerDeletionState(DELETE)
            }

            assertCheckboxShown("Distance")
            assertCheckboxShown("Menstruation")
            scrollToTextAndClick("Distance")
            scrollToTextAndClick("Menstruation")
            onIdle()
            scenario.onActivity { activity ->
                val fragment =
                    activity.supportFragmentManager.findFragmentByTag("") as AllDataFragment
                val selectAllCheckboxPreference =
                    fragment.preferenceScreen.findPreference("key_select_all")
                        as SelectAllCheckboxPreference?
                assertThat(selectAllCheckboxPreference?.getIsChecked()).isTrue()
            }
        }
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_NEW_HOME_SCREEN)
    fun inDeletionState_medicalData_allPermissionTypesChecked_selectAllShouldBeChecked() {
        mockData(listOf(VACCINES, ALLERGIES_INTOLERANCES), setOf(TEST_MEDICAL_DATA_SOURCE))

        launchMedicalAllDataFragment().use { scenario ->
            scenario.onActivity { activity ->
                val fragment = activity.supportFragmentManager.findFragmentByTag("")
                (fragment as AllDataFragment).triggerDeletionState(DELETE)
            }

            assertCheckboxShown("Allergies")
            assertCheckboxShown("Vaccines")
            scrollToTextAndClick("Allergies")
            scrollToTextAndClick("Vaccines")
            scenario.onActivity { activity ->
                val fragment =
                    activity.supportFragmentManager.findFragmentByTag("") as AllDataFragment
                val selectAllCheckboxPreference =
                    fragment.preferenceScreen.findPreference("key_select_all")
                        as SelectAllCheckboxPreference?
                assertThat(selectAllCheckboxPreference?.getIsChecked()).isTrue()
            }
        }
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_NEW_HOME_SCREEN)
    fun inDeletionState_combinedData_onlyMedicalShown_allPermissionTypesChecked_selectAllShouldBeChecked() =
        runTest {
            mockData(listOf(VACCINES, ALLERGIES_INTOLERANCES), setOf(TEST_MEDICAL_DATA_SOURCE))

            launchFragment<AllDataFragment>().use { scenario ->
                scenario.onActivity { activity ->
                    val fragment = activity.supportFragmentManager.findFragmentByTag("")
                    (fragment as AllDataFragment).triggerDeletionState(DELETE)
                }

                assertCheckboxShown("Allergies")
                assertCheckboxShown("Vaccines")
                scrollToTextAndClick("Allergies")
                scrollToTextAndClick("Vaccines")
                onIdle()
                scenario.onActivity { activity ->
                    val fragment =
                        activity.supportFragmentManager.findFragmentByTag("") as AllDataFragment
                    val selectAllCheckboxPreference =
                        fragment.preferenceScreen.findPreference("key_select_all")
                            as SelectAllCheckboxPreference?
                    assertThat(selectAllCheckboxPreference?.getIsChecked()).isTrue()
                }
            }
        }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_NEW_HOME_SCREEN)
    fun inDeletionState_combinedData_allPermissionTypesChecked_selectAllShouldBeChecked() =
        runTest {
            mockData(listOf(DISTANCE, MENSTRUATION))
            mockData(listOf(VACCINES, ALLERGIES_INTOLERANCES), setOf(TEST_MEDICAL_DATA_SOURCE))

            launchFragment<AllDataFragment>().use { scenario ->
                scenario.onActivity { activity ->
                    val fragment = activity.supportFragmentManager.findFragmentByTag("")
                    (fragment as AllDataFragment).triggerDeletionState(DELETE)
                }

                Thread.sleep(5000)

                assertCheckboxShown("Distance")
                assertCheckboxShown("Menstruation")
                assertCheckboxShown("Allergies")
                assertCheckboxShown("Vaccines")
                scrollToTextAndClick("Distance")
                scrollToTextAndClick("Menstruation")
                scrollToTextAndClick("Allergies")
                scrollToTextAndClick("Vaccines")
                onIdle()
                scenario.onActivity { activity ->
                    val fragment =
                        activity.supportFragmentManager.findFragmentByTag("") as AllDataFragment
                    val selectAllCheckboxPreference =
                        fragment.preferenceScreen.findPreference("key_select_all")
                            as SelectAllCheckboxPreference?
                    assertThat(selectAllCheckboxPreference?.getIsChecked()).isTrue()
                }
            }
        }

    @Test
    fun inDeletionState_selectAllChecked_oneUnchecked_selectAllUnchecked() = runTest {
        mockData(listOf(DISTANCE, MENSTRUATION))

        launchFragment<AllDataFragment>().use { scenario ->
            scenario.onActivity { activity ->
                val fragment = activity.supportFragmentManager.findFragmentByTag("")
                (fragment as AllDataFragment).triggerDeletionState(DELETE)
            }

            onIdle()
            scrollToTopOfPreferenceScreen()
            assertCheckboxShown("Select all")
            scrollToTextAndClick("Select all")
            scrollToTextAndClick("Distance")
            scenario.onActivity { activity ->
                val fragment =
                    activity.supportFragmentManager.findFragmentByTag("") as AllDataFragment
                val selectAllCheckboxPreference =
                    fragment.preferenceScreen.findPreference("key_select_all")
                        as SelectAllCheckboxPreference?
                assertThat(selectAllCheckboxPreference?.getIsChecked()).isFalse()
            }
        }
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_NEW_HOME_SCREEN)
    fun inDeletionState_medicalData_selectAllChecked_oneUnchecked_selectAllUnchecked() = runTest {
        mockData(listOf(VACCINES, ALLERGIES_INTOLERANCES), setOf(TEST_MEDICAL_DATA_SOURCE))

        launchMedicalAllDataFragment().use { scenario ->
            scenario.onActivity { activity ->
                val fragment = activity.supportFragmentManager.findFragmentByTag("")
                (fragment as AllDataFragment).triggerDeletionState(DELETE)
            }

            onIdle()
            scrollToTopOfPreferenceScreen()
            assertCheckboxShown("Select all")
            scrollToTextAndClick("Select all")
            scrollToTextAndClick("Allergies")
            scenario.onActivity { activity ->
                val fragment =
                    activity.supportFragmentManager.findFragmentByTag("") as AllDataFragment
                val selectAllCheckboxPreference =
                    fragment.preferenceScreen.findPreference("key_select_all")
                        as SelectAllCheckboxPreference?
                assertThat(selectAllCheckboxPreference?.getIsChecked()).isFalse()
            }
        }
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_NEW_HOME_SCREEN)
    fun inDeletionState_combinedData_onlyMedicalShown_selectAllChecked_oneUnchecked_selectAllUnchecked() =
        runTest {
            mockData(listOf(VACCINES, ALLERGIES_INTOLERANCES), setOf(TEST_MEDICAL_DATA_SOURCE))

            launchFragment<AllDataFragment>().use { scenario ->
                scenario.onActivity { activity ->
                    val fragment = activity.supportFragmentManager.findFragmentByTag("")
                    (fragment as AllDataFragment).triggerDeletionState(DELETE)
                }

                onIdle()
                scrollToTopOfPreferenceScreen()
                assertCheckboxShown("Select all")
                scrollToTextAndClick("Select all")
                scrollToTextAndClick("Allergies")
                scenario.onActivity { activity ->
                    val fragment =
                        activity.supportFragmentManager.findFragmentByTag("") as AllDataFragment
                    val selectAllCheckboxPreference =
                        fragment.preferenceScreen.findPreference("key_select_all")
                            as SelectAllCheckboxPreference?
                    assertThat(selectAllCheckboxPreference?.getIsChecked()).isFalse()
                }
            }
        }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_NEW_HOME_SCREEN)
    fun inDeletionState_combinedData_selectAllChecked_oneUnchecked_selectAllUnchecked() = runTest {
        mockData(listOf(DISTANCE, MENSTRUATION))
        mockData(listOf(VACCINES, ALLERGIES_INTOLERANCES), setOf(TEST_MEDICAL_DATA_SOURCE))

        launchFragment<AllDataFragment>().use { scenario ->
            scenario.onActivity { activity ->
                val fragment = activity.supportFragmentManager.findFragmentByTag("")
                (fragment as AllDataFragment).triggerDeletionState(DELETE)
            }

            onIdle()
            scrollToTopOfPreferenceScreen()
            assertCheckboxShown("Select all")
            scrollToTextAndClick("Select all")
            scrollToTextAndClick("Distance")
            scrollToTextAndClick("Allergies")
            scenario.onActivity { activity ->
                val fragment =
                    activity.supportFragmentManager.findFragmentByTag("") as AllDataFragment
                val selectAllCheckboxPreference =
                    fragment.preferenceScreen.findPreference("key_select_all")
                        as SelectAllCheckboxPreference?
                assertThat(selectAllCheckboxPreference?.getIsChecked()).isFalse()
            }
        }
    }

    @Test
    fun inDeletionState_selectAllChecked_checkboxesRemainOnOrientationChange() = runTest {
        mockData(listOf(DISTANCE, HEART_RATE))

        launchFragment<AllDataFragment>().use { scenario ->
            scenario.onActivity { activity ->
                val fragment = activity.supportFragmentManager.findFragmentByTag("")
                (fragment as AllDataFragment).triggerDeletionState(DELETE)
            }

            onIdle()
            scrollToTopOfPreferenceScreen()
            assertCheckboxShown("Select all")
            scrollToTextAndClick("Select all")

            scenario.recreate()

            scrollToText("Select all")
            scenario.onActivity { activity ->
                val fragment =
                    activity.supportFragmentManager.findFragmentByTag("") as AllDataFragment
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
                        "This includes all the medical records synced to and added to Health\u00A0Connect. This might not be your full medical record and does not include a medical description of your medical records."
                    )
                )
                .check(doesNotExist())
        }
    }

    @Test
    @RequiresFlagsDisabled(Flags.FLAG_NEW_HOME_SCREEN)
    fun inDeletionState_medicalData_checkboxesRemainOnOrientationChange() = runTest {
        mockData(listOf(VACCINES, ALLERGIES_INTOLERANCES), setOf(TEST_MEDICAL_DATA_SOURCE))

        launchMedicalAllDataFragment().use { scenario ->
            scenario.onActivity { activity ->
                val fragment = activity.supportFragmentManager.findFragmentByTag("")
                (fragment as AllDataFragment).triggerDeletionState(DELETE)
            }

            onIdle()
            scrollToTopOfPreferenceScreen()
            assertCheckboxShown("Select all")
            scrollToTextAndClick("Select all")

            scenario.recreate()

            scrollToText("Select all")
            scenario.onActivity { activity ->
                val fragment =
                    activity.supportFragmentManager.findFragmentByTag("") as AllDataFragment
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
                        "This includes all the medical records synced to and added to Health\u00A0Connect. This might not be your full medical record and does not include a medical description of your medical records."
                    )
                )
                .check(doesNotExist())
        }
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_NEW_HOME_SCREEN)
    fun inDeletionState_combinedDataOnlyMedicalShowing_checkboxesRemainOnOrientationChange() =
        runTest {
            mockData(listOf(VACCINES, ALLERGIES_INTOLERANCES), setOf(TEST_MEDICAL_DATA_SOURCE))

            launchFragment<AllDataFragment>().use { scenario ->
                scenario.onActivity { activity ->
                    val fragment = activity.supportFragmentManager.findFragmentByTag("")
                    (fragment as AllDataFragment).triggerDeletionState(DELETE)
                }

                onIdle()
                scrollToTopOfPreferenceScreen()
                assertCheckboxShown("Select all")
                scrollToTextAndClick("Select all")

                scenario.recreate()

                scrollToText("Select all")
                scenario.onActivity { activity ->
                    val fragment =
                        activity.supportFragmentManager.findFragmentByTag("") as AllDataFragment
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
            }
        }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_NEW_HOME_SCREEN)
    fun inDeletionState_combinedData_checkboxesRemainOnOrientationChange() = runTest {
        mockData(listOf(DISTANCE, MENSTRUATION))
        mockData(listOf(VACCINES, ALLERGIES_INTOLERANCES), setOf(TEST_MEDICAL_DATA_SOURCE))

        launchFragment<AllDataFragment>().use { scenario ->
            scenario.onActivity { activity ->
                val fragment = activity.supportFragmentManager.findFragmentByTag("")
                (fragment as AllDataFragment).triggerDeletionState(DELETE)
            }

            onIdle()
            scrollToTopOfPreferenceScreen()
            assertCheckboxShown("Select all")
            scrollToTextAndClick("Select all")

            scenario.recreate()

            scrollToText("Select all")
            scenario.onActivity { activity ->
                val fragment =
                    activity.supportFragmentManager.findFragmentByTag("") as AllDataFragment
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
            assertCheckboxShown("Menstruation")
            assertCheckboxShown("Allergies")
            assertCheckboxShown("Vaccines")
        }
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_SYMPTOMS, Flags.FLAG_SYMPTOMS_DB)
    fun givenSymptomData_displaysAllSymptomsEntry() {
        val recordTypeInfoMap: Map<Class<out Record>, RecordTypeInfoResponse> =
            mapOf(
                SymptomRecord::class.java to
                    RecordTypeInfoResponse(
                        setOf(HealthPermissionCategory.SYMPTOM_SNORE), // Placeholder
                        HealthDataCategory.SYMPTOMS,
                        listOf(getDataOrigin(TEST_APP_PACKAGE_NAME)),
                    )
            )
        manager.stub {
            on { queryAllRecordTypesInfo(any(), any()) } doReturnResult
                Result.success(recordTypeInfoMap)
        }

        launchFragment<AllDataFragment>().use {
            checkTextIsDisplayed("Symptoms") // Category title
            checkTextIsDisplayed("All symptoms") // The single entry

            // Individual symptoms should not be shown
            onView(withText("Cough")).check(doesNotExist())
            onView(withText("Fever")).check(doesNotExist())
        }
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_SYMPTOMS, Flags.FLAG_SYMPTOMS_DB)
    fun givenMultipleSymptomData_displaysOnlyOneAllSymptomsEntry() {
        val recordTypeInfoMap: Map<Class<out Record>, RecordTypeInfoResponse> =
            mapOf(
                SymptomRecord::class.java to
                    RecordTypeInfoResponse(
                        setOf(HealthPermissionCategory.SYMPTOM_SNORE), // Placeholder
                        HealthDataCategory.SYMPTOMS,
                        listOf(
                            getDataOrigin(TEST_APP_PACKAGE_NAME),
                            getDataOrigin("test.app.package.2"),
                        ),
                    )
            )
        manager.stub {
            on { queryAllRecordTypesInfo(any(), any()) } doReturnResult
                Result.success(recordTypeInfoMap)
        }

        launchFragment<AllDataFragment>().use {
            checkTextIsDisplayed("Symptoms") // Category title
            checkTextIsDisplayed("All symptoms") // The single entry

            // Individual symptoms should not be shown
            onView(withText("Cough")).check(doesNotExist())
            onView(withText("Fever")).check(doesNotExist())
            onView(withText("Snore")).check(doesNotExist())
        }
    }

    private fun assertCheckboxShown(title: String, tag: String = "checkbox") {
        scrollToText(title)
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
                        setOf(permissionCategory),
                        healthCategory,
                        listOf(getDataOrigin(TEST_APP_PACKAGE_NAME)),
                    )
            }

        manager.stub {
            on { queryAllRecordTypesInfo(any(), any()) } doReturnResult
                Result.success(recordTypeInfoMap)
        }
    }

    private fun mockData(
        permissionTypesList: List<MedicalPermissionType>,
        medicalDataSources: Set<MedicalDataSource>,
    ) {
        val medicalResourceTypeResources =
            permissionTypesList.map {
                MedicalResourceTypeInfo(toMedicalResourceType(it), medicalDataSources)
            }

        manager.stub {
            on { queryAllMedicalResourceTypeInfos(any(), any()) } doReturnResult
                Result.success(medicalResourceTypeResources)
        }
    }

    private fun launchMedicalAllDataFragment(): ActivityScenario<TestActivity> =
        launchFragment<AllDataFragment>(
            Bundle().apply { putBoolean(IS_BROWSE_MEDICAL_DATA_SCREEN, true) }
        )
}
