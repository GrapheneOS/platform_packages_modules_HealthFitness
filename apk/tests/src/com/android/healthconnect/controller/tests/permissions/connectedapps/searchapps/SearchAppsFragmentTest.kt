package com.android.healthconnect.controller.tests.permissions.connectedapps.searchapps

import android.content.Context
import android.health.connect.HealthConnectManager
import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withTagValue
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.permissions.connectedapps.ConnectedAppsViewModel
import com.android.healthconnect.controller.permissions.connectedapps.searchapps.SearchAppsFragment
import com.android.healthconnect.controller.service.HealthManagerModule
import com.android.healthconnect.controller.shared.app.AppMetadata
import com.android.healthconnect.controller.shared.app.AppPermissionsType
import com.android.healthconnect.controller.shared.app.ConnectedAppMetadata
import com.android.healthconnect.controller.shared.app.ConnectedAppStatus
import com.android.healthconnect.controller.tests.utils.TEST_APP
import com.android.healthconnect.controller.tests.utils.TEST_APP_2
import com.android.healthconnect.controller.tests.utils.TEST_APP_3
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME_2
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME_3
import com.android.healthconnect.controller.tests.utils.isAbove
import com.android.healthconnect.controller.tests.utils.launchFragment
import com.android.healthconnect.controller.utils.logging.AppPermissionsElement
import com.android.healthconnect.controller.utils.logging.HealthConnectLogger
import com.android.healthconnect.controller.utils.logging.PageName
import com.google.common.truth.Truth
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.hamcrest.Matchers.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@HiltAndroidTest
@UninstallModules(HealthManagerModule::class)
@RunWith(AndroidJUnit4::class)
class SearchAppsFragmentTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @BindValue
    val viewModel: ConnectedAppsViewModel = Mockito.mock(ConnectedAppsViewModel::class.java)
    @BindValue val manager: HealthConnectManager = mock()
    @BindValue val healthConnectLogger: HealthConnectLogger = mock()
    private lateinit var navHostController: TestNavHostController
    private lateinit var context: Context

    @Before
    fun setup() {
        hiltRule.inject()
        context = InstrumentationRegistry.getInstrumentation().context
        navHostController = TestNavHostController(context)
    }

    @After
    fun teardown() {
        reset(healthConnectLogger)
    }

    @Test
    fun searchAppsFragment_isDisplayedCorrectly() {
        whenever(viewModel.connectedApps).then {
            MutableLiveData(
                listOf(
                    ConnectedAppMetadata(TEST_APP, status = ConnectedAppStatus.ALLOWED),
                    ConnectedAppMetadata(TEST_APP_2, status = ConnectedAppStatus.DENIED),
                    ConnectedAppMetadata(TEST_APP_3, status = ConnectedAppStatus.INACTIVE),
                )
            )
        }

        launchFragment<SearchAppsFragment>(Bundle()).use {
            onView(withText("Allowed access")).check(matches(isDisplayed()))
            onView(withText(TEST_APP_NAME)).check(matches(isDisplayed()))
            onView(withText("Not allowed access")).check(matches(isDisplayed()))
            onView(withText(TEST_APP_NAME_2)).check(matches(isDisplayed()))
            onView(withText("Inactive apps")).check(matches(isDisplayed()))
            onView(withText(TEST_APP_NAME_3)).check(matches(isDisplayed()))
            onView(withContentDescription("Delete data for Health Connect test app 3 button"))
                .check(matches(isDisplayed()))
            onView(withText(R.string.connected_apps_text)).check(matches(isDisplayed()))

            verify(healthConnectLogger, atLeast(1)).setPageId(PageName.SEARCH_APPS_PAGE)
            verify(healthConnectLogger).logPageImpression()
            verify(healthConnectLogger).logImpression(AppPermissionsElement.CONNECTED_APP_BUTTON)
            verify(healthConnectLogger)
                .logImpression(AppPermissionsElement.NOT_CONNECTED_APP_BUTTON)
            verify(healthConnectLogger).logImpression(AppPermissionsElement.INACTIVE_APP_BUTTON)
        }
    }

    @Test
    fun searchAppsFragment_inactiveAppClicked_deleteDialogOpened() {
        whenever(viewModel.connectedApps).then {
            MutableLiveData(
                listOf(
                    ConnectedAppMetadata(TEST_APP, status = ConnectedAppStatus.ALLOWED),
                    ConnectedAppMetadata(TEST_APP_2, status = ConnectedAppStatus.DENIED),
                    ConnectedAppMetadata(TEST_APP_3, status = ConnectedAppStatus.INACTIVE),
                )
            )
        }

        launchFragment<SearchAppsFragment>(Bundle()).use {
            onView(withTagValue(`is`("Delete button inactive app"))).perform(click())

            onView(withText("Permanently delete all $TEST_APP_NAME_3 data?"))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))
            verify(healthConnectLogger)
                .logInteraction(AppPermissionsElement.INACTIVE_APP_DELETE_BUTTON)
        }
    }

    @Test
    fun noAppsAllowed_doesNotShowAllowedAccessSection() {
        whenever(viewModel.connectedApps).then {
            MutableLiveData(
                listOf(
                    ConnectedAppMetadata(TEST_APP, status = ConnectedAppStatus.DENIED),
                    ConnectedAppMetadata(TEST_APP_2, status = ConnectedAppStatus.DENIED),
                    ConnectedAppMetadata(TEST_APP_3, status = ConnectedAppStatus.INACTIVE),
                )
            )
        }

        launchFragment<SearchAppsFragment>(Bundle()).use {
            onView(withText("Allowed access")).check(doesNotExist())
            onView(withText("Not allowed access")).check(matches(isDisplayed()))
            onView(withText(TEST_APP_NAME)).check(matches(isDisplayed()))
            onView(withText(TEST_APP_NAME_2)).check(matches(isDisplayed()))
            onView(withText("Inactive apps")).check(matches(isDisplayed()))
            onView(withText(TEST_APP_NAME_3)).check(matches(isDisplayed()))
            onView(withText(R.string.connected_apps_text)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun noAppsDenied_doesNotShowAllowedAccessSection() {
        whenever(viewModel.connectedApps).then {
            MutableLiveData(
                listOf(
                    ConnectedAppMetadata(TEST_APP, status = ConnectedAppStatus.ALLOWED),
                    ConnectedAppMetadata(TEST_APP_2, status = ConnectedAppStatus.ALLOWED),
                    ConnectedAppMetadata(TEST_APP_3, status = ConnectedAppStatus.ALLOWED),
                )
            )
        }

        launchFragment<SearchAppsFragment>(Bundle()).use {
            onView(withText("Allowed access")).check(matches(isDisplayed()))
            onView(withText(TEST_APP_NAME)).check(matches(isDisplayed()))
            onView(withText(TEST_APP_NAME_2)).check(matches(isDisplayed()))
            onView(withText(TEST_APP_NAME_3)).check(matches(isDisplayed()))
            onView(withText("Not allowed access")).check(doesNotExist())
            onView(withText("Inactive apps")).check(doesNotExist())
            onView(withText(R.string.connected_apps_text)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun noApps_displaysEmptyState() {
        whenever(viewModel.connectedApps).then {
            MutableLiveData(emptyList<ConnectedAppMetadata>())
        }

        launchFragment<SearchAppsFragment>(Bundle()).use {
            onView(withText("No Results")).check(matches(isDisplayed()))
        }
    }

    @Test
    fun appName_navigatesToFitnessAppPermissions() {
        val connectApp =
            listOf(
                ConnectedAppMetadata(
                    TEST_APP,
                    status = ConnectedAppStatus.ALLOWED,
                    permissionsType = AppPermissionsType.FITNESS_PERMISSIONS_ONLY,
                )
            )
        whenever(viewModel.connectedApps).then { MutableLiveData(connectApp) }

        launchFragment<SearchAppsFragment>(Bundle()) {
                navHostController.setGraph(R.navigation.nav_graph)
                navHostController.setCurrentDestination(R.id.searchConnectedApps)
                Navigation.setViewNavController(this.requireView(), navHostController)
            }
            .use {
                onView(withText(TEST_APP_NAME)).check(matches(isDisplayed()))
                onView(withText(TEST_APP_NAME)).perform(ViewActions.click())
                Truth.assertThat(navHostController.currentDestination?.id)
                    .isEqualTo(R.id.fitnessAppFragment)
            }
    }

    @Test
    fun appName_navigatesToMedicalAppPermissions() {
        val connectApp =
            listOf(
                ConnectedAppMetadata(
                    TEST_APP,
                    status = ConnectedAppStatus.ALLOWED,
                    permissionsType = AppPermissionsType.MEDICAL_PERMISSIONS_ONLY,
                )
            )
        whenever(viewModel.connectedApps).then { MutableLiveData(connectApp) }

        launchFragment<SearchAppsFragment>(Bundle()) {
                navHostController.setGraph(R.navigation.nav_graph)
                navHostController.setCurrentDestination(R.id.searchConnectedApps)
                Navigation.setViewNavController(this.requireView(), navHostController)
            }
            .use {
                onView(withText(TEST_APP_NAME)).check(matches(isDisplayed()))
                onView(withText(TEST_APP_NAME)).perform(ViewActions.click())
                Truth.assertThat(navHostController.currentDestination?.id)
                    .isEqualTo(R.id.medicalAppFragment)
            }
    }

    @Test
    fun appName_navigatesToCombinedPermissions() {
        val connectApp =
            listOf(
                ConnectedAppMetadata(
                    TEST_APP,
                    status = ConnectedAppStatus.ALLOWED,
                    permissionsType = AppPermissionsType.COMBINED_PERMISSIONS,
                )
            )
        whenever(viewModel.connectedApps).then { MutableLiveData(connectApp) }

        launchFragment<SearchAppsFragment>(Bundle()) {
                navHostController.setGraph(R.navigation.nav_graph)
                navHostController.setCurrentDestination(R.id.searchConnectedApps)
                Navigation.setViewNavController(this.requireView(), navHostController)
            }
            .use {
                onView(withText(TEST_APP_NAME)).check(matches(isDisplayed()))
                onView(withText(TEST_APP_NAME)).perform(ViewActions.click())
                Truth.assertThat(navHostController.currentDestination?.id)
                    .isEqualTo(R.id.combinedPermissionsFragment)
            }
    }

    @Test
    fun allowedApps_displayedAlphabetically() {
        val connectedApps =
            listOf(
                ConnectedAppMetadata(
                    AppMetadata(packageName = "package3", appName = "thirdApp", icon = null),
                    status = ConnectedAppStatus.ALLOWED,
                ),
                ConnectedAppMetadata(
                    AppMetadata(packageName = "package1", appName = "firstApp", icon = null),
                    status = ConnectedAppStatus.ALLOWED,
                ),
                ConnectedAppMetadata(
                    AppMetadata(packageName = "package2", appName = "secondApp", icon = null),
                    status = ConnectedAppStatus.ALLOWED,
                ),
            )
        whenever(viewModel.connectedApps).then { MutableLiveData(connectedApps) }

        launchFragment<SearchAppsFragment>(Bundle()).use {
            onView(withText("firstApp")).check(matches(isDisplayed()))
            onView(withText("secondApp")).perform(scrollTo()).check(matches(isDisplayed()))
            onView(withText("firstApp")).check(matches(isAbove(withText("secondApp"))))
            onView(withText("thirdApp")).perform(scrollTo()).check(matches(isDisplayed()))
            onView(withText("secondApp")).check(matches(isAbove(withText("thirdApp"))))
        }
    }

    @Test
    fun notAllowedApps_displayedAlphabetically() {
        val connectedApps =
            listOf(
                ConnectedAppMetadata(
                    AppMetadata(packageName = "package3", appName = "thirdApp", icon = null),
                    status = ConnectedAppStatus.DENIED,
                ),
                ConnectedAppMetadata(
                    AppMetadata(packageName = "package1", appName = "firstApp", icon = null),
                    status = ConnectedAppStatus.DENIED,
                ),
                ConnectedAppMetadata(
                    AppMetadata(packageName = "package2", appName = "secondApp", icon = null),
                    status = ConnectedAppStatus.DENIED,
                ),
            )
        whenever(viewModel.connectedApps).then { MutableLiveData(connectedApps) }

        launchFragment<SearchAppsFragment>(Bundle()).use {
            onView(withText("firstApp")).check(matches(isDisplayed()))
            onView(withText("secondApp")).perform(scrollTo()).check(matches(isDisplayed()))
            onView(withText("firstApp")).check(matches(isAbove(withText("secondApp"))))
            onView(withText("thirdApp")).perform(scrollTo()).check(matches(isDisplayed()))
            onView(withText("secondApp")).check(matches(isAbove(withText("thirdApp"))))
        }
    }

    @Test
    fun inactiveApps_displayedAlphabetically() {
        val connectedApps =
            listOf(
                ConnectedAppMetadata(
                    AppMetadata(packageName = "package3", appName = "thirdApp", icon = null),
                    status = ConnectedAppStatus.INACTIVE,
                ),
                ConnectedAppMetadata(
                    AppMetadata(packageName = "package1", appName = "firstApp", icon = null),
                    status = ConnectedAppStatus.INACTIVE,
                ),
                ConnectedAppMetadata(
                    AppMetadata(packageName = "package2", appName = "secondApp", icon = null),
                    status = ConnectedAppStatus.INACTIVE,
                ),
            )
        whenever(viewModel.connectedApps).then { MutableLiveData(connectedApps) }

        launchFragment<SearchAppsFragment>(Bundle()).use {
            onView(withText("firstApp")).check(matches(isDisplayed()))
            onView(withText("secondApp")).perform(scrollTo()).check(matches(isDisplayed()))
            onView(withText("firstApp")).check(matches(isAbove(withText("secondApp"))))
            onView(withText("thirdApp")).perform(scrollTo()).check(matches(isDisplayed()))
            onView(withText("secondApp")).check(matches(isAbove(withText("thirdApp"))))
        }
    }
}
