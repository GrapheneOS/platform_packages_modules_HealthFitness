package com.android.healthconnect.controller.tests.migration

import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.migration.MigrationNavigationFragment
import com.android.healthconnect.controller.migration.MigrationViewModel
import com.android.healthconnect.controller.migration.api.MigrationRestoreState
import com.android.healthconnect.controller.migration.api.MigrationRestoreState.DataRestoreUiError
import com.android.healthconnect.controller.migration.api.MigrationRestoreState.DataRestoreUiState
import com.android.healthconnect.controller.migration.api.MigrationRestoreState.MigrationUiState
import com.android.healthconnect.controller.tests.TestActivity
import com.android.healthconnect.controller.tests.utils.launchFragment
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.times
import org.mockito.kotlin.whenever

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MigrationNavigationFragmentTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)
    @BindValue
    val migrationViewModel: MigrationViewModel = Mockito.mock(MigrationViewModel::class.java)

    private lateinit var navHostController: TestNavHostController

    @Before
    fun setup() {
        hiltRule.inject()
        navHostController =
            TestNavHostController(InstrumentationRegistry.getInstrumentation().context)
    }

    @Test
    fun whenMigrationFragmentStateLoading_showsLoading() {
        whenever(migrationViewModel.migrationState).then {
            MutableLiveData<MigrationViewModel.MigrationFragmentState>(
                MigrationViewModel.MigrationFragmentState.Loading
            )
        }

        launchFragment<MigrationNavigationFragment>().use {
            onView(withId(R.id.progress_indicator)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun whenMigrationFragmentStateError_showsError() {
        whenever(migrationViewModel.migrationState).then {
            MutableLiveData<MigrationViewModel.MigrationFragmentState>(
                MigrationViewModel.MigrationFragmentState.Error
            )
        }

        launchFragment<MigrationNavigationFragment>().use {
            onView(withId(R.id.error_view)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun whenMigrationStateAllowedNotStarted_navigatesToMigrationPausedFragment() {
        val state = MutableLiveData<MigrationViewModel.MigrationFragmentState>()
        whenever(migrationViewModel.migrationState).thenReturn(state)

        launchFragmentWithNavigation().use {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                state.value =
                    MigrationViewModel.MigrationFragmentState.WithData(
                        MigrationRestoreState(
                            migrationUiState = MigrationUiState.ALLOWED_NOT_STARTED,
                            dataRestoreState = DataRestoreUiState.IDLE,
                            dataRestoreError = DataRestoreUiError.ERROR_NONE,
                        )
                    )
            }
            assertThat(navHostController.currentDestination?.id)
                .isEqualTo(R.id.migrationPausedFragment)
        }
    }

    @Test
    fun whenMigrationStateAllowedPaused_navigatesToMigrationPausedFragment() {
        val state = MutableLiveData<MigrationViewModel.MigrationFragmentState>()
        whenever(migrationViewModel.migrationState).thenReturn(state)

        launchFragmentWithNavigation().use {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                state.value =
                    MigrationViewModel.MigrationFragmentState.WithData(
                        MigrationRestoreState(
                            migrationUiState = MigrationUiState.ALLOWED_PAUSED,
                            dataRestoreState = DataRestoreUiState.IDLE,
                            dataRestoreError = DataRestoreUiError.ERROR_NONE,
                        )
                    )
            }
            assertThat(navHostController.currentDestination?.id)
                .isEqualTo(R.id.migrationPausedFragment)
        }
    }

    @Test
    fun whenMigrationStateAppUpdateRequired_navigatesToAppUpdateRequiredFragment() {
        val state = MutableLiveData<MigrationViewModel.MigrationFragmentState>()
        whenever(migrationViewModel.migrationState).thenReturn(state)

        launchFragmentWithNavigation().use {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                state.value =
                    MigrationViewModel.MigrationFragmentState.WithData(
                        MigrationRestoreState(
                            migrationUiState = MigrationUiState.APP_UPGRADE_REQUIRED,
                            dataRestoreState = DataRestoreUiState.IDLE,
                            dataRestoreError = DataRestoreUiError.ERROR_NONE,
                        )
                    )
            }
            assertThat(navHostController.currentDestination?.id)
                .isEqualTo(R.id.migrationAppUpdateNeededFragment)
        }
    }

    @Test
    fun whenMigrationStateModuleUpdateRequired_navigatesToModuleUpdateRequiredFragment() {
        val state = MutableLiveData<MigrationViewModel.MigrationFragmentState>()
        whenever(migrationViewModel.migrationState).thenReturn(state)

        launchFragmentWithNavigation().use {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                state.value =
                    MigrationViewModel.MigrationFragmentState.WithData(
                        MigrationRestoreState(
                            migrationUiState = MigrationUiState.MODULE_UPGRADE_REQUIRED,
                            dataRestoreState = DataRestoreUiState.IDLE,
                            dataRestoreError = DataRestoreUiError.ERROR_NONE,
                        )
                    )
            }
            assertThat(navHostController.currentDestination?.id)
                .isEqualTo(R.id.migrationModuleUpdateNeededFragment)
        }
    }

    @Test
    fun whenMigrationStateInProgress_navigatesToMigrationInProgressFragment() {
        val state = MutableLiveData<MigrationViewModel.MigrationFragmentState>()
        whenever(migrationViewModel.migrationState).thenReturn(state)

        launchFragmentWithNavigation().use {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                state.value =
                    MigrationViewModel.MigrationFragmentState.WithData(
                        MigrationRestoreState(
                            migrationUiState = MigrationUiState.IN_PROGRESS,
                            dataRestoreState = DataRestoreUiState.IDLE,
                            dataRestoreError = DataRestoreUiError.ERROR_NONE,
                        )
                    )
            }
            assertThat(navHostController.currentDestination?.id)
                .isEqualTo(R.id.migrationInProgressFragment)
        }
    }

    @Test
    fun whenMigrationStateCompleteIdle_navigatesToHomeFragment() {
        val state = MutableLiveData<MigrationViewModel.MigrationFragmentState>()
        whenever(migrationViewModel.migrationState).thenReturn(state)

        launchFragmentWithNavigation().use {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                state.value =
                    MigrationViewModel.MigrationFragmentState.WithData(
                        MigrationRestoreState(
                            migrationUiState = MigrationUiState.COMPLETE_IDLE,
                            dataRestoreState = DataRestoreUiState.IDLE,
                            dataRestoreError = DataRestoreUiError.ERROR_NONE,
                        )
                    )
            }
            assertThat(navHostController.currentDestination?.id)
                .isEqualTo(R.id.healthConnectHomeActivity)
        }
    }

    @Test
    fun whenMigrationStateComplete_navigatesToHomeFragment() {
        val state = MutableLiveData<MigrationViewModel.MigrationFragmentState>()
        whenever(migrationViewModel.migrationState).thenReturn(state)

        launchFragmentWithNavigation().use {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                state.value =
                    MigrationViewModel.MigrationFragmentState.WithData(
                        MigrationRestoreState(
                            migrationUiState = MigrationUiState.COMPLETE,
                            dataRestoreState = DataRestoreUiState.IDLE,
                            dataRestoreError = DataRestoreUiError.ERROR_NONE,
                        )
                    )
            }
            assertThat(navHostController.currentDestination?.id)
                .isEqualTo(R.id.healthConnectHomeActivity)
        }
    }

    @Test
    fun whenMigrationStateIdle_navigatesToHomeFragment() {
        val state = MutableLiveData<MigrationViewModel.MigrationFragmentState>()
        whenever(migrationViewModel.migrationState).thenReturn(state)

        launchFragmentWithNavigation().use {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                state.value =
                    MigrationViewModel.MigrationFragmentState.WithData(
                        MigrationRestoreState(
                            migrationUiState = MigrationUiState.IDLE,
                            dataRestoreState = DataRestoreUiState.IDLE,
                            dataRestoreError = DataRestoreUiError.ERROR_NONE,
                        )
                    )
            }
            assertThat(navHostController.currentDestination?.id)
                .isEqualTo(R.id.healthConnectHomeActivity)
        }
    }

    @Test
    fun whenMigrationStateAllowedMigratorDisabled_navigatesToHomeFragment() {
        val state = MutableLiveData<MigrationViewModel.MigrationFragmentState>()
        whenever(migrationViewModel.migrationState).thenReturn(state)

        launchFragmentWithNavigation().use {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                state.value =
                    MigrationViewModel.MigrationFragmentState.WithData(
                        MigrationRestoreState(
                            migrationUiState = MigrationUiState.ALLOWED_MIGRATOR_DISABLED,
                            dataRestoreState = DataRestoreUiState.IDLE,
                            dataRestoreError = DataRestoreUiError.ERROR_NONE,
                        )
                    )
            }
            assertThat(navHostController.currentDestination?.id)
                .isEqualTo(R.id.healthConnectHomeActivity)
        }
    }

    @Test
    fun whenMigrationStateUnknown_navigatesToHomeFragment() {
        val state = MutableLiveData<MigrationViewModel.MigrationFragmentState>()
        whenever(migrationViewModel.migrationState).thenReturn(state)

        launchFragmentWithNavigation().use {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                state.value =
                    MigrationViewModel.MigrationFragmentState.WithData(
                        MigrationRestoreState(
                            migrationUiState = MigrationUiState.UNKNOWN,
                            dataRestoreState = DataRestoreUiState.IDLE,
                            dataRestoreError = DataRestoreUiError.ERROR_NONE,
                        )
                    )
            }
            assertThat(navHostController.currentDestination?.id)
                .isEqualTo(R.id.healthConnectHomeActivity)
        }
    }

    @Test
    fun whenDataRestoreInProgress_navigatesToDataRestoreInProgressScreen() {
        val state = MutableLiveData<MigrationViewModel.MigrationFragmentState>()
        whenever(migrationViewModel.migrationState).thenReturn(state)

        launchFragmentWithNavigation().use {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                state.value =
                    MigrationViewModel.MigrationFragmentState.WithData(
                        MigrationRestoreState(
                            migrationUiState = MigrationUiState.IDLE,
                            dataRestoreState = DataRestoreUiState.IN_PROGRESS,
                            dataRestoreError = DataRestoreUiError.ERROR_NONE,
                        )
                    )
            }
            assertThat(navHostController.currentDestination?.id)
                .isEqualTo(R.id.dataRestoreInProgressFragment)
        }
    }

    @Test
    fun whenDataRestorePending_navigatesToHomeFragment() {
        val state = MutableLiveData<MigrationViewModel.MigrationFragmentState>()
        whenever(migrationViewModel.migrationState).thenReturn(state)

        launchFragmentWithNavigation().use {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                state.value =
                    MigrationViewModel.MigrationFragmentState.WithData(
                        MigrationRestoreState(
                            migrationUiState = MigrationUiState.IDLE,
                            dataRestoreState = DataRestoreUiState.PENDING,
                            dataRestoreError = DataRestoreUiError.ERROR_NONE,
                        )
                    )
            }
            assertThat(navHostController.currentDestination?.id)
                .isEqualTo(R.id.healthConnectHomeActivity)
        }
    }

    @Test
    fun whenDataRestoreError_navigatesToHomeFragment() {
        val state = MutableLiveData<MigrationViewModel.MigrationFragmentState>()
        whenever(migrationViewModel.migrationState).thenReturn(state)

        launchFragmentWithNavigation().use {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                state.value =
                    MigrationViewModel.MigrationFragmentState.WithData(
                        MigrationRestoreState(
                            migrationUiState = MigrationUiState.IDLE,
                            dataRestoreState = DataRestoreUiState.IDLE,
                            dataRestoreError = DataRestoreUiError.ERROR_FETCHING_DATA,
                        )
                    )
            }
            assertThat(navHostController.currentDestination?.id)
                .isEqualTo(R.id.healthConnectHomeActivity)
        }
    }

    private fun launchFragmentWithNavigation(): ActivityScenario<TestActivity> =
        launchFragment<MigrationNavigationFragment>(Bundle()) {
            navHostController.setGraph(R.navigation.migration_nav_graph)
            navHostController.setCurrentDestination(R.id.migrationNavigationFragment)
            Navigation.setViewNavController(this.requireView(), navHostController)
        }
}
