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

package com.android.healthconnect.controller.tests.exportimport

import android.app.Activity.RESULT_OK
import android.app.Instrumentation.ActivityResult
import android.content.Context
import android.content.Intent
import android.health.connect.exportimport.ExportImportDocumentProvider
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.espresso.Espresso.onIdle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.intent.matcher.IntentMatchers.hasType
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isClickable
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.isNotChecked
import androidx.test.espresso.matcher.ViewMatchers.isNotEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.exportimport.ExportDestinationFragment
import com.android.healthconnect.controller.exportimport.ImportSourceLocationFragment
import com.android.healthconnect.controller.exportimport.api.HealthDataExportManager
import com.android.healthconnect.controller.service.HealthDataExportManagerModule
import com.android.healthconnect.controller.tests.utils.checkBoxOf
import com.android.healthconnect.controller.tests.utils.di.FakeDeviceInfoUtils
import com.android.healthconnect.controller.tests.utils.di.FakeHealthDataExportManager
import com.android.healthconnect.controller.tests.utils.launchFragment
import com.android.healthconnect.controller.tests.utils.toggleAnimation
import com.android.healthconnect.controller.tests.utils.withTitleAndSummary
import com.android.healthconnect.controller.tests.utils.withTitleNoSummary
import com.android.healthconnect.controller.utils.DeviceInfoUtils
import com.android.healthconnect.controller.utils.DeviceInfoUtilsModule
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.ImportSourceLocationElement
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import java.io.File
import org.hamcrest.Matchers.not
import org.hamcrest.Matchers.startsWith
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@HiltAndroidTest
@UninstallModules(DeviceInfoUtilsModule::class, HealthDataExportManagerModule::class)
class ImportSourceLocationFragmentTest {
    companion object {
        private const val TEST_DOCUMENT_PROVIDER_1_TITLE = "Document provider 1"
        private const val TEST_DOCUMENT_PROVIDER_1_AUTHORITY = "documentprovider1.com"
        private const val TEST_DOCUMENT_PROVIDER_1_ICON_RESOURCE = 1
        private const val TEST_DOCUMENT_PROVIDER_1_ROOT_1_SUMMARY = "Account 1"
        private val TEST_DOCUMENT_PROVIDER_1_ROOT_1_URI =
            Uri.parse(
                "content://android.healthconnect.tests.documentprovider1.documents/root/account1"
            )
        private const val TEST_DOCUMENT_PROVIDER_1_ROOT_2_SUMMARY = "Account 2"
        private val TEST_DOCUMENT_PROVIDER_1_ROOT_2_URI =
            Uri.parse(
                "content://android.healthconnect.tests.documentprovider1.documents/root/account2"
            )

        private const val TEST_DOCUMENT_PROVIDER_2_TITLE = "Document provider 2"
        private const val TEST_DOCUMENT_PROVIDER_2_AUTHORITY = "documentprovider2.com"
        private const val TEST_DOCUMENT_PROVIDER_2_ICON_RESOURCE = 2
        private const val TEST_DOCUMENT_PROVIDER_2_ROOT_SUMMARY = "Account"
        private val TEST_DOCUMENT_PROVIDER_2_ROOT_URI =
            Uri.parse(
                "content://android.healthconnect.tests.documentprovider2.documents/root/account"
            )

        private val EXTERNAL_STORAGE_DOCUMENT_URI =
            Uri.parse("content://com.android.externalstorage.documents/document")
        private val DOWNLOADS_DOCUMENT_URI =
            Uri.parse("content://com.android.providers.downloads.documents/document")
    }

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @BindValue val healthDataExportManager: HealthDataExportManager = FakeHealthDataExportManager()
    private val fakeHealthDataExportManager = healthDataExportManager as FakeHealthDataExportManager
    @BindValue val deviceInfoUtils: DeviceInfoUtils = FakeDeviceInfoUtils()
    @BindValue val healthConnectLogger: HealthConnectLogger = mock()

    private lateinit var navHostController: TestNavHostController
    private lateinit var context: Context

    @Before
    fun setup() {
        hiltRule.inject()
        context = InstrumentationRegistry.getInstrumentation().context
        navHostController = TestNavHostController(context)
        Intents.init()

        fakeHealthDataExportManager.setExportImportDocumentProviders(listOf())
        toggleAnimation(false)
    }

    @After
    fun tearDown() {
        Intents.release()
        fakeHealthDataExportManager.reset()
        reset(healthConnectLogger)
        toggleAnimation(true)
    }

    @Test
    fun isDisplayedCorrectly() {
        launchFragment<ImportSourceLocationFragment>(Bundle())

        onView(withText("Import from")).check(matches(isDisplayed()))
        onView(withText("Cancel")).check(matches(isDisplayed()))
        onView(withText("Next")).check(matches(isDisplayed()))
    }

    @Test
    fun impressionsLogged() {
        launchFragment<ImportSourceLocationFragment>(Bundle())

        verify(healthConnectLogger, atLeast(1)).logPageImpression()
        verify(healthConnectLogger)
            .logImpression(ImportSourceLocationElement.IMPORT_SOURCE_LOCATION_NEXT_BUTTON)
        verify(healthConnectLogger)
            .logImpression(ImportSourceLocationElement.IMPORT_SOURCE_LOCATION_CANCEL_BUTTON)
    }

    @Test
    fun cancelButton_isClickable() {
        launchFragment<ImportSourceLocationFragment>(Bundle())

        onView(withId(R.id.secondary_button)).check(matches(isClickable()))
        onView(withId(R.id.secondary_button)).perform(click())
        verify(healthConnectLogger)
            .logInteraction(ImportSourceLocationElement.IMPORT_SOURCE_LOCATION_CANCEL_BUTTON)
    }

    @Test
    fun nextButton_notEnabled() {
        launchFragment<ImportSourceLocationFragment>(Bundle())

        onView(withId(R.id.primary_button_full)).check(matches(isNotEnabled()))
    }

