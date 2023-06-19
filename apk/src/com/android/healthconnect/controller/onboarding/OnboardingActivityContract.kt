package com.android.healthconnect.controller.onboarding

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract

class OnboardingActivityContract : ActivityResultContract<Int, String?>() {

    companion object {
        const val INTENT_RESULT_CANCELLED = "CANCELLED"
    }

    override fun createIntent(context: Context, input: Int): Intent {
        return Intent(context, OnboardingActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): String? {
        return when (resultCode) {
            Activity.RESULT_CANCELED -> INTENT_RESULT_CANCELLED
            else -> null
        }
    }
}
