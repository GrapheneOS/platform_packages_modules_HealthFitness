package com.android.healthconnect.controller.tests.utils

import android.content.Context
import com.android.healthconnect.controller.onboarding.OnboardingActivity

fun showOnboarding(context: Context, show: Boolean) {
    val sharedPreference =
        context.getSharedPreferences(OnboardingActivity.USER_ACTIVITY_TRACKER, Context.MODE_PRIVATE)
    val editor = sharedPreference.edit()
    editor.putBoolean(OnboardingActivity.ONBOARDING_SHOWN_PREF_KEY, !show)
    editor.apply()
}