    @Test
    fun showsDocumentProviders() {
        val documentProviders =
            listOf(
                ExportImportDocumentProvider(
                    TEST_DOCUMENT_PROVIDER_1_TITLE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_1_SUMMARY,
                    TEST_DOCUMENT_PROVIDER_1_ICON_RESOURCE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_1_URI,
                    TEST_DOCUMENT_PROVIDER_1_AUTHORITY,
                ),
                ExportImportDocumentProvider(
                    TEST_DOCUMENT_PROVIDER_2_TITLE,
                    TEST_DOCUMENT_PROVIDER_2_ROOT_SUMMARY,
                    TEST_DOCUMENT_PROVIDER_2_ICON_RESOURCE,
                    TEST_DOCUMENT_PROVIDER_2_ROOT_URI,
                    TEST_DOCUMENT_PROVIDER_2_AUTHORITY,
                ),
            )
        fakeHealthDataExportManager.setExportImportDocumentProviders(documentProviders)
        launchFragment<ImportSourceLocationFragment>(Bundle())

        onView(
                withTitleAndSummary(
                    TEST_DOCUMENT_PROVIDER_1_TITLE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_1_SUMMARY,
                )
            )
            .check(matches(isDisplayed()))
        onView(
                withTitleAndSummary(
                    TEST_DOCUMENT_PROVIDER_2_TITLE,
                    TEST_DOCUMENT_PROVIDER_2_ROOT_SUMMARY,
                )
            )
            .check(matches(isDisplayed()))
    }

    @Test
    fun showsDocumentProviders_notChecked() {
        val documentProviders =
            listOf(
                ExportImportDocumentProvider(
                    TEST_DOCUMENT_PROVIDER_1_TITLE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_1_SUMMARY,
                    TEST_DOCUMENT_PROVIDER_1_ICON_RESOURCE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_1_URI,
                    TEST_DOCUMENT_PROVIDER_1_AUTHORITY,
                ),
                ExportImportDocumentProvider(
                    TEST_DOCUMENT_PROVIDER_2_TITLE,
                    TEST_DOCUMENT_PROVIDER_2_ROOT_SUMMARY,
                    TEST_DOCUMENT_PROVIDER_2_ICON_RESOURCE,
                    TEST_DOCUMENT_PROVIDER_2_ROOT_URI,
                    TEST_DOCUMENT_PROVIDER_2_AUTHORITY,
                ),
            )
        fakeHealthDataExportManager.setExportImportDocumentProviders(documentProviders)
        launchFragment<ImportSourceLocationFragment>(Bundle())

        onView(checkBoxOf(TEST_DOCUMENT_PROVIDER_1_TITLE)).check(matches(isNotChecked()))
        onView(checkBoxOf(TEST_DOCUMENT_PROVIDER_2_TITLE)).check(matches(isNotChecked()))
    }

    @Test
    fun documentProviderClicked_documentProviderIsChecked() {
        val documentProviders =
            listOf(
                ExportImportDocumentProvider(
                    TEST_DOCUMENT_PROVIDER_1_TITLE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_1_SUMMARY,
                    TEST_DOCUMENT_PROVIDER_1_ICON_RESOURCE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_1_URI,
                    TEST_DOCUMENT_PROVIDER_1_AUTHORITY,
                ),
                ExportImportDocumentProvider(
                    TEST_DOCUMENT_PROVIDER_2_TITLE,
                    TEST_DOCUMENT_PROVIDER_2_ROOT_SUMMARY,
                    TEST_DOCUMENT_PROVIDER_2_ICON_RESOURCE,
                    TEST_DOCUMENT_PROVIDER_2_ROOT_URI,
                    TEST_DOCUMENT_PROVIDER_2_AUTHORITY,
                ),
            )
        fakeHealthDataExportManager.setExportImportDocumentProviders(documentProviders)
        launchFragment<ImportSourceLocationFragment>(Bundle())

        onView(withText(TEST_DOCUMENT_PROVIDER_1_TITLE)).perform(click())

        onView(checkBoxOf(TEST_DOCUMENT_PROVIDER_1_TITLE)).check(matches(isChecked()))
    }

    @Test
    fun secondDocumentProviderClicked_otherDocumentProviderIsNotChecked() {
        val documentProviders =
            listOf(
                ExportImportDocumentProvider(
                    TEST_DOCUMENT_PROVIDER_1_TITLE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_1_SUMMARY,
                    TEST_DOCUMENT_PROVIDER_1_ICON_RESOURCE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_1_URI,
                    TEST_DOCUMENT_PROVIDER_1_AUTHORITY,
                ),
                ExportImportDocumentProvider(
                    TEST_DOCUMENT_PROVIDER_2_TITLE,
                    TEST_DOCUMENT_PROVIDER_2_ROOT_SUMMARY,
                    TEST_DOCUMENT_PROVIDER_2_ICON_RESOURCE,
                    TEST_DOCUMENT_PROVIDER_2_ROOT_URI,
                    TEST_DOCUMENT_PROVIDER_2_AUTHORITY,
                ),
            )
        fakeHealthDataExportManager.setExportImportDocumentProviders(documentProviders)
        launchFragment<ImportSourceLocationFragment>(Bundle())

        onView(withText(TEST_DOCUMENT_PROVIDER_1_TITLE)).perform(click())
        onView(withText(TEST_DOCUMENT_PROVIDER_2_TITLE)).perform(click())

        onView(checkBoxOf(TEST_DOCUMENT_PROVIDER_1_TITLE)).check(matches(isNotChecked()))
        onView(checkBoxOf(TEST_DOCUMENT_PROVIDER_2_TITLE)).check(matches(isChecked()))
    }

    @Test
    fun documentProviderClicked_nextButtonIsEnabled() {
        val documentProviders =
            listOf(
                ExportImportDocumentProvider(
                    TEST_DOCUMENT_PROVIDER_1_TITLE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_1_SUMMARY,
                    TEST_DOCUMENT_PROVIDER_1_ICON_RESOURCE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_1_URI,
                    TEST_DOCUMENT_PROVIDER_1_AUTHORITY,
                ),
                ExportImportDocumentProvider(
                    TEST_DOCUMENT_PROVIDER_2_TITLE,
                    TEST_DOCUMENT_PROVIDER_2_ROOT_SUMMARY,
                    TEST_DOCUMENT_PROVIDER_2_ICON_RESOURCE,
                    TEST_DOCUMENT_PROVIDER_2_ROOT_URI,
                    TEST_DOCUMENT_PROVIDER_2_AUTHORITY,
                ),
            )
        fakeHealthDataExportManager.setExportImportDocumentProviders(documentProviders)
        launchFragment<ImportSourceLocationFragment>(Bundle())

        onView(withText(TEST_DOCUMENT_PROVIDER_1_TITLE)).perform(click())

        onView(withId(R.id.primary_button_full)).check(matches(isEnabled()))
    }

