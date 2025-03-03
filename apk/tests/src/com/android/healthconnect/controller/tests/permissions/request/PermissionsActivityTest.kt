/*
 * Copyright (C) 2023 The Android Open Source Project
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
 *
 *
 */

package com.android.healthconnect.controller.tests.permissions.request

import android.app.Activity
import android.app.Activity.RESULT_CANCELED
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.Intent.EXTRA_PACKAGE_NAME
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager.EXTRA_REQUEST_PERMISSIONS_NAMES
import android.content.pm.PackageManager.EXTRA_REQUEST_PERMISSIONS_RESULTS
import android.content.pm.PackageManager.FLAG_PERMISSION_REVOKE_WHEN_REQUESTED
import android.content.pm.PackageManager.FLAG_PERMISSION_USER_FIXED
import android.content.pm.PackageManager.PERMISSION_DENIED
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.health.connect.HealthPermissions.READ_EXERCISE
import android.health.connect.HealthPermissions.READ_HEALTH_DATA_HISTORY
import android.health.connect.HealthPermissions.READ_HEALTH_DATA_IN_BACKGROUND
import android.health.connect.HealthPermissions.READ_HEART_RATE
import android.health.connect.HealthPermissions.READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES
import android.health.connect.HealthPermissions.READ_MEDICAL_DATA_CONDITIONS
import android.health.connect.HealthPermissions.READ_MEDICAL_DATA_VACCINES
import android.health.connect.HealthPermissions.READ_SLEEP
import android.health.connect.HealthPermissions.WRITE_ACTIVE_CALORIES_BURNED
import android.health.connect.HealthPermissions.WRITE_EXERCISE
import android.health.connect.HealthPermissions.WRITE_MEDICAL_DATA
import android.health.connect.HealthPermissions.WRITE_SKIN_TEMPERATURE
import android.health.connect.HealthPermissions.WRITE_SLEEP
import android.os.Build
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import android.platform.test.flag.junit.SetFlagsRule
import android.widget.Button
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario.launchActivityForResult
import androidx.test.espresso.Espresso.onIdle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.scrollToLastPosition
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.migration.MigrationViewModel
import com.android.healthconnect.controller.migration.MigrationViewModel.MigrationFragmentState.WithData
import com.android.healthconnect.controller.migration.api.MigrationRestoreState
import com.android.healthconnect.controller.migration.api.MigrationRestoreState.DataRestoreUiError
import com.android.healthconnect.controller.migration.api.MigrationRestoreState.DataRestoreUiState
import com.android.healthconnect.controller.migration.api.MigrationRestoreState.MigrationUiState
import com.android.healthconnect.controller.permissions.api.HealthPermissionManager
import com.android.healthconnect.controller.permissions.api.LoadAccessDateUseCase
import com.android.healthconnect.controller.permissions.request.PermissionsActivity
import com.android.healthconnect.controller.service.HealthPermissionManagerModule
import com.android.healthconnect.controller.tests.utils.BODY_SENSORS_TEST_APP_NAME
import com.android.healthconnect.controller.tests.utils.BODY_SENSORS_TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.NOW
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.UNSUPPORTED_TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.di.FakeDeviceInfoUtils
import com.android.healthconnect.controller.tests.utils.di.FakeHealthPermissionManager
import com.android.healthconnect.controller.tests.utils.showOnboarding
import com.android.healthconnect.controller.tests.utils.toggleAnimation
import com.android.healthconnect.controller.utils.DeviceInfoUtils
import com.android.healthconnect.controller.utils.DeviceInfoUtilsModule
import com.android.healthfitness.flags.Flags
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

@UninstallModules(HealthPermissionManagerModule::class, DeviceInfoUtilsModule::class)
@HiltAndroidTest
class PermissionsActivityTest {

    companion object {
        private val fitnessPermissions =
            arrayOf(READ_EXERCISE, READ_SLEEP, WRITE_SKIN_TEMPERATURE, WRITE_ACTIVE_CALORIES_BURNED)
        private val fitnessAndMedicalPermissions =
            arrayOf(READ_EXERCISE, READ_MEDICAL_DATA_VACCINES)
        private val fitnessAndAdditionalPermissions =
            arrayOf(WRITE_SLEEP, READ_HEALTH_DATA_IN_BACKGROUND)
        private val medicalPermissions = arrayOf(READ_MEDICAL_DATA_VACCINES, WRITE_MEDICAL_DATA)
        private val medicalAndAdditionalPermissions =
            arrayOf(READ_MEDICAL_DATA_VACCINES, READ_HEALTH_DATA_IN_BACKGROUND)
        private val allThreeCombined =
            arrayOf(READ_HEALTH_DATA_IN_BACKGROUND, READ_SLEEP, READ_MEDICAL_DATA_VACCINES)
    }

    @get:Rule val hiltRule = HiltAndroidRule(this)
    @get:Rule val checkFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()
    @get:Rule val setFlagsRule = SetFlagsRule()

    @BindValue val permissionManager: HealthPermissionManager = FakeHealthPermissionManager()
    @BindValue val deviceInfoUtils: DeviceInfoUtils = FakeDeviceInfoUtils()
    @BindValue val migrationViewModel: MigrationViewModel = mock(MigrationViewModel::class.java)
    @BindValue
    val loadAccessDateUseCase: LoadAccessDateUseCase = mock(LoadAccessDateUseCase::class.java)

    private lateinit var context: Context

    @Before
    fun setup() {
        hiltRule.inject()
        context = getInstrumentation().context
        permissionManager.revokeAllHealthPermissions(TEST_APP_PACKAGE_NAME)
        whenever(migrationViewModel.migrationState).then {
            MutableLiveData(
                WithData(
                    MigrationRestoreState(
                        migrationUiState = MigrationUiState.IDLE,
                        dataRestoreState = DataRestoreUiState.IDLE,
                        dataRestoreError = DataRestoreUiError.ERROR_NONE,
                    )
                )
            )
        }
        whenever(loadAccessDateUseCase.invoke(any())).thenReturn(NOW)
        showOnboarding(context, false)
        toggleAnimation(false)
        (permissionManager as FakeHealthPermissionManager).setGrantedPermissionsForTest(
            TEST_APP_PACKAGE_NAME,
            listOf(),
        )
    }

    @After
    fun tearDown() {
        toggleAnimation(true)
        (permissionManager as FakeHealthPermissionManager).reset()
    }

    @Test
    fun unsupportedApp_sendsResultCancelled() {
        val unsupportedAppIntent =
            Intent.makeMainActivity(ComponentName(context, PermissionsActivity::class.java))
                .putExtra(EXTRA_REQUEST_PERMISSIONS_NAMES, fitnessPermissions)
                .putExtra(EXTRA_PACKAGE_NAME, UNSUPPORTED_TEST_APP_PACKAGE_NAME)
                .addFlags(FLAG_ACTIVITY_NEW_TASK)
                .addFlags(FLAG_ACTIVITY_CLEAR_TASK)

        val scenario = launchActivityForResult<PermissionsActivity>(unsupportedAppIntent)

        assertThat(scenario.result.resultCode).isEqualTo(RESULT_CANCELED)
    }

