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
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.pm.PackageManager.EXTRA_REQUEST_PERMISSIONS_NAMES
import android.content.pm.PackageManager.EXTRA_REQUEST_PERMISSIONS_RESULTS
import android.content.pm.PackageManager.PERMISSION_DENIED
import android.health.connect.HealthPermissions.READ_HEART_RATE
import android.health.connect.HealthPermissions.READ_MEDICAL_DATA_VACCINES
import android.health.connect.HealthPermissions.READ_STEPS
import android.health.connect.HealthPermissions.WRITE_DISTANCE
import android.health.connect.HealthPermissions.WRITE_EXERCISE
import android.health.connect.HealthPermissions.WRITE_MEDICAL_DATA
import android.os.Build
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario.launchActivityForResult
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.scrollToLastPosition
import androidx.test.espresso.contrib.RecyclerViewActions.scrollToPosition
import androidx.test.espresso.matcher.RootMatchers.isDialog
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
import com.android.healthconnect.controller.navigation.TrampolineActivity
import com.android.healthconnect.controller.permissions.data.HealthPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.AdditionalPermission
import com.android.healthconnect.controller.permissions.data.HealthPermission.Companion.fromPermissionString
import com.android.healthconnect.controller.permissions.data.HealthPermission.FitnessPermission
import com.android.healthconnect.controller.permissions.data.PermissionState
import com.android.healthconnect.controller.permissions.request.AdditionalScreenState
import com.android.healthconnect.controller.permissions.request.FitnessScreenState
import com.android.healthconnect.controller.permissions.request.MedicalScreenState
import com.android.healthconnect.controller.permissions.request.PermissionsActivity
import com.android.healthconnect.controller.permissions.request.PermissionsActivityState
import com.android.healthconnect.controller.permissions.request.RequestPermissionViewModel
import com.android.healthconnect.controller.shared.HealthPermissionReader
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.tests.utils.NOW
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_PACKAGE_NAME
import com.android.healthconnect.controller.tests.utils.di.FakeDeviceInfoUtils
import com.android.healthconnect.controller.tests.utils.showOnboarding
import com.android.healthconnect.controller.tests.utils.toggleAnimation
import com.android.healthconnect.controller.utils.DeviceInfoUtils
import com.android.healthconnect.controller.utils.DeviceInfoUtilsModule
import com.android.healthconnect.controller.utils.logging.DataRestoreElement
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.MedicalWritePermissionPageElement
import com.android.healthconnect.controller.utils.logging.MigrationElement
import com.android.healthconnect.controller.utils.logging.PageName
import com.android.healthfitness.flags.Flags
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.anyString
import org.mockito.Mockito.atLeast
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.kotlin.anyArray
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Tests the display of the PermissionsActivity and all the cases where the activity is terminated
 * before displaying anything.
 */
