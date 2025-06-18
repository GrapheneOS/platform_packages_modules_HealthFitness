/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.android.healthconnect.controller.tests.permissions.connectedapps

import android.content.Intent
import android.content.Intent.*
import androidx.core.os.bundleOf
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.preference.PreferenceCategory
import androidx.test.espresso.Espresso.onIdle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.isNotEnabled
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.permissions.additionalaccess.AdditionalAccessViewModel
import com.android.healthconnect.controller.permissions.additionalaccess.PermissionUiState
import com.android.healthconnect.controller.permissions.app.AppPermissionViewModel
import com.android.healthconnect.controller.permissions.app.AppPermissionViewModel.RevokeAllState.NotStarted
import com.android.healthconnect.controller.permissions.app.CombinedPermissionsFragment
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.permissions.data.MedicalPermissionType
import com.android.healthconnect.controller.shared.Constants.EXTRA_APP_NAME
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.tests.TestActivity
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.launchFragment
import com.android.healthconnect.controller.tests.utils.setLocale
import com.android.healthconnect.controller.utils.logging.AppAccessElement
import com.android.healthconnect.controller.utils.logging.CombinedAppAccessElement
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.PageName
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.ZoneId
import java.util.Locale
import java.util.TimeZone
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class CombinedPermissionsFragmentTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @BindValue val viewModel: AppPermissionViewModel = mock()
    @BindValue val healthConnectLogger: HealthConnectLogger = mock()
    @BindValue val healthPermissionReader: HealthPermissionReader = mock()
    @BindValue val additionalAccessViewModel: AdditionalAccessViewModel = mock()
    private lateinit var navHostController: TestNavHostController

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().context
        context.setLocale(Locale.US)
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("UTC")))
        navHostController = TestNavHostController(context)
        hiltRule.inject()
        whenever(viewModel.appInfo).then {
            MutableLiveData(
                AppMetadata(
                    TEST_APP_PACKAGE_NAME,
                    TEST_APP_NAME,
                    context.getDrawable(R.drawable.health_connect_logo),
                )
            )
        }
        whenever(additionalAccessViewModel.additionalAccessState).then {
            MutableLiveData(AdditionalAccessViewModel.State())
        }
        whenever(viewModel.atLeastOneHealthPermissionGranted).then { MediatorLiveData(true) }
        whenever(viewModel.revokeAllHealthPermissionsState).then { MutableLiveData(NotStarted) }
        Intents.init()
    }

    @After
    fun teardown() {
        reset(healthConnectLogger)
        Intents.release()
    }

    @Test
    fun correctNumberOfPreferences_withoutAdditionalAccess() {
        val scenario =
            launchFragment<CombinedPermissionsFragment>(
                bundleOf(
                    EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                    EXTRA_APP_NAME to TEST_APP_NAME,
                )
            )

        scenario.onActivity { activity: TestActivity ->
            val fragment =
                activity.supportFragmentManager.findFragmentById(android.R.id.content)
                    as CombinedPermissionsFragment
            val managePermissions =
                fragment.preferenceScreen.findPreference("manage_permissions")
                    as PreferenceCategory?
            val manageApp =
                fragment.preferenceScreen.findPreference("manage_app") as PreferenceCategory?
            assertThat(managePermissions?.preferenceCount).isEqualTo(2)
            assertThat(manageApp?.preferenceCount).isEqualTo(2)

            verify(healthConnectLogger, atLeast(1)).setPageId(PageName.COMBINED_APP_ACCESS_PAGE)
            verify(healthConnectLogger).logPageImpression()
            verify(healthConnectLogger)
                .logImpression(CombinedAppAccessElement.FITNESS_PERMISSIONS_BUTTON)
            verify(healthConnectLogger)
                .logImpression(CombinedAppAccessElement.MEDICAL_PERMISSIONS_BUTTON)
        }
    }

    @Test
    fun correctNumberOfPreferences_withAdditionalAccess() {
        val validState =
            AdditionalAccessViewModel.State(
                exerciseRoutePermissionUIState = PermissionUiState.ASK_EVERY_TIME,
                exercisePermissionUIState = PermissionUiState.ASK_EVERY_TIME,
            )
        whenever(additionalAccessViewModel.additionalAccessState).then {
            MutableLiveData(validState)
        }
        val scenario =
            launchFragment<CombinedPermissionsFragment>(
                bundleOf(
                    EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                    EXTRA_APP_NAME to TEST_APP_NAME,
                )
            )

        scenario.onActivity { activity: TestActivity ->
            val fragment =
                activity.supportFragmentManager.findFragmentById(android.R.id.content)
                    as CombinedPermissionsFragment
            val managePermissions =
                fragment.preferenceScreen.findPreference("manage_permissions")
                    as PreferenceCategory?
            val manageApp =
                fragment.preferenceScreen.findPreference("manage_app") as PreferenceCategory?
            assertThat(managePermissions?.preferenceCount).isEqualTo(3)
            assertThat(manageApp?.preferenceCount).isEqualTo(2)
        }
    }

    @Test
    fun correctStringsDisplayed_withoutAdditionalAccess() {
        launchFragment<CombinedPermissionsFragment>(
            bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME, EXTRA_APP_NAME to TEST_APP_NAME)
        )

        onView(withText("Health Connect test app")).check(matches(isDisplayed()))
        onView(withText("Permissions")).check(matches(isDisplayed()))
        onView(withText("Fitness and wellness")).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withText("Exercise, sleep, nutrition and others"))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
        onView(withText("Health records")).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withText("Lab results, medications, vaccines and others"))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
        onView(withText("Additional access")).check(doesNotExist())
        onView(withText("Past data, background data")).check(doesNotExist())
        onView(withText("Manage app")).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withText("See app data")).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withText("Remove access for this app"))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
        onView(withText("Remove access for this app"))
            .perform(scrollTo())
            .check(matches(isEnabled()))
    }

    @Test
    fun footer_isDisplayed() {
        whenever(healthPermissionReader.isRationaleIntentDeclared(TEST_APP_PACKAGE_NAME))
            .thenReturn(true)
        whenever(healthPermissionReader.getApplicationRationaleIntent(TEST_APP_PACKAGE_NAME))
            .thenReturn(Intent())

        launchFragment<CombinedPermissionsFragment>(
            bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME, EXTRA_APP_NAME to TEST_APP_NAME)
        )

        onIdle()
        onView(
                withText(
                    "To manage other Android permissions this app can " +
                        "access, go to Settings > Apps" +
                        "\n\n" +
                        "You can learn how $TEST_APP_NAME handles your data in the developer's privacy policy"
                )
            )
            .perform(scrollTo())
            .check(matches(isDisplayed()))
        onView(withText("Read privacy policy")).perform(scrollTo()).check(matches(isDisplayed()))
        verify(healthConnectLogger).logImpression(AppAccessElement.PRIVACY_POLICY_LINK)
    }

    @Test
    fun additionalAccessState_notValid_hidesAdditionalAccess() {
        whenever(additionalAccessViewModel.additionalAccessState).then {
            MutableLiveData(AdditionalAccessViewModel.State())
        }

        launchFragment<CombinedPermissionsFragment>(
            bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME, EXTRA_APP_NAME to TEST_APP_NAME)
        )

        onView(withText("Additional access")).check(doesNotExist())
    }

    @Test
    fun additionalAccessState_valid_showsAdditionalAccess() {
        val validState =
            AdditionalAccessViewModel.State(
                exerciseRoutePermissionUIState = PermissionUiState.ASK_EVERY_TIME,
                exercisePermissionUIState = PermissionUiState.ASK_EVERY_TIME,
            )
        whenever(additionalAccessViewModel.additionalAccessState).then {
            MutableLiveData(validState)
        }

        launchFragment<CombinedPermissionsFragment>(
            bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME, EXTRA_APP_NAME to TEST_APP_NAME)
        )

        onView(withText("Additional access")).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withText("Past data, background data"))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
    }

    @Test
    fun additionalAccessState_onlyOneAdditionalPermission_showsAdditionalAccess() {
        val validState =
            AdditionalAccessViewModel.State(
                backgroundReadUIState =
                    AdditionalAccessViewModel.AdditionalPermissionState(
                        isDeclared = true,
                        isEnabled = false,
                        isGranted = false,
                    )
            )
        whenever(additionalAccessViewModel.additionalAccessState).then {
            MutableLiveData(validState)
        }

        launchFragment<CombinedPermissionsFragment>(
            bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME, EXTRA_APP_NAME to TEST_APP_NAME)
        )

        onView(withText("Additional access")).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withText("Past data, background data"))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
        verify(healthConnectLogger).logImpression(AppAccessElement.ADDITIONAL_ACCESS_BUTTON)
    }

    @Test
    fun additionalAccessState_onClick_navigatesToAdditionalAccessFragment() {
        val validState =
            AdditionalAccessViewModel.State(
                exerciseRoutePermissionUIState = PermissionUiState.ASK_EVERY_TIME,
                exercisePermissionUIState = PermissionUiState.ASK_EVERY_TIME,
            )
        whenever(additionalAccessViewModel.additionalAccessState).then {
            MutableLiveData(validState)
        }

        launchFragment<CombinedPermissionsFragment>(
            bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME, EXTRA_APP_NAME to TEST_APP_NAME)
        ) {
            navHostController.setGraph(R.navigation.nav_graph)
            navHostController.setCurrentDestination(R.id.combinedPermissionsFragment)
            Navigation.setViewNavController(requireView(), navHostController)
        }
        onView(withText("Additional access")).perform(scrollTo()).perform(click())

        onIdle()
        assertThat(navHostController.currentDestination?.id)
            .isEqualTo(R.id.additionalAccessFragment)
        verify(healthConnectLogger).logInteraction(AppAccessElement.ADDITIONAL_ACCESS_BUTTON)
    }

    @Test
    fun fitnessPermissions_navigatesToFitnessAppFragment() {
        launchFragment<CombinedPermissionsFragment>(
            bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME, EXTRA_APP_NAME to TEST_APP_NAME)
        ) {
            navHostController.setGraph(R.navigation.nav_graph)
            navHostController.setCurrentDestination(R.id.combinedPermissionsFragment)
            Navigation.setViewNavController(requireView(), navHostController)
        }
        onView(withText("Fitness and wellness")).perform(scrollTo()).perform(click())

        assertThat(navHostController.currentDestination?.id).isEqualTo(R.id.fitnessAppFragment)
    }

    @Test
    fun medicalPermissions_navigatesToMedicalAppFragment() {
        launchFragment<CombinedPermissionsFragment>(
            bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME, EXTRA_APP_NAME to TEST_APP_NAME)
        ) {
            navHostController.setGraph(R.navigation.nav_graph)
            navHostController.setCurrentDestination(R.id.combinedPermissionsFragment)
            Navigation.setViewNavController(requireView(), navHostController)
        }
        onView(withText("Health records")).perform(scrollTo()).perform(click())

        assertThat(navHostController.currentDestination?.id).isEqualTo(R.id.medicalAppFragment)
    }

    @Test
    fun noHealthPermissionsGranted_removeAccessButtonDisabled() {
        whenever(viewModel.atLeastOneHealthPermissionGranted).then { MediatorLiveData(false) }

        launchFragment<CombinedPermissionsFragment>(
            bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME, EXTRA_APP_NAME to TEST_APP_NAME)
        )
        onView(withText("Remove access for this app"))
            .perform(scrollTo())
            .check(matches(isNotEnabled()))
    }

    @Test
    fun removeAccessButton_withAdditionalPermissions_showsConfirmationDialog() {
        whenever(additionalAccessViewModel.additionalAccessState).then {
            MutableLiveData(
                AdditionalAccessViewModel.State(
                    backgroundReadUIState =
                        AdditionalAccessViewModel.AdditionalPermissionState(
                            isDeclared = true,
                            isEnabled = true,
                            isGranted = true,
                        ),
                    historyReadUIState =
                        AdditionalAccessViewModel.AdditionalPermissionState(
                            isDeclared = true,
                            isEnabled = true,
                            isGranted = true,
                        ),
                )
            )
        }
        whenever(viewModel.revokeAllShouldIncludeBackground()).thenReturn(true)
        whenever(viewModel.revokeAllShouldIncludePastData()).thenReturn(true)
        whenever(viewModel.medicalPermissions).then {
            MutableLiveData(
                listOf(HealthPermission.MedicalPermission(MedicalPermissionType.ALL_MEDICAL_DATA))
            )
        }
        launchFragment<CombinedPermissionsFragment>(
            bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME, EXTRA_APP_NAME to TEST_APP_NAME)
        )
        onView(withText("Remove access for this app"))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
        verify(healthConnectLogger)
            .logImpression(CombinedAppAccessElement.REMOVE_ALL_PERMISSIONS_BUTTON)
        onView(withText("Remove access for this app")).perform(scrollTo()).perform(click())

        onView(withText("Remove all permissions?")).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(
                withText(
                    "$TEST_APP_NAME will no longer be able to read or write" +
                        " data from Health Connect, including background and past data." +
                        "\n\nThis doesn't affect other permissions this app may have, like camera, " +
                        "microphone or location."
                )
            )
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        onView(
                withText(
                    "Also delete fitness data and health records from " +
                        "$TEST_APP_NAME from Health Connect"
                )
            )
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
    }

    @Test
    fun removeAccessButton_withBackgroundPermission_showsConfirmationDialog() {
        whenever(additionalAccessViewModel.additionalAccessState).then {
            MutableLiveData(
                AdditionalAccessViewModel.State(
                    backgroundReadUIState =
                        AdditionalAccessViewModel.AdditionalPermissionState(
                            isDeclared = true,
                            isEnabled = true,
                            isGranted = true,
                        ),
                    historyReadUIState =
                        AdditionalAccessViewModel.AdditionalPermissionState(
                            isDeclared = false,
                            isEnabled = false,
                            isGranted = false,
                        ),
                )
            )
        }
        whenever(viewModel.revokeAllShouldIncludeBackground()).thenReturn(true)
        whenever(viewModel.revokeAllShouldIncludePastData()).thenReturn(false)
        whenever(viewModel.medicalPermissions).then {
            MutableLiveData(
                listOf(HealthPermission.MedicalPermission(MedicalPermissionType.ALL_MEDICAL_DATA))
            )
        }
        launchFragment<CombinedPermissionsFragment>(
            bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME, EXTRA_APP_NAME to TEST_APP_NAME)
        )
        onView(withText("Remove access for this app"))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
        onView(withText("Remove access for this app")).perform(scrollTo()).perform(click())

        onView(withText("Remove all permissions?")).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(
                withText(
                    "$TEST_APP_NAME will no longer be able to read or write" +
                        " data from Health Connect, including background data." +
                        "\n\nThis doesn't affect other permissions this app may have, like camera, " +
                        "microphone or location."
                )
            )
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        onView(
                withText(
                    "Also delete fitness data and health records from " +
                        "$TEST_APP_NAME from Health Connect"
                )
            )
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
    }

    @Test
    fun removeAccessButton_withHistoricReadPermission_showsConfirmationDialog() {
        whenever(additionalAccessViewModel.additionalAccessState).then {
            MutableLiveData(
                AdditionalAccessViewModel.State(
                    backgroundReadUIState =
                        AdditionalAccessViewModel.AdditionalPermissionState(
                            isDeclared = false,
                            isEnabled = false,
                            isGranted = false,
                        ),
                    historyReadUIState =
                        AdditionalAccessViewModel.AdditionalPermissionState(
                            isDeclared = true,
                            isEnabled = true,
                            isGranted = true,
                        ),
                )
            )
        }
        whenever(viewModel.revokeAllShouldIncludeBackground()).thenReturn(false)
        whenever(viewModel.revokeAllShouldIncludePastData()).thenReturn(true)
        whenever(viewModel.medicalPermissions).then {
            MutableLiveData(
                listOf(HealthPermission.MedicalPermission(MedicalPermissionType.ALL_MEDICAL_DATA))
            )
        }
        launchFragment<CombinedPermissionsFragment>(
            bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME, EXTRA_APP_NAME to TEST_APP_NAME)
        )
        onView(withText("Remove access for this app"))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
        onView(withText("Remove access for this app")).perform(scrollTo()).perform(click())

        onView(withText("Remove all permissions?")).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(
                withText(
                    "$TEST_APP_NAME will no longer be able to read or write" +
                        " data from Health Connect, including past data." +
                        "\n\nThis doesn't affect other permissions this app may have, like camera, " +
                        "microphone or location."
                )
            )
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        onView(
                withText(
                    "Also delete fitness data and health records from " +
                        "$TEST_APP_NAME from Health Connect"
                )
            )
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
    }

    @Test
    fun removeAccessButton_noAdditionalPermissions_showsConfirmationDialog() {
        whenever(viewModel.revokeAllShouldIncludeBackground()).thenReturn(false)
        whenever(viewModel.revokeAllShouldIncludePastData()).thenReturn(false)
        whenever(viewModel.medicalPermissions).then {
            MutableLiveData(
                listOf(HealthPermission.MedicalPermission(MedicalPermissionType.ALL_MEDICAL_DATA))
            )
        }
        launchFragment<CombinedPermissionsFragment>(
            bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME, EXTRA_APP_NAME to TEST_APP_NAME)
        )
        onView(withText("Remove access for this app"))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
        onView(withText("Remove access for this app")).perform(scrollTo()).perform(click())

        onView(withText("Remove all permissions?")).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(
                withText(
                    "$TEST_APP_NAME will no longer be able to read or write" +
                        " data from Health Connect." +
                        "\n\nThis doesn't affect other permissions this app may have, like camera, " +
                        "microphone or location."
                )
            )
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        onView(
                withText(
                    "Also delete fitness data and health records from " +
                        "$TEST_APP_NAME from Health Connect"
                )
            )
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
    }

    @Test
    fun removeAccessButton_confirmationDialogWithCheckbox_remainsAfterRotation() {
        whenever(viewModel.revokeAllShouldIncludeBackground()).thenReturn(false)
        whenever(viewModel.revokeAllShouldIncludePastData()).thenReturn(false)
        whenever(viewModel.medicalPermissions).then {
            MutableLiveData(
                listOf(HealthPermission.MedicalPermission(MedicalPermissionType.ALL_MEDICAL_DATA))
            )
        }
        val scenario =
            launchFragment<CombinedPermissionsFragment>(
                bundleOf(
                    EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME,
                    EXTRA_APP_NAME to TEST_APP_NAME,
                )
            )
        onView(withText("Remove access for this app"))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
        onView(withText("Remove access for this app")).perform(scrollTo()).perform(click())

        onView(withText("Remove all permissions?")).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(
                withText(
                    "$TEST_APP_NAME will no longer be able to read or write" +
                        " data from Health Connect." +
                        "\n\nThis doesn't affect other permissions this app may have, like camera, " +
                        "microphone or location."
                )
            )
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        onView(
                withText(
                    "Also delete fitness data and health records from " +
                        "$TEST_APP_NAME from Health Connect"
                )
            )
            .inRoot(isDialog())
            .check(matches(isDisplayed()))

        scenario.recreate()
        onView(withText("Remove all permissions?")).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(
                withText(
                    "$TEST_APP_NAME will no longer be able to read or write" +
                        " data from Health Connect." +
                        "\n\nThis doesn't affect other permissions this app may have, like camera, " +
                        "microphone or location."
                )
            )
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        onView(
                withText(
                    "Also delete fitness data and health records from " +
                        "$TEST_APP_NAME from Health Connect"
                )
            )
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
    }
}
