/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */

package com.android.healthconnect.controller.tests.permissions.additionalaccess

import android.content.Intent.EXTRA_PACKAGE_NAME
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onIdle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.permissions.additionalaccess.AdditionalAccessFragment
import com.android.healthconnect.controller.permissions.additionalaccess.AdditionalAccessViewModel
import com.android.healthconnect.controller.permissions.additionalaccess.AdditionalAccessViewModel.EnableExerciseDialogEvent
import com.android.healthconnect.controller.permissions.additionalaccess.AdditionalAccessViewModel.State
import com.android.healthconnect.controller.permissions.additionalaccess.PermissionUiState.ALWAYS_ALLOW
import com.android.healthconnect.controller.permissions.additionalaccess.PermissionUiState.ASK_EVERY_TIME
import com.android.healthconnect.controller.permissions.additionalaccess.PermissionUiState.NEVER_ALLOW
import com.android.healthconnect.controller.permissions.app.AppPermissionViewModel
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.shared.preference.HealthSwitchPreference
import com.android.healthconnect.controller.tests.utils.NOW
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.launchFragment
import com.android.healthconnect.controller.utils.logging.AdditionalAccessElement
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.PageName
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.reset
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AdditionalAccessFragmentTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @BindValue val permissionsViewModel: AppPermissionViewModel = mock()
    @BindValue val additionalAccessViewModel: AdditionalAccessViewModel = mock()
    @BindValue val healthConnectLogger: HealthConnectLogger = mock()

    private val context = InstrumentationRegistry.getInstrumentation().context

    @Before
    fun setup() {
        hiltRule.inject()

        whenever(permissionsViewModel.appInfo).then {
            MutableLiveData(
                AppMetadata(
                    TEST_APP_PACKAGE_NAME,
                    TEST_APP_NAME,
                    context.getDrawable(R.drawable.health_connect_logo),
                )
            )
        }
        whenever(additionalAccessViewModel.screenState).then {
            MutableLiveData(AdditionalAccessViewModel.ScreenState())
        }
        whenever(additionalAccessViewModel.showEnableExerciseEvent)
            .thenReturn(MediatorLiveData(EnableExerciseDialogEvent()))
        whenever(additionalAccessViewModel.loadAccessDate(any())).thenReturn(NOW)
    }

    @After
    fun teardown() {
        reset(healthConnectLogger)
    }

    @Test
    fun validArgument_startsFragment() {
        launchFragment<AdditionalAccessFragment>(
                bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME)
            )
            .use { scenario -> assertThat(scenario.getState()).isEqualTo(Lifecycle.State.RESUMED) }
    }

    @Test
    fun loadsAdditionalAccessPreferences() {
        launchFragment<AdditionalAccessFragment>(
                bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME)
            )
            .use {
                verify(additionalAccessViewModel)
                    .loadAdditionalAccessPreferences(eq(TEST_APP_PACKAGE_NAME))
            }
    }

    @Test
    fun exerciseRouteDeclared_showsExerciseRouteOption() {
        val exerciseRouteDeclaredState = State(exerciseRoutePermissionUIState = ASK_EVERY_TIME)
        whenever(additionalAccessViewModel.screenState).then {
            MutableLiveData(
                AdditionalAccessViewModel.ScreenState(state = exerciseRouteDeclaredState)
            )
        }

        launchFragment<AdditionalAccessFragment>(
                bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME)
            )
            .use {
                onView(withText(R.string.route_permissions_label)).check(matches(isDisplayed()))
                onView(withText(R.string.route_permissions_ask)).check(matches(isDisplayed()))

                verify(healthConnectLogger, atLeast(1)).setPageId(PageName.ADDITIONAL_ACCESS_PAGE)
                verify(healthConnectLogger).logPageImpression()
                verify(healthConnectLogger)
                    .logImpression(AdditionalAccessElement.EXERCISE_ROUTES_BUTTON)
            }
    }

    @Test
    fun exerciseRouteGranted_showsExerciseRouteOption() {
        val exerciseRouteGrantedState = State(exerciseRoutePermissionUIState = ALWAYS_ALLOW)
        whenever(additionalAccessViewModel.screenState).then {
            MutableLiveData(
                AdditionalAccessViewModel.ScreenState(state = exerciseRouteGrantedState)
            )
        }

        launchFragment<AdditionalAccessFragment>(
                bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME)
            )
            .use {
                onView(withText(R.string.route_permissions_label)).check(matches(isDisplayed()))
                onView(withText(R.string.route_permissions_always_allow))
                    .check(matches(isDisplayed()))
            }
    }

    @Test
    fun exerciseRouteRevoked_showsExerciseRouteOption() {
        val exerciseRouteRevokedState = State(exerciseRoutePermissionUIState = NEVER_ALLOW)
        whenever(additionalAccessViewModel.screenState).then {
            MutableLiveData(
                AdditionalAccessViewModel.ScreenState(state = exerciseRouteRevokedState)
            )
        }

        launchFragment<AdditionalAccessFragment>(
                bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME)
            )
            .use {
                onView(withText(R.string.route_permissions_label)).check(matches(isDisplayed()))
                onView(withText(R.string.route_permissions_deny)).check(matches(isDisplayed()))
            }
    }

    @Test
    fun healthSwitchPreference_contentDescription_isUnchanged() {
        val state =
            State(
                exerciseRoutePermissionUIState = ALWAYS_ALLOW,
                historyReadUIState =
                    AdditionalAccessViewModel.AdditionalPermissionState(
                        isDeclared = true,
                        isEnabled = true,
                        isGranted = true,
                    ),
                backgroundReadUIState =
                    AdditionalAccessViewModel.AdditionalPermissionState(
                        isDeclared = true,
                        isEnabled = true,
                        isGranted = true,
                    ),
            )
        whenever(additionalAccessViewModel.screenState).then {
            MutableLiveData(
                AdditionalAccessViewModel.ScreenState(
                    state = state,
                    appHasDeclaredMedicalPermissions = true,
                    appHasGrantedFitnessReadPermission = true,
                    showMedicalPastDataFooter = true,
                )
            )
        }
        launchFragment<AdditionalAccessFragment>(
                bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME)
            )
            .use { scenario ->
                scenario.onActivity { activity ->
                    val fragment =
                        activity.supportFragmentManager.findFragmentById(android.R.id.content)
                            as AdditionalAccessFragment
                    val historyPreference =
                        fragment.preferenceScreen.findPreference("key_history_read")
                            as HealthSwitchPreference?
                    val backgroundPreference =
                        fragment.preferenceScreen.findPreference("key_background_read")
                            as HealthSwitchPreference?

                    assertThat(historyPreference?.permission).isNull()
                    assertThat(backgroundPreference?.permission).isNull()
                }
            }
    }

    @Test
    fun historyReadDeclaredAndEnabled_onlyFitness_showsHistoryReadPreference() {
        val state =
            State(
                exerciseRoutePermissionUIState = ALWAYS_ALLOW,
                historyReadUIState =
                    AdditionalAccessViewModel.AdditionalPermissionState(
                        isDeclared = true,
                        isEnabled = true,
                        isGranted = true,
                    ),
            )
        whenever(additionalAccessViewModel.screenState).then {
            MutableLiveData(
                AdditionalAccessViewModel.ScreenState(
                    state = state,
                    appHasDeclaredMedicalPermissions = false,
                    appHasGrantedFitnessReadPermission = true,
                    showMedicalPastDataFooter = false,
                )
            )
        }
        launchFragment<AdditionalAccessFragment>(
                bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME)
            )
            .use { scenario ->
                onView(withText("Access exercise routes")).check(matches(isDisplayed()))
                onView(withText("Always allow")).check(matches(isDisplayed()))
                onView(withText("Access past data")).check(matches(isDisplayed()))
                onView(
                        withText(
                            "Allow this app to access Health Connect data added before October 20, 2022"
                        )
                    )
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Enable at least one read permission in order to turn on past data access for this app"
                        )
                    )
                    .check(doesNotExist())

                scenario.onActivity { activity ->
                    val fragment =
                        activity.supportFragmentManager.findFragmentById(android.R.id.content)
                            as AdditionalAccessFragment
                    val historyPreference =
                        fragment.preferenceScreen.findPreference("key_history_read")
                            as HealthSwitchPreference?
                    assertThat(historyPreference?.isChecked).isTrue()
                    assertThat(historyPreference?.isEnabled).isTrue()
                }

                verify(healthConnectLogger)
                    .logImpression(AdditionalAccessElement.EXERCISE_ROUTES_BUTTON)
                verify(healthConnectLogger)
                    .logImpression(AdditionalAccessElement.HISTORY_READ_BUTTON)
            }
    }

    @Test
    fun historyReadDeclaredAndNotEnabled_onlyFitness_showsFooter() {
        val state =
            State(
                exerciseRoutePermissionUIState = ALWAYS_ALLOW,
                historyReadUIState =
                    AdditionalAccessViewModel.AdditionalPermissionState(
                        isDeclared = true,
                        isEnabled = false,
                        isGranted = false,
                    ),
            )
        whenever(additionalAccessViewModel.screenState).then {
            MutableLiveData(
                AdditionalAccessViewModel.ScreenState(
                    state = state,
                    appHasDeclaredMedicalPermissions = false,
                    appHasGrantedFitnessReadPermission = false,
                    showMedicalPastDataFooter = false,
                )
            )
        }
        launchFragment<AdditionalAccessFragment>(
                bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME)
            )
            .use { scenario ->
                onView(withText("Access exercise routes")).check(matches(isDisplayed()))
                onView(withText("Always allow")).check(matches(isDisplayed()))
                onView(withText("Access past data")).check(matches(isDisplayed()))
                onView(
                        withText(
                            "Allow this app to access Health Connect data added before October 20, 2022"
                        )
                    )
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Enable at least one read permission in order to turn on past data access for this app"
                        )
                    )
                    .check(matches(isDisplayed()))

                scenario.onActivity { activity ->
                    val fragment =
                        activity.supportFragmentManager.findFragmentById(android.R.id.content)
                            as AdditionalAccessFragment
                    val historyPreference =
                        fragment.preferenceScreen.findPreference("key_history_read")
                            as HealthSwitchPreference?
                    assertThat(historyPreference?.isChecked).isFalse()
                    assertThat(historyPreference?.isEnabled).isFalse()
                }
            }
    }

    @Test
    fun backgroundReadDeclaredAndEnabled_onlyFitness_showsBackgroundReadPreference() {
        val state =
            State(
                exerciseRoutePermissionUIState = NEVER_ALLOW,
                backgroundReadUIState =
                    AdditionalAccessViewModel.AdditionalPermissionState(
                        isDeclared = true,
                        isEnabled = true,
                        isGranted = true,
                    ),
            )
        whenever(additionalAccessViewModel.screenState).then {
            MutableLiveData(
                AdditionalAccessViewModel.ScreenState(
                    state = state,
                    appHasDeclaredMedicalPermissions = false,
                    appHasGrantedFitnessReadPermission = true,
                    showMedicalPastDataFooter = false,
                )
            )
        }
        launchFragment<AdditionalAccessFragment>(
                bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME)
            )
            .use { scenario ->
                onView(withText("Access exercise routes")).check(matches(isDisplayed()))
                onView(withText("Don't allow")).check(matches(isDisplayed()))
                onView(withText("Access data in the background")).check(matches(isDisplayed()))
                onView(
                        withText(
                            "Allow this app to access Health Connect data when you're not using the app"
                        )
                    )
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Enable at least one read permission in order to turn on background access for this app"
                        )
                    )
                    .check(doesNotExist())

                scenario.onActivity { activity ->
                    val fragment =
                        activity.supportFragmentManager.findFragmentById(android.R.id.content)
                            as AdditionalAccessFragment
                    val backgroundPreference =
                        fragment.preferenceScreen.findPreference("key_background_read")
                            as HealthSwitchPreference?
                    assertThat(backgroundPreference?.isChecked).isTrue()
                    assertThat(backgroundPreference?.isEnabled).isTrue()
                }

                verify(healthConnectLogger)
                    .logImpression(AdditionalAccessElement.EXERCISE_ROUTES_BUTTON)
                verify(healthConnectLogger)
                    .logImpression(AdditionalAccessElement.BACKGROUND_READ_BUTTON)
            }
    }

    @Test
    fun backgroundReadDeclaredAndNotEnabled_onlyFitness_showsFooter() {
        val state =
            State(
                exerciseRoutePermissionUIState = NEVER_ALLOW,
                backgroundReadUIState =
                    AdditionalAccessViewModel.AdditionalPermissionState(
                        isDeclared = true,
                        isEnabled = false,
                        isGranted = false,
                    ),
            )
        whenever(additionalAccessViewModel.screenState).then {
            MutableLiveData(
                AdditionalAccessViewModel.ScreenState(
                    state = state,
                    appHasDeclaredMedicalPermissions = false,
                    appHasGrantedFitnessReadPermission = false,
                    showMedicalPastDataFooter = false,
                )
            )
        }
        launchFragment<AdditionalAccessFragment>(
                bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME)
            )
            .use { scenario ->
                onView(withText("Access exercise routes")).check(matches(isDisplayed()))
                onView(withText("Don't allow")).check(matches(isDisplayed()))
                onView(withText("Access data in the background")).check(matches(isDisplayed()))
                onView(
                        withText(
                            "Allow this app to access Health Connect data when you're not using the app"
                        )
                    )
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Enable at least one read permission in order to turn on background access for this app"
                        )
                    )
                    .check(matches(isDisplayed()))

                scenario.onActivity { activity ->
                    val fragment =
                        activity.supportFragmentManager.findFragmentById(android.R.id.content)
                            as AdditionalAccessFragment
                    val backgroundPreference =
                        fragment.preferenceScreen.findPreference("key_background_read")
                            as HealthSwitchPreference?
                    assertThat(backgroundPreference?.isChecked).isFalse()
                    assertThat(backgroundPreference?.isEnabled).isFalse()
                }
            }
    }

    @Test
    fun backgroundAndHistoryReadDeclaredAndEnabled_onlyFitness_showsBothPreferences() {
        val state =
            State(
                exerciseRoutePermissionUIState = ASK_EVERY_TIME,
                historyReadUIState =
                    AdditionalAccessViewModel.AdditionalPermissionState(
                        isDeclared = true,
                        isEnabled = true,
                        isGranted = true,
                    ),
                backgroundReadUIState =
                    AdditionalAccessViewModel.AdditionalPermissionState(
                        isDeclared = true,
                        isEnabled = true,
                        isGranted = true,
                    ),
            )
        whenever(additionalAccessViewModel.screenState).then {
            MutableLiveData(
                AdditionalAccessViewModel.ScreenState(
                    state = state,
                    appHasDeclaredMedicalPermissions = false,
                    appHasGrantedFitnessReadPermission = true,
                    showMedicalPastDataFooter = false,
                )
            )
        }
        launchFragment<AdditionalAccessFragment>(
                bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME)
            )
            .use { scenario ->
                onView(withText("Access exercise routes")).check(matches(isDisplayed()))
                onView(withText("Ask every time")).check(matches(isDisplayed()))
                onView(withText("Access data in the background")).check(matches(isDisplayed()))
                onView(
                        withText(
                            "Allow this app to access Health Connect data when you're not using the app"
                        )
                    )
                    .check(matches(isDisplayed()))
                onView(withText("Access past data")).check(matches(isDisplayed()))
                onView(
                        withText(
                            "Allow this app to access Health Connect data added before October 20, 2022"
                        )
                    )
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Enable at least one read permission in order to turn on background or past data access for this app"
                        )
                    )
                    .check(doesNotExist())

                scenario.onActivity { activity ->
                    val fragment =
                        activity.supportFragmentManager.findFragmentById(android.R.id.content)
                            as AdditionalAccessFragment
                    val historyPreference =
                        fragment.preferenceScreen.findPreference("key_history_read")
                            as HealthSwitchPreference?
                    assertThat(historyPreference?.isChecked).isTrue()
                    assertThat(historyPreference?.isEnabled).isTrue()

                    val backgroundPreference =
                        fragment.preferenceScreen.findPreference("key_background_read")
                            as HealthSwitchPreference?
                    assertThat(backgroundPreference?.isChecked).isTrue()
                    assertThat(backgroundPreference?.isEnabled).isTrue()
                }

                verify(healthConnectLogger)
                    .logImpression(AdditionalAccessElement.EXERCISE_ROUTES_BUTTON)
                verify(healthConnectLogger)
                    .logImpression(AdditionalAccessElement.HISTORY_READ_BUTTON)
                verify(healthConnectLogger)
                    .logImpression(AdditionalAccessElement.BACKGROUND_READ_BUTTON)
            }
    }

    @Test
    fun backgroundAndHistoryReadDeclaredAndNotEnabled_onlyFitness_showsFooter() {
        val state =
            State(
                exerciseRoutePermissionUIState = ASK_EVERY_TIME,
                historyReadUIState =
                    AdditionalAccessViewModel.AdditionalPermissionState(
                        isDeclared = true,
                        isEnabled = false,
                        isGranted = false,
                    ),
                backgroundReadUIState =
                    AdditionalAccessViewModel.AdditionalPermissionState(
                        isDeclared = true,
                        isEnabled = false,
                        isGranted = false,
                    ),
            )
        whenever(additionalAccessViewModel.screenState).then {
            MutableLiveData(
                AdditionalAccessViewModel.ScreenState(
                    state = state,
                    appHasDeclaredMedicalPermissions = false,
                    appHasGrantedFitnessReadPermission = false,
                    showMedicalPastDataFooter = false,
                )
            )
        }
        launchFragment<AdditionalAccessFragment>(
                bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME)
            )
            .use { scenario ->
                onView(withText("Access exercise routes")).check(matches(isDisplayed()))
                onView(withText("Ask every time")).check(matches(isDisplayed()))
                onView(withText("Access data in the background")).check(matches(isDisplayed()))
                onView(
                        withText(
                            "Allow this app to access Health Connect data when you're not using the app"
                        )
                    )
                    .check(matches(isDisplayed()))
                onView(withText("Access past data")).check(matches(isDisplayed()))
                onView(
                        withText(
                            "Allow this app to access Health Connect data added before October 20, 2022"
                        )
                    )
                    .check(matches(isDisplayed()))

                onView(withId(androidx.preference.R.id.recycler_view))
                    .perform(RecyclerViewActions.scrollToLastPosition<RecyclerView.ViewHolder>())
                onView(
                        withText(
                            "Enable at least one read permission in order to turn on background or past data access for this app"
                        )
                    )
                    .perform(scrollTo())
                    .check(matches(isDisplayed()))

                scenario.onActivity { activity ->
                    val fragment =
                        activity.supportFragmentManager.findFragmentById(android.R.id.content)
                            as AdditionalAccessFragment
                    val historyPreference =
                        fragment.preferenceScreen.findPreference("key_history_read")
                            as HealthSwitchPreference?
                    assertThat(historyPreference?.isChecked).isFalse()
                    assertThat(historyPreference?.isEnabled).isFalse()

                    val backgroundPreference =
                        fragment.preferenceScreen.findPreference("key_background_read")
                            as HealthSwitchPreference?
                    assertThat(backgroundPreference?.isChecked).isFalse()
                    assertThat(backgroundPreference?.isEnabled).isFalse()
                }
            }
    }

    @Test
    fun historyReadDeclaredAndEnabled_fitnessAndMedical_showsHistoryReadPreference() {
        val state =
            State(
                exerciseRoutePermissionUIState = ALWAYS_ALLOW,
                historyReadUIState =
                    AdditionalAccessViewModel.AdditionalPermissionState(
                        isDeclared = true,
                        isEnabled = true,
                        isGranted = true,
                    ),
            )
        whenever(additionalAccessViewModel.screenState).then {
            MutableLiveData(
                AdditionalAccessViewModel.ScreenState(
                    state = state,
                    appHasDeclaredMedicalPermissions = true,
                    appHasGrantedFitnessReadPermission = true,
                    showMedicalPastDataFooter = false,
                )
            )
        }
        launchFragment<AdditionalAccessFragment>(
                bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME)
            )
            .use { scenario ->
                onView(withText("Access exercise routes")).check(matches(isDisplayed()))
                onView(withText("Always allow")).check(matches(isDisplayed()))
                onView(withText("Access past fitness and wellness data"))
                    .check(matches(isDisplayed()))
                onView(withText("Allow this app to access data added before October 20, 2022"))
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Enable at least one read permission in order to turn on past data access for this app"
                        )
                    )
                    .check(doesNotExist())

                scenario.onActivity { activity ->
                    val fragment =
                        activity.supportFragmentManager.findFragmentById(android.R.id.content)
                            as AdditionalAccessFragment
                    val historyPreference =
                        fragment.preferenceScreen.findPreference("key_history_read")
                            as HealthSwitchPreference?
                    assertThat(historyPreference?.isChecked).isTrue()
                    assertThat(historyPreference?.isEnabled).isTrue()
                }

                verify(healthConnectLogger)
                    .logImpression(AdditionalAccessElement.EXERCISE_ROUTES_BUTTON)
                verify(healthConnectLogger)
                    .logImpression(AdditionalAccessElement.HISTORY_READ_BUTTON)
            }
    }

    @Test
    fun historyReadDeclaredAndNotEnabled_fitnessAndMedical_showsFooter() {
        val state =
            State(
                exerciseRoutePermissionUIState = ALWAYS_ALLOW,
                historyReadUIState =
                    AdditionalAccessViewModel.AdditionalPermissionState(
                        isDeclared = true,
                        isEnabled = false,
                        isGranted = false,
                    ),
            )
        whenever(additionalAccessViewModel.screenState).then {
            MutableLiveData(
                AdditionalAccessViewModel.ScreenState(
                    state = state,
                    appHasDeclaredMedicalPermissions = true,
                    appHasGrantedFitnessReadPermission = false,
                    showMedicalPastDataFooter = false,
                )
            )
        }
        launchFragment<AdditionalAccessFragment>(
                bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME)
            )
            .use { scenario ->
                onView(withText("Access exercise routes")).check(matches(isDisplayed()))
                onView(withText("Always allow")).check(matches(isDisplayed()))
                onView(withText("Access past fitness and wellness data"))
                    .check(matches(isDisplayed()))
                onView(withText("Allow this app to access data added before October 20, 2022"))
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Enable at least one read permission in order to turn on past data access for this app"
                        )
                    )
                    .check(matches(isDisplayed()))

                scenario.onActivity { activity ->
                    val fragment =
                        activity.supportFragmentManager.findFragmentById(android.R.id.content)
                            as AdditionalAccessFragment
                    val historyPreference =
                        fragment.preferenceScreen.findPreference("key_history_read")
                            as HealthSwitchPreference?
                    assertThat(historyPreference?.isChecked).isFalse()
                    assertThat(historyPreference?.isEnabled).isFalse()
                }
            }
    }

    @Test
    fun backgroundReadDeclaredAndEnabled_fitnessAndMedical_showsBackgroundReadPreference() {
        val state =
            State(
                exerciseRoutePermissionUIState = NEVER_ALLOW,
                backgroundReadUIState =
                    AdditionalAccessViewModel.AdditionalPermissionState(
                        isDeclared = true,
                        isEnabled = true,
                        isGranted = true,
                    ),
            )
        whenever(additionalAccessViewModel.screenState).then {
            MutableLiveData(
                AdditionalAccessViewModel.ScreenState(
                    state = state,
                    appHasDeclaredMedicalPermissions = true,
                    appHasGrantedFitnessReadPermission = true,
                    showMedicalPastDataFooter = false,
                )
            )
        }
        launchFragment<AdditionalAccessFragment>(
                bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME)
            )
            .use { scenario ->
                onView(withText("Access exercise routes")).check(matches(isDisplayed()))
                onView(withText("Don't allow")).check(matches(isDisplayed()))
                onView(withText("Access fitness and wellness data in the background"))
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Allow this app to access this data " + "when you're not using the app"
                        )
                    )
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Enable at least one read permission in order to turn on background access for this app"
                        )
                    )
                    .check(doesNotExist())

                scenario.onActivity { activity ->
                    val fragment =
                        activity.supportFragmentManager.findFragmentById(android.R.id.content)
                            as AdditionalAccessFragment
                    val backgroundPreference =
                        fragment.preferenceScreen.findPreference("key_background_read")
                            as HealthSwitchPreference?
                    assertThat(backgroundPreference?.isChecked).isTrue()
                    assertThat(backgroundPreference?.isEnabled).isTrue()
                }

                verify(healthConnectLogger)
                    .logImpression(AdditionalAccessElement.EXERCISE_ROUTES_BUTTON)
                verify(healthConnectLogger)
                    .logImpression(AdditionalAccessElement.BACKGROUND_READ_BUTTON)
            }
    }

    @Test
    fun backgroundReadDeclaredAndNotEnabled_fitnessAndMedical_showsFooter() {
        val state =
            State(
                exerciseRoutePermissionUIState = NEVER_ALLOW,
                backgroundReadUIState =
                    AdditionalAccessViewModel.AdditionalPermissionState(
                        isDeclared = true,
                        isEnabled = false,
                        isGranted = false,
                    ),
            )
        whenever(additionalAccessViewModel.screenState).then {
            MutableLiveData(
                AdditionalAccessViewModel.ScreenState(
                    state = state,
                    appHasDeclaredMedicalPermissions = true,
                    appHasGrantedFitnessReadPermission = true,
                    showMedicalPastDataFooter = false,
                )
            )
        }
        launchFragment<AdditionalAccessFragment>(
                bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME)
            )
            .use { scenario ->
                onView(withText("Access exercise routes")).check(matches(isDisplayed()))
                onView(withText("Don't allow")).check(matches(isDisplayed()))
                onView(withText("Access fitness and wellness data in the background"))
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Allow this app to access this data " + "when you're not using the app"
                        )
                    )
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Enable at least one read permission in order to turn on background access for this app"
                        )
                    )
                    .check(matches(isDisplayed()))

                scenario.onActivity { activity ->
                    val fragment =
                        activity.supportFragmentManager.findFragmentById(android.R.id.content)
                            as AdditionalAccessFragment
                    val backgroundPreference =
                        fragment.preferenceScreen.findPreference("key_background_read")
                            as HealthSwitchPreference?
                    assertThat(backgroundPreference?.isChecked).isFalse()
                    assertThat(backgroundPreference?.isEnabled).isFalse()
                }
            }
    }

    @Test
    fun backgroundAndHistoryReadDeclaredAndEnabled_fitnessAndMedical_showsBothPreferences() {
        val state =
            State(
                exerciseRoutePermissionUIState = ASK_EVERY_TIME,
                historyReadUIState =
                    AdditionalAccessViewModel.AdditionalPermissionState(
                        isDeclared = true,
                        isEnabled = true,
                        isGranted = true,
                    ),
                backgroundReadUIState =
                    AdditionalAccessViewModel.AdditionalPermissionState(
                        isDeclared = true,
                        isEnabled = true,
                        isGranted = true,
                    ),
            )
        whenever(additionalAccessViewModel.screenState).then {
            MutableLiveData(
                AdditionalAccessViewModel.ScreenState(
                    state = state,
                    appHasDeclaredMedicalPermissions = true,
                    appHasGrantedFitnessReadPermission = true,
                    showMedicalPastDataFooter = false,
                )
            )
        }
        launchFragment<AdditionalAccessFragment>(
                bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME)
            )
            .use { scenario ->
                onView(withText("Access exercise routes")).check(matches(isDisplayed()))
                onView(withText("Ask every time")).check(matches(isDisplayed()))
                onView(withText("Access fitness and wellness data in the background"))
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Allow this app to access this data " + "when you're not using the app"
                        )
                    )
                    .check(matches(isDisplayed()))
                    .check(matches(isDisplayed()))
                onView(withText("Access past fitness and wellness data"))
                    .check(matches(isDisplayed()))
                onView(withText("Allow this app to access data added before October 20, 2022"))
                    .check(matches(isDisplayed()))

                onView(
                        withText(
                            "Enable at least one read permission in order to turn on background or past data access for this app"
                        )
                    )
                    .check(doesNotExist())

                scenario.onActivity { activity ->
                    val fragment =
                        activity.supportFragmentManager.findFragmentById(android.R.id.content)
                            as AdditionalAccessFragment
                    val historyPreference =
                        fragment.preferenceScreen.findPreference("key_history_read")
                            as HealthSwitchPreference?
                    assertThat(historyPreference?.isChecked).isTrue()
                    assertThat(historyPreference?.isEnabled).isTrue()

                    val backgroundPreference =
                        fragment.preferenceScreen.findPreference("key_background_read")
                            as HealthSwitchPreference?
                    assertThat(backgroundPreference?.isChecked).isTrue()
                    assertThat(backgroundPreference?.isEnabled).isTrue()
                }

                verify(healthConnectLogger)
                    .logImpression(AdditionalAccessElement.EXERCISE_ROUTES_BUTTON)
                verify(healthConnectLogger)
                    .logImpression(AdditionalAccessElement.HISTORY_READ_BUTTON)
                verify(healthConnectLogger)
                    .logImpression(AdditionalAccessElement.BACKGROUND_READ_BUTTON)
            }
    }

    @Test
    fun backgroundAndHistoryReadDeclaredAndNotEnabled_fitnessAndMedical_showsFooter() {
        val state =
            State(
                exerciseRoutePermissionUIState = ASK_EVERY_TIME,
                historyReadUIState =
                    AdditionalAccessViewModel.AdditionalPermissionState(
                        isDeclared = true,
                        isEnabled = false,
                        isGranted = false,
                    ),
                backgroundReadUIState =
                    AdditionalAccessViewModel.AdditionalPermissionState(
                        isDeclared = true,
                        isEnabled = false,
                        isGranted = false,
                    ),
            )
        whenever(additionalAccessViewModel.screenState).then {
            MutableLiveData(
                AdditionalAccessViewModel.ScreenState(
                    state = state,
                    appHasDeclaredMedicalPermissions = true,
                    appHasGrantedFitnessReadPermission = false,
                    showMedicalPastDataFooter = false,
                )
            )
        }
        launchFragment<AdditionalAccessFragment>(
                bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME)
            )
            .use { scenario ->
                onView(withText("Access exercise routes")).check(matches(isDisplayed()))
                onView(withText("Ask every time")).check(matches(isDisplayed()))
                onView(withText("Access all data in the background")).check(matches(isDisplayed()))
                onView(
                        withText(
                            "Allow this app to access fitness and wellness data and medical records " +
                                "when you're not using the app"
                        )
                    )
                    .check(matches(isDisplayed()))
                onView(withText("Access past fitness and wellness data"))
                    .check(matches(isDisplayed()))
                onView(withText("Allow this app to access data added before October 20, 2022"))
                    .check(matches(isDisplayed()))

                onView(withId(androidx.preference.R.id.recycler_view))
                    .perform(RecyclerViewActions.scrollToLastPosition<RecyclerView.ViewHolder>())
                onView(
                        withText(
                            "Enable at least one read permission in order to turn on background or past data access for this app"
                        )
                    )
                    .perform(scrollTo())
                    .check(matches(isDisplayed()))

                scenario.onActivity { activity ->
                    val fragment =
                        activity.supportFragmentManager.findFragmentById(android.R.id.content)
                            as AdditionalAccessFragment
                    val historyPreference =
                        fragment.preferenceScreen.findPreference("key_history_read")
                            as HealthSwitchPreference?
                    assertThat(historyPreference?.isChecked).isFalse()
                    assertThat(historyPreference?.isEnabled).isFalse()

                    val backgroundPreference =
                        fragment.preferenceScreen.findPreference("key_background_read")
                            as HealthSwitchPreference?
                    assertThat(backgroundPreference?.isChecked).isFalse()
                    assertThat(backgroundPreference?.isEnabled).isFalse()
                }
            }
    }

    @Test
    fun whenMedicalReadGranted_andHistoryDeclared_showsInformationFooter() {
        val state =
            State(
                exerciseRoutePermissionUIState = ASK_EVERY_TIME,
                historyReadUIState =
                    AdditionalAccessViewModel.AdditionalPermissionState(
                        isDeclared = true,
                        isEnabled = true,
                        isGranted = false,
                    ),
                backgroundReadUIState =
                    AdditionalAccessViewModel.AdditionalPermissionState(
                        isDeclared = true,
                        isEnabled = true,
                        isGranted = true,
                    ),
            )
        whenever(additionalAccessViewModel.screenState).then {
            MutableLiveData(
                AdditionalAccessViewModel.ScreenState(
                    state = state,
                    appHasDeclaredMedicalPermissions = true,
                    appHasGrantedFitnessReadPermission = true,
                    showMedicalPastDataFooter = true,
                )
            )
        }
        launchFragment<AdditionalAccessFragment>(
                bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME)
            )
            .use {
                onView(withText("Access exercise routes")).check(matches(isDisplayed()))
                onView(withText("Ask every time")).check(matches(isDisplayed()))
                onView(withText("Access all data in the background")).check(matches(isDisplayed()))
                onView(
                        withText(
                            "Allow this app to access fitness and wellness data and medical records " +
                                "when you're not using the app"
                        )
                    )
                    .check(matches(isDisplayed()))
                    .check(matches(isDisplayed()))
                onView(withText("Access past fitness and wellness data"))
                    .check(matches(isDisplayed()))
                onView(withText("Allow this app to access data added before October 20, 2022"))
                    .check(matches(isDisplayed()))

                onView(
                        withText(
                            "Enable at least one read permission in order to turn on background or past data access for this app"
                        )
                    )
                    .check(doesNotExist())
                onView(
                        withText(
                            "$TEST_APP_NAME can already access past data for your medical records. " +
                                "To change this, turn off medical record permissions for this app"
                        )
                    )
                    .check(matches(isDisplayed()))
            }
    }

    @Test
    fun whenMedicalReadGranted_andFitnessReadNotGranted_andHistoryGranted_showsWarning() {
        val state =
            State(
                exerciseRoutePermissionUIState = ASK_EVERY_TIME,
                historyReadUIState =
                    AdditionalAccessViewModel.AdditionalPermissionState(
                        isDeclared = true,
                        isEnabled = true,
                        isGranted = true,
                    ),
                backgroundReadUIState =
                    AdditionalAccessViewModel.AdditionalPermissionState(
                        isDeclared = true,
                        isEnabled = true,
                        isGranted = true,
                    ),
            )
        whenever(additionalAccessViewModel.screenState).then {
            MutableLiveData(
                AdditionalAccessViewModel.ScreenState(
                    state = state,
                    appHasDeclaredMedicalPermissions = true,
                    appHasGrantedFitnessReadPermission = false,
                    showMedicalPastDataFooter = true,
                )
            )
        }
        launchFragment<AdditionalAccessFragment>(
                bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME)
            )
            .use { scenario ->
                onView(withText("Access exercise routes")).check(matches(isDisplayed()))
                onView(withText("Ask every time")).check(matches(isDisplayed()))
                onView(withText("Access medical records in the background"))
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "Allow this app to access this data " + "when you're not using the app"
                        )
                    )
                    .check(matches(isDisplayed()))
                    .check(matches(isDisplayed()))
                onView(withText("Access past fitness and wellness data"))
                    .check(matches(isDisplayed()))
                onView(withText("Allow this app to access data added before October 20, 2022"))
                    .check(matches(isDisplayed()))
                onView(
                        withText(
                            "No fitness data is currently being read because $TEST_APP_NAME has no read permissions on"
                        )
                    )
                    .check(matches(isDisplayed()))

                onView(
                        withText(
                            "Enable at least one read permission in order to turn on background or past data access for this app"
                        )
                    )
                    .check(doesNotExist())

                scenario.onActivity { activity ->
                    val fragment =
                        activity.supportFragmentManager.findFragmentById(android.R.id.content)
                            as AdditionalAccessFragment
                    val historyPreference =
                        fragment.preferenceScreen.findPreference("key_history_read")
                            as HealthSwitchPreference?
                    assertThat(historyPreference?.isChecked).isTrue()
                    assertThat(historyPreference?.isEnabled).isTrue()

                    val backgroundPreference =
                        fragment.preferenceScreen.findPreference("key_background_read")
                            as HealthSwitchPreference?
                    assertThat(backgroundPreference?.isChecked).isTrue()
                    assertThat(backgroundPreference?.isEnabled).isTrue()
                }
            }
    }

    @Test
    fun clickExerciseRoute_opensDialog() {
        val exerciseRouteDeclaredState = State(exerciseRoutePermissionUIState = ASK_EVERY_TIME)
        whenever(additionalAccessViewModel.screenState).then {
            MutableLiveData(
                AdditionalAccessViewModel.ScreenState(state = exerciseRouteDeclaredState)
            )
        }
        whenever(additionalAccessViewModel.additionalAccessState).then {
            MutableLiveData(exerciseRouteDeclaredState)
        }

        launchFragment<AdditionalAccessFragment>(
                bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME)
            )
            .use {
                onView(withText(R.string.route_permissions_label)).perform(click())
                onIdle()

                onView(withId(R.id.exercise_routes_permission_dialog))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()))
                verify(healthConnectLogger)
                    .logInteraction(AdditionalAccessElement.EXERCISE_ROUTES_BUTTON)
                verify(healthConnectLogger)
                    .logImpression(AdditionalAccessElement.EXERCISE_ROUTES_DIALOG_CONTAINER)
                verify(healthConnectLogger)
                    .logImpression(AdditionalAccessElement.EXERCISE_ROUTES_DIALOG_DENY_BUTTON)
                verify(healthConnectLogger)
                    .logImpression(AdditionalAccessElement.EXERCISE_ROUTES_ASK_BUTTON)
                verify(healthConnectLogger)
                    .logImpression(AdditionalAccessElement.EXERCISE_ROUTES_ALLOW_ALL_BUTTON)
            }
    }

    @Test
    fun onShowEnableExerciseEvent_true_opensEnableExercisePermissionDialog() {
        val event = EnableExerciseDialogEvent(shouldShowDialog = true, appName = TEST_APP_NAME)
        whenever(additionalAccessViewModel.showEnableExerciseEvent)
            .thenReturn(MediatorLiveData(event))

        launchFragment<AdditionalAccessFragment>(
                bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME)
            )
            .use {
                onIdle()

                onView(withText(R.string.exercise_permission_dialog_enable_title))
                    .inRoot(RootMatchers.isDialog())
                    .check(matches(isDisplayed()))
            }
    }

    @Test
    fun onShowEnableExerciseEvent_false_doesNotShowEnableExercisePermissionDialog() {
        val event = EnableExerciseDialogEvent(shouldShowDialog = false, appName = TEST_APP_NAME)
        whenever(additionalAccessViewModel.showEnableExerciseEvent)
            .thenReturn(MediatorLiveData(event))

        launchFragment<AdditionalAccessFragment>(
                bundleOf(EXTRA_PACKAGE_NAME to TEST_APP_PACKAGE_NAME)
            )
            .use {
                onIdle()

                onView(withText(R.string.exercise_permission_dialog_enable_title))
                    .check(doesNotExist())
            }
    }
}
