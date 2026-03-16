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

package com.android.healthconnect.controller.tests.backuprestore

import android.Manifest
import android.Manifest.permission.BACKUP
import android.Manifest.permission.BACKUP_HEALTH_CONNECT_DATA_AND_SETTINGS
import android.app.Activity
import android.app.Instrumentation.ActivityResult
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.lifecycle.MutableLiveData
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.backuprestore.BackupAndRestoreSettingsFragment
import com.android.healthconnect.controller.exportimport.ExportSetupActivity
import com.android.healthconnect.controller.exportimport.ImportFlowActivity
import com.android.healthconnect.controller.exportimport.api.DocumentProviders
import com.android.healthconnect.controller.exportimport.api.ExportFrequency
import com.android.healthconnect.controller.exportimport.api.ExportSettings
import com.android.healthconnect.controller.exportimport.api.ExportSettingsViewModel
import com.android.healthconnect.controller.exportimport.api.ExportStatusViewModel
import com.android.healthconnect.controller.exportimport.api.ImportFlowViewModel
import com.android.healthconnect.controller.exportimport.api.ImportStatusViewModel
import com.android.healthconnect.controller.exportimport.api.ImportUiState
import com.android.healthconnect.controller.exportimport.api.ImportUiStatus
import com.android.healthconnect.controller.exportimport.api.ScheduledExportUiState
import com.android.healthconnect.controller.exportimport.api.ScheduledExportUiStatus
import com.android.healthconnect.controller.tests.TestActivity
import com.android.healthconnect.controller.tests.utils.InstantTaskExecutorRule
import com.android.healthconnect.controller.tests.utils.NOW
import com.android.healthconnect.controller.tests.utils.TestTimeSource
import com.android.healthconnect.controller.tests.utils.di.FakeDeviceInfoUtils
import com.android.healthconnect.controller.tests.utils.launchFragment
import com.android.healthconnect.controller.utils.DeviceInfoUtils
import com.android.healthconnect.controller.utils.DeviceInfoUtilsModule
import com.android.healthconnect.controller.utils.ToastManager
import com.android.healthconnect.controller.utils.ToastManagerModule
import com.android.healthconnect.controller.utils.logging.BackupAndRestoreElement
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthfitness.flags.Flags
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.firstValue
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@UninstallModules(DeviceInfoUtilsModule::class, ToastManagerModule::class)
@RunWith(AndroidJUnit4::class)
class BackupAndRestoreSettingsFragmentTest {

    companion object {
        private const val TEST_EXPORT_PERIOD_IN_DAYS = 1
        private const val TEST_LAST_EXPORT_APP_NAME = "Drive"
        private const val TEST_LAST_EXPORT_FILE_NAME = "healthconnect.zip"
        private const val TEST_LAST_IMPORT_URI = "content://com.android.documents.testFile"
        private const val IMPORT_FILE_URI_KEY = "selectedUri"
        private val TEST_LAST_IMPORT_COMPLETION_TIME = Instant.parse("2022-09-20T07:06:05.432Z")
    }

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule val hiltRule = HiltAndroidRule(this)
    @get:Rule val instantTaskExecutorRule = InstantTaskExecutorRule()
    @get:Rule val setFlagsRule = SetFlagsRule()

    // TODO: b/348591669 - Replace the mock with a fake and investigate the UI tests.
    @BindValue val exportSettingsViewModel: ExportSettingsViewModel = mock()
    @BindValue val exportStatusViewModel: ExportStatusViewModel = mock()

    @BindValue val importStatusViewModel: ImportStatusViewModel = mock()

    @BindValue val importFlowViewModel: ImportFlowViewModel = mock()
    @BindValue var toastManager: ToastManager = mock()
    @BindValue val timeSource = TestTimeSource
    @BindValue val healthConnectLogger: HealthConnectLogger = mock()
    @BindValue val deviceInfoUtils: DeviceInfoUtils = FakeDeviceInfoUtils()
    private val fakeDeviceInfoUtils = deviceInfoUtils as FakeDeviceInfoUtils

    private var previousDefaultTimeZone: TimeZone? = null
    private var previousLocale: Locale? = null

    private lateinit var navHostController: TestNavHostController
    private lateinit var context: Context

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        Dispatchers.setMain(testDispatcher)

        previousDefaultTimeZone = TimeZone.getDefault()
        previousLocale = Locale.getDefault()

