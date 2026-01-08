package com.android.healthconnect.controller.tests.migration

import android.content.Context
import android.os.Bundle
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onIdle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.migration.ModuleUpdateRequiredFragment
import com.android.healthconnect.controller.tests.TestActivity
import com.android.healthconnect.controller.tests.utils.launchFragment
import com.android.healthconnect.controller.utils.NavigationUtils
import com.android.healthconnect.controller.utils.SettingsTransitionHelper.createMainlineServiceUpdateSettingsIntent
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.MigrationElement
import com.android.healthconnect.controller.utils.logging.PageName
import com.google.common.truth.Truth
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
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ModuleUpdateRequiredFragmentTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)
    @BindValue val navigationUtils: NavigationUtils = mock()
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
    fun moduleUpdateRequiredFragment_displaysCorrectly() {
        launchFragmentWithNavigation().use {
            onView(withText("Update needed")).check(matches(isDisplayed()))
            onView(
                    withText(
                        "Health Connect is being integrated with the Android system so " +
                            "you can access it directly from your settings.\n\n" +
                            "Before continuing, update your phone system."
                    )
                )
                .check(matches(isDisplayed()))
            onView(
                    withText(
                        "If you've already updated your phone system, " +
                            "try restarting your phone to continue the integration"
                    )
                )
                .check(matches(isDisplayed()))
            onView(withText("Cancel")).check(matches(isDisplayed()))
            onView(withText("Update")).check(matches(isDisplayed()))
            verify(healthConnectLogger, atLeast(1))
                .setPageId(PageName.MIGRATION_MODULE_UPDATE_NEEDED_PAGE)
            verify(healthConnectLogger).logPageImpression()
            verify(healthConnectLogger)
                .logImpression(MigrationElement.MIGRATION_UPDATE_NEEDED_CANCEL_BUTTON)
            verify(healthConnectLogger)
                .logImpression(MigrationElement.MIGRATION_UPDATE_NEEDED_UPDATE_BUTTON)
        }
    }

    @Test
    fun moduleUpdateRequiredFragment_whenCancelButtonPressed_setsSharedPreferences() {
        launchFragmentWithNavigation().use {
            onView(withText("Cancel")).check(matches(isDisplayed()))
            onView(withText("Cancel")).perform(ViewActions.click())

            // Can't use onActivity as it may already be destroyed
            onIdle {
                val preferences =
                    applicationContext.getSharedPreferences(
                        "USER_ACTIVITY_TRACKER",
                        Context.MODE_PRIVATE,
                    )
                Truth.assertThat(preferences.getBoolean("Module Update Seen", false)).isTrue()
            }
            verify(healthConnectLogger)
                .logInteraction(MigrationElement.MIGRATION_UPDATE_NEEDED_CANCEL_BUTTON)
        }
    }

    @Test
    fun moduleUpdateRequiredFragment_whenUpdateButtonPressed_navigatesToSystemUpdate() {
        launchFragmentWithNavigation().use {
            onView(withText("Update")).check(matches(isDisplayed()))
            onView(withText("Update")).perform(ViewActions.click())

            intended(
                hasAction(applicationContext.createMainlineServiceUpdateSettingsIntent().action)
            )
            verify(healthConnectLogger)
                .logInteraction(MigrationElement.MIGRATION_UPDATE_NEEDED_UPDATE_BUTTON)
        }
    }

    private fun launchFragmentWithNavigation(): ActivityScenario<TestActivity> =
        launchFragment<ModuleUpdateRequiredFragment>(Bundle()) {
            navHostController.setGraph(R.navigation.migration_nav_graph)
            navHostController.setCurrentDestination(R.id.migrationModuleUpdateNeededFragment)
            Navigation.setViewNavController(this.requireView(), navHostController)
        }
}
