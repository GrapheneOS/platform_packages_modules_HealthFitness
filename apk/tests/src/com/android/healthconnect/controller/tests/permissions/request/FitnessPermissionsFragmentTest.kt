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
package com.android.healthconnect.controller.tests.permissions.request

import android.health.connect.HealthDataCategory
import android.health.connect.HealthPermissions.READ_DISTANCE
import android.health.connect.HealthPermissions.READ_MINDFULNESS
import android.health.connect.HealthPermissions.READ_SLEEP
import android.health.connect.HealthPermissions.READ_STEPS
import android.health.connect.HealthPermissions.WRITE_HEART_RATE
import android.health.connect.HealthPermissions.WRITE_HYDRATION
import android.os.Build
import android.os.Bundle
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onIdle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.FitnessPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.FitnessPermission.Companion.fromPermissionString
import com.android.healthconnect.controller.permissions.data.PermissionsAccessType
import com.android.healthconnect.controller.permissions.request.FitnessPermissionsFragment
import com.android.healthconnect.controller.permissions.request.FitnessScreenState
import com.android.healthconnect.controller.permissions.request.PermissionGroupKey
import com.android.healthconnect.controller.permissions.request.PermissionsFragment
import com.android.healthconnect.controller.permissions.request.RequestPermissionViewModel
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.shared.preference.HealthMainSwitchPreference
import com.android.healthconnect.controller.shared.preference.HealthToggleExpandablePreference
import com.android.healthconnect.controller.tests.TestActivity
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.any
import com.android.healthconnect.controller.tests.utils.clickOnRecyclerViewItemWithText
import com.android.healthconnect.controller.tests.utils.clickSwitchOnRecyclerViewItemWithText
import com.android.healthconnect.controller.tests.utils.di.FakeDeviceInfoUtils
import com.android.healthconnect.controller.tests.utils.launchFragment
import com.android.healthconnect.controller.tests.utils.scrollToText
import com.android.healthconnect.controller.tests.utils.scrollToTextAndClick
import com.android.healthconnect.controller.tests.utils.setLocale
import com.android.healthconnect.controller.utils.DeviceInfoUtils
import com.android.healthconnect.controller.utils.DeviceInfoUtilsModule
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.PageName
import com.android.healthconnect.controller.utils.logging.PermissionsElement
import com.android.healthconnect.controller.utils.logging.UIAction
import com.android.healthfitness.flags.Flags
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.atLeast
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@UninstallModules(DeviceInfoUtilsModule::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class FitnessPermissionsFragmentTest {
    @get:Rule val hiltRule = HiltAndroidRule(this)

    @get:Rule val setFlagsRule = SetFlagsRule()

    @BindValue
    val viewModel: RequestPermissionViewModel = mock(RequestPermissionViewModel::class.java)

    @BindValue val healthConnectLogger: HealthConnectLogger = mock(HealthConnectLogger::class.java)

    @BindValue val deviceInfoUtils: DeviceInfoUtils = FakeDeviceInfoUtils()

    private lateinit var appMetadata: AppMetadata
    private lateinit var fitnessReadPermissions: List<FitnessPermission>
    private lateinit var fitnessWritePermissions: List<FitnessPermission>
    private lateinit var fitnessReadWritePermissions: List<FitnessPermission>
    private lateinit var mockExpandedPreferences: LiveData<Set<String>>

    @Before
    fun setup() {
        hiltRule.inject()
        val context = getInstrumentation().context
        context.setLocale(Locale.US)
        appMetadata =
            AppMetadata(
                TEST_APP_PACKAGE_NAME,
                TEST_APP_NAME,
                context.getDrawable(R.drawable.health_connect_logo),
            )
        fitnessReadPermissions =
            listOf(fromPermissionString(READ_STEPS), fromPermissionString(READ_SLEEP))
        fitnessWritePermissions =
            listOf(fromPermissionString(WRITE_HEART_RATE), fromPermissionString(WRITE_HYDRATION))
        fitnessReadWritePermissions = fitnessReadPermissions + fitnessWritePermissions
        mockExpandedPreferences = MutableLiveData(emptySet<String>())
        whenever(viewModel.allFitnessPermissionsGranted).then { MutableLiveData(false) }
        whenever(viewModel.grantedFitnessPermissions).then {
            MutableLiveData(emptySet<FitnessPermission>())
        }
        whenever(viewModel.fitnessScreenState).then {
            MutableLiveData(FitnessScreenState.NoFitnessData)
        }
        // Expand Activity category by default
        whenever(viewModel.expandedDataCategoryPreferenceKeys).then {
            MutableLiveData(
                setOf(
                    PermissionGroupKey(PermissionsAccessType.READ, HealthDataCategory.ACTIVITY)
                        .toString()
                )
            )
        }
    }

    @After
    fun teardown() {
        reset(healthConnectLogger)
    }

    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @Test
    fun fitnessReadAndWrite_noMedical_noHistory_displaysCategories_healthConnectBrand() {
        whenever(viewModel.fitnessScreenState).then {
            MutableLiveData(
                FitnessScreenState.ShowFitnessReadWrite(
                    historyGranted = false,
                    hasMedical = false,
                    appMetadata = appMetadata,
                    fitnessPermissions = fitnessReadWritePermissions,
                )
            )
        }
        launchFragment<FitnessPermissionsFragment>(Bundle()).use {
            onView(withText("Allow $TEST_APP_NAME to access Health Connect?"))
                .check(matches(isDisplayed()))
            onView(withText("Choose data you want this app to read or write to Health Connect"))
                .check(matches(isDisplayed()))
            onView(
                    withText(
                        "If you give read access, the app can read new data and data from the past 30 days"
                    )
                )
                .check(matches(isDisplayed()))
            onView(
                    withText(
                        "You can learn how $TEST_APP_NAME handles your data in their privacy policy"
                    )
                )
                .check(matches(isDisplayed()))

            onView(withId(androidx.preference.R.id.recycler_view))
                .perform(
                    RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                        hasDescendant(withText("Allow \u201C$TEST_APP_NAME\u201D to read"))
                    )
                )
            Espresso.onIdle()
            onView(withText("Allow \u201C$TEST_APP_NAME\u201D to read"))
                .check(matches(isDisplayed()))

            onView(withId(androidx.preference.R.id.recycler_view))
                .perform(
                    RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                        hasDescendant(withText("Allow \u201C$TEST_APP_NAME\u201D to write"))
                    )
                )
            Espresso.onIdle()
            onView(withText("Allow \u201C$TEST_APP_NAME\u201D to write"))
                .check(matches(isDisplayed()))

            verify(healthConnectLogger, atLeast(1)).setPageId(PageName.REQUEST_PERMISSIONS_PAGE)
            verify(healthConnectLogger).logPageImpression()
            verify(healthConnectLogger, times(4))
                .logImpression(PermissionsElement.PERMISSION_SWITCH)
            verify(healthConnectLogger).logImpression(PermissionsElement.ALLOW_ALL_SWITCH)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
    @Test
    fun fitnessReadAndWrite_noMedical_noHistory_displaysCategories_healthFitnessBrand() {
        whenever(viewModel.fitnessScreenState).then {
            MutableLiveData(
                FitnessScreenState.ShowFitnessReadWrite(
                    historyGranted = false,
                    hasMedical = false,
                    appMetadata = appMetadata,
                    fitnessPermissions = fitnessReadWritePermissions,
                )
            )
        }
        launchFragment<FitnessPermissionsFragment>(Bundle()).use {
            onView(withText("Allow $TEST_APP_NAME to access your fitness and wellness data?"))
                .check(matches(isDisplayed()))
            onView(
                    withText(
                        "Choose which fitness and wellness data this app can access. This includes data tracked and stored on this device, learn more about how your data is accessed"
                    )
                )
                .check(matches(isDisplayed()))
            onView(
                    withText(
                        "If you give read access, the app can read new data and data from the past 30 days"
                    )
                )
                .check(matches(isDisplayed()))
            onView(
                    withText(
                        "You can learn how $TEST_APP_NAME handles your data in their privacy policy"
                    )
                )
                .check(matches(isDisplayed()))

            onView(withId(androidx.preference.R.id.recycler_view))
                .perform(
                    RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                        hasDescendant(withText("Allow \u201C$TEST_APP_NAME\u201D to read"))
                    )
                )
            Espresso.onIdle()
            onView(withText("Allow \u201C$TEST_APP_NAME\u201D to read"))
                .check(matches(isDisplayed()))

            onView(withId(androidx.preference.R.id.recycler_view))
                .perform(
                    RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                        hasDescendant(withText("Allow \u201C$TEST_APP_NAME\u201D to write"))
                    )
                )
            Espresso.onIdle()
            onView(withText("Allow \u201C$TEST_APP_NAME\u201D to write"))
                .check(matches(isDisplayed()))

            verify(healthConnectLogger, atLeast(1)).setPageId(PageName.REQUEST_PERMISSIONS_PAGE)
            verify(healthConnectLogger).logPageImpression()
            verify(healthConnectLogger, times(4))
                .logImpression(PermissionsElement.PERMISSION_SWITCH)
            verify(healthConnectLogger).logImpression(PermissionsElement.ALLOW_ALL_SWITCH)
        }
    }

    // TODO: b/407072322 - Enable test clicking "learn more" link.
    // @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA)
    // @Test
    // fun fitnessReadAndWrite_clicksLearnMore_jumpsToHealthFitnessPermissionsHelpCenterPage() {
    //     whenever(viewModel.fitnessScreenState).then {
    //         MutableLiveData(
    //             FitnessScreenState.ShowFitnessReadWrite(
    //                 historyGranted = false,
    //                 hasMedical = false,
    //                 appMetadata = appMetadata,
    //                 fitnessPermissions = fitnessReadWritePermissions,
    //             )
    //         )
    //     }
    //     launchFragment<FitnessPermissionsFragment>(android.os.Bundle())
    //
    //     onView(withId(R.id.data_access_type)).check(matches(isClickable()))
    //     onView(withId(R.id.data_access_type)).perform(click())
    //
    //     assertThat(fakeDeviceInfoUtils.healthFitnessPermissionsHelpCenterInvoked).isTrue()
    // }

    @Test
    fun whenMedical_headerDisplaysCorrectTitle() {
        whenever(viewModel.fitnessScreenState).then {
            MutableLiveData(
                FitnessScreenState.ShowFitnessReadWrite(
                    historyGranted = true,
                    hasMedical = true,
                    appMetadata = appMetadata,
                    fitnessPermissions = fitnessReadWritePermissions,
                )
            )
        }
        launchFragment<FitnessPermissionsFragment>(Bundle()).use {
            onView(withText("Allow $TEST_APP_NAME to access fitness and wellness data?"))
                .check(matches(isDisplayed()))
            onView(withText("Allow $TEST_APP_NAME to access Health Connect?")).check(doesNotExist())
        }
    }

    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @Test
    fun whenNoMedical_headerDisplaysCorrectTitle_healthConnectBrand() {
        whenever(viewModel.fitnessScreenState).then {
            MutableLiveData(
                FitnessScreenState.ShowFitnessReadWrite(
                    historyGranted = true,
                    hasMedical = false,
                    appMetadata = appMetadata,
                    fitnessPermissions = fitnessReadWritePermissions,
                )
            )
        }
        launchFragment<FitnessPermissionsFragment>(Bundle()).use {
            onView(withText("Allow $TEST_APP_NAME to access your fitness and wellness data?"))
                .check(doesNotExist())
            onView(withText("Allow $TEST_APP_NAME to access Health Connect?"))
                .check(matches(isDisplayed()))
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
    @Test
    fun whenNoMedical_headerDisplaysCorrectTitle_healthFitnessBrand() {
        whenever(viewModel.fitnessScreenState).then {
            MutableLiveData(
                FitnessScreenState.ShowFitnessReadWrite(
                    historyGranted = true,
                    hasMedical = false,
                    appMetadata = appMetadata,
                    fitnessPermissions = fitnessReadWritePermissions,
                )
            )
        }
        launchFragment<FitnessPermissionsFragment>(Bundle()).use {
            onView(withText("Allow $TEST_APP_NAME to access your fitness and wellness data?"))
                .check(matches(isDisplayed()))
            onView(withText("Allow $TEST_APP_NAME to access Health Connect?")).check(doesNotExist())
        }
    }

    @Test
    fun whenHistoryReadGranted_headerDisplaysCorrectText() {
        whenever(viewModel.fitnessScreenState).then {
            MutableLiveData(
                FitnessScreenState.ShowFitnessReadWrite(
                    historyGranted = true,
                    hasMedical = true,
                    appMetadata = appMetadata,
                    fitnessPermissions = fitnessReadWritePermissions,
                )
            )
        }
        launchFragment<FitnessPermissionsFragment>(Bundle()).use {
            onView(withText("If you give read access, the app can read new and past data"))
                .check(matches(isDisplayed()))
            onView(
                    withText(
                        "If you give read access, the app can read new data and data from the past 30 days"
                    )
                )
                .check(doesNotExist())
        }
    }

    @Test
    fun whenHistoryReadNotGranted_headerDisplaysCorrectText() {
        whenever(viewModel.fitnessScreenState).then {
            MutableLiveData(
                FitnessScreenState.ShowFitnessReadWrite(
                    historyGranted = false,
                    hasMedical = true,
                    appMetadata = appMetadata,
                    fitnessPermissions = fitnessReadWritePermissions,
                )
            )
        }
        launchFragment<FitnessPermissionsFragment>(Bundle()).use {
            onView(withText("If you give read access, the app can read new and past data"))
                .check(doesNotExist())
            onView(
                    withText(
                        "If you give read access, the app can read new data and data from the past 30 days"
                    )
                )
                .check(matches(isDisplayed()))
        }
    }

    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @Test
    fun whenOnlyReadPermissionsRequested_headerDisplaysCorrectText_healthConnectBrand() {
        whenever(viewModel.fitnessScreenState).then {
            MutableLiveData(
                FitnessScreenState.ShowFitnessRead(
                    historyGranted = false,
                    hasMedical = true,
                    appMetadata = appMetadata,
                    fitnessPermissions = fitnessReadPermissions,
                )
            )
        }
        launchFragment<FitnessPermissionsFragment>(Bundle()).use {
            onView(withText("Choose data you want this app to read from Health Connect"))
                .check(matches(isDisplayed()))
            onView(
                    withText(
                        "If you give read access, the app can read new data and data from the past 30 days"
                    )
                )
                .check(matches(isDisplayed()))
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
    @Test
    fun whenOnlyReadPermissionsRequested_headerDisplaysCorrectText_healthFitnessBrand() {
        whenever(viewModel.fitnessScreenState).then {
            MutableLiveData(
                FitnessScreenState.ShowFitnessRead(
                    historyGranted = false,
                    hasMedical = true,
                    appMetadata = appMetadata,
                    fitnessPermissions = fitnessReadPermissions,
                )
            )
        }
        launchFragment<FitnessPermissionsFragment>(Bundle()).use {
            onView(
                    withText(
                        "Choose which fitness and wellness data this app can access. This includes data tracked and stored on this device, learn more about how your data is accessed"
                    )
                )
                .check(matches(isDisplayed()))
            onView(
                    withText(
                        "If you give read access, the app can read new data and data from the past 30 days"
                    )
                )
                .check(matches(isDisplayed()))
        }
    }

    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @Test
    fun whenOnlyWritePermissionsRequested_headerDisplaysCorrectText_healthConnectBrand() {
        whenever(viewModel.fitnessScreenState).then {
            MutableLiveData(
                FitnessScreenState.ShowFitnessWrite(
                    hasMedical = true,
                    appMetadata = appMetadata,
                    fitnessPermissions = fitnessWritePermissions,
                )
            )
        }
        launchFragment<FitnessPermissionsFragment>(Bundle()).use {
            onView(withText("Choose data you want this app to write to Health Connect"))
                .check(matches(isDisplayed()))
            onView(withText("If you give read access, the app can read new and past data"))
                .check(doesNotExist())
            onView(
                    withText(
                        "If you give read access, the app can read new data and data from the past 30 days"
                    )
                )
                .check(doesNotExist())
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
    @Test
    fun whenOnlyWritePermissionsRequested_headerDisplaysCorrectText_healthFitnessBrand() {
        whenever(viewModel.fitnessScreenState).then {
            MutableLiveData(
                FitnessScreenState.ShowFitnessWrite(
                    hasMedical = true,
                    appMetadata = appMetadata,
                    fitnessPermissions = fitnessWritePermissions,
                )
            )
        }
        launchFragment<FitnessPermissionsFragment>(Bundle()).use {
            onView(
                    withText(
                        "Choose which fitness and wellness data this app can access. This includes data tracked and stored on this device, learn more about how your data is accessed"
                    )
                )
                .check(matches(isDisplayed()))
            onView(withText("If you give read access, the app can read new and past data"))
                .check(doesNotExist())
            onView(
                    withText(
                        "If you give read access, the app can read new data and data from the past 30 days"
                    )
                )
                .check(doesNotExist())
        }
    }

    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @Test
    fun whenReadAndWritePermissionsRequested_headerDisplaysCorrectText_healthConnectBrand() {
        whenever(viewModel.fitnessScreenState).then {
            MutableLiveData(
                FitnessScreenState.ShowFitnessReadWrite(
                    historyGranted = false,
                    hasMedical = false,
                    appMetadata = appMetadata,
                    fitnessPermissions = fitnessReadWritePermissions,
                )
            )
        }
        launchFragment<FitnessPermissionsFragment>(Bundle()).use {
            onView(withText("Allow $TEST_APP_NAME to access Health Connect?"))
                .check(matches(isDisplayed()))
            onView(withText("Choose data you want this app to read or write to Health Connect"))
                .check(matches(isDisplayed()))
            onView(
                    withText(
                        "If you give read access, the app can read new data and data from the past 30 days"
                    )
                )
                .check(matches(isDisplayed()))
            onView(
                    withText(
                        "You can learn how $TEST_APP_NAME handles your data in their privacy policy"
                    )
                )
                .check(matches(isDisplayed()))
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
    @Test
    fun whenReadAndWritePermissionsRequested_headerDisplaysCorrectText_healthFitnessBrand() {
        whenever(viewModel.fitnessScreenState).then {
            MutableLiveData(
                FitnessScreenState.ShowFitnessReadWrite(
                    historyGranted = false,
                    hasMedical = false,
                    appMetadata = appMetadata,
                    fitnessPermissions = fitnessReadWritePermissions,
                )
            )
        }
        launchFragment<FitnessPermissionsFragment>(Bundle()).use {
            onView(withText("Allow $TEST_APP_NAME to access your fitness and wellness data?"))
                .check(matches(isDisplayed()))
            onView(
                    withText(
                        "Choose which fitness and wellness data this app can access. This includes data tracked and stored on this device, learn more about how your data is accessed"
                    )
                )
                .check(matches(isDisplayed()))
            onView(
                    withText(
                        "If you give read access, the app can read new data and data from the past 30 days"
                    )
                )
                .check(matches(isDisplayed()))
            onView(
                    withText(
                        "You can learn how $TEST_APP_NAME handles your data in their privacy policy"
                    )
                )
                .check(matches(isDisplayed()))
        }
    }

    @Test
    @DisableFlags(Flags.FLAG_PERMISSIONS_GROUPING_UI)
    fun displaysReadPermissions() {
        whenever(viewModel.fitnessScreenState).then {
            MutableLiveData(
                FitnessScreenState.ShowFitnessRead(
                    historyGranted = false,
                    hasMedical = false,
                    appMetadata = appMetadata,
                    fitnessPermissions = fitnessReadPermissions,
                )
            )
        }
        launchFragment<FitnessPermissionsFragment>(Bundle()).use {
            onView(withId(androidx.preference.R.id.recycler_view))
                .perform(
                    RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                        hasDescendant(withText("Steps"))
                    )
                )
            Espresso.onIdle()
            onView(withText("Steps")).check(matches(isDisplayed()))

            onView(withId(androidx.preference.R.id.recycler_view))
                .perform(
                    RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                        hasDescendant(withText("Sleep"))
                    )
                )
            Espresso.onIdle()
            onView(withText("Sleep")).check(matches(isDisplayed()))
        }
    }

    @Test
    @DisableFlags(Flags.FLAG_PERMISSIONS_GROUPING_UI)
    fun displaysWritePermissions() {
        whenever(viewModel.fitnessScreenState).then {
            MutableLiveData(
                FitnessScreenState.ShowFitnessWrite(
                    hasMedical = false,
                    appMetadata = appMetadata,
                    fitnessPermissions = fitnessWritePermissions,
                )
            )
        }
        launchFragment<FitnessPermissionsFragment>(Bundle()).use {
            onView(withId(androidx.preference.R.id.recycler_view))
                .perform(
                    RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                        hasDescendant(withText("Heart rate"))
                    )
                )
            Espresso.onIdle()
            onView(withText("Heart rate")).check(matches(isDisplayed()))

            onView(withId(androidx.preference.R.id.recycler_view))
                .perform(
                    RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                        hasDescendant(withText("Hydration"))
                    )
                )
            Espresso.onIdle()
            onView(withText("Hydration")).check(matches(isDisplayed()))
        }
    }

    @Test
    @DisableFlags(Flags.FLAG_PERMISSIONS_GROUPING_UI)
    fun whenPermissionSwitchIsOn_forReadWrite_correctContentDescriptionIsDisplayed() {
        whenever(viewModel.fitnessScreenState).then {
            MutableLiveData(
                FitnessScreenState.ShowFitnessReadWrite(
                    hasMedical = false,
                    appMetadata = appMetadata,
                    fitnessPermissions = fitnessReadWritePermissions,
                    historyGranted = false,
                )
            )
        }

        launchFragment<FitnessPermissionsFragment>(Bundle()).use {
            onView(withId(androidx.preference.R.id.recycler_view))
                .perform(
                    RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                        hasDescendant(withText("Hydration"))
                    )
                )
            Espresso.onIdle()
            onView(withText("Hydration")).perform(click())
            Espresso.onIdle()
            onView(withContentDescription("Hydration. Write Access. On"))
                .check(matches(isDisplayed()))

            onView(withId(androidx.preference.R.id.recycler_view))
                .perform(
                    RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                        hasDescendant(withText("Sleep"))
                    )
                )
            Espresso.onIdle()
            onView(withText("Sleep")).perform(click())
            Espresso.onIdle()
            onView(withContentDescription("Sleep. Read Access. On")).check(matches(isDisplayed()))
        }
    }

    @Test
    @DisableFlags(Flags.FLAG_PERMISSIONS_GROUPING_UI)
    fun whenPermissionSwitchIsOff_forReadWrite_correctContentDescriptionIsDisplayed() {
        whenever(viewModel.fitnessScreenState).then {
            MutableLiveData(
                FitnessScreenState.ShowFitnessWrite(
                    hasMedical = false,
                    appMetadata = appMetadata,
                    fitnessPermissions = fitnessReadWritePermissions,
                )
            )
        }

        launchFragment<FitnessPermissionsFragment>(Bundle()).use {
            onView(withId(androidx.preference.R.id.recycler_view))
                .perform(
                    RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                        hasDescendant(withText("Hydration"))
                    )
                )
            Espresso.onIdle()
            onView(withContentDescription("Hydration. Write Access. Off"))
                .check(matches(isDisplayed()))

            onView(withId(androidx.preference.R.id.recycler_view))
                .perform(
                    RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                        hasDescendant(withText("Sleep"))
                    )
                )
            Espresso.onIdle()
            onView(withContentDescription("Sleep. Read Access. Off")).check(matches(isDisplayed()))
        }
    }

    @Test
    @DisableFlags(Flags.FLAG_PERMISSIONS_GROUPING_UI)
    fun togglesPermissions_callsUpdatePermissions() {
        whenever(viewModel.fitnessScreenState).then {
            MutableLiveData(
                FitnessScreenState.ShowFitnessReadWrite(
                    historyGranted = false,
                    hasMedical = false,
                    appMetadata = appMetadata,
                    fitnessPermissions = fitnessReadWritePermissions,
                )
            )
        }
        launchFragment<FitnessPermissionsFragment>(Bundle()).use {
            onView(withId(androidx.preference.R.id.recycler_view))
                .perform(
                    RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                        hasDescendant(withText("Sleep"))
                    )
                )
            Espresso.onIdle()
            onView(withText("Sleep")).perform(click())

            verify(viewModel).updateHealthPermission(any(FitnessPermission::class.java), eq(true))
            verify(healthConnectLogger)
                .logInteraction(PermissionsElement.PERMISSION_SWITCH, UIAction.ACTION_TOGGLE_ON)
        }
    }

    @Test
    fun allowAllToggleOn_updatesAllPermissions() {
        whenever(viewModel.fitnessScreenState).then {
            MutableLiveData(
                FitnessScreenState.ShowFitnessReadWrite(
                    historyGranted = false,
                    hasMedical = false,
                    appMetadata = appMetadata,
                    fitnessPermissions = fitnessReadWritePermissions,
                )
            )
        }
        launchFragment<FitnessPermissionsFragment>(Bundle()).use { activityScenario ->
            onView(withId(androidx.preference.R.id.recycler_view))
                .perform(
                    RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                        hasDescendant(withText("Allow all"))
                    )
                )
            var allowAllPreference: HealthMainSwitchPreference? = null
            activityScenario.onActivity { activity: TestActivity ->
                val fragment =
                    activity.supportFragmentManager.findFragmentById(android.R.id.content)
                        as PermissionsFragment
                allowAllPreference =
                    fragment.preferenceScreen.findPreference("allow_all_preference")
                allowAllPreference?.isChecked =
                    false // makes sure the preference is on so OnPreferenceChecked is triggered
            }

            onView(withText(allowAllPreference?.title?.toString())).perform(click())

            verify(viewModel).updateFitnessPermissions(eq(true))
            verify(healthConnectLogger)
                .logInteraction(PermissionsElement.ALLOW_ALL_SWITCH, UIAction.ACTION_TOGGLE_ON)
        }
    }

    @Test
    fun allowAllToggleOff_updatesAllPermissions() {
        whenever(viewModel.fitnessScreenState).then {
            MutableLiveData(
                FitnessScreenState.ShowFitnessReadWrite(
                    historyGranted = false,
                    hasMedical = false,
                    appMetadata = appMetadata,
                    fitnessPermissions = fitnessReadWritePermissions,
                )
            )
        }
        launchFragment<FitnessPermissionsFragment>(Bundle()).use { activityScenario ->
            onView(withId(androidx.preference.R.id.recycler_view))
                .perform(
                    RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                        hasDescendant(withText("Allow all"))
                    )
                )
            var allowAllPreference: HealthMainSwitchPreference? = null
            activityScenario.onActivity { activity: TestActivity ->
                val fragment =
                    activity.supportFragmentManager.findFragmentById(android.R.id.content)
                        as PermissionsFragment
                allowAllPreference =
                    fragment.preferenceScreen.findPreference("allow_all_preference")
                allowAllPreference?.isChecked =
                    true // makes sure the preference is on so OnPreferenceChecked is triggered
            }

            onView(withText(allowAllPreference?.title?.toString())).perform(click())

            assertThat(viewModel.grantedFitnessPermissions.value).isEmpty()
            verify(healthConnectLogger)
                .logInteraction(PermissionsElement.ALLOW_ALL_SWITCH, UIAction.ACTION_TOGGLE_OFF)
        }
    }

    @Test
    fun allowButton_noFitnessPermissionsSelected_isDisabled() {
        whenever(viewModel.fitnessScreenState).then {
            MutableLiveData(
                FitnessScreenState.ShowFitnessReadWrite(
                    historyGranted = false,
                    hasMedical = false,
                    appMetadata = appMetadata,
                    fitnessPermissions = fitnessReadWritePermissions,
                )
            )
        }
        whenever(viewModel.grantedFitnessPermissions).then {
            MutableLiveData(emptySet<FitnessPermission>())
        }

        launchFragment<FitnessPermissionsFragment>(Bundle()).use {
            onView(withText("Allow")).check(matches(ViewMatchers.isNotEnabled()))
        }
    }

    @Test
    fun allowButton_fitnessPermissionsSelected_isEnabled() {
        whenever(viewModel.fitnessScreenState).then {
            MutableLiveData(
                FitnessScreenState.ShowFitnessReadWrite(
                    historyGranted = false,
                    hasMedical = false,
                    appMetadata = appMetadata,
                    fitnessPermissions = fitnessReadWritePermissions,
                )
            )
        }
        whenever(viewModel.grantedFitnessPermissions).then {
            MutableLiveData(setOf(HealthPermission.fromPermissionString(READ_STEPS)))
        }

        launchFragment<FitnessPermissionsFragment>(Bundle()).use {
            onView(withText("Allow")).check(matches(ViewMatchers.isEnabled()))
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_PERMISSIONS_GROUPING_UI)
    fun displaysGroupedPermissions_firstIsGroupExpanded_whenFlagEnabled() {
        whenever(viewModel.fitnessScreenState).then {
            MutableLiveData(
                FitnessScreenState.ShowFitnessReadWrite(
                    historyGranted = false,
                    hasMedical = false,
                    appMetadata = appMetadata,
                    fitnessPermissions =
                        listOf(
                            fromPermissionString(READ_STEPS),
                            fromPermissionString(READ_MINDFULNESS),
                            fromPermissionString(WRITE_HEART_RATE),
                            fromPermissionString(WRITE_HYDRATION),
                        ),
                )
            )
        }
        launchFragment<FitnessPermissionsFragment>(Bundle()).use { scenario ->
            onIdle()
            // Sorted order is Activity, Sleep for read.
            // So Activity should be expanded.
            onView(withId(androidx.preference.R.id.recycler_view))
                .perform(
                    RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                        hasDescendant(withText("Steps"))
                    )
                )
            onView(withText("Steps")).check(matches(isDisplayed()))

            lateinit var expandablePreference: HealthToggleExpandablePreference
            scenario.onActivity { activity ->
                val fragment =
                    activity.supportFragmentManager.findFragmentById(android.R.id.content)
                        as FitnessPermissionsFragment
                expandablePreference =
                    fragment.preferenceScreen.findPreference(
                        PermissionGroupKey(PermissionsAccessType.READ, HealthDataCategory.ACTIVITY)
                            .toString()
                    )!!
            }
            assertThat(expandablePreference.mIsExpanded).isTrue()

            // Now expand Wellness category
            scrollToTextAndClick("Wellness")
            scrollToText("Mindfulness")
            onView(withText("Mindfulness")).check(matches(isDisplayed()))

            // Now expand Vitals category (write permissions)
            scrollToTextAndClick("Vitals")
            scrollToText("Heart rate")
            onView(withText("Heart rate")).check(matches(isDisplayed()))

            // Now expand Nutrition category
            scrollToTextAndClick("Nutrition")
            scrollToText("Hydration")
            onView(withText("Hydration")).check(matches(isDisplayed()))
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_PERMISSIONS_GROUPING_UI)
    fun togglePermissionInCategory_updatesViewModel_whenFlagEnabled() {
        val stepsPermission = fromPermissionString(READ_STEPS)
        whenever(viewModel.fitnessScreenState).then {
            MutableLiveData(
                FitnessScreenState.ShowFitnessReadWrite(
                    historyGranted = false,
                    hasMedical = false,
                    appMetadata = appMetadata,
                    fitnessPermissions = fitnessReadWritePermissions,
                )
            )
        }
        // Expand Activity category to see "Steps" permission
        whenever(viewModel.expandedDataCategoryPreferenceKeys).then {
            MutableLiveData(
                setOf(
                    PermissionGroupKey(PermissionsAccessType.READ, HealthDataCategory.ACTIVITY)
                        .toString()
                )
            )
        }

        launchFragment<FitnessPermissionsFragment>(Bundle()).use {
            onIdle()
            scrollToTextAndClick("Steps")

            verify(viewModel).updateHealthPermission(stepsPermission, true)
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_PERMISSIONS_GROUPING_UI)
    fun permissionGrouping_correctlyDisplaysGrantedPermissionCount_partialPermissionsGranted() {
        val stepsPermission = fromPermissionString(READ_STEPS)
        val distancePermission = fromPermissionString(READ_DISTANCE)
        whenever(viewModel.fitnessScreenState).then {
            MutableLiveData(
                FitnessScreenState.ShowFitnessReadWrite(
                    historyGranted = false,
                    hasMedical = false,
                    appMetadata = appMetadata,
                    fitnessPermissions = listOf(stepsPermission, distancePermission),
                )
            )
        }

        whenever(viewModel.expandedDataCategoryPreferenceKeys).then {
            MutableLiveData(
                setOf(
                    PermissionGroupKey(PermissionsAccessType.READ, HealthDataCategory.ACTIVITY)
                        .toString()
                )
            )
        }

        whenever(viewModel.grantedFitnessPermissions).then {
            MutableLiveData(setOf(stepsPermission))
        }

        launchFragment<FitnessPermissionsFragment>(Bundle()).use {
            scrollToText("1 of 2 selected")
            onView(withText("1 of 2 selected")).check(matches(isDisplayed()))
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_PERMISSIONS_GROUPING_UI)
    fun permissionGrouping_correctlyDisplaysGrantedPermissionCount_allPermissionsGranted() {
        val stepsPermission = fromPermissionString(READ_STEPS)
        val distancePermission = fromPermissionString(READ_DISTANCE)
        whenever(viewModel.fitnessScreenState).then {
            MutableLiveData(
                FitnessScreenState.ShowFitnessReadWrite(
                    historyGranted = false,
                    hasMedical = false,
                    appMetadata = appMetadata,
                    fitnessPermissions = listOf(stepsPermission, distancePermission),
                )
            )
        }

        whenever(viewModel.expandedDataCategoryPreferenceKeys).then {
            MutableLiveData(
                setOf(
                    PermissionGroupKey(PermissionsAccessType.READ, HealthDataCategory.ACTIVITY)
                        .toString()
                )
            )
        }

        whenever(viewModel.grantedFitnessPermissions).then {
            MutableLiveData(setOf(stepsPermission, distancePermission))
        }

        launchFragment<FitnessPermissionsFragment>(Bundle()).use {
            scrollToText("2 of 2 selected")
            onView(withText("2 of 2 selected")).check(matches(isDisplayed()))
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_PERMISSIONS_GROUPING_UI)
    fun permissionGrouping_correctlyDisplaysGrantedPermissionCount_zeroPermissionsGranted() {
        val stepsPermission = fromPermissionString(READ_STEPS)
        val distancePermission = fromPermissionString(READ_DISTANCE)
        whenever(viewModel.fitnessScreenState).then {
            MutableLiveData(
                FitnessScreenState.ShowFitnessReadWrite(
                    historyGranted = false,
                    hasMedical = false,
                    appMetadata = appMetadata,
                    fitnessPermissions = listOf(stepsPermission, distancePermission),
                )
            )
        }

        whenever(viewModel.expandedDataCategoryPreferenceKeys).then {
            MutableLiveData(
                setOf(
                    PermissionGroupKey(PermissionsAccessType.READ, HealthDataCategory.ACTIVITY)
                        .toString()
                )
            )
        }

        whenever(viewModel.grantedFitnessPermissions).then { MutableLiveData<Set<String>>() }

        launchFragment<FitnessPermissionsFragment>(Bundle()).use {
            scrollToText("0 of 2 selected")
            onView(withText("0 of 2 selected")).check(matches(isDisplayed()))
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_PERMISSIONS_GROUPING_UI)
    fun toggleCategorySwitch_updatesViewModel_whenFlagEnabled() {
        val activityPermissions = listOf(fromPermissionString(READ_STEPS))
        whenever(viewModel.fitnessScreenState).then {
            MutableLiveData(
                FitnessScreenState.ShowFitnessReadWrite(
                    historyGranted = false,
                    hasMedical = false,
                    appMetadata = appMetadata,
                    fitnessPermissions = fitnessReadWritePermissions,
                )
            )
        }

        launchFragment<FitnessPermissionsFragment>(Bundle()).use {
            onIdle()
            scrollToText("Activity")
            clickSwitchOnRecyclerViewItemWithText("Activity")

            verify(viewModel).updateHealthPermissions(activityPermissions, true)
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_PERMISSIONS_GROUPING_UI)
    fun toggleIndividualPermission_updatesParentSwitchState() {
        val stepsPermission = fromPermissionString(READ_STEPS)
        val distancePermission = fromPermissionString(READ_DISTANCE)
        val activityPermissions = listOf(stepsPermission, distancePermission)
        whenever(viewModel.fitnessScreenState).then {
            MutableLiveData(
                FitnessScreenState.ShowFitnessRead(
                    historyGranted = false,
                    hasMedical = false,
                    appMetadata = appMetadata,
                    fitnessPermissions = activityPermissions,
                )
            )
        }

        launchFragment<FitnessPermissionsFragment>(Bundle()).use { scenario ->
            onIdle()
            lateinit var expandablePreference: HealthToggleExpandablePreference
            scenario.onActivity { activity ->
                val fragment =
                    activity.supportFragmentManager.findFragmentById(android.R.id.content)
                        as FitnessPermissionsFragment
                expandablePreference =
                    fragment.preferenceScreen.findPreference(
                        PermissionGroupKey(PermissionsAccessType.READ, HealthDataCategory.ACTIVITY)
                            .toString()
                    )!!
            }
            assertThat(expandablePreference.isChecked).isFalse()

            // 1. Click "Steps" to turn it on
            scrollToTextAndClick("Steps")
            assertThat(expandablePreference.isChecked).isFalse()

            // 2. Click "Distance" to turn it on
            scrollToTextAndClick("Distance")
            assertThat(expandablePreference.mIsExpanded).isTrue()

            // 3. Click "Steps" to turn it off again
            clickOnRecyclerViewItemWithText("Steps")
            assertThat(expandablePreference.isChecked).isFalse()
        }
    }
}
