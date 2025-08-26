/*
 * Copyright (C) 2025 The Android Open Source Project
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

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.ComponentName
import android.content.Intent
import android.content.Intent.EXTRA_PACKAGE_NAME
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.PackageManager.EXTRA_REQUEST_PERMISSIONS_NAMES
import android.health.connect.HealthPermissions.READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES
import android.health.connect.HealthPermissions.READ_MEDICAL_DATA_CONDITIONS
import android.health.connect.HealthPermissions.READ_MEDICAL_DATA_LABORATORY_RESULTS
import android.health.connect.HealthPermissions.READ_MEDICAL_DATA_MEDICATIONS
import android.health.connect.HealthPermissions.READ_MEDICAL_DATA_PERSONAL_DETAILS
import android.health.connect.HealthPermissions.READ_MEDICAL_DATA_PRACTITIONER_DETAILS
import android.health.connect.HealthPermissions.READ_MEDICAL_DATA_PREGNANCY
import android.health.connect.HealthPermissions.READ_MEDICAL_DATA_PROCEDURES
import android.health.connect.HealthPermissions.READ_MEDICAL_DATA_SOCIAL_HISTORY
import android.health.connect.HealthPermissions.READ_MEDICAL_DATA_VACCINES
import android.health.connect.HealthPermissions.READ_MEDICAL_DATA_VISITS
import android.health.connect.HealthPermissions.READ_MEDICAL_DATA_VITAL_SIGNS
import android.health.connect.HealthPermissions.READ_SLEEP
import android.health.connect.HealthPermissions.READ_STEPS
import android.health.connect.HealthPermissions.WRITE_EXERCISE
import android.health.connect.HealthPermissions.WRITE_HEART_RATE
import android.health.connect.HealthPermissions.WRITE_MEDICAL_DATA
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import android.platform.test.flag.junit.SetFlagsRule
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario.launchActivityForResult
import androidx.test.espresso.Espresso.onIdle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.permissions.data.HealthPermission.AdditionalPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.FitnessPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.FitnessPermission.Companion.fromPermissionString
import com.android.healthconnect.controller.permissions.data.HealthPermission.MedicalPermission
import com.android.healthconnect.controller.permissions.request.AdditionalScreenState
import com.android.healthconnect.controller.permissions.request.FitnessScreenState
import com.android.healthconnect.controller.permissions.request.MedicalScreenState
import com.android.healthconnect.controller.permissions.request.PermissionsActivity
import com.android.healthconnect.controller.permissions.request.PermissionsActivityState
import com.android.healthconnect.controller.permissions.request.PermissionsBottomSheetDialogFragment
import com.android.healthconnect.controller.permissions.request.RequestPermissionViewModel
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.tests.utils.NOW
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.di.FakeDeviceInfoUtils
import com.android.healthconnect.controller.tests.utils.launchFragment
import com.android.healthconnect.controller.tests.utils.setLocale
import com.android.healthconnect.controller.utils.DeviceInfoUtils
import com.android.healthconnect.controller.utils.DeviceInfoUtilsModule
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
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
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@UninstallModules(DeviceInfoUtilsModule::class)
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class PermissionsBottomSheetDialogFragmentTest {
    @get:Rule val hiltRule = HiltAndroidRule(this)
    @get:Rule val checkFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()
    @get:Rule val setFlagsRule = SetFlagsRule()

    @BindValue val viewModel: RequestPermissionViewModel = mock<RequestPermissionViewModel>()
    @BindValue val healthConnectLogger: HealthConnectLogger = mock<HealthConnectLogger>()
    @BindValue val deviceInfoUtils: DeviceInfoUtils = FakeDeviceInfoUtils()

    private lateinit var appMetadata: AppMetadata
    private lateinit var fitnessReadPermissions: List<FitnessPermission>
    private lateinit var fitnessWritePermissions: List<FitnessPermission>
    private lateinit var fitnessReadWritePermissions: List<FitnessPermission>
    private lateinit var allMedicalReadPermissions: List<MedicalPermission>
    private lateinit var allMedicalWritePermissions: List<MedicalPermission>
    private lateinit var allMedicalPermissions: List<MedicalPermission>

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
            listOf(fromPermissionString(WRITE_HEART_RATE), fromPermissionString(WRITE_EXERCISE))
        fitnessReadWritePermissions = fitnessReadPermissions + fitnessWritePermissions
        allMedicalReadPermissions =
            listOf(
                MedicalPermission.Companion.fromPermissionString(
                    READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES
                ),
                MedicalPermission.Companion.fromPermissionString(READ_MEDICAL_DATA_CONDITIONS),
                MedicalPermission.Companion.fromPermissionString(
                    READ_MEDICAL_DATA_LABORATORY_RESULTS
                ),
                MedicalPermission.Companion.fromPermissionString(READ_MEDICAL_DATA_MEDICATIONS),
                MedicalPermission.Companion.fromPermissionString(
                    READ_MEDICAL_DATA_PERSONAL_DETAILS
                ),
                MedicalPermission.Companion.fromPermissionString(
                    READ_MEDICAL_DATA_PRACTITIONER_DETAILS
                ),
                MedicalPermission.Companion.fromPermissionString(READ_MEDICAL_DATA_PREGNANCY),
                MedicalPermission.Companion.fromPermissionString(READ_MEDICAL_DATA_PROCEDURES),
                MedicalPermission.Companion.fromPermissionString(READ_MEDICAL_DATA_SOCIAL_HISTORY),
                MedicalPermission.Companion.fromPermissionString(READ_MEDICAL_DATA_VACCINES),
                MedicalPermission.Companion.fromPermissionString(READ_MEDICAL_DATA_VISITS),
                MedicalPermission.Companion.fromPermissionString(READ_MEDICAL_DATA_VITAL_SIGNS),
            )
        allMedicalWritePermissions =
            listOf(MedicalPermission.Companion.fromPermissionString(WRITE_MEDICAL_DATA))
        allMedicalPermissions = allMedicalReadPermissions + allMedicalWritePermissions

        whenever(viewModel.allFitnessPermissionsGranted).then { MutableLiveData(false) }
        whenever(viewModel.grantedFitnessPermissions).then {
            MutableLiveData(emptySet<FitnessPermission>())
        }
        whenever(viewModel.fitnessScreenState).then {
            MutableLiveData(FitnessScreenState.NoFitnessData)
        }
    }

    @After
    fun teardown() {
        reset(healthConnectLogger)
    }

    @Test
    fun fitnessPermissionsRequested_permissionsActivityStateIsShowFitness_displaysReadPermissions() {
        whenever(viewModel.permissionsActivityState).then {
            MutableLiveData(PermissionsActivityState.ShowFitness)
        }
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

        launchFragment<PermissionsBottomSheetDialogFragment>(bundleOf())

        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(
                RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                    hasDescendant(withText("Steps"))
                )
            )
        onIdle()
        onView(withText("Steps")).check(matches(isDisplayed()))

        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(
                RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                    hasDescendant(withText("Sleep"))
                )
            )
        onIdle()
        onView(withText("Sleep")).check(matches(isDisplayed()))
    }

    @Test
    fun fitnessPermissionsRequested_permissionsActivityStateIsShowFitness_displaysWritePermissions() {
        whenever(viewModel.permissionsActivityState).then {
            MutableLiveData(PermissionsActivityState.ShowFitness)
        }
        whenever(viewModel.fitnessScreenState).then {
            MutableLiveData(
                FitnessScreenState.ShowFitnessWrite(
                    hasMedical = false,
                    appMetadata = appMetadata,
                    fitnessPermissions = fitnessWritePermissions,
                )
            )
        }

        launchFragment<PermissionsBottomSheetDialogFragment>(bundleOf())

        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(
                RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                    hasDescendant(withText("Heart rate"))
                )
            )
        onIdle()
        onView(withText("Heart rate")).check(matches(isDisplayed()))

        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(
                RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                    hasDescendant(withText("Exercise"))
                )
            )
        onIdle()
        onView(withText("Exercise")).check(matches(isDisplayed()))
    }

    @Test
    fun additionalPermissionsRequested_medicalGranted_fitnessGranted_permissionsActivityStateIsShowAdditional_additionalPermissionsAreDisplayed() {
        whenever(viewModel.permissionsActivityState).then {
            MutableLiveData(PermissionsActivityState.ShowAdditional(false))
        }
        whenever(viewModel.grantedAdditionalPermissions).then {
            MutableLiveData(emptySet<AdditionalPermission>())
        }
        whenever(viewModel.additionalScreenState).then {
            MutableLiveData(
                AdditionalScreenState.ShowCombined(
                    appMetadata = appMetadata,
                    hasMedical = true,
                    isMedicalReadGranted = true,
                    isFitnessReadGranted = true,
                    dataAccessDate = NOW,
                )
            )
        }

        launchFragment<PermissionsBottomSheetDialogFragment>(bundleOf())

        onView(withText("Allow additional access for $TEST_APP_NAME?"))
            .check(matches(isDisplayed()))
        onView(withText("$TEST_APP_NAME also wants to access these Health Connect settings"))
            .check(matches(isDisplayed()))

        onView(withText("Access past fitness and wellness data")).check(matches(isDisplayed()))
        onView(withText("Allow this app to access data added before October 20, 2022"))
            .check(matches(isDisplayed()))

        onView(withText("Access all data in the background")).check(matches(isDisplayed()))
        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(
                RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                    hasDescendant(
                        withText(
                            "Allow this app to access fitness and wellness data and health records when you're not using the app"
                        )
                    )
                )
            )
        onView(
                withText(
                    "Allow this app to access fitness and wellness data and health records when you're not using the app"
                )
            )
            .check(matches(isDisplayed()))
        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(RecyclerViewActions.scrollToLastPosition<RecyclerView.ViewHolder>())
        onIdle()
        onView(withText("$TEST_APP_NAME can already access past data for your health records"))
            .check(matches(isDisplayed()))

        onView(withText("Don't allow")).check(matches(isDisplayed()))
        onView(withText("Allow")).check(matches(isDisplayed()))
    }

    @Test
    fun medicalReadAndWritePermissionsRequested_permissionsActivityStateIsShowMedical_displaysMedicalCategoriesAndPermissions() {
        whenever(viewModel.permissionsActivityState).then {
            MutableLiveData(PermissionsActivityState.ShowMedical(false))
        }
        whenever(viewModel.allMedicalPermissionsGranted).then { MutableLiveData(false) }
        whenever(viewModel.grantedMedicalPermissions).then {
            MutableLiveData(emptySet<MedicalPermission>())
        }
        whenever(viewModel.medicalScreenState).then {
            MutableLiveData(
                MedicalScreenState.ShowMedicalReadWrite(
                    appMetadata = appMetadata,
                    medicalPermissions = allMedicalPermissions,
                )
            )
        }

        launchFragment<PermissionsBottomSheetDialogFragment>(bundleOf())

        onView(withText("Allow $TEST_APP_NAME to access your health records?"))
            .check(matches(isDisplayed()))
        onView(withText("Choose data you want this app to read or write to Health Connect"))
            .check(matches(isDisplayed()))
        onView(
                withText(
                    "If you give access, the app can read and write data such as allergies, lab results, vaccines and more\nAbout health records"
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
        onIdle()
        onView(withText("Allow \u201C$TEST_APP_NAME\u201D to read")).check(matches(isDisplayed()))

        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(
                RecyclerViewActions.scrollTo<RecyclerView.ViewHolder>(
                    hasDescendant(withText("Allow \u201C$TEST_APP_NAME\u201D to write"))
                )
            )
        onIdle()
        onView(withText("Allow \u201C$TEST_APP_NAME\u201D to write")).check(matches(isDisplayed()))
    }

    @EnableFlags(Flags.FLAG_PERMISSION_REQUEST_BOTTOM_SHEET)
    @Test
    fun permissionsRequestBottomSheetDialog_permissionsActivityStateIsFinishRequest_activityIsDestroyed() {
        whenever(viewModel.permissionsActivityState).then {
            MutableLiveData(PermissionsActivityState.FinishRequest)
        }

        val permissions = arrayOf(READ_STEPS, READ_SLEEP)
        val startActivityIntent = getPermissionScreenIntent(permissions)

        val scenario = launchActivityForResult<PermissionsActivity>(startActivityIntent)

        assertThat(scenario.result.resultCode).isEqualTo(RESULT_OK)
        assertThat(scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
    }

    @EnableFlags(Flags.FLAG_PERMISSION_REQUEST_BOTTOM_SHEET)
    @Test
    fun permissionsRequestBottomSheetDialog_permissionsActivityStateIsNoPermissions_activityIsDestroyed() {
        whenever(viewModel.permissionsActivityState).then {
            MutableLiveData(PermissionsActivityState.NoPermissions)
        }

        val permissions = arrayOf(READ_STEPS, READ_SLEEP)
        val startActivityIntent = getPermissionScreenIntent(permissions)

        val scenario = launchActivityForResult<PermissionsActivity>(startActivityIntent)

        assertThat(scenario.result.resultCode).isEqualTo(RESULT_OK)
        assertThat(scenario.state).isEqualTo(Lifecycle.State.DESTROYED)
    }

    @EnableFlags(Flags.FLAG_PERMISSION_REQUEST_BOTTOM_SHEET)
    @Test
    fun permissionsRequestBottomSheetDialog_onCancel_activityIsDestroyed() {
        whenever(viewModel.permissionsActivityState).then {
            MutableLiveData(PermissionsActivityState.ShowFitness)
        }
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
        val permissions = arrayOf(READ_STEPS, READ_SLEEP)
        val startActivityIntent = getPermissionScreenIntent(permissions)

        val scenario = launchActivityForResult<PermissionsActivity>(startActivityIntent)
        scenario.onActivity { activity ->
            val fragmentManager = activity.supportFragmentManager
            val dialogFragment =
                fragmentManager.findFragmentByTag(PermissionsBottomSheetDialogFragment.TAG)
                    as PermissionsBottomSheetDialogFragment

            assertThat(dialogFragment).isNotNull()
            assertThat(dialogFragment.dialog).isNotNull()
            dialogFragment.dialog?.cancel()
        }
        onIdle()

        assertThat(scenario.result.resultCode).isEqualTo(RESULT_CANCELED)
    }

    @EnableFlags(Flags.FLAG_PERMISSION_REQUEST_BOTTOM_SHEET)
    @Test
    fun onCreateDialog_configuresBottomSheetBehaviorCorrectly() {
        whenever(viewModel.permissionsActivityState).then {
            MutableLiveData(PermissionsActivityState.ShowFitness)
        }
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
        val permissions = arrayOf(READ_STEPS, READ_SLEEP)
        val startActivityIntent = getPermissionScreenIntent(permissions)

        val scenario = launchActivityForResult<PermissionsActivity>(startActivityIntent)

        scenario.onActivity { activity ->
            val fragmentManager = activity.supportFragmentManager
            val dialogFragment =
                fragmentManager.findFragmentByTag(PermissionsBottomSheetDialogFragment.TAG)
                    as PermissionsBottomSheetDialogFragment
            assertThat(dialogFragment).isNotNull()

            dialogFragment.let { bottomSheetDialog ->
                val bottomSheetInternal =
                    bottomSheetDialog.dialog?.findViewById<android.widget.FrameLayout>(
                        com.google.android.material.R.id.design_bottom_sheet
                    )
                assertThat(bottomSheetInternal).isNotNull()
                bottomSheetInternal?.let { frameLayout ->
                    val behavior =
                        com.google.android.material.bottomsheet.BottomSheetBehavior.from(
                            frameLayout
                        )

                    val expectedPeekHeight =
                        (dialogFragment.resources.displayMetrics.heightPixels * 0.8).toInt()
                    assertThat(behavior.peekHeight).isEqualTo(expectedPeekHeight)
                    assertThat(behavior.isFitToContents).isFalse()
                    assertThat(behavior.expandedOffset).isEqualTo(0)
                }
            }
        }
    }

    private fun getPermissionScreenIntent(
        permissions: Array<String>,
        testAppName: String = TEST_APP_PACKAGE_NAME,
    ): Intent =
        Intent.makeMainActivity(
                ComponentName(getInstrumentation().context, PermissionsActivity::class.java)
            )
            .putExtra(EXTRA_REQUEST_PERMISSIONS_NAMES, permissions)
            .putExtra(EXTRA_PACKAGE_NAME, testAppName)
            .addFlags(FLAG_ACTIVITY_NEW_TASK)
            .addFlags(FLAG_ACTIVITY_CLEAR_TASK)
}