    @Test
    fun nextButtonClicked_startsDocumentsUi() {
        val documentProviders =
            listOf(
                ExportImportDocumentProvider(
                    TEST_DOCUMENT_PROVIDER_1_TITLE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_1_SUMMARY,
                    TEST_DOCUMENT_PROVIDER_1_ICON_RESOURCE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_1_URI,
                    TEST_DOCUMENT_PROVIDER_1_AUTHORITY,
                ),
                ExportImportDocumentProvider(
                    TEST_DOCUMENT_PROVIDER_2_TITLE,
                    TEST_DOCUMENT_PROVIDER_2_ROOT_SUMMARY,
                    TEST_DOCUMENT_PROVIDER_2_ICON_RESOURCE,
                    TEST_DOCUMENT_PROVIDER_2_ROOT_URI,
                    TEST_DOCUMENT_PROVIDER_2_AUTHORITY,
                ),
            )
        fakeHealthDataExportManager.setExportImportDocumentProviders(documentProviders)
        launchFragment<ImportSourceLocationFragment>(Bundle())

        onView(withText(TEST_DOCUMENT_PROVIDER_1_TITLE)).perform(click())
        onView(withId(R.id.primary_button_full)).perform(click())

        intended(hasAction(Intent.ACTION_OPEN_DOCUMENT))
        intended(hasType("application/zip"))
        intended(hasExtra(DocumentsContract.EXTRA_INITIAL_URI, TEST_DOCUMENT_PROVIDER_1_ROOT_1_URI))
        verify(healthConnectLogger)
            .logInteraction(ImportSourceLocationElement.IMPORT_SOURCE_LOCATION_NEXT_BUTTON)
    }

