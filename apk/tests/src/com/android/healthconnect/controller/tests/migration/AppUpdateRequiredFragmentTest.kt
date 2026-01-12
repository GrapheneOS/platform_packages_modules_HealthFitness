package com.android.healthconnect.controller.tests.migration

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onIdle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasPackage
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.migration.AppUpdateRequiredFragment
import com.android.healthconnect.controller.tests.TestActivity
import com.android.healthconnect.controller.tests.utils.launchFragment
import com.android.healthconnect.controller.utils.AppStoreUtils
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.MigrationElement
import com.android.healthconnect.controller.utils.logging.PageName
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AppUpdateRequiredFragmentTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)
    @BindValue val appStoreUtils: AppStoreUtils = Mockito.mock(AppStoreUtils::class.java)
    @BindValue val healthConnectLogger: HealthConnectLogger = mock()

    @Inject @ApplicationContext lateinit var applicationContext: Context
    private lateinit var navHostController: TestNavHostController

    @Before
    fun setup() {
        hiltRule.inject()
        Intents.init()
        navHostController = TestNavHostController(applicationContext)
    }

    @After
    fun tearDown() {
        Intents.release()
        reset(healthConnectLogger)
    }

    @Test
    fun appUpdateRequiredFragment_displaysCorrectly() {
        launchFragmentWithNavigation().use {
            onView(withText("Update needed")).check(matches(isDisplayed()))
            onView(
                    withText(
                        "Health Connect is being integrated with the Android system so " +
                            "you can access it directly from your settings.\n\n" +
                            "Before continuing, update the Health Connect app to the latest version."
                    )
                )
                .check(matches(isDisplayed()))
            onView(withText("Cancel")).check(matches(isDisplayed()))
            onView(withText("Update")).check(matches(isDisplayed()))
            verify(healthConnectLogger, atLeast(1))
                .setPageId(PageName.MIGRATION_APP_UPDATE_NEEDED_PAGE)
            verify(healthConnectLogger).logPageImpression()
            verify(healthConnectLogger)
                .logImpression(MigrationElement.MIGRATION_UPDATE_NEEDED_UPDATE_BUTTON)
            verify(healthConnectLogger)
                .logImpression(MigrationElement.MIGRATION_UPDATE_NEEDED_CANCEL_BUTTON)
        }
    }

    @Test
    fun appUpdateRequiredFragment_ifAppStoreExists_intentToAppStore() {
        whenever(appStoreUtils.getAppStoreLink(any()))
            .thenReturn(
                Intent(Intent.ACTION_SHOW_APP_INFO).also { it.setPackage("installer.package.name") }
            )
        launchFragmentWithNavigation().use {
            onView(withText("Update")).check(matches(isDisplayed()))
            onView(withText("Update")).perform(click())

            intended(
                allOf(hasAction(Intent.ACTION_SHOW_APP_INFO), hasPackage("installer.package.name"))
            )
            verify(healthConnectLogger)
                .logInteraction(MigrationElement.MIGRATION_UPDATE_NEEDED_UPDATE_BUTTON)
        }
    }

    @Test
    fun appUpdateRequiredFragment_ifAppStoreDoesNotExist_doesNotNavigateToAppStore() {
        whenever(appStoreUtils.getAppStoreLink(any())).thenReturn(null)

        launchFragmentWithNavigation().use {
            onView(withText("Update")).check(matches(isDisplayed()))
            onView(withText("Update")).perform(click())

            // Check we are still on the same page
            onView(
                    withText(
                        "Health Connect is being integrated with the Android system so " +
                            "you can access it directly from your settings.\n\n" +
                            "Before continuing, update the Health Connect app to the latest version."
                    )
                )
                .check(matches(isDisplayed()))

            verify(healthConnectLogger)
                .logInteraction(MigrationElement.MIGRATION_UPDATE_NEEDED_UPDATE_BUTTON)
        }
    }

    @Test
    fun appUpdateRequiredFragment_whenCancelButtonPressed_setsSharedPreferences() {
        launchFragmentWithNavigation().use {
            onView(withText("Cancel")).check(matches(isDisplayed()))
            onView(withText("Cancel")).perform(click())

            // Can't use onActivity as it may already be destroyed
            onIdle {
                val preferences =
                    applicationContext.getSharedPreferences(
                        "USER_ACTIVITY_TRACKER",
                        Context.MODE_PRIVATE,
                    )
                assertThat(preferences.getBoolean("App Update Seen", false)).isTrue()
            }
            assertThat(navHostController.currentDestination?.id)
                .isEqualTo(R.id.healthConnectHomeActivity)
            verify(healthConnectLogger)
                .logInteraction(MigrationElement.MIGRATION_UPDATE_NEEDED_CANCEL_BUTTON)
        }
    }

    private fun launchFragmentWithNavigation(): ActivityScenario<TestActivity> =
        launchFragment<AppUpdateRequiredFragment>(Bundle()) {
            navHostController.setGraph(R.navigation.migration_nav_graph)
            navHostController.setCurrentDestination(R.id.migrationAppUpdateNeededFragment)
            Navigation.setViewNavController(this.requireView(), navHostController)
        }
}
