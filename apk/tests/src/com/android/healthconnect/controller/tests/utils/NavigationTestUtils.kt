package com.android.healthconnect.controller.tests.utils

import android.content.Context
import com.android.healthconnect.controller.shared.Constants.NATIVE_STEPS_BANNER_SEEN
import com.android.healthconnect.controller.shared.Constants.USER_ACTIVITY_TRACKER

fun showNativeSteps(context: Context, show: Boolean) {
    val sharedPreference = context.getSharedPreferences(USER_ACTIVITY_TRACKER, Context.MODE_PRIVATE)
    val editor = sharedPreference.edit()
    editor.putBoolean(NATIVE_STEPS_BANNER_SEEN, !show)
    editor.apply()
}

fun setPreferenceSeen(context: Context, preferenceName: String, seen: Boolean) {
    val sharedPreference = context.getSharedPreferences(USER_ACTIVITY_TRACKER, Context.MODE_PRIVATE)
    val editor = sharedPreference.edit()
    editor.putBoolean(preferenceName, seen)
    editor.apply()
}
