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
package com.android.healthconnect.controller.tests.data.access

import android.content.Context
import android.health.connect.HealthConnectManager
import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.data.access.AccessFragment
import com.android.healthconnect.controller.data.access.AccessViewModel
import com.android.healthconnect.controller.data.access.AccessViewModel.AccessScreenState
import com.android.healthconnect.controller.data.access.AccessViewModel.AccessScreenState.WithData
import com.android.healthconnect.controller.data.access.AppAccessMetadata
import com.android.healthconnect.controller.data.access.AppAccessState
import com.android.healthconnect.controller.data.appdata.AppDataFragment.Companion.PERMISSION_TYPE_NAME_KEY
import com.android.healthconnect.controller.permissions.data.FitnessPermissionType
import com.android.healthconnect.controller.permissions.data.MedicalPermissionType
import com.android.healthconnect.controller.service.HealthManagerModule
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.shared.app.AppPermissionsType
import com.android.healthconnect.controller.tests.utils.TEST_APP
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME
import com.android.healthconnect.controller.tests.utils.launchFragment
import com.android.healthconnect.controller.tests.utils.setLocale
import com.android.healthconnect.controller.utils.logging.DataAccessElement
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.PageName
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import java.util.Locale
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@HiltAndroidTest
@UninstallModules(HealthManagerModule::class)
@RunWith(AndroidJUnit4::class)
class AccessFragmentTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @BindValue val viewModel: AccessViewModel = mock()
    @BindValue val healthConnectManager: HealthConnectManager = mock()
    private lateinit var navHostController: TestNavHostController
    private lateinit var context: Context
    @BindValue val healthConnectLogger: HealthConnectLogger = mock()

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
    }

    @Test
    fun dataAccessFragment_noSections_noneDisplayed() {
        whenever(viewModel.appMetadataMap).then {
            MutableLiveData<AccessScreenState>(WithData(emptyMap()))
        }
        launchFragment<AccessFragment>(distanceBundle)

        onView(withText("Can read distance")).check(doesNotExist())
        onView(withText("Can write distance")).check(doesNotExist())
        onView(withText("Inactive apps")).check(doesNotExist())
        onView(
                withText(
                    "These apps can no longer read or write distance, but still have data stored in Health\u00A0Connect"
                )
            )
            .check(doesNotExist())
    }

    @Test
    fun dataAccessFragment_medicalPermissionNoSections_noneDisplayed() {
        whenever(viewModel.appMetadataMap).then {
            MutableLiveData<AccessScreenState>(WithData(emptyMap()))
        }
        launchFragment<AccessFragment>(allMedicalDataBundle)

        onView(withText("Can read distance")).check(doesNotExist())
        onView(withText("Can write distance")).check(doesNotExist())
        onView(withText("Inactive apps")).check(doesNotExist())
        onView(
                withText(
                    "These apps can no longer read or write distance, but still have data stored in Health\u00A0Connect"
                )
            )
            .check(doesNotExist())
    }

    @Test
    fun fitnessAccessFragment_logFitnessImpression() {
        val map =
            mapOf(
                AppAccessState.Read to
                    listOf(AppAccessMetadata(AppMetadata("package1", "appName1", null))),
                AppAccessState.Write to emptyList(),
                AppAccessState.Inactive to emptyList(),
            )
        whenever(viewModel.appMetadataMap).then {
            MutableLiveData<AccessScreenState>(WithData(map))
        }
        launchFragment<AccessFragment>(distanceBundle)

        verify(healthConnectLogger, atLeast(1)).setPageId(PageName.TAB_ACCESS_PAGE)
        verify(healthConnectLogger).logPageImpression()
        verify(healthConnectLogger).logImpression(DataAccessElement.DATA_ACCESS_APP_BUTTON)
    }

    @Test
    fun medicalAccessFragment_logMedicalImpression() {
        val map =
            mapOf(
                AppAccessState.Read to
                    listOf(AppAccessMetadata(AppMetadata("package1", "appName1", null))),
                AppAccessState.Write to emptyList(),
                AppAccessState.Inactive to emptyList(),
            )
        whenever(viewModel.appMetadataMap).then {
            MutableLiveData<AccessScreenState>(WithData(map))
        }
        launchFragment<AccessFragment>(immunizationBundle)

        verify(healthConnectLogger, atLeast(1)).setPageId(PageName.TAB_MEDICAL_ACCESS_PAGE)
        verify(healthConnectLogger).logPageImpression()
        verify(healthConnectLogger).logImpression(DataAccessElement.DATA_ACCESS_APP_BUTTON)
    }

    @Test
    fun dataAccessFragment_readSection_isDisplayed() {
        val map =
            mapOf(
                AppAccessState.Read to
                    listOf(AppAccessMetadata(AppMetadata("package1", "appName1", null))),
                AppAccessState.Write to emptyList(),
                AppAccessState.Inactive to emptyList(),
            )
        whenever(viewModel.appMetadataMap).then {
            MutableLiveData<AccessScreenState>(WithData(map))
        }
        launchFragment<AccessFragment>(distanceBundle)

        onView(withText("Can read distance")).check(matches(isDisplayed()))
        onView(withText("Can write distance")).check(doesNotExist())
        onView(withText("Inactive apps")).check(doesNotExist())
        onView(
                withText(
                    "These apps can no longer read or write distance, but still have data stored in Health\u00A0Connect"
                )
            )
            .check(doesNotExist())
    }

    @Test
    fun dataAccessFragment_readAndWriteSections_isDisplayed() {
        val map =
            mapOf(
                AppAccessState.Read to
                    listOf(AppAccessMetadata(AppMetadata("package1", "appName1", null))),
                AppAccessState.Write to
                    listOf(AppAccessMetadata(AppMetadata("package1", "appName1", null))),
                AppAccessState.Inactive to emptyList(),
            )
        whenever(viewModel.appMetadataMap).then {
            MutableLiveData<AccessScreenState>(WithData(map))
        }
        launchFragment<AccessFragment>(distanceBundle)

        onView(withText("Can read distance")).check(matches(isDisplayed()))
        onView(withText("Can write distance")).check(matches(isDisplayed()))
        onView(withText("Inactive apps")).check(doesNotExist())
        onView(
                withText(
                    "These apps can no longer read or write distance, but still have data stored in Health\u00A0Connect"
                )
            )
            .check(doesNotExist())
        verify(healthConnectLogger, times(2))
            .logImpression(DataAccessElement.DATA_ACCESS_APP_BUTTON)
    }

    @Test
    fun dataAccessFragment_inactiveSection_isDisplayed() {
        val map =
            mapOf(
                AppAccessState.Read to emptyList(),
                AppAccessState.Write to emptyList(),
                AppAccessState.Inactive to
                    listOf(AppAccessMetadata(AppMetadata("package1", "appName1", null))),
            )
        whenever(viewModel.appMetadataMap).then {
            MutableLiveData<AccessScreenState>(WithData(map))
        }
        launchFragment<AccessFragment>(distanceBundle)

        onView(withText("Can read distance")).check(doesNotExist())
        onView(withText("Can write distance")).check(doesNotExist())
        onView(withText("Inactive apps")).check(matches(isDisplayed()))
        onView(
                withText(
                    "These apps can no longer read or write distance, but still have data stored in Health\u00A0Connect"
                )
            )
            .check(matches(isDisplayed()))
        verify(healthConnectLogger).logImpression(DataAccessElement.DATA_ACCESS_INACTIVE_APP_BUTTON)
    }

    @Test
    fun dataAccessFragment_loadingState_showsLoading() {
        whenever(viewModel.appMetadataMap).then {
            MutableLiveData<AccessScreenState>(AccessScreenState.Loading)
        }
        launchFragment<AccessFragment>(distanceBundle)
        onView(withId(R.id.progress_indicator)).check(matches(isDisplayed()))
    }

    @Test
    fun dataAccessFragment_withData_hidesLoading() {
        whenever(viewModel.appMetadataMap).then {
            MutableLiveData<AccessScreenState>(WithData(emptyMap()))
        }
        launchFragment<AccessFragment>(distanceBundle)
        onView(withId(R.id.progress_indicator)).check(matches(not(isDisplayed())))
    }

    @Test
    fun dataAccessFragment_withError_showError() {
        whenever(viewModel.appMetadataMap).then {
            MutableLiveData<AccessScreenState>(AccessScreenState.Error)
        }
        launchFragment<AccessFragment>(distanceBundle)
        onView(withId(R.id.progress_indicator)).check(matches(not(isDisplayed())))
        onView(withId(R.id.error_view)).check(matches(isDisplayed()))
    }

    @Test
    fun whenAppNameClicked_navigatesToFitnessApp() {
        val map =
            mapOf(
                AppAccessState.Read to listOf(AppAccessMetadata(TEST_APP)),
                AppAccessState.Write to emptyList(),
                AppAccessState.Inactive to emptyList(),
            )

        whenever(viewModel.appMetadataMap).then {
            MutableLiveData<AccessScreenState>(WithData(map))
        }

        launchFragment<AccessFragment>(distanceBundle) {
            navHostController.setGraph(R.navigation.data_nav_graph_new_ia)
            navHostController.setCurrentDestination(R.id.entriesAndAccessFragment)
            Navigation.setViewNavController(this.requireView(), navHostController)
        }

        onView(withText(TEST_APP_NAME)).check(matches(isDisplayed()))
        onView(withText(TEST_APP_NAME)).perform(click())
        assertThat(navHostController.currentDestination?.id).isEqualTo(R.id.fitnessAppFragment)
    }

    @Test
    fun whenAppNameClicked_navigatesToMedicalApp() {
        val map =
            mapOf(
                AppAccessState.Read to
                    listOf(
                        AppAccessMetadata(TEST_APP, AppPermissionsType.MEDICAL_PERMISSIONS_ONLY)
                    ),
                AppAccessState.Write to emptyList(),
                AppAccessState.Inactive to emptyList(),
            )

        whenever(viewModel.appMetadataMap).then {
            MutableLiveData<AccessScreenState>(WithData(map))
        }

        launchFragment<AccessFragment>(distanceBundle) {
            navHostController.setGraph(R.navigation.data_nav_graph_new_ia)
            navHostController.setCurrentDestination(R.id.entriesAndAccessFragment)
            Navigation.setViewNavController(this.requireView(), navHostController)
        }

        onView(withText(TEST_APP_NAME)).check(matches(isDisplayed()))
        onView(withText(TEST_APP_NAME)).perform(click())
        assertThat(navHostController.currentDestination?.id).isEqualTo(R.id.medicalAppFragment)
    }

    @Test
    fun whenAppNameClicked_navigatesToCombinedPermissions() {
        val map =
            mapOf(
                AppAccessState.Read to
                    listOf(AppAccessMetadata(TEST_APP, AppPermissionsType.COMBINED_PERMISSIONS)),
                AppAccessState.Write to emptyList(),
                AppAccessState.Inactive to emptyList(),
            )

        whenever(viewModel.appMetadataMap).then {
            MutableLiveData<AccessScreenState>(WithData(map))
        }

        launchFragment<AccessFragment>(distanceBundle) {
            navHostController.setGraph(R.navigation.data_nav_graph_new_ia)
            navHostController.setCurrentDestination(R.id.entriesAndAccessFragment)
            Navigation.setViewNavController(this.requireView(), navHostController)
        }

        onView(withText(TEST_APP_NAME)).check(matches(isDisplayed()))
        onView(withText(TEST_APP_NAME)).perform(click())
        assertThat(navHostController.currentDestination?.id)
            .isEqualTo(R.id.combinedPermissionsFragment)
    }

    @Test
    fun dataAccessFragment_medicalPermission_readSectionOnly() {
        val map =
            mapOf(
                AppAccessState.Read to
                    listOf(AppAccessMetadata(AppMetadata("package1", "appName1", null))),
                AppAccessState.Write to emptyList(),
                AppAccessState.Inactive to emptyList(),
            )
        whenever(viewModel.appMetadataMap).then {
            MutableLiveData<AccessScreenState>(WithData(map))
        }
        launchFragment<AccessFragment>(immunizationBundle)

        onView(withText("Can read vaccines")).check(matches(isDisplayed()))
        onView(withText("Can write vaccines")).check(doesNotExist())
        onView(withText("Inactive apps")).check(doesNotExist())
        onView(
                withText(
                    "These apps can no longer read or write distance, but still have data stored in Health\u00A0Connect"
                )
            )
            .check(doesNotExist())
    }

    @Test
    fun dataAccessFragment_medicalPermission_writeSectionOnly() {
        val map =
            mapOf(
                AppAccessState.Read to emptyList(),
                AppAccessState.Write to
                    listOf(AppAccessMetadata(AppMetadata("package1", "appName1", null))),
                AppAccessState.Inactive to emptyList(),
            )
        whenever(viewModel.appMetadataMap).then {
            MutableLiveData<AccessScreenState>(WithData(map))
        }
        launchFragment<AccessFragment>(allMedicalDataBundle)

        onView(withText("Can read all health records")).check(doesNotExist())
        onView(withText("Can write all health records")).check(matches(isDisplayed()))
        onView(withText("Inactive apps")).check(doesNotExist())
        onView(
                withText(
                    "These apps can no longer read or write distance, but still have data stored in Health\u00A0Connect"
                )
            )
            .check(doesNotExist())
    }

    private val distanceBundle: Bundle
        get() {
            val bundle = Bundle()
            bundle.putString(PERMISSION_TYPE_NAME_KEY, FitnessPermissionType.DISTANCE.name)
            return bundle
        }

    private val immunizationBundle: Bundle
        get() {
            val bundle = Bundle()
            bundle.putString(PERMISSION_TYPE_NAME_KEY, MedicalPermissionType.VACCINES.name)
            return bundle
        }

    private val allMedicalDataBundle: Bundle
        get() {
            val bundle = Bundle()
            bundle.putString(PERMISSION_TYPE_NAME_KEY, MedicalPermissionType.ALL_MEDICAL_DATA.name)
            return bundle
        }
}