    @Test
    fun intentSkipsUnrecognisedPermission_excludesItFromResponse() {
        val permissions = arrayOf(READ_EXERCISE, WRITE_SLEEP, "permission")
        val startActivityIntent = getPermissionScreenIntent(permissions)

        val scenario = launchActivityForResult<PermissionsActivity>(startActivityIntent)
        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(scrollToLastPosition<RecyclerView.ViewHolder>())
        onIdle()

        onView(withText("Exercise")).check(matches(isDisplayed()))
        onView(withText("Sleep")).check(matches(isDisplayed()))

        scenario.onActivity { activity: PermissionsActivity ->
            activity.findViewById<Button>(R.id.primary_button_outline).callOnClick()
        }

        assertThat(scenario.result.resultCode).isEqualTo(Activity.RESULT_OK)
        val returnedIntent = scenario.result.resultData

        assertThat(returnedIntent.getStringArrayExtra(EXTRA_REQUEST_PERMISSIONS_NAMES))
            .isEqualTo(arrayOf(READ_EXERCISE, WRITE_SLEEP))
        val expectedResults = intArrayOf(PERMISSION_DENIED, PERMISSION_DENIED)
        assertThat(returnedIntent.getIntArrayExtra(EXTRA_REQUEST_PERMISSIONS_RESULTS))
            .isEqualTo(expectedResults)
    }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    fun phrFlagOn_intentSkipsGrantedPermissions_includesItInResponse() {
        val startActivityIntent = getPermissionScreenIntent(fitnessPermissions)
        (permissionManager as FakeHealthPermissionManager).setGrantedPermissionsForTest(
            TEST_APP_PACKAGE_NAME,
            listOf(READ_EXERCISE),
        )

        val scenario = launchActivityForResult<PermissionsActivity>(startActivityIntent)
        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(scrollToLastPosition<RecyclerView.ViewHolder>())
        onIdle()

        onView(withText("Exercise")).check(doesNotExist())
        onView(withText("Sleep")).check(matches(isDisplayed()))
        onView(withText("Active calories burned")).check(matches(isDisplayed()))
        onView(withText("Skin temperature")).check(matches(isDisplayed()))

        scenario.onActivity { activity: PermissionsActivity ->
            activity.findViewById<Button>(R.id.primary_button_outline).callOnClick()
        }

        assertThat(scenario.result.resultCode).isEqualTo(Activity.RESULT_OK)
        val returnedIntent = scenario.result.resultData

        assertThat(returnedIntent.getStringArrayExtra(EXTRA_REQUEST_PERMISSIONS_NAMES))
            .isEqualTo(fitnessPermissions)
        val expectedResults =
            intArrayOf(PERMISSION_GRANTED, PERMISSION_DENIED, PERMISSION_DENIED, PERMISSION_DENIED)
        assertThat(returnedIntent.getIntArrayExtra(EXTRA_REQUEST_PERMISSIONS_RESULTS))
            .isEqualTo(expectedResults)
    }

