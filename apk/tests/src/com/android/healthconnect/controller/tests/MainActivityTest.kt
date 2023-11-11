package com.android.healthconnect.controller.tests

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import androidx.test.core.app.ActivityScenario.launchActivityForResult
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.MainActivity
import com.android.healthconnect.controller.migration.MigrationViewModel
import com.android.healthconnect.controller.migration.MigrationViewModel.MigrationFragmentState.WithData
import com.android.healthconnect.controller.migration.api.MigrationState
import com.android.healthconnect.controller.tests.utils.showOnboarding
import com.android.healthconnect.controller.tests.utils.whenever
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito

@HiltAndroidTest
@OptIn(ExperimentalCoroutinesApi::class)
class MainActivityTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @BindValue val viewModel: MigrationViewModel = Mockito.mock(MigrationViewModel::class.java)

    private lateinit var context: Context

    @Before
    fun setup() {
        hiltRule.inject()
        context = InstrumentationRegistry.getInstrumentation().context

        showOnboarding(context, show = false)
    }

    @Test
    fun homeSettingsIntent_onboardingDone_launchesMainActivity() = runTest {
        whenever(viewModel.getCurrentMigrationUiState()).then { MigrationState.COMPLETE_IDLE }
        whenever(viewModel.migrationState).then {
            MutableLiveData(WithData(MigrationState.COMPLETE_IDLE))
        }

        val startActivityIntent =
            Intent.makeMainActivity(ComponentName(context, MainActivity::class.java))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        launchActivityForResult<MainActivity>(startActivityIntent)

        onView(withText("Recent access")).check(matches(isDisplayed()))
        onView(withText("Permissions and data")).check(matches(isDisplayed()))
    }

    @Test
    fun homeSettingsIntent_onboardingNotDone_redirectsToOnboarding() = runTest {
        showOnboarding(context, true)
        whenever(viewModel.getCurrentMigrationUiState()).then { MigrationState.COMPLETE_IDLE }
        whenever(viewModel.migrationState).then {
            MutableLiveData(WithData(MigrationState.COMPLETE_IDLE))
        }

        val startActivityIntent =
            Intent.makeMainActivity(ComponentName(context, MainActivity::class.java))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        launchActivityForResult<MainActivity>(startActivityIntent)

        onView(withText("Share data with your apps"))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
    }

    @Test
    fun homeSettingsIntent_migrationInProgress_redirectsToMigrationScreen() = runTest {
        showOnboarding(context, false)
        whenever(viewModel.getCurrentMigrationUiState()).then { MigrationState.IN_PROGRESS }
        whenever(viewModel.migrationState).then {
            MutableLiveData(WithData(MigrationState.IN_PROGRESS))
        }

        val startActivityIntent =
            Intent.makeMainActivity(ComponentName(context, MainActivity::class.java))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        launchActivityForResult<MainActivity>(startActivityIntent)

        onView(withText("Integration in progress")).check(matches(isDisplayed()))
    }

    @After
    fun tearDown() {
        showOnboarding(context, false)
    }
}
