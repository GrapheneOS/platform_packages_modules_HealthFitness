package com.android.healthconnect.controller.tests.onboarding

import android.content.ComponentName
import android.content.Intent
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.platform.app.InstrumentationRegistry
import com.android.healthconnect.controller.R
import com.android.healthconnect.controller.onboarding.OnboardingActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class OnboardingScreenTest {
    @get:Rule val hiltRule = HiltAndroidRule(this)
    @Before
    fun setup() {
        hiltRule.inject()
    }

    private fun startOnboardingActivity() {
        val startOnboardingActivityIntent =
            Intent.makeMainActivity(
                    ComponentName(
                        InstrumentationRegistry.getInstrumentation().getContext(),
                        OnboardingActivity::class.java))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

        InstrumentationRegistry.getInstrumentation()
            .getContext()
            .startActivity(startOnboardingActivityIntent)
    }

    @Test
    fun onboardingScreen_isDisplayedCorrectly() {
        val startOnboardingActivityIntent =
            Intent.makeMainActivity(
                    ComponentName(
                        InstrumentationRegistry.getInstrumentation().getContext(),
                        OnboardingActivity::class.java))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)

        InstrumentationRegistry.getInstrumentation()
            .getContext()
            .startActivity(startOnboardingActivityIntent)

        Espresso.onView(ViewMatchers.withText(R.string.onboarding_title))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withText(R.string.onboarding_description))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withId(R.id.onboarding_image))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withId(R.id.share_icon))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withText(R.string.share_data))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withText(R.string.share_data_description))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withId(R.id.manage_icon))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withText(R.string.manage_your_settings))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withText(R.string.manage_your_settings_description))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withText(R.string.onboarding_get_started_button_text))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withText(R.string.onboarding_go_back_button_text))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun onboardingScreen_goBackButton_isClickable() {
        startOnboardingActivity()

        Espresso.onView(ViewMatchers.withId(R.id.go_back_button)).perform(ViewActions.click())
    }

    @Test
    fun onboardingScreen_getStartedButton_isClickable() {
        startOnboardingActivity()

        Espresso.onView(ViewMatchers.withId(R.id.get_started_button)).perform(ViewActions.click())
    }
}