    @Test
    @DisableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD)
    fun phrFlagOff_intentSkipsGrantedPermissions_includesItInResponse() {
        val startActivityIntent = getPermissionScreenIntent(fitnessPermissions)
        (permissionManager as FakeHealthPermissionManager).setGrantedPermissionsForTest(
            TEST_APP_PACKAGE_NAME,
            listOf(READ_EXERCISE),
        )

        val scenario = launchActivityForResult<PermissionsActivity>(startActivityIntent)
        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(scrollToLastPosition<RecyclerView.ViewHolder>())
        onIdle()

        onView(withText("Exercise")).check(doesNotExist())
        onView(withText("Sleep")).check(matches(isDisplayed()))
        onView(withText("Active calories burned")).check(matches(isDisplayed()))
        onView(withText("Skin temperature")).check(matches(isDisplayed()))

        scenario.onActivity { activity: PermissionsActivity ->
            activity.findViewById<Button>(R.id.primary_button_outline).callOnClick()
        }

        assertThat(scenario.result.resultCode).isEqualTo(Activity.RESULT_OK)
        val returnedIntent = scenario.result.resultData

        assertThat(returnedIntent.getStringArrayExtra(EXTRA_REQUEST_PERMISSIONS_NAMES))
            .isEqualTo(fitnessPermissions)
        val expectedResults =
            intArrayOf(PERMISSION_GRANTED, PERMISSION_DENIED, PERMISSION_DENIED, PERMISSION_DENIED)
        assertThat(returnedIntent.getIntArrayExtra(EXTRA_REQUEST_PERMISSIONS_RESULTS))
            .isEqualTo(expectedResults)
    }

    @Test
    @DisableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD)
    fun intentSkipsHiddenMedicalPermissions() {
        val startActivityIntent = getPermissionScreenIntent(medicalPermissions)

        val scenario = launchActivityForResult<PermissionsActivity>(startActivityIntent)

        assertThat(scenario.result.resultCode).isEqualTo(Activity.RESULT_OK)
        val returnedIntent = scenario.result.resultData
        assertThat(returnedIntent.getStringArrayExtra(EXTRA_REQUEST_PERMISSIONS_NAMES)).isEmpty()
        assertThat(returnedIntent.getIntArrayExtra(EXTRA_REQUEST_PERMISSIONS_RESULTS)).isEmpty()
    }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    fun phrFlagOn_requestAlreadyGrantedPermissions_sendsEmptyResultOk_doesNotModifyPermissions() {
        val startActivityIntent = getPermissionScreenIntent(fitnessPermissions)
        (permissionManager as FakeHealthPermissionManager).setGrantedPermissionsForTest(
            TEST_APP_PACKAGE_NAME,
            fitnessPermissions.toList(),
        )

        val scenario = launchActivityForResult<PermissionsActivity>(startActivityIntent)

        assertThat(scenario.result.resultCode).isEqualTo(Activity.RESULT_OK)
        val returnedIntent = scenario.result.resultData

        assertThat(returnedIntent.getStringArrayExtra(EXTRA_REQUEST_PERMISSIONS_NAMES))
            .isEqualTo(fitnessPermissions)
        val expectedResults =
            intArrayOf(
                PERMISSION_GRANTED,
                PERMISSION_GRANTED,
                PERMISSION_GRANTED,
                PERMISSION_GRANTED,
            )
        assertThat(returnedIntent.getIntArrayExtra(EXTRA_REQUEST_PERMISSIONS_RESULTS))
            .isEqualTo(expectedResults)

        assertThat(permissionManager.revokeHealthPermissionInvocations).isEqualTo(0)
        assertThat(permissionManager.grantHealthPermissionInvocations).isEqualTo(0)
    }

    @Test
    @DisableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD)
    fun phrFlagOff_requestAlreadyGrantedPermissions_sendsEmptyResultOk_doesNotModifyPermissions() {
        val startActivityIntent = getPermissionScreenIntent(fitnessPermissions)
        (permissionManager as FakeHealthPermissionManager).setGrantedPermissionsForTest(
            TEST_APP_PACKAGE_NAME,
            fitnessPermissions.toList(),
        )

        val scenario = launchActivityForResult<PermissionsActivity>(startActivityIntent)

        assertThat(scenario.result.resultCode).isEqualTo(Activity.RESULT_OK)
        val returnedIntent = scenario.result.resultData

        assertThat(returnedIntent.getStringArrayExtra(EXTRA_REQUEST_PERMISSIONS_NAMES))
            .isEqualTo(fitnessPermissions)
        val expectedResults =
            intArrayOf(
                PERMISSION_GRANTED,
                PERMISSION_GRANTED,
                PERMISSION_GRANTED,
                PERMISSION_GRANTED,
            )
        assertThat(returnedIntent.getIntArrayExtra(EXTRA_REQUEST_PERMISSIONS_RESULTS))
            .isEqualTo(expectedResults)

        assertThat(permissionManager.revokeHealthPermissionInvocations).isEqualTo(0)
        assertThat(permissionManager.grantHealthPermissionInvocations).isEqualTo(0)
    }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    fun requestOnlyWriteMedicalPermission_clickOnAllow_sendsResultOk() {
        val permissions = arrayOf(READ_MEDICAL_DATA_VACCINES, WRITE_MEDICAL_DATA)
        val startActivityIntent = getPermissionScreenIntent(permissions)
        (permissionManager as FakeHealthPermissionManager).setGrantedPermissionsForTest(
            TEST_APP_PACKAGE_NAME,
            listOf(READ_MEDICAL_DATA_VACCINES),
        )

        val scenario = launchActivityForResult<PermissionsActivity>(startActivityIntent)

        // Only medical write needs granting
        onView(
                withText(
                    "If you allow, $TEST_APP_NAME can share your health records with Health Connect."
                )
            )
            .check(matches(isDisplayed()))
        onView(withText("Allow")).perform(click())

        assertThat(scenario.result.resultCode).isEqualTo(Activity.RESULT_OK)
        val returnedIntent = scenario.result.resultData

        assertThat(returnedIntent.getStringArrayExtra(EXTRA_REQUEST_PERMISSIONS_NAMES))
            .isEqualTo(permissions)
        val expectedResults = intArrayOf(PERMISSION_GRANTED, PERMISSION_GRANTED)
        assertThat(returnedIntent.getIntArrayExtra(EXTRA_REQUEST_PERMISSIONS_RESULTS))
            .isEqualTo(expectedResults)

        assertThat(permissionManager.getGrantedHealthPermissions(TEST_APP_PACKAGE_NAME))
            .containsExactlyElementsIn(listOf(READ_MEDICAL_DATA_VACCINES, WRITE_MEDICAL_DATA))
    }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    fun requestOnlyWriteMedicalPermission_clickOnDontAllow_sendsResultOk() {
        val permissions = arrayOf(READ_MEDICAL_DATA_VACCINES, WRITE_MEDICAL_DATA)
        val startActivityIntent = getPermissionScreenIntent(permissions)
        (permissionManager as FakeHealthPermissionManager).setGrantedPermissionsForTest(
            TEST_APP_PACKAGE_NAME,
            listOf(READ_MEDICAL_DATA_VACCINES),
        )

        val scenario = launchActivityForResult<PermissionsActivity>(startActivityIntent)

        // Only medical write needs granting
        onView(
                withText(
                    "If you allow, $TEST_APP_NAME can share your health records with Health Connect."
                )
            )
            .check(matches(isDisplayed()))
        onView(withText("Don't allow")).perform(click())

        assertThat(scenario.result.resultCode).isEqualTo(Activity.RESULT_OK)
        val returnedIntent = scenario.result.resultData

        assertThat(returnedIntent.getStringArrayExtra(EXTRA_REQUEST_PERMISSIONS_NAMES))
            .isEqualTo(permissions)
        val expectedResults = intArrayOf(PERMISSION_GRANTED, PERMISSION_DENIED)
        assertThat(returnedIntent.getIntArrayExtra(EXTRA_REQUEST_PERMISSIONS_RESULTS))
            .isEqualTo(expectedResults)

        assertThat(permissionManager.getGrantedHealthPermissions(TEST_APP_PACKAGE_NAME))
            .containsExactlyElementsIn(listOf(READ_MEDICAL_DATA_VACCINES))
    }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    fun requestMedicalPermissions_someGrantedSomeDenied_clickOnAllow_includesAllInResponse() {
        val permissions =
            arrayOf(READ_MEDICAL_DATA_CONDITIONS, READ_MEDICAL_DATA_VACCINES, WRITE_MEDICAL_DATA)
        val startActivityIntent = getPermissionScreenIntent(permissions)
        (permissionManager as FakeHealthPermissionManager).setGrantedPermissionsForTest(
            TEST_APP_PACKAGE_NAME,
            listOf(READ_MEDICAL_DATA_VACCINES),
        )

        val scenario = launchActivityForResult<PermissionsActivity>(startActivityIntent)
        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(scrollToLastPosition<RecyclerView.ViewHolder>())
        onView(withText("Conditions")).perform(click())
        onView(withText("Allow")).perform(click())

        assertThat(scenario.result.resultCode).isEqualTo(Activity.RESULT_OK)
        val returnedIntent = scenario.result.resultData

        assertThat(returnedIntent.getStringArrayExtra(EXTRA_REQUEST_PERMISSIONS_NAMES))
            .isEqualTo(permissions)
        val expectedResults = intArrayOf(PERMISSION_GRANTED, PERMISSION_GRANTED, PERMISSION_DENIED)
        assertThat(returnedIntent.getIntArrayExtra(EXTRA_REQUEST_PERMISSIONS_RESULTS))
            .isEqualTo(expectedResults)

        assertThat(permissionManager.getGrantedHealthPermissions(TEST_APP_PACKAGE_NAME))
            .containsExactlyElementsIn(
                listOf(READ_MEDICAL_DATA_VACCINES, READ_MEDICAL_DATA_CONDITIONS)
            )
    }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    fun requestMedicalPermissions_someGrantedSomeDenied_clickOnDontAllow_includesAllInResponse() {
        val permissions =
            arrayOf(READ_MEDICAL_DATA_CONDITIONS, READ_MEDICAL_DATA_VACCINES, WRITE_MEDICAL_DATA)
        val startActivityIntent = getPermissionScreenIntent(permissions)
        (permissionManager as FakeHealthPermissionManager).setGrantedPermissionsForTest(
            TEST_APP_PACKAGE_NAME,
            listOf(READ_MEDICAL_DATA_VACCINES),
        )

        val scenario = launchActivityForResult<PermissionsActivity>(startActivityIntent)

        // This should not be granted even if we toggled it on
        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(scrollToLastPosition<RecyclerView.ViewHolder>())
        onView(withText("Conditions")).perform(click())
        onView(withText("Don't allow")).perform(click())

        assertThat(scenario.result.resultCode).isEqualTo(Activity.RESULT_OK)
        val returnedIntent = scenario.result.resultData

        assertThat(returnedIntent.getStringArrayExtra(EXTRA_REQUEST_PERMISSIONS_NAMES))
            .isEqualTo(permissions)
        val expectedResults = intArrayOf(PERMISSION_DENIED, PERMISSION_GRANTED, PERMISSION_DENIED)
        assertThat(returnedIntent.getIntArrayExtra(EXTRA_REQUEST_PERMISSIONS_RESULTS))
            .isEqualTo(expectedResults)

        assertThat(permissionManager.getGrantedHealthPermissions(TEST_APP_PACKAGE_NAME))
            .containsExactlyElementsIn(listOf(READ_MEDICAL_DATA_VACCINES))
    }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    fun requestMedicalAndFitness_clickOnAllow_grantsMedical_showsFitness() {
        val permissions =
            arrayOf(
                READ_MEDICAL_DATA_CONDITIONS,
                READ_MEDICAL_DATA_VACCINES,
                READ_SLEEP,
                WRITE_EXERCISE,
                WRITE_MEDICAL_DATA,
            )
        val startActivityIntent = getPermissionScreenIntent(permissions)
        (permissionManager as FakeHealthPermissionManager).setGrantedPermissionsForTest(
            TEST_APP_PACKAGE_NAME,
            listOf(READ_MEDICAL_DATA_VACCINES),
        )

        launchActivityForResult<PermissionsActivity>(startActivityIntent)
        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(scrollToLastPosition<RecyclerView.ViewHolder>())
        onView(withText("Conditions")).perform(click())
        onView(withText("Allow")).perform(click())

        onIdle()

        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(scrollToLastPosition<RecyclerView.ViewHolder>())
        onView(withText("Sleep")).check(matches(isDisplayed()))

        assertThat(permissionManager.revokeHealthPermissionInvocations).isEqualTo(1)
        assertThat(permissionManager.grantHealthPermissionInvocations).isEqualTo(2)
        assertThat(permissionManager.getGrantedHealthPermissions(TEST_APP_PACKAGE_NAME))
            .containsExactlyElementsIn(
                listOf(READ_MEDICAL_DATA_VACCINES, READ_MEDICAL_DATA_CONDITIONS)
            )
    }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    fun requestMedicalAndFitness_clickOnDontAllow_revokesMedical_showsFitness() {
        val permissions =
            arrayOf(
                READ_MEDICAL_DATA_CONDITIONS,
                READ_MEDICAL_DATA_VACCINES,
                READ_SLEEP,
                WRITE_EXERCISE,
                WRITE_MEDICAL_DATA,
            )
        val startActivityIntent = getPermissionScreenIntent(permissions)
        (permissionManager as FakeHealthPermissionManager).setGrantedPermissionsForTest(
            TEST_APP_PACKAGE_NAME,
            listOf(READ_MEDICAL_DATA_VACCINES),
        )

        launchActivityForResult<PermissionsActivity>(startActivityIntent)
        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(scrollToLastPosition<RecyclerView.ViewHolder>())
        // This should not be granted even if we toggled it on
        onView(withText("Conditions")).perform(click())
        onView(withText("Don't allow")).perform(click())

        onIdle()
        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(scrollToLastPosition<RecyclerView.ViewHolder>())
        onView(withText("Sleep")).check(matches(isDisplayed()))

        assertThat(permissionManager.revokeHealthPermissionInvocations).isEqualTo(2)
        assertThat(permissionManager.grantHealthPermissionInvocations).isEqualTo(1)
        assertThat(permissionManager.getGrantedHealthPermissions(TEST_APP_PACKAGE_NAME))
            .containsExactlyElementsIn(listOf(READ_MEDICAL_DATA_VACCINES))
    }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    fun requestMedicalAndBackground_clickOnAllow_showsBackground() {
        val permissions =
            arrayOf(
                READ_MEDICAL_DATA_CONDITIONS,
                READ_MEDICAL_DATA_VACCINES,
                READ_HEALTH_DATA_IN_BACKGROUND,
                WRITE_MEDICAL_DATA,
            )
        val startActivityIntent = getPermissionScreenIntent(permissions)
        (permissionManager as FakeHealthPermissionManager).setGrantedPermissionsForTest(
            TEST_APP_PACKAGE_NAME,
            listOf(READ_MEDICAL_DATA_VACCINES),
        )

        launchActivityForResult<PermissionsActivity>(startActivityIntent)

        // This should not be granted even if we toggled it on
        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(scrollToLastPosition<RecyclerView.ViewHolder>())
        onView(withText("Conditions")).perform(click())
        onView(withText("Allow")).perform(click())

        onIdle()

        onView(withText("Allow $TEST_APP_NAME to access data in the background?"))
            .check(matches(isDisplayed()))

        assertThat(permissionManager.getGrantedHealthPermissions(TEST_APP_PACKAGE_NAME))
            .containsExactlyElementsIn(
                listOf(READ_MEDICAL_DATA_VACCINES, READ_MEDICAL_DATA_CONDITIONS)
            )
    }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    fun requestMedicalAndBackground_clickOnDontAllow_doesNotShowBackground() {
        val permissions =
            arrayOf(
                READ_MEDICAL_DATA_CONDITIONS,
                READ_MEDICAL_DATA_VACCINES,
                READ_HEALTH_DATA_IN_BACKGROUND,
                WRITE_MEDICAL_DATA,
            )
        val startActivityIntent = getPermissionScreenIntent(permissions)

        val scenario = launchActivityForResult<PermissionsActivity>(startActivityIntent)

        // This should not be granted even if we toggled it on
        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(scrollToLastPosition<RecyclerView.ViewHolder>())
        onView(withText("Conditions")).perform(click())
        onView(withText("Don't allow")).perform(click())

        assertThat(scenario.result.resultCode).isEqualTo(Activity.RESULT_OK)
        val returnedIntent = scenario.result.resultData

        assertThat(returnedIntent.getStringArrayExtra(EXTRA_REQUEST_PERMISSIONS_NAMES))
            .isEqualTo(permissions)
        val expectedResults =
            intArrayOf(PERMISSION_DENIED, PERMISSION_DENIED, PERMISSION_DENIED, PERMISSION_DENIED)
        assertThat(returnedIntent.getIntArrayExtra(EXTRA_REQUEST_PERMISSIONS_RESULTS))
            .isEqualTo(expectedResults)

        assertThat(permissionManager.getGrantedHealthPermissions(TEST_APP_PACKAGE_NAME)).isEmpty()
    }

    @Test
    fun requestFitnessPermissions_onboardingNotDone_redirectsToOnboarding() {
        val permissions =
            arrayOf(READ_EXERCISE, READ_SLEEP, WRITE_ACTIVE_CALORIES_BURNED, WRITE_SLEEP)
        val startActivityIntent = getPermissionScreenIntent(permissions)
        showOnboarding(context, true)

        launchActivityForResult<PermissionsActivity>(startActivityIntent)

        onIdle()
        onView(withId(R.id.onboarding)).check(matches(isDisplayed()))
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
    @RequiresFlagsEnabled(
        Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED,
        android.permission.flags.Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED,
    )
    @Test
    fun requestFitnessPermissions_notSplitPermissionRequest_redirectsToOnboarding() {
        val permissions = arrayOf(READ_HEART_RATE)
        val startActivityIntent =
            getPermissionScreenIntent(permissions, BODY_SENSORS_TEST_APP_PACKAGE_NAME)
        (permissionManager as FakeHealthPermissionManager).setHealthPermissionFlags(
            BODY_SENSORS_TEST_APP_PACKAGE_NAME,
            mapOf(READ_HEART_RATE to FLAG_PERMISSION_USER_FIXED),
        )
        // Ensure this app has never shown onboarding before.
        showOnboarding(context, true)

        launchActivityForResult<PermissionsActivity>(startActivityIntent)

        onIdle()
        onView(withId(R.id.onboarding)).check(matches(isDisplayed()))
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
    @RequiresFlagsEnabled(
        Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED,
        android.permission.flags.Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED,
    )
    @Test
    fun requestFitnessPermissions_legacyBodySensorsApp_onboardingSkipped() {
        val permissions = arrayOf(READ_HEART_RATE)
        val startActivityIntent =
            getPermissionScreenIntent(permissions, BODY_SENSORS_TEST_APP_PACKAGE_NAME)
        (permissionManager as FakeHealthPermissionManager).setHealthPermissionFlags(
            BODY_SENSORS_TEST_APP_PACKAGE_NAME,
            mapOf(
                READ_HEART_RATE to FLAG_PERMISSION_REVOKE_WHEN_REQUESTED,
                READ_HEALTH_DATA_IN_BACKGROUND to FLAG_PERMISSION_REVOKE_WHEN_REQUESTED,
            ),
        )
        // Ensure this app has never shown onboarding before.
        showOnboarding(context, true)

        val scenario = launchActivityForResult<PermissionsActivity>(startActivityIntent)

        onIdle()
        onView(withId(R.id.onboarding)).check(doesNotExist())
        onView(withText("Allow")).check(matches(isDisplayed()))
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
    @RequiresFlagsEnabled(
        Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED,
        android.permission.flags.Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED,
    )
    @Test
    fun requestFitnessPermissions_legacyBodySensors_canGrantPermissions() {
        val permissions = arrayOf(READ_HEART_RATE)
        val startActivityIntent =
            getPermissionScreenIntent(permissions, BODY_SENSORS_TEST_APP_PACKAGE_NAME)
        (permissionManager as FakeHealthPermissionManager).setHealthPermissionFlags(
            BODY_SENSORS_TEST_APP_PACKAGE_NAME,
            mapOf(
                READ_HEART_RATE to FLAG_PERMISSION_REVOKE_WHEN_REQUESTED,
                READ_HEALTH_DATA_IN_BACKGROUND to FLAG_PERMISSION_REVOKE_WHEN_REQUESTED,
            ),
        )

        val scenario = launchActivityForResult<PermissionsActivity>(startActivityIntent)
        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(scrollToLastPosition<RecyclerView.ViewHolder>())
        onView(withText("Heart rate")).perform(click())
        onView(withText("Allow")).perform(click())

        assertThat(scenario.result.resultCode).isEqualTo(Activity.RESULT_OK)
        val returnedIntent = scenario.result.resultData
        assertThat(returnedIntent.getStringArrayExtra(EXTRA_REQUEST_PERMISSIONS_NAMES))
            .isEqualTo(permissions)
        val expectedResults = intArrayOf(PERMISSION_GRANTED)
        assertThat(returnedIntent.getIntArrayExtra(EXTRA_REQUEST_PERMISSIONS_RESULTS))
            .isEqualTo(expectedResults)
        assertThat(
                permissionManager.getGrantedHealthPermissions(BODY_SENSORS_TEST_APP_PACKAGE_NAME)
            )
            .containsExactlyElementsIn(listOf(READ_HEART_RATE))
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
    @RequiresFlagsEnabled(
        Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED,
        android.permission.flags.Flags.FLAG_REPLACE_BODY_SENSOR_PERMISSION_ENABLED,
    )
    @Test
    fun requestFitnessPermissions_legacyBodySensors_canGrantBackgroundPermission() {
        val permissions = arrayOf(READ_HEART_RATE, READ_HEALTH_DATA_IN_BACKGROUND)
        val startActivityIntent =
            getPermissionScreenIntent(permissions, BODY_SENSORS_TEST_APP_PACKAGE_NAME)
        (permissionManager as FakeHealthPermissionManager).setHealthPermissionFlags(
            BODY_SENSORS_TEST_APP_PACKAGE_NAME,
            mapOf(
                READ_HEART_RATE to FLAG_PERMISSION_REVOKE_WHEN_REQUESTED,
                READ_HEALTH_DATA_IN_BACKGROUND to FLAG_PERMISSION_REVOKE_WHEN_REQUESTED,
            ),
        )

        val scenario = launchActivityForResult<PermissionsActivity>(startActivityIntent)
        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(scrollToLastPosition<RecyclerView.ViewHolder>())
        onView(withText("Heart rate")).perform(click())
        onView(withText("Allow")).perform(click())
        onView(withText("Allow $BODY_SENSORS_TEST_APP_NAME to access data in the background?"))
            .check(matches(isDisplayed()))
        onView(withText("Allow")).perform(click())
        onIdle()

        assertThat(scenario.result.resultCode).isEqualTo(Activity.RESULT_OK)
        val returnedIntent = scenario.result.resultData
        assertThat(returnedIntent.getStringArrayExtra(EXTRA_REQUEST_PERMISSIONS_NAMES))
            .isEqualTo(permissions)
        val expectedResults = intArrayOf(PERMISSION_GRANTED, PERMISSION_GRANTED)
        assertThat(returnedIntent.getIntArrayExtra(EXTRA_REQUEST_PERMISSIONS_RESULTS))
            .isEqualTo(expectedResults)
        assertThat(
                permissionManager.getGrantedHealthPermissions(BODY_SENSORS_TEST_APP_PACKAGE_NAME)
            )
            .containsExactlyElementsIn(listOf(READ_HEART_RATE, READ_HEALTH_DATA_IN_BACKGROUND))
    }

    @Test
    fun requestFitnessPermissions_someGrantedSomeDenied_clickOnAllow_includesAllInResponse() {
        val permissions =
            arrayOf(READ_EXERCISE, READ_SLEEP, WRITE_ACTIVE_CALORIES_BURNED, WRITE_SLEEP)
        val startActivityIntent = getPermissionScreenIntent(permissions)
        (permissionManager as FakeHealthPermissionManager).setGrantedPermissionsForTest(
            TEST_APP_PACKAGE_NAME,
            listOf(READ_SLEEP),
        )

        val scenario = launchActivityForResult<PermissionsActivity>(startActivityIntent)

        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(scrollToLastPosition<RecyclerView.ViewHolder>())
        onView(withText("Exercise")).perform(click())
        onView(withText("Allow")).perform(click())

        assertThat(scenario.result.resultCode).isEqualTo(Activity.RESULT_OK)
        val returnedIntent = scenario.result.resultData

        assertThat(returnedIntent.getStringArrayExtra(EXTRA_REQUEST_PERMISSIONS_NAMES))
            .isEqualTo(permissions)
        val expectedResults =
            intArrayOf(PERMISSION_GRANTED, PERMISSION_GRANTED, PERMISSION_DENIED, PERMISSION_DENIED)
        assertThat(returnedIntent.getIntArrayExtra(EXTRA_REQUEST_PERMISSIONS_RESULTS))
            .isEqualTo(expectedResults)

        assertThat(permissionManager.getGrantedHealthPermissions(TEST_APP_PACKAGE_NAME))
            .containsExactlyElementsIn(listOf(READ_EXERCISE, READ_SLEEP))
    }

    @Test
    fun requestFitnessPermissions_someGrantedSomeDenied_clickOnDontAllow_includesAllInResponse() {
        val permissions =
            arrayOf(READ_EXERCISE, READ_SLEEP, WRITE_ACTIVE_CALORIES_BURNED, WRITE_SLEEP)
        val startActivityIntent = getPermissionScreenIntent(permissions)
        (permissionManager as FakeHealthPermissionManager).setGrantedPermissionsForTest(
            TEST_APP_PACKAGE_NAME,
            listOf(READ_SLEEP),
        )

        val scenario = launchActivityForResult<PermissionsActivity>(startActivityIntent)

        // This should not be granted even if it's toggled on
        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(scrollToLastPosition<RecyclerView.ViewHolder>())
        onView(withText("Exercise")).perform(click())
        onView(withText("Don't allow")).perform(click())

        assertThat(scenario.result.resultCode).isEqualTo(Activity.RESULT_OK)
        val returnedIntent = scenario.result.resultData

        assertThat(returnedIntent.getStringArrayExtra(EXTRA_REQUEST_PERMISSIONS_NAMES))
            .isEqualTo(permissions)
        val expectedResults =
            intArrayOf(PERMISSION_DENIED, PERMISSION_GRANTED, PERMISSION_DENIED, PERMISSION_DENIED)
        assertThat(returnedIntent.getIntArrayExtra(EXTRA_REQUEST_PERMISSIONS_RESULTS))
            .isEqualTo(expectedResults)

        assertThat(permissionManager.getGrantedHealthPermissions(TEST_APP_PACKAGE_NAME))
            .containsExactlyElementsIn(listOf(READ_SLEEP))
    }

    @Test
    fun requestFitnessAndAdditional_clickOnAllow_showsAdditional() {
        val permissions =
            arrayOf(
                READ_EXERCISE,
                READ_SLEEP,
                READ_HEALTH_DATA_HISTORY,
                WRITE_ACTIVE_CALORIES_BURNED,
                WRITE_SLEEP,
            )
        val startActivityIntent = getPermissionScreenIntent(permissions)
        (permissionManager as FakeHealthPermissionManager).setGrantedPermissionsForTest(
            TEST_APP_PACKAGE_NAME,
            listOf(READ_SLEEP),
        )

        launchActivityForResult<PermissionsActivity>(startActivityIntent)
        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(scrollToLastPosition<RecyclerView.ViewHolder>())
        onView(withText("Exercise")).perform(click())
        onView(withText("Allow")).perform(click())

        onIdle()

        onView(withText("Allow $TEST_APP_NAME to access past data?")).check(matches(isDisplayed()))
        assertThat(permissionManager.getGrantedHealthPermissions(TEST_APP_PACKAGE_NAME))
            .containsExactlyElementsIn(listOf(READ_SLEEP, READ_EXERCISE))
    }

    @Test
    fun requestFitnessAndAdditional_someReadGranted_clickOnDontAllow_showsAdditional() {
        val permissions =
            arrayOf(
                READ_EXERCISE,
                READ_SLEEP,
                READ_HEALTH_DATA_HISTORY,
                WRITE_ACTIVE_CALORIES_BURNED,
                WRITE_SLEEP,
            )
        val startActivityIntent = getPermissionScreenIntent(permissions)
        (permissionManager as FakeHealthPermissionManager).setGrantedPermissionsForTest(
            TEST_APP_PACKAGE_NAME,
            listOf(READ_SLEEP),
        )

        launchActivityForResult<PermissionsActivity>(startActivityIntent)

        // This should not be granted even if it's toggled on
        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(scrollToLastPosition<RecyclerView.ViewHolder>())
        onView(withText("Exercise")).perform(click())
        onView(withText("Don't allow")).perform(click())

        onIdle()

        onView(withText("Allow $TEST_APP_NAME to access past data?")).check(matches(isDisplayed()))
        assertThat(permissionManager.getGrantedHealthPermissions(TEST_APP_PACKAGE_NAME))
            .containsExactlyElementsIn(listOf(READ_SLEEP))
    }

    @Test
    fun requestFitnessAndAdditional_noReadGranted_clickOnDontAllow_doesNotShowAdditional() {
        val permissions =
            arrayOf(
                READ_EXERCISE,
                READ_SLEEP,
                READ_HEALTH_DATA_HISTORY,
                WRITE_ACTIVE_CALORIES_BURNED,
                WRITE_SLEEP,
            )
        val startActivityIntent = getPermissionScreenIntent(permissions)

        val scenario = launchActivityForResult<PermissionsActivity>(startActivityIntent)

        // This should not be granted even if it's toggled on
        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(scrollToLastPosition<RecyclerView.ViewHolder>())
        onView(withText("Exercise")).perform(click())
        onView(withText("Don't allow")).perform(click())

        assertThat(scenario.result.resultCode).isEqualTo(Activity.RESULT_OK)
        val returnedIntent = scenario.result.resultData

        assertThat(returnedIntent.getStringArrayExtra(EXTRA_REQUEST_PERMISSIONS_NAMES))
            .isEqualTo(permissions)
        val expectedResults =
            intArrayOf(
                PERMISSION_DENIED,
                PERMISSION_DENIED,
                PERMISSION_DENIED,
                PERMISSION_DENIED,
                PERMISSION_DENIED,
            )
        assertThat(returnedIntent.getIntArrayExtra(EXTRA_REQUEST_PERMISSIONS_RESULTS))
            .isEqualTo(expectedResults)

        assertThat(permissionManager.getGrantedHealthPermissions(TEST_APP_PACKAGE_NAME)).isEmpty()
    }

    @Test
    fun requestAdditional_noReadGranted_doesNotShowAdditional() {
        val permissions = arrayOf(READ_HEALTH_DATA_HISTORY, READ_HEALTH_DATA_IN_BACKGROUND)
        val startActivityIntent = getPermissionScreenIntent(permissions)

        val scenario = launchActivityForResult<PermissionsActivity>(startActivityIntent)

        assertThat(scenario.result.resultCode).isEqualTo(Activity.RESULT_OK)
        val returnedIntent = scenario.result.resultData

        assertThat(returnedIntent.getStringArrayExtra(EXTRA_REQUEST_PERMISSIONS_NAMES))
            .isEqualTo(permissions)
        val expectedResults = intArrayOf(PERMISSION_DENIED, PERMISSION_DENIED)
        assertThat(returnedIntent.getIntArrayExtra(EXTRA_REQUEST_PERMISSIONS_RESULTS))
            .isEqualTo(expectedResults)

        assertThat(permissionManager.getGrantedHealthPermissions(TEST_APP_PACKAGE_NAME)).isEmpty()
    }

    @Test
    fun requestAdditional_readAndHistoryGranted_showsBackgroundRequest() {
        val permissions = arrayOf(READ_HEALTH_DATA_HISTORY, READ_HEALTH_DATA_IN_BACKGROUND)
        val startActivityIntent = getPermissionScreenIntent(permissions)
        (permissionManager as FakeHealthPermissionManager).setGrantedPermissionsForTest(
            TEST_APP_PACKAGE_NAME,
            listOf(READ_HEALTH_DATA_HISTORY, READ_SLEEP),
        )
        val scenario = launchActivityForResult<PermissionsActivity>(startActivityIntent)

        onView(withText("Allow $TEST_APP_NAME to access data in the background?"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun requestAdditional_readAndBackgroundGranted_showsHistoryRequest() {
        val permissions = arrayOf(READ_HEALTH_DATA_HISTORY, READ_HEALTH_DATA_IN_BACKGROUND)
        val startActivityIntent = getPermissionScreenIntent(permissions)
        (permissionManager as FakeHealthPermissionManager).setGrantedPermissionsForTest(
            TEST_APP_PACKAGE_NAME,
            listOf(READ_HEALTH_DATA_IN_BACKGROUND, READ_SLEEP),
        )
        launchActivityForResult<PermissionsActivity>(startActivityIntent)

        onView(withText("Allow $TEST_APP_NAME to access past data?")).check(matches(isDisplayed()))
    }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    fun requestAdditional_clickOnAllow_includesAllInResponse() {
        val permissions = arrayOf(READ_HEALTH_DATA_HISTORY, READ_HEALTH_DATA_IN_BACKGROUND)
        (permissionManager as FakeHealthPermissionManager).setGrantedPermissionsForTest(
            TEST_APP_PACKAGE_NAME,
            listOf(READ_SLEEP),
        )
        val startActivityIntent = getPermissionScreenIntent(permissions)
        val scenario = launchActivityForResult<PermissionsActivity>(startActivityIntent)

        onView(withText("Access past fitness and wellness data")).perform(click())
        onView(withText("Allow")).perform(click())
        assertThat(scenario.result.resultCode).isEqualTo(Activity.RESULT_OK)
        val returnedIntent = scenario.result.resultData

        assertThat(returnedIntent.getStringArrayExtra(EXTRA_REQUEST_PERMISSIONS_NAMES))
            .isEqualTo(permissions)
        val expectedResults = intArrayOf(PERMISSION_GRANTED, PERMISSION_DENIED)
        assertThat(returnedIntent.getIntArrayExtra(EXTRA_REQUEST_PERMISSIONS_RESULTS))
            .isEqualTo(expectedResults)

        assertThat(permissionManager.getGrantedHealthPermissions(TEST_APP_PACKAGE_NAME))
            .containsExactlyElementsIn(arrayOf(READ_SLEEP, READ_HEALTH_DATA_HISTORY))
    }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    fun requestAdditional_clickOnDontAllow_includesAllInResponse() {
        val permissions = arrayOf(READ_HEALTH_DATA_HISTORY, READ_HEALTH_DATA_IN_BACKGROUND)
        (permissionManager as FakeHealthPermissionManager).setGrantedPermissionsForTest(
            TEST_APP_PACKAGE_NAME,
            listOf(READ_SLEEP),
        )
        val startActivityIntent = getPermissionScreenIntent(permissions)
        val scenario = launchActivityForResult<PermissionsActivity>(startActivityIntent)

        // This should not be granted even if it's toggled on
        onView(withText("Access past fitness and wellness data")).perform(click())
        onView(withText("Don't allow")).perform(click())
        assertThat(scenario.result.resultCode).isEqualTo(Activity.RESULT_OK)
        val returnedIntent = scenario.result.resultData

        assertThat(returnedIntent.getStringArrayExtra(EXTRA_REQUEST_PERMISSIONS_NAMES))
            .isEqualTo(permissions)
        val expectedResults = intArrayOf(PERMISSION_DENIED, PERMISSION_DENIED)
        assertThat(returnedIntent.getIntArrayExtra(EXTRA_REQUEST_PERMISSIONS_RESULTS))
            .isEqualTo(expectedResults)

        assertThat(permissionManager.getGrantedHealthPermissions(TEST_APP_PACKAGE_NAME))
            .containsExactlyElementsIn(arrayOf(READ_SLEEP))
    }

    @Test
    fun requestOneAdditional_clickOnAllow_includesAllInResponse() {
        val permissions = arrayOf(READ_HEALTH_DATA_HISTORY)
        (permissionManager as FakeHealthPermissionManager).setGrantedPermissionsForTest(
            TEST_APP_PACKAGE_NAME,
            listOf(READ_SLEEP),
        )
        val startActivityIntent = getPermissionScreenIntent(permissions)
        val scenario = launchActivityForResult<PermissionsActivity>(startActivityIntent)

        onView(withText("Allow $TEST_APP_NAME to access past data?")).check(matches(isDisplayed()))
        onView(withText("Allow")).perform(click())
        assertThat(scenario.result.resultCode).isEqualTo(Activity.RESULT_OK)
        val returnedIntent = scenario.result.resultData

        assertThat(returnedIntent.getStringArrayExtra(EXTRA_REQUEST_PERMISSIONS_NAMES))
            .isEqualTo(permissions)
        val expectedResults = intArrayOf(PERMISSION_GRANTED)
        assertThat(returnedIntent.getIntArrayExtra(EXTRA_REQUEST_PERMISSIONS_RESULTS))
            .isEqualTo(expectedResults)

        assertThat(permissionManager.getGrantedHealthPermissions(TEST_APP_PACKAGE_NAME))
            .containsExactlyElementsIn(arrayOf(READ_HEALTH_DATA_HISTORY, READ_SLEEP))
    }

    @Test
    fun requestOneAdditional_clickOnDontAllow_includesAllInResponse() {
        val permissions = arrayOf(READ_HEALTH_DATA_IN_BACKGROUND)
        (permissionManager as FakeHealthPermissionManager).setGrantedPermissionsForTest(
            TEST_APP_PACKAGE_NAME,
            listOf(READ_SLEEP),
        )
        val startActivityIntent = getPermissionScreenIntent(permissions)
        val scenario = launchActivityForResult<PermissionsActivity>(startActivityIntent)

        onView(withText("Allow $TEST_APP_NAME to access data in the background?"))
            .check(matches(isDisplayed()))
        onView(withText("Don't allow")).perform(click())
        assertThat(scenario.result.resultCode).isEqualTo(Activity.RESULT_OK)
        val returnedIntent = scenario.result.resultData

        assertThat(returnedIntent.getStringArrayExtra(EXTRA_REQUEST_PERMISSIONS_NAMES))
            .isEqualTo(permissions)
        val expectedResults = intArrayOf(PERMISSION_DENIED)
        assertThat(returnedIntent.getIntArrayExtra(EXTRA_REQUEST_PERMISSIONS_RESULTS))
            .isEqualTo(expectedResults)

        assertThat(permissionManager.getGrantedHealthPermissions(TEST_APP_PACKAGE_NAME))
            .containsExactlyElementsIn(arrayOf(READ_SLEEP))
    }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    @Ignore("b/363994647 - flaky rotation test")
    fun requestFitnessAndAdditionalPermissions_userFixSomeFitness_onRotate_showsAdditional() {
        val startActivityIntent =
            getPermissionScreenIntent(
                arrayOf(READ_EXERCISE, READ_SLEEP, READ_HEALTH_DATA_IN_BACKGROUND)
            )
        permissionManager.revokeHealthPermission(TEST_APP_PACKAGE_NAME, READ_EXERCISE)
        permissionManager.revokeHealthPermission(TEST_APP_PACKAGE_NAME, READ_SLEEP)
        val scenario = launchActivityForResult<PermissionsActivity>(startActivityIntent)
        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(scrollToLastPosition<RecyclerView.ViewHolder>())
        onIdle()

        onView(withText("Sleep")).perform(click())
        onView(withText("Allow")).perform(click())
        onIdle()
        // At this point, READ_EXERCISE should be USER_FIXED
        assertThat(
                permissionManager
                    .getHealthPermissionsFlags(TEST_APP_PACKAGE_NAME, listOf(READ_EXERCISE))[
                        READ_EXERCISE]
            )
            .isEqualTo(FLAG_PERMISSION_USER_FIXED)

        scenario.onActivity { activity ->
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        // The fragment should have checked for the USER_FIXED permission but not end the activity
        // because we are in a flow
        onView(withText("Allow $TEST_APP_NAME to access data in the background?"))
            .check(matches(isDisplayed()))
        scenario.onActivity { activity ->
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        onView(withText("Allow")).perform(click())

        assertThat(scenario.result.resultCode).isEqualTo(Activity.RESULT_OK)
        val returnedIntent = scenario.result.resultData
        assertThat(returnedIntent.getStringArrayExtra(EXTRA_REQUEST_PERMISSIONS_NAMES))
            .isEqualTo(arrayOf(READ_EXERCISE, READ_SLEEP, READ_HEALTH_DATA_IN_BACKGROUND))
        val expectedResults = intArrayOf(PERMISSION_DENIED, PERMISSION_GRANTED, PERMISSION_GRANTED)
        assertThat(returnedIntent.getIntArrayExtra(EXTRA_REQUEST_PERMISSIONS_RESULTS))
            .isEqualTo(expectedResults)
    }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    @Ignore("b/363994647 - flaky rotation test")
    fun requestMedicalAndFitnessPermissions_userFixSomeMedical_onRotate_showsFitness() {
        val startActivityIntent =
            getPermissionScreenIntent(
                arrayOf(
                    READ_MEDICAL_DATA_VACCINES,
                    READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES,
                    READ_EXERCISE,
                    WRITE_SLEEP,
                    READ_HEALTH_DATA_IN_BACKGROUND,
                )
            )
        permissionManager.revokeHealthPermission(
            TEST_APP_PACKAGE_NAME,
            READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES,
        )
        permissionManager.revokeHealthPermission(TEST_APP_PACKAGE_NAME, WRITE_SLEEP)
        val scenario = launchActivityForResult<PermissionsActivity>(startActivityIntent)
        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(scrollToLastPosition<RecyclerView.ViewHolder>())
        onIdle()

        onView(withText("Immunizations")).check(matches(isDisplayed()))
        onView(withText("Immunizations")).perform(click())
        scenario.onActivity { activity: PermissionsActivity ->
            activity.findViewById<Button>(R.id.allow).callOnClick()
        }
        onIdle()
        // At this point, READ_MEDICAL_DATA_ALLERGY_INTOLERANCE should be USER_FIXED
        assertThat(
                permissionManager
                    .getHealthPermissionsFlags(
                        TEST_APP_PACKAGE_NAME,
                        listOf(READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES),
                    )[READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES]
            )
            .isEqualTo(FLAG_PERMISSION_USER_FIXED)

        scenario.onActivity { activity ->
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        onIdle()

        // The fragment should have checked for the USER_FIXED permission but not end the activity
        // because we are in a flow
        onView(withText("Allow $TEST_APP_NAME to access Health Connect?"))
            .check(matches(isDisplayed()))
        scenario.onActivity { activity ->
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        onView(withText("Exercise")).perform(scrollTo()).check(matches(isDisplayed()))
        onView(withText("Exercise")).perform(click())
        onView(withText("Allow")).perform(click())

        onView(withText("Allow $TEST_APP_NAME to access data in the background?"))
            .check(matches(isDisplayed()))

        onIdle()
        onView(withText("Allow")).perform(click())

        assertThat(scenario.result.resultCode).isEqualTo(Activity.RESULT_OK)
        val returnedIntent = scenario.result.resultData
        assertThat(returnedIntent.getStringArrayExtra(EXTRA_REQUEST_PERMISSIONS_NAMES))
            .isEqualTo(
                arrayOf(
                    READ_MEDICAL_DATA_VACCINES,
                    READ_MEDICAL_DATA_ALLERGIES_INTOLERANCES,
                    READ_EXERCISE,
                    WRITE_SLEEP,
                    READ_HEALTH_DATA_IN_BACKGROUND,
                )
            )
        val expectedResults =
            intArrayOf(
                PERMISSION_GRANTED,
                PERMISSION_DENIED,
                PERMISSION_GRANTED,
                PERMISSION_DENIED,
                PERMISSION_GRANTED,
            )
        assertThat(returnedIntent.getIntArrayExtra(EXTRA_REQUEST_PERMISSIONS_RESULTS))
            .isEqualTo(expectedResults)
    }

    private fun getPermissionScreenIntent(
        permissions: Array<String>,
        testAppName: String = TEST_APP_PACKAGE_NAME,
    ): Intent =
        Intent.makeMainActivity(ComponentName(context, PermissionsActivity::class.java))
            .putExtra(EXTRA_REQUEST_PERMISSIONS_NAMES, permissions)
            .putExtra(EXTRA_PACKAGE_NAME, testAppName)
            .addFlags(FLAG_ACTIVITY_NEW_TASK)
            .addFlags(FLAG_ACTIVITY_CLEAR_TASK)
}
