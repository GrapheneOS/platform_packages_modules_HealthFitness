package com.android.healthconnect.controller.tests.permissions.connectedapps.searchapps

import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.android.healthconnect.controller.permissions.connectedapps.ConnectedAppMetadata
import com.android.healthconnect.controller.permissions.connectedapps.ConnectedAppStatus
import com.android.healthconnect.controller.permissions.connectedapps.ConnectedAppsViewModel
import com.android.healthconnect.controller.permissions.connectedapps.searchapps.SearchAppsFragment
import com.android.healthconnect.controller.tests.utils.TEST_APP
import com.android.healthconnect.controller.tests.utils.TEST_APP_2
import com.android.healthconnect.controller.tests.utils.TEST_APP_3
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME_2
import com.android.healthconnect.controller.tests.utils.TEST_APP_NAME_3
import com.android.healthconnect.controller.tests.utils.launchFragment
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito

@HiltAndroidTest
class SearchAppsFragmentTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @BindValue
    val viewModel: ConnectedAppsViewModel = Mockito.mock(ConnectedAppsViewModel::class.java)

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun searchAppsFragment_isDisplayedCorrectly() {
        Mockito.`when`(viewModel.connectedApps).then {
            MutableLiveData(
                listOf(
                    ConnectedAppMetadata(TEST_APP, status = ConnectedAppStatus.ALLOWED),
                    ConnectedAppMetadata(TEST_APP_2, status = ConnectedAppStatus.DENIED),
                    ConnectedAppMetadata(TEST_APP_3, status = ConnectedAppStatus.INACTIVE)))
        }

        launchFragment<SearchAppsFragment>(Bundle())

        onView(withText("Allowed access")).check(ViewAssertions.matches(isDisplayed()))
        onView(withText(TEST_APP_NAME)).check(ViewAssertions.matches(isDisplayed()))
        onView(withText("Not allowed access")).check(ViewAssertions.matches(isDisplayed()))
        onView(withText(TEST_APP_NAME_2)).check(ViewAssertions.matches(isDisplayed()))
        onView(withText("Inactive apps")).check(ViewAssertions.matches(isDisplayed()))
        onView(withText(TEST_APP_NAME_3)).check(ViewAssertions.matches(isDisplayed()))
    }
}
