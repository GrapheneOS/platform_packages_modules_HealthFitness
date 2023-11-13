package com.android.healthconnect.controller.tests.migration

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.android.healthconnect.controller.migration.MigrationInProgressFragment
import com.android.healthconnect.controller.tests.utils.launchFragment
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class MigrationInProgressFragmentTest {

    @get:Rule val hiltRule = HiltAndroidRule(this)

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun migrationInProgressFragment_displaysCorrectly() {
        launchFragment<MigrationInProgressFragment>()

        onView(withText("Integration in progress")).check(matches(isDisplayed()))
        onView(
                withText(
                    "Health Connect is being integrated with the Android system." +
                        "\n\nIt may take some time while your data and permissions are being transferred."))
            .check(matches(isDisplayed()))
    }
}
