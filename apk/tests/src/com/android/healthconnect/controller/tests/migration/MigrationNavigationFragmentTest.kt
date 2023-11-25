package com.android.healthconnect.controller.tests.migration

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.migration.MigrationNavigationFragment
import com.android.healthconnect.controller.migration.MigrationViewModel
import com.android.healthconnect.controller.migration.api.MigrationState
import com.android.healthconnect.controller.tests.utils.launchFragment
import com.android.healthconnect.controller.tests.utils.whenever
import com.android.healthconnect.controller.utils.NavigationUtils
import com.google.common.truth.Truth
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

@HiltAndroidTest
class MigrationNavigationFragmentTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)
    @BindValue val navigationUtils: NavigationUtils = Mockito.mock(NavigationUtils::class.java)
    @BindValue
    val migrationViewModel: MigrationViewModel = Mockito.mock(MigrationViewModel::class.java)

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun migrationNavigationFragment_whenMigrationLoading_showsLoading() {
        whenever(migrationViewModel.migrationState).then {
            MutableLiveData<MigrationViewModel.MigrationFragmentState>(
                MigrationViewModel.MigrationFragmentState.Loading)
        }

        launchFragment<MigrationNavigationFragment>()

        onView(withId(R.id.progress_indicator)).check(matches(isDisplayed()))
    }

    @Test
    fun migrationNavigationFragment_whenMigrationError_showsError() {
        whenever(migrationViewModel.migrationState).then {
            MutableLiveData<MigrationViewModel.MigrationFragmentState>(
                MigrationViewModel.MigrationFragmentState.Error)
        }

        launchFragment<MigrationNavigationFragment>()

        onView(withId(R.id.error_view)).check(matches(isDisplayed()))
    }

    @Test
    fun migrationNavigationFragment_whenMigrationStateAllowedNotStarted_navigatesToMigrationPausedFragment() {
        Mockito.doNothing().whenever(navigationUtils).navigate(any(), any())
        whenever(migrationViewModel.migrationState).then {
            MutableLiveData<MigrationViewModel.MigrationFragmentState>(
                MigrationViewModel.MigrationFragmentState.WithData(
                    MigrationState.ALLOWED_NOT_STARTED))
        }
        launchFragment<MigrationNavigationFragment>()

        verify(navigationUtils, times(1))
            .navigate(any(), eq(R.id.action_migrationNavigationFragment_to_migrationPausedFragment))
    }

    @Test
    fun migrationNavigationFragment_whenMigrationStateAllowedPaused_navigatesToMigrationPausedFragment() {
        Mockito.doNothing().whenever(navigationUtils).navigate(any(), any())
        whenever(migrationViewModel.migrationState).then {
            MutableLiveData<MigrationViewModel.MigrationFragmentState>(
                MigrationViewModel.MigrationFragmentState.WithData(MigrationState.ALLOWED_PAUSED))
        }
        launchFragment<MigrationNavigationFragment>()

        verify(navigationUtils, times(1))
            .navigate(any(), eq(R.id.action_migrationNavigationFragment_to_migrationPausedFragment))
    }

    @Test
    fun migrationNavigationFragment_whenMigrationStateAppUpdateRequired_navigatesToAppUpdateRequiredFragment() {
        Mockito.doNothing().whenever(navigationUtils).navigate(any(), any())
        whenever(migrationViewModel.migrationState).then {
            MutableLiveData<MigrationViewModel.MigrationFragmentState>(
                MigrationViewModel.MigrationFragmentState.WithData(
                    MigrationState.APP_UPGRADE_REQUIRED))
        }
        launchFragment<MigrationNavigationFragment>()

        verify(navigationUtils, times(1))
            .navigate(
                any(),
                eq(R.id.action_migrationNavigationFragment_to_migrationAppUpdateNeededFragment))
    }

    @Test
    fun migrationNavigationFragment_whenMigrationStateModuleUpdateRequired_navigatesToModuleUpdateRequiredFragment() {
        Mockito.doNothing().whenever(navigationUtils).navigate(any(), any())
        whenever(migrationViewModel.migrationState).then {
            MutableLiveData<MigrationViewModel.MigrationFragmentState>(
                MigrationViewModel.MigrationFragmentState.WithData(
                    MigrationState.MODULE_UPGRADE_REQUIRED))
        }
        launchFragment<MigrationNavigationFragment>()

        verify(navigationUtils, times(1))
            .navigate(
                any(),
                eq(R.id.action_migrationNavigationFragment_to_migrationModuleUpdateNeededFragment))
    }

    @Test
    fun migrationNavigationFragment_whenMigrationStateInProgress_navigatesToMigrationInProgressFragment() {
        Mockito.doNothing().whenever(navigationUtils).navigate(any(), any())
        whenever(migrationViewModel.migrationState).then {
            MutableLiveData<MigrationViewModel.MigrationFragmentState>(
                MigrationViewModel.MigrationFragmentState.WithData(MigrationState.IN_PROGRESS))
        }
        launchFragment<MigrationNavigationFragment>()

        verify(navigationUtils, times(1))
            .navigate(
                any(), eq(R.id.action_migrationNavigationFragment_to_migrationInProgressFragment))
    }

    @Test
    fun migrationNavigationFragment_whenMigrationStateCompleteIdle_setsPreferenceAndNavigatesToHomeFragment() {
        Mockito.doNothing().whenever(navigationUtils).navigate(any(), any())
        whenever(migrationViewModel.migrationState).then {
            MutableLiveData<MigrationViewModel.MigrationFragmentState>(
                MigrationViewModel.MigrationFragmentState.WithData(MigrationState.COMPLETE_IDLE))
        }
        val scenario = launchFragment<MigrationNavigationFragment>()
        scenario.onActivity { activity ->
            val preferences =
                activity.getSharedPreferences("USER_ACTIVITY_TRACKER", Context.MODE_PRIVATE)
            Truth.assertThat(preferences.getBoolean("migration_complete_key", false)).isTrue()
        }

        verify(navigationUtils, times(1))
            .navigate(any(), eq(R.id.action_migrationNavigationFragment_to_homeFragment))
    }

    @Test
    fun migrationNavigationFragment_whenMigrationStateComplete_setsPreferenceAndNavigatesToHomeFragment() {
        Mockito.doNothing().whenever(navigationUtils).navigate(any(), any())
        whenever(migrationViewModel.migrationState).then {
            MutableLiveData<MigrationViewModel.MigrationFragmentState>(
                MigrationViewModel.MigrationFragmentState.WithData(MigrationState.COMPLETE))
        }
        val scenario = launchFragment<MigrationNavigationFragment>()
        scenario.onActivity { activity ->
            val preferences =
                activity.getSharedPreferences("USER_ACTIVITY_TRACKER", Context.MODE_PRIVATE)
            Truth.assertThat(preferences.getBoolean("migration_complete_key", false)).isTrue()
        }

        verify(navigationUtils, times(1))
            .navigate(any(), eq(R.id.action_migrationNavigationFragment_to_homeFragment))
    }

    @Test
    fun migrationNavigationFragment_whenMigrationStateIdle_navigatesToHomeFragment() {
        Mockito.doNothing().whenever(navigationUtils).navigate(any(), any())
        whenever(migrationViewModel.migrationState).then {
            MutableLiveData<MigrationViewModel.MigrationFragmentState>(
                MigrationViewModel.MigrationFragmentState.WithData(MigrationState.IDLE))
        }

        launchFragment<MigrationNavigationFragment>()

        verify(navigationUtils, times(1))
            .navigate(any(), eq(R.id.action_migrationNavigationFragment_to_homeFragment))
    }

    @Test
    fun migrationNavigationFragment_whenMigrationStateAllowedMigratorDisabled_navigatesToHomeFragment() {
        Mockito.doNothing().whenever(navigationUtils).navigate(any(), any())
        whenever(migrationViewModel.migrationState).then {
            MutableLiveData<MigrationViewModel.MigrationFragmentState>(
                MigrationViewModel.MigrationFragmentState.WithData(
                    MigrationState.ALLOWED_MIGRATOR_DISABLED))
        }

        launchFragment<MigrationNavigationFragment>()

        verify(navigationUtils, times(1))
            .navigate(any(), eq(R.id.action_migrationNavigationFragment_to_homeFragment))
    }

    @Test
    fun migrationNavigationFragment_whenMigrationStateUnknown_navigatesToHomeFragment() {
        Mockito.doNothing().whenever(navigationUtils).navigate(any(), any())
        whenever(migrationViewModel.migrationState).then {
            MutableLiveData<MigrationViewModel.MigrationFragmentState>(
                MigrationViewModel.MigrationFragmentState.WithData(MigrationState.UNKNOWN))
        }

        launchFragment<MigrationNavigationFragment>()

        verify(navigationUtils, times(1))
            .navigate(any(), eq(R.id.action_migrationNavigationFragment_to_homeFragment))
    }
}