    @Test
    fun chooseFile_navigatesToImportConfirmationDialogFragment() {
        val documentProviders =
            listOf(
                ExportImportDocumentProvider(
                    TEST_DOCUMENT_PROVIDER_1_TITLE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_1_SUMMARY,
                    TEST_DOCUMENT_PROVIDER_1_ICON_RESOURCE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_1_URI,
                    TEST_DOCUMENT_PROVIDER_1_AUTHORITY,
                ),
                ExportImportDocumentProvider(
                    TEST_DOCUMENT_PROVIDER_2_TITLE,
                    TEST_DOCUMENT_PROVIDER_2_ROOT_SUMMARY,
                    TEST_DOCUMENT_PROVIDER_2_ICON_RESOURCE,
                    TEST_DOCUMENT_PROVIDER_2_ROOT_URI,
                    TEST_DOCUMENT_PROVIDER_2_AUTHORITY,
                ),
            )
        fakeHealthDataExportManager.setExportImportDocumentProviders(documentProviders)
        launchFragment<ImportSourceLocationFragment>(Bundle()) {
            navHostController.setGraph(R.navigation.import_nav_graph)
            navHostController.setCurrentDestination(R.id.importSourceLocationFragment)
            Navigation.setViewNavController(this.requireView(), navHostController)
        }

        val testFile = File.createTempFile("testFile", ".zip")

        intending(hasAction(Intent.ACTION_OPEN_DOCUMENT))
            .respondWith(ActivityResult(RESULT_OK, Intent().setData(Uri.fromFile(testFile))))

        onView(withText(TEST_DOCUMENT_PROVIDER_1_TITLE)).perform(click())
        onView(withId(R.id.primary_button_full)).perform(click())

        onView(withText("Import this file?")).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withId(R.id.dialog_custom_message))
            .inRoot(isDialog())
            .check(matches(withText(startsWith(testFile.name))))
        onView(withText("Cancel")).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withText("Import")).inRoot(isDialog()).check(matches(isDisplayed()))
    }

    @Test
    fun chooseExternalStorageFile_doesNotNavigateToNewScreen() {
        val documentProviders =
            listOf(
                ExportImportDocumentProvider(
                    TEST_DOCUMENT_PROVIDER_1_TITLE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_1_SUMMARY,
                    TEST_DOCUMENT_PROVIDER_1_ICON_RESOURCE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_1_URI,
                    TEST_DOCUMENT_PROVIDER_1_AUTHORITY,
                )
            )
        fakeHealthDataExportManager.setExportImportDocumentProviders(documentProviders)
        launchFragment<ImportSourceLocationFragment>(Bundle()) {
            navHostController.setGraph(R.navigation.import_nav_graph)
            navHostController.setCurrentDestination(R.id.importSourceLocationFragment)
            Navigation.setViewNavController(this.requireView(), navHostController)
        }
        intending(hasAction(Intent.ACTION_OPEN_DOCUMENT))
            .respondWith(ActivityResult(RESULT_OK, Intent().setData(EXTERNAL_STORAGE_DOCUMENT_URI)))

        onView(withText(TEST_DOCUMENT_PROVIDER_1_TITLE)).perform(click())
        onView(withId(R.id.primary_button_full)).perform(click())

        assertThat(navHostController.currentDestination?.id)
            .isEqualTo(R.id.importSourceLocationFragment)
    }

    @Test
    fun chooseDownloadsFile_doesNotNavigateToNewScreen() {
        val documentProviders =
            listOf(
                ExportImportDocumentProvider(
                    TEST_DOCUMENT_PROVIDER_1_TITLE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_1_SUMMARY,
                    TEST_DOCUMENT_PROVIDER_1_ICON_RESOURCE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_1_URI,
                    TEST_DOCUMENT_PROVIDER_1_AUTHORITY,
                )
            )
        fakeHealthDataExportManager.setExportImportDocumentProviders(documentProviders)
        launchFragment<ImportSourceLocationFragment>(Bundle()) {
            navHostController.setGraph(R.navigation.import_nav_graph)
            navHostController.setCurrentDestination(R.id.importSourceLocationFragment)
            Navigation.setViewNavController(this.requireView(), navHostController)
        }
        intending(hasAction(Intent.ACTION_OPEN_DOCUMENT))
            .respondWith(ActivityResult(RESULT_OK, Intent().setData(DOWNLOADS_DOCUMENT_URI)))

        onView(withText(TEST_DOCUMENT_PROVIDER_1_TITLE)).perform(click())
        onView(withId(R.id.primary_button_full)).perform(click())

        assertThat(navHostController.currentDestination?.id)
            .isEqualTo(R.id.importSourceLocationFragment)
    }

    @Test
    fun multipleAccounts_doesNotShowSummary() {
        val documentProviders =
            listOf(
                ExportImportDocumentProvider(
                    TEST_DOCUMENT_PROVIDER_1_TITLE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_1_SUMMARY,
                    TEST_DOCUMENT_PROVIDER_1_ICON_RESOURCE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_1_URI,
                    TEST_DOCUMENT_PROVIDER_1_AUTHORITY,
                ),
                ExportImportDocumentProvider(
                    TEST_DOCUMENT_PROVIDER_1_TITLE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_2_SUMMARY,
                    TEST_DOCUMENT_PROVIDER_1_ICON_RESOURCE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_2_URI,
                    TEST_DOCUMENT_PROVIDER_1_AUTHORITY,
                ),
                ExportImportDocumentProvider(
                    TEST_DOCUMENT_PROVIDER_2_TITLE,
                    TEST_DOCUMENT_PROVIDER_2_ROOT_SUMMARY,
                    TEST_DOCUMENT_PROVIDER_2_ICON_RESOURCE,
                    TEST_DOCUMENT_PROVIDER_2_ROOT_URI,
                    TEST_DOCUMENT_PROVIDER_2_AUTHORITY,
                ),
            )
        fakeHealthDataExportManager.setExportImportDocumentProviders(documentProviders)
        launchFragment<ImportSourceLocationFragment>(Bundle())

        onView(withTitleNoSummary(TEST_DOCUMENT_PROVIDER_1_TITLE)).check(matches(isDisplayed()))
    }

    @Test
    fun multipleAccountsClicked_showsAccountPicker() {
        val documentProviders =
            listOf(
                ExportImportDocumentProvider(
                    TEST_DOCUMENT_PROVIDER_1_TITLE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_1_SUMMARY,
                    TEST_DOCUMENT_PROVIDER_1_ICON_RESOURCE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_1_URI,
                    TEST_DOCUMENT_PROVIDER_1_AUTHORITY,
                ),
                ExportImportDocumentProvider(
                    TEST_DOCUMENT_PROVIDER_1_TITLE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_2_SUMMARY,
                    TEST_DOCUMENT_PROVIDER_1_ICON_RESOURCE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_2_URI,
                    TEST_DOCUMENT_PROVIDER_1_AUTHORITY,
                ),
                ExportImportDocumentProvider(
                    TEST_DOCUMENT_PROVIDER_2_TITLE,
                    TEST_DOCUMENT_PROVIDER_2_ROOT_SUMMARY,
                    TEST_DOCUMENT_PROVIDER_2_ICON_RESOURCE,
                    TEST_DOCUMENT_PROVIDER_2_ROOT_URI,
                    TEST_DOCUMENT_PROVIDER_2_AUTHORITY,
                ),
            )
        fakeHealthDataExportManager.setExportImportDocumentProviders(documentProviders)
        launchFragment<ImportSourceLocationFragment>(Bundle())

        onView(withText(TEST_DOCUMENT_PROVIDER_1_TITLE)).perform(click())

        onView(withText("Choose an account")).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withText(TEST_DOCUMENT_PROVIDER_1_ROOT_1_SUMMARY))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        onView(withText(TEST_DOCUMENT_PROVIDER_1_ROOT_2_SUMMARY))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        onView(withText("Done")).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withText("Cancel")).inRoot(isDialog()).check(matches(isDisplayed()))
    }

    @Test
    fun multipleAccountsClickedAndAccountChosen_updatesSummary() {
        val documentProviders =
            listOf(
                ExportImportDocumentProvider(
                    TEST_DOCUMENT_PROVIDER_1_TITLE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_1_SUMMARY,
                    TEST_DOCUMENT_PROVIDER_1_ICON_RESOURCE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_1_URI,
                    TEST_DOCUMENT_PROVIDER_1_AUTHORITY,
                ),
                ExportImportDocumentProvider(
                    TEST_DOCUMENT_PROVIDER_1_TITLE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_2_SUMMARY,
                    TEST_DOCUMENT_PROVIDER_1_ICON_RESOURCE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_2_URI,
                    TEST_DOCUMENT_PROVIDER_1_AUTHORITY,
                ),
                ExportImportDocumentProvider(
                    TEST_DOCUMENT_PROVIDER_2_TITLE,
                    TEST_DOCUMENT_PROVIDER_2_ROOT_SUMMARY,
                    TEST_DOCUMENT_PROVIDER_2_ICON_RESOURCE,
                    TEST_DOCUMENT_PROVIDER_2_ROOT_URI,
                    TEST_DOCUMENT_PROVIDER_2_AUTHORITY,
                ),
            )
        fakeHealthDataExportManager.setExportImportDocumentProviders(documentProviders)
        launchFragment<ImportSourceLocationFragment>(Bundle())

        onView(withText(TEST_DOCUMENT_PROVIDER_1_TITLE)).perform(click())
        onView(withText(TEST_DOCUMENT_PROVIDER_1_ROOT_2_SUMMARY))
            .inRoot(isDialog())
            .perform(click())
        onView(withText("Done")).inRoot(isDialog()).perform(click())

        onView(
                withTitleAndSummary(
                    TEST_DOCUMENT_PROVIDER_1_TITLE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_2_SUMMARY,
                )
            )
            .check(matches(isDisplayed()))
    }

    @Test
    fun multipleAccountsClickedAndAccountChosen_nextButtonClicked_startsDocumentsUi() {
        val documentProviders =
            listOf(
                ExportImportDocumentProvider(
                    TEST_DOCUMENT_PROVIDER_1_TITLE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_1_SUMMARY,
                    TEST_DOCUMENT_PROVIDER_1_ICON_RESOURCE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_1_URI,
                    TEST_DOCUMENT_PROVIDER_1_AUTHORITY,
                ),
                ExportImportDocumentProvider(
                    TEST_DOCUMENT_PROVIDER_1_TITLE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_2_SUMMARY,
                    TEST_DOCUMENT_PROVIDER_1_ICON_RESOURCE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_2_URI,
                    TEST_DOCUMENT_PROVIDER_1_AUTHORITY,
                ),
                ExportImportDocumentProvider(
                    TEST_DOCUMENT_PROVIDER_2_TITLE,
                    TEST_DOCUMENT_PROVIDER_2_ROOT_SUMMARY,
                    TEST_DOCUMENT_PROVIDER_2_ICON_RESOURCE,
                    TEST_DOCUMENT_PROVIDER_2_ROOT_URI,
                    TEST_DOCUMENT_PROVIDER_2_AUTHORITY,
                ),
            )
        fakeHealthDataExportManager.setExportImportDocumentProviders(documentProviders)
        launchFragment<ImportSourceLocationFragment>(Bundle())

        onView(withText(TEST_DOCUMENT_PROVIDER_1_TITLE)).perform(click())
        onView(withText(TEST_DOCUMENT_PROVIDER_1_ROOT_2_SUMMARY))
            .inRoot(isDialog())
            .perform(click())
        onView(withText("Done")).inRoot(isDialog()).perform(click())
        onView(withText("Next")).perform(click())

        intended(hasAction(Intent.ACTION_OPEN_DOCUMENT))
        intended(hasType("application/zip"))
        intended(hasExtra(DocumentsContract.EXTRA_INITIAL_URI, TEST_DOCUMENT_PROVIDER_1_ROOT_2_URI))
    }

    @Test
    fun noProviders_nextButtonNotEnabled() {
        fakeHealthDataExportManager.setExportImportDocumentProviders(listOf())
        launchFragment<ImportSourceLocationFragment>(Bundle())

        onView(withId(R.id.primary_button_full)).check(matches(isNotEnabled()))
    }

    @Test
    fun noProvidersAndPlayStoreNotAvailable_showsNoAppsText() {
        (deviceInfoUtils as FakeDeviceInfoUtils).setPlayStoreAvailability(false)

        fakeHealthDataExportManager.setExportImportDocumentProviders(listOf())
        launchFragment<ImportSourceLocationFragment>(Bundle())

        onView(withText(R.string.export_import_no_apps_text)).check(matches(isDisplayed()))
    }

    @Test
    fun noProvidersAndPlayStoreNotAvailable_doesNotShowPlayStoreButton() {
        (deviceInfoUtils as FakeDeviceInfoUtils).setPlayStoreAvailability(false)

        fakeHealthDataExportManager.setExportImportDocumentProviders(listOf())
        launchFragment<ImportSourceLocationFragment>(Bundle())

        onView(withId(com.android.settingslib.widget.preference.footer.R.id.settingslib_learn_more))
            .check(matches(not(isDisplayed())))
    }

    @Test
    fun noProvidersAndPlayStoreAvailable_showsNoAppsText() {
        (deviceInfoUtils as FakeDeviceInfoUtils).setPlayStoreAvailability(true)

        fakeHealthDataExportManager.setExportImportDocumentProviders(listOf())
        launchFragment<ImportSourceLocationFragment>(Bundle())

        onView(withText(R.string.export_import_no_apps_text)).check(matches(isDisplayed()))
    }

    @Test
    fun noProvidersAndPlayStoreAvailable_showsPlayStoreButton() {
        (deviceInfoUtils as FakeDeviceInfoUtils).setPlayStoreAvailability(true)

        fakeHealthDataExportManager.setExportImportDocumentProviders(listOf())
        launchFragment<ImportSourceLocationFragment>(Bundle())

        onView(withText(R.string.export_import_go_to_play_store_text)).check(matches(isDisplayed()))
    }

    @Test
    fun noProvidersAndPlayStoreAvailable_goToPlayStoreClicked_launchesPlayStore() {
        (deviceInfoUtils as FakeDeviceInfoUtils).setPlayStoreAvailability(true)

        fakeHealthDataExportManager.setExportImportDocumentProviders(listOf())
        launchFragment<ImportSourceLocationFragment>(Bundle()) {
            navHostController.setGraph(R.navigation.import_nav_graph)
            navHostController.setCurrentDestination(R.id.importSourceLocationFragment)
            Navigation.setViewNavController(this.requireView(), navHostController)
        }

        onView(withText(R.string.export_import_go_to_play_store_text)).perform(click())

        assertThat(navHostController.currentDestination?.id).isEqualTo(R.id.play_store_activity)
    }

    @Test
    fun singleProvider_hasRadioButtonChecked() {
        val documentProviders =
            listOf(
                ExportImportDocumentProvider(
                    TEST_DOCUMENT_PROVIDER_1_TITLE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_1_SUMMARY,
                    TEST_DOCUMENT_PROVIDER_1_ICON_RESOURCE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_1_URI,
                    TEST_DOCUMENT_PROVIDER_1_AUTHORITY,
                )
            )
        fakeHealthDataExportManager.setExportImportDocumentProviders(documentProviders)
        launchFragment<ImportSourceLocationFragment>(Bundle())

        onView(withText(TEST_DOCUMENT_PROVIDER_1_TITLE)).check(matches(isDisplayed()))
        onView(checkBoxOf(TEST_DOCUMENT_PROVIDER_1_TITLE)).check(matches(isChecked()))
    }

    @Test
    fun singleProviderSingleAccount_showsAccountSummary() {
        val documentProviders =
            listOf(
                ExportImportDocumentProvider(
                    TEST_DOCUMENT_PROVIDER_1_TITLE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_1_SUMMARY,
                    TEST_DOCUMENT_PROVIDER_1_ICON_RESOURCE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_1_URI,
                    TEST_DOCUMENT_PROVIDER_1_AUTHORITY,
                )
            )
        fakeHealthDataExportManager.setExportImportDocumentProviders(documentProviders)
        launchFragment<ImportSourceLocationFragment>(Bundle())

        onView(withText(TEST_DOCUMENT_PROVIDER_1_ROOT_1_SUMMARY)).check(matches(isDisplayed()))
    }

    @Test
    fun singleProviderSingleAccount_nextButtonClicked_startsDocumentsUi() {
        val documentProviders =
            listOf(
                ExportImportDocumentProvider(
                    TEST_DOCUMENT_PROVIDER_1_TITLE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_1_SUMMARY,
                    TEST_DOCUMENT_PROVIDER_1_ICON_RESOURCE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_1_URI,
                    TEST_DOCUMENT_PROVIDER_1_AUTHORITY,
                )
            )
        fakeHealthDataExportManager.setExportImportDocumentProviders(documentProviders)
        launchFragment<ImportSourceLocationFragment>(Bundle())

        onView(withText("Next")).perform(click())

        intended(hasAction(Intent.ACTION_OPEN_DOCUMENT))
        intended(hasType("application/zip"))
        intended(hasExtra(DocumentsContract.EXTRA_INITIAL_URI, TEST_DOCUMENT_PROVIDER_1_ROOT_1_URI))
    }

    @Test
    fun singleProviderMultipleAccounts_showsTapToSelectAccount() {
        val documentProviders =
            listOf(
                ExportImportDocumentProvider(
                    TEST_DOCUMENT_PROVIDER_1_TITLE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_1_SUMMARY,
                    TEST_DOCUMENT_PROVIDER_1_ICON_RESOURCE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_1_URI,
                    TEST_DOCUMENT_PROVIDER_1_AUTHORITY,
                ),
                ExportImportDocumentProvider(
                    TEST_DOCUMENT_PROVIDER_1_TITLE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_2_SUMMARY,
                    TEST_DOCUMENT_PROVIDER_1_ICON_RESOURCE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_2_URI,
                    TEST_DOCUMENT_PROVIDER_1_AUTHORITY,
                ),
            )
        fakeHealthDataExportManager.setExportImportDocumentProviders(documentProviders)
        launchFragment<ImportSourceLocationFragment>(Bundle())

        onView(withText(R.string.export_import_tap_to_choose_account)).check(matches(isDisplayed()))
    }

    @Test
    fun singleProviderMultipleAccountsClicked_showsAccountPicker() {
        val documentProviders =
            listOf(
                ExportImportDocumentProvider(
                    TEST_DOCUMENT_PROVIDER_1_TITLE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_1_SUMMARY,
                    TEST_DOCUMENT_PROVIDER_1_ICON_RESOURCE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_1_URI,
                    TEST_DOCUMENT_PROVIDER_1_AUTHORITY,
                ),
                ExportImportDocumentProvider(
                    TEST_DOCUMENT_PROVIDER_1_TITLE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_2_SUMMARY,
                    TEST_DOCUMENT_PROVIDER_1_ICON_RESOURCE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_2_URI,
                    TEST_DOCUMENT_PROVIDER_1_AUTHORITY,
                ),
            )
        fakeHealthDataExportManager.setExportImportDocumentProviders(documentProviders)
        launchFragment<ImportSourceLocationFragment>(Bundle())

        onView(withText(TEST_DOCUMENT_PROVIDER_1_TITLE)).perform(click())

        onView(withText("Choose an account")).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withText(TEST_DOCUMENT_PROVIDER_1_ROOT_1_SUMMARY))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        onView(withText(TEST_DOCUMENT_PROVIDER_1_ROOT_2_SUMMARY))
            .inRoot(isDialog())
            .check(matches(isDisplayed()))
        onView(withText("Done")).inRoot(isDialog()).check(matches(isDisplayed()))
        onView(withText("Cancel")).inRoot(isDialog()).check(matches(isDisplayed()))
    }

    @Test
    fun singleProviderMultipleAccountsClickedAndAccountChosen_updatesSummary() {
        val documentProviders =
            listOf(
                ExportImportDocumentProvider(
                    TEST_DOCUMENT_PROVIDER_1_TITLE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_1_SUMMARY,
                    TEST_DOCUMENT_PROVIDER_1_ICON_RESOURCE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_1_URI,
                    TEST_DOCUMENT_PROVIDER_1_AUTHORITY,
                ),
                ExportImportDocumentProvider(
                    TEST_DOCUMENT_PROVIDER_1_TITLE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_2_SUMMARY,
                    TEST_DOCUMENT_PROVIDER_1_ICON_RESOURCE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_2_URI,
                    TEST_DOCUMENT_PROVIDER_1_AUTHORITY,
                ),
            )
        fakeHealthDataExportManager.setExportImportDocumentProviders(documentProviders)
        launchFragment<ImportSourceLocationFragment>(Bundle())

        onView(withText(TEST_DOCUMENT_PROVIDER_1_TITLE)).perform(click())
        onView(withText(TEST_DOCUMENT_PROVIDER_1_ROOT_2_SUMMARY))
            .inRoot(isDialog())
            .perform(click())
        onView(withText("Done")).inRoot(isDialog()).perform(click())

        onView(
                withTitleAndSummary(
                    TEST_DOCUMENT_PROVIDER_1_TITLE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_2_SUMMARY,
                )
            )
            .check(matches(isDisplayed()))
    }

    @Test
    fun singleProviderMultipleAccountsClickedAndAccountChosen_nextButtonClicked_startsDocumentsUi() {
        val documentProviders =
            listOf(
                ExportImportDocumentProvider(
                    TEST_DOCUMENT_PROVIDER_1_TITLE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_1_SUMMARY,
                    TEST_DOCUMENT_PROVIDER_1_ICON_RESOURCE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_1_URI,
                    TEST_DOCUMENT_PROVIDER_1_AUTHORITY,
                ),
                ExportImportDocumentProvider(
                    TEST_DOCUMENT_PROVIDER_1_TITLE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_2_SUMMARY,
                    TEST_DOCUMENT_PROVIDER_1_ICON_RESOURCE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_2_URI,
                    TEST_DOCUMENT_PROVIDER_1_AUTHORITY,
                ),
            )
        fakeHealthDataExportManager.setExportImportDocumentProviders(documentProviders)
        launchFragment<ImportSourceLocationFragment>(Bundle())

        onView(withText(TEST_DOCUMENT_PROVIDER_1_TITLE)).perform(click())
        onView(withText(TEST_DOCUMENT_PROVIDER_1_ROOT_2_SUMMARY))
            .inRoot(isDialog())
            .perform(click())
        onView(withText("Done")).inRoot(isDialog()).perform(click())
        onView(withText("Next")).perform(click())

        intended(hasAction(Intent.ACTION_OPEN_DOCUMENT))
        intended(hasType("application/zip"))
        intended(hasExtra(DocumentsContract.EXTRA_INITIAL_URI, TEST_DOCUMENT_PROVIDER_1_ROOT_2_URI))
    }

    @Test
    fun singleProviderAndPlayStoreNotAvailable_showsInstallAppsText() {
        (deviceInfoUtils as FakeDeviceInfoUtils).setPlayStoreAvailability(false)

        val documentProviders =
            listOf(
                ExportImportDocumentProvider(
                    TEST_DOCUMENT_PROVIDER_1_TITLE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_1_SUMMARY,
                    TEST_DOCUMENT_PROVIDER_1_ICON_RESOURCE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_1_URI,
                    TEST_DOCUMENT_PROVIDER_1_AUTHORITY,
                )
            )
        fakeHealthDataExportManager.setExportImportDocumentProviders(documentProviders)
        launchFragment<ImportSourceLocationFragment>(Bundle())

        onView(withText(R.string.export_import_install_apps_text)).check(matches(isDisplayed()))
    }

    @Test
    fun singleProviderAndPlayStoreNotAvailable_doesNotShowPlayStoreButton() {
        (deviceInfoUtils as FakeDeviceInfoUtils).setPlayStoreAvailability(false)

        val documentProviders =
            listOf(
                ExportImportDocumentProvider(
                    TEST_DOCUMENT_PROVIDER_1_TITLE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_1_SUMMARY,
                    TEST_DOCUMENT_PROVIDER_1_ICON_RESOURCE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_1_URI,
                    TEST_DOCUMENT_PROVIDER_1_AUTHORITY,
                )
            )
        fakeHealthDataExportManager.setExportImportDocumentProviders(documentProviders)
        launchFragment<ImportSourceLocationFragment>(Bundle())

        onView(withId(com.android.settingslib.widget.preference.footer.R.id.settingslib_learn_more))
            .check(matches(not(isDisplayed())))
    }

    @Test
    fun singleProviderAndPlayStoreAvailable_showsInstallAppsText() {
        (deviceInfoUtils as FakeDeviceInfoUtils).setPlayStoreAvailability(true)

        val documentProviders =
            listOf(
                ExportImportDocumentProvider(
                    TEST_DOCUMENT_PROVIDER_1_TITLE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_1_SUMMARY,
                    TEST_DOCUMENT_PROVIDER_1_ICON_RESOURCE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_1_URI,
                    TEST_DOCUMENT_PROVIDER_1_AUTHORITY,
                )
            )
        fakeHealthDataExportManager.setExportImportDocumentProviders(documentProviders)
        launchFragment<ImportSourceLocationFragment>(Bundle())

        onView(withText(R.string.export_import_install_apps_text)).check(matches(isDisplayed()))
    }

    @Test
    fun singleProviderAndPlayStoreAvailable_showsPlayStoreButton() {
        (deviceInfoUtils as FakeDeviceInfoUtils).setPlayStoreAvailability(true)

        val documentProviders =
            listOf(
                ExportImportDocumentProvider(
                    TEST_DOCUMENT_PROVIDER_1_TITLE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_1_SUMMARY,
                    TEST_DOCUMENT_PROVIDER_1_ICON_RESOURCE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_1_URI,
                    TEST_DOCUMENT_PROVIDER_1_AUTHORITY,
                )
            )
        fakeHealthDataExportManager.setExportImportDocumentProviders(documentProviders)
        launchFragment<ImportSourceLocationFragment>(Bundle())

        onView(withText(R.string.export_import_go_to_play_store_text)).check(matches(isDisplayed()))
    }

    @Test
    fun singleProviderAndPlayStoreAvailable_goToPlayStoreClicked_launchesPlayStore() {
        (deviceInfoUtils as FakeDeviceInfoUtils).setPlayStoreAvailability(true)

        val documentProviders =
            listOf(
                ExportImportDocumentProvider(
                    TEST_DOCUMENT_PROVIDER_1_TITLE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_1_SUMMARY,
                    TEST_DOCUMENT_PROVIDER_1_ICON_RESOURCE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_1_URI,
                    TEST_DOCUMENT_PROVIDER_1_AUTHORITY,
                )
            )
        fakeHealthDataExportManager.setExportImportDocumentProviders(documentProviders)
        launchFragment<ImportSourceLocationFragment>(Bundle()) {
            navHostController.setGraph(R.navigation.import_nav_graph)
            navHostController.setCurrentDestination(R.id.importSourceLocationFragment)
            Navigation.setViewNavController(this.requireView(), navHostController)
        }

        onView(withText("Go to the Play Store")).perform(click())

        assertThat(navHostController.currentDestination?.id).isEqualTo(R.id.play_store_activity)
    }

    @Test
    fun switchBackToPreviousSelectedDocumentProvider_previousSelectedAccountIsChecked() {
        val documentProviders =
            listOf(
                ExportImportDocumentProvider(
                    TEST_DOCUMENT_PROVIDER_1_TITLE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_1_SUMMARY,
                    TEST_DOCUMENT_PROVIDER_1_ICON_RESOURCE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_1_URI,
                    TEST_DOCUMENT_PROVIDER_1_AUTHORITY,
                ),
                ExportImportDocumentProvider(
                    TEST_DOCUMENT_PROVIDER_1_TITLE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_2_SUMMARY,
                    TEST_DOCUMENT_PROVIDER_1_ICON_RESOURCE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_2_URI,
                    TEST_DOCUMENT_PROVIDER_1_AUTHORITY,
                ),
                ExportImportDocumentProvider(
                    TEST_DOCUMENT_PROVIDER_2_TITLE,
                    TEST_DOCUMENT_PROVIDER_2_ROOT_SUMMARY,
                    TEST_DOCUMENT_PROVIDER_2_ICON_RESOURCE,
                    TEST_DOCUMENT_PROVIDER_2_ROOT_URI,
                    TEST_DOCUMENT_PROVIDER_2_AUTHORITY,
                ),
            )
        fakeHealthDataExportManager.setExportImportDocumentProviders(documentProviders)
        launchFragment<ExportDestinationFragment>(Bundle())

        // Selects the second account for provider 1.
        onView(withText(TEST_DOCUMENT_PROVIDER_1_TITLE)).perform(click())
        onView(withText(TEST_DOCUMENT_PROVIDER_1_ROOT_2_SUMMARY))
            .inRoot(isDialog())
            .perform(click())
        onView(withText("Done")).inRoot(isDialog()).perform(click())
        onView(
                withTitleAndSummary(
                    TEST_DOCUMENT_PROVIDER_1_TITLE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_2_SUMMARY,
                )
            )
            .check(matches(isDisplayed()))
        // Selects the provider 2.
        onView(withText(TEST_DOCUMENT_PROVIDER_2_TITLE)).perform(click())
        onView(checkBoxOf(TEST_DOCUMENT_PROVIDER_2_TITLE)).check(matches(isChecked()))
        // Switches back to provider 1.
        onView(withText(TEST_DOCUMENT_PROVIDER_1_TITLE)).perform(click())

        onView(withText(TEST_DOCUMENT_PROVIDER_1_ROOT_2_SUMMARY))
            .inRoot(isDialog())
            .check(matches(isChecked()))
    }

    @Test
    fun orientationChanged_keepsSelection() {
        val documentProviders =
            listOf(
                ExportImportDocumentProvider(
                    TEST_DOCUMENT_PROVIDER_1_TITLE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_1_SUMMARY,
                    TEST_DOCUMENT_PROVIDER_1_ICON_RESOURCE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_1_URI,
                    TEST_DOCUMENT_PROVIDER_1_AUTHORITY,
                ),
                ExportImportDocumentProvider(
                    TEST_DOCUMENT_PROVIDER_1_TITLE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_2_SUMMARY,
                    TEST_DOCUMENT_PROVIDER_1_ICON_RESOURCE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_2_URI,
                    TEST_DOCUMENT_PROVIDER_1_AUTHORITY,
                ),
                ExportImportDocumentProvider(
                    TEST_DOCUMENT_PROVIDER_2_TITLE,
                    TEST_DOCUMENT_PROVIDER_2_ROOT_SUMMARY,
                    TEST_DOCUMENT_PROVIDER_2_ICON_RESOURCE,
                    TEST_DOCUMENT_PROVIDER_2_ROOT_URI,
                    TEST_DOCUMENT_PROVIDER_2_AUTHORITY,
                ),
            )
        fakeHealthDataExportManager.setExportImportDocumentProviders(documentProviders)
        val scenario = launchFragment<ImportSourceLocationFragment>(Bundle())

        onView(withText(TEST_DOCUMENT_PROVIDER_1_TITLE)).perform(click())
        onView(withText(TEST_DOCUMENT_PROVIDER_1_ROOT_2_SUMMARY))
            .inRoot(isDialog())
            .perform(click())
        onView(withText("Done")).inRoot(isDialog()).perform(click())

        scenario.recreate()
        onIdle()

        onView(checkBoxOf(TEST_DOCUMENT_PROVIDER_1_TITLE)).check(matches(isChecked()))
        onView(
                withTitleAndSummary(
                    TEST_DOCUMENT_PROVIDER_1_TITLE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_2_SUMMARY,
                )
            )
            .check(matches(isDisplayed()))
    }

    @Test
    fun orientationChanged_nextButtonClicked_startsDocumentsUi() {
        val documentProviders =
            listOf(
                ExportImportDocumentProvider(
                    TEST_DOCUMENT_PROVIDER_1_TITLE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_1_SUMMARY,
                    TEST_DOCUMENT_PROVIDER_1_ICON_RESOURCE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_1_URI,
                    TEST_DOCUMENT_PROVIDER_1_AUTHORITY,
                ),
                ExportImportDocumentProvider(
                    TEST_DOCUMENT_PROVIDER_1_TITLE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_2_SUMMARY,
                    TEST_DOCUMENT_PROVIDER_1_ICON_RESOURCE,
                    TEST_DOCUMENT_PROVIDER_1_ROOT_2_URI,
                    TEST_DOCUMENT_PROVIDER_1_AUTHORITY,
                ),
                ExportImportDocumentProvider(
                    TEST_DOCUMENT_PROVIDER_2_TITLE,
                    TEST_DOCUMENT_PROVIDER_2_ROOT_SUMMARY,
                    TEST_DOCUMENT_PROVIDER_2_ICON_RESOURCE,
                    TEST_DOCUMENT_PROVIDER_2_ROOT_URI,
                    TEST_DOCUMENT_PROVIDER_2_AUTHORITY,
                ),
            )
        fakeHealthDataExportManager.setExportImportDocumentProviders(documentProviders)
        val scenario = launchFragment<ImportSourceLocationFragment>(Bundle())

        onView(withText(TEST_DOCUMENT_PROVIDER_1_TITLE)).perform(click())
        onView(withText(TEST_DOCUMENT_PROVIDER_1_ROOT_2_SUMMARY))
            .inRoot(isDialog())
            .perform(click())
        onView(withText("Done")).inRoot(isDialog()).perform(click())

        scenario.recreate()
        onIdle()
        onView(withText("Next")).perform(click())

        intended(hasAction(Intent.ACTION_OPEN_DOCUMENT))
        intended(hasType("application/zip"))
        intended(hasExtra(DocumentsContract.EXTRA_INITIAL_URI, TEST_DOCUMENT_PROVIDER_1_ROOT_2_URI))
    }
}