        Locale.setDefault(Locale.US)
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("UTC")))

        hiltRule.inject()
        // Required for aconfig flag reading for tests run on pre V devices
        InstrumentationRegistry.getInstrumentation()
            .uiAutomation
            .adoptShellPermissionIdentity(Manifest.permission.READ_DEVICE_CONFIG)
        context = InstrumentationRegistry.getInstrumentation().context
        navHostController = TestNavHostController(context)

        Intents.init()

        whenever(importStatusViewModel.storedImportStatus).then {
            MutableLiveData(
                ImportUiStatus.WithData(
                    ImportUiState(
                        dataImportState = ImportUiState.DataImportState.DATA_IMPORT_ERROR_NONE
                    )
                )
            )
        }
        whenever(exportStatusViewModel.storedScheduledExportStatus).then {
            MutableLiveData(
                ScheduledExportUiStatus.WithData(
                    ScheduledExportUiState(
                        dataExportError =
                            ScheduledExportUiState.DataExportError.DATA_EXPORT_ERROR_NONE,
                        periodInDays = 0,
                        lastExportFileName = TEST_LAST_EXPORT_FILE_NAME,
                        lastExportAppName = TEST_LAST_EXPORT_APP_NAME,
                    )
                )
            )
        }
        whenever(importFlowViewModel.lastImportCompletionInstant).then {
            MutableLiveData(TEST_LAST_IMPORT_COMPLETION_TIME)
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        reset(healthConnectLogger)
        fakeDeviceInfoUtils.reset()
        Intents.release()

        TimeZone.setDefault(previousDefaultTimeZone)
        previousLocale?.let { locale -> Locale.setDefault(locale) }
    }

    @Test
    fun backupAndRestoreSettingsFragmentInit_showsFragmentCorrectly() {
        whenever(exportStatusViewModel.storedScheduledExportStatus).then {
            MutableLiveData(
                ScheduledExportUiStatus.WithData(
                    ScheduledExportUiState(
                        NOW,
                        ScheduledExportUiState.DataExportError.DATA_EXPORT_ERROR_NONE,
                        TEST_EXPORT_PERIOD_IN_DAYS,
                        TEST_LAST_EXPORT_FILE_NAME,
                        TEST_LAST_EXPORT_APP_NAME,
                    )
                )
            )
        }
        whenever(exportSettingsViewModel.storedExportSettings).then {
            MutableLiveData(ExportSettings.WithData(ExportFrequency.EXPORT_FREQUENCY_WEEKLY))
        }

        launchFragment<BackupAndRestoreSettingsFragment>(Bundle()).use {
            onView(withText("Scheduled export")).check(matches(isDisplayed()))
            onView(withText("Import data")).check(matches(isDisplayed()))
            onView(withText("Restore data from a previously exported file"))
                .check(matches(isDisplayed()))
            onView(withText("Export lets you save your data so you can transfer it to a new phone"))
                .check(matches(isDisplayed()))
            onView(withText("About backup and restore")).check(matches(isDisplayed()))
            onView(withText("Drive • healthconnect.zip")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun backupAndRestoreSettingsFragment_impressionsLogged() {
        whenever(exportSettingsViewModel.storedExportSettings).then {
            MutableLiveData(ExportSettings.WithData(ExportFrequency.EXPORT_FREQUENCY_DAILY))
        }

        launchFragment<BackupAndRestoreSettingsFragment>(Bundle()).use {
            verify(healthConnectLogger).logPageImpression()
            verify(healthConnectLogger)
                .logImpression(BackupAndRestoreElement.SCHEDULED_EXPORT_BUTTON)
            verify(healthConnectLogger).logImpression(BackupAndRestoreElement.RESTORE_DATA_BUTTON)
        }
    }

    @Test
    fun backupAndRestoreSettingsFragment_withNoLastSuccessfulDate_doesNotShowLastExportTime() {
        whenever(exportSettingsViewModel.storedExportSettings).then {
            MutableLiveData(ExportSettings.WithData(ExportFrequency.EXPORT_FREQUENCY_WEEKLY))
        }
        whenever(exportStatusViewModel.storedScheduledExportStatus).then {
            MutableLiveData(
                ScheduledExportUiStatus.WithData(
                    ScheduledExportUiState(
                        lastSuccessfulExportTime = null,
                        dataExportError =
                            ScheduledExportUiState.DataExportError.DATA_EXPORT_ERROR_NONE,
                        periodInDays = TEST_EXPORT_PERIOD_IN_DAYS,
                    )
                )
            )
        }
        launchFragment<BackupAndRestoreSettingsFragment>(Bundle()).use {
            onView(withText("Last export: Oct 20, 7:06 AM")).check(doesNotExist())
        }
    }

    @Test
    fun backupAndRestoreSettingsFragment_withNoLastExport_showsCorrectMessage() {
        whenever(exportSettingsViewModel.storedExportSettings).then {
            MutableLiveData(ExportSettings.WithData(ExportFrequency.EXPORT_FREQUENCY_WEEKLY))
        }
        whenever(exportStatusViewModel.storedScheduledExportStatus).then {
            MutableLiveData(
                ScheduledExportUiStatus.WithData(
                    ScheduledExportUiState(
                        lastSuccessfulExportTime = null,
                        lastFailedExportTime = null,
                        dataExportError =
                            ScheduledExportUiState.DataExportError.DATA_EXPORT_ERROR_NONE,
                        periodInDays = TEST_EXPORT_PERIOD_IN_DAYS,
                    )
                )
            )
        }
        launchFragment<BackupAndRestoreSettingsFragment>(Bundle()).use {
            onView(withText("Last export: none")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun backupAndRestoreSettingsFragment_lastExportWithin1Minute_showsLastExportAsNow() {
        whenever(exportSettingsViewModel.storedExportSettings).then {
            MutableLiveData(ExportSettings.WithData(ExportFrequency.EXPORT_FREQUENCY_WEEKLY))
        }
        whenever(exportStatusViewModel.storedScheduledExportStatus).then {
            MutableLiveData(
                ScheduledExportUiStatus.WithData(
                    ScheduledExportUiState(
                        // The fake 'now' is 2022-10-20T07:06:05.432Z.
                        lastSuccessfulExportTime = Instant.parse("2022-10-20T07:05:35.432Z"),
                        dataExportError =
                            ScheduledExportUiState.DataExportError.DATA_EXPORT_ERROR_NONE,
                        periodInDays = TEST_EXPORT_PERIOD_IN_DAYS,
                    )
                )
            )
        }
        launchFragment<BackupAndRestoreSettingsFragment>(Bundle()).use {
            onView(withText("Last export: now")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun backupAndRestoreSettingsFragment_lastExportBetween1MinAnd1Hour_showsLastExportAsXMinutesAgo() {
        whenever(exportSettingsViewModel.storedExportSettings).then {
            MutableLiveData(ExportSettings.WithData(ExportFrequency.EXPORT_FREQUENCY_WEEKLY))
        }
        whenever(exportStatusViewModel.storedScheduledExportStatus).then {
            MutableLiveData(
                ScheduledExportUiStatus.WithData(
                    ScheduledExportUiState(
                        // The fake 'now' is 2022-10-20T07:06:05.432Z.
                        lastSuccessfulExportTime = Instant.parse("2022-10-20T06:36:05.432Z"),
                        dataExportError =
                            ScheduledExportUiState.DataExportError.DATA_EXPORT_ERROR_NONE,
                        periodInDays = TEST_EXPORT_PERIOD_IN_DAYS,
                    )
                )
            )
        }
        launchFragment<BackupAndRestoreSettingsFragment>(Bundle()).use {
            onView(withText("Last export: 30 minutes ago")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun backupAndRestoreSettingsFragment_lastExportAt1MinAgo_showsLastExportAs1MinuteAgo() {
        whenever(exportSettingsViewModel.storedExportSettings).then {
            MutableLiveData(ExportSettings.WithData(ExportFrequency.EXPORT_FREQUENCY_WEEKLY))
        }
        whenever(exportStatusViewModel.storedScheduledExportStatus).then {
            MutableLiveData(
                ScheduledExportUiStatus.WithData(
                    ScheduledExportUiState(
                        // The fake 'now' is 2022-10-20T07:06:05.432Z.
                        lastSuccessfulExportTime = Instant.parse("2022-10-20T07:05:05.432Z"),
                        dataExportError =
                            ScheduledExportUiState.DataExportError.DATA_EXPORT_ERROR_NONE,
                        periodInDays = TEST_EXPORT_PERIOD_IN_DAYS,
                    )
                )
            )
        }
        launchFragment<BackupAndRestoreSettingsFragment>(Bundle()).use {
            onView(withText("Last export: 1 minute ago")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun backupAndRestoreSettingsFragment_lastExportBetween1HourAnd1Day_showsLastExportAsXHoursAgo() {
        whenever(exportSettingsViewModel.storedExportSettings).then {
            MutableLiveData(ExportSettings.WithData(ExportFrequency.EXPORT_FREQUENCY_WEEKLY))
        }
        whenever(exportStatusViewModel.storedScheduledExportStatus).then {
            MutableLiveData(
                ScheduledExportUiStatus.WithData(
                    ScheduledExportUiState(
                        // The fake 'now' is 2022-10-20T07:06:05.432Z.
                        lastSuccessfulExportTime = Instant.parse("2022-10-19T08:06:05.432Z"),
                        dataExportError =
                            ScheduledExportUiState.DataExportError.DATA_EXPORT_ERROR_NONE,
                        periodInDays = TEST_EXPORT_PERIOD_IN_DAYS,
                    )
                )
            )
        }
        launchFragment<BackupAndRestoreSettingsFragment>(Bundle()).use {
            onView(withText("Last export: 23 hours ago")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun backupAndRestoreSettingsFragment_lastExportAt1HourAgo_showsLastExportAs1HourAgo() {
        whenever(exportSettingsViewModel.storedExportSettings).then {
            MutableLiveData(ExportSettings.WithData(ExportFrequency.EXPORT_FREQUENCY_WEEKLY))
        }
        whenever(exportStatusViewModel.storedScheduledExportStatus).then {
            MutableLiveData(
                ScheduledExportUiStatus.WithData(
                    ScheduledExportUiState(
                        // The fake 'now' is 2022-10-20T07:06:05.432Z.
                        lastSuccessfulExportTime = Instant.parse("2022-10-20T06:06:05.432Z"),
                        dataExportError =
                            ScheduledExportUiState.DataExportError.DATA_EXPORT_ERROR_NONE,
                        periodInDays = TEST_EXPORT_PERIOD_IN_DAYS,
                    )
                )
            )
        }
        launchFragment<BackupAndRestoreSettingsFragment>(Bundle()).use {
            onView(withText("Last export: 1 hour ago")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun backupAndRestoreSettingsFragment_lastExportBetween1DayAnd1Year_showsCorrectDate() {
        whenever(exportSettingsViewModel.storedExportSettings).then {
            MutableLiveData(ExportSettings.WithData(ExportFrequency.EXPORT_FREQUENCY_WEEKLY))
        }
        whenever(exportStatusViewModel.storedScheduledExportStatus).then {
            MutableLiveData(
                ScheduledExportUiStatus.WithData(
                    ScheduledExportUiState(
                        // The fake 'now' is 2022-10-20T07:06:05.432Z.
                        lastSuccessfulExportTime = Instant.parse("2021-12-20T01:06:05.432Z"),
                        dataExportError =
                            ScheduledExportUiState.DataExportError.DATA_EXPORT_ERROR_NONE,
                        periodInDays = TEST_EXPORT_PERIOD_IN_DAYS,
                    )
                )
            )
        }
        launchFragment<BackupAndRestoreSettingsFragment>(Bundle()).use {
            onView(withText("Last export: Dec 20, 1:06 AM")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun backupAndRestoreSettingsFragment_lastExportAfter1Year_showsCorrectDate() {
        whenever(exportSettingsViewModel.storedExportSettings).then {
            MutableLiveData(ExportSettings.WithData(ExportFrequency.EXPORT_FREQUENCY_WEEKLY))
        }
        whenever(exportStatusViewModel.storedScheduledExportStatus).then {
            MutableLiveData(
                ScheduledExportUiStatus.WithData(
                    ScheduledExportUiState(
                        // The fake 'now' is 2022-10-20T07:06:05.432Z.
                        lastSuccessfulExportTime = Instant.parse("2021-10-20T07:06:05.432Z"),
                        dataExportError =
                            ScheduledExportUiState.DataExportError.DATA_EXPORT_ERROR_NONE,
                        periodInDays = TEST_EXPORT_PERIOD_IN_DAYS,
                    )
                )
            )
        }
        launchFragment<BackupAndRestoreSettingsFragment>(Bundle()).use {
            onView(withText("Last export: October 20, 2021")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun backupAndRestoreSettingsFragment_whenOnlyAppNameIsAvailable_showsAppName() {
        whenever(exportSettingsViewModel.storedExportSettings).then {
            MutableLiveData(ExportSettings.WithData(ExportFrequency.EXPORT_FREQUENCY_WEEKLY))
        }
        whenever(exportStatusViewModel.storedScheduledExportStatus).then {
            MutableLiveData(
                ScheduledExportUiStatus.WithData(
                    ScheduledExportUiState(
                        lastSuccessfulExportTime = NOW,
                        dataExportError =
                            ScheduledExportUiState.DataExportError.DATA_EXPORT_ERROR_NONE,
                        periodInDays = TEST_EXPORT_PERIOD_IN_DAYS,
                        lastExportAppName = TEST_LAST_EXPORT_APP_NAME,
                    )
                )
            )
        }
        launchFragment<BackupAndRestoreSettingsFragment>(Bundle()).use {
            onView(withText("Drive")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun backupAndRestoreSettingsFragment_whenOnlyFileNameIsAvailable_showsFileName() {
        whenever(exportSettingsViewModel.storedExportSettings).then {
            MutableLiveData(ExportSettings.WithData(ExportFrequency.EXPORT_FREQUENCY_WEEKLY))
        }
        whenever(exportStatusViewModel.storedScheduledExportStatus).then {
            MutableLiveData(
                ScheduledExportUiStatus.WithData(
                    ScheduledExportUiState(
                        lastSuccessfulExportTime = NOW,
                        dataExportError =
                            ScheduledExportUiState.DataExportError.DATA_EXPORT_ERROR_NONE,
                        periodInDays = TEST_EXPORT_PERIOD_IN_DAYS,
                        lastExportFileName = TEST_LAST_EXPORT_FILE_NAME,
                    )
                )
            )
        }
        launchFragment<BackupAndRestoreSettingsFragment>(Bundle()).use {
            onView(withText("healthconnect.zip")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun backupAndRestoreSettingsFragment_whenImportStarted_importPreferenceDisabled() = runTest {
        whenever(exportSettingsViewModel.storedExportSettings).then {
            MutableLiveData(ExportSettings.WithData(ExportFrequency.EXPORT_FREQUENCY_NEVER))
        }
        whenever(exportSettingsViewModel.documentProviders).then {
            MutableLiveData(DocumentProviders.WithData(listOf()))
        }

        val expectedResult =
            ActivityResult(
                Activity.RESULT_OK,
                Intent().putExtra(IMPORT_FILE_URI_KEY, TEST_LAST_IMPORT_URI),
            )
        intending(hasComponent(ImportFlowActivity::class.java.name)).respondWith(expectedResult)

        launchFragment<BackupAndRestoreSettingsFragment>(Bundle()).use {
            onView(withText("Import data")).check(matches(isEnabled()))
            onView(withText("Import data")).perform(click())
            onView(withText("Import data")).check(matches(not(isEnabled())))

            intended(hasComponent(ImportFlowActivity::class.java.name))
            verify(importFlowViewModel).triggerImportOfSelectedFile(Uri.parse(TEST_LAST_IMPORT_URI))
        }
    }

    @Test
    fun backupAndRestoreSettingsFragment_whenImportTriggered_importStatusToastsShown() {
        whenever(exportSettingsViewModel.storedExportSettings).then {
            MutableLiveData(ExportSettings.WithData(ExportFrequency.EXPORT_FREQUENCY_NEVER))
        }
        whenever(exportSettingsViewModel.documentProviders).then {
            MutableLiveData(DocumentProviders.WithData(listOf()))
        }
        whenever(importFlowViewModel.setLastCompletionInstant(Instant.now())).then {
            MutableLiveData(Instant.now())
        }

        val expectedInProgressMessage: Int = R.string.import_in_progress_toast_text
        val expectedCompleteMessage: Int = R.string.import_complete_toast_text

        val expectedResult =
            ActivityResult(
                Activity.RESULT_OK,
                Intent().putExtra(IMPORT_FILE_URI_KEY, TEST_LAST_IMPORT_URI),
            )
        intending(hasComponent(ImportFlowActivity::class.java.name)).respondWith(expectedResult)

        launchFragment<BackupAndRestoreSettingsFragment>(Bundle()).use { scenario ->
            onView(withText("Import data")).perform(click())

            intended(hasComponent(ImportFlowActivity::class.java.name))
            verify(importFlowViewModel).triggerImportOfSelectedFile(Uri.parse(TEST_LAST_IMPORT_URI))

            scenario.onActivity { activity: TestActivity ->
                verify(toastManager).showToast(eq(activity), eq(expectedInProgressMessage), any())
                verify(toastManager).showToast(eq(activity), eq(expectedCompleteMessage), any())
            }
        }
    }

    @Test
    fun backupAndRestoreSettingsFragment_importInstantNotSet_importStatusToastsNotShown() {
        whenever(exportSettingsViewModel.storedExportSettings).then {
            MutableLiveData(ExportSettings.WithData(ExportFrequency.EXPORT_FREQUENCY_NEVER))
        }
        whenever(exportSettingsViewModel.documentProviders).then {
            MutableLiveData(DocumentProviders.WithData(listOf()))
        }
        whenever(importFlowViewModel.lastImportCompletionInstant).then { MutableLiveData(null) }
        val completeMessage: Int = R.string.import_complete_toast_text
        val expectedResult =
            ActivityResult(
                Activity.RESULT_OK,
                Intent().putExtra(IMPORT_FILE_URI_KEY, TEST_LAST_IMPORT_URI),
            )
        intending(hasComponent(ImportFlowActivity::class.java.name)).respondWith(expectedResult)
        launchFragment<BackupAndRestoreSettingsFragment>(Bundle()).use { scenario ->
            onView(withText("Import data")).perform(click())
            intended(hasComponent(ImportFlowActivity::class.java.name))

            verify(importFlowViewModel).triggerImportOfSelectedFile(Uri.parse(TEST_LAST_IMPORT_URI))
            scenario.onActivity { activity: TestActivity ->
                verify(toastManager, never()).showToast(eq(activity), eq(completeMessage), any())
            }
        }
    }

    @Test
    fun backupAndRestoreSettingsFragment_clicksImportData_navigatesToImportFlowActivity() {
        whenever(exportSettingsViewModel.storedExportSettings).then {
            MutableLiveData(ExportSettings.WithData(ExportFrequency.EXPORT_FREQUENCY_NEVER))
        }
        whenever(exportSettingsViewModel.documentProviders).then {
            MutableLiveData(DocumentProviders.WithData(listOf()))
        }

        val expectedResult =
            ActivityResult(
                Activity.RESULT_OK,
                Intent().putExtra(IMPORT_FILE_URI_KEY, TEST_LAST_IMPORT_URI),
            )
        intending(hasComponent(ImportFlowActivity::class.java.name)).respondWith(expectedResult)

        launchFragment<BackupAndRestoreSettingsFragment>(Bundle()).use {
            onView(withText("Import data")).check(matches(isDisplayed()))
            onView(withText("Import data")).check(matches(isEnabled()))
            onView(withText("Import data")).perform(click())

            intended(hasComponent(ImportFlowActivity::class.java.name))

            verify(healthConnectLogger).logInteraction(BackupAndRestoreElement.RESTORE_DATA_BUTTON)
        }
    }

    @Test
    fun backupAndRestoreSettingsFragment_clicksScheduledExportWhenItIsOff_navigatesToExportSetupActivity() {
        whenever(exportSettingsViewModel.storedExportSettings).then {
            MutableLiveData(ExportSettings.WithData(ExportFrequency.EXPORT_FREQUENCY_NEVER))
        }
        launchFragment<BackupAndRestoreSettingsFragment>(Bundle()).use {
            val expectedResult = ActivityResult(Activity.RESULT_OK, Intent())
            intending(hasComponent(ExportSetupActivity::class.java.name))
                .respondWith(expectedResult)

            onView(withText("Scheduled export")).perform(click())

            intended(hasComponent(ExportSetupActivity::class.java.name))
            verify(healthConnectLogger)
                .logInteraction(BackupAndRestoreElement.SCHEDULED_EXPORT_BUTTON)
        }
    }

    @Test
    fun backupAndRestoreSettingsFragment_clicksScheduledExportWhenItIsOn_navigatesToScheduledExportFragment() {
        whenever(exportSettingsViewModel.storedExportSettings).then {
            MutableLiveData(ExportSettings.WithData(ExportFrequency.EXPORT_FREQUENCY_DAILY))
        }
        launchFragment<BackupAndRestoreSettingsFragment>(Bundle()) {
                navHostController.setGraph(R.navigation.nav_graph)
                navHostController.setCurrentDestination(R.id.backupAndRestoreSettingsFragment)
                Navigation.setViewNavController(this.requireView(), navHostController)
            }
            .use {
                onView(withText("Scheduled export")).perform(click())

                assertThat(navHostController.currentDestination?.id)
                    .isEqualTo(R.id.scheduledExportFragment)
            }
    }

    @Test
    fun backupAndRestoreSettingsFragment_whenExportSetupCompletes_toastShown() {
        whenever(exportSettingsViewModel.storedExportSettings).then {
            MutableLiveData(ExportSettings.WithData(ExportFrequency.EXPORT_FREQUENCY_NEVER))
        }
        whenever(exportSettingsViewModel.documentProviders).then {
            MutableLiveData(DocumentProviders.WithData(listOf()))
        }
        whenever(importFlowViewModel.setLastCompletionInstant(Instant.now())).then {
            MutableLiveData(Instant.now())
        }

        val expectedMessage: Int = R.string.scheduled_export_on_toast_text

        val expectedResult = ActivityResult(Activity.RESULT_OK, Intent())
        intending(hasComponent(ExportSetupActivity::class.java.name)).respondWith(expectedResult)

        launchFragment<BackupAndRestoreSettingsFragment>(Bundle()).use { scenario ->
            onView(withText("Scheduled export")).perform(click())
            intended(hasComponent(ExportSetupActivity::class.java.name))

            scenario.onActivity { activity: TestActivity ->
                verify(toastManager).showToast(eq(activity), eq(expectedMessage), any())
            }
        }
    }

    @Test
    fun backupAndRestoreSettingsFragment_whenExportFrequencyIsNever_showsCorrectSummary() {
        whenever(exportSettingsViewModel.storedExportSettings).then {
            MutableLiveData(ExportSettings.WithData(ExportFrequency.EXPORT_FREQUENCY_NEVER))
        }
        launchFragment<BackupAndRestoreSettingsFragment>(Bundle()).use {
            onView(withText("Off")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun backupAndRestoreSettingsFragment_whenExportFrequencyIsDaily_showsCorrectSummary() {
        whenever(exportSettingsViewModel.storedExportSettings).then {
            MutableLiveData(ExportSettings.WithData(ExportFrequency.EXPORT_FREQUENCY_DAILY))
        }
        launchFragment<BackupAndRestoreSettingsFragment>(Bundle()).use {
            onView(withText("On • Daily")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun backupAndRestoreSettingsFragment_whenExportFrequencyIsWeekly_showsCorrectSummary() {
        whenever(exportSettingsViewModel.storedExportSettings).then {
            MutableLiveData(ExportSettings.WithData(ExportFrequency.EXPORT_FREQUENCY_WEEKLY))
        }
        launchFragment<BackupAndRestoreSettingsFragment>(Bundle()).use {
            onView(withText("On • Weekly")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun backupAndRestoreSettingsFragment_whenExportFrequencyIsMonthly_showsCorrectSummary() {
        whenever(exportSettingsViewModel.storedExportSettings).then {
            MutableLiveData(ExportSettings.WithData(ExportFrequency.EXPORT_FREQUENCY_MONTHLY))
        }
        launchFragment<BackupAndRestoreSettingsFragment>(Bundle()).use {
            onView(withText("On • Monthly")).check(matches(isDisplayed()))
        }
    }

    @Test
    @Ignore("b/485196026")
    fun backupAndRestoreSettingsFragment_whenImportErrorIsWrongFile_showsImportErrorBanner() {
        whenever(exportSettingsViewModel.storedExportSettings).then {
            MutableLiveData(ExportSettings.WithData(ExportFrequency.EXPORT_FREQUENCY_NEVER))
        }
        whenever(importStatusViewModel.storedImportStatus).then {
            MutableLiveData(
                ImportUiStatus.WithData(
                    ImportUiState(
                        dataImportState = ImportUiState.DataImportState.DATA_IMPORT_ERROR_WRONG_FILE
                    )
                )
            )
        }
        launchFragment<BackupAndRestoreSettingsFragment>(Bundle()).use {
            onView(withText("Choose file")).check(matches(isDisplayed()))
            onView(withText("Couldn't restore data")).check(matches(isDisplayed()))
            onView(
                    withText(
                        "The file you selected isn't compatible for restore. Make sure to select the correct exported file."
                    )
                )
                .check(matches(isDisplayed()))
            verify(healthConnectLogger)
                .logImpression(BackupAndRestoreElement.IMPORT_WRONG_FILE_ERROR_BANNER)
            verify(healthConnectLogger)
                .logImpression(BackupAndRestoreElement.IMPORT_WRONG_FILE_ERROR_BANNER_BUTTON)
        }
    }

    @Test
    @Ignore("b/485196026")
    fun backupAndRestoreSettingsFragment_whenImportErrorIsVersionMismatch_showsImportErrorBanner() {
        whenever(exportSettingsViewModel.storedExportSettings).then {
            MutableLiveData(ExportSettings.WithData(ExportFrequency.EXPORT_FREQUENCY_NEVER))
        }
        whenever(importStatusViewModel.storedImportStatus).then {
            MutableLiveData(
                ImportUiStatus.WithData(
                    ImportUiState(
                        ImportUiState.DataImportState.DATA_IMPORT_ERROR_VERSION_MISMATCH
                        /** isImportOngoing= */
                    )
                )
            )
        }
        launchFragment<BackupAndRestoreSettingsFragment>(Bundle()).use {
            onView(withText("Update now")).check(matches(isDisplayed()))
            onView(withText("Couldn't restore data")).check(matches(isDisplayed()))
            onView(
                    withText(
                        "Update your system so that Health\u00A0Connect can restore your data, then try again."
                    )
                )
                .check(matches(isDisplayed()))
            verify(healthConnectLogger)
                .logImpression(BackupAndRestoreElement.IMPORT_VERSION_MISMATCH_ERROR_BANNER)
            verify(healthConnectLogger)
                .logImpression(BackupAndRestoreElement.IMPORT_VERSION_MISMATCH_ERROR_BANNER_BUTTON)
        }
    }

    @Test
    @Ignore("b/485196026")
    fun backupAndRestoreSettingsFragment_whenImportErrorIsUnknown_showsImportErrorBanner() {
        whenever(exportSettingsViewModel.storedExportSettings).then {
            MutableLiveData(ExportSettings.WithData(ExportFrequency.EXPORT_FREQUENCY_NEVER))
        }
        whenever(importStatusViewModel.storedImportStatus).then {
            MutableLiveData(
                ImportUiStatus.WithData(
                    ImportUiState(
                        ImportUiState.DataImportState.DATA_IMPORT_ERROR_UNKNOWN
                        /** isImportOngoing= */
                    )
                )
            )
        }
        launchFragment<BackupAndRestoreSettingsFragment>(Bundle()).use {
            onView(withText("Try again")).check(matches(isDisplayed()))
            onView(withText("Couldn't restore data")).check(matches(isDisplayed()))
            onView(withText("There was a problem with restoring data from your export."))
                .check(matches(isDisplayed()))
            verify(healthConnectLogger)
                .logImpression(BackupAndRestoreElement.IMPORT_GENERAL_ERROR_BANNER)
            verify(healthConnectLogger)
                .logImpression(BackupAndRestoreElement.IMPORT_GENERAL_ERROR_BANNER_BUTTON)
        }
    }

    @Test
    fun backupAndRestoreSettingsFragment_whenImportErrorIsNone_doesNotShowImportErrorBanner() {
        whenever(exportSettingsViewModel.storedExportSettings).then {
            MutableLiveData(ExportSettings.WithData(ExportFrequency.EXPORT_FREQUENCY_NEVER))
        }
        whenever(importStatusViewModel.storedImportStatus).then {
            MutableLiveData(
                ImportUiStatus.WithData(
                    ImportUiState(
                        dataImportState = ImportUiState.DataImportState.DATA_IMPORT_ERROR_NONE
                    )
                )
            )
        }
        launchFragment<BackupAndRestoreSettingsFragment>(Bundle()).use {
            onView(withText("Couldn't restore data")).check(doesNotExist())
        }
    }

    @Test
    fun backupAndRestoreSettingsFragment_clicksAboutBackupAndRestore_showsHelpCenterLink() {
        whenever(exportSettingsViewModel.storedExportSettings).then {
            MutableLiveData(ExportSettings.WithData(ExportFrequency.EXPORT_FREQUENCY_WEEKLY))
        }

        launchFragment<BackupAndRestoreSettingsFragment>(Bundle()).use {
            onView(withText("About backup and restore")).check(matches(isDisplayed()))

            onView(withText("About backup and restore")).perform(scrollTo(), click())
            assertThat(fakeDeviceInfoUtils.backupAndRestoreHelpCenterInvoked).isTrue()
        }
    }

    @Test
    @DisableFlags(Flags.FLAG_CLOUD_BACKUP_AND_RESTORE_HC_UI)
    fun cloudBackupRestore_UIElementDisabled() {
        whenever(exportSettingsViewModel.storedExportSettings).then {
            MutableLiveData(ExportSettings.WithData(ExportFrequency.EXPORT_FREQUENCY_WEEKLY))
        }

        // A hander activity with the right permissions is available
        val settingUIComponentName =
            ComponentName("com.example.testsettings", "com.example.testsettings.MockActivity")
        val settingsStartIntent =
            Intent("android.health.connect.action.SHOW_HEALTH_CONNECT_BACKUP_SETTINGS")
        val mockPackageManager =
            mockOutSettingsReceiverActivityWithPermission(
                settingUIComponentName,
                settingsStartIntent,
                BACKUP_HEALTH_CONNECT_DATA_AND_SETTINGS, /* Permission to be granted*/
            )
        intending(hasAction("android.health.connect.action.SHOW_HEALTH_CONNECT_BACKUP_SETTINGS"))
            .respondWith(ActivityResult(Activity.RESULT_OK, Intent()))

        // All steps to show the UI element are executed
        launchFragment<BackupAndRestoreSettingsFragment>(
                Bundle(),
                action = {
                    (this as BackupAndRestoreSettingsFragment).packageManager = mockPackageManager
                    // The fragement lifecycle is already complete when we set the packageManager,
                    // Manually re-trigger the componentResolution to hit the mock PackageManager.
                    this.resolveBackupSettingsComponentAndDisplayOptionIfAvailable()
                },
            )
            .use {
                // But the UI is invisible, because the flag is turned off
                onView(withText("Backup")).check(doesNotExist())
            }
    }

    @Test
    @EnableFlags(Flags.FLAG_CLOUD_BACKUP_AND_RESTORE_HC_UI)
    fun cloudBackupRestore_showsAndTriggersSettingsUI_whenSettingsPermissionGiven() {
        whenever(exportSettingsViewModel.storedExportSettings).then {
            MutableLiveData(ExportSettings.WithData(ExportFrequency.EXPORT_FREQUENCY_WEEKLY))
        }

        // A hander activity with the right permissions is available
        val settingUIComponentName =
            ComponentName("com.example.testsettings", "com.example.testsettings.MockActivity")
        val settingsStartIntent =
            Intent("android.health.connect.action.SHOW_HEALTH_CONNECT_BACKUP_SETTINGS")
        val mockPackageManager =
            mockOutSettingsReceiverActivityWithPermission(
                settingUIComponentName,
                settingsStartIntent,
                BACKUP_HEALTH_CONNECT_DATA_AND_SETTINGS, /* Permission to be granted*/
            )
        intending(hasAction("android.health.connect.action.SHOW_HEALTH_CONNECT_BACKUP_SETTINGS"))
            .respondWith(ActivityResult(Activity.RESULT_OK, Intent()))

        launchFragment<BackupAndRestoreSettingsFragment>(
                Bundle(),
                action = {
                    (this as BackupAndRestoreSettingsFragment).packageManager = mockPackageManager

                    // The fragement lifecycle is already complete when we set the packageManager,
                    // Manually re-trigger the componentResolution to hit the mock PackageManager.
                    this.resolveBackupSettingsComponentAndDisplayOptionIfAvailable()
                },
            )
            .use {
                onView(withText("Backup")).perform(click())

                // The package manager should have requested the resolving activity.
                val intentCaptor: ArgumentCaptor<Intent> =
                    ArgumentCaptor.forClass(Intent::class.java)
                verify(mockPackageManager)
                    .resolveActivity(intentCaptor.capture(), eq(PackageManager.MATCH_DEFAULT_ONLY))
                val actualIntent = intentCaptor.firstValue
                assertThat(actualIntent.action)
                    .isEqualTo("android.health.connect.action.SHOW_HEALTH_CONNECT_BACKUP_SETTINGS")
                assertThat(intentCaptor.allValues.size).isEqualTo(1)

                // And an intent should have been sent to it.
                intended(hasComponent(settingUIComponentName))
                intended(
                    hasAction("android.health.connect.action.SHOW_HEALTH_CONNECT_BACKUP_SETTINGS")
                )
            }
    }

    @Test
    @EnableFlags(Flags.FLAG_CLOUD_BACKUP_AND_RESTORE_HC_UI)
    fun cloudBackupRestore_showsAndTriggersSettingsUI_whenBackupPermissionGiven() {
        whenever(exportSettingsViewModel.storedExportSettings).then {
            MutableLiveData(ExportSettings.WithData(ExportFrequency.EXPORT_FREQUENCY_WEEKLY))
        }

        // A hander activity with the Backup permissions is available
        val settingUIComponentName =
            ComponentName("com.example.testsettings", "com.example.testsettings.MockActivity")
        val settingsStartIntent =
            Intent("android.health.connect.action.SHOW_HEALTH_CONNECT_BACKUP_SETTINGS")
        val mockPackageManager =
            mockOutSettingsReceiverActivityWithPermission(
                settingUIComponentName,
                settingsStartIntent,
                BACKUP, /* Permission to be granted*/
            )
        intending(hasAction("android.health.connect.action.SHOW_HEALTH_CONNECT_BACKUP_SETTINGS"))
            .respondWith(ActivityResult(Activity.RESULT_OK, Intent()))

        launchFragment<BackupAndRestoreSettingsFragment>(
                Bundle(),
                action = {
                    (this as BackupAndRestoreSettingsFragment).packageManager = mockPackageManager
                    // The fragement lifecycle is already complete when we set the packageManager,
                    // Manually re-trigger the componentResolution to hit the mock PackageManager.
                    this.resolveBackupSettingsComponentAndDisplayOptionIfAvailable()
                },
            )
            .use {
                onView(withText("Backup")).perform(click())

                // The package manager should have requested the resolving activity.
                val intentCaptor: ArgumentCaptor<Intent> =
                    ArgumentCaptor.forClass(Intent::class.java)
                verify(mockPackageManager)
                    .resolveActivity(intentCaptor.capture(), eq(PackageManager.MATCH_DEFAULT_ONLY))
                val actualIntent = intentCaptor.firstValue
                assertThat(actualIntent.action)
                    .isEqualTo("android.health.connect.action.SHOW_HEALTH_CONNECT_BACKUP_SETTINGS")
                assertThat(intentCaptor.allValues.size).isEqualTo(1)

                // And an intent should have been sent to it.
                intended(hasComponent(settingUIComponentName))
                intended(
                    hasAction("android.health.connect.action.SHOW_HEALTH_CONNECT_BACKUP_SETTINGS")
                )
            }
    }

    @Test
    @EnableFlags(Flags.FLAG_CLOUD_BACKUP_AND_RESTORE_HC_UI)
    fun cloudBackupRestore_doesNotShowSettingsUIWithoutPermission() {
        whenever(exportSettingsViewModel.storedExportSettings).then {
            MutableLiveData(ExportSettings.WithData(ExportFrequency.EXPORT_FREQUENCY_WEEKLY))
        }

        // A hander activity is available, but does not have permission
        val settingUIComponentName =
            ComponentName("com.example.testsettings", "com.example.testsettings.MockActivity")
        val settingsStartIntent =
            Intent("android.health.connect.action.SHOW_HEALTH_CONNECT_BACKUP_SETTINGS")
        val mockPackageManager =
            mockOutSettingsReceiverActivityWithPermission(
                settingUIComponentName,
                settingsStartIntent,
                "", /* No permission granted*/
            )
        intending(hasAction("android.health.connect.action.SHOW_HEALTH_CONNECT_BACKUP_SETTINGS"))
            .respondWith(ActivityResult(Activity.RESULT_OK, Intent()))

        launchFragment<BackupAndRestoreSettingsFragment>(
                Bundle(),
                action = {
                    (this as BackupAndRestoreSettingsFragment).packageManager = mockPackageManager
                    // The fragement lifecycle is already complete when we set the packageManager,
                    // Manually re-trigger the componentResolution to hit the mock PackageManager.
                    this.resolveBackupSettingsComponentAndDisplayOptionIfAvailable()
                },
            )
            .use {

                // The resolver doesn't hold permissions, so the UI should stay hidden.
                onView(withText("Backup")).check(doesNotExist())
            }
    }

    @Test
    @EnableFlags(Flags.FLAG_CLOUD_BACKUP_AND_RESTORE_HC_UI)
    fun cloudBackupRestore_backupOptionHiddenIfNoSettingsResolverPresent() {
        whenever(exportSettingsViewModel.storedExportSettings).then {
            MutableLiveData(ExportSettings.WithData(ExportFrequency.EXPORT_FREQUENCY_WEEKLY))
        }

        launchFragment<BackupAndRestoreSettingsFragment>(Bundle()).use {
            // We are not mocking out the package manager to provide a component that can handle the
            // settings intent. If there is no settings handler, the backup option should be hidden.
            onView(withText("Backup")).check(doesNotExist())
        }
    }

    // We create our own mockPackagemanager, because ShadowManager can not be used on instrumented
    // tests that run on real devices.
    private fun mockOutSettingsReceiverActivityWithPermission(
        receiverComponent: ComponentName,
        forIntent: Intent,
        permission: String,
    ): PackageManager {

        // Mock out the call for finding the resolving activity by providing an imaginary
        // package and activity that can resolve ACTION_SHOW_HEALTH_CONNECT_BACKUP_SETTINGS
        val mockPackageManagerLocal: PackageManager = mock()
        val resolveInfo =
            ResolveInfo().apply {
                activityInfo =
                    ActivityInfo().apply {
                        applicationInfo =
                            ApplicationInfo().apply {
                                packageName = receiverComponent.packageName
                                name = receiverComponent.className
                            }
                        name = receiverComponent.className
                    }
            }

        // Make our imaginary activity the resolver for ACTION_SHOW_HEALTH_CONNECT_BACKUP_SETTINGS
        whenever(
                mockPackageManagerLocal.resolveActivity(
                    argThat { intent -> intent != null && forIntent.action == intent.action },
                    eq(PackageManager.MATCH_DEFAULT_ONLY),
                )
            )
            .thenReturn(resolveInfo)

        // Give permissions to our imaginary package
        if (permission.isEmpty()) {
            whenever(
                    mockPackageManagerLocal.checkPermission(
                        any(),
                        eq(receiverComponent.packageName),
                    )
                )
                .thenReturn(PackageManager.PERMISSION_DENIED)
        } else {
            whenever(
                    mockPackageManagerLocal.checkPermission(
                        eq(permission),
                        eq(receiverComponent.packageName),
                    )
                )
                .thenReturn(PackageManager.PERMISSION_GRANTED)
        }

        return mockPackageManagerLocal
    }
}
