package com.android.healthconnect.controller.tests.migration

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.preference.PreferenceManager.getDefaultSharedPreferencesName
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.espresso.Espresso.onIdle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.migration.MigrationPausedFragment
import com.android.healthconnect.controller.tests.utils.launchFragment
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.MigrationElement
import com.android.healthconnect.controller.utils.logging.PageName
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MigrationPausedFragmentTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)
    @BindValue val healthConnectLogger: HealthConnectLogger = mock()
    private lateinit var navHostController: TestNavHostController

    @Inject @ApplicationContext lateinit var applicationContext: Context

    @Before
    fun setup() {
        hiltRule.inject()
        val context = InstrumentationRegistry.getInstrumentation().context
        navHostController = TestNavHostController(context)
        val pref =
            applicationContext.getSharedPreferences(
                getDefaultSharedPreferencesName(context),
                MODE_PRIVATE,
            )
        pref.edit().clear().apply()
    }

    @After
    fun tearDown() {
        reset(healthConnectLogger)
    }

    @Test
    fun migrationPausedFragment_displaysCorrectly() {
        launchFragment<MigrationPausedFragment>(Bundle()) {
                navHostController.setGraph(R.navigation.migration_nav_graph)
                navHostController.setCurrentDestination(R.id.migrationPausedFragment)
                Navigation.setViewNavController(this.requireView(), navHostController)
            }
            .use {
                onView(withText("Integration paused")).check(matches(isDisplayed()))
                onView(
                        withText(
                            "The Health Connect app closed while it was being integrated " +
                                "with the Android system.\n\nClick resume to reopen the app and continue " +
                                "transferring your data and permissions."
                        )
                    )
                    .check(matches(isDisplayed()))
                onView(withText("Cancel")).check(matches(isDisplayed()))
                onView(withText("Resume")).check(matches(isDisplayed()))
                verify(healthConnectLogger, atLeast(1)).setPageId(PageName.MIGRATION_PAUSED_PAGE)
                verify(healthConnectLogger).logPageImpression()
                verify(healthConnectLogger)
                    .logImpression(MigrationElement.MIGRATION_PAUSED_CONTINUE_BUTTON)
            }
    }

    @Test
    fun migrationPausedFragment_whenCancelButtonPressed_setsSharedPreferences() {
        //        Mockito.doNothing().whenever(navigationUtils).navigate(any(), any())
        launchFragment<MigrationPausedFragment>(Bundle()) {
                navHostController.setGraph(R.navigation.migration_nav_graph)
                navHostController.setCurrentDestination(R.id.migrationPausedFragment)
                Navigation.setViewNavController(this.requireView(), navHostController)
            }
            .use {
                onView(withText("Cancel")).check(matches(isDisplayed()))
                onView(withText("Cancel")).perform(ViewActions.click())

                // Can't use onActivity as it may already be destroyed
                onIdle {
                    val preferences =
                        applicationContext.getSharedPreferences(
                            "USER_ACTIVITY_TRACKER",
                            MODE_PRIVATE,
                        )
                    assertThat(preferences.getBoolean("integration_paused_seen", false)).isTrue()
                }
                verify(healthConnectLogger)
                    .logInteraction(MigrationElement.MIGRATION_UPDATE_NEEDED_CANCEL_BUTTON)
            }
    }

    @Test
    fun migrationPausedFragment_whenResumeButtonPressed_navigatesToMigratorApk() {
        //        Mockito.doNothing().whenever(navigationUtils).navigate(any(), any())
        launchFragment<MigrationPausedFragment>(Bundle()) {
                navHostController.setGraph(R.navigation.migration_nav_graph)
                navHostController.setCurrentDestination(R.id.migrationPausedFragment)
                Navigation.setViewNavController(this.requireView(), navHostController)
            }
            .use {
                onView(withText("Resume")).check(matches(isDisplayed()))
                onView(withText("Resume")).perform(ViewActions.click())

                assertThat(navHostController.currentDestination?.id).isEqualTo(R.id.migrationApk)
                verify(healthConnectLogger)
                    .logInteraction(MigrationElement.MIGRATION_PAUSED_CONTINUE_BUTTON)
            }
    }

    @Test
    fun migrationPausedFragment_whenNavigateToMigratorApkFails_displaysCorrectly() {
        //        whenever(navigationUtils.navigate(any(),
        // any())).thenThrow(RuntimeException("Exception"))
        launchFragment<MigrationPausedFragment>(Bundle()).use {
            onView(withText("Resume")).check(matches(isDisplayed()))
            onView(withText("Resume")).perform(ViewActions.click())

            verify(healthConnectLogger)
                .logInteraction(MigrationElement.MIGRATION_PAUSED_CONTINUE_BUTTON)

            onView(withText("Integration paused")).check(matches(isDisplayed()))
            onView(
                    withText(
                        "The Health Connect app closed while it was being integrated " +
                            "with the Android system.\n\nClick resume to reopen the app and continue " +
                            "transferring your data and permissions."
                    )
                )
                .check(matches(isDisplayed()))
            onView(withText("Cancel")).check(matches(isDisplayed()))
            onView(withText("Resume")).check(matches(isDisplayed()))
        }
    }
}