@UninstallModules(DeviceInfoUtilsModule::class)
@HiltAndroidTest
class MockedPermissionsActivityTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)
    @get:Rule val setFlagsRule = SetFlagsRule()

    @BindValue
    val viewModel: RequestPermissionViewModel = mock(RequestPermissionViewModel::class.java)
    @BindValue val migrationViewModel: MigrationViewModel = mock(MigrationViewModel::class.java)
    @BindValue val healthConnectLogger: HealthConnectLogger = mock(HealthConnectLogger::class.java)
    @BindValue val deviceInfoUtils: DeviceInfoUtils = FakeDeviceInfoUtils()
    @BindValue
    val healthPermissionReader: HealthPermissionReader = mock(HealthPermissionReader::class.java)

    private lateinit var context: Context
    private lateinit var appMetadata: AppMetadata

    @Before
    fun setup() {
        hiltRule.inject()
        context = getInstrumentation().context
        appMetadata =
            AppMetadata(
                TEST_APP_PACKAGE_NAME,
                TEST_APP_NAME,
                context.getDrawable(R.drawable.health_connect_logo),
            )
        val permissionsList =
            listOf(
                fromPermissionString(READ_STEPS),
                fromPermissionString(READ_HEART_RATE),
                fromPermissionString(WRITE_DISTANCE),
                fromPermissionString(WRITE_EXERCISE),
            )
        whenever(viewModel.appMetadata).then { MutableLiveData(appMetadata) }
        whenever(viewModel.grantedFitnessPermissions).then {
            MutableLiveData(permissionsList.toSet())
        }
        whenever(viewModel.allFitnessPermissionsGranted).then { MutableLiveData(true) }
        whenever(viewModel.isAnyPermissionUserFixed(anyString(), anyArray())).thenReturn(false)
        whenever(viewModel.isAnyReadPermissionGranted()).thenReturn(true)
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
        whenever(viewModel.grantedAdditionalPermissions).then {
            MutableLiveData<Set<AdditionalPermission>>(setOf())
        }
        whenever(viewModel.medicalScreenState).then {
            MutableLiveData(MedicalScreenState.NoMedicalData)
        }
        whenever(viewModel.fitnessScreenState).then {
            MutableLiveData(FitnessScreenState.NoFitnessData)
        }
        whenever(viewModel.additionalScreenState).then {
            MutableLiveData(AdditionalScreenState.NoAdditionalData)
        }

        whenever(healthPermissionReader.isRationaleIntentDeclared(anyString())).thenReturn(true)

        (deviceInfoUtils as FakeDeviceInfoUtils).setHealthConnectAvailable(true)
        showOnboarding(context, false)
        // disable animations
        toggleAnimation(false)
    }

    @After
    fun teardown() {
        reset(healthConnectLogger)
        // enable animations
        toggleAnimation(true)
    }

    @Test
    fun whenHealthConnectNotAvailable_sendsResultCanceled() {
        (deviceInfoUtils as FakeDeviceInfoUtils).setHealthConnectAvailable(false)

        val scenario =
            launchActivityForResult<TrampolineActivity>(
                getPermissionScreenIntent(arrayOf(READ_STEPS))
            )

        assertThat(scenario.result.resultCode).isEqualTo(RESULT_CANCELED)
    }

    @Test
    fun whenNoPackageNameInIntent_sendsResultCanceled() {
        val intent =
            Intent.makeMainActivity(ComponentName(context, PermissionsActivity::class.java))
                .putExtra(EXTRA_REQUEST_PERMISSIONS_NAMES, arrayOf<String>())
                .addFlags(FLAG_ACTIVITY_NEW_TASK)
                .addFlags(FLAG_ACTIVITY_CLEAR_TASK)
        val scenario = launchActivityForResult<PermissionsActivity>(intent)

        assertThat(scenario.result.resultCode).isEqualTo(RESULT_CANCELED)
    }

    @Test
    fun whenNoRationaleIntentDeclared_sendsResultCanceled() {
        whenever(healthPermissionReader.isRationaleIntentDeclared(anyString())).thenReturn(false)
        whenever(viewModel.permissionsActivityState).then {
            MutableLiveData(PermissionsActivityState.ShowMedical(isWriteOnly = true))
        }
        val scenario =
            launchActivityForResult<TrampolineActivity>(
                getPermissionScreenIntent(arrayOf(READ_STEPS))
            )

        assertThat(scenario.result.resultCode).isEqualTo(RESULT_CANCELED)
    }

    @Test
    fun showMedicalPermissionRequest_withOnlyWritePermission() {
        whenever(viewModel.permissionsActivityState).then {
            MutableLiveData(PermissionsActivityState.ShowMedical(isWriteOnly = true))
        }
        whenever(viewModel.medicalScreenState).then {
            MutableLiveData(
                MedicalScreenState.ShowMedicalWrite(
                    appMetadata = appMetadata,
                    medicalPermissions =
                        listOf(
                            HealthPermission.MedicalPermission.fromPermissionString(
                                WRITE_MEDICAL_DATA
                            )
                        ),
                )
            )
        }
        val permissions = arrayOf(WRITE_MEDICAL_DATA)
        val startActivityIntent = getPermissionScreenIntent(permissions)

        launchActivityForResult<PermissionsActivity>(startActivityIntent)
        onView(withText("Allow $TEST_APP_NAME to access your health records?"))
            .check(matches(isDisplayed()))
        onView(
                withText(
                    "If you allow, $TEST_APP_NAME can share your health records with Health Connect."
                )
            )
            .check(matches(isDisplayed()))
        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(scrollToPosition<RecyclerView.ViewHolder>(2))
        onView(withText("Data to share includes")).check(matches(isDisplayed()))
        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(scrollToPosition<RecyclerView.ViewHolder>(3))
        val availableMedicalPermissionsString =
            "Allergies\n" +
                "Conditions\n" +
                "Lab results\n" +
                "Medications\n" +
                "Procedures\n" +
                "Vaccines\n" +
                "Vital signs"
        onView(withText(availableMedicalPermissionsString)).check(matches(isDisplayed()))
        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(scrollToLastPosition<RecyclerView.ViewHolder>())
        Espresso.onIdle()
        onView(
                withText(
                    "Sync your health records from your different apps and sources to keep " +
                        "them in one place"
                )
            )
            .perform(scrollTo())
        onView(
                withText(
                    "Sync your health records from your different apps and sources to keep " +
                        "them in one place"
                )
            )
            .check(matches(isDisplayed()))
        onView(withText("About health records")).check(matches(isDisplayed()))
    }

    @Test
    fun showMedicalPermissionRequest_correctLogging() {
        whenever(viewModel.permissionsActivityState).then {
            MutableLiveData(PermissionsActivityState.ShowMedical(isWriteOnly = true))
        }
        whenever(viewModel.medicalScreenState).then {
            MutableLiveData(
                MedicalScreenState.ShowMedicalWrite(
                    appMetadata = appMetadata,
                    medicalPermissions =
                        listOf(
                            HealthPermission.MedicalPermission.fromPermissionString(
                                WRITE_MEDICAL_DATA
                            )
                        ),
                )
            )
        }
        val permissions = arrayOf(WRITE_MEDICAL_DATA)
        val startActivityIntent = getPermissionScreenIntent(permissions)

        launchActivityForResult<PermissionsActivity>(startActivityIntent)

        verify(healthConnectLogger, atLeast(1))
            .setPageId(PageName.REQUEST_WRITE_MEDICAL_PERMISSION_PAGE)
        verify(healthConnectLogger).logPageImpression()
        verify(healthConnectLogger)
            .logImpression(MedicalWritePermissionPageElement.ALLOW_WRITE_HEALTH_RECORDS_BUTTON)
        verify(healthConnectLogger)
            .logImpression(MedicalWritePermissionPageElement.CANCEL_WRITE_HEALTH_RECORDS_BUTTON)
    }

    @Test
    fun showMedicalPermissionRequest_withReadAndWritePermissions() {
        whenever(viewModel.permissionsActivityState).then {
            MutableLiveData(PermissionsActivityState.ShowMedical(isWriteOnly = false))
        }
        whenever(viewModel.medicalScreenState).then {
            MutableLiveData(
                MedicalScreenState.ShowMedicalReadWrite(
                    appMetadata = appMetadata,
                    medicalPermissions =
                        listOf(READ_MEDICAL_DATA_VACCINES, WRITE_MEDICAL_DATA).map {
                            fromPermissionString(it) as HealthPermission.MedicalPermission
                        },
                )
            )
        }
        whenever(viewModel.allMedicalPermissionsGranted).then { MutableLiveData(true) }
        whenever(viewModel.grantedMedicalPermissions).then {
            MutableLiveData(setOf(WRITE_MEDICAL_DATA, READ_MEDICAL_DATA_VACCINES))
        }
        val permissions = arrayOf(WRITE_MEDICAL_DATA, READ_MEDICAL_DATA_VACCINES)
        val startActivityIntent = getPermissionScreenIntent(permissions)

        launchActivityForResult<PermissionsActivity>(startActivityIntent)
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
            .perform(scrollToPosition<RecyclerView.ViewHolder>(2))
        onView(withText("Allow all")).check(matches(isDisplayed()))
        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(scrollToLastPosition<RecyclerView.ViewHolder>())
        onView(withText("Vaccines")).check(matches(isDisplayed()))
        onView(withText("All health records")).check(matches(isDisplayed()))
    }

    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @Test
    fun showFitnessPermissionRequest_healthConnectBrand() {
        whenever(viewModel.permissionsActivityState).then {
            MutableLiveData(PermissionsActivityState.ShowFitness)
        }
        whenever(viewModel.fitnessScreenState).then {
            MutableLiveData(
                FitnessScreenState.ShowFitnessReadWrite(
                    appMetadata = appMetadata,
                    fitnessPermissions =
                        listOf(READ_STEPS, WRITE_DISTANCE).map {
                            fromPermissionString(it) as HealthPermission.FitnessPermission
                        },
                    hasMedical = false,
                    historyGranted = false,
                )
            )
        }
        whenever(viewModel.grantedFitnessPermissions).then {
            MutableLiveData(setOf(FitnessPermission.fromPermissionString(READ_STEPS)))
        }
        whenever(viewModel.allFitnessPermissionsGranted).then { MutableLiveData(false) }
        val permissions = arrayOf(READ_STEPS, WRITE_DISTANCE)
        val startActivityIntent = getPermissionScreenIntent(permissions)

        launchActivityForResult<PermissionsActivity>(startActivityIntent)
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
            .perform(scrollToPosition<RecyclerView.ViewHolder>(2))
        onView(withText("Allow all")).check(matches(isDisplayed()))
        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(scrollToLastPosition<RecyclerView.ViewHolder>())
        onView(withText("Steps")).check(matches(isDisplayed()))
        onView(withText("Distance")).check(matches(isDisplayed()))
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.BAKLAVA, codeName = "Baklava")
    @Test
    fun showFitnessPermissionRequest_healthFitnessBrand() {
        whenever(viewModel.permissionsActivityState).then {
            MutableLiveData(PermissionsActivityState.ShowFitness)
        }
        whenever(viewModel.fitnessScreenState).then {
            MutableLiveData(
                FitnessScreenState.ShowFitnessReadWrite(
                    appMetadata = appMetadata,
                    fitnessPermissions =
                        listOf(READ_STEPS, WRITE_DISTANCE).map {
                            fromPermissionString(it) as HealthPermission.FitnessPermission
                        },
                    hasMedical = false,
                    historyGranted = false,
                )
            )
        }
        whenever(viewModel.grantedFitnessPermissions).then {
            MutableLiveData(setOf(FitnessPermission.fromPermissionString(READ_STEPS)))
        }
        whenever(viewModel.allFitnessPermissionsGranted).then { MutableLiveData(false) }
        val permissions = arrayOf(READ_STEPS, WRITE_DISTANCE)
        val startActivityIntent = getPermissionScreenIntent(permissions)

        launchActivityForResult<PermissionsActivity>(startActivityIntent)
        onView(withText("Allow $TEST_APP_NAME to access your fitness and wellness data?"))
            .check(matches(isDisplayed()))
        onView(
                withText(
                    "Choose which fitness and wellness data this app can access. This includes data tracked and stored on this device, learn more"
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
            .perform(scrollToPosition<RecyclerView.ViewHolder>(2))
        onView(withText("Allow all")).check(matches(isDisplayed()))
        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(scrollToLastPosition<RecyclerView.ViewHolder>())
        onView(withText("Steps")).check(matches(isDisplayed()))
        onView(withText("Distance")).check(matches(isDisplayed()))
    }

    @Test
    fun showAdditionalPermissionRequest() {
        whenever(viewModel.permissionsActivityState).then {
            MutableLiveData(PermissionsActivityState.ShowAdditional(false))
        }
        whenever(viewModel.additionalScreenState).then {
            MutableLiveData(
                AdditionalScreenState.ShowCombined(
                    hasMedical = false,
                    appMetadata = appMetadata,
                    isMedicalReadGranted = false,
                    isFitnessReadGranted = true,
                    dataAccessDate = NOW,
                )
            )
        }
        whenever(viewModel.grantedAdditionalPermissions).then {
            MutableLiveData(setOf(AdditionalPermission.READ_HEALTH_DATA_HISTORY))
        }
        val permissions = arrayOf(READ_STEPS, WRITE_DISTANCE)
        val startActivityIntent = getPermissionScreenIntent(permissions)

        launchActivityForResult<PermissionsActivity>(startActivityIntent)
        onView(withText("Allow additional access for $TEST_APP_NAME?"))
            .check(matches(isDisplayed()))
        onView(withText("$TEST_APP_NAME also wants to access these Health Connect settings"))
            .check(matches(isDisplayed()))
        onView(withId(androidx.preference.R.id.recycler_view))
            .perform(scrollToLastPosition<RecyclerView.ViewHolder>())
        onView(withText("Access past data")).check(matches(isDisplayed()))
        onView(withText("Access data in the background")).check(matches(isDisplayed()))
    }

    @Test
    fun whenNoPermissions_sendsOkResult() {
        whenever(viewModel.permissionsActivityState).then {
            MutableLiveData(PermissionsActivityState.NoPermissions)
        }
        val startActivityIntent = getPermissionScreenIntent(arrayOf())
        val scenario = launchActivityForResult<PermissionsActivity>(startActivityIntent)
        assertThat(scenario.result.resultCode).isEqualTo(Activity.RESULT_OK)
        val returnedIntent = scenario.result.resultData
        assertThat(returnedIntent.getStringArrayExtra(EXTRA_REQUEST_PERMISSIONS_NAMES)).isEmpty()
        assertThat(returnedIntent.getIntArrayExtra(EXTRA_REQUEST_PERMISSIONS_RESULTS)).isEmpty()
    }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    fun whenPermissionUserFixed_noFlowConcluded_sendsResultOk() {
        whenever(viewModel.isAnyPermissionUserFixed(anyString(), anyArray())).thenReturn(true)
        whenever(viewModel.isFitnessPermissionRequestConcluded()).thenReturn(false)
        whenever(viewModel.isMedicalPermissionRequestConcluded()).thenReturn(false)
        whenever(viewModel.permissionsActivityState).then {
            MutableLiveData(PermissionsActivityState.ShowFitness)
        }
        whenever(viewModel.getPermissionGrants())
            .thenReturn(
                mutableMapOf(
                    fromPermissionString(READ_STEPS) to PermissionState.NOT_GRANTED,
                    fromPermissionString(WRITE_DISTANCE) to PermissionState.NOT_GRANTED,
                )
            )
        val startActivityIntent = getPermissionScreenIntent(arrayOf(READ_STEPS, WRITE_DISTANCE))
        val scenario = launchActivityForResult<PermissionsActivity>(startActivityIntent)
        assertThat(scenario.result.resultCode).isEqualTo(Activity.RESULT_OK)
        val returnedIntent = scenario.result.resultData
        assertThat(returnedIntent.getStringArrayExtra(EXTRA_REQUEST_PERMISSIONS_NAMES))
            .isEqualTo(arrayOf(READ_STEPS, WRITE_DISTANCE))
        assertThat(returnedIntent.getIntArrayExtra(EXTRA_REQUEST_PERMISSIONS_RESULTS))
            .isEqualTo(intArrayOf(PERMISSION_DENIED, PERMISSION_DENIED))
    }

    @Test
    @EnableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD, Flags.FLAG_PERSONAL_HEALTH_RECORD_DATABASE)
    fun whenPermissionUserFixed_flowConcluded_showsRequest() {
        whenever(viewModel.isAnyPermissionUserFixed(anyString(), anyArray())).thenReturn(true)
        whenever(viewModel.isFitnessPermissionRequestConcluded()).thenReturn(false)
        whenever(viewModel.isMedicalPermissionRequestConcluded()).thenReturn(true)
        whenever(viewModel.permissionsActivityState).then {
            MutableLiveData(PermissionsActivityState.ShowFitness)
        }
        whenever(viewModel.fitnessScreenState).then {
            MutableLiveData(
                FitnessScreenState.ShowFitnessReadWrite(
                    hasMedical = true,
                    appMetadata = appMetadata,
                    fitnessPermissions =
                        listOf(READ_STEPS, WRITE_DISTANCE).map {
                            fromPermissionString(it) as FitnessPermission
                        },
                    historyGranted = false,
                )
            )
        }
        whenever(viewModel.grantedFitnessPermissions).then {
            MutableLiveData(setOf(FitnessPermission.fromPermissionString(READ_STEPS)))
        }
        whenever(viewModel.allFitnessPermissionsGranted).then { MutableLiveData(false) }
        val permissions = arrayOf(READ_STEPS, WRITE_DISTANCE)
        val startActivityIntent = getPermissionScreenIntent(permissions)

        launchActivityForResult<PermissionsActivity>(startActivityIntent)
        Espresso.onIdle()
        onView(withText("Allow $TEST_APP_NAME to access fitness and wellness data?"))
            .check(matches(isDisplayed()))
    }

    @Test
    @DisableFlags(Flags.FLAG_PERSONAL_HEALTH_RECORD)
    fun whenPermissionUserFixed_sendsResultOk() {
        whenever(viewModel.isAnyPermissionUserFixed(anyString(), anyArray())).thenReturn(true)
        whenever(viewModel.permissionsActivityState).then {
            MutableLiveData(PermissionsActivityState.ShowFitness)
        }
        whenever(viewModel.getPermissionGrants())
            .thenReturn(
                mutableMapOf(
                    fromPermissionString(READ_STEPS) to PermissionState.NOT_GRANTED,
                    fromPermissionString(WRITE_DISTANCE) to PermissionState.NOT_GRANTED,
                )
            )
        val startActivityIntent = getPermissionScreenIntent(arrayOf(READ_STEPS, WRITE_DISTANCE))
        val scenario = launchActivityForResult<PermissionsActivity>(startActivityIntent)
        assertThat(scenario.result.resultCode).isEqualTo(Activity.RESULT_OK)
        val returnedIntent = scenario.result.resultData
        assertThat(returnedIntent.getStringArrayExtra(EXTRA_REQUEST_PERMISSIONS_NAMES))
            .isEqualTo(arrayOf(READ_STEPS, WRITE_DISTANCE))
        assertThat(returnedIntent.getIntArrayExtra(EXTRA_REQUEST_PERMISSIONS_RESULTS))
            .isEqualTo(intArrayOf(PERMISSION_DENIED, PERMISSION_DENIED))
    }

    @Test
    fun whenMigrationInProgress_showsMigrationInProgressDialog() {
        whenever(migrationViewModel.getCurrentMigrationUiState()).then {
            MigrationRestoreState(
                migrationUiState = MigrationUiState.IN_PROGRESS,
                dataRestoreState = DataRestoreUiState.IDLE,
                dataRestoreError = DataRestoreUiError.ERROR_NONE,
            )
        }
        whenever(migrationViewModel.migrationState).then {
            MutableLiveData(
                WithData(
                    MigrationRestoreState(
                        migrationUiState = MigrationUiState.IN_PROGRESS,
                        dataRestoreState = DataRestoreUiState.IDLE,
                        dataRestoreError = DataRestoreUiError.ERROR_NONE,
                    )
                )
            )
        }
        whenever(viewModel.permissionsActivityState).then {
            MutableLiveData(PermissionsActivityState.ShowFitness)
        }
        whenever(viewModel.fitnessScreenState).then {
            MutableLiveData(
                FitnessScreenState.ShowFitnessReadWrite(
                    hasMedical = true,
                    appMetadata = appMetadata,
                    fitnessPermissions =
                        listOf(READ_STEPS, WRITE_DISTANCE).map {
                            fromPermissionString(it) as FitnessPermission
                        },
                    historyGranted = false,
                )
            )
        }
        whenever(viewModel.grantedFitnessPermissions).then {
            MutableLiveData(setOf(FitnessPermission.fromPermissionString(READ_STEPS)))
        }
        whenever(viewModel.allFitnessPermissionsGranted).then { MutableLiveData(false) }

        val permissions = arrayOf(READ_STEPS, WRITE_DISTANCE)
        val startActivityIntent = getPermissionScreenIntent(permissions)

        val scenario = launchActivityForResult<PermissionsActivity>(startActivityIntent)
        onView(withText("Health Connect integration in progress"))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        onView(
                withText(
                    "Health Connect is being integrated with the Android system.\n\nYou'll get a notification when the process is complete and you can use $TEST_APP_NAME with Health Connect."
                )
            )
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        onView(withText("Got it")).inRoot(isDialog()).check(matches(isDisplayed()))
        verify(healthConnectLogger)
            .logImpression(MigrationElement.MIGRATION_IN_PROGRESS_DIALOG_CONTAINER)
        verify(healthConnectLogger)
            .logImpression(MigrationElement.MIGRATION_IN_PROGRESS_DIALOG_BUTTON)

        onView(withText("Got it")).inRoot(isDialog()).perform(click())
        verify(healthConnectLogger)
            .logInteraction(MigrationElement.MIGRATION_IN_PROGRESS_DIALOG_BUTTON)

        // Needed to make sure activity has finished
        Thread.sleep(2_000)
        assertEquals(Lifecycle.State.DESTROYED, scenario.state)
    }

    @Test
    fun whenRestoreInProgress_showsRestoreInProgressDialog() {
        whenever(migrationViewModel.getCurrentMigrationUiState()).then {
            MigrationRestoreState(
                migrationUiState = MigrationUiState.IDLE,
                dataRestoreState = DataRestoreUiState.IN_PROGRESS,
                dataRestoreError = DataRestoreUiError.ERROR_NONE,
            )
        }
        whenever(migrationViewModel.migrationState).then {
            MutableLiveData(
                WithData(
                    MigrationRestoreState(
                        migrationUiState = MigrationUiState.IDLE,
                        dataRestoreState = DataRestoreUiState.IN_PROGRESS,
                        dataRestoreError = DataRestoreUiError.ERROR_NONE,
                    )
                )
            )
        }
        whenever(viewModel.permissionsActivityState).then {
            MutableLiveData(PermissionsActivityState.ShowFitness)
        }
        whenever(viewModel.fitnessScreenState).then {
            MutableLiveData(
                FitnessScreenState.ShowFitnessReadWrite(
                    hasMedical = true,
                    appMetadata = appMetadata,
                    fitnessPermissions =
                        listOf(READ_STEPS, WRITE_DISTANCE).map {
                            fromPermissionString(it) as FitnessPermission
                        },
                    historyGranted = false,
                )
            )
        }
        whenever(viewModel.grantedFitnessPermissions).then {
            MutableLiveData(setOf(FitnessPermission.fromPermissionString(READ_STEPS)))
        }
        whenever(viewModel.allFitnessPermissionsGranted).then { MutableLiveData(false) }
        val permissions = arrayOf(READ_STEPS, WRITE_DISTANCE)
        val startActivityIntent = getPermissionScreenIntent(permissions)
        val scenario = launchActivityForResult<PermissionsActivity>(startActivityIntent)

        Espresso.onIdle()
        onView(withText("Health Connect restore in progress"))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        onView(
                withText(
                    "Health Connect is restoring data and permissions. This may take some time to complete."
                )
            )
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        onView(withText("Got it")).inRoot(isDialog()).check(matches(isDisplayed()))
        verify(healthConnectLogger)
            .logImpression(DataRestoreElement.RESTORE_IN_PROGRESS_DIALOG_CONTAINER)
        verify(healthConnectLogger)
            .logImpression(DataRestoreElement.RESTORE_IN_PROGRESS_DIALOG_BUTTON)

        onView(withText("Got it")).inRoot(isDialog()).perform(click())
        verify(healthConnectLogger)
            .logInteraction(DataRestoreElement.RESTORE_IN_PROGRESS_DIALOG_BUTTON)

        // Needed to makes sure activity has finished
        Thread.sleep(2_000)
        assertEquals(Lifecycle.State.DESTROYED, scenario.state)
    }

    @Test
    fun whenMigrationPending_showsMigrationPendingDialog() {
        whenever(migrationViewModel.getCurrentMigrationUiState()).then {
            MigrationRestoreState(
                migrationUiState = MigrationUiState.APP_UPGRADE_REQUIRED,
                dataRestoreState = DataRestoreUiState.IDLE,
                dataRestoreError = DataRestoreUiError.ERROR_NONE,
            )
        }
        whenever(migrationViewModel.migrationState).then {
            MutableLiveData(
                WithData(
                    MigrationRestoreState(
                        migrationUiState = MigrationUiState.APP_UPGRADE_REQUIRED,
                        dataRestoreState = DataRestoreUiState.IDLE,
                        dataRestoreError = DataRestoreUiError.ERROR_NONE,
                    )
                )
            )
        }
        whenever(viewModel.permissionsActivityState).then {
            MutableLiveData(PermissionsActivityState.ShowFitness)
        }
        whenever(viewModel.fitnessScreenState).then {
            MutableLiveData(
                FitnessScreenState.ShowFitnessReadWrite(
                    hasMedical = true,
                    appMetadata = appMetadata,
                    fitnessPermissions =
                        listOf(READ_STEPS, WRITE_DISTANCE).map {
                            fromPermissionString(it) as FitnessPermission
                        },
                    historyGranted = false,
                )
            )
        }
        whenever(viewModel.grantedFitnessPermissions).then {
            MutableLiveData(setOf(FitnessPermission.fromPermissionString(READ_STEPS)))
        }
        whenever(viewModel.allFitnessPermissionsGranted).then { MutableLiveData(false) }
        val permissions = arrayOf(READ_STEPS, WRITE_DISTANCE)
        val startActivityIntent = getPermissionScreenIntent(permissions)

        launchActivityForResult<PermissionsActivity>(startActivityIntent)

        onView(
                withText(
                    "Health Connect is ready to be integrated with your Android system. If you give $TEST_APP_NAME access now, some features may not work until integration is complete."
                )
            )
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        // TODO (b/322495982) check navigation to Migration activity
        onView(withText("Start integration")).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withText("Continue")).inRoot(isDialog()).check(matches(isDisplayed()))
        verify(healthConnectLogger)
            .logImpression(MigrationElement.MIGRATION_PENDING_DIALOG_CONTAINER)
        verify(healthConnectLogger)
            .logImpression(MigrationElement.MIGRATION_PENDING_DIALOG_CONTINUE_BUTTON)
        verify(healthConnectLogger)
            .logImpression(MigrationElement.MIGRATION_PENDING_DIALOG_CANCEL_BUTTON)

        onView(withText("Continue")).inRoot(isDialog()).perform(click())
        onView(withText("Continue")).check(doesNotExist())
        verify(healthConnectLogger)
            .logInteraction(MigrationElement.MIGRATION_PENDING_DIALOG_CONTINUE_BUTTON)
    }

    private fun getPermissionScreenIntent(permissions: Array<String>): Intent =
        Intent.makeMainActivity(ComponentName(context, PermissionsActivity::class.java))
            .putExtra(EXTRA_REQUEST_PERMISSIONS_NAMES, permissions)
            .putExtra(Intent.EXTRA_PACKAGE_NAME, TEST_APP_PACKAGE_NAME)
            .addFlags(FLAG_ACTIVITY_NEW_TASK)
            .addFlags(FLAG_ACTIVITY_CLEAR_TASK)
}
